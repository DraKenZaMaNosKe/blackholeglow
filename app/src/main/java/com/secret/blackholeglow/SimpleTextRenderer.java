package com.secret.blackholeglow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * 📊 RENDERIZADOR DE TEXTO SIMPLE PARA OpenGL
 *
 * Dibuja texto usando un bitmap generado dinámicamente
 * Perfecto para mostrar estadísticas y contadores
 */
public class SimpleTextRenderer implements SceneObject {
    private static final String TAG = "depurar";

    // OpenGL
    private int programId;
    private int aPositionLoc;
    private int aTexCoordLoc;
    private int uTextureLoc;
    private int uAlphaLoc;

    // Geometría
    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;

    // Textura del texto
    private int textureId = -1;
    private String currentText = "";
    private boolean needsUpdate = true;

    // Posición y tamaño (NDC)
    private float x, y, width, height;

    // Estilo del texto
    private Paint textPaint;
    private int textColor = Color.WHITE;
    private float textSize = 28f;  // Tamaño más pequeño para UI compacta

    // Shader simple
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
        "void main() {\n" +
        "    vec4 texColor = texture2D(u_Texture, v_TexCoord);\n" +
        "    gl_FragColor = vec4(texColor.rgb, texColor.a * u_Alpha);\n" +
        "}\n";

    /**
     * Constructor
     * @param x Posición X (NDC: -1 a 1)
     * @param y Posición Y (NDC: -1 a 1)
     * @param width Ancho (NDC)
     * @param height Alto (NDC)
     */
    public SimpleTextRenderer(Context context, float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        initShader();
        setupBuffers();
        setupPaint();

        Log.d(TAG, "✓ SimpleTextRenderer creado - pos: (" + x + ", " + y + ")");
    }

    private void initShader() {
        programId = ShaderUtils.createProgram(vertexShader, fragmentShader);

        if (programId == 0) {
            Log.e(TAG, "✗ Error creando shader de texto");
            return;
        }

        aPositionLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        aTexCoordLoc = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        uTextureLoc = GLES20.glGetUniformLocation(programId, "u_Texture");
        uAlphaLoc = GLES20.glGetUniformLocation(programId, "u_Alpha");

        Log.d(TAG, "✓ Shader de texto inicializado - programId: " + programId);
    }

    private void setupBuffers() {
        // Vértices del quad (rectángulo)
        float[] vertices = {
            x,         y,          // Bottom-left
            x + width, y,          // Bottom-right
            x,         y + height, // Top-left
            x + width, y + height  // Top-right
        };

        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        vertexBuffer = vbb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // Coordenadas de textura (UV)
        float[] texCoords = {
            0.0f, 1.0f,  // Bottom-left
            1.0f, 1.0f,  // Bottom-right
            0.0f, 0.0f,  // Top-left
            1.0f, 0.0f   // Top-right
        };

        ByteBuffer tbb = ByteBuffer.allocateDirect(texCoords.length * 4);
        tbb.order(ByteOrder.nativeOrder());
        texCoordBuffer = tbb.asFloatBuffer();
        texCoordBuffer.put(texCoords);
        texCoordBuffer.position(0);
    }

    private void setupPaint() {
        textPaint = new Paint();
        textPaint.setColor(textColor);
        textPaint.setTextSize(textSize);
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    /**
     * Actualiza el texto a mostrar
     */
    public void setText(String text) {
        if (!text.equals(currentText)) {
            currentText = text;
            needsUpdate = true;
        }
    }

    /**
     * Establece el color del texto
     */
    public void setColor(int color) {
        this.textColor = color;
        textPaint.setColor(color);
        needsUpdate = true;
    }

    /**
     * Genera la textura del texto desde un Bitmap
     */
    private void updateTexture() {
        if (currentText.isEmpty()) {
            return;
        }

        // Medir el texto
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textWidth = textPaint.measureText(currentText);
        float textHeight = fm.descent - fm.ascent;

        // Crear bitmap con el texto
        int bitmapWidth = (int)(textWidth + 20);
        int bitmapHeight = (int)(textHeight + 10);

        Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT);

        // Dibujar el texto centrado
        float textX = bitmapWidth / 2f;
        float textY = bitmapHeight / 2f - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(currentText, textX, textY, textPaint);

        // Crear/actualizar textura OpenGL
        if (textureId == -1) {
            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            textureId = textures[0];
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        bitmap.recycle();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        needsUpdate = false;
        Log.d(TAG, "✓ Textura de texto actualizada: \"" + currentText + "\"");
    }

    @Override
    public void update(float deltaTime) {
        // No necesita actualización por frame
    }

    @Override
    public void draw() {
        if (programId == 0 || currentText.isEmpty()) {
            return;
        }

        // Actualizar textura si es necesario
        if (needsUpdate) {
            updateTexture();
        }

        if (textureId == -1) {
            return;
        }

        GLES20.glUseProgram(programId);

        // Desactivar depth test para UI 2D
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Bind textura
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTextureLoc, 0);
        GLES20.glUniform1f(uAlphaLoc, 1.0f);

        // Configurar atributos
        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(aTexCoordLoc);
        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

        // Dibujar
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Limpiar
        GLES20.glDisableVertexAttribArray(aPositionLoc);
        GLES20.glDisableVertexAttribArray(aTexCoordLoc);

        // Restaurar estados
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }
}
