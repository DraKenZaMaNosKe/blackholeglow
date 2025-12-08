package com.secret.blackholeglow.sharing;

import android.content.Context;
import android.opengl.GLES30;
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

    // ⚡ OPTIMIZACIÓN: Matrices y arrays reutilizables (evitar allocations en draw)
    private final float[] modelMatrixCache = new float[16];
    private final float[] finalMatrixCache = new float[16];
    private final float[] currentColorCache = new float[4];
    private final float[] glowColorCache = new float[4];
    private final float[] highlightColorCache = new float[4];
    private final float[] borderColorCache = new float[4];
    private final float[] heartColorCache = new float[4];

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
        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        programId = GLES30.glCreateProgram();
        GLES30.glAttachShader(programId, vertexShader);
        GLES30.glAttachShader(programId, fragmentShader);
        GLES30.glLinkProgram(programId);

        // Obtener handles
        positionHandle = GLES30.glGetAttribLocation(programId, "a_Position");
        mvpMatrixHandle = GLES30.glGetUniformLocation(programId, "u_MVPMatrix");
        colorHandle = GLES30.glGetUniformLocation(programId, "u_Color");

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
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // Usar programa
        GLES30.glUseProgram(programId);

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
        // CAPA 2: CORAZÓN PRINCIPAL (⚡ OPTIMIZADO: usar matrices cacheadas)
        // ═══════════════════════════════════════════════════════════
        android.opengl.Matrix.setIdentityM(modelMatrixCache, 0);
        android.opengl.Matrix.translateM(modelMatrixCache, 0, x, y, 0);
        android.opengl.Matrix.scaleM(modelMatrixCache, 0, size * pulse, size * pulse, 1);

        android.opengl.Matrix.multiplyMM(finalMatrixCache, 0, mvpMatrix, 0, modelMatrixCache, 0);

        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, finalMatrixCache, 0);

        // Determinar color base (⚡ OPTIMIZADO: copiar en lugar de clone)
        float[] sourceColor;
        if (isOnCooldown) {
            sourceColor = colorCooldown;
        } else if (isPressed) {
            sourceColor = colorPressed;
        } else {
            sourceColor = colorNormal;
        }
        System.arraycopy(sourceColor, 0, currentColorCache, 0, 4);

        // Pasar color
        GLES30.glUniform4fv(colorHandle, 1, currentColorCache, 0);

        // Dibujar corazón principal
        GLES30.glEnableVertexAttribArray(positionHandle);
        GLES30.glVertexAttribPointer(positionHandle, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 66);

        // ═══════════════════════════════════════════════════════════
        // CAPA 3: HIGHLIGHT INTERIOR (brillo en el centro)
        // ═══════════════════════════════════════════════════════════
        if (!isOnCooldown) {
            drawHighlight(mvpMatrix, size * pulse * 0.6f);
        }

        // ═══════════════════════════════════════════════════════════
        // CAPA 4: BORDE BRILLANTE
        // ═══════════════════════════════════════════════════════════
        drawBorder(finalMatrixCache);

        GLES30.glDisableVertexAttribArray(positionHandle);
    }

    /**
     * ✨ Dibuja una capa de glow NEÓN con gradiente cyan/rosa
     * ⚡ OPTIMIZADO: Usa matrices y arrays cacheados
     */
    private void drawGlowLayer(float[] mvpMatrix, float glowSize, float alpha) {
        android.opengl.Matrix.setIdentityM(modelMatrixCache, 0);
        android.opengl.Matrix.translateM(modelMatrixCache, 0, x, y, 0);
        android.opengl.Matrix.scaleM(modelMatrixCache, 0, glowSize, glowSize, 1);

        android.opengl.Matrix.multiplyMM(finalMatrixCache, 0, mvpMatrix, 0, modelMatrixCache, 0);

        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, finalMatrixCache, 0);

        // Interpolar entre cyan y rosa según el tiempo para efecto neón pulsante
        float colorMix = (float) (Math.sin(pulsePhase * 2.5) * 0.5 + 0.5);
        glowColorCache[0] = glowCyan[0] * (1 - colorMix) + glowPink[0] * colorMix;
        glowColorCache[1] = glowCyan[1] * (1 - colorMix) + glowPink[1] * colorMix;
        glowColorCache[2] = glowCyan[2] * (1 - colorMix) + glowPink[2] * colorMix;
        glowColorCache[3] = alpha * 0.8f;
        GLES30.glUniform4fv(colorHandle, 1, glowColorCache, 0);

        GLES30.glEnableVertexAttribArray(positionHandle);
        GLES30.glVertexAttribPointer(positionHandle, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 66);
    }

    /**
     * 💫 Dibuja el highlight interior con brillo neón
     * ⚡ OPTIMIZADO: Usa matrices y arrays cacheados
     */
    private void drawHighlight(float[] mvpMatrix, float highlightSize) {
        android.opengl.Matrix.setIdentityM(modelMatrixCache, 0);
        android.opengl.Matrix.translateM(modelMatrixCache, 0, x, y + size * 0.2f, 0);  // Centro-arriba
        android.opengl.Matrix.scaleM(modelMatrixCache, 0, highlightSize * 0.5f, highlightSize * 0.4f, 1);

        android.opengl.Matrix.multiplyMM(finalMatrixCache, 0, mvpMatrix, 0, modelMatrixCache, 0);

        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, finalMatrixCache, 0);

        // Highlight cyan brillante para efecto neón interior
        float highlightPulse = (float) (Math.sin(pulsePhase * 3.0) * 0.2 + 0.5);
        highlightColorCache[0] = 0.5f + glowCyan[0] * 0.5f;
        highlightColorCache[1] = 0.5f + glowCyan[1] * 0.5f;
        highlightColorCache[2] = 0.5f + glowCyan[2] * 0.5f;
        highlightColorCache[3] = highlightPulse;
        GLES30.glUniform4fv(colorHandle, 1, highlightColorCache, 0);

        GLES30.glEnableVertexAttribArray(positionHandle);
        GLES30.glVertexAttribPointer(positionHandle, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 66);
    }

    /**
     * 🔲 Dibuja el borde del corazón con efecto NEÓN brillante
     * ⚡ OPTIMIZADO: Usa array cacheado
     */
    private void drawBorder(float[] matrix) {
        // Borde cyan neón brillante que pulsa
        float borderPulse = (float) (Math.sin(pulsePhase * 4.0) * 0.3 + 0.7);
        borderColorCache[0] = glowCyan[0] * borderPulse + 0.3f;
        borderColorCache[1] = glowCyan[1] * borderPulse + 0.1f;
        borderColorCache[2] = glowCyan[2] * borderPulse;
        borderColorCache[3] = 1.0f;
        GLES30.glUniform4fv(colorHandle, 1, borderColorCache, 0);
        GLES30.glLineWidth(3.0f);  // Borde más grueso para efecto neón
        GLES30.glDrawArrays(GLES30.GL_LINE_LOOP, 1, 65);
    }

    /**
     * ♥ Dibuja el símbolo del corazón en el centro
     * ⚡ OPTIMIZADO: Usa matrices y arrays cacheados
     */
    private void drawHeartSymbol(float[] mvpMatrix, float pulse) {
        // Dibujar un corazón más pequeño en el centro con color más claro
        android.opengl.Matrix.setIdentityM(modelMatrixCache, 0);
        android.opengl.Matrix.translateM(modelMatrixCache, 0, x, y, 0);
        android.opengl.Matrix.scaleM(modelMatrixCache, 0, size * pulse * 0.6f, size * pulse * 0.6f, 1);

        android.opengl.Matrix.multiplyMM(finalMatrixCache, 0, mvpMatrix, 0, modelMatrixCache, 0);

        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, finalMatrixCache, 0);

        heartColorCache[0] = 1.0f;
        heartColorCache[1] = 0.8f;
        heartColorCache[2] = 0.85f;
        heartColorCache[3] = 1.0f;
        GLES30.glUniform4fv(colorHandle, 1, heartColorCache, 0);

        GLES30.glEnableVertexAttribArray(positionHandle);
        GLES30.glVertexAttribPointer(positionHandle, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 34);
        GLES30.glDisableVertexAttribArray(positionHandle);
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
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);
        return shader;
    }

    /**
     * 🗑️ Libera recursos
     */
    public void cleanup() {
        if (programId != 0) {
            GLES30.glDeleteProgram(programId);
            programId = 0;
        }
        isInitialized = false;
    }
}
