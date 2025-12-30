package com.secret.blackholeglow.sharing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import com.secret.blackholeglow.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class LikeButton {
    private static final String TAG = "LikeButton";

    public enum Theme { DEFAULT, ABYSSIA, PYRALIS }
    private Theme currentTheme = Theme.DEFAULT;

    private float x = 0.85f;
    private float y = -0.75f;
    private float size = 0.08f;

    private boolean isPressed = false;
    private boolean isOnCooldown = false;
    private float pulsePhase = 0f;
    private float floatOffset = 0f;

    private int programIdTexture;
    private int programIdColor;
    private FloatBuffer quadBuffer;
    private FloatBuffer heartBuffer;
    private boolean isInitialized = false;

    private int textureAbyssia = -1;
    private int textureFireOrb = -1;
    private Context context;

    private float[] colorNormal = {1.0f, 0.0f, 0.5f, 0.85f};
    private float[] colorPressed = {0.0f, 0.85f, 1.0f, 1.0f};
    private float[] colorCooldown = {0.3f, 0.3f, 0.35f, 0.6f};
    private float[] glowCyan = {0.0f, 0.85f, 1.0f};
    private float[] glowPink = {1.0f, 0.0f, 0.5f};

    private final float[] modelMatrix = new float[16];
    private final float[] finalMatrix = new float[16];
    private final float[] colorCache = new float[4];

    private static final String TEXTURE_VERTEX_SHADER =
            "attribute vec4 a_Position;\n" +
            "attribute vec2 a_TexCoord;\n" +
            "varying vec2 v_TexCoord;\n" +
            "uniform mat4 u_MVPMatrix;\n" +
            "void main() {\n" +
            "    gl_Position = u_MVPMatrix * a_Position;\n" +
            "    v_TexCoord = a_TexCoord;\n" +
            "}";

    private static final String TEXTURE_FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "varying vec2 v_TexCoord;\n" +
            "uniform sampler2D u_Texture;\n" +
            "uniform float u_Alpha;\n" +
            "uniform float u_Pulse;\n" +
            "void main() {\n" +
            "    vec4 texColor = texture2D(u_Texture, v_TexCoord);\n" +
            "    texColor.rgb *= (0.9 + u_Pulse * 0.2);\n" +
            "    texColor.a *= u_Alpha;\n" +
            "    gl_FragColor = texColor;\n" +
            "}";

    private static final String COLOR_VERTEX_SHADER =
            "attribute vec4 a_Position;\n" +
            "uniform mat4 u_MVPMatrix;\n" +
            "void main() {\n" +
            "    gl_Position = u_MVPMatrix * a_Position;\n" +
            "}";

    private static final String COLOR_FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform vec4 u_Color;\n" +
            "void main() {\n" +
            "    gl_FragColor = u_Color;\n" +
            "}";

    private int positionHandleTex, texCoordHandle, mvpMatrixHandleTex, textureHandle, alphaHandle, pulseHandle;
    private int positionHandleColor, mvpMatrixHandleColor, colorHandle;

    public LikeButton(Context context) {
        this.context = context;
    }

    public void init() {
        if (isInitialized) return;
        int vertexShaderTex = loadShader(GLES30.GL_VERTEX_SHADER, TEXTURE_VERTEX_SHADER);
        int fragmentShaderTex = loadShader(GLES30.GL_FRAGMENT_SHADER, TEXTURE_FRAGMENT_SHADER);
        programIdTexture = GLES30.glCreateProgram();
        GLES30.glAttachShader(programIdTexture, vertexShaderTex);
        GLES30.glAttachShader(programIdTexture, fragmentShaderTex);
        GLES30.glLinkProgram(programIdTexture);
        positionHandleTex = GLES30.glGetAttribLocation(programIdTexture, "a_Position");
        texCoordHandle = GLES30.glGetAttribLocation(programIdTexture, "a_TexCoord");
        mvpMatrixHandleTex = GLES30.glGetUniformLocation(programIdTexture, "u_MVPMatrix");
        textureHandle = GLES30.glGetUniformLocation(programIdTexture, "u_Texture");
        alphaHandle = GLES30.glGetUniformLocation(programIdTexture, "u_Alpha");
        pulseHandle = GLES30.glGetUniformLocation(programIdTexture, "u_Pulse");
        int vertexShaderCol = loadShader(GLES30.GL_VERTEX_SHADER, COLOR_VERTEX_SHADER);
        int fragmentShaderCol = loadShader(GLES30.GL_FRAGMENT_SHADER, COLOR_FRAGMENT_SHADER);
        programIdColor = GLES30.glCreateProgram();
        GLES30.glAttachShader(programIdColor, vertexShaderCol);
        GLES30.glAttachShader(programIdColor, fragmentShaderCol);
        GLES30.glLinkProgram(programIdColor);
        positionHandleColor = GLES30.glGetAttribLocation(programIdColor, "a_Position");
        mvpMatrixHandleColor = GLES30.glGetUniformLocation(programIdColor, "u_MVPMatrix");
        colorHandle = GLES30.glGetUniformLocation(programIdColor, "u_Color");
        createQuadGeometry();
        createHeartGeometry();
        loadTextures();
        isInitialized = true;
        Log.d(TAG, "LikeButton init");
    }

    private void createQuadGeometry() {
        float[] quadVertices = { -1.0f, -1.0f, 0.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f };
        ByteBuffer bb = ByteBuffer.allocateDirect(quadVertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        quadBuffer = bb.asFloatBuffer();
        quadBuffer.put(quadVertices);
        quadBuffer.position(0);
    }

    private void createHeartGeometry() {
        int segments = 64;
        float[] vertices = new float[(segments + 2) * 2];
        vertices[0] = 0f; vertices[1] = -0.1f;
        for (int i = 0; i <= segments; i++) {
            float t = (float) (2.0 * Math.PI * i / segments);
            float hx = (float) (16.0 * Math.pow(Math.sin(t), 3));
            float hy = (float) (13.0 * Math.cos(t) - 5.0 * Math.cos(2*t) - 2.0 * Math.cos(3*t) - Math.cos(4*t));
            vertices[(i + 1) * 2] = hx / 17.0f;
            vertices[(i + 1) * 2 + 1] = hy / 17.0f;
        }
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        heartBuffer = bb.asFloatBuffer();
        heartBuffer.put(vertices);
        heartBuffer.position(0);
    }

    private void loadTextures() {
        textureAbyssia = loadTexture(R.drawable.huevo_zerg);
        textureFireOrb = loadTexture(R.drawable.fire_orb);
        Log.d(TAG, "Textures loaded");
    }

    private int loadTexture(int resourceId) {
        final int[] texHandle = new int[1];
        GLES30.glGenTextures(1, texHandle, 0);
        if (texHandle[0] != 0) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texHandle[0]);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();
        }
        return texHandle[0];
    }

    public void setTheme(Theme theme) { this.currentTheme = theme; Log.d(TAG, "Theme: " + theme); }

    public void draw(float[] mvpMatrix, float time) {
        if (!isInitialized) return;
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        pulsePhase = time;
        float pulse = (float) Math.sin(pulsePhase * 2.5) * 0.5f + 0.5f;
        floatOffset = (float) Math.sin(pulsePhase * 1.5) * 0.01f;
        if (currentTheme == Theme.DEFAULT) { drawHeartProcedural(mvpMatrix, pulse); }
        else { drawTexturedButton(mvpMatrix, pulse); }
    }

    private void drawTexturedButton(float[] mvpMatrix, float pulse) {
        int textureId = (currentTheme == Theme.ABYSSIA) ? textureAbyssia : textureFireOrb;
        if (textureId <= 0) return;
        float alpha = isOnCooldown ? 0.4f : (isPressed ? 1.0f : 0.9f);
        float scale = size * (1.0f + pulse * 0.08f);
        float currentY = y + floatOffset;
        drawGlow(mvpMatrix, scale * 1.4f, pulse);
        GLES30.glUseProgram(programIdTexture);
        android.opengl.Matrix.setIdentityM(modelMatrix, 0);
        android.opengl.Matrix.translateM(modelMatrix, 0, x, currentY, 0);
        android.opengl.Matrix.scaleM(modelMatrix, 0, scale, scale, 1);
        android.opengl.Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0);
        GLES30.glUniformMatrix4fv(mvpMatrixHandleTex, 1, false, finalMatrix, 0);
        GLES30.glUniform1f(alphaHandle, alpha);
        GLES30.glUniform1f(pulseHandle, pulse);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(textureHandle, 0);
        quadBuffer.position(0);
        GLES30.glEnableVertexAttribArray(positionHandleTex);
        GLES30.glVertexAttribPointer(positionHandleTex, 2, GLES30.GL_FLOAT, false, 16, quadBuffer);
        quadBuffer.position(2);
        GLES30.glEnableVertexAttribArray(texCoordHandle);
        GLES30.glVertexAttribPointer(texCoordHandle, 2, GLES30.GL_FLOAT, false, 16, quadBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glDisableVertexAttribArray(positionHandleTex);
        GLES30.glDisableVertexAttribArray(texCoordHandle);
    }

    private void drawGlow(float[] mvpMatrix, float glowSize, float pulse) {
        if (isOnCooldown) return;
        GLES30.glUseProgram(programIdColor);
        float currentY = y + floatOffset;
        float[] glowColor = (currentTheme == Theme.ABYSSIA) ? new float[]{0.4f, 0.8f, 1.0f, 0.25f * pulse} : new float[]{1.0f, 0.5f, 0.1f, 0.3f * pulse};
        android.opengl.Matrix.setIdentityM(modelMatrix, 0);
        android.opengl.Matrix.translateM(modelMatrix, 0, x, currentY, 0);
        android.opengl.Matrix.scaleM(modelMatrix, 0, glowSize, glowSize, 1);
        android.opengl.Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0);
        GLES30.glUniformMatrix4fv(mvpMatrixHandleColor, 1, false, finalMatrix, 0);
        GLES30.glUniform4fv(colorHandle, 1, glowColor, 0);
        drawCircle();
    }

    private void drawCircle() {
        int segments = 32;
        float[] circleVerts = new float[(segments + 2) * 2];
        circleVerts[0] = 0f; circleVerts[1] = 0f;
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (2.0 * Math.PI * i / segments);
            circleVerts[(i + 1) * 2] = (float) Math.cos(angle);
            circleVerts[(i + 1) * 2 + 1] = (float) Math.sin(angle);
        }
        ByteBuffer bb = ByteBuffer.allocateDirect(circleVerts.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer circleBuffer = bb.asFloatBuffer();
        circleBuffer.put(circleVerts);
        circleBuffer.position(0);
        GLES30.glEnableVertexAttribArray(positionHandleColor);
        GLES30.glVertexAttribPointer(positionHandleColor, 2, GLES30.GL_FLOAT, false, 0, circleBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, segments + 2);
        GLES30.glDisableVertexAttribArray(positionHandleColor);
    }

    private void drawHeartProcedural(float[] mvpMatrix, float pulse) {
        GLES30.glUseProgram(programIdColor);
        float scale = size * (1.0f + pulse * 0.12f);
        float currentY = y + floatOffset;
        if (!isOnCooldown) { drawHeartGlow(mvpMatrix, scale * 1.5f, 0.2f); drawHeartGlow(mvpMatrix, scale * 1.3f, 0.3f); }
        android.opengl.Matrix.setIdentityM(modelMatrix, 0);
        android.opengl.Matrix.translateM(modelMatrix, 0, x, currentY, 0);
        android.opengl.Matrix.scaleM(modelMatrix, 0, scale, scale, 1);
        android.opengl.Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0);
        GLES30.glUniformMatrix4fv(mvpMatrixHandleColor, 1, false, finalMatrix, 0);
        float[] color = isOnCooldown ? colorCooldown : (isPressed ? colorPressed : colorNormal);
        GLES30.glUniform4fv(colorHandle, 1, color, 0);
        GLES30.glEnableVertexAttribArray(positionHandleColor);
        GLES30.glVertexAttribPointer(positionHandleColor, 2, GLES30.GL_FLOAT, false, 0, heartBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 66);
        float borderPulse = (float) (Math.sin(pulsePhase * 4.0) * 0.3 + 0.7);
        colorCache[0] = glowCyan[0] * borderPulse + 0.3f;
        colorCache[1] = glowCyan[1] * borderPulse + 0.1f;
        colorCache[2] = glowCyan[2] * borderPulse;
        colorCache[3] = 1.0f;
        GLES30.glUniform4fv(colorHandle, 1, colorCache, 0);
        GLES30.glLineWidth(3.0f);
        GLES30.glDrawArrays(GLES30.GL_LINE_LOOP, 1, 65);
        GLES30.glDisableVertexAttribArray(positionHandleColor);
    }

    private void drawHeartGlow(float[] mvpMatrix, float glowSize, float alpha) {
        float currentY = y + floatOffset;
        android.opengl.Matrix.setIdentityM(modelMatrix, 0);
        android.opengl.Matrix.translateM(modelMatrix, 0, x, currentY, 0);
        android.opengl.Matrix.scaleM(modelMatrix, 0, glowSize, glowSize, 1);
        android.opengl.Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0);
        GLES30.glUniformMatrix4fv(mvpMatrixHandleColor, 1, false, finalMatrix, 0);
        float colorMix = (float) (Math.sin(pulsePhase * 2.5) * 0.5 + 0.5);
        colorCache[0] = glowCyan[0] * (1 - colorMix) + glowPink[0] * colorMix;
        colorCache[1] = glowCyan[1] * (1 - colorMix) + glowPink[1] * colorMix;
        colorCache[2] = glowCyan[2] * (1 - colorMix) + glowPink[2] * colorMix;
        colorCache[3] = alpha * 0.8f;
        GLES30.glUniform4fv(colorHandle, 1, colorCache, 0);
        GLES30.glEnableVertexAttribArray(positionHandleColor);
        GLES30.glVertexAttribPointer(positionHandleColor, 2, GLES30.GL_FLOAT, false, 0, heartBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 66);
        GLES30.glDisableVertexAttribArray(positionHandleColor);
    }

    public boolean isTouched(float touchX, float touchY) { float dx = touchX - x; float dy = touchY - y; return Math.sqrt(dx * dx + dy * dy) <= size * 1.5f; }
    public void onPress() { isPressed = true; Log.d(TAG, "Press"); }
    public void onRelease() { isPressed = false; }
    public void setCooldown(boolean cooldown) { isOnCooldown = cooldown; }
    public void setPosition(float x, float y) { this.x = x; this.y = y; }
    public void setSize(float size) { this.size = size; }
    public float getX() { return x; }
    public float getY() { return y; }
    private int loadShader(int type, String shaderCode) { int shader = GLES30.glCreateShader(type); GLES30.glShaderSource(shader, shaderCode); GLES30.glCompileShader(shader); return shader; }
    public void cleanup() { if (programIdTexture != 0) GLES30.glDeleteProgram(programIdTexture); if (programIdColor != 0) GLES30.glDeleteProgram(programIdColor); if (textureAbyssia > 0) GLES30.glDeleteTextures(1, new int[]{textureAbyssia}, 0); if (textureFireOrb > 0) GLES30.glDeleteTextures(1, new int[]{textureFireOrb}, 0); isInitialized = false; }
}
