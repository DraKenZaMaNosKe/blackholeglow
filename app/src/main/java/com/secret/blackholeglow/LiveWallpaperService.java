// LiveWallpaperService.java - VERSIÓN ULTRA-ROBUSTA ANTI-FLICKERING
package com.secret.blackholeglow;

import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.MotionEvent;
import android.content.Context;
import android.opengl.GLSurfaceView;


import com.secret.blackholeglow.core.WallpaperDirector;
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
                        // El sistema llamará onVisibilityChanged si es necesario
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

        private void initializeGL() {
            try {
                String nombreWallpaper = wallpaperPrefs.getSelectedWallpaperSync();
                Log.d(TAG, "🎬 Escena seleccionada: " + nombreWallpaper);

                Log.d(TAG, "Inicializando OpenGL ES 3.0...");

                glSurfaceView = new GLWallpaperSurfaceView(context);

                // OpenGL ES 3.0
                glSurfaceView.setEGLContextClientVersion(3);
                glSurfaceView.setPreserveEGLContextOnPause(true);
                glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 24, 0);

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
