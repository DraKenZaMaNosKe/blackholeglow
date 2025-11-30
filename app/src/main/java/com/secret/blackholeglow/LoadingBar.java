package com.secret.blackholeglow;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║   📊 LoadingBar - Barra de Progreso Visual con Glow              ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * Barra de carga elegante con:
 * - Efecto de glow pulsante
 * - Gradiente de colores (cyan → magenta)
 * - Animación suave del progreso
 * - Texto de estado opcional
 *
 * Usa OpenGL ES 2.0 para máxima compatibilidad
 */
public class LoadingBar implements SceneObject {
    private static final String TAG = "LoadingBar";

    // Estado
    private float currentProgress = 0f;      // 0.0 - 1.0
    private float targetProgress = 0f;       // Progreso objetivo (para animación suave)
    private float displayProgress = 0f;      // Progreso mostrado (animado)
    private boolean isVisible = false;
    private boolean isComplete = false;
    private float alpha = 0f;
    private float time = 0f;

    // Callback cuando termina la carga
    private OnLoadingCompleteListener completeListener;

    // Dimensiones (coordenadas normalizadas -1 a 1)
    private float barY = -0.15f;             // Posición Y (debajo del botón play)
    private float barWidth = 0.7f;           // Ancho total (70% de pantalla)
    private float barHeight = 0.025f;        // Alto de la barra
    private float aspectRatio = 1.0f;

    // OpenGL
    private int shaderProgram = 0;
    private int aPositionLoc = -1;
    private int uColorLoc = -1;
    private int uAlphaLoc = -1;
    private int uTimeLoc = -1;
    private int uProgressLoc = -1;
    private int uIsGlowLoc = -1;
    private FloatBuffer vertexBuffer;

    // Colores
    private static final float[] COLOR_CYAN = {0.0f, 0.9f, 1.0f};
    private static final float[] COLOR_MAGENTA = {1.0f, 0.2f, 0.8f};
    private static final float[] COLOR_BG = {0.1f, 0.1f, 0.15f};

    // Vertex shader
    private static final String VERTEX_SHADER =
        "attribute vec2 aPosition;\n" +
        "void main() {\n" +
        "    gl_Position = vec4(aPosition, 0.0, 1.0);\n" +
        "}\n";

    // Fragment shader con glow y gradiente
    private static final String FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "uniform vec3 uColor;\n" +
        "uniform float uAlpha;\n" +
        "uniform float uTime;\n" +
        "uniform float uProgress;\n" +
        "uniform float uIsGlow;\n" +
        "\n" +
        "void main() {\n" +
        "    if (uIsGlow > 0.5) {\n" +
        "        // Efecto glow pulsante\n" +
        "        float pulse = 0.5 + 0.5 * sin(uTime * 3.0);\n" +
        "        float glowAlpha = uAlpha * 0.4 * pulse;\n" +
        "        gl_FragColor = vec4(uColor, glowAlpha);\n" +
        "    } else {\n" +
        "        // Barra sólida con brillo\n" +
        "        float shine = 0.9 + 0.1 * sin(uTime * 2.0);\n" +
        "        gl_FragColor = vec4(uColor * shine, uAlpha);\n" +
        "    }\n" +
        "}\n";

    public LoadingBar() {
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
        uProgressLoc = GLES20.glGetUniformLocation(shaderProgram, "uProgress");
        uIsGlowLoc = GLES20.glGetUniformLocation(shaderProgram, "uIsGlow");

        // Crear vertex buffer (lo actualizamos en cada draw)
        ByteBuffer bb = ByteBuffer.allocateDirect(8 * 4);  // 4 vertices * 2 floats * 4 bytes
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();

        // Eliminar shaders (ya están en el programa)
        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);

        Log.d(TAG, "✓ LoadingBar OpenGL inicializado");
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
            alpha = Math.min(1f, alpha + dt * 4f);  // Fade in rápido
        } else if (!isVisible && alpha > 0f) {
            alpha = Math.max(0f, alpha - dt * 4f);  // Fade out rápido
        }

        // Animación suave del progreso
        if (displayProgress < targetProgress) {
            displayProgress = Math.min(targetProgress, displayProgress + dt * 0.8f);  // ~1.25 segundos para llenar
        }

        // Detectar cuando llega al 100%
        if (displayProgress >= 0.99f && !isComplete) {
            isComplete = true;
            if (completeListener != null) {
                completeListener.onLoadingComplete();
            }
        }
    }

    @Override
    public void draw() {
        if (alpha <= 0.01f || shaderProgram == 0) return;

        GLES20.glUseProgram(shaderProgram);

        // Uniforms comunes
        GLES20.glUniform1f(uTimeLoc, time);
        GLES20.glUniform1f(uProgressLoc, displayProgress);

        // Calcular dimensiones ajustadas por aspect ratio
        float adjustedHeight = barHeight;
        float adjustedWidth = barWidth;
        if (aspectRatio > 1f) {
            adjustedHeight = barHeight * aspectRatio;
        } else {
            adjustedWidth = barWidth / aspectRatio;
        }

        // ═══════════════════════════════════════════════════════════
        // 1. GLOW EXTERIOR (más grande, transparente)
        // ═══════════════════════════════════════════════════════════
        float glowPadding = 0.015f;
        drawQuad(
            -adjustedWidth/2 - glowPadding,
            barY - adjustedHeight - glowPadding,
            adjustedWidth + glowPadding*2,
            adjustedHeight*2 + glowPadding*2,
            COLOR_CYAN,
            alpha * 0.3f,
            true
        );

        // ═══════════════════════════════════════════════════════════
        // 2. FONDO DE LA BARRA (gris oscuro)
        // ═══════════════════════════════════════════════════════════
        drawQuad(
            -adjustedWidth/2,
            barY - adjustedHeight,
            adjustedWidth,
            adjustedHeight*2,
            COLOR_BG,
            alpha * 0.9f,
            false
        );

        // ═══════════════════════════════════════════════════════════
        // 3. BARRA DE PROGRESO (gradiente cyan → magenta)
        // ═══════════════════════════════════════════════════════════
        if (displayProgress > 0.01f) {
            float progressWidth = adjustedWidth * displayProgress;

            // Interpolar color según progreso
            float[] progressColor = new float[3];
            for (int i = 0; i < 3; i++) {
                progressColor[i] = COLOR_CYAN[i] + (COLOR_MAGENTA[i] - COLOR_CYAN[i]) * displayProgress;
            }

            drawQuad(
                -adjustedWidth/2,
                barY - adjustedHeight,
                progressWidth,
                adjustedHeight*2,
                progressColor,
                alpha,
                false
            );

            // ═══════════════════════════════════════════════════════════
            // 4. BRILLO EN EL BORDE DEL PROGRESO
            // ═══════════════════════════════════════════════════════════
            float edgeX = -adjustedWidth/2 + progressWidth;
            float edgeWidth = 0.02f;
            drawQuad(
                edgeX - edgeWidth/2,
                barY - adjustedHeight,
                edgeWidth,
                adjustedHeight*2,
                new float[]{1f, 1f, 1f},
                alpha * 0.6f * (0.7f + 0.3f * (float)Math.sin(time * 5f)),
                false
            );
        }

        // ═══════════════════════════════════════════════════════════
        // 5. BORDE EXTERIOR (línea fina)
        // ═══════════════════════════════════════════════════════════
        // Top
        drawQuad(-adjustedWidth/2, barY + adjustedHeight, adjustedWidth, 0.003f, COLOR_CYAN, alpha * 0.5f, false);
        // Bottom
        drawQuad(-adjustedWidth/2, barY - adjustedHeight - 0.003f, adjustedWidth, 0.003f, COLOR_CYAN, alpha * 0.5f, false);
        // Left
        drawQuad(-adjustedWidth/2 - 0.003f, barY - adjustedHeight, 0.003f, adjustedHeight*2, COLOR_CYAN, alpha * 0.5f, false);
        // Right
        drawQuad(adjustedWidth/2, barY - adjustedHeight, 0.003f, adjustedHeight*2, COLOR_CYAN, alpha * 0.5f, false);
    }

    private void drawQuad(float x, float y, float w, float h, float[] color, float quadAlpha, boolean isGlow) {
        // Definir vertices del quad
        float[] vertices = {
            x,     y,      // Bottom-left
            x + w, y,      // Bottom-right
            x,     y + h,  // Top-left
            x + w, y + h   // Top-right
        };

        vertexBuffer.clear();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // Uniforms
        GLES20.glUniform3fv(uColorLoc, 1, color, 0);
        GLES20.glUniform1f(uAlphaLoc, quadAlpha);
        GLES20.glUniform1f(uIsGlowLoc, isGlow ? 1f : 0f);

        // Vertex attribute
        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        // Dibujar
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(aPositionLoc);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // API Pública
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Muestra la barra de carga
     */
    public void show() {
        isVisible = true;
        isComplete = false;
        displayProgress = 0f;
        targetProgress = 0f;
        Log.d(TAG, "📊 LoadingBar visible");
    }

    /**
     * Oculta la barra de carga
     */
    public void hide() {
        isVisible = false;
        Log.d(TAG, "📊 LoadingBar oculta");
    }

    /**
     * Establece el progreso (0.0 - 1.0)
     * La barra animará suavemente hacia este valor
     */
    public void setProgress(float progress) {
        targetProgress = Math.max(0f, Math.min(1f, progress));
    }

    /**
     * Establece el progreso inmediatamente sin animación
     */
    public void setProgressImmediate(float progress) {
        targetProgress = Math.max(0f, Math.min(1f, progress));
        displayProgress = targetProgress;
    }

    /**
     * @return Progreso actual (0.0 - 1.0)
     */
    public float getProgress() {
        return displayProgress;
    }

    /**
     * @return true si la carga está completa
     */
    public boolean isComplete() {
        return isComplete;
    }

    /**
     * @return true si la barra está visible
     */
    public boolean isVisible() {
        return isVisible && alpha > 0.01f;
    }

    /**
     * Configura el aspect ratio para ajustar proporciones
     */
    public void setAspectRatio(float ratio) {
        this.aspectRatio = ratio;
    }

    /**
     * Configura la posición Y de la barra
     */
    public void setPositionY(float y) {
        this.barY = y;
    }

    /**
     * Resetea la barra para una nueva carga
     */
    public void reset() {
        displayProgress = 0f;
        targetProgress = 0f;
        isComplete = false;
        time = 0f;
    }

    /**
     * Listener para cuando la carga termina
     */
    public interface OnLoadingCompleteListener {
        void onLoadingComplete();
    }

    public void setOnLoadingCompleteListener(OnLoadingCompleteListener listener) {
        this.completeListener = listener;
    }

    /**
     * Libera recursos OpenGL
     */
    public void release() {
        if (shaderProgram != 0) {
            GLES20.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
    }
}
