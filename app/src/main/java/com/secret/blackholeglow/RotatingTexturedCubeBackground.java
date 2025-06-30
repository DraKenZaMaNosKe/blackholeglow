package com.secret.blackholeglow;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * RotatingTexturedCubeBackground – ahora rellena cada “cuadrito” de la malla de esfera
 * con un color distinto, dibujando triángulos en lugar de líneas.
 */
public class RotatingTexturedCubeBackground implements SceneObject {
    private final int program;
    private final int aPosLoc;
    private final int uMVPMatrixLoc, uColorLoc;

    // Buffer de vértices
    private final FloatBuffer vBuffer;
    // Buffer de índices (6 índices por quad)
    private final ShortBuffer quadIdxBuffer;
    private final int quadCount;

    // Matrices
    private final float[] proj  = new float[16];
    private final float[] view  = new float[16];
    private final float[] model = new float[16];
    private final float[] tmp   = new float[16];
    private final float[] mvp   = new float[16];

    private float angle = 0f;

    public RotatingTexturedCubeBackground(TextureManager ignore) {
        // 1) Compilar shaders
        String vShader =
                "attribute vec4 a_Position;\n" +
                        "uniform mat4 u_MVP;\n" +
                        "void main(){ gl_Position = u_MVP * a_Position; }";
        String fShader =
                "precision mediump float;\n" +
                        "uniform vec4 u_Color;\n" +
                        "void main(){ gl_FragColor = u_Color; }";

        program       = ShaderUtils.createProgram(vShader, fShader);
        aPosLoc       = GLES20.glGetAttribLocation(program,  "a_Position");
        uMVPMatrixLoc = GLES20.glGetUniformLocation(program, "u_MVP");
        uColorLoc     = GLES20.glGetUniformLocation(program, "u_Color");

        // 2) Crear vértices de la esfera (lat/long)
        final int latBands  = 20;
        final int longBands = 20;
        final int latCount  = latBands + 1;
        final int lonCount  = longBands + 1;
        List<Float> verts = new ArrayList<>(latCount * lonCount * 3);
        for (int lat = 0; lat < latCount; lat++) {
            float theta = (float)(Math.PI * lat / latBands);
            float sinT  = (float)Math.sin(theta), cosT = (float)Math.cos(theta);
            for (int lon = 0; lon < lonCount; lon++) {
                float phi = (float)(2 * Math.PI * lon / longBands);
                float sinP = (float)Math.sin(phi), cosP = (float)Math.cos(phi);
                verts.add(sinT * cosP);
                verts.add(cosT);
                verts.add(sinT * sinP);
            }
        }
        float[] vArr = new float[verts.size()];
        for (int i = 0; i < vArr.length; i++) vArr[i] = verts.get(i);

        vBuffer = ByteBuffer
                .allocateDirect(vArr.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vArr);
        vBuffer.position(0);

        // 3) Construir índices por quad (dos triángulos)
        quadCount = latBands * longBands;
        short[] quadIdx = new short[quadCount * 6];
        int idxPos = 0;
        for (int lat = 0; lat < latBands; lat++) {
            for (int lon = 0; lon < longBands; lon++) {
                int a = lat * lonCount + lon;
                int b = a + 1;
                int c = a + lonCount;
                int d = c + 1;
                // triángulo 1: a, c, b
                quadIdx[idxPos++] = (short)a;
                quadIdx[idxPos++] = (short)c;
                quadIdx[idxPos++] = (short)b;
                // triángulo 2: b, c, d
                quadIdx[idxPos++] = (short)b;
                quadIdx[idxPos++] = (short)c;
                quadIdx[idxPos++] = (short)d;
            }
        }
        quadIdxBuffer = ByteBuffer
                .allocateDirect(quadIdx.length * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
                .put(quadIdx);
        quadIdxBuffer.position(0);

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    @Override
    public void update(float dt) {
        angle += 30f * dt;
        if (angle > 360f) angle -= 360f;
    }

    @Override
    public void draw() {
        // fondo negro
        GLES20.glClearColor(0f,0f,0f,1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glUseProgram(program);

        // cámara + proyección
        float aspect = (float)SceneRenderer.screenWidth / SceneRenderer.screenHeight;
        Matrix.perspectiveM(proj, 0, 45, aspect, 1f, 20f);
        Matrix.setLookAtM(view, 0,
                0f,0f,-4f,
                0f,0f, 0f,
                0f,1f, 0f);

        // modelo: rotar esfera
        Matrix.setIdentityM(model, 0);
        Matrix.rotateM(model, 0, angle, 0f,1f,0f);

        // MVP
        Matrix.multiplyMM(tmp, 0, view, 0, model, 0);
        Matrix.multiplyMM(mvp, 0, proj, 0, tmp, 0);
        GLES20.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, mvp, 0);

        // atributos
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(aPosLoc,
                3, GLES20.GL_FLOAT, false,
                3 * 4, vBuffer);

        // dibujar cada quad con un color distinto
        for (int q = 0; q < quadCount; q++) {
            // un gradiente sencillo en función del índice
            float t = q / (float)(quadCount - 1);
            GLES20.glUniform4f(uColorLoc,
                    0.5f + 0.5f * (float)Math.sin(t * Math.PI * 2),
                    0.5f + 0.5f * (float)Math.cos(t * Math.PI * 2),
                    0.3f, 1f);

            quadIdxBuffer.position(q * 6);
            GLES20.glDrawElements(
                    GLES20.GL_TRIANGLES,
                    6,
                    GLES20.GL_UNSIGNED_SHORT,
                    quadIdxBuffer
            );
        }

        GLES20.glDisableVertexAttribArray(aPosLoc);
    }
}
