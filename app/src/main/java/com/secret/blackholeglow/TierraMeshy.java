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
 * ║   🌍 TierraMeshy - Planeta Tierra realista generado con Meshy AI         ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  • Modelo 3D de alta calidad (3,000 triángulos)                          ║
 * ║  • Textura realista con océanos, continentes y nubes                     ║
 * ║  • Rotación configurable                                                 ║
 * ║  • Sistema de HP para daño de meteoritos                                 ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class TierraMeshy extends BaseShaderProgram implements SceneObject, CameraAware {
    private static final String TAG = "TierraMeshy";

    // Tiempo acumulado para animación de nubes
    private float cloudTime = 0f;

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
    private float posY = 0.5f;  // Posición Y por defecto de la Tierra
    private float posZ = 0f;
    private float scale = 1.0f;
    private float rotationY = 0f;
    private float spinSpeed = 2.0f;  // Rotación de la Tierra sobre su eje

    // 🌍 ÓRBITA ALREDEDOR DEL SOL
    private float orbitCenterX = 0f;  // Centro de órbita (posición del Sol)
    private float orbitCenterY = 0.5f;
    private float orbitCenterZ = -5.0f;
    private float orbitRadiusX = 1.8f;  // Radio horizontal
    private float orbitRadiusZ = 1.2f;  // Radio en profundidad (órbita elíptica)
    private float orbitSpeed = 0.15f;   // Velocidad de órbita
    private float orbitAngle = 0f;      // Ángulo actual de órbita

    // Sistema de HP
    private int maxHP = 100;
    private int currentHP = 100;
    private boolean isDead = false;

    // Callback para explosión
    private ExplosionCallback explosionCallback;

    public interface ExplosionCallback {
        void onExplosion(float x, float y, float z);
    }

    public TierraMeshy(Context context, TextureManager textureManager) {
        super(context, "shaders/tierra_fresnel_vertex.glsl", "shaders/tierra_fresnel_fragment.glsl");

        Log.d(TAG, "═══════════════════════════════════════════════");
        Log.d(TAG, "🌍 Cargando Tierra Meshy desde assets");
        Log.d(TAG, "═══════════════════════════════════════════════");

        // Cargar textura de la Tierra
        textureId = textureManager.getTexture(R.drawable.tierra_meshy);
        Log.d(TAG, "✓ Textura de la Tierra cargada - ID: " + textureId);

        // Cargar modelo OBJ
        ObjLoader.Mesh mesh = null;
        try {
            mesh = ObjLoader.loadObj(context, "TierraMeshy.obj");
            Log.d(TAG, "✅ Modelo cargado: " + mesh.vertexCount + " vértices, " + mesh.faces.size() + " caras");
        } catch (IOException e) {
            Log.e(TAG, "❌ Error cargando TierraMeshy.obj", e);
            throw new RuntimeException("No se pudo cargar el modelo de la Tierra", e);
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

        Log.d(TAG, "✅ Tierra Meshy inicializada correctamente");
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

    /**
     * 🌍 Configura la órbita de la Tierra alrededor del Sol
     * @param centerX Centro de órbita X (posición del Sol)
     * @param centerY Centro de órbita Y
     * @param centerZ Centro de órbita Z
     * @param radiusX Radio horizontal de órbita
     * @param radiusZ Radio en profundidad (órbita elíptica)
     * @param speed Velocidad de órbita (radianes/segundo)
     */
    public void setOrbit(float centerX, float centerY, float centerZ,
                         float radiusX, float radiusZ, float speed) {
        this.orbitCenterX = centerX;
        this.orbitCenterY = centerY;
        this.orbitCenterZ = centerZ;
        this.orbitRadiusX = radiusX;
        this.orbitRadiusZ = radiusZ;
        this.orbitSpeed = speed;
        Log.d(TAG, "🌍 Órbita configurada: centro(" + centerX + "," + centerY + "," + centerZ +
              ") radio(" + radiusX + "," + radiusZ + ") velocidad: " + speed);
    }

    public void setExplosionCallback(ExplosionCallback callback) {
        this.explosionCallback = callback;
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    @Override
    public void update(float dt) {
        if (isDead) return;

        // 🌍 ÓRBITA ALREDEDOR DEL SOL
        if (orbitSpeed > 0 && orbitRadiusX > 0) {
            orbitAngle += orbitSpeed * dt;
            if (orbitAngle > Math.PI * 2) orbitAngle -= Math.PI * 2;

            // Calcular nueva posición en órbita elíptica
            posX = orbitCenterX + orbitRadiusX * (float) Math.cos(orbitAngle);
            posZ = orbitCenterZ + orbitRadiusZ * (float) Math.sin(orbitAngle);
            // Y permanece constante (órbita horizontal)
        }

        // Rotación de la Tierra sobre su propio eje
        rotationY += spinSpeed * dt;
        if (rotationY > 360f) rotationY -= 360f;

        // Tiempo para animación de océanos
        cloudTime += dt;
    }

    @Override
    public void draw() {
        if (camera == null || isDead) return;

        GLES30.glUseProgram(programId);

        // 🔧 DESACTIVAR FACE CULLING - Dibujar ambos lados de las caras
        // Los modelos de Meshy pueden tener normales invertidas
        GLES30.glDisable(GLES30.GL_CULL_FACE);

        // Construir matriz de modelo
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, posX, posY, posZ);
        Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f);
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

        // Calcular MVP
        camera.computeMvp(modelMatrix, mvpMatrix);

        // Pasar MVP al shader
        int uMVPLoc = GLES30.glGetUniformLocation(programId, "u_MVP");
        GLES30.glUniformMatrix4fv(uMVPLoc, 1, false, mvpMatrix, 0);

        // Pasar tiempo para animación de nubes - directamente al uniform
        int uTimeLoc = GLES30.glGetUniformLocation(programId, "u_Time");
        GLES30.glUniform1f(uTimeLoc, cloudTime);

        // Alpha opaco
        int uAlphaLoc = GLES30.glGetUniformLocation(programId, "u_Alpha");
        GLES30.glUniform1f(uAlphaLoc, 1.0f);

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
    // 💔 SISTEMA DE HP
    // ═══════════════════════════════════════════════════════════

    public void damage(int amount) {
        if (isDead) return;

        currentHP -= amount;
        Log.d(TAG, "🌍💥 Tierra recibió " + amount + " de daño. HP: " + currentHP + "/" + maxHP);

        if (currentHP <= 0) {
            currentHP = 0;
            isDead = true;
            Log.d(TAG, "🌍💀 ¡LA TIERRA HA SIDO DESTRUIDA!");

            if (explosionCallback != null) {
                explosionCallback.onExplosion(posX, posY, posZ);
            }
        }
    }

    public void heal(int amount) {
        if (isDead) return;
        currentHP = Math.min(currentHP + amount, maxHP);
    }

    public void setMaxHP(int hp) {
        this.maxHP = hp;
        this.currentHP = hp;
    }

    public int getCurrentHealth() { return currentHP; }
    public int getMaxHealth() { return maxHP; }
    public boolean isDead() { return isDead; }

    // Getters de posición
    public float getX() { return posX; }
    public float getY() { return posY; }
    public float getZ() { return posZ; }
    public float getScale() { return scale; }
}
