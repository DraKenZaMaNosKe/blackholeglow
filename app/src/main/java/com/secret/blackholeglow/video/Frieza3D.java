package com.secret.blackholeglow.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.util.ObjLoader;
import com.secret.blackholeglow.image.ImageDownloadManager;
import com.secret.blackholeglow.model.ModelDownloadManager;

import android.app.ActivityManager;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Frieza3D - Modelo 3D de Frieza (Final Form) con textura baked de Meshy AI.
 * Shader con red rim light para efecto dramático.
 */
public class Frieza3D {
    private static final String TAG = "Frieza3D";

    private final Context context;

    // Mesh
    private FloatBuffer vertexBuffer;
    private FloatBuffer uvBuffer;
    private IntBuffer indexBuffer;
    private int indexCount;

    // Shader
    private int shaderProgram = 0;
    private int aPositionLoc, aTexCoordLoc;
    private int uMVPMatrixLoc, uTimeLoc, uTextureLoc;

    // Texture
    private int textureId = 0;

    // Transform
    private float posX = 0.0f;
    private float posY = -0.35f;
    private float posZ = 0.0f;
    private float scale = 0.25f;
    private float rotationX = 0f;
    private float rotationY = 0f;
    private float rotationZ = 0f;

    private float time = 0f;

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    private final float[] tempMatrix = new float[16];

    private int screenWidth = 1;
    private int screenHeight = 1;
    private boolean modelLoaded = false;

    // Vertex shader: standard textured model
    private static final String VERTEX_SHADER =
        "precision highp float;\n" +
        "attribute vec3 aPosition;\n" +
        "attribute vec2 aTexCoord;\n" +
        "uniform mat4 uMVPMatrix;\n" +
        "varying vec2 vTexCoord;\n" +
        "varying vec3 vPosition;\n" +
        "void main() {\n" +
        "    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);\n" +
        "    vTexCoord = aTexCoord;\n" +
        "    vPosition = aPosition;\n" +
        "}\n";

    // Fragment shader: textured + red rim light + subtle energy pulse
    private static final String FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "uniform sampler2D uTexture;\n" +
        "uniform float uTime;\n" +
        "varying vec2 vTexCoord;\n" +
        "varying vec3 vPosition;\n" +
        "void main() {\n" +
        "    vec4 texColor = texture2D(uTexture, vTexCoord);\n" +
        // Rim light (red-orange glow on edges)
        "    float posLen = length(vPosition);\n" +
        "    vec3 nPos = posLen > 0.001 ? vPosition / posLen : vec3(0.0, 0.0, 1.0);\n" +
        "    float rim = pow(1.0 - abs(dot(nPos, vec3(0.0, 0.0, 1.0))), 2.5);\n" +
        "    vec3 rimColor = vec3(0.8, 0.15, 0.05) * rim * (0.6 + 0.3 * sin(uTime * 2.0));\n" +
        // Subtle energy pulse on the whole model
        "    float pulse = 1.0 + 0.05 * sin(uTime * 3.0);\n" +
        "    vec3 finalColor = texColor.rgb * pulse + rimColor;\n" +
        "    gl_FragColor = vec4(finalColor, texColor.a);\n" +
        "}\n";

    public Frieza3D(Context context) {
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
            ModelDownloadManager modelMgr = ModelDownloadManager.getInstance(context);
            String modelPath = modelMgr.getModelPath("frieza.obj");

            if (modelPath != null && new File(modelPath).exists()) {
                Log.d(TAG, "Modelo desde descarga: " + modelPath);
                mesh = ObjLoader.loadObjFromFile(modelPath, true);
            } else {
                Log.w(TAG, "Modelo frieza.obj no descargado aun");
                return;
            }

            vertexBuffer = mesh.vertexBuffer;
            uvBuffer = mesh.uvBuffer;
            indexCount = ObjLoader.countIndices(mesh.faces);
            indexBuffer = ObjLoader.buildIndexBuffer(mesh.faces, indexCount);
            modelLoaded = true;
            Log.d(TAG, "Frieza model loaded: " + indexCount + " indices");
        } catch (IOException e) {
            Log.e(TAG, "Error loading frieza model: " + e.getMessage());
        }
    }

    private void loadTexture() {
        ImageDownloadManager imageMgr = ImageDownloadManager.getInstance(context);
        String downloadPath = imageMgr.getImagePath("frieza_texture.png");

        if (downloadPath != null && new File(downloadPath).exists()) {
            textureId = loadTextureFromFile(downloadPath);
            if (textureId != 0) {
                Log.d(TAG, "Frieza textura cargada: id=" + textureId);
            }
        } else {
            Log.w(TAG, "frieza_texture.png no descargada aun");
        }
    }

    private int loadTextureFromFile(String filePath) {
        try {
            // Detect RAM tier for adaptive texture quality
            int inSampleSize = 2;  // default: 2048→1024
            Bitmap.Config bitmapConfig = Bitmap.Config.ARGB_8888;
            try {
                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(memInfo);
                long totalGB = memInfo.totalMem / (1024L * 1024L * 1024L);

                if (totalGB < 4) {
                    inSampleSize = 4;  // 2048→512px (~1MB VRAM vs ~4MB)
                    bitmapConfig = Bitmap.Config.RGB_565;  // 16-bit, 50% less memory
                    Log.d(TAG, "LOW RAM (" + totalGB + "GB): texture 512px RGB_565");
                } else if (totalGB <= 6) {
                    inSampleSize = 2;  // 2048→1024px
                    bitmapConfig = Bitmap.Config.RGB_565;
                    Log.d(TAG, "MEDIUM RAM (" + totalGB + "GB): texture 1024px RGB_565");
                } else {
                    inSampleSize = 2;  // 2048→1024px (full quality not needed)
                    Log.d(TAG, "HIGH RAM (" + totalGB + "GB): texture 1024px ARGB_8888");
                }
            } catch (Exception e) {
                Log.w(TAG, "RAM detection failed, using defaults");
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            options.inSampleSize = inSampleSize;
            options.inPreferredConfig = bitmapConfig;
            Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
            if (bitmap != null) {
                Log.d(TAG, "Texture loaded: " + bitmap.getWidth() + "x" + bitmap.getHeight()
                    + " (" + (bitmap.getByteCount() / 1024) + " KB)");
                int texId = uploadBitmapToGL(bitmap);
                bitmap.recycle();
                return texId;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error loading texture: " + filePath);
        }
        return 0;
    }

    private int uploadBitmapToGL(Bitmap bitmap) {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        return textures[0];
    }

    private void compileShader() {
        int vs = compileShaderCode(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compileShaderCode(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        if (vs == 0 || fs == 0) return;

        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vs);
        GLES20.glAttachShader(shaderProgram, fs);
        GLES20.glLinkProgram(shaderProgram);

        int[] linked = new int[1];
        GLES20.glGetProgramiv(shaderProgram, GLES20.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            Log.e(TAG, "Shader link error: " + GLES20.glGetProgramInfoLog(shaderProgram));
            GLES20.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
            GLES20.glDeleteShader(vs);
            GLES20.glDeleteShader(fs);
            return;
        }

        aPositionLoc = GLES20.glGetAttribLocation(shaderProgram, "aPosition");
        aTexCoordLoc = GLES20.glGetAttribLocation(shaderProgram, "aTexCoord");
        uMVPMatrixLoc = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");
        uTimeLoc = GLES20.glGetUniformLocation(shaderProgram, "uTime");
        uTextureLoc = GLES20.glGetUniformLocation(shaderProgram, "uTexture");

        GLES20.glDeleteShader(vs);
        GLES20.glDeleteShader(fs);
        Log.d(TAG, "Frieza shader compiled OK");
    }

    private int compileShaderCode(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Shader error: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    public void update(float deltaTime) {
        time += deltaTime;
        if (time > 62.83f) time -= 62.83f;  // ~10 full cycles of sin(time*1..3), safe for mediump
    }

    public void draw() {
        if (!modelLoaded || shaderProgram == 0 || textureId == 0) return;

        GLES20.glUseProgram(shaderProgram);

        float aspect = (float) screenWidth / screenHeight;
        Matrix.perspectiveM(projectionMatrix, 0, 45f, aspect, 0.1f, 100f);
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1f, 0f);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, posX, posY, posZ);
        Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f);
        Matrix.rotateM(modelMatrix, 0, rotationX, 1f, 0f, 0f);
        Matrix.rotateM(modelMatrix, 0, rotationZ, 0f, 0f, 1f);
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);

        GLES20.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, mvpMatrix, 0);
        GLES20.glUniform1f(uTimeLoc, time);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTextureLoc, 0);

        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        if (uvBuffer != null) {
            uvBuffer.position(0);
            GLES20.glEnableVertexAttribArray(aTexCoordLoc);
            GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, uvBuffer);
        }

        indexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_INT, indexBuffer);

        GLES20.glDisableVertexAttribArray(aPositionLoc);
        GLES20.glDisableVertexAttribArray(aTexCoordLoc);
    }

    // Setters / Getters
    public void setPosition(float x, float y, float z) { posX = x; posY = y; posZ = z; }
    public void setScale(float s) { scale = s; }
    public void setRotationX(float a) { rotationX = a; }
    public void setRotationY(float a) { rotationY = a; }
    public void setRotationZ(float a) { rotationZ = a; }
    public void setScreenSize(int w, int h) { screenWidth = w; screenHeight = h; }
    public float getPosX() { return posX; }
    public float getPosY() { return posY; }
    public float getPosZ() { return posZ; }
    public float getScale() { return scale; }
    public float getRotationX() { return rotationX; }
    public float getRotationY() { return rotationY; }
    public float getRotationZ() { return rotationZ; }

    public void release() {
        if (shaderProgram != 0) {
            GLES20.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        if (textureId != 0) {
            GLES20.glDeleteTextures(1, new int[]{textureId}, 0);
            textureId = 0;
        }
        modelLoaded = false;
        Log.d(TAG, "Frieza3D released");
    }
}
