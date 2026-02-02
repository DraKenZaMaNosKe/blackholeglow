package com.secret.blackholeglow.download;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * ============================================================================
 *                      AbstractDownloadManager
 *           Clase base para gestionar descargas desde Supabase
 * ============================================================================
 *
 * RESPONSABILIDADES:
 *   - Descargar recursos desde URLs remotas
 *   - Cachear en almacenamiento local
 *   - Verificar versiones para auto-actualización
 *   - Reportar progreso de descarga
 *   - Gestionar limpieza de cache
 *
 * SUBCLASES:
 *   - VideoDownloadManager  (videos .mp4)
 *   - ImageDownloadManager  (imágenes .png, .webp)
 *   - ModelDownloadManager  (modelos .obj)
 *
 * PATRÓN: Template Method - las subclases implementan métodos abstractos
 *         para obtener URLs y versiones de sus respectivos Configs.
 *
 * ============================================================================
 */
public abstract class AbstractDownloadManager {

    // =========================================================================
    // CONSTANTES DE RED
    // =========================================================================
    protected static final int CONNECTION_TIMEOUT_MS = 15000;  // 15 segundos
    protected static final int READ_TIMEOUT_MS = 30000;        // 30 segundos
    protected static final int BUFFER_SIZE = 8192;             // 8 KB
    protected static final String VERSION_PREFIX = "v_";
    protected static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 10) BlackHoleGlow/1.0";

    // =========================================================================
    // CAMPOS PROTEGIDOS
    // =========================================================================
    protected final Context context;
    protected final File resourceDir;
    protected final ExecutorService executor;
    protected final SharedPreferences versionPrefs;

    private volatile boolean isShutdown = false;

    // =========================================================================
    // MÉTODOS ABSTRACTOS - Implementar en subclases
    // =========================================================================

    /** Tag para logging (ej: "VideoDownload", "ImageDownload") */
    protected abstract String getTag();

    /** Nombre del directorio de cache (ej: "wallpaper_videos") */
    protected abstract String getDirectoryName();

    /** Nombre de SharedPreferences para versiones */
    protected abstract String getPreferencesName();

    /** Obtiene la URL remota desde el Config correspondiente */
    protected abstract String getRemoteUrl(String fileName);

    /** Obtiene el tamaño esperado desde el Config correspondiente */
    protected abstract long getExpectedSize(String fileName);

    /** Obtiene la versión desde el Config correspondiente */
    protected abstract int getResourceVersion(String fileName);

    /** Nombre del tipo de recurso para logs (ej: "video", "imagen", "modelo") */
    protected abstract String getResourceTypeName();

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    protected AbstractDownloadManager(Context context) {
        this.context = context.getApplicationContext();
        this.resourceDir = new File(context.getFilesDir(), getDirectoryName());
        if (!resourceDir.exists()) {
            resourceDir.mkdirs();
        }
        this.executor = Executors.newSingleThreadExecutor();
        this.versionPrefs = context.getSharedPreferences(getPreferencesName(), Context.MODE_PRIVATE);
        Log.d(getTag(), getResourceTypeName() + " storage dir: " + resourceDir.getAbsolutePath());
    }

    // =========================================================================
    // VERIFICACIÓN DE DISPONIBILIDAD
    // =========================================================================

    /**
     * Verifica si el recurso ya está descargado Y es la versión correcta.
     * Si la versión remota es mayor que la local, elimina y retorna false.
     */
    public boolean isResourceAvailable(String fileName) {
        File file = new File(resourceDir, fileName);
        if (!file.exists()) {
            return false;
        }

        // Verificar permisos de lectura
        if (!file.canRead()) {
            Log.e(getTag(), "Sin permiso de lectura: " + fileName);
            return false;
        }

        // Verificar VERSION - si hay nueva versión, eliminar archivo viejo
        int currentVersion = getResourceVersion(fileName);
        int storedVersion = versionPrefs.getInt(VERSION_PREFIX + fileName, 0);
        if (currentVersion > storedVersion) {
            Log.w(getTag(), "Nueva versión disponible: " + fileName +
                  " (local:" + storedVersion + " -> remota:" + currentVersion + ")");
            file.delete();
            return false;
        }

        // Verificar tamaño (descarga parcial?)
        long expectedSize = getExpectedSize(fileName);
        if (expectedSize > 0 && file.length() < expectedSize * 0.95) {
            Log.w(getTag(), "Recurso incompleto: " + fileName +
                  " (" + file.length() + "/" + expectedSize + ")");
            file.delete();
            versionPrefs.edit().remove(VERSION_PREFIX + fileName).apply();
            return false;
        }

        return true;
    }

    /**
     * Obtiene la ruta del recurso en cache.
     * @return null si no está disponible
     */
    public String getResourcePath(String fileName) {
        if (!isResourceAvailable(fileName)) {
            return null;
        }
        return new File(resourceDir, fileName).getAbsolutePath();
    }

    // =========================================================================
    // DESCARGA ASÍNCRONA
    // =========================================================================

    /**
     * Descarga un recurso en background.
     */
    public void downloadResource(String fileName, DownloadCallback callback) {
        if (isShutdown) {
            Log.w(getTag(), "Manager cerrado, no se puede descargar: " + fileName);
            if (callback != null) {
                callback.onError("Download manager cerrado");
            }
            return;
        }

        String remoteUrl = getRemoteUrl(fileName);
        if (remoteUrl == null) {
            Log.e(getTag(), getResourceTypeName() + " no configurado: " + fileName);
            if (callback != null) {
                callback.onError(getResourceTypeName() + " no configurado: " + fileName);
            }
            return;
        }

        executor.execute(() -> {
            try {
                downloadInternal(fileName, remoteUrl, callback);
            } catch (IOException e) {
                Log.e(getTag(), "Error de red descargando " + fileName + ": " + e.getMessage());
                if (callback != null) {
                    callback.onError("Error de red: " + e.getMessage());
                }
            } catch (Exception e) {
                Log.e(getTag(), "Error inesperado descargando " + fileName, e);
                if (callback != null) {
                    callback.onError("Error inesperado: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Lógica interna de descarga (Template Method).
     */
    private void downloadInternal(String fileName, String urlStr, DownloadCallback callback)
            throws IOException {
        Log.d(getTag(), "Descargando: " + urlStr);

        File tempFile = new File(resourceDir, fileName + ".tmp");
        File finalFile = new File(resourceDir, fileName);

        HttpURLConnection connection = null;
        InputStream input = null;
        FileOutputStream output = null;

        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error: " + responseCode);
            }

            long totalBytes = connection.getContentLength();
            Log.d(getTag(), "Tamaño total: " + formatSize(totalBytes));

            input = connection.getInputStream();
            output = new FileOutputStream(tempFile);

            byte[] buffer = new byte[BUFFER_SIZE];
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

            // CRÍTICO: flush + sync para garantizar escritura a disco
            output.flush();
            output.getFD().sync();
            output.close();
            output = null;

            // Renombrar a archivo final
            if (finalFile.exists()) {
                finalFile.delete();
            }
            if (!tempFile.renameTo(finalFile)) {
                throw new IOException("Error moviendo archivo temporal");
            }

            // Verificar que el archivo existe y tiene contenido
            if (!finalFile.exists() || finalFile.length() == 0) {
                throw new IOException("Archivo no disponible después de guardar");
            }

            // Guardar versión con commit() (síncrono)
            saveVersion(fileName);

            Log.d(getTag(), "Descarga completada: " + fileName);
            if (callback != null) {
                callback.onComplete(finalFile.getAbsolutePath());
            }

        } finally {
            closeQuietly(output);
            closeQuietly(input);
            if (connection != null) connection.disconnect();
            if (tempFile.exists()) tempFile.delete();
        }
    }

    // =========================================================================
    // DESCARGA SÍNCRONA
    // =========================================================================

    /**
     * Descarga un recurso de forma SÍNCRONA (bloquea el thread).
     * Usar solo desde background threads como ResourcePreloader.
     *
     * @return true si la descarga fue exitosa o el recurso ya existía
     */
    public boolean downloadResourceSync(String fileName, SyncProgressCallback callback) {
        // Si ya existe, retornar éxito
        if (isResourceAvailable(fileName)) {
            Log.d(getTag(), getResourceTypeName() + " ya disponible: " + fileName);
            return true;
        }

        String remoteUrl = getRemoteUrl(fileName);
        if (remoteUrl == null) {
            Log.e(getTag(), getResourceTypeName() + " no configurado: " + fileName);
            return false;
        }

        try {
            downloadSyncInternal(fileName, remoteUrl, callback);
            saveVersion(fileName);
            return true;
        } catch (IOException e) {
            Log.e(getTag(), "Error de red descargando " + fileName + ": " + e.getMessage());
            return false;
        } catch (Exception e) {
            Log.e(getTag(), "Error inesperado descargando " + fileName, e);
            return false;
        }
    }

    private void downloadSyncInternal(String fileName, String urlStr, SyncProgressCallback callback)
            throws IOException {
        Log.d(getTag(), "Descargando (sync): " + urlStr);

        File tempFile = new File(resourceDir, fileName + ".tmp");
        File finalFile = new File(resourceDir, fileName);

        HttpURLConnection connection = null;
        InputStream input = null;
        FileOutputStream output = null;

        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error: " + responseCode);
            }

            long totalBytes = connection.getContentLength();
            input = connection.getInputStream();
            output = new FileOutputStream(tempFile);

            byte[] buffer = new byte[BUFFER_SIZE];
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

            // CRÍTICO: flush + sync
            output.flush();
            output.getFD().sync();
            output.close();
            output = null;

            // Renombrar a archivo final
            if (finalFile.exists()) {
                finalFile.delete();
            }
            if (!tempFile.renameTo(finalFile)) {
                throw new IOException("Error moviendo archivo temporal");
            }

            // Verificar archivo
            if (!finalFile.exists() || finalFile.length() == 0) {
                throw new IOException("Archivo no disponible después de guardar");
            }

            Log.d(getTag(), "Descarga sync completada: " + fileName);

        } finally {
            closeQuietly(output);
            closeQuietly(input);
            if (connection != null) connection.disconnect();
            if (tempFile.exists()) tempFile.delete();
        }
    }

    // =========================================================================
    // GESTIÓN DE CACHE
    // =========================================================================

    /** Obtiene el directorio donde se guardan los recursos. */
    public File getResourceDirectory() {
        return resourceDir;
    }

    /** Elimina un recurso del storage y su versión. */
    public void deleteResource(String fileName) {
        File file = new File(resourceDir, fileName);
        if (file.exists()) {
            file.delete();
        }
        versionPrefs.edit().remove(VERSION_PREFIX + fileName).apply();
        Log.d(getTag(), "Eliminado: " + fileName);
    }

    /** Elimina todos los recursos del cache y sus versiones. */
    public void clearCache() {
        File[] files = resourceDir.listFiles();
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }
        versionPrefs.edit().clear().apply();
        Log.d(getTag(), "Cache limpiado");
    }

    /** Obtiene el tamaño total del cache en bytes. */
    public long getCacheSize() {
        long size = 0;
        File[] files = resourceDir.listFiles();
        if (files != null) {
            for (File f : files) {
                size += f.length();
            }
        }
        return size;
    }

    /** Obtiene el número de recursos en cache. */
    public int getCachedResourceCount() {
        File[] files = resourceDir.listFiles();
        return files != null ? files.length : 0;
    }

    /**
     * Elimina todos los recursos EXCEPTO los especificados.
     * Usado para limpiar recursos de escenas anteriores al cambiar de wallpaper.
     *
     * @param keepFiles Lista de nombres de archivo a mantener
     * @return Cantidad de bytes liberados
     */
    public long deleteAllExcept(List<String> keepFiles) {
        long freedBytes = 0;
        File[] files = resourceDir.listFiles();

        if (files == null || files.length == 0) {
            Log.d(getTag(), "No hay recursos para limpiar");
            return 0;
        }

        Set<String> keepSet = new HashSet<>(keepFiles);
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
                    Log.d(getTag(), "Eliminado: " + fileName + " (" + formatSize(fileSize) + ")");
                }
            }
        }

        Log.d(getTag(), "Limpieza completada: " + deletedCount + " archivos eliminados, " +
                   formatSize(freedBytes) + " liberados");
        return freedBytes;
    }

    // =========================================================================
    // SHUTDOWN - Liberar recursos
    // =========================================================================

    /**
     * Cierra el ExecutorService y libera recursos.
     * Llamar cuando la app se cierre o el manager ya no sea necesario.
     */
    public void shutdown() {
        if (isShutdown) return;
        isShutdown = true;

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                Log.w(getTag(), "Executor forzado a cerrar");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        Log.d(getTag(), "Manager cerrado correctamente");
    }

    // =========================================================================
    // UTILIDADES PRIVADAS
    // =========================================================================

    private void saveVersion(String fileName) {
        int version = getResourceVersion(fileName);
        // ⚡ FIX ANR: apply() en vez de commit() - no bloquea el thread
        versionPrefs.edit().putInt(VERSION_PREFIX + fileName, version).apply();
        Log.d(getTag(), "Versión " + version + " guardada para: " + fileName);
    }

    private void closeQuietly(java.io.Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                Log.w(getTag(), "Error cerrando stream: " + e.getMessage());
            }
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / 1024 / 1024) + " MB";
    }

    // =========================================================================
    // INTERFACES DE CALLBACK
    // =========================================================================

    /** Callback para progreso de descarga asíncrona. */
    public interface DownloadCallback {
        void onProgress(int percent, long downloadedBytes, long totalBytes);
        void onComplete(String filePath);
        void onError(String message);
    }

    /** Callback simple para progreso síncrono. */
    public interface SyncProgressCallback {
        void onProgress(int percent);
    }
}
