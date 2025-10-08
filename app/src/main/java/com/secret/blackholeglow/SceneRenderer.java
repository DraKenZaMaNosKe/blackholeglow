// SceneRenderer.java - VERSIÓN ESTABLE
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
 * SceneRenderer con cámara fija en posición óptima
 */
public class SceneRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "SceneRenderer";
    public static int screenWidth = 1, screenHeight = 1;

    private final Context context;
    private String selectedItem;
    private boolean paused = false;
    private final List<SceneObject> sceneObjects = new ArrayList<>();
    private long lastTime = System.nanoTime();
    private CameraController sharedCamera;
    private TextureManager textureManager;

    // FPS counter para debug
    private int frameCount = 0;
    private float fpsTimer = 0f;

    public SceneRenderer(Context ctx, String initialItem) {
        this.context = ctx;
        this.selectedItem = initialItem;
        Log.d(TAG, "SceneRenderer creado con item: " + initialItem);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig cfg) {
        Log.d(TAG, "onSurfaceCreated - Iniciando");

        // Configuración OpenGL
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glClearColor(0.02f, 0.02f, 0.05f, 1f); // Fondo espacial oscuro

        // Crear controladores
        sharedCamera = new CameraController();
        textureManager = new TextureManager(context);

        // CONFIGURAR CÁMARA EN POSICIÓN ÓPTIMA
        // Vista 3/4 estilo isométrico pero con perspectiva
        sharedCamera.setMode(CameraController.CameraMode.THIRD_PERSON);
        sharedCamera.setPosition(4f, 3f, 6f);  // Posición elevada y atrás
        sharedCamera.setTarget(0f, 0f, 0f);    // Mirando al centro

        // Preparar escena
        prepareScene();
        Log.d(TAG, "Surface created exitosamente con " + sceneObjects.size() + " objetos");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        GLES20.glViewport(0, 0, w, h);
        screenWidth = w;
        screenHeight = h;

        sharedCamera.updateProjection(w, h);
        Log.d(TAG, "Viewport: " + w + "x" + h);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (paused) return;

        // Delta time
        long now = System.nanoTime();
        float dt = Math.min((now - lastTime) / 1e9f, 0.1f);
        lastTime = now;

        // FPS debug (cada segundo)
        frameCount++;
        fpsTimer += dt;
        if (fpsTimer >= 1.0f) {
            Log.v(TAG, "FPS: " + frameCount);
            frameCount = 0;
            fpsTimer = 0f;
        }

        // Limpiar buffers
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // La cámara NO se actualiza (está fija)
        // Solo actualizamos si queremos una rotación muy lenta
        // sharedCamera.update(dt); // COMENTADO - cámara estática

        // Dibujar objetos
        for (SceneObject obj : sceneObjects) {
            obj.update(dt);
            obj.draw();
        }
    }

    private void prepareScene() {
        Log.d(TAG, "Preparando escena: " + selectedItem);

        if (!textureManager.initialize()) {
            Log.e(TAG, "ERROR: TextureManager no se pudo inicializar");
            return;
        }

        sceneObjects.clear();

        if ("Universo".equals(selectedItem)) {
            setupUniverseScene();
        } else if ("Agujero Negro".equals(selectedItem)) {
            setupBlackHoleScene();
        } else {
            // Por defecto usar Universo
            setupUniverseScene();
        }

        Log.d(TAG, "Escena preparada con " + sceneObjects.size() + " objetos");
    }

    private void setupUniverseScene() {
        Log.d(TAG, "Configurando escena UNIVERSO");

        // FONDO - Solo si tienes la textura
        try {
            UniverseBackground fondo = new UniverseBackground(
                    context, textureManager,
                    "shaders/universe_vertex.glsl",
                    "shaders/universe_fragment.glsl",
                    R.drawable.fondo_universo_cosmico,
                    1.0f
            );
            if (fondo instanceof CameraAware) {
                ((CameraAware) fondo).setCameraController(sharedCamera);
            }
            sceneObjects.add(fondo);
            Log.d(TAG, "Fondo agregado");
        } catch (Exception e) {
            Log.e(TAG, "Error creando fondo: " + e.getMessage());
        }

        // SOL CENTRAL - Más pequeño y centrado
        Planeta sol = new Planeta(
                context, textureManager,
                "shaders/planeta_vertex.glsl",
                "shaders/planeta_fragment.glsl",
                R.drawable.textura_sol,
                0.0f, 0.0f, 0.0f,  // Sin órbita
                0.0f,              // Sin variación
                0.5f,              // Escala mediana
                2.0f,             // Rotación lenta
                false, null, 1.0f,
                null,
                1.0f
        );
        if (sol instanceof CameraAware) {
            ((CameraAware) sol).setCameraController(sharedCamera);
        }
        sceneObjects.add(sol);
        Log.d(TAG, "Sol agregado");

        //glow sobre el sol
        Planeta planeta_glow_del_sol = new Planeta(
                context, textureManager,
                "shaders/planeta_vertex.glsl",
                "shaders/planeta_fragment.glsl",
                R.drawable.textura_sol,
                0.0f, 0.0f, 0.0f,  // Sin órbita
                5f,              // Sin variación
                0.7f,              // Escala mediana
                10.0f,             // Rotación lenta
                true, new float[]{0.526f,0.221f,0.111f,0.5f}, 1.0f,
                1.5f,
                0.5f
        );
        if (planeta_glow_del_sol instanceof CameraAware) {
            ((CameraAware) planeta_glow_del_sol).setCameraController(sharedCamera);
        }
        sceneObjects.add(planeta_glow_del_sol);
        Log.d(TAG, "Glow del sol agregado");

        // PLANETA ORBITANTE
        Planeta planeta1 = new Planeta(
                context, textureManager,
                "shaders/planeta_vertex.glsl",
                "shaders/planeta_fragment.glsl",
                R.drawable.textura_roninplaneta,
                2.5f, 2.0f, 0.3f,  // Órbita mediana
                0.1f,              // Poca variación
                0.2f,              // Tamaño pequeño
                10.0f,             // Rotación media
                false, null, 1.0f,
                null,
                1.0f
        );
        if (planeta1 instanceof CameraAware) {
            ((CameraAware) planeta1).setCameraController(sharedCamera);
        }
        sceneObjects.add(planeta1);
        Log.d(TAG, "Planeta orbitante agregado");
    }

    private void setupBlackHoleScene() {
        Log.d(TAG, "Configurando escena AGUJERO NEGRO");

        // Centro negro
       Planeta blackHole = new Planeta(
                context, textureManager,
                "shaders/planeta_vertex.glsl",
                "shaders/planeta_fragment.glsl",
                R.drawable.fondo_transparente,
                0.0f, 0.0f, 0.0f,
                0.05f,
                2.0f,
                0.0f,
                true,
                new float[]{0.0f, 0.0f, 0.0f, 1.0f},
                1.0f,
                0.98f,
                1.0f
        );
        if (blackHole instanceof CameraAware) {
            ((CameraAware) blackHole).setCameraController(sharedCamera);
        }
        sceneObjects.add(blackHole);

        // Disco de acreción simple
        for (int i = 0; i < 3; i++) {
            float radius = 2.0f + i * 0.8f;
            Planeta particle = new Planeta(
                    context, textureManager,
                    "shaders/planeta_vertex.glsl",
                    "shaders/planeta_fragment.glsl",
                    R.drawable.textura_asteroide,
                    radius, radius * 0.8f, 0.5f / (i + 1),
                    0.1f,
                    0.3f,
                    50.0f,
                    true,
                    new float[]{1.0f, 0.5f, 0.2f, 0.7f},
                    0.8f,
                    null,
                    1.0f
            );
            if (particle instanceof CameraAware) {
                ((CameraAware) particle).setCameraController(sharedCamera);
            }
            sceneObjects.add(particle);
        }

        Log.d(TAG, "Agujero negro configurado");
    }

    public void pause() {
        paused = true;
        Log.d(TAG, "Renderer pausado");
    }

    public void resume() {
        paused = false;
        lastTime = System.nanoTime();
        Log.d(TAG, "Renderer resumido");
    }

    public void release() {
        Log.d(TAG, "Liberando recursos...");
        for (SceneObject obj : sceneObjects) {
            if (obj instanceof UniverseBackground) {
                ((UniverseBackground) obj).release();
            }
        }
    }

    public void setSelectedItem(String item) {
        if (item != null && !item.equals(selectedItem)) {
            Log.d(TAG, "Cambiando escena a: " + item);
            this.selectedItem = item;
            prepareScene();
        }
    }
}