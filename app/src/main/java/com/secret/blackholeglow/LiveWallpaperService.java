package com.secret.blackholeglow;

import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;
import android.content.Context;
import android.opengl.GLSurfaceView;

/*
╔════════════════════════════════════════════════════════════════════════╗
║                                                                        ║
║      🌌🔆  LiveWallpaperService.java - Servicio Live Wallpaper  🔆🌌     ║
║                                                                        ║
║    ASCII Art:                                                           ║
║              .-""""-.         ☾       ╔══════════╗                     ║
║             / -   -  \     🌠  .   🌠   ║  COSMOS  ║                     ║
║            |  o   o  |     .    .     ╚══════════╝                     ║
║            |    >    |         |\     💫𝙍𝙚𝙣𝙙𝙚𝙧𝙞𝙯𝙖𝙣𝙙𝙤 𝙚𝙡 𝙪𝙣𝙞𝙫𝙚𝙧𝙨𝙤💫                  ║
║            |  \___/  |        / \                                        ║
║             \______/        🌌🌠🌌                                       ║
║                                                                        ║
║   Descripción General:                                                 ║
║   • Clase que extiende WallpaperService para crear un fondo animado    ║
║     basado en OpenGL ES.                                               ║
║   • Define un Engine interno (GLWallpaperEngine) que utiliza GLSurfaceView ║
║     para renderizar la escena 3D/2D del fondo de pantalla.              ║
║   • Permite pausar y reanudar el renderizado según la visibilidad.     ║
║                                                                        ║
╚════════════════════════════════════════════════════════════════════════╝
*/

/**
 * LiveWallpaperService - Servicio de fondo de pantalla que utiliza OpenGL ES para renderizar la escena.
 *
 * 📝 Propósito Principal:
 *   - Crear el motor (Engine) que controla la vista OpenGL del wallpaper.
 *   - Manejar eventos de ciclo de vida del wallpaper (visibilidad, creación y destrucción de superficie).
 *   - Delegar la renderización a SceneRenderer, encargado de dibujar objetos animados.
 *
 * 🎨 Equipo Artístico:
 *   - Athena protege el fondo cósmico mientras las estrellas bailan.
 *   - El motor GLWallpaperEngine orquesta la sinfonía gráfica detrás de las galaxias.
 */
public class LiveWallpaperService extends WallpaperService {

    /**
     * onCreateEngine - Se invoca al iniciar el servicio de wallpaper.
     * Crea y retorna una instancia de GLWallpaperEngine, que será el responsable
     * de manejar el ciclo de vida y el renderizado de OpenGL.
     *
     * ASCII Art:
     *   ╔════════════════════════════════════════════════╗
     *   ║    ⚙️  GLWallpaperEngine Creado Aquí   ⚙️     ║
     *   ╚════════════════════════════════════════════════╝
     *           \\      🌟      //
     *            \\  ENGINE   //
     *             \\   ON    //
     *              \\ CREATE //
     */
    @Override
    public Engine onCreateEngine() {
        // Se crea el motor del wallpaper (GLWallpaperEngine) al iniciar el servicio.
        return new GLWallpaperEngine(this);
    }

    /*
    ╔════════════════════════════════════════════════════════════════════════╗
    ║                                                                        ║
    ║   🔧🔧  GLWallpaperEngine - Motor de Wallpaper basado en OpenGL  🔧🔧    ║
    ║                                                                        ║
    ║   • Clase interna que extiende WallpaperService.Engine                ║
    ║   • Se encarga de inicializar y manejar GLSurfaceView para el wallpaper║
    ║   • Controla la pausa/reanudación de renderizado en onVisibilityChanged  ║
    ║   • Transmite eventos de superficie (creación, cambio, destrucción)    ║
    ║                                                                        ║
    ╚════════════════════════════════════════════════════════════════════════╝
    */
    private class GLWallpaperEngine extends Engine {

        // 🌐 Contexto de la aplicación, necesario para crear vistas y cargar recursos.
        private final Context context;

        // 🖼️ Vista basada en GLSurfaceView que dibuja la escena OpenGL en el wallpaper.
        private GLWallpaperSurfaceView glSurfaceView;

        // 🎨 Renderizador principal que contiene la lógica de dibujo (estrellas, nebulosas, efectos).
        private SceneRenderer sceneRenderer;

        /**
         * Constructor GLWallpaperEngine - Inicializa el motor de fondo de pantalla.
         *
         * @param context - Contexto de aplicación proveniente de LiveWallpaperService.
         *
         * ASCII Art:
         *   ╔════════════════════════════════════════════════╗
         *   ║  🔄 Inicializando GLWallpaperSurfaceView y SceneRenderer 🔄 ║
         *   ╚════════════════════════════════════════════════╝
         *                .-""""-.
         *               /      __\    //////
         *              |  (*) (**) |  |||||  <--- Contexto listo
         *               \   <>   /   |||||
         *                '------'    \\\\
         *
         * 🚀 Detalles:
         *   - Deshabilita eventos táctiles (touch) por defecto.
         *   - Crea la vista GLSurfaceView configurada para ES 2.0.
         *   - Asigna SceneRenderer para dibujar la escena en bucle continuo.
         */
        GLWallpaperEngine(Context context) {
            this.context = context;

            // ❌ Deshabilita eventos táctiles (Touch), cambiar si se desea interacción.
            setTouchEventsEnabled(false);

            // ▪️ Crear la vista OpenGL específica para wallpapers.
            glSurfaceView = new GLWallpaperSurfaceView(context);

            // 🔧 Configurar GL ES 2.0
            glSurfaceView.setEGLContextClientVersion(2);

            // 🖌️ Instanciar el renderizador que dibuja las estrellas y efectos.
            sceneRenderer = new SceneRenderer(context);
            glSurfaceView.setRenderer(sceneRenderer);

            // 🔁 Modo continuo: redibuja permanentemente para animaciones fluidas.
            glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        }

        /**
         * onVisibilityChanged - Invocado cuando el wallpaper cambia de visibilidad.
         * Si el wallpaper se vuelve visible, reanuda el renderizado; si se oculta, lo pausa.
         *
         * @param visible - true si el wallpaper se muestra, false si se oculta.
         *
         * ASCII Art:
         *   ╔════════════════════════════════════════════════╗
         *   ║  👁️👁️  onVisibilityChanged:  Visible?  👁️👁️  ║
         *   ╚════════════════════════════════════════════════╝
         *        ┌─────────┐        ┌──────────┐
         *   YES: │ REANUDAR│        │ PAUSAR   │ : NO
         *        └─────────┘        └──────────┘
         *        ╰── escena reanuda      ╰── escena pausa
         */
        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (visible) {
                // ▶️ Reanuda la lógica de actualización de la escena.
                sceneRenderer.resume();
                // ▶️ Reactiva el hilo de renderizado de GLSurfaceView.
                glSurfaceView.onResume();
            } else {
                // ⏸️ Pausa la lógica de actualización de la escena.
                sceneRenderer.pause();
                // ⏸️ Suspende el hilo de renderizado de GLSurfaceView.
                glSurfaceView.onPause();
            }
        }

        /**
         * onSurfaceCreated - Se invoca cuando la superficie del wallpaper está disponible.
         * Le notifica a GLSurfaceView que la superficie ha sido creada para asociar el SurfaceHolder.
         *
         * @param holder - SurfaceHolder donde se dibujará OpenGL.
         *
         * ASCII Art:
         *   ╔════════════════════════════════════════════════╗
         *   ║  🖥️ onSurfaceCreated: Superficie lista para GL 🖥️ ║
         *   ╚════════════════════════════════════════════════╝
         *           ____________
         *          |            |
         *          |  SURFACE   |  <-- Aquí conectamos GLSurfaceView
         *          |   READY    |
         *          |____________|
         */
        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            // 🔄 Redirige la creación de la superficie al GLSurfaceView.
            glSurfaceView.surfaceCreated(holder);
        }

        /**
         * onSurfaceChanged - Se invoca cuando la superficie cambia de tamaño o formato.
         * Actualiza el GLSurfaceView con las nuevas dimensiones.
         *
         * @param holder - SurfaceHolder de la superficie.
         * @param format - Formato de píxel (por ejemplo, PixelFormat.RGBA_8888).
         * @param width  - Nuevo ancho en píxeles.
         * @param height - Nuevo alto en píxeles.
         *
         * ASCII Art:
         *   ╔════════════════════════════════════════════════╗
         *   ║  🔄 onSurfaceChanged: Ajustando dimensiones 🔄 ║
         *   ╚════════════════════════════════════════════════╝
         *      ┌─────┬────────────────┐
         *      │ Wid │ Height changed │
         *      └─────┴────────────────┘
         */
        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            // 🔄 Notifica a GLSurfaceView del cambio de tamaño/formato.
            glSurfaceView.surfaceChanged(holder, format, width, height);
        }

        /**
         * onSurfaceDestroyed - Se invoca cuando la superficie del wallpaper se destruye.
         * Indica a GLSurfaceView que libere recursos relacionados con la superficie.
         *
         * @param holder - SurfaceHolder que ya no existe.
         *
         * ASCII Art:
         *   ╔════════════════════════════════════════════════╗
         *   ║  🗑️ onSurfaceDestroyed: Liberando recursos 🗑️  ║
         *   ╚════════════════════════════════════════════════╝
         *         _______          _______
         *        |       |🗑️     |       |🗑️
         *        |  OLD  | ------>|  NEW  |
         *        |_______|        |_______|
         */
        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            // 🗑️ Delegamos la destrucción a GLSurfaceView para que limpie.
            glSurfaceView.surfaceDestroyed(holder);
        }

        /**
         * onDestroy - Se llama cuando el motor del wallpaper se destruye permanentemente.
         * Limpia la vista GLSurfaceView y luego pasa el control al método padre de Engine.
         *
         * ASCII Art:
         *   ╔════════════════════════════════════════════════════════╗
         *   ║  💀 onDestroy: Destruyendo motor y GLSurfaceView 💀  ║
         *   ╚════════════════════════════════════════════════════════╝
         *           ┌────────────────┐          ┌────────────────┐
         *           │   GLSurface    │   ->     │   Destroyed    │
         *           │     View       │          │    (limpio)    │
         *           └────────────────┘          └────────────────┘
         */
        @Override
        public void onDestroy() {
            // 🔒 Limpia/desvincula la vista OpenGL del motor.
            glSurfaceView.onDestroy();
            // 👉 Llamada a la implementación base de Engine.
            super.onDestroy();
        }

        /*
        ╔════════════════════════════════════════════════════════════════════════╗
        ║                                                                        ║
        ║   🔰🔰  GLWallpaperSurfaceView - Extiende GLSurfaceView para Wallpaper  🔰🔰  ║
        ║                                                                        ║
        ║   • Redirige getHolder() para usar SurfaceHolder del WallpaperService  ║
        ║   • Permite dibujar directamente en la superficie del wallpaper        ║
        ║   • Contiene método onDestroy() para limpiar la vista cuando sea       ║
        ║     destruida                                                              ║
        ║                                                                        ║
        ╚════════════════════════════════════════════════════════════════════════╝
        */
        private class GLWallpaperSurfaceView extends GLSurfaceView {

            /**
             * Constructor GLWallpaperSurfaceView - Inicia la vista GLSurfaceView.
             *
             * @param context - Contexto de la aplicación o servicio.
             */
            public GLWallpaperSurfaceView(Context context) {
                super(context);
            }

            /**
             * getHolder - Devuelve el SurfaceHolder del WallpaperService en lugar del de GLSurfaceView.
             * Esto hace que GLSurfaceView dibuje directamente sobre la superficie gestionada
             * por WallpaperService en lugar de una SurfaceView separada.
             *
             * ASCII Art:
             *   ╔════════════════════════════════════════════════════════════════╗
             *   ║   🎯  getHolder: Retorna SurfaceHolder del Wallpaper  🎯       ║
             *   ╚════════════════════════════════════════════════════════════════╝
             *     ┌───────────────────┐      ┌───────────────────┐
             *     │ GLSurfaceView SH  │  <=  │ Wallpaper SH      │
             *     └───────────────────┘      └───────────────────┘
             */
            @Override
            public SurfaceHolder getHolder() {
                return getSurfaceHolder();
            }

            /**
             * onDestroy - Limpia la vista GLSurfaceView cuando el motor finaliza.
             * Se llama al método super.onDetachedFromWindow() para liberar recursos.
             *
             * ASCII Art:
             *   ╔════════════════════════════════════════════════════════════════╗
             *   ║   🧹 onDestroy: Limpiando GLSurfaceView (detached) 🧹           ║
             *   ╚════════════════════════════════════════════════════════════════╝
             *              .-""""-.
             *             /        \
             *            |   RIP    |   <--- GLSurfaceView vivo hasta ahora
             *             \        /
             *              '------'    <--- Liberado de la ventana
             */
            void onDestroy() {
                super.onDetachedFromWindow();
            }
        }
    }
}
