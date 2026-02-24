package com.secret.blackholeglow.scenes;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import com.secret.blackholeglow.Battery3D;
import com.secret.blackholeglow.Clock3D;
import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;
import com.secret.blackholeglow.image.ImageDownloadManager;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * KenScene - Side-scrolling pixel art fighter with parallax background.
 *
 * Layers (back to front):
 * 1. City background - UV scrolls slowly (parallax)
 * 2. Floor/ground - UV scrolls faster
 * 3. Fighter character - sprite sheet animation (walk + hadouken)
 *
 * Animation loop (~8s):
 * - Walk phase 1 (0-4s): walk cycle + scroll
 * - Hadouken (4-5.5s): hadouken animation + scroll paused
 * - Walk phase 2 (5.5-8s): walk cycle + scroll
 */
public class KenScene extends WallpaperScene {
    private static final String TAG = "KenScene";

    // ═══════════════════════════════════════════════════════════════
    // SHADERS
    // ═══════════════════════════════════════════════════════════════
    private int scrollProgram;   // background & floor (UV scroll + repeat)
    private int spriteProgram;   // character (sprite sheet + alpha clip)

    // Scroll shader locations
    private int scrollPosLoc, scrollUVLoc, scrollTexLoc, scrollOffsetLoc;
    private int scrollCalPosLoc, scrollCalScaleLoc;

    // Sprite shader locations
    private int spritePosLoc, spriteUVLoc, spriteTexLoc;
    private int spriteFrameOffsetLoc, spriteFrameSizeLoc;
    private int spriteCalPosLoc, spriteCalScaleLoc;
    private int spriteTintLoc, spriteAlphaLoc;

    // ═══════════════════════════════════════════════════════════════
    // TEXTURES
    // ═══════════════════════════════════════════════════════════════
    private int bgTextureId = -1;
    private int floorTextureId = -1;
    private int walkTextureId = -1;
    private int hadoukenTextureId = -1;
    private int projectileTextureId = -1;

    // ═══════════════════════════════════════════════════════════════
    // QUAD BUFFERS
    // ═══════════════════════════════════════════════════════════════
    private FloatBuffer bgVertBuf, bgUVBuf;
    private FloatBuffer floorVertBuf, floorUVBuf;
    private FloatBuffer charVertBuf, charUVBuf;

    // ═══════════════════════════════════════════════════════════════
    // ANIMATION
    // ═══════════════════════════════════════════════════════════════
    private float loopTime = 0f;
    private float scrollAccum = 0f;  // accumulated scroll offset
    private float bgUVWidth = 0.32f; // UV width of visible BG window, computed in rebuildBgUV()

    // Sprite sheet: 4 cols x 6 rows, 21 frames used
    private static final int SPRITE_COLS = 4;
    private static final int SPRITE_ROWS = 6;
    private static final int WALK_FRAMES = 21;
    private static final int HADOUKEN_FRAMES = 21;
    private static final float WALK_FPS = 12f;
    private static final float HADOUKEN_FPS = 14f;

    // Scroll speeds (UV units per second) — floor fast (close), bg slow (far) = parallax
    private static final float BG_SCROLL_SPEED = 0.015f;
    private static final float FLOOR_SCROLL_SPEED = 0.35f;

    // Animation phase durations (seconds)
    private static final float WALK1_DURATION = 4.0f;
    private static final float HADOUKEN_DURATION = 1.5f;
    private static final float WALK2_DURATION = 2.5f;
    private static final float LOOP_DURATION = WALK1_DURATION + HADOUKEN_DURATION + WALK2_DURATION;

    // Phase boundaries
    private static final float HADOUKEN_START = WALK1_DURATION;
    private static final float HADOUKEN_END = WALK1_DURATION + HADOUKEN_DURATION;

    // Projectile settings
    private static final float PROJECTILE_LAUNCH = HADOUKEN_START + 0.5f;  // spawns 0.5s into hadouken
    private static final float PROJECTILE_SPEED = 1.8f;   // NDC units per second
    private static final float PROJECTILE_HEIGHT = 0.10f;  // NDC height of projectile quad

    // Projectile state
    private boolean projectileActive = false;
    private float projectileX = 0f;   // current X position in NDC
    private float projectileY = 0f;   // Y position (Ken's hands height)
    private FloatBuffer projectileVertBuf;
    private FloatBuffer projectileUVBuf;

    // ═══════════════════════════════════════════════════════════════
    // UI
    // ═══════════════════════════════════════════════════════════════
    private EqualizerBarsDJ equalizerDJ;
    private Clock3D clock;
    private Battery3D battery;

    // ═══════════════════════════════════════════════════════════════
    // CALIBRATION SYSTEM (tap to cycle mode, drag to adjust)
    // ═══════════════════════════════════════════════════════════════
    private static final boolean CALIBRATION_MODE = false;  // calibración completada

    private float calBgOffX = 0.012f, calBgOffY = 0.90f, calBgScale = 1.434f;
    private float calFloorOffY = 0f, calFloorScaleY = 1.054f;
    private float calKenOffX = -0.164f, calKenOffY = -0.332f, calKenScale = 0.332f;

    private int calMode = 0;
    private static final int CAL_TOTAL = 8;
    private static final String[] CAL_NAMES = {
        "BG POS X", "BG POS Y", "BG SCALE",
        "FLOOR POS Y", "FLOOR SCALE Y",
        "KEN POS X", "KEN POS Y", "KEN SCALE"
    };
    private static final float[] CAL_SENS = {
        0.002f, 0.002f, 0.001f,
        0.002f, 0.001f,
        0.002f, 0.002f, 0.001f
    };

    private float lastTouchY = 0f;
    private boolean isDragging = false;

    // ═══════════════════════════════════════════════════════════════
    // Floor position (NDC): bottom 30% of screen
    // ═══════════════════════════════════════════════════════════════
    private static final float FLOOR_TOP = -0.35f;  // floor goes from -1.0 to -0.35

    // Background image aspect ratio (1737x984 px)
    private static final float BG_IMAGE_ASPECT = 1737f / 984f;

    // Character height in NDC (40% of screen)
    private static final float CHAR_HEIGHT = 0.80f;

    // ═══════════════════════════════════════════════════════════════
    // ABSTRACT IMPLEMENTATIONS
    // ═══════════════════════════════════════════════════════════════

    @Override
    public String getName() { return "KEN"; }

    @Override
    public String getDescription() { return "Ken - Side Scroll Fighter"; }

    @Override
    public int getPreviewResourceId() { return R.drawable.preview_ken; }

    // ═══════════════════════════════════════════════════════════════
    // SETUP
    // ═══════════════════════════════════════════════════════════════

    @Override
    protected void setupScene() {
        Log.d(TAG, "Setting up Ken scene...");

        createScrollShader();
        createSpriteShader();
        createQuadBuffers();
        loadTextures();
        setupUI();

        Log.d(TAG, "Ken scene ready!");
    }

    // --- Scroll shader: UV horizontal scroll with texture repeat ---
    private void createScrollShader() {
        String vs =
            "attribute vec2 aPosition;\n" +
            "attribute vec2 aTexCoord;\n" +
            "uniform vec2 uCalPos;\n" +
            "uniform float uCalScale;\n" +
            "varying vec2 vUV;\n" +
            "void main() {\n" +
            "    vec2 pos = aPosition * uCalScale + uCalPos;\n" +
            "    gl_Position = vec4(pos, 0.0, 1.0);\n" +
            "    vUV = aTexCoord;\n" +
            "}\n";

        String fs =
            "precision mediump float;\n" +
            "uniform sampler2D uTexture;\n" +
            "uniform float uScrollOffset;\n" +
            "varying vec2 vUV;\n" +
            "void main() {\n" +
            "    vec2 uv = vUV;\n" +
            "    uv.x = uv.x + uScrollOffset;\n" +
            "    gl_FragColor = texture2D(uTexture, uv);\n" +
            "}\n";

        scrollProgram = buildProgram(vs, fs);
        scrollPosLoc = GLES20.glGetAttribLocation(scrollProgram, "aPosition");
        scrollUVLoc = GLES20.glGetAttribLocation(scrollProgram, "aTexCoord");
        scrollTexLoc = GLES20.glGetUniformLocation(scrollProgram, "uTexture");
        scrollOffsetLoc = GLES20.glGetUniformLocation(scrollProgram, "uScrollOffset");
        scrollCalPosLoc = GLES20.glGetUniformLocation(scrollProgram, "uCalPos");
        scrollCalScaleLoc = GLES20.glGetUniformLocation(scrollProgram, "uCalScale");
    }

    // --- Sprite shader: sprite sheet with alpha clip ---
    private void createSpriteShader() {
        String vs =
            "attribute vec2 aPosition;\n" +
            "attribute vec2 aTexCoord;\n" +
            "uniform vec2 uCalPos;\n" +
            "uniform float uCalScale;\n" +
            "varying vec2 vUV;\n" +
            "void main() {\n" +
            "    vec2 pos = aPosition * uCalScale + uCalPos;\n" +
            "    gl_Position = vec4(pos, 0.0, 1.0);\n" +
            "    vUV = aTexCoord;\n" +
            "}\n";

        String fs =
            "precision mediump float;\n" +
            "uniform sampler2D uTexture;\n" +
            "uniform vec2 uFrameOffset;\n" +
            "uniform vec2 uFrameSize;\n" +
            "uniform vec3 uTint;\n" +
            "uniform float uAlpha;\n" +
            "varying vec2 vUV;\n" +
            "void main() {\n" +
            "    vec2 uv = vUV * uFrameSize + uFrameOffset;\n" +
            "    vec4 color = texture2D(uTexture, uv);\n" +
            "    if (color.a < 0.3) discard;\n" +
            "    color.rgb = mix(color.rgb, uTint, step(0.01, uTint.r + uTint.g + uTint.b));\n" +
            "    color.a *= uAlpha;\n" +
            "    gl_FragColor = color;\n" +
            "}\n";

        spriteProgram = buildProgram(vs, fs);
        spritePosLoc = GLES20.glGetAttribLocation(spriteProgram, "aPosition");
        spriteUVLoc = GLES20.glGetAttribLocation(spriteProgram, "aTexCoord");
        spriteTexLoc = GLES20.glGetUniformLocation(spriteProgram, "uTexture");
        spriteFrameOffsetLoc = GLES20.glGetUniformLocation(spriteProgram, "uFrameOffset");
        spriteFrameSizeLoc = GLES20.glGetUniformLocation(spriteProgram, "uFrameSize");
        spriteCalPosLoc = GLES20.glGetUniformLocation(spriteProgram, "uCalPos");
        spriteCalScaleLoc = GLES20.glGetUniformLocation(spriteProgram, "uCalScale");
        spriteTintLoc = GLES20.glGetUniformLocation(spriteProgram, "uTint");
        spriteAlphaLoc = GLES20.glGetUniformLocation(spriteProgram, "uAlpha");
    }

    // --- Quad buffers ---
    private void createQuadBuffers() {
        // Background: full screen, UV fills vertically (cover mode)
        bgVertBuf = makeFloatBuffer(new float[]{
            -1f, -1f,  1f, -1f,  -1f, 1f,  1f, 1f
        });
        rebuildBgUV();

        // Floor: bottom strip
        floorVertBuf = makeFloatBuffer(new float[]{
            -1f, -1f,  1f, -1f,  -1f, FLOOR_TOP,  1f, FLOOR_TOP
        });
        floorUVBuf = makeFloatBuffer(new float[]{
            0f, 1f,  4f, 1f,  0f, 0f,  4f, 0f  // 4x repeat horizontally
        });

        // Character: positioned above floor, centered
        float charAspect = 384f / 224f;  // frame aspect ratio
        float screenAspect = (float) screenWidth / screenHeight;
        float charW = CHAR_HEIGHT * charAspect / (screenAspect > 0 ? screenAspect * (16f / 9f) : 1f);
        float charBottom = FLOOR_TOP - CHAR_HEIGHT * 0.55f;
        float charTop = charBottom + CHAR_HEIGHT;
        float charLeft = -charW * 0.5f;
        float charRight = charW * 0.5f;

        charVertBuf = makeFloatBuffer(new float[]{
            charLeft, charBottom,  charRight, charBottom,
            charLeft, charTop,     charRight, charTop
        });
        charUVBuf = makeFloatBuffer(new float[]{
            0f, 1f,  1f, 1f,  0f, 0f,  1f, 0f
        });

        // Projectile: pre-allocate vertex buffer (updated each frame) + UV (static)
        projectileVertBuf = makeFloatBuffer(new float[]{
            0f, 0f,  0f, 0f,  0f, 0f,  0f, 0f  // placeholder, updated in drawProjectile
        });
        projectileUVBuf = makeFloatBuffer(new float[]{
            0f, 1f,  1f, 1f,  0f, 0f,  1f, 0f
        });
    }

    // --- Load textures from Supabase ---
    private void loadTextures() {
        ImageDownloadManager imgMgr = ImageDownloadManager.getInstance(context);

        bgTextureId = downloadAndLoad(imgMgr, "background_ken_scene.png", "Ken Background", GLES20.GL_MIRRORED_REPEAT);
        floorTextureId = downloadAndLoad(imgMgr, "floor_tile.png", "Ken Floor", GLES20.GL_REPEAT);
        walkTextureId = downloadAndLoad(imgMgr, "ken_walk.png", "Ken Walk Sheet", GLES20.GL_CLAMP_TO_EDGE);
        hadoukenTextureId = downloadAndLoad(imgMgr, "ken_hadouken.png", "Ken Hadouken Sheet", GLES20.GL_CLAMP_TO_EDGE);
        projectileTextureId = downloadAndLoad(imgMgr, "hadouken_transparente.png", "Hadouken Projectile", GLES20.GL_CLAMP_TO_EDGE);

        Log.d(TAG, "Textures loaded - BG:" + bgTextureId + " Floor:" + floorTextureId +
                   " Walk:" + walkTextureId + " HK:" + hadoukenTextureId +
                   " Proj:" + projectileTextureId);
    }

    private int downloadAndLoad(ImageDownloadManager mgr, String fileName, String displayName, int wrapS) {
        // Download if not cached
        if (mgr.getImagePath(fileName) == null) {
            Log.d(TAG, "Downloading: " + displayName);
            mgr.downloadImageSync(fileName, p -> {});
        }

        String path = mgr.getImagePath(fileName);
        if (path == null) {
            Log.e(TAG, "Failed to get path for: " + fileName);
            return -1;
        }

        return loadTextureFromFile(path, wrapS);
    }

    private int loadTextureFromFile(String path, int wrapS) {
        File file = new File(path);
        if (!file.exists()) return -1;

        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bmp = BitmapFactory.decodeFile(path, opts);
            if (bmp == null) return -1;

            int[] texIds = new int[1];
            GLES20.glGenTextures(1, texIds, 0);
            int texId = texIds[0];

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, wrapS);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
            bmp.recycle();

            return texId;
        } catch (Exception e) {
            Log.e(TAG, "Texture load error: " + e.getMessage());
            return -1;
        }
    }

    // --- UI setup ---
    private void setupUI() {
        try {
            equalizerDJ = new EqualizerBarsDJ();
            equalizerDJ.initialize();
            equalizerDJ.setTheme(EqualizerBarsDJ.Theme.PYRALIS);
            equalizerDJ.setScreenSize(screenWidth, screenHeight);
        } catch (Exception e) {
            Log.e(TAG, "Equalizer error: " + e.getMessage());
        }

        try {
            clock = new Clock3D(context, Clock3D.THEME_PYRALIS, 0f, 0.8f);
            clock.setShowMilliseconds(false);
        } catch (Exception e) {
            Log.e(TAG, "Clock error: " + e.getMessage());
        }

        try {
            battery = new Battery3D(context, Clock3D.THEME_PYRALIS, 0.81f, -0.34f);
        } catch (Exception e) {
            Log.e(TAG, "Battery error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void update(float deltaTime) {
        if (isPaused || isDisposed) return;

        // Pause animation & scroll when calibrating
        if (!CALIBRATION_MODE) {
            float prevLoopTime = loopTime;
            loopTime += deltaTime;
            if (loopTime >= LOOP_DURATION) {
                loopTime -= LOOP_DURATION;
                projectileActive = false;  // reset on loop
            }

            // Only scroll when NOT in hadouken phase
            boolean isHadouken = loopTime >= HADOUKEN_START && loopTime < HADOUKEN_END;
            if (!isHadouken) {
                scrollAccum += deltaTime;
                // Wrap to prevent float precision loss after hours running
                if (scrollAccum >= 200f) scrollAccum -= 200f;
            }

            // Spawn projectile when Ken "throws" (0.5s into hadouken anim)
            if (!projectileActive && prevLoopTime < PROJECTILE_LAUNCH && loopTime >= PROJECTILE_LAUNCH) {
                projectileActive = true;
                // Spawn at Ken's hands position (right side of Ken sprite)
                // Ken center X in NDC ≈ calKenOffX, hands are to his right
                projectileX = calKenOffX + calKenScale * 0.4f;
                // Ken vertical center ≈ calKenOffY, hands at chest height
                projectileY = calKenOffY - calKenScale * 0.30f;
            }

            // Move projectile to the right
            if (projectileActive) {
                projectileX += PROJECTILE_SPEED * deltaTime;
                // Deactivate when off-screen
                if (projectileX > 1.5f) {
                    projectileActive = false;
                }
            }
        }

        // Update UI
        if (equalizerDJ != null) equalizerDJ.update(deltaTime);
        if (clock != null) clock.update(deltaTime);
        if (battery != null) battery.update(deltaTime);

        super.update(deltaTime);
    }

    // ═══════════════════════════════════════════════════════════════
    // DRAW
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void draw() {
        if (isDisposed) return;

        // Clear
        GLES20.glClearColor(0.02f, 0.02f, 0.08f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // BG scrolls continuously to the left
        float bgOffset = scrollAccum * BG_SCROLL_SPEED + calBgOffX;
        float floorOffset = scrollAccum * FLOOR_SCROLL_SPEED;

        // 1. Draw background (calBgOffY shifts quad, calBgScale scales quad)
        drawScrollQuad(bgTextureId, bgVertBuf, bgUVBuf, bgOffset,
            0f, calBgOffY, calBgScale);

        // 2. Draw floor
        drawScrollQuad(floorTextureId, floorVertBuf, floorUVBuf, floorOffset,
            0f, calFloorOffY, calFloorScaleY);

        // 3. Draw character
        drawCharacter();

        // 4. Draw projectile
        if (projectileActive) {
            drawProjectile();
        }

        // 5. Draw UI
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        if (equalizerDJ != null) equalizerDJ.draw();
        if (clock != null) clock.draw();
        if (battery != null) battery.draw();

        super.draw();
    }

    private void drawScrollQuad(int texId, FloatBuffer verts, FloatBuffer uvs, float offset,
                                float calX, float calY, float calScale) {
        if (texId <= 0 || scrollProgram <= 0) return;

        GLES20.glUseProgram(scrollProgram);

        GLES20.glUniform2f(scrollCalPosLoc, calX, calY);
        GLES20.glUniform1f(scrollCalScaleLoc, calScale);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
        GLES20.glUniform1i(scrollTexLoc, 0);
        GLES20.glUniform1f(scrollOffsetLoc, offset);

        verts.position(0);
        GLES20.glEnableVertexAttribArray(scrollPosLoc);
        GLES20.glVertexAttribPointer(scrollPosLoc, 2, GLES20.GL_FLOAT, false, 0, verts);

        uvs.position(0);
        GLES20.glEnableVertexAttribArray(scrollUVLoc);
        GLES20.glVertexAttribPointer(scrollUVLoc, 2, GLES20.GL_FLOAT, false, 0, uvs);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(scrollPosLoc);
        GLES20.glDisableVertexAttribArray(scrollUVLoc);
    }

    // Afterimage ghost tints (dark blue → blue → purple) and alphas
    private static final float[][] GHOST_TINTS = {
        {0.10f, 0.05f, 0.30f},  // ghost 3 (farthest) — dark indigo
        {0.15f, 0.10f, 0.50f},  // ghost 2 — deep purple
        {0.20f, 0.20f, 0.70f},  // ghost 1 (closest) — blue
    };
    private static final float[] GHOST_ALPHAS = {0.25f, 0.40f, 0.55f};
    private static final float[] GHOST_OFFSETS_X = {-0.09f, -0.06f, -0.03f};  // NDC offset behind Ken


    private void drawCharacter() {
        if (spriteProgram <= 0) return;

        boolean isHadouken = loopTime >= HADOUKEN_START && loopTime < HADOUKEN_END;
        int texId;
        int frameIndex;

        if (isHadouken) {
            texId = hadoukenTextureId;
            float t = (loopTime - HADOUKEN_START) / HADOUKEN_DURATION;
            frameIndex = Math.min((int)(t * HADOUKEN_FRAMES), HADOUKEN_FRAMES - 1);
        } else {
            texId = walkTextureId;
            float walkTime = loopTime < HADOUKEN_START ? loopTime : loopTime - HADOUKEN_DURATION;
            frameIndex = (int)(walkTime * WALK_FPS) % WALK_FRAMES;
        }

        if (texId <= 0) return;

        float frameU = (frameIndex % SPRITE_COLS) / (float) SPRITE_COLS;
        float frameV = (frameIndex / SPRITE_COLS) / (float) SPRITE_ROWS;
        float frameSizeU = 1f / SPRITE_COLS;
        float frameSizeV = 1f / SPRITE_ROWS;

        GLES20.glUseProgram(spriteProgram);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
        GLES20.glUniform1i(spriteTexLoc, 0);
        GLES20.glUniform2f(spriteFrameOffsetLoc, frameU, frameV);
        GLES20.glUniform2f(spriteFrameSizeLoc, frameSizeU, frameSizeV);

        charVertBuf.position(0);
        GLES20.glEnableVertexAttribArray(spritePosLoc);
        GLES20.glVertexAttribPointer(spritePosLoc, 2, GLES20.GL_FLOAT, false, 0, charVertBuf);
        charUVBuf.position(0);
        GLES20.glEnableVertexAttribArray(spriteUVLoc);
        GLES20.glVertexAttribPointer(spriteUVLoc, 2, GLES20.GL_FLOAT, false, 0, charUVBuf);

        // Draw afterimage ghosts during hadouken
        if (isHadouken) {
            for (int i = 0; i < GHOST_TINTS.length; i++) {
                GLES20.glUniform2f(spriteCalPosLoc, calKenOffX + GHOST_OFFSETS_X[i], calKenOffY);
                GLES20.glUniform1f(spriteCalScaleLoc, calKenScale);
                GLES20.glUniform3f(spriteTintLoc, GHOST_TINTS[i][0], GHOST_TINTS[i][1], GHOST_TINTS[i][2]);
                GLES20.glUniform1f(spriteAlphaLoc, GHOST_ALPHAS[i]);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            }
        }

        // Draw Ken (no tint, full alpha)
        GLES20.glUniform2f(spriteCalPosLoc, calKenOffX, calKenOffY);
        GLES20.glUniform1f(spriteCalScaleLoc, calKenScale);
        GLES20.glUniform3f(spriteTintLoc, 0f, 0f, 0f);
        GLES20.glUniform1f(spriteAlphaLoc, 1f);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(spritePosLoc);
        GLES20.glDisableVertexAttribArray(spriteUVLoc);
    }

    /**
     * Draws the hadouken projectile as a simple textured quad.
     * Uses the sprite shader with full-frame UV (no sprite sheet).
     */
    private void drawProjectile() {
        if (projectileTextureId <= 0 || spriteProgram <= 0) return;

        // Build quad at current projectile position
        float aspect = 600f / 342f;  // image aspect ratio
        float h = PROJECTILE_HEIGHT;
        float w = h * aspect * ((float) screenHeight / Math.max(screenWidth, 1));

        float left = projectileX - w * 0.5f;
        float right = projectileX + w * 0.5f;
        float bottom = projectileY - h * 0.5f;
        float top = projectileY + h * 0.5f;

        projectileVertBuf.clear();
        projectileVertBuf.put(new float[]{
            left, bottom,  right, bottom,  left, top,  right, top
        });

        GLES20.glUseProgram(spriteProgram);

        // No calibration offset — position already in NDC
        GLES20.glUniform2f(spriteCalPosLoc, 0f, 0f);
        GLES20.glUniform1f(spriteCalScaleLoc, 1f);
        GLES20.glUniform3f(spriteTintLoc, 0f, 0f, 0f);
        GLES20.glUniform1f(spriteAlphaLoc, 1f);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, projectileTextureId);
        GLES20.glUniform1i(spriteTexLoc, 0);
        // Full image — offset 0,0, size 1,1
        GLES20.glUniform2f(spriteFrameOffsetLoc, 0f, 0f);
        GLES20.glUniform2f(spriteFrameSizeLoc, 1f, 1f);

        projectileVertBuf.position(0);
        GLES20.glEnableVertexAttribArray(spritePosLoc);
        GLES20.glVertexAttribPointer(spritePosLoc, 2, GLES20.GL_FLOAT, false, 0, projectileVertBuf);

        projectileUVBuf.position(0);
        GLES20.glEnableVertexAttribArray(spriteUVLoc);
        GLES20.glVertexAttribPointer(spriteUVLoc, 2, GLES20.GL_FLOAT, false, 0, projectileUVBuf);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(spritePosLoc);
        GLES20.glDisableVertexAttribArray(spriteUVLoc);
    }

    // ═══════════════════════════════════════════════════════════════
    // SCREEN SIZE & MUSIC
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void setScreenSize(int width, int height) {
        super.setScreenSize(width, height);
        if (equalizerDJ != null) equalizerDJ.setScreenSize(width, height);
        // Rebuild quads for correct aspect ratio
        if (width > 0 && height > 0) {
            rebuildBgUV();
            rebuildCharQuad();
        }
    }

    /**
     * Rebuild background UV so image fills screen vertically (cover mode).
     * Shows a narrow window of the wide image that scrolls horizontally.
     */
    private void rebuildBgUV() {
        float screenAspect = screenWidth > 0 && screenHeight > 0 ?
            (float) screenWidth / screenHeight : 9f / 16f;
        // UV width = screen aspect / image aspect → ~0.32 for 9:16 on 16:9 image
        bgUVWidth = screenAspect / BG_IMAGE_ASPECT;
        bgUVBuf = makeFloatBuffer(new float[]{
            0f, 1f,  bgUVWidth, 1f,  0f, 0f,  bgUVWidth, 0f
        });
    }

    private void rebuildCharQuad() {
        float spriteAspect = 384f / 224f;
        float screenAspect = (float) screenWidth / screenHeight;
        // Character should be about 40% of screen height
        // Width = height * spriteAspect * (screenHeight/screenWidth) in NDC
        float charW = CHAR_HEIGHT * spriteAspect * (1f / screenAspect);
        float charBottom = FLOOR_TOP - CHAR_HEIGHT * 0.5f;
        float charTop = charBottom + CHAR_HEIGHT;
        float charLeft = -charW * 0.5f;
        float charRight = charW * 0.5f;

        charVertBuf = makeFloatBuffer(new float[]{
            charLeft, charBottom,  charRight, charBottom,
            charLeft, charTop,     charRight, charTop
        });
    }

    @Override
    public void updateMusicBands(float[] bands) {
        if (equalizerDJ != null && bands != null && bands.length > 0) {
            equalizerDJ.updateFromBands(bands);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // TOUCH / CALIBRATION
    // ═══════════════════════════════════════════════════════════════

    @Override
    public boolean onTouchEvent(float normalizedX, float normalizedY, int action) {
        if (!CALIBRATION_MODE) return false;
        return handleCalibrationTouch(normalizedX, normalizedY, action);
    }

    private boolean handleCalibrationTouch(float x, float y, int action) {
        switch (action) {
            case android.view.MotionEvent.ACTION_DOWN:
                lastTouchY = y;
                isDragging = false;
                return true;

            case android.view.MotionEvent.ACTION_MOVE:
                float dy = y - lastTouchY;
                if (!isDragging && Math.abs(dy * screenHeight) > 10f) {
                    isDragging = true;
                }
                if (isDragging) {
                    float delta = dy * screenHeight * CAL_SENS[calMode];
                    applyCalibrationDelta(delta);
                    lastTouchY = y;
                }
                return true;

            case android.view.MotionEvent.ACTION_UP:
                if (!isDragging) {
                    calMode = (calMode + 1) % CAL_TOTAL;
                    Log.d(TAG, "MODE: [" + calMode + "] " + CAL_NAMES[calMode]);
                }
                logCalibration();
                return true;
        }
        return false;
    }

    private void applyCalibrationDelta(float delta) {
        switch (calMode) {
            case 0: calBgOffX += delta; break;
            case 1: calBgOffY += delta; break;
            case 2: calBgScale += delta; break;
            case 3: calFloorOffY += delta; break;
            case 4: calFloorScaleY += delta; break;
            case 5: calKenOffX += delta; break;
            case 6: calKenOffY += delta; break;
            case 7: calKenScale += delta; break;
        }
    }

    private void logCalibration() {
        Log.d(TAG, "=============================================");
        Log.d(TAG, "  KEN SCENE CALIBRATION  [" + CAL_NAMES[calMode] + "]");
        Log.d(TAG, "---------------------------------------------");
        Log.d(TAG, String.format("  BG:    posX=%.3f posY=%.3f scale=%.3f", calBgOffX, calBgOffY, calBgScale));
        Log.d(TAG, String.format("  FLOOR: posY=%.3f scaleY=%.3f", calFloorOffY, calFloorScaleY));
        Log.d(TAG, String.format("  KEN:   posX=%.3f posY=%.3f scale=%.3f", calKenOffX, calKenOffY, calKenScale));
        Log.d(TAG, "=============================================");
    }

    // ═══════════════════════════════════════════════════════════════
    // RELEASE
    // ═══════════════════════════════════════════════════════════════

    @Override
    protected void releaseSceneResources() {
        // Delete textures
        int[] texIds = { bgTextureId, floorTextureId, walkTextureId, hadoukenTextureId, projectileTextureId };
        for (int id : texIds) {
            if (id > 0) GLES20.glDeleteTextures(1, new int[]{id}, 0);
        }
        bgTextureId = floorTextureId = walkTextureId = hadoukenTextureId = projectileTextureId = -1;

        // Delete shaders
        if (scrollProgram > 0) { GLES20.glDeleteProgram(scrollProgram); scrollProgram = 0; }
        if (spriteProgram > 0) { GLES20.glDeleteProgram(spriteProgram); spriteProgram = 0; }

        // Release UI
        if (equalizerDJ != null) { equalizerDJ.release(); equalizerDJ = null; }
        if (clock != null) { clock.dispose(); clock = null; }
        if (battery != null) { battery.dispose(); battery = null; }

        Log.d(TAG, "Ken scene resources released");
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════════

    private FloatBuffer makeFloatBuffer(float[] data) {
        FloatBuffer buf = ByteBuffer.allocateDirect(data.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
        buf.put(data).position(0);
        return buf;
    }

    private int buildProgram(String vertexSrc, String fragmentSrc) {
        int vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexSrc);
        int fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSrc);
        if (vs == 0 || fs == 0) return 0;

        int prog = GLES20.glCreateProgram();
        GLES20.glAttachShader(prog, vs);
        GLES20.glAttachShader(prog, fs);
        GLES20.glLinkProgram(prog);

        int[] linked = new int[1];
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            Log.e(TAG, "Program link error: " + GLES20.glGetProgramInfoLog(prog));
            GLES20.glDeleteProgram(prog);
            return 0;
        }
        GLES20.glDeleteShader(vs);
        GLES20.glDeleteShader(fs);
        return prog;
    }

    private int compileShader(int type, String source) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, source);
        GLES20.glCompileShader(s);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Shader compile error: " + GLES20.glGetShaderInfoLog(s));
            GLES20.glDeleteShader(s);
            return 0;
        }
        return s;
    }
}
