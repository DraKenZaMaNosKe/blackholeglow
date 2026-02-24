package com.secret.blackholeglow.diagnostic;

import android.content.Context;

import com.secret.blackholeglow.TextureManager.MemoryTier;
import com.secret.blackholeglow.WallpaperPreferences;
import com.secret.blackholeglow.core.DeviceProfile;
import com.secret.blackholeglow.image.ImageDownloadManager;
import com.secret.blackholeglow.model.ModelDownloadManager;
import com.secret.blackholeglow.models.SceneWeight;
import com.secret.blackholeglow.models.WallpaperItem;
import com.secret.blackholeglow.systems.WallpaperCatalog;
import com.secret.blackholeglow.video.VideoDownloadManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot de datos diagnosticos del dispositivo y la app.
 * Recolecta info de RAM, almacenamiento, wallpaper activo y compatibilidad.
 */
public class DiagnosticData {

    // Dispositivo
    public String deviceModel;
    public String androidVersion;
    public MemoryTier memoryTier;
    public long totalRamGB;
    public long availableRamMB;
    public int maxTextureDim;
    public int inSampleSize;

    // Almacenamiento
    public long imageCacheBytes;
    public int imageCacheCount;
    public long videoCacheBytes;
    public int videoCacheCount;
    public long modelCacheBytes;
    public int modelCacheCount;

    // Wallpaper activo
    public String activeWallpaperName;
    public SceneWeight activeWallpaperWeight;
    public CompatLevel activeWallpaperCompat;

    // Compatibilidad por wallpaper
    public List<WallpaperCompat> wallpaperCompatList = new ArrayList<>();

    // ═══════════════════════════════════════════════════════════════

    public enum CompatLevel {
        OPTIMAL,
        MODERATE,
        NOT_RECOMMENDED
    }

    public static class WallpaperCompat {
        public final String nombre;
        public final SceneWeight weight;
        public final CompatLevel level;

        public WallpaperCompat(String nombre, SceneWeight weight, CompatLevel level) {
            this.nombre = nombre;
            this.weight = weight;
            this.level = level;
        }
    }

    // ═══════════════════════════════════════════════════════════════

    public long getTotalCacheBytes() {
        return imageCacheBytes + videoCacheBytes + modelCacheBytes;
    }

    /**
     * Recolecta todos los datos diagnosticos en un snapshot.
     * Llamar desde thread background (getCacheSize() hace I/O).
     */
    public static DiagnosticData collect(Context context) {
        // Init defensivo por si el usuario abre diagnostico antes de setear wallpaper
        DeviceProfile.init(context);

        DiagnosticData d = new DiagnosticData();
        DeviceProfile profile = DeviceProfile.get();

        // --- Dispositivo ---
        d.deviceModel = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
        d.androidVersion = "Android " + android.os.Build.VERSION.RELEASE
                + " (API " + android.os.Build.VERSION.SDK_INT + ")";
        d.memoryTier = profile.getMemoryTier();
        d.totalRamGB = profile.getTotalRamGB();
        d.availableRamMB = profile.getAvailableRamMB();
        d.maxTextureDim = profile.getMaxTextureDimension();
        d.inSampleSize = profile.getDefaultInSampleSize();

        // --- Almacenamiento ---
        ImageDownloadManager imgMgr = ImageDownloadManager.getInstance(context);
        VideoDownloadManager vidMgr = VideoDownloadManager.getInstance(context);
        ModelDownloadManager mdlMgr = ModelDownloadManager.getInstance(context);

        d.imageCacheBytes = imgMgr.getCacheSize();
        d.imageCacheCount = imgMgr.getCachedImageCount();
        d.videoCacheBytes = vidMgr.getCacheSize();
        d.videoCacheCount = vidMgr.getCachedVideoCount();
        d.modelCacheBytes = mdlMgr.getCacheSize();
        d.modelCacheCount = mdlMgr.getCachedModelCount();

        // --- Wallpaper activo ---
        String selected = WallpaperPreferences.getInstance(context).getSelectedWallpaperSync();
        d.activeWallpaperName = selected;
        WallpaperItem activeItem = WallpaperCatalog.get().getByName(selected);
        if (activeItem != null) {
            d.activeWallpaperWeight = activeItem.getSceneWeight();
            d.activeWallpaperCompat = calculateCompat(d.memoryTier, activeItem.getSceneWeight());
        }

        // --- Compatibilidad de todos los wallpapers ---
        for (WallpaperItem item : WallpaperCatalog.get().getAll()) {
            CompatLevel level = calculateCompat(d.memoryTier, item.getSceneWeight());
            d.wallpaperCompatList.add(new WallpaperCompat(
                    item.getNombre(), item.getSceneWeight(), level));
        }

        return d;
    }

    /**
     * Calcula nivel de compatibilidad segun tier de RAM y peso de la escena.
     *
     * HIGH  -> todo OPTIMAL
     * MEDIUM -> LIGHT/MEDIUM=OPTIMAL, HEAVY=MODERATE
     * LOW   -> LIGHT=OPTIMAL, MEDIUM=MODERATE, HEAVY=NOT_RECOMMENDED
     */
    private static CompatLevel calculateCompat(MemoryTier tier, SceneWeight weight) {
        switch (tier) {
            case HIGH:
                return CompatLevel.OPTIMAL;
            case MEDIUM:
                return weight == SceneWeight.HEAVY ? CompatLevel.MODERATE : CompatLevel.OPTIMAL;
            case LOW:
                switch (weight) {
                    case LIGHT: return CompatLevel.OPTIMAL;
                    case MEDIUM: return CompatLevel.MODERATE;
                    case HEAVY: return CompatLevel.NOT_RECOMMENDED;
                }
            default:
                return CompatLevel.MODERATE;
        }
    }
}
