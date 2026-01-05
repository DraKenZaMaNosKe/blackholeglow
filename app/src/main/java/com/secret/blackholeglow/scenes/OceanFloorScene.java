package com.secret.blackholeglow.scenes;

import android.opengl.GLES20;
import android.util.Log;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.Battery3D;
import com.secret.blackholeglow.Clock3D;
import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.video.MediaCodecVideoRenderer;
import com.secret.blackholeglow.video.VideoDownloadManager;
import com.secret.blackholeglow.video.VideoConfig;
import com.secret.blackholeglow.video.AbyssalLurker3D;
import com.secret.blackholeglow.video.AbyssalLeviathan3D;
import com.secret.blackholeglow.video.BubbleSystem;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║              OceanFloorScene - Fondo del Mar Alienígena                  ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Video (MediaCodec) + Pez 3D + Máscara + Ecualizador                     ║
 * ║                                                                          ║
 * ║  INTERACTIVIDAD:                                                         ║
 * ║  • Toca la pantalla → El pez nada hacia donde tocaste                    ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class OceanFloorScene extends WallpaperScene {
    private static final String TAG = "OceanScene";

    private MediaCodecVideoRenderer videoRenderer;
    private AbyssalLurker3D abyssalLurker;
    private AbyssalLeviathan3D abyssalLeviathan;
    private EqualizerBarsDJ equalizerDJ;
    private BubbleSystem bubbleSystem;
    private Clock3D clock;
    private Battery3D battery;
    private VideoDownloadManager downloadManager;

    private static final String VIDEO_FILE = "marZerg.mp4";

    // 🫧 Timers para spawn de burbujas (sincronizado con respiración)
    private float lurkerBubbleTimer = 0f;
    private float leviathanBubbleTimer = 0f;
    private static final float LURKER_BUBBLE_INTERVAL = 1.8f;     // Respiración lenta
    private static final float LEVIATHAN_BUBBLE_INTERVAL = 1.2f;  // Respiración del grande

    private int screenWidth = 1080;
    private int screenHeight = 1920;

    // 🔄 Auto-recovery para video
    private float videoCheckTimer = 0f;
    private static final float VIDEO_CHECK_INTERVAL = 2.0f;
    private boolean sceneIsActive = true;

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎛️ SISTEMA DE CALIBRACIÓN
    // ═══════════════════════════════════════════════════════════════════════════
    private static final boolean CALIBRATION_MODE = false;  // Calibración completada ✓
    private int selectedFish = 0;  // 0 = Lurker, 1 = Leviathan
    private int adjustMode = 0;    // 0=PosXY, 1=PosZ, 2=RotXY, 3=RotZ, 4=Scale
    private float lastTouchX = 0f;
    private float lastTouchY = 0f;
    private long lastTapTime = 0;

    @Override
    public String getName() { return "ABYSSIA"; }

    @Override
    public String getDescription() {
        return "Océano alienígena con plantas bioluminescentes";
    }

    @Override
    public int getPreviewResourceId() { return R.drawable.preview_oceano_sc; }

    @Override
    protected void setupScene() {
        Log.d(TAG, "Configurando escena");

        downloadManager = VideoDownloadManager.getInstance(context);

        // Video de fondo - El preloader ya lo descargo de Supabase
        String localPath = downloadManager.getVideoPath(VIDEO_FILE);

        // Si no existe, descarga on-demand (Preview mode bypass)
        if (localPath == null) {
            Log.d(TAG, "📥 Descargando video on-demand: " + VIDEO_FILE);
            boolean success = downloadManager.downloadVideoSync(VIDEO_FILE, progress -> {
                Log.d(TAG, "📥 Descarga: " + progress + "%");
            });
            if (success) {
                localPath = downloadManager.getVideoPath(VIDEO_FILE);
                Log.d(TAG, "✅ Video descargado: " + localPath);
            } else {
                Log.e(TAG, "❌ Error descargando video");
                return;
            }
        }

        if (localPath != null) {
            Log.d(TAG, "📦 Usando video: " + localPath);
            videoRenderer = new MediaCodecVideoRenderer(context, VIDEO_FILE, localPath);
        } else {
            Log.e(TAG, "❌ Video no disponible: " + VIDEO_FILE);
            return;
        }
        videoRenderer.initialize();

        abyssalLurker = new AbyssalLurker3D(context);
        abyssalLurker.initialize();

        // 🐉 Leviathan - bestia colosal del fondo
        abyssalLeviathan = new AbyssalLeviathan3D(context);
        abyssalLeviathan.initialize();

        // 🎛️ Activar calibración si está habilitada
        if (CALIBRATION_MODE) {
            abyssalLurker.setCalibrationMode(true);
            abyssalLeviathan.setCalibrationMode(true);
            Log.d("CALIBRATE", "════════════════════════════════════════════════");
            Log.d("CALIBRATE", "🎛️ MODO CALIBRACIÓN ACTIVADO");
            Log.d("CALIBRATE", "   Double-tap: Cambiar pez (Lurker↔Leviathan)");
            Log.d("CALIBRATE", "   Tap sup-izq: Cambiar modo ajuste");
            Log.d("CALIBRATE", "   Arrastrar: Ajusta valor según modo");
            Log.d("CALIBRATE", "════════════════════════════════════════════════");
        }

        // 🎵 Ecualizador con tema ABYSSIA (océano profundo)
        try {
            equalizerDJ = new EqualizerBarsDJ();
            equalizerDJ.initialize();
            equalizerDJ.setTheme(EqualizerBarsDJ.Theme.ABYSSIA);  // 🌊 Tema océano
            equalizerDJ.setScreenSize(screenWidth, screenHeight);
        } catch (Exception e) {
            Log.e(TAG, "Error EqualizerBarsDJ: " + e.getMessage());
        }

        // 🫧 Sistema de burbujas
        try {
            bubbleSystem = new BubbleSystem();
            bubbleSystem.initialize();
            Log.d(TAG, "✅ Sistema de burbujas activado");
        } catch (Exception e) {
            Log.e(TAG, "Error BubbleSystem: " + e.getMessage());
        }

        // ⏰ Reloj con tema ABYSSIA (cyan bioluminiscente)
        try {
            clock = new Clock3D(context, Clock3D.THEME_ABYSSIA, 0f, 0.75f);
            Log.d(TAG, "✅ Reloj ABYSSIA activado");
        } catch (Exception e) {
            Log.e(TAG, "Error Clock3D: " + e.getMessage());
        }

        // 🔋 Batería con tema ABYSSIA (orbe bioluminiscente)
        try {
            battery = new Battery3D(context, Battery3D.THEME_ABYSSIA, 0.81f, -0.34f);
            Log.d(TAG, "✅ Batería ABYSSIA activada");
        } catch (Exception e) {
            Log.e(TAG, "Error Battery3D: " + e.getMessage());
        }
    }

    @Override
    protected void releaseSceneResources() {
        Log.d(TAG, "Liberando escena");

        if (videoRenderer != null) {
            videoRenderer.release();
            videoRenderer = null;
        }
        if (abyssalLurker != null) {
            abyssalLurker.release();
            abyssalLurker = null;
        }
        if (abyssalLeviathan != null) {
            abyssalLeviathan.release();
            abyssalLeviathan = null;
        }
        if (equalizerDJ != null) {
            equalizerDJ = null;
        }
        if (clock != null) {
            clock.dispose();
            clock = null;
        }
        if (battery != null) {
            battery.dispose();
            battery = null;
        }
        if (bubbleSystem != null) {
            bubbleSystem = null;
        }
    }

    @Override
    public void update(float deltaTime) {
        // 🔄 AUTO-RECOVERY MEJORADO:
        // Si update() se está llamando, significa que el render loop está activo
        if (!sceneIsActive) {
            Log.w(TAG, "🔧 Auto-fix: update() llamado pero sceneIsActive=false, corrigiendo...");
            sceneIsActive = true;
        }

        videoCheckTimer += deltaTime;
        if (videoCheckTimer >= VIDEO_CHECK_INTERVAL) {
            videoCheckTimer = 0f;
            if (videoRenderer != null && !videoRenderer.isPlaying()) {
                Log.w(TAG, "⚠️ Video detenido pero escena activa - Auto-recovery");
                videoRenderer.resume();
            }
        }

        // Actualizar Leviathan primero
        if (abyssalLeviathan != null) abyssalLeviathan.update(deltaTime);

        // Pasar posición del Leviathan al Lurker (para que huya)
        if (abyssalLurker != null && abyssalLeviathan != null) {
            abyssalLurker.setLeviathanPosition(
                abyssalLeviathan.getPosX(),
                abyssalLeviathan.getPosY()
            );
        }

        // Actualizar Lurker (con info del Leviathan)
        if (abyssalLurker != null) abyssalLurker.update(deltaTime);

        if (equalizerDJ != null) equalizerDJ.update(deltaTime);
        if (clock != null) clock.update(deltaTime);
        if (battery != null) battery.update(deltaTime);

        // 🫧 Actualizar sistema de burbujas
        if (bubbleSystem != null) {
            bubbleSystem.update(deltaTime);

            // Spawn burbujas del Lurker (pez pequeño)
            if (abyssalLurker != null) {
                lurkerBubbleTimer += deltaTime;
                if (lurkerBubbleTimer >= LURKER_BUBBLE_INTERVAL) {
                    lurkerBubbleTimer = 0f;
                    // Usar posición calculada de la boca + Y como profundidad
                    float depth = abyssalLurker.getPosY();  // Y alto = lejos = burbujas pequeñas
                    bubbleSystem.spawn(abyssalLurker.getMouthX(), abyssalLurker.getMouthY(), depth);
                }
            }

            // Spawn burbujas del Leviathan (bestia grande)
            if (abyssalLeviathan != null) {
                leviathanBubbleTimer += deltaTime;
                if (leviathanBubbleTimer >= LEVIATHAN_BUBBLE_INTERVAL) {
                    leviathanBubbleTimer = 0f;
                    // Usar posición calculada de la boca + Y como profundidad
                    float depth = abyssalLeviathan.getPosY();
                    bubbleSystem.spawn(abyssalLeviathan.getMouthX(), abyssalLeviathan.getMouthY(), depth);
                }
            }
        }

        super.update(deltaTime);
    }

    @Override
    public void draw() {
        if (isDisposed || videoRenderer == null) return;

        GLES20.glClearColor(0.02f, 0.0f, 0.05f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Orden: Video → Leviathan → Lurker → Burbujas → Ecualizador
        videoRenderer.draw();
        if (abyssalLeviathan != null) abyssalLeviathan.draw();  // Grande, fondo
        if (abyssalLurker != null) abyssalLurker.draw();        // Pequeño, frente

        // 🎨 Deshabilitar depth test para elementos 2D
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        // 🫧 Burbujas (después de peces, antes de ecualizador)
        if (bubbleSystem != null && screenHeight > 0) {
            float aspectRatio = (float)screenWidth / screenHeight;
            bubbleSystem.draw(aspectRatio);
        }

        if (equalizerDJ != null) equalizerDJ.draw();
        if (clock != null) clock.draw();

        // 🔋 Batería ABYSSIA
        if (battery != null) battery.draw();

        super.draw();
    }

    @Override
    public void onPause() {
        super.onPause();
        sceneIsActive = false;
        if (videoRenderer != null) {
            videoRenderer.pause();
            Log.d(TAG, "⏸️ Video de fondo PAUSADO");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        sceneIsActive = true;
        if (videoRenderer != null) {
            videoRenderer.resume();
            Log.d(TAG, "▶️ Video de fondo REANUDADO");
        }
    }

    @Override
    public void setScreenSize(int width, int height) {
        super.setScreenSize(width, height);
        this.screenWidth = width;
        this.screenHeight = height;

        if (equalizerDJ != null) equalizerDJ.setScreenSize(width, height);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 👆 TOUCH - Calibración o seguimiento normal
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public boolean onTouchEvent(float normalizedX, float normalizedY, int action) {
        if (CALIBRATION_MODE) {
            return handleCalibrationTouch(normalizedX, normalizedY, action);
        }

        // 👆 LURKER sigue el dedo del usuario
        if (action == android.view.MotionEvent.ACTION_DOWN ||
            action == android.view.MotionEvent.ACTION_MOVE) {

            if (abyssalLurker != null) {
                // Convertir coordenadas normalizadas (-1 a 1) a espacio mundo del Lurker
                // La pantalla tiene normalizedX de -1 (izq) a 1 (der)
                // normalizedY de -1 (abajo) a 1 (arriba)
                float worldX = normalizedX * 0.8f;  // Escalar para zona del pez
                float worldY = normalizedY * 0.9f;  // Escalar para zona del pez

                abyssalLurker.setTouchTarget(worldX, worldY);
                return true;
            }
        }

        return false;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎛️ CALIBRACIÓN - Sistema de ajuste por touch
    // ═══════════════════════════════════════════════════════════════════════════
    private static final String[] MODE_NAMES = {"PosXY", "PosZ", "RotXY", "RotZ", "Scale"};

    private boolean handleCalibrationTouch(float x, float y, int action) {
        switch (action) {
            case android.view.MotionEvent.ACTION_DOWN:
                long now = System.currentTimeMillis();

                // Double-tap: cambiar pez seleccionado
                if (now - lastTapTime < 300) {
                    selectedFish = (selectedFish + 1) % 2;
                    String fishName = selectedFish == 0 ? "🐟 LURKER" : "🐉 LEVIATHAN";
                    Log.d("CALIBRATE", "═══ SELECCIONADO: " + fishName + " ═══");
                    logCurrentFish("SELECTED");
                }
                // Tap esquina superior izquierda: cambiar modo
                else if (x < -0.7f && y > 0.7f) {
                    adjustMode = (adjustMode + 1) % 5;
                    Log.d("CALIBRATE", "📐 MODO: " + MODE_NAMES[adjustMode]);
                }

                lastTapTime = now;
                lastTouchX = x;
                lastTouchY = y;
                return true;

            case android.view.MotionEvent.ACTION_MOVE:
                float dx = x - lastTouchX;
                float dy = y - lastTouchY;
                applyCalibrationDelta(dx, dy);
                lastTouchX = x;
                lastTouchY = y;
                return true;

            case android.view.MotionEvent.ACTION_UP:
                logCurrentFish("FINAL");
                return true;
        }
        return false;
    }

    private void applyCalibrationDelta(float dx, float dy) {
        // Sensibilidad diferente por modo
        float posSens = 0.5f;
        float rotSens = 50f;
        float scaleSens = 0.3f;

        if (selectedFish == 0 && abyssalLurker != null) {
            switch (adjustMode) {
                case 0: abyssalLurker.adjustPosition(dx * posSens, -dy * posSens, 0); break;
                case 1: abyssalLurker.adjustPosition(0, 0, dy * posSens); break;
                case 2: abyssalLurker.adjustRotation(-dy * rotSens, dx * rotSens, 0); break;
                case 3: abyssalLurker.adjustRotation(0, 0, dx * rotSens); break;
                case 4: abyssalLurker.adjustScale(-dy * scaleSens); break;
            }
        } else if (selectedFish == 1 && abyssalLeviathan != null) {
            switch (adjustMode) {
                case 0: abyssalLeviathan.adjustPosition(dx * posSens, -dy * posSens, 0); break;
                case 1: abyssalLeviathan.adjustPosition(0, 0, dy * posSens); break;
                case 2: abyssalLeviathan.adjustRotation(-dy * rotSens, dx * rotSens, 0); break;
                case 3: abyssalLeviathan.adjustRotation(0, 0, dx * rotSens); break;
                case 4: abyssalLeviathan.adjustScale(-dy * scaleSens); break;
            }
        }
    }

    private void logCurrentFish(String event) {
        if (selectedFish == 0 && abyssalLurker != null) {
            abyssalLurker.logCalibration(event);
        } else if (selectedFish == 1 && abyssalLeviathan != null) {
            abyssalLeviathan.logCalibration(event);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎵 MÚSICA
    // ═══════════════════════════════════════════════════════════════════════════

    public void updateMusicBands(float[] bands) {
        if (equalizerDJ != null) {
            equalizerDJ.updateFromBands(bands);
        }
    }
}
