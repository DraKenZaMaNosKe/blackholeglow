package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Indicador visual de música en tiempo real - ESTILO LED BARS
 * Muestra múltiples barras verticales con gradiente de color (rojo→amarillo→verde)
 * Similar a ecualizadores LED profesionales
 */
public class MusicIndicator implements SceneObject {
    private static final String TAG = "depurar";

    // Configuración del ecualizador - ESTILO RETRO
    private static final int NUM_BARRAS = 4;  // Solo 4 barras (bass, low-mid, high-mid, treble)
    private static final int LEDS_POR_BARRA = 12;  // 12 LEDs por barra (estilo retro/pixelado)

    private int programId;

    private int aPositionLoc;
    private int aColorLoc;

    // Posición y tamaño del indicador (coordenadas NDC 2D)
    private final float x;
    private final float y;
    private final float width;   // Ancho total del ecualizador
    private final float height;  // Alto total del ecualizador

    // Niveles de música (0-1) - Uno por cada barra
    private float[] barLevels = new float[NUM_BARRAS];

    // Suavizado independiente por barra
    private float[] smoothedLevels = new float[NUM_BARRAS];

    // Asignación de barras a frecuencias
    // Barra 0 = BASS, Barra 1 = LOW-MID, Barra 2 = HIGH-MID, Barra 3 = TREBLE
    private float bassLevel = 0f;
    private float midLevel = 0f;
    private float trebleLevel = 0f;

    // Contador de frames para logs
    private int frameCount = 0;

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

        initShader(context);

        Log.d(TAG, "[MusicIndicator] ✓ Constructor completado");
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

        // Fragment shader con efecto de brillo/glow
        String fragmentShader =
            "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
            "varying vec4 v_Color;\n" +
            "\n" +
            "void main() {\n" +
            "    // Efecto de brillo LED (bordes más suaves)\n" +
            "    vec4 color = v_Color;\n" +
            "    \n" +
            "    // Aumentar brillo si el alpha indica LED encendido\n" +
            "    if (color.a > 0.5) {\n" +
            "        color.rgb *= 1.3;  // Brillo extra para LEDs encendidos\n" +
            "    }\n" +
            "    \n" +
            "    gl_FragColor = color;\n" +
            "}\n";

        programId = ShaderUtils.createProgram(vertexShader, fragmentShader);

        if (programId == 0) {
            Log.e(TAG, "[MusicIndicator] ✗✗✗ ERROR: Shader NO se pudo crear!");
            return;
        }

        aPositionLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        aColorLoc = GLES20.glGetAttribLocation(programId, "a_Color");

        Log.d(TAG, "[MusicIndicator] ✓✓✓ Shader LED inicializado");
        Log.d(TAG, "[MusicIndicator]   programId: " + programId);
        Log.d(TAG, "[MusicIndicator]   aPositionLoc: " + aPositionLoc);
        Log.d(TAG, "[MusicIndicator]   aColorLoc: " + aColorLoc);
    }

    /**
     * Actualiza los niveles de música
     */
    public void updateMusicLevels(float bass, float mid, float treble) {
        this.bassLevel = bass;
        this.midLevel = mid;
        this.trebleLevel = treble;

        // Distribuir los niveles a las 4 barras
        // Barra 0: BASS puro
        barLevels[0] = bass;

        // Barra 1: LOW-MID (mezcla de bass y mid, más bass)
        barLevels[1] = bass * 0.3f + mid * 0.7f;

        // Barra 2: HIGH-MID (mezcla de mid y treble, más mid)
        barLevels[2] = mid * 0.7f + treble * 0.3f;

        // Barra 3: TREBLE puro
        barLevels[3] = treble;

        // Log cada 300 frames (reducido para performance)
        if (frameCount % 300 == 0 && (bass > 0.05f || mid > 0.05f || treble > 0.05f)) {
            Log.d(TAG, String.format("[MusicIndicator] Bass:%.2f Mid:%.2f Treble:%.2f",
                    bass, mid, treble));
        }
    }

    @Override
    public void update(float deltaTime) {
        frameCount++;

        // Suavizar cada barra independientemente para animación fluida
        float smoothing = 0.75f;  // Más suave que antes para efecto LED
        for (int i = 0; i < NUM_BARRAS; i++) {
            smoothedLevels[i] = smoothedLevels[i] * smoothing + barLevels[i] * (1f - smoothing);
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

        // Calcular dimensiones VERTICALES - ESTILO RETRO
        // Cada barra es VERTICAL (crece de ABAJO hacia ARRIBA)
        float barWidth = width / NUM_BARRAS;  // Ancho de cada barra vertical (4 barras lado a lado)
        float ledHeight = height / LEDS_POR_BARRA;  // Alto de cada LED (más grande para estilo pixelado)
        float gap = barWidth * 0.25f;  // Espacio MAYOR entre barras (25% del ancho) para estilo retro

        // Dibujar cada barra VERTICAL (lado a lado horizontalmente)
        for (int barIndex = 0; barIndex < NUM_BARRAS; barIndex++) {
            float barX = x + barIndex * barWidth;  // Posición X de esta barra
            float level = Math.min(1.0f, smoothedLevels[barIndex]);
            int ledsEncendidos = (int)(level * LEDS_POR_BARRA);

            // Dibujar LEDs de esta barra VERTICAL (de ABAJO hacia ARRIBA)
            for (int ledIndex = 0; ledIndex < LEDS_POR_BARRA; ledIndex++) {
                float ledY = y + ledIndex * ledHeight;  // Posición Y del LED (desde abajo)
                boolean encendido = (ledIndex < ledsEncendidos);

                // Calcular color basado en la ALTURA (gradiente rojo→amarillo→verde)
                float[] ledColor = getLedColor(ledIndex, LEDS_POR_BARRA, encendido);

                // Dibujar el LED VERTICAL
                drawLed(barX + gap/2, ledY + gap/2,
                       barWidth - gap, ledHeight - gap,
                       ledColor);
            }
        }

        // Restaurar estados
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * Calcula el color de un LED basado en su ALTURA
     * Gradiente: ROJO (abajo) → AMARILLO (medio) → VERDE (arriba)
     * @param ledIndex Índice del LED (0 = abajo)
     * @param totalLeds Total de LEDs en la barra
     * @param encendido Si el LED está encendido o apagado
     * @return Color RGBA
     */
    private float[] getLedColor(int ledIndex, int totalLeds, boolean encendido) {
        float normalizedHeight = (float)ledIndex / (float)totalLeds;

        float r, g, b, a;

        if (encendido) {
            // Gradiente de color según ALTURA
            if (normalizedHeight < 0.33f) {
                // ZONA ROJA (abajo) - 0% a 33%
                float t = normalizedHeight / 0.33f;  // 0-1 en zona roja
                r = 1.0f;
                g = t * 0.8f;  // De 0 a 0.8 (rojo puro → naranja)
                b = 0.0f;
            } else if (normalizedHeight < 0.66f) {
                // ZONA AMARILLA (medio) - 33% a 66%
                float t = (normalizedHeight - 0.33f) / 0.33f;  // 0-1 en zona amarilla
                r = 1.0f - t * 0.3f;  // De 1.0 a 0.7
                g = 0.8f + t * 0.2f;  // De 0.8 a 1.0 (amarillo brillante)
                b = 0.0f;
            } else {
                // ZONA VERDE (arriba) - 66% a 100%
                float t = (normalizedHeight - 0.66f) / 0.34f;  // 0-1 en zona verde
                r = 0.7f - t * 0.7f;  // De 0.7 a 0.0
                g = 1.0f;
                b = t * 0.3f;  // De 0.0 a 0.3 (verde brillante)
            }
            a = 1.0f;  // Totalmente visible

        } else {
            // LED APAGADO - mostrar color tenue del LED
            // Mismo gradiente pero MUY oscuro
            if (normalizedHeight < 0.33f) {
                r = 0.15f; g = 0.05f; b = 0.0f;  // Rojo oscuro
            } else if (normalizedHeight < 0.66f) {
                r = 0.15f; g = 0.15f; b = 0.0f;  // Amarillo oscuro
            } else {
                r = 0.05f; g = 0.15f; b = 0.05f;  // Verde oscuro
            }
            a = 0.3f;  // Muy transparente
        }

        return new float[]{r, g, b, a};
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
     */
    private void drawQuad(float[] vertices, float[] colors) {
        // Crear buffers
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        FloatBuffer vb = vbb.asFloatBuffer();
        vb.put(vertices);
        vb.position(0);

        ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length * 4);
        cbb.order(ByteOrder.nativeOrder());
        FloatBuffer cb = cbb.asFloatBuffer();
        cb.put(colors);
        cb.position(0);

        // Configurar atributos
        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vb);

        GLES20.glEnableVertexAttribArray(aColorLoc);
        GLES20.glVertexAttribPointer(aColorLoc, 4, GLES20.GL_FLOAT, false, 0, cb);

        // Dibujar usando TRIANGLE_STRIP (4 vértices = 2 triángulos)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Limpiar
        GLES20.glDisableVertexAttribArray(aPositionLoc);
        GLES20.glDisableVertexAttribArray(aColorLoc);
    }
}
