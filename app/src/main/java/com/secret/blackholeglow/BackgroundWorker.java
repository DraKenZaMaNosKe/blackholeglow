package com.secret.blackholeglow;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

/**
 * BackgroundWorker - Thread de background para tareas que NO necesitan GL
 *
 * CASOS DE USO:
 * 1. Sincronización con Firebase (red)
 * 2. Cálculos de física complejos
 * 3. Procesamiento de datos de audio
 * 4. Guardar estadísticas a disco
 * 5. Cualquier operación I/O
 *
 * VENTAJAS:
 * - Libera el GL Thread para rendering puro
 * - Evita jank/stuttering por operaciones blocking
 * - Handler permite programar tareas periódicas
 *
 * USO:
 *   // Ejecutar tarea en background
 *   BackgroundWorker.post(() -> {
 *       // Operación pesada aquí
 *   });
 *
 *   // Ejecutar en Main/UI thread después
 *   BackgroundWorker.postToMain(() -> {
 *       // Actualizar UI aquí
 *   });
 *
 *   // Tarea periódica
 *   BackgroundWorker.postDelayed(() -> {
 *       // Se ejecuta después de delay
 *   }, 1000);
 */
public final class BackgroundWorker {
    private static final String TAG = "BackgroundWorker";

    private static HandlerThread workerThread;
    private static Handler workerHandler;
    private static Handler mainHandler;
    private static boolean initialized = false;

    // Prevenir instanciación
    private BackgroundWorker() {}

    /**
     * Inicializa el worker thread.
     * Llamar una vez al inicio de la app (ej: en Application o Service).
     */
    public static synchronized void initialize() {
        if (initialized) return;

        workerThread = new HandlerThread("BlackHoleGlow-Worker", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        workerThread.start();
        workerHandler = new Handler(workerThread.getLooper());
        mainHandler = new Handler(Looper.getMainLooper());
        initialized = true;

        Log.d(TAG, "✓ BackgroundWorker initialized");
    }

    /**
     * Detiene el worker thread.
     * Llamar cuando la app/service se destruye.
     */
    public static synchronized void shutdown() {
        if (!initialized) return;

        workerHandler.removeCallbacksAndMessages(null);
        mainHandler.removeCallbacksAndMessages(null);
        workerThread.quitSafely();

        workerThread = null;
        workerHandler = null;
        mainHandler = null;
        initialized = false;

        Log.d(TAG, "✓ BackgroundWorker shutdown");
    }

    /**
     * Ejecuta una tarea en el thread de background.
     * THREAD-SAFE: Puede llamarse desde cualquier thread.
     *
     * @param task Runnable a ejecutar
     */
    public static void post(Runnable task) {
        if (!initialized) {
            Log.w(TAG, "BackgroundWorker not initialized, initializing now...");
            initialize();
        }

        if (workerHandler != null) {
            workerHandler.post(wrapWithErrorHandling(task));
        }
    }

    /**
     * Ejecuta una tarea en background después de un delay.
     *
     * @param task Runnable a ejecutar
     * @param delayMs Delay en milisegundos
     */
    public static void postDelayed(Runnable task, long delayMs) {
        if (!initialized) {
            initialize();
        }

        if (workerHandler != null) {
            workerHandler.postDelayed(wrapWithErrorHandling(task), delayMs);
        }
    }

    /**
     * Ejecuta una tarea en el Main/UI thread.
     * Útil para callbacks después de operaciones de background.
     *
     * @param task Runnable a ejecutar
     */
    public static void postToMain(Runnable task) {
        if (!initialized) {
            initialize();
        }

        if (mainHandler != null) {
            mainHandler.post(wrapWithErrorHandling(task));
        }
    }

    /**
     * Ejecuta una tarea en Main thread después de un delay.
     */
    public static void postToMainDelayed(Runnable task, long delayMs) {
        if (!initialized) {
            initialize();
        }

        if (mainHandler != null) {
            mainHandler.postDelayed(wrapWithErrorHandling(task), delayMs);
        }
    }

    /**
     * Cancela todas las tareas pendientes en el worker.
     */
    public static void cancelAll() {
        if (workerHandler != null) {
            workerHandler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * Cancela un runnable específico.
     */
    public static void cancel(Runnable task) {
        if (workerHandler != null) {
            workerHandler.removeCallbacks(task);
        }
        if (mainHandler != null) {
            mainHandler.removeCallbacks(task);
        }
    }

    /**
     * Verifica si estamos en el worker thread.
     */
    public static boolean isOnWorkerThread() {
        return workerThread != null && Thread.currentThread() == workerThread;
    }

    /**
     * Verifica si estamos en el Main thread.
     */
    public static boolean isOnMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    /**
     * Wrapper que captura excepciones para evitar crashes.
     */
    private static Runnable wrapWithErrorHandling(final Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Exception e) {
                Log.e(TAG, "Error in background task: " + e.getMessage(), e);
            }
        };
    }

    // ═══════════════════════════════════════════════════════════════
    // MÉTODOS DE CONVENIENCIA PARA TAREAS COMUNES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Ejecuta una tarea de Firebase en background.
     * La tarea debe verificar conexión internamente si es necesario.
     */
    public static void postFirebaseTask(Runnable task) {
        post(task);
    }

    /**
     * Programa una tarea periódica.
     *
     * @param task Tarea a ejecutar
     * @param intervalMs Intervalo entre ejecuciones
     * @return Runnable que puede usarse para cancelar
     */
    public static Runnable scheduleRepeating(final Runnable task, final long intervalMs) {
        final Runnable repeatingTask = new Runnable() {
            @Override
            public void run() {
                task.run();
                if (workerHandler != null) {
                    workerHandler.postDelayed(this, intervalMs);
                }
            }
        };

        post(repeatingTask);
        return repeatingTask;
    }
}
