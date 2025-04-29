package com.secret.blackholeglow;

import android.opengl.GLES20;
import java.util.Random;

public class Star {
    public float colorR, colorG, colorB;

    private float baseSize;

    private static int uTimeLocation;

    public float x, y, z;
    public float angle;
    public float speed;
    public float alpha;

    private static final Random rand = new Random();

    private static int program = -1;
    private static int aPositionLocation;
    private static int aColorLocation;
    private static int uPointSizeLocation;

    public Star() {
        reset();
    }

    public static void release() {
        if (program != -1) {
            GLES20.glDeleteProgram(program);
            program = -1;
        }
    }

    public void reset() {
        baseSize = 2.0f + rand.nextFloat() * 3.0f; // tamaño base entre 2.0 y 5.0
        angle = rand.nextFloat() * (float)(2 * Math.PI);
        z = 1.0f + rand.nextFloat() * 10.0f; // Estrellas más lejanas
        speed = 1.0f + rand.nextFloat() * 2.5f;
        alpha = 0.2f + rand.nextFloat() * 0.6f;
        // Asignar color basado en tipo espectral
        float[][] coloresEstelares = {
                {0.6f, 0.8f, 1.0f},  // Azul (Tipo O/B)
                {0.9f, 0.9f, 1.0f},  // Blanco (Tipo A)
                {1.0f, 1.0f, 0.8f},  // Amarillo claro (Tipo F/G)
                {1.0f, 0.8f, 0.6f},  // Naranja (Tipo K)
                {1.0f, 0.6f, 0.6f}   // Rojo (Tipo M)
        };

        int tipo = rand.nextInt(coloresEstelares.length);
        float[] rgb = coloresEstelares[tipo];
        colorR = rgb[0];
        colorG = rgb[1];
        colorB = rgb[2];
    }

    public void update(float deltaTime, float tunnelAngle) {
        z -= speed * deltaTime;

        angle += deltaTime * 1.5f;  // rotación tipo vórtice

        if (z < 0.1f) {
            reset();
        }

        // === NUEVO cálculo tipo túnel ===
        float perspective = 1.0f / z;
        float radius = 1.2f * (1.0f - z / 7.0f); // más cerca del centro al inicio

        x = radius * (float)Math.cos(angle);
        y = radius * (float)Math.sin(angle);

        // perspectiva (se aplica también en draw pero aquí ayuda a distribución)
        x *= perspective;
        y *= perspective;
    }

    private int compileShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    public void draw() {

        if (program == -1) {
            int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, StarShader.VERTEX_SHADER);
            int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, StarShader.FRAGMENT_SHADER);
            program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, fragmentShader);
            GLES20.glLinkProgram(program);
            aPositionLocation = GLES20.glGetAttribLocation(program, "a_Position");
            aColorLocation = GLES20.glGetAttribLocation(program, "a_Color");
            uPointSizeLocation = GLES20.glGetUniformLocation(program, "u_PointSize");
            uTimeLocation = GLES20.glGetUniformLocation(program, "u_Time");

        }

        GLES20.glUseProgram(program);

        float perspective = 1.0f / z;
        float[] vertices = {
                x, y, 0f  // ya están transformadas con perspectiva en update()
        };

        float[] colors = {colorR, colorG, colorB, alpha};


        int vertexBufferId = ShaderUtils.createBuffer(vertices);
        int colorBufferId = ShaderUtils.createBuffer(colors);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferId);
        GLES20.glEnableVertexAttribArray(aPositionLocation);
        GLES20.glVertexAttribPointer(aPositionLocation, 3, GLES20.GL_FLOAT, false, 0, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, colorBufferId);
        GLES20.glEnableVertexAttribArray(aColorLocation);
        GLES20.glVertexAttribPointer(aColorLocation, 4, GLES20.GL_FLOAT, false, 0, 0);

        float size = baseSize * (1.0f / z) * 100f; // factor mucho más pequeño
        size = Math.max(2f, Math.min(size, 30f)); // tamaño final entre 2 y 30
        GLES20.glUniform1f(uPointSizeLocation, size);

        float time = System.nanoTime() / 1_000_000_000f;
        GLES20.glUniform1f(uTimeLocation, time);

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);

        GLES20.glDisableVertexAttribArray(aPositionLocation);
        GLES20.glDisableVertexAttribArray(aColorLocation);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }
}