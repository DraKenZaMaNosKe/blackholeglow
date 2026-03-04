// LiveWallpaperService.java - VERSIÓN ULTRA-ROBUSTA ANTI-FLICKERING
package com.secret.blackholeglow;

import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.MotionEvent;
import android.content.Context;
import android.opengl.GLSurfaceView;


import com.secret.blackholeglow.core.PreFlightCheck;
import com.secret.blackholeglow.core.ResourcePreloader;
import com.secret.blackholeglow.core.WallpaperDirector;
import com.secret.blackholeglow.image.ImageDownloadManager;
import com.secret.blackholeglow.model.ModelDownloadManager;
import com.secret.blackholeglow.models.WallpaperItem;
import com.secret.blackholeglow.systems.WallpaperCatalog;
import com.secret.blackholeglow.video.VideoDownloadManager;

import java.util.Collections;
import java.util.List;

// 🔧 FIX ANR: Sistemas de monetización REMOVIDOS del WallpaperService
// Se inicializan en MainActivity donde realmente se necesitan

/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║   🚀 LiveWallpaperService - ANTI-FLICKERING EDITION              ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * SOLUCIÓN AL FLICKERING:
 * - Estado atómico con synchronized
 * - Transiciones de estado validadas
 * - No se procesa ningún cambio durante transiciones
 * - Respuesta instantánea a home/recent apps
 */
public class LiveWallpaperService extends WallpaperService {
    private static final String TAG = "LiveWallpaperService";

    // Referencia al engine activo para delegar onTrimMemory
    private GLWallpaperEngine activeEngine;

    // ═══════════════════════════════════════════════════════════════
    // 🔒 ESTADO ATÓMICO - Previene condiciones de carrera
    // ═══════════════════════════════════════════════════════════════
    private enum RenderState {
        UNINITIALIZED,  // No hay GL context
        STOPPED,        // GL existe pero no renderiza
        RUNNING         // Renderizando activamente
    }

    @Override
    public Engine onCreateEngine() {
        Log.d(TAG, "onCreateEngine llamado");
        activeEngine = new GLWallpaperEngine(this);
        return activeEngine;
    }

    /**
     * 🧠 onTrimMemory - Android nos pide liberar memoria
     *
     * Niveles relevantes:
     * - TRIM_MEMORY_RUNNING_MODERATE (5): Sistema bajo de memoria
     * - TRIM_MEMORY_RUNNING_CRITICAL (15): Siguiente paso es OOM kill
     * - TRIM_MEMORY_UI_HIDDEN (20): UI oculta
     * - TRIM_MEMORY_MODERATE (60): Proceso en background
     * - TRIM_MEMORY_COMPLETE (80): Proceso será killeado pronto
     */
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        // 🔧 FIX: Was TRIM_MEMORY_MODERATE (60), which blocked WallpaperDirector's
        // level 5/10/15 handlers from ever executing. Changed to RUNNING_MODERATE (5)
        // so progressive memory release works as designed.
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE) {
            Log.w(TAG, "⚠️ onTrimMemory level=" + level + " - delegando a Engine");
            if (activeEngine != null) {
                activeEngine.handleTrimMemory(level);
            }
        }
    }

    private class GLWallpaperEngine extends Engine {
        private final WallpaperPreferences wallpaperPrefs;
        private final Context context;
        private GLWallpaperSurfaceView glSurfaceView;
        private WallpaperDirector wallpaperDirector;  // Sistema modular de renderizado
        private ChargingScreenManager chargingScreenManager;

        private final Object stateLock = new Object();
        private RenderState currentState = RenderState.UNINITIALIZED;
        private boolean surfaceExists = false;
        private boolean isSystemPreviewMode = false;  // 🎬 Para mantener el wallpaper visible en preview del sistema

        // 🔧 FIX: Debounce para onVisibilityChanged (evita stuttering en Samsung)
        private long lastVisibilityChangeTime = 0;
        private static final long VISIBILITY_DEBOUNCE_MS = 300;

        // 🎬 Auto-play: arranca wallpaper automáticamente al volver al home
        private final Handler autoPlayHandler = new Handler(Looper.getMainLooper());
        private static final long AUTO_PLAY_DELAY_MS = 500;

        // 🔄 Auto-rotate: cambia wallpaper cada 5 minutos (runs even in background)
        private final Handler autoRotateHandler = new Handler(Looper.getMainLooper());
        private static final long AUTO_ROTATE_INTERVAL_MS = 5 * 60 * 1000; // 5 minutes
        private boolean autoRotateScheduled = false;
        private long autoRotateNextChangeTime = 0; // epoch ms when next rotation fires
        private long autoRotateScheduledAt = 0; // epoch ms when rotation was scheduled (freeze detection)

        // 🔄 Pre-download: after each rotation, download the NEXT wallpaper in background
        private volatile String preDownloadedScene = null;
        private volatile boolean preDownloadInProgress = false;

        // 🔔 Preference listener: react immediately when auto-rotate toggle changes
        private SharedPreferences.OnSharedPreferenceChangeListener prefListener;
        private final Runnable autoPlayRunnable = () -> {
            if (wallpaperDirector != null) {
                Log.d(TAG, "▶️ Auto-play: iniciando wallpaper automáticamente");
                wallpaperDirector.startLoading();
            }
        };

        // 🔄 Fallback wallpaper: always available, downloaded at engine start, never cleaned up
        private static final String FALLBACK_SCENE = "GATITO_IMG";

        // 🔄 Auto-rotate runnable
        // Strategy: pick random → download if needed → switch ONLY if ready → cleanup old → fallback if all fails
        private final Runnable autoRotateRunnable = new Runnable() {
            @Override
            public void run() {
                if (!wallpaperPrefs.isAutoRotateEnabled()) {
                    Log.d(TAG, "🔄 Auto-rotate: disabled, stopping");
                    autoRotateScheduled = false;
                    return;
                }

                // 📊 Samsung freeze detection: if timer fired >30s late, it was frozen
                if (autoRotateScheduledAt > 0) {
                    long expectedDelay = AUTO_ROTATE_INTERVAL_MS;
                    long actualDelay = System.currentTimeMillis() - autoRotateScheduledAt;
                    long drift = actualDelay - expectedDelay;
                    if (drift > 30_000) {
                        Log.w(TAG, "🔄 Auto-rotate: timer was FROZEN for ~" + (drift / 1000) + "s (Samsung FreecessHandler?)");
                    }
                }

                Log.d(TAG, "🔄 Auto-rotate: timer FIRED — finding next wallpaper...");

                new Thread(() -> {
                    try {
                        String nextScene = findAndPrepareNextScene();

                        // Switch on main thread ONLY if we have a confirmed-ready scene
                        autoRotateHandler.post(() -> {
                            try {
                                if (nextScene != null) {
                                    String previousScene = wallpaperPrefs.getSelectedWallpaperSync();
                                    Log.d(TAG, "🔄 Auto-rotate: switching " + previousScene + " → " + nextScene);

                                    wallpaperPrefs.setSelectedWallpaper(nextScene);
                                    if (wallpaperDirector != null) {
                                        wallpaperDirector.changeScene(nextScene);
                                        wallpaperDirector.startLoading();
                                    }

                                    // Cleanup: keep max 2 wallpapers (current + previous) + fallback + panel
                                    cleanupWithHistory(nextScene, previousScene);

                                    // 🚀 Pre-download the NEXT wallpaper for instant switch
                                    preDownloadNextScene();
                                } else {
                                    Log.w(TAG, "🔄 Auto-rotate: nothing ready, keeping current wallpaper");
                                }
                            } finally {
                                scheduleNextRotation();
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "🔄 Auto-rotate CRASH: " + e.getMessage());
                        autoRotateHandler.post(() -> scheduleNextRotation());
                    }
                }, "AutoRotate").start();
            }
        };

        /**
         * Ensures the fallback wallpaper resources are downloaded.
         * Called once at engine startup. Skips if already available.
         */
        private void ensureFallbackDownloaded() {
            new Thread(() -> {
                try {
                    PreFlightCheck.InstallCheckResult check =
                            PreFlightCheck.runInstallCheck(context, FALLBACK_SCENE);
                    if (check.allResourcesReady) {
                        Log.d(TAG, "🔄 Fallback " + FALLBACK_SCENE + " already downloaded");
                        return;
                    }
                    Log.d(TAG, "🔄 Downloading fallback " + FALLBACK_SCENE + "...");
                    boolean ok = downloadResources(check.getAllMissing());
                    Log.d(TAG, "🔄 Fallback download: " + (ok ? "OK" : "FAILED"));
                } catch (Exception e) {
                    Log.w(TAG, "🔄 Fallback download error: " + e.getMessage());
                }
            }, "FallbackDownload").start();
        }

        /**
         * Picks a random wallpaper, downloads its resources if needed.
         * Returns sceneName ONLY if resources are confirmed ready.
         * Falls back to FALLBACK_SCENE if everything else fails.
         * Returns null only if even fallback is unavailable.
         */
        private String findAndPrepareNextScene() {
            // 🚀 Check pre-downloaded scene first (instant switch)
            String preDownloaded = preDownloadedScene;
            if (preDownloaded != null) {
                String currentScene = wallpaperPrefs.getSelectedWallpaperSync();
                if (!preDownloaded.equals(currentScene)) {
                    try {
                        PreFlightCheck.InstallCheckResult check =
                                PreFlightCheck.runInstallCheck(context, preDownloaded);
                        if (check.allResourcesReady) {
                            Log.d(TAG, "🔄 Pre-downloaded " + preDownloaded + " ✓ INSTANT switch");
                            preDownloadedScene = null;
                            return preDownloaded;
                        }
                        Log.w(TAG, "🔄 Pre-downloaded " + preDownloaded + " resources gone (cleanup?), discarding");
                    } catch (Exception e) {
                        Log.w(TAG, "🔄 Pre-download verify error: " + e.getMessage());
                    }
                }
                preDownloadedScene = null; // discard stale or same-as-current
            }

            List<WallpaperItem> candidates = WallpaperCatalog.get().getAutoRotateCandidates();
            if (candidates.isEmpty()) return null;

            String currentScene = wallpaperPrefs.getSelectedWallpaperSync();
            Log.d(TAG, "🔄 Auto-rotate: current=" + currentScene + " candidates=" + candidates.size());

            Collections.shuffle(candidates);

            for (WallpaperItem candidate : candidates) {
                String sceneName = candidate.getSceneName();
                if (sceneName.equals(currentScene)) continue;

                try {
                    PreFlightCheck.InstallCheckResult check =
                            PreFlightCheck.runInstallCheck(context, sceneName);

                    if (check.allResourcesReady) {
                        Log.d(TAG, "🔄 " + sceneName + " ✓ ready (local)");
                        return sceneName;
                    }

                    // Only attempt download if internet is available
                    if (isInternetAvailable()) {
                        Log.d(TAG, "🔄 " + sceneName + " — downloading...");
                        boolean ok = downloadResources(check.getAllMissing());
                        if (ok) {
                            // Double-check: files actually exist and are complete
                            PreFlightCheck.InstallCheckResult recheck =
                                    PreFlightCheck.runInstallCheck(context, sceneName);
                            if (recheck.allResourcesReady) {
                                Log.d(TAG, "🔄 " + sceneName + " ✓ downloaded + verified");
                                return sceneName;
                            }
                            Log.w(TAG, "🔄 " + sceneName + " ✗ verify FAILED (corrupt/partial)");
                        }
                    } else {
                        Log.d(TAG, "🔄 " + sceneName + " ✗ no internet, skipping download");
                    }
                    Log.w(TAG, "🔄 " + sceneName + " ✗ download failed, skipping");
                } catch (Exception e) {
                    Log.w(TAG, "🔄 " + sceneName + " ✗ error: " + e.getMessage());
                }
            }

            // All candidates failed — try fallback
            if (!FALLBACK_SCENE.equals(currentScene)) {
                try {
                    PreFlightCheck.InstallCheckResult check =
                            PreFlightCheck.runInstallCheck(context, FALLBACK_SCENE);
                    if (check.allResourcesReady) {
                        Log.d(TAG, "🔄 Using fallback: " + FALLBACK_SCENE);
                        return FALLBACK_SCENE;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "🔄 Fallback check error: " + e.getMessage());
                }
            }

            return null; // even fallback unavailable — keep current
        }

        /**
         * Cleanup keeping only: current + previous + fallback + panel.
         * Max 2 wallpapers stored + fallback (GATITO_IMG).
         */
        private void cleanupWithHistory(String currentScene, String previousScene) {
            new Thread(() -> {
                try {
                    ResourcePreloader preloader = new ResourcePreloader(context);
                    // Protect previous scene (current is protected by cleanupAfterInstallation)
                    preloader.setActiveSceneToProtect(previousScene);
                    // Protect pre-downloaded scene if one exists
                    String preDown = preDownloadedScene;
                    if (preDown != null) {
                        preloader.setAdditionalSceneToProtect(preDown);
                    }
                    preloader.cleanupAfterInstallation(currentScene);
                    Log.d(TAG, "🔄 Cleanup: keeping " + currentScene + " + " + previousScene + " + " + FALLBACK_SCENE);
                } catch (Exception e) {
                    Log.w(TAG, "🔄 Cleanup error: " + e.getMessage());
                }
            }, "AutoRotateCleanup").start();
        }

        /**
         * 🚀 Pre-downloads the NEXT random wallpaper in background.
         * Called after each successful rotation so the next switch is instant.
         */
        private void preDownloadNextScene() {
            if (preDownloadInProgress) {
                Log.d(TAG, "🔄 Pre-download: already in progress, skipping");
                return;
            }
            if (!wallpaperPrefs.isAutoRotateEnabled()) return;
            if (!isInternetAvailable()) {
                Log.d(TAG, "🔄 Pre-download: no internet, skipping");
                return;
            }

            preDownloadInProgress = true;
            new Thread(() -> {
                try {
                    List<WallpaperItem> candidates = WallpaperCatalog.get().getAutoRotateCandidates();
                    if (candidates.isEmpty()) return;

                    String currentScene = wallpaperPrefs.getSelectedWallpaperSync();
                    Collections.shuffle(candidates);

                    for (WallpaperItem candidate : candidates) {
                        String sceneName = candidate.getSceneName();
                        if (sceneName.equals(currentScene)) continue;

                        PreFlightCheck.InstallCheckResult check =
                                PreFlightCheck.runInstallCheck(context, sceneName);
                        if (check.allResourcesReady) {
                            // Already downloaded — save as pre-downloaded
                            preDownloadedScene = sceneName;
                            Log.d(TAG, "🔄 Pre-download: " + sceneName + " already local ✓");
                            return;
                        }

                        // Download it
                        Log.d(TAG, "🔄 Pre-download: downloading " + sceneName + "...");
                        boolean ok = downloadResources(check.getAllMissing());
                        if (ok) {
                            PreFlightCheck.InstallCheckResult recheck =
                                    PreFlightCheck.runInstallCheck(context, sceneName);
                            if (recheck.allResourcesReady) {
                                preDownloadedScene = sceneName;
                                Log.d(TAG, "🔄 Pre-download: " + sceneName + " ✓ ready for next rotation");
                                return;
                            }
                        }
                        Log.w(TAG, "🔄 Pre-download: " + sceneName + " failed, trying next...");
                    }

                    Log.d(TAG, "🔄 Pre-download: no candidates downloaded (will download at rotation time)");
                } catch (Exception e) {
                    Log.w(TAG, "🔄 Pre-download error: " + e.getMessage());
                } finally {
                    preDownloadInProgress = false;
                }
            }, "PreDownload").start();
        }

        private boolean isInternetAvailable() {
            try {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo net = cm.getActiveNetworkInfo();
                return net != null && net.isConnected();
            } catch (Exception e) {
                return false;
            }
        }

        /**
         * Downloads resource files synchronously. Returns true only if ALL succeed.
         */
        private boolean downloadResources(List<String> files) {
            if (files.isEmpty()) return true;
            for (String file : files) {
                boolean ok;
                try {
                    if (file.endsWith(".mp4")) {
                        ok = VideoDownloadManager.getInstance(context).downloadVideoSync(file, p -> {});
                    } else if (file.endsWith(".obj")) {
                        ok = ModelDownloadManager.getInstance(context).downloadModelSync(file, p -> {});
                    } else {
                        ok = ImageDownloadManager.getInstance(context).downloadImageSync(file, p -> {});
                    }
                } catch (Exception e) {
                    Log.w(TAG, "🔄 Download error: " + file + " → " + e.getMessage());
                    ok = false;
                }
                if (!ok) return false;
            }
            return true;
        }

        private void scheduleNextRotation() {
            autoRotateHandler.removeCallbacks(autoRotateRunnable);
            if (wallpaperPrefs.isAutoRotateEnabled()) {
                autoRotateHandler.postDelayed(autoRotateRunnable, AUTO_ROTATE_INTERVAL_MS);
                autoRotateScheduled = true;
                autoRotateScheduledAt = System.currentTimeMillis();
                autoRotateNextChangeTime = autoRotateScheduledAt + AUTO_ROTATE_INTERVAL_MS;
                long min = (AUTO_ROTATE_INTERVAL_MS / 1000) / 60;
                Log.d(TAG, "🔄 Auto-rotate: SCHEDULED — next change in " + min + " min (enabled=true)");
            } else {
                autoRotateScheduled = false;
                Log.d(TAG, "🔄 Auto-rotate: NOT scheduled (enabled=false)");
            }
        }

        /**
         * Ensures auto-rotate is running if enabled. Safe to call multiple times.
         */
        private void ensureAutoRotateRunning() {
            if (wallpaperPrefs.isAutoRotateEnabled()) {
                if (!autoRotateScheduled) {
                    Log.d(TAG, "🔄 Auto-rotate: was not scheduled, starting now");
                    scheduleNextRotation();
                } else {
                    logAutoRotateRemaining();
                }
            }
        }

        private void logAutoRotateRemaining() {
            if (!autoRotateScheduled || autoRotateNextChangeTime == 0) return;
            long remaining = autoRotateNextChangeTime - System.currentTimeMillis();
            if (remaining <= 0) return;
            long min = (remaining / 1000) / 60;
            long sec = (remaining / 1000) % 60;
            Log.d(TAG, "🔄 Auto-rotate: next change in " + min + ":" + String.format("%02d", sec));
        }

        private void stopAutoRotation() {
            autoRotateHandler.removeCallbacks(autoRotateRunnable);
            autoRotateScheduled = false;
            Log.d(TAG, "🔄 Auto-rotate: stopped");
        }

        // ═══════════════════════════════════════════════════════════════
        // 📱 DETECCIÓN DE EVENTOS DEL SISTEMA
        // ═══════════════════════════════════════════════════════════════
        private ScreenStateReceiver screenStateReceiver;

        GLWallpaperEngine(Context context) {
            Log.d(TAG, "╔════════════════════════════════════════╗");
            Log.d(TAG, "║   🚀 ENGINE INICIANDO (ANTI-FLICKER)   ║");
            Log.d(TAG, "╚════════════════════════════════════════╝");

            this.context = context;
            wallpaperPrefs = WallpaperPreferences.getInstance(context);

            // 🔧 FIX ANR: NO inicializar sistemas de monetización aquí
            // Estos sistemas (Ads, Rewards, Missions) son pesados y NO son necesarios
            // para que el wallpaper funcione. Se inicializan en MainActivity.
            //
            // ELIMINADOS del WallpaperService:
            // - UsageTracker.init()
            // - RewardsManager.init()
            // - RemoteConfigManager.init()
            // - MissionsManager.init()
            // - AdsManager.init() ← Este era el principal culpable (MobileAds.initialize es MUY lento)
            Log.d(TAG, "🚀 WallpaperService ligero - sin sistemas de monetización");

            // Inicializar BackgroundWorker
            BackgroundWorker.initialize();

            // Habilitar touch
            setTouchEventsEnabled(true);

            // 🔄 Ensure fallback wallpaper is always available + start auto-rotate
            ensureFallbackDownloaded();
            ensureAutoRotateRunning();

            // 🔔 Listen for auto-rotate toggle changes → react immediately
            prefListener = (sharedPreferences, key) -> {
                if (WallpaperPreferences.KEY_AUTO_ROTATE_ENABLED.equals(key)) {
                    boolean enabled = wallpaperPrefs.isAutoRotateEnabled();
                    Log.d(TAG, "🔔 Auto-rotate toggled → " + (enabled ? "ON" : "OFF"));
                    if (enabled) {
                        ensureAutoRotateRunning();
                        preDownloadNextScene();
                    } else {
                        stopAutoRotation();
                        preDownloadedScene = null;
                    }
                }
            };
            wallpaperPrefs.registerOnPreferenceChangeListener(prefListener);

            // Gestor de pantalla de carga
            chargingScreenManager = new ChargingScreenManager(context);
            chargingScreenManager.register();

            // 📱 Registrar receptor de eventos de pantalla
            registerScreenStateReceiver();

            initializeGL();
        }

        /**
         * 📱 Registra un BroadcastReceiver para detectar eventos del sistema:
         * - Pantalla encendida/apagada
         * - Usuario desbloqueó
         * - Home presionado
         */
        private void registerScreenStateReceiver() {
            screenStateReceiver = new ScreenStateReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_USER_PRESENT);
            filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);  // Home, recents

            // Android 13+ requiere especificar RECEIVER_NOT_EXPORTED para broadcasts del sistema
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(screenStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                context.registerReceiver(screenStateReceiver, filter);
            }
            Log.d(TAG, "📱 ScreenStateReceiver registrado");
        }

        /**
         * 📱 BroadcastReceiver para eventos del sistema
         */
        private class ScreenStateReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) return;

                switch (action) {
                    case Intent.ACTION_SCREEN_OFF:
                        Log.d(TAG, "📱 SCREEN_OFF - Forzando STOP");
                        forceStopAnimation();
                        break;

                    case Intent.ACTION_SCREEN_ON:
                        Log.d(TAG, "📱 SCREEN_ON");
                        // No iniciamos aquí, esperamos visibilidad
                        break;

                    case Intent.ACTION_USER_PRESENT:
                        Log.d(TAG, "📱 USER_PRESENT - Usuario desbloqueó");
                        // 🔄 Ensure auto-rotate survived sleep
                        ensureAutoRotateRunning();
                        break;

                    case Intent.ACTION_CLOSE_SYSTEM_DIALOGS:
                        // Esto se dispara cuando: Home, Recents, App Switcher
                        String reason = intent.getStringExtra("reason");
                        Log.d(TAG, "📱 CLOSE_SYSTEM_DIALOGS reason=" + reason);
                        if ("homekey".equals(reason) || "recentapps".equals(reason) ||
                            "assist".equals(reason) || "voiceinteraction".equals(reason)) {
                            // Usuario presionó Home o abrió Recents
                            forceStopAnimation();
                        }
                        break;
                }
            }
        }

        /**
         * ⚡ Home/Recents pressed - NO pausar aquí
         * onVisibilityChanged(false) se encargará de pausar correctamente.
         * Pausar aquí sin un resume() correspondiente causa congelamiento
         * porque currentState sigue RUNNING y startRendering() no llama resume().
         */
        private void forceStopAnimation() {
            // NO-OP: onVisibilityChanged maneja pause/resume correctamente
            Log.d(TAG, "⚡ Home/Recents - delegando a onVisibilityChanged");
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            super.onTouchEvent(event);

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                Log.d(TAG, "👆 Touch DOWN en (" + event.getX() + ", " + event.getY() + ") state=" + currentState);
            }
            synchronized (stateLock) {
                // 🔧 FIX: Procesar touch cuando está RUNNING (incluye PANEL_MODE del Director)
                // El WallpaperDirector necesita touch para el botón Play incluso en Panel de Control
                if (currentState == RenderState.RUNNING || currentState == RenderState.STOPPED) {
                    if (wallpaperDirector != null) {
                        wallpaperDirector.onTouchEvent(event);
                    }
                }
            }
        }

        /**
         * Verifies a wallpaper's resources are locally available.
         * If not, falls back to FALLBACK_SCENE. If even that fails, uses default.
         */
        private String getSafeWallpaper(String requested) {
            try {
                PreFlightCheck.InstallCheckResult check =
                        PreFlightCheck.runInstallCheck(context, requested);
                if (check.allResourcesReady) return requested;
                Log.w(TAG, "🔄 " + requested + " resources missing, trying fallback");
            } catch (Exception e) {
                Log.w(TAG, "🔄 Error checking " + requested + ": " + e.getMessage());
            }

            // Try fallback
            if (!FALLBACK_SCENE.equals(requested)) {
                try {
                    PreFlightCheck.InstallCheckResult fallbackCheck =
                            PreFlightCheck.runInstallCheck(context, FALLBACK_SCENE);
                    if (fallbackCheck.allResourcesReady) {
                        Log.d(TAG, "🔄 Using fallback: " + FALLBACK_SCENE);
                        wallpaperPrefs.setSelectedWallpaper(FALLBACK_SCENE);
                        return FALLBACK_SCENE;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "🔄 Fallback check error: " + e.getMessage());
                }
            }

            // Last resort: return fallback anyway — WallpaperDirector will show
            // "Orbix iA" branded screen if the scene can't render
            Log.w(TAG, "🔄 No resources available — Director will show Orbix iA fallback");
            return FALLBACK_SCENE;
        }

        private void initializeGL() {
            try {
                String nombreWallpaper = getSafeWallpaper(wallpaperPrefs.getSelectedWallpaperSync());
                Log.d(TAG, "🎬 Escena seleccionada: " + nombreWallpaper);

                Log.d(TAG, "Inicializando OpenGL ES 3.0...");

                glSurfaceView = new GLWallpaperSurfaceView(context);

                // OpenGL ES 3.0
                glSurfaceView.setEGLContextClientVersion(3);
                glSurfaceView.setPreserveEGLContextOnPause(true);
                // 🧠 RGB565 + 16-bit depth: ahorra ~20MB vs RGBA8888+24bit
                // Live wallpapers no necesitan canal alpha (el sistema lo maneja)
                glSurfaceView.setEGLConfigChooser(5, 6, 5, 0, 16, 0);

                Log.d(TAG, "╔════════════════════════════════════════╗");
                Log.d(TAG, "║   🚀 OPENGL ES 3.0 ACTIVADO           ║");
                Log.d(TAG, "╚════════════════════════════════════════╝");

                // 🎬 WallpaperDirector - Sistema modular de renderizado
                Log.d(TAG, "🎬 Usando WallpaperDirector");
                wallpaperDirector = new WallpaperDirector(context);

                wallpaperDirector.changeScene(nombreWallpaper);

                glSurfaceView.setRenderer(wallpaperDirector);

                // 🔧 FIX GL FREEZE: Siempre CONTINUOUSLY - el Director maneja pausa via Thread.sleep
                // RENDERMODE_WHEN_DIRTY → CONTINUOUSLY no despierta el GL thread confiablemente
                glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                synchronized (stateLock) {
                    currentState = RenderState.RUNNING;
                }
                Log.d(TAG, "✓ OpenGL inicializado en modo CONTINUOUSLY");

            } catch (Exception e) {
                Log.e(TAG, "Error inicializando renderer", e);
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // 🎯 VISIBILIDAD - Respuesta INSTANTÁNEA
        // ═══════════════════════════════════════════════════════════════
        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            Log.d(TAG, visible ? "👁️ VISIBLE" : "🔒 OCULTO");

            synchronized (stateLock) {
                if (glSurfaceView == null || currentState == RenderState.UNINITIALIZED) {
                    Log.w(TAG, "GL no inicializado, ignorando cambio de visibilidad");
                    return;
                }

                if (!surfaceExists) {
                    Log.w(TAG, "Surface no existe, ignorando cambio de visibilidad");
                    return;
                }

                if (visible) {
                    lastVisibilityChangeTime = System.currentTimeMillis();
                    startRendering();
                    // 🔄 Ensure auto-rotate is running (safe to call repeatedly)
                    ensureAutoRotateRunning();
                } else {
                    // 🎬 En preview del sistema, NO parar rendering
                    if (isSystemPreviewMode) {
                        Log.d(TAG, "🎬 Preview del sistema: Ignorando HIDDEN - render sigue activo");
                    } else {
                        // 🔧 FIX: Debounce para evitar pause/resume rápidos (Samsung stuttering)
                        long now = System.currentTimeMillis();
                        long elapsed = now - lastVisibilityChangeTime;
                        if (elapsed < VISIBILITY_DEBOUNCE_MS) {
                            Log.d(TAG, "⏳ Debounce: ignorando HIDDEN (" + elapsed + "ms desde VISIBLE)");
                            return;
                        }
                        lastVisibilityChangeTime = now;
                        autoPlayHandler.removeCallbacks(autoPlayRunnable);  // Cancelar auto-play pendiente
                        // 🔄 Auto-rotate keeps running in background (timer doesn't pause)
                        stopRendering();
                        if (wallpaperDirector != null) {
                            wallpaperDirector.pause();
                            Log.d(TAG, "⚡ Director pausado por pérdida de visibilidad");
                        }
                    }
                }
            }
        }

        /**
         * 🟢 INICIAR RENDERIZADO - Solo si está en STOPPED
         * 🔧 SIEMPRE verifica cambio de wallpaper (incluso si ya está corriendo)
         */
        private void startRendering() {
            // 🔧 FIX: SIEMPRE verificar cambio de wallpaper
            // Si el usuario instaló un wallpaper nuevo mientras otro estaba corriendo,
            // changeScene() detecta la diferencia y auto-switch (sin recrear GL)
            String wallpaperName = wallpaperPrefs.getSelectedWallpaperSync();
            if (wallpaperDirector != null) {
                wallpaperDirector.changeScene(wallpaperName);
                Log.d(TAG, "🎬 Escena verificada: " + wallpaperName);

                // 🔋 AUTO-PANEL + AUTO-PLAY: Al volver al home, forzar panel mode
                // y programar auto-play tras 500ms (evita escena pesada en app-switching rápido)
                // En preview del sistema (wallpaper picker), NO forzar panel
                if (!isSystemPreviewMode) {
                    wallpaperDirector.switchToPanelMode();
                    autoPlayHandler.removeCallbacks(autoPlayRunnable);
                    autoPlayHandler.postDelayed(autoPlayRunnable, AUTO_PLAY_DELAY_MS);
                }
            }

            if (currentState == RenderState.RUNNING) {
                // 🔧 FIX FREEZE: Siempre llamar resume() incluso si GL ya está corriendo.
                // Si algo pausó el director (ej: forceStopAnimation) sin cambiar currentState,
                // el director quedaría pausado indefinidamente sin este resume().
                if (wallpaperDirector != null) {
                    wallpaperDirector.resume();
                }
                Log.d(TAG, "GL ya corriendo, se verificó escena y se aseguró resume");
                return;
            }

            if (currentState != RenderState.STOPPED) {
                Log.w(TAG, "Estado inválido para iniciar: " + currentState);
                return;
            }

            // 🔧 FIX GL FREEZE: NO cambiar render mode - ya es CONTINUOUSLY permanente.
            // Solo necesitamos llamar resume() para que el Director deje de idle/sleep.

            // PASO 1: Reanudar lógica
            if (wallpaperDirector != null) {
                Log.d(TAG, "🎬 Reanudando WallpaperDirector...");
                try {
                    wallpaperDirector.resume();
                    Log.d(TAG, "🎬 WallpaperDirector reanudado");
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error reanudando Director: " + e.getMessage());
                }
            } else {
                Log.e(TAG, "❌ WallpaperDirector es NULL!");
            }

            // PASO 2: Actualizar estado
            currentState = RenderState.RUNNING;

            Log.d(TAG, "🟢 RUNNING");
        }

        /**
         * 🔴 DETENER RENDERIZADO - Solo si está en RUNNING
         */
        private void stopRendering() {
            // Ya dentro de synchronized(stateLock)
            if (currentState == RenderState.STOPPED) {
                Log.d(TAG, "Ya está detenido, ignorando");
                return;
            }

            if (currentState != RenderState.RUNNING) {
                Log.w(TAG, "Estado inválido para detener: " + currentState);
                return;
            }

            // PASO 1: Pausar lógica (el GL thread sigue corriendo pero idle via Thread.sleep)
            if (wallpaperDirector != null) {
                wallpaperDirector.pause();
            }

            // 🔧 FIX GL FREEZE: NO cambiar render mode - el GL thread siempre corre.
            // WallpaperDirector.onDrawFrame() detecta paused=true y duerme 100ms (idle).
            // Esto evita el bug donde WHEN_DIRTY → CONTINUOUSLY no despierta el GL thread.

            // PASO 2: Actualizar estado
            currentState = RenderState.STOPPED;

            Log.d(TAG, "🔴 STOPPED (GL thread idle)");
        }

        // ═══════════════════════════════════════════════════════════════
        // 📐 SURFACE LIFECYCLE
        // ═══════════════════════════════════════════════════════════════
        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            Log.d(TAG, "📐 Surface CREATED");

            // 🎬 Detectar si es preview del sistema AHORA (seguro de llamar después de attach)
            try {
                boolean isSystemPreview = isPreview();
                Log.d(TAG, "🎬 isPreview() = " + isSystemPreview);

                if (isSystemPreview && wallpaperDirector != null) {
                    Log.d(TAG, "🎬 MODO PREVIEW DEL SISTEMA - Activando wallpaper directo");
                    wallpaperDirector.setPreviewMode(true);
                    isSystemPreviewMode = true;
                    // 🔧 FIX GL FREEZE: Ya es CONTINUOUSLY permanente, solo asegurar estado
                    synchronized (stateLock) {
                        currentState = RenderState.RUNNING;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "No se pudo determinar isPreview: " + e.getMessage());
            }

            synchronized (stateLock) {
                surfaceExists = true;
                if (glSurfaceView != null) {
                    glSurfaceView.surfaceCreated(holder);
                }
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            Log.d(TAG, "📐 Surface CHANGED: " + width + "x" + height);

            if (glSurfaceView != null) {
                glSurfaceView.surfaceChanged(holder, format, width, height);
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "📐 Surface DESTROYED");

            synchronized (stateLock) {
                // Detener si está corriendo
                if (currentState == RenderState.RUNNING) {
                    if (wallpaperDirector != null) {
                        wallpaperDirector.pause();
                    }
                    // 🔧 FIX GL FREEZE: No cambiar render mode, solo pausar lógica
                    currentState = RenderState.STOPPED;
                }

                surfaceExists = false;

                if (glSurfaceView != null) {
                    glSurfaceView.surfaceDestroyed(holder);
                }
            }

            super.onSurfaceDestroyed(holder);
        }

        // ═══════════════════════════════════════════════════════════════
        // 🧠 MEMORY PRESSURE - Delegado desde el Service
        // ═══════════════════════════════════════════════════════════════

        void handleTrimMemory(int level) {
            Log.w(TAG, "🧠 Engine handleTrimMemory level=" + level);
            if (wallpaperDirector != null) {
                wallpaperDirector.onTrimMemory(level);
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // 🧹 CLEANUP
        // ═══════════════════════════════════════════════════════════════
        @Override
        public void onDestroy() {
            Log.d(TAG, "╔════════════════════════════════════════╗");
            Log.d(TAG, "║   🧹 DESTRUYENDO ENGINE                ║");
            Log.d(TAG, "╚════════════════════════════════════════╝");

            // UsageTracker removido - no se usa

            synchronized (stateLock) {
                currentState = RenderState.UNINITIALIZED;
                surfaceExists = false;
            }

            // 🔄 Stop auto-rotate
            stopAutoRotation();

            // 🔔 Unregister preference listener
            if (prefListener != null) {
                wallpaperPrefs.unregisterOnPreferenceChangeListener(prefListener);
                prefListener = null;
            }

            // 📱 Desregistrar receptor de eventos de pantalla
            if (screenStateReceiver != null) {
                try {
                    context.unregisterReceiver(screenStateReceiver);
                    Log.d(TAG, "📱 ScreenStateReceiver desregistrado");
                } catch (Exception e) {
                    Log.w(TAG, "Error desregistrando receiver: " + e.getMessage());
                }
            }

            BackgroundWorker.shutdown();

            if (chargingScreenManager != null) {
                chargingScreenManager.unregister();
            }

            // Liberar WallpaperDirector
            if (wallpaperDirector != null) {
                wallpaperDirector.release();
                wallpaperDirector = null;
            }

            if (glSurfaceView != null) {
                glSurfaceView.onDestroy();
            }

            super.onDestroy();
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset,
                                     float xOffsetStep, float yOffsetStep,
                                     int xPixelOffset, int yPixelOffset) {
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset);
        }

        // ═══════════════════════════════════════════════════════════════
        // 🖼️ GLSURFACEVIEW WRAPPER
        // ═══════════════════════════════════════════════════════════════
        private class GLWallpaperSurfaceView extends GLSurfaceView {
            public GLWallpaperSurfaceView(Context context) {
                super(context);
            }

            @Override
            public SurfaceHolder getHolder() {
                return getSurfaceHolder();
            }

            void onDestroy() {
                super.onDetachedFromWindow();
            }
        }
    }
}
