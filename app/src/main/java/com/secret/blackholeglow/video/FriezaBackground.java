package com.secret.blackholeglow.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import com.secret.blackholeglow.image.ImageDownloadManager;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * FriezaBackground - Fondo anime fullscreen con efectos shader.
 * Carga la imagen de fondo desde Supabase y le aplica:
 * - Speed lines anime radiando desde el punto de energia
 * - Chromatic pulse sutil
 * - Leve zoom/pan animado
 */
public class FriezaBackground {
    private static final String TAG = "FriezaBackground";

    private final Context context;
    private int shaderProgram;
    private int textureId;
    private FloatBuffer quadVertices;
    private float time = 0f;
    private int screenWidth = 1, screenHeight = 1;

    // Fullscreen quad (position + UV)
    private static final float[] QUAD_DATA = {
        // x,    y,    u,   v
        -1f, -1f,  0f, 1f,
         1f, -1f,  1f, 1f,
        -1f,  1f,  0f, 0f,
         1f,  1f,  1f, 0f,
    };

    private static final String VERTEX_SHADER =
        "attribute vec2 aPosition;\n" +
        "attribute vec2 aTexCoord;\n" +
        "varying vec2 vUV;\n" +
        "void main() {\n" +
        "    gl_Position = vec4(aPosition, 0.0, 1.0);\n" +
        "    vUV = aTexCoord;\n" +
        "}\n";

    // Fragment shader: background image + speed lines + chromatic pulse
    private static final String FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "uniform sampler2D uTexture;\n" +
        "uniform float uTime;\n" +
        "uniform vec2 uResolution;\n" +
        "varying vec2 vUV;\n" +
        "void main() {\n" +
        // Subtle pan/zoom over time
        "    vec2 uv = vUV;\n" +
        "    float zoom = 1.0 + 0.02 * sin(uTime * 0.3);\n" +
        "    uv = (uv - 0.5) / zoom + 0.5;\n" +
        "    uv.x += 0.005 * sin(uTime * 0.2);\n" +
        // Sample background
        "    vec4 bg = texture2D(uTexture, uv);\n" +
        // Speed lines from energy source point (upper left area where fist is)
        "    vec2 center = vec2(0.35, 0.3);\n" +  // approximate fist position in UV
        "    vec2 dir = vUV - center;\n" +
        "    float dist = length(dir);\n" +
        "    float angle = atan(dir.y, dir.x);\n" +
        // Radial speed lines
        "    float lines = sin(angle * 40.0 + uTime * 2.0) * 0.5 + 0.5;\n" +
        "    lines = pow(lines, 8.0);\n" + // sharp lines
        "    float lineFade = smoothstep(0.1, 0.5, dist) * smoothstep(1.0, 0.6, dist);\n" +
        "    float lineIntensity = lines * lineFade * 0.15;\n" +
        // Purple energy tint from center
        "    vec3 energyColor = vec3(0.5, 0.1, 0.8);\n" +
        "    float energyGlow = exp(-dist * 3.0) * (0.15 + 0.08 * sin(uTime * 3.0));\n" +
        // Chromatic aberration pulse (very subtle)
        "    float chromaOffset = 0.002 * sin(uTime * 2.0);\n" +
        "    float r = texture2D(uTexture, uv + vec2(chromaOffset, 0.0)).r;\n" +
        "    float b = texture2D(uTexture, uv - vec2(chromaOffset, 0.0)).b;\n" +
        "    vec3 color = vec3(r, bg.g, b);\n" +
        // Combine
        "    color += energyColor * lineIntensity;\n" +
        "    color += energyColor * energyGlow;\n" +
        // Slight vignette
        "    float vignette = 1.0 - 0.3 * pow(length(vUV - 0.5) * 1.4, 2.0);\n" +
        "    color *= vignette;\n" +
        "    gl_FragColor = vec4(color, 1.0);\n" +
        "}\n";

    public FriezaBackground(Context context) {
        this.context = context;
        initialize();
    }

    private void initialize() {
        // Build quad buffer
        ByteBuffer bb = ByteBuffer.allocateDirect(QUAD_DATA.length * 4);
        bb.order(ByteOrder.nativeOrder());
        quadVertices = bb.asFloatBuffer();
        quadVertices.put(QUAD_DATA);
        quadVertices.position(0);

        // Compile shader
        shaderProgram = buildProgram();

        // Load background texture
        loadBackgroundTexture();
    }

    private void loadBackgroundTexture() {
        ImageDownloadManager imageMgr = ImageDownloadManager.getInstance(context);
        String path = imageMgr.getImagePath("frieza_bg_anime.png");

        if (path != null && new File(path).exists()) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inScaled = false;
            Bitmap bmp = BitmapFactory.decodeFile(path, opts);
            if (bmp != null) {
                int[] texIds = new int[1];
                GLES20.glGenTextures(1, texIds, 0);
                textureId = texIds[0];
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
                bmp.recycle();
                Log.d(TAG, "Background texture loaded: " + textureId);
            }
        } else {
            Log.w(TAG, "Background image not downloaded yet");
        }
    }

    public void update(float deltaTime) {
        time += deltaTime;
        if (time > 1000f) time -= 1000f;
    }

    public void draw() {
        if (shaderProgram == 0 || textureId == 0) return;

        GLES20.glUseProgram(shaderProgram);

        int posLoc = GLES20.glGetAttribLocation(shaderProgram, "aPosition");
        int uvLoc = GLES20.glGetAttribLocation(shaderProgram, "aTexCoord");
        int texLoc = GLES20.glGetUniformLocation(shaderProgram, "uTexture");
        int timeLoc = GLES20.glGetUniformLocation(shaderProgram, "uTime");
        int resLoc = GLES20.glGetUniformLocation(shaderProgram, "uResolution");

        GLES20.glUniform1f(timeLoc, time);
        GLES20.glUniform2f(resLoc, screenWidth, screenHeight);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(texLoc, 0);

        quadVertices.position(0);
        GLES20.glEnableVertexAttribArray(posLoc);
        GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 16, quadVertices);

        quadVertices.position(2);
        GLES20.glEnableVertexAttribArray(uvLoc);
        GLES20.glVertexAttribPointer(uvLoc, 2, GLES20.GL_FLOAT, false, 16, quadVertices);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(posLoc);
        GLES20.glDisableVertexAttribArray(uvLoc);
    }

    public void setScreenSize(int w, int h) { screenWidth = w; screenHeight = h; }

    private int buildProgram() {
        int vs = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        if (vs == 0 || fs == 0) return 0;

        int prog = GLES20.glCreateProgram();
        GLES20.glAttachShader(prog, vs);
        GLES20.glAttachShader(prog, fs);
        GLES20.glLinkProgram(prog);

        int[] linked = new int[1];
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            Log.e(TAG, "Shader link error: " + GLES20.glGetProgramInfoLog(prog));
            GLES20.glDeleteProgram(prog);
            return 0;
        }
        GLES20.glDeleteShader(vs);
        GLES20.glDeleteShader(fs);
        return prog;
    }

    private int compileShader(int type, String source) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, source);
        GLES20.glCompileShader(s);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Shader error: " + GLES20.glGetShaderInfoLog(s));
            GLES20.glDeleteShader(s);
            return 0;
        }
        return s;
    }

    public void release() {
        if (shaderProgram != 0) {
            GLES20.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        if (textureId != 0) {
            GLES20.glDeleteTextures(1, new int[]{textureId}, 0);
            textureId = 0;
        }
        Log.d(TAG, "FriezaBackground released");
    }
}
