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
    private boolean texturesTried = false;

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
        Gatito.lineaseparadora();
        Log.d("SceneRenderer", "onSurfaceCreated: ");
        // Configuración de OpenGL
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glClearColor(0f, 0f, 0f, 1f);
        // Reinicia shaders por si se perdieron al restaurar contexto
        Star.release();

        // Creamos el gestor de texturas
        textureManager = new TextureManager(context);

        // Esta es la corrección principal: quitamos el `&& textureManager.initialize()`
        if (!texturesTried && textureManager != null) {
            Log.d("SceneRenderer", "⏳ Probando texturas... dentro de if textures null");
            texturesTried = true;
            textureManager.initialize();  // ⚠️ No retorna booleano, solo se llama
        }

        // Siempre agregar objetos aunque las texturas hayan fallado
        sceneObjects.clear();  // Limpia por si se recarga

        sceneObjects.add(new StarTunnelBackground());

        // Este se agrega solo si tenemos texturas
        if (textureManager != null && textureManager.isInitialized()) {
            Log.d("SceneRenderer", "📄 Texturas inicializadas: " + textureManager.isInitialized());
            sceneObjects.add(new StarField(textureManager, 5));
        } else {
            Log.d("SceneRenderer", "❌ Texturas NO inicializadas: " + textureManager.isInitialized());
            // Si falló, igual carga estrellas como puntos blancos
            sceneObjects.add(new StarField(null, 5));
        }
        Log.d("SceneRenderer", "🛸 Texturas inicializadas: " + textureManager.isInitialized());
        Log.d("SceneRenderer", "🧱 Objetos en escena: " + sceneObjects.size());
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d("SceneRenderer", "onSurfaceChanged: ");
        GLES20.glViewport(0, 0, width, height);
        screenWidth = width;
        screenHeight = height;
        Gatito.lineaseparadora();
        Gatito.mensajito("dimensiones detectadas: " + screenWidth + "x" + screenHeight + "");

        // Inicializar textureManager AQUÍ, después de que el viewport esté configurado
        textureManager = new TextureManager(context);
        textureManager.initialize();
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