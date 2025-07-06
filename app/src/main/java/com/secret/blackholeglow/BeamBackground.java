package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import com.secret.blackholeglow.util.ObjLoader;
import com.secret.blackholeglow.util.ObjLoader.Mesh;
import java.nio.*;
import java.util.List;

public class BeamBackground implements SceneObject {

    // Estados de animación
    private enum State { WAITING, REVEAL, HOLD }
    private State state = State.WAITING;

    // Temporizadores
    private float timer = 0f;
    private float nextStrikeIn = 0f;
    private float stepTimer = 0f;

    // Pasos de revelado (0..10)
    private int revealStep = 0;
    private final float stepDuration = 0.1f;   // 0.1s por paso
    private final float holdTime     = 0.2f;

    // Posición X aleatoria del rayo
    private float strikePosX = 0f;

    // Geometría
    private final Mesh mesh;
    private final ShortBuffer indexBuffer;
    private final int indexCount;

    // Shaders
    private final int program;
    private final int aPosLoc, uMVPLoc, uColorLoc, uRevealLoc;

    // Matrizes
    private final float[] proj  = new float[16];
    private final float[] view  = new float[16];
    private final float[] model = new float[16];
    private final float[] tmp   = new float[16];
    private final float[] mvp   = new float[16];

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

        // Compila shaders de assets
        program = ShaderUtils.createProgramFromAssets(
                ctx,
                "shaders/beam_vertex.glsl",
                "shaders/beam_fragment.glsl"
        );
        aPosLoc    = GLES20.glGetAttribLocation(program, "a_Position");
        uMVPLoc    = GLES20.glGetUniformLocation(program, "u_MVP");
        uColorLoc  = GLES20.glGetUniformLocation(program, "u_Color");
        uRevealLoc = GLES20.glGetUniformLocation(program, "u_Reveal");

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        scheduleNextStrike();
    }

    private void scheduleNextStrike() {
        state = State.WAITING;
        timer = 0f;
        stepTimer = 0f;
        revealStep = 0;
        nextStrikeIn = 0.5f + (float)Math.random() * 1.5f;
        strikePosX = -0.5f + (float)Math.random();
    }

    @Override
    public void update(float dt) {
        timer += dt;
        switch (state) {
            case WAITING:
                if (timer >= nextStrikeIn) {
                    state = State.REVEAL;
                    timer = 0f;
                }
                break;
            case REVEAL:
                stepTimer += dt;
                while (stepTimer >= stepDuration && revealStep < 10) {
                    stepTimer -= stepDuration;
                    revealStep++;
                }
                if (revealStep >= 10) {
                    state = State.HOLD;
                    timer = 0f;
                }
                break;
            case HOLD:
                if (timer >= holdTime) {
                    scheduleNextStrike();
                }
                break;
        }
    }

    @Override
    public void draw() {
        GLES20.glUseProgram(program);

        // Proyección ortográfica y cámara
        float aspect = (float)SceneRenderer.screenWidth / SceneRenderer.screenHeight;
        Matrix.orthoM(proj, 0, -aspect, aspect, -1f, 1f, 0.1f, 10f);
        Matrix.setLookAtM(view, 0,
                0f, 0f, 2f,
                0f, 0f, 0f,
                0f, 1f, 0f);

        // Model matrix: translate X, scale, rotate
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, strikePosX, 0f, 0f);
        Matrix.scaleM(    model, 0, 0.5f, 0.5f, 0.5f);
        Matrix.rotateM(   model, 0, 90f, 0f, 0f, 1f);

        // MVP
        Matrix.multiplyMM(tmp, 0, view, 0, model, 0);
        Matrix.multiplyMM(mvp, 0, proj, 0, tmp,   0);
        GLES20.glUniformMatrix4fv(uMVPLoc, 1, false, mvp, 0);

        // Color
        GLES20.glUniform4f(uColorLoc, 0.2f, 0.6f, 1.0f, 1.0f);
        // Reveal uniform
        float reveal = revealStep / 10f;
        GLES20.glUniform1f(uRevealLoc, reveal);

        // Dibujar mesh
        mesh.vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT,
                false, 3 * Float.BYTES, mesh.vertexBuffer);
        int uRevealLoc    = GLES20.glGetUniformLocation(program, "u_Reveal");
        int uHaloWidthLoc = GLES20.glGetUniformLocation(program, "u_HaloWidth");
// revealStep es entero 0..10
        GLES20.glUniform1f(uRevealLoc, revealStep / 10f);
// Define anchura de halo, p.e. 0.1 (10%)
        GLES20.glUniform1f(uHaloWidthLoc, 0.1f);
        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES,
                indexCount,
                GLES20.GL_UNSIGNED_SHORT,
                indexBuffer);
        GLES20.glDisableVertexAttribArray(aPosLoc);
    }
}