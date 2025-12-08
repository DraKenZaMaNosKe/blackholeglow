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

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   🪐 SaturnoMeshy - Planeta Saturno con Anillos (Meshy AI)               ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  • Modelo 3D con anillos icónicos                                        ║
 * ║  • Órbita en sentido CONTRARIO a la Tierra                               ║
 * ║  • Rotación sobre su eje con inclinación característica                  ║
 * ║  • Textura realista con bandas de gas                                    ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class SaturnoMeshy extends BaseShaderProgram implements SceneObject, CameraAware {
    private static final String TAG = "SaturnoMeshy";

    // Buffers del modelo
    private FloatBuffer vertexBuffer;
    private FloatBuffer uvBuffer;
    private IntBuffer indexBuffer;
    private int indexCount;

    // Uniform locations
    private int aPosLoc;
    private int aTexLoc;
    private int uTexLoc;

    // Textura
    private int textureId;

    // Camera
    private CameraController camera;

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // Transformaciones
    private float posX = 0f;
    private float posY = 0f;
    private float posZ = 0f;
    private float scale = 0.6f;  // Saturno más pequeño que el sol
    private float rotationY = 0f;
    private float spinSpeed = 4.0f;  // Saturno rota rápido

    // Inclinación característica de Saturno (26.7 grados)
    private float tiltAngle = 26.7f;

    // 🪐 ÓRBITA EN SENTIDO CONTRARIO A LA TIERRA
    private float orbitCenterX = 0f;
    private float orbitCenterY = 0.5f;
    private float orbitCenterZ = -5.0f;
    private float orbitRadiusX = 2.5f;   // Órbita más grande que la Tierra
    private float orbitRadiusZ = 1.8f;
    private float orbitSpeed = -0.08f;   // NEGATIVO = sentido contrario
    private float orbitAngle = 90f;      // Empieza a un lado (visible)

    private boolean modelLoaded = false;

    // Debug: contador para evitar spam de logs
    private int drawCallCount = 0;

    public SaturnoMeshy(Context context, TextureManager textureManager) {
        super(context, "shaders/saturno_vertex.glsl", "shaders/saturno_fragment.glsl");

        Log.d(TAG, "═══════════════════════════════════════════════");
        Log.d(TAG, "🪐 Cargando Saturno Meshy desde assets");
        Log.d(TAG, "═══════════════════════════════════════════════");

        // Cargar textura de Saturno desde Meshy AI
        textureId = textureManager.getTexture(R.drawable.saturno_meshy);
        Log.d(TAG, "🪐 Textura de Saturno cargada - ID: " + textureId);

        // Cargar modelo OBJ
        try {
            ObjLoader.Mesh mesh = ObjLoader.loadObj(context, "SaturnoMeshy.obj");
            Log.d(TAG, "✅ Modelo cargado: " + mesh.vertexCount + " vértices, " + mesh.faces.size() + " caras");

            this.vertexBuffer = mesh.vertexBuffer;
            this.uvBuffer = mesh.uvBuffer;

            // Construir buffer de índices
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

            ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 4);
            ibb.order(ByteOrder.nativeOrder());
            indexBuffer = ibb.asIntBuffer();
            indexBuffer.put(indices);
            indexBuffer.position(0);

            modelLoaded = true;
            Log.d(TAG, "✅ Saturno Meshy inicializado correctamente");
        } catch (IOException e) {
            Log.e(TAG, "⚠️ SaturnoMeshy.obj no encontrado - Saturno deshabilitado", e);
            modelLoaded = false;
        }

        // Obtener uniform locations
        aPosLoc = GLES30.glGetAttribLocation(programId, "a_Position");
        aTexLoc = GLES30.glGetAttribLocation(programId, "a_TexCoord");
        uTexLoc = GLES30.glGetUniformLocation(programId, "u_Texture");

        Log.d(TAG, "🔧 Shader locations: aPosLoc=" + aPosLoc + " aTexLoc=" + aTexLoc + " uTexLoc=" + uTexLoc);
        Log.d(TAG, "═══════════════════════════════════════════════");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════════════════

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

    public void setOrbitCenter(float x, float y, float z) {
        this.orbitCenterX = x;
        this.orbitCenterY = y;
        this.orbitCenterZ = z;
    }

    public void setOrbitRadius(float radiusX, float radiusZ) {
        this.orbitRadiusX = radiusX;
        this.orbitRadiusZ = radiusZ;
    }

    public void setOrbitSpeed(float speed) {
        // Negativo para órbita en sentido contrario
        this.orbitSpeed = -Math.abs(speed);
    }

    public void setTiltAngle(float angle) {
        this.tiltAngle = angle;
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE - Órbita y rotación
    // ═══════════════════════════════════════════════════════════════════════════

    // Contador para debug logging
    private int updateCount = 0;

    @Override
    public void update(float deltaTime) {
        if (!modelLoaded) return;

        // Debug: Log cada 120 frames (~4 segundos a 30fps)
        updateCount++;
        if (updateCount % 120 == 0) {
            Log.d(TAG, "🪐 UPDATE #" + updateCount + " rotY=" + rotationY + " orbitAngle=" + orbitAngle +
                  " pos(" + posX + ", " + posY + ", " + posZ + ")");
        }

        // Rotación sobre su propio eje (MÁS RÁPIDO)
        rotationY += spinSpeed * deltaTime;
        if (rotationY > 360f) rotationY -= 360f;

        // Órbita alrededor del centro (sentido contrario) - Majestuoso
        orbitAngle += orbitSpeed * deltaTime * 20f;  // Velocidad cinematográfica
        if (orbitAngle < 0) orbitAngle += 360f;
        if (orbitAngle > 360f) orbitAngle -= 360f;

        // Calcular posición en órbita elíptica
        float angleRad = (float) Math.toRadians(orbitAngle);
        posX = orbitCenterX + orbitRadiusX * (float) Math.cos(angleRad);
        posZ = orbitCenterZ + orbitRadiusZ * (float) Math.sin(angleRad);
        posY = orbitCenterY;  // Mantener altura constante
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DRAW
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void draw() {
        if (!modelLoaded || camera == null) return;

        GLES30.glUseProgram(programId);

        // Construir matriz de modelo
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, posX, posY, posZ);

        // Inclinación característica de Saturno
        Matrix.rotateM(modelMatrix, 0, tiltAngle, 0f, 0f, 1f);

        // Rotación sobre su eje
        Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f);

        // Escala
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

        // Calcular MVP usando CameraController
        camera.computeMvp(modelMatrix, mvpMatrix);

        // Pasar MVP al shader
        int uMVPLoc = GLES30.glGetUniformLocation(programId, "u_MVP");
        GLES30.glUniformMatrix4fv(uMVPLoc, 1, false, mvpMatrix, 0);

        // 🔧 FIX: Pasar u_Alpha y u_Time requeridos por el shader
        int uAlphaLoc = GLES30.glGetUniformLocation(programId, "u_Alpha");
        GLES30.glUniform1f(uAlphaLoc, 1.0f);  // Totalmente visible

        int uTimeLoc = GLES30.glGetUniformLocation(programId, "u_Time");
        GLES30.glUniform1f(uTimeLoc, rotationY * 0.01f);  // Usar rotación como tiempo

        // Textura
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(uTexLoc, 0);

        // Configurar atributos
        vertexBuffer.position(0);
        GLES30.glEnableVertexAttribArray(aPosLoc);
        GLES30.glVertexAttribPointer(aPosLoc, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        uvBuffer.position(0);
        GLES30.glEnableVertexAttribArray(aTexLoc);
        GLES30.glVertexAttribPointer(aTexLoc, 2, GLES30.GL_FLOAT, false, 0, uvBuffer);

        // Dibujar
        indexBuffer.position(0);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, indexBuffer);

        // Limpiar
        GLES30.glDisableVertexAttribArray(aPosLoc);
        GLES30.glDisableVertexAttribArray(aTexLoc);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════════════════

    public float getX() { return posX; }
    public float getY() { return posY; }
    public float getZ() { return posZ; }
    public float getScale() { return scale; }
    public boolean isModelLoaded() { return modelLoaded; }
}
