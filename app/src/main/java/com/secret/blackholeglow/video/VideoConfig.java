package com.secret.blackholeglow.video;

import com.secret.blackholeglow.download.ResourceInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================================
 *                           VideoConfig
 *          Configuración de videos remotos en Supabase Storage
 * ============================================================================
 *
 * PARA AGREGAR NUEVOS VIDEOS:
 *   1. Subir video a Supabase (bucket: wallpaper-videos)
 *   2. Agregar entrada en el bloque static con:
 *      - nombre de archivo
 *      - URL completa (usar SUPABASE_VIDEOS_URL + nombre)
 *      - tamaño en bytes
 *      - descripción
 *      - versión (incrementar si reemplazas el video)
 *
 * ============================================================================
 */
public class VideoConfig {

    private static final String SUPABASE_VIDEOS_URL =
        "https://vzuwvsmlyigjtsearxym.supabase.co/storage/v1/object/public/wallpaper-videos/";

    private static final Map<String, ResourceInfo> VIDEOS = new HashMap<>();

    static {
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

        // GokuScene - Kamehameha Final HD
        VIDEOS.put("gokufinalkamehamehaHD.mp4", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "gokufinalkamehamehaHD.mp4",
            53_210_000L,  // 53.21 MB HD
            "Goku Kamehameha Final HD",
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

        // SupermanScene - Man of Steel (v2: Re-encoded 720p for performance)
        VIDEOS.put("superman_scene.mp4", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "superman_scene_720p.mp4",  // 720p version
            11_960_117L,  // ~12 MB (720p optimizado)
            "Superman Man of Steel",
            2  // v2: Re-encoded 720p (was 1440p causing crashes)
        ));

        // AOTScene - Attack on Titan (Eren Jaeger)
        VIDEOS.put("erenEscena01.mp4", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "erenEscena01_720p.mp4",  // 720p version
            3_695_019L,  // ~3.7 MB (720p optimizado)
            "Attack on Titan - Eren",
            1
        ));

        // SpiderScene - Black Spider Horror
        VIDEOS.put("spiderscene.mp4", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "spiderscene.mp4",
            15_378_022L,  // ~14.7 MB (pendiente optimización 720p)
            "Black Spider",
            1
        ));

        // LostAtlantisScene - Templo sumergido
        VIDEOS.put("lostatlanstis.mp4", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "lostatlanstis.mp4",
            15_550_000L,  // ~14.83 MB
            "Lost Atlantis",
            1
        ));
    }

    // =========================================================================
    // MÉTODOS PÚBLICOS
    // =========================================================================

    public static String getRemoteUrl(String fileName) {
        ResourceInfo info = VIDEOS.get(fileName);
        return info != null ? info.url : null;
    }

    public static long getExpectedSize(String fileName) {
        ResourceInfo info = VIDEOS.get(fileName);
        return info != null ? info.sizeBytes : 0;
    }

    public static String getDisplayName(String fileName) {
        ResourceInfo info = VIDEOS.get(fileName);
        return info != null ? info.displayName : fileName;
    }

    public static boolean isRemoteVideo(String fileName) {
        return VIDEOS.containsKey(fileName);
    }

    public static int getVideoVersion(String fileName) {
        ResourceInfo info = VIDEOS.get(fileName);
        return info != null ? info.version : 1;
    }
}
