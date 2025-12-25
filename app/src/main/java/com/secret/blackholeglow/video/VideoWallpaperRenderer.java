package com.secret.blackholeglow.video;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
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
 * VideoWallpaperRenderer - SIMPLE Y DIRECTO
 *
 * El video NUNCA se pausa. Loop infinito hasta que se destruya.
 */
public class VideoWallpaperRenderer {
    private static final String TAG = "VideoRenderer";

    private final Context context;
    private final String videoFileName;

    private MediaPlayer mediaPlayer;
    private SurfaceTexture surfaceTexture;
    private Surface surface;

    private int videoTextureId = -1;
    private boolean isInitialized = false;
    private volatile boolean frameAvailable = false;

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

    public VideoWallpaperRenderer(Context context, String videoFileName) {
        this.context = context;
        this.videoFileName = videoFileName;
        Matrix.setIdentityM(mvpMatrix, 0);
        Matrix.setIdentityM(stMatrix, 0);
    }

    public void initialize() {
        if (isInitialized) return;

        Log.d(TAG, "Inicializando video: " + videoFileName);

        // Buffers
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
        if (shaderProgram == 0) return;

        aPositionLoc = GLES20.glGetAttribLocation(shaderProgram, "aPosition");
        aTexCoordLoc = GLES20.glGetAttribLocation(shaderProgram, "aTexCoord");
        uMVPLoc = GLES20.glGetUniformLocation(shaderProgram, "uMVP");
        uSTLoc = GLES20.glGetUniformLocation(shaderProgram, "uST");

        // Textura OES
        int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0);
        videoTextureId = tex[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, videoTextureId);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        // SurfaceTexture + MediaPlayer
        surfaceTexture = new SurfaceTexture(videoTextureId);
        surfaceTexture.setOnFrameAvailableListener(st -> frameAvailable = true);
        surface = new Surface(surfaceTexture);

        try {
            mediaPlayer = new MediaPlayer();
            AssetFileDescriptor afd = context.getAssets().openFd(videoFileName);
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();

            mediaPlayer.setSurface(surface);
            mediaPlayer.setLooping(true);
            mediaPlayer.setVolume(0f, 0f);

            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "Video listo, iniciando loop infinito");
                mp.start();
                isInitialized = true; // Solo cuando está listo
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "Error: " + what);
                return true;
            });

            mediaPlayer.prepareAsync();

        } catch (IOException e) {
            Log.e(TAG, "Error: " + e.getMessage());
        }
    }

    public void draw() {
        if (!isInitialized || videoTextureId == -1) return;

        // Auto-recovery: Si el video se detuvo, reiniciarlo
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            try {
                Log.d(TAG, "Video detenido - reiniciando...");
                mediaPlayer.start();
            } catch (Exception e) {
                Log.e(TAG, "Error reiniciando: " + e.getMessage());
            }
        }

        // Actualizar textura
        if (frameAvailable && surfaceTexture != null) {
            try {
                surfaceTexture.updateTexImage();
                surfaceTexture.getTransformMatrix(stMatrix);
                frameAvailable = false;
            } catch (Exception ignored) {}
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

    // NO PAUSAR - El video siempre corre
    public void pause() {
        // Intencionalmente vacío - NUNCA pausamos
    }

    public void resume() {
        // Asegurar que esté corriendo
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            try {
                mediaPlayer.start();
            } catch (Exception ignored) {}
        }
    }

    public void release() {
        Log.d(TAG, "Liberando recursos");
        isInitialized = false;
        frameAvailable = false;

        if (mediaPlayer != null) {
            try {
                mediaPlayer.setOnPreparedListener(null); // Evitar callback después de release
                mediaPlayer.setOnErrorListener(null);
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
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
    public boolean isPlaying() { return mediaPlayer != null && mediaPlayer.isPlaying(); }
}
