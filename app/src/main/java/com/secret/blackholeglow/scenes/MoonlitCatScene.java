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
    // 🎮 TOUCH EDITING SYSTEM - TAP TO CYCLE MODE
    // ═══════════════════════════════════════════════════════════════════════════
    private static final boolean EDIT_MODE = false;  // ✅ Posiciones finales fijadas 2026-02-05

    // Modo de edición actual (0-14, tap para cambiar)
    // 0-4:   Cat (X, Y, Z, Scale, Rot)
    // 5-9:   Wall (X, Y, Z, Scale, Rot)
    // 10-12: Moon (X, Y, Radius)
    // 13-14: Buildings (yOffset, height)
    private int editMode = 10;  // Empezar en luna
    private static final int TOTAL_MODES = 15;
    private static final String[] MODE_NAMES = {
        "🐱 CAT X", "🐱 CAT Y", "🐱 CAT Z", "🐱 CAT SCALE", "🐱 CAT ROT",
        "🧱 WALL X", "🧱 WALL Y", "🧱 WALL Z", "🧱 WALL SCALE", "🧱 WALL ROT",
        "🌙 MOON X", "🌙 MOON Y", "🌙 MOON RADIUS",
        "🏘️ BLDG OFFSET", "🏘️ BLDG HEIGHT"
    };

    // Touch tracking
    private float lastTouchY = 0f;
    private boolean isDragging = false;

    // Sensibilidades (ajustables)
    private static final float POS_SENSITIVITY = 0.002f;
    private static final float SCALE_SENSITIVITY = 0.002f;
    private static final float ROT_SENSITIVITY = 0.3f;
    private static final float MOON_POS_SENSITIVITY = 0.0005f;
    private static final float MOON_RADIUS_SENSITIVITY = 0.0002f;
    private static final float BLDG_SENSITIVITY = 0.001f;

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
        String valueStr = "";

        switch (editMode) {
            // === GATO ===
            case 0: // Cat X
                if (cat != null) {
                    cat.setPosition(cat.getPosX() + delta * POS_SENSITIVITY, cat.getPosY(), cat.getPosZ());
                    valueStr = String.format("%.3f", cat.getPosX());
                }
                break;
            case 1: // Cat Y
                if (cat != null) {
                    cat.setPosition(cat.getPosX(), cat.getPosY() + delta * POS_SENSITIVITY, cat.getPosZ());
                    valueStr = String.format("%.3f", cat.getPosY());
                }
                break;
            case 2: // Cat Z
                if (cat != null) {
                    cat.setPosition(cat.getPosX(), cat.getPosY(), cat.getPosZ() + delta * POS_SENSITIVITY);
                    valueStr = String.format("%.3f", cat.getPosZ());
                }
                break;
            case 3: // Cat Scale
                if (cat != null) {
                    float newScale = Math.max(0.01f, cat.getScale() + delta * SCALE_SENSITIVITY);
                    cat.setScale(newScale);
                    valueStr = String.format("%.3f", cat.getScale());
                }
                break;
            case 4: // Cat Rotation
                if (cat != null) {
                    cat.setRotationY(cat.getRotationY() + delta * ROT_SENSITIVITY);
                    valueStr = String.format("%.1f°", cat.getRotationY());
                }
                break;

            // === PARED ===
            case 5: // Wall X
                if (wall != null) {
                    wall.setPosition(wall.getPosX() + delta * POS_SENSITIVITY, wall.getPosY(), wall.getPosZ());
                    valueStr = String.format("%.3f", wall.getPosX());
                }
                break;
            case 6: // Wall Y
                if (wall != null) {
                    wall.setPosition(wall.getPosX(), wall.getPosY() + delta * POS_SENSITIVITY, wall.getPosZ());
                    valueStr = String.format("%.3f", wall.getPosY());
                }
                break;
            case 7: // Wall Z
                if (wall != null) {
                    wall.setPosition(wall.getPosX(), wall.getPosY(), wall.getPosZ() + delta * POS_SENSITIVITY);
                    valueStr = String.format("%.3f", wall.getPosZ());
                }
                break;
            case 8: // Wall Scale
                if (wall != null) {
                    float newScale = Math.max(0.01f, wall.getScale() + delta * SCALE_SENSITIVITY);
                    wall.setScale(newScale);
                    valueStr = String.format("%.3f", wall.getScale());
                }
                break;
            case 9: // Wall Rotation
                if (wall != null) {
                    wall.setRotationY(wall.getRotationY() + delta * ROT_SENSITIVITY);
                    valueStr = String.format("%.1f°", wall.getRotationY());
                }
                break;

            // === LUNA ===
            case 10: // Moon X
                if (nightSky != null) {
                    nightSky.setMoonPosition(nightSky.getMoonPosX() + delta * MOON_POS_SENSITIVITY, nightSky.getMoonPosY());
                    valueStr = String.format("%.3f", nightSky.getMoonPosX());
                }
                break;
            case 11: // Moon Y
                if (nightSky != null) {
                    nightSky.setMoonPosition(nightSky.getMoonPosX(), nightSky.getMoonPosY() + delta * MOON_POS_SENSITIVITY);
                    valueStr = String.format("%.3f", nightSky.getMoonPosY());
                }
                break;
            case 12: // Moon Radius
                if (nightSky != null) {
                    float newRadius = Math.max(0.02f, nightSky.getMoonRadius() + delta * MOON_RADIUS_SENSITIVITY);
                    nightSky.setMoonRadius(newRadius);
                    valueStr = String.format("%.4f", nightSky.getMoonRadius());
                }
                break;

            // === EDIFICIOS ===
            case 13: // Buildings Y Offset
                if (buildings != null) {
                    float newOffset = Math.max(0f, Math.min(0.5f, buildings.getYOffset() + delta * BLDG_SENSITIVITY));
                    buildings.setYOffset(newOffset);
                    valueStr = String.format("%.3f", buildings.getYOffset());
                }
                break;
            case 14: // Buildings Height
                if (buildings != null) {
                    float newHeight = Math.max(0.1f, Math.min(0.9f, buildings.getHeight() + delta * BLDG_SENSITIVITY));
                    buildings.setHeight(newHeight);
                    valueStr = String.format("%.3f", buildings.getHeight());
                }
                break;
        }

        // Log del valor actual mientras arrastras
        Log.d(TAG, "🎮 " + MODE_NAMES[editMode] + " = " + valueStr);
    }

    private void logCurrentPositions() {
        Log.d(TAG, "╔══════════════════════════════════════════════════════════════════════════╗");
        Log.d(TAG, "║  📍 POSICIONES ACTUALES - Copia estos valores al código:                 ║");
        Log.d(TAG, "╠══════════════════════════════════════════════════════════════════════════╣");
        if (cat != null) {
            Log.d(TAG, String.format("║  🐱 CAT:  pos(%.3f, %.3f, %.3f) scale=%.3f rot=%.1f",
                cat.getPosX(), cat.getPosY(), cat.getPosZ(), cat.getScale(), cat.getRotationY()));
        }
        if (wall != null) {
            Log.d(TAG, String.format("║  🧱 WALL: pos(%.3f, %.3f, %.3f) scale=%.3f rot=%.1f",
                wall.getPosX(), wall.getPosY(), wall.getPosZ(), wall.getScale(), wall.getRotationY()));
        }
        if (nightSky != null) {
            Log.d(TAG, String.format("║  🌙 MOON: pos(%.3f, %.3f) radius=%.4f",
                nightSky.getMoonPosX(), nightSky.getMoonPosY(), nightSky.getMoonRadius()));
        }
        if (buildings != null) {
            Log.d(TAG, String.format("║  🏘️ BLDG: yOffset=%.3f height=%.3f",
                buildings.getYOffset(), buildings.getHeight()));
        }
        Log.d(TAG, "╚══════════════════════════════════════════════════════════════════════════╝");
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
