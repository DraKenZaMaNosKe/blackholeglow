package com.secret.blackholeglow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Calendar;

/**
 * Clock3D — Glyph Atlas Edition
 *
 * Pre-renders all digit/symbol glyphs into ONE texture atlas at init time.
 * Each frame only updates UV coordinates — zero Bitmap allocations, zero GL uploads.
 *
 * Characters in atlas: "0123456789:. AMP" (16 glyphs)
 * Max display length: 16 characters (e.g. "12:34:56.789 AM")
 */
public class Clock3D implements SceneObject {
    private static final String TAG = "Clock3D";

    // ═══════════════════════════════════════════════════════════════
    // TEMAS PREDEFINIDOS
    // ═══════════════════════════════════════════════════════════════
    public static final int THEME_ABYSSIA = 0;
    public static final int THEME_PYRALIS = 1;
    public static final int THEME_KAMEHAMEHA = 2;
    public static final int THEME_SYNTHWAVE = 3;
    public static final int THEME_COSMOS = 4;
    public static final int THEME_WALKING_DEAD = 5;
    public static final int THEME_ZELDA = 6;

    private static final int COLOR_ABYSSIA_PRIMARY = 0xFF00CED1;
    private static final int COLOR_ABYSSIA_GLOW = 0xFF00FFFF;
    private static final int COLOR_PYRALIS_PRIMARY = 0xFFFFD700;
    private static final int COLOR_PYRALIS_GLOW = 0xFFFF8C00;
    private static final int COLOR_KAMEHAMEHA_PRIMARY = 0xFF4FC3F7;
    private static final int COLOR_KAMEHAMEHA_GLOW = 0xFF00E5FF;
    private static final int COLOR_SYNTHWAVE_PRIMARY = 0xFFFF1493;
    private static final int COLOR_SYNTHWAVE_GLOW = 0xFFFF00FF;
    private static final int COLOR_COSMOS_PRIMARY = 0xFFFFD700;
    private static final int COLOR_COSMOS_GLOW = 0xFF9370DB;
    private static final int COLOR_WALKINGDEAD_PRIMARY = 0xFF00CC44;
    private static final int COLOR_WALKINGDEAD_GLOW = 0xFF33FF66;
    private static final int COLOR_ZELDA_PRIMARY = 0xFF4CAF50;
    private static final int COLOR_ZELDA_GLOW = 0xFF8BC34A;

    // ═══════════════════════════════════════════════════════════════
    // ATLAS CONSTANTS
    // ═══════════════════════════════════════════════════════════════
    private static final String ATLAS_CHARS = "0123456789:. AMP";
    private static final int GLYPH_COUNT = ATLAS_CHARS.length(); // 16
    private static final int MAX_DISPLAY_CHARS = 16;

    // ═══════════════════════════════════════════════════════════════
    // OPENGL
    // ═══════════════════════════════════════════════════════════════
    private int programId;
    private int aPositionLoc;
    private int aTexCoordLoc;
    private int uTextureLoc;
    private int uAlphaLoc;
    private int uGlowIntensityLoc;

    // Per-glyph quad buffers (pre-allocated, rewritten each frame)
    private final float[] vertexData = new float[MAX_DISPLAY_CHARS * 8];  // 4 verts * 2 coords
    private final float[] texCoordData = new float[MAX_DISPLAY_CHARS * 8];
    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;
    private ShortBuffer indexBuffer;

    // ═══════════════════════════════════════════════════════════════
    // ATLAS TEXTURA
    // ═══════════════════════════════════════════════════════════════
    private int atlasTextureId = -1;
    private boolean atlasNeedsRebuild = true;

    // UV mapping: for each glyph index, store u0 and u1
    private final float[] glyphU0 = new float[GLYPH_COUNT];
    private final float[] glyphU1 = new float[GLYPH_COUNT];
    // Per-glyph normalized width (relative to cell width, for proportional spacing)
    private final float[] glyphWidthRatio = new float[GLYPH_COUNT];

    // ═══════════════════════════════════════════════════════════════
    // POSICIÓN Y TAMAÑO (NDC: -1 a 1)
    // ═══════════════════════════════════════════════════════════════
    private float x, y, width, height;
    private boolean positionDirty = true;

    // ═══════════════════════════════════════════════════════════════
    // ESTILO
    // ═══════════════════════════════════════════════════════════════
    private Paint textPaint;
    private Paint glowPaint;
    private int primaryColor;
    private int glowColor;
    private float textSize = 72f;
    private float glowIntensity = 1.0f;
    private float glowPulse = 0f;

    // Formato de hora
    private boolean use24HourFormat = true;
    private boolean showSeconds = false;
    private boolean showMilliseconds = false;

    // ═══════════════════════════════════════════════════════════════
    // ZERO-ALLOC TIME FORMATTING
    // ═══════════════════════════════════════════════════════════════
    private final char[] timeChars = new char[MAX_DISPLAY_CHARS];
    private int timeCharCount = 0;
    private final char[] prevTimeChars = new char[MAX_DISPLAY_CHARS];
    private int prevTimeCharCount = 0;
    private final Calendar calendarInstance = Calendar.getInstance();

    // Display state
    private int displayCharCount = 0;

    // ═══════════════════════════════════════════════════════════════
    // SHADERS
    // ═══════════════════════════════════════════════════════════════
    private final String vertexShader =
        "attribute vec2 a_Position;\n" +
        "attribute vec2 a_TexCoord;\n" +
        "varying vec2 v_TexCoord;\n" +
        "void main() {\n" +
        "    v_TexCoord = a_TexCoord;\n" +
        "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
        "}\n";

    private final String fragmentShader =
        "precision mediump float;\n" +
        "varying vec2 v_TexCoord;\n" +
        "uniform sampler2D u_Texture;\n" +
        "uniform float u_Alpha;\n" +
        "uniform float u_GlowIntensity;\n" +
        "void main() {\n" +
        "    vec4 texColor = texture2D(u_Texture, v_TexCoord);\n" +
        "    float brightness = 1.0 + (u_GlowIntensity * 0.3);\n" +
        "    vec3 finalColor = texColor.rgb * brightness;\n" +
        "    gl_FragColor = vec4(finalColor, texColor.a * u_Alpha);\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════

    public Clock3D(Context context, int theme, float x, float y) {
        this.x = x;
        this.y = y;
        this.width = 0.6f;
        this.height = 0.2f;

        applyTheme(theme);
        initShader();
        setupPaints();
        allocateBuffers();

        Log.d(TAG, "Clock3D created (glyph atlas mode)");
    }

    public Clock3D(Context context) {
        this(context, THEME_ABYSSIA, -0.3f, 0.7f);
    }

    // ═══════════════════════════════════════════════════════════════
    // TEMA
    // ═══════════════════════════════════════════════════════════════

    private void applyTheme(int theme) {
        switch (theme) {
            case THEME_PYRALIS:
                primaryColor = COLOR_PYRALIS_PRIMARY;
                glowColor = COLOR_PYRALIS_GLOW;
                break;
            case THEME_KAMEHAMEHA:
                primaryColor = COLOR_KAMEHAMEHA_PRIMARY;
                glowColor = COLOR_KAMEHAMEHA_GLOW;
                break;
            case THEME_SYNTHWAVE:
                primaryColor = COLOR_SYNTHWAVE_PRIMARY;
                glowColor = COLOR_SYNTHWAVE_GLOW;
                break;
            case THEME_COSMOS:
                primaryColor = COLOR_COSMOS_PRIMARY;
                glowColor = COLOR_COSMOS_GLOW;
                break;
            case THEME_WALKING_DEAD:
                primaryColor = COLOR_WALKINGDEAD_PRIMARY;
                glowColor = COLOR_WALKINGDEAD_GLOW;
                break;
            case THEME_ZELDA:
                primaryColor = COLOR_ZELDA_PRIMARY;
                glowColor = COLOR_ZELDA_GLOW;
                break;
            case THEME_ABYSSIA:
            default:
                primaryColor = COLOR_ABYSSIA_PRIMARY;
                glowColor = COLOR_ABYSSIA_GLOW;
                break;
        }
    }

    public void setTheme(int theme) {
        applyTheme(theme);
        if (textPaint != null) textPaint.setColor(primaryColor);
        if (glowPaint != null) glowPaint.setColor(glowColor);
        atlasNeedsRebuild = true;
    }

    public void setColors(int primary, int glow) {
        this.primaryColor = primary;
        this.glowColor = glow;
        if (textPaint != null) textPaint.setColor(primary);
        if (glowPaint != null) glowPaint.setColor(glow);
        atlasNeedsRebuild = true;
    }

    // ═══════════════════════════════════════════════════════════════
    // INIT OPENGL
    // ═══════════════════════════════════════════════════════════════

    private void initShader() {
        programId = ShaderUtils.createProgram(vertexShader, fragmentShader);
        if (programId == 0) {
            Log.e(TAG, "Error creating clock shader");
            return;
        }
        aPositionLoc = GLES30.glGetAttribLocation(programId, "a_Position");
        aTexCoordLoc = GLES30.glGetAttribLocation(programId, "a_TexCoord");
        uTextureLoc = GLES30.glGetUniformLocation(programId, "u_Texture");
        uAlphaLoc = GLES30.glGetUniformLocation(programId, "u_Alpha");
        uGlowIntensityLoc = GLES30.glGetUniformLocation(programId, "u_GlowIntensity");
    }

    private void setupPaints() {
        textPaint = new Paint();
        textPaint.setColor(primaryColor);
        textPaint.setTextSize(textSize);
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);

        glowPaint = new Paint();
        glowPaint.setColor(glowColor);
        glowPaint.setTextSize(textSize);
        glowPaint.setAntiAlias(true);
        glowPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.BOLD));
        glowPaint.setTextAlign(Paint.Align.CENTER);
        glowPaint.setMaskFilter(new BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL));
    }

    private void allocateBuffers() {
        // Vertex buffer: MAX_DISPLAY_CHARS quads * 4 verts * 2 floats
        ByteBuffer vbb = ByteBuffer.allocateDirect(MAX_DISPLAY_CHARS * 8 * 4);
        vbb.order(ByteOrder.nativeOrder());
        vertexBuffer = vbb.asFloatBuffer();

        // TexCoord buffer: same size
        ByteBuffer tbb = ByteBuffer.allocateDirect(MAX_DISPLAY_CHARS * 8 * 4);
        tbb.order(ByteOrder.nativeOrder());
        texCoordBuffer = tbb.asFloatBuffer();

        // Index buffer: MAX_DISPLAY_CHARS quads * 6 indices (two triangles per quad)
        short[] indices = new short[MAX_DISPLAY_CHARS * 6];
        for (int i = 0; i < MAX_DISPLAY_CHARS; i++) {
            int vi = i * 4;
            int ii = i * 6;
            indices[ii]     = (short) vi;
            indices[ii + 1] = (short) (vi + 1);
            indices[ii + 2] = (short) (vi + 2);
            indices[ii + 3] = (short) (vi + 2);
            indices[ii + 4] = (short) (vi + 1);
            indices[ii + 5] = (short) (vi + 3);
        }
        ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 2);
        ibb.order(ByteOrder.nativeOrder());
        indexBuffer = ibb.asShortBuffer();
        indexBuffer.put(indices);
        indexBuffer.position(0);
    }

    // ═══════════════════════════════════════════════════════════════
    // GLYPH ATLAS — built once, rebuilt only on theme/size change
    // ═══════════════════════════════════════════════════════════════

    private void buildAtlas() {
        int glyphPadding = 20; // padding around each glyph for glow bleed

        // Measure each glyph to find cell dimensions
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float maxGlyphWidth = 0;
        float[] measuredWidths = new float[GLYPH_COUNT];
        for (int i = 0; i < GLYPH_COUNT; i++) {
            char c = ATLAS_CHARS.charAt(i);
            measuredWidths[i] = textPaint.measureText(String.valueOf(c));
            if (measuredWidths[i] > maxGlyphWidth) {
                maxGlyphWidth = measuredWidths[i];
            }
        }

        int cellWidth = (int)(maxGlyphWidth + glyphPadding * 2);
        int cellHeight = (int)((fm.descent - fm.ascent) + glyphPadding * 2);

        // Atlas dimensions (single row)
        int atlasWidth = nextPowerOfTwo(cellWidth * GLYPH_COUNT);
        int atlasHeight = nextPowerOfTwo(cellHeight);

        Bitmap atlas = Bitmap.createBitmap(atlasWidth, atlasHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(atlas);
        canvas.drawColor(Color.TRANSPARENT);

        float textY = glyphPadding - fm.ascent;

        for (int i = 0; i < GLYPH_COUNT; i++) {
            char c = ATLAS_CHARS.charAt(i);
            String s = String.valueOf(c);

            float cellX = i * cellWidth;
            float drawX = cellX + cellWidth / 2f;

            // Draw glow layer
            canvas.drawText(s, drawX, textY, glowPaint);
            // Draw sharp text layer
            canvas.drawText(s, drawX, textY, textPaint);

            // Store UV coordinates (normalized)
            glyphU0[i] = cellX / (float) atlasWidth;
            glyphU1[i] = (cellX + cellWidth) / (float) atlasWidth;
            glyphWidthRatio[i] = measuredWidths[i] / maxGlyphWidth;
        }

        // Upload to GPU
        if (atlasTextureId == -1) {
            int[] textures = new int[1];
            GLES30.glGenTextures(1, textures, 0);
            atlasTextureId = textures[0];
        }

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, atlasTextureId);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, atlas, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);

        atlas.recycle();
        atlasNeedsRebuild = false;
        positionDirty = true; // need to recalculate quad positions with new glyph metrics

        Log.d(TAG, "Glyph atlas built: " + atlasWidth + "x" + atlasHeight
                + " (" + GLYPH_COUNT + " glyphs, cell=" + cellWidth + "x" + cellHeight + ")");
    }

    private static int glyphIndex(char c) {
        switch (c) {
            case '0': return 0;
            case '1': return 1;
            case '2': return 2;
            case '3': return 3;
            case '4': return 4;
            case '5': return 5;
            case '6': return 6;
            case '7': return 7;
            case '8': return 8;
            case '9': return 9;
            case ':': return 10;
            case '.': return 11;
            case ' ': return 12;
            case 'A': return 13;
            case 'M': return 14;
            case 'P': return 15;
            default:  return 12; // space for unknown
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ZERO-ALLOC TIME STRING
    // ═══════════════════════════════════════════════════════════════

    private void formatCurrentTime() {
        // Swap prev <-> current for change detection
        System.arraycopy(timeChars, 0, prevTimeChars, 0, timeCharCount);
        prevTimeCharCount = timeCharCount;

        calendarInstance.setTimeInMillis(System.currentTimeMillis());
        int pos = 0;

        int hour;
        if (use24HourFormat) {
            hour = calendarInstance.get(Calendar.HOUR_OF_DAY);
        } else {
            hour = calendarInstance.get(Calendar.HOUR);
            if (hour == 0) hour = 12;
        }
        int minute = calendarInstance.get(Calendar.MINUTE);

        // HH
        timeChars[pos++] = (char)('0' + hour / 10);
        timeChars[pos++] = (char)('0' + hour % 10);
        timeChars[pos++] = ':';
        // MM
        timeChars[pos++] = (char)('0' + minute / 10);
        timeChars[pos++] = (char)('0' + minute % 10);

        if (showSeconds || showMilliseconds) {
            int second = calendarInstance.get(Calendar.SECOND);
            timeChars[pos++] = ':';
            timeChars[pos++] = (char)('0' + second / 10);
            timeChars[pos++] = (char)('0' + second % 10);
        }

        if (showMilliseconds) {
            int millis = calendarInstance.get(Calendar.MILLISECOND);
            timeChars[pos++] = '.';
            timeChars[pos++] = (char)('0' + millis / 100);
            timeChars[pos++] = (char)('0' + (millis / 10) % 10);
            timeChars[pos++] = (char)('0' + millis % 10);
        }

        if (!use24HourFormat) {
            timeChars[pos++] = ' ';
            int ampm = calendarInstance.get(Calendar.AM_PM);
            timeChars[pos++] = (ampm == Calendar.AM) ? 'A' : 'P';
            timeChars[pos++] = 'M';
        }

        timeCharCount = pos;
    }

    private boolean timeChanged() {
        if (timeCharCount != prevTimeCharCount) return true;
        for (int i = 0; i < timeCharCount; i++) {
            if (timeChars[i] != prevTimeChars[i]) return true;
        }
        return false;
    }

    // ═══════════════════════════════════════════════════════════════
    // QUAD GEOMETRY — rebuild positions + UVs into pre-allocated arrays
    // ═══════════════════════════════════════════════════════════════

    private void rebuildQuads() {
        if (timeCharCount == 0) return;

        // Calculate total width of displayed string in NDC
        // Each glyph gets a proportional slice of the total width
        float charSlotWidth = width / timeCharCount;
        float halfH = height / 2f;

        float cursorX = x - width / 2f;

        displayCharCount = timeCharCount;

        for (int i = 0; i < displayCharCount; i++) {
            int gi = glyphIndex(timeChars[i]);

            float left = cursorX;
            float right = cursorX + charSlotWidth;
            float bottom = y - halfH;
            float top = y + halfH;

            int vi = i * 8;
            // Bottom-left
            vertexData[vi]     = left;
            vertexData[vi + 1] = bottom;
            // Bottom-right
            vertexData[vi + 2] = right;
            vertexData[vi + 3] = bottom;
            // Top-left
            vertexData[vi + 4] = left;
            vertexData[vi + 5] = top;
            // Top-right
            vertexData[vi + 6] = right;
            vertexData[vi + 7] = top;

            // UV coords for this glyph in atlas
            float u0 = glyphU0[gi];
            float u1 = glyphU1[gi];
            int ti = i * 8;
            // Bottom-left
            texCoordData[ti]     = u0;
            texCoordData[ti + 1] = 1.0f;
            // Bottom-right
            texCoordData[ti + 2] = u1;
            texCoordData[ti + 3] = 1.0f;
            // Top-left
            texCoordData[ti + 4] = u0;
            texCoordData[ti + 5] = 0.0f;
            // Top-right
            texCoordData[ti + 6] = u1;
            texCoordData[ti + 7] = 0.0f;

            cursorX += charSlotWidth;
        }

        // Bulk copy to NIO buffers
        vertexBuffer.position(0);
        vertexBuffer.put(vertexData, 0, displayCharCount * 8);
        vertexBuffer.position(0);

        texCoordBuffer.position(0);
        texCoordBuffer.put(texCoordData, 0, displayCharCount * 8);
        texCoordBuffer.position(0);

        positionDirty = false;
    }

    // ═══════════════════════════════════════════════════════════════
    // SCENEOBJECT INTERFACE
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void update(float deltaTime) {
        formatCurrentTime();

        // Glow pulse
        glowPulse += deltaTime * 2f;
        if (glowPulse > Math.PI * 2) {
            glowPulse -= Math.PI * 2;
        }
        glowIntensity = 0.7f + 0.3f * (float) Math.sin(glowPulse);

        // Only rebuild quads if time string changed or position moved
        if (timeChanged() || positionDirty) {
            rebuildQuads();
        }
    }

    private boolean visible = true;
    public void setVisible(boolean visible) { this.visible = visible; }

    @Override
    public void draw() {
        if (programId == 0 || !visible || displayCharCount == 0) return;

        // Build atlas on first draw (needs GL context)
        if (atlasNeedsRebuild) {
            buildAtlas();
            rebuildQuads();
        }

        if (atlasTextureId == -1) return;

        GLES30.glUseProgram(programId);

        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, atlasTextureId);
        GLES30.glUniform1i(uTextureLoc, 0);
        GLES30.glUniform1f(uAlphaLoc, 1.0f);
        GLES30.glUniform1f(uGlowIntensityLoc, glowIntensity);

        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        GLES30.glEnableVertexAttribArray(aTexCoordLoc);
        GLES30.glVertexAttribPointer(aTexCoordLoc, 2, GLES30.GL_FLOAT, false, 0, texCoordBuffer);

        // Draw all character quads in one call
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, displayCharCount * 6,
                GLES30.GL_UNSIGNED_SHORT, indexBuffer);

        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glDisableVertexAttribArray(aTexCoordLoc);

        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
    }

    // ═══════════════════════════════════════════════════════════════
    // CONFIGURACIÓN (public API unchanged)
    // ═══════════════════════════════════════════════════════════════

    public void set24HourFormat(boolean use24Hour) {
        this.use24HourFormat = use24Hour;
        positionDirty = true;
    }

    public void setShowSeconds(boolean show) {
        this.showSeconds = show;
        positionDirty = true;
    }

    public void setShowMilliseconds(boolean show) {
        this.showMilliseconds = show;
        if (show) this.showSeconds = true;
        positionDirty = true;
    }

    public void setTextSize(float size) {
        this.textSize = size;
        if (textPaint != null) {
            textPaint.setTextSize(size);
            glowPaint.setTextSize(size);
        }
        atlasNeedsRebuild = true;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        positionDirty = true;
    }

    public void dispose() {
        if (atlasTextureId != -1) {
            int[] textures = {atlasTextureId};
            GLES30.glDeleteTextures(1, textures, 0);
            atlasTextureId = -1;
        }
        if (programId != 0) {
            GLES30.glDeleteProgram(programId);
            programId = 0;
        }
        Log.d(TAG, "Clock3D resources released");
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITY
    // ═══════════════════════════════════════════════════════════════

    private static int nextPowerOfTwo(int value) {
        int result = 1;
        while (result < value) {
            result *= 2;
        }
        return result;
    }
}
