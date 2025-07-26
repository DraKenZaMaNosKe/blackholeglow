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

public class UniverseBackground
        extends BaseShaderProgram
        implements SceneObject {

    private static final String TAG = "UniverseBackground";

    private final FloatBuffer vertexBuffer;
    private final FloatBuffer texCoordBuffer; // buffer de UVs
    private final ShortBuffer indexBuffer;
    private final int indexCount;

    private final int aPosLoc;
    private final int aTexLoc;
    private final int uTexLoc;
    private final int textureId;
    private float lastDeltaTime;

    public UniverseBackground(Context ctx, TextureManager texMgr) {
        super(ctx,
                "shaders/universe_vertex.glsl",
                "shaders/universe_fragment.glsl");

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // 1) Carga del mesh (ahora con UVs)
        ObjLoader.Mesh mesh;
        try {
            mesh = ObjLoader.loadObj(ctx, "plano.obj");
            Log.d(TAG, "Mesh loaded: verts=" + mesh.vertexCount +
                    ", faces=" + mesh.faces.size());
        } catch (IOException e) {
            Log.e(TAG, "Error loading plano.obj", e);
            throw new RuntimeException(e);
        }
        vertexBuffer   = mesh.vertexBuffer;
        texCoordBuffer = mesh.uvBuffer;  // ← aquí cambiamos mesh.texCoordBuffer por mesh.uvBuffer

        // 2) Triangulación
        List<short[]> faces = mesh.faces;
        int triCount = 0;
        for (short[] f : faces) triCount += f.length - 2;
        indexCount = triCount * 3;
        Log.d(TAG, "triCount=" + triCount + ", indexCount=" + indexCount);

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

        // 3) Attribute & uniform locations
        aPosLoc = GLES20.glGetAttribLocation(programId,  "a_Position");
        aTexLoc = GLES20.glGetAttribLocation(programId,  "a_TexCoord");
        uTexLoc = GLES20.glGetUniformLocation(programId, "u_Texture");
        Log.d(TAG, "a_Position=" + aPosLoc + "  a_TexCoord=" + aTexLoc + "  u_Texture=" + uTexLoc);

        // 4) Textura
        textureId = texMgr.getTexture(R.drawable.textura_universo_estrellado);
    }

    @Override
    public void update(float dt) {
        lastDeltaTime = dt;
    }

    @Override
    public void draw() {
        useProgram();

        // clear
        GLES20.glClearColor(0f,0f,0f,1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);

        // ortho projection
        float aspect = (float)SceneRenderer.screenWidth/SceneRenderer.screenHeight;
        float h = 1f, w = h*aspect;
        float[] proj = new float[16];
        Matrix.orthoM(proj,0, -w,w, -h,h, 0.1f,100f);

        // view
        float[] view = new float[16];
        Matrix.setLookAtM(view,0, 0f,0f,3f, 0f,0f,0f, 0f,1f,0f);

        // model: top-down
        float[] model = new float[16];
        Matrix.setIdentityM(model,0);
        Matrix.rotateM(model,0, 90f,1f,0f,0f);

        // mvp
        float[] mv  = new float[16];
        float[] mvp = new float[16];
        Matrix.multiplyMM(mv,  0, view,  0, model, 0);
        Matrix.multiplyMM(mvp, 0, proj,  0, mv,    0);

        // send uniforms
        setCommonUniforms(
                lastDeltaTime,
                mvp,
                SceneRenderer.screenWidth,
                SceneRenderer.screenHeight
        );

        // bind posición
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(aPosLoc,3,GLES20.GL_FLOAT,false,0,vertexBuffer);

        // bind coordenadas de textura
        texCoordBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aTexLoc);
        GLES20.glVertexAttribPointer(aTexLoc,2,GLES20.GL_FLOAT,false,0,texCoordBuffer);

        // bind textura
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTexLoc, 0);

        // draw
        indexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                indexCount,
                GLES20.GL_UNSIGNED_SHORT,
                indexBuffer);
        Log.d(TAG, "glDrawElements count=" + indexCount);

        // cleanup
        GLES20.glDisableVertexAttribArray(aPosLoc);
        GLES20.glDisableVertexAttribArray(aTexLoc);
    }
}
