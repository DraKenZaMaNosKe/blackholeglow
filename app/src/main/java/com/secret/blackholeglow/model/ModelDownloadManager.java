package com.secret.blackholeglow.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                    ModelDownloadManager                          ║
 * ║        Especialista en descarga de modelos 3D desde Supabase     ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  RESPONSABILIDADES:                                              ║
 * ║  • Descargar modelos OBJ/MTL desde Supabase                      ║
 * ║  • Cachear modelos en almacenamiento local                       ║
 * ║  • Verificar versiones para auto-actualización                   ║
 * ║  • Reportar progreso de descarga                                 ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  USO:                                                            ║
 * ║    ModelDownloadManager mgr = ModelDownloadManager.getInstance();║
 * ║    if (mgr.isModelAvailable("delorean.obj")) {                   ║
 * ║        String path = mgr.getModelPath("delorean.obj");           ║
 * ║    } else {                                                      ║
 * ║        mgr.downloadModelSync("delorean.obj", callback);          ║
 * ║    }                                                             ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  ARQUITECTURA SRP (Single Responsibility Principle):             ║
 * ║  • VideoDownloadManager  → videos (.mp4)                         ║
 * ║  • ImageDownloadManager  → imágenes y texturas (.png, .webp)     ║
 * ║  • ModelDownloadManager  → modelos 3D (.obj, .mtl)               ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class ModelDownloadManager {
    private static final String TAG = "ModelDownload";
    private static final String MODEL_DIR = "wallpaper_models";
    private static final String PREFS_NAME = "model_versions";
    private static final String VERSION_PREFIX = "v_";

    private static ModelDownloadManager instance;
    private final Context context;
    private final File modelDir;
    private final ExecutorService executor;
    private final SharedPreferences versionPrefs;

    private ModelDownloadManager(Context context) {
        this.context = context.getApplicationContext();
        this.modelDir = new File(context.getFilesDir(), MODEL_DIR);
        if (!modelDir.exists()) {
            modelDir.mkdirs();
        }
        this.executor = Executors.newSingleThreadExecutor();
        this.versionPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Log.d(TAG, "🎭 Model storage dir: " + modelDir.getAbsolutePath());
    }

    public static synchronized ModelDownloadManager getInstance(Context context) {
        if (instance == null) {
            instance = new ModelDownloadManager(context);
        }
        return instance;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // VERIFICACIÓN DE DISPONIBILIDAD
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Verifica si un modelo ya está descargado Y es la versión correcta.
     * Si la versión en ModelConfig es mayor que la descargada, elimina y retorna false.
     * 🛡️ También verifica permisos de lectura.
     */
    public boolean isModelAvailable(String fileName) {
        File modelFile = new File(modelDir, fileName);
        if (!modelFile.exists()) {
            return false;
        }

        // 🛡️ VERIFICAR PERMISOS DE LECTURA
        if (!modelFile.canRead()) {
            Log.e(TAG, "🛡️ Sin permiso de lectura: " + fileName);
            return false;
        }

        // Verificar VERSION - si hay nueva versión, eliminar archivo viejo
        int currentVersion = ModelConfig.getModelVersion(fileName);
        int storedVersion = versionPrefs.getInt(VERSION_PREFIX + fileName, 0);
        if (currentVersion > storedVersion) {
            Log.w(TAG, "🔄 Nueva versión de modelo: " + fileName +
                  " (local:" + storedVersion + " -> remota:" + currentVersion + ")");
            modelFile.delete();
            return false;
        }

        // Verificar tamaño (descarga parcial?)
        long expectedSize = ModelConfig.getExpectedSize(fileName);
        if (expectedSize > 0 && modelFile.length() < expectedSize * 0.95) {
            Log.w(TAG, "⚠️ Modelo incompleto: " + fileName +
                  " (" + modelFile.length() + "/" + expectedSize + ")");
            modelFile.delete();
            versionPrefs.edit().remove(VERSION_PREFIX + fileName).apply();
            return false;
        }

        return true;
    }

    /**
     * Obtiene la ruta de un modelo en storage local.
     * @return null si no está disponible
     */
    public String getModelPath(String fileName) {
        if (!isModelAvailable(fileName)) {
            return null;
        }
        return new File(modelDir, fileName).getAbsolutePath();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DESCARGA ASÍNCRONA
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Descarga un modelo en background.
     */
    public void downloadModel(String fileName, DownloadCallback callback) {
        String remoteUrl = ModelConfig.getRemoteUrl(fileName);
        if (remoteUrl == null) {
            Log.e(TAG, "❌ Modelo no configurado: " + fileName);
            if (callback != null) {
                callback.onError("Modelo no configurado: " + fileName);
            }
            return;
        }

        executor.execute(() -> {
            try {
                downloadModelInternal(fileName, remoteUrl, callback);
            } catch (Exception e) {
                Log.e(TAG, "❌ Error descargando: " + e.getMessage());
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    private void downloadModelInternal(String fileName, String urlStr, DownloadCallback callback)
            throws IOException {
        Log.d(TAG, "📥 Descargando modelo: " + urlStr);

        File tempFile = new File(modelDir, fileName + ".tmp");
        File finalFile = new File(modelDir, fileName);

        HttpURLConnection connection = null;
        InputStream input = null;
        FileOutputStream output = null;

        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error: " + responseCode);
            }

            long totalBytes = connection.getContentLength();
            Log.d(TAG, "📦 Tamaño: " + (totalBytes / 1024) + " KB");

            input = connection.getInputStream();
            output = new FileOutputStream(tempFile);

            byte[] buffer = new byte[8192];
            long downloadedBytes = 0;
            int bytesRead;
            int lastProgress = 0;

            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                downloadedBytes += bytesRead;

                if (totalBytes > 0 && callback != null) {
                    int progress = (int) (downloadedBytes * 100 / totalBytes);
                    if (progress != lastProgress) {
                        lastProgress = progress;
                        callback.onProgress(progress, downloadedBytes, totalBytes);
                    }
                }
            }

            // ⚡ CRÍTICO: flush + sync para garantizar que los datos lleguen al disco
            output.flush();
            output.getFD().sync();  // Fuerza escritura al sistema de archivos
            output.close();
            output = null;

            // Renombrar a archivo final
            if (finalFile.exists()) {
                finalFile.delete();
            }
            if (!tempFile.renameTo(finalFile)) {
                throw new IOException("Error moviendo archivo temporal");
            }

            // ⚡ Verificar que el archivo existe y tiene contenido
            if (!finalFile.exists() || finalFile.length() == 0) {
                throw new IOException("Archivo no disponible después de guardar");
            }

            // ⚠️ CRÍTICO: Guardar versión con commit() (síncrono) NO apply() (async)
            int version = ModelConfig.getModelVersion(fileName);
            boolean saved = versionPrefs.edit().putInt(VERSION_PREFIX + fileName, version).commit();
            if (!saved) {
                Log.e(TAG, "⚠️ Error guardando versión en preferencias");
            }
            Log.d(TAG, "✅ Versión " + version + " guardada (sync) para: " + fileName);

            Log.d(TAG, "✅ Descarga completada: " + fileName);
            if (callback != null) {
                callback.onComplete(finalFile.getAbsolutePath());
            }

        } finally {
            if (output != null) try { output.close(); } catch (Exception ignored) {}
            if (input != null) try { input.close(); } catch (Exception ignored) {}
            if (connection != null) connection.disconnect();
            if (tempFile.exists()) tempFile.delete();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DESCARGA SÍNCRONA (para ResourcePreloader)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Descarga un modelo de forma SÍNCRONA (bloquea el thread).
     * Usar solo desde background threads como ResourcePreloader.
     *
     * @return true si la descarga fue exitosa o el modelo ya existía
     */
    public boolean downloadModelSync(String fileName, SyncProgressCallback callback) {
        // Si ya existe, retornar éxito
        if (isModelAvailable(fileName)) {
            Log.d(TAG, "✅ Modelo ya disponible: " + fileName);
            return true;
        }

        String remoteUrl = ModelConfig.getRemoteUrl(fileName);
        if (remoteUrl == null) {
            Log.e(TAG, "❌ Modelo no configurado: " + fileName);
            return false;
        }

        try {
            downloadModelSyncInternal(fileName, remoteUrl, callback);
            // ⚡ Guardar versión con commit() (síncrono) después de descarga exitosa
            int version = ModelConfig.getModelVersion(fileName);
            boolean saved = versionPrefs.edit().putInt(VERSION_PREFIX + fileName, version).commit();
            if (!saved) {
                Log.e(TAG, "⚠️ Error guardando versión en preferencias");
            }
            Log.d(TAG, "✅ Versión " + version + " guardada (sync) para: " + fileName);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "❌ Error descargando modelo: " + e.getMessage());
            return false;
        }
    }

    private void downloadModelSyncInternal(String fileName, String urlStr, SyncProgressCallback callback)
            throws IOException {
        Log.d(TAG, "📥 Descargando (sync): " + urlStr);

        File tempFile = new File(modelDir, fileName + ".tmp");
        File finalFile = new File(modelDir, fileName);

        HttpURLConnection connection = null;
        InputStream input = null;
        FileOutputStream output = null;

        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error: " + responseCode);
            }

            long totalBytes = connection.getContentLength();
            input = connection.getInputStream();
            output = new FileOutputStream(tempFile);

            byte[] buffer = new byte[8192];
            long downloadedBytes = 0;
            int bytesRead;
            int lastProgress = 0;

            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                downloadedBytes += bytesRead;

                if (totalBytes > 0 && callback != null) {
                    int progress = (int) (downloadedBytes * 100 / totalBytes);
                    if (progress != lastProgress) {
                        lastProgress = progress;
                        callback.onProgress(progress);
                    }
                }
            }

            // ⚡ CRÍTICO: flush + sync para garantizar que los datos lleguen al disco
            output.flush();
            output.getFD().sync();  // Fuerza escritura al sistema de archivos
            output.close();
            output = null;

            // Renombrar a archivo final
            if (finalFile.exists()) {
                finalFile.delete();
            }
            if (!tempFile.renameTo(finalFile)) {
                throw new IOException("Error moviendo archivo temporal");
            }

            // ⚡ Verificar que el archivo existe y tiene contenido
            if (!finalFile.exists() || finalFile.length() == 0) {
                throw new IOException("Archivo no disponible después de guardar");
            }

            Log.d(TAG, "✅ Descarga sync completada: " + fileName);

        } finally {
            if (output != null) try { output.close(); } catch (Exception ignored) {}
            if (input != null) try { input.close(); } catch (Exception ignored) {}
            if (connection != null) connection.disconnect();
            if (tempFile.exists()) tempFile.delete();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GESTIÓN DE CACHE
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Obtiene el directorio donde se guardan los modelos.
     */
    public File getModelDirectory() {
        return modelDir;
    }

    /**
     * Elimina un modelo del storage y su versión.
     */
    public void deleteModel(String fileName) {
        File file = new File(modelDir, fileName);
        if (file.exists()) {
            file.delete();
        }
        versionPrefs.edit().remove(VERSION_PREFIX + fileName).apply();
        Log.d(TAG, "🗑️ Modelo eliminado: " + fileName);
    }

    /**
     * Elimina todos los modelos del cache y sus versiones.
     */
    public void clearCache() {
        File[] files = modelDir.listFiles();
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }
        versionPrefs.edit().clear().apply();
        Log.d(TAG, "🗑️ Cache de modelos limpiado");
    }

    /**
     * Obtiene el tamaño total del cache de modelos en bytes.
     */
    public long getCacheSize() {
        long size = 0;
        File[] files = modelDir.listFiles();
        if (files != null) {
            for (File f : files) {
                size += f.length();
            }
        }
        return size;
    }

    /**
     * Obtiene el número de modelos en cache.
     */
    public int getCachedModelCount() {
        File[] files = modelDir.listFiles();
        return files != null ? files.length : 0;
    }

    /**
     * 🗑️ Elimina todos los modelos EXCEPTO los especificados.
     * Usado para limpiar recursos de escenas anteriores al cambiar de wallpaper.
     *
     * @param keepFiles Lista de nombres de archivo a mantener
     * @return Cantidad de bytes liberados
     */
    public long deleteAllExcept(java.util.List<String> keepFiles) {
        long freedBytes = 0;
        File[] files = modelDir.listFiles();

        if (files == null || files.length == 0) {
            Log.d(TAG, "🗑️ No hay modelos para limpiar");
            return 0;
        }

        java.util.Set<String> keepSet = new java.util.HashSet<>(keepFiles);
        int deletedCount = 0;

        for (File file : files) {
            String fileName = file.getName();
            // Ignorar archivos temporales
            if (fileName.endsWith(".tmp")) {
                file.delete();
                continue;
            }

            // Si NO está en la lista de mantener, eliminar
            if (!keepSet.contains(fileName)) {
                long fileSize = file.length();
                if (file.delete()) {
                    freedBytes += fileSize;
                    deletedCount++;
                    versionPrefs.edit().remove(VERSION_PREFIX + fileName).apply();
                    Log.d(TAG, "🗑️ Eliminado: " + fileName + " (" + (fileSize / 1024) + " KB)");
                }
            }
        }

        Log.d(TAG, "🧹 Limpieza completada: " + deletedCount + " modelos eliminados, " +
                   (freedBytes / 1024) + " KB liberados");
        return freedBytes;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // INTERFACES DE CALLBACK
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Callback para progreso de descarga asíncrona.
     */
    public interface DownloadCallback {
        void onProgress(int percent, long downloadedBytes, long totalBytes);
        void onComplete(String filePath);
        void onError(String message);
    }

    /**
     * Callback simple para progreso síncrono.
     */
    public interface SyncProgressCallback {
        void onProgress(int percent);
    }
}
