package com.secret.blackholeglow.video;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔════════════════════════════════════════════════════════════════╗
 * ║              AlienFishSprite - Pez 2D Animado                  ║
 * ╠════════════════════════════════════════════════════════════════╣
 * ║  Sprite de pez alienígena que nada por la escena.              ║
 * ║  Se renderiza sobre el video de fondo.                         ║
 * ║  Movimiento ondulante + desplazamiento horizontal.             ║
 * ╚════════════════════════════════════════════════════════════════╝
 */
public class AlienFishSprite {
    private static final String TAG = "AlienFishSprite";

    // ═══════════════════════════════════════════════════════════════
    // ESTADO DEL PEZ
    // ═══════════════════════════════════════════════════════════════
    private float posX = -1.5f;      // Posición X (empieza fuera de pantalla)
    private float posY = 0.0f;       // Posición Y (afectado por depth)
    private float speedX = 0.3f;     // Velocidad horizontal
    private float baseSize = 0.15f;  // Tamaño base del pez
    private float size = 0.15f;      // Tamaño actual (cambia con depth)
    private float time = 0f;         // Tiempo para animación
    private boolean movingRight = true;

    // ═══════════════════════════════════════════════════════════════
    // PROFUNDIDAD (Z simulado)
    // ═══════════════════════════════════════════════════════════════
    private float depth = 0.0f;      // 0 = cerca (grande, abajo), 1 = lejos (chico, arriba)
    private float targetDepth = 0.0f;
    private float depthSpeed = 0.3f;
    private float depthTimer = 0f;
    private float nextDepthChange = 3f;  // Segundos hasta próximo cambio de dirección Z

    // Límites según profundidad
    private static final float SIZE_CLOSE = 0.18f;   // Grande cuando cerca
    private static final float SIZE_FAR = 0.06f;     // Pequeño cuando lejos
    private static final float Y_CLOSE = -0.4f;      // Abajo cuando cerca
    private static final float Y_FAR = 0.2f;         // Arriba cuando lejos

    // ═══════════════════════════════════════════════════════════════
    // OPENGL
    // ═══════════════════════════════════════════════════════════════
    private int shaderProgram;
    private int aPositionLoc;
    private int uColorLoc;
    private int uTimeLoc;
    private int uPosLoc;
    private int uSizeLoc;
    private int uFlipLoc;
    private FloatBuffer vertexBuffer;
    private boolean initialized = false;

    // Quad para el pez
    private static final float[] QUAD = {
        -1f, -1f,
         1f, -1f,
        -1f,  1f,
         1f,  1f
    };

    // ═══════════════════════════════════════════════════════════════
    // VERTEX SHADER
    // ═══════════════════════════════════════════════════════════════
    private static final String VERTEX_SHADER =
        "attribute vec2 aPosition;\n" +
        "uniform vec2 uPos;\n" +
        "uniform float uSize;\n" +
        "uniform float uFlip;\n" +
        "varying vec2 vUV;\n" +
        "void main() {\n" +
        "    vec2 pos = aPosition * uSize + uPos;\n" +
        "    gl_Position = vec4(pos, 0.0, 1.0);\n" +
        "    vUV = aPosition * 0.5 + 0.5;\n" +
        "    vUV.x = mix(vUV.x, 1.0 - vUV.x, uFlip);\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════
    // FRAGMENT SHADER - Dibuja pez alienígena procedural
    // ═══════════════════════════════════════════════════════════════
    private static final String FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "uniform vec3 uColor;\n" +
        "uniform float uTime;\n" +
        "varying vec2 vUV;\n" +
        "\n" +
        "void main() {\n" +
        "    vec2 uv = vUV;\n" +
        "    \n" +
        "    // Ondulación del cuerpo\n" +
        "    float wave = sin(uv.x * 6.28 + uTime * 8.0) * 0.05;\n" +
        "    uv.y += wave * (1.0 - uv.x);\n" +  // Más ondulación en la cola
        "    \n" +
        "    // Forma del pez (elipse + cola)\n" +
        "    vec2 bodyCenter = vec2(0.6, 0.5);\n" +
        "    vec2 bodyDiff = (uv - bodyCenter) * vec2(1.0, 1.8);\n" +
        "    float body = 1.0 - smoothstep(0.0, 0.35, length(bodyDiff));\n" +
        "    \n" +
        "    // Cola triangular\n" +
        "    float tail = 0.0;\n" +
        "    if (uv.x < 0.4) {\n" +
        "        float tailWidth = (0.4 - uv.x) * 0.8;\n" +
        "        tail = 1.0 - smoothstep(0.0, tailWidth, abs(uv.y - 0.5));\n" +
        "        tail *= smoothstep(0.0, 0.1, uv.x);\n" +
        "    }\n" +
        "    \n" +
        "    float shape = max(body, tail * 0.8);\n" +
        "    \n" +
        "    // Ojo\n" +
        "    vec2 eyePos = vec2(0.75, 0.55);\n" +
        "    float eye = 1.0 - smoothstep(0.0, 0.06, length(uv - eyePos));\n" +
        "    \n" +
        "    // Color con gradiente bioluminiscente\n" +
        "    vec3 col = uColor;\n" +
        "    col += vec3(0.2, 0.5, 0.8) * (1.0 - uv.x) * 0.5;\n" +  // Brillo en cola
        "    col += vec3(0.0, 1.0, 1.0) * sin(uTime * 3.0 + uv.x * 10.0) * 0.15;\n" +  // Pulso
        "    \n" +
        "    // Alpha\n" +
        "    float alpha = shape * 0.9;\n" +
        "    \n" +
        "    // Ojo brillante\n" +
        "    col = mix(col, vec3(1.0, 1.0, 0.5), eye);\n" +
        "    alpha = max(alpha, eye);\n" +
        "    \n" +
        "    gl_FragColor = vec4(col, alpha);\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════
    // INICIALIZACIÓN
    // ═══════════════════════════════════════════════════════════════
    public void initialize() {
        // Crear buffer
        ByteBuffer bb = ByteBuffer.allocateDirect(QUAD.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(QUAD);
        vertexBuffer.position(0);

        // Compilar shaders
        int vs = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vs);
        GLES20.glAttachShader(shaderProgram, fs);
        GLES20.glLinkProgram(shaderProgram);

        // Obtener locations
        aPositionLoc = GLES20.glGetAttribLocation(shaderProgram, "aPosition");
        uColorLoc = GLES20.glGetUniformLocation(shaderProgram, "uColor");
        uTimeLoc = GLES20.glGetUniformLocation(shaderProgram, "uTime");
        uPosLoc = GLES20.glGetUniformLocation(shaderProgram, "uPos");
        uSizeLoc = GLES20.glGetUniformLocation(shaderProgram, "uSize");
        uFlipLoc = GLES20.glGetUniformLocation(shaderProgram, "uFlip");

        initialized = true;
        Log.d(TAG, "🐟 AlienFishSprite inicializado");
    }

    // ═══════════════════════════════════════════════════════════════
    // UPDATE - Movimiento del pez con profundidad (Z simulado)
    // ═══════════════════════════════════════════════════════════════
    public void update(float deltaTime) {
        time += deltaTime;
        depthTimer += deltaTime;

        // ═══════════════════════════════════════════════════════════
        // MOVIMIENTO EN PROFUNDIDAD (Z)
        // ═══════════════════════════════════════════════════════════

        // Decidir si cambiar dirección de profundidad
        if (depthTimer > nextDepthChange) {
            depthTimer = 0f;
            nextDepthChange = 2f + (float)(Math.random() * 4f); // 2-6 segundos

            // Elegir nueva profundidad objetivo
            if (depth < 0.3f) {
                // Está cerca, puede ir lejos
                targetDepth = (float)(Math.random() * 0.7f + 0.3f); // 0.3 - 1.0
            } else if (depth > 0.7f) {
                // Está lejos, volver cerca
                targetDepth = (float)(Math.random() * 0.4f); // 0.0 - 0.4
            } else {
                // En medio, ir a cualquier lado
                targetDepth = (float)(Math.random());
            }
        }

        // Interpolar hacia profundidad objetivo (suave)
        float depthDiff = targetDepth - depth;
        depth += depthDiff * depthSpeed * deltaTime;
        depth = Math.max(0f, Math.min(1f, depth)); // Clamp 0-1

        // Calcular tamaño según profundidad (grande cerca, chico lejos)
        size = SIZE_CLOSE + (SIZE_FAR - SIZE_CLOSE) * depth;

        // Calcular Y base según profundidad (abajo cerca, arriba lejos)
        float baseY = Y_CLOSE + (Y_FAR - Y_CLOSE) * depth;

        // ═══════════════════════════════════════════════════════════
        // MOVIMIENTO HORIZONTAL (X)
        // ═══════════════════════════════════════════════════════════

        // Velocidad afectada por depth (más lento cuando lejos)
        float currentSpeed = speedX * (1.0f - depth * 0.4f);

        if (movingRight) {
            posX += currentSpeed * deltaTime;
            if (posX > 1.3f) {
                movingRight = false;
            }
        } else {
            posX -= currentSpeed * deltaTime;
            if (posX < -1.3f) {
                movingRight = true;
            }
        }

        // ═══════════════════════════════════════════════════════════
        // POSICIÓN Y FINAL
        // ═══════════════════════════════════════════════════════════

        // Ondulación sutil + Y base según profundidad
        float waveY = (float)Math.sin(time * 1.5) * 0.02f * (1f - depth * 0.5f);
        posY = baseY + waveY;
    }

    // ═══════════════════════════════════════════════════════════════
    // DRAW
    // ═══════════════════════════════════════════════════════════════
    public void draw() {
        if (!initialized) return;

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glUseProgram(shaderProgram);

        // Color cyan/púrpura alienígena
        GLES20.glUniform3f(uColorLoc, 0.4f, 0.2f, 0.8f);
        GLES20.glUniform1f(uTimeLoc, time);
        GLES20.glUniform2f(uPosLoc, posX, posY);
        GLES20.glUniform1f(uSizeLoc, size);
        GLES20.glUniform1f(uFlipLoc, movingRight ? 0.0f : 1.0f);

        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(aPositionLoc);
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════════
    private int compileShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Shader error: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    public void release() {
        if (shaderProgram != 0) {
            GLES20.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        initialized = false;
    }
}
