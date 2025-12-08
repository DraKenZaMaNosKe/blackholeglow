package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * 💥 Barra de Countdown para Meteorito de Pantalla
 * Muestra el progreso del tiempo restante hasta el próximo impacto
 */
public class MeteorCountdownBar implements SceneObject {
    private static final String TAG = "depurar";

    private int programId;
    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;
    private FloatBuffer uvBuffer;

    // Ubicación y tamaño
    private final float x, y;           // Posición en pantalla (0-1)
    private final float width;          // Ancho de la barra
    private final float height;         // Alto de la barra

    // Estado del countdown (0.0 = listo para spawn, 1.0 = recién spawneado)
    private float progress = 0f;  // 0-1

    // Colores
    private final float[] colorEmpty = {0.2f, 0.2f, 0.3f, 0.7f};   // Gris oscuro
    private final float[] colorFull = {1.0f, 0.5f, 0.1f, 0.9f};    // Naranja fuego
    private final float[] colorBorder = {1.0f, 1.0f, 1.0f, 1.0f};  // Blanco

    // Parpadeo cuando está cerca
    private float blinkTimer = 0f;

    public MeteorCountdownBar(Context context, float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        initShader(context);
        setupGeometry();

        Log.d(TAG, "[MeteorCountdownBar] ✓ Barra de countdown creada en (" + x + ", " + y + ")");
    }

    private void initShader(Context context) {
        // Shader con esquinas redondeadas (igual que HPBar)
        String vertexShader =
            "attribute vec2 a_Position;\n" +
            "attribute vec4 a_Color;\n" +
            "attribute vec2 a_TexCoord;\n" +
            "varying vec4 v_Color;\n" +
            "varying vec2 v_TexCoord;\n" +
            "void main() {\n" +
            "    v_Color = a_Color;\n" +
            "    v_TexCoord = a_TexCoord;\n" +
            "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
            "}\n";

        String fragmentShader =
            "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
            "varying vec4 v_Color;\n" +
            "varying vec2 v_TexCoord;\n" +
            "uniform float u_CornerRadius;\n" +
            "\n" +
            "void main() {\n" +
            "    // Calcular distancia a las esquinas para bordes redondeados\n" +
            "    vec2 uv = v_TexCoord;\n" +
            "    vec2 d = abs(uv - 0.5) - 0.5 + u_CornerRadius;\n" +
            "    float dist = length(max(d, 0.0)) - u_CornerRadius;\n" +
            "    \n" +
            "    // Crear borde suave (anti-aliasing)\n" +
            "    float alpha = 1.0 - smoothstep(-0.01, 0.01, dist);\n" +
            "    \n" +
            "    gl_FragColor = vec4(v_Color.rgb, v_Color.a * alpha);\n" +
            "}\n";

        int vShader = ShaderUtils.compileShader(GLES30.GL_VERTEX_SHADER, vertexShader);
        int fShader = ShaderUtils.compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShader);

        programId = GLES30.glCreateProgram();
        GLES30.glAttachShader(programId, vShader);
        GLES30.glAttachShader(programId, fShader);
        GLES30.glLinkProgram(programId);

        // Verificar link
        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "[MeteorCountdownBar] Shader link failed: " + GLES30.glGetProgramInfoLog(programId));
        }

        GLES30.glDeleteShader(vShader);
        GLES30.glDeleteShader(fShader);
    }

    private void setupGeometry() {
        float[] vertices = new float[24];
        vertexBuffer = createFloatBuffer(vertices);

        float[] colors = new float[48];
        colorBuffer = createFloatBuffer(colors);

        // UV coordinates para esquinas redondeadas
        float[] uvs = {
            0.0f, 0.0f,  // Bottom-left
            1.0f, 0.0f,  // Bottom-right
            0.0f, 1.0f,  // Top-left
            1.0f, 0.0f,  // Bottom-right
            1.0f, 1.0f,  // Top-right
            0.0f, 1.0f   // Top-left
        };
        uvBuffer = createFloatBuffer(uvs);
    }

    private FloatBuffer createFloatBuffer(float[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(data);
        fb.position(0);
        return fb;
    }

    /**
     * Actualiza el progreso del countdown
     * @param currentTime Tiempo actual transcurrido desde último spawn
     * @param maxTime Intervalo total para el próximo spawn
     */
    public void setProgress(float currentTime, float maxTime) {
        this.progress = currentTime / maxTime;
        this.progress = Math.max(0f, Math.min(1f, this.progress));
    }

    @Override
    public void update(float deltaTime) {
        // Parpadeo cuando está cerca (último 20%)
        if (progress > 0.8f) {
            blinkTimer += deltaTime * 8f;  // Parpadeo rápido
        } else {
            blinkTimer = 0f;
        }
    }

    @Override
    public void draw() {
        if (!GLES30.glIsProgram(programId)) return;

        GLES30.glUseProgram(programId);

        // Desactivar depth test (UI en 2D)
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // Convertir de coordenadas 0-1 a NDC (-1 a 1)
        float ndcX = x * 2.0f - 1.0f;
        float ndcY = y * 2.0f - 1.0f;
        float ndcW = width * 2.0f;
        float ndcH = height * 2.0f;

        // Interpolar color según progreso
        float[] currentColor = new float[4];
        for (int i = 0; i < 4; i++) {
            currentColor[i] = colorEmpty[i] + (colorFull[i] - colorEmpty[i]) * progress;
        }

        // Efecto de parpadeo cuando está cerca
        if (progress > 0.8f) {
            float blinkFactor = (float) Math.sin(blinkTimer) * 0.5f + 0.5f;
            currentColor[3] *= (0.5f + blinkFactor * 0.5f);  // Parpadea entre 50% y 100% alpha
        }

        // RECTÁNGULO DE FONDO (borde/vacío)
        float[] bgVertices = {
            ndcX, ndcY,
            ndcX + ndcW, ndcY,
            ndcX, ndcY + ndcH,

            ndcX + ndcW, ndcY,
            ndcX + ndcW, ndcY + ndcH,
            ndcX, ndcY + ndcH
        };

        float[] bgColors = new float[24];
        for (int i = 0; i < 6; i++) {
            bgColors[i * 4] = colorBorder[0] * 0.2f;
            bgColors[i * 4 + 1] = colorBorder[1] * 0.2f;
            bgColors[i * 4 + 2] = colorBorder[2] * 0.2f;
            bgColors[i * 4 + 3] = 0.6f;
        }

        // Dibujar fondo
        vertexBuffer.clear();
        vertexBuffer.put(bgVertices);
        vertexBuffer.position(0);

        colorBuffer.clear();
        colorBuffer.put(bgColors);
        colorBuffer.position(0);

        drawQuad();

        // RECTÁNGULO DE PROGRESO (relleno según countdown)
        float fillWidth = ndcW * progress;
        float padding = 0.003f;  // Padding más pequeño para barra más fina

        if (fillWidth > padding * 2) {
            float[] fillVertices = {
                ndcX + padding, ndcY + padding,
                ndcX + fillWidth - padding, ndcY + padding,
                ndcX + padding, ndcY + ndcH - padding,

                ndcX + fillWidth - padding, ndcY + padding,
                ndcX + fillWidth - padding, ndcY + ndcH - padding,
                ndcX + padding, ndcY + ndcH - padding
            };

            float[] fillColors = new float[24];
            for (int i = 0; i < 6; i++) {
                fillColors[i * 4] = currentColor[0];
                fillColors[i * 4 + 1] = currentColor[1];
                fillColors[i * 4 + 2] = currentColor[2];
                fillColors[i * 4 + 3] = currentColor[3];
            }

            // Dibujar relleno
            vertexBuffer.clear();
            vertexBuffer.put(fillVertices);
            vertexBuffer.position(0);

            colorBuffer.clear();
            colorBuffer.put(fillColors);
            colorBuffer.position(0);

            drawQuad();
        }

        // Restaurar estado
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
    }

    private void drawQuad() {
        int aPosLoc = GLES30.glGetAttribLocation(programId, "a_Position");
        int aColorLoc = GLES30.glGetAttribLocation(programId, "a_Color");
        int aTexCoordLoc = GLES30.glGetAttribLocation(programId, "a_TexCoord");
        int uCornerRadiusLoc = GLES30.glGetUniformLocation(programId, "u_CornerRadius");

        // Radio de las esquinas
        GLES30.glUniform1f(uCornerRadiusLoc, 0.25f);  // Más redondeada

        GLES30.glEnableVertexAttribArray(aPosLoc);
        GLES30.glVertexAttribPointer(aPosLoc, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        GLES30.glEnableVertexAttribArray(aColorLoc);
        GLES30.glVertexAttribPointer(aColorLoc, 4, GLES30.GL_FLOAT, false, 0, colorBuffer);

        GLES30.glEnableVertexAttribArray(aTexCoordLoc);
        uvBuffer.position(0);
        GLES30.glVertexAttribPointer(aTexCoordLoc, 2, GLES30.GL_FLOAT, false, 0, uvBuffer);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 6);

        GLES30.glDisableVertexAttribArray(aPosLoc);
        GLES30.glDisableVertexAttribArray(aColorLoc);
        GLES30.glDisableVertexAttribArray(aTexCoordLoc);
    }
}
