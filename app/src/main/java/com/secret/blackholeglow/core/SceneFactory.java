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
import com.secret.blackholeglow.scenes.SupermanScene;
import com.secret.blackholeglow.scenes.AOTScene;
import com.secret.blackholeglow.scenes.SpiderScene;
import com.secret.blackholeglow.scenes.LostAtlantisScene;
import com.secret.blackholeglow.scenes.TheHumanPredatorScene;
import com.secret.blackholeglow.scenes.MoonlitCatScene;
import com.secret.blackholeglow.scenes.FriezaDeathBeamScene;
import com.secret.blackholeglow.scenes.KenScene;
import com.secret.blackholeglow.scenes.ScorpionScene;
import com.secret.blackholeglow.scenes.TrenNocturnoScene;
import com.secret.blackholeglow.scenes.TheEyeScene;
import com.secret.blackholeglow.scenes.GatitoScene;
import com.secret.blackholeglow.scenes.GatitoDJScene;
import com.secret.blackholeglow.scenes.PixelCityScene;
import com.secret.blackholeglow.scenes.KratosScene;
import com.secret.blackholeglow.scenes.KratosCyclopsScene;
import com.secret.blackholeglow.scenes.ItachiScene;
import com.secret.blackholeglow.scenes.SupercampeonesScene;
import com.secret.blackholeglow.scenes.GokuKamehameScene;
import com.secret.blackholeglow.scenes.AlienScene;
import com.secret.blackholeglow.scenes.AlienStaticScene;
import com.secret.blackholeglow.scenes.MegamanShootingScene;
import com.secret.blackholeglow.scenes.MegamanShootingStaticScene;
import com.secret.blackholeglow.scenes.ImageWallpaperScene;
import com.secret.blackholeglow.scenes.DynamicImageScene;
import com.secret.blackholeglow.scenes.DynamicVideoScene;
import com.secret.blackholeglow.systems.DynamicCatalog;
import com.secret.blackholeglow.systems.EventBus;
import com.secret.blackholeglow.systems.ResourceManager;

import java.util.Collections;
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
        if (registeredScenes.containsKey(name)) {
            Log.w(TAG, "⚠️ Overwriting existing scene registration: " + name);
        }
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
        // 🦸 Superman: Man of Steel volando épicamente
        registerScene("SUPERMAN", SupermanScene.class);
        // ⚔️ Attack on Titan: Eren Jaeger con el Titán Colosal
        registerScene("AOT", AOTScene.class);
        // 🕷️ Spider: Black Spider Horror con ojos rojos brillantes
        registerScene("SPIDER", SpiderScene.class);
        // 🏛️ Lost Atlantis: Templo sumergido con energía mística
        registerScene("LOST_ATLANTIS", LostAtlantisScene.class);
        // 🦁 The Human Predator: Guerrero prehistórico vs León gigante
        registerScene("THE_HUMAN_PREDATOR", TheHumanPredatorScene.class);
        // 🌙 Moonlit Cat: Gato negro bajo la luna con cielo shader
        registerScene("MOONLIT_CAT", MoonlitCatScene.class);
        // 💜 Frieza Death Beam: Frieza Final Form con Death Beam
        registerScene("FRIEZA_DEATHBEAM", FriezaDeathBeamScene.class);
        // 🥊 Ken: Side-scrolling pixel art fighter con parallax
        registerScene("KEN", KenScene.class);
        // 🦂 Scorpion: Mortal Kombat fire ninja
        registerScene("SCORPION", ScorpionScene.class);
        // 🚂 Tren Nocturno: Pixel art night train
        registerScene("TREN_NOCTURNO", TrenNocturnoScene.class);
        // 👁️ The Eye: Mysterious iris
        registerScene("THE_EYE", TheEyeScene.class);
        // 🐱 Gatito: Cute cat animation
        registerScene("GATITO", GatitoScene.class);
        // 🎧 Gatito DJ: Dancing DJ cat
        registerScene("GATITO_DJ", GatitoDJScene.class);
        // 🏙️ Pixel City: Retro pixel art city
        registerScene("PIXEL_CITY", PixelCityScene.class);
        // ⚔️ Kratos: God of War - Furia espartana
        registerScene("KRATOS", KratosScene.class);
        // ⚔️ Kratos vs Cyclops: Imagen + aura de energía
        registerScene("KRATOS_CYCLOPS", KratosCyclopsScene.class);
        // 👁️ Itachi: Sharingan eye video
        registerScene("ITACHI", ItachiScene.class);
        // ⚽ Supercampeones: Captain Tsubasa golden kick
        registerScene("SUPERCAMPEONES", SupercampeonesScene.class);
        // 🐉 Goku Kamehameha: SSJ Ki energy blast
        registerScene("GOKU_KAMEHAME", GokuKamehameScene.class);
        // 👽 Alien: Xenomorph acid terror
        registerScene("ALIEN", AlienScene.class);
        registerScene("ALIEN_STATIC", AlienStaticScene.class);
        // 🔫 Megaman: Energy blast
        registerScene("MEGAMAN_SHOOTING", MegamanShootingScene.class);
        registerScene("MEGAMAN_SHOOTING_STATIC", MegamanShootingStaticScene.class);

        // 🖼️ Image (static) versions - all use ImageWallpaperScene with configure()
        for (String name : ImageWallpaperScene.getRegisteredNames()) {
            registerScene(name, ImageWallpaperScene.class);
        }

        Log.d(TAG, "🎭 " + registeredScenes.size() + " escenas registradas");
    }

    /**
     * ¿Existe una escena con este nombre?
     */
    public boolean hasScene(String name) {
        if (name != null && (name.startsWith("DYN_IMG_") || name.startsWith("DYN_VID_"))) {
            return true;
        }
        return registeredScenes.containsKey(name);
    }

    /**
     * Obtiene los nombres de todas las escenas registradas
     */
    public Set<String> getSceneNames() {
        return Collections.unmodifiableSet(registeredScenes.keySet());
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

        // Dynamic scene handling: DYN_IMG_* and DYN_VID_* prefixes
        if (sceneName.startsWith("DYN_IMG_") || sceneName.startsWith("DYN_VID_")) {
            return createDynamicScene(sceneName);
        }

        // Buscar clase de la escena
        Class<? extends WallpaperScene> sceneClass = registeredScenes.get(sceneName);
        if (sceneClass == null) {
            Log.e(TAG, "❌ Escena no registrada: " + sceneName);
            return null;
        }

        try {
            // Crear instancia
            WallpaperScene scene = sceneClass.getConstructor().newInstance();

            // Configurar escenas genéricas (ImageWallpaperScene)
            if (scene instanceof ImageWallpaperScene) {
                ((ImageWallpaperScene) scene).configure(sceneName);
            }

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
     * Creates a dynamic scene (DYN_IMG_* or DYN_VID_*) without static registration.
     */
    private WallpaperScene createDynamicScene(String sceneName) {
        try {
            // Validate the dynamic entry exists before creating scene
            String id = sceneName.startsWith("DYN_IMG_")
                ? sceneName.substring("DYN_IMG_".length())
                : sceneName.substring("DYN_VID_".length());

            DynamicCatalog.DynamicEntry entry = DynamicCatalog.get().getEntryById(id, context);
            if (entry == null) {
                Log.e(TAG, "❌ Dynamic entry not found for: " + sceneName);
                return null;
            }

            WallpaperScene scene;
            if (sceneName.startsWith("DYN_IMG_")) {
                DynamicImageScene imgScene = new DynamicImageScene();
                imgScene.setDynamicId(id, context);
                scene = imgScene;
            } else {
                DynamicVideoScene vidScene = new DynamicVideoScene();
                vidScene.setDynamicId(id, context);
                scene = vidScene;
            }

            if (resourceManager != null) {
                scene.setResourceManager(resourceManager);
            }
            scene.onCreate(context, textureManager, camera);
            scene.setScreenSize(screenWidth, screenHeight);

            currentScene = scene;
            currentSceneName = sceneName;

            Log.d(TAG, "✅ Dynamic scene " + sceneName + " created");
            EventBus.get().publish(EventBus.SCENE_CHANGED,
                new EventBus.EventData().put("scene", sceneName));

            return scene;
        } catch (Exception e) {
            Log.e(TAG, "❌ Error creating dynamic scene: " + e.getMessage());
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

            // 🧠 Liberar texturas de archivo (Supabase downloads) inmediatamente.
            // Las texturas de recursos (drawable) se mantienen en caché.
            if (textureManager != null) {
                textureManager.releaseFileTextures();
            }
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

    /**
     * ¿La escena actual está completamente lista para renderizar?
     * Para escenas de video, requiere primer frame recibido.
     */
    public boolean isCurrentSceneReady() {
        return currentScene != null && currentScene.isReady();
    }

    public String getDefaultSceneName() {
        return defaultSceneName;
    }
}

