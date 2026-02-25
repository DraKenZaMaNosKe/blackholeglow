package com.secret.blackholeglow.effects;

import android.opengl.GLES30;
import android.util.Log;

import com.secret.blackholeglow.SceneObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Fondo procedural para el panel de control.
 * Campo de estrellas con colores variados y parpadeo individual.
 * Zero texturas, solo hash + sin. Ultra ligero ~0.3ms en Mali-G52.
 */
public class ProceduralPanelBackground implements SceneObject {
    private static final String TAG = "ProceduralPanelBg";

    private static final float TIME_CYCLE = 62.831853f; // 10 * TWO_PI

    private int shaderProgram = 0;
    private int aPositionLoc = -1;
    private int uTimeLoc = -1;
    private int uResolutionLoc = -1;
    private FloatBuffer vertexBuffer;

    private float time = 0f;
    private float screenWidth = 1f;
    private float screenHeight = 1f;
    private boolean initialized = false;

    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "in vec2 a_Position;\n" +
        "out vec2 v_UV;\n" +
        "void main() {\n" +
        "    v_UV = a_Position * 0.5 + 0.5;\n" +
        "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "in vec2 v_UV;\n" +
        "out vec4 fragColor;\n" +
        "uniform float u_Time;\n" +
        "uniform vec2 u_Resolution;\n" +
        "\n" +
        // Hash: pseudo-random per grid cell
        "float hash(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}\n" +
        "\n" +
        "void main(){\n" +
        "    float t=u_Time;\n" +
        "    float gW=80.0;\n" +
        "    float gH=gW*u_Resolution.y/u_Resolution.x;\n" +
        "    vec2 p=floor(v_UV*vec2(gW,gH));\n" +
        "\n" +
        // Black background
        "    vec3 col=vec3(0.0);\n" +
        "\n" +
        // Per-cell random values
        "    float h1=hash(p);\n" +
        "    float h2=hash(p+100.0);\n" +
        "    float h3=hash(p+200.0);\n" +
        "\n" +
        // Star if h1 < density threshold
        "    float isStar=step(h1,0.012);\n" +
        "\n" +
        // Twinkle: each star has its own speed and phase
        "    float twinkle=sin(t*(1.5+h2*2.0)+h2*6.2832)*0.4+0.6;\n" +
        "\n" +
        // Color variety based on h3:
        //   [0.0, 0.4) → white-blue
        //   [0.4, 0.7) → warm white-yellow
        //   [0.7, 0.85) → soft pink
        //   [0.85, 1.0) → cyan
        "    vec3 starCol=vec3(0.7,0.8,1.0);\n" +
        "    starCol=mix(starCol,vec3(1.0,0.95,0.7),step(0.4,h3));\n" +
        "    starCol=mix(starCol,vec3(1.0,0.7,0.8),step(0.7,h3));\n" +
        "    starCol=mix(starCol,vec3(0.5,1.0,0.95),step(0.85,h3));\n" +
        "\n" +
        // Brightness variation
        "    float brightness=0.4+h2*0.6;\n" +
        "\n" +
        "    col=isStar*starCol*twinkle*brightness;\n" +
        "\n" +
        "    fragColor=vec4(col,1.0);\n" +
        "}\n";

    public ProceduralPanelBackground() {
        init();
    }

    private void init() {
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
            Log.e(TAG, "Link error: " + GLES30.glGetProgramInfoLog(shaderProgram));
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
            return;
        }

        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);

        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        uTimeLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Time");
        uResolutionLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Resolution");

        // Fullscreen quad: 2 triangles
        float[] vertices = {
            -1f, -1f,
             1f, -1f,
            -1f,  1f,
             1f,  1f
        };

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        initialized = true;
        Log.d(TAG, "Initialized");
    }

    private int compileShader(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
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

    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }

    @Override
    public void update(float deltaTime) {
        time += deltaTime;
        if (time > TIME_CYCLE) {
            time -= TIME_CYCLE;
        }
    }

    @Override
    public void draw() {
        if (!initialized || shaderProgram == 0) return;

        GLES30.glUseProgram(shaderProgram);

        GLES30.glUniform1f(uTimeLoc, time);
        GLES30.glUniform2f(uResolutionLoc, screenWidth, screenHeight);

        vertexBuffer.position(0);
        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glUseProgram(0);
    }

    public void release() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        initialized = false;
        Log.d(TAG, "Released");
    }
}
