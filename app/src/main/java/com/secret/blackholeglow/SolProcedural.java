package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.systems.ScreenManager;
import com.secret.blackholeglow.util.ProceduralSphere;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * ☀️ SOL PROCEDURAL - VERSIÓN OPTIMIZADA
 *
 * Usa esfera procedural (576 triángulos) en lugar de modelo OBJ (7,936 triángulos)
 * Mantiene el shader de lava/plasma para el efecto visual
 *
 * Ahorro: 14x menos triángulos = mejor rendimiento
 */
public class SolProcedural extends BaseShaderProgram implements SceneObject, CameraAware {
    private static final String TAG = "SolProcedural";

    // Buffers de la esfera procedural
    private final FloatBuffer vertexBuffer;
    private final FloatBuffer uvBuffer;
    private final ShortBuffer indexBuffer;
    private final int indexCount;

    // Uniform locations
    private final int aPosLoc;
    private final int aTexLoc;
    private final int uTexLoc;

    // Textura
    private final int textureId;

    // Camera
    private CameraController camera;

    // Matrices reutilizables (evita allocaciones)
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // Transformaciones
    private float posX = 0f, posY = 0f, posZ = 0f;
    private float scale = 1.5f;
    private float rotation = 0f;
    private float spinSpeed = 10.0f;  // Rotación lenta del sol
    private float time = 0f;

    public SolProcedural(Context context, TextureManager textureManager) {
        // Usar el mismo shader de lava que SolRealista
        super(context, "shaders/sol_vertex.glsl", "shaders/sol_lava_fragment.glsl");

        Log.d(TAG, "═══════════════════════════════════════════════");
        Log.d(TAG, "☀️ Creando Sol PROCEDURAL (optimizado)");
        Log.d(TAG, "═══════════════════════════════════════════════");

        // Cargar textura del sol
        textureId = textureManager.getTexture(R.drawable.materialdelsol);

        // ⚡ Usar esfera procedural en lugar de modelo OBJ
        ProceduralSphere.Mesh mesh = ProceduralSphere.generateOptimized(1.0f);

        this.vertexBuffer = mesh.vertexBuffer;
        this.uvBuffer = mesh.uvBuffer;
        this.indexBuffer = mesh.indexBuffer;
        this.indexCount = mesh.indexCount;

        Log.d(TAG, "✅ Esfera procedural: " + mesh.vertexCount + " vértices, " + (mesh.indexCount / 3) + " triángulos");
        Log.d(TAG, "⚡ Ahorro: 7,936 → 576 triángulos (14x menos)");

        // Obtener attribute locations
        aPosLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        aTexLoc = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        uTexLoc = GLES20.glGetUniformLocation(programId, "u_Texture");

        Log.d(TAG, "☀️ Sol Procedural inicializado correctamente");
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
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
    public void update(float dt) {
        // Rotación lenta del sol
        rotation = (rotation + spinSpeed * dt) % 360f;
        time += dt;
    }

    @Override
    public void draw() {
        if (camera == null) {
            Log.e(TAG, "ERROR: Camera no asignada");
            return;
        }

        useProgram();

        // Configurar OpenGL
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Construir matriz modelo
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, posX, posY, posZ);
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);
        Matrix.rotateM(modelMatrix, 0, rotation, 0, 1, 0);

        // Calcular MVP
        camera.computeMvp(modelMatrix, mvpMatrix);

        // Enviar uniforms
        setTime(time);
        setMvpAndResolution(mvpMatrix, ScreenManager.getWidth(), ScreenManager.getHeight());

        // Configurar textura
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTexLoc, 0);

        // Configurar atributos de vértices
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        if (aTexLoc >= 0) {
            uvBuffer.position(0);
            GLES20.glEnableVertexAttribArray(aTexLoc);
            GLES20.glVertexAttribPointer(aTexLoc, 2, GLES20.GL_FLOAT, false, 0, uvBuffer);
        }

        // Dibujar con índices SHORT (no INT)
        indexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        // Limpiar
        GLES20.glDisableVertexAttribArray(aPosLoc);
        if (aTexLoc >= 0) {
            GLES20.glDisableVertexAttribArray(aTexLoc);
        }
    }
}
