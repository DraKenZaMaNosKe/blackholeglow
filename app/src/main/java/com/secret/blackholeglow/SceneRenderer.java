package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase principal que renderiza todos los objetos de la escena con OpenGL ES 2.0.
 */
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
        for (SceneObject obj : sceneObjects) {
            if (obj instanceof VideoBackground) {
                ((VideoBackground) obj).pause();
            }
        }
    }

    public void resume() {
        paused = false;

        // Verificamos si algún objeto es VideoBackground y lo reiniciamos
        for (SceneObject obj : sceneObjects) {
            if (obj instanceof VideoBackground) {
                ((VideoBackground) obj).resumeVideo();
            }
        }
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

        // Reinicia shaders por si se perdieron
        Star.release();

        textureManager = new TextureManager(context); // ✅ Solo instanciamos aquí

        // No creamos ningún objeto todavía
        sceneObjects.clear();
        // ⚠️ Aún no agregamos StarField aquí. Lo haremos en onSurfaceChanged.
        Log.d("SceneRenderer", "🛸 Texturas inicializadas: " + textureManager.isInitialized());
        Log.d("SceneRenderer", "🧱 Objetos en escena: " + sceneObjects.size());
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        screenWidth = width;
        screenHeight = height;

        Log.d("SceneRenderer", "🧭 onSurfaceChanged: tamaño = " + width + "x" + height);

        // ✅ Inicializamos aquí el TextureManager (momento seguro)
        textureManager = new TextureManager(context);
        textureManager.initialize(); // Ya no se necesita variable texturesTried

        // ⚠️ Limpieza previa antes de añadir objetos
        sceneObjects.clear();
        prepareScene();

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

    private void prepareScene() {
        // ⚠️ Solo continuar si el textureManager está listo
        if (textureManager != null && textureManager.initialize()) {
            Log.d("SceneRenderer", "🎨 Texturas listas, creando objetos con textura...");
            sceneObjects.add(new VideoBackground(context)); // 🎥 fondo animado de video
            //sceneObjects.add(new StarTunnelBackground());
            //sceneObjects.add(new StarField(textureManager, 200));
        } else {
            Log.w("SceneRenderer", "🚫 No se pudieron inicializar texturas. Usando modo sin textura...");
            sceneObjects.add(new VideoBackground(context)); // 🎥 fondo animado de video
            //sceneObjects.add(new StarTunnelBackground());
            //sceneObjects.add(new StarField(null, 200));
        }
        Log.d("SceneRenderer", "🎬 Objetos en escena: " + sceneObjects.size());
    }
}