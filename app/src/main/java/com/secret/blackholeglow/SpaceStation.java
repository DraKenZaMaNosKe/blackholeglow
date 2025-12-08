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
 * ╔════════════════════════════════════════════════════════════════════════╗
 * ║  🛰️ ESTACIÓN ESPACIAL FUTURISTA                                       ║
 * ╠════════════════════════════════════════════════════════════════════════╣
 * ║  Características:                                                      ║
 * ║  • Rotación lenta (gravedad artificial)                               ║
 * ║  • Luces de ventanas parpadeantes                                     ║
 * ║  • Pulso cyan en paneles de energía                                   ║
 * ╚════════════════════════════════════════════════════════════════════════╝
 */
public class SpaceStation implements SceneObject, CameraAware {
    private static final String TAG = "SpaceStation";

    private final Context context;
    private final TextureLoader textureLoader;

    // Buffers del modelo (usando IntBuffer como TierraMeshy)
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

    // Transformación
    private float x, y, z;
    private float scale;
    private float rotationY = 0f;
    private float rotationSpeed = 2f;  // Grados por segundo (lento y realista)

    // Posición fija (sin órbita)
    private boolean useOrbit = false;

    // Cámara
    private CameraController camera;

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // ⚡ Tiempo acumulado
    private float timeAccumulator = 0f;

    /**
     * Constructor
     */
    public SpaceStation(Context context, TextureLoader textureLoader,
                        float x, float y, float z, float scale) {
        this.context = context;
        this.textureLoader = textureLoader;
        this.x = x;
        this.y = y;
        this.z = z;
        this.scale = scale;

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Log.d(TAG, "🛰️ Creando Estación Espacial");
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        loadModel();
        loadTexture();
        createShaders();

        Log.d(TAG, "✅ Estación Espacial creada");
    }

    /**
     * Carga el modelo SpaceStation.obj (mismo patrón que TierraMeshy)
     */
    private void loadModel() {
        try {
            Log.d(TAG, "📦 Cargando SpaceStation.obj...");

            ObjLoader.Mesh mesh = ObjLoader.loadObj(context, "SpaceStation.obj");

            Log.d(TAG, "✓ Modelo cargado:");
            Log.d(TAG, "  Vértices: " + mesh.vertexCount);
            Log.d(TAG, "  Caras: " + mesh.faces.size());

            // Usar buffers directamente del ObjLoader
            this.vertexBuffer = mesh.vertexBuffer;
            this.uvBuffer = mesh.uvBuffer;

            // Construir buffer de índices con fan triangulation (como TierraMeshy)
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
            Log.e(TAG, "❌ Error cargando SpaceStation.obj", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Carga la textura
     */
    private void loadTexture() {
        Log.d(TAG, "🎨 Cargando textura spacestation_texture...");

        int textureResId = context.getResources().getIdentifier(
                "spacestation_texture", "drawable", context.getPackageName());

        if (textureResId != 0) {
            textureId = textureLoader.getTexture(textureResId);
            Log.d(TAG, "✓ Textura cargada (ID=" + textureId + ")");
        } else {
            Log.e(TAG, "❌ Textura no encontrada: spacestation_texture");
            textureId = 0;
        }
    }

    /**
     * 🛰️✨ SHADER CON EFECTOS DE ESTACIÓN ESPACIAL
     */
    private void createShaders() {
        Log.d(TAG, "🎨 Creando shaders...");

        // Vertex Shader simple
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

        // Fragment Shader con ventanitas y pulso cyan
        String fragmentShaderCode =
                "precision mediump float;\n" +
                "\n" +
                "uniform sampler2D u_Texture;\n" +
                "uniform float u_Time;\n" +
                "\n" +
                "varying vec2 v_TexCoord;\n" +
                "varying vec3 v_LocalPos;\n" +
                "\n" +
                "void main() {\n" +
                "    // Invertir V para corregir orientación de textura\n" +
                "    vec2 uv = vec2(v_TexCoord.x, 1.0 - v_TexCoord.y);\n" +
                "    vec4 texColor = texture2D(u_Texture, uv);\n" +
                "    vec3 color = texColor.rgb;\n" +
                "    float brightness = dot(color, vec3(0.299, 0.587, 0.114));\n" +
                "    \n" +
                "    // ═══ 1. 🔵 PULSO CYAN en paneles azules ═══\n" +
                "    float blueAmount = color.b - max(color.r, color.g);\n" +
                "    if (blueAmount > 0.05) {\n" +
                "        float pulse = sin(u_Time * 2.0) * 0.2 + 0.8;\n" +
                "        color.b *= pulse;\n" +
                "        color.g *= (pulse * 0.3 + 0.7);\n" +
                "        // Añadir brillo cyan sutil\n" +
                "        color += vec3(0.0, 0.1, 0.15) * pulse * blueAmount;\n" +
                "    }\n" +
                "    \n" +
                "    // ═══ 2. 💡 VENTANITAS amarillas parpadeantes ═══\n" +
                "    if (brightness > 0.6) {\n" +
                "        // ID único para cada ventana basado en posición\n" +
                "        float windowId = floor(v_LocalPos.x * 10.0) + floor(v_LocalPos.z * 10.0);\n" +
                "        float flicker = sin(u_Time * 1.5 + windowId * 2.0) * 0.15 + 0.85;\n" +
                "        // Luz cálida amarilla\n" +
                "        color += vec3(0.12, 0.1, 0.02) * flicker;\n" +
                "    }\n" +
                "    \n" +
                "    gl_FragColor = vec4(color, texColor.a);\n" +
                "}";

        int vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode);

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vertexShader);
        GLES30.glAttachShader(shaderProgram, fragmentShader);
        GLES30.glLinkProgram(shaderProgram);

        // Obtener handles
        aPositionHandle = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        aTexCoordHandle = GLES30.glGetAttribLocation(shaderProgram, "a_TexCoord");
        uMVPMatrixHandle = GLES30.glGetUniformLocation(shaderProgram, "u_MVPMatrix");
        uTextureHandle = GLES30.glGetUniformLocation(shaderProgram, "u_Texture");
        uTimeHandle = GLES30.glGetUniformLocation(shaderProgram, "u_Time");

        Log.d(TAG, "✓ Shaders creados");
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
    // 📍 GETTERS PARA SISTEMA DE COLISIONES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Obtiene la posición X de la estación espacial
     */
    public float getX() { return x; }

    /**
     * Obtiene la posición Y de la estación espacial
     */
    public float getY() { return y; }

    /**
     * Obtiene la posición Z de la estación espacial
     */
    public float getZ() { return z; }

    /**
     * Obtiene la escala de la estación espacial
     */
    public float getScale() { return scale; }

    /**
     * 📏 Radio de colisión para esquivar/chocar
     * La estación tiene forma de anillo, usamos un radio aproximado
     */
    public float getCollisionRadius() {
        return scale * 1.2f;  // Aproximación del tamaño del modelo
    }

    /**
     * Establece posición fija (sin órbita)
     */
    public void setFixedPosition(float x, float y, float z) {
        this.useOrbit = false;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Configura órbita (opcional)
     */
    public void setOrbitParams(float centerX, float centerY, float centerZ,
                               float radius, float height, float speed) {
        this.useOrbit = true;
        // Parámetros de órbita si se necesitan en el futuro
    }

    @Override
    public void update(float deltaTime) {
        timeAccumulator += deltaTime;

        // Rotación lenta sobre su eje
        rotationY += rotationSpeed * deltaTime;
        if (rotationY > 360f) rotationY -= 360f;
    }

    @Override
    public void draw() {
        if (camera == null || shaderProgram == 0) return;

        GLES30.glUseProgram(shaderProgram);

        // ⚠️ CRÍTICO: Desactivar face culling (modelos Meshy pueden tener normales invertidas)
        GLES30.glDisable(GLES30.GL_CULL_FACE);

        // Habilitar blending y depth test
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);

        // Construir matriz modelo
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);
        Matrix.rotateM(modelMatrix, 0, rotationY, 0, 1, 0);
        Matrix.rotateM(modelMatrix, 0, 15f, 1, 0, 0);  // Inclinación
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

        // Calcular MVP
        camera.computeMvp(modelMatrix, mvpMatrix);

        // Pasar uniforms
        GLES30.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLES30.glUniform1f(uTimeHandle, timeAccumulator % 60.0f);

        // Configurar atributos de vértices
        vertexBuffer.position(0);
        GLES30.glEnableVertexAttribArray(aPositionHandle);
        GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        // Configurar UVs
        if (uvBuffer != null && aTexCoordHandle >= 0) {
            uvBuffer.position(0);
            GLES30.glEnableVertexAttribArray(aTexCoordHandle);
            GLES30.glVertexAttribPointer(aTexCoordHandle, 2, GLES30.GL_FLOAT, false, 0, uvBuffer);
        }

        // Bind textura
        if (textureId > 0 && uTextureHandle >= 0) {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
            GLES30.glUniform1i(uTextureHandle, 0);
        }

        // ⚠️ CRÍTICO: Usar GL_UNSIGNED_INT (no GL_UNSIGNED_SHORT)
        indexBuffer.position(0);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, indexBuffer);

        // Limpiar
        GLES30.glDisableVertexAttribArray(aPositionHandle);
        if (aTexCoordHandle >= 0) {
            GLES30.glDisableVertexAttribArray(aTexCoordHandle);
        }

        // Restaurar culling para otros objetos
        GLES30.glEnable(GLES30.GL_CULL_FACE);
    }
}
