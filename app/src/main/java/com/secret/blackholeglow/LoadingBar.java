package com.secret.blackholeglow;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLES30;
import android.opengl.GLUtils;
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
    // ⚡ OPTIMIZACIÓN: Color blanco cacheado para evitar allocation en draw
    private static final float[] COLOR_WHITE = {1f, 1f, 1f};

    // ═══════════════════════════════════════════════════════════════════════════
    // TEXTO "Loading..." animado
    // ═══════════════════════════════════════════════════════════════════════════
    private int textShaderProgram = 0;
    private int textTextureId = 0;
    private Bitmap textBitmap;
    private Canvas textCanvas;
    private Paint textPaint;
    private Paint textGlowPaint;
    private Paint reassuringPaint;
    private int textAPositionLoc = -1;
    private int textATexCoordLoc = -1;
    private int textUTextureLoc = -1;
    private int textUAlphaLoc = -1;
    private FloatBuffer textVertexBuffer;

    private static final int TEXT_TEX_WIDTH = 256;
    private static final int TEXT_TEX_HEIGHT = 96;  // Aumentado para 2 líneas
    private int currentDots = 0;           // 0, 1, 2, 3 para "Loading", "Loading.", "Loading..", "Loading..."
    private float dotAnimTimer = 0f;
    private static final float DOT_ANIM_SPEED = 0.4f;  // segundos por punto

    // Texto del recurso actual (opcional)
    private String currentResourceName = null;

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

    // Shader de texto con textura
    private static final String TEXT_VERTEX_SHADER =
        "attribute vec2 a_Position;\n" +
        "attribute vec2 a_TexCoord;\n" +
        "varying vec2 v_TexCoord;\n" +
        "void main() {\n" +
        "    v_TexCoord = a_TexCoord;\n" +
        "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
        "}\n";

    private static final String TEXT_FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "varying vec2 v_TexCoord;\n" +
        "uniform sampler2D u_Texture;\n" +
        "uniform float u_Alpha;\n" +
        "void main() {\n" +
        "    vec4 texColor = texture2D(u_Texture, v_TexCoord);\n" +
        "    gl_FragColor = vec4(texColor.rgb, texColor.a * u_Alpha);\n" +
        "}\n";

    public LoadingBar() {
        initOpenGL();
        initTextOpenGL();
    }

    private void initOpenGL() {
        // Crear shader program
        int vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        if (vertexShader == 0 || fragmentShader == 0) {
            Log.e(TAG, "Error compilando shaders");
            return;
        }

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vertexShader);
        GLES30.glAttachShader(shaderProgram, fragmentShader);
        GLES30.glLinkProgram(shaderProgram);

        // Verificar linkeo
        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(shaderProgram, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Error linkeando programa: " + GLES30.glGetProgramInfoLog(shaderProgram));
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
            return;
        }

        // Obtener locations
        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "aPosition");
        uColorLoc = GLES30.glGetUniformLocation(shaderProgram, "uColor");
        uAlphaLoc = GLES30.glGetUniformLocation(shaderProgram, "uAlpha");
        uTimeLoc = GLES30.glGetUniformLocation(shaderProgram, "uTime");
        uProgressLoc = GLES30.glGetUniformLocation(shaderProgram, "uProgress");
        uIsGlowLoc = GLES30.glGetUniformLocation(shaderProgram, "uIsGlow");

        // Crear vertex buffer (lo actualizamos en cada draw)
        ByteBuffer bb = ByteBuffer.allocateDirect(8 * 4);  // 4 vertices * 2 floats * 4 bytes
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();

        // Eliminar shaders (ya están en el programa)
        GLES30.glDeleteShader(vertexShader);
        GLES30.glDeleteShader(fragmentShader);

        Log.d(TAG, "✓ LoadingBar OpenGL inicializado");
    }

    private void initTextOpenGL() {
        // Crear bitmap y canvas para el texto
        textBitmap = Bitmap.createBitmap(TEXT_TEX_WIDTH, TEXT_TEX_HEIGHT, Bitmap.Config.ARGB_8888);
        textCanvas = new Canvas(textBitmap);

        // Paint principal para el texto
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(36);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(0xFF00FFFF);  // Cyan

        // Paint para el glow
        textGlowPaint = new Paint(textPaint);
        textGlowPaint.setColor(0xFF00AAFF);
        textGlowPaint.setMaskFilter(new android.graphics.BlurMaskFilter(8, android.graphics.BlurMaskFilter.Blur.NORMAL));

        // Paint para el texto tranquilizador (más pequeño, verde suave)
        reassuringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        reassuringPaint.setTextSize(20);
        reassuringPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        reassuringPaint.setTextAlign(Paint.Align.CENTER);
        reassuringPaint.setColor(0xFF88FF88);  // Verde suave

        // Crear shader program para texto
        int vs = compileShader(GLES30.GL_VERTEX_SHADER, TEXT_VERTEX_SHADER);
        int fs = compileShader(GLES30.GL_FRAGMENT_SHADER, TEXT_FRAGMENT_SHADER);
        if (vs == 0 || fs == 0) {
            Log.e(TAG, "Error compilando shaders de texto");
            return;
        }

        textShaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(textShaderProgram, vs);
        GLES30.glAttachShader(textShaderProgram, fs);
        GLES30.glLinkProgram(textShaderProgram);

        // Obtener locations
        textAPositionLoc = GLES30.glGetAttribLocation(textShaderProgram, "a_Position");
        textATexCoordLoc = GLES30.glGetAttribLocation(textShaderProgram, "a_TexCoord");
        textUTextureLoc = GLES30.glGetUniformLocation(textShaderProgram, "u_Texture");
        textUAlphaLoc = GLES30.glGetUniformLocation(textShaderProgram, "u_Alpha");

        // Vertex buffer para texto (pos + texcoord interleaved)
        // 4 vértices * 4 floats (x, y, u, v) * 4 bytes
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * 4 * 4);
        bb.order(ByteOrder.nativeOrder());
        textVertexBuffer = bb.asFloatBuffer();

        // Crear textura
        int[] textures = new int[1];
        GLES30.glGenTextures(1, textures, 0);
        textTextureId = textures[0];
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textTextureId);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        // Eliminar shaders
        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);

        // Renderizar texto inicial
        updateTextTexture();

        Log.d(TAG, "✓ LoadingBar texto inicializado");
    }

    /**
     * Actualiza la textura del texto "Loading..." con los puntos animados
     * y el mensaje tranquilizador "sí cargará :)"
     */
    private void updateTextTexture() {
        if (textBitmap == null || textCanvas == null) return;

        textBitmap.eraseColor(Color.TRANSPARENT);

        // Construir el texto con puntos animados
        String dots = "";
        for (int i = 0; i < currentDots; i++) {
            dots += ".";
        }
        String loadingText = "Loading" + dots;

        // Añadir nombre del recurso si existe
        if (currentResourceName != null && !currentResourceName.isEmpty()) {
            loadingText = currentResourceName + dots;
        }

        float centerX = TEXT_TEX_WIDTH / 2f;
        float line1Y = TEXT_TEX_HEIGHT / 2f - 5f;   // Línea 1: "Loading..."
        float line2Y = TEXT_TEX_HEIGHT / 2f + 30f;  // Línea 2: "sí cargará :)"

        // Dibujar glow primero (línea 1)
        textCanvas.drawText(loadingText, centerX, line1Y, textGlowPaint);
        // Dibujar texto principal (línea 1)
        textCanvas.drawText(loadingText, centerX, line1Y, textPaint);

        // 😊 Dibujar texto tranquilizador (línea 2)
        String reassuringText = "sí cargará :)";
        textCanvas.drawText(reassuringText, centerX, line2Y, reassuringPaint);

        // Subir textura a GPU
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textTextureId);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, textBitmap, 0);
    }

    private int compileShader(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Error compilando shader: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
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

        // ═══════════════════════════════════════════════════════════════════════════
        // Animación de los puntos "Loading.", "Loading..", "Loading..."
        // ═══════════════════════════════════════════════════════════════════════════
        if (isVisible) {
            dotAnimTimer += dt;
            if (dotAnimTimer >= DOT_ANIM_SPEED) {
                dotAnimTimer = 0f;
                currentDots = (currentDots + 1) % 4;  // 0, 1, 2, 3, 0, 1, 2, 3...
                updateTextTexture();
            }
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

        GLES30.glUseProgram(shaderProgram);

        // Uniforms comunes
        GLES30.glUniform1f(uTimeLoc, time);
        GLES30.glUniform1f(uProgressLoc, displayProgress);

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
            // ⚡ OPTIMIZADO: Usar COLOR_WHITE cacheado
            drawQuad(
                edgeX - edgeWidth/2,
                barY - adjustedHeight,
                edgeWidth,
                adjustedHeight*2,
                COLOR_WHITE,
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

        // ═══════════════════════════════════════════════════════════
        // 6. TEXTO "Loading..." ARRIBA DE LA BARRA
        // ═══════════════════════════════════════════════════════════
        drawText();
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
        GLES30.glUniform3fv(uColorLoc, 1, color, 0);
        GLES30.glUniform1f(uAlphaLoc, quadAlpha);
        GLES30.glUniform1f(uIsGlowLoc, isGlow ? 1f : 0f);

        // Vertex attribute
        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        // Dibujar
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glDisableVertexAttribArray(aPositionLoc);
    }

    /**
     * Dibuja el texto "Loading..." encima de la barra de progreso
     */
    private void drawText() {
        if (textShaderProgram == 0 || textTextureId == 0) return;

        GLES30.glUseProgram(textShaderProgram);

        // Posición del texto (encima de la barra)
        float textY = barY + 0.06f;   // Un poco arriba de la barra
        float textWidth = 0.35f;      // Ancho del texto
        float textHeight = 0.10f;     // Alto del texto (aumentado para 2 líneas)

        // Ajustar por aspect ratio
        if (aspectRatio < 1f) {
            textWidth = textWidth / aspectRatio;
        }

        // Vertices con posición y texcoord interleaved
        float[] vertices = {
            -textWidth, textY,              0f, 1f,   // Bottom-left
            textWidth,  textY,              1f, 1f,   // Bottom-right
            -textWidth, textY + textHeight, 0f, 0f,   // Top-left
            textWidth,  textY + textHeight, 1f, 0f    // Top-right
        };

        textVertexBuffer.clear();
        textVertexBuffer.put(vertices);
        textVertexBuffer.position(0);

        // Activar textura
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textTextureId);
        GLES30.glUniform1i(textUTextureLoc, 0);
        GLES30.glUniform1f(textUAlphaLoc, alpha);

        // Vertex attributes
        GLES30.glEnableVertexAttribArray(textAPositionLoc);
        GLES30.glEnableVertexAttribArray(textATexCoordLoc);

        textVertexBuffer.position(0);
        GLES30.glVertexAttribPointer(textAPositionLoc, 2, GLES30.GL_FLOAT, false, 16, textVertexBuffer);
        textVertexBuffer.position(2);
        GLES30.glVertexAttribPointer(textATexCoordLoc, 2, GLES30.GL_FLOAT, false, 16, textVertexBuffer);

        // Dibujar
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glDisableVertexAttribArray(textAPositionLoc);
        GLES30.glDisableVertexAttribArray(textATexCoordLoc);
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
        dotAnimTimer = 0f;
        currentDots = 0;
        currentResourceName = null;
        updateTextTexture();
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
     * Establece el nombre del recurso que se está cargando
     * (para mostrar qué se está cargando)
     */
    public void setResourceName(String name) {
        this.currentResourceName = name;
        updateTextTexture();
    }

    /**
     * Libera recursos OpenGL
     */
    public void release() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        if (textShaderProgram != 0) {
            GLES30.glDeleteProgram(textShaderProgram);
            textShaderProgram = 0;
        }
        if (textTextureId != 0) {
            int[] textures = {textTextureId};
            GLES30.glDeleteTextures(1, textures, 0);
            textTextureId = 0;
        }
        if (textBitmap != null) {
            textBitmap.recycle();
            textBitmap = null;
        }
    }
}
