package com.secret.blackholeglow.util;

import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   🧵 ThreadUtils - Utilidades para manejo de threads                     ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  PROPÓSITO:                                                               ║
 * ║  Eliminar código duplicado de shutdown de ExecutorService que aparece    ║
 * ║  en múltiples clases (AbstractDownloadManager, MediaCodecVideoRenderer,  ║
 * ║  BackgroundWorker, etc.)                                                 ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public final class ThreadUtils {
    private static final String TAG = "ThreadUtils";

    private ThreadUtils() {
        // No instanciable
    }

    /**
     * Cierra un ExecutorService de forma segura.
     * Primero intenta shutdown graceful, luego fuerza si es necesario.
     *
     * @param executor ExecutorService a cerrar
     * @param timeoutSeconds Tiempo máximo de espera para shutdown graceful
     * @param tag Tag para logging (nombre del componente)
     */
    public static void shutdownExecutor(ExecutorService executor, int timeoutSeconds, String tag) {
        if (executor == null || executor.isShutdown()) {
            return;
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                Log.w(tag, "⚠️ Executor no terminó en " + timeoutSeconds + "s, forzando shutdown...");
                executor.shutdownNow();

                // Esperar un poco más después del shutdownNow
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    Log.e(tag, "❌ Executor no pudo ser terminado");
                }
            }
        } catch (InterruptedException e) {
            Log.w(tag, "⚠️ Interrupted durante shutdown, forzando...");
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Cierra un ExecutorService con timeout por defecto de 5 segundos.
     *
     * @param executor ExecutorService a cerrar
     * @param tag Tag para logging
     */
    public static void shutdownExecutor(ExecutorService executor, String tag) {
        shutdownExecutor(executor, 5, tag);
    }

    /**
     * Espera a que un thread termine con timeout.
     *
     * @param thread Thread a esperar
     * @param timeoutMs Timeout en milisegundos
     * @param tag Tag para logging
     */
    public static void joinThread(Thread thread, long timeoutMs, String tag) {
        if (thread == null || !thread.isAlive()) {
            return;
        }

        try {
            thread.join(timeoutMs);
            if (thread.isAlive()) {
                Log.w(tag, "⚠️ Thread " + thread.getName() + " no terminó, interrumpiendo...");
                thread.interrupt();
            }
        } catch (InterruptedException e) {
            Log.w(tag, "⚠️ Interrupted esperando thread " + thread.getName());
            thread.interrupt();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Ejecuta una tarea en el thread actual con timeout usando un thread auxiliar.
     * Si la tarea no termina en el tiempo especificado, continúa sin esperar.
     *
     * @param runnable Tarea a ejecutar
     * @param timeoutMs Timeout en milisegundos
     * @param tag Tag para logging
     */
    public static void runWithTimeout(Runnable runnable, long timeoutMs, String tag) {
        Thread worker = new Thread(runnable, tag + "-timeout-worker");
        worker.start();
        joinThread(worker, timeoutMs, tag);
    }

    /**
     * Duerme el thread actual de forma segura (captura InterruptedException).
     *
     * @param millis Milisegundos a dormir
     * @return true si durmió completo, false si fue interrumpido
     */
    public static boolean sleepSafely(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Verifica si el thread actual ha sido interrumpido y lanza excepción si es así.
     * Útil en loops largos para permitir cancelación.
     *
     * @throws InterruptedException si el thread fue interrumpido
     */
    public static void checkInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Thread interrumpido");
        }
    }
}
