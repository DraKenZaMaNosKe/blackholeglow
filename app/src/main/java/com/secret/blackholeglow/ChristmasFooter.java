package com.secret.blackholeglow;

import android.graphics.Bitmap;
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

/**
 * CHRISTMAS FOOTER - Mensaje navideño en la parte inferior
 * "Toca para comenzar" con decoraciones
 */
public class ChristmasFooter implements SceneObject {
    private static final String TAG = "ChristmasFooter";

    private int textureId = 0;
    private Bitmap bitmap;
    private Canvas canvas;

    private static final int TEX_WIDTH = 512;
    private static final int TEX_HEIGHT = 60;

    private int shaderProgram = 0;
    private FloatBuffer vertexBuffer;
    private int aPositionLoc, aTexCoordLoc;
    private int uTextureLoc, uAlphaLoc, uTimeLoc;

    private float alpha = 1.0f;
    private float time = 0f;
    private float aspectRatio = 1.0f;
    private boolean initialized = false;

    private float posY = -0.65f;
    private float width = 0.55f;
    private float height = 0.05f;

    public ChristmasFooter() {
        Log.d(TAG, "Creating ChristmasFooter...");
        initBitmap();
        initOpenGL();
        renderText();
    }

    private void initBitmap() {
        bitmap = Bitmap.createBitmap(TEX_WIDTH, TEX_HEIGHT, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
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

        // Shader con efecto de parpadeo suave
        String fragmentShader =
            "precision mediump float;\n" +
            "varying vec2 v_TexCoord;\n" +
            "uniform sampler2D u_Texture;\n" +
            "uniform float u_Alpha;\n" +
            "uniform float u_Time;\n" +
            "void main() {\n" +
            "    vec4 texColor = texture2D(u_Texture, v_TexCoord);\n" +
            "    // Parpadeo navideño suave\n" +
            "    float blink = sin(u_Time * 2.5) * 0.3 + 0.7;\n" +
            "    // Tinte que cambia entre rojo y verde\n" +
            "    float colorMix = sin(u_Time * 1.2) * 0.5 + 0.5;\n" +
            "    vec3 tintRed = vec3(1.0, 0.9, 0.9);\n" +
            "    vec3 tintGreen = vec3(0.9, 1.0, 0.9);\n" +
            "    vec3 tint = mix(tintRed, tintGreen, colorMix);\n" +
            "    vec3 finalColor = texColor.rgb * blink * tint;\n" +
            "    gl_FragColor = vec4(finalColor, texColor.a * u_Alpha * blink);\n" +
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
        textureId = textures[0];
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        initialized = true;
    }

    private void renderText() {
        if (bitmap == null) return;
        bitmap.eraseColor(Color.TRANSPARENT);
        float centerX = TEX_WIDTH / 2f;
        float centerY = TEX_HEIGHT / 2f + 10;

        // Texto principal
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(26);
        textPaint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.ITALIC));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(0xFFFFFFFF);

        // Glow
        Paint glowPaint = new Paint(textPaint);
        glowPaint.setColor(0xFFFFD700);
        glowPaint.setMaskFilter(new android.graphics.BlurMaskFilter(8, android.graphics.BlurMaskFilter.Blur.NORMAL));

        // Decoraciones
        Paint decorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        decorPaint.setTextSize(24);
        decorPaint.setTextAlign(Paint.Align.CENTER);
        decorPaint.setColor(0xFFFF4444);

        String message = "Toca para comenzar";

        canvas.drawText(message, centerX, centerY, glowPaint);
        canvas.drawText(message, centerX, centerY, textPaint);

        // Estrellas decorativas a los lados
        canvas.drawText("\u2605", centerX - 140, centerY, decorPaint);
        decorPaint.setColor(0xFF00CC44);
        canvas.drawText("\u2605", centerX + 140, centerY, decorPaint);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
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
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
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
        if (textureId != 0) { int[] t = {textureId}; GLES30.glDeleteTextures(1, t, 0); }
        if (bitmap != null) bitmap.recycle();
        if (shaderProgram != 0) GLES30.glDeleteProgram(shaderProgram);
    }
}
