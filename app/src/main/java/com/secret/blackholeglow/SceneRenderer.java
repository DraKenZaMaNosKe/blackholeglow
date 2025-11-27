// SceneRenderer.java - VERSIÓN MEJORADA CON LOGGING
package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

// 🚀 OpenGL ES 3.0 - Instanced Rendering
import com.secret.blackholeglow.gl3.InstancedParticles;

// 🎵 Sistema de compartir canciones
import com.secret.blackholeglow.sharing.HeartParticleSystem;
import com.secret.blackholeglow.sharing.LikeButton;
import com.secret.blackholeglow.sharing.MusicNotificationListener;
import com.secret.blackholeglow.sharing.SharedSong;
import com.secret.blackholeglow.sharing.SongNotification;
import com.secret.blackholeglow.sharing.SongSharingManager;
import com.secret.blackholeglow.sharing.UserAvatar;

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
    private Planeta planetaTierra;  // 🌍 Referencia a la Tierra para detectar impactos
    private ForceField forceField;
    private EarthShield earthShield;  // 🌍🛡️ Escudo invisible de la Tierra para mostrar impactos
    private HPBar hpBarSun;
    private HPBar hpBarForceField;
    private MeteorShower meteorShower;
    private Spaceship3D ovni;  // 🛸 OVNI con IA + armas láser
    private InstancedParticles instancedParticles;  // ✨ Sistema de partículas GL3.0 (instanced rendering)
    private PlayerWeapon playerWeapon;  // 🎮 NUEVO: Arma del jugador (separada de MeteorShower)
    private FireButton fireButton;      // 🎯 Botón visual de disparo con indicador de estado
    private boolean solWasDead = false;  // Para detectar cuando respawnea
    // 🚀 Referencia a la escena de batalla espacial (para touch interactivo)
    private SpaceBattleScene spaceBattleScene;

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

    // 📊 CONTADOR DE PLANETAS DESTRUIDOS (UI)
    private SimpleTextRenderer planetsDestroyedCounter;

    // 🏆 SISTEMA DE LEADERBOARD Y BOTS
    private BotManager botManager;
    private LeaderboardManager leaderboardManager;
    private SimpleTextRenderer[] leaderboardTexts = new SimpleTextRenderer[3];  // Textos para Top 3
    private long lastLeaderboardUpdate = 0;
    private static final long LEADERBOARD_UPDATE_INTERVAL = 30000; // 30 segundos

    // 🎵 SISTEMA DE COMPARTIR CANCIONES
    private LikeButton likeButton;
    private HeartParticleSystem heartParticles;
    private UserAvatar userAvatar;
    private SongNotification songNotification;
    private SongSharingManager songSharingManager;
    private SimpleTextRenderer songNotificationUserText;   // Nombre del usuario
    private SimpleTextRenderer songNotificationSongText;   // Título de la canción

    // ▶️ SISTEMA DE PLAY/PAUSE - Control de animación por usuario
    // Por defecto PAUSED (false) para arranque rápido - usuario activa cuando quiere
    private PlayPauseButton playPauseButton;
    private OrbixGreeting orbixGreeting;  // 🤖 Saludos inteligentes + Reloj preciso
    private boolean isAnimationPlaying = false;  // false = congelado (arranque rápido), true = animando
    private boolean frozenFrameReady = false;    // true = ya renderizamos el frame estático
    private int frozenFrameCount = 0;            // Contador para renderizar unos frames antes de congelar

    // 🚀 OPTIMIZACIÓN: Arrays reutilizables (evita allocations en runtime)
    private final float[] identityMatrixCache = new float[16];  // Para UI 2D
    private final float[] hsvCache = new float[3];              // Para colores HSV

    public SceneRenderer(Context ctx, String initialItem) {
        this.context = ctx;
        this.selectedItem = initialItem;

        // 🎮 Inicializar sistema de estadísticas
        this.playerStats = PlayerStats.getInstance(ctx);
        playerStats.printStats();  // Mostrar estadísticas al iniciar

        // 🔄 Escuchar sincronización con Firebase para actualizar contador
        playerStats.setSyncListener(new PlayerStats.SyncListener() {
            @Override
            public void onSyncCompleted(int planetsDestroyed) {
                // Actualizar contador en pantalla cuando se sincronice con Firebase
                if (planetsDestroyedCounter != null) {
                    planetsDestroyedCounter.setText("🪐" + planetsDestroyed);
                    Log.d(TAG, "✅ Contador actualizado después de sincronización: " + planetsDestroyed + " planetas");
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

        // ▶️ INICIALIZAR BOTÓN PLAY/PAUSE
        playPauseButton = new PlayPauseButton();
        playPauseButton.setPlaying(isAnimationPlaying);
        Log.d(TAG, "▶️ PlayPauseButton inicializado");

        // 🤖 INICIALIZAR SALUDOS INTELIGENTES + RELOJ
        // Pasamos context para acceder a UserManager (nombre de usuario + fecha de nacimiento)
        orbixGreeting = new OrbixGreeting(context);
        orbixGreeting.show();  // Visible por defecto (ya que empieza en STOP)
        Log.d(TAG, "🤖 OrbixGreeting inicializado con contexto");

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

        // ▶️ Actualizar aspect ratio del botón Play/Pause
        if (playPauseButton != null) {
            playPauseButton.setAspectRatio(aspectRatio);
        }

        // 🤖 Actualizar aspect ratio del saludo
        if (orbixGreeting != null) {
            orbixGreeting.setAspectRatio(aspectRatio);
        }

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
            frozenFrameReady = false;  // Forzar re-render del frame estático
        }

        // ═══════════════════════════════════════════════════════════════
        // ▶️ SISTEMA PLAY/PAUSE - MODO FROZEN FRAME
        // Si la animación está pausada Y ya tenemos un frame renderizado,
        // solo dibujamos el PlayPauseButton (sin re-renderizar la escena)
        // ═══════════════════════════════════════════════════════════════
        if (!isAnimationPlaying && frozenFrameReady) {
            // MODO CONGELADO: Solo actualizar/dibujar el botón Play + Saludo + Reloj
            long now = System.nanoTime();
            float miniDt = Math.min((now - lastTime) / 1e9f, 0.1f);
            lastTime = now;

            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

            // 🤖 Actualizar y dibujar saludo + reloj (PRIMERO - atrás)
            if (orbixGreeting != null) {
                orbixGreeting.update(miniDt);
                orbixGreeting.draw();
            }

            // ▶️ Actualizar y dibujar botón Play/Pause (ÚLTIMO - al frente)
            if (playPauseButton != null) {
                playPauseButton.update(miniDt);
                playPauseButton.draw();
            }

            return;  // ¡No renderizar nada más! Ahorra CPU/GPU
        }

        // ⚡ OPTIMIZACIÓN: Usar TimeManager centralizado
        // Una sola llamada a System.nanoTime() para todo el frame
        TimeManager.update();
        float dt = TimeManager.getDeltaTime();
        lastTime = System.nanoTime(); // Mantener para compatibilidad con modo frozen

        // Si estamos en proceso de congelar, renderizar unos frames antes de parar
        if (!isAnimationPlaying && !frozenFrameReady) {
            frozenFrameCount++;
            if (frozenFrameCount >= 3) {  // 3 frames para estabilizar
                frozenFrameReady = true;
                Log.d(TAG, "▶️ Frame congelado - modo estático activado");
            }
        }

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

        // ═══════════════════════════════════════════════════════════
        // 🎨 RENDERIZADO EN CAPAS - FireButton siempre encima
        // ═══════════════════════════════════════════════════════════

        // Actualizar TODOS los objetos primero (incluye EarthShield)
        for (SceneObject obj : sceneObjects) {
            obj.update(dt);
        }

        // ✨ Actualizar partículas instanciadas (OpenGL ES 3.0)
        if (instancedParticles != null) {
            instancedParticles.update(dt);
            // Pasar matriz VP para renderizado 3D
            if (sharedCamera != null) {
                instancedParticles.setVPMatrix(sharedCamera.getViewProjectionMatrix());
            }
        }

        // Dibujar objetos del JUEGO (excepto FireButton) - incluye EarthShield
        for (SceneObject obj : sceneObjects) {
            if (!(obj instanceof FireButton)) {
                obj.draw();
            }
        }

        // ✨ Dibujar partículas instanciadas (después de objetos 3D, antes de UI)
        if (instancedParticles != null) {
            instancedParticles.draw();
        }

        // 💥 DIBUJAR FLASH BLANCO SI ESTÁ ACTIVO (puede cubrir el juego)
        if (impactFlashAlpha > 0.01f) {
            drawImpactFlash();
        }

        // 💥 DIBUJAR GRIETAS DE PANTALLA ROTA SI ESTÁN ACTIVAS
        if (crackAlpha > 0.01f) {
            drawScreenCracks();
        }

        // 🎵 DIBUJAR SISTEMA DE COMPARTIR CANCIONES
        drawSongSharingUI();

        // 🎯 DIBUJAR FIREBUTTON AL FINAL - SIEMPRE VISIBLE ENCIMA DE TODO
        if (fireButton != null) {
            fireButton.draw();
        }

        // ▶️ DIBUJAR PLAYPAUSEBUTTON - SIEMPRE ENCIMA DE TODO (última capa)
        if (playPauseButton != null) {
            playPauseButton.update(dt);
            playPauseButton.draw();
        }
    }

    /**
     * 🎵 Dibuja el botón de like y las notificaciones de canciones
     */
    private void drawSongSharingUI() {
        // 🚀 OPTIMIZACIÓN: Reutilizar matriz de identidad (evita allocation cada frame)
        android.opengl.Matrix.setIdentityM(identityMatrixCache, 0);

        // ⚡ OPTIMIZACIÓN: Usar TimeManager en lugar de System.currentTimeMillis()
        float time = TimeManager.getTime();

        // Dibujar botón de Like
        if (likeButton != null) {
            likeButton.setCooldown(!songSharingManager.canShare());
            likeButton.draw(identityMatrixCache, time);
        }

        // 💖 Actualizar y dibujar partículas de corazones
        if (heartParticles != null) {
            // Usar deltaTime aproximado (~60 FPS = 0.016s)
            float particleDeltaTime = 0.016f;
            heartParticles.update(particleDeltaTime);
            heartParticles.draw(identityMatrixCache);
        }

        // Dibujar notificación de canción
        if (songNotification != null) {
            // LOG para debug
            if (frameCount % 300 == 0) {
                Log.d(TAG, "🎵 SongNotification: isVisible=" + songNotification.isVisible() +
                      ", user=" + songNotification.getUserNameText() +
                      ", song=" + songNotification.getSongTitleText());
            }

            if (songNotification.isVisible()) {
                songNotification.draw(identityMatrixCache);
            }

            // 🎵✨ Mostrar nombre y canción en DOS LÍNEAS con efectos de color
            if (songNotificationUserText != null && songNotificationSongText != null) {
                String userName = songNotification.getUserNameText();
                String songTitle = songNotification.getSongTitleText();

                // Mostrar si hay datos
                if (userName != null && !userName.isEmpty() && songTitle != null && !songTitle.isEmpty()) {
                    // 👤 Dibujar avatar del usuario primero
                    if (userAvatar != null) {
                        userAvatar.draw(identityMatrixCache);
                    }

                    // ✨ EFECTO DE COLOR ARCOÍRIS SUAVE (rosa → cyan → rosa)
                    float colorTime = time * 0.3f;  // Velocidad del cambio de color
                    float hue = 0.85f + (float)Math.sin(colorTime) * 0.15f;  // Oscila entre rosa y cyan
                    if (hue > 1.0f) hue -= 1.0f;

                    // 🚀 OPTIMIZACIÓN: Reutilizar array HSV (evita allocation cada frame)
                    hsvCache[0] = hue * 360f;
                    hsvCache[1] = 0.5f;
                    hsvCache[2] = 1.0f;
                    int animatedColor = android.graphics.Color.HSVToColor(255, hsvCache);

                    // 📝 LÍNEA 1: Nombre del usuario (más pequeño, arriba)
                    songNotificationUserText.setColor(animatedColor);
                    songNotificationUserText.setText(userName + ":");
                    songNotificationUserText.draw();

                    // 🎵 LÍNEA 2: Título de la canción (más grande, abajo)
                    // Color ligeramente diferente para variedad visual
                    float hue2 = 0.55f + (float)Math.sin(colorTime + 1.5f) * 0.15f;  // Cyan → verde
                    // 🚀 OPTIMIZACIÓN: Reutilizar array HSV (evita allocation cada frame)
                    hsvCache[0] = hue2 * 360f;
                    hsvCache[1] = 0.6f;
                    hsvCache[2] = 1.0f;
                    int songColor = android.graphics.Color.HSVToColor(255, hsvCache);

                    songNotificationSongText.setColor(songColor);
                    songNotificationSongText.setText(songTitle);
                    songNotificationSongText.draw();
                }
            }
        }
    }

    private void prepareScene() {
        Log.d(TAG, "════════ Preparing Scene: " + selectedItem + " ════════");

        if (!textureManager.initialize()) {
            Log.e(TAG, "✗ ERROR: TextureManager could not initialize");
            return;
        }

        sceneObjects.clear();
        spaceBattleScene = null;  // Limpiar referencia de batalla espacial

        // ═══════════════════════════════════════════════════════════
        // 🎨 SELECTOR DE ESCENAS - 10 WALLPAPERS ÚNICOS
        // ═══════════════════════════════════════════════════════════
        switch (selectedItem) {
            case "🌊 Océano Profundo":
                setupOceanScene();
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
            case "🌍 Tierra Live HD":
                setupTierraLiveHDScene();
                break;
            case "Furia Celestial":
                setupTormentaScene();
                break;
            case "🚀 Batalla Galáctica":
                setupSpaceBattleScene();
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
                    R.drawable.universo001
            );
            sceneObjects.add(starryBg);
            Log.d(TAG, "[SceneRenderer] ✓ Fondo con textura + estrellas agregado");
        } catch (Exception e) {
            Log.e(TAG, "[SceneRenderer] ✗ Error creando fondo: " + e.getMessage());
        }

        // 🌍 PLANETA TIERRA EN EL CENTRO - MODO HÍBRIDO: TEXTURA + SHADERS PROCEDURALES
        // ✨ Textura realista HD como base + Nubes animadas + Atmósfera procedural + Océanos con olas
        // Nota: La variable se llama "sol" por razones históricas (sistema de HP/respawn)
        try {
            sol = new Planeta(
                    context, textureManager,
                    "shaders/tierra_vertex.glsl",        // Shader épico con efectos
                    "shaders/tierra_fragment.glsl",      // 🌍 HÍBRIDO: Textura real + efectos procedurales
                    R.drawable.texturaplanetatierra,     // ✨ TEXTURA HD REALISTA como base
                    0.8f, 0.0f,        // Posición orbital X, Z
                    0.0f,              // orbitSpeed = 0 (FIJO, sin órbita)
                    0.0f,              // 📍 orbitOffsetY = 0.0 (sin altura)
                    0.0f,              // scaleAmplitude = sin variación
                    1.0f,              // 🌎 TAMAÑO PROTAGONISTA (planeta principal)
                    12.0f,             // spinSpeed = rotación SUAVE y relajante
                    false, null, 1.0f,
                    null, 1.0f
            );
            if (sol instanceof CameraAware) {
                ((CameraAware) sol).setCameraController(sharedCamera);
            }
            sol.setMaxHealth(200);  // Tierra tiene 200 HP - objetivo principal a defender
            sol.setOnExplosionListener(this);  // 💥 CONECTAR EXPLOSIÓN ÉPICA

            // ═══ 💾 CARGAR HP GUARDADO ═══
            sol.setPlayerStats(playerStats);  // Inyectar PlayerStats para auto-guardar
            int savedPlanetHP = playerStats.getSavedPlanetHealth();  // Nota: usa "PlanetHealth" (campo Firebase: "sunHealth" por compatibilidad)
            sol.setHealth(savedPlanetHP);  // Cargar HP guardado
            Log.d(TAG, "  💾 TIERRA HP cargado: " + savedPlanetHP + "/200");

            // ═══ ⚡ OPTIMIZACIÓN: ROTACIÓN ANIMADA SIMPLE (sin Calendar) ═══
            // Desactivado tiempo real para mejor rendimiento en dispositivos de gama baja
            sol.setRealTimeRotation(false);  // ⚡ DESACTIVADO - usa rotación animada simple
            // spinSpeed ya está configurado en 80.0f para rotación visible
            Log.d(TAG, "  ⚡ TIERRA rotación SIMPLE (spinSpeed=80, sin Calendar)");

            sceneObjects.add(sol);

            // 🌍 Guardar referencia para detección de impactos
            planetaTierra = sol;

            Log.d(TAG, "════════════════════════════════════════════════");
            Log.d(TAG, "  ✓ 🌍 TIERRA ÉPICA añadida con shader procedural");
            Log.d(TAG, "  ✨ Océanos animados + Continentes + Nubes + Atmósfera");
            Log.d(TAG, "  💫 Luces de ciudades nocturnas + Reflexión solar");
            Log.d(TAG, "  💾 HP: " + savedPlanetHP + "/200");
            Log.d(TAG, "  💥 Explosion listener: ACTIVE");
            Log.d(TAG, "════════════════════════════════════════════════");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creating Earth: " + e.getMessage());
        }

        // 🌍🛡️ CREAR ESCUDO INVISIBLE DE LA TIERRA (para mostrar impactos)
        // Radio: 0.58 (Tierra = 0.5, shield MÁS SEPARADO para evitar Z-fighting)
        earthShield = new EarthShield(
            context, textureManager,
            0.0f, 0.0f, 0.0f,  // Centrado con la Tierra
            1.05f               // Radio mayor que la Tierra para evitar solapamiento
        );
        if (earthShield instanceof CameraAware) {
            ((CameraAware) earthShield).setCameraController(sharedCamera);
        }
        sceneObjects.add(earthShield);
        Log.d(TAG, "  🌍🛡️ Escudo invisible de la Tierra creado (solo muestra impactos)");

        // ═══════════════════════════════════════════════════════════
        // 🚫 CAPA tierraEffects REMOVIDA (causaba Z-buffer issues)
        // ═══════════════════════════════════════════════════════════
        // El nuevo shader tierra_fragment.glsl incluye TODOS los efectos
        // atmosféricos en un solo render pass (más eficiente + sin bugs)
        // ═══════════════════════════════════════════════════════════

        // ✨ 3 ESTRELLAS BAILARINAS - PARTÍCULAS MÁGICAS CON ESTELA ✨
        try {
            estrellasBailarinas.clear();

            // Estrella 1 - Posición superior derecha
            EstrellaBailarina estrella1 = new EstrellaBailarina(
                    context, textureManager,
                    1.8f, 0.8f, 0.5f,   // Posición inicial: arriba-derecha
                    0.02f,              // Escala: MINÚSCULA
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

            Log.d(TAG, "  ✨ 3 Estrellas bailarinas añadidas");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creando estrellas bailarinas: " + e.getMessage());
        }

        // ☀️ SOL PROCEDURAL - VERSIÓN OPTIMIZADA (576 tri vs 7,936)
        SolProcedural solProcedural = null;
        try {
            solProcedural = new SolProcedural(context, textureManager);
            solProcedural.setPosition(-8.0f, 4.0f, -15.0f);   // ☀️ Más cerca y visible
            solProcedural.setScale(1.5f);                     // ☀️ Sol GRANDE y visible
            solProcedural.setCameraController(sharedCamera);

            sceneObjects.add(solProcedural);
            Log.d(TAG, "  ✓ ☀️ SOL PROCEDURAL añadido (576 tri - 14x más eficiente)");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creating procedural sun: " + e.getMessage());
            e.printStackTrace();
        }

        // ═══════════════════════════════════════════════════════════
        // 🚫 CAPA solEffects REMOVIDA (potencial Z-buffer issue)
        // ═══════════════════════════════════════════════════════════
        // El nuevo shader sol_plasma_fragment.glsl incluye TODOS los
        // efectos en un solo render pass (plasma + manchas + corona)
        // ═══════════════════════════════════════════════════════════

        // 🚫 VIENTOS SOLARES - REMOVIDOS (simplificación visual)
        // Eliminado por feedback: complicaba visualmente la escena sin aportar valor

        // 🔴 PLANETA MARTE - REMOVIDO (simplificar escena)
        // Código comentado por solicitud del usuario para simplificar la escena del universo

        // 🌙 LUNA - DESACTIVADA PARA OPTIMIZACIÓN
        // ⚡ OPTIMIZACIÓN: Luna desactivada para mejor rendimiento en dispositivos de gama baja
        Log.d(TAG, "  ⚡ Luna DESACTIVADA (optimización)");

        // ☄️☄️ CINTURÓN DE ASTEROIDES - REMOVIDO (simplificar escena)
        // Código comentado por solicitud del usuario para simplificar la escena del universo
        Log.d(TAG, "  ☄️ Cinturón de asteroides desactivado por simplificación");

        // 🪨 ASTEROIDE REALISTA ESTÁTICO - AHORA MANEJADO POR METEORSHOWER
        // El AsteroideRealista ahora es usado por MeteorShower como sistema dinámico
        // (reemplazó a Meteorito por preferencia visual del usuario)
        Log.d(TAG, "  🪨 Asteroide estático removido - ahora manejado por MeteorShower");

        // 🛸 NAVE ESPACIAL / OVNI - EXPLORACIÓN LIBRE CON IA
        try {
            ovni = new Spaceship3D(
                    context,
                    textureManager,
                    1.8f, 1.5f, -1.0f,    // Posición inicial: arriba-derecha, visible
                    0.07f                  // Escala visible
            );
            ovni.setCameraController(sharedCamera);

            // 🌍 Configurar posición de la Tierra para ESQUIVARLA
            ovni.setEarthPosition(0f, 0f, 0f);
            ovni.setOrbitParams(
                1.5f,   // Distancia segura al planeta (no acercarse más)
                0.35f,  // Velocidad de exploración
                0.0f    // (no usado en modo exploración)
            );

            // 🛡️ Conectar con EarthShield para mostrar impactos de láser
            if (earthShield != null) {
                ovni.setEarthShield(earthShield);
            }

            sceneObjects.add(ovni);

            Log.d(TAG, "════════════════════════════════════════════════");
            Log.d(TAG, "  ✓ 🛸 OVNI EXPLORADOR con IA + ARMAS LÁSER");
            Log.d(TAG, "  🌍 Esquiva automáticamente la Tierra");
            Log.d(TAG, "  🔫 Disparo automático cada 3-7 segundos");
            Log.d(TAG, "  💔 HP: 3 (destruido por meteoritos)");
            Log.d(TAG, "════════════════════════════════════════════════");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creating spaceship: " + e.getMessage());
            e.printStackTrace();
        }

        // ✨ SISTEMA DE PARTÍCULAS INSTANCIADAS - OpenGL ES 3.0
        // Renderiza miles de partículas con UNA sola draw call
        try {
            instancedParticles = new InstancedParticles(context, 500);  // Max 500 partículas
            instancedParticles.setEmitterPosition(0f, -0.5f, 0f);  // Debajo de la Tierra
            instancedParticles.setEmissionRate(15f);  // 15 partículas/segundo
            instancedParticles.setBaseColor(1f, 0.6f, 0.2f, 0.8f);  // Naranja brillante

            // No lo agregamos a sceneObjects porque tiene su propio sistema de dibujado
            Log.d(TAG, "════════════════════════════════════════════════");
            Log.d(TAG, "  ✓ ✨ INSTANCED PARTICLES (OpenGL ES 3.0)");
            Log.d(TAG, "  🚀 Max 500 partículas con 1 draw call");
            Log.d(TAG, "  ⚡ ~10x más eficiente que ES 2.0");
            Log.d(TAG, "════════════════════════════════════════════════");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creating instanced particles: " + e.getMessage());
            e.printStackTrace();
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

        // 🛡️ CAMPO DE FUERZA - ESCUDO DE LA TIERRA
        try {
            forceField = new ForceField(
                    context, textureManager,
                    0.0f, 0.0f, 0.0f,   // 🎯 CENTRADO CON LA TIERRA en (0, 0, 0)
                    1.230f,              // 🛡️ MUCHO MÁS GRANDE (envuelve atmósfera sin tocarla)
                    R.drawable.fondo_transparente,  // Textura transparente para efectos puros
                    new float[]{0.3f, 0.9f, 1.0f},  // Color azul eléctrico suave
                    0.0125f,               // ✨ CASI INVISIBLE (alpha 0%, solo impactos)
                    0.028f,              // Pulsación ULTRA sutil (3% de variación)
                    0.240f                // Pulsación ULTRA LENTA
            );
            forceField.setCameraController(sharedCamera);

            // ═══ 💾 CARGAR HP GUARDADO ═══
            forceField.setPlayerStats(playerStats);  // Inyectar PlayerStats para auto-guardar
            int savedForceFieldHP = playerStats.getSavedForceFieldHealth();
            forceField.setHealth(savedForceFieldHP);  // Cargar HP guardado
            Log.d(TAG, "  💾 ForceField HP cargado: " + savedForceFieldHP + "/50");

            sceneObjects.add(forceField);
            Log.d(TAG, "  🛡️ Campo de fuerza ACTIVADO");
        } catch (Exception e) {
            Log.e(TAG, "[SceneRenderer] ✗ Error creando campo de fuerza: " + e.getMessage());
        }

        // BARRAS HP para Tierra y Campo de Fuerza
        try {
            // Barra HP de la Tierra (azul-verde cuando llena, roja cuando vacía)
            hpBarSun = new HPBar(
                    context,
                    "🌍 TIERRA",  // Actualizado a TIERRA
                    0.05f, 0.92f,  // Posición: arriba izquierda
                    0.25f, 0.03f,  // Tamaño: ancho y alto
                    200,  // Max HP = 200
                    new float[]{0.2f, 0.8f, 0.3f, 1.0f},  // Verde-azul lleno (colores tierra)
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
                    new float[]{0.3f, 0.3f, 0.3f, 0.5f},  // Gris oscuro vacío
                    false  // ⚠️ Deshabilitar parpadeo (solo se usa para indicar carga, no daño)
            );
            chargePowerBar.setHealth(0);  // Empieza vacía
            sceneObjects.add(chargePowerBar);
            Log.d(TAG, "  ⚡✓ Barra de carga de poder agregada (sin parpadeo)");
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
        // ⚠️ OCULTA: Funcionalidad activa pero sin visualización
        /*
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
        */

        // 🎵 INDICADOR VISUAL DE MÚSICA 🎵
        // Muestra 3 barras (BASS, MID, TREBLE) CENTRADAS, ARRIBA DEL SOL
        try {
            Log.d(TAG, "╔════════════════════════════════════════╗");
            Log.d(TAG, "║   CREANDO INDICADOR DE MÚSICA         ║");
            Log.d(TAG, "╚════════════════════════════════════════╝");

            musicIndicator = new MusicIndicator(
                    context,
                    -0.250f,   // X: Centrado (ligeramente a la izquierda del centro)
                    0.2660f,    // Y: Parte superior de la pantalla
                    0.50f,    // Ancho: HORIZONTAL (más ancho que alto)
                    0.10f     // Alto: Delgado y compacto
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
            Log.d(TAG, "║  CREANDO CONTADOR PLANETAS DESTRUIDOS  ║");
            Log.d(TAG, "╚════════════════════════════════════════╝");

            planetsDestroyedCounter = new SimpleTextRenderer(
                    context,
                    0.50f,    // X: Esquina superior derecha
                    0.60f,    // Y: Más abajo (movido desde 0.75)
                    0.40f,    // Ancho
                    0.10f     // Alto
            );
            planetsDestroyedCounter.setColor(android.graphics.Color.rgb(100, 150, 255));  // Azul planeta

            // Inicializar con el valor actual de PlayerStats (puede ser de Firebase o local)
            if (playerStats != null) {
                int currentPlanets = playerStats.getPlanetsDestroyed();
                planetsDestroyedCounter.setText("🪐" + currentPlanets);
                Log.d(TAG, "  🪐 Contador inicializado con: " + currentPlanets + " planetas");
            } else {
                planetsDestroyedCounter.setText("🪐0");
            }

            sceneObjects.add(planetsDestroyedCounter);
            Log.d(TAG, "  🪐✓ CONTADOR agregado - esquina superior derecha");
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
            // BOTS DESHABILITADOS - No se crearán ni actualizarán bots en Firebase
            // botManager = BotManager.getInstance();
            leaderboardManager = LeaderboardManager.getInstance(context);

            // ⚠️ BOTS DESHABILITADOS PARA RELEASE EN PLAY STORE
            // Los bots fueron utilizados durante desarrollo para simular competencia
            // Ahora solo aparecerán jugadores reales en el leaderboard
            /*
            botManager.initializeBots(new BotManager.InitCallback() {
                @Override
                public void onComplete() {
                    Log.d(TAG, "🤖 Bots inicializados");
                    // Actualizar leaderboard después de inicializar bots
                    updateLeaderboardUI();
                }
            });
            */

            // Actualizar leaderboard directamente (sin esperar bots)
            updateLeaderboardUI();

            // Crear textos para Top 3 (VERTICAL, de arriba a abajo, en la parte inferior izquierda)
            float x = -0.99f;        // X fija en el borde izquierdo (alineado con barras HP)
            float startY = 0.10f;   // Y inicial (parte inferior, justo arriba de las barras HP)
            float width = 0.45f;    // Ancho de cada texto
            float spacing = 0.18f;  // Espaciado VERTICAL entre textos

            for (int i = 0; i < 3; i++) {
                float y = startY + (i * spacing);  // Y varía (vertical), X fija
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

        // 🎵 SISTEMA DE COMPARTIR CANCIONES
        try {
            Log.d(TAG, "╔════════════════════════════════════════╗");
            Log.d(TAG, "║   INICIALIZANDO SONG SHARING           ║");
            Log.d(TAG, "╚════════════════════════════════════════╝");

            // Inicializar componentes
            likeButton = new LikeButton();
            likeButton.init();
            likeButton.setPosition(0.80f, -0.45f);  // Esquina derecha, arriba de barra del sistema
            likeButton.setSize(0.10f);

            // 💖 Sistema de partículas e corazones
            heartParticles = new HeartParticleSystem();
            heartParticles.init();

            // 👤 Avatar del usuario que comparte
            userAvatar = new UserAvatar();
            userAvatar.init();
            userAvatar.setPosition(-0.78f, -0.45f);  // A la izquierda del texto
            userAvatar.setSize(0.09f);

            songNotification = new SongNotification();
            songNotification.init();

            songSharingManager = SongSharingManager.getInstance(context);

            // Inicializar texto para la notificación - DOS LÍNEAS
            // Línea 1: Nombre del usuario (arriba)
            songNotificationUserText = new SimpleTextRenderer(context, -0.60f, -0.38f, 1.0f, 0.050f);
            songNotificationUserText.setColor(0xFFFFFFFF);  // Blanco brillante

            // Línea 2: Título de la canción (abajo, más grande)
            songNotificationSongText = new SimpleTextRenderer(context, -0.60f, -0.455f, 1.0f, 0.055f);
            songNotificationSongText.setColor(0xFFFFFFFF);  // Blanco brillante

            // Escuchar nuevas canciones compartidas
            songSharingManager.startListening(new SongSharingManager.OnNewSongListener() {
                @Override
                public void onNewSong(SharedSong song) {
                    Log.d(TAG, "╔═══════════════════════════════════════════════════╗");
                    Log.d(TAG, "║   🎵🎵🎵 NUEVA CANCIÓN RECIBIDA 🎵🎵🎵           ║");
                    Log.d(TAG, "╚═══════════════════════════════════════════════════╝");
                    Log.d(TAG, "👤 Usuario: " + song.getUserName());
                    Log.d(TAG, "🎶 Canción: " + song.getSongTitle());

                    // Mostrar notificación
                    if (songNotification != null) {
                        songNotification.show(song);
                        Log.d(TAG, "✅ songNotification.show() llamado");
                    } else {
                        Log.e(TAG, "❌ songNotification es NULL!");
                    }

                    // 👤 Actualizar avatar del usuario
                    if (userAvatar != null) {
                        userAvatar.setUser(song.getUserName(), song.getUserPhotoUrl());
                        Log.d(TAG, "👤 Avatar actualizado: " + song.getUserName());
                    }
                }
            });

            Log.d(TAG, "[SceneRenderer] ✓ Sistema de compartir canciones inicializado");
        } catch (Exception e) {
            Log.e(TAG, "[SceneRenderer] ✗ Error inicializando song sharing: " + e.getMessage());
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
            // ⚠️ OCULTA: Barra de countdown desactivada visualmente
            /*
            if (meteorCountdownBar != null) {
                meteorShower.setCountdownBar(meteorCountdownBar);
                Log.d(TAG, "[SceneRenderer] ✓ Barra de countdown conectada con MeteorShower");
            }
            */

            // Registrar el sol, planeta Y campo de fuerza para colisiones
            for (SceneObject obj : sceneObjects) {
                if (obj instanceof Planeta || obj instanceof ForceField) {
                    meteorShower.registrarObjetoColisionable(obj);
                }
            }

            // 🛸 Conectar OVNI con MeteorShower para colisiones
            if (ovni != null) {
                meteorShower.setOvni(ovni);
                Log.d(TAG, "[SceneRenderer] 🛸 OVNI conectado con MeteorShower para colisiones");
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

        // 🎯 BOTÓN VISUAL DE DISPARO - INDICADOR DE ESTADO
            Log.d(TAG, "[SceneRenderer] >>> ANTES de crear FireButton");
        try {
            Log.d(TAG, "[SceneRenderer] >>> Creando FireButton...");
            fireButton = new FireButton(context);
            Log.d(TAG, "[SceneRenderer] >>> FireButton creado, asignando cámara...");
            fireButton.setCameraController(sharedCamera);
            Log.d(TAG, "[SceneRenderer] >>> Agregando a sceneObjects...");
            sceneObjects.add(fireButton);
            Log.d(TAG, "[SceneRenderer] 🎯 Botón de disparo agregado (verde=listo, amarillo=cooldown)");
        } catch (Exception e) {
            Log.e(TAG, "[SceneRenderer] ✗✗✗ ERROR FIREBUTTON ✗✗✗");
            Log.e(TAG, "[SceneRenderer] Mensaje: " + e.getMessage());
            Log.e(TAG, "[SceneRenderer] Clase: " + e.getClass().getName());
            e.printStackTrace();
        }
        Log.d(TAG, "[SceneRenderer] >>> DESPUÉS de intentar crear FireButton");

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
                    0.0f, 0.0f,        // orbitRadiusX, orbitRadiusZ
                    0.0f,              // orbitSpeed
                    0.0f,              // 📍 orbitOffsetY
                    0.05f,             // scaleAmplitude
                    2.0f,              // instanceScale
                    0.0f,              // spinSpeed
                    true,              // useSolidColor
                    new float[]{0.0f, 0.0f, 0.0f, 1.0f},  // solidColor (negro)
                    1.0f,              // alpha
                    0.98f,             // scaleOscPercent
                    1.0f               // uvScale
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
                        radius, radius * 0.8f,  // orbitRadiusX, orbitRadiusZ
                        0.5f / (i + 1),         // orbitSpeed
                        0.0f,                   // 📍 orbitOffsetY
                        0.1f,                   // scaleAmplitude
                        0.3f,                   // instanceScale
                        50.0f,                  // spinSpeed
                        true,                   // useSolidColor
                        new float[]{1.0f, 0.5f, 0.2f, 0.7f},  // solidColor
                        0.8f,                   // alpha
                        null,                   // scaleOscPercent
                        1.0f                    // uvScale
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
    // ═══════════════════════════════════════════════════════════════
    // 🌊 OCÉANO PROFUNDO - Mundo submarino mágico
    // ═══════════════════════════════════════════════════════════════
    private void setupOceanScene() {
        Log.d(TAG, "Setting up OCEAN scene...");

        // ============================================
        // FONDO OCÉANO - Textura de agua azul profunda
        // ============================================
        // TODO: Reemplazar universo03 con ocean_background.png cuando esté disponible
        try {
            StarryBackground oceanBg = new StarryBackground(
                    context,
                    textureManager,
                    R.drawable.universo03  // PLACEHOLDER - usar textura de océano real
            );
            sceneObjects.add(oceanBg);
            Log.d(TAG, "  ✓ Ocean background added (TEMPORARY - using universo03)");
            Log.d(TAG, "  ⚠️ TODO: Add ocean_background.png to drawable folder");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creating ocean background: " + e.getMessage());
        }

        // ============================================
        // TODO FASE B: AGREGAR OBJETOS MARINOS
        // ============================================
        // - Peces nadando
        // - Corales y plantas
        // - Burbujas flotando
        // - Rayos de luz (god rays)

        Log.d(TAG, "✓ Ocean scene setup complete (FASE A - fondo básico)");
        Log.d(TAG, "  📝 Scene ready to add marine objects");
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
                    0.0f, 0.0f,         // orbitRadiusX, orbitRadiusZ
                    0.0f,               // orbitSpeed
                    0.0f,               // 📍 orbitOffsetY
                    0.1f,               // scaleAmplitude
                    1.5f,               // instanceScale
                    5.0f,               // spinSpeed
                    true,               // useSolidColor
                    new float[]{0.2f, 0.6f, 0.2f, 1.0f},  // solidColor (verde)
                    1.0f,               // alpha
                    null,               // scaleOscPercent
                    1.0f                // uvScale
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
                    0.0f, 0.0f,          // orbitRadiusX, orbitRadiusZ
                    0.0f,                // orbitSpeed
                    0.0f,                // 📍 orbitOffsetY
                    0.2f,                // scaleAmplitude
                    1.2f,                // instanceScale
                    10.0f,               // spinSpeed
                    true,                // useSolidColor
                    new float[]{1.0f, 0.0f, 1.0f, 1.0f},  // solidColor (magenta)
                    0.8f,                // alpha
                    0.15f,               // scaleOscPercent
                    1.0f                 // uvScale
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
                    0.0f, 0.0f,         // orbitRadiusX, orbitRadiusZ
                    0.0f,               // orbitSpeed
                    0.0f,               // 📍 orbitOffsetY
                    0.15f,              // scaleAmplitude
                    1.8f,               // instanceScale
                    2.0f,               // spinSpeed
                    true,               // useSolidColor
                    new float[]{1.0f, 0.7f, 0.0f, 1.0f},  // solidColor (naranja dorado)
                    1.0f,               // alpha
                    0.1f,               // scaleOscPercent
                    1.0f                // uvScale
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
                    0.0f, 0.0f,          // orbitRadiusX, orbitRadiusZ
                    0.0f,                // orbitSpeed
                    0.0f,                // 📍 orbitOffsetY
                    0.1f,                // scaleAmplitude
                    1.6f,                // instanceScale
                    8.0f,                // spinSpeed
                    true,                // useSolidColor
                    new float[]{0.9f, 0.7f, 0.2f, 1.0f},  // solidColor (amarillo tierra)
                    1.0f,                // alpha
                    null,                // scaleOscPercent
                    1.0f                 // uvScale
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
                    0.0f, 0.0f,          // orbitRadiusX, orbitRadiusZ
                    0.0f,                // orbitSpeed
                    0.0f,                // 📍 orbitOffsetY
                    0.2f,                // scaleAmplitude
                    1.4f,                // instanceScale
                    6.0f,                // spinSpeed
                    true,                // useSolidColor
                    new float[]{0.3f, 0.5f, 0.6f, 1.0f},  // solidColor (azul tormentoso)
                    0.9f,                // alpha
                    null,                // scaleOscPercent
                    1.0f                 // uvScale
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
                    0.0f, 0.0f,          // orbitRadiusX, orbitRadiusZ
                    0.0f,                // orbitSpeed
                    0.0f,                // 📍 orbitOffsetY
                    0.3f,                // scaleAmplitude
                    1.5f,                // instanceScale
                    15.0f,               // spinSpeed
                    true,                // useSolidColor
                    new float[]{1.0f, 0.0f, 1.0f, 1.0f},  // solidColor (magenta retro)
                    1.0f,                // alpha
                    0.2f,                // scaleOscPercent
                    1.0f                 // uvScale
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
                    0.0f, 0.0f,          // orbitRadiusX, orbitRadiusZ
                    0.0f,                // orbitSpeed
                    0.0f,                // 📍 orbitOffsetY
                    0.1f,                // scaleAmplitude
                    1.3f,                // instanceScale
                    4.0f,                // spinSpeed
                    true,                // useSolidColor
                    new float[]{1.0f, 0.8f, 0.9f, 1.0f},  // solidColor (rosa sakura)
                    1.0f,                // alpha
                    0.05f,               // scaleOscPercent
                    1.0f                 // uvScale
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
                    0.0f, 0.0f,          // orbitRadiusX, orbitRadiusZ
                    0.0f,                // orbitSpeed
                    0.0f,                // 📍 orbitOffsetY
                    0.25f,               // scaleAmplitude
                    1.4f,                // instanceScale
                    12.0f,               // spinSpeed
                    true,                // useSolidColor
                    new float[]{1.0f, 1.0f, 0.0f, 1.0f},  // solidColor (amarillo eléctrico)
                    1.0f,                // alpha
                    0.25f,               // scaleOscPercent
                    1.0f                 // uvScale
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

    // ═══════════════════════════════════════════════════════════════
    // 🌍✨ TIERRA LIVE HD - Planeta profesional de Sketchfab
    // ═══════════════════════════════════════════════════════════════
    private void setupTierraLiveHDScene() {
        Log.d(TAG, "════════════════════════════════════════════════");
        Log.d(TAG, "   🌍✨ SETTING UP TIERRA LIVE HD SCENE");
        Log.d(TAG, "   Professional Low-Poly Earth Model");
        Log.d(TAG, "════════════════════════════════════════════════");

        // Color de fondo: Espacio oscuro
        GLES20.glClearColor(0.0f, 0.0f, 0.05f, 1.0f);

        // ═══════════════════════════════════════════════════════════
        // 1. FONDO ESTRELLADO
        // ═══════════════════════════════════════════════════════════
        try {
            StarryBackground starryBg = new StarryBackground(
                    context,
                    textureManager,
                    R.drawable.universo03
            );
            sceneObjects.add(starryBg);
            Log.d(TAG, "  ✓ Starry background added");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creating background: " + e.getMessage());
        }

        // ═══════════════════════════════════════════════════════════
        // 2. 🌍✨ TIERRA LIVE HD - Modelo Sketchfab
        // ═══════════════════════════════════════════════════════════
        // Modelo low-poly estilizado con vegetación, árboles, plantas, océanos
        // ~20k triángulos, colores procedurales desde materiales MTL
        try {
            TierraLiveHD tierraHD = new TierraLiveHD(
                    context,
                    textureManager,
                    0.35f  // ✨ Escala acercada para ver las nubes mejor
            );
            tierraHD.setCameraController(sharedCamera);

            sceneObjects.add(tierraHD);
            Log.d(TAG, "  ✓ 🌍✨ TIERRA LIVE HD agregada");
            Log.d(TAG, "  🌳 Con vegetación, árboles y terreno");
            Log.d(TAG, "  💎 Modelo low-poly estilizado (~20k tris)");
            Log.d(TAG, "  🎨 Materiales: Grass, Sand, Tree, Water, Wood");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creating Tierra Live HD: " + e.getMessage());
            e.printStackTrace();
        }

        Log.d(TAG, "════════════════════════════════════════════════");
        Log.d(TAG, "   ✓ TIERRA LIVE HD SCENE COMPLETE!");
        Log.d(TAG, "   Objects: " + sceneObjects.size());
        Log.d(TAG, "════════════════════════════════════════════════");
    }

    private void setupSpaceBattleScene() {
        Log.d(TAG, "Setting up SPACE BATTLE scene...");

        try {
            // Crear la escena de batalla espacial
            spaceBattleScene = new SpaceBattleScene(context, textureManager);
            
            // Asignar cámara
            if (sharedCamera != null) {
                spaceBattleScene.setCameraController(sharedCamera);
            }
            
            // Inicializar la escena
            spaceBattleScene.initialize();
            
            // Agregar a objetos de escena
            sceneObjects.add(spaceBattleScene);
            
            Log.d(TAG, "  ✓ Space Battle scene created successfully");
        } catch (Exception e) {
            Log.e(TAG, "  ✗ Error creating space battle scene: " + e.getMessage(), e);
        }

        Log.d(TAG, "✓ Batalla Galáctica scene complete");
    }

    /**
     * 🔴 PAUSA - Detiene renderizado y libera recursos
     * IMPORTANTE: Puede llamarse múltiples veces seguidas (cambios rápidos de visibilidad)
     */
    public void pause() {
        paused = true;  // Siempre marcar como pausado (sin verificar estado previo)

        // Pausar audio (rápido y seguro llamar múltiples veces)
        if (musicVisualizer != null) {
            musicVisualizer.pause();
        }

        // Guardar estado solo si no se guardó recientemente
        if (playerStats != null) {
            playerStats.endSession();
            playerStats.saveStats();
        }

        // Resetear estado de animación
        isAnimationPlaying = false;
        frozenFrameReady = false;

        Log.d(TAG, "🔴 PAUSE");
    }

    /**
     * 🟢 RESUME - Reactiva el renderizado
     * IMPORTANTE: Puede llamarse múltiples veces seguidas (cambios rápidos de visibilidad)
     */
    public void resume() {
        paused = false;  // Siempre marcar como activo (sin verificar estado previo)

        // Resetear tiempo para evitar saltos de deltaTime
        lastTime = System.nanoTime();
        TimeManager.update();

        // Reactivar audio (rápido y seguro llamar múltiples veces)
        if (musicVisualizer != null) {
            musicVisualizer.resume();
        }

        // Iniciar sesión de juego
        if (playerStats != null) {
            playerStats.startSession();
        }

        // Preparar para renderizado
        frozenFrameReady = false;
        frozenFrameCount = 0;

        Log.d(TAG, "🟢 RESUME");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ▶️ SISTEMA PLAY/PAUSE - Controla animación y recursos
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Alterna entre modo PLAY (animando) y STOP (congelado)
     * Cuando está en STOP:
     * - No actualiza objetos de escena
     * - No consume CPU en cálculos
     * - Pausa captura de audio (ahorra batería)
     * - Solo renderiza el último frame + botón Play
     */
    public void togglePlayPause() {
        isAnimationPlaying = !isAnimationPlaying;

        if (playPauseButton != null) {
            playPauseButton.setPlaying(isAnimationPlaying);
        }

        if (isAnimationPlaying) {
            // ▶️ MODO PLAY: Reanudar todo
            frozenFrameReady = false;
            frozenFrameCount = 0;
            lastTime = System.nanoTime();  // Reset delta time para evitar saltos

            // 🤖 Ocultar saludo y reloj
            if (orbixGreeting != null) {
                orbixGreeting.hide();
            }

            // Reanudar captura de audio
            if (musicVisualizer != null && musicReactiveEnabled) {
                musicVisualizer.resume();
            }

            Log.d(TAG, "╔════════════════════════════════════════╗");
            Log.d(TAG, "║  ▶️ PLAY - Animación INICIADA          ║");
            Log.d(TAG, "║  Audio capture: ACTIVADO               ║");
            Log.d(TAG, "╚════════════════════════════════════════╝");
        } else {
            // ■ MODO STOP: Pausar todo (excepto el renderizado del frame actual)

            // 🤖 Mostrar saludo y reloj
            if (orbixGreeting != null) {
                orbixGreeting.show();
            }

            // Pausar captura de audio para ahorrar batería
            if (musicVisualizer != null) {
                musicVisualizer.pause();
            }

            Log.d(TAG, "╔════════════════════════════════════════╗");
            Log.d(TAG, "║  ■ STOP - Animación DETENIDA           ║");
            Log.d(TAG, "║  Audio capture: PAUSADO                ║");
            Log.d(TAG, "║  Modo bajo consumo activado            ║");
            Log.d(TAG, "╚════════════════════════════════════════╝");
        }
    }

    /**
     * Verifica si la animación está activa
     * @return true si está animando, false si está congelado
     */
    public boolean isAnimationPlaying() {
        return isAnimationPlaying;
    }

    /**
     * Fuerza el estado de animación (útil para restaurar estado)
     */
    public void setAnimationPlaying(boolean playing) {
        if (this.isAnimationPlaying != playing) {
            togglePlayPause();
        }
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
            // ▶️ VERIFICAR PLAYPAUSEBUTTON PRIMERO (prioridad máxima)
            if (action == android.view.MotionEvent.ACTION_DOWN) {
                float tx = event.getX();
                float ty = event.getY();
                float nx = (tx / screenWidth) * 2.0f - 1.0f;
                float ny = -((ty / screenHeight) * 2.0f - 1.0f);

                if (playPauseButton != null && playPauseButton.isInside(nx, ny)) {
                    Log.d(TAG, "▶️ PlayPauseButton tocado en (" + nx + ", " + ny + ")");
                    togglePlayPause();
                    return;  // No procesar más
                }
            }

            // 🎵 VERIFICAR LIKE BUTTON (funciona en TODAS las escenas)
            if (action == android.view.MotionEvent.ACTION_DOWN) {
                float tx = event.getX();
                float ty = event.getY();
                float nx = (tx / screenWidth) * 2.0f - 1.0f;
                float ny = -((ty / screenHeight) * 2.0f - 1.0f);

                if (likeButton != null && likeButton.isTouched(nx, ny)) {
                    Log.d(TAG, "🎵 LikeButton tocado en (" + nx + ", " + ny + ")");
                    likeButton.onPress();
                    handleLikeButtonPress();
                    return;  // No procesar más
                }
            }
            if (action == android.view.MotionEvent.ACTION_UP) {
                if (likeButton != null) {
                    likeButton.onRelease();
                }
            }

            // 🚀 ENRUTAMIENTO ESPECIAL: Si estamos en Batalla Espacial, enrutar eventos táctiles
            if (spaceBattleScene != null) {
                spaceBattleScene.handleTouch(event);
                return;  // No procesar más eventos de touch
            }

            switch (action) {
                case android.view.MotionEvent.ACTION_DOWN:
                    // Usuario empezó a tocar
                    touchX = event.getX();
                    touchY = event.getY();

                    // Convertir coordenadas de píxeles a normalizadas (-1 a 1)
                    float normalizedX = (touchX / screenWidth) * 2.0f - 1.0f;
                    float normalizedY = -((touchY / screenHeight) * 2.0f - 1.0f);  // Invertir Y

                    // (LikeButton ya verificado arriba)

                    // Verificar si el toque está dentro del botón de disparo
                    if (fireButton != null && fireButton.isTouchInside(normalizedX, normalizedY)) {
                        // ═══════════════════════════════════════════════════════════
                        // 🎯 TOQUE EN EL BOTÓN: DISPARO INMEDIATO DE 1 METEORITO
                        // ═══════════════════════════════════════════════════════════
                        if (fireButton.isReady()) {
                            // Disparar inmediatamente 1 meteorito sin carga
                            shootMeteor(0.3f);  // Potencia fija 30%
                            fireButton.startCooldown();
                            Log.d(TAG, String.format("👆🟢 BOTÓN: disparo inmediato - (%.0f, %.0f)", touchX, touchY));
                        } else {
                            Log.d(TAG, "👆🟡 BOTÓN en cooldown - ignorado");
                        }
                        // No activar sistema de carga
                        isTouching = false;
                    } else {
                        // ═══════════════════════════════════════════════════════════
                        // 🎮 TOQUE FUERA DEL BOTÓN: SISTEMA DE CARGA ORIGINAL
                        // ═══════════════════════════════════════════════════════════
                        isTouching = true;
                        touchStartTime = 0f;
                        chargeLevel = 0f;
                        Log.d(TAG, String.format("👆🔋 CARGA iniciada - (%.0f, %.0f)", touchX, touchY));
                    }
                    break;

                case android.view.MotionEvent.ACTION_MOVE:
                    // Usuario está moviendo el dedo (actualizar posición)
                    if (isTouching) {
                        touchX = event.getX();
                        touchY = event.getY();
                    }
                    break;

                case android.view.MotionEvent.ACTION_UP:
                    // (likeButton.onRelease() ya manejado arriba)

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

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎵 SISTEMA DE COMPARTIR CANCIONES - LIKE BUTTON
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 🎵 Maneja el evento de presionar el botón de Like
     *
     * FASE 1: Prototipo manual - comparte una canción de prueba
     * FASE 2: Captura automática de la canción que está reproduciendo
     */
    private void handleLikeButtonPress() {
        try {
            if (songSharingManager == null) {
                Log.e(TAG, "❌ SongSharingManager no inicializado");
                return;
            }

            // Verificar si el usuario puede compartir (rate limiting)
            if (!songSharingManager.canShare()) {
                long remaining = songSharingManager.getRemainingCooldown();
                Log.w(TAG, "⏱️ Cooldown activo: espera " + remaining + "s");
                return;
            }

            // Verificar si el usuario está autenticado
            if (!songSharingManager.isUserLoggedIn()) {
                Log.w(TAG, "👤 Usuario no autenticado - no se puede compartir");
                return;
            }

            // FASE 2: Capturar canción REAL que está reproduciendo
            String songToShare;
            if (MusicNotificationListener.isMusicPlaying()) {
                songToShare = MusicNotificationListener.getFormattedSong();
                Log.d(TAG, "🎵 Música detectada: " + songToShare);
            } else {
                // Si no hay música, usar texto genérico
                songToShare = "♫ Escuchando música";
                Log.w(TAG, "⚠️ No hay música reproduciéndose");
            }

            Log.d(TAG, "🎵 Intentando compartir: " + songToShare);

            // Compartir canción
            songSharingManager.shareSong(songToShare, new SongSharingManager.ShareCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "✅ Canción compartida exitosamente!");
                    // El botón entrará en cooldown automáticamente
                    if (likeButton != null) {
                        likeButton.setCooldown(true);

                        // 💖 Emitir partículas de corazones
                        if (heartParticles != null) {
                            heartParticles.emit(likeButton.getX(), likeButton.getY(), 15);
                        }
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "❌ Error al compartir: " + error);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "❌ Error en handleLikeButtonPress: " + e.getMessage());
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

            // Activar cooldown del botón de disparo (evita doble tap/long press)
            if (fireButton != null) {
                fireButton.startCooldown();
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
    /**
     * 🌍💥 Activa efecto de IMPACTO EN LA TIERRA
     * Muestra impactos naranja/rojo en el escudo invisible
     */
    public void triggerEarthImpact(float x, float y, float z) {
        if (earthShield != null) {
            earthShield.registerImpact(x, y, z);
            Log.d(TAG, String.format("🌍💥 IMPACTO EN LA TIERRA! Posición: (%.2f, %.2f, %.2f)", x, y, z));
        }
    }

    /**
     * @return Referencia al escudo invisible de la Tierra
     */
    public EarthShield getEarthShield() {
        return earthShield;
    }

    /**
     * @return Referencia a la Tierra para detección de colisiones
     */
    public Planeta getEarth() {
        return planetaTierra;
    }

    public void triggerScreenImpact(float intensity) {
        // Screen shake - DESACTIVADO para apreciar mejor el efecto del sol
        /*
        if (sharedCamera != null) {
            sharedCamera.triggerScreenShake(intensity * 0.8f, 0.3f);
        }
        */

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
        // Screen shake MÁS FUERTE - DESACTIVADO para apreciar mejor el efecto del sol
        /*
        if (sharedCamera != null) {
            sharedCamera.triggerScreenShake(intensity * 1.2f, 0.5f);
        }
        */

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
                            String text = icon + " #" + entry.rank + " " + entry.displayName + "\n🪐" + entry.planetsDestroyed;
                            leaderboardTexts[i].setText(text);

                            // Color diferente para el usuario actual
                            if (!entry.isBot && playerStats != null &&
                                entry.planetsDestroyed == playerStats.getPlanetsDestroyed()) {
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

        // ⚠️ BOTS DESHABILITADOS - No se actualizarán automáticamente
        // if (botManager != null) {
        //     botManager.updateBotsIfNeeded();
        // }
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

        // 🪐 REGISTRAR PLANETA DESTRUIDO EN ESTADÍSTICAS (debe hacerse ANTES de actualizar contador)
        if (playerStats != null) {
            playerStats.onPlanetDestroyed();
            Log.d(TAG, "   🪐 Planeta destruido registrado en PlayerStats");
        }

        // 📊 ACTUALIZAR CONTADOR DE PLANETAS DESTRUIDOS (ahora con el valor incrementado)
        if (planetsDestroyedCounter != null && playerStats != null) {
            int totalPlanets = playerStats.getPlanetsDestroyed();
            planetsDestroyedCounter.setText("🪐" + totalPlanets);
            Log.d(TAG, "   📊 Contador actualizado: " + totalPlanets + " planetas destruidos");
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