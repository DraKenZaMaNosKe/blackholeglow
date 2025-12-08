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
 * ARCADE TITLE - Logo estilo Street Fighter (VERTICAL)
 *         HUMANS
 *           VS
 *         ALIENS
 */
public class ArcadeTitle implements SceneObject {
    private static final String TAG = "ArcadeTitle";

    private int titleTextureId = 0;
    private Bitmap titleBitmap;
    private Canvas titleCanvas;

    private static final int TEX_WIDTH = 512;
    private static final int TEX_HEIGHT = 300;

    private int shaderProgram = 0;
    private FloatBuffer vertexBuffer;
    private int aPositionLoc, aTexCoordLoc;
    private int uTextureLoc, uAlphaLoc, uTimeLoc;

    private float alpha = 1.0f;
    private float time = 0f;
    private float aspectRatio = 1.0f;
    private boolean initialized = false;

    private float posY = 0.58f;
    private float width = 0.70f;
    private float height = 0.28f;

    public ArcadeTitle() {
        Log.d(TAG, "Creating ArcadeTitle VERTICAL...");
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

        String fragmentShader =
            "precision mediump float;\n" +
            "varying vec2 v_TexCoord;\n" +
            "uniform sampler2D u_Texture;\n" +
            "uniform float u_Alpha;\n" +
            "uniform float u_Time;\n" +
            "void main() {\n" +
            "    vec4 texColor = texture2D(u_Texture, v_TexCoord);\n" +
            "    float pulse = sin(u_Time * 2.0) * 0.15 + 1.0;\n" +
            "    vec3 finalColor = texColor.rgb * pulse;\n" +
            "    float glow = sin(u_Time * 3.0) * 0.1 + 0.1;\n" +
            "    finalColor += texColor.rgb * glow;\n" +
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
        float humansY = 80, vsY = 155, aliensY = 230;

        Paint humansPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        humansPaint.setTextSize(64);
        humansPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        humansPaint.setTextAlign(Paint.Align.CENTER);
        LinearGradient humansGradient = new LinearGradient(0, humansY - 50, 0, humansY + 20,
            new int[]{0xFF00FFFF, 0xFFFFFFFF, 0xFF00DDFF, 0xFF0099CC}, new float[]{0f, 0.3f, 0.6f, 1f}, Shader.TileMode.CLAMP);
        humansPaint.setShader(humansGradient);

        Paint humansShadow = new Paint(humansPaint);
        humansShadow.setShader(null);
        humansShadow.setColor(0xFF004466);

        Paint aliensPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        aliensPaint.setTextSize(64);
        aliensPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        aliensPaint.setTextAlign(Paint.Align.CENTER);
        LinearGradient aliensGradient = new LinearGradient(0, aliensY - 50, 0, aliensY + 20,
            new int[]{0xFFFF00FF, 0xFFFF66FF, 0xFFDD00DD, 0xFF8800AA}, new float[]{0f, 0.3f, 0.6f, 1f}, Shader.TileMode.CLAMP);
        aliensPaint.setShader(aliensGradient);

        Paint aliensShadow = new Paint(aliensPaint);
        aliensShadow.setShader(null);
        aliensShadow.setColor(0xFF440066);

        Paint vsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        vsPaint.setTextSize(48);
        vsPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC));
        vsPaint.setTextAlign(Paint.Align.CENTER);
        vsPaint.setColor(0xFFFFDD00);

        Paint vsGlow = new Paint(vsPaint);
        vsGlow.setColor(0xFFFF8800);
        vsGlow.setMaskFilter(new android.graphics.BlurMaskFilter(12, android.graphics.BlurMaskFilter.Blur.NORMAL));

        Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outlinePaint.setTextSize(64);
        outlinePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        outlinePaint.setTextAlign(Paint.Align.CENTER);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(4);
        outlinePaint.setColor(0xFF000000);

        for (int i = 4; i > 0; i--) {
            float offset = i * 2;
            titleCanvas.drawText("HUMANS", centerX + offset, humansY + offset, humansShadow);
            titleCanvas.drawText("ALIENS", centerX + offset, aliensY + offset, aliensShadow);
        }

        titleCanvas.drawText("HUMANS", centerX, humansY, outlinePaint);
        titleCanvas.drawText("ALIENS", centerX, aliensY, outlinePaint);
        titleCanvas.drawText("HUMANS", centerX, humansY, humansPaint);
        titleCanvas.drawText("ALIENS", centerX, aliensY, aliensPaint);
        titleCanvas.drawText("VS", centerX, vsY, vsGlow);
        titleCanvas.drawText("VS", centerX, vsY, vsPaint);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, titleTextureId);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, titleBitmap, 0);
    }

    private int compileShader(int type, String code) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, code);
        GLES30.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) { GLES30.glDeleteShader(shader); return 0; }
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
