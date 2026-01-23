package com.secret.blackholeglow.core;

import android.content.Context;
import android.util.Log;

import com.secret.blackholeglow.CameraController;
import com.secret.blackholeglow.TextureManager;
import com.secret.blackholeglow.scenes.AdventureTimeScene;
import com.secret.blackholeglow.scenes.GokuScene;
import com.secret.blackholeglow.scenes.LabScene;
import com.secret.blackholeglow.scenes.NeonCityScene;
import com.secret.blackholeglow.scenes.SaintSeiyaScene;
import com.secret.blackholeglow.scenes.OceanFloorScene;
import com.secret.blackholeglow.scenes.WalkingDeadScene;
import com.secret.blackholeglow.scenes.WallpaperScene;
import com.secret.blackholeglow.scenes.ZeldaParallaxScene;
import com.secret.blackholeglow.systems.EventBus;
import com.secret.blackholeglow.systems.ResourceManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                        SceneFactory                              ║
 * ║            Fábrica de Escenas de Wallpaper                       ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  RESPONSABILIDADES:                                              ║
 * ║  • Registrar escenas disponibles                                 ║
 * ║  • Crear instancias de escenas                                   ║
 * ║  • Inyectar dependencias a escenas                               ║
 * ║  • Destruir escenas                                              ║
 * ║  • Gestionar el ciclo de vida de escenas                         ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class SceneFactory {
    private static final String TAG = "SceneFactory";

    // ═══════════════════════════════════════════════════════════════
    // 📚 REGISTRO DE ESCENAS
    // ═══════════════════════════════════════════════════════════════

    private final Map<String, Class<? extends WallpaperScene>> registeredScenes = new HashMap<>();
    private final String defaultSceneName = "PYRALIS";

    // Dependencias para inyectar
    private Context context;
    private TextureManager textureManager;
    private CameraController camera;
    private ResourceManager resourceManager;
    private int screenWidth = 1;
    private int screenHeight = 1;

    // Escena actual
    private WallpaperScene currentScene;
    private String currentSceneName = "";

    public SceneFactory() {
        Log.d(TAG, "🎭 SceneFactory inicializado");
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔧 CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════

    public void setContext(Context ctx) {
        this.context = ctx.getApplicationContext();
    }

    public void setTextureManager(TextureManager tm) {
        this.textureManager = tm;
    }

    public void setCamera(CameraController cam) {
        this.camera = cam;
    }

    public void setResourceManager(ResourceManager rm) {
        this.resourceManager = rm;
    }

    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        if (currentScene != null) {
            currentScene.setScreenSize(width, height);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 📝 REGISTRO DE ESCENAS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Registra una escena con un nombre
     */
    public void registerScene(String name, Class<? extends WallpaperScene> sceneClass) {
        registeredScenes.put(name, sceneClass);
        Log.d(TAG, "📝 Escena registrada: " + name);
    }

    /**
     * Registra las escenas por defecto
     * Solo Lab y Ocean están disponibles
     */
    public void registerDefaultScenes() {
        // 🌊 Fondo del Mar: Video wallpaper alienígena con efectos shader
        registerScene("ABYSSIA", OceanFloorScene.class);
        // 🔥 Pyralis: Portal cósmico con nubes de fuego y nave Enterprise
        registerScene("PYRALIS", LabScene.class);
        // 🐉 Dragon Ball: Goku Kamehameha con energía Ki
        registerScene("GOKU", GokuScene.class);
        // 🌳 Adventure Time: Parallax de Hora de Aventura
        registerScene("ADVENTURE_TIME", AdventureTimeScene.class);
        // 🚗 Neon City: Synthwave DeLorean en carretera infinita
        registerScene("NEON_CITY", NeonCityScene.class);
        // ⭐ Saint Seiya: Cosmos power con caballeros del zodiaco
        registerScene("SAINT_SEIYA", SaintSeiyaScene.class);
        registerScene("WALKING_DEAD", WalkingDeadScene.class);
        // 🗡️ Zelda BOTW: Parallax con giroscopio estilo Breath of the Wild
        registerScene("ZELDA_BOTW", ZeldaParallaxScene.class);

        Log.d(TAG, "🎭 " + registeredScenes.size() + " escenas registradas");
    }

    /**
     * ¿Existe una escena con este nombre?
     */
    public boolean hasScene(String name) {
        return registeredScenes.containsKey(name);
    }

    /**
     * Obtiene los nombres de todas las escenas registradas
     */
    public Set<String> getSceneNames() {
        return registeredScenes.keySet();
    }

    // ═══════════════════════════════════════════════════════════════
    // 🏭 CREACIÓN DE ESCENAS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Crea y carga una escena por nombre
     * @param sceneName Nombre de la escena (o null/vacío para default)
     * @return La escena creada, o null si falló
     */
    public WallpaperScene createScene(String sceneName) {
        // Usar default si no se especifica
        if (sceneName == null || sceneName.isEmpty()) {
            sceneName = defaultSceneName;
        }

        // Ya estamos en esta escena?
        if (sceneName.equals(currentSceneName) && currentScene != null) {
            Log.d(TAG, "⚠️ Ya estamos en la escena: " + sceneName);
            return currentScene;
        }

        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║   🏭 CREANDO ESCENA                    ║");
        Log.d(TAG, "║   " + sceneName);
        Log.d(TAG, "╚════════════════════════════════════════╝");

        // Destruir escena actual
        destroyCurrentScene();

        // Buscar clase de la escena
        Class<? extends WallpaperScene> sceneClass = registeredScenes.get(sceneName);
        if (sceneClass == null) {
            Log.e(TAG, "❌ Escena no registrada: " + sceneName);
            return null;
        }

        try {
            // Crear instancia
            WallpaperScene scene = sceneClass.getConstructor().newInstance();

            // Inyectar dependencias
            if (resourceManager != null) {
                scene.setResourceManager(resourceManager);
            }

            // Inicializar
            scene.onCreate(context, textureManager, camera);
            scene.setScreenSize(screenWidth, screenHeight);

            // Guardar referencia
            currentScene = scene;
            currentSceneName = sceneName;

            Log.d(TAG, "✅ Escena " + sceneName + " creada exitosamente");

            // Publicar evento
            EventBus.get().publish(EventBus.SCENE_CHANGED,
                new EventBus.EventData().put("scene", sceneName));

            return scene;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error creando escena: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Destruye la escena actual
     */
    public void destroyCurrentScene() {
        if (currentScene != null) {
            Log.d(TAG, "🗑️ Destruyendo escena: " + currentSceneName);
            currentScene.onDestroy();
            currentScene = null;
            currentSceneName = "";
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ⏯️ CICLO DE VIDA
    // ═══════════════════════════════════════════════════════════════

    /**
     * Pausa la escena actual
     */
    public void pauseCurrentScene() {
        if (currentScene != null) {
            currentScene.onPause();
        }
    }

    /**
     * Reanuda la escena actual
     */
    public void resumeCurrentScene() {
        if (currentScene != null) {
            currentScene.onResume();
        }
    }

    /**
     * Actualiza la escena actual
     */
    public void updateCurrentScene(float deltaTime) {
        if (currentScene != null) {
            currentScene.update(deltaTime);
        }
    }

    /**
     * Dibuja la escena actual
     */
    public void drawCurrentScene() {
        if (currentScene != null) {
            currentScene.draw();
        }
    }

    /**
     * Pasa evento de toque a la escena actual
     */
    public boolean onSceneTouchEvent(float nx, float ny, int action) {
        if (currentScene != null) {
            return currentScene.onTouchEvent(nx, ny, action);
        }
        return false;
    }

    // ═══════════════════════════════════════════════════════════════
    // 📊 GETTERS
    // ═══════════════════════════════════════════════════════════════

    public WallpaperScene getCurrentScene() {
        return currentScene;
    }

    public String getCurrentSceneName() {
        return currentSceneName;
    }

    public boolean hasCurrentScene() {
        return currentScene != null;
    }

    public String getDefaultSceneName() {
        return defaultSceneName;
    }
}

