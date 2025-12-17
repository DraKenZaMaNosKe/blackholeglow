package com.secret.blackholeglow;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * CHRISTMAS TITLE - Texto festivo navideño
 *         FELIZ
 *        NAVIDAD
 *          ❄
 */
public class ChristmasTitle implements SceneObject {
    private static final String TAG = "ChristmasTitle";

    private int titleTextureId = 0;
    private Bitmap titleBitmap;
    private Canvas titleCanvas;

    private static final int TEX_WIDTH = 512;
    private static final int TEX_HEIGHT = 280;

    private int shaderProgram = 0;
    private FloatBuffer vertexBuffer;
    private int aPositionLoc, aTexCoordLoc;
    private int uTextureLoc, uAlphaLoc, uTimeLoc;

    private float alpha = 1.0f;
    private float time = 0f;
    private float aspectRatio = 1.0f;
    private boolean initialized = false;

    private float posY = 0.55f;
    private float width = 0.65f;
    private float height = 0.26f;

    public ChristmasTitle() {
        Log.d(TAG, "Creating ChristmasTitle...");
        initBitmap();
        initOpenGL();
        renderTitle();
    }

    private void initBitmap() {
        titleBitmap = Bitmap.createBitmap(TEX_WIDTH, TEX_HEIGHT, Bitmap.Config.ARGB_8888);
        titleCanvas = new Canvas(titleBitmap);
    }

    private void initOpenGL() {
        String vertexShader =
            "attribute vec2 a_Position;\n" +
            "attribute vec2 a_TexCoord;\n" +
            "varying vec2 v_TexCoord;\n" +
            "void main() {\n" +
            "    v_TexCoord = a_TexCoord;\n" +
            "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
            "}\n";

        // Shader con efecto de brillo navideño (rojo/verde alternando)
        String fragmentShader =
            "precision mediump float;\n" +
            "varying vec2 v_TexCoord;\n" +
            "uniform sampler2D u_Texture;\n" +
            "uniform float u_Alpha;\n" +
            "uniform float u_Time;\n" +
            "void main() {\n" +
            "    vec4 texColor = texture2D(u_Texture, v_TexCoord);\n" +
            "    // Pulso suave de brillo\n" +
            "    float pulse = sin(u_Time * 1.5) * 0.12 + 1.0;\n" +
            "    // Tinte navideño que alterna rojo/verde\n" +
            "    float colorShift = sin(u_Time * 0.8) * 0.5 + 0.5;\n" +
            "    vec3 redTint = vec3(1.0, 0.85, 0.85);\n" +
            "    vec3 greenTint = vec3(0.85, 1.0, 0.85);\n" +
            "    vec3 tint = mix(redTint, greenTint, colorShift);\n" +
            "    vec3 finalColor = texColor.rgb * pulse * tint;\n" +
            "    // Glow dorado suave\n" +
            "    float glow = sin(u_Time * 2.5) * 0.08 + 0.08;\n" +
            "    finalColor += vec3(1.0, 0.9, 0.6) * texColor.a * glow;\n" +
            "    gl_FragColor = vec4(finalColor, texColor.a * u_Alpha);\n" +
            "}\n";

        int vs = compileShader(GLES30.GL_VERTEX_SHADER, vertexShader);
        int fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShader);
        if (vs == 0 || fs == 0) return;

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vs);
        GLES30.glAttachShader(shaderProgram, fs);
        GLES30.glLinkProgram(shaderProgram);

        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        aTexCoordLoc = GLES30.glGetAttribLocation(shaderProgram, "a_TexCoord");
        uTextureLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Texture");
        uAlphaLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Alpha");
        uTimeLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Time");

        float[] vertices = { -1f, -1f, 0f, 1f, 1f, -1f, 1f, 1f, -1f, 1f, 0f, 0f, 1f, 1f, 1f, 0f };
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        int[] textures = new int[1];
        GLES30.glGenTextures(1, textures, 0);
        titleTextureId = textures[0];
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, titleTextureId);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        initialized = true;
    }

    private void renderTitle() {
        if (titleBitmap == null) return;
        titleBitmap.eraseColor(Color.TRANSPARENT);
        float centerX = TEX_WIDTH / 2f;
        float felizY = 85, navidadY = 170, snowflakeY = 240;

        // "FELIZ" - Rojo navideño con borde dorado
        Paint felizPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        felizPaint.setTextSize(72);
        felizPaint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        felizPaint.setTextAlign(Paint.Align.CENTER);
        LinearGradient felizGradient = new LinearGradient(0, felizY - 50, 0, felizY + 20,
            new int[]{0xFFFF4444, 0xFFCC0000, 0xFFFF2222, 0xFF880000},
            new float[]{0f, 0.3f, 0.6f, 1f}, Shader.TileMode.CLAMP);
        felizPaint.setShader(felizGradient);

        Paint felizShadow = new Paint(felizPaint);
        felizShadow.setShader(null);
        felizShadow.setColor(0xFF330000);

        // "NAVIDAD" - Verde navideño con borde dorado
        Paint navidadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        navidadPaint.setTextSize(72);
        navidadPaint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        navidadPaint.setTextAlign(Paint.Align.CENTER);
        LinearGradient navidadGradient = new LinearGradient(0, navidadY - 50, 0, navidadY + 20,
            new int[]{0xFF00CC44, 0xFF008833, 0xFF00AA33, 0xFF006622},
            new float[]{0f, 0.3f, 0.6f, 1f}, Shader.TileMode.CLAMP);
        navidadPaint.setShader(navidadGradient);

        Paint navidadShadow = new Paint(navidadPaint);
        navidadShadow.setShader(null);
        navidadShadow.setColor(0xFF003311);

        // Copo de nieve decorativo
        Paint snowflakePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        snowflakePaint.setTextSize(48);
        snowflakePaint.setTextAlign(Paint.Align.CENTER);
        snowflakePaint.setColor(0xFFFFFFFF);

        Paint snowflakeGlow = new Paint(snowflakePaint);
        snowflakeGlow.setColor(0xFF88DDFF);
        snowflakeGlow.setMaskFilter(new android.graphics.BlurMaskFilter(12, android.graphics.BlurMaskFilter.Blur.NORMAL));

        // Borde dorado para texto principal
        Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outlinePaint.setTextSize(72);
        outlinePaint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        outlinePaint.setTextAlign(Paint.Align.CENTER);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(5);
        outlinePaint.setColor(0xFFFFD700); // Dorado

        // Sombra 3D
        for (int i = 4; i > 0; i--) {
            float offset = i * 2;
            titleCanvas.drawText("FELIZ", centerX + offset, felizY + offset, felizShadow);
            titleCanvas.drawText("NAVIDAD", centerX + offset, navidadY + offset, navidadShadow);
        }

        // Borde dorado
        titleCanvas.drawText("FELIZ", centerX, felizY, outlinePaint);
        titleCanvas.drawText("NAVIDAD", centerX, navidadY, outlinePaint);

        // Texto principal
        titleCanvas.drawText("FELIZ", centerX, felizY, felizPaint);
        titleCanvas.drawText("NAVIDAD", centerX, navidadY, navidadPaint);

        // Copo de nieve
        titleCanvas.drawText("\u2744", centerX, snowflakeY, snowflakeGlow);
        titleCanvas.drawText("\u2744", centerX, snowflakeY, snowflakePaint);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, titleTextureId);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, titleBitmap, 0);
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

    @Override
    public void update(float deltaTime) {
        time += deltaTime;
        if (time > 100f) time -= 100f;
    }

    @Override
    public void draw() {
        if (!initialized || shaderProgram == 0 || alpha <= 0.01f) return;
        GLES30.glUseProgram(shaderProgram);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        float halfWidth = width / aspectRatio;
        float halfHeight = height;
        float[] vertices = { -halfWidth, posY - halfHeight, 0f, 1f, halfWidth, posY - halfHeight, 1f, 1f,
            -halfWidth, posY + halfHeight, 0f, 0f, halfWidth, posY + halfHeight, 1f, 0f };

        vertexBuffer.clear();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glEnableVertexAttribArray(aTexCoordLoc);
        vertexBuffer.position(0);
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer);
        vertexBuffer.position(2);
        GLES30.glVertexAttribPointer(aTexCoordLoc, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, titleTextureId);
        GLES30.glUniform1i(uTextureLoc, 0);
        GLES30.glUniform1f(uAlphaLoc, alpha);
        GLES30.glUniform1f(uTimeLoc, time);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glDisableVertexAttribArray(aTexCoordLoc);
    }

    public void setAspectRatio(float ratio) { this.aspectRatio = ratio; }
    public void setAlpha(float alpha) { this.alpha = alpha; }
    public void show() { this.alpha = 1.0f; }
    public void hide() { this.alpha = 0.0f; }

    public void dispose() {
        if (titleTextureId != 0) { int[] t = {titleTextureId}; GLES30.glDeleteTextures(1, t, 0); }
        if (titleBitmap != null) titleBitmap.recycle();
        if (shaderProgram != 0) GLES30.glDeleteProgram(shaderProgram);
    }
}
