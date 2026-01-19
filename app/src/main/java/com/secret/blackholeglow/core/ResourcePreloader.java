package com.secret.blackholeglow.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.image.ImageDownloadManager;
import com.secret.blackholeglow.model.ModelDownloadManager;
import com.secret.blackholeglow.video.VideoDownloadManager;

import java.util.ArrayList;
import java.util.Arrays;
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

    // ═══════════════════════════════════════════════════════════════════════
    // 🏠 RECURSOS DEL PANEL - NUNCA SE ELIMINAN
    // ═══════════════════════════════════════════════════════════════════════
    private static final List<String> PANEL_VIDEOS = Arrays.asList(
        "thehouse.mp4"  // Video de fondo del panel
    );

    private static final List<String> PANEL_IMAGES = Arrays.asList(
        "grimoire_texture.png",  // Textura del grimorio
        "huevo_zerg.png",        // Elemento del panel
        "fire_orb.png"           // Elemento del panel
    );

    private static final List<String> PANEL_MODELS = Arrays.asList(
        "grimoire.obj"  // Modelo 3D del grimorio
    );

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

        // 🧹 LIMPIEZA: Eliminar recursos de escenas anteriores
        cleanupOtherSceneResources(sceneName);

        // Determinar que escena es y preparar tareas apropiadas
        if (sceneName == null) {
            prepareLabSceneTasks();
            return;
        }

        switch (sceneName) {
            case "Portal Cosmico":
            case "PYRALIS":
            case "LabScene":
                prepareLabSceneTasks();
                break;

            case "ABYSSIA":
            case "Oceano":
            case "OceanFloorScene":
                prepareOceanSceneTasks();
                break;

            case "GOKU":
                prepareGokuSceneTasks();
                break;

            case "ADVENTURE_TIME":
                prepareAdventureTimeSceneTasks();
                break;

            case "NEON_CITY":
                prepareNeonCitySceneTasks();
                break;

            case "SAINT_SEIYA":
                prepareSaintSeiyaSceneTasks();
                break;

            case "WALKING_DEAD":
                prepareWalkingDeadSceneTasks();
                break;

            case "ZELDA_BOTW":
                prepareZeldaSceneTasks();
                break;

            default:
                // Default: usar Lab
                prepareLabSceneTasks();
                break;
        }
    }

    /**
     * Prepara tareas para el Panel de Control (lobby)
     * Incluye grimoire, video de fondo y texturas del LikeButton
     * DEBE ejecutarse ANTES de mostrar el panel
     */
    public void preparePanelTasks() {
        tasks.clear();

        // 1. VIDEO - Fondo del panel (thehouse.mp4)
        addVideoDownloadTask("Video Panel", "thehouse.mp4", 8);

        // 2. Modelo 3D del Grimorio (libro mágico)
        addModelDownloadTask("Modelo Grimorio", "grimoire.obj", 3);

        // 3. Textura del Grimorio
        addImageDownloadTask("Textura Grimorio", "grimoire_texture.png", 5);

        // 4. Texturas del LikeButton (usadas en todas las escenas)
        addImageDownloadTask("Huevo Zerg", "huevo_zerg.png", 1);
        addImageDownloadTask("Orbe de Fuego", "fire_orb.png", 1);

        // Calcular total
        calculateTotalWeight();
        Log.d(TAG, "PanelTasks: " + tasks.size() + " tareas (peso: " + totalTasks + ")");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🧹 LIMPIEZA DE RECURSOS - Solo mantiene panel + escena actual
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Elimina recursos de escenas anteriores, manteniendo solo:
     * - Recursos del Panel (thehouse.mp4, grimoire.obj, etc.)
     * - Recursos de la escena actual
     *
     * Esto evita que el almacenamiento se llene con videos de todas las escenas.
     */
    private void cleanupOtherSceneResources(String currentScene) {
        Log.d(TAG, "🧹 Limpiando recursos de escenas anteriores...");

        // Obtener recursos de la escena actual
        List<String> sceneVideos = getSceneVideos(currentScene);
        List<String> sceneImages = getSceneImages(currentScene);
        List<String> sceneModels = getSceneModels(currentScene);

        // Combinar con recursos del panel (siempre se mantienen)
        List<String> keepVideos = new ArrayList<>(PANEL_VIDEOS);
        keepVideos.addAll(sceneVideos);

        List<String> keepImages = new ArrayList<>(PANEL_IMAGES);
        keepImages.addAll(sceneImages);

        List<String> keepModels = new ArrayList<>(PANEL_MODELS);
        keepModels.addAll(sceneModels);

        // Ejecutar limpieza
        long freedVideos = VideoDownloadManager.getInstance(context).deleteAllExcept(keepVideos);
        long freedImages = ImageDownloadManager.getInstance(context).deleteAllExcept(keepImages);
        long freedModels = ModelDownloadManager.getInstance(context).deleteAllExcept(keepModels);

        long totalFreed = freedVideos + freedImages + freedModels;
        if (totalFreed > 0) {
            Log.d(TAG, "🧹 Total liberado: " + (totalFreed / 1024 / 1024) + " MB");
        }
    }

    /**
     * Obtiene los videos necesarios para una escena específica
     */
    private List<String> getSceneVideos(String sceneName) {
        if (sceneName == null) return new ArrayList<>();

        switch (sceneName) {
            case "Portal Cosmico":
            case "PYRALIS":
            case "LabScene":
                return Arrays.asList("cielovolando.mp4");

            case "ABYSSIA":
            case "Oceano":
            case "OceanFloorScene":
                return Arrays.asList("marZerg.mp4");

            case "GOKU":
                return Arrays.asList("gokufinalkamehamehaHD.mp4");

            case "ADVENTURE_TIME":
                return Arrays.asList("escenaHDA.mp4");

            case "NEON_CITY":
                return Arrays.asList("neonCityDeLorean.mp4");

            case "SAINT_SEIYA":
                return Arrays.asList();  // Solo usa imágenes

            case "WALKING_DEAD":
                return Arrays.asList("walkingdeathscene.mp4");

            case "ZELDA_BOTW":
                return Arrays.asList();  // Solo usa imágenes, no videos

            default:
                return new ArrayList<>();
        }
    }

    /**
     * Obtiene las imágenes necesarias para una escena específica
     */
    private List<String> getSceneImages(String sceneName) {
        if (sceneName == null) return new ArrayList<>();

        switch (sceneName) {
            case "Portal Cosmico":
            case "PYRALIS":
            case "LabScene":
                return Arrays.asList("human_interceptor_texture.png", "thruster_flames.png");

            case "ABYSSIA":
            case "Oceano":
            case "OceanFloorScene":
                return Arrays.asList("abyssal_leviathan_texture.png", "abyssal_lurker_texture.png");

            case "NEON_CITY":
                return Arrays.asList("delorean_texture.png");

            case "SAINT_SEIYA":
                return Arrays.asList(
                    "seiya_fondo_cosmos.png",
                    "seiya_fondo_cosmos_depth.png",
                    "seiya_character_cosmos.png",
                    "seiya_character_cosmos_depth.png"
                );

            case "WALKING_DEAD":
                return Arrays.asList("zombie_head_texture.png", "zombie_body_texture.webp");

            case "ZELDA_BOTW":
                return Arrays.asList(
                    "zelda_fondo.png",
                    "zelda_paisaje.png",
                    "zelda_piedra.png",
                    "zelda_link.png",
                    "zelda_fondo_depth.png",    // Solo fondo tiene parallax
                    "link_3d_texture.webp"      // Textura del modelo 3D de Link (WebP)
                );

            // GOKU, ADVENTURE_TIME solo usan videos
            default:
                return new ArrayList<>();
        }
    }

    /**
     * Obtiene los modelos 3D necesarios para una escena específica
     */
    private List<String> getSceneModels(String sceneName) {
        if (sceneName == null) return new ArrayList<>();

        switch (sceneName) {
            case "Portal Cosmico":
            case "PYRALIS":
            case "LabScene":
                return Arrays.asList("human_interceptor_flames.obj");

            case "ABYSSIA":
            case "Oceano":
            case "OceanFloorScene":
                return Arrays.asList("abyssal_leviathan.obj", "abyssal_lurker.obj");

            case "NEON_CITY":
                return Arrays.asList("delorean.obj");

            case "WALKING_DEAD":
                return Arrays.asList("zombie_head.obj", "zombie_body.obj");

            // GOKU, ADVENTURE_TIME, SAINT_SEIYA no usan modelos 3D
            default:
                return new ArrayList<>();
        }
    }

    /**
     * Prepara tareas para LabScene (Portal Cosmico)
     * Incluye descarga del video de nubes de fuego, modelo 3D y textura de la nave
     */
    public void prepareLabSceneTasks() {
        tasks.clear();

        // 1. VIDEO - Lo mas importante y pesado
        addVideoDownloadTask("Video Portal Cosmico", "cielovolando.mp4", 10);

        // 2. Modelo 3D de la nave (TravelingShip)
        addModelDownloadTask("Modelo Nave", "human_interceptor_flames.obj", 3);

        // 3. Textura de la nave
        addImageDownloadTask("Textura Nave", "human_interceptor_texture.png", 4);

        // 4. Texturas extras (thruster flames, fire orb)
        addImageDownloadTask("Llamas Thruster", "thruster_flames.png", 1);
        addImageDownloadTask("Orbe de Fuego", "fire_orb.png", 1);

        // Calcular total
        calculateTotalWeight();
        Log.d(TAG, "LabScene: " + tasks.size() + " tareas (peso: " + totalTasks + ")");
    }

    /**
     * Prepara tareas para OceanFloorScene (Fondo del Mar)
     * Incluye descarga del video, modelos 3D y texturas de las criaturas
     */
    public void prepareOceanSceneTasks() {
        tasks.clear();

        // 1. VIDEO - Lo mas importante
        addVideoDownloadTask("Video Abyssia", "marZerg.mp4", 10);

        // 2. Modelos 3D de las criaturas
        addModelDownloadTask("Modelo Leviatán", "abyssal_leviathan.obj", 4);
        addModelDownloadTask("Modelo Acechador", "abyssal_lurker.obj", 3);

        // 3. Texturas de las criaturas
        addImageDownloadTask("Textura Leviatán", "abyssal_leviathan_texture.png", 3);
        addImageDownloadTask("Textura Acechador", "abyssal_lurker_texture.png", 4);

        // 4. Extras
        addImageDownloadTask("Huevo Zerg", "huevo_zerg.png", 1);

        // Calcular total
        calculateTotalWeight();
        Log.d(TAG, "OceanScene: " + tasks.size() + " tareas (peso: " + totalTasks + ")");
    }

    /**
     * Prepara tareas para GokuScene (Kamehameha)
     * Incluye descarga del video de Goku
     */
    public void prepareGokuSceneTasks() {
        tasks.clear();

        // 1. VIDEO - Goku Kamehameha Final HD desde Supabase
        addVideoDownloadTask("Video Goku Kamehameha HD", "gokufinalkamehamehaHD.mp4", 10);

        // 2. Preview texture
        addTextureTask("Preparando escena Goku", R.drawable.preview_goku, 2);

        // Calcular total
        calculateTotalWeight();
        Log.d(TAG, "GokuScene: " + tasks.size() + " tareas (peso: " + totalTasks + ")");
    }

    /**
     * Prepara tareas para AdventureTimeScene (Hora de Aventura)
     * Solo descarga del video (preview es local: hdapreview.webp)
     */
    public void prepareAdventureTimeSceneTasks() {
        tasks.clear();

        // 1. VIDEO - Adventure Time Fogata desde Supabase
        addVideoDownloadTask("Video Adventure Time", "escenaHDA.mp4", 10);

        // Preview ahora es local (hdapreview.webp) - no requiere descarga

        // Calcular total
        calculateTotalWeight();
        Log.d(TAG, "AdventureTimeScene: " + tasks.size() + " tareas (peso: " + totalTasks + ")");
    }

    /**
     * Prepara tareas para NeonCityScene (Synthwave DeLorean)
     * Descarga del video, modelo 3D y textura del DeLorean
     */
    public void prepareNeonCitySceneTasks() {
        tasks.clear();

        // 1. VIDEO - Neon City Synthwave desde Supabase
        addVideoDownloadTask("Video Neon City", "neoncityScene.mp4", 10);

        // 2. Modelo 3D del DeLorean
        addModelDownloadTask("Modelo DeLorean", "delorean.obj", 3);

        // 3. Textura del DeLorean
        addImageDownloadTask("Textura DeLorean", "delorean_texture.png", 4);

        // Calcular total
        calculateTotalWeight();
        Log.d(TAG, "NeonCityScene: " + tasks.size() + " tareas (peso: " + totalTasks + ")");
    }

    public void prepareSaintSeiyaSceneTasks() {
        tasks.clear();

        // ═══════════════════════════════════════════════════════════════
        // SISTEMA 2 CAPAS: Fondo + Seiya separados
        // ═══════════════════════════════════════════════════════════════

        // CAPA 1: Fondo cosmos
        addImageDownloadTask("Fondo Universo", "fondouniverso.png", 8);
        addImageDownloadTask("Fondo Depth", "fondouniverso3d.png", 2);

        // CAPA 2: Seiya (personaje sin fondo)
        addImageDownloadTask("Seiya Solo", "seiya_solo.png", 5);
        addImageDownloadTask("Seiya Depth", "seiya_depth.png", 3);  // Depth existente

        // Calcular total
        calculateTotalWeight();
        Log.d(TAG, "SaintSeiyaScene: " + tasks.size() + " recursos 2-capas (peso: " + totalTasks + ")");
    }

    public void prepareWalkingDeadSceneTasks() {
        tasks.clear();

        // 1. Video del cementerio zombie
        addVideoDownloadTask("Walking Dead Video", "walkingdeathscene.mp4", 15);

        // 2. Modelo 3D - Cabeza zombi colgante (Meshy AI)
        addModelDownloadTask("Modelo Cabeza Zombi", "zombie_head.obj", 3);

        // 3. Textura de la cabeza zombi
        addImageDownloadTask("Textura Zombi", "zombie_head_texture.png", 5);

        // 4. Modelo 3D - Zombie cuerpo completo (Meshy AI)
        addModelDownloadTask("Modelo Zombie Cuerpo", "zombie_body.obj", 3);

        // 5. Textura zombie cuerpo (WebP optimizado 356KB)
        addImageDownloadTask("Textura Zombie Cuerpo", "zombie_body_texture.webp", 2);

        // Calcular total
        calculateTotalWeight();
        Log.d(TAG, "WalkingDeadScene: " + tasks.size() + " recursos (peso: " + totalTasks + ")");
    }

    /**
     * 🗡️ Prepara tareas para ZeldaParallaxScene (BOTW Parallax 3D)
     * 4 capas con depth maps para efecto 3D de profundidad
     */
    public void prepareZeldaSceneTasks() {
        tasks.clear();

        // ═══════════════════════════════════════════════════════════════
        // CAPA 1: CIELO (Fondo) - CON PARALLAX
        // ═══════════════════════════════════════════════════════════════
        addImageDownloadTask("Zelda Cielo", "zelda_fondo.png", 4);
        addImageDownloadTask("Zelda Cielo Depth", "zelda_fondo_depth.png", 1);

        // ═══════════════════════════════════════════════════════════════
        // CAPAS ESTÁTICAS (sin depth maps)
        // ═══════════════════════════════════════════════════════════════
        addImageDownloadTask("Zelda Paisaje", "zelda_paisaje.png", 5);
        addImageDownloadTask("Zelda Piedra", "zelda_piedra.png", 3);
        addImageDownloadTask("Zelda Link 2D", "zelda_link.png", 3);

        // ═══════════════════════════════════════════════════════════════
        // 🗡️ LINK 3D MODEL - Textura del modelo (Meshy AI) - WebP comprimido
        // ═══════════════════════════════════════════════════════════════
        addImageDownloadTask("Link 3D Textura", "link_3d_texture.webp", 2);

        // Calcular total
        calculateTotalWeight();
        Log.d(TAG, "🗡️ ZeldaScene: " + tasks.size() + " recursos (peso: " + totalTasks + ")");
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
        // ⚡ CRÍTICO: Pequeña pausa para que el sistema de archivos termine de sincronizar
        // Esto evita race conditions donde los archivos se reportan como "descargados"
        // pero aún no están completamente disponibles para lectura
        try {
            Thread.sleep(300);  // 300ms de "settling time"
            Log.d(TAG, "⚡ Settling time completado - archivos listos");
        } catch (InterruptedException ignored) {}

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

    /**
     * Agrega tarea de descarga de imagen desde Supabase.
     * Usa ImageDownloadManager (especialista en imágenes).
     * Si la imagen ya existe, la tarea completa inmediatamente.
     */
    private void addImageDownloadTask(String name, String imageFileName, int weight) {
        tasks.add(new PreloadTask(name, () -> {
            ImageDownloadManager downloader = ImageDownloadManager.getInstance(context);

            if (downloader.isImageAvailable(imageFileName)) {
                Log.d(TAG, "🖼️ Imagen ya descargada: " + imageFileName);
                return;
            }

            // Imagen no disponible - descargar
            Log.d(TAG, "🖼️ Descargando imagen: " + imageFileName);
            boolean success = downloader.downloadImageSync(imageFileName, percent -> {
                // Actualizar progreso de descarga
                String progressText = name + " (" + percent + "%)";
                mainHandler.post(() -> {
                    if (listener != null) {
                        listener.onProgressUpdate(completedTasks, totalTasks, progressText);
                    }
                });
            });

            if (!success) {
                Log.e(TAG, "❌ Error descargando imagen: " + imageFileName);
            }
        }, weight));
    }

    /**
     * Agrega tarea de descarga de modelo 3D desde Supabase.
     * Usa ModelDownloadManager (especialista en modelos OBJ).
     * Si el modelo ya existe, la tarea completa inmediatamente.
     */
    private void addModelDownloadTask(String name, String modelFileName, int weight) {
        tasks.add(new PreloadTask(name, () -> {
            ModelDownloadManager downloader = ModelDownloadManager.getInstance(context);

            if (downloader.isModelAvailable(modelFileName)) {
                Log.d(TAG, "🎮 Modelo ya descargado: " + modelFileName);
                return;
            }

            // Modelo no disponible - descargar
            Log.d(TAG, "🎮 Descargando modelo: " + modelFileName);
            boolean success = downloader.downloadModelSync(modelFileName, percent -> {
                // Actualizar progreso de descarga
                String progressText = name + " (" + percent + "%)";
                mainHandler.post(() -> {
                    if (listener != null) {
                        listener.onProgressUpdate(completedTasks, totalTasks, progressText);
                    }
                });
            });

            if (!success) {
                Log.e(TAG, "❌ Error descargando modelo: " + modelFileName);
            }
        }, weight));
    }
}
