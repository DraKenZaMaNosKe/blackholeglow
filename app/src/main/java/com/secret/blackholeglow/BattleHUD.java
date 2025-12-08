package com.secret.blackholeglow;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║   🎮 BattleHUD - HUD estilo Street Fighter / King of Fighters                ║
 * ╠═══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                               ║
 * ║   ┌─────────────────┐    ∞    ┌─────────────────┐                            ║
 * ║   │ P1    [██████  ]│         │[  ██████]    P2 │                            ║
 * ║   │ dron  [████    ]│         │[    ████]    IA │                            ║
 * ║   └─────────────────┘         └─────────────────┘                            ║
 * ║                                                                               ║
 * ║   • 2 barras humanos (izquierda) - Se vacían hacia la derecha               ║
 * ║   • 2 barras aliens (derecha) - Se vacían hacia la izquierda                ║
 * ║   • Símbolo ∞ en el centro (tiempo infinito estilo KOF)                     ║
 * ║   • Actualización en tiempo real según HP de las naves                       ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 */
public class BattleHUD implements SceneObject {
    private static final String TAG = "BattleHUD";

    // Textura y canvas
    private int textureId = 0;
    private Bitmap bitmap;
    private Canvas canvas;
    private static final int TEX_WIDTH = 512;
    private static final int TEX_HEIGHT = 128;

    // OpenGL
    private int shaderProgram = 0;
    private FloatBuffer vertexBuffer;
    private int aPositionLoc, aTexCoordLoc;
    private int uTextureLoc, uAlphaLoc;

    // Estado
    private float alpha = 1.0f;
    private float aspectRatio = 1.0f;
    private boolean initialized = false;
    private boolean needsUpdate = true;

    // Posición del HUD (parte superior)
    private float posY = 0.85f;
    private float width = 0.95f;
    private float height = 0.12f;

    // ═══════════════════════════════════════════════════════════════════════════
    // HP de las 4 naves (0.0 - 1.0)
    // ═══════════════════════════════════════════════════════════════════════════
    private float humanP1_HP = 1.0f;      // Jugador 1 manual
    private float humanDron_HP = 1.0f;    // Dron IA aliado
    private float alienP2_HP = 1.0f;      // OVNI enemigo 1
    private float alienIA_HP = 1.0f;      // OVNI enemigo 2

    private int humanP1_MaxHP = 5;
    private int humanDron_MaxHP = 4;
    private int alienP2_MaxHP = 3;
    private int alienIA_MaxHP = 6;

    // Paints
    private Paint framePaint;
    private Paint barBgPaint;
    private Paint barHumanPaint;
    private Paint barAlienPaint;
    private Paint labelPaint;
    private Paint infinityPaint;
    private Paint infinityGlowPaint;

    // Shaders
    private static final String VERTEX_SHADER =
        "attribute vec2 a_Position;\n" +
        "attribute vec2 a_TexCoord;\n" +
        "varying vec2 v_TexCoord;\n" +
        "void main() {\n" +
        "    v_TexCoord = a_TexCoord;\n" +
        "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "varying vec2 v_TexCoord;\n" +
        "uniform sampler2D u_Texture;\n" +
        "uniform float u_Alpha;\n" +
        "void main() {\n" +
        "    vec4 texColor = texture2D(u_Texture, v_TexCoord);\n" +
        "    gl_FragColor = vec4(texColor.rgb, texColor.a * u_Alpha);\n" +
        "}\n";

    public BattleHUD() {
        Log.d(TAG, "🎮 Creando BattleHUD estilo Street Fighter...");
        initPaints();
        initBitmap();
        initOpenGL();
        renderHUD();
    }

    private void initPaints() {
        // Marco metálico
        framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        framePaint.setColor(0xFF444444);
        framePaint.setStyle(Paint.Style.STROKE);
        framePaint.setStrokeWidth(3);

        // Fondo de barras
        barBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barBgPaint.setColor(0xFF222222);

        // Barra humanos (cyan/azul)
        barHumanPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Barra aliens (verde/magenta)
        barAlienPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Labels
        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTextSize(18);
        labelPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        labelPaint.setColor(0xFFFFFFFF);

        // Símbolo infinito
        infinityPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        infinityPaint.setTextSize(36);
        infinityPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        infinityPaint.setTextAlign(Paint.Align.CENTER);
        infinityPaint.setColor(0xFFFFDD00);  // Amarillo dorado

        infinityGlowPaint = new Paint(infinityPaint);
        infinityGlowPaint.setColor(0xFFFF8800);
        infinityGlowPaint.setMaskFilter(new android.graphics.BlurMaskFilter(8, android.graphics.BlurMaskFilter.Blur.NORMAL));
    }

    private void initBitmap() {
        bitmap = Bitmap.createBitmap(TEX_WIDTH, TEX_HEIGHT, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
    }

    private void initOpenGL() {
        int vs = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        if (vs == 0 || fs == 0) return;

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vs);
        GLES30.glAttachShader(shaderProgram, fs);
        GLES30.glLinkProgram(shaderProgram);

        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        aTexCoordLoc = GLES30.glGetAttribLocation(shaderProgram, "a_TexCoord");
        uTextureLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Texture");
        uAlphaLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Alpha");

        // Vertex buffer (4 vértices * 4 floats)
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * 4 * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();

        // Textura
        int[] textures = new int[1];
        GLES30.glGenTextures(1, textures, 0);
        textureId = textures[0];
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);

        initialized = true;
        Log.d(TAG, "✓ BattleHUD OpenGL inicializado");
    }

    private int compileShader(int type, String code) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, code);
        GLES30.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Error shader: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    /**
     * Renderiza todo el HUD en el bitmap
     */
    private void renderHUD() {
        if (bitmap == null || canvas == null) return;
        bitmap.eraseColor(Color.TRANSPARENT);

        float centerX = TEX_WIDTH / 2f;
        float barWidth = 150;
        float barHeight = 20;
        float barSpacing = 28;
        float sideMargin = 30;

        // ═══════════════════════════════════════════════════════════════════════════
        // LADO IZQUIERDO - HUMANOS (P1 y dron)
        // ═══════════════════════════════════════════════════════════════════════════
        float leftX = sideMargin;
        float topY = 25;

        // P1 - Jugador manual (barra cyan que se vacía hacia la derecha)
        drawHealthBar(leftX, topY, barWidth, barHeight, humanP1_HP, true, true);
        labelPaint.setTextAlign(Paint.Align.LEFT);
        labelPaint.setColor(0xFF00FFFF);  // Cyan
        canvas.drawText("P1", leftX, topY - 5, labelPaint);

        // dron - IA aliada (barra roja/naranja)
        float dronY = topY + barSpacing + barHeight;
        drawHealthBar(leftX, dronY, barWidth, barHeight, humanDron_HP, true, false);
        labelPaint.setColor(0xFFFF6644);  // Naranja/rojo
        canvas.drawText("dron", leftX, dronY - 5, labelPaint);

        // ═══════════════════════════════════════════════════════════════════════════
        // CENTRO - Símbolo ∞ (infinito)
        // ═══════════════════════════════════════════════════════════════════════════
        float infinityY = TEX_HEIGHT / 2f + 10;
        canvas.drawText("∞", centerX, infinityY, infinityGlowPaint);
        canvas.drawText("∞", centerX, infinityY, infinityPaint);

        // ═══════════════════════════════════════════════════════════════════════════
        // LADO DERECHO - ALIENS (P2 e IA)
        // ═══════════════════════════════════════════════════════════════════════════
        float rightX = TEX_WIDTH - sideMargin - barWidth;

        // P2 - OVNI enemigo 1 (barra verde que se vacía hacia la izquierda)
        drawHealthBar(rightX, topY, barWidth, barHeight, alienP2_HP, false, true);
        labelPaint.setTextAlign(Paint.Align.RIGHT);
        labelPaint.setColor(0xFF00FF44);  // Verde
        canvas.drawText("P2", TEX_WIDTH - sideMargin, topY - 5, labelPaint);

        // IA - OVNI enemigo 2 (barra magenta)
        float iaY = topY + barSpacing + barHeight;
        drawHealthBar(rightX, iaY, barWidth, barHeight, alienIA_HP, false, false);
        labelPaint.setColor(0xFFFF00FF);  // Magenta
        canvas.drawText("IA", TEX_WIDTH - sideMargin, iaY - 5, labelPaint);

        // Subir textura a GPU
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);

        needsUpdate = false;
    }

    /**
     * Dibuja una barra de vida individual
     * @param x Posición X
     * @param y Posición Y
     * @param w Ancho
     * @param h Alto
     * @param hpPercent HP como porcentaje (0.0 - 1.0)
     * @param isHuman true = humano (cyan/naranja), false = alien (verde/magenta)
     * @param isPrimary true = nave principal, false = secundaria
     */
    private void drawHealthBar(float x, float y, float w, float h, float hpPercent, boolean isHuman, boolean isPrimary) {
        RectF bgRect = new RectF(x, y, x + w, y + h);

        // Fondo oscuro
        canvas.drawRoundRect(bgRect, 4, 4, barBgPaint);

        // Barra de HP
        if (hpPercent > 0.01f) {
            float hpWidth = w * Math.max(0, Math.min(1, hpPercent));

            // Calcular posición según si es izquierda o derecha
            RectF hpRect;
            if (isHuman) {
                // Humanos: barra se vacía hacia la derecha (HP empieza en izquierda)
                hpRect = new RectF(x, y, x + hpWidth, y + h);
            } else {
                // Aliens: barra se vacía hacia la izquierda (HP empieza en derecha)
                hpRect = new RectF(x + w - hpWidth, y, x + w, y + h);
            }

            // Gradiente según equipo y tipo
            Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            int[] colors;
            if (isHuman) {
                if (isPrimary) {
                    // P1: Cyan brillante
                    colors = new int[]{0xFF00FFFF, 0xFF0088FF, 0xFF00CCFF};
                } else {
                    // Dron: Rojo/Naranja
                    colors = new int[]{0xFFFF4444, 0xFFFF8800, 0xFFFFAA00};
                }
            } else {
                if (isPrimary) {
                    // P2 Alien: Verde
                    colors = new int[]{0xFF00FF44, 0xFF00CC33, 0xFF88FF00};
                } else {
                    // IA Alien: Magenta
                    colors = new int[]{0xFFFF00FF, 0xFFCC00CC, 0xFFFF66FF};
                }
            }

            LinearGradient gradient = new LinearGradient(
                hpRect.left, hpRect.top, hpRect.left, hpRect.bottom,
                colors, null, Shader.TileMode.CLAMP
            );
            barPaint.setShader(gradient);
            canvas.drawRoundRect(hpRect, 4, 4, barPaint);

            // Brillo superior
            Paint shinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            shinePaint.setColor(0x44FFFFFF);
            RectF shineRect = new RectF(hpRect.left + 2, y + 2, hpRect.right - 2, y + h/3);
            canvas.drawRoundRect(shineRect, 2, 2, shinePaint);
        }

        // Marco exterior
        framePaint.setColor(0xFF666666);
        canvas.drawRoundRect(bgRect, 4, 4, framePaint);

        // Borde interior brillante
        framePaint.setColor(0xFF888888);
        framePaint.setStrokeWidth(1);
        RectF innerRect = new RectF(x + 1, y + 1, x + w - 1, y + h - 1);
        canvas.drawRoundRect(innerRect, 3, 3, framePaint);
        framePaint.setStrokeWidth(3);
    }

    @Override
    public void update(float deltaTime) {
        if (needsUpdate) {
            renderHUD();
        }
    }

    @Override
    public void draw() {
        if (!initialized || shaderProgram == 0 || alpha <= 0.01f) return;

        GLES30.glUseProgram(shaderProgram);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        float halfWidth = width / 2f;
        if (aspectRatio < 1f) {
            halfWidth = halfWidth / aspectRatio;
        }
        float halfHeight = height / 2f;

        float[] vertices = {
            -halfWidth, posY - halfHeight, 0f, 1f,
             halfWidth, posY - halfHeight, 1f, 1f,
            -halfWidth, posY + halfHeight, 0f, 0f,
             halfWidth, posY + halfHeight, 1f, 0f
        };

        vertexBuffer.clear();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glEnableVertexAttribArray(aTexCoordLoc);
        vertexBuffer.position(0);
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer);
        vertexBuffer.position(2);
        GLES30.glVertexAttribPointer(aTexCoordLoc, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(uTextureLoc, 0);
        GLES30.glUniform1f(uAlphaLoc, alpha);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glDisableVertexAttribArray(aTexCoordLoc);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // API PÚBLICA - Actualizar HP
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Actualiza el HP del jugador 1 (nave manual)
     */
    public void setHumanP1_HP(int current, int max) {
        humanP1_MaxHP = max;
        humanP1_HP = (float) current / max;
        needsUpdate = true;
    }

    /**
     * Actualiza el HP del dron aliado
     */
    public void setHumanDron_HP(int current, int max) {
        humanDron_MaxHP = max;
        humanDron_HP = (float) current / max;
        needsUpdate = true;
    }

    /**
     * Actualiza el HP del OVNI enemigo 1 (P2)
     */
    public void setAlienP2_HP(int current, int max) {
        alienP2_MaxHP = max;
        alienP2_HP = (float) current / max;
        needsUpdate = true;
    }

    /**
     * Actualiza el HP del OVNI enemigo 2 (IA)
     */
    public void setAlienIA_HP(int current, int max) {
        alienIA_MaxHP = max;
        alienIA_HP = (float) current / max;
        needsUpdate = true;
    }

    /**
     * Actualiza todos los HP a la vez
     */
    public void updateAllHP(int p1HP, int p1Max, int dronHP, int dronMax,
                           int alien1HP, int alien1Max, int alien2HP, int alien2Max) {
        humanP1_HP = (float) p1HP / p1Max;
        humanDron_HP = (float) dronHP / dronMax;
        alienP2_HP = (float) alien1HP / alien1Max;
        alienIA_HP = (float) alien2HP / alien2Max;
        humanP1_MaxHP = p1Max;
        humanDron_MaxHP = dronMax;
        alienP2_MaxHP = alien1Max;
        alienIA_MaxHP = alien2Max;
        needsUpdate = true;
    }

    public void setAspectRatio(float ratio) {
        this.aspectRatio = ratio;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public void show() {
        this.alpha = 1.0f;
    }

    public void hide() {
        this.alpha = 0.0f;
    }

    public void dispose() {
        if (textureId != 0) {
            int[] t = {textureId};
            GLES30.glDeleteTextures(1, t, 0);
            textureId = 0;
        }
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
    }
}
