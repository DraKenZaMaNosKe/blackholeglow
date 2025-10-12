package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Indicador visual de música en tiempo real
 * Muestra 3 barras: BASS (rojo), MID (verde), TREBLE (azul)
 */
public class MusicIndicator implements SceneObject {
    private static final String TAG = "depurar";

    private int programId;
    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;

    private int aPositionLoc;
    private int aColorLoc;

    // Posición y tamaño del indicador (coordenadas NDC 2D)
    private final float x;
    private final float y;
    private final float barWidth;
    private final float maxBarHeight;

    // Niveles de música (0-1)
    private float bassLevel = 0f;
    private float midLevel = 0f;
    private float trebleLevel = 0f;

    // Suavizado
    private float smoothedBass = 0f;
    private float smoothedMid = 0f;
    private float smoothedTreble = 0f;

    // Contador de frames para logs
    private int frameCount = 0;

    public MusicIndicator(Context context, float x, float y, float barWidth, float maxBarHeight) {
        Log.d(TAG, "╔══════════════════════════════════════════════╗");
        Log.d(TAG, "║      CREANDO MUSIC INDICATOR                ║");
        Log.d(TAG, "╚══════════════════════════════════════════════╝");

        this.x = x;
        this.y = y;
        this.barWidth = barWidth;
        this.maxBarHeight = maxBarHeight;

        Log.d(TAG, "[MusicIndicator] Posición: (" + x + ", " + y + ")");
        Log.d(TAG, "[MusicIndicator] Tamaño barras: " + barWidth + " x " + maxBarHeight);

        initShader(context);

        Log.d(TAG, "[MusicIndicator] ✓ Constructor completado");
    }

    private void initShader(Context context) {
        Log.d(TAG, "[MusicIndicator] Iniciando shader...");

        // Shader simple inline (igual que HPBar) para rectángulos 2D
        String vertexShader =
            "attribute vec2 a_Position;\n" +
            "attribute vec4 a_Color;\n" +
            "varying vec4 v_Color;\n" +
            "void main() {\n" +
            "    v_Color = a_Color;\n" +
            "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
            "}\n";

        String fragmentShader =
            "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
            "varying vec4 v_Color;\n" +
            "void main() {\n" +
            "    gl_FragColor = v_Color;\n" +
            "}\n";

        Log.d(TAG, "[MusicIndicator] Compilando shaders inline...");

        int vShader = ShaderUtils.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        int fShader = ShaderUtils.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        Log.d(TAG, "[MusicIndicator] Vertex shader: " + vShader + ", Fragment shader: " + fShader);

        programId = GLES20.glCreateProgram();
        GLES20.glAttachShader(programId, vShader);
        GLES20.glAttachShader(programId, fShader);
        GLES20.glLinkProgram(programId);

        // Verificar link
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "[MusicIndicator] ✗✗✗ Shader link FALLÓ: " + GLES20.glGetProgramInfoLog(programId));
            return;
        }

        GLES20.glDeleteShader(vShader);
        GLES20.glDeleteShader(fShader);

        Log.d(TAG, "[MusicIndicator] Shader creado - programId=" + programId);

        if (programId == 0) {
            Log.e(TAG, "[MusicIndicator] ✗✗✗ ERROR CRÍTICO: Shader NO se pudo crear! programId=0");
            return;
        }

        if (!GLES20.glIsProgram(programId)) {
            Log.e(TAG, "[MusicIndicator] ✗✗✗ ERROR: programId no es válido según glIsProgram!");
            return;
        }

        aPositionLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        aColorLoc = GLES20.glGetAttribLocation(programId, "a_Color");

        Log.d(TAG, "[MusicIndicator] ✓✓✓ Shader inicializado CORRECTAMENTE");
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

        // Log DESACTIVADO para performance
        // Solo log cada 300 frames (cada 5 segundos) si hay audio significativo
        if (frameCount % 300 == 0 && (bass > 0.05f || mid > 0.05f || treble > 0.05f)) {
            Log.d(TAG, String.format("[MusicIndicator] Bass:%.2f Mid:%.2f Treble:%.2f",
                    bass, mid, treble));
        }
    }

    @Override
    public void update(float deltaTime) {
        frameCount++;

        // Suavizar los valores para animación fluida
        float smoothing = 0.85f;
        smoothedBass = smoothedBass * smoothing + bassLevel * (1f - smoothing);
        smoothedMid = smoothedMid * smoothing + midLevel * (1f - smoothing);
        smoothedTreble = smoothedTreble * smoothing + trebleLevel * (1f - smoothing);

        // Log desactivado para performance
    }

    @Override
    public void draw() {
        // Verificación sin logs constantes
        if (!GLES20.glIsProgram(programId)) {
            return;
        }

        GLES20.glUseProgram(programId);

        // Desactivar depth test para UI 2D
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Dibujar las 3 barras
        float gap = 0.01f;  // Espacio entre barras

        // BASS (izquierda, rojo)
        drawBar(x, y, smoothedBass, 1.0f, 0.2f, 0.2f, 0.9f);

        // MID (centro, verde)
        drawBar(x + barWidth + gap, y, smoothedMid, 0.2f, 1.0f, 0.2f, 0.9f);

        // TREBLE (derecha, azul)
        drawBar(x + (barWidth + gap) * 2, y, smoothedTreble, 0.2f, 0.4f, 1.0f, 0.9f);

        // Restaurar estados
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    /**
     * Dibuja una barra individual
     */
    private void drawBar(float xPos, float yPos, float level, float r, float g, float b, float a) {
        float barHeight = level * maxBarHeight;

        // Borde/fondo oscuro (barra completa)
        float[] bgVertices = {
            xPos, yPos,
            xPos + barWidth, yPos,
            xPos, yPos + maxBarHeight,
            xPos + barWidth, yPos + maxBarHeight
        };

        float[] bgColors = {
            0.1f, 0.1f, 0.1f, 0.6f,
            0.1f, 0.1f, 0.1f, 0.6f,
            0.1f, 0.1f, 0.1f, 0.6f,
            0.1f, 0.1f, 0.1f, 0.6f
        };

        drawQuad(bgVertices, bgColors);

        // Barra de nivel (animada)
        if (barHeight > 0.001f) {
            float[] barVertices = {
                xPos + 0.002f, yPos + 0.002f,
                xPos + barWidth - 0.002f, yPos + 0.002f,
                xPos + 0.002f, yPos + barHeight,
                xPos + barWidth - 0.002f, yPos + barHeight
            };

            // Gradiente: más brillante arriba
            float[] barColors = {
                r * 0.7f, g * 0.7f, b * 0.7f, a,  // Abajo más oscuro
                r * 0.7f, g * 0.7f, b * 0.7f, a,
                r, g, b, a,  // Arriba más brillante
                r, g, b, a
            };

            drawQuad(barVertices, barColors);
        }
    }

    /**
     * Dibuja un quad (2 triángulos)
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

        // Dibujar
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Limpiar
        GLES20.glDisableVertexAttribArray(aPositionLoc);
        GLES20.glDisableVertexAttribArray(aColorLoc);
    }
}
