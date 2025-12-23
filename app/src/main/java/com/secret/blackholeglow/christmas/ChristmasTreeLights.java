package com.secret.blackholeglow.christmas;

import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║   💡 ChristmasTreeLights - 11 Luces Navideñas con Colores Individuales   ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                           ║
 * ║   CÓMO EDITAR:                                                            ║
 * ║   1. Posición: Edita LIGHT_POSITIONS[i] = {X, Y}                         ║
 * ║   2. Color:    Edita LIGHT_COLORS[i] = {R, G, B}                         ║
 * ║                                                                           ║
 * ║   Coordenadas: X (-1 izq, 1 der), Y (-1 abajo, 1 arriba)                 ║
 * ║   Colores:     R, G, B de 0.0 a 1.0                                      ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 */
public class ChristmasTreeLights {
    private static final String TAG = "TreeLights";

    // ═══════════════════════════════════════════════════════════════
    // 🎯 POSICIONES DE LAS 11 LUCES (edita aquí)
    // ═══════════════════════════════════════════════════════════════

    // ═══════════════════════════════════════════════════════════════
    // 📍 POSICIONES: X(-1=izq, 0=centro, 1=der) Y(-1=abajo, 0=centro, 1=arriba)
    // ═══════════════════════════════════════════════════════════════
    private static final float[][] LIGHT_POSITIONS = {
        // ⭐ Estrella en la punta (CENTRO: X=-0.069)
        { -0.069f,  0.460f },  // Luz 0  - ESTRELLA ⭐

        // Todas en línea recta debajo de la estrella (X=-0.069)
        // Muévelas manualmente desde aquí
        { -0.069f,  0.38f },   // Luz 1  - NARANJA
        { -0.069f,  0.32f },   // Luz 2  - AMARILLO
        { -0.069f,  0.26f },   // Luz 3  - VERDE LIMA
        { -0.069f,  0.20f },   // Luz 4  - VERDE
        { -0.069f,  0.14f },   // Luz 5  - VERDE AGUA
        { -0.069f,  0.08f },   // Luz 6  - CYAN
        { -0.069f,  0.02f },   // Luz 7  - AZUL CLARO
        { -0.069f, -0.04f },   // Luz 8  - AZUL
        { -0.069f, -0.10f },   // Luz 9  - VIOLETA
        { -0.069f, -0.16f },   // Luz 10 - MAGENTA
    };

    // ═══════════════════════════════════════════════════════════════
    // 🎨 COLORES DE LAS 11 LUCES (edita aquí) - RGB de 0.0 a 1.0
    // ═══════════════════════════════════════════════════════════════

    private static final float[][] LIGHT_COLORS = {
        // Colores únicos para identificar cada luz fácilmente
        { 1.00f, 0.50f, 0.00f },  // Luz 0  - ROJO
        { 1.00f, 0.50f, 0.00f },  // Luz 1  - NARANJA
        { 1.00f, 1.00f, 0.00f },  // Luz 2  - AMARILLO
        { 0.50f, 1.00f, 0.00f },  // Luz 3  - VERDE LIMA
        { 0.00f, 1.00f, 0.00f },  // Luz 4  - VERDE
        { 0.00f, 1.00f, 0.50f },  // Luz 5  - VERDE AGUA
        { 0.00f, 1.00f, 1.00f },  // Luz 6  - CYAN
        { 0.00f, 0.50f, 1.00f },  // Luz 7  - AZUL CLARO
        { 0.00f, 0.00f, 1.00f },  // Luz 8  - AZUL
        { 0.50f, 0.00f, 1.00f },  // Luz 9  - VIOLETA
        { 1.00f, 0.00f, 1.00f },  // Luz 10 - MAGENTA
    };

    private static final int NUM_LIGHTS = LIGHT_POSITIONS.length;
    private static final float POINT_SIZE = 80.0f;       // Tamaño luces normales
    private static final float STAR_SIZE = 180.0f;       // ⭐ Tamaño de la estrella (Luz 0)

    // ═══════════════════════════════════════════════════════════════
    // OPENGL
    // ═══════════════════════════════════════════════════════════════

    private int shaderProgram, vboId;
    private FloatBuffer vertexBuffer;
    private int aPositionLoc, aColorLoc, aSizeLoc, aPhaseLoc, aSpeedLoc;
    private int uTimeLoc, uAspectLoc;

    private boolean initialized = false;
    private boolean dataGenerated = false;
    private boolean needsUpdate = false;
    private boolean visible = true;
    private float time = 0f;
    private float aspectRatio = 1.0f;

    private float[] lightData;
    private static final int FLOATS_PER_LIGHT = 9;

    // Copias locales editables
    private float[][] positions;
    private float[][] colors;

    // ═══════════════════════════════════════════════════════════════
    // SHADERS
    // ═══════════════════════════════════════════════════════════════

    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "precision highp float;\n" +
        "in vec2 a_Position;\n" +
        "in vec4 a_Color;\n" +
        "in float a_Size;\n" +
        "in float a_Phase;\n" +
        "in float a_Speed;\n" +
        "uniform float u_Time;\n" +
        "uniform float u_Aspect;\n" +
        "out vec4 v_Color;\n" +
        "out float v_Brightness;\n" +
        "void main() {\n" +
        "    float twinkle = sin(u_Time * a_Speed + a_Phase) * 0.5 + 0.5;\n" +
        "    twinkle = 0.6 + twinkle * 0.4;\n" +
        "    vec2 pos = a_Position;\n" +
        "    pos.x /= u_Aspect;\n" +
        "    gl_Position = vec4(pos, 0.0, 1.0);\n" +
        "    gl_PointSize = a_Size * (0.85 + twinkle * 0.3);\n" +
        "    v_Color = a_Color;\n" +
        "    v_Brightness = twinkle;\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "in vec4 v_Color;\n" +
        "in float v_Brightness;\n" +
        "out vec4 fragColor;\n" +
        "void main() {\n" +
        "    vec2 coord = gl_PointCoord - vec2(0.5);\n" +
        "    float dist = length(coord) * 2.0;\n" +
        "    bool isStar = v_Color.a > 1.5;\n" +
        "    \n" +
        "    if (isStar) {\n" +
        "        // ⭐ ESTRELLA con rayos orgánicos y bordes suaves\n" +
        "        float angle = atan(coord.y, coord.x);\n" +
        "        \n" +
        "        // Rayos de diferentes tamaños (4 principales + 4 secundarios)\n" +
        "        float ray1 = pow(abs(sin(angle * 2.0)), 8.0);\n" +  // 4 rayos largos
        "        float ray2 = pow(abs(sin(angle * 2.0 + 0.785)), 12.0) * 0.6;\n" +  // 4 rayos cortos (45°)
        "        float rays = ray1 + ray2;\n" +
        "        \n" +
        "        // Los rayos se desvanecen con la distancia (no linealmente)\n" +
        "        float rayFade = 1.0 - smoothstep(0.0, 0.9, dist);\n" +
        "        rays *= rayFade * rayFade;\n" +
        "        \n" +
        "        // Núcleo brillante\n" +
        "        float core = 1.0 - smoothstep(0.0, 0.12, dist);\n" +
        "        float innerGlow = 1.0 - smoothstep(0.0, 0.35, dist);\n" +
        "        \n" +
        "        // Glow exterior muy suave (desvanece los bordes)\n" +
        "        float outerGlow = 1.0 - smoothstep(0.0, 1.0, dist * dist);\n" +
        "        \n" +
        "        // Combinar todo\n" +
        "        float glow = core * 0.7 + innerGlow * 0.4 + outerGlow * 0.15 + rays * 0.5;\n" +
        "        glow *= v_Brightness;\n" +
        "        \n" +
        "        // Fade suave en los bordes (elimina el cuadrado)\n" +
        "        float edgeFade = 1.0 - smoothstep(0.4, 0.5, max(abs(coord.x), abs(coord.y)));\n" +
        "        glow *= edgeFade;\n" +
        "        \n" +
        "        vec3 gold = vec3(1.0, 0.85, 0.4);\n" +
        "        vec3 white = vec3(1.0, 1.0, 0.9);\n" +
        "        vec3 finalColor = mix(gold, white, core * v_Brightness);\n" +
        "        finalColor += rays * gold * 0.4 * v_Brightness;\n" +
        "        fragColor = vec4(finalColor, glow);\n" +
        "    } else {\n" +
        "        // Luces normales\n" +
        "        if (dist > 1.0) discard;\n" +
        "        float core = 1.0 - smoothstep(0.0, 0.25, dist);\n" +
        "        float innerGlow = 1.0 - smoothstep(0.0, 0.5, dist);\n" +
        "        float outerGlow = 1.0 - smoothstep(0.0, 1.0, dist);\n" +
        "        float glow = core * 0.5 + innerGlow * 0.35 + outerGlow * 0.15;\n" +
        "        glow *= v_Brightness;\n" +
        "        vec3 baseColor = v_Color.rgb * (0.9 + v_Brightness * 0.3);\n" +
        "        vec3 finalColor = mix(baseColor, vec3(1.0, 1.0, 0.9), core * 0.7 * v_Brightness);\n" +
        "        fragColor = vec4(finalColor, glow);\n" +
        "    }\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════

    public ChristmasTreeLights() {
        positions = new float[NUM_LIGHTS][2];
        colors = new float[NUM_LIGHTS][3];
        for (int i = 0; i < NUM_LIGHTS; i++) {
            positions[i][0] = LIGHT_POSITIONS[i][0];
            positions[i][1] = LIGHT_POSITIONS[i][1];
            colors[i][0] = LIGHT_COLORS[i][0];
            colors[i][1] = LIGHT_COLORS[i][1];
            colors[i][2] = LIGHT_COLORS[i][2];
        }
    }

    public void init() {
        if (dataGenerated) return;
        Log.d(TAG, "💡 Inicializando " + NUM_LIGHTS + " luces...");
        generateLightData();
        dataGenerated = true;
        logAll();
    }

    private void generateLightData() {
        lightData = new float[NUM_LIGHTS * FLOATS_PER_LIGHT];
        for (int i = 0; i < NUM_LIGHTS; i++) {
            int idx = i * FLOATS_PER_LIGHT;
            lightData[idx + 0] = positions[i][0];
            lightData[idx + 1] = positions[i][1];
            lightData[idx + 2] = colors[i][0];
            lightData[idx + 3] = colors[i][1];
            lightData[idx + 4] = colors[i][2];

            if (i == 0) {
                // ⭐ ESTRELLA - Luz 0: más grande, pulso más rápido, alpha especial
                lightData[idx + 5] = 2.0f;       // alpha > 1 = flag de estrella
                lightData[idx + 6] = STAR_SIZE;  // Tamaño grande
                lightData[idx + 7] = 0.0f;       // Phase
                lightData[idx + 8] = 4.0f;       // Speed más rápido
            } else {
                // Luces normales
                lightData[idx + 5] = 1.0f;
                lightData[idx + 6] = POINT_SIZE;
                lightData[idx + 7] = (float)(i * Math.PI * 2.0 / NUM_LIGHTS);
                lightData[idx + 8] = 2.0f + (i % 3) * 0.5f;
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 🎯 SETTERS - USA ESTOS PARA EDITAR EN RUNTIME
    // ═══════════════════════════════════════════════════════════════

    /**
     * Cambia la posición de una luz
     * @param index 0-10
     * @param x -1 (izq) a 1 (der)
     * @param y -1 (abajo) a 1 (arriba)
     */
    public void setLightPosition(int index, float x, float y) {
        if (index < 0 || index >= NUM_LIGHTS) return;
        positions[index][0] = x;
        positions[index][1] = y;
        if (lightData != null) {
            int idx = index * FLOATS_PER_LIGHT;
            lightData[idx + 0] = x;
            lightData[idx + 1] = y;
            needsUpdate = true;
        }
        Log.d(TAG, "📍 Luz " + index + " pos → (" + x + ", " + y + ")");
    }

    /**
     * Cambia el color de una luz
     * @param index 0-10
     * @param r Rojo 0.0-1.0
     * @param g Verde 0.0-1.0
     * @param b Azul 0.0-1.0
     */
    public void setLightColor(int index, float r, float g, float b) {
        if (index < 0 || index >= NUM_LIGHTS) return;
        colors[index][0] = r;
        colors[index][1] = g;
        colors[index][2] = b;
        if (lightData != null) {
            int idx = index * FLOATS_PER_LIGHT;
            lightData[idx + 2] = r;
            lightData[idx + 3] = g;
            lightData[idx + 4] = b;
            needsUpdate = true;
        }
        Log.d(TAG, "🎨 Luz " + index + " color → (" + r + ", " + g + ", " + b + ")");
    }

    /**
     * Imprime todas las posiciones y colores
     */
    public void logAll() {
        Log.d(TAG, "════════════════════════════════════════════════");
        Log.d(TAG, "📋 CONFIGURACIÓN DE LAS " + NUM_LIGHTS + " LUCES:");
        Log.d(TAG, "════════════════════════════════════════════════");
        for (int i = 0; i < NUM_LIGHTS; i++) {
            Log.d(TAG, String.format("  Luz %2d: pos(%5.2f, %5.2f)  color(%4.2f, %4.2f, %4.2f)",
                i, positions[i][0], positions[i][1],
                colors[i][0], colors[i][1], colors[i][2]));
        }
        Log.d(TAG, "════════════════════════════════════════════════");
    }

    // ═══════════════════════════════════════════════════════════════
    // OPENGL
    // ═══════════════════════════════════════════════════════════════

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
        if (status[0] == 0) return;

        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        aColorLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Color");
        aSizeLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Size");
        aPhaseLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Phase");
        aSpeedLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Speed");
        uTimeLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Time");
        uAspectLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Aspect");

        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);

        createVBO();
        initialized = true;
        Log.d(TAG, "✅ OpenGL listo");
    }

    private int compileShader(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private void createVBO() {
        ByteBuffer bb = ByteBuffer.allocateDirect(lightData.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(lightData);
        vertexBuffer.position(0);

        int[] vbos = new int[1];
        GLES30.glGenBuffers(1, vbos, 0);
        vboId = vbos[0];

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, lightData.length * 4, vertexBuffer, GLES30.GL_DYNAMIC_DRAW);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
    }

    private void updateVBO() {
        if (vertexBuffer == null || vboId == 0) return;
        vertexBuffer.position(0);
        vertexBuffer.put(lightData);
        vertexBuffer.position(0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId);
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, lightData.length * 4, vertexBuffer);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
        needsUpdate = false;
    }

    public void update(float dt) {
        time += dt;
        if (time > 3600f) time = 0f;
    }

    public void draw() {
        if (!visible || !dataGenerated) return;
        if (!initialized) { initOpenGL(); if (!initialized) return; }
        if (needsUpdate) updateVBO();

        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        GLES30.glUseProgram(shaderProgram);
        GLES30.glUniform1f(uTimeLoc, time);
        GLES30.glUniform1f(uAspectLoc, aspectRatio);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId);
        int stride = FLOATS_PER_LIGHT * 4;

        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, stride, 0);
        GLES30.glEnableVertexAttribArray(aColorLoc);
        GLES30.glVertexAttribPointer(aColorLoc, 4, GLES30.GL_FLOAT, false, stride, 2*4);
        GLES30.glEnableVertexAttribArray(aSizeLoc);
        GLES30.glVertexAttribPointer(aSizeLoc, 1, GLES30.GL_FLOAT, false, stride, 6*4);
        GLES30.glEnableVertexAttribArray(aPhaseLoc);
        GLES30.glVertexAttribPointer(aPhaseLoc, 1, GLES30.GL_FLOAT, false, stride, 7*4);
        GLES30.glEnableVertexAttribArray(aSpeedLoc);
        GLES30.glVertexAttribPointer(aSpeedLoc, 1, GLES30.GL_FLOAT, false, stride, 8*4);

        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, NUM_LIGHTS);

        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glDisableVertexAttribArray(aColorLoc);
        GLES30.glDisableVertexAttribArray(aSizeLoc);
        GLES30.glDisableVertexAttribArray(aPhaseLoc);
        GLES30.glDisableVertexAttribArray(aSpeedLoc);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
    }

    public void setScreenSize(int w, int h) { aspectRatio = (float)w/h; }
    public void show() { visible = true; }
    public void hide() { visible = false; }
    public boolean isVisible() { return visible; }

    public void dispose() {
        if (shaderProgram != 0) { GLES30.glDeleteProgram(shaderProgram); shaderProgram = 0; }
        if (vboId != 0) { GLES30.glDeleteBuffers(1, new int[]{vboId}, 0); vboId = 0; }
        initialized = false;
        dataGenerated = false;
    }
}
