package com.secret.blackholeglow.scenes;

import android.content.Context;
import android.opengl.GLES30;
import android.util.Log;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.Battery3D;
import com.secret.blackholeglow.Clock3D;
import com.secret.blackholeglow.DeLorean3D;
import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.video.MediaCodecVideoRenderer;
import com.secret.blackholeglow.video.VideoDownloadManager;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                    🚗 NEON CITY SCENE - Synthwave                        ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  DeLorean en carretera infinita estilo synthwave/retrowave.              ║
 * ║  Video de fondo con grid neón + modelo 3D del carro.                     ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class NeonCityScene extends WallpaperScene {
    private static final String TAG = "NeonCityScene";
    private static final String VIDEO_FILE = "neoncityScene.mp4";

    private MediaCodecVideoRenderer videoBackground;
    private VideoDownloadManager downloadManager;
    private EqualizerBarsDJ equalizerDJ;
    private Clock3D clock;
    private Battery3D battery;
    private DeLorean3D delorean;

    @Override
    public String getName() {
        return "NEON_CITY";
    }

    @Override
    public String getDescription() {
        return "Neon City - Synthwave Retrowave";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_neoncity;
    }

    @Override
    protected void setupScene() {
        Log.d(TAG, "🚗 Configurando Neon City Synthwave...");

        downloadManager = VideoDownloadManager.getInstance(context);

        // Video de fondo - Descargar si no existe
        try {
            String localPath = downloadManager.getVideoPath(VIDEO_FILE);

            if (localPath == null) {
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
                Log.d(TAG, "✅ Video Neon City activado");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error Video: " + e.getMessage());
        }

        // 🎵 Ecualizador con tema SYNTHWAVE (Hot Pink → Cyan → Magenta)
        try {
            equalizerDJ = new EqualizerBarsDJ();
            equalizerDJ.initialize();
            equalizerDJ.setTheme(EqualizerBarsDJ.Theme.SYNTHWAVE);  // 🌆 Tema retrowave 80s
            equalizerDJ.setScreenSize(screenWidth, screenHeight);
            Log.d(TAG, "✅ Ecualizador SYNTHWAVE activado");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error EqualizerBarsDJ: " + e.getMessage());
        }

        // ⏰ Reloj con tema SYNTHWAVE (Hot Pink neón)
        try {
            clock = new Clock3D(context, Clock3D.THEME_SYNTHWAVE, 0f, 0.75f);
            Log.d(TAG, "✅ Reloj SYNTHWAVE activado");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error Clock3D: " + e.getMessage());
        }

        // 🔋 Batería con tema SYNTHWAVE (Cyan/Pink)
        try {
            battery = new Battery3D(context, Battery3D.THEME_SYNTHWAVE, 0.81f, -0.34f);
            Log.d(TAG, "✅ Batería SYNTHWAVE activada");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error Battery3D: " + e.getMessage());
        }

        // 🚗 DeLorean 3D
        try {
            delorean = new DeLorean3D(context, textureManager);
            delorean.setCameraController(camera);
            Log.d(TAG, "✅ DeLorean 3D cargado");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error DeLorean3D: " + e.getMessage());
        }

        Log.d(TAG, "🚗 Neon City Synthwave listo!");
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
        if (delorean != null) {
            delorean.release();
            delorean = null;
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
        if (delorean != null) delorean.update(deltaTime);
        super.update(deltaTime);
    }

    @Override
    public void draw() {
        if (isDisposed) return;

        GLES30.glClearColor(0f, 0f, 0f, 1.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        // 1. Video de fondo (carretera synthwave)
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        if (videoBackground != null) videoBackground.draw();

        // 2. 🚗 DeLorean 3D (sobre el video)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        if (delorean != null) delorean.draw();

        // 3. Elementos UI
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // 4. 🎵 Ecualizador SYNTHWAVE
        if (equalizerDJ != null) equalizerDJ.draw();

        // 5. ⏰ Reloj
        if (clock != null) clock.draw();

        // 6. 🔋 Batería
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
            Log.d(TAG, "⏸️ Video Neon City PAUSADO");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        sceneIsActive = true;
        videoCheckTimer = 0f;

        if (videoBackground != null) {
            videoBackground.resume();
            Log.d(TAG, "▶️ Video Neon City REANUDADO");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🔧 TOUCH - SISTEMA DE CALIBRACIÓN DELOREAN
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public boolean onTouchEvent(float normalizedX, float normalizedY, int action) {
        // Pasar TODOS los eventos de touch al DeLorean para calibración completa
        if (delorean != null) {
            boolean handled = delorean.onTouchEvent(normalizedX, normalizedY, action);
            if (handled) return true;
        }
        return super.onTouchEvent(normalizedX, normalizedY, action);
    }
}
