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
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   © ARCADE FOOTER - Copyright Orbix iA 2025                              ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Footer estilo arcade con copyright                                       ║
 * ║  Similar a "© CAPCOM 1987" en Street Fighter                             ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class ArcadeFooter implements SceneObject {
    private static final String TAG = "ArcadeFooter";

    // Textura
    private int textureId = 0;
    private Bitmap textBitmap;
    private Canvas textCanvas;

    // Dimensiones
    private static final int TEX_WIDTH = 400;
    private static final int TEX_HEIGHT = 48;

    // OpenGL
    private int shaderProgram = 0;
    private FloatBuffer vertexBuffer;
    private int aPositionLoc, aTexCoordLoc;
    private int uTextureLoc, uAlphaLoc;

    // Estado
    private float alpha = 1.0f;
    private float aspectRatio = 1.0f;
    private boolean initialized = false;
    private boolean isVisible = true;

    // Posición
    private float posY = -0.85f;  // Muy abajo
    private float width = 0.5f;
    private float height = 0.04f;

    public ArcadeFooter() {
        Log.d(TAG, "© Creando ArcadeFooter...");
        initBitmap();
        initOpenGL();
        renderText();
    }

    private void initBitmap() {
        textBitmap = Bitmap.createBitmap(TEX_WIDTH, TEX_HEIGHT, Bitmap.Config.ARGB_8888);
        textCanvas = new Canvas(textBitmap);
    }

    private void initOpenGL() {
        String vertexShader =
            "attribute vec2 a_Position;\n" +
            "attribute vec2 a_TexCoord;\n" +
            "varying vec2 v_TexCoord;\n" +
            "void main() {\n" +
            "    v_TexCoord = a_TexCoord;\n" +
            "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
            "}\n";

        String fragmentShader =
            "precision mediump float;\n" +
            "varying vec2 v_TexCoord;\n" +
            "uniform sampler2D u_Texture;\n" +
            "uniform float u_Alpha;\n" +
            "void main() {\n" +
            "    vec4 texColor = texture2D(u_Texture, v_TexCoord);\n" +
            "    gl_FragColor = vec4(texColor.rgb, texColor.a * u_Alpha);\n" +
            "}\n";

        int vs = compileShader(GLES30.GL_VERTEX_SHADER, vertexShader);
        int fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShader);

        if (vs == 0 || fs == 0) {
            Log.e(TAG, "Error compilando shaders");
            return;
        }

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vs);
        GLES30.glAttachShader(shaderProgram, fs);
        GLES30.glLinkProgram(shaderProgram);

        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        aTexCoordLoc = GLES30.glGetAttribLocation(shaderProgram, "a_TexCoord");
        uTextureLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Texture");
        uAlphaLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Alpha");

        // Vertex buffer
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

        // Crear textura
        int[] textures = new int[1];
        GLES30.glGenTextures(1, textures, 0);
        textureId = textures[0];

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        initialized = true;
        Log.d(TAG, "✅ OpenGL inicializado");
    }

    private void renderText() {
        if (textBitmap == null) return;

        textBitmap.eraseColor(Color.TRANSPARENT);

        float centerX = TEX_WIDTH / 2f;
        float centerY = TEX_HEIGHT / 2f;

        // Texto cyan sutil
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(22);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(0xFF00AAAA);  // Cyan oscuro

        // Glow sutil
        Paint glowPaint = new Paint(textPaint);
        glowPaint.setColor(0xFF008888);
        glowPaint.setMaskFilter(new android.graphics.BlurMaskFilter(4, android.graphics.BlurMaskFilter.Blur.NORMAL));

        String text = "© Orbix iA 2025";

        // Dibujar glow
        textCanvas.drawText(text, centerX, centerY + 8, glowPaint);

        // Dibujar texto
        textCanvas.drawText(text, centerX, centerY + 8, textPaint);

        // Subir a GPU
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, textBitmap, 0);

        Log.d(TAG, "✅ Footer renderizado: " + text);
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

    @Override
    public void update(float deltaTime) {
        // Sin animación para el footer
    }

    @Override
    public void draw() {
        if (!initialized || shaderProgram == 0 || !isVisible) return;

        GLES30.glUseProgram(shaderProgram);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // Calcular quad
        float halfWidth = width / aspectRatio;
        float halfHeight = height;

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

    public void setAspectRatio(float ratio) {
        this.aspectRatio = ratio;
    }

    public void show() {
        this.isVisible = true;
    }

    public void hide() {
        this.isVisible = false;
    }

    public void dispose() {
        if (textureId != 0) {
            int[] textures = {textureId};
            GLES30.glDeleteTextures(1, textures, 0);
        }
        if (textBitmap != null) {
            textBitmap.recycle();
        }
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
        }
        Log.d(TAG, "🧹 ArcadeFooter disposed");
    }
}
