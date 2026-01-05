package com.secret.blackholeglow.effects;

import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║              ⚡ ELECTRIC SPARKS OVERLAY - Ultra Instinct Style           ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Rayos eléctricos que aparecen donde tocas la pantalla.                  ║
 * ║  Estilo aura de Goku Ultra Instinct - cyan/blanco brillante.             ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class ElectricSparksOverlay {
    private static final String TAG = "ElectricSparks";

    // Shader program
    private int shaderProgram;
    private FloatBuffer vertexBuffer;

    // Uniforms
    private int uTimeLocation;
    private int uResolutionLocation;
    private int uTouchPosLocation;
    private int uLifetimeLocation;
    private int uSeedLocation;

    // Active sparks - Balance entre visibilidad y rendimiento
    private List<Spark> activeSparks = new ArrayList<>();
    private static final float SPARK_DURATION = 0.5f;   // Duración media
    private static final int MAX_SPARKS = 3;            // Máximo 3 simultáneas

    private int screenWidth = 1080;
    private int screenHeight = 1920;
    private float globalTime = 0f;

    // Spark data class
    private static class Spark {
        float x, y;           // Posición normalizada (-1 a 1)
        float lifetime;       // Tiempo restante
        float seed;           // Semilla para variación
        float maxLifetime;

        Spark(float x, float y) {
            this.x = x;
            this.y = y;
            this.lifetime = SPARK_DURATION;
            this.maxLifetime = SPARK_DURATION;
            this.seed = (float) Math.random() * 100f;
        }

        boolean update(float dt) {
            lifetime -= dt;
            return lifetime > 0;
        }

        float getAlpha() {
            return lifetime / maxLifetime;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SHADERS - Rayos procedurales estilo Ultra Instinct
    // ═══════════════════════════════════════════════════════════════════════

    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "in vec4 aPosition;\n" +
        "out vec2 vUV;\n" +
        "void main() {\n" +
        "    gl_Position = aPosition;\n" +
        "    vUV = aPosition.xy * 0.5 + 0.5;\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════════════
    // SHADER VISIBLE - 5 rayos grandes y brillantes, optimizado
    // ═══════════════════════════════════════════════════════════════════════
    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "in vec2 vUV;\n" +
        "out vec4 fragColor;\n" +
        "\n" +
        "uniform float uTime;\n" +
        "uniform vec2 uResolution;\n" +
        "uniform vec2 uTouchPos;\n" +
        "uniform float uLifetime;\n" +
        "uniform float uSeed;\n" +
        "\n" +
        "float hash(float n) {\n" +
        "    return fract(sin(n) * 43758.5453);\n" +
        "}\n" +
        "\n" +
        "float lightning(vec2 uv, vec2 origin, float angle, float len) {\n" +
        "    vec2 dir = vec2(cos(angle), sin(angle));\n" +
        "    vec2 perp = vec2(-dir.y, dir.x);\n" +
        "    vec2 rel = uv - origin;\n" +
        "    float along = dot(rel, dir);\n" +
        "    float across = dot(rel, perp);\n" +
        "    \n" +
        "    if (along < 0.0 || along > len) return 0.0;\n" +
        "    \n" +
        "    // Zigzag más pronunciado\n" +
        "    float zigzag = sin(along * 25.0 + uTime * 25.0 + uSeed) * 0.035;\n" +
        "    float dist = abs(across - zigzag);\n" +
        "    \n" +
        "    // Rayo más grueso con glow\n" +
        "    float core = smoothstep(0.012, 0.0, dist);\n" +
        "    float glow = smoothstep(0.05, 0.0, dist) * 0.4;\n" +
        "    return core + glow;\n" +
        "}\n" +
        "\n" +
        "void main() {\n" +
        "    vec2 uv = vUV;\n" +
        "    float aspect = uResolution.x / uResolution.y;\n" +
        "    uv.x *= aspect;\n" +
        "    \n" +
        "    vec2 touchUV = uTouchPos;\n" +
        "    touchUV.x *= aspect;\n" +
        "    \n" +
        "    float intensity = 0.0;\n" +
        "    \n" +
        "    // 5 rayos bien visibles\n" +
        "    for (int i = 0; i < 5; i++) {\n" +
        "        float fi = float(i);\n" +
        "        float angle = (fi + uSeed) * 1.256;  // ~72 grados entre rayos\n" +
        "        float len = 0.2 + hash(fi + uSeed) * 0.15;  // Más largos\n" +
        "        float flicker = step(0.25, hash(fi + floor(uTime * 12.0)));\n" +
        "        intensity += lightning(uv, touchUV, angle, len) * flicker;\n" +
        "    }\n" +
        "    \n" +
        "    // Glow central grande y brillante\n" +
        "    float centerDist = length(uv - touchUV);\n" +
        "    intensity += exp(-centerDist * 8.0) * 1.2;\n" +
        "    \n" +
        "    intensity *= uLifetime;\n" +
        "    \n" +
        "    // Color cyan brillante Ultra Instinct\n" +
        "    vec3 color = vec3(0.5, 0.95, 1.0) * intensity * 1.5;\n" +
        "    fragColor = vec4(color, min(intensity, 1.0));\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════

    public void initialize() {
        // Fullscreen quad vertices
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

        // Compile shaders
        int vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        if (vertexShader == 0 || fragmentShader == 0) {
            Log.e(TAG, "Failed to compile shaders");
            return;
        }

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vertexShader);
        GLES30.glAttachShader(shaderProgram, fragmentShader);
        GLES30.glLinkProgram(shaderProgram);

        // Get uniform locations
        uTimeLocation = GLES30.glGetUniformLocation(shaderProgram, "uTime");
        uResolutionLocation = GLES30.glGetUniformLocation(shaderProgram, "uResolution");
        uTouchPosLocation = GLES30.glGetUniformLocation(shaderProgram, "uTouchPos");
        uLifetimeLocation = GLES30.glGetUniformLocation(shaderProgram, "uLifetime");
        uSeedLocation = GLES30.glGetUniformLocation(shaderProgram, "uSeed");

        Log.d(TAG, "⚡ Electric Sparks inicializado");
    }

    private int compileShader(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);

        if (compiled[0] == 0) {
            String log = GLES30.glGetShaderInfoLog(shader);
            Log.e(TAG, "Shader compile error: " + log);
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TOUCH HANDLING
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Trigger sparks at touch position - SYNCHRONIZED para evitar ConcurrentModificationException
     * @param normalizedX -1 to 1 (left to right)
     * @param normalizedY -1 to 1 (bottom to top)
     */
    public synchronized void triggerSpark(float normalizedX, float normalizedY) {
        // Limit active sparks
        if (activeSparks.size() >= MAX_SPARKS) {
            activeSparks.remove(0);
        }

        // Convert to UV space (0 to 1)
        float uvX = (normalizedX + 1f) * 0.5f;
        float uvY = (normalizedY + 1f) * 0.5f;

        activeSparks.add(new Spark(uvX, uvY));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UPDATE & DRAW
    // ═══════════════════════════════════════════════════════════════════════

    public synchronized void update(float deltaTime) {
        globalTime += deltaTime;

        // Update sparks and remove dead ones
        Iterator<Spark> it = activeSparks.iterator();
        while (it.hasNext()) {
            if (!it.next().update(deltaTime)) {
                it.remove();
            }
        }
    }

    public synchronized void draw() {
        if (shaderProgram == 0 || activeSparks.isEmpty()) return;

        // Save GL state
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE);  // Additive blending

        GLES30.glUseProgram(shaderProgram);

        // Set common uniforms
        GLES30.glUniform1f(uTimeLocation, globalTime);
        GLES30.glUniform2f(uResolutionLocation, screenWidth, screenHeight);

        // Vertex attribute
        int posAttrib = GLES30.glGetAttribLocation(shaderProgram, "aPosition");
        GLES30.glEnableVertexAttribArray(posAttrib);
        GLES30.glVertexAttribPointer(posAttrib, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        // Draw each active spark
        for (Spark spark : activeSparks) {
            GLES30.glUniform2f(uTouchPosLocation, spark.x, spark.y);
            GLES30.glUniform1f(uLifetimeLocation, spark.getAlpha());
            GLES30.glUniform1f(uSeedLocation, spark.seed);

            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        }

        GLES30.glDisableVertexAttribArray(posAttrib);

        // Restore blend mode
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }

    public synchronized void release() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        activeSparks.clear();
        Log.d(TAG, "⚡ Electric Sparks liberado");
    }

    public synchronized boolean hasActiveSparks() {
        return !activeSparks.isEmpty();
    }
}
