// Projectile.java - Proyectil para batalla espacial
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
import java.nio.ShortBuffer;

/**
 * Proyectil simple con pooling.
 * Renderiza un billboard pequeño con textura.
 */
public class Projectile extends BaseShaderProgram implements SceneObject, CameraAware {
    private static final String TAG = "Projectile";

    // Configuración
    private final Context context;
    private final TextureLoader textureLoader;
    private final int textureResourceId;

    // Estado
    public boolean active = false;
    public boolean isPlayerProjectile = true; // true = jugador, false = enemigo

    // Propiedades físicas
    public float x, y, z;
    public float vx, vy, vz;
    public float speed = 8.0f;      // Velocidad de movimiento
    public float size = 0.3f;       // Tamaño del billboard
    public float collisionRadius = 0.15f;
    public int damage = 10;

    // Uniform locations
    private final int aPosLoc, aTexLoc;
    private final int uTexLoc, uTintColorLoc, uAlphaLoc;

    // 🪨 MODELO 3D COMPARTIDO (static para optimización)
    private static FloatBuffer vertexBuffer;
    private static FloatBuffer texCoordBuffer;
    private static IntBuffer indexBuffer;  // INT para modelos con >32k vértices
    private static int indexCount = 0;
    private static boolean buffersInitialized = false;

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // Camera
    private CameraController camera;

    // Geometría del billboard
    private static final float[] VERTICES = {
        -0.5f,  0.5f, 0f,
        -0.5f, -0.5f, 0f,
         0.5f, -0.5f, 0f,
         0.5f,  0.5f, 0f
    };

    private static final float[] TEX_COORDS = {
        0f, 0f,
        0f, 1f,
        1f, 1f,
        1f, 0f
    };

    private static final short[] INDICES = {
        0, 1, 2,
        0, 2, 3
    };

    /**
     * Constructor
     */
    public Projectile(Context context, TextureLoader textureLoader, int textureResourceId) {
        super(context, "shaders/spaceship_vertex.glsl", "shaders/spaceship_fragment.glsl");

        this.context = context;
        this.textureLoader = textureLoader;
        this.textureResourceId = textureResourceId;

        // ════════════════════════════════════════════════════════════
        // 🪨 CARGAR MODELO 3D UNA SOLA VEZ (compartido entre proyectiles)
        // ════════════════════════════════════════════════════════════
        if (!buffersInitialized) {
            Log.d("Projectile", "🪨 Cargando modelo 3D AsteroideRealista.obj para proyectiles...");

            try {
                ObjLoader.Mesh mesh = ObjLoader.loadObj(context, "AsteroideRealista.obj");

                // Usar buffers del OBJ
                vertexBuffer = mesh.vertexBuffer;
                texCoordBuffer = mesh.uvBuffer;

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

                indexCount = totalIndices;

                // Crear IntBuffer
                ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 4);
                ibb.order(ByteOrder.nativeOrder());
                indexBuffer = ibb.asIntBuffer();
                indexBuffer.put(indices);
                indexBuffer.position(0);

                Log.d("Projectile", "✅ Modelo 3D cargado para proyectiles - vértices: " + mesh.vertexCount +
                                   ", índices: " + indexCount);

                buffersInitialized = true;

            } catch (IOException e) {
                Log.e("Projectile", "❌ Error cargando modelo 3D, usando billboard fallback", e);

                // Fallback a billboard si falla
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

                // Convertir short[] a int[] para consistencia
                int[] intIndices = new int[INDICES.length];
                for (int i = 0; i < INDICES.length; i++) {
                    intIndices[i] = INDICES[i] & 0xFFFF;
                }

                indexBuffer = ByteBuffer.allocateDirect(intIndices.length * 4)
                        .order(ByteOrder.nativeOrder())
                        .asIntBuffer()
                        .put(intIndices);
                indexBuffer.position(0);

                indexCount = intIndices.length;

                buffersInitialized = true;
            }
        }

        // Obtener uniform locations
        aPosLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        aTexLoc = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        uTexLoc = GLES20.glGetUniformLocation(programId, "u_Texture");
        uTintColorLoc = GLES20.glGetUniformLocation(programId, "u_TintColor");
        uAlphaLoc = GLES20.glGetUniformLocation(programId, "u_Alpha");
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    /**
     * Activa el proyectil con posición y dirección
     */
    public void activate(float x, float y, float z, float dirX, float dirY, float dirZ, boolean isPlayerProjectile) {
        this.x = x;
        this.y = y;
        this.z = z;

        // Normalizar dirección y aplicar velocidad
        float magnitude = (float) Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (magnitude > 0) {
            this.vx = (dirX / magnitude) * speed;
            this.vy = (dirY / magnitude) * speed;
            this.vz = (dirZ / magnitude) * speed;
        } else {
            this.vx = 0;
            this.vy = speed; // Default: hacia arriba
            this.vz = 0;
        }

        this.isPlayerProjectile = isPlayerProjectile;
        this.active = true;
    }

    /**
     * Desactiva el proyectil (vuelve al pool)
     */
    public void deactivate() {
        this.active = false;
    }

    @Override
    public void update(float deltaTime) {
        if (!active) return;

        // Actualizar posición
        x += vx * deltaTime;
        y += vy * deltaTime;
        z += vz * deltaTime;

        // Desactivar si sale de los límites
        float bounds = 10.0f;
        if (Math.abs(x) > bounds || Math.abs(y) > bounds || Math.abs(z) > bounds) {
            deactivate();
        }
    }

    @Override
    public void draw() {
        if (!active) return;
        if (camera == null) return;

        // ✅ CONFIGURAR BLENDING PARA TRANSPARENCIA
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // ✅ DESACTIVAR DEPTH WRITE para proyectiles brillantes
        GLES20.glDepthMask(false);

        // Activar programa
        useProgram();

        // Construir matriz de modelo
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);
        Matrix.rotateM(modelMatrix, 0, -90, 1, 0, 0); // Top-down view
        Matrix.scaleM(modelMatrix, 0, size, size, size);

        // Calcular MVP
        camera.computeMvp(modelMatrix, mvpMatrix);

        // Enviar MVP al shader
        int uMvpLoc = GLES20.glGetUniformLocation(programId, "u_MVP");
        GLES20.glUniformMatrix4fv(uMvpLoc, 1, false, mvpMatrix, 0);

        // Tinte de color (sin tinte, solo brillo de la textura)
        float[] tintColor = {1f, 1f, 1f, 0f};
        GLES20.glUniform4fv(uTintColorLoc, 1, tintColor, 0);
        GLES20.glUniform1f(uAlphaLoc, 1.0f);

        // Bind textura
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureLoader.getTexture(textureResourceId));
        GLES20.glUniform1i(uTexLoc, 0);

        // Enviar geometría
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(aTexLoc);
        GLES20.glVertexAttribPointer(aTexLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

        // Dibujar con índices INT (modelo 3D o billboard fallback)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_INT, indexBuffer);

        // Limpiar
        GLES20.glDisableVertexAttribArray(aPosLoc);
        GLES20.glDisableVertexAttribArray(aTexLoc);

        // ✅ RESTAURAR DEPTH WRITE
        GLES20.glDepthMask(true);
    }
}
