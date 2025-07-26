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
import java.util.List;

/**
 * Clase Planeta
 *
 * Dibuja el wireframe de la esfera importada de Blender,
 * la hace rotar sobre su eje Y, y además la hace orbitar
 * indefinidamente siguiendo una trayectoria elíptica.
 * Su tamaño varía en función de su posición en Z (simulando
 * perspectiva) y siempre escala al 50% ± un 50% extra.
 */
public class Planeta
        extends BaseShaderProgram
        implements SceneObject {

    private static final String TAG = "Planeta";

    /** Buffer de vértices XYZ de la malla */
    private final FloatBuffer vertexBuffer;
    /** Buffer de índices para dibujar las aristas (LINES) */
    private final ShortBuffer wireframeBuffer;
    /** Total de índices en wireframeBuffer */
    private final int wireframeIndexCount;
    /** Localización del atributo a_Position en el shader */
    private final int aPosLoc;

    // Matrices temporales para calcular MVP = P * V * M
    private final float[] model = new float[16];
    private final float[] mv    = new float[16];
    private final float[] mvp   = new float[16];

    /** Ángulo de rotación de la propia esfera (grados) */
    private float rotation   = 0f;
    /** Ángulo actual de la órbita (radianes) */
    private float orbitAngle = 0f;
    /** Último delta-time recibido */
    private float lastDt     = 0f;

    // Parámetros de la órbita
    private static final float ORBIT_SPEED      = 1.0f;   // rad/s
    private static final float ORBIT_RADIUS_X   = 1.0f;   // Radio elipse X
    private static final float ORBIT_RADIUS_Z   = 0.6f;   // Radio elipse Z

    // Escala base y amplitud extra según Z
    private static final float BASE_SCALE       = 0.5f;   // 50% del espacio
    private static final float SCALE_AMPLITUDE  = 0.5f;   // ±50%

    /**
     * Constructor:
     * - Compila shaders.
     * - Habilita depth-test y wireframe.
     * - Carga OBJ y genera buffer de aristas.
     */
    public Planeta(Context ctx) {
        super(ctx,
                "shaders/planeta_vertex.glsl",
                "shaders/planeta_fragment.glsl");

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);

        // Obtiene la ubicación del atributo de posición
        aPosLoc = GLES20.glGetAttribLocation(programId, "a_Position");

        // Carga la malla de planeta.obj
        ObjLoader.Mesh mesh;
        try {
            mesh = ObjLoader.loadObj(ctx, "planeta.obj");
            Log.d(TAG, "Planeta cargado: vértices=" + mesh.vertexCount +
                    ", caras=" + mesh.faces.size());
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar planeta.obj", e);
        }
        vertexBuffer = mesh.vertexBuffer;

        // Cuenta triángulos para aristas
        List<short[]> faces = mesh.faces;
        int triCount = 0;
        for (short[] f : faces) triCount += f.length - 2;

        // Cada triángulo → 3 aristas × 2 índices
        wireframeIndexCount = triCount * 3 * 2;
        wireframeBuffer = ByteBuffer
                .allocateDirect(wireframeIndexCount * Short.BYTES)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();

        // Rellena buffer con pares de índices para LINES
        for (short[] f : faces) {
            short v0 = f[0];
            for (int k = 1; k < f.length - 1; k++) {
                short v1 = f[k], v2 = f[k+1];
                wireframeBuffer.put(v0).put(v1);
                wireframeBuffer.put(v1).put(v2);
                wireframeBuffer.put(v2).put(v0);
            }
        }
        wireframeBuffer.position(0);
    }

    /**
     * Actualiza:
     * - La rotación propia.
     * - El ángulo de la órbita.
     */
    @Override
    public void update(float dt) {
        lastDt     = dt;
        rotation   = (rotation + dt * 20f) % 360f;
        orbitAngle = (orbitAngle + dt * ORBIT_SPEED) % (2 * (float)Math.PI);
    }

    /**
     * Dibuja el planeta en:
     * - Proyección ortográfica frontal.
     * - Órbita elíptica centrada.
     * - Escala dinámica según Z.
     * - Wireframe.
     */
    @Override
    public void draw() {
        useProgram();

        // 1) Proyección ortográfica
        float aspect = (float)SceneRenderer.screenWidth / SceneRenderer.screenHeight;
        float h = 1f, w = h * aspect;
        float[] proj = new float[16];
        Matrix.orthoM(proj, 0, -w, w, -h, h, 0.1f, 100f);

        // 2) Vista: ojo en (0,0,3)
        float[] view = new float[16];
        Matrix.setLookAtM(view, 0,
                0f,0f,3f,    // eye
                0f,0f,0f,    // center
                0f,1f,0f);   // up

        // 3) Modelo: traslación orbital + escala + rotación
        //    a) posición en XZ según elipse
        float ox = ORBIT_RADIUS_X * (float)Math.cos(orbitAngle);
        float oz = ORBIT_RADIUS_Z * (float)Math.sin(orbitAngle);

        //    b) escala dinámica: base + amplitud·(oz / radioZ)
        float dynamicScale = BASE_SCALE +
                SCALE_AMPLITUDE * (oz / ORBIT_RADIUS_Z);

        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, ox, 0f, oz);
        Matrix.scaleM(model,     0,
                dynamicScale,
                dynamicScale,
                dynamicScale);
        Matrix.rotateM(model,    0,
                rotation,
                0f,1f,0f);

        // 4) MVP = proj * view * model
        Matrix.multiplyMM(mv,  0, view,  0, model, 0);
        Matrix.multiplyMM(mvp, 0, proj,  0, mv,    0);

        // 5) Enviar MVP al shader
        int uMvpLoc = GLES20.glGetUniformLocation(programId, "u_MVP");
        GLES20.glUniformMatrix4fv(uMvpLoc, 1, false, mvp, 0);

        // 6) Preparar atributo posición
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(
                aPosLoc, 3, GLES20.GL_FLOAT,
                false, 0, vertexBuffer
        );

        // 7) Dibujar wireframe
        wireframeBuffer.position(0);
        GLES20.glDrawElements(
                GLES20.GL_LINES,
                wireframeIndexCount,
                GLES20.GL_UNSIGNED_SHORT,
                wireframeBuffer
        );

        // 8) Limpiar estado
        GLES20.glDisableVertexAttribArray(aPosLoc);
    }
}
