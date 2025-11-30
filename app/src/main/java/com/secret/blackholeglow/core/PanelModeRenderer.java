package com.secret.blackholeglow.core;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import com.secret.blackholeglow.LoadingBar;
import com.secret.blackholeglow.MiniStopButton;
import com.secret.blackholeglow.OrbixGreeting;
import com.secret.blackholeglow.PlayPauseButton;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                     PanelModeRenderer                            ║
 * ║            Renderizador de UI del Panel de Control               ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  COMPONENTES:                                                    ║
 * ║  • OrbixGreeting: Saludo + reloj + cuenta regresiva              ║
 * ║  • PlayPauseButton: Botón de play central                        ║
 * ║  • LoadingBar: Barra de carga                                    ║
 * ║  • MiniStopButton: Botón stop pequeño (modo wallpaper)           ║
 * ║                                                                  ║
 * ║  RESPONSABILIDADES:                                              ║
 * ║  • Inicializar componentes de UI                                 ║
 * ║  • Actualizar y dibujar según el modo                            ║
 * ║  • Gestionar visibilidad de componentes                          ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class PanelModeRenderer {
    private static final String TAG = "PanelModeRenderer";

    // Componentes UI
    private PlayPauseButton playPauseButton;
    private OrbixGreeting orbixGreeting;
    private LoadingBar loadingBar;
    private MiniStopButton miniStopButton;

    // Estado
    private boolean initialized = false;
    private final Context context;

    // Listener para eventos de carga
    public interface LoadingCompleteListener {
        void onLoadingComplete();
    }
    private LoadingCompleteListener loadingListener;

    public PanelModeRenderer(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Inicializa todos los componentes de UI
     */
    public void initialize() {
        if (initialized) return;

        Log.d(TAG, "🎛️ Inicializando Panel de Control...");

        // PlayPauseButton
        playPauseButton = new PlayPauseButton();
        playPauseButton.setPlaying(false);
        Log.d(TAG, "▶️ PlayPauseButton inicializado");

        // OrbixGreeting
        orbixGreeting = new OrbixGreeting(context);
        orbixGreeting.show();
        Log.d(TAG, "🤖 OrbixGreeting inicializado");

        // LoadingBar
        loadingBar = new LoadingBar();
        loadingBar.setOnLoadingCompleteListener(() -> {
            if (loadingListener != null) {
                loadingListener.onLoadingComplete();
            }
        });
        Log.d(TAG, "📊 LoadingBar inicializado");

        // MiniStopButton
        miniStopButton = new MiniStopButton();
        miniStopButton.hide();
        Log.d(TAG, "⏹️ MiniStopButton inicializado");

        initialized = true;
        Log.d(TAG, "✅ Panel de Control inicializado");
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔄 UPDATE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Actualiza componentes para PANEL_MODE
     */
    public void updatePanelMode(float deltaTime) {
        if (orbixGreeting != null) {
            orbixGreeting.update(deltaTime);
        }
        if (playPauseButton != null) {
            playPauseButton.update(deltaTime);
        }
    }

    /**
     * Actualiza componentes para LOADING_MODE
     */
    public void updateLoadingMode(float deltaTime) {
        if (loadingBar != null) {
            loadingBar.update(deltaTime);
        }
        if (orbixGreeting != null) {
            orbixGreeting.update(deltaTime);
        }
    }

    /**
     * Actualiza componentes para WALLPAPER_MODE
     */
    public void updateWallpaperMode(float deltaTime) {
        if (miniStopButton != null) {
            miniStopButton.update(deltaTime);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 🎨 DRAW
    // ═══════════════════════════════════════════════════════════════

    /**
     * Dibuja UI para PANEL_MODE
     */
    public void drawPanelMode() {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        if (orbixGreeting != null) {
            orbixGreeting.draw();
        }
        if (playPauseButton != null) {
            playPauseButton.draw();
        }

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    /**
     * Dibuja UI para LOADING_MODE
     */
    public void drawLoadingMode() {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        if (orbixGreeting != null) {
            orbixGreeting.draw();
        }
        if (loadingBar != null) {
            loadingBar.draw();
        }

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    /**
     * Dibuja MiniStopButton (overlay sobre wallpaper)
     */
    public void drawWallpaperOverlay() {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        if (miniStopButton != null && miniStopButton.isVisible()) {
            miniStopButton.draw();
        }

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔄 TRANSICIONES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Prepara UI para modo de carga
     */
    public void onStartLoading() {
        if (orbixGreeting != null) {
            orbixGreeting.hide();
        }
        if (loadingBar != null) {
            loadingBar.reset();
            loadingBar.show();
            loadingBar.setProgress(1.0f);
        }
    }

    /**
     * Prepara UI para modo wallpaper
     */
    public void onWallpaperActivated() {
        if (miniStopButton != null) {
            miniStopButton.show();
        }
        if (playPauseButton != null) {
            playPauseButton.setPlaying(true);
        }
    }

    /**
     * Prepara UI para volver al panel
     */
    public void onReturnToPanel() {
        if (miniStopButton != null) {
            miniStopButton.hide();
        }
        if (orbixGreeting != null) {
            orbixGreeting.show();
        }
        if (playPauseButton != null) {
            playPauseButton.setPlaying(false);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 👆 TOUCH DETECTION
    // ═══════════════════════════════════════════════════════════════

    /**
     * ¿Toque en PlayPauseButton?
     */
    public boolean isPlayButtonTouched(float nx, float ny) {
        return playPauseButton != null && playPauseButton.isInside(nx, ny);
    }

    /**
     * ¿Toque en MiniStopButton?
     */
    public boolean isStopButtonTouched(float nx, float ny) {
        return miniStopButton != null && miniStopButton.isVisible() && miniStopButton.isInside(nx, ny);
    }

    // ═══════════════════════════════════════════════════════════════
    // 📐 SCREEN SIZE
    // ═══════════════════════════════════════════════════════════════

    public void setScreenSize(int width, int height) {
        float aspectRatio = (float) width / height;

        if (playPauseButton != null) {
            playPauseButton.setAspectRatio(aspectRatio);
        }
        if (orbixGreeting != null) {
            orbixGreeting.setAspectRatio(aspectRatio);
        }
        if (miniStopButton != null) {
            miniStopButton.setAspectRatio(aspectRatio);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔧 UTILIDADES
    // ═══════════════════════════════════════════════════════════════

    public boolean isLoadingComplete() {
        return loadingBar != null && loadingBar.isComplete();
    }

    public void setLoadingListener(LoadingCompleteListener listener) {
        this.loadingListener = listener;
    }

    public boolean isInitialized() {
        return initialized;
    }

    // Getters para acceso directo si es necesario
    public PlayPauseButton getPlayPauseButton() { return playPauseButton; }
    public OrbixGreeting getOrbixGreeting() { return orbixGreeting; }
    public LoadingBar getLoadingBar() { return loadingBar; }
    public MiniStopButton getMiniStopButton() { return miniStopButton; }
}
