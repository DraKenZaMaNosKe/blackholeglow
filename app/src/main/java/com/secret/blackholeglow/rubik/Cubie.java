package com.secret.blackholeglow.rubik;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * 🧊 CUBIE - Un cubito individual del Cubo Rubik
 *
 * Cada cubie tiene 6 caras, cada una puede tener un color diferente.
 * Los cubies se posicionan en una grilla 3x3x3.
 */
public class Cubie {

    // Colores del Rubik estándar (RGBA)
    public static final float[] WHITE  = {1.0f, 1.0f, 1.0f, 1.0f};   // Arriba (U)
    public static final float[] YELLOW = {1.0f, 1.0f, 0.0f, 1.0f};   // Abajo (D)
    public static final float[] RED    = {1.0f, 0.0f, 0.0f, 1.0f};   // Frente (F)
    public static final float[] ORANGE = {1.0f, 0.5f, 0.0f, 1.0f};   // Atrás (B)
    public static final float[] BLUE   = {0.0f, 0.0f, 1.0f, 1.0f};   // Derecha (R)
    public static final float[] GREEN  = {0.0f, 1.0f, 0.0f, 1.0f};   // Izquierda (L)
    public static final float[] BLACK  = {0.1f, 0.1f, 0.1f, 1.0f};   // Interior (no visible)

    // Índices de caras
    public static final int FACE_FRONT  = 0;  // Z+
    public static final int FACE_BACK   = 1;  // Z-
    public static final int FACE_LEFT   = 2;  // X-
    public static final int FACE_RIGHT  = 3;  // X+
    public static final int FACE_TOP    = 4;  // Y+
    public static final int FACE_BOTTOM = 5;  // Y-

    // Posición en la grilla (−1, 0, 1 para cada eje)
    public int gridX, gridY, gridZ;

    // Colores de cada cara [6][4] = 6 caras × RGBA
    private float[][] faceColors = new float[6][4];

    // Transformación del cubie
    private float[] modelMatrix = new float[16];
    private float[] rotationMatrix = new float[16];

    // Tamaño del cubie
    private static final float SIZE = 0.45f;  // Un poco menor que 0.5 para dejar gap
    private static final float GAP = 0.02f;

    // Buffers OpenGL
    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;
    private ShortBuffer indexBuffer;

    // Vértices del cubo (8 vértices × 3 coords)
    private float[] vertices;
    private float[] colors;
    private short[] indices;

    private boolean buffersInitialized = false;

    public Cubie(int gx, int gy, int gz) {
        this.gridX = gx;
        this.gridY = gy;
        this.gridZ = gz;

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.setIdentityM(rotationMatrix, 0);

        // Posicionar en el espacio
        updatePosition();

        // Inicializar colores según posición en el cubo
        initializeColors();
    }

    private void updatePosition() {
        Matrix.setIdentityM(modelMatrix, 0);
        float offset = SIZE * 2 + GAP;
        Matrix.translateM(modelMatrix, 0, gridX * offset, gridY * offset, gridZ * offset);
    }

    /**
     * Asigna colores basándose en la posición del cubie en el cubo
     */
    private void initializeColors() {
        // Por defecto todas las caras son negras (internas)
        for (int i = 0; i < 6; i++) {
            System.arraycopy(BLACK, 0, faceColors[i], 0, 4);
        }

        // Asignar colores según posición en los bordes del cubo
        if (gridZ == 1)  setFaceColor(FACE_FRONT, RED);     // Frente
        if (gridZ == -1) setFaceColor(FACE_BACK, ORANGE);   // Atrás
        if (gridX == -1) setFaceColor(FACE_LEFT, GREEN);    // Izquierda
        if (gridX == 1)  setFaceColor(FACE_RIGHT, BLUE);    // Derecha
        if (gridY == 1)  setFaceColor(FACE_TOP, WHITE);     // Arriba
        if (gridY == -1) setFaceColor(FACE_BOTTOM, YELLOW); // Abajo
    }

    public void setFaceColor(int face, float[] color) {
        System.arraycopy(color, 0, faceColors[face], 0, 4);
        buffersInitialized = false;  // Necesita rebuild
    }

    public float[] getFaceColor(int face) {
        return faceColors[face];
    }

    /**
     * Inicializa los buffers de OpenGL para renderizar
     */
    public void initBuffers() {
        if (buffersInitialized) return;

        float s = SIZE;

        // 24 vértices (4 por cara para normales correctas)
        vertices = new float[] {
            // Front face (Z+)
            -s, -s,  s,   s, -s,  s,   s,  s,  s,  -s,  s,  s,
            // Back face (Z-)
             s, -s, -s,  -s, -s, -s,  -s,  s, -s,   s,  s, -s,
            // Left face (X-)
            -s, -s, -s,  -s, -s,  s,  -s,  s,  s,  -s,  s, -s,
            // Right face (X+)
             s, -s,  s,   s, -s, -s,   s,  s, -s,   s,  s,  s,
            // Top face (Y+)
            -s,  s,  s,   s,  s,  s,   s,  s, -s,  -s,  s, -s,
            // Bottom face (Y-)
            -s, -s, -s,   s, -s, -s,   s, -s,  s,  -s, -s,  s
        };

        // Colores para cada vértice (4 vértices por cara × 6 caras)
        colors = new float[24 * 4];
        for (int face = 0; face < 6; face++) {
            for (int v = 0; v < 4; v++) {
                int idx = (face * 4 + v) * 4;
                System.arraycopy(faceColors[face], 0, colors, idx, 4);
            }
        }

        // Índices (2 triángulos por cara × 6 caras)
        indices = new short[] {
            0, 1, 2, 0, 2, 3,       // Front
            4, 5, 6, 4, 6, 7,       // Back
            8, 9, 10, 8, 10, 11,    // Left
            12, 13, 14, 12, 14, 15, // Right
            16, 17, 18, 16, 18, 19, // Top
            20, 21, 22, 20, 22, 23  // Bottom
        };

        // Crear buffers
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        vertexBuffer = vbb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length * 4);
        cbb.order(ByteOrder.nativeOrder());
        colorBuffer = cbb.asFloatBuffer();
        colorBuffer.put(colors);
        colorBuffer.position(0);

        ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 2);
        ibb.order(ByteOrder.nativeOrder());
        indexBuffer = ibb.asShortBuffer();
        indexBuffer.put(indices);
        indexBuffer.position(0);

        buffersInitialized = true;
    }

    /**
     * Aplica una rotación temporal durante animación
     */
    public void applyAnimationRotation(float angle, float axisX, float axisY, float axisZ) {
        Matrix.setIdentityM(rotationMatrix, 0);
        Matrix.rotateM(rotationMatrix, 0, angle, axisX, axisY, axisZ);
    }

    /**
     * Confirma la rotación (cuando termina la animación)
     */
    public void commitRotation() {
        // Rotar la posición en la grilla
        // Esto se maneja desde RubiksCube
        Matrix.setIdentityM(rotationMatrix, 0);
    }

    /**
     * Renderiza el cubie
     */
    public void draw(int shaderProgram, float[] viewProjectionMatrix) {
        if (!buffersInitialized) {
            initBuffers();
        }

        // Calcular matriz MVP
        float[] mvpMatrix = new float[16];
        float[] tempMatrix = new float[16];

        // Model = rotation * position
        Matrix.multiplyMM(tempMatrix, 0, rotationMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, viewProjectionMatrix, 0, tempMatrix, 0);

        // Obtener ubicaciones de atributos
        int positionHandle = GLES20.glGetAttribLocation(shaderProgram, "a_Position");
        int colorHandle = GLES20.glGetAttribLocation(shaderProgram, "a_Color");
        int mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "u_MVPMatrix");

        // Pasar matriz MVP
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        // Configurar vértices
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        // Configurar colores
        GLES20.glEnableVertexAttribArray(colorHandle);
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 0, colorBuffer);

        // Dibujar
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        // Limpiar
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(colorHandle);
    }

    /**
     * Actualiza posición en grilla después de una rotación
     */
    public void updateGridPosition(int newX, int newY, int newZ) {
        this.gridX = newX;
        this.gridY = newY;
        this.gridZ = newZ;
        updatePosition();
    }

    /**
     * Rota los colores de las caras (cuando el cubie físicamente rota)
     */
    public void rotateFaceColors(int axis, boolean clockwise) {
        float[] temp = new float[4];

        if (axis == 0) { // Eje X
            if (clockwise) {
                System.arraycopy(faceColors[FACE_FRONT], 0, temp, 0, 4);
                System.arraycopy(faceColors[FACE_BOTTOM], 0, faceColors[FACE_FRONT], 0, 4);
                System.arraycopy(faceColors[FACE_BACK], 0, faceColors[FACE_BOTTOM], 0, 4);
                System.arraycopy(faceColors[FACE_TOP], 0, faceColors[FACE_BACK], 0, 4);
                System.arraycopy(temp, 0, faceColors[FACE_TOP], 0, 4);
            } else {
                System.arraycopy(faceColors[FACE_FRONT], 0, temp, 0, 4);
                System.arraycopy(faceColors[FACE_TOP], 0, faceColors[FACE_FRONT], 0, 4);
                System.arraycopy(faceColors[FACE_BACK], 0, faceColors[FACE_TOP], 0, 4);
                System.arraycopy(faceColors[FACE_BOTTOM], 0, faceColors[FACE_BACK], 0, 4);
                System.arraycopy(temp, 0, faceColors[FACE_BOTTOM], 0, 4);
            }
        } else if (axis == 1) { // Eje Y
            if (clockwise) {
                System.arraycopy(faceColors[FACE_FRONT], 0, temp, 0, 4);
                System.arraycopy(faceColors[FACE_LEFT], 0, faceColors[FACE_FRONT], 0, 4);
                System.arraycopy(faceColors[FACE_BACK], 0, faceColors[FACE_LEFT], 0, 4);
                System.arraycopy(faceColors[FACE_RIGHT], 0, faceColors[FACE_BACK], 0, 4);
                System.arraycopy(temp, 0, faceColors[FACE_RIGHT], 0, 4);
            } else {
                System.arraycopy(faceColors[FACE_FRONT], 0, temp, 0, 4);
                System.arraycopy(faceColors[FACE_RIGHT], 0, faceColors[FACE_FRONT], 0, 4);
                System.arraycopy(faceColors[FACE_BACK], 0, faceColors[FACE_RIGHT], 0, 4);
                System.arraycopy(faceColors[FACE_LEFT], 0, faceColors[FACE_BACK], 0, 4);
                System.arraycopy(temp, 0, faceColors[FACE_LEFT], 0, 4);
            }
        } else { // Eje Z
            if (clockwise) {
                System.arraycopy(faceColors[FACE_TOP], 0, temp, 0, 4);
                System.arraycopy(faceColors[FACE_LEFT], 0, faceColors[FACE_TOP], 0, 4);
                System.arraycopy(faceColors[FACE_BOTTOM], 0, faceColors[FACE_LEFT], 0, 4);
                System.arraycopy(faceColors[FACE_RIGHT], 0, faceColors[FACE_BOTTOM], 0, 4);
                System.arraycopy(temp, 0, faceColors[FACE_RIGHT], 0, 4);
            } else {
                System.arraycopy(faceColors[FACE_TOP], 0, temp, 0, 4);
                System.arraycopy(faceColors[FACE_RIGHT], 0, faceColors[FACE_TOP], 0, 4);
                System.arraycopy(faceColors[FACE_BOTTOM], 0, faceColors[FACE_RIGHT], 0, 4);
                System.arraycopy(faceColors[FACE_LEFT], 0, faceColors[FACE_BOTTOM], 0, 4);
                System.arraycopy(temp, 0, faceColors[FACE_LEFT], 0, 4);
            }
        }

        buffersInitialized = false;  // Necesita rebuild de colores
    }
}
