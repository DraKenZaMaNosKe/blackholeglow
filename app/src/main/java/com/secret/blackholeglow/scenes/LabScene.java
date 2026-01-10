package com.secret.blackholeglow.scenes;

import android.content.Context;
import android.opengl.GLES30;
import android.util.Log;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.Battery3D;
import com.secret.blackholeglow.Clock3D;
import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.video.MediaCodecVideoRenderer;
import com.secret.blackholeglow.video.VideoDownloadManager;
import com.secret.blackholeglow.video.VideoConfig;
import com.secret.blackholeglow.TravelingShip;
import com.secret.blackholeglow.GyroscopeManager;

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
    private Clock3D clock;
    private Battery3D battery;
    private GyroscopeManager gyroscope;

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
                videoBackground = new MediaCodecVideoRenderer(context, VIDEO_FILE, localPath);
            } else {
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

        // 📱 Giroscopio para control por inclinación
        try {
            gyroscope = new GyroscopeManager(context);
            if (gyroscope.isAvailable() && travelingShip != null) {
                travelingShip.setGyroEnabled(true);
                gyroscope.start();
                Log.d(TAG, "✅ Giroscopio activado para nave");
            } else {
                Log.w(TAG, "⚠️ Giroscopio no disponible");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error Gyroscope: " + e.getMessage());
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

        // ⏰ Reloj con tema PYRALIS (dorado celestial)
        try {
            clock = new Clock3D(context, Clock3D.THEME_PYRALIS, 0f, 0.75f);
            clock.setShowMilliseconds(true);
            Log.d(TAG, "✅ Reloj PYRALIS activado");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error Clock3D: " + e.getMessage());
        }

        // 🔋 Batería con tema PYRALIS (reactor de plasma)
        try {
            battery = new Battery3D(context, Battery3D.THEME_PYRALIS, 0.81f, -0.34f);
            Log.d(TAG, "✅ Batería PYRALIS activada");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error Battery3D: " + e.getMessage());
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
        if (gyroscope != null) {
            gyroscope.release();
            gyroscope = null;
        }
    }

    // 🔄 Auto-recovery para video
    private float videoCheckTimer = 0f;
    private static final float VIDEO_CHECK_INTERVAL = 2.0f;  // Cada 2 segundos
    private boolean sceneIsActive = true;  // Flag para saber si debemos estar corriendo

    @Override
    public void update(float deltaTime) {
        // 📱 Pasar datos del giroscopio a la nave
        if (gyroscope != null && travelingShip != null && gyroscope.isEnabled()) {
            travelingShip.setTiltInput(gyroscope.getTiltX(), gyroscope.getTiltY());
        }

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

        if (travelingShip != null) travelingShip.update(deltaTime);
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

        // 2. Nave
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        if (travelingShip != null) travelingShip.draw();

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
    // ⏸️▶️ PAUSE/RESUME - Libera recursos cuando no es visible
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void onPause() {
        super.onPause();
        sceneIsActive = false;  // 🛑 Marcar escena como inactiva
        videoCheckTimer = 0f;   // Reset timer

        // ⏸️ CRÍTICO: Pausar video para liberar CPU/batería
        if (videoBackground != null) {
            videoBackground.pause();
            Log.d(TAG, "⏸️ Video de fondo PAUSADO");
        }
        // 📱 Pausar giroscopio para ahorrar batería
        if (gyroscope != null) {
            gyroscope.pause();
            Log.d(TAG, "⏸️ Giroscopio PAUSADO");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        sceneIsActive = true;   // ✅ Marcar escena como activa
        videoCheckTimer = 0f;   // Reset timer

        // ▶️ Reanudar video
        if (videoBackground != null) {
            videoBackground.resume();
            Log.d(TAG, "▶️ Video de fondo REANUDADO");
        }
        // 📱 Reanudar giroscopio
        if (gyroscope != null && gyroscope.isAvailable()) {
            gyroscope.resume();
            Log.d(TAG, "▶️ Giroscopio REANUDADO");
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
