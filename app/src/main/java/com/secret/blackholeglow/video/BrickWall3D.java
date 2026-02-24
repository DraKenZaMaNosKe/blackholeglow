package com.secret.blackholeglow.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.core.DeviceProfile;
import com.secret.blackholeglow.util.ObjLoader;
import com.secret.blackholeglow.image.ImageDownloadManager;
import com.secret.blackholeglow.model.ModelDownloadManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║           🧱 BrickWall3D - Barda de Ladrillos (Meshy AI)                 ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Modelo 3D de una barda horizontal de ladrillos donde se sienta          ║
 * ║  el gato negro. Posición fija en la parte inferior de la pantalla.      ║
 * ║  Sin animación (estático).                                              ║
 * ║                                                                          ║
 * ║  ESTADO: Activo - Descarga remota (Supabase) con fallback local          ║
 * ║  Archivos:                                                              ║
 * ║    - brick_wall.obj          (Supabase → ModelDownloadManager)          ║
 * ║    - brick_wall_texture.png  (Supabase → ImageDownloadManager)          ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class BrickWall3D {
    private static final String TAG = "BrickWall3D";
    private static final String OBJ_ASSET_PATH = "models/brick_wall.obj";
    private static final String TEXTURE_ASSET_PATH = "models/brick_wall_texture.png";

    private final Context context;

    // OpenGL buffers
    private FloatBuffer vertexBuffer;
    private FloatBuffer uvBuffer;
    private IntBuffer indexBuffer;
    private int indexCount;

    // Shader
    private int shaderProgram = 0;
    private int aPositionLoc, aTexCoordLoc;
    private int uMVPMatrixLoc, uTextureLoc;

    // Texture
    private int textureId = 0;

    // Transform - horizontal wall at bottom of screen
    private float posX = 0.0f;
    private float posY = -0.55f;
    private float posZ = 0.0f;
    private float scale = 0.4f;
    private float rotationY = 0f;

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    private final float[] tempMatrix = new float[16];

    // Screen size
    private int screenWidth = 1;
    private int screenHeight = 1;
    private boolean mvpDirty = true; // Solo recalcular MVP cuando cambia posicion/escala

    private boolean modelLoaded = false;

    // ═══════════════════════════════════════════════════════════════════════════
    // SHADERS - Basic texture only (static object)
    // ═══════════════════════════════════════════════════════════════════════════
    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "precision highp float;\n" +
        "in vec3 aPosition;\n" +
        "in vec2 aTexCoord;\n" +
        "uniform mat4 uMVPMatrix;\n" +
        "out vec2 vTexCoord;\n" +
        "void main() {\n" +
        "    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);\n" +
        "    vTexCoord = aTexCoord;\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "uniform sampler2D uTexture;\n" +
        "in vec2 vTexCoord;\n" +
        "out vec4 fragColor;\n" +
        "void main() {\n" +
        "    fragColor = texture(uTexture, vTexCoord);\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════

    public BrickWall3D(Context context) {
        this.context = context;
        initialize();
    }

    private void initialize() {
        compileShader();
        loadModel();
        loadTexture();
    }

    private void loadModel() {
        try {
            ObjLoader.Mesh mesh;
            // Prioridad: descarga remota > assets locales
            ModelDownloadManager modelMgr = ModelDownloadManager.getInstance(context);
            String modelPath = modelMgr.getModelPath("brick_wall.obj");

            if (modelPath != null && new File(modelPath).exists()) {
                Log.d(TAG, "🌐 Modelo desde descarga: " + modelPath);
                mesh = ObjLoader.loadObjFromFile(modelPath, true);
            } else {
                Log.d(TAG, "📂 Modelo desde assets (fallback)");
                mesh = ObjLoader.loadObj(context, OBJ_ASSET_PATH, true);
            }

            this.vertexBuffer = mesh.vertexBuffer;
            this.uvBuffer = mesh.uvBuffer;
            this.indexCount = ObjLoader.countIndices(mesh.faces);
            this.indexBuffer = ObjLoader.buildIndexBuffer(mesh.faces, indexCount);

            modelLoaded = true;
            Log.d(TAG, "🧱 Model loaded: " + indexCount + " indices");
        } catch (IOException e) {
            Log.e(TAG, "❌ Error loading model: " + e.getMessage());
        }
    }

    private void loadTexture() {
        // Prioridad: descarga remota > assets locales
        ImageDownloadManager imageMgr = ImageDownloadManager.getInstance(context);
        String texturePath = imageMgr.getImagePath("brick_wall_texture.png");

        if (texturePath != null && new File(texturePath).exists()) {
            textureId = loadTextureFromFile(texturePath);
            if (textureId != 0) {
                Log.d(TAG, "🌐 Textura remota cargada: " + textureId);
                return;
            }
        }

        // Fallback: assets locales
        loadTextureFromAssets();
    }

    private int loadTextureFromFile(String filePath) {
        try {
            // Adaptive downscale: LOW=4 (512px, ~1MB), MEDIUM/HIGH=2 (1024px, ~4MB)
            int inSampleSize = DeviceProfile.get().isLowRam() ? 4 : 2;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            options.inSampleSize = inSampleSize;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);

            if (bitmap != null) {
                int texId = uploadBitmapToGL(bitmap);
                bitmap.recycle();
                return texId;
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error loading texture from file: " + e.getMessage());
        }
        return 0;
    }

    private void loadTextureFromAssets() {
        InputStream is = null;
        try {
            is = context.getAssets().open(TEXTURE_ASSET_PATH);
            int inSampleSize = DeviceProfile.get().isLowRam() ? 4 : 2;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            options.inSampleSize = inSampleSize;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);

            if (bitmap != null) {
                textureId = uploadBitmapToGL(bitmap);
                bitmap.recycle();
                Log.d(TAG, "📂 Textura assets cargada: " + textureId);
            }
        } catch (IOException e) {
            Log.e(TAG, "❌ Error loading texture from assets: " + e.getMessage());
        } finally {
            if (is != null) {
                try { is.close(); } catch (IOException ignored) {}
            }
        }
    }

    private int uploadBitmapToGL(Bitmap bitmap) {
        int[] textures = new int[1];
        GLES30.glGenTextures(1, textures, 0);
        int texId = textures[0];

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);

        return texId;
    }

    private void compileShader() {
        int vs = compileShaderCode(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compileShaderCode(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        if (vs == 0 || fs == 0) return;

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vs);
        GLES30.glAttachShader(shaderProgram, fs);
        GLES30.glLinkProgram(shaderProgram);

        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "aPosition");
        aTexCoordLoc = GLES30.glGetAttribLocation(shaderProgram, "aTexCoord");
        uMVPMatrixLoc = GLES30.glGetUniformLocation(shaderProgram, "uMVPMatrix");
        uTextureLoc = GLES30.glGetUniformLocation(shaderProgram, "uTexture");

        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);
        Log.d(TAG, "🧱 Shader compiled");
    }

    private int compileShaderCode(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
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

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE & DRAW
    // ═══════════════════════════════════════════════════════════════════════════

    public void update(float deltaTime) {
        // Static object - no animation needed
    }

    public void draw() {
        if (!modelLoaded || shaderProgram == 0 || vertexBuffer == null || indexBuffer == null) return;

        GLES30.glUseProgram(shaderProgram);

        // MVP cacheado: solo recalcular cuando cambia posicion/escala/rotacion
        if (mvpDirty) {
            float aspect = (float) screenWidth / screenHeight;
            Matrix.perspectiveM(projectionMatrix, 0, 45f, aspect, 0.1f, 100f);
            Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1f, 0f);

            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.translateM(modelMatrix, 0, posX, posY, posZ);
            Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f);
            Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

            Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);
            mvpDirty = false;
        }

        // Uniforms
        GLES30.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, mvpMatrix, 0);

        // Texture
        if (textureId != 0) {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
            GLES30.glUniform1i(uTextureLoc, 0);
        }

        // Vertices
        vertexBuffer.position(0);
        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glVertexAttribPointer(aPositionLoc, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        // UVs
        if (uvBuffer != null) {
            uvBuffer.position(0);
            GLES30.glEnableVertexAttribArray(aTexCoordLoc);
            GLES30.glVertexAttribPointer(aTexCoordLoc, 2, GLES30.GL_FLOAT, false, 0, uvBuffer);
        }

        // Draw
        indexBuffer.position(0);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, indexBuffer);

        GLES30.glDisableVertexAttribArray(aPositionLoc);
        if (uvBuffer != null) GLES30.glDisableVertexAttribArray(aTexCoordLoc);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SETTERS
    // ═══════════════════════════════════════════════════════════════════════════

    public void setPosition(float x, float y, float z) {
        this.posX = x; this.posY = y; this.posZ = z; mvpDirty = true;
    }

    public void setScale(float s) {
        this.scale = s; mvpDirty = true;
    }

    public void setScreenSize(int width, int height) {
        this.screenWidth = width; this.screenHeight = height; mvpDirty = true;
    }

    public void setRotationY(float angle) {
        this.rotationY = angle; mvpDirty = true;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GETTERS (for debug/editing)
    // ═══════════════════════════════════════════════════════════════════════════

    public float getPosX() { return posX; }
    public float getPosY() { return posY; }
    public float getPosZ() { return posZ; }
    public float getScale() { return scale; }
    public float getRotationY() { return rotationY; }

    // ═══════════════════════════════════════════════════════════════════════════
    // RELEASE
    // ═══════════════════════════════════════════════════════════════════════════

    public void release() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        if (textureId != 0) {
            GLES30.glDeleteTextures(1, new int[]{textureId}, 0);
            textureId = 0;
        }
        modelLoaded = false;
        Log.d(TAG, "🧱 BrickWall3D released");
    }
}
