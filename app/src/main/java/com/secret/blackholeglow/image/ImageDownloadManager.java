package com.secret.blackholeglow.image;

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
 * ║                    ImageDownloadManager                          ║
 * ║         Especialista en descarga de imágenes desde Supabase      ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  RESPONSABILIDADES:                                              ║
 * ║  • Descargar imágenes desde Supabase                             ║
 * ║  • Cachear imágenes en almacenamiento local                      ║
 * ║  • Verificar versiones para auto-actualización                   ║
 * ║  • Reportar progreso de descarga                                 ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  USO:                                                            ║
 * ║    ImageDownloadManager mgr = ImageDownloadManager.getInstance();║
 * ║    if (mgr.isImageAvailable("seiya_solo.png")) {                 ║
 * ║        String path = mgr.getImagePath("seiya_solo.png");         ║
 * ║    } else {                                                      ║
 * ║        mgr.downloadImageSync("seiya_solo.png", callback);        ║
 * ║    }                                                             ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class ImageDownloadManager {
    private static final String TAG = "ImageDownload";
    private static final String IMAGE_DIR = "wallpaper_images";
    private static final String PREFS_NAME = "image_versions";
    private static final String VERSION_PREFIX = "v_";

    private static ImageDownloadManager instance;
    private final Context context;
    private final File imageDir;
    private final ExecutorService executor;
    private final SharedPreferences versionPrefs;

    private ImageDownloadManager(Context context) {
        this.context = context.getApplicationContext();
        this.imageDir = new File(context.getFilesDir(), IMAGE_DIR);
        if (!imageDir.exists()) {
            imageDir.mkdirs();
        }
        this.executor = Executors.newSingleThreadExecutor();
        this.versionPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Log.d(TAG, "🖼️ Image storage dir: " + imageDir.getAbsolutePath());
    }

    public static synchronized ImageDownloadManager getInstance(Context context) {
        if (instance == null) {
            instance = new ImageDownloadManager(context);
        }
        return instance;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // VERIFICACIÓN DE DISPONIBILIDAD
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Verifica si una imagen ya está descargada Y es la versión correcta.
     * Si la versión en ImageConfig es mayor que la descargada, elimina y retorna false.
     */
    public boolean isImageAvailable(String fileName) {
        File imageFile = new File(imageDir, fileName);
        if (!imageFile.exists()) {
            return false;
        }

        // Verificar VERSION - si hay nueva versión, eliminar archivo viejo
        int currentVersion = ImageConfig.getImageVersion(fileName);
        int storedVersion = versionPrefs.getInt(VERSION_PREFIX + fileName, 0);
        if (currentVersion > storedVersion) {
            Log.w(TAG, "🔄 Nueva versión de imagen: " + fileName +
                  " (local:" + storedVersion + " -> remota:" + currentVersion + ")");
            imageFile.delete();
            return false;
        }

        // Verificar tamaño (descarga parcial?)
        long expectedSize = ImageConfig.getExpectedSize(fileName);
        if (expectedSize > 0 && imageFile.length() < expectedSize * 0.95) {
            Log.w(TAG, "⚠️ Imagen incompleta: " + fileName +
                  " (" + imageFile.length() + "/" + expectedSize + ")");
            imageFile.delete();
            versionPrefs.edit().remove(VERSION_PREFIX + fileName).apply();
            return false;
        }

        return true;
    }

    /**
     * Obtiene la ruta de una imagen en storage local.
     * @return null si no está disponible
     */
    public String getImagePath(String fileName) {
        if (!isImageAvailable(fileName)) {
            return null;
        }
        return new File(imageDir, fileName).getAbsolutePath();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DESCARGA ASÍNCRONA
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Descarga una imagen en background.
     */
    public void downloadImage(String fileName, DownloadCallback callback) {
        String remoteUrl = ImageConfig.getRemoteUrl(fileName);
        if (remoteUrl == null) {
            Log.e(TAG, "❌ Imagen no configurada: " + fileName);
            if (callback != null) {
                callback.onError("Imagen no configurada: " + fileName);
            }
            return;
        }

        executor.execute(() -> {
            try {
                downloadImageInternal(fileName, remoteUrl, callback);
            } catch (Exception e) {
                Log.e(TAG, "❌ Error descargando: " + e.getMessage());
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    private void downloadImageInternal(String fileName, String urlStr, DownloadCallback callback)
            throws IOException {
        Log.d(TAG, "📥 Descargando imagen: " + urlStr);

        File tempFile = new File(imageDir, fileName + ".tmp");
        File finalFile = new File(imageDir, fileName);

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
            int version = ImageConfig.getImageVersion(fileName);
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
     * Descarga una imagen de forma SÍNCRONA (bloquea el thread).
     * Usar solo desde background threads como ResourcePreloader.
     *
     * @return true si la descarga fue exitosa o la imagen ya existía
     */
    public boolean downloadImageSync(String fileName, SyncProgressCallback callback) {
        // Si ya existe, retornar éxito
        if (isImageAvailable(fileName)) {
            Log.d(TAG, "✅ Imagen ya disponible: " + fileName);
            return true;
        }

        String remoteUrl = ImageConfig.getRemoteUrl(fileName);
        if (remoteUrl == null) {
            Log.e(TAG, "❌ Imagen no configurada: " + fileName);
            return false;
        }

        try {
            downloadImageSyncInternal(fileName, remoteUrl, callback);
            // ⚡ Guardar versión con commit() (síncrono) después de descarga exitosa
            int version = ImageConfig.getImageVersion(fileName);
            boolean saved = versionPrefs.edit().putInt(VERSION_PREFIX + fileName, version).commit();
            if (!saved) {
                Log.e(TAG, "⚠️ Error guardando versión en preferencias");
            }
            Log.d(TAG, "✅ Versión " + version + " guardada (sync) para: " + fileName);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "❌ Error descargando imagen: " + e.getMessage());
            return false;
        }
    }

    private void downloadImageSyncInternal(String fileName, String urlStr, SyncProgressCallback callback)
            throws IOException {
        Log.d(TAG, "📥 Descargando (sync): " + urlStr);

        File tempFile = new File(imageDir, fileName + ".tmp");
        File finalFile = new File(imageDir, fileName);

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
     * Obtiene el directorio donde se guardan las imágenes.
     */
    public File getImageDirectory() {
        return imageDir;
    }

    /**
     * Elimina una imagen del storage y su versión.
     */
    public void deleteImage(String fileName) {
        File file = new File(imageDir, fileName);
        if (file.exists()) {
            file.delete();
        }
        versionPrefs.edit().remove(VERSION_PREFIX + fileName).apply();
        Log.d(TAG, "🗑️ Imagen eliminada: " + fileName);
    }

    /**
     * Elimina todas las imágenes del cache y sus versiones.
     */
    public void clearCache() {
        File[] files = imageDir.listFiles();
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }
        versionPrefs.edit().clear().apply();
        Log.d(TAG, "🗑️ Cache de imágenes limpiado");
    }

    /**
     * Obtiene el tamaño total del cache de imágenes en bytes.
     */
    public long getCacheSize() {
        long size = 0;
        File[] files = imageDir.listFiles();
        if (files != null) {
            for (File f : files) {
                size += f.length();
            }
        }
        return size;
    }

    /**
     * Obtiene el número de imágenes en cache.
     */
    public int getCachedImageCount() {
        File[] files = imageDir.listFiles();
        return files != null ? files.length : 0;
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
