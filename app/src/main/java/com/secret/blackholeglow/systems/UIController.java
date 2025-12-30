package com.secret.blackholeglow.systems;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

import com.secret.blackholeglow.PlayPauseButton;
import com.secret.blackholeglow.sharing.HeartParticleSystem;
import com.secret.blackholeglow.sharing.LikeButton;
import com.secret.blackholeglow.sharing.SharedSong;
import com.secret.blackholeglow.sharing.SongSharingManager;
import com.secret.blackholeglow.sharing.UserAvatar;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                        UIController                               ║
 * ║                    "Director de UI"                               ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  Coordina TODOS los elementos de interfaz de usuario.            ║
 * ║                                                                  ║
 * ║  NOTA: Los componentes UI son INYECTADOS, no creados aquí.       ║
 * ║  Esto permite que SceneRenderer siga funcionando mientras        ║
 * ║  migramos gradualmente al nuevo sistema.                         ║
 * ║                                                                  ║
 * ║  RESPONSABILIDADES:                                              ║
 * ║  • Coordinar componentes UI                                      ║
 * ║  • Manejar touch en UI                                           ║
 * ║  • Publicar eventos via EventBus                                 ║
 * ║                                                                  ║
 * ║  NO HACE:                                                        ║
 * ║  ✗ Crear componentes (son inyectados)                            ║
 * ║  ✗ Renderizar escena 3D                                          ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class UIController {
    private static final String TAG = "UIController";

    // ═══════════════════════════════════════════════════════════════
    // 📦 SINGLETON
    // ═══════════════════════════════════════════════════════════════

    private static UIController instance;

    // ═══════════════════════════════════════════════════════════════
    // 🎮 COMPONENTES UI (INYECTADOS)
    // ═══════════════════════════════════════════════════════════════

    private LikeButton likeButton;
    private PlayPauseButton playPauseButton;
    private HeartParticleSystem heartParticles;
    private UserAvatar userAvatar;
    private SongSharingManager songSharingManager;

    // ═══════════════════════════════════════════════════════════════
    // 📐 CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════

    private int screenWidth = 1;
    private int screenHeight = 1;
    private boolean isPreviewMode = false;

    // ═══════════════════════════════════════════════════════════════
    // 🔧 SINGLETON
    // ═══════════════════════════════════════════════════════════════

    private UIController() {
        Log.d(TAG, "🎮 UIController creado");
    }

    public static UIController get() {
        if (instance == null) {
            instance = new UIController();
        }
        return instance;
    }

    // ═══════════════════════════════════════════════════════════════
    // 💉 INYECCIÓN DE COMPONENTES
    // ═══════════════════════════════════════════════════════════════

    public void setLikeButton(LikeButton button) {
        this.likeButton = button;
    }

    public void setPlayPauseButton(PlayPauseButton button) {
        this.playPauseButton = button;
    }

    public void setHeartParticles(HeartParticleSystem particles) {
        this.heartParticles = particles;
    }

    public void setUserAvatar(UserAvatar avatar) {
        this.userAvatar = avatar;
    }

    public void setSongSharingManager(SongSharingManager manager) {
        this.songSharingManager = manager;
    }

    // ═══════════════════════════════════════════════════════════════
    // 📐 CONFIGURACIÓN DE PANTALLA
    // ═══════════════════════════════════════════════════════════════

    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }

    public void setPreviewMode(boolean preview) {
        this.isPreviewMode = preview;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    // ═══════════════════════════════════════════════════════════════
    // 👆 TOUCH HANDLING
    // ═══════════════════════════════════════════════════════════════

    /**
     * Manejar evento de touch
     * @return true si el touch fue consumido por la UI
     */
    public boolean handleTouch(MotionEvent event) {
        if (isPreviewMode) return false;

        float touchX = event.getX();
        float touchY = event.getY();

        // Normalizar coordenadas a rango -1 a 1
        float normalizedX = (touchX / screenWidth) * 2.0f - 1.0f;
        float normalizedY = -((touchY / screenHeight) * 2.0f - 1.0f);

        int action = event.getAction();

        // Verificar LikeButton
        if (likeButton != null && likeButton.isTouched(normalizedX, normalizedY)) {
            if (action == MotionEvent.ACTION_DOWN) {
                likeButton.onPress();
                return true;
            } else if (action == MotionEvent.ACTION_UP) {
                likeButton.onRelease();
                handleLikePressed(normalizedX, normalizedY);
                return true;
            }
        }

        return false;
    }

    /**
     * Maneja el touch del LikeButton y genera efectos
     */
    private void handleLikePressed(float x, float y) {
        // Generar partículas de corazones
        if (heartParticles != null && likeButton != null) {
            heartParticles.emit(likeButton.getX(), likeButton.getY(), 15);
        }

        // Publicar evento
        EventBus.get().publish(EventBus.LIKE_PRESSED,
            new EventBus.EventData()
                .put("x", x)
                .put("y", y));

        Log.d(TAG, "♥ Like button pressed!");

        // Compartir canción - el manager obtiene la canción actual
        // Por ahora solo emitimos el evento, la lógica de compartir
        // se maneja en otro lugar
        Log.d(TAG, "♥ Like event emitted via EventBus");
    }

    // ═══════════════════════════════════════════════════════════════
    // 📦 GETTERS
    // ═══════════════════════════════════════════════════════════════

    public LikeButton getLikeButton() {
        return likeButton;
    }

    public PlayPauseButton getPlayPauseButton() {
        return playPauseButton;
    }

    public HeartParticleSystem getHeartParticles() {
        return heartParticles;
    }

    public UserAvatar getUserAvatar() {
        return userAvatar;
    }

    public SongSharingManager getSongSharingManager() {
        return songSharingManager;
    }

    // ═══════════════════════════════════════════════════════════════
    // 🎵 NOTIFICACIONES DE CANCIONES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Notificar que hay una nueva canción
     */
    public void onNewSong(SharedSong song) {
        EventBus.get().publish(EventBus.SONG_CHANGED,
            new EventBus.EventData()
                .put("title", song.getSongTitle())
                .put("artist", song.getSongArtist())
                .put("userId", song.getUserId())
                .put("song", song));

        Log.d(TAG, "🎵 Nueva canción: " + song.getSongTitle());
    }

    // ═══════════════════════════════════════════════════════════════
    // 🗑️ LIMPIEZA
    // ═══════════════════════════════════════════════════════════════

    /**
     * Limpiar referencias (no liberar - lo hace SceneRenderer aún)
     */
    public void clear() {
        likeButton = null;
        playPauseButton = null;
        heartParticles = null;
        userAvatar = null;
        songSharingManager = null;
        Log.d(TAG, "🧹 UIController referencias limpiadas");
    }

    /**
     * Reset singleton (para tests o recreación)
     */
    public static void reset() {
        if (instance != null) {
            instance.clear();
            instance = null;
        }
    }
}
