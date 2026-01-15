package com.secret.blackholeglow.video;

import java.util.HashMap;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                         VideoConfig                              ║
 * ║        Configuración de videos remotos en Supabase Storage       ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  RESPONSABILIDADES:                                              ║
 * ║  • Mapear nombres de archivo a URLs de Supabase                  ║
 * ║  • Almacenar tamaños esperados para validación                   ║
 * ║  • Gestionar versiones para auto-actualización                   ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  PARA AGREGAR NUEVOS VIDEOS:                                     ║
 * ║  1. Subir video a Supabase (bucket: wallpaper-videos)            ║
 * ║  2. Agregar entrada en el bloque static con:                     ║
 * ║     - nombre de archivo                                          ║
 * ║     - URL completa                                               ║
 * ║     - tamaño en bytes                                            ║
 * ║     - descripción                                                ║
 * ║     - versión (incrementar si reemplazas el video)               ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  NOTA: Para imágenes usar ImageConfig                            ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class VideoConfig {

    // Base URL de Supabase Storage para videos
    private static final String SUPABASE_VIDEOS_URL =
        "https://vzuwvsmlyigjtsearxym.supabase.co/storage/v1/object/public/wallpaper-videos/";

    // Mapeo: nombre de archivo -> información del recurso
    private static final Map<String, ResourceInfo> VIDEOS = new HashMap<>();

    static {
        // ═══════════════════════════════════════════════════════════════
        // VIDEOS - Escenas de Wallpaper
        // ═══════════════════════════════════════════════════════════════

        // LabScene - Portal cósmico con nubes de fuego
        VIDEOS.put("cielovolando.mp4", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "cielovolando.mp4",
            10_199_549L,  // 10.2 MB
            "Portal Cosmico",
            1
        ));

        // OceanFloorScene - Abyssia (océano alienígena)
        VIDEOS.put("marZerg.mp4", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "marZerg.mp4",
            9_983_000L,  // ~10 MB
            "Abyssia",
            1
        ));

        // Panel de Control - The House (cabaña acogedora con chimenea)
        VIDEOS.put("thehouse.mp4", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "thehouse.mp4",
            10_235_904L,  // 9.76 MB
            "The House",
            1
        ));

        // GokuScene - Kamehameha Final
        VIDEOS.put("gokukamehameFinal.mp4", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "gokukamehameFinal.mp4",
            8_210_000L,  // 7.83 MB
            "Goku Kamehameha Final",
            1
        ));

        // AdventureTimeScene - Hora de Aventura Fogata
        VIDEOS.put("escenaHDA.mp4", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "escenaHDA.mp4",
            13_000_000L,  // ~13 MB
            "Adventure Time Fogata",
            1
        ));

        // NeonCityScene - Synthwave Endless Road
        VIDEOS.put("neoncityScene.mp4", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "neoncityScene.mp4",
            13_680_000L,  // 13.05 MB
            "Neon City Synthwave",
            1
        ));

        // WalkingDeadScene - Cementerio Zombie Apocalíptico
        VIDEOS.put("walkingdeathscene.mp4", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "walkingdeathscene.mp4",
            29_000_000L,  // ~29 MB (1080p)
            "The Walking Dead",
            2  // v2: Re-encoded 1080p
        ));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MÉTODOS PÚBLICOS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Obtiene la URL remota para un video.
     */
    public static String getRemoteUrl(String fileName) {
        ResourceInfo info = VIDEOS.get(fileName);
        return info != null ? info.url : null;
    }

    /**
     * Obtiene el tamaño esperado del video en bytes.
     */
    public static long getExpectedSize(String fileName) {
        ResourceInfo info = VIDEOS.get(fileName);
        return info != null ? info.sizeBytes : 0;
    }

    /**
     * Obtiene el nombre descriptivo del video.
     */
    public static String getDisplayName(String fileName) {
        ResourceInfo info = VIDEOS.get(fileName);
        return info != null ? info.displayName : fileName;
    }

    /**
     * Verifica si un video está configurado para descarga remota.
     */
    public static boolean isRemoteVideo(String fileName) {
        return VIDEOS.containsKey(fileName);
    }

    /**
     * Obtiene la versión de un video.
     * Incrementar cuando se reemplaza el video en Supabase.
     */
    public static int getVideoVersion(String fileName) {
        ResourceInfo info = VIDEOS.get(fileName);
        return info != null ? info.version : 1;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CLASE INTERNA - ResourceInfo
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Información de un recurso de video remoto.
     */
    public static class ResourceInfo {
        public final String url;
        public final long sizeBytes;
        public final String displayName;
        public final int version;

        ResourceInfo(String url, long sizeBytes, String displayName) {
            this(url, sizeBytes, displayName, 1);
        }

        ResourceInfo(String url, long sizeBytes, String displayName, int version) {
            this.url = url;
            this.sizeBytes = sizeBytes;
            this.displayName = displayName;
            this.version = version;
        }
    }
}
