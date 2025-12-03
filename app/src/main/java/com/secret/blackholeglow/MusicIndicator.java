package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/**
 * Indicador visual de música en tiempo real - ESTILO LED BARS
 * Muestra múltiples barras verticales con gradiente de color (rojo→amarillo→verde)
 * Similar a ecualizadores LED profesionales
 */
public class MusicIndicator implements SceneObject {
    private static final String TAG = "depurar";

    // Configuración del ecualizador - 7 BARRAS POR RANGOS DE FRECUENCIA
    private static final int NUM_BARRAS = 7;  // 7 barras para visualización óptima (equilibrio perfecto)
    private static final int LEDS_POR_BARRA = 6;  // 6 LEDs por barra (balance visual/rendimiento)
    private static final float SMOOTHING_FACTOR = 0.5f;  // Factor de suavizado (más responsivo al beat)

    // ════════════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN DE PEAK HOLDERS (Picos flotantes) 🔺
    // ════════════════════════════════════════════════════════════════════════
    private static final float PEAK_FALL_SPEED = 0.4f;   // Velocidad de caída del pico (lento para efecto dramático)
    private static final float PEAK_HOLD_TIME = 0.3f;    // Tiempo que el pico se mantiene arriba antes de caer
    private static final float MIN_BAR_LEVEL = 0.15f;    // Nivel mínimo SIEMPRE visible (15%)

    // ════════════════════════════════════════════════════════════════════════
    // ESCALA DE BARRAS (de grande a pequeño: bass → treble)
    // Afecta ANCHO y ALTO proporcionalmente
    // ════════════════════════════════════════════════════════════════════════
    private static final float[] BAR_WIDTH_SCALE = {1.6f, 1.35f, 1.15f, 1.0f, 0.85f, 0.7f, 0.55f};
    // Altura decreciente normal (bass grande → treble pequeño)
    private static final float[] BAR_HEIGHT_SCALE = {1.0f, 0.92f, 0.84f, 0.76f, 0.68f, 0.60f, 0.52f};

    // ════════════════════════════════════════════════════════════════════════
    // AMPLIFICACIÓN por barra - Barras 4-6 SUPER REACTIVAS
    // ════════════════════════════════════════════════════════════════════════
    private static final float[] BAR_SENSITIVITY = {1.2f, 1.4f, 2.0f, 3.0f, 7.0f, 9.0f, 12.0f};

    // Mínimo de LEDs cuando NO hay música (solo 1 para que se vea la barra)
    private static final int MIN_LEDS_NO_MUSIC = 1;
    // Mínimo de LEDs cuando HAY música (más activo)
    private static final int MIN_LEDS_WITH_MUSIC = 2;

    // ════════════════════════════════════════════════════════════════════════
    // DETECCIÓN DE BEAT - Boost global cuando hay golpe fuerte
    // ════════════════════════════════════════════════════════════════════════
    private static final float BEAT_THRESHOLD = 0.4f;    // Umbral para detectar beat
    private static final float BEAT_BOOST = 0.35f;       // Boost que reciben TODAS las barras
    private static final float BEAT_DECAY = 3.0f;        // Velocidad de decaimiento del boost

    // Peak holder ALEJADO de los LEDs para ver su caída
    private static final float PEAK_OFFSET = 0.06f;  // Distancia visible sobre los LEDs

    // ════════════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN DE CHISPAS MÁGICAS ✨
    // Solo salen cuando HAY MÚSICA REAL (no con niveles mínimos)
    // ════════════════════════════════════════════════════════════════════════
    private static final float SPARK_THRESHOLD = 0.45f;  // 45% - Solo con música real, no con mínimos
    private static final float SPARK_SPEED = 0.3f;       // Velocidad de subida (NDC/segundo)
    private static final float SPARK_LIFETIME = 1.5f;    // Duración en segundos
    private static final float SPARK_SIZE = 0.006f;      // Tamaño de chispa
    private static final float SPARK_COOLDOWN = 0.15f;   // Cooldown entre emisiones
    private static final int MAX_SPARKS_PER_TRIGGER = 2; // Base de chispas (se multiplica por intensidad)

    private int programId;

    private int aPositionLoc;
    private int aColorLoc;

    // ════════════════════════════════════════════════════════════════════════
    // BUFFERS REUTILIZABLES (OPTIMIZACIÓN - evita allocations cada frame)
    // ════════════════════════════════════════════════════════════════════════
    private FloatBuffer reusableVertexBuffer;
    private FloatBuffer reusableColorBuffer;
    private final float[] tempVertices = new float[8];   // 4 vértices × 2 coords
    private final float[] tempColors = new float[16];    // 4 vértices × 4 RGBA
    private final float[] tempLedColor = new float[4];   // Color LED reutilizable

    // ════════════════════════════════════════════════════════════════════════
    // GEOMETRÍA PRE-CALCULADA (calculada UNA vez en constructor)
    // ════════════════════════════════════════════════════════════════════════
    private final float[] barXPositions = new float[NUM_BARRAS];  // Posición X de cada barra
    private final float[] barWidths = new float[NUM_BARRAS];      // Ancho de cada barra
    private final float[] barHeights = new float[NUM_BARRAS];     // Alto máximo de cada barra
    private float ledBaseHeight;  // Alto base de un LED

    // Posición y tamaño del indicador (coordenadas NDC 2D)
    private final float x;
    private final float y;
    private final float width;   // Ancho total del ecualizador
    private final float height;  // Alto total del ecualizador

    // Niveles de música (0-1) - Uno por cada barra
    private float[] barLevels = new float[NUM_BARRAS];

    // Suavizado independiente por barra
    private float[] smoothedLevels = new float[NUM_BARRAS];

    // ════════════════════════════════════════════════════════════════════════
    // PEAK HOLDERS - Picos flotantes que caen lentamente 🔺
    // ════════════════════════════════════════════════════════════════════════
    private float[] peakLevels = new float[NUM_BARRAS];      // Nivel actual del pico
    private float[] peakHoldTimers = new float[NUM_BARRAS];  // Tiempo restante de "hold"

    // ════════════════════════════════════════════════════════════════════════
    // RANGOS DE FRECUENCIA POR BARRA (Hz) - 7 BANDAS ÓPTIMAS
    // ════════════════════════════════════════════════════════════════════════
    // Barra 0: SUB-BASS    60-250 Hz     (Bombo, bajo profundo) 🥁
    // Barra 1: BASS        250-500 Hz    (Bajo, guitarra baja) 🎸
    // Barra 2: MID-LOW     500-1000 Hz   (Voces graves masculinas) 🎤
    // Barra 3: MID         1000-2000 Hz  (Piano, guitarra, voces) 🎹
    // Barra 4: MID-HIGH    2000-4000 Hz  (Voces agudas, claridad) 👩‍🎤
    // Barra 5: TREBLE      4000-8000 Hz  (Violín, brillo) 🎻
    // Barra 6: AIR         8000-16000 Hz (Platillos, aire, espacio) ✨
    // ════════════════════════════════════════════════════════════════════════

    private float bassLevel = 0f;
    private float midLevel = 0f;
    private float trebleLevel = 0f;

    // ════════════════════════════════════════════════════════════════════════
    // SISTEMA DE BEAT DETECTION
    // ════════════════════════════════════════════════════════════════════════
    private float lastBassLevel = 0f;       // Para detectar cambios bruscos
    private float currentBeatBoost = 0f;    // Boost activo actual
    private float energyHistory = 0f;       // Historial de energía para detección
    private boolean musicPlaying = false;   // ¿Hay música reproduciéndose?

    // Contador de frames para logs
    private int frameCount = 0;

    // ════════════════════════════════════════════════════════════════════════
    // SISTEMA DE PARTÍCULAS (CHISPAS MÁGICAS) ✨
    // ════════════════════════════════════════════════════════════════════════
    private static class Spark {
        float x, y;           // Posición actual
        float vx, vy;         // Velocidad
        float[] color;        // Color RGBA
        float age;            // Edad en segundos
        float lifetime;       // Duración total
        float peakY;          // Posición Y del peak de su barra (para detectar cuando lo pasa)
        boolean passedPeak;   // ¿Ya pasó el peak?
        boolean exploded;     // ¿Ya explotó?
    }

    // Mini-explosiones cuando chispa pasa el peak
    private static class MiniExplosion {
        float x, y;
        float age;
        float[] color;
        float size;
    }
    private ArrayList<MiniExplosion> explosions = new ArrayList<>();

    private ArrayList<Spark> sparks = new ArrayList<>();
    private float[] barCooldowns = new float[NUM_BARRAS];  // Cooldown por barra
    private Random random = new Random();

    public MusicIndicator(Context context, float x, float y, float width, float height) {
        Log.d(TAG, "╔══════════════════════════════════════════════╗");
        Log.d(TAG, "║      CREANDO LED MUSIC EQUALIZER            ║");
        Log.d(TAG, "╚══════════════════════════════════════════════╝");

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        Log.d(TAG, "[MusicIndicator] Posición: (" + x + ", " + y + ")");
        Log.d(TAG, "[MusicIndicator] Tamaño: " + width + " x " + height);
        Log.d(TAG, "[MusicIndicator] Barras: " + NUM_BARRAS + " x " + LEDS_POR_BARRA + " LEDs");

        // ════════════════════════════════════════════════════════════════════════
        // PRE-CALCULAR GEOMETRÍA (UNA SOLA VEZ - no cada frame)
        // ════════════════════════════════════════════════════════════════════════
        float totalWidthScale = 0f;
        for (int i = 0; i < NUM_BARRAS; i++) {
            totalWidthScale += BAR_WIDTH_SCALE[i];
        }
        float baseBarWidth = width / totalWidthScale;
        ledBaseHeight = height / LEDS_POR_BARRA;

        float currentX = x;
        for (int i = 0; i < NUM_BARRAS; i++) {
            barXPositions[i] = currentX;
            barWidths[i] = baseBarWidth * BAR_WIDTH_SCALE[i];
            barHeights[i] = height * BAR_HEIGHT_SCALE[i];  // Alto variable por barra
            currentX += barWidths[i];
        }
        Log.d(TAG, "[MusicIndicator] ✓ Geometría pre-calculada");

        // OPTIMIZACIÓN: Crear buffers reutilizables UNA SOLA VEZ
        ByteBuffer vbb = ByteBuffer.allocateDirect(8 * 4);  // 8 floats × 4 bytes
        vbb.order(ByteOrder.nativeOrder());
        reusableVertexBuffer = vbb.asFloatBuffer();

        ByteBuffer cbb = ByteBuffer.allocateDirect(16 * 4); // 16 floats × 4 bytes
        cbb.order(ByteOrder.nativeOrder());
        reusableColorBuffer = cbb.asFloatBuffer();

        initShader(context);

        Log.d(TAG, "[MusicIndicator] ✓ Constructor completado (SUPER-OPTIMIZADO)");
    }

    private void initShader(Context context) {
        Log.d(TAG, "[MusicIndicator] Iniciando shader LED...");

        // Vertex shader simple para barras 2D
        String vertexShader =
            "attribute vec2 a_Position;\n" +
            "attribute vec4 a_Color;\n" +
            "varying vec4 v_Color;\n" +
            "void main() {\n" +
            "    v_Color = a_Color;\n" +
            "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
            "}\n";

        // Fragment shader con efecto NEÓN GLOW épico ✨
        String fragmentShader =
            "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
            "varying vec4 v_Color;\n" +
            "\n" +
            "void main() {\n" +
            "    vec4 color = v_Color;\n" +
            "    \n" +
            "    // EFECTO NEÓN: Aumentar saturación y brillo para LEDs encendidos\n" +
            "    if (color.a > 0.5) {\n" +
            "        // Boost de brillo neón (1.5x más brillante)\n" +
            "        color.rgb *= 1.5;\n" +
            "        // Añadir bloom/glow sutil\n" +
            "        float glow = 0.3;\n" +
            "        color.rgb = mix(color.rgb, vec3(1.0), glow * color.a);\n" +
            "    }\n" +
            "    \n" +
            "    gl_FragColor = color;\n" +
            "}\n";

        Log.d(TAG, "[MusicIndicator] Compilando shaders...");
        programId = ShaderUtils.createProgram(vertexShader, fragmentShader);

        if (programId == 0) {
            Log.e(TAG, "[MusicIndicator] ✗✗✗ ERROR CRÍTICO: Shader NO se pudo crear!");
            Log.e(TAG, "[MusicIndicator] Verifica los logs anteriores de ShaderUtils para detalles");
            return;
        }

        aPositionLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        aColorLoc = GLES20.glGetAttribLocation(programId, "a_Color");

        if (aPositionLoc == -1 || aColorLoc == -1) {
            Log.e(TAG, "[MusicIndicator] ✗ Error: No se encontraron los atributos del shader");
            Log.e(TAG, "[MusicIndicator]   aPositionLoc: " + aPositionLoc);
            Log.e(TAG, "[MusicIndicator]   aColorLoc: " + aColorLoc);
        }

        Log.d(TAG, "[MusicIndicator] ✓✓✓ Shader LED inicializado");
        Log.d(TAG, "[MusicIndicator]   programId: " + programId);
        Log.d(TAG, "[MusicIndicator]   aPositionLoc: " + aPositionLoc);
        Log.d(TAG, "[MusicIndicator]   aColorLoc: " + aColorLoc);
    }

    /**
     * Actualiza los niveles de música con distribución de 7 bandas de frecuencia
     * NUEVO: Usa las 32 bandas del MusicVisualizer para mayor precisión
     */
    public void updateMusicLevels(float bass, float mid, float treble) {
        this.bassLevel = bass;
        this.midLevel = mid;
        this.trebleLevel = treble;

        // ════════════════════════════════════════════════════════════════════════
        // DETECCIÓN DE BEAT - Detecta golpes fuertes de bajo/batería
        // ════════════════════════════════════════════════════════════════════════
        float totalEnergy = bass + mid * 0.5f + treble * 0.3f;
        float bassJump = bass - lastBassLevel;

        if (bassJump > BEAT_THRESHOLD || (totalEnergy - energyHistory) > BEAT_THRESHOLD * 1.5f) {
            currentBeatBoost = BEAT_BOOST;
        }

        lastBassLevel = bass;
        energyHistory = energyHistory * 0.9f + totalEnergy * 0.1f;
        musicPlaying = totalEnergy > 0.08f;

        // ════════════════════════════════════════════════════════════════════════
        // DISTRIBUCIÓN MEJORADA - Más variación entre barras
        // ════════════════════════════════════════════════════════════════════════

        // Barra 0: SUB-BASS - Solo bass puro (kick drum)
        barLevels[0] = bass * 0.95f;

        // Barra 1: BASS - Bass con algo de mid
        barLevels[1] = bass * 0.7f + mid * 0.2f;

        // Barra 2: LOW-MID - Transición bass a mid
        barLevels[2] = bass * 0.3f + mid * 0.5f;

        // Barra 3: MID - Medios puros (voces)
        barLevels[3] = mid * 0.85f;

        // Barra 4: HIGH-MID - Mid con treble
        barLevels[4] = mid * 0.4f + treble * 0.5f;

        // Barra 5: LOW-TREBLE - Más treble
        barLevels[5] = mid * 0.15f + treble * 0.75f;

        // Barra 6: HIGH-TREBLE - Treble puro (hi-hats, cymbals)
        barLevels[6] = treble * 0.9f;

        // ════════════════════════════════════════════════════════════════════════
        // APLICAR SENSIBILIDAD DIFERENCIADA
        // ════════════════════════════════════════════════════════════════════════
        for (int i = 0; i < NUM_BARRAS; i++) {
            barLevels[i] = barLevels[i] * BAR_SENSITIVITY[i];

            // Beat boost más sutil
            float boostMultiplier = 1.0f + (i * 0.1f);
            barLevels[i] += currentBeatBoost * boostMultiplier * 0.5f;

            // Mínimo solo cuando hay música
            if (musicPlaying) {
                float minLevel = 0.08f + (i * 0.01f);  // Mínimo variable
                barLevels[i] = Math.max(barLevels[i], minLevel);
            }

            barLevels[i] = Math.min(0.95f, barLevels[i]);  // No llegar al 100%
        }
    }

    /**
     * 🎵 NUEVO: Actualiza usando las 32 bandas de frecuencia directamente
     * Proporciona variación mucho más realista entre barras
     */
    public void updateFromBands(float[] bands) {
        if (bands == null || bands.length < 7) return;

        // ════════════════════════════════════════════════════════════════════════
        // MAPEAR 32 BANDAS → 7 BARRAS (seleccionar bandas representativas)
        // Esto da variación REAL porque cada barra usa bandas diferentes
        // ════════════════════════════════════════════════════════════════════════

        // Barra 0: SUB-BASS (bandas 0-3) - ~20-80 Hz
        barLevels[0] = (bands[0] + bands[1] + bands[2] + bands[3]) / 4f;

        // Barra 1: BASS (bandas 4-7) - ~80-200 Hz
        barLevels[1] = (bands[4] + bands[5] + bands[6] + bands[7]) / 4f;

        // Barra 2: LOW-MID (bandas 8-11) - ~200-500 Hz
        barLevels[2] = (bands[8] + bands[9] + bands[10] + bands[11]) / 4f;

        // Barra 3: MID (bandas 12-16) - ~500-1500 Hz
        barLevels[3] = (bands[12] + bands[13] + bands[14] + bands[15] + bands[16]) / 5f;

        // Barra 4: HIGH-MID (bandas 17-21) - ~1500-4000 Hz
        barLevels[4] = (bands[17] + bands[18] + bands[19] + bands[20] + bands[21]) / 5f;

        // Barra 5: LOW-TREBLE (bandas 22-26) - ~4000-8000 Hz
        barLevels[5] = (bands[22] + bands[23] + bands[24] + bands[25] + bands[26]) / 5f;

        // Barra 6: HIGH-TREBLE (bandas 27-31) - ~8000-16000 Hz
        barLevels[6] = (bands[27] + bands[28] + bands[29] + bands[30] + bands[31]) / 5f;

        // Detectar música
        float totalEnergy = 0;
        for (int i = 0; i < NUM_BARRAS; i++) totalEnergy += barLevels[i];
        musicPlaying = totalEnergy > 0.3f;

        // Beat detection basado en sub-bass
        float bassJump = barLevels[0] - lastBassLevel;
        if (bassJump > 0.15f) {
            currentBeatBoost = BEAT_BOOST;
        }
        lastBassLevel = barLevels[0];

        // Aplicar sensibilidad por barra (las treble necesitan más boost)
        float[] bandSensitivity = {0.9f, 1.0f, 1.1f, 1.2f, 1.4f, 1.6f, 1.8f};
        for (int i = 0; i < NUM_BARRAS; i++) {
            barLevels[i] *= bandSensitivity[i];
            barLevels[i] += currentBeatBoost * (0.5f + i * 0.1f);

            // Mínimo sutil cuando hay música
            if (musicPlaying) {
                barLevels[i] = Math.max(barLevels[i], 0.05f);
            }

            // Limitar para que no todas lleguen al máximo
            barLevels[i] = Math.min(0.92f, barLevels[i]);
        }

        // Log reducido
        if (frameCount % 300 == 0 && musicPlaying) {
            Log.d(TAG, String.format("[EQ] Barras: %.2f %.2f %.2f %.2f %.2f %.2f %.2f",
                    barLevels[0], barLevels[1], barLevels[2], barLevels[3],
                    barLevels[4], barLevels[5], barLevels[6]));
        }
    }

    @Override
    public void update(float deltaTime) {
        frameCount++;

        // ════════════════════════════════════════════════════════════════════════
        // DECAER BEAT BOOST
        // ════════════════════════════════════════════════════════════════════════
        if (currentBeatBoost > 0) {
            currentBeatBoost -= BEAT_DECAY * deltaTime;
            if (currentBeatBoost < 0) currentBeatBoost = 0;
        }

        // Suavizar cada barra con respuesta MÁS RÁPIDA
        float fastSmoothing = 0.3f;  // Más reactivo que antes
        int minLeds = musicPlaying ? MIN_LEDS_WITH_MUSIC : MIN_LEDS_NO_MUSIC;
        float minLevel = (float) minLeds / LEDS_POR_BARRA;

        for (int i = 0; i < NUM_BARRAS; i++) {
            smoothedLevels[i] = smoothedLevels[i] * fastSmoothing + barLevels[i] * (1f - fastSmoothing);
            smoothedLevels[i] = Math.max(smoothedLevels[i], minLevel);
        }

        // ════════════════════════════════════════════════════════════════════════
        // ACTUALIZAR PEAK HOLDERS 🌈 (peaks caen hasta el nivel del LED)
        // ════════════════════════════════════════════════════════════════════════
        for (int i = 0; i < NUM_BARRAS; i++) {
            float currentLevel = smoothedLevels[i];

            // Si el nivel actual supera el pico, actualizar el pico
            if (currentLevel >= peakLevels[i]) {
                peakLevels[i] = currentLevel;
                peakHoldTimers[i] = PEAK_HOLD_TIME;
            } else {
                if (peakHoldTimers[i] > 0) {
                    peakHoldTimers[i] -= deltaTime;
                } else {
                    // Caer hacia el nivel actual del LED (no quedarse flotando)
                    peakLevels[i] -= PEAK_FALL_SPEED * deltaTime;
                    // El peak NO puede estar por debajo del nivel actual del LED
                    peakLevels[i] = Math.max(peakLevels[i], currentLevel);
                }
            }
        }

        // ════════════════════════════════════════════════════════════════════════
        // SISTEMA DE CHISPAS MÁGICAS ✨
        // ════════════════════════════════════════════════════════════════════════

        // Actualizar cooldowns
        for (int i = 0; i < NUM_BARRAS; i++) {
            if (barCooldowns[i] > 0) {
                barCooldowns[i] -= deltaTime;
            }
        }

        // ════════════════════════════════════════════════════════════════════════
        // GENERAR CHISPAS SOLO CON MÚSICA REAL
        // ════════════════════════════════════════════════════════════════════════
        // Verificar si hay música real (no solo niveles mínimos)
        float totalMusicEnergy = bassLevel + midLevel + trebleLevel;
        boolean hasMusicPlaying = totalMusicEnergy > 0.15f;  // Umbral para detectar música real

        for (int barIndex = 0; barIndex < NUM_BARRAS; barIndex++) {
            float level = smoothedLevels[barIndex];
            float sparkMinLevel = (float) MIN_LEDS_WITH_MUSIC / LEDS_POR_BARRA;

            // SOLO generar chispas si:
            // 1. Hay música reproduciéndose
            // 2. El nivel está por encima del mínimo + threshold
            // 3. El cooldown terminó
            boolean realActivity = level > (sparkMinLevel + 0.1f) && hasMusicPlaying;

            if (realActivity && level >= SPARK_THRESHOLD && barCooldowns[barIndex] <= 0) {
                // Cantidad de chispas proporcional a la intensidad
                int intensityBonus = (int)((level - SPARK_THRESHOLD) * 5);  // 0-2 extra según intensidad
                int numSparks = MAX_SPARKS_PER_TRIGGER + intensityBonus;
                numSparks = Math.min(numSparks, 5);  // Máximo 5 chispas

                for (int s = 0; s < numSparks; s++) {
                    Spark spark = new Spark();

                    // OPTIMIZADO: Usar geometría pre-calculada
                    float barX = barXPositions[barIndex];
                    float barW = barWidths[barIndex];
                    float barH = barHeights[barIndex];
                    float ledH = barH / LEDS_POR_BARRA;

                    int ledsEncendidos = (int)(level * LEDS_POR_BARRA);
                    float topLedY = y + ledsEncendidos * ledH;

                    spark.x = barX + barW * 0.5f + (random.nextFloat() - 0.5f) * barW * 0.3f;
                    spark.y = topLedY;

                    // Velocidad: sube lentamente con ligera variación horizontal
                    spark.vx = (random.nextFloat() - 0.5f) * 0.05f;
                    spark.vy = SPARK_SPEED + random.nextFloat() * 0.1f;

                    // Guardar posición del peak para detectar cuando lo pasa
                    spark.peakY = y + peakLevels[barIndex] * barH + PEAK_OFFSET;
                    spark.passedPeak = false;
                    spark.exploded = false;

                    // Color: mismo que el LED superior de esta barra
                    int topLedIndex = Math.min(ledsEncendidos - 1, LEDS_POR_BARRA - 1);
                    spark.color = getLedColor(topLedIndex, LEDS_POR_BARRA, true);

                    spark.age = 0f;
                    spark.lifetime = SPARK_LIFETIME;

                    sparks.add(spark);
                }

                // Reiniciar cooldown
                barCooldowns[barIndex] = SPARK_COOLDOWN;
            }
        }

        // Actualizar partículas existentes
        Iterator<Spark> iterator = sparks.iterator();
        while (iterator.hasNext()) {
            Spark spark = iterator.next();
            spark.age += deltaTime;

            // Mover la chispa
            spark.x += spark.vx * deltaTime;
            spark.y += spark.vy * deltaTime;

            // ════════════════════════════════════════════════════════════════════════
            // DETECTAR SI PASÓ EL PEAK → EXPLOTAR Y DESVANECER
            // ════════════════════════════════════════════════════════════════════════
            if (!spark.passedPeak && spark.y >= spark.peakY) {
                spark.passedPeak = true;

                // Crear mini-explosión
                if (!spark.exploded) {
                    spark.exploded = true;
                    // Crear 3-5 partículas de explosión
                    int numExplosionParts = 3 + random.nextInt(3);
                    for (int e = 0; e < numExplosionParts; e++) {
                        MiniExplosion exp = new MiniExplosion();
                        exp.x = spark.x + (random.nextFloat() - 0.5f) * 0.02f;
                        exp.y = spark.y + (random.nextFloat() - 0.5f) * 0.02f;
                        exp.age = 0f;
                        exp.color = new float[]{spark.color[0], spark.color[1], spark.color[2], 1.0f};
                        exp.size = 0.004f + random.nextFloat() * 0.003f;
                        explosions.add(exp);
                    }
                }
            }

            // Desvanecimiento - MÁS RÁPIDO después de pasar el peak
            float fadeProgress = spark.age / spark.lifetime;
            if (spark.passedPeak) {
                // Fade muy rápido después del peak
                spark.color[3] = Math.max(0, spark.color[3] - deltaTime * 3.0f);
            } else {
                spark.color[3] = (1.0f - fadeProgress) * 0.9f;
            }

            // Remover si terminó su vida o está invisible
            if (spark.age >= spark.lifetime || spark.color[3] <= 0.01f) {
                iterator.remove();
            }
        }

        // Actualizar mini-explosiones
        Iterator<MiniExplosion> expIterator = explosions.iterator();
        while (expIterator.hasNext()) {
            MiniExplosion exp = expIterator.next();
            exp.age += deltaTime;
            exp.size += deltaTime * 0.02f;  // Crece mientras desaparece
            exp.color[3] = 1.0f - (exp.age / 0.3f);  // Fade en 0.3 segundos

            if (exp.age >= 0.3f) {
                expIterator.remove();
            }
        }
    }

    @Override
    public void draw() {
        if (!GLES20.glIsProgram(programId)) {
            return;
        }

        GLES20.glUseProgram(programId);

        // Desactivar depth test para UI 2D
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);  // Blending aditivo para brillo

        // ════════════════════════════════════════════════════════════════════════
        // DIBUJAR CADA BARRA (SIN GAP - colores difuminados)
        // ════════════════════════════════════════════════════════════════════════
        for (int barIndex = 0; barIndex < NUM_BARRAS; barIndex++) {
            float barX = barXPositions[barIndex];
            float barW = barWidths[barIndex];
            float barH = barHeights[barIndex];
            float ledH = barH / LEDS_POR_BARRA;

            float level = Math.min(1.0f, smoothedLevels[barIndex]);

            // Dibujar barra como GRADIENTE CONTINUO (sin separación visible)
            // Altura visible basada en el nivel de música
            float visibleHeight = level * barH;

            if (visibleHeight > 0.001f) {
                // Dibujar la barra como un solo quad con gradiente de vértices
                drawGradientBar(barX, y, barW, visibleHeight, barH);
            }

            // ════════════════════════════════════════════════════════════════════════
            // DIBUJAR PEAK HOLDER 🌈 ARCOÍRIS (color según altura)
            // ════════════════════════════════════════════════════════════════════════
            float peakLevel = peakLevels[barIndex];
            float peakY = y + peakLevel * barH + PEAK_OFFSET;
            float peakHeight = ledH * 0.35f;

            // 🌈 Color del peak basado en su altura (mismo gradiente que la barra)
            float peakR, peakG, peakB;
            if (peakLevel < 0.25f) {
                float p = peakLevel / 0.25f;
                peakR = 1.0f; peakG = 0.0f; peakB = 0.8f - p * 0.8f;  // Rosa → Rojo
            } else if (peakLevel < 0.5f) {
                float p = (peakLevel - 0.25f) / 0.25f;
                peakR = 1.0f; peakG = p * 0.8f; peakB = 0.0f;  // Rojo → Naranja
            } else if (peakLevel < 0.75f) {
                float p = (peakLevel - 0.5f) / 0.25f;
                peakR = 1.0f - p; peakG = 0.8f + p * 0.2f; peakB = 0.0f;  // Naranja → Verde
            } else {
                float p = (peakLevel - 0.75f) / 0.25f;
                peakR = 0.0f; peakG = 1.0f; peakB = p * 0.5f;  // Verde → Cyan
            }

            // Peak principal con color arcoíris + brillo
            tempLedColor[0] = Math.min(1.0f, peakR + 0.2f);
            tempLedColor[1] = Math.min(1.0f, peakG + 0.2f);
            tempLedColor[2] = Math.min(1.0f, peakB + 0.2f);
            tempLedColor[3] = 0.95f;

            drawLedOptimized(barX, peakY, barW, peakHeight, tempLedColor);

            // Glow del mismo color (más tenue)
            tempLedColor[0] = peakR;
            tempLedColor[1] = peakG;
            tempLedColor[2] = peakB;
            tempLedColor[3] = 0.3f;

            drawLedOptimized(barX, peakY - peakHeight * 0.4f, barW, peakHeight * 0.3f, tempLedColor);
        }

        // ════════════════════════════════════════════════════════════════════════
        // DIBUJAR CHISPAS MÁGICAS ✨
        // ════════════════════════════════════════════════════════════════════════
        for (Spark spark : sparks) {
            drawSpark(spark);
        }

        // ════════════════════════════════════════════════════════════════════════
        // DIBUJAR MINI-EXPLOSIONES 💥
        // ════════════════════════════════════════════════════════════════════════
        for (MiniExplosion exp : explosions) {
            drawExplosion(exp);
        }

        // Restaurar estados
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * 🌈 Calcula el color de un LED con GRADIENTE ARCOÍRIS NEÓN
     * Gradiente: MAGENTA (abajo) → ROJO → NARANJA → AMARILLO → VERDE → CYAN (arriba)
     * @param ledIndex Índice del LED (0 = abajo)
     * @param totalLeds Total de LEDs en la barra
     * @param encendido Si el LED está encendido o apagado
     * @return Color RGBA
     */
    private float[] getLedColor(int ledIndex, int totalLeds, boolean encendido) {
        float t = (float)ledIndex / (float)(totalLeds - 1);  // 0.0 a 1.0

        float r, g, b, a;

        if (encendido) {
            // 🌈 GRADIENTE ARCOÍRIS NEÓN (de abajo hacia arriba)
            if (t < 0.2f) {
                // MAGENTA → ROJO (0% - 20%)
                float p = t / 0.2f;
                r = 1.0f;
                g = 0.0f;
                b = 1.0f - p;  // Magenta a Rojo
            } else if (t < 0.4f) {
                // ROJO → NARANJA/AMARILLO (20% - 40%)
                float p = (t - 0.2f) / 0.2f;
                r = 1.0f;
                g = p * 0.8f;  // Rojo a Naranja
                b = 0.0f;
            } else if (t < 0.6f) {
                // AMARILLO → VERDE (40% - 60%)
                float p = (t - 0.4f) / 0.2f;
                r = 1.0f - p;  // Amarillo a Verde
                g = 0.8f + p * 0.2f;
                b = 0.0f;
            } else if (t < 0.8f) {
                // VERDE → CYAN (60% - 80%)
                float p = (t - 0.6f) / 0.2f;
                r = 0.0f;
                g = 1.0f;
                b = p;  // Verde a Cyan
            } else {
                // CYAN → AZUL BRILLANTE (80% - 100%)
                float p = (t - 0.8f) / 0.2f;
                r = 0.0f;
                g = 1.0f - p * 0.3f;  // Cyan a Azul claro
                b = 1.0f;
            }
            a = 1.0f;

        } else {
            // LED APAGADO - Color tenue del gradiente
            if (t < 0.2f) {
                r = 0.15f; g = 0.0f; b = 0.1f;   // Magenta oscuro
            } else if (t < 0.4f) {
                r = 0.15f; g = 0.05f; b = 0.0f;  // Rojo/naranja oscuro
            } else if (t < 0.6f) {
                r = 0.1f; g = 0.12f; b = 0.0f;   // Amarillo oscuro
            } else if (t < 0.8f) {
                r = 0.0f; g = 0.12f; b = 0.05f;  // Verde oscuro
            } else {
                r = 0.0f; g = 0.1f; b = 0.15f;   // Cyan oscuro
            }
            a = 0.25f;
        }

        return new float[]{r, g, b, a};
    }

    /**
     * 🚀 GRADIENTE DIFUMINADO - LEDs encienden gradualmente con música
     * Sin música = APAGADOS. Con música = encienden de abajo hacia arriba
     */
    private void getLedColorGradient(int ledIndex, int totalLeds, float barLevel, float[] out) {
        float ledPosition = (float)ledIndex / (float)(totalLeds - 1);  // 0.0 (abajo) a 1.0 (arriba)

        // ════════════════════════════════════════════════════════════════════════
        // CALCULAR BRILLO - APAGADO sin música, ENCENDIDO gradual con música
        // ════════════════════════════════════════════════════════════════════════
        float brightness;
        float fadeZone = 0.3f;  // Zona de transición suave

        if (barLevel < 0.05f) {
            // SIN MÚSICA = APAGADO completamente
            brightness = 0.0f;
        } else if (ledPosition <= barLevel) {
            // LED debajo del nivel = ENCENDIDO
            brightness = 1.0f;
        } else if (ledPosition <= barLevel + fadeZone) {
            // Zona de transición suave (fade hacia arriba)
            float fadeProgress = (ledPosition - barLevel) / fadeZone;
            brightness = 1.0f - fadeProgress;  // De 1.0 a 0.0
        } else {
            // LED arriba del nivel = APAGADO
            brightness = 0.0f;
        }

        // ════════════════════════════════════════════════════════════════════════
        // GRADIENTE DE COLOR ARCOÍRIS (de abajo hacia arriba)
        // ════════════════════════════════════════════════════════════════════════
        float r, g, b;
        float t = ledPosition;

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

        // Aplicar brillo (0 = apagado, 1 = máximo)
        out[0] = r * brightness;
        out[1] = g * brightness;
        out[2] = b * brightness;
        out[3] = brightness;  // Alpha = brillo (invisible cuando apagado)
    }

    /**
     * 🎨 Dibuja una barra con GRADIENTE CONTINUO (rosa abajo → verde arriba)
     * Sin separación visible entre segmentos
     */
    private void drawGradientBar(float px, float py, float w, float visibleH, float maxH) {
        // Vértices del quad
        tempVertices[0] = px;     tempVertices[1] = py;              // Bottom-left
        tempVertices[2] = px + w; tempVertices[3] = py;              // Bottom-right
        tempVertices[4] = px;     tempVertices[5] = py + visibleH;   // Top-left
        tempVertices[6] = px + w; tempVertices[7] = py + visibleH;   // Top-right

        // Calcular colores del gradiente
        // Abajo: MAGENTA/ROSA
        float bottomR = 1.0f, bottomG = 0.0f, bottomB = 0.8f;

        // Arriba: color basado en qué tan alto llega (rosa → naranja → amarillo → verde)
        float topPosition = visibleH / maxH;  // 0.0 a 1.0
        float topR, topG, topB;

        if (topPosition < 0.25f) {
            // Rosa → Rojo
            float p = topPosition / 0.25f;
            topR = 1.0f; topG = 0.0f; topB = 0.8f - p * 0.8f;
        } else if (topPosition < 0.5f) {
            // Rojo → Naranja/Amarillo
            float p = (topPosition - 0.25f) / 0.25f;
            topR = 1.0f; topG = p * 0.8f; topB = 0.0f;
        } else if (topPosition < 0.75f) {
            // Amarillo → Verde
            float p = (topPosition - 0.5f) / 0.25f;
            topR = 1.0f - p; topG = 0.8f + p * 0.2f; topB = 0.0f;
        } else {
            // Verde → Cyan
            float p = (topPosition - 0.75f) / 0.25f;
            topR = 0.0f; topG = 1.0f; topB = p * 0.5f;
        }

        // Colores por vértice (bottom = rosa, top = color según nivel)
        tempColors[0] = bottomR; tempColors[1] = bottomG; tempColors[2] = bottomB; tempColors[3] = 1.0f;  // Bottom-left
        tempColors[4] = bottomR; tempColors[5] = bottomG; tempColors[6] = bottomB; tempColors[7] = 1.0f;  // Bottom-right
        tempColors[8] = topR;    tempColors[9] = topG;    tempColors[10] = topB;   tempColors[11] = 1.0f; // Top-left
        tempColors[12] = topR;   tempColors[13] = topG;   tempColors[14] = topB;   tempColors[15] = 1.0f; // Top-right

        drawQuadOptimized();
    }

    /**
     * 🚀 VERSIÓN OPTIMIZADA - Reutiliza tempVertices y tempColors
     */
    private void drawLedOptimized(float px, float py, float w, float h, float[] color) {
        // Escribir directamente en arrays reutilizables
        tempVertices[0] = px;     tempVertices[1] = py;      // Bottom-left
        tempVertices[2] = px + w; tempVertices[3] = py;      // Bottom-right
        tempVertices[4] = px;     tempVertices[5] = py + h;  // Top-left
        tempVertices[6] = px + w; tempVertices[7] = py + h;  // Top-right

        // Colores (mismo para los 4 vértices)
        for (int i = 0; i < 4; i++) {
            int offset = i * 4;
            tempColors[offset] = color[0];
            tempColors[offset + 1] = color[1];
            tempColors[offset + 2] = color[2];
            tempColors[offset + 3] = color[3];
        }

        drawQuadOptimized();
    }

    /**
     * 🚀 VERSIÓN OPTIMIZADA - Usa tempVertices y tempColors directamente
     */
    private void drawQuadOptimized() {
        reusableVertexBuffer.clear();
        reusableVertexBuffer.put(tempVertices);
        reusableVertexBuffer.position(0);

        reusableColorBuffer.clear();
        reusableColorBuffer.put(tempColors);
        reusableColorBuffer.position(0);

        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, reusableVertexBuffer);

        GLES20.glEnableVertexAttribArray(aColorLoc);
        GLES20.glVertexAttribPointer(aColorLoc, 4, GLES20.GL_FLOAT, false, 0, reusableColorBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(aPositionLoc);
        GLES20.glDisableVertexAttribArray(aColorLoc);
    }

    /**
     * Dibuja un LED individual (rectángulo pequeño con brillo)
     */
    private void drawLed(float x, float y, float w, float h, float[] color) {
        // Vértices del LED (rectángulo)
        float[] vertices = {
            x,     y,      // Bottom-left
            x + w, y,      // Bottom-right
            x,     y + h,  // Top-left
            x + w, y + h   // Top-right
        };

        // Colores (mismo color en todos los vértices)
        float[] colors = {
            color[0], color[1], color[2], color[3],
            color[0], color[1], color[2], color[3],
            color[0], color[1], color[2], color[3],
            color[0], color[1], color[2], color[3]
        };

        drawQuad(vertices, colors);
    }

    /**
     * Dibuja un quad (rectángulo) usando 2 triángulos
     * OPTIMIZADO: Reutiliza buffers en lugar de crear nuevos cada frame
     */
    private void drawQuad(float[] vertices, float[] colors) {
        // OPTIMIZACIÓN: Reutilizar buffers existentes (NO crear nuevos)
        reusableVertexBuffer.clear();
        reusableVertexBuffer.put(vertices);
        reusableVertexBuffer.position(0);

        reusableColorBuffer.clear();
        reusableColorBuffer.put(colors);
        reusableColorBuffer.position(0);

        // Configurar atributos
        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, reusableVertexBuffer);

        GLES20.glEnableVertexAttribArray(aColorLoc);
        GLES20.glVertexAttribPointer(aColorLoc, 4, GLES20.GL_FLOAT, false, 0, reusableColorBuffer);

        // Dibujar usando TRIANGLE_STRIP (4 vértices = 2 triángulos)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Limpiar
        GLES20.glDisableVertexAttribArray(aPositionLoc);
        GLES20.glDisableVertexAttribArray(aColorLoc);
    }

    /**
     * 〰️ Dibuja una línea de onda que conecta los picos de todas las barras
     * Efecto tipo waveform/audio visualizer profesional
     */
    private void drawWaveLine(float barWidth, float ledHeight, float gap) {
        // Grosor de la línea de onda
        float lineThickness = 0.008f;

        // Dibujar segmentos de línea conectando cada barra con la siguiente
        for (int i = 0; i < NUM_BARRAS - 1; i++) {
            // Posición X del centro de cada barra
            float x1 = x + i * barWidth + barWidth * 0.5f;
            float x2 = x + (i + 1) * barWidth + barWidth * 0.5f;

            // Posición Y basada en el nivel de cada barra (pico)
            float level1 = Math.min(1.0f, smoothedLevels[i]);
            float level2 = Math.min(1.0f, smoothedLevels[i + 1]);

            float y1 = y + level1 * height + ledHeight * 0.5f;
            float y2 = y + level2 * height + ledHeight * 0.5f;

            // Color del segmento (promedio de ambos extremos con efecto neón)
            float t1 = level1;
            float t2 = level2;
            float avgT = (t1 + t2) / 2.0f;

            // Color brillante basado en la altura promedio
            float[] lineColor = getWaveColor(avgT);

            // Dibujar el segmento como un quad delgado (para que tenga grosor)
            drawLineSegment(x1, y1, x2, y2, lineThickness, lineColor);

            // Dibujar glow detrás (más grueso y transparente)
            float[] glowColor = new float[]{lineColor[0], lineColor[1], lineColor[2], 0.3f};
            drawLineSegment(x1, y1, x2, y2, lineThickness * 3f, glowColor);
        }
    }

    /**
     * 🌈 Color para la línea de onda basado en altura
     */
    private float[] getWaveColor(float t) {
        // Colores neón vibrantes
        float r, g, b;

        if (t < 0.33f) {
            // Cyan → Verde
            float p = t / 0.33f;
            r = 0.0f;
            g = 1.0f;
            b = 1.0f - p * 0.5f;
        } else if (t < 0.66f) {
            // Verde → Amarillo
            float p = (t - 0.33f) / 0.33f;
            r = p;
            g = 1.0f;
            b = 0.0f;
        } else {
            // Amarillo → Magenta
            float p = (t - 0.66f) / 0.34f;
            r = 1.0f;
            g = 1.0f - p * 0.7f;
            b = p * 0.8f;
        }

        return new float[]{r, g, b, 0.9f};
    }

    /**
     * Dibuja un segmento de línea como un quad con grosor
     */
    private void drawLineSegment(float x1, float y1, float x2, float y2, float thickness, float[] color) {
        // Calcular la dirección perpendicular para dar grosor
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        if (length < 0.001f) return;  // Evitar división por cero

        // Normal perpendicular
        float nx = -dy / length * thickness * 0.5f;
        float ny = dx / length * thickness * 0.5f;

        // 4 vértices del quad
        float[] vertices = {
            x1 + nx, y1 + ny,  // Top-left
            x1 - nx, y1 - ny,  // Bottom-left
            x2 + nx, y2 + ny,  // Top-right
            x2 - nx, y2 - ny   // Bottom-right
        };

        float[] colors = {
            color[0], color[1], color[2], color[3],
            color[0], color[1], color[2], color[3],
            color[0], color[1], color[2], color[3],
            color[0], color[1], color[2], color[3]
        };

        drawQuad(vertices, colors);
    }

    /**
     * Dibuja una chispa mágica (partícula pequeña brillante) ✨
     */
    private void drawSpark(Spark spark) {
        float halfSize = SPARK_SIZE * 0.5f;

        // Usar arrays reutilizables
        tempVertices[0] = spark.x - halfSize; tempVertices[1] = spark.y - halfSize;
        tempVertices[2] = spark.x + halfSize; tempVertices[3] = spark.y - halfSize;
        tempVertices[4] = spark.x - halfSize; tempVertices[5] = spark.y + halfSize;
        tempVertices[6] = spark.x + halfSize; tempVertices[7] = spark.y + halfSize;

        for (int i = 0; i < 4; i++) {
            int offset = i * 4;
            tempColors[offset] = spark.color[0];
            tempColors[offset + 1] = spark.color[1];
            tempColors[offset + 2] = spark.color[2];
            tempColors[offset + 3] = spark.color[3];
        }

        drawQuadOptimized();
    }

    /**
     * 💥 Dibuja una mini-explosión (partícula que crece y desaparece)
     */
    private void drawExplosion(MiniExplosion exp) {
        float halfSize = exp.size * 0.5f;

        tempVertices[0] = exp.x - halfSize; tempVertices[1] = exp.y - halfSize;
        tempVertices[2] = exp.x + halfSize; tempVertices[3] = exp.y - halfSize;
        tempVertices[4] = exp.x - halfSize; tempVertices[5] = exp.y + halfSize;
        tempVertices[6] = exp.x + halfSize; tempVertices[7] = exp.y + halfSize;

        // Color con brillo extra (blanco mezclado)
        float alpha = exp.color[3];
        float r = Math.min(1.0f, exp.color[0] + 0.3f);
        float g = Math.min(1.0f, exp.color[1] + 0.3f);
        float b = Math.min(1.0f, exp.color[2] + 0.3f);

        for (int i = 0; i < 4; i++) {
            int offset = i * 4;
            tempColors[offset] = r;
            tempColors[offset + 1] = g;
            tempColors[offset + 2] = b;
            tempColors[offset + 3] = alpha;
        }

        drawQuadOptimized();
    }
}
