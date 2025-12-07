package com.secret.blackholeglow;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║   ⏹️ MiniStopButton - Botón STOP Minimalista                      ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * Botón pequeño y discreto para detener el wallpaper.
 * Se posiciona en la esquina inferior derecha.
 * Diseño minimalista para no sobrecargar la escena.
 *
 * Características:
 * - Tamaño pequeño (no intrusivo)
 * - Semi-transparente
 * - Glow sutil al tocar
 * - Auto-fade después de unos segundos
 */
public class MiniStopButton implements SceneObject {
    private static final String TAG = "MiniStopButton";

    // Posición (esquina superior izquierda - no interfiere con corazón ni nav bar)
    private float posX = -0.85f;  // Coordenada X normalizada - IZQUIERDA
    private float posY = 0.75f;   // Coordenada Y normalizada - ARRIBA
    private float size = 0.08f;   // Tamaño pequeño
    private float aspectRatio = 1.0f;

    // Estado
    private boolean isVisible = true;
    private float alpha = 0.6f;          // Semi-transparente por defecto
    private float targetAlpha = 0.6f;
    private float time = 0f;

    // Auto-hide
    private float idleTimer = 0f;
    private static final float IDLE_TIMEOUT = 5.0f;  // Ocultar después de 5 segundos
    private static final float MIN_ALPHA = 0.3f;     // Alpha mínimo (siempre visible pero sutil)

    // ✨ EFECTO "LLAMA LA ATENCIÓN" - Pulso periódico para que el usuario lo note
    private float attentionTimer = 0f;
    private static final float ATTENTION_INTERVAL = 8.0f;  // Cada 8 segundos
    private static final float ATTENTION_DURATION = 1.5f;  // Dura 1.5 segundos
    private boolean isAttentionActive = false;
    private float attentionScale = 1.0f;  // Escala durante el efecto

    // OpenGL
    private int shaderProgram = 0;
    private int aPositionLoc = -1;
    private int uColorLoc = -1;
    private int uAlphaLoc = -1;
    private int uTimeLoc = -1;
    private FloatBuffer vertexBuffer;

    // ⚡ OPTIMIZACIÓN: Colores cacheados para evitar allocations en draw()
    private final float[] bgColorCache = {0.15f, 0.15f, 0.2f};
    private final float[] attentionColorCache = {1.0f, 0.9f, 0.2f};
    private final float[] cyanColorCache = {0.0f, 0.7f, 0.9f};
    private final float[] redColorCache = {0.9f, 0.3f, 0.3f};
    private final float[] glowColorCache = {1.0f, 0.8f, 0.1f};
    private final float[] quadVerticesCache = new float[8];

    // Vertex shader simple
    private static final String VERTEX_SHADER =
        "attribute vec2 aPosition;\n" +
        "void main() {\n" +
        "    gl_Position = vec4(aPosition, 0.0, 1.0);\n" +
        "}\n";

    // Fragment shader con glow sutil
    private static final String FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "uniform vec3 uColor;\n" +
        "uniform float uAlpha;\n" +
        "uniform float uTime;\n" +
        "\n" +
        "void main() {\n" +
        "    float pulse = 0.9 + 0.1 * sin(uTime * 2.0);\n" +
        "    gl_FragColor = vec4(uColor * pulse, uAlpha);\n" +
        "}\n";

    public MiniStopButton() {
        initOpenGL();
    }

    private void initOpenGL() {
        // Compilar shaders
        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        if (vertexShader == 0 || fragmentShader == 0) {
            Log.e(TAG, "Error compilando shaders");
            return;
        }

        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vertexShader);
        GLES20.glAttachShader(shaderProgram, fragmentShader);
        GLES20.glLinkProgram(shaderProgram);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(shaderProgram, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Error linkeando programa");
            GLES20.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
            return;
        }

        aPositionLoc = GLES20.glGetAttribLocation(shaderProgram, "aPosition");
        uColorLoc = GLES20.glGetUniformLocation(shaderProgram, "uColor");
        uAlphaLoc = GLES20.glGetUniformLocation(shaderProgram, "uAlpha");
        uTimeLoc = GLES20.glGetUniformLocation(shaderProgram, "uTime");

        // Buffer para vertices
        // drawCircle: 24 segments * 3 vertices * 2 floats = 144 floats
        // drawRing: 24 segments * 6 vertices * 2 floats = 288 floats
        // Total máximo necesario: ~300 floats, usar 500 por seguridad
        ByteBuffer bb = ByteBuffer.allocateDirect(500 * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();

        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);

        Log.d(TAG, "✓ MiniStopButton inicializado");
    }

    private int compileShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Error: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    @Override
    public void update(float dt) {
        time += dt;
        if (time > 100f) time = 0f;  // Reset para evitar overflow

        // Auto-fade después de idle
        idleTimer += dt;
        if (idleTimer > IDLE_TIMEOUT) {
            targetAlpha = MIN_ALPHA;
        }

        // Smooth transition de alpha
        if (alpha < targetAlpha) {
            alpha = Math.min(targetAlpha, alpha + dt * 2f);
        } else if (alpha > targetAlpha) {
            alpha = Math.max(targetAlpha, alpha - dt * 2f);
        }

        // ✨ EFECTO "LLAMA LA ATENCIÓN" - Pulso periódico
        attentionTimer += dt;
        if (attentionTimer >= ATTENTION_INTERVAL) {
            // Activar efecto de atención
            isAttentionActive = true;
            attentionTimer = 0f;
        }

        if (isAttentionActive) {
            // Calcular fase del efecto (0 a 1)
            float phase = attentionTimer / ATTENTION_DURATION;
            if (phase >= 1.0f) {
                // Efecto terminado
                isAttentionActive = false;
                attentionScale = 1.0f;
                attentionTimer = 0f;
            } else {
                // Efecto de escala: crece y luego vuelve (easing suave)
                // Usa seno para un movimiento natural
                attentionScale = 1.0f + 0.3f * (float)Math.sin(phase * Math.PI);
                // También aumentar alpha temporalmente
                if (alpha < 0.9f) {
                    alpha = Math.min(0.9f, alpha + dt * 4f);
                }
            }
        }
    }

    @Override
    public void draw() {
        if (!isVisible || shaderProgram == 0 || alpha < 0.01f) return;

        GLES20.glUseProgram(shaderProgram);
        GLES20.glUniform1f(uTimeLoc, time);

        // Ajustar tamaño por aspect ratio Y por escala de atención
        float sizeX = size * attentionScale;
        float sizeY = size * aspectRatio * attentionScale;

        // ═══════════════════════════════════════════════════════════
        // 1. FONDO CIRCULAR (gris oscuro semi-transparente)
        // ⚡ OPTIMIZADO: Usar color cacheado
        // ═══════════════════════════════════════════════════════════
        drawCircle(posX, posY, sizeX, sizeY, bgColorCache, alpha * 0.7f);

        // ═══════════════════════════════════════════════════════════
        // 2. BORDE CIRCULAR - Color especial durante atención (cyan → amarillo)
        // ⚡ OPTIMIZADO: Usar colores cacheados
        // ═══════════════════════════════════════════════════════════
        float[] borderColor = isAttentionActive ? attentionColorCache : cyanColorCache;
        drawRing(posX, posY, sizeX, sizeY, sizeX * 0.85f, sizeY * 0.85f,
                 borderColor, alpha * (isAttentionActive ? 0.9f : 0.5f));

        // ═══════════════════════════════════════════════════════════
        // 3. ICONO STOP (cuadrado pequeño)
        // ⚡ OPTIMIZADO: Usar color cacheado
        // ═══════════════════════════════════════════════════════════
        float iconSize = size * 0.4f * attentionScale;
        drawQuad(posX - iconSize/2, posY - iconSize * aspectRatio/2,
                 iconSize, iconSize * aspectRatio,
                 redColorCache, alpha);  // Rojo suave

        // ═══════════════════════════════════════════════════════════
        // 4. GLOW EXTRA durante atención (anillo exterior difuso)
        // ⚡ OPTIMIZADO: Usar color cacheado
        // ═══════════════════════════════════════════════════════════
        if (isAttentionActive) {
            float glowSize = sizeX * 1.3f;
            float glowSizeY = sizeY * 1.3f;
            drawRing(posX, posY, glowSize, glowSizeY, sizeX, sizeY,
                     glowColorCache, alpha * 0.4f);
        }
    }

    private void drawCircle(float cx, float cy, float rx, float ry, float[] color, float circleAlpha) {
        int segments = 24;
        vertexBuffer.clear();

        for (int i = 0; i < segments; i++) {
            float a1 = (float)(i * 2.0 * Math.PI / segments);
            float a2 = (float)((i + 1) * 2.0 * Math.PI / segments);

            vertexBuffer.put(cx);
            vertexBuffer.put(cy);
            vertexBuffer.put(cx + rx * (float)Math.cos(a1));
            vertexBuffer.put(cy + ry * (float)Math.sin(a1));
            vertexBuffer.put(cx + rx * (float)Math.cos(a2));
            vertexBuffer.put(cy + ry * (float)Math.sin(a2));
        }

        vertexBuffer.position(0);

        GLES20.glUniform3fv(uColorLoc, 1, color, 0);
        GLES20.glUniform1f(uAlphaLoc, circleAlpha);

        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, segments * 3);
        GLES20.glDisableVertexAttribArray(aPositionLoc);
    }

    private void drawRing(float cx, float cy, float outerRx, float outerRy,
                          float innerRx, float innerRy, float[] color, float ringAlpha) {
        int segments = 24;
        vertexBuffer.clear();

        for (int i = 0; i < segments; i++) {
            float a1 = (float)(i * 2.0 * Math.PI / segments);
            float a2 = (float)((i + 1) * 2.0 * Math.PI / segments);

            float cos1 = (float)Math.cos(a1);
            float sin1 = (float)Math.sin(a1);
            float cos2 = (float)Math.cos(a2);
            float sin2 = (float)Math.sin(a2);

            // Triangle 1
            vertexBuffer.put(cx + outerRx * cos1);
            vertexBuffer.put(cy + outerRy * sin1);
            vertexBuffer.put(cx + innerRx * cos1);
            vertexBuffer.put(cy + innerRy * sin1);
            vertexBuffer.put(cx + outerRx * cos2);
            vertexBuffer.put(cy + outerRy * sin2);

            // Triangle 2
            vertexBuffer.put(cx + outerRx * cos2);
            vertexBuffer.put(cy + outerRy * sin2);
            vertexBuffer.put(cx + innerRx * cos1);
            vertexBuffer.put(cy + innerRy * sin1);
            vertexBuffer.put(cx + innerRx * cos2);
            vertexBuffer.put(cy + innerRy * sin2);
        }

        vertexBuffer.position(0);

        GLES20.glUniform3fv(uColorLoc, 1, color, 0);
        GLES20.glUniform1f(uAlphaLoc, ringAlpha);

        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, segments * 6);
        GLES20.glDisableVertexAttribArray(aPositionLoc);
    }

    private void drawQuad(float x, float y, float w, float h, float[] color, float quadAlpha) {
        // ⚡ OPTIMIZADO: Usar array cacheado en lugar de new float[]
        quadVerticesCache[0] = x;
        quadVerticesCache[1] = y;
        quadVerticesCache[2] = x + w;
        quadVerticesCache[3] = y;
        quadVerticesCache[4] = x;
        quadVerticesCache[5] = y + h;
        quadVerticesCache[6] = x + w;
        quadVerticesCache[7] = y + h;

        vertexBuffer.clear();
        vertexBuffer.put(quadVerticesCache);
        vertexBuffer.position(0);

        GLES20.glUniform3fv(uColorLoc, 1, color, 0);
        GLES20.glUniform1f(uAlphaLoc, quadAlpha);

        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(aPositionLoc);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // API Pública
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Verifica si las coordenadas están dentro del botón
     */
    public boolean isInside(float x, float y) {
        float dx = (x - posX) / size;
        float dy = (y - posY) / (size * aspectRatio);
        return (dx * dx + dy * dy) <= 1.5f;  // Radio con margen
    }

    /**
     * Llamar cuando el usuario interactúa (resetea el idle timer)
     */
    public void onInteraction() {
        idleTimer = 0f;
        targetAlpha = 0.8f;  // Mostrar más brillante temporalmente
    }

    public void show() {
        isVisible = true;
        idleTimer = 0f;
        targetAlpha = 0.6f;
    }

    public void hide() {
        isVisible = false;
    }

    public void setAspectRatio(float ratio) {
        this.aspectRatio = ratio;
    }

    public void setPosition(float x, float y) {
        this.posX = x;
        this.posY = y;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void release() {
        if (shaderProgram != 0) {
            GLES20.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
    }
}
