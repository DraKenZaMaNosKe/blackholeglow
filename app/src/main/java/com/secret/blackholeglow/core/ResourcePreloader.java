package com.secret.blackholeglow.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.secret.blackholeglow.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * ResourcePreloader - Sistema de Precarga de Recursos
 *
 * Carga texturas, shaders y otros recursos necesarios para una escena
 * antes de mostrarla, reportando el progreso en tiempo real.
 *
 * RECURSOS QUE PRECARGA:
 * - Texturas (bitmaps en memoria)
 * - Shaders (validacion de archivos)
 * - Modelos 3D (verificacion)
 * - Assets varios
 */
public class ResourcePreloader {
    private static final String TAG = "ResourcePreloader";

    private final Context context;
    private PreloadListener listener;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private Handler mainHandler;

    private List<PreloadTask> tasks;
    private int totalTasks = 0;
    private int completedTasks = 0;
    private boolean isCancelled = false;

    /**
     * Listener para reportar progreso de carga
     */
    public interface PreloadListener {
        void onProgressUpdate(int progress, int total, String currentTask);
        void onPreloadComplete();
        void onPreloadError(String error);
    }

    /**
     * Tarea de precarga individual
     */
    private static class PreloadTask {
        String name;
        Runnable task;
        int weight;  // Peso relativo (algunas tareas son mas pesadas)

        PreloadTask(String name, Runnable task, int weight) {
            this.name = name;
            this.task = task;
            this.weight = weight;
        }
    }

    public ResourcePreloader(Context context) {
        this.context = context.getApplicationContext();
        this.tasks = new ArrayList<>();
        this.mainHandler = new Handler(context.getMainLooper());
    }

    public void setListener(PreloadListener listener) {
        this.listener = listener;
    }

    /**
     * Prepara las tareas de precarga para "Batalla Cosmica"
     */
    public void prepareBatallaCosmicaTasks() {
        tasks.clear();

        // ═══════════════════════════════════════════════════════════════
        // TEXTURAS - Las mas pesadas
        // ═══════════════════════════════════════════════════════════════
        addTextureTask("Fondo Universo", R.drawable.universo001, 3);
        addTextureTask("Textura Tierra", R.drawable.texturaplanetatierra, 3);
        addTextureTask("Textura Sol", R.drawable.textura_sol, 2);

        // ═══════════════════════════════════════════════════════════════
        // SHADERS - Validar que existen
        // ═══════════════════════════════════════════════════════════════
        addShaderTask("Shader Tierra", "shaders/tierra_vertex.glsl", "shaders/tierra_fragment.glsl", 1);
        addShaderTask("Shader Planeta", "shaders/planeta_vertex.glsl", "shaders/planeta_fragment.glsl", 1);
        addShaderTask("Shader Sol", "shaders/sol_procedural_vertex.glsl", "shaders/sol_procedural_fragment.glsl", 1);
        addShaderTask("Shader Background", "shaders/starry_vertex.glsl", "shaders/starry_fragment.glsl", 1);
        addShaderTask("Shader Meteoritos", "shaders/meteoro_vertex.glsl", "shaders/meteoro_fragment.glsl", 1);
        addShaderTask("Shader OVNI", "shaders/ovni_vertex.glsl", "shaders/ovni_fragment.glsl", 1);

        // ═══════════════════════════════════════════════════════════════
        // MODELOS 3D - Verificar existencia
        // ═══════════════════════════════════════════════════════════════
        addModelTask("Modelo Planeta", "planeta.obj", 2);
        addModelTask("Modelo OVNI", "ovni.obj", 2);
        addModelTask("Modelo Meteoro", "meteoro.obj", 1);

        // ═══════════════════════════════════════════════════════════════
        // SISTEMAS - Inicializacion ligera
        // ═══════════════════════════════════════════════════════════════
        addSystemTask("Sistema de Audio", this::preloadAudioSystem, 1);
        addSystemTask("Sistema de Particulas", this::preloadParticleSystem, 1);
        addSystemTask("UI Components", this::preloadUIComponents, 1);
        addSystemTask("Leaderboard Cache", this::preloadLeaderboardCache, 1);

        // Calcular total
        totalTasks = 0;
        for (PreloadTask task : tasks) {
            totalTasks += task.weight;
        }

        Log.d(TAG, "Preparadas " + tasks.size() + " tareas (peso total: " + totalTasks + ")");
    }

    /**
     * Inicia la precarga en background
     */
    public void startPreloading() {
        if (tasks.isEmpty()) {
            Log.e(TAG, "No hay tareas preparadas!");
            if (listener != null) {
                listener.onPreloadError("No hay tareas de precarga");
            }
            return;
        }

        isCancelled = false;
        completedTasks = 0;

        backgroundThread = new HandlerThread("ResourcePreloader");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());

        // Ejecutar tareas secuencialmente para mejor control
        backgroundHandler.post(this::executeNextTask);
    }

    private int currentTaskIndex = 0;

    private void executeNextTask() {
        if (isCancelled) {
            cleanup();
            return;
        }

        if (currentTaskIndex >= tasks.size()) {
            // Todas las tareas completadas
            notifyComplete();
            cleanup();
            return;
        }

        PreloadTask task = tasks.get(currentTaskIndex);

        // Notificar progreso
        notifyProgress(task.name);

        // Ejecutar tarea
        try {
            task.task.run();
            completedTasks += task.weight;
            Log.d(TAG, "✓ " + task.name + " (" + completedTasks + "/" + totalTasks + ")");
        } catch (Exception e) {
            Log.e(TAG, "✗ Error en " + task.name + ": " + e.getMessage());
            // Continuar con la siguiente tarea aunque falle una
        }

        // Pequena pausa para que la UI se actualice
        try {
            Thread.sleep(50);
        } catch (InterruptedException ignored) {}

        currentTaskIndex++;
        backgroundHandler.post(this::executeNextTask);
    }

    private void notifyProgress(String currentTask) {
        if (listener != null) {
            mainHandler.post(() -> {
                listener.onProgressUpdate(completedTasks, totalTasks, currentTask);
            });
        }
    }

    private void notifyComplete() {
        if (listener != null) {
            mainHandler.post(() -> {
                listener.onPreloadComplete();
            });
        }
    }

    private void cleanup() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            backgroundThread = null;
        }
        backgroundHandler = null;
        currentTaskIndex = 0;
    }

    public void cancel() {
        isCancelled = true;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPER METHODS PARA AGREGAR TAREAS
    // ═══════════════════════════════════════════════════════════════════════

    private void addTextureTask(String name, int resourceId, int weight) {
        tasks.add(new PreloadTask(name, () -> {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeResource(context.getResources(), resourceId, options);

                // Ahora cargar a baja resolucion para calentar cache
                options.inJustDecodeBounds = false;
                options.inSampleSize = 4;  // 1/4 del tamano
                Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
                if (bitmap != null) {
                    bitmap.recycle();  // Liberar - solo queremos calentar el cache
                }
            } catch (Exception e) {
                Log.w(TAG, "Error precargando textura: " + e.getMessage());
            }
        }, weight));
    }

    private void addShaderTask(String name, String vertexPath, String fragmentPath, int weight) {
        tasks.add(new PreloadTask(name, () -> {
            try {
                // Verificar que los shaders existen
                InputStream vertexStream = context.getAssets().open(vertexPath);
                vertexStream.close();

                InputStream fragmentStream = context.getAssets().open(fragmentPath);
                fragmentStream.close();
            } catch (IOException e) {
                Log.w(TAG, "Shader no encontrado: " + e.getMessage());
            }
        }, weight));
    }

    private void addModelTask(String name, String modelPath, int weight) {
        tasks.add(new PreloadTask(name, () -> {
            try {
                InputStream stream = context.getAssets().open(modelPath);
                // Leer primeros bytes para verificar
                byte[] buffer = new byte[1024];
                stream.read(buffer);
                stream.close();
            } catch (IOException e) {
                Log.w(TAG, "Modelo no encontrado: " + e.getMessage());
            }
        }, weight));
    }

    private void addSystemTask(String name, Runnable task, int weight) {
        tasks.add(new PreloadTask(name, task, weight));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TAREAS DE SISTEMAS
    // ═══════════════════════════════════════════════════════════════════════

    private void preloadAudioSystem() {
        // Simular inicializacion del sistema de audio
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {}
    }

    private void preloadParticleSystem() {
        // Simular precarga de sistema de particulas
        try {
            Thread.sleep(80);
        } catch (InterruptedException ignored) {}
    }

    private void preloadUIComponents() {
        // Simular precarga de componentes UI
        try {
            Thread.sleep(60);
        } catch (InterruptedException ignored) {}
    }

    private void preloadLeaderboardCache() {
        // Simular precarga del cache de leaderboard
        try {
            Thread.sleep(50);
        } catch (InterruptedException ignored) {}
    }
}
