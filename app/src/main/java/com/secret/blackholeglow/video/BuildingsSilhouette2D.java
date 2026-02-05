package com.secret.blackholeglow.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║           🏘️ BuildingsSilhouette2D - Capa de Edificios                  ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Renderiza una imagen PNG de edificios como capa 2D.                     ║
 * ║  Se posiciona en la parte inferior de la pantalla.                       ║
 * ║  Soporta transparencia (alpha blending).                                 ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class BuildingsSilhouette2D {
    private static final String TAG = "BuildingsSilhouette2D";
    private static final String TEXTURE_PATH = "models/buildings_silhouette.png";

    private final Context context;

    // OpenGL resources
    private int shaderProgram = 0;
    private int textureId = 0;
    private FloatBuffer vertexBuffer;

    // Uniform/Attribute locations
    private int aPositionLoc;
    private int aTexCoordLoc;
    private int uTextureLoc;
    private int uAlphaLoc;

    // State
    private boolean initialized = false;
    private float alpha = 1.0f;

    // Position and scale (normalized 0-1)
    private float yOffset = 0.0f;  // Offset vertical (0 = bottom)
    private float height = 0.5f;   // Altura de la capa (0.5 = mitad inferior)

    // Vertex data: position (x,y) + texCoord (u,v)
    private float[] vertices;

    // Shaders
    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "in vec2 aPosition;\n" +
        "in vec2 aTexCoord;\n" +
        "out vec2 vTexCoord;\n" +
        "void main() {\n" +
        "    gl_Position = vec4(aPosition, 0.0, 1.0);\n" +
        "    vTexCoord = aTexCoord;\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "in vec2 vTexCoord;\n" +
        "uniform sampler2D uTexture;\n" +
        "uniform float uAlpha;\n" +
        "out vec4 fragColor;\n" +
        "void main() {\n" +
        "    vec4 texColor = texture(uTexture, vTexCoord);\n" +
        "    fragColor = vec4(texColor.rgb, texColor.a * uAlpha);\n" +
        "}\n";

    public BuildingsSilhouette2D(Context context) {
        this.context = context;
    }

    public void initialize() {
        // Build vertex data for a quad covering bottom portion
        updateVertices();

        // Compile shaders
        int vs = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        if (vs == 0 || fs == 0) {
            Log.e(TAG, "Failed to compile shaders");
            return;
        }

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vs);
        GLES30.glAttachShader(shaderProgram, fs);
        GLES30.glLinkProgram(shaderProgram);

        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(shaderProgram, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Shader link error: " + GLES30.glGetProgramInfoLog(shaderProgram));
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
            return;
        }

        // Get locations
        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "aPosition");
        aTexCoordLoc = GLES30.glGetAttribLocation(shaderProgram, "aTexCoord");
        uTextureLoc = GLES30.glGetUniformLocation(shaderProgram, "uTexture");
        uAlphaLoc = GLES30.glGetUniformLocation(shaderProgram, "uAlpha");

        // Cleanup shaders
        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);

        // Load texture
        loadTexture();

        initialized = true;
        Log.d(TAG, "🏘️ BuildingsSilhouette2D initialized");
    }

    private void updateVertices() {
        // Quad from bottom of screen up to 'height'
        // NDC: -1 to 1, so bottom is -1, top is 1
        float bottom = -1.0f + yOffset * 2.0f;
        float top = bottom + height * 2.0f;

        // Position (x,y) + TexCoord (u,v)
        vertices = new float[] {
            // Bottom-left
            -1f, bottom,  0f, 1f,
            // Bottom-right
             1f, bottom,  1f, 1f,
            // Top-left
            -1f, top,     0f, 0f,
            // Top-right
             1f, top,     1f, 0f
        };

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);
    }

    private void loadTexture() {
        InputStream is = null;
        try {
            is = context.getAssets().open(TEXTURE_PATH);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);

            if (bitmap != null) {
                int[] textures = new int[1];
                GLES30.glGenTextures(1, textures, 0);
                textureId = textures[0];

                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
                GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);

                Log.d(TAG, "🏘️ Buildings texture loaded: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                bitmap.recycle();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error loading buildings texture: " + e.getMessage());
        } finally {
            if (is != null) {
                try { is.close(); } catch (IOException ignored) {}
            }
        }
    }

    public void draw() {
        if (!initialized || shaderProgram == 0 || textureId == 0) return;

        GLES30.glUseProgram(shaderProgram);

        // Enable blending for transparency
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // Bind texture
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(uTextureLoc, 0);
        GLES30.glUniform1f(uAlphaLoc, alpha);

        // Set vertex attributes
        int stride = 4 * 4; // 4 floats * 4 bytes

        vertexBuffer.position(0);
        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, stride, vertexBuffer);

        vertexBuffer.position(2);
        GLES30.glEnableVertexAttribArray(aTexCoordLoc);
        GLES30.glVertexAttribPointer(aTexCoordLoc, 2, GLES30.GL_FLOAT, false, stride, vertexBuffer);

        // Draw quad
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        // Cleanup
        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glDisableVertexAttribArray(aTexCoordLoc);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SETTERS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Set vertical position offset (0 = bottom of screen)
     */
    public void setYOffset(float offset) {
        this.yOffset = offset;
        if (initialized) updateVertices();
    }

    /**
     * Set height of the layer (0.5 = half screen)
     */
    public void setHeight(float h) {
        this.height = h;
        if (initialized) updateVertices();
    }

    /**
     * Set transparency (0 = invisible, 1 = opaque)
     */
    public void setAlpha(float a) {
        this.alpha = a;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GETTERS (for debug/editing)
    // ═══════════════════════════════════════════════════════════════════════════

    public float getYOffset() { return yOffset; }
    public float getHeight() { return height; }
    public float getAlpha() { return alpha; }

    // ═══════════════════════════════════════════════════════════════════════════
    // RELEASE
    // ═══════════════════════════════════════════════════════════════════════════

    public void release() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        if (textureId != 0) {
            int[] textures = {textureId};
            GLES30.glDeleteTextures(1, textures, 0);
            textureId = 0;
        }
        initialized = false;
        Log.d(TAG, "🏘️ BuildingsSilhouette2D released");
    }

    private int compileShader(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Shader compile error: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }
}
