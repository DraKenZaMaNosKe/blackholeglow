package com.secret.blackholeglow.video;

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
 * ║                    VideoDownloadManager                          ║
 * ║          Especialista en descarga de videos desde Supabase       ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  RESPONSABILIDADES:                                              ║
 * ║  • Descargar videos desde Supabase                               ║
 * ║  • Cachear videos en almacenamiento local                        ║
 * ║  • Verificar versiones para auto-actualización                   ║
 * ║  • Reportar progreso de descarga                                 ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  USO:                                                            ║
 * ║    VideoDownloadManager mgr = VideoDownloadManager.getInstance();║
 * ║    if (mgr.isVideoAvailable("cielovolando.mp4")) {               ║
 * ║        String path = mgr.getVideoPath("cielovolando.mp4");       ║
 * ║    } else {                                                      ║
 * ║        mgr.downloadVideo("cielovolando.mp4", callback);          ║
 * ║    }                                                             ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  NOTA: Para imágenes usar ImageDownloadManager                   ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class VideoDownloadManager {
    private static final String TAG = "VideoDownload";
    private static final String VIDEO_DIR = "wallpaper_videos";
    private static final String PREFS_NAME = "resource_versions";
    private static final String VERSION_PREFIX = "v_";

    private static VideoDownloadManager instance;
    private final Context context;
    private final File videoDir;
    private final ExecutorService executor;
    private final SharedPreferences versionPrefs;

    private VideoDownloadManager(Context context) {
        this.context = context.getApplicationContext();
        // Usar filesDir en lugar de videoDir para persistencia
        // Los videos no se eliminan automaticamente como en cache
        this.videoDir = new File(context.getFilesDir(), VIDEO_DIR);
        if (!videoDir.exists()) {
            videoDir.mkdirs();
        }
        this.executor = Executors.newSingleThreadExecutor();
        this.versionPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Log.d(TAG, "Video storage dir: " + videoDir.getAbsolutePath());
    }

    public static synchronized VideoDownloadManager getInstance(Context context) {
        if (instance == null) {
            instance = new VideoDownloadManager(context);
        }
        return instance;
    }

    /**
     * Verifica si el video ya esta descargado en cache Y es la version correcta
     * Si la version en VideoConfig es mayor que la descargada, elimina y retorna false
     */
    public boolean isVideoAvailable(String fileName) {
        File videoFile = new File(videoDir, fileName);
        if (!videoFile.exists()) {
            return false;
        }

        // Verificar VERSION - si hay nueva version, eliminar archivo viejo
        int currentVersion = VideoConfig.getVideoVersion(fileName);
        int storedVersion = versionPrefs.getInt(VERSION_PREFIX + fileName, 0);
        if (currentVersion > storedVersion) {
            Log.w(TAG, "🔄 Nueva version disponible: " + fileName +
                  " (local:" + storedVersion + " -> remota:" + currentVersion + ")");
            videoFile.delete();
            return false;
        }

        // Verificar tamano (descarga parcial?)
        long expectedSize = VideoConfig.getExpectedSize(fileName);
        if (expectedSize > 0 && videoFile.length() < expectedSize * 0.95) {
            Log.w(TAG, "Video incompleto: " + fileName +
                  " (" + videoFile.length() + "/" + expectedSize + ")");
            videoFile.delete();
            versionPrefs.edit().remove(VERSION_PREFIX + fileName).apply();
            return false;
        }

        return true;
    }

    /**
     * Obtiene la ruta del video en cache
     * @return null si no esta disponible
     */
    public String getVideoPath(String fileName) {
        if (!isVideoAvailable(fileName)) {
            return null;
        }
        return new File(videoDir, fileName).getAbsolutePath();
    }

    /**
     * Descarga un video en background
     */
    public void downloadVideo(String fileName, DownloadCallback callback) {
        String remoteUrl = VideoConfig.getRemoteUrl(fileName);
        if (remoteUrl == null) {
            Log.e(TAG, "Video no configurado: " + fileName);
            if (callback != null) {
                callback.onError("Video no configurado: " + fileName);
            }
            return;
        }

        executor.execute(() -> {
            try {
                downloadVideoInternal(fileName, remoteUrl, callback);
            } catch (Exception e) {
                Log.e(TAG, "Error descargando: " + e.getMessage());
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    private void downloadVideoInternal(String fileName, String urlStr, DownloadCallback callback)
            throws IOException {
        Log.d(TAG, "Descargando: " + urlStr);

        File tempFile = new File(videoDir, fileName + ".tmp");
        File finalFile = new File(videoDir, fileName);

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
            Log.d(TAG, "Tamano total: " + (totalBytes / 1024 / 1024) + " MB");

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

            // ⚠️ CRÍTICO: Guardar version con commit() (síncrono) NO apply() (async)
            // Si no se guarda, isVideoAvailable() detectará "nueva versión" y eliminará el archivo
            int version = VideoConfig.getVideoVersion(fileName);
            boolean saved = versionPrefs.edit().putInt(VERSION_PREFIX + fileName, version).commit();
            if (!saved) {
                Log.e(TAG, "⚠️ Error guardando versión en preferencias");
            }
            Log.d(TAG, "✓ Version " + version + " guardada (sync) para: " + fileName);

            Log.d(TAG, "Descarga completada: " + fileName);
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

    /**
     * Descarga un video de forma SINCRONA (bloquea el thread)
     * Usar solo desde background threads como ResourcePreloader
     *
     * @return true si la descarga fue exitosa o el video ya existia
     */
    public boolean downloadVideoSync(String fileName, SyncProgressCallback callback) {
        // Si ya existe, retornar exito
        if (isVideoAvailable(fileName)) {
            Log.d(TAG, "Video ya disponible: " + fileName);
            return true;
        }

        String remoteUrl = VideoConfig.getRemoteUrl(fileName);
        if (remoteUrl == null) {
            Log.e(TAG, "Video no configurado: " + fileName);
            return false;
        }

        try {
            downloadVideoSyncInternal(fileName, remoteUrl, callback);
            // ⚡ Guardar version con commit() (síncrono) después de descarga exitosa
            int version = VideoConfig.getVideoVersion(fileName);
            boolean saved = versionPrefs.edit().putInt(VERSION_PREFIX + fileName, version).commit();
            if (!saved) {
                Log.e(TAG, "⚠️ Error guardando versión en preferencias");
            }
            Log.d(TAG, "✓ Version " + version + " guardada (sync) para: " + fileName);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error descargando: " + e.getMessage());
            return false;
        }
    }

    private void downloadVideoSyncInternal(String fileName, String urlStr, SyncProgressCallback callback)
            throws IOException {
        Log.d(TAG, "Descargando (sync): " + urlStr);

        File tempFile = new File(videoDir, fileName + ".tmp");
        File finalFile = new File(videoDir, fileName);

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

            Log.d(TAG, "Descarga sync completada: " + fileName);

        } finally {
            if (output != null) try { output.close(); } catch (Exception ignored) {}
            if (input != null) try { input.close(); } catch (Exception ignored) {}
            if (connection != null) connection.disconnect();
            if (tempFile.exists()) tempFile.delete();
        }
    }

    /**
     * Callback simple para progreso sincrono
     */
    public interface SyncProgressCallback {
        void onProgress(int percent);
    }

    /**
     * Obtiene el directorio donde se guardan los videos
     */
    public File getVideoDirectory() {
        return videoDir;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GESTIÓN DE CACHE
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Elimina un video del storage y su versión.
     */
    public void deleteVideo(String fileName) {
        File file = new File(videoDir, fileName);
        if (file.exists()) {
            file.delete();
        }
        versionPrefs.edit().remove(VERSION_PREFIX + fileName).apply();
        Log.d(TAG, "🗑️ Video eliminado: " + fileName);
    }

    /**
     * Elimina todos los videos del cache y sus versiones.
     */
    public void clearCache() {
        File[] files = videoDir.listFiles();
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }
        versionPrefs.edit().clear().apply();
        Log.d(TAG, "🗑️ Cache de videos limpiado");
    }

    /**
     * Obtiene el tamaño total del cache de videos en bytes.
     */
    public long getCacheSize() {
        long size = 0;
        File[] files = videoDir.listFiles();
        if (files != null) {
            for (File f : files) {
                size += f.length();
            }
        }
        return size;
    }

    /**
     * Obtiene el número de videos en cache.
     */
    public int getCachedVideoCount() {
        File[] files = videoDir.listFiles();
        return files != null ? files.length : 0;
    }

    /**
     * 🗑️ Elimina todos los videos EXCEPTO los especificados.
     * Usado para limpiar recursos de escenas anteriores al cambiar de wallpaper.
     *
     * @param keepFiles Lista de nombres de archivo a mantener (ej: "thehouse.mp4", "gokuHD.mp4")
     * @return Cantidad de bytes liberados
     */
    public long deleteAllExcept(java.util.List<String> keepFiles) {
        long freedBytes = 0;
        File[] files = videoDir.listFiles();

        if (files == null || files.length == 0) {
            Log.d(TAG, "🗑️ No hay videos para limpiar");
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
                    // También eliminar la versión guardada
                    versionPrefs.edit().remove(VERSION_PREFIX + fileName).apply();
                    Log.d(TAG, "🗑️ Eliminado: " + fileName + " (" + (fileSize / 1024 / 1024) + " MB)");
                }
            }
        }

        Log.d(TAG, "🧹 Limpieza completada: " + deletedCount + " videos eliminados, " +
                   (freedBytes / 1024 / 1024) + " MB liberados");
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
}
