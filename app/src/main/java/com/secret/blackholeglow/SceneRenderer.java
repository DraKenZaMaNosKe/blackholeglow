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

    private CameraController sharedCamera;
    private final Context context;
    public static int screenWidth = 1, screenHeight = 1;
    private boolean paused = false;
    private final List<SceneObject> sceneObjects = new ArrayList<>();
    private long lastTime = System.nanoTime();
    private TextureManager textureManager;
    private String item_seleccinado;

    public SceneRenderer(Context ctx, String item) {
        this.context = ctx;
        this.item_seleccinado = item;
    }

    public void adjustYaw(float deltaDegrees) {
        sharedCamera.addOrbitOffset(deltaDegrees);
    }

    public void pause() { paused = true; }
    public void resume() { paused = false; }

    public void setSelectedItem(String item) {
        this.item_seleccinado = item;
        sceneObjects.clear();
        prepareScene();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig cfg) {
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClearColor(0, 0, 0, 1);
        textureManager = new TextureManager(context);
        sharedCamera   = new CameraController();
        sceneObjects.clear();
        Log.d(TAG, "onSurfaceCreated");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        GLES20.glViewport(0, 0, w, h);
        screenWidth  = w;
        screenHeight = h;

        // Proyección
        sharedCamera.updateProjection(w, h);

        // Vista inicial + órbita
        sharedCamera.setView(
                0f, 0f, 6f,    // ojo X,Y,Z
                0f, 0f, 0f,    // centro
                0f, 1f, 0f     // up
        );
        sharedCamera.startOrbit(12f);

        // —— Aquí elegimos si queremos zoom o cámara fija ——
//      // MODO ZOOM (comportamiento original):
        //sharedCamera.startZoomLoop(20f, 10f);
//      // MODO FIJO (misma distancia, sólo órbita):
        sharedCamera.disableZoomLoop();

        textureManager.initialize();
        sceneObjects.clear();
        prepareScene();
        Log.d(TAG, "onSurfaceChanged to " + w + "x" + h);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (paused) return;

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        long now = System.nanoTime();
        float dt = (now - lastTime) / 1_000_000_000f;
        lastTime = now;

        sharedCamera.update(dt);
        Log.d(TAG, "onDrawFrame dt=" + dt);

        for (SceneObject obj : sceneObjects) {
            Log.d(TAG, "Actualizando objeto: " + obj.getClass().getSimpleName());
            obj.update(dt);
            Log.d(TAG, "Dibujando objeto: " + obj.getClass().getSimpleName());
            obj.draw();
        }
    }

    private void prepareScene() {
        if (!textureManager.initialize()) {
            Log.e(TAG, "No se pudo inicializar TextureManager");
            return;
        }
        if ("Agujero Negro".equals(item_seleccinado)) {
            BlenderCubeBackground bg = new BlenderCubeBackground(context, textureManager);
            if (bg instanceof CameraAware) {
                ((CameraAware) bg).setCameraController(sharedCamera);
                Log.d(TAG, "CameraController inyectado en BlenderCubeBackground");
            }
            sceneObjects.add(bg);
            Log.d(TAG, "BlenderCubeBackground agregado a la escena");
        }else if ("Universo".equals(item_seleccinado)) {
            sceneObjects.add(new UniverseBackground(context, textureManager));
        }
        // ... otros casos ...
    }
}
