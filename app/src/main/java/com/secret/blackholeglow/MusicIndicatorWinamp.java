package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔════════════════════════════════════════════════════════════════════════╗
 * ║  🎵 RESPALDO - ESTILO WINAMP LED BARS (7 BARRAS)                      ║
 * ║  Para volver a usar: renombrar a MusicIndicator.java                  ║
 * ╚════════════════════════════════════════════════════════════════════════╝
 *
 * Indicador visual de música en tiempo real - ESTILO LED BARS
 * Muestra múltiples barras verticales con gradiente de color (rojo→amarillo→verde)
 * Similar a ecualizadores LED profesionales
 */
public class MusicIndicatorWinamp implements SceneObject {
    private static final String TAG = "depurar";

    // Configuración del ecualizador - 7 BARRAS POR RANGOS DE FRECUENCIA
    private static final int NUM_BARRAS = 7;  // 7 barras para visualización óptima (equilibrio perfecto)
    private static final int LEDS_POR_BARRA = 12;  // 12 LEDs por barra (estilo retro)
    private static final float SMOOTHING_FACTOR = 0.6f;  // Factor de suavizado (0.0 = sin suavizado, 0.9 = muy suave)

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

    // Contador de frames para logs
    private int frameCount = 0;

    public MusicIndicatorWinamp(Context context, float x, float y, float width, float height) {
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
     * Actualiza los niveles de música con distribución de 7 bandas de frecuencia (óptimo)
     * Mapea 3 bandas básicas (bass, mid, treble) a 7 barras visuales con interpolación
     */
    public void updateMusicLevels(float bass, float mid, float treble) {
        this.bassLevel = bass;
        this.midLevel = mid;
        this.trebleLevel = treble;

        // ════════════════════════════════════════════════════════════════════════
        // DISTRIBUCIÓN DE 3 BANDAS (bass, mid, treble) → 7 BARRAS (ÓPTIMO)
        // ════════════════════════════════════════════════════════════════════════

        // Barra 0: SUB-BASS (60-250 Hz) - Bass puro con énfasis en graves profundos
        barLevels[0] = bass * 1.2f;  // Amplificado para que se note el bombo

        // Barra 1: BASS (250-500 Hz) - Bass dominante
        barLevels[1] = bass * 0.9f + mid * 0.1f;

        // Barra 2: MID-LOW (500-1000 Hz) - Bass + medios graves
        barLevels[2] = bass * 0.4f + mid * 0.6f;

        // Barra 3: MID (1000-2000 Hz) - Medios centrales (voces, piano)
        barLevels[3] = bass * 0.1f + mid * 0.9f;

        // Barra 4: MID-HIGH (2000-4000 Hz) - Medios altos + transición a agudos
        barLevels[4] = mid * 0.6f + treble * 0.4f;

        // Barra 5: TREBLE (4000-8000 Hz) - Agudos (violín, claridad)
        barLevels[5] = mid * 0.2f + treble * 0.8f;

        // Barra 6: AIR (8000-16000 Hz) - Súper agudos (platillos, aire, brillo)
        barLevels[6] = treble * 1.2f;  // Amplificado para destacar brillos

        // Limitar valores a rango 0.0-1.0
        for (int i = 0; i < NUM_BARRAS; i++) {
            barLevels[i] = Math.min(1.0f, Math.max(0.0f, barLevels[i]));
        }

        // Log cada 300 frames (reducido para performance)
        if (frameCount % 300 == 0 && (bass > 0.05f || mid > 0.05f || treble > 0.05f)) {
            Log.d(TAG, String.format("[MusicIndicator] 🎵 Bass:%.2f Mid:%.2f Treble:%.2f",
                    bass, mid, treble));
            Log.d(TAG, String.format("[MusicIndicator] 📊 Barras: [%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f]",
                    barLevels[0], barLevels[1], barLevels[2], barLevels[3], barLevels[4],
                    barLevels[5], barLevels[6]));
        }
    }

    @Override
    public void update(float deltaTime) {
        frameCount++;

        // Suavizar cada barra independientemente para animación fluida
        // SMOOTHING_FACTOR: 0.0 = sin suavizado (respuesta instantánea)
        //                   0.6 = suavizado medio (estilo Winamp clásico)
        //                   0.9 = muy suave (efecto flotante)
        for (int i = 0; i < NUM_BARRAS; i++) {
            smoothedLevels[i] = smoothedLevels[i] * SMOOTHING_FACTOR + barLevels[i] * (1f - SMOOTHING_FACTOR);
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
