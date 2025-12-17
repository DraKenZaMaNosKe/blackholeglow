package com.secret.blackholeglow;

import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * CHRISTMAS SNOWFALL - Copos de nieve cayendo con shader GPU
 * Efecto procedural sin texturas - copos brillantes y suaves
 */
public class ChristmasSnowfall implements SceneObject {
    private static final String TAG = "ChristmasSnowfall";

    private int shaderProgram = 0;
    private int aPositionLoc = -1;
    private int uTimeLoc = -1;
    private int uAspectLoc = -1;
    private int uAlphaLoc = -1;

    private FloatBuffer vertexBuffer;
    private static final float[] QUAD_VERTICES = {
        -1.0f, -1.0f,
         1.0f, -1.0f,
        -1.0f,  1.0f,
         1.0f,  1.0f
    };

    private float time = 0f;
    private float alpha = 1.0f;
    private float aspectRatio = 1.0f;
    private boolean initialized = false;

    public ChristmasSnowfall() {
        Log.d(TAG, "Creating ChristmasSnowfall...");
        compileShaders();
    }

    private void compileShaders() {
        String vertexShaderCode =
            "#version 300 es\n" +
            "in vec2 a_Position;\n" +
            "out vec2 v_UV;\n" +
            "void main() {\n" +
            "    v_UV = a_Position * 0.5 + 0.5;\n" +
            "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
            "}\n";

        String fragmentShaderCode =
            "#version 300 es\n" +
            "precision mediump float;\n" +
            "in vec2 v_UV;\n" +
            "out vec4 fragColor;\n" +
            "uniform float u_Time;\n" +
            "uniform float u_Aspect;\n" +
            "uniform float u_Alpha;\n" +
            "\n" +
            "#define NUM_LAYERS 5.0\n" +
            "#define NUM_FLAKES 15.0\n" +
            "\n" +
            "// Hash function for pseudo-random values\n" +
            "float hash(float n) {\n" +
            "    return fract(sin(n) * 43758.5453123);\n" +
            "}\n" +
            "\n" +
            "// Snowflake shape - soft glowing circle\n" +
            "float snowflake(vec2 uv, vec2 center, float size) {\n" +
            "    float d = length(uv - center);\n" +
            "    // Soft glow\n" +
            "    float glow = smoothstep(size * 2.0, 0.0, d);\n" +
            "    // Core\n" +
            "    float core = smoothstep(size, size * 0.3, d);\n" +
            "    return glow * 0.3 + core * 0.7;\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 uv = v_UV;\n" +
            "    uv.x *= u_Aspect;\n" +
            "    \n" +
            "    vec3 color = vec3(0.0);\n" +
            "    float totalAlpha = 0.0;\n" +
            "    \n" +
            "    // Multiple layers of snowflakes for depth\n" +
            "    for (float layer = 0.0; layer < NUM_LAYERS; layer++) {\n" +
            "        float layerDepth = (layer + 1.0) / NUM_LAYERS;\n" +
            "        float speed = 0.08 + layerDepth * 0.12;\n" +
            "        float size = 0.008 + layerDepth * 0.015;\n" +
            "        float brightness = 0.4 + layerDepth * 0.6;\n" +
            "        \n" +
            "        for (float i = 0.0; i < NUM_FLAKES; i++) {\n" +
            "            float id = layer * NUM_FLAKES + i;\n" +
            "            \n" +
            "            // Random position and movement\n" +
            "            float xOffset = hash(id * 1.234);\n" +
            "            float yOffset = hash(id * 2.345);\n" +
            "            float wobbleSpeed = hash(id * 3.456) * 2.0 + 1.0;\n" +
            "            float wobbleAmp = hash(id * 4.567) * 0.03 + 0.01;\n" +
            "            \n" +
            "            // Position with falling and horizontal wobble\n" +
            "            float x = xOffset * u_Aspect + sin(u_Time * wobbleSpeed + id) * wobbleAmp;\n" +
            "            float y = mod(yOffset + u_Time * speed, 1.3) - 0.15;\n" +
            "            \n" +
            "            vec2 flakePos = vec2(x, y);\n" +
            "            float flake = snowflake(uv, flakePos, size);\n" +
            "            \n" +
            "            // Twinkling effect\n" +
            "            float twinkle = sin(u_Time * 3.0 + id * 0.5) * 0.3 + 0.7;\n" +
            "            flake *= twinkle * brightness;\n" +
            "            \n" +
            "            // Soft white/blue color\n" +
            "            vec3 flakeColor = vec3(0.9, 0.95, 1.0);\n" +
            "            color += flakeColor * flake;\n" +
            "            totalAlpha += flake * 0.5;\n" +
            "        }\n" +
            "    }\n" +
            "    \n" +
            "    // Clamp and apply global alpha\n" +
            "    color = clamp(color, 0.0, 1.0);\n" +
            "    totalAlpha = clamp(totalAlpha, 0.0, 0.8) * u_Alpha;\n" +
            "    \n" +
            "    fragColor = vec4(color, totalAlpha);\n" +
            "}\n";

        int vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode);

        if (vertexShader == 0 || fragmentShader == 0) {
            Log.e(TAG, "Error compiling snowfall shaders");
            return;
        }

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vertexShader);
        GLES30.glAttachShader(shaderProgram, fragmentShader);
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
        uTimeLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Time");
        uAspectLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Aspect");
        uAlphaLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Alpha");

        ByteBuffer bb = ByteBuffer.allocateDirect(QUAD_VERTICES.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(QUAD_VERTICES);
        vertexBuffer.position(0);

        initialized = true;
        Log.d(TAG, "ChristmasSnowfall shaders compiled OK");
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
        if (time > 1000f) time -= 1000f;
    }

    @Override
    public void draw() {
        if (!initialized || shaderProgram == 0 || alpha <= 0.01f) return;

        GLES30.glUseProgram(shaderProgram);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        vertexBuffer.position(0);
        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        GLES30.glUniform1f(uTimeLoc, time);
        GLES30.glUniform1f(uAspectLoc, aspectRatio);
        GLES30.glUniform1f(uAlphaLoc, alpha);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glUseProgram(0);
    }

    public void setAspectRatio(float ratio) { this.aspectRatio = ratio; }
    public void setAlpha(float alpha) { this.alpha = alpha; }
    public void show() { this.alpha = 1.0f; }
    public void hide() { this.alpha = 0.0f; }

    public void dispose() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
    }
}
