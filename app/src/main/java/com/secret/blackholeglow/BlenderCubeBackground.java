package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.secret.blackholeglow.util.ObjLoader;
import com.secret.blackholeglow.util.ObjLoader.Mesh;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.List;

/**
 * Dibuja un cubo importado sin rotación propia, con cámara que se aleja.
 */
public class BlenderCubeBackground implements SceneObject, CameraAware {

    private CameraController camera;
    private final Mesh mesh;
    private final ShortBuffer indexBuffer;
    private final int indexCount;

    private final int program;
    private final int aPosLoc;
    private final int uMVPLoc;

    private final float[] model = new float[16];
    private final float[] mvp   = new float[16];

    public BlenderCubeBackground(Context ctx, TextureManager ignore) {
        // 1) Cargar malla
        try {
            mesh = ObjLoader.loadObj(ctx, "cubo.obj");
        } catch (Exception e) {
            throw new RuntimeException("❌ Error cargando cubo.obj", e);
        }

        // 2) Triangulación
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
                ib.put(i0).put(f[k]).put(f[k + 1]);
            }
        }
        ib.position(0);
        indexBuffer = ib;

        // 3) Programa (vertex + fragment) para cubo amarillo permanente
        program = ShaderUtils.createProgramFromAssets(
                ctx,
                "shaders/cube_vertex.glsl",
                "shaders/cube_fragment.glsl"
        );
        aPosLoc = GLES20.glGetAttribLocation(program, "a_Position");
        uMVPLoc = GLES20.glGetUniformLocation(program, "u_MVP");

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    @Override
    public void setCameraController(CameraController cam) {
        this.camera = cam;
        // Vista inicial
        cam.setView(
                0f, 2f, 4f,   // eye
                0f, 0f, 0f,   // center
                0f, 1f, 0f    // up
        );
        // Órbita en 12s y zoom en bucle en 20s hasta radio=10
        cam.startOrbit(12f);
        cam.startZoomLoop(20f, 10f);
    }

    @Override
    public void update(float dt) {
        // Actualiza la cámara cada frame
        camera.update(dt);
    }

    @Override
    public void draw() {
        GLES20.glUseProgram(program);

        // Modelo sin rotación
        Matrix.setIdentityM(model, 0);

        // Calcula MVP con la cámara
        camera.computeMvp(model, mvp);
        GLES20.glUniformMatrix4fv(uMVPLoc, 1, false, mvp, 0);

        // Dibujar cubo
        mesh.vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(
                aPosLoc, 3, GLES20.GL_FLOAT,
                false, 3 * Float.BYTES, mesh.vertexBuffer
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
