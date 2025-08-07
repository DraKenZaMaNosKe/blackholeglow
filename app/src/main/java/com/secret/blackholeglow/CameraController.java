package com.secret.blackholeglow;

import android.opengl.Matrix;

/**
 * Controlador de cámara: vista Top-Orthographic fija (sin rotación ni zoom).
 */
public class CameraController {
    // Centro de la escena (origen)
    private final float centerX = 0f, centerY = 0f, centerZ = 0f;

    // Matrices de proyección y vista
    private final float[] projMatrix = new float[16];
    private final float[] viewMatrix = new float[16];

    /**
     * Llamar en onSurfaceChanged(): crea una proyección ortográfica
     * y sitúa la cámara “desde arriba” con un LookAt fijo.
     */
    public void updateProjection(int width, int height) {
        float aspect = (float) width / height;
        // ortho(left, right, bottom, top, near, far)
        Matrix.orthoM(
                projMatrix, 0,
                -aspect, aspect,
                -1f, 1f,
                -10f, 10f
        );
        // Vista Top: ojo en Y positivo mirando al origen
        float eyeY = centerY + 5f;
        Matrix.setLookAtM(
                viewMatrix, 0,
                centerX, eyeY, centerZ,   // ojo
                centerX, centerY, centerZ, // centro
                0f, 0f, -1f               // up hacia -Z para orientar bien la vista
        );
    }

    /**
     * No hace nada: cámara estática.
     */
    public void update(float dt) {
        // no-op
    }

    /**
     * No rota la cámara.
     */
    public void addOrbitOffset(float deltaDegrees) {
        // no-op
    }

    /**
     * Genera MVP = P * V * M.
     */
    public void computeMvp(float[] model, float[] outMvp) {
        float[] temp = new float[16];
        Matrix.multiplyMM(temp, 0, viewMatrix, 0, model, 0);
        Matrix.multiplyMM(outMvp, 0, projMatrix, 0, temp, 0);
    }
}
