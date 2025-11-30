package com.secret.blackholeglow.core;

import android.util.Log;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                    RenderModeController                          ║
 * ║           Controlador de Modos de Renderizado                    ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  MODOS:                                                          ║
 * ║  • PANEL_MODE: Panel de control (Play button + Greeting)         ║
 * ║  • LOADING_MODE: Cargando recursos (barra de progreso)           ║
 * ║  • WALLPAPER_MODE: Escena 3D activa                              ║
 * ║                                                                  ║
 * ║  RESPONSABILIDADES:                                              ║
 * ║  • Gestionar el modo actual                                      ║
 * ║  • Validar transiciones entre modos                              ║
 * ║  • Notificar cambios de modo via listener                        ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class RenderModeController {
    private static final String TAG = "RenderModeCtrl";

    // ═══════════════════════════════════════════════════════════════
    // 🎮 MODOS DE RENDERIZADO
    // ═══════════════════════════════════════════════════════════════

    public enum RenderMode {
        PANEL_MODE,      // Panel de Control (fondo negro + greeting + play button)
        LOADING_MODE,    // Cargando recursos (barra de progreso)
        WALLPAPER_MODE   // Escena 3D completa
    }

    // ═══════════════════════════════════════════════════════════════
    // 📢 LISTENER
    // ═══════════════════════════════════════════════════════════════

    public interface ModeChangeListener {
        void onModeChanged(RenderMode oldMode, RenderMode newMode);
        void onLoadingStarted();
        void onLoadingCompleted();
        void onWallpaperStopped();
    }

    // ═══════════════════════════════════════════════════════════════
    // 📦 ESTADO
    // ═══════════════════════════════════════════════════════════════

    private RenderMode currentMode = RenderMode.PANEL_MODE;
    private ModeChangeListener listener;
    private boolean isPreviewMode = false;

    public RenderModeController() {
        Log.d(TAG, "🎮 RenderModeController inicializado");
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔄 TRANSICIONES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Inicia la carga del wallpaper (Play presionado)
     * PANEL_MODE → LOADING_MODE
     */
    public boolean startLoading() {
        if (currentMode == RenderMode.LOADING_MODE) {
            Log.d(TAG, "⚠️ Ya en LOADING_MODE");
            return false;
        }
        if (currentMode == RenderMode.WALLPAPER_MODE) {
            Log.d(TAG, "⚠️ Ya en WALLPAPER_MODE");
            return false;
        }

        RenderMode oldMode = currentMode;
        currentMode = RenderMode.LOADING_MODE;

        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║   ▶️ TRANSICIÓN: PANEL → LOADING        ║");
        Log.d(TAG, "╚════════════════════════════════════════╝");

        notifyModeChanged(oldMode, currentMode);
        if (listener != null) {
            listener.onLoadingStarted();
        }

        return true;
    }

    /**
     * Carga completada, activar escena
     * LOADING_MODE → WALLPAPER_MODE
     */
    public boolean activateWallpaper() {
        if (currentMode == RenderMode.WALLPAPER_MODE) {
            Log.d(TAG, "⚠️ Ya en WALLPAPER_MODE");
            return false;
        }

        RenderMode oldMode = currentMode;
        currentMode = RenderMode.WALLPAPER_MODE;

        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║   ✅ TRANSICIÓN: LOADING → WALLPAPER    ║");
        Log.d(TAG, "╚════════════════════════════════════════╝");

        notifyModeChanged(oldMode, currentMode);
        if (listener != null) {
            listener.onLoadingCompleted();
        }

        return true;
    }

    /**
     * Volver al Panel de Control (Stop presionado)
     * WALLPAPER_MODE → PANEL_MODE
     */
    public boolean stopWallpaper() {
        if (currentMode == RenderMode.PANEL_MODE) {
            Log.d(TAG, "⚠️ Ya en PANEL_MODE");
            return false;
        }

        RenderMode oldMode = currentMode;
        currentMode = RenderMode.PANEL_MODE;

        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║   ⏹️ TRANSICIÓN: WALLPAPER → PANEL      ║");
        Log.d(TAG, "╚════════════════════════════════════════╝");

        notifyModeChanged(oldMode, currentMode);
        if (listener != null) {
            listener.onWallpaperStopped();
        }

        return true;
    }

    /**
     * Ir directamente a WALLPAPER_MODE (para preview)
     */
    public void goDirectToWallpaper() {
        RenderMode oldMode = currentMode;
        currentMode = RenderMode.WALLPAPER_MODE;
        Log.d(TAG, "🖼️ Directo a WALLPAPER_MODE (preview)");
        notifyModeChanged(oldMode, currentMode);
    }

    // ═══════════════════════════════════════════════════════════════
    // 📢 NOTIFICACIONES
    // ═══════════════════════════════════════════════════════════════

    private void notifyModeChanged(RenderMode oldMode, RenderMode newMode) {
        if (listener != null) {
            listener.onModeChanged(oldMode, newMode);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔧 GETTERS/SETTERS
    // ═══════════════════════════════════════════════════════════════

    public RenderMode getCurrentMode() {
        return currentMode;
    }

    public boolean isPanelMode() {
        return currentMode == RenderMode.PANEL_MODE;
    }

    public boolean isLoadingMode() {
        return currentMode == RenderMode.LOADING_MODE;
    }

    public boolean isWallpaperMode() {
        return currentMode == RenderMode.WALLPAPER_MODE;
    }

    public boolean isPlaying() {
        return currentMode == RenderMode.WALLPAPER_MODE;
    }

    public void setListener(ModeChangeListener listener) {
        this.listener = listener;
    }

    public void setPreviewMode(boolean preview) {
        this.isPreviewMode = preview;
    }

    public boolean isPreviewMode() {
        return isPreviewMode;
    }
}
