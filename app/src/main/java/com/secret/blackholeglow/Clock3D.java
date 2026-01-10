package com.secret.blackholeglow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
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
import java.util.Calendar;

/**
 * ⏰ RELOJ 3D CON EFECTO GLOW PARA LIVE WALLPAPERS
 *
 * Muestra la hora actual con un efecto de brillo temático
 * - ABYSSIA: Cyan bioluminiscente (#00CED1)
 * - PYRALIS: Dorado celestial (#FFD700)
 *
 * Características:
 * - Actualización automática cada minuto
 * - Efecto glow/bloom suave
 * - Fuente moderna y legible
 * - Posición configurable
 */
public class Clock3D implements SceneObject {
    private static final String TAG = "Clock3D";

    // ═══════════════════════════════════════════════════════════════
    // TEMAS PREDEFINIDOS
    // ═══════════════════════════════════════════════════════════════
    public static final int THEME_ABYSSIA = 0;    // Cyan bioluminiscente
    public static final int THEME_PYRALIS = 1;    // Dorado celestial
    public static final int THEME_KAMEHAMEHA = 2; // Azul energía Ki
    public static final int THEME_SYNTHWAVE = 3;  // Hot Pink retrowave 80s
    public static final int THEME_COSMOS = 4;     // Dorado celestial Saint Seiya

    // Colores por tema
    private static final int COLOR_ABYSSIA_PRIMARY = 0xFF00CED1;   // Dark Turquoise
    private static final int COLOR_ABYSSIA_GLOW = 0xFF00FFFF;      // Cyan
    private static final int COLOR_PYRALIS_PRIMARY = 0xFFFFD700;   // Gold
    private static final int COLOR_PYRALIS_GLOW = 0xFFFF8C00;      // Dark Orange
    private static final int COLOR_KAMEHAMEHA_PRIMARY = 0xFF4FC3F7; // Light Blue
    private static final int COLOR_KAMEHAMEHA_GLOW = 0xFF00E5FF;   // Cyan accent
    private static final int COLOR_SYNTHWAVE_PRIMARY = 0xFFFF1493; // Hot Pink
    private static final int COLOR_SYNTHWAVE_GLOW = 0xFFFF00FF;    // Magenta
    private static final int COLOR_COSMOS_PRIMARY = 0xFFFFD700;    // Gold (cosmos dorado)
    private static final int COLOR_COSMOS_GLOW = 0xFF9370DB;       // Medium Purple (cosmos púrpura)

    // ═══════════════════════════════════════════════════════════════
    // OPENGL
    // ═══════════════════════════════════════════════════════════════
    private int programId;
    private int aPositionLoc;
    private int aTexCoordLoc;
    private int uTextureLoc;
    private int uAlphaLoc;
    private int uGlowIntensityLoc;

    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;

    // ═══════════════════════════════════════════════════════════════
    // TEXTURA Y ESTADO
    // ═══════════════════════════════════════════════════════════════
    private int textureId = -1;
    private String currentTime = "";
    private int lastMinute = -1;
    private boolean needsUpdate = true;

    // ═══════════════════════════════════════════════════════════════
    // POSICIÓN Y TAMAÑO (NDC: -1 a 1)
    // ═══════════════════════════════════════════════════════════════
    private float x, y, width, height;

    // ═══════════════════════════════════════════════════════════════
    // ESTILO
    // ═══════════════════════════════════════════════════════════════
    private Paint textPaint;
    private Paint glowPaint;
    private int primaryColor;
    private int glowColor;
    private float textSize = 72f;
    private float glowIntensity = 1.0f;
    private float glowPulse = 0f;

    // Formato de hora
    private boolean use24HourFormat = true;
    private boolean showSeconds = false;
    private boolean showMilliseconds = false;
    private long lastMillis = -1;

    // ═══════════════════════════════════════════════════════════════
    // SHADERS CON EFECTO GLOW
    // ═══════════════════════════════════════════════════════════════
    private final String vertexShader =
        "attribute vec2 a_Position;\n" +
        "attribute vec2 a_TexCoord;\n" +
        "varying vec2 v_TexCoord;\n" +
        "void main() {\n" +
        "    v_TexCoord = a_TexCoord;\n" +
        "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
        "}\n";

    private final String fragmentShader =
        "precision mediump float;\n" +
        "varying vec2 v_TexCoord;\n" +
        "uniform sampler2D u_Texture;\n" +
        "uniform float u_Alpha;\n" +
        "uniform float u_GlowIntensity;\n" +
        "void main() {\n" +
        "    vec4 texColor = texture2D(u_Texture, v_TexCoord);\n" +
        "    // Intensificar el brillo basado en la intensidad del glow\n" +
        "    float brightness = 1.0 + (u_GlowIntensity * 0.3);\n" +
        "    vec3 finalColor = texColor.rgb * brightness;\n" +
        "    gl_FragColor = vec4(finalColor, texColor.a * u_Alpha);\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════

    /**
     * Constructor con tema
     * @param context Contexto Android
     * @param theme THEME_ABYSSIA o THEME_PYRALIS
     * @param x Posición X (NDC: -1 a 1)
     * @param y Posición Y (NDC: -1 a 1)
     */
    public Clock3D(Context context, int theme, float x, float y) {
        this.x = x;
        this.y = y;
        this.width = 0.6f;   // Ancho del reloj
        this.height = 0.2f;  // Alto del reloj

        // Aplicar tema
        applyTheme(theme);

        initShader();
        setupBuffers();
        setupPaints();

        Log.d(TAG, "✓ Clock3D creado - tema: " + (theme == THEME_ABYSSIA ? "ABYSSIA" : "PYRALIS"));
    }

    /**
     * Constructor por defecto (tema ABYSSIA, posición superior)
     */
    public Clock3D(Context context) {
        this(context, THEME_ABYSSIA, -0.3f, 0.7f);
    }

    // ═══════════════════════════════════════════════════════════════
    // CONFIGURACIÓN DE TEMA
    // ═══════════════════════════════════════════════════════════════

    private void applyTheme(int theme) {
        switch (theme) {
            case THEME_PYRALIS:
                primaryColor = COLOR_PYRALIS_PRIMARY;
                glowColor = COLOR_PYRALIS_GLOW;
                break;
            case THEME_KAMEHAMEHA:
                primaryColor = COLOR_KAMEHAMEHA_PRIMARY;
                glowColor = COLOR_KAMEHAMEHA_GLOW;
                break;
            case THEME_SYNTHWAVE:
                primaryColor = COLOR_SYNTHWAVE_PRIMARY;
                glowColor = COLOR_SYNTHWAVE_GLOW;
                break;
            case THEME_COSMOS:
                primaryColor = COLOR_COSMOS_PRIMARY;
                glowColor = COLOR_COSMOS_GLOW;
                break;
            case THEME_ABYSSIA:
            default:
                primaryColor = COLOR_ABYSSIA_PRIMARY;
                glowColor = COLOR_ABYSSIA_GLOW;
                break;
        }
    }

    /**
     * Cambia el tema del reloj en tiempo de ejecución
     */
    public void setTheme(int theme) {
        applyTheme(theme);
        if (textPaint != null) {
            textPaint.setColor(primaryColor);
        }
        if (glowPaint != null) {
            glowPaint.setColor(glowColor);
        }
        needsUpdate = true;
    }

    /**
     * Establece colores personalizados
     */
    public void setColors(int primary, int glow) {
        this.primaryColor = primary;
        this.glowColor = glow;
        if (textPaint != null) textPaint.setColor(primary);
        if (glowPaint != null) glowPaint.setColor(glow);
        needsUpdate = true;
    }

    // ═══════════════════════════════════════════════════════════════
    // INICIALIZACIÓN OPENGL
    // ═══════════════════════════════════════════════════════════════

    private void initShader() {
        programId = ShaderUtils.createProgram(vertexShader, fragmentShader);

        if (programId == 0) {
            Log.e(TAG, "✗ Error creando shader del reloj");
            return;
        }

        aPositionLoc = GLES30.glGetAttribLocation(programId, "a_Position");
        aTexCoordLoc = GLES30.glGetAttribLocation(programId, "a_TexCoord");
        uTextureLoc = GLES30.glGetUniformLocation(programId, "u_Texture");
        uAlphaLoc = GLES30.glGetUniformLocation(programId, "u_Alpha");
        uGlowIntensityLoc = GLES30.glGetUniformLocation(programId, "u_GlowIntensity");

        Log.d(TAG, "✓ Shader del reloj inicializado");
    }

    private void setupBuffers() {
        // Vértices del quad centrado en la posición
        float halfW = width / 2f;
        float halfH = height / 2f;

        float[] vertices = {
            x - halfW, y - halfH,  // Bottom-left
            x + halfW, y - halfH,  // Bottom-right
            x - halfW, y + halfH,  // Top-left
            x + halfW, y + halfH   // Top-right
        };

        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        vertexBuffer = vbb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // Coordenadas de textura
        float[] texCoords = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f
        };

        ByteBuffer tbb = ByteBuffer.allocateDirect(texCoords.length * 4);
        tbb.order(ByteOrder.nativeOrder());
        texCoordBuffer = tbb.asFloatBuffer();
        texCoordBuffer.put(texCoords);
        texCoordBuffer.position(0);
    }

    private void setupPaints() {
        // Paint principal para el texto
        textPaint = new Paint();
        textPaint.setColor(primaryColor);
        textPaint.setTextSize(textSize);
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Paint para el efecto glow
        glowPaint = new Paint();
        glowPaint.setColor(glowColor);
        glowPaint.setTextSize(textSize);
        glowPaint.setAntiAlias(true);
        glowPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.BOLD));
        glowPaint.setTextAlign(Paint.Align.CENTER);
        glowPaint.setMaskFilter(new BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL));
    }

    // ═══════════════════════════════════════════════════════════════
    // ACTUALIZACIÓN DE TIEMPO
    // ═══════════════════════════════════════════════════════════════

    private String getCurrentTimeString() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(use24HourFormat ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
        int minute = cal.get(Calendar.MINUTE);
        int second = cal.get(Calendar.SECOND);
        int millis = cal.get(Calendar.MILLISECOND);

        if (showMilliseconds) {
            // Formato: HH:MM:SS.mmm
            return String.format("%02d:%02d:%02d.%03d", hour, minute, second, millis);
        } else if (showSeconds) {
            return String.format("%02d:%02d:%02d", hour, minute, second);
        } else {
            return String.format("%02d:%02d", hour, minute);
        }
    }

    private void checkTimeUpdate() {
        Calendar cal = Calendar.getInstance();
        int currentMinute = cal.get(Calendar.MINUTE);
        long currentMillis = System.currentTimeMillis();

        // Milisegundos: actualizar cada ~33ms (~30 FPS para los números)
        if (showMilliseconds) {
            if (currentMillis - lastMillis >= 33) {
                lastMillis = currentMillis;
                currentTime = getCurrentTimeString();
                needsUpdate = true;
            }
            return;
        }

        // Segundos: actualizar cada segundo
        if (showSeconds) {
            int currentSecond = cal.get(Calendar.SECOND);
            String newTime = getCurrentTimeString();
            if (!newTime.equals(currentTime)) {
                currentTime = newTime;
                needsUpdate = true;
            }
            return;
        }

        // Solo minutos: actualizar cuando cambia el minuto
        if (currentMinute != lastMinute) {
            lastMinute = currentMinute;
            currentTime = getCurrentTimeString();
            needsUpdate = true;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GENERACIÓN DE TEXTURA CON GLOW
    // ═══════════════════════════════════════════════════════════════

    private void updateTexture() {
        if (currentTime.isEmpty()) {
            currentTime = getCurrentTimeString();
        }

        // Medir el texto
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textWidth = textPaint.measureText(currentTime);
        float textHeight = fm.descent - fm.ascent;

        // Bitmap más grande para el glow
        int padding = 40;  // Espacio para el glow
        int bitmapWidth = (int)(textWidth + padding * 2);
        int bitmapHeight = (int)(textHeight + padding * 2);

        // Asegurar dimensiones potencia de 2 para mejor compatibilidad
        bitmapWidth = nextPowerOfTwo(bitmapWidth);
        bitmapHeight = nextPowerOfTwo(bitmapHeight);

        Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT);

        // Posición centrada
        float textX = bitmapWidth / 2f;
        float textY = bitmapHeight / 2f - (fm.ascent + fm.descent) / 2f;

        // Dibujar glow primero (capa de atrás)
        canvas.drawText(currentTime, textX, textY, glowPaint);

        // Dibujar texto principal encima
        canvas.drawText(currentTime, textX, textY, textPaint);

        // Crear/actualizar textura OpenGL
        if (textureId == -1) {
            int[] textures = new int[1];
            GLES30.glGenTextures(1, textures, 0);
            textureId = textures[0];
        }

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);

        bitmap.recycle();
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);

        needsUpdate = false;
        Log.d(TAG, "✓ Textura del reloj actualizada: " + currentTime);
    }

    private int nextPowerOfTwo(int value) {
        int result = 1;
        while (result < value) {
            result *= 2;
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    // SCENEOBJECT INTERFACE
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void update(float deltaTime) {
        // Verificar si cambió el tiempo
        checkTimeUpdate();

        // Efecto de pulso suave en el glow
        glowPulse += deltaTime * 2f;
        if (glowPulse > Math.PI * 2) {
            glowPulse -= Math.PI * 2;
        }
        glowIntensity = 0.7f + 0.3f * (float)Math.sin(glowPulse);
    }

    @Override
    public void draw() {
        if (programId == 0) {
            return;
        }

        // Actualizar textura si es necesario
        if (needsUpdate) {
            updateTexture();
        }

        if (textureId == -1) {
            return;
        }

        GLES30.glUseProgram(programId);

        // Configurar blending para transparencia
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // Bind textura
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(uTextureLoc, 0);
        GLES30.glUniform1f(uAlphaLoc, 1.0f);
        GLES30.glUniform1f(uGlowIntensityLoc, glowIntensity);

        // Configurar atributos
        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        GLES30.glEnableVertexAttribArray(aTexCoordLoc);
        GLES30.glVertexAttribPointer(aTexCoordLoc, 2, GLES30.GL_FLOAT, false, 0, texCoordBuffer);

        // Dibujar
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        // Limpiar
        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glDisableVertexAttribArray(aTexCoordLoc);

        // Restaurar estados
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
    }

    // ═══════════════════════════════════════════════════════════════
    // CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════

    /**
     * Establece el formato de 24 horas
     */
    public void set24HourFormat(boolean use24Hour) {
        this.use24HourFormat = use24Hour;
        needsUpdate = true;
    }

    /**
     * Muestra/oculta los segundos
     */
    public void setShowSeconds(boolean show) {
        this.showSeconds = show;
        needsUpdate = true;
    }

    /**
     * Muestra/oculta los milisegundos (incluye segundos automáticamente)
     */
    public void setShowMilliseconds(boolean show) {
        this.showMilliseconds = show;
        if (show) this.showSeconds = true;  // Milisegundos implica segundos
        needsUpdate = true;
    }

    /**
     * Establece el tamaño del texto
     */
    public void setTextSize(float size) {
        this.textSize = size;
        if (textPaint != null) {
            textPaint.setTextSize(size);
            glowPaint.setTextSize(size);
        }
        needsUpdate = true;
    }

    /**
     * Establece la posición del reloj
     */
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        setupBuffers();  // Recalcular vértices
    }

    /**
     * Libera recursos OpenGL
     */
    public void dispose() {
        if (textureId != -1) {
            int[] textures = {textureId};
            GLES30.glDeleteTextures(1, textures, 0);
            textureId = -1;
        }
        if (programId != 0) {
            GLES30.glDeleteProgram(programId);
            programId = 0;
        }
        Log.d(TAG, "✓ Clock3D recursos liberados");
    }
}
