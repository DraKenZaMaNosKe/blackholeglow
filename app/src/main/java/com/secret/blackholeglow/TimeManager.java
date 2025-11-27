package com.secret.blackholeglow;

import android.util.Log;
import java.util.Calendar;

/**
 * TimeManager - Gestor centralizado de tiempo para todo el wallpaper
 *
 * VENTAJAS:
 * 1. UNA SOLA llamada a System.currentTimeMillis() por frame
 * 2. Valores pre-calculados (sin, cos) disponibles para todos
 * 3. Tiempo sincronizado entre todos los objetos
 * 4. Thread-safe para uso desde múltiples threads
 *
 * USO:
 *   // En onDrawFrame, una sola vez:
 *   TimeManager.update();
 *
 *   // En cualquier objeto:
 *   float time = TimeManager.getTime();
 *   float sinTime = TimeManager.getSinTime();
 */
public final class TimeManager {
    private static final String TAG = "TimeManager";

    // ═══════════════════════════════════════════════════════════════
    // TIEMPO GLOBAL - Actualizado UNA vez por frame
    // ═══════════════════════════════════════════════════════════════

    // Tiempo en segundos desde inicio (con wrap para evitar overflow)
    private static volatile float globalTime = 0f;
    private static volatile float deltaTime = 0f;
    private static volatile long lastUpdateNanos = 0;
    private static volatile long currentMillis = 0;

    // Valores pre-calculados de sin/cos para animaciones comunes
    private static volatile float sinTime = 0f;        // sin(time)
    private static volatile float cosTime = 0f;        // cos(time)
    private static volatile float sinTimeFast = 0f;    // sin(time * 2)
    private static volatile float cosTimeFast = 0f;    // cos(time * 2)
    private static volatile float sinTimeSlow = 0f;    // sin(time * 0.5)
    private static volatile float cosTimeSlow = 0f;    // cos(time * 0.5)

    // Fase de shader (0 a 2π, cíclica)
    private static volatile float shaderPhase = 0f;

    // ═══════════════════════════════════════════════════════════════
    // TIEMPO REAL (Calendario) - Actualizado cada 100ms
    // ═══════════════════════════════════════════════════════════════

    private static volatile int hour = 0;
    private static volatile int minute = 0;
    private static volatile int second = 0;
    private static volatile int millis = 0;
    private static volatile int dayOfYear = 0;
    private static volatile float dayFraction = 0f;    // 0.0 a 1.0 (fracción del día)

    private static long lastCalendarUpdate = 0;
    private static final long CALENDAR_UPDATE_INTERVAL = 100; // 100ms
    private static final Calendar calendar = Calendar.getInstance();

    // ═══════════════════════════════════════════════════════════════
    // CONSTANTES
    // ═══════════════════════════════════════════════════════════════

    private static final float TWO_PI = (float)(Math.PI * 2.0);
    private static final float TIME_WRAP = 3600f; // Wrap cada hora para evitar pérdida de precisión

    // Prevenir instanciación
    private TimeManager() {}

    /**
     * Actualiza todos los valores de tiempo.
     * DEBE llamarse UNA VEZ al inicio de cada frame (en onDrawFrame).
     */
    public static void update() {
        long nowNanos = System.nanoTime();
        currentMillis = System.currentTimeMillis();

        // Calcular delta time
        if (lastUpdateNanos == 0) {
            lastUpdateNanos = nowNanos;
            deltaTime = 0.016f; // Asumir 60 FPS en el primer frame
        } else {
            deltaTime = (nowNanos - lastUpdateNanos) / 1_000_000_000f;
            // Clamp para evitar saltos grandes (ej: después de pausa)
            if (deltaTime > 0.1f) deltaTime = 0.1f;
            if (deltaTime < 0f) deltaTime = 0f;
        }
        lastUpdateNanos = nowNanos;

        // Actualizar tiempo global (con wrap)
        globalTime = (globalTime + deltaTime) % TIME_WRAP;

        // Pre-calcular valores trigonométricos
        sinTime = (float)Math.sin(globalTime);
        cosTime = (float)Math.cos(globalTime);
        sinTimeFast = (float)Math.sin(globalTime * 2.0);
        cosTimeFast = (float)Math.cos(globalTime * 2.0);
        sinTimeSlow = (float)Math.sin(globalTime * 0.5);
        cosTimeSlow = (float)Math.cos(globalTime * 0.5);

        // Fase de shader (0 a 2π)
        shaderPhase = (globalTime % 1.0f) * TWO_PI;

        // Actualizar calendario (menos frecuente)
        if (currentMillis - lastCalendarUpdate >= CALENDAR_UPDATE_INTERVAL) {
            updateCalendar();
            lastCalendarUpdate = currentMillis;
        }
    }

    /**
     * Actualiza valores del calendario (hora, minuto, etc.)
     */
    private static void updateCalendar() {
        calendar.setTimeInMillis(currentMillis);
        hour = calendar.get(Calendar.HOUR_OF_DAY);
        minute = calendar.get(Calendar.MINUTE);
        second = calendar.get(Calendar.SECOND);
        millis = calendar.get(Calendar.MILLISECOND);
        dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);

        // Fracción del día (0.0 a 1.0)
        dayFraction = (hour + minute / 60f + second / 3600f + millis / 3600000f) / 24f;
    }

    /**
     * Resetea el tiempo (útil al reanudar después de pausa)
     */
    public static void reset() {
        lastUpdateNanos = 0;
        globalTime = 0f;
        deltaTime = 0.016f;
        Log.d(TAG, "TimeManager reset");
    }

    // ═══════════════════════════════════════════════════════════════
    // GETTERS - Thread-safe (volatile)
    // ═══════════════════════════════════════════════════════════════

    /** Tiempo global en segundos (0 a 3600, luego wrap) */
    public static float getTime() { return globalTime; }

    /** Delta time del último frame en segundos */
    public static float getDeltaTime() { return deltaTime; }

    /** Milisegundos actuales (System.currentTimeMillis) */
    public static long getMillis() { return currentMillis; }

    /** sin(time) pre-calculado */
    public static float getSinTime() { return sinTime; }

    /** cos(time) pre-calculado */
    public static float getCosTime() { return cosTime; }

    /** sin(time * 2) pre-calculado - animaciones rápidas */
    public static float getSinTimeFast() { return sinTimeFast; }

    /** cos(time * 2) pre-calculado - animaciones rápidas */
    public static float getCosTimeFast() { return cosTimeFast; }

    /** sin(time * 0.5) pre-calculado - animaciones lentas */
    public static float getSinTimeSlow() { return sinTimeSlow; }

    /** cos(time * 0.5) pre-calculado - animaciones lentas */
    public static float getCosTimeSlow() { return cosTimeSlow; }

    /** Fase para shaders (0 a 2π) */
    public static float getShaderPhase() { return shaderPhase; }

    /** Hora actual (0-23) */
    public static int getHour() { return hour; }

    /** Minuto actual (0-59) */
    public static int getMinute() { return minute; }

    /** Segundo actual (0-59) */
    public static int getSecond() { return second; }

    /** Milisegundos del segundo actual (0-999) */
    public static int getMillisOfSecond() { return millis; }

    /** Día del año (1-366) */
    public static int getDayOfYear() { return dayOfYear; }

    /** Fracción del día (0.0 a 1.0) - útil para rotación sincronizada con hora */
    public static float getDayFraction() { return dayFraction; }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS - Cálculos comunes pre-optimizados
    // ═══════════════════════════════════════════════════════════════

    /**
     * Calcula sin para una frecuencia específica usando el tiempo global.
     * Más eficiente que Math.sin() para frecuencias comunes.
     *
     * @param frequency Frecuencia del seno (1.0 = normal, 2.0 = doble velocidad)
     * @return Valor de seno (-1 a 1)
     */
    public static float sin(float frequency) {
        if (frequency == 1.0f) return sinTime;
        if (frequency == 2.0f) return sinTimeFast;
        if (frequency == 0.5f) return sinTimeSlow;
        return (float)Math.sin(globalTime * frequency);
    }

    /**
     * Calcula cos para una frecuencia específica usando el tiempo global.
     */
    public static float cos(float frequency) {
        if (frequency == 1.0f) return cosTime;
        if (frequency == 2.0f) return cosTimeFast;
        if (frequency == 0.5f) return cosTimeSlow;
        return (float)Math.cos(globalTime * frequency);
    }

    /**
     * Genera un pulso suave (0 a 1) basado en el tiempo.
     * Útil para efectos de brillo/escala pulsante.
     *
     * @param frequency Frecuencia del pulso
     * @return Valor entre 0 y 1
     */
    public static float pulse(float frequency) {
        return sin(frequency) * 0.5f + 0.5f;
    }

    /**
     * Verifica si ha pasado un intervalo desde la última vez.
     * Útil para throttling de logs o updates periódicos.
     *
     * @param lastTime Última vez que se ejecutó (en millis)
     * @param intervalMs Intervalo mínimo en milisegundos
     * @return true si ha pasado suficiente tiempo
     */
    public static boolean hasElapsed(long lastTime, long intervalMs) {
        return (currentMillis - lastTime) >= intervalMs;
    }
}
