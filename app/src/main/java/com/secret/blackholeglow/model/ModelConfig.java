package com.secret.blackholeglow.model;

import java.util.HashMap;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                        ModelConfig                               ║
 * ║      Configuración de modelos 3D remotos en Supabase Storage     ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  RESPONSABILIDADES:                                              ║
 * ║  • Mapear nombres de archivo OBJ/MTL a URLs de Supabase          ║
 * ║  • Almacenar tamaños esperados para validación                   ║
 * ║  • Gestionar versiones para auto-actualización                   ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  PARA AGREGAR NUEVOS MODELOS:                                    ║
 * ║  1. Subir modelo a Supabase (bucket: wallpaper-models)           ║
 * ║  2. Agregar entrada en el bloque static con:                     ║
 * ║     - nombre de archivo (.obj o .mtl)                            ║
 * ║     - URL completa                                               ║
 * ║     - tamaño en bytes                                            ║
 * ║     - descripción                                                ║
 * ║     - versión (incrementar si reemplazas el modelo)              ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class ModelConfig {

    // Base URL de Supabase Storage para modelos
    private static final String SUPABASE_MODELS_URL =
        "https://vzuwvsmlyigjtsearxym.supabase.co/storage/v1/object/public/wallpaper-models/";

    // Mapeo: nombre de archivo -> información del recurso
    private static final Map<String, ResourceInfo> MODELS = new HashMap<>();

    static {
        // ═══════════════════════════════════════════════════════════════
        // OCEAN FLOOR SCENE (Abyssia) - Criaturas marinas
        // ═══════════════════════════════════════════════════════════════

        // Leviatán Abisal - Criatura grande del fondo oceánico
        MODELS.put("abyssal_leviathan.obj", new ResourceInfo(
            SUPABASE_MODELS_URL + "abyssal_leviathan.obj",
            740_000L,  // ~719 KB
            "Leviatán Abisal",
            1
        ));

        // Acechador Abisal - Criatura depredadora
        MODELS.put("abyssal_lurker.obj", new ResourceInfo(
            SUPABASE_MODELS_URL + "abyssal_lurker.obj",
            520_000L,  // ~504 KB
            "Acechador Abisal",
            1
        ));

        // ═══════════════════════════════════════════════════════════════
        // NEON CITY SCENE (Synthwave) - DeLorean
        // ═══════════════════════════════════════════════════════════════

        // DeLorean DMC-12 estilo Back to the Future
        MODELS.put("delorean.obj", new ResourceInfo(
            SUPABASE_MODELS_URL + "delorean.obj",
            500_000L,  // ~482 KB
            "DeLorean DMC-12",
            1
        ));

        // ═══════════════════════════════════════════════════════════════
        // PANEL MODE (Arcane Grimoire) - Libro mágico
        // ═══════════════════════════════════════════════════════════════

        // Grimorio Arcano - Libro místico del panel principal
        MODELS.put("grimoire.obj", new ResourceInfo(
            SUPABASE_MODELS_URL + "grimoire.obj",
            490_000L,  // ~476 KB
            "Grimorio Arcano",
            1
        ));

        // ═══════════════════════════════════════════════════════════════
        // LAB SCENE (Pyralis) - Nave interceptora
        // ═══════════════════════════════════════════════════════════════

        // Interceptor Humano con llamas en los propulsores
        MODELS.put("human_interceptor_flames.obj", new ResourceInfo(
            SUPABASE_MODELS_URL + "human_interceptor_flames.obj",
            290_000L,  // ~281 KB
            "Interceptor con Llamas",
            1
        ));

        // ═══════════════════════════════════════════════════════════════
        // WALKING DEAD SCENE - Cabeza zombi colgante
        // ═══════════════════════════════════════════════════════════════

        // Cabeza zombi encadenada (Meshy AI)
        // NOTA: Subido a wallpaper-videos bucket
        MODELS.put("zombie_head.obj", new ResourceInfo(
            "https://vzuwvsmlyigjtsearxym.supabase.co/storage/v1/object/public/wallpaper-videos/zombie_head.obj",
            280_000L,  // ~279 KB
            "Cabeza Zombi",
            1
        ));

        // Zombie cuerpo completo (Meshy AI) - Asomando desde abajo
        MODELS.put("zombie_body.obj", new ResourceInfo(
            SUPABASE_MODELS_URL + "zombie_body.obj",
            274_000L,  // ~273.55 KB
            "Zombie Cuerpo",
            1
        ));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MÉTODOS PÚBLICOS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Obtiene la URL remota para un modelo.
     */
    public static String getRemoteUrl(String fileName) {
        ResourceInfo info = MODELS.get(fileName);
        return info != null ? info.url : null;
    }

    /**
     * Obtiene el tamaño esperado del modelo en bytes.
     */
    public static long getExpectedSize(String fileName) {
        ResourceInfo info = MODELS.get(fileName);
        return info != null ? info.sizeBytes : 0;
    }

    /**
     * Obtiene el nombre descriptivo del modelo.
     */
    public static String getDisplayName(String fileName) {
        ResourceInfo info = MODELS.get(fileName);
        return info != null ? info.displayName : fileName;
    }

    /**
     * Verifica si un modelo está configurado para descarga remota.
     */
    public static boolean isRemoteModel(String fileName) {
        return MODELS.containsKey(fileName);
    }

    /**
     * Obtiene la versión de un modelo.
     * Incrementar cuando se reemplaza el modelo en Supabase.
     */
    public static int getModelVersion(String fileName) {
        ResourceInfo info = MODELS.get(fileName);
        return info != null ? info.version : 1;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CLASE INTERNA - ResourceInfo
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Información de un recurso de modelo remoto.
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
