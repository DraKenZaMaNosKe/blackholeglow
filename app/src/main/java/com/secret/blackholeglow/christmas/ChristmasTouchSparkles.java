package com.secret.blackholeglow.christmas;

import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║   ✨ ChristmasTouchSparkles - Chispitas navideñas al tocar               ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║   - Partículas que aparecen donde tocas la pantalla                      ║
 * ║   - Siguen el dedo mientras lo mueves                                    ║
 * ║   - Colores navideños: dorado, rojo, verde, blanco                       ║
 * ║   - Se desvanecen suavemente                                             ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 */
public class ChristmasTouchSparkles {
    private static final String TAG = "TouchSparkles";

    // Configuración
    private static final int MAX_PARTICLES = 80;
    private static final float PARTICLE_LIFE = 1.2f;      // Segundos de vida
    private static final float SPAWN_RATE = 0.02f;        // Segundos entre spawns
    private static final float SPREAD = 0.08f;            // Dispersión inicial
    private static final float GRAVITY = -0.3f;           // Caída suave

    // Partículas
    private float[] posX = new float[MAX_PARTICLES];
    private float[] posY = new float[MAX_PARTICLES];
    private float[] velX = new float[MAX_PARTICLES];
    private float[] velY = new float[MAX_PARTICLES];
    private float[] life = new float[MAX_PARTICLES];
    private float[] size = new float[MAX_PARTICLES];
    private float[] colorR = new float[MAX_PARTICLES];
    private float[] colorG = new float[MAX_PARTICLES];
    private float[] colorB = new float[MAX_PARTICLES];

    // Estado del touch
    private boolean isTouching = false;
    private float touchX = 0f;
    private float touchY = 0f;
    private float spawnTimer = 0f;

    // OpenGL
    private int shaderProgram;
    private FloatBuffer vertexBuffer;
    private int aPositionLoc, aSizeLoc, aColorLoc, aAlphaLoc;
    private int uAspectLoc;

    private boolean initialized = false;
    private float aspectRatio = 1.0f;
    private int nextParticle = 0;

    // Colores navideños
    private static final float[][] COLORS = {
        {1.0f, 0.85f, 0.2f},   // Dorado
        {1.0f, 0.9f, 0.5f},    // Dorado claro
        {1.0f, 0.2f, 0.2f},    // Rojo
        {0.2f, 0.9f, 0.3f},    // Verde
        {1.0f, 1.0f, 1.0f},    // Blanco
        {1.0f, 0.6f, 0.8f},    // Rosa
    };

    private static final int FLOATS_PER_PARTICLE = 5; // x, y, size, alpha, colorIndex

    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "in vec2 a_Position;\n" +
        "in float a_Size;\n" +
        "in float a_Alpha;\n" +
        "in vec3 a_Color;\n" +
        "uniform float u_Aspect;\n" +
        "out float v_Alpha;\n" +
        "out vec3 v_Color;\n" +
        "void main() {\n" +
        "    vec2 pos = a_Position;\n" +
        "    pos.x /= u_Aspect;\n" +
        "    gl_Position = vec4(pos, 0.0, 1.0);\n" +
        "    gl_PointSize = a_Size;\n" +
        "    v_Alpha = a_Alpha;\n" +
        "    v_Color = a_Color;\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "in float v_Alpha;\n" +
        "in vec3 v_Color;\n" +
        "out vec4 fragColor;\n" +
        "\n" +
        "void main() {\n" +
        "    vec2 coord = gl_PointCoord - 0.5;\n" +
        "    float dist = length(coord);\n" +
        "    \n" +
        "    // Estrella de 4 puntas\n" +
        "    float angle = atan(coord.y, coord.x);\n" +
        "    float star = abs(cos(angle * 2.0)) * 0.3 + 0.2;\n" +
        "    \n" +
        "    // Core brillante + rayos\n" +
        "    float core = 1.0 - smoothstep(0.0, 0.15, dist);\n" +
        "    float rays = (1.0 - smoothstep(0.0, star, dist)) * 0.6;\n" +
        "    float glow = (1.0 - smoothstep(0.0, 0.5, dist)) * 0.3;\n" +
        "    \n" +
        "    float brightness = core + rays + glow;\n" +
        "    \n" +
        "    fragColor = vec4(v_Color * brightness, brightness * v_Alpha);\n" +
        "}\n";

    public ChristmasTouchSparkles() {
        // Inicializar partículas como inactivas
        for (int i = 0; i < MAX_PARTICLES; i++) {
            life[i] = -1f;
        }
        Log.d(TAG, "✨ Touch Sparkles creado");
    }

    private void initOpenGL() {
        if (initialized) return;

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
            Log.e(TAG, "Link error");
            return;
        }

        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);

        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        aSizeLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Size");
        aAlphaLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Alpha");
        aColorLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Color");
        uAspectLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Aspect");

        // Buffer: x, y, size, alpha, r, g, b = 7 floats per particle
        ByteBuffer bb = ByteBuffer.allocateDirect(MAX_PARTICLES * 7 * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();

        initialized = true;
        Log.d(TAG, "✨ OpenGL inicializado");
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

    public void update(float dt) {
        // Spawn nuevas partículas si está tocando
        if (isTouching) {
            spawnTimer += dt;
            while (spawnTimer >= SPAWN_RATE) {
                spawnParticle();
                spawnTimer -= SPAWN_RATE;
            }
        }

        // Actualizar partículas existentes
        for (int i = 0; i < MAX_PARTICLES; i++) {
            if (life[i] > 0) {
                life[i] -= dt;

                // Física simple
                velY[i] += GRAVITY * dt;
                posX[i] += velX[i] * dt;
                posY[i] += velY[i] * dt;

                // Reducir tamaño gradualmente
                size[i] *= (1.0f - dt * 0.5f);
            }
        }
    }

    private void spawnParticle() {
        int i = nextParticle;
        nextParticle = (nextParticle + 1) % MAX_PARTICLES;

        // Posición cerca del touch con dispersión
        posX[i] = touchX + (float)(Math.random() - 0.5) * SPREAD;
        posY[i] = touchY + (float)(Math.random() - 0.5) * SPREAD;

        // Velocidad inicial (explotan hacia afuera y arriba)
        float angle = (float)(Math.random() * Math.PI * 2.0);
        float speed = 0.3f + (float)Math.random() * 0.4f;
        velX[i] = (float)Math.cos(angle) * speed;
        velY[i] = (float)Math.sin(angle) * speed * 0.7f + 0.2f; // Bias hacia arriba

        // Vida y tamaño
        life[i] = PARTICLE_LIFE;
        size[i] = 25f + (float)Math.random() * 35f;

        // Color aleatorio navideño
        int colorIdx = (int)(Math.random() * COLORS.length);
        colorR[i] = COLORS[colorIdx][0];
        colorG[i] = COLORS[colorIdx][1];
        colorB[i] = COLORS[colorIdx][2];
    }

    public void draw() {
        if (!initialized) {
            initOpenGL();
            if (!initialized) return;
        }

        // Contar y construir buffer
        vertexBuffer.clear();
        int count = 0;

        for (int i = 0; i < MAX_PARTICLES; i++) {
            if (life[i] > 0) {
                float alpha = Math.min(1.0f, life[i] / (PARTICLE_LIFE * 0.3f));

                vertexBuffer.put(posX[i]);
                vertexBuffer.put(posY[i]);
                vertexBuffer.put(size[i]);
                vertexBuffer.put(alpha);
                vertexBuffer.put(colorR[i]);
                vertexBuffer.put(colorG[i]);
                vertexBuffer.put(colorB[i]);
                count++;
            }
        }

        if (count == 0) return;

        vertexBuffer.position(0);

        // Blending aditivo para brillo
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE);

        GLES30.glUseProgram(shaderProgram);
        GLES30.glUniform1f(uAspectLoc, aspectRatio);

        int stride = 7 * 4; // 7 floats * 4 bytes

        vertexBuffer.position(0);
        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, stride, vertexBuffer);

        vertexBuffer.position(2);
        GLES30.glEnableVertexAttribArray(aSizeLoc);
        GLES30.glVertexAttribPointer(aSizeLoc, 1, GLES30.GL_FLOAT, false, stride, vertexBuffer);

        vertexBuffer.position(3);
        GLES30.glEnableVertexAttribArray(aAlphaLoc);
        GLES30.glVertexAttribPointer(aAlphaLoc, 1, GLES30.GL_FLOAT, false, stride, vertexBuffer);

        vertexBuffer.position(4);
        GLES30.glEnableVertexAttribArray(aColorLoc);
        GLES30.glVertexAttribPointer(aColorLoc, 3, GLES30.GL_FLOAT, false, stride, vertexBuffer);

        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, count);

        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glDisableVertexAttribArray(aSizeLoc);
        GLES30.glDisableVertexAttribArray(aAlphaLoc);
        GLES30.glDisableVertexAttribArray(aColorLoc);

        // Restaurar blending normal
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
    }

    // ═══════════════════════════════════════════════════════════════
    // TOUCH EVENTS
    // ═══════════════════════════════════════════════════════════════

    public void onTouchDown(float x, float y) {
        isTouching = true;
        touchX = x;
        touchY = y;
        spawnTimer = 0f;
        // Spawn inmediato de varias partículas
        for (int i = 0; i < 5; i++) {
            spawnParticle();
        }
    }

    public void onTouchMove(float x, float y) {
        touchX = x;
        touchY = y;
    }

    public void onTouchUp() {
        isTouching = false;
        // Burst final de partículas
        for (int i = 0; i < 8; i++) {
            spawnParticle();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SETTERS
    // ═══════════════════════════════════════════════════════════════

    public void setScreenSize(int w, int h) {
        aspectRatio = (float) w / h;
    }

    public void dispose() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        initialized = false;
    }
}
