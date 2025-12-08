package com.secret.blackholeglow;

import android.content.Context;
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
 * 🔮 HolographicTitle - Título Holográfico ULTRA OPTIMIZADO
 *
 * - Buffers pre-alocados
 * - Sin allocations en draw/update loop
 * - Shader mínimo (1 lectura de textura)
 * - Glitch basado en tiempo, no en random()
 */
public class HolographicTitle implements SceneObject {
    private static final String TAG = "HolographicTitle";

    // ═══════════════════════════════════════════════════════════════
    // 🎨 CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════

    private static final float TITLE_Y = 0.88f;
    private static final float TITLE_WIDTH = 1.2f;
    private static final float TITLE_HEIGHT = 0.10f;
    private static final float TITLE_TEXT_SIZE = 48f;

    private static final float[] COLOR_HUMANS = {0.0f, 0.9f, 1.0f};
    private static final float[] COLOR_ALIENS = {0.4f, 1.0f, 0.3f};

    // TIME_WRAP para evitar overflow (10π segundos)
    private static final float TIME_WRAP = 62.831853f;

    // ═══════════════════════════════════════════════════════════════
    // 🔧 OPENGL - Buffers pre-alocados
    // ═══════════════════════════════════════════════════════════════

    private int shaderProgram;
    private int aPositionLoc = -1;
    private int aTexCoordLoc = -1;
    private int uTextureLoc = -1;
    private int uTimeLoc = -1;
    private int uGlitchLoc = -1;
    private int uBaseColorLoc = -1;
    private int uAccentColorLoc = -1;
    private int uAlphaLoc = -1;

    private int titleTextureId = -1;
    private boolean textureReady = false;

    // Buffers pre-alocados (nunca se recrean)
    private final FloatBuffer vertexBuffer;
    private final FloatBuffer texCoordBuffer;

    // Pre-alocado para cleanup
    private final int[] tempTextureIds = new int[1];

    // Estado
    private float time = 0f;
    private float glitchIntensity = 0f;
    private float glitchTimer = 0f;

    private final Context context;
    private Paint titlePaint;

    // ═══════════════════════════════════════════════════════════════
    // 📐 CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════

    public HolographicTitle(Context context) {
        this.context = context;

        // Pre-alocar buffers UNA sola vez
        float x = -TITLE_WIDTH / 2f;
        float y = TITLE_Y - TITLE_HEIGHT / 2f;

        float[] vertices = {
            x, y,
            x + TITLE_WIDTH, y,
            x, y + TITLE_HEIGHT,
            x + TITLE_WIDTH, y + TITLE_HEIGHT
        };

        float[] texCoords = {
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f
        };

        vertexBuffer = allocateBuffer(vertices);
        texCoordBuffer = allocateBuffer(texCoords);

        initPaint();
        initShaders();

        Log.d(TAG, "🔮 HolographicTitle inicializado");
    }

    private FloatBuffer allocateBuffer(float[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(data);
        fb.position(0);
        return fb;
    }

    private void initPaint() {
        titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setTextSize(TITLE_TEXT_SIZE);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextAlign(Paint.Align.CENTER);
    }

    private void initShaders() {
        String vertexCode = ShaderUtils.loadAssetAsString(context, "shaders/holographic_title_vertex.glsl");
        String fragmentCode = ShaderUtils.loadAssetAsString(context, "shaders/holographic_title_fragment.glsl");

        if (vertexCode == null || fragmentCode == null) {
            Log.e(TAG, "❌ Error cargando shaders");
            return;
        }

        shaderProgram = ShaderUtils.createProgram(vertexCode, fragmentCode);
        if (shaderProgram == 0) {
            Log.e(TAG, "❌ Error creando programa");
            return;
        }

        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        aTexCoordLoc = GLES30.glGetAttribLocation(shaderProgram, "a_TexCoord");
        uTextureLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Texture");
        uTimeLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Time");
        uGlitchLoc = GLES30.glGetUniformLocation(shaderProgram, "u_GlitchIntensity");
        uBaseColorLoc = GLES30.glGetUniformLocation(shaderProgram, "u_BaseColor");
        uAccentColorLoc = GLES30.glGetUniformLocation(shaderProgram, "u_AccentColor");
        uAlphaLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Alpha");

        Log.d(TAG, "✓ Shaders cargados");
    }

    // ═══════════════════════════════════════════════════════════════
    // 🖼️ TEXTURA (se crea una sola vez)
    // ═══════════════════════════════════════════════════════════════

    private void createTexture() {
        if (textureReady) return;

        // Medir texto
        Paint.FontMetrics fm = titlePaint.getFontMetrics();
        float textHeight = fm.descent - fm.ascent;

        titlePaint.setColor(Color.WHITE);
        float humansW = titlePaint.measureText("HUMANS ");
        float vsW = titlePaint.measureText("vs ");
        float aliensW = titlePaint.measureText("ALIENS");
        float totalW = humansW + vsW + aliensW;

        int bmpW = (int)(totalW + 40);
        int bmpH = (int)(textHeight + 20);

        Bitmap bitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        float baseY = bmpH / 2f - (fm.ascent + fm.descent) / 2f;
        float startX = 20;

        // HUMANS en cyan
        titlePaint.setColor(Color.rgb(0, 230, 255));
        canvas.drawText("HUMANS ", startX + humansW / 2f, baseY, titlePaint);

        // vs en blanco
        titlePaint.setColor(Color.WHITE);
        canvas.drawText("vs ", startX + humansW + vsW / 2f, baseY, titlePaint);

        // ALIENS en verde
        titlePaint.setColor(Color.rgb(100, 255, 75));
        canvas.drawText("ALIENS", startX + humansW + vsW + aliensW / 2f, baseY, titlePaint);

        // Crear textura OpenGL
        GLES30.glGenTextures(1, tempTextureIds, 0);
        titleTextureId = tempTextureIds[0];

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, titleTextureId);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);

        bitmap.recycle();
        textureReady = true;

        Log.d(TAG, "✓ Textura creada");
    }

    // ═══════════════════════════════════════════════════════════════
    // 🎮 UPDATE & DRAW
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void update(float deltaTime) {
        time += deltaTime;
        if (time > TIME_WRAP) time -= TIME_WRAP;  // Evitar overflow

        // Glitch basado en tiempo (sin random)
        if (glitchTimer > 0) {
            glitchTimer -= deltaTime;
            if (glitchTimer <= 0) {
                glitchIntensity = 0f;
            }
        }
    }

    @Override
    public void draw() {
        if (shaderProgram == 0) return;

        // Crear textura si no existe
        if (!textureReady) {
            createTexture();
            if (!textureReady) return;
        }

        GLES30.glUseProgram(shaderProgram);

        // Estado GL
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // Uniforms
        GLES30.glUniform1f(uTimeLoc, time);
        GLES30.glUniform1f(uGlitchLoc, glitchIntensity);
        GLES30.glUniform1f(uAlphaLoc, 1.0f);
        GLES30.glUniform3f(uBaseColorLoc, COLOR_HUMANS[0], COLOR_HUMANS[1], COLOR_HUMANS[2]);
        GLES30.glUniform3f(uAccentColorLoc, COLOR_ALIENS[0], COLOR_ALIENS[1], COLOR_ALIENS[2]);

        // Textura
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, titleTextureId);
        GLES30.glUniform1i(uTextureLoc, 0);

        // Buffers pre-alocados
        vertexBuffer.position(0);
        texCoordBuffer.position(0);

        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        GLES30.glEnableVertexAttribArray(aTexCoordLoc);
        GLES30.glVertexAttribPointer(aTexCoordLoc, 2, GLES30.GL_FLOAT, false, 0, texCoordBuffer);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        // Limpiar
        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glDisableVertexAttribArray(aTexCoordLoc);
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
    }

    // ═══════════════════════════════════════════════════════════════
    // 🎯 API PÚBLICA
    // ═══════════════════════════════════════════════════════════════

    public void triggerGlitch(float intensity) {
        this.glitchIntensity = Math.min(intensity, 1.0f);
        this.glitchTimer = 0.3f;
    }

    // ═══════════════════════════════════════════════════════════════
    // 🗑️ LIMPIEZA
    // ═══════════════════════════════════════════════════════════════

    public void cleanup() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }

        if (titleTextureId != -1) {
            tempTextureIds[0] = titleTextureId;
            GLES30.glDeleteTextures(1, tempTextureIds, 0);
            titleTextureId = -1;
        }

        textureReady = false;
        Log.d(TAG, "✓ Limpiado");
    }
}
