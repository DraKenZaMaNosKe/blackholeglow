// HealthBar.java - Barra de vida estilo videojuego
package com.secret.blackholeglow;

import android.opengl.GLES30;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   💚 HealthBar - Barra de vida flotante                                  ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  • Se dibuja sobre las naves mostrando HP restante                       ║
 * ║  • Color verde → amarillo → rojo según vida                              ║
 * ║  • Borde negro para contraste                                            ║
 * ║  • Billboard: siempre mira a la cámara                                   ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class HealthBar {

    // Configuración visual
    private static final float BAR_WIDTH = 0.4f;      // Ancho de la barra
    private static final float BAR_HEIGHT = 0.06f;   // Alto de la barra
    private static final float BORDER_SIZE = 0.01f;  // Grosor del borde
    private static final float OFFSET_Y = 0.35f;     // Distancia sobre la nave

    // OpenGL
    private static int shaderProgram = 0;
    private static int aPositionHandle;
    private static int uMVPMatrixHandle;
    private static int uColorHandle;

    private FloatBuffer vertexBuffer;

    // Estado
    private int maxHP;
    private int currentHP;
    private boolean isEnemy;  // true = OVNI (rojo cuando bajo), false = Defender (azul)

    /**
     * Constructor
     * @param maxHP Vida máxima
     * @param isEnemy true para OVNI, false para Defender
     */
    public HealthBar(int maxHP, boolean isEnemy) {
        this.maxHP = maxHP;
        this.currentHP = maxHP;
        this.isEnemy = isEnemy;

        // Inicializar shaders si no existen
        if (shaderProgram == 0) {
            initShaders();
        }

        // Crear buffer de vértices
        vertexBuffer = ByteBuffer.allocateDirect(4 * 3 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
    }

    /**
     * Inicializar shaders
     */
    private static void initShaders() {
        String vertexShaderCode =
            "attribute vec4 a_Position;\n" +
            "uniform mat4 u_MVPMatrix;\n" +
            "void main() {\n" +
            "  gl_Position = u_MVPMatrix * a_Position;\n" +
            "}";

        String fragmentShaderCode =
            "precision mediump float;\n" +
            "uniform vec4 u_Color;\n" +
            "void main() {\n" +
            "  gl_FragColor = u_Color;\n" +
            "}";

        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode);

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vertexShader);
        GLES30.glAttachShader(shaderProgram, fragmentShader);
        GLES30.glLinkProgram(shaderProgram);

        aPositionHandle = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        uMVPMatrixHandle = GLES30.glGetUniformLocation(shaderProgram, "u_MVPMatrix");
        uColorHandle = GLES30.glGetUniformLocation(shaderProgram, "u_Color");
    }

    private static int loadShader(int type, String shaderCode) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);
        return shader;
    }

    /**
     * Actualizar HP actual
     */
    public void setHP(int hp) {
        this.currentHP = Math.max(0, Math.min(hp, maxHP));
    }

    /**
     * Dibujar la barra de vida
     * @param x Posición X de la nave
     * @param y Posición Y de la nave
     * @param z Posición Z de la nave
     * @param mvpMatrix Matriz MVP de la cámara
     */
    public void draw(float x, float y, float z, float[] mvpMatrix) {
        if (currentHP <= 0) return;  // No dibujar si está muerto

        GLES30.glUseProgram(shaderProgram);

        // Deshabilitar depth test para que siempre se vea
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // Posición de la barra (encima de la nave)
        float barY = y + OFFSET_Y;

        // Calcular proporción de vida
        float hpRatio = (float) currentHP / maxHP;

        // ════════════════════════════════════════════════════════════
        // 1️⃣ DIBUJAR FONDO (borde negro semi-transparente)
        // ════════════════════════════════════════════════════════════
        float bgHalfW = BAR_WIDTH / 2f + BORDER_SIZE;
        float bgHalfH = BAR_HEIGHT / 2f + BORDER_SIZE;

        float[] bgVertices = {
            x - bgHalfW, barY - bgHalfH, z,
            x + bgHalfW, barY - bgHalfH, z,
            x - bgHalfW, barY + bgHalfH, z,
            x + bgHalfW, barY + bgHalfH, z,
        };

        vertexBuffer.clear();
        vertexBuffer.put(bgVertices);
        vertexBuffer.position(0);

        GLES30.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLES30.glUniform4f(uColorHandle, 0.0f, 0.0f, 0.0f, 0.7f);

        GLES30.glEnableVertexAttribArray(aPositionHandle);
        GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        // ════════════════════════════════════════════════════════════
        // 2️⃣ DIBUJAR FONDO VACÍO (gris oscuro)
        // ════════════════════════════════════════════════════════════
        float halfW = BAR_WIDTH / 2f;
        float halfH = BAR_HEIGHT / 2f;

        float[] emptyVertices = {
            x - halfW, barY - halfH, z + 0.001f,
            x + halfW, barY - halfH, z + 0.001f,
            x - halfW, barY + halfH, z + 0.001f,
            x + halfW, barY + halfH, z + 0.001f,
        };

        vertexBuffer.clear();
        vertexBuffer.put(emptyVertices);
        vertexBuffer.position(0);

        GLES30.glUniform4f(uColorHandle, 0.2f, 0.2f, 0.2f, 0.8f);
        GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        // ════════════════════════════════════════════════════════════
        // 3️⃣ DIBUJAR BARRA DE VIDA (color según HP)
        // ════════════════════════════════════════════════════════════
        float filledWidth = BAR_WIDTH * hpRatio;
        float leftX = x - halfW;
        float rightX = leftX + filledWidth;

        float[] hpVertices = {
            leftX, barY - halfH, z + 0.002f,
            rightX, barY - halfH, z + 0.002f,
            leftX, barY + halfH, z + 0.002f,
            rightX, barY + halfH, z + 0.002f,
        };

        vertexBuffer.clear();
        vertexBuffer.put(hpVertices);
        vertexBuffer.position(0);

        // Color según HP y tipo de nave
        float r, g, b;
        if (isEnemy) {
            // OVNI: Verde cuando full → Rojo cuando bajo
            if (hpRatio > 0.6f) {
                // Verde
                r = 0.2f;
                g = 1.0f;
                b = 0.3f;
            } else if (hpRatio > 0.3f) {
                // Amarillo/Naranja
                r = 1.0f;
                g = 0.8f;
                b = 0.2f;
            } else {
                // Rojo
                r = 1.0f;
                g = 0.2f;
                b = 0.2f;
            }
        } else {
            // Defender: Cyan cuando full → Naranja cuando bajo
            if (hpRatio > 0.6f) {
                // Cyan brillante
                r = 0.2f;
                g = 0.9f;
                b = 1.0f;
            } else if (hpRatio > 0.3f) {
                // Amarillo
                r = 1.0f;
                g = 0.9f;
                b = 0.3f;
            } else {
                // Naranja/Rojo
                r = 1.0f;
                g = 0.4f;
                b = 0.2f;
            }
        }

        GLES30.glUniform4f(uColorHandle, r, g, b, 1.0f);
        GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        // ════════════════════════════════════════════════════════════
        // 4️⃣ DIBUJAR BRILLO EN LA PARTE SUPERIOR
        // ════════════════════════════════════════════════════════════
        float glowHeight = BAR_HEIGHT * 0.3f;
        float[] glowVertices = {
            leftX, barY + halfH - glowHeight, z + 0.003f,
            rightX, barY + halfH - glowHeight, z + 0.003f,
            leftX, barY + halfH, z + 0.003f,
            rightX, barY + halfH, z + 0.003f,
        };

        vertexBuffer.clear();
        vertexBuffer.put(glowVertices);
        vertexBuffer.position(0);

        GLES30.glUniform4f(uColorHandle, 1.0f, 1.0f, 1.0f, 0.3f);
        GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glDisableVertexAttribArray(aPositionHandle);

        // Restaurar depth test
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
    }

    /**
     * Dibujar segmentos individuales (estilo retro)
     * Cada vida = un segmento
     */
    public void drawSegmented(float x, float y, float z, float[] mvpMatrix) {
        if (currentHP <= 0) return;

        GLES30.glUseProgram(shaderProgram);
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        float barY = y + OFFSET_Y;
        float segmentWidth = BAR_WIDTH / maxHP;
        float gap = 0.015f;  // Espacio entre segmentos

        // Fondo negro
        float bgHalfW = BAR_WIDTH / 2f + BORDER_SIZE;
        float bgHalfH = BAR_HEIGHT / 2f + BORDER_SIZE;

        float[] bgVertices = {
            x - bgHalfW, barY - bgHalfH, z,
            x + bgHalfW, barY - bgHalfH, z,
            x - bgHalfW, barY + bgHalfH, z,
            x + bgHalfW, barY + bgHalfH, z,
        };

        vertexBuffer.clear();
        vertexBuffer.put(bgVertices);
        vertexBuffer.position(0);

        GLES30.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLES30.glUniform4f(uColorHandle, 0.0f, 0.0f, 0.0f, 0.8f);

        GLES30.glEnableVertexAttribArray(aPositionHandle);
        GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        // Dibujar cada segmento
        float halfH = BAR_HEIGHT / 2f;
        float startX = x - BAR_WIDTH / 2f;

        for (int i = 0; i < maxHP; i++) {
            float segLeft = startX + i * segmentWidth + gap / 2f;
            float segRight = segLeft + segmentWidth - gap;

            float[] segVertices = {
                segLeft, barY - halfH, z + 0.001f,
                segRight, barY - halfH, z + 0.001f,
                segLeft, barY + halfH, z + 0.001f,
                segRight, barY + halfH, z + 0.001f,
            };

            vertexBuffer.clear();
            vertexBuffer.put(segVertices);
            vertexBuffer.position(0);

            if (i < currentHP) {
                // Segmento activo
                if (isEnemy) {
                    // OVNI: Verde → Rojo
                    float hpRatio = (float)(i + 1) / maxHP;
                    float r = 1.0f - hpRatio;
                    float g = hpRatio;
                    GLES30.glUniform4f(uColorHandle, r, g, 0.2f, 1.0f);
                } else {
                    // Defender: Cyan
                    GLES30.glUniform4f(uColorHandle, 0.2f, 0.9f, 1.0f, 1.0f);
                }
            } else {
                // Segmento vacío (gris oscuro)
                GLES30.glUniform4f(uColorHandle, 0.15f, 0.15f, 0.15f, 0.9f);
            }

            GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        }

        GLES30.glDisableVertexAttribArray(aPositionHandle);
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
    }
}
