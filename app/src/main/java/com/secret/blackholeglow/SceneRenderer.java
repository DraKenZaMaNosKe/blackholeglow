// SceneRenderer.java
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
 * Este Renderer controla:
 * 1. El bucle de renderizado (onDrawFrame).
 * 2. La cámara (sin zoom loop, con head-sway).
 * 3. El viewport/proyección al cambiar tamaño.
 * 4. Carga y dibujado de los SceneObject (planeta, asteroide, fondo).
 */
public class    SceneRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "SceneRenderer";
    public static int screenWidth = 1, screenHeight = 1;

    private final Context context;
    private String selectedItem;
    private boolean paused = false;
    private final List<SceneObject> sceneObjects = new ArrayList<>();
    private long lastTime = System.nanoTime();
    private CameraController sharedCamera;
    private TextureManager textureManager;

    public SceneRenderer(Context ctx, String initialItem) {
        this.context = ctx;
        this.selectedItem = initialItem;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig cfg) {
        // Depth + blending
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glClearColor(0f, 0f, 0f, 1f);

        sharedCamera = new CameraController();
        textureManager = new TextureManager(context);

        prepareScene();
        Log.d(TAG, "Surface created → scene ready");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        GLES20.glViewport(0, 0, w, h);
        screenWidth = w;
        screenHeight = h;

        // Proyección de cámara
        sharedCamera.updateProjection(w, h);
        // Vista fija: ojo en (0,0,6), mirando al origen

        Log.d(TAG, String.format("Viewport changed to %dx%d", w, h));
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (paused) return;
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Tiempo delta
        long now = System.nanoTime();


        // 🔧 Calculamos delta time pero limitamos dt máximo (por ejemplo, 0.1s)
        float dt = Math.min((now - lastTime) / 1e9f, 0.1f);
        lastTime = now;


        sharedCamera.update(dt);

        // Actualiza y dibuja cada objeto de la escena
        for (SceneObject obj : sceneObjects) {
            obj.update(dt);
            obj.draw();
        }
    }

    private void prepareScene() {
        if (!textureManager.initialize()) {
            Log.e(TAG, "Failed to init textures");
            return;
        }
        sceneObjects.clear();

        if ("Universo".equals(selectedItem)) {

            /*UniverseBackground universofondo = new UniverseBackground(
                    context, textureManager,
                    "shaders/universe_vertex.glsl",
                    "shaders/universe_fragment.glsl",
                    R.drawable.fondo_universo_cosmico,
                    1.0f
            );

            if (universofondo instanceof CameraAware) {
                ((CameraAware) universofondo).setCameraController(sharedCamera);
                sceneObjects.add(universofondo);
            }*/

            UniverseBackground fondo_transparente = new UniverseBackground(
                    context, textureManager,
                    "shaders/black_vertex.glsl",
                    "shaders/black_fragment.glsl",
                    null,
                    1.0f
            );

            if (fondo_transparente instanceof CameraAware) {
                ((CameraAware) fondo_transparente).setCameraController(sharedCamera);
                sceneObjects.add(fondo_transparente);
            }
                        /*
            // 2) Agregar un planeta
            //    Parámetros:
            //      - vertex shader: "shaders/planeta-vertex.glsl"
            //      - fragment shader: "shaders/planeta-fragment.glsl"
            //      - textura: R.drawable.textura_mi_planeta
            //      - orbitRadiusX, orbitRadiusZ, orbitSpeed en rad/s
            //      - scaleAmplitude, instanceScale, spinSpeed en grados/s
            //      - useSolidColor, solidColor[], alpha (transparencia)
            //      - scaleOscPercent (puede ser null), uvScale
            sceneObjects.add(new Planeta(
                    context,
                    textureManager,
                    "shaders/planeta_vertex.glsl",    // vertex shader
                    "shaders/planeta_fragment.glsl",  // fragment shader
                    R.drawable.textura_sol,           // textura
                    0.0f,  // orbitRadiusX (m)
                    0.0f,  // orbitRadiusZ (m)
                    0.0f,  // orbitSpeed (rad/s)
                    0.0f,  // scaleAmplitude (coef)
                    7.0f,  // instanceScale (escala base)
                    4.0f,  // spinSpeed (deg/s), si quieres girar
                    false, // useSolidColor
                    null,  // solidColor
                    1.0f,  // alpha
                    null,  // scaleOscPercent → null desactiva oscilación
                    8.0f   // uvScale
            ));

            // 3) Planeta GLOW (misma posición que el central)
            //
            // - orbitRadiusX/Z = 0 → fija
            // - scaleAmplitude > 0 → controla cuánto varia el brillo
            // - scaleOscPercent != null → porcentaje del pulso de escala
            sceneObjects.add(new Planeta(
                    context,
                    textureManager,
                    "shaders/planeta_vertex.glsl",
                    "shaders/planeta_fragment.glsl",
                    R.drawable.textura_sol,
                    0.0f,
                    0.0f,
                    0.0f,
                    0.2f,     // pulso suave
                    8.5f,     // ligeramente mayor que el central
                    30.0f,
                    true,
                    new float[]{1f, 0.2f, 0.1f, 0.3f}, // tono glow
                    0.3f,
                    1.1f,     // 30% de oscilación
                    1.0f
            ));

            // 4) Planeta en órbita elíptica
            //
            // - orbitRadiusX/Z > 0 → órbita
            // - orbitSpeed   > 0 → velocidad angular
            // - scaleAmplitude > 0 → escala varía con la posición orbital (dinámico)
            // - scaleOscPercent = null → sin pulso adicional
            sceneObjects.add(new Planeta(
                    context,
                    textureManager,
                    "shaders/planeta_vertex.glsl",
                    "shaders/planeta_fragment.glsl",
                    R.drawable.textura_roninplaneta,
                    0.18f,  // orbitRadiusX
                    0.15f,  // orbitRadiusZ
                    0.3f,   // orbitSpeed
                    0.3f,   // scaleAmplitude dinámico
                    2.0f,   // instanceScale
                    25f,    // spinSpeed
                    false,
                    null,
                    1.0f,
                    null,   // no pulso extra
                    1.0f
            ));

            // 5) Asteroide (igual que antes)
            sceneObjects.add(new Asteroide(
                    context,
                    textureManager,
                    "shaders/asteroide_vertex.glsl",
                    "shaders/asteroide_fragment.glsl",
                    R.drawable.textura_asteroide,
                    0.1f, false, null, 1.0f, 2.0f
            ));
            */

        }
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
        lastTime = System.nanoTime();
        Log.d(TAG, "Render resumed → tiempo reiniciado");
    }

    // En SceneRenderer.java
    public void release() {
        for (SceneObject obj : sceneObjects) {
            if (obj instanceof UniverseBackground) {
                ((UniverseBackground) obj).release();
            }
            // Si tienes otros objetos con release(), agrégalos aquí
        }
    }

    public void setSelectedItem(String item) {
        this.selectedItem = item;
    }
}


