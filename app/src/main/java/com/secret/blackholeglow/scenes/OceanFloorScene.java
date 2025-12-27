package com.secret.blackholeglow.scenes;

import android.opengl.GLES20;
import android.util.Log;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.video.MediaCodecVideoRenderer;
import com.secret.blackholeglow.video.AbyssalLurker3D;
import com.secret.blackholeglow.video.ForegroundMask;
import com.secret.blackholeglow.video.DecorationSprite;

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
    private ForegroundMask foregroundMask;
    private EqualizerBarsDJ equalizerDJ;
    private DecorationSprite crystals;

    private static final String VIDEO_FILE = "escena_fondoSC.mp4";

    private int screenWidth = 1080;
    private int screenHeight = 1920;

    @Override
    public String getName() { return "Fondo del Mar"; }

    @Override
    public String getDescription() {
        return "Océano alienígena con plantas bioluminescentes";
    }

    @Override
    public int getPreviewResourceId() { return R.drawable.preview_oceano_sc; }

    @Override
    protected void setupScene() {
        Log.d(TAG, "Configurando escena");

        videoRenderer = new MediaCodecVideoRenderer(context, VIDEO_FILE);
        videoRenderer.initialize();

        abyssalLurker = new AbyssalLurker3D(context);
        abyssalLurker.initialize();

        foregroundMask = new ForegroundMask(context, "foreground_plants.png");
        foregroundMask.initialize();

        // 💎 Cristales bioluminiscentes (derecha-centro)
        crystals = new DecorationSprite(context, R.drawable.cristalesbio, 0.72f, -0.32f, 0.5f, 0.4f);
        crystals.initialize();

        // 🎵 Ecualizador
        try {
            equalizerDJ = new EqualizerBarsDJ();
            equalizerDJ.initialize();
            equalizerDJ.setScreenSize(screenWidth, screenHeight);
        } catch (Exception e) {
            Log.e(TAG, "Error EqualizerBarsDJ: " + e.getMessage());
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
        if (foregroundMask != null) {
            foregroundMask.release();
            foregroundMask = null;
        }
        if (crystals != null) {
            crystals.release();
            crystals = null;
        }
        if (equalizerDJ != null) {
            equalizerDJ = null;
        }
    }

    @Override
    public void update(float deltaTime) {
        if (abyssalLurker != null) abyssalLurker.update(deltaTime);
        if (equalizerDJ != null) equalizerDJ.update(deltaTime);
        super.update(deltaTime);
    }

    @Override
    public void draw() {
        if (isDisposed || videoRenderer == null) return;

        GLES20.glClearColor(0.02f, 0.0f, 0.05f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Orden: Video → Pez3D → [2D overlays sin depth test] → Ecualizador
        videoRenderer.draw();
        if (abyssalLurker != null) abyssalLurker.draw();

        // 🎨 Deshabilitar depth test para sprites 2D (todos en Z=0)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        if (foregroundMask != null) foregroundMask.draw();  // Plantas
        if (crystals != null) crystals.draw();              // Cristales (frente)
        if (equalizerDJ != null) equalizerDJ.draw();

        super.draw();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (videoRenderer != null) videoRenderer.pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (videoRenderer != null) videoRenderer.resume();
    }

    @Override
    public void setScreenSize(int width, int height) {
        super.setScreenSize(width, height);
        this.screenWidth = width;
        this.screenHeight = height;

        if (equalizerDJ != null) equalizerDJ.setScreenSize(width, height);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 👆 TOUCH - El pez nada hacia donde toques
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public boolean onTouchEvent(float normalizedX, float normalizedY, int action) {
        if (action == android.view.MotionEvent.ACTION_DOWN) {
            // 📍 LOG PARA CALIBRAR POSICIÓN DE DECORACIONES
            Log.d(TAG, "📍 TOUCH: X=" + String.format("%.2f", normalizedX) + " Y=" + String.format("%.2f", normalizedY));
        }

        if (action == android.view.MotionEvent.ACTION_DOWN ||
            action == android.view.MotionEvent.ACTION_MOVE) {
            if (abyssalLurker != null) {
                abyssalLurker.setTargetPosition(normalizedX, normalizedY);
            }
            return true;
        }
        return false;
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
