// Planeta.java
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
 * Representa un planeta texturizado:
 *  - Carga mesh desde planeta.obj
 *  - Calcula su órbita y giro
 *  - Usa un fragment shader con pulso de brillo cada 0.5s
 */
public class Planeta extends BaseShaderProgram implements SceneObject {
    // TAG para logs
    private static final String TAG = "Planeta";

    // Constantes de cámara y escala
    private static final float CAMERA_DISTANCE = 12f;
    private static final float BASE_SCALE      = 0.01f;

    // Buffers de vértices, UVs e índices
    private final FloatBuffer  vertexBuffer;
    private final FloatBuffer  texCoordBuffer;
    private final ShortBuffer  indexBuffer;
    private final int          indexCount;

    // Ubicaciones GLSL
    private final int aPosLoc, aTexLoc, uTexLoc;

    // ID de textura
    private final int textureId;

    // Parámetros de movimiento y escala
    private final float orbitRadiusX, orbitRadiusZ, orbitSpeed;
    private final float scaleAmplitude;
    private final float instanceScale;
    private final float spinSpeed;

    // Estado dinámico
    private float orbitAngle      = 0f;
    private float rotation        = 0f;
    private float accumulatedTime = 0f;

    // Matrices temporales
    private final float[] model = new float[16];
    private final float[] mv    = new float[16];
    private final float[] mvp   = new float[16];

    /**
     * @param ctx            Contexto de Android
     * @param texMgr         Gestor de texturas
     * @param orbitRadiusX   Radio de órbita en X
     * @param orbitRadiusZ   Radio de órbita en Z
     * @param orbitSpeed     Velocidad de órbita (rad/s)
     * @param scaleAmplitude Amplitud de escala dinámica
     * @param instanceScale  Escala base del planeta
     * @param spinSpeed      Velocidad de giro propio (°/s)
     */
    public Planeta(Context ctx,
                   TextureManager texMgr,
                   float orbitRadiusX,
                   float orbitRadiusZ,
                   float orbitSpeed,
                   float scaleAmplitude,
                   float instanceScale,
                   float spinSpeed) {
        super(ctx,
                "shaders/planeta_vertex.glsl",
                "shaders/planeta_fragment.glsl");

        // Habilita depth test & culling
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Localiza atributos y uniforms
        aPosLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        aTexLoc = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        uTexLoc = GLES20.glGetUniformLocation(programId, "u_Texture");

        // Carga la textura del planeta
        textureId = texMgr.getTexture(R.drawable.textura_planeta_marciano);

        // Carga el mesh desde OBJ
        ObjLoader.Mesh mesh;
        try {
            mesh = ObjLoader.loadObj(ctx, "planeta.obj");
            Log.d(TAG, "Mesh cargado: v=" + mesh.vertexCount +
                    " f=" + mesh.faces.size());
        } catch (IOException e) {
            throw new RuntimeException("Error cargando planeta.obj", e);
        }
        vertexBuffer   = mesh.vertexBuffer;
        texCoordBuffer = mesh.uvBuffer;

        // Construye buffer de índices (triangulación)
        List<short[]> faces = mesh.faces;
        int triCount = 0;
        for (short[] f : faces) triCount += f.length - 2;
        indexCount = triCount * 3;
        ShortBuffer ib = ByteBuffer
                .allocateDirect(indexCount * Short.BYTES)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        for (short[] f : faces) {
            short v0 = f[0];
            for (int i = 1; i < f.length - 1; i++) {
                ib.put(v0).put(f[i]).put(f[i + 1]);
            }
        }
        ib.position(0);
        indexBuffer = ib;

        // Asigna parámetros
        this.orbitRadiusX  = orbitRadiusX;
        this.orbitRadiusZ  = orbitRadiusZ;
        this.orbitSpeed    = orbitSpeed;
        this.scaleAmplitude= scaleAmplitude;
        this.instanceScale = instanceScale;
        this.spinSpeed     = spinSpeed;
    }

    @Override
    public void update(float dt) {
        // Giro propio y órbita
        rotation = (rotation + dt * spinSpeed) % 360f;
        if (orbitRadiusX > 0f && orbitRadiusZ > 0f && orbitSpeed > 0f) {
            orbitAngle = (orbitAngle + dt * orbitSpeed)
                    % (2f * (float)Math.PI);
        }
        accumulatedTime += dt;
        Log.d(TAG, String.format(
                "update() dt=%.3f  rot=%.1f°  orbit=%.2frad",
                dt, rotation, orbitAngle
        ));
    }

    @Override
    public void draw() {
        useProgram();

        // ——— 1) Calcula fase del pulso (0…2π cada 0.5s) ———
        float period = 0.5f;                  // #PULSE_PERIOD#
        float twoPi  = (float)(Math.PI * 2);
        float tPhase = accumulatedTime % period;
        float wrappedPhase = tPhase * twoPi / period;
        setTime(wrappedPhase);

        // ——— 2) Calcula matrices MVP ———
        float aspect = (float)SceneRenderer.screenWidth
                / SceneRenderer.screenHeight;
        float h = 1f, w = h * aspect;
        float[] proj = new float[16], view = new float[16];
        Matrix.orthoM(proj, 0, -w, w, -h, h, 0.1f, 100f);
        Matrix.setLookAtM(view, 0,
                0f, 0f, CAMERA_DISTANCE,
                0f, 0f, 0f,
                0f, 1f, 0f
        );

        Matrix.setIdentityM(model, 0);
        if (orbitRadiusX>0f && orbitRadiusZ>0f) {
            float ox = orbitRadiusX * (float)Math.cos(orbitAngle);
            float oz = orbitRadiusZ * (float)Math.sin(orbitAngle);
            Matrix.translateM(model, 0, ox, 0f, oz);
            float dyn = 1f + scaleAmplitude * (oz/orbitRadiusZ);
            Matrix.scaleM(model, 0,
                    BASE_SCALE * instanceScale * dyn,
                    BASE_SCALE * instanceScale * dyn,
                    BASE_SCALE * instanceScale * dyn
            );
        } else {
            Matrix.scaleM(model, 0,
                    BASE_SCALE * instanceScale,
                    BASE_SCALE * instanceScale,
                    BASE_SCALE * instanceScale
            );
        }
        Matrix.rotateM(model, 0, rotation, 0f, 1f, 0f);
        Matrix.multiplyMM(mv,  0, view,  0, model, 0);
        Matrix.multiplyMM(mvp, 0, proj,  0, mv,    0);

        // ——— 3) Envía MVP + resolución ———
        setMvpAndResolution(
                mvp,
                SceneRenderer.screenWidth,
                SceneRenderer.screenHeight
        );

        // ——— 4) Bind de textura y atributos ———
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(
                GLES20.GL_TEXTURE_2D, textureId
        );
        GLES20.glUniform1i(uTexLoc, 0);

        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(
                aPosLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer
        );
        texCoordBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aTexLoc);
        GLES20.glVertexAttribPointer(
                aTexLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer
        );

        // ——— 5) Dibuja el mesh ———
        indexBuffer.position(0);
        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES, indexCount,
                GLES20.GL_UNSIGNED_SHORT, indexBuffer
        );

        GLES20.glDisableVertexAttribArray(aPosLoc);
        GLES20.glDisableVertexAttribArray(aTexLoc);
    }
}
