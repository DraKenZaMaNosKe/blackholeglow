package com.secret.blackholeglow.scenes;

import android.opengl.GLES20;
import android.util.Log;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.video.VideoWallpaperRenderer;
import com.secret.blackholeglow.video.AlienFishSprite;
import com.secret.blackholeglow.video.ForegroundMask;

/**
 * OceanFloorScene - Fondo del Mar Alienígena
 *
 * Video + Pez + Máscara de profundidad
 * El video NUNCA se pausa - loop infinito.
 */
public class OceanFloorScene extends WallpaperScene {
    private static final String TAG = "OceanScene";

    private VideoWallpaperRenderer videoRenderer;
    private AlienFishSprite alienFish;
    private ForegroundMask foregroundMask;

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

        videoRenderer = new VideoWallpaperRenderer(context, VIDEO_FILE);
        videoRenderer.initialize();

        alienFish = new AlienFishSprite();
        alienFish.initialize();

        foregroundMask = new ForegroundMask(context, "foreground_plants.png");
        foregroundMask.initialize();

        Log.d(TAG, "Escena lista");
    }

    @Override
    protected void releaseSceneResources() {
        Log.d(TAG, "Liberando escena");

        if (videoRenderer != null) {
            videoRenderer.release();
            videoRenderer = null;
        }
        if (alienFish != null) {
            alienFish.release();
            alienFish = null;
        }
        if (foregroundMask != null) {
            foregroundMask.release();
            foregroundMask = null;
        }
    }

    @Override
    public void update(float deltaTime) {
        if (alienFish != null) alienFish.update(deltaTime);
        super.update(deltaTime);
    }

    @Override
    public void draw() {
        if (isDisposed || videoRenderer == null) return;

        GLES20.glClearColor(0.02f, 0.0f, 0.05f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        videoRenderer.draw();
        if (alienFish != null) alienFish.draw();
        if (foregroundMask != null) foregroundMask.draw();

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
    }
}
