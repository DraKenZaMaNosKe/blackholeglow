package com.secret.blackholeglow.image;

import com.secret.blackholeglow.download.ResourceInfo;

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
        // PANEL MODE (Gaming Controller) - Textura v5.0.8
        // ═══════════════════════════════════════════════════════════════

        IMAGES.put("controlxbox_texture.png", new ResourceInfo(
            "https://vzuwvsmlyigjtsearxym.supabase.co/storage/v1/object/public/wallpaper-models/controlxbox_texture.png",
            4_500_000L,  // ~4.37 MB
            "Textura Gaming Controller",
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

        // Textura zombie cuerpo completo (WebP optimizado)
        IMAGES.put("zombie_body_texture.webp", new ResourceInfo(
            "https://vzuwvsmlyigjtsearxym.supabase.co/storage/v1/object/public/wallpaper-models/zombie_body_texture.webp",
            351_000L,  // ~350.47 KB (v2: más oscuro, tono verdoso)
            "Textura Zombie Cuerpo",
            2  // v2: ajuste de colores para integración con escena
        ));

        // ═══════════════════════════════════════════════════════════════
        // ZELDA BOTW SCENE - Parallax Layers
        // ═══════════════════════════════════════════════════════════════

        IMAGES.put("zelda_fondo.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "zelda_fondo.png",
            980_000L,  // ~982 KB real
            "Zelda Cielo BOTW",
            1
        ));

        IMAGES.put("zelda_paisaje.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "zelda_paisaje.png",
            960_000L,  // ~960 KB real
            "Zelda Paisaje Hyrule",
            1
        ));

        IMAGES.put("zelda_piedra.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "zelda_piedra.png",
            197_000L,  // ~197 KB real
            "Zelda Roca Link",
            1
        ));

        IMAGES.put("zelda_link.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "zelda_link.png",
            272_000L,  // ~272 KB real
            "Zelda Link 2D",
            1
        ));

        // ═══════════════════════════════════════════════════════════════
        // ZELDA BOTW SCENE - Depth Map (solo fondo tiene parallax)
        // ═══════════════════════════════════════════════════════════════

        IMAGES.put("zelda_fondo_depth.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "zelda_fondo_depth.png",
            136_000L,  // ~136 KB real
            "Zelda Cielo Depth",
            1
        ));

        // NOTA: zelda_paisaje_depth.png y zelda_piedra_depth.png eliminados
        // porque esas capas son estáticas (no usan parallax)

        // ═══════════════════════════════════════════════════════════════
        // ZELDA BOTW SCENE - Link 3D Model Texture
        // ═══════════════════════════════════════════════════════════════

        IMAGES.put("link_3d_texture.webp", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "link_3d_texture.webp",
            600_000L,  // ~580 KB (WebP comprimido)
            "Link 3D Textura",
            1
        ));

        // ═══════════════════════════════════════════════════════════════
        // MOONLIT CAT SCENE - Atlas blink (open/half/closed)
        // ═══════════════════════════════════════════════════════════════

        IMAGES.put("cat_open.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "cat_open.png",
            890_000L,  // ~869 KB
            "Gato Ojos Abiertos",
            1
        ));

        IMAGES.put("cat_half.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "cat_half.png",
            763_000L,  // ~745 KB
            "Gato Ojos Semicerrados",
            1
        ));

        IMAGES.put("cat_closed.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "cat_closed.png",
            693_000L,  // ~677 KB
            "Gato Ojos Cerrados",
            1
        ));

        // Silueta de edificios (Moonlit Cat - capa 2D)
        IMAGES.put("buildings_silhouette.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "buildings_silhouette.png",
            360_000L,  // ~348 KB
            "Silueta Edificios",
            1
        ));

        // Textura luna (Moonlit Cat - NightSkyRenderer)
        IMAGES.put("moon_texture.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "moon_texture.png",
            190_000L,  // ~181 KB
            "Textura Luna",
            1
        ));

        // Textura barda de ladrillos (Meshy AI)
        IMAGES.put("brick_wall_texture.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "brick_wall_texture.png",
            7_254_000L,  // ~6.9 MB
            "Textura Barda Ladrillos",
            1
        ));

        // ═══════════════════════════════════════════════════════════════
        // 🏯 JAPANESE GARDEN SCENE - 8 capas parallax 2.5D
        // ═══════════════════════════════════════════════════════════════

        IMAGES.put("sky_moon.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "sky_moon.png",
            84_000L,  // ~84 KB
            "Cielo y Luna",
            1
        ));

        IMAGES.put("mountains.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "mountains.png",
            88_000L,  // ~88 KB
            "Montañas",
            1
        ));

        IMAGES.put("sakura_alpha.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "sakura_alpha.png",
            302_000L,  // ~302 KB
            "Cerezos Sakura",
            1
        ));

        IMAGES.put("torii_alpha.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "torii_alpha.png",
            67_000L,  // ~67 KB
            "Torii Gate",
            1
        ));

        IMAGES.put("lantern_alpha.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "lantern_alpha.png",
            62_000L,  // ~62 KB
            "Linternas Japonesas",
            1
        ));

        IMAGES.put("bridge_alpha.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "bridge_alpha.png",
            58_000L,  // ~58 KB
            "Puente de Madera",
            1
        ));

        IMAGES.put("bamboo_alpha.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "bamboo_alpha.png",
            202_000L,  // ~202 KB
            "Bambú",
            1
        ));

        IMAGES.put("foliage_alpha.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "foliage_alpha.png",
            344_000L,  // ~344 KB
            "Follaje Frontal",
            1
        ));

        // ═══════════════════════════════════════════════════════════════
        // FRIEZA DEATH BEAM SCENE - Texturas
        // ═══════════════════════════════════════════════════════════════

        // Textura baked de Frieza (Meshy AI, 2048x2048)
        IMAGES.put("frieza_texture.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "frieza_texture.png",
            5_312_000L,  // ~5.1 MB
            "Textura Frieza",
            1
        ));

        // Fondo anime estilo Dragon Ball
        IMAGES.put("frieza_bg_anime.png", new ResourceInfo(
            SUPABASE_VIDEOS_URL + "frieza_bg_anime.png",
            1_472_000L,  // ~1.4 MB
            "Fondo Anime Frieza",
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
}
