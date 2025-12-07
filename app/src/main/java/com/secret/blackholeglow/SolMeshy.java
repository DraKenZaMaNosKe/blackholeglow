package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.util.ObjLoader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   ☀️ SolMeshy - Sol realista generado con Meshy AI                       ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  • Modelo 3D de alta calidad (3,000 triángulos)                          ║
 * ║  • Textura realista con manchas solares                                  ║
 * ║  • Rotación lenta para efecto de estrella viva                           ║
 * ║  • Escala y posición configurable                                        ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class SolMeshy extends BaseShaderProgram implements SceneObject, CameraAware {
    private static final String TAG = "SolMeshy";

    // Buffers del modelo
    private final FloatBuffer vertexBuffer;
    private final FloatBuffer uvBuffer;
    private final IntBuffer indexBuffer;
    private final int indexCount;

    // Uniform locations
    private final int aPosLoc;
    private final int aTexLoc;
    private final int uTexLoc;

    // Textura
    private final int textureId;

    // Camera
    private CameraController camera;

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // Transformaciones
    private float posX = 0f;
    private float posY = 0f;
    private float posZ = 0f;
    private float scale = 1.0f;
    private float rotationY = 0f;
    private float spinSpeed = 0.5f;  // Rotación muy lenta del sol (realista)

    // Tiempo acumulado para efectos
    private float time = 0f;

    public SolMeshy(Context context, TextureManager textureManager) {
        // ☀️ Usar shaders de PLASMA con distorsión de calor
        super(context, "shaders/sol_plasma_vertex.glsl", "shaders/sol_plasma_fragment.glsl");

        Log.d(TAG, "═══════════════════════════════════════════════");
        Log.d(TAG, "☀️ Cargando Sol Meshy desde assets");
        Log.d(TAG, "═══════════════════════════════════════════════");

        // Cargar textura del sol
        textureId = textureManager.getTexture(R.drawable.sol_meshy);
        Log.d(TAG, "✓ Textura del sol cargada - ID: " + textureId);

        // Cargar modelo OBJ
        ObjLoader.Mesh mesh = null;
        try {
            mesh = ObjLoader.loadObj(context, "SolMeshy.obj");
            Log.d(TAG, "✅ Modelo cargado: " + mesh.vertexCount + " vértices, " + mesh.faces.size() + " caras");
        } catch (IOException e) {
            Log.e(TAG, "❌ Error cargando SolMeshy.obj", e);
            throw new RuntimeException("No se pudo cargar el modelo del sol", e);
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
        aPosLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        aTexLoc = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        uTexLoc = GLES20.glGetUniformLocation(programId, "u_Texture");

        Log.d(TAG, "✅ Sol Meshy inicializado correctamente");
        Log.d(TAG, "   Vértices: " + mesh.vertexCount);
        Log.d(TAG, "   Índices: " + indexCount);
        Log.d(TAG, "═══════════════════════════════════════════════");
    }

    public void setPosition(float x, float y, float z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setSpinSpeed(float speed) {
        this.spinSpeed = speed;
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    private static final float TIME_WRAP = 1000f;

    @Override
    public void update(float dt) {
        // Rotación lenta del sol
        rotationY += spinSpeed * dt;
        if (rotationY > 360f) rotationY -= 360f;

        // Acumular tiempo para efectos de plasma
        time += dt;
        if (time > TIME_WRAP) time -= TIME_WRAP;
    }

    @Override
    public void draw() {
        if (camera == null) return;

        GLES20.glUseProgram(programId);

        // 🔧 DESACTIVAR FACE CULLING - Dibujar ambos lados de las caras
        GLES20.glDisable(GLES20.GL_CULL_FACE);

        // Construir matriz de modelo
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, posX, posY, posZ);
        Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f);
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

        // Calcular MVP
        camera.computeMvp(modelMatrix, mvpMatrix);

        // Pasar MVP al shader
        int uMVPLoc = GLES20.glGetUniformLocation(programId, "u_MVP");
        GLES20.glUniformMatrix4fv(uMVPLoc, 1, false, mvpMatrix, 0);

        // Pasar tiempo para efectos de plasma
        int uTimeLoc = GLES20.glGetUniformLocation(programId, "u_Time");
        GLES20.glUniform1f(uTimeLoc, time);

        // Alpha opaco
        int uAlphaLoc = GLES20.glGetUniformLocation(programId, "u_Alpha");
        GLES20.glUniform1f(uAlphaLoc, 1.0f);

        // Activar textura
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTexLoc, 0);

        // Configurar atributos
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(aTexLoc);
        GLES20.glVertexAttribPointer(aTexLoc, 2, GLES20.GL_FLOAT, false, 0, uvBuffer);

        // Dibujar
        indexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_INT, indexBuffer);

        // Limpiar
        GLES20.glDisableVertexAttribArray(aPosLoc);
        GLES20.glDisableVertexAttribArray(aTexLoc);

        // Restaurar culling
        GLES20.glEnable(GLES20.GL_CULL_FACE);
    }

    // Getters
    public float getX() { return posX; }
    public float getY() { return posY; }
    public float getZ() { return posZ; }
    public float getScale() { return scale; }
}
