// BlenderCubeBackground.java
package com.secret.blackholeglow;

import static java.lang.Math.sin;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.util.ObjLoader;
import com.secret.blackholeglow.util.ObjLoader.Mesh;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.List;

/**
 * BlenderCubeBackground:
 * Carga un cubo exportado desde Blender (.obj) y lo dibuja rotándolo lentamente.
 */
public class BlenderCubeBackground implements SceneObject {
    private final int uTimeLoc; // <-- NUEVO
    private float time = 0f;    // <-- NUEVO
    private final Mesh mesh;
    private final ShortBuffer indexBuffer;
    private final int indexCount;

    private final int program;
    private final int aPosLoc;
    private final int uMVPLoc;
    private final int uColorLoc;

    private final float[] proj  = new float[16];
    private final float[] view  = new float[16];
    private final float[] model = new float[16];
    private final float[] tmp   = new float[16];
    private final float[] mvp   = new float[16];

    private float angle = 0f;

    public BlenderCubeBackground(Context ctx, TextureManager ignore) {
        // 1) Cargar malla del cubo
        try {
            mesh = ObjLoader.loadObj(ctx, "cubo.obj");
        } catch (Exception e) {
            throw new RuntimeException("❌ Error cargando cubo.obj", e);
        }
        // 2) Triangulación de caras (fan)
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

        // 3) Compilar shaders desde assets/shaders/
        program = ShaderUtils.createProgramFromAssets(
                ctx,
                "shaders/cube_vertex.glsl",
                "shaders/cube_fragment.glsl"
        );
        aPosLoc   = GLES20.glGetAttribLocation(program, "a_Position");
        uMVPLoc   = GLES20.glGetUniformLocation(program, "u_MVP");
        uColorLoc = GLES20.glGetUniformLocation(program, "u_Color");
        uTimeLoc  = GLES20.glGetUniformLocation(program, "u_Time"); // <-- NUEVO

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    @Override
    public void update(float dt) {
        // Rotación continua alrededor del eje Y
        angle += dt * 30f; // 30° por segundo
        time  += dt;       // Acumula tiempo para el shader
    }

    @Override
    public void draw() {
        GLES20.glUseProgram(program);
        // 1) Proyección perspectiva
        float aspect = (float) SceneRenderer.screenWidth / SceneRenderer.screenHeight;
        Matrix.perspectiveM(proj, 0, 45, aspect, 1f, 10f);
        // 2) Cámara fija
        Matrix.setLookAtM(view, 0,
                0f, 2f, 4f,   // ojo
                0f, 0f, 0f,   // centro
                0f, 1f, 0f);  // up
        // 3) Model matrix: rotación Y
        Matrix.setIdentityM(model, 0);
        Matrix.rotateM(model, 0, angle, 0f, 1f, 0f);
        // 4) MVP = proj * view * model
        Matrix.multiplyMM(tmp , 0, view , 0, model, 0);
        Matrix.multiplyMM(mvp , 0, proj , 0, tmp  , 0);
        GLES20.glUniformMatrix4fv(uMVPLoc, 1, false, mvp, 0);
        // 5) Color uniforme (blanco)
        GLES20.glUniform4f(uColorLoc, 1f, 1f, 1f, 1f);

        // Envía el tiempo al shader
        GLES20.glUniform1f(uTimeLoc, time);

        Log.d("blendercubebackground", "la variable utimeloc contiene: " + uTimeLoc + ", " +
                "y la variable time , contiene: " + time);

        float prueba = (float)((Math.sin(time) + 1.0) * 0.5f); // Rango 0 a 1
        Log.d("blendercubebackground", " la variable prueba contiene: " + prueba);

        // 6) Dibujar triángulos
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
