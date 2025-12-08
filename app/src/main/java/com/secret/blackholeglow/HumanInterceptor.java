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
import java.util.ArrayList;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   ✈️ HUMAN INTERCEPTOR - Caza Interceptor Humano                         ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  CARACTERÍSTICAS:                                                         ║
 * ║  • Rápido y ágil - el más veloz de la batalla                            ║
 * ║  • Alas delta con acentos rojos                                          ║
 * ║  • Disparo rápido (ráfagas)                                              ║
 * ║  • 4 HP (frágil pero letal)                                              ║
 * ║  • Objetivo primario: UfoAttacker                                        ║
 * ║  • Objetivo secundario: UfoScout                                         ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class HumanInterceptor implements SceneObject, CameraAware {
    private static final String TAG = "HumanInterceptor";

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
    private int uAlphaHandle;
    private int uBoostHandle;

    // Transformación
    public float x, y, z;
    private float scale;
    private float rotationY = 0f;
    private float tiltX = 0f;
    private float tiltZ = 0f;

    // ═══════════════════════════════════════════════════════════════════════
    // 🧠 IA DE MOVIMIENTO ÁGIL Y RÁPIDO
    // ═══════════════════════════════════════════════════════════════════════
    private float velocityX = 0f, velocityY = 0f, velocityZ = 0f;
    private float targetX, targetY, targetZ;
    private float baseScale;

    // Movimiento MUY rápido
    private float moveSpeed = 1.2f;
    private float maxSpeed = 2.5f;
    private float acceleration = 2.5f;
    private float deceleration = 0.92f;

    // Timers
    private float wanderTimer = 0f;
    private float wanderInterval = 2.0f;
    private float curvePhase = 0f;
    private float curveAmplitude = 0.4f;
    private float curveFrequency = 2.0f;

    // Estados - AMPLIADOS para exploración libre
    public enum State {
        FLANKING,    // Maniobras de flanqueo
        ATTACKING,   // Ataque activo
        EVADING,     // Evadiendo
        PURSUING,    // Persiguiendo
        EXPLORING,   // 🌌 Explorando la escena libremente
        PATROLLING   // 🛸 Patrullando puntos de interés
    }
    private State currentState = State.EXPLORING;

    // ═══════════════════════════════════════════════════════════════════════
    // 🎯 SISTEMA DE OBJETIVOS (Primario y Secundario)
    // ═══════════════════════════════════════════════════════════════════════
    private UfoAttacker primaryTarget = null;   // Objetivo principal
    private UfoScout secondaryTarget = null;    // Objetivo secundario
    private float attackRange = 15f;    // Rango aumentado para más agresividad
    private float evadeRange = 0.6f;    // Evade cuando está muy cerca

    // ═══════════════════════════════════════════════════════════════════════
    // 🚧 REFERENCIAS A OTRAS NAVES (para evitar colisiones)
    // ═══════════════════════════════════════════════════════════════════════
    private DefenderShip allyDefender = null;
    private float safeDistance = 1.2f;  // Distancia mínima entre naves

    // ═══════════════════════════════════════════════════════════════════════
    // 💔 SISTEMA DE VIDA (Frágil pero rápido)
    // ═══════════════════════════════════════════════════════════════════════
    private int health = 4;
    private int maxHealth = 4;
    private boolean destroyed = false;
    private float respawnTimer = 0f;
    private float respawnDelay = 7.0f;
    private float invincibilityTimer = 0f;
    private float invincibilityDuration = 1.2f;

    // Cámara
    private CameraController camera;

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    // Pre-asignadas para drawTeleportParticles (evita GC en draw loop)
    private final float[] teleportParticleModel = new float[16];
    private final float[] teleportParticleMvp = new float[16];

    // Tiempo
    private float timeAccumulator = 0f;

    // ═══════════════════════════════════════════════════════════════════════
    // 🌌 LÍMITES DE MOVIMIENTO EXPANDIDOS - TODA LA ESCENA
    // ═══════════════════════════════════════════════════════════════════════
    private static final float BOUND_X = 3.5f;        // Más ancho
    private static final float BOUND_Y_MIN = -2.8f;   // ¡Hasta el ecualizador!
    private static final float BOUND_Y_MAX = 3.5f;    // Hasta arriba
    private static final float BOUND_Z_MIN = -4.0f;   // Más profundidad
    private static final float BOUND_Z_MAX = 3.0f;    // Más cerca de cámara

    // ═══════════════════════════════════════════════════════════════════════
    // 🎯 PUNTOS DE INTERÉS (POIs) - Lugares a explorar
    // ═══════════════════════════════════════════════════════════════════════
    private static final float[][] POINTS_OF_INTEREST = {
        // {x, y, z, nombre}
        {-3.5f, 2.0f, -2.0f},    // 🛰️ Cerca de la estación espacial
        {3.0f, 2.5f, -1.5f},     // ☀️ Cerca del sol
        {0.0f, 0.5f, 0.0f},      // 🌍 Cerca de la Tierra
        {-2.5f, -2.0f, 1.0f},    // 🎵 Zona del ecualizador (izquierda)
        {2.5f, -2.0f, 1.0f},     // 🎵 Zona del ecualizador (derecha)
        {0.0f, -2.5f, 2.0f},     // 🎵 Centro del ecualizador (frente)
        {-3.0f, 0.0f, -3.0f},    // 🌌 Galaxias lejanas (izquierda)
        {3.0f, 0.0f, -3.0f},     // 🌌 Galaxias lejanas (derecha)
        {0.0f, 3.0f, 0.0f},      // ⬆️ Zona superior central
        {-2.0f, 1.5f, 2.0f},     // 📍 Punto cercano izquierda
        {2.0f, 1.5f, 2.0f},      // 📍 Punto cercano derecha
    };
    private int currentPOI = 0;
    private float exploreTimer = 0f;
    private float exploreInterval = 4.0f;  // Cambiar POI cada 4 segundos
    private float combatDisengageTimer = 0f;
    private static final float COMBAT_DISENGAGE_TIME = 8.0f;  // Desengancharse del combate cada 8s

    // ═══════════════════════════════════════════════════════════════════════
    // 🔫 SISTEMA DE DISPAROS (TEAM HUMAN - LÁSER CYAN RÁPIDO)
    // ═══════════════════════════════════════════════════════════════════════
    private static final int MAX_LASERS = 10;
    private final List<Laser> lasers = new ArrayList<>();
    private float shootTimer = 0f;
    private float shootInterval = 0.8f;
    private static final float MIN_SHOOT_INTERVAL = 0.5f;
    private static final float MAX_SHOOT_INTERVAL = 1.0f;

    // ═══════════════════════════════════════════════════════════════════════
    // 🚀 BOOST DE VELOCIDAD (Afterburner)
    // ═══════════════════════════════════════════════════════════════════════
    private boolean boostActive = false;
    private float boostTimer = 0f;
    private float boostDuration = 3.0f;
    private float boostCooldown = 0f;
    private float boostCooldownMax = 10.0f;
    private float boostIntensity = 0f;
    private static final float BOOST_SPEED_MULT = 1.8f;

    // ═══════════════════════════════════════════════════════════════════════
    // ✨ HABILIDAD ESPECIAL 1: TELETRANSPORTE TÁCTICO
    // ═══════════════════════════════════════════════════════════════════════
    private boolean teleportAvailable = true;
    private float teleportCooldown = 0f;
    private float teleportCooldownMax = 10.0f;
    private boolean teleportEffectActive = false;
    private float teleportEffectTimer = 0f;
    private float teleportEffectDuration = 0.5f;
    private float teleportOriginX, teleportOriginY, teleportOriginZ;
    private float teleportDestX, teleportDestY, teleportDestZ;

    // Shader para efecto de teletransporte
    private int teleportProgram = 0;
    private int teleportAPositionHandle;
    private int teleportUMVPHandle;
    private int teleportUTimeHandle;
    private int teleportUColorHandle;
    private FloatBuffer teleportParticleBuffer;
    private static final int TELEPORT_PARTICLES = 32;

    // ═══════════════════════════════════════════════════════════════════════
    // 🔥 HABILIDAD ESPECIAL 2: RÁFAGA TRIPLE
    // ═══════════════════════════════════════════════════════════════════════
    private boolean tripleBurstActive = false;
    private float tripleBurstTimer = 0f;
    private float tripleBurstDuration = 5.0f;
    private float tripleBurstCooldown = 0f;
    private float tripleBurstCooldownMax = 12.0f;

    /**
     * Constructor
     */
    public HumanInterceptor(Context context, TextureLoader textureLoader,
                            float x, float y, float z, float scale) {
        this.context = context;
        this.textureLoader = textureLoader;
        this.x = x;
        this.y = y;
        this.z = z;
        this.scale = scale;
        this.baseScale = scale;
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Log.d(TAG, "✈️ Creando Human Interceptor");
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        loadModel();
        loadTexture();
        createShaders();
        initLasers();
        initTeleportEffect();

        Log.d(TAG, "✅ Human Interceptor creado en (" + x + ", " + y + ", " + z + ")");
    }

    private void initLasers() {
        for (int i = 0; i < MAX_LASERS; i++) {
            lasers.add(new Laser(Laser.TEAM_HUMAN));
        }
        shootInterval = MIN_SHOOT_INTERVAL + (float)(Math.random() * (MAX_SHOOT_INTERVAL - MIN_SHOOT_INTERVAL));
        Log.d(TAG, "🔫 Sistema de láseres cyan inicializado");
    }

    /**
     * ✨ Inicializar efectos visuales de teletransporte
     */
    private void initTeleportEffect() {
        // Crear shader para partículas de teletransporte
        String vertexShader =
            "attribute vec4 a_Position;\n" +
            "uniform mat4 u_MVPMatrix;\n" +
            "uniform float u_Time;\n" +
            "void main() {\n" +
            "    vec4 pos = a_Position;\n" +
            "    // Partículas se expanden hacia afuera\n" +
            "    float expansion = u_Time * 2.0;\n" +
            "    pos.xyz += normalize(pos.xyz) * expansion;\n" +
            "    gl_Position = u_MVPMatrix * pos;\n" +
            "    gl_PointSize = 12.0 * (1.0 - u_Time);\n" +
            "}";

        String fragmentShader =
            "precision mediump float;\n" +
            "uniform vec4 u_Color;\n" +
            "uniform float u_Time;\n" +
            "void main() {\n" +
            "    vec2 coord = gl_PointCoord - vec2(0.5);\n" +
            "    float dist = length(coord);\n" +
            "    if (dist > 0.5) discard;\n" +
            "    float alpha = (1.0 - u_Time) * (1.0 - dist * 2.0);\n" +
            "    gl_FragColor = vec4(u_Color.rgb, u_Color.a * alpha);\n" +
            "}";

        int vs = compileShader(GLES30.GL_VERTEX_SHADER, vertexShader);
        int fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShader);

        teleportProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(teleportProgram, vs);
        GLES30.glAttachShader(teleportProgram, fs);
        GLES30.glLinkProgram(teleportProgram);

        teleportAPositionHandle = GLES30.glGetAttribLocation(teleportProgram, "a_Position");
        teleportUMVPHandle = GLES30.glGetUniformLocation(teleportProgram, "u_MVPMatrix");
        teleportUTimeHandle = GLES30.glGetUniformLocation(teleportProgram, "u_Time");
        teleportUColorHandle = GLES30.glGetUniformLocation(teleportProgram, "u_Color");

        // Crear buffer de partículas (esfera de partículas)
        float[] particles = new float[TELEPORT_PARTICLES * 3];
        for (int i = 0; i < TELEPORT_PARTICLES; i++) {
            double theta = Math.random() * Math.PI * 2;
            double phi = Math.acos(2 * Math.random() - 1);
            float r = 0.3f;
            particles[i * 3] = (float)(r * Math.sin(phi) * Math.cos(theta));
            particles[i * 3 + 1] = (float)(r * Math.sin(phi) * Math.sin(theta));
            particles[i * 3 + 2] = (float)(r * Math.cos(phi));
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(particles.length * 4);
        bb.order(ByteOrder.nativeOrder());
        teleportParticleBuffer = bb.asFloatBuffer();
        teleportParticleBuffer.put(particles);
        teleportParticleBuffer.position(0);

        Log.d(TAG, "✨ Efecto de teletransporte inicializado");
    }

    private void loadModel() {
        try {
            Log.d(TAG, "📦 Cargando human_interceptor.obj...");

            ObjLoader.Mesh mesh = ObjLoader.loadObj(context, "human_interceptor.obj");

            Log.d(TAG, "✓ Modelo cargado:");
            Log.d(TAG, "  Vértices: " + mesh.vertexCount);
            Log.d(TAG, "  Caras: " + mesh.faces.size());

            this.vertexBuffer = mesh.vertexBuffer;
            this.uvBuffer = mesh.uvBuffer;

            // Construir índices
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

            Log.d(TAG, "  Índices: " + indexCount);

        } catch (IOException e) {
            Log.e(TAG, "❌ Error cargando human_interceptor.obj", e);
            throw new RuntimeException(e);
        }
    }

    private void loadTexture() {
        Log.d(TAG, "🎨 Cargando textura human_interceptor_texture...");

        int textureResId = context.getResources().getIdentifier(
                "human_interceptor_texture", "drawable", context.getPackageName());

        if (textureResId != 0) {
            textureId = textureLoader.getTexture(textureResId);
            Log.d(TAG, "✓ Textura cargada (ID=" + textureId + ")");
        } else {
            Log.e(TAG, "❌ Textura no encontrada: human_interceptor_texture");
            textureId = 0;
        }
    }

    /**
     * ✈️ SHADER CON EFECTOS DE MOTOR Y BOOST
     */
    private void createShaders() {
        Log.d(TAG, "🎨 Creando shaders con efectos de afterburner...");

        String vertexShaderCode =
                "attribute vec4 a_Position;\n" +
                "attribute vec2 a_TexCoord;\n" +
                "uniform mat4 u_MVPMatrix;\n" +
                "varying vec2 v_TexCoord;\n" +
                "varying vec3 v_LocalPos;\n" +
                "void main() {\n" +
                "  gl_Position = u_MVPMatrix * a_Position;\n" +
                "  v_TexCoord = a_TexCoord;\n" +
                "  v_LocalPos = a_Position.xyz;\n" +
                "}";

        // Fragment Shader con efectos de motor y boost
        String fragmentShaderCode =
                "precision mediump float;\n" +
                "\n" +
                "uniform sampler2D u_Texture;\n" +
                "uniform float u_Time;\n" +
                "uniform float u_Alpha;\n" +
                "uniform float u_Boost;\n" +
                "\n" +
                "varying vec2 v_TexCoord;\n" +
                "varying vec3 v_LocalPos;\n" +
                "\n" +
                "void main() {\n" +
                "    vec2 uv = vec2(v_TexCoord.x, 1.0 - v_TexCoord.y);\n" +
                "    vec4 texColor = texture2D(u_Texture, uv);\n" +
                "    vec3 color = texColor.rgb;\n" +
                "    \n" +
                "    // ═══ ✈️ DETECTAR ZONAS ROJAS (acentos) ═══\n" +
                "    float redAmount = texColor.r - max(texColor.g, texColor.b);\n" +
                "    float isAccent = smoothstep(0.2, 0.4, redAmount);\n" +
                "    \n" +
                "    // Pulso en acentos rojos\n" +
                "    if (isAccent > 0.1) {\n" +
                "        float pulse = sin(u_Time * 4.0) * 0.2 + 0.8;\n" +
                "        color.r += 0.2 * pulse * isAccent;\n" +
                "    }\n" +
                "    \n" +
                "    // ═══ 🔥 EFECTO DE MOTOR TRASERO ═══\n" +
                "    // Detectar zona trasera (z negativo en el modelo)\n" +
                "    float engineGlow = smoothstep(-0.8, -0.3, v_LocalPos.z);\n" +
                "    engineGlow *= smoothstep(0.3, 0.0, abs(v_LocalPos.y));\n" +
                "    \n" +
                "    // Brillo cyan del motor\n" +
                "    float enginePulse = sin(u_Time * 8.0) * 0.3 + 0.7;\n" +
                "    vec3 engineColor = vec3(0.3, 0.8, 1.0) * engineGlow * enginePulse * 0.5;\n" +
                "    color += engineColor;\n" +
                "    \n" +
                "    // ═══ 💫 RIM LIGHT SUTIL ═══\n" +
                "    float rim = 1.0 - abs(v_LocalPos.y);\n" +
                "    rim = pow(rim, 3.0);\n" +
                "    color += vec3(0.2, 0.4, 0.5) * rim * 0.2;\n" +
                "    \n" +
                "    // ═══ 🚀 BOOST AFTERBURNER ═══\n" +
                "    if (u_Boost > 0.01) {\n" +
                "        // Motor INTENSO durante boost\n" +
                "        vec3 boostColor = vec3(0.5, 0.9, 1.0) * u_Boost;\n" +
                "        color += boostColor * engineGlow * 2.0;\n" +
                "        \n" +
                "        // Brillo general del casco\n" +
                "        float boostPulse = sin(u_Time * 15.0) * 0.2 + 0.8;\n" +
                "        color += vec3(0.1, 0.3, 0.4) * u_Boost * boostPulse;\n" +
                "        \n" +
                "        // Estela de energía\n" +
                "        float trail = smoothstep(-1.0, 0.0, v_LocalPos.z) * u_Boost;\n" +
                "        color += vec3(0.2, 0.6, 0.8) * trail * 0.3;\n" +
                "    }\n" +
                "    \n" +
                "    gl_FragColor = vec4(color, texColor.a * u_Alpha);\n" +
                "}";

        int vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode);

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vertexShader);
        GLES30.glAttachShader(shaderProgram, fragmentShader);
        GLES30.glLinkProgram(shaderProgram);

        aPositionHandle = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        aTexCoordHandle = GLES30.glGetAttribLocation(shaderProgram, "a_TexCoord");
        uMVPMatrixHandle = GLES30.glGetUniformLocation(shaderProgram, "u_MVPMatrix");
        uTextureHandle = GLES30.glGetUniformLocation(shaderProgram, "u_Texture");
        uTimeHandle = GLES30.glGetUniformLocation(shaderProgram, "u_Time");
        uAlphaHandle = GLES30.glGetUniformLocation(shaderProgram, "u_Alpha");
        uBoostHandle = GLES30.glGetUniformLocation(shaderProgram, "u_Boost");

        Log.d(TAG, "✓ Shaders creados (Motor + Afterburner)");
    }

    private int compileShader(int type, String shaderCode) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "❌ Error compilando shader: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🎯 CONFIGURACIÓN DE OBJETIVOS
    // ═══════════════════════════════════════════════════════════════════════

    public void setPrimaryTarget(UfoAttacker target) {
        this.primaryTarget = target;
    }

    public void setSecondaryTarget(UfoScout target) {
        this.secondaryTarget = target;
    }

    /**
     * 🚧 Establece referencia al aliado para evitar colisiones
     */
    public void setAllyDefender(DefenderShip ally) {
        this.allyDefender = ally;
    }

    /**
     * Obtiene el objetivo activo actual
     */
    private Object getActiveTarget() {
        // Prioridad: primario si existe y no está destruido
        if (primaryTarget != null && !primaryTarget.isDestroyed()) {
            return primaryTarget;
        }
        // Fallback al secundario
        if (secondaryTarget != null && !secondaryTarget.isDestroyed()) {
            return secondaryTarget;
        }
        return null;
    }

    private float getTargetX() {
        Object target = getActiveTarget();
        if (target instanceof UfoAttacker) return ((UfoAttacker) target).x;
        if (target instanceof UfoScout) return ((UfoScout) target).getX();
        return x;
    }

    private float getTargetY() {
        Object target = getActiveTarget();
        if (target instanceof UfoAttacker) return ((UfoAttacker) target).y;
        if (target instanceof UfoScout) return ((UfoScout) target).getY();
        return y;
    }

    private float getTargetZ() {
        Object target = getActiveTarget();
        if (target instanceof UfoAttacker) return ((UfoAttacker) target).z;
        if (target instanceof UfoScout) return ((UfoScout) target).getZ();
        return z;
    }

    // Getters para colisiones
    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public float getScale() { return scale; }
    public float getCollisionRadius() { return scale * 0.4f; }
    public boolean isDestroyed() { return destroyed; }

    // Getters para sistema de vida
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }

    /**
     * 🎮 Verifica si el jugador puede disparar manualmente
     * @return true si la nave está viva y lista para disparar
     */
    public boolean canFireManually() {
        // No puede disparar si:
        // - Está destruida
        // - Está en proceso de respawn
        // - Está en invencibilidad (acaba de recibir daño)
        return !destroyed && health > 0 && respawnTimer <= 0;
    }

    @Override
    public void update(float deltaTime) {
        timeAccumulator += deltaTime;

        // Actualizar láseres siempre
        updateLasers(deltaTime);

        if (destroyed) {
            respawnTimer += deltaTime;
            if (respawnTimer >= respawnDelay) {
                respawn();
            }
            return;
        }

        if (invincibilityTimer > 0) {
            invincibilityTimer -= deltaTime;
        }

        // 🚀 Actualizar boost
        updateBoost(deltaTime);

        // ✨ Actualizar habilidades especiales
        updateTeleport(deltaTime);
        updateTripleBurst(deltaTime);

        // IA Ágil
        updateAI(deltaTime);

        // Sistema de disparo
        updateShooting(deltaTime);

        // Movimiento
        updateMovement(deltaTime);
    }

    /**
     * 🚀 Sistema de Boost/Afterburner - MÁS AGRESIVO
     */
    private void updateBoost(float deltaTime) {
        if (boostCooldown > 0) {
            boostCooldown -= deltaTime;
        }

        if (boostActive) {
            boostTimer += deltaTime;

            // Intensidad visual
            if (boostTimer < 0.3f) {
                boostIntensity = boostTimer / 0.3f;
            } else if (boostTimer > boostDuration - 0.3f) {
                boostIntensity = (boostDuration - boostTimer) / 0.3f;
            } else {
                boostIntensity = 0.9f + (float)Math.sin(timeAccumulator * 20f) * 0.1f;
            }

            if (boostTimer >= boostDuration) {
                boostActive = false;
                boostTimer = 0f;
                boostCooldown = boostCooldownMax;
                boostIntensity = 0f;
                Log.d(TAG, "🚀 Afterburner desactivado");
            }
        } else {
            boostIntensity = Math.max(0f, boostIntensity - deltaTime * 3f);

            // ⚡ Activar boost más frecuentemente
            if (boostCooldown <= 0) {
                boolean shouldBoost = false;

                // Boost cuando persigue
                if (currentState == State.PURSUING) {
                    shouldBoost = Math.random() < 0.3 * deltaTime;
                }

                // Boost cuando ataca (para maniobras rápidas)
                if (currentState == State.ATTACKING) {
                    shouldBoost = Math.random() < 0.15 * deltaTime;
                }

                // Boost cuando evade (para escapar)
                if (currentState == State.EVADING) {
                    shouldBoost = Math.random() < 0.5 * deltaTime;
                }

                if (shouldBoost) {
                    activateBoost();
                }
            }
        }
    }

    public void activateBoost() {
        if (boostCooldown <= 0 && !boostActive && !destroyed) {
            boostActive = true;
            boostTimer = 0f;
            Log.d(TAG, "🚀🚀🚀 ¡AFTERBURNER ACTIVADO! 🚀🚀🚀");
        }
    }

    /**
     * ✨ HABILIDAD ESPECIAL 1: TELETRANSPORTE TÁCTICO
     * Aparece instantáneamente detrás del enemigo
     */
    private void updateTeleport(float deltaTime) {
        // Actualizar cooldown
        if (teleportCooldown > 0) {
            teleportCooldown -= deltaTime;
            if (teleportCooldown <= 0) {
                teleportAvailable = true;
            }
        }

        // Actualizar efecto visual
        if (teleportEffectActive) {
            teleportEffectTimer += deltaTime;
            if (teleportEffectTimer >= teleportEffectDuration) {
                teleportEffectActive = false;
            }
        }

        // Auto-activar cuando está en peligro o para flanquear
        if (teleportAvailable && !destroyed) {
            Object target = getActiveTarget();
            if (target != null) {
                float tx = getTargetX();
                float ty = getTargetY();
                float tz = getTargetZ();
                float dx = tx - x;
                float dy = ty - y;
                float dz = tz - z;
                float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

                // Teletransporte ofensivo: flanquear al enemigo
                if (currentState == State.ATTACKING && dist > 2.0f && dist < 5.0f) {
                    if (Math.random() < 0.3 * deltaTime) {
                        activateTeleport(tx, ty, tz);
                    }
                }

                // Teletransporte defensivo: escapar si está muy cerca
                if (currentState == State.EVADING && dist < 1.0f) {
                    if (Math.random() < 0.8 * deltaTime) {
                        // Teletransportarse a posición segura
                        float safeX = (float)(Math.random() * BOUND_X * 2 - BOUND_X);
                        float safeY = BOUND_Y_MIN + 1.0f + (float)(Math.random());
                        float safeZ = BOUND_Z_MAX - 1.0f;
                        activateTeleportToPosition(safeX, safeY, safeZ);
                    }
                }

                // Teletransporte de persecución: si el objetivo está lejos
                if (currentState == State.PURSUING && dist > 4.0f) {
                    if (Math.random() < 0.2 * deltaTime) {
                        activateTeleport(tx, ty, tz);
                    }
                }
            }
        }
    }

    /**
     * Activa teletransporte detrás del objetivo
     */
    private void activateTeleport(float targetX, float targetY, float targetZ) {
        // Guardar posición original para efecto visual
        teleportOriginX = x;
        teleportOriginY = y;
        teleportOriginZ = z;

        // Calcular posición detrás del objetivo
        float dx = x - targetX;
        float dz = z - targetZ;
        float angle = (float) Math.atan2(dx, dz);

        // Aparecer detrás del objetivo (1.5 unidades de distancia)
        float behindDistance = 1.5f;
        teleportDestX = targetX - (float) Math.sin(angle) * behindDistance;
        teleportDestY = targetY + (float)(Math.random() - 0.5) * 0.5f;
        teleportDestZ = targetZ - (float) Math.cos(angle) * behindDistance;

        // Clamp a límites
        teleportDestX = Math.max(-BOUND_X, Math.min(BOUND_X, teleportDestX));
        teleportDestY = Math.max(BOUND_Y_MIN, Math.min(BOUND_Y_MAX, teleportDestY));
        teleportDestZ = Math.max(BOUND_Z_MIN, Math.min(BOUND_Z_MAX, teleportDestZ));

        // Teletransportar
        x = teleportDestX;
        y = teleportDestY;
        z = teleportDestZ;

        // Reset velocidad
        velocityX = 0;
        velocityY = 0;
        velocityZ = 0;

        // Activar efecto visual
        teleportEffectActive = true;
        teleportEffectTimer = 0f;

        // Cooldown
        teleportAvailable = false;
        teleportCooldown = teleportCooldownMax;

        Log.d(TAG, "✨✨✨ ¡TELETRANSPORTE TÁCTICO! ✨✨✨");
        Log.d(TAG, "  De: (" + teleportOriginX + ", " + teleportOriginZ + ")");
        Log.d(TAG, "  A:  (" + x + ", " + z + ")");
    }

    /**
     * Activa teletransporte a posición específica (escape)
     */
    private void activateTeleportToPosition(float destX, float destY, float destZ) {
        teleportOriginX = x;
        teleportOriginY = y;
        teleportOriginZ = z;

        teleportDestX = destX;
        teleportDestY = destY;
        teleportDestZ = destZ;

        x = destX;
        y = destY;
        z = destZ;

        velocityX = 0;
        velocityY = 0;
        velocityZ = 0;

        teleportEffectActive = true;
        teleportEffectTimer = 0f;

        teleportAvailable = false;
        teleportCooldown = teleportCooldownMax;

        Log.d(TAG, "✨ ¡TELETRANSPORTE DE ESCAPE!");
    }

    /**
     * 🔥 HABILIDAD ESPECIAL 2: RÁFAGA TRIPLE
     * Dispara 3 láseres a la vez en abanico durante 5 segundos
     */
    private void updateTripleBurst(float deltaTime) {
        // Actualizar cooldown
        if (tripleBurstCooldown > 0) {
            tripleBurstCooldown -= deltaTime;
        }

        // Actualizar duración si está activo
        if (tripleBurstActive) {
            tripleBurstTimer += deltaTime;
            if (tripleBurstTimer >= tripleBurstDuration) {
                tripleBurstActive = false;
                tripleBurstCooldown = tripleBurstCooldownMax;
                Log.d(TAG, "🔥 Ráfaga Triple terminada");
            }
        } else {
            // Auto-activar en combate
            if (tripleBurstCooldown <= 0 && !destroyed) {
                Object target = getActiveTarget();
                if (target != null) {
                    float dist = getDistanceToTarget();

                    // Activar cuando está en rango óptimo de ataque
                    if (dist < attackRange * 0.6f && dist > evadeRange) {
                        if (Math.random() < 0.15 * deltaTime) {
                            activateTripleBurst();
                        }
                    }

                    // Activar cuando tiene baja vida (modo desesperado)
                    if (health <= 2 && dist < attackRange) {
                        if (Math.random() < 0.3 * deltaTime) {
                            activateTripleBurst();
                        }
                    }
                }
            }
        }
    }

    private float getDistanceToTarget() {
        Object target = getActiveTarget();
        if (target == null) return Float.MAX_VALUE;

        float dx = getTargetX() - x;
        float dy = getTargetY() - y;
        float dz = getTargetZ() - z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Activa el modo Ráfaga Triple
     */
    public void activateTripleBurst() {
        if (!tripleBurstActive && tripleBurstCooldown <= 0 && !destroyed) {
            tripleBurstActive = true;
            tripleBurstTimer = 0f;
            Log.d(TAG, "🔥🔥🔥 ¡RÁFAGA TRIPLE ACTIVADA! 🔥🔥🔥");
        }
    }

    private void updateAI(float deltaTime) {
        wanderTimer -= deltaTime;
        exploreTimer -= deltaTime;
        combatDisengageTimer += deltaTime;
        curvePhase += deltaTime * curveFrequency;

        Object target = getActiveTarget();

        // ═══════════════════════════════════════════════════════════════
        // 🌌 DECISIÓN: ¿Explorar o Combatir?
        // ═══════════════════════════════════════════════════════════════
        boolean shouldExplore = false;

        // Desengancharse del combate periódicamente para explorar
        if (combatDisengageTimer > COMBAT_DISENGAGE_TIME) {
            if (Math.random() < 0.4) {  // 40% probabilidad de explorar
                shouldExplore = true;
                combatDisengageTimer = 0f;
            } else {
                combatDisengageTimer = COMBAT_DISENGAGE_TIME * 0.5f;  // Reintentar pronto
            }
        }

        // Si está en modo exploración, continuar explorando
        if (currentState == State.EXPLORING || currentState == State.PATROLLING) {
            if (exploreTimer > 0) {
                shouldExplore = true;
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // 🛸 MODO EXPLORACIÓN / PATRULLA
        // ═══════════════════════════════════════════════════════════════
        if (shouldExplore || target == null) {
            updateExplorationBehavior(deltaTime);
            return;
        }

        // ═══════════════════════════════════════════════════════════════
        // ⚔️ MODO COMBATE (cuando hay objetivo y no está explorando)
        // ═══════════════════════════════════════════════════════════════
        float tx = getTargetX();
        float ty = getTargetY();
        float tz = getTargetZ();

        float dx = tx - x;
        float dy = ty - y;
        float dz = tz - z;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist < evadeRange) {
            // Muy cerca - evadir hacia cualquier dirección
            currentState = State.EVADING;
            maxSpeed = 3.0f;
            float escapeAngle = (float) Math.atan2(-dx, -dz) + (float)(Math.random() - 0.5) * 2f;
            float escapeDistance = 3f + (float)(Math.random() * 2f);
            targetX = x + (float) Math.sin(escapeAngle) * escapeDistance;
            targetZ = z + (float) Math.cos(escapeAngle) * escapeDistance;
            // Puede escapar hacia arriba O hacia abajo (ecualizador)
            targetY = y + (float)(Math.random() - 0.5) * 4f;

        } else if (dist < attackRange * 0.5f) {
            // Rango óptimo - atacar con maniobras amplias
            currentState = State.ATTACKING;
            maxSpeed = 2.5f;

            // Vuelo en círculos MÁS AMPLIOS alrededor del objetivo
            float circleAngle = (float) Math.atan2(x - tx, z - tz);
            circleAngle += deltaTime * 1.2f;
            float orbitRadius = 2.0f + (float) Math.sin(curvePhase) * 1.0f;
            targetX = tx + (float) Math.sin(circleAngle) * orbitRadius;
            targetZ = tz + (float) Math.cos(circleAngle) * orbitRadius;
            // Variar altura significativamente durante combate
            targetY = ty + (float) Math.sin(curvePhase * 0.6f) * 2.0f;

        } else if (dist < attackRange) {
            // Acercándose - flanquear con movimientos amplios
            currentState = State.FLANKING;
            maxSpeed = 2.8f;

            if (wanderTimer <= 0) {
                wanderTimer = wanderInterval;
                // Aproximación en zigzag MÁS AMPLIO
                float zigzag = (float)(Math.random() - 0.5) * 5f;
                targetX = tx + zigzag;
                targetY = ty + (float)(Math.random() - 0.5) * 3f;
                targetZ = tz + (float)(Math.random() - 0.5) * 3f;
            }
        } else {
            // Lejos - perseguir pero con curvas
            currentState = State.PURSUING;
            maxSpeed = 3.0f;

            // No ir directo, hacer curvas mientras persigue
            float curveOffset = (float) Math.sin(curvePhase * 0.5f) * 1.5f;
            targetX = tx + curveOffset;
            targetY = ty + (float) Math.sin(curvePhase * 0.3f) * 1.0f;
            targetZ = tz;
        }

        // Clamp targets a los nuevos límites expandidos
        targetX = Math.max(-BOUND_X, Math.min(BOUND_X, targetX));
        targetY = Math.max(BOUND_Y_MIN, Math.min(BOUND_Y_MAX, targetY));
        targetZ = Math.max(BOUND_Z_MIN, Math.min(BOUND_Z_MAX, targetZ));
    }

    /**
     * 🌌 Comportamiento de exploración libre
     * Las naves visitan puntos de interés por toda la escena
     */
    private void updateExplorationBehavior(float deltaTime) {
        // Decidir entre explorar POIs o movimiento libre
        if (Math.random() < 0.6) {
            currentState = State.PATROLLING;
        } else {
            currentState = State.EXPLORING;
        }

        maxSpeed = 2.0f;  // Velocidad moderada mientras explora

        if (exploreTimer <= 0) {
            exploreTimer = exploreInterval + (float)(Math.random() * 3f);
            Log.d(TAG, "🎯 Interceptor exploring: POI=" + currentPOI + " pos=(" +
                  String.format("%.1f", x) + "," + String.format("%.1f", y) + "," + String.format("%.1f", z) + ")");

            if (currentState == State.PATROLLING) {
                // 🎯 Ir a un punto de interés aleatorio
                currentPOI = (int)(Math.random() * POINTS_OF_INTEREST.length);
                float[] poi = POINTS_OF_INTEREST[currentPOI];
                targetX = poi[0] + (float)(Math.random() - 0.5) * 1.0f;
                targetY = poi[1] + (float)(Math.random() - 0.5) * 0.5f;
                targetZ = poi[2] + (float)(Math.random() - 0.5) * 1.0f;
            } else {
                // 🌌 Movimiento libre aleatorio por toda la escena
                targetX = (float)(Math.random() * BOUND_X * 2 - BOUND_X);
                targetY = (float)(Math.random() * (BOUND_Y_MAX - BOUND_Y_MIN) + BOUND_Y_MIN);
                targetZ = (float)(Math.random() * (BOUND_Z_MAX - BOUND_Z_MIN) + BOUND_Z_MIN);
            }
        }

        // Verificar si llegó al destino
        float dx = targetX - x;
        float dy = targetY - y;
        float dz = targetZ - z;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist < 0.5f) {
            // Llegó al destino, elegir nuevo punto
            exploreTimer = 0f;
        }

        // Si detecta enemigo cercano mientras explora, puede entrar en combate
        Object target = getActiveTarget();
        if (target != null) {
            float tdx = getTargetX() - x;
            float tdy = getTargetY() - y;
            float tdz = getTargetZ() - z;
            float targetDist = (float) Math.sqrt(tdx * tdx + tdy * tdy + tdz * tdz);

            // Si el enemigo está muy cerca, entrar en combate
            if (targetDist < attackRange * 0.4f) {
                currentState = State.ATTACKING;
                exploreTimer = 0f;
                combatDisengageTimer = 0f;
            }
        }

        // Clamp targets
        targetX = Math.max(-BOUND_X, Math.min(BOUND_X, targetX));
        targetY = Math.max(BOUND_Y_MIN, Math.min(BOUND_Y_MAX, targetY));
        targetZ = Math.max(BOUND_Z_MIN, Math.min(BOUND_Z_MAX, targetZ));
    }

    private void updateShooting(float deltaTime) {
        shootTimer += deltaTime;

        Object target = getActiveTarget();

        if (target != null) {
            float tx = getTargetX();
            float ty = getTargetY();
            float tz = getTargetZ();

            float dx = tx - x;
            float dy = ty - y;
            float dz = tz - z;
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

            // 🔫 Calcular intervalo de disparo
            float currentShootInterval = shootInterval;

            // Durante boost, dispara más rápido
            if (boostActive) {
                currentShootInterval *= 0.5f;  // 2x más rápido
            }

            // Disparar si está en rango (SIEMPRE disparar, en cualquier estado)
            if (dist < attackRange && shootTimer >= currentShootInterval) {
                shootTimer = 0f;
                shootInterval = MIN_SHOOT_INTERVAL + (float)(Math.random() * (MAX_SHOOT_INTERVAL - MIN_SHOOT_INTERVAL));

                // Predicción de movimiento del objetivo para mejor puntería
                float leadTime = dist / 8.0f;  // Tiempo estimado de viaje del láser
                float predictedX = tx;
                float predictedY = ty;
                float predictedZ = tz;

                // Disparar al objetivo (con ligera predicción)
                shootAt(predictedX, predictedY, predictedZ);
            }
        }
    }

    private void shootAt(float tX, float tY, float tZ) {
        if (tripleBurstActive) {
            // 🔥 RÁFAGA TRIPLE: Dispara 3 láseres en abanico
            float dx = tX - x;
            float dz = tZ - z;
            float baseAngle = (float) Math.atan2(dx, dz);
            float spreadAngle = 0.15f; // ~8.6 grados de separación

            int lasersFired = 0;
            float[] angles = { baseAngle - spreadAngle, baseAngle, baseAngle + spreadAngle };

            for (float angle : angles) {
                for (Laser laser : lasers) {
                    if (!laser.isActive()) {
                        laser.setCameraController(camera);
                        // Calcular destino con el ángulo
                        float dist = (float) Math.sqrt(dx * dx + dz * dz);
                        float targetX = x + (float) Math.sin(angle) * dist;
                        float targetZ = z + (float) Math.cos(angle) * dist;
                        laser.fire(x, y, z, targetX, tY, targetZ);
                        lasersFired++;
                        break;
                    }
                }
            }

            if (lasersFired > 0) {
                Log.d(TAG, "🔥 ¡RÁFAGA TRIPLE! " + lasersFired + " láseres disparados!");
            }
        } else {
            // Disparo normal: 1 láser
            for (Laser laser : lasers) {
                if (!laser.isActive()) {
                    laser.setCameraController(camera);
                    laser.fire(x, y, z, tX, tY, tZ);
                    Log.d(TAG, "🔫 Interceptor disparó láser cyan!");
                    return;
                }
            }
        }
    }

    private void updateLasers(float deltaTime) {
        for (Laser laser : lasers) {
            laser.update(deltaTime);

            if (laser.isActive()) {
                // Verificar colisión con UfoAttacker
                if (primaryTarget != null && !primaryTarget.isDestroyed()) {
                    if (laser.checkCollision(primaryTarget.x, primaryTarget.y, primaryTarget.z, 0.35f)) {
                        laser.deactivate();
                        primaryTarget.takeDamage();
                        Log.d(TAG, "💥 ¡Láser impactó a UfoAttacker!");
                    }
                }

                // Verificar colisión con UfoScout
                if (secondaryTarget != null && !secondaryTarget.isDestroyed()) {
                    if (laser.checkCollision(secondaryTarget.getX(), secondaryTarget.getY(), secondaryTarget.getZ(), 0.3f)) {
                        laser.deactivate();
                        secondaryTarget.takeDamage();
                        Log.d(TAG, "💥 ¡Láser impactó a UfoScout!");
                    }
                }
            }
        }
    }

    public List<Laser> getLasers() {
        return lasers;
    }

    private void updateMovement(float deltaTime) {
        float speedMult = boostActive ? BOOST_SPEED_MULT : 1.0f;
        float currentMaxSpeed = maxSpeed * speedMult;
        float currentAccel = acceleration * speedMult;

        // ═══════════════════════════════════════════════════════════
        // 🚧 ANTI-COLISIÓN CON OTRAS NAVES
        // ═══════════════════════════════════════════════════════════
        float avoidX = 0, avoidY = 0, avoidZ = 0;

        // Evitar al aliado DefenderShip
        if (allyDefender != null && !allyDefender.isDestroyed()) {
            float dx = x - allyDefender.x;
            float dy = y - allyDefender.y;
            float dz = z - allyDefender.z;
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (dist < safeDistance && dist > 0.01f) {
                float pushForce = (safeDistance - dist) / dist * 3.0f;
                avoidX += dx * pushForce;
                avoidY += dy * pushForce;
                avoidZ += dz * pushForce;
            }
        }

        // Evitar a UfoAttacker (enemigo primario)
        if (primaryTarget != null && !primaryTarget.isDestroyed()) {
            float dx = x - primaryTarget.x;
            float dy = y - primaryTarget.y;
            float dz = z - primaryTarget.z;
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (dist < safeDistance * 0.8f && dist > 0.01f) {
                float pushForce = (safeDistance * 0.8f - dist) / dist * 4.0f;
                avoidX += dx * pushForce;
                avoidY += dy * pushForce;
                avoidZ += dz * pushForce;
            }
        }

        // Evitar a UfoScout (enemigo secundario)
        if (secondaryTarget != null && !secondaryTarget.isDestroyed()) {
            float dx = x - secondaryTarget.getX();
            float dy = y - secondaryTarget.getY();
            float dz = z - secondaryTarget.getZ();
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (dist < safeDistance * 0.8f && dist > 0.01f) {
                float pushForce = (safeDistance * 0.8f - dist) / dist * 4.0f;
                avoidX += dx * pushForce;
                avoidY += dy * pushForce;
                avoidZ += dz * pushForce;
            }
        }

        // Aplicar fuerza de evasión
        velocityX += avoidX * deltaTime * 5.0f;
        velocityY += avoidY * deltaTime * 5.0f;
        velocityZ += avoidZ * deltaTime * 5.0f;

        float dx = targetX - x;
        float dy = targetY - y;
        float dz = targetZ - z;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist > 0.05f) {
            float invDist = 1.0f / dist;
            float dirX = dx * invDist;
            float dirY = dy * invDist;
            float dirZ = dz * invDist;

            // Curva de vuelo
            float curveOffset = (float) Math.sin(curvePhase) * curveAmplitude;
            float perpX = -dirZ * curveOffset;
            float perpZ = dirX * curveOffset;

            float accel = currentAccel * deltaTime;
            velocityX += (dirX + perpX) * accel;
            velocityY += dirY * accel;
            velocityZ += (dirZ + perpZ) * accel;

            float currentSpeed = (float) Math.sqrt(velocityX*velocityX + velocityY*velocityY + velocityZ*velocityZ);
            if (currentSpeed > currentMaxSpeed) {
                float s = currentMaxSpeed / currentSpeed;
                velocityX *= s;
                velocityY *= s;
                velocityZ *= s;
            }
        }

        velocityX *= deceleration;
        velocityY *= deceleration;
        velocityZ *= deceleration;

        x += velocityX * deltaTime;
        y += velocityY * deltaTime;
        z += velocityZ * deltaTime;

        // Rotación agresiva hacia dirección de movimiento
        float speedXZ = (float) Math.sqrt(velocityX*velocityX + velocityZ*velocityZ);
        if (speedXZ > 0.01f) {
            float targetRotY = (float) Math.toDegrees(Math.atan2(velocityX, velocityZ));
            float deltaRot = targetRotY - rotationY;
            while (deltaRot > 180) deltaRot -= 360;
            while (deltaRot < -180) deltaRot += 360;
            rotationY += deltaRot * 4f * deltaTime;  // Rotación más rápida
        }

        // Inclinación agresiva en curvas
        float tiltFactor = 35f;
        float targetTiltX = -velocityZ * tiltFactor;
        float targetTiltZ = velocityX * tiltFactor;
        tiltX += (targetTiltX - tiltX) * 4f * deltaTime;
        tiltZ += (targetTiltZ - tiltZ) * 4f * deltaTime;

        // Clamp posición
        x = Math.max(-BOUND_X, Math.min(BOUND_X, x));
        y = Math.max(BOUND_Y_MIN, Math.min(BOUND_Y_MAX, y));
        z = Math.max(BOUND_Z_MIN, Math.min(BOUND_Z_MAX, z));

        // Escala dinámica
        float zNormalized = (z - BOUND_Z_MIN) / (BOUND_Z_MAX - BOUND_Z_MIN);
        float scaleMultiplier = 0.7f + zNormalized * 0.6f;
        scale = baseScale * scaleMultiplier;
    }

    public void takeDamage() {
        if (destroyed || invincibilityTimer > 0) return;

        health--;
        invincibilityTimer = invincibilityDuration;

        Log.d(TAG, "💥 Interceptor recibió daño! HP: " + health + "/" + maxHealth);

        if (health <= 0) {
            destroy();
        }
    }

    private void destroy() {
        destroyed = true;
        respawnTimer = 0f;
        Log.d(TAG, "💀 INTERCEPTOR DESTRUIDO! Respawn en " + respawnDelay + " segundos");
    }

    private void respawn() {
        destroyed = false;
        health = maxHealth;
        respawnTimer = 0f;
        invincibilityTimer = 2.0f;
        boostActive = false;
        boostIntensity = 0f;

        x = (float)(Math.random() * BOUND_X * 2 - BOUND_X);
        y = BOUND_Y_MIN + 1.0f;
        z = BOUND_Z_MAX - 0.5f;

        targetX = x;
        targetY = y;
        targetZ = z;

        Log.d(TAG, "🔄 INTERCEPTOR RESPAWN en (" + x + ", " + y + ", " + z + ")");
    }

    @Override
    public void draw() {
        if (camera == null || shaderProgram == 0) return;

        if (destroyed) {
            drawLasers();
            return;
        }

        // Parpadeo durante invencibilidad
        if (invincibilityTimer > 0) {
            if ((int)(invincibilityTimer * 10) % 2 == 0) {
                drawLasers();
                return;
            }
        }

        GLES30.glUseProgram(shaderProgram);
        GLES30.glDisable(GLES30.GL_CULL_FACE);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);
        Matrix.rotateM(modelMatrix, 0, rotationY, 0, 1, 0);
        Matrix.rotateM(modelMatrix, 0, tiltX, 1, 0, 0);
        Matrix.rotateM(modelMatrix, 0, tiltZ, 0, 0, 1);
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

        camera.computeMvp(modelMatrix, mvpMatrix);

        GLES30.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLES30.glUniform1f(uTimeHandle, timeAccumulator % 60.0f);
        GLES30.glUniform1f(uAlphaHandle, 1.0f);
        GLES30.glUniform1f(uBoostHandle, boostIntensity);

        vertexBuffer.position(0);
        GLES30.glEnableVertexAttribArray(aPositionHandle);
        GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        if (uvBuffer != null && aTexCoordHandle >= 0) {
            uvBuffer.position(0);
            GLES30.glEnableVertexAttribArray(aTexCoordHandle);
            GLES30.glVertexAttribPointer(aTexCoordHandle, 2, GLES30.GL_FLOAT, false, 0, uvBuffer);
        }

        if (textureId > 0 && uTextureHandle >= 0) {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
            GLES30.glUniform1i(uTextureHandle, 0);
        }

        indexBuffer.position(0);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, indexBuffer);

        GLES30.glDisableVertexAttribArray(aPositionHandle);
        if (aTexCoordHandle >= 0) {
            GLES30.glDisableVertexAttribArray(aTexCoordHandle);
        }

        GLES30.glEnable(GLES30.GL_CULL_FACE);

        // ✨ Dibujar efecto de teletransporte
        drawTeleportEffect();

        drawLasers();
    }

    /**
     * ✨ Dibuja el efecto visual del teletransporte
     */
    private void drawTeleportEffect() {
        if (!teleportEffectActive || teleportProgram == 0 || camera == null) return;

        GLES30.glUseProgram(teleportProgram);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE);
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);

        float progress = teleportEffectTimer / teleportEffectDuration;

        // Dibujar efecto en posición origen (cyan desvaneciendo)
        drawTeleportParticles(teleportOriginX, teleportOriginY, teleportOriginZ,
                              progress, 0.3f, 0.8f, 1.0f);  // Cyan

        // Dibujar efecto en posición destino (cyan apareciendo)
        drawTeleportParticles(teleportDestX, teleportDestY, teleportDestZ,
                              1.0f - progress, 0.5f, 1.0f, 1.0f);  // Cyan brillante

        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void drawTeleportParticles(float px, float py, float pz,
                                       float timePhase, float r, float g, float b) {
        // Usar matrices pre-asignadas (evita GC en draw loop)
        Matrix.setIdentityM(teleportParticleModel, 0);
        Matrix.translateM(teleportParticleModel, 0, px, py, pz);
        Matrix.scaleM(teleportParticleModel, 0, scale * 2f, scale * 2f, scale * 2f);

        camera.computeMvp(teleportParticleModel, teleportParticleMvp);

        GLES30.glUniformMatrix4fv(teleportUMVPHandle, 1, false, teleportParticleMvp, 0);
        GLES30.glUniform1f(teleportUTimeHandle, timePhase);
        GLES30.glUniform4f(teleportUColorHandle, r, g, b, 1.0f - timePhase);

        teleportParticleBuffer.position(0);
        GLES30.glEnableVertexAttribArray(teleportAPositionHandle);
        GLES30.glVertexAttribPointer(teleportAPositionHandle, 3, GLES30.GL_FLOAT,
                                     false, 0, teleportParticleBuffer);

        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, TELEPORT_PARTICLES);

        GLES30.glDisableVertexAttribArray(teleportAPositionHandle);
    }

    private void drawLasers() {
        for (Laser laser : lasers) {
            if (laser.hasActiveEffect()) {
                laser.draw();
            }
        }
    }

    // =========================================================================
    // LIMPIEZA DE RECURSOS OPENGL
    // =========================================================================

    /**
     * Libera todos los recursos OpenGL asociados a esta nave.
     * DEBE llamarse cuando la escena se destruye.
     */
    public void cleanup() {
        Log.d(TAG, "=== CLEANUP HumanInterceptor ===");

        // Eliminar shader programs
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            Log.d(TAG, "  Shader program eliminado: " + shaderProgram);
            shaderProgram = 0;
        }

        if (teleportProgram != 0) {
            GLES30.glDeleteProgram(teleportProgram);
            Log.d(TAG, "  Teleport program eliminado: " + teleportProgram);
            teleportProgram = 0;
        }

        // Eliminar textura
        if (textureId != 0) {
            GLES30.glDeleteTextures(1, new int[]{textureId}, 0);
            Log.d(TAG, "  Textura eliminada: " + textureId);
            textureId = 0;
        }

        // Limpiar buffers
        vertexBuffer = null;
        uvBuffer = null;
        indexBuffer = null;
        teleportParticleBuffer = null;

        // Limpiar laseres
        if (lasers != null) {
            for (Laser laser : lasers) {
                laser.cleanup();
            }
            lasers.clear();
        }

        Log.d(TAG, "=== CLEANUP COMPLETADO ===");
    }
}
