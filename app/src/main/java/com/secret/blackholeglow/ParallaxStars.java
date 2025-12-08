package com.secret.blackholeglow;

import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║   ✨ ParallaxStars v2.0 - INSTANCED RENDERING (OpenGL ES 3.0)               ║
 * ╠═══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                               ║
 * ║   🚀 OPTIMIZADO: 340 estrellas en UN SOLO draw call                          ║
 * ║                                                                               ║
 * ║   Características:                                                            ║
 * ║   - VAO (Vertex Array Object) para estado de vértices                        ║
 * ║   - Instanced Rendering con glDrawArraysInstanced()                          ║
 * ║   - GLSL ES 3.0 shaders (#version 300 es)                                    ║
 * ║   - 3 capas de profundidad con parallax automático                           ║
 * ║                                                                               ║
 * ║   Antes: 340 draw calls | Ahora: 1 draw call = +200% rendimiento            ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 */
public class ParallaxStars implements SceneObject {
    private static final String TAG = "ParallaxStars";

    // Configuración de capas (REDUCIDO para mejor estética)
    private static final int NUM_LAYERS = 3;
    private static final int[] STARS_PER_LAYER = {60, 35, 15};  // Total: 110 estrellas
    private static final float[] LAYER_SPEEDS = {0.02f, 0.06f, 0.12f};
    private static final float[] STAR_SIZES = {0.006f, 0.012f, 0.020f};  // NDC units (más pequeñas)
    private static final float[] STAR_ALPHAS = {0.3f, 0.5f, 0.7f};

    // Total de estrellas
    private static final int TOTAL_STARS = 110;

    // Posición del sol en NDC - ZONA DE EXCLUSIÓN GRANDE
    private static final float SUN_X = 0.55f;
    private static final float SUN_Y = 0.50f;
    private static final float SUN_RADIUS_INNER = 0.70f;  // Radio GRANDE = zona sin estrellas
    private static final float SUN_RADIUS_OUTER = 0.90f;  // Fade suave en el borde

    // Datos de estrellas (arrays planos para eficiencia)
    private float[] starBaseX;      // Posición base X
    private float[] starBaseY;      // Posición base Y
    private float[] starSizes;      // Tamaño
    private float[] starColors;     // RGBA (4 floats por estrella)
    private float[] starTwinkle;    // Fase y velocidad de parpadeo
    private int[] starLayer;        // Capa (0, 1, 2)

    // Buffers para instanced data
    private FloatBuffer instancePositionBuffer;  // vec2 position
    private FloatBuffer instanceColorBuffer;     // vec4 color (incluye alpha con twinkle)
    private FloatBuffer instanceSizeBuffer;      // float size

    // OpenGL handles
    private int shaderProgram = 0;
    private int vao = 0;
    private int vboQuad = 0;
    private int vboInstancePos = 0;
    private int vboInstanceColor = 0;
    private int vboInstanceSize = 0;

    // Parallax state
    private float parallaxX = 0f;
    private float parallaxY = 0f;
    private float time = 0f;

    private Random random;
    private boolean enabled = true;

    // ═══════════════════════════════════════════════════════════════════════════
    // GLSL ES 3.0 SHADERS
    // ═══════════════════════════════════════════════════════════════════════════

    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "precision highp float;\n" +
        "\n" +
        "// Quad vertex (shared)\n" +
        "layout(location = 0) in vec2 aQuadVertex;\n" +
        "\n" +
        "// Instance attributes\n" +
        "layout(location = 1) in vec2 aInstancePos;\n" +
        "layout(location = 2) in vec4 aInstanceColor;\n" +
        "layout(location = 3) in float aInstanceSize;\n" +
        "\n" +
        "out vec4 vColor;\n" +
        "out vec2 vQuadCoord;\n" +
        "\n" +
        "void main() {\n" +
        "    // Scale quad by instance size (in NDC units, not pixels)\n" +
        "    vec2 scaledQuad = aQuadVertex * aInstanceSize;\n" +
        "    \n" +
        "    // Final position\n" +
        "    vec2 pos = aInstancePos + scaledQuad;\n" +
        "    gl_Position = vec4(pos, 0.0, 1.0);\n" +
        "    \n" +
        "    // Pass to fragment\n" +
        "    vColor = aInstanceColor;\n" +
        "    vQuadCoord = aQuadVertex + 0.5; // 0-1 range\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "\n" +
        "in vec4 vColor;\n" +
        "in vec2 vQuadCoord;\n" +
        "\n" +
        "out vec4 fragColor;\n" +
        "\n" +
        "void main() {\n" +
        "    // Circular star with glow\n" +
        "    vec2 center = vQuadCoord - 0.5;\n" +
        "    float dist = length(center);\n" +
        "    \n" +
        "    // Discard outside circle\n" +
        "    if (dist > 0.5) discard;\n" +
        "    \n" +
        "    // Soft glow from center\n" +
        "    float glow = 1.0 - smoothstep(0.0, 0.5, dist);\n" +
        "    glow = pow(glow, 1.5); // Sharper center\n" +
        "    \n" +
        "    fragColor = vec4(vColor.rgb * glow, vColor.a * glow);\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════

    public ParallaxStars() {
        Log.d(TAG, "✨ Creando ParallaxStars v2.0 (Instanced Rendering)...");
        random = new Random();

        // Allocate arrays
        starBaseX = new float[TOTAL_STARS];
        starBaseY = new float[TOTAL_STARS];
        starSizes = new float[TOTAL_STARS];
        starColors = new float[TOTAL_STARS * 4];  // RGBA
        starTwinkle = new float[TOTAL_STARS * 2]; // phase, speed
        starLayer = new int[TOTAL_STARS];

        initStarData();
        initOpenGL();

        Log.d(TAG, "✓ ParallaxStars v2.0 inicializado - " + TOTAL_STARS + " estrellas en 1 draw call");
    }

    private void initStarData() {
        int starIndex = 0;

        for (int layer = 0; layer < NUM_LAYERS; layer++) {
            int count = STARS_PER_LAYER[layer];

            for (int i = 0; i < count; i++) {
                // Position (wider area for scrolling)
                starBaseX[starIndex] = -1.5f + random.nextFloat() * 3.0f;
                starBaseY[starIndex] = -1.5f + random.nextFloat() * 3.0f;

                // Size with variation
                starSizes[starIndex] = STAR_SIZES[layer] * (0.6f + random.nextFloat() * 0.8f);

                // Layer
                starLayer[starIndex] = layer;

                // Twinkle
                starTwinkle[starIndex * 2] = random.nextFloat() * 6.28f;     // phase
                starTwinkle[starIndex * 2 + 1] = 1.0f + random.nextFloat() * 2.5f; // speed

                // Color
                int colorIdx = starIndex * 4;
                float colorType = random.nextFloat();

                if (colorType < 0.55f) {
                    // White/Blue
                    starColors[colorIdx] = 0.9f + random.nextFloat() * 0.1f;
                    starColors[colorIdx + 1] = 0.9f + random.nextFloat() * 0.1f;
                    starColors[colorIdx + 2] = 1.0f;
                } else if (colorType < 0.75f) {
                    // Cyan
                    starColors[colorIdx] = 0.6f + random.nextFloat() * 0.2f;
                    starColors[colorIdx + 1] = 0.95f;
                    starColors[colorIdx + 2] = 1.0f;
                } else if (colorType < 0.92f) {
                    // Yellow/Warm
                    starColors[colorIdx] = 1.0f;
                    starColors[colorIdx + 1] = 0.9f + random.nextFloat() * 0.1f;
                    starColors[colorIdx + 2] = 0.6f + random.nextFloat() * 0.3f;
                } else {
                    // Red/Orange (rare)
                    starColors[colorIdx] = 1.0f;
                    starColors[colorIdx + 1] = 0.5f + random.nextFloat() * 0.4f;
                    starColors[colorIdx + 2] = 0.3f + random.nextFloat() * 0.2f;
                }
                // Base alpha
                starColors[colorIdx + 3] = STAR_ALPHAS[layer] * (0.7f + random.nextFloat() * 0.3f);

                starIndex++;
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // OPENGL INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════

    private void initOpenGL() {
        // Compile shaders
        int vs = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        if (vs == 0 || fs == 0) {
            Log.e(TAG, "Failed to compile shaders");
            return;
        }

        // Link program
        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vs);
        GLES30.glAttachShader(shaderProgram, fs);
        GLES30.glLinkProgram(shaderProgram);

        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(shaderProgram, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Program link error: " + GLES30.glGetProgramInfoLog(shaderProgram));
            return;
        }

        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);

        // Create VAO
        int[] vaos = new int[1];
        GLES30.glGenVertexArrays(1, vaos, 0);
        vao = vaos[0];
        GLES30.glBindVertexArray(vao);

        // Create VBOs
        int[] vbos = new int[4];
        GLES30.glGenBuffers(4, vbos, 0);
        vboQuad = vbos[0];
        vboInstancePos = vbos[1];
        vboInstanceColor = vbos[2];
        vboInstanceSize = vbos[3];

        // Quad vertices (unit quad centered at origin)
        float[] quadVertices = {
            -0.5f, -0.5f,
             0.5f, -0.5f,
            -0.5f,  0.5f,
             0.5f, -0.5f,
             0.5f,  0.5f,
            -0.5f,  0.5f
        };

        FloatBuffer quadBuffer = ByteBuffer.allocateDirect(quadVertices.length * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        quadBuffer.put(quadVertices).position(0);

        // Setup quad VBO (location 0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboQuad);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, quadVertices.length * 4, quadBuffer, GLES30.GL_STATIC_DRAW);
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, 0);
        GLES30.glEnableVertexAttribArray(0);

        // Allocate instance buffers
        instancePositionBuffer = ByteBuffer.allocateDirect(TOTAL_STARS * 2 * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        instanceColorBuffer = ByteBuffer.allocateDirect(TOTAL_STARS * 4 * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        instanceSizeBuffer = ByteBuffer.allocateDirect(TOTAL_STARS * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();

        // Setup instance position VBO (location 1)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboInstancePos);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, TOTAL_STARS * 2 * 4, null, GLES30.GL_DYNAMIC_DRAW);
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 0, 0);
        GLES30.glEnableVertexAttribArray(1);
        GLES30.glVertexAttribDivisor(1, 1); // Per-instance

        // Setup instance color VBO (location 2)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboInstanceColor);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, TOTAL_STARS * 4 * 4, null, GLES30.GL_DYNAMIC_DRAW);
        GLES30.glVertexAttribPointer(2, 4, GLES30.GL_FLOAT, false, 0, 0);
        GLES30.glEnableVertexAttribArray(2);
        GLES30.glVertexAttribDivisor(2, 1); // Per-instance

        // Setup instance size VBO (location 3)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboInstanceSize);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, TOTAL_STARS * 4, null, GLES30.GL_DYNAMIC_DRAW);
        GLES30.glVertexAttribPointer(3, 1, GLES30.GL_FLOAT, false, 0, 0);
        GLES30.glEnableVertexAttribArray(3);
        GLES30.glVertexAttribDivisor(3, 1); // Per-instance

        // Unbind
        GLES30.glBindVertexArray(0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);

        Log.d(TAG, "✓ OpenGL ES 3.0 initialized - VAO: " + vao + ", Shader: " + shaderProgram);
    }

    private int compileShader(int type, String code) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, code);
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

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void update(float deltaTime) {
        if (!enabled) return;

        time += deltaTime;

        // Smooth parallax oscillation (floating through space feeling)
        parallaxX = (float)Math.sin(time * 0.15) * 0.08f + (float)Math.sin(time * 0.07) * 0.04f;
        parallaxY = (float)Math.cos(time * 0.12) * 0.06f + (float)Math.cos(time * 0.05) * 0.03f;

        // Update instance buffers
        instancePositionBuffer.clear();
        instanceColorBuffer.clear();
        instanceSizeBuffer.clear();

        for (int i = 0; i < TOTAL_STARS; i++) {
            int layer = starLayer[i];
            float speed = LAYER_SPEEDS[layer];

            // Apply parallax
            float x = starBaseX[i] + parallaxX * speed * 10f;
            float y = starBaseY[i] + parallaxY * speed * 10f;

            // Wrap around
            if (x < -1.5f) starBaseX[i] += 3.0f;
            if (x > 1.5f) starBaseX[i] -= 3.0f;
            if (y < -1.5f) starBaseY[i] += 3.0f;
            if (y > 1.5f) starBaseY[i] -= 3.0f;

            // Recalculate with wrap
            x = starBaseX[i] + parallaxX * speed * 10f;
            y = starBaseY[i] + parallaxY * speed * 10f;

            // Position
            instancePositionBuffer.put(x);
            instancePositionBuffer.put(y);

            // Twinkle effect
            starTwinkle[i * 2] += starTwinkle[i * 2 + 1] * deltaTime;
            float twinkle = 0.7f + 0.3f * (float)Math.sin(starTwinkle[i * 2]);

            // ═══════════════════════════════════════════════════════════════
            // SUN EXCLUSION ZONE - Estrellas DESAPARECEN cerca del sol
            // ═══════════════════════════════════════════════════════════════
            float distToSun = (float)Math.sqrt((x - SUN_X) * (x - SUN_X) + (y - SUN_Y) * (y - SUN_Y));
            float sunFade = 1.0f;
            if (distToSun < SUN_RADIUS_INNER) {
                // Dentro del radio interno = COMPLETAMENTE INVISIBLE
                sunFade = 0.0f;
            } else if (distToSun < SUN_RADIUS_OUTER) {
                // Entre inner y outer = fade gradual
                sunFade = (distToSun - SUN_RADIUS_INNER) / (SUN_RADIUS_OUTER - SUN_RADIUS_INNER);
            }

            // Color with twinkle AND sun fade
            int colorIdx = i * 4;
            float finalAlpha = starColors[colorIdx + 3] * twinkle * sunFade;
            instanceColorBuffer.put(starColors[colorIdx]);     // R
            instanceColorBuffer.put(starColors[colorIdx + 1]); // G
            instanceColorBuffer.put(starColors[colorIdx + 2]); // B
            instanceColorBuffer.put(finalAlpha);               // A with twinkle + sun fade

            // Size
            instanceSizeBuffer.put(starSizes[i]);
        }

        instancePositionBuffer.position(0);
        instanceColorBuffer.position(0);
        instanceSizeBuffer.position(0);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DRAW - SINGLE INSTANCED CALL!
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void draw() {
        if (!enabled || shaderProgram == 0 || vao == 0) return;

        GLES30.glUseProgram(shaderProgram);

        // Blending
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE); // Additive

        // Update instance VBOs with new data
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboInstancePos);
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, TOTAL_STARS * 2 * 4, instancePositionBuffer);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboInstanceColor);
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, TOTAL_STARS * 4 * 4, instanceColorBuffer);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboInstanceSize);
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, TOTAL_STARS * 4, instanceSizeBuffer);

        // Unbind buffer before drawing
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);

        // Bind VAO and draw ALL stars in ONE call!
        GLES30.glBindVertexArray(vao);
        GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLES, 0, 6, TOTAL_STARS);

        // Unbind VAO
        GLES30.glBindVertexArray(0);

        // IMPORTANT: Reset divisors to 0 (some drivers don't isolate this in VAO)
        GLES30.glVertexAttribDivisor(1, 0);
        GLES30.glVertexAttribDivisor(2, 0);
        GLES30.glVertexAttribDivisor(3, 0);

        // Restore blend mode
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setParallaxOffset(float x, float y) {
        parallaxX = x;
        parallaxY = y;
    }

    public void cleanup() {
        if (vao != 0) {
            int[] vaos = {vao};
            GLES30.glDeleteVertexArrays(1, vaos, 0);
            vao = 0;
        }
        if (vboQuad != 0) {
            int[] vbos = {vboQuad, vboInstancePos, vboInstanceColor, vboInstanceSize};
            GLES30.glDeleteBuffers(4, vbos, 0);
            vboQuad = 0;
        }
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
    }
}
