package com.secret.blackholeglow.sharing;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * 💜 BOTÓN DE LIKE NEÓN - Estilo Cyberpunk
 *
 * Botón con forma de corazón que permite compartir canciones.
 * Renderizado con OpenGL ES 2.0.
 *
 * Estilo Visual:
 * - Glow neón cyan/rosa que pulsa suavemente
 * - Interior semi-transparente rosa neón
 * - Borde brillante cyan que parpadea
 * - Tamaño compacto (no intrusivo)
 *
 * Características:
 * - Animación de pulso con interpolación de colores
 * - Detección de toques con feedback visual
 * - Estado de cooldown visual (gris apagado)
 * - Múltiples capas de glow para efecto neón profundo
 */
public class LikeButton {
    private static final String TAG = "LikeButton";

    // Posición en coordenadas normalizadas (-1 a 1)
    private float x = 0.92f;    // Pegado al borde derecho
    private float y = -0.55f;   // Un poco más arriba

    // Tamaño del botón (MICRO)
    private float size = 0.012f;  // Micro - como un icono pequeño

    // Estado
    private boolean isPressed = false;
    private boolean isOnCooldown = false;
    private float pulsePhase = 0f;

    // OpenGL
    private int programId;
    private FloatBuffer vertexBuffer;
    private boolean isInitialized = false;

    // Colores NEÓN Cyberpunk
    private float[] colorNormal = {1.0f, 0.0f, 0.5f, 0.85f};    // Rosa neón semi-transparente
    private float[] colorPressed = {0.0f, 0.85f, 1.0f, 1.0f};   // Cyan brillante al presionar
    private float[] colorCooldown = {0.3f, 0.3f, 0.35f, 0.6f};  // Gris apagado

    // Colores para glow neón
    private float[] glowCyan = {0.0f, 0.85f, 1.0f};    // #00D9FF
    private float[] glowPink = {1.0f, 0.0f, 0.5f};     // #FF0080

    // Shaders
    private static final String VERTEX_SHADER =
            "attribute vec4 a_Position;\n" +
            "uniform mat4 u_MVPMatrix;\n" +
            "void main() {\n" +
            "    gl_Position = u_MVPMatrix * a_Position;\n" +
            "}";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform vec4 u_Color;\n" +
            "void main() {\n" +
            "    gl_FragColor = u_Color;\n" +
            "}";

    // Handles
    private int positionHandle;
    private int mvpMatrixHandle;
    private int colorHandle;

    public LikeButton() {
        // Constructor vacío - inicialización en init()
    }

    /**
     * 🎨 Inicializa los recursos de OpenGL
     * Debe llamarse en el hilo de OpenGL (onSurfaceCreated)
     */
    public void init() {
        if (isInitialized) return;

        // Crear programa de shaders
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        programId = GLES20.glCreateProgram();
        GLES20.glAttachShader(programId, vertexShader);
        GLES20.glAttachShader(programId, fragmentShader);
        GLES20.glLinkProgram(programId);

        // Obtener handles
        positionHandle = GLES20.glGetAttribLocation(programId, "a_Position");
        mvpMatrixHandle = GLES20.glGetUniformLocation(programId, "u_MVPMatrix");
        colorHandle = GLES20.glGetUniformLocation(programId, "u_Color");

        // Crear geometría del corazón (simplificado como círculo con pico)
        createHeartGeometry();

        isInitialized = true;
        Log.d(TAG, "♥ LikeButton inicializado");
    }

    /**
     * 💖 Crea la geometría del corazón estilo TikTok
     */
    private void createHeartGeometry() {
        // Forma de corazón usando ecuación paramétrica
        int segments = 64;
        float[] vertices = new float[(segments + 2) * 2];

        // Centro
        vertices[0] = 0f;
        vertices[1] = -0.1f;  // Ligeramente abajo para centrar

        // Puntos del corazón
        for (int i = 0; i <= segments; i++) {
            float t = (float) (2.0 * Math.PI * i / segments);

            // Ecuación paramétrica del corazón
            float x = (float) (16.0 * Math.pow(Math.sin(t), 3));
            float y = (float) (13.0 * Math.cos(t) - 5.0 * Math.cos(2*t)
                              - 2.0 * Math.cos(3*t) - Math.cos(4*t));

            // Normalizar a rango -1 a 1 y ajustar orientación
            vertices[(i + 1) * 2] = x / 17.0f;
            vertices[(i + 1) * 2 + 1] = y / 17.0f;
        }

        // Crear buffer
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);
    }

    /**
     * 🎬 Dibuja el botón con efectos de glow
     */
    public void draw(float[] mvpMatrix, float time) {
        if (!isInitialized) return;

        // Habilitar blending para transparencia
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Usar programa
        GLES20.glUseProgram(programId);

        // Actualizar fase de pulso - más pronunciado
        pulsePhase = time;
        float pulse = 1.0f + (float) Math.sin(pulsePhase * 3.0) * 0.12f;
        float glowPulse = 1.0f + (float) Math.sin(pulsePhase * 2.0) * 0.15f;

        // ═══════════════════════════════════════════════════════════
        // CAPA 1: GLOW EXTERIOR (más grande, muy transparente)
        // ═══════════════════════════════════════════════════════════
        if (!isOnCooldown) {
            drawGlowLayer(mvpMatrix, size * 1.8f * glowPulse, 0.15f);
            drawGlowLayer(mvpMatrix, size * 1.5f * glowPulse, 0.25f);
            drawGlowLayer(mvpMatrix, size * 1.25f * glowPulse, 0.35f);
        }

        // ═══════════════════════════════════════════════════════════
        // CAPA 2: CORAZÓN PRINCIPAL
        // ═══════════════════════════════════════════════════════════
        float[] modelMatrix = new float[16];
        android.opengl.Matrix.setIdentityM(modelMatrix, 0);
        android.opengl.Matrix.translateM(modelMatrix, 0, x, y, 0);
        android.opengl.Matrix.scaleM(modelMatrix, 0, size * pulse, size * pulse, 1);

        float[] finalMatrix = new float[16];
        android.opengl.Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0);

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, finalMatrix, 0);

        // Determinar color base
        float[] currentColor;
        if (isOnCooldown) {
            currentColor = colorCooldown.clone();
        } else if (isPressed) {
            currentColor = colorPressed.clone();
        } else {
            currentColor = colorNormal.clone();
        }

        // Pasar color
        GLES20.glUniform4fv(colorHandle, 1, currentColor, 0);

        // Dibujar corazón principal
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 66);

        // ═══════════════════════════════════════════════════════════
        // CAPA 3: HIGHLIGHT INTERIOR (brillo en el centro)
        // ═══════════════════════════════════════════════════════════
        if (!isOnCooldown) {
            drawHighlight(mvpMatrix, size * pulse * 0.6f);
        }

        // ═══════════════════════════════════════════════════════════
        // CAPA 4: BORDE BRILLANTE
        // ═══════════════════════════════════════════════════════════
        drawBorder(finalMatrix);

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    /**
     * ✨ Dibuja una capa de glow NEÓN con gradiente cyan/rosa
     */
    private void drawGlowLayer(float[] mvpMatrix, float glowSize, float alpha) {
        float[] modelMatrix = new float[16];
        android.opengl.Matrix.setIdentityM(modelMatrix, 0);
        android.opengl.Matrix.translateM(modelMatrix, 0, x, y, 0);
        android.opengl.Matrix.scaleM(modelMatrix, 0, glowSize, glowSize, 1);

        float[] finalMatrix = new float[16];
        android.opengl.Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0);

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, finalMatrix, 0);

        // Interpolar entre cyan y rosa según el tiempo para efecto neón pulsante
        float colorMix = (float) (Math.sin(pulsePhase * 2.5) * 0.5 + 0.5);
        float[] glowColor = {
            glowCyan[0] * (1 - colorMix) + glowPink[0] * colorMix,
            glowCyan[1] * (1 - colorMix) + glowPink[1] * colorMix,
            glowCyan[2] * (1 - colorMix) + glowPink[2] * colorMix,
            alpha * 0.8f  // Más transparente para efecto neón suave
        };
        GLES20.glUniform4fv(colorHandle, 1, glowColor, 0);

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 66);
    }

    /**
     * 💫 Dibuja el highlight interior con brillo neón
     */
    private void drawHighlight(float[] mvpMatrix, float highlightSize) {
        float[] modelMatrix = new float[16];
        android.opengl.Matrix.setIdentityM(modelMatrix, 0);
        android.opengl.Matrix.translateM(modelMatrix, 0, x, y + size * 0.2f, 0);  // Centro-arriba
        android.opengl.Matrix.scaleM(modelMatrix, 0, highlightSize * 0.5f, highlightSize * 0.4f, 1);

        float[] finalMatrix = new float[16];
        android.opengl.Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0);

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, finalMatrix, 0);

        // Highlight cyan brillante para efecto neón interior
        float highlightPulse = (float) (Math.sin(pulsePhase * 3.0) * 0.2 + 0.5);
        float[] highlightColor = {
            0.5f + glowCyan[0] * 0.5f,
            0.5f + glowCyan[1] * 0.5f,
            0.5f + glowCyan[2] * 0.5f,
            highlightPulse
        };
        GLES20.glUniform4fv(colorHandle, 1, highlightColor, 0);

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 66);
    }

    /**
     * 🔲 Dibuja el borde del corazón con efecto NEÓN brillante
     */
    private void drawBorder(float[] matrix) {
        // Borde cyan neón brillante que pulsa
        float borderPulse = (float) (Math.sin(pulsePhase * 4.0) * 0.3 + 0.7);
        float[] borderColor = {
            glowCyan[0] * borderPulse + 0.3f,
            glowCyan[1] * borderPulse + 0.1f,
            glowCyan[2] * borderPulse,
            1.0f
        };
        GLES20.glUniform4fv(colorHandle, 1, borderColor, 0);
        GLES20.glLineWidth(3.0f);  // Borde más grueso para efecto neón
        GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 1, 65);
    }

    /**
     * ♥ Dibuja el símbolo del corazón en el centro
     */
    private void drawHeartSymbol(float[] mvpMatrix, float pulse) {
        // Dibujar un corazón más pequeño en el centro con color más claro
        float[] modelMatrix = new float[16];
        android.opengl.Matrix.setIdentityM(modelMatrix, 0);
        android.opengl.Matrix.translateM(modelMatrix, 0, x, y, 0);
        android.opengl.Matrix.scaleM(modelMatrix, 0, size * pulse * 0.6f, size * pulse * 0.6f, 1);

        float[] finalMatrix = new float[16];
        android.opengl.Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0);

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, finalMatrix, 0);

        float[] heartColor = {1.0f, 0.8f, 0.85f, 1.0f};  // Rosa muy claro
        GLES20.glUniform4fv(colorHandle, 1, heartColor, 0);

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 34);
        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    /**
     * 👆 Verifica si un toque está dentro del botón
     *
     * @param touchX Coordenada X normalizada (-1 a 1)
     * @param touchY Coordenada Y normalizada (-1 a 1)
     * @return true si el toque está dentro del botón
     */
    public boolean isTouched(float touchX, float touchY) {
        float dx = touchX - x;
        float dy = touchY - y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        return distance <= size * 1.2f;  // Margen extra para facilitar el toque
    }

    /**
     * 👇 Maneja el evento de presionar
     */
    public void onPress() {
        isPressed = true;
        Log.d(TAG, "♥ Botón presionado");
    }

    /**
     * 👆 Maneja el evento de soltar
     */
    public void onRelease() {
        isPressed = false;
    }

    /**
     * ⏱️ Establece el estado de cooldown
     */
    public void setCooldown(boolean cooldown) {
        isOnCooldown = cooldown;
    }

    /**
     * 📍 Establece la posición del botón
     */
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    /**
     * 📐 Establece el tamaño del botón
     */
    public void setSize(float size) {
        this.size = size;
    }

    /**
     * 📍 Obtiene la posición X del botón
     */
    public float getX() {
        return x;
    }

    /**
     * 📍 Obtiene la posición Y del botón
     */
    public float getY() {
        return y;
    }

    /**
     * 🎨 Carga un shader
     */
    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    /**
     * 🗑️ Libera recursos
     */
    public void cleanup() {
        if (programId != 0) {
            GLES20.glDeleteProgram(programId);
            programId = 0;
        }
        isInitialized = false;
    }
}
