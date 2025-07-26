package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.util.ArrayList;
import java.util.List;

public class SceneRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "Depurando";

    private final Context context;
    private String item_seleccinado;
    public static int screenWidth = 1, screenHeight = 1;
    private boolean paused = false;
    private final List<SceneObject> sceneObjects = new ArrayList<>();
    private long lastTime = System.nanoTime();
    private CameraController sharedCamera;
    private TextureManager textureManager;

    public SceneRenderer(Context ctx, String initialItem) {
        this.context = ctx;
        this.item_seleccinado = initialItem;
    }

    /** Permite girar la cámara desde el service */
    public void adjustYaw(float deltaDegrees) {
        if (sharedCamera != null) {
            sharedCamera.addOrbitOffset(deltaDegrees);
        }
    }

    /** Llamado desde service al ocultar el wallpaper */
    public void pause() {
        paused = true;
    }

    /** Llamado desde service al mostrar el wallpaper */
    public void resume() {
        paused = false;
        // Reinicia el temporizador para evitar un dt gigantesco
        lastTime = System.nanoTime();
    }

    /** Cambia el fondo seleccionado y reconstruye la escena */
    public void setSelectedItem(String item) {
        this.item_seleccinado = item;
        sceneObjects.clear();
        prepareScene();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig cfg) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glClearColor(0f,0f,0f,1f);

        sharedCamera   = new CameraController();
        textureManager = new TextureManager(context);
        sceneObjects.clear();
        Log.d(TAG, "onSurfaceCreated");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        GLES20.glViewport(0, 0, w, h);
        screenWidth  = w;
        screenHeight = h;

        // Configura proyección y vista inicial
        sharedCamera.updateProjection(w, h);
        sharedCamera.setView(
                0f, 0f, 6f,   // eye
                0f, 0f, 0f,   // center
                0f, 1f, 0f    // up
        );
        sharedCamera.startZoomLoop(20f, 10f);
        //sharedCamera.disableZoomLoop();

        textureManager.initialize();
        sceneObjects.clear();
        prepareScene();

        Log.d(TAG, "onSurfaceChanged to " + w + "x" + h);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (paused) return;

        // Clear una sola vez
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        long now = System.nanoTime();
        float dt = (now - lastTime) / 1_000_000_000f;
        lastTime = now;

        sharedCamera.update(dt);

        for (SceneObject obj : sceneObjects) {
            Log.d(TAG, "Actualizando: " + obj.getClass().getSimpleName());
            obj.update(dt);
            Log.d(TAG, "Dibujando: " + obj.getClass().getSimpleName());
            obj.draw();
        }
    }

    private void prepareScene() {
        if (!textureManager.initialize()) {
            Log.e(TAG, "No se pudo inicializar TextureManager");
            return;
        }

        if ("Universo".equals(item_seleccinado)) {
            // Fondo
            sceneObjects.add(new UniverseBackground(context, textureManager));

            sceneObjects.add(new Planeta(
                    context, textureManager,
                    0f, 0f,        // sin órbita
                    0f,            // orbitSpeed
                    0f,            // scaleAmplitude
                    8.3f,          // instanceScale
                    15f            // spinSpeed: 15°/s en este caso
            ));
            sceneObjects.add(new Planeta(
                    context, textureManager,
                    0.2f, 0.1f, 2.0f, 0.2f, 1.0f, 30f  // spinSpeed=30°/s
            ));
        }
        else if ("Agujero Negro".equals(item_seleccinado)) {
            BlenderCubeBackground bg = new BlenderCubeBackground(context, textureManager);
            if (bg instanceof CameraAware) {
                ((CameraAware) bg).setCameraController(sharedCamera);
            }
            sceneObjects.add(bg);
        }
        // ... otros casos ...
    }
}
