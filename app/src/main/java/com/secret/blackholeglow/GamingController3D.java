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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   🎮 GamingController3D - Control Flotante para el Panel de Control      ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  CARACTERÍSTICAS:                                                         ║
 * ║  • Modelo 3D cargado desde controlxbox_texture.obj                       ║
 * ║  • Textura cyberpunk con colores neón (cyan/magenta)                     ║
 * ║  • Animación de flotación suave                                          ║
 * ║  • Efecto glow neón pulsante                                             ║
 * ║  • Detección de toque para activar wallpaper                             ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  v5.0.7 - Reemplaza ArcaneGrimoire (libro mágico)                        ║
 * ║  Intuitivo: Los usuarios ven un control y saben que deben tocarlo        ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class GamingController3D implements SceneObject {
    private static final String TAG = "GamingController3D";

    // Archivos del modelo
    private static final String MODEL_FILE = "controlxbox_texture.obj";
    private static final String TEXTURE_FILE = "controlxbox_texture.png";

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
    private float posY = -0.1f;    // Ligeramente abajo del centro
    private float posZ = -3.5f;    // Profundidad
    private float scale = 0.4f;    // Escala del control (reducido)
    private float rotationY = 0f;
    private float rotationX = 0f;

    // Animación
    private static final float TWO_PI = (float)(2.0 * Math.PI);
    private static final float TIME_CYCLE = TWO_PI * 10f;  // Ciclo cada ~62 segundos
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

    // Touch detection - área más grande para el control
    private float hitRadius = 0.25f;

    // ═══════════════════════════════════════════════════════════════════════
    // SHADERS - Estilo Cyberpunk Neón
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
        "    // Time limitado con fract para evitar overflow\n" +
        "    float t = fract(uTime * 0.1) * 10.0;\n" +
        "    \n" +
        "    // Detectar cyan (RGB alto en G y B) con smoothstep\n" +
        "    float cyanRaw = min(texColor.g, texColor.b) - texColor.r * 0.5;\n" +
        "    float cyanAmount = smoothstep(0.0, 0.3, cyanRaw);\n" +
        "    \n" +
        "    // Detectar magenta/púrpura (RGB alto en R y B) con smoothstep\n" +
        "    float magentaRaw = min(texColor.r, texColor.b) - texColor.g * 0.3;\n" +
        "    float magentaAmount = smoothstep(0.0, 0.25, magentaRaw);\n" +
        "    \n" +
        "    // Pulsos suaves con smoothstep aplicado al seno\n" +
        "    float sinWave1 = sin(t * 2.5) * 0.5 + 0.5;\n" +
        "    float sinWave2 = sin(t * 2.0 + 1.57) * 0.5 + 0.5;\n" +
        "    float pulse = smoothstep(0.2, 0.8, sinWave1) * 0.5 + 0.5;\n" +
        "    float pulse2 = smoothstep(0.2, 0.8, sinWave2) * 0.4 + 0.6;\n" +
        "    \n" +
        "    // Glow neón cyan y magenta con intensidad controlada\n" +
        "    vec3 cyanGlow = vec3(0.0, 0.9, 1.0) * cyanAmount * pulse * uGlow;\n" +
        "    vec3 magentaGlow = vec3(1.0, 0.2, 0.8) * magentaAmount * pulse2 * uGlow;\n" +
        "    \n" +
        "    // Color final con glow aditivo suave\n" +
        "    vec3 finalColor = texColor.rgb + (cyanGlow + magentaGlow) * 0.5;\n" +
        "    \n" +
        "    // Rim light suave estilo cyberpunk\n" +
        "    vec3 normPos = normalize(vPosition);\n" +
        "    float rimDot = 1.0 - abs(dot(normPos, vec3(0.0, 0.0, 1.0)));\n" +
        "    float rim = smoothstep(0.3, 1.0, rimDot);\n" +
        "    finalColor += vec3(0.3, 0.1, 0.5) * rim * 0.3;\n" +
        "    \n" +
        "    // Boost de saturación suave\n" +
        "    float gray = dot(finalColor, vec3(0.299, 0.587, 0.114));\n" +
        "    finalColor = mix(vec3(gray), finalColor, 1.2);\n" +
        "    \n" +
        "    fragColor = vec4(finalColor, texColor.a);\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════

    public GamingController3D(Context context) {
        this.context = context;
    }

    public void initialize() {
        loadModel();
        loadTexture();
        compileShader();
        setupMatrices();
        Log.d(TAG, "🎮 GamingController3D inicializado");
    }

    private void loadModel() {
        try {
            Log.d(TAG, "📦 Cargando " + MODEL_FILE + "...");

            ModelDownloadManager modelMgr = ModelDownloadManager.getInstance(context);
            String modelPath = modelMgr.getModelPath(MODEL_FILE);

            if (modelPath == null) {
                Log.e(TAG, "❌ Modelo no disponible: " + MODEL_FILE);
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
            ImageDownloadManager imageMgr = ImageDownloadManager.getInstance(context);
            String texturePath = imageMgr.getImagePath(TEXTURE_FILE);

            if (texturePath == null) {
                Log.e(TAG, "❌ Textura no disponible: " + TEXTURE_FILE);
                return;
            }

            Log.d(TAG, "🌐 Cargando textura desde descarga: " + texturePath);
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.RGB_565;  // 🔧 FIX MEMORY: 50% less GPU for opaque texture
            Bitmap bitmap = BitmapFactory.decodeFile(texturePath, opts);

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
        Matrix.setLookAtM(viewMatrix, 0,
            0f, 0f, 3f,    // Posición de cámara
            0f, 0f, 0f,    // Punto de mira
            0f, 1f, 0f);   // Up vector
    }

    public void setScreenSize(int width, int height) {
        aspectRatio = (float) width / height;
        Matrix.perspectiveM(projectionMatrix, 0, 45f, aspectRatio, 0.1f, 100f);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UPDATE - Animación de flotación
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void update(float deltaTime) {
        // Incrementar time con límite para evitar pérdida de precisión
        time += deltaTime;
        if (time > TIME_CYCLE) {
            time -= TIME_CYCLE;
        }

        // Flotación suave (senoidal)
        floatOffset = (float) Math.sin(time * 1.2) * 0.05f;

        // Rotación suave en Y (giro lento)
        rotationY += deltaTime * 15f;  // 15 grados por segundo
        if (rotationY > 360f) rotationY -= 360f;

        // Inclinación sutil en X (cabeceo)
        rotationX = (float) Math.sin(time * 0.7) * 4f;

        // Pulso de glow suave
        glowIntensity = 0.8f + (float) Math.sin(time * 1.5) * 0.2f;
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
        Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f);    // Rotación Y (giro)
        Matrix.rotateM(modelMatrix, 0, rotationX, 1f, 0f, 0f);    // Inclinación X
        Matrix.rotateM(modelMatrix, 0, -15f, 1f, 0f, 0f);         // Inclinar hacia el usuario
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

        // Configurar OpenGL
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glDepthFunc(GLES30.GL_LESS);
        GLES30.glDisable(GLES30.GL_CULL_FACE);  // Ver ambos lados
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        indexBuffer.position(0);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, indexBuffer);

        GLES30.glDisableVertexAttribArray(aPositionHandle);
        GLES30.glDisableVertexAttribArray(aTexCoordHandle);
        GLES30.glEnable(GLES30.GL_CULL_FACE);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TOUCH DETECTION
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Verifica si un toque en coordenadas normalizadas (-1 a 1) está sobre el control
     */
    public boolean isInside(float nx, float ny) {
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
        Log.d(TAG, "🧹 GamingController3D recursos liberados");
    }
}
