package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.os.SystemClock;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * UniverseBackground2D - Versión 2D pura sin perspectiva
 * Dibuja el fondo como un quad fullscreen en espacio de pantalla
 * Garantiza cobertura completa sin distorsión
 */
public class UniverseBackground2D implements SceneObject {
    private static final String TAG = "UniverseBackground2D";

    // Shader program
    private int programId;

    // Buffers
    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;

    // Uniform locations
    private int uTextureLoc;
    private int uTimeLoc;
    private int uResolutionLoc;
    private int uAspectRatioLoc;

    // Attribute locations
    private int aPositionLoc;
    private int aTexCoordLoc;

    // Texture
    private final int textureId;
    private final float timeOffset;
    private final Context context;

    public UniverseBackground2D(Context context, TextureManager textureManager, int textureResourceId) {
        Log.d(TAG, "Creating 2D Background");

        this.context = context;
        this.timeOffset = SystemClock.uptimeMillis() * 0.001f;
        this.textureId = textureManager.getTexture(textureResourceId);

        // Crear shader program desde archivos
        programId = ShaderUtils.createProgramFromAssets(context,
            "shaders/universe_background_vertex.glsl",
            "shaders/universe_background_fragment.glsl");

        if (programId == 0) {
            Log.e(TAG, "Error creating shader program for UniverseBackground2D");
            throw new RuntimeException("Error creating shader program");
        }

        // Obtener locations
        aPositionLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        aTexCoordLoc = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        uTextureLoc = GLES20.glGetUniformLocation(programId, "u_Texture");
        uTimeLoc = GLES20.glGetUniformLocation(programId, "u_Time");
        uResolutionLoc = GLES20.glGetUniformLocation(programId, "u_Resolution");
        uAspectRatioLoc = GLES20.glGetUniformLocation(programId, "u_AspectRatio");

        // Crear quad fullscreen (-1 a 1 en espacio NDC)
        float[] vertices = {
            -1.0f, -1.0f,  // Bottom left
             1.0f, -1.0f,  // Bottom right
            -1.0f,  1.0f,  // Top left
             1.0f,  1.0f   // Top right
        };

        // UV coordinates (0 a 1)
        float[] texCoords = {
            0.0f, 1.0f,  // Bottom left
            1.0f, 1.0f,  // Bottom right
            0.0f, 0.0f,  // Top left
            1.0f, 0.0f   // Top right
        };

        // Crear buffers
        vertexBuffer = createFloatBuffer(vertices);
        texCoordBuffer = createFloatBuffer(texCoords);

        Log.d(TAG, "2D Background initialized successfully");
    }

    private FloatBuffer createFloatBuffer(float[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(data);
        fb.position(0);
        return fb;
    }

    @Override
    public void update(float deltaTime) {
        // No animation in model space
    }

    @Override
    public void draw() {
        // Usar nuestro programa
        GLES20.glUseProgram(programId);

        // Desactivar depth test para fondo
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthMask(false);

        // Configurar uniforms
        float time = (SystemClock.uptimeMillis() * 0.001f - timeOffset) % 100.0f;
        GLES20.glUniform1f(uTimeLoc, time);

        float screenWidth = SceneRenderer.screenWidth;
        float screenHeight = SceneRenderer.screenHeight;
        GLES20.glUniform2f(uResolutionLoc, screenWidth, screenHeight);
        GLES20.glUniform1f(uAspectRatioLoc, screenWidth / screenHeight);

        // Configurar textura
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTextureLoc, 0);

        // Configurar atributos
        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(aTexCoordLoc);
        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

        // Dibujar como triangle strip
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Limpiar
        GLES20.glDisableVertexAttribArray(aPositionLoc);
        GLES20.glDisableVertexAttribArray(aTexCoordLoc);

        // Restaurar depth test
        GLES20.glDepthMask(true);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }
}