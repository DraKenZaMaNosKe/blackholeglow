package com.secret.blackholeglow.core;

import android.util.Log;
import android.view.MotionEvent;

import com.secret.blackholeglow.scenes.WallpaperScene;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                        TouchRouter                               ║
 * ║              Enrutador de Eventos de Toque                       ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  RESPONSABILIDADES:                                              ║
 * ║  • Normalizar coordenadas de pantalla a NDC (-1 a 1)             ║
 * ║  • Detectar qué componente fue tocado                            ║
 * ║  • Delegar eventos al componente correcto                        ║
 * ║  • Gestionar estado de toque (presionado/liberado)               ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class TouchRouter {
    private static final String TAG = "TouchRouter";

    // ═══════════════════════════════════════════════════════════════
    // 📢 LISTENER
    // ═══════════════════════════════════════════════════════════════

    public interface TouchListener {
        void onPlayButtonTapped();
        void onBackButtonTapped();
        void onLikeButtonPressed();
        void onLikeButtonReleased();
        void onLikeButtonTapped();
        boolean onSceneTouched(float nx, float ny, int action);
    }

    // ═══════════════════════════════════════════════════════════════
    // 📦 ESTADO
    // ═══════════════════════════════════════════════════════════════

    private int screenWidth = 1;
    private int screenHeight = 1;
    private TouchListener listener;

    // Referencias a detectores de componentes
    private PanelModeRenderer panelRenderer;
    private SongSharingController songSharing;
    private RenderModeController modeController;
    private WallpaperScene currentScene;

    // Estado de toque
    private boolean likeButtonPressed = false;

    public TouchRouter() {
        Log.d(TAG, "👆 TouchRouter inicializado");
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔧 CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════

    public void setScreenSize(int width, int height) {
        this.screenWidth = Math.max(1, width);
        this.screenHeight = Math.max(1, height);
    }

    public void setPanelRenderer(PanelModeRenderer renderer) {
        this.panelRenderer = renderer;
    }

    public void setSongSharing(SongSharingController controller) {
        this.songSharing = controller;
    }

    public void setModeController(RenderModeController controller) {
        this.modeController = controller;
    }

    public void setCurrentScene(WallpaperScene scene) {
        this.currentScene = scene;
    }

    public void setListener(TouchListener listener) {
        this.listener = listener;
    }

    // ═══════════════════════════════════════════════════════════════
    // 👆 PROCESAMIENTO DE TOQUES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Procesa un evento de toque y lo distribuye al componente correcto
     * @return true si el evento fue consumido
     */
    public boolean onTouchEvent(MotionEvent event) {
        if (modeController == null) return false;

        // Guardar el evento para escenas que necesitan multi-touch
        this.lastMotionEvent = event;

        // Normalizar coordenadas a NDC (-1 a 1)
        float nx = normalizeX(event.getX());
        float ny = normalizeY(event.getY());
        int action = event.getActionMasked();  // Usar getActionMasked para multi-touch

        // 📍 LOG DE TOUCH PARA DEBUG - Coordenadas UV para shader
        if (action == MotionEvent.ACTION_DOWN) {
            float uvX = event.getX() / screenWidth;
            float uvY = event.getY() / screenHeight;  // NO invertir - UV(0,0) es arriba-izquierda
            Log.d(TAG, "📍 TOUCH: nx=" + String.format("%.2f", nx) + " ny=" + String.format("%.2f", ny) +
                       " (raw: " + (int)event.getX() + "," + (int)event.getY() + ")");
            Log.d(TAG, "🎯 UV SHADER: vec2(" + String.format("%.2f", uvX) + ", " + String.format("%.2f", uvY) + ")");
        }

        // Distribuir según el modo actual
        RenderModeController.RenderMode mode = modeController.getCurrentMode();

        switch (mode) {
            case PANEL_MODE:
                return handlePanelModeTouch(nx, ny, action);

            case LOADING_MODE:
                return false; // No hay interacción en modo carga

            case WALLPAPER_MODE:
                return handleWallpaperModeTouch(nx, ny, action);

            default:
                return false;
        }
    }

    /**
     * Maneja toques en PANEL_MODE
     */
    private boolean handlePanelModeTouch(float nx, float ny, int action) {
        if (panelRenderer == null) return false;


        // Verificar PlayPauseButton (solo en DOWN/UP)
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP) {
            if (panelRenderer.isPlayButtonTouched(nx, ny)) {
                if (action == MotionEvent.ACTION_UP) {
                    Log.d(TAG, "▶️ Play button tapped");
                    if (listener != null) {
                        listener.onPlayButtonTapped();
                    }
                }
                return true;
            }
        }

        return false;
    }

    // Variable para guardar el último MotionEvent para escenas multi-touch
    private MotionEvent lastMotionEvent;

    /**
     * Maneja toques en WALLPAPER_MODE
     */
    private boolean handleWallpaperModeTouch(float nx, float ny, int action) {
        // Back button (controller overlay, top-left) - check FIRST before like button
        if (panelRenderer != null && panelRenderer.isBackButtonTouched(nx, ny)) {
            if (action == MotionEvent.ACTION_UP) {
                Log.d(TAG, "🔙 Back button tapped (controller overlay)");
                if (listener != null) {
                    listener.onBackButtonTapped();
                }
            }
            return true;
        }

        // LikeButton
        if (songSharing != null && songSharing.isTouchOnLikeButton(nx, ny)) {
            if (action == MotionEvent.ACTION_DOWN) {
                likeButtonPressed = true;
                songSharing.onLikeButtonPressed();
                if (listener != null) {
                    listener.onLikeButtonPressed();
                }
            } else if (action == MotionEvent.ACTION_UP) {
                songSharing.onLikeButtonReleased();
                if (likeButtonPressed) {
                    likeButtonPressed = false;
                    if (listener != null) {
                        listener.onLikeButtonTapped();
                    }
                }
            }
            return true;
        }

        // Soltar LikeButton si se levanta el dedo en cualquier lugar
        if (action == MotionEvent.ACTION_UP && likeButtonPressed) {
            likeButtonPressed = false;
            if (songSharing != null) {
                songSharing.onLikeButtonReleased();
            }
        }

        // Prioridad 3: Escena actual - usar método raw para multi-touch
        if (currentScene != null) {
            if (lastMotionEvent != null) {
                return currentScene.onTouchEventRaw(lastMotionEvent);
            }
            return currentScene.onTouchEvent(nx, ny, action);
        }

        return false;
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔢 NORMALIZACIÓN DE COORDENADAS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Normaliza coordenada X a rango -1 a 1
     */
    private float normalizeX(float screenX) {
        return (screenX / screenWidth) * 2.0f - 1.0f;
    }

    /**
     * Normaliza coordenada Y a rango -1 a 1 (invertida para OpenGL)
     */
    private float normalizeY(float screenY) {
        return -((screenY / screenHeight) * 2.0f - 1.0f);
    }

    /**
     * Obtiene coordenadas normalizadas de un evento
     */
    public float[] getNormalizedCoords(MotionEvent event) {
        return new float[] {
            normalizeX(event.getX()),
            normalizeY(event.getY())
        };
    }
}
