package com.secret.blackholeglow.video;

import android.content.Context;

import com.secret.blackholeglow.download.AbstractDownloadManager;

import java.io.File;
import java.util.List;

/**
 * ============================================================================
 *                       VideoDownloadManager
 *            Especialista en descarga de videos desde Supabase
 * ============================================================================
 *
 * Extiende AbstractDownloadManager para reutilizar lógica común de descarga.
 * Solo implementa métodos específicos para videos (Config lookup).
 *
 * USO:
 *   VideoDownloadManager mgr = VideoDownloadManager.getInstance(context);
 *   if (mgr.isVideoAvailable("cielovolando.mp4")) {
 *       String path = mgr.getVideoPath("cielovolando.mp4");
 *   } else {
 *       mgr.downloadVideo("cielovolando.mp4", callback);
 *   }
 *
 * ============================================================================
 */
public class VideoDownloadManager extends AbstractDownloadManager {

    private static final String TAG = "VideoDownload";
    private static final String VIDEO_DIR = "wallpaper_videos";
    private static final String PREFS_NAME = "resource_versions";

    // Singleton con double-checked locking thread-safe
    private static volatile VideoDownloadManager instance;
    private static final Object LOCK = new Object();

    // =========================================================================
    // SINGLETON
    // =========================================================================

    private VideoDownloadManager(Context context) {
        super(context);
    }

    public static VideoDownloadManager getInstance(Context context) {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new VideoDownloadManager(context);
                }
            }
        }
        return instance;
    }

    // =========================================================================
    // IMPLEMENTACIÓN DE MÉTODOS ABSTRACTOS
    // =========================================================================

    @Override
    protected String getTag() {
        return TAG;
    }

    @Override
    protected String getDirectoryName() {
        return VIDEO_DIR;
    }

    @Override
    protected String getPreferencesName() {
        return PREFS_NAME;
    }

    @Override
    protected String getRemoteUrl(String fileName) {
        return VideoConfig.getRemoteUrl(fileName);
    }

    @Override
    protected long getExpectedSize(String fileName) {
        return VideoConfig.getExpectedSize(fileName);
    }

    @Override
    protected int getResourceVersion(String fileName) {
        return VideoConfig.getVideoVersion(fileName);
    }

    @Override
    protected String getResourceTypeName() {
        return "Video";
    }

    // =========================================================================
    // API PÚBLICA - Métodos específicos de Video (delegación)
    // =========================================================================

    /** Verifica si el video ya está descargado y es la versión correcta. */
    public boolean isVideoAvailable(String fileName) {
        return isResourceAvailable(fileName);
    }

    /** Obtiene la ruta del video en cache. */
    public String getVideoPath(String fileName) {
        return getResourcePath(fileName);
    }

    /** Descarga un video en background. */
    public void downloadVideo(String fileName, DownloadCallback callback) {
        downloadResource(fileName, callback);
    }

    /** Descarga un video de forma síncrona. */
    public boolean downloadVideoSync(String fileName, SyncProgressCallback callback) {
        return downloadResourceSync(fileName, callback);
    }

    /** Obtiene el directorio de videos. */
    public File getVideoDirectory() {
        return getResourceDirectory();
    }

    /** Elimina un video. */
    public void deleteVideo(String fileName) {
        deleteResource(fileName);
    }

    /** Obtiene el número de videos en cache. */
    public int getCachedVideoCount() {
        return getCachedResourceCount();
    }
}
