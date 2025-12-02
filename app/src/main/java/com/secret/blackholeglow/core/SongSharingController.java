package com.secret.blackholeglow.core;

import android.content.Context;
import android.util.Log;

import com.secret.blackholeglow.GeminiService;
import com.secret.blackholeglow.SongMessageRenderer;
import com.secret.blackholeglow.sharing.HeartParticleSystem;
import com.secret.blackholeglow.sharing.LikeButton;
import com.secret.blackholeglow.sharing.MusicNotificationListener;
import com.secret.blackholeglow.sharing.SongSharingManager;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                    SongSharingController                         ║
 * ║          Controlador de Compartir Canciones con IA               ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  RESPONSABILIDADES:                                              ║
 * ║  • Gestionar el botón de Like (corazón)                          ║
 * ║  • Emitir partículas de corazones                                ║
 * ║  • Generar mensajes creativos con Gemini AI                      ║
 * ║  • Mostrar mensajes mágicos en pantalla                          ║
 * ║  • Compartir canciones en Firebase                               ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class SongSharingController {
    private static final String TAG = "SongSharingCtrl";

    // Componentes
    private LikeButton likeButton;
    private HeartParticleSystem heartParticles;
    private SongSharingManager songSharingManager;
    private SongMessageRenderer songMessageRenderer;

    // Estado
    private boolean initialized = false;
    private final Context context;

    public SongSharingController(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Inicializa todos los componentes del sistema de compartir
     */
    public void initialize() {
        if (initialized) return;

        try {
            // Botón de Like (corazón neón - micro)
            likeButton = new LikeButton();
            likeButton.init();
            likeButton.setPosition(0.85f, -0.50f);
            likeButton.setSize(0.04f);  // Tamaño mini
            Log.d(TAG, "♥ LikeButton inicializado");

            // Sistema de partículas de corazones
            heartParticles = new HeartParticleSystem();
            heartParticles.init();
            Log.d(TAG, "💖 HeartParticleSystem inicializado");

            // Manager para Firebase
            songSharingManager = SongSharingManager.getInstance(context);
            Log.d(TAG, "🎵 SongSharingManager inicializado");

            // Renderizador mágico de mensajes
            songMessageRenderer = new SongMessageRenderer(context);
            Log.d(TAG, "✨ SongMessageRenderer inicializado");

            initialized = true;
            Log.d(TAG, "✅ SongSharingController completamente inicializado");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error inicializando: " + e.getMessage());
        }
    }

    /**
     * Actualiza componentes (llamar cada frame)
     */
    public void update(float deltaTime) {
        if (!initialized) return;

        // Actualizar partículas de corazones
        if (heartParticles != null) {
            heartParticles.update(deltaTime);
        }

        // Actualizar mensaje de canción
        if (songMessageRenderer != null) {
            songMessageRenderer.update(deltaTime);
        }

        // Actualizar estado de cooldown del botón
        if (likeButton != null && songSharingManager != null) {
            likeButton.setCooldown(!songSharingManager.canShare());
        }
    }

    /**
     * Dibuja todos los componentes visuales
     */
    public void draw(float[] mvpMatrix, float time) {
        if (!initialized) return;

        // Dibujar botón de like
        if (likeButton != null) {
            likeButton.draw(mvpMatrix, time);
        }

        // Dibujar partículas de corazones
        if (heartParticles != null) {
            heartParticles.draw(mvpMatrix);
        }

        // Dibujar mensaje mágico
        if (songMessageRenderer != null && songMessageRenderer.isVisible()) {
            songMessageRenderer.draw();
        }
    }

    /**
     * Verifica si el toque está en el botón de like
     */
    public boolean isTouchOnLikeButton(float nx, float ny) {
        return likeButton != null && likeButton.isTouched(nx, ny);
    }

    /**
     * Maneja el evento de presionar el botón
     */
    public void onLikeButtonPressed() {
        if (likeButton != null) {
            likeButton.onPress();
        }
    }

    /**
     * Maneja el evento de soltar el botón
     */
    public void onLikeButtonReleased() {
        if (likeButton != null) {
            likeButton.onRelease();
        }
    }

    /**
     * Ejecuta la acción de compartir canción con IA
     */
    public void shareSongWithAI() {
        if (!initialized || songSharingManager == null) {
            Log.e(TAG, "❌ No inicializado");
            return;
        }

        if (!songSharingManager.canShare()) {
            Log.d(TAG, "⏳ En cooldown, no se puede compartir");
            return;
        }

        // Obtener canción actual
        String songTitle = MusicNotificationListener.getCurrentSongTitle();
        String artist = MusicNotificationListener.getCurrentArtist();
        String fullSong = MusicNotificationListener.getFormattedSong();

        if (songTitle == null || songTitle.isEmpty()) {
            songTitle = "esta canción";
            fullSong = "música increíble";
        }

        Log.d(TAG, "♥ Canción detectada: " + fullSong);

        // Obtener nombre del usuario
        String userName = songSharingManager.getCurrentUserName();

        // Emitir partículas inmediatamente
        emitHeartParticles();

        // Generar mensaje con IA
        final String finalSongTitle = fullSong;
        final String finalUserName = userName;

        GeminiService.getInstance().generateSongComment(fullSong, userName,
            new GeminiService.GeminiCallback() {
                @Override
                public void onResponse(String response) {
                    showMessage(response);
                    shareToFirebase(finalUserName + ": " + response);
                }

                @Override
                public void onError(String error) {
                    String fallback = "♥ A " + finalUserName + " le encanta:\n🎵 " + finalSongTitle;
                    showMessage(fallback);
                    shareToFirebase(fallback);
                }
            });
    }

    /**
     * Emite partículas de corazones desde el botón
     */
    private void emitHeartParticles() {
        if (likeButton != null && heartParticles != null) {
            heartParticles.emit(likeButton.getX(), likeButton.getY(), 15);
        }
    }

    /**
     * Muestra un mensaje mágico en pantalla
     */
    private void showMessage(String message) {
        if (songMessageRenderer != null) {
            songMessageRenderer.showMessage(message);
        }
        Log.d(TAG, "🎵 Mensaje: " + message);
    }

    /**
     * Comparte el mensaje en Firebase
     */
    private void shareToFirebase(String message) {
        if (songSharingManager == null) return;

        songSharingManager.shareSong(message, new SongSharingManager.ShareCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "✅ Compartido exitosamente");
                if (likeButton != null) {
                    likeButton.setCooldown(true);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error: " + error);
            }
        });
    }

    /**
     * ¿Hay un mensaje visible actualmente?
     */
    public boolean isMessageVisible() {
        return songMessageRenderer != null && songMessageRenderer.isVisible();
    }

    /**
     * Libera recursos
     */
    public void release() {
        if (songMessageRenderer != null) {
            songMessageRenderer.cleanup();
            songMessageRenderer = null;
        }
        likeButton = null;
        heartParticles = null;
        songSharingManager = null;
        initialized = false;
        Log.d(TAG, "🗑️ SongSharingController liberado");
    }

    // Getters para acceso directo si es necesario
    public LikeButton getLikeButton() { return likeButton; }
    public HeartParticleSystem getHeartParticles() { return heartParticles; }
    public boolean isInitialized() { return initialized; }
}
