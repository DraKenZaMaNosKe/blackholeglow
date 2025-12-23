package com.secret.blackholeglow.christmas;

import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║   🌫️ ChimneySmoke v2 - Humo Orgánico Mejorado                            ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║   - Partículas wispy (alargadas, no círculos)                            ║
 * ║   - Movimiento ondulante suave                                           ║
 * ║   - Color gris-azulado para mezclarse con el fondo                       ║
 * ║   - Más transparente y difuso                                            ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 */
public class ChimneySmoke {
    private static final String TAG = "ChimneySmoke";

    // ═══════════════════════════════════════════════════════════════
    // 📍 CONFIGURACIÓN - Editar posición de la chimenea aquí
    // ═══════════════════════════════════════════════════════════════
    private static final float CHIMNEY_X = 0.28f;      // Posición X de la chimenea
    private static final float CHIMNEY_Y = 0.25f;      // Posición Y de la chimenea
    private static final float SPAWN_WIDTH = 0.015f;   // Ancho del área de spawn (más angosto)

    // Configuración de partículas mejorada
    private static final int NUM_PARTICLES = 35;       // Más partículas para mejor cobertura
    private static final float RISE_SPEED = 0.04f;     // Más lento (como humo real)
    private static final float DRIFT_AMOUNT = 0.025f;  // Deriva suave
    private static final float EXPAND_RATE = 0.4f;     // Expansión gradual
    private static final float LIFE_SPAN = 5.0f;       // Vida más larga

    // ═══════════════════════════════════════════════════════════════
    // PARTÍCULAS
    // ═══════════════════════════════════════════════════════════════
    private float[] particleX;
    private float[] particleY;
    private float[] particleLife;
    private float[] particleSize;
    private float[] particleAlpha;
    private float[] particleDrift;
    private float[] particleSpeed;
    private float[] particlePhase;     // Fase individual para ondulación
    private float[] particleRotation;  // Rotación para forma wispy

    // OpenGL
    private int shaderProgram;
    private FloatBuffer vertexBuffer;
    private int aPositionLoc, aSizeLoc, aAlphaLoc, aPhaseLoc, aRotationLoc;
    private int uAspectLoc, uTimeLoc;

    private boolean initialized = false;
    private boolean visible = true;
    private float time = 0f;
    private float aspectRatio = 1.0f;

    private static final int FLOATS_PER_PARTICLE = 6; // x, y, size, alpha, phase, rotation

    // ═══════════════════════════════════════════════════════════════
    // SHADERS MEJORADOS
    // ═══════════════════════════════════════════════════════════════
    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "precision highp float;\n" +
        "in vec2 a_Position;\n" +
        "in float a_Size;\n" +
        "in float a_Alpha;\n" +
        "in float a_Phase;\n" +
        "in float a_Rotation;\n" +
        "uniform float u_Aspect;\n" +
        "uniform float u_Time;\n" +
        "out float v_Alpha;\n" +
        "out float v_Phase;\n" +
        "out float v_Rotation;\n" +
        "void main() {\n" +
        "    vec2 pos = a_Position;\n" +
        "    // Ondulación suave\n" +
        "    pos.x += sin(u_Time * 0.5 + a_Phase) * 0.008;\n" +
        "    pos.x /= u_Aspect;\n" +
        "    gl_Position = vec4(pos, 0.0, 1.0);\n" +
        "    gl_PointSize = a_Size;\n" +
        "    v_Alpha = a_Alpha;\n" +
        "    v_Phase = a_Phase;\n" +
        "    v_Rotation = a_Rotation;\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "in float v_Alpha;\n" +
        "in float v_Phase;\n" +
        "in float v_Rotation;\n" +
        "out vec4 fragColor;\n" +
        "\n" +
        "void main() {\n" +
        "    vec2 coord = gl_PointCoord - vec2(0.5);\n" +
        "    \n" +
        "    // Rotar coordenadas para forma de hebra/hilo\n" +
        "    float c = cos(v_Rotation);\n" +
        "    float s = sin(v_Rotation);\n" +
        "    vec2 rotated = vec2(coord.x * c - coord.y * s, coord.x * s + coord.y * c);\n" +
        "    \n" +
        "    // Forma MUY alargada (como hebras de humo)\n" +
        "    rotated.x *= 0.4;\n" +  // Muy estirado horizontalmente
        "    rotated.y *= 2.0;\n" +  // Muy comprimido verticalmente
        "    float dist = length(rotated) * 2.0;\n" +
        "    \n" +
        "    // Forma suave con centro más denso\n" +
        "    float smoke = 1.0 - smoothstep(0.0, 0.8, dist);\n" +
        "    smoke = pow(smoke, 1.5);\n" +
        "    \n" +
        "    // Variación ondulante en la hebra\n" +
        "    float wave = sin(coord.y * 15.0 + v_Phase * 5.0) * 0.15 + 0.85;\n" +
        "    smoke *= wave;\n" +
        "    \n" +
        "    // Color BLANCO humo (casi blanco puro)\n" +
        "    vec3 smokeColor = vec3(0.95, 0.95, 0.97);\n" +
        "    \n" +
        "    // Alpha más visible\n" +
        "    float finalAlpha = smoke * v_Alpha * 0.6;\n" +
        "    \n" +
        "    fragColor = vec4(smokeColor, finalAlpha);\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════
    public ChimneySmoke() {
        particleX = new float[NUM_PARTICLES];
        particleY = new float[NUM_PARTICLES];
        particleLife = new float[NUM_PARTICLES];
        particleSize = new float[NUM_PARTICLES];
        particleAlpha = new float[NUM_PARTICLES];
        particleDrift = new float[NUM_PARTICLES];
        particleSpeed = new float[NUM_PARTICLES];
        particlePhase = new float[NUM_PARTICLES];
        particleRotation = new float[NUM_PARTICLES];

        // Inicializar partículas escalonadas
        for (int i = 0; i < NUM_PARTICLES; i++) {
            particleLife[i] = -i * (LIFE_SPAN / NUM_PARTICLES);
            resetParticle(i);
        }

        Log.d(TAG, "🌫️ Sistema de humo v2 creado con " + NUM_PARTICLES + " partículas wispy");
    }

    private void resetParticle(int i) {
        // Posición inicial en la chimenea
        particleX[i] = CHIMNEY_X + (float)(Math.random() - 0.5) * SPAWN_WIDTH;
        particleY[i] = CHIMNEY_Y;

        // Propiedades aleatorias para variedad natural
        particleDrift[i] = (float)(Math.random() - 0.5) * 2.0f;
        particleSpeed[i] = RISE_SPEED * (0.6f + (float)Math.random() * 0.8f);
        particleSize[i] = 60.0f + (float)Math.random() * 40.0f;  // Más grande
        particleAlpha[i] = 0.5f + (float)Math.random() * 0.5f;   // Más visible
        particlePhase[i] = (float)(Math.random() * Math.PI * 2.0);
        particleRotation[i] = (float)(Math.random() * Math.PI);  // Rotación aleatoria
    }

    // ═══════════════════════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════════════════════
    public void update(float dt) {
        time += dt;

        for (int i = 0; i < NUM_PARTICLES; i++) {
            particleLife[i] += dt;

            if (particleLife[i] >= LIFE_SPAN) {
                resetParticle(i);
                particleLife[i] = 0;
            } else if (particleLife[i] >= 0) {
                float lifeProgress = particleLife[i] / LIFE_SPAN;

                // Subir lentamente
                particleY[i] += particleSpeed[i] * dt;

                // Deriva ondulante suave (sinusoidal doble)
                float wave1 = (float)Math.sin(time * 0.8 + particlePhase[i]) * DRIFT_AMOUNT;
                float wave2 = (float)Math.sin(time * 1.5 + particlePhase[i] * 2.0) * DRIFT_AMOUNT * 0.5f;
                particleX[i] += (wave1 + wave2) * particleDrift[i] * dt;

                // Rotar lentamente
                particleRotation[i] += dt * 0.2f * particleDrift[i];

                // Expandir mientras sube
                particleSize[i] += EXPAND_RATE * dt * 25.0f;

                // Desvanecer suavemente (curva cuadrática)
                float fadeOut = 1.0f - lifeProgress;
                particleAlpha[i] = fadeOut * fadeOut * (0.4f + (float)Math.sin(particlePhase[i]) * 0.1f);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // OPENGL
    // ═══════════════════════════════════════════════════════════════
    private void initOpenGL() {
        if (initialized) return;

        int vs = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        if (vs == 0 || fs == 0) {
            Log.e(TAG, "Error compilando shaders");
            return;
        }

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vs);
        GLES30.glAttachShader(shaderProgram, fs);
        GLES30.glLinkProgram(shaderProgram);

        int[] status = new int[1];
        GLES30.glGetProgramiv(shaderProgram, GLES30.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) {
            Log.e(TAG, "Error linking program");
            return;
        }

        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        aSizeLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Size");
        aAlphaLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Alpha");
        aPhaseLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Phase");
        aRotationLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Rotation");
        uAspectLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Aspect");
        uTimeLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Time");

        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);

        // Buffer
        ByteBuffer bb = ByteBuffer.allocateDirect(NUM_PARTICLES * FLOATS_PER_PARTICLE * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();

        initialized = true;
        Log.d(TAG, "✅ OpenGL v2 inicializado");
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

    public void draw() {
        if (!visible) return;
        if (!initialized) {
            initOpenGL();
            if (!initialized) return;
        }

        // Actualizar buffer
        vertexBuffer.position(0);
        int activeCount = 0;
        for (int i = 0; i < NUM_PARTICLES; i++) {
            if (particleLife[i] >= 0 && particleAlpha[i] > 0.01f) {
                vertexBuffer.put(particleX[i]);
                vertexBuffer.put(particleY[i]);
                vertexBuffer.put(particleSize[i]);
                vertexBuffer.put(particleAlpha[i]);
                vertexBuffer.put(particlePhase[i]);
                vertexBuffer.put(particleRotation[i]);
                activeCount++;
            }
        }

        if (activeCount == 0) return;

        vertexBuffer.position(0);

        // Blending suave para humo
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        GLES30.glUseProgram(shaderProgram);
        GLES30.glUniform1f(uAspectLoc, aspectRatio);
        GLES30.glUniform1f(uTimeLoc, time);

        int stride = FLOATS_PER_PARTICLE * 4;

        GLES30.glEnableVertexAttribArray(aPositionLoc);
        vertexBuffer.position(0);
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, stride, vertexBuffer);

        GLES30.glEnableVertexAttribArray(aSizeLoc);
        vertexBuffer.position(2);
        GLES30.glVertexAttribPointer(aSizeLoc, 1, GLES30.GL_FLOAT, false, stride, vertexBuffer);

        GLES30.glEnableVertexAttribArray(aAlphaLoc);
        vertexBuffer.position(3);
        GLES30.glVertexAttribPointer(aAlphaLoc, 1, GLES30.GL_FLOAT, false, stride, vertexBuffer);

        GLES30.glEnableVertexAttribArray(aPhaseLoc);
        vertexBuffer.position(4);
        GLES30.glVertexAttribPointer(aPhaseLoc, 1, GLES30.GL_FLOAT, false, stride, vertexBuffer);

        GLES30.glEnableVertexAttribArray(aRotationLoc);
        vertexBuffer.position(5);
        GLES30.glVertexAttribPointer(aRotationLoc, 1, GLES30.GL_FLOAT, false, stride, vertexBuffer);

        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, activeCount);

        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glDisableVertexAttribArray(aSizeLoc);
        GLES30.glDisableVertexAttribArray(aAlphaLoc);
        GLES30.glDisableVertexAttribArray(aPhaseLoc);
        GLES30.glDisableVertexAttribArray(aRotationLoc);
    }

    // ═══════════════════════════════════════════════════════════════
    // SETTERS
    // ═══════════════════════════════════════════════════════════════
    public void setScreenSize(int w, int h) {
        aspectRatio = (float) w / h;
    }

    public void show() { visible = true; }
    public void hide() { visible = false; }
    public boolean isVisible() { return visible; }

    public void dispose() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        initialized = false;
    }
}
