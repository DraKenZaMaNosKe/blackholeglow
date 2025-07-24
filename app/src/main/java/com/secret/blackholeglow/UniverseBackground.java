package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * UniverseBackground.java – Fondo animado de universo basado en GLSL fragment shader.
 */
public class UniverseBackground extends BaseShaderProgram implements SceneObject, CameraAware {

    private final FloatBuffer vertexBuffer;
    private final int aPosLoc;
    private CameraController camera;
    private float lastDeltaTime;

    private static final float[] QUAD_COORDS = {
            -1f, -1f,
            1f, -1f,
            -1f,  1f,
            1f,  1f
    };

    public UniverseBackground(Context ctx, TextureManager texMgr) {
        super(ctx, "shaders/universe_vertex.glsl", "shaders/universe_fragment.glsl");
        // Buffer del quad
        vertexBuffer = ByteBuffer
                .allocateDirect(QUAD_COORDS.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(QUAD_COORDS);
        vertexBuffer.position(0);

        aPosLoc = GLES20.glGetAttribLocation(programId, "a_Position");
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    @Override
    public void update(float dt) {
        lastDeltaTime = dt;
    }

    @Override
    public void draw() {
        useProgram();

        // MVP identidad (no cámara real)
        float[] mvp = new float[16];
        Matrix.setIdentityM(mvp, 0);
        setCommonUniforms(lastDeltaTime, mvp,
                SceneRenderer.screenWidth,
                SceneRenderer.screenHeight);

        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(aPosLoc, 2, GLES20.GL_FLOAT,
                false, 2*4, vertexBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(aPosLoc);
    }
}
