package com.secret.blackholeglow;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Sistema de Lluvia de Meteoritos
 * Gestiona múltiples meteoritos, colisiones y efectos especiales
 * Reacciona a la música aumentando la intensidad con el volumen
 */
public class MeteorShower implements SceneObject, CameraAware, MusicReactive {
    private static final String TAG = "depurar";

    // Configuración OPTIMIZADA para máximo rendimiento
    private static final int MAX_METEORITOS = 3;  // Pool de 3 meteoritos (performance)
    private static final int METEORITOS_ACTIVOS_MAX = 2;  // Máximo 2 activos simultáneos
    private static final float SPAWN_INTERVAL = 2.5f;  // Spawn cada 2.5 segundos
    private static final float SPAWN_DISTANCE = 12.0f;  // Distancia de spawn

    // Pool de meteoritos
    private final List<Meteorito> poolMeteorites = new ArrayList<>();
    private final List<Meteorito> meteoritosActivos = new ArrayList<>();

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
    private SceneRenderer sceneRenderer;  // Para efectos de impacto en pantalla
    private MeteorCountdownBar countdownBar;  // Barra visual de countdown

    // Estadísticas
    private int totalMeteoritosLanzados = 0;
    private int totalImpactos = 0;

    // ===== SISTEMA DE REACTIVIDAD MUSICAL =====
    private boolean musicReactive = true;
    private float musicIntensityBoost = 0f;  // Boost de intensidad por música

    // ===== 💥 SISTEMA DE METEORITOS A PANTALLA (GRIETAS) 💥 =====
    private float screenMeteorTimer = 0f;           // Tiempo desde último meteorito a pantalla
    private float screenMeteorInterval = 40f;       // Intervalo aleatorio (30-60 segundos)
    private final List<Meteorito> screenDirectedMeteors = new ArrayList<>();  // Meteoritos hacia pantalla

    public MeteorShower(Context context, TextureManager textureManager) {
        this.context = context;
        this.textureManager = textureManager;

        // Inicializar pool
        for (int i = 0; i < MAX_METEORITOS; i++) {
            Meteorito m = new Meteorito(context, textureManager);
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
     * 💥 Conecta el SceneRenderer para efectos de impacto en pantalla
     */
    public void setSceneRenderer(SceneRenderer renderer) {
        this.sceneRenderer = renderer;
        Log.d(TAG, "[MeteorShower] 💥 Sistema de impacto en pantalla conectado");
    }

    /**
     * 💥 Conecta la barra de countdown visual
     */
    public void setCountdownBar(MeteorCountdownBar bar) {
        this.countdownBar = bar;
        Log.d(TAG, "[MeteorShower] 💥 Barra de countdown conectada");
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
        // Asignar cámara a todos los meteoritos
        for (Meteorito m : poolMeteorites) {
            m.setCameraController(camera);
        }
    }

    @Override
    public void update(float deltaTime) {
        if (!activo) return;

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

        // Actualizar meteoritos activos
        List<Meteorito> paraRemover = new ArrayList<>();

        for (Meteorito m : meteoritosActivos) {
            m.update(deltaTime);

            // 💥 VERIFICAR IMPACTO EN PANTALLA (meteoritos dirigidos a pantalla)
            if (screenDirectedMeteors.contains(m) && m.getEstado() == Meteorito.Estado.CAYENDO) {
                if (verificarImpactoPantalla(m)) {
                    paraRemover.add(m);
                    screenDirectedMeteors.remove(m);
                    continue;  // No verificar otras colisiones
                }
            }

            // Verificar colisiones solo si está cayendo
            if (m.getEstado() == Meteorito.Estado.CAYENDO) {
                verificarColisiones(m);
            }

            // Si está inactivo, devolverlo al pool
            if (!m.estaActivo()) {
                paraRemover.add(m);
            }
        }

        // Devolver al pool
        for (Meteorito m : paraRemover) {
            meteoritosActivos.remove(m);
            screenDirectedMeteors.remove(m);  // Asegurar que se remueva de ambas listas
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
     * Genera un nuevo meteorito
     */
    private void spawnMeteorito() {
        if (poolMeteorites.isEmpty()) return;

        Meteorito m = poolMeteorites.remove(0);

        // Posición aleatoria en esfera alrededor de la escena
        float angulo1 = (float) (Math.random() * Math.PI * 2);
        float angulo2 = (float) (Math.random() * Math.PI);

        float x = SPAWN_DISTANCE * (float) (Math.sin(angulo2) * Math.cos(angulo1));
        float y = SPAWN_DISTANCE * (float) (Math.sin(angulo2) * Math.sin(angulo1));
        float z = SPAWN_DISTANCE * (float) Math.cos(angulo2);

        // Velocidad hacia el centro con algo de variación
        // Más velocidad si hay más batería
        float powerBoost = powerBar != null ? powerBar.getPowerMultiplier() : 1.0f;
        float velocidadBase = (2.0f + (float) Math.random() * 3.0f) * powerBoost;
        float targetX = (float) (Math.random() * 2 - 1);  // Punto cerca del centro
        float targetY = (float) (Math.random() * 2 - 1);
        float targetZ = (float) (Math.random() * 2 - 1);

        float dx = targetX - x;
        float dy = targetY - y;
        float dz = targetZ - z;
        float dist = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);

        float vx = (dx / dist) * velocidadBase;
        float vy = (dy / dist) * velocidadBase;
        float vz = (dz / dist) * velocidadBase;

        // Tamaño aleatorio (más grandes con más batería)
        float tamaño = (0.05f + (float) Math.random() * 0.1f) * (0.8f + powerBoost * 0.4f);

        m.activar(x, y, z, vx, vy, vz, tamaño);
        meteoritosActivos.add(m);

        totalMeteoritosLanzados++;
        Log.d(TAG, "[MeteorShower] Meteorito #" + totalMeteoritosLanzados + " lanzado desde (" +
                   String.format("%.1f,%.1f,%.1f", x, y, z) + ")");
    }

    /**
     * 🚀 DISPARA UN METEORITO CONTROLADO POR EL JUGADOR
     * Lanzado desde la parte inferior de la pantalla hacia el sol
     * PROTEGIDO contra crashes
     * @param power Potencia del disparo (0.0 - 1.0)
     */
    public void shootPlayerMeteor(float power) {
        try {
            if (poolMeteorites.isEmpty()) {
                Log.w(TAG, "[shootPlayerMeteor] ⚠️ Pool vacío - esperando reciclar meteorito");
                return;
            }

            // Validar poder
            if (power < 0.0f || power > 1.0f) {
                Log.w(TAG, "[shootPlayerMeteor] ⚠️ Poder inválido: " + power);
                return;
            }

            Meteorito m = poolMeteorites.remove(0);

        // 🎯 POSICIÓN INICIAL: Desde la parte inferior-frontal de la pantalla
        // (En coordenadas 3D: abajo y hacia la cámara)
        float x = 0.0f;           // Centro horizontal
        float y = -3.0f;          // Abajo
        float z = 4.0f;           // Adelante (hacia la cámara)

        // 🚀 VELOCIDAD: Dirección hacia el sol (centro en 0,0,0)
        // Velocidad base escalada por la potencia
        float velocidadBase = 5.0f + (power * 10.0f);  // 5-15 unidades/seg según potencia

        float targetX = 0.0f;  // Sol en el centro
        float targetY = 0.0f;
        float targetZ = 0.0f;

        // Calcular vector de dirección
        float dx = targetX - x;
        float dy = targetY - y;
        float dz = targetZ - z;
        float dist = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);

        // Normalizar y aplicar velocidad
        float vx = (dx / dist) * velocidadBase;
        float vy = (dy / dist) * velocidadBase;
        float vz = (dz / dist) * velocidadBase;

        // 💪 TAMAÑO: Más grande con más potencia
        float tamaño = 0.08f + (power * 0.12f);  // 0.08 - 0.20 según potencia

        // Activar el meteorito
        m.activar(x, y, z, vx, vy, vz, tamaño);
        meteoritosActivos.add(m);

        totalMeteoritosLanzados++;
        Log.d(TAG, "╔════════════════════════════════════════════════════════╗");
        Log.d(TAG, "║                                                        ║");
        Log.d(TAG, String.format("║   🚀 METEORITO DEL JUGADOR DISPARADO! (%.0f%%)        ║", power * 100));
        Log.d(TAG, "║                                                        ║");
        Log.d(TAG, String.format("║   Posición: (%.1f, %.1f, %.1f)                        ║", x, y, z));
        Log.d(TAG, String.format("║   Velocidad: %.1f unidades/seg                       ║", velocidadBase));
        Log.d(TAG, String.format("║   Tamaño: %.3f                                        ║", tamaño));
        Log.d(TAG, "║                                                        ║");
        Log.d(TAG, "╚════════════════════════════════════════════════════════╝");
        } catch (Exception e) {
            Log.e(TAG, "✗ Error disparando meteorito del jugador: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 💥💥💥 LANZA UN METEORITO HACIA LA PANTALLA DEL USUARIO 💥💥💥
     * El meteorito vuela directo hacia la cámara y causa grietas en la pantalla
     * VISIBLE: Se ve acercándose para crear SUSPENSO
     */
    private void spawnScreenMeteor() {
        if (poolMeteorites.isEmpty()) {
            Log.w(TAG, "[spawnScreenMeteor] ⚠️ Pool vacío");
            return;
        }

        Meteorito m = poolMeteorites.remove(0);

        // 📍 POSICIÓN INICIAL: DENTRO DEL CAMPO DE VISIÓN
        // Cámara está en (4, 3, 6) mirando hacia (0, 0, 0)
        // El meteorito debe venir desde ADELANTE (Z negativo) hacia la cámara (Z=6)

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

        // 💪 TAMAÑO: MÁS GRANDE para que sea MUY VISIBLE
        float tamaño = 0.25f + (float)(Math.random() * 0.15f);  // 0.25 - 0.40 (GRANDE!)

        // Activar el meteorito
        m.activar(x, y, z, vx, vy, vz, tamaño);
        meteoritosActivos.add(m);
        screenDirectedMeteors.add(m);  // Marcar como dirigido a pantalla

        totalMeteoritosLanzados++;
        Log.d(TAG, "╔════════════════════════════════════════════════════════╗");
        Log.d(TAG, "║                                                        ║");
        Log.d(TAG, "║   💥💥💥 METEORITO VISIBLE LANZADO! 💥💥💥          ║");
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
     * 💥 VERIFICA SI UN METEORITO DIRIGIDO A PANTALLA HA IMPACTADO
     * Cuando el meteorito alcanza la posición Z de la cámara, activa las grietas
     * @return true si impactó (para removerlo de la lista)
     */
    private boolean verificarImpactoPantalla(Meteorito m) {
        if (sceneRenderer == null || camera == null) return false;

        float[] pos = m.getPosicion();

        // UMBRAL DE IMPACTO: Cuando el meteorito llega CERCA de la cámara
        // Cámara está en Z=6, impactar cuando llegue a Z >= 5.7
        // (Más cerca = más dramático, parece que va a golpear la pantalla de verdad)
        float cameraZ = 6.0f;  // Posición Z de la cámara
        float impactThreshold = cameraZ - 0.3f;  // Impactar 0.3 unidades antes (MÁS CERCA)

        if (pos[2] >= impactThreshold) {
            // ¡IMPACTO EN PANTALLA!

            // Calcular coordenadas de pantalla (0-1) basadas en posición 3D
            // La cámara mira hacia (0,0,0) desde (4,3,6)
            // Proyectar la posición XY del meteorito a coordenadas de pantalla

            // Mapear X: -4 a +4 → 0 a 1 (rango ajustado para mejor precisión)
            float screenX = (pos[0] + 4f) / 8f;
            screenX = Math.max(0f, Math.min(1f, screenX));  // Clamp 0-1

            // Mapear Y: -2 a +5 → 0 a 1 (invertido porque OpenGL Y+ es arriba)
            float screenY = 1f - ((pos[1] + 2f) / 7f);
            screenY = Math.max(0f, Math.min(1f, screenY));  // Clamp 0-1

            // Intensidad basada en tamaño del meteorito (0.25-0.40 → 0.8-0.95)
            // Los meteoritos de pantalla son MÁS GRANDES, así que más intensos
            float sizeNormalized = (m.getTamaño() - 0.25f) / 0.15f;  // 0-1
            float intensity = 0.8f + sizeNormalized * 0.15f;
            intensity = Math.max(0.8f, Math.min(0.95f, intensity));  // Clamp 0.8-0.95

            // 💥💥💥 ACTIVAR GRIETAS EN LA PANTALLA 💥💥💥
            sceneRenderer.triggerScreenCrack(screenX, screenY, intensity);

            // Marcar el meteorito como impactado
            m.impactar();

            totalImpactos++;
            Log.d(TAG, "╔════════════════════════════════════════════════════════╗");
            Log.d(TAG, "║                                                        ║");
            Log.d(TAG, "║   💥💥💥 ¡IMPACTO EN PANTALLA! 💥💥💥                ║");
            Log.d(TAG, "║   ¡EL METEORITO GOLPEÓ LA PANTALLA!                   ║");
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
    private void verificarColisiones(Meteorito m) {
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

                // 💥 EFECTO DE IMPACTO EN PANTALLA (ESCUDO) - MÁS SUTIL
                // Intensidad basada en tamaño del meteorito (0.05-0.20 → 0.15-0.3)
                if (sceneRenderer != null) {
                    float intensity = 0.15f + (radioMeteorito / 0.20f) * 0.15f;
                    intensity = Math.min(0.3f, Math.max(0.15f, intensity));  // Clamp 0.15-0.3
                    sceneRenderer.triggerScreenImpact(intensity);
                }

                totalImpactos++;
                Log.d(TAG, "[MeteorShower] ¡¡IMPACTO EN CAMPO DE FUERZA!! HP: " +
                           campoFuerza.getCurrentHealth() + "/" + campoFuerza.getMaxHealth());
                return;  // No verificar más colisiones
            }
        }

        // PRIORIDAD 2: Colisión con el sol (si existe y no está muerto)
        if (sol != null && !sol.isDead()) {
            float[] posSol = {0, 0, 0};  // El sol está CENTRADO en el origen
            float radioSol = 0.4f;       // Tamaño reducido del sol

            float distancia = calcularDistancia(posMeteorito, posSol);

            if (distancia < (radioMeteorito + radioSol)) {
                // ¡IMPACTO CON EL SOL!
                m.impactar();
                crearEfectoImpacto(posMeteorito[0], posMeteorito[1], posMeteorito[2], true);

                // CAUSAR DAÑO AL SOL
                sol.damage(1);  // 1 punto de daño por meteorito

                // ACTUALIZAR HP BAR del sol
                if (hpBarSun != null) {
                    hpBarSun.setHealth(sol.getCurrentHealth());
                }

                // 💥💥 EFECTO DE IMPACTO EN PANTALLA (SOL) - MÁS SUTIL
                // Intensidad basada en tamaño del meteorito (0.05-0.20 → 0.2-0.4)
                if (sceneRenderer != null) {
                    float intensity = 0.2f + (radioMeteorito / 0.20f) * 0.2f;
                    intensity = Math.min(0.4f, Math.max(0.2f, intensity));  // Clamp 0.2-0.4
                    sceneRenderer.triggerScreenImpact(intensity);
                }

                totalImpactos++;
                Log.d(TAG, "[MeteorShower] ¡¡IMPACTO EN EL SOL!! HP: " +
                           sol.getCurrentHealth() + "/" + sol.getMaxHealth());
                return;
            }
        }

        // PRIORIDAD 3: Colisión con planeta orbitante
        if (planetaOrbitante != null) {
            // Aquí necesitaríamos obtener la posición actual del planeta
            // Por ahora simplificamos con órbita ampliada
            float distanciaPlaneta = calcularDistancia(posMeteorito, new float[]{3.2f, 0, 0});
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
        // Dibujar todos los meteoritos activos
        for (Meteorito m : meteoritosActivos) {
            m.draw();
        }

        // Dibujar efectos de impacto
        for (ImpactEffect efecto : efectosImpacto) {
            efecto.draw(camera);
        }
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
}