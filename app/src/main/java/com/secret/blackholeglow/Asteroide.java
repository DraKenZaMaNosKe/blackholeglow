package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.util.ObjLoader;
import com.secret.blackholeglow.util.ObjLoader.Mesh;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.List;
import java.util.Random;

/**
 * ====================================================================
 * Asteroide
 * ====================================================================
 * Cada 6–8 s nace aleatoriamente cerca del centro (±0.5),
 * recorre Z desde muy atrás a traspasar la cámara a 5 m/s,
 * escala con easing de 0.01→0.08 m y nunca supera ese máximo,
 * deriva senoidal suave convergiendo al centro.
 */
public class Asteroide extends BaseShaderProgram implements SceneObject {
    private static final String TAG = "Asteroide";

    // Trayectoria en Z: de muy atrás (−50 m) a más allá de la cámara (+13 m)
    private static final float Z_START  = -50f;   // metros
    private static final float Z_END    = 13f;    // metros
    private static final float Z_SPEED  = 5f;     // m/s

    // Escala objetivo (radio de bounding sphere) en metros
    private static final float SCALE_MIN = 0.01f; // m
    private static final float SCALE_MAX = 0.08f; // m, igual al planeta central

    // Intervalo de spawn aleatorio [6..8] s
    private static final float SPAWN_MIN = 6f;    // s
    private static final float SPAWN_MAX = 8f;    // s

    // Amplitud de deriva lateral (senoidal), adimensional
    private static final float DRIFT_AMP = 0.2f;

    // Parámetros desde constructor
    private final float instanceScale;
    private final boolean useSolidColor;
    private final float[] solidColor;
    private final float alpha;
    private final float uvScale;

    // OpenGL
    private final int textureId;
    private final FloatBuffer vertexBuffer, texCoordBuffer;
    private final ShortBuffer indexBuffer;
    private final int indexCount;
    private final int aPosLoc, aTexLoc;
    private final int uMvpLoc, uTexLoc, uUseSolidColorLoc,
            uSolidColorLoc, uAlphaLoc, uUvScaleLoc;

    // Estado dinámico
    private final Random rand = new Random();
    private float timer     = 0f;    // s
    private float nextSpawn = 0f;    // s
    private boolean active  = false;
    private float zPos      = Z_START;
    private float baseX, baseY;

    // Matrices de trabajo
    private final float[] model = new float[16];
    private final float[] view  = new float[16];
    private final float[] proj  = new float[16];
    private final float[] mv    = new float[16];
    private final float[] mvp   = new float[16];

    public Asteroide(Context ctx,
                     TextureManager texMgr,
                     String vertAsset,
                     String fragAsset,
                     int textureResId,
                     float instanceScale,
                     boolean useSolidColor,
                     float[] solidColor,
                     float alpha,
                     float uvScale) {
        super(ctx, vertAsset, fragAsset);

        this.instanceScale = instanceScale;
        this.useSolidColor = useSolidColor;
        this.solidColor    = solidColor != null
                ? solidColor
                : new float[]{1f,1f,1f,1f};
        this.alpha         = alpha;
        this.uvScale       = uvScale;

        // cargar textura
        textureId = texMgr.getTexture(textureResId);

        // cargar malla
        Mesh mesh;
        try {
            mesh = ObjLoader.loadObj(ctx, "asteroide.obj");
        } catch (IOException e) {
            throw new RuntimeException("Error cargando asteroide.obj", e);
        }
        vertexBuffer   = mesh.vertexBuffer;
        texCoordBuffer = mesh.uvBuffer;

        // construir índices
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
            for (int i = 1; i < f.length - 1; i++) {
                ib.put(v0).put(f[i]).put(f[i+1]);
            }
        }
        ib.position(0);
        indexBuffer = ib;

        // atributos & uniforms
        aPosLoc           = GLES20.glGetAttribLocation(programId, "a_Position");
        aTexLoc           = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        uMvpLoc           = GLES20.glGetUniformLocation(programId, "u_MVP");
        uTexLoc           = GLES20.glGetUniformLocation(programId, "u_Texture");
        uUseSolidColorLoc = GLES20.glGetUniformLocation(programId, "u_UseSolidColor");
        uSolidColorLoc    = GLES20.glGetUniformLocation(programId, "u_SolidColor");
        uAlphaLoc         = GLES20.glGetUniformLocation(programId, "u_Alpha");
        uUvScaleLoc       = GLES20.glGetUniformLocation(programId, "u_UvScale");

        scheduleNextSpawn();
    }

    private void scheduleNextSpawn() {
        active    = false;
        timer     = 0f;
        zPos      = Z_START;
        nextSpawn = SPAWN_MIN + rand.nextFloat()*(SPAWN_MAX - SPAWN_MIN);
        // punto de partida alejado del centro (radio entre 0.1 y 0.5)
        do {
            baseX = (rand.nextFloat()*2f - 1f) * 0.5f;
            baseY = (rand.nextFloat()*2f - 1f) * 0.5f;
        } while (Math.hypot(baseX, baseY) < 0.1f);
        Log.d(TAG, String.format(
                "Re-scheduling spawn en %.2f s, punto inicio (%.2f,%.2f)",
                nextSpawn, baseX, baseY));
    }

    @Override
    public void update(float dt) {
        timer += dt;
        if (!active) {
            if (timer >= nextSpawn) {
                active = true;
                timer  = 0f;
                Log.d(TAG, "Asteroide activo: iniciando viaje desde Z=" + Z_START + " m");
            } else {
                return;
            }
        }
        // avanzar en Z a velocidad constante
        zPos += Z_SPEED * dt;
        if (zPos >= Z_END) {
            Log.d(TAG, "Asteroide traspasó cámara a Z=" + zPos + " m, reiniciando.");
            scheduleNextSpawn();
        }
    }

    @Override
    public void draw() {
        if (!active) return;

        useProgram();

        // progreso normalizado [0..1]
        float p = (zPos - Z_START) / (Z_END - Z_START);
        p = Math.max(0f, Math.min(1f, p));
        // easing cubic in‐out
        float e = p < 0.5f
                ? 4*p*p*p
                : 1f - (float)Math.pow(-2f*p + 2f, 3)/2f;

        // escala interpolada
        float scaleFrac = SCALE_MIN + e*(SCALE_MAX - SCALE_MIN);
        float finalScale = scaleFrac * instanceScale;
        finalScale = Math.min(finalScale, SCALE_MAX);

        // Camera ortho fija en (0,0,12) m
        float camX = 0f, camY = 0f, camZ = 12f;
        // Distancias reales
        float distCam = (float)Math.hypot(
                Math.hypot(baseX*(1-p), baseY*(1-p)),
                camZ - zPos);
        float distCenter = (float)Math.hypot(
                Math.hypot(baseX*(1-p), baseY*(1-p)),
                zPos);

        Log.d(TAG, String.format(
                "Asteroide pos=(%.2f,%.2f,%.2f) m  escala=%.3f m  distCam=%.2f m(%.3f km)  distCentro=%.2f m",
                baseX*(1-p), baseY*(1-p), zPos,
                finalScale,
                distCam, distCam/1000f,
                distCenter));

        // proyección orto + vista fija
        float aspect = (float)SceneRenderer.screenWidth
                / SceneRenderer.screenHeight;
        float h = 1f, w = h*aspect;
        Matrix.orthoM(proj, 0, -w, w, -h, h, 0.1f, 100f);
        Matrix.setLookAtM(view, 0,
                camX, camY, camZ,
                0f,    0f,    0f,
                0f,    1f,    0f);

        // uniforms color/alpha/uvScale
        GLES20.glUniform1i(uUseSolidColorLoc, useSolidColor?1:0);
        GLES20.glUniform4fv(uSolidColorLoc, 1, solidColor, 0);
        GLES20.glUniform1f(uAlphaLoc, alpha);
        GLES20.glUniform1f(uUvScaleLoc, uvScale);

        // deriva senoidal suavizada
        float dx = DRIFT_AMP * (float)Math.sin(e * 2f * Math.PI) * (1f - p);
        float dy = DRIFT_AMP * (float)Math.cos(e * 2f * Math.PI) * (1f - p);
        float actualX = baseX * (1f - p) + dx;
        float actualY = baseY * (1f - p) + dy;

        // modelo 3D
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, actualX, actualY, zPos);
        Matrix.scaleM    (model, 0, finalScale, finalScale, finalScale);

        // MVP
        Matrix.multiplyMM(mv,  0, view,  0, model, 0);
        Matrix.multiplyMM(mvp, 0, proj,  0, mv,    0);
        GLES20.glUniformMatrix4fv(uMvpLoc, 1, false, mvp, 0);

        // bind y dibujar
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

        indexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        GLES20.glDisableVertexAttribArray(aPosLoc);
        GLES20.glDisableVertexAttribArray(aTexLoc);
    }
}
