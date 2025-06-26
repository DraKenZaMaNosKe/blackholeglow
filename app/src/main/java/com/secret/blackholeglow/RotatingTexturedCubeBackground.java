package com.secret.blackholeglow;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * RotatingTexturedCubeBackground – Un cubo 3D con la misma textura en cada cara,
 * que gira suavemente sobre su eje.
 */
public class RotatingTexturedCubeBackground implements SceneObject {
    // —– GLSL handles —–
    private final int program;
    private final int aPosLoc, aTexLoc;
    private final int uMVPMatrixLoc, uTexSamplerLoc;

    // —– Buffers —–
    private final FloatBuffer vBuffer;
    private final FloatBuffer uvBuffer;
    private final ShortBuffer idxBuffer;

    // —– Textura única —–
    private final int textureId;

    // —– Matrices —–
    private final float[] proj = new float[16];
    private final float[] view = new float[16];
    private final float[] model = new float[16];
    private final float[] mvp   = new float[16];
    private final float[] tmp   = new float[16];

    // —– Ángulo de rotación —–
    private float angle = 0f;

    // ­­­­­­­­­—– Geometría del cubo —–
    // 8 vértices (x,y,z)
    private static final float[] CUBE_VERTICES = {
            -1, -1, -1,   1, -1, -1,   1,  1, -1,  -1,  1, -1,
            -1, -1,  1,   1, -1,  1,   1,  1,  1,  -1,  1,  1
    };
    // indices para 12 triángulos
    private static final short[] CUBE_INDICES = {
            0,1,2, 0,2,3,
            4,5,6, 4,6,7,
            0,1,5, 0,5,4,
            2,3,7, 2,7,6,
            0,3,7, 0,7,4,
            1,2,6, 1,6,5
    };
    // UVs (repite para ambos “planos” del cubo)
    private static final float[] CUBE_UVS = {
            0f,0f, 1f,0f, 1f,1f, 0f,1f,
            0f,0f, 1f,0f, 1f,1f, 0f,1f
    };

    public RotatingTexturedCubeBackground(TextureManager loader) {
        // 1) compilar shaders (usa tu ShaderUtils)
        String vShader =
                "attribute vec4 a_Position;\n" +
                        "attribute vec2 a_TexCoord;\n" +
                        "uniform mat4 u_MVP;\n" +
                        "varying vec2 v_Tex;\n" +
                        "void main(){\n" +
                        "  v_Tex = a_TexCoord;\n" +
                        "  gl_Position = u_MVP * a_Position;\n" +
                        "}";
        String fShader =
                "precision mediump float;\n" +
                        "varying vec2 v_Tex;\n" +
                        "uniform sampler2D u_Texture;\n" +
                        "void main(){\n" +
                        "  gl_FragColor = texture2D(u_Texture, v_Tex);\n" +
                        "}";
        program = ShaderUtils.createProgram(vShader, fShader);
        aPosLoc       = GLES20.glGetAttribLocation(program,   "a_Position");
        aTexLoc       = GLES20.glGetAttribLocation(program,   "a_TexCoord");
        uMVPMatrixLoc = GLES20.glGetUniformLocation(program,  "u_MVP");
        uTexSamplerLoc= GLES20.glGetUniformLocation(program,  "u_Texture");

        // 2) buffers
        vBuffer = ByteBuffer.allocateDirect(CUBE_VERTICES.length*4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(CUBE_VERTICES);
        vBuffer.position(0);

        uvBuffer = ByteBuffer.allocateDirect(CUBE_UVS.length*4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(CUBE_UVS);
        uvBuffer.position(0);

        idxBuffer = ByteBuffer.allocateDirect(CUBE_INDICES.length*2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
                .put(CUBE_INDICES);
        idxBuffer.position(0);

        // 3) textura (la misma en todas las caras)
        textureId = loader.getCubeTexture();
    }

    @Override
    public void update(float deltaTime) {
        // gira 30° por segundo
        angle += 30f * deltaTime;
        if (angle > 360f) angle -= 360f;
    }

    @Override
    public void draw() {
        GLES20.glUseProgram(program);

        // cámara y proyección
        float aspect = (float)SceneRenderer.screenWidth / SceneRenderer.screenHeight;
        Matrix.perspectiveM(proj,0,45,aspect,1f,20f);
        Matrix.setLookAtM(view,0,
                0,0,-6,   // ojo
                0,0, 0,   // centro
                0,1, 0);  // up

        // modelo: rotar en los ejes X+Y
        Matrix.setIdentityM(model,0);
        Matrix.rotateM(model,0, angle, 1f,1f,0f);

        // MVP = P * V * M
        Matrix.multiplyMM(tmp,0, view,0, model,0);
        Matrix.multiplyMM(mvp,0, proj,0, tmp,0);
        GLES20.glUniformMatrix4fv(uMVPMatrixLoc,1,false,mvp,0);

        // bind textura
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTexSamplerLoc,0);

        // atributos
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(aPosLoc,3,GLES20.GL_FLOAT,false,0,vBuffer);

        GLES20.glEnableVertexAttribArray(aTexLoc);
        GLES20.glVertexAttribPointer(aTexLoc,2,GLES20.GL_FLOAT,false,0,uvBuffer);

        // dibujar con índices
        GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                CUBE_INDICES.length,
                GLES20.GL_UNSIGNED_SHORT,
                idxBuffer);

        GLES20.glDisableVertexAttribArray(aPosLoc);
        GLES20.glDisableVertexAttribArray(aTexLoc);
    }
}
