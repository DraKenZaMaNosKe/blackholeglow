package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * 🔥 BARRA DE COMBO VISUAL ÉPICA
 *
 * Muestra una barra que se llena con cada impacto
 * Cuando llega al máximo... ¡DISPARA EL RAYO LÁSER SUPERPODEROSO!
 *
 * ⚡ CARACTERÍSTICAS:
 * - Barra que se llena progresivamente con combos
 * - Colores dinámicos según nivel de combo
 * - Efectos de parpadeo cuando está casi llena
 * - Trigger para el rayo láser superpoderoso
 * - Texto de combo multiplicador (x2, x3, x5, x10!)
 */
public class ComboBar {
    private static final String TAG = "ComboBar";

    // Configuración de la barra
    private static final float BAR_WIDTH = 0.3f;  // Ancho de la barra (30% pantalla - más pequeña)
    private static final float BAR_HEIGHT = 0.06f; // Alto de la barra (más delgada)
    private static final float BAR_X = -0.85f;     // Posición X (borde izquierdo)
    private static final float BAR_Y = 0.65f;       // Posición Y (centro vertical)

    // Estados del combo
    private static final int MAX_COMBO_FOR_LASER = 5;  // Combo x5 = SUPERDISPARO ÉPICO!
    private float currentFill = 0.0f;  // 0.0 a 1.0
    private float targetFill = 0.0f;
    private int currentCombo = 0;
    private boolean isLaserReady = false;
    private float pulseTime = 0.0f;

    // Colores según nivel
    private final float[] COLOR_LOW = {0.2f, 0.8f, 0.2f, 0.8f};    // Verde
    private final float[] COLOR_MID = {1.0f, 0.8f, 0.0f, 0.9f};    // Amarillo
    private final float[] COLOR_HIGH = {1.0f, 0.4f, 0.0f, 1.0f};   // Naranja
    private final float[] COLOR_MAX = {1.0f, 0.0f, 0.0f, 1.0f};    // Rojo pulsante
    private final float[] COLOR_LASER = {0.0f, 0.8f, 1.0f, 1.0f};  // Cyan eléctrico

    // OpenGL
    private int shaderProgram;
    private FloatBuffer vertexBuffer;
    private FloatBuffer fillVertexBuffer;
    private int positionHandle;
    private int colorHandle;

    // Shader simple para la barra
    private final String vertexShaderCode =
        "attribute vec4 vPosition;" +
        "void main() {" +
        "  gl_Position = vPosition;" +
        "}";

    private final String fragmentShaderCode =
        "precision mediump float;" +
        "uniform vec4 vColor;" +
        "void main() {" +
        "  gl_FragColor = vColor;" +
        "}";

    // Efectos visuales
    private float shakeAmount = 0.0f;
    private float glowIntensity = 0.0f;

    // Texto del combo
    private String comboText = "";
    private float textScale = 1.0f;
    private float textAlpha = 0.0f;

    public ComboBar(Context context) {
        setupShaders();
        setupBuffers();
        Log.d(TAG, "⚡ Barra de combo inicializada");
    }

    private void setupShaders() {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vertexShader);
        GLES20.glAttachShader(shaderProgram, fragmentShader);
        GLES20.glLinkProgram(shaderProgram);

        positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        colorHandle = GLES20.glGetUniformLocation(shaderProgram, "vColor");
    }

    private void setupBuffers() {
        // Vértices para el marco de la barra (rectángulo) - ahora usando BAR_X
        float[] frameCoords = {
            BAR_X, BAR_Y - BAR_HEIGHT/2, 0.0f,            // Bottom left
            BAR_X + BAR_WIDTH, BAR_Y - BAR_HEIGHT/2, 0.0f, // Bottom right
            BAR_X + BAR_WIDTH, BAR_Y + BAR_HEIGHT/2, 0.0f, // Top right
            BAR_X, BAR_Y + BAR_HEIGHT/2, 0.0f,            // Top left
        };

        ByteBuffer bb = ByteBuffer.allocateDirect(frameCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(frameCoords);
        vertexBuffer.position(0);

        // Buffer para el relleno (se actualiza dinámicamente)
        ByteBuffer fillBb = ByteBuffer.allocateDirect(4 * 3 * 4);
        fillBb.order(ByteOrder.nativeOrder());
        fillVertexBuffer = fillBb.asFloatBuffer();
    }

    /**
     * Actualiza el estado del combo
     */
    public void updateCombo(int combo, int score) {
        currentCombo = combo;

        // Calcular el llenado de la barra
        targetFill = Math.min(1.0f, (float)combo / MAX_COMBO_FOR_LASER);

        // Verificar si el láser está listo
        if (combo >= MAX_COMBO_FOR_LASER && !isLaserReady) {
            isLaserReady = true;
            shakeAmount = 0.1f;
            Log.d(TAG, "⚡⚡⚡ RAYO LÁSER LISTO! COMBO x" + combo + " ⚡⚡⚡");
        }

        // Actualizar texto según combo
        if (combo >= 5) {
            comboText = "READY! x" + combo + "!";
            textScale = 2.0f;
            textAlpha = 1.0f;
        } else if (combo == 4) {
            comboText = "ALMOST! x" + combo;
            textScale = 1.6f;
            textAlpha = 1.0f;
        } else if (combo == 3) {
            comboText = "COMBO x" + combo;
            textScale = 1.4f;
            textAlpha = 1.0f;
        } else if (combo == 2) {
            comboText = "x" + combo;
            textScale = 1.2f;
            textAlpha = 0.9f;
        } else if (combo > 0) {
            comboText = "x" + combo;
            textScale = 1.0f;
            textAlpha = 0.8f;
        } else {
            textAlpha = 0.0f;
        }
    }

    /**
     * Resetea el combo (cuando se pierde)
     */
    public void resetCombo() {
        targetFill = 0.0f;
        isLaserReady = false;
        comboText = "";
        textAlpha = 0.0f;
        Log.d(TAG, "Combo perdido - barra reseteada");
    }

    /**
     * Dispara el rayo láser y resetea la barra
     */
    public boolean fireLaser() {
        if (isLaserReady) {
            isLaserReady = false;
            currentCombo = 0;
            targetFill = 0.0f;
            currentFill = 0.0f;
            textAlpha = 0.0f;  // Resetear texto también
            comboText = "";
            Log.d(TAG, "🔥⚡ RAYO LÁSER SUPERPODEROSO DISPARADO! ⚡🔥");
            return true;
        }
        return false;
    }

    /**
     * Actualiza la animación
     */
    public void update(float deltaTime) {
        // Animar el llenado de la barra
        float fillSpeed = 3.0f; // Velocidad de llenado
        if (currentFill < targetFill) {
            currentFill += deltaTime * fillSpeed;
            if (currentFill > targetFill) currentFill = targetFill;
        } else if (currentFill > targetFill) {
            currentFill -= deltaTime * fillSpeed * 0.5f; // Baja más lento
            if (currentFill < targetFill) currentFill = targetFill;
        }

        // Pulso cuando está llena
        if (isLaserReady) {
            pulseTime += deltaTime * 5.0f;
            glowIntensity = 0.5f + 0.5f * (float)Math.sin(pulseTime);
        } else {
            pulseTime = 0.0f;
            glowIntensity = 0.0f;
        }

        // Reducir shake
        if (shakeAmount > 0) {
            shakeAmount -= deltaTime * 2.0f;
            if (shakeAmount < 0) shakeAmount = 0;
        }

        // Fade del texto
        if (textAlpha > 0 && currentCombo == 0) {
            textAlpha -= deltaTime * 2.0f;
            if (textAlpha < 0) textAlpha = 0;
        }
    }

    /**
     * Dibuja la barra de combo
     */
    public void draw() {
        GLES20.glUseProgram(shaderProgram);

        // Aplicar shake si hay
        float shakeX = 0;
        if (shakeAmount > 0) {
            shakeX = (float)(Math.random() - 0.5) * shakeAmount * 0.02f;
        }

        // 1. Dibujar el marco de la barra (contorno)
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer);

        // Color del marco
        GLES20.glUniform4f(colorHandle, 0.3f, 0.3f, 0.3f, 0.8f);
        GLES20.glLineWidth(3.0f);
        GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, 4);

        // 2. Dibujar el relleno de la barra
        if (currentFill > 0) {
            // Actualizar vértices del relleno según currentFill
            float fillWidth = BAR_WIDTH * currentFill;
            float[] fillCoords = {
                BAR_X + 0.01f + shakeX, BAR_Y - BAR_HEIGHT/2 + 0.01f, 0.0f,
                BAR_X + fillWidth - 0.01f + shakeX, BAR_Y - BAR_HEIGHT/2 + 0.01f, 0.0f,
                BAR_X + fillWidth - 0.01f + shakeX, BAR_Y + BAR_HEIGHT/2 - 0.01f, 0.0f,
                BAR_X + 0.01f + shakeX, BAR_Y + BAR_HEIGHT/2 - 0.01f, 0.0f,
            };

            fillVertexBuffer.put(fillCoords);
            fillVertexBuffer.position(0);

            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, fillVertexBuffer);

            // Color según nivel de combo (adaptado para x5)
            float[] color;
            if (isLaserReady) {
                // Parpadeo cyan-blanco cuando está listo (x5)
                float flash = glowIntensity;
                color = new float[] {
                    COLOR_LASER[0] * (1-flash) + flash,
                    COLOR_LASER[1] * (1-flash) + flash,
                    COLOR_LASER[2] * (1-flash) + flash,
                    1.0f
                };
            } else if (currentCombo == 4) {
                color = COLOR_HIGH;  // Naranja para x4 (casi listo)
            } else if (currentCombo == 3) {
                color = COLOR_MID;  // Amarillo para x3
            } else {
                color = COLOR_LOW;  // Verde para x1-2
            }

            GLES20.glUniform4fv(colorHandle, 1, color, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);

            // Efecto de brillo adicional si está casi llena
            if (currentFill > 0.8f) {
                float[] glowColor = {color[0], color[1], color[2], color[3] * 0.3f * glowIntensity};
                GLES20.glUniform4fv(colorHandle, 1, glowColor, 0);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
            }
        }

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    /**
     * Verifica si el láser está listo para disparar
     */
    public boolean isLaserReady() {
        return isLaserReady;
    }

    /**
     * Obtiene el combo actual
     */
    public int getCurrentCombo() {
        return currentCombo;
    }

    /**
     * Obtiene el texto del combo para mostrar
     */
    public String getComboText() {
        return comboText;
    }

    public float getTextScale() {
        return textScale;
    }

    public float getTextAlpha() {
        return textAlpha;
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}