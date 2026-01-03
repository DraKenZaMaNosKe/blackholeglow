package com.secret.blackholeglow;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   📱 GyroscopeManager - Control por inclinación del dispositivo          ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  CARACTERÍSTICAS:                                                         ║
 * ║  • Lee sensores de acelerómetro y magnetómetro                           ║
 * ║  • Calcula orientación del dispositivo (pitch, roll)                     ║
 * ║  • Suavizado de valores para movimiento fluido                           ║
 * ║  • Valores normalizados -1 a 1 para fácil integración                    ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class GyroscopeManager implements SensorEventListener {
    private static final String TAG = "GyroscopeManager";

    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    private final Sensor magnetometer;

    // Datos de sensores
    private float[] gravity = new float[3];
    private float[] geomagnetic = new float[3];
    private float[] rotationMatrix = new float[9];
    private float[] orientation = new float[3];

    // Valores de inclinación (-1 a 1)
    private float tiltX = 0f;  // Roll (inclinación lateral)
    private float tiltY = 0f;  // Pitch (inclinación adelante/atrás)

    // Suavizado
    private static final float SMOOTHING_FACTOR = 0.15f;  // Mayor = más suave
    private float smoothTiltX = 0f;
    private float smoothTiltY = 0f;

    // Estado
    private boolean isEnabled = false;
    private boolean hasAccelerometer = false;
    private boolean hasMagnetometer = false;

    // Sensibilidad (cuánto afecta la inclinación)
    private float sensitivity = 1.0f;

    // Zona muerta (ignora pequeñas inclinaciones)
    private static final float DEAD_ZONE = 0.05f;

    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════

    public GyroscopeManager(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        hasAccelerometer = (accelerometer != null);
        hasMagnetometer = (magnetometer != null);

        Log.d(TAG, "📱 GyroscopeManager creado - Acelerómetro: " + hasAccelerometer +
              ", Magnetómetro: " + hasMagnetometer);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONTROL
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Inicia la escucha de sensores
     */
    public void start() {
        if (isEnabled) return;

        if (hasAccelerometer) {
            sensorManager.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_GAME);
        }
        if (hasMagnetometer) {
            sensorManager.registerListener(this, magnetometer,
                SensorManager.SENSOR_DELAY_GAME);
        }

        isEnabled = true;
        Log.d(TAG, "▶️ Sensores activados");
    }

    /**
     * Detiene la escucha de sensores (ahorra batería)
     */
    public void stop() {
        if (!isEnabled) return;

        sensorManager.unregisterListener(this);
        isEnabled = false;

        // Resetear valores
        tiltX = 0f;
        tiltY = 0f;
        smoothTiltX = 0f;
        smoothTiltY = 0f;

        Log.d(TAG, "⏹️ Sensores desactivados");
    }

    /**
     * Pausa temporalmente (para cuando el wallpaper no es visible)
     */
    public void pause() {
        stop();
    }

    /**
     * Reanuda después de pausa
     */
    public void resume() {
        start();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SENSOR CALLBACKS
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Low-pass filter para suavizar acelerómetro
            gravity[0] = gravity[0] * 0.8f + event.values[0] * 0.2f;
            gravity[1] = gravity[1] * 0.8f + event.values[1] * 0.2f;
            gravity[2] = gravity[2] * 0.8f + event.values[2] * 0.2f;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic[0] = event.values[0];
            geomagnetic[1] = event.values[1];
            geomagnetic[2] = event.values[2];
        }

        // Calcular orientación cuando tenemos ambos datos
        if (hasAccelerometer && hasMagnetometer) {
            boolean success = SensorManager.getRotationMatrix(
                rotationMatrix, null, gravity, geomagnetic);

            if (success) {
                SensorManager.getOrientation(rotationMatrix, orientation);

                // orientation[0] = azimuth (brújula, no nos interesa)
                // orientation[1] = pitch (inclinación adelante/atrás)
                // orientation[2] = roll (inclinación lateral)

                // Convertir de radianes a valores normalizados (-1 a 1)
                // Pitch: -90° a 90° → -1 a 1
                float rawPitch = (float) Math.toDegrees(orientation[1]);
                tiltY = clamp(rawPitch / 45f, -1f, 1f);  // 45° = máximo

                // Roll: -90° a 90° → -1 a 1
                float rawRoll = (float) Math.toDegrees(orientation[2]);
                tiltX = clamp(rawRoll / 45f, -1f, 1f);

                // Aplicar zona muerta
                if (Math.abs(tiltX) < DEAD_ZONE) tiltX = 0f;
                if (Math.abs(tiltY) < DEAD_ZONE) tiltY = 0f;

                // Suavizado exponencial
                smoothTiltX += (tiltX - smoothTiltX) * SMOOTHING_FACTOR;
                smoothTiltY += (tiltY - smoothTiltY) * SMOOTHING_FACTOR;
            }
        } else if (hasAccelerometer) {
            // Fallback: usar solo acelerómetro (menos preciso pero funciona)
            float gx = gravity[0];
            float gy = gravity[1];
            float gz = gravity[2];

            // Normalizar por magnitud de gravedad
            float magnitude = (float) Math.sqrt(gx*gx + gy*gy + gz*gz);
            if (magnitude > 0.1f) {
                tiltX = clamp(-gx / magnitude, -1f, 1f);  // Roll
                tiltY = clamp(-gy / magnitude, -1f, 1f);  // Pitch (invertido)

                // Aplicar zona muerta
                if (Math.abs(tiltX) < DEAD_ZONE) tiltX = 0f;
                if (Math.abs(tiltY) < DEAD_ZONE) tiltY = 0f;

                // Suavizado
                smoothTiltX += (tiltX - smoothTiltX) * SMOOTHING_FACTOR;
                smoothTiltY += (tiltY - smoothTiltY) * SMOOTHING_FACTOR;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No necesitamos manejar cambios de precisión
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Inclinación lateral (roll) suavizada
     * @return -1 (izquierda) a 1 (derecha)
     */
    public float getTiltX() {
        return smoothTiltX * sensitivity;
    }

    /**
     * Inclinación adelante/atrás (pitch) suavizada
     * @return -1 (hacia ti) a 1 (alejándose)
     */
    public float getTiltY() {
        return smoothTiltY * sensitivity;
    }

    /**
     * Valores raw sin suavizar (para debug)
     */
    public float getRawTiltX() { return tiltX; }
    public float getRawTiltY() { return tiltY; }

    /**
     * ¿Están los sensores disponibles?
     */
    public boolean isAvailable() {
        return hasAccelerometer;  // Mínimo necesitamos acelerómetro
    }

    /**
     * ¿Está activo?
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Ajusta la sensibilidad (0.5 = menos sensible, 2.0 = más sensible)
     */
    public void setSensitivity(float sensitivity) {
        this.sensitivity = clamp(sensitivity, 0.1f, 3f);
    }

    /**
     * Resetea los valores suavizados a cero
     */
    public void reset() {
        smoothTiltX = 0f;
        smoothTiltY = 0f;
        tiltX = 0f;
        tiltY = 0f;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════════════════

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Libera recursos
     */
    public void release() {
        stop();
        Log.d(TAG, "🗑️ GyroscopeManager liberado");
    }
}
