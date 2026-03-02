package com.secret.blackholeglow.core;

import android.content.Context;
import android.os.StatFs;
import android.util.Log;

import com.secret.blackholeglow.TextureManager.MemoryTier;
import com.secret.blackholeglow.image.ImageDownloadManager;
import com.secret.blackholeglow.model.ModelDownloadManager;
import com.secret.blackholeglow.models.SceneWeight;
import com.secret.blackholeglow.models.WallpaperItem;
import com.secret.blackholeglow.systems.WallpaperCatalog;
import com.secret.blackholeglow.video.VideoDownloadManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pre-Flight Check - Verificacion rapida antes de lanzar un wallpaper.
 *
 * Verifica memoria, disco, carpetas y pronostico de rendimiento
 * ANTES de proceder a la pantalla de carga. Si algo falla,
 * informa al usuario con acciones claras sin crashear la app.
 *
 * Tiempo objetivo: <50ms, sin red.
 */
public class PreFlightCheck {
    private static final String TAG = "PreFlightCheck";

    // ═══════════════════════════════════════════════════════════════
    // INNER TYPES
    // ═══════════════════════════════════════════════════════════════

    public enum Severity {
        OK,
        WARNING,
        BLOCKING
    }

    public static class Issue {
        public final String titulo;
        public final String detalle;
        public final Severity severity;
        public final String accionSugerida;

        public Issue(String titulo, String detalle, Severity severity, String accionSugerida) {
            this.titulo = titulo;
            this.detalle = detalle;
            this.severity = severity;
            this.accionSugerida = accionSugerida;
        }
    }

    public static class Result {
        public final boolean canProceed;
        public final List<Issue> issues;

        Result(boolean canProceed, List<Issue> issues) {
            this.canProceed = canProceed;
            this.issues = Collections.unmodifiableList(issues);
        }

        /** Sin issues = limpio, proceder directo */
        public boolean isClean() {
            return issues.isEmpty();
        }

        /** Issues que bloquean (no dejar continuar) */
        public List<Issue> getBlockers() {
            List<Issue> blockers = new ArrayList<>();
            for (Issue issue : issues) {
                if (issue.severity == Severity.BLOCKING) blockers.add(issue);
            }
            return blockers;
        }

        /** Issues de advertencia (dejar continuar con warning) */
        public List<Issue> getWarnings() {
            List<Issue> warnings = new ArrayList<>();
            for (Issue issue : issues) {
                if (issue.severity == Severity.WARNING) warnings.add(issue);
            }
            return warnings;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // API PUBLICA
    // ═══════════════════════════════════════════════════════════════

    /**
     * Ejecuta los 5 checks pre-vuelo para la escena indicada.
     *
     * @param context   Contexto Android
     * @param sceneName Nombre de la escena (ej: "GOKU", "ABYSSIA")
     * @return Result con canProceed e issues encontrados
     */
    public static Result run(Context context, String sceneName) {
        long start = System.currentTimeMillis();
        List<Issue> issues = new ArrayList<>();

        // Asegurar que DeviceProfile esté inicializado (puede llamarse antes que WallpaperDirector)
        DeviceProfile.init(context);

        // Obtener info de la escena del catalogo
        WallpaperItem item = WallpaperCatalog.get().getBySceneName(sceneName);
        SceneWeight weight = item != null ? item.getSceneWeight() : SceneWeight.MEDIUM;

        // 1. RAM disponible
        checkRam(issues, weight);

        // 2. Recursos descargados
        checkResources(context, issues, sceneName);

        // 3. Directorio de storage
        checkStorageDir(context, issues);

        // 4. Espacio en disco
        checkDiskSpace(context, issues);

        // 5. Pronostico de rendimiento
        checkPerformanceForecast(issues, weight);

        boolean canProceed = true;
        for (Issue issue : issues) {
            if (issue.severity == Severity.BLOCKING) {
                canProceed = false;
                break;
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        Log.d(TAG, "Pre-flight para '" + sceneName + "': " + issues.size()
                + " issues, canProceed=" + canProceed + " (" + elapsed + "ms)");

        return new Result(canProceed, issues);
    }

    // ═══════════════════════════════════════════════════════════════
    // CHECKS INDIVIDUALES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Check 1: RAM disponible.
     * Umbrales dependen del peso de la escena.
     */
    private static void checkRam(List<Issue> issues, SceneWeight weight) {
        DeviceProfile dp = DeviceProfile.get();
        long availMB = dp.getAvailableRamMB();
        if (availMB < 0) return; // No se pudo leer

        long blockingThreshold;
        long warningThreshold;

        switch (weight) {
            case HEAVY:
                blockingThreshold = 150;
                warningThreshold = 250;
                break;
            case MEDIUM:
                blockingThreshold = 100;
                warningThreshold = 180;
                break;
            default: // LIGHT
                blockingThreshold = 50;
                warningThreshold = 100;
                break;
        }

        if (availMB < blockingThreshold) {
            issues.add(new Issue(
                    "Very low memory",
                    "Only " + availMB + " MB free. This wallpaper needs at least " + blockingThreshold + " MB.",
                    Severity.BLOCKING,
                    "Close other apps and try again."
            ));
        } else if (availMB < warningThreshold) {
            issues.add(new Issue(
                    "Low memory",
                    availMB + " MB free. There may be lag or unexpected crashes.",
                    Severity.WARNING,
                    "Close background apps for better performance."
            ));
        }
    }

    /**
     * Check 2: Recursos descargados (videos, imagenes, modelos).
     * BLOCKING nunca (se descargan en la pantalla de carga).
     * WARNING si faltan recursos que se necesitan descargar.
     */
    private static void checkResources(Context context, List<Issue> issues, String sceneName) {
        if (sceneName == null) return;

        List<String> missingTypes = new ArrayList<>();

        // Videos
        List<String> videos = ResourcePreloader.getRequiredVideos(sceneName);
        if (!videos.isEmpty()) {
            VideoDownloadManager videoDM = VideoDownloadManager.getInstance(context);
            for (String video : videos) {
                if (!videoDM.isVideoAvailable(video)) {
                    missingTypes.add("video");
                    break;
                }
            }
        }

        // Imagenes
        List<String> images = ResourcePreloader.getRequiredImages(sceneName);
        if (!images.isEmpty()) {
            ImageDownloadManager imageDM = ImageDownloadManager.getInstance(context);
            for (String image : images) {
                if (!imageDM.isImageAvailable(image)) {
                    missingTypes.add("imagenes");
                    break;
                }
            }
        }

        // Modelos
        List<String> models = ResourcePreloader.getRequiredModels(sceneName);
        if (!models.isEmpty()) {
            ModelDownloadManager modelDM = ModelDownloadManager.getInstance(context);
            for (String model : models) {
                if (!modelDM.isModelAvailable(model)) {
                    missingTypes.add("modelos 3D");
                    break;
                }
            }
        }

        if (!missingTypes.isEmpty()) {
            String missing = String.join(", ", missingTypes);
            issues.add(new Issue(
                    "Download required",
                    "Missing resources (" + missing + "). They will be downloaded automatically.",
                    Severity.WARNING,
                    "Make sure you have an internet connection."
            ));
        }
    }

    /**
     * Check 3: Directorio de storage accesible y writable.
     */
    private static void checkStorageDir(Context context, List<Issue> issues) {
        File filesDir = context.getFilesDir();
        if (filesDir == null || !filesDir.exists() || !filesDir.canWrite()) {
            issues.add(new Issue(
                    "Storage unavailable",
                    "Cannot access the app data directory.",
                    Severity.BLOCKING,
                    "Restart the app or check storage permissions."
            ));
        }
    }

    /**
     * Check 4: Espacio en disco libre.
     */
    private static void checkDiskSpace(Context context, List<Issue> issues) {
        try {
            File filesDir = context.getFilesDir();
            if (filesDir == null) return; // Ya cubierto por checkStorageDir

            StatFs stat = new StatFs(filesDir.getAbsolutePath());
            long freeMB = (stat.getAvailableBlocksLong() * stat.getBlockSizeLong()) / (1024L * 1024L);

            if (freeMB < 50) {
                issues.add(new Issue(
                        "Very low disk space",
                        "Only " + freeMB + " MB free on device.",
                        Severity.BLOCKING,
                        "Free up space by deleting unused photos, videos or apps."
                ));
            } else if (freeMB < 100) {
                issues.add(new Issue(
                        "Low disk space",
                        freeMB + " MB free. Some wallpapers may need more.",
                        Severity.WARNING,
                        "Consider freeing up space for better performance."
                ));
            }
        } catch (Exception e) {
            Log.w(TAG, "Error verificando espacio en disco: " + e.getMessage());
        }
    }

    /**
     * Check 5: Pronostico de rendimiento.
     * LOW RAM + HEAVY/MEDIUM = warning (usuario decide).
     * Nunca es BLOCKING (el usuario puede intentar).
     */
    private static void checkPerformanceForecast(List<Issue> issues, SceneWeight weight) {
        DeviceProfile dp = DeviceProfile.get();
        MemoryTier tier = dp.getMemoryTier();

        if (tier == MemoryTier.LOW && weight == SceneWeight.HEAVY) {
            issues.add(new Issue(
                    "Limited performance",
                    "Your device has low RAM and this wallpaper is very demanding. There may be lag.",
                    Severity.WARNING,
                    "Try lighter wallpapers if you experience issues."
            ));
        } else if (tier == MemoryTier.LOW && weight == SceneWeight.MEDIUM) {
            issues.add(new Issue(
                    "Reduced performance",
                    "Your device has limited RAM. This wallpaper may run slow.",
                    Severity.WARNING,
                    "Close other apps to free memory."
            ));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // INSTALL CHECK - Resumen detallado pre-instalación
    // ═══════════════════════════════════════════════════════════════

    public enum ResourceType {
        VIDEO, IMAGE, MODEL
    }

    public static class ResourceStatus {
        public final ResourceType type;
        public final int total;
        public final int available;
        public final List<String> missing;

        ResourceStatus(ResourceType type, int total, int available, List<String> missing) {
            this.type = type;
            this.total = total;
            this.available = available;
            this.missing = Collections.unmodifiableList(missing);
        }

        public boolean isComplete() {
            return missing.isEmpty();
        }
    }

    public static class InstallCheckResult {
        public final List<ResourceStatus> resources;
        public final long availableRamMB;
        public final long freeDiskMB;
        public final boolean allResourcesReady;
        public final boolean systemHealthy;
        public final boolean canInstall;

        InstallCheckResult(List<ResourceStatus> resources, long availableRamMB, long freeDiskMB) {
            this.resources = Collections.unmodifiableList(resources);
            this.availableRamMB = availableRamMB;
            this.freeDiskMB = freeDiskMB;

            boolean allReady = true;
            for (ResourceStatus rs : resources) {
                if (!rs.isComplete()) {
                    allReady = false;
                    break;
                }
            }
            this.allResourcesReady = allReady;
            // Si availableRamMB es -1, no se pudo leer → asumir OK (no bloquear)
            this.systemHealthy = freeDiskMB >= 30 && (availableRamMB < 0 || availableRamMB >= 50);
            this.canInstall = allResourcesReady && systemHealthy;
        }

        /** Devuelve todos los archivos faltantes de todos los tipos */
        public List<String> getAllMissing() {
            List<String> all = new ArrayList<>();
            for (ResourceStatus rs : resources) {
                all.addAll(rs.missing);
            }
            return all;
        }
    }

    /**
     * Ejecuta verificación detallada de recursos + sistema para el dialog pre-instalación.
     */
    public static InstallCheckResult runInstallCheck(Context context, String sceneName) {
        // Asegurar que DeviceProfile esté inicializado
        DeviceProfile.init(context);

        List<ResourceStatus> resources = new ArrayList<>();

        // 1. Videos
        List<String> requiredVideos = ResourcePreloader.getRequiredVideos(sceneName);
        if (!requiredVideos.isEmpty()) {
            VideoDownloadManager videoDM = VideoDownloadManager.getInstance(context);
            List<String> missingVideos = new ArrayList<>();
            int availableVideos = 0;
            for (String v : requiredVideos) {
                if (videoDM.isVideoAvailable(v)) {
                    availableVideos++;
                } else {
                    missingVideos.add(v);
                }
            }
            resources.add(new ResourceStatus(ResourceType.VIDEO,
                    requiredVideos.size(), availableVideos, missingVideos));
        }

        // 2. Imágenes
        List<String> requiredImages = ResourcePreloader.getRequiredImages(sceneName);
        if (!requiredImages.isEmpty()) {
            ImageDownloadManager imageDM = ImageDownloadManager.getInstance(context);
            List<String> missingImages = new ArrayList<>();
            int availableImages = 0;
            for (String img : requiredImages) {
                if (imageDM.isImageAvailable(img)) {
                    availableImages++;
                } else {
                    missingImages.add(img);
                }
            }
            resources.add(new ResourceStatus(ResourceType.IMAGE,
                    requiredImages.size(), availableImages, missingImages));
        }

        // 3. Modelos
        List<String> requiredModels = ResourcePreloader.getRequiredModels(sceneName);
        if (!requiredModels.isEmpty()) {
            ModelDownloadManager modelDM = ModelDownloadManager.getInstance(context);
            List<String> missingModels = new ArrayList<>();
            int availableModels = 0;
            for (String model : requiredModels) {
                if (modelDM.isModelAvailable(model)) {
                    availableModels++;
                } else {
                    missingModels.add(model);
                }
            }
            resources.add(new ResourceStatus(ResourceType.MODEL,
                    requiredModels.size(), availableModels, missingModels));
        }

        // 4. RAM disponible
        long availableRamMB = DeviceProfile.get().getAvailableRamMB();

        // 5. Espacio en disco
        long freeDiskMB = 0;
        try {
            File filesDir = context.getFilesDir();
            if (filesDir != null) {
                StatFs stat = new StatFs(filesDir.getAbsolutePath());
                freeDiskMB = (stat.getAvailableBlocksLong() * stat.getBlockSizeLong()) / (1024L * 1024L);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error leyendo espacio en disco: " + e.getMessage());
        }

        return new InstallCheckResult(resources, availableRamMB, freeDiskMB);
    }
}
