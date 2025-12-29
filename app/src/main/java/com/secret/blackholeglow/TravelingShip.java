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
        "uniform float uTime;\n" +
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
        "uniform float uEngineGlow;\n" +
        "in vec2 vTexCoord;\n" +
        "in vec3 vPosition;\n" +
        "out vec4 fragColor;\n" +
        "\n" +
        "// Ruido simple para ondulación\n" +
        "float noise(float x) {\n" +
        "    return fract(sin(x * 12.9898) * 43758.5453);\n" +
        "}\n" +
        "\n" +
        "void main() {\n" +
        "    vec4 texColor = texture(uTexture, vTexCoord);\n" +
        "    \n" +
        "    // ═══════════════════════════════════════════════════════════\n" +
        "    // 🔥 DETECCIÓN DE LLAMAS (conos, X > 0.65)\n" +
        "    // ═══════════════════════════════════════════════════════════\n" +
        "    float flameArea = smoothstep(0.65, 0.75, vPosition.x);\n" +
        "    \n" +
        "    if (flameArea > 0.01) {\n" +
        "        // ═══════════════════════════════════════════════════════\n" +
        "        // 🔥 EFECTO PLASMA PARA LAS LLAMAS\n" +
        "        // ═══════════════════════════════════════════════════════\n" +
        "        \n" +
        "        // Distancia desde la base (0) hasta la punta (1)\n" +
        "        float distFromBase = (vPosition.x - 0.65) / 0.5;\n" +
        "        distFromBase = clamp(distFromBase, 0.0, 1.0);\n" +
        "        \n" +
        "        // Ondulación animada\n" +
        "        float wave1 = sin(vPosition.y * 20.0 + uTime * 10.0) * 0.5 + 0.5;\n" +
        "        float wave2 = sin(vPosition.z * 15.0 + uTime * 8.0) * 0.5 + 0.5;\n" +
        "        float wave3 = sin(distFromBase * 10.0 - uTime * 12.0) * 0.5 + 0.5;\n" +
        "        float flicker = (wave1 + wave2 + wave3) / 3.0;\n" +
        "        \n" +
        "        // Gradiente de colores: cyan (base) → naranja → amarillo (punta)\n" +
        "        vec3 cyanColor = vec3(0.2, 0.8, 1.0);\n" +
        "        vec3 orangeColor = vec3(1.0, 0.5, 0.1);\n" +
        "        vec3 yellowColor = vec3(1.0, 0.9, 0.3);\n" +
        "        \n" +
        "        vec3 flameColor;\n" +
        "        if (distFromBase < 0.5) {\n" +
        "            flameColor = mix(cyanColor, orangeColor, distFromBase * 2.0);\n" +
        "        } else {\n" +
        "            flameColor = mix(orangeColor, yellowColor, (distFromBase - 0.5) * 2.0);\n" +
        "        }\n" +
        "        \n" +
        "        // Añadir variación con ondulación\n" +
        "        flameColor = mix(flameColor, vec3(1.0, 1.0, 0.8), flicker * 0.3);\n" +
        "        \n" +
        "        // Brillo pulsante global\n" +
        "        float pulse = 0.8 + 0.2 * sin(uTime * 6.0);\n" +
        "        \n" +
        "        // Intensidad: más brillante en la base, se desvanece en la punta\n" +
        "        float intensity = (1.0 - distFromBase * 0.6) * pulse * uEngineGlow;\n" +
        "        \n" +
        "        // Transparencia: sólido en base, transparente en punta\n" +
        "        float alpha = 1.0 - distFromBase * 0.5;\n" +
        "        alpha *= (0.7 + flicker * 0.3);\n" +
        "        \n" +
        "        // Efecto de brillo (additive)\n" +
        "        flameColor *= intensity * 1.5;\n" +
        "        \n" +
        "        fragColor = vec4(flameColor, alpha);\n" +
        "    } else {\n" +
        "        // ═══════════════════════════════════════════════════════\n" +
        "        // 🚀 NAVE NORMAL (con glow sutil)\n" +
        "        // ═══════════════════════════════════════════════════════\n" +
        "        float engineArea = smoothstep(0.0, -0.5, vPosition.y);\n" +
        "        float pulse = 0.5 + 0.5 * sin(uTime * 8.0);\n" +
        "        vec3 glowColor = vec3(0.4, 0.7, 1.0) * engineArea * uEngineGlow * pulse * 0.3;\n" +
        "        fragColor = vec4(texColor.rgb + glowColor, texColor.a);\n" +
        "    }\n" +
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
    private static final float DRIFT_AMPLITUDE_X = 0.15f;
    private static final float DRIFT_SPEED_X = 0.6f;
    private static final float ROLL_FACTOR = 10f;
    private static final float PITCH_AMPLITUDE = 2.5f;

    // Parámetros de planeo en hover (MÁS DINÁMICO)
    private static final float HOVER_DRIFT_AMPLITUDE = 0.75f;  // Más desplazamiento lateral
    private static final float HOVER_DRIFT_SPEED = 0.5f;       // Un poco más rápido

    // Random para destinos
    private java.util.Random random = new java.util.Random();

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
     * Con banking realista - las alas se inclinan al moverse lateralmente
     */
    private void updateHovering(float deltaTime) {
        // Posición fija en origen
        z = ORIGIN_Z;
        scale = ORIGIN_SCALE;

        // Rotación mirando al horizonte
        rotationY = lerpAngle(rotationY, BASE_ROTATION_Y, deltaTime * 3f);

        // ═══════════════════════════════════════════════════════════════
        // 🛫 PLANEO CON ESTABILIZACIÓN REALISTA
        // ═══════════════════════════════════════════════════════════════

        // Drift lateral - movimiento principal
        float drift = (float)(
            Math.sin(time * HOVER_DRIFT_SPEED) * 0.7f +
            Math.sin(time * HOVER_DRIFT_SPEED * 1.5f) * 0.2f +
            Math.sin(time * HOVER_DRIFT_SPEED * 0.6f) * 0.1f
        );
        x = ORIGIN_X + drift * HOVER_DRIFT_AMPLITUDE;

        // Bobbing vertical + pequeñas turbulencias
        float bob = (float)(Math.sin(time * 1.2f) * 0.05f);
        float turbulenceY = (float)(Math.sin(time * 3.5f) * 0.015f);  // Micro-turbulencia
        y = ORIGIN_Y + bob + turbulenceY;

        // ═══════════════════════════════════════════════════════════════
        // ✈️ BANKING REALISTA - Alas se inclinan según dirección
        // ═══════════════════════════════════════════════════════════════

        // Velocidad del drift (derivada = coseno)
        float driftVelocity = (float)(
            Math.cos(time * HOVER_DRIFT_SPEED) * HOVER_DRIFT_SPEED * 0.7f +
            Math.cos(time * HOVER_DRIFT_SPEED * 1.5f) * HOVER_DRIFT_SPEED * 1.5f * 0.2f
        );

        // Roll principal - MUCHO más pronunciado (factor 2.5x)
        float mainRoll = driftVelocity * ROLL_FACTOR * 2.5f;

        // Micro-correcciones de estabilización (como si luchara contra el viento)
        float stabilization = (float)(
            Math.sin(time * 4.0f) * 3.0f +   // Corrección rápida
            Math.sin(time * 6.5f) * 1.5f     // Micro-ajuste
        );

        rotationZ = 4.0f + mainRoll + stabilization;

        // ═══════════════════════════════════════════════════════════════
        // 📐 PITCH - Nariz sube/baja con el planeo
        // ═══════════════════════════════════════════════════════════════

        // Pitch principal
        float mainPitch = (float)(Math.sin(time * 0.7f) * 3.5f);

        // Corrección de pitch (como si ajustara altitud)
        float pitchCorrection = (float)(Math.sin(time * 2.8f) * 2.0f);

        rotationX = mainPitch + pitchCorrection;

        // Engine glow suave (idle)
        engineGlow = 1.0f + 0.15f * (float) Math.sin(time * 4.0);
    }

    /**
     * Actualiza el movimiento en figura de 8
     * La nave traza un 8 vertical: sube hacia horizonte, hace loops, regresa
     */
    private void updateFigure8(float deltaTime) {
        float t = figure8Phase;
        float PI = (float) Math.PI;

        // ═══════════════════════════════════════════════════════════════
        // 🎱 TRAYECTORIA EN FIGURA DE 8
        // ═══════════════════════════════════════════════════════════════
        // X: oscila lateralmente 2 veces (crea los loops del 8)
        // Y: sube hacia horizonte y baja una vez
        // Z: hacia horizonte y regresa

        // Progreso vertical: sin(t/2) va de 0 → 1 → 0 cuando t va de 0 → 2π
        float verticalProgress = (float) Math.sin(t / 2.0f);

        // Loops laterales: sin(t) oscila completo, crea el cruce del 8
        float lateralLoop = (float) Math.sin(t);

        // Posición base con el 8
        x = ORIGIN_X + FIGURE_8_WIDTH * lateralLoop;
        y = ORIGIN_Y + FIGURE_8_HEIGHT * verticalProgress;
        z = ORIGIN_Z - FIGURE_8_DEPTH * verticalProgress;

        // Escala: más pequeña cuando está lejos (horizonte)
        float minScale = 0.15f;
        scale = ORIGIN_SCALE - (ORIGIN_SCALE - minScale) * verticalProgress;

        // ═══════════════════════════════════════════════════════════════
        // 🔄 ROTACIÓN - Siempre mirando hacia donde va
        // ═══════════════════════════════════════════════════════════════
        // Calcular dirección del movimiento (derivada de la posición)
        float dx = FIGURE_8_WIDTH * (float) Math.cos(t);         // derivada de sin(t)
        float dy = (FIGURE_8_HEIGHT / 2.0f) * (float) Math.cos(t / 2.0f);  // derivada de sin(t/2)

        // Ángulo basado en si sube o baja + dirección lateral
        // Cuando sube (dy > 0): mira al horizonte
        // Cuando baja (dy < 0): mira hacia cámara
        if (dy >= 0) {
            // Subiendo - mira al horizonte (rotación base)
            targetRotationY = BASE_ROTATION_Y;
        } else {
            // Bajando - mira hacia cámara (180° rotado)
            targetRotationY = BASE_ROTATION_Y + 180f;
        }
        rotationY = lerpAngle(rotationY, targetRotationY, deltaTime * 4f);

        // ═══════════════════════════════════════════════════════════════
        // ✈️ BANKING - Inclinación realista en las curvas
        // ═══════════════════════════════════════════════════════════════
        // Roll basado en movimiento lateral (dx)
        // Positivo = va a la derecha = inclinar derecha
        float rollIntensity = dx * 30f;  // Hasta 30° de inclinación

        // Añadir estabilización sutil
        float stabilization = (float)(
            Math.sin(time * 4.0f) * 2.0f +
            Math.sin(time * 7.0f) * 1.0f
        );
        rotationZ = 4.0f + rollIntensity + stabilization;

        // Pitch basado en si sube o baja
        float pitchFromClimb = dy * 8f;  // Nariz arriba al subir
        float pitchOsc = (float)(Math.sin(time * 2.0f) * 2.0f);
        rotationX = pitchFromClimb + pitchOsc;

        // ═══════════════════════════════════════════════════════════════
        // 🔥 ENGINE GLOW - Más intenso en las curvas
        // ═══════════════════════════════════════════════════════════════
        float curveIntensity = Math.abs(dx);  // Más intenso en los extremos del 8
        engineGlow = 1.3f + curveIntensity * 0.5f + 0.2f * (float) Math.sin(time * 5.0);
    }

    /**
     * Interpola ángulos correctamente (maneja el wrap de 360°)
     */
    private float lerpAngle(float from, float to, float t) {
        float diff = to - from;
        while (diff > 180f) diff -= 360f;
        while (diff < -180f) diff += 360f;
        return from + diff * Math.min(1f, t);
    }

    // Interpolación lineal
    private float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }

    // Curva ease-in-out para movimiento suave
    private float easeInOutCubic(float t) {
        return t < 0.5f
            ? 4f * t * t * t
            : 1f - (float)Math.pow(-2f * t + 2f, 3) / 2f;
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

        GLES30.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);
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
