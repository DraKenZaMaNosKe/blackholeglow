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

    // Estadísticas
    private int totalMeteoritosLanzados = 0;
    private int totalImpactos = 0;

    // ===== SISTEMA DE REACTIVIDAD MUSICAL =====
    private boolean musicReactive = true;
    private float musicIntensityBoost = 0f;  // Boost de intensidad por música

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

        // Actualizar meteoritos activos
        List<Meteorito> paraRemover = new ArrayList<>();

        for (Meteorito m : meteoritosActivos) {
            m.update(deltaTime);

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