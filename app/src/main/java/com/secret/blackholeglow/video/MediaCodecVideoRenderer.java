package com.secret.blackholeglow.video;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * MediaCodecVideoRenderer - Reproducción de video usando MediaCodec directamente
 *
 * Ventajas sobre ExoPlayer/MediaPlayer:
 * - Control TOTAL del lifecycle
 * - Podemos reiniciar el decoder cuando queramos
 * - No dependemos de abstracciones que pueden fallar
 *
 * Basado en: https://bigflake.com/mediacodec/
 * y https://github.com/PhilLab/Android-MediaCodec-Examples
 */
public class MediaCodecVideoRenderer {
    private static final String TAG = "MediaCodecVideo";

    private final Context context;
    private final String videoFileName;

    // MediaCodec components
    private MediaExtractor extractor;
    private MediaCodec decoder;
    private int videoTrackIndex = -1;

    // Surface para recibir frames decodificados
    private SurfaceTexture surfaceTexture;
    private Surface surface;
    private int videoTextureId = -1;

    // Estado
    private boolean isInitialized = false;
    private volatile boolean isRunning = false;
    private Thread decoderThread;

    // OpenGL
    private int shaderProgram;
    private int aPositionLoc, aTexCoordLoc, uMVPLoc, uSTLoc;
    private FloatBuffer vertexBuffer, texCoordBuffer;
    private final float[] mvpMatrix = new float[16];
    private final float[] stMatrix = new float[16];

    private static final float[] VERTICES = {-1,-1, 1,-1, -1,1, 1,1};
    private static final float[] TEX_COORDS = {0,0, 1,0, 0,1, 1,1};

    private static final String VERTEX_SHADER =
        "attribute vec4 aPosition;\n" +
        "attribute vec2 aTexCoord;\n" +
        "uniform mat4 uMVP;\n" +
        "uniform mat4 uST;\n" +
        "varying vec2 vTexCoord;\n" +
        "void main() {\n" +
        "    gl_Position = uMVP * aPosition;\n" +
        "    vTexCoord = (uST * vec4(aTexCoord, 0.0, 1.0)).xy;\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "#extension GL_OES_EGL_image_external : require\n" +
        "precision mediump float;\n" +
        "uniform samplerExternalOES uTexture;\n" +
        "varying vec2 vTexCoord;\n" +
        "void main() {\n" +
        "    gl_FragColor = texture2D(uTexture, vTexCoord);\n" +
        "}\n";

    public MediaCodecVideoRenderer(Context context, String videoFileName) {
        this.context = context;
        this.videoFileName = videoFileName;
        Matrix.setIdentityM(mvpMatrix, 0);
        Matrix.setIdentityM(stMatrix, 0);
    }

    public void initialize() {
        if (isInitialized) return;

        Log.d(TAG, "🎬 Inicializando MediaCodec para: " + videoFileName);

        // Buffers OpenGL
        ByteBuffer bb = ByteBuffer.allocateDirect(VERTICES.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(VERTICES).position(0);

        bb = ByteBuffer.allocateDirect(TEX_COORDS.length * 4);
        bb.order(ByteOrder.nativeOrder());
        texCoordBuffer = bb.asFloatBuffer();
        texCoordBuffer.put(TEX_COORDS).position(0);

        // Shader
        shaderProgram = createProgram();
        if (shaderProgram == 0) {
            Log.e(TAG, "Error creando shader program");
            return;
        }

        aPositionLoc = GLES20.glGetAttribLocation(shaderProgram, "aPosition");
        aTexCoordLoc = GLES20.glGetAttribLocation(shaderProgram, "aTexCoord");
        uMVPLoc = GLES20.glGetUniformLocation(shaderProgram, "uMVP");
        uSTLoc = GLES20.glGetUniformLocation(shaderProgram, "uST");

        // Textura OES para video
        int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0);
        videoTextureId = tex[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, videoTextureId);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        // SurfaceTexture + Surface
        surfaceTexture = new SurfaceTexture(videoTextureId);
        surface = new Surface(surfaceTexture);

        // Iniciar decoder
        startDecoder();

        isInitialized = true;
        Log.d(TAG, "✅ MediaCodec inicializado");
    }

    private void startDecoder() {
        isRunning = true;
        decoderThread = new Thread(this::decoderLoop, "MediaCodecDecoder");
        decoderThread.start();
    }

    private void decoderLoop() {
        Log.d(TAG, "🔄 Iniciando loop de decodificación");

        while (isRunning) {
            try {
                // Inicializar extractor y decoder
                if (!initializeMediaCodec()) {
                    Log.e(TAG, "Error inicializando MediaCodec, reintentando en 1s...");
                    Thread.sleep(1000);
                    continue;
                }

                // Loop de decodificación
                decodeLoop();

                // Si llegamos aquí, el video terminó - reiniciar para loop
                releaseDecoder();
                Log.d(TAG, "🔄 Reiniciando video (loop)");

            } catch (InterruptedException e) {
                Log.d(TAG, "Decoder thread interrumpido");
                break;
            } catch (Exception e) {
                Log.e(TAG, "Error en decoder loop: " + e.getMessage());
                releaseDecoder();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    break;
                }
            }
        }

        releaseDecoder();
        Log.d(TAG, "🛑 Decoder loop terminado");
    }

    private boolean initializeMediaCodec() {
        try {
            // Abrir video desde assets
            AssetFileDescriptor afd = context.getAssets().openFd(videoFileName);

            extractor = new MediaExtractor();
            extractor.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();

            // Buscar track de video
            videoTrackIndex = -1;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("video/")) {
                    videoTrackIndex = i;
                    break;
                }
            }

            if (videoTrackIndex < 0) {
                Log.e(TAG, "No se encontró track de video");
                return false;
            }

            extractor.selectTrack(videoTrackIndex);
            MediaFormat format = extractor.getTrackFormat(videoTrackIndex);
            String mime = format.getString(MediaFormat.KEY_MIME);

            Log.d(TAG, "📹 Video: " + mime + " " +
                format.getInteger(MediaFormat.KEY_WIDTH) + "x" +
                format.getInteger(MediaFormat.KEY_HEIGHT));

            // Crear decoder
            decoder = MediaCodec.createDecoderByType(mime);
            decoder.configure(format, surface, null, 0);
            decoder.start();

            Log.d(TAG, "✅ MediaCodec configurado");
            return true;

        } catch (IOException e) {
            Log.e(TAG, "Error abriendo video: " + e.getMessage());
            return false;
        }
    }

    private void decodeLoop() {
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        boolean inputDone = false;
        boolean outputDone = false;
        long startTime = System.nanoTime();

        while (!outputDone && isRunning) {
            // Enviar datos al decoder
            if (!inputDone) {
                int inputIndex = decoder.dequeueInputBuffer(10000);
                if (inputIndex >= 0) {
                    ByteBuffer inputBuffer = decoder.getInputBuffer(inputIndex);
                    int sampleSize = extractor.readSampleData(inputBuffer, 0);

                    if (sampleSize < 0) {
                        // Fin del archivo - enviar EOS
                        decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                    } else {
                        long presentationTimeUs = extractor.getSampleTime();
                        decoder.queueInputBuffer(inputIndex, 0, sampleSize, presentationTimeUs, 0);
                        extractor.advance();
                    }
                }
            }

            // Obtener frames decodificados
            int outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000);
            if (outputIndex >= 0) {
                // Sincronización de tiempo para playback suave
                long presentationTimeNs = bufferInfo.presentationTimeUs * 1000;
                long elapsed = System.nanoTime() - startTime;
                long sleepTime = (presentationTimeNs - elapsed) / 1000000;

                if (sleepTime > 0 && sleepTime < 100) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                // Liberar buffer y renderizar al Surface
                decoder.releaseOutputBuffer(outputIndex, true);

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    outputDone = true;
                }
            }
        }
    }

    private void releaseDecoder() {
        if (decoder != null) {
            try {
                decoder.stop();
                decoder.release();
            } catch (Exception ignored) {}
            decoder = null;
        }

        if (extractor != null) {
            try {
                extractor.release();
            } catch (Exception ignored) {}
            extractor = null;
        }
    }

    public void draw() {
        if (!isInitialized || videoTextureId == -1) return;

        // Siempre intentar actualizar textura - updateTexImage() es idempotente
        if (surfaceTexture != null) {
            try {
                surfaceTexture.updateTexImage();
                surfaceTexture.getTransformMatrix(stMatrix);
            } catch (Exception e) {
                // Ignorar - no hay frame nuevo disponible todavía
            }
        }

        GLES20.glUseProgram(shaderProgram);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, videoTextureId);

        GLES20.glUniformMatrix4fv(uMVPLoc, 1, false, mvpMatrix, 0);
        GLES20.glUniformMatrix4fv(uSTLoc, 1, false, stMatrix, 0);

        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(aTexCoordLoc);
        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(aPositionLoc);
        GLES20.glDisableVertexAttribArray(aTexCoordLoc);
    }

    public void pause() {
        Log.d(TAG, "⏸️ Pause (no hace nada - video sigue corriendo)");
    }

    public void resume() {
        Log.d(TAG, "▶️ Resume");
    }

    public void release() {
        Log.d(TAG, "🗑️ Liberando recursos MediaCodec");
        isInitialized = false;
        isRunning = false;

        // Detener thread
        if (decoderThread != null) {
            decoderThread.interrupt();
            try {
                decoderThread.join(1000);
            } catch (InterruptedException ignored) {}
            decoderThread = null;
        }

        releaseDecoder();

        if (surface != null) {
            surface.release();
            surface = null;
        }

        if (surfaceTexture != null) {
            surfaceTexture.release();
            surfaceTexture = null;
        }

        if (videoTextureId != -1) {
            GLES20.glDeleteTextures(1, new int[]{videoTextureId}, 0);
            videoTextureId = -1;
        }

        if (shaderProgram != 0) {
            GLES20.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
    }

    public void setScreenSize(int w, int h) {
        // No necesitamos hacer nada especial
    }

    private int createProgram() {
        int vs = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        if (vs == 0 || fs == 0) return 0;

        int prog = GLES20.glCreateProgram();
        GLES20.glAttachShader(prog, vs);
        GLES20.glAttachShader(prog, fs);
        GLES20.glLinkProgram(prog);

        int[] status = new int[1];
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) {
            GLES20.glDeleteProgram(prog);
            return 0;
        }

        GLES20.glDeleteShader(vs);
        GLES20.glDeleteShader(fs);
        return prog;
    }

    private int compileShader(int type, String src) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, src);
        GLES20.glCompileShader(shader);

        int[] status = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    public boolean isInitialized() { return isInitialized; }
    public boolean isPlaying() { return isRunning && decoder != null; }
}
