package com.secret.blackholeglow;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.opengl.GLES11Ext;
import android.view.Surface;
import android.util.Log;

import java.io.IOException;
import java.nio.FloatBuffer;

public class VideoBackground implements SceneObject, SurfaceTexture.OnFrameAvailableListener {

    private float frameTimer = 0f;

    private final Context context;
    private MediaPlayer mediaPlayer;
    private SurfaceTexture surfaceTexture;
    private int textureId;

    private int program;
    private int uMatrixLocation;
    private int aPositionLocation;
    private int aTexCoordLocation;

    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;

    private boolean updateFrame = false;

    public VideoBackground(Context context) {
        this.context = context;

        setupGL();
        setupMediaPlayer();
    }

    private void setupGL() {
        textureId = ShaderUtils.createExternalTexture();
        surfaceTexture = new SurfaceTexture(textureId);
        surfaceTexture.setOnFrameAvailableListener(this);

        // Shader para video con GL_TEXTURE_EXTERNAL_OES
        String vertexShader =
                "attribute vec4 a_Position;" +
                        "attribute vec2 a_TexCoord;" +
                        "varying vec2 v_TexCoord;" +
                        "void main() {" +
                        "  gl_Position = a_Position;" +
                        "  v_TexCoord = a_TexCoord;" +
                        "}";

        String fragmentShader =
                "#extension GL_OES_EGL_image_external : require\n" +
                        "precision mediump float;" +
                        "uniform samplerExternalOES u_Texture;" +
                        "varying vec2 v_TexCoord;" +
                        "void main() {" +
                        "  gl_FragColor = texture2D(u_Texture, v_TexCoord);" +
                        "}";

        program = ShaderUtils.createProgram(vertexShader, fragmentShader);
        aPositionLocation = GLES20.glGetAttribLocation(program, "a_Position");
        aTexCoordLocation = GLES20.glGetAttribLocation(program, "a_TexCoord");
    }

    private void setupMediaPlayer() {
        mediaPlayer = MediaPlayer.create(context, R.raw.gusanoxx);
        mediaPlayer.setSurface(new Surface(surfaceTexture));
        mediaPlayer.setLooping(true);
        mediaPlayer.setVolume(0f, 0f); // 🔇 sin sonido
        mediaPlayer.start();
    }

    @Override
    public void update(float deltaTime) {
        frameTimer += deltaTime;

        // Si han pasado más de 0.1 segundos, forzamos actualización del frame
        if (updateFrame || frameTimer > 0.1f) {
            surfaceTexture.updateTexImage();
            updateFrame = false;
            frameTimer = 0f;
        }

        if (!mediaPlayer.isPlaying()) {
            Log.w("VideoBackground", "⚠️ MediaPlayer se detuvo, reiniciando...");
            mediaPlayer.start();
        }
    }

    @Override
    public void draw() {
        GLES20.glUseProgram(program);

        float[] vertices = {
                -1f, -1f,
                1f, -1f,
                -1f,  1f,
                1f,  1f
        };

        float[] texCoords = {
                0f, 1f,
                1f, 1f,
                0f, 0f,
                1f, 0f
        };

        vertexBuffer = ShaderUtils.createFloatBuffer(vertices);
        texCoordBuffer = ShaderUtils.createFloatBuffer(texCoords);

        GLES20.glEnableVertexAttribArray(aPositionLocation);
        GLES20.glVertexAttribPointer(aPositionLocation, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(aTexCoordLocation);
        GLES20.glVertexAttribPointer(aTexCoordLocation, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(aPositionLocation);
        GLES20.glDisableVertexAttribArray(aTexCoordLocation);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        updateFrame = true;
    }
    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void resume() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    public void resumeVideo() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            Log.d("VideoBackground", "🎬 Video reanudado desde resume()");
        }
    }
}