package com.secret.blackholeglow.scenes;

import android.content.Context;
import android.opengl.GLES30;
import android.util.Log;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.Battery3D;
import com.secret.blackholeglow.Clock3D;
import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.effects.ElectricSparksOverlay;
import com.secret.blackholeglow.video.MediaCodecVideoRenderer;
import com.secret.blackholeglow.video.VideoDownloadManager;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                    🐉 GOKU SCENE - Kamehameha                            ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Video de Goku lanzando Kamehameha con ecualizador azul energía Ki.      ║
 * ║  Estilo Dragon Ball FighterZ - visual espectacular.                      ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class GokuScene extends WallpaperScene {
    private static final String TAG = "GokuScene";
    private static final String VIDEO_FILE = "gokukamehameFinal.mp4";

    private MediaCodecVideoRenderer videoBackground;
    private VideoDownloadManager downloadManager;
    private EqualizerBarsDJ equalizerDJ;
    private Clock3D clock;
    private Battery3D battery;
    private ElectricSparksOverlay electricSparks;  // ⚡ Rayos Ultra Instinct

    @Override
    public String getName() {
        return "GOKU";
    }

    @Override
    public String getDescription() {
        return "Goku Kamehameha - Energía Ki";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_goku;
    }

    @Override
    protected void setupScene() {
        Log.d(TAG, "🐉 Configurando Goku Kamehameha...");

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
                Log.d(TAG, "✅ Video de Goku activado");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error Video: " + e.getMessage());
        }

        // 🎵 Ecualizador con tema KAMEHAMEHA (azul/cyan/blanco)
        try {
            equalizerDJ = new EqualizerBarsDJ();
            equalizerDJ.initialize();
            equalizerDJ.setTheme(EqualizerBarsDJ.Theme.KAMEHAMEHA);  // 🐉 Tema energía Ki
            equalizerDJ.setScreenSize(screenWidth, screenHeight);
            Log.d(TAG, "✅ Ecualizador KAMEHAMEHA activado");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error EqualizerBarsDJ: " + e.getMessage());
        }

        // ⏰ Reloj con tema KAMEHAMEHA (azul energía)
        try {
            clock = new Clock3D(context, Clock3D.THEME_KAMEHAMEHA, 0f, 0.75f);
            clock.setShowMilliseconds(true);
            Log.d(TAG, "✅ Reloj KAMEHAMEHA activado");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error Clock3D: " + e.getMessage());
        }

        // 🔋 Batería con tema KAMEHAMEHA (energía Ki)
        try {
            battery = new Battery3D(context, Battery3D.THEME_KAMEHAMEHA, 0.81f, -0.34f);
            Log.d(TAG, "✅ Batería KAMEHAMEHA activada");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error Battery3D: " + e.getMessage());
        }

        // ⚡ Electric Sparks - Rayos Ultra Instinct al tocar
        try {
            electricSparks = new ElectricSparksOverlay();
            electricSparks.initialize();
            electricSparks.setScreenSize(screenWidth, screenHeight);
            Log.d(TAG, "✅ Electric Sparks activado ⚡");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error ElectricSparks: " + e.getMessage());
        }

        Log.d(TAG, "🐉 Goku Kamehameha listo!");
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
        if (electricSparks != null) {
            electricSparks.release();
            electricSparks = null;
        }
    }

    // 🔄 Auto-recovery para video
    private float videoCheckTimer = 0f;
    private static final float VIDEO_CHECK_INTERVAL = 2.0f;
    private boolean sceneIsActive = true;

    @Override
    public void update(float deltaTime) {
        // 🔄 AUTO-RECOVERY (sin logs para mejor rendimiento)
        if (!sceneIsActive) {
            sceneIsActive = true;
        }

        videoCheckTimer += deltaTime;
        if (videoCheckTimer >= VIDEO_CHECK_INTERVAL) {
            videoCheckTimer = 0f;
            if (videoBackground != null && !videoBackground.isPlaying()) {
                videoBackground.resume();
            }
        }

        if (equalizerDJ != null) equalizerDJ.update(deltaTime);
        if (clock != null) clock.update(deltaTime);
        if (battery != null) battery.update(deltaTime);
        if (electricSparks != null) electricSparks.update(deltaTime);  // ⚡
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

        // 2. ⚡ Electric Sparks (rayos sobre el video)
        if (electricSparks != null) electricSparks.draw();

        // 3. Elementos UI
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // 4. 🎵 Ecualizador KAMEHAMEHA
        if (equalizerDJ != null) equalizerDJ.draw();

        // 4. ⏰ Reloj KAMEHAMEHA
        if (clock != null) clock.draw();

        // 5. 🔋 Batería KAMEHAMEHA
        if (battery != null) battery.draw();

        super.draw();
    }

    @Override
    public void setScreenSize(int width, int height) {
        super.setScreenSize(width, height);
        if (equalizerDJ != null) equalizerDJ.setScreenSize(width, height);
        if (electricSparks != null) electricSparks.setScreenSize(width, height);
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
            Log.d(TAG, "⏸️ Video de Goku PAUSADO");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        sceneIsActive = true;
        videoCheckTimer = 0f;

        if (videoBackground != null) {
            videoBackground.resume();
            Log.d(TAG, "▶️ Video de Goku REANUDADO");
        }
    }

    // ⚡ Touch events - Rayos Ultra Instinct
    @Override
    public boolean onTouchEvent(float normalizedX, float normalizedY, int action) {
        // Trigger electric sparks on touch down and move
        if (electricSparks != null && (action == 0 || action == 2)) {  // ACTION_DOWN=0, ACTION_MOVE=2
            electricSparks.triggerSpark(normalizedX, normalizedY);
            return true;
        }
        return false;
    }
}
