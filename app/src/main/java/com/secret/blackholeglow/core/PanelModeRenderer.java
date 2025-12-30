package com.secret.blackholeglow.core;

import android.content.Context;
import android.opengl.GLES30;
import android.util.Log;

import com.secret.blackholeglow.LoadingBar;
import com.secret.blackholeglow.OrbixGreeting;
import com.secret.blackholeglow.OrbixMascotButton;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                     PanelModeRenderer                            ║
 * ║            Renderizador de UI del Panel de Control               ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  COMPONENTES:                                                    ║
 * ║  • OrbixGreeting: Saludo + reloj + cuenta regresiva              ║
 * ║  • PlayPauseButton: Botón de play central                        ║
 * ║  • LoadingBar: Barra de carga                                    ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class PanelModeRenderer {
    private static final String TAG = "PanelModeRenderer";

    // Componentes UI estándar
    private OrbixMascotButton playPauseButton;
    private OrbixGreeting orbixGreeting;
    private LoadingBar loadingBar;

    // Estado
    private boolean initialized = false;
    private final Context context;
    private boolean greetingEnabled = true;

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
        playPauseButton = new OrbixMascotButton(context);
        playPauseButton.initialize();
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

        initialized = true;
        Log.d(TAG, "✅ Panel de Control inicializado");
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔄 UPDATE
    // ═══════════════════════════════════════════════════════════════

    public void updatePanelMode(float deltaTime) {
        if (orbixGreeting != null) {
            orbixGreeting.update(deltaTime);
        }
        if (playPauseButton != null) {
            playPauseButton.update(deltaTime);
        }
    }

    public void updateLoadingMode(float deltaTime) {
        if (loadingBar != null) {
            loadingBar.update(deltaTime);
        }
        if (orbixGreeting != null) {
            orbixGreeting.update(deltaTime);
        }
    }

    public void updateWallpaperMode(float deltaTime) {
        // Nothing to update in wallpaper mode
    }

    // ═══════════════════════════════════════════════════════════════
    // 🎨 DRAW
    // ═══════════════════════════════════════════════════════════════

    public void drawPanelMode() {
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);

        if (orbixGreeting != null) {
            orbixGreeting.draw();
        }
        if (playPauseButton != null) {
            playPauseButton.draw();
        }

        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
    }

    public void drawLoadingMode() {
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);

        if (orbixGreeting != null) {
            orbixGreeting.draw();
        }
        if (loadingBar != null) {
            loadingBar.draw();
        }

        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
    }

    public void drawWallpaperOverlay() {
        // No overlay elements to draw
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔄 TRANSICIONES
    // ═══════════════════════════════════════════════════════════════

    public void onStartLoading(String sceneName) {
        if (orbixGreeting != null) {
            orbixGreeting.hide();
        }
        if (loadingBar != null) {
            loadingBar.reset();
            loadingBar.setBackgroundForScene(context, sceneName);
            loadingBar.show();
            loadingBar.setProgress(1.0f);
        }
        Log.d(TAG, "📊 Cargando escena: " + sceneName);
    }

    public void onStartLoading() {
        onStartLoading(null);
    }

    public void onWallpaperActivated() {
        Log.d(TAG, "🎬 Wallpaper activado");

        if (orbixGreeting != null) orbixGreeting.hide();
        if (playPauseButton != null) {
            playPauseButton.setPlaying(true);
            playPauseButton.setVisible(false);
        }
    }

    public void onReturnToPanel() {
        greetingEnabled = true;
        if (orbixGreeting != null) {
            orbixGreeting.show();
        }
        if (playPauseButton != null) {
            playPauseButton.setVisible(true);
            playPauseButton.setPlaying(false);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 👆 TOUCH DETECTION
    // ═══════════════════════════════════════════════════════════════

    public boolean isPlayButtonTouched(float nx, float ny) {
        return playPauseButton != null && playPauseButton.isInside(nx, ny);
    }

    // ═══════════════════════════════════════════════════════════════
    // 📐 SCREEN SIZE
    // ═══════════════════════════════════════════════════════════════

    public void setScreenSize(int width, int height) {
        float aspectRatio = (float) width / height;

        if (playPauseButton != null) {
            playPauseButton.setAspectRatio(aspectRatio);
            playPauseButton.setSize(0.18f);
            playPauseButton.setPosition(0.0f, 0.0f);
        }
        if (orbixGreeting != null) {
            orbixGreeting.setAspectRatio(aspectRatio);
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

    public OrbixMascotButton getPlayPauseButton() { return playPauseButton; }
    public OrbixGreeting getOrbixGreeting() { return orbixGreeting; }
    public LoadingBar getLoadingBar() { return loadingBar; }

    public void setGreetingEnabled(boolean enabled) {
        this.greetingEnabled = enabled;
        if (orbixGreeting != null) {
            if (enabled) {
                orbixGreeting.show();
            } else {
                orbixGreeting.hide();
            }
        }
        Log.d(TAG, "🤖 Greeting " + (enabled ? "habilitado" : "deshabilitado"));
    }

    // Métodos stub para compatibilidad (ya no hacen nada)
    public void setArcadeModeEnabled(boolean enabled) {
        // Arcade mode removed
    }

    public boolean isArcadeModeEnabled() {
        return false;
    }

    public void setChristmasModeEnabled(boolean enabled) {
        // Christmas mode removed
    }

    public boolean isChristmasModeEnabled() {
        return false;
    }

    public void onTouchDown(float nx, float ny) {
        // Touch sparkles removed
    }

    public void onTouchMove(float nx, float ny) {
        // Touch sparkles removed
    }

    public void onTouchUp() {
        // Touch sparkles removed
    }

    public void updateMusicBands(float[] bands) {
        // Equalizer removed
    }

    public void triggerGiftPhotoReveal() {
        // Gift photo reveal removed
    }

    public boolean hasGalleryPermission() {
        return false;
    }

    public String getGalleryPermission() {
        return null;
    }

    public void release() {
        if (orbixGreeting != null) {
            orbixGreeting.dispose();
            orbixGreeting = null;
        }

        Log.d(TAG, "🧹 PanelModeRenderer recursos liberados");
    }
}
