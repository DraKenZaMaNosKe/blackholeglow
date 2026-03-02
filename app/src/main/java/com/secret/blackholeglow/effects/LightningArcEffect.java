package com.secret.blackholeglow.effects;

import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

/**
 * LightningArcEffect - Arcos eléctricos en tiempo real con OpenGL ES 3.0
 *
 * Genera rayos fractales entre dos puntos usando subdivisión recursiva.
 * Renderiza quads con additive blending para el efecto de brillo.
 *
 * Inspirado en el sistema de partículas "dischargearc" de Wallpaper Engine.
 */
public class LightningArcEffect {
    private static final String TAG = "LightningArc";

    // ═══════════════════════════════════════════════════════════════
    // CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════
    private static final int MAX_ARCS = 6;
    private static final int SUBDIVISIONS = 6;          // 2^6 = 64 segments per arc
    private static final int POINTS_PER_ARC = (1 << SUBDIVISIONS) + 1; // 65
    private static final float ARC_WIDTH_CORE = 0.008f;  // Ancho del core
    private static final float ARC_WIDTH_GLOW = 0.035f;  // Ancho del glow
    private static final float FLICKER_INTERVAL = 0.05f;  // Re-randomize cada 50ms

    // 6 verts per segment (2 triangles = 1 quad), 3 floats per vert (x, y, alpha)
    private static final int FLOATS_PER_VERTEX = 3;
    private static final int MAX_SEGMENTS = POINTS_PER_ARC - 1;
    private static final int VERTS_PER_SEGMENT = 6;
    private static final int BUFFER_SIZE = MAX_ARCS * MAX_SEGMENTS * VERTS_PER_SEGMENT * FLOATS_PER_VERTEX * 2; // x2 for core+glow

    // ═══════════════════════════════════════════════════════════════
    // ARC DATA
    // ═══════════════════════════════════════════════════════════════
    private final float[] startX = new float[MAX_ARCS];
    private final float[] startY = new float[MAX_ARCS];
    private final float[] endX = new float[MAX_ARCS];
    private final float[] endY = new float[MAX_ARCS];
    private final float[] displacement = new float[MAX_ARCS];
    private final float[] life = new float[MAX_ARCS];
    private final float[] maxLife = new float[MAX_ARCS];
    private final boolean[] active = new boolean[MAX_ARCS];
    private int arcCount = 0;

    // Fractal points buffer (reused)
    private final float[] pointsX = new float[POINTS_PER_ARC];
    private final float[] pointsY = new float[POINTS_PER_ARC];

    // ═══════════════════════════════════════════════════════════════
    // GL RESOURCES
    // ═══════════════════════════════════════════════════════════════
    private int shaderProgram = 0;
    private int vbo = 0;
    private FloatBuffer vertexBuffer;
    private float[] vertexData;

    // Uniform locations
    private int uMVPMatrix, uCoreColor, uGlowColor, uIntensity, uTime;
    // Attribute locations
    private int aPosition, aAlpha;

    private final float[] mvpMatrix = new float[16];
    private float elapsedTime = 0f;
    private float flickerTimer = 0f;
    private boolean needsRegen = true;

    private final Random random = new Random();

    // Colors
    private float[] coreColor = {1.0f, 1.0f, 1.0f};       // Blanco
    private float[] glowColor = {0.3f, 0.6f, 1.0f};       // Azul eléctrico

    // ═══════════════════════════════════════════════════════════════
    // VERTEX & FRAGMENT SHADERS (inline for simplicity)
    // ═══════════════════════════════════════════════════════════════
    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "in vec2 a_Position;\n" +
        "in float a_Alpha;\n" +
        "uniform mat4 u_MVPMatrix;\n" +
        "out float v_Alpha;\n" +
        "void main() {\n" +
        "    v_Alpha = a_Alpha;\n" +
        "    gl_Position = u_MVPMatrix * vec4(a_Position, 0.0, 1.0);\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "in float v_Alpha;\n" +
        "uniform vec3 u_CoreColor;\n" +
        "uniform vec3 u_GlowColor;\n" +
        "uniform float u_Intensity;\n" +
        "uniform float u_Time;\n" +
        "out vec4 fragColor;\n" +
        "void main() {\n" +
        "    float flicker = 0.8 + 0.2 * sin(u_Time * 25.0 + v_Alpha * 8.0);\n" +
        "    vec3 color = mix(u_GlowColor, u_CoreColor, v_Alpha);\n" +
        "    float alpha = v_Alpha * u_Intensity * flicker;\n" +
        "    fragColor = vec4(color * alpha, alpha);\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════
    // INICIALIZACIÓN
    // ═══════════════════════════════════════════════════════════════

    public void initialize() {
        // Compile shaders
        int vertShader = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragShader = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        if (vertShader == 0 || fragShader == 0) {
            Log.e(TAG, "Failed to compile shaders");
            return;
        }

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vertShader);
        GLES30.glAttachShader(shaderProgram, fragShader);
        GLES30.glLinkProgram(shaderProgram);

        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(shaderProgram, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Program link failed: " + GLES30.glGetProgramInfoLog(shaderProgram));
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
            return;
        }

        GLES30.glDeleteShader(vertShader);
        GLES30.glDeleteShader(fragShader);

        // Get locations
        aPosition = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        aAlpha = GLES30.glGetAttribLocation(shaderProgram, "a_Alpha");
        uMVPMatrix = GLES30.glGetUniformLocation(shaderProgram, "u_MVPMatrix");
        uCoreColor = GLES30.glGetUniformLocation(shaderProgram, "u_CoreColor");
        uGlowColor = GLES30.glGetUniformLocation(shaderProgram, "u_GlowColor");
        uIntensity = GLES30.glGetUniformLocation(shaderProgram, "u_Intensity");
        uTime = GLES30.glGetUniformLocation(shaderProgram, "u_Time");

        // Create VBO
        int[] buffers = new int[1];
        GLES30.glGenBuffers(1, buffers, 0);
        vbo = buffers[0];

        // Allocate vertex data
        vertexData = new float[BUFFER_SIZE];
        vertexBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        // Identity MVP (NDC coordinates)
        Matrix.setIdentityM(mvpMatrix, 0);

        Log.d(TAG, "⚡ LightningArcEffect initialized (max " + MAX_ARCS + " arcs, " + POINTS_PER_ARC + " points each)");
    }

    // ═══════════════════════════════════════════════════════════════
    // CONFIGURACIÓN DE COLORES
    // ═══════════════════════════════════════════════════════════════

    public void setColors(float[] core, float[] glow) {
        this.coreColor = core;
        this.glowColor = glow;
    }

    // ═══════════════════════════════════════════════════════════════
    // SPAWN ARCS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Agrega un arco eléctrico persistente entre dos puntos (NDC coords: -1 to 1)
     */
    public void addArc(float sx, float sy, float ex, float ey, float displace, float lifetime) {
        int slot = -1;
        for (int i = 0; i < MAX_ARCS; i++) {
            if (!active[i]) { slot = i; break; }
        }
        if (slot == -1) slot = arcCount % MAX_ARCS; // Overwrite oldest

        startX[slot] = sx;
        startY[slot] = sy;
        endX[slot] = ex;
        endY[slot] = ey;
        displacement[slot] = displace;
        life[slot] = lifetime;
        maxLife[slot] = lifetime;
        active[slot] = true;
        arcCount++;
        needsRegen = true;
    }

    /**
     * Agrega un arco permanente (no expira)
     */
    public void addPermanentArc(float sx, float sy, float ex, float ey, float displace) {
        addArc(sx, sy, ex, ey, displace, Float.MAX_VALUE);
    }

    /**
     * Limpia todos los arcos
     */
    public void clearArcs() {
        for (int i = 0; i < MAX_ARCS; i++) active[i] = false;
        arcCount = 0;
    }

    // ═══════════════════════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════════════════════

    public void update(float deltaTime) {
        if (shaderProgram == 0) return;

        elapsedTime += deltaTime;
        flickerTimer += deltaTime;

        // Update lifetimes
        for (int i = 0; i < MAX_ARCS; i++) {
            if (!active[i]) continue;
            if (life[i] != Float.MAX_VALUE) {
                life[i] -= deltaTime;
                if (life[i] <= 0) {
                    active[i] = false;
                }
            }
        }

        // Re-generate fractal paths periodically for flicker
        if (flickerTimer >= FLICKER_INTERVAL) {
            flickerTimer = 0f;
            needsRegen = true;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // DRAW
    // ═══════════════════════════════════════════════════════════════

    public void draw() {
        if (shaderProgram == 0) return;

        // Count active arcs
        int activeCount = 0;
        for (int i = 0; i < MAX_ARCS; i++) {
            if (active[i]) activeCount++;
        }
        if (activeCount == 0) return;

        // Regenerate paths if needed
        if (needsRegen) {
            needsRegen = false;
            buildVertexData();
        }

        // Save GL state
        int[] prevBlendSrc = new int[1], prevBlendDst = new int[1];
        GLES30.glGetIntegerv(GLES30.GL_BLEND_SRC_ALPHA, prevBlendSrc, 0);
        GLES30.glGetIntegerv(GLES30.GL_BLEND_DST_ALPHA, prevBlendDst, 0);

        // Additive blending (the secret sauce!)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE);
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);

        GLES30.glUseProgram(shaderProgram);

        // Uniforms
        GLES30.glUniformMatrix4fv(uMVPMatrix, 1, false, mvpMatrix, 0);
        GLES30.glUniform1f(uTime, elapsedTime);

        // Upload vertex data
        vertexBuffer.position(0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, totalVertices * FLOATS_PER_VERTEX * 4,
                vertexBuffer, GLES30.GL_DYNAMIC_DRAW);

        // Attributes: [x, y, alpha] = stride 12 bytes
        int stride = FLOATS_PER_VERTEX * 4;
        GLES30.glEnableVertexAttribArray(aPosition);
        GLES30.glVertexAttribPointer(aPosition, 2, GLES30.GL_FLOAT, false, stride, 0);
        GLES30.glEnableVertexAttribArray(aAlpha);
        GLES30.glVertexAttribPointer(aAlpha, 1, GLES30.GL_FLOAT, false, stride, 8);

        // Draw glow layer first (wider, dimmer)
        GLES30.glUniform3fv(uCoreColor, 1, coreColor, 0);
        GLES30.glUniform3fv(uGlowColor, 1, glowColor, 0);
        GLES30.glUniform1f(uIntensity, 0.4f);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, glowOffset, glowVertCount);

        // Draw core layer (thinner, brighter)
        GLES30.glUniform1f(uIntensity, 1.2f);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, coreOffset, coreVertCount);

        // Cleanup
        GLES30.glDisableVertexAttribArray(aPosition);
        GLES30.glDisableVertexAttribArray(aAlpha);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);

        // Restore GL state
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
    }

    // ═══════════════════════════════════════════════════════════════
    // FRACTAL LIGHTNING GENERATION
    // ═══════════════════════════════════════════════════════════════

    private int totalVertices = 0;
    private int glowOffset = 0, glowVertCount = 0;
    private int coreOffset = 0, coreVertCount = 0;

    private void buildVertexData() {
        int offset = 0;

        // Pass 1: Glow (wide)
        glowOffset = 0;
        for (int i = 0; i < MAX_ARCS; i++) {
            if (!active[i]) continue;
            generateFractalPath(startX[i], startY[i], endX[i], endY[i], displacement[i]);
            float lifeRatio = (life[i] == Float.MAX_VALUE) ? 1f : Math.min(life[i] / maxLife[i], 1f);
            offset = writeQuadStrip(offset, ARC_WIDTH_GLOW, lifeRatio * 0.6f);
        }
        glowVertCount = offset / FLOATS_PER_VERTEX - glowOffset;

        // Pass 2: Core (thin)
        coreOffset = offset / FLOATS_PER_VERTEX;
        for (int i = 0; i < MAX_ARCS; i++) {
            if (!active[i]) continue;
            generateFractalPath(startX[i], startY[i], endX[i], endY[i], displacement[i]);
            float lifeRatio = (life[i] == Float.MAX_VALUE) ? 1f : Math.min(life[i] / maxLife[i], 1f);
            offset = writeQuadStrip(offset, ARC_WIDTH_CORE, lifeRatio);
        }
        coreVertCount = offset / FLOATS_PER_VERTEX - coreOffset;

        totalVertices = offset / FLOATS_PER_VERTEX;

        // Upload
        vertexBuffer.position(0);
        vertexBuffer.put(vertexData, 0, offset);
        vertexBuffer.position(0);
    }

    /**
     * Genera un path fractal entre dos puntos usando subdivisión de punto medio.
     */
    private void generateFractalPath(float sx, float sy, float ex, float ey, float maxDisplace) {
        pointsX[0] = sx;
        pointsY[0] = sy;
        pointsX[POINTS_PER_ARC - 1] = ex;
        pointsY[POINTS_PER_ARC - 1] = ey;

        int step = POINTS_PER_ARC - 1;
        float currentDisplace = maxDisplace;

        while (step > 1) {
            int halfStep = step / 2;
            for (int i = halfStep; i < POINTS_PER_ARC - 1; i += step) {
                int prev = i - halfStep;
                int next = i + halfStep;

                // Midpoint
                pointsX[i] = (pointsX[prev] + pointsX[next]) * 0.5f;
                pointsY[i] = (pointsY[prev] + pointsY[next]) * 0.5f;

                // Perpendicular displacement
                float dx = pointsX[next] - pointsX[prev];
                float dy = pointsY[next] - pointsY[prev];
                float len = (float) Math.sqrt(dx * dx + dy * dy);
                if (len < 0.0001f) len = 0.0001f;

                // Normal perpendicular
                float nx = -dy / len;
                float ny = dx / len;

                float offset = (random.nextFloat() * 2f - 1f) * currentDisplace;
                pointsX[i] += nx * offset;
                pointsY[i] += ny * offset;
            }

            currentDisplace *= 0.55f; // Reduce displacement each level
            step = halfStep;
        }
    }

    /**
     * Escribe quads a lo largo del path fractal generado.
     * Cada segmento se convierte en un quad (2 triángulos).
     */
    private int writeQuadStrip(int offset, float width, float alphaMultiplier) {
        for (int i = 0; i < POINTS_PER_ARC - 1; i++) {
            float x0 = pointsX[i];
            float y0 = pointsY[i];
            float x1 = pointsX[i + 1];
            float y1 = pointsY[i + 1];

            // Direction
            float dx = x1 - x0;
            float dy = y1 - y0;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len < 0.00001f) continue;

            // Perpendicular normal
            float nx = (-dy / len) * width;
            float ny = (dx / len) * width;

            // Alpha: stronger in center, weaker at endpoints
            float t0 = (float) i / (POINTS_PER_ARC - 1);
            float t1 = (float) (i + 1) / (POINTS_PER_ARC - 1);
            // Parabolic falloff: 1 at center, 0 at edges
            float a0 = (1f - (2f * t0 - 1f) * (2f * t0 - 1f)) * alphaMultiplier;
            float a1 = (1f - (2f * t1 - 1f) * (2f * t1 - 1f)) * alphaMultiplier;
            // Minimum alpha so endpoints are still slightly visible
            a0 = Math.max(a0, 0.15f * alphaMultiplier);
            a1 = Math.max(a1, 0.15f * alphaMultiplier);

            // Triangle 1: (0-left, 0-right, 1-left)
            offset = putVertex(offset, x0 - nx, y0 - ny, a0 * 0.3f);  // edge = dimmer
            offset = putVertex(offset, x0 + nx, y0 + ny, a0 * 0.3f);
            offset = putVertex(offset, x1 - nx, y1 - ny, a1 * 0.3f);

            // Triangle 2: (0-right, 1-right, 1-left)
            offset = putVertex(offset, x0 + nx, y0 + ny, a0 * 0.3f);
            offset = putVertex(offset, x1 + nx, y1 + ny, a1 * 0.3f);
            offset = putVertex(offset, x1 - nx, y1 - ny, a1 * 0.3f);
        }
        return offset;
    }

    private int putVertex(int offset, float x, float y, float alpha) {
        if (offset + 2 < vertexData.length) {
            vertexData[offset++] = x;
            vertexData[offset++] = y;
            vertexData[offset++] = alpha;
        }
        return offset;
    }

    // ═══════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════

    public void release() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        if (vbo != 0) {
            GLES30.glDeleteBuffers(1, new int[]{vbo}, 0);
            vbo = 0;
        }
        Log.d(TAG, "⚡ LightningArcEffect released");
    }

    // ═══════════════════════════════════════════════════════════════
    // SHADER COMPILATION
    // ═══════════════════════════════════════════════════════════════

    private int compileShader(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Shader compile error: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }
}
