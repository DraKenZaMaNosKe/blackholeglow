package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
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
 * ║   👾 UFO ATTACKER - Nave de Ataque Alienígena Principal                  ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  CARACTERÍSTICAS:                                                         ║
 * ║  • Más grande y amenazante que UfoScout                                  ║
 * ║  • Brillo púrpura del núcleo de energía                                  ║
 * ║  • Movimiento agresivo, persigue directamente                            ║
 * ║  • Dispara menos frecuente pero con más impacto                          ║
 * ║  • 8 HP (más resistente)                                                 ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class UfoAttacker implements SceneObject, CameraAware {
    private static final String TAG = "UfoAttacker";

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
    private int uOverchargeHandle;  // Para efecto de sobrecarga

    // Transformación
    public float x, y, z;
    private float scale;
    private float rotationY = 0f;
    private float tiltX = 0f;
    private float tiltZ = 0f;

    // ═══════════════════════════════════════════════════════════════════════
    // 🧠 IA DE MOVIMIENTO AGRESIVO
    // ═══════════════════════════════════════════════════════════════════════
    private float velocityX = 0f, velocityY = 0f, velocityZ = 0f;
    private float targetX, targetY, targetZ;
    private float baseScale;

    // Movimiento más lento pero directo
    private float moveSpeed = 0.6f;
    private float maxSpeed = 1.5f;
    private float acceleration = 1.2f;
    private float deceleration = 0.93f;

    // Timers
    private float wanderTimer = 0f;
    private float wanderInterval = 4.0f;
    private float curvePhase = 0f;
    private float curveAmplitude = 0.2f;
    private float curveFrequency = 0.8f;

    // Estados - AMPLIADOS para exploración libre
    public enum State {
        HUNTING,     // Cazando objetivo
        ATTACKING,   // Atacando
        RETREATING,  // Retrocediendo
        EXPLORING,   // 🌌 Explorando la escena
        PATROLLING   // 🛸 Patrullando puntos de interés
    }
    private State currentState = State.EXPLORING;

    // ═══════════════════════════════════════════════════════════════════════
    // 🎯 SISTEMA DE OBJETIVOS (Primario y Secundario)
    // ═══════════════════════════════════════════════════════════════════════
    private DefenderShip primaryTarget = null;      // Objetivo principal
    private HumanInterceptor secondaryTarget = null; // Objetivo secundario
    private float attackRange = 12f;
    private float retreatRange = 1.0f;

    // ═══════════════════════════════════════════════════════════════════════
    // 🚧 REFERENCIAS A OTRAS NAVES (para evitar colisiones)
    // ═══════════════════════════════════════════════════════════════════════
    private UfoScout allyScout = null;
    private float safeDistance = 1.2f;  // Distancia mínima entre naves

    // ═══════════════════════════════════════════════════════════════════════
    // 💔 SISTEMA DE VIDA (más HP que Scout)
    // ═══════════════════════════════════════════════════════════════════════
    private int health = 8;
    private int maxHealth = 8;
    private boolean destroyed = false;
    private float respawnTimer = 0f;
    private float respawnDelay = 12.0f;  // Tarda más en respawnear
    private float invincibilityTimer = 0f;
    private float invincibilityDuration = 1.5f;

    // Cámara
    private CameraController camera;

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // Tiempo
    private float timeAccumulator = 0f;

    // ═══════════════════════════════════════════════════════════════════════
    // 🌌 LÍMITES DE MOVIMIENTO EXPANDIDOS - TODA LA ESCENA
    // ═══════════════════════════════════════════════════════════════════════
    private static final float BOUND_X = 3.5f;        // Más ancho
    private static final float BOUND_Y_MIN = -2.5f;   // ¡Hasta el ecualizador!
    private static final float BOUND_Y_MAX = 3.5f;    // Hasta arriba
    private static final float BOUND_Z_MIN = -4.0f;   // Profundidad
    private static final float BOUND_Z_MAX = 3.0f;    // Más cerca de cámara

    // ═══════════════════════════════════════════════════════════════════════
    // 🎯 PUNTOS DE INTERÉS (POIs) - Lugares a explorar
    // ═══════════════════════════════════════════════════════════════════════
    private static final float[][] POINTS_OF_INTEREST = {
        {-3.0f, 2.5f, -2.0f},    // 🛰️ Cerca de la estación espacial
        {2.5f, 2.0f, -1.0f},     // ☀️ Cerca del sol
        {0.5f, 0.0f, 0.5f},      // 🌍 Cerca de la Tierra
        {-2.0f, -1.5f, 1.5f},    // 🎵 Zona del ecualizador (izquierda)
        {2.0f, -1.5f, 1.5f},     // 🎵 Zona del ecualizador (derecha)
        {0.0f, -2.0f, 2.0f},     // 🎵 Centro del ecualizador
        {-3.0f, 1.0f, -3.5f},    // 🌌 Galaxias lejanas (izquierda)
        {3.0f, 1.0f, -3.5f},     // 🌌 Galaxias lejanas (derecha)
        {0.0f, 3.0f, -1.0f},     // ⬆️ Zona superior
        {-1.5f, 0.5f, 2.5f},     // 📍 Frente izquierda
        {1.5f, 0.5f, 2.5f},      // 📍 Frente derecha
    };
    private int currentPOI = 0;
    private float exploreTimer = 0f;
    private float exploreInterval = 5.0f;
    private float combatDisengageTimer = 0f;
    private static final float COMBAT_DISENGAGE_TIME = 10.0f;

    // ═══════════════════════════════════════════════════════════════════════
    // 🔫 SISTEMA DE DISPAROS (TEAM ALIEN - LÁSER VERDE POTENTE)
    // ═══════════════════════════════════════════════════════════════════════
    private static final int MAX_LASERS = 10;  // Más láseres para sobrecarga
    private final List<Laser> lasers = new ArrayList<>();
    private float shootTimer = 0f;
    private float shootInterval = 2.0f;  // Dispara más lento
    private static final float MIN_SHOOT_INTERVAL = 1.5f;
    private static final float MAX_SHOOT_INTERVAL = 3.0f;

    // ═══════════════════════════════════════════════════════════════════════
    // ⚡ HABILIDAD ESPECIAL: SOBRECARGA DE PLASMA
    // ═══════════════════════════════════════════════════════════════════════
    private boolean plasmaOverchargeActive = false;
    private float overchargeTimer = 0f;
    private float overchargeDuration = 5.0f;      // Dura 5 segundos
    private float overchargeCooldown = 0f;
    private float overchargeCooldownMax = 18.0f;  // Cooldown 18 segundos

    // Multiplicadores durante sobrecarga
    private static final float OVERCHARGE_SPEED_MULT = 2.0f;    // 2x velocidad
    private static final float OVERCHARGE_SHOOT_MULT = 0.33f;   // 3x más rápido (intervalo / 3)
    private static final float OVERCHARGE_MIN_SHOOT = 0.3f;     // Disparo mínimo durante sobrecarga

    // Visual de sobrecarga
    private float overchargeIntensity = 0f;  // 0 a 1, para el shader

    /**
     * Constructor
     */
    public UfoAttacker(Context context, TextureLoader textureLoader,
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
        Log.d(TAG, "👾 Creando UFO Attacker");
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        loadModel();
        loadTexture();
        createShaders();
        initLasers();

        Log.d(TAG, "✅ UFO Attacker creado en (" + x + ", " + y + ", " + z + ")");
    }

    private void initLasers() {
        for (int i = 0; i < MAX_LASERS; i++) {
            lasers.add(new Laser(Laser.TEAM_ALIEN));
        }
        shootInterval = MIN_SHOOT_INTERVAL + (float)(Math.random() * (MAX_SHOOT_INTERVAL - MIN_SHOOT_INTERVAL));
        Log.d(TAG, "🔫 Sistema de láseres inicializado (" + MAX_LASERS + " láseres)");
    }

    private void loadModel() {
        try {
            Log.d(TAG, "📦 Cargando ufo_attacker.obj...");

            ObjLoader.Mesh mesh = ObjLoader.loadObj(context, "ufo_attacker.obj");

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
            Log.e(TAG, "❌ Error cargando ufo_attacker.obj", e);
            throw new RuntimeException(e);
        }
    }

    private void loadTexture() {
        Log.d(TAG, "🎨 Cargando textura ufo_attacker_texture...");

        int textureResId = context.getResources().getIdentifier(
                "ufo_attacker_texture", "drawable", context.getPackageName());

        if (textureResId != 0) {
            textureId = textureLoader.getTexture(textureResId);
            Log.d(TAG, "✓ Textura cargada (ID=" + textureId + ")");
        } else {
            Log.e(TAG, "❌ Textura no encontrada: ufo_attacker_texture");
            textureId = 0;
        }
    }

    /**
     * 👾✨ SHADER CON BRILLO PÚRPURA ALIENÍGENA
     */
    private void createShaders() {
        Log.d(TAG, "🎨 Creando shaders con brillo púrpura...");

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

        // Fragment Shader - MOTOR DE PLASMA ACTIVO 🔥
        String fragmentShaderCode =
                "precision mediump float;\n" +
                "\n" +
                "uniform sampler2D u_Texture;\n" +
                "uniform float u_Time;\n" +
                "uniform float u_Alpha;\n" +
                "uniform float u_Overcharge;\n" +  // 0.0 = normal, 1.0 = sobrecarga máxima
                "\n" +
                "varying vec2 v_TexCoord;\n" +
                "varying vec3 v_LocalPos;\n" +
                "\n" +
                "// Función de ruido simple para plasma\n" +
                "float hash(vec2 p) {\n" +
                "    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);\n" +
                "}\n" +
                "\n" +
                "float noise(vec2 p) {\n" +
                "    vec2 i = floor(p);\n" +
                "    vec2 f = fract(p);\n" +
                "    f = f * f * (3.0 - 2.0 * f);\n" +
                "    float a = hash(i);\n" +
                "    float b = hash(i + vec2(1.0, 0.0));\n" +
                "    float c = hash(i + vec2(0.0, 1.0));\n" +
                "    float d = hash(i + vec2(1.0, 1.0));\n" +
                "    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);\n" +
                "}\n" +
                "\n" +
                "void main() {\n" +
                "    // Invertir V para Meshy\n" +
                "    vec2 uv = vec2(v_TexCoord.x, 1.0 - v_TexCoord.y);\n" +
                "    vec4 texColor = texture2D(u_Texture, uv);\n" +
                "    vec3 color = texColor.rgb;\n" +
                "    \n" +
                "    // ═══ 👾 DETECTAR ZONA PÚRPURA DEL NÚCLEO ═══\n" +
                "    float purpleAmount = texColor.r * 0.5 + texColor.b * 0.8 - texColor.g * 0.6;\n" +
                "    float isCore = smoothstep(0.3, 0.6, purpleAmount);\n" +
                "    \n" +
                "    // ═══ 🔥 PLASMA ACTIVO EN EL NÚCLEO ═══\n" +
                "    if (isCore > 0.1) {\n" +
                "        // Múltiples capas de plasma moviéndose\n" +
                "        float plasma1 = noise(uv * 8.0 + u_Time * 1.5);\n" +
                "        float plasma2 = noise(uv * 12.0 - u_Time * 2.0 + 100.0);\n" +
                "        float plasma3 = noise(uv * 5.0 + vec2(u_Time * 0.8, -u_Time * 1.2));\n" +
                "        \n" +
                "        float plasma = (plasma1 + plasma2 + plasma3) / 3.0;\n" +
                "        \n" +
                "        // Pulso de respiración del núcleo\n" +
                "        float breathe = sin(u_Time * 2.5) * 0.3 + 0.7;\n" +
                "        \n" +
                "        // Intensificar el púrpura con plasma\n" +
                "        vec3 plasmaColor = vec3(0.7, 0.2, 1.0) * plasma * breathe;\n" +
                "        vec3 coreGlow = vec3(1.0, 0.4, 1.0) * pow(plasma, 2.0) * 0.5;\n" +
                "        \n" +
                "        color += (plasmaColor + coreGlow) * isCore * 0.6;\n" +
                "    }\n" +
                "    \n" +
                "    // ═══ ⚡ VENAS DE ENERGÍA EN EL CASCO ═══\n" +
                "    float veinNoise = noise(uv * 15.0 + vec2(u_Time * 0.5, 0.0));\n" +
                "    float veins = smoothstep(0.6, 0.8, veinNoise);\n" +
                "    vec3 veinColor = vec3(0.5, 0.1, 0.8) * veins * 0.25;\n" +
                "    color += veinColor * (1.0 - isCore);  // Solo en el casco, no en núcleo\n" +
                "    \n" +
                "    // ═══ 💫 RIM LIGHT ENERGÉTICO ═══\n" +
                "    float rim = 1.0 - abs(v_LocalPos.y);\n" +
                "    rim = pow(rim, 2.0);\n" +
                "    float rimPulse = sin(u_Time * 4.0 + v_LocalPos.x * 10.0) * 0.5 + 0.5;\n" +
                "    vec3 rimColor = vec3(0.6, 0.2, 0.9) * rim * rimPulse * 0.4;\n" +
                "    color += rimColor;\n" +
                "    \n" +
                "    // ═══ ✨ DESTELLO DE ENERGÍA OCASIONAL ═══\n" +
                "    float flash = pow(sin(u_Time * 8.0) * 0.5 + 0.5, 8.0);\n" +
                "    color += vec3(0.8, 0.5, 1.0) * flash * isCore * 0.3;\n" +
                "    \n" +
                "    // ═══ ⚡⚡⚡ SOBRECARGA DE PLASMA ⚡⚡⚡ ═══\n" +
                "    if (u_Overcharge > 0.01) {\n" +
                "        // Plasma EXTREMO en todo el casco\n" +
                "        float overPlasma = noise(uv * 20.0 + u_Time * 4.0) * u_Overcharge;\n" +
                "        \n" +
                "        // Rayos de energía saliendo del núcleo\n" +
                "        float rays = sin(atan(v_LocalPos.x, v_LocalPos.z) * 8.0 + u_Time * 10.0);\n" +
                "        rays = pow(max(rays, 0.0), 2.0) * u_Overcharge;\n" +
                "        \n" +
                "        // Color de sobrecarga: blanco-púrpura intenso\n" +
                "        vec3 overchargeColor = vec3(1.0, 0.6, 1.0) * overPlasma * 0.8;\n" +
                "        vec3 rayColor = vec3(1.0, 0.8, 1.0) * rays * 0.5;\n" +
                "        \n" +
                "        color += overchargeColor + rayColor;\n" +
                "        \n" +
                "        // Pulso de energía rápido\n" +
                "        float fastPulse = sin(u_Time * 20.0) * 0.3 + 0.7;\n" +
                "        color *= (1.0 + u_Overcharge * fastPulse * 0.5);\n" +
                "        \n" +
                "        // Brillo general aumentado\n" +
                "        color += vec3(0.3, 0.1, 0.4) * u_Overcharge;\n" +
                "    }\n" +
                "    \n" +
                "    gl_FragColor = vec4(color, texColor.a * u_Alpha);\n" +
                "}";

        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vertexShader);
        GLES20.glAttachShader(shaderProgram, fragmentShader);
        GLES20.glLinkProgram(shaderProgram);

        aPositionHandle = GLES20.glGetAttribLocation(shaderProgram, "a_Position");
        aTexCoordHandle = GLES20.glGetAttribLocation(shaderProgram, "a_TexCoord");
        uMVPMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "u_MVPMatrix");
        uTextureHandle = GLES20.glGetUniformLocation(shaderProgram, "u_Texture");
        uTimeHandle = GLES20.glGetUniformLocation(shaderProgram, "u_Time");
        uAlphaHandle = GLES20.glGetUniformLocation(shaderProgram, "u_Alpha");
        uOverchargeHandle = GLES20.glGetUniformLocation(shaderProgram, "u_Overcharge");

        Log.d(TAG, "✓ Shaders creados (Motor de Plasma + Sobrecarga)");
    }

    private int compileShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "❌ Error compilando shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
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

    /**
     * 🎯 Establece el objetivo primario (DefenderShip)
     */
    public void setPrimaryTarget(DefenderShip target) {
        this.primaryTarget = target;
    }

    /**
     * 🎯 Establece el objetivo primario (método legacy)
     */
    public void setTarget(DefenderShip target) {
        this.primaryTarget = target;
    }

    /**
     * 🎯 Establece el objetivo secundario (HumanInterceptor)
     */
    public void setSecondaryTarget(HumanInterceptor target) {
        this.secondaryTarget = target;
    }

    /**
     * 🚧 Establece referencia al aliado UfoScout para evitar colisiones
     */
    public void setAllyScout(UfoScout ally) {
        this.allyScout = ally;
    }

    /**
     * 🎯 Obtiene el objetivo activo actual (primario si existe, sino secundario)
     */
    private Object getActiveTarget() {
        if (primaryTarget != null && !primaryTarget.isDestroyed()) {
            return primaryTarget;
        }
        if (secondaryTarget != null && !secondaryTarget.isDestroyed()) {
            return secondaryTarget;
        }
        return null;
    }

    /**
     * 🎯 Obtiene la posición X del objetivo activo
     */
    private float getTargetX() {
        Object target = getActiveTarget();
        if (target instanceof DefenderShip) return ((DefenderShip) target).x;
        if (target instanceof HumanInterceptor) return ((HumanInterceptor) target).getX();
        return x;
    }

    /**
     * 🎯 Obtiene la posición Y del objetivo activo
     */
    private float getTargetY() {
        Object target = getActiveTarget();
        if (target instanceof DefenderShip) return ((DefenderShip) target).y;
        if (target instanceof HumanInterceptor) return ((HumanInterceptor) target).getY();
        return y;
    }

    /**
     * 🎯 Obtiene la posición Z del objetivo activo
     */
    private float getTargetZ() {
        Object target = getActiveTarget();
        if (target instanceof DefenderShip) return ((DefenderShip) target).z;
        if (target instanceof HumanInterceptor) return ((HumanInterceptor) target).getZ();
        return z;
    }

    // Getters para colisiones
    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public float getScale() { return scale; }
    public float getCollisionRadius() { return scale * 0.6f; }
    public boolean isDestroyed() { return destroyed; }

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

        // ⚡ ACTUALIZAR SOBRECARGA DE PLASMA
        updatePlasmaOvercharge(deltaTime);

        // IA Agresiva
        updateAI(deltaTime);

        // Sistema de disparo
        updateShooting(deltaTime);

        // Movimiento
        updateMovement(deltaTime);
    }

    /**
     * ⚡ SISTEMA DE SOBRECARGA DE PLASMA
     */
    private void updatePlasmaOvercharge(float deltaTime) {
        // Reducir cooldown
        if (overchargeCooldown > 0) {
            overchargeCooldown -= deltaTime;
        }

        if (plasmaOverchargeActive) {
            // Sobrecarga activa
            overchargeTimer += deltaTime;

            // Intensidad visual con efecto de encendido/apagado
            if (overchargeTimer < 0.5f) {
                // Encendido rápido
                overchargeIntensity = overchargeTimer * 2f;
            } else if (overchargeTimer > overchargeDuration - 0.5f) {
                // Apagado gradual
                overchargeIntensity = (overchargeDuration - overchargeTimer) * 2f;
            } else {
                // Máxima intensidad con pulsos
                overchargeIntensity = 0.8f + (float)Math.sin(timeAccumulator * 15f) * 0.2f;
            }

            // Fin de sobrecarga
            if (overchargeTimer >= overchargeDuration) {
                plasmaOverchargeActive = false;
                overchargeTimer = 0f;
                overchargeCooldown = overchargeCooldownMax;
                overchargeIntensity = 0f;
                Log.d(TAG, "⚡ Sobrecarga de plasma TERMINADA");
            }
        } else {
            // Verificar si podemos activar sobrecarga
            overchargeIntensity = Math.max(0f, overchargeIntensity - deltaTime * 2f);

            // Activar sobrecarga automáticamente cuando está listo y en combate
            if (overchargeCooldown <= 0 && currentState == State.ATTACKING) {
                // 15% de probabilidad cada segundo de activar
                if (Math.random() < 0.15 * deltaTime) {
                    activatePlasmaOvercharge();
                }
            }
        }
    }

    /**
     * ⚡ Activa la sobrecarga de plasma
     */
    public void activatePlasmaOvercharge() {
        if (overchargeCooldown <= 0 && !plasmaOverchargeActive && !destroyed) {
            plasmaOverchargeActive = true;
            overchargeTimer = 0f;
            Log.d(TAG, "⚡⚡⚡ ¡SOBRECARGA DE PLASMA ACTIVADA! ⚡⚡⚡");
        }
    }

    /**
     * @return true si la sobrecarga está activa
     */
    public boolean isOverchargeActive() {
        return plasmaOverchargeActive;
    }

    private void updateAI(float deltaTime) {
        wanderTimer -= deltaTime;
        exploreTimer -= deltaTime;
        combatDisengageTimer += deltaTime;
        curvePhase += deltaTime * curveFrequency;

        Object activeTarget = getActiveTarget();

        // ═══════════════════════════════════════════════════════════════
        // 🌌 DECISIÓN: ¿Explorar o Combatir?
        // ═══════════════════════════════════════════════════════════════
        boolean shouldExplore = false;

        // Desengancharse del combate periódicamente
        if (combatDisengageTimer > COMBAT_DISENGAGE_TIME) {
            if (Math.random() < 0.35) {
                shouldExplore = true;
                combatDisengageTimer = 0f;
            } else {
                combatDisengageTimer = COMBAT_DISENGAGE_TIME * 0.6f;
            }
        }

        // Continuar explorando si ya está en ese modo
        if (currentState == State.EXPLORING || currentState == State.PATROLLING) {
            if (exploreTimer > 0) {
                shouldExplore = true;
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // 🛸 MODO EXPLORACIÓN / PATRULLA
        // ═══════════════════════════════════════════════════════════════
        if (shouldExplore || activeTarget == null) {
            updateExplorationBehavior(deltaTime);
            return;
        }

        // ═══════════════════════════════════════════════════════════════
        // ⚔️ MODO COMBATE
        // ═══════════════════════════════════════════════════════════════
        float tx = getTargetX();
        float ty = getTargetY();
        float tz = getTargetZ();

        float dx = tx - x;
        float dy = ty - y;
        float dz = tz - z;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist < retreatRange) {
            // Retroceder hacia cualquier dirección
            currentState = State.RETREATING;
            maxSpeed = 2.0f;
            float retreatAngle = (float)(Math.random() * Math.PI * 2);
            targetX = x + (float) Math.sin(retreatAngle) * 3f;
            targetY = y + (float)(Math.random() - 0.5) * 3f;
            targetZ = z + (float) Math.cos(retreatAngle) * 3f;

        } else if (dist < attackRange) {
            // ATACAR con movimientos más amplios
            currentState = State.ATTACKING;
            maxSpeed = 1.5f;

            // Movimiento en espiral alrededor del objetivo
            float spiralAngle = curvePhase * 0.5f;
            float spiralRadius = 1.5f + (float) Math.sin(curvePhase) * 0.8f;
            targetX = tx + (float) Math.sin(spiralAngle) * spiralRadius;
            targetZ = tz + (float) Math.cos(spiralAngle) * spiralRadius;
            // Variar altura durante ataque
            targetY = ty + (float) Math.sin(curvePhase * 0.4f) * 1.5f;

        } else {
            // CAZAR con movimientos amplios
            currentState = State.HUNTING;
            maxSpeed = 1.8f;

            if (wanderTimer <= 0) {
                wanderTimer = wanderInterval;
                targetX = tx + (float)(Math.random() - 0.5) * 4f;
                targetY = ty + (float)(Math.random() - 0.5) * 2f;
                targetZ = tz + (float)(Math.random() - 0.5) * 3f;
            }
        }

        // Clamp targets
        targetX = Math.max(-BOUND_X, Math.min(BOUND_X, targetX));
        targetY = Math.max(BOUND_Y_MIN, Math.min(BOUND_Y_MAX, targetY));
        targetZ = Math.max(BOUND_Z_MIN, Math.min(BOUND_Z_MAX, targetZ));
    }

    /**
     * 🌌 Comportamiento de exploración libre para UfoAttacker
     */
    private void updateExplorationBehavior(float deltaTime) {
        if (Math.random() < 0.5) {
            currentState = State.PATROLLING;
        } else {
            currentState = State.EXPLORING;
        }

        maxSpeed = 1.2f;  // Velocidad de crucero alienígena

        if (exploreTimer <= 0) {
            exploreTimer = exploreInterval + (float)(Math.random() * 4f);

            if (currentState == State.PATROLLING) {
                // Ir a un POI aleatorio
                currentPOI = (int)(Math.random() * POINTS_OF_INTEREST.length);
                float[] poi = POINTS_OF_INTEREST[currentPOI];
                targetX = poi[0] + (float)(Math.random() - 0.5) * 1.5f;
                targetY = poi[1] + (float)(Math.random() - 0.5) * 1.0f;
                targetZ = poi[2] + (float)(Math.random() - 0.5) * 1.5f;
            } else {
                // Movimiento libre
                targetX = (float)(Math.random() * BOUND_X * 2 - BOUND_X);
                targetY = (float)(Math.random() * (BOUND_Y_MAX - BOUND_Y_MIN) + BOUND_Y_MIN);
                targetZ = (float)(Math.random() * (BOUND_Z_MAX - BOUND_Z_MIN) + BOUND_Z_MIN);
            }
        }

        // Verificar si llegó
        float dx = targetX - x;
        float dy = targetY - y;
        float dz = targetZ - z;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist < 0.8f) {
            exploreTimer = 0f;
        }

        // Si enemigo está cerca, entrar en combate
        Object target = getActiveTarget();
        if (target != null) {
            float tdx = getTargetX() - x;
            float tdy = getTargetY() - y;
            float tdz = getTargetZ() - z;
            float targetDist = (float) Math.sqrt(tdx * tdx + tdy * tdy + tdz * tdz);

            if (targetDist < attackRange * 0.5f) {
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

        // ⚡ Calcular intervalo de disparo (más rápido durante sobrecarga)
        float currentShootInterval = shootInterval;
        if (plasmaOverchargeActive) {
            currentShootInterval = Math.max(OVERCHARGE_MIN_SHOOT, shootInterval * OVERCHARGE_SHOOT_MULT);
        }

        // Obtener objetivo activo
        Object target = getActiveTarget();

        // Disparar si hay objetivo válido
        if (target != null) {
            float tx = getTargetX();
            float ty = getTargetY();
            float tz = getTargetZ();

            boolean canShoot = (currentState == State.ATTACKING);

            // También disparar en modo HUNTING si está cerca
            if (currentState == State.HUNTING) {
                float dx = tx - x;
                float dy = ty - y;
                float dz = tz - z;
                float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (dist < attackRange * 0.8f) {
                    canShoot = true;
                }
            }

            // Durante sobrecarga, siempre puede disparar
            if (plasmaOverchargeActive) {
                canShoot = true;
            }

            if (canShoot && shootTimer >= currentShootInterval) {
                shootTimer = 0f;
                shootInterval = MIN_SHOOT_INTERVAL + (float)(Math.random() * (MAX_SHOOT_INTERVAL - MIN_SHOOT_INTERVAL));
                shootAt(tx, ty, tz);
            }
        }
    }

    private void shootAt(float tX, float tY, float tZ) {
        for (Laser laser : lasers) {
            if (!laser.isActive()) {
                laser.setCameraController(camera);
                laser.fire(x, y, z, tX, tY, tZ);
                Log.d(TAG, "🔫 UfoAttacker disparó láser hacia DefenderShip!");
                return;
            }
        }
    }

    private void updateLasers(float deltaTime) {
        for (Laser laser : lasers) {
            laser.update(deltaTime);

            if (laser.isActive()) {
                // Verificar colisión con DefenderShip (objetivo primario)
                if (primaryTarget != null && !primaryTarget.isDestroyed()) {
                    if (laser.checkCollision(primaryTarget.x, primaryTarget.y, primaryTarget.z, 0.35f)) {
                        laser.deactivate();
                        primaryTarget.takeDamage();
                        Log.d(TAG, "💥 ¡Láser de UfoAttacker impactó a DefenderShip!");
                        continue;
                    }
                }

                // Verificar colisión con HumanInterceptor (objetivo secundario)
                if (secondaryTarget != null && !secondaryTarget.isDestroyed()) {
                    if (laser.checkCollision(secondaryTarget.getX(), secondaryTarget.getY(), secondaryTarget.getZ(), 0.25f)) {
                        laser.deactivate();
                        secondaryTarget.takeDamage();
                        Log.d(TAG, "💥 ¡Láser de UfoAttacker impactó a HumanInterceptor!");
                    }
                }
            }
        }
    }

    public List<Laser> getLasers() {
        return lasers;
    }

    private void updateMovement(float deltaTime) {
        // ⚡ Multiplicador de velocidad durante sobrecarga
        float speedMult = plasmaOverchargeActive ? OVERCHARGE_SPEED_MULT : 1.0f;
        float currentMaxSpeed = maxSpeed * speedMult;
        float currentAccel = acceleration * speedMult;

        // ═══════════════════════════════════════════════════════════
        // 🚧 ANTI-COLISIÓN CON OTRAS NAVES
        // ═══════════════════════════════════════════════════════════
        float avoidX = 0, avoidY = 0, avoidZ = 0;

        // Evitar al aliado UfoScout
        if (allyScout != null && !allyScout.isDestroyed()) {
            float adx = x - allyScout.getX();
            float ady = y - allyScout.getY();
            float adz = z - allyScout.getZ();
            float adist = (float) Math.sqrt(adx * adx + ady * ady + adz * adz);

            if (adist < safeDistance && adist > 0.01f) {
                float pushForce = (safeDistance - adist) / adist * 3.0f;
                avoidX += adx * pushForce;
                avoidY += ady * pushForce;
                avoidZ += adz * pushForce;
            }
        }

        // Evitar a DefenderShip (enemigo)
        if (primaryTarget != null && !primaryTarget.isDestroyed()) {
            float adx = x - primaryTarget.x;
            float ady = y - primaryTarget.y;
            float adz = z - primaryTarget.z;
            float adist = (float) Math.sqrt(adx * adx + ady * ady + adz * adz);

            if (adist < safeDistance * 0.7f && adist > 0.01f) {
                float pushForce = (safeDistance * 0.7f - adist) / adist * 4.0f;
                avoidX += adx * pushForce;
                avoidY += ady * pushForce;
                avoidZ += adz * pushForce;
            }
        }

        // Evitar a HumanInterceptor (enemigo secundario)
        if (secondaryTarget != null && !secondaryTarget.isDestroyed()) {
            float adx = x - secondaryTarget.getX();
            float ady = y - secondaryTarget.getY();
            float adz = z - secondaryTarget.getZ();
            float adist = (float) Math.sqrt(adx * adx + ady * ady + adz * adz);

            if (adist < safeDistance * 0.7f && adist > 0.01f) {
                float pushForce = (safeDistance * 0.7f - adist) / adist * 4.0f;
                avoidX += adx * pushForce;
                avoidY += ady * pushForce;
                avoidZ += adz * pushForce;
            }
        }

        // Aplicar fuerza de evasión
        velocityX += avoidX * deltaTime * 5.0f;
        velocityY += avoidY * deltaTime * 5.0f;
        velocityZ += avoidZ * deltaTime * 5.0f;

        // ═══════════════════════════════════════════════════════════

        float dx = targetX - x;
        float dy = targetY - y;
        float dz = targetZ - z;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist > 0.05f) {
            float invDist = 1.0f / dist;
            float dirX = dx * invDist;
            float dirY = dy * invDist;
            float dirZ = dz * invDist;

            float accel = currentAccel * deltaTime;
            velocityX += dirX * accel;
            velocityY += dirY * accel;
            velocityZ += dirZ * accel;

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

        // Rotación hacia dirección de movimiento
        float speedXZ = (float) Math.sqrt(velocityX*velocityX + velocityZ*velocityZ);
        if (speedXZ > 0.01f) {
            float targetRotY = (float) Math.toDegrees(Math.atan2(velocityX, velocityZ));
            float deltaRot = targetRotY - rotationY;
            while (deltaRot > 180) deltaRot -= 360;
            while (deltaRot < -180) deltaRot += 360;
            rotationY += deltaRot * 2f * deltaTime;
        }

        // Inclinación
        float tiltFactor = 20f;
        float targetTiltX = -velocityZ * tiltFactor;
        float targetTiltZ = velocityX * tiltFactor;
        tiltX += (targetTiltX - tiltX) * 2.5f * deltaTime;
        tiltZ += (targetTiltZ - tiltZ) * 2.5f * deltaTime;

        // Clamp posición
        x = Math.max(-BOUND_X, Math.min(BOUND_X, x));
        y = Math.max(BOUND_Y_MIN, Math.min(BOUND_Y_MAX, y));
        z = Math.max(BOUND_Z_MIN, Math.min(BOUND_Z_MAX, z));

        // Escala dinámica según profundidad
        float zNormalized = (z - BOUND_Z_MIN) / (BOUND_Z_MAX - BOUND_Z_MIN);
        float scaleMultiplier = 0.7f + zNormalized * 0.6f;
        scale = baseScale * scaleMultiplier;
    }

    public void takeDamage() {
        if (destroyed || invincibilityTimer > 0) return;

        health--;
        invincibilityTimer = invincibilityDuration;

        Log.d(TAG, "💥 UfoAttacker recibió daño! HP: " + health + "/" + maxHealth);

        if (health <= 0) {
            destroy();
        }
    }

    private void destroy() {
        destroyed = true;
        respawnTimer = 0f;
        Log.d(TAG, "💀 UFO ATTACKER DESTRUIDO! Respawn en " + respawnDelay + " segundos");
    }

    private void respawn() {
        destroyed = false;
        health = maxHealth;
        respawnTimer = 0f;
        invincibilityTimer = 2.5f;

        // Respawn en posición alejada
        x = (float)(Math.random() * BOUND_X * 2 - BOUND_X);
        y = BOUND_Y_MAX - 0.5f;
        z = BOUND_Z_MIN + (float)(Math.random() * 1.5f);

        targetX = x;
        targetY = y;
        targetZ = z;

        Log.d(TAG, "🔄 UFO ATTACKER RESPAWN en (" + x + ", " + y + ", " + z + ")");
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
            if ((int)(invincibilityTimer * 8) % 2 == 0) {
                drawLasers();
                return;
            }
        }

        GLES20.glUseProgram(shaderProgram);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);
        Matrix.rotateM(modelMatrix, 0, rotationY, 0, 1, 0);
        Matrix.rotateM(modelMatrix, 0, tiltX, 1, 0, 0);
        Matrix.rotateM(modelMatrix, 0, tiltZ, 0, 0, 1);
        // Hover
        float hover = (float) Math.sin(timeAccumulator * 1.5f) * 0.04f;
        Matrix.translateM(modelMatrix, 0, 0, hover, 0);
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

        camera.computeMvp(modelMatrix, mvpMatrix);

        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform1f(uTimeHandle, timeAccumulator % 60.0f);
        GLES20.glUniform1f(uAlphaHandle, 1.0f);
        GLES20.glUniform1f(uOverchargeHandle, overchargeIntensity);  // ⚡ Sobrecarga

        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        if (uvBuffer != null && aTexCoordHandle >= 0) {
            uvBuffer.position(0);
            GLES20.glEnableVertexAttribArray(aTexCoordHandle);
            GLES20.glVertexAttribPointer(aTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, uvBuffer);
        }

        if (textureId > 0 && uTextureHandle >= 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(uTextureHandle, 0);
        }

        indexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_INT, indexBuffer);

        GLES20.glDisableVertexAttribArray(aPositionHandle);
        if (aTexCoordHandle >= 0) {
            GLES20.glDisableVertexAttribArray(aTexCoordHandle);
        }

        GLES20.glEnable(GLES20.GL_CULL_FACE);

        drawLasers();
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
        Log.d(TAG, "=== CLEANUP UfoAttacker ===");

        // Eliminar shader program
        if (shaderProgram != 0) {
            GLES20.glDeleteProgram(shaderProgram);
            Log.d(TAG, "  Shader program eliminado: " + shaderProgram);
            shaderProgram = 0;
        }

        // Eliminar textura
        if (textureId != 0) {
            GLES20.glDeleteTextures(1, new int[]{textureId}, 0);
            Log.d(TAG, "  Textura eliminada: " + textureId);
            textureId = 0;
        }

        // Limpiar buffers
        vertexBuffer = null;
        uvBuffer = null;
        indexBuffer = null;

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
