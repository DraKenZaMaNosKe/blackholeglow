package com.secret.blackholeglow.effects;

import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   ✨ BloomEffect - Post-Processing Bloom con OpenGL ES 3.0              ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║   Pipeline:                                                              ║
 * ║   1. Renderizar escena a FBO principal                                   ║
 * ║   2. Extraer pixeles brillantes (bright pass)                            ║
 * ║   3. Aplicar blur gaussiano horizontal                                   ║
 * ║   4. Aplicar blur gaussiano vertical                                     ║
 * ║   5. Combinar escena original + bloom                                    ║
 * ║                                                                          ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class BloomEffect {
    private static final String TAG = "BloomEffect";

    // Configuración
    private static final float BLOOM_THRESHOLD = 0.7f;   // Umbral de brillo
    private static final float BLOOM_INTENSITY = 0.8f;   // Intensidad del bloom
    private static final int BLUR_PASSES = 2;            // Número de pasadas de blur

    // Dimensiones (reducidas para blur = mejor performance)
    private int screenWidth = 1;
    private int screenHeight = 1;
    private int bloomWidth = 1;
    private int bloomHeight = 1;
    private static final int BLOOM_SCALE = 4;  // Bloom a 1/4 de resolución

    // FBOs
    private int[] sceneFBO = new int[1];
    private int[] sceneTexture = new int[1];
    private int[] sceneDepth = new int[1];

    private int[] brightFBO = new int[1];
    private int[] brightTexture = new int[1];

    private int[] blurFBO = new int[2];      // Ping-pong buffers
    private int[] blurTexture = new int[2];

    // Shaders
    private int brightPassProgram;
    private int blurProgram;
    private int compositeProgram;

    // Quad para fullscreen pass
    private FloatBuffer quadBuffer;
    private int quadVAO;
    private int quadVBO;

    // Uniform locations
    private int uBrightThreshold;
    private int uBlurHorizontal;
    private int uBlurTexelSize;
    private int uCompositeBloomIntensity;

    // Estado
    private boolean initialized = false;
    private boolean enabled = false;  // DESHABILITADO por defecto hasta pruebas
    private boolean capturing = false;

    // ═══════════════════════════════════════════════════════════════════════════
    // SHADERS GLSL ES 3.0
    // ═══════════════════════════════════════════════════════════════════════════

    private static final String FULLSCREEN_VERTEX =
        "#version 300 es\n" +
        "precision highp float;\n" +
        "layout(location = 0) in vec2 aPosition;\n" +
        "out vec2 vTexCoord;\n" +
        "void main() {\n" +
        "    vTexCoord = aPosition * 0.5 + 0.5;\n" +
        "    gl_Position = vec4(aPosition, 0.0, 1.0);\n" +
        "}\n";

    // Bright Pass: Extrae solo los píxeles brillantes
    private static final String BRIGHT_PASS_FRAGMENT =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "in vec2 vTexCoord;\n" +
        "out vec4 fragColor;\n" +
        "uniform sampler2D uScene;\n" +
        "uniform float uThreshold;\n" +
        "void main() {\n" +
        "    vec4 color = texture(uScene, vTexCoord);\n" +
        "    float brightness = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));\n" +
        "    if (brightness > uThreshold) {\n" +
        "        fragColor = vec4(color.rgb * (brightness - uThreshold), 1.0);\n" +
        "    } else {\n" +
        "        fragColor = vec4(0.0);\n" +
        "    }\n" +
        "}\n";

    // Blur Gaussiano (9 taps)
    private static final String BLUR_FRAGMENT =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "in vec2 vTexCoord;\n" +
        "out vec4 fragColor;\n" +
        "uniform sampler2D uTexture;\n" +
        "uniform bool uHorizontal;\n" +
        "uniform vec2 uTexelSize;\n" +
        "\n" +
        "const float weights[5] = float[](0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);\n" +
        "\n" +
        "void main() {\n" +
        "    vec3 result = texture(uTexture, vTexCoord).rgb * weights[0];\n" +
        "    vec2 offset = uHorizontal ? vec2(uTexelSize.x, 0.0) : vec2(0.0, uTexelSize.y);\n" +
        "    \n" +
        "    for (int i = 1; i < 5; i++) {\n" +
        "        result += texture(uTexture, vTexCoord + offset * float(i)).rgb * weights[i];\n" +
        "        result += texture(uTexture, vTexCoord - offset * float(i)).rgb * weights[i];\n" +
        "    }\n" +
        "    fragColor = vec4(result, 1.0);\n" +
        "}\n";

    // Composite: Combina escena original + bloom
    private static final String COMPOSITE_FRAGMENT =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "in vec2 vTexCoord;\n" +
        "out vec4 fragColor;\n" +
        "uniform sampler2D uScene;\n" +
        "uniform sampler2D uBloom;\n" +
        "uniform float uBloomIntensity;\n" +
        "void main() {\n" +
        "    vec3 scene = texture(uScene, vTexCoord).rgb;\n" +
        "    vec3 bloom = texture(uBloom, vTexCoord).rgb;\n" +
        "    vec3 result = scene + bloom * uBloomIntensity;\n" +
        "    // Tone mapping simple para evitar saturación\n" +
        "    result = result / (result + vec3(1.0));\n" +
        "    fragColor = vec4(result, 1.0);\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════════════════
    // INICIALIZACIÓN
    // ═══════════════════════════════════════════════════════════════════════════

    public BloomEffect() {
        Log.d(TAG, "✨ Creando BloomEffect...");
        initQuadBuffer();
    }

    private void initQuadBuffer() {
        // Fullscreen quad: 2 triángulos
        float[] quadVertices = {
            -1.0f, -1.0f,
             1.0f, -1.0f,
            -1.0f,  1.0f,
             1.0f, -1.0f,
             1.0f,  1.0f,
            -1.0f,  1.0f
        };

        ByteBuffer bb = ByteBuffer.allocateDirect(quadVertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        quadBuffer = bb.asFloatBuffer();
        quadBuffer.put(quadVertices);
        quadBuffer.position(0);
    }

    public void initialize(int width, int height) {
        if (width <= 0 || height <= 0) return;

        Log.d(TAG, "✨ Inicializando BloomEffect: " + width + "x" + height);

        screenWidth = width;
        screenHeight = height;
        bloomWidth = Math.max(1, width / BLOOM_SCALE);
        bloomHeight = Math.max(1, height / BLOOM_SCALE);

        // Compilar shaders
        brightPassProgram = compileProgram(FULLSCREEN_VERTEX, BRIGHT_PASS_FRAGMENT);
        blurProgram = compileProgram(FULLSCREEN_VERTEX, BLUR_FRAGMENT);
        compositeProgram = compileProgram(FULLSCREEN_VERTEX, COMPOSITE_FRAGMENT);

        if (brightPassProgram == 0 || blurProgram == 0 || compositeProgram == 0) {
            Log.e(TAG, "✗ Error compilando shaders de bloom");
            enabled = false;
            return;
        }

        // Obtener uniform locations
        uBrightThreshold = GLES30.glGetUniformLocation(brightPassProgram, "uThreshold");
        uBlurHorizontal = GLES30.glGetUniformLocation(blurProgram, "uHorizontal");
        uBlurTexelSize = GLES30.glGetUniformLocation(blurProgram, "uTexelSize");
        uCompositeBloomIntensity = GLES30.glGetUniformLocation(compositeProgram, "uBloomIntensity");

        // Crear FBOs
        createSceneFBO();
        createBloomFBOs();
        createQuadVAO();

        initialized = true;
        Log.d(TAG, "✓ BloomEffect inicializado (bloom: " + bloomWidth + "x" + bloomHeight + ")");
    }

    private void createSceneFBO() {
        // FBO para capturar la escena completa
        GLES30.glGenFramebuffers(1, sceneFBO, 0);
        GLES30.glGenTextures(1, sceneTexture, 0);
        GLES30.glGenRenderbuffers(1, sceneDepth, 0);

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, sceneFBO[0]);

        // Textura de color
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sceneTexture[0]);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8,
            screenWidth, screenHeight, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D, sceneTexture[0], 0);

        // Renderbuffer de profundidad
        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, sceneDepth[0]);
        GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER, GLES30.GL_DEPTH_COMPONENT16,
            screenWidth, screenHeight);
        GLES30.glFramebufferRenderbuffer(GLES30.GL_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT,
            GLES30.GL_RENDERBUFFER, sceneDepth[0]);

        int status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER);
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG, "✗ Scene FBO incompleto: " + status);
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
    }

    private void createBloomFBOs() {
        // FBO para bright pass
        GLES30.glGenFramebuffers(1, brightFBO, 0);
        GLES30.glGenTextures(1, brightTexture, 0);

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, brightFBO[0]);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, brightTexture[0]);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8,
            bloomWidth, bloomHeight, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D, brightTexture[0], 0);

        // FBOs ping-pong para blur
        GLES30.glGenFramebuffers(2, blurFBO, 0);
        GLES30.glGenTextures(2, blurTexture, 0);

        for (int i = 0; i < 2; i++) {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, blurFBO[i]);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, blurTexture[i]);
            GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8,
                bloomWidth, bloomHeight, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
                GLES30.GL_TEXTURE_2D, blurTexture[i], 0);
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
    }

    private void createQuadVAO() {
        int[] vao = new int[1];
        int[] vbo = new int[1];

        GLES30.glGenVertexArrays(1, vao, 0);
        GLES30.glGenBuffers(1, vbo, 0);

        quadVAO = vao[0];
        quadVBO = vbo[0];

        GLES30.glBindVertexArray(quadVAO);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, quadVBO);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, quadBuffer.capacity() * 4, quadBuffer, GLES30.GL_STATIC_DRAW);

        GLES30.glEnableVertexAttribArray(0);
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, 0);

        GLES30.glBindVertexArray(0);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CAPTURA Y APLICACIÓN DEL BLOOM
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Comienza a capturar la escena al FBO
     * Llamar ANTES de dibujar la escena
     */
    public void beginCapture() {
        if (!initialized || !enabled) return;

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, sceneFBO[0]);
        GLES30.glViewport(0, 0, screenWidth, screenHeight);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        capturing = true;
    }

    /**
     * Termina la captura y aplica el efecto bloom
     * Llamar DESPUÉS de dibujar la escena
     */
    public void endCaptureAndApply() {
        if (!initialized || !enabled || !capturing) {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
            return;
        }

        capturing = false;

        // Guardar estado
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glDisable(GLES30.GL_BLEND);

        // 1. Bright Pass: Extraer píxeles brillantes
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, brightFBO[0]);
        GLES30.glViewport(0, 0, bloomWidth, bloomHeight);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);

        GLES30.glUseProgram(brightPassProgram);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sceneTexture[0]);
        GLES30.glUniform1i(GLES30.glGetUniformLocation(brightPassProgram, "uScene"), 0);
        GLES30.glUniform1f(uBrightThreshold, BLOOM_THRESHOLD);
        drawFullscreenQuad();

        // 2. Blur Gaussiano (ping-pong)
        boolean horizontal = true;
        int inputTexture = brightTexture[0];

        GLES30.glUseProgram(blurProgram);
        GLES30.glUniform1i(GLES30.glGetUniformLocation(blurProgram, "uTexture"), 0);
        GLES30.glUniform2f(uBlurTexelSize, 1.0f / bloomWidth, 1.0f / bloomHeight);

        for (int i = 0; i < BLUR_PASSES * 2; i++) {
            int targetFBO = blurFBO[horizontal ? 0 : 1];

            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, targetFBO);
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inputTexture);
            GLES30.glUniform1i(uBlurHorizontal, horizontal ? 1 : 0);
            drawFullscreenQuad();

            inputTexture = blurTexture[horizontal ? 0 : 1];
            horizontal = !horizontal;
        }

        // 3. Composite: Combinar escena + bloom
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        GLES30.glViewport(0, 0, screenWidth, screenHeight);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        GLES30.glUseProgram(compositeProgram);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sceneTexture[0]);
        GLES30.glUniform1i(GLES30.glGetUniformLocation(compositeProgram, "uScene"), 0);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inputTexture);  // Último resultado del blur
        GLES30.glUniform1i(GLES30.glGetUniformLocation(compositeProgram, "uBloom"), 1);

        GLES30.glUniform1f(uCompositeBloomIntensity, BLOOM_INTENSITY);
        drawFullscreenQuad();

        // Restaurar estado
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void drawFullscreenQuad() {
        GLES30.glBindVertexArray(quadVAO);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 6);
        GLES30.glBindVertexArray(0);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════════════════════

    private int compileProgram(String vertexSrc, String fragmentSrc) {
        int vs = compileShader(GLES30.GL_VERTEX_SHADER, vertexSrc);
        int fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSrc);
        if (vs == 0 || fs == 0) {
            // 🔧 FIX: Limpiar shader válido antes de retornar
            if (vs != 0) GLES30.glDeleteShader(vs);
            if (fs != 0) GLES30.glDeleteShader(fs);
            return 0;
        }

        int program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vs);
        GLES30.glAttachShader(program, fs);
        GLES30.glLinkProgram(program);

        int[] linked = new int[1];
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            Log.e(TAG, "Link error: " + GLES30.glGetProgramInfoLog(program));
            GLES30.glDeleteProgram(program);
            return 0;
        }

        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);

        return program;
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

    public void resize(int width, int height) {
        if (width == screenWidth && height == screenHeight) return;

        release();
        initialize(width, height);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        Log.d(TAG, "Bloom " + (enabled ? "habilitado" : "deshabilitado"));
    }

    public boolean isEnabled() {
        return enabled && initialized;
    }

    public void release() {
        if (!initialized) return;

        Log.d(TAG, "Liberando BloomEffect...");

        // Eliminar FBOs
        GLES30.glDeleteFramebuffers(1, sceneFBO, 0);
        GLES30.glDeleteTextures(1, sceneTexture, 0);
        GLES30.glDeleteRenderbuffers(1, sceneDepth, 0);

        GLES30.glDeleteFramebuffers(1, brightFBO, 0);
        GLES30.glDeleteTextures(1, brightTexture, 0);

        GLES30.glDeleteFramebuffers(2, blurFBO, 0);
        GLES30.glDeleteTextures(2, blurTexture, 0);

        // Eliminar VAO/VBO
        int[] vaoArr = {quadVAO};
        int[] vboArr = {quadVBO};
        GLES30.glDeleteVertexArrays(1, vaoArr, 0);
        GLES30.glDeleteBuffers(1, vboArr, 0);

        // Eliminar programas
        if (brightPassProgram != 0) GLES30.glDeleteProgram(brightPassProgram);
        if (blurProgram != 0) GLES30.glDeleteProgram(blurProgram);
        if (compositeProgram != 0) GLES30.glDeleteProgram(compositeProgram);

        initialized = false;
    }
}
