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

    // Movimiento AI
    public float velocityX = 0f;
    public float velocityY = 0f;
    public float velocityZ = 0f;
    private float moveSpeed = 0.5f;  // Velocidad de movimiento
    private float directionChangeTimer = 0f;
    private float directionChangeInterval = 3.0f;  // Cambiar dirección cada 3 segundos

    // Límites de la escena (basados en el frustum de la cámara)
    private float minX = -2.0f;
    private float maxX = 2.0f;
    private float minY = -1.5f;
    private float maxY = 1.5f;
    private float minZ = -3.0f;
    private float maxZ = 0.5f;

    // Cámara
    private CameraController camera;

    // Matrices
    private float[] modelMatrix = new float[16];
    private float[] mvpMatrix = new float[16];

    // ✅ CRÍTICO: Tiempo relativo para evitar overflow
    private final long startTime = System.currentTimeMillis();

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
        // IA: Cambiar dirección periódicamente
        directionChangeTimer += deltaTime;
        if (directionChangeTimer >= directionChangeInterval) {
            changeDirection();
            directionChangeTimer = 0f;
        }

        // Aplicar movimiento
        x += velocityX * deltaTime;
        y += velocityY * deltaTime;
        z += velocityZ * deltaTime;

        // Detectar límites y rebotar suavemente
        if (x < minX) {
            x = minX;
            velocityX = Math.abs(velocityX);  // Invertir hacia la derecha
            changeDirection();  // Cambiar dirección al tocar límite
        } else if (x > maxX) {
            x = maxX;
            velocityX = -Math.abs(velocityX);  // Invertir hacia la izquierda
            changeDirection();
        }

        if (y < minY) {
            y = minY;
            velocityY = Math.abs(velocityY);  // Invertir hacia arriba
            changeDirection();
        } else if (y > maxY) {
            y = maxY;
            velocityY = -Math.abs(velocityY);  // Invertir hacia abajo
            changeDirection();
        }

        if (z < minZ) {
            z = minZ;
            velocityZ = Math.abs(velocityZ);  // Invertir hacia adelante
            changeDirection();
        } else if (z > maxZ) {
            z = maxZ;
            velocityZ = -Math.abs(velocityZ);  // Invertir hacia atrás
            changeDirection();
        }

        // Rotar lentamente mientras se mueve
        rotationY += 20f * deltaTime;
    }

    /**
     * Cambia la dirección de movimiento aleatoriamente
     */
    private void changeDirection() {
        // Generar dirección aleatoria
        velocityX = (float) (Math.random() * 2.0 - 1.0) * moveSpeed;
        velocityY = (float) (Math.random() * 2.0 - 1.0) * moveSpeed;
        velocityZ = (float) (Math.random() * 2.0 - 1.0) * moveSpeed;

        // Normalizar el vector de velocidad para movimiento uniforme
        float magnitude = (float) Math.sqrt(
            velocityX * velocityX +
            velocityY * velocityY +
            velocityZ * velocityZ
        );

        if (magnitude > 0.001f) {
            velocityX = (velocityX / magnitude) * moveSpeed;
            velocityY = (velocityY / magnitude) * moveSpeed;
            velocityZ = (velocityZ / magnitude) * moveSpeed;
        }

        Log.d(TAG, "🎯 Nueva dirección: (" +
            String.format("%.2f", velocityX) + ", " +
            String.format("%.2f", velocityY) + ", " +
            String.format("%.2f", velocityZ) + ")");
    }

    @Override
    public void draw() {
        if (camera == null) return;

        // Deshabilitar face culling (para ver todas las caras)
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Usar shader
        GLES20.glUseProgram(shaderProgram);

        // Construir matriz modelo
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);
        if (rotationY != 0f) {
            Matrix.rotateM(modelMatrix, 0, rotationY, 0, 1, 0);
        }
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

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
