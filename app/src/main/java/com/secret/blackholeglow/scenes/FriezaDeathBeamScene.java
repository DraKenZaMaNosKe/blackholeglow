package com.secret.blackholeglow.scenes;

import android.opengl.GLES20;
import android.util.Log;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;
import com.secret.blackholeglow.video.DeathBeamFX;
import com.secret.blackholeglow.video.Frieza3D;
import com.secret.blackholeglow.video.FriezaBackground;

/**
 * FriezaDeathBeamScene - Frieza Final Form disparando Death Beam.
 *
 * Componentes:
 * - FriezaBackground: fondo con warp streaks animados
 * - Frieza3D: modelo OBJ con rim light shader
 * - DeathBeamFX: esfera de energia + rayo conico con shaders animados
 */
public class FriezaDeathBeamScene extends BaseVideoScene {
    private static final String TAG = "FriezaDeathBeam";

    // ═══════════════════════════════════════════════════════════════
    // 🎮 EDIT MODE - tap izquierdo/derecho para rotar dirección
    // ═══════════════════════════════════════════════════════════════
    private static final boolean EDIT_MODE = true;
    private static final float ANGLE_STEP = 0.1745f; // 10 grados en radianes

    private Frieza3D frieza;
    private DeathBeamFX deathBeam;
    private FriezaBackground background;

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
    protected String getVideoFileName() { return null; }

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
            background = new FriezaBackground(context);
            background.setScreenSize(screenWidth, screenHeight);
        } catch (Exception e) {
            Log.w(TAG, "FriezaBackground not ready: " + e.getMessage());
        }

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
        if (background != null) background.update(deltaTime);
        if (frieza != null) frieza.update(deltaTime);
        if (deathBeam != null) deathBeam.update(deltaTime);
    }

    @Override
    protected void drawSceneSpecific() {
        GLES20.glClearColor(0.02f, 0.01f, 0.05f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Background primero (fullscreen quad, sin depth test ni depth write)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthMask(false);
        if (background != null) background.draw();
        GLES20.glDepthMask(true);

        // 3D scene con depth
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        if (frieza != null) frieza.draw();

        if (deathBeam != null) deathBeam.draw();
    }

    // ═══════════════════════════════════════════════════════════════
    // 🎮 TOUCH - Calibración de dirección de warp streaks
    // ═══════════════════════════════════════════════════════════════

    @Override
    public boolean onTouchEvent(float normalizedX, float normalizedY, int action) {
        if (!EDIT_MODE || background == null) return false;
        if (action != 0) return false; // Solo ACTION_DOWN

        float currentAngle = background.getDirectionAngle();

        if (normalizedX < 0) {
            // Tap izquierdo: rotar counter-clockwise
            currentAngle -= ANGLE_STEP;
        } else {
            // Tap derecho: rotar clockwise
            currentAngle += ANGLE_STEP;
        }

        background.setDirectionAngle(currentAngle);

        float degrees = (float) Math.toDegrees(currentAngle);
        Log.d(TAG, String.format("🎯 STREAK ANGLE: %.4f rad (%.1f°)", currentAngle, degrees));

        return true;
    }

    // ═══════════════════════════════════════════════════════════════
    // SCREEN SIZE & RELEASE
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void setScreenSize(int width, int height) {
        super.setScreenSize(width, height);
        if (background != null) background.setScreenSize(width, height);
        if (frieza != null) frieza.setScreenSize(width, height);
        if (deathBeam != null) deathBeam.setScreenSize(width, height);
    }

    @Override
    protected void releaseSceneSpecificResources() {
        if (background != null) { background.release(); background = null; }
        if (frieza != null) { frieza.release(); frieza = null; }
        if (deathBeam != null) { deathBeam.release(); deathBeam = null; }
        Log.d(TAG, "Frieza Death Beam resources released");
    }

    @Override
    public boolean isReady() {
        return !isDisposed;
    }
}
