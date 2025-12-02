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

    private static final float SMOOTHING_UP = 0.35f;
    private static final float SMOOTHING_DOWN = 0.10f;

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

        Matrix.setIdentityM(projectionMatrix, 0);

        initialized = true;
        Log.d(TAG, "✓ EqualizerBarsDJ v2.0 inicializado con glow y peaks");
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
     * Actualiza los niveles de música
     */
    public void updateMusicLevels(float bass, float mid, float treble) {
        this.bassLevel = bass;
        this.midLevel = mid;
        this.trebleLevel = treble;

        int center = NUM_BARS / 2;

        for (int i = 0; i < NUM_BARS; i++) {
            float distFromCenter = Math.abs(i - center) / (float) center;

            // Mezcla: centro = más bass, lados = más treble
            float bassWeight = 1.0f - distFromCenter * 0.7f;
            float midWeight = 0.5f + distFromCenter * 0.3f;
            float trebleWeight = distFromCenter * 0.9f;

            float level = bass * bassWeight * 1.6f +
                         mid * midWeight * 1.3f +
                         treble * trebleWeight * 2.2f;

            // Variación sutil
            float variation = (float) Math.sin(i * 0.8f + System.currentTimeMillis() * 0.004f) * 0.08f;
            level += variation * level;

            level *= heightMultipliers[i];

            targetLevels[i] = Math.max(0.08f, Math.min(1.0f, level));
        }
    }

    @Override
    public void update(float deltaTime) {
        if (!initialized) return;

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
                // Nuevo peak
                peakLevels[i] = barLevels[i];
                peakHoldTime[i] = PEAK_HOLD_TIME;
            } else {
                // Decrementar hold time
                peakHoldTime[i] -= deltaTime;
                if (peakHoldTime[i] <= 0) {
                    // Peak cae
                    peakLevels[i] -= PEAK_FALL_SPEED * deltaTime;
                    if (peakLevels[i] < barLevels[i]) {
                        peakLevels[i] = barLevels[i];
                    }
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

    /**
     * Limpieza - desregistrarse del AspectRatioManager
     */
    public void release() {
        AspectRatioManager.get().unregister(this);
        Log.d(TAG, "🎵 EqualizerBarsDJ liberado");
    }
}
