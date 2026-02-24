package com.secret.blackholeglow.core;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.secret.blackholeglow.TextureManager.MemoryTier;

/**
 * Watchdog de memoria que pollea RAM disponible y notifica cambios de nivel.
 * Singleton con hysteresis para evitar flapping entre niveles.
 *
 * USO:
 *   SceneHealthMonitor.get().start(listener);
 *   SceneHealthMonitor.get().stop();
 *   SceneHealthMonitor.get().getCurrentLevel();
 */
public class SceneHealthMonitor {
    private static final String TAG = "SceneHealthMonitor";

    private static SceneHealthMonitor instance;

    // Intervalos de polling (ms)
    private static final long INTERVAL_NORMAL_MS = 3000;
    private static final long INTERVAL_STRESSED_MS = 1500;

    // Hysteresis: lecturas consecutivas requeridas para cambiar nivel
    private static final int HYSTERESIS_COUNT = 2;

    private Handler handler;
    private MemoryPressureListener listener;
    private MemoryPressureLevel currentLevel = MemoryPressureLevel.NORMAL;
    private boolean running = false;

    // Hysteresis state
    private MemoryPressureLevel pendingLevel = MemoryPressureLevel.NORMAL;
    private int pendingCount = 0;

    public interface MemoryPressureListener {
        void onMemoryPressureChanged(MemoryPressureLevel level);
    }

    private SceneHealthMonitor() {}

    public static synchronized SceneHealthMonitor get() {
        if (instance == null) {
            instance = new SceneHealthMonitor();
        }
        return instance;
    }

    public static synchronized void reset() {
        if (instance != null) {
            instance.stop();
            instance = null;
        }
    }

    /**
     * Inicia el monitoreo periodico de memoria.
     */
    public void start(MemoryPressureListener listener) {
        this.listener = listener;
        if (running) return;

        handler = new Handler(Looper.getMainLooper());
        running = true;
        currentLevel = MemoryPressureLevel.NORMAL;
        pendingLevel = MemoryPressureLevel.NORMAL;
        pendingCount = 0;

        Log.d(TAG, "SceneHealthMonitor iniciado");
        handler.post(pollRunnable);
    }

    /**
     * Detiene el monitoreo.
     */
    public void stop() {
        running = false;
        if (handler != null) {
            handler.removeCallbacks(pollRunnable);
        }
        listener = null;
        Log.d(TAG, "SceneHealthMonitor detenido");
    }

    public MemoryPressureLevel getCurrentLevel() {
        return currentLevel;
    }

    /**
     * Fuerza una lectura inmediata (para diagnostico).
     */
    public MemoryPressureLevel checkNow() {
        long availMB = DeviceProfile.get().getAvailableRamMB();
        return classifyLevel(availMB);
    }

    // ═══════════════════════════════════════════════════════════════
    // POLLING
    // ═══════════════════════════════════════════════════════════════

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) return;

            long availMB = DeviceProfile.get().getAvailableRamMB();
            if (availMB < 0) {
                // ActivityManager no disponible, reintentar
                scheduleNext();
                return;
            }

            MemoryPressureLevel measured = classifyLevel(availMB);

            // Hysteresis: solo cambiar si N lecturas consecutivas muestran nuevo nivel
            if (measured != currentLevel) {
                if (measured == pendingLevel) {
                    pendingCount++;
                } else {
                    pendingLevel = measured;
                    pendingCount = 1;
                }

                if (pendingCount >= HYSTERESIS_COUNT) {
                    MemoryPressureLevel oldLevel = currentLevel;
                    currentLevel = measured;
                    pendingCount = 0;

                    Log.d(TAG, "Nivel de memoria: " + oldLevel + " -> " + currentLevel
                            + " (RAM disponible: " + availMB + "MB)");

                    if (listener != null) {
                        listener.onMemoryPressureChanged(currentLevel);
                    }
                }
            } else {
                // Nivel estable, reset pending
                pendingLevel = currentLevel;
                pendingCount = 0;
            }

            scheduleNext();
        }
    };

    private void scheduleNext() {
        if (!running || handler == null) return;
        long interval = (currentLevel == MemoryPressureLevel.NORMAL)
                ? INTERVAL_NORMAL_MS : INTERVAL_STRESSED_MS;
        handler.postDelayed(pollRunnable, interval);
    }

    /**
     * Clasifica el nivel de memoria segun umbrales tier-aware.
     *
     * LOW:    NORMAL >200MB, WARNING 100-200MB, CRITICAL <100MB
     * MEDIUM: NORMAL >300MB, WARNING 150-300MB, CRITICAL <150MB
     * HIGH:   NORMAL >400MB, WARNING 200-400MB, CRITICAL <200MB
     */
    private MemoryPressureLevel classifyLevel(long availMB) {
        MemoryTier tier = DeviceProfile.get().getMemoryTier();

        long warningThreshold;
        long criticalThreshold;

        switch (tier) {
            case LOW:
                warningThreshold = 200;
                criticalThreshold = 100;
                break;
            case HIGH:
                warningThreshold = 400;
                criticalThreshold = 200;
                break;
            default: // MEDIUM
                warningThreshold = 300;
                criticalThreshold = 150;
                break;
        }

        if (availMB < criticalThreshold) {
            return MemoryPressureLevel.CRITICAL;
        } else if (availMB < warningThreshold) {
            return MemoryPressureLevel.WARNING;
        } else {
            return MemoryPressureLevel.NORMAL;
        }
    }
}
