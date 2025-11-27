package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES30;
import android.util.Log;

import com.secret.blackholeglow.gl3.ShaderProgram3;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║   ✨ StarryBackground - OpenGL ES 3.0 con VAO/VBO ✨              ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * Fondo de estrellas procedurales optimizado para OpenGL ES 3.0.
 * Usa VAO para guardar estado de atributos (menos llamadas GL por frame).
 */
public class StarryBackground implements SceneObject {
    private static final String TAG = "StarryBackground";

    // OpenGL ES 3.0
    private int vaoId;
    private int vboId;
    private ShaderProgram3 shader;

    // Texture
    private final int textureId;
    private final float timeOffset;

    // Cache para evitar cálculos repetidos
    private float cachedAspectRatio = 1.0f;

    public StarryBackground(Context context, TextureManager textureManager, int textureResourceId) {
        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║   ✨ STARRY BACKGROUND GL3.0 ✨        ║");
        Log.d(TAG, "╚════════════════════════════════════════╝");

        this.timeOffset = TimeManager.getTime();
        this.textureId = textureManager.getTexture(textureResourceId);

        // ═══ CREAR SHADER GLSL 300 es ═══
        shader = new ShaderProgram3(context,
                "shaders/gl3/starry_vertex.glsl",
                "shaders/gl3/starry_fragment.glsl");

        if (!shader.isValid()) {
            Log.e(TAG, "Error creando shader GL3, intentando fallback ES 2.0...");
            // Fallback a shaders ES 2.0 si falla
            shader = new ShaderProgram3(context,
                    "shaders/starry_vertex.glsl",
                    "shaders/starry_fragment.glsl");
        }

        // ═══ CREAR VAO ═══
        int[] vaoArray = new int[1];
        GLES30.glGenVertexArrays(1, vaoArray, 0);
        vaoId = vaoArray[0];
        GLES30.glBindVertexArray(vaoId);

        // ═══ CREAR VBO CON DATOS INTERLEAVED ═══
        // Layout: pos(2) + uv(2) = 4 floats por vértice
        float[] vertexData = {
            // Posición      // UV
            -1.0f, -1.0f,    0.0f, 1.0f,  // Bottom-left
             1.0f, -1.0f,    1.0f, 1.0f,  // Bottom-right
            -1.0f,  1.0f,    0.0f, 0.0f,  // Top-left
             1.0f,  1.0f,    1.0f, 0.0f   // Top-right
        };

        int[] vboArray = new int[1];
        GLES30.glGenBuffers(1, vboArray, 0);
        vboId = vboArray[0];

        FloatBuffer buffer = createFloatBuffer(vertexData);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,
                vertexData.length * 4, buffer, GLES30.GL_STATIC_DRAW);

        // ═══ CONFIGURAR ATRIBUTOS (se guarda en el VAO) ═══
        int stride = 4 * 4;  // 4 floats * 4 bytes

        // location 0: a_Position (vec2)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, stride, 0);
        GLES30.glEnableVertexAttribArray(0);

        // location 1: a_TexCoord (vec2)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, stride, 2 * 4);
        GLES30.glEnableVertexAttribArray(1);

        // Unbind
        GLES30.glBindVertexArray(0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);

        Log.d(TAG, "✓ VAO=" + vaoId + ", VBO=" + vboId + ", Texture=" + textureId);
        Log.d(TAG, "✓ Shader válido: " + shader.isValid());
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
        // Actualizar aspect ratio si cambió la pantalla
        float w = SceneRenderer.screenWidth;
        float h = SceneRenderer.screenHeight;
        if (h > 0) {
            cachedAspectRatio = w / h;
        }
    }

    @Override
    public void draw() {
        if (!shader.isValid()) {
            return;
        }

        // ═══ USAR SHADER ═══
        shader.use();

        // ═══ CONFIGURACIÓN DE SKYBOX ═══
        // Renderizar en Z=0.9999 (infinitamente lejos)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glDepthFunc(GLES30.GL_LEQUAL);
        GLES30.glDepthMask(false);  // No escribir en depth buffer

        // ═══ CONFIGURAR UNIFORMS ═══
        float time = (TimeManager.getTime() - timeOffset) % 100.0f;
        shader.setUniform("u_Time", time);
        shader.setUniform("u_Resolution", SceneRenderer.screenWidth, SceneRenderer.screenHeight);
        shader.setUniform("u_AspectRatio", cachedAspectRatio);

        // ═══ CONFIGURAR TEXTURA ═══
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        shader.setUniform("u_Texture", 0);

        // ═══ DIBUJAR CON VAO ═══
        // Solo 1 llamada para bind, el estado de atributos ya está guardado
        GLES30.glBindVertexArray(vaoId);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glBindVertexArray(0);

        // ═══ RESTAURAR ESTADO ═══
        GLES30.glDepthMask(true);
        GLES30.glDepthFunc(GLES30.GL_LESS);
    }

    /**
     * Libera recursos OpenGL
     */
    public void dispose() {
        if (vaoId != 0) {
            GLES30.glDeleteVertexArrays(1, new int[]{vaoId}, 0);
            vaoId = 0;
        }
        if (vboId != 0) {
            GLES30.glDeleteBuffers(1, new int[]{vboId}, 0);
            vboId = 0;
        }
        if (shader != null) {
            shader.dispose();
            shader = null;
        }
        Log.d(TAG, "Recursos liberados");
    }
}
