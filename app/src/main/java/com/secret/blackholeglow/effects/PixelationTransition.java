package com.secret.blackholeglow.effects;

import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Efecto de pixeleo al presionar el Xbox controller.
 * Captura el frame actual a un FBO, luego renderiza con pixeles
 * que crecen progresivamente hasta fade a negro.
 */
public class PixelationTransition {
    private static final String TAG = "PixelationTransition";

    private static final float DURATION = 0.8f; // seconds
    private static final float MAX_PIXEL_SIZE = 64.0f;

    // FBO for frame capture
    private int[] fbo = new int[1];
    private int[] fboTexture = new int[1];
    private boolean fboCreated = false;

    // Shader
    private int shaderProgram = 0;
    private int aPositionLoc = -1;
    private int uTextureLoc = -1;
    private int uPixelSizeLoc = -1;
    private int uResolutionLoc = -1;
    private int uFadeLoc = -1;
    private FloatBuffer vertexBuffer;

    // State
    private boolean active = false;
    private float progress = 0f; // 0..1
    private int screenWidth = 1;
    private int screenHeight = 1;
    private boolean initialized = false;

    // Callback
    private Runnable onComplete;

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
        "uniform sampler2D u_Texture;\n" +
        "uniform float u_PixelSize;\n" +
        "uniform vec2 u_Resolution;\n" +
        "uniform float u_Fade;\n" +
        "void main() {\n" +
        "    vec2 pixelUV = floor(v_UV * u_Resolution / u_PixelSize) * u_PixelSize / u_Resolution;\n" +
        "    vec4 color = texture(u_Texture, pixelUV);\n" +
        "    color.rgb *= u_Fade;\n" +
        "    fragColor = color;\n" +
        "}\n";

    public PixelationTransition() {
        initShader();
    }

    private void initShader() {
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
        uTextureLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Texture");
        uPixelSizeLoc = GLES30.glGetUniformLocation(shaderProgram, "u_PixelSize");
        uResolutionLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Resolution");
        uFadeLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Fade");

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
        Log.d(TAG, "Shader initialized");
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

        // Recreate FBO if size changed
        if (fboCreated) {
            releaseFBO();
        }
    }

    /**
     * Creates the FBO for capturing. Call on GL thread.
     */
    private boolean createFBO() {
        if (screenWidth <= 0 || screenHeight <= 0) return false;

        GLES30.glGenFramebuffers(1, fbo, 0);
        GLES30.glGenTextures(1, fboTexture, 0);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, fboTexture[0]);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8,
                screenWidth, screenHeight, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo[0]);
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
                GLES30.GL_TEXTURE_2D, fboTexture[0], 0);

        int status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);

        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG, "FBO incomplete: " + status);
            releaseFBO();
            return false;
        }

        fboCreated = true;
        Log.d(TAG, "FBO created: " + screenWidth + "x" + screenHeight);
        return true;
    }

    /**
     * Begins FBO capture. Call before drawing panel contents.
     * @return true if capturing started, false if FBO unavailable (use fallback)
     */
    public boolean beginCapture() {
        if (!initialized || shaderProgram == 0) return false;

        if (!fboCreated) {
            if (!createFBO()) return false;
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo[0]);
        GLES30.glViewport(0, 0, screenWidth, screenHeight);
        GLES30.glClearColor(0f, 0f, 0f, 1f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        return true;
    }

    /**
     * Ends FBO capture and starts the pixelation animation.
     */
    public void endCaptureAndStart(Runnable onCompleteCallback) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        this.onComplete = onCompleteCallback;
        this.progress = 0f;
        this.active = true;
        Log.d(TAG, "Transition started");
    }

    /**
     * Updates animation progress.
     */
    public void update(float deltaTime) {
        if (!active) return;

        progress += deltaTime / DURATION;
        if (progress >= 1.0f) {
            progress = 1.0f;
            active = false;

            // Release FBO immediately after transition
            releaseFBO();

            if (onComplete != null) {
                onComplete.run();
                onComplete = null;
            }
            Log.d(TAG, "Transition complete");
        }
    }

    /**
     * Draws the pixelation effect. Call during draw phase.
     */
    public void draw() {
        if (!active || !fboCreated || shaderProgram == 0) return;

        // easeInQuad: t^2
        float eased = progress * progress;
        float pixelSize = 1.0f + eased * (MAX_PIXEL_SIZE - 1.0f);

        // Fade to black in last 30% of transition
        float fade;
        if (progress > 0.7f) {
            fade = 1.0f - (progress - 0.7f) / 0.3f;
        } else {
            fade = 1.0f;
        }

        GLES30.glUseProgram(shaderProgram);
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glDisable(GLES30.GL_BLEND);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, fboTexture[0]);
        GLES30.glUniform1i(uTextureLoc, 0);

        GLES30.glUniform1f(uPixelSizeLoc, pixelSize);
        GLES30.glUniform2f(uResolutionLoc, screenWidth, screenHeight);
        GLES30.glUniform1f(uFadeLoc, fade);

        vertexBuffer.position(0);
        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        GLES30.glUseProgram(0);
    }

    public boolean isActive() {
        return active;
    }

    private void releaseFBO() {
        if (fboTexture[0] != 0) {
            GLES30.glDeleteTextures(1, fboTexture, 0);
            fboTexture[0] = 0;
        }
        if (fbo[0] != 0) {
            GLES30.glDeleteFramebuffers(1, fbo, 0);
            fbo[0] = 0;
        }
        fboCreated = false;
    }

    public void release() {
        releaseFBO();
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        active = false;
        initialized = false;
        Log.d(TAG, "Released");
    }
}
