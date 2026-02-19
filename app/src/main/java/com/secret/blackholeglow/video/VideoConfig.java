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
        // LabScene - Portal cósmico (v2: 360x360 optimizado, era 480x480)
        VIDEOS.put("cielovolando.mp4", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "cielovolando.mp4",
            9_903_257L,  // ~9.9 MB (360x360 @ 1.5Mbps)
            "Portal Cosmico",
            2  // v2: Re-encoded 360p para reducir RAM
        ));

        // OceanFloorScene - Abyssia (v2: 360x360 optimizado, era 480x480)
        VIDEOS.put("marZerg.mp4", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "marZerg.mp4",
            9_488_423L,  // ~9.5 MB (360x360 @ 1.5Mbps)
            "Abyssia",
            2  // v2: Re-encoded 360p para reducir RAM
        ));

        // GokuScene - Kamehameha Final (v2: 540x800 optimizado, era 1080x1602)
        VIDEOS.put("gokufinalkamehamehaHD.mp4", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "gokufinalkamehamehaHD.mp4",
            11_372_197L,  // ~11.4 MB (540x800 @ 2.5Mbps, antes 55.8 MB!)
            "Goku Kamehameha Final HD",
            2  // v2: Re-encoded 540p (-80% tamaño, -20MB RAM decoder)
        ));

        // AdventureTimeScene - Hora de Aventura (v2: 360x534, era 480x712)
        VIDEOS.put("escenaHDA.mp4", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "escenaHDA.mp4",
            11_262_319L,  // ~11.3 MB (360x534 @ 1.5Mbps)
            "Adventure Time Fogata",
            2  // v2: Re-encoded 360p para reducir RAM
        ));

        // NeonCityScene - Synthwave (v2: 360x360, era 480x480)
        VIDEOS.put("neoncityScene.mp4", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "neoncityScene.mp4",
            11_447_391L,  // ~11.4 MB (360x360 @ 1.5Mbps)
            "Neon City Synthwave",
            2  // v2: Re-encoded 360p para reducir RAM
        ));

        // WalkingDeadScene - Cementerio Zombie (v3: 540x956, era 1080x1912)
        VIDEOS.put("walkingdeathscene.mp4", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "walkingdeathscene.mp4",
            6_036_028L,  // ~6.0 MB (540x956 @ 2.5Mbps, antes 30.4 MB!)
            "The Walking Dead",
            3  // v3: Re-encoded 540p (-80% tamaño, -20MB RAM decoder)
        ));

        // SupermanScene - Man of Steel (v3: 540x800, era 720x1068)
        VIDEOS.put("superman_scene.mp4", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "superman_scene_720p.mp4",
            7_548_912L,  // ~7.5 MB (540x800 @ 2.5Mbps, antes 12 MB)
            "Superman Man of Steel",
            3  // v3: Re-encoded 540p para reducir RAM
        ));

        // AOTScene - Attack on Titan (v2: 540x800, era 720x1068)
        VIDEOS.put("erenEscena01.mp4", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "erenEscena01_720p.mp4",
            3_202_424L,  // ~3.2 MB (540x800 @ 2.5Mbps)
            "Attack on Titan - Eren",
            2  // v2: Re-encoded 540p para reducir RAM
        ));

        // SpiderScene - Black Spider (v2: 540x800, era 1080x1600)
        VIDEOS.put("spiderscene.mp4", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "spiderscene.mp4",
            3_183_074L,  // ~3.2 MB (540x800 @ 2.5Mbps, antes 15.4 MB!)
            "Black Spider",
            2  // v2: Re-encoded 540p (-79% tamaño, -20MB RAM decoder)
        ));

        // LostAtlantisScene - Templo sumergido (v2: 540x800, era 1080x1600)
        VIDEOS.put("lostatlanstis.mp4", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "lostatlanstis.mp4",
            3_302_401L,  // ~3.1 MB (540x800 @ 2.5Mbps, antes 14.83 MB)
            "Lost Atlantis",
            2  // v2: Re-encoded 540p para reducir RAM del decoder
        ));

        // TheHumanPredatorScene - Guerrero vs León
        VIDEOS.put("guerrerovsleon.mp4", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "guerrerovsleon.mp4",
            15_672_871L,  // ~15.6 MB
            "The Human Predator",
            1  // v1: Initial version
        ));

        // FriezaDeathBeamScene - Fondo anime speed lines morado (v2: 540x960, 3s loop)
        VIDEOS.put("frieza_deathbeam_bg.mp4", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "frieza_deathbeam_bg.mp4",
            440_255L,  // ~440 KB (540x960 @ 1.2Mbps)
            "Frieza Death Beam BG",
            2  // v2: Purple version
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
