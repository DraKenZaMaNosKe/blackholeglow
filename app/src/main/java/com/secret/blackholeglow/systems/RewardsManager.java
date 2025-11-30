package com.secret.blackholeglow.systems;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.secret.blackholeglow.models.WallpaperTier;

import java.util.HashSet;
import java.util.Set;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                       RewardsManager                              ║
 * ║                   "El Recompensador"                              ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  Actor especializado en el sistema de recompensas por uso.        ║
 * ║                                                                   ║
 * ║  RESPONSABILIDADES:                                               ║
 * ║  • Calcular puntos basados en tiempo de uso                       ║
 * ║  • Gestionar desbloqueo de wallpapers                             ║
 * ║  • Aplicar bonus de eventos (Navidad, Black Friday, etc.)         ║
 * ║  • Persistir progreso del usuario                                 ║
 * ║                                                                   ║
 * ║  REGLAS (desde RemoteConfigManager):                              ║
 * ║  • minutesPerPoint: Minutos para ganar 1 punto                    ║
 * ║  • pointsToUnlock: Puntos para desbloquear wallpaper              ║
 * ║  • eventMultiplier: Multiplicador de eventos especiales           ║
 * ║                                                                   ║
 * ║  PRINCIPIOS:                                                      ║
 * ║  • Máximo 10 métodos públicos                                     ║
 * ║  • Un solo propósito: sistema de recompensas                      ║
 * ║  • Singleton para acceso global                                   ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class RewardsManager {
    private static final String TAG = "RewardsManager";
    private static final String PREFS_NAME = "rewards_prefs";
    private static final String KEY_POINTS = "total_points";
    private static final String KEY_UNLOCKED_WALLPAPERS = "unlocked_wallpapers";
    private static final String KEY_LAST_CALCULATED_MINUTES = "last_calculated_minutes";

    // ═══════════════════════════════════════════════════════════════
    // SINGLETON
    // ═══════════════════════════════════════════════════════════════

    private static RewardsManager instance;
    private SharedPreferences prefs;

    // Estado
    private int totalPoints = 0;
    private long lastCalculatedMinutes = 0;
    private Set<String> unlockedWallpapers = new HashSet<>();

    // Reglas (valores por defecto, se actualizan desde RemoteConfig)
    private int minutesPerPoint = 100;      // 100 minutos = 1 punto
    private int pointsToUnlock = 1000;      // 1000 puntos = desbloquear
    private float eventMultiplier = 1.0f;   // Multiplicador de eventos

    public static RewardsManager get() {
        if (instance == null) {
            instance = new RewardsManager();
        }
        return instance;
    }

    /**
     * Inicializa el manager con contexto
     */
    public static void init(Context context) {
        if (instance == null) {
            instance = new RewardsManager();
        }
        instance.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        instance.loadData();

        // Suscribirse a actualizaciones de RemoteConfig
        instance.subscribeToConfigUpdates();

        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║   🏆 RewardsManager Inicializado       ║");
        Log.d(TAG, "║   Puntos: " + instance.totalPoints + "                      ║");
        Log.d(TAG, "║   Desbloqueados: " + instance.unlockedWallpapers.size() + "                  ║");
        Log.d(TAG, "╚════════════════════════════════════════╝");
    }

    private RewardsManager() {
        // Constructor privado para singleton
    }

    // ═══════════════════════════════════════════════════════════════
    // 1. OBTENER ESTADO
    // ═══════════════════════════════════════════════════════════════

    /**
     * Obtiene los puntos totales del usuario
     */
    public int getTotalPoints() {
        return totalPoints;
    }

    /**
     * Obtiene el progreso hacia el próximo desbloqueo (0.0 - 1.0)
     */
    public float getProgressToNextUnlock() {
        int pointsInCycle = totalPoints % pointsToUnlock;
        return (float) pointsInCycle / pointsToUnlock;
    }

    /**
     * Obtiene los puntos restantes para el próximo desbloqueo
     */
    public int getPointsToNextUnlock() {
        return pointsToUnlock - (totalPoints % pointsToUnlock);
    }

    /**
     * Obtiene el número de wallpapers desbloqueados
     */
    public int getUnlockedCount() {
        return unlockedWallpapers.size();
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. VERIFICAR ACCESO
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verifica si un wallpaper está desbloqueado
     * @param wallpaperName Nombre del wallpaper
     * @return true si está desbloqueado o es gratuito
     */
    public boolean isUnlocked(String wallpaperName) {
        // Los wallpapers FREE siempre están desbloqueados
        var wallpaper = WallpaperCatalog.get().getByName(wallpaperName);
        if (wallpaper != null && wallpaper.getTier() == WallpaperTier.FREE) {
            return true;
        }

        // Verificar si fue desbloqueado con puntos
        return unlockedWallpapers.contains(wallpaperName);
    }

    /**
     * Verifica si el usuario puede desbloquear un nuevo wallpaper
     */
    public boolean canUnlockNew() {
        return totalPoints >= pointsToUnlock;
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. ACTUALIZAR PUNTOS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Llamado por UsageTracker cuando se actualiza el tiempo de uso
     * @param totalMinutes Minutos totales de uso
     */
    public void onUsageUpdated(long totalMinutes) {
        // Calcular nuevos puntos ganados
        long minutesSinceLastCalculation = totalMinutes - lastCalculatedMinutes;

        if (minutesSinceLastCalculation >= minutesPerPoint) {
            int newPoints = (int) (minutesSinceLastCalculation / minutesPerPoint);

            // Aplicar multiplicador de evento
            newPoints = Math.round(newPoints * eventMultiplier);

            if (newPoints > 0) {
                addPoints(newPoints);
                lastCalculatedMinutes = totalMinutes - (totalMinutes % minutesPerPoint);
                saveData();
            }
        }
    }

    /**
     * Añade puntos manualmente (para bonus, ads, etc.)
     */
    public void addPoints(int points) {
        int oldPoints = totalPoints;
        totalPoints += points;
        saveData();

        // Publicar evento
        EventBus.get().publish("points_earned",
                new EventBus.EventData()
                        .put("points_earned", points)
                        .put("total_points", totalPoints)
                        .put("can_unlock", canUnlockNew()));

        // Verificar si puede desbloquear
        if (!canUnlockNew(oldPoints) && canUnlockNew()) {
            EventBus.get().publish("unlock_available",
                    new EventBus.EventData()
                            .put("total_points", totalPoints));
        }

        Log.d(TAG, "🏆 +" + points + " puntos! (Total: " + totalPoints + ")");
    }

    private boolean canUnlockNew(int points) {
        return points >= pointsToUnlock;
    }

    // ═══════════════════════════════════════════════════════════════
    // 4. DESBLOQUEAR WALLPAPERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Desbloquea un wallpaper gastando puntos
     * @param wallpaperName Nombre del wallpaper a desbloquear
     * @return true si se desbloqueó exitosamente
     */
    public boolean unlockWallpaper(String wallpaperName) {
        // Verificar que no esté ya desbloqueado
        if (isUnlocked(wallpaperName)) {
            Log.w(TAG, "Wallpaper ya desbloqueado: " + wallpaperName);
            return false;
        }

        // Verificar que tenga suficientes puntos
        if (totalPoints < pointsToUnlock) {
            Log.w(TAG, "Puntos insuficientes: " + totalPoints + "/" + pointsToUnlock);
            return false;
        }

        // Descontar puntos y desbloquear
        totalPoints -= pointsToUnlock;
        unlockedWallpapers.add(wallpaperName);
        saveData();

        // Publicar evento
        EventBus.get().publish("wallpaper_unlocked",
                new EventBus.EventData()
                        .put("wallpaper_name", wallpaperName)
                        .put("remaining_points", totalPoints));

        Log.d(TAG, "🔓 Wallpaper desbloqueado: " + wallpaperName);
        return true;
    }

    // ═══════════════════════════════════════════════════════════════
    // 5. CONFIGURACIÓN REMOTA
    // ═══════════════════════════════════════════════════════════════

    /**
     * Actualiza las reglas desde RemoteConfigManager
     */
    public void updateRules(int minutesPerPoint, int pointsToUnlock, float eventMultiplier) {
        this.minutesPerPoint = minutesPerPoint;
        this.pointsToUnlock = pointsToUnlock;
        this.eventMultiplier = eventMultiplier;

        Log.d(TAG, "📋 Reglas actualizadas: " +
                minutesPerPoint + " min/punto, " +
                pointsToUnlock + " puntos/unlock, " +
                eventMultiplier + "x multiplicador");
    }

    /**
     * Suscribirse a actualizaciones de RemoteConfig
     */
    private void subscribeToConfigUpdates() {
        EventBus.get().subscribe("remote_config_updated", data -> {
            int newMinutesPerPoint = data.getInt("minutes_per_point", minutesPerPoint);
            int newPointsToUnlock = data.getInt("points_to_unlock", pointsToUnlock);
            float newMultiplier = data.getFloat("event_multiplier", eventMultiplier);
            updateRules(newMinutesPerPoint, newPointsToUnlock, newMultiplier);
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // 6. PERSISTENCIA
    // ═══════════════════════════════════════════════════════════════

    private void loadData() {
        if (prefs != null) {
            totalPoints = prefs.getInt(KEY_POINTS, 0);
            lastCalculatedMinutes = prefs.getLong(KEY_LAST_CALCULATED_MINUTES, 0);
            unlockedWallpapers = prefs.getStringSet(KEY_UNLOCKED_WALLPAPERS, new HashSet<>());
            // Crear copia mutable
            unlockedWallpapers = new HashSet<>(unlockedWallpapers);
        }
    }

    private void saveData() {
        if (prefs != null) {
            prefs.edit()
                    .putInt(KEY_POINTS, totalPoints)
                    .putLong(KEY_LAST_CALCULATED_MINUTES, lastCalculatedMinutes)
                    .putStringSet(KEY_UNLOCKED_WALLPAPERS, unlockedWallpapers)
                    .apply();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 7. RESET
    // ═══════════════════════════════════════════════════════════════

    /**
     * Reset del singleton
     */
    public static void reset() {
        if (instance != null) {
            instance.prefs = null;
        }
        instance = null;
        Log.d(TAG, "RewardsManager reset");
    }

    /**
     * Reset de datos (para testing)
     */
    public void resetData() {
        totalPoints = 0;
        lastCalculatedMinutes = 0;
        unlockedWallpapers.clear();
        saveData();
        Log.d(TAG, "⚠️ Datos de recompensas reseteados");
    }

    // ═══════════════════════════════════════════════════════════════
    // GETTERS DE REGLAS (para UI)
    // ═══════════════════════════════════════════════════════════════

    public int getMinutesPerPoint() { return minutesPerPoint; }
    public int getPointsToUnlock() { return pointsToUnlock; }
    public float getEventMultiplier() { return eventMultiplier; }
    public boolean hasActiveEvent() { return eventMultiplier > 1.0f; }
}
