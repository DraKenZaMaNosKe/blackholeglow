package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.util.ObjLoader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   🚗 DeLorean3D - DeLorean viajando por la carretera synthwave           ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  CARACTERÍSTICAS:                                                         ║
 * ║  • Posición fija en la parte inferior de la pantalla                     ║
 * ║  • Vista desde atrás (vemos las luces traseras)                          ║
 * ║  • Sutil animación de balanceo (como si fuera en la carretera)           ║
 * ║  • Efecto de underglow neón (cyan/magenta)                               ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class DeLorean3D implements SceneObject, CameraAware {
    private static final String TAG = "DeLorean3D";

    private final Context context;
    private final TextureLoader textureLoader;

    // Buffers del modelo
    private FloatBuffer vertexBuffer;
    private FloatBuffer uvBuffer;
    private IntBuffer indexBuffer;
    private int indexCount;

    // Textura
    private int textureId;

    // Shader
    private int shaderProgram;
    private int aPositionHandle;
    private int aTexCoordHandle;
    private int uMVPMatrixHandle;
    private int uModelMatrixHandle;
    private int uTextureHandle;
    private int uTimeHandle;

    // Transformación - POSICIÓN CALIBRADA (usuario 2026-01-06 21:25)
    private float x = -0.52f;      // Posición horizontal
    private float y = -4.98f;      // Sobre la carretera (abajo)
    private float z = 0.21f;       // Profundidad
    private float scale = 0.77f;   // Tamaño

    // Rotación base calibrada
    private float rotationX = 0.8f;    // Inclinación
    private float rotationY = -55.3f;  // Orientación horizontal
    private float rotationZ = 36.0f;   // Rotación volante

    // ═══════════════════════════════════════════════════════════════════════
    // 🚗 SISTEMA DE MICRO-MOVIMIENTOS SUTILES
    // ═══════════════════════════════════════════════════════════════════════

    private boolean autonomousDriving = true;  // Activar movimiento sutil

    // Posición base (el carro se queda aquí, solo micro-movimientos)
    private static final float BASE_X = -0.52f;
    private static final float BASE_Y = -4.98f;
    private static final float BASE_ROT_Z = 36.0f;

    // Micro-movimientos MUY sutiles
    private float wobblePhase = 0f;
    private static final float WOBBLE_SPEED = 1.5f;       // Velocidad del movimiento
    private static final float WOBBLE_X = 0.015f;         // Movimiento lateral MUY pequeño
    private static final float WOBBLE_Y = 0.008f;         // Movimiento vertical MUY pequeño
    private static final float WOBBLE_ROT = 0.8f;         // Rotación MUY sutil (grados)

    // ═══════════════════════════════════════════════════════════════════════
    // 🎮 SISTEMA DE CALIBRACIÓN POR TOUCH
    // ═══════════════════════════════════════════════════════════════════════

    public enum AdjustMode {
        OFF,           // Sin ajuste
        POSITION_XY,   // Mover en X/Y
        POSITION_Z,    // Mover en Z (profundidad)
        ROTATE_Y,      // Girar horizontalmente
        ROTATE_X,      // Inclinar adelante/atrás
        ROTATE_Z,      // Rotar como volante
        SCALE          // Cambiar tamaño
    }

    private AdjustMode currentMode = AdjustMode.POSITION_XY;  // Modo inicial
    private boolean adjustEnabled = false;  // 🔧 DESACTIVADO - conducción autónoma activa

    // Sensibilidad de ajustes
    private static final float SENSITIVITY_POSITION = 5.0f;
    private static final float SENSITIVITY_ROTATION = 180.0f;
    private static final float SENSITIVITY_SCALE = 2.0f;
    private static final float SENSITIVITY_Z = 10.0f;

    // Para detectar TAP vs DRAG
    private float lastTouchX = 0f;
    private float lastTouchY = 0f;
    private long touchStartTime = 0;
    private boolean isDragging = false;
    private static final long TAP_TIMEOUT = 200; // ms
    private static final float DRAG_THRESHOLD = 0.02f;

    // Tiempo para animaciones
    private float time = 0f;

    // Balanceo sutil
    private float bobOffset = 0f;
    private static final float BOB_SPEED = 2.0f;
    private static final float BOB_AMOUNT = 0.02f;

    // Cámara
    private CameraController camera;

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // ═══════════════════════════════════════════════════════════════════════
    // SHADERS - Simple con underglow neón
    // ═══════════════════════════════════════════════════════════════════════

    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "precision highp float;\n" +
        "in vec3 aPosition;\n" +
        "in vec2 aTexCoord;\n" +
        "uniform mat4 uMVPMatrix;\n" +
        "uniform mat4 uModelMatrix;\n" +
        "out vec2 vTexCoord;\n" +
        "out vec3 vPosition;\n" +
        "void main() {\n" +
        "    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);\n" +
        "    vTexCoord = aTexCoord;\n" +
        "    vPosition = aPosition;\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "uniform sampler2D uTexture;\n" +
        "uniform float uTime;\n" +
        "in vec2 vTexCoord;\n" +
        "in vec3 vPosition;\n" +
        "out vec4 fragColor;\n" +
        "\n" +
        "void main() {\n" +
        "    vec4 texColor = texture(uTexture, vTexCoord);\n" +
        "    \n" +
        "    // Underglow neón en la parte inferior del carro\n" +
        "    float underglowArea = smoothstep(-0.3, -0.1, vPosition.y) * (1.0 - smoothstep(-0.1, 0.1, vPosition.y));\n" +
        "    \n" +
        "    // Color del underglow: pulsa entre cyan y magenta\n" +
        "    vec3 glowColor1 = vec3(0.0, 1.0, 1.0);  // Cyan\n" +
        "    vec3 glowColor2 = vec3(1.0, 0.0, 1.0);  // Magenta\n" +
        "    float pulse = sin(uTime * 3.0) * 0.5 + 0.5;\n" +
        "    vec3 glowColor = mix(glowColor1, glowColor2, pulse) * 0.5;\n" +
        "    \n" +
        "    // Combinar textura con underglow\n" +
        "    vec3 finalColor = texColor.rgb + glowColor * underglowArea;\n" +
        "    \n" +
        "    // Luces traseras rojas (zona posterior del carro)\n" +
        "    float tailLightArea = smoothstep(0.3, 0.5, vPosition.z) * smoothstep(-0.2, 0.0, vPosition.y);\n" +
        "    vec3 tailLightColor = vec3(1.0, 0.1, 0.1) * (sin(uTime * 2.0) * 0.2 + 0.8);\n" +
        "    finalColor += tailLightColor * tailLightArea * 0.3;\n" +
        "    \n" +
        "    fragColor = vec4(finalColor, texColor.a);\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════

    public DeLorean3D(Context context, TextureLoader textureLoader) {
        this.context = context;
        this.textureLoader = textureLoader;

        loadModel();
        loadTexture();
        compileShader();

        Log.d(TAG, "🚗 DeLorean3D creado - rumbo al horizonte synthwave");
    }

    private void loadModel() {
        try {
            Log.d(TAG, "📦 Cargando delorean.obj...");

            ObjLoader.Mesh mesh = ObjLoader.loadObj(context, "delorean.obj", true);

            Log.d(TAG, "✓ Modelo cargado: " + mesh.vertexCount + " vértices");

            this.vertexBuffer = mesh.vertexBuffer;
            this.uvBuffer = mesh.uvBuffer;

            // Construir índices desde faces
            int totalIndices = 0;
            for (int[] face : mesh.faces) {
                totalIndices += (face.length - 2) * 3;
            }

            int[] indices = new int[totalIndices];
            int idx = 0;
            for (int[] face : mesh.faces) {
                int v0 = face[0];
                for (int i = 1; i < face.length - 1; i++) {
                    indices[idx++] = v0;
                    indices[idx++] = face[i];
                    indices[idx++] = face[i + 1];
                }
            }

            this.indexCount = totalIndices;

            ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 4);
            ibb.order(ByteOrder.nativeOrder());
            indexBuffer = ibb.asIntBuffer();
            indexBuffer.put(indices);
            indexBuffer.position(0);

            Log.d(TAG, "✅ Modelo listo: " + indexCount + " índices");

        } catch (IOException e) {
            Log.e(TAG, "❌ Error cargando modelo: " + e.getMessage());
        }
    }

    private void loadTexture() {
        textureId = textureLoader.getTexture(R.drawable.delorean_texture);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);

        Log.d(TAG, "✅ Textura DeLorean cargada: " + textureId);
    }

    private void compileShader() {
        int vs = compileShaderCode(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compileShaderCode(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vs);
        GLES30.glAttachShader(shaderProgram, fs);
        GLES30.glLinkProgram(shaderProgram);

        aPositionHandle = GLES30.glGetAttribLocation(shaderProgram, "aPosition");
        aTexCoordHandle = GLES30.glGetAttribLocation(shaderProgram, "aTexCoord");
        uMVPMatrixHandle = GLES30.glGetUniformLocation(shaderProgram, "uMVPMatrix");
        uModelMatrixHandle = GLES30.glGetUniformLocation(shaderProgram, "uModelMatrix");
        uTextureHandle = GLES30.glGetUniformLocation(shaderProgram, "uTexture");
        uTimeHandle = GLES30.glGetUniformLocation(shaderProgram, "uTime");

        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);

        Log.d(TAG, "✅ Shader DeLorean compilado");
    }

    private int compileShaderCode(int type, String code) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, code);
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

    // ═══════════════════════════════════════════════════════════════════════
    // UPDATE & DRAW
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void update(float deltaTime) {
        time += deltaTime;

        // Balanceo sutil (como si fuera en la carretera)
        bobOffset = (float) Math.sin(time * BOB_SPEED) * BOB_AMOUNT;

        // 🚗 CONDUCCIÓN AUTÓNOMA
        if (autonomousDriving && !adjustEnabled) {
            updateAutonomousDriving(deltaTime);
        }
    }

    /**
     * Sistema de micro-movimientos sutiles (el carro se queda en su lugar)
     */
    private void updateAutonomousDriving(float deltaTime) {
        // Avanzar fase del wobble
        wobblePhase += deltaTime * WOBBLE_SPEED;

        // Micro-movimiento lateral (como pequeñas correcciones de volante)
        float wobbleX = (float) Math.sin(wobblePhase) * WOBBLE_X;
        wobbleX += (float) Math.sin(wobblePhase * 1.7f) * WOBBLE_X * 0.5f;  // Segundo armónico

        // Micro-movimiento vertical (como pequeños baches en la carretera)
        float wobbleY = (float) Math.sin(wobblePhase * 1.3f) * WOBBLE_Y;

        // Micro-rotación (como ajustes sutiles del volante)
        float wobbleRot = (float) Math.sin(wobblePhase * 0.8f) * WOBBLE_ROT;

        // Aplicar micro-movimientos a la posición base
        x = BASE_X + wobbleX;
        y = BASE_Y + wobbleY;
        rotationZ = BASE_ROT_Z + wobbleRot;
    }

    @Override
    public void draw() {
        if (vertexBuffer == null || indexBuffer == null) return;

        GLES30.glUseProgram(shaderProgram);

        // Model matrix con posición, rotación y escala
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y + bobOffset, z);
        Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f);
        Matrix.rotateM(modelMatrix, 0, rotationX, 1f, 0f, 0f);
        Matrix.rotateM(modelMatrix, 0, rotationZ, 0f, 0f, 1f);
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

        // MVP matrix
        if (camera != null) {
            float[] vpMatrix = camera.getViewProjectionMatrix();
            Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);
        } else {
            System.arraycopy(modelMatrix, 0, mvpMatrix, 0, 16);
        }

        // Uniforms
        GLES30.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLES30.glUniformMatrix4fv(uModelMatrixHandle, 1, false, modelMatrix, 0);
        GLES30.glUniform1f(uTimeHandle, time);

        // Textura
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(uTextureHandle, 0);

        // Vertices
        vertexBuffer.position(0);
        GLES30.glEnableVertexAttribArray(aPositionHandle);
        GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        // UVs
        uvBuffer.position(0);
        GLES30.glEnableVertexAttribArray(aTexCoordHandle);
        GLES30.glVertexAttribPointer(aTexCoordHandle, 2, GLES30.GL_FLOAT, false, 0, uvBuffer);

        // Draw
        indexBuffer.position(0);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, indexBuffer);

        GLES30.glDisableVertexAttribArray(aPositionHandle);
        GLES30.glDisableVertexAttribArray(aTexCoordHandle);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SETTERS
    // ═══════════════════════════════════════════════════════════════════════

    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setRotation(float rx, float ry, float rz) {
        this.rotationX = rx;
        this.rotationY = ry;
        this.rotationZ = rz;
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🎮 SISTEMA DE CALIBRACIÓN COMPLETO
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Maneja eventos de touch para calibración
     * @param normalizedX -1 (izquierda) a 1 (derecha)
     * @param normalizedY -1 (abajo) a 1 (arriba)
     * @param action MotionEvent action (ACTION_DOWN, ACTION_MOVE, ACTION_UP)
     * @return true si se procesó el touch
     */
    public boolean onTouchEvent(float normalizedX, float normalizedY, int action) {
        if (!adjustEnabled || currentMode == AdjustMode.OFF) return false;

        switch (action) {
            case android.view.MotionEvent.ACTION_DOWN:
                lastTouchX = normalizedX;
                lastTouchY = normalizedY;
                touchStartTime = System.currentTimeMillis();
                isDragging = false;
                logCurrentState("👆 TOUCH DOWN");
                return true;

            case android.view.MotionEvent.ACTION_MOVE:
                float deltaX = normalizedX - lastTouchX;
                float deltaY = normalizedY - lastTouchY;

                // Detectar si es drag
                if (Math.abs(deltaX) > DRAG_THRESHOLD || Math.abs(deltaY) > DRAG_THRESHOLD) {
                    isDragging = true;
                    applyAdjustment(deltaX, deltaY);
                    lastTouchX = normalizedX;
                    lastTouchY = normalizedY;
                }
                return true;

            case android.view.MotionEvent.ACTION_UP:
                long touchDuration = System.currentTimeMillis() - touchStartTime;

                // Si fue un TAP rápido (no drag), cambiar modo
                if (!isDragging && touchDuration < TAP_TIMEOUT) {
                    cycleMode();
                }
                return true;
        }
        return false;
    }

    /**
     * Aplica el ajuste según el modo actual
     */
    private void applyAdjustment(float deltaX, float deltaY) {
        switch (currentMode) {
            case POSITION_XY:
                x += deltaX * SENSITIVITY_POSITION;
                y += deltaY * SENSITIVITY_POSITION;
                logCurrentState("📍 POSICIÓN X/Y");
                break;

            case POSITION_Z:
                z += deltaY * SENSITIVITY_Z;
                logCurrentState("📍 POSICIÓN Z (profundidad)");
                break;

            case ROTATE_Y:
                rotationY += deltaX * SENSITIVITY_ROTATION;
                logCurrentState("🔄 ROTACIÓN Y (horizontal)");
                break;

            case ROTATE_X:
                rotationX += deltaY * SENSITIVITY_ROTATION;
                logCurrentState("🔄 ROTACIÓN X (inclinación)");
                break;

            case ROTATE_Z:
                rotationZ += deltaX * SENSITIVITY_ROTATION;
                logCurrentState("🔄 ROTACIÓN Z (volante)");
                break;

            case SCALE:
                scale += deltaY * SENSITIVITY_SCALE;
                scale = Math.max(0.1f, Math.min(5.0f, scale)); // Limitar entre 0.1 y 5.0
                logCurrentState("📐 ESCALA");
                break;
        }
    }

    /**
     * Cambia al siguiente modo de ajuste
     */
    private void cycleMode() {
        AdjustMode[] modes = AdjustMode.values();
        int currentIndex = currentMode.ordinal();
        int nextIndex = (currentIndex + 1) % modes.length;

        // Saltar OFF en el ciclo
        if (modes[nextIndex] == AdjustMode.OFF) {
            nextIndex = (nextIndex + 1) % modes.length;
        }

        currentMode = modes[nextIndex];

        Log.d(TAG, "═══════════════════════════════════════════════════");
        Log.d(TAG, "🎮 MODO CAMBIADO A: " + getModeDescription(currentMode));
        Log.d(TAG, "═══════════════════════════════════════════════════");
        logCurrentState("📊 VALORES ACTUALES");
    }

    /**
     * Obtiene descripción del modo
     */
    private String getModeDescription(AdjustMode mode) {
        switch (mode) {
            case POSITION_XY: return "POSICIÓN X/Y (arrastra para mover)";
            case POSITION_Z:  return "POSICIÓN Z (arrastra vertical = profundidad)";
            case ROTATE_Y:    return "ROTACIÓN Y (arrastra horizontal = girar)";
            case ROTATE_X:    return "ROTACIÓN X (arrastra vertical = inclinar)";
            case ROTATE_Z:    return "ROTACIÓN Z (arrastra horizontal = rotar volante)";
            case SCALE:       return "ESCALA (arrastra vertical = tamaño)";
            default:          return "DESACTIVADO";
        }
    }

    /**
     * Log del estado actual con todos los valores
     */
    private void logCurrentState(String action) {
        Log.d(TAG, "───────────────────────────────────────────────────");
        Log.d(TAG, action);
        Log.d(TAG, "🎮 MODO: " + getModeDescription(currentMode));
        Log.d(TAG, "───────────────────────────────────────────────────");
        Log.d(TAG, String.format("📍 POSICIÓN:  x=%.2f  y=%.2f  z=%.2f", x, y, z));
        Log.d(TAG, String.format("🔄 ROTACIÓN:  rX=%.1f°  rY=%.1f°  rZ=%.1f°", rotationX, rotationY, rotationZ));
        Log.d(TAG, String.format("📐 ESCALA:    %.2f", scale));
        Log.d(TAG, "───────────────────────────────────────────────────");
    }

    /**
     * Activa/desactiva modo ajuste
     */
    public void setAdjustEnabled(boolean enabled) {
        this.adjustEnabled = enabled;
        Log.d(TAG, "🔧 Sistema de ajuste: " + (enabled ? "ACTIVADO" : "DESACTIVADO"));
        if (enabled) {
            logCurrentState("📊 ESTADO INICIAL");
        }
    }

    /**
     * Establece el modo de ajuste
     */
    public void setAdjustMode(AdjustMode mode) {
        this.currentMode = mode;
        Log.d(TAG, "🎮 Modo establecido: " + getModeDescription(mode));
    }

    /**
     * Imprime los valores finales para copiar al código
     */
    public void printFinalValues() {
        Log.d(TAG, "╔══════════════════════════════════════════════════════════════╗");
        Log.d(TAG, "║  📋 VALORES FINALES PARA COPIAR AL CÓDIGO:                   ║");
        Log.d(TAG, "╠══════════════════════════════════════════════════════════════╣");
        Log.d(TAG, String.format("║  private float x = %.2ff;", x));
        Log.d(TAG, String.format("║  private float y = %.2ff;", y));
        Log.d(TAG, String.format("║  private float z = %.2ff;", z));
        Log.d(TAG, String.format("║  private float scale = %.2ff;", scale));
        Log.d(TAG, String.format("║  private float rotationX = %.1ff;", rotationX));
        Log.d(TAG, String.format("║  private float rotationY = %.1ff;", rotationY));
        Log.d(TAG, String.format("║  private float rotationZ = %.1ff;", rotationZ));
        Log.d(TAG, "╚══════════════════════════════════════════════════════════════╝");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // RELEASE
    // ═══════════════════════════════════════════════════════════════════════

    public void release() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        // Imprimir valores finales al liberar
        if (adjustEnabled) {
            printFinalValues();
        }
        Log.d(TAG, "🚗 DeLorean3D liberado");
    }
}
