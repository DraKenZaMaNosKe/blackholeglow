package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.secret.blackholeglow.util.ObjLoader;
import com.secret.blackholeglow.util.ObjLoader.Mesh;

public class DeformableCubeBackground implements SceneObject {
    private final Mesh mesh;
    private final int program, aPosLoc, uMVPLoc, uColorLoc;
    private final float[] proj  = new float[16];
    private final float[] view  = new float[16];
    private final float[] model = new float[16];
    private final float[] tmp   = new float[16];
    private final float[] mvp   = new float[16];
    private float time = 0f;

    public DeformableCubeBackground(Context ctx, TextureManager ignore) {
        // Carga el mesh desde assets/cube.obj
        try {
            mesh = ObjLoader.loadObj(ctx, "cube.obj");
        } catch (Exception e) {
            throw new RuntimeException("Error cargando cube.obj", e);
        }

        // Shaders básicos
        String vShader =
                "attribute vec4 a_Position;\n" +
                        "uniform mat4 u_MVP;\n" +
                        "void main() {\n" +
                        "    gl_Position = u_MVP * a_Position;\n" +
                        "}";
        String fShader =
                "precision mediump float;\n" +
                        "uniform vec4 u_Color;\n" +
                        "void main() {\n" +
                        "    gl_FragColor = u_Color;\n" +
                        "}";

        program   = ShaderUtils.createProgram(vShader, fShader);
        aPosLoc   = GLES20.glGetAttribLocation(program, "a_Position");
        uMVPLoc   = GLES20.glGetUniformLocation(program, "u_MVP");
        uColorLoc = GLES20.glGetUniformLocation(program, "u_Color");

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    @Override
    public void update(float dt) {
        time += dt;
        // Ejemplo: deformación oscilante en Y
        float amp = 0.1f, freq = 3f;
        float[] verts = mesh.originalVertices.clone();
        for (int i = 0; i < mesh.vertexCount; i++) {
            int idx = i * 3;
            float x = verts[idx], y = verts[idx+1], z = verts[idx+2];
            verts[idx+1] = y + (float)Math.sin(time * freq + x*5 + z*5) * amp;
        }
        // Actualiza el buffer
        mesh.vertexBuffer.position(0);
        mesh.vertexBuffer.put(verts);
        mesh.vertexBuffer.position(0);
    }

    @Override
    public void draw() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(program);

        // Cámara más alejada para ver todo el cubo
        Matrix.setLookAtM(view, 0,
                0f, 0f, 5f,   // ojo en Z=5
                0f, 0f, 0f,   // mira al origen
                0f, 1f, 0f    // up vector
        );
        float aspect = (float)SceneRenderer.screenWidth / SceneRenderer.screenHeight;
        Matrix.perspectiveM(proj, 0, 45, aspect, 1f, 20f);

        // Modelo con escala y rotación suave
        Matrix.setIdentityM(model, 0);
        Matrix.scaleM(model, 0, 0.5f, 0.5f, 0.5f);
        Matrix.rotateM(model, 0, time * 20f, 0f, 1f, 0f);

        Matrix.multiplyMM(tmp, 0, view,  0, model, 0);
        Matrix.multiplyMM(mvp, 0, proj,  0, tmp,   0);
        GLES20.glUniformMatrix4fv(uMVPLoc, 1, false, mvp, 0);

        GLES20.glUniform4f(uColorLoc, 0f, 1f, 1f, 1f);

        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(
                aPosLoc,
                3, GLES20.GL_FLOAT,
                false,
                3*4,
                mesh.vertexBuffer
        );

        GLES20.glLineWidth(2f);
        GLES20.glDrawElements(
                GLES20.GL_LINES,
                mesh.indexCount,
                GLES20.GL_UNSIGNED_SHORT,
                mesh.indexBuffer
        );
        GLES20.glDisableVertexAttribArray(aPosLoc);
    }
}
