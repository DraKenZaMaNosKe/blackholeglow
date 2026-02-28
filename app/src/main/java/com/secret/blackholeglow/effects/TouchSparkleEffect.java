package com.secret.blackholeglow.effects;

import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Rainbow Touch Sparkle Particles.
 *
 * Zero-allocation particle system using fixed-size arrays and a ring buffer.
 * Each particle gets a random hue from the rainbow spectrum.
 * Renders with GL_POINTS + additive blending in a single draw call.
 * Includes shimmer/twinkle animation via time uniform.
 */
public class TouchSparkleEffect {
    private static final String TAG = "TouchSparkle";

    private static final int MAX_PARTICLES = 50;

    // Per-particle data (struct-of-arrays)
    private final float[] posX = new float[MAX_PARTICLES];
    private final float[] posY = new float[MAX_PARTICLES];
    private final float[] velX = new float[MAX_PARTICLES];
    private final float[] velY = new float[MAX_PARTICLES];
    private final float[] life = new float[MAX_PARTICLES];
    private final float[] maxLife = new float[MAX_PARTICLES];
    private final float[] hue = new float[MAX_PARTICLES];    // 0..1 rainbow hue
    private final float[] pSize = new float[MAX_PARTICLES];
    private final boolean[] alive = new boolean[MAX_PARTICLES];

    private int ringHead = 0;

    // GL resources
    private int shaderProgram = 0;
    private int aPositionLoc;
    private int aColorDataLoc;   // vec4: hue, alpha, lifeRatio, pointSize
    private int uTimeLoc;

    // 6 floats per particle: x, y, hue, alpha, lifeRatio, pointSize
    private static final int FLOATS_PER_PARTICLE = 6;
    private final float[] packedData = new float[MAX_PARTICLES * FLOATS_PER_PARTICLE];
    private FloatBuffer vertexBuffer;

    // Time (wraps to avoid overflow)
    private float time = 0f;
    private static final float TIME_WRAP = 628.318f;  // ~100 * TAU

    // Constants
    private static final float PARTICLE_LIFETIME = 1.2f;
    private static final float BASE_SPEED = 0.35f;
    private static final float DECELERATION = 0.93f;
    private static final float BASE_POINT_SIZE = 22.0f;

    // Hue offset: slowly cycles so consecutive touches get different colors
    private float hueOffset = 0f;

    // LCG random (zero allocations)
    private long rngState = System.nanoTime();

    // ═══════════════════════════════════════════════════════════════
    // SHADERS
    // ═══════════════════════════════════════════════════════════════

    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "in vec2 aPosition;\n" +
        "in vec4 aColorData;\n" +  // hue, alpha, lifeRatio, pointSize
        "uniform float uTime;\n" +
        "out float vHue;\n" +
        "out float vAlpha;\n" +
        "out float vLife;\n" +
        "out float vShimmer;\n" +
        "void main() {\n" +
        "    gl_Position = vec4(aPosition, 0.0, 1.0);\n" +
        "    gl_PointSize = aColorData.w;\n" +
        "    vHue = aColorData.x;\n" +
        "    vAlpha = aColorData.y;\n" +
        "    vLife = aColorData.z;\n" +
        // Shimmer: unique per-particle using position as seed
        "    vShimmer = sin(uTime * 12.0 + aPosition.x * 37.0 + aPosition.y * 53.0);\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "in float vHue;\n" +
        "in float vAlpha;\n" +
        "in float vLife;\n" +
        "in float vShimmer;\n" +
        "out vec4 fragColor;\n" +
        "\n" +
        // HSV to RGB (optimized, no branches)
        "vec3 hsv2rgb(float h) {\n" +
        "    vec3 c = clamp(abs(mod(h * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0, 0.0, 1.0);\n" +
        "    return c;\n" +
        "}\n" +
        "\n" +
        "void main() {\n" +
        "    vec2 center = gl_PointCoord - vec2(0.5);\n" +
        "    float dist = length(center) * 2.0;\n" +
        "    if (dist > 1.0) discard;\n" +
        "\n" +
        // Multi-layer glow
        "    float soft = 1.0 - dist * dist;\n" +           // outer soft glow
        "    float core = exp(-dist * 5.0);\n" +             // bright core
        "    float ring = exp(-pow(dist - 0.3, 2.0) * 20.0);\n" + // subtle ring
        "\n" +
        // Rainbow color with saturated edges, white-hot center
        "    vec3 rainbow = hsv2rgb(vHue);\n" +
        // Boost saturation: make colors more vivid
        "    rainbow = mix(rainbow, rainbow * rainbow, 0.3);\n" +
        "    vec3 color = mix(rainbow, vec3(1.0), core * 0.8);\n" +
        "\n" +
        // Shimmer twinkle (±20% brightness variation)
        "    float shimmer = 1.0 + vShimmer * 0.2;\n" +
        "    color *= shimmer;\n" +
        "\n" +
        // Combine layers
        "    float glow = soft + ring * 0.3;\n" +
        "    float a = glow * vAlpha;\n" +
        "    fragColor = vec4(color * a, a);\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════

    public TouchSparkleEffect() {
        vertexBuffer = ByteBuffer.allocateDirect(MAX_PARTICLES * FLOATS_PER_PARTICLE * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
    }

    // ═══════════════════════════════════════════════════════════════
    // SPAWN
    // ═══════════════════════════════════════════════════════════════

    public void spawn(float ndcX, float ndcY, int count) {
        for (int i = 0; i < count; i++) {
            int idx = ringHead;
            ringHead = (ringHead + 1) % MAX_PARTICLES;

            // Direction: mostly upward with spread
            float angle;
            if (nextRandom() > 0.25f) {
                // Biased upward fan (45° to 135°)
                angle = (float) (Math.PI * 0.25f + nextRandom() * Math.PI * 0.5f);
            } else {
                // Full radial burst for sparkle variety
                angle = nextRandom() * (float) (Math.PI * 2.0);
            }

            float speed = BASE_SPEED * (0.3f + nextRandom() * 0.7f);

            posX[idx] = ndcX + (nextRandom() - 0.5f) * 0.03f;
            posY[idx] = ndcY + (nextRandom() - 0.5f) * 0.03f;
            velX[idx] = (float) Math.cos(angle) * speed;
            velY[idx] = (float) Math.sin(angle) * speed;

            float lt = PARTICLE_LIFETIME * (0.5f + nextRandom() * 0.5f);
            life[idx] = lt;
            maxLife[idx] = lt;

            // Rainbow hue: offset cycles slowly so each touch burst shifts color
            hue[idx] = (hueOffset + nextRandom() * 0.3f) % 1.0f;
            hueOffset += 0.07f;
            if (hueOffset > 1.0f) hueOffset -= 1.0f;

            pSize[idx] = BASE_POINT_SIZE * (0.5f + nextRandom() * 0.5f);
            alive[idx] = true;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════════════════════

    public void update(float deltaTime) {
        // Wrap time to avoid float precision loss
        time += deltaTime;
        if (time > TIME_WRAP) time -= TIME_WRAP;

        for (int i = 0; i < MAX_PARTICLES; i++) {
            if (!alive[i]) continue;

            posX[i] += velX[i] * deltaTime;
            posY[i] += velY[i] * deltaTime;

            velX[i] *= DECELERATION;
            velY[i] *= DECELERATION;

            // Add subtle gravity (particles float up then drift down gently)
            velY[i] -= 0.05f * deltaTime;

            life[i] -= deltaTime;
            if (life[i] <= 0f) {
                alive[i] = false;
                continue;
            }

            // Shrink over lifetime
            pSize[i] *= 0.993f;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // DRAW
    // ═══════════════════════════════════════════════════════════════

    public void draw() {
        int aliveCount = 0;
        for (int i = 0; i < MAX_PARTICLES; i++) {
            if (!alive[i]) continue;

            float lifeRatio = life[i] / maxLife[i];
            // Smooth fade: ease-in at start, ease-out at end
            float alpha = lifeRatio < 0.15f ? lifeRatio / 0.15f
                        : lifeRatio > 0.8f ? 1.0f
                        : 1.0f;
            // Fade out in last 30%
            if (lifeRatio < 0.3f) alpha = lifeRatio / 0.3f;

            int offset = aliveCount * FLOATS_PER_PARTICLE;
            packedData[offset]     = posX[i];
            packedData[offset + 1] = posY[i];
            packedData[offset + 2] = hue[i];
            packedData[offset + 3] = alpha;
            packedData[offset + 4] = lifeRatio;
            packedData[offset + 5] = pSize[i];
            aliveCount++;
        }

        if (aliveCount == 0) return;

        if (shaderProgram == 0) {
            initShader();
            if (shaderProgram == 0) return;
        }

        vertexBuffer.clear();
        vertexBuffer.put(packedData, 0, aliveCount * FLOATS_PER_PARTICLE);
        vertexBuffer.position(0);

        // GL state
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE);  // additive

        GLES30.glUseProgram(shaderProgram);
        GLES30.glUniform1f(uTimeLoc, time);

        int stride = FLOATS_PER_PARTICLE * 4;

        // aPosition: 2 floats at offset 0
        vertexBuffer.position(0);
        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, stride, vertexBuffer);

        // aColorData: 4 floats at offset 2 (hue, alpha, lifeRatio, pointSize)
        vertexBuffer.position(2);
        GLES30.glEnableVertexAttribArray(aColorDataLoc);
        GLES30.glVertexAttribPointer(aColorDataLoc, 4, GLES30.GL_FLOAT, false, stride, vertexBuffer);

        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, aliveCount);

        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glDisableVertexAttribArray(aColorDataLoc);

        // Restore
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
    }

    // ═══════════════════════════════════════════════════════════════
    // SHADER INIT
    // ═══════════════════════════════════════════════════════════════

    private void initShader() {
        int vs = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        if (vs == 0 || fs == 0) return;

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vs);
        GLES30.glAttachShader(shaderProgram, fs);
        GLES30.glLinkProgram(shaderProgram);

        int[] status = new int[1];
        GLES30.glGetProgramiv(shaderProgram, GLES30.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) {
            Log.e(TAG, "Link error: " + GLES30.glGetProgramInfoLog(shaderProgram));
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }

        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);

        if (shaderProgram != 0) {
            aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "aPosition");
            aColorDataLoc = GLES30.glGetAttribLocation(shaderProgram, "aColorData");
            uTimeLoc = GLES30.glGetUniformLocation(shaderProgram, "uTime");
            Log.d(TAG, "Rainbow shader OK");
        }
    }

    private int compileShader(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Compile error: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    // ═══════════════════════════════════════════════════════════════
    // RELEASE
    // ═══════════════════════════════════════════════════════════════

    public void release() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
            Log.d(TAG, "Released");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // FAST RANDOM (LCG)
    // ═══════════════════════════════════════════════════════════════

    private float nextRandom() {
        rngState = rngState * 6364136223846793005L + 1442695040888963407L;
        return ((rngState >>> 33) & 0x7FFFFFFFL) / (float) 0x80000000L;
    }
}
