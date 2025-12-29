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
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   🚀 TravelingShip - Nave viajando hacia el sol (LabScene)               ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  CARACTERÍSTICAS:                                                         ║
 * ║  • Posición fija en la parte inferior de la pantalla                     ║
 * ║  • Apunta hacia arriba (hacia el sol/destino)                            ║
 * ║  • Sutil animación de balanceo (bobbing)                                 ║
 * ║  • Efecto de propulsión/engine glow                                      ║
 * ║  • Usa human_interceptor.obj como modelo                                 ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class TravelingShip implements SceneObject, CameraAware {
    private static final String TAG = "TravelingShip";

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
    private int uEngineGlowHandle;

    // Transformación - POSICIÓN FIJA (donde tocó el usuario)
    // 📍 RAW: (530, 1795) en pantalla 1080x2340
    private float x = -2.5f;         // Centro horizontal
    private float y = -4.9f;  // Valor calibrado por touch     // Más abajo
    private float z = -2.5f;      // Profundidad
    private float scale = 0.8f;   // Tamaño

    // Rotación - ajustable para apuntar hacia el sol
    private float rotationX = 0f;
    private float rotationY = 0f;
    private float rotationZ = 4.0f;  // Valor encontrado por touch

    // Tiempo para efectos
    private float time = 0f;

    // Engine glow
    private float engineGlow = 1.0f;

    // Cámara
    private CameraController camera;

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // ═══════════════════════════════════════════════════════════════════════
    // SHADERS
    // ═══════════════════════════════════════════════════════════════════════

    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "precision highp float;\n" +
        "in vec3 aPosition;\n" +
        "in vec2 aTexCoord;\n" +
        "uniform mat4 uMVPMatrix;\n" +
        "uniform float uTime;\n" +
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
        "uniform float uEngineGlow;\n" +
        "in vec2 vTexCoord;\n" +
        "in vec3 vPosition;\n" +
        "out vec4 fragColor;\n" +
        "void main() {\n" +
        "    vec4 texColor = texture(uTexture, vTexCoord);\n" +
        "    \n" +
        "    // ENGINE GLOW ÉPICO - estilo GRUBL\n" +
        "    float engineArea = smoothstep(0.0, -1.0, vPosition.y);\n" +
        "    float coreGlow = smoothstep(-0.2, -0.6, vPosition.y);\n" +
        "    \n" +
        "    // Pulso dinámico\n" +
        "    float pulse1 = 0.5 + 0.5 * sin(uTime * 10.0);\n" +
        "    float pulse2 = 0.5 + 0.5 * sin(uTime * 7.0 + 1.5);\n" +
        "    \n" +
        "    // Colores: núcleo blanco-cyan, exterior naranja-dorado\n" +
        "    vec3 coreColor = vec3(0.8, 0.95, 1.0);  // Blanco-cyan brillante\n" +
        "    vec3 outerColor = vec3(1.0, 0.6, 0.2);  // Naranja dorado\n" +
        "    vec3 glowColor = mix(outerColor, coreColor, coreGlow);\n" +
        "    \n" +
        "    // Intensidad final\n" +
        "    float intensity = engineArea * uEngineGlow * (0.7 + 0.3 * pulse1 + 0.2 * pulse2);\n" +
        "    \n" +
        "    // Combinar con additive blending\n" +
        "    fragColor = vec4(texColor.rgb + glowColor * intensity, texColor.a);\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════

    public TravelingShip(Context context, TextureLoader textureLoader) {
        this.context = context;
        this.textureLoader = textureLoader;

        loadModel();
        loadTexture();
        compileShader();

        Log.d(TAG, "🚀 TravelingShip creada - viajando hacia el sol");
    }

    private void loadModel() {
        try {
            Log.d(TAG, "📦 Cargando human_interceptor.obj...");

            // flipV=true para corregir orientación de texturas
            ObjLoader.Mesh mesh = ObjLoader.loadObj(context, "human_interceptor.obj", true);

            Log.d(TAG, "✓ Modelo cargado: " + mesh.vertexCount + " vértices");

            this.vertexBuffer = mesh.vertexBuffer;
            this.uvBuffer = mesh.uvBuffer;

            // Construir índices desde faces
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
        textureId = textureLoader.getTexture(R.drawable.human_interceptor_texture);
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
        uTextureHandle = GLES30.glGetUniformLocation(shaderProgram, "uTexture");
        uTimeHandle = GLES30.glGetUniformLocation(shaderProgram, "uTime");
        uEngineGlowHandle = GLES30.glGetUniformLocation(shaderProgram, "uEngineGlow");

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
    // UPDATE - Nave FIJA para posicionamiento
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void update(float deltaTime) {
        time += deltaTime;

        // 🔄 ROTACIÓN EN EJE X
        // Rotación controlada por touch



        // Engine glow pulsante (único efecto activo)
        engineGlow = 1.0f + 0.2f * (float) Math.sin(time * 5.0);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DRAW
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void draw() {
        if (shaderProgram == 0 || camera == null || vertexBuffer == null) return;

        GLES30.glUseProgram(shaderProgram);

        // Construir matriz de modelo
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);
        Matrix.rotateM(modelMatrix, 0, rotationX, 1f, 0f, 0f);  // Pitch hacia arriba
        Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f);  // Yaw
        Matrix.rotateM(modelMatrix, 0, rotationZ, 0f, 0f, 1f);  // Roll (balanceo)
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

        // Usar computeMvp del CameraController
        camera.computeMvp(modelMatrix, mvpMatrix);

        GLES30.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLES30.glUniform1f(uTimeHandle, time);
        GLES30.glUniform1f(uEngineGlowHandle, engineGlow);

        // Textura
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(uTextureHandle, 0);

        // Vértices
        GLES30.glEnableVertexAttribArray(aPositionHandle);
        GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        // UVs
        GLES30.glEnableVertexAttribArray(aTexCoordHandle);
        GLES30.glVertexAttribPointer(aTexCoordHandle, 2, GLES30.GL_FLOAT, false, 0, uvBuffer);

        // Dibujar
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, indexBuffer);

        GLES30.glDisableVertexAttribArray(aPositionHandle);
        GLES30.glDisableVertexAttribArray(aTexCoordHandle);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setRotation(float pitch, float yaw, float roll) {
        this.rotationX = pitch;
        this.rotationY = yaw;
        this.rotationZ = roll;
    }


    // 🎮 CONTROL POR TOUCH
    public void onTouchMove(float normalizedX) {
        // Y ya fijo en -4.9f  // Control Y por touch
    }

    public void onTouchRelease() {
        Log.d(TAG, "📍 VALOR FINAL y = " + y);
    }

    public void release() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        Log.d(TAG, "🗑️ TravelingShip liberada");
    }
}
