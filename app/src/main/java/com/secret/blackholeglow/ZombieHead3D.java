package com.secret.blackholeglow;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
 * ║   🧟 ZombieHead3D - Cabeza zombi colgante para Walking Dead Scene        ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  CARACTERÍSTICAS:                                                         ║
 * ║  • Cabeza colgando de una cadena (modelo Meshy AI)                       ║
 * ║  • Balanceo suave como si colgara del viento                             ║
 * ║  • 📱 GIROSCOPIO: Movimiento sutil al inclinar el celular                ║
 * ║  • Proyección propia (no depende de CameraController)                    ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class ZombieHead3D implements SceneObject, CameraAware, SensorEventListener {
    private static final String TAG = "ZombieHead3D";

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

    // ═══════════════════════════════════════════════════════════════════════
    // 🧟 POSICIÓN Y TRANSFORMACIÓN - VALORES CALIBRADOS (2026-01-16)
    // ═══════════════════════════════════════════════════════════════════════

    private float x = -0.44f;     // Izquierda del centro
    private float y = 2.21f;      // Arriba (colgando)
    private float z = -1.94f;     // Profundidad calibrada
    private float scale = 0.77f;  // Tamaño calibrado

    // Rotación base
    private float rotationX = 0f;
    private float rotationY = 1f;     // Casi de frente
    private float rotationZ = 0f;

    // ═══════════════════════════════════════════════════════════════════════
    // 🔗 SISTEMA DE BALANCEO (como si colgara de una cadena)
    // ═══════════════════════════════════════════════════════════════════════

    private float swingPhase = 0f;
    private static final float SWING_SPEED = 0.8f;
    private static final float SWING_ANGLE_X = 5f;
    private static final float SWING_ANGLE_Z = 8f;

    // ═══════════════════════════════════════════════════════════════════════
    // 📱 GIROSCOPIO - Efecto PÉNDULO/LLAVERO orgánico
    // ═══════════════════════════════════════════════════════════════════════

    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private boolean gyroEnabled = true;

    // Valores del giroscopio (suavizados)
    private float gyroX = 0f;  // Inclinación adelante/atrás
    private float gyroY = 0f;  // Inclinación izquierda/derecha

    // 🔗 EFECTO PÉNDULO - Rotación sobre ejes (como cabeza colgando)
    private float pendulumRotX = 0f;      // Inclinación arriba/abajo (asentir)
    private float pendulumRotY = 0f;      // Giro izquierda/derecha (decir "no")
    private float pendulumRotZ = 0f;      // Ladeo lateral
    private float pendulumVelRotX = 0f;   // Velocidad rotación X
    private float pendulumVelRotY = 0f;   // Velocidad rotación Y
    private float pendulumRotVelZ = 0f;   // Velocidad rotación Z

    // Configuración del giroscopio - ULTRA RÁPIDO
    private static final float GYRO_MAX_ANGLE = 40f;      // Máximo ±40 grados
    private static final float GYRO_SMOOTHING = 0.6f;     // Casi sin suavizado
    private static final float GYRO_SENSITIVITY = 80f;    // Ultra sensible

    // 🔗 Configuración del GIRO LIBRE - Como trompo
    private static final float SPIN_FRICTION = 0.985f;     // Muy poca fricción (gira mucho tiempo)
    private static final float SPIN_SLOWDOWN = 2.0f;       // Desaceleración gradual

    // 👆 Configuración del TOUCH - Hacer girar la cabeza
    private static final float TOUCH_SPIN_FORCE = 1500.0f; // Fuerza de giro (muy fuerte)

    // Valores crudos del sensor (para suavizado)
    private float rawGyroX = 0f;
    private float rawGyroY = 0f;

    // ═══════════════════════════════════════════════════════════════════════
    // 🎮 SISTEMA DE CALIBRACIÓN POR TOUCH
    // ═══════════════════════════════════════════════════════════════════════

    public enum AdjustMode {
        POSITION_XY,   // Mover en X/Y
        POSITION_Z,    // Mover en Z (profundidad)
        ROTATE_Y,      // Girar horizontalmente
        SCALE          // Cambiar tamaño
    }

    private AdjustMode currentMode = AdjustMode.POSITION_XY;
    private boolean calibrationEnabled = false;  // 🔒 DESACTIVADO - posición fija

    private float lastTouchX = 0f;
    private float lastTouchY = 0f;
    private long touchStartTime = 0;
    private boolean isDragging = false;
    private static final long TAP_TIMEOUT = 250;
    private static final float DRAG_THRESHOLD = 0.02f;

    // Sensibilidad
    private static final float SENSITIVITY_POSITION = 3.0f;
    private static final float SENSITIVITY_Z = 5.0f;
    private static final float SENSITIVITY_ROTATION = 180.0f;
    private static final float SENSITIVITY_SCALE = 2.0f;

    // Tiempo para animaciones
    private float time = 0f;

    // Pantalla y proyección
    private int screenWidth = 1080;
    private int screenHeight = 1920;
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] vpMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // Cámara (opcional, usamos nuestra propia proyección)
    private CameraController camera;

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
        "// ═══════════════════════════════════════════════════════════════\n" +
        "// ⚡ CONSTANTES - Evitar cálculos repetidos\n" +
        "// ═══════════════════════════════════════════════════════════════\n" +
        "const float PI = 3.14159265;\n" +
        "const float TWO_PI = 6.28318530;\n" +
        "const vec3 ZOMBIE_TINT = vec3(0.75, 0.88, 0.65);\n" +
        "const vec3 EYE_COLOR = vec3(1.0, 0.1, 0.0);\n" +
        "const vec3 BLOOD_COLOR = vec3(0.6, 0.0, 0.0);\n" +
        "const vec3 VEIN_COLOR = vec3(0.4, 0.1, 0.15);\n" +
        "const vec3 RIM_COLOR = vec3(0.8, 0.2, 0.0);\n" +
        "const vec3 FOG_COLOR = vec3(0.2, 0.5, 0.2);\n" +
        "const vec3 GLITCH_COLOR = vec3(0.3, 0.5, 0.2);\n" +
        "\n" +
        "// Posiciones de ojos en UV (ajustar según textura)\n" +
        "const vec2 EYE_L = vec2(0.35, 0.55);\n" +
        "const vec2 EYE_R = vec2(0.65, 0.55);\n" +
        "\n" +
        "// ═══════════════════════════════════════════════════════════════\n" +
        "// 🩸 Función para gotas de sangre cayendo (OPTIMIZADA)\n" +
        "// ═══════════════════════════════════════════════════════════════\n" +
        "float bloodDrip(vec2 uv, float t) {\n" +
        "    float drip = 0.0;\n" +
        "    // Desenrollar loop para mejor rendimiento en GPU\n" +
        "    float yAnim0 = mod(t * 0.30, 1.5);\n" +
        "    float yAnim1 = mod(t * 0.45 + 0.5, 1.5);\n" +
        "    float yAnim2 = mod(t * 0.60 + 1.0, 1.5);\n" +
        "    \n" +
        "    // smoothstep CORRECTO: edge0 < edge1, luego invertir con 1.0 -\n" +
        "    float d0 = length(uv - vec2(0.30, 1.0 - yAnim0));\n" +
        "    float d1 = length(uv - vec2(0.55, 1.0 - yAnim1));\n" +
        "    float d2 = length(uv - vec2(0.80, 1.0 - yAnim2));\n" +
        "    \n" +
        "    drip += (1.0 - smoothstep(0.0, 0.03, d0)) * (1.0 - yAnim0 * 0.33);\n" +
        "    drip += (1.0 - smoothstep(0.0, 0.03, d1)) * (1.0 - yAnim1 * 0.33);\n" +
        "    drip += (1.0 - smoothstep(0.0, 0.03, d2)) * (1.0 - yAnim2 * 0.33);\n" +
        "    \n" +
        "    return drip;\n" +
        "}\n" +
        "\n" +
        "void main() {\n" +
        "    vec4 texColor = texture(uTexture, vTexCoord);\n" +
        "    \n" +
        "    // ⚡ Tiempo normalizado para diferentes velocidades de animación\n" +
        "    // Usar fract() para ciclos cortos, mod() para ciclos más largos\n" +
        "    float tSlow = uTime;           // Para efectos lentos (rim, fog)\n" +
        "    float tMed = mod(uTime, 10.0); // Para efectos medios (venas, pulso)\n" +
        "    float tFast = mod(uTime, 2.0); // Para efectos rápidos (glitch)\n" +
        "    \n" +
        "    // ═══════════════════════════════════════════════════\n" +
        "    // 🧟 TONO ZOMBI - Verde enfermizo\n" +
        "    // ═══════════════════════════════════════════════════\n" +
        "    vec3 color = texColor.rgb * ZOMBIE_TINT;\n" +
        "    \n" +
        "    // ═══════════════════════════════════════════════════\n" +
        "    // 👁️ OJOS BRILLANTES - Rojo demoniaco\n" +
        "    // ═══════════════════════════════════════════════════\n" +
        "    float dL = length(vTexCoord - EYE_L);\n" +
        "    float dR = length(vTexCoord - EYE_R);\n" +
        "    // smoothstep CORRECTO: 0.02 < 0.08, invertir resultado\n" +
        "    float eyeGlow = (1.0 - smoothstep(0.02, 0.08, dL)) + \n" +
        "                    (1.0 - smoothstep(0.02, 0.08, dR));\n" +
        "    float eyePulse = sin(tMed * 4.0) * 0.3 + 0.7;\n" +
        "    color += EYE_COLOR * eyeGlow * eyePulse * 0.8;\n" +
        "    \n" +
        "    // ═══════════════════════════════════════════════════\n" +
        "    // 🩸 SANGRE GOTEANDO\n" +
        "    // ═══════════════════════════════════════════════════\n" +
        "    float blood = bloodDrip(vTexCoord, tSlow);\n" +
        "    color = mix(color, BLOOD_COLOR, blood * 0.7);\n" +
        "    \n" +
        "    // ═══════════════════════════════════════════════════\n" +
        "    // 💀 VENAS PULSANTES\n" +
        "    // ═══════════════════════════════════════════════════\n" +
        "    float veinPulse = sin(tMed * 3.0 + vTexCoord.y * 10.0) * 0.5 + 0.5;\n" +
        "    // smoothstep CORRECTO: 0.4 < 0.6 ✓\n" +
        "    float veinMask = smoothstep(0.4, 0.6, texColor.r - texColor.g);\n" +
        "    color += VEIN_COLOR * veinPulse * veinMask * 0.3;\n" +
        "    \n" +
        "    // ═══════════════════════════════════════════════════\n" +
        "    // ⚡ EFECTO GLITCH/TWITCHING (con tiempo acotado)\n" +
        "    // ═══════════════════════════════════════════════════\n" +
        "    // Usar tFast para evitar overflow en sin()\n" +
        "    float glitchSeed = tFast * 25.0 + vPosition.y * 50.0;\n" +
        "    float glitch = step(0.97, fract(sin(glitchSeed) * 43758.5));\n" +
        "    color += GLITCH_COLOR * glitch * 0.4;\n" +
        "    \n" +
        "    // ═══════════════════════════════════════════════════\n" +
        "    // 🔥 RIM LIGHT INFERNAL (luz desde abajo)\n" +
        "    // ═══════════════════════════════════════════════════\n" +
        "    // smoothstep CORRECTO: -0.3 < 0.3 ✓\n" +
        "    float rimBottom = 1.0 - smoothstep(-0.3, 0.3, vPosition.y);\n" +
        "    float rimPulse = sin(tMed * 2.5) * 0.2 + 0.8;\n" +
        "    color += RIM_COLOR * rimBottom * rimPulse * 0.4;\n" +
        "    \n" +
        "    // ═══════════════════════════════════════════════════\n" +
        "    // 🌫️ NIEBLA VERDE FANTASMAL\n" +
        "    // ═══════════════════════════════════════════════════\n" +
        "    float fogNoise = sin(vPosition.x * 5.0 + tSlow) * \n" +
        "                     sin(vPosition.z * 5.0 + tSlow * 0.7);\n" +
        "    // smoothstep CORRECTO: 0.0 < 0.5 ✓\n" +
        "    float fog = smoothstep(0.0, 0.5, fogNoise * 0.5 + 0.5) * 0.15;\n" +
        "    color = mix(color, FOG_COLOR, fog);\n" +
        "    \n" +
        "    // Oscurecer ligeramente para ambiente tenebroso\n" +
        "    color *= 0.95;\n" +
        "    \n" +
        "    fragColor = vec4(color, texColor.a);\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════

    public ZombieHead3D(Context context, TextureLoader textureLoader) {
        this.context = context;
        this.textureLoader = textureLoader;

        setupProjection();
        loadModel();
        loadTexture();
        compileShader();
        setupGyroscope();

        Log.d(TAG, "🧟 ZombieHead3D creado - POSICIÓN FIJA + EFECTOS + GIROSCOPIO");
        Log.d(TAG, "📍 Posición: x=" + x + " y=" + y + " z=" + z + " scale=" + scale);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 📱 INICIALIZACIÓN DEL GIROSCOPIO
    // ═══════════════════════════════════════════════════════════════════════

    private void setupGyroscope() {
        try {
            sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager != null) {
                // Preferir ROTATION_VECTOR (más estable) sobre GYROSCOPE
                rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
                if (rotationSensor == null) {
                    // Fallback a GAME_ROTATION_VECTOR
                    rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
                }
                if (rotationSensor != null) {
                    // ⚡ SENSOR_DELAY_GAME (~20ms/50Hz) para respuesta rápida
                    sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
                    Log.d(TAG, "📱 Giroscopio activado (GAME): " + rotationSensor.getName());
                } else {
                    gyroEnabled = false;
                    Log.w(TAG, "⚠️ Giroscopio no disponible en este dispositivo");
                }
            }
        } catch (Exception e) {
            gyroEnabled = false;
            Log.e(TAG, "❌ Error configurando giroscopio: " + e.getMessage());
        }
    }

    private void setupProjection() {
        // View matrix - cámara mirando hacia -Z
        Matrix.setLookAtM(viewMatrix, 0,
            0f, 0f, 3f,    // Posición cámara
            0f, 0f, 0f,    // Mirando a
            0f, 1f, 0f);   // Up vector

        // Projection matrix
        float ratio = (float) screenWidth / screenHeight;
        Matrix.perspectiveM(projectionMatrix, 0, 60f, ratio, 0.1f, 100f);

        // VP matrix
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
    }

    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        setupProjection();
        Log.d(TAG, "📐 Screen size: " + width + "x" + height);
    }

    private void loadModel() {
        try {
            Log.d(TAG, "📦 Cargando zombie_head.obj...");

            ObjLoader.Mesh mesh;
            ModelDownloadManager modelMgr = ModelDownloadManager.getInstance(context);
            String modelPath = modelMgr.getModelPath("zombie_head.obj");

            if (modelPath != null) {
                Log.d(TAG, "🌐 Cargando modelo desde: " + modelPath);
                mesh = ObjLoader.loadObjFromFile(modelPath, true);
            } else {
                Log.e(TAG, "❌ Modelo no disponible: zombie_head.obj");
                return;
            }

            Log.d(TAG, "✓ Modelo cargado: " + mesh.vertexCount + " vértices");

            this.vertexBuffer = mesh.vertexBuffer;
            this.uvBuffer = mesh.uvBuffer;

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
        String texturePath = imageMgr.getImagePath("zombie_head_texture.png");

        if (texturePath != null && textureLoader instanceof TextureManager) {
            Log.d(TAG, "🌐 Cargando textura desde: " + texturePath);
            textureId = ((TextureManager) textureLoader).loadTextureFromFile(texturePath);
        } else {
            Log.e(TAG, "❌ Textura no disponible");
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
        uModelMatrixHandle = GLES30.glGetUniformLocation(shaderProgram, "uModelMatrix");
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

    // Constante para ciclo de tiempo (2π ≈ 6.28, usamos 100 para más margen)
    private static final float TIME_CYCLE = 100.0f;

    @Override
    public void update(float deltaTime) {
        // ⚡ OPTIMIZACIÓN: Mantener time en rango [0, TIME_CYCLE] para precisión float
        time += deltaTime;
        if (time > TIME_CYCLE) {
            time -= TIME_CYCLE;
        }

        swingPhase += deltaTime * SWING_SPEED;
        if (swingPhase > TIME_CYCLE) {
            swingPhase -= TIME_CYCLE;
        }

        // 📱 Suavizar valores del giroscopio (si está disponible)
        if (gyroEnabled) {
            gyroX += (rawGyroX - gyroX) * GYRO_SMOOTHING;
            gyroY += (rawGyroY - gyroY) * GYRO_SMOOTHING;

            // El giroscopio también puede hacer girar la cabeza
            pendulumVelRotY += gyroY * 2.0f;
        }

        // ═══════════════════════════════════════════════════════════════
        // 🔗 FÍSICA DE GIRO LIBRE - Como trompo (sin límites)
        // ═══════════════════════════════════════════════════════════════

        // Aplicar velocidad a la rotación (GIRO LIBRE - puede dar vueltas completas)
        pendulumRotY += pendulumVelRotY * deltaTime;

        // Mantener el ángulo en rango 0-360 para evitar overflow
        if (pendulumRotY > 360f) pendulumRotY -= 360f;
        if (pendulumRotY < -360f) pendulumRotY += 360f;

        // Fricción gradual (la cabeza va frenando poco a poco)
        pendulumVelRotY *= SPIN_FRICTION;

        // Si la velocidad es muy baja, detener completamente
        if (Math.abs(pendulumVelRotY) < 0.1f) {
            pendulumVelRotY = 0f;
        }
    }

    @Override
    public void draw() {
        if (vertexBuffer == null || indexBuffer == null || textureId == 0) {
            return;
        }

        GLES30.glUseProgram(shaderProgram);

        // Calcular balanceo suave (animación automática)
        float swingX = (float) Math.sin(swingPhase) * SWING_ANGLE_X;
        float swingZ = (float) Math.sin(swingPhase * 0.7f) * SWING_ANGLE_Z;

        // Model matrix con GIRO LIBRE por touch
        Matrix.setIdentityM(modelMatrix, 0);

        // Posición fija
        Matrix.translateM(modelMatrix, 0, x, y, z);

        // 🔄 ROTACIÓN PRINCIPAL: base + giro del touch (puede dar vueltas completas)
        Matrix.rotateM(modelMatrix, 0, rotationY + pendulumRotY, 0f, 1f, 0f);

        // Balanceo suave automático (como si colgara)
        Matrix.rotateM(modelMatrix, 0, rotationX + swingX, 1f, 0f, 0f);
        Matrix.rotateM(modelMatrix, 0, rotationZ + swingZ, 0f, 0f, 1f);

        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

        // MVP matrix (usar nuestra propia proyección)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);

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
    // 🎮 SISTEMA DE CALIBRACIÓN POR TOUCH
    // ═══════════════════════════════════════════════════════════════════════

    public boolean onTouchEvent(float normalizedX, float normalizedY, int action) {
        // 👆 SIEMPRE procesar touch para empujar la cabeza
        Log.d(TAG, "👆 TOUCH recibido: nx=" + normalizedX + " ny=" + normalizedY + " action=" + action);

        switch (action) {
            case android.view.MotionEvent.ACTION_DOWN:
                lastTouchX = normalizedX;
                lastTouchY = normalizedY;
                touchStartTime = System.currentTimeMillis();
                isDragging = false;
                return true;

            case android.view.MotionEvent.ACTION_MOVE:
                float deltaX = normalizedX - lastTouchX;

                // 👆 GIRAR LA CABEZA con el deslizamiento horizontal
                if (Math.abs(deltaX) > 0.001f) {
                    // Deslizar a la derecha = girar a la derecha (positivo)
                    // Deslizar a la izquierda = girar a la izquierda (negativo)
                    pendulumVelRotY += deltaX * TOUCH_SPIN_FORCE;

                    Log.d(TAG, "🔄 GIRO: velRotY=" + pendulumVelRotY + " rotY=" + pendulumRotY);

                    isDragging = true;
                    lastTouchX = normalizedX;
                    lastTouchY = normalizedY;
                }
                return true;

            case android.view.MotionEvent.ACTION_UP:
                // Al soltar, la cabeza sigue con momentum
                return true;
        }
        return false;
    }

    private void applyAdjustment(float deltaX, float deltaY) {
        switch (currentMode) {
            case POSITION_XY:
                x += deltaX * SENSITIVITY_POSITION;
                y += deltaY * SENSITIVITY_POSITION;
                break;
            case POSITION_Z:
                z += deltaY * SENSITIVITY_Z;
                break;
            case ROTATE_Y:
                rotationY += deltaX * SENSITIVITY_ROTATION;
                break;
            case SCALE:
                scale += deltaY * SENSITIVITY_SCALE;
                scale = Math.max(0.05f, Math.min(2.0f, scale));
                break;
        }
        logCurrentState();
    }

    private void cycleMode() {
        AdjustMode[] modes = AdjustMode.values();
        int nextIndex = (currentMode.ordinal() + 1) % modes.length;
        currentMode = modes[nextIndex];
        Log.d(TAG, "🎮 MODO CAMBIADO: " + currentMode.name());
        logCurrentState();
    }

    private void logCurrentState() {
        Log.d(TAG, String.format("🧟 CALIBRACIÓN [%s] x=%.2f y=%.2f z=%.2f scale=%.2f rotY=%.1f",
            currentMode.name(), x, y, z, scale, rotationY));
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

    public void setCalibrationEnabled(boolean enabled) {
        this.calibrationEnabled = enabled;
        Log.d(TAG, "🎮 Calibración: " + (enabled ? "ACTIVADA" : "DESACTIVADA"));
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 📱 SENSOR EVENT LISTENER - Giroscopio
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!gyroEnabled || event.sensor.getType() != Sensor.TYPE_ROTATION_VECTOR &&
            event.sensor.getType() != Sensor.TYPE_GAME_ROTATION_VECTOR) {
            return;
        }

        // Obtener quaternion de rotación
        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);

        // Obtener ángulos de orientación
        float[] orientation = new float[3];
        SensorManager.getOrientation(rotationMatrix, orientation);

        // orientation[0] = azimuth (no lo usamos)
        // orientation[1] = pitch (inclinación adelante/atrás)
        // orientation[2] = roll (inclinación lateral)

        // Convertir a grados y aplicar sensibilidad
        float pitch = (float) Math.toDegrees(orientation[1]) * (GYRO_SENSITIVITY / 90f);
        float roll = (float) Math.toDegrees(orientation[2]) * (GYRO_SENSITIVITY / 90f);

        // Limitar a ángulo máximo
        rawGyroX = Math.max(-GYRO_MAX_ANGLE, Math.min(GYRO_MAX_ANGLE, pitch));
        rawGyroY = Math.max(-GYRO_MAX_ANGLE, Math.min(GYRO_MAX_ANGLE, roll));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No necesitamos manejar cambios de precisión
    }

    /**
     * Pausar el giroscopio (llamar cuando el wallpaper no es visible)
     */
    public void pauseGyroscope() {
        if (sensorManager != null && rotationSensor != null) {
            sensorManager.unregisterListener(this);
            Log.d(TAG, "📱 Giroscopio pausado");
        }
    }

    /**
     * Reanudar el giroscopio (llamar cuando el wallpaper vuelve a ser visible)
     */
    public void resumeGyroscope() {
        if (sensorManager != null && rotationSensor != null && gyroEnabled) {
            // ⚡ SENSOR_DELAY_GAME para respuesta rápida
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
            Log.d(TAG, "📱 Giroscopio reanudado (SENSOR_DELAY_GAME)");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════════════

    public void dispose() {
        // 📱 Desregistrar el sensor del giroscopio
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            sensorManager = null;
            rotationSensor = null;
            Log.d(TAG, "📱 Giroscopio desregistrado");
        }

        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        if (textureId != 0) {
            int[] textures = {textureId};
            GLES30.glDeleteTextures(1, textures, 0);
            textureId = 0;
        }
        Log.d(TAG, "🧟 ZombieHead3D disposed");
    }
}
