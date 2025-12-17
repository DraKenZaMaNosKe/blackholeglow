package com.secret.blackholeglow.christmas;

import android.content.Context;
import android.opengl.GLES30;
import android.util.Log;

import com.secret.blackholeglow.CameraAware;
import com.secret.blackholeglow.CameraController;
import com.secret.blackholeglow.R;
import com.secret.blackholeglow.SceneObject;
import com.secret.blackholeglow.TextureManager;
import com.secret.blackholeglow.TimeManager;
import com.secret.blackholeglow.gl3.ShaderProgram3;
import com.secret.blackholeglow.scenes.Disposable;
import com.secret.blackholeglow.systems.ScreenManager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                                                                           ║
 * ║   🎄 ChristmasBackground - Fondo de Bosque Navideño 🎄                   ║
 * ║                                                                           ║
 * ║   ❄️ "Un mágico bosque invernal con aurora boreal" ❄️                    ║
 * ║                                                                           ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                           ║
 * ║   CARACTERÍSTICAS:                                                        ║
 * ║   • OpenGL ES 3.0 con VAO/VBO                                            ║
 * ║   • Textura de bosque nevado con aurora boreal                           ║
 * ║   • Efectos de shader: aurora animada, brillo de luces                   ║
 * ║   • Renderizado en el far plane (skybox technique)                       ║
 * ║                                                                           ║
 * ║   SHADER EFFECTS:                                                         ║
 * ║   • Aurora boreal ondulante                                              ║
 * ║   • Luces del pueblo parpadeantes                                        ║
 * ║   • Vignette atmosférico                                                 ║
 * ║   • Tinte invernal (colores fríos)                                       ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 */
public class ChristmasBackground implements SceneObject, CameraAware, Disposable {
    private static final String TAG = "ChristmasBackground";

    // ═══════════════════════════════════════════════════════════════
    // 🎨 OPENGL RESOURCES
    // ═══════════════════════════════════════════════════════════════
    private int vaoId;
    private int vboId;
    private ShaderProgram3 shader;

    // ═══════════════════════════════════════════════════════════════
    // 🖼️ TEXTURA
    // ═══════════════════════════════════════════════════════════════
    private final int textureId;
    private final float timeOffset;

    // ═══════════════════════════════════════════════════════════════
    // 🎬 ESTADO
    // ═══════════════════════════════════════════════════════════════
    private CameraController camera;
    private float cachedAspectRatio = 1.0f;
    private boolean isDisposed = false;

    // ═══════════════════════════════════════════════════════════════
    // 🏗️ CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════

    public ChristmasBackground(Context context, TextureManager textureManager) {
        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║   🎄 CHRISTMAS BACKGROUND GL3.0 🎄     ║");
        Log.d(TAG, "╚════════════════════════════════════════╝");

        this.timeOffset = TimeManager.getTime();
        this.textureId = textureManager.getTexture(R.drawable.christmas_background);

        // ═══ CREAR SHADER GLSL 300 es ═══
        shader = new ShaderProgram3(context,
                "shaders/gl3/christmas_bg_vertex.glsl",
                "shaders/gl3/christmas_bg_fragment.glsl");

        if (!shader.isValid()) {
            Log.e(TAG, "Error creando shader GL3 para Christmas Background");
        }

        initVAO();

        Log.d(TAG, "✓ VAO=" + vaoId + ", VBO=" + vboId + ", Texture=" + textureId);
        Log.d(TAG, "✓ Shader válido: " + shader.isValid());
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔧 INICIALIZACIÓN VAO/VBO
    // ═══════════════════════════════════════════════════════════════

    private void initVAO() {
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
    }

    private FloatBuffer createFloatBuffer(float[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(data);
        fb.position(0);
        return fb;
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔄 UPDATE
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void update(float deltaTime) {
        // Actualizar aspect ratio si cambió la pantalla
        float w = ScreenManager.getWidth();
        float h = ScreenManager.getHeight();
        if (h > 0) {
            cachedAspectRatio = w / h;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 🎨 DRAW
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void draw() {
        if (isDisposed || !shader.isValid()) {
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
        shader.setUniform("u_Resolution", ScreenManager.getWidth(), ScreenManager.getHeight());
        shader.setUniform("u_AspectRatio", cachedAspectRatio);

        // ═══ CONFIGURAR TEXTURA ═══
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        shader.setUniform("u_Texture", 0);

        // ═══ DIBUJAR CON VAO ═══
        GLES30.glBindVertexArray(vaoId);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glBindVertexArray(0);

        // ═══ RESTAURAR ESTADO ═══
        GLES30.glDepthMask(true);
        GLES30.glDepthFunc(GLES30.GL_LESS);
    }

    // ═══════════════════════════════════════════════════════════════
    // 📷 CAMERA
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    // ═══════════════════════════════════════════════════════════════
    // 🗑️ DISPOSE
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void dispose() {
        if (isDisposed) return;
        isDisposed = true;

        Log.d(TAG, "🗑️ Liberando recursos de ChristmasBackground...");

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

        Log.d(TAG, "✓ ChristmasBackground liberado");
    }

    @Override
    public boolean isDisposed() {
        return isDisposed;
    }
}
