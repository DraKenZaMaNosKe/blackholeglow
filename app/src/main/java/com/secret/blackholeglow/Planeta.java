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
 * ====================================================================
 * Planeta
 * ====================================================================
 * Ahora con tiling de textura (uvScale) y wrap REPEAT para evitar bordes negros.
 */
public class Planeta extends BaseShaderProgram implements SceneObject {
    private static final String TAG = "Planeta";
    // Parámetros de configuración
    private final float uvScale;            // Factor de repetición de textura
    private final int   textureId;          // ID OpenGL de la textura

    // Resto de parámetros (igual que antes)...
    private final float orbitRadiusX, orbitRadiusZ, orbitSpeed;
    private final float scaleAmplitude, instanceScale, spinSpeed;
    private final boolean useSolidColor;
    private final float[] solidColor;
    private final float alpha;
    private final Float scaleOscPercent;

    // Uniform locations
    private final int aPosLoc, aTexLoc;
    private final int uTexLoc, uUseSolidColorLoc, uSolidColorLoc, uAlphaLoc;
    private final int uUvScaleLoc;         // <-- ubicación de nuestro nuevo uniform

    // Buffers y conteos
    private final FloatBuffer vertexBuffer, texCoordBuffer;
    private final ShortBuffer indexBuffer;
    private final int indexCount;

    // Estado dinámico
    private float orbitAngle = 0f, rotation = 0f, accumulatedTime = 0f;

    // Matrices temporales
    private final float[] model = new float[16], mv = new float[16], mvp = new float[16];

    // Constantes internas
    private static final float CAMERA_DISTANCE = 12f;
    private static final float BASE_SCALE      = 0.01f;
    private static final float SCALE_OSC_FREQ  = 0.2f;

    /**
     * Constructor extendido con uvScale al final.
     */
    public Planeta(Context ctx,
                   TextureManager texMgr,
                   String vertexShaderAssetPath,
                   String fragmentShaderAssetPath,
                   int textureResId,
                   float orbitRadiusX,
                   float orbitRadiusZ,
                   float orbitSpeed,
                   float scaleAmplitude,
                   float instanceScale,
                   float spinSpeed,
                   boolean useSolidColor,
                   float[] solidColor,
                   float alpha,
                   Float scaleOscPercent,
                   float uvScale) {
        super(ctx, vertexShaderAssetPath, fragmentShaderAssetPath);

        // Guardamos el factor de tiling
        this.uvScale        = uvScale;

        // Habilitamos profundidad y culling
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Cargamos la textura y forzamos wrap REPEAT
        textureId = texMgr.getTexture(textureResId);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_REPEAT
        );


        // Asignamos parámetros de movimiento y color
        this.orbitRadiusX   = orbitRadiusX;
        this.orbitRadiusZ   = orbitRadiusZ;
        this.orbitSpeed     = orbitSpeed;
        this.scaleAmplitude = scaleAmplitude;
        this.instanceScale  = instanceScale;
        this.spinSpeed      = spinSpeed;
        this.useSolidColor  = useSolidColor;
        this.solidColor     = (solidColor != null)
                ? solidColor
                : new float[]{1f,1f,1f,1f};
        this.alpha          = alpha;
        this.scaleOscPercent= scaleOscPercent;

        // Cargamos malla .obj
        ObjLoader.Mesh mesh;
        try {
            mesh = ObjLoader.loadObj(ctx, "planeta.obj");
        } catch (IOException e) {
            throw new RuntimeException("Error cargando planeta.obj", e);
        }
        vertexBuffer   = mesh.vertexBuffer;
        texCoordBuffer = mesh.uvBuffer;

        // Construimos índices
        List<short[]> faces = mesh.faces;
        int triCount = 0;
        for (short[] f: faces) triCount += f.length - 2;
        indexCount = triCount * 3;
        ShortBuffer ib = ByteBuffer
                .allocateDirect(indexCount * Short.BYTES)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        for (short[] f: faces) {
            short v0 = f[0];
            for (int i = 1; i < f.length-1; i++) {
                ib.put(v0).put(f[i]).put(f[i+1]);
            }
        }
        ib.position(0);
        indexBuffer = ib;

        // Uniforms y atributos GLSL
        aPosLoc           = GLES20.glGetAttribLocation(programId, "a_Position");
        aTexLoc           = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        uTexLoc           = GLES20.glGetUniformLocation(programId, "u_Texture");
        uUseSolidColorLoc = GLES20.glGetUniformLocation(programId, "u_UseSolidColor");
        uSolidColorLoc    = GLES20.glGetUniformLocation(programId, "u_SolidColor");
        uAlphaLoc         = GLES20.glGetUniformLocation(programId, "u_Alpha");
        uUvScaleLoc       = GLES20.glGetUniformLocation(programId, "u_UvScale"); // nuestro uniform
    }

    @Override
    public void update(float dt) {
        // Giro propio
        rotation = (rotation + dt * spinSpeed) % 360f;
        // Orbita
        if (orbitRadiusX>0 && orbitRadiusZ>0 && orbitSpeed>0) {
            orbitAngle = (orbitAngle + dt * orbitSpeed)
                    % (2f*(float)Math.PI);
        }
        accumulatedTime += dt;
    }

    @Override
    public void draw() {
        useProgram();

        // Fase de animación para shaders que lo usan
        float phase = (accumulatedTime % 0.5f)
                * 2f * (float)Math.PI / 0.5f;
        setTime(phase);

        // Enviamos el factor de tiling
        GLES20.glUniform1f(uUvScaleLoc, uvScale);

        // MVP ortográfico
        float aspect = (float)SceneRenderer.screenWidth
                / SceneRenderer.screenHeight;
        float h = 1f, w = h*aspect;
        float[] proj = new float[16], view = new float[16];
        Matrix.orthoM(proj,0, -w,w, -h,h, 0.1f,100f);
        Matrix.setLookAtM(
                view,0,
                0,0,CAMERA_DISTANCE,
                0,0,0,
                0,1,0
        );

        // Modelo: orbita, escala y rotación
        Matrix.setIdentityM(model,0);
        if (orbitRadiusX>0 && orbitRadiusZ>0) {
            float ox = orbitRadiusX*(float)Math.cos(orbitAngle);
            float oz = orbitRadiusZ*(float)Math.sin(orbitAngle);
            Matrix.translateM(model,0, ox,0, oz);
            float dyn = 1f + scaleAmplitude*(oz/orbitRadiusZ);
            float osc = 1f;
            if (scaleOscPercent!=null) {
                float s = 0.5f+0.5f*(float)Math.sin(
                        accumulatedTime * SCALE_OSC_FREQ * 2f * Math.PI
                );
                osc = scaleOscPercent + (1f-scaleOscPercent)*s;
            }
            float finalScale = BASE_SCALE
                    * instanceScale
                    * dyn
                    * osc;
            Matrix.scaleM(
                    model,0,
                    finalScale,finalScale,finalScale
            );
        } else {
            float osc=1f;
            if (scaleOscPercent!=null) {
                float s = 0.5f+0.5f*(float)Math.sin(
                        accumulatedTime * SCALE_OSC_FREQ * 2f * Math.PI
                );
                osc = scaleOscPercent + (1f-scaleOscPercent)*s;
            }
            float finalScale = BASE_SCALE * instanceScale * osc;
            Matrix.scaleM(
                    model,0,
                    finalScale,finalScale,finalScale
            );
        }
        Matrix.rotateM(model,0, rotation, 0,1,0);

        // MV y MVP
        Matrix.multiplyMM(mv,0, view,0, model,0);
        Matrix.multiplyMM(mvp,0, proj,0, mv,0);
        setMvpAndResolution(
                mvp,
                SceneRenderer.screenWidth,
                SceneRenderer.screenHeight
        );

        // Textura y color
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(
                GLES20.GL_TEXTURE_2D, textureId
        );
        GLES20.glUniform1i(uTexLoc, 0);
        GLES20.glUniform1i(
                uUseSolidColorLoc,
                useSolidColor?1:0
        );
        GLES20.glUniform4fv(
                uSolidColorLoc,1, solidColor,0
        );
        GLES20.glUniform1f(uAlphaLoc, alpha);

        // Atributos y dibujado
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(
                aPosLoc, 3, GLES20.GL_FLOAT,
                false, 0, vertexBuffer
        );
        texCoordBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aTexLoc);
        GLES20.glVertexAttribPointer(
                aTexLoc, 2, GLES20.GL_FLOAT,
                false, 0, texCoordBuffer
        );
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
