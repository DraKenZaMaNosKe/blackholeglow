package com.secret.blackholeglow;

import android.content.Context;
import android.util.Log;

import com.secret.blackholeglow.systems.EventBus;

import java.util.ArrayList;
import java.util.List;

/**
 * Sistema de Lluvia de Meteoritos
 * Gestiona múltiples meteoritos, colisiones y efectos especiales
 * Reacciona a la música aumentando la intensidad con el volumen
 */
public class MeteorShower implements SceneObject, CameraAware, MusicReactive {
    private static final String TAG = "depurar";

    // Configuración ULTRA OPTIMIZADA para dispositivos de gama baja
    private static final int MAX_METEORITOS = 2;  // Pool de 2 meteoritos (ULTRA performance)
    private static final int METEORITOS_ACTIVOS_MAX = 1;  // Máximo 1 activo (ULTRA optimizado)
    private static final float SPAWN_INTERVAL = 4.0f;  // Spawn cada 4 segundos (más lento)
    private static final float SPAWN_DISTANCE = 12.0f;  // Distancia de spawn

    // Pool de asteroides realistas
    private final List<AsteroideRealista> poolMeteorites = new ArrayList<>();
    private final List<AsteroideRealista> meteoritosActivos = new ArrayList<>();

    // Control de spawn
    private float tiempoDesdeUltimoSpawn = 0;
    private float intensidad = 1.0f;  // Multiplicador de frecuencia
    private boolean activo = true;

    // Referencias para colisiones
    private Planeta sol = null;
    private Planeta planetaOrbitante = null;
    private ForceField campoFuerza = null;  // Campo de fuerza del sol
    private List<SceneObject> objetosColisionables = new ArrayList<>();

    // Referencias para sistema de HP
    private HPBar hpBarSun = null;
    private HPBar hpBarForceField = null;

    // Efectos de impacto
    private List<ImpactEffect> efectosImpacto = new ArrayList<>();

    // Referencias
    private final Context context;
    private final TextureManager textureManager;
    private CameraController camera;
    private BatteryPowerBar powerBar;  // Para efectos basados en batería
    // NOTA: Efectos de pantalla ahora se comunican via EventBus
    private MeteorCountdownBar countdownBar;  // Barra visual de countdown

    // 🛸 REFERENCIA AL OVNI (para colisiones)
    private Spaceship3D ovniRef = null;

    // ⚡ OPTIMIZACIÓN: Lista reutilizable para evitar allocaciones en update()
    private final List<AsteroideRealista> paraRemover = new ArrayList<>();

    // ⚡ OPTIMIZACIÓN: Arrays estáticos para verificarColisiones (evita allocaciones)
    // 🌍 NUEVA POSICIÓN DE LA TIERRA (Y=1.8, centrada en X y Z)
    private static final float[] POS_TIERRA = {0f, 1.8f, 0f};
    private static final float[] POS_PLANETA_ORBITANTE = {3.2f, 0f, 0f};

    // 🌍 GRAVEDAD DE LA TIERRA - Atrae meteoritos cercanos
    private static final float GRAVITY_RANGE = 4.0f;      // Rango de influencia gravitacional
    private static final float GRAVITY_STRENGTH = 0.8f;   // Fuerza de la gravedad

    // Estadísticas
    private int totalMeteoritosLanzados = 0;
    private int totalImpactos = 0;

    // 🎮 SISTEMA DE ESTADÍSTICAS DEL JUGADOR
    private PlayerStats playerStats;

    // ⚡ BARRA DE COMBO Y LLUVIA DE METEORITOS ÉPICA
    private ComboBar comboBar;
    // 🌟 LLUVIA DE METEORITOS ÉPICA (COMBO x10)
    private boolean epicMeteorShowerActive = false;
    private float epicMeteorShowerDuration = 0f;
    private static final float EPIC_SHOWER_DURATION = 3.0f;  // 3 segundos de lluvia épica
    private static final int EPIC_METEOR_COUNT = 30;  // 30 meteoritos en la lluvia épica

    // ===== SISTEMA DE REACTIVIDAD MUSICAL =====
    private boolean musicReactive = true;
    private float musicIntensityBoost = 0f;  // Boost de intensidad por música

    // ===== 💥 SISTEMA DE METEORITOS A PANTALLA (GRIETAS) 💥 =====
    private float screenMeteorTimer = 0f;           // Tiempo desde último meteorito a pantalla
    private float screenMeteorInterval = 40f;       // Intervalo aleatorio (30-60 segundos)
    private final List<AsteroideRealista> screenDirectedMeteors = new ArrayList<>();  // Asteroides hacia pantalla

    public MeteorShower(Context context, TextureManager textureManager) {
        this.context = context;
        this.textureManager = textureManager;

        // 🎮 Inicializar sistema de estadísticas
        this.playerStats = PlayerStats.getInstance(context);

        // ⚡ Inicializar barra de combo
        this.comboBar = new ComboBar(context);

        // Inicializar pool con AsteroideRealista
        for (int i = 0; i < MAX_METEORITOS; i++) {
            AsteroideRealista m = new AsteroideRealista(context, textureManager);
            poolMeteorites.add(m);
        }

        // Inicializar efectos de impacto
        for (int i = 0; i < 5; i++) {
            efectosImpacto.add(new ImpactEffect());
        }

        Log.d(TAG, "[MeteorShower] Inicializado con pool de " + MAX_METEORITOS + " meteoritos");
    }

    /**
     * Establece la referencia a la barra de poder
     */
    public void setPowerBar(BatteryPowerBar powerBar) {
        this.powerBar = powerBar;
    }

    /**
     * Conecta el sistema de HP (Sol, Campo de Fuerza y sus barras)
     */
    public void setHPSystem(Planeta sol, ForceField forceField, HPBar hpBarSun, HPBar hpBarForceField) {
        this.sol = sol;
        this.campoFuerza = forceField;
        this.hpBarSun = hpBarSun;
        this.hpBarForceField = hpBarForceField;
        Log.d(TAG, "[MeteorShower] ✓ Sistema HP conectado");
    }

    /**
     * 💥 DEPRECATED: Los efectos de pantalla ahora se comunican via EventBus
     * Este metodo se mantiene por compatibilidad pero no hace nada
     * @deprecated Usar EventBus.SCREEN_IMPACT, EventBus.SCREEN_CRACK, EventBus.EARTH_IMPACT
     */
    @Deprecated
    public void setSceneRenderer(Object renderer) {
        Log.d(TAG, "[MeteorShower] 💥 Sistema de efectos ahora usa EventBus");
    }

    /**
     * 💥 Conecta la barra de countdown visual
     */
    public void setCountdownBar(MeteorCountdownBar bar) {
        this.countdownBar = bar;
        Log.d(TAG, "[MeteorShower] 💥 Barra de countdown conectada");
    }

    /**
     * 🛸 Conecta el OVNI para detección de colisiones
     */
    public void setOvni(Spaceship3D ovni) {
        this.ovniRef = ovni;
        Log.d(TAG, "[MeteorShower] 🛸 OVNI conectado para colisiones");
    }

    /**
     * Registra objetos para detectar colisiones
     */
    public void registrarObjetoColisionable(SceneObject objeto) {
        if (objeto instanceof Planeta) {
            Planeta p = (Planeta) objeto;
            // Detectar si es el sol o un planeta por su tamaño/posición
            // Por ahora asumimos el primero es el sol
            if (sol == null) {
                sol = p;
                Log.d(TAG, "[MeteorShower] Sol registrado para colisiones");
            } else if (planetaOrbitante == null) {
                planetaOrbitante = p;
                Log.d(TAG, "[MeteorShower] Planeta orbitante registrado para colisiones");
            }
        } else if (objeto instanceof ForceField) {
            campoFuerza = (ForceField) objeto;
            Log.d(TAG, "[MeteorShower] Campo de fuerza registrado para colisiones");
        }
        objetosColisionables.add(objeto);
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
        // Asignar cámara a todos los asteroides
        for (AsteroideRealista m : poolMeteorites) {
            m.setCameraController(camera);
        }
    }

    /**
     * 🎮 VERIFICA COLISIONES DE UN ASTEROIDE EXTERNO (del jugador)
     * Permite que PlayerWeapon delegue la lógica de colisiones a MeteorShower
     */
    public void verificarColisionMeteorito(AsteroideRealista m) {
        if (m.getEstado() == AsteroideRealista.Estado.ACTIVO) {
            verificarColisiones(m);
        }
    }

    @Override
    public void update(float deltaTime) {
        if (!activo) return;

        // 🎮 ACTUALIZAR SISTEMA DE COMBOS (timeout automático)
        playerStats.updateCombo();

        // ⚡ ACTUALIZAR BARRA DE COMBO Y RAYO LÁSER
        if (comboBar != null) {
            int currentCombo = playerStats.getCurrentCombo();
            comboBar.updateCombo(currentCombo, playerStats.getTotalScore());
            comboBar.update(deltaTime);

            // Si el combo se perdió, resetear la barra visual también
            if (currentCombo == 0 && comboBar.getCurrentCombo() > 0) {
                comboBar.resetCombo();
            }

            // Si el combo llega a x10, ¡ACTIVAR LLUVIA DE METEORITOS ÉPICA!
            if (comboBar.isLaserReady() && !epicMeteorShowerActive) {
                fireEpicMeteorShower();
            }
        }

        // 🌟 ACTUALIZAR LLUVIA DE METEORITOS ÉPICA
        if (epicMeteorShowerActive) {
            epicMeteorShowerDuration -= deltaTime;

            // Generar asteroides más frecuentemente durante la lluvia épica
            if (epicMeteorShowerDuration > 0) {
                // Lanzar 10 asteroides por segundo durante la lluvia épica
                if (tiempoDesdeUltimoSpawn > 0.1f) {  // Cada 0.1 segundos
                    for (int i = 0; i < 3; i++) {  // 3 asteroides a la vez
                        AsteroideRealista nuevo = lanzarMeteoritoEpico();
                        if (nuevo != null) {
                            meteoritosActivos.add(nuevo);
                        }
                    }
                    tiempoDesdeUltimoSpawn = 0;
                }
            } else {
                // Terminar la lluvia épica
                epicMeteorShowerActive = false;
                epicMeteorShowerDuration = 0;
                Log.d(TAG, "🌟 Lluvia de meteoritos épica terminada");
            }
        }

        // Actualizar tiempo de spawn
        tiempoDesdeUltimoSpawn += deltaTime;

        // Spawn de nuevos meteoritos con boost musical
        float effectiveIntensity = intensidad;
        if (musicReactive && musicIntensityBoost > 0) {
            effectiveIntensity *= (1.0f + musicIntensityBoost);
        }

        if (tiempoDesdeUltimoSpawn > SPAWN_INTERVAL / effectiveIntensity &&
            meteoritosActivos.size() < METEORITOS_ACTIVOS_MAX) {

            spawnMeteorito();
            tiempoDesdeUltimoSpawn = 0;
        }

        // 💥 SISTEMA DE METEORITO A PANTALLA 💥
        screenMeteorTimer += deltaTime;

        // Actualizar barra de countdown visual
        if (countdownBar != null) {
            countdownBar.setProgress(screenMeteorTimer, screenMeteorInterval);
        }

        if (screenMeteorTimer >= screenMeteorInterval && !poolMeteorites.isEmpty()) {
            spawnScreenMeteor();
            screenMeteorTimer = 0f;
            // Nuevo intervalo aleatorio entre 30-60 segundos
            screenMeteorInterval = 30f + (float)(Math.random() * 30f);
        }

        // Actualizar asteroides activos
        // ⚡ OPTIMIZACIÓN: Reutilizar lista en vez de crear nueva cada frame
        paraRemover.clear();

        for (AsteroideRealista m : meteoritosActivos) {
            // 🌍 APLICAR GRAVEDAD DE LA TIERRA antes de actualizar
            if (m.getEstado() == AsteroideRealista.Estado.ACTIVO) {
                aplicarGravedadTierra(m, deltaTime);
            }

            m.update(deltaTime);

            // 💥 VERIFICAR IMPACTO EN PANTALLA (asteroides dirigidos a pantalla)
            if (screenDirectedMeteors.contains(m) && m.getEstado() == AsteroideRealista.Estado.ACTIVO) {
                if (verificarImpactoPantalla(m)) {
                    paraRemover.add(m);
                    // NO hacer remove aquí, se hará después del loop
                    continue;  // No verificar otras colisiones
                }
            }

            // Verificar colisiones solo si está activo
            if (m.getEstado() == AsteroideRealista.Estado.ACTIVO) {
                verificarColisiones(m);

                // 🛸 VERIFICAR COLISIÓN CON OVNI
                if (ovniRef != null && !ovniRef.isDestroyed()) {
                    float[] pos = m.getPosicion();
                    float meteorRadius = m.getTamaño() * 0.5f;
                    if (ovniRef.checkMeteorCollision(pos[0], pos[1], pos[2], meteorRadius)) {
                        ovniRef.takeDamage();
                        m.desactivar();  // Desactivar el meteorito
                        Log.d(TAG, "🛸💥 Meteorito impactó al OVNI!");
                    }
                }
            }

            // Si está inactivo, devolverlo al pool
            if (!m.estaActivo()) {
                paraRemover.add(m);
            }
        }

        // Devolver al pool
        for (AsteroideRealista m : paraRemover) {
            meteoritosActivos.remove(m);
            screenDirectedMeteors.remove(m);  // Asegurar que se remueva de ambas listas
            poolMeteorites.add(m);
        }

        // Actualizar efectos de impacto
        for (ImpactEffect efecto : efectosImpacto) {
            efecto.update(deltaTime);
        }

        // 🛡️ ACTUALIZAR BARRA HP DEL FORCEFIELD (SIEMPRE)
        if (campoFuerza != null && hpBarForceField != null) {
            // Si está destruido, mostrar HP = 0
            if (campoFuerza.isDestroyed()) {
                hpBarForceField.setHealth(0);
            } else {
                // Si está vivo, actualizar con HP actual
                hpBarForceField.setHealth(campoFuerza.getCurrentHealth());
            }
        }

        // Log de estadísticas cada 5 segundos (simplificado)
        if ((int)(tiempoDesdeUltimoSpawn * 2) % 10 == 0 && tiempoDesdeUltimoSpawn > 0.1f) {
            Log.d(TAG, "[MeteorShower] Activos:" + meteoritosActivos.size() +
                      " | Lanzados:" + totalMeteoritosLanzados +
                      " | Impactos:" + totalImpactos);
        }
    }

    /**
     * Genera un nuevo asteroide
     * 🌍 ACTUALIZADO: Trayectorias más naturales hacia la Tierra (Y=1.8)
     */
    private void spawnMeteorito() {
        if (poolMeteorites.isEmpty()) return;

        AsteroideRealista m = poolMeteorites.remove(0);

        // Posición aleatoria en esfera alrededor de la escena
        float angulo1 = (float) (Math.random() * Math.PI * 2);
        float angulo2 = (float) (Math.random() * Math.PI);

        float x = SPAWN_DISTANCE * (float) (Math.sin(angulo2) * Math.cos(angulo1));
        float y = SPAWN_DISTANCE * (float) (Math.sin(angulo2) * Math.sin(angulo1));
        float z = SPAWN_DISTANCE * (float) Math.cos(angulo2);

        // Velocidad hacia la Tierra con variación natural
        // Más velocidad si hay más batería
        float powerBoost = powerBar != null ? powerBar.getPowerMultiplier() : 1.0f;
        float velocidadBase = (2.0f + (float) Math.random() * 3.0f) * powerBoost;

        // 🌍 TARGET: La Tierra está en Y=1.8 - Variación para trayectorias naturales
        // 70% de meteoritos van hacia la Tierra, 30% pasan cerca
        float targetBias = (float) Math.random();
        float targetX, targetY, targetZ;

        if (targetBias < 0.7f) {
            // 70% - Directo hacia la Tierra con pequeña variación
            targetX = POS_TIERRA[0] + (float)(Math.random() * 1.0 - 0.5);  // ±0.5
            targetY = POS_TIERRA[1] + (float)(Math.random() * 0.6 - 0.3);  // ±0.3 de Y=1.8
            targetZ = POS_TIERRA[2] + (float)(Math.random() * 1.0 - 0.5);  // ±0.5
        } else {
            // 30% - Pasan cerca pero no directos (más variado/natural)
            targetX = (float)(Math.random() * 3.0 - 1.5);   // -1.5 a 1.5
            targetY = 1.0f + (float)(Math.random() * 1.6);  // 1.0 a 2.6 (cerca del nivel de la Tierra)
            targetZ = (float)(Math.random() * 3.0 - 1.5);   // -1.5 a 1.5
        }

        float dx = targetX - x;
        float dy = targetY - y;
        float dz = targetZ - z;
        float dist = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);

        float vx = (dx / dist) * velocidadBase;
        float vy = (dy / dist) * velocidadBase;
        float vz = (dz / dist) * velocidadBase;

        // Tamaño VARIABLE (pequeños, medianos, grandes) - ✅ MÁS PEQUEÑOS QUE LA LUNA
        // 50% pequeños, 30% medianos, 20% grandes
        // Luna = 0.27, asteroides MAX = 0.20
        float sizeRoll = (float) Math.random();
        float tamaño;
        if (sizeRoll < 0.5f) {
            // Pequeños (50%)
            tamaño = 0.015f + (float) Math.random() * 0.025f;  // 0.015-0.04
        } else if (sizeRoll < 0.8f) {
            // Medianos (30%)
            tamaño = 0.04f + (float) Math.random() * 0.05f;  // 0.04-0.09
        } else {
            // Grandes (20%)
            tamaño = 0.09f + (float) Math.random() * 0.06f;  // 0.09-0.15
        }
        tamaño *= (0.9f + powerBoost * 0.2f);  // Boost de batería (MAX ~0.18)

        m.activar(x, y, z, vx, vy, vz, tamaño);
        meteoritosActivos.add(m);

        totalMeteoritosLanzados++;
        Log.d(TAG, "[MeteorShower] Meteorito #" + totalMeteoritosLanzados + " lanzado desde (" +
                   String.format("%.1f,%.1f,%.1f", x, y, z) + ")");
    }

    /**
     * 🚀 DISPARA UN ASTEROIDE CONTROLADO POR EL JUGADOR
     * Lanzado desde la parte inferior de la pantalla hacia el sol
     * PROTEGIDO contra crashes
     * @param power Potencia del disparo (0.0 - 1.0)
     */
    public void shootPlayerMeteor(float power) {
        try {
            if (poolMeteorites.isEmpty()) {
                Log.w(TAG, "[shootPlayerMeteor] ⚠️ Pool vacío - esperando reciclar asteroide");
                return;
            }

            // Validar poder
            if (power < 0.0f || power > 1.0f) {
                Log.w(TAG, "[shootPlayerMeteor] ⚠️ Poder inválido: " + power);
                return;
            }

            AsteroideRealista m = poolMeteorites.remove(0);

        // 🎯 POSICIÓN INICIAL: Desde la parte inferior-frontal de la pantalla
        // (En coordenadas 3D: abajo y hacia la cámara)
        float x = 0.0f;           // Centro horizontal
        float y = -3.0f;          // Abajo
        float z = 4.0f;           // Adelante (hacia la cámara)

        // 🚀 VELOCIDAD: Dirección hacia la Tierra (Y=1.8)
        // Velocidad base escalada por la potencia
        float velocidadBase = 5.0f + (power * 10.0f);  // 5-15 unidades/seg según potencia

        // 🌍 TARGET: La Tierra ahora está en Y=1.8
        float targetX = POS_TIERRA[0];  // 0.0
        float targetY = POS_TIERRA[1];  // 1.8
        float targetZ = POS_TIERRA[2];  // 0.0

        // Calcular vector de dirección
        float dx = targetX - x;
        float dy = targetY - y;
        float dz = targetZ - z;
        float dist = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);

        // Normalizar y aplicar velocidad
        float vx = (dx / dist) * velocidadBase;
        float vy = (dy / dist) * velocidadBase;
        float vz = (dz / dist) * velocidadBase;

        // 💪 TAMAÑO VARIABLE: Más grande con más potencia, pero con variación - ✅ MÁS PEQUEÑOS QUE LA LUNA
        // 50% pequeños, 30% medianos, 20% grandes
        // Luna = 0.27, asteroides MAX = 0.20
        float sizeRoll = (float) Math.random();
        float tamaño;
        if (sizeRoll < 0.5f) {
            // Pequeños (50%)
            tamaño = 0.02f + (float) Math.random() * 0.02f;  // 0.02-0.04
        } else if (sizeRoll < 0.8f) {
            // Medianos (30%)
            tamaño = 0.04f + (float) Math.random() * 0.04f;  // 0.04-0.08
        } else {
            // Grandes (20%)
            tamaño = 0.08f + (float) Math.random() * 0.04f;  // 0.08-0.12
        }
        // Aplicar boost de potencia
        tamaño *= (0.8f + power * 0.4f);  // 80%-120% según potencia (MAX ~0.14)

        // Activar el meteorito
        m.activar(x, y, z, vx, vy, vz, tamaño);
        meteoritosActivos.add(m);

        totalMeteoritosLanzados++;

        // 🎮 REGISTRAR DISPARO EN ESTADÍSTICAS
        playerStats.onMeteorLaunched();

        Log.d(TAG, "╔════════════════════════════════════════════════════════╗");
        Log.d(TAG, "║                                                        ║");
        Log.d(TAG, String.format("║   🚀 ASTEROIDE DEL JUGADOR DISPARADO! (%.0f%%)        ║", power * 100));
        Log.d(TAG, "║                                                        ║");
        Log.d(TAG, String.format("║   Posición: (%.1f, %.1f, %.1f)                        ║", x, y, z));
        Log.d(TAG, String.format("║   Velocidad: %.1f unidades/seg                       ║", velocidadBase));
        Log.d(TAG, String.format("║   Tamaño: %.3f                                        ║", tamaño));
        Log.d(TAG, "║                                                        ║");
        Log.d(TAG, "╚════════════════════════════════════════════════════════╝");
        } catch (Exception e) {
            Log.e(TAG, "✗ Error disparando asteroide del jugador: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 💥💥💥 LANZA UN ASTEROIDE HACIA LA PANTALLA DEL USUARIO 💥💥💥
     * El asteroide vuela directo hacia la cámara y causa grietas en la pantalla
     * VISIBLE: Se ve acercándose para crear SUSPENSO
     */
    private void spawnScreenMeteor() {
        if (poolMeteorites.isEmpty()) {
            Log.w(TAG, "[spawnScreenMeteor] ⚠️ Pool vacío");
            return;
        }

        AsteroideRealista m = poolMeteorites.remove(0);

        // 📍 POSICIÓN INICIAL: DENTRO DEL CAMPO DE VISIÓN
        // Cámara está en (4, 3, 6) mirando hacia (0, 0, 0)
        // El asteroide debe venir desde ADELANTE (Z negativo) hacia la cámara (Z=6)

        float spawnType = (float) Math.random();
        float x, y, z;

        // Spawn en posiciones VISIBLES dentro del frustum de la cámara
        // Rango visible aproximado: X(-3 a 3), Y(-2 a 4), Z(-5 a 5)

        if (spawnType < 0.25f) {
            // Desde la DERECHA (visible)
            x = 2.5f + (float)(Math.random() * 1.5f);  // 2.5 a 4.0
            y = (float)(Math.random() * 4f) - 1f;      // -1 a 3
            z = -4f - (float)(Math.random() * 2f);     // -4 a -6 (LEJOS, adelante)
        } else if (spawnType < 0.5f) {
            // Desde la IZQUIERDA (visible)
            x = -2.5f - (float)(Math.random() * 1.5f); // -2.5 a -4.0
            y = (float)(Math.random() * 4f) - 1f;      // -1 a 3
            z = -4f - (float)(Math.random() * 2f);     // -4 a -6 (LEJOS, adelante)
        } else if (spawnType < 0.75f) {
            // Desde ARRIBA (visible)
            x = (float)(Math.random() * 4f) - 2f;      // -2 a 2
            y = 3f + (float)(Math.random() * 2f);      // 3 a 5 (arriba)
            z = -4f - (float)(Math.random() * 2f);     // -4 a -6 (LEJOS, adelante)
        } else {
            // Desde el CENTRO (directo)
            x = (float)(Math.random() * 2f) - 1f;      // -1 a 1 (centro)
            y = (float)(Math.random() * 2f);           // 0 a 2
            z = -5f - (float)(Math.random() * 3f);     // -5 a -8 (MUY LEJOS)
        }

        // 🎯 OBJETIVO: La posición de la CÁMARA (para que vuele directo a la pantalla)
        float targetX = 4f + (float)(Math.random() * 0.5f) - 0.25f;  // Cerca de cámara X
        float targetY = 3f + (float)(Math.random() * 0.5f) - 0.25f;  // Cerca de cámara Y
        float targetZ = 6f + (float)(Math.random() * 0.3f);          // Hacia/pasando la cámara

        // 🚀 VELOCIDAD: MÁS LENTO para dar tiempo de verlo y crear SUSPENSO
        float velocidadBase = 4.0f + (float)(Math.random() * 2f);  // 4-6 unidades/seg (REDUCIDO)

        float dx = targetX - x;
        float dy = targetY - y;
        float dz = targetZ - z;
        float dist = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);

        float vx = (dx / dist) * velocidadBase;
        float vy = (dy / dist) * velocidadBase;
        float vz = (dz / dist) * velocidadBase;

        // 💪 TAMAÑO VARIABLE: MÁS GRANDE para que sea MUY VISIBLE - ✅ MÁS PEQUEÑOS QUE LA LUNA
        // 50% grandes, 30% muy grandes, 20% gigantes
        // Luna = 0.27, asteroides MAX = 0.25
        float sizeRoll = (float) Math.random();
        float tamaño;
        if (sizeRoll < 0.5f) {
            // Grandes (50%)
            tamaño = 0.10f + (float) Math.random() * 0.04f;  // 0.10-0.14
        } else if (sizeRoll < 0.8f) {
            // Muy grandes (30%)
            tamaño = 0.14f + (float) Math.random() * 0.05f;  // 0.14-0.19
        } else {
            // Gigantes (20%)
            tamaño = 0.19f + (float) Math.random() * 0.06f;  // 0.19-0.25
        }

        // Activar el meteorito
        m.activar(x, y, z, vx, vy, vz, tamaño);
        meteoritosActivos.add(m);
        screenDirectedMeteors.add(m);  // Marcar como dirigido a pantalla

        totalMeteoritosLanzados++;
        Log.d(TAG, "╔════════════════════════════════════════════════════════╗");
        Log.d(TAG, "║                                                        ║");
        Log.d(TAG, "║   💥💥💥 ASTEROIDE VISIBLE LANZADO! 💥💥💥          ║");
        Log.d(TAG, "║   ¡MIRA CÓMO SE ACERCA A LA PANTALLA!                ║");
        Log.d(TAG, "║                                                        ║");
        Log.d(TAG, String.format("║   Desde: (%.1f, %.1f, %.1f) [VISIBLE]                ║", x, y, z));
        Log.d(TAG, String.format("║   Hacia: (%.1f, %.1f, %.1f) [CÁMARA]                 ║", targetX, targetY, targetZ));
        Log.d(TAG, String.format("║   Velocidad: %.1f u/s (LENTO = SUSPENSO)            ║", velocidadBase));
        Log.d(TAG, String.format("║   Tamaño: %.3f (GRANDE Y VISIBLE)                    ║", tamaño));
        Log.d(TAG, String.format("║   Distancia: %.1f unidades                            ║", dist));
        Log.d(TAG, String.format("║   Tiempo aprox: %.1f segundos                         ║", dist / velocidadBase));
        Log.d(TAG, String.format("║   Próximo en: %.0f segundos                          ║", screenMeteorInterval));
        Log.d(TAG, "║                                                        ║");
        Log.d(TAG, "╚════════════════════════════════════════════════════════╝");
    }

    /**
     * 💥 VERIFICA SI UN ASTEROIDE DIRIGIDO A PANTALLA HA IMPACTADO
     * Cuando el asteroide alcanza la posición Z de la cámara, activa las grietas
     * @return true si impactó (para removerlo de la lista)
     */
    private boolean verificarImpactoPantalla(AsteroideRealista m) {
        if (camera == null) return false;

        float[] pos = m.getPosicion();

        // UMBRAL DE IMPACTO: Cuando el asteroide llega CERCA de la cámara
        // Cámara está en Z=6, impactar cuando llegue a Z >= 5.7
        // (Más cerca = más dramático, parece que va a golpear la pantalla de verdad)
        float cameraZ = 6.0f;  // Posición Z de la cámara
        float impactThreshold = cameraZ - 0.3f;  // Impactar 0.3 unidades antes (MÁS CERCA)

        if (pos[2] >= impactThreshold) {
            // ¡IMPACTO EN PANTALLA!

            // Calcular coordenadas de pantalla (0-1) basadas en posición 3D
            // La cámara mira hacia (0,0,0) desde (4,3,6)
            // Proyectar la posición XY del asteroide a coordenadas de pantalla

            // Mapear X: -4 a +4 → 0 a 1 (rango ajustado para mejor precisión)
            float screenX = (pos[0] + 4f) / 8f;
            screenX = Math.max(0f, Math.min(1f, screenX));  // Clamp 0-1

            // Mapear Y: -2 a +5 → 0 a 1 (invertido porque OpenGL Y+ es arriba)
            float screenY = 1f - ((pos[1] + 2f) / 7f);
            screenY = Math.max(0f, Math.min(1f, screenY));  // Clamp 0-1

            // Intensidad basada en tamaño del asteroide (0.25-0.40 → 0.8-0.95)
            // Los asteroides de pantalla son MÁS GRANDES, así que más intensos
            float sizeNormalized = (m.getTamaño() - 0.25f) / 0.15f;  // 0-1
            float intensity = 0.8f + sizeNormalized * 0.15f;
            intensity = Math.max(0.8f, Math.min(0.95f, intensity));  // Clamp 0.8-0.95

            // 💥💥💥 ACTIVAR GRIETAS EN LA PANTALLA via EventBus 💥💥💥
            EventBus.get().publish(EventBus.SCREEN_CRACK,
                new EventBus.EventData()
                    .put("x", screenX)
                    .put("y", screenY)
                    .put("intensity", intensity));

            // Marcar el asteroide como impactado
            m.impactar();

            totalImpactos++;
            Log.d(TAG, "╔════════════════════════════════════════════════════════╗");
            Log.d(TAG, "║                                                        ║");
            Log.d(TAG, "║   💥💥💥 ¡IMPACTO EN PANTALLA! 💥💥💥                ║");
            Log.d(TAG, "║   ¡EL ASTEROIDE GOLPEÓ LA PANTALLA!                   ║");
            Log.d(TAG, "║                                                        ║");
            Log.d(TAG, String.format("║   Posición 3D: (%.2f, %.2f, %.2f)                    ║", pos[0], pos[1], pos[2]));
            Log.d(TAG, String.format("║   Pantalla: (%.2f, %.2f)                             ║", screenX, screenY));
            Log.d(TAG, String.format("║   Tamaño: %.3f                                        ║", m.getTamaño()));
            Log.d(TAG, String.format("║   Intensidad: %.0f%%                                  ║", intensity * 100));
            Log.d(TAG, "║                                                        ║");
            Log.d(TAG, "╚════════════════════════════════════════════════════════╝");

            return true;  // Impactó, remover de lista
        }

        return false;  // Aún no impacta
    }

    /**
     * Verifica colisiones con objetos de la escena
     */
    private void verificarColisiones(AsteroideRealista m) {
        float[] posMeteorito = m.getPosicion();
        float radioMeteorito = m.getTamaño();

        // PRIORIDAD 1: Colisión con campo de fuerza (si existe y no está destruido)
        if (campoFuerza != null && !campoFuerza.isDestroyed()) {
            if (campoFuerza.containsPoint(posMeteorito[0], posMeteorito[1], posMeteorito[2])) {
                // ¡IMPACTO EN CAMPO DE FUERZA!
                m.impactar();
                campoFuerza.registerImpact(posMeteorito[0], posMeteorito[1], posMeteorito[2]);
                crearEfectoImpacto(posMeteorito[0], posMeteorito[1], posMeteorito[2], true);

                // ACTUALIZAR HP BAR del escudo
                if (hpBarForceField != null) {
                    hpBarForceField.setHealth(campoFuerza.getCurrentHealth());
                }

                // 💥 EFECTO DE IMPACTO EN PANTALLA (ESCUDO) - MÁS SUTIL via EventBus
                // Intensidad basada en tamaño del meteorito (0.05-0.20 → 0.15-0.3)
                float intensityShield = 0.15f + (radioMeteorito / 0.20f) * 0.15f;
                intensityShield = Math.min(0.3f, Math.max(0.15f, intensityShield));  // Clamp 0.15-0.3
                EventBus.get().publish(EventBus.SCREEN_IMPACT,
                    new EventBus.EventData().put("intensity", intensityShield));

                totalImpactos++;

                // 🎮 REGISTRAR IMPACTO EN ESTADÍSTICAS (campo de fuerza)
                int points = playerStats.onImpact(false);

                Log.d(TAG, "[MeteorShower] ¡¡IMPACTO EN CAMPO DE FUERZA!! HP: " +
                           campoFuerza.getCurrentHealth() + "/" + campoFuerza.getMaxHealth() +
                           " | +" + points + " pts");
                return;  // No verificar más colisiones
            }
        }

        // PRIORIDAD 2: Colisión con la TIERRA (si existe y no está muerta)
        if (sol != null && !sol.isDead()) {
            // ⚡ OPTIMIZACIÓN: Usa array estático en vez de crear nuevo
            float radioTierra = 0.5f;       // Tamaño de la Tierra (1.0 de escala)

            float distancia = calcularDistancia(posMeteorito, POS_TIERRA);

            if (distancia < (radioMeteorito + radioTierra)) {
                // ¡IMPACTO CON LA TIERRA!
                m.impactar();
                crearEfectoImpacto(posMeteorito[0], posMeteorito[1], posMeteorito[2], true);

                // 🌍💥 ACTIVAR EFECTO ÉPICO DE IMPACTO EN LA TIERRA via EventBus
                EventBus.get().publish(EventBus.EARTH_IMPACT,
                    new EventBus.EventData()
                        .put("x", posMeteorito[0])
                        .put("y", posMeteorito[1])
                        .put("z", posMeteorito[2]));

                // CAUSAR DAÑO A LA TIERRA
                sol.damage(1);  // 1 punto de daño por meteorito

                // ACTUALIZAR HP BAR de la Tierra
                if (hpBarSun != null) {
                    hpBarSun.setHealth(sol.getCurrentHealth());
                }

                // 🎮 REGISTRAR IMPACTO EN ESTADÍSTICAS (Tierra directa)
                int points = playerStats.onImpact(true);

                // 🔥 VERIFICAR SI LA TIERRA FUE DESTRUIDA
                // NOTA: El incremento de planetas destruidos se hace en SceneRenderer.onExplosion()
                // para mantener la sincronización con la actualización del contador visual

                // 💥💥 EFECTO DE IMPACTO EN PANTALLA (TIERRA) - MÁS INTENSO via EventBus
                // Intensidad basada en tamaño del meteorito (0.05-0.20 → 0.3-0.5)
                float intensityEarth = 0.3f + (radioMeteorito / 0.20f) * 0.2f;
                intensityEarth = Math.min(0.5f, Math.max(0.3f, intensityEarth));  // Clamp 0.3-0.5
                EventBus.get().publish(EventBus.SCREEN_IMPACT,
                    new EventBus.EventData().put("intensity", intensityEarth));

                totalImpactos++;
                Log.d(TAG, "[MeteorShower] 🌍💥 ¡¡IMPACTO EN LA TIERRA!! HP: " +
                           sol.getCurrentHealth() + "/" + sol.getMaxHealth() +
                           " | +" + points + " pts | Combo: x" + playerStats.getCurrentCombo());
                return;
            }
        }

        // PRIORIDAD 3: Colisión con planeta orbitante
        if (planetaOrbitante != null) {
            // Aquí necesitaríamos obtener la posición actual del planeta
            // ⚡ OPTIMIZACIÓN: Usa array estático en vez de crear nuevo
            float distanciaPlaneta = calcularDistancia(posMeteorito, POS_PLANETA_ORBITANTE);
            if (distanciaPlaneta < (radioMeteorito + 0.18f)) {
                m.impactar();
                crearEfectoImpacto(posMeteorito[0], posMeteorito[1], posMeteorito[2], false);
                Log.d(TAG, "[MeteorShower] ¡Impacto en planeta!");
            }
        }
    }

    /**
     * Calcula distancia entre dos puntos 3D
     */
    private float calcularDistancia(float[] p1, float[] p2) {
        float dx = p1[0] - p2[0];
        float dy = p1[1] - p2[1];
        float dz = p1[2] - p2[2];
        return (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    /**
     * 🌍 APLICA EFECTO DE GRAVEDAD DE LA TIERRA
     * Cuando un meteorito está dentro del rango gravitacional,
     * su velocidad es atraída gradualmente hacia la Tierra.
     * Esto crea un efecto natural de curvatura en la trayectoria.
     */
    private void aplicarGravedadTierra(AsteroideRealista m, float deltaTime) {
        float[] pos = m.getPosicion();
        float[] vel = m.getVelocidad();

        // Calcular distancia a la Tierra
        float dx = POS_TIERRA[0] - pos[0];
        float dy = POS_TIERRA[1] - pos[1];
        float dz = POS_TIERRA[2] - pos[2];
        float distancia = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);

        // Solo aplicar gravedad si está dentro del rango
        if (distancia < GRAVITY_RANGE && distancia > 0.1f) {
            // Normalizar dirección hacia la Tierra
            float nx = dx / distancia;
            float ny = dy / distancia;
            float nz = dz / distancia;

            // La gravedad es más fuerte mientras más cerca esté (ley del cuadrado inverso suavizada)
            // Factor de atracción: más fuerte cerca, más suave lejos
            float gravityFactor = GRAVITY_STRENGTH * (1.0f - (distancia / GRAVITY_RANGE));
            gravityFactor *= gravityFactor;  // Cuadrático para efecto más realista

            // Aplicar aceleración gravitacional a la velocidad
            float accelX = nx * gravityFactor * deltaTime;
            float accelY = ny * gravityFactor * deltaTime;
            float accelZ = nz * gravityFactor * deltaTime;

            // Actualizar velocidad del meteorito
            m.ajustarVelocidad(
                vel[0] + accelX,
                vel[1] + accelY,
                vel[2] + accelZ
            );
        }
    }

    /**
     * Crea efecto visual de impacto
     */
    private void crearEfectoImpacto(float x, float y, float z, boolean enSol) {
        for (ImpactEffect efecto : efectosImpacto) {
            if (!efecto.activo) {
                efecto.activar(x, y, z, enSol);
                break;
            }
        }
    }

    @Override
    public void draw() {
        // Dibujar todos los meteoritos activos (usar índice para thread-safety)
        for (int i = 0; i < meteoritosActivos.size(); i++) {
            try {
                meteoritosActivos.get(i).draw();
            } catch (IndexOutOfBoundsException e) {
                // La lista fue modificada durante la iteración, salir
                break;
            }
        }

        // Dibujar efectos de impacto (usar índice para thread-safety)
        for (int i = 0; i < efectosImpacto.size(); i++) {
            try {
                efectosImpacto.get(i).draw(camera);
            } catch (IndexOutOfBoundsException e) {
                break;
            }
        }

        // 🔥 DIBUJAR BARRA DE COMBO (UI overlay)
        // ⚠️ OCULTA VISUALMENTE - Funcionalidad activa pero sin UI
        // if (comboBar != null) {
        //     comboBar.draw();
        // }
    }

    /**
     * Controla la intensidad de la lluvia
     */
    public void setIntensidad(float intensidad) {
        this.intensidad = Math.max(0.1f, Math.min(5.0f, intensidad));
    }

    public void activar() { activo = true; }
    public void desactivar() { activo = false; }

    /**
     * Clase interna para efectos de impacto
     */
    private class ImpactEffect {
        boolean activo = false;
        float x, y, z;
        float tiempo = 0;
        float tamaño = 0;
        boolean enSol = false;
        float opacidad = 1;

        void activar(float x, float y, float z, boolean enSol) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.enSol = enSol;
            this.activo = true;
            this.tiempo = 0;
            this.tamaño = 0.1f;
            this.opacidad = 1;
        }

        void update(float dt) {
            if (!activo) return;

            tiempo += dt;
            tamaño += dt * 3.0f;  // Expansión rápida
            opacidad = 1.0f - (tiempo / 1.0f);  // Desvanecer en 1 segundo

            if (tiempo > 1.0f) {
                activo = false;
            }
        }

        void draw(CameraController camera) {
            if (!activo || camera == null) return;

            // Aquí dibujaríamos el efecto de onda expansiva
            // Por ahora es un placeholder
            // Podríamos usar un shader especial para esto
        }
    }

    // ===== IMPLEMENTACIÓN DE MUSICREACTIVE =====

    @Override
    public void onMusicData(float bassLevel, float midLevel, float trebleLevel,
                            float volumeLevel, float beatIntensity, boolean isBeat) {
        if (!musicReactive) return;

        // VOLUMEN GENERAL → Aumenta intensidad de la lluvia
        // Más música = más meteoritos
        musicIntensityBoost = volumeLevel * 1.5f;  // Hasta 150% más meteoritos

        // BEATS → Spawn instantáneo de meteoritos extra
        if (isBeat && beatIntensity > 0.7f && poolMeteorites.size() > 0) {
            // En beats fuertes, lanzar meteorito extra
            if (meteoritosActivos.size() < METEORITOS_ACTIVOS_MAX + 1) {  // Permitir uno extra
                spawnMeteorito();
                Log.v(TAG, "[MeteorShower] 🎵 BEAT SPAWN! Meteorito extra lanzado");
            }
        }
    }

    @Override
    public void setMusicReactive(boolean enabled) {
        this.musicReactive = enabled;
        if (!enabled) {
            musicIntensityBoost = 0f;
        }
        Log.d(TAG, "[MeteorShower] Reactividad musical " + (enabled ? "ACTIVADA" : "DESACTIVADA"));
    }

    @Override
    public boolean isMusicReactive() {
        return musicReactive;
    }

    /**
     * 🌟💥 DISPARA LA LLUVIA DE METEORITOS ÉPICA 💥🌟
     *
     * Cuando el combo llega a x10, se desata una lluvia masiva
     * de 30 meteoritos durante 3 segundos - ¡DESTRUCCIÓN TOTAL!
     */
    private void fireEpicMeteorShower() {
        if (comboBar == null) return;

        // Resetear combo en la barra
        comboBar.fireLaser(); // Usa el mismo método para resetear

        // ACTIVAR LLUVIA ÉPICA
        epicMeteorShowerActive = true;
        epicMeteorShowerDuration = EPIC_SHOWER_DURATION;
        tiempoDesdeUltimoSpawn = 0; // Resetear timer para spawn inmediato

        Log.d(TAG, "╔════════════════════════════════════════════╗");
        Log.d(TAG, "║                                            ║");
        Log.d(TAG, "║  🌟💥 LLUVIA DE METEORITOS ÉPICA! 💥🌟    ║");
        Log.d(TAG, "║  ¡COMBO x10 ACTIVADO!                      ║");
        Log.d(TAG, "║  30 METEORITOS CAYENDO DURANTE 3 SEGUNDOS  ║");
        Log.d(TAG, "║                                            ║");
        Log.d(TAG, "╚════════════════════════════════════════════╝");

        // Registrar evento especial en estadísticas
        playerStats.onSpecialAttack("EPIC_METEOR_SHOWER");
    }

    /**
     * 🎮 VERIFICA SI EL COMBO ESTÁ LISTO (x10) PARA DISPARO ÉPICO
     */
    public boolean isComboReady() {
        return comboBar != null && comboBar.isLaserReady();
    }

    /**
     * 🎮 RESETEA EL COMBO (cuando se dispara el ataque épico del jugador)
     */
    public void resetCombo() {
        if (comboBar != null) {
            comboBar.fireLaser();  // Usa el método existente para resetear
            Log.d(TAG, "[MeteorShower] Combo reseteado por disparo épico del jugador");
        }
    }

    /**
     * 🌟 LANZA UN ASTEROIDE ÉPICO (parte de la lluvia x10)
     * Asteroides más grandes, más rápidos y más destructivos
     */
    private AsteroideRealista lanzarMeteoritoEpico() {
        if (poolMeteorites.isEmpty()) return null;

        AsteroideRealista m = poolMeteorites.remove(0);

        // Posición aleatoria en esfera alrededor de la escena
        // Vienen desde TODAS las direcciones para máximo caos
        float angulo1 = (float) (Math.random() * Math.PI * 2);
        float angulo2 = (float) (Math.random() * Math.PI * 0.5); // Solo hemisferio superior

        float distance = SPAWN_DISTANCE * 0.7f; // Más cerca para impacto más rápido
        float x = distance * (float) (Math.sin(angulo2) * Math.cos(angulo1));
        float y = distance * (float) Math.abs(Math.sin(angulo2) * Math.sin(angulo1)); // Solo desde arriba
        float z = distance * (float) Math.cos(angulo2);

        // VELOCIDAD ÉPICA - Mucho más rápido que meteoritos normales
        float velocidadBase = 8.0f + (float) Math.random() * 4.0f; // 8-12 unidades/seg (RÁPIDO!)

        // 🌍 Apuntar directamente a la Tierra (Y=1.8) con pequeña variación
        float targetX = POS_TIERRA[0] + (float)(Math.random() * 0.5 - 0.25);  // Pequeña variación
        float targetY = POS_TIERRA[1] + (float)(Math.random() * 0.3 - 0.15);  // Cerca de Y=1.8
        float targetZ = POS_TIERRA[2] + (float)(Math.random() * 0.5 - 0.25);

        float dx = targetX - x;
        float dy = targetY - y;
        float dz = targetZ - z;
        float dist = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);

        float vx = (dx / dist) * velocidadBase;
        float vy = (dy / dist) * velocidadBase;
        float vz = (dz / dist) * velocidadBase;

        // TAMAÑO ÉPICO - Todos son GRANDES para máximo daño - ✅ MÁS PEQUEÑOS QUE LA LUNA
        // Luna = 0.27, asteroides MAX = 0.20
        float tamaño = 0.10f + (float) Math.random() * 0.10f;  // 0.10-0.20 (GRANDES!)

        m.activar(x, y, z, vx, vy, vz, tamaño);

        totalMeteoritosLanzados++;
        Log.d(TAG, "🌟 Asteroide ÉPICO #" + totalMeteoritosLanzados + " - Tamaño: " +
                   String.format("%.2f", tamaño) + " - Velocidad: " + String.format("%.1f", velocidadBase));

        return m;
    }
}