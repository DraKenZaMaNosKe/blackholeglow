package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.secret.blackholeglow.util.ObjLoader;
import com.secret.blackholeglow.util.ObjLoader.Mesh;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Dibuja un plano subdividido y anima solo un vértice seleccionado en Z.
 */
public class DeformableCubeBackground implements SceneObject {
    private final Mesh mesh;
    private final ShortBuffer edgeBuffer;
    private final int edgeCount;

    private final int program;
    private final int aPosLoc;
    private final int uMVPLoc;
    private final int uColorLoc;

    private final float[] proj  = new float[16];
    private final float[] view  = new float[16];
    private final float[] model = new float[16];
    private final float[] tmp   = new float[16];
    private final float[] mvp   = new float[16];

    // Solo un vértice animado
    private final int movingIndex;
    private final float originalZ;
    private final float phaseOffset;
    private final Random random = new Random();
    private float time = 0f;

    public DeformableCubeBackground(Context ctx, TextureManager ignore) {
        try {
            mesh = ObjLoader.loadObj(ctx, "cube.obj");
        } catch (Exception e) {
            throw new RuntimeException("Error cargando plane.obj", e);
        }

        // Construir aristas únicas como antes
        Set<Long> edges = new HashSet<>();
        for (short[] face : mesh.faces) {
            int n = face.length;
            for (int i = 0; i < n; i++) {
                short a = face[i];
                short b = face[(i+1)%n];
                int v0 = Math.min(a, b);
                int v1 = Math.max(a, b);
                edges.add((((long)v0)<<16) | (v1 & 0xFFFF));
            }
        }
        edgeCount = edges.size() * 2;
        ShortBuffer eb = ByteBuffer
                .allocateDirect(edgeCount * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        for (Long code : edges) {
            eb.put((short)(code >> 16)).put((short)(code & 0xFFFF));
        }
        eb.position(0);
        edgeBuffer = eb;

        // Shaders
        String vShader =
                "attribute vec4 a_Position;\n" +
                        "uniform mat4 u_MVP;\n" +
                        "void main(){ gl_Position = u_MVP * a_Position; }";
        String fShader =
                "precision mediump float;\n" +
                        "uniform vec4 u_Color;\n" +
                        "void main(){ gl_FragColor = u_Color; }";

        program   = ShaderUtils.createProgram(vShader, fShader);
        aPosLoc   = GLES20.glGetAttribLocation(program, "a_Position");
        uMVPLoc   = GLES20.glGetUniformLocation(program, "u_MVP");
        uColorLoc = GLES20.glGetUniformLocation(program, "u_Color");
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Elegir un vértice en el centro de la malla
        int vCount = mesh.originalVertices.length / 3;
        movingIndex = vCount / 2 + (vCount % 2 == 0 ? vCount/10 : 0);
        originalZ   = mesh.originalVertices[movingIndex*3 + 2];
        phaseOffset = random.nextFloat() * (float)Math.PI * 2f;
    }

    @Override
    public void update(float dt) {
        time += dt;
        float amplitude = 0.15f;
        float frequency = 1.0f;

        // Clonamos vértices
        float[] verts = mesh.originalVertices.clone();
        // Modificamos solo el vértice seleccionado
        int idx = movingIndex * 3;
        verts[idx+2] = originalZ + amplitude * (float)Math.sin(time * frequency + phaseOffset);

        // Actualizamos el buffer
        mesh.vertexBuffer.position(0);
        mesh.vertexBuffer.put(verts);
        mesh.vertexBuffer.position(0);
    }

    @Override
    public void draw() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(program);

        // Cámara fija en perspectiva
        Matrix.setLookAtM(view,0, 2f,3f,5f, 0f,0f,0f, 0f,1f,0f);
        float aspect = (float)SceneRenderer.screenWidth / SceneRenderer.screenHeight;
        Matrix.perspectiveM(proj,0,45,aspect,1f,10f);
        Matrix.setIdentityM(model,0);

        Matrix.multiplyMM(tmp,0, view,0, model,0);
        Matrix.multiplyMM(mvp,0, proj,0, tmp,0);
        GLES20.glUniformMatrix4fv(uMVPLoc,1,false,mvp,0);

        GLES20.glUniform4f(uColorLoc,1f,1f,1f,1f);
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, 3*4, mesh.vertexBuffer);

        // Dibujar wireframe sin diagonales
        GLES20.glLineWidth(1.5f);
        GLES20.glDrawElements(GLES20.GL_LINES, edgeCount, GLES20.GL_UNSIGNED_SHORT, edgeBuffer);
        GLES20.glDisableVertexAttribArray(aPosLoc);
    }
}
