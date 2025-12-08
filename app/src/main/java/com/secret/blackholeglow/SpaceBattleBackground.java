// SpaceBattleBackground.java - Fondo simple para batalla espacial
package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Fondo espacial simple para batalla top-down.
 * Renderiza un quad grande con textura que cubre toda la vista.
 */
public class SpaceBattleBackground extends BaseShaderProgram implements SceneObject, CameraAware {
    private static final String TAG = "SpaceBattleBackground";

    private final Context context;
    private final TextureLoader textureLoader;
    private final int textureResourceId;

    // Uniform locations
    private final int aPosLoc, aTexLoc;
    private final int uTexLoc, uAlphaLoc;

    // Buffers
    private final FloatBuffer vertexBuffer;
    private final FloatBuffer texCoordBuffer;
    private final ShortBuffer indexBuffer;

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // Camera
    private CameraController camera;

    // Geometría: Quad en coordenadas NORMALIZADAS de pantalla (-1 a 1)
    // Esto cubre TODA la pantalla uniformemente sin perspectiva
    private static final float[] VERTICES = {
        -1f,  1f, 0.999f,  // Top-left (Z cerca de 1 = muy atrás)
        -1f, -1f, 0.999f,  // Bottom-left
         1f, -1f, 0.999f,  // Bottom-right
         1f,  1f, 0.999f   // Top-right
    };

    // UVs SIN tiling (textura estirada, sin repetición)
    private static final float[] TEX_COORDS = {
        0f, 0f,   // Top-left
        0f, 1f,   // Bottom-left
        1f, 1f,   // Bottom-right
        1f, 0f    // Top-right
    };

    private static final short[] INDICES = {
        0, 1, 2,
        0, 2, 3
    };

    public SpaceBattleBackground(Context context,
                                TextureLoader textureLoader,
                                int textureResourceId) {
        super(context, "shaders/spaceship_vertex.glsl", "shaders/spaceship_fragment.glsl");

        this.context = context;
        this.textureLoader = textureLoader;
        this.textureResourceId = textureResourceId;

        // Obtener uniform locations
        aPosLoc = GLES30.glGetAttribLocation(programId, "a_Position");
        aTexLoc = GLES30.glGetAttribLocation(programId, "a_TexCoord");
        uTexLoc = GLES30.glGetUniformLocation(programId, "u_Texture");
        uAlphaLoc = GLES30.glGetUniformLocation(programId, "u_Alpha");

        // Inicializar buffers
        vertexBuffer = ByteBuffer.allocateDirect(VERTICES.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(VERTICES);
        vertexBuffer.position(0);

        texCoordBuffer = ByteBuffer.allocateDirect(TEX_COORDS.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(TEX_COORDS);
        texCoordBuffer.position(0);

        indexBuffer = ByteBuffer.allocateDirect(INDICES.length * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
                .put(INDICES);
        indexBuffer.position(0);
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    @Override
    public void update(float deltaTime) {
        // Fondo estático, no necesita actualización
    }

    @Override
    public void draw() {
        // Desactivar depth test para el fondo (siempre atrás)
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);

        // Activar programa
        useProgram();

        // ✅ MATRIZ IDENTIDAD: renderiza en coordenadas de pantalla directas
        // NO usar la cámara 3D para evitar distorsión de perspectiva
        Matrix.setIdentityM(mvpMatrix, 0);

        // Enviar MVP al shader (matriz identidad = sin transformación)
        int uMvpLoc = GLES30.glGetUniformLocation(programId, "u_MVP");
        GLES30.glUniformMatrix4fv(uMvpLoc, 1, false, mvpMatrix, 0);

        // Sin tinte, alpha completo
        int uTintColorLoc = GLES30.glGetUniformLocation(programId, "u_TintColor");
        float[] tintColor = {1f, 1f, 1f, 0f};
        GLES30.glUniform4fv(uTintColorLoc, 1, tintColor, 0);
        GLES30.glUniform1f(uAlphaLoc, 1.0f);

        // Bind textura
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureLoader.getTexture(textureResourceId));
        GLES30.glUniform1i(uTexLoc, 0);

        // Enviar geometría
        GLES30.glEnableVertexAttribArray(aPosLoc);
        GLES30.glVertexAttribPointer(aPosLoc, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        GLES30.glEnableVertexAttribArray(aTexLoc);
        GLES30.glVertexAttribPointer(aTexLoc, 2, GLES30.GL_FLOAT, false, 0, texCoordBuffer);

        // Dibujar
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, INDICES.length, GLES30.GL_UNSIGNED_SHORT, indexBuffer);

        // Limpiar
        GLES30.glDisableVertexAttribArray(aPosLoc);
        GLES30.glDisableVertexAttribArray(aTexLoc);

        // Reactivar depth test para otros objetos
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
    }
}
