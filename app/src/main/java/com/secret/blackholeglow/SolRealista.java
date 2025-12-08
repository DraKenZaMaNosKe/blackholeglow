package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.util.ObjLoader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

/**
 * ☀️ SOL REALISTA
 * Carga el modelo 3D detallado del Sol desde assets
 */
public class SolRealista extends BaseShaderProgram implements SceneObject, CameraAware {
    private static final String TAG = "SolRealista";

    // Buffers
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
    private float posX = 0f, posY = 0f, posZ = 0f;
    private float scale = 1.5f;
    private float rotation = 0f;
    private float spinSpeed = -1.0f;

    public SolRealista(Context context, TextureManager textureManager) {
        super(context, "shaders/sol_vertex.glsl", "shaders/sol_lava_fragment.glsl");

        Log.d(TAG, "═══════════════════════════════════════════════");
        Log.d(TAG, "☀️ Cargando Sol Realista desde assets");
        Log.d(TAG, "═══════════════════════════════════════════════");

        // Cargar textura
        textureId = textureManager.getTexture(R.drawable.materialdelsol);

        // Cargar modelo OBJ
        ObjLoader.Mesh mesh = null;
        try {
            mesh = ObjLoader.loadObj(context, "Solrealista.obj");
            Log.d(TAG, "✅ Modelo cargado: " + mesh.vertexCount + " vértices, " + mesh.faces.size() + " caras");
        } catch (IOException e) {
            Log.e(TAG, "❌ Error cargando Solrealista.obj", e);
            throw new RuntimeException("No se pudo cargar el modelo del Sol", e);
        }

        // Usar los buffers ya preparados por ObjLoader
        this.vertexBuffer = mesh.vertexBuffer;
        this.uvBuffer = mesh.uvBuffer;

        // Construir buffer de índices desde las caras usando fan triangulation
        int totalIndices = 0;
        for (int[] face : mesh.faces) {
            // Cada cara con N vértices produce N-2 triángulos
            totalIndices += (face.length - 2) * 3;
        }

        int[] indices = new int[totalIndices];
        int idx = 0;
        for (int[] face : mesh.faces) {
            // Fan triangulation: (v0, v1, v2), (v0, v2, v3), (v0, v3, v4), ...
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

        Log.d(TAG, "✅ Sol Realista inicializado correctamente");
        Log.d(TAG, "   Vértices: " + mesh.vertexCount);
        Log.d(TAG, "   Caras: " + mesh.faces.size());
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

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    @Override
    public void update(float dt) {
        // Rotación lenta
        rotation += spinSpeed * dt;
        if (rotation > 360f) rotation -= 360f;
    }

    @Override
    public void draw() {
        if (camera == null) {
            Log.w(TAG, "⚠️ Camera no asignada");
            return;
        }

        GLES30.glUseProgram(programId);

        // Construir matriz de modelo
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, posX, posY, posZ);
        Matrix.rotateM(modelMatrix, 0, rotation, 0f, 1f, 0f);  // Rotación Y
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

        // Calcular MVP
        camera.computeMvp(modelMatrix, mvpMatrix);

        // Pasar MVP al vertex shader
        int uMVPLoc = GLES30.glGetUniformLocation(programId, "u_MVP");
        GLES30.glUniformMatrix4fv(uMVPLoc, 1, false, mvpMatrix, 0);

        // Pasar tiempo
        setTime((System.currentTimeMillis() % 60000) / 1000.0f);

        // Activar textura
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(uTexLoc, 0);

        // Configurar atributos
        GLES30.glEnableVertexAttribArray(aPosLoc);
        GLES30.glVertexAttribPointer(aPosLoc, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        GLES30.glEnableVertexAttribArray(aTexLoc);
        GLES30.glVertexAttribPointer(aTexLoc, 2, GLES30.GL_FLOAT, false, 0, uvBuffer);

        // Dibujar con índices (usa int en lugar de short porque el modelo tiene >32k vértices)
        indexBuffer.position(0);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, indexBuffer);

        // Limpiar
        GLES30.glDisableVertexAttribArray(aPosLoc);
        GLES30.glDisableVertexAttribArray(aTexLoc);
    }
}
