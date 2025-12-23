package com.secret.blackholeglow.christmas;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import com.secret.blackholeglow.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║   ❄️ ChristmasSnowEffect - OPTIMIZADO (ES 2.0 compatible)                 ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * Renderiza copos de nieve usando instanced-like rendering:
 * - 28 copos, 6 vértices cada uno = 168 vértices total
 * - UN SOLO draw call para todos
 * - Vertex shader hace toda la animación
 * - Fragment shader simple = RÁPIDO
 * - Uses ES 2.0 compatible shaders (work in ES 3.0 context)
 */
public class ChristmasSnowEffect {
    private static final String TAG = "ChristmasSnow";

    private static final int NUM_SNOWFLAKES = 28;

    // OpenGL handles
    private int shaderProgram;
    private int textureId;
    private FloatBuffer vertexBuffer;

    // Attribute/Uniform locations
    private int aPositionLoc;
    private int aTexCoordLoc;
    private int aFlakeDataLoc;
    private int aFlakeData2Loc;
    private int uTimeLoc;
    private int uResolutionLoc;
    private int uTextureLoc;

    // Screen
    private float screenWidth = 1080f;
    private float screenHeight = 1920f;

    // State
    private boolean initialized = false;
    private boolean visible = true;
    private float time = 0.0f;
    private int vertexCount;

    private Context context;

    // VERTEX SHADER - ES 2.0 compatible
    private static final String VERTEX_SHADER =
        "attribute vec2 a_Position;\n" +
        "attribute vec2 a_TexCoord;\n" +
        "attribute vec4 a_FlakeData;\n" +  // baseX, size, speed, phase
        "attribute vec2 a_FlakeData2;\n" + // endY, layer
        "\n" +
        "uniform float u_Time;\n" +
        "uniform vec2 u_Resolution;\n" +
        "\n" +
        "varying vec2 v_TexCoord;\n" +
        "varying float v_Alpha;\n" +
        "varying float v_Twinkle;\n" +
        "varying vec3 v_Color;\n" +
        "\n" +
        "void main() {\n" +
        "    float baseX = a_FlakeData.x;\n" +
        "    float size = a_FlakeData.y;\n" +
        "    float speed = a_FlakeData.z;\n" +
        "    float phase = a_FlakeData.w;\n" +
        "    float endY = a_FlakeData2.x;\n" +
        "    float layer = a_FlakeData2.y;\n" +
        "    \n" +
        "    float cycleLen = (1.1 - endY) / speed;\n" +
        "    float t = mod(u_Time + phase, cycleLen * 1.3);\n" +
        "    float py = 1.1 - t * speed;\n" +
        "    \n" +
        "    float wt = u_Time + phase * 2.0;\n" +
        "    float wind = sin(wt * 0.25) * 0.08 + sin(wt * 0.7) * 0.03;\n" +
        "    float extraWind = endY > 0.2 ? (endY - 0.2) * sin(wt * 0.4) * 0.2 : 0.0;\n" +
        "    float px = baseX + wind + extraWind;\n" +
        "    \n" +
        "    float fadeStart = endY + 0.12;\n" +
        "    float fade = py > fadeStart ? 1.0 : \n" +
        "                 py > endY ? (py - endY) / (fadeStart - endY) : 0.0;\n" +
        "    \n" +
        "    float baseAlpha = 0.95 - layer * 0.15;\n" +
        "    v_Alpha = baseAlpha * fade;\n" +
        "    \n" +
        "    v_Twinkle = 0.7 + 0.3 * sin(u_Time * (2.0 + phase) + phase * 6.28);\n" +
        "    \n" +
        "    float colorType = mod(phase * 3.0, 3.0);\n" +
        "    if (colorType < 1.0) {\n" +
        "        v_Color = vec3(1.0, 1.0, 1.0);\n" +
        "    } else if (colorType < 2.0) {\n" +
        "        v_Color = vec3(0.85, 0.92, 1.0);\n" +
        "    } else {\n" +
        "        v_Color = vec3(1.0, 0.95, 0.85);\n" +
        "    }\n" +
        "    \n" +
        "    float angle = wt * 0.4 + wind * 2.0;\n" +
        "    float c = cos(angle);\n" +
        "    float s = sin(angle);\n" +
        "    vec2 rotated = vec2(a_Position.x * c - a_Position.y * s,\n" +
        "                        a_Position.x * s + a_Position.y * c);\n" +
        "    \n" +
        "    float aspect = u_Resolution.x / u_Resolution.y;\n" +
        "    vec2 pos = vec2(px, py) * 2.0 - 1.0;\n" +
        "    pos.x /= aspect;\n" +
        "    pos += rotated * size * 2.0;\n" +
        "    \n" +
        "    gl_Position = vec4(pos, 0.0, 1.0);\n" +
        "    v_TexCoord = a_TexCoord;\n" +
        "}\n";

    // FRAGMENT SHADER - ES 2.0 compatible
    private static final String FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "\n" +
        "uniform sampler2D u_Texture;\n" +
        "\n" +
        "varying vec2 v_TexCoord;\n" +
        "varying float v_Alpha;\n" +
        "varying float v_Twinkle;\n" +
        "varying vec3 v_Color;\n" +
        "\n" +
        "void main() {\n" +
        "    if (v_Alpha < 0.02) discard;\n" +
        "    \n" +
        "    vec4 tex = texture2D(u_Texture, v_TexCoord);\n" +
        "    \n" +
        "    vec2 center = v_TexCoord - 0.5;\n" +
        "    float dist = length(center);\n" +
        "    \n" +
        "    float glow = smoothstep(0.5, 0.2, dist) * 0.5;\n" +
        "    vec3 glowColor = vec3(0.7, 0.85, 1.0) * glow * v_Twinkle;\n" +
        "    \n" +
        "    if (tex.a > 0.1) {\n" +
        "        vec3 snowColor = tex.rgb * v_Color * v_Twinkle;\n" +
        "        snowColor += glowColor * 0.3;\n" +
        "        gl_FragColor = vec4(snowColor, tex.a * v_Alpha);\n" +
        "        return;\n" +
        "    }\n" +
        "    \n" +
        "    float glowAlpha = glow * v_Alpha * 0.6;\n" +
        "    if (glowAlpha < 0.01) discard;\n" +
        "    gl_FragColor = vec4(glowColor, glowAlpha);\n" +
        "}\n";

    public ChristmasSnowEffect() {}

    public void init(Context ctx) {
        this.context = ctx;
        initGL();
    }

    private void initGL() {
        // Compile shaders
        int vs = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER, "vertex");
        int fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER, "fragment");
        if (vs == 0 || fs == 0) {
            Log.e(TAG, "❌ Shader compilation failed");
            return;
        }

        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vs);
        GLES20.glAttachShader(shaderProgram, fs);
        GLES20.glLinkProgram(shaderProgram);

        int[] status = new int[1];
        GLES20.glGetProgramiv(shaderProgram, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) {
            Log.e(TAG, "Link error: " + GLES20.glGetProgramInfoLog(shaderProgram));
            return;
        }

        GLES20.glDeleteShader(vs);
        GLES20.glDeleteShader(fs);

        // Get locations
        aPositionLoc = GLES20.glGetAttribLocation(shaderProgram, "a_Position");
        aTexCoordLoc = GLES20.glGetAttribLocation(shaderProgram, "a_TexCoord");
        aFlakeDataLoc = GLES20.glGetAttribLocation(shaderProgram, "a_FlakeData");
        aFlakeData2Loc = GLES20.glGetAttribLocation(shaderProgram, "a_FlakeData2");
        uTimeLoc = GLES20.glGetUniformLocation(shaderProgram, "u_Time");
        uResolutionLoc = GLES20.glGetUniformLocation(shaderProgram, "u_Resolution");
        uTextureLoc = GLES20.glGetUniformLocation(shaderProgram, "u_Texture");

        // Build vertex data
        buildVertexData();

        // Load texture
        loadTexture();

        initialized = true;
        Log.d(TAG, "❄️ Snow inicializado: " + NUM_SNOWFLAKES + " copos, " + vertexCount + " vertices");
    }

    private void buildVertexData() {
        // 6 vertices per flake, 10 floats per vertex
        int floatsPerVertex = 10; // pos(2) + tex(2) + flakeData(4) + flakeData2(2)
        vertexCount = NUM_SNOWFLAKES * 6;
        float[] data = new float[vertexCount * floatsPerVertex];

        // Quad template (positions + UVs)
        float[][] quad = {
            {-1, -1, 0, 0},
            { 1, -1, 1, 0},
            {-1,  1, 0, 1},
            { 1, -1, 1, 0},
            { 1,  1, 1, 1},
            {-1,  1, 0, 1}
        };

        int idx = 0;
        for (int i = 0; i < NUM_SNOWFLAKES; i++) {
            float layer = i % 3;
            float baseX = 0.05f + (i / (float)NUM_SNOWFLAKES) * 0.9f;
            float size = layer == 0 ? 0.011f : layer == 1 ? 0.008f : 0.006f;
            size += (i * 0.31f % 1.0f) * 0.002f;
            float speed = layer == 0 ? 0.055f : layer == 1 ? 0.04f : 0.028f;
            speed += (i * 0.19f % 1.0f) * 0.015f;
            float phase = (i * 0.73f % 1.0f) * 5.0f;
            float destiny = (i * 0.67f % 1.0f);
            float endY = destiny < 0.4f ? 0.12f : destiny < 0.7f ? 0.3f : 0.5f;

            for (int v = 0; v < 6; v++) {
                data[idx++] = quad[v][0];
                data[idx++] = quad[v][1];
                data[idx++] = quad[v][2];
                data[idx++] = quad[v][3];
                data[idx++] = baseX;
                data[idx++] = size;
                data[idx++] = speed;
                data[idx++] = phase;
                data[idx++] = endY;
                data[idx++] = layer;
            }
        }

        vertexBuffer = ByteBuffer.allocateDirect(data.length * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(data).position(0);
    }

    private void loadTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        textureId = textures[0];

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = false;
        Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.copodenieve, opts);
        if (bmp != null) {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
            Log.d(TAG, "❄️ Texture: " + bmp.getWidth() + "x" + bmp.getHeight());
            bmp.recycle();
        }
    }

    private int compileShader(int type, String source, String name) {
        int shader = GLES20.glCreateShader(type);
        if (shader == 0) {
            Log.e(TAG, "❌ glCreateShader returned 0 for " + name);
            return 0;
        }
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Shader error (" + name + "): " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    public void update(float deltaTime) {
        time += deltaTime;
        if (time > 1000f) time -= 1000f;
    }

    public void draw() {
        if (!initialized || !visible || shaderProgram == 0) return;

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glUseProgram(shaderProgram);

        GLES20.glUniform1f(uTimeLoc, time);
        GLES20.glUniform2f(uResolutionLoc, screenWidth, screenHeight);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTextureLoc, 0);

        int stride = 40; // 10 floats * 4 bytes

        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, stride, vertexBuffer);

        vertexBuffer.position(2);
        GLES20.glEnableVertexAttribArray(aTexCoordLoc);
        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, stride, vertexBuffer);

        vertexBuffer.position(4);
        GLES20.glEnableVertexAttribArray(aFlakeDataLoc);
        GLES20.glVertexAttribPointer(aFlakeDataLoc, 4, GLES20.GL_FLOAT, false, stride, vertexBuffer);

        vertexBuffer.position(8);
        GLES20.glEnableVertexAttribArray(aFlakeData2Loc);
        GLES20.glVertexAttribPointer(aFlakeData2Loc, 2, GLES20.GL_FLOAT, false, stride, vertexBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

        GLES20.glDisableVertexAttribArray(aPositionLoc);
        GLES20.glDisableVertexAttribArray(aTexCoordLoc);
        GLES20.glDisableVertexAttribArray(aFlakeDataLoc);
        GLES20.glDisableVertexAttribArray(aFlakeData2Loc);
    }

    public void setScreenSize(int w, int h) {
        screenWidth = w;
        screenHeight = h;
    }

    public void setVisible(boolean v) { visible = v; }
    public boolean isVisible() { return visible; }

    public void dispose() {
        if (textureId != 0) {
            int[] t = {textureId};
            GLES20.glDeleteTextures(1, t, 0);
            textureId = 0;
        }
        if (shaderProgram != 0) {
            GLES20.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        initialized = false;
        Log.d(TAG, "🗑️ ChristmasSnowEffect disposed");
    }
}
