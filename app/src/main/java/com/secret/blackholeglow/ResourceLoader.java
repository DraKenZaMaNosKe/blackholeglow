package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║   🚀 ResourceLoader - Gestor de Carga de Recursos GPU            ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * Carga recursos de manera progresiva con callbacks de progreso.
 * Diseñado para cargar en el GL Thread de manera controlada.
 *
 * FLUJO:
 * 1. Registrar tareas de carga (addTask)
 * 2. Iniciar carga (startLoading)
 * 3. Llamar loadNextStep() en cada frame desde GL Thread
 * 4. Recibir callbacks de progreso
 * 5. Callback final cuando todo está listo
 *
 * IMPORTANTE: Toda la carga GL debe ejecutarse en el GL Thread
 */
public class ResourceLoader {
    private static final String TAG = "ResourceLoader";

    // Estados del loader
    public enum LoaderState {
        IDLE,           // No hay carga pendiente
        LOADING,        // Cargando recursos
        COMPLETE,       // Carga completada
        ERROR           // Error durante la carga
    }

    private LoaderState state = LoaderState.IDLE;
    private Context context;
    private TextureManager textureManager;

    // Lista de tareas de carga
    private List<LoadTask> tasks = new ArrayList<>();
    private int currentTaskIndex = 0;
    private int totalTasks = 0;

    // Callbacks
    private OnProgressListener progressListener;
    private OnCompleteListener completeListener;
    private OnErrorListener errorListener;

    // Control de tiempo para no bloquear el frame
    private static final long MAX_LOAD_TIME_PER_FRAME_MS = 8;  // ~8ms máx por frame (deja tiempo para render)

    /**
     * Tarea individual de carga
     */
    public static class LoadTask {
        public enum TaskType {
            TEXTURE,        // Cargar textura
            SHADER,         // Compilar shader
            MODEL,          // Cargar modelo 3D
            SCENE_OBJECT,   // Inicializar objeto de escena
            CUSTOM          // Tarea personalizada
        }

        public TaskType type;
        public String name;           // Nombre descriptivo
        public int resourceId;        // ID del recurso (para texturas)
        public String assetPath;      // Path en assets (para shaders/modelos)
        public Runnable customTask;   // Tarea personalizada
        public float weight;          // Peso relativo (para calcular progreso)
        public boolean completed;

        // Constructor para texturas
        public static LoadTask texture(String name, int resourceId, float weight) {
            LoadTask task = new LoadTask();
            task.type = TaskType.TEXTURE;
            task.name = name;
            task.resourceId = resourceId;
            task.weight = weight;
            task.completed = false;
            return task;
        }

        // Constructor para shaders
        public static LoadTask shader(String name, String vertexPath, String fragmentPath, float weight) {
            LoadTask task = new LoadTask();
            task.type = TaskType.SHADER;
            task.name = name;
            task.assetPath = vertexPath + "|" + fragmentPath;  // Concatenamos paths
            task.weight = weight;
            task.completed = false;
            return task;
        }

        // Constructor para tarea personalizada
        public static LoadTask custom(String name, Runnable runnable, float weight) {
            LoadTask task = new LoadTask();
            task.type = TaskType.CUSTOM;
            task.name = name;
            task.customTask = runnable;
            task.weight = weight;
            task.completed = false;
            return task;
        }

        // Constructor para objeto de escena
        public static LoadTask sceneObject(String name, Runnable initializer, float weight) {
            LoadTask task = new LoadTask();
            task.type = TaskType.SCENE_OBJECT;
            task.name = name;
            task.customTask = initializer;
            task.weight = weight;
            task.completed = false;
            return task;
        }
    }

    public ResourceLoader(Context context, TextureManager textureManager) {
        this.context = context;
        this.textureManager = textureManager;
    }

    /**
     * Agrega una tarea de carga a la cola
     */
    public void addTask(LoadTask task) {
        if (state == LoaderState.LOADING) {
            Log.w(TAG, "No se puede agregar tareas mientras se está cargando");
            return;
        }
        tasks.add(task);
        totalTasks = tasks.size();
    }

    /**
     * Agrega múltiples tareas
     */
    public void addTasks(List<LoadTask> newTasks) {
        for (LoadTask task : newTasks) {
            addTask(task);
        }
    }

    /**
     * Limpia todas las tareas
     */
    public void clearTasks() {
        if (state == LoaderState.LOADING) {
            Log.w(TAG, "No se puede limpiar tareas mientras se está cargando");
            return;
        }
        tasks.clear();
        currentTaskIndex = 0;
        totalTasks = 0;
    }

    /**
     * Inicia la carga de recursos
     * IMPORTANTE: Llamar desde GL Thread
     */
    public void startLoading() {
        if (tasks.isEmpty()) {
            Log.w(TAG, "No hay tareas para cargar");
            state = LoaderState.COMPLETE;
            if (completeListener != null) {
                completeListener.onLoadComplete();
            }
            return;
        }

        state = LoaderState.LOADING;
        currentTaskIndex = 0;
        totalTasks = tasks.size();

        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║   🚀 INICIANDO CARGA DE RECURSOS       ║");
        Log.d(TAG, "║   Total: " + totalTasks + " tareas                    ║");
        Log.d(TAG, "╚════════════════════════════════════════╝");

        // Notificar progreso inicial
        if (progressListener != null) {
            progressListener.onProgress(0f, "Iniciando...");
        }
    }

    /**
     * Carga el siguiente paso
     * Llamar en cada frame desde onDrawFrame()
     *
     * @return true si hay más tareas pendientes, false si terminó
     */
    public boolean loadNextStep() {
        if (state != LoaderState.LOADING) {
            return false;
        }

        if (currentTaskIndex >= tasks.size()) {
            // Carga completada
            state = LoaderState.COMPLETE;
            Log.d(TAG, "╔════════════════════════════════════════╗");
            Log.d(TAG, "║   ✅ CARGA COMPLETADA                  ║");
            Log.d(TAG, "╚════════════════════════════════════════╝");

            if (progressListener != null) {
                progressListener.onProgress(1f, "¡Listo!");
            }
            if (completeListener != null) {
                completeListener.onLoadComplete();
            }
            return false;
        }

        long startTime = System.currentTimeMillis();

        // Cargar tareas mientras haya tiempo en el frame
        while (currentTaskIndex < tasks.size()) {
            LoadTask task = tasks.get(currentTaskIndex);

            try {
                executeTask(task);
                task.completed = true;
                currentTaskIndex++;

                // Calcular progreso
                float progress = (float) currentTaskIndex / totalTasks;
                String statusText = getStatusText(task);

                Log.d(TAG, String.format("📦 [%d/%d] %s (%.0f%%)",
                    currentTaskIndex, totalTasks, task.name, progress * 100));

                if (progressListener != null) {
                    progressListener.onProgress(progress, statusText);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error cargando: " + task.name, e);
                state = LoaderState.ERROR;
                if (errorListener != null) {
                    errorListener.onError(task.name, e);
                }
                return false;
            }

            // Verificar si hemos usado mucho tiempo en este frame
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= MAX_LOAD_TIME_PER_FRAME_MS) {
                // Dejar tiempo para el siguiente frame
                break;
            }
        }

        return currentTaskIndex < tasks.size();
    }

    /**
     * Ejecuta una tarea específica
     */
    private void executeTask(LoadTask task) {
        switch (task.type) {
            case TEXTURE:
                // Cargar textura en GPU
                if (textureManager != null && task.resourceId != 0) {
                    textureManager.getTexture(task.resourceId);
                }
                break;

            case SHADER:
                // Los shaders se compilan en las clases que los usan
                // Esta tarea es más para tracking de progreso
                break;

            case MODEL:
                // Los modelos se cargan via ObjLoader
                // Esta tarea es más para tracking de progreso
                break;

            case SCENE_OBJECT:
            case CUSTOM:
                // Ejecutar tarea personalizada
                if (task.customTask != null) {
                    task.customTask.run();
                }
                break;
        }
    }

    private String getStatusText(LoadTask task) {
        switch (task.type) {
            case TEXTURE:
                return "Cargando " + task.name + "...";
            case SHADER:
                return "Compilando " + task.name + "...";
            case MODEL:
                return "Cargando modelo " + task.name + "...";
            case SCENE_OBJECT:
                return "Preparando " + task.name + "...";
            default:
                return "Procesando...";
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Getters y Estado
    // ═══════════════════════════════════════════════════════════════════════════

    public LoaderState getState() {
        return state;
    }

    public boolean isLoading() {
        return state == LoaderState.LOADING;
    }

    public boolean isComplete() {
        return state == LoaderState.COMPLETE;
    }

    public float getProgress() {
        if (totalTasks == 0) return 0f;
        return (float) currentTaskIndex / totalTasks;
    }

    public int getCurrentTaskIndex() {
        return currentTaskIndex;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    /**
     * Reinicia el loader para una nueva carga
     */
    public void reset() {
        state = LoaderState.IDLE;
        currentTaskIndex = 0;
        for (LoadTask task : tasks) {
            task.completed = false;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Listeners
    // ═══════════════════════════════════════════════════════════════════════════

    public interface OnProgressListener {
        void onProgress(float progress, String statusText);
    }

    public interface OnCompleteListener {
        void onLoadComplete();
    }

    public interface OnErrorListener {
        void onError(String taskName, Exception error);
    }

    public void setOnProgressListener(OnProgressListener listener) {
        this.progressListener = listener;
    }

    public void setOnCompleteListener(OnCompleteListener listener) {
        this.completeListener = listener;
    }

    public void setOnErrorListener(OnErrorListener listener) {
        this.errorListener = listener;
    }
}
