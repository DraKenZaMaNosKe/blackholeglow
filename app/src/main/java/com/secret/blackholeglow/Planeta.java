// src/main/java/com/secret/blackholeglow/Planeta.java
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
 * Planeta texturizado, con órbita elíptica opcional,
 * rotación propia y variación de escala según Z.
 *
 * Parámetros de instancia:
 *  • orbitRadiusX, orbitRadiusZ: radios de órbita
 *  • orbitSpeed:     velocidad angular de órbita (rad/s)
 *  • scaleAmplitude: amplitud de variación de escala (0 = nada)
 *  • instanceScale:  escala base (1.0 = 100% del espacio visible)
 *  • spinSpeed:      velocidad de rotación propia (grados/s)
 */
public class Planeta extends BaseShaderProgram implements SceneObject {
    private static final String TAG = "Planeta";

    // Cámara fija
    private static final float CAMERA_DISTANCE = 12f;
    // Escala base (ahora al 50% de antes)
    private static final float BASE_SCALE      = 0.01f;

    // Buffers de malla
    private final FloatBuffer vertexBuffer, texCoordBuffer;
    private final ShortBuffer indexBuffer;
    private final int         indexCount;

    // GLSL locations
    private final int aPosLoc, aTexLoc, uTexLoc;
    private final int textureId;

    // Parámetros de órbita
    private final float orbitRadiusX, orbitRadiusZ, orbitSpeed;
    private final float scaleAmplitude;
    // Parámetros de rotación propia
    private final float spinSpeed;
    // Escala base de esta instancia
    private final float instanceScale;

    // Estado
    private float orbitAngle      = 0f;
    private float rotation        = 0f;
    private float accumulatedTime = 0f;

    // Matrices de trabajo
    private final float[] model = new float[16];
    private final float[] mv    = new float[16];
    private final float[] mvp   = new float[16];

    /**
     * @param orbitRadiusX   radio X de la elipse (0 = sin órbita)
     * @param orbitRadiusZ   radio Z de la elipse (0 = sin órbita)
     * @param orbitSpeed     velocidad angular de órbita (rad/s)
     * @param scaleAmplitude variación de escala (0 = sin variación)
     * @param instanceScale  escala base de este planeta
     * @param spinSpeed      velocidad de rotación propia (grados/s)
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

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // GLSL locations
        aPosLoc   = GLES20.glGetAttribLocation(programId, "a_Position");
        aTexLoc   = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        uTexLoc   = GLES20.glGetUniformLocation(programId, "u_Texture");
        textureId = texMgr.getTexture(R.drawable.textura_planeta);

        // Carga OBJ
        ObjLoader.Mesh mesh;
        try {
            mesh = ObjLoader.loadObj(ctx, "planeta.obj");
            Log.d(TAG, "Planeta cargado: v=" + mesh.vertexCount +
                    " f=" + mesh.faces.size());
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar planeta.obj", e);
        }
        vertexBuffer   = mesh.vertexBuffer;
        texCoordBuffer = mesh.uvBuffer;

        // Triangulación
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
                ib.put(v0).put(f[i]).put(f[i+1]);
            }
        }
        ib.position(0);
        indexBuffer = ib;

        // Instancia parámetros
        this.orbitRadiusX  = orbitRadiusX;
        this.orbitRadiusZ  = orbitRadiusZ;
        this.orbitSpeed    = orbitSpeed;
        this.scaleAmplitude= scaleAmplitude;
        this.instanceScale = instanceScale;
        this.spinSpeed     = spinSpeed;
    }

    @Override
    public void update(float dt) {
        // Rotación propia siempre
        rotation += dt * spinSpeed;
        rotation %= 360f;

        // Órbita sólo si los radios y velocidad > 0
        if (orbitRadiusX > 0f && orbitRadiusZ > 0f && orbitSpeed > 0f) {
            orbitAngle = (orbitAngle + dt * orbitSpeed) % (2f * (float)Math.PI);
        }
        accumulatedTime += dt;
    }

    @Override
    public void draw() {
        useProgram();

        // 1) Proyección ortográfica frontal
        float aspect = (float)SceneRenderer.screenWidth / SceneRenderer.screenHeight;
        float h = 1f, w = h * aspect;
        float[] proj = new float[16], view = new float[16];
        Matrix.orthoM(proj, 0, -w, w, -h, h, 0.1f, 100f);
        Matrix.setLookAtM(view, 0,
                0f,0f,CAMERA_DISTANCE,
                0f,0f,0f,
                0f,1f,0f);

        // 2) Modelo
        Matrix.setIdentityM(model, 0);

        if (orbitRadiusX > 0f && orbitRadiusZ > 0f) {
            // 2a) Traslación elíptica
            float ox = orbitRadiusX * (float)Math.cos(orbitAngle);
            float oz = orbitRadiusZ * (float)Math.sin(orbitAngle);
            Matrix.translateM(model, 0, ox, 0f, oz);
            // 2b) Escala dinámica según Z
            float dyn = 1f + scaleAmplitude * (oz / orbitRadiusZ);
            Matrix.scaleM(model, 0,
                    BASE_SCALE * instanceScale * dyn,
                    BASE_SCALE * instanceScale * dyn,
                    BASE_SCALE * instanceScale * dyn);
        } else {
            // Planeta central: escala base
            Matrix.scaleM(model, 0,
                    BASE_SCALE * instanceScale,
                    BASE_SCALE * instanceScale,
                    BASE_SCALE * instanceScale);
        }

        // 2c) Rotación propia (SIEMPRE después de escalar)
        Matrix.rotateM(model, 0, rotation, 0f, 1f, 0f);

        // 3) MVP = proj * view * model
        Matrix.multiplyMM(mv,  0, view,  0, model, 0);
        Matrix.multiplyMM(mvp, 0, proj,  0, mv,    0);

        // 4) Uniforms comunes: u_Time, u_MVP, u_Resolution
        setCommonUniforms(
                accumulatedTime,
                mvp,
                SceneRenderer.screenWidth,
                SceneRenderer.screenHeight
        );

        // 5) Bind textura + atributos
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTexLoc, 0);

        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(aPosLoc,
                3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        texCoordBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aTexLoc);
        GLES20.glVertexAttribPointer(aTexLoc,
                2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

        // 6) Dibujar
        indexBuffer.position(0);
        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES,
                indexCount,
                GLES20.GL_UNSIGNED_SHORT,
                indexBuffer
        );

        GLES20.glDisableVertexAttribArray(aPosLoc);
        GLES20.glDisableVertexAttribArray(aTexLoc);
    }
}
