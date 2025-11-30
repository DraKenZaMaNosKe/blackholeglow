package com.secret.blackholeglow.scenes;

import android.content.Context;
import android.util.Log;

import com.secret.blackholeglow.CameraController;
import com.secret.blackholeglow.TextureManager;

import java.util.HashMap;
import java.util.Map;

/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║   🎬 SceneManager - Gestor de Escenas de Wallpaper               ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * Responsabilidades:
 * - Registrar escenas disponibles
 * - Cargar/descargar escenas bajo demanda
 * - Liberar recursos al cambiar de escena
 * - Mantener solo UNA escena activa a la vez
 *
 * USO:
 * 1. Registrar escenas: sceneManager.registerScene("Ocean", OceanPearlScene.class)
 * 2. Cargar escena: sceneManager.loadScene("Ocean")
 * 3. Update/Draw: sceneManager.update(dt) / sceneManager.draw()
 * 4. Cambiar: sceneManager.loadScene("Universo") - libera anterior automaticamente
 */
public class SceneManager {

    private static final String TAG = "SceneManager";

    // ═══════════════════════════════════════════════════════════════
    // ESCENAS REGISTRADAS
    // ═══════════════════════════════════════════════════════════════
    private final Map<String, Class<? extends WallpaperScene>> registeredScenes = new HashMap<>();

    // ═══════════════════════════════════════════════════════════════
    // ESCENA ACTUAL
    // ═══════════════════════════════════════════════════════════════
    private WallpaperScene currentScene = null;
    private String currentSceneName = null;

    // ═══════════════════════════════════════════════════════════════
    // RECURSOS COMPARTIDOS
    // ═══════════════════════════════════════════════════════════════
    private Context context;
    private TextureManager textureManager;
    private CameraController camera;

    // ═══════════════════════════════════════════════════════════════
    // ESTADO
    // ═══════════════════════════════════════════════════════════════
    private boolean isInitialized = false;
    private int screenWidth = 1080;
    private int screenHeight = 1920;

    // ═══════════════════════════════════════════════════════════════
    // CALLBACKS
    // ═══════════════════════════════════════════════════════════════
    public interface SceneLoadCallback {
        void onSceneLoaded(String sceneName, WallpaperScene scene);
        void onSceneLoadError(String sceneName, Exception error);
    }

    private SceneLoadCallback loadCallback;

    /**
     * Constructor del SceneManager.
     */
    public SceneManager() {
        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║   🎬 SCENE MANAGER CREADO              ║");
        Log.d(TAG, "╚════════════════════════════════════════╝");
    }

    /**
     * Inicializa el SceneManager con los recursos necesarios.
     * DEBE llamarse antes de cargar cualquier escena.
     */
    public void initialize(Context context, TextureManager textureManager, CameraController camera) {
        this.context = context;
        this.textureManager = textureManager;
        this.camera = camera;
        this.isInitialized = true;

        Log.d(TAG, "✓ SceneManager inicializado");
    }

    /**
     * Configura las dimensiones de pantalla.
     */
    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;

        if (currentScene != null) {
            currentScene.setScreenSize(width, height);
        }
    }

    /**
     * Establece el callback para eventos de carga.
     */
    public void setLoadCallback(SceneLoadCallback callback) {
        this.loadCallback = callback;
    }

    // ═══════════════════════════════════════════════════════════════
    // REGISTRO DE ESCENAS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Registra una escena disponible.
     *
     * @param name       Nombre unico de la escena
     * @param sceneClass Clase que extiende WallpaperScene
     */
    public void registerScene(String name, Class<? extends WallpaperScene> sceneClass) {
        registeredScenes.put(name, sceneClass);
        Log.d(TAG, "📝 Escena registrada: " + name + " → " + sceneClass.getSimpleName());
    }

    /**
     * Verifica si una escena esta registrada.
     */
    public boolean isSceneRegistered(String name) {
        return registeredScenes.containsKey(name);
    }

    /**
     * Obtiene los nombres de todas las escenas registradas.
     */
    public String[] getRegisteredSceneNames() {
        return registeredScenes.keySet().toArray(new String[0]);
    }

    // ═══════════════════════════════════════════════════════════════
    // CARGA Y DESCARGA DE ESCENAS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Carga una escena por su nombre.
     * Si hay una escena activa, la destruye primero.
     *
     * DEBE llamarse desde el GL thread.
     *
     * @param sceneName Nombre de la escena a cargar
     * @return true si la escena se cargo correctamente
     */
    public boolean loadScene(String sceneName) {
        if (!isInitialized) {
            Log.e(TAG, "❌ SceneManager no inicializado!");
            return false;
        }

        // Verificar si la escena esta registrada
        if (!registeredScenes.containsKey(sceneName)) {
            Log.e(TAG, "❌ Escena no registrada: " + sceneName);
            if (loadCallback != null) {
                loadCallback.onSceneLoadError(sceneName, new IllegalArgumentException("Escena no registrada"));
            }
            return false;
        }

        // Si ya esta cargada esta escena, no hacer nada
        if (sceneName.equals(currentSceneName) && currentScene != null && currentScene.isLoaded()) {
            Log.d(TAG, "⚡ Escena ya cargada: " + sceneName);
            return true;
        }

        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║   🔄 CAMBIANDO ESCENA                  ║");
        Log.d(TAG, "║   De: " + (currentSceneName != null ? currentSceneName : "ninguna"));
        Log.d(TAG, "║   A:  " + sceneName);
        Log.d(TAG, "╚════════════════════════════════════════╝");

        // 1. DESTRUIR escena actual (liberar recursos)
        if (currentScene != null) {
            try {
                Log.d(TAG, "🗑️ Destruyendo escena anterior: " + currentSceneName);
                currentScene.onDestroy();
            } catch (Exception e) {
                Log.e(TAG, "Error destruyendo escena: " + currentSceneName, e);
            }
            currentScene = null;
            currentSceneName = null;
        }

        // 2. CREAR nueva escena
        try {
            Class<? extends WallpaperScene> sceneClass = registeredScenes.get(sceneName);
            currentScene = sceneClass.newInstance();

            // 3. INICIALIZAR escena
            currentScene.setScreenSize(screenWidth, screenHeight);
            currentScene.onCreate(context, textureManager, camera);

            currentSceneName = sceneName;

            Log.d(TAG, "✓ Escena cargada: " + sceneName + " (" + currentScene.getObjectCount() + " objetos)");

            if (loadCallback != null) {
                loadCallback.onSceneLoaded(sceneName, currentScene);
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error cargando escena: " + sceneName, e);
            currentScene = null;
            currentSceneName = null;

            if (loadCallback != null) {
                loadCallback.onSceneLoadError(sceneName, e);
            }

            return false;
        }
    }

    /**
     * Descarga la escena actual y libera todos los recursos.
     * DEBE llamarse desde el GL thread.
     */
    public void unloadCurrentScene() {
        if (currentScene != null) {
            Log.d(TAG, "🗑️ Descargando escena: " + currentSceneName);
            try {
                currentScene.onDestroy();
            } catch (Exception e) {
                Log.e(TAG, "Error descargando escena", e);
            }
            currentScene = null;
            currentSceneName = null;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // UPDATE & DRAW
    // ═══════════════════════════════════════════════════════════════

    /**
     * Actualiza la escena actual.
     *
     * @param deltaTime Tiempo desde el ultimo frame (segundos)
     */
    public void update(float deltaTime) {
        if (currentScene != null && currentScene.isLoaded()) {
            currentScene.update(deltaTime);
        }
    }

    /**
     * Dibuja la escena actual.
     */
    public void draw() {
        if (currentScene != null && currentScene.isLoaded()) {
            currentScene.draw();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CONTROL DE ESTADO
    // ═══════════════════════════════════════════════════════════════

    /**
     * Pausa la escena actual.
     */
    public void pause() {
        if (currentScene != null) {
            currentScene.onPause();
        }
    }

    /**
     * Reanuda la escena actual.
     */
    public void resume() {
        if (currentScene != null) {
            currentScene.onResume();
        }
    }

    /**
     * Destruye el SceneManager y libera todos los recursos.
     * DEBE llamarse desde el GL thread.
     */
    public void destroy() {
        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║   🗑️ DESTRUYENDO SCENE MANAGER        ║");
        Log.d(TAG, "╚════════════════════════════════════════╝");

        unloadCurrentScene();
        registeredScenes.clear();
        isInitialized = false;
    }

    // ═══════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════

    public WallpaperScene getCurrentScene() {
        return currentScene;
    }

    public String getCurrentSceneName() {
        return currentSceneName;
    }

    public boolean hasSceneLoaded() {
        return currentScene != null && currentScene.isLoaded();
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public int getRegisteredSceneCount() {
        return registeredScenes.size();
    }
}
