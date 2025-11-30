package com.secret.blackholeglow;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║   ⭕ CircularLoadingRing - Anillo de Carga Circular               ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * Anillo de carga elegante que rodea el botón de Play:
 * - Progreso circular (como los de descarga)
 * - Efecto de glow pulsante
 * - Gradiente cyan → magenta según progreso
 * - Partículas brillantes en el borde del progreso
 *
 * Usa OpenGL ES 2.0
 */
public class CircularLoadingRing implements SceneObject {
    private static final String TAG = "CircularLoadingRing";

    // Estado
    private float currentProgress = 0f;      // 0.0 - 1.0
    private float targetProgress = 0f;       // Progreso objetivo
    private float displayProgress = 0f;      // Progreso mostrado (animado)
    private boolean isVisible = false;
    private boolean isComplete = false;
    private float alpha = 0f;
    private float time = 0f;

    // Callback cuando termina la carga
    private OnLoadingCompleteListener completeListener;

    // Dimensiones del anillo
    private float centerX = 0f;              // Centro X (coordenadas normalizadas)
    private float centerY = 0.05f;           // Centro Y (ligeramente arriba del centro)
    private float outerRadius = 0.28f;       // Radio exterior
    private float innerRadius = 0.22f;       // Radio interior (grosor del anillo)
    private float aspectRatio = 1.0f;

    // Número de segmentos para el círculo
    private static final int SEGMENTS = 72;  // 72 segmentos = suave

    // OpenGL
    private int shaderProgram = 0;
    private int aPositionLoc = -1;
    private int uColorLoc = -1;
    private int uAlphaLoc = -1;
    private int uTimeLoc = -1;
    private FloatBuffer vertexBuffer;

    // Vertex shader
    private static final String VERTEX_SHADER =
        "attribute vec2 aPosition;\n" +
        "void main() {\n" +
        "    gl_Position = vec4(aPosition, 0.0, 1.0);\n" +
        "}\n";

    // Fragment shader con glow
    private static final String FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "uniform vec3 uColor;\n" +
        "uniform float uAlpha;\n" +
        "uniform float uTime;\n" +
        "\n" +
        "void main() {\n" +
        "    float pulse = 0.85 + 0.15 * sin(uTime * 4.0);\n" +
        "    gl_FragColor = vec4(uColor * pulse, uAlpha);\n" +
        "}\n";

    public CircularLoadingRing() {
        initOpenGL();
    }

    private void initOpenGL() {
        // Crear shader program
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

        // Verificar linkeo
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(shaderProgram, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Error linkeando programa: " + GLES20.glGetProgramInfoLog(shaderProgram));
            GLES20.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
            return;
        }

        // Obtener locations
        aPositionLoc = GLES20.glGetAttribLocation(shaderProgram, "aPosition");
        uColorLoc = GLES20.glGetUniformLocation(shaderProgram, "uColor");
        uAlphaLoc = GLES20.glGetUniformLocation(shaderProgram, "uAlpha");
        uTimeLoc = GLES20.glGetUniformLocation(shaderProgram, "uTime");

        // Crear vertex buffer (máximo necesario para el anillo completo + círculo brillante)
        // Cada segmento del arco: 2 triángulos = 6 vertices = 12 floats
        // Círculo pequeño: 16 segmentos * 3 vertices * 2 floats = 96 floats
        // Buffer extra para múltiples draw calls: x4
        int maxFloats = (SEGMENTS * 12 + 96) * 4;
        ByteBuffer bb = ByteBuffer.allocateDirect(maxFloats * 4);  // 4 bytes por float
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();

        // Eliminar shaders (ya están en el programa)
        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);

        Log.d(TAG, "✓ CircularLoadingRing OpenGL inicializado");
    }

    private int compileShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Error compilando shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    @Override
    public void update(float dt) {
        time += dt;

        // Animación de fade in/out
        if (isVisible && alpha < 1f) {
            alpha = Math.min(1f, alpha + dt * 5f);  // Fade in rápido
        } else if (!isVisible && alpha > 0f) {
            alpha = Math.max(0f, alpha - dt * 5f);  // Fade out rápido
        }

        // Animación suave del progreso
        float progressSpeed = 2.0f;  // Velocidad de animación
        if (displayProgress < targetProgress) {
            displayProgress = Math.min(targetProgress, displayProgress + dt * progressSpeed);
        }

        // Detectar cuando llega al 100%
        if (displayProgress >= 0.99f && !isComplete && isVisible) {
            isComplete = true;
            Log.d(TAG, "⭕ Carga circular completa!");
            if (completeListener != null) {
                completeListener.onLoadingComplete();
            }
        }
    }

    @Override
    public void draw() {
        if (alpha <= 0.01f || shaderProgram == 0) return;
        if (displayProgress <= 0.001f) return;  // No dibujar si no hay progreso

        GLES20.glUseProgram(shaderProgram);
        GLES20.glUniform1f(uTimeLoc, time);

        // Ajustar por aspect ratio
        float radiusX = outerRadius;
        float radiusY = outerRadius * aspectRatio;
        float innerRadiusX = innerRadius;
        float innerRadiusY = innerRadius * aspectRatio;

        // ═══════════════════════════════════════════════════════════
        // 1. GLOW EXTERIOR (más grande, transparente)
        // ═══════════════════════════════════════════════════════════
        float glowPulse = 0.3f + 0.2f * (float)Math.sin(time * 3.0);
        drawArc(
            centerX, centerY,
            radiusX + 0.03f, radiusY + 0.03f,
            innerRadiusX - 0.01f, innerRadiusY - 0.01f,
            0f, displayProgress,
            new float[]{0.0f, 0.8f, 1.0f},  // Cyan
            alpha * glowPulse
        );

        // ═══════════════════════════════════════════════════════════
        // 2. FONDO DEL ANILLO (gris oscuro semi-transparente)
        // ═══════════════════════════════════════════════════════════
        drawArc(
            centerX, centerY,
            radiusX, radiusY,
            innerRadiusX, innerRadiusY,
            0f, 1f,  // Círculo completo
            new float[]{0.15f, 0.15f, 0.2f},
            alpha * 0.5f
        );

        // ═══════════════════════════════════════════════════════════
        // 3. ARCO DE PROGRESO (gradiente cyan → magenta)
        // ═══════════════════════════════════════════════════════════
        // Interpolar color según progreso
        float[] progressColor = new float[3];
        float[] cyan = {0.0f, 0.9f, 1.0f};
        float[] magenta = {1.0f, 0.2f, 0.8f};
        for (int i = 0; i < 3; i++) {
            progressColor[i] = cyan[i] + (magenta[i] - cyan[i]) * displayProgress;
        }

        drawArc(
            centerX, centerY,
            radiusX, radiusY,
            innerRadiusX, innerRadiusY,
            0f, displayProgress,
            progressColor,
            alpha
        );

        // ═══════════════════════════════════════════════════════════
        // 4. PUNTO BRILLANTE EN EL FRENTE DEL PROGRESO
        // ═══════════════════════════════════════════════════════════
        if (displayProgress > 0.01f && displayProgress < 0.99f) {
            float angle = (float)(displayProgress * 2.0 * Math.PI) - (float)(Math.PI / 2.0);  // Empezar desde arriba
            float dotRadius = 0.025f;
            float midRadius = (radiusX + innerRadiusX) / 2f;
            float midRadiusY = (radiusY + innerRadiusY) / 2f;

            float dotX = centerX + midRadius * (float)Math.cos(angle);
            float dotY = centerY + midRadiusY * (float)Math.sin(angle);

            // Punto brillante pulsante
            float dotPulse = 0.8f + 0.2f * (float)Math.sin(time * 8.0);
            drawCircle(dotX, dotY, dotRadius, dotRadius * aspectRatio,
                      new float[]{1f, 1f, 1f}, alpha * dotPulse);
        }
    }

    /**
     * Dibuja un arco (porción de anillo)
     */
    private void drawArc(float cx, float cy, float outerRx, float outerRy,
                         float innerRx, float innerRy,
                         float startProgress, float endProgress,
                         float[] color, float arcAlpha) {

        if (endProgress <= startProgress) return;

        // Calcular ángulos (empezar desde arriba, sentido horario)
        float startAngle = (float)(-Math.PI / 2.0 + startProgress * 2.0 * Math.PI);
        float endAngle = (float)(-Math.PI / 2.0 + endProgress * 2.0 * Math.PI);

        int numSegments = (int)(SEGMENTS * (endProgress - startProgress)) + 1;
        numSegments = Math.max(3, numSegments);

        float angleStep = (endAngle - startAngle) / numSegments;

        vertexBuffer.clear();

        for (int i = 0; i < numSegments; i++) {
            float a1 = startAngle + i * angleStep;
            float a2 = startAngle + (i + 1) * angleStep;

            float cos1 = (float)Math.cos(a1);
            float sin1 = (float)Math.sin(a1);
            float cos2 = (float)Math.cos(a2);
            float sin2 = (float)Math.sin(a2);

            // Outer vertices
            float ox1 = cx + outerRx * cos1;
            float oy1 = cy + outerRy * sin1;
            float ox2 = cx + outerRx * cos2;
            float oy2 = cy + outerRy * sin2;

            // Inner vertices
            float ix1 = cx + innerRx * cos1;
            float iy1 = cy + innerRy * sin1;
            float ix2 = cx + innerRx * cos2;
            float iy2 = cy + innerRy * sin2;

            // Triangle 1
            vertexBuffer.put(ox1); vertexBuffer.put(oy1);
            vertexBuffer.put(ix1); vertexBuffer.put(iy1);
            vertexBuffer.put(ox2); vertexBuffer.put(oy2);

            // Triangle 2
            vertexBuffer.put(ox2); vertexBuffer.put(oy2);
            vertexBuffer.put(ix1); vertexBuffer.put(iy1);
            vertexBuffer.put(ix2); vertexBuffer.put(iy2);
        }

        vertexBuffer.position(0);

        // Uniforms
        GLES20.glUniform3fv(uColorLoc, 1, color, 0);
        GLES20.glUniform1f(uAlphaLoc, arcAlpha);

        // Vertex attribute
        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        // Dibujar
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, numSegments * 6);

        GLES20.glDisableVertexAttribArray(aPositionLoc);
    }

    /**
     * Dibuja un círculo sólido (para el punto brillante)
     */
    private void drawCircle(float cx, float cy, float rx, float ry, float[] color, float circleAlpha) {
        int segments = 16;
        vertexBuffer.clear();

        for (int i = 0; i < segments; i++) {
            float a1 = (float)(i * 2.0 * Math.PI / segments);
            float a2 = (float)((i + 1) * 2.0 * Math.PI / segments);

            // Centro
            vertexBuffer.put(cx); vertexBuffer.put(cy);
            // Punto 1
            vertexBuffer.put(cx + rx * (float)Math.cos(a1));
            vertexBuffer.put(cy + ry * (float)Math.sin(a1));
            // Punto 2
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

    // ═══════════════════════════════════════════════════════════════════════════
    // API Pública
    // ═══════════════════════════════════════════════════════════════════════════

    public void show() {
        isVisible = true;
        isComplete = false;
        displayProgress = 0f;
        targetProgress = 0f;
        Log.d(TAG, "⭕ CircularLoadingRing visible");
    }

    public void hide() {
        isVisible = false;
        Log.d(TAG, "⭕ CircularLoadingRing oculto");
    }

    public void setProgress(float progress) {
        targetProgress = Math.max(0f, Math.min(1f, progress));
    }

    public void setProgressImmediate(float progress) {
        targetProgress = Math.max(0f, Math.min(1f, progress));
        displayProgress = targetProgress;
    }

    public float getProgress() {
        return displayProgress;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public boolean isVisible() {
        return isVisible && alpha > 0.01f;
    }

    public void setAspectRatio(float ratio) {
        this.aspectRatio = ratio;
    }

    public void setCenter(float x, float y) {
        this.centerX = x;
        this.centerY = y;
    }

    public void setRadius(float outer, float inner) {
        this.outerRadius = outer;
        this.innerRadius = inner;
    }

    public void reset() {
        displayProgress = 0f;
        targetProgress = 0f;
        isComplete = false;
        time = 0f;
        alpha = 0f;
    }

    public interface OnLoadingCompleteListener {
        void onLoadingComplete();
    }

    public void setOnLoadingCompleteListener(OnLoadingCompleteListener listener) {
        this.completeListener = listener;
    }

    public void release() {
        if (shaderProgram != 0) {
            GLES20.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
    }
}
