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
 * Renderer principal: gestiona la cámara, la escena y el bucle de dibujo.
 * Instancia Planeta y UniverseBackground según el ítem seleccionado.
 */
public class SceneRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "SceneRenderer";

    public static int screenWidth  = 1;
    public static int screenHeight = 1;

    private final Context      context;
    private       String       selectedItem;
    private       boolean      paused = false;
    private final List<SceneObject> sceneObjects = new ArrayList<>();
    private long    lastTime      = System.nanoTime();
    private CameraController sharedCamera;
    private TextureManager  textureManager;

    public SceneRenderer(Context ctx, String initialItem) {
        this.context      = ctx;
        this.selectedItem = initialItem;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig cfg) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glBlendFunc(
                GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA
        );
        GLES20.glClearColor(0f, 0f, 0f, 1f);

        sharedCamera   = new CameraController();
        textureManager = new TextureManager(context);

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
                0f,0f,6f,  // eye
                0f,0f,0f,  // center
                0f,1f,0f   // up
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
        float dt = (now - lastTime)/1_000_000_000f;
        lastTime = now;

        sharedCamera.update(dt);

        for (SceneObject obj : sceneObjects) {
            obj.update(dt);
            obj.draw();
        }
    }

    /** Inicializa y agrega objetos a la escena según selección. */
    private void prepareScene() {
        if (!textureManager.initialize()) {
            Log.e(TAG, "No se pudo inicializar TextureManager");
            return;
        }
        sceneObjects.clear();

        if ("Universo".equals(selectedItem)) {
            sceneObjects.add(
                    new UniverseBackground(context, textureManager)
            );
            sceneObjects.add(
                    new Planeta(context, textureManager,
                            0f, 0f, 0f, 0f, 5.3f, 10f)
            );
            sceneObjects.add(
                    new Planeta(context, textureManager,
                            0.1f, 1.1f, 0.3f, 0.2f, 2.0f, 20f)
            );
        } else if ("Agujero Negro".equals(selectedItem)) {
            DeformableCubeBackground bg =
                    new DeformableCubeBackground(context, textureManager);
            if (bg instanceof CameraAware) {
                ((CameraAware)bg).setCameraController(sharedCamera);
            }
            sceneObjects.add(bg);
        }
    }

    public void pause()  { paused = true; }
    public void resume() { paused = false; lastTime = System.nanoTime(); }
    public void adjustYaw(float delta) {
        if (sharedCamera != null) {
            sharedCamera.addOrbitOffset(delta);
        }
    }
    public void setSelectedItem(String item) {
        this.selectedItem = item;
        // podrías hacer hot-reload aquí si quieres:
        // sceneObjects.clear(); prepareScene();
    }
}
