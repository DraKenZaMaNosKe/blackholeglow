package com.secret.blackholeglow.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.video.VideoDownloadManager;

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
     * Prepara tareas segun el nombre del wallpaper
     */
    public void prepareTasksForScene(String sceneName) {
        tasks.clear();

        // Determinar que escena es y preparar tareas apropiadas
        if (sceneName == null) {
            prepareLabSceneTasks();
            return;
        }

        switch (sceneName) {
            case "Portal Cosmico":
            case "Laboratorio":
            case "LabScene":
                prepareLabSceneTasks();
                break;

            case "Fondo del Mar":
            case "Oceano":
            case "OceanFloorScene":
                prepareOceanSceneTasks();
                break;

            default:
                // Default: usar Lab
                prepareLabSceneTasks();
                break;
        }
    }

    /**
     * Prepara tareas para LabScene (Portal Cosmico)
     * Incluye descarga del video de nubes de fuego
     */
    public void prepareLabSceneTasks() {
        tasks.clear();

        // 1. VIDEO - Lo mas importante y pesado
        addVideoDownloadTask("Video Portal Cosmico", "cielovolando.mp4", 10);

        // 2. Texturas (placeholder - la escena carga sus propias texturas)
        addTextureTask("Preparando escena", R.drawable.preview_oceano_sc, 2);

        // Calcular total
        calculateTotalWeight();
        Log.d(TAG, "LabScene: " + tasks.size() + " tareas (peso: " + totalTasks + ")");
    }

    /**
     * Prepara tareas para OceanFloorScene (Fondo del Mar)
     * Incluye descarga del video del oceano
     */
    public void prepareOceanSceneTasks() {
        tasks.clear();

        // 1. VIDEO - Lo mas importante
        addVideoDownloadTask("Video Abyssia", "marZerg.mp4", 10);

        // 2. Texturas
        addTextureTask("Textura Pez", R.drawable.abyssal_lurker_texture, 2);

        // Calcular total
        calculateTotalWeight();
        Log.d(TAG, "OceanScene: " + tasks.size() + " tareas (peso: " + totalTasks + ")");
    }

    private void calculateTotalWeight() {
        totalTasks = 0;
        for (PreloadTask task : tasks) {
            totalTasks += task.weight;
        }
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

    /**
     * Agrega tarea de descarga de video desde Supabase
     * Si el video ya existe, la tarea completa inmediatamente
     */
    private void addVideoDownloadTask(String name, String videoFileName, int weight) {
        tasks.add(new PreloadTask(name, () -> {
            VideoDownloadManager downloader = VideoDownloadManager.getInstance(context);

            if (downloader.isVideoAvailable(videoFileName)) {
                Log.d(TAG, "Video ya descargado: " + videoFileName);
                return;
            }

            // Video no disponible - descargar
            Log.d(TAG, "Descargando video: " + videoFileName);
            boolean success = downloader.downloadVideoSync(videoFileName, percent -> {
                // Actualizar progreso de descarga
                String progressText = name + " (" + percent + "%)";
                mainHandler.post(() -> {
                    if (listener != null) {
                        listener.onProgressUpdate(completedTasks, totalTasks, progressText);
                    }
                });
            });

            if (!success) {
                Log.e(TAG, "Error descargando video: " + videoFileName);
            }
        }, weight));
    }
}
