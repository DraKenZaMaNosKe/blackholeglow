package com.secret.blackholeglow.christmas;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.BaseShaderProgram;
import com.secret.blackholeglow.CameraAware;
import com.secret.blackholeglow.CameraController;
import com.secret.blackholeglow.R;
import com.secret.blackholeglow.SceneObject;
import com.secret.blackholeglow.TextureManager;
import com.secret.blackholeglow.util.ObjLoader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   🎄 ChristmasTree - Árbol de Navidad con modelo de Meshy AI             ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  • Modelo 3D estilizado (~5,000 triángulos)                              ║
 * ║  • Textura con ornamentos integrados                                      ║
 * ║  • Animación de balanceo suave con el viento                              ║
 * ║  • Interactividad con touch                                               ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class ChristmasTree extends BaseShaderProgram implements SceneObject, CameraAware {
    private static final String TAG = "ChristmasTree";

    // Buffers del modelo
    private final FloatBuffer vertexBuffer;
    private final FloatBuffer uvBuffer;
    private final IntBuffer indexBuffer;
    private final int indexCount;

    // Uniform locations
    private final int aPosLoc;
    private final int aTexLoc;
    private final int uTexLoc;
    private final int uMVPLoc;
    private final int uTimeLoc;
    private final int uWindLoc;

    // Textura
    private final int textureId;

    // Camera
    private CameraController camera;

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // Transformaciones
    private float posX = 0f;
    private float posY = -0.8f;  // Posición Y para que esté en el suelo
    private float posZ = -2.0f;  // Profundidad en la escena
    private float scale = 0.8f;  // Escala del árbol
    private float rotationY = 0f;

    // Animación
    private float time = 0f;
    private float windStrength = 0.02f;  // Intensidad del balanceo
    private float windSpeed = 1.5f;       // Velocidad del viento

    // Touch interaction
    private float touchShake = 0f;
    private float touchShakeDecay = 0.95f;

    public ChristmasTree(Context context, TextureManager textureManager) {
        super(context,
              "shaders/gl3/christmas_tree_vertex.glsl",
              "shaders/gl3/christmas_tree_fragment.glsl");

        Log.d(TAG, "═══════════════════════════════════════════════");
        Log.d(TAG, "🎄 Cargando Árbol de Navidad (Meshy Pine)");
        Log.d(TAG, "═══════════════════════════════════════════════");

        // Cargar textura del árbol (modelo nuevo de Meshy)
        textureId = textureManager.getTexture(R.drawable.christmas_pine_texture);
        Log.d(TAG, "✓ Textura del árbol cargada - ID: " + textureId);

        // Cargar modelo OBJ (9,927 triángulos - Meshy AI)
        ObjLoader.Mesh mesh = null;
        try {
            // ✅ flipV=true para modelos de Blender (UV invertido)
            mesh = ObjLoader.loadObj(context, "christmas_pine.obj", true);
            Log.d(TAG, "✅ Modelo cargado: " + mesh.vertexCount + " vértices, " + mesh.faces.size() + " caras");
        } catch (IOException e) {
            Log.e(TAG, "❌ Error cargando christmas_pine.obj", e);
            throw new RuntimeException("No se pudo cargar el modelo del árbol", e);
        }

        // Usar los buffers del ObjLoader
        this.vertexBuffer = mesh.vertexBuffer;
        this.uvBuffer = mesh.uvBuffer;

        // Construir buffer de índices con fan triangulation
        int totalIndices = 0;
        for (int[] face : mesh.faces) {
            totalIndices += (face.length - 2) * 3;
        }

        int[] indices = new int[totalIndices];
        int idx = 0;
        for (int[] face : mesh.faces) {
            int v0 = face[0];
            for (int i = 1; i < face.length - 1; i++) {
                indices[idx++] = v0;
                indices[idx++] = face[i];
                indices[idx++] = face[i + 1];
            }
        }

        this.indexCount = totalIndices;

        // Crear IntBuffer para los índices
        ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 4);
        ibb.order(ByteOrder.nativeOrder());
        indexBuffer = ibb.asIntBuffer();
        indexBuffer.put(indices);
        indexBuffer.position(0);

        // Obtener uniform locations
        aPosLoc = GLES30.glGetAttribLocation(programId, "a_Position");
        aTexLoc = GLES30.glGetAttribLocation(programId, "a_TexCoord");
        uTexLoc = GLES30.glGetUniformLocation(programId, "u_Texture");
        uMVPLoc = GLES30.glGetUniformLocation(programId, "u_MVP");
        uTimeLoc = GLES30.glGetUniformLocation(programId, "u_Time");
        uWindLoc = GLES30.glGetUniformLocation(programId, "u_Wind");

        Log.d(TAG, "✅ Árbol de Navidad inicializado correctamente");
        Log.d(TAG, "   Vértices: " + mesh.vertexCount);
        Log.d(TAG, "   Índices: " + indexCount);
        Log.d(TAG, "   Triángulos: " + (indexCount / 3));
        Log.d(TAG, "═══════════════════════════════════════════════");
    }

    // ═══════════════════════════════════════════════════════════
    // 🔧 CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════

    public void setPosition(float x, float y, float z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setRotationY(float degrees) {
        this.rotationY = degrees;
    }

    public void setWindStrength(float strength) {
        this.windStrength = strength;
    }

    public void setWindSpeed(float speed) {
        this.windSpeed = speed;
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    // ═══════════════════════════════════════════════════════════
    // 🎮 INTERACTIVIDAD
    // ═══════════════════════════════════════════════════════════

    /**
     * Sacude el árbol (cuando el usuario lo toca)
     */
    public void shake(float intensity) {
        touchShake = Math.min(touchShake + intensity, 0.3f);
        Log.d(TAG, "🎄 Árbol sacudido! Intensidad: " + touchShake);
    }

    // ═══════════════════════════════════════════════════════════
    // 🔄 UPDATE
    // ═══════════════════════════════════════════════════════════

    @Override
    public void update(float dt) {
        time += dt;

        // Decay del shake
        if (touchShake > 0.001f) {
            touchShake *= touchShakeDecay;
        } else {
            touchShake = 0f;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 🎨 DRAW
    // ═══════════════════════════════════════════════════════════

    private static int drawCallCount = 0;

    @Override
    public void draw() {
        if (camera == null) {
            Log.w(TAG, "draw() - camera is NULL!");
            return;
        }

        // Debug: Log cada 100 frames
        if (drawCallCount++ % 100 == 0) {
            Log.d(TAG, "draw() called #" + drawCallCount +
                  " | aPosLoc=" + aPosLoc +
                  " | aTexLoc=" + aTexLoc +
                  " | textureId=" + textureId +
                  " | indexCount=" + indexCount);
        }

        GLES30.glUseProgram(programId);

        // 🔧 DESACTIVAR FACE CULLING - Los modelos de Meshy pueden tener normales invertidas
        GLES30.glDisable(GLES30.GL_CULL_FACE);

        // Calcular balanceo del viento
        float windOffset = (float) Math.sin(time * windSpeed) * windStrength;
        float totalSwing = windOffset + touchShake * (float) Math.sin(time * 15f);

        // Construir matriz de modelo
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, posX, posY, posZ);
        Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f);
        // Pequeña inclinación para el efecto de viento
        Matrix.rotateM(modelMatrix, 0, totalSwing * 5f, 0f, 0f, 1f);
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

        // Calcular MVP
        camera.computeMvp(modelMatrix, mvpMatrix);

        // Pasar uniforms al shader
        GLES30.glUniformMatrix4fv(uMVPLoc, 1, false, mvpMatrix, 0);
        GLES30.glUniform1f(uTimeLoc, time);
        GLES30.glUniform1f(uWindLoc, totalSwing);

        // Activar textura
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(uTexLoc, 0);

        // Configurar atributos
        GLES30.glEnableVertexAttribArray(aPosLoc);
        GLES30.glVertexAttribPointer(aPosLoc, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        GLES30.glEnableVertexAttribArray(aTexLoc);
        GLES30.glVertexAttribPointer(aTexLoc, 2, GLES30.GL_FLOAT, false, 0, uvBuffer);

        // Dibujar
        indexBuffer.position(0);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, indexBuffer);

        // Limpiar
        GLES30.glDisableVertexAttribArray(aPosLoc);
        GLES30.glDisableVertexAttribArray(aTexLoc);

        // Restaurar culling para otros objetos
        GLES30.glEnable(GLES30.GL_CULL_FACE);
    }

    // ═══════════════════════════════════════════════════════════
    // 🗑️ CLEANUP
    // ═══════════════════════════════════════════════════════════

    public void cleanup() {
        Log.d(TAG, "🗑️ Liberando recursos de ChristmasTree");
        // Los buffers se liberan automáticamente por el GC
        // La textura se libera por TextureManager
    }

    // ═══════════════════════════════════════════════════════════
    // 📊 GETTERS
    // ═══════════════════════════════════════════════════════════

    public float getX() { return posX; }
    public float getY() { return posY; }
    public float getZ() { return posZ; }
    public float getScale() { return scale; }
}
