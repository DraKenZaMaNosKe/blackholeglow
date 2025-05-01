package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import java.util.ArrayList;
import java.util.List;

public class SceneRenderer implements android.opengl.GLSurfaceView.Renderer {
    private final Context context;

    public static int screenWidth = 1;
    public static int screenHeight = 1;

    private boolean paused = false;
    public boolean isWallpaper = false;
    private final List<SceneObject> sceneObjects = new ArrayList<>();

    private long lastTime = System.nanoTime();

    private TextureManager textureManager;

    public SceneRenderer(Context context) {
        this.context = context;
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }

    public void addObject(SceneObject object) {
        sceneObjects.add(object);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glClearColor(0f, 0f, 0f, 1f);

        Star.release(); // Reset shaders

        // Inicializar TextureManager SOLO cuando OpenGL ya esté listo
        textureManager = new TextureManager(context);
        textureManager.initialize();

        // Fondo de vórtice
        sceneObjects.add(new StarTunnelBackground());

        // Campo estelar con gestor de texturas
        sceneObjects.add(new StarField(textureManager, 50));
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        screenWidth = width;
        screenHeight = height;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (paused) return;

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        long currentTime = System.nanoTime();
        float deltaTime = (currentTime - lastTime) / 1_000_000_000f;
        lastTime = currentTime;

        for (SceneObject object : sceneObjects) {
            object.update(deltaTime);
            object.draw();
        }
    }
}
