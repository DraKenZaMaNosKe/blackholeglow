package com.secret.blackholeglow.systems;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

/**
 * ScreenManager - Actor Especializado en Dimensiones de Pantalla
 *
 * RESPONSABILIDADES:
 * - Detectar y almacenar dimensiones del dispositivo
 * - Proveer acceso global a width/height para shaders y UI
 * - Convertir entre sistemas de coordenadas (pixels, NDC, OpenGL)
 * - Notificar cambios de dimension via EventBus
 * - Calcular aspect ratio y safe areas
 *
 * USO EN SHADERS (GLSL):
 *   uniform vec2 u_Resolution;
 *   // u_Resolution = vec2(ScreenManager.getWidth(), ScreenManager.getHeight())
 *
 * USO EN JAVA:
 *   float x = ScreenManager.pixelToNdcX(touchX);
 *   float y = ScreenManager.pixelToNdcY(touchY);
 *
 * SISTEMAS DE COORDENADAS:
 * - Pixels: (0,0) top-left, (width,height) bottom-right
 * - NDC (Normalized Device Coords): (-1,-1) bottom-left, (1,1) top-right
 * - OpenGL: (-1,-1) bottom-left, (1,1) top-right (Y invertido vs pixels)
 */
public class ScreenManager {
    private static final String TAG = "ScreenManager";

    // ═══════════════════════════════════════════════════════════════
    // DIMENSIONES GLOBALES - Acceso rapido para shaders
    // ═══════════════════════════════════════════════════════════════
    private static int width = 1;
    private static int height = 1;
    private static float aspectRatio = 1.0f;
    private static float density = 1.0f;
    private static int densityDpi = 160;

    // ⚡ OPTIMIZACIÓN: Arrays cacheados para evitar allocations en getResolutionArray()
    private static final float[] resolutionCache = new float[2];
    private static final float[] resolutionFloatCache = new float[2];

    // Singleton instance para metodos de instancia
    private static ScreenManager instance;
    private Context context;
    private boolean initialized = false;

    // ═══════════════════════════════════════════════════════════════
    // SINGLETON
    // ═══════════════════════════════════════════════════════════════

    private ScreenManager() {}

    public static ScreenManager get() {
        if (instance == null) {
            instance = new ScreenManager();
        }
        return instance;
    }

    /**
     * Inicializa el ScreenManager con el contexto de la app
     * Debe llamarse una vez al inicio (en WallpaperDirector o Application)
     */
    public void init(Context ctx) {
        if (initialized && context != null) {
            return;
        }

        this.context = ctx.getApplicationContext();

        // Obtener dimensiones iniciales del sistema
        try {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (wm != null) {
                DisplayMetrics metrics = new DisplayMetrics();
                wm.getDefaultDisplay().getMetrics(metrics);

                width = metrics.widthPixels;
                height = metrics.heightPixels;
                density = metrics.density;
                densityDpi = metrics.densityDpi;
                aspectRatio = (float) width / height;

                Log.d(TAG, "╔════════════════════════════════════════╗");
                Log.d(TAG, "║   📐 SCREEN MANAGER INICIALIZADO       ║");
                Log.d(TAG, "╠════════════════════════════════════════╣");
                Log.d(TAG, "║   Dimensiones: " + width + " x " + height);
                Log.d(TAG, "║   Aspect Ratio: " + String.format("%.2f", aspectRatio));
                Log.d(TAG, "║   Density: " + density + " (" + densityDpi + " dpi)");
                Log.d(TAG, "╚════════════════════════════════════════╝");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error obteniendo dimensiones: " + e.getMessage());
        }

        initialized = true;
    }

    /**
     * Actualiza las dimensiones (llamar desde onSurfaceChanged)
     * Este es el metodo principal que deben usar los renderers
     */
    public static void updateDimensions(int newWidth, int newHeight) {
        if (newWidth <= 0 || newHeight <= 0) {
            Log.w(TAG, "Dimensiones invalidas: " + newWidth + "x" + newHeight);
            return;
        }

        boolean changed = (width != newWidth || height != newHeight);

        width = newWidth;
        height = newHeight;
        aspectRatio = (float) width / height;

        if (changed) {
            Log.d(TAG, "📐 Dimensiones actualizadas: " + width + " x " + height +
                      " (AR: " + String.format("%.2f", aspectRatio) + ")");

            // Notificar via EventBus
            EventBus.get().publish(EventBus.SCREEN_RESIZED,
                new EventBus.EventData()
                    .put("width", width)
                    .put("height", height)
                    .put("aspectRatio", aspectRatio));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GETTERS ESTATICOS - Para uso en shaders y UI
    // ═══════════════════════════════════════════════════════════════

    /** Ancho de pantalla en pixels */
    public static int getWidth() {
        return width;
    }

    /** Alto de pantalla en pixels */
    public static int getHeight() {
        return height;
    }

    /** Aspect ratio (width / height) */
    public static float getAspectRatio() {
        return aspectRatio;
    }

    /** Densidad de pantalla (1.0 = mdpi, 2.0 = xhdpi, etc) */
    public static float getDensity() {
        return density;
    }

    /** DPI de la pantalla */
    public static int getDensityDpi() {
        return densityDpi;
    }

    /** Retorna true si la pantalla es portrait */
    public static boolean isPortrait() {
        return height > width;
    }

    /** Retorna true si la pantalla es landscape */
    public static boolean isLandscape() {
        return width > height;
    }

    // ═══════════════════════════════════════════════════════════════
    // CONVERSIONES DE COORDENADAS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Convierte coordenada X de pixels a NDC (-1 a 1)
     * @param pixelX Coordenada X en pixels (0 = izquierda)
     * @return Coordenada X en NDC (-1 = izquierda, 1 = derecha)
     */
    public static float pixelToNdcX(float pixelX) {
        return (pixelX / width) * 2.0f - 1.0f;
    }

    /**
     * Convierte coordenada Y de pixels a NDC (-1 a 1)
     * NOTA: Invierte Y porque pixels tiene Y=0 arriba, OpenGL tiene Y=0 abajo
     * @param pixelY Coordenada Y en pixels (0 = arriba)
     * @return Coordenada Y en NDC (-1 = abajo, 1 = arriba)
     */
    public static float pixelToNdcY(float pixelY) {
        return -((pixelY / height) * 2.0f - 1.0f);
    }

    /**
     * Convierte coordenada X de NDC a pixels
     * @param ndcX Coordenada X en NDC (-1 a 1)
     * @return Coordenada X en pixels
     */
    public static float ndcToPixelX(float ndcX) {
        return ((ndcX + 1.0f) / 2.0f) * width;
    }

    /**
     * Convierte coordenada Y de NDC a pixels
     * @param ndcY Coordenada Y en NDC (-1 a 1)
     * @return Coordenada Y en pixels
     */
    public static float ndcToPixelY(float ndcY) {
        return ((1.0f - ndcY) / 2.0f) * height;
    }

    /**
     * Convierte un tamaño en pixels a unidades NDC (horizontal)
     */
    public static float pixelSizeToNdcX(float pixelSize) {
        return (pixelSize / width) * 2.0f;
    }

    /**
     * Convierte un tamaño en pixels a unidades NDC (vertical)
     */
    public static float pixelSizeToNdcY(float pixelSize) {
        return (pixelSize / height) * 2.0f;
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILIDADES PARA POSICIONAMIENTO
    // ═══════════════════════════════════════════════════════════════

    /**
     * Calcula posicion X para centrar un objeto de cierto ancho
     * @param objectWidth Ancho del objeto en NDC
     * @return Posicion X en NDC para centrar horizontalmente
     */
    public static float centerX(float objectWidth) {
        return -objectWidth / 2.0f;
    }

    /**
     * Calcula posicion Y para centrar un objeto de cierto alto
     * @param objectHeight Alto del objeto en NDC
     * @return Posicion Y en NDC para centrar verticalmente
     */
    public static float centerY(float objectHeight) {
        return -objectHeight / 2.0f;
    }

    /**
     * Obtiene el margen seguro desde el borde (para evitar notch, etc)
     * @return Margen en NDC
     */
    public static float getSafeMargin() {
        // Aproximadamente 5% del ancho menor
        float minDim = Math.min(width, height);
        return pixelSizeToNdcX(minDim * 0.05f);
    }

    /**
     * Clamp de coordenada X dentro de limites de pantalla
     * @param x Coordenada X en NDC
     * @param margin Margen desde el borde
     * @return Coordenada clampeada
     */
    public static float clampX(float x, float margin) {
        return Math.max(-1.0f + margin, Math.min(1.0f - margin, x));
    }

    /**
     * Clamp de coordenada Y dentro de limites de pantalla
     * @param y Coordenada Y en NDC
     * @param margin Margen desde el borde
     * @return Coordenada clampeada
     */
    public static float clampY(float y, float margin) {
        return Math.max(-1.0f + margin, Math.min(1.0f - margin, y));
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILIDADES PARA SHADERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Obtiene las dimensiones como array para uniform vec2
     * ⚡ OPTIMIZADO: Retorna array cacheado (NO crear nuevos objetos)
     * @return float[] {width, height}
     */
    public static float[] getResolutionArray() {
        resolutionCache[0] = width;
        resolutionCache[1] = height;
        return resolutionCache;
    }

    /**
     * Obtiene las dimensiones como array float para uniform
     * ⚡ OPTIMIZADO: Retorna array cacheado (NO crear nuevos objetos)
     * @return float[] {width, height} como floats
     */
    public static float[] getResolutionAsFloats() {
        resolutionFloatCache[0] = (float) width;
        resolutionFloatCache[1] = (float) height;
        return resolutionFloatCache;
    }

    // ═══════════════════════════════════════════════════════════════
    // DEBUG
    // ═══════════════════════════════════════════════════════════════

    /**
     * Imprime informacion de debug sobre las dimensiones actuales
     */
    public static void logInfo() {
        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║   📐 SCREEN INFO                       ║");
        Log.d(TAG, "╠════════════════════════════════════════╣");
        Log.d(TAG, "║   Size: " + width + " x " + height + " px");
        Log.d(TAG, "║   Aspect: " + String.format("%.3f", aspectRatio));
        Log.d(TAG, "║   Orientation: " + (isPortrait() ? "Portrait" : "Landscape"));
        Log.d(TAG, "║   Density: " + density + " (" + densityDpi + " dpi)");
        Log.d(TAG, "║   Safe Margin: " + String.format("%.3f", getSafeMargin()) + " NDC");
        Log.d(TAG, "╚════════════════════════════════════════╝");
    }

    /**
     * Reset para testing
     */
    public static void reset() {
        width = 1;
        height = 1;
        aspectRatio = 1.0f;
        if (instance != null) {
            instance.initialized = false;
            instance.context = null;
        }
        instance = null;
    }
}
