package com.secret.blackholeglow;

import android.opengl.Matrix;

/**
 * Controlador de cámara: órbita + zoom en bucle + generación de MVP.
 */
public class CameraController {
    // Centro de la escena
    private float centerX, centerY, centerZ;
    // Vector “up”
    private float upX = 0, upY = 1, upZ = 0;

    // ** Proyección **
    private final float[] projMatrix = new float[16];

    // ** Vista **
    private final float[] viewMatrix = new float[16];

    // Altura fija de la cámara
    private float eyeY;

    // Parámetros de órbita
    private float orbitDuration = 12f;
    private float orbitTime     = 0f;

    // Parámetros de zoom en bucle
    private float minRadius     = 4f;  // distancia inicial
    private float maxRadius     = 10f; // distancia donde “desaparece”
    private float zoomDuration  = 20f; // segundos para ir de min→max
    private float zoomTime      = 0f;  // va de 0 → zoomDuration

    /**
     * Inicializa la cámara: ojo, centro y “up”.
     * Aquí calculamos el minRadius automáticamente.
     */
    public void setView(float eyeX, float eyeY, float eyeZ,
                        float centerX, float centerY, float centerZ,
                        float upX, float upY, float upZ) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.upX      = upX;
        this.upY      = upY;
        this.upZ      = upZ;
        this.eyeY     = eyeY;

        // Calcula minRadius = distancia inicial en XY
        float dx = eyeX - centerX;
        float dz = eyeZ - centerZ;
        this.minRadius = (float)Math.hypot(dx, dz);
    }

    /** Duración del ciclo de órbita completa (360°). */
    public void startOrbit(float seconds) {
        this.orbitDuration = seconds;
        this.orbitTime     = 0f;
    }

    /** Duración del ciclo de zoom de min→max→min (reinicia al llegar). */
    public void startZoomLoop(float seconds, float targetMaxRadius) {
        this.zoomDuration = seconds;
        this.maxRadius    = targetMaxRadius;
        this.zoomTime     = 0f;
    }

    /**
     * Debe llamarse cuando cambie el tamaño de la vista.
     * Calcula la matriz de proyección.
     */
    public void updateProjection(int width, int height) {
        float aspect = (float)width / height;
        Matrix.perspectiveM(projMatrix, 0, 45f, aspect, 1f, 100f);
    }

    /**
     * Llamar en cada frame con dt:
     * - Actualiza órbita y zoom.
     * - Genera la matriz de vista (lookAt).
     */
    public void update(float dt) {
        // —— Órbita ——
        orbitTime = (orbitTime + dt) % orbitDuration;
        float angle = (orbitTime / orbitDuration) * 2f * (float)Math.PI;

        // —— Zoom en bucle ——
        zoomTime = (zoomTime + dt) % zoomDuration;
        float t      = zoomTime / zoomDuration; // [0,1)
        float radius = minRadius + t * (maxRadius - minRadius);

        // Posición del ojo en plano XZ
        float eyeX = centerX + radius * (float)Math.cos(angle);
        float eyeZ = centerZ + radius * (float)Math.sin(angle);

        // LookAt
        Matrix.setLookAtM(
                viewMatrix, 0,
                eyeX, eyeY, eyeZ,            // ojo
                centerX, centerY, centerZ,   // centro
                upX, upY, upZ                // up
        );
    }

    /**
     * Añade un pequeño desplazamiento al ángulo de órbita.
     */
    public void addOrbitOffset(float deltaDegrees) {
        // Convertimos grados a fracción de 2·PI y lo sumamos a orbitTime:
        float deltaRadians = (float)Math.toRadians(deltaDegrees);
        float orbitFraction = deltaRadians / (2f * (float)Math.PI);
        // orbitDuration recoge cuánto tardamos en un giro completo, así que:
        orbitTime = (orbitTime + orbitFraction * orbitDuration) % orbitDuration;
    }

    /**
     * Genera MVP = P * V * M en un solo método.
     * @param model  matriz modelo (4x4)
     * @param outMvp resultado (4x4)
     */
    public void computeMvp(float[] model, float[] outMvp) {
        float[] temp = new float[16];
        // V * M
        Matrix.multiplyMM(temp, 0, viewMatrix, 0, model, 0);
        // P * (V * M)
        Matrix.multiplyMM(outMvp, 0, projMatrix, 0, temp, 0);
    }
}
