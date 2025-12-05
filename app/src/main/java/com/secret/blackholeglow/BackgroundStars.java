package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔════════════════════════════════════════════════════════════════════════╗
 * ║  ✨ ESTRELLAS DE FONDO - Efecto de Profundidad                        ║
 * ║  Estrellas pequeñas que parpadean suavemente                          ║
 * ╚════════════════════════════════════════════════════════════════════════╝
 *
 * Características:
 * - Estrellas muy pequeñas para simular lejanía
 * - Parpadeo suave con ondas sinusoidales
 * - Colores blancos/azulados como estrellas reales
 * - MUY OPTIMIZADO: Un solo draw call para todas las estrellas
 * - Sin texturas, solo puntos GL
 */
public class BackgroundStars implements SceneObject {
    private static final String TAG = "BackgroundStars";

    // ════════════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN - Optimizado para bajo consumo de GPU
    // ════════════════════════════════════════════════════════════════════════
    private static final int NUM_STARS = 60;           // Cantidad moderada
    private static final float MIN_SIZE = 1.5f;        // Tamaño mínimo (muy pequeñas)
    private static final float MAX_SIZE = 3.5f;        // Tamaño máximo
    private static final float TWINKLE_SPEED = 1.5f;   // Velocidad de parpadeo

    // ════════════════════════════════════════════════════════════════════════
    // DATOS
    // ════════════════════════════════════════════════════════════════════════
    private float[] positions;      // x, y por estrella
    private float[] phases;         // Fase de parpadeo única
    private float[] baseBrightness; // Brillo base (0.3 - 1.0)
    private float[] colors;         // RGB por estrella (blanco/azulado)

    private float time = 0f;

    // OpenGL
    private int programId;
    private int aPositionLoc;
    private int aColorLoc;
    private int uPointSizeLoc;  // Uniform para tamaño base

    // Buffers reutilizables (evita crear nuevos cada frame)
    private FloatBuffer positionBuffer;
    private FloatBuffer colorBuffer;

    // Cache para optimización
    private int frameCount = 0;
    private static final int UPDATE_EVERY_N_FRAMES = 2;  // Actualizar parpadeo cada 2 frames

    public BackgroundStars(Context context) {
        Log.d(TAG, "✨ Creando estrellas de fondo...");

        initShader();
        initStars();
        initBuffers();

        Log.d(TAG, "✨ " + NUM_STARS + " estrellas de fondo creadas");
    }

    private void initShader() {
        // Vertex shader simple - tamaño fijo para todas las estrellas
        String vertexShader =
            "attribute vec2 a_Position;\n" +
            "attribute vec4 a_Color;\n" +
            "uniform float u_PointSize;\n" +
            "varying vec4 v_Color;\n" +
            "void main() {\n" +
            "    v_Color = a_Color;\n" +
            "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
            "    gl_PointSize = u_PointSize;\n" +
            "}\n";

        // Fragment shader con efecto de brillo suave
        String fragmentShader =
            "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
            "varying vec4 v_Color;\n" +
            "void main() {\n" +
            "    vec2 coord = gl_PointCoord - vec2(0.5);\n" +
            "    float dist = length(coord);\n" +
            "    // Gradiente suave desde el centro\n" +
            "    float alpha = v_Color.a * (1.0 - smoothstep(0.0, 0.5, dist));\n" +
            "    // Centro más brillante\n" +
            "    float glow = 1.0 + (1.0 - dist * 2.0) * 0.5;\n" +
            "    gl_FragColor = vec4(v_Color.rgb * glow, alpha);\n" +
            "}\n";

        programId = ShaderUtils.createProgram(vertexShader, fragmentShader);

        if (programId != 0) {
            aPositionLoc = GLES20.glGetAttribLocation(programId, "a_Position");
            aColorLoc = GLES20.glGetAttribLocation(programId, "a_Color");
            uPointSizeLoc = GLES20.glGetUniformLocation(programId, "u_PointSize");
            Log.d(TAG, "✓ Shader compilado - Locations: pos=" + aPositionLoc + " col=" + aColorLoc + " size=" + uPointSizeLoc);
        } else {
            Log.e(TAG, "✗ Error compilando shader");
        }
    }

    private void initStars() {
        positions = new float[NUM_STARS * 2];
        phases = new float[NUM_STARS];
        baseBrightness = new float[NUM_STARS];
        colors = new float[NUM_STARS * 3];

        // Semilla fija para reproducibilidad
        java.util.Random rand = new java.util.Random(12345);

        for (int i = 0; i < NUM_STARS; i++) {
            // Posición aleatoria en pantalla (NDC: -1 a 1)
            positions[i * 2] = -0.95f + rand.nextFloat() * 1.9f;      // X
            positions[i * 2 + 1] = -0.95f + rand.nextFloat() * 1.9f;  // Y

            // Fase única para cada estrella (parpadeo desfasado)
            phases[i] = rand.nextFloat() * (float) Math.PI * 2f;

            // Brillo base variable (algunas más tenues, otras más brillantes)
            baseBrightness[i] = 0.4f + rand.nextFloat() * 0.6f;

            // Colores: mayoría blancas, algunas azuladas, algunas amarillentas
            float colorType = rand.nextFloat();
            if (colorType < 0.6f) {
                // Blanca
                colors[i * 3] = 1.0f;
                colors[i * 3 + 1] = 1.0f;
                colors[i * 3 + 2] = 1.0f;
            } else if (colorType < 0.8f) {
                // Azulada (estrella joven/caliente)
                colors[i * 3] = 0.7f;
                colors[i * 3 + 1] = 0.85f;
                colors[i * 3 + 2] = 1.0f;
            } else {
                // Amarillenta (estrella vieja)
                colors[i * 3] = 1.0f;
                colors[i * 3 + 1] = 0.95f;
                colors[i * 3 + 2] = 0.7f;
            }
        }
    }

    private void initBuffers() {
        // Buffer de posiciones (estático, no cambia)
        ByteBuffer bb = ByteBuffer.allocateDirect(positions.length * 4);
        bb.order(ByteOrder.nativeOrder());
        positionBuffer = bb.asFloatBuffer();
        positionBuffer.put(positions);
        positionBuffer.position(0);

        // Buffer de colores (dinámico, incluye alpha para brillo)
        ByteBuffer cb = ByteBuffer.allocateDirect(NUM_STARS * 4 * 4);  // RGBA
        cb.order(ByteOrder.nativeOrder());
        colorBuffer = cb.asFloatBuffer();

        // ¡IMPORTANTE! Inicializar colores con valores iniciales
        for (int i = 0; i < NUM_STARS; i++) {
            colorBuffer.put(colors[i * 3]);       // R
            colorBuffer.put(colors[i * 3 + 1]);   // G
            colorBuffer.put(colors[i * 3 + 2]);   // B
            colorBuffer.put(baseBrightness[i]);   // A (brillo inicial)
        }
        colorBuffer.position(0);
    }

    @Override
    public void update(float deltaTime) {
        frameCount++;
        time += deltaTime;

        // Optimización: actualizar parpadeo cada N frames
        if (frameCount % UPDATE_EVERY_N_FRAMES != 0) return;

        // Actualizar colores con efecto de parpadeo
        colorBuffer.position(0);

        for (int i = 0; i < NUM_STARS; i++) {
            // Calcular brillo con onda sinusoidal suave
            float twinkle = (float) Math.sin(time * TWINKLE_SPEED + phases[i]);
            // Convertir de [-1,1] a [0.3, 1.0] para parpadeo suave
            float brightness = baseBrightness[i] * (0.65f + 0.35f * twinkle);

            // Escribir RGBA
            colorBuffer.put(colors[i * 3] * brightness);      // R
            colorBuffer.put(colors[i * 3 + 1] * brightness);  // G
            colorBuffer.put(colors[i * 3 + 2] * brightness);  // B
            colorBuffer.put(brightness);                       // A
        }

        colorBuffer.position(0);
    }

    @Override
    public void draw() {
        if (programId == 0) return;

        GLES20.glUseProgram(programId);

        // Configurar blending aditivo para brillo
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);

        // Tamaño de punto uniforme para todas las estrellas
        GLES20.glUniform1f(uPointSizeLoc, 4.0f);

        // Configurar atributos
        GLES20.glEnableVertexAttribArray(aPositionLoc);
        positionBuffer.position(0);
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, positionBuffer);

        GLES20.glEnableVertexAttribArray(aColorLoc);
        colorBuffer.position(0);
        GLES20.glVertexAttribPointer(aColorLoc, 4, GLES20.GL_FLOAT, false, 0, colorBuffer);

        // ¡UN SOLO DRAW CALL para todas las estrellas!
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, NUM_STARS);

        // Limpiar
        GLES20.glDisableVertexAttribArray(aPositionLoc);
        GLES20.glDisableVertexAttribArray(aColorLoc);

        // Restaurar estados
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }
}
