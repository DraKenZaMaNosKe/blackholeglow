package com.secret.blackholeglow.video;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔════════════════════════════════════════════════════════════════════════════╗
 * ║                    🫧 BubbleSystem - Burbujas Submarinas                   ║
 * ╠════════════════════════════════════════════════════════════════════════════╣
 * ║  Sistema de partículas para burbujas que salen de los peces.              ║
 * ║  • Suben con movimiento ondulante                                          ║
 * ║  • Se desvanecen gradualmente                                              ║
 * ║  • Efecto de brillo interior                                               ║
 * ╚════════════════════════════════════════════════════════════════════════════╝
 */
public class BubbleSystem {
    private static final String TAG = "BubbleSystem";

    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════════════════
    private static final int MAX_BUBBLES = 8;               // Reducido de 15 para mejor rendimiento
    private static final float BUBBLE_LIFETIME = 2.0f;      // Vida corta
    private static final float RISE_SPEED = 0.06f;          // Subida más lenta
    private static final float WOBBLE_SPEED = 2.5f;         // Oscilación suave
    private static final float WOBBLE_AMOUNT = 0.005f;      // Muy sutil
    private static final float MIN_SIZE = 0.0015f;          // MUY diminutas
    private static final float MAX_SIZE = 0.004f;           // Pequeñitas

    // ═══════════════════════════════════════════════════════════════════════════
    // DATOS DE BURBUJAS
    // ═══════════════════════════════════════════════════════════════════════════
    private float[] bubbleX = new float[MAX_BUBBLES];
    private float[] bubbleY = new float[MAX_BUBBLES];
    private float[] bubbleZ = new float[MAX_BUBBLES];       // Profundidad (para escala)
    private float[] bubbleSize = new float[MAX_BUBBLES];
    private float[] bubbleLife = new float[MAX_BUBBLES];
    private float[] bubblePhase = new float[MAX_BUBBLES];   // Para wobble único
    private float[] bubbleSpeedMult = new float[MAX_BUBBLES]; // Variación de velocidad
    private int bubbleCount = 0;

    private float time = 0f;
    private java.util.Random random = new java.util.Random();

    // ═══════════════════════════════════════════════════════════════════════════
    // OPENGL
    // ═══════════════════════════════════════════════════════════════════════════
    private int shaderProgram;
    private int aPosLoc, uColorLoc, uMVPLoc;
    private FloatBuffer vertexBuffer;
    private boolean initialized = false;

    private final float[] projMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    private final float[] modelMatrix = new float[16];

    // Círculo con 12 segmentos
    private static final int CIRCLE_SEGMENTS = 12;
    private float[] circleVertices;

    // ═══════════════════════════════════════════════════════════════════════════
    // SHADERS
    // ═══════════════════════════════════════════════════════════════════════════
    private static final String VERTEX_SHADER =
        "attribute vec2 a_Position;\n" +
        "uniform mat4 u_MVP;\n" +
        "void main() {\n" +
        "    gl_Position = u_MVP * vec4(a_Position, 0.0, 1.0);\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "uniform vec4 u_Color;\n" +
        "void main() {\n" +
        "    gl_FragColor = u_Color;\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════
    public BubbleSystem() {
        // Generar vértices del círculo
        circleVertices = new float[(CIRCLE_SEGMENTS + 2) * 2];

        // Centro del círculo
        circleVertices[0] = 0f;
        circleVertices[1] = 0f;

        // Puntos del perímetro
        for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
            float angle = (float)(i * 2.0 * Math.PI / CIRCLE_SEGMENTS);
            circleVertices[(i + 1) * 2] = (float)Math.cos(angle);
            circleVertices[(i + 1) * 2 + 1] = (float)Math.sin(angle);
        }

        Log.d(TAG, "🫧 BubbleSystem creado");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INICIALIZACIÓN OPENGL
    // ═══════════════════════════════════════════════════════════════════════════
    public void initialize() {
        if (initialized) return;

        // Compilar shaders
        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        if (vertexShader == 0 || fragmentShader == 0) {
            Log.e(TAG, "❌ Error compilando shaders");
            return;
        }

        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vertexShader);
        GLES20.glAttachShader(shaderProgram, fragmentShader);
        GLES20.glLinkProgram(shaderProgram);

        int[] linked = new int[1];
        GLES20.glGetProgramiv(shaderProgram, GLES20.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            Log.e(TAG, "❌ Error linkeando: " + GLES20.glGetProgramInfoLog(shaderProgram));
            return;
        }

        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);

        aPosLoc = GLES20.glGetAttribLocation(shaderProgram, "a_Position");
        uColorLoc = GLES20.glGetUniformLocation(shaderProgram, "u_Color");
        uMVPLoc = GLES20.glGetUniformLocation(shaderProgram, "u_MVP");

        // Crear buffer de vértices
        ByteBuffer bb = ByteBuffer.allocateDirect(circleVertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(circleVertices);
        vertexBuffer.position(0);

        initialized = true;
        Log.d(TAG, "✅ BubbleSystem inicializado");
    }

    private int compileShader(int type, String code) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
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

    // ═══════════════════════════════════════════════════════════════════════════
    // SPAWN - Crear nuevas burbujas
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Genera una burbuja en la posición especificada (sin Z, usa 0)
     */
    public void spawn(float x, float y) {
        spawn(x, y, 0f);
    }

    /**
     * Genera una burbuja en la posición especificada con profundidad Z
     * @param x Posición X (mundo)
     * @param y Posición Y (mundo)
     * @param z Profundidad (para escala dinámica)
     */
    public void spawn(float x, float y, float z) {
        if (bubbleCount >= MAX_BUBBLES) {
            // Reemplazar la burbuja más vieja
            int oldest = 0;
            float minLife = bubbleLife[0];
            for (int i = 1; i < bubbleCount; i++) {
                if (bubbleLife[i] < minLife) {
                    minLife = bubbleLife[i];
                    oldest = i;
                }
            }
            setBubble(oldest, x, y, z);
        } else {
            setBubble(bubbleCount, x, y, z);
            bubbleCount++;
        }
    }

    /**
     * Genera múltiples burbujas con dispersión
     */
    public void spawnBurst(float x, float y, float z, int count) {
        for (int i = 0; i < count; i++) {
            float offsetX = (random.nextFloat() - 0.5f) * 0.02f;
            float offsetY = (random.nextFloat() - 0.5f) * 0.01f;
            spawn(x + offsetX, y + offsetY, z);
        }
    }

    private void setBubble(int index, float x, float y, float z) {
        bubbleX[index] = x + (random.nextFloat() - 0.5f) * 0.008f;  // Dispersión mínima
        bubbleY[index] = y;
        bubbleZ[index] = z;  // Guardar profundidad
        bubbleSize[index] = MIN_SIZE + random.nextFloat() * (MAX_SIZE - MIN_SIZE);
        bubbleLife[index] = BUBBLE_LIFETIME;
        bubblePhase[index] = random.nextFloat() * 6.28f;
        bubbleSpeedMult[index] = 0.8f + random.nextFloat() * 0.4f;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════════════════════════════════
    public void update(float deltaTime) {
        time += deltaTime;

        int alive = 0;
        for (int i = 0; i < bubbleCount; i++) {
            bubbleLife[i] -= deltaTime;

            if (bubbleLife[i] > 0) {
                // Subir con wobble
                float wobble = (float)Math.sin(time * WOBBLE_SPEED + bubblePhase[i]) * WOBBLE_AMOUNT;
                bubbleX[i] += wobble * deltaTime * 2f;
                bubbleY[i] += RISE_SPEED * bubbleSpeedMult[i] * deltaTime;

                // Reducir tamaño gradualmente
                float lifeRatio = bubbleLife[i] / BUBBLE_LIFETIME;
                if (lifeRatio < 0.3f) {
                    bubbleSize[i] *= 0.995f;
                }

                // Compactar array
                if (alive != i) {
                    bubbleX[alive] = bubbleX[i];
                    bubbleY[alive] = bubbleY[i];
                    bubbleZ[alive] = bubbleZ[i];
                    bubbleSize[alive] = bubbleSize[i];
                    bubbleLife[alive] = bubbleLife[i];
                    bubblePhase[alive] = bubblePhase[i];
                    bubbleSpeedMult[alive] = bubbleSpeedMult[i];
                }
                alive++;
            }
        }
        bubbleCount = alive;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DRAW
    // ═══════════════════════════════════════════════════════════════════════════
    public void draw(float aspectRatio) {
        if (!initialized || bubbleCount == 0) return;

        GLES20.glUseProgram(shaderProgram);

        // Matriz de proyección ortográfica
        Matrix.orthoM(projMatrix, 0, -aspectRatio, aspectRatio, -1f, 1f, -1f, 1f);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(aPosLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        for (int i = 0; i < bubbleCount; i++) {
            float lifeRatio = bubbleLife[i] / BUBBLE_LIFETIME;
            float alpha = Math.min(1f, lifeRatio * 2f) * 0.35f;  // Más transparentes

            // Escalar burbuja según profundidad Z (0.5 a 1.5 multiplicador)
            // Z negativo = más cerca = más grande, Z positivo = más lejos = más pequeño
            float depthScale = 1.0f - bubbleZ[i] * 0.5f;
            depthScale = Math.max(0.3f, Math.min(1.5f, depthScale));
            float finalSize = bubbleSize[i] * depthScale;

            // Matriz de modelo (posición y escala con profundidad)
            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.translateM(modelMatrix, 0, bubbleX[i], bubbleY[i], 0);
            Matrix.scaleM(modelMatrix, 0, finalSize, finalSize, 1f);

            // MVP
            Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, modelMatrix, 0);
            GLES20.glUniformMatrix4fv(uMVPLoc, 1, false, mvpMatrix, 0);

            // Color cyan translúcido con brillo
            float brightness = 0.8f + 0.2f * (float)Math.sin(time * 4f + bubblePhase[i]);
            GLES20.glUniform4f(uColorLoc,
                0.4f * brightness,   // R
                0.9f * brightness,   // G
                1.0f * brightness,   // B
                alpha);

            // Dibujar círculo relleno
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, CIRCLE_SEGMENTS + 2);

            // Dibujar borde más brillante
            GLES20.glUniform4f(uColorLoc, 0.7f, 1.0f, 1.0f, alpha * 0.8f);
            GLES20.glLineWidth(1.5f);
            GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 1, CIRCLE_SEGMENTS);
        }

        GLES20.glDisableVertexAttribArray(aPosLoc);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════════════════
    public int getBubbleCount() {
        return bubbleCount;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
