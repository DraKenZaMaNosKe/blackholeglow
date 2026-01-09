package com.secret.blackholeglow;

import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.systems.AspectRatioManager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   🎵 EqualizerBarsDJ v3.0 - Ecualizador Temático Premium                ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  CARACTERÍSTICAS:                                                        ║
 * ║  • 32 barras delgadas en la parte inferior                              ║
 * ║  • Diseño simétrico: centro más alto, lados más bajos                   ║
 * ║  • 🎨 TEMAS: ABYSSIA (océano) / PYRALIS (fuego)                         ║
 * ║  • Efecto GLOW neón con resplandor                                      ║
 * ║  • Peak markers que caen suavemente                                     ║
 * ║  • Integración con AspectRatioManager                                   ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class EqualizerBarsDJ implements SceneObject, AspectRatioManager.AspectRatioAware {
    private static final String TAG = "EqualizerBarsDJ";

    // ════════════════════════════════════════════════════════════════════════
    // 🎨 SISTEMA DE TEMAS
    // ════════════════════════════════════════════════════════════════════════
    public enum Theme {
        DEFAULT,   // Rosa → Cyan (original)
        ABYSSIA,   // Púrpura bioluminiscente → Turquesa (océano profundo)
        PYRALIS,   // Rojo → Naranja → Amarillo (fuego)
        KAMEHAMEHA, // Azul → Cyan → Blanco (energía Ki)
        SYNTHWAVE, // Hot Pink → Cyan → Magenta (retrowave 80s)
        COSMOS     // Azul → Dorado → Púrpura (Saint Seiya cosmos)
    }

    private Theme currentTheme = Theme.DEFAULT;

    // ════════════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN DE BARRAS
    // ════════════════════════════════════════════════════════════════════════
    private static final int NUM_BARS = 32;
    private static final float BAR_SPACING = 0.0f;  // Sin separación entre barras
    private static final float MAX_HEIGHT = 0.38f;
    private static final float MIN_HEIGHT = 0.025f;
    private static final float BASE_Y = -0.95f;

    // ════════════════════════════════════════════════════════════════════════
    // 🎨 COLORES POR TEMA - Arrays dinámicos
    // ════════════════════════════════════════════════════════════════════════

    // === DEFAULT (Rosa → Cyan) ===
    private static final float[] DEFAULT_BASS = {1.0f, 0.2f, 0.6f};      // Rosa neón
    private static final float[] DEFAULT_TREBLE = {0.2f, 0.9f, 1.0f};    // Cyan neón
    private static final float[] DEFAULT_PEAK = {1.0f, 1.0f, 1.0f};      // Blanco
    private static final float[] DEFAULT_WAVE_STRONG = {0.3f, 1.0f, 1.0f};   // Cyan
    private static final float[] DEFAULT_WAVE_NORMAL = {1.0f, 0.3f, 0.9f};   // Rosa
    private static final float[] DEFAULT_LIGHTNING_1 = {0.3f, 0.9f, 1.0f};   // Cyan eléctrico
    private static final float[] DEFAULT_LIGHTNING_2 = {1.0f, 0.4f, 0.9f};   // Rosa eléctrico

    // === ABYSSIA (Océano Profundo - Púrpura → Turquesa) ===
    private static final float[] ABYSSIA_BASS = {0.6f, 0.2f, 1.0f};      // Púrpura bioluminiscente
    private static final float[] ABYSSIA_TREBLE = {0.25f, 0.88f, 0.82f}; // Turquesa
    private static final float[] ABYSSIA_PEAK = {0.5f, 1.0f, 1.0f};      // Cyan brillante (no blanco)
    private static final float[] ABYSSIA_WAVE_STRONG = {0.0f, 0.9f, 0.9f};   // Cyan profundo
    private static final float[] ABYSSIA_WAVE_NORMAL = {0.5f, 0.3f, 1.0f};   // Púrpura
    private static final float[] ABYSSIA_LIGHTNING_1 = {0.3f, 0.8f, 1.0f};   // Tentáculo cyan
    private static final float[] ABYSSIA_LIGHTNING_2 = {0.6f, 0.3f, 1.0f};   // Tentáculo púrpura

    // === PYRALIS (Fuego - Rojo → Amarillo) ===
    private static final float[] PYRALIS_BASS = {1.0f, 0.15f, 0.0f};     // Rojo intenso
    private static final float[] PYRALIS_TREBLE = {1.0f, 0.85f, 0.0f};   // Amarillo/Dorado
    private static final float[] PYRALIS_PEAK = {1.0f, 1.0f, 0.6f};      // Amarillo claro
    private static final float[] PYRALIS_WAVE_STRONG = {1.0f, 0.6f, 0.0f};   // Naranja brillante
    private static final float[] PYRALIS_WAVE_NORMAL = {1.0f, 0.2f, 0.0f};   // Rojo fuego
    private static final float[] PYRALIS_LIGHTNING_1 = {1.0f, 0.5f, 0.0f};   // Arco naranja
    private static final float[] PYRALIS_LIGHTNING_2 = {1.0f, 0.9f, 0.3f};   // Arco dorado

    // === KAMEHAMEHA (Energía Ki - Azul → Cyan → Blanco) ===
    private static final float[] KAMEHAMEHA_BASS = {0.0f, 0.4f, 1.0f};       // Azul profundo Ki
    private static final float[] KAMEHAMEHA_TREBLE = {0.4f, 0.9f, 1.0f};     // Cyan energía
    private static final float[] KAMEHAMEHA_PEAK = {0.85f, 0.95f, 1.0f};     // Blanco-azulado brillante
    private static final float[] KAMEHAMEHA_WAVE_STRONG = {0.0f, 0.85f, 1.0f};   // Cyan intenso
    private static final float[] KAMEHAMEHA_WAVE_NORMAL = {0.2f, 0.5f, 1.0f};    // Azul medio
    private static final float[] KAMEHAMEHA_LIGHTNING_1 = {0.5f, 0.9f, 1.0f};    // Rayo cyan
    private static final float[] KAMEHAMEHA_LIGHTNING_2 = {0.7f, 0.85f, 1.0f};   // Rayo blanco-azul

    // === SYNTHWAVE (Retrowave 80s - Hot Pink → Cyan → Magenta) ===
    private static final float[] SYNTHWAVE_BASS = {1.0f, 0.08f, 0.58f};      // Hot Pink (#FF1493)
    private static final float[] SYNTHWAVE_TREBLE = {0.0f, 1.0f, 1.0f};      // Cyan puro (#00FFFF)
    private static final float[] SYNTHWAVE_PEAK = {1.0f, 0.0f, 1.0f};        // Magenta (#FF00FF)
    private static final float[] SYNTHWAVE_WAVE_STRONG = {0.0f, 1.0f, 1.0f}; // Cyan neón
    private static final float[] SYNTHWAVE_WAVE_NORMAL = {1.0f, 0.2f, 0.8f}; // Pink/Magenta
    private static final float[] SYNTHWAVE_LIGHTNING_1 = {0.4f, 0.0f, 1.0f}; // Electric Purple
    private static final float[] SYNTHWAVE_LIGHTNING_2 = {1.0f, 0.0f, 0.6f}; // Hot Pink eléctrico

    // === COSMOS (Saint Seiya - Azul → Dorado → Púrpura) ===
    private static final float[] COSMOS_BASS = {0.2f, 0.3f, 1.0f};           // Azul cosmos profundo
    private static final float[] COSMOS_TREBLE = {1.0f, 0.84f, 0.0f};        // Dorado celestial (#FFD700)
    private static final float[] COSMOS_PEAK = {1.0f, 1.0f, 0.8f};           // Blanco dorado brillante
    private static final float[] COSMOS_WAVE_STRONG = {0.8f, 0.6f, 1.0f};    // Púrpura cósmico
    private static final float[] COSMOS_WAVE_NORMAL = {0.3f, 0.5f, 1.0f};    // Azul constelación
    private static final float[] COSMOS_LIGHTNING_1 = {1.0f, 0.9f, 0.3f};    // Rayo dorado
    private static final float[] COSMOS_LIGHTNING_2 = {0.6f, 0.4f, 1.0f};    // Rayo púrpura

    // Colores activos (se actualizan con setTheme)
    private float[] colorBass = DEFAULT_BASS;
    private float[] colorTreble = DEFAULT_TREBLE;
    private float[] colorPeak = DEFAULT_PEAK;
    private float[] colorWaveStrong = DEFAULT_WAVE_STRONG;
    private float[] colorWaveNormal = DEFAULT_WAVE_NORMAL;
    private float[] colorLightning1 = DEFAULT_LIGHTNING_1;
    private float[] colorLightning2 = DEFAULT_LIGHTNING_2;

    // ════════════════════════════════════════════════════════════════════════
    // GLOW - Configuración del resplandor
    // ════════════════════════════════════════════════════════════════════════
    private static final float GLOW_INTENSITY = 0.4f;
    private static final float GLOW_WIDTH_MULT = 2.5f;  // Multiplicador de ancho para glow

    // ════════════════════════════════════════════════════════════════════════
    // PEAK MARKERS - Indicadores de máximo
    // ════════════════════════════════════════════════════════════════════════
    private float[] peakLevels;           // Nivel del peak actual
    private float[] peakHoldTime;         // Tiempo que el peak se mantiene arriba
    private static final float PEAK_HOLD_TIME = 0.5f;    // Segundos que el peak se mantiene
    private static final float PEAK_FALL_SPEED = 0.8f;   // Velocidad de caída del peak
    private static final float PEAK_HEIGHT = 0.015f;     // Altura del marcador de peak

    // ════════════════════════════════════════════════════════════════════════
    // CURVA SIMÉTRICA
    // ════════════════════════════════════════════════════════════════════════
    private float[] heightMultipliers;

    // ════════════════════════════════════════════════════════════════════════
    // AUDIO
    // ════════════════════════════════════════════════════════════════════════
    private float[] barLevels;
    private float[] smoothedLevels;
    private float[] targetLevels;

    private float bassLevel = 0f;
    private float midLevel = 0f;
    private float trebleLevel = 0f;

    private static final float SMOOTHING_UP = 0.40f;   // Subida rápida
    private static final float SMOOTHING_DOWN = 0.15f;  // Bajada más rápida (era 0.10)

    // ════════════════════════════════════════════════════════════════════════
    // 💥 BEAT DETECTION - Detección de ritmo con historial de energía
    // ════════════════════════════════════════════════════════════════════════
    private static final int ENERGY_HISTORY_SIZE = 43;  // ~1 segundo de historia a 43fps
    private float[] energyHistory = new float[ENERGY_HISTORY_SIZE];
    private int energyHistoryIndex = 0;
    private float lastBassEnergy = 0f;
    private float bassEnergyHistory = 0f;
    private boolean beatDetected = false;
    private float beatIntensity = 0f;
    private float timeSinceLastBeat = 1.0f;
    private float warmupTime = 0f;
    private static final float BEAT_SENSITIVITY = 0.8f;    // MUY sensible
    private static final float BEAT_COOLDOWN = 0.15f;      // 150ms (400 BPM max)
    private static final float BEAT_DECAY = 1.5f;          // Decay visual
    private static final float WARMUP_DURATION = 0.2f;     // Warmup corto
    private int beatCounter = 0;

    // ════════════════════════════════════════════════════════════════════════
    // ⚡ SISTEMA DE RAYOS ELÉCTRICOS - Conectan barras altas en los beats
    // ════════════════════════════════════════════════════════════════════════
    private static final int MAX_LIGHTNINGS = 8;   // Máximo de rayos simultáneos
    private static final int LIGHTNING_SEGMENTS = 6;  // Segmentos por rayo (zigzag)

    // Cada rayo tiene: posición inicio/fin, vida, color, puntos del zigzag
    private float[] lightningStartX = new float[MAX_LIGHTNINGS];
    private float[] lightningStartY = new float[MAX_LIGHTNINGS];
    private float[] lightningEndX = new float[MAX_LIGHTNINGS];
    private float[] lightningEndY = new float[MAX_LIGHTNINGS];
    private float[] lightningLife = new float[MAX_LIGHTNINGS];
    private float[] lightningR = new float[MAX_LIGHTNINGS];
    private float[] lightningG = new float[MAX_LIGHTNINGS];
    private float[] lightningB = new float[MAX_LIGHTNINGS];
    private float[] lightningIntensity = new float[MAX_LIGHTNINGS];
    // Puntos intermedios del zigzag (para cada rayo)
    private float[][] lightningPointsX = new float[MAX_LIGHTNINGS][LIGHTNING_SEGMENTS + 1];
    private float[][] lightningPointsY = new float[MAX_LIGHTNINGS][LIGHTNING_SEGMENTS + 1];

    private int lightningCount = 0;
    private java.util.Random random = new java.util.Random();
    // 🔧 FIX: Cache pre-allocado para evitar crear array en cada beat
    private final int[] highBarsCache = new int[NUM_BARS];

    // ════════════════════════════════════════════════════════════════════════
    // ✨ CHISPAS ENTRE PEAKS - Mini rayos que saltan entre peaks cercanos
    // ════════════════════════════════════════════════════════════════════════
    private static final int MAX_PEAK_SPARKS = 12;
    private float[] sparkStartX = new float[MAX_PEAK_SPARKS];
    private float[] sparkStartY = new float[MAX_PEAK_SPARKS];
    private float[] sparkEndX = new float[MAX_PEAK_SPARKS];
    private float[] sparkEndY = new float[MAX_PEAK_SPARKS];
    private float[] sparkLife = new float[MAX_PEAK_SPARKS];
    private float[] sparkR = new float[MAX_PEAK_SPARKS];
    private float[] sparkG = new float[MAX_PEAK_SPARKS];
    private float[] sparkB = new float[MAX_PEAK_SPARKS];
    private int sparkCount = 0;
    private float sparkTimer = 0f;
    private static final float SPARK_INTERVAL = 0.08f;  // Generar chispas cada 80ms

    // ════════════════════════════════════════════════════════════════════════
    // 🌊 ONDAS DE ENERGÍA - Se expanden en los beats (MEJORADAS)
    // ════════════════════════════════════════════════════════════════════════
    private static final int MAX_WAVES = 3;  // Menos ondas pero más visibles
    private float[] waveRadius = new float[MAX_WAVES];
    private float[] waveAlpha = new float[MAX_WAVES];
    private float[] waveR = new float[MAX_WAVES];
    private float[] waveG = new float[MAX_WAVES];
    private float[] waveB = new float[MAX_WAVES];
    private int waveIndex = 0;
    private static final float WAVE_SPEED = 1.2f;  // Velocidad moderada
    private static final float WAVE_MAX_RADIUS = 0.6f;  // Tamaño máximo
    private static final float WAVE_THICKNESS = 0.025f;  // Grosor de la onda

    // ════════════════════════════════════════════════════════════════════════
    // OPENGL
    // ════════════════════════════════════════════════════════════════════════
    private int shaderProgram;
    private int aPositionHandle;
    private int aColorHandle;
    private int uMVPMatrixHandle;

    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;
    private FloatBuffer glowVertexBuffer;
    private FloatBuffer glowColorBuffer;
    private FloatBuffer peakVertexBuffer;
    private FloatBuffer peakColorBuffer;

    // Buffers para rayos eléctricos
    private FloatBuffer lightningVertexBuffer;
    private FloatBuffer lightningColorBuffer;

    // Buffers para chispas entre peaks
    private FloatBuffer sparkVertexBuffer;
    private FloatBuffer sparkColorBuffer;

    // Buffers para ondas de energía
    private static final int WAVE_SEGMENTS = 32;
    private FloatBuffer waveVertexBuffer;
    private FloatBuffer waveColorBuffer;

    private final float[] projectionMatrix = new float[16];

    private float aspectRatio = 0.5f;
    private boolean initialized = false;

    // ⚡ OPTIMIZACIÓN: Arrays reutilizables para evitar allocations en métodos de dibujo
    // Geometry caches (tamaño: NUM_BARS * 4 * 3 para vértices, NUM_BARS * 4 * 4 para colores)
    private final float[] barVerticesCache = new float[NUM_BARS * 4 * 3];
    private final float[] barColorsCache = new float[NUM_BARS * 4 * 4];
    private final float[] glowVerticesCache = new float[NUM_BARS * 4 * 3];
    private final float[] glowColorsCache = new float[NUM_BARS * 4 * 4];
    private final float[] peakVerticesCache = new float[NUM_BARS * 4 * 3];
    private final float[] peakColorsCache = new float[NUM_BARS * 4 * 4];

    // Cache para getBarColor (evitar crear float[3] cada llamada)
    private final float[] colorResultCache = new float[3];
    private final float[] baseColorCache = new float[3];
    private final float[] topColorCache = new float[3];
    private final float[] glowColorCache = new float[3];
    private final float[] lightning1ColorCache = new float[3];
    private final float[] lightning2ColorCache = new float[3];

    // Caches para rayos eléctricos (LIGHTNING_SEGMENTS * 4 * 3/4)
    private final float[] lightningVerticesCache = new float[LIGHTNING_SEGMENTS * 4 * 3];
    private final float[] lightningColorsCache = new float[LIGHTNING_SEGMENTS * 4 * 4];

    // Caches para ondas de energía (24 segmentos * 4 * 3/4)
    private static final int WAVE_SEGMENTS_DRAW = 24;
    private final float[] waveVerticesCache = new float[WAVE_SEGMENTS_DRAW * 4 * 3];
    private final float[] waveColorsCache = new float[WAVE_SEGMENTS_DRAW * 4 * 4];

    // Caches para chispas (3 segmentos * 4 * 3/4)
    private final float[] sparkPointsXCache = new float[4];
    private final float[] sparkPointsYCache = new float[4];
    private final float[] sparkVerticesCache = new float[3 * 4 * 3];
    private final float[] sparkColorsCache = new float[3 * 4 * 4];

    // ════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ════════════════════════════════════════════════════════════════════════
    public EqualizerBarsDJ() {
        barLevels = new float[NUM_BARS];
        smoothedLevels = new float[NUM_BARS];
        targetLevels = new float[NUM_BARS];
        heightMultipliers = new float[NUM_BARS];
        peakLevels = new float[NUM_BARS];
        peakHoldTime = new float[NUM_BARS];

        calculateSymmetricCurve();

        // Registrarse en AspectRatioManager
        AspectRatioManager.get().register(this);

        Log.d(TAG, "🎵 EqualizerBarsDJ v3.0 creado con " + NUM_BARS + " barras");
    }

    /**
     * 🎨 Establece el tema de colores del ecualizador
     * @param theme Theme.ABYSSIA, Theme.PYRALIS, o Theme.DEFAULT
     */
    public void setTheme(Theme theme) {
        this.currentTheme = theme;

        switch (theme) {
            case ABYSSIA:
                colorBass = ABYSSIA_BASS;
                colorTreble = ABYSSIA_TREBLE;
                colorPeak = ABYSSIA_PEAK;
                colorWaveStrong = ABYSSIA_WAVE_STRONG;
                colorWaveNormal = ABYSSIA_WAVE_NORMAL;
                colorLightning1 = ABYSSIA_LIGHTNING_1;
                colorLightning2 = ABYSSIA_LIGHTNING_2;
                Log.d(TAG, "🌊 Tema ABYSSIA activado - Océano profundo");
                break;

            case PYRALIS:
                colorBass = PYRALIS_BASS;
                colorTreble = PYRALIS_TREBLE;
                colorPeak = PYRALIS_PEAK;
                colorWaveStrong = PYRALIS_WAVE_STRONG;
                colorWaveNormal = PYRALIS_WAVE_NORMAL;
                colorLightning1 = PYRALIS_LIGHTNING_1;
                colorLightning2 = PYRALIS_LIGHTNING_2;
                Log.d(TAG, "🔥 Tema PYRALIS activado - Fuego");
                break;

            case KAMEHAMEHA:
                colorBass = KAMEHAMEHA_BASS;
                colorTreble = KAMEHAMEHA_TREBLE;
                colorPeak = KAMEHAMEHA_PEAK;
                colorWaveStrong = KAMEHAMEHA_WAVE_STRONG;
                colorWaveNormal = KAMEHAMEHA_WAVE_NORMAL;
                colorLightning1 = KAMEHAMEHA_LIGHTNING_1;
                colorLightning2 = KAMEHAMEHA_LIGHTNING_2;
                Log.d(TAG, "🐉 Tema KAMEHAMEHA activado - Energía Ki");
                break;

            case SYNTHWAVE:
                colorBass = SYNTHWAVE_BASS;
                colorTreble = SYNTHWAVE_TREBLE;
                colorPeak = SYNTHWAVE_PEAK;
                colorWaveStrong = SYNTHWAVE_WAVE_STRONG;
                colorWaveNormal = SYNTHWAVE_WAVE_NORMAL;
                colorLightning1 = SYNTHWAVE_LIGHTNING_1;
                colorLightning2 = SYNTHWAVE_LIGHTNING_2;
                Log.d(TAG, "🌆 Tema SYNTHWAVE activado - Retrowave 80s");
                break;

            case COSMOS:
                colorBass = COSMOS_BASS;
                colorTreble = COSMOS_TREBLE;
                colorPeak = COSMOS_PEAK;
                colorWaveStrong = COSMOS_WAVE_STRONG;
                colorWaveNormal = COSMOS_WAVE_NORMAL;
                colorLightning1 = COSMOS_LIGHTNING_1;
                colorLightning2 = COSMOS_LIGHTNING_2;
                Log.d(TAG, "⭐ Tema COSMOS activado - Saint Seiya Power");
                break;

            default:
                colorBass = DEFAULT_BASS;
                colorTreble = DEFAULT_TREBLE;
                colorPeak = DEFAULT_PEAK;
                colorWaveStrong = DEFAULT_WAVE_STRONG;
                colorWaveNormal = DEFAULT_WAVE_NORMAL;
                colorLightning1 = DEFAULT_LIGHTNING_1;
                colorLightning2 = DEFAULT_LIGHTNING_2;
                Log.d(TAG, "✨ Tema DEFAULT activado - Rosa/Cyan");
                break;
        }
    }

    /**
     * @return El tema actual del ecualizador
     */
    public Theme getTheme() {
        return currentTheme;
    }

    /**
     * Calcula la curva simétrica para las alturas máximas
     */
    private void calculateSymmetricCurve() {
        int center = NUM_BARS / 2;
        for (int i = 0; i < NUM_BARS; i++) {
            float distFromCenter = Math.abs(i - center) / (float) center;
            heightMultipliers[i] = 0.35f + 0.65f * (float) Math.cos(distFromCenter * Math.PI * 0.5f);
        }
    }

    /**
     * Callback de AspectRatioManager
     */
    @Override
    public void onAspectRatioChanged(int width, int height, float newAspectRatio) {
        this.aspectRatio = newAspectRatio;
        Log.d(TAG, "📐 AspectRatio actualizado: " + aspectRatio);
    }

    /**
     * Inicializa shaders y buffers OpenGL
     */
    public void initialize() {
        if (initialized) return;

        // Shader con soporte para transparencia (glow)
        String vertexShaderCode =
            "attribute vec4 a_Position;" +
            "attribute vec4 a_Color;" +
            "varying vec4 v_Color;" +
            "uniform mat4 u_MVPMatrix;" +
            "void main() {" +
            "    gl_Position = u_MVPMatrix * a_Position;" +
            "    v_Color = a_Color;" +
            "}";

        String fragmentShaderCode =
            "precision mediump float;" +
            "varying vec4 v_Color;" +
            "void main() {" +
            "    gl_FragColor = v_Color;" +
            "}";

        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode);

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vertexShader);
        GLES30.glAttachShader(shaderProgram, fragmentShader);
        GLES30.glLinkProgram(shaderProgram);

        int[] linked = new int[1];
        GLES30.glGetProgramiv(shaderProgram, GLES30.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            Log.e(TAG, "❌ Error linkeando programa: " + GLES30.glGetProgramInfoLog(shaderProgram));
            GLES30.glDeleteProgram(shaderProgram);
            GLES30.glDeleteShader(vertexShader);
            GLES30.glDeleteShader(fragmentShader);
            return;
        }

        // 🔧 FIX: Eliminar shaders después de linkear (ya no se necesitan)
        GLES30.glDeleteShader(vertexShader);
        GLES30.glDeleteShader(fragmentShader);

        aPositionHandle = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        aColorHandle = GLES30.glGetAttribLocation(shaderProgram, "a_Color");
        uMVPMatrixHandle = GLES30.glGetUniformLocation(shaderProgram, "u_MVPMatrix");

        // Buffers para barras principales
        int vertexCount = NUM_BARS * 4 * 3;
        vertexBuffer = createFloatBuffer(vertexCount);
        colorBuffer = createFloatBuffer(NUM_BARS * 4 * 4);

        // Buffers para glow (mismo número de quads pero más anchos)
        glowVertexBuffer = createFloatBuffer(vertexCount);
        glowColorBuffer = createFloatBuffer(NUM_BARS * 4 * 4);

        // Buffers para peak markers
        peakVertexBuffer = createFloatBuffer(vertexCount);
        peakColorBuffer = createFloatBuffer(NUM_BARS * 4 * 4);

        // Buffers para rayos eléctricos (cada rayo = LIGHTNING_SEGMENTS líneas = SEGMENTS*2 vértices)
        // MAX_LIGHTNINGS rayos * (SEGMENTS+1) puntos * 2 (para líneas gruesas) * 3 coords
        lightningVertexBuffer = createFloatBuffer(MAX_LIGHTNINGS * (LIGHTNING_SEGMENTS + 1) * 4 * 3);
        lightningColorBuffer = createFloatBuffer(MAX_LIGHTNINGS * (LIGHTNING_SEGMENTS + 1) * 4 * 4);

        // Buffers para chispas entre peaks (cada chispa = 3 segmentos zigzag * 4 vértices)
        sparkVertexBuffer = createFloatBuffer(MAX_PEAK_SPARKS * 3 * 4 * 3);
        sparkColorBuffer = createFloatBuffer(MAX_PEAK_SPARKS * 3 * 4 * 4);

        // Buffers para ondas (cada onda es un círculo de líneas)
        waveVertexBuffer = createFloatBuffer(MAX_WAVES * WAVE_SEGMENTS * 2 * 3);
        waveColorBuffer = createFloatBuffer(MAX_WAVES * WAVE_SEGMENTS * 2 * 4);

        Matrix.setIdentityM(projectionMatrix, 0);

        initialized = true;
        Log.d(TAG, "✓ EqualizerBarsDJ v3.0 inicializado con glow, peaks, partículas y ondas");
    }

    private FloatBuffer createFloatBuffer(int size) {
        ByteBuffer bb = ByteBuffer.allocateDirect(size * 4);
        bb.order(ByteOrder.nativeOrder());
        return bb.asFloatBuffer();
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "❌ Error compilando shader: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    /**
     * Actualiza los niveles de música (método legacy)
     */
    public void updateMusicLevels(float bass, float mid, float treble) {
        this.bassLevel = bass;
        this.midLevel = mid;
        this.trebleLevel = treble;
        // Este método ya no se usa directamente, updateFromBands lo reemplaza
    }

    /**
     * 🎵 Actualiza usando las 32 bandas de frecuencia directamente
     * Incluye detección de beats para efectos especiales
     */
    public void updateFromBands(float[] bands) {
        if (bands == null || bands.length < NUM_BARS) return;

        // 🔧 FIX: Cache de tiempo al inicio (evita llamar 32x por frame)
        final long cachedTime = System.currentTimeMillis() % 10000;

        // ════════════════════════════════════════════════════════════════════════
        // 💥 BEAT DETECTION - Algoritmo con historial de energía
        // ════════════════════════════════════════════════════════════════════════

        // Calcular energía actual de bass (bandas 0-5 = sub-bass y kick)
        float currentBassEnergy = 0f;
        for (int i = 0; i < 6; i++) {
            currentBassEnergy += bands[i];
        }
        currentBassEnergy /= 6f;

        // Guardar en historial circular
        energyHistory[energyHistoryIndex] = currentBassEnergy;
        energyHistoryIndex = (energyHistoryIndex + 1) % ENERGY_HISTORY_SIZE;

        // Calcular promedio del historial
        float avgEnergy = 0f;
        for (int i = 0; i < ENERGY_HISTORY_SIZE; i++) {
            avgEnergy += energyHistory[i];
        }
        avgEnergy /= ENERGY_HISTORY_SIZE;

        // Calcular varianza para adaptarse al volumen
        float variance = 0f;
        for (int i = 0; i < ENERGY_HISTORY_SIZE; i++) {
            float diff = energyHistory[i] - avgEnergy;
            variance += diff * diff;
        }
        variance /= ENERGY_HISTORY_SIZE;
        float stdDev = (float)Math.sqrt(variance);

        // Threshold dinámico: promedio + (sensibilidad * desviación estándar)
        float dynamicThreshold = avgEnergy + BEAT_SENSITIVITY * Math.max(stdDev, 0.05f);

        // Detectar beat: energía actual supera threshold Y hay subida respecto al frame anterior
        boolean isRising = currentBassEnergy > lastBassEnergy * 1.1f;  // Subiendo al menos 10%
        boolean isBeatCandidate = currentBassEnergy > dynamicThreshold && isRising;

        if (isBeatCandidate &&
            timeSinceLastBeat > BEAT_COOLDOWN &&
            warmupTime > WARMUP_DURATION) {

            // ¡BEAT DETECTADO! 💥
            beatDetected = true;
            float excessEnergy = (currentBassEnergy - avgEnergy) / Math.max(avgEnergy, 0.1f);
            beatIntensity = Math.min(1.0f, excessEnergy * 1.5f);
            timeSinceLastBeat = 0f;
            beatCounter++;

            // Crear efectos visuales
            spawnLightning(beatIntensity);
            spawnEnergyWave(beatIntensity);
        }

        // Actualizar historial
        bassEnergyHistory = bassEnergyHistory * 0.8f + currentBassEnergy * 0.2f;
        lastBassEnergy = currentBassEnergy;

        // ════════════════════════════════════════════════════════════════════════
        // ACTUALIZAR NIVELES DE BARRAS
        // ════════════════════════════════════════════════════════════════════════
        for (int i = 0; i < NUM_BARS; i++) {
            float level = bands[i];

            // Aplicar curva simétrica
            level *= heightMultipliers[i];

            // Compensación por frecuencia
            float freqBoost = 1.0f + (i / (float)NUM_BARS) * 0.8f;
            level *= freqBoost;

            // 💥 BEAT BOOST - Las barras saltan más en los beats
            if (beatIntensity > 0.1f) {
                // Las barras de bass reciben más boost
                float bassInfluence = 1.0f - (i / (float)NUM_BARS);
                level += beatIntensity * 0.3f * bassInfluence;
            }

            // Micro variación solo cuando hay nivel real
            // 🔧 FIX: Usar tiempo cacheado en lugar de llamar System.currentTimeMillis() 32x
            if (level > 0.05f) {
                float microVariation = (float) Math.sin(i * 1.2f + cachedTime * 0.006f) * 0.015f;
                level += microVariation * level;
            }

            // Permitir que llegue a casi cero (min 0.005 en vez de 0.02)
            targetLevels[i] = Math.max(0.005f, Math.min(0.95f, level));
        }
    }

    /**
     * ⚡ Genera rayos eléctricos entre barras altas cuando hay un beat
     */
    private void spawnLightning(float intensity) {
        // Encontrar las barras más altas para conectar con rayos
        int numLightnings = 2 + (int)(intensity * 4);  // 2-6 rayos por beat

        // 🔧 FIX: Usar array pre-allocado en lugar de crear uno nuevo cada beat
        int highBarCount = 0;
        float threshold = 0.4f;  // Umbral para considerar una barra "alta"

        for (int i = 0; i < NUM_BARS; i++) {
            if (barLevels[i] > threshold) {
                highBarsCache[highBarCount++] = i;
            }
        }

        // Si no hay suficientes barras altas, usar posiciones aleatorias
        if (highBarCount < 2) {
            highBarCount = NUM_BARS;
            for (int i = 0; i < NUM_BARS; i++) {
                highBarsCache[i] = i;
            }
        }

        // Calcular ancho de barras para posiciones X
        float totalWidth = aspectRatio * 2f * 0.92f;
        float barWidth = (totalWidth - (NUM_BARS - 1) * BAR_SPACING) / NUM_BARS;
        float startX = -totalWidth / 2f;

        for (int i = 0; i < numLightnings && lightningCount < MAX_LIGHTNINGS; i++) {
            int idx = lightningCount;

            // Seleccionar dos barras aleatorias para conectar
            int bar1Idx = highBarsCache[random.nextInt(highBarCount)];
            int bar2Idx = highBarsCache[random.nextInt(highBarCount)];

            // Asegurar que sean diferentes y no muy cercanas
            int attempts = 0;
            while (Math.abs(bar1Idx - bar2Idx) < 3 && attempts < 10) {
                bar2Idx = highBarsCache[random.nextInt(highBarCount)];
                attempts++;
            }

            // Calcular posiciones X de las barras
            float x1 = startX + bar1Idx * (barWidth + BAR_SPACING) + barWidth / 2f;
            float x2 = startX + bar2Idx * (barWidth + BAR_SPACING) + barWidth / 2f;

            // Posiciones Y basadas en la altura de las barras
            float y1 = BASE_Y + MIN_HEIGHT + barLevels[bar1Idx] * (MAX_HEIGHT - MIN_HEIGHT);
            float y2 = BASE_Y + MIN_HEIGHT + barLevels[bar2Idx] * (MAX_HEIGHT - MIN_HEIGHT);

            // Guardar inicio y fin
            lightningStartX[idx] = x1;
            lightningStartY[idx] = y1;
            lightningEndX[idx] = x2;
            lightningEndY[idx] = y2;

            // Vida del rayo (corta para efecto eléctrico)
            lightningLife[idx] = 0.15f + random.nextFloat() * 0.2f;  // 0.15-0.35 segundos
            lightningIntensity[idx] = intensity;

            // Color según tema (con variación aleatoria)
            float variation = random.nextFloat() * 0.2f;
            if (random.nextFloat() > 0.3f) {
                // Color primario del tema (70% de probabilidad)
                lightningR[idx] = colorLightning1[0] + variation;
                lightningG[idx] = colorLightning1[1] + variation;
                lightningB[idx] = colorLightning1[2];
            } else {
                // Color secundario del tema (30%)
                lightningR[idx] = colorLightning2[0];
                lightningG[idx] = colorLightning2[1] + variation;
                lightningB[idx] = colorLightning2[2] + variation;
            }

            // Generar puntos del zigzag
            generateLightningPath(idx);

            lightningCount++;
        }
    }

    /**
     * ⚡ Genera el camino zigzagueante del rayo
     */
    private void generateLightningPath(int idx) {
        float x1 = lightningStartX[idx];
        float y1 = lightningStartY[idx];
        float x2 = lightningEndX[idx];
        float y2 = lightningEndY[idx];

        // Primer punto = inicio
        lightningPointsX[idx][0] = x1;
        lightningPointsY[idx][0] = y1;

        // Último punto = fin
        lightningPointsX[idx][LIGHTNING_SEGMENTS] = x2;
        lightningPointsY[idx][LIGHTNING_SEGMENTS] = y2;

        // Puntos intermedios con desplazamiento aleatorio (zigzag)
        for (int i = 1; i < LIGHTNING_SEGMENTS; i++) {
            float t = (float) i / LIGHTNING_SEGMENTS;

            // Posición base (interpolación lineal)
            float baseX = x1 + (x2 - x1) * t;
            float baseY = y1 + (y2 - y1) * t;

            // Calcular perpendicular para el desplazamiento
            float dx = x2 - x1;
            float dy = y2 - y1;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            float perpX = -dy / len;
            float perpY = dx / len;

            // Desplazamiento aleatorio perpendicular (más en el centro, menos en extremos)
            float maxOffset = 0.08f * (1.0f - Math.abs(t - 0.5f) * 2f);  // Máximo en el centro
            float offset = (random.nextFloat() - 0.5f) * 2f * maxOffset;

            lightningPointsX[idx][i] = baseX + perpX * offset;
            lightningPointsY[idx][i] = baseY + perpY * offset;
        }
    }

    /**
     * ✨ Genera chispas eléctricas entre peaks cercanos con nivel alto
     */
    private void spawnPeakSparks() {
        // Calcular posiciones de las barras
        float totalWidth = aspectRatio * 2f * 0.92f;
        float barWidth = (totalWidth - (NUM_BARS - 1) * BAR_SPACING) / NUM_BARS;
        float startX = -totalWidth / 2f;

        // Buscar pares de peaks cercanos con nivel alto para conectar
        float minPeakLevel = 0.35f;  // Umbral mínimo para generar chispa

        for (int i = 0; i < NUM_BARS - 1 && sparkCount < MAX_PEAK_SPARKS; i++) {
            // Verificar si este peak y alguno cercano están altos
            if (peakLevels[i] < minPeakLevel) continue;

            // Buscar un peak cercano (1-4 barras de distancia)
            int maxDist = 4;
            for (int j = i + 1; j <= Math.min(i + maxDist, NUM_BARS - 1); j++) {
                if (peakLevels[j] < minPeakLevel) continue;

                // Probabilidad de generar chispa basada en los niveles
                float avgLevel = (peakLevels[i] + peakLevels[j]) / 2f;
                if (random.nextFloat() > avgLevel * 0.5f) continue;  // Mayor nivel = más probabilidad

                int idx = sparkCount;

                // Calcular posiciones
                float x1 = startX + i * (barWidth + BAR_SPACING) + barWidth / 2f;
                float y1 = BASE_Y + MIN_HEIGHT + peakLevels[i] * (MAX_HEIGHT - MIN_HEIGHT);
                float x2 = startX + j * (barWidth + BAR_SPACING) + barWidth / 2f;
                float y2 = BASE_Y + MIN_HEIGHT + peakLevels[j] * (MAX_HEIGHT - MIN_HEIGHT);

                sparkStartX[idx] = x1;
                sparkStartY[idx] = y1;
                sparkEndX[idx] = x2;
                sparkEndY[idx] = y2;

                // Vida corta para efecto de chispa rápida
                sparkLife[idx] = 0.06f + random.nextFloat() * 0.08f;  // 60-140ms

                // Color: mezcla de los colores de ambas barras
                // ⚡ OPTIMIZADO: Usar caches en lugar de crear arrays
                getBarColor(i, 1.0f, lightning1ColorCache);
                getBarColor(j, 1.0f, lightning2ColorCache);
                sparkR[idx] = (lightning1ColorCache[0] + lightning2ColorCache[0]) / 2f;
                sparkG[idx] = (lightning1ColorCache[1] + lightning2ColorCache[1]) / 2f;
                sparkB[idx] = (lightning1ColorCache[2] + lightning2ColorCache[2]) / 2f;

                // Hacer más brillante
                sparkR[idx] = Math.min(1.0f, sparkR[idx] * 1.5f);
                sparkG[idx] = Math.min(1.0f, sparkG[idx] * 1.5f);
                sparkB[idx] = Math.min(1.0f, sparkB[idx] * 1.5f);

                sparkCount++;

                // Solo una chispa por peak origen en esta iteración
                break;
            }
        }
    }

    /**
     * 🌊 Genera una onda de energía cuando hay un beat
     */
    private void spawnEnergyWave(float intensity) {
        waveRadius[waveIndex] = 0.05f;  // Empezar más grande
        waveAlpha[waveIndex] = 1.0f;  // Alpha máximo

        // Color de la onda según tema e intensidad
        if (intensity > 0.6f) {
            // Beat fuerte: color wave strong del tema
            waveR[waveIndex] = colorWaveStrong[0];
            waveG[waveIndex] = colorWaveStrong[1];
            waveB[waveIndex] = colorWaveStrong[2];
        } else {
            // Beat normal: color wave normal del tema
            waveR[waveIndex] = colorWaveNormal[0];
            waveG[waveIndex] = colorWaveNormal[1];
            waveB[waveIndex] = colorWaveNormal[2];
        }

        waveIndex = (waveIndex + 1) % MAX_WAVES;
    }

    @Override
    public void update(float deltaTime) {
        if (!initialized) return;

        // ════════════════════════════════════════════════════════════════════════
        // ACTUALIZAR BARRAS Y PEAKS
        // ════════════════════════════════════════════════════════════════════════
        for (int i = 0; i < NUM_BARS; i++) {
            float target = targetLevels[i];
            float current = smoothedLevels[i];

            // Suavizado
            if (target > current) {
                smoothedLevels[i] = current + (target - current) * SMOOTHING_UP;
            } else {
                smoothedLevels[i] = current + (target - current) * SMOOTHING_DOWN;
            }

            barLevels[i] = smoothedLevels[i];

            // Actualizar peak markers
            if (barLevels[i] > peakLevels[i]) {
                peakLevels[i] = barLevels[i];
                peakHoldTime[i] = PEAK_HOLD_TIME;
            } else {
                peakHoldTime[i] -= deltaTime;
                if (peakHoldTime[i] <= 0) {
                    peakLevels[i] -= PEAK_FALL_SPEED * deltaTime;
                    if (peakLevels[i] < barLevels[i]) {
                        peakLevels[i] = barLevels[i];
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════════════════
        // 💥 ACTUALIZAR BEAT INTENSITY (decay) y WARMUP
        // ════════════════════════════════════════════════════════════════════════
        timeSinceLastBeat += deltaTime;
        warmupTime += deltaTime;  // Contador de warmup

        if (beatIntensity > 0) {
            beatIntensity -= BEAT_DECAY * deltaTime;
            if (beatIntensity < 0) beatIntensity = 0;
        }
        beatDetected = false;

        // ════════════════════════════════════════════════════════════════════════
        // ⚡ ACTUALIZAR RAYOS ELÉCTRICOS
        // ════════════════════════════════════════════════════════════════════════
        int aliveLightnings = 0;
        for (int i = 0; i < lightningCount; i++) {
            lightningLife[i] -= deltaTime;

            if (lightningLife[i] > 0) {
                // Regenerar zigzag periódicamente para efecto de "parpadeo"
                if (random.nextFloat() < 0.3f) {  // 30% de probabilidad por frame
                    generateLightningPath(i);
                }

                // Compactar array
                if (aliveLightnings != i) {
                    lightningStartX[aliveLightnings] = lightningStartX[i];
                    lightningStartY[aliveLightnings] = lightningStartY[i];
                    lightningEndX[aliveLightnings] = lightningEndX[i];
                    lightningEndY[aliveLightnings] = lightningEndY[i];
                    lightningLife[aliveLightnings] = lightningLife[i];
                    lightningR[aliveLightnings] = lightningR[i];
                    lightningG[aliveLightnings] = lightningG[i];
                    lightningB[aliveLightnings] = lightningB[i];
                    lightningIntensity[aliveLightnings] = lightningIntensity[i];
                    // Copiar puntos del zigzag
                    for (int p = 0; p <= LIGHTNING_SEGMENTS; p++) {
                        lightningPointsX[aliveLightnings][p] = lightningPointsX[i][p];
                        lightningPointsY[aliveLightnings][p] = lightningPointsY[i][p];
                    }
                }
                aliveLightnings++;
            }
        }
        lightningCount = aliveLightnings;

        // ════════════════════════════════════════════════════════════════════════
        // ✨ ACTUALIZAR CHISPAS ENTRE PEAKS
        // ════════════════════════════════════════════════════════════════════════
        sparkTimer += deltaTime;
        if (sparkTimer >= SPARK_INTERVAL) {
            sparkTimer = 0f;
            spawnPeakSparks();
        }

        // Actualizar vida de chispas existentes
        int aliveSparks = 0;
        for (int i = 0; i < sparkCount; i++) {
            sparkLife[i] -= deltaTime;
            if (sparkLife[i] > 0) {
                if (aliveSparks != i) {
                    sparkStartX[aliveSparks] = sparkStartX[i];
                    sparkStartY[aliveSparks] = sparkStartY[i];
                    sparkEndX[aliveSparks] = sparkEndX[i];
                    sparkEndY[aliveSparks] = sparkEndY[i];
                    sparkLife[aliveSparks] = sparkLife[i];
                    sparkR[aliveSparks] = sparkR[i];
                    sparkG[aliveSparks] = sparkG[i];
                    sparkB[aliveSparks] = sparkB[i];
                }
                aliveSparks++;
            }
        }
        sparkCount = aliveSparks;

        // ════════════════════════════════════════════════════════════════════════
        // 🌊 ACTUALIZAR ONDAS DE ENERGÍA
        // ════════════════════════════════════════════════════════════════════════
        for (int i = 0; i < MAX_WAVES; i++) {
            if (waveAlpha[i] > 0) {
                waveRadius[i] += WAVE_SPEED * deltaTime;
                waveAlpha[i] -= 0.8f * deltaTime;  // Fade más lento (era 1.5)

                if (waveRadius[i] > WAVE_MAX_RADIUS || waveAlpha[i] <= 0) {
                    waveAlpha[i] = 0;
                }
            }
        }
    }

    @Override
    public void draw() {
        if (!initialized) return;

        GLES30.glUseProgram(shaderProgram);

        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // Matriz ortográfica
        Matrix.orthoM(projectionMatrix, 0,
                     -aspectRatio, aspectRatio,
                     -1f, 1f,
                     -1f, 1f);

        GLES30.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, projectionMatrix, 0);

        // 1. Dibujar GLOW (detrás, más grande, semi-transparente)
        updateGlowGeometry();
        drawBuffers(glowVertexBuffer, glowColorBuffer);

        // 2. Dibujar barras principales
        updateBarGeometry();
        drawBuffers(vertexBuffer, colorBuffer);

        // 3. Dibujar peak markers
        updatePeakGeometry();
        drawBuffers(peakVertexBuffer, peakColorBuffer);

        // 4. 🌊 Dibujar ondas de energía
        drawEnergyWaves();

        // 5. ⚡ Dibujar rayos eléctricos
        drawLightning();

        // 6. ✨ Dibujar chispas entre peaks
        drawPeakSparks();

        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
    }

    private void drawBuffers(FloatBuffer vBuffer, FloatBuffer cBuffer) {
        vBuffer.position(0);
        GLES30.glEnableVertexAttribArray(aPositionHandle);
        GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, vBuffer);

        cBuffer.position(0);
        GLES30.glEnableVertexAttribArray(aColorHandle);
        GLES30.glVertexAttribPointer(aColorHandle, 4, GLES30.GL_FLOAT, false, 0, cBuffer);

        for (int i = 0; i < NUM_BARS; i++) {
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, i * 4, 4);
        }

        GLES30.glDisableVertexAttribArray(aPositionHandle);
        GLES30.glDisableVertexAttribArray(aColorHandle);
    }

    /**
     * Calcula el color de una barra basado en su posición (gradiente rosa→cyan)
     * ⚡ OPTIMIZADO: Escribe resultado en array de salida para evitar allocations
     * @param barIndex Índice de la barra (0 a NUM_BARS-1)
     * @param intensity Intensidad del color (0 a 1)
     * @param outColor Array de salida de tamaño 3 donde se escriben los valores RGB
     */
    private void getBarColor(int barIndex, float intensity, float[] outColor) {
        float t = (float) barIndex / (NUM_BARS - 1);  // 0 a 1 (izquierda a derecha)

        // Distancia del centro (0 = centro, 1 = extremos)
        float distFromCenter = Math.abs(t - 0.5f) * 2f;

        // Interpolar entre rosa (centro) y cyan (lados)
        float r = colorBass[0] + (colorTreble[0] - colorBass[0]) * distFromCenter;
        float g = colorBass[1] + (colorTreble[1] - colorBass[1]) * distFromCenter;
        float b = colorBass[2] + (colorTreble[2] - colorBass[2]) * distFromCenter;

        // Si el nivel es muy alto, mezclar con color peak del tema
        if (intensity > 0.8f) {
            float peakMix = (intensity - 0.8f) * 2.5f;  // 0 a 0.5
            r = r + (colorPeak[0] - r) * peakMix;
            g = g + (colorPeak[1] - g) * peakMix;
            b = b + (colorPeak[2] - b) * peakMix;
        }

        outColor[0] = r * intensity;
        outColor[1] = g * intensity;
        outColor[2] = b * intensity;
    }

    /**
     * Actualiza la geometría de las barras principales
     * ⚡ OPTIMIZADO: Usa arrays cacheados en lugar de crear nuevos
     */
    private void updateBarGeometry() {
        // ⚡ OPTIMIZADO: Usar caches en lugar de new float[]
        float[] vertices = barVerticesCache;
        float[] colors = barColorsCache;

        float totalWidth = aspectRatio * 2f * 0.92f;
        float barWidth = (totalWidth - (NUM_BARS - 1) * BAR_SPACING) / NUM_BARS;
        float startX = -totalWidth / 2f;

        for (int i = 0; i < NUM_BARS; i++) {
            float x = startX + i * (barWidth + BAR_SPACING);
            float level = barLevels[i];
            float height = MIN_HEIGHT + level * (MAX_HEIGHT - MIN_HEIGHT);

            int vi = i * 12;
            int ci = i * 16;

            // Vértices
            vertices[vi + 0] = x;
            vertices[vi + 1] = BASE_Y;
            vertices[vi + 2] = 0f;

            vertices[vi + 3] = x + barWidth;
            vertices[vi + 4] = BASE_Y;
            vertices[vi + 5] = 0f;

            vertices[vi + 6] = x;
            vertices[vi + 7] = BASE_Y + height;
            vertices[vi + 8] = 0f;

            vertices[vi + 9] = x + barWidth;
            vertices[vi + 10] = BASE_Y + height;
            vertices[vi + 11] = 0f;

            // Colores con gradiente vertical
            // ⚡ OPTIMIZADO: Usar caches en lugar de crear arrays
            getBarColor(i, 0.5f + level * 0.3f, baseColorCache);
            getBarColor(i, 0.8f + level * 0.2f, topColorCache);

            // Bottom colors (más oscuros)
            colors[ci + 0] = baseColorCache[0] * 0.6f;
            colors[ci + 1] = baseColorCache[1] * 0.6f;
            colors[ci + 2] = baseColorCache[2] * 0.6f;
            colors[ci + 3] = 1.0f;

            colors[ci + 4] = baseColorCache[0] * 0.6f;
            colors[ci + 5] = baseColorCache[1] * 0.6f;
            colors[ci + 6] = baseColorCache[2] * 0.6f;
            colors[ci + 7] = 1.0f;

            // Top colors (más brillantes)
            colors[ci + 8] = topColorCache[0];
            colors[ci + 9] = topColorCache[1];
            colors[ci + 10] = topColorCache[2];
            colors[ci + 11] = 1.0f;

            colors[ci + 12] = topColorCache[0];
            colors[ci + 13] = topColorCache[1];
            colors[ci + 14] = topColorCache[2];
            colors[ci + 15] = 1.0f;
        }

        vertexBuffer.clear();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        colorBuffer.clear();
        colorBuffer.put(colors);
        colorBuffer.position(0);
    }

    /**
     * Actualiza la geometría del glow (resplandor detrás de las barras)
     * ⚡ OPTIMIZADO: Usa arrays cacheados en lugar de crear nuevos
     */
    private void updateGlowGeometry() {
        // ⚡ OPTIMIZADO: Usar caches en lugar de new float[]
        float[] vertices = glowVerticesCache;
        float[] colors = glowColorsCache;

        float totalWidth = aspectRatio * 2f * 0.92f;
        float barWidth = (totalWidth - (NUM_BARS - 1) * BAR_SPACING) / NUM_BARS;
        float glowWidth = barWidth * GLOW_WIDTH_MULT;
        float glowOffset = (glowWidth - barWidth) / 2f;
        float startX = -totalWidth / 2f;

        for (int i = 0; i < NUM_BARS; i++) {
            float x = startX + i * (barWidth + BAR_SPACING) - glowOffset;
            float level = barLevels[i];
            float height = (MIN_HEIGHT + level * (MAX_HEIGHT - MIN_HEIGHT)) * 1.15f;  // Glow un poco más alto

            int vi = i * 12;
            int ci = i * 16;

            // Vértices del glow
            vertices[vi + 0] = x;
            vertices[vi + 1] = BASE_Y - 0.01f;
            vertices[vi + 2] = 0f;

            vertices[vi + 3] = x + glowWidth;
            vertices[vi + 4] = BASE_Y - 0.01f;
            vertices[vi + 5] = 0f;

            vertices[vi + 6] = x;
            vertices[vi + 7] = BASE_Y + height;
            vertices[vi + 8] = 0f;

            vertices[vi + 9] = x + glowWidth;
            vertices[vi + 10] = BASE_Y + height;
            vertices[vi + 11] = 0f;

            // Colores del glow (semi-transparentes)
            // ⚡ OPTIMIZADO: Usar cache en lugar de crear array
            getBarColor(i, 0.6f + level * 0.4f, glowColorCache);
            float alpha = GLOW_INTENSITY * level;

            // Glow con transparencia en los bordes
            colors[ci + 0] = glowColorCache[0];
            colors[ci + 1] = glowColorCache[1];
            colors[ci + 2] = glowColorCache[2];
            colors[ci + 3] = alpha * 0.3f;  // Muy transparente abajo

            colors[ci + 4] = glowColorCache[0];
            colors[ci + 5] = glowColorCache[1];
            colors[ci + 6] = glowColorCache[2];
            colors[ci + 7] = alpha * 0.3f;

            colors[ci + 8] = glowColorCache[0];
            colors[ci + 9] = glowColorCache[1];
            colors[ci + 10] = glowColorCache[2];
            colors[ci + 11] = alpha * 0.6f;  // Más opaco arriba

            colors[ci + 12] = glowColorCache[0];
            colors[ci + 13] = glowColorCache[1];
            colors[ci + 14] = glowColorCache[2];
            colors[ci + 15] = alpha * 0.6f;
        }

        glowVertexBuffer.clear();
        glowVertexBuffer.put(vertices);
        glowVertexBuffer.position(0);

        glowColorBuffer.clear();
        glowColorBuffer.put(colors);
        glowColorBuffer.position(0);
    }

    /**
     * Actualiza la geometría de los peak markers con colores heredados de la barra
     * Rosa en el centro, Cyan en los lados - NO BLANCOS
     * ⚡ OPTIMIZADO: Usa arrays cacheados en lugar de crear nuevos
     */
    private void updatePeakGeometry() {
        // ⚡ OPTIMIZADO: Usar caches en lugar de new float[]
        float[] vertices = peakVerticesCache;
        float[] colors = peakColorsCache;

        float totalWidth = aspectRatio * 2f * 0.92f;
        float barWidth = (totalWidth - (NUM_BARS - 1) * BAR_SPACING) / NUM_BARS;
        float startX = -totalWidth / 2f;

        // Tiempo para efecto de pulso/brillo
        long safeTime = System.currentTimeMillis() % 5000;
        float pulse = 0.85f + 0.15f * (float)Math.sin(safeTime * 0.008f);

        for (int i = 0; i < NUM_BARS; i++) {
            float x = startX + i * (barWidth + BAR_SPACING);
            float peakY = BASE_Y + MIN_HEIGHT + peakLevels[i] * (MAX_HEIGHT - MIN_HEIGHT);

            // Peak delgado
            float peakHeight = PEAK_HEIGHT;

            int vi = i * 12;
            int ci = i * 16;

            // Peak marker como una línea horizontal
            vertices[vi + 0] = x;
            vertices[vi + 1] = peakY;
            vertices[vi + 2] = 0f;

            vertices[vi + 3] = x + barWidth;
            vertices[vi + 4] = peakY;
            vertices[vi + 5] = 0f;

            vertices[vi + 6] = x;
            vertices[vi + 7] = peakY + peakHeight;
            vertices[vi + 8] = 0f;

            vertices[vi + 9] = x + barWidth;
            vertices[vi + 10] = peakY + peakHeight;
            vertices[vi + 11] = 0f;

            // ═══════════════════════════════════════════════════════════════
            // COLOR DEL PEAK - Mismo gradiente que las barras (según tema)
            // ═══════════════════════════════════════════════════════════════
            float t = (float) i / (NUM_BARS - 1);  // 0 a 1
            float distFromCenter = Math.abs(t - 0.5f) * 2f;  // 0 = centro, 1 = lados

            // Interpolar entre bass (centro) y treble (lados) según tema
            float r = colorBass[0] + (colorTreble[0] - colorBass[0]) * distFromCenter;
            float g = colorBass[1] + (colorTreble[1] - colorBass[1]) * distFromCenter;
            float b = colorBass[2] + (colorTreble[2] - colorBass[2]) * distFromCenter;

            // Aplicar pulso y brillo
            r = r * pulse * 1.2f;
            g = g * pulse * 1.2f;
            b = b * pulse * 1.2f;

            // Clamp sin llegar a blanco
            r = Math.min(0.95f, r);
            g = Math.min(0.95f, g);
            b = Math.min(0.95f, b);

            float alpha = 0.95f;

            // Todos los vértices con el mismo color (sin gradiente vertical)
            for (int v = 0; v < 4; v++) {
                colors[ci + v * 4 + 0] = r;
                colors[ci + v * 4 + 1] = g;
                colors[ci + v * 4 + 2] = b;
                colors[ci + v * 4 + 3] = alpha;
            }
        }

        peakVertexBuffer.clear();
        peakVertexBuffer.put(vertices);
        peakVertexBuffer.position(0);

        peakColorBuffer.clear();
        peakColorBuffer.put(colors);
        peakColorBuffer.position(0);
    }

    public void setAspectRatio(float ratio) {
        this.aspectRatio = ratio;
    }

    public void setScreenSize(int width, int height) {
        this.aspectRatio = (float) width / height;
    }

    public boolean isInitialized() {
        return initialized;
    }

    // ════════════════════════════════════════════════════════════════════════
    // ⚡ DIBUJO DE RAYOS ELÉCTRICOS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Dibuja los rayos eléctricos zigzagueantes entre las barras
     */
    private void drawLightning() {
        if (lightningCount == 0) return;

        // Asegurar que el shader y matriz estén activos
        GLES30.glUseProgram(shaderProgram);
        GLES30.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, projectionMatrix, 0);

        // Usar blending aditivo para que los rayos brillen intensamente
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE);

        // Dibujar cada rayo como una serie de quads conectados (línea delgada)
        float lineThickness = 0.003f;  // Grosor del rayo (más fino)

        for (int l = 0; l < lightningCount; l++) {
            float life = lightningLife[l];
            float maxLife = 0.35f;  // Vida máxima aproximada
            float lifeRatio = Math.min(1.0f, life / maxLife);

            // Alpha basado en vida (fade out)
            float alpha = lifeRatio;

            // Color del rayo con brillo
            float r = lightningR[l];
            float g = lightningG[l];
            float b = lightningB[l];

            // Cada segmento del rayo es un quad
            // ⚡ OPTIMIZADO: Usar caches en lugar de new float[]
            float[] vertices = lightningVerticesCache;
            float[] colors = lightningColorsCache;

            int vi = 0;
            int ci = 0;

            for (int s = 0; s < LIGHTNING_SEGMENTS; s++) {
                float x1 = lightningPointsX[l][s];
                float y1 = lightningPointsY[l][s];
                float x2 = lightningPointsX[l][s + 1];
                float y2 = lightningPointsY[l][s + 1];

                // Calcular perpendicular para el grosor
                float dx = x2 - x1;
                float dy = y2 - y1;
                float len = (float) Math.sqrt(dx * dx + dy * dy);
                if (len < 0.001f) len = 0.001f;
                float perpX = (-dy / len) * lineThickness;
                float perpY = (dx / len) * lineThickness;

                // 4 vértices del quad (línea gruesa)
                vertices[vi++] = x1 - perpX; vertices[vi++] = y1 - perpY; vertices[vi++] = 0f;
                vertices[vi++] = x1 + perpX; vertices[vi++] = y1 + perpY; vertices[vi++] = 0f;
                vertices[vi++] = x2 - perpX; vertices[vi++] = y2 - perpY; vertices[vi++] = 0f;
                vertices[vi++] = x2 + perpX; vertices[vi++] = y2 + perpY; vertices[vi++] = 0f;

                // Colores con brillo en el centro (efecto glow)
                // Los vértices exteriores tienen el color base
                // Efecto de brillo: centro más blanco
                float centerBrightness = 0.5f + lifeRatio * 0.5f;

                colors[ci++] = r; colors[ci++] = g; colors[ci++] = b; colors[ci++] = alpha;
                colors[ci++] = Math.min(1f, r + centerBrightness);
                colors[ci++] = Math.min(1f, g + centerBrightness);
                colors[ci++] = Math.min(1f, b + centerBrightness);
                colors[ci++] = alpha;
                colors[ci++] = r; colors[ci++] = g; colors[ci++] = b; colors[ci++] = alpha;
                colors[ci++] = Math.min(1f, r + centerBrightness);
                colors[ci++] = Math.min(1f, g + centerBrightness);
                colors[ci++] = Math.min(1f, b + centerBrightness);
                colors[ci++] = alpha;
            }

            // Actualizar buffers
            lightningVertexBuffer.clear();
            lightningVertexBuffer.put(vertices);
            lightningVertexBuffer.position(0);

            lightningColorBuffer.clear();
            lightningColorBuffer.put(colors);
            lightningColorBuffer.position(0);

            // Dibujar
            GLES30.glEnableVertexAttribArray(aPositionHandle);
            GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, lightningVertexBuffer);

            GLES30.glEnableVertexAttribArray(aColorHandle);
            GLES30.glVertexAttribPointer(aColorHandle, 4, GLES30.GL_FLOAT, false, 0, lightningColorBuffer);

            for (int s = 0; s < LIGHTNING_SEGMENTS; s++) {
                GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, s * 4, 4);
            }

            GLES30.glDisableVertexAttribArray(aPositionHandle);
            GLES30.glDisableVertexAttribArray(aColorHandle);

            // === DIBUJAR GLOW (versión más grande y transparente) ===
            float glowThickness = lineThickness * 2.5f;

            vi = 0;
            ci = 0;

            for (int s = 0; s < LIGHTNING_SEGMENTS; s++) {
                float x1 = lightningPointsX[l][s];
                float y1 = lightningPointsY[l][s];
                float x2 = lightningPointsX[l][s + 1];
                float y2 = lightningPointsY[l][s + 1];

                float dx = x2 - x1;
                float dy = y2 - y1;
                float len = (float) Math.sqrt(dx * dx + dy * dy);
                if (len < 0.001f) len = 0.001f;
                float perpX = (-dy / len) * glowThickness;
                float perpY = (dx / len) * glowThickness;

                vertices[vi++] = x1 - perpX; vertices[vi++] = y1 - perpY; vertices[vi++] = 0f;
                vertices[vi++] = x1 + perpX; vertices[vi++] = y1 + perpY; vertices[vi++] = 0f;
                vertices[vi++] = x2 - perpX; vertices[vi++] = y2 - perpY; vertices[vi++] = 0f;
                vertices[vi++] = x2 + perpX; vertices[vi++] = y2 + perpY; vertices[vi++] = 0f;

                // Glow con alpha bajo
                float glowAlpha = alpha * 0.3f;
                for (int v = 0; v < 4; v++) {
                    colors[ci++] = r;
                    colors[ci++] = g;
                    colors[ci++] = b;
                    colors[ci++] = glowAlpha;
                }
            }

            // Dibujar glow
            lightningVertexBuffer.clear();
            lightningVertexBuffer.put(vertices);
            lightningVertexBuffer.position(0);

            lightningColorBuffer.clear();
            lightningColorBuffer.put(colors);
            lightningColorBuffer.position(0);

            GLES30.glEnableVertexAttribArray(aPositionHandle);
            GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, lightningVertexBuffer);

            GLES30.glEnableVertexAttribArray(aColorHandle);
            GLES30.glVertexAttribPointer(aColorHandle, 4, GLES30.GL_FLOAT, false, 0, lightningColorBuffer);

            for (int s = 0; s < LIGHTNING_SEGMENTS; s++) {
                GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, s * 4, 4);
            }

            GLES30.glDisableVertexAttribArray(aPositionHandle);
            GLES30.glDisableVertexAttribArray(aColorHandle);
        }

        // Restaurar blending normal
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 🌊 DIBUJO DE ONDAS DE ENERGÍA (ARCOS GRUESOS)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Dibuja las ondas de energía como arcos semicirculares gruesos
     */
    private void drawEnergyWaves() {
        // Contar ondas activas
        int activeWaves = 0;
        for (int i = 0; i < MAX_WAVES; i++) {
            if (waveAlpha[i] > 0.01f) activeWaves++;
        }
        if (activeWaves == 0) return;

        // Asegurar shader y matriz activos
        GLES30.glUseProgram(shaderProgram);
        GLES30.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, projectionMatrix, 0);

        // Usar blending aditivo para ondas brillantes
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE);

        // Centro de las ondas (base de las barras)
        float centerX = 0f;
        float centerY = BASE_Y + 0.08f;

        // Dibujar cada onda como un arco grueso (quad strip)
        for (int w = 0; w < MAX_WAVES; w++) {
            if (waveAlpha[w] <= 0.01f) continue;

            float radius = waveRadius[w];
            float alpha = waveAlpha[w];

            // Calcular fade basado en radio
            float fadeFactor = 1.0f - (radius / WAVE_MAX_RADIUS);
            float finalAlpha = alpha * fadeFactor * 0.8f;

            // Radio interno y externo para el arco grueso
            float innerRadius = radius;
            float outerRadius = radius + WAVE_THICKNESS;

            // Crear arco semicircular (solo parte superior)
            // ⚡ OPTIMIZADO: Usar caches en lugar de new float[]
            int numSegments = WAVE_SEGMENTS_DRAW;  // 24 segmentos (constante)
            float[] vertices = waveVerticesCache;
            float[] colors = waveColorsCache;

            int vi = 0;
            int ci = 0;

            for (int s = 0; s < numSegments; s++) {
                // Ángulos para el arco (0 a PI = semicírculo superior)
                float angle1 = (float)(s * Math.PI / numSegments);
                float angle2 = (float)((s + 1) * Math.PI / numSegments);

                // Puntos del quad (inner y outer radius)
                float x1Inner = centerX + (float)Math.cos(angle1) * innerRadius * aspectRatio * 1.5f;
                float y1Inner = centerY + (float)Math.sin(angle1) * innerRadius;
                float x1Outer = centerX + (float)Math.cos(angle1) * outerRadius * aspectRatio * 1.5f;
                float y1Outer = centerY + (float)Math.sin(angle1) * outerRadius;

                float x2Inner = centerX + (float)Math.cos(angle2) * innerRadius * aspectRatio * 1.5f;
                float y2Inner = centerY + (float)Math.sin(angle2) * innerRadius;
                float x2Outer = centerX + (float)Math.cos(angle2) * outerRadius * aspectRatio * 1.5f;
                float y2Outer = centerY + (float)Math.sin(angle2) * outerRadius;

                // Quad como triangle strip (4 vértices)
                vertices[vi++] = x1Inner; vertices[vi++] = y1Inner; vertices[vi++] = 0f;
                vertices[vi++] = x1Outer; vertices[vi++] = y1Outer; vertices[vi++] = 0f;
                vertices[vi++] = x2Inner; vertices[vi++] = y2Inner; vertices[vi++] = 0f;
                vertices[vi++] = x2Outer; vertices[vi++] = y2Outer; vertices[vi++] = 0f;

                // Colores - borde interno más brillante, externo con fade
                float r = waveR[w];
                float g = waveG[w];
                float b = waveB[w];

                // Inner vertices - más brillantes
                colors[ci++] = r; colors[ci++] = g; colors[ci++] = b; colors[ci++] = finalAlpha;
                // Outer vertices - fade
                colors[ci++] = r; colors[ci++] = g; colors[ci++] = b; colors[ci++] = finalAlpha * 0.3f;
                colors[ci++] = r; colors[ci++] = g; colors[ci++] = b; colors[ci++] = finalAlpha;
                colors[ci++] = r; colors[ci++] = g; colors[ci++] = b; colors[ci++] = finalAlpha * 0.3f;
            }

            // Actualizar buffers
            waveVertexBuffer.clear();
            waveVertexBuffer.put(vertices);
            waveVertexBuffer.position(0);

            waveColorBuffer.clear();
            waveColorBuffer.put(colors);
            waveColorBuffer.position(0);

            // Dibujar quads
            GLES30.glEnableVertexAttribArray(aPositionHandle);
            GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, waveVertexBuffer);

            GLES30.glEnableVertexAttribArray(aColorHandle);
            GLES30.glVertexAttribPointer(aColorHandle, 4, GLES30.GL_FLOAT, false, 0, waveColorBuffer);

            // Dibujar cada segmento como triangle strip
            for (int s = 0; s < numSegments; s++) {
                GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, s * 4, 4);
            }

            GLES30.glDisableVertexAttribArray(aPositionHandle);
            GLES30.glDisableVertexAttribArray(aColorHandle);
        }

        // Restaurar blending normal
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
    }

    // ════════════════════════════════════════════════════════════════════════
    // ✨ DIBUJO DE CHISPAS ENTRE PEAKS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Dibuja las chispas eléctricas entre peaks cercanos
     * Cada chispa es un pequeño zigzag de 3 segmentos que conecta dos peaks
     */
    private void drawPeakSparks() {
        if (sparkCount == 0) return;

        // Asegurar shader y matriz activos
        GLES30.glUseProgram(shaderProgram);
        GLES30.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, projectionMatrix, 0);

        // Usar blending aditivo para chispas brillantes
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE);

        float sparkThickness = 0.002f;  // Grosor de la chispa (muy fina)

        for (int sp = 0; sp < sparkCount; sp++) {
            float life = sparkLife[sp];
            float maxLife = 0.14f;  // Vida máxima aproximada
            float lifeRatio = Math.min(1.0f, life / maxLife);

            // Alpha basado en vida (fade out rápido)
            float alpha = lifeRatio * 0.9f;

            // Color de la chispa
            float r = sparkR[sp];
            float g = sparkG[sp];
            float b = sparkB[sp];

            // Posiciones inicio y fin
            float x1 = sparkStartX[sp];
            float y1 = sparkStartY[sp];
            float x2 = sparkEndX[sp];
            float y2 = sparkEndY[sp];

            // Generar zigzag de 3 segmentos (4 puntos)
            // ⚡ OPTIMIZADO: Usar caches en lugar de new float[]
            float[] pointsX = sparkPointsXCache;
            float[] pointsY = sparkPointsYCache;

            pointsX[0] = x1;
            pointsY[0] = y1;
            pointsX[3] = x2;
            pointsY[3] = y2;

            // Puntos intermedios con desplazamiento aleatorio para zigzag
            float dx = x2 - x1;
            float dy = y2 - y1;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len < 0.001f) len = 0.001f;

            // Perpendicular para desplazamiento
            float perpX = -dy / len;
            float perpY = dx / len;

            // Offset aleatorio pero consistente durante la vida de la chispa
            // Usar la vida como seed para que el zigzag cambie mientras vive
            float offset1 = (float) Math.sin(life * 50f) * 0.03f;
            float offset2 = (float) Math.cos(life * 70f) * 0.03f;

            // Puntos 1 y 2 (intermedios)
            pointsX[1] = x1 + dx * 0.33f + perpX * offset1;
            pointsY[1] = y1 + dy * 0.33f + perpY * offset1;
            pointsX[2] = x1 + dx * 0.66f + perpX * offset2;
            pointsY[2] = y1 + dy * 0.66f + perpY * offset2;

            // Crear quads para los 3 segmentos
            // ⚡ OPTIMIZADO: Usar caches en lugar de new float[]
            int numSegments = 3;
            float[] vertices = sparkVerticesCache;
            float[] colors = sparkColorsCache;

            int vi = 0;
            int ci = 0;

            for (int s = 0; s < numSegments; s++) {
                float sx1 = pointsX[s];
                float sy1 = pointsY[s];
                float sx2 = pointsX[s + 1];
                float sy2 = pointsY[s + 1];

                // Calcular perpendicular para el grosor
                float sdx = sx2 - sx1;
                float sdy = sy2 - sy1;
                float slen = (float) Math.sqrt(sdx * sdx + sdy * sdy);
                if (slen < 0.001f) slen = 0.001f;
                float sperpX = (-sdy / slen) * sparkThickness;
                float sperpY = (sdx / slen) * sparkThickness;

                // 4 vértices del quad
                vertices[vi++] = sx1 - sperpX; vertices[vi++] = sy1 - sperpY; vertices[vi++] = 0f;
                vertices[vi++] = sx1 + sperpX; vertices[vi++] = sy1 + sperpY; vertices[vi++] = 0f;
                vertices[vi++] = sx2 - sperpX; vertices[vi++] = sy2 - sperpY; vertices[vi++] = 0f;
                vertices[vi++] = sx2 + sperpX; vertices[vi++] = sy2 + sperpY; vertices[vi++] = 0f;

                // Colores con brillo en el centro
                float brightness = 0.4f + lifeRatio * 0.6f;
                for (int v = 0; v < 4; v++) {
                    colors[ci++] = Math.min(1f, r + brightness * 0.3f);
                    colors[ci++] = Math.min(1f, g + brightness * 0.3f);
                    colors[ci++] = Math.min(1f, b + brightness * 0.3f);
                    colors[ci++] = alpha;
                }
            }

            // Actualizar buffers
            sparkVertexBuffer.clear();
            sparkVertexBuffer.put(vertices);
            sparkVertexBuffer.position(0);

            sparkColorBuffer.clear();
            sparkColorBuffer.put(colors);
            sparkColorBuffer.position(0);

            // Dibujar
            GLES30.glEnableVertexAttribArray(aPositionHandle);
            GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, sparkVertexBuffer);

            GLES30.glEnableVertexAttribArray(aColorHandle);
            GLES30.glVertexAttribPointer(aColorHandle, 4, GLES30.GL_FLOAT, false, 0, sparkColorBuffer);

            for (int s = 0; s < numSegments; s++) {
                GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, s * 4, 4);
            }

            GLES30.glDisableVertexAttribArray(aPositionHandle);
            GLES30.glDisableVertexAttribArray(aColorHandle);
        }

        // Restaurar blending normal
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * Limpieza - desregistrarse del AspectRatioManager
     */
    public void release() {
        AspectRatioManager.get().unregister(this);
        Log.d(TAG, "🎵 EqualizerBarsDJ liberado");
    }
}
