package com.secret.blackholeglow.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import android.view.MotionEvent;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import com.secret.blackholeglow.CameraController;
import com.secret.blackholeglow.MusicVisualizer;
import com.secret.blackholeglow.TextureManager;
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
import com.secret.blackholeglow.core.MemoryPressureLevel;
import com.secret.blackholeglow.models.SceneWeight;
import com.secret.blackholeglow.models.WallpaperItem;
import com.secret.blackholeglow.systems.UIController;
import com.secret.blackholeglow.gl3.MatrixPool;
import com.secret.blackholeglow.effects.BloomEffect;
import com.secret.blackholeglow.effects.TouchSparkleEffect;
import com.secret.blackholeglow.systems.WallpaperNotificationManager;

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
    private TouchSparkleEffect touchSparkles;
    private ResourcePreloader resourcePreloader;

    // ESTADO
    private final Context context;
    private boolean initialized = false;
    // 🔧 FIX GL FREEZE: volatile porque main thread escribe (pause/resume)
    // y GL thread lee (onDrawFrame). Sin volatile, GL thread podría cachear el valor.
    private volatile boolean paused = false;
    private int screenWidth = 1;
    private int screenHeight = 1;
    private String pendingSceneName = "";
    private boolean pendingPreviewMode = false; // Para guardar preview mode antes de inicializar

    // 🔧 FIX MEMORY LEAK: Destruir escenas en GL thread, no en UI thread
    private volatile boolean pendingSceneDestroy = false;
    private volatile boolean pendingReturnToPanel = false;
    private volatile boolean pendingSceneAutoLoad = false;  // 🔧 FIX FREEZE: Auto-cargar nueva escena después de destruir
    private volatile boolean pendingFileTextureRelease = false;  // 🧠 Release file textures on GL thread
    private final Object pendingFlagsLock = new Object();  // 🔧 Lock for atomic multi-flag reads/writes
    private int resourceCheckRetries = 0;  // 🛡️ Contador de reintentos para verificación de recursos
    private static final int MAX_RESOURCE_CHECK_RETRIES = 300;  // ~10 segundos a 30fps

    // 🔧 FIX LOADING: Fallback de completación de carga
    // Cuando ResourcePreloader termina, marcamos resourcesReady = true.
    // Si LoadingBar.isComplete() no responde en 2 segundos, forzamos la activación.
    private volatile boolean resourcesReady = false;
    private float resourcesReadyTimer = 0f;
    private static final float LOADING_FALLBACK_TIMEOUT = 2.0f;  // 2 segundos máximo de animación

    // Auto-start: inicia wallpaper automáticamente después de 3s en panel
    private float panelAutoStartTimer = 0f;
    private boolean panelAutoStartFired = false;
    private static final float PANEL_AUTO_START_DELAY = 3.0f; // 3 segundos

    // 🔋 Adaptive FPS: reducir FPS cuando no hay actividad
    private long lastActivityTime = System.currentTimeMillis();
    private static final long IDLE_TIMEOUT_MS = 5_000;  // 5s sin actividad → idle
    private boolean isIdle = false;

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

        // 🔧 FIX ANR: NO cargar escena en onSurfaceCreated (es muy pesado y causa ANR)
        // En su lugar, marcar para cargar en el primer frame de onDrawFrame()
        // donde tenemos más control sobre el timing y podemos mostrar loading
        if (pendingSceneName != null && !pendingSceneName.isEmpty()) {
            if (modeController.isPreviewMode()) {
                // 🎬 Preview del sistema: auto-cargar escena directamente (sin panel)
                Log.d(TAG, "🎬 PREVIEW: Auto-cargando escena: " + pendingSceneName);
                pendingSceneAutoLoad = true;
                modeController.goDirectToWallpaper();
            } else {
                // 🎮 Modo normal: mostrar PANEL DE CONTROL - usuario presiona PLAY
                Log.d(TAG, "🎮 PANEL: Esperando PLAY para escena: " + pendingSceneName);
                pendingSceneAutoLoad = false;
                // Se queda en PANEL_MODE por defecto
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

    // 🛡️ ERROR RECOVERY: Contador de errores consecutivos para auto-recovery
    private int consecutiveErrors = 0;
    private static final int MAX_CONSECUTIVE_ERRORS = 3;
    private long lastErrorTime = 0;

    @Override
    public void onDrawFrame(GL10 gl) {
        // Reset pools de matrices para evitar allocations en draw
        MatrixPool.reset();

        // 🎮 GLStateManager: Inicio de frame (calcula deltaTime y limpia buffers)
        float deltaTime = GLStateManager.get().beginFrame();

        if (!initialized) {
            return;
        }

        // 🔧 FIX GL THREAD FREEZE: Cuando está pausado y no hay operaciones pendientes,
        // dormir brevemente y retornar. Esto reemplaza RENDERMODE_WHEN_DIRTY que no
        // despertaba al GL thread de forma confiable al volver a CONTINUOUSLY.
        // El GL thread SIEMPRE corre, pero idle cuando está pausado → garantiza que
        // pendingSceneDestroy SIEMPRE se procesa.
        // 🔧 FIX RACE CONDITION: Snapshot and clear pending flags atomically.
        // Main thread writes multiple flags together (e.g., destroy + returnToPanel).
        // Without lock, GL thread could see destroy=true but returnToPanel=false → black screen.
        boolean doDestroy, doReturnToPanel;
        synchronized (pendingFlagsLock) {
            doDestroy = pendingSceneDestroy;
            doReturnToPanel = pendingReturnToPanel;
            if (doDestroy) pendingSceneDestroy = false;
            if (doReturnToPanel) pendingReturnToPanel = false;
        }

        if (paused && !doDestroy && !pendingSceneAutoLoad) {
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            return;
        }

        // 🔧 FIX MEMORY LEAK: Procesar destrucción de escenas pendientes EN el GL thread
        // Esto garantiza que glDeleteTextures/glDeleteProgram funcionen correctamente
        if (doDestroy) {
            Log.d(TAG, "🗑️ [GL Thread] Destruyendo escena pendiente...");
            try {
                if (sceneFactory != null) {
                    sceneFactory.destroyCurrentScene();
                }
                Log.d(TAG, "✅ [GL Thread] Escena destruida - memoria GPU liberada");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error destruyendo escena: " + e.getMessage(), e);
            }

            // 🔧 SIEMPRE volver al panel, incluso si la destrucción falló
            if (doReturnToPanel) {
                Log.d(TAG, "🔙 [GL Thread] Regresando al panel...");
                if (panelRenderer != null) {
                    panelRenderer.onReturnToPanel();
                    panelRenderer.resume();
                }
            }
        }

        // 🧠 Process pending file texture release on GL thread (from onTrimMemory)
        if (pendingFileTextureRelease) {
            pendingFileTextureRelease = false;
            if (textureManager != null) {
                textureManager.releaseFileTextures();
                Log.d(TAG, "🧠 [GL Thread] File textures released due to memory pressure");
            }
        }

        // 🔧 FIX ANR: Auto-cargar escena pendiente (después de destrucción O en primera carga)
        // Esto se ejecuta en onDrawFrame() en lugar de onSurfaceCreated() para evitar ANR
        if (pendingSceneAutoLoad) {
            if (sceneFactory != null && pendingSceneName != null && !pendingSceneName.isEmpty()) {

                // 🛡️ FIX BLACK SCREEN: Verificar que los recursos están disponibles ANTES de crear
                // Si el video no se ha descargado aún, esperar y reintentar en el siguiente frame
                if (!ResourcePreloader.areSceneResourcesReady(context, pendingSceneName)) {
                    resourceCheckRetries++;
                    if (resourceCheckRetries % 30 == 1) {  // Log cada ~1 segundo
                        Log.d(TAG, "⏳ [GL Thread] Esperando recursos para: " + pendingSceneName
                                + " (intento " + resourceCheckRetries + "/" + MAX_RESOURCE_CHECK_RETRIES + ")");
                    }
                    if (resourceCheckRetries >= MAX_RESOURCE_CHECK_RETRIES) {
                        // 🛡️ FIX BLACK SCREEN: NO cargar escena sin recursos - volver al panel
                        Log.e(TAG, "🛡️ [GL Thread] Timeout esperando recursos - abortando carga: " + pendingSceneName);
                        pendingSceneAutoLoad = false;
                        resourceCheckRetries = 0;
                        // Volver al panel de forma segura en vez de mostrar pantalla negra
                        if (modeController != null) {
                            modeController.stopWallpaper();
                        }
                        if (panelRenderer != null) {
                            panelRenderer.onReturnToPanel();
                            panelRenderer.resume();
                        }
                        return;
                    } else {
                        // No está listo, reintentar en el siguiente frame
                        return;
                    }
                }

                pendingSceneAutoLoad = false;
                resourceCheckRetries = 0;
                Log.d(TAG, "🎬 [GL Thread] Auto-cargando escena: " + pendingSceneName);

                sceneFactory.createScene(pendingSceneName);

                // 🛡️ GUARD: Si createScene falló, volver al panel sin crash
                if (!sceneFactory.hasCurrentScene()) {
                    Log.e(TAG, "❌ Scene creation failed (autoload): " + pendingSceneName);
                    if (modeController != null) modeController.stopWallpaper();
                    if (panelRenderer != null) {
                        panelRenderer.onReturnToPanel();
                        panelRenderer.resume();
                    }
                    return;
                }

                startMemoryMonitor();

                // 🔧 FIX VIDEO PAUSADO: Resumir la escena INMEDIATAMENTE después de crearla
                // Antes, resume() se llamaba ANTES de que la escena existiera (en startRendering),
                // así que scene.onResume() nunca se ejecutaba → video pausado, texturas sin cargar
                sceneFactory.resumeCurrentScene();

                // Asegurar que estamos en WALLPAPER_MODE
                modeController.activateWallpaper();

                if (panelRenderer != null) {
                    panelRenderer.onWallpaperActivated();
                    panelRenderer.setGreetingEnabled(true);
                }

                // 🎵 Reconectar MusicVisualizer si no está activo
                // 🔋 NO despertar si está en sleep mode (sin audio)
                if (musicVisualizer != null && !musicVisualizer.isSleeping()) {
                    if (!musicVisualizer.isEnabled() || !musicVisualizer.isReceivingAudio()) {
                        Log.d(TAG, "🎵 [GL Thread] Reanudando MusicVisualizer post-autoload");
                        musicVisualizer.resume();
                    }
                }

                // Aplicar tema del Like button según la escena
                applySceneModeToPanel(pendingSceneName);

                Log.d(TAG, "✅ [GL Thread] Escena cargada y resumida: " + pendingSceneName);
            }
        }

        // Actualizar tiempo total para animaciones
        updateTotalTime(deltaTime);

        // 🔋 Adaptive FPS: detectar idle vs activo
        {
            long now = System.currentTimeMillis();
            boolean musicActive = musicVisualizer != null && musicVisualizer.isReceivingAudio();
            boolean recentActivity = (now - lastActivityTime) < IDLE_TIMEOUT_MS;
            boolean loading = modeController.isLoadingMode();
            boolean transitioning = panelRenderer != null && panelRenderer.isTransitioning();

            boolean sceneRunning = modeController.isWallpaperMode()
                    && sceneFactory != null && sceneFactory.hasCurrentScene();
            boolean shouldBeActive = musicActive || recentActivity || loading || transitioning || sceneRunning;

            if (shouldBeActive && isIdle) {
                isIdle = false;
                GLStateManager.get().setTargetFPS(GLStateManager.getFpsActive());
            } else if (!shouldBeActive && !isIdle) {
                isIdle = true;
                GLStateManager.get().setTargetFPS(GLStateManager.getFpsIdle());
            }
        }

        // 🛡️ ROBUST ERROR HANDLING: Envolver todo el render en try-catch
        try {
            RenderModeController.RenderMode mode = modeController.getCurrentMode();
            switch (mode) {
                case PANEL_MODE:
                    panelRenderer.updatePanelMode(deltaTime);
                    panelRenderer.drawPanelMode();
                    // Auto-start wallpaper after 3s in panel
                    if (!panelAutoStartFired && pendingSceneName != null && !pendingSceneName.isEmpty()) {
                        panelAutoStartTimer += deltaTime;
                        if (panelAutoStartTimer >= PANEL_AUTO_START_DELAY) {
                            panelAutoStartFired = true;
                            Log.d(TAG, "⏱️ Auto-start: iniciando wallpaper tras 3s");
                            startLoading();
                        }
                    }
                    break;
                case LOADING_MODE:
                    panelRenderer.updateLoadingMode(deltaTime);
                    // If pixelation transition is active, draw panel (shows pixelation)
                    if (panelRenderer.isTransitioning()) {
                        panelRenderer.drawPanelMode();
                    } else {
                        panelRenderer.drawLoadingMode();
                        checkLoadingComplete();
                    }
                    break;
                case WALLPAPER_MODE:
                    renderWallpaperModeSafe(deltaTime);
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

            // Reset error counter on successful frame
            consecutiveErrors = 0;

        } catch (Exception e) {
            handleRenderError(e);
        }
    }

    /**
     * 🛡️ Render wallpaper mode con manejo de errores separado
     * Si falla, intenta recuperar o volver al panel
     */
    private void renderWallpaperModeSafe(float deltaTime) {
        try {
            updateWallpaperMode(deltaTime);
            drawWallpaperMode();
        } catch (Exception e) {
            Log.e(TAG, "❌ Error en WALLPAPER_MODE: " + e.getMessage(), e);
            // Intentar recuperar la escena
            try {
                if (sceneFactory != null && sceneFactory.hasCurrentScene()) {
                    Log.d(TAG, "🔧 Intentando recuperar escena...");
                    sceneFactory.resumeCurrentScene();
                }
            } catch (Exception recovery) {
                Log.e(TAG, "❌ Recuperación fallida, volviendo al panel: " + recovery.getMessage());
                // Si la recuperación falla, volver al panel de forma segura
                forceReturnToPanel();
            }
            throw e; // Re-throw para que handleRenderError lo procese
        }
    }

    /**
     * 🛡️ Maneja errores de renderizado con auto-recovery
     */
    private void handleRenderError(Exception e) {
        long now = System.currentTimeMillis();

        // Reset contador si pasó más de 5 segundos desde el último error
        if (now - lastErrorTime > 5000) {
            consecutiveErrors = 0;
        }

        consecutiveErrors++;
        lastErrorTime = now;

        Log.e(TAG, "❌ Error en onDrawFrame (#" + consecutiveErrors + "): " + e.getMessage(), e);

        // Si hay muchos errores consecutivos, forzar retorno al panel
        if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
            Log.e(TAG, "🚨 Demasiados errores consecutivos (" + consecutiveErrors + "), forzando retorno al panel");
            forceReturnToPanel();
            consecutiveErrors = 0;
        }

        // Intentar limpiar estado GL para evitar cascada de errores
        try {
            GLES30.glGetError(); // Clear any pending GL errors
        } catch (Exception ignored) {}
    }

    /**
     * 🛡️ Fuerza retorno al panel de forma segura (sin excepciones)
     */
    private void forceReturnToPanel() {
        try {
            Log.d(TAG, "Forzando retorno seguro al panel...");
            SceneHealthMonitor.get().stop();

            // Destruir escena actual
            if (sceneFactory != null) {
                try {
                    sceneFactory.destroyCurrentScene();
                } catch (Exception ignored) {}
            }

            // Volver al panel mode
            if (modeController != null) {
                modeController.stopWallpaper();
            }

            // Notificar al panel
            if (panelRenderer != null) {
                panelRenderer.onReturnToPanel();
            }

            Log.d(TAG, "✅ Retorno al panel completado");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error crítico en forceReturnToPanel: " + e.getMessage());
        }
    }

    private void updateWallpaperMode(float deltaTime) {
        if (musicVisualizer != null) {
            // 🔧 AUTO-RECOVERY: Si estamos renderizando pero el visualizer está pausado,
            // reanudarlo automáticamente (fix para callbacks desordenados de Android)
            // 🔋 NO despertar si está en sleep mode (ahorrando batería intencionalmente)
            if (!musicVisualizer.isEnabled() && !musicVisualizer.isSleeping()) {
                Log.d(TAG, "🔧 Auto-recovery: MusicVisualizer pausado durante render, reanudando...");
                musicVisualizer.resume();
            }

            WallpaperScene scene = sceneFactory.getCurrentScene();
            float[] bands = musicVisualizer.getFrequencyBands();

            // ⚡ OPTIMIZADO: Dispatch polimórfico via WallpaperScene.updateMusicBands()
            if (scene != null) {
                scene.updateMusicBands(bands);
            }
        }
        sceneFactory.updateCurrentScene(deltaTime);
        if (screenEffects != null) screenEffects.update(deltaTime);
        if (touchSparkles != null) touchSparkles.update(deltaTime);
        panelRenderer.updateWallpaperMode(deltaTime);
        songSharing.update(deltaTime);
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔄 BRANDED FALLBACK — "Orbix iA" when no scene can render
    // Self-contained: own shader + own texture, no external files
    // ═══════════════════════════════════════════════════════════════
    private int fallbackShader = 0;
    private int fallbackTexture = 0;
    private int fallbackTimeLoc = -1;
    private FloatBuffer fallbackVB;
    private float fallbackTime = 0f;

    private void drawFallbackScreen() {
        if (fallbackShader == 0) {
            String vs = "#version 300 es\n"
                + "in vec2 p; in vec2 t; out vec2 uv;\n"
                + "void main() { uv = t; gl_Position = vec4(p, 0, 1); }";

            String fs = "#version 300 es\n"
                + "precision mediump float;\n"
                + "in vec2 uv;\n"
                + "uniform sampler2D s;\n"
                + "uniform float u_time;\n"
                + "out vec4 fc;\n"
                + "void main() {\n"
                // Elegant purple/blue/pink aurora
                + "  float w1 = sin(uv.y * 3.5 + u_time * 0.35) * 0.5 + 0.5;\n"
                + "  float w2 = cos(uv.x * 2.5 + u_time * 0.25 + uv.y * 1.8) * 0.5 + 0.5;\n"
                + "  float w3 = sin((uv.x + uv.y) * 2.0 - u_time * 0.2) * 0.5 + 0.5;\n"
                + "  float w4 = sin(uv.y * 5.0 - u_time * 0.15 + uv.x * 1.5) * 0.5 + 0.5;\n"
                + "  vec3 bg = vec3(0.04, 0.02, 0.08);\n"           // dark purple base
                + "  bg += vec3(0.03, 0.01, 0.06) * w1;\n"          // deep purple wave
                + "  bg += vec3(0.01, 0.02, 0.05) * w2;\n"          // blue wave
                + "  bg += vec3(0.04, 0.01, 0.04) * w3;\n"          // pink/magenta wave
                + "  bg += vec3(0.02, 0.01, 0.03) * w4;\n"          // violet accent
                // Soft vignette
                + "  float vig = 1.0 - length((uv - 0.5) * 1.3);\n"
                + "  vig = smoothstep(0.0, 1.0, vig);\n"
                + "  bg *= (0.5 + 0.5 * vig);\n"
                // Text with gentle breathing glow
                + "  vec4 tex = texture(s, uv);\n"
                + "  float breathe = 0.6 + 0.4 * sin(u_time * 1.0);\n"
                + "  vec3 color = bg + tex.rgb * tex.a * breathe;\n"
                + "  fc = vec4(color, 1.0);\n"
                + "}";

            int vId = GLES30.glCreateShader(GLES30.GL_VERTEX_SHADER);
            GLES30.glShaderSource(vId, vs); GLES30.glCompileShader(vId);
            int fId = GLES30.glCreateShader(GLES30.GL_FRAGMENT_SHADER);
            GLES30.glShaderSource(fId, fs); GLES30.glCompileShader(fId);
            fallbackShader = GLES30.glCreateProgram();
            GLES30.glAttachShader(fallbackShader, vId);
            GLES30.glAttachShader(fallbackShader, fId);
            GLES30.glLinkProgram(fallbackShader);
            GLES30.glDeleteShader(vId); GLES30.glDeleteShader(fId);
            fallbackTimeLoc = GLES30.glGetUniformLocation(fallbackShader, "u_time");

            float[] v = {-1,-1,0,1, 1,-1,1,1, -1,1,0,0, 1,1,1,0};
            fallbackVB = ByteBuffer.allocateDirect(v.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            fallbackVB.put(v).position(0);
        }
        if (fallbackTexture == 0) {
            int w = 512, h = 512;
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            canvas.drawColor(0); // transparent — shader handles background
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setTextAlign(Paint.Align.CENTER);

            // "Orbix" — soft lavender/periwinkle
            paint.setColor(Color.parseColor("#B8A9E8"));
            paint.setTextSize(78f);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            paint.setShadowLayer(16f, 0f, 0f, Color.parseColor("#6B3FA0"));
            canvas.drawText("Orbix", 256f, 220f, paint);
            paint.clearShadowLayer();

            // "iA" — soft rose/pink
            paint.setTextSize(44f);
            paint.setColor(Color.parseColor("#D4A0C0"));
            paint.setShadowLayer(8f, 0f, 0f, Color.parseColor("#8B4070"));
            canvas.drawText("iA", 256f, 275f, paint);
            paint.clearShadowLayer();

            // "Waiting for connection..." — muted blue-gray
            paint.setTextSize(17f);
            paint.setColor(Color.parseColor("#5A5080"));
            canvas.drawText("Waiting for connection...", 256f, 340f, paint);

            int[] tex = new int[1];
            GLES30.glGenTextures(1, tex, 0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex[0]);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bmp, 0);
            bmp.recycle();
            fallbackTexture = tex[0];
            Log.d(TAG, "🔄 Fallback elegant texture created");
        }

        // Advance time for animation
        fallbackTime += 0.016f; // ~60fps

        GLES30.glClearColor(0.03f, 0.03f, 0.07f, 1f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        GLES30.glUseProgram(fallbackShader);
        if (fallbackTimeLoc >= 0) GLES30.glUniform1f(fallbackTimeLoc, fallbackTime);

        int pLoc = GLES30.glGetAttribLocation(fallbackShader, "p");
        int tLoc = GLES30.glGetAttribLocation(fallbackShader, "t");
        int sLoc = GLES30.glGetUniformLocation(fallbackShader, "s");

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, fallbackTexture);
        GLES30.glUniform1i(sLoc, 0);
        GLES30.glEnableVertexAttribArray(pLoc);
        GLES30.glEnableVertexAttribArray(tLoc);
        fallbackVB.position(0);
        GLES30.glVertexAttribPointer(pLoc, 2, GLES30.GL_FLOAT, false, 16, fallbackVB);
        fallbackVB.position(2);
        GLES30.glVertexAttribPointer(tLoc, 2, GLES30.GL_FLOAT, false, 16, fallbackVB);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glDisableVertexAttribArray(pLoc);
        GLES30.glDisableVertexAttribArray(tLoc);
    }

    private void drawWallpaperMode() {
        // 🔄 If scene has no renderable content (missing textures/video), show branded fallback
        if (!sceneFactory.hasRenderableContent()) {
            drawFallbackScreen();
            return;
        }

        // ✨ Bloom: DESHABILITADO TEMPORALMENTE para debug
        // if (bloomEffect != null && bloomEffect.isEnabled()) {
        //     bloomEffect.beginCapture();
        // }

        // Dibujar escena 3D
        sceneFactory.drawCurrentScene();
        if (screenEffects != null) screenEffects.draw();
        if (touchSparkles != null) touchSparkles.draw();

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
        if (panelRenderer.isLoadingComplete()) {
            onLoadingComplete();
            return;
        }

        // 🔧 FIX LOADING FALLBACK: Si los recursos están listos pero la animación
        // del LoadingBar no ha terminado (por threading visibility o timing),
        // forzar la activación después de un timeout.
        if (resourcesReady) {
            resourcesReadyTimer += GLStateManager.get().getDeltaTime();
            if (resourcesReadyTimer >= LOADING_FALLBACK_TIMEOUT) {
                Log.w(TAG, "⚠️ Loading fallback: recursos listos pero LoadingBar no completó en "
                        + LOADING_FALLBACK_TIMEOUT + "s, forzando activación");
                onLoadingComplete();
            }
        }
    }

    private void onLoadingComplete() {
        if (modeController == null || modeController.isWallpaperMode()) return;

        // Reset fallback state
        resourcesReady = false;
        resourcesReadyTimer = 0f;

        // 🧠 OOM risk warning: LOW RAM + HEAVY scene + low available memory
        DeviceProfile dp = DeviceProfile.get();
        if (dp.isLowRam()) {
            WallpaperItem item = WallpaperCatalog.get().getBySceneName(pendingSceneName);
            if (item != null && item.getSceneWeight() == SceneWeight.HEAVY) {
                long availMB = dp.getAvailableRamMB();
                if (availMB >= 0 && availMB < 200) {
                    Log.w(TAG, "OOM RISK: LOW RAM + HEAVY scene '" + pendingSceneName
                            + "' + only " + availMB + "MB available");
                }
            }
        }

        Log.d(TAG, "Carga completada - activando wallpaper");
        sceneFactory.createScene(pendingSceneName);

        // 🛡️ GUARD: Si createScene falló, volver al panel sin crash
        if (!sceneFactory.hasCurrentScene()) {
            Log.e(TAG, "❌ Scene creation failed (loading): " + pendingSceneName);
            if (modeController != null) modeController.stopWallpaper();
            if (panelRenderer != null) {
                panelRenderer.onReturnToPanel();
                panelRenderer.resume();
            }
            return;
        }

        startMemoryMonitor();
        modeController.activateWallpaper();
        panelRenderer.onWallpaperActivated();

        // 🔔 Notificar al usuario que el wallpaper fue instalado
        try {
            WallpaperItem item = WallpaperCatalog.get().getBySceneName(pendingSceneName);
            String name = item != null ? item.getNombre() : pendingSceneName;
            WallpaperNotificationManager.getInstance(context)
                    .notifyWallpaperInstalled(pendingSceneName, name);
        } catch (Exception e) {
            Log.w(TAG, "Error enviando notificación de instalación: " + e.getMessage());
        }

        // Habilitar saludos Gemini para todos los wallpapers
        panelRenderer.setGreetingEnabled(true);

        // 🎵 Solo reconectar MusicVisualizer si NO está funcionando
        // ⚠️ Reconectar puede pausar Spotify, así que evitarlo si ya funciona
        // 🔋 NO despertar si está en sleep mode (sin audio, ahorrando batería)
        if (musicVisualizer != null && !musicVisualizer.isSleeping()) {
            if (musicVisualizer.isEnabled() && musicVisualizer.isReceivingAudio()) {
                Log.d(TAG, "🎵 MusicVisualizer ya funcionando, sin reconectar");
            } else {
                Log.d(TAG, "🎵 MusicVisualizer no activo, reanudando...");
                musicVisualizer.resume();  // Usar resume() en vez de reconnect()
            }
        }
    }

    public void startLoading() {
        if (modeController == null) {
            Log.w(TAG, "startLoading() called before GL init, ignoring");
            return;
        }
        if (!modeController.startLoading()) {
            return;
        }
        markActivity();

        // Reset fallback state para nueva carga
        resourcesReady = false;
        resourcesReadyTimer = 0f;

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
                // 🔧 FIX LOADING: Marcar recursos como listos para el fallback timer
                resourcesReady = true;
                resourcesReadyTimer = 0f;

                // Notificación movida a WallpaperPreviewActivity
            }

            @Override
            public void onPreloadError(String error) {
                Log.e(TAG, "❌ Error en ResourcePreloader: " + error);
                // 🛡️ FIX BLACK SCREEN: Verificar si los recursos existen a pesar del error
                if (ResourcePreloader.areSceneResourcesReady(context, pendingSceneName)) {
                    // Recursos disponibles (descargados previamente) - continuar
                    Log.d(TAG, "🛡️ Recursos disponibles a pesar de error - continuando");
                    panelRenderer.updateLoadingProgress(1.0f, "Iniciando...");
                    resourcesReady = true;
                    resourcesReadyTimer = 0f;
                } else {
                    // 🛡️ Recursos NO disponibles - volver al panel (no cargar escena rota)
                    Log.e(TAG, "🛡️ Recursos NO disponibles - abortando carga");
                    panelRenderer.updateLoadingProgress(0f, "Error: " + error);
                    if (modeController != null) {
                        modeController.stopWallpaper();
                    }
                    if (panelRenderer != null) {
                        panelRenderer.onReturnToPanel();
                        panelRenderer.resume();
                    }
                }
            }
        });

        // Preparar y ejecutar tareas de la escena
        resourcePreloader.prepareTasksForScene(pendingSceneName);
        resourcePreloader.startPreloading();

        Log.d(TAG, "📦 ResourcePreloader iniciado para: " + pendingSceneName);
    }

    public void switchToPanelMode() {
        if (modeController == null) return;
        // Reset auto-start para que funcione de nuevo al volver al panel
        panelAutoStartTimer = 0f;
        panelAutoStartFired = false;
        SceneHealthMonitor.get().stop();
        if (modeController.stopWallpaper()) {
            // 🔧 FIX RACE CONDITION: Write both flags atomically so GL thread
            // always sees them together (avoids destroy without return → black screen)
            synchronized (pendingFlagsLock) {
                pendingSceneDestroy = true;
                pendingReturnToPanel = true;
            }
            Log.d(TAG, "switchToPanelMode: destruccion programada en GL thread");
        }
    }

    private void initializeSharedSystems() {
        Log.d(TAG, "Inicializando sistemas compartidos...");

        // 🧠 DeviceProfile: singleton central de deteccion de RAM (debe ser primero)
        DeviceProfile.init(context);

        camera = new CameraController();
        camera.setMode(CameraController.CameraMode.PERSPECTIVE_3_4);
        resources = ResourceManager.get();
        resources.init(context);
        textureManager = new TextureManager(context);
        textureManager.initialize();
        // 🧠 Propagate memory tier to ResourceManager
        resources.setInSampleSize(textureManager.getDefaultInSampleSize());
        Log.d(TAG, "🧠 Memory tier: " + textureManager.getMemoryTier()
                + ", maxDim=" + textureManager.getMaxTextureDimension());
        // 🎵 Usar constructor con Context para habilitar auto-resume de música
        musicVisualizer = new MusicVisualizer(context);
        musicVisualizer.initialize();
        screenEffects = new ScreenEffectsManager();
        touchSparkles = new TouchSparkleEffect();
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
            @Override public void onBackButtonTapped() { switchToPanelMode(); }
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
            } else if (sceneName.equals("SPIDER") || sceneName.toLowerCase().contains("spider") || sceneName.toLowerCase().contains("araña")) {
                songSharing.setLikeButtonTheme(LikeButton.Theme.SPIDER);
            } else {
                songSharing.setLikeButtonTheme(LikeButton.Theme.DEFAULT);
            }
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (!initialized) return false;
        markActivity();
        touchRouter.setCurrentScene(sceneFactory.getCurrentScene());
        boolean consumed = touchRouter.onTouchEvent(event);

        // Sparkles on touch if scene didn't consume the event (static wallpapers)
        if (!consumed && modeController != null && modeController.isWallpaperMode() && touchSparkles != null) {
            float nx = (event.getX() / screenWidth) * 2.0f - 1.0f;
            float ny = -((event.getY() / screenHeight) * 2.0f - 1.0f);
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                touchSparkles.spawn(nx, ny, 5);
            } else if (action == MotionEvent.ACTION_MOVE) {
                touchSparkles.spawn(nx, ny, 2);
            }
        }

        return consumed;
    }

    // NOTA: deltaTime y FPS ahora son manejados por GLStateManager
    // Solo mantenemos totalTime para animaciones que lo necesiten
    private void updateTotalTime(float deltaTime) {
        totalTime += deltaTime;
        if (totalTime > TIME_WRAP) totalTime -= TIME_WRAP;  // Evitar overflow
    }

    /**
     * 🔋 Marca actividad reciente para mantener FPS alto (30)
     * Llamar desde touch, loading, scene change, resume, etc.
     */
    private void markActivity() {
        lastActivityTime = System.currentTimeMillis();
        if (isIdle) {
            isIdle = false;
            GLStateManager.get().setTargetFPS(GLStateManager.getFpsActive());
        }
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
        markActivity();

        // 🔧 FIX FREEZE: Si hay una destrucción de escena pendiente, NO resumir la escena actual.
        // La escena actual está a punto de ser destruida y reemplazada por una nueva.
        // Resumirla causaría que intente abrir recursos que ya fueron limpiados.
        if (pendingSceneDestroy || pendingSceneAutoLoad) {
            Log.d(TAG, "▶️ Resume SKIP: Destrucción/auto-load pendiente, la nueva escena se resumirá en onDrawFrame");
            // Solo asegurar que el panel esté pausado
            if (panelRenderer != null) panelRenderer.pause();
            return;
        }

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

        // 🎵 Reanudar MusicVisualizer con smart resume
        // 🔋 Si no hay música, entra directo a sleep mode para no consumir batería
        if (musicVisualizer != null) {
            musicVisualizer.smartResume();
            Log.d(TAG, "🎵 MusicVisualizer smart resume: sleeping=" + musicVisualizer.isSleeping());
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
        markActivity();

        // Si es la misma escena, no hacer nada
        if (sceneName != null && sceneName.equals(pendingSceneName)) {
            Log.d(TAG, "📱 Escena ya seleccionada: " + sceneName);
            return;
        }

        String previousScene = pendingSceneName;
        pendingSceneName = sceneName;

        // Reset auto-start timer para la nueva escena
        panelAutoStartTimer = 0f;
        panelAutoStartFired = false;

        // 🎮 Si NO estamos en PANEL_MODE, volver al panel para que el usuario presione PLAY
        // Esto maneja tanto WALLPAPER_MODE (escena activa) como LOADING_MODE (carga en curso)
        if (modeController != null && !modeController.isPanelMode()) {
            Log.d(TAG, "🎮 Modo activo detectado (" + modeController.getCurrentMode()
                    + ", escena: " + previousScene + "), volviendo al panel para: " + sceneName);
            // 🔧 FIX RACE CONDITION: Write all flags atomically
            synchronized (pendingFlagsLock) {
                pendingSceneDestroy = true;    // Destruir escena si existe
                pendingReturnToPanel = true;   // Volver al panel de control
                pendingSceneAutoLoad = false;  // NO auto-cargar - el usuario presiona PLAY
            }
            modeController.stopWallpaper(); // → PANEL_MODE
            Log.d(TAG, "✅ Panel de control activado - presiona PLAY para iniciar: " + sceneName);
        } else if (panelRenderer != null) {
            Log.d(TAG, "📱 Panel listo para: " + sceneName);
        } else {
            Log.d(TAG, "Panel mode pendiente - panelRenderer aún no inicializado");
        }
    }

    /**
     * 🧠 onTrimMemory - Responde a presión de memoria del sistema
     * Libera recursos no esenciales para evitar OOM kill.
     *
     * @param level Nivel de presión de memoria (ComponentCallbacks2.TRIM_MEMORY_*)
     */
    public void onTrimMemory(int level) {
        Log.w(TAG, "⚠️ onTrimMemory level=" + level);

        // 🔧 FIX: Respuesta progresiva a presión de memoria
        // Level 5 (RUNNING_MODERATE): Liberar ecualizador
        if (level >= 5) {
            if (musicVisualizer != null && !paused) {
                musicVisualizer.release();
                Log.d(TAG, "🎵 MusicVisualizer liberado (level=" + level + ")");
            }
        }

        // Level 10 (RUNNING_LOW): Liberar texturas no usadas
        if (level >= 10) {
            if (resources != null) {
                resources.releaseSceneResources();
                Log.d(TAG, "📦 ResourceManager scene resources released (level=" + level + ")");
            }
            // 🧠 Schedule file texture release on GL thread
            pendingFileTextureRelease = true;
            Log.d(TAG, "🧠 File texture release scheduled (level=" + level + ")");
        }

        // Level 15 (RUNNING_CRITICAL): Forzar GC y log de emergencia
        if (level >= 15) {
            Log.e(TAG, "🚨 CRITICAL memory pressure (level=" + level + ")");
            System.gc();
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
        if (touchSparkles != null) touchSparkles.release();
        if (bloomEffect != null) bloomEffect.release();

        // Flush final y liberar FirebaseQueueManager
        if (firebaseQueue != null) {
            firebaseQueue.forceFlush();
            firebaseQueue.release();
            firebaseQueue = null;
        }

        SceneHealthMonitor.reset();
        DeviceProfile.reset();
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

        Log.d(TAG, "Suscrito a eventos de efectos de pantalla");
    }

    /**
     * Inicia el monitor de memoria adaptativo para la escena activa.
     * El listener propaga cambios de nivel a la escena actual.
     */
    private void startMemoryMonitor() {
        SceneHealthMonitor.get().start(level -> {
            WallpaperScene scene = sceneFactory != null ? sceneFactory.getCurrentScene() : null;
            if (scene != null) {
                scene.onMemoryPressure(level);
            }
        });
    }
}
