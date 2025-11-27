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
        PERSPECTIVE_DRAMATIC, // Vista dramática desde abajo
        ORBIT_AUTO            // 🌀 Órbita automática alrededor del sol
    }

    // ====== ESTADO ACTUAL ======
    private CameraMode currentMode = CameraMode.PERSPECTIVE_3_4;

    // ====== PARÁMETROS DE POSICIÓN (FIJOS) ======
    private float[] position = {4f, 3f, 6f};      // Posición de la cámara
    private float[] target = {0f, 0f, 0f};        // Punto que mira la cámara
    private float[] up = {0f, 1f, 0f};            // Vector "arriba"

    // ====== SCREEN SHAKE (EFECTO DE IMPACTO) ======
    private float shakeIntensity = 0f;            // Intensidad actual del temblor
    private float shakeTimer = 0f;                // Tiempo restante del temblor
    private final java.util.Random shakeRandom = new java.util.Random();

    // ====== 🌀 ÓRBITA AUTOMÁTICA ======
    private float orbitAngle = 0f;                // Ángulo actual de la órbita (0-360°)
    private float orbitSpeed = 0.15f;             // Velocidad de rotación (radianes/segundo)
    private float orbitRadius = 10f;              // Radio de la órbita (distancia al centro)
    private float orbitHeightBase = 3f;           // Altura base de la órbita
    private float orbitHeightVariation = 2f;      // Variación de altura (sube/baja)
    private float orbitHeightSpeed = 0.08f;       // Velocidad del movimiento vertical

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

    // Log counter para no saturar
    private int updateLogCounter = 0;

    /**
     * Actualización - maneja órbita automática y screen shake
     */
    public void update(float deltaTime) {
        // ====== 🌀 ÓRBITA AUTOMÁTICA ======
        if (currentMode == CameraMode.ORBIT_AUTO) {
            // Incrementar ángulo de órbita
            orbitAngle += orbitSpeed * deltaTime;

            // Calcular posición en círculo (plano XZ)
            float x = (float) Math.cos(orbitAngle) * orbitRadius;
            float z = (float) Math.sin(orbitAngle) * orbitRadius;

            // Calcular altura con movimiento ondulante
            float heightWave = (float) Math.sin(orbitAngle * orbitHeightSpeed * 10f);
            float y = orbitHeightBase + heightWave * orbitHeightVariation;

            // Actualizar posición de la cámara
            position[0] = x;
            position[1] = y;
            position[2] = z;

            // Siempre mirar al centro (sol)
            target[0] = 0f;
            target[1] = 0f;
            target[2] = 0f;

            updateViewMatrix();
        }

        // ====== SCREEN SHAKE ======
        if (shakeTimer > 0) {
            shakeTimer -= deltaTime;
            if (shakeTimer <= 0) {
                shakeTimer = 0;
                shakeIntensity = 0;
                updateViewMatrix();  // Restaurar posición original
            } else {
                // Decay de la intensidad
                shakeIntensity *= 0.92f;
                updateViewMatrix();  // Aplicar shake
            }
        }
    }

    /**
     * Actualiza la matriz de vista (con screen shake si está activo)
     */
    private void updateViewMatrix() {
        // Aplicar shake si está activo
        float shakeX = 0f, shakeY = 0f, shakeZ = 0f;
        if (shakeIntensity > 0.01f) {
            shakeX = (shakeRandom.nextFloat() * 2f - 1f) * shakeIntensity;
            shakeY = (shakeRandom.nextFloat() * 2f - 1f) * shakeIntensity;
            shakeZ = (shakeRandom.nextFloat() * 2f - 1f) * shakeIntensity * 0.5f;  // Menos shake en Z
        }

        Matrix.setLookAtM(
                viewMatrix, 0,
                position[0] + shakeX, position[1] + shakeY, position[2] + shakeZ,
                target[0], target[1], target[2],
                up[0], up[1], up[2]
        );
    }

    /**
     * Activa el screen shake (efecto de impacto)
     * @param intensity Intensidad del temblor (0.0 - 1.0, recomendado 0.3-0.8)
     * @param duration Duración en segundos (recomendado 0.2-0.5)
     */
    public void triggerScreenShake(float intensity, float duration) {
        shakeIntensity = intensity * 0.5f;  // 0.5f = factor de escala para no ser muy extremo
        shakeTimer = duration;
    }

    /**
     * Calcula MVP = Projection * View * Model
     */
    public void computeMvp(float[] modelMatrix, float[] outMvp) {
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(outMvp, 0, projMatrix, 0, tempMatrix, 0);
    }

    // Cache para VP matrix (evita allocation cada frame)
    private final float[] vpMatrixCache = new float[16];

    /**
     * Obtiene la matriz View-Projection (VP = P * V)
     * Útil para instanced rendering donde solo pasamos VP al shader
     * @return Array de 16 floats con la matriz VP
     */
    public float[] getViewProjectionMatrix() {
        Matrix.multiplyMM(vpMatrixCache, 0, projMatrix, 0, viewMatrix, 0);
        return vpMatrixCache;
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
                // 🎬 Vista "Estratégica Elevada" - Tierra protagonista en el centro
                position[0] = 5f;      // Posición X (lateral derecha)
                position[1] = 4f;      // Posición Y (altura media-alta)
                position[2] = 7f;      // Posición Z (profundidad cercana)
                target[0] = 0f;        // Mirando al centro (donde está la Tierra)
                target[1] = 0f;
                target[2] = 0f;
                up[0] = 0f;
                up[1] = 1f;
                up[2] = 0f;
                fov = 60f;             // Campo de visión óptimo
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

            case ORBIT_AUTO:
                // 🌀 Órbita automática alrededor del sol (MUY PRONUNCIADA)
                orbitAngle = 0f;
                orbitSpeed = 0.5f;            // VELOCIDAD RÁPIDA (3x más rápido)
                orbitRadius = 7f;             // MÁS CERCA (era 10)
                orbitHeightBase = 2f;         // Más bajo
                orbitHeightVariation = 4f;    // MUCHA variación vertical (±4 unidades)
                orbitHeightSpeed = 0.08f;     // Velocidad de ondulación vertical
                target[0] = 0f;
                target[1] = 0f;
                target[2] = 0f;
                up[0] = 0f;
                up[1] = 1f;
                up[2] = 0f;
                fov = 60f;
                // Posición inicial se calcula en update()
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