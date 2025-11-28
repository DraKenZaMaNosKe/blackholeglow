// Spaceship3D.java - Nave espacial 3D desde Blender
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
import java.nio.ShortBuffer;
import java.util.List;
import java.util.Random;

/**
 * Spaceship3D - Nave espacial 3D cargada desde Spaceships.obj
 * CON TEXTURAS
 */
public class Spaceship3D implements SceneObject, CameraAware {
    private static final String TAG = "Spaceship3D";

    private final Context context;
    private final TextureLoader textureLoader;

    // Buffers del modelo
    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;
    private ShortBuffer indexBuffer;
    private int indexCount;

    // Textura
    private int textureId;

    // Shader
    private int shaderProgram;
    private int aPositionHandle;
    private int aTexCoordHandle;
    private int uMVPMatrixHandle;
    private int uTextureHandle;
    private int uTimeHandle;         // ✨ Para animaciones
    private int uModelMatrixHandle;  // ✨ Para calcular WorldPos

    // Transformación
    public float x, y, z;
    public float scale;
    public float rotationY = 0f;

    // ═══════════════════════════════════════════════════════════
    // 🛸 SISTEMA DE EXPLORACIÓN LIBRE CON IA
    // ═══════════════════════════════════════════════════════════

    // Velocidad y dirección
    private float velocityX = 0.3f;
    private float velocityY = 0.1f;
    private float velocityZ = -0.2f;
    private float currentSpeed = 0.4f;
    private float maxSpeed = 0.6f;
    private float minSpeed = 0.2f;

    // Comportamiento orgánico
    private float directionChangeTimer = 0f;
    private float directionChangeInterval = 3.0f;
    private float wanderAngle = 0f;             // Ángulo de deambulación suave

    // 🌍 LÍMITES VISIBLES (para pantalla portrait)
    private float minX = -2.0f;
    private float maxX = 2.0f;
    private float minY = -1.8f;
    private float maxY = 2.5f;
    private float minZ = -3.0f;
    private float maxZ = 2.0f;

    // 🌍 POSICIÓN DE LA TIERRA (para esquivarla y dispararle)
    private float earthX = 0f, earthY = 1.8f, earthZ = 0f;
    private float earthRadius = 1.2f;           // Radio de seguridad de la Tierra
    private float safeDistanceEarth = 2.5f;     // Distancia mínima a la Tierra

    // ☀️ POSICIÓN DEL SOL (para esquivarlo)
    private float sunX = 0f, sunY = -1.0f, sunZ = 0f;
    private float sunRadius = 0.8f;             // Radio de seguridad del Sol
    private float safeDistanceSun = 1.8f;       // Distancia mínima al Sol

    // ✨ TELETRANSPORTACIÓN
    private float teleportTimer = 0f;
    private float teleportInterval = 12.0f;     // Cada 12 segundos (varía)
    private float minTeleportInterval = 8.0f;
    private float maxTeleportInterval = 18.0f;
    private boolean isTeleporting = false;
    private float teleportFadeTimer = 0f;
    private float teleportFadeDuration = 0.5f;  // Duración del fade in/out
    private float teleportAlpha = 1.0f;         // Para efecto de fade

    // 👀 ACERCAMIENTO A CÁMARA (fly-by dramático)
    private float cameraApproachTimer = 0f;
    private float cameraApproachInterval = 25.0f;  // Cada 25 segundos
    private boolean isApproachingCamera = false;
    private float approachDuration = 3.0f;
    private float approachTimer = 0f;
    private float approachStartZ = 0f;

    // 🔫 SISTEMA DE ARMAS
    private java.util.ArrayList<UfoLaser> lasers = new java.util.ArrayList<>();
    private float shootTimer = 0f;
    private float shootInterval = 4.0f;         // Disparar cada 4 segundos (varía)
    private float minShootInterval = 3.0f;
    private float maxShootInterval = 7.0f;
    private CameraController cameraRef;         // Para pasar MVP a láseres

    // 💔 SISTEMA DE VIDA
    private int health = 3;                     // 3 golpes = destruido
    private boolean destroyed = false;
    private float respawnTimer = 0f;
    private float respawnDelay = 8.0f;          // Reaparece después de 8 segundos
    private float invincibilityTimer = 0f;      // Invencibilidad después de golpe
    private float invincibilityDuration = 1.5f;

    // 💥 EXPLOSIÓN
    private boolean exploding = false;
    private float explosionTimer = 0f;
    private float explosionDuration = 1.0f;

    // 🌍 Referencia al escudo para impactos
    private EarthShield earthShieldRef;

    // Cámara
    private CameraController camera;

    // Matrices
    private float[] modelMatrix = new float[16];
    private float[] mvpMatrix = new float[16];

    // ⚡ OPTIMIZACIÓN: Matrices estáticas para láseres (evita allocaciones en draw)
    private final float[] laserMvp = new float[16];
    private final float[] identityModel = new float[16];

    // ✅ CRÍTICO: Tiempo relativo para evitar overflow
    private final long startTime = System.currentTimeMillis();

    // ⚡ OPTIMIZACIÓN: Random reutilizable (evita Math.random() costoso)
    private final Random random = new Random();
    private float randomCache1 = 0f, randomCache2 = 0f;  // Cache de valores random
    private int frameCounter = 0;  // Para actualizar random cada N frames

    /**
     * Constructor
     */
    public Spaceship3D(Context context, TextureLoader textureLoader,
                       float x, float y, float z, float scale) {
        this.context = context;
        this.textureLoader = textureLoader;
        this.x = x;
        this.y = y;
        this.z = z;
        this.scale = scale;

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Log.d(TAG, "🛸 Creando Spaceship3D CON TEXTURAS");
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // 1. Cargar modelo
        loadModel();

        // 2. Cargar textura
        loadTexture();

        // 3. Crear shaders
        createShaders();

        Log.d(TAG, "✅ Spaceship3D creado exitosamente");
        Log.d(TAG, "   Posición: (" + x + ", " + y + ", " + z + ")");
        Log.d(TAG, "   Escala: " + scale);
    }

    /**
     * Carga el modelo Spaceships.obj
     */
    private void loadModel() {
        try {
            Log.d(TAG, "📦 Cargando Spaceships.obj...");

            ObjLoader.Mesh mesh = ObjLoader.loadObj(context, "Spaceships.obj");

            Log.d(TAG, "✓ Modelo cargado:");
            Log.d(TAG, "  Vértices: " + mesh.vertexCount);
            Log.d(TAG, "  Caras: " + mesh.faces.size());

            // CENTRAR EL MODELO EN EL ORIGEN
            // Calcular bounding box
            mesh.vertexBuffer.position(0);
            float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
            float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

            for (int i = 0; i < mesh.vertexCount * 3; i += 3) {
                float vx = mesh.vertexBuffer.get(i);
                float vy = mesh.vertexBuffer.get(i + 1);
                float vz = mesh.vertexBuffer.get(i + 2);
                minX = Math.min(minX, vx); maxX = Math.max(maxX, vx);
                minY = Math.min(minY, vy); maxY = Math.max(maxY, vy);
                minZ = Math.min(minZ, vz); maxZ = Math.max(maxZ, vz);
            }

            float centerX = (minX + maxX) / 2f;
            float centerY = (minY + maxY) / 2f;
            float centerZ = (minZ + maxZ) / 2f;

            Log.d(TAG, "  Centro original: (" + centerX + ", " + centerY + ", " + centerZ + ")");
            Log.d(TAG, "  Trasladando al origen...");

            // Trasladar todos los vértices al origen
            for (int i = 0; i < mesh.vertexCount * 3; i += 3) {
                mesh.vertexBuffer.put(i + 0, mesh.vertexBuffer.get(i + 0) - centerX);
                mesh.vertexBuffer.put(i + 1, mesh.vertexBuffer.get(i + 1) - centerY);
                mesh.vertexBuffer.put(i + 2, mesh.vertexBuffer.get(i + 2) - centerZ);
            }
            mesh.vertexBuffer.position(0);

            Log.d(TAG, "  ✓ Modelo centrado en el origen");

            // GENERAR UVs AUTOMÁTICAMENTE (proyección planar desde arriba)
            // El modelo no tiene UVs, así que los generamos
            Log.d(TAG, "  Generando UVs automáticos (proyección planar XZ)...");

            // Primero encontrar el rango de X y Z para normalizar
            float minXuv = Float.MAX_VALUE, maxXuv = -Float.MAX_VALUE;
            float minZuv = Float.MAX_VALUE, maxZuv = -Float.MAX_VALUE;

            mesh.vertexBuffer.position(0);
            for (int i = 0; i < mesh.vertexCount; i++) {
                float vx = mesh.vertexBuffer.get(i * 3 + 0);
                float vz = mesh.vertexBuffer.get(i * 3 + 2);
                minXuv = Math.min(minXuv, vx);
                maxXuv = Math.max(maxXuv, vx);
                minZuv = Math.min(minZuv, vz);
                maxZuv = Math.max(maxZuv, vz);
            }

            float rangeX = maxXuv - minXuv;
            float rangeZ = maxZuv - minZuv;
            float maxRange = Math.max(rangeX, rangeZ);

            FloatBuffer autoUVs = ByteBuffer
                    .allocateDirect(mesh.vertexCount * 2 * Float.BYTES)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();

            mesh.vertexBuffer.position(0);
            for (int i = 0; i < mesh.vertexCount; i++) {
                float vx = mesh.vertexBuffer.get(i * 3 + 0);
                float vz = mesh.vertexBuffer.get(i * 3 + 2);

                // Proyección planar: mapear X,Z a U,V (vista desde arriba)
                // Centrar y normalizar para que la textura circular quede centrada
                float u = 0.5f + (vx / maxRange);
                float v = 0.5f + (vz / maxRange);

                autoUVs.put(u);
                autoUVs.put(v);
            }
            autoUVs.position(0);

            Log.d(TAG, "  ✓ UVs generados (planar XZ, rango=" + maxRange + ")");

            // Copiar buffers
            vertexBuffer = mesh.vertexBuffer;
            texCoordBuffer = autoUVs;

            // Crear index buffer (triangular las caras usando fan triangulation)
            List<int[]> faces = mesh.faces;  // ✅ int[] para compatibilidad con modelos grandes
            int triangles = 0;
            for (int[] face : faces) {
                triangles += face.length - 2;
            }
            indexCount = triangles * 3;

            ShortBuffer ib = ByteBuffer
                    .allocateDirect(indexCount * Short.BYTES)
                    .order(ByteOrder.nativeOrder())
                    .asShortBuffer();

            for (int[] face : faces) {
                short v0 = (short) face[0];  // ✅ Cast a short (modelo pequeño <32k vértices)
                for (int k = 1; k < face.length - 1; k++) {
                    ib.put(v0).put((short) face[k]).put((short) face[k + 1]);
                }
            }
            ib.position(0);
            indexBuffer = ib;

            Log.d(TAG, "  Índices: " + indexCount);

        } catch (IOException e) {
            Log.e(TAG, "❌ Error cargando Spaceships.obj", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Carga la textura forerunnercentralplates
     */
    private void loadTexture() {
        Log.d(TAG, "🎨 Cargando textura forerunnercentralplates...");

        int textureResId = context.getResources().getIdentifier(
                "forerunnercentralplates", "drawable", context.getPackageName());

        if (textureResId != 0) {
            textureId = textureLoader.getTexture(textureResId);
            Log.d(TAG, "✓ Textura cargada: forerunnercentralplates (ID=" + textureId + ")");
        } else {
            Log.e(TAG, "❌ Textura no encontrada: forerunnercentralplates");
            textureId = 0;
        }
    }

    /**
     * 🛸✨ SHADER ÉPICO CON EFECTOS ALIEN
     */
    private void createShaders() {
        Log.d(TAG, "🎨 Creando shaders ÉPICOS con efectos alien...");

        // ═══════════════════════════════════════════════════════════════
        // VERTEX SHADER - Calcula posición mundial
        // ═══════════════════════════════════════════════════════════════
        String vertexShaderCode =
                "attribute vec4 a_Position;\n" +
                "attribute vec2 a_TexCoord;\n" +
                "uniform mat4 u_MVPMatrix;\n" +
                "uniform mat4 u_ModelMatrix;\n" +
                "varying vec2 v_TexCoord;\n" +
                "varying vec3 v_WorldPos;\n" +
                "varying vec3 v_Normal;\n" +
                "void main() {\n" +
                "  gl_Position = u_MVPMatrix * a_Position;\n" +
                "  v_TexCoord = a_TexCoord;\n" +
                "  \n" +
                "  // Posición en espacio mundial\n" +
                "  vec4 worldPos = u_ModelMatrix * a_Position;\n" +
                "  v_WorldPos = worldPos.xyz;\n" +
                "  \n" +
                "  // Normal aproximada (para OVNI plano: usar Y)\n" +
                "  v_Normal = normalize(a_Position.xyz);\n" +
                "}";

        // ═══════════════════════════════════════════════════════════════
        // FRAGMENT SHADER ÉPICO - Efectos alien
        // ═══════════════════════════════════════════════════════════════
        String fragmentShaderCode =
                "precision mediump float;\n" +
                "\n" +
                "uniform sampler2D u_Texture;\n" +
                "uniform float u_Time;\n" +
                "\n" +
                "varying vec2 v_TexCoord;\n" +
                "varying vec3 v_WorldPos;\n" +
                "varying vec3 v_Normal;\n" +
                "\n" +
                // ─── Función de noise ────
                "float noise(vec2 st) {\n" +
                "    return fract(sin(dot(st, vec2(12.9898, 78.233))) * 43758.5453);\n" +
                "}\n" +
                "\n" +
                "void main() {\n" +
                "    // Color base de la textura\n" +
                "    vec4 texColor = texture2D(u_Texture, v_TexCoord);\n" +
                "    vec3 baseColor = texColor.rgb * 1.8;  // Brillo base\n" +
                "    \n" +
                "    // ═══ 1. 💎 CÚPULA MEJORADA (energía pulsante) ═══\n" +
                "    float cupulaFactor = smoothstep(0.1, 0.6, v_Normal.y);  // Rango más amplio\n" +
                "    if (cupulaFactor > 0.05) {\n" +
                "        // Colores de energía alien (verde-cyan con toques azules)\n" +
                "        vec3 color1 = vec3(0.0, 1.0, 0.8);   // Verde-cyan\n" +
                "        vec3 color2 = vec3(0.2, 0.8, 1.0);   // Azul cielo\n" +
                "        \n" +
                "        // Pulso de energía (más dramático)\n" +
                "        float pulse = sin(u_Time * 2.0) * 0.5 + 0.5;\n" +
                "        vec3 glowColor = mix(color1, color2, pulse);\n" +
                "        \n" +
                "        // Ondas de energía radiales\n" +
                "        float distFromCenter = length(v_WorldPos.xz);\n" +
                "        float waves = sin(distFromCenter * 12.0 - u_Time * 4.0) * 0.3 + 0.7;\n" +
                "        \n" +
                "        // Intensidad total\n" +
                "        float glowIntensity = (2.0 + pulse * 0.8) * waves;\n" +
                "        baseColor += glowColor * cupulaFactor * glowIntensity;\n" +
                "    }\n" +
                "    \n" +
                "    // ═══ 2. ✨ LUCES MEJORADAS (siempre visibles) ═══\n" +
                "    // Calcular ángulo en el plano XZ (normalizado 0 a 1)\n" +
                "    float angle = atan(v_WorldPos.x, v_WorldPos.z);\n" +
                "    float normalizedAngle = (angle + 3.14159) / 6.28318;  // 0 a 1\n" +
                "    \n" +
                "    // Fase rotante\n" +
                "    float lightPhase = fract(normalizedAngle - u_Time * 0.3);  // Rotar más lento\n" +
                "    \n" +
                "    // 8 luces con transición suave (arreglado para que no desaparezcan)\n" +
                "    float lightPattern = fract(lightPhase * 8.0);  // 0 a 1 repetido 8 veces\n" +
                "    float lightIntensity = smoothstep(0.75, 1.0, lightPattern);  // ✅ Pico brillante simple\n" +
                "    \n" +
                "    // Solo en el borde (Y cercano a 0)\n" +
                "    float bordeFactor = 1.0 - smoothstep(0.0, 0.3, abs(v_Normal.y));\n" +
                "    if (bordeFactor > 0.3) {\n" +
                "        // Naranja dorado brillante con pulsación\n" +
                "        float lightPulse = sin(u_Time * 5.0) * 0.3 + 1.0;\n" +
                "        baseColor += vec3(1.0, 0.6, 0.1) * lightIntensity * bordeFactor * 5.0 * lightPulse;  // ✅ Aumentado de 3.0 a 5.0\n" +
                "    }\n" +
                "    \n" +
                "    // ═══ 3. 🔦 HAZ DE LUZ TRACTORA (solo en parte inferior) ═══\n" +
                "    if (v_Normal.y < -0.3) {  // Y- = abajo\n" +
                "        // Distancia del centro en XZ\n" +
                "        float distFromCenter = length(v_WorldPos.xz);\n" +
                "        float beamIntensity = smoothstep(1.5, 0.0, distFromCenter);\n" +
                "        \n" +
                "        // Animación de ondas\n" +
                "        float beamWave = sin(u_Time * 4.0 + distFromCenter * 10.0) * 0.5 + 0.5;\n" +
                "        beamIntensity *= beamWave * 0.6;\n" +
                "        \n" +
                "        vec3 beamColor = vec3(0.3, 1.0, 0.7);  // Verde-azul alien\n" +
                "        baseColor += beamColor * beamIntensity;\n" +
                "    }\n" +
                "    \n" +
                "    // ═══ 4. 🌀 ANILLO DE ENERGÍA ALREDEDOR ═══\n" +
                "    float distFromOVNI = length(v_WorldPos);\n" +
                "    float ringPulse = sin(distFromOVNI * 8.0 - u_Time * 6.0) * 0.5 + 0.5;\n" +
                "    float ringIntensity = ringPulse * smoothstep(2.0, 1.2, distFromOVNI);\n" +
                "    ringIntensity *= smoothstep(1.0, 1.2, distFromOVNI);  // Solo en el anillo\n" +
                "    \n" +
                "    baseColor += vec3(0.4, 0.6, 1.0) * ringIntensity * 0.4;\n" +
                "    \n" +
                "    gl_FragColor = vec4(baseColor, texColor.a);\n" +
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
        uModelMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "u_ModelMatrix");
        uTextureHandle = GLES20.glGetUniformLocation(shaderProgram, "u_Texture");
        uTimeHandle = GLES20.glGetUniformLocation(shaderProgram, "u_Time");

        Log.d(TAG, "✓ Shaders ÉPICOS creados con efectos alien (program=" + shaderProgram + ")");
        Log.d(TAG, "  💡 Glow en cúpula");
        Log.d(TAG, "  ✨ Luces parpadeantes");
        Log.d(TAG, "  🔦 Haz de luz tractora");
        Log.d(TAG, "  🌀 Anillo de energía");
    }

    private int compileShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        // Verificar compilación
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
        Log.d(TAG, "📷 Cámara asignada");
    }

    @Override
    public void update(float deltaTime) {
        // ═══════════════════════════════════════════════════════════
        // 🛸 EXPLORACIÓN LIBRE CON IA INTELIGENTE (OPTIMIZADO)
        // ═══════════════════════════════════════════════════════════

        if (destroyed) return;

        // ⚡ OPTIMIZACIÓN: Actualizar cache de random cada 10 frames
        frameCounter++;
        if (frameCounter >= 10) {
            frameCounter = 0;
            randomCache1 = random.nextFloat() - 0.5f;
            randomCache2 = random.nextFloat() - 0.5f;
        }

        // ═══════════════════════════════════════════════════════════
        // ✨ SISTEMA DE TELETRANSPORTACIÓN
        // ═══════════════════════════════════════════════════════════
        if (isTeleporting) {
            teleportFadeTimer += deltaTime;
            float phase = teleportFadeTimer / teleportFadeDuration;

            if (phase < 0.5f) {
                // Fase 1: Fade out (desaparecer)
                teleportAlpha = 1.0f - (phase * 2.0f);
            } else if (phase < 0.6f) {
                // Fase 2: Teletransportar a nueva posición
                if (teleportAlpha <= 0.01f) {
                    teleportToRandomPosition();
                }
                teleportAlpha = 0f;
            } else {
                // Fase 3: Fade in (aparecer)
                teleportAlpha = (phase - 0.5f) * 2.0f;
                if (phase >= 1.0f) {
                    isTeleporting = false;
                    teleportAlpha = 1.0f;
                    teleportInterval = minTeleportInterval + random.nextFloat() * (maxTeleportInterval - minTeleportInterval);
                    Log.d(TAG, "✨ Teletransportación completada!");
                }
            }
            return; // No hacer otros updates durante teletransportación
        }

        // Timer para próxima teletransportación
        teleportTimer += deltaTime;
        if (teleportTimer >= teleportInterval) {
            isTeleporting = true;
            teleportFadeTimer = 0f;
            teleportTimer = 0f;
            Log.d(TAG, "✨ Iniciando teletransportación...");
        }

        // ═══════════════════════════════════════════════════════════
        // 👀 ACERCAMIENTO A CÁMARA (fly-by dramático)
        // ═══════════════════════════════════════════════════════════
        cameraApproachTimer += deltaTime;
        if (!isApproachingCamera && cameraApproachTimer >= cameraApproachInterval) {
            isApproachingCamera = true;
            approachTimer = 0f;
            approachStartZ = z;
            cameraApproachTimer = 0f;
            cameraApproachInterval = 20.0f + random.nextFloat() * 15.0f;
            Log.d(TAG, "👀 OVNI iniciando fly-by hacia la cámara!");
        }

        if (isApproachingCamera) {
            approachTimer += deltaTime;
            float phase = approachTimer / approachDuration;

            if (phase < 1.0f) {
                // Volar hacia la cámara (Z aumenta hacia 5.0)
                float targetZ = 4.5f;  // Muy cerca de la cámara (en Z=6)
                z = approachStartZ + (targetZ - approachStartZ) * phase;

                // Reducir X e Y para centrarse en pantalla
                x *= 0.98f;
                y = y * 0.98f + 2.0f * 0.02f;  // Hacia el centro-arriba

                // Aumentar escala un poco para efecto dramático
                // (se maneja en draw())
            } else {
                // Terminar approach, volver a explorar
                isApproachingCamera = false;
                z = 1.5f;  // Volver a posición normal
                Log.d(TAG, "👀 Fly-by completado, volviendo a explorar");
            }
        }

        // 1️⃣ DEAMBULACIÓN ORGÁNICA (cambio gradual de dirección)
        if (!isApproachingCamera) {
            wanderAngle += randomCache1 * 2.0f * deltaTime;
            velocityX += (float) Math.cos(wanderAngle) * 0.1f * deltaTime;
            velocityZ += (float) Math.sin(wanderAngle) * 0.1f * deltaTime;
            velocityY += randomCache2 * 0.05f * deltaTime;
        }

        // 2️⃣ CAMBIO DE DIRECCIÓN PERIÓDICO
        directionChangeTimer += deltaTime;
        if (directionChangeTimer >= directionChangeInterval && !isApproachingCamera) {
            float angle = random.nextFloat() * (float) (Math.PI * 2);
            float elevation = (random.nextFloat() - 0.5f) * 0.5f;
            velocityX = (float) Math.cos(angle) * currentSpeed;
            velocityZ = (float) Math.sin(angle) * currentSpeed;
            velocityY = elevation * currentSpeed;

            directionChangeTimer = 0f;
            directionChangeInterval = 2.0f + random.nextFloat() * 3.0f;
        }

        // 3️⃣ 🌍 ESQUIVAR LA TIERRA (CRÍTICO - nunca atravesar)
        float dx = x - earthX;
        float dy = y - earthY;
        float dz = z - earthZ;
        float distToEarth = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distToEarth < safeDistanceEarth) {
            float escapeForce = (safeDistanceEarth - distToEarth) / safeDistanceEarth;
            escapeForce = escapeForce * escapeForce * 4.0f;

            if (distToEarth > 0.01f) {
                velocityX += (dx / distToEarth) * escapeForce;
                velocityY += (dy / distToEarth) * escapeForce;
                velocityZ += (dz / distToEarth) * escapeForce;
            }

            if (distToEarth < earthRadius + 0.5f) {
                float safeRadius = safeDistanceEarth + 0.5f;
                x = earthX + (dx / distToEarth) * safeRadius;
                y = earthY + (dy / distToEarth) * safeRadius;
                z = earthZ + (dz / distToEarth) * safeRadius;
            }
        }

        // 4️⃣ ☀️ ESQUIVAR EL SOL (igual de importante)
        float dxSun = x - sunX;
        float dySun = y - sunY;
        float dzSun = z - sunZ;
        float distToSun = (float) Math.sqrt(dxSun * dxSun + dySun * dySun + dzSun * dzSun);

        if (distToSun < safeDistanceSun) {
            float escapeForce = (safeDistanceSun - distToSun) / safeDistanceSun;
            escapeForce = escapeForce * escapeForce * 4.0f;

            if (distToSun > 0.01f) {
                velocityX += (dxSun / distToSun) * escapeForce;
                velocityY += (dySun / distToSun) * escapeForce;
                velocityZ += (dzSun / distToSun) * escapeForce;
            }

            if (distToSun < sunRadius + 0.3f) {
                float safeRadius = safeDistanceSun + 0.3f;
                x = sunX + (dxSun / distToSun) * safeRadius;
                y = sunY + (dySun / distToSun) * safeRadius;
                z = sunZ + (dzSun / distToSun) * safeRadius;
            }
        }

        // 5️⃣ NORMALIZAR VELOCIDAD
        float speed = (float) Math.sqrt(velocityX * velocityX + velocityY * velocityY + velocityZ * velocityZ);
        if (speed > maxSpeed) {
            velocityX = (velocityX / speed) * maxSpeed;
            velocityY = (velocityY / speed) * maxSpeed;
            velocityZ = (velocityZ / speed) * maxSpeed;
        } else if (speed < minSpeed && speed > 0.01f) {
            velocityX = (velocityX / speed) * minSpeed;
            velocityY = (velocityY / speed) * minSpeed;
            velocityZ = (velocityZ / speed) * minSpeed;
        }

        // 6️⃣ APLICAR MOVIMIENTO (solo si no está en fly-by)
        if (!isApproachingCamera) {
            x += velocityX * deltaTime;
            y += velocityY * deltaTime;
            z += velocityZ * deltaTime;
        }

        // 7️⃣ REBOTE SUAVE EN LÍMITES DE PANTALLA
        if (x < minX) { x = minX; velocityX = Math.abs(velocityX) * 0.8f; }
        if (x > maxX) { x = maxX; velocityX = -Math.abs(velocityX) * 0.8f; }
        if (y < minY) { y = minY; velocityY = Math.abs(velocityY) * 0.8f; }
        if (y > maxY) { y = maxY; velocityY = -Math.abs(velocityY) * 0.8f; }
        if (z < minZ) { z = minZ; velocityZ = Math.abs(velocityZ) * 0.8f; }
        if (z > maxZ) { z = maxZ; velocityZ = -Math.abs(velocityZ) * 0.8f; }

        // 8️⃣ ROTACIÓN - Mira hacia donde va
        if (speed > 0.01f) {
            float targetRotation = (float) Math.toDegrees(Math.atan2(velocityX, velocityZ));
            float rotDiff = targetRotation - rotationY;
            while (rotDiff > 180) rotDiff -= 360;
            while (rotDiff < -180) rotDiff += 360;
            rotationY += rotDiff * 2.0f * deltaTime;
        }

        // 9️⃣ 🔫 SISTEMA DE DISPARO AUTOMÁTICO
        shootTimer += deltaTime;
        if (shootTimer >= shootInterval && !isApproachingCamera) {
            shootLaser();
            shootTimer = 0f;
            shootInterval = minShootInterval + random.nextFloat() * (maxShootInterval - minShootInterval);
        }

        // 🔟 ACTUALIZAR LÁSERES
        for (int i = lasers.size() - 1; i >= 0; i--) {
            UfoLaser laser = lasers.get(i);
            laser.update(deltaTime, earthX, earthY, earthZ, earthRadius);

            if (laser.hitTarget && earthShieldRef != null) {
                earthShieldRef.registerImpact(laser.x, laser.y, laser.z);
            }

            if (!laser.active) {
                lasers.remove(i);
            }
        }

        // 1️⃣1️⃣ INVENCIBILIDAD POST-GOLPE
        if (invincibilityTimer > 0) {
            invincibilityTimer -= deltaTime;
        }
    }

    /**
     * ✨ Teletransportar a posición aleatoria segura
     */
    private void teleportToRandomPosition() {
        // Generar posición aleatoria
        float newX, newY, newZ;
        int attempts = 0;

        do {
            newX = minX + random.nextFloat() * (maxX - minX);
            newY = minY + 0.5f + random.nextFloat() * (maxY - minY - 1.0f);  // Evitar extremos
            newZ = minZ + random.nextFloat() * (maxZ - minZ);
            attempts++;

            // Verificar distancia a Tierra y Sol
            float distEarth = (float) Math.sqrt(
                (newX - earthX) * (newX - earthX) +
                (newY - earthY) * (newY - earthY) +
                (newZ - earthZ) * (newZ - earthZ));

            float distSun = (float) Math.sqrt(
                (newX - sunX) * (newX - sunX) +
                (newY - sunY) * (newY - sunY) +
                (newZ - sunZ) * (newZ - sunZ));

            if (distEarth > safeDistanceEarth && distSun > safeDistanceSun) {
                break;  // Posición válida
            }
        } while (attempts < 20);

        x = newX;
        y = newY;
        z = newZ;

        // Nueva velocidad aleatoria
        float angle = random.nextFloat() * (float) (Math.PI * 2);
        velocityX = (float) Math.cos(angle) * currentSpeed;
        velocityZ = (float) Math.sin(angle) * currentSpeed;
        velocityY = (random.nextFloat() - 0.5f) * currentSpeed * 0.5f;

        Log.d(TAG, "✨ Teletransportado a: (" + x + ", " + y + ", " + z + ")");
    }

    /**
     * 🔫 Disparar láser hacia la Tierra
     */
    private void shootLaser() {
        if (destroyed) return;

        // Crear láser desde la posición actual hacia la Tierra
        UfoLaser laser = new UfoLaser(x, y - 0.05f, z, earthX, earthY, earthZ);
        lasers.add(laser);
        Log.d(TAG, "🔫 OVNI disparó láser! Total activos: " + lasers.size());
    }

    /**
     * 💔 Recibir daño de meteorito
     */
    public void takeDamage() {
        if (destroyed || invincibilityTimer > 0) return;

        health--;
        invincibilityTimer = invincibilityDuration;
        Log.d(TAG, "💔 OVNI golpeado! HP restante: " + health);

        if (health <= 0) {
            destroyed = true;
            exploding = true;
            explosionTimer = 0f;
            Log.d(TAG, "💥 OVNI DESTRUIDO!");
        }
    }

    /**
     * 🔄 Reaparcer OVNI después de destrucción
     */
    public void respawn() {
        destroyed = false;
        exploding = false;
        health = 3;
        invincibilityTimer = invincibilityDuration;

        // Posición aleatoria segura
        x = (random.nextFloat() - 0.5f) * 3f;
        y = 1.5f + random.nextFloat();
        z = -1f + random.nextFloat();

        Log.d(TAG, "🛸 OVNI reapareció con 3 HP");
    }

    /**
     * 🌍 Establecer referencia al escudo de la Tierra
     */
    public void setEarthShield(EarthShield shield) {
        this.earthShieldRef = shield;
    }

    /**
     * 📍 Verificar colisión con un meteorito (OPTIMIZADO)
     * ⚡ Usa distancia al cuadrado para evitar sqrt
     */
    public boolean checkMeteorCollision(float mx, float my, float mz, float mRadius) {
        if (destroyed || invincibilityTimer > 0) return false;

        float dx = x - mx;
        float dy = y - my;
        float dz = z - mz;
        float distSq = dx * dx + dy * dy + dz * dz;

        // ⚡ Comparar distancias al cuadrado (evita sqrt)
        float collisionRadius = mRadius + scale * 0.5f;
        return distSq < (collisionRadius * collisionRadius);
    }

    /**
     * 🎯 Obtener láseres activos para dibujar
     */
    public java.util.ArrayList<UfoLaser> getLasers() {
        return lasers;
    }

    /**
     * ❓ Verificar si el OVNI está destruido
     */
    public boolean isDestroyed() {
        return destroyed;
    }

    /**
     * ❓ Verificar si el OVNI está explotando
     */
    public boolean isExploding() {
        return exploding;
    }

    /**
     * 🌍 Establece la posición de la Tierra (para esquivarla)
     */
    public void setEarthPosition(float ex, float ey, float ez) {
        this.earthX = ex;
        this.earthY = ey;
        this.earthZ = ez;
        Log.d(TAG, "🌍 Posición de Tierra para esquivar: (" + ex + ", " + ey + ", " + ez + ")");
    }

    /**
     * ☀️ Establece la posición del Sol (para esquivarlo)
     */
    public void setSunPosition(float sx, float sy, float sz) {
        this.sunX = sx;
        this.sunY = sy;
        this.sunZ = sz;
        Log.d(TAG, "☀️ Posición de Sol para esquivar: (" + sx + ", " + sy + ", " + sz + ")");
    }

    /**
     * ⚙️ Configurar parámetros de exploración
     */
    public void setOrbitParams(float radius, float speed, float height) {
        // Convertido a parámetros de exploración
        this.safeDistanceEarth = radius + 0.5f;  // Distancia segura a la Tierra
        this.maxSpeed = speed * 2.0f;
        this.currentSpeed = speed;
        Log.d(TAG, "🛸 Exploración configurada: safeDistanceEarth=" + safeDistanceEarth + ", speed=" + speed);
    }

    @Override
    public void draw() {
        if (camera == null) return;

        // Guardar referencia a la cámara para los láseres
        this.cameraRef = camera;

        // 🔫 DIBUJAR LÁSERES (siempre, incluso si OVNI destruido)
        // ⚡ OPTIMIZACIÓN: Usa matrices de instancia (sin allocaciones)
        Matrix.setIdentityM(identityModel, 0);
        camera.computeMvp(identityModel, laserMvp);

        for (UfoLaser laser : lasers) {
            if (laser.active) {
                laser.draw(laserMvp);
            }
        }

        // No dibujar OVNI si está destruido
        if (destroyed) return;

        // ✨ No dibujar si está invisible (teletransportación)
        if (teleportAlpha <= 0.01f) return;

        // Parpadeo durante invencibilidad
        if (invincibilityTimer > 0) {
            // Parpadear rápido (no dibujar en frames alternos)
            if ((int)(invincibilityTimer * 10) % 2 == 0) return;
        }

        // Deshabilitar face culling (para ver todas las caras)
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // ✨ Habilitar blending para efecto de teletransportación
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Usar shader
        GLES20.glUseProgram(shaderProgram);

        // 👀 Calcular escala dinámica (más grande cuando se acerca a cámara)
        float dynamicScale = scale;
        if (isApproachingCamera) {
            float approachPhase = approachTimer / approachDuration;
            dynamicScale = scale * (1.0f + approachPhase * 0.8f);  // Hasta 80% más grande
        }

        // ✨ Efecto de escala durante teletransportación
        if (isTeleporting) {
            dynamicScale *= teleportAlpha;  // Encoge/crece con el fade
        }

        // Construir matriz modelo
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);
        if (rotationY != 0f) {
            Matrix.rotateM(modelMatrix, 0, rotationY, 0, 1, 0);
        }
        Matrix.scaleM(modelMatrix, 0, dynamicScale, dynamicScale, dynamicScale);

        // Calcular MVP
        camera.computeMvp(modelMatrix, mvpMatrix);

        // ✨ Pasar uniforms al shader
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniformMatrix4fv(uModelMatrixHandle, 1, false, modelMatrix, 0);

        // ✨ Tiempo relativo CÍCLICO (CRÍTICO: evita overflow Y pérdida de precisión en GLSL)
        float currentTime = ((System.currentTimeMillis() - startTime) / 1000.0f) % 60.0f;
        GLES20.glUniform1f(uTimeHandle, currentTime);

        // Configurar atributos de vértices
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        // Configurar atributos de UVs
        if (texCoordBuffer != null && aTexCoordHandle >= 0) {
            texCoordBuffer.position(0);
            GLES20.glEnableVertexAttribArray(aTexCoordHandle);
            GLES20.glVertexAttribPointer(aTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);
        }

        // Bind textura
        if (textureId > 0 && uTextureHandle >= 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(uTextureHandle, 0);
        }

        // Dibujar
        indexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        // Desactivar atributos
        GLES20.glDisableVertexAttribArray(aPositionHandle);
        if (aTexCoordHandle >= 0) {
            GLES20.glDisableVertexAttribArray(aTexCoordHandle);
        }
    }
}
