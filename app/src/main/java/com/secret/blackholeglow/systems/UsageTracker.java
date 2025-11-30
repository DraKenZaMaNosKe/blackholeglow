package com.secret.blackholeglow.systems;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                        UsageTracker                               ║
 * ║                    "El Contador de Tiempo"                        ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  Actor especializado en medir el tiempo de uso del wallpaper.     ║
 * ║                                                                   ║
 * ║  RESPONSABILIDADES:                                               ║
 * ║  • Registrar cuando el wallpaper está activo/visible              ║
 * ║  • Calcular minutos totales de uso                                ║
 * ║  • Persistir datos localmente                                     ║
 * ║  • Notificar a RewardsManager para cálculo de puntos              ║
 * ║                                                                   ║
 * ║  CÓMO FUNCIONA:                                                   ║
 * ║  • onWallpaperVisible() → Inicia contador                         ║
 * ║  • onWallpaperHidden() → Detiene y acumula tiempo                 ║
 * ║  • Cada minuto completo se notifica a RewardsManager              ║
 * ║                                                                   ║
 * ║  PRINCIPIOS:                                                      ║
 * ║  • Máximo 10 métodos públicos                                     ║
 * ║  • Un solo propósito: tracking de tiempo                          ║
 * ║  • Singleton para acceso global                                   ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class UsageTracker {
    private static final String TAG = "UsageTracker";
    private static final String PREFS_NAME = "usage_tracker_prefs";
    private static final String KEY_TOTAL_SECONDS = "total_seconds";
    private static final String KEY_LAST_SESSION_DATE = "last_session_date";
    private static final String KEY_TODAY_SECONDS = "today_seconds";

    // ═══════════════════════════════════════════════════════════════
    // SINGLETON
    // ═══════════════════════════════════════════════════════════════

    private static UsageTracker instance;
    private SharedPreferences prefs;

    // Estado de tracking
    private long sessionStartTime = 0;
    private boolean isTracking = false;
    private long totalSeconds = 0;
    private long todaySeconds = 0;
    private String lastSessionDate = "";
    private long lastMinuteNotified = 0;  // Para eventos de minutos

    public static UsageTracker get() {
        if (instance == null) {
            instance = new UsageTracker();
        }
        return instance;
    }

    /**
     * Inicializa el tracker con contexto
     * Llamar desde Application o primera Activity
     */
    public static void init(Context context) {
        if (instance == null) {
            instance = new UsageTracker();
        }
        instance.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        instance.loadData();
        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║   ⏱️ UsageTracker Inicializado         ║");
        Log.d(TAG, "║   Total: " + instance.getTotalMinutes() + " minutos           ║");
        Log.d(TAG, "╚════════════════════════════════════════╝");
    }

    private UsageTracker() {
        // Constructor privado para singleton
    }

    // ═══════════════════════════════════════════════════════════════
    // 1. CONTROL DE SESIÓN
    // ═══════════════════════════════════════════════════════════════

    /**
     * Llamar cuando el wallpaper se vuelve visible
     * (onVisibilityChanged = true en LiveWallpaperService)
     */
    public void onWallpaperVisible() {
        if (isTracking) return; // Ya está trackeando

        sessionStartTime = System.currentTimeMillis();
        isTracking = true;
        lastMinuteNotified = getTotalMinutes();  // Inicializar contador de minutos

        // Verificar si es un nuevo día
        checkNewDay();

        Log.d(TAG, "▶️ Sesión iniciada");
    }

    /**
     * Llamar cuando el wallpaper se oculta
     * (onVisibilityChanged = false en LiveWallpaperService)
     */
    public void onWallpaperHidden() {
        if (!isTracking) return; // No está trackeando

        long sessionDuration = (System.currentTimeMillis() - sessionStartTime) / 1000;
        totalSeconds += sessionDuration;
        todaySeconds += sessionDuration;
        isTracking = false;

        // Guardar datos
        saveData();

        // Notificar a RewardsManager
        notifyRewardsManager(sessionDuration);

        Log.d(TAG, "⏹️ Sesión terminada: " + sessionDuration + "s (Total: " + getTotalMinutes() + " min)");
    }

    /**
     * Llamar periódicamente mientras el wallpaper está visible
     * para actualizar el tiempo en tiempo real
     */
    public void tick() {
        if (!isTracking) return;

        long currentSessionSeconds = (System.currentTimeMillis() - sessionStartTime) / 1000;
        long currentTotalMinutes = (totalSeconds + currentSessionSeconds) / 60;

        // Cada minuto completo, emitir evento para misiones
        if (currentTotalMinutes > lastMinuteNotified) {
            lastMinuteNotified = currentTotalMinutes;

            // 🎯 Emitir evento de minuto para MissionsManager
            EventBus.get().publish("usage_minute_tick",
                    new EventBus.EventData()
                            .put("total_minutes", (float) currentTotalMinutes));

            Log.d(TAG, "⏱️ Minuto " + currentTotalMinutes + " completado");

            // Guardar parcialmente
            if (prefs != null) {
                prefs.edit().putLong(KEY_TOTAL_SECONDS, totalSeconds + currentSessionSeconds).apply();
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. OBTENER ESTADÍSTICAS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Obtiene el total de minutos de uso (histórico)
     */
    public long getTotalMinutes() {
        long currentSession = isTracking ?
                (System.currentTimeMillis() - sessionStartTime) / 1000 : 0;
        return (totalSeconds + currentSession) / 60;
    }

    /**
     * Obtiene el total de segundos de uso (histórico)
     */
    public long getTotalSeconds() {
        long currentSession = isTracking ?
                (System.currentTimeMillis() - sessionStartTime) / 1000 : 0;
        return totalSeconds + currentSession;
    }

    /**
     * Obtiene los minutos de uso de hoy
     */
    public long getTodayMinutes() {
        checkNewDay();
        long currentSession = isTracking ?
                (System.currentTimeMillis() - sessionStartTime) / 1000 : 0;
        return (todaySeconds + currentSession) / 60;
    }

    /**
     * Obtiene la duración de la sesión actual en segundos
     */
    public long getCurrentSessionSeconds() {
        if (!isTracking) return 0;
        return (System.currentTimeMillis() - sessionStartTime) / 1000;
    }

    /**
     * Verifica si está trackeando actualmente
     */
    public boolean isTracking() {
        return isTracking;
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. PERSISTENCIA
    // ═══════════════════════════════════════════════════════════════

    private void loadData() {
        if (prefs != null) {
            totalSeconds = prefs.getLong(KEY_TOTAL_SECONDS, 0);
            todaySeconds = prefs.getLong(KEY_TODAY_SECONDS, 0);
            lastSessionDate = prefs.getString(KEY_LAST_SESSION_DATE, "");
            checkNewDay();
        }
    }

    private void saveData() {
        if (prefs != null) {
            prefs.edit()
                    .putLong(KEY_TOTAL_SECONDS, totalSeconds)
                    .putLong(KEY_TODAY_SECONDS, todaySeconds)
                    .putString(KEY_LAST_SESSION_DATE, getTodayDate())
                    .apply();
        }
    }

    /**
     * Verifica si es un nuevo día y resetea el contador diario
     */
    private void checkNewDay() {
        String today = getTodayDate();
        if (!today.equals(lastSessionDate)) {
            todaySeconds = 0;
            lastSessionDate = today;
            Log.d(TAG, "📅 Nuevo día detectado, contador diario reseteado");
        }
    }

    private String getTodayDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd",
                java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }

    // ═══════════════════════════════════════════════════════════════
    // 4. INTEGRACIÓN CON REWARDS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Notifica a RewardsManager sobre tiempo de uso
     */
    private void notifyRewardsManager(long sessionSeconds) {
        // Publicar evento via EventBus
        EventBus.get().publish("usage_session_ended",
                new EventBus.EventData()
                        .put("session_seconds", (float) sessionSeconds)
                        .put("total_minutes", (float) getTotalMinutes())
                        .put("today_minutes", (float) getTodayMinutes()));

        // También notificar directamente a RewardsManager
        RewardsManager.get().onUsageUpdated(getTotalMinutes());
    }

    // ═══════════════════════════════════════════════════════════════
    // 5. RESET
    // ═══════════════════════════════════════════════════════════════

    /**
     * Reset del singleton
     */
    public static void reset() {
        if (instance != null) {
            instance.isTracking = false;
            instance.prefs = null;
        }
        instance = null;
        Log.d(TAG, "UsageTracker reset");
    }

    /**
     * Reset de datos (para testing o nuevo usuario)
     */
    public void resetData() {
        totalSeconds = 0;
        todaySeconds = 0;
        lastSessionDate = "";
        saveData();
        Log.d(TAG, "⚠️ Datos de uso reseteados");
    }
}
