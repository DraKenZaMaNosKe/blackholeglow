// Spaceship.java - Nave espacial base para batalla automática
package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Nave espacial con HP, movimiento automático y disparo.
 * Renderiza un billboard (quad) con textura.
 */
public class Spaceship extends BaseShaderProgram implements SceneObject, CameraAware {
    private static final String TAG = "Spaceship";

    // Tipo de nave
    public enum ShipType {
        PLAYER,    // Nave del jugador (azul)
        ENEMY      // Nave enemiga (roja)
    }

    // Configuración
    private final Context context;
    private final TextureLoader textureLoader;
    private final int textureResourceId;
    private final ShipType shipType;

    // Propiedades físicas
    public float x, y, z;              // Posición
    public float vx, vy;               // Velocidad
    public float size;                 // Tamaño del billboard
    public float collisionRadius;      // Radio para colisiones

    // Sistema de vida
    public int maxHealth;
    public int currentHealth;
    public boolean isDead = false;

    // Sistema de disparo
    public float fireRate = 0.5f;      // Disparos por segundo
    private float fireTimer = 0f;

    // Uniform locations
    private final int aPosLoc, aTexLoc;
    private final int uTexLoc, uTintColorLoc, uAlphaLoc;

    // Buffers del billboard (quad)
    private final FloatBuffer vertexBuffer;
    private final FloatBuffer texCoordBuffer;
    private final ShortBuffer indexBuffer;

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // Camera
    private CameraController camera;

    // Color de tinte (para efectos de daño)
    private float[] tintColor = {1f, 1f, 1f, 0f}; // RGBA - por defecto sin tinte
    private float alpha = 1.0f;

    // Geometría del billboard (quad con centro en origen)
    private static final float[] VERTICES = {
        -0.5f,  0.5f, 0f,  // Top-left
        -0.5f, -0.5f, 0f,  // Bottom-left
         0.5f, -0.5f, 0f,  // Bottom-right
         0.5f,  0.5f, 0f   // Top-right
    };

    private static final float[] TEX_COORDS = {
        0f, 0f,  // Top-left
        0f, 1f,  // Bottom-left
        1f, 1f,  // Bottom-right
        1f, 0f   // Top-right
    };

    private static final short[] INDICES = {
        0, 1, 2,  // Primer triángulo
        0, 2, 3   // Segundo triángulo
    };

    /**
     * Constructor
     */
    public Spaceship(Context context,
                     TextureLoader textureLoader,
                     int textureResourceId,
                     ShipType shipType,
                     float x, float y, float z,
                     float size,
                     int maxHealth) {
        super(context, "shaders/spaceship_vertex.glsl", "shaders/spaceship_fragment.glsl");

        this.context = context;
        this.textureLoader = textureLoader;
        this.textureResourceId = textureResourceId;
        this.shipType = shipType;

        // Posición inicial
        this.x = x;
        this.y = y;
        this.z = z;

        // Tamaño y colisión
        this.size = size;
        this.collisionRadius = size * 0.4f; // Radio ligeramente menor que el tamaño visual

        // Sistema de vida
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;

        // Obtener uniform locations
        aPosLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        aTexLoc = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        uTexLoc = GLES20.glGetUniformLocation(programId, "u_Texture");
        uTintColorLoc = GLES20.glGetUniformLocation(programId, "u_TintColor");
        uAlphaLoc = GLES20.glGetUniformLocation(programId, "u_Alpha");

        // Inicializar buffers
        vertexBuffer = ByteBuffer.allocateDirect(VERTICES.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(VERTICES);
        vertexBuffer.position(0);

        texCoordBuffer = ByteBuffer.allocateDirect(TEX_COORDS.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(TEX_COORDS);
        texCoordBuffer.position(0);

        indexBuffer = ByteBuffer.allocateDirect(INDICES.length * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
                .put(INDICES);
        indexBuffer.position(0);

        Log.d(TAG, "Spaceship created: " + shipType + " at (" + x + ", " + y + ", " + z + ")");
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    @Override
    public void update(float deltaTime) {
        if (isDead) return;

        // Actualizar posición
        x += vx * deltaTime;
        y += vy * deltaTime;

        // Mantener dentro de límites (ajustar según tu escena)
        float bounds = 4.0f;
        if (x < -bounds) x = -bounds;
        if (x > bounds) x = bounds;
        if (y < -bounds) y = -bounds;
        if (y > bounds) y = bounds;

        // Actualizar timer de disparo
        fireTimer -= deltaTime;
        if (fireTimer < 0) {
            fireTimer = 0;
        }
    }

    @Override
    public void draw() {
        if (isDead) return;
        if (camera == null) return;

        // ✅ CONFIGURAR BLENDING PARA TRANSPARENCIA
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // ✅ DESACTIVAR DEPTH WRITE (pero mantener depth test)
        // Esto evita que objetos transparentes bloqueen otros objetos detrás
        GLES20.glDepthMask(false);

        // Activar programa
        useProgram();

        // Construir matriz de modelo (traslación + escala + billboard)
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);

        // Billboard: rotar para que siempre mire a la cámara
        // Para simplificar, usamos rotación fija (top-down view)
        Matrix.rotateM(modelMatrix, 0, -90, 1, 0, 0); // Rotar para vista top-down

        Matrix.scaleM(modelMatrix, 0, size, size, size);

        // Calcular MVP
        camera.computeMvp(modelMatrix, mvpMatrix);

        // Enviar MVP al shader
        int uMvpLoc = GLES20.glGetUniformLocation(programId, "u_MVP");
        GLES20.glUniformMatrix4fv(uMvpLoc, 1, false, mvpMatrix, 0);

        // Enviar tintColor y alpha
        GLES20.glUniform4fv(uTintColorLoc, 1, tintColor, 0);
        GLES20.glUniform1f(uAlphaLoc, alpha);

        // Bind textura
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureLoader.getTexture(textureResourceId));
        GLES20.glUniform1i(uTexLoc, 0);

        // Enviar geometría
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(aTexLoc);
        GLES20.glVertexAttribPointer(aTexLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

        // Dibujar
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, INDICES.length, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        // Limpiar
        GLES20.glDisableVertexAttribArray(aPosLoc);
        GLES20.glDisableVertexAttribArray(aTexLoc);

        // ✅ RESTAURAR DEPTH WRITE
        GLES20.glDepthMask(true);
    }

    /**
     * Recibe daño
     */
    public void takeDamage(int damage) {
        if (isDead) return;

        currentHealth -= damage;
        if (currentHealth <= 0) {
            currentHealth = 0;
            isDead = true;
            Log.d(TAG, shipType + " destroyed!");
        }

        // Efecto visual de daño (tinte rojo temporal)
        tintColor = new float[]{1f, 0f, 0f, 0.5f}; // Rojo con 50% intensidad
    }

    /**
     * Verifica si puede disparar
     */
    public boolean canFire() {
        return !isDead && fireTimer <= 0;
    }

    /**
     * Marca que disparó (resetea timer)
     */
    public void didFire() {
        fireTimer = 1.0f / fireRate;
    }

    /**
     * Obtiene dirección de disparo (normalizada)
     */
    public float[] getFireDirection() {
        // Jugador dispara hacia arriba (Y+), enemigos hacia abajo (Y-)
        if (shipType == ShipType.PLAYER) {
            return new float[]{0f, 1f, 0f};
        } else {
            return new float[]{0f, -1f, 0f};
        }
    }

    /**
     * Respawnea la nave
     */
    public void respawn(float newX, float newY, float newZ) {
        this.x = newX;
        this.y = newY;
        this.z = newZ;
        this.currentHealth = maxHealth;
        this.isDead = false;
        this.vx = 0;
        this.vy = 0;
        this.fireTimer = 0;
        this.tintColor = new float[]{1f, 1f, 1f, 0f}; // Sin tinte
        this.alpha = 1.0f;
        Log.d(TAG, shipType + " respawned at (" + x + ", " + y + ", " + z + ")");
    }
}
