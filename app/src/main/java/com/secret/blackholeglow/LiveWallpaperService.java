// LiveWallpaperService.java - VERSIÓN ARREGLADA
package com.secret.blackholeglow;

import android.content.SharedPreferences;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.MotionEvent;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

/**
 * LiveWallpaperService con inicialización robusta
 */
public class LiveWallpaperService extends WallpaperService {
    private static final String TAG = "LiveWallpaperService";

    @Override
    public Engine onCreateEngine() {
        Log.d(TAG, "onCreateEngine llamado");
        return new GLWallpaperEngine(this);
    }

    private class GLWallpaperEngine extends Engine {
        private final WallpaperPreferences wallpaperPrefs;  // ✨ Nueva clase de preferencias
        private final Context context;
        private GLWallpaperSurfaceView glSurfaceView;
        private SceneRenderer sceneRenderer;
        private boolean rendererSet = false;
        private Handler mainHandler;
        private ChargingScreenManager chargingScreenManager;  // 🔋 Gestor de pantalla de carga

        GLWallpaperEngine(Context context) {
            Log.d(TAG, "GLWallpaperEngine constructor");
            this.context = context;
            this.mainHandler = new Handler(Looper.getMainLooper());

            wallpaperPrefs = WallpaperPreferences.getInstance(context);

            // 👆 HABILITAR TOUCH para sistema interactivo de disparo
            setTouchEventsEnabled(true);
            Log.d(TAG, "✨ Touch events HABILITADOS para interactividad");

            // 🔋 Inicializar gestor de pantalla de carga
            chargingScreenManager = new ChargingScreenManager(context);
            chargingScreenManager.register();
            Log.d(TAG, "🔋 ChargingScreenManager ACTIVADO");

            initializeGL();
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            super.onTouchEvent(event);

            // Pasar evento al renderer para procesamiento
            if (sceneRenderer != null) {
                sceneRenderer.onTouchEvent(event);
            }
        }

        private void initializeGL() {
            try {
                Log.d(TAG, "Inicializando OpenGL...");

                glSurfaceView = new GLWallpaperSurfaceView(context);

                // Configuración más conservadora
                glSurfaceView.setEGLContextClientVersion(2);
                glSurfaceView.setPreserveEGLContextOnPause(true);

                // Configurar EGL con valores seguros
                glSurfaceView.setEGLConfigChooser(
                        5, 6, 5, 0, 16, 0  // RGB565, depth 16, sin stencil
                );

                // ✨ Obtener wallpaper usando WallpaperPreferences (síncrono para inicialización)
                String nombreWallpaper = wallpaperPrefs.getSelectedWallpaperSync();
                Log.d(TAG, "Wallpaper seleccionado: " + nombreWallpaper);

                sceneRenderer = new SceneRenderer(context, nombreWallpaper);
                glSurfaceView.setRenderer(sceneRenderer);
                glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                rendererSet = true;

                Log.d(TAG, "OpenGL inicializado correctamente");

            } catch (Exception e) {
                Log.e(TAG, "Error inicializando OpenGL", e);
                rendererSet = false;
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            Log.d(TAG, "Visibilidad cambiada: " + visible);

            if (!rendererSet) {
                Log.w(TAG, "Renderer no configurado, ignorando cambio de visibilidad");
                return;
            }

            if (visible) {
                // ✨ Recargar configuración con callback asíncrono (primero Firebase, luego local)
                wallpaperPrefs.getSelectedWallpaper(new WallpaperPreferences.WallpaperCallback() {
                    @Override
                    public void onWallpaperReceived(@NonNull String wallpaperName) {
                        Log.d(TAG, "Wallpaper recibido: " + wallpaperName);

                        // Usar handler para evitar problemas de threading
                        mainHandler.post(() -> {
                            if (sceneRenderer != null) {
                                sceneRenderer.setSelectedItem(wallpaperName);
                                sceneRenderer.resume();
                            }
                            if (glSurfaceView != null) {
                                glSurfaceView.onResume();
                            }
                        });
                    }
                });

                Log.d(TAG, "Wallpaper visible y resumido");
            } else {
                mainHandler.post(() -> {
                    if (sceneRenderer != null) {
                        sceneRenderer.pause();
                    }
                    if (glSurfaceView != null) {
                        glSurfaceView.onPause();
                    }
                });

                Log.d(TAG, "Wallpaper oculto y pausado");
            }
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            Log.d(TAG, "onSurfaceCreated");

            if (glSurfaceView != null) {
                glSurfaceView.surfaceCreated(holder);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            Log.d(TAG, "onSurfaceChanged: " + width + "x" + height);

            if (glSurfaceView != null) {
                glSurfaceView.surfaceChanged(holder, format, width, height);
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "onSurfaceDestroyed");

            if (glSurfaceView != null) {
                glSurfaceView.surfaceDestroyed(holder);
            }

            super.onSurfaceDestroyed(holder);
        }

        @Override
        public void onDestroy() {
            Log.d(TAG, "onDestroy");

            // 🔋 Desregistrar gestor de pantalla de carga
            if (chargingScreenManager != null) {
                chargingScreenManager.unregister();
                Log.d(TAG, "🔋 ChargingScreenManager desactivado");
            }

            if (glSurfaceView != null) {
                glSurfaceView.onDestroy();
            }

            super.onDestroy();
        }

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