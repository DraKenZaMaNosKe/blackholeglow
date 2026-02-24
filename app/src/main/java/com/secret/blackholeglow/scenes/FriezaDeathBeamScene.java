package com.secret.blackholeglow.scenes;

import android.opengl.GLES20;
import android.util.Log;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;
import com.secret.blackholeglow.video.DeathBeamFX;
import com.secret.blackholeglow.video.Frieza3D;
import com.secret.blackholeglow.video.FriezaHalo3D;

/**
 * FriezaDeathBeamScene - Frieza Final Form disparando Death Beam.
 *
 * Componentes:
 * - Video background: anime speed lines (frieza_deathbeam_bg.mp4)
 * - Frieza3D: modelo OBJ con rim light shader
 * - FriezaHalo3D: aureola angelical con shader de glow blanco
 * - DeathBeamFX: esfera de energia + rayo conico con shaders animados
 * - Floating animation: bobbing + drift sinusoidal
 */
public class FriezaDeathBeamScene extends BaseVideoScene {
    private static final String TAG = "FriezaDeathBeam";

    private Frieza3D frieza;
    private FriezaHalo3D halo;
    private DeathBeamFX deathBeam;

    // ═══════════════════════════════════════════════════════════════
    // FLOATING ANIMATION
    // ═══════════════════════════════════════════════════════════════
    private float floatTime = 0f;

    // Base positions (calibrated)
    private static final float FRIEZA_BASE_X = 0.0120f;
    private static final float FRIEZA_BASE_Y = -0.0330f;
    private static final float FRIEZA_BASE_Z = 1.2780f;

    private static final float BEAM_BASE_X = -0.0480f;
    private static final float BEAM_BASE_Y = 0.2370f;
    private static final float BEAM_BASE_Z = 1.5330f;

    // Halo floats above head (offset from Frieza base)
    private static final float HALO_Y_OFFSET = 0.02f;  // lift above head

    // Floating parameters
    private static final float BOB_AMPLITUDE = 0.008f;   // vertical bob
    private static final float BOB_SPEED = 1.8f;          // bob frequency
    private static final float DRIFT_AMPLITUDE = 0.004f;  // horizontal drift
    private static final float DRIFT_SPEED = 1.1f;        // drift frequency (desfasado del bob)

    // Shared scale (Frieza and Halo share coordinate space)
    private static final float MODEL_SCALE = 0.2310f;

    // Sphere energy pulse (unstable vibration)
    private static final float SPHERE_BASE_SCALE = 0.0340f;
    private static final float SPHERE_PULSE_AMOUNT = 0.006f;  // ~18% variation

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
            frieza.setPosition(FRIEZA_BASE_X, FRIEZA_BASE_Y, FRIEZA_BASE_Z);
            frieza.setScale(MODEL_SCALE);
        } catch (Exception e) {
            Log.w(TAG, "Frieza3D not ready: " + e.getMessage());
        }

        try {
            halo = new FriezaHalo3D(context);
            halo.setScreenSize(screenWidth, screenHeight);
            halo.setPosition(FRIEZA_BASE_X, FRIEZA_BASE_Y + HALO_Y_OFFSET, FRIEZA_BASE_Z);
            halo.setScale(MODEL_SCALE);
        } catch (Exception e) {
            Log.w(TAG, "FriezaHalo3D not ready: " + e.getMessage());
        }

        try {
            deathBeam = new DeathBeamFX();
            deathBeam.setScreenSize(screenWidth, screenHeight);
            deathBeam.setSpherePosition(BEAM_BASE_X, BEAM_BASE_Y, BEAM_BASE_Z);
            deathBeam.setSphereScale(SPHERE_BASE_SCALE);
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
        // Floating animation
        floatTime += deltaTime;
        if (floatTime > 62.83f) floatTime -= 62.83f;  // ~10*2*PI, safe for mediump precision

        float bobOffset = (float) Math.sin(floatTime * BOB_SPEED) * BOB_AMPLITUDE;
        float driftOffset = (float) Math.cos(floatTime * DRIFT_SPEED) * DRIFT_AMPLITUDE;

        // Apply same offset to Frieza, Halo, and DeathBeam so they move together
        if (frieza != null) {
            frieza.setPosition(
                FRIEZA_BASE_X + driftOffset,
                FRIEZA_BASE_Y + bobOffset,
                FRIEZA_BASE_Z
            );
            frieza.update(deltaTime);
        }

        if (halo != null) {
            halo.setPosition(
                FRIEZA_BASE_X + driftOffset,
                FRIEZA_BASE_Y + HALO_Y_OFFSET + bobOffset,
                FRIEZA_BASE_Z
            );
            halo.update(deltaTime);
        }

        if (deathBeam != null) {
            deathBeam.setSpherePosition(
                BEAM_BASE_X + driftOffset,
                BEAM_BASE_Y + bobOffset,
                BEAM_BASE_Z
            );
            // Unstable energy sphere - vibrates and pulses
            float pulse1 = (float) Math.sin(floatTime * 7.3f) * 0.4f;
            float pulse2 = (float) Math.sin(floatTime * 11.7f) * 0.3f;
            float pulse3 = (float) Math.cos(floatTime * 5.1f) * 0.3f;
            float scaleOffset = (pulse1 + pulse2 + pulse3) * SPHERE_PULSE_AMOUNT;
            deathBeam.setSphereScale(SPHERE_BASE_SCALE + scaleOffset);
            deathBeam.update(deltaTime);
        }
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
        if (halo != null) halo.draw();

        if (deathBeam != null) deathBeam.draw();
    }

    // ═══════════════════════════════════════════════════════════════
    // SCREEN SIZE & RELEASE
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void setScreenSize(int width, int height) {
        super.setScreenSize(width, height);
        if (frieza != null) frieza.setScreenSize(width, height);
        if (halo != null) halo.setScreenSize(width, height);
        if (deathBeam != null) deathBeam.setScreenSize(width, height);
    }

    @Override
    protected void releaseSceneSpecificResources() {
        if (frieza != null) { frieza.release(); frieza = null; }
        if (halo != null) { halo.release(); halo = null; }
        if (deathBeam != null) { deathBeam.release(); deathBeam = null; }
        Log.d(TAG, "Frieza Death Beam resources released");
    }

    @Override
    public boolean isReady() {
        return !isDisposed;
    }
}
