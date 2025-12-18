package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES30;
import android.util.Log;

import com.secret.blackholeglow.util.MaterialGroup;
import com.secret.blackholeglow.util.MtlLoader;
import com.secret.blackholeglow.util.ObjLoaderWithMaterials;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TierraLiveHD - Modelo de Tierra de alta definición con vegetación
 * Modelo Low-Poly estilizado con árboles, plantas y terreno
 * Exportado desde Sketchfab con ~20k triángulos
 *
 * ✨ VERSIÓN CON MATERIALES - Usa colores del archivo MTL
 */
public class TierraLiveHD implements SceneObject, CameraAware {
    private static final String TAG = "TierraLiveHD";

    private final Context context;
    private final TextureManager textureManager;
    private CameraController camera;

    // Modelo 3D con materiales
    private ObjLoaderWithMaterials.MeshWithMaterials mesh;
    private FloatBuffer vertexBuffer;
    private FloatBuffer uvBuffer;
    private int vertexCount;
    private List<MaterialGroup> materialGroups;
    private Map<MaterialGroup, java.nio.IntBuffer> indexBuffers = new HashMap<>();

    // Shader program
    private int shaderProgram;
    private int uMVPHandle;
    private int uColorHandle;  // ✨ Uniform para color del material
    private int uTimeHandle;   // ✨ Uniform para animaciones
    private int uIsWaterHandle; // ✨ Uniform para detectar agua
    private int aPositionHandle;
    private int aTexCoordHandle;

    // Transformación
    private float rotationY = 0.0f;
    private float spinSpeed = 20.0f;  // Rotación lenta
    private float scale = 1.0f;

    // ✅ FIX: Tiempo relativo para evitar números gigantes
    private final long startTime = System.currentTimeMillis();

    /**
     * Constructor
     */
    public TierraLiveHD(Context ctx, TextureManager texManager, float scale) {
        this.context = ctx;
        this.textureManager = texManager;
        this.scale = scale;

        loadModel();
        createShaderProgram();
    }

    /**
     * Cargar modelo OBJ con materiales MTL
     */
    private void loadModel() {
        try {
            Log.d(TAG, "════════════════════════════════════════════════");
            Log.d(TAG, "Cargando modelo liveEarth.obj con materiales...");

            // Cargar modelo con grupos de materiales
            mesh = ObjLoaderWithMaterials.loadObjWithMaterials(
                    context,
                    "liveEarth.obj",
                    "liveEarth.mtl"
            );

            // Cargar materiales del archivo MTL
            Map<String, MtlLoader.Material> materials = MtlLoader.loadMtl(context, "liveEarth.mtl");

            Log.d(TAG, "✓ Materiales cargados: " + materials.size());
            for (String matName : materials.keySet()) {
                MtlLoader.Material mat = materials.get(matName);
                Log.d(TAG, "  - " + matName + ": RGB(" +
                        mat.diffuseColor[0] + ", " +
                        mat.diffuseColor[1] + ", " +
                        mat.diffuseColor[2] + ")");
            }

            // Asignar materiales a grupos
            materialGroups = mesh.materialGroups;
            for (MaterialGroup group : materialGroups) {
                group.material = materials.get(group.materialName);
                if (group.material == null) {
                    Log.w(TAG, "⚠️  Material no encontrado: " + group.materialName);
                }
            }

            // Crear buffers
            vertexBuffer = mesh.vertexBuffer;
            uvBuffer = mesh.uvBuffer;
            vertexCount = mesh.vertexCount;

            // Crear index buffers para cada grupo de material
            // ✅ FIX: Usa IntBuffer para modelos >32k vértices
            for (MaterialGroup group : materialGroups) {
                java.nio.IntBuffer indexBuffer = ObjLoaderWithMaterials.buildIndexBufferForGroup(group);
                indexBuffers.put(group, indexBuffer);
                Log.d(TAG, "  - " + group.materialName + ": " +
                        group.faces.size() + " caras, " +
                        group.getTriangleCount() + " triángulos");
            }

            Log.d(TAG, "✓ Tierra Live HD cargada:");
            Log.d(TAG, "  Vértices: " + vertexCount);
            Log.d(TAG, "  Grupos de materiales: " + materialGroups.size());
            Log.d(TAG, "════════════════════════════════════════════════");

        } catch (IOException e) {
            Log.e(TAG, "ERROR cargando liveEarth.obj: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ✨ CREAR SHADER PROGRAM ÉPICO CON EFECTOS PROCEDURALES
     */
    private void createShaderProgram() {
        // ═══════════════════════════════════════════════════════════════
        // VERTEX SHADER - Calcula normales y pasa datos al fragment
        // ═══════════════════════════════════════════════════════════════
        String vertexShader =
                "uniform mat4 u_MVP;\n" +
                "attribute vec4 a_Position;\n" +
                "attribute vec2 a_TexCoord;\n" +
                "varying vec2 v_TexCoord;\n" +
                "varying vec3 v_Normal;\n" +     // Normal para iluminación
                "varying vec3 v_WorldPos;\n" +   // Posición para efectos
                "void main() {\n" +
                "    gl_Position = u_MVP * a_Position;\n" +
                "    v_TexCoord = a_TexCoord;\n" +
                // Para una esfera, la normal es la posición normalizada
                "    v_Normal = normalize(a_Position.xyz);\n" +
                "    v_WorldPos = a_Position.xyz;\n" +
                "}";

        // ═══════════════════════════════════════════════════════════════
        // FRAGMENT SHADER ÉPICO - Iluminación + Atmósfera + ☁️ NUBES 3D
        // ═══════════════════════════════════════════════════════════════
        String fragmentShader =
                "precision mediump float;\n" +
                "\n" +
                "varying vec2 v_TexCoord;\n" +
                "varying vec3 v_Normal;\n" +
                "varying vec3 v_WorldPos;\n" +
                "\n" +
                "uniform vec3 u_Color;\n" +
                "uniform float u_Time;\n" +
                "uniform float u_IsWater;\n" +
                "\n" +
                // ─── Función de noise 2D ────
                "float noise(vec2 st) {\n" +
                "    return fract(sin(dot(st, vec2(12.9898, 78.233))) * 43758.5453);\n" +
                "}\n" +
                "\n" +
                // ─── Función de noise 3D ────
                "float noise3D(vec3 p) {\n" +
                "    vec3 i = floor(p);\n" +
                "    vec3 f = fract(p);\n" +
                "    f = f * f * (3.0 - 2.0 * f);\n" +
                "    \n" +
                "    float n = i.x + i.y * 157.0 + 113.0 * i.z;\n" +
                "    return fract(sin(n) * 43758.5453);\n" +
                "}\n" +
                "\n" +
                // ☁️ FBM (Fractional Brownian Motion) para nubes volumétricas
                "float fbm(vec3 p) {\n" +
                "    float value = 0.0;\n" +
                "    float amplitude = 0.5;\n" +
                "    \n" +
                "    for(int i = 0; i < 4; i++) {\n" +
                "        value += amplitude * noise3D(p);\n" +
                "        p *= 2.0;\n" +
                "        amplitude *= 0.5;\n" +
                "    }\n" +
                "    return value;\n" +
                "}\n" +
                "\n" +
                "void main() {\n" +
                "    vec3 normal = normalize(v_Normal);\n" +
                "    \n" +
                "    // ═══ ILUMINACIÓN ═══\n" +
                "    vec3 lightDir = normalize(vec3(0.5, 0.3, 1.0));\n" +
                "    float diffuse = max(dot(normal, lightDir), 0.0);\n" +
                "    \n" +
                "    float ambient = 0.4;\n" +
                "    float lighting = ambient + diffuse * 0.8;\n" +
                "    \n" +
                "    // ═══ COLOR BASE ═══\n" +
                "    vec3 baseColor = u_Color;\n" +
                "    \n" +
                "    // ═══ EFECTOS EN AGUA ═══\n" +
                "    if (u_IsWater > 0.5) {\n" +
                "        vec2 waveUV = v_TexCoord * 15.0 + u_Time * 0.1;\n" +
                "        float wave = noise(waveUV) * 0.15;\n" +
                "        baseColor += vec3(wave);\n" +
                "        \n" +
                "        vec3 viewDir = normalize(-v_WorldPos);\n" +
                "        vec3 halfVector = normalize(lightDir + viewDir);\n" +
                "        float spec = pow(max(dot(normal, halfVector), 0.0), 32.0);\n" +
                "        baseColor += vec3(1.0, 0.98, 0.9) * spec * 0.5;\n" +
                "    }\n" +
                "    \n" +
                "    // ═══ VARIACIÓN EN VEGETACIÓN ═══\n" +
                "    if (u_IsWater < 0.5 && baseColor.g > baseColor.r) {\n" +
                "        float variation = noise(v_TexCoord * 30.0) * 0.1;\n" +
                "        baseColor += vec3(variation * 0.3, variation, variation * 0.2);\n" +
                "    }\n" +
                "    \n" +
                "    // ☁️☁️☁️ NUBES PROCEDURALES 3D (NUBES GRANDES) ☁️☁️☁️\n" +
                "    // Posición de nubes: esfera ligeramente más grande que el planeta\n" +
                "    vec3 cloudPos = v_WorldPos * 1.05;  // Más cerca de la superficie\n" +
                "    \n" +
                "    // Movimiento de nubes con el tiempo\n" +
                "    cloudPos.x += u_Time * 0.03;  // Nubes se mueven más rápido\n" +
                "    cloudPos.y += u_Time * 0.015;  // Movimiento vertical más visible\n" +
                "    \n" +
                "    // Calcular densidad de nubes usando FBM\n" +
                "    // ESCALA MÁS PEQUEÑA = NUBES MÁS GRANDES (1.5 → 0.8)\n" +
                "    float cloudDensity = fbm(cloudPos * 0.8);\n" +
                "    \n" +
                "    // Umbral MÁS BAJO = Más nubes (0.40 → 0.35)\n" +
                "    float cloudThreshold = 0.35;\n" +
                "    float cloudAmount = smoothstep(cloudThreshold, cloudThreshold + 0.25, cloudDensity);\n" +
                "    \n" +
                "    // Color de las nubes (blanco más brillante y opaco)\n" +
                "    vec3 cloudColor = vec3(1.0, 1.0, 1.0);  // Blanco puro\n" +
                "    \n" +
                "    // Iluminación de nubes más brillante\n" +
                "    float cloudLighting = ambient + max(dot(normal, lightDir), 0.0) * 0.8;\n" +
                "    cloudColor *= cloudLighting;\n" +
                "    \n" +
                "    // Sombras de nubes en el terreno más marcadas\n" +
                "    float cloudShadow = 1.0 - cloudAmount * 0.4;\n" +
                "    lighting *= cloudShadow;\n" +
                "    \n" +
                "    // Aplicar iluminación a superficie\n" +
                "    vec3 litColor = baseColor * lighting;\n" +
                "    \n" +
                "    // 🐛 MODO DEBUG: Visualizar densidad de nubes\n" +
                "    // Descomentar para ver en rojo dónde deberían aparecer nubes:\n" +
                "    // litColor = mix(litColor, vec3(1.0, 0.0, 0.0), cloudDensity);  // Rojo = densidad\n" +
                "    // litColor = mix(litColor, vec3(0.0, 1.0, 0.0), cloudAmount);   // Verde = nubes finales\n" +
                "    \n" +
                "    // Mezclar nubes sobre la superficie (MÁS OPACAS: 0.7 → 0.9)\n" +
                "    litColor = mix(litColor, cloudColor, cloudAmount * 0.9);\n" +
                "    \n" +
                "    // 🔥 DEBUG EXTREMO: Forzar nubes en todo el planeta\n" +
                "    // Descomentar para ver si el problema es el FBM o el rendering:\n" +
                "    // litColor = mix(litColor, vec3(1.0, 1.0, 1.0), 0.5);  // 50% blanco en toda la esfera\n" +
                "    \n" +
                "    // ═══ ATMÓSFERA (glow azul en bordes) ═══\n" +
                "    vec3 viewDir = normalize(-v_WorldPos);\n" +
                "    float fresnel = pow(1.0 - max(dot(normal, viewDir), 0.0), 3.0);\n" +
                "    vec3 atmosphereColor = vec3(0.4, 0.6, 1.0);\n" +
                "    litColor += atmosphereColor * fresnel * 0.3;\n" +
                "    \n" +
                "    gl_FragColor = vec4(litColor, 1.0);\n" +
                "}";

        int vShader = ShaderUtils.loadShader(GLES30.GL_VERTEX_SHADER, vertexShader);
        int fShader = ShaderUtils.loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShader);

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vShader);
        GLES30.glAttachShader(shaderProgram, fShader);
        GLES30.glLinkProgram(shaderProgram);

        // Get handles
        uMVPHandle = GLES30.glGetUniformLocation(shaderProgram, "u_MVP");
        uColorHandle = GLES30.glGetUniformLocation(shaderProgram, "u_Color");
        uTimeHandle = GLES30.glGetUniformLocation(shaderProgram, "u_Time");
        uIsWaterHandle = GLES30.glGetUniformLocation(shaderProgram, "u_IsWater");
        aPositionHandle = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        aTexCoordHandle = GLES30.glGetAttribLocation(shaderProgram, "a_TexCoord");

        Log.d(TAG, "✨ Shader épico creado con iluminación + atmósfera");
    }

    @Override
    public void setCameraController(CameraController cameraController) {
        this.camera = cameraController;
    }

    @Override
    public void update(float deltaTime) {
        // Rotación constante
        rotationY += spinSpeed * deltaTime;
        if (rotationY >= 360.0f) rotationY -= 360.0f;
    }

    // Variables para logging periódico
    private int frameCounter = 0;
    private long lastLogTime = 0;

    @Override
    public void draw() {
        if (mesh == null || camera == null) return;

        GLES30.glUseProgram(shaderProgram);

        // Matriz de transformación
        float[] modelMatrix = new float[16];
        float[] mvpMatrix = new float[16];

        android.opengl.Matrix.setIdentityM(modelMatrix, 0);
        android.opengl.Matrix.scaleM(modelMatrix, 0, scale, scale, scale);
        android.opengl.Matrix.rotateM(modelMatrix, 0, rotationY, 0.0f, 1.0f, 0.0f);

        // Calcular MVP usando CameraController
        camera.computeMvp(modelMatrix, mvpMatrix);

        // Set MVP uniform (compartido por todos los materiales)
        GLES30.glUniformMatrix4fv(uMVPHandle, 1, false, mvpMatrix, 0);

        // ✨ Set tiempo para animaciones (TIEMPO RELATIVO CÍCLICO)
        // ✅ CRÍTICO: Módulo 60s para evitar pérdida de precisión en GLSL mediump float
        float currentTime = ((System.currentTimeMillis() - startTime) / 1000.0f) % 60.0f;
        GLES30.glUniform1f(uTimeHandle, currentTime);

        // 🐛 LOG DE DEBUG CADA 60 FRAMES (1 segundo aprox)
        frameCounter++;
        long now = System.currentTimeMillis();
        if (frameCounter % 60 == 0 || (now - lastLogTime) > 2000) {
            Log.d(TAG, "═══════════════════════════════════════");
            Log.d(TAG, "🐛 DEBUG NUBES - Frame: " + frameCounter);
            Log.d(TAG, "  u_Time: " + currentTime);
            Log.d(TAG, "  uTimeHandle: " + uTimeHandle);
            Log.d(TAG, "  uColorHandle: " + uColorHandle);
            Log.d(TAG, "  uIsWaterHandle: " + uIsWaterHandle);
            Log.d(TAG, "  Scale: " + scale);
            Log.d(TAG, "  Rotation Y: " + rotationY);
            Log.d(TAG, "═══════════════════════════════════════");
            lastLogTime = now;
        }

        // Set vertex buffer (compartido)
        vertexBuffer.position(0);
        GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);
        GLES30.glEnableVertexAttribArray(aPositionHandle);

        if (aTexCoordHandle >= 0) {
            uvBuffer.position(0);
            GLES30.glVertexAttribPointer(aTexCoordHandle, 2, GLES30.GL_FLOAT, false, 0, uvBuffer);
            GLES30.glEnableVertexAttribArray(aTexCoordHandle);
        }

        // ═══════════════════════════════════════════════════════════════
        // 🎨 RENDERIZAR CADA GRUPO DE MATERIAL CON SU COLOR
        // ═══════════════════════════════════════════════════════════════
        for (MaterialGroup group : materialGroups) {
            // ✨ Detectar si es agua para efectos especiales
            boolean isWater = group.materialName != null && group.materialName.equals("Water");
            GLES30.glUniform1f(uIsWaterHandle, isWater ? 1.0f : 0.0f);

            if (group.material != null) {
                // ✅ FIX: Cambiar "material" gris a marrón tierra
                if (group.materialName != null && group.materialName.equals("material")) {
                    // Material genérico: usar marrón tierra/roca
                    GLES30.glUniform3f(uColorHandle, 0.45f, 0.35f, 0.25f);  // Marrón tierra
                } else {
                    // Usar color del archivo MTL
                    float[] diffuseColor = group.material.diffuseColor;
                    GLES30.glUniform3f(uColorHandle, diffuseColor[0], diffuseColor[1], diffuseColor[2]);
                }
            } else {
                // Fallback: marrón oscuro
                GLES30.glUniform3f(uColorHandle, 0.3f, 0.25f, 0.2f);
            }

            // Draw este grupo con índices
            // ✅ FIX: Usa IntBuffer + GL_UNSIGNED_INT para modelos >32k vértices
            java.nio.IntBuffer indexBuffer = indexBuffers.get(group);
            if (indexBuffer != null) {
                indexBuffer.position(0);
                int indexCount = indexBuffer.capacity();
                GLES30.glDrawElements(
                        GLES30.GL_TRIANGLES,
                        indexCount,
                        GLES30.GL_UNSIGNED_INT,
                        indexBuffer
                );
            }
        }

        // Cleanup
        GLES30.glDisableVertexAttribArray(aPositionHandle);
        if (aTexCoordHandle >= 0) {
            GLES30.glDisableVertexAttribArray(aTexCoordHandle);
        }
    }
}
