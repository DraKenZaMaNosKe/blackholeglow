package com.secret.blackholeglow;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/*
 ╔══════════════════════════════════════════════╗
 ║   🤖🖐 Hand2DBackground                       ║
 ║   Dibuja un contorno de mano en 2D sin giro  ║
 ╚══════════════════════════════════════════════╝
*/
public class RotatingHandMeshBackground implements SceneObject {
    private final int program;
    private final int aPosLoc;
    private final int uMVPLoc;
    private final int uColorLoc;

    private final FloatBuffer vBuffer;
    private final ShortBuffer iBuffer;
    private final int indexCount;

    // Matriz identidad para MVP (no hay cámara ni proyección especial)
    private final float[] mvp = new float[16];

    public RotatingHandMeshBackground(TextureManager ignore) {
        // 1️⃣ – Shaders básicos (posición + color)
        String vShader =
                "attribute vec4 a_Position;\n" +
                        "uniform mat4 u_MVP;\n" +
                        "void main() {\n" +
                        "    gl_Position = u_MVP * a_Position;\n" +
                        "}";
        String fShader =
                "precision mediump float;\n" +
                        "uniform vec4 u_Color;\n" +
                        "void main() {\n" +
                        "    gl_FragColor = u_Color;\n" +
                        "}";

        // 2️⃣ – Creamos el programa y obtenemos ubicaciones
        program   = ShaderUtils.createProgram(vShader, fShader);
        aPosLoc   = GLES20.glGetAttribLocation(program, "a_Position");
        uMVPLoc   = GLES20.glGetUniformLocation(program, "u_MVP");
        uColorLoc = GLES20.glGetUniformLocation(program, "u_Color");

        // 3️⃣ – Definimos vértices de la silueta de mano (XY, Z=0)
        float[] verts = {
                // muñeca izquierda, palma, dedos y muñeca derecha
                -0.5f, -0.8f, 0f,  // v0
                -0.5f, -0.2f, 0f,  // v1
                -0.4f, -0.2f, 0f,  // v2
                -0.45f, 0.2f, 0f,  // v3 (base dedo índice)
                -0.3f, 0.4f, 0f,   // v4 (punta índice)
                -0.2f, 0.2f, 0f,   // v5 (base dedo medio)
                -0.1f, 0.6f, 0f,   // v6 (punta medio)
                0.0f, 0.4f, 0f,   // v7 (base anular)
                0.1f, 0.6f, 0f,   // v8 (punta anular)
                0.2f, 0.2f, 0f,   // v9 (base meñique)
                0.3f, 0.4f, 0f,   // v10 (punta meñique)
                0.45f,0.2f, 0f,   // v11 (volver palma derecha)
                0.4f, -0.2f,0f,   // v12
                0.5f, -0.2f,0f,   // v13
                0.5f, -0.8f,0f    // v14
        };

        // 4️⃣ – Definimos índices para conectar en un “loop” las líneas
        short[] idx = {
                0,1,  1,2,  2,3,  3,4,  4,5,
                5,6,  6,7,  7,8,  8,9,  9,10,
                10,11,11,12,12,13,13,14,14,0
        };
        indexCount = idx.length;

        // 5️⃣ – Creamos buffers nativos
        vBuffer = ByteBuffer
                .allocateDirect(verts.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(verts);
        vBuffer.position(0);

        iBuffer = ByteBuffer
                .allocateDirect(idx.length * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
                .put(idx);
        iBuffer.position(0);

        // 6️⃣ – Matriz identidad MVP
        Matrix.setIdentityM(mvp, 0);

        // activamos test de profundidad (opcional aquí)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    @Override
    public void update(float dt) {
        // sin animación ni giro
    }

    @Override
    public void draw() {
        // Limpiamos pantalla
        GLES20.glClearColor(0f,0f,0f,1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Usamos nuestro shader
        GLES20.glUseProgram(program);

        // Pasamos la matriz MVP (identidad)
        GLES20.glUniformMatrix4fv(uMVPLoc, 1, false, mvp, 0);
        // Color blanco para la silueta
        GLES20.glUniform4f(uColorLoc, 1f, 1f, 1f, 1f);

        // Activamos atributo de posición
        GLES20.glEnableVertexAttribArray(aPosLoc);
        GLES20.glVertexAttribPointer(
                aPosLoc,
                3, GLES20.GL_FLOAT,
                false,
                3 * 4,
                vBuffer
        );

        // Dibujamos líneas con GL_LINES
        GLES20.glLineWidth(2f);
        GLES20.glDrawElements(
                GLES20.GL_LINES,
                indexCount,
                GLES20.GL_UNSIGNED_SHORT,
                iBuffer
        );

        // Deshabilitamos atributo
        GLES20.glDisableVertexAttribArray(aPosLoc);
    }
}
