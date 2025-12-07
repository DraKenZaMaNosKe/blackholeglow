// DefenderShip.java - Nave defensora de la Tierra
package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.util.ObjLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   🚀 DefenderShip - Nave Defensora de la Tierra                          ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  COMPORTAMIENTO:                                                          ║
 * ║  • Orbita alrededor de la Tierra protegiéndola                           ║
 * ║  • Persigue y dispara al OVNI enemigo                                    ║
 * ║  • Tiene sistema de HP (3 vidas)                                         ║
 * ║  • Celebra cuando destruye al OVNI                                       ║
 * ║  • Respawn después de ser destruida                                      ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class DefenderShip implements SceneObject, CameraAware {
    private static final String TAG = "DefenderShip";

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
    private int uTimeHandle;
    private int uModelMatrixHandle;

    // Transformación
    public float x, y, z;
    public float scale;
    public float rotationY = 0f;
    public float rotationX = 0f;  // Pitch (arriba/abajo)
    public float rotationZ = 0f;  // Roll (banking en curvas)

    // Velocidad actual (para vuelo natural)
    private float velocityX = 0f;
    private float velocityY = 0f;
    private float velocityZ = 0f;
    private float currentSpeed = 0f;
    private float maxSpeed = 2.0f;
    private float acceleration = 3.0f;

    // ═══════════════════════════════════════════════════════════
    // 🌍 ÓRBITA ALREDEDOR DE LA TIERRA
    // ═══════════════════════════════════════════════════════════
    private float orbitAngle = 0f;
    private float orbitRadius = 2.0f;
    private float orbitSpeed = 0.8f;        // Velocidad de órbita base
    private float orbitHeight = 0f;         // Variación vertical
    private float orbitHeightPhase = 0f;

    // 🌍 Posición de la Tierra (centro de órbita)
    private float earthX = 0f, earthY = 1.8f, earthZ = 0f;
    private float earthRadius = 1.2f;           // Radio visual de la Tierra
    private float safeDistanceEarth = 2.0f;     // Distancia mínima segura a la Tierra

    // 🛰️ ESTACIÓN ESPACIAL (para esquivarla)
    private SpaceStation spaceStationRef;
    private float stationRadius = 0.9f;         // Radio de colisión de la estación
    private float safeDistanceStation = 1.5f;   // Distancia mínima a la estación

    // ═══════════════════════════════════════════════════════════
    // 🎯 SISTEMA DE COMBATE (Objetivos: UfoScout y UfoAttacker)
    // ═══════════════════════════════════════════════════════════
    private UfoScout targetUfoScout;        // 🛸 Objetivo primario (UfoScout)
    private UfoAttacker targetUfoAttacker;  // 👾 Objetivo secundario (UfoAttacker)
    private float shootTimer = 0f;
    private float shootInterval = 2.0f;     // Dispara cada 2 segundos (más rápido)
    private float minShootInterval = 1.2f;
    private float maxShootInterval = 2.5f;

    // ═══════════════════════════════════════════════════════════
    // 🚧 REFERENCIAS A OTRAS NAVES (para evitar colisiones)
    // ═══════════════════════════════════════════════════════════
    private HumanInterceptor allyInterceptor = null;
    private float safeDistance = 1.2f;  // Distancia mínima entre naves

    // ═══════════════════════════════════════════════════════════
    // 🔫 SISTEMA DE DISPAROS (TEAM HUMAN - LÁSER AZUL)
    // ═══════════════════════════════════════════════════════════
    private static final int MAX_LASERS = 8;
    private final List<Laser> lasers = new ArrayList<>();
    private static final float MIN_SHOOT_INTERVAL = 0.8f;
    private static final float MAX_SHOOT_INTERVAL = 1.8f;


    // ═══════════════════════════════════════════════════════════
    // 💔 SISTEMA DE VIDA (AUMENTADO)
    // ═══════════════════════════════════════════════════════════
    private int health = 5;  // 5 HP para durar más

    // ═══════════════════════════════════════════════════════════════
    // 💥 SISTEMA DE EXPLOSIÓN
    // ═══════════════════════════════════════════════════════════════
    private static final int EXPLOSION_PARTICLES = 30;
    private float[] explosionX = new float[EXPLOSION_PARTICLES];
    private float[] explosionY = new float[EXPLOSION_PARTICLES];
    private float[] explosionZ = new float[EXPLOSION_PARTICLES];
    private float[] explosionVX = new float[EXPLOSION_PARTICLES];
    private float[] explosionVY = new float[EXPLOSION_PARTICLES];
    private float[] explosionVZ = new float[EXPLOSION_PARTICLES];
    private float[] explosionLife = new float[EXPLOSION_PARTICLES];
    private float[] explosionSize = new float[EXPLOSION_PARTICLES];
    private float[] explosionR = new float[EXPLOSION_PARTICLES];
    private float[] explosionG = new float[EXPLOSION_PARTICLES];
    private float[] explosionB = new float[EXPLOSION_PARTICLES];
    private boolean explosionActive = false;
    private float explosionTimer = 0f;
    private static final float EXPLOSION_DURATION = 1.5f;

    // Shader para explosión
    private int explosionProgram;
    private int expPositionLoc;
    private int expColorLoc;
    private int expPointSizeLoc;
    private FloatBuffer explosionVertexBuffer;
    private FloatBuffer explosionColorBuffer;
    private int maxHealth = 5;  // 5 HP máximo
    private boolean destroyed = false;
    private float respawnTimer = 0f;
    private float respawnDelay = 6.0f;
    private float invincibilityTimer = 0f;
    private float invincibilityDuration = 1.5f;

    // ═══════════════════════════════════════════════════════════
    // 🛡️ HABILIDAD 1: ESCUDO DE ENERGÍA
    // ═══════════════════════════════════════════════════════════
    private boolean shieldActive = false;
    private float shieldTimer = 0f;
    private float shieldDuration = 4.0f;        // 4 segundos de invencibilidad
    private float shieldCooldown = 0f;
    private float shieldCooldownMax = 15.0f;    // 15 segundos de cooldown
    private float shieldPulse = 0f;             // Para efecto visual pulsante
    private float shieldRadius = 0.8f;          // Radio del escudo

    // ═══════════════════════════════════════════════════════════
    // 🎯 HABILIDAD 2: MISILES RASTREADORES
    // ═══════════════════════════════════════════════════════════
    private static final int MAX_MISSILES = 4;
    private float[] missileX = new float[MAX_MISSILES];
    private float[] missileY = new float[MAX_MISSILES];
    private float[] missileZ = new float[MAX_MISSILES];
    private float[] missileVX = new float[MAX_MISSILES];
    private float[] missileVY = new float[MAX_MISSILES];
    private float[] missileVZ = new float[MAX_MISSILES];
    private boolean[] missileActive = new boolean[MAX_MISSILES];
    private float[] missileLife = new float[MAX_MISSILES];
    private float missileSpeed = 4.0f;
    private float missileTurnRate = 3.0f;       // Velocidad de giro hacia objetivo
    private float missileCooldown = 0f;
    private float missileCooldownMax = 12.0f;   // 12 segundos entre ráfagas
    private float missileLifeMax = 5.0f;        // Duración máxima del misil

    // ═══════════════════════════════════════════════════════════
    // 🔄 BUFFERS PRE-ASIGNADOS (evita GC en draw loops)
    // ═══════════════════════════════════════════════════════════
    private static final int SHIELD_SEGMENTS = 32;
    private static final int MISSILE_TRAIL_POINTS = 6;
    private final float[] shieldVertices = new float[SHIELD_SEGMENTS * 3];
    private final float[] shieldColors = new float[SHIELD_SEGMENTS * 4];
    private FloatBuffer shieldVertexBuffer;
    private FloatBuffer shieldColorBuffer;
    private final float[] missileTrailVerts = new float[MISSILE_TRAIL_POINTS * 3];
    private final float[] missileTrailCols = new float[MISSILE_TRAIL_POINTS * 4];
    private FloatBuffer missileTrailVertexBuffer;
    private FloatBuffer missileTrailColorBuffer;

    // ═══════════════════════════════════════════════════════════
    // 🧠 IA DE VUELO ORGÁNICO
    // ═══════════════════════════════════════════════════════════
    private float baseScale;  // Escala original para perspectiva
    private float curvePhase = 0f;
    private float curveFrequency = 1.2f;
    private float curveAmplitude = 0.25f;
    private float depthChangeTimer = 0f;
    private float depthChangeInterval = 5.0f;
    private float wanderTimer = 0f;
    private float wanderInterval = 3.0f;

    // Límites de movimiento (modo vertical)
    private static final float BOUND_X = 2.0f;
    private static final float BOUND_Y_MIN = 0.3f;
    private static final float BOUND_Y_MAX = 3.2f;
    private static final float BOUND_Z_MIN = -3.0f;
    private static final float BOUND_Z_MAX = 2.0f;

    // ═══════════════════════════════════════════════════════════
    // 🎉 SISTEMA DE CELEBRACIÓN
    // ═══════════════════════════════════════════════════════════
    private boolean celebrating = false;
    private float celebrationTimer = 0f;
    private float celebrationDuration = 3.0f;
    private int celebrationSpins = 0;

    // ═══════════════════════════════════════════════════════════
    // 🎯 MODO PERSECUCIÓN
    // ═══════════════════════════════════════════════════════════
    private boolean pursuingTarget = false;
    private float pursuitTimer = 0f;
    private float pursuitDuration = 4.0f;   // Persigue por 4 segundos
    private float pursuitCooldown = 0f;
    private float pursuitCooldownDuration = 5.0f;

    // Cámara
    private CameraController camera;

    // 💚 BARRA DE VIDA
    private HealthBar healthBar;

    // Matrices
    private float[] modelMatrix = new float[16];
    private float[] mvpMatrix = new float[16];
    private final float[] laserMvp = new float[16];
    private final float[] identityModel = new float[16];

    // Tiempo relativo
    private final long startTime = System.currentTimeMillis();
    private final Random random = new Random();

    /**
     * Constructor
     */
    public DefenderShip(Context context, TextureLoader textureLoader,
                        float x, float y, float z, float scale) {
        this.context = context;
        this.textureLoader = textureLoader;
        this.x = x;
        this.y = y;
        this.z = z;
        this.scale = scale;
        this.baseScale = scale;  // Guardar escala original para perspectiva

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Log.d(TAG, "🚀 Creando DefenderShip");
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // 1. Cargar modelo
        loadModel();

        // 2. Cargar textura
        loadTexture();

        // 3. Crear shaders
        createShaders();

        // 4. Crear barra de vida (Defender = aliado)
        healthBar = new HealthBar(3, false);

        // 5. Crear shader de explosión
        createExplosionShader();

        // 6. Inicializar láseres
        initLasers();

        Log.d(TAG, "✅ DefenderShip creado exitosamente");
    }

    /**
     * Carga el modelo defender_ship.obj
     */
    private void loadModel() {
        try {
            Log.d(TAG, "📦 Cargando defender_ship.obj...");

            ObjLoader.Mesh mesh = ObjLoader.loadObj(context, "defender_ship.obj");

            Log.d(TAG, "✓ Modelo cargado:");
            Log.d(TAG, "  Vértices: " + mesh.vertexCount);
            Log.d(TAG, "  Caras: " + mesh.faces.size());

            // CENTRAR EL MODELO EN EL ORIGEN
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

            // Trasladar al origen
            for (int i = 0; i < mesh.vertexCount * 3; i += 3) {
                mesh.vertexBuffer.put(i + 0, mesh.vertexBuffer.get(i + 0) - centerX);
                mesh.vertexBuffer.put(i + 1, mesh.vertexBuffer.get(i + 1) - centerY);
                mesh.vertexBuffer.put(i + 2, mesh.vertexBuffer.get(i + 2) - centerZ);
            }
            mesh.vertexBuffer.position(0);

            Log.d(TAG, "  ✓ Modelo centrado en el origen");

            // Usar UVs del modelo si existen, sino generar automáticos
            if (mesh.uvBuffer != null) {
                texCoordBuffer = mesh.uvBuffer;
                Log.d(TAG, "  ✓ Usando UVs del modelo");
            } else {
                // Generar UVs automáticos
                Log.d(TAG, "  Generando UVs automáticos...");
                float rangeX = maxX - minX;
                float rangeZ = maxZ - minZ;
                float maxRange = Math.max(rangeX, rangeZ);

                FloatBuffer autoUVs = ByteBuffer
                        .allocateDirect(mesh.vertexCount * 2 * Float.BYTES)
                        .order(ByteOrder.nativeOrder())
                        .asFloatBuffer();

                mesh.vertexBuffer.position(0);
                for (int i = 0; i < mesh.vertexCount; i++) {
                    float vx = mesh.vertexBuffer.get(i * 3 + 0);
                    float vz = mesh.vertexBuffer.get(i * 3 + 2);
                    float u = 0.5f + (vx / maxRange);
                    float v = 0.5f + (vz / maxRange);
                    autoUVs.put(u);
                    autoUVs.put(v);
                }
                autoUVs.position(0);
                texCoordBuffer = autoUVs;
            }

            vertexBuffer = mesh.vertexBuffer;

            // Crear index buffer
            int triangles = 0;
            for (int[] face : mesh.faces) {
                triangles += face.length - 2;
            }
            indexCount = triangles * 3;

            ShortBuffer ib = ByteBuffer
                    .allocateDirect(indexCount * Short.BYTES)
                    .order(ByteOrder.nativeOrder())
                    .asShortBuffer();

            for (int[] face : mesh.faces) {
                short v0 = (short) face[0];
                for (int k = 1; k < face.length - 1; k++) {
                    ib.put(v0).put((short) face[k]).put((short) face[k + 1]);
                }
            }
            ib.position(0);
            indexBuffer = ib;

            Log.d(TAG, "  Índices: " + indexCount);

        } catch (IOException e) {
            Log.e(TAG, "❌ Error cargando defender_ship.obj", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Carga la textura de la nave
     */
    private void loadTexture() {
        Log.d(TAG, "🎨 Cargando textura defender_ship.png desde assets...");

        try {
            // Cargar textura desde assets
            textureId = loadTextureFromAssets("defender_ship.png");
            Log.d(TAG, "✓ Textura cargada (ID=" + textureId + ")");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error cargando textura: " + e.getMessage());
            textureId = 0;
        }
    }

    /**
     * Carga una textura PNG desde assets
     */
    private int loadTextureFromAssets(String filename) {
        int[] textureHandle = new int[1];
        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0) {
            try {
                InputStream is = context.getAssets().open(filename);
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(is);
                is.close();

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

                android.opengl.GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
                GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);

                bitmap.recycle();

                Log.d(TAG, "✓ Textura cargada desde assets: " + filename);
            } catch (IOException e) {
                Log.e(TAG, "❌ Error cargando textura desde assets: " + filename, e);
                GLES20.glDeleteTextures(1, textureHandle, 0);
                return 0;
            }
        }

        return textureHandle[0];
    }

    /**
     * 🎨 SHADERS ÉPICOS - Efectos de nave de combate futurista
     */
    private void createShaders() {
        Log.d(TAG, "🎨 Creando shaders ÉPICOS para DefenderShip...");

        String vertexShaderCode =
                "attribute vec4 a_Position;\n" +
                "attribute vec2 a_TexCoord;\n" +
                "uniform mat4 u_MVPMatrix;\n" +
                "uniform mat4 u_ModelMatrix;\n" +
                "varying vec2 v_TexCoord;\n" +
                "varying vec3 v_WorldPos;\n" +
                "varying vec3 v_LocalPos;\n" +
                "varying vec3 v_Normal;\n" +
                "void main() {\n" +
                "  gl_Position = u_MVPMatrix * a_Position;\n" +
                "  v_TexCoord = a_TexCoord;\n" +
                "  vec4 worldPos = u_ModelMatrix * a_Position;\n" +
                "  v_WorldPos = worldPos.xyz;\n" +
                "  v_LocalPos = a_Position.xyz;\n" +
                "  v_Normal = normalize(a_Position.xyz);\n" +
                "}";

        String fragmentShaderCode =
                "precision mediump float;\n" +
                "uniform sampler2D u_Texture;\n" +
                "uniform float u_Time;\n" +
                "varying vec2 v_TexCoord;\n" +
                "varying vec3 v_WorldPos;\n" +
                "varying vec3 v_LocalPos;\n" +
                "varying vec3 v_Normal;\n" +
                "\n" +
                "void main() {\n" +
                "    vec4 texColor = texture2D(u_Texture, v_TexCoord);\n" +
                "    vec3 baseColor = texColor.rgb;\n" +
                "    \n" +
                "    // ═══════════════════════════════════════════════════════\n" +
                "    // 🔥 PROPULSORES CON LLAMAS DE ENERGÍA\n" +
                "    // ═══════════════════════════════════════════════════════\n" +
                "    float thrusterZone = smoothstep(0.0, -0.6, v_LocalPos.z);\n" +
                "    thrusterZone *= smoothstep(-0.4, 0.2, v_LocalPos.y);\n" +
                "    \n" +
                "    if (thrusterZone > 0.05) {\n" +
                "        // Colores de plasma (cyan → blanco → azul)\n" +
                "        float flicker = sin(u_Time * 25.0 + v_LocalPos.z * 10.0) * 0.3 + 0.7;\n" +
                "        float pulse = sin(u_Time * 8.0) * 0.2 + 0.8;\n" +
                "        \n" +
                "        vec3 plasmaCore = vec3(0.8, 0.95, 1.0);   // Blanco-cyan centro\n" +
                "        vec3 plasmaEdge = vec3(0.2, 0.6, 1.0);    // Azul borde\n" +
                "        vec3 plasmaColor = mix(plasmaEdge, plasmaCore, flicker * pulse);\n" +
                "        \n" +
                "        // Intensidad que aumenta hacia el centro\n" +
                "        float centerDist = abs(v_LocalPos.x) / 0.3;\n" +
                "        float intensity = (1.0 - centerDist) * thrusterZone * 2.0;\n" +
                "        \n" +
                "        baseColor = mix(baseColor, plasmaColor, intensity);\n" +
                "        baseColor += plasmaColor * intensity * flicker * 0.8;  // Glow aditivo\n" +
                "    }\n" +
                "    \n" +
                "    // ═══════════════════════════════════════════════════════\n" +
                "    // 🔷 FRESNEL EDGE GLOW (bordes brillantes estilo Tron)\n" +
                "    // ═══════════════════════════════════════════════════════\n" +
                "    vec3 viewDir = normalize(vec3(0.0, 0.0, 1.0));\n" +
                "    float fresnel = 1.0 - abs(dot(v_Normal, viewDir));\n" +
                "    fresnel = pow(fresnel, 2.0);\n" +
                "    \n" +
                "    vec3 fresnelColor = vec3(0.3, 0.7, 1.0);  // Cyan brillante\n" +
                "    float fresnelPulse = sin(u_Time * 3.0) * 0.15 + 0.85;\n" +
                "    baseColor += fresnelColor * fresnel * 0.6 * fresnelPulse;\n" +
                "    \n" +
                "    // ═══════════════════════════════════════════════════════\n" +
                "    // ✨ LÍNEAS DE ENERGÍA (tech lines)\n" +
                "    // ═══════════════════════════════════════════════════════\n" +
                "    float techLine1 = smoothstep(0.02, 0.0, abs(fract(v_LocalPos.y * 8.0 + u_Time * 0.5) - 0.5));\n" +
                "    float techLine2 = smoothstep(0.02, 0.0, abs(fract(v_LocalPos.x * 6.0) - 0.5));\n" +
                "    float techLines = max(techLine1 * 0.3, techLine2 * 0.2);\n" +
                "    \n" +
                "    vec3 techColor = vec3(0.4, 0.8, 1.0);\n" +
                "    baseColor += techColor * techLines * (sin(u_Time * 4.0) * 0.3 + 0.7);\n" +
                "    \n" +
                "    // ═══════════════════════════════════════════════════════\n" +
                "    // 💡 LUCES DE NAVEGACIÓN (parpadeo realista)\n" +
                "    // ═══════════════════════════════════════════════════════\n" +
                "    float wingTip = smoothstep(0.7, 1.0, abs(v_LocalPos.x));\n" +
                "    if (wingTip > 0.1) {\n" +
                "        // Parpadeo tipo avión real (on-off-on-off-pausa)\n" +
                "        float blinkPattern = step(0.8, fract(u_Time * 1.5));\n" +
                "        blinkPattern += step(0.9, fract(u_Time * 1.5 + 0.1)) * 0.5;\n" +
                "        \n" +
                "        if (v_LocalPos.x < 0.0) {\n" +
                "            baseColor += vec3(1.0, 0.2, 0.2) * wingTip * blinkPattern * 1.5;\n" +
                "        } else {\n" +
                "            baseColor += vec3(0.2, 1.0, 0.2) * wingTip * blinkPattern * 1.5;\n" +
                "        }\n" +
                "    }\n" +
                "    \n" +
                "    // ═══════════════════════════════════════════════════════\n" +
                "    // 🎯 COCKPIT HOLOGRÁFICO\n" +
                "    // ═══════════════════════════════════════════════════════\n" +
                "    float cockpitZone = smoothstep(0.2, 0.7, v_LocalPos.z);\n" +
                "    cockpitZone *= smoothstep(0.0, 0.5, v_LocalPos.y);\n" +
                "    cockpitZone *= smoothstep(0.4, 0.0, abs(v_LocalPos.x));\n" +
                "    \n" +
                "    if (cockpitZone > 0.1) {\n" +
                "        // Scanlines holográficas\n" +
                "        float scanline = sin(v_LocalPos.y * 50.0 + u_Time * 10.0) * 0.5 + 0.5;\n" +
                "        vec3 holoColor = vec3(0.3, 0.8, 1.0);\n" +
                "        \n" +
                "        // Interferencia ocasional\n" +
                "        float interference = sin(u_Time * 15.0) * sin(u_Time * 23.0);\n" +
                "        interference = step(0.7, interference) * 0.3;\n" +
                "        \n" +
                "        baseColor += holoColor * cockpitZone * (0.4 + scanline * 0.3 + interference);\n" +
                "    }\n" +
                "    \n" +
                "    // ═══════════════════════════════════════════════════════\n" +
                "    // 🌟 ESCUDO DE ENERGÍA SUTIL\n" +
                "    // ═══════════════════════════════════════════════════════\n" +
                "    float shieldPulse = sin(u_Time * 2.0 + length(v_LocalPos) * 5.0) * 0.5 + 0.5;\n" +
                "    vec3 shieldColor = vec3(0.2, 0.5, 1.0);\n" +
                "    baseColor += shieldColor * fresnel * shieldPulse * 0.15;\n" +
                "    \n" +
                "    // ═══════════════════════════════════════════════════════\n" +
                "    // 💫 BRILLO GENERAL + HDR\n" +
                "    // ═══════════════════════════════════════════════════════\n" +
                "    baseColor *= 1.4;\n" +
                "    \n" +
                "    // Tone mapping simple para evitar oversaturation\n" +
                "    baseColor = baseColor / (baseColor + vec3(1.0));\n" +
                "    baseColor = pow(baseColor, vec3(0.9));  // Gamma\n" +
                "    \n" +
                "    gl_FragColor = vec4(baseColor, texColor.a);\n" +
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
        uModelMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "u_ModelMatrix");
        uTextureHandle = GLES20.glGetUniformLocation(shaderProgram, "u_Texture");
        uTimeHandle = GLES20.glGetUniformLocation(shaderProgram, "u_Time");

        Log.d(TAG, "✓ Shaders creados (program=" + shaderProgram + ")");
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

    /**
     * 💥 Crear shader para efecto de explosión
     */
    private void createExplosionShader() {
        String vertexShader =
            "attribute vec3 a_Position;\n" +
            "attribute vec4 a_Color;\n" +
            "uniform mat4 u_MVP;\n" +
            "uniform float u_PointSize;\n" +
            "varying vec4 v_Color;\n" +
            "void main() {\n" +
            "    v_Color = a_Color;\n" +
            "    gl_Position = u_MVP * vec4(a_Position, 1.0);\n" +
            "    gl_PointSize = u_PointSize;\n" +
            "}\n";

        String fragmentShader =
            "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
            "varying vec4 v_Color;\n" +
            "void main() {\n" +
            "    vec2 coord = gl_PointCoord - vec2(0.5);\n" +
            "    float dist = length(coord);\n" +
            "    float alpha = v_Color.a * (1.0 - smoothstep(0.0, 0.5, dist));\n" +
            "    // Brillo en el centro\n" +
            "    float glow = 1.0 + (1.0 - dist * 2.0) * 0.8;\n" +
            "    gl_FragColor = vec4(v_Color.rgb * glow, alpha);\n" +
            "}\n";

        explosionProgram = ShaderUtils.createProgram(vertexShader, fragmentShader);
        if (explosionProgram != 0) {
            expPositionLoc = GLES20.glGetAttribLocation(explosionProgram, "a_Position");
            expColorLoc = GLES20.glGetAttribLocation(explosionProgram, "a_Color");
            expPointSizeLoc = GLES20.glGetUniformLocation(explosionProgram, "u_PointSize");
        }

        // Crear buffers para partículas
        ByteBuffer bb = ByteBuffer.allocateDirect(EXPLOSION_PARTICLES * 3 * 4);
        bb.order(ByteOrder.nativeOrder());
        explosionVertexBuffer = bb.asFloatBuffer();

        ByteBuffer cb = ByteBuffer.allocateDirect(EXPLOSION_PARTICLES * 4 * 4);
        cb.order(ByteOrder.nativeOrder());
        explosionColorBuffer = cb.asFloatBuffer();

        // Crear buffers pre-asignados para escudo (evita GC en draw)
        ByteBuffer svb = ByteBuffer.allocateDirect(SHIELD_SEGMENTS * 3 * 4);
        svb.order(ByteOrder.nativeOrder());
        shieldVertexBuffer = svb.asFloatBuffer();

        ByteBuffer scb = ByteBuffer.allocateDirect(SHIELD_SEGMENTS * 4 * 4);
        scb.order(ByteOrder.nativeOrder());
        shieldColorBuffer = scb.asFloatBuffer();

        // Crear buffers pre-asignados para misiles (evita GC en draw)
        ByteBuffer mvb = ByteBuffer.allocateDirect(MISSILE_TRAIL_POINTS * 3 * 4);
        mvb.order(ByteOrder.nativeOrder());
        missileTrailVertexBuffer = mvb.asFloatBuffer();

        ByteBuffer mcb = ByteBuffer.allocateDirect(MISSILE_TRAIL_POINTS * 4 * 4);
        mcb.order(ByteOrder.nativeOrder());
        missileTrailColorBuffer = mcb.asFloatBuffer();

        Log.d(TAG, "💥 Shader de explosión creado + buffers pre-asignados");
    }

    /**
     * 🔫 Inicializa el pool de láseres
     */
    private void initLasers() {
        for (int i = 0; i < MAX_LASERS; i++) {
            lasers.add(new Laser(Laser.TEAM_HUMAN));
        }
        // Intervalo inicial aleatorio
        shootInterval = MIN_SHOOT_INTERVAL + (float)(Math.random() * (MAX_SHOOT_INTERVAL - MIN_SHOOT_INTERVAL));
        Log.d(TAG, "🔫 Sistema de láseres inicializado (" + MAX_LASERS + " láseres azules)");
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
                Log.d(TAG, "🔫 NAVE1 disparó láser azul hacia OVNI1!");
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
                // Verificar colisión con UfoScout (objetivo primario)
                if (targetUfoScout != null && !targetUfoScout.isTeleportingNow() && !targetUfoScout.isDestroyed()) {
                    if (laser.checkCollision(targetUfoScout.getX(), targetUfoScout.getY(), targetUfoScout.getZ(), 0.3f)) {
                        laser.deactivate();
                        targetUfoScout.takeDamage();
                        Log.d(TAG, "💥 ¡Láser azul impactó a UfoScout!");
                        continue;  // No verificar más colisiones para este láser
                    }
                }

                // Verificar colisión con UfoAttacker (objetivo secundario)
                if (targetUfoAttacker != null && !targetUfoAttacker.isDestroyed()) {
                    if (laser.checkCollision(targetUfoAttacker.getX(), targetUfoAttacker.getY(), targetUfoAttacker.getZ(), 0.35f)) {
                        laser.deactivate();
                        targetUfoAttacker.takeDamage();
                        Log.d(TAG, "💥 ¡Láser azul impactó a UfoAttacker!");
                    }
                }
            }
        }
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

    /**
     * 🔫 Obtiene la lista de láseres
     */
    public List<Laser> getLasers() {
        return lasers;
    }

    /**
     * 💥 Iniciar explosión en la posición actual
     */
    private void startExplosion() {
        explosionActive = true;
        explosionTimer = 0f;

        for (int i = 0; i < EXPLOSION_PARTICLES; i++) {
            // Posición inicial = posición de la nave
            explosionX[i] = x;
            explosionY[i] = y;
            explosionZ[i] = z;

            // Velocidad en dirección aleatoria (esfera)
            float theta = random.nextFloat() * (float)(Math.PI * 2);
            float phi = random.nextFloat() * (float)Math.PI;
            float speed = 1.5f + random.nextFloat() * 2.5f;

            explosionVX[i] = speed * (float)(Math.sin(phi) * Math.cos(theta));
            explosionVY[i] = speed * (float)(Math.sin(phi) * Math.sin(theta));
            explosionVZ[i] = speed * (float)(Math.cos(phi));

            // Vida inicial
            explosionLife[i] = 1.0f;

            // Tamaño aleatorio
            explosionSize[i] = 8.0f + random.nextFloat() * 15.0f;

            // Colores: naranja, amarillo, rojo, blanco (explosión)
            float colorType = random.nextFloat();
            if (colorType < 0.3f) {
                // Naranja brillante
                explosionR[i] = 1.0f;
                explosionG[i] = 0.5f + random.nextFloat() * 0.3f;
                explosionB[i] = 0.0f;
            } else if (colorType < 0.6f) {
                // Amarillo
                explosionR[i] = 1.0f;
                explosionG[i] = 0.9f + random.nextFloat() * 0.1f;
                explosionB[i] = 0.2f + random.nextFloat() * 0.3f;
            } else if (colorType < 0.8f) {
                // Rojo fuego
                explosionR[i] = 1.0f;
                explosionG[i] = 0.2f + random.nextFloat() * 0.2f;
                explosionB[i] = 0.0f;
            } else {
                // Blanco caliente (centro)
                explosionR[i] = 1.0f;
                explosionG[i] = 1.0f;
                explosionB[i] = 0.8f + random.nextFloat() * 0.2f;
            }
        }

        Log.d(TAG, "💥 ¡EXPLOSIÓN INICIADA!");
    }

    /**
     * 💥 Actualizar partículas de explosión
     */
    private void updateExplosion(float deltaTime) {
        if (!explosionActive) return;

        explosionTimer += deltaTime;

        for (int i = 0; i < EXPLOSION_PARTICLES; i++) {
            // Mover partícula
            explosionX[i] += explosionVX[i] * deltaTime;
            explosionY[i] += explosionVY[i] * deltaTime;
            explosionZ[i] += explosionVZ[i] * deltaTime;

            // Desacelerar (fricción del espacio)
            explosionVX[i] *= 0.98f;
            explosionVY[i] *= 0.98f;
            explosionVZ[i] *= 0.98f;

            // Reducir vida
            explosionLife[i] -= deltaTime / EXPLOSION_DURATION;
            if (explosionLife[i] < 0) explosionLife[i] = 0;
        }

        // Terminar explosión
        if (explosionTimer >= EXPLOSION_DURATION) {
            explosionActive = false;
            Log.d(TAG, "💥 Explosión terminada");
        }
    }

    /**
     * 💥 Dibujar partículas de explosión
     */
    private void drawExplosion() {
        if (!explosionActive || explosionProgram == 0 || camera == null) return;

        GLES20.glUseProgram(explosionProgram);

        // Blending aditivo para brillo
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);

        // Llenar buffers
        explosionVertexBuffer.position(0);
        explosionColorBuffer.position(0);

        int aliveCount = 0;
        for (int i = 0; i < EXPLOSION_PARTICLES; i++) {
            if (explosionLife[i] <= 0) continue;

            explosionVertexBuffer.put(explosionX[i]);
            explosionVertexBuffer.put(explosionY[i]);
            explosionVertexBuffer.put(explosionZ[i]);

            float alpha = explosionLife[i];
            explosionColorBuffer.put(explosionR[i]);
            explosionColorBuffer.put(explosionG[i]);
            explosionColorBuffer.put(explosionB[i]);
            explosionColorBuffer.put(alpha);

            aliveCount++;
        }

        if (aliveCount == 0) return;

        explosionVertexBuffer.position(0);
        explosionColorBuffer.position(0);

        // MVP matrix (identidad para modelo, usar viewProj de cámara)
        Matrix.setIdentityM(identityModel, 0);
        camera.computeMvp(identityModel, laserMvp);

        int uMVPLoc = GLES20.glGetUniformLocation(explosionProgram, "u_MVP");
        GLES20.glUniformMatrix4fv(uMVPLoc, 1, false, laserMvp, 0);

        // Tamaño de partícula
        float sizeMultiplier = 1.0f + (1.0f - explosionTimer / EXPLOSION_DURATION) * 0.5f;
        GLES20.glUniform1f(expPointSizeLoc, 15.0f * sizeMultiplier);

        // Atributos
        GLES20.glEnableVertexAttribArray(expPositionLoc);
        GLES20.glVertexAttribPointer(expPositionLoc, 3, GLES20.GL_FLOAT, false, 0, explosionVertexBuffer);

        GLES20.glEnableVertexAttribArray(expColorLoc);
        GLES20.glVertexAttribPointer(expColorLoc, 4, GLES20.GL_FLOAT, false, 0, explosionColorBuffer);

        // Dibujar
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, aliveCount);

        // Limpiar
        GLES20.glDisableVertexAttribArray(expPositionLoc);
        GLES20.glDisableVertexAttribArray(expColorLoc);

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    /**
     * 🛸 Establece el UfoScout (OVNI1) como objetivo
     */
    public void setTargetUfoScout(UfoScout ufoScout) {
        this.targetUfoScout = ufoScout;
        Log.d(TAG, "🎯 NAVE1: Objetivo primario OVNI1 (UfoScout) establecido");
    }

    public void setTargetUfoAttacker(UfoAttacker ufoAttacker) {
        this.targetUfoAttacker = ufoAttacker;
        Log.d(TAG, "🎯 NAVE1: Objetivo secundario OVNI2 (UfoAttacker) establecido");
    }

    /**
     * 🎯 Obtiene el objetivo activo (primario o secundario)
     */
    private Object getActiveTarget() {
        // Prioridad: UfoScout si existe y no está destruido/teletransportándose
        if (targetUfoScout != null && !targetUfoScout.isDestroyed() && !targetUfoScout.isTeleportingNow()) {
            return targetUfoScout;
        }
        // Fallback: UfoAttacker
        if (targetUfoAttacker != null && !targetUfoAttacker.isDestroyed()) {
            return targetUfoAttacker;
        }
        return null;
    }

    private float getTargetX() {
        Object target = getActiveTarget();
        if (target instanceof UfoScout) return ((UfoScout) target).getX();
        if (target instanceof UfoAttacker) return ((UfoAttacker) target).getX();
        return x;
    }

    private float getTargetY() {
        Object target = getActiveTarget();
        if (target instanceof UfoScout) return ((UfoScout) target).getY();
        if (target instanceof UfoAttacker) return ((UfoAttacker) target).getY();
        return y;
    }

    private float getTargetZ() {
        Object target = getActiveTarget();
        if (target instanceof UfoScout) return ((UfoScout) target).getZ();
        if (target instanceof UfoAttacker) return ((UfoAttacker) target).getZ();
        return z;
    }

    /**
     * 🚧 Establece referencia al aliado HumanInterceptor para evitar colisiones
     */
    public void setAllyInterceptor(HumanInterceptor ally) {
        this.allyInterceptor = ally;
    }

    /**
     * Establecer posición de la Tierra (centro de órbita)
     */
    public void setEarthPosition(float ex, float ey, float ez) {
        this.earthX = ex;
        this.earthY = ey;
        this.earthZ = ez;
    }

    /**
     * Configurar parámetros de órbita
     */
    public void setOrbitParams(float radius, float speed) {
        this.orbitRadius = radius;
        this.orbitSpeed = speed;
    }

    /**
     * 🛰️ Establece referencia a la Estación Espacial (para esquivarla)
     */
    public void setSpaceStation(SpaceStation station) {
        this.spaceStationRef = station;
        if (station != null) {
            this.stationRadius = station.getCollisionRadius();
            this.safeDistanceStation = stationRadius + 0.5f;  // Margen de seguridad
            Log.d(TAG, "🛰️ Estación Espacial conectada para esquivar (radio=" + stationRadius + ")");
        }
    }

    @Override
    public void update(float deltaTime) {
        // 💥 SIEMPRE actualizar explosión (aunque esté destruida)
        updateExplosion(deltaTime);

        // Si está destruida, esperar respawn
        if (destroyed) {
            respawnTimer += deltaTime;
            if (respawnTimer >= respawnDelay) {
                respawn();
            }
            return;
        }

        // ═══════════════════════════════════════════════════════════
        // 🎉 MODO CELEBRACIÓN
        // ═══════════════════════════════════════════════════════════
        if (celebrating) {
            celebrationTimer += deltaTime;
            rotationY += 720f * deltaTime;
            rotationZ = (float)Math.sin(celebrationTimer * 8f) * 30f;
            y = earthY + 0.5f + (float)Math.sin(celebrationTimer * 10f) * 0.3f;

            if (celebrationTimer >= celebrationDuration) {
                celebrating = false;
                celebrationTimer = 0f;
                rotationZ = 0f;
            }
            return;
        }

        // ═══════════════════════════════════════════════════════════
        // 🎯 CALCULAR OBJETIVO Y DIRECCIÓN
        // ═══════════════════════════════════════════════════════════
        float targetX, targetY, targetZ;
        boolean hasVisibleTarget = false;

        // Verificar si el UfoScout está visible (no teletransportándose)
        boolean ufoVisible = targetUfoScout != null && !targetUfoScout.isTeleportingNow();

        if (ufoVisible) {
            hasVisibleTarget = true;
            // Apuntar al UfoScout
            targetX = targetUfoScout.x;
            targetY = targetUfoScout.y;
            targetZ = targetUfoScout.z;

            float distToUfo = distanceTo(targetX, targetY, targetZ);

            // 🚫 EVITAR COLISIÓN - mantener distancia mínima de 0.8
            if (distToUfo < 0.8f) {
                // Alejarse del OVNI
                float dx = x - targetUfoScout.x;
                float dy = y - targetUfoScout.y;
                float dz = z - targetUfoScout.z;
                float dist = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
                if (dist > 0.01f) {
                    targetX = x + (dx / dist) * 2.0f;
                    targetY = y + (dy / dist) * 1.0f;
                    targetZ = z + (dz / dist) * 2.0f;
                }
            } else if (distToUfo < 1.5f) {
                // Rodear al OVNI a distancia de ataque
                float angle = (float)Math.atan2(z - targetUfoScout.z, x - targetUfoScout.x);
                angle += 2.0f * deltaTime;
                targetX = targetUfoScout.x + (float)Math.cos(angle) * 1.5f;
                targetZ = targetUfoScout.z + (float)Math.sin(angle) * 1.5f;
                targetY = targetUfoScout.y + 0.3f;
            }
        } else {
            // 🔍 MODO BÚSQUEDA ACTIVA - OVNI no visible (teletransportándose o destruido)
            // Búsqueda rápida y agresiva por toda la escena
            orbitAngle += orbitSpeed * 3.0f * deltaTime;  // 3x más rápido buscando
            orbitHeightPhase += deltaTime * 1.5f;

            // Patrón de búsqueda en espiral que cubre toda la escena
            float searchRadius = 1.5f + (float)Math.sin(orbitAngle * 0.3f) * 1.0f;
            float searchHeight = (float)Math.sin(orbitHeightPhase) * 1.2f;

            targetX = earthX + (float)Math.cos(orbitAngle) * searchRadius;
            targetZ = earthZ + (float)Math.sin(orbitAngle) * searchRadius;
            targetY = earthY + searchHeight;

            // Aumentar velocidad máxima durante búsqueda
            maxSpeed = 2.5f;
        }

        // Restaurar velocidad normal cuando tiene objetivo
        if (hasVisibleTarget) {
            maxSpeed = 2.0f;
        }

        // ═══════════════════════════════════════════════════════════
        // ✈️ VUELO NATURAL CON FÍSICA
        // ═══════════════════════════════════════════════════════════
        float dx = targetX - x;
        float dy = targetY - y;
        float dz = targetZ - z;
        float distToTarget = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);

        if (distToTarget > 0.1f) {
            // Dirección normalizada al objetivo
            float dirX = dx / distToTarget;
            float dirY = dy / distToTarget;
            float dirZ = dz / distToTarget;

            // Acelerar hacia el objetivo
            float accel = acceleration * deltaTime;
            velocityX += dirX * accel;
            velocityY += dirY * accel * 0.5f;  // Menos aceleración vertical
            velocityZ += dirZ * accel;
        }

        // Limitar velocidad máxima
        currentSpeed = (float)Math.sqrt(velocityX*velocityX + velocityY*velocityY + velocityZ*velocityZ);
        if (currentSpeed > maxSpeed) {
            velocityX = (velocityX / currentSpeed) * maxSpeed;
            velocityY = (velocityY / currentSpeed) * maxSpeed;
            velocityZ = (velocityZ / currentSpeed) * maxSpeed;
            currentSpeed = maxSpeed;
        }

        // Fricción (desaceleración natural)
        float friction = 0.98f;
        velocityX *= friction;
        velocityY *= friction;
        velocityZ *= friction;

        // ═══════════════════════════════════════════════════════════
        // 🚧 ANTI-COLISIÓN CON OTRAS NAVES
        // ═══════════════════════════════════════════════════════════
        float avoidX = 0, avoidY = 0, avoidZ = 0;

        // Evitar al aliado HumanInterceptor
        if (allyInterceptor != null && !allyInterceptor.isDestroyed()) {
            float adx = x - allyInterceptor.getX();
            float ady = y - allyInterceptor.getY();
            float adz = z - allyInterceptor.getZ();
            float adist = (float) Math.sqrt(adx * adx + ady * ady + adz * adz);

            if (adist < safeDistance && adist > 0.01f) {
                float pushForce = (safeDistance - adist) / adist * 3.0f;
                avoidX += adx * pushForce;
                avoidY += ady * pushForce;
                avoidZ += adz * pushForce;
            }
        }

        // Evitar a UfoScout (enemigo)
        if (targetUfoScout != null && !targetUfoScout.isDestroyed() && !targetUfoScout.isTeleportingNow()) {
            float adx = x - targetUfoScout.getX();
            float ady = y - targetUfoScout.getY();
            float adz = z - targetUfoScout.getZ();
            float adist = (float) Math.sqrt(adx * adx + ady * ady + adz * adz);

            if (adist < safeDistance * 0.7f && adist > 0.01f) {
                float pushForce = (safeDistance * 0.7f - adist) / adist * 4.0f;
                avoidX += adx * pushForce;
                avoidY += ady * pushForce;
                avoidZ += adz * pushForce;
            }
        }

        // Evitar a UfoAttacker (enemigo secundario)
        if (targetUfoAttacker != null && !targetUfoAttacker.isDestroyed()) {
            float adx = x - targetUfoAttacker.getX();
            float ady = y - targetUfoAttacker.getY();
            float adz = z - targetUfoAttacker.getZ();
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

        // Aplicar movimiento
        x += velocityX * deltaTime;
        y += velocityY * deltaTime;
        z += velocityZ * deltaTime;

        // ═══════════════════════════════════════════════════════════
        // 🌍 EVITAR COLISIÓN CON LA TIERRA (CRÍTICO - nunca atravesar)
        // ═══════════════════════════════════════════════════════════
        float dxE = x - earthX;
        float dyE = y - earthY;
        float dzE = z - earthZ;
        float distToEarth = (float) Math.sqrt(dxE * dxE + dyE * dyE + dzE * dzE);

        if (distToEarth < safeDistanceEarth) {
            // Fuerza de escape proporcional a qué tan cerca está
            float escapeForce = (safeDistanceEarth - distToEarth) / safeDistanceEarth;
            escapeForce = escapeForce * escapeForce * 5.0f;  // Cuadrático para más urgencia

            if (distToEarth > 0.01f) {
                // Empujar en dirección opuesta a la Tierra
                velocityX += (dxE / distToEarth) * escapeForce;
                velocityY += (dyE / distToEarth) * escapeForce;
                velocityZ += (dzE / distToEarth) * escapeForce;
            }

            // Si está MUY cerca (casi tocando), reposicionar instantáneamente
            if (distToEarth < earthRadius + 0.3f) {
                float safeRadius = safeDistanceEarth + 0.3f;
                x = earthX + (dxE / distToEarth) * safeRadius;
                y = earthY + (dyE / distToEarth) * safeRadius;
                z = earthZ + (dzE / distToEarth) * safeRadius;
                Log.d(TAG, "🌍 DefenderShip evitó colisión con Tierra!");
            }
        }

        // ═══════════════════════════════════════════════════════════
        // 🛰️ EVITAR COLISIÓN CON LA ESTACIÓN ESPACIAL
        // ═══════════════════════════════════════════════════════════
        if (spaceStationRef != null) {
            float stationX = spaceStationRef.getX();
            float stationY = spaceStationRef.getY();
            float stationZ = spaceStationRef.getZ();

            float dxS = x - stationX;
            float dyS = y - stationY;
            float dzS = z - stationZ;
            float distToStation = (float) Math.sqrt(dxS * dxS + dyS * dyS + dzS * dzS);

            if (distToStation < safeDistanceStation) {
                // Fuerza de escape proporcional a qué tan cerca está
                float escapeForce = (safeDistanceStation - distToStation) / safeDistanceStation;
                escapeForce = escapeForce * escapeForce * 5.0f;  // Cuadrático y fuerte

                if (distToStation > 0.01f) {
                    // Empujar en dirección opuesta a la estación
                    velocityX += (dxS / distToStation) * escapeForce;
                    velocityY += (dyS / distToStation) * escapeForce;
                    velocityZ += (dzS / distToStation) * escapeForce;
                }

                // Si está MUY cerca, reposicionar instantáneamente
                if (distToStation < stationRadius + 0.2f) {
                    float safeRadius = safeDistanceStation + 0.3f;
                    x = stationX + (dxS / distToStation) * safeRadius;
                    y = stationY + (dyS / distToStation) * safeRadius;
                    z = stationZ + (dzS / distToStation) * safeRadius;
                    Log.d(TAG, "🛰️ DefenderShip evitó colisión con Estación Espacial!");
                }
            }
        }

        // ═══════════════════════════════════════════════════════════
        // 📍 LÍMITES DE LA ESCENA (modo vertical)
        // ═══════════════════════════════════════════════════════════
        if (x < -BOUND_X) { x = -BOUND_X; velocityX = Math.abs(velocityX) * 0.5f; }
        if (x > BOUND_X) { x = BOUND_X; velocityX = -Math.abs(velocityX) * 0.5f; }
        if (y < BOUND_Y_MIN) { y = BOUND_Y_MIN; velocityY = Math.abs(velocityY) * 0.5f; }
        if (y > BOUND_Y_MAX) { y = BOUND_Y_MAX; velocityY = -Math.abs(velocityY) * 0.5f; }
        if (z < BOUND_Z_MIN) { z = BOUND_Z_MIN; velocityZ = Math.abs(velocityZ) * 0.5f; }
        if (z > BOUND_Z_MAX) { z = BOUND_Z_MAX; velocityZ = -Math.abs(velocityZ) * 0.5f; }

        // ═══════════════════════════════════════════════════════════
        // 📐 ESCALA DINÁMICA SEGÚN PROFUNDIDAD (Z)
        // ═══════════════════════════════════════════════════════════
        // Más cerca (Z mayor) = más grande, más lejos (Z menor) = más pequeño
        if (baseScale > 0) {
            float zNormalized = (z - BOUND_Z_MIN) / (BOUND_Z_MAX - BOUND_Z_MIN);  // 0 a 1
            float scaleMultiplier = 0.6f + zNormalized * 0.8f;  // 0.6 a 1.4
            scale = baseScale * scaleMultiplier;
        }

        // ═══════════════════════════════════════════════════════════
        // 🔄 ROTACIÓN NATURAL (yaw, pitch, roll)
        // ═══════════════════════════════════════════════════════════

        // YAW - Dirección de vuelo (basado en velocidad horizontal)
        if (currentSpeed > 0.1f) {
            float targetYaw = (float)Math.toDegrees(Math.atan2(velocityX, velocityZ));
            float yawDiff = targetYaw - rotationY;
            while (yawDiff > 180) yawDiff -= 360;
            while (yawDiff < -180) yawDiff += 360;
            rotationY += yawDiff * 4.0f * deltaTime;

            // ROLL - Banking en curvas (inclinación lateral)
            float targetRoll = -yawDiff * 0.8f;  // Inclinarse hacia la curva
            targetRoll = Math.max(-35f, Math.min(35f, targetRoll));  // Limitar a ±35°
            rotationZ += (targetRoll - rotationZ) * 3.0f * deltaTime;

            // PITCH - Inclinación arriba/abajo según velocidad vertical
            float targetPitch = -velocityY * 20f;
            targetPitch = Math.max(-25f, Math.min(25f, targetPitch));
            rotationX += (targetPitch - rotationX) * 3.0f * deltaTime;
        } else {
            // Volver a nivel cuando está quieto
            rotationZ += (0 - rotationZ) * 2.0f * deltaTime;
            rotationX += (0 - rotationX) * 2.0f * deltaTime;
        }

        // ═══════════════════════════════════════════════════════════
        // 🔫 SISTEMA DE COMBATE - DISPARAR A OVNI1
        // ═══════════════════════════════════════════════════════════
        shootTimer += deltaTime;
        updateLasers(deltaTime);

        // Disparar si hay objetivo válido (prioridad: UfoScout, luego UfoAttacker)
        Object activeTarget = getActiveTarget();
        if (activeTarget != null) {
            if (shootTimer >= shootInterval) {
                shootTimer = 0f;
                // Nuevo intervalo aleatorio
                shootInterval = MIN_SHOOT_INTERVAL + random.nextFloat() * (MAX_SHOOT_INTERVAL - MIN_SHOOT_INTERVAL);

                // Disparar al objetivo activo
                shootAt(getTargetX(), getTargetY(), getTargetZ());
            }
        }

        if (invincibilityTimer > 0) {
            invincibilityTimer -= deltaTime;
        }

        // ═══════════════════════════════════════════════════════════
        // 🛡️ ACTUALIZAR ESCUDO DE ENERGÍA
        // ═══════════════════════════════════════════════════════════
        updateShield(deltaTime);

        // ═══════════════════════════════════════════════════════════
        // 🎯 ACTUALIZAR MISILES RASTREADORES
        // ═══════════════════════════════════════════════════════════
        updateMissiles(deltaTime);
    }


    /**
     * Iniciar celebración por destruir al OVNI
     */
    private void startCelebration() {
        celebrating = true;
        celebrationTimer = 0f;
        celebrationSpins = 0;
        Log.d(TAG, "🎉🎉🎉 ¡VICTORIA! ¡OVNI DESTRUIDO! 🎉🎉🎉");
    }

    /**
     * Recibir daño
     */
    public void takeDamage() {
        if (destroyed || invincibilityTimer > 0) return;

        // 🛡️ Si el escudo está activo, absorbe el daño
        if (shieldActive) {
            Log.d(TAG, "🛡️ ¡Escudo absorbió el impacto!");
            return;
        }

        health--;
        invincibilityTimer = invincibilityDuration;
        Log.d(TAG, "💔 Defender golpeado! HP: " + health);

        if (health <= 0) {
            destroyed = true;
            respawnTimer = 0f;
            // 💥 INICIAR EXPLOSIÓN
            startExplosion();
            Log.d(TAG, "💥 ¡DEFENDER DESTRUIDO!");
        }
    }

    /**
     * Respawn después de ser destruido
     */
    private void respawn() {
        destroyed = false;
        health = maxHealth;
        invincibilityTimer = invincibilityDuration;
        respawnTimer = 0f;

        // Posición aleatoria en órbita
        orbitAngle = random.nextFloat() * (float)(Math.PI * 2);

        Log.d(TAG, "🚀 Defender reapareció con " + health + " HP");
    }

    /**
     * Verificar colisión con láser del OVNI
     */
    public boolean checkLaserCollision(float lx, float ly, float lz, float radius) {
        if (destroyed || invincibilityTimer > 0) return false;

        float dist = distanceTo(lx, ly, lz);
        return dist < (radius + scale * 0.3f);
    }

    /**
     * Distancia a un punto
     */
    private float distanceTo(float tx, float ty, float tz) {
        float dx = x - tx;
        float dy = y - ty;
        float dz = z - tz;
        return (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    /**
     * Getters
     */
    public boolean isDestroyed() { return destroyed; }
    public boolean isCelebrating() { return celebrating; }
    public int getHealth() { return health; }

    @Override
    public void draw() {
        if (camera == null) return;

        // 💥 SIEMPRE dibujar explosión (incluso si la nave está destruida)
        drawExplosion();

        // No dibujar nave si está destruida
        if (destroyed) return;

        // Parpadeo durante invencibilidad
        if (invincibilityTimer > 0) {
            if ((int)(invincibilityTimer * 10) % 2 == 0) return;
        }

        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glUseProgram(shaderProgram);

        // Calcular escala basada en distancia (perspectiva realista)
        // Más lejos de la cámara = más pequeño
        float distanceToCamera = (float)Math.sqrt(x*x + y*y + z*z);
        float distanceScale = 1.0f / (1.0f + distanceToCamera * 0.15f);
        distanceScale = Math.max(0.3f, Math.min(1.2f, distanceScale)); // Clamp

        float dynamicScale = scale * distanceScale;

        // Más grande durante celebración
        if (celebrating) {
            dynamicScale *= (1.0f + (float)Math.sin(celebrationTimer * 15f) * 0.2f);
        }

        // Construir matriz modelo con rotación completa (yaw, pitch, roll)
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);
        Matrix.rotateM(modelMatrix, 0, rotationY, 0, 1, 0);  // Yaw
        Matrix.rotateM(modelMatrix, 0, rotationX, 1, 0, 0);  // Pitch
        Matrix.rotateM(modelMatrix, 0, rotationZ, 0, 0, 1);  // Roll (banking)
        Matrix.scaleM(modelMatrix, 0, dynamicScale, dynamicScale, dynamicScale);

        // MVP
        camera.computeMvp(modelMatrix, mvpMatrix);

        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniformMatrix4fv(uModelMatrixHandle, 1, false, modelMatrix, 0);

        float currentTime = ((System.currentTimeMillis() - startTime) / 1000.0f) % 60.0f;
        GLES20.glUniform1f(uTimeHandle, currentTime);

        // Vértices
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        // UVs
        if (texCoordBuffer != null && aTexCoordHandle >= 0) {
            texCoordBuffer.position(0);
            GLES20.glEnableVertexAttribArray(aTexCoordHandle);
            GLES20.glVertexAttribPointer(aTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);
        }

        // Textura
        if (textureId > 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(uTextureHandle, 0);
        }

        // Dibujar
        indexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        GLES20.glDisableVertexAttribArray(aPositionHandle);
        if (aTexCoordHandle >= 0) {
            GLES20.glDisableVertexAttribArray(aTexCoordHandle);
        }

        // 💚 BARRA DE VIDA - OCULTA VISUALMENTE
        // La funcionalidad de HP sigue activa, solo no se muestra la barra
        // if (healthBar != null && !destroyed) {
        //     healthBar.setHP(health);
        //     healthBar.drawSegmented(x, y, z, mvpMatrix);
        // }

        // ═══ 🔫 DIBUJAR LÁSERES ═══
        drawLasers();

        // ═══ 🛡️ DIBUJAR ESCUDO ═══
        if (shieldActive) {
            drawShield();
        }

        // ═══ 🎯 DIBUJAR MISILES ═══
        drawMissiles();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🛡️ ESCUDO DE ENERGÍA - MÉTODOS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * 🛡️ Actualiza el estado del escudo
     */
    private void updateShield(float deltaTime) {
        // Actualizar cooldown
        if (shieldCooldown > 0) {
            shieldCooldown -= deltaTime;
        }

        // Si el escudo está activo
        if (shieldActive) {
            shieldTimer += deltaTime;
            shieldPulse += deltaTime * 8f;  // Pulsación rápida

            // Desactivar cuando termine la duración
            if (shieldTimer >= shieldDuration) {
                shieldActive = false;
                shieldTimer = 0f;
                shieldCooldown = shieldCooldownMax;
                Log.d(TAG, "🛡️ Escudo desactivado");
            }
        } else {
            // Activar automáticamente cuando recibe mucho daño o en combate intenso
            if (shieldCooldown <= 0 && health <= 2 && health > 0) {
                // Activar escudo de emergencia cuando tiene poca vida
                if (Math.random() < 0.02 * deltaTime * 60) {  // ~2% por segundo
                    activateShield();
                }
            }
        }
    }

    /**
     * 🛡️ Activa el escudo de energía
     */
    public void activateShield() {
        if (shieldCooldown <= 0 && !shieldActive && !destroyed) {
            shieldActive = true;
            shieldTimer = 0f;
            shieldPulse = 0f;
            Log.d(TAG, "🛡️ ¡ESCUDO DE ENERGÍA ACTIVADO!");
        }
    }

    /**
     * 🛡️ Dibuja el escudo visual (esfera pulsante)
     * OPTIMIZADO: Usa buffers pre-asignados para evitar GC
     */
    private void drawShield() {
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);

        // Dibujar múltiples anillos para efecto de escudo
        float pulse = (float)(0.7f + 0.3f * Math.sin(shieldPulse));
        float alpha = 0.4f * pulse;

        // Radio pulsante
        float currentRadius = shieldRadius * (0.9f + 0.1f * (float)Math.sin(shieldPulse * 2));

        for (int ring = 0; ring < 3; ring++) {
            float ringRadius = currentRadius * (0.8f + ring * 0.15f);
            float ringAlpha = alpha * (1f - ring * 0.25f);

            // Usar arrays pre-asignados (no crear new cada frame)
            for (int i = 0; i < SHIELD_SEGMENTS; i++) {
                float angle = (float)(i * Math.PI * 2 / SHIELD_SEGMENTS);
                shieldVertices[i * 3] = x + (float)Math.cos(angle) * ringRadius;
                shieldVertices[i * 3 + 1] = y + (float)Math.sin(angle + shieldPulse * 0.5f) * ringRadius * 0.3f;
                shieldVertices[i * 3 + 2] = z + (float)Math.sin(angle) * ringRadius;

                // Color azul brillante
                shieldColors[i * 4] = 0.3f;
                shieldColors[i * 4 + 1] = 0.7f;
                shieldColors[i * 4 + 2] = 1.0f;
                shieldColors[i * 4 + 3] = ringAlpha;
            }

            // Usar buffers pre-asignados (no ByteBuffer.allocateDirect cada frame)
            shieldVertexBuffer.clear();
            shieldVertexBuffer.put(shieldVertices).position(0);

            shieldColorBuffer.clear();
            shieldColorBuffer.put(shieldColors).position(0);

            if (explosionProgram > 0) {
                GLES20.glUseProgram(explosionProgram);
                GLES20.glUniform1f(expPointSizeLoc, 12.0f * pulse);
                GLES20.glVertexAttribPointer(expPositionLoc, 3, GLES20.GL_FLOAT, false, 0, shieldVertexBuffer);
                GLES20.glEnableVertexAttribArray(expPositionLoc);
                GLES20.glVertexAttribPointer(expColorLoc, 4, GLES20.GL_FLOAT, false, 0, shieldColorBuffer);
                GLES20.glEnableVertexAttribArray(expColorLoc);
                GLES20.glDrawArrays(GLES20.GL_POINTS, 0, SHIELD_SEGMENTS);
            }
        }

        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public boolean isShieldActive() {
        return shieldActive;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🎯 MISILES RASTREADORES - MÉTODOS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * 🎯 Actualiza los misiles rastreadores
     */
    private void updateMissiles(float deltaTime) {
        // Actualizar cooldown
        if (missileCooldown > 0) {
            missileCooldown -= deltaTime;
        }

        // Actualizar misiles activos
        for (int i = 0; i < MAX_MISSILES; i++) {
            if (missileActive[i]) {
                missileLife[i] -= deltaTime;

                if (missileLife[i] <= 0) {
                    missileActive[i] = false;
                    continue;
                }

                // Buscar objetivo más cercano
                float targetMX = missileX[i];
                float targetMY = missileY[i];
                float targetMZ = missileZ[i];
                float closestDist = Float.MAX_VALUE;
                boolean hasTarget = false;

                // Buscar UfoScout
                if (targetUfoScout != null && !targetUfoScout.isDestroyed() && !targetUfoScout.isTeleportingNow()) {
                    float dx = targetUfoScout.getX() - missileX[i];
                    float dy = targetUfoScout.getY() - missileY[i];
                    float dz = targetUfoScout.getZ() - missileZ[i];
                    float dist = dx * dx + dy * dy + dz * dz;
                    if (dist < closestDist) {
                        closestDist = dist;
                        targetMX = targetUfoScout.getX();
                        targetMY = targetUfoScout.getY();
                        targetMZ = targetUfoScout.getZ();
                        hasTarget = true;
                    }
                }

                // Buscar UfoAttacker
                if (targetUfoAttacker != null && !targetUfoAttacker.isDestroyed()) {
                    float dx = targetUfoAttacker.getX() - missileX[i];
                    float dy = targetUfoAttacker.getY() - missileY[i];
                    float dz = targetUfoAttacker.getZ() - missileZ[i];
                    float dist = dx * dx + dy * dy + dz * dz;
                    if (dist < closestDist) {
                        closestDist = dist;
                        targetMX = targetUfoAttacker.getX();
                        targetMY = targetUfoAttacker.getY();
                        targetMZ = targetUfoAttacker.getZ();
                        hasTarget = true;
                    }
                }

                if (hasTarget) {
                    // Girar hacia el objetivo
                    float dx = targetMX - missileX[i];
                    float dy = targetMY - missileY[i];
                    float dz = targetMZ - missileZ[i];
                    float dist = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);

                    if (dist > 0.01f) {
                        float targetVX = dx / dist;
                        float targetVY = dy / dist;
                        float targetVZ = dz / dist;

                        // Interpolar hacia la dirección objetivo
                        float turn = missileTurnRate * deltaTime;
                        missileVX[i] += (targetVX - missileVX[i]) * turn;
                        missileVY[i] += (targetVY - missileVY[i]) * turn;
                        missileVZ[i] += (targetVZ - missileVZ[i]) * turn;

                        // Normalizar velocidad
                        float vLen = (float)Math.sqrt(missileVX[i]*missileVX[i] + missileVY[i]*missileVY[i] + missileVZ[i]*missileVZ[i]);
                        if (vLen > 0.01f) {
                            missileVX[i] /= vLen;
                            missileVY[i] /= vLen;
                            missileVZ[i] /= vLen;
                        }
                    }

                    // Verificar colisión
                    if (dist < 0.4f) {
                        missileActive[i] = false;
                        // Aplicar daño
                        if (targetUfoScout != null && !targetUfoScout.isDestroyed()) {
                            float dxs = targetUfoScout.getX() - missileX[i];
                            float dys = targetUfoScout.getY() - missileY[i];
                            float dzs = targetUfoScout.getZ() - missileZ[i];
                            if (dxs*dxs + dys*dys + dzs*dzs < 0.5f) {
                                targetUfoScout.takeDamage();
                                targetUfoScout.takeDamage();  // Doble daño de misil
                                Log.d(TAG, "🎯💥 ¡Misil impactó a UfoScout!");
                            }
                        }
                        if (targetUfoAttacker != null && !targetUfoAttacker.isDestroyed()) {
                            float dxa = targetUfoAttacker.getX() - missileX[i];
                            float dya = targetUfoAttacker.getY() - missileY[i];
                            float dza = targetUfoAttacker.getZ() - missileZ[i];
                            if (dxa*dxa + dya*dya + dza*dza < 0.5f) {
                                targetUfoAttacker.takeDamage();
                                targetUfoAttacker.takeDamage();  // Doble daño de misil
                                Log.d(TAG, "🎯💥 ¡Misil impactó a UfoAttacker!");
                            }
                        }
                        continue;
                    }
                }

                // Mover misil
                missileX[i] += missileVX[i] * missileSpeed * deltaTime;
                missileY[i] += missileVY[i] * missileSpeed * deltaTime;
                missileZ[i] += missileVZ[i] * missileSpeed * deltaTime;
            }
        }

        // Lanzar misiles automáticamente cuando hay enemigos cerca
        if (missileCooldown <= 0 && !destroyed) {
            boolean enemyNearby = false;
            if (targetUfoScout != null && !targetUfoScout.isDestroyed()) enemyNearby = true;
            if (targetUfoAttacker != null && !targetUfoAttacker.isDestroyed()) enemyNearby = true;

            if (enemyNearby && Math.random() < 0.01 * deltaTime * 60) {  // ~1% por segundo
                fireMissiles();
            }
        }
    }

    /**
     * 🎯 Dispara una ráfaga de misiles rastreadores
     */
    public void fireMissiles() {
        if (missileCooldown > 0 || destroyed) return;

        Log.d(TAG, "🎯🚀 ¡MISILES RASTREADORES LANZADOS!");
        missileCooldown = missileCooldownMax;

        // Lanzar todos los misiles en diferentes direcciones
        for (int i = 0; i < MAX_MISSILES; i++) {
            missileActive[i] = true;
            missileLife[i] = missileLifeMax;
            missileX[i] = x;
            missileY[i] = y;
            missileZ[i] = z;

            // Dirección inicial en abanico
            float angle = (float)(i * Math.PI * 0.3f - Math.PI * 0.45f);
            missileVX[i] = (float)Math.sin(rotationY * Math.PI / 180 + angle);
            missileVY[i] = 0.2f;
            missileVZ[i] = (float)Math.cos(rotationY * Math.PI / 180 + angle);
        }
    }

    /**
     * 🎯 Dibuja los misiles activos
     * OPTIMIZADO: Usa buffers pre-asignados para evitar GC
     */
    private void drawMissiles() {
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);

        for (int i = 0; i < MAX_MISSILES; i++) {
            if (missileActive[i]) {
                // Usar arrays pre-asignados (no crear new cada frame)
                for (int j = 0; j < MISSILE_TRAIL_POINTS; j++) {
                    float t = j * 0.05f;
                    missileTrailVerts[j * 3] = missileX[i] - missileVX[i] * t;
                    missileTrailVerts[j * 3 + 1] = missileY[i] - missileVY[i] * t;
                    missileTrailVerts[j * 3 + 2] = missileZ[i] - missileVZ[i] * t;

                    float alpha = 1f - j * 0.15f;
                    missileTrailCols[j * 4] = 1.0f;      // Rojo
                    missileTrailCols[j * 4 + 1] = 0.5f - j * 0.08f;  // Naranja a rojo
                    missileTrailCols[j * 4 + 2] = 0.1f;
                    missileTrailCols[j * 4 + 3] = alpha;
                }

                // Usar buffers pre-asignados (no ByteBuffer.allocateDirect cada frame)
                missileTrailVertexBuffer.clear();
                missileTrailVertexBuffer.put(missileTrailVerts).position(0);

                missileTrailColorBuffer.clear();
                missileTrailColorBuffer.put(missileTrailCols).position(0);

                if (explosionProgram > 0) {
                    GLES20.glUseProgram(explosionProgram);
                    GLES20.glUniform1f(expPointSizeLoc, 15.0f);
                    GLES20.glVertexAttribPointer(expPositionLoc, 3, GLES20.GL_FLOAT, false, 0, missileTrailVertexBuffer);
                    GLES20.glEnableVertexAttribArray(expPositionLoc);
                    GLES20.glVertexAttribPointer(expColorLoc, 4, GLES20.GL_FLOAT, false, 0, missileTrailColorBuffer);
                    GLES20.glEnableVertexAttribArray(expColorLoc);
                    GLES20.glDrawArrays(GLES20.GL_POINTS, 0, MISSILE_TRAIL_POINTS);
                }
            }
        }

        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    // =========================================================================
    // LIMPIEZA DE RECURSOS OPENGL
    // =========================================================================

    /**
     * Libera todos los recursos OpenGL asociados a esta nave.
     * DEBE llamarse cuando la escena se destruye o la nave ya no se usa.
     */
    public void cleanup() {
        Log.d(TAG, "=== CLEANUP DefenderShip ===");

        // Eliminar shader programs
        if (shaderProgram != 0) {
            GLES20.glDeleteProgram(shaderProgram);
            Log.d(TAG, "  Shader program eliminado: " + shaderProgram);
            shaderProgram = 0;
        }

        if (explosionProgram != 0) {
            GLES20.glDeleteProgram(explosionProgram);
            Log.d(TAG, "  Explosion program eliminado: " + explosionProgram);
            explosionProgram = 0;
        }

        // Eliminar textura
        if (textureId != 0) {
            GLES20.glDeleteTextures(1, new int[]{textureId}, 0);
            Log.d(TAG, "  Textura eliminada: " + textureId);
            textureId = 0;
        }

        // Limpiar buffers (Java memory, no necesitan glDelete)
        vertexBuffer = null;
        texCoordBuffer = null;
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
