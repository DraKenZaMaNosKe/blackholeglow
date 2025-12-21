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
import com.google.android.filament.gltfio.FilamentInstance;
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
    private Skybox skybox;  // 🌤️ Cielo

    // 🎥 CAMERA CONTROLLER - Estilo FPS/COD Mobile
    private ChristmasCameraController cameraController;

    // GLTF Loading
    private AssetLoader assetLoader;
    private ResourceLoader resourceLoader;
    private MaterialProvider materialProvider;

    // ═══════════════════════════════════════════════════════════════════
    // 🎅 SISTEMA PRE-CARGA: Todas las animaciones cargadas al inicio
    // ═══════════════════════════════════════════════════════════════════
    private FilamentAsset[] santaAssets = new FilamentAsset[3];  // walk, wave, look
    private Animator[] santaAnimators = new Animator[3];
    private int currentAnimationIndex = 0;  // Cuál está visible ahora

    // Terrain y Tree (Mundo 3D)
    private FilamentAsset terrainAsset;
    private static final String TERRAIN_FILE = "terrain.glb";  // Terreno 3D nevado
    private FilamentAsset treeAsset;
    private static final String TREE_FILE = "christmas_tree.glb";

    // Animation files
    private static final String[] ANIMATION_FILES = {
        "santa_walk.glb",   // 0 - Caminando
        "santa_wave.glb",   // 1 - Saludando
        "santa_look.glb"    // 2 - Mirando
    };
    private static final String[] ANIMATION_NAMES = {"Caminando", "Saludando", "Mirando"};

    private float animationCycleTime = 0f;
    private static final float ANIMATION_CYCLE_DURATION = 30f; // 30 seg caminando

    // ═══════════════════════════════════════════════════════════════════
    // 🤖 IA DE EXPLORACIÓN: Santa camina inteligentemente
    // ═══════════════════════════════════════════════════════════════════
    private float santaX = 0.0f;
    private float santaY = 0.3f;   // Sobre el terreno (más bajo)
    private float santaZ = 4.0f;   // Más adentro del terreno
    private float santaRotation = 0f;
    private static final float SANTA_SCALE = 0.12f;  // Santa pequeño como humano a distancia
    private int currentWaypoint = 0;
    private int targetWaypoint = 0;      // Destino elegido por IA
    private static final float WALK_SPEED = 0.15f;   // Más lento para verse natural
    private TransformManager transformManager;
    private java.util.Random random = new java.util.Random();

    // 🗺️ SENDERO COMPLETO - Circuito sobre el terreno 3D
    // Todos los puntos a Y=0.3 (sobre el terreno)
    private static final float[][] PATH_WAYPOINTS = {
        // ═══ FRENTE (cerca de cámara) ═══
        { 1.0f, 0.3f,  3.0f},    // 0: Derecha frontal
        { 0.0f, 0.3f,  3.5f},    // 1: Centro
        {-1.0f, 0.3f,  3.0f},    // 2: Izquierda frontal

        // ═══ CENTRO DEL TERRENO ═══
        {-1.5f, 0.3f,  5.0f},    // 3: Izquierda
        { 0.0f, 0.3f,  6.0f},    // 4: Centro
        { 1.5f, 0.3f,  5.0f},    // 5: Derecha

        // ═══ FONDO DEL TERRENO ═══
        { 1.0f, 0.3f,  7.0f},    // 6: Derecha-fondo
        { 0.0f, 0.3f,  8.0f},    // 7: Centro-fondo
        {-1.0f, 0.3f,  7.0f},    // 8: Izquierda-fondo

        // ═══ REGRESO ═══
        {-0.5f, 0.3f,  5.5f},    // 9: Bajando
        { 0.5f, 0.3f,  4.5f},    // 10: Centro
        { 0.0f, 0.3f,  4.0f},    // 11: Centro-frente
    };

    // Para IA: índices de waypoints por zona de interés
    private static final int[] FRONT_WAYPOINTS = {0, 1, 2, 11};     // Frente (cerca de cámara)
    private static final int[] CENTER_WAYPOINTS = {3, 4, 5};        // Centro del terreno
    private static final int[] BACK_WAYPOINTS = {6, 7, 8};          // Fondo del terreno
    private static final int[] PATH_WAYPOINTS_ALL;                   // Todos

    static {
        PATH_WAYPOINTS_ALL = new int[PATH_WAYPOINTS.length];
        for (int i = 0; i < PATH_WAYPOINTS.length; i++) {
            PATH_WAYPOINTS_ALL[i] = i;
        }
    }

    // Lighting
    private int sunLight;

    // ═══════════════════════════════════════════════════════════════════
    // ✨ MAGIC ORBS - Bolitas brillantes flotantes con efecto de viento
    // ═══════════════════════════════════════════════════════════════════
    private static final String MAGIC_ORB_FILE = "magic_orb.glb";
    private static final int NUM_ORBS = 15;  // Más bolitas para llenar la escena
    private static final float ORB_SCALE_MIN = 0.15f;  // Tamaño mínimo
    private static final float ORB_SCALE_MAX = 0.45f;  // Tamaño máximo

    private FilamentAsset[] orbAssets = new FilamentAsset[NUM_ORBS];
    private float[] orbBaseX = new float[NUM_ORBS];   // Posición base X
    private float[] orbBaseY = new float[NUM_ORBS];   // Posición base Y
    private float[] orbBaseZ = new float[NUM_ORBS];   // Posición base Z
    private float[] orbScale = new float[NUM_ORBS];   // Escala individual de cada bolita
    private float[] orbPhase = new float[NUM_ORBS];     // Fase para movimiento ondulante
    private float[] orbSpeedX = new float[NUM_ORBS];    // Velocidad horizontal
    private float[] orbSpeedY = new float[NUM_ORBS];    // Velocidad vertical
    private float[] orbAmplitude = new float[NUM_ORBS]; // Amplitud del movimiento

    // ═══════════════════════════════════════════════════════════════════
    // 🎄 CHRISTMAS TREE - Posición y escala configurables
    // ═══════════════════════════════════════════════════════════════════
    private float treeX = -1.5f;   // A la izquierda
    private float treeY = 0.2f;    // Sobre el terreno
    private float treeZ = 3.0f;    // Cerca del frente
    private float treeScale = 0.35f;  // Árbol proporcional al nuevo Santa

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

            // ✨ Activar BLOOM para que las bolitas brillen mágicamente
            View.BloomOptions bloomOptions = new View.BloomOptions();
            bloomOptions.enabled = true;
            bloomOptions.strength = 0.3f;      // Intensidad del brillo (0.0 - 1.0)
            bloomOptions.threshold = true;     // Solo objetos brillantes
            bloomOptions.levels = 6;           // Niveles de blur
            bloomOptions.blendMode = View.BloomOptions.BlendMode.ADD;
            view.setBloomOptions(bloomOptions);
            Log.d(TAG, "✨ Bloom activado para efectos mágicos");

            // Camera
            int cameraEntity = EntityManager.get().create();
            camera = engine.createCamera(cameraEntity);
            view.setCamera(camera);
            setupCamera();

            // 🌤️ CIELO INVERNAL - Azul claro brillante
            skybox = new Skybox.Builder()
                    .color(0.4f, 0.6f, 0.9f, 1.0f)  // Azul cielo invernal
                    .build(engine);
            scene.setSkybox(skybox);
            Log.d(TAG, "✓ Cielo azul invernal configurado");

            // Luz
            setupLighting();

            // GLTF Loader
            setupGltfLoader();

            // ✨ Cargar bolitas mágicas (después del loader)
            setupMagicOrbs();

            // Cargar terreno 3D nevado
            loadTerrain();

            // Cargar árbol de navidad
            loadChristmasTree();

            // 🎅 PRE-CARGAR todas las animaciones de Santa (cambio instantáneo)
            preloadAllSantaAnimations();

            // Elegir primer destino inteligente
            chooseNextDestination();

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
        // 🎥 Inicializar el controlador de cámara FPS
        cameraController = new ChristmasCameraController(camera);
        cameraController.setAspectRatio((float) width / height);

        // Modo: recorrido automático por el terreno
        cameraController.setMode(ChristmasCameraController.CameraMode.AUTO_WALK);

        Log.d(TAG, "✓ Cámara FPS inicializada - Recorrido automático");
    }

    private void setupLighting() {
        // ☀️ SOL BRILLANTE - Iluminación de día invernal
        sunLight = EntityManager.get().create();
        new LightManager.Builder(LightManager.Type.DIRECTIONAL)
                .color(1.0f, 0.95f, 0.85f)    // Luz cálida de sol invernal
                .intensity(120000.0f)          // Intensidad de sol real
                .direction(0.2f, -0.8f, -0.5f) // Sol desde arriba-izquierda
                .castShadows(true)
                .shadowOptions(new LightManager.ShadowOptions())
                .build(engine, sunLight);
        scene.addEntity(sunLight);

        // 💡 LUZ DE RELLENO - Para que no haya sombras muy oscuras
        int fillLight = EntityManager.get().create();
        new LightManager.Builder(LightManager.Type.DIRECTIONAL)
                .color(0.6f, 0.7f, 0.9f)      // Azul cielo (luz ambiental)
                .intensity(30000.0f)           // Más suave
                .direction(-0.5f, -0.3f, 0.5f) // Desde el lado opuesto
                .castShadows(false)
                .build(engine, fillLight);
        scene.addEntity(fillLight);

        Log.d(TAG, "✓ Iluminación solar configurada (sol + relleno)");
    }

    /**
     * ✨ Carga las bolitas mágicas GLB que flotan con el viento
     */
    private void setupMagicOrbs() {
        Log.d(TAG, "✨ Cargando " + NUM_ORBS + " bolitas mágicas GLB...");

        try {
            // Leer el archivo GLB una vez
            InputStream inputStream = context.getAssets().open(MAGIC_ORB_FILE);
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            inputStream.close();

            for (int i = 0; i < NUM_ORBS; i++) {
                // Posición distribuida por TODA la escena visible
                // Más dispersas para cubrir todo el fondo nevado
                orbBaseX[i] = (random.nextFloat() * 6f) - 3f;    // -3 a 3 (ancho visible)
                orbBaseY[i] = random.nextFloat() * 2f + 1.0f;    // 1.0 a 3.0 (flotando a altura humana)
                orbBaseZ[i] = random.nextFloat() * 8f + 2f;      // 2 a 10 (sobre el terreno visible)

                // Escala variada: algunas pequeñitas, otras más grandes
                orbScale[i] = ORB_SCALE_MIN + random.nextFloat() * (ORB_SCALE_MAX - ORB_SCALE_MIN);

                // Fase aleatoria para movimiento desfasado
                orbPhase[i] = random.nextFloat() * (float) Math.PI * 2f;

                // Velocidades y amplitudes aleatorias para variedad natural
                orbSpeedX[i] = 0.4f + random.nextFloat() * 0.6f;  // 0.4 - 1.0
                orbSpeedY[i] = 0.3f + random.nextFloat() * 0.4f;  // 0.3 - 0.7
                orbAmplitude[i] = 0.4f + random.nextFloat() * 0.6f;  // 0.4 - 1.0

                // Cargar GLB para esta bolita
                ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
                buffer.put(bytes);
                buffer.flip();

                orbAssets[i] = assetLoader.createAsset(buffer);
                if (orbAssets[i] != null) {
                    resourceLoader.loadResources(orbAssets[i]);
                    scene.addEntities(orbAssets[i].getEntities());

                    // Posicionar la bolita
                    updateOrbTransform(i, orbBaseX[i], orbBaseY[i], orbBaseZ[i]);
                    Log.d(TAG, "✨ Orb " + i + " pos(" +
                          String.format("%.1f", orbBaseX[i]) + "," +
                          String.format("%.1f", orbBaseY[i]) + "," +
                          String.format("%.1f", orbBaseZ[i]) + ")");
                }
            }

            Log.d(TAG, "✨ " + NUM_ORBS + " bolitas mágicas cargadas! Escala=" + ORB_SCALE_MIN + "-" + ORB_SCALE_MAX);

        } catch (Exception e) {
            Log.e(TAG, "❌ Error cargando bolitas mágicas: " + e.getMessage());
        }
    }

    /**
     * ✨ Actualiza la transformación de una bolita específica
     */
    private void updateOrbTransform(int index, float x, float y, float z) {
        if (orbAssets[index] == null) return;

        int rootEntity = orbAssets[index].getRoot();
        int instance = transformManager.getInstance(rootEntity);
        if (instance == 0) return;

        // Matriz de transformación: escala individual + posición
        float s = orbScale[index];
        float[] matrix = new float[] {
            s, 0, 0, 0,      // columna 0
            0, s, 0, 0,      // columna 1
            0, 0, s, 0,      // columna 2
            x, y, z, 1       // columna 3 (posición)
        };

        transformManager.setTransform(instance, matrix);
    }

    /**
     * ✨ Actualiza la posición de las bolitas con movimiento de viento mágico
     */
    private void updateMagicOrbs(float time) {
        for (int i = 0; i < NUM_ORBS; i++) {
            if (orbAssets[i] == null) continue;

            // Movimiento ondulante simulando viento mágico
            float phase = orbPhase[i];
            float speedX = orbSpeedX[i];
            float speedY = orbSpeedY[i];
            float amp = orbAmplitude[i];

            // Movimiento sinusoidal en X (viento horizontal suave)
            float windX = (float) Math.sin(time * speedX + phase) * amp;
            // Movimiento sinusoidal en Y (flotando arriba/abajo)
            float windY = (float) Math.sin(time * speedY + phase * 1.5f) * amp * 0.5f;
            // Movimiento lento en Z (deriva hacia adelante/atrás)
            float windZ = (float) Math.sin(time * 0.15f + phase * 2f) * amp * 0.4f;

            // Posición final = base + ondulación
            float finalX = orbBaseX[i] + windX;
            float finalY = orbBaseY[i] + windY;
            float finalZ = orbBaseZ[i] + windZ;

            // Actualizar transformación de la bolita GLB
            updateOrbTransform(i, finalX, finalY, finalZ);
        }
    }

    private void setupGltfLoader() {
        materialProvider = new UbershaderProvider(engine);
        assetLoader = new AssetLoader(engine, materialProvider, EntityManager.get());
        resourceLoader = new ResourceLoader(engine);
        Log.d(TAG, "✓ GLTF loader configurado");
    }

    // ═══════════════════════════════════════════════════════════════════
    // 🏔️ TERRAIN 3D - Terreno nevado con camino
    // ═══════════════════════════════════════════════════════════════════
    private float terrainX = 0.0f;
    private float terrainY = 0.0f;   // A nivel del suelo
    private float terrainZ = 5.0f;   // Centrado en la escena
    private float terrainScale = 4.0f;  // Un poco más grande

    /**
     * Carga el terreno 3D nevado
     */
    private void loadTerrain() {
        Log.d(TAG, "🏔️ Cargando terreno 3D: " + TERRAIN_FILE);

        try {
            InputStream inputStream = context.getAssets().open(TERRAIN_FILE);
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            inputStream.close();

            ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
            buffer.put(bytes);
            buffer.flip();

            terrainAsset = assetLoader.createAsset(buffer);
            if (terrainAsset == null) {
                Log.e(TAG, "❌ Error cargando terreno");
                return;
            }

            resourceLoader.loadResources(terrainAsset);
            scene.addEntities(terrainAsset.getEntities());

            // Posicionar el terreno como suelo
            positionTerrain();

            Log.d(TAG, "✅ Terreno cargado - Entidades: " + terrainAsset.getEntities().length);

        } catch (IOException e) {
            Log.e(TAG, "❌ Error cargando terreno: " + e.getMessage());
        }
    }

    /**
     * Posiciona el terreno 3D en la escena
     */
    private void positionTerrain() {
        if (terrainAsset == null || transformManager == null) return;

        int rootEntity = terrainAsset.getRoot();
        int transformInstance = transformManager.getInstance(rootEntity);

        if (transformInstance == 0) return;

        // Matriz de transformación: escala + posición
        float s = terrainScale;
        float[] transform = new float[] {
            s,        0f, 0f, 0f,   // columna 0
            0f,       s,  0f, 0f,   // columna 1
            0f,       0f, s,  0f,   // columna 2
            terrainX, terrainY, terrainZ, 1f  // columna 3 (posición)
        };

        transformManager.setTransform(transformInstance, transform);
        Log.d(TAG, "🏔️ Terreno posicionado: (" + terrainX + ", " + terrainY + ", " + terrainZ + ") escala=" + terrainScale);
    }

    /**
     * Setters para ajustar el terreno manualmente
     */
    public void setTerrainPosition(float x, float y, float z) {
        this.terrainX = x;
        this.terrainY = y;
        this.terrainZ = z;
        positionTerrain();
    }

    public void setTerrainScale(float scale) {
        this.terrainScale = scale;
        positionTerrain();
    }

    public void setTerrainTransform(float x, float y, float z, float scale) {
        this.terrainX = x;
        this.terrainY = y;
        this.terrainZ = z;
        this.terrainScale = scale;
        positionTerrain();
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

        // Usa las variables configurables
        float[] transform = new float[] {
            treeScale, 0f,        0f,        0f,
            0f,        treeScale, 0f,        0f,
            0f,        0f,        treeScale, 0f,
            treeX,     treeY,     treeZ,     1f
        };

        transformManager.setTransform(transformInstance, transform);
        Log.d(TAG, "🎄 Árbol posicionado: (" + treeX + ", " + treeY + ", " + treeZ + ") escala=" + treeScale);
    }

    // ═══════════════════════════════════════════════════════════════════
    // 🎄 SETTERS PARA ÁRBOL DE NAVIDAD - Ajuste manual de posición
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Establece la posición del árbol de navidad
     * @param x Posición horizontal (-5 izquierda, 5 derecha)
     * @param y Posición vertical (negativo = abajo)
     * @param z Profundidad (0 = frente, 10 = fondo/lejos)
     */
    public void setTreePosition(float x, float y, float z) {
        this.treeX = x;
        this.treeY = y;
        this.treeZ = z;
        positionTree();
        Log.d(TAG, "🎄 Nueva posición árbol: (" + x + ", " + y + ", " + z + ")");
    }

    /**
     * Establece la escala del árbol
     * @param scale Tamaño (0.5 = pequeño, 1.0 = normal, 2.0 = grande)
     */
    public void setTreeScale(float scale) {
        this.treeScale = scale;
        positionTree();
        Log.d(TAG, "🎄 Nueva escala árbol: " + scale);
    }

    /**
     * Establece posición y escala del árbol en una sola llamada
     */
    public void setTreeTransform(float x, float y, float z, float scale) {
        this.treeX = x;
        this.treeY = y;
        this.treeZ = z;
        this.treeScale = scale;
        positionTree();
        Log.d(TAG, "🎄 Árbol transform: pos(" + x + ", " + y + ", " + z + ") escala=" + scale);
    }

    // Getters para leer valores actuales
    public float getTreeX() { return treeX; }
    public float getTreeY() { return treeY; }
    public float getTreeZ() { return treeZ; }
    public float getTreeScale() { return treeScale; }

    // ═══════════════════════════════════════════════════════════════════
    // 🎅 SISTEMA PRE-CARGA: Carga todas las animaciones al inicio
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Pre-carga TODAS las animaciones de Santa al inicio
     * Solo la primera se muestra, las otras quedan listas para cambio instantáneo
     */
    private void preloadAllSantaAnimations() {
        Log.d(TAG, "🎅 Pre-cargando todas las animaciones de Santa...");

        for (int i = 0; i < ANIMATION_FILES.length; i++) {
            try {
                String filename = ANIMATION_FILES[i];
                InputStream inputStream = context.getAssets().open(filename);
                byte[] bytes = new byte[inputStream.available()];
                inputStream.read(bytes);
                inputStream.close();

                ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
                buffer.put(bytes);
                buffer.flip();

                FilamentAsset asset = assetLoader.createAsset(buffer);
                if (asset == null) {
                    Log.e(TAG, "❌ Error cargando: " + filename);
                    continue;
                }

                resourceLoader.loadResources(asset);
                santaAssets[i] = asset;
                santaAnimators[i] = asset.getInstance().getAnimator();

                // Solo agregar la primera animación a la escena
                if (i == 0) {
                    scene.addEntities(asset.getEntities());
                    currentAnimationIndex = 0;
                }

                Log.d(TAG, "✅ " + ANIMATION_NAMES[i] + " pre-cargado");

            } catch (IOException e) {
                Log.e(TAG, "❌ Error pre-cargando " + ANIMATION_FILES[i] + ": " + e.getMessage());
            }
        }

        updateAllSantaTransforms();
        Log.d(TAG, "🎅 Todas las animaciones pre-cargadas!");
    }

    /**
     * Cambia a la siguiente animación - INSTANTÁNEO (sin carga)
     */
    public void nextAnimation() {
        int nextIndex = (currentAnimationIndex + 1) % ANIMATION_FILES.length;
        switchToAnimation(nextIndex);
    }

    /**
     * Cambia a una animación específica - INSTANTÁNEO
     */
    private void switchToAnimation(int index) {
        if (index < 0 || index >= santaAssets.length) return;
        if (santaAssets[index] == null) return;
        if (index == currentAnimationIndex) return;

        // Quitar animación actual de la escena
        if (santaAssets[currentAnimationIndex] != null) {
            scene.removeEntities(santaAssets[currentAnimationIndex].getEntities());
        }

        // Agregar nueva animación a la escena
        scene.addEntities(santaAssets[index].getEntities());
        currentAnimationIndex = index;

        // Aplicar posición actual a la nueva animación
        updateCurrentSantaTransform();

        Log.d(TAG, "🔄 Cambio instantáneo a: " + ANIMATION_NAMES[index]);
        animationCycleTime = 0f;
    }

    /**
     * Aplica la transformación a TODAS las animaciones pre-cargadas
     */
    private void updateAllSantaTransforms() {
        for (int i = 0; i < santaAssets.length; i++) {
            if (santaAssets[i] != null) {
                updateSantaTransformForAsset(santaAssets[i]);
            }
        }
    }

    /**
     * Aplica la transformación solo a la animación actual
     */
    private void updateCurrentSantaTransform() {
        if (santaAssets[currentAnimationIndex] != null) {
            updateSantaTransformForAsset(santaAssets[currentAnimationIndex]);
        }
    }

    /**
     * Aplica transformación a un asset específico
     */
    private void updateSantaTransformForAsset(FilamentAsset asset) {
        if (asset == null || transformManager == null) return;

        int root = asset.getRoot();
        int transformInstance = transformManager.getInstance(root);
        if (transformInstance == 0) return;

        float scale = SANTA_SCALE;  // Escala proporcional para primera persona
        float cos = (float) Math.cos(santaRotation);
        float sin = (float) Math.sin(santaRotation);

        float[] transform = new float[] {
            scale * cos,  0f,           scale * sin,  0f,
            0f,           scale,        0f,           0f,
            -scale * sin, 0f,           scale * cos,  0f,
            santaX,       santaY,       santaZ,       1f
        };

        transformManager.setTransform(transformInstance, transform);
    }

    // ═══════════════════════════════════════════════════════════════════
    // 🤖 IA DE EXPLORACIÓN: Santa camina dinámicamente por el sendero
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Elige el próximo destino - Exploración por el terreno 3D
     */
    private void chooseNextDestination() {
        int roll = random.nextInt(100);
        int[] selectedZone;
        String zoneName;

        // Probabilidades para exploración natural del terreno
        if (roll < 40) {
            // Seguir el circuito natural (40%) - FAVORECIDO
            targetWaypoint = (targetWaypoint + 1) % PATH_WAYPOINTS.length;
            Log.d(TAG, "🚶 Circuito → wp " + targetWaypoint +
                  " pos(" + PATH_WAYPOINTS[targetWaypoint][0] + "," + PATH_WAYPOINTS[targetWaypoint][2] + ")");
            return;
        } else if (roll < 60) {
            // Zona frontal (20%) - más cerca de la cámara
            selectedZone = FRONT_WAYPOINTS;
            zoneName = "👀 FRENTE";
        } else if (roll < 80) {
            // Centro del terreno (20%)
            selectedZone = CENTER_WAYPOINTS;
            zoneName = "🎯 CENTRO";
        } else if (roll < 95) {
            // Fondo del terreno (15%)
            selectedZone = BACK_WAYPOINTS;
            zoneName = "🌲 FONDO";
        } else {
            // Punto completamente aleatorio (5%)
            selectedZone = PATH_WAYPOINTS_ALL;
            zoneName = "🎲 ALEATORIO";
        }

        // Elegir waypoint dentro de la zona
        int oldTarget = targetWaypoint;
        int attempts = 0;
        do {
            targetWaypoint = selectedZone[random.nextInt(selectedZone.length)];
            attempts++;
        } while (targetWaypoint == oldTarget && selectedZone.length > 1 && attempts < 5);

        float[] pos = PATH_WAYPOINTS[targetWaypoint];
        Log.d(TAG, "🤖 " + zoneName + " → wp " + targetWaypoint +
              " pos(" + pos[0] + "," + pos[2] + ")");
    }

    /**
     * Calcula la distancia al destino actual
     */
    private float distanceToTarget() {
        float[] target = PATH_WAYPOINTS[targetWaypoint];
        float dx = target[0] - santaX;
        float dz = target[2] - santaZ;
        return (float) Math.sqrt(dx * dx + dz * dz);
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

        // 🎥 Actualizar aspect ratio del controlador de cámara
        if (cameraController != null) {
            cameraController.setAspectRatio((float) width / height);
        }

        // Recalcular posición del terreno para nuevas dimensiones
        positionTerrain();

        Log.d(TAG, "📐 Viewport: " + width + "x" + height);
    }

    /**
     * Renderiza un frame
     */
    public void render(long frameTimeNanos) {
        if (!initialized || swapChain == null) return;

        float deltaTime = 0.016f; // ~60fps
        float time = (frameTimeNanos - startTime) / 1_000_000_000f;

        // 🎥 ACTUALIZAR CÁMARA FPS - Recorrido por el terreno
        if (cameraController != null) {
            cameraController.update(deltaTime);
        }

        // ✨ Actualizar bolitas mágicas flotantes
        updateMagicOrbs(time);

        // Actualizar animación actual de Santa (sistema pre-cargado)
        Animator currentAnimator = santaAnimators[currentAnimationIndex];
        if (currentAnimator != null) {
            float duration = currentAnimator.getAnimationDuration(0);
            float animTime = time % duration;
            currentAnimator.applyAnimation(0, animTime);
            currentAnimator.updateBoneMatrices();
        }

        // 🤖 IA DE MOVIMIENTO: Solo camina en animación 0
        if (santaAssets[currentAnimationIndex] != null) {
            if (currentAnimationIndex == 0) {
                // 🚶 Caminando hacia destino elegido por IA
                float[] target = PATH_WAYPOINTS[targetWaypoint];
                float targetX = target[0];
                float targetY = target[1];
                float targetZ = target[2];

                float dx = targetX - santaX;
                float dy = targetY - santaY;
                float dz = targetZ - santaZ;
                float distance = (float) Math.sqrt(dx * dx + dz * dz);

                // ¿Llegó al destino?
                if (distance < 0.3f) {  // Aumentado threshold
                    // 🤖 IA elige nuevo destino aleatorio
                    chooseNextDestination();
                } else {
                    // Mover hacia el destino - velocidad aumentada
                    float speed = WALK_SPEED * 1.5f;  // Más rápido
                    float moveX = (dx / distance) * speed * deltaTime;
                    float moveY = (dy / distance) * speed * deltaTime * 0.3f;
                    float moveZ = (dz / distance) * speed * deltaTime;

                    santaX += moveX;
                    santaY += moveY;
                    santaZ += moveZ;

                    // 🔄 Mirar hacia donde camina (rotación completa)
                    santaRotation = (float) Math.atan2(dx, dz) + (float) Math.PI;
                }
            }
            // Aplicar transformación (mantiene posición en wave/look)
            updateCurrentSantaTransform();
        }

        // Ciclo automático de animaciones
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

        // Limpiar TODAS las animaciones de Santa pre-cargadas
        for (int i = 0; i < santaAssets.length; i++) {
            if (santaAssets[i] != null) {
                scene.removeEntities(santaAssets[i].getEntities());
                assetLoader.destroyAsset(santaAssets[i]);
                santaAssets[i] = null;
                santaAnimators[i] = null;
            }
        }

        // ✨ Limpiar bolitas mágicas GLB
        for (int i = 0; i < NUM_ORBS; i++) {
            if (orbAssets[i] != null) {
                scene.removeEntities(orbAssets[i].getEntities());
                assetLoader.destroyAsset(orbAssets[i]);
                orbAssets[i] = null;
            }
        }

        // Limpiar terreno
        if (terrainAsset != null) {
            scene.removeEntities(terrainAsset.getEntities());
            assetLoader.destroyAsset(terrainAsset);
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
    public int getCurrentAnimationIndex() { return currentAnimationIndex; }
    public String getCurrentAnimationName() {
        return ANIMATION_NAMES[currentAnimationIndex];
    }
}
