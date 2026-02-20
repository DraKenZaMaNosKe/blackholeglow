package com.secret.blackholeglow.video;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.util.ObjLoader;
import com.secret.blackholeglow.model.ModelDownloadManager;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * FriezaHalo3D - Aureola angelical de Frieza.
 * Objeto separado del cuerpo con shader de glow blanco brillante.
 * Usa el mismo sistema de coordenadas que Frieza3D (comparten origin).
 */
public class FriezaHalo3D {
    private static final String TAG = "FriezaHalo3D";

    private final Context context;

    // Mesh
    private FloatBuffer vertexBuffer;
    private IntBuffer indexBuffer;
    private int indexCount;

    // Shader
    private int shaderProgram = 0;
    private int aPositionLoc;
    private int uMVPMatrixLoc, uTimeLoc;

    // Transform
    private float posX = 0f, posY = 0f, posZ = 0f;
    private float scale = 0.25f;
    private float time = 0f;

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    private final float[] tempMatrix = new float[16];

    private int screenWidth = 1, screenHeight = 1;
    private boolean modelLoaded = false;

    // Vertex shader
    private static final String VERTEX_SHADER =
        "precision highp float;\n" +
        "attribute vec3 aPosition;\n" +
        "uniform mat4 uMVPMatrix;\n" +
        "varying vec3 vPosition;\n" +
        "void main() {\n" +
        "    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);\n" +
        "    vPosition = aPosition;\n" +
        "}\n";

    // Fragment shader: bright angelic white glow with shimmer
    private static final String FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "uniform float uTime;\n" +
        "varying vec3 vPosition;\n" +
        "void main() {\n" +
        // Simple bright white - no complex normal calculations
        // Use position-based lighting as fallback (normals may be unreliable)
        "    vec3 baseColor = vec3(0.95, 0.93, 0.85);\n" +
        //
        // Traveling shimmer along the ring
        "    float angle = atan(vPosition.z, vPosition.x);\n" +
        "    float shimmer1 = 0.85 + 0.15 * sin(angle * 4.0 + uTime * 3.0);\n" +
        "    float shimmer2 = 0.92 + 0.08 * sin(angle * 7.0 - uTime * 5.0);\n" +
        //
        // Subtle brightness pulse (breathing)
        "    float pulse = 0.92 + 0.08 * sin(uTime * 2.5);\n" +
        //
        // Bright! This is an angelic halo, it glows
        "    vec3 col = baseColor * shimmer1 * shimmer2;\n" +
        "    float brightness = 2.2 * pulse;\n" +
        "    gl_FragColor = vec4(col * brightness, 1.0);\n" +
        "}\n";

    public FriezaHalo3D(Context context) {
        this.context = context;
        initialize();
    }

    private void initialize() {
        compileShader();
        loadModel();
    }

    private void loadModel() {
        try {
            ObjLoader.Mesh mesh;
            ModelDownloadManager modelMgr = ModelDownloadManager.getInstance(context);
            String modelPath = modelMgr.getModelPath("frieza_halo.obj");

            if (modelPath != null && new File(modelPath).exists()) {
                Log.d(TAG, "Halo model from download: " + modelPath);
                mesh = ObjLoader.loadObjFromFile(modelPath, true);
            } else {
                Log.w(TAG, "frieza_halo.obj not downloaded yet");
                return;
            }

            vertexBuffer = mesh.vertexBuffer;
            indexCount = ObjLoader.countIndices(mesh.faces);
            indexBuffer = ObjLoader.buildIndexBuffer(mesh.faces, indexCount);
            modelLoaded = true;
            Log.d(TAG, "Halo loaded: " + indexCount + " indices");
        } catch (IOException e) {
            Log.e(TAG, "Error loading halo: " + e.getMessage());
        }
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
        uMVPMatrixLoc = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");
        uTimeLoc = GLES20.glGetUniformLocation(shaderProgram, "uTime");

        GLES20.glDeleteShader(vs);
        GLES20.glDeleteShader(fs);
        Log.d(TAG, "Halo shader compiled OK");
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
        if (time > 62.83f) time -= 62.83f;
    }

    public void draw() {
        if (!modelLoaded || shaderProgram == 0) return;

        GLES20.glUseProgram(shaderProgram);

        float aspect = (float) screenWidth / screenHeight;
        Matrix.perspectiveM(projectionMatrix, 0, 45f, aspect, 0.1f, 100f);
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1f, 0f);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, posX, posY, posZ);
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);

        GLES20.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, mvpMatrix, 0);
        GLES20.glUniform1f(uTimeLoc, time);

        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        indexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_INT, indexBuffer);

        GLES20.glDisableVertexAttribArray(aPositionLoc);
    }

    // Setters
    public void setPosition(float x, float y, float z) { posX = x; posY = y; posZ = z; }
    public void setScale(float s) { scale = s; }
    public void setScreenSize(int w, int h) { screenWidth = w; screenHeight = h; }

    public void release() {
        if (shaderProgram != 0) {
            GLES20.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        modelLoaded = false;
        Log.d(TAG, "FriezaHalo3D released");
    }
}
