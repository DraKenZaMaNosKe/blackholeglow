// LiveWallpaperService.java - VERSIÓN ULTRA-ROBUSTA ANTI-FLICKERING
package com.secret.blackholeglow;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.MotionEvent;
import android.content.Context;
import android.opengl.GLSurfaceView;

import androidx.annotation.NonNull;

import com.secret.blackholeglow.core.WallpaperDirector;
import com.secret.blackholeglow.filament.FilamentChristmasRenderer;
import com.secret.blackholeglow.systems.UsageTracker;
import com.secret.blackholeglow.systems.RewardsManager;
import com.secret.blackholeglow.systems.RemoteConfigManager;
import com.secret.blackholeglow.systems.MissionsManager;
import com.secret.blackholeglow.systems.AdsManager;

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
        return new GLWallpaperEngine(this);
    }

    private class GLWallpaperEngine extends Engine {
        private final WallpaperPreferences wallpaperPrefs;
        private final Context context;
        private GLWallpaperSurfaceView glSurfaceView;
        private WallpaperDirector wallpaperDirector;  // Sistema modular de renderizado
        private ChargingScreenManager chargingScreenManager;

        // 🎄 Filament para escena navideña con Santa animado
        private FilamentChristmasRenderer filamentRenderer;
        private boolean useFilament = false;

        private final Object stateLock = new Object();
        private RenderState currentState = RenderState.UNINITIALIZED;
        private boolean surfaceExists = false;
        private boolean isSystemPreviewMode = false;  // 🎬 Para mantener el wallpaper visible en preview del sistema
        private android.os.Handler mainHandler;  // 🎄 Para switches de renderer seguros
        private long touchDownTime = 0;  // 🎄 Para detectar long-press en modo Filament
        private static final long LONG_PRESS_DURATION = 1500; // 1.5 segundos
        private Runnable longPressRunnable;

        // ═══════════════════════════════════════════════════════════════
        // 📱 DETECCIÓN DE EVENTOS DEL SISTEMA
        // ═══════════════════════════════════════════════════════════════
        private boolean isScreenOn = true;
        private boolean isUserPresent = false;
        private ScreenStateReceiver screenStateReceiver;

        GLWallpaperEngine(Context context) {
            Log.d(TAG, "╔════════════════════════════════════════╗");
            Log.d(TAG, "║   🚀 ENGINE INICIANDO (ANTI-FLICKER)   ║");
            Log.d(TAG, "╚════════════════════════════════════════╝");

            this.context = context;
            wallpaperPrefs = WallpaperPreferences.getInstance(context);

            // 🏆 Inicializar sistema de recompensas y monetización
            UsageTracker.init(context);
            RewardsManager.init(context);
            RemoteConfigManager.init(context);
            MissionsManager.init(context);
            AdsManager.init(context);

            // Inicializar BackgroundWorker
            BackgroundWorker.initialize();

            // Habilitar touch
            setTouchEventsEnabled(true);

            // 🎄 Handler para switches de renderer seguros en main thread
            mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

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
                        isScreenOn = false;
                        isUserPresent = false;
                        forceStopAnimation();
                        break;

                    case Intent.ACTION_SCREEN_ON:
                        Log.d(TAG, "📱 SCREEN_ON");
                        isScreenOn = true;
                        // No iniciamos aquí, esperamos USER_PRESENT o visibilidad
                        break;

                    case Intent.ACTION_USER_PRESENT:
                        Log.d(TAG, "📱 USER_PRESENT - Usuario desbloqueó");
                        isUserPresent = true;
                        // El sistema llamará onVisibilityChanged si es necesario
                        break;

                    case Intent.ACTION_CLOSE_SYSTEM_DIALOGS:
                        // Esto se dispara cuando: Home, Recents, App Switcher
                        String reason = intent.getStringExtra("reason");
                        Log.d(TAG, "📱 CLOSE_SYSTEM_DIALOGS reason=" + reason);
                        // 🎄 Filament NO usa panel, así que no lo detenemos aquí
                        // Solo detener para OpenGL ES con panel de control
                        if (!useFilament) {
                            if ("homekey".equals(reason) || "recentapps".equals(reason) ||
                                "assist".equals(reason) || "voiceinteraction".equals(reason)) {
                                // Usuario presionó Home o abrió Recents
                                forceStopAnimation();
                            }
                        }
                        break;
                }
            }
        }

        /**
         * ⚡ Fuerza el wallpaper a PANEL_MODE inmediatamente
         * Este es el método clave anti-flickering: siempre vuelve al Panel de Control
         */
        private void forceStopAnimation() {
            synchronized (stateLock) {
                // 🎄 Filament
                if (useFilament && filamentRenderer != null) {
                    filamentRenderer.stop();
                    currentState = RenderState.STOPPED;
                    Log.d(TAG, "⚡ Filament detenido");
                    return;
                }

                if (wallpaperDirector != null) {
                    wallpaperDirector.pause();
                }
            }
            Log.d(TAG, "⚡ PANEL_MODE forzado - sin flickering");
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            super.onTouchEvent(event);

            // 🎄 Modo Filament: Detectar LONG-PRESS (1.5s) para volver al panel
            if (useFilament) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        touchDownTime = System.currentTimeMillis();
                        // Programar callback para long-press
                        if (longPressRunnable != null) {
                            mainHandler.removeCallbacks(longPressRunnable);
                        }
                        longPressRunnable = () -> {
                            Log.d(TAG, "🎄 Long-press detectado (1.5s) - Volviendo al panel");
                            switchToOpenGL();
                        };
                        mainHandler.postDelayed(longPressRunnable, LONG_PRESS_DURATION);
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        // Cancelar long-press si levantó el dedo antes
                        if (longPressRunnable != null) {
                            mainHandler.removeCallbacks(longPressRunnable);
                            longPressRunnable = null;
                        }
                        break;
                }
                return; // No procesar más en modo Filament
            }

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

                // 🚀 SIEMPRE usar OpenGL ES con panel de control primero
                // Filament se activará cuando el usuario presione PLAY
                useFilament = false;
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

                // 🎄 Listener para cambio a Filament (escenas con animación 3D)
                wallpaperDirector.setOnFilamentSceneListener(sceneName -> {
                    Log.d(TAG, "🎄 Callback recibido: Cambiar a Filament para " + sceneName);
                    // Postear al main thread con delay para asegurar cleanup de OpenGL ES
                    mainHandler.postDelayed(() -> switchToFilament(), 100);
                });

                wallpaperDirector.changeScene(nombreWallpaper);

                glSurfaceView.setRenderer(wallpaperDirector);

                // CRÍTICO: Empezar DETENIDO - el modo preview se configura en onSurfaceCreated
                glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
                synchronized (stateLock) {
                    currentState = RenderState.STOPPED;
                }
                Log.d(TAG, "✓ OpenGL inicializado en modo STOPPED");

            } catch (Exception e) {
                Log.e(TAG, "Error inicializando OpenGL", e);
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // 🎯 VISIBILIDAD - Respuesta INSTANTÁNEA
        // ═══════════════════════════════════════════════════════════════
        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            Log.d(TAG, visible ? "👁️ VISIBLE" : "🔒 OCULTO");

            // 🎄 Filament: Al ocultarse, DESTRUIR completamente para liberar recursos
            if (useFilament && !visible) {
                Log.d(TAG, "🎄 Wallpaper oculto - Destruyendo Filament para liberar recursos");
                UsageTracker.get().onWallpaperHidden();

                // Destruir Filament completamente
                if (filamentRenderer != null) {
                    filamentRenderer.stop();
                    filamentRenderer.destroy();
                    filamentRenderer = null;
                }

                synchronized (stateLock) {
                    useFilament = false;
                    currentState = RenderState.STOPPED;
                }

                Log.d(TAG, "✓ Filament destruido - Panel se mostrará al volver");
                return;
            }

            // 🚀 Si volvemos visible y no hay renderer (después de destruir Filament), recrear
            Log.d(TAG, "🔍 Check recreate: visible=" + visible + " glSurfaceView=" + (glSurfaceView != null) +
                       " useFilament=" + useFilament + " surfaceExists=" + surfaceExists);
            if (visible && glSurfaceView == null && !useFilament && surfaceExists) {
                Log.d(TAG, "🚀 Recreando OpenGL ES con panel (volviendo de Filament)");
                mainHandler.post(() -> recreateOpenGLWithPanel());
                return;
            }

            synchronized (stateLock) {
                // OpenGL ES normal
                if (glSurfaceView == null || currentState == RenderState.UNINITIALIZED) {
                    Log.w(TAG, "GL no inicializado, ignorando cambio de visibilidad");
                    return;
                }

                if (!surfaceExists) {
                    Log.w(TAG, "Surface no existe, ignorando cambio de visibilidad");
                    return;
                }

                if (visible) {
                    startRendering();
                    UsageTracker.get().onWallpaperVisible();
                } else {
                    UsageTracker.get().onWallpaperHidden();

                    // 🎬 En preview del sistema, NO forzar panel
                    if (isSystemPreviewMode) {
                        Log.d(TAG, "🎬 Preview del sistema: Manteniendo wallpaper visible");
                        stopRendering();
                    } else {
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
         * 🚀 Recrea OpenGL ES con el panel (después de destruir Filament)
         */
        private void recreateOpenGLWithPanel() {
            Log.d(TAG, "🚀 Recreando sistema OpenGL ES...");

            try {
                String nombreWallpaper = wallpaperPrefs.getSelectedWallpaperSync();

                glSurfaceView = new GLWallpaperSurfaceView(context);
                glSurfaceView.setEGLContextClientVersion(3);
                glSurfaceView.setPreserveEGLContextOnPause(true);
                glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 24, 0);

                wallpaperDirector = new WallpaperDirector(context);

                // Reconectar el listener de Filament
                wallpaperDirector.setOnFilamentSceneListener(sceneName -> {
                    Log.d(TAG, "🎄 Callback: Cambiar a Filament para " + sceneName);
                    mainHandler.postDelayed(() -> switchToFilament(), 100);
                });

                wallpaperDirector.changeScene(nombreWallpaper);
                glSurfaceView.setRenderer(wallpaperDirector);
                glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

                // 🔑 Forzar inicio del renderer ya que la Surface ya existe
                SurfaceHolder holder = getSurfaceHolder();
                if (holder != null && holder.getSurface() != null && holder.getSurface().isValid()) {
                    Log.d(TAG, "🔑 Surface válida, forzando surfaceCreated");
                    android.graphics.Rect rect = holder.getSurfaceFrame();
                    glSurfaceView.surfaceCreated(holder);
                    glSurfaceView.surfaceChanged(holder, android.graphics.PixelFormat.RGBA_8888, rect.width(), rect.height());
                }

                synchronized (stateLock) {
                    currentState = RenderState.RUNNING;
                }

                Log.d(TAG, "✅ OpenGL ES recreado con panel navideño");
            } catch (Exception e) {
                Log.e(TAG, "Error recreando OpenGL ES", e);
            }
        }

        /**
         * 🟢 INICIAR RENDERIZADO - Solo si está en STOPPED
         */
        private void startRendering() {
            // Ya dentro de synchronized(stateLock)
            if (currentState == RenderState.RUNNING) {
                Log.d(TAG, "Ya está corriendo, ignorando");
                return;
            }

            if (currentState != RenderState.STOPPED) {
                Log.w(TAG, "Estado inválido para iniciar: " + currentState);
                return;
            }

            // 🎄 Filament
            if (useFilament && filamentRenderer != null) {
                filamentRenderer.start();
                currentState = RenderState.RUNNING;
                Log.d(TAG, "🟢 RUNNING (Filament + Santa)");
                return;
            }

            // 🔧 FIX: Cargar wallpaper SINCRÓNICAMENTE ANTES de reanudar
            // Esto asegura que arcadeModeEnabled esté correcto antes del primer frame
            String wallpaperName = wallpaperPrefs.getSelectedWallpaperSync();
            if (wallpaperDirector != null) {
                wallpaperDirector.changeScene(wallpaperName);
                Log.d(TAG, "🎬 Escena cargada síncronamente: " + wallpaperName);
            }

            // PASO 1: Cambiar modo de render
            glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

            // PASO 2: Reanudar lógica
            if (wallpaperDirector != null) {
                wallpaperDirector.resume();
            }

            // PASO 3: Actualizar estado
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

            // 🎄 Filament
            if (useFilament && filamentRenderer != null) {
                filamentRenderer.stop();
                currentState = RenderState.STOPPED;
                Log.d(TAG, "🔴 STOPPED (Filament)");
                return;
            }

            // PASO 1: Pausar lógica PRIMERO
            if (wallpaperDirector != null) {
                wallpaperDirector.pause();
            }

            // PASO 2: Cambiar modo de render
            glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

            // PASO 3: Actualizar estado
            currentState = RenderState.STOPPED;

            Log.d(TAG, "🔴 STOPPED");
        }

        /**
         * 🎄 Cambia de OpenGL ES a Filament para escenas con animación 3D (Christmas)
         */
        private void switchToFilament() {
            Log.d(TAG, "╔════════════════════════════════════════╗");
            Log.d(TAG, "║   🎄 CAMBIANDO A FILAMENT...          ║");
            Log.d(TAG, "╚════════════════════════════════════════╝");

            // 1. DESTRUIR completamente OpenGL ES FUERA del lock para evitar deadlock
            if (glSurfaceView != null) {
                Log.d(TAG, "🔧 Destruyendo OpenGL ES...");

                // Pausar y detener el render thread
                glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
                glSurfaceView.onPause();

                // Liberar el director
                if (wallpaperDirector != null) {
                    wallpaperDirector.pause();
                    wallpaperDirector.release();
                    wallpaperDirector = null;
                }

                glSurfaceView = null;
                Log.d(TAG, "✓ OpenGL ES destruido");
            }

            synchronized (stateLock) {
                // 2. Activar modo Filament
                useFilament = true;
                currentState = RenderState.STOPPED;
            }

            // 3. Dar tiempo para que EGL se libere completamente
            mainHandler.postDelayed(() -> {
                Log.d(TAG, "🎄 Iniciando Filament después de cleanup...");

                // 4. Crear renderer de Filament
                if (filamentRenderer == null) {
                    filamentRenderer = new FilamentChristmasRenderer(context);
                }

                // 5. Inicializar con el SurfaceHolder actual
                SurfaceHolder holder = getSurfaceHolder();
                if (holder != null && holder.getSurface() != null && holder.getSurface().isValid()) {
                    filamentRenderer.initialize(holder);
                    android.graphics.Rect frame = holder.getSurfaceFrame();
                    filamentRenderer.setSize(frame.width(), frame.height());
                    filamentRenderer.start();
                    synchronized (stateLock) {
                        currentState = RenderState.RUNNING;
                    }
                    Log.d(TAG, "🎄 ¡Filament ACTIVO con Santa!");
                } else {
                    Log.w(TAG, "Surface no válido para Filament");
                }
            }, 200); // 200ms delay para asegurar cleanup de EGL
        }

        /**
         * 🔙 Vuelve de Filament a OpenGL ES (panel de control)
         */
        private void switchToOpenGL() {
            Log.d(TAG, "╔════════════════════════════════════════╗");
            Log.d(TAG, "║   🔙 VOLVIENDO A OPENGL ES...         ║");
            Log.d(TAG, "╚════════════════════════════════════════╝");

            // 1. Detener Filament FUERA del lock
            if (filamentRenderer != null) {
                Log.d(TAG, "🔧 Destruyendo Filament...");
                filamentRenderer.stop();
                filamentRenderer.destroy();
                filamentRenderer = null;
                Log.d(TAG, "✓ Filament destruido");
            }

            synchronized (stateLock) {
                // 2. Desactivar modo Filament
                useFilament = false;
                currentState = RenderState.STOPPED;
            }

            // 3. Dar tiempo para que Filament se libere
            mainHandler.postDelayed(() -> {
                Log.d(TAG, "🚀 Recreando OpenGL ES...");

                // 4. Reinicializar OpenGL ES completo
                try {
                    String nombreWallpaper = wallpaperPrefs.getSelectedWallpaperSync();

                    glSurfaceView = new GLWallpaperSurfaceView(context);
                    glSurfaceView.setEGLContextClientVersion(3);
                    glSurfaceView.setPreserveEGLContextOnPause(true);
                    glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 24, 0);

                    wallpaperDirector = new WallpaperDirector(context);

                    // Reconectar el listener de Filament
                    wallpaperDirector.setOnFilamentSceneListener(sceneName -> {
                        Log.d(TAG, "🎄 Callback recibido: Cambiar a Filament para " + sceneName);
                        mainHandler.postDelayed(() -> switchToFilament(), 100);
                    });

                    wallpaperDirector.changeScene(nombreWallpaper);
                    glSurfaceView.setRenderer(wallpaperDirector);

                    // Iniciar rendering
                    glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

                    synchronized (stateLock) {
                        currentState = RenderState.RUNNING;
                    }

                    Log.d(TAG, "🚀 ¡OpenGL ES restaurado con panel!");
                } catch (Exception e) {
                    Log.e(TAG, "Error recreando OpenGL ES", e);
                }
            }, 200);
        }

        private void loadWallpaperAsync() {
            wallpaperPrefs.getSelectedWallpaper(new WallpaperPreferences.WallpaperCallback() {
                @Override
                public void onWallpaperReceived(@NonNull String wallpaperName) {
                    synchronized (stateLock) {
                        if (currentState == RenderState.RUNNING) {
                            if (wallpaperDirector != null) {
                                wallpaperDirector.changeScene(wallpaperName);
                            }
                        }
                    }
                }
            });
        }

        // ═══════════════════════════════════════════════════════════════
        // 📐 SURFACE LIFECYCLE
        // ═══════════════════════════════════════════════════════════════
        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            Log.d(TAG, "📐 Surface CREATED");

            // 🎄 Si usamos Filament, inicializar aquí
            if (useFilament && filamentRenderer != null) {
                Log.d(TAG, "🎄 Inicializando Filament con SurfaceHolder");
                filamentRenderer.initialize(holder);
                synchronized (stateLock) {
                    surfaceExists = true;
                    currentState = RenderState.STOPPED;
                }
                return;
            }

            // 🎬 Detectar si es preview del sistema AHORA (seguro de llamar después de attach)
            try {
                boolean isSystemPreview = isPreview();
                Log.d(TAG, "🎬 isPreview() = " + isSystemPreview);

                if (isSystemPreview && wallpaperDirector != null) {
                    Log.d(TAG, "🎬 MODO PREVIEW DEL SISTEMA - Activando wallpaper directo");
                    wallpaperDirector.setPreviewMode(true);
                    isSystemPreviewMode = true;

                    // Cambiar a modo continuo para preview
                    if (glSurfaceView != null) {
                        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                    }
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

            // 🎄 Filament
            if (useFilament && filamentRenderer != null) {
                filamentRenderer.setSize(width, height);
                return;
            }

            if (glSurfaceView != null) {
                glSurfaceView.surfaceChanged(holder, format, width, height);
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "📐 Surface DESTROYED");

            synchronized (stateLock) {
                // 🎄 Filament
                if (useFilament && filamentRenderer != null) {
                    filamentRenderer.stop();
                    currentState = RenderState.STOPPED;
                    surfaceExists = false;
                    super.onSurfaceDestroyed(holder);
                    return;
                }

                // Detener si está corriendo
                if (currentState == RenderState.RUNNING) {
                    if (wallpaperDirector != null) {
                        wallpaperDirector.pause();
                    }
                    if (glSurfaceView != null) {
                        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
                    }
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
        // 🧹 CLEANUP
        // ═══════════════════════════════════════════════════════════════
        @Override
        public void onDestroy() {
            Log.d(TAG, "╔════════════════════════════════════════╗");
            Log.d(TAG, "║   🧹 DESTRUYENDO ENGINE                ║");
            Log.d(TAG, "╚════════════════════════════════════════╝");

            // ⏱️ Detener tracking de uso
            UsageTracker.get().onWallpaperHidden();

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

            // 🎄 Liberar Filament
            if (filamentRenderer != null) {
                filamentRenderer.destroy();
                filamentRenderer = null;
                Log.d(TAG, "🎄 Filament destruido");
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
