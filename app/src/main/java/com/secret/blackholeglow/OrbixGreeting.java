package com.secret.blackholeglow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║   🤖 OrbixGreeting - Título "Orbix iA"                            ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * Muestra el título "Orbix iA" con efecto de gradiente animado.
 * Optimizado: solo 1 textura GL (título), sin texturas innecesarias.
 *
 * Usa OpenGL ES 3.0
 */
public class OrbixGreeting implements SceneObject {
    private static final String TAG = "OrbixGreeting";

    // Estado
    private boolean isVisible = false;
    private float alpha = 0f;
    private float targetAlpha = 0f;
    private float time = 0f;
    private static final float TIME_CYCLE = 62.831853f;  // 10 * TWO_PI - evita overflow

    // Textura del título (única textura necesaria)
    private int titleTextureId = 0;
    private Bitmap titleBitmap;
    private Canvas titleCanvas;

    // Dimensiones de textura
    private static final int TITLE_TEX_WIDTH = 512;
    private static final int TITLE_TEX_HEIGHT = 64;

    // OpenGL
    private int shaderProgram = 0;
    private int aPositionLoc = -1;
    private int aTexCoordLoc = -1;
    private int uTextureLoc = -1;
    private int uAlphaLoc = -1;
    private int uTimeLoc = -1;
    private FloatBuffer vertexBuffer;

    // ⚡ OPTIMIZACIÓN: Cache de vertices para evitar allocations
    private final float[] vertexCache = new float[16];

    // Posición Y del título
    private float titleY = 0.65f;

    private float aspectRatio = 1.0f;
    private boolean needsTitleUpdate = true;

    public OrbixGreeting(Context context) {
        initBitmaps();
        initOpenGL();
    }

    public OrbixGreeting() {
        initBitmaps();
        initOpenGL();
    }

    private void initBitmaps() {
        // Solo crear bitmap del título (único elemento que se dibuja)
        titleBitmap = Bitmap.createBitmap(TITLE_TEX_WIDTH, TITLE_TEX_HEIGHT, Bitmap.Config.ARGB_8888);
        titleCanvas = new Canvas(titleBitmap);
    }

    private void initOpenGL() {
        // Vertex shader with breathing animation
        String vertexShader =
            "#version 300 es\n" +
            "precision mediump float;\n" +
            "in vec2 a_Position;\n" +
            "in vec2 a_TexCoord;\n" +
            "out vec2 v_TexCoord;\n" +
            "uniform float u_Time;\n" +
            "void main() {\n" +
            "    v_TexCoord = a_TexCoord;\n" +
            "    float breath = 0.985 + sin(u_Time * 1.5) * 0.015;\n" +
            "    vec2 pos = a_Position * breath;\n" +
            "    gl_Position = vec4(pos, 0.0, 1.0);\n" +
            "}\n";

        // Fragment shader with halo glow, scan line, edge glow, improved gradient
        String fragmentShader =
            "#version 300 es\n" +
            "precision mediump float;\n" +
            "in vec2 v_TexCoord;\n" +
            "out vec4 fragColor;\n" +
            "uniform sampler2D u_Texture;\n" +
            "uniform float u_Alpha;\n" +
            "uniform float u_Time;\n" +
            "\n" +
            "void main() {\n" +
            "    vec4 texColor = texture(u_Texture, v_TexCoord);\n" +
            "    float pulse = sin(u_Time * 2.0) * 0.1 + 0.9;\n" +
            "\n" +
            // Halo glow: sample neighbors for alpha spread
            "    vec2 texel = vec2(1.0 / 512.0, 1.0 / 64.0);\n" +
            "    float halo = 0.0;\n" +
            "    halo += texture(u_Texture, v_TexCoord + vec2( texel.x * 2.0, 0.0)).a;\n" +
            "    halo += texture(u_Texture, v_TexCoord + vec2(-texel.x * 2.0, 0.0)).a;\n" +
            "    halo += texture(u_Texture, v_TexCoord + vec2(0.0,  texel.y * 2.0)).a;\n" +
            "    halo += texture(u_Texture, v_TexCoord + vec2(0.0, -texel.y * 2.0)).a;\n" +
            "    halo = clamp(halo * 0.25, 0.0, 1.0);\n" +
            "\n" +
            // Improved gradient: branchless with smoothstep blending
            "    float shift = sin(v_TexCoord.x * 3.14159 + u_Time) * 0.5 + 0.5;\n" +
            "    vec3 cyan = vec3(0.0, 1.0, 1.0);\n" +
            "    vec3 magenta = vec3(1.0, 0.4, 1.0);\n" +
            "    vec3 purple = vec3(0.6, 0.2, 1.0);\n" +
            "    vec3 gradientColor = mix(cyan, magenta, smoothstep(0.0, 0.5, shift));\n" +
            "    gradientColor = mix(gradientColor, purple, smoothstep(0.5, 1.0, shift));\n" +
            "\n" +
            // Scan line: horizontal sweep
            "    float scanPos = fract(u_Time * 0.3);\n" +
            "    float scanLine = smoothstep(scanPos - 0.05, scanPos, v_TexCoord.x)\n" +
            "                   - smoothstep(scanPos, scanPos + 0.05, v_TexCoord.x);\n" +
            "    scanLine *= 0.4;\n" +
            "\n" +
            // Edge glow: bright on text edges
            "    float edgeGlow = clamp(halo - texColor.a, 0.0, 1.0) * 0.6;\n" +
            "\n" +
            // Combine
            "    vec3 finalColor = gradientColor * texColor.rgb * pulse;\n" +
            "    finalColor += gradientColor * texColor.a * 0.2;\n" +
            "    finalColor += cyan * halo * 0.15;\n" +
            "    finalColor += vec3(1.0) * scanLine * texColor.a;\n" +
            "    finalColor += gradientColor * edgeGlow;\n" +
            "    float finalAlpha = max(texColor.a, halo * 0.3) * u_Alpha;\n" +
            "    fragColor = vec4(finalColor, finalAlpha);\n" +
            "}\n";

        int vs = compileShader(GLES30.GL_VERTEX_SHADER, vertexShader);
        int fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShader);

        if (vs == 0 || fs == 0) {
            Log.e(TAG, "Error compilando shaders GL3.0");
            return;
        }

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vs);
        GLES30.glAttachShader(shaderProgram, fs);
        GLES30.glLinkProgram(shaderProgram);

        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(shaderProgram, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Error linking: " + GLES30.glGetProgramInfoLog(shaderProgram));
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
            return;
        }

        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        aTexCoordLoc = GLES30.glGetAttribLocation(shaderProgram, "a_TexCoord");
        uTextureLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Texture");
        uAlphaLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Alpha");
        uTimeLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Time");

        float[] vertices = {
            -1.0f, -1.0f,   0.0f, 1.0f,
             1.0f, -1.0f,   1.0f, 1.0f,
            -1.0f,  1.0f,   0.0f, 0.0f,
             1.0f,  1.0f,   1.0f, 0.0f
        };

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // Crear solo 1 textura (antes: 6)
        int[] textures = new int[1];
        GLES30.glGenTextures(1, textures, 0);
        titleTextureId = textures[0];
        setupTexture(titleTextureId);

        Log.d(TAG, "OrbixGreeting GL3.0 inicializado (1 textura)");
    }

    private void setupTexture(int textureId) {
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
    }

    private int compileShader(int type, String code) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, code);
        GLES30.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Shader error: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private void updateTitleTexture() {
        if (!needsTitleUpdate || titleBitmap == null) return;

        titleBitmap.eraseColor(0x00000000);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(34);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint glowPaint = new Paint(textPaint);
        glowPaint.setColor(0xFF00FFFF);
        glowPaint.setMaskFilter(new android.graphics.BlurMaskFilter(6, android.graphics.BlurMaskFilter.Blur.NORMAL));

        float centerX = TITLE_TEX_WIDTH / 2f;
        float centerY = TITLE_TEX_HEIGHT / 2f + 12;

        titleCanvas.drawText("Orbix iA", centerX, centerY, glowPaint);
        titleCanvas.drawText("Orbix iA", centerX, centerY, textPaint);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, titleTextureId);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, titleBitmap, 0);

        needsTitleUpdate = false;
    }

    @Override
    public void update(float dt) {
        time += dt;
        if (time > TIME_CYCLE) {
            time -= TIME_CYCLE;
        }

        float alphaSpeed = 4.0f;
        if (alpha < targetAlpha) {
            alpha = Math.min(alpha + dt * alphaSpeed, targetAlpha);
        } else if (alpha > targetAlpha) {
            alpha = Math.max(alpha - dt * alphaSpeed, targetAlpha);
        }
    }

    @Override
    public void draw() {
        if (alpha <= 0.01f || shaderProgram == 0 || vertexBuffer == null) return;

        updateTitleTexture();

        GLES30.glUseProgram(shaderProgram);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glEnableVertexAttribArray(aTexCoordLoc);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glUniform1i(uTextureLoc, 0);
        GLES30.glUniform1f(uAlphaLoc, alpha);
        GLES30.glUniform1f(uTimeLoc, time);

        // Título "Orbix iA"
        drawTextQuad(titleTextureId, titleY, 0.38f, 0.05f);

        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glDisableVertexAttribArray(aTexCoordLoc);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        GLES30.glUseProgram(0);
    }

    private void drawTextQuad(int textureId, float posY, float width, float height) {
        float halfWidth = width / aspectRatio;
        float halfHeight = height;

        vertexCache[0] = -halfWidth; vertexCache[1] = posY - halfHeight; vertexCache[2] = 0.0f; vertexCache[3] = 1.0f;
        vertexCache[4] = halfWidth;  vertexCache[5] = posY - halfHeight; vertexCache[6] = 1.0f; vertexCache[7] = 1.0f;
        vertexCache[8] = -halfWidth; vertexCache[9] = posY + halfHeight; vertexCache[10] = 0.0f; vertexCache[11] = 0.0f;
        vertexCache[12] = halfWidth; vertexCache[13] = posY + halfHeight; vertexCache[14] = 1.0f; vertexCache[15] = 0.0f;

        vertexBuffer.clear();
        vertexBuffer.put(vertexCache);
        vertexBuffer.position(0);

        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer);

        vertexBuffer.position(2);
        GLES30.glVertexAttribPointer(aTexCoordLoc, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
    }

    public void show() {
        isVisible = true;
        targetAlpha = 1.0f;
    }

    public void hide() {
        isVisible = false;
        targetAlpha = 0.0f;
    }

    public void setAspectRatio(float aspect) {
        this.aspectRatio = aspect;
    }

    public boolean isVisible() {
        return isVisible && alpha > 0.01f;
    }

    public void dispose() {
        if (titleTextureId != 0) {
            GLES30.glDeleteTextures(1, new int[]{titleTextureId}, 0);
            titleTextureId = 0;
        }
        if (titleBitmap != null) {
            titleBitmap.recycle();
            titleBitmap = null;
        }
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        Log.d(TAG, "OrbixGreeting disposed");
    }
}
