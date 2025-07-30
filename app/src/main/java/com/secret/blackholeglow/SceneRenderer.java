// ============================================================
// SceneRenderer.java
// ============================================================
package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * ============================================================================
 * SceneRenderer
 * ============================================================================
 * Renderer principal que gestiona:
 *  1. El bucle de renderizado (callbacks GLSurfaceView.Renderer).
 *  2. Configuración de cámara y viewport.
 *  3. Carga y actualización de objetos de escena.
 *  4. Control de pausa/reanudación.
 *
 * Variables globales:
 *  - screenWidth  : Ancho del viewport GL en píxeles. Rango mínimo: 1.
 *  - screenHeight : Alto del viewport GL en píxeles. Rango mínimo: 1.
 *
 * Uso típico:
 *  SceneRenderer renderer = new SceneRenderer(context, "Universo");
 *  glSurfaceView.setRenderer(renderer);
 */
public class SceneRenderer implements GLSurfaceView.Renderer {

    /** Tag para logs de depuración */
    private static final String TAG = "SceneRenderer";

    /** Ancho actual del viewport OpenGL (en píxeles). Inicializado en 1 para evitar división por cero. */
    public static int screenWidth  = 1;
    /** Alto actual del viewport OpenGL (en píxeles). Inicializado en 1 para evitar división por cero. */
    public static int screenHeight = 1;

    /**
     * Contexto de Android para cargar recursos (texturas, etc.). No debe ser nulo.
     */
    private final Context context;
    /**
     * Ítem de la escena a renderizar (por ejemplo, "Universo" o "Agujero Negro").
     */
    private String selectedItem;
    /**
     * Indica si el bucle de render está pausado.
     * true = pausa render (onDrawFrame no hace nada).
     */
    private boolean paused = false;

    /**
     * Lista de objetos de la escena que implementan SceneObject.
     * Se itera en onDrawFrame para update() y draw().
     */
    private final List<SceneObject> sceneObjects = new ArrayList<>();

    /**
     * Tiempo del último frame en nanosegundos (System.nanoTime()).
     * Se usa para calcular delta time (dt).
     */
    private long lastTime = System.nanoTime();

    /**
     * Controlador de cámara compartido entre objetos (posicion, proyección).
     */
    private CameraController sharedCamera;
    /**
     * Gestor de texturas para cargar y almacenar texturas OpenGL.
     */
    private TextureManager textureManager;

    /**
     * Constructor.
     *
     * @param ctx         Contexto de Android (no nulo).
     * @param initialItem Nombre del ítem inicial a renderizar ("Universo", etc.).
     */
    public SceneRenderer(Context ctx, String initialItem) {
        this.context      = ctx;
        this.selectedItem = initialItem;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig cfg) {
        // Habilitar Z-buffer
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        // Configurar blending alpha
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        // Color de fondo: negro opaco
        GLES20.glClearColor(0f, 0f, 0f, 1f);

        // Inicializar cámara y texturas
        sharedCamera   = new CameraController();
        textureManager = new TextureManager(context);

        // Preparar escena según selectedItem
        prepareScene();
        Log.d(TAG, "Surface created → escena preparada");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        // Ajustar viewport
        GLES20.glViewport(0, 0, w, h);
        screenWidth  = w;
        screenHeight = h;

        // Actualizar proyección y vista de la cámara
        sharedCamera.updateProjection(w, h);
        sharedCamera.setView(
                0f, 0f, 6f,   // eye: x,y,z
                0f, 0f, 0f,   // center: x,y,z
                0f, 1f, 0f    // up vector
        );
        // Iniciar zoom automático: rango [10..20]
        sharedCamera.startZoomLoop(20f, 10f);

        Log.d(TAG, "Viewport cambiado a " + w + "x" + h);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (paused) return;

        // Limpiar buffers
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Calcular delta time (dt) en segundos
        long now = System.nanoTime();
        float dt = (now - lastTime) / 1_000_000_000f; // típico [0.001..0.033]
        lastTime = now;

        // Actualizar cámara
        sharedCamera.update(dt);

        // Recorrer todos los objetos: update() y draw()
        for (SceneObject obj : sceneObjects) {
            obj.update(dt);
            obj.draw();
        }
    }

    /**
     * Inicializa Textures y crea los objetos de escena según selectedItem.
     * ``Universo``: UniverseBackground + varios Planeta.
     * ``Agujero Negro``: DeformableCubeBackground.
     */
    private void prepareScene() {
        if (!textureManager.initialize()) {
            Log.e(TAG, "No se pudo inicializar TextureManager");
            return;
        }
        sceneObjects.clear();

        if ("Universo".equals(selectedItem)) {
            sceneObjects.add(new UniverseBackground(context, textureManager));

            // Planeta con textura, sin oscilación de escala (scaleOscPercent=null)
            sceneObjects.add(new Planeta(
                    context,
                    textureManager,
                    0f, 0f,     // orbitRadiusX, orbitRadiusZ
                    0f,         // orbitSpeed
                    0f,         // scaleAmplitude
                    15.3f,      // instanceScale (base) [0.1..5]
                    10f,        // spinSpeed en °/s [0..360]
                    false,      // useSolidColor
                    null,       // solidColor (RGBA) o null para blanco
                    1.0f,       // alpha [0..1]
                    null        // scaleOscPercent [0..1) o null
            ));
            sceneObjects.add(new Planeta(
                    context,
                    textureManager,
                    0f, 0f,     // orbitRadiusX, orbitRadiusZ
                    0f,         // orbitSpeed
                    0f,         // scaleAmplitude
                    20.3f,      // instanceScale (base) [0.1..5]
                    10f,        // spinSpeed en °/s [0..360]
                    true,      // useSolidColor
                    new float[]{1.0f, 0.25f, 0.08f, 0f},       // solidColor (RGBA) o null para blanco
                    0.0f,       // alpha [0..1]
                    1.05f        // scaleOscPercent [0..1) o null
            ));
            sceneObjects.add(new Planeta(
                    context,
                    textureManager,
                    0.3f, 0.3f,     // orbitRadiusX, orbitRadiusZ
                    0.77f,         // orbitSpeed
                    0f,         // scaleAmplitude
                    4f,      // instanceScale (base) [0.1..5]
                    10f,        // spinSpeed en °/s [0..360]
                    false,      // useSolidColor
                    null,       // solidColor (RGBA) o null para blanco
                    1.0f,       // alpha [0..1]
                    null        // scaleOscPercent [0..1) o null
            ));
            // ... agregar más planetas según necesidad ...
        }
        else if ("Agujero Negro".equals(selectedItem)) {
            DeformableCubeBackground bg = new DeformableCubeBackground(context, textureManager);
            if (bg instanceof CameraAware) {
                ((CameraAware) bg).setCameraController(sharedCamera);
            }
            sceneObjects.add(bg);
        }
    }

    /** Pausa el render loop. */
    public void pause() { paused = true; }
    /** Reanuda el render loop y reinicia el temporizador. */
    public void resume() { paused = false; lastTime = System.nanoTime(); }

    /** Ajusta yaw de cámara en grados. */
    public void adjustYaw(float delta) {
        if (sharedCamera != null) sharedCamera.addOrbitOffset(delta);
    }

    /** Cambia el ítem seleccionado y (opc.) recarga escena. */
    public void setSelectedItem(String item) {
        this.selectedItem = item;
        // Para recarga en caliente:
        // sceneObjects.clear();
        // prepareScene();
    }
}
