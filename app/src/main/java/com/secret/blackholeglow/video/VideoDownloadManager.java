package com.secret.blackholeglow.video;

import android.content.Context;
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
 * VideoDownloadManager - Descarga videos de Supabase y los guarda en cache local
 *
 * Uso:
 *   VideoDownloadManager manager = VideoDownloadManager.getInstance(context);
 *
 *   if (manager.isVideoAvailable("cielovolando.mp4")) {
 *       String path = manager.getVideoPath("cielovolando.mp4");
 *       // Usar path para reproducir
 *   } else {
 *       manager.downloadVideo("cielovolando.mp4", new DownloadCallback() {...});
 *   }
 */
public class VideoDownloadManager {
    private static final String TAG = "VideoDownload";
    private static final String VIDEO_DIR = "wallpaper_videos";

    private static VideoDownloadManager instance;
    private final Context context;
    private final File videoDir;
    private final ExecutorService executor;

    private VideoDownloadManager(Context context) {
        this.context = context.getApplicationContext();
        // Usar filesDir en lugar de videoDir para persistencia
        // Los videos no se eliminan automaticamente como en cache
        this.videoDir = new File(context.getFilesDir(), VIDEO_DIR);
        if (!videoDir.exists()) {
            videoDir.mkdirs();
        }
        this.executor = Executors.newSingleThreadExecutor();
        Log.d(TAG, "Video storage dir: " + videoDir.getAbsolutePath());
    }

    public static synchronized VideoDownloadManager getInstance(Context context) {
        if (instance == null) {
            instance = new VideoDownloadManager(context);
        }
        return instance;
    }

    /**
     * Verifica si el video ya esta descargado en cache
     */
    public boolean isVideoAvailable(String fileName) {
        File videoFile = new File(videoDir, fileName);
        if (!videoFile.exists()) {
            return false;
        }

        // Verificar tamano (descarga parcial?)
        long expectedSize = VideoConfig.getExpectedSize(fileName);
        if (expectedSize > 0 && videoFile.length() < expectedSize * 0.95) {
            Log.w(TAG, "Video incompleto: " + fileName +
                  " (" + videoFile.length() + "/" + expectedSize + ")");
            videoFile.delete();
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

            output.flush();
            output.close();
            output = null;

            // Renombrar a archivo final
            if (finalFile.exists()) {
                finalFile.delete();
            }
            if (!tempFile.renameTo(finalFile)) {
                throw new IOException("Error moviendo archivo temporal");
            }

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

            output.flush();
            output.close();
            output = null;

            // Renombrar a archivo final
            if (finalFile.exists()) {
                finalFile.delete();
            }
            if (!tempFile.renameTo(finalFile)) {
                throw new IOException("Error moviendo archivo temporal");
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
    // METODOS PARA IMAGENES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Verifica si una imagen ya esta descargada
     */
    public boolean isImageAvailable(String fileName) {
        File imageFile = new File(videoDir, fileName);
        if (!imageFile.exists()) {
            return false;
        }

        long expectedSize = VideoConfig.getImageExpectedSize(fileName);
        if (expectedSize > 0 && imageFile.length() < expectedSize * 0.95) {
            Log.w(TAG, "Imagen incompleta: " + fileName);
            imageFile.delete();
            return false;
        }

        return true;
    }

    /**
     * Obtiene la ruta de una imagen en storage local
     */
    public String getImagePath(String fileName) {
        if (!isImageAvailable(fileName)) {
            return null;
        }
        return new File(videoDir, fileName).getAbsolutePath();
    }

    /**
     * Descarga una imagen de forma sincrona
     */
    public boolean downloadImageSync(String fileName, SyncProgressCallback callback) {
        if (isImageAvailable(fileName)) {
            Log.d(TAG, "Imagen ya disponible: " + fileName);
            return true;
        }

        String remoteUrl = VideoConfig.getImageRemoteUrl(fileName);
        if (remoteUrl == null) {
            Log.e(TAG, "Imagen no configurada: " + fileName);
            return false;
        }

        try {
            downloadVideoSyncInternal(fileName, remoteUrl, callback);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error descargando imagen: " + e.getMessage());
            return false;
        }
    }

    /**
     * Elimina un video del storage
     */
    public void deleteVideo(String fileName) {
        File videoFile = new File(videoDir, fileName);
        if (videoFile.exists()) {
            videoFile.delete();
            Log.d(TAG, "Video eliminado: " + fileName);
        }
    }

    /**
     * Elimina todos los videos del cache
     */
    public void clearCache() {
        File[] files = videoDir.listFiles();
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }
        Log.d(TAG, "Cache limpiado");
    }

    /**
     * Obtiene el tamano total del cache en bytes
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
     * Callback para progreso de descarga
     */
    public interface DownloadCallback {
        void onProgress(int percent, long downloadedBytes, long totalBytes);
        void onComplete(String filePath);
        void onError(String message);
    }
}
