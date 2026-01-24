package com.secret.blackholeglow.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                      PanelResources                              ║
 * ║           Constantes centralizadas del Panel de Control          ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  PROPÓSITO:                                                      ║
 * ║  • Evitar duplicación de constantes en múltiples archivos        ║
 * ║  • Fuente única de verdad para recursos del Panel                ║
 * ║  • Facilitar mantenimiento y actualizaciones                     ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  USADO POR:                                                      ║
 * ║  • SplashActivity - Descarga inicial de recursos                 ║
 * ║  • AnimatedWallpaperListFragment - Verificación de recursos      ║
 * ║  • ResourcePreloader - Precarga y limpieza de recursos           ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  v5.0.8: Gaming Controller reemplaza ArcaneGrimoire              ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public final class PanelResources {

    private PanelResources() {
        // Utility class - no instantiation
    }

    // ═══════════════════════════════════════════════════════════════
    // 🖼️ IMÁGENES DEL PANEL
    // ═══════════════════════════════════════════════════════════════

    /**
     * Imágenes requeridas para el Panel de Control.
     * Estos archivos NUNCA se eliminan del dispositivo.
     */
    public static final List<String> IMAGES = Collections.unmodifiableList(Arrays.asList(
        "controlxbox_texture.png",  // Textura del Gaming Controller
        "huevo_zerg.png",           // LikeButton para ABYSSIA
        "fire_orb.png"              // LikeButton para PYRALIS
    ));

    /**
     * Array para compatibilidad con código legacy que usa String[]
     */
    public static final String[] IMAGES_ARRAY = IMAGES.toArray(new String[0]);

    // ═══════════════════════════════════════════════════════════════
    // 📦 MODELOS 3D DEL PANEL
    // ═══════════════════════════════════════════════════════════════

    /**
     * Modelos 3D requeridos para el Panel de Control.
     * Estos archivos NUNCA se eliminan del dispositivo.
     */
    public static final List<String> MODELS = Collections.unmodifiableList(Arrays.asList(
        "controlxbox_texture.obj"   // Gaming Controller 3D
    ));

    /**
     * Array para compatibilidad con código legacy que usa String[]
     */
    public static final String[] MODELS_ARRAY = MODELS.toArray(new String[0]);

    /**
     * Modelo principal del Panel (Gaming Controller)
     */
    public static final String PRIMARY_MODEL = "controlxbox_texture.obj";

    // ═══════════════════════════════════════════════════════════════
    // 📊 UTILIDADES
    // ═══════════════════════════════════════════════════════════════

    /**
     * @return Total de recursos del Panel (imágenes + modelos)
     */
    public static int getTotalResourceCount() {
        return IMAGES.size() + MODELS.size();
    }

    /**
     * Verifica si un recurso es parte del Panel (nunca debe eliminarse)
     * @param resourceName Nombre del archivo
     * @return true si es recurso del Panel
     */
    public static boolean isPanelResource(String resourceName) {
        return IMAGES.contains(resourceName) || MODELS.contains(resourceName);
    }
}
