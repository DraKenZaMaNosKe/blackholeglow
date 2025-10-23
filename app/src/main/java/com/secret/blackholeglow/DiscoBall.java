// ============================================
// DiscoBall.java
// Bola disco con cuadritos espejo, rotación,
// breathing effect y audio reactivity
// ============================================

package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.util.ObjLoader;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.List;

/**
 * Bola disco renderizada con shader procedural de cuadritos espejo.
 *
 * Features:
 *  - Rotación continua en Y axis
 *  - Breathing effect (pulsación)
 *  - Mirror tile grid (procedural en shader)
 *  - Color cycling
 *  - Audio reactivity (bass response)
 */
public class DiscoBall implements SceneObject, CameraAware, MusicReactive {
    private static final String TAG = "DiscoBall";

    // Shader program
    private final DiscoBallShaderProgram shader;

    // Buffers y mesh
    private final FloatBuffer vertexBuffer;
    private final FloatBuffer texCoordBuffer;
    private final FloatBuffer normalBuffer;  // Normales para iluminación
    private final ShortBuffer indexBuffer;
    private final int indexCount;

    // Attribute locations
    private final int aPosLoc;
    private final int aTexLoc;
    private final int aNormalLoc;

    // Estado de animación
    private float rotation = 0f;  // Rotación en Y axis
    private float accumulatedTime = 0f;
    private final float spinSpeed;  // Velocidad de rotación
    private final float scale;  // Tamaño de la bola

    // Matrices
    private final float[] model = new float[16];
    private final float[] mvp = new float[16];

    // Cámara
    private CameraController camera;

    // Audio reactivity
    private boolean musicReactive = true;
    private float musicBassBoost = 0f;

    /**
     * Constructor
     *
     * @param ctx Context
     * @param spinSpeed Velocidad de rotación (radianes/seg)
     * @param scale Tamaño de la bola
     */
    public DiscoBall(Context ctx, float spinSpeed, float scale) {
        this.spinSpeed = spinSpeed;
        this.scale = scale;

        Log.d(TAG, String.format("Creando disco ball: spin=%.2f, scale=%.2f", spinSpeed, scale));

        // Crear shader program
        shader = new DiscoBallShaderProgram(ctx);

        // Cargar malla .obj (usamos planeta.obj que es una esfera)
        ObjLoader.Mesh mesh;
        try {
            mesh = ObjLoader.loadObj(ctx, "planeta.obj");
            Log.d(TAG, "Malla cargada: " + mesh.vertexCount + " vértices");
        } catch (IOException e) {
            throw new RuntimeException("Error cargando planeta.obj para disco ball", e);
        }

        vertexBuffer = mesh.vertexBuffer;
        texCoordBuffer = mesh.uvBuffer;

        // ============================================
        // CALCULAR NORMALES (para iluminación)
        // ============================================
        // Para una esfera centrada en origen, normal = posición normalizada
        normalBuffer = mesh.vertexBuffer.duplicate();  // Las normales son iguales a vértices en esfera unitaria

        // ============================================
        // CONSTRUIR ÍNDICES
        // ============================================
        List<short[]> faces = mesh.faces;
        int triCount = 0;
        for (short[] f : faces) {
            triCount += f.length - 2;
        }
        indexCount = triCount * 3;

        indexBuffer = ObjLoader.buildIndexBuffer(faces, indexCount);

        // ============================================
        // OBTENER ATTRIBUTE LOCATIONS
        // ============================================
        shader.useProgram();
        aPosLoc = GLES20.glGetAttribLocation(shader.programId, "a_Position");
        aTexLoc = GLES20.glGetAttribLocation(shader.programId, "a_TexCoord");
        aNormalLoc = GLES20.glGetAttribLocation(shader.programId, "a_Normal");

        Log.d(TAG, String.format("Attribute locations - aPos:%d, aTex:%d, aNormal:%d",
                aPosLoc, aTexLoc, aNormalLoc));

        if (aPosLoc == -1 || aTexLoc == -1 || aNormalLoc == -1) {
            Log.e(TAG, "⚠️ ERROR: Algunos attributes no encontrados en shader!");
        }

        Log.d(TAG, "DiscoBall inicializada correctamente");
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
        Log.d(TAG, "CameraController asignado a DiscoBall");
    }

    @Override
    public void update(float dt) {
        // Actualizar rotación
        rotation += spinSpeed * dt;
        if (rotation > 2f * (float)Math.PI) {
            rotation -= 2f * (float)Math.PI;
        }

        // Acumular tiempo para shader
        accumulatedTime += dt;
    }

    @Override
    public void draw() {
        if (camera == null) {
            Log.w(TAG, "Camera no asignada - skipping draw");
            return;
        }

        // ============================================
        // MATRIZ MODEL
        // ============================================
        Matrix.setIdentityM(model, 0);

        // Escala (base + boost musical)
        float totalScale = scale * (1.0f + musicBassBoost);
        Matrix.scaleM(model, 0, totalScale, totalScale, totalScale);

        // Rotación en Y (la bola gira)
        Matrix.rotateM(model, 0, (float)Math.toDegrees(rotation), 0f, 1f, 0f);

        // ============================================
        // MATRIZ MVP
        // ============================================
        camera.computeMvp(model, mvp);

        // ============================================
        // ACTIVAR SHADER Y ENVIAR UNIFORMS
        // ============================================
        shader.useProgram();
        shader.setTime(accumulatedTime);
        shader.setMvpAndResolution(mvp, SceneRenderer.screenWidth, SceneRenderer.screenHeight);
        shader.setAudioBass(musicBassBoost);

        // ============================================
        // CONFIGURAR ATTRIBUTES
        // ============================================
        // Posiciones
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        // UV coords
        texCoordBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aTexLoc);
        GLES20.glVertexAttribPointer(aTexLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

        // Normales
        normalBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aNormalLoc);
        GLES20.glVertexAttribPointer(aNormalLoc, 3, GLES20.GL_FLOAT, false, 0, normalBuffer);

        // ============================================
        // DIBUJAR
        // ============================================
        indexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        // ============================================
        // CLEANUP
        // ============================================
        GLES20.glDisableVertexAttribArray(aPosLoc);
        GLES20.glDisableVertexAttribArray(aTexLoc);
        GLES20.glDisableVertexAttribArray(aNormalLoc);
    }

    // ============================================
    // MUSIC REACTIVITY
    // ============================================

    @Override
    public void setMusicReactive(boolean enabled) {
        this.musicReactive = enabled;
    }

    @Override
    public void onMusicData(float bassLevel, float midLevel, float trebleLevel,
                           float volumeLevel, float beatIntensity, boolean isBeat) {
        if (!musicReactive) {
            musicBassBoost = 0f;
            return;
        }

        // Bass boost (0.0 - 1.0)
        // Escala la bola hasta 30% más grande con bajos fuertes
        musicBassBoost = bassLevel * 0.3f;
    }

    @Override
    public boolean isMusicReactive() {
        return musicReactive;
    }
}
