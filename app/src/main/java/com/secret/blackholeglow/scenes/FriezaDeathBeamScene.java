package com.secret.blackholeglow.scenes;

import android.opengl.GLES20;
import android.util.Log;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;
import com.secret.blackholeglow.video.DeathBeamFX;
import com.secret.blackholeglow.video.Frieza3D;

/**
 * FriezaDeathBeamScene - Frieza Final Form disparando Death Beam.
 *
 * Componentes:
 * - Video background: anime speed lines (frieza_deathbeam_bg.mp4)
 * - Frieza3D: modelo OBJ con rim light shader
 * - DeathBeamFX: esfera de energia + rayo conico con shaders animados
 */
public class FriezaDeathBeamScene extends BaseVideoScene {
    private static final String TAG = "FriezaDeathBeam";

    private Frieza3D frieza;
    private DeathBeamFX deathBeam;

    // ═══════════════════════════════════════════════════════════════
    // ABSTRACT IMPLEMENTATIONS
    // ═══════════════════════════════════════════════════════════════

    @Override
    public String getName() { return "FRIEZA_DEATHBEAM"; }

    @Override
    public String getDescription() { return "Frieza - Death Beam"; }

    @Override
    public int getPreviewResourceId() { return R.drawable.preview_frieza_deathbeam; }

    @Override
    protected String getVideoFileName() { return "frieza_deathbeam_bg.mp4"; }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.ABYSSIA;
    }

    // ═══════════════════════════════════════════════════════════════
    // SETUP
    // ═══════════════════════════════════════════════════════════════

    @Override
    protected void setupSceneSpecific() {
        Log.d(TAG, "Setting up Frieza Death Beam scene...");

        try {
            frieza = new Frieza3D(context);
            frieza.setScreenSize(screenWidth, screenHeight);
            frieza.setPosition(0.0120f, -0.0330f, 1.2780f);
            frieza.setScale(0.2310f);
        } catch (Exception e) {
            Log.w(TAG, "Frieza3D not ready: " + e.getMessage());
        }

        try {
            deathBeam = new DeathBeamFX();
            deathBeam.setScreenSize(screenWidth, screenHeight);
            deathBeam.setSpherePosition(-0.0480f, 0.2370f, 1.5330f);
            deathBeam.setSphereScale(0.0340f);
            deathBeam.setBeamDirection(-0.7732f, -0.4844f, 0.4092f);
            deathBeam.setBeamLength(4.4720f);
            deathBeam.setBeamRadius(0.2128f);
        } catch (Exception e) {
            Log.w(TAG, "DeathBeamFX not ready: " + e.getMessage());
        }

        Log.d(TAG, "Frieza scene ready!");
    }

    // ═══════════════════════════════════════════════════════════════
    // UPDATE & DRAW
    // ═══════════════════════════════════════════════════════════════

    @Override
    protected void updateSceneSpecific(float deltaTime) {
        if (frieza != null) frieza.update(deltaTime);
        if (deathBeam != null) deathBeam.update(deltaTime);
    }

    @Override
    protected void drawSceneSpecific() {
        // Video background is drawn by BaseVideoScene before this method
        // Just clear depth for 3D overlay
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);

        // 3D scene con depth
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        if (frieza != null) frieza.draw();

        if (deathBeam != null) deathBeam.draw();
    }

    // ═══════════════════════════════════════════════════════════════
    // SCREEN SIZE & RELEASE
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void setScreenSize(int width, int height) {
        super.setScreenSize(width, height);
        if (frieza != null) frieza.setScreenSize(width, height);
        if (deathBeam != null) deathBeam.setScreenSize(width, height);
    }

    @Override
    protected void releaseSceneSpecificResources() {
        if (frieza != null) { frieza.release(); frieza = null; }
        if (deathBeam != null) { deathBeam.release(); deathBeam = null; }
        Log.d(TAG, "Frieza Death Beam resources released");
    }

    @Override
    public boolean isReady() {
        return !isDisposed;
    }
}
