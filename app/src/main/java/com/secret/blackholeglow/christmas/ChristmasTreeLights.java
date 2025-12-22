package com.secret.blackholeglow.christmas;

import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║   💡 ChristmasTreeLights - Luces Animadas del Árbol de Navidad           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * Renderiza luces brillantes con efecto twinkle sobre el árbol navideño.
 *
 * Características:
 * - 35 luces distribuidas en forma de cono (silueta del árbol)
 * - Colores: Dorado cálido (40%), Rojo (20%), Verde (15%), Azul (15%), Blanco (10%)
 * - Cada luz parpadea a su propio ritmo (fase aleatoria)
 * - Glow suave difuminado alrededor de cada punto
 * - Efecto "ola" opcional que enciende luces en secuencia
 *
 * Técnica: GL_POINTS con fragment shader para glow radial
 */
public class ChristmasTreeLights {
    private static final String TAG = "TreeLights";

    // ═══════════════════════════════════════════════════════════════
    // CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════

    private static final int NUM_LIGHTS = 45;           // Cantidad de luces
    private static final float TREE_CENTER_X = -0.05f;  // Centro X del árbol (ligeramente izquierda)
    private static final float TREE_TOP_Y = 0.72f;      // Punta del árbol (cerca de la estrella)
    private static final float TREE_BOTTOM_Y = 0.18f;   // Base del árbol (encima de los regalos)
    private static final float TREE_WIDTH = 0.26f;      // Ancho máximo en la base

    // Tamaños de las luces
    private static final float MIN_POINT_SIZE = 12.0f;
    private static final float MAX_POINT_SIZE = 22.0f;

    // Velocidades de parpadeo
    private static final float MIN_TWINKLE_SPEED = 1.5f;
    private static final float MAX_TWINKLE_SPEED = 4.0f;

    // ═══════════════════════════════════════════════════════════════
    // COLORES NAVIDEÑOS (RGBA)
    // ═══════════════════════════════════════════════════════════════

    // Dorado cálido - 40%
    private static final float[] COLOR_GOLD = {1.0f, 0.85f, 0.4f, 1.0f};
    // Rojo festivo - 20%
    private static final float[] COLOR_RED = {1.0f, 0.25f, 0.2f, 1.0f};
    // Verde pino - 15%
    private static final float[] COLOR_GREEN = {0.3f, 0.9f, 0.4f, 1.0f};
    // Azul hielo - 15%
    private static final float[] COLOR_BLUE = {0.4f, 0.7f, 1.0f, 1.0f};
    // Blanco cálido - 10%
    private static final float[] COLOR_WHITE = {1.0f, 0.95f, 0.85f, 1.0f};

    // ═══════════════════════════════════════════════════════════════
    // OPENGL
    // ═══════════════════════════════════════════════════════════════

    private int shaderProgram;
    private int vboId;
    private FloatBuffer vertexBuffer;

    // Attribute locations
    private int aPositionLoc;
    private int aColorLoc;
    private int aSizeLoc;
    private int aPhaseLoc;
    private int aSpeedLoc;

    // Uniform locations
    private int uTimeLoc;
    private int uAspectLoc;

    // Estado
    private boolean initialized = false;
    private boolean dataGenerated = false;
    private boolean visible = true;
    private float time = 0f;
    private float aspectRatio = 1.0f;

    // Datos de las luces
    private float[] lightData;  // x, y, r, g, b, a, size, phase, speed (9 floats por luz)
    private static final int FLOATS_PER_LIGHT = 9;

    // ═══════════════════════════════════════════════════════════════
    // SHADERS
    // ═══════════════════════════════════════════════════════════════

    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "precision highp float;\n" +
        "\n" +
        "// Atributos por luz\n" +
        "in vec2 a_Position;   // Posición de la luz\n" +
        "in vec4 a_Color;      // Color RGBA\n" +
        "in float a_Size;      // Tamaño base del punto\n" +
        "in float a_Phase;     // Fase inicial del parpadeo\n" +
        "in float a_Speed;     // Velocidad de parpadeo\n" +
        "\n" +
        "// Uniforms\n" +
        "uniform float u_Time;\n" +
        "uniform float u_Aspect;\n" +
        "\n" +
        "// Output al fragment shader\n" +
        "out vec4 v_Color;\n" +
        "out float v_Brightness;\n" +
        "\n" +
        "void main() {\n" +
        "    // Calcular brillo con parpadeo suave\n" +
        "    float twinkle = sin(u_Time * a_Speed + a_Phase);\n" +
        "    twinkle = twinkle * 0.5 + 0.5;  // Normalizar a [0, 1]\n" +
        "    twinkle = 0.4 + twinkle * 0.6;   // Rango [0.4, 1.0] - nunca se apaga del todo\n" +
        "    \n" +
        "    // Posición con corrección de aspecto\n" +
        "    vec2 pos = a_Position;\n" +
        "    pos.x /= u_Aspect;\n" +
        "    \n" +
        "    gl_Position = vec4(pos, 0.0, 1.0);\n" +
        "    \n" +
        "    // Tamaño del punto varía con el brillo\n" +
        "    gl_PointSize = a_Size * (0.8 + twinkle * 0.4);\n" +
        "    \n" +
        "    // Pasar color y brillo al fragment\n" +
        "    v_Color = a_Color;\n" +
        "    v_Brightness = twinkle;\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "\n" +
        "in vec4 v_Color;\n" +
        "in float v_Brightness;\n" +
        "\n" +
        "out vec4 fragColor;\n" +
        "\n" +
        "void main() {\n" +
        "    // Distancia desde el centro del punto\n" +
        "    vec2 coord = gl_PointCoord - vec2(0.5);\n" +
        "    float dist = length(coord) * 2.0;  // [0, 1] desde centro a borde\n" +
        "    \n" +
        "    // Descartar píxeles fuera del círculo\n" +
        "    if (dist > 1.0) discard;\n" +
        "    \n" +
        "    // Crear glow suave con múltiples capas\n" +
        "    // Núcleo brillante\n" +
        "    float core = 1.0 - smoothstep(0.0, 0.3, dist);\n" +
        "    // Glow medio\n" +
        "    float midGlow = 1.0 - smoothstep(0.0, 0.6, dist);\n" +
        "    // Glow exterior\n" +
        "    float outerGlow = 1.0 - smoothstep(0.0, 1.0, dist);\n" +
        "    \n" +
        "    // Combinar capas\n" +
        "    float glow = core * 0.6 + midGlow * 0.3 + outerGlow * 0.1;\n" +
        "    glow *= v_Brightness;\n" +
        "    \n" +
        "    // Color final con brillo\n" +
        "    vec3 finalColor = v_Color.rgb * (0.8 + v_Brightness * 0.4);\n" +
        "    \n" +
        "    // Añadir blanco al centro para efecto de luz intensa\n" +
        "    finalColor = mix(finalColor, vec3(1.0), core * 0.5 * v_Brightness);\n" +
        "    \n" +
        "    fragColor = vec4(finalColor, glow * v_Color.a);\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTOR E INICIALIZACIÓN
    // ═══════════════════════════════════════════════════════════════

    public ChristmasTreeLights() {
        // Constructor vacío - init() se llama después
    }

    public void init() {
        if (dataGenerated) return;

        Log.d(TAG, "💡 Preparando datos de luces del árbol...");

        // Solo generar datos - la inicialización OpenGL se hace en draw()
        generateLightData();
        dataGenerated = true;

        Log.d(TAG, "✅ Datos de " + NUM_LIGHTS + " luces preparados");
    }

    /**
     * Inicializa recursos OpenGL (llamado desde draw cuando el contexto está listo)
     */
    private void initOpenGL() {
        if (initialized) return;

        Log.d(TAG, "🔧 Inicializando OpenGL para luces...");

        // Crear shader program
        if (!createShaderProgram()) {
            Log.e(TAG, "❌ Error creando shader program - reintentando en próximo frame");
            return;
        }

        // Crear VBO
        createVBO();

        initialized = true;
        Log.d(TAG, "✅ OpenGL inicializado para " + NUM_LIGHTS + " luces");
    }

    private void generateLightData() {
        lightData = new float[NUM_LIGHTS * FLOATS_PER_LIGHT];
        Random random = new Random(42);  // Seed fijo para reproducibilidad

        int index = 0;
        for (int i = 0; i < NUM_LIGHTS; i++) {
            // Distribución en forma de cono (árbol)
            // t va de 0 (punta) a 1 (base)
            float t = random.nextFloat();
            t = (float) Math.pow(t, 0.7);  // Más luces hacia la base

            // Altura: interpolación entre punta y base
            float y = TREE_TOP_Y - t * (TREE_TOP_Y - TREE_BOTTOM_Y);

            // Ancho disponible a esta altura (cono)
            float widthAtHeight = TREE_WIDTH * t;

            // Posición X aleatoria dentro del cono
            float x = TREE_CENTER_X + (random.nextFloat() - 0.5f) * 2.0f * widthAtHeight;

            // Pequeña variación para que no queden en línea perfecta
            x += (random.nextFloat() - 0.5f) * 0.03f;
            y += (random.nextFloat() - 0.5f) * 0.02f;

            // Seleccionar color basado en probabilidad
            float[] color = selectColor(random.nextFloat());

            // Tamaño aleatorio
            float size = MIN_POINT_SIZE + random.nextFloat() * (MAX_POINT_SIZE - MIN_POINT_SIZE);

            // Fase inicial aleatoria (para que parpadeen desfasadas)
            float phase = random.nextFloat() * (float)(Math.PI * 2.0);

            // Velocidad de parpadeo aleatoria
            float speed = MIN_TWINKLE_SPEED + random.nextFloat() * (MAX_TWINKLE_SPEED - MIN_TWINKLE_SPEED);

            // Guardar datos: x, y, r, g, b, a, size, phase, speed
            lightData[index++] = x;
            lightData[index++] = y;
            lightData[index++] = color[0];  // R
            lightData[index++] = color[1];  // G
            lightData[index++] = color[2];  // B
            lightData[index++] = color[3];  // A
            lightData[index++] = size;
            lightData[index++] = phase;
            lightData[index++] = speed;
        }

        Log.d(TAG, "📊 Datos de " + NUM_LIGHTS + " luces generados");
    }

    private float[] selectColor(float probability) {
        // Distribución: Dorado 40%, Rojo 20%, Verde 15%, Azul 15%, Blanco 10%
        if (probability < 0.40f) {
            return COLOR_GOLD;
        } else if (probability < 0.60f) {
            return COLOR_RED;
        } else if (probability < 0.75f) {
            return COLOR_GREEN;
        } else if (probability < 0.90f) {
            return COLOR_BLUE;
        } else {
            return COLOR_WHITE;
        }
    }

    private boolean createShaderProgram() {
        int vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        if (vertexShader == 0 || fragmentShader == 0) {
            return false;
        }

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vertexShader);
        GLES30.glAttachShader(shaderProgram, fragmentShader);
        GLES30.glLinkProgram(shaderProgram);

        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(shaderProgram, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Error linkeando: " + GLES30.glGetProgramInfoLog(shaderProgram));
            GLES30.glDeleteProgram(shaderProgram);
            return false;
        }

        // Obtener locations
        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        aColorLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Color");
        aSizeLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Size");
        aPhaseLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Phase");
        aSpeedLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Speed");

        uTimeLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Time");
        uAspectLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Aspect");

        // Eliminar shaders individuales
        GLES30.glDeleteShader(vertexShader);
        GLES30.glDeleteShader(fragmentShader);

        return true;
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

    private void createVBO() {
        // Crear buffer
        ByteBuffer bb = ByteBuffer.allocateDirect(lightData.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(lightData);
        vertexBuffer.position(0);

        // Crear VBO
        int[] vbos = new int[1];
        GLES30.glGenBuffers(1, vbos, 0);
        vboId = vbos[0];

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, lightData.length * 4, vertexBuffer, GLES30.GL_STATIC_DRAW);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
    }

    // ═══════════════════════════════════════════════════════════════
    // UPDATE Y DRAW
    // ═══════════════════════════════════════════════════════════════

    public void update(float deltaTime) {
        // Actualizar tiempo para animación
        time += deltaTime;

        // Resetear para evitar overflow después de ~1 hora
        if (time > 3600f) {
            time = 0f;
        }
    }

    public void draw() {
        if (!visible || !dataGenerated) return;

        // Lazy init de OpenGL cuando el contexto está listo
        if (!initialized) {
            initOpenGL();
            if (!initialized) return;  // Aún no listo
        }

        // Habilitar blending para el glow
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // Usar programa
        GLES30.glUseProgram(shaderProgram);

        // Uniforms
        GLES30.glUniform1f(uTimeLoc, time);
        GLES30.glUniform1f(uAspectLoc, aspectRatio);

        // Bind VBO
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId);

        int stride = FLOATS_PER_LIGHT * 4;  // 9 floats * 4 bytes

        // Position (2 floats)
        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, stride, 0);

        // Color (4 floats)
        GLES30.glEnableVertexAttribArray(aColorLoc);
        GLES30.glVertexAttribPointer(aColorLoc, 4, GLES30.GL_FLOAT, false, stride, 2 * 4);

        // Size (1 float)
        GLES30.glEnableVertexAttribArray(aSizeLoc);
        GLES30.glVertexAttribPointer(aSizeLoc, 1, GLES30.GL_FLOAT, false, stride, 6 * 4);

        // Phase (1 float)
        GLES30.glEnableVertexAttribArray(aPhaseLoc);
        GLES30.glVertexAttribPointer(aPhaseLoc, 1, GLES30.GL_FLOAT, false, stride, 7 * 4);

        // Speed (1 float)
        GLES30.glEnableVertexAttribArray(aSpeedLoc);
        GLES30.glVertexAttribPointer(aSpeedLoc, 1, GLES30.GL_FLOAT, false, stride, 8 * 4);

        // Dibujar puntos
        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, NUM_LIGHTS);

        // Cleanup
        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glDisableVertexAttribArray(aColorLoc);
        GLES30.glDisableVertexAttribArray(aSizeLoc);
        GLES30.glDisableVertexAttribArray(aPhaseLoc);
        GLES30.glDisableVertexAttribArray(aSpeedLoc);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
    }

    // ═══════════════════════════════════════════════════════════════
    // SETTERS Y CONTROL
    // ═══════════════════════════════════════════════════════════════

    public void setScreenSize(int width, int height) {
        aspectRatio = (float) width / height;
    }

    public void show() {
        visible = true;
    }

    public void hide() {
        visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    // ═══════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════

    public void dispose() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        if (vboId != 0) {
            int[] vbos = {vboId};
            GLES30.glDeleteBuffers(1, vbos, 0);
            vboId = 0;
        }
        initialized = false;
        Log.d(TAG, "🗑️ ChristmasTreeLights liberado");
    }
}
