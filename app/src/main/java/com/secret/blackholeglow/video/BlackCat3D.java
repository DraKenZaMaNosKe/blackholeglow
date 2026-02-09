package com.secret.blackholeglow.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.util.ObjLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * BlackCat3D - Gato Negro con Parpadeo por Texture Swap
 *
 * Cuerpo y ojos son meshes separados. El parpadeo se logra
 * cambiando la textura de los ojos entre sprites (open/half/closed).
 *
 * Assets:
 *   - black_cat_body.obj   (cuerpo estático)
 *   - black_cat_eyes.obj   (iris + esclera, posición relativa correcta)
 *   - black_cat_texture.png (textura del cuerpo)
 *   - eye_open.png, eye_half.png, eye_closed.png (sprites de ojos)
 */
public class BlackCat3D {
    private static final String TAG = "BlackCat3D";

    private final Context context;

    // Body mesh
    private FloatBuffer bodyVertexBuffer;
    private FloatBuffer bodyUvBuffer;
    private IntBuffer bodyIndexBuffer;
    private int bodyIndexCount;

    // Eyes mesh
    private FloatBuffer eyesVertexBuffer;
    private FloatBuffer eyesUvBuffer;
    private IntBuffer eyesIndexBuffer;
    private int eyesIndexCount;

    // Shader
    private int shaderProgram = 0;
    private int aPositionLoc, aTexCoordLoc;
    private int uMVPMatrixLoc, uTimeLoc, uTextureLoc;

    // Textures
    private int bodyTextureId = 0;
    private int[] eyeTextureIds = new int[3]; // 0=open, 1=half, 2=closed
    private int currentEyeTexture = 0; // index into eyeTextureIds

    // Blink animation
    private static final int EYE_OPEN = 0;
    private static final int EYE_HALF = 1;
    private static final int EYE_CLOSED = 2;
    private static final float BLINK_INTERVAL = 4.0f;
    private static final float BLINK_FRAME_DURATION = 0.07f;

    // Blink state machine
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

    // Shader - simplified, no morph targets
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
        loadModels();
        loadTextures();
    }

    private void loadModels() {
        try {
            // Load body mesh
            ObjLoader.Mesh bodyMesh = ObjLoader.loadObj(context, "models/black_cat_body.obj", true);
            bodyVertexBuffer = bodyMesh.vertexBuffer;
            bodyUvBuffer = bodyMesh.uvBuffer;
            bodyIndexCount = ObjLoader.countIndices(bodyMesh.faces);
            bodyIndexBuffer = ObjLoader.buildIndexBuffer(bodyMesh.faces, bodyIndexCount);
            Log.d(TAG, "Body loaded: " + bodyIndexCount + " indices");

            // Load eyes mesh
            ObjLoader.Mesh eyesMesh = ObjLoader.loadObj(context, "models/black_cat_eyes.obj", true);
            eyesVertexBuffer = eyesMesh.vertexBuffer;
            eyesUvBuffer = eyesMesh.uvBuffer;
            eyesIndexCount = ObjLoader.countIndices(eyesMesh.faces);
            eyesIndexBuffer = ObjLoader.buildIndexBuffer(eyesMesh.faces, eyesIndexCount);
            Log.d(TAG, "Eyes loaded: " + eyesIndexCount + " indices");

            modelLoaded = true;
        } catch (IOException e) {
            Log.e(TAG, "Error loading models: " + e.getMessage());
        }
    }

    private void loadTextures() {
        // Body texture
        bodyTextureId = loadTextureFromAsset("models/black_cat_texture.png");

        // Eye textures for blink animation
        eyeTextureIds[EYE_OPEN] = loadTextureFromAsset("models/eye_open.png");
        eyeTextureIds[EYE_HALF] = loadTextureFromAsset("models/eye_half.png");
        eyeTextureIds[EYE_CLOSED] = loadTextureFromAsset("models/eye_closed.png");

        currentEyeTexture = eyeTextureIds[EYE_OPEN];
    }

    private int loadTextureFromAsset(String path) {
        InputStream is = null;
        try {
            is = context.getAssets().open(path);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
            if (bitmap != null) {
                int[] textures = new int[1];
                GLES20.glGenTextures(1, textures, 0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
                GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
                bitmap.recycle();
                Log.d(TAG, "Texture loaded: " + path + " id=" + textures[0]);
                return textures[0];
            }
        } catch (IOException e) {
            Log.e(TAG, "Error loading texture " + path + ": " + e.getMessage());
        } finally {
            if (is != null) try { is.close(); } catch (IOException ignored) {}
        }
        return 0;
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
        Log.d(TAG, "Shader compiled OK");
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
            case 0: // Idle - waiting for next blink
                if (blinkTimer >= BLINK_INTERVAL) {
                    blinkState = 1; // Start closing
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
                        blinkState = 2; // Start opening
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
                        blinkState = 0; // Back to idle
                        blinkTimer = 0f;
                    }
                }
                break;
        }

        currentEyeTexture = eyeTextureIds[blinkFrame];
    }

    public void draw() {
        if (!modelLoaded || shaderProgram == 0) return;

        GLES20.glUseProgram(shaderProgram);

        // Build MVP matrix
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

        // === DRAW BODY ===
        if (bodyVertexBuffer != null && bodyIndexBuffer != null && bodyTextureId != 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, bodyTextureId);
            GLES20.glUniform1i(uTextureLoc, 0);

            bodyVertexBuffer.position(0);
            GLES20.glEnableVertexAttribArray(aPositionLoc);
            GLES20.glVertexAttribPointer(aPositionLoc, 3, GLES20.GL_FLOAT, false, 0, bodyVertexBuffer);

            if (bodyUvBuffer != null) {
                bodyUvBuffer.position(0);
                GLES20.glEnableVertexAttribArray(aTexCoordLoc);
                GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, bodyUvBuffer);
            }

            bodyIndexBuffer.position(0);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, bodyIndexCount, GLES20.GL_UNSIGNED_INT, bodyIndexBuffer);
        }

        // === DRAW EYES (with swappable texture) ===
        if (eyesVertexBuffer != null && eyesIndexBuffer != null && currentEyeTexture != 0) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, currentEyeTexture);

            eyesVertexBuffer.position(0);
            GLES20.glVertexAttribPointer(aPositionLoc, 3, GLES20.GL_FLOAT, false, 0, eyesVertexBuffer);

            if (eyesUvBuffer != null) {
                eyesUvBuffer.position(0);
                GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, eyesUvBuffer);
            }

            eyesIndexBuffer.position(0);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, eyesIndexCount, GLES20.GL_UNSIGNED_INT, eyesIndexBuffer);
        }

        GLES20.glDisableVertexAttribArray(aPositionLoc);
        GLES20.glDisableVertexAttribArray(aTexCoordLoc);
    }

    // ═══════════════════════════════════════════════════════════════
    // SETTERS / GETTERS
    // ═══════════════════════════════════════════════════════════════

    public void setPosition(float x, float y, float z) {
        this.posX = x; this.posY = y; this.posZ = z;
    }
    public void setScale(float s) { this.scale = s; }
    public void setRotationX(float angle) { this.rotationX = angle; }
    public void setRotationY(float angle) { this.rotationY = angle; }
    public void setRotationZ(float angle) { this.rotationZ = angle; }
    public void setScreenSize(int w, int h) { screenWidth = w; screenHeight = h; }

    public float getPosX() { return posX; }
    public float getPosY() { return posY; }
    public float getPosZ() { return posZ; }
    public float getScale() { return scale; }
    public float getRotationX() { return rotationX; }
    public float getRotationY() { return rotationY; }
    public float getRotationZ() { return rotationZ; }

    // Legacy eye getters/setters for MoonlitCatScene compatibility
    private float eyeLeftX, eyeLeftY, eyeLeftZ;
    private float eyeRightX, eyeRightY, eyeRightZ;
    private float eyeRadius = 0.5f;

    public void setEyeLeft(float x, float y, float z) { eyeLeftX = x; eyeLeftY = y; eyeLeftZ = z; }
    public void setEyeRight(float x, float y, float z) { eyeRightX = x; eyeRightY = y; eyeRightZ = z; }
    public void setEyeRadius(float r) { eyeRadius = r; }
    public float getEyeLeftX() { return eyeLeftX; }
    public float getEyeLeftY() { return eyeLeftY; }
    public float getEyeLeftZ() { return eyeLeftZ; }
    public float getEyeRightX() { return eyeRightX; }
    public float getEyeRightY() { return eyeRightY; }
    public float getEyeRightZ() { return eyeRightZ; }
    public float getEyeRadius() { return eyeRadius; }

    // ═══════════════════════════════════════════════════════════════
    // RELEASE
    // ═══════════════════════════════════════════════════════════════

    public void release() {
        if (shaderProgram != 0) {
            GLES20.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        if (bodyTextureId != 0) {
            GLES20.glDeleteTextures(1, new int[]{bodyTextureId}, 0);
            bodyTextureId = 0;
        }
        for (int i = 0; i < eyeTextureIds.length; i++) {
            if (eyeTextureIds[i] != 0) {
                GLES20.glDeleteTextures(1, new int[]{eyeTextureIds[i]}, 0);
                eyeTextureIds[i] = 0;
            }
        }
        modelLoaded = false;
        Log.d(TAG, "BlackCat3D released");
    }
}
