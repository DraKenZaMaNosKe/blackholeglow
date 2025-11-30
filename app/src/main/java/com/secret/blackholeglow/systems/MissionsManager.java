package com.secret.blackholeglow.systems;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                       MissionsManager                             ║
 * ║                   "El Maestro de Misiones"                        ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  Actor especializado en misiones y objetivos por wallpaper.       ║
 * ║                                                                   ║
 * ║  RESPONSABILIDADES:                                               ║
 * ║  • Definir misiones únicas por wallpaper                          ║
 * ║  • Rastrear progreso de misiones                                  ║
 * ║  • Otorgar recompensas al completar                               ║
 * ║  • Cargar misiones desde RemoteConfig                             ║
 * ║                                                                   ║
 * ║  TIPOS DE MISIONES:                                               ║
 * ║  • TIME_SPENT: Usar wallpaper X minutos                           ║
 * ║  • INTERACTIONS: Tocar pantalla X veces                           ║
 * ║  • CONSECUTIVE_DAYS: Usar X días seguidos                         ║
 * ║  • WATCH_AD: Ver un anuncio recompensado                          ║
 * ║                                                                   ║
 * ║  PRINCIPIOS:                                                      ║
 * ║  • Máximo 10 métodos públicos                                     ║
 * ║  • Un solo propósito: sistema de misiones                         ║
 * ║  • Singleton para acceso global                                   ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class MissionsManager {
    private static final String TAG = "MissionsManager";
    private static final String PREFS_NAME = "missions_prefs";
    private static final String KEY_MISSION_PROGRESS = "mission_progress";
    private static final String KEY_COMPLETED_MISSIONS = "completed_missions";

    // ═══════════════════════════════════════════════════════════════
    // SINGLETON
    // ═══════════════════════════════════════════════════════════════

    private static MissionsManager instance;
    private SharedPreferences prefs;
    private Context context;

    // Misiones por wallpaper
    private Map<String, List<Mission>> wallpaperMissions = new HashMap<>();
    // Progreso de misiones
    private Map<String, Integer> missionProgress = new HashMap<>();
    // Misiones completadas
    private List<String> completedMissions = new ArrayList<>();

    public static MissionsManager get() {
        if (instance == null) {
            instance = new MissionsManager();
        }
        return instance;
    }

    /**
     * Inicializa el manager con contexto
     */
    public static void init(Context context) {
        if (instance == null) {
            instance = new MissionsManager();
        }
        instance.context = context.getApplicationContext();
        instance.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        instance.loadProgress();
        instance.setupDefaultMissions();
        instance.subscribeToEvents();

        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║   🎯 MissionsManager Inicializado      ║");
        Log.d(TAG, "║   Misiones activas: " + instance.getTotalMissionCount() + "              ║");
        Log.d(TAG, "╚════════════════════════════════════════╝");
    }

    private MissionsManager() {
        // Constructor privado para singleton
    }

    // ═══════════════════════════════════════════════════════════════
    // TIPOS DE MISIÓN
    // ═══════════════════════════════════════════════════════════════

    public enum MissionType {
        TIME_SPENT,         // Usar wallpaper X minutos
        INTERACTIONS,       // Tocar pantalla X veces
        CONSECUTIVE_DAYS,   // Usar X días seguidos
        WATCH_AD,           // Ver anuncio recompensado
        SET_WALLPAPER,      // Establecer el wallpaper
        SHARE_WALLPAPER     // Compartir el wallpaper
    }

    /**
     * Clase que representa una misión
     */
    public static class Mission {
        public final String id;
        public final String wallpaperId;
        public final String title;
        public final String description;
        public final MissionType type;
        public final int targetValue;
        public final int rewardPoints;
        public final String rewardBadge;  // Badge especial al completar

        public Mission(String id, String wallpaperId, String title, String description,
                       MissionType type, int targetValue, int rewardPoints, String rewardBadge) {
            this.id = id;
            this.wallpaperId = wallpaperId;
            this.title = title;
            this.description = description;
            this.type = type;
            this.targetValue = targetValue;
            this.rewardPoints = rewardPoints;
            this.rewardBadge = rewardBadge;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 1. OBTENER MISIONES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Obtiene todas las misiones de un wallpaper
     */
    public List<Mission> getMissionsForWallpaper(String wallpaperId) {
        List<Mission> missions = wallpaperMissions.get(wallpaperId);
        return missions != null ? missions : new ArrayList<>();
    }

    /**
     * Obtiene las misiones activas (no completadas) de un wallpaper
     */
    public List<Mission> getActiveMissions(String wallpaperId) {
        List<Mission> active = new ArrayList<>();
        List<Mission> all = getMissionsForWallpaper(wallpaperId);
        for (Mission m : all) {
            if (!completedMissions.contains(m.id)) {
                active.add(m);
            }
        }
        return active;
    }

    /**
     * Obtiene el progreso de una misión (0 a targetValue)
     */
    public int getMissionProgress(String missionId) {
        return missionProgress.getOrDefault(missionId, 0);
    }

    /**
     * Obtiene el progreso como porcentaje (0.0 a 1.0)
     */
    public float getMissionProgressPercent(Mission mission) {
        int progress = getMissionProgress(mission.id);
        return Math.min(1.0f, (float) progress / mission.targetValue);
    }

    /**
     * Verifica si una misión está completada
     */
    public boolean isMissionCompleted(String missionId) {
        return completedMissions.contains(missionId);
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. ACTUALIZAR PROGRESO
    // ═══════════════════════════════════════════════════════════════

    /**
     * Incrementa el progreso de misiones de un tipo específico
     * @param wallpaperId Wallpaper actual
     * @param type Tipo de misión
     * @param amount Cantidad a incrementar
     */
    public void incrementProgress(String wallpaperId, MissionType type, int amount) {
        List<Mission> missions = getMissionsForWallpaper(wallpaperId);

        for (Mission mission : missions) {
            if (mission.type == type && !isMissionCompleted(mission.id)) {
                int currentProgress = getMissionProgress(mission.id);
                int newProgress = currentProgress + amount;
                missionProgress.put(mission.id, newProgress);

                Log.d(TAG, "📊 Progreso: " + mission.title + " = " + newProgress + "/" + mission.targetValue);

                // Verificar si se completó
                if (newProgress >= mission.targetValue) {
                    completeMission(mission);
                }
            }
        }
        saveProgress();
    }

    /**
     * Completa una misión manualmente (para WATCH_AD, etc.)
     */
    public void completeMissionOfType(String wallpaperId, MissionType type) {
        List<Mission> missions = getMissionsForWallpaper(wallpaperId);

        for (Mission mission : missions) {
            if (mission.type == type && !isMissionCompleted(mission.id)) {
                completeMission(mission);
                break; // Solo completar una
            }
        }
    }

    /**
     * Marca una misión como completada y da recompensa
     */
    private void completeMission(Mission mission) {
        if (completedMissions.contains(mission.id)) return;

        completedMissions.add(mission.id);
        saveProgress();

        // Dar puntos de recompensa
        RewardsManager.get().addPoints(mission.rewardPoints);

        // Publicar evento
        EventBus.get().publish("mission_completed",
                new EventBus.EventData()
                        .put("mission_id", mission.id)
                        .put("mission_title", mission.title)
                        .put("reward_points", mission.rewardPoints)
                        .put("reward_badge", mission.rewardBadge));

        Log.d(TAG, "🎉 ¡Misión completada! " + mission.title + " (+" + mission.rewardPoints + " puntos)");
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. CONFIGURACIÓN DE MISIONES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Configura misiones por defecto para cada wallpaper
     */
    private void setupDefaultMissions() {
        // 🌌 Universo - Misiones espaciales
        addMission(new Mission(
                "universo_time_30", "Universo",
                "Explorador Espacial",
                "Usa el wallpaper Universo durante 30 minutos",
                MissionType.TIME_SPENT, 30, 50, "🚀"
        ));
        addMission(new Mission(
                "universo_time_120", "Universo",
                "Viajero Cósmico",
                "Usa el wallpaper Universo durante 2 horas",
                MissionType.TIME_SPENT, 120, 200, "🌟"
        ));
        addMission(new Mission(
                "universo_ad", "Universo",
                "Apoyo Galáctico",
                "Ve un anuncio para potenciar tu nave",
                MissionType.WATCH_AD, 1, 100, "💎"
        ));

        // 🕳️ Agujero Negro - Misiones oscuras
        addMission(new Mission(
                "agujero_time_30", "Agujero Negro",
                "Observador del Vacío",
                "Contempla el Agujero Negro por 30 minutos",
                MissionType.TIME_SPENT, 30, 50, "🌑"
        ));
        addMission(new Mission(
                "agujero_time_120", "Agujero Negro",
                "Maestro de la Oscuridad",
                "Sumérgete en el Agujero Negro por 2 horas",
                MissionType.TIME_SPENT, 120, 200, "⚫"
        ));
        addMission(new Mission(
                "agujero_ad", "Agujero Negro",
                "Energía Oscura",
                "Ve un anuncio para absorber energía",
                MissionType.WATCH_AD, 1, 100, "✨"
        ));

        // 🔥 Misiones globales (aplican a todos)
        addMission(new Mission(
                "global_first_wallpaper", "_GLOBAL",
                "Primer Paso",
                "Establece tu primer wallpaper",
                MissionType.SET_WALLPAPER, 1, 25, "🎯"
        ));
        addMission(new Mission(
                "global_daily_use", "_GLOBAL",
                "Usuario Dedicado",
                "Usa cualquier wallpaper por 10 minutos",
                MissionType.TIME_SPENT, 10, 10, "⭐"
        ));
    }

    /**
     * Añade una misión al registro
     */
    private void addMission(Mission mission) {
        String key = mission.wallpaperId;
        if (!wallpaperMissions.containsKey(key)) {
            wallpaperMissions.put(key, new ArrayList<>());
        }
        wallpaperMissions.get(key).add(mission);
    }

    /**
     * Carga misiones desde RemoteConfig (para actualizar dinámicamente)
     */
    public void loadMissionsFromRemoteConfig(String jsonMissions) {
        try {
            JSONArray missions = new JSONArray(jsonMissions);
            for (int i = 0; i < missions.length(); i++) {
                JSONObject m = missions.getJSONObject(i);
                Mission mission = new Mission(
                        m.getString("id"),
                        m.getString("wallpaper_id"),
                        m.getString("title"),
                        m.getString("description"),
                        MissionType.valueOf(m.getString("type")),
                        m.getInt("target"),
                        m.getInt("reward_points"),
                        m.optString("badge", "🏆")
                );
                addMission(mission);
            }
            Log.d(TAG, "📋 " + missions.length() + " misiones cargadas desde RemoteConfig");
        } catch (JSONException e) {
            Log.e(TAG, "Error parseando misiones: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 4. EVENTOS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Suscribirse a eventos relevantes
     */
    private void subscribeToEvents() {
        // Cuando se establece un wallpaper
        EventBus.get().subscribe("wallpaper_set", data -> {
            String wallpaperId = data.getString("wallpaper_id", "");
            incrementProgress(wallpaperId, MissionType.SET_WALLPAPER, 1);
            incrementProgress("_GLOBAL", MissionType.SET_WALLPAPER, 1);
        });

        // Cuando se ve un anuncio recompensado
        EventBus.get().subscribe("reward_earned", data -> {
            String currentWallpaper = getCurrentWallpaper();
            if (!currentWallpaper.isEmpty()) {
                completeMissionOfType(currentWallpaper, MissionType.WATCH_AD);
            }
        });

        // Actualizar tiempo de uso
        EventBus.get().subscribe("usage_minute_tick", data -> {
            String currentWallpaper = getCurrentWallpaper();
            if (!currentWallpaper.isEmpty()) {
                incrementProgress(currentWallpaper, MissionType.TIME_SPENT, 1);
                incrementProgress("_GLOBAL", MissionType.TIME_SPENT, 1);
            }
        });
    }

    private String getCurrentWallpaper() {
        // Obtener wallpaper actual desde preferencias
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences("blackholeglow_prefs", Context.MODE_PRIVATE);
            return prefs.getString("selected_wallpaper", "Universo");
        }
        return "Universo";
    }

    // ═══════════════════════════════════════════════════════════════
    // 5. PERSISTENCIA
    // ═══════════════════════════════════════════════════════════════

    private void loadProgress() {
        if (prefs == null) return;

        // Cargar progreso
        String progressJson = prefs.getString(KEY_MISSION_PROGRESS, "{}");
        try {
            JSONObject json = new JSONObject(progressJson);
            for (var it = json.keys(); it.hasNext();) {
                String key = it.next();
                missionProgress.put(key, json.getInt(key));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error cargando progreso: " + e.getMessage());
        }

        // Cargar completadas
        String completedJson = prefs.getString(KEY_COMPLETED_MISSIONS, "[]");
        try {
            JSONArray arr = new JSONArray(completedJson);
            for (int i = 0; i < arr.length(); i++) {
                completedMissions.add(arr.getString(i));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error cargando completadas: " + e.getMessage());
        }
    }

    private void saveProgress() {
        if (prefs == null) return;

        // Guardar progreso
        JSONObject progressJson = new JSONObject(missionProgress);
        JSONArray completedJson = new JSONArray(completedMissions);

        prefs.edit()
                .putString(KEY_MISSION_PROGRESS, progressJson.toString())
                .putString(KEY_COMPLETED_MISSIONS, completedJson.toString())
                .apply();
    }

    // ═══════════════════════════════════════════════════════════════
    // 6. UTILIDADES
    // ═══════════════════════════════════════════════════════════════

    private int getTotalMissionCount() {
        int count = 0;
        for (List<Mission> missions : wallpaperMissions.values()) {
            count += missions.size();
        }
        return count;
    }

    /**
     * Reset del singleton
     */
    public static void reset() {
        if (instance != null) {
            instance.prefs = null;
            instance.context = null;
        }
        instance = null;
        Log.d(TAG, "MissionsManager reset");
    }

    /**
     * Reset de datos (para testing)
     */
    public void resetData() {
        missionProgress.clear();
        completedMissions.clear();
        saveProgress();
        Log.d(TAG, "⚠️ Datos de misiones reseteados");
    }
}
