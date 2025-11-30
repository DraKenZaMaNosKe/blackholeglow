package com.secret.blackholeglow.systems;

import android.content.Context;
import android.util.Log;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.HashMap;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                    RemoteConfigManager                            ║
 * ║                 "El Mensajero de las Reglas"                      ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  Actor especializado en obtener configuración remota de Firebase. ║
 * ║                                                                   ║
 * ║  RESPONSABILIDADES:                                               ║
 * ║  • Conectar con Firebase Remote Config                            ║
 * ║  • Cachear configuración localmente                               ║
 * ║  • Notificar cambios a otros actores                              ║
 * ║  • Gestionar eventos especiales (Navidad, Black Friday, etc.)     ║
 * ║                                                                   ║
 * ║  CLAVES DE CONFIGURACIÓN:                                         ║
 * ║  • minutes_per_point: Minutos para ganar 1 punto                  ║
 * ║  • points_to_unlock: Puntos para desbloquear wallpaper            ║
 * ║  • event_multiplier: Multiplicador de evento activo               ║
 * ║  • event_name: Nombre del evento activo                           ║
 * ║  • show_ads_on_set: Mostrar ad al establecer wallpaper            ║
 * ║  • ad_reward_points: Puntos por ver ad                            ║
 * ║                                                                   ║
 * ║  PRINCIPIOS:                                                      ║
 * ║  • Máximo 10 métodos públicos                                     ║
 * ║  • Un solo propósito: configuración remota                        ║
 * ║  • Singleton para acceso global                                   ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * CONFIGURACIÓN EN FIREBASE CONSOLE:
 * 1. Ve a Firebase Console > Remote Config
 * 2. Agrega los parámetros:
 *    - minutes_per_point (Number): 100
 *    - points_to_unlock (Number): 1000
 *    - event_multiplier (Number): 1.0
 *    - event_name (String): ""
 *    - show_ads_on_set (Boolean): true
 *    - ad_reward_points (Number): 50
 */
public class RemoteConfigManager {
    private static final String TAG = "RemoteConfigManager";

    // ═══════════════════════════════════════════════════════════════
    // CLAVES DE CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════

    public static final String KEY_MINUTES_PER_POINT = "minutes_per_point";
    public static final String KEY_POINTS_TO_UNLOCK = "points_to_unlock";
    public static final String KEY_EVENT_MULTIPLIER = "event_multiplier";
    public static final String KEY_EVENT_NAME = "event_name";
    public static final String KEY_SHOW_ADS_ON_SET = "show_ads_on_set";
    public static final String KEY_AD_REWARD_POINTS = "ad_reward_points";
    public static final String KEY_MAINTENANCE_MODE = "maintenance_mode";
    public static final String KEY_MIN_APP_VERSION = "min_app_version";

    // ═══════════════════════════════════════════════════════════════
    // VALORES POR DEFECTO (usados si no hay conexión)
    // ═══════════════════════════════════════════════════════════════

    private static final int DEFAULT_MINUTES_PER_POINT = 100;
    private static final int DEFAULT_POINTS_TO_UNLOCK = 1000;
    private static final double DEFAULT_EVENT_MULTIPLIER = 1.0;
    private static final String DEFAULT_EVENT_NAME = "";
    private static final boolean DEFAULT_SHOW_ADS_ON_SET = true;
    private static final int DEFAULT_AD_REWARD_POINTS = 50;

    // ═══════════════════════════════════════════════════════════════
    // SINGLETON
    // ═══════════════════════════════════════════════════════════════

    private static RemoteConfigManager instance;
    private FirebaseRemoteConfig remoteConfig;
    private boolean isInitialized = false;

    public static RemoteConfigManager get() {
        if (instance == null) {
            instance = new RemoteConfigManager();
        }
        return instance;
    }

    /**
     * Inicializa el manager con contexto
     */
    public static void init(Context context) {
        if (instance == null) {
            instance = new RemoteConfigManager();
        }
        instance.initialize();
    }

    private RemoteConfigManager() {
        // Constructor privado para singleton
    }

    // ═══════════════════════════════════════════════════════════════
    // 1. INICIALIZACIÓN
    // ═══════════════════════════════════════════════════════════════

    /**
     * Inicializa Firebase Remote Config
     */
    private void initialize() {
        try {
            remoteConfig = FirebaseRemoteConfig.getInstance();

            // Configuración de desarrollo (fetch cada 1 minuto)
            // En producción, cambiar a 12 horas (43200)
            FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(3600) // 1 hora en producción
                    .build();
            remoteConfig.setConfigSettingsAsync(configSettings);

            // Establecer valores por defecto
            Map<String, Object> defaults = new HashMap<>();
            defaults.put(KEY_MINUTES_PER_POINT, DEFAULT_MINUTES_PER_POINT);
            defaults.put(KEY_POINTS_TO_UNLOCK, DEFAULT_POINTS_TO_UNLOCK);
            defaults.put(KEY_EVENT_MULTIPLIER, DEFAULT_EVENT_MULTIPLIER);
            defaults.put(KEY_EVENT_NAME, DEFAULT_EVENT_NAME);
            defaults.put(KEY_SHOW_ADS_ON_SET, DEFAULT_SHOW_ADS_ON_SET);
            defaults.put(KEY_AD_REWARD_POINTS, DEFAULT_AD_REWARD_POINTS);
            defaults.put(KEY_MAINTENANCE_MODE, false);
            defaults.put(KEY_MIN_APP_VERSION, 1);

            remoteConfig.setDefaultsAsync(defaults);

            isInitialized = true;

            // Fetch inicial
            fetchConfig();

            Log.d(TAG, "╔════════════════════════════════════════╗");
            Log.d(TAG, "║   ☁️ RemoteConfigManager Inicializado  ║");
            Log.d(TAG, "╚════════════════════════════════════════╝");

        } catch (Exception e) {
            Log.e(TAG, "Error inicializando RemoteConfig: " + e.getMessage());
            isInitialized = false;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. FETCH DE CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════

    /**
     * Obtiene la configuración más reciente de Firebase
     */
    public void fetchConfig() {
        if (!isInitialized || remoteConfig == null) {
            Log.w(TAG, "RemoteConfig no inicializado");
            return;
        }

        remoteConfig.fetchAndActivate()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean updated = task.getResult();
                        Log.d(TAG, "☁️ Config " + (updated ? "actualizada" : "sin cambios"));

                        // Notificar a otros actores
                        notifyConfigUpdated();
                    } else {
                        Log.w(TAG, "Error obteniendo config remota");
                    }
                });
    }

    /**
     * Notifica a otros actores que la configuración se actualizó
     */
    private void notifyConfigUpdated() {
        EventBus.get().publish("remote_config_updated",
                new EventBus.EventData()
                        .put("minutes_per_point", getMinutesPerPoint())
                        .put("points_to_unlock", getPointsToUnlock())
                        .put("event_multiplier", getEventMultiplier())
                        .put("event_name", getEventName())
                        .put("show_ads_on_set", shouldShowAdsOnSet()));

        Log.d(TAG, "📢 Configuración notificada: " +
                getMinutesPerPoint() + " min/punto, " +
                getPointsToUnlock() + " puntos/unlock, " +
                getEventMultiplier() + "x multiplicador");
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. GETTERS DE CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════

    /**
     * Minutos de uso para ganar 1 punto
     */
    public int getMinutesPerPoint() {
        if (!isInitialized || remoteConfig == null) {
            return DEFAULT_MINUTES_PER_POINT;
        }
        return (int) remoteConfig.getLong(KEY_MINUTES_PER_POINT);
    }

    /**
     * Puntos necesarios para desbloquear un wallpaper
     */
    public int getPointsToUnlock() {
        if (!isInitialized || remoteConfig == null) {
            return DEFAULT_POINTS_TO_UNLOCK;
        }
        return (int) remoteConfig.getLong(KEY_POINTS_TO_UNLOCK);
    }

    /**
     * Multiplicador de evento activo (1.0 = normal)
     */
    public float getEventMultiplier() {
        if (!isInitialized || remoteConfig == null) {
            return (float) DEFAULT_EVENT_MULTIPLIER;
        }
        return (float) remoteConfig.getDouble(KEY_EVENT_MULTIPLIER);
    }

    /**
     * Nombre del evento activo (vacío si no hay evento)
     */
    public String getEventName() {
        if (!isInitialized || remoteConfig == null) {
            return DEFAULT_EVENT_NAME;
        }
        return remoteConfig.getString(KEY_EVENT_NAME);
    }

    /**
     * Si hay un evento activo
     */
    public boolean hasActiveEvent() {
        String eventName = getEventName();
        return eventName != null && !eventName.isEmpty() && getEventMultiplier() > 1.0f;
    }

    /**
     * Si se deben mostrar ads al establecer wallpaper
     */
    public boolean shouldShowAdsOnSet() {
        if (!isInitialized || remoteConfig == null) {
            return DEFAULT_SHOW_ADS_ON_SET;
        }
        return remoteConfig.getBoolean(KEY_SHOW_ADS_ON_SET);
    }

    /**
     * Puntos que se dan por ver un ad
     */
    public int getAdRewardPoints() {
        if (!isInitialized || remoteConfig == null) {
            return DEFAULT_AD_REWARD_POINTS;
        }
        return (int) remoteConfig.getLong(KEY_AD_REWARD_POINTS);
    }

    /**
     * Si la app está en modo mantenimiento
     */
    public boolean isMaintenanceMode() {
        if (!isInitialized || remoteConfig == null) {
            return false;
        }
        return remoteConfig.getBoolean(KEY_MAINTENANCE_MODE);
    }

    // ═══════════════════════════════════════════════════════════════
    // 4. ESTADO
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verifica si está inicializado
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    // ═══════════════════════════════════════════════════════════════
    // 5. RESET
    // ═══════════════════════════════════════════════════════════════

    /**
     * Reset del singleton
     */
    public static void reset() {
        if (instance != null) {
            instance.remoteConfig = null;
            instance.isInitialized = false;
        }
        instance = null;
        Log.d(TAG, "RemoteConfigManager reset");
    }
}
