package com.secret.blackholeglow.systems;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                         EventBus                                  ║
 * ║                      "El Mensajero"                               ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  Sistema de comunicación desacoplada entre componentes.          ║
 * ║                                                                  ║
 * ║  Permite que:                                                    ║
 * ║  • Un planeta notifique que fue destruido                        ║
 * ║  • Un meteorito notifique que impactó                            ║
 * ║  • El botón de like notifique que fue presionado                 ║
 * ║  • La música notifique cambios de beat                           ║
 * ║                                                                  ║
 * ║  Sin que los componentes se conozcan entre sí.                   ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * USO:
 *
 * // Suscribirse a eventos
 * EventBus.get().subscribe(EventBus.PLANET_DESTROYED, (data) -> {
 *     float x = data.getFloat("x");
 *     float y = data.getFloat("y");
 *     // Hacer algo...
 * });
 *
 * // Publicar evento
 * EventBus.get().publish(EventBus.PLANET_DESTROYED,
 *     new EventData().put("x", 1.5f).put("y", 2.0f));
 */
public class EventBus {
    private static final String TAG = "EventBus";

    // ═══════════════════════════════════════════════════════════════
    // 📢 TIPOS DE EVENTOS
    // ═══════════════════════════════════════════════════════════════

    // 🌍 Eventos de planetas
    public static final String PLANET_HIT = "planet_hit";
    public static final String PLANET_DESTROYED = "planet_destroyed";
    public static final String PLANET_RESPAWNED = "planet_respawned";

    // ☄️ Eventos de meteoritos
    public static final String METEOR_LAUNCHED = "meteor_launched";
    public static final String METEOR_IMPACT = "meteor_impact";
    public static final String METEOR_DESTROYED = "meteor_destroyed";

    // 🛸 Eventos de enemigos
    public static final String UFO_SPAWNED = "ufo_spawned";
    public static final String UFO_DESTROYED = "ufo_destroyed";
    public static final String UFO_LASER_FIRED = "ufo_laser_fired";

    // 🎮 Eventos de UI
    public static final String LIKE_PRESSED = "like_pressed";
    public static final String FIRE_PRESSED = "fire_pressed";
    public static final String PLAY_PAUSE_TOGGLED = "play_pause_toggled";
    public static final String STOP_PRESSED = "stop_pressed";

    // 🎵 Eventos de música
    public static final String MUSIC_BEAT = "music_beat";
    public static final String MUSIC_LEVELS_CHANGED = "music_levels_changed";
    public static final String SONG_CHANGED = "song_changed";

    // 💥 Eventos de efectos
    public static final String SCREEN_FLASH = "screen_flash";
    public static final String SCREEN_CRACK = "screen_crack";
    public static final String EXPLOSION = "explosion";

    // 🎬 Eventos de escena
    public static final String SCENE_LOADED = "scene_loaded";
    public static final String SCENE_UNLOADED = "scene_unloaded";
    public static final String SCENE_PAUSED = "scene_paused";
    public static final String SCENE_RESUMED = "scene_resumed";
    public static final String SCENE_CHANGED = "scene_changed";

    // 🏆 Eventos de juego
    public static final String SCORE_CHANGED = "score_changed";
    public static final String COMBO_CHANGED = "combo_changed";
    public static final String LEADERBOARD_UPDATED = "leaderboard_updated";

    // ═══════════════════════════════════════════════════════════════
    // 🔧 SINGLETON
    // ═══════════════════════════════════════════════════════════════

    private static EventBus instance;

    public static EventBus get() {
        if (instance == null) {
            instance = new EventBus();
        }
        return instance;
    }

    // ═══════════════════════════════════════════════════════════════
    // 📦 ALMACENAMIENTO DE SUSCRIPTORES
    // ═══════════════════════════════════════════════════════════════

    private final Map<String, List<EventListener>> listeners = new HashMap<>();
    private boolean debugMode = false;

    private EventBus() {
        Log.d(TAG, "📢 EventBus inicializado");
    }

    // ═══════════════════════════════════════════════════════════════
    // 📝 SUSCRIPCIÓN A EVENTOS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Suscribirse a un tipo de evento
     * @param eventType Tipo de evento (usar constantes de EventBus)
     * @param listener Callback que se ejecutará cuando ocurra el evento
     * @return SubscriptionToken para cancelar la suscripción
     */
    public SubscriptionToken subscribe(String eventType, EventListener listener) {
        if (!listeners.containsKey(eventType)) {
            listeners.put(eventType, new ArrayList<>());
        }
        listeners.get(eventType).add(listener);

        if (debugMode) {
            Log.d(TAG, "➕ Suscriptor añadido para: " + eventType +
                       " (total: " + listeners.get(eventType).size() + ")");
        }

        return new SubscriptionToken(eventType, listener);
    }

    /**
     * Cancelar suscripción usando el token
     */
    public void unsubscribe(SubscriptionToken token) {
        if (token != null && listeners.containsKey(token.eventType)) {
            listeners.get(token.eventType).remove(token.listener);

            if (debugMode) {
                Log.d(TAG, "➖ Suscriptor removido de: " + token.eventType);
            }
        }
    }

    /**
     * Cancelar todas las suscripciones de un tipo de evento
     */
    public void unsubscribeAll(String eventType) {
        if (listeners.containsKey(eventType)) {
            int count = listeners.get(eventType).size();
            listeners.get(eventType).clear();

            if (debugMode) {
                Log.d(TAG, "🗑️ Removidos " + count + " suscriptores de: " + eventType);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 📣 PUBLICACIÓN DE EVENTOS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Publicar un evento sin datos
     */
    public void publish(String eventType) {
        publish(eventType, new EventData());
    }

    /**
     * Publicar un evento con datos
     */
    public void publish(String eventType, EventData data) {
        if (debugMode) {
            Log.d(TAG, "📣 Publicando: " + eventType);
        }

        List<EventListener> eventListeners = listeners.get(eventType);
        if (eventListeners != null && !eventListeners.isEmpty()) {
            // Crear copia para evitar ConcurrentModificationException
            List<EventListener> listenersCopy = new ArrayList<>(eventListeners);

            for (EventListener listener : listenersCopy) {
                try {
                    listener.onEvent(data);
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error en listener de " + eventType + ": " + e.getMessage());
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔧 UTILIDADES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Activar/desactivar modo debug
     */
    public void setDebugMode(boolean enabled) {
        this.debugMode = enabled;
        Log.d(TAG, "🔧 Debug mode: " + (enabled ? "ON" : "OFF"));
    }

    /**
     * Limpiar todos los suscriptores (llamar al destruir)
     */
    public void clear() {
        int total = 0;
        for (List<EventListener> list : listeners.values()) {
            total += list.size();
        }
        listeners.clear();
        Log.d(TAG, "🧹 EventBus limpiado (" + total + " suscriptores removidos)");
    }

    /**
     * Reset singleton (para recreación completa)
     */
    public static void reset() {
        if (instance != null) {
            instance.clear();
            instance = null;
        }
        Log.d(TAG, "🔄 EventBus reset");
    }

    /**
     * Obtener cantidad de suscriptores para un evento
     */
    public int getSubscriberCount(String eventType) {
        List<EventListener> list = listeners.get(eventType);
        return list != null ? list.size() : 0;
    }

    // ═══════════════════════════════════════════════════════════════
    // 📦 CLASES INTERNAS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Interfaz para escuchar eventos
     */
    public interface EventListener {
        void onEvent(EventData data);
    }

    /**
     * Token para cancelar suscripción
     */
    public static class SubscriptionToken {
        final String eventType;
        final EventListener listener;

        SubscriptionToken(String eventType, EventListener listener) {
            this.eventType = eventType;
            this.listener = listener;
        }
    }

    /**
     * Contenedor de datos para eventos (tipo-seguro)
     */
    public static class EventData {
        private final Map<String, Object> data = new HashMap<>();

        // ═══ SETTERS (encadenables) ═══

        public EventData put(String key, float value) {
            data.put(key, value);
            return this;
        }

        public EventData put(String key, int value) {
            data.put(key, value);
            return this;
        }

        public EventData put(String key, boolean value) {
            data.put(key, value);
            return this;
        }

        public EventData put(String key, String value) {
            data.put(key, value);
            return this;
        }

        public EventData put(String key, Object value) {
            data.put(key, value);
            return this;
        }

        // ═══ GETTERS (con valores por defecto) ═══

        public float getFloat(String key) {
            return getFloat(key, 0f);
        }

        public float getFloat(String key, float defaultValue) {
            Object value = data.get(key);
            if (value instanceof Float) return (Float) value;
            if (value instanceof Number) return ((Number) value).floatValue();
            return defaultValue;
        }

        public int getInt(String key) {
            return getInt(key, 0);
        }

        public int getInt(String key, int defaultValue) {
            Object value = data.get(key);
            if (value instanceof Integer) return (Integer) value;
            if (value instanceof Number) return ((Number) value).intValue();
            return defaultValue;
        }

        public boolean getBoolean(String key) {
            return getBoolean(key, false);
        }

        public boolean getBoolean(String key, boolean defaultValue) {
            Object value = data.get(key);
            if (value instanceof Boolean) return (Boolean) value;
            return defaultValue;
        }

        public String getString(String key) {
            return getString(key, "");
        }

        public String getString(String key, String defaultValue) {
            Object value = data.get(key);
            if (value instanceof String) return (String) value;
            if (value != null) return value.toString();
            return defaultValue;
        }

        @SuppressWarnings("unchecked")
        public <T> T get(String key, Class<T> type) {
            Object value = data.get(key);
            if (type.isInstance(value)) {
                return (T) value;
            }
            return null;
        }

        public boolean has(String key) {
            return data.containsKey(key);
        }

        @Override
        public String toString() {
            return "EventData" + data.toString();
        }
    }
}
