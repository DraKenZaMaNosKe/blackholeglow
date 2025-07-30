
// ============================================================
// Planeta.java
// ============================================================
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
 * ============================================================================
 * Planeta
 * ============================================================================
 * Objeto 3D que representa un planeta con:
 *  - Órbita elíptica (orbitRadiusX,Z + orbitSpeed)
 *  - Rotación propia (spinSpeed)
 *  - Pulsos de escala (scaleAmplitude)
 *  - Oscilación de escala entre scaleOscPercent y 1.0
 *  - Textura o color sólido (useSolidColor)
 *  - Transparencia global (alpha)
 *
 * Parámetros (rangos recomendados):
 *  - orbitRadiusX,Z: [0.0 .. 20.0]             (0 = estacionario)
 *  - orbitSpeed    : [0.0 .. 6.28 rad/s]       (≈ 2π rad/s)
 *  - scaleAmplitude: [0.0 .. 1.0]              (0 = sin pulsos)
 *  - instanceScale : [0.1 .. 5.0]              (escala base)
 *  - spinSpeed     : [0.0 .. 360 °/s]          (0 = sin giro)
 *  - useSolidColor : true/false                (textura vs color)
 *  - solidColor    : float[4] RGBA [0.0..1.0]   (null = blanco)
 *  - alpha         : [0.0 .. 1.0]              (0 = invisible)
 *  - scaleOscPercent: null o [0.0 .. 1.0)       (null=no oscilación)
 *
 * Velocidad de oscilación de escala definida en:
 *   private static final float SCALE_OSC_FREQ = 0.2f; // ciclos/s
 *   // PARA AJUSTAR VELOCIDAD: modificar SCALE_OSC_FREQ
 *   // Rango sugerido: 0.05f (muy lento) .. 1.0f (rápido)
 *   // Para 30% más lento: usar SCALE_OSC_FREQ * 0.7f
 */
public class Planeta extends BaseShaderProgram implements SceneObject {
    private static final String TAG = "Planeta";

    /** Distancia fija de la cámara al origen (para ortho). */
    private static final float CAMERA_DISTANCE   = 12f;
    /** Factor base de escala aplicado siempre. */
    private static final float BASE_SCALE        = 0.01f;
    /** Ciclos por segundo para oscilación de escala. */
    private static final float SCALE_OSC_FREQ    = 0.2f; // modificar aquí para ajustar velocidad
    // -------------------------------------------------------------------------

    // --- Buffers de geometría y conteo de índices ---
    private final FloatBuffer vertexBuffer;   // coordenadas XYZ
    private final FloatBuffer texCoordBuffer; // coordenadas UV
    private final ShortBuffer indexBuffer;    // índices para glDrawElements
    private final int indexCount;             // total de índices

    // --- Localizaciones de atributos y uniforms en shader GLSL ---
    private final int aPosLoc;
    private final int aTexLoc;
    private final int uTexLoc;
    private final int uUseSolidColorLoc;
    private final int uSolidColorLoc;
    private final int uAlphaLoc;

    // --- ID de textura cargada por TextureManager ---
    private final int textureId;

    // --- Parámetros configurables ---
    private final float orbitRadiusX;
    private final float orbitRadiusZ;
    private final float orbitSpeed;
    private final float scaleAmplitude;
    private final float instanceScale;
    private final float spinSpeed;
    private final boolean useSolidColor;
    private final float[] solidColor;
    private final float alpha;
    private final Float scaleOscPercent; // null=no oscilación; [0..1)=mínimo porcentual

    // --- Estado dinámico ---
    private float orbitAngle = 0f;
    private float rotation   = 0f;
    private float accumulatedTime = 0f;

    // --- Matrices de transformación ---
    private final float[] model = new float[16];
    private final float[] mv    = new float[16];
    private final float[] mvp   = new float[16];

    /**
     * Constructor completo para Planeta.
     *
     * @param ctx            Contexto Android (no nulo)
     * @param texMgr         TextureManager (no nulo)
     * @param orbitRadiusX   Radio órbita eje X [0..20]
     * @param orbitRadiusZ   Radio órbita eje Z [0..20]
     * @param orbitSpeed     Velocidad órbita rad/s [0..6.28]
     * @param scaleAmplitude Amplitud pulsos escala [0..1]
     * @param instanceScale  Escala base [0.1..5]
     * @param spinSpeed      Velocidad giro propio °/s [0..360]
     * @param useSolidColor  true=usar solidColor; false=textura
     * @param solidColor     RGBA [0..1] o null para blanco
     * @param alpha          Transparencia [0..1]
     * @param scaleOscPercent
     *        null = sin oscilación,
     *        [0.0..1.0)=porcentaje mínimo de escala (oscila entre este valor y 1.0)
     */
    public Planeta(Context ctx,
                   TextureManager texMgr,
                   float orbitRadiusX,
                   float orbitRadiusZ,
                   float orbitSpeed,
                   float scaleAmplitude,
                   float instanceScale,
                   float spinSpeed,
                   boolean useSolidColor,
                   float[] solidColor,
                   float alpha,
                   Float scaleOscPercent) {
        super(ctx, "shaders/planeta_vertex.glsl", "shaders/planeta_fragment.glsl");
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Obtener locations en shader
        aPosLoc           = GLES20.glGetAttribLocation(programId, "a_Position");
        aTexLoc           = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        uTexLoc           = GLES20.glGetUniformLocation(programId, "u_Texture");
        uUseSolidColorLoc = GLES20.glGetUniformLocation(programId, "u_UseSolidColor");
        uSolidColorLoc    = GLES20.glGetUniformLocation(programId, "u_SolidColor");
        uAlphaLoc         = GLES20.glGetUniformLocation(programId, "u_Alpha");

        // Cargar textura default
        textureId = texMgr.getTexture(R.drawable.textura_planetaold);

        // Carga malla desde OBJ
        ObjLoader.Mesh mesh;
        try {
            mesh = ObjLoader.loadObj(ctx, "planeta.obj");
        } catch (IOException e) {
            throw new RuntimeException("Error cargando planeta.obj", e);
        }
        vertexBuffer   = mesh.vertexBuffer;
        texCoordBuffer = mesh.uvBuffer;
        List<short[]> faces = mesh.faces;
        int triCount = 0; for (short[] f: faces) triCount += f.length-2;
        indexCount = triCount*3;
        ShortBuffer ib = ByteBuffer.allocateDirect(indexCount*Short.BYTES)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        for (short[] f: faces) {
            short v0 = f[0];
            for (int i=1; i<f.length-1; i++) ib.put(v0).put(f[i]).put(f[i+1]);
        }
        ib.position(0);
        indexBuffer = ib;

        // Asignar parámetros
        this.orbitRadiusX    = orbitRadiusX;
        this.orbitRadiusZ    = orbitRadiusZ;
        this.orbitSpeed      = orbitSpeed;
        this.scaleAmplitude  = scaleAmplitude;
        this.instanceScale   = instanceScale;
        this.spinSpeed       = spinSpeed;
        this.useSolidColor   = useSolidColor;
        this.solidColor      = solidColor != null ? solidColor : new float[]{1f,1f,1f,1f};
        this.alpha           = alpha;
        this.scaleOscPercent = scaleOscPercent;
    }

    @Override
    public void update(float dt) {
        // Rotación propia
        rotation = (rotation + dt * spinSpeed) % 360f;
        // Órbita si procede
        if (orbitRadiusX>0 && orbitRadiusZ>0 && orbitSpeed>0) {
            orbitAngle = (orbitAngle + dt * orbitSpeed) % (2f * (float)Math.PI);
        }
        // Acumular tiempo para pulsos/oscilaciones
        accumulatedTime += dt;
    }

    @Override
    public void draw() {
        useProgram();

        // Enviar fase al shader (opcional)
        float period = 0.5f;
        float phase  = (accumulatedTime % period) * 2f * (float)Math.PI / period;
        setTime(phase);

        // Configurar proyección ortográfica y vista estática
        float aspect = (float)SceneRenderer.screenWidth / SceneRenderer.screenHeight;
        float h = 1f, w = h * aspect;
        float[] proj = new float[16], view = new float[16];
        Matrix.orthoM(proj,0, -w, w, -h, h, 0.1f, 100f);
        Matrix.setLookAtM(view,0, 0,0,CAMERA_DISTANCE, 0,0,0, 0,1,0);

        // Identidad y traslación orbital
        Matrix.setIdentityM(model,0);
        if (orbitRadiusX>0 && orbitRadiusZ>0) {
            float ox = orbitRadiusX * (float)Math.cos(orbitAngle);
            float oz = orbitRadiusZ * (float)Math.sin(orbitAngle);
            Matrix.translateM(model,0, ox,0, oz);
            // Pulsos de escala orbital
            float dyn = 1f + scaleAmplitude * (oz / orbitRadiusZ);

            // OSCILACIÓN DE ESCALA:
            float oscFactor = 1f;
            if (scaleOscPercent != null) {
                // sin() ∈ [-1,1] -> [0,1]
                float osc = 0.5f + 0.5f * (float)Math.sin(accumulatedTime * SCALE_OSC_FREQ * 2f * Math.PI);
                // oscFactor oscila entre scaleOscPercent y 1.0
                oscFactor = scaleOscPercent + (1f - scaleOscPercent) * osc;
            }

            float finalScale = BASE_SCALE * instanceScale * dyn * oscFactor;
            Matrix.scaleM(model,0, finalScale, finalScale, finalScale);
        } else {
            // Sin órbita: aplicar solo oscilación base
            float oscFactor = 1f;
            if (scaleOscPercent != null) {
                float osc = 0.5f + 0.5f * (float)Math.sin(accumulatedTime * SCALE_OSC_FREQ * 2f * Math.PI);
                oscFactor = scaleOscPercent + (1f - scaleOscPercent) * osc;
            }
            float finalScale = BASE_SCALE * instanceScale * oscFactor;
            Matrix.scaleM(model,0, finalScale, finalScale, finalScale);
        }

        // Rotación propia
        Matrix.rotateM(model,0, rotation, 0,1,0);

        // MV y MVP
        Matrix.multiplyMM(mv,0, view,0, model,0);
        Matrix.multiplyMM(mvp,0, proj,0, mv,0);
        setMvpAndResolution(mvp,
                SceneRenderer.screenWidth,
                SceneRenderer.screenHeight);

        // Bind de textura y uniforms de color/alpha
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTexLoc, 0);
        GLES20.glUniform1i(uUseSolidColorLoc, useSolidColor?1:0);
        GLES20.glUniform4fv(uSolidColorLoc,1, solidColor,0);
        GLES20.glUniform1f(uAlphaLoc, alpha);

        // Atributos de posición y UV
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(aPosLoc,
                3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        texCoordBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aTexLoc);
        GLES20.glVertexAttribPointer(aTexLoc,
                2, GLES20.GL_FLOAT,false,0,texCoordBuffer);

        // Dibujar malla
        indexBuffer.position(0);
        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES,
                indexCount,
                GLES20.GL_UNSIGNED_SHORT,
                indexBuffer);

        // Limpieza de atributos
        GLES20.glDisableVertexAttribArray(aPosLoc);
        GLES20.glDisableVertexAttribArray(aTexLoc);
    }
}
