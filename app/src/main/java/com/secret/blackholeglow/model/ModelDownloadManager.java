package com.secret.blackholeglow.model;

import android.content.Context;

import com.secret.blackholeglow.download.AbstractDownloadManager;

import java.io.File;
import java.util.List;

/**
 * ============================================================================
 *                       ModelDownloadManager
 *         Especialista en descarga de modelos 3D desde Supabase
 * ============================================================================
 *
 * Extiende AbstractDownloadManager para reutilizar lógica común de descarga.
 * Solo implementa métodos específicos para modelos 3D (Config lookup).
 *
 * USO:
 *   ModelDownloadManager mgr = ModelDownloadManager.getInstance(context);
 *   if (mgr.isModelAvailable("delorean.obj")) {
 *       String path = mgr.getModelPath("delorean.obj");
 *   } else {
 *       mgr.downloadModelSync("delorean.obj", callback);
 *   }
 *
 * ARQUITECTURA SRP (Single Responsibility Principle):
 *   - VideoDownloadManager  → videos (.mp4)
 *   - ImageDownloadManager  → imágenes y texturas (.png, .webp)
 *   - ModelDownloadManager  → modelos 3D (.obj, .mtl)
 *
 * ============================================================================
 */
public class ModelDownloadManager extends AbstractDownloadManager {

    private static final String TAG = "ModelDownload";
    private static final String MODEL_DIR = "wallpaper_models";
    private static final String PREFS_NAME = "model_versions";

    // Singleton con double-checked locking thread-safe
    private static volatile ModelDownloadManager instance;
    private static final Object LOCK = new Object();

    // =========================================================================
    // SINGLETON
    // =========================================================================

    private ModelDownloadManager(Context context) {
        super(context);
    }

    public static ModelDownloadManager getInstance(Context context) {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new ModelDownloadManager(context);
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
        return MODEL_DIR;
    }

    @Override
    protected String getPreferencesName() {
        return PREFS_NAME;
    }

    @Override
    protected String getRemoteUrl(String fileName) {
        return ModelConfig.getRemoteUrl(fileName);
    }

    @Override
    protected long getExpectedSize(String fileName) {
        return ModelConfig.getExpectedSize(fileName);
    }

    @Override
    protected int getResourceVersion(String fileName) {
        return ModelConfig.getModelVersion(fileName);
    }

    @Override
    protected String getResourceTypeName() {
        return "Modelo";
    }

    // =========================================================================
    // API PÚBLICA - Métodos específicos de Modelo (delegación)
    // =========================================================================

    /** Verifica si el modelo ya está descargado y es la versión correcta. */
    public boolean isModelAvailable(String fileName) {
        return isResourceAvailable(fileName);
    }

    /** Obtiene la ruta del modelo en cache. */
    public String getModelPath(String fileName) {
        return getResourcePath(fileName);
    }

    /** Descarga un modelo en background. */
    public void downloadModel(String fileName, DownloadCallback callback) {
        downloadResource(fileName, callback);
    }

    /** Descarga un modelo de forma síncrona. */
    public boolean downloadModelSync(String fileName, SyncProgressCallback callback) {
        return downloadResourceSync(fileName, callback);
    }

    /** Obtiene el directorio de modelos. */
    public File getModelDirectory() {
        return getResourceDirectory();
    }

    /** Elimina un modelo. */
    public void deleteModel(String fileName) {
        deleteResource(fileName);
    }

    /** Obtiene el número de modelos en cache. */
    public int getCachedModelCount() {
        return getCachedResourceCount();
    }
}
