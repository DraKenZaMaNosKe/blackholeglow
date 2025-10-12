// SceneRenderer.java - VERSIÓN MEJORADA CON LOGGING
package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * SceneRenderer con sistema de logging detallado para desarrollo
 */
public class SceneRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "depurar";
    public static int screenWidth = 1, screenHeight = 1;

    private final Context context;
    private String selectedItem;
    private boolean paused = false;
    private final List<SceneObject> sceneObjects = new ArrayList<>();
    private long lastTime = System.nanoTime();
    private CameraController sharedCamera;
    private TextureManager textureManager;

    // Referencias para el sistema de HP y respawn
    private Planeta sol;
    private ForceField forceField;
    private HPBar hpBarSun;
    private HPBar hpBarForceField;
    private MeteorShower meteorShower;
    private boolean solWasDead = false;  // Para detectar cuando respawnea

    // Sistema de visualización musical
    private MusicVisualizer musicVisualizer;
    private boolean musicReactiveEnabled = true;  // Activado por defecto
    private MusicIndicator musicIndicator;  // Indicador visual de música
    private EstrelaBailarina estrellaBailarina;  // Referencia para actualizar con música
    private HPBar musicStatusBar;  // Barra de prueba para indicador de música

    // Métricas de rendimiento
    private int frameCount = 0;
    private float fpsTimer = 0f;
    private long totalFrames = 0;
    private long renderStartTime;
    private float currentFPS = 0;
    private float averageFPS = 0;
    private float minFPS = Float.MAX_VALUE;
    private float maxFPS = 0;

    // Información del sistema
    private String deviceInfo = "";
    private long totalMemory = 0;
    private long availableMemory = 0;

    public SceneRenderer(Context ctx, String initialItem) {
        this.context = ctx;
        this.selectedItem = initialItem;

        // Obtener información del dispositivo
        deviceInfo = Build.MANUFACTURER + " " + Build.MODEL;
        renderStartTime = System.currentTimeMillis();

        Log.d(TAG, "╔══════════════════════════════════════════════╗");
        Log.d(TAG, "║          SCENE RENDERER INITIALIZED         ║");
        Log.d(TAG, "╠══════════════════════════════════════════════╣");
        Log.d(TAG, "║ Device: " + String.format("%-37s", deviceInfo) + "║");
        Log.d(TAG, "║ Android: " + String.format("%-36s", "API " + Build.VERSION.SDK_INT) + "║");
        Log.d(TAG, "║ Initial Scene: " + String.format("%-30s", initialItem) + "║");
        Log.d(TAG, "╚══════════════════════════════════════════════╝");
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig cfg) {
        Log.d(TAG, "════════ onSurfaceCreated START ════════");

        // Configuración OpenGL
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glClearColor(0.02f, 0.02f, 0.05f, 1f);

        // Obtener información de OpenGL
        String vendor = GLES20.glGetString(GLES20.GL_VENDOR);
        String renderer = GLES20.glGetString(GLES20.GL_RENDERER);
        String version = GLES20.glGetString(GLES20.GL_VERSION);

        Log.d(TAG, "╔══════════════════════════════════════════════╗");
        Log.d(TAG, "║           OPENGL INFORMATION                ║");
        Log.d(TAG, "╠══════════════════════════════════════════════╣");
        Log.d(TAG, "║ Vendor: " + String.format("%-37s", vendor) + "║");
        Log.d(TAG, "║ Renderer: " + String.format("%-35s", renderer.substring(0, Math.min(35, renderer.length()))) + "║");
        Log.d(TAG, "║ Version: " + String.format("%-36s", version.substring(0, Math.min(36, version.length()))) + "║");
        Log.d(TAG, "╚══════════════════════════════════════════════╝");

        // Crear controladores
        sharedCamera = new CameraController();
        textureManager = new TextureManager(context);

        // CONFIGURAR CÁMARA EN POSICIÓN ÓPTIMA
        sharedCamera.setMode(CameraController.CameraMode.PERSPECTIVE_3_4);
        Log.d(TAG, "✓ Camera mode set to PERSPECTIVE_3_4");

        // INICIALIZAR VISUALIZADOR MUSICAL
        musicVisualizer = new MusicVisualizer();
        if (musicVisualizer.initialize()) {
            musicReactiveEnabled = true;
            Log.d(TAG, "╔════════════════════════════════════════╗");
            Log.d(TAG, "║   🎵 MUSIC VISUALIZER ACTIVATED 🎵    ║");
            Log.d(TAG, "║   Wallpaper reacts to your music!     ║");
            Log.d(TAG, "╚════════════════════════════════════════╝");
        } else {
            Log.w(TAG, "⚠️ Music visualizer could not be initialized (missing permissions?)");
            Log.w(TAG, "⚠️ Will retry initialization automatically...");
            musicReactiveEnabled = false;
        }

        // Preparar escena
        prepareScene();

        Log.d(TAG, "════════ onSurfaceCreated END ════════");
        Log.d(TAG, "✓ Surface created with " + sceneObjects.size() + " objects");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        GLES20.glViewport(0, 0, w, h);
        screenWidth = w;
        screenHeight = h;

        sharedCamera.updateProjection(w, h);

        boolean isPortrait = h > w;
        float aspectRatio = (float) w / h;

        Log.d(TAG, "╔══════════════════════════════════════════════╗");
        Log.d(TAG, "║          VIEWPORT CHANGED                   ║");
        Log.d(TAG, "╠══════════════════════════════════════════════╣");
        Log.d(TAG, "║ Resolution: " + String.format("%-33s", w + " x " + h) + "║");
        Log.d(TAG, "║ Orientation: " + String.format("%-32s", isPortrait ? "PORTRAIT" : "LANDSCAPE") + "║");
        Log.d(TAG, "║ Aspect Ratio: " + String.format("%-31.2f", aspectRatio) + "║");
        Log.d(TAG, "╚══════════════════════════════════════════════╝");
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (paused) return;

        // Delta time
        long now = System.nanoTime();
        float dt = Math.min((now - lastTime) / 1e9f, 0.1f);
        lastTime = now;

        // Actualizar métricas
        frameCount++;
        totalFrames++;
        fpsTimer += dt;

        // Calcular FPS cada 10 segundos para MÍNIMO overhead
        if (fpsTimer >= 10.0f) {
            currentFPS = frameCount / fpsTimer;
            minFPS = Math.min(minFPS, currentFPS);
            maxFPS = Math.max(maxFPS, currentFPS);

            // Calcular FPS promedio
            long elapsedSeconds = (System.currentTimeMillis() - renderStartTime) / 1000;
            if (elapsedSeconds > 0) {
                averageFPS = totalFrames / (float) elapsedSeconds;
            }

            // Log muy simplificado (solo si FPS bajo)
            if (currentFPS < 50) {
                Log.d(TAG, String.format("[Renderer] FPS: %.1f (avg:%.1f)", currentFPS, averageFPS));
            }

            frameCount = 0;
            fpsTimer = 0f;
        }

        // Limpiar buffers
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // La cámara NO se actualiza (está fija)
        //sharedCamera.update(dt); // COMENTADO - cámara estática

        // Coordinar respawn de Sol y Campo de Fuerza
        coordinarRespawn();

        // ╔═══════════════════════════════════════════════════════════════╗
        // ║  REINTENTO AUTOMÁTICO DE INICIALIZACIÓN SI NO TIENE PERMISOS ║
        // ╚═══════════════════════════════════════════════════════════════╝
        // Si el visualizer no está habilitado, reintentar cada 60 frames (1 seg aprox)
        if (!musicReactiveEnabled && musicVisualizer != null && frameCount % 60 == 0) {
            if (musicVisualizer.initialize()) {
                musicReactiveEnabled = true;
                Log.d(TAG, "╔════════════════════════════════════════╗");
                Log.d(TAG, "║  ✓✓✓ AUDIO PERMISSIONS GRANTED! ✓✓✓  ║");
                Log.d(TAG, "║  Music visualizer NOW ACTIVE!         ║");
                Log.d(TAG, "╚════════════════════════════════════════╝");
            }
        }

        // ╔═══════════════════════════════════════════════════════════════╗
        // ║  RECONEXIÓN AUTOMÁTICA SI PERDIÓ AUDIO O SOLO HAY SILENCIO  ║
        // ╚═══════════════════════════════════════════════════════════════╝
        // Verificar cada 2 segundos (120 frames) si está recibiendo audio REAL
        if (musicReactiveEnabled && musicVisualizer != null && frameCount % 120 == 0) {
            if (!musicVisualizer.isReceivingAudio()) {
                // Log reducido - solo cada 10 segundos
                if (frameCount % 600 == 0) {
                    Log.w(TAG, "⚠️ No audio - reconnecting...");
                }

                if (musicVisualizer.reconnect()) {
                    // Log solo en primera reconexión exitosa
                } else {
                    musicReactiveEnabled = false;  // Forzar re-inicialización completa
                }
            }
        }

        // Actualizar barra de estado musical (verde si recibiendo audio, rojo si no)
        if (musicStatusBar != null) {
            // Verde solo si está habilitado Y recibiendo datos de audio REAL
            boolean isReceivingAudio = musicReactiveEnabled
                                    && musicVisualizer != null
                                    && musicVisualizer.isEnabled()
                                    && musicVisualizer.isReceivingAudio();

            if (isReceivingAudio) {
                // Verde = tiene permisos y está recibiendo audio REAL
                musicStatusBar.setHealth(100);
            } else {
                // Rojo = no tiene permisos, no está recibiendo audio, o solo silencio
                musicStatusBar.setHealth(0);
            }

            // Log muy reducido - solo cada 20 segundos
            if (frameCount % 1200 == 0) {
                Log.d(TAG, String.format("🎵 Audio: %s", isReceivingAudio ? "✓" : "✗"));
            }
        }

        // Distribuir datos musicales a objetos reactivos
        if (musicReactiveEnabled && musicVisualizer != null && musicVisualizer.isEnabled()) {
            distribuirDatosMusicales();
        }

        // Dibujar objetos
        for (SceneObject obj : sceneObjects) {
            obj.update(dt);
            obj.draw();
        }
    }

    private void prepareScene() {
        Log.d(TAG, "════════ Preparing Scene: " + selectedItem + " ════════");

        if (!textureManager.initialize()) {
            Log.e(TAG, "✗ ERROR: TextureManager could not initialize");
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

        Log.d(TAG, "✓ Scene prepared with " + sceneObjects.size() + " objects");
    }

    private void setupUniverseScene() {
        Log.d(TAG, "Setting up UNIVERSE scene...");

        // FONDO CON TEXTURA + ESTRELLAS PROCEDURALES (TODO EN UNO)
        try {
            StarryBackground starryBg = new StarryBackground(
                    context,
                    textureManager,
                    R.drawable.universo03
            );
            sceneObjects.add(starryBg);
            Log.d(TAG, "[SceneRenderer] ✓ Fondo con textura + estrellas agregado");
        } catch (Exception e) {
            Log.e(TAG, "[SceneRenderer] ✗ Error creando fondo: " + e.getMessage());
        }


        // SOL CENTRAL CON NUEVO SHADER DE LAVA (CENTRADO EN 0,0,0)
        try {
            sol = new Planeta(
                    context, textureManager,
                    "shaders/planeta_vertex.glsl",
                    "shaders/sol_lava_fragment.glsl",  // NUEVO SHADER DE LAVA
                    R.drawable.textura_sol,
                    0.0f,              // orbitRadiusX = 0 (centro)
                    0.0f,              // orbitRadiusZ = 0 (centro)
                    0.0f,              // orbitSpeed = 0 (sin órbita)
                    0.0f,              // scaleAmplitude = sin variación
                    0.4f,              // instanceScale = tamaño del sol
                    3.0f,              // spinSpeed = rotación muy lenta para lava
                    false, null, 1.0f,
                    null, 1.0f
            );
            if (sol instanceof CameraAware) {
                ((CameraAware) sol).setCameraController(sharedCamera);
            }
            sol.setMaxHealth(30);  // Sol tiene 30 HP
            sceneObjects.add(sol);
            Log.d(TAG, "  ✓ Sun added with lava shader (opaque) - HP: 30");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creating sun: " + e.getMessage());
        }

        // ✨ ESTRELLA BAILARINA - PARTÍCULA MÁGICA CON ESTELA ✨
        // Casi invisible, solo se ve la estela arcoíris
        try {
            estrellaBailarina = new EstrelaBailarina(
                    context, textureManager,
                    1.5f, 0.5f, 0.0f,   // Posición inicial
                    0.02f,              // Escala: MINÚSCULA (casi invisible, solo estela)
                    45.0f               // Rotación: rápida
            );
            estrellaBailarina.setCameraController(sharedCamera);
            sceneObjects.add(estrellaBailarina);
            Log.d(TAG, "  ✨ ESTRELLA BAILARINA agregada (minúscula - solo estela visible) ✨");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando estrella bailarina: " + e.getMessage());
        }

        // PLANETA ORBITANTE (REDUCIDO Y ALEJADO)
        try {
            Planeta planeta1 = new Planeta(
                    context, textureManager,
                    "shaders/planeta_vertex.glsl",
                    "shaders/planeta_iluminado_fragment.glsl",  // SHADER CON ILUMINACIÓN
                    R.drawable.textura_roninplaneta,
                    3.2f, 2.8f, 0.3f,  // Órbita más amplia (alejada)
                    0.1f,              // Poca variación
                    0.18f,             // REDUCIDO de 0.25 a 0.18 (28% más pequeño)
                    30.0f,             // Rotación media
                    false, null, 1.0f,
                    null,
                    1.0f
            );
            if (planeta1 instanceof CameraAware) {
                ((CameraAware) planeta1).setCameraController(sharedCamera);
            }
            sceneObjects.add(planeta1);
            Log.d(TAG, "  ✓ Orbiting planet added with illumination");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creating planet: " + e.getMessage());
        }

        // BARRA DE PODER DE BATERÍA - UI ELEMENT
        BatteryPowerBar powerBar = null;
        try {
            powerBar = new BatteryPowerBar(context);
            sceneObjects.add(powerBar);
            Log.d(TAG, "  ✓ Battery power bar added");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creating power bar: " + e.getMessage());
        }

        // CAMPO DE FUERZA INTERACTIVO DEL SOL - AZUL ELÉCTRICO (CENTRADO CON EL SOL)
        try {
            forceField = new ForceField(
                    context, textureManager,
                    0.0f, 0.0f, 0.0f,   // CENTRADO con el sol en (0, 0, 0)
                    0.55f,              // Radio más pequeño y contenido
                    R.drawable.fondo_transparente,  // Textura transparente para efectos puros
                    new float[]{0.2f, 0.6f, 1.0f},  // Color azul eléctrico brillante
                    0.45f,              // Alpha más visible
                    0.06f,              // Pulsación MUY sutil (6% de variación)
                    0.4f                // Pulsación LENTA (menos de la mitad de velocidad)
            );
            forceField.setCameraController(sharedCamera);
            sceneObjects.add(forceField);
            Log.d(TAG, "[SceneRenderer] ✓ Campo de fuerza interactivo agregado");
        } catch (Exception e) {
            Log.e(TAG, "[SceneRenderer] ✗ Error creando campo de fuerza: " + e.getMessage());
        }

        // BARRAS HP para Sol y Campo de Fuerza
        try {
            // Barra HP del Sol (amarilla cuando llena, roja cuando vacía)
            hpBarSun = new HPBar(
                    context,
                    "SOL",
                    0.05f, 0.92f,  // Posición: arriba izquierda
                    0.25f, 0.03f,  // Tamaño: ancho y alto
                    30,  // Max HP = 30
                    new float[]{1.0f, 0.8f, 0.0f, 1.0f},  // Amarillo lleno
                    new float[]{1.0f, 0.0f, 0.0f, 1.0f}   // Rojo vacío
            );
            sceneObjects.add(hpBarSun);

            // Barra HP del Campo de Fuerza (azul cuando llena, roja cuando vacía)
            hpBarForceField = new HPBar(
                    context,
                    "ESCUDO",
                    0.05f, 0.87f,  // Posición: debajo de la barra del sol
                    0.25f, 0.03f,  // Tamaño
                    20,  // Max HP = 20
                    new float[]{0.2f, 0.6f, 1.0f, 1.0f},  // Azul lleno
                    new float[]{1.0f, 0.0f, 0.0f, 1.0f}   // Rojo vacío
            );
            sceneObjects.add(hpBarForceField);

            Log.d(TAG, "[SceneRenderer] ✓ Barras HP agregadas (Sol y Escudo)");
        } catch (Exception e) {
            Log.e(TAG, "[SceneRenderer] ✗ Error creando barras HP: " + e.getMessage());
        }

        // 🎵 INDICADOR DE ESTADO MUSICAL 🎵
        // OCULTO VISUALMENTE - Solo se usa internamente para monitoreo
        try {
            musicStatusBar = new HPBar(
                    context,
                    "♪ AUDIO",
                    0.05f, 0.82f,
                    0.25f, 0.035f,
                    100,
                    new float[]{0.1f, 0.9f, 0.3f, 1.0f},
                    new float[]{0.8f, 0.15f, 0.15f, 0.8f}
            );
            musicStatusBar.setHealth(0);
            // NO agregarlo a sceneObjects para que no se dibuje
            // sceneObjects.add(musicStatusBar);  // ← COMENTADO
            Log.d(TAG, "  🎵✓ Indicador de audio creado (oculto)");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ ERROR creando indicador de audio: " + e.getMessage());
        }

        // 🎵 INDICADOR VISUAL DE MÚSICA 🎵
        // Muestra 3 barras (BASS, MID, TREBLE) CENTRADAS, ARRIBA DEL SOL
        try {
            Log.d(TAG, "╔════════════════════════════════════════╗");
            Log.d(TAG, "║   CREANDO INDICADOR DE MÚSICA         ║");
            Log.d(TAG, "╚════════════════════════════════════════╝");

            musicIndicator = new MusicIndicator(
                    context,
                    -0.15f, 0.65f, // Posición: CENTRADO HORIZONTALMENTE, ARRIBA del sol
                    0.09f,         // Ancho de cada barra (más grande: 0.06 → 0.09)
                    0.25f          // Altura máxima de las barras (más grande: 0.08 → 0.25)
            );
            sceneObjects.add(musicIndicator);
            Log.d(TAG, "  🎵✓ INDICADOR DE MÚSICA agregado - CENTRADO, ARRIBA del sol");
        } catch (Exception e) {
            Log.e(TAG, "  ✗✗✗ ERROR CRÍTICO creando indicador de música: " + e.getMessage());
            e.printStackTrace();
        }

        // SISTEMA DE LLUVIA DE METEORITOS - AÑADIDO DESPUÉS DE LOS PLANETAS
        try {
            meteorShower = new MeteorShower(context, textureManager);
            meteorShower.setCameraController(sharedCamera);

            // Conectar con la barra de poder
            if (powerBar != null) {
                meteorShower.setPowerBar(powerBar);
            }

            // Conectar con el sistema de HP
            if (sol != null && forceField != null && hpBarSun != null && hpBarForceField != null) {
                meteorShower.setHPSystem(sol, forceField, hpBarSun, hpBarForceField);
                Log.d(TAG, "[SceneRenderer] ✓ Sistema HP conectado con MeteorShower");
            }

            // Registrar el sol, planeta Y campo de fuerza para colisiones
            for (SceneObject obj : sceneObjects) {
                if (obj instanceof Planeta || obj instanceof ForceField) {
                    meteorShower.registrarObjetoColisionable(obj);
                }
            }

            sceneObjects.add(meteorShower);
            Log.d(TAG, "[SceneRenderer] ✓ Sistema de meteoritos agregado (con campo de fuerza)");
        } catch (Exception e) {
            Log.e(TAG, "[SceneRenderer] ✗ Error creando sistema de meteoritos: " + e.getMessage());
        }

        Log.d(TAG, "✓ Universe scene setup complete");
    }

    private void setupBlackHoleScene() {
        Log.d(TAG, "Setting up BLACK HOLE scene...");

        // Centro negro
        try {
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
            Log.d(TAG, "  ✓ Black hole core added");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creating black hole: " + e.getMessage());
        }

        // Disco de acreción simple
        for (int i = 0; i < 3; i++) {
            try {
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
                Log.d(TAG, "  ✓ Accretion disk particle " + (i+1) + " added");
            } catch (Exception e) {
                Log.e(TAG, "  ✗ Error creating particle " + i + ": " + e.getMessage());
            }
        }

        Log.d(TAG, "✓ Black hole scene setup complete");
    }

    public void pause() {
        paused = true;
        if (musicVisualizer != null) {
            musicVisualizer.pause();
        }
        Log.d(TAG, "Renderer PAUSED");
    }

    public void resume() {
        paused = false;
        lastTime = System.nanoTime();
        if (musicVisualizer != null) {
            musicVisualizer.resume();
        }
        Log.d(TAG, "Renderer RESUMED");
    }

    public void release() {
        Log.d(TAG, "Releasing resources...");

        // Liberar visualizador musical
        if (musicVisualizer != null) {
            musicVisualizer.release();
            musicVisualizer = null;
        }

        for (SceneObject obj : sceneObjects) {
            if (obj instanceof UniverseBackground) {
                ((UniverseBackground) obj).release();
            }
            // La nueva clase 2D no necesita release especial
        }
        Log.d(TAG, "✓ Resources released");
    }

    /**
     * Distribuye datos musicales a todos los objetos reactivos
     */
    private void distribuirDatosMusicales() {
        float bass = musicVisualizer.getBassLevel();
        float mid = musicVisualizer.getMidLevel();
        float treble = musicVisualizer.getTrebleLevel();
        float volume = musicVisualizer.getVolumeLevel();
        float beatIntensity = musicVisualizer.getBeatIntensity();
        boolean isBeat = musicVisualizer.isBeat();

        // Log desactivado para performance - solo debug crítico
        // Si necesitas debug, descomenta la siguiente línea:
        // if (frameCount % 600 == 0) Log.d(TAG, String.format("🎵 B:%.2f M:%.2f T:%.2f", bass, mid, treble));

        // Actualizar indicador visual de música
        if (musicIndicator != null) {
            musicIndicator.updateMusicLevels(bass, mid, treble);
        } else {
            if (frameCount % 120 == 0) {
                Log.e(TAG, "[SceneRenderer] ✗ musicIndicator es NULL! No se puede actualizar");
            }
        }

        // Enviar datos a todos los objetos que implementen MusicReactive
        for (SceneObject obj : sceneObjects) {
            if (obj instanceof MusicReactive) {
                ((MusicReactive) obj).onMusicData(bass, mid, treble, volume, beatIntensity, isBeat);
            }
        }
    }

    /**
     * Coordina el respawn del Sol y Campo de Fuerza juntos
     */
    private void coordinarRespawn() {
        if (sol == null || forceField == null) return;

        boolean solIsDead = sol.isDead();

        // Detectar cuando el sol acaba de morir
        if (solIsDead && !solWasDead) {
            Log.d(TAG, "╔════════════════════════════════════════╗");
            Log.d(TAG, "║   ¡¡¡ SOL DESTRUIDO !!!               ║");
            Log.d(TAG, "║   Campo de Fuerza caído...            ║");
            Log.d(TAG, "║   Respawn en 3 segundos...            ║");
            Log.d(TAG, "╚════════════════════════════════════════╝");
        }

        // Detectar cuando el sol acaba de respawnear
        if (!solIsDead && solWasDead) {
            // RESPAWN COORDINADO: Resetear campo de fuerza y HP bars juntos
            if (forceField != null) {
                forceField.reset();
            }
            if (hpBarSun != null) {
                hpBarSun.reset();
            }
            if (hpBarForceField != null) {
                hpBarForceField.reset();
            }

            Log.d(TAG, "╔════════════════════════════════════════╗");
            Log.d(TAG, "║   ✨ RESPAWN COMPLETO ✨              ║");
            Log.d(TAG, "║   Sol: HP restaurado                  ║");
            Log.d(TAG, "║   Campo de Fuerza: ACTIVO             ║");
            Log.d(TAG, "╚════════════════════════════════════════╝");
        }

        solWasDead = solIsDead;
    }

    public void setSelectedItem(String item) {
        if (item != null && !item.equals(selectedItem)) {
            Log.d(TAG, "Scene change requested: " + selectedItem + " → " + item);
            this.selectedItem = item;
            prepareScene();
        }
    }
}