package com.secret.blackholeglow.filament;

import android.util.Log;
import com.google.android.filament.Camera;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 🎥 CHRISTMAS CAMERA CONTROLLER - Cámara estilo FPS/COD Mobile
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Cámara en primera persona que recorre el terreno nevado como si fuéramos
 * un personaje caminando por el mundo 3D.
 *
 * Características:
 * - Altura de ojos humanos (1.7m en escala del mundo)
 * - Recorrido automático por waypoints del terreno
 * - Head bob (movimiento natural al caminar)
 * - Smooth look-at hacia puntos de interés
 * - FOV dinámico (más amplio al correr)
 *
 * @author Claude + Lalo
 * @version 1.0 - Diciembre 2024
 */
public class ChristmasCameraController {
    private static final String TAG = "XmasCamera";

    // ═══════════════════════════════════════════════════════════════════
    // 📐 CONFIGURACIÓN DE ALTURA Y ESCALA
    // ═══════════════════════════════════════════════════════════════════

    // Altura de ojos humanos relativa al terreno
    // MUY PEQUEÑO = montañas se ven GIGANTES
    private static final float EYE_HEIGHT = 0.04f;   // Somos pequeñitos en el mundo
    private static final float CROUCH_HEIGHT = 0.02f;  // Agachado

    // FOV (Field of View)
    private static final float FOV_NORMAL = 70.0f;   // FOV amplio para inmersión
    private static final float FOV_SPRINT = 80.0f;   // FOV corriendo
    private static final float FOV_AIM = 50.0f;      // FOV apuntando/zoom

    // ═══════════════════════════════════════════════════════════════════
    // 🚶 MOVIMIENTO Y VELOCIDAD
    // ═══════════════════════════════════════════════════════════════════

    private static final float WALK_SPEED = 0.08f;   // Caminar lento y natural
    private static final float SPRINT_SPEED = 0.15f; // Correr
    private static final float LOOK_SPEED = 0.5f;    // Velocidad de rotación de vista

    // Head bob (movimiento de cabeza al caminar)
    private static final float HEAD_BOB_AMOUNT = 0.005f;  // Muy sutil
    private static final float HEAD_BOB_SPEED = 6.0f;     // Frecuencia natural

    // ═══════════════════════════════════════════════════════════════════
    // 🗺️ RUTA DE LA CÁMARA - Recorrido por el terreno
    // ═══════════════════════════════════════════════════════════════════

    // Waypoints del recorrido de cámara (X, Y_terreno, Z)
    // La cámara seguirá este camino automáticamente
    private static final float[][] CAMERA_PATH = {
        // ═══ INICIO - Borde del terreno ═══
        { 0.0f, 0.0f,  0.5f},    // 0: Inicio, mirando al terreno
        { 0.5f, 0.0f,  1.5f},    // 1: Entrando al terreno
        { 0.0f, 0.0f,  2.5f},    // 2: Avanzando

        // ═══ EXPLORANDO EL TERRENO ═══
        {-0.5f, 0.05f, 3.5f},    // 3: Hacia la izquierda
        { 0.0f, 0.1f,  4.5f},    // 4: Centro, subiendo colina
        { 0.5f, 0.15f, 5.0f},    // 5: Cima de colina pequeña

        // ═══ HACIA LAS MONTAÑAS ═══
        { 0.0f, 0.1f,  6.0f},    // 6: Bajando
        {-0.3f, 0.05f, 7.0f},    // 7: Acercándose a montañas
        { 0.3f, 0.0f,  7.5f},    // 8: Vista panorámica

        // ═══ REGRESO ═══
        { 0.0f, 0.05f, 6.5f},    // 9: Volviendo
        { 0.5f, 0.1f,  5.5f},    // 10: Por otro lado
        {-0.5f, 0.05f, 4.0f},    // 11: Regresando
        { 0.0f, 0.0f,  2.0f},    // 12: Casi al inicio
    };

    // Puntos de interés para mirar (Santa, árbol, montañas)
    private static final float[][] LOOK_AT_POINTS = {
        { 0.0f, 0.3f, 5.0f},     // Centro del terreno (Santa)
        {-1.5f, 0.5f, 3.0f},     // Árbol de navidad
        { 0.0f, 1.0f, 10.0f},    // Montañas lejanas
    };

    // ═══════════════════════════════════════════════════════════════════
    // 📊 ESTADO DE LA CÁMARA
    // ═══════════════════════════════════════════════════════════════════

    private Camera camera;
    private float aspectRatio = 9f/16f;

    // Posición actual
    private float posX = 0.0f;
    private float posY = 0.0f;  // Altura del terreno en este punto
    private float posZ = 0.5f;

    // Dirección de vista (hacia dónde miramos)
    private float lookX = 0.0f;
    private float lookY = 0.3f;
    private float lookZ = 5.0f;

    // Estado de movimiento
    private int currentPathIndex = 0;
    private int targetPathIndex = 1;
    private float pathProgress = 0.0f;  // 0.0 a 1.0 entre waypoints

    // Estado de vista
    private int currentLookTarget = 0;
    private float lookTransitionProgress = 1.0f;  // 1.0 = ya llegó al target

    // Efectos
    private float currentFOV = FOV_NORMAL;
    private float targetFOV = FOV_NORMAL;
    private float headBobPhase = 0.0f;
    private boolean isWalking = true;
    private boolean isSprinting = false;

    // Tiempo
    private float totalTime = 0.0f;

    // ═══════════════════════════════════════════════════════════════════
    // 🎮 MODOS DE CÁMARA
    // ═══════════════════════════════════════════════════════════════════

    public enum CameraMode {
        AUTO_WALK,      // Recorrido automático por el terreno
        STATIC,         // Posición fija mirando a Santa
        CINEMATIC,      // Movimientos suaves cinematográficos
        FOLLOW_SANTA    // Seguir a Santa desde atrás
    }

    private CameraMode currentMode = CameraMode.AUTO_WALK;

    // ═══════════════════════════════════════════════════════════════════
    // 🏗️ CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════

    public ChristmasCameraController(Camera camera) {
        this.camera = camera;
        Log.d(TAG, "🎥 ChristmasCameraController inicializado");
    }

    // ═══════════════════════════════════════════════════════════════════
    // 🔄 UPDATE - Llamar cada frame
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Actualiza la cámara cada frame
     * @param deltaTime Tiempo desde el último frame en segundos
     */
    public void update(float deltaTime) {
        totalTime += deltaTime;

        switch (currentMode) {
            case AUTO_WALK:
                updateAutoWalk(deltaTime);
                break;
            case STATIC:
                // No se mueve, solo aplica head bob si está "respirando"
                updateBreathing(deltaTime);
                break;
            case CINEMATIC:
                updateCinematic(deltaTime);
                break;
            case FOLLOW_SANTA:
                // Se actualizará desde afuera con setSantaPosition()
                break;
        }

        // Actualizar FOV suavemente
        updateFOV(deltaTime);

        // Aplicar head bob
        updateHeadBob(deltaTime);

        // Aplicar cámara
        applyCamera();
    }

    // ═══════════════════════════════════════════════════════════════════
    // 🚶 AUTO WALK - Recorrido automático
    // ═══════════════════════════════════════════════════════════════════

    private void updateAutoWalk(float deltaTime) {
        float speed = isSprinting ? SPRINT_SPEED : WALK_SPEED;

        // Obtener posiciones actual y objetivo
        float[] current = CAMERA_PATH[currentPathIndex];
        float[] target = CAMERA_PATH[targetPathIndex];

        // Calcular distancia al objetivo
        float dx = target[0] - current[0];
        float dy = target[1] - current[1];
        float dz = target[2] - current[2];
        float distance = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);

        // Avanzar en el camino
        if (distance > 0.01f) {
            pathProgress += (speed * deltaTime) / distance;
        }

        // Interpolar posición
        posX = lerp(current[0], target[0], pathProgress);
        posY = lerp(current[1], target[1], pathProgress) + EYE_HEIGHT;
        posZ = lerp(current[2], target[2], pathProgress);

        // Mirar hacia adelante en la dirección del movimiento
        // con un poco de anticipación
        float lookAhead = Math.min(pathProgress + 0.3f, 1.0f);
        float nextX = lerp(current[0], target[0], lookAhead);
        float nextZ = lerp(current[2], target[2], lookAhead);

        // Mirar un poco más adelante en Z y ligeramente arriba
        lookX = lerp(lookX, nextX, deltaTime * 2.0f);
        lookY = lerp(lookY, posY + 0.1f, deltaTime * 2.0f);
        lookZ = lerp(lookZ, nextZ + 2.0f, deltaTime * 2.0f);

        // Llegamos al waypoint?
        if (pathProgress >= 1.0f) {
            currentPathIndex = targetPathIndex;
            targetPathIndex = (targetPathIndex + 1) % CAMERA_PATH.length;
            pathProgress = 0.0f;

            Log.d(TAG, "🚶 Waypoint " + currentPathIndex + " → " + targetPathIndex);

            // Ocasionalmente cambiar punto de interés
            if (Math.random() < 0.3) {
                currentLookTarget = (currentLookTarget + 1) % LOOK_AT_POINTS.length;
            }
        }

        isWalking = true;
    }

    // ═══════════════════════════════════════════════════════════════════
    // 🎬 CINEMATIC - Movimiento cinematográfico suave
    // ═══════════════════════════════════════════════════════════════════

    private void updateCinematic(float deltaTime) {
        // Movimiento circular lento alrededor del terreno
        float radius = 3.0f;
        float height = 0.8f;
        float speed = 0.1f;

        float angle = totalTime * speed;

        posX = (float) Math.sin(angle) * radius;
        posY = height + (float) Math.sin(angle * 2) * 0.2f;
        posZ = 3.0f + (float) Math.cos(angle) * radius * 0.5f;

        // Siempre mirar al centro
        lookX = 0.0f;
        lookY = 0.3f;
        lookZ = 5.0f;

        isWalking = false;
    }

    // ═══════════════════════════════════════════════════════════════════
    // 🫁 BREATHING - Efecto de respiración cuando está quieto
    // ═══════════════════════════════════════════════════════════════════

    private void updateBreathing(float deltaTime) {
        // Pequeño movimiento vertical de respiración
        float breathAmount = 0.005f;
        float breathSpeed = 2.0f;
        posY += (float) Math.sin(totalTime * breathSpeed) * breathAmount;
    }

    // ═══════════════════════════════════════════════════════════════════
    // 👀 HEAD BOB - Movimiento de cabeza al caminar
    // ═══════════════════════════════════════════════════════════════════

    private float headBobOffsetY = 0.0f;
    private float headBobOffsetX = 0.0f;

    private void updateHeadBob(float deltaTime) {
        if (isWalking) {
            float bobSpeed = isSprinting ? HEAD_BOB_SPEED * 1.5f : HEAD_BOB_SPEED;
            headBobPhase += deltaTime * bobSpeed;

            // Movimiento vertical (arriba-abajo)
            headBobOffsetY = (float) Math.sin(headBobPhase) * HEAD_BOB_AMOUNT;

            // Movimiento horizontal sutil (lado a lado)
            headBobOffsetX = (float) Math.sin(headBobPhase * 0.5f) * HEAD_BOB_AMOUNT * 0.5f;
        } else {
            // Reducir gradualmente el bob
            headBobOffsetY = lerp(headBobOffsetY, 0, deltaTime * 5.0f);
            headBobOffsetX = lerp(headBobOffsetX, 0, deltaTime * 5.0f);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 🔭 FOV - Campo de visión dinámico
    // ═══════════════════════════════════════════════════════════════════

    private void updateFOV(float deltaTime) {
        // Transición suave del FOV
        currentFOV = lerp(currentFOV, targetFOV, deltaTime * 3.0f);
    }

    // ═══════════════════════════════════════════════════════════════════
    // 📷 APLICAR CÁMARA
    // ═══════════════════════════════════════════════════════════════════

    private void applyCamera() {
        if (camera == null) return;

        // Aplicar FOV
        camera.setProjection(currentFOV, aspectRatio, 0.05, 100.0, Camera.Fov.VERTICAL);

        // Posición final con head bob
        float finalX = posX + headBobOffsetX;
        float finalY = posY + headBobOffsetY;
        float finalZ = posZ;

        // Aplicar lookAt
        camera.lookAt(
            finalX, finalY, finalZ,      // eye position
            lookX, lookY, lookZ,          // look at point
            0.0, 1.0, 0.0                 // up vector
        );
    }

    // ═══════════════════════════════════════════════════════════════════
    // 🎮 CONTROLES PÚBLICOS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Cambiar modo de cámara
     */
    public void setMode(CameraMode mode) {
        this.currentMode = mode;
        Log.d(TAG, "🎥 Modo: " + mode.name());
    }

    /**
     * Activar/desactivar sprint
     */
    public void setSprinting(boolean sprinting) {
        this.isSprinting = sprinting;
        this.targetFOV = sprinting ? FOV_SPRINT : FOV_NORMAL;
    }

    /**
     * Para modo FOLLOW_SANTA - actualizar posición de Santa
     */
    public void setSantaPosition(float santaX, float santaY, float santaZ) {
        if (currentMode == CameraMode.FOLLOW_SANTA) {
            // Posicionarse detrás de Santa
            float followDistance = 1.5f;
            float followHeight = 0.5f;

            posX = santaX;
            posY = santaY + followHeight;
            posZ = santaZ - followDistance;

            lookX = santaX;
            lookY = santaY + 0.2f;
            lookZ = santaZ;
        }
    }

    /**
     * Mirar hacia un punto específico
     */
    public void lookAt(float x, float y, float z) {
        lookX = x;
        lookY = y;
        lookZ = z;
    }

    /**
     * Establecer posición manual
     */
    public void setPosition(float x, float y, float z) {
        posX = x;
        posY = y;
        posZ = z;
    }

    /**
     * Actualizar aspect ratio cuando cambia el tamaño de pantalla
     */
    public void setAspectRatio(float aspect) {
        this.aspectRatio = aspect;
    }

    /**
     * Obtener posición actual
     */
    public float[] getPosition() {
        return new float[] { posX, posY, posZ };
    }

    /**
     * Obtener dirección de vista
     */
    public float[] getLookAt() {
        return new float[] { lookX, lookY, lookZ };
    }

    // ═══════════════════════════════════════════════════════════════════
    // 🔧 UTILIDADES
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Interpolación lineal
     */
    private float lerp(float a, float b, float t) {
        return a + (b - a) * Math.min(Math.max(t, 0), 1);
    }

    /**
     * Interpolación suave (ease in-out)
     */
    private float smoothstep(float a, float b, float t) {
        t = Math.min(Math.max(t, 0), 1);
        t = t * t * (3 - 2 * t);
        return a + (b - a) * t;
    }
}
