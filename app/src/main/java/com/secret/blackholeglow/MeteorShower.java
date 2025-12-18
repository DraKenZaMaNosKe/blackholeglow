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

    // Configuración NATURAL - Asteroides ocasionales y realistas
    private static final int MAX_METEORITOS = 2;  // Pool de 2 meteoritos
    private static final int METEORITOS_ACTIVOS_MAX = 1;  // Máximo 1 activo a la vez
    private static final float SPAWN_INTERVAL = 12.0f;  // Spawn cada 12 segundos (más natural)
    private static final float SPAWN_DISTANCE = 15.0f;  // Distancia de spawn (más lejos)

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
    private List<SceneObject> objetosColisionables = new ArrayList<>();

    // Referencias para sistema de HP
    private HPBar hpBarSun = null;

    // Efectos de impacto
    private List<ImpactEffect> efectosImpacto = new ArrayList<>();

    // Referencias
    private final Context context;
    private final TextureManager textureManager;
    private CameraController camera;
    private BatteryPowerBar powerBar;  // Para efectos basados en batería
    // NOTA: Efectos de pantalla ahora se comunican via EventBus
    private MeteorCountdownBar countdownBar;  // Barra visual de countdown

    // 🛸 REFERENCIA AL OVNI REMOVIDA - UfoScout maneja sus propias colisiones

    // 🌍 REFERENCIA A LA TIERRA (para posición dinámica durante órbita)
    private TierraMeshy tierraRef = null;

    // 🛰️ REFERENCIA A LA ESTACIÓN ESPACIAL (para colisiones)
    private SpaceStation spaceStationRef = null;

    // ☀️ REFERENCIA AL SOL PROCEDURAL (para colisiones - alternativa a Planeta sol)
    private SolMeshy solMeshyRef = null;

    // ⚡ OPTIMIZACIÓN: Lista reutilizable para evitar allocaciones en update()
    private final List<AsteroideRealista> paraRemover = new ArrayList<>();

    // ⚡ OPTIMIZACIÓN: Arrays reutilizables para verificarColisiones (evita allocaciones)
    // 🌍 POSICIÓN DE LA TIERRA (se actualiza dinámicamente si hay tierraRef)
    private final float[] posTierra = {0f, 0.5f, -5.0f};
    private static final float[] POS_PLANETA_ORBITANTE = {3.2f, 0f, 0f};

    // 🌍 GRAVEDAD DE LA TIERRA - Atrae meteoritos cercanos
    private static final float GRAVITY_RANGE = 4.0f;      // Rango de influencia gravitacional
    private static final float GRAVITY_STRENGTH = 0.8f;   // Fuerza de la gravedad

    // Estadísticas
    private int totalMeteoritosLanzados = 0;
    private int totalImpactos = 0;

    // 🎮 SISTEMA DE ESTADÍSTICAS DEL JUGADOR
    private PlayerStats playerStats;

    // ⚡ BARRA DE COMBO (sin lluvia épica)
    private ComboBar comboBar;

    // ===== SISTEMA DE REACTIVIDAD MUSICAL =====
    private boolean musicReactive = true;
    private float musicIntensityBoost = 0f;  // Boost de intensidad por música

    // Sistema de meteoritos a pantalla DESACTIVADO
    // private float screenMeteorTimer = 0f;
    // private float screenMeteorInterval = 40f;
    // private final List<AsteroideRealista> screenDirectedMeteors = new ArrayList<>();

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
     * Conecta el sistema de HP (Sol y su barra)
     */
    public void setHPSystem(Planeta sol, HPBar hpBarSun) {
        this.sol = sol;
        this.hpBarSun = hpBarSun;
        Log.d(TAG, "[MeteorShower] ✓ Sistema HP conectado");
    }

    /**
     * 💥 DEPRECATED: Los efectos de pantalla ahora se comunican via EventBus
     * Este metodo se mantiene por compatibilidad pero no hace nada
     * @deprecated Usar EventBus.SCREEN_IMPACT, EventBus.SCREEN_CRACK, EventBus.EARTH_IMPACT
     */
    @Deprecated
    public void setWallpaperDirector(Object director) {
        Log.d(TAG, "[MeteorShower] 💥 Sistema de efectos ahora usa EventBus");
    }

    /**
     * 💥 Conecta la barra de countdown visual
     */
    public void setCountdownBar(MeteorCountdownBar bar) {
        this.countdownBar = bar;
        Log.d(TAG, "[MeteorShower] 💥 Barra de countdown conectada");
    }

    // setOvni() REMOVIDO - UfoScout maneja sus propias colisiones

    /**
     * 🌍 Conecta la Tierra para tracking dinámico de posición (órbita)
     */
    public void setTierra(TierraMeshy tierra) {
        this.tierraRef = tierra;
        Log.d(TAG, "[MeteorShower] 🌍 Tierra conectada para posición dinámica");
    }

    /**
     * 🛰️ Conecta la Estación Espacial para detección de colisiones
     */
    public void setSpaceStation(SpaceStation station) {
        this.spaceStationRef = station;
        Log.d(TAG, "[MeteorShower] 🛰️ Estación Espacial conectada para colisiones");
    }

    /**
     * ☀️ Conecta el Sol Procedural para detección de colisiones
     */
    public void setSolMeshy(SolMeshy solMeshy) {
        this.solMeshyRef = solMeshy;
        Log.d(TAG, "[MeteorShower] ☀️ Sol Meshy conectado para colisiones");
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

        // 🌍 ACTUALIZAR POSICIÓN DE LA TIERRA (si está orbitando)
        if (tierraRef != null) {
            posTierra[0] = tierraRef.getX();
            posTierra[1] = tierraRef.getY();
            posTierra[2] = tierraRef.getZ();
        }

        // 🎮 ACTUALIZAR SISTEMA DE COMBOS (timeout automático)
        playerStats.updateCombo();

        // ⚡ ACTUALIZAR BARRA DE COMBO (sin lluvia épica)
        if (comboBar != null) {
            int currentCombo = playerStats.getCurrentCombo();
            comboBar.updateCombo(currentCombo, playerStats.getTotalScore());
            comboBar.update(deltaTime);

            if (currentCombo == 0 && comboBar.getCurrentCombo() > 0) {
                comboBar.resetCombo();
            }
        }

        // Actualizar tiempo de spawn
        tiempoDesdeUltimoSpawn += deltaTime;

        // Spawn de meteoritos ocasionales (sin boost musical, sin lluvia)
        if (tiempoDesdeUltimoSpawn > SPAWN_INTERVAL &&
            meteoritosActivos.size() < METEORITOS_ACTIVOS_MAX) {

            spawnMeteorito();
            tiempoDesdeUltimoSpawn = 0;
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

            // Verificar colisiones solo si está activo
            if (m.getEstado() == AsteroideRealista.Estado.ACTIVO) {
                verificarColisiones(m);
                // 🛸 Colisiones con UfoScout se manejan en UfoScout.java
            }

            // Si está inactivo, devolverlo al pool
            if (!m.estaActivo()) {
                paraRemover.add(m);
            }
        }

        // Devolver al pool
        for (AsteroideRealista m : paraRemover) {
            meteoritosActivos.remove(m);
            poolMeteorites.add(m);
        }

        // Actualizar efectos de impacto
        for (ImpactEffect efecto : efectosImpacto) {
            efecto.update(deltaTime);
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

        // Velocidad hacia la Tierra - MÁS LENTA y natural
        // Los asteroides viajan lentamente por el espacio
        float powerBoost = powerBar != null ? powerBar.getPowerMultiplier() : 1.0f;
        float velocidadBase = (0.8f + (float) Math.random() * 1.2f) * powerBoost;  // 0.8-2.0 (muy lento)

        // 🌍 TARGET: La Tierra está en Y=1.8 - Variación para trayectorias naturales
        // 70% de meteoritos van hacia la Tierra, 30% pasan cerca
        float targetBias = (float) Math.random();
        float targetX, targetY, targetZ;

        if (targetBias < 0.7f) {
            // 70% - Directo hacia la Tierra con pequeña variación
            targetX = posTierra[0] + (float)(Math.random() * 1.0 - 0.5);  // ±0.5
            targetY = posTierra[1] + (float)(Math.random() * 0.6 - 0.3);  // ±0.3 de Y=1.8
            targetZ = posTierra[2] + (float)(Math.random() * 1.0 - 0.5);  // ±0.5
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

        // Tamaño PEQUEÑO y natural - asteroides diminutos en el espacio
        // 70% muy pequeños, 25% pequeños, 5% medianos
        float sizeRoll = (float) Math.random();
        float tamaño;
        if (sizeRoll < 0.70f) {
            // Muy pequeños (70%) - como rocas espaciales
            tamaño = 0.02f + (float) Math.random() * 0.02f;  // 0.02-0.04
        } else if (sizeRoll < 0.95f) {
            // Pequeños (25%)
            tamaño = 0.04f + (float) Math.random() * 0.03f;  // 0.04-0.07
        } else {
            // Medianos (5%) - raros
            tamaño = 0.07f + (float) Math.random() * 0.03f;  // 0.07-0.10
        }
        // Sin boost de batería para mantener tamaños naturales

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
        float targetX = posTierra[0];  // 0.0
        float targetY = posTierra[1];  // 1.8
        float targetZ = posTierra[2];  // 0.0

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

    // ═══════════════════════════════════════════════════════════════════
    // FUNCIONES DE LLUVIA DE METEORITOS Y PANTALLA - DESACTIVADAS
    // ═══════════════════════════════════════════════════════════════════
    // spawnScreenMeteor() - ELIMINADO
    // verificarImpactoPantalla() - ELIMINADO

    /**
     * Verifica colisiones con objetos de la escena
     */
    private void verificarColisiones(AsteroideRealista m) {
        float[] posMeteorito = m.getPosicion();
        float radioMeteorito = m.getTamaño();

        // PRIORIDAD 1: Colisión con la TIERRA (si existe y no está muerta)
        // NOTA: AsteroideRealista ahora maneja su propia colisión con la Tierra internamente
        // Pero MeteorShower necesita detectarla para aplicar daño y efectos
        if (sol != null && !sol.isDead()) {
            // Radio de colisión consistente con AsteroideRealista.EARTH_RADIUS (0.55f)
            float radioTierra = 0.55f;      // Mismo que EARTH_RADIUS en AsteroideRealista

            float distancia = calcularDistancia(posMeteorito, posTierra);

            // Detectar si está explotando (ya impactó) o está muy cerca
            if (distancia < (radioMeteorito + radioTierra) || m.isExploding()) {
                // ¡IMPACTO CON LA TIERRA!
                if (!m.isExploding()) {
                    m.impactar();  // Solo llamar impactar si no está ya explotando
                }
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
                // El incremento de planetas destruidos se hace en BatallaCosmicaScene.onExplosion()

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

        // PRIORIDAD 2: Colisión con planeta orbitante
        if (planetaOrbitante != null) {
            // Aquí necesitaríamos obtener la posición actual del planeta
            // ⚡ OPTIMIZACIÓN: Usa array estático en vez de crear nuevo
            float distanciaPlaneta = calcularDistancia(posMeteorito, POS_PLANETA_ORBITANTE);
            if (distanciaPlaneta < (radioMeteorito + 0.18f)) {
                m.impactar();
                crearEfectoImpacto(posMeteorito[0], posMeteorito[1], posMeteorito[2], false);
                Log.d(TAG, "[MeteorShower] ¡Impacto en planeta!");
                return;
            }
        }

        // PRIORIDAD 3: 🛰️ Colisión con ESTACIÓN ESPACIAL
        if (spaceStationRef != null) {
            float stationX = spaceStationRef.getX();
            float stationY = spaceStationRef.getY();
            float stationZ = spaceStationRef.getZ();
            float stationRadius = spaceStationRef.getCollisionRadius();

            // Calcular distancia al centro de la estación
            float dx = posMeteorito[0] - stationX;
            float dy = posMeteorito[1] - stationY;
            float dz = posMeteorito[2] - stationZ;
            float distToStation = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);

            if (distToStation < (radioMeteorito + stationRadius)) {
                // ¡IMPACTO EN ESTACIÓN ESPACIAL!
                m.impactar();
                crearEfectoImpacto(posMeteorito[0], posMeteorito[1], posMeteorito[2], false);

                totalImpactos++;
                Log.d(TAG, "[MeteorShower] 🛰️💥 ¡¡IMPACTO EN ESTACIÓN ESPACIAL!!");

                // Efecto de impacto visual via EventBus
                float intensityStation = 0.2f + (radioMeteorito / 0.20f) * 0.15f;
                intensityStation = Math.min(0.4f, Math.max(0.2f, intensityStation));
                EventBus.get().publish(EventBus.SCREEN_IMPACT,
                    new EventBus.EventData().put("intensity", intensityStation));
                return;
            }
        }

        // PRIORIDAD 4: ☀️ Colisión con SOL PROCEDURAL (SolMeshy)
        if (solMeshyRef != null) {
            float solX = solMeshyRef.getX();
            float solY = solMeshyRef.getY();
            float solZ = solMeshyRef.getZ();
            float solRadius = solMeshyRef.getScale() * 0.8f;  // Radio aproximado del sol

            // Calcular distancia al centro del sol
            float dx = posMeteorito[0] - solX;
            float dy = posMeteorito[1] - solY;
            float dz = posMeteorito[2] - solZ;
            float distToSol = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);

            if (distToSol < (radioMeteorito + solRadius)) {
                // ¡IMPACTO EN SOL!
                m.impactar();
                crearEfectoImpacto(posMeteorito[0], posMeteorito[1], posMeteorito[2], true);

                totalImpactos++;
                Log.d(TAG, "[MeteorShower] ☀️💥 ¡¡IMPACTO EN EL SOL!! Asteroide vaporizado!");

                // Efecto de impacto visual intenso via EventBus
                float intensitySol = 0.35f + (radioMeteorito / 0.20f) * 0.25f;
                intensitySol = Math.min(0.6f, Math.max(0.35f, intensitySol));
                EventBus.get().publish(EventBus.SCREEN_IMPACT,
                    new EventBus.EventData().put("intensity", intensitySol));
                return;
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
        float dx = posTierra[0] - pos[0];
        float dy = posTierra[1] - pos[1];
        float dz = posTierra[2] - pos[2];
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

    // ===== IMPLEMENTACIÓN DE MUSICREACTIVE (DESACTIVADA) =====
    // Los meteoritos ya no reaccionan a la música para mantener un ritmo natural

    @Override
    public void onMusicData(float bassLevel, float midLevel, float trebleLevel,
                            float volumeLevel, float beatIntensity, boolean isBeat) {
        // DESACTIVADO - Los asteroides aparecen a ritmo constante, no con la música
    }

    @Override
    public void setMusicReactive(boolean enabled) {
        this.musicReactive = false;  // Siempre desactivado
    }

    @Override
    public boolean isMusicReactive() {
        return false;  // Siempre desactivado
    }

    // ═══════════════════════════════════════════════════════════════════
    // FUNCIONES DE LLUVIA ÉPICA - ELIMINADAS
    // ═══════════════════════════════════════════════════════════════════
    // fireEpicMeteorShower() - ELIMINADO
    // isComboReady() - ELIMINADO
    // resetCombo() - ELIMINADO
    // lanzarMeteoritoEpico() - ELIMINADO
}