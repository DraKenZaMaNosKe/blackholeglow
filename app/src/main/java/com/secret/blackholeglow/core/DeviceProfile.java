package com.secret.blackholeglow.core;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

import com.secret.blackholeglow.TextureManager.MemoryTier;

/**
 * Singleton central de deteccion de RAM del dispositivo.
 * Reemplaza la logica duplicada en TextureManager, Frieza3D y DeathBeamFX.
 *
 * USO:
 *   DeviceProfile.init(context);  // Una vez en WallpaperDirector.initializeSharedSystems()
 *   DeviceProfile.get().isLowRam();
 *   DeviceProfile.get().getMemoryTier();
 */
public class DeviceProfile {
    private static final String TAG = "DeviceProfile";

    private static DeviceProfile instance;

    private MemoryTier memoryTier = MemoryTier.MEDIUM;
    private long totalRamBytes = 0;
    private int maxTextureDimension = 1536;
    private int defaultInSampleSize = 1;

    private ActivityManager activityManager;

    private DeviceProfile() {}

    /**
     * Inicializa el singleton con el contexto de la app.
     * Debe llamarse una sola vez al inicio (WallpaperDirector.initializeSharedSystems).
     */
    public static void init(Context context) {
        if (instance != null && instance.activityManager != null) return;
        instance = new DeviceProfile();
        instance.detect(context.getApplicationContext());
    }

    public static DeviceProfile get() {
        if (instance == null) {
            // Fallback seguro: si no se inicializo, usar MEDIUM por defecto
            // NO cachear la instancia rota — permitir que init() la reemplace después
            Log.w(TAG, "DeviceProfile.get() llamado sin init() - usando MEDIUM por defecto");
            return new DeviceProfile();
        }
        return instance;
    }

    /** Para testing */
    public static void reset() {
        instance = null;
    }

    private void detect(Context context) {
        try {
            activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memInfo);
            totalRamBytes = memInfo.totalMem;

            long totalGB = totalRamBytes / (1024L * 1024L * 1024L);

            if (totalGB < 4) {
                memoryTier = MemoryTier.LOW;
                maxTextureDimension = 1024;
                defaultInSampleSize = 2;
            } else if (totalGB <= 6) {
                memoryTier = MemoryTier.MEDIUM;
                maxTextureDimension = 1536;
                defaultInSampleSize = 1;
            } else {
                memoryTier = MemoryTier.HIGH;
                maxTextureDimension = 2048;
                defaultInSampleSize = 1;
            }

            Log.d(TAG, "DeviceProfile initialized: tier=" + memoryTier
                    + ", RAM=" + totalGB + "GB (" + totalRamBytes + " bytes)"
                    + ", maxDim=" + maxTextureDimension
                    + ", inSampleSize=" + defaultInSampleSize);
        } catch (Exception e) {
            Log.w(TAG, "RAM detection failed, using MEDIUM: " + e.getMessage());
            memoryTier = MemoryTier.MEDIUM;
            maxTextureDimension = 1536;
            defaultInSampleSize = 1;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // API PUBLICA
    // ═══════════════════════════════════════════════════════════════

    public MemoryTier getMemoryTier() {
        return memoryTier;
    }

    public boolean isLowRam() {
        return memoryTier == MemoryTier.LOW;
    }

    public long getTotalRamGB() {
        return totalRamBytes / (1024L * 1024L * 1024L);
    }

    /**
     * Memoria disponible actual en MB.
     * Lectura en vivo (no cacheada) para diagnostico OOM.
     */
    public long getAvailableRamMB() {
        if (activityManager == null) return -1;
        try {
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memInfo);
            return memInfo.availMem / (1024L * 1024L);
        } catch (Exception e) {
            return -1;
        }
    }

    public int getMaxTextureDimension() {
        return maxTextureDimension;
    }

    public int getDefaultInSampleSize() {
        return defaultInSampleSize;
    }
}
