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
 *   Ahora implementa CameraAware para calcular MVP vía CameraController.
 */
public class UniverseBackground
        extends BaseShaderProgram
        implements SceneObject, CameraAware {

    private static final String TAG = "UniverseBackground";

    // Buffers y conteo de índices
    private final FloatBuffer  vertexBuffer, texCoordBuffer;
    private final ShortBuffer  indexBuffer;
    private final int          indexCount;

    // Locaciones GLSL
    private final int aPosLoc, aTexLoc;
    private final int uMvpLoc, uAlphaLoc, uTimeLoc, uTexLoc, uResolutionLoc;

    // Textura y transparencia
    private final boolean hasTexture;
    private final int     textureId;
    private final float   alpha;

    // Para animaciones y offest
    private static final float BACKGROUND_OFFSET_Y = -0.1f;
    private final float timeOffset;

    // Control de cámara
    private CameraController camera;

    /**
     * Inyecta el CameraController para usar perspectiva y vista.
     */
    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

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
        this.alpha      = alpha;
        this.timeOffset = SystemClock.uptimeMillis() * 0.001f;

        float t = (SystemClock.uptimeMillis() * 0.001f - timeOffset) % 60.0f;

        // Habilita test de profundidad y culling
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Carga la malla del plano
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

        // Índices para triangulación
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

        // Obtiene locaciones de atributos y uniforms
        aPosLoc         = GLES20.glGetAttribLocation(programId, "a_Position");
        aTexLoc         = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        uMvpLoc         = GLES20.glGetUniformLocation(programId, "u_MVP");
        uAlphaLoc       = GLES20.glGetUniformLocation(programId, "u_Alpha");
        uTimeLoc        = GLES20.glGetUniformLocation(programId, "u_Time");
        uTexLoc         = GLES20.glGetUniformLocation(programId, "u_Texture");
        uResolutionLoc  = GLES20.glGetUniformLocation(programId, "u_Resolution");
        Log.d(TAG, "locs: aPos=" + aPosLoc +
                " aTex=" + aTexLoc +
                " uMVP=" + uMvpLoc +
                " uAlpha=" + uAlphaLoc +
                " uTime=" + uTimeLoc +
                " uTex=" + uTexLoc +
                " uRes=" + uResolutionLoc);

        // Textura opcional
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
        // Sin animaciones de modelo aquí
    }

    public void release() {
        if (hasTexture && textureId > 0) {
            int[] textures = { textureId };
            GLES20.glDeleteTextures(1, textures, 0);
            Log.d(TAG, "Textura liberada: " + textureId);
        }
    }

    @Override
    public void draw() {
        if (camera == null) {
            Log.e(TAG, "CameraController no inyectado, draw abortado.");
            return;
        }

        useProgram();

        // Desactiva depth y culling para fondo
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDepthMask(false);

        // Prepara matriz modelo (identidad + offset + rotación X)
        float[] model = new float[16];
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, 0f, BACKGROUND_OFFSET_Y, 0f);
        Matrix.rotateM(model, 0, 0f, 1f, 0f, 0f);

        // Calcula MVP vía CameraController
        float[] mvp = new float[16];
        camera.computeMvp(model, mvp);
        GLES20.glUniformMatrix4fv(uMvpLoc, 1, false, mvp, 0);

        // Envía uniforms adicionales
        GLES20.glUniform1f(uAlphaLoc, alpha);
        float t = (SystemClock.uptimeMillis() * 0.001f - timeOffset) % 60.0f;

        GLES20.glUniform1f(uTimeLoc, t);
        GLES20.glUniform2f(uResolutionLoc,
                (float)SceneRenderer.screenWidth,
                (float)SceneRenderer.screenHeight);
        Log.d(TAG, "u_Resolution sent: " + SceneRenderer.screenWidth + "x" + SceneRenderer.screenHeight);

        // Textura si aplica
        if (hasTexture) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(uTexLoc, 0);
        }

        // Atributos y dibujado del mesh
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        if (aTexLoc >= 0) {
            texCoordBuffer.position(0);
            GLES20.glEnableVertexAttribArray(aTexLoc);
            GLES20.glVertexAttribPointer(aTexLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);
        }
        indexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        // Restaura estados
        GLES20.glDisableVertexAttribArray(aPosLoc);
        if (aTexLoc >= 0) GLES20.glDisableVertexAttribArray(aTexLoc);
        GLES20.glDepthMask(true);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }
}
