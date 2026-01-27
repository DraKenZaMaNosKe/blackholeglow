package com.secret.blackholeglow.core;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.MotionEvent;

import com.secret.blackholeglow.CameraController;
import com.secret.blackholeglow.MusicVisualizer;
import com.secret.blackholeglow.TextureManager;
import com.secret.blackholeglow.scenes.OceanFloorScene;
import com.secret.blackholeglow.scenes.LabScene;
import com.secret.blackholeglow.scenes.GokuScene;
import com.secret.blackholeglow.scenes.AdventureTimeScene;
import com.secret.blackholeglow.scenes.NeonCityScene;
import com.secret.blackholeglow.scenes.SaintSeiyaScene;
import com.secret.blackholeglow.scenes.WalkingDeadScene;
import com.secret.blackholeglow.scenes.ZeldaParallaxScene;
import com.secret.blackholeglow.scenes.SupermanScene;
import com.secret.blackholeglow.scenes.AOTScene;
import com.secret.blackholeglow.sharing.LikeButton;
import com.secret.blackholeglow.scenes.WallpaperScene;
import com.secret.blackholeglow.systems.AspectRatioManager;
import com.secret.blackholeglow.systems.EventBus;
import com.secret.blackholeglow.systems.FirebaseQueueManager;
import com.secret.blackholeglow.systems.GLStateManager;
import com.secret.blackholeglow.systems.ResourceManager;
import com.secret.blackholeglow.systems.ScreenEffectsManager;
import com.secret.blackholeglow.systems.ScreenManager;
import com.secret.blackholeglow.systems.WallpaperCatalog;
import com.secret.blackholeglow.models.WallpaperItem;
import com.secret.blackholeglow.systems.UIController;
import com.secret.blackholeglow.gl3.MatrixPool;
import com.secret.blackholeglow.effects.BloomEffect;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * WallpaperDirector - Director Central con Arquitectura de Actores v2.0
 *
 * ACTORES:
 * - RenderModeController: Transiciones de modo
 * - PanelModeRenderer: UI del panel de control
 * - SceneFactory: Creacion/destruccion de escenas
 * - SongSharingController: Like, corazones, Gemini AI
 * - TouchRouter: Distribucion de eventos de toque
 */
public class WallpaperDirector implements GLSurfaceView.Renderer {
    private static final String TAG = "WallpaperDirector";

    // ACTORES ESPECIALIZADOS
    private RenderModeController modeController;
    private PanelModeRenderer panelRenderer;
    private SceneFactory sceneFactory;
    private SongSharingController songSharing;
    private TouchRouter touchRouter;

    // SISTEMAS COMPARTIDOS
    private CameraController camera;
    private ResourceManager resources;
    private TextureManager textureManager;
    private MusicVisualizer musicVisualizer;
    private ScreenEffectsManager screenEffects;
    private EventBus eventBus;
    private FirebaseQueueManager firebaseQueue;
    private BloomEffect bloomEffect;
    private ResourcePreloader resourcePreloader;

    // ESTADO
    private final Context context;
    private boolean initialized = false;
    private boolean paused = false;
    private long lastBandsLogTime = 0;  // Para reducir logging excesivo
    private int screenWidth = 1;
    private int screenHeight = 1;
    private String pendingSceneName = "";
    private boolean pendingPreviewMode = false; // Para guardar preview mode antes de inicializar

    // 🔧 FIX MEMORY LEAK: Destruir escenas en GL thread, no en UI thread
    private volatile boolean pendingSceneDestroy = false;
    private volatile boolean pendingReturnToPanel = false;


    // TIMING (deltaTime y FPS manejados por GLStateManager)
    private static final float TIME_WRAP = 3600f;  // Reset cada hora para evitar overflow
    private float totalTime = 0f;
    private final float[] identityMatrix = new float[16];


    public WallpaperDirector(Context context) {
        this.context = context.getApplicationContext();
        this.eventBus = EventBus.get();
        Matrix.setIdentityM(identityMatrix, 0);
        Log.d(TAG, "WallpaperDirector v2.0 - Arquitectura de Actores");
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated START");

        // 🎮 GLStateManager: Actor especializado en configuracion de OpenGL
        GLStateManager.get().initialize();

        initializeSharedSystems();
        initializeActors();
        wireActors();

        if (modeController.isPreviewMode()) {
            Log.d(TAG, "PREVIEW MODE - cargando escena directamente: " + pendingSceneName);
            modeController.goDirectToWallpaper();
            sceneFactory.createScene(pendingSceneName);
            // 🔧 OPTIMIZACIÓN: Liberar recursos del panel en preview mode también
            panelRenderer.onWallpaperActivated();
        }
        // Modo normal: PANEL_MODE → usuario presiona botón → WALLPAPER_MODE

        initialized = true;
        Log.d(TAG, "onSurfaceCreated END");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "onSurfaceChanged: " + width + "x" + height);

        // 🎮 GLStateManager: Configurar viewport
        GLStateManager.get().setViewport(width, height);

        screenWidth = width;
        screenHeight = height;

        // 📐 ScreenManager: Actor especializado en dimensiones
        ScreenManager.updateDimensions(width, height);

        // 📐 AspectRatioManager: Notifica a todos los listeners registrados
        AspectRatioManager.get().updateDimensions(width, height);

        if (camera != null) camera.updateProjection(width, height);
        if (panelRenderer != null) panelRenderer.setScreenSize(width, height);
        if (sceneFactory != null) sceneFactory.setScreenSize(width, height);
        if (touchRouter != null) touchRouter.setScreenSize(width, height);
        if (bloomEffect != null) bloomEffect.resize(width, height);
        UIController.get().setScreenSize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Reset pools de matrices para evitar allocations en draw
        MatrixPool.reset();

        // 🎮 GLStateManager: Inicio de frame (calcula deltaTime y limpia buffers)
        float deltaTime = GLStateManager.get().beginFrame();

        if (!initialized) {
            return;
        }

        // 🔧 FIX MEMORY LEAK: Procesar destrucción de escenas pendientes EN el GL thread
        // Esto garantiza que glDeleteTextures/glDeleteProgram funcionen correctamente
        if (pendingSceneDestroy) {
            pendingSceneDestroy = false;
            Log.d(TAG, "🗑️ [GL Thread] Destruyendo escena pendiente...");
            if (sceneFactory != null) {
                sceneFactory.destroyCurrentScene();
            }
            if (pendingReturnToPanel) {
                pendingReturnToPanel = false;
                if (panelRenderer != null) {
                    panelRenderer.onReturnToPanel();
                }
            }
            Log.d(TAG, "✅ [GL Thread] Escena destruida - memoria GPU liberada");
        }

        // Actualizar tiempo total para animaciones
        updateTotalTime(deltaTime);

        RenderModeController.RenderMode mode = modeController.getCurrentMode();
        switch (mode) {
            case PANEL_MODE:
                panelRenderer.updatePanelMode(deltaTime);
                panelRenderer.drawPanelMode();
                break;
            case LOADING_MODE:
                panelRenderer.updateLoadingMode(deltaTime);
                panelRenderer.drawLoadingMode();
                checkLoadingComplete();
                break;
            case WALLPAPER_MODE:
                updateWallpaperMode(deltaTime);
                drawWallpaperMode();
                break;
        }

        // 🔴 STOP BUTTON: Dibujar como LO ÚLTIMO ABSOLUTO (solo en WALLPAPER_MODE)
        if (mode == RenderModeController.RenderMode.WALLPAPER_MODE) {
            GLES30.glDisable(GLES30.GL_DEPTH_TEST);
            GLES30.glDisable(GLES30.GL_CULL_FACE);
            GLES30.glEnable(GLES30.GL_BLEND);
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
            GLES30.glViewport(0, 0, screenWidth, screenHeight);

        }
    }

    private void updateWallpaperMode(float deltaTime) {
        if (musicVisualizer != null) {
            // 🔧 AUTO-RECOVERY: Si estamos renderizando pero el visualizer está pausado,
            // reanudarlo automáticamente (fix para callbacks desordenados de Android)
            if (!musicVisualizer.isEnabled()) {
                Log.d(TAG, "🔧 Auto-recovery: MusicVisualizer pausado durante render, reanudando...");
                musicVisualizer.resume();
            }

            WallpaperScene scene = sceneFactory.getCurrentScene();
            float[] bands = musicVisualizer.getFrequencyBands();

            // Debug: verificar flujo de datos (solo log cada 2 segundos para evitar spam)
            float sum = 0;
            if (bands != null) for (float b : bands) sum += b;
            long now = System.currentTimeMillis();
            if (sum > 0.5f && (now - lastBandsLogTime) > 2000) {
                Log.d(TAG, "🎶 Bands OK sum=" + String.format("%.2f", sum) + " scene=" + (scene != null ? scene.getClass().getSimpleName() : "null"));
                lastBandsLogTime = now;
            }

            if (scene instanceof OceanFloorScene) {
                // 🌊 Abyssia tiene ecualizador
                ((OceanFloorScene) scene).updateMusicBands(bands);
            } else if (scene instanceof LabScene) {
                // 🔥 Pyralis tiene ecualizador
                ((LabScene) scene).updateMusicBands(bands);
            } else if (scene instanceof GokuScene) {
                // 🐉 Goku tiene ecualizador KAMEHAMEHA
                ((GokuScene) scene).updateMusicBands(bands);
            } else if (scene instanceof AdventureTimeScene) {
                // 🌳 Adventure Time tiene ecualizador PYRALIS
                ((AdventureTimeScene) scene).updateMusicBands(bands);
            } else if (scene instanceof NeonCityScene) {
                // 🌆 Neon City tiene ecualizador SYNTHWAVE
                ((NeonCityScene) scene).updateMusicBands(bands);
            } else if (scene instanceof SaintSeiyaScene) {
                // ⭐ Saint Seiya tiene ecualizador COSMOS
                ((SaintSeiyaScene) scene).updateMusicBands(bands);
            } else if (scene instanceof WalkingDeadScene) {
                // 🧟 Walking Dead tiene ecualizador WALKING_DEAD
                ((WalkingDeadScene) scene).updateMusicBands(bands);
            } else if (scene instanceof ZeldaParallaxScene) {
                // 🗡️ Zelda BOTW tiene ecualizador ZELDA
                ((ZeldaParallaxScene) scene).updateMusicBands(bands);
            } else if (scene instanceof SupermanScene) {
                // 🦸 Superman tiene ecualizador SUPERMAN
                ((SupermanScene) scene).updateMusicBands(bands);
            } else if (scene instanceof AOTScene) {
                // ⚔️ AOT tiene ecualizador AOT
                ((AOTScene) scene).updateMusicBands(bands);
            }
        }
        sceneFactory.updateCurrentScene(deltaTime);
        if (screenEffects != null) screenEffects.update(deltaTime);
        panelRenderer.updateWallpaperMode(deltaTime);
        songSharing.update(deltaTime);
    }

    private void drawWallpaperMode() {
        // ✨ Bloom: DESHABILITADO TEMPORALMENTE para debug
        // if (bloomEffect != null && bloomEffect.isEnabled()) {
        //     bloomEffect.beginCapture();
        // }

        // Dibujar escena 3D
        sceneFactory.drawCurrentScene();
        if (screenEffects != null) screenEffects.draw();

        // ✨ Bloom: DESHABILITADO TEMPORALMENTE
        // if (bloomEffect != null && bloomEffect.isEnabled()) {
        //     bloomEffect.endCaptureAndApply();
        // }

        // Song sharing UI
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        songSharing.draw(identityMatrix, totalTime);

        // UI overlay
        GLES30.glDisable(GLES30.GL_CULL_FACE);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        GLES30.glViewport(0, 0, screenWidth, screenHeight);

        panelRenderer.drawWallpaperOverlay();

        // Restaurar estado
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_CULL_FACE);
    }

    private void checkLoadingComplete() {
        if (panelRenderer.isLoadingComplete()) onLoadingComplete();
    }

    private void onLoadingComplete() {
        if (modeController.isWallpaperMode()) return;
        Log.d(TAG, "Carga completada - activando wallpaper");
        sceneFactory.createScene(pendingSceneName);
        modeController.activateWallpaper();
        panelRenderer.onWallpaperActivated();

        // Habilitar saludos Gemini para todos los wallpapers
        panelRenderer.setGreetingEnabled(true);

        // 🎵 Solo reconectar MusicVisualizer si NO está funcionando
        // ⚠️ Reconectar puede pausar Spotify, así que evitarlo si ya funciona
        if (musicVisualizer != null) {
            if (musicVisualizer.isEnabled() && musicVisualizer.isReceivingAudio()) {
                Log.d(TAG, "🎵 MusicVisualizer ya funcionando, sin reconectar");
            } else {
                Log.d(TAG, "🎵 MusicVisualizer no activo, reanudando...");
                musicVisualizer.resume();  // Usar resume() en vez de reconnect()
            }
        }
    }

    public void startLoading() {
        if (!modeController.startLoading()) {
            return;
        }

        // 📊 Obtener info del wallpaper para el tema de la barra
        WallpaperItem wallpaperItem = WallpaperCatalog.get().getBySceneName(pendingSceneName);
        String displayName = wallpaperItem != null ? wallpaperItem.getNombre() : pendingSceneName;
        int glowColor = wallpaperItem != null ? wallpaperItem.getGlowColor() : 0xFF00D4FF;
        int previewResourceId = wallpaperItem != null ? wallpaperItem.getResourceIdPreview() : 0;

        // 🖼️ Iniciar pantalla de carga con preview del wallpaper
        panelRenderer.onStartLoadingWithPreview(pendingSceneName, displayName, glowColor, previewResourceId);

        // 📦 Crear ResourcePreloader y preparar tareas para la escena
        resourcePreloader = new ResourcePreloader(context);
        resourcePreloader.setListener(new ResourcePreloader.PreloadListener() {
            @Override
            public void onProgressUpdate(int progress, int total, String currentTask) {
                // Actualizar LoadingBar con progreso real
                float percent = total > 0 ? (float) progress / total : 0f;
                panelRenderer.updateLoadingProgress(percent, currentTask);
            }

            @Override
            public void onPreloadComplete() {
                Log.d(TAG, "✅ ResourcePreloader completado - recursos listos");
                // Marcar progreso al 100% y dejar que el sistema detecte la completación
                panelRenderer.updateLoadingProgress(1.0f, "¡Listo!");
            }

            @Override
            public void onPreloadError(String error) {
                Log.e(TAG, "❌ Error en ResourcePreloader: " + error);
                // Aún así intentar cargar (los recursos pueden estar parcialmente disponibles)
                panelRenderer.updateLoadingProgress(1.0f, "Iniciando...");
            }
        });

        // Preparar y ejecutar tareas de la escena
        resourcePreloader.prepareTasksForScene(pendingSceneName);
        resourcePreloader.startPreloading();

        Log.d(TAG, "📦 ResourcePreloader iniciado para: " + pendingSceneName);
    }

    public void switchToPanelMode() {
        if (modeController.stopWallpaper()) {
            // 🔧 FIX MEMORY LEAK: Programar destrucción en GL thread
            pendingSceneDestroy = true;
            pendingReturnToPanel = true;
            Log.d(TAG, "🔧 switchToPanelMode: destrucción programada en GL thread");
        }
    }

    private void initializeSharedSystems() {
        Log.d(TAG, "Inicializando sistemas compartidos...");
        camera = new CameraController();
        camera.setMode(CameraController.CameraMode.PERSPECTIVE_3_4);
        resources = ResourceManager.get();
        resources.init(context);
        textureManager = new TextureManager(context);
        // 🎵 Usar constructor con Context para habilitar auto-resume de música
        musicVisualizer = new MusicVisualizer(context);
        musicVisualizer.initialize();
        screenEffects = new ScreenEffectsManager();
        // TODO: BloomEffect deshabilitado temporalmente para debugging
        // bloomEffect = new BloomEffect();

        // 💥 Suscribir a eventos de efectos de pantalla via EventBus
        subscribeToScreenEffectEvents();

        // Inicializar FirebaseQueueManager para batching de operaciones
        try {
            firebaseQueue = FirebaseQueueManager.getInstance(context);
            Log.d(TAG, "FirebaseQueueManager inicializado");
        } catch (Exception e) {
            Log.w(TAG, "FirebaseQueueManager no disponible: " + e.getMessage());
        }

        Log.d(TAG, "Sistemas compartidos OK");
    }

    private void initializeActors() {
        Log.d(TAG, "Inicializando 5 actores...");
        modeController = new RenderModeController();
        // Aplicar preview mode pendiente
        if (pendingPreviewMode) {
            modeController.setPreviewMode(true);
        }
        panelRenderer = new PanelModeRenderer(context);
        panelRenderer.initialize();

        sceneFactory = new SceneFactory();
        sceneFactory.setContext(context);
        sceneFactory.setTextureManager(textureManager);
        sceneFactory.setCamera(camera);
        sceneFactory.setResourceManager(resources);
        sceneFactory.registerDefaultScenes();
        songSharing = new SongSharingController(context);
        songSharing.initialize();
        touchRouter = new TouchRouter();
        Log.d(TAG, "5 actores inicializados OK");
    }

    private void wireActors() {
        Log.d(TAG, "Conectando actores...");
        touchRouter.setPanelRenderer(panelRenderer);
        touchRouter.setSongSharing(songSharing);
        touchRouter.setModeController(modeController);
        touchRouter.setListener(new TouchRouter.TouchListener() {
            @Override public void onPlayButtonTapped() { startLoading(); }
            @Override public void onLikeButtonPressed() {}
            @Override public void onLikeButtonReleased() {}
            @Override public void onLikeButtonTapped() { songSharing.shareSongWithAI(); }
            @Override public boolean onSceneTouched(float nx, float ny, int action) {
                return sceneFactory.onSceneTouchEvent(nx, ny, action);
            }
        });
        panelRenderer.setLoadingListener(this::onLoadingComplete);
        Log.d(TAG, "Actores conectados OK");

        // 🎄 Aplicar modo pendiente ahora que panelRenderer está listo
        if (pendingSceneName != null) {
            applySceneModeToPanel(pendingSceneName);
        }
    }

    /**
     * Aplica el modo correcto al panel según el nombre de la escena
     */
    private void applySceneModeToPanel(String sceneName) {
        if (panelRenderer == null || sceneName == null) return;
        Log.d(TAG, "📱 Modo ESTÁNDAR aplicado: " + sceneName);

        // Establecer tema del botón Like según la escena
        if (songSharing != null) {
            if (sceneName.equals("ABYSSIA") || sceneName.toLowerCase().contains("abyssia")) {
                songSharing.setLikeButtonTheme(LikeButton.Theme.ABYSSIA);
            } else if (sceneName.equals("PYRALIS") || sceneName.toLowerCase().contains("pyralis")) {
                songSharing.setLikeButtonTheme(LikeButton.Theme.PYRALIS);
            } else if (sceneName.equals("ADVENTURE_TIME") || sceneName.toLowerCase().contains("adventure")) {
                songSharing.setLikeButtonTheme(LikeButton.Theme.ADVENTURE_TIME);
            } else if (sceneName.equals("GOKU") || sceneName.toLowerCase().contains("goku")) {
                songSharing.setLikeButtonTheme(LikeButton.Theme.GOKU);
            } else if (sceneName.equals("NEON_CITY") || sceneName.toLowerCase().contains("neon")) {
                songSharing.setLikeButtonTheme(LikeButton.Theme.SYNTHWAVE);
            } else if (sceneName.equals("SAINT_SEIYA") || sceneName.toLowerCase().contains("seiya")) {
                songSharing.setLikeButtonTheme(LikeButton.Theme.COSMOS);
            } else if (sceneName.equals("SUPERMAN") || sceneName.toLowerCase().contains("superman")) {
                songSharing.setLikeButtonTheme(LikeButton.Theme.SUPERMAN);
            } else if (sceneName.equals("WALKING_DEAD") || sceneName.toLowerCase().contains("walking")) {
                songSharing.setLikeButtonTheme(LikeButton.Theme.WALKING_DEAD);
            } else if (sceneName.equals("AOT") || sceneName.toLowerCase().contains("aot") || sceneName.toLowerCase().contains("titan")) {
                songSharing.setLikeButtonTheme(LikeButton.Theme.AOT);
            } else {
                songSharing.setLikeButtonTheme(LikeButton.Theme.DEFAULT);
            }
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (!initialized) return false;
        touchRouter.setCurrentScene(sceneFactory.getCurrentScene());
        return touchRouter.onTouchEvent(event);
    }

    // NOTA: deltaTime y FPS ahora son manejados por GLStateManager
    // Solo mantenemos totalTime para animaciones que lo necesiten
    private void updateTotalTime(float deltaTime) {
        totalTime += deltaTime;
        if (totalTime > TIME_WRAP) totalTime -= TIME_WRAP;  // Evitar overflow
    }

    public void pause() {
        paused = true;

        if (sceneFactory != null) {
            sceneFactory.pauseCurrentScene();
        }
        if (musicVisualizer != null) musicVisualizer.pause();

        // ⏸️ Pausar video del panel para ahorrar batería y CPU
        if (panelRenderer != null) panelRenderer.pause();

        // Flush Firebase queue al pausar para guardar datos pendientes
        try {
            if (firebaseQueue != null) {
                firebaseQueue.forceFlush();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error en forceFlush durante pause: " + e.getMessage());
        }

        Log.d(TAG, "WallpaperDirector pausado");
    }

    public void resume() {
        paused = false;

        // 🎯 Solo reanudar el video correspondiente al MODO ACTUAL
        RenderModeController.RenderMode currentMode = modeController != null ?
            modeController.getCurrentMode() : RenderModeController.RenderMode.PANEL_MODE;

        if (currentMode == RenderModeController.RenderMode.WALLPAPER_MODE) {
            // En WALLPAPER_MODE: Solo reanudar escena (NO panel video)
            if (sceneFactory != null && sceneFactory.hasCurrentScene()) {
                sceneFactory.resumeCurrentScene();
            }
            // Asegurar que panel video está PAUSADO
            if (panelRenderer != null) panelRenderer.pause();
            Log.d(TAG, "▶️ Resume WALLPAPER_MODE: Solo escena activa");
        } else {
            // En PANEL_MODE o LOADING_MODE: Solo reanudar panel video (NO escena)
            if (panelRenderer != null) panelRenderer.resume();
            // Pausar escena si existe
            if (sceneFactory != null && sceneFactory.hasCurrentScene()) {
                sceneFactory.pauseCurrentScene();
            }
            Log.d(TAG, "▶️ Resume PANEL_MODE: Solo panel activo");
        }

        // Preview mode handling
        if (modeController != null && modeController.isPreviewMode()) {
            modeController.goDirectToWallpaper();
        }

        // 🎵 Reanudar MusicVisualizer - SIEMPRE reconectar para evitar estados inválidos
        // después de ciclos rápidos de pause/resume del sistema Android
        if (musicVisualizer != null) {
            // SIMPLIFICADO: siempre llamar resume() que maneja reconexión automática si falla
            musicVisualizer.resume();
            Log.d(TAG, "🎵 MusicVisualizer estado después de resume: enabled=" + musicVisualizer.isEnabled());
        }
        Log.d(TAG, "WallpaperDirector reanudado");
    }

    public void setPreviewMode(boolean preview) {
        pendingPreviewMode = preview;
        if (modeController != null) {
            modeController.setPreviewMode(preview);
        }
    }

    public void changeScene(String sceneName) {
        Log.d(TAG, "🔄 changeScene llamado: " + sceneName + " (actual: " + pendingSceneName + ")");

        // Si es la misma escena, no hacer nada
        if (sceneName != null && sceneName.equals(pendingSceneName)) {
            Log.d(TAG, "📱 Escena ya seleccionada: " + sceneName);
            return;
        }

        String previousScene = pendingSceneName;
        pendingSceneName = sceneName;

        // ⚡ FIX: Si estamos en WALLPAPER_MODE con una escena activa,
        // forzar retorno al panel para que el usuario presione PLAY de nuevo
        if (modeController != null && modeController.isWallpaperMode() && sceneFactory != null) {
            Log.d(TAG, "⚡ Escena activa detectada (" + previousScene + "), programando destrucción en GL thread");
            // 🔧 FIX MEMORY LEAK: Marcar para destruir en GL thread, NO destruir aquí (UI thread)
            // Esto evita que glDeleteTextures/glDeleteProgram fallen silenciosamente
            pendingSceneDestroy = true;
            pendingReturnToPanel = true;
            modeController.stopWallpaper();
            Log.d(TAG, "✅ Destrucción programada - Usuario debe presionar PLAY para: " + sceneName);
        } else if (panelRenderer != null) {
            Log.d(TAG, "📱 Modo ESTÁNDAR para: " + sceneName);
        } else {
            Log.d(TAG, "Panel mode pendiente - panelRenderer aún no inicializado");
        }
    }

    public void release() {
        Log.d(TAG, "Liberando WallpaperDirector...");
        // NOTA: release() se llama durante la destrucción del servicio.
        // Aunque esto no es en el GL thread, el proceso terminará pronto
        // y todos los recursos GPU se liberarán con el contexto GL.
        // El fix de memory leak principal es para scene switches en runtime.
        if (sceneFactory != null) sceneFactory.destroyCurrentScene();
        if (songSharing != null) songSharing.release();
        if (musicVisualizer != null) musicVisualizer.release();
        if (resources != null) resources.release();
        if (screenEffects != null) screenEffects.release();
        if (bloomEffect != null) bloomEffect.release();

        // Flush final y liberar FirebaseQueueManager
        if (firebaseQueue != null) {
            firebaseQueue.forceFlush();
            firebaseQueue.release();
            firebaseQueue = null;
        }

        ResourceManager.reset();
        UIController.reset();
        EventBus.reset();
        GLStateManager.reset();
        ScreenManager.reset();
        AspectRatioManager.reset();
        initialized = false;
        Log.d(TAG, "WallpaperDirector liberado");
    }

    // GETTERS
    public boolean isInitialized() { return initialized; }
    public boolean isPaused() { return paused; }
    public float getCurrentFPS() { return GLStateManager.get().getFPS(); }
    public CameraController getCamera() { return camera; }
    public int getScreenWidth() { return screenWidth; }
    public int getScreenHeight() { return screenHeight; }

    public RenderModeController.RenderMode getCurrentRenderMode() {
        return modeController != null ? modeController.getCurrentMode() : RenderModeController.RenderMode.PANEL_MODE;
    }

    public String getCurrentSceneName() {
        return sceneFactory != null ? sceneFactory.getCurrentSceneName() : "";
    }

    public WallpaperScene getCurrentScene() {
        return sceneFactory != null ? sceneFactory.getCurrentScene() : null;
    }

    public boolean isPlaying() {
        return modeController != null && modeController.isPlaying();
    }

    public void setPlaying(boolean playing) {
        // DESHABILITADO: El wallpaper NO debe reaccionar a play/pause de música
        // Los wallpapers de video deben correr independiente de la música
    }

    // ═══════════════════════════════════════════════════════════════
    // 💥 SUSCRIPCIÓN A EVENTOS DE EFECTOS DE PANTALLA
    // ═══════════════════════════════════════════════════════════════

    /**
     * Suscribe a eventos de efectos de pantalla via EventBus
     * Los eventos son publicados por MeteorShower y otros sistemas
     */
    private void subscribeToScreenEffectEvents() {
        // 💥 Impacto en pantalla (flash)
        eventBus.subscribe(EventBus.SCREEN_IMPACT, data -> {
            if (screenEffects != null) {
                float intensity = data.getFloat("intensity", 0.3f);
                screenEffects.triggerScreenImpact(intensity);
            }
        });

        // 💥💥 Grietas en pantalla
        eventBus.subscribe(EventBus.SCREEN_CRACK, data -> {
            if (screenEffects != null) {
                float x = data.getFloat("x", 0.5f);
                float y = data.getFloat("y", 0.5f);
                float intensity = data.getFloat("intensity", 0.8f);
                screenEffects.triggerScreenCrack(x, y, intensity);
            }
        });

        // 🌍💥 Impacto en la Tierra (efectos especiales)
        eventBus.subscribe(EventBus.EARTH_IMPACT, data -> {
            if (screenEffects != null) {
                // El impacto en la Tierra genera un flash más intenso
                screenEffects.triggerScreenImpact(0.4f);
            }
        });

        Log.d(TAG, "💥 Suscrito a eventos de efectos de pantalla");
    }
}
