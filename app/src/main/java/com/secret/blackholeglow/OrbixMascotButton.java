package com.secret.blackholeglow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class OrbixMascotButton implements SceneObject {
    private static final String TAG = "OrbixMascot";
    private Context context;
    private int textureId = 0;
    private int shaderProgram = 0;
    private int aPositionLoc, aTexCoordLoc, uTextureLoc, uTimeLoc, uAlphaLoc, uScaleLoc;
    private FloatBuffer vertexBuffer;
    private float time = 0f;
    private float size = 0.12f;
    private float centerX = 0f, centerY = 0f;
    private float aspectRatio = 1f;
    private float alpha = 1f;
    private boolean isVisible = true;
    private boolean isPressed = false;
    private float pressScale = 1f;
    private static final float TIME_CYCLE = 62.831853f;

    public OrbixMascotButton(Context ctx) { this.context = ctx; }

    public void initialize() {
        String vs =
            "#version 300 es\n" +
            "in vec2 aPosition;\n" +
            "in vec2 aTexCoord;\n" +
            "out vec2 vTexCoord;\n" +
            "uniform float uScale;\n" +
            "void main() {\n" +
            "    vTexCoord = aTexCoord;\n" +
            "    gl_Position = vec4(aPosition * uScale, 0.0, 1.0);\n" +
            "}\n";

        String fs =
            "#version 300 es\n" +
            "precision mediump float;\n" +
            "in vec2 vTexCoord;\n" +
            "out vec4 fragColor;\n" +
            "uniform sampler2D uTexture;\n" +
            "uniform float uTime;\n" +
            "uniform float uAlpha;\n" +
            "#define PI 3.14159265\n" +
            "float star(vec2 uv, vec2 pos, float sz) {\n" +
            "    float d = length(uv - pos);\n" +
            "    float angle = atan(uv.y - pos.y, uv.x - pos.x);\n" +
            "    float rays = abs(cos(angle * 4.0)) * 0.5 + 0.5;\n" +
            "    float s = smoothstep(sz, 0.0, d) * rays;\n" +
            "    s += smoothstep(sz * 0.4, 0.0, d);\n" +
            "    return s;\n" +
            "}\n" +
            "void main() {\n" +
            "    vec4 tex = texture(uTexture, vTexCoord);\n" +
            "    vec3 finalColor = tex.rgb;\n" +
            "    float finalAlpha = tex.a;\n" +
            "    float radius = 0.38;\n" +
            "    float starAlpha = 0.0;\n" +
            "    vec3 starColor = vec3(0.0);\n" +
            "    for (int i = 0; i < 4; i++) {\n" +
            "        float fi = float(i);\n" +
            "        float angle = fi * PI * 0.5 + uTime * (0.8 + fi * 0.1);\n" +
            "        vec2 starPos = vec2(0.5) + vec2(cos(angle), sin(angle)) * radius;\n" +
            "        float twinkle = sin(uTime * 4.0 + fi * 2.0) * 0.3 + 0.7;\n" +
            "        float s = star(vTexCoord, starPos, 0.04) * twinkle;\n" +
            "        vec3 col = vec3(1.0, 0.9, 0.5);\n" +
            "        if (i == 1) col = vec3(0.5, 0.8, 1.0);\n" +
            "        if (i == 2) col = vec3(1.0, 0.6, 0.8);\n" +
            "        if (i == 3) col = vec3(0.6, 1.0, 0.7);\n" +
            "        starColor += col * s;\n" +
            "        starAlpha += s * 0.8;\n" +
            "    }\n" +
            "    finalColor = mix(finalColor, starColor, min(starAlpha, 1.0) * (1.0 - tex.a));\n" +
            "    finalColor += starColor * 0.5;\n" +
            "    finalAlpha = max(finalAlpha, starAlpha * 0.6);\n" +
            "    fragColor = vec4(finalColor, finalAlpha * uAlpha);\n" +
            "}\n";

        int vsh = compileShader(GLES30.GL_VERTEX_SHADER, vs);
        int fsh = compileShader(GLES30.GL_FRAGMENT_SHADER, fs);
        if (vsh == 0 || fsh == 0) return;
        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vsh);
        GLES30.glAttachShader(shaderProgram, fsh);
        GLES30.glLinkProgram(shaderProgram);
        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "aPosition");
        aTexCoordLoc = GLES30.glGetAttribLocation(shaderProgram, "aTexCoord");
        uTextureLoc = GLES30.glGetUniformLocation(shaderProgram, "uTexture");
        uTimeLoc = GLES30.glGetUniformLocation(shaderProgram, "uTime");
        uAlphaLoc = GLES30.glGetUniformLocation(shaderProgram, "uAlpha");
        uScaleLoc = GLES30.glGetUniformLocation(shaderProgram, "uScale");
        float[] verts = { -1f, -1f, 0f, 1f, 1f, -1f, 1f, 1f, -1f, 1f, 0f, 0f, 1f, 1f, 1f, 0f };
        ByteBuffer bb = ByteBuffer.allocateDirect(verts.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(verts);
        vertexBuffer.position(0);
        loadTexture();
        Log.d(TAG, "Gatito mascota inicializado");
    }

    private void loadTexture() {
        if (context == null) return;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = false;
        Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.gatito_orbix, opts);
        if (bmp == null) { Log.e(TAG, "Error cargando gatito"); return; }
        int[] tex = new int[1];
        GLES30.glGenTextures(1, tex, 0);
        textureId = tex[0];
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bmp, 0);
        bmp.recycle();
        Log.d(TAG, "Textura gatito cargada");
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
    public void update(float dt) {
        time += dt;
        if (time > TIME_CYCLE) time -= TIME_CYCLE;
        if (isPressed && pressScale > 0.85f) pressScale -= dt * 4f;
        else if (!isPressed && pressScale < 1f) { pressScale += dt * 6f; if (pressScale > 1f) pressScale = 1f; }
    }

    @Override
    public void draw() {
        if (!isVisible || shaderProgram == 0 || textureId == 0) return;
        GLES30.glUseProgram(shaderProgram);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        float finalScale = size * pressScale;
        GLES30.glUniform1f(uTimeLoc, time);
        GLES30.glUniform1f(uAlphaLoc, alpha);
        GLES30.glUniform1f(uScaleLoc, finalScale);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(uTextureLoc, 0);
        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glEnableVertexAttribArray(aTexCoordLoc);
        vertexBuffer.position(0);
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer);
        vertexBuffer.position(2);
        GLES30.glVertexAttribPointer(aTexCoordLoc, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glDisableVertexAttribArray(aTexCoordLoc);
        GLES30.glUseProgram(0);
    }

    public boolean isInside(float touchX, float touchY) {
        float hitSize = size * 1.3f;
        float dx = touchX - centerX;
        float dy = touchY - centerY;
        return (dx * dx + dy * dy) <= hitSize * hitSize;
    }

    public void onTouchDown() { isPressed = true; }
    public void onTouchUp() { isPressed = false; }
    public boolean toggle() { return true; }
    public void setSize(float s) { this.size = s; }
    public void setPosition(float x, float y) { centerX = x; centerY = y; }
    public void setAspectRatio(float a) { aspectRatio = a; }
    public void setAlpha(float a) { alpha = a; }
    public void setVisible(boolean v) { isVisible = v; }
    public boolean isVisible() { return isVisible; }
    public void show() { isVisible = true; alpha = 1f; }
    public void setPlaying(boolean p) {}
    public boolean isPlaying() { return false; }
    public void resetTime() { time = 0f; }

    public void dispose() {
        if (textureId != 0) { int[] t = {textureId}; GLES30.glDeleteTextures(1, t, 0); }
        if (shaderProgram != 0) GLES30.glDeleteProgram(shaderProgram);
    }
}
