package com.secret.blackholeglow;

import android.opengl.Matrix;

/**
 * Sistema de Cámara Simplificado para Wallpapers
 * Mantiene solo las perspectivas útiles para fondos animados
 * Sin movimiento - cámara completamente estática
 */
public class CameraController {

    // ====== TIPOS DE PERSPECTIVA ======
    public enum CameraMode {
        PERSPECTIVE_3_4,      // Vista 3/4 estilo isométrico con perspectiva
        PERSPECTIVE_FRONT,    // Vista frontal directa
        PERSPECTIVE_TOP,      // Vista desde arriba
        PERSPECTIVE_DRAMATIC  // Vista dramática desde abajo
    }

    // ====== ESTADO ACTUAL ======
    private CameraMode currentMode = CameraMode.PERSPECTIVE_3_4;

    // ====== PARÁMETROS DE POSICIÓN (FIJOS) ======
    private float[] position = {4f, 3f, 6f};      // Posición de la cámara
    private float[] target = {0f, 0f, 0f};        // Punto que mira la cámara
    private float[] up = {0f, 1f, 0f};            // Vector "arriba"

    // ====== PARÁMETROS DE PROYECCIÓN ======
    private float fov = 60f;           // Campo de visión
    private float nearPlane = 0.1f;
    private float farPlane = 100f;
    private float aspect = 1f;

    // ====== MATRICES ======
    private final float[] viewMatrix = new float[16];
    private final float[] projMatrix = new float[16];
    private final float[] tempMatrix = new float[16];

    /**
     * Constructor - Inicializa con vista por defecto 3/4
     */
    public CameraController() {
        setMode(CameraMode.PERSPECTIVE_3_4);
        updateViewMatrix();
    }

    /**
     * Actualiza la proyección cuando cambia el viewport
     */
    public void updateProjection(int width, int height) {
        aspect = (float) width / height;

        // Proyección perspectiva
        Matrix.perspectiveM(
                projMatrix, 0,
                fov,        // Campo de visión
                aspect,     // Aspecto
                nearPlane,  // Plano cercano
                farPlane    // Plano lejano
        );
    }

    /**
     * Actualización simplificada - ya no hay movimiento
     * Este método se mantiene por compatibilidad pero no hace nada
     */
    public void update(float deltaTime) {
        // Cámara completamente estática - no hay actualización
    }

    /**
     * Actualiza la matriz de vista
     */
    private void updateViewMatrix() {
        Matrix.setLookAtM(
                viewMatrix, 0,
                position[0], position[1], position[2],    // Posición de cámara
                target[0], target[1], target[2],          // Punto objetivo
                up[0], up[1], up[2]                       // Vector arriba
        );
    }

    /**
     * Calcula MVP = Projection * View * Model
     */
    public void computeMvp(float[] modelMatrix, float[] outMvp) {
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(outMvp, 0, projMatrix, 0, tempMatrix, 0);
    }

    /**
     * Establecer objetivo de la cámara
     */
    public void setTarget(float x, float y, float z) {
        target[0] = x;
        target[1] = y;
        target[2] = z;
        updateViewMatrix();
    }

    /**
     * Establecer posición de la cámara
     */
    public void setPosition(float x, float y, float z) {
        position[0] = x;
        position[1] = y;
        position[2] = z;
        updateViewMatrix();
    }

    /**
     * Cambiar modo de perspectiva
     */
    public void setMode(CameraMode mode) {
        currentMode = mode;

        // Configurar parámetros según el modo
        switch (mode) {
            case PERSPECTIVE_3_4:
                // Vista 3/4 clásica - buena para ver toda la escena
                position[0] = 4f;
                position[1] = 3f;
                position[2] = 6f;
                target[0] = 0f;
                target[1] = 0f;
                target[2] = 0f;
                up[0] = 0f;
                up[1] = 1f;
                up[2] = 0f;
                fov = 60f;
                break;

            case PERSPECTIVE_FRONT:
                // Vista frontal directa
                position[0] = 0f;
                position[1] = 0f;
                position[2] = 8f;
                target[0] = 0f;
                target[1] = 0f;
                target[2] = 0f;
                up[0] = 0f;
                up[1] = 1f;
                up[2] = 0f;
                fov = 50f;
                break;

            case PERSPECTIVE_TOP:
                // Vista desde arriba
                position[0] = 0f;
                position[1] = 10f;
                position[2] = 0.1f;  // Ligeramente desplazado para evitar gimbal lock
                target[0] = 0f;
                target[1] = 0f;
                target[2] = 0f;
                up[0] = 0f;
                up[1] = 0f;
                up[2] = -1f;
                fov = 45f;
                break;

            case PERSPECTIVE_DRAMATIC:
                // Vista dramática desde abajo
                position[0] = 3f;
                position[1] = -2f;
                position[2] = 5f;
                target[0] = 0f;
                target[1] = 0f;
                target[2] = 0f;
                up[0] = 0f;
                up[1] = 1f;
                up[2] = 0f;
                fov = 70f;
                break;
        }

        updateViewMatrix();
    }

    /**
     * Obtener modo actual
     */
    public CameraMode getMode() {
        return currentMode;
    }

    /**
     * Establecer campo de visión (FOV)
     */
    public void setFOV(float fov) {
        this.fov = Math.max(20f, Math.min(120f, fov));
    }

    // ====== GETTERS PARA DEBUG ======
    public float[] getPosition() {
        return position.clone();
    }

    public float[] getTarget() {
        return target.clone();
    }

    public float getFOV() {
        return fov;
    }
}