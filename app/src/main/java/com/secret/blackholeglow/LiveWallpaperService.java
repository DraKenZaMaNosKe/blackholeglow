package com.secret.blackholeglow;

import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;
import android.content.Context;
import android.opengl.GLSurfaceView;

/**
 * Servicio de fondo de pantalla que utiliza OpenGL ES para renderizar la escena.
 */
public class LiveWallpaperService extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        // Se crea el motor del wallpaper (GLWallpaperEngine) al iniciar el servicio.
        return new GLWallpaperEngine(this);
    }

    /**
     * Motor de wallpaper basado en OpenGL.
     * Permite asignar diferentes renderers (escenas u objetos) para ampliar la funcionalidad.
     */
    private class GLWallpaperEngine extends Engine {
        private final Context context;
        private GLWallpaperSurfaceView glSurfaceView;
        private SceneRenderer sceneRenderer;

        /**
         * Constructor del motor de fondo de pantalla usando OpenGL.
         * Inicializa la vista GLSurfaceView y el SceneRenderer.
         */
        GLWallpaperEngine(Context context) {
            this.context = context;
            // Evita recibir eventos táctiles (puede cambiarse si se quiere interacción).
            setTouchEventsEnabled(false);

            // Crear el GLSurfaceView y asignar el renderer actual.
            glSurfaceView = new GLWallpaperSurfaceView(context);
            // Usamos OpenGL ES 2.0.
            glSurfaceView.setEGLContextClientVersion(2);
            // Instanciamos el renderer que ya existe (SceneRenderer) para dibujar la escena.
            sceneRenderer = new SceneRenderer();
            glSurfaceView.setRenderer(sceneRenderer);
            // Modo de renderizado continuo (actualizaciones constantes).
            glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        }

        /**
         * Llamado cuando la visibilidad del wallpaper cambia (se muestra u oculta).
         * Pausa o reanuda el renderizado según corresponda.
         */
        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (visible) {
                // Si el wallpaper es visible, reanudar el dibujo.
                glSurfaceView.onResume();
            } else {
                // Si deja de ser visible, pausar el dibujo.
                glSurfaceView.onPause();
            }
        }

        /**
         * Llamado cuando se crea la superficie del wallpaper.
         * Notifica al GLSurfaceView que la superficie está lista.
         */
        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            // Delegamos la creación de la superficie al GLSurfaceView.
            glSurfaceView.surfaceCreated(holder);
        }

        /**
         * Llamado cuando cambian las dimensiones o formato de la superficie.
         * Actualiza el GLSurfaceView con los nuevos valores.
         */
        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            // Informamos a GLSurfaceView del cambio de tamaño de la superficie.
            glSurfaceView.surfaceChanged(holder, format, width, height);
        }

        /**
         * Llamado cuando se destruye la superficie del wallpaper.
         * Informa al GLSurfaceView para que libere recursos.
         */
        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            // Delegamos la destrucción de la superficie al GLSurfaceView.
            glSurfaceView.surfaceDestroyed(holder);
        }

        /**
         * Llamado cuando se destruye el motor del wallpaper.
         * Se limpia la vista GLSurfaceView y luego se llama al método padre.
         */
        @Override
        public void onDestroy() {
            // Detach GLSurfaceView del motor de wallpaper.
            glSurfaceView.onDestroy();
            super.onDestroy();
        }

        /**
         * Clase interna que extiende GLSurfaceView para redirigir el SurfaceHolder
         * al motor del wallpaper. Esto permite usar GLSurfaceView dentro de un WallpaperService.
         */
        private class GLWallpaperSurfaceView extends GLSurfaceView {
            public GLWallpaperSurfaceView(Context context) {
                super(context);
            }

            /**
             * Se anula para devolver el SurfaceHolder del wallpaper en lugar del de GLSurfaceView.
             * De esta forma, GLSurfaceView dibujará directamente en la superficie del wallpaper.
             */
            @Override
            public SurfaceHolder getHolder() {
                return getSurfaceHolder();
            }

            /**
             * Limpia la vista GLSurfaceView cuando el motor se destruye.
             */
            void onDestroy() {
                super.onDetachedFromWindow();
            }
        }
    }
}