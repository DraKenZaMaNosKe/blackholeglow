package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import com.secret.blackholeglow.util.ObjLoader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.List;

/**
 * UniverseBackground:
 *   Dibuja un plano frontal usando el pair (vertexShader, fragmentShader)
 *   que se le pase; opcionalmente muestrea una textura con alpha.
 */
public class UniverseBackground
        extends BaseShaderProgram
        implements SceneObject {

    private static final String TAG = "UniverseBackground";

    // Buffers
    private final FloatBuffer  vertexBuffer, texCoordBuffer;
    private final ShortBuffer  indexBuffer;
    private final int          indexCount;

    // GLSL locations
    private final int aPosLoc, aTexLoc;
    private final int uMvpLoc, uAlphaLoc, uTimeLoc, uTexLoc;

    // Configuración
    private final boolean hasTexture;
    private final int     textureId;
    private final float   alpha;

    // Offset tras rotar en X
    private static final float BACKGROUND_OFFSET_Y = -0.1f;

    // Para tiempo relativo
    private final float timeOffset;

    public UniverseBackground(
            Context ctx,
            TextureManager texMgr,
            String vertShaderAsset,
            String fragShaderAsset,
            Integer textureResId,
            float alpha
    ) {
        super(ctx, vertShaderAsset, fragShaderAsset);
        Log.d(TAG, "Constructor: vert=" + vertShaderAsset +
                " frag=" + fragShaderAsset +
                " tex=" + textureResId +
                " alpha=" + alpha);
        this.alpha = alpha;
        // arrancamos el contador en el instante de creación
        this.timeOffset = SystemClock.uptimeMillis() * 0.001f;

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // 1) Carga mesh plano.obj
        ObjLoader.Mesh mesh;
        try {
            mesh = ObjLoader.loadObj(ctx, "plano.obj");
            Log.d(TAG, "Mesh loaded: verts=" + mesh.vertexCount +
                    " faces=" + mesh.faces.size());
        } catch (IOException e) {
            Log.e(TAG, "Error loading plano.obj", e);
            throw new RuntimeException(e);
        }
        vertexBuffer   = mesh.vertexBuffer;
        texCoordBuffer = mesh.uvBuffer;

        // 2) Índices
        List<short[]> faces = mesh.faces;
        int tris = 0;
        for (short[] f : faces) tris += f.length - 2;
        indexCount = tris * 3;
        ShortBuffer ib = ByteBuffer
                .allocateDirect(indexCount * Short.BYTES)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        for (short[] f : faces) {
            short v0 = f[0];
            for (int k = 1; k < f.length - 1; k++) {
                ib.put(v0).put(f[k]).put(f[k + 1]);
            }
        }
        ib.position(0);
        indexBuffer = ib;

        // 3) GLSL locations (ahora buscando u_Time)
        aPosLoc   = GLES20.glGetAttribLocation(programId, "a_Position");
        aTexLoc   = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        uMvpLoc   = GLES20.glGetUniformLocation(programId, "u_MVP");
        uAlphaLoc = GLES20.glGetUniformLocation(programId, "u_Alpha");
        uTimeLoc  = GLES20.glGetUniformLocation(programId, "u_Time");
        uTexLoc   = GLES20.glGetUniformLocation(programId, "u_Texture");
        Log.d(TAG, "locs: aPos=" + aPosLoc +
                " aTex=" + aTexLoc +
                " uMVP=" + uMvpLoc +
                " uAlpha=" + uAlphaLoc +
                " uTime=" + uTimeLoc +   // ahora coincide
                " uTex=" + uTexLoc);

        // 4) Textura opcional
        if (textureResId != null) {
            hasTexture = true;
            textureId  = texMgr.getTexture(textureResId);
            Log.d(TAG, "Loaded texture res=" + textureResId +
                    " texId=" + textureId);
        } else {
            hasTexture = false;
            textureId  = -1;
        }
    }

    @Override
    public void update(float dt) {
        // no-op
    }

    @Override
    public void draw() {
        Log.d(TAG, "draw(): hasTex=" + hasTexture +
                " uTimeLoc=" + uTimeLoc +
                " alpha=" + alpha);
        useProgram();

        // 1) Depth off
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDepthMask(false);

        // 2) Ortho / view / model / MVP
        float aspect = (float)SceneRenderer.screenWidth / SceneRenderer.screenHeight;
        float h = 1f, w = h * aspect;
        float[] proj = new float[16], view = new float[16], model = new float[16];
        Matrix.orthoM(proj,0,-w,w,-h,h,0.1f,100f);
        Matrix.setLookAtM(view,0,0f,0f,3f,0f,0f,0f,0f,1f,0f);
        Matrix.setIdentityM(model,0);
        Matrix.translateM(model,0,0f,BACKGROUND_OFFSET_Y,0f);
        Matrix.rotateM(model,0,90f,1f,0f,0f);
        float[] mv  = new float[16], mvp = new float[16];
        Matrix.multiplyMM(mv,0,view,0,model,0);
        Matrix.multiplyMM(mvp,0,proj,0,mv,0);
        GLES20.glUniformMatrix4fv(uMvpLoc,1,false,mvp,0);

        // 3) Alpha
        GLES20.glUniform1f(uAlphaLoc, alpha);

        // 4) Tiempo RELATIVO
        float t = SystemClock.uptimeMillis() * 0.001f - timeOffset;
        GLES20.glUniform1f(uTimeLoc, t);
        Log.d(TAG, "  -> local u_Time = " + t);

        // 5) Textura
        if (hasTexture) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(uTexLoc, 0);
        }

        // 6) Atributos + dibujado
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(aPosLoc,3,GLES20.GL_FLOAT,false,0,vertexBuffer);
        if (aTexLoc>=0) {
            texCoordBuffer.position(0);
            GLES20.glEnableVertexAttribArray(aTexLoc);
            GLES20.glVertexAttribPointer(aTexLoc,2,GLES20.GL_FLOAT,false,0,texCoordBuffer);
        }
        indexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES,indexCount,GLES20.GL_UNSIGNED_SHORT,indexBuffer);

        // 7) Restore
        GLES20.glDisableVertexAttribArray(aPosLoc);
        if (aTexLoc>=0) GLES20.glDisableVertexAttribArray(aTexLoc);
        GLES20.glDepthMask(true);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }
}
