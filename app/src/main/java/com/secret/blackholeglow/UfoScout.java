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
 * 🛸 OVNI1 - UFO SCOUT - Nave Exploradora Alienígena
 * Con sistema de disparo láser verde (Team Alien)
 */
public class UfoScout implements SceneObject, CameraAware {
    private static final String TAG = "UfoScout";

    private final Context context;
    private final TextureLoader textureLoader;

    // Buffers del modelo (usando IntBuffer para modelos grandes)
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
    private int uAlphaHandle;  // Para efecto de teletransportación

    // Transformación
    public float x, y, z;
    private float scale;
    private float rotationY = 0f;
    private float tiltX = 0f;  // Inclinación al moverse
    private float tiltZ = 0f;

    // ═══════════════════════════════════════════════════════════════════════
    // 🧠 IA DE MOVIMIENTO ORGÁNICO E INTELIGENTE
    // ═══════════════════════════════════════════════════════════════════════
    private float velocityX = 0f, velocityY = 0f, velocityZ = 0f;
    private float targetX, targetY, targetZ;
    private float baseScale;  // Escala base para calcular perspectiva

    // Movimiento orgánico con aceleración suave
    private float moveSpeed = 0.8f;
    private float maxSpeed = 2.0f;
    private float acceleration = 1.5f;
    private float deceleration = 0.95f;  // Fricción

    // Timers para comportamiento variado
    private float wanderTimer = 0f;
    private float wanderInterval = 3.0f;
    private float depthChangeTimer = 0f;  // Para cambios de profundidad (Z)
    private float depthChangeInterval = 4.0f;

    // Curvas de vuelo orgánico (movimiento en arcos)
    private float curvePhase = 0f;
    private float curveAmplitude = 0.3f;
    private float curveFrequency = 1.5f;

    // Estados - Con PATROLLING para visitar POIs
    public enum State {
        EXPLORING,     // 🌌 Explorando libremente
        ATTACKING,     // Atacando
        EVADING,       // Evadiendo
        TELEPORTING,   // Teletransportándose
        CIRCLING,      // Circulando objetivo
        PATROLLING     // 🛸 Patrullando POIs
    }
    private State currentState = State.EXPLORING;

    // ═══════════════════════════════════════════════════════════════════════
    // 🎯 SISTEMA DE OBJETIVOS (Primario y Secundario)
    // ═══════════════════════════════════════════════════════════════════════
    private DefenderShip primaryTarget = null;      // Objetivo principal
    private HumanInterceptor secondaryTarget = null; // Objetivo secundario
    private float attackRange = 15f;
    private float evadeRange = 1.2f;
    private float circleRange = 3.0f;  // Distancia para orbitar al enemigo

    // ═══════════════════════════════════════════════════════════════════════
    // 🚧 REFERENCIAS A OTRAS NAVES (para evitar colisiones)
    // ═══════════════════════════════════════════════════════════════════════
    private UfoAttacker allyAttacker = null;
    private float safeDistance = 1.0f;  // Distancia mínima entre naves

    // ═══════════════════════════════════════════════════════════════════════
    // 💔 SISTEMA DE VIDA (HP)
    // ═══════════════════════════════════════════════════════════════════════
    private int health = 5;  // 5 HP para durar más
    private int maxHealth = 5;
    private boolean destroyed = false;
    private float respawnTimer = 0f;
    private float respawnDelay = 8.0f;  // 8 segundos para respawn
    private float invincibilityTimer = 0f;
    private float invincibilityDuration = 1.0f;

    // ═══════════════════════════════════════════════════════════════════════
    // 🌀 SISTEMA DE TELETRANSPORTACIÓN
    // ═══════════════════════════════════════════════════════════════════════
    private boolean isTeleporting = false;
    private float teleportTimer = 0f;
    private float teleportCooldown = 0f;
    private static final float TELEPORT_DURATION = 0.5f;  // Duración del efecto
    private static final float TELEPORT_COOLDOWN = 8f;    // Cooldown entre teletransportes
    private float teleportAlpha = 1.0f;  // Para efecto de desvanecimiento
    private float teleportDestX, teleportDestY, teleportDestZ;


    // Cámara
    private CameraController camera;

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // Tiempo acumulado para efectos
    private float timeAccumulator = 0f;

    // ═══════════════════════════════════════════════════════════════════════
    // 🌌 LÍMITES DE MOVIMIENTO EXPANDIDOS - TODA LA ESCENA
    // ═══════════════════════════════════════════════════════════════════════
    private static final float BOUND_X = 3.5f;        // Más ancho
    private static final float BOUND_Y_MIN = -2.5f;   // ¡Hasta el ecualizador!
    private static final float BOUND_Y_MAX = 3.5f;    // Hasta arriba
    private static final float BOUND_Z_MIN = -4.0f;   // Más profundidad
    private static final float BOUND_Z_MAX = 3.0f;    // Más cerca de cámara

    // ═══════════════════════════════════════════════════════════════════════
    // 🎯 PUNTOS DE INTERÉS (POIs) - Lugares a explorar
    // ═══════════════════════════════════════════════════════════════════════
    private static final float[][] POINTS_OF_INTEREST = {
        {-2.5f, 2.5f, -1.5f},    // 🛰️ Cerca de la estación espacial
        {3.0f, 2.0f, -2.0f},     // ☀️ Cerca del sol
        {-0.5f, 0.5f, 0.0f},     // 🌍 Cerca de la Tierra
        {-2.5f, -2.0f, 2.0f},    // 🎵 Zona del ecualizador (izquierda)
        {2.5f, -2.0f, 2.0f},     // 🎵 Zona del ecualizador (derecha)
        {0.0f, -2.3f, 2.5f},     // 🎵 Centro del ecualizador (frente)
        {-3.0f, 0.5f, -3.5f},    // 🌌 Galaxias lejanas
        {3.0f, 0.5f, -3.5f},     // 🌌 Galaxias lejanas
        {0.0f, 3.2f, 0.0f},      // ⬆️ Zona superior
        {-2.0f, 1.0f, 2.0f},     // 📍 Frente izquierda
        {2.0f, 1.0f, 2.0f},      // 📍 Frente derecha
    };
    private int currentPOI = 0;
    private float poiTimer = 0f;
    private float poiInterval = 4.0f;

    // Referencia a la Tierra para colisiones láser
    private float earthX = 0f, earthY = 0f, earthZ = 0f, earthRadius = 1.0f;

    // ═══════════════════════════════════════════════════════════════════════
    // 🔫 SISTEMA DE DISPAROS (TEAM ALIEN - LÁSER VERDE)
    // ═══════════════════════════════════════════════════════════════════════
    private static final int MAX_LASERS = 8;
    private final List<Laser> lasers = new ArrayList<>();
    private float shootTimer = 0f;
    private float shootInterval = 1.0f;  // Dispara cada 1 segundo
    private static final float MIN_SHOOT_INTERVAL = 0.7f;
    private static final float MAX_SHOOT_INTERVAL = 1.5f;

    /**
     * Constructor
     */
    public UfoScout(Context context, TextureLoader textureLoader,
                    float x, float y, float z, float scale) {
        this.context = context;
        this.textureLoader = textureLoader;
        this.x = x;
        this.y = y;
        this.z = z;
        this.scale = scale;
        this.baseScale = scale;  // Guardar escala original
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Log.d(TAG, "🛸 Creando UFO Scout");
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        loadModel();
        loadTexture();
        createShaders();
        initLasers();

        Log.d(TAG, "✅ UFO Scout creado en (" + x + ", " + y + ", " + z + ")");
    }

    /**
     * 🔫 Inicializa el pool de láseres
     */
    private void initLasers() {
        for (int i = 0; i < MAX_LASERS; i++) {
            lasers.add(new Laser(Laser.TEAM_ALIEN));
        }
        // Intervalo inicial aleatorio
        shootInterval = MIN_SHOOT_INTERVAL + (float)(Math.random() * (MAX_SHOOT_INTERVAL - MIN_SHOOT_INTERVAL));
        Log.d(TAG, "🔫 Sistema de láseres inicializado (" + MAX_LASERS + " láseres)");
    }

    /**
     * Carga el modelo ufo_scout.obj (mismo patrón que SpaceStation)
     */
    private void loadModel() {
        try {
            Log.d(TAG, "📦 Cargando ufo_scout.obj...");

            ObjLoader.Mesh mesh = ObjLoader.loadObj(context, "ufo_scout.obj");

            Log.d(TAG, "✓ Modelo cargado:");
            Log.d(TAG, "  Vértices: " + mesh.vertexCount);
            Log.d(TAG, "  Caras: " + mesh.faces.size());

            // Usar buffers directamente del ObjLoader
            this.vertexBuffer = mesh.vertexBuffer;
            this.uvBuffer = mesh.uvBuffer;

            // Construir buffer de índices con fan triangulation
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

            // Crear IntBuffer para los índices (NO ShortBuffer)
            ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 4);
            ibb.order(ByteOrder.nativeOrder());
            indexBuffer = ibb.asIntBuffer();
            indexBuffer.put(indices);
            indexBuffer.position(0);

            Log.d(TAG, "  Índices: " + indexCount);

        } catch (IOException e) {
            Log.e(TAG, "❌ Error cargando ufo_scout.obj", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Carga la textura
     */
    private void loadTexture() {
        Log.d(TAG, "🎨 Cargando textura ufo_scout_texture...");

        int textureResId = context.getResources().getIdentifier(
                "ufo_scout_texture", "drawable", context.getPackageName());

        if (textureResId != 0) {
            textureId = textureLoader.getTexture(textureResId);
            Log.d(TAG, "✓ Textura cargada (ID=" + textureId + ")");
        } else {
            Log.e(TAG, "❌ Textura no encontrada: ufo_scout_texture");
            textureId = 0;
        }
    }

    /**
     * 🛸✨ SHADER CON EFECTOS ALIENÍGENAS
     */
    private void createShaders() {
        Log.d(TAG, "🎨 Creando shaders...");

        // Vertex Shader
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

        // Fragment Shader con brillo alienígena y efecto de teletransportación
        String fragmentShaderCode =
                "precision mediump float;\n" +
                "\n" +
                "uniform sampler2D u_Texture;\n" +
                "uniform float u_Time;\n" +
                "uniform float u_Alpha;\n" +  // Para teletransportación
                "\n" +
                "varying vec2 v_TexCoord;\n" +
                "varying vec3 v_LocalPos;\n" +
                "\n" +
                "void main() {\n" +
                "    // Invertir V para corregir orientación de textura (Meshy)\n" +
                "    vec2 uv = vec2(v_TexCoord.x, 1.0 - v_TexCoord.y);\n" +
                "    vec4 texColor = texture2D(u_Texture, uv);\n" +
                "    vec3 color = texColor.rgb;\n" +
                "    \n" +
                "    // ═══ 🛸 BRILLO ALIENÍGENA CYAN/VERDE ═══\n" +
                "    float pulse = sin(u_Time * 3.0) * 0.3 + 0.7;\n" +
                "    float glow = sin(u_Time * 5.0 + v_LocalPos.y * 10.0) * 0.15 + 0.85;\n" +
                "    \n" +
                "    // Detectar áreas claras/metálicas para aplicar brillo\n" +
                "    float brightness = dot(color, vec3(0.299, 0.587, 0.114));\n" +
                "    \n" +
                "    // Añadir tinte cyan/verde alienígena\n" +
                "    if (brightness > 0.3) {\n" +
                "        color.g += 0.08 * pulse;\n" +
                "        color.b += 0.12 * pulse;\n" +
                "    }\n" +
                "    \n" +
                "    // Rim light alienígena en los bordes\n" +
                "    float rim = 1.0 - abs(v_LocalPos.y);\n" +
                "    rim = pow(rim, 2.0);\n" +
                "    color += vec3(0.1, 0.4, 0.3) * rim * glow * 0.5;\n" +
                "    \n" +
                "    // 🌀 Efecto de teletransportación (brillo cyan cuando desvanece)\n" +
                "    if (u_Alpha < 0.9) {\n" +
                "        color += vec3(0.2, 0.8, 1.0) * (1.0 - u_Alpha) * 0.5;\n" +
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

        // Obtener handles
        aPositionHandle = GLES20.glGetAttribLocation(shaderProgram, "a_Position");
        aTexCoordHandle = GLES20.glGetAttribLocation(shaderProgram, "a_TexCoord");
        uMVPMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "u_MVPMatrix");
        uTextureHandle = GLES20.glGetUniformLocation(shaderProgram, "u_Texture");
        uTimeHandle = GLES20.glGetUniformLocation(shaderProgram, "u_Time");
        uAlphaHandle = GLES20.glGetUniformLocation(shaderProgram, "u_Alpha");

        Log.d(TAG, "✓ Shaders creados (con soporte de teletransportación)");
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

    /**
     * Configura la referencia a la Tierra para colisiones láser
     */
    public void setEarthReference(float ex, float ey, float ez, float radius) {
        this.earthX = ex;
        this.earthY = ey;
        this.earthZ = ez;
        this.earthRadius = radius;
    }

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
     * 🚧 Establece referencia al aliado UfoAttacker para evitar colisiones
     */
    public void setAllyAttacker(UfoAttacker ally) {
        this.allyAttacker = ally;
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

    // ═══════════════════════════════════════════════════════════════════════
    // 📍 GETTERS PARA COLISIONES
    // ═══════════════════════════════════════════════════════════════════════

    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public float getScale() { return scale; }

    public float getCollisionRadius() {
        return scale * 0.5f;
    }

    @Override
    public void update(float deltaTime) {
        timeAccumulator += deltaTime;

        // ═══ 🔫 ACTUALIZAR LÁSERES (siempre, incluso durante teletransporte) ═══
        updateLasers(deltaTime);

        // ═══ 💀 SI ESTÁ DESTRUIDO, MANEJAR RESPAWN ═══
        if (destroyed) {
            respawnTimer += deltaTime;
            if (respawnTimer >= respawnDelay) {
                respawn();
            }
            return;
        }

        // ═══ 💔 ACTUALIZAR INVENCIBILIDAD ═══
        if (invincibilityTimer > 0) {
            invincibilityTimer -= deltaTime;
        }

        // ═══ 🌀 SISTEMA DE TELETRANSPORTACIÓN ═══
        updateTeleport(deltaTime);

        // Si está teletransportándose, no hacer nada más
        if (isTeleporting) return;

        // ═══ 🧠 IA DE COMPORTAMIENTO ═══
        updateAI(deltaTime);

        // ═══ 🔫 SISTEMA DE DISPARO ═══
        updateShooting(deltaTime);

        // ═══ 🛸 MOVIMIENTO SUAVE ═══
        updateMovement(deltaTime);
    }

    /**
     * 💔 Recibe daño de un láser
     */
    public void takeDamage() {
        if (destroyed || invincibilityTimer > 0) return;

        health--;
        invincibilityTimer = invincibilityDuration;

        Log.d(TAG, "💥 OVNI1 recibió daño! HP: " + health + "/" + maxHealth);

        if (health <= 0) {
            destroy();
        }
    }

    /**
     * 💀 Destruye el OVNI
     */
    private void destroy() {
        destroyed = true;
        respawnTimer = 0f;
        Log.d(TAG, "💀 OVNI1 DESTRUIDO! Respawn en " + respawnDelay + " segundos");
    }

    /**
     * 🔄 Respawn del OVNI
     */
    private void respawn() {
        destroyed = false;
        health = maxHealth;
        respawnTimer = 0f;
        invincibilityTimer = 2.0f;  // 2 segundos de invencibilidad al respawn
        teleportAlpha = 1.0f;

        // Posición aleatoria lejos del enemigo
        x = (float)(Math.random() * BOUND_X * 2 - BOUND_X);
        y = (float)(Math.random() * (BOUND_Y_MAX - BOUND_Y_MIN) + BOUND_Y_MIN);
        z = BOUND_Z_MIN + (float)(Math.random() * 1.5f);  // Aparece lejos

        targetX = x;
        targetY = y;
        targetZ = z;

        Log.d(TAG, "🔄 OVNI1 RESPAWN en (" + x + ", " + y + ", " + z + ")");
    }

    /**
     * 💔 Verifica si está destruido
     */
    public boolean isDestroyed() {
        return destroyed;
    }

    /**
     * 🔫 Actualiza el sistema de disparo
     */
    private void updateShooting(float deltaTime) {
        shootTimer += deltaTime;

        // Obtener objetivo activo (primario o secundario)
        Object target = getActiveTarget();

        // Disparar si hay objetivo válido
        if (target != null) {
            float tx = getTargetX();
            float ty = getTargetY();
            float tz = getTargetZ();

            // Disparar en modos: ATTACKING, CIRCLING, y EXPLORING (si está cerca)
            boolean canShoot = (currentState == State.ATTACKING || currentState == State.CIRCLING);

            // También disparar en modo EXPLORING si está dentro del rango de ataque
            if (currentState == State.EXPLORING) {
                float dx = tx - x;
                float dy = ty - y;
                float dz = tz - z;
                float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (dist < attackRange) {
                    canShoot = true;
                }
            }

            if (canShoot && shootTimer >= shootInterval) {
                shootTimer = 0f;
                // Nuevo intervalo aleatorio
                shootInterval = MIN_SHOOT_INTERVAL + (float)(Math.random() * (MAX_SHOOT_INTERVAL - MIN_SHOOT_INTERVAL));

                // Disparar al objetivo activo
                shootAt(tx, ty, tz);
            }
        }
    }

    /**
     * 🔫 Dispara un láser hacia una posición
     */
    private void shootAt(float targetX, float targetY, float targetZ) {
        // Buscar láser inactivo
        for (Laser laser : lasers) {
            if (!laser.isActive()) {
                laser.setCameraController(camera);
                laser.fire(x, y, z, targetX, targetY, targetZ);
                Log.d(TAG, "🔫 OVNI1 disparó láser verde hacia NAVE1!");
                return;
            }
        }
    }

    /**
     * 🔫 Actualiza todos los láseres (activos y efectos de impacto)
     */
    private void updateLasers(float deltaTime) {
        for (Laser laser : lasers) {
            // Siempre actualizar para efectos de impacto
            laser.update(deltaTime);

            // Solo verificar colisiones si está activo
            if (laser.isActive()) {
                // Verificar colisión con DefenderShip (objetivo primario)
                if (primaryTarget != null && !primaryTarget.isDestroyed()) {
                    if (laser.checkCollision(primaryTarget.x, primaryTarget.y, primaryTarget.z, 0.3f)) {
                        laser.deactivate();
                        primaryTarget.takeDamage();
                        Log.d(TAG, "💥 ¡Láser verde impactó a DefenderShip!");
                        continue;  // No verificar más colisiones para este láser
                    }
                }

                // Verificar colisión con HumanInterceptor (objetivo secundario)
                if (secondaryTarget != null && !secondaryTarget.isDestroyed()) {
                    if (laser.checkCollision(secondaryTarget.getX(), secondaryTarget.getY(), secondaryTarget.getZ(), 0.25f)) {
                        laser.deactivate();
                        secondaryTarget.takeDamage();
                        Log.d(TAG, "💥 ¡Láser verde impactó a HumanInterceptor!");
                    }
                }
            }
        }
    }

    /**
     * 🔫 Obtiene la lista de láseres (para que otras clases puedan verificar colisiones)
     */
    public List<Laser> getLasers() {
        return lasers;
    }

    /**
     * 🌀 Actualiza el sistema de teletransportación
     */
    private void updateTeleport(float deltaTime) {
        teleportCooldown -= deltaTime;

        if (isTeleporting) {
            teleportTimer += deltaTime;

            // Efecto de desvanecimiento
            if (teleportTimer < TELEPORT_DURATION / 2) {
                // Desvanecer (fade out)
                teleportAlpha = 1.0f - (teleportTimer / (TELEPORT_DURATION / 2));
            } else if (teleportTimer < TELEPORT_DURATION) {
                // Aparecer en nueva posición (fade in)
                if (teleportTimer >= TELEPORT_DURATION / 2 && teleportAlpha < 0.1f) {
                    // Teletransportar a nueva posición
                    x = teleportDestX;
                    y = teleportDestY;
                    z = teleportDestZ;
                    targetX = x;
                    targetY = y;
                    targetZ = z;
                }
                teleportAlpha = (teleportTimer - TELEPORT_DURATION / 2) / (TELEPORT_DURATION / 2);
            } else {
                // Terminar teletransportación
                isTeleporting = false;
                teleportTimer = 0f;
                teleportAlpha = 1.0f;
                teleportCooldown = TELEPORT_COOLDOWN;
                currentState = State.EXPLORING;
                Log.d(TAG, "🌀 OVNI1 apareció en (" + x + ", " + y + ", " + z + ")");
            }
        } else if (teleportCooldown <= 0 && shouldTeleport()) {
            // Iniciar teletransportación
            startTeleport();
        }
    }

    /**
     * Determina si el OVNI debe teletransportarse
     */
    private boolean shouldTeleport() {
        // Teletransportarse cuando está siendo perseguido muy de cerca
        // o aleatoriamente para confundir
        if (currentState == State.EVADING) {
            return Math.random() < 0.02;  // 2% chance cada frame cuando evade
        }
        return Math.random() < 0.001;  // 0.1% chance cada frame normalmente
    }

    /**
     * Inicia el proceso de teletransportación
     */
    private void startTeleport() {
        isTeleporting = true;
        teleportTimer = 0f;
        currentState = State.TELEPORTING;

        // Calcular destino aleatorio en el espacio 3D
        teleportDestX = (float)(Math.random() * BOUND_X * 2 - BOUND_X);
        teleportDestY = (float)(Math.random() * (BOUND_Y_MAX - BOUND_Y_MIN) + BOUND_Y_MIN);
        teleportDestZ = (float)(Math.random() * (BOUND_Z_MAX - BOUND_Z_MIN) + BOUND_Z_MIN);

        // Evitar teletransportarse muy cerca del objetivo activo
        Object activeTarget = getActiveTarget();
        if (activeTarget != null) {
            float tx = getTargetX();
            float ty = getTargetY();
            float tz = getTargetZ();

            float dx = teleportDestX - tx;
            float dy = teleportDestY - ty;
            float dz = teleportDestZ - tz;
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (dist < 2.0f) {
                // Ajustar destino más lejos
                teleportDestX = tx + (float)(Math.random() > 0.5 ? 3 : -3);
                teleportDestZ = tz + (float)(Math.random() > 0.5 ? 3 : -3);
            }
        }

        Log.d(TAG, "🌀 OVNI1 teletransportándose a (" + teleportDestX + ", " + teleportDestY + ", " + teleportDestZ + ")");
    }

    /**
     * Verifica si está teletransportándose
     */
    public boolean isTeleportingNow() {
        return isTeleporting;
    }

    /**
     * 🧠 Actualiza la IA de comportamiento ORGÁNICO
     */
    private void updateAI(float deltaTime) {
        wanderTimer -= deltaTime;
        depthChangeTimer -= deltaTime;
        curvePhase += deltaTime * curveFrequency;

        // Usar sistema de objetivos dinámico
        Object activeTarget = getActiveTarget();

        if (activeTarget != null) {
            float tx = getTargetX();
            float ty = getTargetY();
            float tz = getTargetZ();

            float dx = tx - x;
            float dy = ty - y;
            float dz = tz - z;
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (dist < evadeRange) {
                // ═══ EVADIR - Alejarse rápidamente ═══
                currentState = State.EVADING;
                maxSpeed = 2.5f;  // Más rápido evadiendo

                // Escapar en dirección opuesta con curva
                float escapeAngle = (float) Math.atan2(-dx, -dz) + (float)(Math.random() - 0.5) * 1.5f;
                targetX = x + (float) Math.sin(escapeAngle) * 3f;
                targetZ = z + (float) Math.cos(escapeAngle) * 3f;
                targetY = y + (float)(Math.random() - 0.5) * 2f;

            } else if (dist < circleRange) {
                // ═══ ORBITAR - Rodear al enemigo mientras dispara ═══
                currentState = State.CIRCLING;
                maxSpeed = 1.5f;

                // Movimiento circular alrededor del objetivo
                float circleAngle = (float) Math.atan2(x - tx, z - tz);
                circleAngle += deltaTime * 1.2f;  // Rotar alrededor

                float orbitRadius = 2.0f + (float) Math.sin(curvePhase) * 0.5f;
                targetX = tx + (float) Math.sin(circleAngle) * orbitRadius;
                targetZ = tz + (float) Math.cos(circleAngle) * orbitRadius;
                targetY = ty + 0.3f + (float) Math.sin(curvePhase * 0.7f) * 0.4f;

            } else if (dist < attackRange) {
                // ═══ ATACAR - Acercarse en zigzag ═══
                currentState = State.ATTACKING;
                maxSpeed = 1.8f;

                if (wanderTimer <= 0) {
                    wanderTimer = 1.5f + (float)(Math.random() * 1.5f);

                    // Zigzag hacia el objetivo
                    float zigzag = (float)(Math.random() - 0.5) * 2.5f;
                    targetX = tx + zigzag;
                    targetZ = tz + (float)(Math.random() - 0.5) * 2f;
                    targetY = ty + (float)(Math.random()) * 1.2f;
                }
            } else {
                // ═══ EXPLORAR - Vuelo libre orgánico ═══
                currentState = State.EXPLORING;
                maxSpeed = 1.2f;

                if (wanderTimer <= 0) {
                    wanderTimer = wanderInterval + (float)(Math.random() * 2f);

                    // Destino aleatorio con preferencia hacia el objetivo
                    float bias = 0.3f;  // 30% de bias hacia el enemigo
                    targetX = x + (tx - x) * bias + (float)(Math.random() - 0.5) * 4f;
                    targetY = (float)(Math.random() * (BOUND_Y_MAX - BOUND_Y_MIN) + BOUND_Y_MIN);
                    targetZ = z + (tz - z) * bias + (float)(Math.random() - 0.5) * 4f;
                }
            }
        } else {
            // ═══ SIN OBJETIVO - Exploración libre con POIs ═══
            poiTimer -= deltaTime;
            maxSpeed = 1.2f;

            if (poiTimer <= 0) {
                poiTimer = poiInterval + (float)(Math.random() * 3f);

                // 60% ir a POI, 40% movimiento libre
                if (Math.random() < 0.6) {
                    currentState = State.PATROLLING;
                    currentPOI = (int)(Math.random() * POINTS_OF_INTEREST.length);
                    float[] poi = POINTS_OF_INTEREST[currentPOI];
                    targetX = poi[0] + (float)(Math.random() - 0.5) * 1.0f;
                    targetY = poi[1] + (float)(Math.random() - 0.5) * 0.8f;
                    targetZ = poi[2] + (float)(Math.random() - 0.5) * 1.0f;
                } else {
                    currentState = State.EXPLORING;
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
            if (dist < 0.8f) {
                poiTimer = 0f;  // Elegir nuevo destino
            }
        }

        // ═══ CAMBIO DE PROFUNDIDAD (Z) - Acercarse/Alejarse de la cámara ═══
        if (depthChangeTimer <= 0) {
            depthChangeTimer = depthChangeInterval + (float)(Math.random() * 3f);
            // Cambiar profundidad dramáticamente
            targetZ = BOUND_Z_MIN + (float)(Math.random() * (BOUND_Z_MAX - BOUND_Z_MIN));
        }

        // Clamp targets a límites
        targetX = Math.max(-BOUND_X, Math.min(BOUND_X, targetX));
        targetY = Math.max(BOUND_Y_MIN, Math.min(BOUND_Y_MAX, targetY));
        targetZ = Math.max(BOUND_Z_MIN, Math.min(BOUND_Z_MAX, targetZ));
    }

    /**
     * 🚀 Actualiza el movimiento ORGÁNICO con aceleración y curvas
     */
    private void updateMovement(float deltaTime) {
        // ═══════════════════════════════════════════════════════════
        // 🚧 ANTI-COLISIÓN CON OTRAS NAVES
        // ═══════════════════════════════════════════════════════════
        float avoidX = 0, avoidY = 0, avoidZ = 0;

        // Evitar al aliado UfoAttacker
        if (allyAttacker != null && !allyAttacker.isDestroyed()) {
            float adx = x - allyAttacker.getX();
            float ady = y - allyAttacker.getY();
            float adz = z - allyAttacker.getZ();
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

        // Aplicar fuerza de evasión a la velocidad
        velocityX += avoidX * deltaTime * 5.0f;
        velocityY += avoidY * deltaTime * 5.0f;
        velocityZ += avoidZ * deltaTime * 5.0f;

        // ═══════════════════════════════════════════════════════════

        float dx = targetX - x;
        float dy = targetY - y;
        float dz = targetZ - z;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist > 0.05f) {
            // Normalizar dirección
            float invDist = 1.0f / dist;
            float dirX = dx * invDist;
            float dirY = dy * invDist;
            float dirZ = dz * invDist;

            // Agregar curva orgánica al movimiento
            float curveOffset = (float) Math.sin(curvePhase) * curveAmplitude;
            // Perpendicular en XZ
            float perpX = -dirZ * curveOffset;
            float perpZ = dirX * curveOffset;

            // Acelerar hacia el objetivo con curva
            float accel = acceleration * deltaTime;
            velocityX += (dirX + perpX) * accel;
            velocityY += dirY * accel;
            velocityZ += (dirZ + perpZ) * accel;

            // Limitar velocidad máxima
            float currentSpeed = (float) Math.sqrt(velocityX*velocityX + velocityY*velocityY + velocityZ*velocityZ);
            if (currentSpeed > maxSpeed) {
                float scale = maxSpeed / currentSpeed;
                velocityX *= scale;
                velocityY *= scale;
                velocityZ *= scale;
            }
        }

        // Aplicar fricción/desaceleración
        velocityX *= deceleration;
        velocityY *= deceleration;
        velocityZ *= deceleration;

        // Aplicar velocidad a posición
        x += velocityX * deltaTime;
        y += velocityY * deltaTime;
        z += velocityZ * deltaTime;

        // ═══ ROTACIÓN SUAVE hacia dirección de movimiento ═══
        float speedXZ = (float) Math.sqrt(velocityX*velocityX + velocityZ*velocityZ);
        if (speedXZ > 0.01f) {
            float targetRotY = (float) Math.toDegrees(Math.atan2(velocityX, velocityZ));
            float deltaRot = targetRotY - rotationY;
            while (deltaRot > 180) deltaRot -= 360;
            while (deltaRot < -180) deltaRot += 360;
            rotationY += deltaRot * 2.5f * deltaTime;  // Rotación más suave
        }

        // ═══ INCLINACIÓN DINÁMICA según velocidad ═══
        float tiltFactor = 25f;
        float targetTiltX = -velocityZ * tiltFactor;
        float targetTiltZ = velocityX * tiltFactor;
        tiltX += (targetTiltX - tiltX) * 3f * deltaTime;
        tiltZ += (targetTiltZ - tiltZ) * 3f * deltaTime;

        // Clamp posición
        x = Math.max(-BOUND_X, Math.min(BOUND_X, x));
        y = Math.max(BOUND_Y_MIN, Math.min(BOUND_Y_MAX, y));
        z = Math.max(BOUND_Z_MIN, Math.min(BOUND_Z_MAX, z));

        // ═══ ESCALA DINÁMICA según profundidad (Z) ═══
        // Más cerca (Z mayor) = más grande, más lejos (Z menor) = más pequeño
        float zNormalized = (z - BOUND_Z_MIN) / (BOUND_Z_MAX - BOUND_Z_MIN);  // 0 a 1
        float scaleMultiplier = 0.6f + zNormalized * 0.8f;  // 0.6 a 1.4
        scale = baseScale * scaleMultiplier;
    }

    @Override
    public void draw() {
        if (camera == null || shaderProgram == 0) return;

        // No dibujar si está destruido
        if (destroyed) {
            // Solo dibujar láseres activos
            drawLasers();
            return;
        }

        // ═══ EFECTO PARPADEO DURANTE INVENCIBILIDAD ═══
        if (invincibilityTimer > 0) {
            // Parpadear rápido (5Hz)
            if ((int)(invincibilityTimer * 10) % 2 == 0) {
                drawLasers();
                return;  // No dibujar el modelo este frame
            }
        }

        // ═══ DIBUJAR UFO ═══
        GLES20.glUseProgram(shaderProgram);

        // Desactivar face culling (modelos Meshy)
        GLES20.glDisable(GLES20.GL_CULL_FACE);

        // Habilitar blending y depth test
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Construir matriz modelo
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);
        Matrix.rotateM(modelMatrix, 0, rotationY, 0, 1, 0);
        Matrix.rotateM(modelMatrix, 0, tiltX, 1, 0, 0);
        Matrix.rotateM(modelMatrix, 0, tiltZ, 0, 0, 1);
        // Hover flotante
        float hover = (float) Math.sin(timeAccumulator * 2f) * 0.03f;
        Matrix.translateM(modelMatrix, 0, 0, hover, 0);
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

        // Calcular MVP
        camera.computeMvp(modelMatrix, mvpMatrix);

        // Pasar uniforms
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform1f(uTimeHandle, timeAccumulator % 60.0f);
        GLES20.glUniform1f(uAlphaHandle, teleportAlpha);  // Alpha para teletransportación

        // Configurar atributos de vértices
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        // Configurar UVs
        if (uvBuffer != null && aTexCoordHandle >= 0) {
            uvBuffer.position(0);
            GLES20.glEnableVertexAttribArray(aTexCoordHandle);
            GLES20.glVertexAttribPointer(aTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, uvBuffer);
        }

        // Bind textura
        if (textureId > 0 && uTextureHandle >= 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(uTextureHandle, 0);
        }

        // Usar GL_UNSIGNED_INT (para modelos grandes)
        indexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_INT, indexBuffer);

        // Limpiar
        GLES20.glDisableVertexAttribArray(aPositionHandle);
        if (aTexCoordHandle >= 0) {
            GLES20.glDisableVertexAttribArray(aTexCoordHandle);
        }

        // Restaurar culling
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // ═══ 🔫 DIBUJAR LÁSERES ═══
        drawLasers();
    }

    /**
     * 🔫 Dibuja todos los láseres activos y sus efectos
     */
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
        Log.d(TAG, "=== CLEANUP UfoScout ===");

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
