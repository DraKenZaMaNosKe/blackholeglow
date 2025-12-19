package com.secret.blackholeglow.core;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.MotionEvent;

import com.secret.blackholeglow.CameraController;
import com.secret.blackholeglow.MusicVisualizer;
import com.secret.blackholeglow.ResourceLoader;
import com.secret.blackholeglow.TextureManager;
import com.secret.blackholeglow.scenes.BatallaCosmicaScene;
import com.secret.blackholeglow.scenes.SceneConstants;
import com.secret.blackholeglow.scenes.WallpaperScene;
import com.secret.blackholeglow.systems.AspectRatioManager;
import com.secret.blackholeglow.systems.EventBus;
import com.secret.blackholeglow.systems.FirebaseQueueManager;
import com.secret.blackholeglow.systems.GLStateManager;
import com.secret.blackholeglow.systems.ResourceManager;
import com.secret.blackholeglow.systems.ScreenEffectsManager;
import com.secret.blackholeglow.systems.ScreenManager;
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
    private ResourceLoader resourceLoader;
    private EventBus eventBus;
    private FirebaseQueueManager firebaseQueue;
    private BloomEffect bloomEffect;

    // ESTADO
    private final Context context;
    private boolean initialized = false;
    private boolean paused = false;
    private int screenWidth = 1;
    private int screenHeight = 1;
    private String pendingSceneName = "";
    private boolean pendingPreviewMode = false; // Para guardar preview mode antes de inicializar
    private boolean pendingArcadeMode = false;  // 🎮 Para guardar arcade mode antes de inicializar

    // 🎄 FILAMENT CALLBACK - Para escenas que usan Filament (Christmas)
    public interface OnFilamentSceneListener {
        void onFilamentSceneRequested(String sceneName);
    }
    private OnFilamentSceneListener filamentListener;

    public void setOnFilamentSceneListener(OnFilamentSceneListener listener) {
        this.filamentListener = listener;
    }

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
            // 🎄 Para Christmas: SIEMPRE mostrar panel primero (para el botón de Filament)
            if (pendingSceneName != null && pendingSceneName.contains("Navideño")) {
                Log.d(TAG, "🎄 PREVIEW MODE + Christmas: Mostrando panel con botón");
                // No cargar escena directamente - dejar que el usuario presione el botón
            } else {
                Log.d(TAG, "PREVIEW MODE - cargando escena directamente");
                modeController.goDirectToWallpaper();
                sceneFactory.createScene(pendingSceneName);
            }
        }

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

            // Dibujar MiniStopButton directamente
            if (panelRenderer != null && panelRenderer.getMiniStopButton() != null) {
                panelRenderer.getMiniStopButton().update(deltaTime);
                panelRenderer.getMiniStopButton().draw();
            }
        }
    }

    private void updateWallpaperMode(float deltaTime) {
        if (musicVisualizer != null) {
            WallpaperScene scene = sceneFactory.getCurrentScene();
            if (scene instanceof BatallaCosmicaScene) {
                // Usar las 32 bandas de frecuencia para mejor visualización
                ((BatallaCosmicaScene) scene).updateMusicBands(
                    musicVisualizer.getFrequencyBands()
                );
                // También actualizar los valores legacy (bass/mid/treble)
                ((BatallaCosmicaScene) scene).updateMusicLevels(
                    musicVisualizer.getBassLevel(),
                    musicVisualizer.getMidLevel(),
                    musicVisualizer.getTrebleLevel()
                );
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

        // 🔴 STOP BUTTON: Asegurar que esté ENCIMA de todo
        // Resetear completamente el estado OpenGL
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glDisable(GLES30.GL_CULL_FACE);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // Asegurar viewport correcto
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

        // Deshabilitar saludos Gemini para Batalla Cósmica (se usará en otros wallpapers)
        if (pendingSceneName.contains("Batalla") || pendingSceneName.contains("Universo")) {
            panelRenderer.setGreetingEnabled(false);
        } else {
            panelRenderer.setGreetingEnabled(true);
        }

        // 🎵 CRÍTICO: Forzar reconexión del MusicVisualizer al entrar en WALLPAPER_MODE
        // Esto es necesario porque el visualizador puede haber perdido la conexión
        // durante las transiciones de preview a wallpaper real
        if (musicVisualizer != null) {
            Log.d(TAG, "🎵 Forzando reconexión de MusicVisualizer para WALLPAPER_MODE...");
            // Siempre reconectar para asegurar que funcione
            musicVisualizer.reconnect();
            Log.d(TAG, "🎵 MusicVisualizer reconectado para WALLPAPER_MODE (enabled=" + musicVisualizer.isEnabled() + ")");
        }
    }

    public void startLoading() {
        // 🎄 Si es escena navideña, usar Filament en lugar de OpenGL ES
        if (pendingSceneName != null && pendingSceneName.contains("Navideño")) {
            if (filamentListener != null) {
                Log.d(TAG, "🎄 Solicitando cambio a Filament para: " + pendingSceneName);
                filamentListener.onFilamentSceneRequested(pendingSceneName);
                return;
            }
        }

        if (modeController.startLoading()) {
            // 🖼️ Pasar el nombre de la escena para fondo dinámico
            panelRenderer.onStartLoading(pendingSceneName);
        }
    }

    public void switchToPanelMode() {
        if (modeController.stopWallpaper()) {
            panelRenderer.onReturnToPanel();
            sceneFactory.destroyCurrentScene();
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
        resourceLoader = new ResourceLoader(context, textureManager);
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

        // 🎮 Aplicar modo arcade pendiente
        if (pendingArcadeMode) {
            panelRenderer.setArcadeModeEnabled(true);
            Log.d(TAG, "🎮 Modo ARCADE aplicado (estaba pendiente)");
        }

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
            @Override public void onStopButtonTapped() { switchToPanelMode(); }
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
     * 🎄 Aplica el modo correcto al panel según el nombre de la escena
     */
    private void applySceneModeToPanel(String sceneName) {
        if (panelRenderer == null || sceneName == null) return;

        boolean shouldUseArcade = sceneName.contains("Batalla") || sceneName.contains("Universo");
        boolean shouldUseChristmas = sceneName.contains("Bosque") || sceneName.contains("Navide");

        if (shouldUseArcade) {
            panelRenderer.setChristmasModeEnabled(false);
            panelRenderer.setArcadeModeEnabled(true);
            panelRenderer.setStopButtonPosition(
                SceneConstants.StopButton.BATALLA_X,
                SceneConstants.StopButton.BATALLA_Y
            );
            Log.d(TAG, "🎮 Modo ARCADE APLICADO en panel: " + sceneName);
        } else if (shouldUseChristmas) {
            panelRenderer.setArcadeModeEnabled(false);
            panelRenderer.setChristmasModeEnabled(true);
            panelRenderer.setStopButtonPosition(
                SceneConstants.StopButton.CHRISTMAS_X,
                SceneConstants.StopButton.CHRISTMAS_Y
            );
            Log.d(TAG, "🎄 Modo CHRISTMAS APLICADO en panel: " + sceneName);
        } else {
            panelRenderer.setArcadeModeEnabled(false);
            panelRenderer.setChristmasModeEnabled(false);
            panelRenderer.setStopButtonPosition(
                SceneConstants.StopButton.DEFAULT_X,
                SceneConstants.StopButton.DEFAULT_Y
            );
            Log.d(TAG, "📱 Modo ESTÁNDAR aplicado: " + sceneName);
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
        if (modeController != null && !modeController.isPreviewMode()) {
            switchToPanelMode();
        }
        if (sceneFactory != null) {
            sceneFactory.pauseCurrentScene();
        }
        if (musicVisualizer != null) musicVisualizer.pause();

        // Flush Firebase queue al pausar para guardar datos pendientes
        // NOTA: Envuelto en try-catch para evitar crash si el handler está muerto
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
        // GLStateManager maneja el timing automáticamente en beginFrame()
        if (modeController != null && sceneFactory != null &&
            modeController.isPreviewMode() && sceneFactory.hasCurrentScene()) {
            sceneFactory.resumeCurrentScene();
            modeController.goDirectToWallpaper();
        }

        // 🎵 Reanudar MusicVisualizer - SIEMPRE reconectar en WALLPAPER_MODE
        // para evitar problemas de conexión perdida
        if (musicVisualizer != null) {
            if (modeController != null && modeController.isWallpaperMode()) {
                // En WALLPAPER_MODE siempre reconectar para garantizar funcionamiento
                Log.d(TAG, "🎵 Resume en WALLPAPER_MODE: Reconectando MusicVisualizer...");
                musicVisualizer.reconnect();
            } else {
                // En otros modos, solo hacer resume normal
                musicVisualizer.resume();
            }
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
        Log.d(TAG, "Escena pendiente: " + sceneName);
        pendingSceneName = sceneName;

        // 🎮 Determinar si debe usar modo ARCADE (para Batalla Cósmica)
        boolean shouldUseArcade = sceneName.contains("Batalla") || sceneName.contains("Universo");
        pendingArcadeMode = shouldUseArcade;

        // 🎄 Determinar si debe usar modo CHRISTMAS (para Bosque Navideño)
        boolean shouldUseChristmas = sceneName.contains("Bosque") || sceneName.contains("Navide");

        // Activar el modo correcto en el panel
        if (panelRenderer != null) {
            if (shouldUseArcade) {
                panelRenderer.setArcadeModeEnabled(true);
                // 🔴 Posición del botón stop para Batalla Cósmica
                panelRenderer.setStopButtonPosition(
                    SceneConstants.StopButton.BATALLA_X,
                    SceneConstants.StopButton.BATALLA_Y
                );
                Log.d(TAG, "🎮 Modo ARCADE ACTIVADO para: " + sceneName);
            } else if (shouldUseChristmas) {
                panelRenderer.setChristmasModeEnabled(true);
                // 🔴 Posición del botón stop para Christmas
                panelRenderer.setStopButtonPosition(
                    SceneConstants.StopButton.CHRISTMAS_X,
                    SceneConstants.StopButton.CHRISTMAS_Y
                );
                Log.d(TAG, "🎄 Modo CHRISTMAS ACTIVADO para: " + sceneName);
            } else {
                // Desactivar todos los modos especiales
                panelRenderer.setArcadeModeEnabled(false);
                panelRenderer.setChristmasModeEnabled(false);
                // 🔴 Posición por defecto del botón stop
                panelRenderer.setStopButtonPosition(
                    SceneConstants.StopButton.DEFAULT_X,
                    SceneConstants.StopButton.DEFAULT_Y
                );
                Log.d(TAG, "📱 Modo ESTÁNDAR para: " + sceneName);
            }
        } else {
            Log.d(TAG, "Panel mode pendiente - panelRenderer aún no inicializado");
        }
    }

    public void release() {
        Log.d(TAG, "Liberando WallpaperDirector...");
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
        if (modeController == null) return;
        if (playing && !modeController.isWallpaperMode()) startLoading();
        else if (!playing && !modeController.isPanelMode()) switchToPanelMode();
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
