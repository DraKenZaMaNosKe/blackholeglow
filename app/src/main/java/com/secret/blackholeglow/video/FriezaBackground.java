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
 * FriezaBackground - Fondo anime fullscreen con warp streaks.
 * Imagen limpia + rayas tipo viaje en el tiempo que aparecen,
 * se estiran en una dirección y se desvanecen.
 */
public class FriezaBackground {
    private static final String TAG = "FriezaBackground";

    private final Context context;
    private int shaderProgram;
    private int textureId;
    private FloatBuffer quadVertices;
    private float time = 0f;
    private int screenWidth = 1, screenHeight = 1;

    // Dirección de las rayas (ángulo en radianes, ajustable por tap)
    private float dirAngle = 2.36f; // ~135 deg default (diagonal upper-left to lower-right)

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

    // Fragment shader: clean background + warp streaks (time travel effect)
    private static final String FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "uniform sampler2D uTexture;\n" +
        "uniform float uTime;\n" +
        "uniform vec2 uResolution;\n" +
        "uniform float uAngle;\n" +
        "varying vec2 vUV;\n" +
        "\n" +
        "float hash(float n) { return fract(sin(n) * 43758.5453); }\n" +
        "\n" +
        // Warp streak: one layer of streaks that spawn, stretch, and fade
        "float warpStreak(float along, float across, float time, float freq, float speed) {\n" +
        "    float lane = across * freq;\n" +
        "    float cellId = floor(lane);\n" +
        "    float cellFrac = fract(lane);\n" +
        "    float r1 = hash(cellId);\n" +
        "    float r2 = hash(cellId + 100.0);\n" +
        "    float r3 = hash(cellId + 200.0);\n" +
        // Phase: each lane cycles independently
        "    float phase = fract(time * speed * (0.4 + r1 * 0.6) + r1 * 17.0);\n" +
        // Streak head advances, tail follows with delay
        "    float startPos = r2 * 0.6 - 0.5;\n" +
        "    float headPos = startPos + phase * (0.8 + r3 * 0.5);\n" +
        "    float len = 0.01 + phase * (0.08 + r3 * 0.15);\n" +
        "    float tailPos = headPos - len;\n" +
        // Pixel inside streak?
        "    float inStreak = smoothstep(tailPos - 0.005, tailPos, along)\n" +
        "                   * smoothstep(headPos + 0.005, headPos, along);\n" +
        // Thin line: brighter near lane center
        "    float thin = smoothstep(0.5, 0.1, abs(cellFrac - 0.5));\n" +
        // Lifecycle: fade in, then fade out
        "    float fade = smoothstep(0.0, 0.15, phase) * smoothstep(1.0, 0.55, phase);\n" +
        // Brighter at head
        "    float grad = smoothstep(tailPos, headPos, along);\n" +
        "    return inStreak * thin * fade * grad * (0.5 + r2 * 0.5);\n" +
        "}\n" +
        "\n" +
        "void main() {\n" +
        // Cover fit
        "    vec2 uv = vUV;\n" +
        "    float screenAspect = uResolution.x / uResolution.y;\n" +
        "    float imgAspect = 1080.0 / 1920.0;\n" +
        "    if (screenAspect > imgAspect) {\n" +
        "        float scale = screenAspect / imgAspect;\n" +
        "        uv.y = (uv.y - 0.5) / scale + 0.5;\n" +
        "    } else {\n" +
        "        float scale = imgAspect / screenAspect;\n" +
        "        uv.x = (uv.x - 0.5) / scale + 0.5;\n" +
        "    }\n" +
        // Clean background
        "    vec3 color = texture2D(uTexture, uv).rgb;\n" +
        "\n" +
        // Direction from angle uniform
        "    vec2 dir = vec2(cos(uAngle), sin(uAngle));\n" +
        "    vec2 perp = vec2(-dir.y, dir.x);\n" +
        "    vec2 centered = vUV - 0.5;\n" +
        "    float along = dot(centered, dir);\n" +
        "    float across = dot(centered, perp);\n" +
        "\n" +
        // 3 layers of warp streaks (different density/speed for depth)
        "    float s1 = warpStreak(along, across, uTime, 30.0, 0.8);\n" +
        "    float s2 = warpStreak(along, across + 3.7, uTime, 18.0, 0.5);\n" +
        "    float s3 = warpStreak(along, across + 7.3, uTime, 10.0, 0.3);\n" +
        "\n" +
        "    float streaks = s1 * 0.4 + s2 * 0.25 + s3 * 0.12;\n" +
        "    vec3 streakColor = vec3(0.75, 1.0, 0.8);\n" +
        "    color += streakColor * streaks;\n" +
        "\n" +
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
        String path = imageMgr.getImagePath("frieza_deathbeam_bg.png");

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

        int angleLoc = GLES20.glGetUniformLocation(shaderProgram, "uAngle");

        GLES20.glUniform1f(timeLoc, time);
        GLES20.glUniform2f(resLoc, screenWidth, screenHeight);
        GLES20.glUniform1f(angleLoc, dirAngle);

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

    public void setDirectionAngle(float angle) { this.dirAngle = angle; }
    public float getDirectionAngle() { return dirAngle; }

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
