package com.secret.blackholeglow.christmas;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import com.secret.blackholeglow.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║   🎁 ChristmasGiftButton - OPTIMIZADO (ES 2.0 compatible)                 ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * Botón cajita de regalo con textura PNG:
 * - Dibuja SOLO un quad pequeño, no fullscreen
 * - Glow dorado alrededor
 * - Animación flotante suave
 * - ES 2.0 compatible shaders (work in ES 3.0 context)
 */
public class ChristmasOrnamentButton {
    private static final String TAG = "ChristmasGiftBtn";

    // OpenGL
    private int shaderProgram;
    private int textureId;
    private FloatBuffer vertexBuffer;

    // Uniforms/Attributes
    private int aPositionLoc;
    private int aTexCoordLoc;
    private int uTextureLoc;
    private int uTimeLoc;

    // Screen
    private float screenWidth = 1080f;
    private float screenHeight = 1920f;
    private float aspectRatio = 1.0f;

    // State
    private boolean initialized = false;
    private boolean visible = true;
    private boolean isPressed = false;
    // Posición y tamaño - MODIFICA AQUÍ o usa los setters
    // NDC: X(-1 izq, 0 centro, 1 der) Y(-1 abajo, 0 centro, 1 arriba)
    private float posX = 0.0f;    // Centro horizontal
    private float posY = -0.65f;  // Abajo (sobre los regalos)
    private float size = 0.22f;   // Tamaño mediano-grande
    private float time = 0.0f;
    private float shakeIntensity = 0.0f;

    private Context context;

    // Simple vertex shader - ES 2.0 compatible
    private static final String VERTEX_SHADER =
        "attribute vec2 a_Position;\n" +
        "attribute vec2 a_TexCoord;\n" +
        "varying vec2 v_TexCoord;\n" +
        "void main() {\n" +
        "    v_TexCoord = a_TexCoord;\n" +
        "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
        "}\n";

    // Fragment shader - ES 2.0 compatible
    private static final String FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "varying vec2 v_TexCoord;\n" +
        "uniform sampler2D u_Texture;\n" +
        "uniform float u_Time;\n" +
        "\n" +
        "void main() {\n" +
        "    vec4 tex = texture2D(u_Texture, v_TexCoord);\n" +
        "    vec2 uv = v_TexCoord;\n" +
        "    \n" +
        "    vec2 center = uv - 0.5;\n" +
        "    float dist = length(center);\n" +
        "    \n" +
        "    float edgeFade = smoothstep(0.5, 0.35, abs(uv.x - 0.5));\n" +
        "    edgeFade *= smoothstep(0.5, 0.35, abs(uv.y - 0.5));\n" +
        "    \n" +
        "    float phase = mod(u_Time * 1.2, 6.28318);\n" +
        "    float pulse = 0.9 + 0.1 * sin(phase);\n" +
        "    \n" +
        "    if (tex.a > 0.1) {\n" +
        "        vec3 finalColor = tex.rgb * pulse;\n" +
        "        gl_FragColor = vec4(finalColor, tex.a);\n" +
        "        return;\n" +
        "    }\n" +
        "    \n" +
        "    float glow = smoothstep(0.5, 0.15, dist) * edgeFade * pulse * 0.4;\n" +
        "    vec3 glowColor = vec3(1.0, 0.85, 0.3) * glow;\n" +
        "    gl_FragColor = vec4(glowColor, glow);\n" +
        "}\n";

    public ChristmasOrnamentButton() {}

    public void init(Context ctx) {
        this.context = ctx;
        initGL();
    }

    private void initGL() {
        int vs = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER, "vertex");
        int fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER, "fragment");
        if (vs == 0 || fs == 0) return;

        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vs);
        GLES20.glAttachShader(shaderProgram, fs);
        GLES20.glLinkProgram(shaderProgram);

        int[] status = new int[1];
        GLES20.glGetProgramiv(shaderProgram, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) {
            Log.e(TAG, "Link error: " + GLES20.glGetProgramInfoLog(shaderProgram));
            return;
        }

        GLES20.glDeleteShader(vs);
        GLES20.glDeleteShader(fs);

        // Get locations
        aPositionLoc = GLES20.glGetAttribLocation(shaderProgram, "a_Position");
        aTexCoordLoc = GLES20.glGetAttribLocation(shaderProgram, "a_TexCoord");
        uTextureLoc = GLES20.glGetUniformLocation(shaderProgram, "u_Texture");
        uTimeLoc = GLES20.glGetUniformLocation(shaderProgram, "u_Time");

        // Create vertex buffer (will be updated per frame with position)
        vertexBuffer = ByteBuffer.allocateDirect(6 * 4 * 4) // 6 vertices, 4 floats each
            .order(ByteOrder.nativeOrder()).asFloatBuffer();

        // Load texture
        loadTexture();

        initialized = true;
        Log.d(TAG, "🎁 GiftButton inicializado");
    }

    private void loadTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        textureId = textures[0];

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = false;
        Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.cajitaderegalo, opts);
        if (bmp != null) {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
            Log.d(TAG, "🎁 Texture: " + bmp.getWidth() + "x" + bmp.getHeight());
            bmp.recycle();
        }
    }

    private int compileShader(int type, String source, String name) {
        int shader = GLES20.glCreateShader(type);
        if (shader == 0) {
            Log.e(TAG, "❌ glCreateShader returned 0 for " + name);
            return 0;
        }
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Shader error (" + name + "): " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    public void update(float deltaTime) {
        time += deltaTime;
        if (time > 1000f) time -= 1000f;
        if (shakeIntensity > 0) {
            shakeIntensity -= deltaTime * 3.0f;
            if (shakeIntensity < 0) shakeIntensity = 0;
        }
    }

    public void draw() {
        if (!initialized || !visible || shaderProgram == 0) return;

        // Calculate floating position
        float floatX = posX + (float)Math.sin(time * 0.5f) * 0.003f;
        float floatY = posY + (float)Math.sin(time * 0.8f) * 0.008f;

        // Add shake
        if (shakeIntensity > 0) {
            floatX += (float)Math.sin(time * 12f) * shakeIntensity * 0.015f;
            floatY += (float)Math.sin(time * 15f + 1.57f) * shakeIntensity * 0.012f;
        }

        // Calculate quad corners in NDC (mantener proporciones cuadradas)
        float halfW = size * 1.3f;
        float halfH = size * 1.3f * aspectRatio;

        float left = floatX - halfW;
        float right = floatX + halfW;
        float bottom = floatY - halfH;
        float top = floatY + halfH;

        // Build vertex data: position(2) + texcoord(2) for 6 vertices (2 triangles)
        float[] vertices = {
            // Triangle 1
            left, bottom,   0, 1,
            right, bottom,  1, 1,
            left, top,      0, 0,
            // Triangle 2
            right, bottom,  1, 1,
            right, top,     1, 0,
            left, top,      0, 0
        };

        vertexBuffer.clear();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // Draw
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glUseProgram(shaderProgram);

        // Uniforms
        GLES20.glUniform1f(uTimeLoc, time);

        // Texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTextureLoc, 0);

        // Attributes
        int stride = 4 * 4; // 4 floats * 4 bytes
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, stride, vertexBuffer);

        vertexBuffer.position(2);
        GLES20.glEnableVertexAttribArray(aTexCoordLoc);
        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, stride, vertexBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

        GLES20.glDisableVertexAttribArray(aPositionLoc);
        GLES20.glDisableVertexAttribArray(aTexCoordLoc);
    }

    public boolean contains(float touchX, float touchY) {
        // Simple distance check - generous hit area
        float dx = touchX - posX;
        float dy = touchY - posY;
        // Use generous hit area (0.35) to ensure button is easy to tap
        return Math.abs(dx) < 0.35f && Math.abs(dy) < 0.35f;
    }

    /**
     * Check if normalized coordinates are inside the button
     * @param nx Normalized X (-1 to 1)
     * @param ny Normalized Y (-1 to 1)
     * @return true if inside button area
     */
    public boolean isInside(float nx, float ny) {
        return contains(nx, ny);
    }

    public void setPressed(boolean pressed) {
        this.isPressed = pressed;
        if (pressed) shake();
    }

    public void shake() { shakeIntensity = 1.0f; }
    public void setPosition(float x, float y) { posX = x; posY = y; }
    public void setSize(float s) { size = s; }
    public void setAspectRatio(float r) { aspectRatio = r; }

    public void setScreenSize(int w, int h) {
        screenWidth = w;
        screenHeight = h;
        aspectRatio = (float)w / h;
    }

    public void show() { visible = true; }
    public void hide() { visible = false; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean v) { visible = v; }

    public void dispose() {
        if (textureId != 0) {
            int[] t = {textureId};
            GLES20.glDeleteTextures(1, t, 0);
            textureId = 0;
        }
        if (shaderProgram != 0) {
            GLES20.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        initialized = false;
        Log.d(TAG, "🗑️ GiftButton disposed");
    }
}
