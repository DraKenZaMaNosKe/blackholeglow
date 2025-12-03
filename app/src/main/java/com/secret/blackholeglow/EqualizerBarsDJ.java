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
    private static final float BAR_SPACING = 0.006f;
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
    // ✨ SISTEMA DE PARTÍCULAS - Chispas que explotan en los beats
    // ════════════════════════════════════════════════════════════════════════
    private static final int MAX_PARTICLES = 100;  // Más partículas
    private float[] particleX = new float[MAX_PARTICLES];
    private float[] particleY = new float[MAX_PARTICLES];
    private float[] particleVX = new float[MAX_PARTICLES];
    private float[] particleVY = new float[MAX_PARTICLES];
    private float[] particleLife = new float[MAX_PARTICLES];
    private float[] particleR = new float[MAX_PARTICLES];
    private float[] particleG = new float[MAX_PARTICLES];
    private float[] particleB = new float[MAX_PARTICLES];
    private int particleCount = 0;
    private java.util.Random random = new java.util.Random();

    // ════════════════════════════════════════════════════════════════════════
    // 🌊 ONDAS DE ENERGÍA - Se expanden en los beats
    // ════════════════════════════════════════════════════════════════════════
    private static final int MAX_WAVES = 5;  // Más ondas
    private float[] waveRadius = new float[MAX_WAVES];
    private float[] waveAlpha = new float[MAX_WAVES];
    private float[] waveR = new float[MAX_WAVES];
    private float[] waveG = new float[MAX_WAVES];
    private float[] waveB = new float[MAX_WAVES];
    private int waveIndex = 0;
    private static final float WAVE_SPEED = 0.8f;  // Más lento para verse mejor
    private static final float WAVE_MAX_RADIUS = 0.8f;  // Más grande

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

    // Buffers para partículas
    private FloatBuffer particleVertexBuffer;
    private FloatBuffer particleColorBuffer;

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

        // Buffers para partículas (cada partícula es un quad = 4 vértices) - 100 max
        particleVertexBuffer = createFloatBuffer(100 * 4 * 3);
        particleColorBuffer = createFloatBuffer(100 * 4 * 4);

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
            spawnBeatParticles(beatIntensity);
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
            if (level > 0.05f) {
                float microVariation = (float) Math.sin(i * 1.2f + System.currentTimeMillis() * 0.006f) * 0.015f;
                level += microVariation * level;
            }

            // Permitir que llegue a casi cero (min 0.005 en vez de 0.02)
            targetLevels[i] = Math.max(0.005f, Math.min(0.95f, level));
        }
    }

    /**
     * ✨ Genera chispas cuando hay un beat
     */
    private void spawnBeatParticles(float intensity) {
        int numParticles = 15 + (int)(intensity * 20);  // 15-35 chispas por beat

        for (int i = 0; i < numParticles && particleCount < MAX_PARTICLES; i++) {
            int idx = particleCount;

            // Posición inicial: desde las barras que tienen más energía
            float xRange = aspectRatio * 0.85f;
            particleX[idx] = (random.nextFloat() - 0.5f) * 2f * xRange;
            particleY[idx] = BASE_Y + MAX_HEIGHT * (0.5f + random.nextFloat() * 0.4f);

            // Velocidad: explotan en todas direcciones hacia arriba
            float angle = (float)(Math.PI * 0.2f + random.nextFloat() * Math.PI * 0.6f);  // 36-144 grados
            float speed = 0.4f + random.nextFloat() * 0.6f * (0.5f + intensity);
            particleVX[idx] = (float)Math.cos(angle) * speed * (random.nextBoolean() ? 1 : -1);
            particleVY[idx] = (float)Math.abs(Math.sin(angle)) * speed * 1.2f;  // Siempre hacia arriba

            // Vida más larga para que se vean
            particleLife[idx] = 0.8f + random.nextFloat() * 0.7f;

            // Color: rosa/cyan según posición
            float colorMix = random.nextFloat();
            particleR[idx] = COLOR_BASS[0] * (1-colorMix) + COLOR_TREBLE[0] * colorMix;
            particleG[idx] = COLOR_BASS[1] * (1-colorMix) + COLOR_TREBLE[1] * colorMix;
            particleB[idx] = COLOR_BASS[2] * (1-colorMix) + COLOR_TREBLE[2] * colorMix;

            particleCount++;
        }
    }

    /**
     * 🌊 Genera una onda de energía cuando hay un beat
     */
    private void spawnEnergyWave(float intensity) {
        waveRadius[waveIndex] = 0.02f;
        waveAlpha[waveIndex] = 0.9f * intensity;  // Más visible

        // Color de la onda basado en intensidad
        if (intensity > 0.5f) {
            // Beat fuerte: blanco/cyan brillante
            waveR[waveIndex] = 1.0f;
            waveG[waveIndex] = 1.0f;
            waveB[waveIndex] = 1.0f;
        } else {
            // Beat normal: rosa/magenta
            waveR[waveIndex] = 1.0f;
            waveG[waveIndex] = 0.4f;
            waveB[waveIndex] = 0.8f;
        }

        Log.d(TAG, "🌊 Wave spawned at index " + waveIndex + " alpha=" + waveAlpha[waveIndex]);
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
        // ✨ ACTUALIZAR PARTÍCULAS
        // ════════════════════════════════════════════════════════════════════════
        int aliveParticles = 0;
        for (int i = 0; i < particleCount; i++) {
            particleLife[i] -= deltaTime;

            if (particleLife[i] > 0) {
                // Mover partícula
                particleX[i] += particleVX[i] * deltaTime;
                particleY[i] += particleVY[i] * deltaTime;

                // Gravedad suave
                particleVY[i] -= 0.8f * deltaTime;

                // Fricción del aire
                particleVX[i] *= 0.98f;
                particleVY[i] *= 0.98f;

                // Compactar array (mover partículas vivas al frente)
                if (aliveParticles != i) {
                    particleX[aliveParticles] = particleX[i];
                    particleY[aliveParticles] = particleY[i];
                    particleVX[aliveParticles] = particleVX[i];
                    particleVY[aliveParticles] = particleVY[i];
                    particleLife[aliveParticles] = particleLife[i];
                    particleR[aliveParticles] = particleR[i];
                    particleG[aliveParticles] = particleG[i];
                    particleB[aliveParticles] = particleB[i];
                }
                aliveParticles++;
            }
        }
        particleCount = aliveParticles;

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

        // 4. 🌊 Dibujar ondas de energía (detrás de las partículas)
        drawEnergyWaves();

        // 5. ✨ Dibujar partículas explosivas
        drawParticles();

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
     * Actualiza la geometría de los peak markers
     */
    private void updatePeakGeometry() {
        float[] vertices = new float[NUM_BARS * 4 * 3];
        float[] colors = new float[NUM_BARS * 4 * 4];

        float totalWidth = aspectRatio * 2f * 0.92f;
        float barWidth = (totalWidth - (NUM_BARS - 1) * BAR_SPACING) / NUM_BARS;
        float startX = -totalWidth / 2f;

        for (int i = 0; i < NUM_BARS; i++) {
            float x = startX + i * (barWidth + BAR_SPACING);
            float peakY = BASE_Y + MIN_HEIGHT + peakLevels[i] * (MAX_HEIGHT - MIN_HEIGHT);

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
            vertices[vi + 7] = peakY + PEAK_HEIGHT;
            vertices[vi + 8] = 0f;

            vertices[vi + 9] = x + barWidth;
            vertices[vi + 10] = peakY + PEAK_HEIGHT;
            vertices[vi + 11] = 0f;

            // Color del peak (blanco brillante con el tono de la barra)
            float[] peakColor = getBarColor(i, 1.0f);
            float alpha = 0.9f;

            // Todos los vértices del peak con el mismo color brillante
            for (int j = 0; j < 4; j++) {
                colors[ci + j * 4 + 0] = Math.min(1.0f, peakColor[0] * 1.3f);
                colors[ci + j * 4 + 1] = Math.min(1.0f, peakColor[1] * 1.3f);
                colors[ci + j * 4 + 2] = Math.min(1.0f, peakColor[2] * 1.3f);
                colors[ci + j * 4 + 3] = alpha;
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
    // ✨ DIBUJO DE PARTÍCULAS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Dibuja las partículas explosivas que salen de las barras en los beats
     */
    private void drawParticles() {
        if (particleCount == 0) return;

        // Log siempre que hay partículas
        Log.d(TAG, "🎆 DRAW " + particleCount + " particles at Y=" +
              String.format("%.2f", particleY[0]));

        // Asegurar que el shader y matriz estén activos
        GLES20.glUseProgram(shaderProgram);
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, projectionMatrix, 0);

        // Usar blending aditivo para que las partículas brillen
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);

        float[] vertices = new float[particleCount * 4 * 3];
        float[] colors = new float[particleCount * 4 * 4];

        float particleSize = 0.03f;  // Tamaño visible

        for (int i = 0; i < particleCount; i++) {
            float x = particleX[i];
            float y = particleY[i];
            float life = particleLife[i];

            // Tamaño basado en vida
            float size = particleSize * (0.6f + life * 0.4f);

            int vi = i * 12;
            int ci = i * 16;

            // Quad de la partícula
            vertices[vi + 0] = x - size;
            vertices[vi + 1] = y - size;
            vertices[vi + 2] = 0f;

            vertices[vi + 3] = x + size;
            vertices[vi + 4] = y - size;
            vertices[vi + 5] = 0f;

            vertices[vi + 6] = x - size;
            vertices[vi + 7] = y + size;
            vertices[vi + 8] = 0f;

            vertices[vi + 9] = x + size;
            vertices[vi + 10] = y + size;
            vertices[vi + 11] = 0f;

            // Color MUY brillante (blanco/rosa)
            float alpha = Math.min(1.0f, life * 1.5f);

            // Colores muy saturados y brillantes
            for (int j = 0; j < 4; j++) {
                colors[ci + j * 4 + 0] = 1.0f;  // R máximo
                colors[ci + j * 4 + 1] = 0.5f + life * 0.5f;  // G
                colors[ci + j * 4 + 2] = 1.0f;  // B máximo (rosa/magenta brillante)
                colors[ci + j * 4 + 3] = alpha;
            }
        }

        // Actualizar buffers
        particleVertexBuffer.clear();
        particleVertexBuffer.put(vertices);
        particleVertexBuffer.position(0);

        particleColorBuffer.clear();
        particleColorBuffer.put(colors);
        particleColorBuffer.position(0);

        // Dibujar
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 0, particleVertexBuffer);

        GLES20.glEnableVertexAttribArray(aColorHandle);
        GLES20.glVertexAttribPointer(aColorHandle, 4, GLES20.GL_FLOAT, false, 0, particleColorBuffer);

        for (int i = 0; i < particleCount; i++) {
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, i * 4, 4);
        }

        GLES20.glDisableVertexAttribArray(aPositionHandle);
        GLES20.glDisableVertexAttribArray(aColorHandle);

        // Restaurar blending normal
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 🌊 DIBUJO DE ONDAS DE ENERGÍA
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Dibuja las ondas de energía que se expanden desde las barras en los beats
     */
    private void drawEnergyWaves() {
        // Contar ondas activas
        int activeWaves = 0;
        for (int i = 0; i < MAX_WAVES; i++) {
            if (waveAlpha[i] > 0) activeWaves++;
        }
        if (activeWaves == 0) return;

        // Asegurar shader y matriz activos
        GLES20.glUseProgram(shaderProgram);
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, projectionMatrix, 0);

        // Usar blending aditivo para ondas brillantes
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);

        // Centro de las ondas (donde están las barras)
        float centerX = 0f;
        float centerY = BASE_Y + 0.15f;

        float[] vertices = new float[MAX_WAVES * WAVE_SEGMENTS * 2 * 3];
        float[] colors = new float[MAX_WAVES * WAVE_SEGMENTS * 2 * 4];

        int vertexIndex = 0;
        int colorIndex = 0;
        int segmentsDrawn = 0;

        for (int w = 0; w < MAX_WAVES; w++) {
            if (waveAlpha[w] <= 0) continue;

            float radius = waveRadius[w];
            float alpha = waveAlpha[w];

            // Dibujar círculo como una serie de líneas
            for (int s = 0; s < WAVE_SEGMENTS; s++) {
                float angle1 = (float)(s * 2 * Math.PI / WAVE_SEGMENTS);
                float angle2 = (float)((s + 1) * 2 * Math.PI / WAVE_SEGMENTS);

                // Solo dibujar la mitad superior del círculo (semicírculo hacia arriba)
                if (angle1 > Math.PI) continue;

                float x1 = centerX + (float)Math.cos(angle1) * radius * aspectRatio;
                float y1 = centerY + (float)Math.sin(angle1) * radius;
                float x2 = centerX + (float)Math.cos(angle2) * radius * aspectRatio;
                float y2 = centerY + (float)Math.sin(angle2) * radius;

                // Línea (2 vértices)
                vertices[vertexIndex++] = x1;
                vertices[vertexIndex++] = y1;
                vertices[vertexIndex++] = 0f;

                vertices[vertexIndex++] = x2;
                vertices[vertexIndex++] = y2;
                vertices[vertexIndex++] = 0f;

                // Colores con fade hacia afuera
                float fadeOuter = 1.0f - (radius / WAVE_MAX_RADIUS);
                float finalAlpha = alpha * fadeOuter;

                // Color 1
                colors[colorIndex++] = waveR[w];
                colors[colorIndex++] = waveG[w];
                colors[colorIndex++] = waveB[w];
                colors[colorIndex++] = finalAlpha;

                // Color 2
                colors[colorIndex++] = waveR[w];
                colors[colorIndex++] = waveG[w];
                colors[colorIndex++] = waveB[w];
                colors[colorIndex++] = finalAlpha;

                segmentsDrawn++;
            }
        }

        if (segmentsDrawn == 0) {
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            return;
        }

        // Actualizar buffers
        waveVertexBuffer.clear();
        waveVertexBuffer.put(vertices, 0, vertexIndex);
        waveVertexBuffer.position(0);

        waveColorBuffer.clear();
        waveColorBuffer.put(colors, 0, colorIndex);
        waveColorBuffer.position(0);

        // Dibujar líneas
        GLES20.glLineWidth(3.0f);

        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 0, waveVertexBuffer);

        GLES20.glEnableVertexAttribArray(aColorHandle);
        GLES20.glVertexAttribPointer(aColorHandle, 4, GLES20.GL_FLOAT, false, 0, waveColorBuffer);

        GLES20.glDrawArrays(GLES20.GL_LINES, 0, segmentsDrawn * 2);

        GLES20.glDisableVertexAttribArray(aPositionHandle);
        GLES20.glDisableVertexAttribArray(aColorHandle);

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
