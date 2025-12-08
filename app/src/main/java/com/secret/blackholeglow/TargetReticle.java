package com.secret.blackholeglow;

import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   🎯 TARGET RETICLE - Mira Visual de Targeting                           ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  ESTADOS VISUALES:                                                        ║
 * ║  • SEARCHING: Mira AMARILLA girando, buscando lock                       ║
 * ║  • LOCKED: Mira ROJA pulsante, lista para disparar                       ║
 * ║  • FIRING: Efecto de destello al disparar                                ║
 * ║                                                                           ║
 * ║  DISEÑO:                                                                  ║
 * ║       ╱ ╲           ┏━━━┓                                                ║
 * ║      ╱   ╲          ┃ ◉ ┃   ← Centro pulsante                           ║
 * ║  ───(  ·  )───  →  ╋═◉═╋   ← Cruz + anillos                            ║
 * ║      ╲   ╱          ┃ ◉ ┃                                                ║
 * ║       ╲ ╱           ┗━━━┛                                                ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class TargetReticle implements SceneObject, CameraAware {
    private static final String TAG = "TargetReticle";

    // ═══════════════════════════════════════════════════════════════════════
    // 🎨 CONFIGURACIÓN VISUAL
    // ═══════════════════════════════════════════════════════════════════════
    private static final float BASE_SIZE = 0.4f;
    private static final int RING_SEGMENTS = 32;
    private static final int CORNER_COUNT = 4;

    // Colores
    private static final float[] COLOR_SEARCHING = {1.0f, 0.9f, 0.2f};  // Amarillo
    private static final float[] COLOR_LOCKED = {1.0f, 0.2f, 0.2f};     // Rojo
    private static final float[] COLOR_FIRING = {1.0f, 1.0f, 1.0f};     // Blanco

    // ═══════════════════════════════════════════════════════════════════════
    // 📊 ESTADO
    // ═══════════════════════════════════════════════════════════════════════
    private TargetingSystem targetingSystem;
    private CameraController camera;
    private boolean visible = false;
    private float time = 0f;

    // Posición actual (animada suavemente)
    private float currentX = 0f;
    private float currentY = 0f;
    private float currentZ = 0f;

    // ═══════════════════════════════════════════════════════════════════════
    // 🎮 OPENGL
    // ═══════════════════════════════════════════════════════════════════════
    private int shaderProgram = 0;
    private int aPositionHandle;
    private int uMVPMatrixHandle;
    private int uColorHandle;
    private int uAlphaHandle;
    private int uTimeHandle;

    private FloatBuffer ringBuffer;
    private FloatBuffer crossBuffer;
    private FloatBuffer cornerBuffer;
    private FloatBuffer centerBuffer;

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // Cache de colores para evitar allocations
    private final float[] currentColor = new float[3];

    /**
     * Constructor
     */
    public TargetReticle() {
        initShaders();
        initGeometry();
        Log.d(TAG, "🎯 TargetReticle inicializado");
    }

    public void setTargetingSystem(TargetingSystem system) {
        this.targetingSystem = system;
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🔧 INICIALIZACIÓN
    // ═══════════════════════════════════════════════════════════════════════

    private void initShaders() {
        String vertexShaderCode =
            "attribute vec4 a_Position;\n" +
            "uniform mat4 u_MVPMatrix;\n" +
            "void main() {\n" +
            "    gl_Position = u_MVPMatrix * a_Position;\n" +
            "    gl_PointSize = 8.0;\n" +
            "}";

        String fragmentShaderCode =
            "precision mediump float;\n" +
            "uniform vec3 u_Color;\n" +
            "uniform float u_Alpha;\n" +
            "uniform float u_Time;\n" +
            "void main() {\n" +
            "    float glow = 0.8 + 0.2 * sin(u_Time * 5.0);\n" +
            "    gl_FragColor = vec4(u_Color * glow, u_Alpha);\n" +
            "}";

        int vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode);

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vertexShader);
        GLES30.glAttachShader(shaderProgram, fragmentShader);
        GLES30.glLinkProgram(shaderProgram);

        aPositionHandle = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        uMVPMatrixHandle = GLES30.glGetUniformLocation(shaderProgram, "u_MVPMatrix");
        uColorHandle = GLES30.glGetUniformLocation(shaderProgram, "u_Color");
        uAlphaHandle = GLES30.glGetUniformLocation(shaderProgram, "u_Alpha");
        uTimeHandle = GLES30.glGetUniformLocation(shaderProgram, "u_Time");
    }

    private int compileShader(int type, String shaderCode) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);
        return shader;
    }

    private void initGeometry() {
        // ═══════════════════════════════════════════════════════════════
        // ANILLO EXTERIOR
        // ═══════════════════════════════════════════════════════════════
        float[] ringVertices = new float[RING_SEGMENTS * 3];
        for (int i = 0; i < RING_SEGMENTS; i++) {
            float angle = (float) (i * 2.0 * Math.PI / RING_SEGMENTS);
            ringVertices[i * 3] = (float) Math.cos(angle) * BASE_SIZE;
            ringVertices[i * 3 + 1] = (float) Math.sin(angle) * BASE_SIZE;
            ringVertices[i * 3 + 2] = 0f;
        }
        ringBuffer = createBuffer(ringVertices);

        // ═══════════════════════════════════════════════════════════════
        // CRUZ CENTRAL
        // ═══════════════════════════════════════════════════════════════
        float crossSize = BASE_SIZE * 0.6f;
        float[] crossVertices = {
            // Línea horizontal
            -crossSize, 0f, 0f,
             crossSize, 0f, 0f,
            // Línea vertical
            0f, -crossSize, 0f,
            0f,  crossSize, 0f
        };
        crossBuffer = createBuffer(crossVertices);

        // ═══════════════════════════════════════════════════════════════
        // ESQUINAS (brackets)
        // ═══════════════════════════════════════════════════════════════
        float cornerSize = BASE_SIZE * 0.3f;
        float cornerOffset = BASE_SIZE * 0.8f;
        float[] cornerVertices = new float[CORNER_COUNT * 4 * 3];  // 4 esquinas, 4 vértices cada una
        int idx = 0;

        // Esquina superior derecha
        cornerVertices[idx++] = cornerOffset; cornerVertices[idx++] = cornerOffset - cornerSize; cornerVertices[idx++] = 0f;
        cornerVertices[idx++] = cornerOffset; cornerVertices[idx++] = cornerOffset; cornerVertices[idx++] = 0f;
        cornerVertices[idx++] = cornerOffset; cornerVertices[idx++] = cornerOffset; cornerVertices[idx++] = 0f;
        cornerVertices[idx++] = cornerOffset - cornerSize; cornerVertices[idx++] = cornerOffset; cornerVertices[idx++] = 0f;

        // Esquina superior izquierda
        cornerVertices[idx++] = -cornerOffset; cornerVertices[idx++] = cornerOffset - cornerSize; cornerVertices[idx++] = 0f;
        cornerVertices[idx++] = -cornerOffset; cornerVertices[idx++] = cornerOffset; cornerVertices[idx++] = 0f;
        cornerVertices[idx++] = -cornerOffset; cornerVertices[idx++] = cornerOffset; cornerVertices[idx++] = 0f;
        cornerVertices[idx++] = -cornerOffset + cornerSize; cornerVertices[idx++] = cornerOffset; cornerVertices[idx++] = 0f;

        // Esquina inferior derecha
        cornerVertices[idx++] = cornerOffset; cornerVertices[idx++] = -cornerOffset + cornerSize; cornerVertices[idx++] = 0f;
        cornerVertices[idx++] = cornerOffset; cornerVertices[idx++] = -cornerOffset; cornerVertices[idx++] = 0f;
        cornerVertices[idx++] = cornerOffset; cornerVertices[idx++] = -cornerOffset; cornerVertices[idx++] = 0f;
        cornerVertices[idx++] = cornerOffset - cornerSize; cornerVertices[idx++] = -cornerOffset; cornerVertices[idx++] = 0f;

        // Esquina inferior izquierda
        cornerVertices[idx++] = -cornerOffset; cornerVertices[idx++] = -cornerOffset + cornerSize; cornerVertices[idx++] = 0f;
        cornerVertices[idx++] = -cornerOffset; cornerVertices[idx++] = -cornerOffset; cornerVertices[idx++] = 0f;
        cornerVertices[idx++] = -cornerOffset; cornerVertices[idx++] = -cornerOffset; cornerVertices[idx++] = 0f;
        cornerVertices[idx++] = -cornerOffset + cornerSize; cornerVertices[idx++] = -cornerOffset; cornerVertices[idx++] = 0f;

        cornerBuffer = createBuffer(cornerVertices);

        // ═══════════════════════════════════════════════════════════════
        // PUNTO CENTRAL
        // ═══════════════════════════════════════════════════════════════
        float[] centerVertices = {0f, 0f, 0f};
        centerBuffer = createBuffer(centerVertices);
    }

    private FloatBuffer createBuffer(float[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer buffer = bb.asFloatBuffer();
        buffer.put(data);
        buffer.position(0);
        return buffer;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🔄 UPDATE
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void update(float deltaTime) {
        time += deltaTime;

        if (targetingSystem == null) {
            visible = false;
            return;
        }

        visible = targetingSystem.hasTarget() || targetingSystem.isFiring();

        if (visible) {
            // Interpolar posición suavemente hacia el objetivo
            float targetX = targetingSystem.getTargetWorldX();
            float targetY = targetingSystem.getTargetWorldY();
            float targetZ = targetingSystem.getTargetWorldZ();

            float lerpSpeed = 10f * deltaTime;
            currentX += (targetX - currentX) * lerpSpeed;
            currentY += (targetY - currentY) * lerpSpeed;
            currentZ += (targetZ - currentZ) * lerpSpeed;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🎨 DRAW
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void draw() {
        if (!visible || shaderProgram == 0 || camera == null) return;
        if (targetingSystem == null) return;

        GLES30.glUseProgram(shaderProgram);

        // Habilitar blending
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);

        // Configurar color según estado
        TargetingSystem.TargetState state = targetingSystem.getState();
        float alpha = 0.9f;

        if (state == TargetingSystem.TargetState.SEARCHING) {
            float progress = targetingSystem.getLockProgress();
            // Interpolar de amarillo a rojo según progreso
            currentColor[0] = COLOR_SEARCHING[0] + (COLOR_LOCKED[0] - COLOR_SEARCHING[0]) * progress;
            currentColor[1] = COLOR_SEARCHING[1] + (COLOR_LOCKED[1] - COLOR_SEARCHING[1]) * progress;
            currentColor[2] = COLOR_SEARCHING[2] + (COLOR_LOCKED[2] - COLOR_SEARCHING[2]) * progress;
        } else if (state == TargetingSystem.TargetState.LOCKED) {
            // Rojo pulsante
            float pulse = 0.8f + 0.2f * (float) Math.sin(time * 8.0);
            currentColor[0] = COLOR_LOCKED[0] * pulse;
            currentColor[1] = COLOR_LOCKED[1] * pulse;
            currentColor[2] = COLOR_LOCKED[2] * pulse;
        } else if (state == TargetingSystem.TargetState.FIRING) {
            // Destello blanco
            currentColor[0] = COLOR_FIRING[0];
            currentColor[1] = COLOR_FIRING[1];
            currentColor[2] = COLOR_FIRING[2];
            alpha = 1.0f;
        } else {
            currentColor[0] = COLOR_SEARCHING[0];
            currentColor[1] = COLOR_SEARCHING[1];
            currentColor[2] = COLOR_SEARCHING[2];
        }

        GLES30.glUniform3fv(uColorHandle, 1, currentColor, 0);
        GLES30.glUniform1f(uAlphaHandle, alpha);
        GLES30.glUniform1f(uTimeHandle, time);

        // Construir matriz de modelo
        android.opengl.Matrix.setIdentityM(modelMatrix, 0);
        android.opengl.Matrix.translateM(modelMatrix, 0, currentX, currentY, currentZ);

        // Hacer que la mira siempre mire a la cámara (billboard)
        // Rotar para enfrentar la cámara
        float scale = 0.5f + (state == TargetingSystem.TargetState.LOCKED ? 0.1f * (float)Math.sin(time * 6.0) : 0f);
        android.opengl.Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

        // Rotación para animación
        if (state == TargetingSystem.TargetState.SEARCHING) {
            // Girar mientras busca
            android.opengl.Matrix.rotateM(modelMatrix, 0, time * 90f, 0f, 0f, 1f);
        } else if (state == TargetingSystem.TargetState.LOCKED) {
            // Pequeña oscilación cuando está lockeado
            android.opengl.Matrix.rotateM(modelMatrix, 0, (float) Math.sin(time * 4.0) * 5f, 0f, 0f, 1f);
        }

        // Calcular MVP
        camera.computeMvp(modelMatrix, mvpMatrix);
        GLES30.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);

        // Dibujar elementos
        GLES30.glLineWidth(3.0f);

        // Anillo exterior
        GLES30.glEnableVertexAttribArray(aPositionHandle);
        GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, ringBuffer);
        GLES30.glDrawArrays(GLES30.GL_LINE_LOOP, 0, RING_SEGMENTS);

        // Cruz central
        GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, crossBuffer);
        GLES30.glDrawArrays(GLES30.GL_LINES, 0, 4);

        // Esquinas
        GLES30.glLineWidth(4.0f);
        GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, cornerBuffer);
        GLES30.glDrawArrays(GLES30.GL_LINES, 0, 16);

        // Punto central (más brillante cuando está lockeado)
        if (state == TargetingSystem.TargetState.LOCKED || state == TargetingSystem.TargetState.FIRING) {
            GLES30.glUniform1f(uAlphaHandle, 1.0f);
            GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, centerBuffer);
            GLES30.glDrawArrays(GLES30.GL_POINTS, 0, 1);
        }

        GLES30.glDisableVertexAttribArray(aPositionHandle);

        // Restaurar estados
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glDisable(GLES30.GL_BLEND);
        GLES30.glLineWidth(1.0f);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🧹 CLEANUP
    // ═══════════════════════════════════════════════════════════════════════

    public void release() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
    }
}
