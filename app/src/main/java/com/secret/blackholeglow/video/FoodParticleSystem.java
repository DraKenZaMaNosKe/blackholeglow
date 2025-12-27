package com.secret.blackholeglow.video;

import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║              FoodParticleSystem - Comida Interactiva             ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  • Partículas doradas con glow que caen como en agua             ║
 * ║  • Física realista: caída lenta, drift horizontal, wobble        ║
 * ║  • Burst de celebración cuando el pez come                       ║
 * ║  • Touch spawns 3-5 partículas de comida                         ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class FoodParticleSystem {
    private static final String TAG = "FoodParticles";

    // Configuración de partículas de COMIDA
    private static final int MAX_FOOD_PARTICLES = 5;  // Pocas a la vez
    private static final float FOOD_SIZE = 0.008f;  // Pequeña
    private static final float FALL_SPEED = 0.03f;  // Lento para que el pez alcance
    private static final float DRIFT_AMPLITUDE = 0.015f;  // Movimiento lateral
    private static final float DRIFT_FREQUENCY = 2.5f;    // Frecuencia del drift
    private static final float FOOD_LIFETIME = 10.0f;     // Segundos antes de desaparecer
    private static final int SPAWN_COUNT_MIN = 3;
    private static final int SPAWN_COUNT_MAX = 5;

    // Configuración de partículas de BURST (celebración)
    private static final int MAX_BURST_PARTICLES = 100;
    private static final float BURST_SIZE = 0.015f;
    private static final float BURST_SPEED = 0.4f;
    private static final float BURST_LIFETIME = 0.8f;
    private static final int BURST_COUNT = 12;

    // Listas de partículas
    private final List<FoodParticle> foodParticles = new ArrayList<>();
    private final List<BurstParticle> burstParticles = new ArrayList<>();

    // OpenGL
    private int programId;
    private int burstProgramId;
    private FloatBuffer vertexBuffer;
    private boolean initialized = false;

    // Uniforms
    private int uPositionLoc, uSizeLoc, uColorLoc, uAlphaLoc, uTimeLoc;
    private int uBurstPositionLoc, uBurstSizeLoc, uBurstColorLoc, uBurstAlphaLoc;

    // Tiempo global para animaciones
    private float globalTime = 0f;

    // Screen size para conversión de coordenadas
    private int screenWidth = 1080;
    private int screenHeight = 1920;

    // ══════════════════════════════════════════════════════════════════
    // 🐟 SISTEMA NATURAL (atracción mutua suave)
    // ══════════════════════════════════════════════════════════════════
    private float fishX = 0f;
    private float fishY = 0f;
    private static final float ATTRACT_RADIUS = 0.5f;      // Radio donde comida empieza a moverse hacia pez
    private static final float ATTRACT_STRENGTH = 0.3f;    // MUY suave - la comida se mueve poquito
    private static final float EAT_DISTANCE = 0.08f;       // Distancia para ser comida

    // ══════════════════════════════════════════════════════════════════
    // SHADERS
    // ══════════════════════════════════════════════════════════════════

    // Shader para comida (glow dorado)
    private static final String FOOD_VERTEX_SHADER =
        "attribute vec4 a_Position;\n" +
        "void main() {\n" +
        "    gl_Position = a_Position;\n" +
        "    gl_PointSize = 1.0;\n" +
        "}\n";

    private static final String FOOD_FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "uniform vec2 u_Position;\n" +
        "uniform float u_Size;\n" +
        "uniform vec3 u_Color;\n" +
        "uniform float u_Alpha;\n" +
        "uniform float u_Time;\n" +
        "\n" +
        "void main() {\n" +
        "    vec2 uv = gl_FragCoord.xy / vec2(1080.0, 1920.0);\n" +
        "    uv = uv * 2.0 - 1.0;\n" +
        "    \n" +
        "    float dist = length(uv - u_Position);\n" +
        "    \n" +
        "    // Núcleo brillante\n" +
        "    float core = smoothstep(u_Size, u_Size * 0.3, dist);\n" +
        "    \n" +
        "    // Glow exterior suave\n" +
        "    float glow = smoothstep(u_Size * 3.0, u_Size * 0.5, dist) * 0.5;\n" +
        "    \n" +
        "    // Pulso sutil\n" +
        "    float pulse = 0.9 + 0.1 * sin(u_Time * 4.0 + u_Position.x * 10.0);\n" +
        "    \n" +
        "    float alpha = (core + glow) * u_Alpha * pulse;\n" +
        "    \n" +
        "    if (alpha < 0.01) discard;\n" +
        "    \n" +
        "    // Color dorado con brillo\n" +
        "    vec3 finalColor = u_Color * (core * 1.5 + glow);\n" +
        "    \n" +
        "    gl_FragColor = vec4(finalColor, alpha);\n" +
        "}\n";

    // Shader para burst (explosión de partículas)
    private static final String BURST_FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "uniform vec2 u_Position;\n" +
        "uniform float u_Size;\n" +
        "uniform vec3 u_Color;\n" +
        "uniform float u_Alpha;\n" +
        "\n" +
        "void main() {\n" +
        "    vec2 uv = gl_FragCoord.xy / vec2(1080.0, 1920.0);\n" +
        "    uv = uv * 2.0 - 1.0;\n" +
        "    \n" +
        "    float dist = length(uv - u_Position);\n" +
        "    \n" +
        "    // Estrellita con rayos\n" +
        "    vec2 dir = normalize(uv - u_Position);\n" +
        "    float angle = atan(dir.y, dir.x);\n" +
        "    float rays = 0.7 + 0.3 * cos(angle * 4.0);\n" +
        "    \n" +
        "    float star = smoothstep(u_Size * rays, 0.0, dist);\n" +
        "    \n" +
        "    float alpha = star * u_Alpha;\n" +
        "    \n" +
        "    if (alpha < 0.01) discard;\n" +
        "    \n" +
        "    gl_FragColor = vec4(u_Color * 1.5, alpha);\n" +
        "}\n";

    // ══════════════════════════════════════════════════════════════════
    // CLASES INTERNAS
    // ══════════════════════════════════════════════════════════════════

    private static class FoodParticle {
        float x, y;           // Posición
        float vx, vy;         // Velocidad
        float spawnTime;      // Tiempo de creación (para drift)
        float lifetime;       // Tiempo restante
        float size;           // Tamaño (variación)

        FoodParticle(float x, float y, float time) {
            this.x = x;
            this.y = y;
            this.vx = 0;
            this.vy = -FALL_SPEED;
            this.spawnTime = time;
            this.lifetime = FOOD_LIFETIME;
            this.size = FOOD_SIZE * (0.8f + (float)Math.random() * 0.4f);
        }
    }

    private static class BurstParticle {
        float x, y;
        float vx, vy;
        float lifetime;
        float maxLifetime;
        float[] color;

        BurstParticle(float x, float y, float angle, float speed, float[] color) {
            this.x = x;
            this.y = y;
            this.vx = (float)Math.cos(angle) * speed;
            this.vy = (float)Math.sin(angle) * speed;
            this.lifetime = BURST_LIFETIME;
            this.maxLifetime = BURST_LIFETIME;
            this.color = color;
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // INICIALIZACIÓN
    // ══════════════════════════════════════════════════════════════════

    public void initialize() {
        if (initialized) return;

        // Crear programa de comida
        int vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, FOOD_VERTEX_SHADER);
        int fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, FOOD_FRAGMENT_SHADER);

        programId = GLES30.glCreateProgram();
        GLES30.glAttachShader(programId, vertexShader);
        GLES30.glAttachShader(programId, fragmentShader);
        GLES30.glLinkProgram(programId);

        uPositionLoc = GLES30.glGetUniformLocation(programId, "u_Position");
        uSizeLoc = GLES30.glGetUniformLocation(programId, "u_Size");
        uColorLoc = GLES30.glGetUniformLocation(programId, "u_Color");
        uAlphaLoc = GLES30.glGetUniformLocation(programId, "u_Alpha");
        uTimeLoc = GLES30.glGetUniformLocation(programId, "u_Time");

        // Crear programa de burst
        int burstFragShader = compileShader(GLES30.GL_FRAGMENT_SHADER, BURST_FRAGMENT_SHADER);

        burstProgramId = GLES30.glCreateProgram();
        GLES30.glAttachShader(burstProgramId, vertexShader);
        GLES30.glAttachShader(burstProgramId, burstFragShader);
        GLES30.glLinkProgram(burstProgramId);

        uBurstPositionLoc = GLES30.glGetUniformLocation(burstProgramId, "u_Position");
        uBurstSizeLoc = GLES30.glGetUniformLocation(burstProgramId, "u_Size");
        uBurstColorLoc = GLES30.glGetUniformLocation(burstProgramId, "u_Color");
        uBurstAlphaLoc = GLES30.glGetUniformLocation(burstProgramId, "u_Alpha");

        // Fullscreen quad
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
        Log.d(TAG, "✅ FoodParticleSystem inicializado");
    }

    // ══════════════════════════════════════════════════════════════════
    // SPAWN DE COMIDA
    // ══════════════════════════════════════════════════════════════════

    /**
     * Spawn UNA comida en posición de touch
     */
    public void spawnFood(float touchX, float touchY) {
        // Límite de comida en pantalla
        if (foodParticles.size() >= MAX_FOOD_PARTICLES) {
            foodParticles.remove(0);
        }

        // Convertir coordenadas de pantalla a GL (-1 a 1)
        float glX = (touchX / screenWidth) * 2f - 1f;
        float glY = 1f - (touchY / screenHeight) * 2f;

        // Solo UNA partícula de comida
        FoodParticle p = new FoodParticle(glX, glY, globalTime);
        foodParticles.add(p);
    }

    /**
     * Spawn burst de celebración cuando el pez come
     */
    public void spawnBurst(float x, float y) {
        // Colores de celebración (dorado, naranja, amarillo, blanco)
        float[][] colors = {
            {1.0f, 0.85f, 0.2f},  // Dorado
            {1.0f, 0.6f, 0.1f},   // Naranja
            {1.0f, 1.0f, 0.4f},   // Amarillo brillante
            {1.0f, 1.0f, 0.9f},   // Blanco cálido
            {0.4f, 1.0f, 0.8f},   // Cyan (bioluminiscente)
        };

        for (int i = 0; i < BURST_COUNT; i++) {
            float angle = (float)(i * Math.PI * 2.0 / BURST_COUNT);
            angle += (float)(Math.random() - 0.5) * 0.5f;  // Variación

            float speed = BURST_SPEED * (0.6f + (float)Math.random() * 0.8f);
            float[] color = colors[(int)(Math.random() * colors.length)];

            burstParticles.add(new BurstParticle(x, y, angle, speed, color));
        }

        Log.d(TAG, "✨ Burst de celebración en (" + x + ", " + y + ")");
    }

    // ══════════════════════════════════════════════════════════════════
    // UPDATE (FÍSICA)
    // ══════════════════════════════════════════════════════════════════

    /**
     * 🐟 Actualiza posición del pez
     */
    public void setFishPosition(float x, float y) {
        this.fishX = x;
        this.fishY = y;
    }

    public void update(float deltaTime) {
        globalTime += deltaTime;

        // Update partículas de comida
        Iterator<FoodParticle> foodIt = foodParticles.iterator();
        while (foodIt.hasNext()) {
            FoodParticle p = foodIt.next();

            // Calcular distancia al pez
            float dx = fishX - p.x;
            float dy = fishY - p.y;
            float distToFish = (float)Math.sqrt(dx * dx + dy * dy);

            // ¿El pez llegó a la comida?
            if (distToFish < EAT_DISTANCE) {
                foodIt.remove();
                continue;
            }

            // ══════════════════════════════════════════════════════════
            // COMPORTAMIENTO NORMAL: Caída + drift (SIEMPRE)
            // ══════════════════════════════════════════════════════════
            float driftTime = globalTime - p.spawnTime;
            float drift = (float)Math.sin(driftTime * DRIFT_FREQUENCY) * DRIFT_AMPLITUDE;
            p.x += drift * deltaTime;

            // Caída lenta
            p.y += p.vy * deltaTime;
            p.vy = -FALL_SPEED * (0.9f + 0.1f * (float)Math.sin(driftTime * 3f));

            // ══════════════════════════════════════════════════════════
            // ATRACCIÓN SUAVE: Solo cuando el pez está cerca
            // ══════════════════════════════════════════════════════════
            if (distToFish < ATTRACT_RADIUS && distToFish > 0.01f) {
                // Atracción MUY suave hacia el pez (complementa el movimiento del pez)
                float attractPower = 1.0f - (distToFish / ATTRACT_RADIUS);
                float attractForce = ATTRACT_STRENGTH * attractPower * deltaTime;

                p.x += (dx / distToFish) * attractForce;
                p.y += (dy / distToFish) * attractForce;
            }

            // Reducir lifetime
            p.lifetime -= deltaTime;

            // Remover si expiró o llegó al fondo
            if (p.lifetime <= 0 || p.y < -1.2f) {
                foodIt.remove();
            }
        }

        // Update partículas de burst
        Iterator<BurstParticle> burstIt = burstParticles.iterator();
        while (burstIt.hasNext()) {
            BurstParticle p = burstIt.next();

            p.x += p.vx * deltaTime;
            p.y += p.vy * deltaTime;

            // Desacelerar
            p.vx *= 0.96f;
            p.vy *= 0.96f;

            p.lifetime -= deltaTime;

            if (p.lifetime <= 0) {
                burstIt.remove();
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // DRAW
    // ══════════════════════════════════════════════════════════════════

    public void draw() {
        if (!initialized) return;

        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE);  // Additive para glow

        int positionHandle = GLES30.glGetAttribLocation(programId, "a_Position");

        // ═══ DIBUJAR COMIDA ═══
        GLES30.glUseProgram(programId);

        vertexBuffer.position(0);
        GLES30.glVertexAttribPointer(positionHandle, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);
        GLES30.glEnableVertexAttribArray(positionHandle);

        GLES30.glUniform1f(uTimeLoc, globalTime);

        // Color dorado/naranja para comida
        float[] foodColor = {1.0f, 0.7f, 0.2f};

        for (FoodParticle p : foodParticles) {
            GLES30.glUniform2f(uPositionLoc, p.x, p.y);
            GLES30.glUniform1f(uSizeLoc, p.size);
            GLES30.glUniform3fv(uColorLoc, 1, foodColor, 0);

            // Fade out al final del lifetime
            float alpha = Math.min(1f, p.lifetime / 2f);
            GLES30.glUniform1f(uAlphaLoc, alpha);

            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        }

        // ═══ DIBUJAR BURST ═══
        GLES30.glUseProgram(burstProgramId);

        int burstPosHandle = GLES30.glGetAttribLocation(burstProgramId, "a_Position");
        vertexBuffer.position(0);
        GLES30.glVertexAttribPointer(burstPosHandle, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);
        GLES30.glEnableVertexAttribArray(burstPosHandle);

        for (BurstParticle p : burstParticles) {
            GLES30.glUniform2f(uBurstPositionLoc, p.x, p.y);

            // Tamaño crece y luego decrece
            float lifeRatio = p.lifetime / p.maxLifetime;
            float size = BURST_SIZE * (1f + (1f - lifeRatio) * 0.5f);
            GLES30.glUniform1f(uBurstSizeLoc, size);

            GLES30.glUniform3fv(uBurstColorLoc, 1, p.color, 0);

            // Fade out
            float alpha = lifeRatio;
            GLES30.glUniform1f(uBurstAlphaLoc, alpha);

            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        }

        GLES30.glDisableVertexAttribArray(positionHandle);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
    }

    // ══════════════════════════════════════════════════════════════════
    // MÉTODOS PARA EL PEZ
    // ══════════════════════════════════════════════════════════════════

    /**
     * Obtiene la posición de la comida más cercana al pez
     * @return float[2] con {x, y} o null si no hay comida
     */
    public float[] getNearestFood(float fishX, float fishY) {
        if (foodParticles.isEmpty()) return null;

        float nearestDist = Float.MAX_VALUE;
        FoodParticle nearest = null;

        for (FoodParticle p : foodParticles) {
            float dx = p.x - fishX;
            float dy = p.y - fishY;
            float dist = dx * dx + dy * dy;

            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = p;
            }
        }

        if (nearest != null) {
            return new float[]{nearest.x, nearest.y};
        }
        return null;
    }

    /**
     * El pez intenta comer en una posición
     * @return true si había comida y fue comida
     */
    public boolean tryEatAt(float x, float y, float eatRadius) {
        Iterator<FoodParticle> it = foodParticles.iterator();
        while (it.hasNext()) {
            FoodParticle p = it.next();
            float dx = p.x - x;
            float dy = p.y - y;
            float dist = (float)Math.sqrt(dx * dx + dy * dy);

            if (dist < eatRadius) {
                // ¡Comió!
                it.remove();
                // Sin festejo - solo desaparece la comida
                return true;
            }
        }
        return false;
    }

    /**
     * @return true si hay comida disponible
     */
    public boolean hasFood() {
        return !foodParticles.isEmpty();
    }

    /**
     * @return cantidad de partículas de comida
     */
    public int getFoodCount() {
        return foodParticles.size();
    }

    // ══════════════════════════════════════════════════════════════════
    // UTILIDADES
    // ══════════════════════════════════════════════════════════════════

    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }

    private int compileShader(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            String log = GLES30.glGetShaderInfoLog(shader);
            Log.e(TAG, "Error compilando shader: " + log);
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    public void release() {
        if (programId != 0) {
            GLES30.glDeleteProgram(programId);
            programId = 0;
        }
        if (burstProgramId != 0) {
            GLES30.glDeleteProgram(burstProgramId);
            burstProgramId = 0;
        }
        foodParticles.clear();
        burstParticles.clear();
        initialized = false;
        Log.d(TAG, "🧹 FoodParticleSystem liberado");
    }
}
