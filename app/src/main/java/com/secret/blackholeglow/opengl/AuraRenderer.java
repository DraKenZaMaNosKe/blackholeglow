package com.secret.blackholeglow.opengl;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class AuraRenderer implements GLSurfaceView.Renderer {

    private final Context context;
    private int program;
    private int aPositionLocation;
    private int uTimeLocation;
    private int uResolutionLocation;
    private float time = 0f;
    private int screenWidth = 1;
    private int screenHeight = 1;

    private final float[] quadCoords = {
            -1f, -1f,
            1f, -1f,
            -1f,  1f,
            1f,  1f
    };

    private int quadBufferId;

    public AuraRenderer(Context context) {
        this.context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0f, 0f, 0f, 0f);

        String vertexShaderCode = loadShaderFromAssets("shaders/aura_vertex.glsl");
        String fragmentShaderCode = loadShaderFromAssets("shaders/aura_fragment.glsl");

        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        aPositionLocation = GLES20.glGetAttribLocation(program, "a_Position");
        uTimeLocation = GLES20.glGetUniformLocation(program, "u_Time");
        uResolutionLocation = GLES20.glGetUniformLocation(program, "u_Resolution");

        // Create buffer
        int[] buffers = new int[1];
        GLES20.glGenBuffers(1, buffers, 0);
        quadBufferId = buffers[0];

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, quadBufferId);
        java.nio.FloatBuffer vertexBuffer = java.nio.ByteBuffer
                .allocateDirect(quadCoords.length * 4)
                .order(java.nio.ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(quadCoords).position(0);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, quadCoords.length * 4, vertexBuffer, GLES20.GL_STATIC_DRAW);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        screenWidth = width;
        screenHeight = height;
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(program);

        time += 0.01f;
        GLES20.glUniform1f(uTimeLocation, time);
        GLES20.glUniform2f(uResolutionLocation, screenWidth, screenHeight);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, quadBufferId);
        GLES20.glEnableVertexAttribArray(aPositionLocation);
        GLES20.glVertexAttribPointer(aPositionLocation, 2, GLES20.GL_FLOAT, false, 0, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(aPositionLocation);
    }

    private String loadShaderFromAssets(String fileName) {
        StringBuilder shaderCode = new StringBuilder();
        try {
            InputStream inputStream = context.getAssets().open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                shaderCode.append(line).append("\n");
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return shaderCode.toString();
    }

    private int compileShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}
