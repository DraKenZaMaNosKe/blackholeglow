package com.secret.blackholeglow.scenes;

import android.content.Context;
import android.opengl.GLES30;
import android.util.Log;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.video.MediaCodecVideoRenderer;
import com.secret.blackholeglow.video.VideoDownloadManager;
import com.secret.blackholeglow.video.VideoConfig;
import com.secret.blackholeglow.TravelingShip;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                    🌌 LAB SCENE - Portal Cósmico                         ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Video de fondo con nubes de fuego + nave Enterprise viajando.           ║
 * ║  Video editado en CapCut con técnica de transiciones seamless.           ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class LabScene extends WallpaperScene {
    private static final String TAG = "LabScene";
    private static final String VIDEO_FILE = "cielovolando.mp4";

    private MediaCodecVideoRenderer videoBackground;
    private TravelingShip travelingShip;
    private VideoDownloadManager downloadManager;
    private EqualizerBarsDJ equalizerDJ;

    @Override
    public String getName() {
        return "PYRALIS";
    }

    @Override
    public String getDescription() {
        return "Portal cósmico con nubes de fuego";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_space;
    }

    @Override
    protected void setupScene() {
        Log.d(TAG, "🌌 Configurando Portal Cosmico...");

        downloadManager = VideoDownloadManager.getInstance(context);

        // Video de fondo - El preloader ya lo descargo de Supabase
        try {
            String localPath = downloadManager.getVideoPath(VIDEO_FILE);

            if (localPath != null) {
                // Video descargado de Supabase
                Log.d(TAG, "📦 Usando video: " + localPath);
                videoBackground = new MediaCodecVideoRenderer(context, VIDEO_FILE, localPath);
            } else {
                // Video no disponible - esto no deberia pasar si el preloader funciono
                Log.e(TAG, "❌ Video no disponible: " + VIDEO_FILE);
                return;
            }

            videoBackground.initialize();
            Log.d(TAG, "✅ Video de fondo activado");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error Video: " + e.getMessage());
        }

        // Nave Enterprise
        try {
            travelingShip = new TravelingShip(context, textureManager);
            travelingShip.setCameraController(camera);
            Log.d(TAG, "✅ Nave activada");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error Nave: " + e.getMessage());
        }

        // 🎵 Ecualizador con tema PYRALIS (fuego)
        try {
            equalizerDJ = new EqualizerBarsDJ();
            equalizerDJ.initialize();
            equalizerDJ.setTheme(EqualizerBarsDJ.Theme.PYRALIS);  // 🔥 Tema fuego
            equalizerDJ.setScreenSize(screenWidth, screenHeight);
            Log.d(TAG, "✅ Ecualizador PYRALIS activado");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error EqualizerBarsDJ: " + e.getMessage());
        }

        Log.d(TAG, "🌌 Portal Cósmico listo!");
    }

    @Override
    protected void releaseSceneResources() {
        if (videoBackground != null) {
            videoBackground.release();
            videoBackground = null;
        }
        if (travelingShip != null) {
            travelingShip.release();
            travelingShip = null;
        }
        if (equalizerDJ != null) {
            equalizerDJ = null;
        }
    }

    @Override
    public void update(float deltaTime) {
        if (travelingShip != null) travelingShip.update(deltaTime);
        if (equalizerDJ != null) equalizerDJ.update(deltaTime);
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

        // 2. Nave
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        if (travelingShip != null) travelingShip.draw();

        // 3. 🎵 Ecualizador PYRALIS
        if (equalizerDJ != null) equalizerDJ.draw();

        super.draw();
    }

    @Override
    public void setScreenSize(int width, int height) {
        super.setScreenSize(width, height);
        if (equalizerDJ != null) equalizerDJ.setScreenSize(width, height);
    }

    public void updateMusicBands(float[] bands) {
        if (equalizerDJ != null && bands != null && bands.length > 0) {
            // Debug: verificar si hay datos
            float sum = 0;
            for (float b : bands) sum += b;
            if (sum > 0.1f) {
                Log.d(TAG, "🎵 Datos música: sum=" + sum);
            }
            equalizerDJ.updateFromBands(bands);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TOUCH PARA AJUSTAR LLAMAS (EJE X)
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public boolean onTouchEvent(float normalizedX, float normalizedY, int action) {
        if (travelingShip == null) return false;

        // Convertir NDC (-1,1) a coordenadas de pantalla para sensibilidad
        float screenX = (normalizedX + 1f) * 500f;  // Rango aproximado 0-1000
        float screenY = (normalizedY + 1f) * 500f;

        switch (action) {
            case android.view.MotionEvent.ACTION_DOWN:
                travelingShip.onTouchDown(screenX, screenY);
                return true;
            case android.view.MotionEvent.ACTION_MOVE:
                travelingShip.onTouchMove(screenX, screenY);
                return true;
            case android.view.MotionEvent.ACTION_UP:
            case android.view.MotionEvent.ACTION_CANCEL:
                travelingShip.onTouchUp(screenX, screenY);
                return true;
        }
        return false;
    }

}
