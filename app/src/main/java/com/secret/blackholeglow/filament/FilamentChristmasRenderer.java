package com.secret.blackholeglow.filament;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.Choreographer;
import android.view.SurfaceHolder;

import com.google.android.filament.Camera;
import com.google.android.filament.Engine;
import com.google.android.filament.EntityManager;
import com.google.android.filament.Filament;
import com.google.android.filament.LightManager;
import com.google.android.filament.Material;
import com.google.android.filament.MaterialInstance;
import com.google.android.filament.RenderableManager;
import com.google.android.filament.Renderer;
import com.google.android.filament.Scene;
import com.google.android.filament.Skybox;
import com.google.android.filament.SwapChain;
import com.google.android.filament.Texture;
import com.google.android.filament.TextureSampler;
import com.google.android.filament.TransformManager;
import com.google.android.filament.View;
import com.google.android.filament.Viewport;
import com.google.android.filament.gltfio.Animator;
import com.google.android.filament.gltfio.AssetLoader;
import com.google.android.filament.gltfio.FilamentAsset;
import com.google.android.filament.gltfio.Gltfio;
import com.google.android.filament.gltfio.MaterialProvider;
import com.google.android.filament.gltfio.ResourceLoader;
import com.google.android.filament.gltfio.UbershaderProvider;

import com.secret.blackholeglow.R;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   🎄 FilamentChristmasRenderer - Escena Navideña con Filament           ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Renderiza la escena de Navidad completa usando Google Filament:        ║
 * ║  • Fondo de bosque navideño                                             ║
 * ║  • Santa Claus con 3 animaciones (caminar, saludar, mirar)             ║
 * ║  • Iluminación nocturna                                                 ║
 * ║  • Ciclo automático de animaciones                                      ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class FilamentChristmasRenderer {
    private static final String TAG = "FilamentChristmas";

    // Context
    private Context context;
    private boolean initialized = false;

    // Filament Core
    private Engine engine;
    private Renderer renderer;
    private Scene scene;
    private View view;
    private Camera camera;
    private SwapChain swapChain;

    // GLTF Loading
    private AssetLoader assetLoader;
    private ResourceLoader resourceLoader;
    private MaterialProvider materialProvider;

    // Santa - Current animation
    private FilamentAsset santaAsset;
    private Animator santaAnimator;
    private int currentAnimationIndex = 0;

    // Background - Plano con textura del bosque navideño
    private FilamentAsset backgroundAsset;
    private static final String BACKGROUND_FILE = "christmas_background.glb";

    // Christmas Tree - Árbol de navidad
    private FilamentAsset treeAsset;
    private static final String TREE_FILE = "christmas_tree.glb";

    // Animation files
    private static final String[] ANIMATION_FILES = {
        "santa_walk.glb",   // 0 - Caminando
        "santa_wave.glb",   // 1 - Saludando
        "santa_look.glb"    // 2 - Mirando a los lados
    };
    private static final String[] ANIMATION_NAMES = {
        "Caminando", "Saludando", "Mirando"
    };
    private int currentFileIndex = 0;
    private float animationCycleTime = 0f;
    private static final float ANIMATION_CYCLE_DURATION = 10f; // Cambiar cada 10 segundos

    // Walking movement - Santa camina por un sendero natural
    private float santaX = 2f;           // Posición X actual (empieza a la derecha)
    private float santaY = -1.5f;        // Posición Y (altura - más abajo en la nieve)
    private float santaZ = 0f;           // Posición Z (profundidad)
    private float santaRotation = 0f;    // Rotación actual (hacia donde mira)
    private int currentWaypoint = 0;     // Punto actual del sendero
    private static final float WALK_SPEED = 0.5f;    // Velocidad de caminata
    private TransformManager transformManager;

    // Sendero natural por la nieve (waypoints X, Y, Z)
    // Sigue el camino visible en la imagen del fondo
    private static final float[][] PATH_WAYPOINTS = {
        { 2.0f, -1.5f,  0.0f},   // 0: Inicio derecha (frente)
        { 0.0f, -1.2f,  3.0f},   // 1: Centro, un poco atrás
        {-1.5f, -1.0f,  5.0f},   // 2: Izquierda, hacia la cabaña
        { 0.0f, -1.2f,  3.0f},   // 3: Regreso al centro
        { 2.0f, -1.5f,  0.0f},   // 4: Regreso a inicio
    };

    // Lighting
    private int sunLight;

    // Animation timing
    private long startTime;
    private int width = 1080;
    private int height = 1920;

    // Choreographer
    private Choreographer choreographer;
    private Choreographer.FrameCallback frameCallback;
    private boolean isRunning = false;

    public FilamentChristmasRenderer(Context context) {
        this.context = context;
        Log.d(TAG, "═══════════════════════════════════════════════");
        Log.d(TAG, "🎄 FilamentChristmasRenderer creado");
        Log.d(TAG, "═══════════════════════════════════════════════");
    }

    /**
     * Inicializa Filament - llamar desde el thread principal
     */
    public void initialize(SurfaceHolder holder) {
        if (initialized) return;

        try {
            // Inicializar librerías nativas
            Filament.init();
            Gltfio.init();
            Log.d(TAG, "✓ Librerías nativas inicializadas");

            // Engine
            engine = Engine.create();
            transformManager = engine.getTransformManager();
            Log.d(TAG, "✓ Engine creado");

            // Renderer con color de fondo navideño
            renderer = engine.createRenderer();
            renderer.setClearOptions(new Renderer.ClearOptions() {{
                // Color del bosque navideño nocturno
                clearColor = new float[]{0.02f, 0.06f, 0.12f, 1.0f};
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
            setupCamera();

            // Skybox con gradiente navideño
            Skybox skybox = new Skybox.Builder()
                    .color(0.02f, 0.06f, 0.12f, 1.0f)
                    .build(engine);
            scene.setSkybox(skybox);

            // Luz
            setupLighting();

            // GLTF Loader
            setupGltfLoader();

            // Cargar fondo del bosque navideño
            loadBackground();

            // Cargar árbol de navidad
            loadChristmasTree();

            // Cargar primera animación de Santa
            loadSantaAnimation(currentFileIndex);

            // SwapChain
            swapChain = engine.createSwapChain(holder.getSurface());

            startTime = System.nanoTime();
            initialized = true;

            Log.d(TAG, "✅ FilamentChristmasRenderer inicializado");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error inicializando: " + e.getMessage(), e);
        }
    }

    private void setupCamera() {
        // Santa pequeño en la parte inferior-derecha
        camera.setProjection(45.0, 1.0, 0.1, 100.0, Camera.Fov.VERTICAL);
        camera.lookAt(
                2.0, 3.0, -25.0,  // eye - lejos para Santa pequeño
                2.0, 0.0, 0.0,    // center - Santa a la derecha
                0.0, 1.0, 0.0     // up
        );
        Log.d(TAG, "✓ Cámara: Santa pequeño en esquina");
    }

    private void setupLighting() {
        // Luz principal - Luna fría navideña
        sunLight = EntityManager.get().create();
        new LightManager.Builder(LightManager.Type.DIRECTIONAL)
                .color(0.7f, 0.8f, 1.0f)      // Azul frío (luna)
                .intensity(80000.0f)
                .direction(-0.3f, -1.0f, 0.5f)
                .castShadows(true)
                .build(engine, sunLight);
        scene.addEntity(sunLight);
        Log.d(TAG, "✓ Iluminación nocturna configurada");
    }

    private void setupGltfLoader() {
        materialProvider = new UbershaderProvider(engine);
        assetLoader = new AssetLoader(engine, materialProvider, EntityManager.get());
        resourceLoader = new ResourceLoader(engine);
        Log.d(TAG, "✓ GLTF loader configurado");
    }

    /**
     * Carga el fondo del bosque navideño (plano con textura)
     */
    private void loadBackground() {
        Log.d(TAG, "🌲 Cargando fondo: " + BACKGROUND_FILE);

        try {
            InputStream inputStream = context.getAssets().open(BACKGROUND_FILE);
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            inputStream.close();

            ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
            buffer.put(bytes);
            buffer.flip();

            backgroundAsset = assetLoader.createAsset(buffer);
            if (backgroundAsset == null) {
                Log.e(TAG, "❌ Error cargando fondo");
                return;
            }

            resourceLoader.loadResources(backgroundAsset);
            scene.addEntities(backgroundAsset.getEntities());

            // Posicionar el fondo detrás de Santa
            positionBackground();

            Log.d(TAG, "✅ Fondo cargado - Entidades: " + backgroundAsset.getEntities().length);

        } catch (IOException e) {
            Log.e(TAG, "❌ Error cargando fondo: " + e.getMessage());
        }
    }

    /**
     * Posiciona y escala el fondo para que llene EXACTAMENTE la pantalla
     * Calcula el tamaño basado en la geometría de la cámara
     */
    private void positionBackground() {
        if (backgroundAsset == null || transformManager == null) return;

        int rootEntity = backgroundAsset.getRoot();
        int transformInstance = transformManager.getInstance(rootEntity);

        if (transformInstance == 0) return;

        // Posición del fondo (detrás de Santa)
        float posZ = 40.0f;

        // La cámara está en Z=-25, mirando hacia Z=0
        // Distancia desde cámara al fondo = posZ - (-25) = posZ + 25
        float cameraZ = -25.0f;
        float distance = posZ - cameraZ;

        // FOV vertical = 45 grados
        float fovRadians = (float) Math.toRadians(45.0 / 2.0);

        // Altura visible a esa distancia: h = 2 * d * tan(fov/2)
        float visibleHeight = 2.0f * distance * (float) Math.tan(fovRadians);

        // Ancho visible basado en aspect ratio de pantalla
        float aspectRatio = (float) width / (float) height;
        float visibleWidth = visibleHeight * aspectRatio;

        // El plano en Blender es 9x16 unidades (después del escalado)
        // Necesitamos escalar para que cubra el área visible
        float planeWidth = 9.0f;   // Ancho del plano en Blender
        float planeHeight = 16.0f; // Alto del plano en Blender

        // Calcular escala para mostrar TODA la imagen (sin recortar)
        float scaleX = visibleWidth / planeWidth;
        float scaleY = visibleHeight / planeHeight;

        // Usar la escala MENOR para que toda la imagen sea visible
        float scale = Math.min(scaleX, scaleY);

        // Centrar con la cámara (cámara mira a X=2, Y=0)
        float posX = 2.0f;
        float posY = 0.0f;  // Centrado verticalmente

        // Matriz de transformación: escala + rotación 90° en X + traslación
        float[] transform = new float[] {
            scale, 0f,    0f,    0f,   // columna 0
            0f,    0f,    scale, 0f,   // columna 1 (rotación Y→Z)
            0f,    -scale, 0f,   0f,   // columna 2 (rotación Z→-Y)
            posX,  posY,  posZ,  1f    // columna 3 (traslación)
        };

        transformManager.setTransform(transformInstance, transform);
        Log.d(TAG, "✓ Fondo: dist=" + distance + " visible=" + visibleWidth + "x" + visibleHeight + " scale=" + scale);
    }

    /**
     * Carga el árbol de navidad
     */
    private void loadChristmasTree() {
        Log.d(TAG, "🎄 Cargando árbol: " + TREE_FILE);

        try {
            InputStream inputStream = context.getAssets().open(TREE_FILE);
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            inputStream.close();

            ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
            buffer.put(bytes);
            buffer.flip();

            treeAsset = assetLoader.createAsset(buffer);
            if (treeAsset == null) {
                Log.e(TAG, "❌ Error cargando árbol");
                return;
            }

            resourceLoader.loadResources(treeAsset);
            scene.addEntities(treeAsset.getEntities());

            // Posicionar el árbol
            positionTree();

            Log.d(TAG, "✅ Árbol cargado - Entidades: " + treeAsset.getEntities().length);

        } catch (IOException e) {
            Log.e(TAG, "❌ Error cargando árbol: " + e.getMessage());
        }
    }

    /**
     * Posiciona el árbol de navidad en la escena
     */
    private void positionTree() {
        if (treeAsset == null || transformManager == null) return;

        int rootEntity = treeAsset.getRoot();
        int transformInstance = transformManager.getInstance(rootEntity);

        if (transformInstance == 0) return;

        // Posición del árbol (a la derecha del sendero)
        float scale = 0.8f;   // Tamaño del árbol
        float posX = 3.5f;    // A la derecha
        float posY = -2.0f;   // En el suelo
        float posZ = 2.0f;    // Un poco atrás

        // Matriz de transformación simple (solo escala y traslación)
        float[] transform = new float[] {
            scale, 0f,    0f,    0f,
            0f,    scale, 0f,    0f,
            0f,    0f,    scale, 0f,
            posX,  posY,  posZ,  1f
        };

        transformManager.setTransform(transformInstance, transform);
        Log.d(TAG, "✓ Árbol posicionado: (" + posX + ", " + posY + ", " + posZ + ") escala=" + scale);
    }

    /**
     * Carga una animación de Santa desde assets
     */
    private void loadSantaAnimation(int fileIndex) {
        // Limpiar asset anterior si existe
        if (santaAsset != null) {
            scene.removeEntities(santaAsset.getEntities());
            assetLoader.destroyAsset(santaAsset);
            santaAsset = null;
            santaAnimator = null;
        }

        String filename = ANIMATION_FILES[fileIndex];
        Log.d(TAG, "🎅 Cargando animación: " + ANIMATION_NAMES[fileIndex] + " (" + filename + ")");

        try {
            InputStream inputStream = context.getAssets().open(filename);
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            inputStream.close();

            ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
            buffer.put(bytes);
            buffer.flip();

            santaAsset = assetLoader.createAsset(buffer);
            if (santaAsset == null) {
                Log.e(TAG, "❌ Error cargando: " + filename);
                return;
            }

            resourceLoader.loadResources(santaAsset);
            scene.addEntities(santaAsset.getEntities());

            // Animator
            santaAsset.getInstance();
            santaAnimator = santaAsset.getInstance().getAnimator();

            if (santaAnimator != null) {
                Log.d(TAG, "✅ " + ANIMATION_NAMES[fileIndex] + " cargado - Duración: " +
                      santaAnimator.getAnimationDuration(0) + "s");
            }

            currentFileIndex = fileIndex;

        } catch (IOException e) {
            Log.e(TAG, "❌ Error: " + e.getMessage());
        }
    }

    /**
     * Cambia a la siguiente animación
     */
    public void nextAnimation() {
        int nextIndex = (currentFileIndex + 1) % ANIMATION_FILES.length;
        loadSantaAnimation(nextIndex);
        animationCycleTime = 0f;
    }

    /**
     * Cambia a una animación específica
     * @param index 0=walk, 1=wave, 2=look
     */
    public void setAnimation(int index) {
        if (index >= 0 && index < ANIMATION_FILES.length && index != currentFileIndex) {
            loadSantaAnimation(index);
            animationCycleTime = 0f;
        }
    }

    /**
     * Actualiza el viewport cuando cambia el tamaño
     */
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;

        if (view != null) {
            view.setViewport(new Viewport(0, 0, width, height));
        }
        if (camera != null) {
            double aspect = (double) width / height;
            camera.setProjection(45.0, aspect, 0.1, 100.0, Camera.Fov.VERTICAL);
        }

        // Recalcular posición del fondo para nuevas dimensiones
        positionBackground();

        Log.d(TAG, "📐 Viewport: " + width + "x" + height);
    }

    /**
     * Renderiza un frame
     */
    public void render(long frameTimeNanos) {
        if (!initialized || swapChain == null) return;

        float deltaTime = 0.016f; // ~60fps
        float time = (frameTimeNanos - startTime) / 1_000_000_000f;

        // Actualizar animación de Santa
        if (santaAnimator != null) {
            float duration = santaAnimator.getAnimationDuration(0);
            float animTime = time % duration;
            santaAnimator.applyAnimation(0, animTime);
            santaAnimator.updateBoneMatrices();
        }

        // Mover a Santa cuando está caminando (animación 0)
        if (currentFileIndex == 0 && santaAsset != null) {
            // Obtener waypoint destino
            float[] target = PATH_WAYPOINTS[currentWaypoint];
            float targetX = target[0];
            float targetY = target[1];
            float targetZ = target[2];

            // Calcular dirección hacia el waypoint
            float dx = targetX - santaX;
            float dy = targetY - santaY;
            float dz = targetZ - santaZ;
            float distance = (float) Math.sqrt(dx * dx + dz * dz);

            // Si llegamos al waypoint, ir al siguiente
            if (distance < 0.1f) {
                currentWaypoint = (currentWaypoint + 1) % PATH_WAYPOINTS.length;
            } else {
                // Mover hacia el waypoint
                float moveX = (dx / distance) * WALK_SPEED * deltaTime;
                float moveY = (dy / distance) * WALK_SPEED * deltaTime * 0.5f; // Y más lento
                float moveZ = (dz / distance) * WALK_SPEED * deltaTime;

                santaX += moveX;
                santaY += moveY;
                santaZ += moveZ;

                // Calcular rotación para mirar hacia donde camina
                // +PI porque el modelo de Santa mira hacia -Z por defecto
                santaRotation = (float) Math.atan2(dx, dz) + (float) Math.PI;
            }

            // Aplicar transformación al root entity
            updateSantaTransform();
        }

        // Ciclo automático de animaciones (cada 10 segundos)
        animationCycleTime += deltaTime;
        if (animationCycleTime >= ANIMATION_CYCLE_DURATION) {
            nextAnimation();
        }

        // Renderizar
        if (renderer.beginFrame(swapChain, frameTimeNanos)) {
            renderer.render(view);
            renderer.endFrame();
        }
    }

    /**
     * Aplica la transformación de posición y rotación a Santa
     * Santa mira hacia donde camina (usa santaRotation calculado de la dirección)
     */
    private void updateSantaTransform() {
        if (santaAsset == null || transformManager == null) return;

        int rootEntity = santaAsset.getRoot();
        int transformInstance = transformManager.getInstance(rootEntity);

        if (transformInstance == 0) return;

        // Matriz de rotación en Y (Santa mira hacia donde camina)
        float cosY = (float) Math.cos(santaRotation);
        float sinY = (float) Math.sin(santaRotation);

        // Matriz 4x4 en formato column-major (como OpenGL)
        float[] transform = new float[] {
            cosY,  0f, sinY, 0f,      // columna 0
            0f,    1f, 0f,   0f,      // columna 1
            -sinY, 0f, cosY, 0f,      // columna 2
            santaX, santaY, santaZ, 1f // columna 3 (traslación X, Y, Z)
        };

        transformManager.setTransform(transformInstance, transform);
    }

    /**
     * Iniciar loop de renderizado
     */
    public void start() {
        if (isRunning) return;
        isRunning = true;

        choreographer = Choreographer.getInstance();
        frameCallback = new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                if (isRunning) {
                    render(frameTimeNanos);
                    choreographer.postFrameCallback(this);
                }
            }
        };
        choreographer.postFrameCallback(frameCallback);
        Log.d(TAG, "▶️ Renderizado iniciado");
    }

    /**
     * Detener loop de renderizado
     */
    public void stop() {
        isRunning = false;
        if (choreographer != null && frameCallback != null) {
            choreographer.removeFrameCallback(frameCallback);
        }
        Log.d(TAG, "⏹️ Renderizado detenido");
    }

    /**
     * Liberar recursos
     */
    public void destroy() {
        stop();

        // Limpiar Santa
        if (santaAsset != null) {
            scene.removeEntities(santaAsset.getEntities());
            assetLoader.destroyAsset(santaAsset);
        }

        // Limpiar fondo
        if (backgroundAsset != null) {
            scene.removeEntities(backgroundAsset.getEntities());
            assetLoader.destroyAsset(backgroundAsset);
        }

        // Limpiar árbol
        if (treeAsset != null) {
            scene.removeEntities(treeAsset.getEntities());
            assetLoader.destroyAsset(treeAsset);
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

        initialized = false;
        Log.d(TAG, "🗑️ Recursos liberados");
    }

    /**
     * Actualizar superficie cuando cambia
     */
    public void onSurfaceChanged(android.view.Surface surface) {
        if (engine != null) {
            if (swapChain != null) {
                engine.destroySwapChain(swapChain);
            }
            swapChain = engine.createSwapChain(surface);
        }
    }

    // Getters
    public boolean isInitialized() { return initialized; }
    public int getCurrentAnimationIndex() { return currentFileIndex; }
    public String getCurrentAnimationName() {
        return ANIMATION_NAMES[currentFileIndex];
    }
}
