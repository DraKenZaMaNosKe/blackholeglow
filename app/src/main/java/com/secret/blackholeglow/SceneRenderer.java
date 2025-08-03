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
 * ====================================================================
 * SceneRenderer
 * ====================================================================
 * Renderer principal que gestiona:
 *  1. El bucle de renderizado (callbacks GLSurfaceView.Renderer).
 *  2. Configuración de cámara y viewport.
 *  3. Carga y actualización de objetos de escena.
 *  4. Control de pausa/reanudación.
 */
public class SceneRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "SceneRenderer";
    public static int screenWidth  = 1;
    public static int screenHeight = 1;

    private final Context context;
    private String selectedItem;
    private boolean paused = false;
    private final List<SceneObject> sceneObjects = new ArrayList<>();
    private long lastTime = System.nanoTime();
    private CameraController sharedCamera;
    private TextureManager textureManager;

    public SceneRenderer(Context ctx, String initialItem) {
        this.context      = ctx;
        this.selectedItem = initialItem;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig cfg) {
        // Habilitar Z-buffer
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        // Habilitar blending para transparencias
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(
                GLES20.GL_SRC_ALPHA,
                GLES20.GL_ONE_MINUS_SRC_ALPHA
        );
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
        GLES20.glViewport(0, 0, w, h);
        screenWidth  = w;
        screenHeight = h;

        sharedCamera.updateProjection(w, h);
        sharedCamera.setView(
                0f,0f,6f,   // eye
                0f,0f,0f,   // center
                0f,1f,0f    // up
        );
        sharedCamera.startZoomLoop(20f, 10f);

        Log.d(TAG, "Viewport cambiado a " + w + "x" + h);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (paused) return;

        GLES20.glClear(
                GLES20.GL_COLOR_BUFFER_BIT |
                        GLES20.GL_DEPTH_BUFFER_BIT
        );
        long now = System.nanoTime();
        float dt = (now - lastTime) / 1_000_000_000f;
        lastTime = now;

        sharedCamera.update(dt);

        for (SceneObject obj : sceneObjects) {
            obj.update(dt);
            obj.draw();
        }
    }

    private void prepareScene() {
        if (!textureManager.initialize()) {
            Log.e(TAG, "No se pudo inicializar TextureManager");
            return;
        }
        sceneObjects.clear();

        if ("Universo".equals(selectedItem)) {
            sceneObjects.add(new UniverseBackground(context, textureManager));

            // Ejemplo: un planeta texturizado
            sceneObjects.add(new Planeta(
                    context,
                    textureManager,
                    "shaders/planeta_texture_vertex.glsl",
                    "shaders/planeta_texture_fragment.glsl",
                    R.drawable.textura_sol,
                    0f,0f,      // orbitRadiusX, orbitRadiusZ
                    0f,         // orbitSpeed
                    0f,         // scaleAmplitude
                    8.0f,      // instanceScale
                    9f,        // spinSpeed
                    false,      // useSolidColor
                    null,       // solidColor
                    1.0f,       // alpha
                    null,       // scaleOscPercent
                    8.0f        // uvScale
            ));
            sceneObjects.add(new Planeta(
                    context,
                    textureManager,
                    "shaders/planeta_vertex.glsl",
                    "shaders/planeta_fragment.glsl",
                    R.drawable.textura_sol,
                    0f,0f,      // orbitRadiusX, orbitRadiusZ
                    3f,         // orbitSpeed
                    0f,         // scaleAmplitude
                    11.0f,      // instanceScale
                    1.5f,        // spinSpeed
                    true,      // useSolidColor
                    new float[]{0.3f,0.0f,0.0f,0.8f},       // solidColor
                    0.3f,       // alpha
                    0.8f,       // scaleOscPercent
                    8.0f        // uvScale
            ));

            sceneObjects.add(new Planeta(
                    context,
                    textureManager,
                    "shaders/planeta_vertex.glsl",
                    "shaders/planeta_fragment.glsl",
                    R.drawable.textura_roninplaneta,
                    /* orbitRadiusX */ 0.18f,    // eje X de la elipse [0..20]
                    /* orbitRadiusZ */ 0.15f,    // eje Z de la elipse [0..20]
                    /* orbitSpeed   */ 0.25f,    // velocidad rad/s [0..6.28]
                    /* scaleAmplitude */ 0.3f,  // pulsos de escala
                    /* instanceScale  */ 2.0f,    // escala base
                    /* spinSpeed      */ 25.0f,   // giro propio
                    false,                       // useSolidColor
                    null,                        // solidColor
                    1.0f,                        // alpha
                    null,                       // scaleOscPercent
                    /* uvScale        */ 1.0f   // tiling de UV
            ));
            sceneObjects.add(new Asteroide(
                    context,
                    textureManager,
                    "shaders/asteroide_vertex.glsl",
                    "shaders/asteroide_fragment.glsl",
                    R.drawable.textura_asteroide, // textura
                    0.7f,                          // instanceScale (tamaño base)
                    false,                         // useSolidColor
                    null,                          // solidColor (se ignora si useSolidColor=false)
                    1.0f,                          // alpha
                    2.0f                           // uvScale (tiling UV)
            ));

        }
        else if ("Agujero Negro".equals(selectedItem)) {
            DeformableCubeBackground bg =
                    new DeformableCubeBackground(context, textureManager);
            if (bg instanceof CameraAware) {
                ((CameraAware) bg).setCameraController(sharedCamera);
            }
            sceneObjects.add(bg);
        }
    }

    public void pause()  { paused = true; }
    public void resume() { paused = false; lastTime = System.nanoTime(); }
    public void adjustYaw(float delta) {
        if (sharedCamera != null) sharedCamera.addOrbitOffset(delta);
    }
    public void setSelectedItem(String item) {
        this.selectedItem = item;
    }
}
