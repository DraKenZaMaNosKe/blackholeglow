package com.secret.blackholeglow.scenes;

import android.opengl.GLES30;
import android.util.Log;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;
import com.secret.blackholeglow.video.BlackCat3D;
import com.secret.blackholeglow.video.BrickWall3D;
import com.secret.blackholeglow.video.BuildingsSilhouette2D;
import com.secret.blackholeglow.video.NightSkyRenderer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║           🌙 MOONLIT CAT SCENE - Gato Bajo la Luna                      ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Escena 3D completa SIN video:                                          ║
 * ║  • Cielo nocturno procedural con estrellas titilantes                   ║
 * ║  • Luna gigante con glow                                                ║
 * ║  • Siluetas de edificios con ventanas encendidas                        ║
 * ║  • Gato negro sentado en barda de ladrillos (modelo 3D, Meshy AI)       ║
 * ║  • Barda de ladrillos (modelo 3D, Meshy AI)                             ║
 * ║                                                                          ║
 * ║  Hereda de BaseVideoScene para obtener Clock3D + Battery3D +            ║
 * ║  EqualizerBarsDJ gratis, pero getVideoFileName() retorna null           ║
 * ║  para que NO se inicialice el video renderer.                           ║
 * ║                                                                          ║
 * ║  El fondo es un shader fullscreen (NightSkyRenderer) que reemplaza      ║
 * ║  la función del video.                                                  ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class MoonlitCatScene extends BaseVideoScene {
    private static final String TAG = "MoonlitCatScene";

    // Scene-specific objects
    private NightSkyRenderer nightSky;
    private BuildingsSilhouette2D buildings;
    private BlackCat3D cat;
    private BrickWall3D wall;

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎮 TOUCH EDITING SYSTEM - TAP TO CYCLE, DRAG TO ADJUST
    // ═══════════════════════════════════════════════════════════════════════════
    private static final boolean EDIT_MODE = true;

    // Modos de calibración completa del gato
    // 0: Rotation X    4: Position X
    // 1: Rotation Y    5: Position Y
    // 2: Rotation Z    6: Position Z
    // 3: Scale
    private int editMode = 1;  // Start on Rotation Y (most common)
    private static final int TOTAL_MODES = 7;
    private static final String[] MODE_NAMES = {
        "🐱 ROT X",
        "🐱 ROT Y",
        "🐱 ROT Z",
        "🐱 SCALE",
        "🐱 POS X",
        "🐱 POS Y",
        "🐱 POS Z"
    };

    // Touch tracking
    private float lastTouchY = 0f;
    private boolean isDragging = false;

    // ═══════════════════════════════════════════════════════════════════════════
    // ABSTRACT METHOD IMPLEMENTATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public String getName() {
        return "MOONLIT_CAT";
    }

    @Override
    public String getDescription() {
        return "Moonlit Cat - Gato bajo la luna";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_moonlit_cat;
    }

    /**
     * No video - this scene uses a shader-based night sky instead.
     * BaseVideoScene handles null gracefully (skips video init).
     */
    @Override
    protected String getVideoFileName() {
        return null;
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.ABYSSIA;  // Blues/turquoise for night atmosphere
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCENE-SPECIFIC SETUP
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    protected void setupSceneSpecific() {
        Log.d(TAG, "🌙 Setting up Moonlit Cat scene...");

        // 1. Night sky renderer (fullscreen shader background)
        nightSky = new NightSkyRenderer(context);
        nightSky.initialize(screenWidth, screenHeight);

        // 2. Buildings silhouette (2D layer behind cat/wall)
        try {
            buildings = new BuildingsSilhouette2D(context);
            buildings.initialize();
            buildings.setYOffset(0.236f);
            buildings.setHeight(0.596f);
        } catch (Exception e) {
            Log.w(TAG, "⏳ Buildings layer not ready: " + e.getMessage());
        }

        // 3. Brick wall (static, bottom of screen)
        try {
            wall = new BrickWall3D(context);
            wall.setScreenSize(screenWidth, screenHeight);
            wall.setPosition(-0.008f, -0.538f, 1.720f);
            wall.setScale(0.332f);
            wall.setRotationY(-13.2f);
        } catch (Exception e) {
            Log.w(TAG, "⏳ BrickWall3D not ready (models pending): " + e.getMessage());
        }

        // 4. Black cat (sitting on wall)
        try {
            cat = new BlackCat3D(context);
            cat.setScreenSize(screenWidth, screenHeight);
            cat.setPosition(-0.184f, -0.090f, 1.672f);
            cat.setScale(0.118f);
            cat.setRotationY(42.6f);
        } catch (Exception e) {
            Log.w(TAG, "⏳ BlackCat3D not ready (models pending): " + e.getMessage());
        }

        Log.d(TAG, "🌙 Moonlit Cat scene ready!");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    protected void updateSceneSpecific(float deltaTime) {
        if (nightSky != null) nightSky.update(deltaTime);
        if (cat != null) cat.update(deltaTime);
        if (wall != null) wall.update(deltaTime);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎮 TOUCH HANDLING - TAP TO CYCLE, DRAG TO EDIT
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public boolean onTouchEvent(float normalizedX, float normalizedY, int action) {
        if (!EDIT_MODE) return false;

        float touchY = normalizedY * screenHeight;

        switch (action) {
            case 0: // ACTION_DOWN
                lastTouchY = touchY;
                isDragging = false;
                return true;

            case 2: // ACTION_MOVE
                float dy = touchY - lastTouchY;

                // Si movió más de 10px, es drag
                if (Math.abs(dy) > 10) {
                    isDragging = true;
                    applyDragToCurrentMode(-dy);  // Negativo: arriba = aumentar
                    lastTouchY = touchY;
                }
                return true;

            case 1: // ACTION_UP
                if (!isDragging) {
                    // Fue un TAP - cambiar al siguiente modo
                    editMode = (editMode + 1) % TOTAL_MODES;
                    Log.d(TAG, "═══════════════════════════════════════════════════");
                    Log.d(TAG, "🎮 MODO: " + MODE_NAMES[editMode] + " (arrastra arriba/abajo para cambiar)");
                    Log.d(TAG, "═══════════════════════════════════════════════════");
                }
                // Siempre logear posiciones al soltar
                logCurrentPositions();
                return true;

            default:
                return false;
        }
    }

    private void applyDragToCurrentMode(float delta) {
        if (cat == null) return;

        String valueStr = "";

        switch (editMode) {
            case 0: // Rotation X
                cat.setRotationX(cat.getRotationX() + delta * 0.3f);
                valueStr = String.format("%.1f°", cat.getRotationX());
                break;
            case 1: // Rotation Y
                cat.setRotationY(cat.getRotationY() + delta * 0.3f);
                valueStr = String.format("%.1f°", cat.getRotationY());
                break;
            case 2: // Rotation Z
                cat.setRotationZ(cat.getRotationZ() + delta * 0.3f);
                valueStr = String.format("%.1f°", cat.getRotationZ());
                break;
            case 3: // Scale
                float newScale = Math.max(0.01f, cat.getScale() + delta * 0.001f);
                cat.setScale(newScale);
                valueStr = String.format("%.3f", cat.getScale());
                break;
            case 4: // Position X
                cat.setPosition(cat.getPosX() + delta * 0.002f, cat.getPosY(), cat.getPosZ());
                valueStr = String.format("%.3f", cat.getPosX());
                break;
            case 5: // Position Y
                cat.setPosition(cat.getPosX(), cat.getPosY() + delta * 0.002f, cat.getPosZ());
                valueStr = String.format("%.3f", cat.getPosY());
                break;
            case 6: // Position Z
                cat.setPosition(cat.getPosX(), cat.getPosY(), cat.getPosZ() + delta * 0.002f);
                valueStr = String.format("%.3f", cat.getPosZ());
                break;
        }

        Log.d(TAG, "🎮 " + MODE_NAMES[editMode] + " = " + valueStr);
    }

    private void logCurrentPositions() {
        Log.d(TAG, "╔═══════════════════════════════════════════════════════════════════╗");
        Log.d(TAG, "║  🐱 CALIBRACIÓN DEL GATO                                         ║");
        Log.d(TAG, "╠═══════════════════════════════════════════════════════════════════╣");
        if (cat != null) {
            Log.d(TAG, String.format("║  🔄 ROT:   X=%.1f° Y=%.1f° Z=%.1f°",
                cat.getRotationX(), cat.getRotationY(), cat.getRotationZ()));
            Log.d(TAG, String.format("║  📏 SCALE: %.3f", cat.getScale()));
            Log.d(TAG, String.format("║  📍 POS:   (%.3f, %.3f, %.3f)",
                cat.getPosX(), cat.getPosY(), cat.getPosZ()));
        }
        Log.d(TAG, "╚═══════════════════════════════════════════════════════════════════╝");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DRAW
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    protected void drawSceneSpecific() {
        // 1. Night sky (fullscreen shader - replaces video background)
        //    Draw WITHOUT depth test (fullscreen quad covers everything)
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        if (nightSky != null) {
            nightSky.draw();
        }

        // 2. Buildings silhouette (2D layer with transparency)
        //    Still no depth test (2D overlay on sky)
        if (buildings != null) {
            buildings.draw();
        }

        // 3. 3D models ON TOP of buildings, WITH depth test
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        if (wall != null) wall.draw();
        if (cat != null) cat.draw();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCREEN SIZE
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void setScreenSize(int width, int height) {
        super.setScreenSize(width, height);
        if (nightSky != null) nightSky.setScreenSize(width, height);
        if (cat != null) cat.setScreenSize(width, height);
        if (wall != null) wall.setScreenSize(width, height);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // RELEASE
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    protected void releaseSceneSpecificResources() {
        if (nightSky != null) {
            nightSky.release();
            nightSky = null;
        }
        if (buildings != null) {
            buildings.release();
            buildings = null;
        }
        if (cat != null) {
            cat.release();
            cat = null;
        }
        if (wall != null) {
            wall.release();
            wall = null;
        }
        Log.d(TAG, "🌙 Moonlit Cat resources released");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // isReady override - no video to wait for
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public boolean isReady() {
        // No video dependency - scene is ready as soon as shaders compile
        return !isDisposed;
    }
}
