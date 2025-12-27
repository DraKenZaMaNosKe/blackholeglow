package com.secret.blackholeglow.video;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║              🔥 VideoSphere3D - Video Texture en Esfera 3D               ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Reproduce un video MP4 mapeado sobre una esfera 3D.                     ║
 * ║                                                                          ║
 * ║  Características:                                                        ║
 * ║  • MediaCodec para decodificación eficiente                              ║
 * ║  • SurfaceTexture → GL_TEXTURE_EXTERNAL_OES                              ║
 * ║  • Esfera generada proceduralmente (UV sphere)                           ║
 * ║  • Rotación automática sobre eje Y                                       ║
 * ║  • Loop infinito del video                                               ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class VideoSphere3D {
    private static final String TAG = "VideoSphere3D";

    // Contexto y video
    private final Context context;
    private final String videoFileName;

    // MediaCodec
    private MediaExtractor extractor;
    private MediaCodec decoder;
    private Surface surface;
    private SurfaceTexture surfaceTexture;
    private int videoTextureId = -1;

    // Decoder thread
    private Thread decoderThread;
    private volatile boolean isRunning = false;

    // Esfera 3D
    private static final int SPHERE_SLICES = 32;
    private static final int SPHERE_STACKS = 24;
    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;
    private ShortBuffer indexBuffer;
    private int indexCount;

    // Shader
    private int shaderProgram;
    private int aPositionLoc;
    private int aTexCoordLoc;
    private int uMVPMatrixLoc;
    private int uTextureLoc;

    // Matrices
    private float[] modelMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] projectionMatrix = new float[16];
    private float[] mvpMatrix = new float[16];
    private float[] tempMatrix = new float[16];

    // Transformaciones
    private float rotationY = 0f;
    private float rotationSpeed = 8f;  // grados por segundo
    private float positionX = 0f;
    private float positionY = 0.5f;    // Un poco arriba del centro
    private float positionZ = -3f;     // Alejado de la cámara
    private float scale = 0.8f;

    // Estado
    private boolean initialized = false;
    private int screenWidth = 1080;
    private int screenHeight = 1920;

    // ═══════════════════════════════════════════════════════════════
    // SHADERS
    // ═══════════════════════════════════════════════════════════════

    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "precision highp float;\n" +
        "in vec3 aPosition;\n" +
        "in vec2 aTexCoord;\n" +
        "uniform mat4 uMVPMatrix;\n" +
        "out vec2 vTexCoord;\n" +
        "void main() {\n" +
        "    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);\n" +
        "    vTexCoord = aTexCoord;\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "#extension GL_OES_EGL_image_external_essl3 : require\n" +
        "precision mediump float;\n" +
        "uniform samplerExternalOES uTexture;\n" +
        "in vec2 vTexCoord;\n" +
        "out vec4 fragColor;\n" +
        "void main() {\n" +
        "    fragColor = texture(uTexture, vTexCoord);\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════

    public VideoSphere3D(Context context, String videoFileName) {
        this.context = context.getApplicationContext();
        this.videoFileName = videoFileName;
        Log.d(TAG, "🔥 VideoSphere3D creado: " + videoFileName);
    }

    // ═══════════════════════════════════════════════════════════════
    // INICIALIZACIÓN
    // ═══════════════════════════════════════════════════════════════

    public void initialize() {
        if (initialized) return;

        Log.d(TAG, "🔧 Inicializando VideoSphere3D...");

        // 1. Crear textura OES
        createVideoTexture();

        // 2. Compilar shader
        compileShader();

        // 3. Generar esfera
        generateSphere();

        // 4. Inicializar matrices
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.setIdentityM(viewMatrix, 0);
        Matrix.setLookAtM(viewMatrix, 0,
            0f, 0f, 0f,    // eye
            0f, 0f, -1f,   // center
            0f, 1f, 0f);   // up

        // 5. Iniciar decoder
        initDecoder();

        initialized = true;
        Log.d(TAG, "✅ VideoSphere3D inicializado");
    }

    private void createVideoTexture() {
        int[] textures = new int[1];
        GLES30.glGenTextures(1, textures, 0);
        videoTextureId = textures[0];

        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, videoTextureId);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        surfaceTexture = new SurfaceTexture(videoTextureId);
        surface = new Surface(surfaceTexture);

        Log.d(TAG, "📹 Textura OES creada: " + videoTextureId);
    }

    private void compileShader() {
        int vs = compileShaderCode(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compileShaderCode(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vs);
        GLES30.glAttachShader(shaderProgram, fs);
        GLES30.glLinkProgram(shaderProgram);

        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "aPosition");
        aTexCoordLoc = GLES30.glGetAttribLocation(shaderProgram, "aTexCoord");
        uMVPMatrixLoc = GLES30.glGetUniformLocation(shaderProgram, "uMVPMatrix");
        uTextureLoc = GLES30.glGetUniformLocation(shaderProgram, "uTexture");

        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);

        Log.d(TAG, "🎨 Shader compilado");
    }

    private int compileShaderCode(int type, String code) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, code);
        GLES30.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Shader error: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    // ═══════════════════════════════════════════════════════════════
    // GENERACIÓN DE ESFERA
    // ═══════════════════════════════════════════════════════════════

    private void generateSphere() {
        int numVertices = (SPHERE_SLICES + 1) * (SPHERE_STACKS + 1);
        float[] vertices = new float[numVertices * 3];
        float[] texCoords = new float[numVertices * 2];

        int vertexIndex = 0;
        int texIndex = 0;

        for (int stack = 0; stack <= SPHERE_STACKS; stack++) {
            float phi = (float) (Math.PI * stack / SPHERE_STACKS);
            float sinPhi = (float) Math.sin(phi);
            float cosPhi = (float) Math.cos(phi);

            for (int slice = 0; slice <= SPHERE_SLICES; slice++) {
                float theta = (float) (2 * Math.PI * slice / SPHERE_SLICES);
                float sinTheta = (float) Math.sin(theta);
                float cosTheta = (float) Math.cos(theta);

                // Posición del vértice
                float x = sinPhi * cosTheta;
                float y = cosPhi;
                float z = sinPhi * sinTheta;

                vertices[vertexIndex++] = x;
                vertices[vertexIndex++] = y;
                vertices[vertexIndex++] = z;

                // Coordenadas UV
                float u = (float) slice / SPHERE_SLICES;
                float v = (float) stack / SPHERE_STACKS;
                texCoords[texIndex++] = u;
                texCoords[texIndex++] = v;
            }
        }

        // Generar índices
        int numIndices = SPHERE_SLICES * SPHERE_STACKS * 6;
        short[] indices = new short[numIndices];
        int index = 0;

        for (int stack = 0; stack < SPHERE_STACKS; stack++) {
            for (int slice = 0; slice < SPHERE_SLICES; slice++) {
                int first = stack * (SPHERE_SLICES + 1) + slice;
                int second = first + SPHERE_SLICES + 1;

                indices[index++] = (short) first;
                indices[index++] = (short) second;
                indices[index++] = (short) (first + 1);

                indices[index++] = (short) second;
                indices[index++] = (short) (second + 1);
                indices[index++] = (short) (first + 1);
            }
        }

        indexCount = numIndices;

        // Crear buffers
        vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(vertices).position(0);

        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.length * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        texCoordBuffer.put(texCoords).position(0);

        indexBuffer = ByteBuffer.allocateDirect(indices.length * 2)
            .order(ByteOrder.nativeOrder()).asShortBuffer();
        indexBuffer.put(indices).position(0);

        Log.d(TAG, "🌐 Esfera generada: " + numVertices + " vértices, " + numIndices + " índices");
    }

    // ═══════════════════════════════════════════════════════════════
    // MEDIACODEC DECODER
    // ═══════════════════════════════════════════════════════════════

    private void initDecoder() {
        try {
            extractor = new MediaExtractor();
            AssetFileDescriptor afd = context.getAssets().openFd(videoFileName);
            extractor.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();

            // Buscar track de video
            int trackIndex = -1;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("video/")) {
                    trackIndex = i;
                    break;
                }
            }

            if (trackIndex < 0) {
                Log.e(TAG, "No se encontró track de video");
                return;
            }

            extractor.selectTrack(trackIndex);
            MediaFormat format = extractor.getTrackFormat(trackIndex);
            String mime = format.getString(MediaFormat.KEY_MIME);

            decoder = MediaCodec.createDecoderByType(mime);
            decoder.configure(format, surface, null, 0);
            decoder.start();

            isRunning = true;
            decoderThread = new Thread(this::decoderLoop, "VideoSphereDecoder");
            decoderThread.start();

            Log.d(TAG, "▶️ Decoder iniciado");

        } catch (IOException e) {
            Log.e(TAG, "Error iniciando decoder: " + e.getMessage());
        }
    }

    private void decoderLoop() {
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean sawEndOfStream = false;

        while (isRunning) {
            // Si llegamos al final, hacer flush y reiniciar ANTES de seguir
            if (sawEndOfStream) {
                decoder.flush();
                extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                sawEndOfStream = false;
                Log.d(TAG, "🔄 Video loop reiniciado");
                continue;
            }

            // Input
            int inputIndex = decoder.dequeueInputBuffer(10000);
            if (inputIndex >= 0) {
                ByteBuffer inputBuffer = decoder.getInputBuffer(inputIndex);
                int sampleSize = extractor.readSampleData(inputBuffer, 0);

                if (sampleSize < 0) {
                    // Fin del video - marcar pero NO hacer seek todavía
                    decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                } else {
                    decoder.queueInputBuffer(inputIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                    extractor.advance();
                }
            }

            // Output - vaciar TODOS los frames pendientes
            int outputIndex;
            while ((outputIndex = decoder.dequeueOutputBuffer(info, 0)) >= 0) {
                decoder.releaseOutputBuffer(outputIndex, true);

                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    sawEndOfStream = true;
                }
            }

            // Control de framerate (~30 fps)
            try {
                Thread.sleep(33);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // UPDATE Y DRAW
    // ═══════════════════════════════════════════════════════════════

    public void update(float deltaTime) {
        // Rotar el sol
        rotationY += rotationSpeed * deltaTime;
        if (rotationY > 360f) rotationY -= 360f;
    }

    public void draw() {
        if (!initialized || shaderProgram == 0) return;

        // Actualizar textura de video
        try {
            surfaceTexture.updateTexImage();
        } catch (Exception e) {
            // Ignorar
        }

        GLES30.glUseProgram(shaderProgram);

        // Calcular matrices
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, positionX, positionY, positionZ);
        Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f);
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

        // MVP = Projection * View * Model
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);

        GLES30.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, mvpMatrix, 0);

        // Textura
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, videoTextureId);
        GLES30.glUniform1i(uTextureLoc, 0);

        // Vértices
        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glVertexAttribPointer(aPositionLoc, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        GLES30.glEnableVertexAttribArray(aTexCoordLoc);
        GLES30.glVertexAttribPointer(aTexCoordLoc, 2, GLES30.GL_FLOAT, false, 0, texCoordBuffer);

        // Dibujar esfera
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_SHORT, indexBuffer);

        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glDisableVertexAttribArray(aTexCoordLoc);
    }

    // ═══════════════════════════════════════════════════════════════
    // CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════

    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;

        float aspect = (float) width / height;
        Matrix.perspectiveM(projectionMatrix, 0, 45f, aspect, 0.1f, 100f);
    }

    public void setPosition(float x, float y, float z) {
        this.positionX = x;
        this.positionY = y;
        this.positionZ = z;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setRotationSpeed(float speed) {
        this.rotationSpeed = speed;
    }

    // ═══════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════

    public void release() {
        Log.d(TAG, "🗑️ Liberando VideoSphere3D...");

        isRunning = false;
        if (decoderThread != null) {
            try {
                decoderThread.join(1000);
            } catch (InterruptedException e) {
                // Ignorar
            }
        }

        if (decoder != null) {
            try {
                decoder.stop();
                decoder.release();
            } catch (Exception e) {
                // Ignorar
            }
            decoder = null;
        }

        if (extractor != null) {
            extractor.release();
            extractor = null;
        }

        if (surface != null) {
            surface.release();
            surface = null;
        }

        if (surfaceTexture != null) {
            surfaceTexture.release();
            surfaceTexture = null;
        }

        if (videoTextureId != -1) {
            GLES30.glDeleteTextures(1, new int[]{videoTextureId}, 0);
            videoTextureId = -1;
        }

        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }

        initialized = false;
        Log.d(TAG, "✅ VideoSphere3D liberado");
    }
}
