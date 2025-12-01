package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.scenes.SceneConstants;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║   🎵 MusicIndicator3D - Ecualizador 3D con Cubos/Prismas             ║
 * ╠══════════════════════════════════════════════════════════════════════╣
 * ║  CARACTERÍSTICAS:                                                    ║
 * ║  • 7 barras 3D (cubos/prismas) con profundidad real                  ║
 * ║  • Gradiente arcoíris neón (rosa → rojo → naranja → verde → cyan)    ║
 * ║  • Peak holders flotantes con color según altura                     ║
 * ║  • Sistema de chispas 3D que explotan al pasar el peak               ║
 * ║  • Detección de beat para reactividad extra                          ║
 * ║  • Compatible con CameraController para perspectiva 3D               ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
public class MusicIndicator3D implements SceneObject, CameraAware {
    private static final String TAG = "MusicIndicator3D";

    // ════════════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN DEL ECUALIZADOR - 7 BARRAS POR RANGOS DE FRECUENCIA
    // ════════════════════════════════════════════════════════════════════════
    private static final int NUM_BARRAS = 7;
    private static final float SMOOTHING_FACTOR = 0.5f;

    // ════════════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN DE PEAK HOLDERS (Picos flotantes) 🔺
    // ════════════════════════════════════════════════════════════════════════
    private static final float PEAK_FALL_SPEED = 0.4f;
    private static final float PEAK_HOLD_TIME = 0.3f;
    private static final float MIN_BAR_LEVEL = 0.15f;

    // ════════════════════════════════════════════════════════════════════════
    // ESCALA DE BARRAS 3D (de grande a pequeño: bass → treble)
    // ════════════════════════════════════════════════════════════════════════
    private static final float[] BAR_WIDTH_SCALE = {1.6f, 1.35f, 1.15f, 1.0f, 0.85f, 0.7f, 0.55f};
    private static final float[] BAR_HEIGHT_SCALE = {1.0f, 0.92f, 0.84f, 0.76f, 0.68f, 0.60f, 0.52f};
    private static final float[] BAR_DEPTH_SCALE = {1.0f, 0.95f, 0.90f, 0.85f, 0.80f, 0.75f, 0.70f};

    // ════════════════════════════════════════════════════════════════════════
    // AMPLIFICACIÓN por barra - Barras treble SUPER REACTIVAS
    // ════════════════════════════════════════════════════════════════════════
    private static final float[] BAR_SENSITIVITY = {1.2f, 1.4f, 2.0f, 3.0f, 7.0f, 9.0f, 12.0f};

    // Mínimo de actividad cuando NO hay música
    private static final int MIN_LEVEL_NO_MUSIC = 1;
    private static final int MIN_LEVEL_WITH_MUSIC = 2;
    private static final int MAX_SEGMENTS = 6;

    // ════════════════════════════════════════════════════════════════════════
    // DETECCIÓN DE BEAT
    // ════════════════════════════════════════════════════════════════════════
    private static final float BEAT_THRESHOLD = 0.4f;
    private static final float BEAT_BOOST = 0.35f;
    private static final float BEAT_DECAY = 3.0f;

    private static final float PEAK_OFFSET = 0.15f;  // Distancia del peak sobre la barra

    // ════════════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN DE CHISPAS 3D ✨
    // ════════════════════════════════════════════════════════════════════════
    private static final float SPARK_THRESHOLD = 0.45f;
    private static final float SPARK_SPEED = 0.8f;
    private static final float SPARK_LIFETIME = 1.5f;
    private static final float SPARK_SIZE = 0.04f;
    private static final float SPARK_COOLDOWN = 0.15f;
    private static final int MAX_SPARKS_PER_TRIGGER = 2;

    // ════════════════════════════════════════════════════════════════════════
    // POSICIÓN Y TAMAÑO 3D
    // ════════════════════════════════════════════════════════════════════════
    private float posX, posY, posZ;           // Posición base del ecualizador en mundo 3D
    private float totalWidth = 2.2f;          // Ancho total del ecualizador
    private float maxHeight = 0.6f;           // Altura máxima de las barras
    private float barDepth = 0.06f;           // Profundidad de cada barra

    // ════════════════════════════════════════════════════════════════════════
    // DISTRIBUCIÓN EN Z (todas en línea - sin efecto abanico)
    // ════════════════════════════════════════════════════════════════════════
    private float zSpread = 0.0f;             // Sin abanico (todas en línea)
    // Todas las barras en la misma Z
    private static final float[] BAR_Z_OFFSET = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};

    // ════════════════════════════════════════════════════════════════════════
    // PERSPECTIVA Y EFECTOS VISUALES
    // ════════════════════════════════════════════════════════════════════════
    private float rotationX = 0f;             // Inclinación adelante/atrás
    private float rotationY = 30f;             // Rotación horizontal (se ajustará con TEST_MODE)
    private boolean drawReflection = false;   // Sin reflejo (más limpio)
    private float reflectionAlpha = 0.3f;     // Transparencia del reflejo

    // ════════════════════════════════════════════════════════════════════════
    // 🧪 MODO PRUEBA - Rotación automática (DESACTIVADO)
    // ════════════════════════════════════════════════════════════════════════
    private static final boolean TEST_ROTATION_MODE = false;  // Desactivado
    private float testRotationSpeed = 15f;
    private float lastLogTime = 0f;
    private float logInterval = 1.0f;

    // ════════════════════════════════════════════════════════════════════════
    // CILINDROS - Configuración
    // ════════════════════════════════════════════════════════════════════════
    private static final int CYLINDER_SEGMENTS = 16;  // Segmentos del círculo (más = más suave)
    private FloatBuffer cylinderVertexBuffer;
    private FloatBuffer cylinderColorBuffer;
    private ShortBuffer cylinderIndexBuffer;
    private int cylinderIndexCount;

    // OpenGL
    private int shaderProgram;
    private int glowShaderProgram;            // Shader para efecto glow
    private int aPositionHandle;
    private int aColorHandle;
    private int uMVPMatrixHandle;
    private int uGlowIntensityHandle;

    // Buffers para cubo unitario
    private FloatBuffer cubeVertexBuffer;
    private ShortBuffer cubeIndexBuffer;
    private FloatBuffer cubeColorBuffer;
    private int cubeIndexCount;

    // Cámara
    private CameraController camera;

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // Niveles de música
    private float[] barLevels = new float[NUM_BARRAS];
    private float[] smoothedLevels = new float[NUM_BARRAS];

    // Peak holders
    private float[] peakLevels = new float[NUM_BARRAS];
    private float[] peakHoldTimers = new float[NUM_BARRAS];

    // Beat detection
    private float bassLevel = 0f, midLevel = 0f, trebleLevel = 0f;
    private float lastBassLevel = 0f;
    private float currentBeatBoost = 0f;
    private float energyHistory = 0f;
    private boolean musicPlaying = false;

    // Geometría pre-calculada (ahora lee de SceneConstants para edición manual)
    private final float[] barXPositions = new float[NUM_BARRAS];
    private final float[] barYPositions = new float[NUM_BARRAS];  // Posición Y individual
    private final float[] barZPositions = new float[NUM_BARRAS];  // Posición Z individual
    private final float[] barWidths = new float[NUM_BARRAS];
    private final float[] barMaxHeights = new float[NUM_BARRAS];
    private final float[] barDepths = new float[NUM_BARRAS];
    // Rotaciones individuales por barra
    private final float[] barRotX = new float[NUM_BARRAS];
    private final float[] barRotY = new float[NUM_BARRAS];
    private final float[] barRotZ = new float[NUM_BARRAS];
    // Sensibilidad individual por barra
    private final float[] barSensitivity = new float[NUM_BARRAS];

    // Chispas 3D
    private static class Spark3D {
        float x, y, z;
        float vx, vy, vz;
        float[] color;
        float age;
        float lifetime;
        float peakY;
        boolean passedPeak;
        boolean exploded;
    }

    private static class MiniExplosion3D {
        float x, y, z;
        float age;
        float[] color;
        float size;
    }

    private ArrayList<Spark3D> sparks = new ArrayList<>();
    private ArrayList<MiniExplosion3D> explosions = new ArrayList<>();
    private float[] barCooldowns = new float[NUM_BARRAS];
    private Random random = new Random();

    private int frameCount = 0;
    private final Context context;

    /**
     * Constructor
     * @param posX Posición X en mundo 3D
     * @param posY Posición Y en mundo 3D (base del ecualizador)
     * @param posZ Posición Z en mundo 3D
     */
    public MusicIndicator3D(Context context, float posX, float posY, float posZ) {
        Log.d(TAG, "╔══════════════════════════════════════════════════════╗");
        Log.d(TAG, "║   🎵 CREANDO ECUALIZADOR 3D CON CUBOS               ║");
        Log.d(TAG, "╚══════════════════════════════════════════════════════╝");

        this.context = context;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;

        // Pre-calcular geometría de barras
        calculateBarGeometry();

        // Crear buffers del cubo unitario (respaldo)
        createCubeBuffers();

        // Crear buffers del CILINDRO (principal)
        createCylinderBuffers();

        // Crear shader
        createShader();

        Log.d(TAG, "✓ MusicIndicator3D creado en posición (" + posX + ", " + posY + ", " + posZ + ")");
    }

    /**
     * Calcula la geometría de cada barra LEYENDO DE SceneConstants
     * ¡Puedes editar SceneConstants.java para ajustar cada barra individualmente!
     */
    private void calculateBarGeometry() {
        // ═══════════════════════════════════════════════════════════════════
        // BARRA 0 - Lee de SceneConstants.EqBar0
        // ═══════════════════════════════════════════════════════════════════
        barXPositions[0] = posX + SceneConstants.EqBar0.POS_X;
        barYPositions[0] = posY + SceneConstants.EqBar0.POS_Y;
        barZPositions[0] = posZ + SceneConstants.EqBar0.POS_Z;
        barRotX[0] = SceneConstants.EqBar0.ROT_X;
        barRotY[0] = SceneConstants.EqBar0.ROT_Y;
        barRotZ[0] = SceneConstants.EqBar0.ROT_Z;
        barWidths[0] = SceneConstants.EqBar0.WIDTH;
        barMaxHeights[0] = SceneConstants.EqBar0.HEIGHT;
        barDepths[0] = SceneConstants.EqBar0.DEPTH;
        barSensitivity[0] = SceneConstants.EqBar0.SENSITIVITY;

        // ═══════════════════════════════════════════════════════════════════
        // BARRA 1 - Lee de SceneConstants.EqBar1
        // ═══════════════════════════════════════════════════════════════════
        barXPositions[1] = posX + SceneConstants.EqBar1.POS_X;
        barYPositions[1] = posY + SceneConstants.EqBar1.POS_Y;
        barZPositions[1] = posZ + SceneConstants.EqBar1.POS_Z;
        barRotX[1] = SceneConstants.EqBar1.ROT_X;
        barRotY[1] = SceneConstants.EqBar1.ROT_Y;
        barRotZ[1] = SceneConstants.EqBar1.ROT_Z;
        barWidths[1] = SceneConstants.EqBar1.WIDTH;
        barMaxHeights[1] = SceneConstants.EqBar1.HEIGHT;
        barDepths[1] = SceneConstants.EqBar1.DEPTH;
        barSensitivity[1] = SceneConstants.EqBar1.SENSITIVITY;

        // ═══════════════════════════════════════════════════════════════════
        // BARRA 2 - Lee de SceneConstants.EqBar2
        // ═══════════════════════════════════════════════════════════════════
        barXPositions[2] = posX + SceneConstants.EqBar2.POS_X;
        barYPositions[2] = posY + SceneConstants.EqBar2.POS_Y;
        barZPositions[2] = posZ + SceneConstants.EqBar2.POS_Z;
        barRotX[2] = SceneConstants.EqBar2.ROT_X;
        barRotY[2] = SceneConstants.EqBar2.ROT_Y;
        barRotZ[2] = SceneConstants.EqBar2.ROT_Z;
        barWidths[2] = SceneConstants.EqBar2.WIDTH;
        barMaxHeights[2] = SceneConstants.EqBar2.HEIGHT;
        barDepths[2] = SceneConstants.EqBar2.DEPTH;
        barSensitivity[2] = SceneConstants.EqBar2.SENSITIVITY;

        // ═══════════════════════════════════════════════════════════════════
        // BARRA 3 - Lee de SceneConstants.EqBar3 (CENTRO)
        // ═══════════════════════════════════════════════════════════════════
        barXPositions[3] = posX + SceneConstants.EqBar3.POS_X;
        barYPositions[3] = posY + SceneConstants.EqBar3.POS_Y;
        barZPositions[3] = posZ + SceneConstants.EqBar3.POS_Z;
        barRotX[3] = SceneConstants.EqBar3.ROT_X;
        barRotY[3] = SceneConstants.EqBar3.ROT_Y;
        barRotZ[3] = SceneConstants.EqBar3.ROT_Z;
        barWidths[3] = SceneConstants.EqBar3.WIDTH;
        barMaxHeights[3] = SceneConstants.EqBar3.HEIGHT;
        barDepths[3] = SceneConstants.EqBar3.DEPTH;
        barSensitivity[3] = SceneConstants.EqBar3.SENSITIVITY;

        // ═══════════════════════════════════════════════════════════════════
        // BARRA 4 - Lee de SceneConstants.EqBar4
        // ═══════════════════════════════════════════════════════════════════
        barXPositions[4] = posX + SceneConstants.EqBar4.POS_X;
        barYPositions[4] = posY + SceneConstants.EqBar4.POS_Y;
        barZPositions[4] = posZ + SceneConstants.EqBar4.POS_Z;
        barRotX[4] = SceneConstants.EqBar4.ROT_X;
        barRotY[4] = SceneConstants.EqBar4.ROT_Y;
        barRotZ[4] = SceneConstants.EqBar4.ROT_Z;
        barWidths[4] = SceneConstants.EqBar4.WIDTH;
        barMaxHeights[4] = SceneConstants.EqBar4.HEIGHT;
        barDepths[4] = SceneConstants.EqBar4.DEPTH;
        barSensitivity[4] = SceneConstants.EqBar4.SENSITIVITY;

        // ═══════════════════════════════════════════════════════════════════
        // BARRA 5 - Lee de SceneConstants.EqBar5
        // ═══════════════════════════════════════════════════════════════════
        barXPositions[5] = posX + SceneConstants.EqBar5.POS_X;
        barYPositions[5] = posY + SceneConstants.EqBar5.POS_Y;
        barZPositions[5] = posZ + SceneConstants.EqBar5.POS_Z;
        barRotX[5] = SceneConstants.EqBar5.ROT_X;
        barRotY[5] = SceneConstants.EqBar5.ROT_Y;
        barRotZ[5] = SceneConstants.EqBar5.ROT_Z;
        barWidths[5] = SceneConstants.EqBar5.WIDTH;
        barMaxHeights[5] = SceneConstants.EqBar5.HEIGHT;
        barDepths[5] = SceneConstants.EqBar5.DEPTH;
        barSensitivity[5] = SceneConstants.EqBar5.SENSITIVITY;

        // ═══════════════════════════════════════════════════════════════════
        // BARRA 6 - Lee de SceneConstants.EqBar6
        // ═══════════════════════════════════════════════════════════════════
        barXPositions[6] = posX + SceneConstants.EqBar6.POS_X;
        barYPositions[6] = posY + SceneConstants.EqBar6.POS_Y;
        barZPositions[6] = posZ + SceneConstants.EqBar6.POS_Z;
        barRotX[6] = SceneConstants.EqBar6.ROT_X;
        barRotY[6] = SceneConstants.EqBar6.ROT_Y;
        barRotZ[6] = SceneConstants.EqBar6.ROT_Z;
        barWidths[6] = SceneConstants.EqBar6.WIDTH;
        barMaxHeights[6] = SceneConstants.EqBar6.HEIGHT;
        barDepths[6] = SceneConstants.EqBar6.DEPTH;
        barSensitivity[6] = SceneConstants.EqBar6.SENSITIVITY;

        Log.d(TAG, "  ✓ Geometría de " + NUM_BARRAS + " barras cargada desde SceneConstants");
        Log.d(TAG, "  ✓ Edita SceneConstants.EqBar0-6 para ajustar cada barra");
    }

    /**
     * Crea los buffers para un cubo unitario (1x1x1 centrado en origen)
     */
    private void createCubeBuffers() {
        // Vértices del cubo unitario
        float[] vertices = {
            // Front face
            -0.5f, -0.5f,  0.5f,
             0.5f, -0.5f,  0.5f,
             0.5f,  0.5f,  0.5f,
            -0.5f,  0.5f,  0.5f,
            // Back face
            -0.5f, -0.5f, -0.5f,
            -0.5f,  0.5f, -0.5f,
             0.5f,  0.5f, -0.5f,
             0.5f, -0.5f, -0.5f,
            // Top face
            -0.5f,  0.5f, -0.5f,
            -0.5f,  0.5f,  0.5f,
             0.5f,  0.5f,  0.5f,
             0.5f,  0.5f, -0.5f,
            // Bottom face
            -0.5f, -0.5f, -0.5f,
             0.5f, -0.5f, -0.5f,
             0.5f, -0.5f,  0.5f,
            -0.5f, -0.5f,  0.5f,
            // Right face
             0.5f, -0.5f, -0.5f,
             0.5f,  0.5f, -0.5f,
             0.5f,  0.5f,  0.5f,
             0.5f, -0.5f,  0.5f,
            // Left face
            -0.5f, -0.5f, -0.5f,
            -0.5f, -0.5f,  0.5f,
            -0.5f,  0.5f,  0.5f,
            -0.5f,  0.5f, -0.5f,
        };

        // Índices para triangular el cubo
        short[] indices = {
            0,  1,  2,    0,  2,  3,   // front
            4,  5,  6,    4,  6,  7,   // back
            8,  9,  10,   8,  10, 11,  // top
            12, 13, 14,   12, 14, 15,  // bottom
            16, 17, 18,   16, 18, 19,  // right
            20, 21, 22,   20, 22, 23   // left
        };

        cubeIndexCount = indices.length;

        // Crear vertex buffer
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        cubeVertexBuffer = vbb.asFloatBuffer();
        cubeVertexBuffer.put(vertices);
        cubeVertexBuffer.position(0);

        // Crear index buffer
        ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 2);
        ibb.order(ByteOrder.nativeOrder());
        cubeIndexBuffer = ibb.asShortBuffer();
        cubeIndexBuffer.put(indices);
        cubeIndexBuffer.position(0);

        // Buffer de colores (24 vértices × 4 RGBA)
        ByteBuffer cbb = ByteBuffer.allocateDirect(24 * 4 * 4);
        cbb.order(ByteOrder.nativeOrder());
        cubeColorBuffer = cbb.asFloatBuffer();

        Log.d(TAG, "  ✓ Buffers de cubo unitario creados");
    }

    /**
     * Crea los buffers para un cilindro unitario (radio 0.5, altura 1, centrado en origen)
     */
    private void createCylinderBuffers() {
        int segments = CYLINDER_SEGMENTS;

        // Vértices: 2 centros + (segments * 2) para los bordes superior e inferior
        // = 2 + segments*2 vértices para las tapas
        // + segments*2 vértices para el cuerpo (duplicados para normales)
        int numVertices = (segments + 1) * 2 + segments * 2;
        float[] vertices = new float[numVertices * 3];

        int vIndex = 0;

        // Centro inferior (y = -0.5)
        vertices[vIndex++] = 0f;
        vertices[vIndex++] = -0.5f;
        vertices[vIndex++] = 0f;

        // Centro superior (y = 0.5)
        vertices[vIndex++] = 0f;
        vertices[vIndex++] = 0.5f;
        vertices[vIndex++] = 0f;

        // Vértices del borde inferior y superior
        for (int i = 0; i < segments; i++) {
            float angle = (float) (2.0 * Math.PI * i / segments);
            float x = (float) Math.cos(angle) * 0.5f;
            float z = (float) Math.sin(angle) * 0.5f;

            // Borde inferior
            vertices[vIndex++] = x;
            vertices[vIndex++] = -0.5f;
            vertices[vIndex++] = z;

            // Borde superior
            vertices[vIndex++] = x;
            vertices[vIndex++] = 0.5f;
            vertices[vIndex++] = z;
        }

        // Índices para triángulos
        // Tapa inferior: segments triángulos desde centro inferior
        // Tapa superior: segments triángulos desde centro superior
        // Cuerpo: segments * 2 triángulos
        int numIndices = segments * 3 * 2 + segments * 6;
        short[] indices = new short[numIndices];

        int iIndex = 0;

        // Tapa inferior (centro = 0)
        for (int i = 0; i < segments; i++) {
            int current = 2 + i * 2;      // Vértice inferior actual
            int next = 2 + ((i + 1) % segments) * 2;  // Siguiente vértice inferior

            indices[iIndex++] = 0;  // Centro inferior
            indices[iIndex++] = (short) next;
            indices[iIndex++] = (short) current;
        }

        // Tapa superior (centro = 1)
        for (int i = 0; i < segments; i++) {
            int current = 2 + i * 2 + 1;      // Vértice superior actual
            int next = 2 + ((i + 1) % segments) * 2 + 1;  // Siguiente vértice superior

            indices[iIndex++] = 1;  // Centro superior
            indices[iIndex++] = (short) current;
            indices[iIndex++] = (short) next;
        }

        // Cuerpo del cilindro (quads como 2 triángulos)
        for (int i = 0; i < segments; i++) {
            int bottomCurrent = 2 + i * 2;
            int topCurrent = 2 + i * 2 + 1;
            int bottomNext = 2 + ((i + 1) % segments) * 2;
            int topNext = 2 + ((i + 1) % segments) * 2 + 1;

            // Triángulo 1
            indices[iIndex++] = (short) bottomCurrent;
            indices[iIndex++] = (short) bottomNext;
            indices[iIndex++] = (short) topCurrent;

            // Triángulo 2
            indices[iIndex++] = (short) topCurrent;
            indices[iIndex++] = (short) bottomNext;
            indices[iIndex++] = (short) topNext;
        }

        cylinderIndexCount = iIndex;

        // Crear vertex buffer
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        cylinderVertexBuffer = vbb.asFloatBuffer();
        cylinderVertexBuffer.put(vertices);
        cylinderVertexBuffer.position(0);

        // Crear index buffer
        ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 2);
        ibb.order(ByteOrder.nativeOrder());
        cylinderIndexBuffer = ibb.asShortBuffer();
        cylinderIndexBuffer.put(indices);
        cylinderIndexBuffer.position(0);

        // Buffer de colores
        ByteBuffer cbb = ByteBuffer.allocateDirect(numVertices * 4 * 4);
        cbb.order(ByteOrder.nativeOrder());
        cylinderColorBuffer = cbb.asFloatBuffer();

        Log.d(TAG, "  ✓ Buffers de CILINDRO creados (" + segments + " segmentos)");
    }

    /**
     * Crea el shader para renderizar cubos 3D con gradiente y glow
     */
    private void createShader() {
        // ════════════════════════════════════════════════════════════════════════
        // VERTEX SHADER - Pasa posición local para calcular bordes
        // ════════════════════════════════════════════════════════════════════════
        String vertexShaderCode =
            "attribute vec4 a_Position;\n" +
            "attribute vec4 a_Color;\n" +
            "uniform mat4 u_MVPMatrix;\n" +
            "varying vec4 v_Color;\n" +
            "varying vec3 v_LocalPos;\n" +
            "void main() {\n" +
            "    gl_Position = u_MVPMatrix * a_Position;\n" +
            "    v_Color = a_Color;\n" +
            "    v_LocalPos = a_Position.xyz;\n" +
            "}\n";

        // ════════════════════════════════════════════════════════════════════════
        // FRAGMENT SHADER - Efecto neón con bordes difuminados (glow)
        // ════════════════════════════════════════════════════════════════════════
        String fragmentShaderCode =
            "precision mediump float;\n" +
            "varying vec4 v_Color;\n" +
            "varying vec3 v_LocalPos;\n" +
            "\n" +
            "void main() {\n" +
            "    vec4 color = v_Color;\n" +
            "    \n" +
            "    // ═══ CÁLCULO DE DISTANCIA AL BORDE ═══\n" +
            "    // El cubo unitario va de -0.5 a 0.5\n" +
            "    float edgeX = 0.5 - abs(v_LocalPos.x);\n" +
            "    float edgeY = 0.5 - abs(v_LocalPos.y);\n" +
            "    float edgeZ = 0.5 - abs(v_LocalPos.z);\n" +
            "    float edgeDist = min(min(edgeX, edgeY), edgeZ);\n" +
            "    \n" +
            "    // ═══ GLOW EN BORDES (difuminado) ═══\n" +
            "    float glowWidth = 0.15;  // Ancho de la zona de glow\n" +
            "    float glowFactor = 1.0 - smoothstep(0.0, glowWidth, edgeDist);\n" +
            "    \n" +
            "    // El borde es más brillante y tiene glow\n" +
            "    float brightness = 1.0 + glowFactor * 0.8;\n" +
            "    color.rgb *= brightness;\n" +
            "    \n" +
            "    // ═══ EFECTO NEÓN GENERAL ═══\n" +
            "    if (color.a > 0.5) {\n" +
            "        color.rgb *= 1.3;\n" +
            "        // Añadir blanco en los bordes para efecto de luz\n" +
            "        color.rgb = mix(color.rgb, vec3(1.0), glowFactor * 0.4);\n" +
            "    }\n" +
            "    \n" +
            "    // ═══ TRANSPARENCIA EN BORDES (difuminado suave) ═══\n" +
            "    // Los bordes tienen alpha ligeramente reducido para efecto difuso\n" +
            "    float alphaFade = 1.0 - glowFactor * 0.2;\n" +
            "    color.a *= alphaFade;\n" +
            "    \n" +
            "    gl_FragColor = color;\n" +
            "}\n";

        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vertexShader);
        GLES20.glAttachShader(shaderProgram, fragmentShader);
        GLES20.glLinkProgram(shaderProgram);

        aPositionHandle = GLES20.glGetAttribLocation(shaderProgram, "a_Position");
        aColorHandle = GLES20.glGetAttribLocation(shaderProgram, "a_Color");
        uMVPMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "u_MVPMatrix");

        Log.d(TAG, "  ✓ Shader 3D con GLOW creado (program=" + shaderProgram + ")");
    }

    private int compileShader(int type, String code) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "❌ Error compilando shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
        Log.d(TAG, "📷 Cámara asignada al MusicIndicator3D");
    }

    /**
     * Actualiza los niveles de música con distribución de 7 bandas
     */
    public void updateMusicLevels(float bass, float mid, float treble) {
        this.bassLevel = bass;
        this.midLevel = mid;
        this.trebleLevel = treble;

        // ════════════════════════════════════════════════════════════════════════
        // DETECCIÓN DE BEAT
        // ════════════════════════════════════════════════════════════════════════
        float totalEnergy = bass + mid * 0.5f + treble * 0.3f;
        float bassJump = bass - lastBassLevel;

        if (bassJump > BEAT_THRESHOLD || (totalEnergy - energyHistory) > BEAT_THRESHOLD * 1.5f) {
            currentBeatBoost = BEAT_BOOST;
        }

        lastBassLevel = bass;
        energyHistory = energyHistory * 0.9f + totalEnergy * 0.1f;
        musicPlaying = totalEnergy > 0.15f;

        // ════════════════════════════════════════════════════════════════════════
        // DISTRIBUCIÓN A 7 BARRAS
        // ════════════════════════════════════════════════════════════════════════
        barLevels[0] = bass * 1.2f;                              // SUB-BASS
        barLevels[1] = bass * 0.9f + mid * 0.1f;                 // BASS
        barLevels[2] = bass * 0.4f + mid * 0.6f;                 // MID-LOW
        barLevels[3] = bass * 0.1f + mid * 0.9f;                 // MID
        barLevels[4] = mid * 0.6f + treble * 0.4f;               // MID-HIGH
        barLevels[5] = mid * 0.2f + treble * 0.8f;               // TREBLE
        barLevels[6] = treble * 1.0f;                            // AIR

        // Aplicar sensibilidad y beat boost (usa sensibilidad individual de SceneConstants)
        int minLevel = musicPlaying ? MIN_LEVEL_WITH_MUSIC : MIN_LEVEL_NO_MUSIC;
        float minLevelF = (float) minLevel / MAX_SEGMENTS;

        for (int i = 0; i < NUM_BARRAS; i++) {
            barLevels[i] = barLevels[i] * barSensitivity[i];  // Usa sensibilidad de SceneConstants
            float boostMultiplier = 1.0f + (i * 0.15f);
            barLevels[i] += currentBeatBoost * boostMultiplier;
            barLevels[i] = Math.max(barLevels[i], minLevelF);
            barLevels[i] = Math.min(1.0f, barLevels[i]);
        }
    }

    @Override
    public void update(float deltaTime) {
        frameCount++;

        // ════════════════════════════════════════════════════════════════════════
        // 🧪 MODO PRUEBA - Rotar automáticamente y mostrar en log
        // ════════════════════════════════════════════════════════════════════════
        if (TEST_ROTATION_MODE) {
            rotationY += testRotationSpeed * deltaTime;
            if (rotationY >= 360f) rotationY -= 360f;
            if (rotationY < 0f) rotationY += 360f;

            lastLogTime += deltaTime;
            if (lastLogTime >= logInterval) {
                lastLogTime = 0f;
                Log.d(TAG, "🧪 ROTACIÓN Y = " + String.format("%.1f", rotationY) + "° (cuando veas la cara frontal, anota este valor)");
            }
        }

        // Decaer beat boost
        if (currentBeatBoost > 0) {
            currentBeatBoost -= BEAT_DECAY * deltaTime;
            if (currentBeatBoost < 0) currentBeatBoost = 0;
        }

        // Suavizar niveles
        int minLevel = musicPlaying ? MIN_LEVEL_WITH_MUSIC : MIN_LEVEL_NO_MUSIC;
        float minLevelF = (float) minLevel / MAX_SEGMENTS;

        for (int i = 0; i < NUM_BARRAS; i++) {
            smoothedLevels[i] = smoothedLevels[i] * 0.3f + barLevels[i] * 0.7f;
            smoothedLevels[i] = Math.max(smoothedLevels[i], minLevelF);
        }

        // ════════════════════════════════════════════════════════════════════════
        // ACTUALIZAR PEAK HOLDERS
        // ════════════════════════════════════════════════════════════════════════
        for (int i = 0; i < NUM_BARRAS; i++) {
            float currentLevel = smoothedLevels[i];

            if (currentLevel >= peakLevels[i]) {
                peakLevels[i] = currentLevel;
                peakHoldTimers[i] = PEAK_HOLD_TIME;
            } else {
                if (peakHoldTimers[i] > 0) {
                    peakHoldTimers[i] -= deltaTime;
                } else {
                    peakLevels[i] -= PEAK_FALL_SPEED * deltaTime;
                    peakLevels[i] = Math.max(peakLevels[i], currentLevel);
                }
            }
        }

        // ════════════════════════════════════════════════════════════════════════
        // SISTEMA DE CHISPAS 3D
        // ════════════════════════════════════════════════════════════════════════
        for (int i = 0; i < NUM_BARRAS; i++) {
            if (barCooldowns[i] > 0) {
                barCooldowns[i] -= deltaTime;
            }
        }

        float totalMusicEnergy = bassLevel + midLevel + trebleLevel;
        boolean hasMusicPlaying = totalMusicEnergy > 0.15f;

        for (int barIndex = 0; barIndex < NUM_BARRAS; barIndex++) {
            float level = smoothedLevels[barIndex];
            float sparkMinLevel = (float) MIN_LEVEL_WITH_MUSIC / MAX_SEGMENTS;

            boolean realActivity = level > (sparkMinLevel + 0.1f) && hasMusicPlaying;

            if (realActivity && level >= SPARK_THRESHOLD && barCooldowns[barIndex] <= 0) {
                int intensityBonus = (int)((level - SPARK_THRESHOLD) * 5);
                int numSparks = MAX_SPARKS_PER_TRIGGER + intensityBonus;
                numSparks = Math.min(numSparks, 5);

                for (int s = 0; s < numSparks; s++) {
                    Spark3D spark = new Spark3D();

                    // ═══ USA POSICIONES INDIVIDUALES DE CADA BARRA ═══
                    float barBaseY = barYPositions[barIndex];  // Posición Y individual de SceneConstants
                    float barHeight = level * barMaxHeights[barIndex];

                    spark.x = barXPositions[barIndex] + (random.nextFloat() - 0.5f) * barWidths[barIndex] * 0.5f;
                    spark.y = barBaseY + barHeight;  // Usa la Y individual de la barra
                    spark.z = barZPositions[barIndex] + (random.nextFloat() - 0.5f) * barDepths[barIndex];

                    spark.vx = (random.nextFloat() - 0.5f) * 0.3f;
                    spark.vy = SPARK_SPEED + random.nextFloat() * 0.3f;
                    spark.vz = (random.nextFloat() - 0.5f) * 0.3f;

                    // Peak también usa la Y individual
                    spark.peakY = barBaseY + peakLevels[barIndex] * barMaxHeights[barIndex] + PEAK_OFFSET;
                    spark.passedPeak = false;
                    spark.exploded = false;

                    spark.color = getBarColor(level, true);
                    spark.age = 0f;
                    spark.lifetime = SPARK_LIFETIME;

                    sparks.add(spark);
                }
                barCooldowns[barIndex] = SPARK_COOLDOWN;
            }
        }

        // Actualizar chispas existentes
        Iterator<Spark3D> sparkIter = sparks.iterator();
        while (sparkIter.hasNext()) {
            Spark3D spark = sparkIter.next();
            spark.age += deltaTime;

            spark.x += spark.vx * deltaTime;
            spark.y += spark.vy * deltaTime;
            spark.z += spark.vz * deltaTime;

            // Gravedad suave
            spark.vy -= 0.5f * deltaTime;

            if (!spark.passedPeak && spark.y >= spark.peakY) {
                spark.passedPeak = true;

                if (!spark.exploded) {
                    spark.exploded = true;
                    int numExplosionParts = 3 + random.nextInt(3);
                    for (int e = 0; e < numExplosionParts; e++) {
                        MiniExplosion3D exp = new MiniExplosion3D();
                        exp.x = spark.x + (random.nextFloat() - 0.5f) * 0.1f;
                        exp.y = spark.y + (random.nextFloat() - 0.5f) * 0.1f;
                        exp.z = spark.z + (random.nextFloat() - 0.5f) * 0.1f;
                        exp.age = 0f;
                        exp.color = new float[]{spark.color[0], spark.color[1], spark.color[2], 1.0f};
                        exp.size = 0.03f + random.nextFloat() * 0.02f;
                        explosions.add(exp);
                    }
                }
            }

            float fadeProgress = spark.age / spark.lifetime;
            if (spark.passedPeak) {
                spark.color[3] = Math.max(0, spark.color[3] - deltaTime * 3.0f);
            } else {
                spark.color[3] = (1.0f - fadeProgress) * 0.9f;
            }

            if (spark.age >= spark.lifetime || spark.color[3] <= 0.01f) {
                sparkIter.remove();
            }
        }

        // Actualizar explosiones
        Iterator<MiniExplosion3D> expIter = explosions.iterator();
        while (expIter.hasNext()) {
            MiniExplosion3D exp = expIter.next();
            exp.age += deltaTime;
            exp.size += deltaTime * 0.1f;
            exp.color[3] = 1.0f - (exp.age / 0.3f);

            if (exp.age >= 0.3f) {
                expIter.remove();
            }
        }
    }

    @Override
    public void draw() {
        if (camera == null || !GLES20.glIsProgram(shaderProgram)) return;

        GLES20.glUseProgram(shaderProgram);

        // Habilitar depth test y blending
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);  // Blending aditivo

        // ════════════════════════════════════════════════════════════════════════
        // 🪞 DIBUJAR REFLEJO (primero, debajo de las barras)
        // ════════════════════════════════════════════════════════════════════════
        if (drawReflection) {
            drawAllBarsWithReflection(true);
        }

        // ════════════════════════════════════════════════════════════════════════
        // DIBUJAR BARRAS PRINCIPALES
        // ════════════════════════════════════════════════════════════════════════
        drawAllBarsWithReflection(false);

        // ════════════════════════════════════════════════════════════════════════
        // DIBUJAR CHISPAS 3D
        // ════════════════════════════════════════════════════════════════════════
        for (Spark3D spark : sparks) {
            drawSpark3D(spark);
        }

        // ════════════════════════════════════════════════════════════════════════
        // DIBUJAR EXPLOSIONES 3D
        // ════════════════════════════════════════════════════════════════════════
        for (MiniExplosion3D exp : explosions) {
            drawExplosion3D(exp);
        }

        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * Dibuja todas las barras (con opción de reflejo)
     * Usa posiciones Y individuales de cada barra
     */
    private void drawAllBarsWithReflection(boolean isReflection) {
        float alphaMultiplier = isReflection ? reflectionAlpha : 1.0f;
        float yFlip = isReflection ? -1.0f : 1.0f;
        float yOffset = isReflection ? -0.05f : 0f;  // Pequeño offset para el reflejo

        for (int barIndex = 0; barIndex < NUM_BARRAS; barIndex++) {
            float level = Math.min(1.0f, smoothedLevels[barIndex]);
            float barHeight = level * barMaxHeights[barIndex];

            if (barHeight > 0.01f) {
                drawBar3DEx(barIndex, barHeight, level, yFlip, yOffset, alphaMultiplier);
            }

            // Peak holder (solo en barras principales, no en reflejo)
            if (!isReflection) {
                float peakLevel = peakLevels[barIndex];
                // La posición Y del peak ahora se calcula en drawPeak3D usando barYPositions
                float peakHeight = barMaxHeights[barIndex] * 0.08f;
                drawPeak3D(barIndex, 0, peakHeight, peakLevel);  // peakY ya no se usa, se calcula internamente
            }
        }
    }

    /**
     * Dibuja una barra 3D con gradiente vertical (versión simple)
     */
    private void drawBar3D(int barIndex, float barHeight, float level) {
        drawBar3DEx(barIndex, barHeight, level, 1.0f, 0f, 1.0f);
    }

    /**
     * Dibuja una barra 3D extendida con soporte para reflejo y perspectiva
     * Usa rotaciones INDIVIDUALES de cada barra desde SceneConstants
     * @param yFlip -1 para reflejo, 1 para normal
     * @param yOffset Offset vertical adicional
     * @param alphaMultiplier Multiplicador de alpha (para transparencia del reflejo)
     */
    private void drawBar3DEx(int barIndex, float barHeight, float level,
                              float yFlip, float yOffset, float alphaMultiplier) {
        float bx = barXPositions[barIndex];
        float by = barYPositions[barIndex];   // Posición Y individual de SceneConstants
        float bz = barZPositions[barIndex];   // Posición Z individual
        float bw = barWidths[barIndex];
        float bd = barDepths[barIndex];

        // Configurar colores del cubo con gradiente vertical
        setBarGradientColorsEx(level, barHeight, barMaxHeights[barIndex], alphaMultiplier, yFlip < 0);

        // Calcular posición Y base (usa la Y individual + offset)
        float baseY = by + yOffset;

        // Construir matriz modelo - Cada barra en su posición individual
        Matrix.setIdentityM(modelMatrix, 0);

        // Trasladar a la posición de la barra (X, Y, Z individuales)
        Matrix.translateM(modelMatrix, 0, bx, baseY + (barHeight / 2f) * yFlip, bz);

        // ═══ ROTACIONES INDIVIDUALES POR BARRA ═══
        // Primero rotación Y (giro horizontal)
        float totalRotY = rotationY + barRotY[barIndex];
        if (totalRotY != 0) {
            Matrix.rotateM(modelMatrix, 0, totalRotY, 0, 1, 0);
        }
        // Luego rotación X (inclinación)
        float totalRotX = (rotationX + barRotX[barIndex]) * yFlip;
        if (totalRotX != 0) {
            Matrix.rotateM(modelMatrix, 0, totalRotX, 1, 0, 0);
        }
        // Finalmente rotación Z (ladeo)
        if (barRotZ[barIndex] != 0) {
            Matrix.rotateM(modelMatrix, 0, barRotZ[barIndex], 0, 0, 1);
        }

        // Escalar: rectángulo/cubo
        Matrix.scaleM(modelMatrix, 0, bw, barHeight, bd);

        // Calcular MVP
        camera.computeMvp(modelMatrix, mvpMatrix);

        // Pasar uniforms
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);

        // Configurar colores del CUBO (rectángulo)
        setBarGradientColorsEx(level, barHeight, barMaxHeights[barIndex], alphaMultiplier, yFlip < 0);

        // Configurar atributos - USAR CUBO/RECTÁNGULO
        cubeVertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 0, cubeVertexBuffer);

        cubeColorBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aColorHandle);
        GLES20.glVertexAttribPointer(aColorHandle, 4, GLES20.GL_FLOAT, false, 0, cubeColorBuffer);

        // Dibujar CUBO/RECTÁNGULO
        cubeIndexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, cubeIndexCount, GLES20.GL_UNSIGNED_SHORT, cubeIndexBuffer);

        GLES20.glDisableVertexAttribArray(aPositionHandle);
        GLES20.glDisableVertexAttribArray(aColorHandle);
    }

    /**
     * Configura los colores del CILINDRO con gradiente vertical arcoíris
     */
    private void setCylinderGradientColors(float level, float alphaMultiplier, boolean isReflection) {
        int segments = CYLINDER_SEGMENTS;
        int numVertices = (segments + 1) * 2 + segments * 2;

        // Colores: abajo = rosa/magenta, arriba = según nivel
        float[] bottomColor = {1.0f, 0.0f, 0.8f, 1.0f * alphaMultiplier};
        float[] topColor = getBarColor(level, true);
        topColor[3] *= alphaMultiplier;

        if (isReflection) {
            float[] temp = bottomColor;
            bottomColor = topColor;
            topColor = temp;
            topColor[3] *= 0.3f;
        }

        float[] colors = new float[numVertices * 4];
        int cIndex = 0;

        // Centro inferior (vértice 0)
        colors[cIndex++] = bottomColor[0];
        colors[cIndex++] = bottomColor[1];
        colors[cIndex++] = bottomColor[2];
        colors[cIndex++] = bottomColor[3];

        // Centro superior (vértice 1)
        colors[cIndex++] = topColor[0];
        colors[cIndex++] = topColor[1];
        colors[cIndex++] = topColor[2];
        colors[cIndex++] = topColor[3];

        // Vértices del borde (alternando inferior/superior)
        for (int i = 0; i < segments; i++) {
            // Borde inferior
            colors[cIndex++] = bottomColor[0];
            colors[cIndex++] = bottomColor[1];
            colors[cIndex++] = bottomColor[2];
            colors[cIndex++] = bottomColor[3];

            // Borde superior
            colors[cIndex++] = topColor[0];
            colors[cIndex++] = topColor[1];
            colors[cIndex++] = topColor[2];
            colors[cIndex++] = topColor[3];
        }

        cylinderColorBuffer.clear();
        cylinderColorBuffer.put(colors);
        cylinderColorBuffer.position(0);
    }

    /**
     * Configura los colores del cubo con gradiente vertical arcoíris (legacy)
     */
    private void setBarGradientColors(float level, float barHeight, float maxHeight) {
        // Colores: abajo = rosa/magenta, arriba = según nivel (rosa→rojo→naranja→verde→cyan)
        float[] bottomColor = {1.0f, 0.0f, 0.8f, 1.0f};  // Magenta

        float topPosition = barHeight / maxHeight;
        float[] topColor = getBarColor(topPosition, true);

        // Asignar colores a cada vértice del cubo
        // El cubo tiene 24 vértices (4 por cara)
        float[] colors = new float[24 * 4];

        for (int i = 0; i < 24; i++) {
            int vertexIndex = i % 4;
            int faceIndex = i / 4;

            float[] color;
            // Caras superior e inferior tienen color uniforme
            if (faceIndex == 2) {  // Top face
                color = topColor;
            } else if (faceIndex == 3) {  // Bottom face
                color = bottomColor;
            } else {
                // Otras caras: gradiente vertical
                // Vértices 0,1 son abajo, 2,3 son arriba (dentro de cada cara)
                if (vertexIndex == 2 || vertexIndex == 3) {
                    color = topColor;
                } else {
                    color = bottomColor;
                }
            }

            colors[i * 4 + 0] = color[0];
            colors[i * 4 + 1] = color[1];
            colors[i * 4 + 2] = color[2];
            colors[i * 4 + 3] = color[3];
        }

        cubeColorBuffer.clear();
        cubeColorBuffer.put(colors);
        cubeColorBuffer.position(0);
    }

    /**
     * Configura colores con soporte para alpha y reflejo invertido
     */
    private void setBarGradientColorsEx(float level, float barHeight, float maxHeight,
                                         float alphaMultiplier, boolean isReflection) {
        // Colores: abajo = rosa/magenta, arriba = según nivel
        float[] bottomColor = {1.0f, 0.0f, 0.8f, 1.0f * alphaMultiplier};

        float topPosition = barHeight / maxHeight;
        float[] topColor = getBarColor(topPosition, true);
        topColor[3] *= alphaMultiplier;

        // Para el reflejo, invertir el gradiente (arriba es abajo)
        if (isReflection) {
            float[] temp = bottomColor;
            bottomColor = topColor;
            topColor = temp;
            // También reducir más el alpha del reflejo en la parte "lejana"
            topColor[3] *= 0.3f;
        }

        float[] colors = new float[24 * 4];

        for (int i = 0; i < 24; i++) {
            int vertexIndex = i % 4;
            int faceIndex = i / 4;

            float[] color;
            if (faceIndex == 2) {  // Top face
                color = topColor;
            } else if (faceIndex == 3) {  // Bottom face
                color = bottomColor;
            } else {
                if (vertexIndex == 2 || vertexIndex == 3) {
                    color = topColor;
                } else {
                    color = bottomColor;
                }
            }

            colors[i * 4 + 0] = color[0];
            colors[i * 4 + 1] = color[1];
            colors[i * 4 + 2] = color[2];
            colors[i * 4 + 3] = color[3];
        }

        cubeColorBuffer.clear();
        cubeColorBuffer.put(colors);
        cubeColorBuffer.position(0);
    }

    /**
     * 🌈 Calcula el color arcoíris basado en el nivel (0-1)
     */
    private float[] getBarColor(float t, boolean bright) {
        float r, g, b, a;

        if (bright) {
            if (t < 0.2f) {
                float p = t / 0.2f;
                r = 1.0f; g = 0.0f; b = 1.0f - p;  // Magenta → Rojo
            } else if (t < 0.4f) {
                float p = (t - 0.2f) / 0.2f;
                r = 1.0f; g = p * 0.7f; b = 0.0f;  // Rojo → Naranja
            } else if (t < 0.6f) {
                float p = (t - 0.4f) / 0.2f;
                r = 1.0f - p * 0.5f; g = 0.7f + p * 0.3f; b = 0.0f;  // Naranja → Verde
            } else if (t < 0.8f) {
                float p = (t - 0.6f) / 0.2f;
                r = 0.5f - p * 0.5f; g = 1.0f; b = p * 0.8f;  // Verde → Cyan
            } else {
                float p = (t - 0.8f) / 0.2f;
                r = 0.0f; g = 1.0f - p * 0.2f; b = 0.8f + p * 0.2f;  // Cyan → Azul
            }
            a = 1.0f;
        } else {
            // Versión tenue (para LEDs apagados)
            if (t < 0.2f) {
                r = 0.15f; g = 0.0f; b = 0.1f;
            } else if (t < 0.4f) {
                r = 0.15f; g = 0.05f; b = 0.0f;
            } else if (t < 0.6f) {
                r = 0.1f; g = 0.12f; b = 0.0f;
            } else if (t < 0.8f) {
                r = 0.0f; g = 0.12f; b = 0.05f;
            } else {
                r = 0.0f; g = 0.1f; b = 0.15f;
            }
            a = 0.25f;
        }

        return new float[]{r, g, b, a};
    }

    /**
     * Dibuja el peak holder 3D (rectángulo delgado)
     * Usa posición Y individual y rotaciones de SceneConstants
     */
    private void drawPeak3D(int barIndex, float peakY, float peakHeight, float peakLevel) {
        float bx = barXPositions[barIndex];
        float by = barYPositions[barIndex];  // Posición Y individual
        float bz = barZPositions[barIndex];
        float bw = barWidths[barIndex];
        float bd = barDepths[barIndex];

        // Calcular posición Y del peak relativa a la barra
        float actualPeakY = by + peakLevel * barMaxHeights[barIndex] + PEAK_OFFSET;

        // Color del peak basado en altura (arcoíris)
        float[] peakColor = getBarColor(peakLevel, true);
        // Hacerlo más brillante
        peakColor[0] = Math.min(1.0f, peakColor[0] + 0.2f);
        peakColor[1] = Math.min(1.0f, peakColor[1] + 0.2f);
        peakColor[2] = Math.min(1.0f, peakColor[2] + 0.2f);
        peakColor[3] = 0.95f;

        // Color uniforme para el peak (cubo)
        float[] colors = new float[24 * 4];
        for (int i = 0; i < 24; i++) {
            colors[i * 4 + 0] = peakColor[0];
            colors[i * 4 + 1] = peakColor[1];
            colors[i * 4 + 2] = peakColor[2];
            colors[i * 4 + 3] = peakColor[3];
        }
        cubeColorBuffer.clear();
        cubeColorBuffer.put(colors);
        cubeColorBuffer.position(0);

        // Construir matriz modelo
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, bx, actualPeakY, bz);

        // Aplicar rotaciones individuales igual que la barra
        float totalRotY = rotationY + barRotY[barIndex];
        if (totalRotY != 0) {
            Matrix.rotateM(modelMatrix, 0, totalRotY, 0, 1, 0);
        }
        if (barRotX[barIndex] != 0) {
            Matrix.rotateM(modelMatrix, 0, barRotX[barIndex], 1, 0, 0);
        }
        if (barRotZ[barIndex] != 0) {
            Matrix.rotateM(modelMatrix, 0, barRotZ[barIndex], 0, 0, 1);
        }

        Matrix.scaleM(modelMatrix, 0, bw * 1.1f, peakHeight, bd * 0.5f);

        camera.computeMvp(modelMatrix, mvpMatrix);
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);

        // Usar CUBO para el peak
        cubeVertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 0, cubeVertexBuffer);

        cubeColorBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aColorHandle);
        GLES20.glVertexAttribPointer(aColorHandle, 4, GLES20.GL_FLOAT, false, 0, cubeColorBuffer);

        cubeIndexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, cubeIndexCount, GLES20.GL_UNSIGNED_SHORT, cubeIndexBuffer);

        GLES20.glDisableVertexAttribArray(aPositionHandle);
        GLES20.glDisableVertexAttribArray(aColorHandle);
    }

    /**
     * Configura color uniforme para el peak (cilindro)
     */
    private void setPeakCylinderColor(float[] color) {
        int segments = CYLINDER_SEGMENTS;
        int numVertices = (segments + 1) * 2 + segments * 2;

        float[] colors = new float[numVertices * 4];
        for (int i = 0; i < numVertices; i++) {
            colors[i * 4 + 0] = color[0];
            colors[i * 4 + 1] = color[1];
            colors[i * 4 + 2] = color[2];
            colors[i * 4 + 3] = color[3];
        }

        cylinderColorBuffer.clear();
        cylinderColorBuffer.put(colors);
        cylinderColorBuffer.position(0);
    }

    /**
     * Dibuja una chispa 3D (cubo pequeño)
     */
    private void drawSpark3D(Spark3D spark) {
        // Color de la chispa
        float[] colors = new float[24 * 4];
        for (int i = 0; i < 24; i++) {
            colors[i * 4 + 0] = spark.color[0];
            colors[i * 4 + 1] = spark.color[1];
            colors[i * 4 + 2] = spark.color[2];
            colors[i * 4 + 3] = spark.color[3];
        }
        cubeColorBuffer.clear();
        cubeColorBuffer.put(colors);
        cubeColorBuffer.position(0);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, spark.x, spark.y, spark.z);
        Matrix.scaleM(modelMatrix, 0, SPARK_SIZE, SPARK_SIZE, SPARK_SIZE);

        camera.computeMvp(modelMatrix, mvpMatrix);
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);

        cubeVertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 0, cubeVertexBuffer);

        cubeColorBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aColorHandle);
        GLES20.glVertexAttribPointer(aColorHandle, 4, GLES20.GL_FLOAT, false, 0, cubeColorBuffer);

        cubeIndexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, cubeIndexCount, GLES20.GL_UNSIGNED_SHORT, cubeIndexBuffer);

        GLES20.glDisableVertexAttribArray(aPositionHandle);
        GLES20.glDisableVertexAttribArray(aColorHandle);
    }

    /**
     * Dibuja una mini-explosión 3D
     */
    private void drawExplosion3D(MiniExplosion3D exp) {
        float r = Math.min(1.0f, exp.color[0] + 0.3f);
        float g = Math.min(1.0f, exp.color[1] + 0.3f);
        float b = Math.min(1.0f, exp.color[2] + 0.3f);
        float a = exp.color[3];

        float[] colors = new float[24 * 4];
        for (int i = 0; i < 24; i++) {
            colors[i * 4 + 0] = r;
            colors[i * 4 + 1] = g;
            colors[i * 4 + 2] = b;
            colors[i * 4 + 3] = a;
        }
        cubeColorBuffer.clear();
        cubeColorBuffer.put(colors);
        cubeColorBuffer.position(0);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, exp.x, exp.y, exp.z);
        Matrix.scaleM(modelMatrix, 0, exp.size, exp.size, exp.size);

        camera.computeMvp(modelMatrix, mvpMatrix);
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);

        cubeVertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 0, cubeVertexBuffer);

        cubeColorBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aColorHandle);
        GLES20.glVertexAttribPointer(aColorHandle, 4, GLES20.GL_FLOAT, false, 0, cubeColorBuffer);

        cubeIndexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, cubeIndexCount, GLES20.GL_UNSIGNED_SHORT, cubeIndexBuffer);

        GLES20.glDisableVertexAttribArray(aPositionHandle);
        GLES20.glDisableVertexAttribArray(aColorHandle);
    }

    // ════════════════════════════════════════════════════════════════════════
    // SETTERS PARA CONFIGURACIÓN
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Establece la posición del ecualizador en el mundo 3D
     */
    public void setPosition(float x, float y, float z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        calculateBarGeometry();
    }

    /**
     * Establece las dimensiones del ecualizador
     */
    public void setDimensions(float width, float height, float depth) {
        this.totalWidth = width;
        this.maxHeight = height;
        this.barDepth = depth;
        calculateBarGeometry();
    }
}
