package com.secret.blackholeglow;

import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.gl3.MatrixPool;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   🔫 LASER ÉPICO - Proyectil de energía con efectos avanzados            ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  CARACTERÍSTICAS:                                                         ║
 * ║  • Núcleo brillante con gradiente de intensidad                          ║
 * ║  • Halo exterior con glow suave                                          ║
 * ║  • Trail/estela que sigue al láser                                       ║
 * ║  • Partículas de energía volando alrededor                               ║
 * ║  • Impacto explosivo con ondas de choque                                 ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class Laser {
    private static final String TAG = "Laser";

    // Equipos
    public static final int TEAM_HUMAN = 0;  // Azul/Cyan
    public static final int TEAM_ALIEN = 1;  // Verde/Lima

    // Posición y movimiento
    public float x, y, z;
    public float vx, vy, vz;
    private float speed = 6.0f;

    // Propiedades
    private int team;
    private boolean active = false;
    private float lifetime = 0f;
    private static final float MAX_LIFETIME = 3.0f;

    // ═══════════════════════════════════════════════════════════════════════
    // 🎨 DIMENSIONES Y VISUAL (proporcionales a las naves)
    // ═══════════════════════════════════════════════════════════════════════
    private static final float CORE_LENGTH = 0.12f;    // Núcleo central (pequeño)
    private static final float CORE_WIDTH = 0.006f;    // Muy delgado
    private static final float GLOW_LENGTH = 0.15f;    // Halo exterior
    private static final float GLOW_WIDTH = 0.02f;     // Glow sutil

    // Trail (estela)
    private static final int TRAIL_SEGMENTS = 8;
    private float[] trailX = new float[TRAIL_SEGMENTS];
    private float[] trailY = new float[TRAIL_SEGMENTS];
    private float[] trailZ = new float[TRAIL_SEGMENTS];
    private int trailIndex = 0;
    private float trailTimer = 0f;
    private static final float TRAIL_UPDATE_RATE = 0.02f;

    // Partículas de energía (más pequeñas)
    private static final int ENERGY_PARTICLES = 4;
    private float[] particleAngle = new float[ENERGY_PARTICLES];
    private float[] particleRadius = new float[ENERGY_PARTICLES];
    private float[] particleSpeed = new float[ENERGY_PARTICLES];

    // ═══════════════════════════════════════════════════════════════════════
    // 💥 SISTEMA DE IMPACTO ÉPICO
    // ═══════════════════════════════════════════════════════════════════════
    private boolean impactActive = false;
    private float impactX, impactY, impactZ;
    private float impactTimer = 0f;
    private static final float IMPACT_DURATION = 0.6f;

    // Partículas de impacto (menos y más pequeñas)
    private static final int IMPACT_PARTICLES = 10;
    private float[] impactPX = new float[IMPACT_PARTICLES];
    private float[] impactPY = new float[IMPACT_PARTICLES];
    private float[] impactPZ = new float[IMPACT_PARTICLES];
    private float[] impactVX = new float[IMPACT_PARTICLES];
    private float[] impactVY = new float[IMPACT_PARTICLES];
    private float[] impactVZ = new float[IMPACT_PARTICLES];
    private float[] impactLife = new float[IMPACT_PARTICLES];
    private float[] impactSize = new float[IMPACT_PARTICLES];

    // Onda de choque
    private float shockwaveRadius = 0f;
    private float shockwaveAlpha = 1f;

    // ═══════════════════════════════════════════════════════════════════════
    // 🎮 SHADERS Y BUFFERS
    // ═══════════════════════════════════════════════════════════════════════
    private static int coreProgram = 0;
    private static int glowProgram = 0;
    private static int particleProgram = 0;

    // Core shader locations
    private static int corePositionLoc, coreUVLoc;
    private static int coreMVPLoc, coreColorLoc, coreTimeLoc, coreIntensityLoc;

    // Glow shader locations
    private static int glowPositionLoc;
    private static int glowMVPLoc, glowColorLoc, glowAlphaLoc;

    // Particle shader locations
    private static int particlePositionLoc;
    private static int particleMVPLoc, particleColorLoc, particleSizeLoc;

    // Buffers
    private static FloatBuffer coreVertexBuffer;
    private static FloatBuffer coreUVBuffer;
    private static FloatBuffer glowVertexBuffer;
    private static FloatBuffer particleVertexBuffer;

    // Colores por equipo (más vibrantes)
    private static final float[] COLOR_HUMAN_CORE = {0.4f, 0.8f, 1.0f, 1.0f};   // Cyan brillante
    private static final float[] COLOR_HUMAN_GLOW = {0.2f, 0.5f, 1.0f, 0.6f};   // Azul suave
    private static final float[] COLOR_ALIEN_CORE = {0.4f, 1.0f, 0.3f, 1.0f};   // Verde lima
    private static final float[] COLOR_ALIEN_GLOW = {0.2f, 0.8f, 0.2f, 0.6f};   // Verde suave

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    private CameraController camera;
    private static long startTime = System.currentTimeMillis();

    public Laser(int team) {
        this.team = team;

        if (coreProgram == 0) {
            initShaders();
        }

        initParticles();
    }

    /**
     * Inicializa las partículas de energía
     */
    private void initParticles() {
        for (int i = 0; i < ENERGY_PARTICLES; i++) {
            particleAngle[i] = (float)(Math.random() * Math.PI * 2);
            particleRadius[i] = 0.01f + (float)(Math.random() * 0.015f);  // Radio pequeño
            particleSpeed[i] = 4f + (float)(Math.random() * 3f);
        }
    }

    /**
     * Inicializa todos los shaders
     */
    private static void initShaders() {
        Log.d(TAG, "🔫 Inicializando shaders de láser épico...");

        // ═══════════════════════════════════════════════════════════════
        // SHADER DEL NÚCLEO (con gradiente de intensidad)
        // ═══════════════════════════════════════════════════════════════
        String coreVertexCode =
            "attribute vec4 a_Position;\n" +
            "attribute vec2 a_UV;\n" +
            "uniform mat4 u_MVP;\n" +
            "varying vec2 v_UV;\n" +
            "void main() {\n" +
            "    gl_Position = u_MVP * a_Position;\n" +
            "    v_UV = a_UV;\n" +
            "}\n";

        String coreFragmentCode =
            "precision mediump float;\n" +
            "uniform vec4 u_Color;\n" +
            "uniform float u_Time;\n" +
            "uniform float u_Intensity;\n" +
            "varying vec2 v_UV;\n" +
            "void main() {\n" +
            "    // Gradiente radial desde el centro\n" +
            "    float distFromCenter = abs(v_UV.y - 0.5) * 2.0;\n" +
            "    float coreGlow = 1.0 - distFromCenter;\n" +
            "    coreGlow = pow(coreGlow, 0.5);\n" +
            "    \n" +
            "    // Pulso de energía\n" +
            "    float pulse = 0.85 + sin(u_Time * 25.0 + v_UV.x * 10.0) * 0.15;\n" +
            "    \n" +
            "    // Efecto de flujo de energía\n" +
            "    float flow = sin(v_UV.x * 20.0 - u_Time * 15.0) * 0.1 + 0.9;\n" +
            "    \n" +
            "    // Color final con brillo intenso en el centro\n" +
            "    vec3 finalColor = u_Color.rgb * coreGlow * pulse * flow * u_Intensity * 2.0;\n" +
            "    \n" +
            "    // Agregar blanco al centro para más brillo\n" +
            "    float whiteness = pow(coreGlow, 3.0) * 0.5;\n" +
            "    finalColor += vec3(whiteness);\n" +
            "    \n" +
            "    gl_FragColor = vec4(finalColor, coreGlow * u_Color.a);\n" +
            "}\n";

        coreProgram = createProgram(coreVertexCode, coreFragmentCode);
        if (coreProgram != 0) {
            corePositionLoc = GLES30.glGetAttribLocation(coreProgram, "a_Position");
            coreUVLoc = GLES30.glGetAttribLocation(coreProgram, "a_UV");
            coreMVPLoc = GLES30.glGetUniformLocation(coreProgram, "u_MVP");
            coreColorLoc = GLES30.glGetUniformLocation(coreProgram, "u_Color");
            coreTimeLoc = GLES30.glGetUniformLocation(coreProgram, "u_Time");
            coreIntensityLoc = GLES30.glGetUniformLocation(coreProgram, "u_Intensity");
        }

        // ═══════════════════════════════════════════════════════════════
        // SHADER DEL GLOW (halo exterior suave)
        // ═══════════════════════════════════════════════════════════════
        String glowVertexCode =
            "attribute vec4 a_Position;\n" +
            "uniform mat4 u_MVP;\n" +
            "void main() {\n" +
            "    gl_Position = u_MVP * a_Position;\n" +
            "}\n";

        String glowFragmentCode =
            "precision mediump float;\n" +
            "uniform vec4 u_Color;\n" +
            "uniform float u_Alpha;\n" +
            "void main() {\n" +
            "    gl_FragColor = vec4(u_Color.rgb, u_Color.a * u_Alpha);\n" +
            "}\n";

        glowProgram = createProgram(glowVertexCode, glowFragmentCode);
        if (glowProgram != 0) {
            glowPositionLoc = GLES30.glGetAttribLocation(glowProgram, "a_Position");
            glowMVPLoc = GLES30.glGetUniformLocation(glowProgram, "u_MVP");
            glowColorLoc = GLES30.glGetUniformLocation(glowProgram, "u_Color");
            glowAlphaLoc = GLES30.glGetUniformLocation(glowProgram, "u_Alpha");
        }

        // ═══════════════════════════════════════════════════════════════
        // SHADER DE PARTÍCULAS (puntos brillantes)
        // ═══════════════════════════════════════════════════════════════
        String particleVertexCode =
            "attribute vec4 a_Position;\n" +
            "uniform mat4 u_MVP;\n" +
            "uniform float u_PointSize;\n" +
            "void main() {\n" +
            "    gl_Position = u_MVP * a_Position;\n" +
            "    gl_PointSize = u_PointSize;\n" +
            "}\n";

        String particleFragmentCode =
            "precision mediump float;\n" +
            "uniform vec4 u_Color;\n" +
            "void main() {\n" +
            "    // Círculo suave\n" +
            "    vec2 coord = gl_PointCoord - vec2(0.5);\n" +
            "    float dist = length(coord);\n" +
            "    float alpha = 1.0 - smoothstep(0.3, 0.5, dist);\n" +
            "    gl_FragColor = vec4(u_Color.rgb * 1.5, u_Color.a * alpha);\n" +
            "}\n";

        particleProgram = createProgram(particleVertexCode, particleFragmentCode);
        if (particleProgram != 0) {
            particlePositionLoc = GLES30.glGetAttribLocation(particleProgram, "a_Position");
            particleMVPLoc = GLES30.glGetUniformLocation(particleProgram, "u_MVP");
            particleColorLoc = GLES30.glGetUniformLocation(particleProgram, "u_Color");
            particleSizeLoc = GLES30.glGetUniformLocation(particleProgram, "u_PointSize");
        }

        // Crear buffers de geometría
        createBuffers();

        Log.d(TAG, "✅ Shaders de láser épico inicializados");
    }

    private static int createProgram(String vertexCode, String fragmentCode) {
        int vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexCode);
        int fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentCode);

        if (vertexShader == 0 || fragmentShader == 0) return 0;

        int program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vertexShader);
        GLES30.glAttachShader(program, fragmentShader);
        GLES30.glLinkProgram(program);

        return program;
    }

    private static int compileShader(int type, String code) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, code);
        GLES30.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Error: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private static void createBuffers() {
        // Buffer del núcleo (quad con UVs)
        float[] coreVerts = {
            -CORE_WIDTH, 0, 0,
             CORE_WIDTH, 0, 0,
             CORE_WIDTH, 0, CORE_LENGTH,
            -CORE_WIDTH, 0, CORE_LENGTH,
            // Cara vertical
            0, -CORE_WIDTH, 0,
            0,  CORE_WIDTH, 0,
            0,  CORE_WIDTH, CORE_LENGTH,
            0, -CORE_WIDTH, CORE_LENGTH,
        };

        float[] coreUVs = {
            0, 0,  1, 0,  1, 1,  0, 1,
            0, 0,  1, 0,  1, 1,  0, 1,
        };

        coreVertexBuffer = createFloatBuffer(coreVerts);
        coreUVBuffer = createFloatBuffer(coreUVs);

        // Buffer del glow (quad más grande)
        float[] glowVerts = {
            -GLOW_WIDTH, 0, -0.05f,
             GLOW_WIDTH, 0, -0.05f,
             GLOW_WIDTH, 0, GLOW_LENGTH,
            -GLOW_WIDTH, 0, GLOW_LENGTH,
            0, -GLOW_WIDTH, -0.05f,
            0,  GLOW_WIDTH, -0.05f,
            0,  GLOW_WIDTH, GLOW_LENGTH,
            0, -GLOW_WIDTH, GLOW_LENGTH,
        };

        glowVertexBuffer = createFloatBuffer(glowVerts);

        // Buffer de partículas (un punto)
        float[] particleVerts = { 0, 0, 0 };
        particleVertexBuffer = createFloatBuffer(particleVerts);
    }

    private static FloatBuffer createFloatBuffer(float[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(data);
        fb.position(0);
        return fb;
    }

    /**
     * Dispara el láser
     */
    public void fire(float startX, float startY, float startZ,
                     float targetX, float targetY, float targetZ) {
        this.x = startX;
        this.y = startY;
        this.z = startZ;

        float dx = targetX - startX;
        float dy = targetY - startY;
        float dz = targetZ - startZ;
        float dist = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);

        if (dist > 0.01f) {
            this.vx = (dx / dist) * speed;
            this.vy = (dy / dist) * speed;
            this.vz = (dz / dist) * speed;
        } else {
            this.vx = 0;
            this.vy = 0;
            this.vz = speed;
        }

        this.active = true;
        this.lifetime = 0f;

        // Inicializar trail
        for (int i = 0; i < TRAIL_SEGMENTS; i++) {
            trailX[i] = x;
            trailY[i] = y;
            trailZ[i] = z;
        }
        trailIndex = 0;

        Log.d(TAG, "🔫 Láser " + (team == TEAM_HUMAN ? "CYAN" : "VERDE") + " disparado!");
    }

    /**
     * Actualiza el láser
     */
    public void update(float deltaTime) {
        if (impactActive) {
            updateImpact(deltaTime);
        }

        if (!active) return;

        // Mover
        x += vx * deltaTime;
        y += vy * deltaTime;
        z += vz * deltaTime;

        // Actualizar trail
        trailTimer += deltaTime;
        if (trailTimer >= TRAIL_UPDATE_RATE) {
            trailTimer = 0;
            trailIndex = (trailIndex + 1) % TRAIL_SEGMENTS;
            trailX[trailIndex] = x;
            trailY[trailIndex] = y;
            trailZ[trailIndex] = z;
        }

        // Actualizar partículas de energía
        float time = lifetime * 10f;
        for (int i = 0; i < ENERGY_PARTICLES; i++) {
            particleAngle[i] += particleSpeed[i] * deltaTime;
        }

        lifetime += deltaTime;

        if (lifetime > MAX_LIFETIME) {
            active = false;
            return;
        }

        if (Math.abs(x) > 10 || Math.abs(y) > 10 || Math.abs(z) > 10) {
            active = false;
        }
    }

    private void updateImpact(float deltaTime) {
        impactTimer += deltaTime;

        // Actualizar partículas
        for (int i = 0; i < IMPACT_PARTICLES; i++) {
            impactPX[i] += impactVX[i] * deltaTime;
            impactPY[i] += impactVY[i] * deltaTime;
            impactPZ[i] += impactVZ[i] * deltaTime;

            impactVX[i] *= 0.94f;
            impactVY[i] *= 0.94f;
            impactVZ[i] *= 0.94f;

            impactLife[i] -= deltaTime / IMPACT_DURATION;
            if (impactLife[i] < 0) impactLife[i] = 0;
        }

        // Actualizar onda de choque
        shockwaveRadius += deltaTime * 1.5f;
        shockwaveAlpha = 1f - (impactTimer / IMPACT_DURATION);

        if (impactTimer >= IMPACT_DURATION) {
            impactActive = false;
        }
    }

    private void startImpact() {
        impactActive = true;
        impactTimer = 0f;
        impactX = x;
        impactY = y;
        impactZ = z;
        shockwaveRadius = 0.02f;
        shockwaveAlpha = 1f;

        for (int i = 0; i < IMPACT_PARTICLES; i++) {
            impactPX[i] = impactX;
            impactPY[i] = impactY;
            impactPZ[i] = impactZ;

            float theta = (float)(Math.random() * Math.PI * 2);
            float phi = (float)(Math.random() * Math.PI);
            float spd = 0.8f + (float)(Math.random() * 1.5f);

            impactVX[i] = spd * (float)(Math.sin(phi) * Math.cos(theta));
            impactVY[i] = spd * (float)(Math.sin(phi) * Math.sin(theta));
            impactVZ[i] = spd * (float)(Math.cos(phi));

            impactLife[i] = 0.7f + (float)(Math.random() * 0.3f);
            impactSize[i] = 5f + (float)(Math.random() * 6f);
        }

        Log.d(TAG, "💥 IMPACTO!");
    }

    /**
     * Dibuja el láser
     */
    public void draw() {
        if (impactActive && camera != null) {
            drawImpact();
        }

        if (!active || camera == null) return;

        float time = ((System.currentTimeMillis() - startTime) / 1000.0f) % 100.0f;

        // Calcular rotación
        float spd = (float) Math.sqrt(vx*vx + vy*vy + vz*vz);
        float yaw = 0, pitch = 0;
        if (spd > 0.01f) {
            yaw = (float) Math.toDegrees(Math.atan2(vx, vz));
            pitch = (float) Math.toDegrees(Math.asin(-vy / spd));
        }

        // ═══════════════════════════════════════════════════════════════
        // 1. DIBUJAR TRAIL (estela)
        // ═══════════════════════════════════════════════════════════════
        drawTrail(time, yaw, pitch);

        // ═══════════════════════════════════════════════════════════════
        // 2. DIBUJAR GLOW (halo exterior)
        // ═══════════════════════════════════════════════════════════════
        drawGlow(yaw, pitch);

        // ═══════════════════════════════════════════════════════════════
        // 3. DIBUJAR NÚCLEO (centro brillante)
        // ═══════════════════════════════════════════════════════════════
        drawCore(time, yaw, pitch);

        // ═══════════════════════════════════════════════════════════════
        // 4. DIBUJAR PARTÍCULAS DE ENERGÍA
        // ═══════════════════════════════════════════════════════════════
        drawEnergyParticles(time, yaw, pitch);
    }

    private void drawTrail(float time, float yaw, float pitch) {
        if (glowProgram == 0) return;

        GLES30.glUseProgram(glowProgram);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE);
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);

        float[] color = (team == TEAM_HUMAN) ? COLOR_HUMAN_GLOW : COLOR_ALIEN_GLOW;

        for (int i = 0; i < TRAIL_SEGMENTS; i++) {
            int idx = (trailIndex - i + TRAIL_SEGMENTS) % TRAIL_SEGMENTS;
            float alpha = (1f - (float)i / TRAIL_SEGMENTS) * 0.4f;
            float scale = 1f - (float)i / TRAIL_SEGMENTS * 0.7f;

            // OPTIMIZADO: Usar MatrixPool en lugar de new float[16]
            float[] model = MatrixPool.obtain();
            Matrix.setIdentityM(model, 0);
            Matrix.translateM(model, 0, trailX[idx], trailY[idx], trailZ[idx]);
            Matrix.rotateM(model, 0, yaw, 0, 1, 0);
            Matrix.rotateM(model, 0, pitch, 1, 0, 0);
            Matrix.scaleM(model, 0, scale, scale, scale * 0.5f);

            float[] mvp = MatrixPool.obtain();
            camera.computeMvp(model, mvp);

            GLES30.glUniformMatrix4fv(glowMVPLoc, 1, false, mvp, 0);
            GLES30.glUniform4fv(glowColorLoc, 1, color, 0);
            GLES30.glUniform1f(glowAlphaLoc, alpha);

            glowVertexBuffer.position(0);
            GLES30.glEnableVertexAttribArray(glowPositionLoc);
            GLES30.glVertexAttribPointer(glowPositionLoc, 3, GLES30.GL_FLOAT, false, 0, glowVertexBuffer);
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 4);
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 4, 4);
            GLES30.glDisableVertexAttribArray(glowPositionLoc);
        }
    }

    private void drawGlow(float yaw, float pitch) {
        if (glowProgram == 0) return;

        GLES30.glUseProgram(glowProgram);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE);
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);

        float[] color = (team == TEAM_HUMAN) ? COLOR_HUMAN_GLOW : COLOR_ALIEN_GLOW;

        // OPTIMIZADO: Usar matrices pre-asignadas
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);
        Matrix.rotateM(modelMatrix, 0, yaw, 0, 1, 0);
        Matrix.rotateM(modelMatrix, 0, pitch, 1, 0, 0);

        camera.computeMvp(modelMatrix, mvpMatrix);

        GLES30.glUniformMatrix4fv(glowMVPLoc, 1, false, mvpMatrix, 0);
        GLES30.glUniform4fv(glowColorLoc, 1, color, 0);
        GLES30.glUniform1f(glowAlphaLoc, 0.7f);

        glowVertexBuffer.position(0);
        GLES30.glEnableVertexAttribArray(glowPositionLoc);
        GLES30.glVertexAttribPointer(glowPositionLoc, 3, GLES30.GL_FLOAT, false, 0, glowVertexBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 4);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 4, 4);
        GLES30.glDisableVertexAttribArray(glowPositionLoc);
    }

    private void drawCore(float time, float yaw, float pitch) {
        if (coreProgram == 0) return;

        GLES30.glUseProgram(coreProgram);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE);
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);

        float[] color = (team == TEAM_HUMAN) ? COLOR_HUMAN_CORE : COLOR_ALIEN_CORE;

        // OPTIMIZADO: Usar MatrixPool
        float[] model = MatrixPool.obtain();
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, x, y, z);
        Matrix.rotateM(model, 0, yaw, 0, 1, 0);
        Matrix.rotateM(model, 0, pitch, 1, 0, 0);

        float[] mvp = MatrixPool.obtain();
        camera.computeMvp(model, mvp);

        GLES30.glUniformMatrix4fv(coreMVPLoc, 1, false, mvp, 0);
        GLES30.glUniform4fv(coreColorLoc, 1, color, 0);
        GLES30.glUniform1f(coreTimeLoc, time);
        GLES30.glUniform1f(coreIntensityLoc, 1.0f);

        coreVertexBuffer.position(0);
        coreUVBuffer.position(0);
        GLES30.glEnableVertexAttribArray(corePositionLoc);
        GLES30.glEnableVertexAttribArray(coreUVLoc);
        GLES30.glVertexAttribPointer(corePositionLoc, 3, GLES30.GL_FLOAT, false, 0, coreVertexBuffer);
        GLES30.glVertexAttribPointer(coreUVLoc, 2, GLES30.GL_FLOAT, false, 0, coreUVBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 4);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 4, 4);
        GLES30.glDisableVertexAttribArray(corePositionLoc);
        GLES30.glDisableVertexAttribArray(coreUVLoc);
    }

    private void drawEnergyParticles(float time, float yaw, float pitch) {
        if (particleProgram == 0) return;

        GLES30.glUseProgram(particleProgram);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE);
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);

        float[] color = (team == TEAM_HUMAN) ? COLOR_HUMAN_CORE : COLOR_ALIEN_CORE;

        for (int i = 0; i < ENERGY_PARTICLES; i++) {
            float angle = particleAngle[i];
            float radius = particleRadius[i];

            // Posición en espiral alrededor del láser
            float px = x + (float)(Math.cos(angle) * radius);
            float py = y + (float)(Math.sin(angle) * radius);
            float pz = z + (i * 0.05f);

            // OPTIMIZADO: Usar MatrixPool
            float[] model = MatrixPool.obtain();
            Matrix.setIdentityM(model, 0);
            Matrix.translateM(model, 0, px, py, pz);

            float[] mvp = MatrixPool.obtain();
            camera.computeMvp(model, mvp);

            GLES30.glUniformMatrix4fv(particleMVPLoc, 1, false, mvp, 0);
            GLES30.glUniform4fv(particleColorLoc, 1, color, 0);
            GLES30.glUniform1f(particleSizeLoc, 3f + (float)Math.sin(time * 5f + i) * 1.5f);

            particleVertexBuffer.position(0);
            GLES30.glEnableVertexAttribArray(particlePositionLoc);
            GLES30.glVertexAttribPointer(particlePositionLoc, 3, GLES30.GL_FLOAT, false, 0, particleVertexBuffer);
            GLES30.glDrawArrays(GLES30.GL_POINTS, 0, 1);
            GLES30.glDisableVertexAttribArray(particlePositionLoc);
        }
    }

    private void drawImpact() {
        if (particleProgram == 0 || camera == null) return;

        GLES30.glUseProgram(particleProgram);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE);
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);

        float[] color = (team == TEAM_HUMAN) ? COLOR_HUMAN_CORE : COLOR_ALIEN_CORE;

        // Dibujar partículas de impacto
        for (int i = 0; i < IMPACT_PARTICLES; i++) {
            if (impactLife[i] <= 0) continue;

            // OPTIMIZADO: Usar MatrixPool
            float[] model = MatrixPool.obtain();
            Matrix.setIdentityM(model, 0);
            Matrix.translateM(model, 0, impactPX[i], impactPY[i], impactPZ[i]);

            float[] mvp = MatrixPool.obtain();
            camera.computeMvp(model, mvp);

            // Color que se desvanece con chispas más brillantes
            float[] particleColor = {
                color[0] * (1f + impactLife[i]),
                color[1] * (1f + impactLife[i]),
                color[2] * (1f + impactLife[i]),
                impactLife[i]
            };

            GLES30.glUniformMatrix4fv(particleMVPLoc, 1, false, mvp, 0);
            GLES30.glUniform4fv(particleColorLoc, 1, particleColor, 0);
            GLES30.glUniform1f(particleSizeLoc, impactSize[i] * impactLife[i]);

            particleVertexBuffer.position(0);
            GLES30.glEnableVertexAttribArray(particlePositionLoc);
            GLES30.glVertexAttribPointer(particlePositionLoc, 3, GLES30.GL_FLOAT, false, 0, particleVertexBuffer);
            GLES30.glDrawArrays(GLES30.GL_POINTS, 0, 1);
            GLES30.glDisableVertexAttribArray(particlePositionLoc);
        }

        // Dibujar onda de choque (anillo expandiéndose)
        if (shockwaveAlpha > 0.1f && glowProgram != 0) {
            GLES30.glUseProgram(glowProgram);

            // Dibujar múltiples puntos en círculo para simular el anillo
            int ringPoints = 16;
            for (int i = 0; i < ringPoints; i++) {
                float angle = (float)(i * Math.PI * 2 / ringPoints);
                float rx = impactX + (float)(Math.cos(angle) * shockwaveRadius);
                float ry = impactY + (float)(Math.sin(angle) * shockwaveRadius);

                // OPTIMIZADO: Usar MatrixPool
                float[] model = MatrixPool.obtain();
                Matrix.setIdentityM(model, 0);
                Matrix.translateM(model, 0, rx, ry, impactZ);
                Matrix.scaleM(model, 0, 0.008f, 0.008f, 0.008f);

                float[] mvp = MatrixPool.obtain();
                camera.computeMvp(model, mvp);

                GLES30.glUniformMatrix4fv(glowMVPLoc, 1, false, mvp, 0);
                GLES30.glUniform4fv(glowColorLoc, 1, color, 0);
                GLES30.glUniform1f(glowAlphaLoc, shockwaveAlpha * 0.5f);

                glowVertexBuffer.position(0);
                GLES30.glEnableVertexAttribArray(glowPositionLoc);
                GLES30.glVertexAttribPointer(glowPositionLoc, 3, GLES30.GL_FLOAT, false, 0, glowVertexBuffer);
                GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 4);
                GLES30.glDisableVertexAttribArray(glowPositionLoc);
            }
        }

        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
    }

    public boolean checkCollision(float targetX, float targetY, float targetZ, float radius) {
        if (!active) return false;

        float dx = x - targetX;
        float dy = y - targetY;
        float dz = z - targetZ;
        float dist = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);

        return dist < (0.15f + radius);
    }

    public void deactivate() {
        if (active) {
            startImpact();
        }
        active = false;
    }

    // Getters
    public boolean isActive() { return active; }
    public boolean hasActiveEffect() { return active || impactActive; }
    public int getTeam() { return team; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }

    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    // =========================================================================
    // LIMPIEZA DE RECURSOS OPENGL
    // =========================================================================

    /**
     * Limpieza por instancia (no hace nada porque los shaders son estaticos)
     */
    public void cleanup() {
        // Los shaders son estaticos, se limpian con cleanupStatic()
        active = false;
        impactActive = false;
    }

    /**
     * Limpieza de recursos estaticos compartidos.
     * DEBE llamarse UNA VEZ cuando la escena se destruye completamente.
     */
    public static void cleanupStatic() {
        Log.d(TAG, "=== CLEANUP STATIC Laser ===");

        if (coreProgram != 0) {
            GLES30.glDeleteProgram(coreProgram);
            Log.d(TAG, "  Core program eliminado: " + coreProgram);
            coreProgram = 0;
        }

        if (glowProgram != 0) {
            GLES30.glDeleteProgram(glowProgram);
            Log.d(TAG, "  Glow program eliminado: " + glowProgram);
            glowProgram = 0;
        }

        if (particleProgram != 0) {
            GLES30.glDeleteProgram(particleProgram);
            Log.d(TAG, "  Particle program eliminado: " + particleProgram);
            particleProgram = 0;
        }

        // Limpiar buffers estaticos
        coreVertexBuffer = null;
        coreUVBuffer = null;
        glowVertexBuffer = null;
        particleVertexBuffer = null;

        Log.d(TAG, "=== CLEANUP STATIC COMPLETADO ===");
    }
}
