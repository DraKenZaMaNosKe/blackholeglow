package com.secret.blackholeglow.video;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuracion de recursos remotos en Supabase Storage
 *
 * Videos e imagenes se descargan bajo demanda y se guardan en cache local.
 * Esto reduce significativamente el tamano del APK.
 */
public class VideoConfig {

    // Base URLs de Supabase Storage
    private static final String SUPABASE_VIDEOS_URL =
        "https://vzuwvsmlyigjtsearxym.supabase.co/storage/v1/object/public/wallpaper-videos/";

    private static final String SUPABASE_IMAGES_URL =
        "https://vzuwvsmlyigjtsearxym.supabase.co/storage/v1/object/public/wallpaper-images/";

    // Mapeo: nombre de archivo -> URL remota
    private static final Map<String, ResourceInfo> VIDEOS = new HashMap<>();
    private static final Map<String, ResourceInfo> IMAGES = new HashMap<>();

    static {
        // ═══════════════════════════════════════════════════════════════
        // VIDEOS
        // ═══════════════════════════════════════════════════════════════

        // LabScene - Portal cosmico con nubes de fuego
        VIDEOS.put("cielovolando.mp4", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "cielovolando.mp4",
            10_199_549L,  // 10.2 MB
            "Portal Cosmico"
        ));

        // OceanFloorScene - Abyssia (océano alienígena)
        VIDEOS.put("marZerg.mp4", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "marZerg.mp4",
            9_983_000L,  // ~10 MB
            "Abyssia"
        ));

        // Panel de Control - The House (cabaña acogedora con chimenea)
        VIDEOS.put("thehouse.mp4", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "thehouse.mp4",
            10_235_904L,  // 9.76 MB
            "The House"
        ));

        // GokuScene - Kamehameha Final (Goku lanzando el Kamehameha)
        VIDEOS.put("gokukamehameFinal.mp4", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "gokukamehameFinal.mp4",
            8_210_000L,  // 7.83 MB
            "Goku Kamehameha Final"
        ));

        // AdventureTimeScene - Hora de Aventura Fogata
        VIDEOS.put("escenaHDA.mp4", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "escenaHDA.mp4",
            13_000_000L,  // ~13 MB
            "Adventure Time Fogata"
        ));

        // ═══════════════════════════════════════════════════════════════
        // IMAGENES - Escena Navidad
        // ═══════════════════════════════════════════════════════════════

        IMAGES.put("christmas_background.png", new ResourceInfo(
            SUPABASE_IMAGES_URL + "christmas_background.png",
            5_500_000L,  // ~5.5 MB
            "Fondo Navidad"
        ));

        IMAGES.put("christmas_bg.png", new ResourceInfo(
            SUPABASE_IMAGES_URL + "christmas_bg.png",
            1_800_000L,  // ~1.8 MB
            "Fondo Navidad Panel"
        ));

        IMAGES.put("preview_navidad.png", new ResourceInfo(
            SUPABASE_IMAGES_URL + "preview_navidad.png",
            5_500_000L,  // ~5.5 MB
            "Preview Navidad"
        ));

        // ═══════════════════════════════════════════════════════════════
        // IMAGENES - Escena Goku
        // ═══════════════════════════════════════════════════════════════

        IMAGES.put("gokuprimerframe.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "gokuprimerframe.png",  // Está en wallpaper-videos
            2_900_000L,  // ~2.9 MB
            "Preview Goku"
        ));

        // ═══════════════════════════════════════════════════════════════
        // IMAGENES - Escena Adventure Time
        // ═══════════════════════════════════════════════════════════════

        IMAGES.put("hdaPreview.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "hdaPreview.png",  // Está en wallpaper-videos
            1_180_000L,  // ~1.18 MB
            "Preview Adventure Time"
        ));
    }

    // ═══════════════════════════════════════════════════════════════
    // METODOS PARA VIDEOS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Obtiene la URL remota para un video
     */
    public static String getRemoteUrl(String fileName) {
        ResourceInfo info = VIDEOS.get(fileName);
        return info != null ? info.url : null;
    }

    /**
     * Obtiene el tamano esperado del video en bytes
     */
    public static long getExpectedSize(String fileName) {
        ResourceInfo info = VIDEOS.get(fileName);
        return info != null ? info.sizeBytes : 0;
    }

    /**
     * Obtiene el nombre descriptivo del video
     */
    public static String getDisplayName(String fileName) {
        ResourceInfo info = VIDEOS.get(fileName);
        return info != null ? info.displayName : fileName;
    }

    /**
     * Verifica si un video esta configurado para descarga remota
     */
    public static boolean isRemoteVideo(String fileName) {
        return VIDEOS.containsKey(fileName);
    }

    // ═══════════════════════════════════════════════════════════════
    // METODOS PARA IMAGENES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Obtiene la URL remota para una imagen
     */
    public static String getImageRemoteUrl(String fileName) {
        ResourceInfo info = IMAGES.get(fileName);
        return info != null ? info.url : null;
    }

    /**
     * Obtiene el tamano esperado de la imagen en bytes
     */
    public static long getImageExpectedSize(String fileName) {
        ResourceInfo info = IMAGES.get(fileName);
        return info != null ? info.sizeBytes : 0;
    }

    /**
     * Obtiene el nombre descriptivo de la imagen
     */
    public static String getImageDisplayName(String fileName) {
        ResourceInfo info = IMAGES.get(fileName);
        return info != null ? info.displayName : fileName;
    }

    /**
     * Verifica si una imagen esta configurada para descarga remota
     */
    public static boolean isRemoteImage(String fileName) {
        return IMAGES.containsKey(fileName);
    }

    // ═══════════════════════════════════════════════════════════════
    // CLASE DE INFORMACION DE RECURSO
    // ═══════════════════════════════════════════════════════════════

    /**
     * Informacion de un recurso remoto (video o imagen)
     */
    public static class ResourceInfo {
        public final String url;
        public final long sizeBytes;
        public final String displayName;

        ResourceInfo(String url, long sizeBytes, String displayName) {
            this.url = url;
            this.sizeBytes = sizeBytes;
            this.displayName = displayName;
        }
    }
}
