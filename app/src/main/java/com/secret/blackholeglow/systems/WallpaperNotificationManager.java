package com.secret.blackholeglow.systems;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.WallpaperPreferences;
import com.secret.blackholeglow.activities.MainActivity;
import com.secret.blackholeglow.models.WallpaperItem;
import com.secret.blackholeglow.video.VideoDownloadManager;
import com.secret.blackholeglow.image.ImageDownloadManager;
import com.secret.blackholeglow.model.ModelDownloadManager;

import java.io.File;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║              WallpaperNotificationManager                        ║
 * ║          Notificaciones del sistema para el usuario              ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  NOTIFICACIONES:                                                 ║
 * ║  • Recursos listos: cantidad y tamaño total                     ║
 * ║  • Wallpaper instalado: confirmación                            ║
 * ║  • NO revela nombres de archivos ni extensiones                 ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class WallpaperNotificationManager {
    private static final String TAG = "WallpaperNotif";
    private static final String CHANNEL_ID = "orbix_wallpaper_status";
    private static final int NOTIF_ID_RESOURCES_READY = 1001;
    private static final int NOTIF_ID_INSTALLED = 1002;

    private static WallpaperNotificationManager instance;
    private final Context context;
    private final NotificationManager notificationManager;

    private WallpaperNotificationManager(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    public static synchronized WallpaperNotificationManager getInstance(Context context) {
        if (instance == null) {
            instance = new WallpaperNotificationManager(context);
        }
        return instance;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Wallpaper Status",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Wallpaper installation status");
            notificationManager.createNotificationChannel(channel);
        }
    }

    private boolean canShowNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context,
                    android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private PendingIntent getOpenAppIntent() {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024f);
        return String.format("%.1f MB", bytes / (1024f * 1024f));
    }

    // ═══════════════════════════════════════════════════════════════
    // NOTIFICACIÓN: Recursos listos para un wallpaper
    // ═══════════════════════════════════════════════════════════════

    /**
     * Notifica que los recursos de un wallpaper están listos.
     * Muestra solo cantidad de recursos y tamaño total.
     * NO revela nombres de archivos, extensiones ni tipos.
     */
    public void notifyResourcesReady(String sceneName, String displayName) {
        if (!canShowNotifications()) {
            Log.w(TAG, "Sin permiso de notificaciones");
            return;
        }

        // Contar recursos sin revelar detalles técnicos
        long totalSize = 0;
        int resourceCount = 0;

        // Escanear videos
        VideoDownloadManager videoMgr = VideoDownloadManager.getInstance(context);
        File videoDir = videoMgr.getResourceDirectory();
        List<String> videos = getSceneVideos(sceneName);
        for (String video : videos) {
            File f = new File(videoDir, video);
            if (f.exists()) {
                totalSize += f.length();
                resourceCount++;
            }
        }

        // Escanear imágenes
        ImageDownloadManager imgMgr = ImageDownloadManager.getInstance(context);
        File imgDir = imgMgr.getResourceDirectory();
        List<String> images = getSceneImages(sceneName);
        for (String img : images) {
            File f = new File(imgDir, img);
            if (f.exists()) {
                totalSize += f.length();
                resourceCount++;
            }
        }

        // Escanear modelos
        ModelDownloadManager modelMgr = ModelDownloadManager.getInstance(context);
        File modelDir = modelMgr.getResourceDirectory();
        List<String> models = getSceneModels(sceneName);
        for (String model : models) {
            File f = new File(modelDir, model);
            if (f.exists()) {
                totalSize += f.length();
                resourceCount++;
            }
        }

        if (resourceCount == 0) {
            Log.d(TAG, "Sin recursos descargados para: " + sceneName);
            return;
        }

        String title = displayName + " ready";
        String summary = resourceCount + " resources downloaded (" + formatSize(totalSize) + ")";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(summary)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(getOpenAppIntent())
                .setAutoCancel(true);

        notificationManager.notify(NOTIF_ID_RESOURCES_READY, builder.build());
        Log.d(TAG, "Notificación: " + title + " - " + summary);
    }

    // ═══════════════════════════════════════════════════════════════
    // VERIFICACIÓN: Wallpaper activo del sistema
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verifica contra el sistema operativo cuál wallpaper está realmente activo.
     * Consulta WallpaperManager + SharedPreferences para obtener info completa.
     */
    public InstalledWallpaperInfo verifyInstalledWallpaper() {
        boolean isOurWallpaperActive = false;
        try {
            WallpaperManager wm = WallpaperManager.getInstance(context);
            WallpaperInfo info = wm.getWallpaperInfo();
            if (info != null) {
                isOurWallpaperActive = info.getPackageName().equals(context.getPackageName());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error verificando wallpaper activo: " + e.getMessage());
        }

        if (!isOurWallpaperActive) {
            Log.d(TAG, "Nuestro wallpaper NO está activo en el sistema");
            return new InstalledWallpaperInfo(false, null, null);
        }

        // Nuestro servicio está activo - obtener qué escena está seleccionada
        String sceneName = WallpaperPreferences.getInstance(context).getSelectedWallpaperSync();
        String displayName = sceneName;

        // Intentar obtener el displayName del catálogo
        try {
            WallpaperItem item = WallpaperCatalog.get().getBySceneName(sceneName);
            if (item != null) {
                displayName = item.getNombre();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error obteniendo displayName del catálogo: " + e.getMessage());
        }

        Log.d(TAG, "Wallpaper activo verificado: " + sceneName + " (" + displayName + ")");
        return new InstalledWallpaperInfo(true, sceneName, displayName);
    }

    /**
     * Información del wallpaper actualmente instalado.
     */
    public static class InstalledWallpaperInfo {
        public final boolean isOurWallpaperActive;
        public final String sceneName;
        public final String displayName;

        public InstalledWallpaperInfo(boolean isOurWallpaperActive, String sceneName, String displayName) {
            this.isOurWallpaperActive = isOurWallpaperActive;
            this.sceneName = sceneName;
            this.displayName = displayName;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // NOTIFICACIÓN: Wallpaper instalado
    // ═══════════════════════════════════════════════════════════════

    /**
     * Notifica que un wallpaper fue instalado exitosamente.
     * Verifica contra el sistema antes de notificar para evitar notificaciones engañosas.
     */
    public void notifyWallpaperInstalled(String sceneName, String displayName) {
        if (!canShowNotifications()) {
            Log.w(TAG, "Sin permiso de notificaciones");
            return;
        }

        // Verificar que nuestro wallpaper realmente está activo en el sistema
        InstalledWallpaperInfo info = verifyInstalledWallpaper();
        if (!info.isOurWallpaperActive) {
            Log.w(TAG, "No se envía notificación: nuestro wallpaper no está activo en el sistema");
            return;
        }

        String title = displayName + " activated";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText("Wallpaper installed successfully")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(getOpenAppIntent())
                .setAutoCancel(true);

        notificationManager.notify(NOTIF_ID_INSTALLED, builder.build());
        Log.d(TAG, "Notificación: " + title);
    }

    // ═══════════════════════════════════════════════════════════════
    // SCANNER DE RECURSOS (sin detalles técnicos)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Escanea todas las carpetas de recursos y genera un reporte genérico.
     * NO incluye nombres de archivos ni extensiones.
     */
    public String scanAllResources() {
        long grandTotal = 0;
        int grandCount = 0;

        // Videos
        File videoDir = new File(context.getFilesDir(), "wallpaper_videos");
        if (videoDir.exists()) {
            File[] files = videoDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!f.getName().endsWith(".tmp")) {
                        grandTotal += f.length();
                        grandCount++;
                    }
                }
            }
        }

        // Imágenes
        File imgDir = new File(context.getFilesDir(), "wallpaper_images");
        if (imgDir.exists()) {
            File[] files = imgDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!f.getName().endsWith(".tmp")) {
                        grandTotal += f.length();
                        grandCount++;
                    }
                }
            }
        }

        // Modelos
        File modelDir = new File(context.getFilesDir(), "wallpaper_models");
        if (modelDir.exists()) {
            File[] files = modelDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!f.getName().endsWith(".tmp")) {
                        grandTotal += f.length();
                        grandCount++;
                    }
                }
            }
        }

        return grandCount + " resources on device (" + formatSize(grandTotal) + ")";
    }

    // ═══════════════════════════════════════════════════════════════
    // DATOS DE ESCENAS
    // ═══════════════════════════════════════════════════════════════

    private List<String> getSceneVideos(String sceneName) {
        return com.secret.blackholeglow.core.ResourcePreloader.getRequiredVideos(sceneName);
    }

    private List<String> getSceneImages(String sceneName) {
        return com.secret.blackholeglow.core.ResourcePreloader.getRequiredImages(sceneName);
    }

    private List<String> getSceneModels(String sceneName) {
        return com.secret.blackholeglow.core.ResourcePreloader.getRequiredModels(sceneName);
    }
}
