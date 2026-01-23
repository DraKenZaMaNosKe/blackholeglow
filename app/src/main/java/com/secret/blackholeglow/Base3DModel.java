package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES30;
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
 * ║   🎨 Base3DModel - Clase base para todos los modelos 3D                  ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  ELIMINA CÓDIGO DUPLICADO (~600 líneas) de:                              ║
 * ║  • ZombieHead3D, ZombieBody3D, Link3D, DeLorean3D                        ║
 * ║                                                                           ║
 * ║  PROVEE:                                                                  ║
 * ║  • Carga de modelo OBJ (assets o descargado)                             ║
 * ║  • Carga de textura (assets o descargado)                                ║
 * ║  • Compilación de shaders                                                ║
 * ║  • Matrices de proyección propias                                        ║
 * ║  • Gestión de buffers OpenGL                                             ║
 * ║  • Liberación de recursos                                                ║
 * ║                                                                           ║
 * ║  SUBCLASES DEBEN IMPLEMENTAR:                                            ║
 * ║  • getModelFileName() - Nombre del archivo .obj                          ║
 * ║  • getTextureFileName() - Nombre del archivo de textura                  ║
 * ║  • getVertexShader() - Código GLSL del vertex shader                     ║
 * ║  • getFragmentShader() - Código GLSL del fragment shader                 ║
 * ║  • updateAnimation(deltaTime) - Lógica de animación específica           ║
 * ║  • buildModelMatrix(modelMatrix) - Construir matriz de modelo            ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public abstract class Base3DModel implements SceneObject {
    private static final String TAG = "Base3DModel";

    // ═══════════════════════════════════════════════════════════════════════
    // CONTEXTO Y DEPENDENCIAS
    // ═══════════════════════════════════════════════════════════════════════

    protected final Context context;
    protected final TextureLoader textureLoader;

    // ═══════════════════════════════════════════════════════════════════════
    // BUFFERS DEL MODELO
    // ═══════════════════════════════════════════════════════════════════════

    protected FloatBuffer vertexBuffer;
    protected FloatBuffer uvBuffer;
    protected IntBuffer indexBuffer;
    protected int indexCount;
    protected boolean modelLoaded = false;

    // ═══════════════════════════════════════════════════════════════════════
    // TEXTURA
    // ═══════════════════════════════════════════════════════════════════════

    protected int textureId = 0;

    // ═══════════════════════════════════════════════════════════════════════
    // SHADER
    // ═══════════════════════════════════════════════════════════════════════

    protected int shaderProgram = 0;
    protected int aPositionHandle;
    protected int aTexCoordHandle;
    protected int uMVPMatrixHandle;
    protected int uTextureHandle;
    protected int uTimeHandle;

    // ═══════════════════════════════════════════════════════════════════════
    // TRANSFORMACIÓN
    // ═══════════════════════════════════════════════════════════════════════

    protected float positionX = 0f;
    protected float positionY = 0f;
    protected float positionZ = 0f;
    protected float scale = 1f;
    protected float rotationX = 0f;
    protected float rotationY = 0f;
    protected float rotationZ = 0f;

    // ═══════════════════════════════════════════════════════════════════════
    // TIEMPO (protegido contra overflow)
    // ═══════════════════════════════════════════════════════════════════════

    protected float time = 0f;
    protected static final float TIME_CYCLE = 100.0f;

    // ═══════════════════════════════════════════════════════════════════════
    // MATRICES
    // ═══════════════════════════════════════════════════════════════════════

    protected int screenWidth = 1080;
    protected int screenHeight = 1920;
    protected final float[] projectionMatrix = new float[16];
    protected final float[] viewMatrix = new float[16];
    protected final float[] vpMatrix = new float[16];
    protected final float[] modelMatrix = new float[16];
    protected final float[] mvpMatrix = new float[16];

    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════

    public Base3DModel(Context context, TextureLoader textureLoader) {
        this.context = context;
        this.textureLoader = textureLoader;

        setupProjection();
        loadModel();
        loadTexture();
        compileShader();

        Log.d(getTag(), "✅ " + getClass().getSimpleName() + " creado");
    }

    /**
     * Constructor simplificado sin TextureLoader
     */
    public Base3DModel(Context context) {
        this(context, null);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MÉTODOS ABSTRACTOS - SUBCLASES DEBEN IMPLEMENTAR
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Nombre del archivo .obj (ej: "zombie_head.obj")
     */
    protected abstract String getModelFileName();

    /**
     * Nombre del archivo de textura (ej: "zombie_head_texture.png")
     */
    protected abstract String getTextureFileName();

    /**
     * Código GLSL del vertex shader
     */
    protected abstract String getVertexShader();

    /**
     * Código GLSL del fragment shader
     */
    protected abstract String getFragmentShader();

    /**
     * Lógica de animación específica del modelo
     */
    protected abstract void updateAnimation(float deltaTime);

    /**
     * Construye la matriz de modelo con posición, rotación, escala
     * La subclase puede agregar efectos adicionales (balanceo, temblor, etc.)
     */
    protected abstract void buildModelMatrix(float[] matrix);

    /**
     * Tag para logging (por defecto usa el nombre de la clase)
     */
    protected String getTag() {
        return getClass().getSimpleName();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN DE PROYECCIÓN
    // ═══════════════════════════════════════════════════════════════════════

    protected void setupProjection() {
        // View matrix - cámara mirando hacia -Z
        Matrix.setLookAtM(viewMatrix, 0,
            0f, 0f, 3f,    // Posición cámara
            0f, 0f, 0f,    // Mirando a
            0f, 1f, 0f);   // Up vector

        // Projection matrix
        float ratio = (float) screenWidth / screenHeight;
        Matrix.perspectiveM(projectionMatrix, 0, 60f, ratio, 0.1f, 100f);

        // VP matrix
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
    }

    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        setupProjection();
        Log.d(getTag(), "📐 Screen size: " + width + "x" + height);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CARGA DE MODELO
    // ═══════════════════════════════════════════════════════════════════════

    protected void loadModel() {
        String modelFileName = getModelFileName();
        if (modelFileName == null || modelFileName.isEmpty()) {
            Log.w(getTag(), "⚠️ No model file specified");
            return;
        }

        try {
            Log.d(getTag(), "📦 Cargando " + modelFileName + "...");

            ObjLoader.Mesh mesh;
            ModelDownloadManager modelMgr = ModelDownloadManager.getInstance(context);
            String modelPath = modelMgr.getModelPath(modelFileName);

            if (modelPath != null) {
                Log.d(getTag(), "🌐 Cargando desde cache: " + modelPath);
                mesh = ObjLoader.loadObjFromFile(modelPath, true);
            } else {
                Log.d(getTag(), "📂 Cargando desde assets");
                mesh = ObjLoader.loadObj(context, modelFileName, true);
            }

            Log.d(getTag(), "✓ Modelo cargado: " + mesh.vertexCount + " vértices");

            this.vertexBuffer = mesh.vertexBuffer;
            this.uvBuffer = mesh.uvBuffer;

            // Convertir faces a índices triangulares
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
            Log.d(getTag(), "✅ Modelo listo: " + indexCount + " índices");

        } catch (IOException e) {
            Log.e(getTag(), "❌ Error cargando modelo: " + e.getMessage());
            modelLoaded = false;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CARGA DE TEXTURA
    // ═══════════════════════════════════════════════════════════════════════

    protected void loadTexture() {
        String textureFileName = getTextureFileName();
        if (textureFileName == null || textureFileName.isEmpty()) {
            Log.w(getTag(), "⚠️ No texture file specified");
            return;
        }

        ImageDownloadManager imageMgr = ImageDownloadManager.getInstance(context);
        String texturePath = imageMgr.getImagePath(textureFileName);

        if (texturePath != null && textureLoader instanceof TextureManager) {
            Log.d(getTag(), "🌐 Cargando textura desde cache: " + texturePath);
            textureId = ((TextureManager) textureLoader).loadTextureFromFile(texturePath);
        } else {
            Log.e(getTag(), "❌ Textura no disponible: " + textureFileName);
            return;
        }

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);

        Log.d(getTag(), "✅ Textura cargada: " + textureId);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // COMPILACIÓN DE SHADER
    // ═══════════════════════════════════════════════════════════════════════

    protected void compileShader() {
        String vertexShaderCode = getVertexShader();
        String fragmentShaderCode = getFragmentShader();

        if (vertexShaderCode == null || fragmentShaderCode == null) {
            Log.e(getTag(), "❌ Shaders no definidos");
            return;
        }

        int vs = compileShaderCode(GLES30.GL_VERTEX_SHADER, vertexShaderCode);
        int fs = compileShaderCode(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode);

        if (vs == 0 || fs == 0) {
            Log.e(getTag(), "❌ Error compilando shaders");
            return;
        }

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vs);
        GLES30.glAttachShader(shaderProgram, fs);
        GLES30.glLinkProgram(shaderProgram);

        // Obtener ubicaciones de atributos y uniforms
        aPositionHandle = GLES30.glGetAttribLocation(shaderProgram, "aPosition");
        aTexCoordHandle = GLES30.glGetAttribLocation(shaderProgram, "aTexCoord");
        uMVPMatrixHandle = GLES30.glGetUniformLocation(shaderProgram, "uMVPMatrix");
        uTextureHandle = GLES30.glGetUniformLocation(shaderProgram, "uTexture");
        uTimeHandle = GLES30.glGetUniformLocation(shaderProgram, "uTime");

        // Permitir que subclases obtengan uniforms adicionales
        onShaderCompiled();

        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);

        Log.d(getTag(), "✅ Shader compilado");
    }

    /**
     * Hook para que subclases obtengan uniforms adicionales después de compilar
     */
    protected void onShaderCompiled() {
        // Subclases pueden override para obtener uniforms adicionales
    }

    private int compileShaderCode(int type, String code) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, code);
        GLES30.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(getTag(), "Shader error: " + GLES30.glGetShaderInfoLog(shader));
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
        // Mantener time en rango para evitar overflow en floats
        time += deltaTime;
        if (time > TIME_CYCLE) {
            time -= TIME_CYCLE;
        }

        // Llamar animación específica de la subclase
        updateAnimation(deltaTime);
    }

    @Override
    public void draw() {
        if (!modelLoaded || vertexBuffer == null || indexBuffer == null || shaderProgram == 0) {
            return;
        }

        GLES30.glUseProgram(shaderProgram);

        // Construir model matrix (subclase implementa)
        buildModelMatrix(modelMatrix);

        // MVP matrix
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);

        // Uniforms
        GLES30.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);
        if (uTimeHandle >= 0) {
            GLES30.glUniform1f(uTimeHandle, time);
        }

        // Textura
        if (textureId != 0) {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
            GLES30.glUniform1i(uTextureHandle, 0);
        }

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
    // SETTERS COMUNES
    // ═══════════════════════════════════════════════════════════════════════

    public void setPosition(float x, float y, float z) {
        this.positionX = x;
        this.positionY = y;
        this.positionZ = z;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setRotation(float rx, float ry, float rz) {
        this.rotationX = rx;
        this.rotationY = ry;
        this.rotationZ = rz;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPER: Construir matriz de modelo básica
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Construye una matriz de modelo básica con posición, rotación y escala.
     * Las subclases pueden llamar esto y luego agregar efectos adicionales.
     */
    protected void buildBasicModelMatrix(float[] matrix) {
        Matrix.setIdentityM(matrix, 0);
        Matrix.translateM(matrix, 0, positionX, positionY, positionZ);
        Matrix.rotateM(matrix, 0, rotationY, 0f, 1f, 0f);
        Matrix.rotateM(matrix, 0, rotationX, 1f, 0f, 0f);
        Matrix.rotateM(matrix, 0, rotationZ, 0f, 0f, 1f);
        Matrix.scaleM(matrix, 0, scale, scale, scale);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LIBERACIÓN DE RECURSOS
    // ═══════════════════════════════════════════════════════════════════════

    public void dispose() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        if (textureId != 0) {
            int[] textures = {textureId};
            GLES30.glDeleteTextures(1, textures, 0);
            textureId = 0;
        }
        modelLoaded = false;
        Log.d(getTag(), "🗑️ " + getClass().getSimpleName() + " disposed");
    }
}
