// Planeta.java - VERSIÓN CORREGIDA
package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.util.ObjLoader;
import com.secret.blackholeglow.util.ProceduralSphere;
import com.secret.blackholeglow.util.TextureConfig;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.List;

/**
 * Planeta CORREGIDO - Ahora usa CameraController correctamente
 * Implementa MusicReactive para reaccionar a la música en tiempo real
 */
public class Planeta extends BaseShaderProgram implements SceneObject, CameraAware, MusicReactive {
    private static final String TAG = "Planeta";

    // Parámetros de configuración
    private final float uvScale;
    private final int textureId;
    private final float orbitRadiusX, orbitRadiusZ, orbitSpeed;
    private final float scaleAmplitude, instanceScale, spinSpeed;
    private final boolean useSolidColor;
    private final float[] solidColor;
    private final float alpha;
    private final Float scaleOscPercent;

    // Uniform locations
    private final int aPosLoc, aTexLoc;
    private final int uTexLoc, uUseSolidColorLoc, uSolidColorLoc, uAlphaLoc;
    private final int uUvScaleLoc;

    // Buffers y conteos
    private final FloatBuffer vertexBuffer, texCoordBuffer;
    private final ShortBuffer indexBuffer;
    private final int indexCount;

    // Estado dinámico
    private float orbitAngle = 0f, rotation = 0f, accumulatedTime = 0f;

    // Matrices temporales
    private final float[] model = new float[16];
    private final float[] mvp = new float[16];

    // ===== CAMERA CONTROLLER =====
    private CameraController camera;

    // ===== 💾 PLAYER STATS (para auto-guardar HP) =====
    private PlayerStats playerStats;

    // ===== SISTEMA DE VIDA Y RESPAWN =====
    private int maxHealth = 0;
    private int currentHealth = 0;
    private boolean isDead = false;
    private float respawnTimer = 0f;
    private static final float RESPAWN_DELAY = 3.0f;  // 3 segundos para respawn
    private float deathAnimationTime = 0f;

    // ===== 🔥 SISTEMA DE ADVERTENCIA Y EXPLOSIÓN ÉPICA 🔥 =====
    private float criticalFlashTimer = 0f;           // Temporizador para parpadeo
    private boolean isCritical = false;               // HP < 30%
    private boolean hasExploded = false;              // Ya explotó?
    private OnExplosionListener explosionListener;    // Callback para explosión

    // ===== SISTEMA DE REACTIVIDAD MUSICAL =====
    private boolean musicReactive = true;  // Activado por defecto
    private float musicBassBoost = 0f;     // Boost de escala por bajos
    private float musicSpeedBoost = 0f;    // Boost de velocidad orbital
    private float musicBeatPulse = 0f;     // Pulso por beats
    private long lastMusicLogTime = 0;     // Para logs periódicos

    // Constantes mejoradas
    private static final float BASE_SCALE = 1.0f; // Escala base más grande
    private static final float SCALE_OSC_FREQ = 0.2f;
    private static final float MUSIC_SCALE_FACTOR = 0.3f;    // Factor de escala musical (30% máx)
    private static final float MUSIC_SPEED_FACTOR = 2.0f;    // Factor de velocidad (2x máx)
    private static final float MUSIC_BEAT_FACTOR = 0.2f;     // Factor de pulso por beat (20%)

    // ===== 🔥 INTERFACE PARA EXPLOSIÓN ÉPICA 🔥 =====
    public interface OnExplosionListener {
        void onExplosion(float x, float y, float z, float intensity);
    }

    public void setOnExplosionListener(OnExplosionListener listener) {
        this.explosionListener = listener;
    }

    /**
     * Constructor
     */
    public Planeta(Context ctx,
                   TextureManager texMgr,
                   String vertexShaderAssetPath,
                   String fragmentShaderAssetPath,
                   int textureResId,
                   float orbitRadiusX,
                   float orbitRadiusZ,
                   float orbitSpeed,
                   float scaleAmplitude,
                   float instanceScale,
                   float spinSpeed,
                   boolean useSolidColor,
                   float[] solidColor,
                   float alpha,
                   Float scaleOscPercent,
                   float uvScale) {
        super(ctx, vertexShaderAssetPath, fragmentShaderAssetPath);

        this.uvScale = uvScale;
        this.orbitRadiusX = orbitRadiusX;
        this.orbitRadiusZ = orbitRadiusZ;
        this.orbitSpeed = orbitSpeed;
        this.scaleAmplitude = scaleAmplitude;
        this.instanceScale = instanceScale;
        this.spinSpeed = spinSpeed;
        this.useSolidColor = useSolidColor;
        this.solidColor = (solidColor != null) ? solidColor : new float[]{1f,1f,1f,1f};
        this.alpha = alpha;
        this.scaleOscPercent = scaleOscPercent;

        Log.d(TAG, String.format("Creando planeta: orbit(%.2f,%.2f) scale:%.2f spin:%.1f",
                orbitRadiusX, orbitRadiusZ, instanceScale, spinSpeed));

        // Habilitar depth test pero NO culling para evitar agujeros
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        // NO habilitar GL_CULL_FACE para ver todas las caras de la esfera

        // ════════════════════════════════════════════════════════════
        // ✅ CARGAR Y CONFIGURAR TEXTURA usando TextureConfig
        // ════════════════════════════════════════════════════════════
        textureId = texMgr.getTexture(textureResId);
        // Configurar como textura de planeta (REPEAT + mipmaps)
        TextureConfig.configure(textureId, TextureConfig.Type.PLANET);

        // ════════════════════════════════════════════════════════════
        // ✅ USAR ESFERA PROCEDURAL con UVs perfectos
        // ════════════════════════════════════════════════════════════
        // Migrado de planeta.obj a generación procedural para:
        //  - UVs perfectos sin seams
        //  - Mejor rendimiento
        //  - Texturas se ven correctamente
        // ════════════════════════════════════════════════════════════

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Log.d(TAG, "✨ Usando ESFERA PROCEDURAL (UVs perfectos)");
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        ProceduralSphere.Mesh mesh = ProceduralSphere.generateMedium(1.0f);

        vertexBuffer = mesh.vertexBuffer;
        texCoordBuffer = mesh.uvBuffer;

        Log.d(TAG, "✓ Planeta mesh preparada:");
        Log.d(TAG, "  Vértices: " + mesh.vertexCount);
        Log.d(TAG, "  Triángulos: " + (mesh.indexCount / 3));

        // ✅ ProceduralSphere ya incluye indexBuffer listo para usar
        indexBuffer = mesh.indexBuffer;
        indexCount = mesh.indexCount;

        // Obtener uniform locations
        aPosLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        aTexLoc = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        uTexLoc = GLES20.glGetUniformLocation(programId, "u_Texture");
        uUseSolidColorLoc = GLES20.glGetUniformLocation(programId, "u_UseSolidColor");
        uSolidColorLoc = GLES20.glGetUniformLocation(programId, "u_SolidColor");
        uAlphaLoc = GLES20.glGetUniformLocation(programId, "u_Alpha");
        uUvScaleLoc = GLES20.glGetUniformLocation(programId, "u_UvScale");

        Log.d(TAG, "Planeta inicializado correctamente");
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
        Log.d(TAG, "CameraController asignado al planeta");
    }

    @Override
    public void update(float dt) {
        // Manejar respawn si está muerto
        updateRespawn(dt);

        if (!isDead) {
            // 🔥 DETECTAR ESTADO CRÍTICO (HP < 30%)
            float healthPercent = (float)currentHealth / maxHealth;
            isCritical = healthPercent < 0.3f && maxHealth > 0;

            // 🔥 ACTUALIZAR PARPADEO DE ADVERTENCIA
            if (isCritical) {
                criticalFlashTimer += dt * 8.0f;  // Parpadea rápido (8x velocidad)
            }

            // Velocidad de rotación reactiva a la música
            float currentSpinSpeed = spinSpeed;
            if (musicReactive && musicSpeedBoost > 0) {
                currentSpinSpeed *= (1.0f + musicSpeedBoost);
            }
            rotation = (rotation + dt * currentSpinSpeed) % 360f;

            // Órbita con velocidad reactiva a la música
            if (orbitRadiusX > 0 && orbitRadiusZ > 0 && orbitSpeed > 0) {
                float currentOrbitSpeed = orbitSpeed;
                if (musicReactive && musicSpeedBoost > 0) {
                    currentOrbitSpeed *= (1.0f + musicSpeedBoost);
                }
                orbitAngle = (orbitAngle + dt * currentOrbitSpeed) % (2f * (float)Math.PI);
            }

            accumulatedTime += dt;
        }
    }

    @Override
    public void draw() {
        // No dibujar si está muerto
        if (isDead) return;

        if (camera == null) {
            Log.e(TAG, "ERROR: CameraController no asignado!");
            return;
        }

        useProgram();

        // IMPORTANTE: Desactivar culling para evitar agujeros en la esfera
        GLES20.glDisable(GLES20.GL_CULL_FACE);

        // Asegurar que depth test esté activo
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);

        // Habilitar blending para transparencia
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Fase de animación
        float phase = (accumulatedTime % 0.5f) * 2f * (float)Math.PI / 0.5f;
        setTime(phase);

        // Enviar factor de tiling
        if (uUvScaleLoc >= 0) {
            GLES20.glUniform1f(uUvScaleLoc, uvScale);
        }

        // ===== CONSTRUIR MATRIZ MODELO =====
        Matrix.setIdentityM(model, 0);

        // 1. Aplicar órbita (traslación)
        if (orbitRadiusX > 0 && orbitRadiusZ > 0) {
            float ox = orbitRadiusX * (float)Math.cos(orbitAngle);
            float oz = orbitRadiusZ * (float)Math.sin(orbitAngle);
            Matrix.translateM(model, 0, ox, 0, oz);

            Log.v(TAG, String.format("Órbita: x=%.2f z=%.2f angle=%.2f", ox, oz, orbitAngle));
        }

        // 2. Calcular escala final
        float finalScale = BASE_SCALE * instanceScale;

        // Añadir variación dinámica si está configurada
        if (scaleOscPercent != null) {
            float s = 0.5f + 0.5f * (float)Math.sin(accumulatedTime * SCALE_OSC_FREQ * 2f * Math.PI);
            float osc = scaleOscPercent + (1f - scaleOscPercent) * s;
            finalScale *= osc;
        }

        // REACTIVIDAD MUSICAL → Añadir boost de escala por bajos y beats
        if (musicReactive) {
            float musicScaleBoost = 1.0f + musicBassBoost + musicBeatPulse;
            finalScale *= musicScaleBoost;
        }

        // 3. Aplicar escala
        Matrix.scaleM(model, 0, finalScale, finalScale, finalScale);

        // 4. Aplicar rotación
        Matrix.rotateM(model, 0, rotation, 0, 1, 0);

        // ===== USAR CAMERACONTROLLER PARA MVP =====
        camera.computeMvp(model, mvp);

        // Enviar MVP al shader
        setMvpAndResolution(mvp, SceneRenderer.screenWidth, SceneRenderer.screenHeight);

        // Configurar textura
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTexLoc, 0);

        // 🔥 CONFIGURAR COLOR CON EFECTOS CRÍTICOS
        float[] finalColor = solidColor.clone();
        float finalAlpha = alpha;

        if (isCritical) {
            // Parpadeo de advertencia (oscila entre normal y rojo intenso)
            float flashValue = (float)Math.sin(criticalFlashTimer) * 0.5f + 0.5f;  // 0.0 - 1.0

            // Mezclar con rojo peligroso
            float[] dangerColor = new float[]{1.0f, 0.2f, 0.0f, 1.0f};  // Rojo-naranja intenso
            finalColor[0] = solidColor[0] * (1 - flashValue) + dangerColor[0] * flashValue;
            finalColor[1] = solidColor[1] * (1 - flashValue) + dangerColor[1] * flashValue;
            finalColor[2] = solidColor[2] * (1 - flashValue) + dangerColor[2] * flashValue;

            // Alpha también parpadea (más visible cuando está crítico)
            finalAlpha = alpha * (0.7f + flashValue * 0.3f);
        }

        GLES20.glUniform1i(uUseSolidColorLoc, useSolidColor ? 1 : 0);
        GLES20.glUniform4fv(uSolidColorLoc, 1, finalColor, 0);
        GLES20.glUniform1f(uAlphaLoc, finalAlpha);

        // Configurar atributos de vértices
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        if (aTexLoc >= 0) {
            texCoordBuffer.position(0);
            GLES20.glEnableVertexAttribArray(aTexLoc);
            GLES20.glVertexAttribPointer(aTexLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);
        }

        // Dibujar
        indexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        // Limpiar
        GLES20.glDisableVertexAttribArray(aPosLoc);
        if (aTexLoc >= 0) {
            GLES20.glDisableVertexAttribArray(aTexLoc);
        }
    }

    // ===== SISTEMA DE VIDA Y RESPAWN =====

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
        Log.d(TAG, "Salud máxima establecida: " + maxHealth);
    }

    public void damage(int amount) {
        if (isDead) return;

        currentHealth = Math.max(0, currentHealth - amount);
        Log.d(TAG, "Planeta dañado: " + currentHealth + "/" + maxHealth);

        // 💾 AUTO-GUARDAR HP en PlayerStats
        if (playerStats != null) {
            playerStats.updateSunHealth(currentHealth);
        }

        if (currentHealth <= 0) {
            die();
        }
    }

    private void die() {
        isDead = true;
        deathAnimationTime = 0f;

        // 🔥🔥🔥 EXPLOSIÓN ÉPICA 🔥🔥🔥
        if (!hasExploded && explosionListener != null) {
            // Calcular posición de explosión (centro del planeta)
            float explosionX = 0f;
            float explosionY = 0f;
            float explosionZ = 0f;

            if (orbitRadiusX > 0 && orbitRadiusZ > 0) {
                explosionX = orbitRadiusX * (float)Math.cos(orbitAngle);
                explosionZ = orbitRadiusZ * (float)Math.sin(orbitAngle);
            }

            // Intensidad MÁXIMA para el sol
            float intensity = 2.5f;  // Mucho más intenso que explosiones normales

            explosionListener.onExplosion(explosionX, explosionY, explosionZ, intensity);
            hasExploded = true;

            Log.d(TAG, "🔥🔥🔥 ¡¡EXPLOSIÓN ÉPICA DEL SOL!! 🔥🔥🔥 Intensidad: " + intensity);
        }

        Log.d(TAG, "¡¡PLANETA DESTRUIDO!!");
    }

    public void respawn() {
        isDead = false;
        currentHealth = maxHealth;
        respawnTimer = 0f;
        deathAnimationTime = 0f;
        hasExploded = false;         // Resetear flag de explosión
        isCritical = false;          // Ya no está crítico
        criticalFlashTimer = 0f;    // Resetear parpadeo
        Log.d(TAG, "Planeta RESPAWN - HP: " + maxHealth);
    }

    public boolean isDead() {
        return isDead;
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    /**
     * 💾 Establece el HP directamente (usado para cargar estado guardado)
     */
    public void setHealth(int health) {
        this.currentHealth = Math.max(0, Math.min(health, maxHealth));
        Log.d(TAG, "HP establecido: " + currentHealth + "/" + maxHealth);
    }

    /**
     * 💾 Inyecta PlayerStats para auto-guardar HP
     */
    public void setPlayerStats(PlayerStats stats) {
        this.playerStats = stats;
    }

    public void updateRespawn(float dt) {
        if (!isDead) return;

        respawnTimer += dt;
        deathAnimationTime += dt;

        if (respawnTimer >= RESPAWN_DELAY) {
            respawn();
        }
    }

    // ===== IMPLEMENTACIÓN DE MUSICREACTIVE =====

    @Override
    public void onMusicData(float bassLevel, float midLevel, float trebleLevel,
                            float volumeLevel, float beatIntensity, boolean isBeat) {
        if (!musicReactive || isDead) return;

        // BAJOS → Aumentan la escala del planeta (efecto de pulso)
        musicBassBoost = bassLevel * MUSIC_SCALE_FACTOR;

        // MEDIOS → Aumentan la velocidad orbital (planetas bailan más rápido)
        musicSpeedBoost = midLevel * MUSIC_SPEED_FACTOR;

        // BEATS → Pulso repentino adicional
        if (isBeat && beatIntensity > 0.5f) {
            musicBeatPulse = beatIntensity * MUSIC_BEAT_FACTOR;
            Log.v(TAG, "🎵 PLANETA BEAT! Intensidad: " + String.format("%.2f", beatIntensity));
        } else {
            // Decay suave del pulso
            musicBeatPulse *= 0.9f;
        }

        // Log periódico de reactividad (cada 4 segundos)
        long now = System.currentTimeMillis();
        if (now - lastMusicLogTime > 4000 && (bassLevel > 0.1f || midLevel > 0.1f)) {
            Log.d(TAG, String.format("🎵 [Planeta Reactivo] Bass:%.2f→Scale+%.0f%% Mid:%.2f→Speed+%.0f%%",
                    bassLevel, musicBassBoost * 100, midLevel, musicSpeedBoost * 100));
            lastMusicLogTime = now;
        }
    }

    @Override
    public void setMusicReactive(boolean enabled) {
        this.musicReactive = enabled;
        if (!enabled) {
            // Resetear valores cuando se desactiva
            musicBassBoost = 0f;
            musicSpeedBoost = 0f;
            musicBeatPulse = 0f;
        }
        Log.d(TAG, "Reactividad musical " + (enabled ? "ACTIVADA" : "DESACTIVADA"));
    }

    @Override
    public boolean isMusicReactive() {
        return musicReactive;
    }
}