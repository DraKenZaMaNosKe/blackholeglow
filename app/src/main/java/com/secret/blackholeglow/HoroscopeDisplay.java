package com.secret.blackholeglow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║   ✨ HoroscopeDisplay - Visualización Mágica del Horóscopo        ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * CARACTERÍSTICAS:
 * - Aparece/desaparece suavemente con fade
 * - Símbolo zodiacal con glow pulsante
 * - Texto del horóscopo con efecto de brillo
 * - Aparece periódicamente (cada 2-3 minutos)
 * - Se oculta después de 15 segundos
 */
public class HoroscopeDisplay implements SceneObject {
    private static final String TAG = "HoroscopeDisplay";

    // Timing
    private static final float FADE_DURATION = 1.5f;
    private static final float VISIBLE_DURATION = 15.0f;     // 15 segundos visible
    private static final float SHOW_INTERVAL = 150.0f;       // Cada 2.5 minutos
    private static final float INITIAL_DELAY = 8.0f;         // Esperar 8 segundos al inicio

    // Estado
    private enum State { HIDDEN, FADING_IN, VISIBLE, FADING_OUT }
    private State currentState = State.HIDDEN;
    private float stateTimer = 0f;
    private float timeSinceLastShow = -INITIAL_DELAY;  // Negativo para delay inicial
    private float alpha = 0f;
    private float time = 0f;
    private boolean showOnFirstLoad = true;

    // Datos del horóscopo
    private String horoscopeText = "";
    private String zodiacSymbol = "♈";
    private String zodiacName = "Aries";
    private boolean hasContent = false;

    // Texturas
    private int symbolTextureId = -1;
    private int textTextureId = -1;
    private Bitmap symbolBitmap;
    private Bitmap textBitmap;
    private Canvas symbolCanvas;
    private Canvas textCanvas;

    // Dimensiones - Textura VERTICAL para pantalla portrait
    private static final int SYMBOL_TEX_SIZE = 128;
    private static final int TEXT_TEX_WIDTH = 420;   // Ancho
    private static final int TEXT_TEX_HEIGHT = 750;  // Altura máxima

    // Altura real calculada del contenido (se actualiza en updateTextTexture)
    private float contentHeightRatio = 0.45f;  // Proporción altura/ancho del contenido real

    // OpenGL
    private int shaderProgram = 0;
    private FloatBuffer vertexBuffer;
    private float aspectRatio = 1.0f;

    // Posiciones (coordenadas normalizadas) - Para pantalla VERTICAL
    private float symbolY = 0.72f;   // Símbolo arriba del cuadro
    private float textY = 0.15f;     // Cuadro centrado-arriba

    // Context
    private final Context context;
    private HoroscopeManager horoscopeManager;
    private boolean needsTextureUpdate = true;

    // Shader sources
    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "in vec2 a_Position;\n" +
        "in vec2 a_TexCoord;\n" +
        "out vec2 v_TexCoord;\n" +
        "void main() {\n" +
        "    v_TexCoord = a_TexCoord;\n" +
        "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "in vec2 v_TexCoord;\n" +
        "out vec4 fragColor;\n" +
        "uniform sampler2D u_Texture;\n" +
        "uniform float u_Alpha;\n" +
        "uniform float u_Time;\n" +
        "uniform float u_IsSymbol;\n" +
        "\n" +
        "void main() {\n" +
        "    vec4 texColor = texture(u_Texture, v_TexCoord);\n" +
        "    \n" +
        "    // Colores místicos\n" +
        "    vec3 color1 = vec3(0.6, 0.3, 1.0);  // Púrpura\n" +
        "    vec3 color2 = vec3(0.2, 0.8, 1.0);  // Cyan\n" +
        "    vec3 color3 = vec3(1.0, 0.6, 0.8);  // Rosa\n" +
        "    \n" +
        "    float pulse = sin(u_Time * 2.0) * 0.15 + 0.85;\n" +
        "    float shimmer = sin(v_TexCoord.x * 10.0 + u_Time * 3.0) * 0.1 + 0.9;\n" +
        "    \n" +
        "    vec3 gradientColor;\n" +
        "    if (u_IsSymbol > 0.5) {\n" +
        "        // Símbolo: pulso más intenso y dorado\n" +
        "        float symbolPulse = sin(u_Time * 3.0) * 0.3 + 0.7;\n" +
        "        gradientColor = mix(vec3(1.0, 0.8, 0.3), color1, sin(u_Time) * 0.5 + 0.5);\n" +
        "        pulse = symbolPulse + 0.3;\n" +
        "    } else {\n" +
        "        // Texto: gradiente horizontal suave\n" +
        "        float t = sin(u_Time * 0.5) * 0.5 + 0.5;\n" +
        "        gradientColor = mix(color1, color2, v_TexCoord.x + t * 0.3);\n" +
        "        gradientColor = mix(gradientColor, color3, sin(v_TexCoord.y * 3.14159) * 0.3);\n" +
        "    }\n" +
        "    \n" +
        "    vec3 finalColor = gradientColor * texColor.rgb * pulse * shimmer;\n" +
        "    \n" +
        "    // Agregar glow\n" +
        "    finalColor += gradientColor * texColor.a * 0.15;\n" +
        "    \n" +
        "    fragColor = vec4(finalColor, texColor.a * u_Alpha);\n" +
        "}\n";

    public HoroscopeDisplay(Context context) {
        this.context = context;
        this.horoscopeManager = HoroscopeManager.getInstance(context);

        initBitmaps();
        initOpenGL();

        // Cargar horóscopo
        loadHoroscope();

        Log.d(TAG, "✨ HoroscopeDisplay inicializado");
    }

    private void initBitmaps() {
        symbolBitmap = Bitmap.createBitmap(SYMBOL_TEX_SIZE, SYMBOL_TEX_SIZE, Bitmap.Config.ARGB_8888);
        symbolCanvas = new Canvas(symbolBitmap);

        textBitmap = Bitmap.createBitmap(TEXT_TEX_WIDTH, TEXT_TEX_HEIGHT, Bitmap.Config.ARGB_8888);
        textCanvas = new Canvas(textBitmap);
    }

    private void initOpenGL() {
        int vs = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        if (vs == 0 || fs == 0) {
            Log.e(TAG, "Error compilando shaders");
            return;
        }

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vs);
        GLES30.glAttachShader(shaderProgram, fs);
        GLES30.glLinkProgram(shaderProgram);

        // Crear vertex buffer
        float[] vertices = {
            -1f, -1f, 0f, 1f,
             1f, -1f, 1f, 1f,
            -1f,  1f, 0f, 0f,
             1f,  1f, 1f, 0f
        };

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // Crear texturas
        int[] texIds = new int[2];
        GLES30.glGenTextures(2, texIds, 0);
        symbolTextureId = texIds[0];
        textTextureId = texIds[1];

        setupTexture(symbolTextureId);
        setupTexture(textTextureId);
    }

    private void setupTexture(int texId) {
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
    }

    private int compileShader(int type, String code) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, code);
        GLES30.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Shader error: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    /**
     * Carga el horóscopo desde HoroscopeManager
     */
    private void loadHoroscope() {
        if (!horoscopeManager.hasValidSign()) {
            Log.d(TAG, "📅 Usuario sin fecha de nacimiento, horóscopo deshabilitado");
            return;
        }

        horoscopeManager.getWeeklyHoroscope(new HoroscopeManager.HoroscopeCallback() {
            @Override
            public void onHoroscopeReady(String horoscope, HoroscopeManager.ZodiacSign sign) {
                horoscopeText = horoscope;
                zodiacSymbol = sign.symbol;
                zodiacName = sign.name;
                hasContent = true;
                needsTextureUpdate = true;

                Log.d(TAG, "✨ Horóscopo cargado para " + zodiacName);
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "⚠️ Error cargando horóscopo: " + error);
                hasContent = false;
            }
        });
    }

    /**
     * Muestra el horóscopo
     */
    public void show() {
        if (!hasContent) return;
        if (currentState == State.VISIBLE || currentState == State.FADING_IN) return;

        currentState = State.FADING_IN;
        stateTimer = 0f;
        Log.d(TAG, "✨ Mostrando horóscopo de " + zodiacName);
    }

    /**
     * Oculta el horóscopo
     */
    public void hide() {
        if (currentState == State.HIDDEN || currentState == State.FADING_OUT) return;

        currentState = State.FADING_OUT;
        stateTimer = 0f;
    }

    @Override
    public void update(float deltaTime) {
        time += deltaTime;
        if (time > 62.83f) time -= 62.83f;

        timeSinceLastShow += deltaTime;

        // Mostrar al inicio después del delay
        if (showOnFirstLoad && hasContent && timeSinceLastShow >= 0) {
            showOnFirstLoad = false;
            show();
            timeSinceLastShow = 0f;
        }

        // Mostrar periódicamente
        if (currentState == State.HIDDEN && hasContent && timeSinceLastShow >= SHOW_INTERVAL) {
            timeSinceLastShow = 0f;
            show();
        }

        // Actualizar estado
        switch (currentState) {
            case FADING_IN:
                stateTimer += deltaTime;
                alpha = Math.min(1f, stateTimer / FADE_DURATION);
                if (stateTimer >= FADE_DURATION) {
                    currentState = State.VISIBLE;
                    stateTimer = 0f;
                    alpha = 1f;
                }
                break;

            case VISIBLE:
                stateTimer += deltaTime;
                if (stateTimer >= VISIBLE_DURATION) {
                    currentState = State.FADING_OUT;
                    stateTimer = 0f;
                }
                break;

            case FADING_OUT:
                stateTimer += deltaTime;
                alpha = Math.max(0f, 1f - (stateTimer / FADE_DURATION));
                if (stateTimer >= FADE_DURATION) {
                    currentState = State.HIDDEN;
                    alpha = 0f;
                }
                break;
        }
    }

    @Override
    public void draw() {
        if (currentState == State.HIDDEN || alpha <= 0.01f || !hasContent) return;
        if (shaderProgram == 0) return;

        // Actualizar texturas si es necesario
        if (needsTextureUpdate) {
            updateSymbolTexture();
            updateTextTexture();
            needsTextureUpdate = false;
        }

        GLES30.glUseProgram(shaderProgram);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        int aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        int aTexCoordLoc = GLES30.glGetAttribLocation(shaderProgram, "a_TexCoord");
        int uTextureLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Texture");
        int uAlphaLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Alpha");
        int uTimeLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Time");
        int uIsSymbolLoc = GLES30.glGetUniformLocation(shaderProgram, "u_IsSymbol");

        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glEnableVertexAttribArray(aTexCoordLoc);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glUniform1i(uTextureLoc, 0);
        GLES30.glUniform1f(uAlphaLoc, alpha);
        GLES30.glUniform1f(uTimeLoc, time);

        // Dibujar cuadro del horóscopo (tamaño dinámico basado en contenido)
        GLES30.glUniform1f(uIsSymbolLoc, 0.0f);
        float quadWidth = 0.50f;
        float quadHeight = quadWidth * contentHeightRatio;  // Altura proporcional al contenido
        // Calcular qué porción de la textura usar (solo la parte con contenido)
        float texVMax = (contentHeightRatio * TEXT_TEX_WIDTH) / TEXT_TEX_HEIGHT;
        drawQuadWithTexCoords(textTextureId, textY, quadWidth, quadHeight, texVMax, aPositionLoc, aTexCoordLoc);

        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glDisableVertexAttribArray(aTexCoordLoc);
    }

    private void drawQuadWithTexCoords(int textureId, float posY, float width, float height, float texVMax, int aPos, int aTex) {
        float halfW = width / aspectRatio;
        float halfH = height;

        // texVMax indica hasta dónde llega el contenido en la textura (0 a 1)
        float[] verts = {
            -halfW, posY - halfH, 0f, texVMax,   // Abajo izq - usa texVMax en lugar de 1
             halfW, posY - halfH, 1f, texVMax,   // Abajo der
            -halfW, posY + halfH, 0f, 0f,        // Arriba izq
             halfW, posY + halfH, 1f, 0f         // Arriba der
        };

        vertexBuffer.clear();
        vertexBuffer.put(verts);
        vertexBuffer.position(0);

        GLES30.glVertexAttribPointer(aPos, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer);
        vertexBuffer.position(2);
        GLES30.glVertexAttribPointer(aTex, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
    }

    private void updateSymbolTexture() {
        if (symbolBitmap == null) return;

        symbolBitmap.eraseColor(0x00000000);

        // Glow
        Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setColor(0xFFFFCC00);
        glowPaint.setTextSize(80);
        glowPaint.setTextAlign(Paint.Align.CENTER);
        glowPaint.setMaskFilter(new android.graphics.BlurMaskFilter(15, android.graphics.BlurMaskFilter.Blur.NORMAL));

        // Símbolo principal
        Paint symbolPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        symbolPaint.setColor(0xFFFFFFFF);
        symbolPaint.setTextSize(80);
        symbolPaint.setTextAlign(Paint.Align.CENTER);
        symbolPaint.setTypeface(Typeface.DEFAULT_BOLD);

        float cx = SYMBOL_TEX_SIZE / 2f;
        float cy = SYMBOL_TEX_SIZE / 2f + 25;

        symbolCanvas.drawText(zodiacSymbol, cx, cy, glowPaint);
        symbolCanvas.drawText(zodiacSymbol, cx, cy, symbolPaint);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, symbolTextureId);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, symbolBitmap, 0);
    }

    private void updateTextTexture() {
        if (textBitmap == null) return;

        textBitmap.eraseColor(0x00000000);

        float cx = TEXT_TEX_WIDTH / 2f;
        float margin = 15f;
        float padding = 20f;
        float cornerRadius = 18f;

        float contentLeft = margin + padding;
        float contentWidth = TEXT_TEX_WIDTH - (margin * 2) - (padding * 2);

        // ═══════════════════════════════════════════════════════════
        // PRIMERO: Calcular altura real del contenido
        // ═══════════════════════════════════════════════════════════
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(22);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setShadowLayer(2, 1, 1, 0x88000000);

        // Calcular cuántas líneas necesita el texto
        String[] words = horoscopeText.split(" ");
        int lineCount = 1;
        StringBuilder testLine = new StringBuilder();
        for (String word : words) {
            String test = testLine.toString() + (testLine.length() > 0 ? " " : "") + word;
            if (textPaint.measureText(test) > contentWidth) {
                lineCount++;
                testLine = new StringBuilder(word);
            } else {
                if (testLine.length() > 0) testLine.append(" ");
                testLine.append(word);
            }
        }

        // Calcular altura total del cuadro
        float titleHeight = 50;      // Título
        float subtitleHeight = 28;   // Subtítulo
        float lineDecoHeight = 45;   // Línea decorativa
        float textStartOffset = 35;  // Espacio antes del texto
        float lineHeight = 30;       // Altura por línea de texto
        float bottomPadding = 25;    // Padding inferior

        float totalContentHeight = margin + titleHeight + subtitleHeight + lineDecoHeight +
                                   textStartOffset + (lineCount * lineHeight) + bottomPadding + margin;

        // Limitar altura mínima y máxima
        totalContentHeight = Math.max(totalContentHeight, 200);
        totalContentHeight = Math.min(totalContentHeight, TEXT_TEX_HEIGHT - margin * 2);

        // Guardar ratio para el draw
        contentHeightRatio = totalContentHeight / TEXT_TEX_WIDTH;

        // ═══════════════════════════════════════════════════════════
        // FONDO DEL CUADRO (tamaño dinámico)
        // ═══════════════════════════════════════════════════════════
        android.graphics.RectF boxRect = new android.graphics.RectF(
            margin, margin,
            TEXT_TEX_WIDTH - margin, totalContentHeight
        );

        // Fondo con gradiente púrpura místico
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setShader(new LinearGradient(0, margin, 0, totalContentHeight,
            new int[]{0xDD1a0a2e, 0xEE16082a, 0xEE16082a, 0xDD1a0a2e},
            new float[]{0f, 0.1f, 0.9f, 1f},
            Shader.TileMode.CLAMP));
        textCanvas.drawRoundRect(boxRect, cornerRadius, cornerRadius, bgPaint);

        // Borde brillante
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2);
        borderPaint.setShader(new LinearGradient(0, margin, TEXT_TEX_WIDTH, totalContentHeight,
            new int[]{0xFFAA66FF, 0xFF6644AA, 0xFFAA66FF},
            new float[]{0f, 0.5f, 1f},
            Shader.TileMode.CLAMP));
        textCanvas.drawRoundRect(boxRect, cornerRadius, cornerRadius, borderPaint);

        // Glow externo del borde
        Paint glowBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowBorder.setStyle(Paint.Style.STROKE);
        glowBorder.setStrokeWidth(4);
        glowBorder.setColor(0x44AA66FF);
        glowBorder.setMaskFilter(new android.graphics.BlurMaskFilter(6, android.graphics.BlurMaskFilter.Blur.NORMAL));
        textCanvas.drawRoundRect(boxRect, cornerRadius, cornerRadius, glowBorder);

        // ═══════════════════════════════════════════════════════════
        // TÍTULO: "♊ Géminis"
        // ═══════════════════════════════════════════════════════════
        Paint titleGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
        titleGlow.setColor(0xFFFFCC00);
        titleGlow.setTextSize(32);
        titleGlow.setTextAlign(Paint.Align.CENTER);
        titleGlow.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titleGlow.setMaskFilter(new android.graphics.BlurMaskFilter(5, android.graphics.BlurMaskFilter.Blur.NORMAL));

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(0xFFFFFFFF);
        titlePaint.setTextSize(32);
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setShadowLayer(3, 1, 1, 0xFF000000);

        String title = zodiacSymbol + " " + zodiacName;
        float titleY = margin + titleHeight;
        textCanvas.drawText(title, cx, titleY, titleGlow);
        textCanvas.drawText(title, cx, titleY, titlePaint);

        // Subtítulo
        Paint subPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subPaint.setColor(0xBBFFFFFF);
        subPaint.setTextSize(18);
        subPaint.setTextAlign(Paint.Align.CENTER);
        textCanvas.drawText("✨ Horóscopo Semanal ✨", cx, titleY + subtitleHeight, subPaint);

        // Línea decorativa
        Paint linePaint = new Paint();
        linePaint.setColor(0x66AA66FF);
        linePaint.setStrokeWidth(1);
        float lineY = titleY + lineDecoHeight;
        textCanvas.drawLine(contentLeft, lineY, TEXT_TEX_WIDTH - margin - padding, lineY, linePaint);

        // ═══════════════════════════════════════════════════════════
        // TEXTO DEL HORÓSCOPO (multilínea con word wrap)
        // ═══════════════════════════════════════════════════════════
        StringBuilder line = new StringBuilder();
        float y = lineY + textStartOffset;

        for (String word : words) {
            String test = line.toString() + (line.length() > 0 ? " " : "") + word;
            if (textPaint.measureText(test) > contentWidth) {
                if (line.length() > 0) {
                    textCanvas.drawText(line.toString(), contentLeft, y, textPaint);
                    y += lineHeight;
                }
                line = new StringBuilder(word);
            } else {
                if (line.length() > 0) line.append(" ");
                line.append(word);
            }
        }

        // Última línea
        if (line.length() > 0) {
            textCanvas.drawText(line.toString(), contentLeft, y, textPaint);
        }

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textTextureId);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, textBitmap, 0);
    }

    public void setAspectRatio(float aspect) {
        this.aspectRatio = aspect;
    }

    public boolean isVisible() {
        return currentState != State.HIDDEN && alpha > 0.01f;
    }

    public boolean hasContent() {
        return hasContent;
    }

    public void release() {
        if (symbolTextureId != -1) {
            int[] texIds = {symbolTextureId, textTextureId};
            GLES30.glDeleteTextures(2, texIds, 0);
        }
        if (symbolBitmap != null) symbolBitmap.recycle();
        if (textBitmap != null) textBitmap.recycle();
        if (shaderProgram != 0) GLES30.glDeleteProgram(shaderProgram);
    }
}
