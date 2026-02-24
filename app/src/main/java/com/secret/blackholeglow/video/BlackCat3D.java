package com.secret.blackholeglow.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.util.ObjLoader;
import com.secret.blackholeglow.image.ImageDownloadManager;
import com.secret.blackholeglow.model.ModelDownloadManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * BlackCat3D - Gato Negro con Parpadeo por Atlas Texture Swap
 *
 * Un solo mesh (el OBJ original). El parpadeo se logra intercambiando
 * el atlas completo entre versiones con ojos abiertos/medio/cerrados.
 *
 * Assets:
 *   - black_cat_clean.obj    (modelo con UV limpio re-bakeado)
 *   - cat_open.png           (atlas ojos abiertos)
 *   - cat_half.png           (atlas ojos medio cerrados)
 *   - cat_closed.png         (atlas ojos cerrados)
 */
public class BlackCat3D {
    private static final String TAG = "BlackCat3D";

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

    // Atlas textures for blink (same mesh, different atlas)
    private static final int EYE_OPEN = 0;
    private static final int EYE_HALF = 1;
    private static final int EYE_CLOSED = 2;
    private int[] atlasTextureIds = new int[3];
    private int currentTexture = 0;

    // Blink animation
    private static final float BLINK_INTERVAL = 4.0f;
    private static final float BLINK_FRAME_DURATION = 0.07f;
    private float blinkTimer = 0f;
    private int blinkState = 0; // 0=idle, 1=closing, 2=opening
    private float blinkFrameTimer = 0f;
    private int blinkFrame = EYE_OPEN;

    // Transform
    private float posX = 0.0f;
    private float posY = -0.35f;
    private float posZ = 0.0f;
    private float scale = 0.25f;
    private float rotationX = 0f;
    private float rotationY = 0f;
    private float rotationZ = 0f;

    // Animation
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
    private boolean mvpDirty = true; // Solo recalcular MVP cuando cambia posicion/escala

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

    private static final String FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "uniform sampler2D uTexture;\n" +
        "uniform float uTime;\n" +
        "varying vec2 vTexCoord;\n" +
        "varying vec3 vPosition;\n" +
        "void main() {\n" +
        "    vec4 texColor = texture2D(uTexture, vTexCoord);\n" +
        "    float posLen = length(vPosition);\n" +
        "    vec3 nPos = posLen > 0.001 ? vPosition / posLen : vec3(0.0, 0.0, 1.0);\n" +
        "    float rim = pow(1.0 - abs(dot(nPos, vec3(0.0, 0.0, 1.0))), 2.0);\n" +
        "    vec3 rimColor = vec3(0.15, 0.2, 0.4) * rim * 0.5;\n" +
        "    float breathe = sin(uTime * 1.2) * 0.02 + 1.0;\n" +
        "    vec3 finalColor = texColor.rgb * breathe + rimColor;\n" +
        "    gl_FragColor = vec4(finalColor, texColor.a);\n" +
        "}\n";

    public BlackCat3D(Context context) {
        this.context = context;
        initialize();
    }

    private void initialize() {
        compileShader();
        loadModel();
        loadTextures();
    }

    private void loadModel() {
        try {
            ObjLoader.Mesh mesh;
            // Prioridad: descarga remota > assets locales
            ModelDownloadManager modelMgr = ModelDownloadManager.getInstance(context);
            String modelPath = modelMgr.getModelPath("black_cat_clean.obj");

            if (modelPath != null && new File(modelPath).exists()) {
                Log.d(TAG, "🌐 Modelo desde descarga: " + modelPath);
                mesh = ObjLoader.loadObjFromFile(modelPath, true);
            } else {
                Log.d(TAG, "📂 Modelo desde assets (fallback)");
                mesh = ObjLoader.loadObj(context, "models/black_cat_clean.obj", true);
            }

            vertexBuffer = mesh.vertexBuffer;
            uvBuffer = mesh.uvBuffer;
            indexCount = ObjLoader.countIndices(mesh.faces);
            indexBuffer = ObjLoader.buildIndexBuffer(mesh.faces, indexCount);
            modelLoaded = true;
            Log.d(TAG, "Model loaded: " + indexCount + " indices");
        } catch (IOException e) {
            Log.e(TAG, "Error loading model: " + e.getMessage());
        }
    }

    private void loadTextures() {
        // Prioridad: descarga remota > assets locales
        atlasTextureIds[EYE_OPEN] = loadTexture("cat_open.png", "models/cat_open.png");
        atlasTextureIds[EYE_HALF] = loadTexture("cat_half.png", "models/cat_half.png");
        atlasTextureIds[EYE_CLOSED] = loadTexture("cat_closed.png", "models/cat_closed.png");

        // Fallback: si half/closed no existen, usar open
        if (atlasTextureIds[EYE_HALF] == 0) atlasTextureIds[EYE_HALF] = atlasTextureIds[EYE_OPEN];
        if (atlasTextureIds[EYE_CLOSED] == 0) atlasTextureIds[EYE_CLOSED] = atlasTextureIds[EYE_OPEN];

        currentTexture = atlasTextureIds[EYE_OPEN];
    }

    /**
     * Carga textura: primero intenta desde descarga remota, luego fallback a assets.
     * Optimizado: downscale 2048→1024 para reducir VRAM.
     */
    private int loadTexture(String remoteFileName, String assetPath) {
        // Intentar desde descarga remota
        ImageDownloadManager imageMgr = ImageDownloadManager.getInstance(context);
        String downloadPath = imageMgr.getImagePath(remoteFileName);

        if (downloadPath != null && new File(downloadPath).exists()) {
            int texId = loadTextureFromFile(downloadPath);
            if (texId != 0) {
                Log.d(TAG, "🌐 Textura remota: " + remoteFileName + " id=" + texId);
                return texId;
            }
        }

        // Fallback: assets locales
        return loadTextureFromAsset(assetPath);
    }

    private int loadTextureFromFile(String filePath) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            options.inSampleSize = 2;  // 2048→1024
            options.inPreferredConfig = Bitmap.Config.RGB_565;  // 2 bpp vs 4 bpp = 50% VRAM
            Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
            if (bitmap != null) {
                int texId = uploadBitmapToGL(bitmap);
                bitmap.recycle();
                return texId;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error loading texture from file: " + filePath);
        }
        return 0;
    }

    private int loadTextureFromAsset(String path) {
        InputStream is = null;
        try {
            is = context.getAssets().open(path);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            options.inSampleSize = 2;  // 2048→1024
            options.inPreferredConfig = Bitmap.Config.RGB_565;  // 2 bpp vs 4 bpp = 50% VRAM
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
            if (bitmap != null) {
                int texId = uploadBitmapToGL(bitmap);
                bitmap.recycle();
                Log.d(TAG, "📂 Textura assets: " + path + " id=" + texId);
                return texId;
            }
        } catch (IOException e) {
            Log.w(TAG, "Texture not found: " + path + " (will use fallback)");
        } finally {
            if (is != null) try { is.close(); } catch (IOException ignored) {}
        }
        return 0;
    }

    private int uploadBitmapToGL(Bitmap bitmap) {
        int[] textures = new int[1];
        GLES30.glGenTextures(1, textures, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[0]);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);
        return textures[0];
    }

    private void compileShader() {
        int vs = compileShaderCode(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compileShaderCode(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        if (vs == 0 || fs == 0) return;

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vs);
        GLES30.glAttachShader(shaderProgram, fs);
        GLES30.glLinkProgram(shaderProgram);

        int[] linked = new int[1];
        GLES30.glGetProgramiv(shaderProgram, GLES30.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            Log.e(TAG, "Shader link error: " + GLES30.glGetProgramInfoLog(shaderProgram));
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
            GLES30.glDeleteShader(vs);
            GLES30.glDeleteShader(fs);
            return;
        }

        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "aPosition");
        aTexCoordLoc = GLES30.glGetAttribLocation(shaderProgram, "aTexCoord");
        uMVPMatrixLoc = GLES30.glGetUniformLocation(shaderProgram, "uMVPMatrix");
        uTimeLoc = GLES30.glGetUniformLocation(shaderProgram, "uTime");
        uTextureLoc = GLES30.glGetUniformLocation(shaderProgram, "uTexture");

        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);
        Log.d(TAG, "Shader compiled OK");
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

    // ═══════════════════════════════════════════════════════════════
    // UPDATE & DRAW
    // ═══════════════════════════════════════════════════════════════

    public void update(float deltaTime) {
        time += deltaTime;
        if (time > 1000f) time -= 1000f;
        updateBlink(deltaTime);
    }

    private void updateBlink(float deltaTime) {
        blinkTimer += deltaTime;

        switch (blinkState) {
            case 0: // Idle
                if (blinkTimer >= BLINK_INTERVAL) {
                    blinkState = 1;
                    blinkFrameTimer = 0f;
                    blinkFrame = EYE_OPEN;
                }
                break;

            case 1: // Closing: open -> half -> closed
                blinkFrameTimer += deltaTime;
                if (blinkFrameTimer >= BLINK_FRAME_DURATION) {
                    blinkFrameTimer = 0f;
                    blinkFrame++;
                    if (blinkFrame > EYE_CLOSED) {
                        blinkFrame = EYE_CLOSED;
                        blinkState = 2;
                    }
                }
                break;

            case 2: // Opening: closed -> half -> open
                blinkFrameTimer += deltaTime;
                if (blinkFrameTimer >= BLINK_FRAME_DURATION) {
                    blinkFrameTimer = 0f;
                    blinkFrame--;
                    if (blinkFrame < EYE_OPEN) {
                        blinkFrame = EYE_OPEN;
                        blinkState = 0;
                        blinkTimer = 0f;
                    }
                }
                break;
        }

        currentTexture = atlasTextureIds[blinkFrame];
    }

    public void draw() {
        if (!modelLoaded || shaderProgram == 0) return;

        GLES30.glUseProgram(shaderProgram);

        // MVP cacheado: solo recalcular cuando cambia posicion/escala/rotacion
        if (mvpDirty) {
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
            mvpDirty = false;
        }

        GLES30.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, mvpMatrix, 0);
        GLES30.glUniform1f(uTimeLoc, time);

        // Bind current atlas texture (swapped for blink)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, currentTexture);
        GLES30.glUniform1i(uTextureLoc, 0);

        // Draw single mesh
        vertexBuffer.position(0);
        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glVertexAttribPointer(aPositionLoc, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        if (uvBuffer != null) {
            uvBuffer.position(0);
            GLES30.glEnableVertexAttribArray(aTexCoordLoc);
            GLES30.glVertexAttribPointer(aTexCoordLoc, 2, GLES30.GL_FLOAT, false, 0, uvBuffer);
        }

        indexBuffer.position(0);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, indexBuffer);

        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glDisableVertexAttribArray(aTexCoordLoc);
    }

    // ═══════════════════════════════════════════════════════════════
    // SETTERS / GETTERS
    // ═══════════════════════════════════════════════════════════════

    public void setPosition(float x, float y, float z) {
        this.posX = x; this.posY = y; this.posZ = z; mvpDirty = true;
    }
    public void setScale(float s) { this.scale = s; mvpDirty = true; }
    public void setRotationX(float angle) { this.rotationX = angle; mvpDirty = true; }
    public void setRotationY(float angle) { this.rotationY = angle; mvpDirty = true; }
    public void setRotationZ(float angle) { this.rotationZ = angle; mvpDirty = true; }
    public void setScreenSize(int w, int h) { screenWidth = w; screenHeight = h; mvpDirty = true; }

    public float getPosX() { return posX; }
    public float getPosY() { return posY; }
    public float getPosZ() { return posZ; }
    public float getScale() { return scale; }
    public float getRotationX() { return rotationX; }
    public float getRotationY() { return rotationY; }
    public float getRotationZ() { return rotationZ; }

    // ═══════════════════════════════════════════════════════════════
    // RELEASE
    // ═══════════════════════════════════════════════════════════════

    public void release() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        // Delete unique texture IDs (avoid double-delete if fallback shares ID)
        int lastDeleted = 0;
        for (int i = 0; i < atlasTextureIds.length; i++) {
            if (atlasTextureIds[i] != 0 && atlasTextureIds[i] != lastDeleted) {
                GLES30.glDeleteTextures(1, new int[]{atlasTextureIds[i]}, 0);
                lastDeleted = atlasTextureIds[i];
                atlasTextureIds[i] = 0;
            }
        }
        modelLoaded = false;
        Log.d(TAG, "BlackCat3D released");
    }
}
