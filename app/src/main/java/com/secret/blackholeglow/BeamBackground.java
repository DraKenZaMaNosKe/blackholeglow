package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import com.secret.blackholeglow.util.ObjLoader;
import com.secret.blackholeglow.util.ObjLoader.Mesh;
import java.nio.*;
import java.util.List;

public class BeamBackground implements SceneObject {

    // Geometría
    private final Mesh mesh;
    private final ShortBuffer indexBuffer;
    private final int indexCount;

    // Shaders
    private final int program;
    private final int aPosLoc;
    private final int uMVPLoc;
    private final int uRevealLoc;
    private final int uHaloWidthLoc;

    // Matrices
    private final float[] proj  = new float[16];
    private final float[] view  = new float[16];
    private final float[] model = new float[16];
    private final float[] tmp   = new float[16];
    private final float[] mvp   = new float[16];

    // Parámetros de prueba para revelar y halo
    private float reveal    = 0.5f;  // 50% de altura
    private float haloWidth = 0.1f;  // 10% de ancho

    public BeamBackground(Context ctx, TextureManager ignore) {
        // Carga OBJ
        try {
            mesh = ObjLoader.loadObj(ctx, "beam.obj");
        } catch (Exception e) {
            throw new RuntimeException("Error cargando beam.obj", e);
        }
        // Triangulación fan
        List<short[]> faces = mesh.faces;
        int tris = faces.stream().mapToInt(f -> f.length - 2).sum();
        indexCount = tris * 3;
        ShortBuffer ib = ByteBuffer
                .allocateDirect(indexCount * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        for (short[] f : faces) {
            short i0 = f[0];
            for (int k = 1; k < f.length - 1; k++) {
                ib.put(i0).put(f[k]).put(f[k+1]);
            }
        }
        ib.position(0);
        indexBuffer = ib;

        // Compila tus shaders GLSL
        program = ShaderUtils.createProgramFromAssets(
                ctx,
                "shaders/beam_vertex.glsl",
                "shaders/beam_fragment.glsl"
        );
        aPosLoc       = GLES20.glGetAttribLocation(program, "a_Position");
        uMVPLoc       = GLES20.glGetUniformLocation(program, "u_MVP");
        uRevealLoc    = GLES20.glGetUniformLocation(program, "u_Reveal");
        uHaloWidthLoc = GLES20.glGetUniformLocation(program, "u_HaloWidth");

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    @Override
    public void update(float dt) {
        // Por si quieres animar el reveal:
        // reveal = Math.min(1f, reveal + dt * 0.2f);
    }

    @Override
    public void draw() {
        GLES20.glUseProgram(program);

        // 1) Proyección ortográfica centrada
        float aspect = (float)SceneRenderer.screenWidth / SceneRenderer.screenHeight;
        Matrix.orthoM(proj, 0, -aspect, aspect, -1f, 1f, 0.1f, 10f);
        Matrix.setLookAtM(view, 0,
                0f, 0f, 2f,
                0f, 0f, 0f,
                0f, 1f, 0f);

        // 2) Model matrix
        Matrix.setIdentityM(model, 0);
        // Mantener tamaño original de mesh
        float scale = 1f;
        Matrix.scaleM(model, 0, scale, scale, 1f);

        // 3) MVP final
        Matrix.multiplyMM(tmp, 0, view, 0, model, 0);
        Matrix.multiplyMM(mvp, 0, proj, 0, tmp,   0);
        GLES20.glUniformMatrix4fv(uMVPLoc, 1, false, mvp, 0);

        // 4) Uniforms fragment shader
        GLES20.glUniform1f(uRevealLoc, reveal);
        GLES20.glUniform1f(uHaloWidthLoc, haloWidth);

        // 5) Draw
        mesh.vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(
                aPosLoc, 3,
                GLES20.GL_FLOAT,
                false,
                3 * Float.BYTES,
                mesh.vertexBuffer
        );
        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES,
                indexCount,
                GLES20.GL_UNSIGNED_SHORT,
                indexBuffer
        );
        GLES20.glDisableVertexAttribArray(aPosLoc);
    }
}
