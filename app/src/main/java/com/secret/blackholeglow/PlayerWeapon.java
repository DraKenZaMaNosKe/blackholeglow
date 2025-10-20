package com.secret.blackholeglow;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * 🎮 SISTEMA DE ARMA DEL JUGADOR
 *
 * Sistema de disparo independiente para meteoritos controlados por el jugador.
 * Separado de MeteorShower para mantener una arquitectura limpia:
 * - MeteorShower = Lluvia ambiental de meteoritos
 * - PlayerWeapon = Disparos controlados por el jugador
 *
 * CARACTERÍSTICAS:
 * - Disparo único cuando se toca la pantalla
 * - Disparo múltiple épico cuando la barra de combos está llena (x10)
 * - Meteoritos de diferentes tamaños y trayectorias
 * - Integración con sistema de combos y estadísticas
 */
public class PlayerWeapon implements SceneObject, CameraAware {
    private static final String TAG = "depurar";

    // Configuración de disparos
    private static final int MAX_METEORITOS = 15;  // Pool suficiente para disparo épico
    private static final int EPIC_SHOT_COUNT = 7;  // 7 meteoritos en disparo épico

    // Pool de meteoritos del jugador
    private final List<Meteorito> poolMeteorites = new ArrayList<>();
    private final List<Meteorito> meteoritosActivos = new ArrayList<>();

    // Referencias
    private final Context context;
    private final TextureManager textureManager;
    private CameraController camera;

    // Referencias para colisiones (delegadas a MeteorShower)
    private MeteorShower meteorShower;

    // Estadísticas
    private int totalShotsFired = 0;
    private int epicShotsFired = 0;

    // Sistema de estadísticas del jugador
    private PlayerStats playerStats;

    public PlayerWeapon(Context context, TextureManager textureManager) {
        this.context = context;
        this.textureManager = textureManager;

        // Inicializar sistema de estadísticas
        this.playerStats = PlayerStats.getInstance(context);

        // Inicializar pool de meteoritos
        for (int i = 0; i < MAX_METEORITOS; i++) {
            Meteorito m = new Meteorito(context, textureManager);
            poolMeteorites.add(m);
        }

        Log.d(TAG, "[PlayerWeapon] ✓ Inicializado con pool de " + MAX_METEORITOS + " meteoritos");
    }

    /**
     * Conecta con MeteorShower para delegar el manejo de colisiones
     */
    public void setMeteorShower(MeteorShower meteorShower) {
        this.meteorShower = meteorShower;
        Log.d(TAG, "[PlayerWeapon] ✓ Conectado con MeteorShower para colisiones");
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
        // Asignar cámara a todos los meteoritos
        for (Meteorito m : poolMeteorites) {
            m.setCameraController(camera);
        }
    }

    /**
     * 🚀 DISPARO ÚNICO - Toque normal en pantalla
     * Lanza UN meteorito desde la parte inferior hacia el centro
     * @param power Potencia del disparo (0.0 - 1.0)
     */
    public void shootSingle(float power) {
        if (poolMeteorites.isEmpty()) {
            Log.w(TAG, "[PlayerWeapon] ⚠️ Pool vacío - esperando reciclar meteorito");
            return;
        }

        // Validar poder
        power = Math.max(0.0f, Math.min(1.0f, power));

        Meteorito m = poolMeteorites.remove(0);

        // Posición inicial: parte inferior-frontal de la pantalla
        float x = 0.0f;           // Centro horizontal
        float y = -3.0f;          // Abajo
        float z = 4.0f;           // Adelante (hacia la cámara)

        // Velocidad hacia el sol (centro en 0,0,0)
        float velocidadBase = 5.0f + (power * 10.0f);  // 5-15 unidades/seg

        float targetX = 0.0f;
        float targetY = 0.0f;
        float targetZ = 0.0f;

        // Vector de dirección normalizado
        float dx = targetX - x;
        float dy = targetY - y;
        float dz = targetZ - z;
        float dist = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);

        float vx = (dx / dist) * velocidadBase;
        float vy = (dy / dist) * velocidadBase;
        float vz = (dz / dist) * velocidadBase;

        // Tamaño variable según potencia
        float sizeRoll = (float) Math.random();
        float tamaño;
        if (sizeRoll < 0.5f) {
            tamaño = 0.08f + (float) Math.random() * 0.06f;  // 0.08-0.14
        } else if (sizeRoll < 0.8f) {
            tamaño = 0.14f + (float) Math.random() * 0.08f;  // 0.14-0.22
        } else {
            tamaño = 0.22f + (float) Math.random() * 0.10f;  // 0.22-0.32
        }
        tamaño *= (0.8f + power * 0.4f);  // Boost de potencia

        m.activar(x, y, z, vx, vy, vz, tamaño);

        // ⚠️ THREAD-SAFE: Sincronizar al agregar meteoritos
        synchronized (meteoritosActivos) {
            meteoritosActivos.add(m);
        }

        totalShotsFired++;
        playerStats.onMeteorLaunched();

        Log.d(TAG, String.format("[PlayerWeapon] 🚀 DISPARO ÚNICO #%d | Potencia: %.0f%% | Tamaño: %.3f",
                totalShotsFired, power * 100, tamaño));
    }

    /**
     * 🌟💥 DISPARO ÉPICO - Combo x10 activado
     * Lanza MÚLTIPLES meteoritos desde diferentes direcciones
     * Crea un bombardeo espectacular hacia el sol
     */
    public void shootEpic() {
        int meteoritosDisponibles = poolMeteorites.size();
        int meteoritosALanzar = Math.min(EPIC_SHOT_COUNT, meteoritosDisponibles);

        if (meteoritosALanzar == 0) {
            Log.w(TAG, "[PlayerWeapon] ⚠️ No hay meteoritos disponibles para disparo épico");
            return;
        }

        Log.d(TAG, "╔════════════════════════════════════════════════════════╗");
        Log.d(TAG, "║                                                        ║");
        Log.d(TAG, "║  🌟💥 DISPARO ÉPICO ACTIVADO! 💥🌟                   ║");
        Log.d(TAG, "║  ¡COMBO x10 ALCANZADO!                                 ║");
        Log.d(TAG, String.format("║  Lanzando %d meteoritos simultáneos...                 ║", meteoritosALanzar));
        Log.d(TAG, "║                                                        ║");
        Log.d(TAG, "╚════════════════════════════════════════════════════════╝");

        // Lanzar meteoritos desde diferentes ángulos
        for (int i = 0; i < meteoritosALanzar; i++) {
            Meteorito m = poolMeteorites.remove(0);

            // POSICIONES VARIADAS: Desde un semicírculo alrededor del jugador
            // Ángulo entre -90° y +90° (semicírculo frontal)
            float angulo = (float) Math.toRadians(-90 + (180.0 / (meteoritosALanzar - 1)) * i);

            // Radio de spawn
            float radio = 4.5f + (float) Math.random() * 1.0f;  // 4.5-5.5 unidades

            // Posición en semicírculo
            float x = radio * (float) Math.sin(angulo);
            float y = -2.5f + (float) Math.random() * 1.0f;  // Ligeramente variable en Y
            float z = 3.0f + radio * (float) Math.cos(angulo);

            // Todos apuntan al centro (sol)
            float targetX = (float) (Math.random() * 0.3f - 0.15f);  // Pequeña variación
            float targetY = (float) (Math.random() * 0.3f - 0.15f);
            float targetZ = (float) (Math.random() * 0.3f - 0.15f);

            // Velocidad ÉPICA - Muy rápido
            float velocidadBase = 12.0f + (float) Math.random() * 4.0f;  // 12-16 unidades/seg

            float dx = targetX - x;
            float dy = targetY - y;
            float dz = targetZ - z;
            float dist = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);

            float vx = (dx / dist) * velocidadBase;
            float vy = (dy / dist) * velocidadBase;
            float vz = (dz / dist) * velocidadBase;

            // Tamaños VARIADOS: Mezcla de pequeños, medianos y grandes
            float tamaño;
            float sizeType = i / (float) meteoritosALanzar;  // 0.0 a 1.0

            if (sizeType < 0.3f) {
                // 30% pequeños
                tamaño = 0.12f + (float) Math.random() * 0.06f;  // 0.12-0.18
            } else if (sizeType < 0.7f) {
                // 40% medianos
                tamaño = 0.18f + (float) Math.random() * 0.10f;  // 0.18-0.28
            } else {
                // 30% grandes
                tamaño = 0.28f + (float) Math.random() * 0.12f;  // 0.28-0.40
            }

            m.activar(x, y, z, vx, vy, vz, tamaño);

            // ⚠️ THREAD-SAFE: Sincronizar al agregar meteoritos
            synchronized (meteoritosActivos) {
                meteoritosActivos.add(m);
            }

            totalShotsFired++;
            playerStats.onMeteorLaunched();

            Log.d(TAG, String.format("  🌟 Meteorito épico %d/%d | Ángulo: %.0f° | Tamaño: %.3f | Velocidad: %.1f",
                    i + 1, meteoritosALanzar, Math.toDegrees(angulo), tamaño, velocidadBase));
        }

        epicShotsFired++;
        playerStats.onSpecialAttack("EPIC_PLAYER_SHOT");

        Log.d(TAG, String.format("[PlayerWeapon] ✓ Disparo épico #%d completado - Total de disparos: %d",
                epicShotsFired, totalShotsFired));
    }

    @Override
    public void update(float deltaTime) {
        // ⚠️ THREAD-SAFE: Usar índice descendente para evitar ConcurrentModificationException
        // Esto permite que otros threads agreguen meteoritos mientras iteramos
        synchronized (meteoritosActivos) {
            for (int i = meteoritosActivos.size() - 1; i >= 0; i--) {
                Meteorito m = meteoritosActivos.get(i);
                m.update(deltaTime);

                // Verificar colisiones delegando a MeteorShower
                if (meteorShower != null && m.getEstado() == Meteorito.Estado.CAYENDO) {
                    meteorShower.verificarColisionMeteorito(m);
                }

                // Si está inactivo, devolverlo al pool inmediatamente
                if (!m.estaActivo()) {
                    meteoritosActivos.remove(i);
                    poolMeteorites.add(m);
                }
            }
        }
    }

    @Override
    public void draw() {
        // ⚠️ THREAD-SAFE: Sincronizar al dibujar meteoritos
        synchronized (meteoritosActivos) {
            // Dibujar meteoritos activos
            for (int i = 0; i < meteoritosActivos.size(); i++) {
                try {
                    meteoritosActivos.get(i).draw();
                } catch (IndexOutOfBoundsException e) {
                    // Lista modificada durante iteración, salir del loop
                    break;
                }
            }
        }
    }

    /**
     * Obtiene la lista de meteoritos activos del jugador
     * Para que MeteorShower pueda verificar colisiones
     */
    public List<Meteorito> getMeteoritosActivos() {
        return meteoritosActivos;
    }

    /**
     * Retorna estadísticas del arma
     */
    public void printStats() {
        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║     🎮 ESTADÍSTICAS DEL ARMA 🎮       ║");
        Log.d(TAG, "╠════════════════════════════════════════╣");
        Log.d(TAG, String.format("║ 🚀 Disparos totales:     %-12d║", totalShotsFired));
        Log.d(TAG, String.format("║ 🌟 Disparos épicos:      %-12d║", epicShotsFired));
        Log.d(TAG, String.format("║ 💥 Meteoritos activos:   %-12d║", meteoritosActivos.size()));
        Log.d(TAG, String.format("║ 🔄 Pool disponible:      %-12d║", poolMeteorites.size()));
        Log.d(TAG, "╚════════════════════════════════════════╝");
    }

    public int getTotalShotsFired() { return totalShotsFired; }
    public int getEpicShotsFired() { return epicShotsFired; }
}
