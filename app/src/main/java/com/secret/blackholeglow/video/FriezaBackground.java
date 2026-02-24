package com.secret.blackholeglow.video;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * FriezaBackground - Radial speed lines (anime burst) puro shader.
 *
 * Efecto radial de lineas de velocidad estilo anime emanando de un punto central.
 * Adaptado de Shadertoy "Radial Speed Lines" (WtfXRj).
 * Sin imagen de fondo - solo las lineas sobre negro.
 */
public class FriezaBackground {
    private static final String TAG = "FriezaBackground";

    private int shaderProgram;
    private FloatBuffer quadVertices;
    private float time = 0f;
    private int screenWidth = 1, screenHeight = 1;

    // Cached uniform/attrib locations
    private int posLoc = -1, uvLoc = -1, timeLoc = -1, resLoc = -1;
    private int centerLoc = -1, speedLoc = -1, brightnessLoc = -1;

    // Effect parameters (touch-calibratable)
    private float centerX = 0.0f;      // center X in NDC (-1 to 1, 0=center)
    private float centerY = 0.3f;      // center Y in NDC (positive=up)
    private float speed = 0.07f;       // animation speed
    private float brightness = 1.0f;   // effect brightness/mix

    // Fullscreen quad (position + UV)
    private static final float[] QUAD_DATA = {
        -1f, -1f,  0f, 1f,
         1f, -1f,  1f, 1f,
        -1f,  1f,  0f, 0f,
         1f,  1f,  1f, 0f,
    };

    private static final String VERTEX_SHADER =
        "attribute vec2 aPosition;\n" +
        "attribute vec2 aTexCoord;\n" +
        "varying vec2 vUV;\n" +
        "void main() {\n" +
        "    gl_Position = vec4(aPosition, 0.0, 1.0);\n" +
        "    vUV = aTexCoord;\n" +
        "}\n";

    // Fragment shader: radial anime speed lines (no background image)
    private static final String FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "uniform float uTime;\n" +
        "uniform vec2 uResolution;\n" +
        "uniform vec2 uCenter;\n" +
        "uniform float uSpeed;\n" +
        "uniform float uBrightness;\n" +
        "varying vec2 vUV;\n" +
        "\n" +
        "float rand(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }\n" +
        "\n" +
        "float noise(vec2 p) {\n" +
        "    vec2 i = floor(p);\n" +
        "    vec2 f = fract(p);\n" +
        "    vec2 u = f * f * (3.0 - 2.0 * f);\n" +
        "    float a = rand(i);\n" +
        "    float b = rand(i + vec2(1.0, 0.0));\n" +
        "    float c = rand(i + vec2(0.0, 1.0));\n" +
        "    float d = rand(i + vec2(1.0, 1.0));\n" +
        "    return (a + (b - a) * u.x + (c - a) * u.y + (a - b - c + d) * u.x * u.y) / 4.0;\n" +
        "}\n" +
        "\n" +
        "float mirror(float t, float r) {\n" +
        "    t = fract(t + r);\n" +
        "    return 2.0 * abs(t - 0.5);\n" +
        "}\n" +
        "\n" +
        "float radialNoise(float t, float d, float time) {\n" +
        "    d = pow(d, 0.01);\n" +
        "    float doff = -time;\n" +
        "    vec2 p1 = vec2(mirror(t, 0.1), d + doff);\n" +
        "    float f1 = noise(p1 * 45.0);\n" +
        "    vec2 p2 = 2.1 * vec2(mirror(t, 0.4), d + doff);\n" +
        "    float f2 = noise(p2 * 45.0);\n" +
        "    return pow((f1 + 0.5 * f2) * 4.0, 1.0);\n" +
        "}\n" +
        "\n" +
        "vec3 colorize(float x) {\n" +
        "    x = clamp(x, 0.0, 1.0);\n" +
        "    vec3 c = mix(vec3(0.0, 0.2, 0.0), vec3(0.3, 1.0, 0.2), x);\n" +
        "    c = mix(c, vec3(0.8, 1.0, 0.7), x * 4.0 - 3.0) * x;\n" +
        "    c = max(c, vec3(0.0));\n" +
        "    c = mix(c, vec3(0.1, 0.6, 0.15), smoothstep(1.0, 0.2, x) * smoothstep(0.15, 0.9, x));\n" +
        "    return c;\n" +
        "}\n" +
        "\n" +
        "void main() {\n" +
        "    float screenAspect = uResolution.x / uResolution.y;\n" +
        "\n" +
        // Radial speed lines (centered on uCenter)
        "    vec2 uv = (vUV * 2.0 - 1.0) * vec2(screenAspect, 1.0) * 0.5;\n" +
        "    uv -= uCenter;\n" +
        "    float d = dot(uv, uv);\n" +
        "    float t = atan(uv.y, uv.x) / 6.28318;\n" +
        "    float v = radialNoise(t, d, uTime * uSpeed);\n" +
        "    v = v * 2.5 - 1.4;\n" +
        "    v = mix(0.0, v, 0.8 * smoothstep(0.0, 0.8, d));\n" +
        "    vec3 lines = colorize(v);\n" +
        "\n" +
        "    gl_FragColor = vec4(clamp(lines * uBrightness, 0.0, 1.0), 1.0);\n" +
        "}\n";

    public FriezaBackground() {
        initialize();
    }

    private void initialize() {
        ByteBuffer bb = ByteBuffer.allocateDirect(QUAD_DATA.length * 4);
        bb.order(ByteOrder.nativeOrder());
        quadVertices = bb.asFloatBuffer();
        quadVertices.put(QUAD_DATA);
        quadVertices.position(0);

        shaderProgram = buildProgram();
        if (shaderProgram != 0) {
            posLoc = GLES20.glGetAttribLocation(shaderProgram, "aPosition");
            uvLoc = GLES20.glGetAttribLocation(shaderProgram, "aTexCoord");
            timeLoc = GLES20.glGetUniformLocation(shaderProgram, "uTime");
            resLoc = GLES20.glGetUniformLocation(shaderProgram, "uResolution");
            centerLoc = GLES20.glGetUniformLocation(shaderProgram, "uCenter");
            speedLoc = GLES20.glGetUniformLocation(shaderProgram, "uSpeed");
            brightnessLoc = GLES20.glGetUniformLocation(shaderProgram, "uBrightness");
        }
    }

    public void update(float deltaTime) {
        time += deltaTime;
        // Wrap at 60s to prevent mediump float precision loss
        // (at speed=0.07, shader time max = 4.2, noise input max ~189 - safe for mediump)
        if (time > 60f) time -= 60f;
    }

    public void draw() {
        if (shaderProgram == 0) return;

        GLES20.glUseProgram(shaderProgram);

        GLES20.glUniform1f(timeLoc, time);
        GLES20.glUniform2f(resLoc, screenWidth, screenHeight);
        GLES20.glUniform2f(centerLoc, centerX, centerY);
        GLES20.glUniform1f(speedLoc, speed);
        GLES20.glUniform1f(brightnessLoc, brightness);

        quadVertices.position(0);
        GLES20.glEnableVertexAttribArray(posLoc);
        GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 16, quadVertices);

        quadVertices.position(2);
        GLES20.glEnableVertexAttribArray(uvLoc);
        GLES20.glVertexAttribPointer(uvLoc, 2, GLES20.GL_FLOAT, false, 16, quadVertices);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(posLoc);
        GLES20.glDisableVertexAttribArray(uvLoc);
    }

    public void setScreenSize(int w, int h) { screenWidth = w; screenHeight = h; }

    // Calibration getters/setters
    public float getCenterX() { return centerX; }
    public void setCenterX(float v) { centerX = v; }
    public float getCenterY() { return centerY; }
    public void setCenterY(float v) { centerY = v; }
    public float getSpeed() { return speed; }
    public void setSpeed(float v) { speed = Math.max(0.01f, v); }
    public float getBrightness() { return brightness; }
    public void setBrightness(float v) { brightness = Math.max(0.0f, v); }

    private int buildProgram() {
        int vs = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        if (vs == 0 || fs == 0) return 0;

        int prog = GLES20.glCreateProgram();
        GLES20.glAttachShader(prog, vs);
        GLES20.glAttachShader(prog, fs);
        GLES20.glLinkProgram(prog);

        int[] linked = new int[1];
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            Log.e(TAG, "Shader link error: " + GLES20.glGetProgramInfoLog(prog));
            GLES20.glDeleteProgram(prog);
            return 0;
        }
        GLES20.glDeleteShader(vs);
        GLES20.glDeleteShader(fs);
        return prog;
    }

    private int compileShader(int type, String source) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, source);
        GLES20.glCompileShader(s);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Shader error: " + GLES20.glGetShaderInfoLog(s));
            GLES20.glDeleteShader(s);
            return 0;
        }
        return s;
    }

    public void release() {
        if (shaderProgram != 0) {
            GLES20.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        Log.d(TAG, "FriezaBackground released");
    }
}
