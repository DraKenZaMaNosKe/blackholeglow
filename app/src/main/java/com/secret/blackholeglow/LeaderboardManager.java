package com.secret.blackholeglow;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 🏆 GESTOR DEL LEADERBOARD (TABLA DE CLASIFICACIÓN)
 *
 * Consulta y gestiona el Top 3 de jugadores:
 * - Top 3 globales (pueden ser bots o jugadores reales)
 *
 * 🚀 OPTIMIZADO:
 * - Cache local con TTL de 30 segundos
 * - Queries limitados (solo Top 3)
 * - Actualización asíncrona (no bloquea render)
 */
public class LeaderboardManager {
    private static final String TAG = "LeaderboardManager";
    private static final String COLLECTION_LEADERBOARD = "leaderboard";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private static LeaderboardManager instance;

    // Cache con TTL de 30 segundos
    private List<LeaderboardEntry> cachedTop3 = null;
    private long lastUpdate = 0;
    private static final long CACHE_TTL = 30000; // 30 segundos

    // Listener para cambios en el leaderboard
    private LeaderboardListener listener;

    private LeaderboardManager() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        Log.d(TAG, "🏆 LeaderboardManager inicializado");
    }

    public static LeaderboardManager getInstance() {
        if (instance == null) {
            instance = new LeaderboardManager();
        }
        return instance;
    }

    public void setListener(LeaderboardListener listener) {
        this.listener = listener;
    }

    /**
     * 🔝 Obtiene el Top 3 del leaderboard
     * Incluye cache para optimizar rendimiento
     */
    public void getTop3(final Top3Callback callback) {
        long now = System.currentTimeMillis();

        // Verificar cache
        if (cachedTop3 != null && (now - lastUpdate) < CACHE_TTL) {
            Log.d(TAG, "📦 Usando cache del leaderboard");
            callback.onSuccess(cachedTop3);
            return;
        }

        // Consultar Firebase
        Log.d(TAG, "🔄 Consultando Top 3 desde Firebase (solo jugadores reales)...");

        // ⚠️ FILTRAR BOTS - Solo mostrar jugadores reales en el leaderboard
        db.collection(COLLECTION_LEADERBOARD)
            .whereEqualTo("isBot", false)  // ✅ SOLO JUGADORES REALES
            .orderBy("sunsDestroyed", Query.Direction.DESCENDING)
            .limit(3)  // Solo Top 3
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot = task.getResult();
                        List<LeaderboardEntry> entries = new ArrayList<>();

                        int rank = 1;

                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            String userId = doc.getString("userId");
                            String displayName = doc.getString("displayName");
                            Long planets = doc.getLong("sunsDestroyed");  // ⚠️ Mantener nombre de campo Firebase
                            Boolean isBot = doc.getBoolean("isBot");

                            if (userId != null && planets != null) {
                                LeaderboardEntry entry = new LeaderboardEntry(
                                    userId,
                                    displayName != null ? displayName : "Player",
                                    planets.intValue(),
                                    isBot != null && isBot,
                                    rank
                                );

                                entries.add(entry);
                                rank++;
                            }
                        }

                        // Actualizar cache
                        cachedTop3 = entries;
                        lastUpdate = System.currentTimeMillis();

                        Log.d(TAG, "✅ Top 3 obtenido: " + entries.size() + " entradas");
                        callback.onSuccess(entries);

                        // Notificar listener
                        if (listener != null) {
                            listener.onLeaderboardUpdated(entries);
                        }
                    } else {
                        Log.e(TAG, "❌ Error consultando leaderboard: " + task.getException());
                        callback.onError("Error consultando leaderboard");
                    }
                }
            });
    }

    /**
     * 🔄 Fuerza actualización del cache (ignorando TTL)
     */
    public void forceRefresh() {
        cachedTop3 = null;
        lastUpdate = 0;
        Log.d(TAG, "🔄 Cache limpiado, forzando actualización...");
    }

    /**
     * 👤 Obtiene el userId del usuario actual
     */
    private String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    /**
     * 📊 Clase para representar una entrada del leaderboard
     */
    public static class LeaderboardEntry {
        public final String userId;
        public final String displayName;
        public final int planetsDestroyed;
        public final boolean isBot;
        public final int rank;
        public Bitmap avatarBitmap;  // Se carga después

        public LeaderboardEntry(String userId, String displayName, int planetsDestroyed, boolean isBot, int rank) {
            this.userId = userId;
            this.displayName = displayName;
            this.planetsDestroyed = planetsDestroyed;
            this.isBot = isBot;
            this.rank = rank;
            this.avatarBitmap = null;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof LeaderboardEntry) {
                return userId.equals(((LeaderboardEntry) obj).userId);
            }
            return false;
        }

        @Override
        public String toString() {
            return "#" + rank + " " + displayName + " - " + planetsDestroyed + " planetas" + (isBot ? " [BOT]" : "");
        }
    }

    /**
     * Callbacks
     */
    public interface Top3Callback {
        void onSuccess(List<LeaderboardEntry> top3);
        void onError(String error);
    }

    public interface LeaderboardListener {
        void onLeaderboardUpdated(List<LeaderboardEntry> top3);
    }
}
