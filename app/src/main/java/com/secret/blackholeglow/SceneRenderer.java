package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.util.ArrayList;
import java.util.List;

/**
 * Renderiza la escena y deja que cada objeto maneje su cámara.
 */
public class SceneRenderer implements GLSurfaceView.Renderer {
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

    /**
     * Este método es invocado por LiveWallpaperService para rotar manualmente la cámara
     * cuando el usuario arrastra el dedo.
     */
    public void adjustYaw(float deltaDegrees) {
        // Por ahora simplemente afectamos el ángulo de órbita directamente:
        // avanzamos (o retrocedemos) el tiempo de órbita según el delta
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
        sharedCamera = new CameraController();
        sceneObjects.clear();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        GLES20.glViewport(0, 0, w, h);
        screenWidth  = w;
        screenHeight = h;
        sharedCamera.updateProjection(w, h);
        textureManager.initialize();
        sceneObjects.clear();
        prepareScene();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (paused) return;

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        long now = System.nanoTime();
        float dt = (now - lastTime) / 1_000_000_000f;
        lastTime = now;

        // Actualiza la cámara global (ó rbita + zoom)
        sharedCamera.update(dt);
        Log.d("SceneRenderer", "onDrawFrame dt=" + dt);

        // Dibuja cada objeto de la escena
        for (SceneObject obj : sceneObjects) {
            obj.update(dt);
            obj.draw();
        }
    }

    private void prepareScene() {
        if (!textureManager.initialize()) return;
        if ("CuboMesh".equals(item_seleccinado)) {
            BlenderCubeBackground bg = new BlenderCubeBackground(context, textureManager);
            bg.setCameraController(sharedCamera);
            sceneObjects.add(bg);
        }
        // ... otros casos si los necesitas
    }
}
