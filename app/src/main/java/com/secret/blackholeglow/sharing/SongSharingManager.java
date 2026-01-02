package com.secret.blackholeglow.sharing;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.secret.blackholeglow.NetworkUtils;
import com.secret.blackholeglow.systems.FirebaseQueueManager;

import java.util.HashMap;
import java.util.Map;

/**
 * 🎵 GESTOR DE CANCIONES COMPARTIDAS
 *
 * Maneja el envío y recepción de canciones compartidas en tiempo real.
 * Usa Firebase Firestore para sincronización instantánea entre usuarios.
 *
 * Características:
 * - Compartir canciones con un toque
 * - Recibir canciones de otros usuarios en tiempo real
 * - Rate limiting (1 canción cada 5 minutos)
 * - Verificación de conectividad
 */
public class SongSharingManager {
    private static final String TAG = "SongSharing";
    private static final String COLLECTION_SHARED_SONGS = "shared_songs";
    private static final long RATE_LIMIT_MS = 5 * 60 * 1000;  // 5 minutos anti-spam

    private final Context context;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private static SongSharingManager instance;
    private ListenerRegistration songsListener;
    private long lastShareTime = 0;

    // 🔄 POLLING FALLBACK para dispositivos viejos
    private android.os.Handler pollingHandler;
    private Runnable pollingRunnable;
    private String lastPolledSongId = null;
    private static final long POLLING_INTERVAL_MS = 3000;  // Cada 3 segundos

    // Callback para nuevas canciones
    public interface OnNewSongListener {
        void onNewSong(SharedSong song);
    }

    private OnNewSongListener newSongListener;

    // ═══════════════════════════════════════════════════════════
    // SINGLETON
    // ═══════════════════════════════════════════════════════════

    private SongSharingManager(Context context) {
        this.context = context.getApplicationContext();
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        Log.d(TAG, "🎵 SongSharingManager inicializado");
    }

    public static SongSharingManager getInstance(Context context) {
        if (instance == null) {
            instance = new SongSharingManager(context);
        }
        return instance;
    }

    // ═══════════════════════════════════════════════════════════
    // COMPARTIR CANCIÓN
    // ═══════════════════════════════════════════════════════════

    /**
     * 🎵 Comparte una canción con todos los usuarios
     *
     * @param songTitle Título de la canción
     * @param callback  Callback con resultado
     */
    public void shareSong(String songTitle, ShareCallback callback) {
        // Verificar usuario autenticado
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "❌ Usuario no autenticado");
            if (callback != null) callback.onError("Debes iniciar sesión para compartir");
            return;
        }

        // Verificar conectividad
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.w(TAG, "📡 Sin conexión a internet");
            if (callback != null) callback.onError("Sin conexión a internet");
            return;
        }

        // Verificar rate limiting
        long now = System.currentTimeMillis();
        if (now - lastShareTime < RATE_LIMIT_MS) {
            long remainingSeconds = (RATE_LIMIT_MS - (now - lastShareTime)) / 1000;
            Log.w(TAG, "⏱️ Rate limit: espera " + remainingSeconds + " segundos");
            if (callback != null) callback.onError("Espera " + remainingSeconds + "s para compartir otra canción");
            return;
        }

        // Crear objeto SharedSong - usar NICKNAME para privacidad
        String userName = getCurrentUserName();

        String userPhotoUrl = null;
        if (user.getPhotoUrl() != null) {
            userPhotoUrl = user.getPhotoUrl().toString();
        }

        // Usar FirebaseQueueManager para batching eficiente
        try {
            FirebaseQueueManager queueManager = FirebaseQueueManager.getInstance(context);
            queueManager.shareSong(
                user.getUid(),
                userName,
                userPhotoUrl,
                songTitle
            );

            lastShareTime = System.currentTimeMillis();
            Log.d(TAG, "✅ Canción encolada para compartir: " + songTitle);
            if (callback != null) callback.onSuccess();

        } catch (Exception e) {
            // Fallback: escritura directa si el queue falla
            Log.w(TAG, "⚠️ Queue no disponible, usando escritura directa");

            Map<String, Object> songData = new HashMap<>();
            songData.put("userId", user.getUid());
            songData.put("userName", userName);
            songData.put("userPhotoUrl", userPhotoUrl);
            songData.put("songTitle", songTitle);
            songData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
            songData.put("likes", 0);

            // Usar userId como ID del documento - así solo hay UNA canción por usuario
            // Si el usuario comparte otra, REEMPLAZA la anterior
            db.collection(COLLECTION_SHARED_SONGS)
                    .document(user.getUid())  // ID fijo = reemplaza
                    .set(songData)
                    .addOnSuccessListener(aVoid -> {
                        lastShareTime = System.currentTimeMillis();
                        Log.d(TAG, "✅ Canción compartida (reemplaza anterior): " + songTitle);
                        if (callback != null) callback.onSuccess();
                    })
                    .addOnFailureListener(ex -> {
                        Log.e(TAG, "❌ Error compartiendo canción: " + ex.getMessage());
                        if (callback != null) callback.onError("Error al compartir: " + ex.getMessage());
                    });
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ESCUCHAR NUEVAS CANCIONES (TIEMPO REAL)
    // ═══════════════════════════════════════════════════════════

    /**
     * 🔔 Inicia el listener de nuevas canciones
     * Notifica cuando otros usuarios comparten canciones
     */
    public void startListening(OnNewSongListener listener) {
        this.newSongListener = listener;

        // Detener listener anterior si existe
        stopListening();

        // Escuchar solo canciones nuevas (últimos 10 segundos para evitar cargar historial)
        long tenSecondsAgo = System.currentTimeMillis() - 10000;

        Log.d(TAG, "🔔🔔🔔 INICIANDO LISTENER FIREBASE 🔔🔔🔔");

        // Guardar tiempo de inicio para filtrar canciones viejas
        final long listenerStartTime = System.currentTimeMillis();

        songsListener = db.collection(COLLECTION_SHARED_SONGS)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)  // Solo la más reciente
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    private String lastSongId = null;
                    private boolean isFirstCallback = true;

                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.e(TAG, "❌ Error escuchando canciones: " + e.getMessage());
                            return;
                        }

                        if (snapshots == null || snapshots.isEmpty()) {
                            return;
                        }

                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                SharedSong song = dc.getDocument().toObject(SharedSong.class);
                                song.setId(dc.getDocument().getId());

                                // IMPORTANTE: Ignorar la primera canción (es histórica)
                                if (isFirstCallback) {
                                    isFirstCallback = false;
                                    lastSongId = song.getId();
                                    Log.d(TAG, "⏭️ Ignorando canción histórica: " + song.getSongTitle());
                                    continue;
                                }

                                // Verificar que es reciente (últimos 2 minutos)
                                if (song.getTimestamp() != null) {
                                    long songTime = song.getTimestamp().getTime();
                                    long twoMinutesAgo = System.currentTimeMillis() - 120000;
                                    if (songTime < twoMinutesAgo) {
                                        Log.d(TAG, "⏭️ Canción muy vieja, ignorando");
                                        continue;
                                    }
                                }

                                // Evitar duplicados
                                if (song.getId().equals(lastSongId)) {
                                    continue;
                                }

                                // No mostrar propias canciones
                                FirebaseUser currentUser = auth.getCurrentUser();
                                if (currentUser != null && song.getUserId() != null
                                        && song.getUserId().equals(currentUser.getUid())) {
                                    continue;
                                }

                                lastSongId = song.getId();
                                Log.d(TAG, "🎵✅ Nueva canción: " + song.getSongTitle());

                                if (newSongListener != null) {
                                    newSongListener.onNewSong(song);
                                }
                            }
                        }
                    }
                });

        Log.d(TAG, "🔔 Listener de canciones iniciado");

        // 🔄 INICIAR POLLING FALLBACK (para dispositivos viejos como Huawei)
        startPollingFallback();
    }

    /**
     * 🔄 Inicia el polling como fallback para dispositivos donde el listener no funciona
     */
    private void startPollingFallback() {
        if (pollingHandler == null) {
            pollingHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        }

        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                pollForNewSongs();
                pollingHandler.postDelayed(this, POLLING_INTERVAL_MS);
            }
        };

        pollingHandler.postDelayed(pollingRunnable, POLLING_INTERVAL_MS);
        Log.d(TAG, "🔄 Polling fallback iniciado (cada " + POLLING_INTERVAL_MS/1000 + "s)");
    }

    /**
     * 🔄 Consulta Firestore para buscar nuevas canciones
     */
    private void pollForNewSongs() {
        if (newSongListener == null) return;

        db.collection(COLLECTION_SHARED_SONGS)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots == null || snapshots.isEmpty()) return;

                    SharedSong song = snapshots.getDocuments().get(0).toObject(SharedSong.class);
                    if (song == null) return;
                    song.setId(snapshots.getDocuments().get(0).getId());

                    // Verificar si es nueva
                    if (song.getId().equals(lastPolledSongId)) {
                        return;  // Ya la vimos
                    }

                    // Filtrar canciones viejas (más de 2 minutos)
                    if (song.getTimestamp() != null) {
                        long twoMinutesAgo = System.currentTimeMillis() - 120000;
                        if (song.getTimestamp().getTime() < twoMinutesAgo) {
                            lastPolledSongId = song.getId();
                            return;  // Muy vieja
                        }
                    }

                    // No mostrar propias canciones
                    FirebaseUser currentUser = auth.getCurrentUser();
                    if (currentUser != null && song.getUserId() != null
                            && song.getUserId().equals(currentUser.getUid())) {
                        lastPolledSongId = song.getId();
                        return;
                    }

                    lastPolledSongId = song.getId();
                    Log.d(TAG, "🔄 Nueva canción: " + song.getSongTitle());

                    if (newSongListener != null) {
                        newSongListener.onNewSong(song);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "🔄 Error en polling: " + e.getMessage());
                });
    }

    /**
     * 🔕 Detiene el listener de canciones
     */
    public void stopListening() {
        if (songsListener != null) {
            songsListener.remove();
            songsListener = null;
            Log.d(TAG, "🔕 Listener de canciones detenido");
        }

        // Detener polling
        if (pollingHandler != null && pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
            Log.d(TAG, "🔄 Polling detenido");
        }
    }

    // ═══════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════

    /**
     * ⏱️ Verifica si el usuario puede compartir (rate limit)
     */
    public boolean canShare() {
        return System.currentTimeMillis() - lastShareTime >= RATE_LIMIT_MS;
    }

    /**
     * ⏱️ Obtiene segundos restantes para poder compartir
     */
    public long getRemainingCooldown() {
        long elapsed = System.currentTimeMillis() - lastShareTime;
        if (elapsed >= RATE_LIMIT_MS) {
            return 0;
        }
        return (RATE_LIMIT_MS - elapsed) / 1000;
    }

    /**
     * 👤 Verifica si el usuario está autenticado
     */
    public boolean isUserLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    /**
     * 👤 Obtiene el NICKNAME del usuario (parte antes del @ del email)
     * Esto protege la privacidad - no muestra el nombre real
     */
    public String getCurrentUserName() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return "Usuario";

        // Prioridad: email prefix (nickname) > displayName > fallback
        String email = user.getEmail();
        if (email != null && email.contains("@")) {
            String nickname = email.substring(0, email.indexOf("@"));
            // Capitalizar primera letra para verse mejor
            if (!nickname.isEmpty()) {
                return nickname.substring(0, 1).toUpperCase() + nickname.substring(1).toLowerCase();
            }
        }

        // Fallback a displayName si no hay email
        if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            // Solo usar primer nombre para privacidad
            String[] parts = user.getDisplayName().split(" ");
            return parts[0];
        }

        return "Usuario";
    }

    // ═══════════════════════════════════════════════════════════
    // CALLBACKS
    // ═══════════════════════════════════════════════════════════

    public interface ShareCallback {
        void onSuccess();
        void onError(String error);
    }
}
