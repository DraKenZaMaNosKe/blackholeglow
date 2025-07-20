// BlenderCubeBackground.java
package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.util.ObjLoader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class BlenderCubeBackground
        extends BaseShaderProgram
        implements SceneObject, CameraAware {
    private static final String TAG = "Depurando";

    // Buffers + counts
    private final FloatBuffer vertexBuffer;
    private final ShortBuffer indexBuffer;
    private final int indexCount;
    private final int[] faceIndexOffsets;
    private final int[] faceTriangleCounts;
    private final int faceCount;

    // attribute / color location
    private final int aPosLoc;
    private final int uColorLoc;

    private CameraController camera;
    private float rotationAngle = 0f;
    private float lastDeltaTime = 0f;

    public BlenderCubeBackground(Context ctx, TextureManager texMgr) {
        super(ctx,
                "shaders/cube_vertex.glsl",
                "shaders/cube_fragment.glsl");

        Log.d(TAG, "=== BLENDER CUBE: START constructor ===");

        ObjLoader.Mesh mesh;
        try {
            mesh = ObjLoader.loadObj(ctx, "cube.obj");
            Log.d(TAG, "Mesh loaded: vertexCount=" + mesh.vertexCount +
                    ", faces=" + mesh.faces.size());
        } catch (IOException e) {
            Log.e(TAG, "Error loading cube.obj", e);
            mesh = createEmptyMesh();
        }

        vertexBuffer = mesh.vertexBuffer;
        List<short[]> faces = mesh.faces;
        faceCount = faces.size();

        int totalTris = 0;
        for (short[] f : faces) totalTris += (f.length - 2);
        indexCount = totalTris * 3;

        ShortBuffer ib = ByteBuffer
                .allocateDirect(indexCount * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();

        faceIndexOffsets   = new int[faceCount];
        faceTriangleCounts = new int[faceCount];

        int offset = 0;
        for (int i = 0; i < faceCount; i++) {
            short[] f = faces.get(i);
            int triCount = f.length - 2;
            faceIndexOffsets[i]   = offset;
            faceTriangleCounts[i] = triCount;
            short i0 = f[0];
            for (int k = 1; k < f.length - 1; k++) {
                ib.put(i0).put(f[k]).put(f[k+1]);
            }
            offset += triCount * 3;
        }
        ib.position(0);
        indexBuffer = ib;
        Log.d(TAG, "Index count = " + indexCount);

        // localiza atributos / color
        aPosLoc   = GLES20.glGetAttribLocation(programId, "a_Position");
        uColorLoc = GLES20.glGetUniformLocation(programId, "u_Color");
        Log.d(TAG, "Locations → aPos: " + aPosLoc +
                " | uColor: " + uColorLoc);

        Log.d(TAG, "=== BLENDER CUBE: END constructor ===");
    }

    private ObjLoader.Mesh createEmptyMesh() {
        FloatBuffer vb = ByteBuffer.allocateDirect(0)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        return new ObjLoader.Mesh(vb, new float[0], Collections.emptyList(), vb, 0);
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    @Override
    public void update(float dt) {
        lastDeltaTime = dt;
        rotationAngle = (rotationAngle + 30f * dt) % 360f;
    }

    @Override
    public void draw() {
        if (camera == null) {
            Log.e(TAG, "CameraController no inyectado, draw abortado.");
            return;
        }

        useProgram();

        // 1) calcula modelo + MVP
        float[] model = new float[16];
        Matrix.setIdentityM(model, 0);
        Matrix.rotateM(model, 0, rotationAngle, 0f, 1f, 0f);
        float[] mvp = new float[16];
        camera.computeMvp(model, mvp);

        // 2) envía uniforms comunes (time, MVP, resolución)
        setCommonUniforms(
                lastDeltaTime,
                mvp,
                SceneRenderer.screenWidth,
                SceneRenderer.screenHeight
        );

        // 3) habilita vértices
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(
                aPosLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer
        );

        // 4) bucket de tiempo para cambio cada 0.5s
        int bucket = (int)((System.nanoTime() / 1_000_000_000f) / 0.5f);

        // 5) dibuja cara a cara con color pseudo-aleatorio
        for (int i = 0; i < faceCount; i++) {
            Random rnd = new Random(bucket * 31 + i);
            float r = rnd.nextFloat();
            float g = rnd.nextFloat();
            float b = rnd.nextFloat();
            GLES20.glUniform4f(uColorLoc, r, g, b, 1f);

            indexBuffer.position(faceIndexOffsets[i]);
            int tris = faceTriangleCounts[i];
            GLES20.glDrawElements(
                    GLES20.GL_TRIANGLES,
                    tris * 3,
                    GLES20.GL_UNSIGNED_SHORT,
                    indexBuffer
            );
        }

        GLES20.glDisableVertexAttribArray(aPosLoc);
    }
}
