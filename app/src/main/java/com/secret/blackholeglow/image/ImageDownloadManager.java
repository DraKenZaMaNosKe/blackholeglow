package com.secret.blackholeglow.image;

import android.content.Context;

import com.secret.blackholeglow.download.AbstractDownloadManager;

import java.io.File;
import java.util.List;

/**
 * ============================================================================
 *                       ImageDownloadManager
 *           Especialista en descarga de imágenes desde Supabase
 * ============================================================================
 *
 * Extiende AbstractDownloadManager para reutilizar lógica común de descarga.
 * Solo implementa métodos específicos para imágenes (Config lookup).
 *
 * USO:
 *   ImageDownloadManager mgr = ImageDownloadManager.getInstance(context);
 *   if (mgr.isImageAvailable("seiya_solo.png")) {
 *       String path = mgr.getImagePath("seiya_solo.png");
 *   } else {
 *       mgr.downloadImageSync("seiya_solo.png", callback);
 *   }
 *
 * ============================================================================
 */
public class ImageDownloadManager extends AbstractDownloadManager {

    private static final String TAG = "ImageDownload";
    private static final String IMAGE_DIR = "wallpaper_images";
    private static final String PREFS_NAME = "image_versions";

    // Singleton con double-checked locking thread-safe
    private static volatile ImageDownloadManager instance;
    private static final Object LOCK = new Object();

    // =========================================================================
    // SINGLETON
    // =========================================================================

    private ImageDownloadManager(Context context) {
        super(context);
    }

    public static ImageDownloadManager getInstance(Context context) {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new ImageDownloadManager(context);
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
        return IMAGE_DIR;
    }

    @Override
    protected String getPreferencesName() {
        return PREFS_NAME;
    }

    @Override
    protected String getRemoteUrl(String fileName) {
        return ImageConfig.getRemoteUrl(fileName);
    }

    @Override
    protected long getExpectedSize(String fileName) {
        return ImageConfig.getExpectedSize(fileName);
    }

    @Override
    protected int getResourceVersion(String fileName) {
        return ImageConfig.getImageVersion(fileName);
    }

    @Override
    protected String getResourceTypeName() {
        return "Imagen";
    }

    // =========================================================================
    // API PÚBLICA - Métodos específicos de Imagen (delegación)
    // =========================================================================

    /** Verifica si la imagen ya está descargada y es la versión correcta. */
    public boolean isImageAvailable(String fileName) {
        return isResourceAvailable(fileName);
    }

    /** Obtiene la ruta de la imagen en cache. */
    public String getImagePath(String fileName) {
        return getResourcePath(fileName);
    }

    /** Descarga una imagen en background. */
    public void downloadImage(String fileName, DownloadCallback callback) {
        downloadResource(fileName, callback);
    }

    /** Descarga una imagen de forma síncrona. */
    public boolean downloadImageSync(String fileName, SyncProgressCallback callback) {
        return downloadResourceSync(fileName, callback);
    }

    /** Obtiene el directorio de imágenes. */
    public File getImageDirectory() {
        return getResourceDirectory();
    }

    /** Elimina una imagen. */
    public void deleteImage(String fileName) {
        deleteResource(fileName);
    }

    /** Obtiene el número de imágenes en cache. */
    public int getCachedImageCount() {
        return getCachedResourceCount();
    }
}
