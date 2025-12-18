package com.secret.blackholeglow.christmas;

import android.opengl.GLES30;
import android.util.Log;

import com.secret.blackholeglow.SceneObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║   🟢 MiniStopButton - Esfera Navideña VERDE (Botón Stop)                  ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * Copia exacta del ChristmasOrnamentButton pero:
 * - Color VERDE en lugar de rojo
 * - Icono de STOP (cuadrado) en lugar de PLAY
 * - Posición en esquina superior derecha
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
    private float posX = 0.25f;   // Más centrado horizontalmente
    private float posY = 0.6f;    // Un poco más abajo
    private float size = 0.08f;   // Pequeño
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

    // Fragment shader - Esfera VERDE navideña con icono STOP (OPTIMIZADO)
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
        "    float radius = u_Size * (1.0 - u_Pressed * 0.08);\n" +
        "    \n" +
        "    // ⚡ EARLY DISCARD: Descartar píxeles muy lejos del botón\n" +
        "    float quickDist = abs(uv.x - u_Position.x) + abs(uv.y - u_Position.y);\n" +
        "    if (quickDist > radius * 3.0) {\n" +
        "        discard;\n" +
        "    }\n" +
        "    \n" +
        "    // Movimiento mágico: flotar suavemente\n" +
        "    vec2 magicPos = u_Position;\n" +
        "    magicPos.y += sin(u_Time * 1.5) * 0.008;\n" +
        "    magicPos.x += sin(u_Time * 1.2) * 0.003;\n" +
        "    \n" +
        "    // Efecto shake cuando se toca\n" +
        "    magicPos.x += sin(u_Time * 25.0) * u_Shake * 0.02;\n" +
        "    magicPos.y += cos(u_Time * 30.0) * u_Shake * 0.015;\n" +
        "    \n" +
        "    vec2 pos = uv - magicPos;\n" +
        "    float dist = length(pos);\n" +
        "    \n" +
        "    // ⚡ EARLY DISCARD 2: Descartar si muy lejos del área visible\n" +
        "    if (dist > radius * 2.0) {\n" +
        "        discard;\n" +
        "    }\n" +
        "    \n" +
        "    // PARTÍCULAS: Solo 2 para rendimiento (en lugar de 4)\n" +
        "    float sparkleZone = radius * 1.8;\n" +
        "    if (dist > radius && dist < sparkleZone) {\n" +
        "        for (float i = 0.0; i < 2.0; i++) {\n" +
        "            float angle = u_Time * 0.8 + i * 3.14;\n" +
        "            vec2 sparklePos = magicPos + vec2(cos(angle), sin(angle)) * radius * 1.3;\n" +
        "            float sparkDist = length(uv - sparklePos);\n" +
        "            if (sparkDist < 0.01) {\n" +
        "                float sparkle = 1.0 - sparkDist / 0.01;\n" +
        "                float twinkle = 0.5 + 0.5 * sin(u_Time * 6.0 + i * 2.0);\n" +
        "                gl_FragColor = vec4(vec3(0.7, 1.0, 0.8) * sparkle * twinkle, sparkle * 0.7);\n" +
        "                return;\n" +
        "            }\n" +
        "        }\n" +
        "    }\n" +
        "    \n" +
        "    // ESFERA VERDE\n" +
        "    if (dist < radius) {\n" +
        "        vec3 baseColor = vec3(0.1, 0.65, 0.25);\n" +
        "        float sphere = sqrt(1.0 - dist / radius);\n" +
        "        vec3 color = baseColor * (0.5 + 0.5 * sphere);\n" +
        "        \n" +
        "        // Reflejo\n" +
        "        vec2 highlightPos = pos + vec2(0.02, 0.03);\n" +
        "        float highlight = max(0.0, 1.0 - length(highlightPos) / (radius * 0.4));\n" +
        "        color += vec3(0.9, 1.0, 0.95) * highlight * highlight * highlight * 0.8;\n" +
        "        \n" +
        "        // Pulso\n" +
        "        color += baseColor * (0.06 + 0.06 * sin(u_Time * 2.0));\n" +
        "        \n" +
        "        // ICONO STOP\n" +
        "        float stopSize = radius * 0.35;\n" +
        "        if (abs(pos.x) < stopSize && abs(pos.y) < stopSize) {\n" +
        "            color = vec3(1.0);\n" +
        "        }\n" +
        "        \n" +
        "        gl_FragColor = vec4(color, 1.0);\n" +
        "        return;\n" +
        "    }\n" +
        "    \n" +
        "    // GANCHO DORADO (simplificado)\n" +
        "    vec2 hookBase = vec2(magicPos.x, magicPos.y + radius);\n" +
        "    vec2 hookPos = uv - hookBase;\n" +
        "    float capW = radius * 0.22;\n" +
        "    float capH = radius * 0.12;\n" +
        "    if (abs(hookPos.x) < capW && hookPos.y > 0.0 && hookPos.y < capH) {\n" +
        "        gl_FragColor = vec4(0.85, 0.65, 0.2, 1.0);\n" +
        "        return;\n" +
        "    }\n" +
        "    \n" +
        "    // Anillo\n" +
        "    vec2 ringC = hookBase + vec2(0.0, capH + radius * 0.06);\n" +
        "    float ringD = abs(length(uv - ringC) - radius * 0.08);\n" +
        "    if (ringD < radius * 0.02) {\n" +
        "        gl_FragColor = vec4(0.85, 0.65, 0.2, 1.0);\n" +
        "        return;\n" +
        "    }\n" +
        "    \n" +
        "    // GLOW\n" +
        "    float glowDist = dist - radius;\n" +
        "    if (glowDist > 0.0 && glowDist < radius * 0.35) {\n" +
        "        float glow = 1.0 - glowDist / (radius * 0.35);\n" +
        "        glow = glow * glow;\n" +
        "        gl_FragColor = vec4(vec3(0.3, 1.0, 0.5) * glow * 0.3, glow * 0.35);\n" +
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
