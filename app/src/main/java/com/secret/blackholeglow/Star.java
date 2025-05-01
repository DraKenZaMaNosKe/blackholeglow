package com.secret.blackholeglow;

import android.opengl.GLES20;
import java.util.Random;

public class Star {
    private static int program = -1;
    private static int aPositionLocation;
    private static int uColorLocation;
    private float x, y, z;
    private float angle, speed, alpha;
    private float baseSize;

    private static final Random rand = new Random();

    public Star(int unused) {
        reset();
    }

    public static void release() {
        if (program != -1) {
            GLES20.glDeleteProgram(program);
            program = -1;
        }
    }

    public void reset() {
        baseSize = 2.0f + rand.nextFloat() * 3.0f;
        angle = rand.nextFloat() * (float) (2 * Math.PI);
        z = 1.0f + rand.nextFloat() * 10.0f;
        speed = 1.0f + rand.nextFloat() * 2.5f;
        alpha = 0.2f + rand.nextFloat() * 0.6f;
    }

    public void update(float deltaTime, float tunnelAngle) {
        z -= speed * deltaTime;
        angle += deltaTime * 1.5f;
        if (z < 0.1f) reset();

        float perspective = 1.0f / z;
        float radius = 1.5f * (1.0f - z / 20.0f);
        x = radius * (float) Math.cos(angle) * perspective;
        y = radius * (float) Math.sin(angle) * perspective;
    }

    public void draw() {
        if (program == -1) {
            String vertexShaderCode =
                    "attribute vec4 a_Position;" +
                            "void main() {" +
                            "    gl_Position = a_Position;" +
                            "    gl_PointSize = 5.0;" +
                            "}";

            String fragmentShaderCode =
                    "precision mediump float;" +
                            "uniform vec4 u_Color;" +
                            "void main() {" +
                            "    gl_FragColor = u_Color;" +
                            "}";

            int vertexShader = ShaderUtils.createShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            int fragmentShader = ShaderUtils.createShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

            program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, fragmentShader);
            GLES20.glLinkProgram(program);

            aPositionLocation = GLES20.glGetAttribLocation(program, "a_Position");
            uColorLocation = GLES20.glGetUniformLocation(program, "u_Color");
        }

        GLES20.glUseProgram(program);

        float[] position = {x, y, 0f};

        GLES20.glVertexAttribPointer(aPositionLocation, 3, GLES20.GL_FLOAT, false, 0, ShaderUtils.createFloatBuffer(position));
        GLES20.glEnableVertexAttribArray(aPositionLocation);

        GLES20.glUniform4f(uColorLocation, 1f, 1f, 1f, alpha); // blanco con alpha

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);

        GLES20.glDisableVertexAttribArray(aPositionLocation);
    }
}