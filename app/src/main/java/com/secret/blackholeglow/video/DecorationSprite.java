package com.secret.blackholeglow.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Sprite decorativo 2D con posición, escala y efecto bioluminiscente.
 * Carga imágenes desde res/drawable con transparencia.
 */
public class DecorationSprite {
    private Context context;
    private int resourceId;

    // Posición y escala
    private float posX, posY;      // Centro del sprite (-1 a 1)
    private float scaleX, scaleY;  // Escala (1.0 = tamaño original relativo)

    // OpenGL
    private int textureId = -1;
    private int shaderProgram;
    private int aPositionLoc, aTexCoordLoc;
    private int uTextureLoc, uTimeLoc, uPosLoc, uScaleLoc;
    private FloatBuffer vertexBuffer, texCoordBuffer;

    private boolean initialized = false;
    private float time = 0f;

    // Quad unitario centrado
    private static final float[] VERTICES = {
        -0.5f, -0.5f,
         0.5f, -0.5f,
        -0.5f,  0.5f,
         0.5f,  0.5f
    };

    private static final float[] TEX_COORDS = {
        0f, 1f,
        1f, 1f,
        0f, 0f,
        1f, 0f
    };

    private static final String VERTEX_SHADER =
        "attribute vec2 aPosition;\n" +
        "attribute vec2 aTexCoord;\n" +
        "uniform vec2 uPos;\n" +
        "uniform vec2 uScale;\n" +
        "varying vec2 vTexCoord;\n" +
        "void main() {\n" +
        "    vec2 pos = aPosition * uScale + uPos;\n" +
        "    gl_Position = vec4(pos, 0.0, 1.0);\n" +
        "    vTexCoord = aTexCoord;\n" +
        "}\n";

    // Shader con brillo bioluminiscente pulsante
    private static final String FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "uniform sampler2D uTexture;\n" +
        "uniform float uTime;\n" +
        "varying vec2 vTexCoord;\n" +
        "void main() {\n" +
        "    vec4 color = texture2D(uTexture, vTexCoord);\n" +
        "    // Brillo cyan pulsante\n" +
        "    float isCyan = step(0.3, color.b) * step(color.r, 0.7);\n" +
        "    float pulse = 0.15 * sin(uTime * 2.5) + 0.15;\n" +
        "    color.rgb += vec3(0.1, 0.3, 0.5) * pulse * isCyan * color.a;\n" +
        "    gl_FragColor = color;\n" +
        "}\n";

    public DecorationSprite(Context context, int resourceId, float posX, float posY, float scaleX, float scaleY) {
        this.context = context;
        this.resourceId = resourceId;
        this.posX = posX;
        this.posY = posY;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    public void initialize() {
        // Buffers
        ByteBuffer bb = ByteBuffer.allocateDirect(VERTICES.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(VERTICES);
        vertexBuffer.position(0);

        bb = ByteBuffer.allocateDirect(TEX_COORDS.length * 4);
        bb.order(ByteOrder.nativeOrder());
        texCoordBuffer = bb.asFloatBuffer();
        texCoordBuffer.put(TEX_COORDS);
        texCoordBuffer.position(0);

        // Shaders
        int vs = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vs);
        GLES20.glAttachShader(shaderProgram, fs);
        GLES20.glLinkProgram(shaderProgram);

        aPositionLoc = GLES20.glGetAttribLocation(shaderProgram, "aPosition");
        aTexCoordLoc = GLES20.glGetAttribLocation(shaderProgram, "aTexCoord");
        uTextureLoc = GLES20.glGetUniformLocation(shaderProgram, "uTexture");
        uTimeLoc = GLES20.glGetUniformLocation(shaderProgram, "uTime");
        uPosLoc = GLES20.glGetUniformLocation(shaderProgram, "uPos");
        uScaleLoc = GLES20.glGetUniformLocation(shaderProgram, "uScale");

        loadTexture();
        initialized = true;
    }

    private void loadTexture() {
        int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0);
        textureId = tex[0];

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, opts);

        if (bitmap != null) {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();
        }
    }

    public void draw(float deltaTime) {
        if (!initialized || textureId == -1) return;

        time += deltaTime;
        if (time > 628f) time -= 628f;

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glUseProgram(shaderProgram);

        GLES20.glUniform2f(uPosLoc, posX, posY);
        GLES20.glUniform2f(uScaleLoc, scaleX, scaleY);
        GLES20.glUniform1f(uTimeLoc, time);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTextureLoc, 0);

        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(aTexCoordLoc);
        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(aPositionLoc);
        GLES20.glDisableVertexAttribArray(aTexCoordLoc);
    }

    public void draw() { draw(0.016f); }

    private int compileShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        return shader;
    }

    public void release() {
        if (textureId != -1) {
            GLES20.glDeleteTextures(1, new int[]{textureId}, 0);
            textureId = -1;
        }
        if (shaderProgram != 0) {
            GLES20.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        initialized = false;
    }

    // Setters para modificar en runtime
    public void setPosition(float x, float y) { posX = x; posY = y; }
    public void setScale(float sx, float sy) { scaleX = sx; scaleY = sy; }
}
