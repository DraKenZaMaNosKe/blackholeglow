package com.secret.blackholeglow.video;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ⚡ AuraEnergy3D — Fullscreen quad con shader de energía radial animada
 *
 * Dibuja un quad fullscreen. El shader radial define dónde aparece el efecto
 * usando uCenter + uRadius. El efecto se difumina libremente sin bordes.
 */
public class AuraEnergy3D {
    private static final String TAG = "AuraEnergy3D";

    // GL resources
    private int shaderProgram;
    private FloatBuffer quadBuffer;

    // Uniforms
    private int uMVPMatrixLoc;
    private int uTimeLoc;
    private int uColorLoc;
    private int uIntensityLoc;
    private int uCenterLoc;
    private int uRadiusLoc;

    // Animation state
    private float time = 0f;

    // Matrices
    private final float[] projMatrix = new float[16];

    // Effect parameters — adjustable via calibration
    private float centerX = 0.326f;
    private float centerY = 0.556f;
    private float radius = 1.1f;
    private float intensity = 1.5f;
    private float colorR = 0.4f, colorG = 1.0f, colorB = 0.5f;

    // Ortho projection bounds (Blender coordinate space)
    private static final float ORTHO_LEFT = -1.117f;
    private static final float ORTHO_RIGHT = 1.117f;
    private static final float ORTHO_BOTTOM = -2.0f;
    private static final float ORTHO_TOP = 2.0f;

    // ═══════════════════════════════════════════════════════════════
    // SHADERS
    // ═══════════════════════════════════════════════════════════════

    private static final String VERTEX_SHADER =
        "precision highp float;\n" +
        "attribute vec3 aPosition;\n" +
        "uniform mat4 uMVPMatrix;\n" +
        "varying vec2 vWorldPos;\n" +
        "void main() {\n" +
        "    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);\n" +
        "    vWorldPos = aPosition.xy;\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "uniform float uTime;\n" +
        "uniform vec3 uColor;\n" +
        "uniform float uIntensity;\n" +
        "uniform vec2 uCenter;\n" +
        "uniform float uRadius;\n" +
        "varying vec2 vWorldPos;\n" +
        "\n" +
        "void main() {\n" +
        "    float t = uTime;\n" +
        "    vec2 delta = vWorldPos - uCenter;\n" +
        "    float dist = length(delta);\n" +
        "    float angle = atan(delta.y, delta.x);\n" +
        "\n" +
        "    float wave1 = sin(dist * 8.0 - t * 3.0) * 0.5 + 0.5;\n" +
        "    float wave2 = sin(dist * 12.0 - t * 4.5 + 1.5) * 0.5 + 0.5;\n" +
        "    float wave3 = sin(dist * 5.0 - t * 2.0 + 3.0) * 0.5 + 0.5;\n" +
        "\n" +
        "    float ang1 = sin(angle * 3.0 + t * 1.2) * 0.5 + 0.5;\n" +
        "    float ang2 = sin(angle * 5.0 - t * 0.8 + 2.0) * 0.5 + 0.5;\n" +
        "\n" +
        "    float energy = wave1 * 0.4 + wave2 * 0.3 + wave3 * 0.3;\n" +
        "    energy *= 0.7 + 0.3 * ang1;\n" +
        "    energy += 0.15 * ang2 * wave1;\n" +
        "\n" +
        "    float falloff = 1.0 - smoothstep(0.0, uRadius, dist);\n" +
        "    energy *= falloff;\n" +
        "\n" +
        "    if (energy < 0.01) discard;\n" +
        "\n" +
        "    float pulse = 0.8 + 0.2 * sin(t * 2.5);\n" +
        "\n" +
        "    float alpha = energy * pulse * uIntensity;\n" +
        "    alpha = clamp(alpha, 0.0, 1.0);\n" +
        "\n" +
        "    vec3 col = uColor * (0.5 + 0.5 * energy);\n" +
        "    col += vec3(0.1, 0.4, 0.15) * wave1 * falloff;\n" +
        "    col += vec3(0.0, 0.2, 0.15) * (1.0 - falloff);\n" +
        "    col += vec3(0.3, 0.6, 0.2) * pow(energy, 2.0);\n" +
        "\n" +
        "    gl_FragColor = vec4(col, alpha);\n" +
        "}\n";

    public AuraEnergy3D(Context context) {
        compileShader();
        createFullscreenQuad();
        setupMatrices();
        Log.d(TAG, "⚡ AuraEnergy3D initialized (fullscreen quad)");
    }

    private void compileShader() {
        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vertexShader);
        GLES30.glAttachShader(shaderProgram, fragmentShader);
        GLES30.glLinkProgram(shaderProgram);

        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(shaderProgram, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            String error = GLES30.glGetProgramInfoLog(shaderProgram);
            Log.e(TAG, "Shader link error: " + error);
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }

        GLES30.glDeleteShader(vertexShader);
        GLES30.glDeleteShader(fragmentShader);

        uMVPMatrixLoc = GLES30.glGetUniformLocation(shaderProgram, "uMVPMatrix");
        uTimeLoc = GLES30.glGetUniformLocation(shaderProgram, "uTime");
        uColorLoc = GLES30.glGetUniformLocation(shaderProgram, "uColor");
        uIntensityLoc = GLES30.glGetUniformLocation(shaderProgram, "uIntensity");
        uCenterLoc = GLES30.glGetUniformLocation(shaderProgram, "uCenter");
        uRadiusLoc = GLES30.glGetUniformLocation(shaderProgram, "uRadius");
    }

    private void createFullscreenQuad() {
        float[] vertices = {
            ORTHO_LEFT,  ORTHO_BOTTOM, 0f,
            ORTHO_RIGHT, ORTHO_BOTTOM, 0f,
            ORTHO_LEFT,  ORTHO_TOP,    0f,
            ORTHO_RIGHT, ORTHO_TOP,    0f,
        };

        quadBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
        quadBuffer.put(vertices).position(0);
    }

    private void setupMatrices() {
        Matrix.setIdentityM(projMatrix, 0);
        Matrix.orthoM(projMatrix, 0, ORTHO_LEFT, ORTHO_RIGHT, ORTHO_BOTTOM, ORTHO_TOP, -1f, 1f);
    }

    public void update(float deltaTime) {
        time += deltaTime;
        if (time > 60f) time -= 60f;
    }

    public void draw() {
        if (shaderProgram == 0 || quadBuffer == null) return;

        GLES30.glUseProgram(shaderProgram);

        GLES30.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, projMatrix, 0);
        GLES30.glUniform1f(uTimeLoc, time);
        GLES30.glUniform3f(uColorLoc, colorR, colorG, colorB);
        GLES30.glUniform1f(uIntensityLoc, intensity);
        GLES30.glUniform2f(uCenterLoc, centerX, centerY);
        GLES30.glUniform1f(uRadiusLoc, radius);

        int positionHandle = GLES30.glGetAttribLocation(shaderProgram, "aPosition");
        GLES30.glEnableVertexAttribArray(positionHandle);
        quadBuffer.position(0);
        GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 0, quadBuffer);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glDisableVertexAttribArray(positionHandle);
    }

    // ═══════════════════════════════════════════════════════════════
    // Calibration setters
    // ═══════════════════════════════════════════════════════════════

    public void setCenter(float x, float y) {
        this.centerX = x;
        this.centerY = y;
    }

    public void setRadius(float r) {
        this.radius = Math.max(0.1f, r);
    }

    public void setIntensity(float i) {
        this.intensity = Math.max(0.0f, i);
    }

    public void setColor(float r, float g, float b) {
        this.colorR = r;
        this.colorG = g;
        this.colorB = b;
    }

    public float getCenterX() { return centerX; }
    public float getCenterY() { return centerY; }
    public float getRadius() { return radius; }
    public float getIntensity() { return intensity; }
    public float getColorR() { return colorR; }
    public float getColorG() { return colorG; }
    public float getColorB() { return colorB; }

    /** Converts normalized screen coords (0-1) to Blender world coords */
    public static float screenToWorldX(float nx) {
        return ORTHO_LEFT + nx * (ORTHO_RIGHT - ORTHO_LEFT);
    }

    public static float screenToWorldY(float ny) {
        // ny=0 is top, ny=1 is bottom → flip for Blender Y (bottom=ORTHO_BOTTOM)
        return ORTHO_TOP - ny * (ORTHO_TOP - ORTHO_BOTTOM);
    }

    public void dispose() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        Log.d(TAG, "⚡ AuraEnergy3D disposed");
    }

    private static int loadShader(int type, String shaderCode) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            String error = GLES30.glGetShaderInfoLog(shader);
            Log.e(TAG, "Shader compile error: " + error);
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }
}
