package com.secret.blackholeglow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║   🎵 SongMessageRenderer - Mensajes de Canción con Estilo Mágico  ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * Renderiza mensajes de canciones compartidas con:
 * - Gradiente de colores cósmicos (Rosa → Cyan → Magenta)
 * - Fondo semi-transparente con bordes redondeados
 * - Auto-ajuste del tamaño de texto según longitud
 * - Efecto de glow/resplandor
 * - Animación de fade in/out
 */
public class SongMessageRenderer implements SceneObject {
    private static final String TAG = "SongMessage";

    // Dimensiones del bitmap
    private static final int TEX_WIDTH = 1024;
    private static final int TEX_HEIGHT = 200;

    // OpenGL
    private int programId;
    private int aPositionLoc;
    private int aTexCoordLoc;
    private int uTextureLoc;
    private int uAlphaLoc;
    private int uTimeLoc;

    // Buffers
    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;

    // Textura
    private int textureId = -1;
    private String currentText = "";
    private boolean needsUpdate = false;

    // Posición (NDC)
    private float x = 0.0f;
    private float y = 0.60f;  // Arriba de la pantalla
    private float width = 1.90f;
    private float height = 0.25f;

    // Animación
    private float alpha = 0.0f;
    private float targetAlpha = 0.0f;
    private long showStartTime = 0;
    private static final long SHOW_DURATION = 8000;  // 8 segundos
    private boolean isVisible = false;

    // Colores del gradiente cósmico
    private int[] gradientColors = {
        Color.rgb(255, 100, 200),   // Rosa brillante
        Color.rgb(100, 220, 255),   // Cyan
        Color.rgb(200, 100, 255),   // Magenta
        Color.rgb(255, 180, 100)    // Dorado
    };

    // Shader con efecto de glow
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
        "uniform float u_Time;\n" +
        "void main() {\n" +
        "    vec4 texColor = texture2D(u_Texture, v_TexCoord);\n" +
        // Efecto de brillo pulsante sutil
        "    float pulse = 1.0 + 0.1 * sin(u_Time * 3.0);\n" +
        "    vec3 finalColor = texColor.rgb * pulse;\n" +
        "    gl_FragColor = vec4(finalColor, texColor.a * u_Alpha);\n" +
        "}\n";

    private Context context;
    private float time = 0f;

    public SongMessageRenderer(Context context) {
        this.context = context;
        initShader();
        setupBuffers();
        Log.d(TAG, "🎵 SongMessageRenderer inicializado");
    }

    private void initShader() {
        programId = ShaderUtils.createProgram(vertexShader, fragmentShader);

        if (programId == 0) {
            Log.e(TAG, "Error creando shader");
            return;
        }

        aPositionLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        aTexCoordLoc = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        uTextureLoc = GLES20.glGetUniformLocation(programId, "u_Texture");
        uAlphaLoc = GLES20.glGetUniformLocation(programId, "u_Alpha");
        uTimeLoc = GLES20.glGetUniformLocation(programId, "u_Time");
    }

    private void setupBuffers() {
        // Vértices (centrado)
        float left = x - width / 2;
        float right = x + width / 2;
        float bottom = y - height / 2;
        float top = y + height / 2;

        float[] vertices = {
            left, bottom,
            right, bottom,
            left, top,
            right, top
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

    /**
     * 🎵 Muestra un mensaje con estilo mágico
     */
    public void showMessage(String message) {
        if (message == null || message.isEmpty()) return;

        currentText = message;
        needsUpdate = true;
        isVisible = true;
        targetAlpha = 1.0f;
        showStartTime = System.currentTimeMillis();

        Log.d(TAG, "🎵 Mostrando: " + message);
    }

    /**
     * Oculta el mensaje
     */
    public void hide() {
        targetAlpha = 0.0f;
        isVisible = false;
    }

    /**
     * 🎨 Genera la textura con el texto estilizado
     */
    private void updateTexture() {
        if (!needsUpdate || currentText.isEmpty()) return;
        needsUpdate = false;

        // Crear bitmap
        Bitmap bitmap = Bitmap.createBitmap(TEX_WIDTH, TEX_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Fondo transparente con bordes redondeados y gradiente
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setShader(new LinearGradient(
            0, 0, TEX_WIDTH, 0,
            new int[]{
                Color.argb(180, 20, 10, 40),    // Púrpura oscuro
                Color.argb(200, 40, 20, 60),    // Púrpura medio
                Color.argb(180, 20, 10, 40)     // Púrpura oscuro
            },
            new float[]{0f, 0.5f, 1f},
            Shader.TileMode.CLAMP
        ));

        RectF bgRect = new RectF(20, 20, TEX_WIDTH - 20, TEX_HEIGHT - 20);
        canvas.drawRoundRect(bgRect, 30, 30, bgPaint);

        // Borde brillante
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f);
        borderPaint.setShader(new LinearGradient(
            0, 0, TEX_WIDTH, 0,
            gradientColors,
            null,
            Shader.TileMode.CLAMP
        ));
        canvas.drawRoundRect(bgRect, 30, 30, borderPaint);

        // Configurar texto
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Auto-ajustar tamaño según longitud del texto
        float textSize = calculateTextSize(currentText);
        textPaint.setTextSize(textSize);

        // Gradiente de colores para el texto
        textPaint.setShader(new LinearGradient(
            100, 0, TEX_WIDTH - 100, 0,
            gradientColors,
            null,
            Shader.TileMode.CLAMP
        ));

        // Sombra para efecto glow
        textPaint.setShadowLayer(8f, 0, 0, Color.argb(200, 255, 100, 255));

        // Dividir texto en líneas si es necesario
        String[] lines = wrapText(currentText, textPaint, TEX_WIDTH - 80);

        // Dibujar cada línea centrada
        float lineHeight = textSize * 1.3f;
        float totalHeight = lines.length * lineHeight;
        float startY = (TEX_HEIGHT - totalHeight) / 2 + textSize;

        for (int i = 0; i < lines.length; i++) {
            canvas.drawText(lines[i], TEX_WIDTH / 2f, startY + (i * lineHeight), textPaint);
        }

        // Subir textura a OpenGL
        if (textureId == -1) {
            int[] texIds = new int[1];
            GLES20.glGenTextures(1, texIds, 0);
            textureId = texIds[0];
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        bitmap.recycle();
    }

    /**
     * 📏 Calcula el tamaño del texto según la longitud
     */
    private float calculateTextSize(String text) {
        int length = text.length();
        if (length < 20) return 56f;
        if (length < 40) return 48f;
        if (length < 60) return 40f;
        if (length < 80) return 34f;
        return 28f;
    }

    /**
     * 📐 Divide el texto en líneas que quepan
     */
    private String[] wrapText(String text, Paint paint, float maxWidth) {
        if (paint.measureText(text) <= maxWidth) {
            return new String[]{text};
        }

        // Dividir por palabras
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        java.util.List<String> lines = new java.util.ArrayList<>();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = new StringBuilder(testLine);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
                currentLine = new StringBuilder(word);
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        // Máximo 3 líneas
        if (lines.size() > 3) {
            lines = lines.subList(0, 3);
            String lastLine = lines.get(2);
            if (lastLine.length() > 3) {
                lines.set(2, lastLine.substring(0, lastLine.length() - 3) + "...");
            }
        }

        return lines.toArray(new String[0]);
    }

    @Override
    public void update(float deltaTime) {
        time += deltaTime;

        // Actualizar alpha con interpolación suave
        alpha += (targetAlpha - alpha) * 0.1f;

        // Auto-ocultar después del tiempo
        if (isVisible) {
            long elapsed = System.currentTimeMillis() - showStartTime;
            if (elapsed > SHOW_DURATION) {
                hide();
            } else if (elapsed > SHOW_DURATION - 1000) {
                // Fade out en el último segundo
                targetAlpha = (SHOW_DURATION - elapsed) / 1000.0f;
            }
        }
    }

    @Override
    public void draw() {
        if (programId == 0 || alpha < 0.01f) return;

        // Actualizar textura si es necesario
        updateTexture();

        if (textureId == -1) return;

        GLES20.glUseProgram(programId);

        // Habilitar blending
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Textura
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTextureLoc, 0);

        // Uniforms
        GLES20.glUniform1f(uAlphaLoc, alpha);
        GLES20.glUniform1f(uTimeLoc, time);

        // Vértices
        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        // Coords de textura
        GLES20.glEnableVertexAttribArray(aTexCoordLoc);
        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

        // Dibujar
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Limpiar
        GLES20.glDisableVertexAttribArray(aPositionLoc);
        GLES20.glDisableVertexAttribArray(aTexCoordLoc);
    }

    public boolean isVisible() {
        return isVisible || alpha > 0.01f;
    }

    public void cleanup() {
        if (textureId != -1) {
            int[] texIds = {textureId};
            GLES20.glDeleteTextures(1, texIds, 0);
            textureId = -1;
        }
        if (programId != 0) {
            GLES20.glDeleteProgram(programId);
            programId = 0;
        }
    }
}
