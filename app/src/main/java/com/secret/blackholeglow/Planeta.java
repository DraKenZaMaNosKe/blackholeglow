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
    private final float orbitOffsetY;  // 📍 Offset vertical (para mover planetas arriba/abajo)
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

    // ===== 🌍 SISTEMA DE SINCRONIZACIÓN CON TIEMPO REAL =====
    private boolean useRealTimeRotation = false;   // Sincronizar rotación con hora del día
    private boolean useRealTimeOrbit = false;      // Sincronizar órbita con día del año
    private float realTimeRotationPeriodHours = 24.0f;  // Horas por rotación completa (24h = Tierra, 27*24h = Sol)
    private float realTimeOrbitPeriodHours = 365.25f * 24.0f;  // Horas por órbita completa (365.25 días = Tierra)
    private float timeAccelerationFactor = 1.0f;      // Tiempo REAL sin aceleración - Reloj Astronómico ⏰

    // ===== 🌙 SISTEMA DE ÓRBITA RELATIVA (para Luna orbitando Tierra) =====
    private Planeta parentPlanet = null;           // Planeta padre (ej: Tierra para Luna)
    private float[] currentOrbitalPosition = new float[3];  // Posición orbital actual {x, y, z}

    // ===== 🔍 DEBUG - Contador para logs periódicos de órbitas =====
    private int orbitDebugCounter = 0;
    private static final int ORBIT_DEBUG_INTERVAL = 60;  // Log cada 60 frames (~1 segundo a 60fps)

    // ===== ⚡ OPTIMIZACIÓN: Caché de tiempo para evitar Calendar.getInstance() cada frame =====
    private static java.util.Calendar cachedCalendar = java.util.Calendar.getInstance();
    private static long lastCalendarUpdate = 0;
    private static final long CALENDAR_UPDATE_INTERVAL = 100; // Actualizar cada 100ms (10 FPS para tiempo)
    private static int cachedHour = 0;
    private static int cachedMinute = 0;
    private static int cachedSecond = 0;
    private static int cachedMillis = 0;
    private static int cachedDayOfYear = 0;

    // ⚡ OPTIMIZACIÓN: Arrays reutilizables para evitar allocaciones en draw()
    private float[] reusableFinalColor = new float[4];
    private float[] reusableParentPos = new float[3];  // Para órbitas relativas (lunas)

    // Constantes mejoradas
    private static final float BASE_SCALE = 1.0f; // Escala base más grande
    private static final float SCALE_OSC_FREQ = 0.2f;
    private static final float MUSIC_SCALE_FACTOR = 0.10f;    // Factor de escala musical (10% máx - más sutil)
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
                   float orbitOffsetY,  // 📍 NUEVO: Offset vertical
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
        this.orbitOffsetY = orbitOffsetY;  // Guardar offset Y
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
        Log.d(TAG, "✨ Usando ESFERA PROCEDURAL OPTIMIZADA (576 tri)");
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // ⚡ OPTIMIZACIÓN: Nivel intermedio (576 tri vs 256 LowPoly vs 1024 Medium)
        // Balance perfecto: se ve redonda pero sigue siendo eficiente
        ProceduralSphere.Mesh mesh = ProceduralSphere.generateOptimized(1.0f);

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

    /**
     * ⚡ OPTIMIZACIÓN: Actualiza el caché de tiempo solo cuando es necesario
     * Evita llamar Calendar.getInstance() 60 veces por segundo
     */
    private static void updateTimeCache() {
        long now = System.currentTimeMillis();
        if (now - lastCalendarUpdate >= CALENDAR_UPDATE_INTERVAL) {
            cachedCalendar.setTimeInMillis(now);
            cachedHour = cachedCalendar.get(java.util.Calendar.HOUR_OF_DAY);
            cachedMinute = cachedCalendar.get(java.util.Calendar.MINUTE);
            cachedSecond = cachedCalendar.get(java.util.Calendar.SECOND);
            cachedMillis = cachedCalendar.get(java.util.Calendar.MILLISECOND);
            cachedDayOfYear = cachedCalendar.get(java.util.Calendar.DAY_OF_YEAR);
            lastCalendarUpdate = now;
        }
    }

    @Override
    public void update(float dt) {
        // Manejar respawn si está muerto
        updateRespawn(dt);

        if (!isDead) {
            // 🔥 DETECTAR ESTADO CRÍTICO (HP < 30%)
            float healthPercent = (float)currentHealth / maxHealth;
            isCritical = healthPercent < 0.3f && maxHealth > 0;

            // 🔥 ACTUALIZAR PARPADEO DE ADVERTENCIA (más lento, no acelerado)
            if (isCritical) {
                criticalFlashTimer += dt * 3.0f;  // Parpadea moderado (3x velocidad - menos acelerado)
            }

            // ⚡ OPTIMIZACIÓN: Actualizar caché de tiempo UNA VEZ (compartido entre todos los planetas)
            updateTimeCache();

            // ===== 🌍 ROTACIÓN - Sincronizada con tiempo real o animada =====
            if (useRealTimeRotation) {
                // MODO TIEMPO REAL ACELERADO: Usar valores cacheados
                // Calcular fracción del día (0.0 a 1.0) con precisión de milisegundos
                float dayFraction = (cachedHour + cachedMinute / 60.0f + cachedSecond / 3600.0f + cachedMillis / 3600000.0f) / realTimeRotationPeriodHours;

                // Aplicar aceleración de tiempo (120x = 1 minuto real = 1 hora simulada)
                dayFraction = (dayFraction * timeAccelerationFactor) % 1.0f;

                // Convertir a ángulo (0 a 360 grados)
                rotation = (dayFraction * 360.0f) % 360f;
            } else {
                // MODO ANIMADO: Rotación continua normal
                float currentSpinSpeed = spinSpeed;
                if (musicReactive && musicSpeedBoost > 0) {
                    currentSpinSpeed *= (1.0f + musicSpeedBoost);
                }
                rotation = (rotation + dt * currentSpinSpeed) % 360f;
            }

            // ===== 🪐 ÓRBITA - Sincronizada con tiempo real o animada =====
            if (useRealTimeOrbit && orbitRadiusX > 0 && orbitRadiusZ > 0) {
                // ⚡ OPTIMIZACIÓN: Usar valores cacheados (ya actualizados arriba)

                // Calcular tiempo actual según el período orbital
                float currentTime = 0;

                if (realTimeOrbitPeriodHours >= 24.0f) {
                    // Períodos de días (>= 24 horas): usar día del año
                    currentTime = (cachedDayOfYear - 1) * 24.0f + cachedHour + cachedMinute / 60.0f + cachedSecond / 3600.0f + cachedMillis / 3600000.0f;
                } else if (realTimeOrbitPeriodHours > 1.0f) {
                    // Períodos de múltiples horas (> 1 hora): usar horas del día
                    currentTime = cachedHour + cachedMinute / 60.0f + cachedSecond / 3600.0f + cachedMillis / 3600000.0f;
                } else if (realTimeOrbitPeriodHours == 1.0f) {
                    // Período de 1 hora exacta: usar solo minutos dentro de la hora actual
                    currentTime = cachedMinute / 60.0f + cachedSecond / 3600.0f + cachedMillis / 3600000.0f;
                } else if (realTimeOrbitPeriodHours >= 1.0f / 60.0f) {
                    // Períodos de minutos (>= 1 minuto): usar minutos Y segundos dentro de la hora
                    currentTime = cachedMinute / 60.0f + cachedSecond / 3600.0f + cachedMillis / 3600000.0f;
                } else {
                    // Períodos menores a 1 minuto: usar solo segundos
                    currentTime = cachedSecond / 3600.0f + cachedMillis / 3600000.0f;
                }

                // Calcular fracción de órbita (0.0 a 1.0) basada en período configurado
                float orbitFraction = (currentTime * timeAccelerationFactor / realTimeOrbitPeriodHours) % 1.0f;

                // Convertir a ángulo (0 a 2π)
                // IMPORTANTE: Negativo para movimiento en sentido de las manecillas del reloj
                orbitAngle = -orbitFraction * 2f * (float)Math.PI;

                // 🔍 DEBUG: Log periódico de cálculos orbitales (para verificar que Marte se mueve)
                orbitDebugCounter++;
                if (orbitDebugCounter >= ORBIT_DEBUG_INTERVAL) {
                    orbitDebugCounter = 0;
                    if (realTimeOrbitPeriodHours <= 24.0f) {  // Solo para planetas del reloj astronómico
                        Log.d(TAG, String.format("⏰ ÓRBITA [Período=%.2fh] Time=%.4f, Fraction=%.4f, Angle=%.2f° (%.3frad)",
                            realTimeOrbitPeriodHours, currentTime, orbitFraction,
                            Math.toDegrees(orbitAngle), orbitAngle));
                    }
                }
            } else if (orbitRadiusX > 0 && orbitRadiusZ > 0 && orbitSpeed > 0) {
                // MODO ANIMADO: Órbita continua normal
                float currentOrbitSpeed = orbitSpeed;
                if (musicReactive && musicSpeedBoost > 0) {
                    currentOrbitSpeed *= (1.0f + musicSpeedBoost);
                }
                orbitAngle = (orbitAngle + dt * currentOrbitSpeed) % (2f * (float)Math.PI);
            }

            // ✅ CRÍTICO: Mantener tiempo cíclico para evitar pérdida de precisión en float
            accumulatedTime = (accumulatedTime + dt) % 60.0f;
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

            // 🌙 Si tiene planeta padre, orbitar alrededor de él en lugar del Sol
            if (parentPlanet != null) {
                float[] parentPos = parentPlanet.getCurrentOrbitalPosition();
                ox += parentPos[0];  // Sumar posición X del padre
                oz += parentPos[2];  // Sumar posición Z del padre
                Log.v(TAG, String.format("🌙 Órbita relativa: parent=(%.2f, %.2f) local=(%.2f, %.2f)",
                    parentPos[0], parentPos[2], ox, oz));
            }

            // Guardar posición orbital actual (para lunas que dependan de este planeta)
            currentOrbitalPosition[0] = ox;
            currentOrbitalPosition[1] = orbitOffsetY;  // 📍 Incluir offset Y
            currentOrbitalPosition[2] = oz;

            Matrix.translateM(model, 0, ox, orbitOffsetY, oz);  // 📍 Aplicar offset Y

            Log.v(TAG, String.format("Órbita: x=%.2f z=%.2f angle=%.2f", ox, oz, orbitAngle));
        } else if (orbitOffsetY != 0) {
            // Planeta fijo en el centro pero con offset Y (ej: Sol elevado)
            Matrix.translateM(model, 0, 0, orbitOffsetY, 0);
            currentOrbitalPosition[0] = 0;
            currentOrbitalPosition[1] = orbitOffsetY;
            currentOrbitalPosition[2] = 0;
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
        // ⚡ OPTIMIZACIÓN: Usar array reutilizable en lugar de .clone()
        reusableFinalColor[0] = solidColor[0];
        reusableFinalColor[1] = solidColor[1];
        reusableFinalColor[2] = solidColor[2];
        reusableFinalColor[3] = solidColor[3];
        float finalAlpha = alpha;

        if (isCritical) {
            // Parpadeo de advertencia (oscila entre normal y rojo intenso)
            float flashValue = (float)Math.sin(criticalFlashTimer) * 0.5f + 0.5f;  // 0.0 - 1.0

            // Mezclar con rojo peligroso (constantes inline para evitar allocación)
            reusableFinalColor[0] = solidColor[0] * (1 - flashValue) + 1.0f * flashValue;
            reusableFinalColor[1] = solidColor[1] * (1 - flashValue) + 0.2f * flashValue;
            reusableFinalColor[2] = solidColor[2] * (1 - flashValue) + 0.0f * flashValue;

            // ✨ ALPHA SIEMPRE OPACO (sin transparencia, más realista)
            finalAlpha = 1.0f;  // El sol NO se hace transparente
        }

        GLES20.glUniform1i(uUseSolidColorLoc, useSolidColor ? 1 : 0);
        GLES20.glUniform4fv(uSolidColorLoc, 1, reusableFinalColor, 0);
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
            playerStats.updatePlanetHealth(currentHealth);
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

    // ===== 🌍 MÉTODOS DE SINCRONIZACIÓN CON TIEMPO REAL =====

    /**
     * Activa/desactiva la sincronización de rotación con la hora del día
     * @param enabled true para sincronizar con la hora actual
     */
    public void setRealTimeRotation(boolean enabled) {
        this.useRealTimeRotation = enabled;
        Log.d(TAG, "🌍 Rotación en tiempo real " + (enabled ? "ACTIVADA" : "DESACTIVADA"));
    }

    /**
     * Activa/desactiva la sincronización de órbita con el día del año
     * @param enabled true para sincronizar con el calendario
     */
    public void setRealTimeOrbit(boolean enabled) {
        this.useRealTimeOrbit = enabled;
        Log.d(TAG, "🪐 Órbita en tiempo real " + (enabled ? "ACTIVADA" : "DESACTIVADA"));
    }

    /**
     * Configura el período de rotación en horas para sincronización con tiempo real
     * @param hours Horas por rotación completa (24 = Tierra, 27*24 = Sol)
     */
    public void setRealTimeRotationPeriod(float hours) {
        this.realTimeRotationPeriodHours = hours;
        Log.d(TAG, "⏰ Período de rotación configurado: " + hours + " horas");
    }

    /**
     * Configura el período de órbita en horas para sincronización con tiempo real (RELOJ ASTRONÓMICO)
     * @param hours Horas por órbita completa (24 = Tierra-horas, 1 = Marte-minutos, 1/60 = Luna-segundos)
     */
    public void setRealTimeOrbitPeriod(float hours) {
        this.realTimeOrbitPeriodHours = hours;
        Log.d(TAG, "🪐 Período de órbita configurado: " + hours + " horas");
    }

    /**
     * Configura el factor de aceleración del tiempo
     * @param factor Multiplicador de velocidad (60 = 60x más rápido, 120 = 120x, etc.)
     *               120x = La Tierra rota cada 12 minutos, órbita cada ~3 horas
     */
    public void setTimeAccelerationFactor(float factor) {
        this.timeAccelerationFactor = factor;
        Log.d(TAG, "⚡ Aceleración de tiempo configurada: " + factor + "x");
        Log.d(TAG, "   • Rotación de 24h → " + (24 * 60 / factor) + " min reales");
        Log.d(TAG, "   • Órbita de 365d → " + (365 * 24 * 60 / factor) + " min reales (~" + (365 * 24 / factor) + "h)");
    }

    /**
     * 🌙 Configura un planeta padre para que este planeta orbite alrededor de él
     * Útil para crear lunas orbitando planetas (ej: Luna orbitando Tierra)
     * @param parent Planeta padre alrededor del cual orbitar (null = orbitar el Sol)
     */
    public void setParentPlanet(Planeta parent) {
        this.parentPlanet = parent;
        if (parent != null) {
            Log.d(TAG, "🌙 Planeta configurado para orbitar alrededor de otro planeta (sistema luna-planeta)");
        } else {
            Log.d(TAG, "🪐 Planeta configurado para orbitar alrededor del Sol (sistema normal)");
        }
    }

    /**
     * Obtiene la posición orbital actual del planeta en coordenadas del mundo
     * Útil para que otros objetos (lunas) puedan orbitar alrededor de este planeta
     * ⚡ OPTIMIZACIÓN: Copia valores a array destino en lugar de crear uno nuevo
     * @param dest Array destino donde copiar {x, y, z} (debe tener al menos 3 elementos)
     */
    public void getCurrentOrbitalPosition(float[] dest) {
        dest[0] = currentOrbitalPosition[0];
        dest[1] = currentOrbitalPosition[1];
        dest[2] = currentOrbitalPosition[2];
    }

    /**
     * ⚠️ DEPRECATED: Usa getCurrentOrbitalPosition(float[] dest) para evitar allocaciones
     * @return Array {x, y, z} con la posición actual (CREA NUEVO ARRAY - evitar en render loop)
     */
    public float[] getCurrentOrbitalPosition() {
        return new float[]{currentOrbitalPosition[0], currentOrbitalPosition[1], currentOrbitalPosition[2]};
    }
}