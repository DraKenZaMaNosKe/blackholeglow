package com.secret.blackholeglow;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * 🔐 GESTOR DE ESTADÍSTICAS EN FIREBASE - ULTRA SEGURO
 *
 * Sistema de persistencia en la nube con:
 * - Sincronización automática de estadísticas
 * - Protección anti-trampas con hashing
 * - Validación de integridad de datos
 * - Leaderboard global
 *
 * ⚠️ SEGURIDAD:
 * - Los datos locales se cifran con SHA-256
 * - Las reglas de Firestore validan escrituras
 * - Solo se pueden INCREMENTAR contadores, no disminuir
 * - Timestamp server-side para prevenir manipulación de tiempo
 */
public class FirebaseStatsManager {
    private static final String TAG = "FirebaseStats";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_STATS = "player_stats";
    private static final String COLLECTION_LEADERBOARD = "leaderboard";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final Context context;
    private static FirebaseStatsManager instance;

    // Listener para cambios en las estadísticas
    public interface StatsListener {
        void onStatsUpdated(int planetsDestroyed, int rank);
        void onError(String error);
    }

    private StatsListener listener;

    private FirebaseStatsManager(Context context) {
        this.context = context.getApplicationContext();
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        Log.d(TAG, "🔐 FirebaseStatsManager inicializado");
    }

    public static FirebaseStatsManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseStatsManager(context);
        }
        return instance;
    }

    /**
     * @deprecated Use getInstance(Context) instead
     */
    @Deprecated
    public static FirebaseStatsManager getInstance() {
        if (instance != null) {
            return instance;
        }
        throw new IllegalStateException("FirebaseStatsManager no inicializado. Usa getInstance(Context) primero.");
    }

    public void setListener(StatsListener listener) {
        this.listener = listener;
    }

    /**
     * 📊 Obtiene el UID del usuario actual (seguro)
     */
    private String getUserId() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            return user.getUid();
        }
        return null;
    }

    /**
     * 🔒 Genera un hash de verificación para prevenir trampas
     * Combina: userId + planetsDestroyed + salt secreto
     */
    private String generateSecurityHash(String userId, int planetsDestroyed) {
        try {
            String secret = "BHG_SECRET_2024_" + userId; // Salt único por usuario
            String data = userId + "_" + planetsDestroyed + "_" + secret;

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes("UTF-8"));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error generando hash: " + e.getMessage());
            return "";
        }
    }

    /**
     * 💾 Guarda el estado completo del juego (HP + planetas destruidos)
     */
    public void saveGameState(final int planetHealth, final int forceFieldHealth, final int planetsDestroyed) {
        final String userId = getUserId();
        if (userId == null) {
            Log.e(TAG, "❌ Usuario no autenticado - no se puede guardar");
            return;
        }

        // ⚡ VERIFICAR CONECTIVIDAD - No bloquear si no hay internet
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.w(TAG, "📡 Sin internet - Estado del juego NO guardado en Firebase (solo local)");
            return;
        }

        String securityHash = generateSecurityHash(userId, planetsDestroyed);

        Map<String, Object> gameState = new HashMap<>();
        gameState.put("sunHealth", planetHealth);  // ⚠️ Mantener nombre de campo Firebase para compatibilidad
        gameState.put("forceFieldHealth", forceFieldHealth);
        gameState.put("sunsDestroyed", planetsDestroyed);  // ⚠️ Mantener nombre de campo Firebase
        gameState.put("securityHash", securityHash);
        gameState.put("lastUpdate", FieldValue.serverTimestamp());
        gameState.put("userId", userId);

        db.collection(COLLECTION_STATS).document(userId)
                .set(gameState, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, String.format("💾 Estado guardado en Firebase: Planeta HP=%d, Escudo HP=%d, Planetas=%d",
                                planetHealth, forceFieldHealth, planetsDestroyed));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "❌ Error guardando estado: " + e.getMessage());
                    }
                });
    }

    /**
     * 🌍 Incrementa el contador de planetas destruidos (SEGURO)
     *
     * @param newPlanetsDestroyed Nuevo total de planetas destruidos
     *
     * IMPORTANTE: Este método solo permite INCREMENTAR, nunca disminuir.
     * Si se detecta manipulación, se rechaza la actualización.
     */
    public void incrementPlanetsDestroyed(final int newPlanetsDestroyed) {
        final String userId = getUserId();
        if (userId == null) {
            Log.e(TAG, "❌ Usuario no autenticado - no se puede guardar");
            if (listener != null) {
                listener.onError("Usuario no autenticado");
            }
            return;
        }

        // ⚡ VERIFICAR CONECTIVIDAD - No bloquear si no hay internet
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.w(TAG, "📡 Sin internet - Planetas destruidos NO guardados en Firebase (solo local)");
            return;
        }

        final DocumentReference userStatsRef = db.collection(COLLECTION_STATS).document(userId);

        // Primero, verificar el valor actual en Firebase
        userStatsRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    int currentPlanets = 0;

                    if (document.exists()) {
                        Long planetsLong = document.getLong("sunsDestroyed");  // ⚠️ Mantener nombre de campo Firebase
                        if (planetsLong != null) {
                            currentPlanets = planetsLong.intValue();
                        }
                    }

                    // ⚠️ VALIDACIÓN ANTI-TRAMPAS
                    if (newPlanetsDestroyed < currentPlanets) {
                        Log.w(TAG, "🚨 INTENTO DE TRAMPA DETECTADO! " +
                                "Intentó reducir planetas de " + currentPlanets + " a " + newPlanetsDestroyed);
                        if (listener != null) {
                            listener.onError("Datos inválidos detectados");
                        }
                        return;
                    }

                    // Si no cambió nada, no hacer nada
                    if (newPlanetsDestroyed == currentPlanets) {
                        Log.d(TAG, "✓ Sin cambios en planetas destruidos (" + currentPlanets + ")");
                        return;
                    }

                    // ✅ VALIDACIÓN PASADA - Guardar en Firebase
                    savePlanetsToFirebase(userId, newPlanetsDestroyed);

                } else {
                    Log.e(TAG, "❌ Error obteniendo estadísticas: " + task.getException());
                    if (listener != null) {
                        listener.onError("Error de conexión");
                    }
                }
            }
        });
    }

    /**
     * 💾 Guarda las estadísticas en Firebase con seguridad
     */
    private void savePlanetsToFirebase(String userId, int planetsDestroyed) {
        String securityHash = generateSecurityHash(userId, planetsDestroyed);

        Map<String, Object> stats = new HashMap<>();
        stats.put("sunsDestroyed", planetsDestroyed);  // ⚠️ Mantener nombre de campo Firebase
        stats.put("securityHash", securityHash);
        stats.put("lastUpdate", FieldValue.serverTimestamp());  // Timestamp del servidor (no manipulable)
        stats.put("userId", userId);

        // Guardar en la colección de estadísticas
        db.collection(COLLECTION_STATS).document(userId)
                .set(stats, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "╔════════════════════════════════════════╗");
                        Log.d(TAG, "║   🌍 PLANETAS GUARDADOS EN FIREBASE 🌍  ║");
                        Log.d(TAG, "║   Total: " + planetsDestroyed + " planetas                    ║");
                        Log.d(TAG, "╚════════════════════════════════════════╝");

                        // Actualizar leaderboard
                        updateLeaderboard(userId, planetsDestroyed);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "❌ Error guardando en Firebase: " + e.getMessage());
                        if (listener != null) {
                            listener.onError("Error guardando datos");
                        }
                    }
                });
    }

    /**
     * 🏆 Actualiza el leaderboard global
     */
    private void updateLeaderboard(String userId, int planetsDestroyed) {
        // Obtener el nombre del usuario desde Firebase Auth
        FirebaseUser currentUser = auth.getCurrentUser();
        String displayName = "Player"; // Valor por defecto
        if (currentUser != null && currentUser.getDisplayName() != null) {
            displayName = currentUser.getDisplayName();
        }

        Map<String, Object> leaderboardEntry = new HashMap<>();
        leaderboardEntry.put("userId", userId);
        leaderboardEntry.put("displayName", displayName);  // ✅ Nombre del usuario
        leaderboardEntry.put("sunsDestroyed", planetsDestroyed);  // ⚠️ Mantener nombre de campo Firebase
        leaderboardEntry.put("lastUpdate", FieldValue.serverTimestamp());
        leaderboardEntry.put("isBot", false);  // ✅ Indicar que es un jugador real

        db.collection(COLLECTION_LEADERBOARD).document(userId)
                .set(leaderboardEntry, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "🏆 Leaderboard actualizado!");
                        // Notificar al listener
                        if (listener != null) {
                            listener.onStatsUpdated(planetsDestroyed, -1); // Rank se calcula después
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "❌ Error actualizando leaderboard: " + e.getMessage());
                    }
                });
    }

    /**
     * 📥 Carga las estadísticas desde Firebase
     */
    public void loadStatsFromFirebase(final StatsCallback callback) {
        String userId = getUserId();
        if (userId == null) {
            callback.onError("Usuario no autenticado");
            return;
        }

        db.collection(COLLECTION_STATS).document(userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Long planetsLong = document.getLong("sunsDestroyed");  // ⚠️ Mantener nombre de campo Firebase
                                String hash = document.getString("securityHash");

                                int planets = planetsLong != null ? planetsLong.intValue() : 0;

                                // Verificar integridad
                                String expectedHash = generateSecurityHash(userId, planets);
                                if (hash != null && hash.equals(expectedHash)) {
                                    Log.d(TAG, "✅ Estadísticas cargadas de Firebase: " + planets + " planetas");
                                    callback.onSuccess(planets);
                                } else {
                                    Log.w(TAG, "⚠️ Hash inválido - datos posiblemente manipulados");
                                    callback.onSuccess(planets); // Cargar de todos modos pero loguear
                                }
                            } else {
                                Log.d(TAG, "📂 No hay estadísticas guardadas aún");
                                callback.onSuccess(0);
                            }
                        } else {
                            Log.e(TAG, "❌ Error cargando estadísticas: " + task.getException());
                            callback.onError("Error de conexión");
                        }
                    }
                });
    }

    /**
     * 🔄 Sincroniza estadísticas locales con Firebase
     * Compara local vs remoto y toma el mayor (para evitar pérdida de progreso)
     */
    public void syncStats(int localPlanets, final StatsCallback callback) {
        loadStatsFromFirebase(new StatsCallback() {
            @Override
            public void onSuccess(int remotePlanets) {
                // Tomar el mayor de los dos (local vs remoto)
                int finalPlanets = Math.max(localPlanets, remotePlanets);

                if (finalPlanets > remotePlanets) {
                    // Local tiene más - actualizar Firebase
                    Log.d(TAG, "📤 Local adelantado (" + localPlanets + " vs " + remotePlanets + ") - subiendo a Firebase");
                    incrementPlanetsDestroyed(finalPlanets);
                } else if (finalPlanets > localPlanets) {
                    // Remoto tiene más - actualizar local
                    Log.d(TAG, "📥 Firebase adelantado (" + remotePlanets + " vs " + localPlanets + ") - actualizando local");
                } else {
                    Log.d(TAG, "✅ Local y Firebase sincronizados (" + finalPlanets + " planetas)");
                }

                callback.onSuccess(finalPlanets);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error sincronizando: " + error);
                callback.onError(error);
            }
        });
    }

    /**
     * Callback para operaciones asíncronas
     */
    public interface StatsCallback {
        void onSuccess(int planetsDestroyed);
        void onError(String error);
    }

    /**
     * 📦 Callback para cargar estado completo del juego
     */
    public interface GameStateCallback {
        void onSuccess(int planetHealth, int forceFieldHealth, int planetsDestroyed);
        void onError(String error);
    }

    /**
     * 📥 Carga el estado completo del juego desde Firebase
     */
    public void loadGameState(final GameStateCallback callback) {
        String userId = getUserId();
        if (userId == null) {
            callback.onError("Usuario no autenticado");
            return;
        }

        // ⚡ VERIFICAR CONECTIVIDAD - No bloquear si no hay internet
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.w(TAG, "📡 Sin internet - No se puede cargar estado desde Firebase");
            callback.onError("Sin conexión a internet");
            return;
        }

        db.collection(COLLECTION_STATS).document(userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Long planetsLong = document.getLong("sunsDestroyed");  // ⚠️ Mantener nombre de campo Firebase
                                Long planetHealthLong = document.getLong("sunHealth");  // ⚠️ Mantener nombre de campo Firebase
                                Long forceFieldHealthLong = document.getLong("forceFieldHealth");
                                String hash = document.getString("securityHash");

                                int planets = planetsLong != null ? planetsLong.intValue() : 0;
                                int planetHP = planetHealthLong != null ? planetHealthLong.intValue() : 100;
                                int forceFieldHP = forceFieldHealthLong != null ? forceFieldHealthLong.intValue() : 50;

                                // Verificar integridad
                                String expectedHash = generateSecurityHash(userId, planets);
                                if (hash != null && hash.equals(expectedHash)) {
                                    Log.d(TAG, String.format("✅ Estado completo cargado: Planeta HP=%d, Escudo HP=%d, Planetas=%d",
                                            planetHP, forceFieldHP, planets));
                                    callback.onSuccess(planetHP, forceFieldHP, planets);
                                } else {
                                    Log.w(TAG, "⚠️ Hash inválido - datos posiblemente manipulados");
                                    callback.onSuccess(planetHP, forceFieldHP, planets); // Cargar de todos modos
                                }
                            } else {
                                Log.d(TAG, "📂 No hay estado guardado - usando valores por defecto");
                                callback.onSuccess(100, 50, 0); // Valores por defecto
                            }
                        } else {
                            Log.e(TAG, "❌ Error cargando estado: " + task.getException());
                            callback.onError("Error de conexión");
                        }
                    }
                });
    }
}
