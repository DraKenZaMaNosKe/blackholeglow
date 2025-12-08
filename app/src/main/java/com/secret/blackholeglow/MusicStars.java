package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔════════════════════════════════════════════════════════════════════════╗
 * ║  🌀 GALAXIAS ESPIRALES MUSICALES - OPTIMIZADO                         ║
 * ╠════════════════════════════════════════════════════════════════════════╣
 * ║  OPTIMIZACIONES:                                                       ║
 * ║  • Buffers pre-allocados (sin GC pressure)                            ║
 * ║  • Un solo draw call para todas las galaxias                          ║
 * ║  • Tiempo con módulo para evitar overflow                             ║
 * ║  • Velocidades de giro individuales                                   ║
 * ║  • Direcciones de giro alternadas                                     ║
 * ╚════════════════════════════════════════════════════════════════════════╝
 */
public class MusicStars implements SceneObject {
    private static final String TAG = "MusicStars";

    // ════════════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN
    // ════════════════════════════════════════════════════════════════════════
    private static final int TOTAL_GALAXIAS = 7;
    private static final float TWO_PI = (float)(Math.PI * 2.0);
    private static final float TIME_WRAP = 1000f;  // Reiniciar tiempo cada 1000 segundos

    // ════════════════════════════════════════════════════════════════════════
    // 🌀 CONFIGURACIÓN DE CADA GALAXIA
    // Formato: {x, y, freqType, tamaño, velocidadGiro, direccion, alpha}
    // direccion: 1.0 = horario, -1.0 = antihorario
    // alpha: 0.0 = invisible, 1.0 = completamente visible
    // ════════════════════════════════════════════════════════════════════════
    private static final float[][] GALAXY_CONFIG = {
        //    X       Y     freq   size   speed   dir   alpha
        { 0.48f,  0.04f,   0,    150f,   0.50f,  -1f,   0.50f},  // Galaxia 0
        {-0.10f,  0.07f,   0,     95f,   0.4f,   1f,   0.85f}, // Galaxia 1
        {-0.44f,  0.31f,   1,    100f,   0.25f, -1f,   0.9f},  // Galaxia 2
        {-0.75f, -0.12f,   1,    110f,   0.35f,  1f,   0.8f},  // Galaxia 3
        {-0.50f, -0.38f,   2,    100f,   0.45f, -1f,   0.85f}, // Galaxia 4
        { 0.14f, -0.34f,   2,     80f,   0.3f,   1f,   0.9f},  // Galaxia 5
        { 0.66f, -0.44f,   1,     80f,   0.5f,  -1f,   0.8f},  // Galaxia 6
    };

    // Colores sutiles que coinciden con galaxias del fondo (blancos/azulados)
    private static final float[][] FREQ_COLORS = {
        {0.7f, 0.8f, 1.0f},    // 0: Azul claro (galaxias azuladas)
        {0.9f, 0.9f, 1.0f},    // 1: Blanco azulado (galaxias blancas)
        {0.8f, 0.85f, 1.0f},   // 2: Azul muy claro
    };

    // ════════════════════════════════════════════════════════════════════════
    // DATOS PRE-ALLOCADOS (evita GC)
    // ════════════════════════════════════════════════════════════════════════
    private final float[] positions = new float[TOTAL_GALAXIAS * 2];  // x,y por galaxia
    private final float[] colors = new float[TOTAL_GALAXIAS * 4];     // RGBA por galaxia
    private final float[] sizes = new float[TOTAL_GALAXIAS];          // tamaño por galaxia
    private final float[] speeds = new float[TOTAL_GALAXIAS];         // velocidad giro
    private final float[] directions = new float[TOTAL_GALAXIAS];     // dirección giro
    private final float[] alphas = new float[TOTAL_GALAXIAS];         // transparencia base
    private final int[] freqTypes = new int[TOTAL_GALAXIAS];          // tipo frecuencia
    private final float[] phases = new float[TOTAL_GALAXIAS];         // fase inicial

    // Buffers OpenGL (pre-allocados una sola vez)
    private FloatBuffer positionBuffer;
    private FloatBuffer colorBuffer;

    // Niveles de música
    private float bassLevel = 0f;
    private float midLevel = 0f;
    private float trebleLevel = 0f;

    // Tiempo (con wrap para evitar overflow)
    private float time = 0f;

    // Shader
    private int programId;
    private int uTimeLoc;
    private int uPointSizeLoc;
    private int uSpeedLoc;
    private int uDirectionLoc;

    public MusicStars(Context context) {
        Log.d(TAG, "🌀 Inicializando " + TOTAL_GALAXIAS + " galaxias espirales...");

        initData();
        initBuffers();
        initShader();

        Log.d(TAG, "✓ MusicStars optimizado listo");
    }

    private void initData() {
        for (int i = 0; i < TOTAL_GALAXIAS; i++) {
            float[] cfg = GALAXY_CONFIG[i];

            // Posiciones
            positions[i * 2] = cfg[0];      // X
            positions[i * 2 + 1] = cfg[1];  // Y

            // Tipo de frecuencia y propiedades
            freqTypes[i] = (int) cfg[2];
            sizes[i] = cfg[3];
            speeds[i] = cfg[4];
            directions[i] = cfg[5];
            alphas[i] = cfg[6];  // Transparencia individual

            // Fase inicial aleatoria (pero determinista por índice)
            phases[i] = (i * 0.9f) % TWO_PI;

            // Colores iniciales
            float[] baseColor = FREQ_COLORS[freqTypes[i]];
            colors[i * 4] = baseColor[0];
            colors[i * 4 + 1] = baseColor[1];
            colors[i * 4 + 2] = baseColor[2];
            colors[i * 4 + 3] = alphas[i];  // Usar alpha configurado
        }
    }

    private void initBuffers() {
        // Buffer de posiciones (estático)
        ByteBuffer pbb = ByteBuffer.allocateDirect(positions.length * 4);
        pbb.order(ByteOrder.nativeOrder());
        positionBuffer = pbb.asFloatBuffer();
        positionBuffer.put(positions);
        positionBuffer.position(0);

        // Buffer de colores (dinámico pero pre-allocado)
        ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length * 4);
        cbb.order(ByteOrder.nativeOrder());
        colorBuffer = cbb.asFloatBuffer();
        colorBuffer.put(colors);
        colorBuffer.position(0);
    }

    private void initShader() {
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

        // Fragment shader: SOLO BRAZOS ESPIRALES (sin núcleo) para mezclarse con fondo
        String fragmentShader =
            "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
            "varying vec4 v_Color;\n" +
            "uniform float u_Time;\n" +
            "uniform float u_Speed;\n" +
            "uniform float u_Direction;\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 uv = gl_PointCoord - vec2(0.5);\n" +
            "    float dist = length(uv);\n" +
            "    float angle = atan(uv.y, uv.x);\n" +
            "    \n" +
            "    // Espiral con brazos más definidos (como galaxia real)\n" +
            "    float spiral = angle + dist * 8.0 + u_Time * u_Speed * u_Direction;\n" +
            "    float arms = sin(spiral * 2.0) * 0.5 + 0.5;\n" +
            "    arms = pow(arms, 0.8);  // Brazos más suaves\n" +
            "    \n" +
            "    // SIN núcleo brillante - solo los brazos\n" +
            "    // Fade desde el centro hacia afuera (más visible en el medio)\n" +
            "    float innerFade = smoothstep(0.05, 0.2, dist);  // Evitar centro\n" +
            "    float outerFade = 1.0 - smoothstep(0.3, 0.5, dist);  // Fade en bordes\n" +
            "    \n" +
            "    // Solo los brazos, muy sutiles\n" +
            "    float brightness = arms * innerFade * outerFade * 0.4;\n" +
            "    \n" +
            "    // Color sutil, casi transparente\n" +
            "    vec3 color = v_Color.rgb;\n" +
            "    float alpha = v_Color.a * brightness * 0.5;  // Muy transparente\n" +
            "    \n" +
            "    if (alpha < 0.01) discard;\n" +
            "    gl_FragColor = vec4(color, alpha);\n" +
            "}\n";

        programId = ShaderUtils.createProgram(vertexShader, fragmentShader);

        if (programId == 0) {
            Log.e(TAG, "✗ Error creando shader");
            return;
        }

        uTimeLoc = GLES30.glGetUniformLocation(programId, "u_Time");
        uPointSizeLoc = GLES30.glGetUniformLocation(programId, "u_PointSize");
        uSpeedLoc = GLES30.glGetUniformLocation(programId, "u_Speed");
        uDirectionLoc = GLES30.glGetUniformLocation(programId, "u_Direction");

        Log.d(TAG, "✓ Shader compilado");
    }

    public void updateMusicLevels(float bass, float mid, float treble) {
        this.bassLevel = bass;
        this.midLevel = mid;
        this.trebleLevel = treble;
    }

    @Override
    public void update(float deltaTime) {
        // Actualizar tiempo con wrap para evitar overflow
        time += deltaTime;
        if (time > TIME_WRAP) {
            time -= TIME_WRAP;
        }

        // Actualizar colores basado en música (sin crear objetos nuevos)
        for (int i = 0; i < TOTAL_GALAXIAS; i++) {
            float intensity;
            switch (freqTypes[i]) {
                case 0: intensity = bassLevel; break;
                case 1: intensity = midLevel; break;
                default: intensity = trebleLevel; break;
            }

            // Modular alpha según intensidad de música (respetando alpha base)
            float[] baseColor = FREQ_COLORS[freqTypes[i]];
            float brightnessMod = 0.85f + intensity * 0.15f;

            colors[i * 4] = baseColor[0] * brightnessMod;
            colors[i * 4 + 1] = baseColor[1] * brightnessMod;
            colors[i * 4 + 2] = baseColor[2] * brightnessMod;
            // Alpha base + boost por música (máximo alpha configurado)
            colors[i * 4 + 3] = alphas[i] * (0.85f + intensity * 0.15f);
        }

        // Actualizar buffer de colores
        colorBuffer.position(0);
        colorBuffer.put(colors);
        colorBuffer.position(0);
    }

    @Override
    public void draw() {
        if (programId == 0) return;

        GLES30.glUseProgram(programId);

        // Estados OpenGL
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE);

        // Tiempo global
        GLES30.glUniform1f(uTimeLoc, time);

        int aPositionLoc = GLES30.glGetAttribLocation(programId, "a_Position");
        int aColorLoc = GLES30.glGetAttribLocation(programId, "a_Color");

        // Dibujar cada galaxia (necesario por tamaños diferentes)
        for (int i = 0; i < TOTAL_GALAXIAS; i++) {
            // Uniforms específicos de esta galaxia
            GLES30.glUniform1f(uPointSizeLoc, sizes[i]);
            GLES30.glUniform1f(uSpeedLoc, speeds[i]);
            GLES30.glUniform1f(uDirectionLoc, directions[i]);

            // Posición de esta galaxia
            positionBuffer.position(i * 2);
            GLES30.glEnableVertexAttribArray(aPositionLoc);
            GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 0, positionBuffer);

            // Color de esta galaxia
            colorBuffer.position(i * 4);
            GLES30.glEnableVertexAttribArray(aColorLoc);
            GLES30.glVertexAttribPointer(aColorLoc, 4, GLES30.GL_FLOAT, false, 0, colorBuffer);

            // Dibujar
            GLES30.glDrawArrays(GLES30.GL_POINTS, 0, 1);
        }

        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glDisableVertexAttribArray(aColorLoc);

        // Restaurar estados
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
    }
}
