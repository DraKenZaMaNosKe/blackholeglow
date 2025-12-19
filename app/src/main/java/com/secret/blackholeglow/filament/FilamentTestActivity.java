package com.secret.blackholeglow.filament;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Choreographer;
import android.view.SurfaceView;

import com.google.android.filament.Camera;
import com.google.android.filament.Engine;
import com.google.android.filament.EntityManager;
import com.google.android.filament.LightManager;
import com.google.android.filament.Renderer;
import com.google.android.filament.Scene;
import com.google.android.filament.Skybox;
import com.google.android.filament.SwapChain;
import com.google.android.filament.View;
import com.google.android.filament.Viewport;
import com.google.android.filament.android.UiHelper;
import com.google.android.filament.gltfio.Animator;
import com.google.android.filament.gltfio.AssetLoader;
import com.google.android.filament.gltfio.FilamentAsset;
import com.google.android.filament.gltfio.MaterialProvider;
import com.google.android.filament.gltfio.ResourceLoader;
import com.google.android.filament.gltfio.UbershaderProvider;
import com.google.android.filament.gltfio.Gltfio;
import com.google.android.filament.Filament;
import com.google.android.filament.utils.Float3;
import com.google.android.filament.utils.Manipulator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   🎅 FilamentTestActivity - Prueba de renderizado de Santa Claus        ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Activity de prueba para validar que Filament carga el modelo GLB       ║
 * ║  con animaciones correctamente. Esto es solo para testing.              ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class FilamentTestActivity extends Activity {
    private static final String TAG = "FilamentTest";

    // Filament
    private Engine engine;
    private Renderer renderer;
    private Scene scene;
    private View view;
    private Camera camera;
    private SwapChain swapChain;
    private UiHelper uiHelper;

    // GLTF
    private AssetLoader assetLoader;
    private ResourceLoader resourceLoader;
    private MaterialProvider materialProvider;
    private FilamentAsset santaAsset;
    private Animator animator;

    // Lighting
    @SuppressWarnings("FieldCanBeLocal")
    private int sunLight;

    // Animation
    private long startTime;
    private int currentAnimation = 0;

    // Choreographer for frame sync
    private Choreographer choreographer;
    private Choreographer.FrameCallback frameCallback = this::onFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "═══════════════════════════════════════════════");
        Log.d(TAG, "🎅 FilamentTestActivity - Iniciando");
        Log.d(TAG, "═══════════════════════════════════════════════");

        // Crear SurfaceView
        SurfaceView surfaceView = new SurfaceView(this);
        setContentView(surfaceView);

        choreographer = Choreographer.getInstance();

        // Inicializar Filament
        initFilament(surfaceView);
    }

    private void initFilament(SurfaceView surfaceView) {
        // Inicializar librerías nativas de Filament
        Filament.init();
        Gltfio.init();
        Log.d(TAG, "✓ Filament + Gltfio nativos inicializados");

        // Engine
        engine = Engine.create();
        Log.d(TAG, "✓ Engine creado");

        // Renderer
        renderer = engine.createRenderer();
        renderer.setClearOptions(new Renderer.ClearOptions() {{
            clearColor = new float[]{0.1f, 0.15f, 0.2f, 1.0f}; // Azul oscuro
            clear = true;
        }});

        // Scene
        scene = engine.createScene();

        // View
        view = engine.createView();
        view.setScene(scene);

        // Camera
        int cameraEntity = EntityManager.get().create();
        camera = engine.createCamera(cameraEntity);
        view.setCamera(camera);

        // Setup camera - Santa SUPER pequeño, esquina inferior
        camera.setProjection(45.0, 1.0, 0.1, 100.0, Camera.Fov.VERTICAL);
        camera.lookAt(
                2.0, 3.0, -25.0,  // eye - SUPER lejos
                2.0, 0.0, 0.0,    // center - Santa en esquina derecha
                0.0, 1.0, 0.0     // up
        );
        Log.d(TAG, "✓ Cámara: Santa super pequeño");

        // Skybox - Color navideño (bosque nocturno)
        Skybox skybox = new Skybox.Builder()
                .color(0.05f, 0.08f, 0.12f, 1.0f)  // Azul oscuro nocturno
                .build(engine);
        scene.setSkybox(skybox);

        // Luz direccional - Luna/Nieve navideña
        sunLight = EntityManager.get().create();
        new LightManager.Builder(LightManager.Type.DIRECTIONAL)
                .color(0.8f, 0.85f, 1.0f)     // Luz fría azulada (luna)
                .intensity(80000.0f)           // Menos intensa (noche)
                .direction(-0.3f, -1.0f, 0.5f) // Desde arriba
                .castShadows(true)
                .build(engine, sunLight);
        scene.addEntity(sunLight);
        Log.d(TAG, "✓ Luz nocturna navideña agregada");

        // UiHelper para manejar la superficie
        uiHelper = new UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK);
        uiHelper.setRenderCallback(new UiHelper.RendererCallback() {
            @Override
            public void onNativeWindowChanged(android.view.Surface surface) {
                if (swapChain != null) {
                    engine.destroySwapChain(swapChain);
                }
                swapChain = engine.createSwapChain(surface);
            }

            @Override
            public void onDetachedFromSurface() {
                if (swapChain != null) {
                    engine.destroySwapChain(swapChain);
                    swapChain = null;
                }
            }

            @Override
            public void onResized(int width, int height) {
                view.setViewport(new Viewport(0, 0, width, height));
                double aspect = (double) width / height;
                camera.setProjection(45.0, aspect, 0.1, 100.0, Camera.Fov.VERTICAL);
            }
        });
        uiHelper.attachTo(surfaceView);

        // Load GLTF assets
        loadGltfLoader();
        loadSanta();

        startTime = System.nanoTime();

        // Start frame callback
        choreographer.postFrameCallback(frameCallback);
    }

    private void loadGltfLoader() {
        materialProvider = new UbershaderProvider(engine);
        assetLoader = new AssetLoader(engine, materialProvider, EntityManager.get());
        resourceLoader = new ResourceLoader(engine);
        Log.d(TAG, "✓ GLTF loader inicializado");
    }

    private void loadSanta() {
        try {
            // Leer archivo GLB - Probar animación de caminar
            InputStream inputStream = getAssets().open("santa_walk.glb");
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            inputStream.close();

            ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
            buffer.put(bytes);
            buffer.flip();

            // Crear asset
            santaAsset = assetLoader.createAsset(buffer);

            if (santaAsset == null) {
                Log.e(TAG, "❌ Error creando asset de Santa");
                return;
            }

            // Cargar recursos
            resourceLoader.loadResources(santaAsset);

            // Agregar a escena
            scene.addEntities(santaAsset.getEntities());

            // Obtener animator
            santaAsset.getInstance();
            animator = santaAsset.getInstance().getAnimator();

            Log.d(TAG, "✅ Santa cargado!");
            Log.d(TAG, "   Entidades: " + santaAsset.getEntities().length);

            if (animator != null) {
                int animCount = animator.getAnimationCount();
                Log.d(TAG, "   Animaciones: " + animCount);
                for (int i = 0; i < animCount; i++) {
                    String name = animator.getAnimationName(i);
                    float duration = animator.getAnimationDuration(i);
                    Log.d(TAG, "     [" + i + "] " + name + " (" + duration + "s)");
                }
            }

        } catch (IOException e) {
            Log.e(TAG, "❌ Error cargando santa.glb: " + e.getMessage(), e);
        }
    }

    private void onFrame(long frameTimeNanos) {
        // Calcular delta time
        float time = (frameTimeNanos - startTime) / 1_000_000_000f;

        // Actualizar animación
        if (animator != null) {
            float duration = animator.getAnimationDuration(currentAnimation);
            float animTime = time % duration;
            animator.applyAnimation(currentAnimation, animTime);
            animator.updateBoneMatrices();
        }

        // Renderizar
        if (uiHelper.isReadyToRender() && swapChain != null) {
            if (renderer.beginFrame(swapChain, frameTimeNanos)) {
                renderer.render(view);
                renderer.endFrame();
            }
        }

        // Siguiente frame
        choreographer.postFrameCallback(frameCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        choreographer.postFrameCallback(frameCallback);
    }

    @Override
    protected void onPause() {
        super.onPause();
        choreographer.removeFrameCallback(frameCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        choreographer.removeFrameCallback(frameCallback);

        if (santaAsset != null) {
            scene.removeEntities(santaAsset.getEntities());
            assetLoader.destroyAsset(santaAsset);
        }

        if (resourceLoader != null) resourceLoader.destroy();
        if (assetLoader != null) assetLoader.destroy();
        if (materialProvider != null) {
            materialProvider.destroyMaterials();
            materialProvider.destroy();
        }

        if (view != null) engine.destroyView(view);
        if (scene != null) engine.destroyScene(scene);
        if (renderer != null) engine.destroyRenderer(renderer);
        if (camera != null) engine.destroyCameraComponent(camera.getEntity());
        if (swapChain != null) engine.destroySwapChain(swapChain);
        if (engine != null) engine.destroy();

        Log.d(TAG, "🗑️ Recursos liberados");
    }
}
