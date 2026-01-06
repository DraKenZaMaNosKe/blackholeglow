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
 * ║   🚗 DeLorean3D - DeLorean viajando por la carretera synthwave           ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  CARACTERÍSTICAS:                                                         ║
 * ║  • Posición fija en la parte inferior de la pantalla                     ║
 * ║  • Vista desde atrás (vemos las luces traseras)                          ║
 * ║  • Sutil animación de balanceo (como si fuera en la carretera)           ║
 * ║  • Efecto de underglow neón (cyan/magenta)                               ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class DeLorean3D implements SceneObject, CameraAware {
    private static final String TAG = "DeLorean3D";

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
    private int uModelMatrixHandle;
    private int uTextureHandle;
    private int uTimeHandle;

    // Transformación - POSICIÓN FIJA (calibrada por usuario 2026-01-06)
    private float x = 0f;       // Centrado en la carretera
    private float y = -4.0f;    // Sobre la carretera (abajo)
    private float z = -4.0f;    // Profundidad
    private float scale = 1.2f;

    // Rotación - Vemos el carro desde atrás (luces traseras)
    private float rotationX = 15f;   // Inclinado hacia adelante
    private float rotationY = -90f;  // Rotado para ver la parte trasera
    private float rotationZ = 0f;

    // 🔧 MODO AJUSTE - Touch para posicionar
    private boolean adjustMode = false;  // DESACTIVADO - posición calibrada

    // Tiempo para animaciones
    private float time = 0f;

    // Balanceo sutil
    private float bobOffset = 0f;
    private static final float BOB_SPEED = 2.0f;
    private static final float BOB_AMOUNT = 0.02f;

    // Cámara
    private CameraController camera;

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // ═══════════════════════════════════════════════════════════════════════
    // SHADERS - Simple con underglow neón
    // ═══════════════════════════════════════════════════════════════════════

    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "precision highp float;\n" +
        "in vec3 aPosition;\n" +
        "in vec2 aTexCoord;\n" +
        "uniform mat4 uMVPMatrix;\n" +
        "uniform mat4 uModelMatrix;\n" +
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
        "in vec2 vTexCoord;\n" +
        "in vec3 vPosition;\n" +
        "out vec4 fragColor;\n" +
        "\n" +
        "void main() {\n" +
        "    vec4 texColor = texture(uTexture, vTexCoord);\n" +
        "    \n" +
        "    // Underglow neón en la parte inferior del carro\n" +
        "    float underglowArea = smoothstep(-0.3, -0.1, vPosition.y) * (1.0 - smoothstep(-0.1, 0.1, vPosition.y));\n" +
        "    \n" +
        "    // Color del underglow: pulsa entre cyan y magenta\n" +
        "    vec3 glowColor1 = vec3(0.0, 1.0, 1.0);  // Cyan\n" +
        "    vec3 glowColor2 = vec3(1.0, 0.0, 1.0);  // Magenta\n" +
        "    float pulse = sin(uTime * 3.0) * 0.5 + 0.5;\n" +
        "    vec3 glowColor = mix(glowColor1, glowColor2, pulse) * 0.5;\n" +
        "    \n" +
        "    // Combinar textura con underglow\n" +
        "    vec3 finalColor = texColor.rgb + glowColor * underglowArea;\n" +
        "    \n" +
        "    // Luces traseras rojas (zona posterior del carro)\n" +
        "    float tailLightArea = smoothstep(0.3, 0.5, vPosition.z) * smoothstep(-0.2, 0.0, vPosition.y);\n" +
        "    vec3 tailLightColor = vec3(1.0, 0.1, 0.1) * (sin(uTime * 2.0) * 0.2 + 0.8);\n" +
        "    finalColor += tailLightColor * tailLightArea * 0.3;\n" +
        "    \n" +
        "    fragColor = vec4(finalColor, texColor.a);\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════

    public DeLorean3D(Context context, TextureLoader textureLoader) {
        this.context = context;
        this.textureLoader = textureLoader;

        loadModel();
        loadTexture();
        compileShader();

        Log.d(TAG, "🚗 DeLorean3D creado - rumbo al horizonte synthwave");
    }

    private void loadModel() {
        try {
            Log.d(TAG, "📦 Cargando delorean.obj...");

            ObjLoader.Mesh mesh = ObjLoader.loadObj(context, "delorean.obj", true);

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
        textureId = textureLoader.getTexture(R.drawable.delorean_texture);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);

        Log.d(TAG, "✅ Textura DeLorean cargada: " + textureId);
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
        uModelMatrixHandle = GLES30.glGetUniformLocation(shaderProgram, "uModelMatrix");
        uTextureHandle = GLES30.glGetUniformLocation(shaderProgram, "uTexture");
        uTimeHandle = GLES30.glGetUniformLocation(shaderProgram, "uTime");

        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);

        Log.d(TAG, "✅ Shader DeLorean compilado");
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
    // UPDATE & DRAW
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void update(float deltaTime) {
        time += deltaTime;

        // Balanceo sutil (como si fuera en la carretera)
        bobOffset = (float) Math.sin(time * BOB_SPEED) * BOB_AMOUNT;
    }

    @Override
    public void draw() {
        if (vertexBuffer == null || indexBuffer == null) return;

        GLES30.glUseProgram(shaderProgram);

        // Model matrix con posición, rotación y escala
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y + bobOffset, z);
        Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f);
        Matrix.rotateM(modelMatrix, 0, rotationX, 1f, 0f, 0f);
        Matrix.rotateM(modelMatrix, 0, rotationZ, 0f, 0f, 1f);
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

        // MVP matrix
        if (camera != null) {
            float[] vpMatrix = camera.getViewProjectionMatrix();
            Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);
        } else {
            System.arraycopy(modelMatrix, 0, mvpMatrix, 0, 16);
        }

        // Uniforms
        GLES30.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLES30.glUniformMatrix4fv(uModelMatrixHandle, 1, false, modelMatrix, 0);
        GLES30.glUniform1f(uTimeHandle, time);

        // Textura
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(uTextureHandle, 0);

        // Vertices
        vertexBuffer.position(0);
        GLES30.glEnableVertexAttribArray(aPositionHandle);
        GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        // UVs
        uvBuffer.position(0);
        GLES30.glEnableVertexAttribArray(aTexCoordHandle);
        GLES30.glVertexAttribPointer(aTexCoordHandle, 2, GLES30.GL_FLOAT, false, 0, uvBuffer);

        // Draw
        indexBuffer.position(0);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, indexBuffer);

        GLES30.glDisableVertexAttribArray(aPositionHandle);
        GLES30.glDisableVertexAttribArray(aTexCoordHandle);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SETTERS
    // ═══════════════════════════════════════════════════════════════════════

    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setRotation(float rx, float ry, float rz) {
        this.rotationX = rx;
        this.rotationY = ry;
        this.rotationZ = rz;
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    /**
     * 🔧 Maneja touch para posicionar el carro
     * @param normalizedX -1 (izquierda) a 1 (derecha)
     * @param normalizedY -1 (abajo) a 1 (arriba)
     * @return true si se procesó el touch
     */
    public boolean onTouchEvent(float normalizedX, float normalizedY) {
        if (!adjustMode) return false;

        // Convertir coordenadas normalizadas a posición 3D
        // X: -1 a 1 -> -3 a 3
        // Y: -1 a 1 -> -5 a 0
        this.x = normalizedX * 3.0f;
        this.y = normalizedY * 2.5f - 2.5f;  // Ajuste para que quede en la parte inferior

        Log.d(TAG, "🎯 DeLorean posición: x=" + x + ", y=" + y + ", z=" + z);
        Log.d(TAG, "   Rotación: rX=" + rotationX + ", rY=" + rotationY);

        return true;
    }

    /**
     * Activa/desactiva modo ajuste
     */
    public void setAdjustMode(boolean enabled) {
        this.adjustMode = enabled;
        Log.d(TAG, "🔧 Modo ajuste: " + (enabled ? "ON" : "OFF"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // RELEASE
    // ═══════════════════════════════════════════════════════════════════════

    public void release() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        Log.d(TAG, "🚗 DeLorean3D liberado");
    }
}
