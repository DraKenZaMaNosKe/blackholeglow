package com.secret.blackholeglow.effects;

import android.opengl.GLES30;
import android.util.Log;

import com.secret.blackholeglow.SceneObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Fondo procedural para el panel de control.
 * Negro profundo con ondas sutiles de luz en cyan oscuro y magenta tenue.
 * Zero texturas, solo math. ~1-2ms en Mali-G52.
 */
public class ProceduralPanelBackground implements SceneObject {
    private static final String TAG = "ProceduralPanelBg";

    private static final float TIME_CYCLE = 62.831853f; // 10 * TWO_PI

    private int shaderProgram = 0;
    private int aPositionLoc = -1;
    private int uTimeLoc = -1;
    private int uResolutionLoc = -1;
    private FloatBuffer vertexBuffer;

    private float time = 0f;
    private float screenWidth = 1f;
    private float screenHeight = 1f;
    private boolean initialized = false;

    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "in vec2 a_Position;\n" +
        "out vec2 v_UV;\n" +
        "void main() {\n" +
        "    v_UV = a_Position * 0.5 + 0.5;\n" +
        "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "in vec2 v_UV;\n" +
        "out vec4 fragColor;\n" +
        "uniform float u_Time;\n" +
        "uniform vec2 u_Resolution;\n" +
        "\n" +
        "void main() {\n" +
        "    vec2 uv = v_UV;\n" +
        "    float aspect = u_Resolution.x / u_Resolution.y;\n" +
        "    vec2 center = vec2(0.5, 0.5);\n" +
        "    vec2 p = uv - center;\n" +
        "    p.x *= aspect;\n" +
        "\n" +
        "    float dist = length(p);\n" +
        "    float t = u_Time;\n" +
        "\n" +
        // Ring 1: expanding concentric wave
        "    float ring1 = sin(dist * 12.0 - t * 1.2) * 0.5 + 0.5;\n" +
        "    ring1 = smoothstep(0.45, 0.55, ring1);\n" +
        "    ring1 *= exp(-dist * 3.0);\n" +
        "\n" +
        // Ring 2: slower, offset phase
        "    float ring2 = sin(dist * 8.0 - t * 0.8 + 1.57) * 0.5 + 0.5;\n" +
        "    ring2 = smoothstep(0.4, 0.6, ring2);\n" +
        "    ring2 *= exp(-dist * 2.5);\n" +
        "\n" +
        // Ring 3: ghost trail
        "    float ring3 = sin(dist * 15.0 - t * 1.5 + 3.14) * 0.5 + 0.5;\n" +
        "    ring3 = smoothstep(0.48, 0.52, ring3);\n" +
        "    ring3 *= exp(-dist * 4.0);\n" +
        "\n" +
        // Subtle angular variation
        "    float angle = atan(p.y, p.x);\n" +
        "    float angular = sin(angle * 3.0 + t * 0.5) * 0.15 + 0.85;\n" +
        "\n" +
        // Cyan dark: #003344 = (0.0, 0.2, 0.267)
        "    vec3 cyan = vec3(0.0, 0.2, 0.267);\n" +
        // Magenta tenue: #220033 = (0.133, 0.0, 0.2)
        "    vec3 magenta = vec3(0.133, 0.0, 0.2);\n" +
        "\n" +
        // Mix colors based on angle and time
        "    float colorMix = sin(angle * 2.0 + t * 0.3) * 0.5 + 0.5;\n" +
        "    vec3 ringColor1 = mix(cyan, magenta, colorMix);\n" +
        "    vec3 ringColor2 = mix(magenta, cyan, colorMix);\n" +
        "\n" +
        // Combine rings
        "    vec3 color = vec3(0.0);\n" +
        "    color += ringColor1 * ring1 * 0.7 * angular;\n" +
        "    color += ringColor2 * ring2 * 0.5 * angular;\n" +
        "    color += cyan * ring3 * 0.3;\n" +
        "\n" +
        // Gentle center glow
        "    float centerGlow = exp(-dist * dist * 8.0);\n" +
        "    color += mix(cyan, magenta, sin(t * 0.2) * 0.5 + 0.5) * centerGlow * 0.08;\n" +
        "\n" +
        // Dark vignette on edges
        "    float vignette = 1.0 - smoothstep(0.3, 0.85, dist);\n" +
        "    color *= vignette;\n" +
        "\n" +
        // Ensure deep black base
        "    fragColor = vec4(color, 1.0);\n" +
        "}\n";

    public ProceduralPanelBackground() {
        init();
    }

    private void init() {
        int vs = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        if (vs == 0 || fs == 0) {
            Log.e(TAG, "Failed to compile shaders");
            return;
        }

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vs);
        GLES30.glAttachShader(shaderProgram, fs);
        GLES30.glLinkProgram(shaderProgram);

        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(shaderProgram, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Link error: " + GLES30.glGetProgramInfoLog(shaderProgram));
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
            return;
        }

        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);

        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        uTimeLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Time");
        uResolutionLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Resolution");

        // Fullscreen quad: 2 triangles
        float[] vertices = {
            -1f, -1f,
             1f, -1f,
            -1f,  1f,
             1f,  1f
        };

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        initialized = true;
        Log.d(TAG, "Initialized");
    }

    private int compileShader(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
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

    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }

    @Override
    public void update(float deltaTime) {
        time += deltaTime;
        if (time > TIME_CYCLE) {
            time -= TIME_CYCLE;
        }
    }

    @Override
    public void draw() {
        if (!initialized || shaderProgram == 0) return;

        GLES30.glUseProgram(shaderProgram);

        GLES30.glUniform1f(uTimeLoc, time);
        GLES30.glUniform2f(uResolutionLoc, screenWidth, screenHeight);

        vertexBuffer.position(0);
        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glUseProgram(0);
    }

    public void release() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        initialized = false;
        Log.d(TAG, "Released");
    }
}
