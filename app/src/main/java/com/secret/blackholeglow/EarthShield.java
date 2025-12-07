package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import com.secret.blackholeglow.util.ProceduralSphere;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * EarthShield - Escudo invisible alrededor de la Tierra
 * - 100% transparente (no se ve la esfera)
 * - Efectos únicos en impactos de meteoritos:
 *   • Grietas radiales volcánicas
 *   • Ondas de choque concéntricas
 *   • Distorsión de calor
 * - Sin sistema de HP ni destrucción
 * - Color rojo/naranja volcánico (diferente del forcefield azul plasma)
 * - Shaders propios: earth_shield_vertex.glsl + earth_shield_fragment.glsl
 */
public class EarthShield implements SceneObject, CameraAware {
    private static final String TAG = "EarthShield";

    // Shader y geometría
    private int programId;
    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;
    private ShortBuffer indexBuffer;
    private CameraController camera;

    // Uniforms
    private int uMvpLoc;
    private int uTimeLoc;
    private int uTextureLoc;
    private int uColorLoc;
    private int uAlphaLoc;
    private int uImpactPosLoc;
    private int uImpactIntensityLoc;
    private int uHealthLoc;

    // Estado del escudo
    private final float[] position;
    private final float scale;
    private float rotationAngle = 0f;
    private final float rotationSpeed = 5.0f;
    private final int textureId;

    // Sistema de impactos (16 impactos simultáneos)
    private static final int MAX_IMPACTS = 16;
    private final float[] impactPositions = new float[MAX_IMPACTS * 3];
    private final float[] impactIntensities = new float[MAX_IMPACTS];
    private int currentImpactIndex = 0;

    // ⚡ OPTIMIZACIÓN: Matrices reutilizables (evitar allocations en draw)
    private final float[] modelMatrixCache = new float[16];
    private final float[] mvpMatrixCache = new float[16];
    private final float[] impactColorCache = new float[3];

    /**
     * Constructor del EarthShield
     * @param x Posición X (centro de la Tierra)
     * @param y Posición Y (centro de la Tierra)
     * @param z Posición Z (centro de la Tierra)
     * @param scale Radio del escudo (ligeramente mayor que la Tierra)
     */
    public EarthShield(Context context, TextureManager textureManager,
                       float x, float y, float z, float scale) {
        this.position = new float[]{x, y, z};
        this.scale = scale;
        this.textureId = textureManager.getTexture(R.drawable.fondo_transparente);

        // Inicializar arrays de impactos
        for (int i = 0; i < MAX_IMPACTS; i++) {
            impactIntensities[i] = 0f;
        }

        initShader(context);
        setupGeometry(context);

        Log.d(TAG, "[EarthShield] ✓ Escudo invisible de la Tierra creado");
        Log.d(TAG, "[EarthShield]   Posición: (" + x + ", " + y + ", " + z + ")");
        Log.d(TAG, "[EarthShield]   Radio: " + scale);
    }

    private void initShader(Context context) {
        // Usar SHADERS PROPIOS del EarthShield (efectos volcánicos/fracturas)
        programId = ShaderUtils.createProgramFromAssets(context,
                "shaders/earth_shield_vertex.glsl",
                "shaders/earth_shield_fragment.glsl");

        if (programId == 0) {
            Log.e(TAG, "[EarthShield] ✗ ERROR creando shader");
            return;
        }

        // Obtener locations
        uMvpLoc = GLES20.glGetUniformLocation(programId, "u_MVP");
        uTimeLoc = GLES20.glGetUniformLocation(programId, "u_Time");
        uTextureLoc = GLES20.glGetUniformLocation(programId, "u_Texture");
        uColorLoc = GLES20.glGetUniformLocation(programId, "u_Color");
        uAlphaLoc = GLES20.glGetUniformLocation(programId, "u_Alpha");
        uImpactPosLoc = GLES20.glGetUniformLocation(programId, "u_ImpactPos");
        uImpactIntensityLoc = GLES20.glGetUniformLocation(programId, "u_ImpactIntensity");
        uHealthLoc = GLES20.glGetUniformLocation(programId, "u_Health");

        Log.d(TAG, "[EarthShield] ✓ Shader PROPIO inicializado - programId=" + programId);
        Log.d(TAG, "[EarthShield]   Efectos: Grietas + Ondas + Calor");
    }

    private void setupGeometry(Context context) {
        // Usar esfera procedural con UVs perfectos
        ProceduralSphere.Mesh mesh = ProceduralSphere.generateMedium(1.0f);

        vertexBuffer = mesh.vertexBuffer;
        texCoordBuffer = mesh.uvBuffer;
        indexBuffer = mesh.indexBuffer;

        Log.d(TAG, "[EarthShield] ✓ Geometría preparada:");
        Log.d(TAG, "[EarthShield]   Vértices: " + mesh.vertexCount);
        Log.d(TAG, "[EarthShield]   Triángulos: " + (mesh.indexCount / 3));
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    /**
     * Registra un impacto de meteorito en el escudo
     */
    public void registerImpact(float worldX, float worldY, float worldZ) {
        // Convertir coordenadas mundiales a coordenadas locales del objeto
        // (restar la posición del centro del escudo)
        float localX = worldX - position[0];
        float localY = worldY - position[1];
        float localZ = worldZ - position[2];

        // Normalizar a la superficie de la esfera (radio = 1.0 en espacio local)
        float dist = (float)Math.sqrt(localX*localX + localY*localY + localZ*localZ);
        if (dist > 0.001f) {
            localX /= dist;
            localY /= dist;
            localZ /= dist;
        }

        // Registrar posición del impacto en espacio local
        int idx = currentImpactIndex % MAX_IMPACTS;
        impactPositions[idx * 3] = localX;
        impactPositions[idx * 3 + 1] = localY;
        impactPositions[idx * 3 + 2] = localZ;
        impactIntensities[idx] = 1.0f;  // Intensidad máxima

        currentImpactIndex++;

        Log.d(TAG, "[EarthShield] 💥 IMPACTO en mundo(" + worldX + "," + worldY + "," + worldZ +
              ") -> local(" + localX + "," + localY + "," + localZ + ")");
    }

    /**
     * Verifica si una posición está dentro del escudo
     */
    public boolean containsPoint(float worldX, float worldY, float worldZ) {
        float dx = worldX - position[0];
        float dy = worldY - position[1];
        float dz = worldZ - position[2];
        float distance = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);

        return distance <= scale;
    }

    @Override
    public void update(float deltaTime) {
        rotationAngle += rotationSpeed * deltaTime;

        // Decrementar intensidades de impactos (fade out rápido)
        for (int i = 0; i < MAX_IMPACTS; i++) {
            if (impactIntensities[i] > 0) {
                impactIntensities[i] -= deltaTime * 1.5f;  // Fade en ~0.67 segundos
                if (impactIntensities[i] < 0) impactIntensities[i] = 0;
            }
        }
    }

    @Override
    public void draw() {
        if (!GLES20.glIsProgram(programId) || camera == null) return;

        GLES20.glUseProgram(programId);

        // Configurar blending para transparencia
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Construir matriz de modelo (⚡ OPTIMIZADO: usar caches)
        android.opengl.Matrix.setIdentityM(modelMatrixCache, 0);
        android.opengl.Matrix.translateM(modelMatrixCache, 0, position[0], position[1], position[2]);
        android.opengl.Matrix.rotateM(modelMatrixCache, 0, rotationAngle, 0, 1, 0);
        android.opengl.Matrix.scaleM(modelMatrixCache, 0, scale, scale, scale);

        // Construir matriz MVP
        camera.computeMvp(modelMatrixCache, mvpMatrixCache);

        // Enviar uniforms
        GLES20.glUniformMatrix4fv(uMvpLoc, 1, false, mvpMatrixCache, 0);
        GLES20.glUniform1f(uTimeLoc, rotationAngle);

        // Textura
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTextureLoc, 0);

        // Color base ROJO VOLCÁNICO (⚡ OPTIMIZADO: usar cache)
        impactColorCache[0] = 0.8f;
        impactColorCache[1] = 0.2f;
        impactColorCache[2] = 0.0f;
        GLES20.glUniform3fv(uColorLoc, 1, impactColorCache, 0);

        // Alpha = 0.0 (COMPLETAMENTE INVISIBLE - solo se ven grietas/ondas/calor)
        GLES20.glUniform1f(uAlphaLoc, 0.0f);

        // Health = 100 (siempre activo, sin destrucción)
        GLES20.glUniform1f(uHealthLoc, 1.0f);

        // Enviar posiciones e intensidades de impactos
        GLES20.glUniform3fv(uImpactPosLoc, MAX_IMPACTS, impactPositions, 0);
        GLES20.glUniform1fv(uImpactIntensityLoc, MAX_IMPACTS, impactIntensities, 0);

        // Dibujar la esfera
        int aPositionLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        int aTexCoordLoc = GLES20.glGetAttribLocation(programId, "a_TexCoord");

        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(aTexCoordLoc);
        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexBuffer.capacity(),
                GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        GLES20.glDisableVertexAttribArray(aPositionLoc);
        GLES20.glDisableVertexAttribArray(aTexCoordLoc);

        GLES20.glDisable(GLES20.GL_BLEND);
    }
}
