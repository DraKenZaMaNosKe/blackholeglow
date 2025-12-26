package com.secret.blackholeglow.scenes;

import android.opengl.GLES20;
import android.util.Log;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.video.MediaCodecVideoRenderer;
import com.secret.blackholeglow.video.AbyssalLurker3D;
import com.secret.blackholeglow.video.ForegroundMask;

/**
 * OceanFloorScene - Fondo del Mar Alienígena
 *
 * Video (MediaCodec directo) + Pez 3D + Máscara de profundidad + Ecualizador
 * El video NUNCA se pausa - loop infinito.
 */
public class OceanFloorScene extends WallpaperScene {
    private static final String TAG = "OceanScene";

    private MediaCodecVideoRenderer videoRenderer;
    private AbyssalLurker3D abyssalLurker;  // 🐟 Pez 3D Meshy
    private ForegroundMask foregroundMask;
    private EqualizerBarsDJ equalizerDJ;    // 🎵 Ecualizador

    private static final String VIDEO_FILE = "escena_fondoSC.mp4";

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

        // 🎵 Ecualizador
        try {
            equalizerDJ = new EqualizerBarsDJ();
            equalizerDJ.initialize();
            Log.d(TAG, "✓ 🎵 EqualizerBarsDJ agregado");
        } catch (Exception e) {
            Log.e(TAG, "✗ Error creando EqualizerBarsDJ: " + e.getMessage());
        }

        Log.d(TAG, "Escena lista (3D Abyssal Lurker + Ecualizador)");
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

        // Orden de profundidad: Video → Pez3D → Máscara → Ecualizador
        videoRenderer.draw();
        if (abyssalLurker != null) abyssalLurker.draw();
        if (foregroundMask != null) foregroundMask.draw();
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
        if (equalizerDJ != null) equalizerDJ.setScreenSize(width, height);
    }

    // 🎵 Recibe datos de música desde WallpaperDirector
    public void updateMusicBands(float[] bands) {
        if (equalizerDJ != null) {
            equalizerDJ.updateFromBands(bands);
        }
    }
}
