package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.image.ImageDownloadManager;
import com.secret.blackholeglow.model.ModelDownloadManager;
import com.secret.blackholeglow.util.ObjLoader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   🧟 ZombieBody3D - Zombie cuerpo completo para Walking Dead Scene       ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  CARACTERÍSTICAS:                                                         ║
 * ║  • Zombie de cuerpo completo (modelo Meshy AI)                           ║
 * ║  • Posicionado abajo - solo se ve mitad superior (cintura para arriba)   ║
 * ║  • 👆 TOUCH: Giro libre 360° al deslizar                                 ║
 * ║  • Balanceo suave como si estuviera trepando                             ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class ZombieBody3D implements SceneObject {
    private static final String TAG = "ZombieBody3D";

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
    private int uTextureHandle;
    private int uTimeHandle;

    // ═══════════════════════════════════════════════════════════════════════
    // 🧟 POSICIÓN - Mitad inferior de pantalla (solo se ve cintura para arriba)
    // ═══════════════════════════════════════════════════════════════════════

    // ═══════════════════════════════════════════════════════════════════════
    // 🧟 POSICIÓN CALIBRADA v2 (2026-01-18 23:34)
    // ═══════════════════════════════════════════════════════════════════════
    private float x = 0.63f;      // Ligeramente a la derecha
    private float y = -2.29f;     // Asomando desde abajo
    private float z = -0.32f;     // Cerca de cámara
    private float scale = 2.38f;  // Tamaño grande

    // Rotación base calibrada
    private float rotationX = 7.5f;
    private float rotationY = -14.0f;   // Girado hacia la izquierda
    private float rotationZ = 3.4f;

    // ═══════════════════════════════════════════════════════════════════════
    // 🧟 MOVIMIENTO ORGÁNICO "VIVO" (sin touch - solo animación automática)
    // ═══════════════════════════════════════════════════════════════════════

    // Respiración - MUY SUTIL
    private float breathPhase = 0f;
    private static final float BREATH_SPEED = 0.4f;      // Muy lento
    private static final float BREATH_SCALE = 0.008f;    // Apenas perceptible

    // Temblor sutil (como si luchara) - SUAVE
    private float trembleTimer = 0f;
    private float trembleX = 0f;
    private float trembleY = 0f;
    private static final float TREMBLE_SPEED = 2.0f;     // Lento
    private static final float TREMBLE_AMOUNT = 0.15f;   // Muy sutil

    // Reaching (alcanzando hacia la cámara) - SUAVE
    private float reachPhase = 0f;
    private static final float REACH_SPEED = 0.15f;      // Muy lento
    private static final float REACH_AMOUNT = 0.02f;     // Apenas perceptible

    // ═══════════════════════════════════════════════════════════════════════
    // 🎮 SISTEMA DE CALIBRACIÓN POR TOUCH
    // ═══════════════════════════════════════════════════════════════════════

    public enum AdjustMode {
        POSITION_XY,   // Mover en X/Y
        POSITION_Z,    // Mover en Z (profundidad)
        ROTATE_Y,      // Girar horizontalmente
        ROTATE_XZ,     // Inclinar
        SCALE          // Cambiar tamaño
    }

    private AdjustMode currentMode = AdjustMode.POSITION_XY;
    private boolean calibrationEnabled = false;  // 🔒 DESACTIVADO - posición fija

    private float lastTouchX = 0f;
    private float lastTouchY = 0f;
    private long touchStartTime = 0;
    private boolean isDragging = false;
    private static final long TAP_TIMEOUT = 300;

    // Sensibilidad de ajustes
    private static final float SENS_POSITION = 3.0f;
    private static final float SENS_Z = 5.0f;
    private static final float SENS_ROTATION = 180.0f;
    private static final float SENS_SCALE = 2.0f;

    // ═══════════════════════════════════════════════════════════════════════
    // 🔗 BALANCEO SUAVE (como si estuviera trepando)
    // ═══════════════════════════════════════════════════════════════════════

    private float swingPhase = 0f;
    private static final float SWING_SPEED = 0.2f;    // Más lento
    private static final float SWING_ANGLE = 1.5f;    // Más sutil

    // Tiempo
    private float time = 0f;
    private static final float TIME_CYCLE = 100.0f;

    // Pantalla y proyección
    private int screenWidth = 1080;
    private int screenHeight = 1920;
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] vpMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // ═══════════════════════════════════════════════════════════════════════
    // SHADERS
    // ═══════════════════════════════════════════════════════════════════════

    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "precision highp float;\n" +
        "in vec3 aPosition;\n" +
        "in vec2 aTexCoord;\n" +
        "uniform mat4 uMVPMatrix;\n" +
        "out vec2 vTexCoord;\n" +
        "void main() {\n" +
        "    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);\n" +
        "    vTexCoord = aTexCoord;\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "uniform sampler2D uTexture;\n" +
        "uniform float uTime;\n" +
        "in vec2 vTexCoord;\n" +
        "out vec4 fragColor;\n" +
        "\n" +
        "void main() {\n" +
        "    vec4 texColor = texture(uTexture, vTexCoord);\n" +
        "    vec3 color = texColor.rgb;\n" +
        "    \n" +
        "    // ═══════════════════════════════════════════════════════════\n" +
        "    // 🌑 OSCURECER ZOMBIE - Para mejor integración con fondo\n" +
        "    // ═══════════════════════════════════════════════════════════\n" +
        "    color *= 0.55;  // Reducir brillo general al 55%\n" +
        "    \n" +
        "    // ═══════════════════════════════════════════════════════════\n" +
        "    // 👁️ EFECTO OJOS - Solo las partes MUY brillantes\n" +
        "    // ═══════════════════════════════════════════════════════════\n" +
        "    \n" +
        "    // Detectar SOLO las áreas más brillantes (umbral alto)\n" +
        "    float brightness = (texColor.r + texColor.g + texColor.b) / 3.0;\n" +
        "    float eyeMask = smoothstep(0.82, 0.95, brightness);\n" +
        "    \n" +
        "    // Pulso MUY sutil\n" +
        "    float subtlePulse = sin(uTime * 2.0) * 0.15 + 0.85;\n" +
        "    \n" +
        "    // Color del brillo: rojo oscuro sutil\n" +
        "    vec3 eyeColor = vec3(0.8, 0.15, 0.1);\n" +
        "    \n" +
        "    // Aplicar ojos (estos SÍ brillan, no se oscurecen)\n" +
        "    color = mix(color, eyeColor, eyeMask * 0.6);\n" +
        "    color += eyeColor * eyeMask * subtlePulse * 0.25;\n" +
        "    \n" +
        "    fragColor = vec4(color, texColor.a);\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════

    public ZombieBody3D(Context context, TextureLoader textureLoader) {
        this.context = context;
        this.textureLoader = textureLoader;

        setupProjection();
        loadModel();
        loadTexture();
        compileShader();

        Log.d(TAG, "🧟 ZombieBody3D creado - MOVIMIENTO ORGÁNICO");
        Log.d(TAG, "📍 Posición: x=" + x + " y=" + y + " z=" + z + " scale=" + scale);
    }

    private void setupProjection() {
        Matrix.setLookAtM(viewMatrix, 0,
            0f, 0f, 3f,
            0f, 0f, 0f,
            0f, 1f, 0f);

        float ratio = (float) screenWidth / screenHeight;
        Matrix.perspectiveM(projectionMatrix, 0, 60f, ratio, 0.1f, 100f);
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
    }

    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        setupProjection();
    }

    private void loadModel() {
        try {
            Log.d(TAG, "📦 Cargando zombie_body.obj...");

            ObjLoader.Mesh mesh;
            ModelDownloadManager modelMgr = ModelDownloadManager.getInstance(context);
            String modelPath = modelMgr.getModelPath("zombie_body.obj");

            if (modelPath != null) {
                Log.d(TAG, "🌐 Cargando modelo desde: " + modelPath);
                mesh = ObjLoader.loadObjFromFile(modelPath, true);
            } else {
                Log.e(TAG, "❌ Modelo no disponible: zombie_body.obj");
                return;
            }

            Log.d(TAG, "✓ Modelo cargado: " + mesh.vertexCount + " vértices");

            this.vertexBuffer = mesh.vertexBuffer;
            this.uvBuffer = mesh.uvBuffer;

            // Crear index buffer
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
        ImageDownloadManager imageMgr = ImageDownloadManager.getInstance(context);
        String texturePath = imageMgr.getImagePath("zombie_body_texture.webp");

        if (texturePath != null && textureLoader instanceof TextureManager) {
            Log.d(TAG, "🌐 Cargando textura desde: " + texturePath);
            textureId = ((TextureManager) textureLoader).loadTextureFromFile(texturePath);
        } else {
            Log.e(TAG, "❌ Textura no disponible: zombie_body_texture.webp");
            return;
        }

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);

        Log.d(TAG, "✅ Textura cargada: " + textureId);
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
        uTextureHandle = GLES30.glGetUniformLocation(shaderProgram, "uTexture");
        uTimeHandle = GLES30.glGetUniformLocation(shaderProgram, "uTime");

        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);

        Log.d(TAG, "✅ Shader compilado");
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
        if (time > TIME_CYCLE) time -= TIME_CYCLE;

        // 🫁 Respiración
        breathPhase += deltaTime * BREATH_SPEED * 2.0f * (float) Math.PI;
        if (breathPhase > 6.28f) breathPhase -= 6.28f;

        // 💀 Temblor sutil (como si luchara por salir)
        trembleTimer += deltaTime * TREMBLE_SPEED;
        trembleX = (float) Math.sin(trembleTimer * 1.7f) * TREMBLE_AMOUNT;
        trembleY = (float) Math.sin(trembleTimer * 2.3f) * TREMBLE_AMOUNT * 0.7f;

        // 🖐️ Reaching hacia la cámara
        reachPhase += deltaTime * REACH_SPEED * 2.0f * (float) Math.PI;
        if (reachPhase > 6.28f) reachPhase -= 6.28f;

        // Balanceo suave (ya existente)
        swingPhase += deltaTime * SWING_SPEED;
        if (swingPhase > TIME_CYCLE) swingPhase -= TIME_CYCLE;
    }

    @Override
    public void draw() {
        if (vertexBuffer == null || indexBuffer == null || textureId == 0) {
            return;
        }

        GLES30.glUseProgram(shaderProgram);

        // 🧟 Cálculo de efectos orgánicos
        float swing = (float) Math.sin(swingPhase) * SWING_ANGLE;
        float breath = (float) Math.sin(breathPhase) * BREATH_SCALE;
        float reach = (float) Math.sin(reachPhase) * REACH_AMOUNT;

        // Model matrix
        Matrix.setIdentityM(modelMatrix, 0);

        // Posición base + reaching sutil hacia cámara
        Matrix.translateM(modelMatrix, 0, x, y, z + reach);

        // Rotación base + temblor + balanceo
        Matrix.rotateM(modelMatrix, 0, rotationY + trembleY, 0f, 1f, 0f);
        Matrix.rotateM(modelMatrix, 0, rotationX + swing + trembleX, 1f, 0f, 0f);
        Matrix.rotateM(modelMatrix, 0, rotationZ, 0f, 0f, 1f);

        // Escala con respiración sutil
        float breathingScale = scale * (1.0f + breath);
        Matrix.scaleM(modelMatrix, 0, breathingScale, breathingScale, breathingScale);

        // MVP
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);

        // Uniforms
        GLES30.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);
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
    // 👆 TOUCH - Giro libre
    // ═══════════════════════════════════════════════════════════════════════

    public boolean onTouchEvent(float normalizedX, float normalizedY, int action) {
        // 🎮 MODO CALIBRACIÓN (solo cuando está habilitado)
        if (calibrationEnabled) {
            switch (action) {
                case android.view.MotionEvent.ACTION_DOWN:
                    lastTouchX = normalizedX;
                    lastTouchY = normalizedY;
                    touchStartTime = System.currentTimeMillis();
                    isDragging = false;
                    return true;

                case android.view.MotionEvent.ACTION_MOVE:
                    float deltaX = normalizedX - lastTouchX;
                    float deltaY = normalizedY - lastTouchY;

                    if (Math.abs(deltaX) > 0.005f || Math.abs(deltaY) > 0.005f) {
                        isDragging = true;
                        applyAdjustment(deltaX, deltaY);
                        lastTouchX = normalizedX;
                        lastTouchY = normalizedY;
                    }
                    return true;

                case android.view.MotionEvent.ACTION_UP:
                    long duration = System.currentTimeMillis() - touchStartTime;
                    if (!isDragging && duration < TAP_TIMEOUT) {
                        cycleMode();
                    }
                    return true;
            }
        }
        // 🧟 Modo normal: NO responde al touch (solo la cabeza gira)
        return false;
    }

    private void applyAdjustment(float deltaX, float deltaY) {
        switch (currentMode) {
            case POSITION_XY:
                x += deltaX * SENS_POSITION;
                y += deltaY * SENS_POSITION;
                break;
            case POSITION_Z:
                z += deltaY * SENS_Z;
                break;
            case ROTATE_Y:
                rotationY += deltaX * SENS_ROTATION;
                break;
            case ROTATE_XZ:
                rotationX += deltaY * SENS_ROTATION;
                rotationZ += deltaX * SENS_ROTATION;
                break;
            case SCALE:
                scale += deltaY * SENS_SCALE;
                scale = Math.max(0.1f, Math.min(5.0f, scale));
                break;
        }
        logCurrentState();
    }

    private void cycleMode() {
        AdjustMode[] modes = AdjustMode.values();
        int nextIndex = (currentMode.ordinal() + 1) % modes.length;
        currentMode = modes[nextIndex];
        Log.d(TAG, "═══════════════════════════════════════════════════");
        Log.d(TAG, "🎮 MODO CAMBIADO: " + currentMode.name());
        Log.d(TAG, "═══════════════════════════════════════════════════");
        logCurrentState();
    }

    private void logCurrentState() {
        Log.d(TAG, String.format("🧟 [%s] x=%.2f y=%.2f z=%.2f | rotX=%.1f rotY=%.1f rotZ=%.1f | scale=%.2f",
            currentMode.name(), x, y, z, rotationX, rotationY, rotationZ, scale));
    }

    public void setCalibrationEnabled(boolean enabled) {
        this.calibrationEnabled = enabled;
        Log.d(TAG, "🎮 Calibración: " + (enabled ? "ACTIVADA" : "DESACTIVADA"));
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

    // ═══════════════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════════════

    public void dispose() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        if (textureId != 0) {
            int[] textures = {textureId};
            GLES30.glDeleteTextures(1, textures, 0);
            textureId = 0;
        }
        Log.d(TAG, "🧟 ZombieBody3D disposed");
    }
}
