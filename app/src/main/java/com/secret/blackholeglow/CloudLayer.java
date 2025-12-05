package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.util.ProceduralSphere;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   ☁️ CloudLayer - Capa de nubes animadas para la Tierra                  ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  • Esfera procedural ligeramente más grande que la Tierra                ║
 * ║  • Nubes procedurales con FBM (Fractal Brownian Motion)                  ║
 * ║  • Animación independiente de la rotación del planeta                    ║
 * ║  • Transparencia para ver el planeta debajo                              ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class CloudLayer extends BaseShaderProgram implements SceneObject, CameraAware {
    private static final String TAG = "CloudLayer";

    // Buffers de la esfera
    private final FloatBuffer vertexBuffer;
    private final FloatBuffer uvBuffer;
    private final ShortBuffer indexBuffer;
    private final int indexCount;

    // Uniform locations
    private int aPosLoc;
    private int aTexLoc;
    private int uMVPLoc;
    private int uTimeLoc;
    private int uAlphaLoc;

    // Camera
    private CameraController camera;

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // Transformaciones - seguir a la Tierra
    private float posX = 0f;
    private float posY = 1.8f;
    private float posZ = 0f;
    private float baseScale = 1.2f;
    private float cloudLayerScale = 1.08f; // 8% más grande que la Tierra

    // Tiempo para animación
    private float cloudTime = 0f;

    public CloudLayer(Context context) {
        super(context, "shaders/cloud_vertex.glsl", "shaders/cloud_fragment.glsl");

        Log.d(TAG, "═══════════════════════════════════════════════");
        Log.d(TAG, "☁️ Inicializando capa de nubes");
        Log.d(TAG, "═══════════════════════════════════════════════");

        // Generar esfera procedural
        ProceduralSphere.Mesh mesh = ProceduralSphere.generateOptimized(1.0f);

        this.vertexBuffer = mesh.vertexBuffer;
        this.uvBuffer = mesh.uvBuffer;
        this.indexBuffer = mesh.indexBuffer;
        this.indexCount = mesh.indexCount;

        // Obtener locations
        GLES20.glUseProgram(programId);
        aPosLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        aTexLoc = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        uMVPLoc = GLES20.glGetUniformLocation(programId, "u_MVP");
        uTimeLoc = GLES20.glGetUniformLocation(programId, "u_Time");
        uAlphaLoc = GLES20.glGetUniformLocation(programId, "u_Alpha");

        Log.d(TAG, "✅ CloudLayer inicializado");
        Log.d(TAG, "   Índices: " + indexCount);
        Log.d(TAG, "   aPosLoc: " + aPosLoc + ", aTexLoc: " + aTexLoc);
        Log.d(TAG, "═══════════════════════════════════════════════");
    }

    /**
     * Sincronizar posición con la Tierra
     */
    public void setPosition(float x, float y, float z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
    }

    /**
     * Sincronizar escala base con la Tierra
     */
    public void setBaseScale(float scale) {
        this.baseScale = scale;
    }

    /**
     * Ajustar cuánto más grande es la capa de nubes vs la Tierra
     */
    public void setCloudLayerScale(float scale) {
        this.cloudLayerScale = scale;
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    @Override
    public void update(float dt) {
        // Incrementar tiempo para animación de nubes
        cloudTime += dt;
    }

    @Override
    public void draw() {
        if (camera == null) return;

        GLES20.glUseProgram(programId);

        // Habilitar blending para transparencia
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Desactivar depth write (pero mantener depth test)
        GLES20.glDepthMask(false);

        // Desactivar culling para ver ambos lados
        GLES20.glDisable(GLES20.GL_CULL_FACE);

        // Construir matriz de modelo
        float finalScale = baseScale * cloudLayerScale;
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, posX, posY, posZ);
        Matrix.scaleM(modelMatrix, 0, finalScale, finalScale, finalScale);

        // Calcular MVP
        camera.computeMvp(modelMatrix, mvpMatrix);

        // Pasar uniforms
        GLES20.glUniformMatrix4fv(uMVPLoc, 1, false, mvpMatrix, 0);
        GLES20.glUniform1f(uTimeLoc, cloudTime);
        GLES20.glUniform1f(uAlphaLoc, 1.0f);

        // Configurar atributos
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        uvBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aTexLoc);
        GLES20.glVertexAttribPointer(aTexLoc, 2, GLES20.GL_FLOAT, false, 0, uvBuffer);

        // Dibujar
        indexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        // Limpiar
        GLES20.glDisableVertexAttribArray(aPosLoc);
        GLES20.glDisableVertexAttribArray(aTexLoc);

        // Restaurar estado
        GLES20.glDepthMask(true);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
    }
}
