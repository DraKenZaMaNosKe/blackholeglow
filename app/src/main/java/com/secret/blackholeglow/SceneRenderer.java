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
    private static final String TAG = "SceneRenderer";

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

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig cfg) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glClearColor(0f, 0f, 0f, 1f);

        sharedCamera   = new CameraController();
        textureManager = new TextureManager(context);

        prepareScene();
        Log.d(TAG, "onSurfaceCreated → escena preparada");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        GLES20.glViewport(0, 0, w, h);
        screenWidth  = w;
        screenHeight = h;

        sharedCamera.updateProjection(w, h);
        sharedCamera.setView(
                0f, 0f, 6f,
                0f, 0f, 0f,
                0f, 1f, 0f
        );
        sharedCamera.startZoomLoop(20f, 10f);

        Log.d(TAG, "onSurfaceChanged to " + w + "x" + h);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (paused) return;

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        long now = System.nanoTime();
        float dt = (now - lastTime) / 1_000_000_000f;
        lastTime = now;

        // LOG de diagnóstico: dt y FPS
        float fps = dt > 0f ? 1f / dt : -1f;
        Log.d(TAG, String.format("Frame dt=%.4fs  FPS=%.1f", dt, fps));

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
        if ("Universo".equals(item_seleccinado)) {
            sceneObjects.add(new UniverseBackground(context, textureManager));

            // Sol central, giro propio 15°/s, 130% escala
            sceneObjects.add(new Planeta(
                    context, textureManager,
                    0f,   // orbitRadiusX
                    0f,   // orbitRadiusZ
                    0f,   // orbitSpeed
                    0f,   // scaleAmplitude
                    20.3f, // instanceScale
                    15f   // spinSpeed
            ));

            // Planeta A (órbita rápida)
            sceneObjects.add(new Planeta(
                    context, textureManager,
                    0.3f, // orbitRadiusX
                    1.8f, // orbitRadiusZ
                    0.3f, // orbitSpeed
                    0.2f, // scaleAmplitude
                    3.0f, // instanceScale
                    30f   // spinSpeed
            ));
        } else if ("Agujero Negro".equals(item_seleccinado)) {
            BlenderCubeBackground bg = new BlenderCubeBackground(context, textureManager);
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
        this.item_seleccinado = item;
        // Para cambiar en caliente:
        // sceneObjects.clear();
        // prepareScene();
    }
}
