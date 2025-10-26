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
public class SceneRenderer implements GLSurfaceView.Renderer, Planeta.OnExplosionListener {
    private static final String TAG = "depurar";
    public static int screenWidth = 1, screenHeight = 1;

    private final Context context;
    private String selectedItem;
    private boolean paused = false;
    private final List<SceneObject> sceneObjects = new ArrayList<>();
    private long lastTime = System.nanoTime();
    private CameraController sharedCamera;
    private TextureManager textureManager;

    // Flag para recrear escena en GL thread
    private volatile boolean needsSceneRecreation = false;

    // Referencias para el sistema de HP y respawn
    private Planeta sol;
    private ForceField forceField;
    private HPBar hpBarSun;
    private HPBar hpBarForceField;
    private MeteorShower meteorShower;
    private PlayerWeapon playerWeapon;  // 🎮 NUEVO: Arma del jugador (separada de MeteorShower)
    private boolean solWasDead = false;  // Para detectar cuando respawnea

    // Sistema de visualización musical
    private MusicVisualizer musicVisualizer;
    private boolean musicReactiveEnabled = true;  // Activado por defecto
    private MusicIndicator musicIndicator;  // Indicador visual de música
    private List<EstrellaBailarina> estrellasBailarinas = new ArrayList<>();  // 3 estrella bailarina
    private HPBar musicStatusBar;  // Barra de prueba para indicador de música

    // ===== 👆 SISTEMA DE TOQUE INTERACTIVO 👆 =====
    private boolean isTouching = false;           // Usuario está tocando la pantalla?
    private float touchStartTime = 0f;            // Cuándo empezó a tocar
    private float chargeLevel = 0f;               // Nivel de carga (0.0 - 1.0)
    private static final float MAX_CHARGE_TIME = 1.5f;  // 1.5 segundos (antes de que Android muestre menú)
    private HPBar chargePowerBar;                 // Barra visual de carga
    private float touchX = 0f;                    // Posición X del toque (en coordenadas de pantalla)
    private float touchY = 0f;                    // Posición Y del toque

    // ===== 💥 SISTEMA DE IMPACTO EN PANTALLA 💥 =====
    private float impactFlashAlpha = 0f;          // Alpha del flash blanco (0-1)
    private float impactFlashTimer = 0f;          // Tiempo restante del flash
    private int flashShaderProgramId = 0;         // Shader para el flash blanco
    private int flashAPositionLoc = -1;
    private int flashAColorLoc = -1;

    // ===== 💥 SISTEMA DE PANTALLA ROTA (GRIETAS) 💥 =====
    private float crackAlpha = 0f;                // Alpha de las grietas (0-1)
    private float crackTimer = 0f;                // Tiempo desde el impacto
    private float crackX = 0.5f;                  // Posición X del impacto (0-1)
    private float crackY = 0.5f;                  // Posición Y del impacto (0-1)
    private int crackShaderProgramId = 0;         // Shader para las grietas
    private int crackAPositionLoc = -1;
    private int crackATexCoordLoc = -1;
    private int crackUTimeLoc = -1;
    private int crackUImpactPosLoc = -1;
    private int crackUAlphaLoc = -1;

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

    // 🎮 SISTEMA DE ESTADÍSTICAS DEL JUGADOR
    private PlayerStats playerStats;

    // 📊 CONTADOR DE SOLES DESTRUIDOS (UI)
    private SimpleTextRenderer sunsDestroyedCounter;

    // 🏆 SISTEMA DE LEADERBOARD Y BOTS
    private BotManager botManager;
    private LeaderboardManager leaderboardManager;
    private SimpleTextRenderer[] leaderboardTexts = new SimpleTextRenderer[3];  // Textos para Top 3
    private long lastLeaderboardUpdate = 0;
    private static final long LEADERBOARD_UPDATE_INTERVAL = 30000; // 30 segundos

    public SceneRenderer(Context ctx, String initialItem) {
        this.context = ctx;
        this.selectedItem = initialItem;

        // 🎮 Inicializar sistema de estadísticas
        this.playerStats = PlayerStats.getInstance(ctx);
        playerStats.printStats();  // Mostrar estadísticas al iniciar

        // 🔄 Escuchar sincronización con Firebase para actualizar contador
        playerStats.setSyncListener(new PlayerStats.SyncListener() {
            @Override
            public void onSyncCompleted(int sunsDestroyed) {
                // Actualizar contador en pantalla cuando se sincronice con Firebase
                if (sunsDestroyedCounter != null) {
                    sunsDestroyedCounter.setText("☀️" + sunsDestroyed);
                    Log.d(TAG, "✅ Contador actualizado después de sincronización: " + sunsDestroyed + " soles");
                }
            }
        });

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

        // CONFIGURAR CÁMARA EN PERSPECTIVA FIJA (3/4 isométrica)
        sharedCamera.setMode(CameraController.CameraMode.PERSPECTIVE_3_4);
        Log.d(TAG, "📷 Camera mode set to PERSPECTIVE_3_4 - Cámara fija activada");

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

        // ===== RECREAR ESCENA SI ES NECESARIO (GL THREAD SAFE) =====
        if (needsSceneRecreation) {
            Log.d(TAG, "════════ RECREANDO ESCENA EN GL THREAD ════════");
            prepareScene();
            needsSceneRecreation = false;
        }

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

            // ⚡ MEDIDOR DE RENDIMIENTO MEJORADO
            // Logs más visibles cuando hay problemas de rendimiento
            if (currentFPS < 30) {
                // FPS bajo - alerta CRÍTICA
                Log.w(TAG, "╔════════════════════════════════════════╗");
                Log.w(TAG, "║   ⚠️  RENDIMIENTO BAJO DETECTADO      ║");
                Log.w(TAG, "╠════════════════════════════════════════╣");
                Log.w(TAG, String.format("║ FPS Actual:   %.1f FPS                    ║", currentFPS));
                Log.w(TAG, String.format("║ FPS Promedio: %.1f FPS                    ║", averageFPS));
                Log.w(TAG, String.format("║ FPS Mínimo:   %.1f FPS                    ║", minFPS));
                Log.w(TAG, String.format("║ FPS Máximo:   %.1f FPS                    ║", maxFPS));
                Log.w(TAG, "║                                        ║");
                Log.w(TAG, "║ Sugerencia: Reducir objetos o efectos ║");
                Log.w(TAG, "╚════════════════════════════════════════╝");
            } else {
                // FPS bueno - log minimal cada 30 segundos
                if (elapsedSeconds % 30 == 0) {
                    Log.d(TAG, String.format("[Renderer] ✓ FPS: %.1f (promedio: %.1f)", currentFPS, averageFPS));
                }
            }

            frameCount = 0;
            fpsTimer = 0f;
        }

        // 🏆 Actualizar leaderboard periódicamente (cada 30 segundos)
        updateLeaderboardUI();

        // Limpiar buffers
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Actualizar cámara (para screen shake)
        sharedCamera.update(dt);

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

        // 👆 ACTUALIZAR SISTEMA DE CARGA DE PODER
        updateChargeSystem(dt);

        // 💥 ACTUALIZAR FLASH DE IMPACTO
        if (impactFlashTimer > 0) {
            impactFlashTimer -= dt;
            impactFlashAlpha *= 0.85f;  // Decay rápido
            if (impactFlashTimer <= 0) {
                impactFlashAlpha = 0f;
            }
        }

        // 💥 ACTUALIZAR GRIETAS DE PANTALLA ROTA
        if (crackTimer > 0) {
            crackTimer += dt;

            // Fase 1 (0-0.5s): Grietas aparecen y se expanden rápidamente
            if (crackTimer < 0.5f) {
                crackAlpha = crackTimer / 0.5f;  // 0 → 1
            }
            // Fase 2 (0.5-3.5s): Grietas visibles
            else if (crackTimer < 3.5f) {
                crackAlpha = 1.0f;  // Máximo
            }
            // Fase 3 (3.5-5.0s): Grietas se desvanecen
            else if (crackTimer < 5.0f) {
                crackAlpha = 1.0f - ((crackTimer - 3.5f) / 1.5f);  // 1 → 0
            }
            // Fin
            else {
                crackTimer = 0f;
                crackAlpha = 0f;
            }
        }

        // Dibujar objetos
        for (SceneObject obj : sceneObjects) {
            obj.update(dt);
            obj.draw();
        }

        // 💥 DIBUJAR FLASH BLANCO SI ESTÁ ACTIVO
        if (impactFlashAlpha > 0.01f) {
            drawImpactFlash();
        }

        // 💥 DIBUJAR GRIETAS DE PANTALLA ROTA SI ESTÁN ACTIVAS
        if (crackAlpha > 0.01f) {
            drawScreenCracks();
        }
    }

    private void prepareScene() {
        Log.d(TAG, "════════ Preparing Scene: " + selectedItem + " ════════");

        if (!textureManager.initialize()) {
            Log.e(TAG, "✗ ERROR: TextureManager could not initialize");
            return;
        }

        sceneObjects.clear();

        // ═══════════════════════════════════════════════════════════
        // 🎨 SELECTOR DE ESCENAS - 10 WALLPAPERS ÚNICOS
        // ═══════════════════════════════════════════════════════════
        switch (selectedItem) {
            case "DiscoBall":
                setupDiscoBallScene();
                break;
            case "Universo":
                setupUniverseScene();
                break;
            case "Bosque Encantado":
                setupBosqueScene();
                break;
            case "Neo Tokyo 2099":
                setupCyberpunkScene();
                break;
            case "Paraíso Dorado":
                setupPlayaScene();
                break;
            case "Safari Salvaje":
                setupSafariScene();
                break;
            case "Lluvia Mística":
                setupLluviaScene();
                break;
            case "Pixel Quest":
                setupRetroScene();
                break;
            case "Agujero Negro":
                setupBlackHoleScene();
                break;
            case "Jardín Zen":
                setupZenScene();
                break;
            case "Furia Celestial":
                setupTormentaScene();
                break;
            default:
                Log.w(TAG, "⚠️ Escena desconocida: " + selectedItem + " - usando Universo");
                setupUniverseScene();
                break;
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

        // SOL CENTRAL CON TEXTURA VOLCÁNICA REALISTA (CENTRADO EN 0,0,0)
        try {
            sol = new Planeta(
                    context, textureManager,
                    "shaders/planeta_vertex.glsl",
                    "shaders/planeta_iluminado_fragment.glsl",  // ✨ MISMO SHADER QUE LA TIERRA - MUESTRA LA TEXTURA
                    R.drawable.texturasolvolcanico,  // ✨ TEXTURA VOLCÁNICA REALISTA
                    0.0f,              // orbitRadiusX = 0 (centro)
                    0.0f,              // orbitRadiusZ = 0 (centro)
                    0.0f,              // orbitSpeed = 0 (sin órbita)
                    0.0f,              // scaleAmplitude = sin variación
                    0.45f,             // instanceScale = Sol grande (estrella principal)
                    35.0f,             // spinSpeed = rotación MUY visible
                    false, null, 1.0f,
                    null, 1.0f
            );
            if (sol instanceof CameraAware) {
                ((CameraAware) sol).setCameraController(sharedCamera);
            }
            sol.setMaxHealth(200);  // Sol tiene 200 HP (incrementado aún más para partidas más largas)
            sol.setOnExplosionListener(this);  // 💥 CONECTAR EXPLOSIÓN ÉPICA

            // ═══ 💾 CARGAR HP GUARDADO ═══
            sol.setPlayerStats(playerStats);  // Inyectar PlayerStats para auto-guardar
            int savedSunHP = playerStats.getSavedSunHealth();
            sol.setHealth(savedSunHP);  // Cargar HP guardado
            Log.d(TAG, "  💾 Sol HP cargado: " + savedSunHP + "/200");

            // ═══ 🌞 SINCRONIZACIÓN CON TIEMPO REAL ACELERADA ═══
            sol.setRealTimeRotation(true);           // Rotación sincronizada con tiempo real
            sol.setRealTimeRotationPeriod(27 * 24);  // Sol rota cada 27 días terrestres (648 horas)
            sol.setTimeAccelerationFactor(120.0f);   // Acelerar 120x para que sea visible
            Log.d(TAG, "  ⏰ Sol configurado: 27 días real → " + (27 * 24 * 60 / 120) + " min acelerado");

            sceneObjects.add(sol);
            Log.d(TAG, "  ✓ Sun added with Fresnel Glow shader - HP: 200");
            Log.d(TAG, "  💥 Explosion listener connected for EPIC particle show");
            Log.d(TAG, "  ✨ Fresnel Glow effect: Edges glow brighter than center");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creating sun: " + e.getMessage());
        }

        // ✨ 3 ESTRELLAS BAILARINAS - PARTÍCULAS MÁGICAS CON ESTELA ✨
        // Casi invisibles, solo se ve la estela arcoíris de cada una
        try {
            // Limpiar lista anterior por si acaso
            estrellasBailarinas.clear();

            // Estrella 1 - Posición superior derecha
            EstrellaBailarina estrella1 = new EstrellaBailarina(
                    context, textureManager,
                    1.8f, 0.8f, 0.5f,   // Posición inicial: arriba-derecha
                    0.02f,              // Escala: MINÚSCULA (casi invisible, solo estela)
                    45.0f               // Rotación: rápida
            );
            estrella1.setCameraController(sharedCamera);
            sceneObjects.add(estrella1);
            estrellasBailarinas.add(estrella1);

            

            // Estrella 2 - Posición izquierda
            EstrellaBailarina estrella2 = new EstrellaBailarina(
                    context, textureManager,
                    -1.5f, 0.3f, -0.8f,  // Posición inicial: izquierda-atrás
                    0.02f,               // Escala: MINÚSCULA
                    38.0f                // Rotación: ligeramente diferente
            );
            estrella2.setCameraController(sharedCamera);
            sceneObjects.add(estrella2);
            estrellasBailarinas.add(estrella2);

            // Estrella 3 - Posición abajo
            EstrellaBailarina estrella3 = new EstrellaBailarina(
                    context, textureManager,
                    0.5f, -0.6f, 1.2f,   // Posición inicial: abajo-adelante
                    0.02f,               // Escala: MINÚSCULA
                    52.0f                // Rotación: más rápida
            );
            estrella3.setCameraController(sharedCamera);
            sceneObjects.add(estrella3);
            estrellasBailarinas.add(estrella3);

            Log.d(TAG, "  ✨✨✨ 3 ESTRELLAS BAILARINAS agregadas (épico!) ✨✨✨");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando estrellas bailarinas: " + e.getMessage());
        }

        // 🌍 PLANETA TIERRA ORBITANDO AL SOL (INDICADOR DE HORAS)
        Planeta planetaTierra = null;  // Referencia para la Luna
        try {
            planetaTierra = new Planeta(
                    context, textureManager,
                    "shaders/planeta_vertex.glsl",
                    "shaders/planeta_iluminado_fragment.glsl",  // SHADER CON ILUMINACIÓN
                    R.drawable.texturaplanetatierra,            // ✨ TEXTURA DE LA TIERRA
                    2.4f, 2.0f, 0.25f,  // Órbita más cercana al Sol
                    0.1f,              // Poca variación
                    0.15f,             // Tamaño realista (más pequeña que el Sol)
                    80.0f,             // Rotación MUY visible
                    false, null, 1.0f,
                    null,
                    1.0f               // UV scale 1.0 para textura completa
            );
            if (planetaTierra instanceof CameraAware) {
                ((CameraAware) planetaTierra).setCameraController(sharedCamera);
            }

            // ═══ 🕐 RELOJ ASTRONÓMICO - TIERRA = SEGUNDOS (60 segundos por órbita) ═══
            planetaTierra.setRealTimeRotation(true);        // Rotación sincronizada
            planetaTierra.setRealTimeRotationPeriod(24);    // 24 horas/rotación
            planetaTierra.setRealTimeOrbit(true);           // Órbita = indicador de SEGUNDOS
            planetaTierra.setRealTimeOrbitPeriod(1.0f / 60.0f);    // 1/60 hora = 60 segundos
            planetaTierra.setTimeAccelerationFactor(1.0f);  // Sin aceleración - tiempo REAL
            Log.d(TAG, "  🕐 TIERRA configurada como indicador de SEGUNDOS:");
            Log.d(TAG, "     • Órbita completa = 60 segundos REALES");

            sceneObjects.add(planetaTierra);
            Log.d(TAG, "  🌍 TIERRA añadida orbitando al Sol con iluminación");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creating planet: " + e.getMessage());
        }

        // 🔴 PLANETA MARTE - INDICADOR DE MINUTOS (60 minutos por órbita)
        try {
            Planeta planetaMarte = new Planeta(
                    context, textureManager,
                    "shaders/planeta_vertex.glsl",
                    "shaders/planeta_iluminado_fragment.glsl",
                    R.drawable.textura_marte,            // Textura de Marte
                    3.2f, 2.7f, 0.30f,  // Órbita más cercana (ligeramente)
                    0.08f,              // Poca variación
                    0.12f,              // Tamaño realista (más pequeño que la Tierra)
                    90.0f,              // Rotación MUY visible (casi el doble)
                    false, null, 1.0f,
                    null,
                    1.0f
            );
            if (planetaMarte instanceof CameraAware) {
                ((CameraAware) planetaMarte).setCameraController(sharedCamera);
            }

            // ═══ 🕐 RELOJ ASTRONÓMICO - MARTE = CADA MINUTO (órbita rápida) ═══
            planetaMarte.setRealTimeRotation(true);
            planetaMarte.setRealTimeRotationPeriod(24);
            planetaMarte.setRealTimeOrbit(true);           // Órbita = cada minuto
            planetaMarte.setRealTimeOrbitPeriod(1.0f / 60.0f);  // 1/60 hora = 1 minuto REAL
            planetaMarte.setTimeAccelerationFactor(1.0f);  // Sin aceleración - tiempo REAL
            Log.d(TAG, "  🕐 MARTE configurado para órbita RÁPIDA:");
            Log.d(TAG, "     • Órbita completa = 1 minuto REAL (60 segundos)");

            sceneObjects.add(planetaMarte);
            Log.d(TAG, "  🔴 MARTE añadido orbitando al Sol");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creating Mars: " + e.getMessage());
        }

        // 🌙 LUNA - INDICADOR DE SEGUNDOS (60 segundos por órbita alrededor de la Tierra)
        try {
            Planeta planetaLuna = new Planeta(
                    context, textureManager,
                    "shaders/planeta_vertex.glsl",
                    "shaders/planeta_iluminado_fragment.glsl",
                    R.drawable.textura_luna,             // Textura de la Luna
                    0.6f, 0.5f, 1.0f,    // Órbita más cercana a la Tierra
                    0.05f,               // Muy poca variación
                    0.06f,               // Pequeña (proporción realista con la Tierra)
                    20.0f,               // Rotación visible
                    false, null, 1.0f,
                    null,
                    1.0f
            );
            if (planetaLuna instanceof CameraAware) {
                ((CameraAware) planetaLuna).setCameraController(sharedCamera);
            }

            // ═══ 🕐 RELOJ ASTRONÓMICO - LUNA = SEGUNDOS (40 segundos por órbita - más rápida) ═══
            // La Luna orbita la Tierra (no el Sol)
            if (planetaTierra != null) {
                planetaLuna.setParentPlanet(planetaTierra);
                Log.d(TAG, "     • Luna configurada para orbitar la Tierra");
            }

            planetaLuna.setRealTimeRotation(true);
            planetaLuna.setRealTimeRotationPeriod(1);
            planetaLuna.setRealTimeOrbit(true);            // Órbita = indicador de SEGUNDOS
            planetaLuna.setRealTimeOrbitPeriod(1.0f / 90.0f);  // 1/90 hora = 40 segundos (más rápida)
            planetaLuna.setTimeAccelerationFactor(1.0f);   // Sin aceleración - tiempo REAL
            Log.d(TAG, "  🕐 LUNA configurada como indicador de SEGUNDOS:");
            Log.d(TAG, "     • Órbita completa = 40 segundos REALES (acelerada)");
            Log.d(TAG, "     • Orbita alrededor de la TIERRA (no del Sol)");

            sceneObjects.add(planetaLuna);
            Log.d(TAG, "  🌙 LUNA añadida orbitando a la Tierra");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creating Moon: " + e.getMessage());
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

        // 👋 SALUDO PERSONALIZADO CON NOMBRE DE USUARIO
        try {
            GreetingText greetingText = new GreetingText(context);
            sceneObjects.add(greetingText);
            Log.d(TAG, "  👋 Greeting text added");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creating greeting text: " + e.getMessage());
        }

        // CAMPO DE FUERZA INTERACTIVO DEL SOL - CASI INVISIBLE, MÁS GRANDE
        try {
            forceField = new ForceField(
                    context, textureManager,
                    0.0f, 0.0f, 0.0f,   // CENTRADO con el sol en (0, 0, 0)
                    0.85f,              // Radio más grande que antes (0.85 vs 0.68)
                    R.drawable.fondo_transparente,  // Textura transparente para efectos puros
                    new float[]{0.3f, 0.9f, 1.0f},  // Color azul eléctrico suave
                    0.0f,               // ✨ CASI INVISIBLE (alpha 0%, solo impactos)
                    0.03f,              // Pulsación ULTRA sutil (3% de variación)
                    0.3f                // Pulsación ULTRA LENTA
            );
            forceField.setCameraController(sharedCamera);

            // ═══ 💾 CARGAR HP GUARDADO ═══
            forceField.setPlayerStats(playerStats);  // Inyectar PlayerStats para auto-guardar
            int savedForceFieldHP = playerStats.getSavedForceFieldHealth();
            forceField.setHealth(savedForceFieldHP);  // Cargar HP guardado
            Log.d(TAG, "  💾 ForceField HP cargado: " + savedForceFieldHP + "/50");

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
                    200,  // Max HP = 200 (incrementado aún más)
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
                    50,  // Max HP = 50 (incrementado para que dure más)
                    new float[]{0.2f, 0.6f, 1.0f, 1.0f},  // Azul lleno
                    new float[]{1.0f, 0.0f, 0.0f, 1.0f}   // Rojo vacío
            );
            sceneObjects.add(hpBarForceField);

            Log.d(TAG, "[SceneRenderer] ✓ Barras HP agregadas (Sol y Escudo)");
        } catch (Exception e) {
            Log.e(TAG, "[SceneRenderer] ✗ Error creando barras HP: " + e.getMessage());
        }

        // 👆 BARRA DE CARGA DE PODER (PARA DISPARAR METEORITOS)
        try {
            chargePowerBar = new HPBar(
                    context,
                    "⚡ PODER",
                    0.35f, 0.15f,  // Posición: centro-abajo
                    0.30f, 0.04f,  // Tamaño: más ancha y gruesa
                    100,  // Max = 100 (porcentaje)
                    new float[]{1.0f, 0.9f, 0.2f, 1.0f},  // Amarillo brillante
                    new float[]{0.3f, 0.3f, 0.3f, 0.5f}   // Gris oscuro vacío
            );
            chargePowerBar.setHealth(0);  // Empieza vacía
            sceneObjects.add(chargePowerBar);
            Log.d(TAG, "  ⚡✓ Barra de carga de poder agregada");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ ERROR creando barra de carga: " + e.getMessage());
        }

        // 🎵 INDICADOR DE ESTADO MUSICAL 🎵
        // OCULTO VISUALMENTE - Solo se usa internamente para monitoreo
        try {
            musicStatusBar = new HPBar(
                    context,
                    "♪ AUDIO",
                    0.05f, 1.82f,
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

        // 💥 BARRA DE COUNTDOWN PARA METEORITO DE PANTALLA 💥
        MeteorCountdownBar meteorCountdownBar = null;
        try {
            meteorCountdownBar = new MeteorCountdownBar(
                    context,
                    0.70f, 0.87f,  // Posición: alineada con barra del escudo (Y=0.87)
                    0.25f, 0.025f  // Tamaño: más fina que HP bar
            );
            sceneObjects.add(meteorCountdownBar);
            Log.d(TAG, "  💥✓ Barra de countdown alineada con escudo");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ ERROR creando barra de countdown: " + e.getMessage());
        }

        // 🎵 INDICADOR VISUAL DE MÚSICA 🎵
        // Muestra 3 barras (BASS, MID, TREBLE) CENTRADAS, ARRIBA DEL SOL
        try {
            Log.d(TAG, "╔════════════════════════════════════════╗");
            Log.d(TAG, "║   CREANDO INDICADOR DE MÚSICA         ║");
            Log.d(TAG, "╚════════════════════════════════════════╝");

            musicIndicator = new MusicIndicator(
                    context,
                    -0.15f,   // X: Centrado (ligeramente a la izquierda del centro)
                    0.75f,    // Y: Parte superior de la pantalla
                    0.30f,    // Ancho: HORIZONTAL (más ancho que alto)
                    0.08f     // Alto: Delgado y compacto
            );
            sceneObjects.add(musicIndicator);
            Log.d(TAG, "  🎵✓ INDICADOR DE MÚSICA agregado - CENTRADO, ARRIBA del sol");
        } catch (Exception e) {
            Log.e(TAG, "  ✗✗✗ ERROR CRÍTICO creando indicador de música: " + e.getMessage());
            e.printStackTrace();
        }

        // ☀️💀 CONTADOR DE SOLES DESTRUIDOS
        try {
            Log.d(TAG, "╔════════════════════════════════════════╗");
            Log.d(TAG, "║   CREANDO CONTADOR SOLES DESTRUIDOS   ║");
            Log.d(TAG, "╚════════════════════════════════════════╝");

            sunsDestroyedCounter = new SimpleTextRenderer(
                    context,
                    0.50f,    // X: Esquina superior derecha
                    0.75f,    // Y: Parte superior (mismo nivel que MusicIndicator)
                    0.40f,    // Ancho
                    0.10f     // Alto
            );
            sunsDestroyedCounter.setColor(android.graphics.Color.rgb(255, 200, 50));  // Amarillo dorado

            // Inicializar con el valor actual de PlayerStats (puede ser de Firebase o local)
            if (playerStats != null) {
                int currentSuns = playerStats.getSunsDestroyed();
                sunsDestroyedCounter.setText("☀️" + currentSuns);
                Log.d(TAG, "  ☀️ Contador inicializado con: " + currentSuns + " soles");
            } else {
                sunsDestroyedCounter.setText("☀️0");
            }

            sceneObjects.add(sunsDestroyedCounter);
            Log.d(TAG, "  ☀️✓ CONTADOR agregado - esquina superior derecha");
        } catch (Exception e) {
            Log.e(TAG, "  ✗✗✗ ERROR CRÍTICO creando contador: " + e.getMessage());
            e.printStackTrace();
        }

        // 🏆 SISTEMA DE LEADERBOARD (Top 4)
        try {
            Log.d(TAG, "╔════════════════════════════════════════╗");
            Log.d(TAG, "║   INICIALIZANDO LEADERBOARD           ║");
            Log.d(TAG, "╚════════════════════════════════════════╝");

            // Inicializar managers
            botManager = BotManager.getInstance();
            leaderboardManager = LeaderboardManager.getInstance();

            // Inicializar bots (solo primera vez)
            botManager.initializeBots(new BotManager.InitCallback() {
                @Override
                public void onComplete() {
                    Log.d(TAG, "🤖 Bots inicializados");
                    // Actualizar leaderboard después de inicializar bots
                    updateLeaderboardUI();
                }
            });

            // Crear textos para Top 3 (horizontal, de izquierda a derecha)
            float startX = -0.95f;  // Comienza en el borde izquierdo
            float y = -0.50f;       // Más arriba para no taparse con iconos del sistema
            float width = 0.45f;    // Ancho de cada texto
            float spacing = 0.60f;  // Espaciado entre textos

            for (int i = 0; i < 3; i++) {
                float x = startX + (i * spacing);
                leaderboardTexts[i] = new SimpleTextRenderer(context, x, y, width, 0.08f);
                leaderboardTexts[i].setColor(android.graphics.Color.WHITE);
                leaderboardTexts[i].setText("#" + (i+1) + " ---");
                sceneObjects.add(leaderboardTexts[i]);
            }

            Log.d(TAG, "  🏆✓ LEADERBOARD UI creado - 3 posiciones");
        } catch (Exception e) {
            Log.e(TAG, "  ✗✗✗ ERROR CRÍTICO creando leaderboard: " + e.getMessage());
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

            // 💥 Conectar sistema de impacto en pantalla
            meteorShower.setSceneRenderer(this);

            // 💥 Conectar barra de countdown de meteorito
            if (meteorCountdownBar != null) {
                meteorShower.setCountdownBar(meteorCountdownBar);
                Log.d(TAG, "[SceneRenderer] ✓ Barra de countdown conectada con MeteorShower");
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

        // 🎮 ARMA DEL JUGADOR - SISTEMA DE DISPARO CONTROLADO
        try {
            playerWeapon = new PlayerWeapon(context, textureManager);
            playerWeapon.setCameraController(sharedCamera);

            // Conectar con MeteorShower para que maneje las colisiones
            if (meteorShower != null) {
                playerWeapon.setMeteorShower(meteorShower);
            }

            sceneObjects.add(playerWeapon);
            Log.d(TAG, "[SceneRenderer] 🎮 Sistema de arma del jugador agregado");
        } catch (Exception e) {
            Log.e(TAG, "[SceneRenderer] ✗ Error creando arma del jugador: " + e.getMessage());
        }

        // ✨ AVATAR DEL USUARIO - ESFERA 3D FLOTANTE ✨
        // Carga la foto de perfil del usuario y la muestra orbitando el sol
        try {
            Log.d(TAG, "╔════════════════════════════════════════╗");
            Log.d(TAG, "║   CARGANDO AVATAR DEL USUARIO        ║");
            Log.d(TAG, "╚════════════════════════════════════════╝");

            // Crear AvatarSphere (se creará sin textura primero)
            final AvatarSphere avatarSphere = new AvatarSphere(context, textureManager, null);
            // Le pasamos la cámara para que pueda calcular MVP, pero se mantiene en posición fija
            avatarSphere.setCameraController(sharedCamera);
            sceneObjects.add(avatarSphere);

            // Cargar avatar del usuario de forma asíncrona
            AvatarLoader.loadCurrentUserAvatar(context, new AvatarLoader.AvatarLoadListener() {
                @Override
                public void onAvatarLoaded(android.graphics.Bitmap bitmap) {
                    // Avatar cargado exitosamente
                    avatarSphere.updateAvatar(bitmap);
                    Log.d(TAG, "  ✨✓ AVATAR DEL USUARIO CARGADO EN 3D ✨");
                }

                @Override
                public void onAvatarLoadFailed() {
                    Log.w(TAG, "  ⚠️ No se pudo cargar el avatar del usuario");
                }
            });

            Log.d(TAG, "  ✓ AvatarSphere agregado (cargando textura...)");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando avatar sphere: " + e.getMessage());
            e.printStackTrace();
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

    /**
     * ============================================
     * DISCO BALL SCENE - Music Visualization
     * ============================================
     * Features:
     *  - Central disco ball with mirror tiles
     *  - Rotating spotlights (laser beams)
     *  - Bokeh background effect
     *  - Cinematic camera movements
     *  - Audio reactive breathing
     */
    private void setupDiscoBallScene() {
        Log.d(TAG, "Setting up DISCO BALL scene...");

        // ============================================
        // FONDO OSCURO (club atmosphere)
        // ============================================
        // Por ahora sin fondo - el clear color es negro (ya está configurado)
        // TODO FASE B: Agregar StarryBackground o bokeh effect
        Log.d(TAG, "  ✓ Using black clear color as background");

        // ============================================
        // BOLA DISCO CENTRAL ⭐
        // ============================================
        try {
            DiscoBall discoBall = new DiscoBall(
                    context,
                    1.0f,  // spinSpeed: 1 rad/sec (rotación suave)
                    2.5f   // scale: bola grande y prominente
            );
            discoBall.setCameraController(sharedCamera);
            sceneObjects.add(discoBall);
            Log.d(TAG, "  ✓ Disco ball added (spin: 1.0 rad/s, scale: 2.5)");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creating disco ball: " + e.getMessage());
            e.printStackTrace();
        }

        // ============================================
        // TODO FASE B: RAYOS LASER
        // ============================================
        // Agregar 4-6 laser beams rotando alrededor de la bola
        // usando beam.obj con colores cyan, magenta, yellow

        // ============================================
        // TODO FASE B: PARTÍCULAS DE HUMO
        // ============================================
        // Plano con shader de niebla sutil

        // ============================================
        // TODO FASE C: SISTEMA CINEMÁTICO
        // ============================================
        // CinematicSequence con 5 shots predefinidos

        Log.d(TAG, "✓ Disco ball scene setup complete (FASE A - básico)");
    }

    // ═══════════════════════════════════════════════════════════════
    // 🌲 BOSQUE ENCANTADO - Verde oscuro mágico
    // ═══════════════════════════════════════════════════════════════
    private void setupBosqueScene() {
        Log.d(TAG, "Setting up BOSQUE ENCANTADO scene...");

        // Color de fondo: Verde bosque oscuro
        GLES20.glClearColor(0.04f, 0.18f, 0.12f, 1f);  // #0A2F1F

        // Planeta central verde (simulando árbol mágico)
        try {
            Planeta arbolMagico = new Planeta(
                    context, textureManager,
                    "shaders/planeta_vertex.glsl",
                    "shaders/planeta_fragment.glsl",
                    R.drawable.textura_roninplaneta,
                    0.0f, 0.0f, 0.0f,
                    0.1f, 1.5f, 5.0f,
                    true, new float[]{0.2f, 0.6f, 0.2f, 1.0f}, 1.0f,
                    null, 1.0f
            );
            if (arbolMagico instanceof CameraAware) {
                ((CameraAware) arbolMagico).setCameraController(sharedCamera);
            }
            sceneObjects.add(arbolMagico);
            Log.d(TAG, "  ✓ Bosque scene - árbol mágico verde");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creating bosque: " + e.getMessage());
        }

        Log.d(TAG, "✓ Bosque Encantado scene complete");
    }

    // ═══════════════════════════════════════════════════════════════
    // 🏙️ NEO TOKYO 2099 - Rosa neón cyberpunk
    // ═══════════════════════════════════════════════════════════════
    private void setupCyberpunkScene() {
        Log.d(TAG, "Setting up CYBERPUNK scene...");

        // Color de fondo: Rosa neón intenso
        GLES20.glClearColor(1.0f, 0.0f, 0.5f, 1f);  // #FF0080

        // Esfera neón pulsante (edificio holográfico)
        try {
            Planeta neonSphere = new Planeta(
                    context, textureManager,
                    "shaders/planeta_vertex.glsl",
                    "shaders/planeta_fragment.glsl",
                    R.drawable.agujero_negro,
                    0.0f, 0.0f, 0.0f,
                    0.2f, 1.2f, 10.0f,
                    true, new float[]{1.0f, 0.0f, 1.0f, 1.0f}, 0.8f,
                    0.15f, 1.0f
            );
            if (neonSphere instanceof CameraAware) {
                ((CameraAware) neonSphere).setCameraController(sharedCamera);
            }
            sceneObjects.add(neonSphere);
            Log.d(TAG, "  ✓ Cyberpunk scene - esfera neón rosa");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creating cyberpunk: " + e.getMessage());
        }

        Log.d(TAG, "✓ Neo Tokyo 2099 scene complete");
    }

    // ═══════════════════════════════════════════════════════════════
    // 🏖️ PARAÍSO DORADO - Naranja atardecer
    // ═══════════════════════════════════════════════════════════════
    private void setupPlayaScene() {
        Log.d(TAG, "Setting up PLAYA scene...");

        // Color de fondo: Naranja dorado atardecer
        GLES20.glClearColor(1.0f, 0.55f, 0.0f, 1f);  // #FF8C00

        // Sol de atardecer (amarillo-naranja)
        try {
            Planeta solAtardecer = new Planeta(
                    context, textureManager,
                    "shaders/planeta_vertex.glsl",
                    "shaders/planeta_fragment.glsl",
                    R.drawable.textura_sol,
                    0.0f, 0.0f, 0.0f,
                    0.15f, 1.8f, 2.0f,
                    true, new float[]{1.0f, 0.7f, 0.0f, 1.0f}, 1.0f,
                    0.1f, 1.0f
            );
            if (solAtardecer instanceof CameraAware) {
                ((CameraAware) solAtardecer).setCameraController(sharedCamera);
            }
            sceneObjects.add(solAtardecer);
            Log.d(TAG, "  ✓ Playa scene - sol dorado");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creating playa: " + e.getMessage());
        }

        Log.d(TAG, "✓ Paraíso Dorado scene complete");
    }

    // ═══════════════════════════════════════════════════════════════
    // 🦁 SAFARI SALVAJE - Amarillo tierra savanna
    // ═══════════════════════════════════════════════════════════════
    private void setupSafariScene() {
        Log.d(TAG, "Setting up SAFARI scene...");

        // Color de fondo: Amarillo tierra/savanna
        GLES20.glClearColor(0.85f, 0.65f, 0.13f, 1f);  // #DAA520

        // Planeta texturizado (tierra africana)
        try {
            Planeta savanna = new Planeta(
                    context, textureManager,
                    "shaders/planeta_vertex.glsl",
                    "shaders/planeta_fragment.glsl",
                    R.drawable.textura_asteroide,
                    0.0f, 0.0f, 0.0f,
                    0.1f, 1.6f, 8.0f,
                    true, new float[]{0.9f, 0.7f, 0.2f, 1.0f}, 1.0f,
                    null, 1.0f
            );
            if (savanna instanceof CameraAware) {
                ((CameraAware) savanna).setCameraController(sharedCamera);
            }
            sceneObjects.add(savanna);
            Log.d(TAG, "  ✓ Safari scene - sabana dorada");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creating safari: " + e.getMessage());
        }

        Log.d(TAG, "✓ Safari Salvaje scene complete");
    }

    // ═══════════════════════════════════════════════════════════════
    // 🌧️ LLUVIA MÍSTICA - Gris azulado tormentoso
    // ═══════════════════════════════════════════════════════════════
    private void setupLluviaScene() {
        Log.d(TAG, "Setting up LLUVIA scene...");

        // Color de fondo: Gris pizarra tormentoso
        GLES20.glClearColor(0.18f, 0.31f, 0.31f, 1f);  // #2F4F4F

        // Planeta oscuro con lluvia
        try {
            Planeta tormenta = new Planeta(
                    context, textureManager,
                    "shaders/planeta_vertex.glsl",
                    "shaders/planeta_fragment.glsl",
                    R.drawable.universo03,
                    0.0f, 0.0f, 0.0f,
                    0.2f, 1.4f, 6.0f,
                    true, new float[]{0.3f, 0.5f, 0.6f, 1.0f}, 0.9f,
                    null, 1.0f
            );
            if (tormenta instanceof CameraAware) {
                ((CameraAware) tormenta).setCameraController(sharedCamera);
            }
            sceneObjects.add(tormenta);
            Log.d(TAG, "  ✓ Lluvia scene - tormenta gris");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creating lluvia: " + e.getMessage());
        }

        Log.d(TAG, "✓ Lluvia Mística scene complete");
    }

    // ═══════════════════════════════════════════════════════════════
    // 🎮 PIXEL QUEST - Magenta retro gaming
    // ═══════════════════════════════════════════════════════════════
    private void setupRetroScene() {
        Log.d(TAG, "Setting up RETRO scene...");

        // Color de fondo: Magenta retro gaming
        GLES20.glClearColor(1.0f, 0.0f, 1.0f, 1f);  // #FF00FF

        // Cubo pixelado (8-bit style)
        try {
            Planeta pixelCube = new Planeta(
                    context, textureManager,
                    "shaders/planeta_vertex.glsl",
                    "shaders/planeta_fragment.glsl",
                    R.drawable.fondo_transparente,
                    0.0f, 0.0f, 0.0f,
                    0.3f, 1.5f, 15.0f,
                    true, new float[]{1.0f, 0.0f, 1.0f, 1.0f}, 1.0f,
                    0.2f, 1.0f
            );
            if (pixelCube instanceof CameraAware) {
                ((CameraAware) pixelCube).setCameraController(sharedCamera);
            }
            sceneObjects.add(pixelCube);
            Log.d(TAG, "  ✓ Retro scene - cubo magenta");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creating retro: " + e.getMessage());
        }

        Log.d(TAG, "✓ Pixel Quest scene complete");
    }

    // ═══════════════════════════════════════════════════════════════
    // 🌸 JARDÍN ZEN - Rosa sakura suave
    // ═══════════════════════════════════════════════════════════════
    private void setupZenScene() {
        Log.d(TAG, "Setting up ZEN scene...");

        // Color de fondo: Rosa sakura suave
        GLES20.glClearColor(1.0f, 0.72f, 0.77f, 1f);  // #FFB7C5

        // Planeta rosa (flor de cerezo)
        try {
            Planeta sakura = new Planeta(
                    context, textureManager,
                    "shaders/planeta_vertex.glsl",
                    "shaders/planeta_fragment.glsl",
                    R.drawable.textura_roninplaneta,
                    0.0f, 0.0f, 0.0f,
                    0.1f, 1.3f, 4.0f,
                    true, new float[]{1.0f, 0.8f, 0.9f, 1.0f}, 1.0f,
                    0.05f, 1.0f
            );
            if (sakura instanceof CameraAware) {
                ((CameraAware) sakura).setCameraController(sharedCamera);
            }
            sceneObjects.add(sakura);
            Log.d(TAG, "  ✓ Zen scene - sakura rosa");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creating zen: " + e.getMessage());
        }

        Log.d(TAG, "✓ Jardín Zen scene complete");
    }

    // ═══════════════════════════════════════════════════════════════
    // ⚡ FURIA CELESTIAL - Amarillo eléctrico brillante
    // ═══════════════════════════════════════════════════════════════
    private void setupTormentaScene() {
        Log.d(TAG, "Setting up TORMENTA scene...");

        // Color de fondo: Amarillo eléctrico brillante
        GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1f);  // #FFFF00

        // Esfera eléctrica pulsante
        try {
            Planeta rayo = new Planeta(
                    context, textureManager,
                    "shaders/planeta_vertex.glsl",
                    "shaders/planeta_fragment.glsl",
                    R.drawable.textura_sol,
                    0.0f, 0.0f, 0.0f,
                    0.25f, 1.4f, 12.0f,
                    true, new float[]{1.0f, 1.0f, 0.0f, 1.0f}, 1.0f,
                    0.25f, 1.0f
            );
            if (rayo instanceof CameraAware) {
                ((CameraAware) rayo).setCameraController(sharedCamera);
            }
            sceneObjects.add(rayo);
            Log.d(TAG, "  ✓ Tormenta scene - rayo amarillo");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creating tormenta: " + e.getMessage());
        }

        Log.d(TAG, "✓ Furia Celestial scene complete");
    }

    public void pause() {
        paused = true;
        if (musicVisualizer != null) {
            musicVisualizer.pause();
        }

        // 🎮 FINALIZAR SESIÓN DE JUEGO
        playerStats.endSession();
        playerStats.saveStats();

        Log.d(TAG, "Renderer PAUSED");
    }

    public void resume() {
        paused = false;
        lastTime = System.nanoTime();
        if (musicVisualizer != null) {
            musicVisualizer.resume();
        }

        // 🎮 INICIAR NUEVA SESIÓN DE JUEGO
        playerStats.startSession();
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
        if (item != null) {
            if (!item.equals(selectedItem)) {
                Log.d(TAG, "Scene change requested: " + selectedItem + " → " + item);
                Log.d(TAG, "⚠️  Marcando escena para recreación en GL thread (thread-safe)");
                this.selectedItem = item;
                // NO llamar prepareScene() aquí - puede estar en MAIN THREAD
                // En su lugar, marcar flag para recrear en GL thread (onDrawFrame)
                needsSceneRecreation = true;
            } else {
                // Mismo wallpaper, pero forzar recreación de escena
                // (útil cuando el GL context fue recreado)
                Log.d(TAG, "Scene refresh requested for: " + selectedItem);
                Log.d(TAG, "⚠️  Marcando escena para recreación en GL thread (thread-safe)");
                needsSceneRecreation = true;
            }
        }
    }

    // ===== 👆 SISTEMA DE TOQUE INTERACTIVO 👆 =====

    /**
     * Maneja eventos de toque del usuario
     * PROTEGIDO contra interferencia del menú de Android
     */
    public void onTouchEvent(android.view.MotionEvent event) {
        int action = event.getAction();

        try {
            switch (action) {
                case android.view.MotionEvent.ACTION_DOWN:
                    // Usuario empezó a tocar
                    isTouching = true;
                    touchStartTime = 0f;  // Se actualizará en onDrawFrame
                    chargeLevel = 0f;
                    touchX = event.getX();
                    touchY = event.getY();
                    Log.d(TAG, String.format("👆 TOUCH DOWN en (%.0f, %.0f)", touchX, touchY));
                    break;

                case android.view.MotionEvent.ACTION_MOVE:
                    // Usuario está moviendo el dedo (actualizar posición)
                    if (isTouching) {
                        touchX = event.getX();
                        touchY = event.getY();
                    }
                    break;

                case android.view.MotionEvent.ACTION_UP:
                    // Usuario soltó el dedo - DISPARAR METEORITO
                    if (isTouching && chargeLevel > 0.1f) {  // Mínimo 10% de carga
                        shootMeteor(chargeLevel);
                        Log.d(TAG, String.format("🚀 DISPARAR - Carga: %.0f%%", chargeLevel * 100));
                    }
                    isTouching = false;
                    chargeLevel = 0f;
                    break;

                case android.view.MotionEvent.ACTION_CANCEL:
                    // Sistema canceló el toque (ej: menú de Android apareció)
                    // NO disparar, solo limpiar estado
                    Log.d(TAG, "⚠️ Touch CANCELADO por sistema (menú Android?) - limpiando estado");
                    isTouching = false;
                    chargeLevel = 0f;
                    break;
            }
        } catch (Exception e) {
            // Protección contra crashes
            Log.e(TAG, "✗ Error en onTouchEvent: " + e.getMessage());
            isTouching = false;
            chargeLevel = 0f;
        }
    }

    /**
     * Actualiza el sistema de carga de poder (llamado desde onDrawFrame)
     * PROTEGIDO: Auto-cancela si excede tiempo máximo
     */
    private void updateChargeSystem(float dt) {
        if (isTouching) {
            touchStartTime += dt;
            chargeLevel = Math.min(1.0f, touchStartTime / MAX_CHARGE_TIME);

            // PROTECCIÓN: Si alcanza el tiempo máximo, disparar automáticamente
            // Esto previene conflictos con el menú de Android
            if (touchStartTime >= MAX_CHARGE_TIME) {
                if (chargeLevel > 0.1f) {
                    shootMeteor(chargeLevel);
                    Log.d(TAG, "⚡ AUTO-DISPARO al 100% (prevención de menú Android)");
                }
                isTouching = false;
                chargeLevel = 0f;
                touchStartTime = 0f;
            }

            // Actualizar barra visual
            if (chargePowerBar != null) {
                try {
                    chargePowerBar.setHealth((int)(chargeLevel * 100));  // 0-100
                } catch (Exception e) {
                    Log.e(TAG, "Error actualizando barra de carga: " + e.getMessage());
                }
            }

            // Log reducido para performance
            if (frameCount % 120 == 0) {
                Log.d(TAG, String.format("⚡ Cargando: %.0f%%", chargeLevel * 100));
            }
        } else {
            // Resetear barra cuando no está tocando
            if (chargePowerBar != null) {
                try {
                    chargePowerBar.setHealth(0);
                } catch (Exception e) {
                    // Ignorar errores al resetear
                }
            }
        }
    }

    /**
     * Dispara un meteorito hacia el sol
     * PROTEGIDO contra crashes
     *
     * 🌟 NUEVO: Si la barra de combo está llena (x10), dispara MÚLTIPLES meteoritos épicos
     */
    private void shootMeteor(float power) {
        try {
            if (playerWeapon == null) {
                Log.w(TAG, "⚠️ PlayerWeapon no disponible");
                return;
            }

            if (power < 0.01f || power > 1.0f) {
                Log.w(TAG, "⚠️ Poder inválido: " + power + " (debe ser 0.0-1.0)");
                return;
            }

            // VERIFICAR SI LA BARRA DE COMBO ESTÁ LLENA (COMBO x10)
            if (meteorShower != null && meteorShower.isComboReady()) {
                // 🌟💥 DISPARO ÉPICO - ¡MÚLTIPLES METEORITOS!
                playerWeapon.shootEpic();

                // Resetear el combo en MeteorShower
                meteorShower.resetCombo();

                Log.d(TAG, "╔════════════════════════════════════════════════════════╗");
                Log.d(TAG, "║                                                        ║");
                Log.d(TAG, "║  🌟💥 DISPARO ÉPICO ACTIVADO! 💥🌟                   ║");
                Log.d(TAG, "║  ¡MÚLTIPLES METEORITOS LANZADOS!                      ║");
                Log.d(TAG, "║                                                        ║");
                Log.d(TAG, "╚════════════════════════════════════════════════════════╝");
            } else {
                // DISPARO NORMAL - UN SOLO METEORITO
                playerWeapon.shootSingle(power);
                Log.d(TAG, String.format("🚀 DISPARO - Poder: %.0f%%", power * 100));
            }

        } catch (Exception e) {
            Log.e(TAG, "✗ Error disparando meteorito: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===== 💥 SISTEMA DE IMPACTO EN PANTALLA 💥 =====

    /**
     * Activa efecto de impacto en pantalla (screen shake + flash blanco)
     * @param intensity Intensidad del impacto (0.0 - 1.0)
     */
    public void triggerScreenImpact(float intensity) {
        // Screen shake
        if (sharedCamera != null) {
            sharedCamera.triggerScreenShake(intensity * 0.8f, 0.3f);
        }

        // Flash blanco
        impactFlashAlpha = intensity * 0.6f;  // Máximo 60% de alpha para no cegar
        impactFlashTimer = 0.25f;  // 0.25 segundos

        Log.d(TAG, String.format("💥 IMPACTO EN PANTALLA! Intensidad: %.0f%%", intensity * 100));
    }

    /**
     * 💥💥 Activa efecto de PANTALLA ROTA con grietas procedurales
     * @param screenX Posición X del impacto en coordenadas de pantalla (0-1)
     * @param screenY Posición Y del impacto en coordenadas de pantalla (0-1)
     * @param intensity Intensidad del impacto (0.0 - 1.0)
     */
    public void triggerScreenCrack(float screenX, float screenY, float intensity) {
        // Screen shake MÁS FUERTE
        if (sharedCamera != null) {
            sharedCamera.triggerScreenShake(intensity * 1.2f, 0.5f);
        }

        // Flash blanco MÁS INTENSO
        impactFlashAlpha = intensity * 0.8f;  // Máximo 80%
        impactFlashTimer = 0.4f;

        // GRIETAS
        crackX = screenX;
        crackY = screenY;
        crackTimer = 0.01f;  // Iniciar animación
        crackAlpha = 0f;

        Log.d(TAG, "╔════════════════════════════════════════════════════════╗");
        Log.d(TAG, "║                                                        ║");
        Log.d(TAG, "║    💥💥💥 ¡PANTALLA ROTA! 💥💥💥                      ║");
        Log.d(TAG, "║                                                        ║");
        Log.d(TAG, String.format("║    Impacto en: (%.2f, %.2f)                           ║", screenX, screenY));
        Log.d(TAG, String.format("║    Intensidad: %.0f%%                                  ║", intensity * 100));
        Log.d(TAG, "║                                                        ║");
        Log.d(TAG, "╚════════════════════════════════════════════════════════╝");
    }

    /**
     * Dibuja un flash blanco semi-transparente en toda la pantalla
     */
    private void drawImpactFlash() {
        // Desactivar depth test y habilitar blending
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // ╔═════════════════════════════════════════════════════════╗
        // ║  INICIALIZACIÓN LAZY DEL SHADER (solo primera vez)     ║
        // ╚═════════════════════════════════════════════════════════╝
        if (flashShaderProgramId == 0) {
            // Shader muy simple para dibujar quad 2D con color
            String vertexShader =
                "attribute vec2 a_Position;\n" +
                "attribute vec4 a_Color;\n" +
                "varying vec4 v_Color;\n" +
                "void main() {\n" +
                "    v_Color = a_Color;\n" +
                "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
                "}\n";

            String fragmentShader =
                "#ifdef GL_ES\n" +
                "precision mediump float;\n" +
                "#endif\n" +
                "varying vec4 v_Color;\n" +
                "void main() {\n" +
                "    gl_FragColor = v_Color;\n" +
                "}\n";

            int vShader = ShaderUtils.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
            int fShader = ShaderUtils.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

            flashShaderProgramId = GLES20.glCreateProgram();
            GLES20.glAttachShader(flashShaderProgramId, vShader);
            GLES20.glAttachShader(flashShaderProgramId, fShader);
            GLES20.glLinkProgram(flashShaderProgramId);

            // Verificar link
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(flashShaderProgramId, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] == 0) {
                Log.e(TAG, "💥 Flash shader link failed: " + GLES20.glGetProgramInfoLog(flashShaderProgramId));
                flashShaderProgramId = 0;
                return;
            }

            GLES20.glDeleteShader(vShader);
            GLES20.glDeleteShader(fShader);

            flashAPositionLoc = GLES20.glGetAttribLocation(flashShaderProgramId, "a_Position");
            flashAColorLoc = GLES20.glGetAttribLocation(flashShaderProgramId, "a_Color");

            Log.d(TAG, "💥 Flash shader creado - ID: " + flashShaderProgramId);
        }

        // ╔═════════════════════════════════════════════════════════╗
        // ║  DIBUJAR QUAD BLANCO SEMI-TRANSPARENTE                 ║
        // ╚═════════════════════════════════════════════════════════╝
        if (flashShaderProgramId > 0 && GLES20.glIsProgram(flashShaderProgramId)) {
            GLES20.glUseProgram(flashShaderProgramId);

            // Vértices en NDC que cubren toda la pantalla (TRIANGLE_STRIP)
            float[] vertices = {
                -1.0f, -1.0f,  // Bottom-left
                 1.0f, -1.0f,  // Bottom-right
                -1.0f,  1.0f,  // Top-left
                 1.0f,  1.0f   // Top-right
            };

            // Color blanco con alpha variable
            float[] colors = new float[16];
            for (int i = 0; i < 4; i++) {
                colors[i * 4] = 1.0f;  // R
                colors[i * 4 + 1] = 1.0f;  // G
                colors[i * 4 + 2] = 1.0f;  // B
                colors[i * 4 + 3] = impactFlashAlpha;  // A
            }

            // Crear buffers
            java.nio.ByteBuffer vbb = java.nio.ByteBuffer.allocateDirect(vertices.length * 4);
            vbb.order(java.nio.ByteOrder.nativeOrder());
            java.nio.FloatBuffer vb = vbb.asFloatBuffer();
            vb.put(vertices);
            vb.position(0);

            java.nio.ByteBuffer cbb = java.nio.ByteBuffer.allocateDirect(colors.length * 4);
            cbb.order(java.nio.ByteOrder.nativeOrder());
            java.nio.FloatBuffer cb = cbb.asFloatBuffer();
            cb.put(colors);
            cb.position(0);

            // Configurar atributos
            GLES20.glEnableVertexAttribArray(flashAPositionLoc);
            GLES20.glVertexAttribPointer(flashAPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vb);

            GLES20.glEnableVertexAttribArray(flashAColorLoc);
            GLES20.glVertexAttribPointer(flashAColorLoc, 4, GLES20.GL_FLOAT, false, 0, cb);

            // Dibujar
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

            // Limpiar
            GLES20.glDisableVertexAttribArray(flashAPositionLoc);
            GLES20.glDisableVertexAttribArray(flashAColorLoc);
        }

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    /**
     * 💥💥 Dibuja grietas procedurales en la pantalla
     */
    private void drawScreenCracks() {
        // Desactivar depth test y habilitar blending
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // ╔═════════════════════════════════════════════════════════╗
        // ║  INICIALIZACIÓN LAZY DEL SHADER (solo primera vez)     ║
        // ╚═════════════════════════════════════════════════════════╝
        if (crackShaderProgramId == 0) {
            // Vertex shader simple
            String vertexShader =
                "attribute vec2 a_Position;\n" +
                "attribute vec2 a_TexCoord;\n" +
                "varying vec2 v_TexCoord;\n" +
                "void main() {\n" +
                "    v_TexCoord = a_TexCoord;\n" +
                "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
                "}\n";

            // Fragment shader MEJORADO - Grietas épicas y caóticas
            String fragmentShader =
                "#ifdef GL_ES\n" +
                "precision mediump float;\n" +
                "#endif\n" +
                "varying vec2 v_TexCoord;\n" +
                "uniform float u_Time;\n" +
                "uniform vec2 u_ImpactPos;\n" +
                "uniform float u_Alpha;\n" +
                "\n" +
                "// Funciones de ruido mejoradas\n" +
                "float hash(float n) {\n" +
                "    return fract(sin(n) * 43758.5453);\n" +
                "}\n" +
                "\n" +
                "float noise(vec2 p) {\n" +
                "    vec2 i = floor(p);\n" +
                "    vec2 f = fract(p);\n" +
                "    f = f * f * (3.0 - 2.0 * f);\n" +
                "    float n = i.x + i.y * 57.0;\n" +
                "    return mix(mix(hash(n), hash(n + 1.0), f.x),\n" +
                "               mix(hash(n + 57.0), hash(n + 58.0), f.x), f.y);\n" +
                "}\n" +
                "\n" +
                "void main() {\n" +
                "    vec2 uv = v_TexCoord;\n" +
                "    vec2 toImpact = uv - u_ImpactPos;\n" +
                "    float dist = length(toImpact);\n" +
                "    float angle = atan(toImpact.y, toImpact.x);\n" +
                "    \n" +
                "    // ===== GRIETAS PRINCIPALES (8 rayos) =====\n" +
                "    float numCracks = 8.0;  // Reducido de 12 a 8\n" +
                "    float crackPattern = 0.0;\n" +
                "    \n" +
                "    for (float i = 0.0; i < numCracks; i++) {\n" +
                "        float crackAngle = (i / numCracks) * 6.28318 + hash(i) * 0.3;  // Variación\n" +
                "        float angleDiff = abs(mod(angle - crackAngle + 3.14159, 6.28318) - 3.14159);\n" +
                "        \n" +
                "        // Grieta MÁS FINA con variación caótica\n" +
                "        float crackNoise = noise(vec2(dist * 30.0, i)) * 0.5 + 0.5;\n" +
                "        float crackWidth = 0.004 + crackNoise * 0.003;  // MUY FINA (0.004 vs 0.02)\n" +
                "        float crack = smoothstep(crackWidth, 0.0, angleDiff);\n" +
                "        \n" +
                "        // Ramificaciones caóticas\n" +
                "        float branch = noise(vec2(dist * 15.0 + i, angle * 8.0));\n" +
                "        crack *= (0.7 + branch * 0.3);\n" +
                "        \n" +
                "        // Fade out con la distancia\n" +
                "        float distFade = smoothstep(1.0, 0.0, dist);\n" +
                "        crack *= distFade;\n" +
                "        \n" +
                "        // Expansión animada rápida\n" +
                "        float expansion = smoothstep(dist * 2.0, dist * 2.0 + 0.15, u_Time * 3.0);\n" +
                "        crack *= expansion;\n" +
                "        \n" +
                "        crackPattern = max(crackPattern, crack);\n" +
                "    }\n" +
                "    \n" +
                "    // ===== GRIETAS SECUNDARIAS (3 rayos sutiles) =====\n" +
                "    float secondaryCracks = 0.0;\n" +
                "    for (float i = 0.0; i < 3.0; i++) {  // Reducido de 6 a 3\n" +
                "        float offset = hash(i + 10.0) * 6.28318;\n" +
                "        float crackAngle = (i / 3.0) * 6.28318 + offset;\n" +
                "        float angleDiff = abs(mod(angle - crackAngle + 3.14159, 6.28318) - 3.14159);\n" +
                "        \n" +
                "        float crack = smoothstep(0.003, 0.0, angleDiff);  // Super finas\n" +
                "        float distFade = smoothstep(0.6, 0.0, dist);  // Más cortas\n" +
                "        crack *= distFade;\n" +
                "        \n" +
                "        float expansion = smoothstep(dist * 2.0, dist * 2.0 + 0.15, u_Time * 3.0);\n" +
                "        crack *= expansion * 0.4;  // Mucho más sutiles\n" +
                "        \n" +
                "        secondaryCracks = max(secondaryCracks, crack);\n" +
                "    }\n" +
                "    \n" +
                "    crackPattern = max(crackPattern, secondaryCracks);\n" +
                "    \n" +
                "    // ===== DESTELLO EN PUNTO DE IMPACTO =====\n" +
                "    float impactGlow = 0.0;\n" +
                "    if (dist < 0.15) {\n" +
                "        impactGlow = (1.0 - dist / 0.15) * smoothstep(0.3, 0.0, u_Time);\n" +
                "        impactGlow = pow(impactGlow, 2.0);\n" +
                "    }\n" +
                "    \n" +
                "    // ===== COLOR ENERGÉTICO (azul eléctrico/cyan) =====\n" +
                "    vec3 crackColor = mix(\n" +
                "        vec3(0.3, 0.8, 1.0),  // Cyan eléctrico\n" +
                "        vec3(0.9, 0.95, 1.0), // Blanco\n" +
                "        crackPattern * 0.6    // Interpolación\n" +
                "    );\n" +
                "    \n" +
                "    // Agregar destello naranja en el centro\n" +
                "    crackColor = mix(crackColor, vec3(1.0, 0.7, 0.3), impactGlow * 0.8);\n" +
                "    \n" +
                "    float finalAlpha = (crackPattern + impactGlow) * u_Alpha * 0.7;  // Reducido\n" +
                "    gl_FragColor = vec4(crackColor, finalAlpha);\n" +
                "}\n";

            int vShader = ShaderUtils.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
            int fShader = ShaderUtils.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

            crackShaderProgramId = GLES20.glCreateProgram();
            GLES20.glAttachShader(crackShaderProgramId, vShader);
            GLES20.glAttachShader(crackShaderProgramId, fShader);
            GLES20.glLinkProgram(crackShaderProgramId);

            // Verificar link
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(crackShaderProgramId, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] == 0) {
                Log.e(TAG, "💥 Crack shader link failed: " + GLES20.glGetProgramInfoLog(crackShaderProgramId));
                crackShaderProgramId = 0;
                return;
            }

            GLES20.glDeleteShader(vShader);
            GLES20.glDeleteShader(fShader);

            crackAPositionLoc = GLES20.glGetAttribLocation(crackShaderProgramId, "a_Position");
            crackATexCoordLoc = GLES20.glGetAttribLocation(crackShaderProgramId, "a_TexCoord");
            crackUTimeLoc = GLES20.glGetUniformLocation(crackShaderProgramId, "u_Time");
            crackUImpactPosLoc = GLES20.glGetUniformLocation(crackShaderProgramId, "u_ImpactPos");
            crackUAlphaLoc = GLES20.glGetUniformLocation(crackShaderProgramId, "u_Alpha");

            Log.d(TAG, "💥 Crack shader creado - ID: " + crackShaderProgramId);
        }

        // ╔═════════════════════════════════════════════════════════╗
        // ║  DIBUJAR GRIETAS PROCEDURALES                           ║
        // ╚═════════════════════════════════════════════════════════╝
        if (crackShaderProgramId > 0 && GLES20.glIsProgram(crackShaderProgramId)) {
            GLES20.glUseProgram(crackShaderProgramId);

            // Vértices en NDC
            float[] vertices = {
                -1.0f, -1.0f,  // Bottom-left
                 1.0f, -1.0f,  // Bottom-right
                -1.0f,  1.0f,  // Top-left
                 1.0f,  1.0f   // Top-right
            };

            // UV coordinates
            float[] uvs = {
                0.0f, 0.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f
            };

            // Crear buffers
            java.nio.ByteBuffer vbb = java.nio.ByteBuffer.allocateDirect(vertices.length * 4);
            vbb.order(java.nio.ByteOrder.nativeOrder());
            java.nio.FloatBuffer vb = vbb.asFloatBuffer();
            vb.put(vertices);
            vb.position(0);

            java.nio.ByteBuffer ubb = java.nio.ByteBuffer.allocateDirect(uvs.length * 4);
            ubb.order(java.nio.ByteOrder.nativeOrder());
            java.nio.FloatBuffer ub = ubb.asFloatBuffer();
            ub.put(uvs);
            ub.position(0);

            // Configurar uniforms
            GLES20.glUniform1f(crackUTimeLoc, crackTimer);
            GLES20.glUniform2f(crackUImpactPosLoc, crackX, crackY);
            GLES20.glUniform1f(crackUAlphaLoc, crackAlpha);

            // Configurar atributos
            GLES20.glEnableVertexAttribArray(crackAPositionLoc);
            GLES20.glVertexAttribPointer(crackAPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vb);

            GLES20.glEnableVertexAttribArray(crackATexCoordLoc);
            GLES20.glVertexAttribPointer(crackATexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, ub);

            // Dibujar
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

            // Limpiar
            GLES20.glDisableVertexAttribArray(crackAPositionLoc);
            GLES20.glDisableVertexAttribArray(crackATexCoordLoc);
        }

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    // ===== 🏆 SISTEMA DE LEADERBOARD =====

    /**
     * Actualiza el leaderboard UI (cada 30 segundos)
     */
    private void updateLeaderboardUI() {
        long now = System.currentTimeMillis();
        if (now - lastLeaderboardUpdate < LEADERBOARD_UPDATE_INTERVAL) {
            return; // No actualizar muy seguido
        }

        lastLeaderboardUpdate = now;

        if (leaderboardManager != null) {
            leaderboardManager.getTop3(new LeaderboardManager.Top3Callback() {
                @Override
                public void onSuccess(java.util.List<LeaderboardManager.LeaderboardEntry> top3) {
                    Log.d(TAG, "🏆 Leaderboard actualizado - " + top3.size() + " entradas");

                    // Actualizar textos en el GL thread (next frame)
                    for (int i = 0; i < Math.min(top3.size(), 3); i++) {
                        LeaderboardManager.LeaderboardEntry entry = top3.get(i);
                        if (leaderboardTexts[i] != null) {
                            String icon = entry.isBot ? "🤖" : "👤";
                            String text = icon + " #" + entry.rank + " " + entry.displayName + "\n☀️" + entry.sunsDestroyed;
                            leaderboardTexts[i].setText(text);

                            // Color diferente para el usuario actual
                            if (!entry.isBot && playerStats != null &&
                                entry.sunsDestroyed == playerStats.getSunsDestroyed()) {
                                leaderboardTexts[i].setColor(android.graphics.Color.rgb(255, 215, 0)); // Oro
                            } else if (entry.isBot) {
                                leaderboardTexts[i].setColor(android.graphics.Color.rgb(100, 200, 255)); // Azul claro
                            } else {
                                leaderboardTexts[i].setColor(android.graphics.Color.WHITE);
                            }
                        }
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "❌ Error actualizando leaderboard: " + error);
                }
            });
        }

        // También actualizar bots si es necesario
        if (botManager != null) {
            botManager.updateBotsIfNeeded();
        }
    }

    // ===== 💥💥💥 EXPLOSIÓN ÉPICA DEL SOL 💥💥💥 =====

    /**
     * Callback cuando el sol explota - GENERA EXPLOSIÓN MASIVA DE PARTÍCULAS
     * Llamado desde Planeta cuando HP llega a 0
     */
    @Override
    public void onExplosion(float x, float y, float z, float intensity) {
        Log.d(TAG, "╔════════════════════════════════════════════════════════╗");
        Log.d(TAG, "║                                                        ║");
        Log.d(TAG, "║         💥💥💥 ¡¡¡EXPLOSIÓN ÉPICA!!! 💥💥💥           ║");
        Log.d(TAG, "║                                                        ║");
        Log.d(TAG, "║   El sol ha sido destruido!                           ║");
        Log.d(TAG, String.format("║   Intensidad: %.1f (MÁXIMA)                           ║", intensity));
        Log.d(TAG, String.format("║   Posición: (%.2f, %.2f, %.2f)                        ║", x, y, z));
        Log.d(TAG, "║                                                        ║");
        Log.d(TAG, "║   🌟 ACTIVANDO EXPLOSIÓN MASIVA DE PARTÍCULAS 🌟     ║");
        Log.d(TAG, "║                                                        ║");
        Log.d(TAG, "╚════════════════════════════════════════════════════════╝");

        // ☀️ REGISTRAR SOL DESTRUIDO EN ESTADÍSTICAS (debe hacerse ANTES de actualizar contador)
        if (playerStats != null) {
            playerStats.onSunDestroyed();
            Log.d(TAG, "   ☀️ Sol destruido registrado en PlayerStats");
        }

        // 📊 ACTUALIZAR CONTADOR DE SOLES DESTRUIDOS (ahora con el valor incrementado)
        if (sunsDestroyedCounter != null && playerStats != null) {
            int totalSuns = playerStats.getSunsDestroyed();
            sunsDestroyedCounter.setText("☀️" + totalSuns);
            Log.d(TAG, "   📊 Contador actualizado: " + totalSuns + " soles destruidos");
        }

        // 🏆 FORZAR ACTUALIZACIÓN DEL LEADERBOARD
        if (leaderboardManager != null) {
            leaderboardManager.forceRefresh();
            lastLeaderboardUpdate = 0; // Forzar actualización en próximo frame
        }

        // Disparar explosiones MASIVAS en TODAS las estrellas bailarinas
        if (estrellasBailarinas != null && !estrellasBailarinas.isEmpty()) {
            for (EstrellaBailarina estrella : estrellasBailarinas) {
                if (estrella != null) {
                    // Explosión con intensidad MÁXIMA (2.5x la normal)
                    estrella.triggerExplosion(intensity * 2.5f);
                    Log.d(TAG, "   💥 Estrella bailarina activada con intensidad " + (intensity * 2.5f));
                }
            }
            Log.d(TAG, "   ✨✨✨ " + estrellasBailarinas.size() + " EXPLOSIONES MASIVAS DISPARADAS! ✨✨✨");
        } else {
            Log.w(TAG, "   ⚠️ No hay estrellas bailarinas disponibles para explosión");
        }
    }
}