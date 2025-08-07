package com.secret.blackholeglow;

import android.content.SharedPreferences;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.MotionEvent;
import android.content.Context;
import android.opengl.GLSurfaceView;

public class LiveWallpaperService extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new GLWallpaperEngine(this);
    }

    private class GLWallpaperEngine extends Engine {
        private final SharedPreferences prefs;
        private final Context context;
        private GLWallpaperSurfaceView glSurfaceView;
        private SceneRenderer sceneRenderer;
        private float previousX;

        GLWallpaperEngine(Context context) {
            this.context = context;
            prefs = context.getSharedPreferences("blackholeglow_prefs", MODE_PRIVATE);
            setTouchEventsEnabled(true);
            glSurfaceView = new GLWallpaperSurfaceView(context);
            glSurfaceView.setEGLConfigChooser(8,8,8,8,16,0);
            glSurfaceView.setEGLContextClientVersion(2);
            String nombreWallpaper = prefs.getString("selected_wallpaper", "Estrellas");
            Log.d("LiveWallpaperService", "Wallpaper seleccionado: " + nombreWallpaper);
            sceneRenderer = new SceneRenderer(context, nombreWallpaper);
            glSurfaceView.setRenderer(sceneRenderer);
            glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    previousX = event.getX();
                    break;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getX() - previousX;
                    previousX = event.getX();
                    glSurfaceView.queueEvent(() -> {

                    });
                    break;
            }
            super.onTouchEvent(event);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (visible) {
                String nuevo = prefs.getString("selected_wallpaper", "Estrellas");
                sceneRenderer.setSelectedItem(nuevo);
                sceneRenderer.resume();
                glSurfaceView.onResume();
            } else {
                sceneRenderer.pause();
                glSurfaceView.onPause();
            }
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            glSurfaceView.surfaceCreated(holder);
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            glSurfaceView.surfaceChanged(holder, format, width, height);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            glSurfaceView.surfaceDestroyed(holder);
        }

        @Override
        public void onDestroy() {
            glSurfaceView.onDestroy();
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
