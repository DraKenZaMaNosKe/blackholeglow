package com.secret.blackholeglow;

import android.util.Log;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   ✈️ FlightController - Controla el patrón de vuelo de la nave          ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  GROK SUGGESTION: Separar lógica de vuelo para modularidad              ║
 * ║                                                                          ║
 * ║  ESTADOS:                                                                ║
 * ║  • HOVERING: Planea en el origen con drift lateral                      ║
 * ║  • FIGURE_8: Vuelo en figura de 8 (lemniscata)                         ║
 * ║                                                                          ║
 * ║  OUTPUTS: x, y, z, scale, rotationX/Y/Z, engineGlow                     ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class FlightController {
    private static final String TAG = "FlightController";

    // ═══════════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN DE VUELO (externalizables a SharedPreferences)
    // ═══════════════════════════════════════════════════════════════════════

    // Posición de ORIGEN
    private float originX = -2.5f;
    private float originY = -4.9f;
    private float originZ = -2.5f;
    private float originScale = 0.8f;

    // Dimensiones del figura-8
    private float figure8Width = 0.8f;
    private float figure8Height = 3.5f;
    private float figure8Depth = 4.0f;

    // Duración del figura-8
    private float figure8DurationMin = 20f;
    private float figure8DurationMax = 35f;
    private float currentFigure8Duration = 25f;

    // Duración del hover
    private float hoverDurationMin = 4f;
    private float hoverDurationMax = 10f;
    private float currentHoverDuration = 6f;

    // Parámetros de hover
    private float hoverDriftAmplitude = 0.75f;
    private float hoverDriftSpeed = 0.5f;

    // Rotación base
    private float baseRotationY = 304.7f;

    // Roll factor
    private float rollFactor = 10f;

    // ═══════════════════════════════════════════════════════════════════════
    // ESTADO INTERNO
    // ═══════════════════════════════════════════════════════════════════════

    private static final int STATE_HOVERING = 0;
    private static final int STATE_FIGURE_8 = 1;
    private int currentState = STATE_HOVERING;

    private float time = 0f;
    private float hoverTime = 0f;
    private float figure8Phase = 0f;
    private float targetRotationY;

    private final java.util.Random random = new java.util.Random();

    // ═══════════════════════════════════════════════════════════════════════
    // OUTPUTS (leídos por TravelingShip)
    // ═══════════════════════════════════════════════════════════════════════

    public float x, y, z;
    public float scale;
    public float rotationX, rotationY, rotationZ;
    public float engineGlow = 1.0f;

    // ═══════════════════════════════════════════════════════════════════════
    // GYROSCOPE INPUT
    // ═══════════════════════════════════════════════════════════════════════

    private boolean gyroEnabled = false;
    private float gyroTiltX = 0f;
    private float gyroTiltY = 0f;
    private float gyroXInfluence = 1.5f;
    private float gyroRollInfluence = 15f;

    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════

    public FlightController() {
        // Inicializar con valores de origen
        x = originX;
        y = originY;
        z = originZ;
        scale = originScale;
        rotationY = baseRotationY;
        targetRotationY = baseRotationY;

        Log.d(TAG, "✈️ FlightController inicializado");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UPDATE PRINCIPAL
    // ═══════════════════════════════════════════════════════════════════════

    public void update(float deltaTime) {
        time += deltaTime;

        switch (currentState) {
            case STATE_HOVERING:
                hoverTime += deltaTime;
                updateHovering(deltaTime);
                if (hoverTime >= currentHoverDuration) {
                    transitionToFigure8();
                }
                break;

            case STATE_FIGURE_8:
                float phaseSpeed = (float)(2.0 * Math.PI) / currentFigure8Duration;
                figure8Phase += deltaTime * phaseSpeed;
                updateFigure8(deltaTime);
                if (figure8Phase >= 2.0f * Math.PI) {
                    transitionToHover();
                }
                break;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TRANSICIONES DE ESTADO
    // ═══════════════════════════════════════════════════════════════════════

    private void transitionToFigure8() {
        hoverTime = 0f;
        figure8Phase = 0f;
        currentFigure8Duration = figure8DurationMin +
            random.nextFloat() * (figure8DurationMax - figure8DurationMin);
        currentState = STATE_FIGURE_8;
        Log.d(TAG, "🎱 Iniciando figura 8 - Duración: " +
            String.format("%.1f", currentFigure8Duration) + "s");
    }

    private void transitionToHover() {
        figure8Phase = 0f;
        currentState = STATE_HOVERING;
        targetRotationY = baseRotationY;
        currentHoverDuration = hoverDurationMin +
            random.nextFloat() * (hoverDurationMax - hoverDurationMin);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HOVERING UPDATE
    // ═══════════════════════════════════════════════════════════════════════

    private void updateHovering(float deltaTime) {
        z = originZ;
        scale = originScale;
        rotationY = lerpAngle(rotationY, baseRotationY, deltaTime * 3f);

        // Cálculos cacheados
        float t1 = time * hoverDriftSpeed;
        float sinT1 = (float) Math.sin(t1);
        float cosT1 = (float) Math.cos(t1);
        float sinT2 = (float) Math.sin(time * 1.2f);
        float sinT3 = (float) Math.sin(time * 4.0f);

        // Drift lateral
        float drift = sinT1 * 0.8f + (float) Math.sin(t1 * 1.5f) * 0.2f;
        float baseX = originX + drift * hoverDriftAmplitude;

        // Giroscopio
        float gyroOffsetX = 0f;
        float gyroRoll = 0f;
        if (gyroEnabled) {
            gyroOffsetX = gyroTiltX * gyroXInfluence;
            gyroRoll = gyroTiltX * gyroRollInfluence;
        }

        x = baseX + gyroOffsetX;
        y = originY + sinT2 * 0.05f;

        // Roll
        float driftVelocity = cosT1 * hoverDriftSpeed * 0.9f;
        float mainRoll = driftVelocity * rollFactor * 2.5f;
        float stabilization = sinT3 * 3.5f;
        rotationZ = 4.0f + mainRoll + stabilization + gyroRoll;

        // Pitch
        rotationX = (float) Math.sin(time * 0.7f) * 4.0f;

        // Engine glow
        engineGlow = 1.0f + sinT3 * 0.15f;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FIGURE-8 UPDATE
    // ═══════════════════════════════════════════════════════════════════════

    private void updateFigure8(float deltaTime) {
        float t = figure8Phase;

        float sinT = (float) Math.sin(t);
        float cosT = (float) Math.cos(t);
        float sinHalfT = (float) Math.sin(t * 0.5f);
        float cosHalfT = (float) Math.cos(t * 0.5f);

        // Posición del 8
        x = originX + figure8Width * sinT;
        y = originY + figure8Height * sinHalfT;
        z = originZ - figure8Depth * sinHalfT;

        // Escala según distancia
        scale = originScale - (originScale - 0.15f) * sinHalfT;

        // Derivadas para dirección
        float dx = figure8Width * cosT;
        float dy = (figure8Height * 0.5f) * cosHalfT;

        // Rotación Y según dirección
        targetRotationY = (dy >= 0) ? baseRotationY : baseRotationY + 180f;
        rotationY = lerpAngle(rotationY, targetRotationY, deltaTime * 4f);

        // Banking
        float sinStab = (float) Math.sin(time * 4.0f);
        rotationZ = 4.0f + dx * 30f + sinStab * 2.5f;

        // Pitch
        rotationX = dy * 8f + sinStab * 0.5f;

        // Engine glow
        engineGlow = 1.3f + Math.abs(dx) * 0.5f + sinStab * 0.1f;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════════════════

    private float lerpAngle(float from, float to, float t) {
        float diff = to - from;
        while (diff > 180f) diff -= 360f;
        while (diff < -180f) diff += 360f;
        return from + diff * Math.min(1f, t);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SETTERS PARA CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════════════

    public void setGyroEnabled(boolean enabled) {
        this.gyroEnabled = enabled;
    }

    public void setTiltInput(float tiltX, float tiltY) {
        this.gyroTiltX = tiltX;
        this.gyroTiltY = tiltY;
    }

    public boolean isGyroEnabled() {
        return gyroEnabled;
    }

    // Setters para configuración externa (SharedPreferences)
    public void setOrigin(float x, float y, float z, float scale) {
        this.originX = x;
        this.originY = y;
        this.originZ = z;
        this.originScale = scale;
    }

    public void setFigure8Dimensions(float width, float height, float depth) {
        this.figure8Width = width;
        this.figure8Height = height;
        this.figure8Depth = depth;
    }

    public void setHoverParams(float amplitude, float speed) {
        this.hoverDriftAmplitude = amplitude;
        this.hoverDriftSpeed = speed;
    }

    public void setBaseRotationY(float rotation) {
        this.baseRotationY = rotation;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GETTERS PARA DEBUG
    // ═══════════════════════════════════════════════════════════════════════

    public String getCurrentStateName() {
        return currentState == STATE_HOVERING ? "HOVERING" : "FIGURE_8";
    }

    public float getTime() {
        return time;
    }
}
