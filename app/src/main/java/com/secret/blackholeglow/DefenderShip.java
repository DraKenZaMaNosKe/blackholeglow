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

    // ═══════════════════════════════════════════════════════════
    // 🎯 SISTEMA DE COMBATE
    // ═══════════════════════════════════════════════════════════
    private Spaceship3D targetUfo;          // Referencia al OVNI enemigo
    private float shootTimer = 0f;
    private float shootInterval = 2.5f;     // Dispara cada 2.5 segundos
    private float minShootInterval = 1.5f;
    private float maxShootInterval = 3.5f;

    // Láseres
    private ArrayList<DefenderLaser> lasers = new ArrayList<>();

    // ═══════════════════════════════════════════════════════════
    // 💔 SISTEMA DE VIDA
    // ═══════════════════════════════════════════════════════════
    private int health = 3;
    private int maxHealth = 3;
    private boolean destroyed = false;
    private float respawnTimer = 0f;
    private float respawnDelay = 6.0f;
    private float invincibilityTimer = 0f;
    private float invincibilityDuration = 1.5f;

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

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    /**
     * Establecer el OVNI enemigo como objetivo
     */
    public void setTargetUfo(Spaceship3D ufo) {
        this.targetUfo = ufo;
        Log.d(TAG, "🎯 Objetivo establecido: OVNI enemigo");
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

    @Override
    public void update(float deltaTime) {
        // Si está destruida, esperar respawn
        if (destroyed) {
            respawnTimer += deltaTime;
            if (respawnTimer >= respawnDelay) {
                respawn();
            }
            updateLasers(deltaTime);
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
            updateLasers(deltaTime);
            return;
        }

        // ═══════════════════════════════════════════════════════════
        // 🎯 CALCULAR OBJETIVO Y DIRECCIÓN
        // ═══════════════════════════════════════════════════════════
        float targetX, targetY, targetZ;
        boolean hasVisibleTarget = false;

        // Verificar si el OVNI está visible (no destruido, no teletransportándose)
        boolean ufoVisible = targetUfo != null && !targetUfo.isDestroyed() && !targetUfo.isTeleportingNow();

        if (ufoVisible) {
            hasVisibleTarget = true;
            // Apuntar al OVNI
            targetX = targetUfo.x;
            targetY = targetUfo.y;
            targetZ = targetUfo.z;

            float distToUfo = distanceTo(targetX, targetY, targetZ);

            // 🚫 EVITAR COLISIÓN - mantener distancia mínima de 0.8
            if (distToUfo < 0.8f) {
                // Alejarse del OVNI
                float dx = x - targetUfo.x;
                float dy = y - targetUfo.y;
                float dz = z - targetUfo.z;
                float dist = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
                if (dist > 0.01f) {
                    targetX = x + (dx / dist) * 2.0f;
                    targetY = y + (dy / dist) * 1.0f;
                    targetZ = z + (dz / dist) * 2.0f;
                }
            } else if (distToUfo < 1.5f) {
                // Rodear al OVNI a distancia de ataque
                float angle = (float)Math.atan2(z - targetUfo.z, x - targetUfo.x);
                angle += 2.0f * deltaTime;
                targetX = targetUfo.x + (float)Math.cos(angle) * 1.5f;
                targetZ = targetUfo.z + (float)Math.sin(angle) * 1.5f;
                targetY = targetUfo.y + 0.3f;
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
        // 📍 LÍMITES DE LA ESCENA
        // ═══════════════════════════════════════════════════════════
        float minX = -2.5f, maxX = 2.5f;
        float minY = -0.5f, maxY = 3.5f;
        float minZ = -2.5f, maxZ = 2.5f;

        if (x < minX) { x = minX; velocityX = Math.abs(velocityX) * 0.5f; }
        if (x > maxX) { x = maxX; velocityX = -Math.abs(velocityX) * 0.5f; }
        if (y < minY) { y = minY; velocityY = Math.abs(velocityY) * 0.5f; }
        if (y > maxY) { y = maxY; velocityY = -Math.abs(velocityY) * 0.5f; }
        if (z < minZ) { z = minZ; velocityZ = Math.abs(velocityZ) * 0.5f; }
        if (z > maxZ) { z = maxZ; velocityZ = -Math.abs(velocityZ) * 0.5f; }

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
        // 🔫 SISTEMA DE DISPARO AGRESIVO
        // ═══════════════════════════════════════════════════════════
        shootTimer += deltaTime;
        if (targetUfo != null && !targetUfo.isDestroyed() && !targetUfo.isTeleportingNow()) {
            float distToUfo = distanceTo(targetUfo.x, targetUfo.y, targetUfo.z);

            // Disparar más frecuentemente cuando está cerca
            float effectiveInterval = shootInterval;
            if (distToUfo < 2.0f) effectiveInterval *= 0.5f;  // Doble velocidad de disparo

            if (shootTimer >= effectiveInterval && distToUfo < 6.0f) {
                shootLaser();
                shootTimer = 0f;
                shootInterval = minShootInterval + random.nextFloat() * (maxShootInterval - minShootInterval);
            }
        }

        updateLasers(deltaTime);

        if (invincibilityTimer > 0) {
            invincibilityTimer -= deltaTime;
        }
    }

    /**
     * Actualizar láseres
     */
    private void updateLasers(float deltaTime) {
        for (int i = lasers.size() - 1; i >= 0; i--) {
            DefenderLaser laser = lasers.get(i);
            laser.update(deltaTime);

            // Verificar impacto con OVNI
            if (laser.active && targetUfo != null && !targetUfo.isDestroyed()) {
                float dist = laser.distanceTo(targetUfo.x, targetUfo.y, targetUfo.z);
                if (dist < 0.4f) {  // Radio de colisión
                    laser.active = false;
                    targetUfo.takeDamage();
                    Log.d(TAG, "💥 ¡Impacto en OVNI!");

                    // Si destruimos al OVNI, celebrar!
                    if (targetUfo.isDestroyed()) {
                        startCelebration();
                    }
                }
            }

            if (!laser.active) {
                lasers.remove(i);
            }
        }
    }

    /**
     * Disparar láser hacia el OVNI
     */
    private void shootLaser() {
        if (targetUfo == null) return;

        DefenderLaser laser = new DefenderLaser(
            x, y, z,
            targetUfo.x, targetUfo.y, targetUfo.z
        );
        lasers.add(laser);
        Log.d(TAG, "🔫 Defender disparó! Láseres activos: " + lasers.size());
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

        health--;
        invincibilityTimer = invincibilityDuration;
        Log.d(TAG, "💔 Defender golpeado! HP: " + health);

        if (health <= 0) {
            destroyed = true;
            respawnTimer = 0f;
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
    public ArrayList<DefenderLaser> getLasers() { return lasers; }

    @Override
    public void draw() {
        if (camera == null) return;

        // Dibujar láseres siempre
        Matrix.setIdentityM(identityModel, 0);
        camera.computeMvp(identityModel, laserMvp);
        for (DefenderLaser laser : lasers) {
            if (laser.active) {
                laser.draw(laserMvp);
            }
        }

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

        // 💚 DIBUJAR BARRA DE VIDA
        if (healthBar != null && !destroyed) {
            healthBar.setHP(health);
            healthBar.drawSegmented(x, y, z, mvpMatrix);
        }
    }
}
