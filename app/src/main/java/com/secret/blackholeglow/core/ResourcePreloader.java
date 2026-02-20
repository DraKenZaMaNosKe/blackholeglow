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

    // 🏠 RECURSOS DEL PANEL - Ver PanelResources.java (v5.0.8)

    private final Context context;
    private PreloadListener listener;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private Handler mainHandler;

    // 🔒 THREAD-SAFE: Sincronizado con tasksLock
    private final Object tasksLock = new Object();
    private final List<PreloadTask> tasks = new ArrayList<>();
    private volatile int totalTasks = 0;
    private volatile int completedTasks = 0;
    private volatile boolean isCancelled = false;
    private volatile int failedTaskCount = 0;
    private volatile String firstFailureMessage = null;

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
        this.mainHandler = new Handler(context.getMainLooper());
    }

    public void setListener(PreloadListener listener) {
        this.listener = listener;
    }

    // 🔧 FIX ANR: Guardar sceneName para limpieza diferida en background
    private String pendingCleanupScene = null;
    private String activeSceneToProtect = null;  // 🔧 Escena activa que NO debe limpiarse

    /**
     * 🔧 Establece la escena activa que debe protegerse durante limpieza.
     * Llamar ANTES de cambiar la preferencia del wallpaper.
     */
    public void setActiveSceneToProtect(String sceneName) {
        this.activeSceneToProtect = sceneName;
    }

    /**
     * Prepara tareas segun el nombre del wallpaper
     */
    public void prepareTasksForScene(String sceneName) {
        synchronized (tasksLock) {
            tasks.clear();
        }

        // 🛡️ Limpieza movida a DESPUÉS de instalación exitosa (WallpaperPreviewActivity)
        // Ya NO se limpian recursos antes de descargar - si falla, el wallpaper anterior sigue

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

            case "SUPERMAN":
                prepareSupermanSceneTasks();
                break;

            case "AOT":
                prepareAOTSceneTasks();
                break;

            case "SPIDER":
                prepareSpiderSceneTasks();
                break;

            case "LOST_ATLANTIS":
                prepareLostAtlantisSceneTasks();
                break;

            case "THE_HUMAN_PREDATOR":
                prepareTheHumanPredatorSceneTasks();
                break;

            case "MOONLIT_CAT":
                prepareMoonlitCatSceneTasks();
                break;

            case "FRIEZA_DEATHBEAM":
                prepareFriezaDeathBeamSceneTasks();
                break;

            default:
                // Default: usar Lab
                prepareLabSceneTasks();
                break;
        }
    }

    /**
     * Prepara tareas para el Panel de Control (lobby)
     * v5.0.8: Gaming Controller (control Xbox) + texturas del LikeButton
     * DEBE ejecutarse ANTES de mostrar el panel
     */
    public void preparePanelTasks() {
        tasks.clear();

        // 1. Modelo 3D del Gaming Controller (control Xbox)
        addModelDownloadTask("Modelo Controller", "controlxbox_texture.obj", 3);

        // 2. Textura del Gaming Controller
        addImageDownloadTask("Textura Controller", "controlxbox_texture.png", 5);

        // 3. Texturas del LikeButton (usadas en todas las escenas)
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
     * 🧹 Limpia recursos de escenas anteriores DESPUÉS de una instalación exitosa.
     * Se ejecuta en un hilo de fondo para no bloquear la UI.
     * Solo mantiene: recursos del panel + recursos de la escena instalada.
     *
     * @param installedScene El sceneName del wallpaper recién instalado
     */
    public void cleanupAfterInstallation(String installedScene) {
        new Thread(() -> {
            try {
                Log.d(TAG, "🧹 Limpieza post-instalación para: " + installedScene);
                cleanupOtherSceneResources(installedScene);
            } catch (Exception e) {
                Log.w(TAG, "Error en limpieza post-instalación: " + e.getMessage());
            }
        }, "ResourceCleanup").start();
    }

    /**
     * Elimina recursos de escenas anteriores, manteniendo solo:
     * - Recursos del Panel (Gaming Controller, texturas LikeButton)
     * - Recursos de la escena actual
     *
     * Esto evita que el almacenamiento se llene con videos de todas las escenas.
     */
    private void cleanupOtherSceneResources(String newScene) {
        Log.d(TAG, "🧹 Limpiando recursos de escenas anteriores...");

        // 🔧 FIX CRASH: Arrays.asList() retorna lista de tamaño fijo que NO soporta addAll().
        // Envolver en new ArrayList<>() para hacerla mutable.
        List<String> sceneVideos = new ArrayList<>(getSceneVideos(newScene));
        List<String> sceneImages = new ArrayList<>(getSceneImages(newScene));
        List<String> sceneModels = new ArrayList<>(getSceneModels(newScene));

        // 🔧 FIX FREEZE: También mantener recursos del wallpaper ACTUALMENTE CORRIENDO
        // El wallpaper activo sigue necesitando sus recursos hasta que sea reemplazado
        if (activeSceneToProtect != null && !activeSceneToProtect.equals(newScene)) {
            Log.d(TAG, "🛡️ Manteniendo recursos del wallpaper activo: " + activeSceneToProtect);
            sceneVideos.addAll(getSceneVideos(activeSceneToProtect));
            sceneImages.addAll(getSceneImages(activeSceneToProtect));
            sceneModels.addAll(getSceneModels(activeSceneToProtect));
        }

        // Combinar con recursos del panel (siempre se mantienen)
        List<String> keepVideos = new ArrayList<>(sceneVideos);

        List<String> keepImages = new ArrayList<>(PanelResources.IMAGES);
        keepImages.addAll(sceneImages);

        List<String> keepModels = new ArrayList<>(PanelResources.MODELS);
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
                return Arrays.asList("neoncityScene.mp4");

            case "SAINT_SEIYA":
                return Arrays.asList();  // Solo usa imágenes

            case "WALKING_DEAD":
                return Arrays.asList("walkingdeathscene.mp4");

            case "ZELDA_BOTW":
                return Arrays.asList();  // Solo usa imágenes, no videos

            case "SUPERMAN":
                return Arrays.asList("superman_scene.mp4");

            case "AOT":
                return Arrays.asList("erenEscena01.mp4");

            case "SPIDER":
                return Arrays.asList("spiderscene.mp4");

            case "LOST_ATLANTIS":
                return Arrays.asList("lostatlanstis.mp4");

            case "THE_HUMAN_PREDATOR":
                return Arrays.asList("guerrerovsleon.mp4");

            case "MOONLIT_CAT":
                return Arrays.asList();  // No video - shader-based sky

            case "FRIEZA_DEATHBEAM":
                return Arrays.asList("frieza_deathbeam_bg.mp4");

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
                    "zelda_fondo_depth.png",
                    "link_3d_texture.webp"
                );

            case "MOONLIT_CAT":
                return Arrays.asList("cat_open.png", "cat_half.png", "cat_closed.png",
                        "brick_wall_texture.png", "buildings_silhouette.png", "moon_texture.png");

            case "FRIEZA_DEATHBEAM":
                return Arrays.asList("frieza_texture.png");

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

            case "ZELDA_BOTW":
                return Arrays.asList("link_3d.obj");

            case "MOONLIT_CAT":
                return Arrays.asList("black_cat_clean.obj", "brick_wall.obj");

            case "FRIEZA_DEATHBEAM":
                return Arrays.asList("frieza.obj", "frieza_halo.obj");

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
        // 🗡️ LINK 3D MODEL - Modelo + Textura (Meshy AI)
        // ═══════════════════════════════════════════════════════════════
        addModelDownloadTask("Link 3D Modelo", "link_3d.obj", 2);
        addImageDownloadTask("Link 3D Textura", "link_3d_texture.webp", 2);

        // Calcular total
        calculateTotalWeight();
        Log.d(TAG, "🗡️ ZeldaScene: " + tasks.size() + " recursos (peso: " + totalTasks + ")");
    }

    /**
     * Prepara tareas para SupermanScene (Man of Steel)
     * Video de Superman volando épicamente
     */
    public void prepareSupermanSceneTasks() {
        tasks.clear();

        // 1. VIDEO - Superman Scene desde Supabase
        addVideoDownloadTask("Video Superman", "superman_scene.mp4", 10);

        // 2. Preview texture
        addTextureTask("Preparando escena Superman", R.drawable.preview_superman, 2);

        // Calcular total
        calculateTotalWeight();
        Log.d(TAG, "🦸 SupermanScene: " + tasks.size() + " tareas (peso: " + totalTasks + ")");
    }

    /**
     * ⚔️ AOT Scene - Attack on Titan (Eren Jaeger)
     */
    public void prepareAOTSceneTasks() {
        tasks.clear();

        // 1. VIDEO - AOT Scene desde Supabase
        addVideoDownloadTask("Video AOT", "erenEscena01.mp4", 10);

        // 2. Preview texture
        addTextureTask("Preparando escena AOT", R.drawable.preview_aot, 2);

        // Calcular total
        calculateTotalWeight();
        Log.d(TAG, "⚔️ AOTScene: " + tasks.size() + " tareas (peso: " + totalTasks + ")");
    }

    /**
     * 🕷️ Spider Scene - Black Spider Horror
     */
    public void prepareSpiderSceneTasks() {
        tasks.clear();

        // 1. VIDEO - Spider Scene desde Supabase
        addVideoDownloadTask("Video Spider", "spiderscene.mp4", 10);

        // 2. Preview texture
        addTextureTask("Preparando escena Spider", R.drawable.preview_spider, 2);

        // Calcular total
        calculateTotalWeight();
        Log.d(TAG, "🕷️ SpiderScene: " + tasks.size() + " tareas (peso: " + totalTasks + ")");
    }

    /**
     * 🏛️ Lost Atlantis Scene - Templo sumergido
     */
    public void prepareLostAtlantisSceneTasks() {
        tasks.clear();

        // 1. VIDEO - Lost Atlantis Scene desde Supabase
        addVideoDownloadTask("Video Lost Atlantis", "lostatlanstis.mp4", 10);

        // 2. Preview texture
        addTextureTask("Preparando escena Lost Atlantis", R.drawable.preview_lost_atlantis, 2);

        // Calcular total
        calculateTotalWeight();
        Log.d(TAG, "🏛️ LostAtlantisScene: " + tasks.size() + " tareas (peso: " + totalTasks + ")");
    }

    /**
     * 🦁 The Human Predator Scene - Guerrero vs León
     */
    public void prepareTheHumanPredatorSceneTasks() {
        tasks.clear();

        // 1. VIDEO - Guerrero vs León desde Supabase
        addVideoDownloadTask("Video Human Predator", "guerrerovsleon.mp4", 10);

        // 2. Preview texture
        addTextureTask("Preparando escena Human Predator", R.drawable.preview_human_predator, 2);

        // Calcular total
        calculateTotalWeight();
        Log.d(TAG, "🦁 TheHumanPredatorScene: " + tasks.size() + " tareas (peso: " + totalTasks + ")");
    }

    /**
     * 🌙 Moonlit Cat Scene - Gato bajo la luna (shader-only, no video)
     * Models and textures from Meshy AI via Supabase
     */
    public void prepareMoonlitCatSceneTasks() {
        tasks.clear();

        // No video - scene uses shader-based night sky

        // 1. Modelo 3D del gato negro (UV limpio re-bakeado)
        addModelDownloadTask("Modelo Gato", "black_cat_clean.obj", 3);

        // 2. Texturas del gato (atlas blink: open/half/closed)
        addImageDownloadTask("Textura Gato Open", "cat_open.png", 3);
        addImageDownloadTask("Textura Gato Half", "cat_half.png", 2);
        addImageDownloadTask("Textura Gato Closed", "cat_closed.png", 2);

        // 3. Modelo y textura de la barda
        addModelDownloadTask("Modelo Barda", "brick_wall.obj", 3);
        addImageDownloadTask("Textura Barda", "brick_wall_texture.png", 5);

        // 4. Escenario: edificios y luna
        addImageDownloadTask("Silueta Edificios", "buildings_silhouette.png", 1);
        addImageDownloadTask("Textura Luna", "moon_texture.png", 1);

        // Calcular total
        calculateTotalWeight();
        Log.d(TAG, "🌙 MoonlitCatScene: " + tasks.size() + " tareas (peso: " + totalTasks + ")");
    }

    public void prepareFriezaDeathBeamSceneTasks() {
        tasks.clear();

        // 1. Video de fondo - anime speed lines (540x960, 3s loop)
        addVideoDownloadTask("Video Fondo Anime", "frieza_deathbeam_bg.mp4", 3);

        // 2. Modelo 3D de Frieza (Meshy AI, sin aureola)
        addModelDownloadTask("Modelo Frieza", "frieza.obj", 3);

        // 3. Aureola angelical (separada del cuerpo)
        addModelDownloadTask("Aureola Frieza", "frieza_halo.obj", 1);

        // 4. Textura baked de Frieza (2048x2048)
        addImageDownloadTask("Textura Frieza", "frieza_texture.png", 5);

        calculateTotalWeight();
        Log.d(TAG, "💜 FriezaDeathBeam: " + tasks.size() + " tareas (peso: " + totalTasks + ")");
    }

    private void calculateTotalWeight() {
        totalTasks = 0;
        for (PreloadTask task : tasks) {
            totalTasks += task.weight;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🛡️ VERIFICACIÓN DE RECURSOS - Safety net para el engine
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Verifica si los recursos CRÍTICOS de una escena están disponibles en disco.
     * Solo verifica videos (son los recursos más grandes y obligatorios).
     * Imágenes y modelos pueden faltar sin causar pantalla negra.
     *
     * @return true si todos los videos requeridos existen en disco
     */
    public static boolean areSceneResourcesReady(Context context, String sceneName) {
        if (sceneName == null) return true;

        // Obtener videos requeridos para esta escena
        List<String> requiredVideos = getSceneVideosStatic(sceneName);

        if (requiredVideos.isEmpty()) {
            // Escenas sin video (Saint Seiya, Zelda) siempre están listas
            return true;
        }

        VideoDownloadManager downloader = VideoDownloadManager.getInstance(context);
        for (String videoFile : requiredVideos) {
            if (!downloader.isVideoAvailable(videoFile)) {
                Log.d(TAG, "🛡️ Recurso NO disponible: " + videoFile + " para " + sceneName);
                return false;
            }
        }

        Log.d(TAG, "🛡️ Recursos verificados OK para: " + sceneName);
        return true;
    }

    /**
     * Versión estática de getSceneVideos para uso desde otros componentes
     */
    private static List<String> getSceneVideosStatic(String sceneName) {
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
                return Arrays.asList("neoncityScene.mp4");
            case "WALKING_DEAD":
                return Arrays.asList("walkingdeathscene.mp4");
            case "SUPERMAN":
                return Arrays.asList("superman_scene.mp4");
            case "AOT":
                return Arrays.asList("erenEscena01.mp4");
            case "SPIDER":
                return Arrays.asList("spiderscene.mp4");
            case "LOST_ATLANTIS":
                return Arrays.asList("lostatlanstis.mp4");
            case "THE_HUMAN_PREDATOR":
                return Arrays.asList("guerrerovsleon.mp4");
            case "MOONLIT_CAT":
                return Arrays.asList();  // No video - shader-based sky
            case "FRIEZA_DEATHBEAM":
                return Arrays.asList("frieza_deathbeam_bg.mp4");
            default:
                return new ArrayList<>();
        }
    }

    /**
     * Inicia la precarga en background
     */
    public void startPreloading() {
        synchronized (tasksLock) {
            if (tasks.isEmpty()) {
                Log.e(TAG, "No hay tareas preparadas!");
                if (listener != null) {
                    listener.onPreloadError("No hay tareas de precarga");
                }
                return;
            }
        }

        isCancelled = false;
        completedTasks = 0;
        currentTaskIndex = 0;
        failedTaskCount = 0;
        firstFailureMessage = null;

        backgroundThread = new HandlerThread("ResourcePreloader");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());

        // 🔧 FIX ANR: Ejecutar limpieza en background thread PRIMERO
        backgroundHandler.post(() -> {
            // 🛡️ Limpieza movida a DESPUÉS de instalación exitosa
            // Ya NO se borran recursos antes de descargar - así el wallpaper anterior
            // sigue funcionando si la descarga falla (sin datos, batería baja, etc.)

            // Ejecutar tareas de descarga directamente
            executeNextTask();
        });
    }

    private volatile int currentTaskIndex = 0;

    private void executeNextTask() {
        if (isCancelled) {
            cleanup();
            return;
        }

        PreloadTask task;
        synchronized (tasksLock) {
            if (currentTaskIndex >= tasks.size()) {
                // Todas las tareas completadas
                notifyComplete();
                cleanup();
                return;
            }
            task = tasks.get(currentTaskIndex);
        }

        // Notificar progreso
        notifyProgress(task.name);

        // Ejecutar tarea
        try {
            task.task.run();
            completedTasks += task.weight;
            Log.d(TAG, "✓ " + task.name + " (" + completedTasks + "/" + totalTasks + ")");
        } catch (Exception e) {
            Log.e(TAG, "✗ Error en " + task.name + ": " + e.getMessage());
            failedTaskCount++;
            if (firstFailureMessage == null) {
                firstFailureMessage = task.name;
            }
            completedTasks += task.weight;  // Avanzar progreso aunque falle
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
            if (failedTaskCount > 0) {
                // 🛡️ FIX BLACK SCREEN: Reportar error en vez de éxito si hubo descargas fallidas
                String errorMsg = failedTaskCount + " recurso(s) no se descargaron: " + firstFailureMessage;
                Log.e(TAG, "🛡️ Precarga terminó con errores: " + errorMsg);
                mainHandler.post(() -> listener.onPreloadError(errorMsg));
            } else {
                mainHandler.post(() -> listener.onPreloadComplete());
            }
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
                throw new RuntimeException("Error descargando video: " + videoFileName);
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
                throw new RuntimeException("Error descargando imagen: " + imageFileName);
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
                throw new RuntimeException("Error descargando modelo: " + modelFileName);
            }
        }, weight));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 📋 CONSULTAS ESTÁTICAS DE RECURSOS POR ESCENA
    // Usadas por WallpaperNotificationManager para reportes
    // ═══════════════════════════════════════════════════════════════════════

    public static List<String> getRequiredVideos(String sceneName) {
        return getSceneVideosStatic(sceneName);
    }

    public static List<String> getRequiredImages(String sceneName) {
        if (sceneName == null) return new ArrayList<>();
        switch (sceneName) {
            case "ABYSSIA":
                return Arrays.asList("abyssal_leviathan_texture.png", "abyssal_lurker_texture.png", "huevo_zerg.png");
            case "NEON_CITY":
                return Arrays.asList("delorean_texture.png");
            case "SAINT_SEIYA":
                return Arrays.asList("fondouniverso.png", "fondouniverso3d.png", "seiya_solo.png", "seiya_depth.png");
            case "WALKING_DEAD":
                return Arrays.asList("zombie_head_texture.png", "zombie_body_texture.webp");
            case "ZELDA_BOTW":
                return Arrays.asList("zelda_fondo.png", "zelda_fondo_depth.png", "zelda_paisaje.png",
                        "zelda_piedra.png", "zelda_link.png", "link_3d_texture.webp");
            case "MOONLIT_CAT":
                return Arrays.asList("cat_open.png", "cat_half.png", "cat_closed.png",
                        "brick_wall_texture.png", "buildings_silhouette.png", "moon_texture.png");
            default:
                return new ArrayList<>();
        }
    }

    public static List<String> getRequiredModels(String sceneName) {
        if (sceneName == null) return new ArrayList<>();
        switch (sceneName) {
            case "ABYSSIA":
                return Arrays.asList("abyssal_leviathan.obj", "abyssal_lurker.obj");
            case "NEON_CITY":
                return Arrays.asList("delorean.obj");
            case "WALKING_DEAD":
                return Arrays.asList("zombie_head.obj", "zombie_body.obj");
            case "ZELDA_BOTW":
                return Arrays.asList("link_3d.obj");
            case "MOONLIT_CAT":
                return Arrays.asList("black_cat_clean.obj", "brick_wall.obj");
            default:
                return new ArrayList<>();
        }
    }
}
