package com.secret.blackholeglow.core;

import android.content.Context;
import android.opengl.GLES30;
import android.util.Log;

import com.secret.blackholeglow.ArcaneGrimoire;
import com.secret.blackholeglow.LoadingBar;
import com.secret.blackholeglow.OrbixGreeting;
import com.secret.blackholeglow.video.MediaCodecVideoRenderer;
import com.secret.blackholeglow.video.VideoDownloadManager;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                     PanelModeRenderer                            ║
 * ║            Renderizador de UI del Panel de Control               ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  COMPONENTES:                                                    ║
 * ║  • Video de fondo: The House (cabaña acogedora con chimenea)     ║
 * ║  • ArcaneGrimoire: Libro mágico 3D flotante (toca para iniciar)  ║
 * ║  • OrbixGreeting: Saludo + reloj + cuenta regresiva              ║
 * ║  • LoadingBar: Barra de carga                                    ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class PanelModeRenderer {
    private static final String TAG = "PanelModeRenderer";

    // Video de fondo del panel
    private static final String PANEL_VIDEO_FILE = "thehouse.mp4";
    private MediaCodecVideoRenderer videoBackground;
    private VideoDownloadManager videoDownloadManager;
    private boolean videoReady = false;
    private boolean videoDownloading = false;
    private int downloadProgress = 0;

    // Componentes UI estándar
    private ArcaneGrimoire grimoire;
    private OrbixGreeting orbixGreeting;
    private LoadingBar loadingBar;

    // Estado
    private boolean initialized = false;
    private final Context context;
    private boolean greetingEnabled = true;
    private int screenWidth, screenHeight;

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

        // VideoDownloadManager
        videoDownloadManager = VideoDownloadManager.getInstance(context);

        // Verificar si el video ya está descargado
        if (videoDownloadManager.isVideoAvailable(PANEL_VIDEO_FILE)) {
            Log.d(TAG, "🎬 Video de panel ya disponible");
            initializeVideoBackground();
        } else {
            Log.d(TAG, "📥 Descargando video de panel...");
            startVideoDownload();
        }

        // ArcaneGrimoire - Libro mágico 3D
        grimoire = new ArcaneGrimoire(context);
        grimoire.initialize();
        Log.d(TAG, "📖 ArcaneGrimoire inicializado");

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

    /**
     * Inicializa el video de fondo una vez descargado
     */
    private void initializeVideoBackground() {
        String videoPath = videoDownloadManager.getVideoPath(PANEL_VIDEO_FILE);
        if (videoPath == null) {
            Log.e(TAG, "❌ Video path es null");
            return;
        }

        videoBackground = new MediaCodecVideoRenderer(context, PANEL_VIDEO_FILE, videoPath);
        videoBackground.initialize();
        if (screenWidth > 0 && screenHeight > 0) {
            videoBackground.setScreenSize(screenWidth, screenHeight);
        }
        videoReady = true;
        Log.d(TAG, "🎬 Video de fondo inicializado: " + videoPath);
    }

    /**
     * Inicia la descarga del video de fondo
     */
    private void startVideoDownload() {
        videoDownloading = true;
        downloadProgress = 0;

        videoDownloadManager.downloadVideo(PANEL_VIDEO_FILE, new VideoDownloadManager.DownloadCallback() {
            @Override
            public void onProgress(int percent, long downloadedBytes, long totalBytes) {
                downloadProgress = percent;
                Log.d(TAG, "📥 Descargando video: " + percent + "%");
            }

            @Override
            public void onComplete(String filePath) {
                Log.d(TAG, "✅ Video descargado: " + filePath);
                videoDownloading = false;
                // Inicializar video en el siguiente frame (desde GL thread)
                pendingVideoInit = true;
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "❌ Error descargando video: " + message);
                videoDownloading = false;
            }
        });
    }

    // Flag para inicializar video desde GL thread
    private volatile boolean pendingVideoInit = false;

    // ═══════════════════════════════════════════════════════════════
    // 🔄 UPDATE
    // ═══════════════════════════════════════════════════════════════

    public void updatePanelMode(float deltaTime) {
        // Inicializar video si está pendiente (debe hacerse en GL thread)
        if (pendingVideoInit) {
            pendingVideoInit = false;
            initializeVideoBackground();
        }

        if (grimoire != null) {
            grimoire.update(deltaTime);
        }
        if (orbixGreeting != null) {
            orbixGreeting.update(deltaTime);
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
        // 1. Dibujar video de fondo (si está listo)
        if (videoReady && videoBackground != null) {
            GLES30.glDisable(GLES30.GL_DEPTH_TEST);
            GLES30.glDisable(GLES30.GL_BLEND);
            videoBackground.draw();
        }

        // 2. Dibujar UI encima del video
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        if (grimoire != null) {
            grimoire.draw();
        }
        if (orbixGreeting != null) {
            orbixGreeting.draw();
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

    /**
     * Inicia la pantalla de carga con el tema del wallpaper
     * @param sceneName ID de la escena (para SceneFactory)
     * @param displayName Nombre bonito del wallpaper (ej: "ABYSSIA")
     * @param glowColor Color glow del wallpaper
     */
    public void onStartLoading(String sceneName, String displayName, int glowColor) {
        if (orbixGreeting != null) {
            orbixGreeting.hide();
        }
        if (loadingBar != null) {
            loadingBar.reset();
            loadingBar.setWallpaperTheme(displayName, glowColor);
            loadingBar.setBackgroundForScene(context, sceneName);
            loadingBar.show();
            loadingBar.setProgress(1.0f);
        }
        Log.d(TAG, "Cargando: " + displayName + " (escena: " + sceneName + ")");
    }

    public void onStartLoading(String sceneName) {
        // Fallback con colores por defecto
        onStartLoading(sceneName, sceneName, 0xFF00D4FF);
    }

    public void onStartLoading() {
        onStartLoading(null, "Orbix", 0xFF00D4FF);
    }

    public void onWallpaperActivated() {
        Log.d(TAG, "🎬 Wallpaper activado");
        if (orbixGreeting != null) orbixGreeting.hide();
    }

    public void onReturnToPanel() {
        greetingEnabled = true;
        if (orbixGreeting != null) {
            orbixGreeting.show();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 👆 TOUCH DETECTION
    // ═══════════════════════════════════════════════════════════════

    public boolean isPlayButtonTouched(float nx, float ny) {
        // Grimoire touch detection - toca el libro para activar
        if (grimoire != null) {
            return grimoire.isInside(nx, ny);
        }
        return true;  // Fallback: cualquier toque
    }

    // ═══════════════════════════════════════════════════════════════
    // 📐 SCREEN SIZE
    // ═══════════════════════════════════════════════════════════════

    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        float aspectRatio = (float) width / height;

        // Video de fondo
        if (videoBackground != null) {
            videoBackground.setScreenSize(width, height);
        }

        // Grimoire
        if (grimoire != null) {
            grimoire.setScreenSize(width, height);
            grimoire.setAspectRatio(aspectRatio);
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
        // Liberar video de fondo
        if (videoBackground != null) {
            videoBackground.release();
            videoBackground = null;
        }
        videoReady = false;

        if (grimoire != null) {
            grimoire.release();
            grimoire = null;
        }

        if (orbixGreeting != null) {
            orbixGreeting.dispose();
            orbixGreeting = null;
        }

        Log.d(TAG, "🧹 PanelModeRenderer recursos liberados");
    }

    /**
     * @return true si el video de fondo está listo para reproducir
     */
    public boolean isVideoReady() {
        return videoReady;
    }

    /**
     * @return progreso de descarga del video (0-100)
     */
    public int getDownloadProgress() {
        return downloadProgress;
    }

    /**
     * @return true si el video se está descargando
     */
    public boolean isVideoDownloading() {
        return videoDownloading;
    }
}
