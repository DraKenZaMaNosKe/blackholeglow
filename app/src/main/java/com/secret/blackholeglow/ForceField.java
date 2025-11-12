package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import com.secret.blackholeglow.util.ObjLoader;
import com.secret.blackholeglow.util.ProceduralSphere;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * ForceField - Campo de fuerza interactivo alrededor del sol
 * - Detecta colisiones con meteoritos
 * - Ilumina las zonas de impacto
 * - Se destruye progresivamente con múltiples impactos
 * - Reacciona a la música en tiempo real
 */
public class ForceField implements SceneObject, CameraAware, MusicReactive {
    private static final String TAG = "depurar";

    // Shader y geometría
    private int programId;
    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;
    private ShortBuffer indexBuffer;
    private CameraController camera;

    // ===== 💾 PLAYER STATS (para auto-guardar HP) =====
    private PlayerStats playerStats;

    // Uniforms
    private int uMvpLoc;
    private int uTimeLoc;
    private int uTextureLoc;
    private int uColorLoc;
    private int uAlphaLoc;
    private int uImpactPosLoc;  // Posiciones de impacto
    private int uImpactIntensityLoc;  // Intensidad de cada impacto
    private int uHealthLoc;  // Vida del campo de fuerza

    // 🎵 PLASMA: Uniforms para reactividad musical
    private int uMusicBassLoc;
    private int uMusicTrebleLoc;
    private int uMusicBeatLoc;

    // Estado del campo de fuerza
    private final float[] position;
    private final float scale;
    private float rotationAngle = 0f;
    private final float rotationSpeed;
    private final int textureId;
    private final float[] baseColor;
    private float baseAlpha;

    // Sistema de impactos (hasta 16 impactos simultáneos - aumentado para evitar saturación)
    private static final int MAX_IMPACTS = 16;
    private final float[] impactPositions = new float[MAX_IMPACTS * 3];  // x, y, z
    private final float[] impactIntensities = new float[MAX_IMPACTS];
    private int currentImpactIndex = 0;

    // Sistema de vida
    private static final int MAX_HEALTH = 100;  // 🛡️ Más resistente - dura el doble
    private int currentHealth = MAX_HEALTH;
    private boolean isDestroyed = false;
    private float destructionAnimation = 0f;

    // Pulsación
    private final Float scaleOscPercent;
    private final float scaleOscSpeed;

    // ===== SISTEMA DE REACTIVIDAD MUSICAL =====
    private boolean musicReactive = true;
    private float musicTrebleIntensity = 0f;  // Agudos → Brillo/intensidad
    private float musicBeatFlash = 0f;        // Beats → Destellos
    private float musicEnergyBoost = 0f;      // Energía general → Alpha
    private long lastMusicLogTime = 0;        // Para logs periódicos

    public ForceField(Context context, TextureManager textureManager,
                      float x, float y, float z, float scale,
                      int textureResourceId,
                      float[] color, float alpha,
                      Float scaleOscPercent, float scaleOscSpeed) {
        this.position = new float[]{x, y, z};
        this.scale = scale;
        this.textureId = textureManager.getTexture(textureResourceId);
        this.baseColor = color;
        this.baseAlpha = alpha;
        this.scaleOscPercent = scaleOscPercent;
        this.scaleOscSpeed = scaleOscSpeed;
        this.rotationSpeed = 5.0f;

        // Inicializar arrays de impactos
        for (int i = 0; i < MAX_IMPACTS; i++) {
            impactIntensities[i] = 0f;
        }

        initShader(context);
        setupGeometry(context);

        Log.d(TAG, "[ForceField] ✓ Campo de fuerza creado - Health: " + currentHealth);
    }

    private void initShader(Context context) {
        // 🌩️ USAR NUEVO SHADER DE PLASMA
        Log.d(TAG, "[ForceField] Intentando cargar shaders...");
        Log.d(TAG, "[ForceField]   Vertex: shaders/forcefield_vertex.glsl");
        Log.d(TAG, "[ForceField]   Fragment: shaders/plasma_forcefield_fragment.glsl");

        programId = ShaderUtils.createProgramFromAssets(context,
                "shaders/forcefield_vertex.glsl",
                "shaders/plasma_forcefield_fragment.glsl");

        if (programId == 0) {
            Log.e(TAG, "[ForceField] ✗✗✗ ERROR CRÍTICO creando shader de PLASMA");
            Log.e(TAG, "[ForceField] El shader NO se compiló o NO se enlazó correctamente");
            Log.e(TAG, "[ForceField] Verifica los logs anteriores de ShaderUtils para detalles");
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

        // 🎵 PLASMA: Locations de música
        uMusicBassLoc = GLES20.glGetUniformLocation(programId, "u_MusicBass");
        uMusicTrebleLoc = GLES20.glGetUniformLocation(programId, "u_MusicTreble");
        uMusicBeatLoc = GLES20.glGetUniformLocation(programId, "u_MusicBeat");

        Log.d(TAG, "[ForceField] ⚡ Shader de PLASMA inicializado - programId=" + programId);
    }

    /**
     * ════════════════════════════════════════════════════════════
     * ✅ USAR ESFERA PROCEDURAL con UVs perfectos
     * ════════════════════════════════════════════════════════════
     * Reemplazamos createOptimizedSphere() manual por ProceduralSphere:
     *  - UVs matemáticamente correctos (antes eran placeholder)
     *  - Código más limpio (antes ~70 líneas, ahora ~10)
     *  - Misma o mejor performance
     *  - Consistencia con otros objetos esféricos
     * ════════════════════════════════════════════════════════════
     */
    private void setupGeometry(Context context) {
        Log.d(TAG, "[ForceField] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Log.d(TAG, "[ForceField] ✨ Usando ESFERA PROCEDURAL (UVs perfectos)");
        Log.d(TAG, "[ForceField] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Usar resolución media para el campo de fuerza (balance calidad/rendimiento)
        ProceduralSphere.Mesh mesh = ProceduralSphere.generateMedium(1.0f);

        vertexBuffer = mesh.vertexBuffer;
        texCoordBuffer = mesh.uvBuffer;
        indexBuffer = mesh.indexBuffer;

        Log.d(TAG, "[ForceField] ✓ ForceField mesh preparada:");
        Log.d(TAG, "[ForceField]   Vértices: " + mesh.vertexCount + " (vs ~200 manual anterior)");
        Log.d(TAG, "[ForceField]   Triángulos: " + (mesh.indexCount / 3));
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    /**
     * Registra un impacto de meteorito en el campo de fuerza
     */
    public void registerImpact(float worldX, float worldY, float worldZ) {
        if (isDestroyed) return;

        // Reducir vida
        currentHealth--;

        // 💾 AUTO-GUARDAR HP en PlayerStats
        if (playerStats != null) {
            playerStats.updateForceFieldHealth(currentHealth);
        }

        // Convertir coordenadas mundiales a coordenadas locales del objeto
        // (restar la posición del centro del ForceField)
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

        Log.d(TAG, "[ForceField] ¡IMPACTO! Health: " + currentHealth + "/" + MAX_HEALTH +
                   " mundo(" + worldX + "," + worldY + "," + worldZ + ") -> local(" +
                   localX + "," + localY + "," + localZ + ")");

        // Verificar destrucción
        if (currentHealth <= 0) {
            isDestroyed = true;
            Log.d(TAG, "[ForceField] ✗✗✗ CAMPO DE FUERZA DESTRUIDO ✗✗✗");
        }
    }

    /**
     * Verifica si una posición está dentro del campo de fuerza
     * ✅ Si está destruido, NO bloquea meteoros (dejan pasar)
     */
    public boolean containsPoint(float worldX, float worldY, float worldZ) {
        // Si está destruido, no bloquea nada (meteoros pasan directo al sol)
        if (isDestroyed) return false;

        float dx = worldX - position[0];
        float dy = worldY - position[1];
        float dz = worldZ - position[2];
        float distance = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);

        return distance <= scale;
    }

    public boolean isDestroyed() {
        return isDestroyed;
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public int getMaxHealth() {
        return MAX_HEALTH;
    }

    /**
     * 💾 Establece el HP directamente (usado para cargar estado guardado)
     */
    public void setHealth(int health) {
        this.currentHealth = Math.max(0, Math.min(health, MAX_HEALTH));
        if (currentHealth <= 0) {
            isDestroyed = true;
        }
        Log.d(TAG, "HP establecido: " + currentHealth + "/" + MAX_HEALTH);
    }

    /**
     * 💾 Inyecta PlayerStats para auto-guardar HP
     */
    public void setPlayerStats(PlayerStats stats) {
        this.playerStats = stats;
    }

    public void reset() {
        currentHealth = MAX_HEALTH;
        isDestroyed = false;
        destructionAnimation = 0f;
        currentImpactIndex = 0;

        // Limpiar todos los impactos
        for (int i = 0; i < MAX_IMPACTS; i++) {
            impactIntensities[i] = 0f;
        }

        Log.d(TAG, "[ForceField] ✓ Campo de fuerza RESETEADO - Health: " + MAX_HEALTH);
    }

    @Override
    public void update(float deltaTime) {
        rotationAngle += rotationSpeed * deltaTime;

        // Decrementar intensidades de impactos (fade out más rápido)
        for (int i = 0; i < MAX_IMPACTS; i++) {
            if (impactIntensities[i] > 0) {
                impactIntensities[i] -= deltaTime * 1.2f;  // Fade en ~0.83 segundos (más rápido)
                if (impactIntensities[i] < 0) impactIntensities[i] = 0;
            }
        }

        // Animación de destrucción
        if (isDestroyed) {
            destructionAnimation += deltaTime * 2.0f;  // 0.5 segundos para destruirse
        }
    }

    @Override
    public void draw() {
        if (!GLES20.glIsProgram(programId)) return;

        // ✅ Si está destruido, NO dibujarlo (desaparece completamente)
        if (isDestroyed) {
            return;  // No dibujar nada cuando está destruido
        }

        GLES20.glUseProgram(programId);

        // ════════════════════════════════════════════════════════════
        // ✗ ESCALA GRADUAL ELIMINADA (era solo para pruebas)
        // ════════════════════════════════════════════════════════════
        float currentScale = scale;
        // if (scaleOscPercent != null) {  // COMENTADO - ya no necesario
        //     float osc = 1.0f + (float)(Math.sin(rotationAngle * scaleOscSpeed) * scaleOscPercent);
        //     currentScale *= osc;
        // }

        // ✗ DESTRUCCIÓN - NO crece, simplemente desaparece (early return arriba)
        // if (isDestroyed) {
        //     currentScale *= (1.0f + destructionAnimation * 0.5f);  // ELIMINADO
        // }

        // Matriz de modelo
        float[] modelMatrix = new float[16];
        android.opengl.Matrix.setIdentityM(modelMatrix, 0);
        android.opengl.Matrix.translateM(modelMatrix, 0, position[0], position[1], position[2]);
        android.opengl.Matrix.rotateM(modelMatrix, 0, rotationAngle, 0, 1, 0);
        android.opengl.Matrix.scaleM(modelMatrix, 0, currentScale, currentScale, currentScale);

        // MVP
        float[] mvpMatrix = new float[16];
        if (camera != null) {
            camera.computeMvp(modelMatrix, mvpMatrix);
        } else {
            System.arraycopy(modelMatrix, 0, mvpMatrix, 0, 16);
        }

        GLES20.glUniformMatrix4fv(uMvpLoc, 1, false, mvpMatrix, 0);
        GLES20.glUniform1f(uTimeLoc, rotationAngle);

        // Color y alpha con reactividad musical
        float currentAlpha = baseAlpha;
        // ✗ YA NO NECESITAMOS FADE OUT - el campo desaparece instantáneamente
        // if (isDestroyed) {
        //     currentAlpha *= Math.max(0, 1.0f - destructionAnimation);  // ELIMINADO
        // }

        // REACTIVIDAD MUSICAL → Aumentar brillo y alpha
        float musicBrightness = 1.0f;
        if (musicReactive) {
            // Agudos aumentan brillo general
            musicBrightness += musicTrebleIntensity * 0.5f;
            // Beats causan destellos brillantes
            musicBrightness += musicBeatFlash;
            // Energía aumenta alpha (más visible)
            currentAlpha = Math.min(1.0f, currentAlpha + musicEnergyBoost * 0.3f);
        }

        // Aplicar brillo musical al color
        float r = Math.min(1.0f, baseColor[0] * musicBrightness);
        float g = Math.min(1.0f, baseColor[1] * musicBrightness);
        float b = Math.min(1.0f, baseColor[2] * musicBrightness);

        GLES20.glUniform4f(uColorLoc, r, g, b, currentAlpha);
        GLES20.glUniform1f(uAlphaLoc, currentAlpha);

        // Impactos
        GLES20.glUniform3fv(uImpactPosLoc, MAX_IMPACTS, impactPositions, 0);
        GLES20.glUniform1fv(uImpactIntensityLoc, MAX_IMPACTS, impactIntensities, 0);

        // Salud (0.0 a 1.0)
        float healthPercent = (float)currentHealth / MAX_HEALTH;
        GLES20.glUniform1f(uHealthLoc, healthPercent);

        // 🎵 PLASMA: Enviar datos de música al shader
        float bassValue = musicReactive ? musicEnergyBoost : 0f;  // Usar energía como graves
        float trebleValue = musicReactive ? musicTrebleIntensity : 0f;
        float beatValue = musicReactive ? musicBeatFlash : 0f;

        GLES20.glUniform1f(uMusicBassLoc, bassValue);
        GLES20.glUniform1f(uMusicTrebleLoc, trebleValue);
        GLES20.glUniform1f(uMusicBeatLoc, beatValue);

        // Textura
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTextureLoc, 0);

        // Atributos
        int aPosLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        int aTexLoc = GLES20.glGetAttribLocation(programId, "a_TexCoord");

        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(aTexLoc);
        GLES20.glVertexAttribPointer(aTexLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

        // ════════════════════════════════════════════════════════════
        // ✅ USAR glDrawElements con indexBuffer de ProceduralSphere
        // ════════════════════════════════════════════════════════════
        // Cambio de glDrawArrays a glDrawElements para usar índices
        // Esto es más eficiente y correcto con ProceduralSphere
        indexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexBuffer.capacity(), GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        GLES20.glDisableVertexAttribArray(aPosLoc);
        GLES20.glDisableVertexAttribArray(aTexLoc);
    }

    // ===== IMPLEMENTACIÓN DE MUSICREACTIVE =====

    @Override
    public void onMusicData(float bassLevel, float midLevel, float trebleLevel,
                            float volumeLevel, float beatIntensity, boolean isBeat) {
        if (!musicReactive || isDestroyed) return;

        // AGUDOS → Aumentan brillo e intensidad del campo
        musicTrebleIntensity = trebleLevel;

        // ENERGÍA GENERAL → Aumenta visibilidad (alpha)
        musicEnergyBoost = (bassLevel * 0.4f + midLevel * 0.3f + trebleLevel * 0.3f);

        // BEATS → Destellos intensos y repentinos
        if (isBeat && beatIntensity > 0.5f) {
            musicBeatFlash = beatIntensity * 0.8f;  // Destello fuerte
            Log.v(TAG, "⚡ CAMPO DE FUERZA DESTELLO! Beat: " + String.format("%.2f", beatIntensity));
        } else {
            // Decay rápido del destello
            musicBeatFlash *= 0.85f;
        }

        // Log periódico de reactividad (cada 4 segundos)
        long now = System.currentTimeMillis();
        if (now - lastMusicLogTime > 4000 && musicEnergyBoost > 0.1f) {
            Log.d(TAG, String.format("⚡ [ForceField Reactivo] Treble:%.2f Energy:%.2f Brightness:%.0f%%",
                    musicTrebleIntensity, musicEnergyBoost, (musicTrebleIntensity * 50 + musicBeatFlash * 100)));
            lastMusicLogTime = now;
        }
    }

    @Override
    public void setMusicReactive(boolean enabled) {
        this.musicReactive = enabled;
        if (!enabled) {
            musicTrebleIntensity = 0f;
            musicBeatFlash = 0f;
            musicEnergyBoost = 0f;
        }
        Log.d(TAG, "[ForceField] Reactividad musical " + (enabled ? "ACTIVADA" : "DESACTIVADA"));
    }

    @Override
    public boolean isMusicReactive() {
        return musicReactive;
    }
}
