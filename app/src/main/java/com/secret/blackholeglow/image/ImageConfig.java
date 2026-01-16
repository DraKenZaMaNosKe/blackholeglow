package com.secret.blackholeglow.image;

import java.util.HashMap;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                        ImageConfig                               ║
 * ║       Configuración de imágenes remotas en Supabase Storage      ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  RESPONSABILIDADES:                                              ║
 * ║  • Mapear nombres de archivo a URLs de Supabase                  ║
 * ║  • Almacenar tamaños esperados para validación                   ║
 * ║  • Gestionar versiones para auto-actualización                   ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  PARA AGREGAR NUEVAS IMÁGENES:                                   ║
 * ║  1. Subir imagen a Supabase (bucket: wallpaper-images o videos)  ║
 * ║  2. Agregar entrada en el bloque static con:                     ║
 * ║     - nombre de archivo                                          ║
 * ║     - URL completa                                               ║
 * ║     - tamaño en bytes                                            ║
 * ║     - descripción                                                ║
 * ║     - versión (incrementar si reemplazas la imagen)              ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class ImageConfig {

    // Base URLs de Supabase Storage
    private static final String SUPABASE_IMAGES_URL =
        "https://vzuwvsmlyigjtsearxym.supabase.co/storage/v1/object/public/wallpaper-images/";

    private static final String SUPABASE_VIDEOS_URL =
        "https://vzuwvsmlyigjtsearxym.supabase.co/storage/v1/object/public/wallpaper-videos/";

    // Mapeo: nombre de archivo -> información del recurso
    private static final Map<String, ResourceInfo> IMAGES = new HashMap<>();

    static {
        // ═══════════════════════════════════════════════════════════════
        // SAINT SEIYA - Sistema 2 Capas (Fondo + Personaje)
        // ═══════════════════════════════════════════════════════════════

        // Seiya solo (sin fondo, con transparencia)
        IMAGES.put("seiya_solo.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "seiya_solo.png",
            1_600_000L,  // ~1.58 MB
            "Seiya Solo",
            2
        ));

        // Depth map de Seiya (grayscale)
        IMAGES.put("seiya_depth.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "seiya_depth.png",
            160_000L,  // ~155 KB
            "Seiya Depth",
            2
        ));

        // Fondo cosmos
        IMAGES.put("fondouniverso.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "fondouniverso.png",
            1_900_000L,  // ~1.89 MB
            "Fondo Universo",
            2
        ));

        // Depth map del fondo (grayscale)
        IMAGES.put("fondouniverso3d.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "fondouniverso3d.png",
            75_000L,  // ~73 KB
            "Fondo Universo Depth",
            2
        ));

        // ═══════════════════════════════════════════════════════════════
        // NAVIDAD - Escena de Navidad
        // ═══════════════════════════════════════════════════════════════

        IMAGES.put("christmas_background.png", new ResourceInfo(
            SUPABASE_IMAGES_URL + "christmas_background.png",
            5_500_000L,  // ~5.5 MB
            "Fondo Navidad",
            1
        ));

        IMAGES.put("christmas_bg.png", new ResourceInfo(
            SUPABASE_IMAGES_URL + "christmas_bg.png",
            1_800_000L,  // ~1.8 MB
            "Fondo Navidad Panel",
            1
        ));

        IMAGES.put("preview_navidad.png", new ResourceInfo(
            SUPABASE_IMAGES_URL + "preview_navidad.png",
            5_500_000L,  // ~5.5 MB
            "Preview Navidad",
            1
        ));

        // ═══════════════════════════════════════════════════════════════
        // GOKU - Preview
        // ═══════════════════════════════════════════════════════════════

        IMAGES.put("gokuprimerframe.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "gokuprimerframe.png",
            2_900_000L,  // ~2.9 MB
            "Preview Goku",
            1
        ));

        // ═══════════════════════════════════════════════════════════════
        // ADVENTURE TIME - Preview
        // ═══════════════════════════════════════════════════════════════

        IMAGES.put("hdaPreview.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "hdaPreview.png",
            1_180_000L,  // ~1.18 MB
            "Preview Adventure Time",
            1
        ));

        // ═══════════════════════════════════════════════════════════════
        // OCEAN FLOOR SCENE (Abyssia) - Texturas
        // ═══════════════════════════════════════════════════════════════

        IMAGES.put("abyssal_lurker_texture.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "abyssal_lurker_texture.png",
            6_000_000L,  // ~5.8 MB
            "Textura Acechador Abisal",
            1
        ));

        IMAGES.put("abyssal_leviathan_texture.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "abyssal_leviathan_texture.png",
            2_100_000L,  // ~2 MB
            "Textura Leviatán Abisal",
            1
        ));

        IMAGES.put("huevo_zerg.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "huevo_zerg.png",
            260_000L,  // ~248 KB
            "Huevo Zerg",
            1
        ));

        // ═══════════════════════════════════════════════════════════════
        // NEON CITY SCENE (Synthwave) - Texturas
        // ═══════════════════════════════════════════════════════════════

        IMAGES.put("delorean_texture.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "delorean_texture.png",
            4_000_000L,  // ~3.9 MB
            "Textura DeLorean",
            1
        ));

        // ═══════════════════════════════════════════════════════════════
        // LAB SCENE (Pyralis) - Texturas
        // ═══════════════════════════════════════════════════════════════

        IMAGES.put("human_interceptor_texture.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "human_interceptor_texture.png",
            3_700_000L,  // ~3.6 MB
            "Textura Interceptor",
            1
        ));

        IMAGES.put("thruster_flames.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "thruster_flames.png",
            340_000L,  // ~331 KB
            "Llamas del Thruster",
            1
        ));

        IMAGES.put("fire_orb.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "fire_orb.png",
            230_000L,  // ~218 KB
            "Orbe de Fuego",
            1
        ));

        // ═══════════════════════════════════════════════════════════════
        // PANEL MODE (Arcane Grimoire) - Texturas
        // ═══════════════════════════════════════════════════════════════

        IMAGES.put("grimoire_texture.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "grimoire_texture.png",
            6_400_000L,  // ~6.23 MB
            "Textura Grimorio",
            1
        ));

        // ═══════════════════════════════════════════════════════════════
        // WALKING DEAD SCENE - Cabeza Zombi
        // ═══════════════════════════════════════════════════════════════

        IMAGES.put("zombie_head_texture.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "zombie_head_texture.png",
            5_200_000L,  // ~5.19 MB
            "Textura Cabeza Zombi",
            1
        ));

    }

    // ═══════════════════════════════════════════════════════════════════════
    // MÉTODOS PÚBLICOS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Obtiene la URL remota para una imagen.
     */
    public static String getRemoteUrl(String fileName) {
        ResourceInfo info = IMAGES.get(fileName);
        return info != null ? info.url : null;
    }

    /**
     * Obtiene el tamaño esperado de la imagen en bytes.
     */
    public static long getExpectedSize(String fileName) {
        ResourceInfo info = IMAGES.get(fileName);
        return info != null ? info.sizeBytes : 0;
    }

    /**
     * Obtiene el nombre descriptivo de la imagen.
     */
    public static String getDisplayName(String fileName) {
        ResourceInfo info = IMAGES.get(fileName);
        return info != null ? info.displayName : fileName;
    }

    /**
     * Verifica si una imagen está configurada para descarga remota.
     */
    public static boolean isRemoteImage(String fileName) {
        return IMAGES.containsKey(fileName);
    }

    /**
     * Obtiene la versión de una imagen.
     * Incrementar cuando se reemplaza la imagen en Supabase.
     */
    public static int getImageVersion(String fileName) {
        ResourceInfo info = IMAGES.get(fileName);
        return info != null ? info.version : 1;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CLASE INTERNA - ResourceInfo
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Información de un recurso de imagen remoto.
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
