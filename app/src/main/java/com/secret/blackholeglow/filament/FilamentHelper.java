package com.secret.blackholeglow.filament;

import android.content.Context;
import android.content.res.AssetManager;
import android.opengl.EGLContext;
import android.util.Log;
import android.view.Surface;

import com.google.android.filament.Camera;
import com.google.android.filament.Engine;
import com.google.android.filament.EntityManager;
import com.google.android.filament.Renderer;
import com.google.android.filament.Scene;
import com.google.android.filament.Skybox;
import com.google.android.filament.SwapChain;
import com.google.android.filament.View;
import com.google.android.filament.Viewport;
import com.google.android.filament.android.DisplayHelper;
import com.google.android.filament.android.UiHelper;
import com.google.android.filament.gltfio.Animator;
import com.google.android.filament.gltfio.AssetLoader;
import com.google.android.filament.gltfio.FilamentAsset;
import com.google.android.filament.gltfio.MaterialProvider;
import com.google.android.filament.gltfio.ResourceLoader;
import com.google.android.filament.gltfio.UbershaderProvider;
import com.google.android.filament.utils.AutomationEngine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   🎬 FilamentHelper - Motor de renderizado para modelos GLB animados    ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Usa Google Filament para cargar y renderizar modelos GLB/GLTF con      ║
 * ║  soporte completo para:                                                  ║
 * ║  • Animaciones con huesos (skeletal animation)                          ║
 * ║  • Materiales PBR                                                        ║
 * ║  • Múltiples animaciones por modelo                                     ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class FilamentHelper {
    private static final String TAG = "FilamentHelper";

    // Filament core components
    private Engine engine;
    private Renderer renderer;
    private Scene scene;
    private View view;
    private Camera camera;
    private SwapChain swapChain;

    // GLTF loading
    private AssetLoader assetLoader;
    private ResourceLoader resourceLoader;
    private MaterialProvider materialProvider;

    // Context
    private Context context;
    private boolean initialized = false;

    // Screen size
    private int width = 1080;
    private int height = 1920;

    /**
     * Inicializa Filament con el contexto de la app
     */
    public FilamentHelper(Context context) {
        this.context = context;
        Log.d(TAG, "═══════════════════════════════════════════════");
        Log.d(TAG, "🎬 Inicializando Filament Helper");
        Log.d(TAG, "═══════════════════════════════════════════════");
    }

    /**
     * Inicializa el engine de Filament
     * DEBE llamarse desde el GL thread
     */
    public void initialize() {
        if (initialized) {
            Log.w(TAG, "⚠️ Filament ya está inicializado");
            return;
        }

        try {
            // Crear engine
            engine = Engine.create();
            Log.d(TAG, "✓ Engine creado");

            // Crear renderer
            renderer = engine.createRenderer();
            Log.d(TAG, "✓ Renderer creado");

            // Crear scene
            scene = engine.createScene();
            Log.d(TAG, "✓ Scene creada");

            // Crear view
            view = engine.createView();
            view.setScene(scene);
            Log.d(TAG, "✓ View creada");

            // Crear camera
            int cameraEntity = EntityManager.get().create();
            camera = engine.createCamera(cameraEntity);
            view.setCamera(camera);
            Log.d(TAG, "✓ Camera creada");

            // Configurar skybox transparente (para mezclar con OpenGL existente)
            Skybox skybox = new Skybox.Builder()
                    .color(0f, 0f, 0f, 0f)  // Transparente
                    .build(engine);
            scene.setSkybox(skybox);

            // Inicializar GLTF loader
            initGltfLoader();

            initialized = true;
            Log.d(TAG, "✅ Filament inicializado correctamente");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error inicializando Filament: " + e.getMessage(), e);
        }
    }

    /**
     * Inicializa el cargador de archivos GLTF/GLB
     */
    private void initGltfLoader() {
        // Material provider para shaders
        materialProvider = new UbershaderProvider(engine);
        Log.d(TAG, "✓ MaterialProvider creado");

        // Asset loader para cargar GLB
        assetLoader = new AssetLoader(engine, materialProvider, EntityManager.get());
        Log.d(TAG, "✓ AssetLoader creado");

        // Resource loader para texturas y buffers
        resourceLoader = new ResourceLoader(engine);
        Log.d(TAG, "✓ ResourceLoader creado");
    }

    /**
     * Carga un modelo GLB desde assets
     * @param assetPath Ruta del archivo en assets (ej: "santa_idle.glb")
     * @return FilamentAsset cargado o null si falla
     */
    public FilamentAsset loadGlbFromAssets(String assetPath) {
        if (!initialized) {
            Log.e(TAG, "❌ Filament no está inicializado");
            return null;
        }

        try {
            // Leer archivo desde assets
            AssetManager assets = context.getAssets();
            InputStream inputStream = assets.open(assetPath);

            // Leer todos los bytes
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            inputStream.close();

            // Crear ByteBuffer
            ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
            buffer.put(bytes);
            buffer.flip();

            // Cargar asset
            FilamentAsset asset = assetLoader.createAsset(buffer);

            if (asset == null) {
                Log.e(TAG, "❌ Error creando asset desde: " + assetPath);
                return null;
            }

            // Cargar recursos (texturas, etc)
            resourceLoader.loadResources(asset);

            // Agregar a la escena
            scene.addEntities(asset.getEntities());

            Log.d(TAG, "✅ Modelo cargado: " + assetPath);
            Log.d(TAG, "   Entidades: " + asset.getEntities().length);

            // Crear instancia para animaciones
            asset.getInstance();

            // Log animaciones disponibles
            Animator animator = asset.getInstance().getAnimator();
            if (animator != null) {
                int animCount = animator.getAnimationCount();
                Log.d(TAG, "   Animaciones: " + animCount);
                for (int i = 0; i < animCount; i++) {
                    String animName = animator.getAnimationName(i);
                    float duration = animator.getAnimationDuration(i);
                    Log.d(TAG, "     [" + i + "] " + animName + " (" + duration + "s)");
                }
            } else {
                Log.d(TAG, "   Sin animaciones");
            }

            return asset;

        } catch (IOException e) {
            Log.e(TAG, "❌ Error cargando GLB: " + assetPath, e);
            return null;
        }
    }

    /**
     * Configura la cámara de Filament
     */
    public void setupCamera(float eyeX, float eyeY, float eyeZ,
                            float targetX, float targetY, float targetZ) {
        if (camera == null) return;

        camera.lookAt(
                eyeX, eyeY, eyeZ,      // eye position
                targetX, targetY, targetZ,  // target
                0, 1, 0                 // up vector
        );

        // Configurar proyección perspectiva
        float aspect = (float) width / height;
        camera.setProjection(45.0, aspect, 0.1, 100.0, Camera.Fov.VERTICAL);
    }

    /**
     * Actualiza el viewport cuando cambia el tamaño de pantalla
     */
    public void setViewport(int width, int height) {
        this.width = width;
        this.height = height;

        if (view != null) {
            view.setViewport(new Viewport(0, 0, width, height));
        }

        // Actualizar proyección de cámara
        if (camera != null) {
            float aspect = (float) width / height;
            camera.setProjection(45.0, aspect, 0.1, 100.0, Camera.Fov.VERTICAL);
        }
    }

    /**
     * Renderiza un frame
     * @param deltaTime Tiempo desde el último frame en segundos
     */
    public void render(float deltaTime) {
        if (!initialized || renderer == null) return;

        // Renderizar
        if (renderer.beginFrame(swapChain, 0)) {
            renderer.render(view);
            renderer.endFrame();
        }
    }

    /**
     * Limpia recursos de un asset
     */
    public void destroyAsset(FilamentAsset asset) {
        if (asset == null) return;

        scene.removeEntities(asset.getEntities());
        assetLoader.destroyAsset(asset);
    }

    /**
     * Libera todos los recursos de Filament
     */
    public void destroy() {
        Log.d(TAG, "🗑️ Destruyendo Filament...");

        if (resourceLoader != null) {
            resourceLoader.destroy();
            resourceLoader = null;
        }

        if (assetLoader != null) {
            assetLoader.destroy();
            assetLoader = null;
        }

        if (materialProvider != null) {
            materialProvider.destroyMaterials();
            materialProvider.destroy();
            materialProvider = null;
        }

        if (view != null) {
            engine.destroyView(view);
            view = null;
        }

        if (scene != null) {
            engine.destroyScene(scene);
            scene = null;
        }

        if (renderer != null) {
            engine.destroyRenderer(renderer);
            renderer = null;
        }

        if (camera != null) {
            engine.destroyCameraComponent(camera.getEntity());
            camera = null;
        }

        if (engine != null) {
            engine.destroy();
            engine = null;
        }

        initialized = false;
        Log.d(TAG, "✅ Filament destruido");
    }

    // Getters
    public Engine getEngine() { return engine; }
    public Scene getScene() { return scene; }
    public View getView() { return view; }
    public Camera getCamera() { return camera; }
    public boolean isInitialized() { return initialized; }
}
