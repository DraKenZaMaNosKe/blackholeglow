package com.secret.blackholeglow.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import com.secret.blackholeglow.image.ImageDownloadManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║           🌙 NightSkyRenderer - Cielo Nocturno Procedural               ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Renderiza un cielo nocturno completo con un solo shader fullscreen:     ║
 * ║  • Gradiente dark blue → black                                          ║
 * ║  • Estrellas procedurales con twinkling                                 ║
 * ║  • Luna con glow suave                                                  ║
 * ║  • Silueta de edificios en la parte inferior                            ║
 * ║                                                                          ║
 * ║  Todo en un solo draw call para máxima eficiencia.                      ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class NightSkyRenderer {
    private static final String TAG = "NightSkyRenderer";

    private final Context context;

    // OpenGL resources
    private int shaderProgram = 0;
    private FloatBuffer vertexBuffer;
    private int moonTextureId = 0;

    // Uniform locations
    private int uTimeLoc;
    private int uResolutionLoc;
    private int uMoonTextureLoc;

    // Attribute locations
    private int aPositionLoc;

    // Moon texture path
    private static final String MOON_TEXTURE_PATH = "models/moon_texture.png";

    // State
    private float time = 0f;
    private int screenWidth = 1;
    private int screenHeight = 1;
    private boolean initialized = false;

    // Moon parameters (final positions 2026-02-05)
    private float moonPosX = 0.538f;
    private float moonPosY = 0.715f;
    private float moonRadius = 0.136f;
    private int uMoonPosLoc;
    private int uMoonRadiusLoc;

    // NOTE: Procedural buildings removed from shader for performance
    // Using PNG overlay (BuildingsSilhouette2D) instead

    // ═══════════════════════════════════════════════════════════════════════════
    // FULLSCREEN QUAD VERTICES (NDC: -1 to 1, triangle strip)
    // ═══════════════════════════════════════════════════════════════════════════
    private static final float[] QUAD_VERTICES = {
        -1f, -1f,   // Bottom-left
         1f, -1f,   // Bottom-right
        -1f,  1f,   // Top-left
         1f,  1f    // Top-right
    };

    // ═══════════════════════════════════════════════════════════════════════════
    // VERTEX SHADER - Simple passthrough
    // ═══════════════════════════════════════════════════════════════════════════
    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "in vec2 aPosition;\n" +
        "void main() {\n" +
        "    gl_Position = vec4(aPosition, 0.0, 1.0);\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════════════════
    // FRAGMENT SHADER - Cielo nocturno completo
    // ═══════════════════════════════════════════════════════════════════════════
    // ═══════════════════════════════════════════════════════════════════════════
    // FRAGMENT SHADER - Cielo nocturno OPTIMIZADO
    // ═══════════════════════════════════════════════════════════════════════════
    // OPTIMIZATIONS:
    // - Removed procedural buildings (using PNG overlay instead)
    // - Reduced star layers from 3 to 2
    // - Simplified hash calculations
    // - Fewer branches in main loop
    // ═══════════════════════════════════════════════════════════════════════════
    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "\n" +
        "uniform float u_Time;\n" +
        "uniform vec2 u_Resolution;\n" +
        "uniform sampler2D u_MoonTexture;\n" +
        "uniform vec2 u_MoonPos;\n" +
        "uniform float u_MoonRadius;\n" +
        "out vec4 fragColor;\n" +
        "\n" +
        "// ═══════════════════════════════════════════\n" +
        "// OPTIMIZED HASH - single function\n" +
        "// ═══════════════════════════════════════════\n" +
        "float hash(vec2 p) {\n" +
        "    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);\n" +
        "}\n" +
        "\n" +
        "// ═══════════════════════════════════════════\n" +
        "// NIGHT SKY GRADIENT - simplified\n" +
        "// ═══════════════════════════════════════════\n" +
        "vec3 skyGradient(float y) {\n" +
        "    vec3 bottom = vec3(0.02, 0.02, 0.06);\n" +
        "    vec3 top = vec3(0.04, 0.04, 0.18);\n" +
        "    return mix(bottom, top, smoothstep(0.0, 1.0, y));\n" +
        "}\n" +
        "\n" +
        "// ═══════════════════════════════════════════\n" +
        "// OPTIMIZED STARS - 2 layers instead of 3\n" +
        "// ═══════════════════════════════════════════\n" +
        "float stars(vec2 uv, float time) {\n" +
        "    float total = 0.0;\n" +
        "    \n" +
        "    // Layer 1: Small stars\n" +
        "    vec2 grid1 = uv * vec2(60.0, 90.0);\n" +
        "    vec2 id1 = floor(grid1);\n" +
        "    vec2 f1 = fract(grid1) - 0.5;\n" +
        "    float rnd1 = hash(id1);\n" +
        "    f1 -= (vec2(hash(id1 + 1.0), hash(id1 + 2.0)) - 0.5) * 0.5;\n" +
        "    float d1 = length(f1);\n" +
        "    float twinkle1 = sin(time * (1.5 + rnd1 * 2.5) + rnd1 * 6.28) * 0.4 + 0.6;\n" +
        "    total += smoothstep(0.05, 0.0, d1) * step(0.93, rnd1) * twinkle1 * 0.8;\n" +
        "    \n" +
        "    // Layer 2: Bright stars (fewer)\n" +
        "    vec2 grid2 = uv * vec2(25.0, 35.0);\n" +
        "    vec2 id2 = floor(grid2);\n" +
        "    vec2 f2 = fract(grid2) - 0.5;\n" +
        "    float rnd2 = hash(id2 + 77.0);\n" +
        "    f2 -= (vec2(hash(id2 + 3.0), hash(id2 + 4.0)) - 0.5) * 0.4;\n" +
        "    float d2 = length(f2);\n" +
        "    float twinkle2 = sin(time * (1.0 + rnd2 * 2.0) + rnd2 * 6.28) * 0.3 + 0.7;\n" +
        "    total += smoothstep(0.04, 0.0, d2) * step(0.96, rnd2) * twinkle2 * 1.2;\n" +
        "    \n" +
        "    // Fade near bottom (buildings area)\n" +
        "    return total * smoothstep(0.2, 0.4, uv.y);\n" +
        "}\n" +
        "\n" +
        "// ═══════════════════════════════════════════\n" +
        "// MOON with TEXTURE - aspect ratio corrected\n" +
        "// ═══════════════════════════════════════════\n" +
        "vec3 moon(vec2 uv, vec2 pos, float r, sampler2D tex, float aspect) {\n" +
        "    // Correct for aspect ratio to make moon circular\n" +
        "    vec2 diff = uv - pos;\n" +
        "    diff.x *= aspect;  // Scale X by aspect ratio\n" +
        "    float d = length(diff);\n" +
        "    \n" +
        "    // Early exit for pixels far from moon\n" +
        "    if (d > r * 4.0) return vec3(0.0);\n" +
        "    \n" +
        "    vec3 result = vec3(0.0);\n" +
        "    \n" +
        "    // Moon disk with texture\n" +
        "    if (d < r) {\n" +
        "        vec2 moonUV = diff / (r * 2.0) + 0.5;\n" +
        "        vec4 texCol = texture(tex, moonUV);\n" +
        "        float mask = smoothstep(r, r - 0.003, d);\n" +
        "        result = texCol.rgb * mask * texCol.a;\n" +
        "    }\n" +
        "    \n" +
        "    // Glow (combined inner + outer)\n" +
        "    vec3 glowCol = vec3(0.9, 0.8, 0.5);\n" +
        "    float glow = smoothstep(r * 3.5, r * 0.9, d) * 0.25;\n" +
        "    result += glowCol * glow * (1.0 - smoothstep(0.0, r, d));\n" +
        "    \n" +
        "    return result;\n" +
        "}\n" +
        "\n" +
        "// ═══════════════════════════════════════════\n" +
        "// MAIN - streamlined\n" +
        "// ═══════════════════════════════════════════\n" +
        "void main() {\n" +
        "    vec2 uv = gl_FragCoord.xy / u_Resolution;\n" +
        "    float aspect = u_Resolution.x / u_Resolution.y;\n" +
        "    \n" +
        "    // 1. Sky gradient\n" +
        "    vec3 col = skyGradient(uv.y);\n" +
        "    \n" +
        "    // 2. Stars\n" +
        "    col += vec3(0.9, 0.92, 1.0) * stars(uv, u_Time);\n" +
        "    \n" +
        "    // 3. Moon (with aspect ratio correction for circular shape)\n" +
        "    col += moon(uv, u_MoonPos, u_MoonRadius, u_MoonTexture, aspect);\n" +
        "    \n" +
        "    // 4. Subtle moonlight ambient\n" +
        "    col += vec3(0.1, 0.15, 0.3) * smoothstep(0.3, 0.9, uv.y) * 0.025;\n" +
        "    \n" +
        "    fragColor = vec4(col, 1.0);\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR & INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════

    public NightSkyRenderer(Context context) {
        this.context = context;
    }

    /**
     * Initialize OpenGL resources. Must be called on GL thread.
     */
    public void initialize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;

        // Create vertex buffer for fullscreen quad
        ByteBuffer bb = ByteBuffer.allocateDirect(QUAD_VERTICES.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(QUAD_VERTICES);
        vertexBuffer.position(0);

        // Compile shader program
        int vs = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        if (vs == 0 || fs == 0) {
            Log.e(TAG, "❌ Failed to compile shaders");
            return;
        }

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vs);
        GLES30.glAttachShader(shaderProgram, fs);
        GLES30.glLinkProgram(shaderProgram);

        // Check link status
        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(shaderProgram, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "❌ Shader link error: " + GLES30.glGetProgramInfoLog(shaderProgram));
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
            return;
        }

        // Get locations
        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "aPosition");
        uTimeLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Time");
        uResolutionLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Resolution");
        uMoonTextureLoc = GLES30.glGetUniformLocation(shaderProgram, "u_MoonTexture");
        uMoonPosLoc = GLES30.glGetUniformLocation(shaderProgram, "u_MoonPos");
        uMoonRadiusLoc = GLES30.glGetUniformLocation(shaderProgram, "u_MoonRadius");

        // Cleanup shaders (linked into program)
        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);

        // Load moon texture
        loadMoonTexture();

        initialized = true;
        Log.d(TAG, "🌙 NightSkyRenderer initialized (" + width + "x" + height + ")");
    }

    /**
     * Load moon texture - Supabase download first, assets fallback
     */
    private void loadMoonTexture() {
        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inSampleSize = 2;  // La luna ocupa ~15% de pantalla, no necesita full res
        options.inPreferredConfig = Bitmap.Config.RGB_565;  // 50% menos VRAM

        // Prioridad: descarga remota (Supabase)
        ImageDownloadManager imageMgr = ImageDownloadManager.getInstance(context);
        String texturePath = imageMgr.getImagePath("moon_texture.png");

        if (texturePath != null && new File(texturePath).exists()) {
            bitmap = BitmapFactory.decodeFile(texturePath, options);
            if (bitmap != null) {
                Log.d(TAG, "🌐 Moon texture from download");
            }
        }

        // Fallback: assets locales
        if (bitmap == null) {
            InputStream is = null;
            try {
                is = context.getAssets().open(MOON_TEXTURE_PATH);
                bitmap = BitmapFactory.decodeStream(is, null, options);
                if (bitmap != null) {
                    Log.d(TAG, "📂 Moon texture from assets");
                }
            } catch (IOException e) {
                Log.e(TAG, "❌ Error loading moon texture: " + e.getMessage());
            } finally {
                if (is != null) {
                    try { is.close(); } catch (IOException ignored) {}
                }
            }
        }

        if (bitmap != null) {
            int[] textures = new int[1];
            GLES30.glGenTextures(1, textures, 0);
            moonTextureId = textures[0];

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, moonTextureId);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();
            Log.d(TAG, "🌕 Moon texture loaded: " + moonTextureId);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE & DRAW
    // ═══════════════════════════════════════════════════════════════════════════

    public void update(float deltaTime) {
        time += deltaTime;
        // Wrap to avoid float precision loss
        if (time > 1000f) time -= 1000f;
    }

    /**
     * Draw the night sky fullscreen. Should be called with depth test DISABLED.
     */
    public void draw() {
        if (!initialized || shaderProgram == 0) return;

        GLES30.glUseProgram(shaderProgram);

        // Set uniforms
        GLES30.glUniform1f(uTimeLoc, time);
        GLES30.glUniform2f(uResolutionLoc, (float) screenWidth, (float) screenHeight);
        GLES30.glUniform2f(uMoonPosLoc, moonPosX, moonPosY);
        GLES30.glUniform1f(uMoonRadiusLoc, moonRadius);

        // Bind moon texture to texture unit 0
        if (moonTextureId != 0) {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, moonTextureId);
            GLES30.glUniform1i(uMoonTextureLoc, 0);
        }

        // Set vertex attribute
        vertexBuffer.position(0);
        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        // Draw fullscreen quad
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        // Cleanup
        GLES30.glDisableVertexAttribArray(aPositionLoc);
    }

    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MOON CONTROLS (for editing)
    // ═══════════════════════════════════════════════════════════════════════════

    public void setMoonPosition(float x, float y) {
        this.moonPosX = x;
        this.moonPosY = y;
    }

    public void setMoonRadius(float r) {
        this.moonRadius = r;
    }

    public float getMoonPosX() { return moonPosX; }
    public float getMoonPosY() { return moonPosY; }
    public float getMoonRadius() { return moonRadius; }

    // ═══════════════════════════════════════════════════════════════════════════
    // RELEASE
    // ═══════════════════════════════════════════════════════════════════════════

    public void release() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        if (moonTextureId != 0) {
            int[] textures = {moonTextureId};
            GLES30.glDeleteTextures(1, textures, 0);
            moonTextureId = 0;
        }
        initialized = false;
        Log.d(TAG, "🌙 NightSkyRenderer released");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SHADER COMPILATION
    // ═══════════════════════════════════════════════════════════════════════════

    private int compileShader(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            String typeStr = (type == GLES30.GL_VERTEX_SHADER) ? "vertex" : "fragment";
            Log.e(TAG, "❌ " + typeStr + " shader error: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }
}
