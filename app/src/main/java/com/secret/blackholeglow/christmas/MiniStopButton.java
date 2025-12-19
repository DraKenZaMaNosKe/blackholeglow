package com.secret.blackholeglow.christmas;

import android.opengl.GLES30;
import android.util.Log;

import com.secret.blackholeglow.SceneObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║   🔴 MiniStopButton - Botón Stop Genérico (ROJO)                          ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * Botón minimalista para detener el wallpaper:
 * - Círculo rojo con icono de STOP (cuadrado blanco)
 * - Glow sutil rojo
 * - Funciona para TODAS las escenas (genérico)
 */
public class MiniStopButton implements SceneObject {
    private static final String TAG = "MiniStopButton";

    // OpenGL
    private int shaderProgram;
    private FloatBuffer vertexBuffer;

    // Uniforms
    private int uTimeLoc;
    private int uAspectLoc;
    private int uPositionLoc;
    private int uSizeLoc;
    private int uPressedLoc;
    private int uShakeLoc;
    private int uResolutionLoc;
    private int aPositionLoc;

    // Screen resolution
    private float screenWidth = 1080f;
    private float screenHeight = 1920f;

    // Estado
    private boolean initialized = false;
    private boolean visible = true;
    private boolean isPressed = false;
    private float posX = -0.35f;   // Izquierda
    private float posY = 0.55f;   // Debajo de la barra de vida
    private float size = 0.04f;   // Pequeño
    private float aspectRatio = 1.0f;
    private float time = 0.0f;

    // Efecto shake
    private float shakeIntensity = 0.0f;
    private static final float SHAKE_DECAY = 3.0f;

    // Vertex shader
    private static final String VERTEX_SHADER =
        "attribute vec2 a_Position;\n" +
        "void main() {\n" +
        "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
        "}\n";

    // Fragment shader - Botón STOP genérico (rojo, minimalista)
    private static final String FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "\n" +
        "uniform float u_Time;\n" +
        "uniform float u_Aspect;\n" +
        "uniform vec2 u_Position;\n" +
        "uniform float u_Size;\n" +
        "uniform float u_Pressed;\n" +
        "uniform float u_Shake;\n" +
        "uniform vec2 u_Resolution;\n" +
        "\n" +
        "void main() {\n" +
        "    vec2 uv = gl_FragCoord.xy / u_Resolution;\n" +
        "    uv = uv * 2.0 - 1.0;\n" +
        "    uv.x *= u_Aspect;\n" +
        "    \n" +
        "    float radius = u_Size * (1.0 - u_Pressed * 0.1);\n" +
        "    \n" +
        "    // Early discard\n" +
        "    float quickDist = abs(uv.x - u_Position.x) + abs(uv.y - u_Position.y);\n" +
        "    if (quickDist > radius * 2.5) { discard; }\n" +
        "    \n" +
        "    vec2 pos = uv - u_Position;\n" +
        "    \n" +
        "    // Shake effect\n" +
        "    pos.x += sin(u_Time * 25.0) * u_Shake * 0.02;\n" +
        "    pos.y += cos(u_Time * 30.0) * u_Shake * 0.015;\n" +
        "    \n" +
        "    float dist = length(pos);\n" +
        "    \n" +
        "    if (dist > radius * 1.5) { discard; }\n" +
        "    \n" +
        "    // Circulo rojo\n" +
        "    if (dist < radius) {\n" +
        "        vec3 baseColor = vec3(0.85, 0.15, 0.15);\n" +
        "        float sphere = sqrt(1.0 - dist / radius);\n" +
        "        vec3 color = baseColor * (0.6 + 0.4 * sphere);\n" +
        "        \n" +
        "        // Highlight\n" +
        "        vec2 hlPos = pos + vec2(0.015, 0.02);\n" +
        "        float hl = max(0.0, 1.0 - length(hlPos) / (radius * 0.35));\n" +
        "        color += vec3(1.0, 0.8, 0.8) * hl * hl * hl * 0.6;\n" +
        "        \n" +
        "        // Icono STOP (cuadrado blanco)\n" +
        "        float stopSize = radius * 0.32;\n" +
        "        if (abs(pos.x) < stopSize && abs(pos.y) < stopSize) {\n" +
        "            color = vec3(1.0);\n" +
        "        }\n" +
        "        \n" +
        "        gl_FragColor = vec4(color, 1.0);\n" +
        "        return;\n" +
        "    }\n" +
        "    \n" +
        "    // Glow sutil\n" +
        "    float glowDist = dist - radius;\n" +
        "    if (glowDist > 0.0 && glowDist < radius * 0.3) {\n" +
        "        float glow = 1.0 - glowDist / (radius * 0.3);\n" +
        "        glow = glow * glow;\n" +
        "        gl_FragColor = vec4(vec3(1.0, 0.3, 0.3) * glow * 0.4, glow * 0.4);\n" +
        "        return;\n" +
        "    }\n" +
        "    \n" +
        "    discard;\n" +
        "}\n";

    public MiniStopButton() {
        init();
    }

    private void init() {
        int vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        if (vertexShader == 0 || fragmentShader == 0) {
            Log.e(TAG, "Error compilando shaders");
            return;
        }

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vertexShader);
        GLES30.glAttachShader(shaderProgram, fragmentShader);
        GLES30.glLinkProgram(shaderProgram);

        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        uTimeLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Time");
        uAspectLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Aspect");
        uPositionLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Position");
        uSizeLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Size");
        uPressedLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Pressed");
        uShakeLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Shake");
        uResolutionLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Resolution");

        float[] vertices = {
            -1.0f, -1.0f,
             1.0f, -1.0f,
            -1.0f,  1.0f,
             1.0f,  1.0f
        };

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        GLES30.glDeleteShader(vertexShader);
        GLES30.glDeleteShader(fragmentShader);

        initialized = true;
        Log.d(TAG, "✓ MiniStopButton inicializado");
    }

    private int compileShader(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Error: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    @Override
    public void update(float deltaTime) {
        time += deltaTime;
        // ✅ FIX: Evitar overflow - ciclo cada ~10 minutos (628 segundos ≈ 2π * 100)
        if (time > 628.318f) {
            time -= 628.318f;
        }
        if (shakeIntensity > 0) {
            shakeIntensity -= deltaTime * SHAKE_DECAY;
            if (shakeIntensity < 0) shakeIntensity = 0;
        }
    }

    @Override
    public void draw() {
        if (!initialized || !visible || shaderProgram == 0) return;

        GLES30.glUseProgram(shaderProgram);

        GLES30.glUniform1f(uTimeLoc, time);
        GLES30.glUniform1f(uAspectLoc, aspectRatio);
        GLES30.glUniform2f(uPositionLoc, posX, posY);
        GLES30.glUniform1f(uSizeLoc, size);
        GLES30.glUniform1f(uPressedLoc, isPressed ? 1.0f : 0.0f);
        GLES30.glUniform1f(uShakeLoc, shakeIntensity);
        GLES30.glUniform2f(uResolutionLoc, screenWidth, screenHeight);

        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glDisableVertexAttribArray(aPositionLoc);
    }

    public boolean isInside(float touchX, float touchY) {
        float shaderTouchX = touchX * aspectRatio;
        float dx = shaderTouchX - posX;
        float dy = touchY - posY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        return dist < size * 1.5f;
    }

    public void onInteraction() {
        shakeIntensity = 1.0f;
    }

    public void show() {
        visible = true;
    }

    public void hide() {
        visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setAspectRatio(float ratio) {
        this.aspectRatio = ratio;
    }

    public void setPosition(float x, float y) {
        this.posX = x;
        this.posY = y;
    }

    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        this.aspectRatio = (float) width / height;
    }

    public void release() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        initialized = false;
    }
}
