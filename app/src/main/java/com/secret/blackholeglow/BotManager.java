package com.secret.blackholeglow;

import android.util.Log;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 🤖 GESTOR DE BOTS COMPETIDORES
 *
 * Crea y administra 3 bots que compiten en el leaderboard:
 * - Bot Champion (el mejor, difícil de alcanzar)
 * - Bot Master (intermedio, alcanzable con esfuerzo)
 * - Bot Hunter (más débil, primer objetivo)
 *
 * 🎯 ESTRATEGIA:
 * - Bots inician con puntuaciones superiores al usuario promedio
 * - Se incrementan periódicamente según estadísticas globales
 * - "Se dejan ganar" cuando jugadores reales están cerca
 * - Mantienen competencia justa y motivante
 */
public class BotManager {
    private static final String TAG = "BotManager";
    private static final String COLLECTION_BOTS = "player_stats";
    private static final String COLLECTION_LEADERBOARD = "leaderboard";

    // 🤖 Configuración de los 3 bots
    private static final String BOT1_ID = "bot_champion";
    private static final String BOT1_NAME = "🏆 Champion";
    private static final int BOT1_INITIAL_SUNS = 100;  // El mejor

    private static final String BOT2_ID = "bot_master";
    private static final String BOT2_NAME = "⚡ Master";
    private static final int BOT2_INITIAL_SUNS = 65;   // Intermedio

    private static final String BOT3_ID = "bot_hunter";
    private static final String BOT3_NAME = "🎯 Hunter";
    private static final int BOT3_INITIAL_SUNS = 35;   // Más accesible

    private final FirebaseFirestore db;
    private final Random random;
    private static BotManager instance;

    // Última actualización de bots (para no actualizar muy seguido)
    private long lastBotUpdate = 0;
    private static final long BOT_UPDATE_INTERVAL = 3600000; // 1 hora en ms

    private BotManager() {
        this.db = FirebaseFirestore.getInstance();
        this.random = new Random();
        Log.d(TAG, "🤖 BotManager inicializado");
    }

    public static BotManager getInstance() {
        if (instance == null) {
            instance = new BotManager();
        }
        return instance;
    }

    /**
     * 🚀 Inicializa los 3 bots en Firebase (solo primera vez)
     * Verifica si existen, si no, los crea con puntuaciones iniciales
     */
    public void initializeBots(final InitCallback callback) {
        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║   🤖 INICIALIZANDO BOTS              ║");
        Log.d(TAG, "╚════════════════════════════════════════╝");

        // Verificar e inicializar cada bot
        initializeBot(BOT1_ID, BOT1_NAME, BOT1_INITIAL_SUNS, new BotInitCallback() {
            @Override
            public void onSuccess(boolean created) {
                if (created) Log.d(TAG, "✅ " + BOT1_NAME + " creado con " + BOT1_INITIAL_SUNS + " soles");

                initializeBot(BOT2_ID, BOT2_NAME, BOT2_INITIAL_SUNS, new BotInitCallback() {
                    @Override
                    public void onSuccess(boolean created) {
                        if (created) Log.d(TAG, "✅ " + BOT2_NAME + " creado con " + BOT2_INITIAL_SUNS + " soles");

                        initializeBot(BOT3_ID, BOT3_NAME, BOT3_INITIAL_SUNS, new BotInitCallback() {
                            @Override
                            public void onSuccess(boolean created) {
                                if (created) Log.d(TAG, "✅ " + BOT3_NAME + " creado con " + BOT3_INITIAL_SUNS + " soles");
                                Log.d(TAG, "🎮 Todos los bots están listos!");
                                if (callback != null) callback.onComplete();
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "❌ Error inicializando " + BOT3_NAME + ": " + error);
                                if (callback != null) callback.onComplete();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "❌ Error inicializando " + BOT2_NAME + ": " + error);
                        if (callback != null) callback.onComplete();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error inicializando " + BOT1_NAME + ": " + error);
                if (callback != null) callback.onComplete();
            }
        });
    }

    /**
     * Inicializa un bot individual
     */
    private void initializeBot(final String botId, final String botName, final int initialPlanets, final BotInitCallback callback) {
        db.collection(COLLECTION_BOTS).document(botId)
            .get()
            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot doc = task.getResult();
                        if (!doc.exists()) {
                            // Bot no existe, crearlo
                            createBot(botId, botName, initialPlanets, callback);
                        } else {
                            // Bot ya existe
                            Log.d(TAG, "🤖 " + botName + " ya existe");
                            callback.onSuccess(false);
                        }
                    } else {
                        callback.onError("Error verificando bot: " + task.getException());
                    }
                }
            });
    }

    /**
     * Crea un bot nuevo en Firebase
     */
    private void createBot(String botId, String botName, int initialPlanets, final BotInitCallback callback) {
        Map<String, Object> botData = new HashMap<>();
        botData.put("userId", botId);
        botData.put("displayName", botName);
        botData.put("sunsDestroyed", initialPlanets);  // ⚠️ Mantener nombre de campo Firebase
        botData.put("isBot", true);
        botData.put("securityHash", "bot_verified");
        botData.put("lastUpdate", FieldValue.serverTimestamp());

        // Guardar en player_stats
        db.collection(COLLECTION_BOTS).document(botId)
            .set(botData)
            .addOnSuccessListener(aVoid -> {
                // También agregar al leaderboard
                Map<String, Object> leaderboardData = new HashMap<>();
                leaderboardData.put("userId", botId);
                leaderboardData.put("displayName", botName);
                leaderboardData.put("sunsDestroyed", initialPlanets);  // ⚠️ Mantener nombre de campo Firebase
                leaderboardData.put("isBot", true);
                leaderboardData.put("lastUpdate", FieldValue.serverTimestamp());

                db.collection(COLLECTION_LEADERBOARD).document(botId)
                    .set(leaderboardData)
                    .addOnSuccessListener(aVoid2 -> {
                        callback.onSuccess(true);
                    })
                    .addOnFailureListener(e -> {
                        callback.onError("Error agregando bot al leaderboard: " + e.getMessage());
                    });
            })
            .addOnFailureListener(e -> {
                callback.onError("Error creando bot: " + e.getMessage());
            });
    }

    /**
     * 📈 Actualiza puntos de los bots según algoritmo adaptativo
     * Solo actualiza si ha pasado el intervalo de tiempo
     */
    public void updateBotsIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastBotUpdate < BOT_UPDATE_INTERVAL) {
            Log.d(TAG, "⏱️ Bots actualizados recientemente, saltando...");
            return;
        }

        Log.d(TAG, "🔄 Actualizando bots...");
        lastBotUpdate = now;

        // Primero, obtener estadísticas de jugadores reales
        getAveragePlayerStats(new StatsCallback() {
            @Override
            public void onSuccess(int avgPlanets, int topPlayerPlanets) {
                Log.d(TAG, "📊 Promedio jugadores: " + avgPlanets + " | Top player: " + topPlayerPlanets);

                // Actualizar cada bot con algoritmo adaptativo
                updateBotAdaptive(BOT1_ID, BOT1_NAME, avgPlanets, topPlayerPlanets, 3.0f, 5, 10);
                updateBotAdaptive(BOT2_ID, BOT2_NAME, avgPlanets, topPlayerPlanets, 2.0f, 3, 7);
                updateBotAdaptive(BOT3_ID, BOT3_NAME, avgPlanets, topPlayerPlanets, 1.5f, 1, 5);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error obteniendo stats: " + error);
            }
        });
    }

    /**
     * 🧠 Algoritmo adaptativo para actualizar un bot
     * @param multiplier Multiplicador del promedio (ej: 2.0 = doble del promedio)
     * @param minIncrement Incremento mínimo
     * @param maxIncrement Incremento máximo
     */
    private void updateBotAdaptive(final String botId, final String botName,
                                   final int avgPlanets, final int topPlayerPlanets,
                                   final float multiplier, final int minIncrement, final int maxIncrement) {

        db.collection(COLLECTION_BOTS).document(botId)
            .get()
            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot doc = task.getResult();
                        Long currentPlanets = doc.getLong("sunsDestroyed");  // ⚠️ Mantener nombre de campo Firebase
                        if (currentPlanets == null) currentPlanets = 0L;

                        int targetPlanets = (int)(avgPlanets * multiplier);

                        // 🎯 LÓGICA ADAPTATIVA
                        int increment;

                        if (currentPlanets < targetPlanets) {
                            // Bot está por debajo del objetivo, crecer más rápido
                            increment = random.nextInt(maxIncrement - minIncrement + 1) + minIncrement;
                        } else if (topPlayerPlanets > currentPlanets - 15) {
                            // Jugador real está cerca, "dejarse ganar" (no crecer o crecer poco)
                            increment = random.nextInt(2); // 0 o 1
                            Log.d(TAG, "🎯 " + botName + " se está dejando alcanzar! (top player cerca)");
                        } else {
                            // Bot está bien posicionado, crecer normalmente
                            increment = random.nextInt(maxIncrement - minIncrement + 1) + minIncrement;
                        }

                        if (increment > 0) {
                            int newPlanets = currentPlanets.intValue() + increment;
                            updateBotPlanets(botId, botName, newPlanets);
                        } else {
                            Log.d(TAG, "🤖 " + botName + " no crece esta ronda");
                        }
                    }
                }
            });
    }

    /**
     * Actualiza los planetas de un bot en Firebase
     */
    private void updateBotPlanets(String botId, String botName, int newPlanets) {
        Map<String, Object> update = new HashMap<>();
        update.put("sunsDestroyed", newPlanets);  // ⚠️ Mantener nombre de campo Firebase
        update.put("lastUpdate", FieldValue.serverTimestamp());

        // Actualizar en player_stats
        db.collection(COLLECTION_BOTS).document(botId)
            .update(update)
            .addOnSuccessListener(aVoid -> {
                // Actualizar en leaderboard
                db.collection(COLLECTION_LEADERBOARD).document(botId)
                    .update(update)
                    .addOnSuccessListener(aVoid2 -> {
                        Log.d(TAG, "✅ " + botName + " actualizado a " + newPlanets + " planetas");
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Error actualizando " + botName + ": " + e.getMessage());
            });
    }

    /**
     * 📊 Obtiene estadísticas promedio de jugadores REALES (no bots)
     */
    private void getAveragePlayerStats(final StatsCallback callback) {
        db.collection(COLLECTION_LEADERBOARD)
            .whereEqualTo("isBot", false)  // Solo jugadores reales
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot = task.getResult();

                        if (snapshot.isEmpty()) {
                            // No hay jugadores reales aún, usar valores por defecto
                            callback.onSuccess(10, 10);
                            return;
                        }

                        int totalPlanets = 0;
                        int topPlanets = 0;
                        int count = 0;

                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Long planets = doc.getLong("sunsDestroyed");  // ⚠️ Mantener nombre de campo Firebase
                            if (planets != null) {
                                totalPlanets += planets;
                                count++;
                                if (planets > topPlanets) {
                                    topPlanets = planets.intValue();
                                }
                            }
                        }

                        int avgPlanets = count > 0 ? totalPlanets / count : 10;
                        callback.onSuccess(avgPlanets, topPlanets);
                    } else {
                        callback.onError("Error consultando stats: " + task.getException());
                    }
                }
            });
    }

    /**
     * Callbacks
     */
    public interface InitCallback {
        void onComplete();
    }

    private interface BotInitCallback {
        void onSuccess(boolean created);
        void onError(String error);
    }

    private interface StatsCallback {
        void onSuccess(int avgPlanets, int topPlayerPlanets);
        void onError(String error);
    }
}
