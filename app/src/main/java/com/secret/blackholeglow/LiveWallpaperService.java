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
        private SceneRenderer sceneRenderer;
        private ChargingScreenManager chargingScreenManager;

        private final Object stateLock = new Object();
        private RenderState currentState = RenderState.UNINITIALIZED;
        private boolean surfaceExists = false;

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
         * ⚡ Fuerza el wallpaper a PANEL_MODE inmediatamente
         * Este es el método clave anti-flickering: siempre vuelve al Panel de Control
         */
        private void forceStopAnimation() {
            synchronized (stateLock) {
                if (sceneRenderer != null) {
                    // Forzar cambio INMEDIATO a Panel de Control
                    sceneRenderer.switchToPanelMode();
                }
            }
            Log.d(TAG, "⚡ PANEL_MODE forzado - sin flickering");
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            super.onTouchEvent(event);
            synchronized (stateLock) {
                if (sceneRenderer != null && currentState == RenderState.RUNNING) {
                    sceneRenderer.onTouchEvent(event);
                }
            }
        }

        private void initializeGL() {
            try {
                Log.d(TAG, "Inicializando OpenGL ES 3.0...");

                glSurfaceView = new GLWallpaperSurfaceView(context);

                // OpenGL ES 3.0
                glSurfaceView.setEGLContextClientVersion(3);
                glSurfaceView.setPreserveEGLContextOnPause(true);
                glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 24, 0);

                Log.d(TAG, "╔════════════════════════════════════════╗");
                Log.d(TAG, "║   🚀 OPENGL ES 3.0 ACTIVADO           ║");
                Log.d(TAG, "╚════════════════════════════════════════╝");

                String nombreWallpaper = wallpaperPrefs.getSelectedWallpaperSync();
                sceneRenderer = new SceneRenderer(context, nombreWallpaper);
                glSurfaceView.setRenderer(sceneRenderer);

                // CRÍTICO: Empezar DETENIDO
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
                    startRendering();
                } else {
                    // 📱 IMPORTANTE: Cuando no es visible, volver a PANEL_MODE
                    stopRendering();
                    // Forzar PANEL_MODE para evitar flickering al volver
                    if (sceneRenderer != null) {
                        sceneRenderer.switchToPanelMode();
                        Log.d(TAG, "⚡ PANEL_MODE activado por pérdida de visibilidad");
                    }
                }
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

            // PASO 1: Cambiar modo de render PRIMERO
            glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

            // PASO 2: Reanudar lógica
            if (sceneRenderer != null) {
                sceneRenderer.resume();
            }

            // PASO 3: Actualizar estado
            currentState = RenderState.RUNNING;

            Log.d(TAG, "🟢 RUNNING");

            // Cargar wallpaper en background
            loadWallpaperAsync();
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

            // PASO 1: Pausar lógica PRIMERO
            if (sceneRenderer != null) {
                sceneRenderer.pause();
            }

            // PASO 2: Cambiar modo de render
            glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

            // PASO 3: Actualizar estado
            currentState = RenderState.STOPPED;

            Log.d(TAG, "🔴 STOPPED");
        }

        private void loadWallpaperAsync() {
            wallpaperPrefs.getSelectedWallpaper(new WallpaperPreferences.WallpaperCallback() {
                @Override
                public void onWallpaperReceived(@NonNull String wallpaperName) {
                    synchronized (stateLock) {
                        if (sceneRenderer != null && currentState == RenderState.RUNNING) {
                            sceneRenderer.setSelectedItem(wallpaperName);
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
                    if (sceneRenderer != null) {
                        sceneRenderer.pause();
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
