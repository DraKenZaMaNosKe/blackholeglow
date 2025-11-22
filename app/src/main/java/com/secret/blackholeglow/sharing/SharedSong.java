package com.secret.blackholeglow.sharing;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

/**
 * 🎵 MODELO DE CANCIÓN COMPARTIDA
 *
 * Representa una canción que un usuario comparte con la comunidad.
 * Se almacena en Firebase Firestore y se sincroniza en tiempo real.
 */
public class SharedSong {

    // ID del documento en Firebase
    private String id;

    // Datos del usuario que comparte
    private String userId;
    private String userName;
    private String userPhotoUrl;

    // Datos de la canción
    private String songTitle;
    private String songArtist;  // Opcional (para FASE 2)

    // Timestamp del servidor
    @ServerTimestamp
    private Date timestamp;

    // Contador de likes (para futuro)
    private int likes;

    // Constructor vacío requerido por Firebase
    public SharedSong() {}

    // Constructor completo
    public SharedSong(String userId, String userName, String userPhotoUrl,
                      String songTitle, String songArtist) {
        this.userId = userId;
        this.userName = userName;
        this.userPhotoUrl = userPhotoUrl;
        this.songTitle = songTitle;
        this.songArtist = songArtist;
        this.likes = 0;
    }

    // Constructor simplificado (sin artista)
    public SharedSong(String userId, String userName, String userPhotoUrl, String songTitle) {
        this(userId, userName, userPhotoUrl, songTitle, null);
    }

    // ═══════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserPhotoUrl() {
        return userPhotoUrl;
    }

    public String getSongTitle() {
        return songTitle;
    }

    public String getSongArtist() {
        return songArtist;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public int getLikes() {
        return likes;
    }

    // ═══════════════════════════════════════════════════════════
    // SETTERS
    // ═══════════════════════════════════════════════════════════

    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setUserPhotoUrl(String userPhotoUrl) {
        this.userPhotoUrl = userPhotoUrl;
    }

    public void setSongTitle(String songTitle) {
        this.songTitle = songTitle;
    }

    public void setSongArtist(String songArtist) {
        this.songArtist = songArtist;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    // ═══════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════

    /**
     * Retorna el texto formateado para mostrar en la notificación
     * Ejemplo: "🎵 Roar - Katy Perry" o "🎵 Roar"
     */
    public String getDisplayText() {
        if (songArtist != null && !songArtist.isEmpty()) {
            return songTitle + " - " + songArtist;
        }
        return songTitle;
    }

    @Override
    public String toString() {
        return "SharedSong{" +
                "userName='" + userName + '\'' +
                ", songTitle='" + songTitle + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
