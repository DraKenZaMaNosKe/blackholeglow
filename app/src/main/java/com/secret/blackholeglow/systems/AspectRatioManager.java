package com.secret.blackholeglow.systems;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   📐 AspectRatioManager - Actor Centralizado de Aspect Ratio            ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  RESPONSABILIDAD ÚNICA:                                                  ║
 * ║  • Mantener y distribuir el aspect ratio correcto a toda la app         ║
 * ║  • Notificar a todos los listeners cuando cambia                        ║
 * ║  • Proveer utilidades de conversión de coordenadas                      ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  USO:                                                                    ║
 * ║  1. Implementar AspectRatioAware en tu clase                            ║
 * ║  2. Registrarte: AspectRatioManager.get().register(this)                ║
 * ║  3. Recibir automáticamente onAspectRatioChanged(width, height, ratio)  ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  VALORES DISPONIBLES:                                                    ║
 * ║  • getAspectRatio() - width/height (ej: 0.46 para portrait)             ║
 * ║  • getInverseAspectRatio() - height/width (ej: 2.17 para portrait)      ║
 * ║  • getScreenWidth(), getScreenHeight()                                   ║
 * ║  • isPortrait(), isLandscape()                                          ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class AspectRatioManager {
    private static final String TAG = "AspectRatioManager";

    // ════════════════════════════════════════════════════════════════════════
    // SINGLETON
    // ════════════════════════════════════════════════════════════════════════
    private static AspectRatioManager instance;

    public static AspectRatioManager get() {
        if (instance == null) {
            synchronized (AspectRatioManager.class) {
                if (instance == null) {
                    instance = new AspectRatioManager();
                }
            }
        }
        return instance;
    }

    public static void reset() {
        if (instance != null) {
            instance.listeners.clear();
            instance = null;
        }
        Log.d(TAG, "📐 AspectRatioManager reset");
    }

    // ════════════════════════════════════════════════════════════════════════
    // ESTADO
    // ════════════════════════════════════════════════════════════════════════
    private int screenWidth = 1080;
    private int screenHeight = 1920;
    private float aspectRatio = 1080f / 1920f;  // width/height
    private float inverseAspectRatio = 1920f / 1080f;  // height/width

    // ════════════════════════════════════════════════════════════════════════
    // LISTENERS
    // ════════════════════════════════════════════════════════════════════════
    private final List<AspectRatioAware> listeners = new ArrayList<>();

    // ════════════════════════════════════════════════════════════════════════
    // INTERFACE
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Interface que deben implementar los objetos que necesitan el aspect ratio
     */
    public interface AspectRatioAware {
        /**
         * Llamado cuando el aspect ratio cambia
         * @param width Ancho de pantalla en pixels
         * @param height Alto de pantalla en pixels
         * @param aspectRatio width/height
         */
        void onAspectRatioChanged(int width, int height, float aspectRatio);
    }

    // ════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ════════════════════════════════════════════════════════════════════════
    private AspectRatioManager() {
        Log.d(TAG, "📐 AspectRatioManager inicializado");
    }

    // ════════════════════════════════════════════════════════════════════════
    // REGISTRO DE LISTENERS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Registra un listener para recibir cambios de aspect ratio.
     * Inmediatamente recibe el aspect ratio actual.
     */
    public void register(AspectRatioAware listener) {
        if (listener == null) return;

        if (!listeners.contains(listener)) {
            listeners.add(listener);
            // Notificar inmediatamente con el valor actual
            listener.onAspectRatioChanged(screenWidth, screenHeight, aspectRatio);
            Log.d(TAG, "📐 Registrado: " + listener.getClass().getSimpleName() +
                       " (total: " + listeners.size() + ")");
        }
    }

    /**
     * Desregistra un listener
     */
    public void unregister(AspectRatioAware listener) {
        if (listener == null) return;

        if (listeners.remove(listener)) {
            Log.d(TAG, "📐 Desregistrado: " + listener.getClass().getSimpleName());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // ACTUALIZACIÓN
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Actualiza las dimensiones de pantalla.
     * Llamar desde onSurfaceChanged() del renderer.
     * Notifica a todos los listeners si hay cambio.
     */
    public void updateDimensions(int width, int height) {
        if (width <= 0 || height <= 0) return;

        // Solo notificar si realmente cambió
        if (width == screenWidth && height == screenHeight) return;

        screenWidth = width;
        screenHeight = height;
        aspectRatio = (float) width / height;
        inverseAspectRatio = (float) height / width;

        Log.d(TAG, String.format("📐 Dimensiones: %dx%d | AR: %.3f | Inv: %.3f | %s",
                width, height, aspectRatio, inverseAspectRatio,
                isPortrait() ? "PORTRAIT" : "LANDSCAPE"));

        // Notificar a todos los listeners
        notifyListeners();
    }

    /**
     * Notifica a todos los listeners registrados
     */
    private void notifyListeners() {
        for (AspectRatioAware listener : listeners) {
            try {
                listener.onAspectRatioChanged(screenWidth, screenHeight, aspectRatio);
            } catch (Exception e) {
                Log.e(TAG, "Error notificando a " + listener.getClass().getSimpleName(), e);
            }
        }
        Log.d(TAG, "📐 Notificados " + listeners.size() + " listeners");
    }

    // ════════════════════════════════════════════════════════════════════════
    // GETTERS
    // ════════════════════════════════════════════════════════════════════════

    /** @return width/height (ej: 0.46 para 1080x2340 portrait) */
    public float getAspectRatio() {
        return aspectRatio;
    }

    /** @return height/width (ej: 2.17 para 1080x2340 portrait) */
    public float getInverseAspectRatio() {
        return inverseAspectRatio;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public boolean isPortrait() {
        return screenHeight > screenWidth;
    }

    public boolean isLandscape() {
        return screenWidth > screenHeight;
    }

    // ════════════════════════════════════════════════════════════════════════
    // UTILIDADES DE CONVERSIÓN
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Convierte coordenadas de pixel a NDC (-1 a 1)
     * @param pixelX Coordenada X en pixels
     * @return X en NDC
     */
    public float pixelToNdcX(float pixelX) {
        return (pixelX / screenWidth) * 2f - 1f;
    }

    /**
     * Convierte coordenadas de pixel a NDC (-1 a 1)
     * @param pixelY Coordenada Y en pixels
     * @return Y en NDC (invertido para OpenGL)
     */
    public float pixelToNdcY(float pixelY) {
        return 1f - (pixelY / screenHeight) * 2f;
    }

    /**
     * Convierte NDC a pixels
     */
    public float ndcToPixelX(float ndcX) {
        return (ndcX + 1f) * 0.5f * screenWidth;
    }

    /**
     * Convierte NDC a pixels
     */
    public float ndcToPixelY(float ndcY) {
        return (1f - ndcY) * 0.5f * screenHeight;
    }

    /**
     * Para proyección ortográfica que respeta aspect ratio:
     * Matrix.orthoM(matrix, 0, -aspectRatio, aspectRatio, -1, 1, -1, 1);
     *
     * @return Los límites X para orthoM: [-aspectRatio, aspectRatio]
     */
    public float getOrthoLeft() {
        return -aspectRatio;
    }

    public float getOrthoRight() {
        return aspectRatio;
    }

    /**
     * Calcula el ancho en unidades NDC para un porcentaje del ancho de pantalla
     * @param percent 0.0 a 1.0 (ej: 0.8 = 80% del ancho)
     * @return Ancho en unidades de proyección ortográfica
     */
    public float percentWidthToOrtho(float percent) {
        return aspectRatio * 2f * percent;
    }

    /**
     * Calcula la altura en unidades NDC para un porcentaje del alto de pantalla
     * @param percent 0.0 a 1.0 (ej: 0.1 = 10% del alto)
     * @return Altura en unidades de proyección ortográfica
     */
    public float percentHeightToOrtho(float percent) {
        return 2f * percent;  // Y va de -1 a 1
    }
}
