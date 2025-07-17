// BlenderCubeBackground.java
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
 * BlenderCubeBackground:
 * Carga un cubo exportado desde Blender (.obj) y lo dibuja rotándolo lentamente,
 * usando un shader que da un color distinto a cada cara cada INTERVALO segundos.
 */
public class BlenderCubeBackground implements SceneObject {
    // --- Geometría y buffers ---
    private final Mesh        mesh;
    private final ShortBuffer indexBuffer;
    private final int         indexCount;

    // --- IDs de programa y atributos/uniforms ---
    private final int program;
    private final int aPosLoc;       // atributo posición
    private final int uMVPLoc;       // uniform mat4 MVP
    private final int uTimeLoc;      // uniform float u_Time
    private final int uIntervalLoc;  // uniform float u_Interval

    // --- Matrices de cámara/modelo ---
    private final float[] proj  = new float[16];
    private final float[] view  = new float[16];
    private final float[] model = new float[16];
    private final float[] tmp   = new float[16];
    private final float[] mvp   = new float[16];

    // --- Rotación y control de tiempo para shader ---
    private float angle = 0f;                 // ángulo de rotación (grados)
    private final long startTimeNs;           // para calcular u_Time
    private static final float INTERVAL = 2f; // segundos entre cambio de color

    public BlenderCubeBackground(Context ctx, TextureManager ignore) {
        // 1) Guardamos el instante de inicio para uniform u_Time
        startTimeNs = System.nanoTime();

        // 2) Cargo la malla desde assets/cubo.obj
        try {
            mesh = ObjLoader.loadObj(ctx, "cubo.obj");
        } catch (Exception e) {
            throw new RuntimeException("❌ Error cargando cubo.obj", e);
        }

        // 3) Triangulo las caras (fan triangulation)
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

        // 4) Compilo los shaders desde assets/shaders/
        program = ShaderUtils.createProgramFromAssets(
                ctx,
                "shaders/cube_vertex.glsl",
                "shaders/cube_fragment.glsl"
        );

        // 5) Localizo atributos y uniforms en el programa
        aPosLoc      = GLES20.glGetAttribLocation(program, "a_Position");
        uMVPLoc      = GLES20.glGetUniformLocation(program, "u_MVP");
        uTimeLoc     = GLES20.glGetUniformLocation(program, "u_Time");
        uIntervalLoc = GLES20.glGetUniformLocation(program, "u_Interval");

        // 6) Habilito depth test para que se vea correctamente 3D
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    @Override
    public void update(float dt) {
        // Aumento el ángulo de rotación en 30°/s
        angle += dt * 30f;
        if (angle > 360f) angle -= 360f; // wrap-around opcional
    }

    @Override
    public void draw() {
        GLES20.glUseProgram(program);

        // — Uniforms de tiempo para el shader de color —
        float timeSec = (System.nanoTime() - startTimeNs) * 1e-9f;
        GLES20.glUniform1f(uTimeLoc, timeSec);
        GLES20.glUniform1f(uIntervalLoc, INTERVAL);

        // — Calcular y enviar la matriz MVP —

        // Proyección perspectiva (FOV 45°, aspect, near=1, far=10)
        float aspect = (float) SceneRenderer.screenWidth / SceneRenderer.screenHeight;
        Matrix.perspectiveM(proj, 0, 45f, aspect, 1f, 10f);

        // Cámara fija (ojo en (0,2,4), mira al origen)
        Matrix.setLookAtM(view, 0,
                0f, 2f, 4f,
                0f, 0f, 0f,
                0f, 1f, 0f);

        // Modelo: aplico rotación Y usando el ángulo acumulado
        Matrix.setIdentityM(model, 0);
        Matrix.rotateM(model, 0, angle, 0f, 1f, 0f);

        // MVP = proj * view * model
        Matrix.multiplyMM(tmp, 0, view,  0, model,  0);
        Matrix.multiplyMM(mvp, 0, proj,  0, tmp,    0);
        GLES20.glUniformMatrix4fv(uMVPLoc, 1, false, mvp, 0);

        // — Dibujar la malla del cubo —
        mesh.vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(
                aPosLoc,
                3,
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
