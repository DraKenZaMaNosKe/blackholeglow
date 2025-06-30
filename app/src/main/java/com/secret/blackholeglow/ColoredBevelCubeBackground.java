package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.secret.blackholeglow.util.ObjLoader;
import com.secret.blackholeglow.util.ObjLoader.MeshWithMaterials;
import com.secret.blackholeglow.util.ObjLoader.SubMesh;

public class ColoredBevelCubeBackground implements SceneObject {
    private final MeshWithMaterials mesh;
    private final int program, aPosLoc, uMVPLoc, uColorLoc;
    private final float[] proj  = new float[16];
    private final float[] view  = new float[16];
    private final float[] model = new float[16];
    private final float[] tmp   = new float[16];
    private final float[] mvp   = new float[16];
    private float angle = 0f;

    public ColoredBevelCubeBackground(Context ctx, TextureManager ignore) {
        try {
            mesh = ObjLoader.loadMeshWithMaterials(ctx,
                    "bevel_cube.obj", "bevel_cube.mtl");
        } catch (Exception e) {
            throw new RuntimeException("Error cargando bevel_cube.obj/mtl", e);
        }

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
    }

    @Override
    public void update(float dt) {
        angle = (angle + dt * 20f) % 360f;
    }

    @Override
    public void draw() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT
                | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(program);

        // Cámara y proyección
        Matrix.setLookAtM(view,0,
                0f,0f,5f,   0f,0f,0f,   0f,1f,0f);
        float aspect = (float)SceneRenderer.screenWidth
                / SceneRenderer.screenHeight;
        Matrix.perspectiveM(proj,0,45,aspect,1f,10f);

        // Modelo: escala + rotación
        Matrix.setIdentityM(model,0);
        Matrix.scaleM(model,0,0.8f,0.8f,0.8f);
        Matrix.rotateM(model,0,angle,0f,1f,0f);

        Matrix.multiplyMM(tmp,0, view,0, model,0);
        Matrix.multiplyMM(mvp,0, proj,0, tmp,0);
        GLES20.glUniformMatrix4fv(uMVPLoc,1,false,mvp,0);

        // Dibujar cada sub-malla con su color
        for (SubMesh sm : mesh.subMeshes) {
            GLES20.glUniform4f(uColorLoc,
                    sm.color[0], sm.color[1], sm.color[2], 1f);
            GLES20.glEnableVertexAttribArray(aPosLoc);
            GLES20.glVertexAttribPointer(
                    aPosLoc, 3, GLES20.GL_FLOAT, false,
                    3*4, mesh.vertexBuffer
            );
            GLES20.glDrawElements(
                    GLES20.GL_TRIANGLES,
                    sm.indexCount,
                    GLES20.GL_UNSIGNED_SHORT,
                    sm.indexBuffer
            );
        }
        GLES20.glDisableVertexAttribArray(aPosLoc);
    }
}
