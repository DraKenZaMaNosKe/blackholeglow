package com.secret.blackholeglow;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.systems.AspectRatioManager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   🎵 EqualizerBarsDJ v2.0 - Ecualizador Estilo DJ Premium               ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  CARACTERÍSTICAS:                                                        ║
 * ║  • 32 barras delgadas en la parte inferior                              ║
 * ║  • Diseño simétrico: centro más alto, lados más bajos                   ║
 * ║  • Gradiente de colores: Rosa (bass) → Cyan (treble)                    ║
 * ║  • Efecto GLOW neón con resplandor                                      ║
 * ║  • Peak markers que caen suavemente                                     ║
 * ║  • Integración con AspectRatioManager                                   ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class EqualizerBarsDJ implements SceneObject, AspectRatioManager.AspectRatioAware {
    private static final String TAG = "EqualizerBarsDJ";

    // ════════════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN DE BARRAS
    // ════════════════════════════════════════════════════════════════════════
    private static final int NUM_BARS = 32;
    private static final float BAR_SPACING = 0.0f;  // Sin separación entre barras
    private static final float MAX_HEIGHT = 0.38f;
    private static final float MIN_HEIGHT = 0.025f;
    private static final float BASE_Y = -0.95f;

    // ════════════════════════════════════════════════════════════════════════
    // COLORES - Gradiente Rosa → Cyan
    // ════════════════════════════════════════════════════════════════════════
    // Rosa/Magenta para bajos (centro)
    private static final float[] COLOR_BASS = {1.0f, 0.2f, 0.6f};      // Rosa neón
    // Cyan para agudos (lados)
    private static final float[] COLOR_TREBLE = {0.2f, 0.9f, 1.0f};    // Cyan neón
    // Blanco para picos muy altos
    private static final float[] COLOR_PEAK = {1.0f, 1.0f, 1.0f};

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

        Log.d(TAG, "🎵 EqualizerBarsDJ v2.0 creado con " + NUM_BARS + " barras");
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

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vertexShader);
        GLES20.glAttachShader(shaderProgram, fragmentShader);
        GLES20.glLinkProgram(shaderProgram);

        int[] linked = new int[1];
        GLES20.glGetProgramiv(shaderProgram, GLES20.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            Log.e(TAG, "❌ Error linkeando programa: " + GLES20.glGetProgramInfoLog(shaderProgram));
            GLES20.glDeleteProgram(shaderProgram);
            return;
        }

        aPositionHandle = GLES20.glGetAttribLocation(shaderProgram, "a_Position");
        aColorHandle = GLES20.glGetAttribLocation(shaderProgram, "a_Color");
        uMVPMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "u_MVPMatrix");

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
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
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
            // Usar módulo para evitar overflow de time (ciclo cada ~10 segundos)
            if (level > 0.05f) {
                long safeTime = System.currentTimeMillis() % 10000;  // Ciclo cada 10 segundos
                float microVariation = (float) Math.sin(i * 1.2f + safeTime * 0.006f) * 0.015f;
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

        // Buscar índices de barras con nivel alto
        int[] highBars = new int[NUM_BARS];
        int highBarCount = 0;
        float threshold = 0.4f;  // Umbral para considerar una barra "alta"

        for (int i = 0; i < NUM_BARS; i++) {
            if (barLevels[i] > threshold) {
                highBars[highBarCount++] = i;
            }
        }

        // Si no hay suficientes barras altas, usar posiciones aleatorias
        if (highBarCount < 2) {
            highBarCount = NUM_BARS;
            for (int i = 0; i < NUM_BARS; i++) {
                highBars[i] = i;
            }
        }

        // Calcular ancho de barras para posiciones X
        float totalWidth = aspectRatio * 2f * 0.92f;
        float barWidth = (totalWidth - (NUM_BARS - 1) * BAR_SPACING) / NUM_BARS;
        float startX = -totalWidth / 2f;

        for (int i = 0; i < numLightnings && lightningCount < MAX_LIGHTNINGS; i++) {
            int idx = lightningCount;

            // Seleccionar dos barras aleatorias para conectar
            int bar1Idx = highBars[random.nextInt(highBarCount)];
            int bar2Idx = highBars[random.nextInt(highBarCount)];

            // Asegurar que sean diferentes y no muy cercanas
            int attempts = 0;
            while (Math.abs(bar1Idx - bar2Idx) < 3 && attempts < 10) {
                bar2Idx = highBars[random.nextInt(highBarCount)];
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

            // Color: cyan/blanco eléctrico o rosa/magenta
            if (random.nextFloat() > 0.3f) {
                // Cyan eléctrico (70% de probabilidad)
                lightningR[idx] = 0.3f + random.nextFloat() * 0.3f;
                lightningG[idx] = 0.8f + random.nextFloat() * 0.2f;
                lightningB[idx] = 1.0f;
            } else {
                // Rosa/magenta eléctrico (30%)
                lightningR[idx] = 1.0f;
                lightningG[idx] = 0.3f + random.nextFloat() * 0.3f;
                lightningB[idx] = 0.8f + random.nextFloat() * 0.2f;
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
                float[] color1 = getBarColor(i, 1.0f);
                float[] color2 = getBarColor(j, 1.0f);
                sparkR[idx] = (color1[0] + color2[0]) / 2f;
                sparkG[idx] = (color1[1] + color2[1]) / 2f;
                sparkB[idx] = (color1[2] + color2[2]) / 2f;

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

        // Color de la onda basado en intensidad - siempre brillante
        if (intensity > 0.6f) {
            // Beat fuerte: cyan brillante
            waveR[waveIndex] = 0.3f;
            waveG[waveIndex] = 1.0f;
            waveB[waveIndex] = 1.0f;
        } else {
            // Beat normal: rosa/magenta brillante
            waveR[waveIndex] = 1.0f;
            waveG[waveIndex] = 0.3f;
            waveB[waveIndex] = 0.9f;
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

        GLES20.glUseProgram(shaderProgram);

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Matriz ortográfica
        Matrix.orthoM(projectionMatrix, 0,
                     -aspectRatio, aspectRatio,
                     -1f, 1f,
                     -1f, 1f);

        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, projectionMatrix, 0);

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

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    private void drawBuffers(FloatBuffer vBuffer, FloatBuffer cBuffer) {
        vBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vBuffer);

        cBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aColorHandle);
        GLES20.glVertexAttribPointer(aColorHandle, 4, GLES20.GL_FLOAT, false, 0, cBuffer);

        for (int i = 0; i < NUM_BARS; i++) {
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, i * 4, 4);
        }

        GLES20.glDisableVertexAttribArray(aPositionHandle);
        GLES20.glDisableVertexAttribArray(aColorHandle);
    }

    /**
     * Calcula el color de una barra basado en su posición (gradiente rosa→cyan)
     */
    private float[] getBarColor(int barIndex, float intensity) {
        float t = (float) barIndex / (NUM_BARS - 1);  // 0 a 1 (izquierda a derecha)

        // Distancia del centro (0 = centro, 1 = extremos)
        float distFromCenter = Math.abs(t - 0.5f) * 2f;

        // Interpolar entre rosa (centro) y cyan (lados)
        float r = COLOR_BASS[0] + (COLOR_TREBLE[0] - COLOR_BASS[0]) * distFromCenter;
        float g = COLOR_BASS[1] + (COLOR_TREBLE[1] - COLOR_BASS[1]) * distFromCenter;
        float b = COLOR_BASS[2] + (COLOR_TREBLE[2] - COLOR_BASS[2]) * distFromCenter;

        // Si el nivel es muy alto, mezclar con blanco
        if (intensity > 0.8f) {
            float whiteMix = (intensity - 0.8f) * 2.5f;  // 0 a 0.5
            r = r + (COLOR_PEAK[0] - r) * whiteMix;
            g = g + (COLOR_PEAK[1] - g) * whiteMix;
            b = b + (COLOR_PEAK[2] - b) * whiteMix;
        }

        return new float[]{r * intensity, g * intensity, b * intensity};
    }

    /**
     * Actualiza la geometría de las barras principales
     */
    private void updateBarGeometry() {
        float[] vertices = new float[NUM_BARS * 4 * 3];
        float[] colors = new float[NUM_BARS * 4 * 4];

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
            float[] baseColor = getBarColor(i, 0.5f + level * 0.3f);
            float[] topColor = getBarColor(i, 0.8f + level * 0.2f);

            // Bottom colors (más oscuros)
            colors[ci + 0] = baseColor[0] * 0.6f;
            colors[ci + 1] = baseColor[1] * 0.6f;
            colors[ci + 2] = baseColor[2] * 0.6f;
            colors[ci + 3] = 1.0f;

            colors[ci + 4] = baseColor[0] * 0.6f;
            colors[ci + 5] = baseColor[1] * 0.6f;
            colors[ci + 6] = baseColor[2] * 0.6f;
            colors[ci + 7] = 1.0f;

            // Top colors (más brillantes)
            colors[ci + 8] = topColor[0];
            colors[ci + 9] = topColor[1];
            colors[ci + 10] = topColor[2];
            colors[ci + 11] = 1.0f;

            colors[ci + 12] = topColor[0];
            colors[ci + 13] = topColor[1];
            colors[ci + 14] = topColor[2];
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
     */
    private void updateGlowGeometry() {
        float[] vertices = new float[NUM_BARS * 4 * 3];
        float[] colors = new float[NUM_BARS * 4 * 4];

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
            float[] glowColor = getBarColor(i, 0.6f + level * 0.4f);
            float alpha = GLOW_INTENSITY * level;

            // Glow con transparencia en los bordes
            colors[ci + 0] = glowColor[0];
            colors[ci + 1] = glowColor[1];
            colors[ci + 2] = glowColor[2];
            colors[ci + 3] = alpha * 0.3f;  // Muy transparente abajo

            colors[ci + 4] = glowColor[0];
            colors[ci + 5] = glowColor[1];
            colors[ci + 6] = glowColor[2];
            colors[ci + 7] = alpha * 0.3f;

            colors[ci + 8] = glowColor[0];
            colors[ci + 9] = glowColor[1];
            colors[ci + 10] = glowColor[2];
            colors[ci + 11] = alpha * 0.6f;  // Más opaco arriba

            colors[ci + 12] = glowColor[0];
            colors[ci + 13] = glowColor[1];
            colors[ci + 14] = glowColor[2];
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
     */
    private void updatePeakGeometry() {
        float[] vertices = new float[NUM_BARS * 4 * 3];
        float[] colors = new float[NUM_BARS * 4 * 4];

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
            // COLOR DEL PEAK - Mismo gradiente que las barras (rosa→cyan)
            // ═══════════════════════════════════════════════════════════════
            float t = (float) i / (NUM_BARS - 1);  // 0 a 1
            float distFromCenter = Math.abs(t - 0.5f) * 2f;  // 0 = centro, 1 = lados

            // Interpolar entre rosa (centro) y cyan (lados) - SIN BLANCO
            float r = COLOR_BASS[0] + (COLOR_TREBLE[0] - COLOR_BASS[0]) * distFromCenter;
            float g = COLOR_BASS[1] + (COLOR_TREBLE[1] - COLOR_BASS[1]) * distFromCenter;
            float b = COLOR_BASS[2] + (COLOR_TREBLE[2] - COLOR_BASS[2]) * distFromCenter;

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
        GLES20.glUseProgram(shaderProgram);
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, projectionMatrix, 0);

        // Usar blending aditivo para que los rayos brillen intensamente
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);

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
            int numQuads = LIGHTNING_SEGMENTS;
            float[] vertices = new float[numQuads * 4 * 3];
            float[] colors = new float[numQuads * 4 * 4];

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
            GLES20.glEnableVertexAttribArray(aPositionHandle);
            GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 0, lightningVertexBuffer);

            GLES20.glEnableVertexAttribArray(aColorHandle);
            GLES20.glVertexAttribPointer(aColorHandle, 4, GLES20.GL_FLOAT, false, 0, lightningColorBuffer);

            for (int s = 0; s < LIGHTNING_SEGMENTS; s++) {
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, s * 4, 4);
            }

            GLES20.glDisableVertexAttribArray(aPositionHandle);
            GLES20.glDisableVertexAttribArray(aColorHandle);

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

            GLES20.glEnableVertexAttribArray(aPositionHandle);
            GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 0, lightningVertexBuffer);

            GLES20.glEnableVertexAttribArray(aColorHandle);
            GLES20.glVertexAttribPointer(aColorHandle, 4, GLES20.GL_FLOAT, false, 0, lightningColorBuffer);

            for (int s = 0; s < LIGHTNING_SEGMENTS; s++) {
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, s * 4, 4);
            }

            GLES20.glDisableVertexAttribArray(aPositionHandle);
            GLES20.glDisableVertexAttribArray(aColorHandle);
        }

        // Restaurar blending normal
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
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
        GLES20.glUseProgram(shaderProgram);
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, projectionMatrix, 0);

        // Usar blending aditivo para ondas brillantes
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);

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
            int numSegments = 24;
            float[] vertices = new float[numSegments * 4 * 3];  // 4 vértices por segmento (quad)
            float[] colors = new float[numSegments * 4 * 4];

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
            GLES20.glEnableVertexAttribArray(aPositionHandle);
            GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 0, waveVertexBuffer);

            GLES20.glEnableVertexAttribArray(aColorHandle);
            GLES20.glVertexAttribPointer(aColorHandle, 4, GLES20.GL_FLOAT, false, 0, waveColorBuffer);

            // Dibujar cada segmento como triangle strip
            for (int s = 0; s < numSegments; s++) {
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, s * 4, 4);
            }

            GLES20.glDisableVertexAttribArray(aPositionHandle);
            GLES20.glDisableVertexAttribArray(aColorHandle);
        }

        // Restaurar blending normal
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
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
        GLES20.glUseProgram(shaderProgram);
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, projectionMatrix, 0);

        // Usar blending aditivo para chispas brillantes
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);

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
            float[] pointsX = new float[4];
            float[] pointsY = new float[4];

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
            int numSegments = 3;
            float[] vertices = new float[numSegments * 4 * 3];
            float[] colors = new float[numSegments * 4 * 4];

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
            GLES20.glEnableVertexAttribArray(aPositionHandle);
            GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 0, sparkVertexBuffer);

            GLES20.glEnableVertexAttribArray(aColorHandle);
            GLES20.glVertexAttribPointer(aColorHandle, 4, GLES20.GL_FLOAT, false, 0, sparkColorBuffer);

            for (int s = 0; s < numSegments; s++) {
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, s * 4, 4);
            }

            GLES20.glDisableVertexAttribArray(aPositionHandle);
            GLES20.glDisableVertexAttribArray(aColorHandle);
        }

        // Restaurar blending normal
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * Limpieza - desregistrarse del AspectRatioManager
     */
    public void release() {
        AspectRatioManager.get().unregister(this);
        Log.d(TAG, "🎵 EqualizerBarsDJ liberado");
    }
}
