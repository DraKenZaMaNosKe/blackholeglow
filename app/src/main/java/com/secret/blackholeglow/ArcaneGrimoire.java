package com.secret.blackholeglow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.image.ImageDownloadManager;
import com.secret.blackholeglow.model.ModelDownloadManager;
import com.secret.blackholeglow.util.ObjLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   📖 ArcaneGrimoire - Libro Mágico Flotante para el Panel de Control     ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  CARACTERÍSTICAS:                                                         ║
 * ║  • Modelo 3D cargado desde grimoire.obj                                  ║
 * ║  • Textura con runas brillantes                                          ║
 * ║  • Animación de flotación suave                                          ║
 * ║  • Efecto glow en las runas                                              ║
 * ║  • Detección de toque para activar wallpaper                             ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class ArcaneGrimoire implements SceneObject {
    private static final String TAG = "ArcaneGrimoire";

    private final Context context;

    // Buffers del modelo
    private FloatBuffer vertexBuffer;
    private FloatBuffer uvBuffer;
    private IntBuffer indexBuffer;
    private int indexCount;
    private boolean modelLoaded = false;

    // Textura
    private int textureId = 0;

    // Shader
    private int shaderProgram = 0;
    private int aPositionHandle;
    private int aTexCoordHandle;
    private int uMVPMatrixHandle;
    private int uTextureHandle;
    private int uTimeHandle;
    private int uGlowHandle;

    // Transformaciones
    private float posX = 0f;
    private float posY = -0.2f;   // Ligeramente abajo del centro
    private float posZ = -3f;     // Profundidad
    private float scale = 0.56f;  // 30% m�s peque�o
    private float rotationY = 0f;

    // Animación
    private float time = 0f;
    private float floatOffset = 0f;
    private float glowIntensity = 1.0f;

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    private final float[] tempMatrix = new float[16];

    // Screen size para aspect ratio
    private float aspectRatio = 1.0f;

    // Visibilidad
    private boolean visible = true;

    // Touch detection
    private float hitRadius = 0.18f;  // Radio reducido para touch m�s preciso

    // ═══════════════════════════════════════════════════════════════════════
    // SHADERS - Estilo mágico con glow
    // ═══════════════════════════════════════════════════════════════════════

    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "precision highp float;\n" +
        "in vec3 aPosition;\n" +
        "in vec2 aTexCoord;\n" +
        "uniform mat4 uMVPMatrix;\n" +
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
        "uniform float uGlow;\n" +
        "in vec2 vTexCoord;\n" +
        "in vec3 vPosition;\n" +
        "out vec4 fragColor;\n" +
        "\n" +
        "void main() {\n" +
        "    vec4 texColor = texture(uTexture, vTexCoord);\n" +
        "    \n" +
        "    // Detectar partes brillantes (runas doradas)\n" +
        "    float brightness = dot(texColor.rgb, vec3(0.299, 0.587, 0.114));\n" +
        "    float runeGlow = smoothstep(0.5, 0.8, brightness);\n" +
        "    \n" +
        "    // Pulso de brillo en las runas\n" +
        "    float pulse = sin(uTime * 2.0) * 0.3 + 0.7;\n" +
        "    vec3 glowColor = vec3(1.0, 0.85, 0.4) * runeGlow * pulse * uGlow;\n" +
        "    \n" +
        "    // Color final con glow aditivo\n" +
        "    vec3 finalColor = texColor.rgb + glowColor * 0.5;\n" +
        "    \n" +
        "    // Rim light sutil\n" +
        "    float rim = pow(1.0 - abs(dot(normalize(vPosition), vec3(0.0, 0.0, 1.0))), 2.0);\n" +
        "    finalColor += vec3(0.3, 0.2, 0.5) * rim * 0.3;\n" +
        "    \n" +
        "    fragColor = vec4(finalColor, texColor.a);\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════

    public ArcaneGrimoire(Context context) {
        this.context = context;
    }

    public void initialize() {
        loadModel();
        loadTexture();
        compileShader();
        setupMatrices();
        Log.d(TAG, "📖 ArcaneGrimoire inicializado");
    }

    private void loadModel() {
        try {
            Log.d(TAG, "📦 Cargando grimoire.obj...");

            // Cargar desde archivos descargados (SplashActivity garantiza disponibilidad)
            ModelDownloadManager modelMgr = ModelDownloadManager.getInstance(context);
            String modelPath = modelMgr.getModelPath("grimoire.obj");

            if (modelPath == null) {
                Log.e(TAG, "❌ Modelo no disponible: grimoire.obj");
                return;
            }

            Log.d(TAG, "🌐 Cargando modelo desde descarga: " + modelPath);
            ObjLoader.Mesh mesh = ObjLoader.loadObjFromFile(modelPath, true);

            Log.d(TAG, "✓ Modelo cargado: " + mesh.vertexCount + " vértices, " +
                       mesh.triangleCount + " triángulos");

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

            modelLoaded = true;
            Log.d(TAG, "✅ Modelo listo: " + indexCount + " índices");

        } catch (IOException e) {
            Log.e(TAG, "❌ Error cargando modelo: " + e.getMessage());
            modelLoaded = false;
        }
    }

    private void loadTexture() {
        try {
            // Cargar desde archivos descargados (SplashActivity garantiza disponibilidad)
            ImageDownloadManager imageMgr = ImageDownloadManager.getInstance(context);
            String texturePath = imageMgr.getImagePath("grimoire_texture.png");

            if (texturePath == null) {
                Log.e(TAG, "❌ Textura no disponible: grimoire_texture.png");
                return;
            }

            Log.d(TAG, "🌐 Cargando textura desde descarga: " + texturePath);
            Bitmap bitmap = BitmapFactory.decodeFile(texturePath);

            if (bitmap == null) {
                Log.e(TAG, "❌ Error: bitmap es null");
                return;
            }

            int[] textures = new int[1];
            GLES30.glGenTextures(1, textures, 0);
            textureId = textures[0];

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
            GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);

            bitmap.recycle();
            Log.d(TAG, "✅ Textura cargada: " + textureId);

        } catch (Exception e) {
            Log.e(TAG, "❌ Error cargando textura: " + e.getMessage());
        }
    }

    private void compileShader() {
        int vs = compileShaderCode(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compileShaderCode(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        if (vs == 0 || fs == 0) {
            Log.e(TAG, "❌ Error compilando shaders");
            return;
        }

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vs);
        GLES30.glAttachShader(shaderProgram, fs);
        GLES30.glLinkProgram(shaderProgram);

        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(shaderProgram, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Error linking: " + GLES30.glGetProgramInfoLog(shaderProgram));
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
            return;
        }

        aPositionHandle = GLES30.glGetAttribLocation(shaderProgram, "aPosition");
        aTexCoordHandle = GLES30.glGetAttribLocation(shaderProgram, "aTexCoord");
        uMVPMatrixHandle = GLES30.glGetUniformLocation(shaderProgram, "uMVPMatrix");
        uTextureHandle = GLES30.glGetUniformLocation(shaderProgram, "uTexture");
        uTimeHandle = GLES30.glGetUniformLocation(shaderProgram, "uTime");
        uGlowHandle = GLES30.glGetUniformLocation(shaderProgram, "uGlow");

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

    private void setupMatrices() {
        // View matrix - cámara mirando hacia el origen
        Matrix.setLookAtM(viewMatrix, 0,
            0f, 0f, 3f,    // Posición de cámara
            0f, 0f, 0f,    // Punto de mira
            0f, 1f, 0f);   // Up vector
    }

    public void setScreenSize(int width, int height) {
        aspectRatio = (float) width / height;

        // Projection matrix - perspectiva
        Matrix.perspectiveM(projectionMatrix, 0, 45f, aspectRatio, 0.1f, 100f);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UPDATE - Animación de flotación
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void update(float deltaTime) {
        time += deltaTime;

        // Flotación suave (senoidal)
        floatOffset = (float) Math.sin(time * 1.5) * 0.08f;

        // Rotación lenta
        rotationY += deltaTime * 15f;  // 15 grados por segundo
        if (rotationY > 360f) rotationY -= 360f;

        // Pulso de glow
        glowIntensity = 0.8f + (float) Math.sin(time * 2.5) * 0.2f;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DRAW
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void draw() {
        if (!visible || !modelLoaded || shaderProgram == 0 || vertexBuffer == null) return;

        GLES30.glUseProgram(shaderProgram);

        // Construir matriz de modelo
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, posX, posY + floatOffset, posZ);
        Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f);  // Rotación Y
        Matrix.rotateM(modelMatrix, 0, -20f, 1f, 0f, 0f);       // Inclinar hacia el usuario
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

        // MVP = Projection * View * Model
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);

        // Uniforms
        GLES30.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLES30.glUniform1f(uTimeHandle, time);
        GLES30.glUniform1f(uGlowHandle, glowIntensity);

        // Textura
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(uTextureHandle, 0);

        // Vértices
        vertexBuffer.position(0);
        GLES30.glEnableVertexAttribArray(aPositionHandle);
        GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        // UVs
        uvBuffer.position(0);
        GLES30.glEnableVertexAttribArray(aTexCoordHandle);
        GLES30.glVertexAttribPointer(aTexCoordHandle, 2, GLES30.GL_FLOAT, false, 0, uvBuffer);

        // Configurar OpenGL para renderizar objeto 3D rotando
        // DEPTH TEST habilitado para ordenar caras correctamente
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glDepthFunc(GLES30.GL_LESS);

        // Deshabilitar face culling para ver ambos lados del libro
        GLES30.glDisable(GLES30.GL_CULL_FACE);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        indexBuffer.position(0);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, indexBuffer);

        GLES30.glDisableVertexAttribArray(aPositionHandle);
        GLES30.glDisableVertexAttribArray(aTexCoordHandle);
        // Restaurar face culling
        GLES30.glEnable(GLES30.GL_CULL_FACE);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TOUCH DETECTION
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Verifica si un toque en coordenadas normalizadas (-1 a 1) está sobre el libro
     */
    public boolean isInside(float nx, float ny) {
        // Simplificado: área circular alrededor del centro del libro
        float dx = nx - posX;
        float dy = ny - posY;
        return (dx * dx + dy * dy) <= hitRadius * hitRadius;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SETTERS
    // ═══════════════════════════════════════════════════════════════════════

    public void setPosition(float x, float y) {
        this.posX = x;
        this.posY = y;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setAspectRatio(float aspect) {
        this.aspectRatio = aspect;
        Matrix.perspectiveM(projectionMatrix, 0, 45f, aspectRatio, 0.1f, 100f);
    }

    public void release() {
        if (textureId != 0) {
            int[] textures = {textureId};
            GLES30.glDeleteTextures(1, textures, 0);
            textureId = 0;
        }
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        Log.d(TAG, "🧹 ArcaneGrimoire recursos liberados");
    }
}
