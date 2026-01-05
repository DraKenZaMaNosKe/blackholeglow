package com.secret.blackholeglow.scenes;

import android.opengl.GLES30;
import android.util.Log;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.Battery3D;
import com.secret.blackholeglow.Clock3D;
import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.video.MediaCodecVideoRenderer;
import com.secret.blackholeglow.video.VideoDownloadManager;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║          🌳 ADVENTURE TIME SCENE - Hora de Aventura Fogata              ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Video de Finn, Jake, Tronquitos y Princess Bubblegum alrededor de      ║
 * ║  una fogata en el bosque al atardecer.                                   ║
 * ║                                                                          ║
 * ║  Estilo: Cozy campfire vibes con ecualizador cálido.                    ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class AdventureTimeScene extends WallpaperScene {
    private static final String TAG = "AdventureTime";
    private static final String VIDEO_FILE = "escenaHDA.mp4";

    private MediaCodecVideoRenderer videoBackground;
    private VideoDownloadManager downloadManager;
    private EqualizerBarsDJ equalizerDJ;
    private Clock3D clock;
    private Battery3D battery;

    @Override
    public String getName() {
        return "ADVENTURE_TIME";
    }

    @Override
    public String getDescription() {
        return "Hora de Aventura - Fogata con Finn y Jake";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.hdapreview;
    }

    @Override
    protected void setupScene() {
        Log.d(TAG, "🌳 Configurando Adventure Time Fogata...");

        downloadManager = VideoDownloadManager.getInstance(context);

        // Video de fondo - Descargar si no existe
        try {
            String localPath = downloadManager.getVideoPath(VIDEO_FILE);

            if (localPath == null) {
                // Video no descargado - descargar ahora
                Log.d(TAG, "📥 Descargando video: " + VIDEO_FILE);
                boolean success = downloadManager.downloadVideoSync(VIDEO_FILE, percent -> {
                    Log.d(TAG, "📥 Descarga: " + percent + "%");
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
                videoBackground = new MediaCodecVideoRenderer(context, VIDEO_FILE, localPath);
                videoBackground.initialize();
                Log.d(TAG, "✅ Video de Adventure Time activado");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error Video: " + e.getMessage());
        }

        // 🎵 Ecualizador con tema PYRALIS (rojo/naranja/amarillo como fuego)
        try {
            equalizerDJ = new EqualizerBarsDJ();
            equalizerDJ.initialize();
            equalizerDJ.setTheme(EqualizerBarsDJ.Theme.PYRALIS);  // 🔥 Colores fuego para fogata
            equalizerDJ.setScreenSize(screenWidth, screenHeight);
            Log.d(TAG, "✅ Ecualizador PYRALIS activado");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error EqualizerBarsDJ: " + e.getMessage());
        }

        // ⏰ Reloj con tema PYRALIS (dorado cálido)
        try {
            clock = new Clock3D(context, Clock3D.THEME_PYRALIS, 0f, 0.75f);
            Log.d(TAG, "✅ Reloj PYRALIS activado");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error Clock3D: " + e.getMessage());
        }

        // 🔋 Batería con tema PYRALIS (dorado cálido)
        try {
            battery = new Battery3D(context, Battery3D.THEME_PYRALIS, 0.81f, -0.34f);
            Log.d(TAG, "✅ Batería PYRALIS activada");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error Battery3D: " + e.getMessage());
        }

        Log.d(TAG, "🌳 Adventure Time Fogata listo!");
    }

    @Override
    protected void releaseSceneResources() {
        if (videoBackground != null) {
            videoBackground.release();
            videoBackground = null;
        }
        if (clock != null) {
            clock.dispose();
            clock = null;
        }
        if (battery != null) {
            battery.dispose();
            battery = null;
        }
        if (equalizerDJ != null) {
            equalizerDJ.release();
            equalizerDJ = null;
        }
    }

    // 🔄 Auto-recovery para video
    private float videoCheckTimer = 0f;
    private static final float VIDEO_CHECK_INTERVAL = 2.0f;
    private boolean sceneIsActive = true;

    @Override
    public void update(float deltaTime) {
        // 🔄 AUTO-RECOVERY MEJORADO
        if (!sceneIsActive) {
            Log.w(TAG, "🔧 Auto-fix: update() llamado pero sceneIsActive=false, corrigiendo...");
            sceneIsActive = true;
        }

        videoCheckTimer += deltaTime;
        if (videoCheckTimer >= VIDEO_CHECK_INTERVAL) {
            videoCheckTimer = 0f;
            if (videoBackground != null && !videoBackground.isPlaying()) {
                Log.w(TAG, "⚠️ Video detenido pero escena activa - Auto-recovery");
                videoBackground.resume();
            }
        }

        if (equalizerDJ != null) equalizerDJ.update(deltaTime);
        if (clock != null) clock.update(deltaTime);
        if (battery != null) battery.update(deltaTime);
        super.update(deltaTime);
    }

    @Override
    public void draw() {
        if (isDisposed) return;

        GLES30.glClearColor(0f, 0f, 0f, 1.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        // 1. Video de fondo
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        if (videoBackground != null) videoBackground.draw();

        // 2. Elementos UI
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // 3. 🎵 Ecualizador PYRALIS
        if (equalizerDJ != null) equalizerDJ.draw();

        // 4. ⏰ Reloj PYRALIS
        if (clock != null) clock.draw();

        // 5. 🔋 Batería PYRALIS
        if (battery != null) battery.draw();

        super.draw();
    }

    @Override
    public void setScreenSize(int width, int height) {
        super.setScreenSize(width, height);
        if (equalizerDJ != null) equalizerDJ.setScreenSize(width, height);
    }

    public void updateMusicBands(float[] bands) {
        if (equalizerDJ != null && bands != null && bands.length > 0) {
            float sum = 0;
            for (float b : bands) sum += b;
            if (sum > 0.1f) {
                Log.d(TAG, "🎵 Datos música: sum=" + sum);
            }
            equalizerDJ.updateFromBands(bands);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ⏸️▶️ PAUSE/RESUME
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void onPause() {
        super.onPause();
        sceneIsActive = false;
        videoCheckTimer = 0f;

        if (videoBackground != null) {
            videoBackground.pause();
            Log.d(TAG, "⏸️ Video Adventure Time PAUSADO");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        sceneIsActive = true;
        videoCheckTimer = 0f;

        if (videoBackground != null) {
            videoBackground.resume();
            Log.d(TAG, "▶️ Video Adventure Time REANUDADO");
        }
    }

    // Touch events (no electric sparks, just simple interaction)
    @Override
    public boolean onTouchEvent(float normalizedX, float normalizedY, int action) {
        // No special touch effects for this cozy scene
        return false;
    }
}
