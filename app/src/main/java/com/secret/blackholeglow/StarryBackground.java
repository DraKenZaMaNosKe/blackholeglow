package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.os.SystemClock;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * StarryBackground - Fondo de estrellas procedurales
 * Dibuja estrellas brillantes usando shaders sin necesidad de geometría 3D
 * Se renderiza como un quad fullscreen en espacio de pantalla
 */
public class StarryBackground implements SceneObject {
    private static final String TAG = "depurar";

    // Shader program
    private int programId;

    // Buffers
    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;

    // Uniform locations
    private int uTimeLoc;
    private int uResolutionLoc;
    private int uAspectRatioLoc;
    private int uTextureLoc;

    // Attribute locations
    private int aPositionLoc;
    private int aTexCoordLoc;

    // Texture
    private final int textureId;
    private final float timeOffset;
    private final Context context;

    public StarryBackground(Context context, TextureManager textureManager, int textureResourceId) {
        Log.d(TAG, "[StarryBackground] ========================================");
        Log.d(TAG, "[StarryBackground] INICIANDO CREACIÓN DE FONDO ESTRELLADO");
        Log.d(TAG, "[StarryBackground] ========================================");

        this.context = context;
        this.timeOffset = SystemClock.uptimeMillis() * 0.001f;
        this.textureId = textureManager.getTexture(textureResourceId);

        // Crear shader program desde archivos
        programId = ShaderUtils.createProgramFromAssets(context,
            "shaders/starry_vertex.glsl",
            "shaders/starry_fragment.glsl");

        if (programId == 0) {
            Log.e(TAG, "[StarryBackground] ✗✗✗ ERROR: Shader NO se creó!");

            // Intentar obtener log de errores
            int[] shaders = new int[2];
            GLES20.glGetAttachedShaders(programId, 2, null, 0, shaders, 0);

            throw new RuntimeException("Error creating shader program");
        }

        Log.d(TAG, "[StarryBackground] ✓ Shader creado, programId=" + programId);

        // Verificar que el programa está linkeado correctamente
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            String log = GLES20.glGetProgramInfoLog(programId);
            Log.e(TAG, "[StarryBackground] ✗ Shader link error: " + log);
        } else {
            Log.d(TAG, "[StarryBackground] ✓ Shader linkeado correctamente");
        }

        // Obtener locations
        aPositionLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        aTexCoordLoc = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        uTimeLoc = GLES20.glGetUniformLocation(programId, "u_Time");
        uResolutionLoc = GLES20.glGetUniformLocation(programId, "u_Resolution");
        uAspectRatioLoc = GLES20.glGetUniformLocation(programId, "u_AspectRatio");
        uTextureLoc = GLES20.glGetUniformLocation(programId, "u_Texture");

        Log.d(TAG, "[StarryBackground] Shader locations - Pos:" + aPositionLoc +
                   " Tex:" + aTexCoordLoc + " Time:" + uTimeLoc +
                   " Res:" + uResolutionLoc + " Aspect:" + uAspectRatioLoc +
                   " Texture:" + uTextureLoc);

        // Verificar locations críticos
        if (aPositionLoc == -1) Log.e(TAG, "[StarryBackground] ✗ a_Position NO encontrado!");
        if (aTexCoordLoc == -1) Log.e(TAG, "[StarryBackground] ✗ a_TexCoord NO encontrado!");
        if (uTimeLoc == -1) Log.w(TAG, "[StarryBackground] ⚠ u_Time NO encontrado!");
        if (uResolutionLoc == -1) Log.w(TAG, "[StarryBackground] ⚠ u_Resolution NO encontrado!");
        if (uAspectRatioLoc == -1) Log.w(TAG, "[StarryBackground] ⚠ u_AspectRatio NO encontrado!");

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

        Log.d(TAG, "[StarryBackground] ✓ Buffers creados");
        Log.d(TAG, "[StarryBackground] ========================================");
        Log.d(TAG, "[StarryBackground] ✓✓✓ FONDO ESTRELLADO INICIALIZADO");
        Log.d(TAG, "[StarryBackground] ========================================");
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

    private static int drawCallCount = 0;

    @Override
    public void draw() {
        drawCallCount++;

        if (drawCallCount % 300 == 0) {
            Log.d(TAG, "[StarryBackground] ========================================");
            Log.d(TAG, "[StarryBackground] draw() llamado, frame #" + drawCallCount);
            Log.d(TAG, "[StarryBackground] programId=" + programId);
        }

        // Verificar si el shader es válido
        if (!GLES20.glIsProgram(programId)) {
            if (drawCallCount % 60 == 0) {
                Log.e(TAG, "[StarryBackground] ✗ programId no es válido!");
            }
            return;
        }

        // Usar nuestro programa
        GLES20.glUseProgram(programId);

        // Desactivar depth test para fondo (se dibuja primero, detrás de todo)
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

        if (drawCallCount % 300 == 0) {
            Log.d(TAG, "[StarryBackground] ✓ Uniforms configurados - time:" + time +
                       " resolution:" + screenWidth + "x" + screenHeight +
                       " texture:" + textureId);
        }

        // Configurar atributos
        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(aTexCoordLoc);
        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

        // Dibujar como triangle strip
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Verificar errores GL
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR && drawCallCount % 300 == 0) {
            Log.e(TAG, "[StarryBackground] ✗ Error GL: " + error);
        }

        // Limpiar
        GLES20.glDisableVertexAttribArray(aPositionLoc);
        GLES20.glDisableVertexAttribArray(aTexCoordLoc);

        // Restaurar depth test para los objetos 3D
        GLES20.glDepthMask(true);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        if (drawCallCount % 300 == 0) {
            Log.d(TAG, "[StarryBackground] ✓ Frame completado");
            Log.d(TAG, "[StarryBackground] ========================================");
        }
    }
}
