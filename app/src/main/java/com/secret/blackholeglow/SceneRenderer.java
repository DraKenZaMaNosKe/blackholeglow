package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.util.ArrayList;
import java.util.List;

public class SceneRenderer implements GLSurfaceView.Renderer {
    private final Context context;
    public static int screenWidth = 1;
    public static int screenHeight = 1;
    private boolean paused = false;
    private final List<SceneObject> sceneObjects = new ArrayList<>();
    private long lastTime = System.nanoTime();
    private float manualYawOffset = 0f;
    private TextureManager textureManager;
    private String item_seleccinado;

    public SceneRenderer(Context context, String item) {
        this.context = context;
        this.item_seleccinado = item;
    }

    public void adjustYaw(float deltaDegrees) {
        manualYawOffset += deltaDegrees;
    }

    public void pause() { paused = true; }
    public void resume() { paused = false; }
    public void setSelectedItem(String item) {
        this.item_seleccinado = item;
        sceneObjects.clear();
        prepareScene();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClearColor(0f,0f,0f,1f);
        textureManager = new TextureManager(context);
        sceneObjects.clear();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0,0,width,height);
        screenWidth = width;
        screenHeight = height;
        textureManager = new TextureManager(context);
        textureManager.initialize();
        sceneObjects.clear();
        prepareScene();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (paused) return;
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        long now = System.nanoTime();
        float dt = (now - lastTime)/1_000_000_000f;
        lastTime = now;
        for (SceneObject obj : sceneObjects) {
            if (obj instanceof RotatingTexturedCubeBackground) {
                ((RotatingTexturedCubeBackground)obj).yawOverride(manualYawOffset);
                manualYawOffset = 0f;
            }
            obj.update(dt);
            obj.draw();
        }
    }

    private void prepareScene() {
        if (textureManager.initialize()) {
            if ("FondoCubo".equals(item_seleccinado)) {
                sceneObjects.add(new RotatingTexturedCubeBackground(textureManager));
            }
            else if ("MeshMano".equals(item_seleccinado)) {
                // 👉 Aquí activamos el mesh de mano giratoria
                sceneObjects.add(new RotatingHandMeshBackground(textureManager));
            }else if ("CuboDeformable".equals(item_seleccinado)) {
                sceneObjects.add(new DeformableCubeBackground(context, textureManager));
            }
            // otros items...
        } else {
            // modo sin texturas...
        }
    }
}
