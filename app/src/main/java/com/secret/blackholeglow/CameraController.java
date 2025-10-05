package com.secret.blackholeglow;

import android.opengl.Matrix;

/**
 * Sistema de Cámara Profesional con múltiples perspectivas de videojuego
 * Incluye: Primera Persona, Tercera Persona, Órbita, Top-Down, Cinemático
 */
public class CameraController {

    // ====== TIPOS DE CÁMARA ======
    public enum CameraMode {
        FIRST_PERSON,      // Vista FPS - dentro de la escena
        THIRD_PERSON,      // Detrás del objeto focal
        ORBIT_CAMERA,      // Órbita libre alrededor del centro
        TOP_DOWN,          // Vista desde arriba (como Blender)
        ISOMETRIC,         // Vista isométrica (45°)
        CINEMATIC,         // Movimiento automático cinematográfico
        FREE_CAMERA        // Cámara libre tipo editor
    }

    // ====== ESTADO ACTUAL ======
    private CameraMode currentMode = CameraMode.ORBIT_CAMERA;
    private boolean smoothTransitions = true;

    // ====== PARÁMETROS DE POSICIÓN ======
    private float[] position = {0f, 5f, 10f};      // Posición actual de la cámara
    private float[] target = {0f, 0f, 0f};         // Punto que mira la cámara
    private float[] up = {0f, 1f, 0f};             // Vector "arriba"

    // ====== PARÁMETROS DE ORIENTACIÓN ======
    private float yaw = 0f;           // Rotación horizontal (grados)
    private float pitch = 30f;         // Rotación vertical (grados)
    private float roll = 0f;           // Rotación de ladeo (grados)

    // ====== PARÁMETROS DE MOVIMIENTO ======
    private float distance = 10f;      // Distancia al objetivo
    private float targetDistance = 10f;
    private float orbitSpeed = 20f;    // Velocidad de órbita (grados/segundo)
    private float moveSpeed = 5f;      // Velocidad de movimiento (unidades/segundo)

    // ====== PARÁMETROS DE PROYECCIÓN ======
    private float fov = 60f;           // Campo de visión (perspectiva)
    private float nearPlane = 0.1f;
    private float farPlane = 100f;
    private float aspect = 1f;
    private boolean usePerspective = true;  // true = perspectiva, false = ortográfica

    // ====== MATRICES ======
    private final float[] viewMatrix = new float[16];
    private final float[] projMatrix = new float[16];
    private final float[] tempMatrix = new float[16];

    // ====== SUAVIZADO ======
    private float smoothFactor = 0.15f;
    private float[] smoothPosition = {0f, 5f, 10f};
    private float[] smoothTarget = {0f, 0f, 0f};
    private float smoothYaw = 0f;
    private float smoothPitch = 30f;

    // ====== ANIMACIÓN CINEMATOGRÁFICA ======
    private float cinematicTime = 0f;
    private float cinematicSpeed = 0.5f;

    // ====== LÍMITES ======
    private static final float MIN_PITCH = -85f;
    private static final float MAX_PITCH = 85f;
    private static final float MIN_DISTANCE = 2f;
    private static final float MAX_DISTANCE = 50f;
    private static final float MIN_FOV = 20f;
    private static final float MAX_FOV = 120f;

    /**
     * Constructor - Inicializa con vista por defecto
     */
    public CameraController() {
        updateViewMatrix();
    }

    /**
     * Actualiza la proyección cuando cambia el viewport
     */
    public void updateProjection(int width, int height) {
        aspect = (float) width / height;

        if (usePerspective) {
            // Proyección perspectiva (3D real)
            Matrix.perspectiveM(
                    projMatrix, 0,
                    fov,        // Campo de visión
                    aspect,     // Aspecto
                    nearPlane,  // Plano cercano
                    farPlane    // Plano lejano
            );
        } else {
            // Proyección ortográfica (sin perspectiva)
            float halfHeight = distance * 0.5f;
            float halfWidth = halfHeight * aspect;
            Matrix.orthoM(
                    projMatrix, 0,
                    -halfWidth, halfWidth,
                    -halfHeight, halfHeight,
                    nearPlane, farPlane
            );
        }
    }

    /**
     * Actualización principal - llamar cada frame
     */
    public void update(float deltaTime) {
        // Actualizar según el modo actual
        switch (currentMode) {
            case FIRST_PERSON:
                updateFirstPerson(deltaTime);
                break;
            case THIRD_PERSON:
                updateThirdPerson(deltaTime);
                break;
            case ORBIT_CAMERA:
                updateOrbitCamera(deltaTime);
                break;
            case TOP_DOWN:
                updateTopDown(deltaTime);
                break;
            case ISOMETRIC:
                updateIsometric(deltaTime);
                break;
            case CINEMATIC:
                updateCinematic(deltaTime);
                break;
            case FREE_CAMERA:
                updateFreeCamera(deltaTime);
                break;
        }

        // Aplicar suavizado si está activo
        if (smoothTransitions) {
            applySmoothTransitions();
        }

        // Actualizar matriz de vista
        updateViewMatrix();
    }

    /**
     * Modo Primera Persona - Vista desde dentro de la escena
     */
    private void updateFirstPerson(float dt) {
        // Calcular posición basada en yaw/pitch
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        // Posición fija cerca del suelo
        position[0] = 0f;
        position[1] = 1.5f;  // Altura de ojos humanos
        position[2] = 0f;

        // Calcular hacia dónde mira
        target[0] = position[0] + (float)(Math.sin(yawRad) * Math.cos(pitchRad));
        target[1] = position[1] + (float)Math.sin(pitchRad);
        target[2] = position[2] + (float)(Math.cos(yawRad) * Math.cos(pitchRad));

        // FOV más amplio para FPS
        fov = 90f;
        usePerspective = true;
    }

    /**
     * Modo Tercera Persona - Detrás del personaje/objeto
     */
    private void updateThirdPerson(float dt) {
        // Suavizar distancia
        distance = lerp(distance, targetDistance, smoothFactor);

        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        // Posición detrás y arriba del objetivo
        position[0] = target[0] - distance * (float)(Math.sin(yawRad) * Math.cos(pitchRad));
        position[1] = target[1] + distance * (float)Math.sin(pitchRad) + 2f;
        position[2] = target[2] - distance * (float)(Math.cos(yawRad) * Math.cos(pitchRad));

        // FOV estándar
        fov = 60f;
        usePerspective = true;
    }

    /**
     * Modo Órbita - Gira alrededor del centro
     */
    private void updateOrbitCamera(float dt) {
        // Auto-rotar
        yaw += orbitSpeed * dt;
        if (yaw > 360f) yaw -= 360f;

        // Suavizar distancia
        distance = lerp(distance, targetDistance, smoothFactor);

        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        // Calcular posición orbital
        position[0] = target[0] + distance * (float)(Math.sin(yawRad) * Math.cos(pitchRad));
        position[1] = target[1] + distance * (float)Math.sin(pitchRad);
        position[2] = target[2] + distance * (float)(Math.cos(yawRad) * Math.cos(pitchRad));

        usePerspective = true;
    }

    /**
     * Modo Top-Down - Vista desde arriba (como en Blender)
     */
    private void updateTopDown(float dt) {
        // Posición directamente arriba
        position[0] = target[0];
        position[1] = target[1] + distance;
        position[2] = target[2];

        // Mirar hacia abajo
        up[0] = 0f;
        up[1] = 0f;
        up[2] = -1f;

        // Usar ortográfica para vista plana
        usePerspective = false;
    }

    /**
     * Modo Isométrico - Vista 3/4
     */
    private void updateIsometric(float dt) {
        // Ángulo isométrico clásico (30°-45°)
        float isoAngle = 45f;
        float isoElevation = 35.264f;  // Ángulo isométrico real

        float angleRad = (float) Math.toRadians(isoAngle);
        float elevRad = (float) Math.toRadians(isoElevation);

        position[0] = target[0] + distance * (float)(Math.sin(angleRad) * Math.cos(elevRad));
        position[1] = target[1] + distance * (float)Math.sin(elevRad);
        position[2] = target[2] + distance * (float)(Math.cos(angleRad) * Math.cos(elevRad));

        // Isométrico usa ortográfica
        usePerspective = false;
    }

    /**
     * Modo Cinemático - Movimiento automático espectacular
     */
    private void updateCinematic(float dt) {
        cinematicTime += dt * cinematicSpeed;

        // Trayectoria en figura 8
        float t = cinematicTime;
        float radius = 15f;

        position[0] = radius * (float)Math.sin(t);
        position[1] = 5f + 3f * (float)Math.sin(t * 2f);
        position[2] = radius * (float)Math.sin(t * 2f) / 2f;

        // Mirar siempre al centro con algo de adelanto
        target[0] = 2f * (float)Math.sin(t + 0.5f);
        target[1] = 0f;
        target[2] = 2f * (float)Math.cos(t + 0.5f);

        // FOV dinámico para dramatismo
        fov = 50f + 20f * (float)Math.sin(t * 3f);
        usePerspective = true;
    }

    /**
     * Modo Cámara Libre - Control total
     */
    private void updateFreeCamera(float dt) {
        // La posición se controla externamente
        // Solo actualizar orientación si es necesario
        usePerspective = true;
    }

    /**
     * Aplica suavizado a las transiciones
     */
    private void applySmoothTransitions() {
        // Suavizar posición
        smoothPosition[0] = lerp(smoothPosition[0], position[0], smoothFactor);
        smoothPosition[1] = lerp(smoothPosition[1], position[1], smoothFactor);
        smoothPosition[2] = lerp(smoothPosition[2], position[2], smoothFactor);

        // Suavizar objetivo
        smoothTarget[0] = lerp(smoothTarget[0], target[0], smoothFactor);
        smoothTarget[1] = lerp(smoothTarget[1], target[1], smoothFactor);
        smoothTarget[2] = lerp(smoothTarget[2], target[2], smoothFactor);

        // Suavizar rotación
        smoothYaw = lerpAngle(smoothYaw, yaw, smoothFactor);
        smoothPitch = lerp(smoothPitch, pitch, smoothFactor);
    }

    /**
     * Actualiza la matriz de vista
     */
    private void updateViewMatrix() {
        float[] pos = smoothTransitions ? smoothPosition : position;
        float[] tgt = smoothTransitions ? smoothTarget : target;

        Matrix.setLookAtM(
                viewMatrix, 0,
                pos[0], pos[1], pos[2],    // Posición de cámara
                tgt[0], tgt[1], tgt[2],    // Punto objetivo
                up[0], up[1], up[2]        // Vector arriba
        );
    }

    /**
     * Calcula MVP = Projection * View * Model
     */
    public void computeMvp(float[] modelMatrix, float[] outMvp) {
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(outMvp, 0, projMatrix, 0, tempMatrix, 0);
    }

    // ====== CONTROLES DE USUARIO ======

    /**
     * Rotar cámara con el dedo (orbit/free camera)
     */
    public void rotate(float deltaX, float deltaY) {
        yaw += deltaX * 0.5f;
        pitch = clamp(pitch - deltaY * 0.5f, MIN_PITCH, MAX_PITCH);
    }

    /**
     * Zoom in/out
     */
    public void zoom(float delta) {
        targetDistance = clamp(targetDistance - delta, MIN_DISTANCE, MAX_DISTANCE);

        // Ajustar FOV para efecto de zoom en perspectiva
        if (usePerspective) {
            fov = clamp(fov - delta * 5f, MIN_FOV, MAX_FOV);
        }
    }

    /**
     * Mover cámara (free camera)
     */
    public void move(float forward, float right, float up) {
        if (currentMode == CameraMode.FREE_CAMERA) {
            float yawRad = (float) Math.toRadians(yaw);

            // Movimiento adelante/atrás
            position[0] += forward * Math.sin(yawRad);
            position[2] += forward * Math.cos(yawRad);

            // Movimiento lateral
            position[0] += right * Math.cos(yawRad);
            position[2] -= right * Math.sin(yawRad);

            // Movimiento vertical
            position[1] += up;
        }
    }

    /**
     * Establecer objetivo de la cámara
     */
    public void setTarget(float x, float y, float z) {
        target[0] = x;
        target[1] = y;
        target[2] = z;
    }

    /**
     * Establecer posición de la cámara
     */
    public void setPosition(float x, float y, float z) {
        position[0] = x;
        position[1] = y;
        position[2] = z;
    }

    /**
     * Cambiar modo de cámara
     */
    public void setMode(CameraMode mode) {
        currentMode = mode;

        // Configurar parámetros específicos del modo
        switch (mode) {
            case FIRST_PERSON:
                smoothFactor = 0.3f;
                break;
            case THIRD_PERSON:
                targetDistance = 8f;
                pitch = 25f;
                break;
            case TOP_DOWN:
                pitch = 90f;
                targetDistance = 15f;
                break;
            case ISOMETRIC:
                smoothFactor = 0.1f;
                break;
            case CINEMATIC:
                cinematicTime = 0f;
                break;
        }
    }

    /**
     * Obtener modo actual
     */
    public CameraMode getMode() {
        return currentMode;
    }

    /**
     * Ciclar entre modos
     */
    public void nextMode() {
        CameraMode[] modes = CameraMode.values();
        int nextIndex = (currentMode.ordinal() + 1) % modes.length;
        setMode(modes[nextIndex]);
    }

    // ====== UTILIDADES ======

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private float lerpAngle(float a, float b, float t) {
        float diff = b - a;
        while (diff > 180f) diff -= 360f;
        while (diff < -180f) diff += 360f;
        return a + diff * t;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    // ====== GETTERS PARA DEBUG ======

    public float[] getPosition() { return position.clone(); }
    public float[] getTarget() { return target.clone(); }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public float getDistance() { return distance; }
    public float getFOV() { return fov; }
    public boolean isPerspective() { return usePerspective; }
}