package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import java.util.ArrayList;
import java.util.List;

/*
╔════════════════════════════════════════════════════════════════════════╗
║                                                                        ║
║   🌌🌠  SceneRenderer.java - Renderizador Principal de la Escena  🌠🌌   ║
║                                                                        ║
║   ASCII Art:                                                           ║
║            .-~~~~~~~~~-._       🪐       .-~~~~~~~~~-._                  ║
║        __.'              ~.   🌟   .~              `.__              ║
║      .'//                  \\\\       ////                  \\\\.          ║
║     '///                     \\\\   ////                     \\\\\\         ║
║    ///      𝙍𝙚𝙣𝙙𝙚𝙧 𝙈𝙪𝙣𝙙𝙤 𝙈𝙖𝙜𝙞𝙘𝙤     \\\\ ////     𝙎𝙚𝙡𝙚𝙘𝙩𝙤       \\\\\        ║
║    |||                        |||   |||                        |||       ║
║    \\\\                         //// ////                         ////       ║
║     \\\\\\                     //// ////                     ////          ║
║      `'\\\                🌌   |||   🌌                ////'`            ║
║          `~-.______________🌠🌠🌠_______________.-~`                    ║
║                                                                        ║
║   Descripción General:                                                 ║
║   • Clase encargada de inicializar OpenGL ES y gestionar el ciclo de   ║
║     vida del renderizado (creación, cambio de superficie, dibujo       ║
║     continuo, pausa y reanudación).                                     ║
║   • Mantiene una lista de SceneObject (estrellas, nebulosas, túnel)    ║
║     que se actualizan y dibujan cada frame.                             ║
║   • Utiliza TextureManager para cargar texturas de manera segura.       ║
║                                                                        ║
╚════════════════════════════════════════════════════════════════════════╝
*/

/**
 * SceneRenderer - Clase principal que renderiza todos los objetos de la escena con OpenGL ES 2.0.
 *
 * 📝 Propósito Principal:
 *   - Gestionar la inicialización de OpenGL (configuración de blend, clear color, etc.).
 *   - Mantener y dibujar múltiples SceneObject (por ejemplo StarField, StarTunnelBackground).
 *   - Manejar la carga de texturas a través de TextureManager.
 *   - Controlar la pausa/reanudación del renderizado cuando el wallpaper
 *     pierde/gana visibilidad.
 *
 * 📚 Buenas Prácticas:
 *   - Separar la inicialización de texturas (onSurfaceChanged) para evitar pérdidas de contexto.
 *   - Usar System.nanoTime() para calcular deltaTime preciso.
 *   - No bloquear el hilo de renderizado: actualizar y dibujar rápidamente.
 */
public class SceneRenderer implements android.opengl.GLSurfaceView.Renderer {

    // 🌐 Contexto de la aplicación para acceder a recursos, assets, etc.
    private final Context context;

    /**
     * 📱 screenWidth, screenHeight - Dimensiones actuales de la ventana de renderizado.
     * Se actualizan en onSurfaceChanged para que los objetos puedan conocer la resolución.
     */
    public static int screenWidth = 1;
    public static int screenHeight = 1;

    // ⏸️ paused - Indicador de si el renderizado está pausado.
    private boolean paused = false;

    // 🌌 isWallpaper - Flag opcional para distinguir entre preview en actividad o wallpaper real.
    public boolean isWallpaper = false;

    /**
     * 🎨 sceneObjects - Lista de objetos de la escena (implementan SceneObject).
     * Cada objeto se actualiza y dibuja en onDrawFrame.
     */
    private final List<SceneObject> sceneObjects = new ArrayList<>();

    // ⏱️ lastTime - Marca de tiempo en nanosegundos de la última llamada a onDrawFrame.
    private long lastTime = System.nanoTime();

    // 🧱 textureManager - Maneja la carga y liberación de texturas OpenGL.
    private TextureManager textureManager;


    /**
     * Constructor SceneRenderer - Recibe el contexto de la aplicación.
     * @param context - Contexto necesario para inicializar TextureManager y otros recursos.
     *
     * ASCII Art:
     *   ╔═════════════════════════════════╗
     *   ║  ⚙️  SceneRenderer Inicializado  ⚙️ ║
     *   ╚═════════════════════════════════╝
     *        /\\         🌠       /\\
     *       /  \\  new SceneRenderer /  \\
     *      /____\\       🚀       /____\\
     */
    public SceneRenderer(Context context) {
        this.context = context;
    }

    /**
     * pause - Pausa el renderizado de la escena. next onDrawFrame no hará nada.
     *
     * ASCII Art:
     *   ╔═════════════════════════════════╗
     *   ║      ⏸️  Pausando Escena  ⏸️      ║
     *   ╚═════════════════════════════════╝
     */
    public void pause() {
        paused = true;
    }

    /**
     * resume - Reanuda el renderizado de la escena. onDrawFrame volverá a procesar.
     *
     * ASCII Art:
     *   ╔═════════════════════════════════╗
     *   ║      ▶️  Reanudando Escena  ▶️    ║
     *   ╚═════════════════════════════════╝
     */
    public void resume() {
        paused = false;
    }

    /**
     * addObject - Agrega un SceneObject a la lista de renderizado.
     * @param object - Instancia de SceneObject (StarField, StarTunnelBackground, etc.).
     *
     * ASCII Art:
     *   ╔═════════════════════════════════╗
     *   ║  ➕  Objeto añadido a la escena  ➕  ║
     *   ╚═════════════════════════════════╝
     *          .-""""-.
     *         /  🚀   \
     *        |  Scene   |
     *         \Object /
     *          '----'
     */
    public void addObject(SceneObject object) {
        sceneObjects.add(object);
    }

    /**
     * onSurfaceCreated - Se invoca al crear la superficie OpenGL.
     * Inicializa configuraciones globales de OpenGL (blend, clear color),
     * libera recursos de shaders previos y crea el TextureManager.
     *
     * @param gl     - Objeto GL10 (heredado de OpenGL ES 1.x, no usado aquí).
     * @param config - Configuración EGL actual (información de configuración).
     *
     * ASCII Art:
     *   ╔════════════════════════════════════════════════╗
     *   ║   🎨  onSurfaceCreated: Setup OpenGL Global 🎨   ║
     *   ╚════════════════════════════════════════════════╝
     *     ┌──────────────────────────────────────────┐
     *     │ ▪ Habilitar BLEND para transparencia     │
     *     │ ▪ Deshabilitar DEPTH_TEST para efectos   │
     *     │ ▪ Establecer CLEAR COLOR a negro puro    │
     *     │ ▪ Liberar shaders antiguos (Star.release) │
     *     │ ▪ Instanciar TextureManager               │
     *     │ ▪ Vaciar lista de sceneObjects           │
     *     └──────────────────────────────────────────┘
     */
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Habilitar fusión alfa para transparencia en texturas
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Deshabilitar prueba de profundidad: dibujamos en 2D/planar
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        // Establecer color de borrado: negro opaco
        GLES20.glClearColor(0f, 0f, 0f, 1f);

        // Liberar recursos de la clase Star (texturas/shaders) si existían
        Star.release();

        // Instanciar TextureManager para cargar texturas más adelante
        textureManager = new TextureManager(context); // Solo instanciamos aquí, inicialización en onSurfaceChanged

        // Limpiar cualquier objeto previo en la escena
        sceneObjects.clear();

        // NOTA: No agregamos aún objetos a la escena; se hace en onSurfaceChanged
        Log.d("SceneRenderer", "🛸 Texturas inicializadas: " + textureManager.isInitialized());
        Log.d("SceneRenderer", "🧱 Objetos en escena: " + sceneObjects.size());
    }

    /**
     * onSurfaceChanged - Se invoca cuando la superficie cambia de tamaño o formato.
     * Ajusta la vista de recorte (viewport), actualiza dimensiones globales,
     * inicializa el TextureManager (carga real de texturas) y prepara los objetos de la escena.
     *
     * @param gl     - Objeto GL10 (no usado directamente).
     * @param width  - Nuevo ancho de la superficie en píxeles.
     * @param height - Nuevo alto de la superficie en píxeles.
     *
     * ASCII Art:
     *   ╔════════════════════════════════════════════════╗
     *   ║   🔄 onSurfaceChanged: Ajuste de Dimensiones 🔄  ║
     *   ╚════════════════════════════════════════════════╝
     *     ┌──────────────────────────────────────────┐
     *     │ ▪ glViewport(0,0,width,height)           │
     *     │ ▪ screenWidth = width                    │
     *     │ ▪ screenHeight = height                  │
     *     │ ▪ Inicializar TextureManager.realmente    │
     *     │ ▪ sceneObjects.clear()                   │
     *     │ ▪ prepareScene()                         │
     *     └──────────────────────────────────────────┘
     */
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        // Ajustar el viewport al nuevo tamaño de la superficie
        GLES20.glViewport(0, 0, width, height);

        // Actualizar variables estáticas de resolución
        screenWidth = width;
        screenHeight = height;

        Log.d("SceneRenderer", "🧭 onSurfaceChanged: tamaño = " + width + "x" + height);

        // Instanciar y cargar texturas en el momento seguro (superficie lista)
        textureManager = new TextureManager(context);
        textureManager.initialize(); // Carga física de texturas en GPU

        // Limpiar cualquier objeto de la escena previa
        sceneObjects.clear();

        // Preparar y agregar objetos a la escena (StarTunnelBackground, StarField, etc.)
        prepareScene();
    }

    /**
     * onDrawFrame - Se invoca para cada frame de renderizado.
     * Si no está pausado, limpia la pantalla y actualiza/dibuja cada SceneObject.
     *
     * @param gl - Objeto GL10 (no utilizado directamente).
     *
     * ASCII Art:
     *   ╔════════════════════════════════════════════════╗
     *   ║       🔄 onDrawFrame: Ciclo de Renderizado 🔄    ║
     *   ╚════════════════════════════════════════════════╝
     *      ┌──────────────────────────────────────────┐
     *      │ ▪ Si paused = true, no hacer nada         │
     *      │ ▪ GLES20.glClear(...) limpia pantalla     │
     *      │ ▪ Calcular deltaTime (tiempo transcurrido)│
     *      │ ▪ Para cada SceneObject:                  │
     *      │     • object.update(deltaTime)            │
     *      │     • object.draw()                       │
     *      └──────────────────────────────────────────┘
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        // Si está pausado, salimos sin renderizar
        if (paused) return;

        // Limpiar color y buffer de profundidad antes de dibujar
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Calcular tiempo transcurrido desde el último frame (en segundos)
        long currentTime = System.nanoTime();
        float deltaTime = (currentTime - lastTime) / 1_000_000_000f;
        lastTime = currentTime;

        // Actualizar y dibujar cada objeto de la escena
        for (SceneObject object : sceneObjects) {
            object.update(deltaTime);
            object.draw();
        }
    }

    /**
     * prepareScene - Crea y agrega objetos a la escena según disponibilidad de texturas.
     * Verifica si TextureManager inicializó correctamente; si no, usa modo sin textura.
     *
     * ASCII Art:
     *   ╔════════════════════════════════════════════════════════════════╗
     *   ║       🎬 prepareScene: Creación de Objetos en Escena       ║
     *   ╚════════════════════════════════════════════════════════════════╝
     *     ┌────────────────────────────────────────────────────────┐
     *     │ ▪ if textureManager.initialize() == true:              │
     *     │      • Agregar StarTunnelBackground()                  │
     *     │      • Agregar StarField(textureManager, cantidad)     │
     *     │   else:                                                │
     *     │      • Agregar StarTunnelBackground()                  │
     *     │      • Agregar StarField(null, cantidad) (sin textura) │
     *     │ ▪ Log de objetos creados                               │
     *     └────────────────────────────────────────────────────────┘
     */
    private void prepareScene() {
        // Intentar inicializar texturas verdaderamente; retorna true si tuvo éxito
        if (textureManager != null && textureManager.initialize()) {
            Log.d("SceneRenderer", "🎨 Texturas listas, creando objetos con textura...");
            sceneObjects.add(new StarTunnelBackground());
            sceneObjects.add(new StarField(textureManager, 5));
        } else {
            Log.w("SceneRenderer", "🚫 No se pudieron inicializar texturas. Usando modo sin textura...");
            sceneObjects.add(new StarTunnelBackground());
            sceneObjects.add(new StarField(null, 5));
        }
        Log.d("SceneRenderer", "🎬 Objetos en escena: " + sceneObjects.size());
    }
}
