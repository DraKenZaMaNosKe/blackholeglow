package com.secret.blackholeglow;

import android.opengl.GLES20;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.util.ArrayList;
import java.util.List;

public class SceneRenderer implements android.opengl.GLSurfaceView.Renderer {

    public boolean isWallpaper = false;
    private final List<SceneObject> sceneObjects = new ArrayList<>();

    private long lastTime = System.nanoTime();

    // Puedes usar esto para agregar objetos a la escena desde fuera
    public void addObject(SceneObject object) {
        sceneObjects.add(object);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //GLES20.glEnable(GLES20.GL_PROGRAM_POINT_SIZE);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glClearColor(0f, 0f, 0f, 1f); // Fondo negro

        Star.release(); // Resetear el shader de las estrellas

        // Inicializar los objetos de la escena
        StarField starField = new StarField(200);
        sceneObjects.add(starField);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Calcular tiempo entre frames
        long currentTime = System.nanoTime();
        float deltaTime = (currentTime - lastTime) / 1_000_000_000f;
        lastTime = currentTime;

        // Actualizar y dibujar cada objeto de la escena
        for (SceneObject object : sceneObjects) {
            object.update(deltaTime);
            object.draw();
        }
    }
}