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
 * ║   🚀 TravelingShip - Nave viajando hacia el sol (LabScene)               ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  CARACTERÍSTICAS:                                                         ║
 * ║  • Posición fija en la parte inferior de la pantalla                     ║
 * ║  • Apunta hacia arriba (hacia el sol/destino)                            ║
 * ║  • Sutil animación de balanceo (bobbing)                                 ║
 * ║  • Efecto de propulsión/engine glow                                      ║
 * ║  • Usa human_interceptor_flames.obj (nave + llamas integradas)           ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class TravelingShip implements SceneObject, CameraAware {
    private static final String TAG = "TravelingShip";

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
    private int uCameraPosHandle;
    private int uTextureHandle;
    private int uTimeHandle;
    private int uEngineGlowHandle;

    // Transformación - POSICIÓN FIJA (donde tocó el usuario)
    // 📍 RAW: (530, 1795) en pantalla 1080x2340
    private float x = -2.5f;         // Centro horizontal
    private float y = -4.9f;  // Valor calibrado por touch     // Más abajo
    private float z = -2.5f;      // Profundidad
    private float scale = 0.8f;   // Tamaño

    // Rotación - ajustable para apuntar hacia el sol
    private float rotationX = 0f;
    private float rotationY = 304.7f;  // Valor fijo encontrado por touch
    private float rotationZ = 4.0f;

    // Tiempo para efectos
    private float time = 0f;

    // Engine glow
    private float engineGlow = 1.0f;

    // 🔧 MODOS DE PRUEBA (desactivados)
    private boolean shipAdjustMode = false;
    private boolean testLinearMode = false;  // Desactivado - vuelo normal
    private float testTime = 0f;

    // 📱 CONTROL POR GIROSCOPIO
    private boolean gyroEnabled = false;
    private float gyroTiltX = 0f;  // -1 (izquierda) a 1 (derecha)
    private float gyroTiltY = 0f;  // -1 (hacia usuario) a 1 (alejándose)
    private static final float GYRO_X_INFLUENCE = 1.5f;  // Cuánto afecta tilt lateral
    private static final float GYRO_ROLL_INFLUENCE = 15f;  // Cuánto afecta al banking
    private static final float ADJUST_X = -2.5f;
    private static final float ADJUST_Y = -4.9f;
    private static final float ADJUST_Z = -2.5f;
    private static final float ADJUST_SCALE = 0.8f;

    // Cámara
    private CameraController camera;

    // Matrices
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
        "uniform mat4 uModelMatrix;\n" +
        "out vec2 vTexCoord;\n" +
        "out vec3 vPosition;\n" +
        "out vec3 vWorldPos;\n" +
        "void main() {\n" +
        "    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);\n" +
        "    vTexCoord = aTexCoord;\n" +
        "    vPosition = aPosition;\n" +
        "    vWorldPos = (uModelMatrix * vec4(aPosition, 1.0)).xyz;\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════════════
    // 🔥 FRAGMENT SHADER CON FRESNEL RIM LIGHT
    // ═══════════════════════════════════════════════════════════════════════
    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "uniform sampler2D uTexture;\n" +
        "uniform float uTime;\n" +
        "uniform float uEngineGlow;\n" +
        "uniform vec3 uCameraPos;\n" +
        "in vec2 vTexCoord;\n" +
        "in vec3 vPosition;\n" +
        "in vec3 vWorldPos;\n" +
        "out vec4 fragColor;\n" +
        "\n" +
        "void main() {\n" +
        "    // Detección de llamas (conos X > 0.65)\n" +
        "    float flameArea = smoothstep(0.65, 0.75, vPosition.x);\n" +
        "    \n" +
        "    // Textura base\n" +
        "    vec4 texColor = texture(uTexture, vTexCoord);\n" +
        "    \n" +
        "    // ═══════════════════════════════════════════════════════════\n" +
        "    // 🔆 FRESNEL RIM LIGHT (solo en nave, no en llamas)\n" +
        "    // ═══════════════════════════════════════════════════════════\n" +
        "    vec3 normal = normalize(cross(dFdx(vWorldPos), dFdy(vWorldPos)));\n" +
        "    vec3 viewDir = normalize(uCameraPos - vWorldPos);\n" +
        "    float fresnel = pow(1.0 - abs(dot(normal, viewDir)), 3.0);\n" +
        "    \n" +
        "    // Color del rim: naranja/dorado del ambiente de fuego\n" +
        "    vec3 rimColor = vec3(1.0, 0.6, 0.2) * fresnel * 1.2;\n" +
        "    \n" +
        "    // Aplicar rim solo a la nave (no a las llamas)\n" +
        "    vec3 shipColor = texColor.rgb + rimColor * (1.0 - flameArea);\n" +
        "    \n" +
        "    // ═══════════════════════════════════════════════════════════\n" +
        "    // 🔥 LLAMAS (igual que antes)\n" +
        "    // ═══════════════════════════════════════════════════════════\n" +
        "    float dist = clamp((vPosition.x - 0.65) * 2.0, 0.0, 1.0);\n" +
        "    float wave = sin(vPosition.y * 15.0 + vPosition.z * 10.0 + uTime * 8.0) * 0.5 + 0.5;\n" +
        "    \n" +
        "    vec3 flameColor = mix(\n" +
        "        mix(vec3(0.2, 0.8, 1.0), vec3(1.0, 0.5, 0.1), dist * 2.0),\n" +
        "        mix(vec3(1.0, 0.5, 0.1), vec3(1.0, 0.9, 0.3), dist * 2.0 - 1.0),\n" +
        "        step(0.5, dist)\n" +
        "    );\n" +
        "    \n" +
        "    float intensity = (1.0 - dist * 0.5) * uEngineGlow * (0.85 + wave * 0.15);\n" +
        "    float flameAlpha = (1.0 - dist * 0.4) * (0.8 + wave * 0.2);\n" +
        "    \n" +
        "    // Mezclar nave (con rim) y llamas\n" +
        "    vec3 finalColor = mix(shipColor, flameColor * intensity * 1.3, flameArea);\n" +
        "    float finalAlpha = mix(texColor.a, flameAlpha, flameArea);\n" +
        "    \n" +
        "    fragColor = vec4(finalColor, finalAlpha);\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════

    public TravelingShip(Context context, TextureLoader textureLoader) {
        this.context = context;
        this.textureLoader = textureLoader;

        loadModel();
        loadTexture();
        compileShader();

        Log.d(TAG, "🚀 TravelingShip creada - viajando hacia el sol");
    }

    private void loadModel() {
        try {
            Log.d(TAG, "📦 Cargando human_interceptor_flames.obj (nave + llamas)...");

            // flipV=true para corregir orientación de texturas
            ObjLoader.Mesh mesh = ObjLoader.loadObj(context, "human_interceptor_flames.obj", true);

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
        textureId = textureLoader.getTexture(R.drawable.human_interceptor_texture);
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
        uModelMatrixHandle = GLES30.glGetUniformLocation(shaderProgram, "uModelMatrix");
        uCameraPosHandle = GLES30.glGetUniformLocation(shaderProgram, "uCameraPos");
        uTextureHandle = GLES30.glGetUniformLocation(shaderProgram, "uTexture");
        uTimeHandle = GLES30.glGetUniformLocation(shaderProgram, "uTime");
        uEngineGlowHandle = GLES30.glGetUniformLocation(shaderProgram, "uEngineGlow");

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
    // UPDATE - Vuelo ida y vuelta con destinos aleatorios
    // ═══════════════════════════════════════════════════════════════════════

    // Posición de ORIGEN (siempre vuelve aquí)
    private static final float ORIGIN_X = -2.5f;
    private static final float ORIGIN_Y = -4.9f;
    private static final float ORIGIN_Z = -2.5f;
    private static final float ORIGIN_SCALE = 0.8f;


    // Estados del viaje
    private static final int STATE_HOVERING = 0;    // Planeando en origen
    private static final int STATE_FIGURE_8 = 1;    // Vuelo en figura de 8
    private int travelState = STATE_HOVERING;

    // Parámetros de la figura de 8
    private static final float FIGURE_8_DURATION_MIN = 20f;  // Mínimo 20 seg para completar el 8
    private static final float FIGURE_8_DURATION_MAX = 35f;  // Máximo 35 seg
    private float currentFigure8Duration = 25f;
    private float figure8Phase = 0f;  // 0 a 2π

    // Dimensiones del 8
    private static final float FIGURE_8_WIDTH = 0.8f;   // Ancho lateral del 8
    private static final float FIGURE_8_HEIGHT = 3.5f;  // Altura vertical (Y)
    private static final float FIGURE_8_DEPTH = 4.0f;   // Profundidad (Z - hacia horizonte)

    // Tiempo de planeo - VARIABLE (aleatorio)
    private static final float HOVER_DURATION_MIN = 4f;
    private static final float HOVER_DURATION_MAX = 10f;
    private float currentHoverDuration = 6f;
    private float hoverTime = 0f;

    // Rotación base
    private static final float BASE_ROTATION_Y = 304.7f;  // Mirando al horizonte
    private float targetRotationY = BASE_ROTATION_Y;

    // Parámetros de vuelo orgánico
    private static final float ROLL_FACTOR = 10f;

    // Parámetros de planeo en hover
    private static final float HOVER_DRIFT_AMPLITUDE = 0.75f;
    private static final float HOVER_DRIFT_SPEED = 0.5f;

    // Random para duraciones
    private final java.util.Random random = new java.util.Random();

    @Override
    public void update(float deltaTime) {
        time += deltaTime;

        // ═══════════════════════════════════════════════════════════════
        // 🔧 MODO AJUSTE - Nave fija en centro para calibrar llamas
        // ═══════════════════════════════════════════════════════════════
        if (shipAdjustMode) {
            x = ADJUST_X;
            y = ADJUST_Y;
            z = ADJUST_Z;
            scale = ADJUST_SCALE;
            rotationX = 0f;
            rotationY = BASE_ROTATION_Y;  // 304.7° - posición REAL de vuelo
            rotationZ = 0f;
            engineGlow = 1.0f;

            return;  // No ejecutar lógica de vuelo
        }

        // ═══════════════════════════════════════════════════════════════
        // 🧪 PRUEBA: Movimiento lineal en Z (hacia horizonte)
        // ═══════════════════════════════════════════════════════════════
        if (testLinearMode) {
            testTime += deltaTime;

            // Posición base
            x = ADJUST_X;      // -2.5
            y = ADJUST_Y;      // -4.9
            scale = ADJUST_SCALE;

            // Rotación FIJA (sin roll ni pitch)
            rotationX = 0f;
            rotationY = BASE_ROTATION_Y;  // 304.7°
            rotationZ = 0f;

            // Avanzar en Z (hacia horizonte) durante 3 segundos
            float progress = Math.min(testTime, 3.0f);  // Máximo 3 seg
            z = ADJUST_Z + progress * (-2.0f);  // Avanza 6 unidades en Z (se aleja)

            engineGlow = 1.0f;
            return;
        }

        // ═══════════════════════════════════════════════════════════════
        // 🚀 MÁQUINA DE ESTADOS DEL VIAJE
        // ═══════════════════════════════════════════════════════════════

        switch (travelState) {
            case STATE_HOVERING:
                // Planeando en el origen antes de partir
                hoverTime += deltaTime;
                updateHovering(deltaTime);
                if (hoverTime >= currentHoverDuration) {
                    hoverTime = 0f;
                    figure8Phase = 0f;
                    // Generar duración aleatoria para el 8
                    currentFigure8Duration = FIGURE_8_DURATION_MIN +
                        random.nextFloat() * (FIGURE_8_DURATION_MAX - FIGURE_8_DURATION_MIN);
                    travelState = STATE_FIGURE_8;
                    Log.d(TAG, "🎱 Iniciando figura 8 - Duración: " +
                        String.format("%.1f", currentFigure8Duration) + "s");
                }
                break;

            case STATE_FIGURE_8:
                // Vuelo en figura de 8 continuo
                float phaseSpeed = (float)(2.0 * Math.PI) / currentFigure8Duration;
                figure8Phase += deltaTime * phaseSpeed;
                updateFigure8(deltaTime);

                // Cuando completa el 8 (2π), vuelve a hover
                if (figure8Phase >= 2.0f * Math.PI) {
                    figure8Phase = 0f;
                    travelState = STATE_HOVERING;
                    targetRotationY = BASE_ROTATION_Y;
                    // Nueva duración de hover
                    currentHoverDuration = HOVER_DURATION_MIN +
                        random.nextFloat() * (HOVER_DURATION_MAX - HOVER_DURATION_MIN);
                }
                break;
        }
    }

    /**
     * Actualiza el movimiento cuando está planeando en el origen
     * OPTIMIZADO: Cálculos de sin/cos reducidos usando cache
     * 📱 GYRO: Responde a inclinación del dispositivo
     */
    private void updateHovering(float deltaTime) {
        // Posición fija en origen
        z = ORIGIN_Z;
        scale = ORIGIN_SCALE;

        // Rotación mirando al horizonte
        rotationY = lerpAngle(rotationY, BASE_ROTATION_Y, deltaTime * 3f);

        // ═══════════════════════════════════════════════════════════════
        // 🛫 CÁLCULOS CACHEADOS (evita múltiples Math.sin/cos)
        // ═══════════════════════════════════════════════════════════════
        float t1 = time * HOVER_DRIFT_SPEED;
        float sinT1 = (float) Math.sin(t1);
        float cosT1 = (float) Math.cos(t1);
        float sinT2 = (float) Math.sin(time * 1.2f);  // Para bob
        float sinT3 = (float) Math.sin(time * 4.0f);  // Para estabilización y glow

        // Drift lateral simplificado (1 sin principal + 1 armónico)
        float drift = sinT1 * 0.8f + (float) Math.sin(t1 * 1.5f) * 0.2f;
        float baseX = ORIGIN_X + drift * HOVER_DRIFT_AMPLITUDE;

        // ═══════════════════════════════════════════════════════════════
        // 📱 GIROSCOPIO: Añade movimiento basado en inclinación
        // ═══════════════════════════════════════════════════════════════
        float gyroOffsetX = 0f;
        float gyroRoll = 0f;
        if (gyroEnabled) {
            // Mover lateralmente según inclinación
            gyroOffsetX = gyroTiltX * GYRO_X_INFLUENCE;
            // Inclinar visualmente la nave según tilt
            gyroRoll = gyroTiltX * GYRO_ROLL_INFLUENCE;
        }

        x = baseX + gyroOffsetX;

        // Bobbing vertical
        y = ORIGIN_Y + sinT2 * 0.05f;

        // Roll basado en velocidad del drift + giroscopio
        float driftVelocity = cosT1 * HOVER_DRIFT_SPEED * 0.9f;
        float mainRoll = driftVelocity * ROLL_FACTOR * 2.5f;
        float stabilization = sinT3 * 3.5f;
        rotationZ = 4.0f + mainRoll + stabilization + gyroRoll;

        // Pitch simplificado
        rotationX = (float) Math.sin(time * 0.7f) * 4.0f;

        // Engine glow
        engineGlow = 1.0f + sinT3 * 0.15f;
    }

    /**
     * Actualiza el movimiento en figura de 8
     * OPTIMIZADO: Cálculos de sin/cos cacheados
     */
    private void updateFigure8(float deltaTime) {
        float t = figure8Phase;

        // ═══════════════════════════════════════════════════════════════
        // 🎱 CÁLCULOS CACHEADOS
        // ═══════════════════════════════════════════════════════════════
        float sinT = (float) Math.sin(t);
        float cosT = (float) Math.cos(t);
        float sinHalfT = (float) Math.sin(t * 0.5f);
        float cosHalfT = (float) Math.cos(t * 0.5f);

        // Posición del 8
        x = ORIGIN_X + FIGURE_8_WIDTH * sinT;
        y = ORIGIN_Y + FIGURE_8_HEIGHT * sinHalfT;
        z = ORIGIN_Z - FIGURE_8_DEPTH * sinHalfT;

        // Escala según distancia
        scale = ORIGIN_SCALE - (ORIGIN_SCALE - 0.15f) * sinHalfT;

        // Derivadas para dirección
        float dx = FIGURE_8_WIDTH * cosT;
        float dy = (FIGURE_8_HEIGHT * 0.5f) * cosHalfT;

        // Rotación Y según dirección vertical
        targetRotationY = (dy >= 0) ? BASE_ROTATION_Y : BASE_ROTATION_Y + 180f;
        rotationY = lerpAngle(rotationY, targetRotationY, deltaTime * 4f);

        // Banking simplificado
        float sinStab = (float) Math.sin(time * 4.0f);
        rotationZ = 4.0f + dx * 30f + sinStab * 2.5f;

        // Pitch
        rotationX = dy * 8f + sinStab * 0.5f;

        // Engine glow
        engineGlow = 1.3f + Math.abs(dx) * 0.5f + sinStab * 0.1f;
    }

    /** Interpola ángulos correctamente (maneja el wrap de 360°) */
    private float lerpAngle(float from, float to, float t) {
        float diff = to - from;
        while (diff > 180f) diff -= 360f;
        while (diff < -180f) diff += 360f;
        return from + diff * Math.min(1f, t);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DRAW
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void draw() {
        if (shaderProgram == 0 || camera == null || vertexBuffer == null) return;

        GLES30.glUseProgram(shaderProgram);

        // Construir matriz de modelo
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);
        Matrix.rotateM(modelMatrix, 0, rotationX, 1f, 0f, 0f);  // Pitch hacia arriba
        Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f);  // Yaw
        Matrix.rotateM(modelMatrix, 0, rotationZ, 0f, 0f, 1f);  // Roll (balanceo)
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

        // Usar computeMvp del CameraController
        camera.computeMvp(modelMatrix, mvpMatrix);

        // Uniforms de matrices y cámara
        GLES30.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLES30.glUniformMatrix4fv(uModelMatrixHandle, 1, false, modelMatrix, 0);

        // Posición de cámara para Fresnel (aproximada desde la posición conocida)
        GLES30.glUniform3f(uCameraPosHandle, 4f, 3f, 6f);

        GLES30.glUniform1f(uTimeHandle, time);
        GLES30.glUniform1f(uEngineGlowHandle, engineGlow);

        // Textura
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(uTextureHandle, 0);

        // Vértices
        GLES30.glEnableVertexAttribArray(aPositionHandle);
        GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        // UVs
        GLES30.glEnableVertexAttribArray(aTexCoordHandle);
        GLES30.glVertexAttribPointer(aTexCoordHandle, 2, GLES30.GL_FLOAT, false, 0, uvBuffer);

        // Dibujar
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, indexBuffer);

        GLES30.glDisableVertexAttribArray(aPositionHandle);
        GLES30.glDisableVertexAttribArray(aTexCoordHandle);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setRotation(float pitch, float yaw, float roll) {
        this.rotationX = pitch;
        this.rotationY = yaw;
        this.rotationZ = roll;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 📱 CONTROL POR GIROSCOPIO
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Habilita/deshabilita el control por giroscopio
     */
    public void setGyroEnabled(boolean enabled) {
        this.gyroEnabled = enabled;
        Log.d(TAG, "📱 Giroscopio " + (enabled ? "habilitado" : "deshabilitado"));
    }

    /**
     * Recibe valores de inclinación del GyroscopeManager
     * @param tiltX -1 (izquierda) a 1 (derecha)
     * @param tiltY -1 (hacia usuario) a 1 (alejándose)
     */
    public void setTiltInput(float tiltX, float tiltY) {
        this.gyroTiltX = tiltX;
        this.gyroTiltY = tiltY;
    }

    public boolean isGyroEnabled() {
        return gyroEnabled;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🎮 CONTROL POR TOUCH - Rotación en ejes
    // ═══════════════════════════════════════════════════════════════════════

    private float lastTouchX = 0f;
    private boolean isTouching = false;
    private static final float ROTATION_SENSITIVITY = 180f;  // Grados por unidad de movimiento

    /**
     * Touch deshabilitado - nave vuela automáticamente
     */
    public void onTouchDown(float x, float y) {
        // Deshabilitado
    }

    public void onTouchMove(float x, float y) {
        // Deshabilitado
    }

    public void onTouchUp(float x, float y) {
        // Deshabilitado
    }

    /**
     * Alias para compatibilidad
     */
    public void onTouchRelease() {
        onTouchUp(0, 0);
    }

    /**
     * Normaliza el ángulo a rango 0-360
     */
    private float normalizeAngle(float angle) {
        while (angle < 0) angle += 360f;
        while (angle >= 360) angle -= 360f;
        return angle;
    }

    /**
     * Logea los ángulos actuales X, Y, Z
     */
    private void logAngles(String event) {
        Log.d(TAG, String.format("🔄 [%s] ÁNGULOS → X: %.1f° | Y: %.1f° | Z: %.1f°",
            event, rotationX, rotationY, rotationZ));
    }

    // Getters para los ángulos (útil para debug externo)
    public float getRotationX() { return rotationX; }
    public float getRotationY() { return rotationY; }
    public float getRotationZ() { return rotationZ; }

    public void release() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        Log.d(TAG, "🗑️ TravelingShip liberada");
    }
}
