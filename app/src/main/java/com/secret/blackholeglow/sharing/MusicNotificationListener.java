package com.secret.blackholeglow.sharing;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.List;

/**
 * 🎵 LISTENER DE MÚSICA EN REPRODUCCIÓN
 *
 * Captura la canción que está reproduciendo el usuario
 * desde Spotify, YouTube Music, Samsung Music, etc.
 */
public class MusicNotificationListener extends NotificationListenerService {
    private static final String TAG = "MusicListener";

    private static MusicNotificationListener instance;
    private static String currentSongTitle = "";
    private static String currentArtist = "";
    private static boolean isPlaying = false;

    private MediaSessionManager mediaSessionManager;
    private MediaSessionManager.OnActiveSessionsChangedListener sessionsListener;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.d(TAG, "🎵 MusicNotificationListener creado");

        // Inicializar MediaSessionManager
        try {
            mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
            if (mediaSessionManager != null) {
                sessionsListener = sessions -> updateCurrentSong();
                mediaSessionManager.addOnActiveSessionsChangedListener(
                    sessionsListener,
                    new ComponentName(this, MusicNotificationListener.class)
                );
                updateCurrentSong();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "❌ Sin permiso para MediaSession: " + e.getMessage());
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // Actualizar cuando llega notificación de música
        updateCurrentSong();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Puede que la música se detuvo
        updateCurrentSong();
    }

    /**
     * 🔄 Actualiza la canción actual desde MediaSession
     */
    private void updateCurrentSong() {
        if (mediaSessionManager == null) return;

        try {
            List<MediaController> controllers = mediaSessionManager.getActiveSessions(
                new ComponentName(this, MusicNotificationListener.class)
            );

            for (MediaController controller : controllers) {
                PlaybackState state = controller.getPlaybackState();
                MediaMetadata metadata = controller.getMetadata();

                if (state != null && metadata != null) {
                    boolean playing = state.getState() == PlaybackState.STATE_PLAYING;

                    if (playing) {
                        String title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
                        String artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);

                        if (title != null && !title.isEmpty()) {
                            currentSongTitle = title;
                            currentArtist = artist != null ? artist : "";
                            isPlaying = true;

                            Log.d(TAG, "🎵 Reproduciendo: " + currentArtist + " - " + currentSongTitle);
                            return;
                        }
                    }
                }
            }

            // No hay música reproduciéndose
            isPlaying = false;

        } catch (SecurityException e) {
            Log.e(TAG, "❌ Error accediendo a MediaSession: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // MÉTODOS ESTÁTICOS PARA ACCEDER DESDE OTRAS CLASES
    // ═══════════════════════════════════════════════════════════

    /**
     * 🎵 Obtiene el título de la canción actual
     */
    public static String getCurrentSongTitle() {
        return currentSongTitle;
    }

    /**
     * 🎤 Obtiene el artista actual
     */
    public static String getCurrentArtist() {
        return currentArtist;
    }

    /**
     * 🎵 Obtiene título formateado: "Artista - Canción" o solo "Canción"
     */
    public static String getFormattedSong() {
        if (currentSongTitle.isEmpty()) {
            return "Sin música";
        }
        if (currentArtist.isEmpty()) {
            return currentSongTitle;
        }
        return currentArtist + " - " + currentSongTitle;
    }

    /**
     * ▶️ Verifica si hay música reproduciéndose
     */
    public static boolean isMusicPlaying() {
        return isPlaying && !currentSongTitle.isEmpty();
    }

    /**
     * 🔌 Verifica si el servicio está activo
     */
    public static boolean isServiceRunning() {
        return instance != null;
    }

    @Override
    public void onDestroy() {
        if (mediaSessionManager != null && sessionsListener != null) {
            try {
                mediaSessionManager.removeOnActiveSessionsChangedListener(sessionsListener);
            } catch (Exception e) {
                Log.w(TAG, "⚠️ Error desregistrando listener: " + e.getMessage());
            }
            sessionsListener = null;
        }
        instance = null;
        Log.d(TAG, "🔴 MusicNotificationListener destruido");
        super.onDestroy();
    }
}
