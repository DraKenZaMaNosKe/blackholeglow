package com.secret.blackholeglow;

import android.opengl.GLES20;
import android.util.Log;

import java.util.Random;
import java.nio.FloatBuffer;

public class Star {
    private static int program = -1;
    private static int aPositionLocation;
    private static int aTexCoordLocation;
    private static int uTextureLocation;

    private int textureId;
    private float baseSize;
    public float x, y, z;
    public float angle;
    public float speed;
    public float alpha;

    private static final Random rand = new Random();

    public Star(int textureId) {
        Log.d("Star", "Constructor con textura: " + textureId + " es es el textureId");
        this.textureId = textureId;
        reset();
    }

    public Star() {
        Log.d("Star", "Constructor sin textura, textureId= 0 ");
        this.textureId = 0; // textura desactivada
        reset();
    }

    public static void release() {
        Log.d("Star", "release: ");
        if (program != -1) {
            Log.d("releaseStar","");
            GLES20.glDeleteProgram(program);
            program = -1;
        }
    }

    public void reset() {
        baseSize = 2.0f + rand.nextFloat() * 3.0f;
        angle = rand.nextFloat() * (float)(2 * Math.PI);
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
        x = radius * (float)Math.cos(angle) * perspective;
        y = radius * (float)Math.sin(angle) * perspective;
    }

    public void draw() {
        if (textureId <= 0) {
                // Modo punto (fallback seguro)
            Log.d("Star", "🌠 Dibujando estrella con texturaId=" + textureId);
            drawAsPoint();
        } else {
            // Modo textura
            Log.d("Star", "🌠 Dibujando estrella con PUNTO=" + textureId);
            drawWithTexture();
        }
    }

    private void drawAsPoint() {
        Log.d("Star", "drawAsPoint() dibujando como punto");
        if (program == -1) {
            program = ShaderUtils.createProgram(
                    "attribute vec4 a_Position;\n" +
                            "void main() {\n" +
                            "    gl_Position = a_Position;\n" +
                            "    gl_PointSize = 5.0;\n" +
                            "}",
                    "precision mediump float;\n" +
                            "void main() {\n" +
                            "    gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);\n" +
                            "}"
            );
            aPositionLocation = GLES20.glGetAttribLocation(program, "a_Position");
        }

        GLES20.glUseProgram(program);

        float[] point = { x, y, 0f };
        FloatBuffer pointBuffer = ShaderUtils.createFloatBuffer(point);

        GLES20.glEnableVertexAttribArray(aPositionLocation);
        GLES20.glVertexAttribPointer(aPositionLocation, 3, GLES20.GL_FLOAT, false, 0, pointBuffer);

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);

        GLES20.glDisableVertexAttribArray(aPositionLocation);
    }

    private void drawWithTexture() {
        Log.d("Star", "drawWithTexture dibujando como textura");
        if (textureId <= 0 || !GLES20.glIsTexture(textureId)) {
            Gatito.mensajito("dentro de drawithtexture , if, se dibuja como punto");
            drawAsPoint();
            Gatito.mensajito("texture id es: " + textureId);
            return;
        }
        Log.w("Star", "⚠️ Textura válida. textureId=" + textureId + ", glIsTexture=" + GLES20.glIsTexture(textureId));


        if (program == -1) {
            program = ShaderUtils.createProgram(StarShader.VERTEX_SHADER, StarShader.FRAGMENT_SHADER);
            aPositionLocation = GLES20.glGetAttribLocation(program, "a_Position");
            aTexCoordLocation = GLES20.glGetAttribLocation(program, "a_TexCoord");
            uTextureLocation = GLES20.glGetUniformLocation(program, "u_Texture");
        }

        GLES20.glUseProgram(program);

        float size = baseSize * (1.0f / z) * 0.1f;
        size = Math.max(0.01f, Math.min(size, 0.05f));

        float[] vertices = {
                x - size, y - size, 0f,
                x + size, y - size, 0f,
                x - size, y + size, 0f,
                x + size, y + size, 0f
        };

        float[] texCoords = {
                0f, 0f,
                1f, 0f,
                0f, 1f,
                1f, 1f
        };

        FloatBuffer vertexBuffer = ShaderUtils.createFloatBuffer(vertices);
        FloatBuffer texCoordBuffer = ShaderUtils.createFloatBuffer(texCoords);

        GLES20.glEnableVertexAttribArray(aPositionLocation);
        GLES20.glVertexAttribPointer(aPositionLocation, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(aTexCoordLocation);
        GLES20.glVertexAttribPointer(aTexCoordLocation, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTextureLocation, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(aPositionLocation);
        GLES20.glDisableVertexAttribArray(aTexCoordLocation);
    }
}