package com.secret.blackholeglow.models;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                        WallpaperItem                              ║
 * ║                   "La Ficha del Wallpaper"                        ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  Modelo de datos que representa un wallpaper en el catálogo.      ║
 * ║                                                                   ║
 * ║  DATOS BÁSICOS:                                                   ║
 * ║  • nombre, descripcion, resourceIdPreview                         ║
 * ║                                                                   ║
 * ║  DATOS DE ACCESO:                                                 ║
 * ║  • tier (FREE, PREMIUM, VIP, COMING_SOON, BETA)                   ║
 * ║  • sceneName (nombre interno para SceneFactory)                   ║
 * ║                                                                   ║
 * ║  DATOS DE PRESENTACIÓN:                                           ║
 * ║  • badge (etiqueta: "NUEVO", "HOT", "EXCLUSIVO")                  ║
 * ║  • glowColor (color de efecto glow para destacar)                 ║
 * ║  • isFeatured (si está destacado en el catálogo)                  ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class WallpaperItem {

    // ═══════════════════════════════════════════════════════════════
    // DATOS BÁSICOS
    // ═══════════════════════════════════════════════════════════════

    private final String nombre;
    private final String descripcion;
    private final int resourceIdPreview;

    // ═══════════════════════════════════════════════════════════════
    // DATOS DE ACCESO
    // ═══════════════════════════════════════════════════════════════

    private final String sceneName;      // Nombre interno para SceneFactory ("Universo", "Batalla Cósmica")
    private final WallpaperTier tier;    // Nivel de acceso

    // ═══════════════════════════════════════════════════════════════
    // DATOS DE PRESENTACIÓN
    // ═══════════════════════════════════════════════════════════════

    private final String badge;          // Etiqueta especial (null si no tiene)
    private final int glowColor;         // Color de glow para destacar (0 = sin glow)
    private final boolean isFeatured;    // Si está destacado

    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTORES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Constructor completo con todos los campos
     */
    public WallpaperItem(
            String nombre,
            String descripcion,
            int resourceIdPreview,
            String sceneName,
            WallpaperTier tier,
            String badge,
            int glowColor,
            boolean isFeatured
    ) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.resourceIdPreview = resourceIdPreview;
        this.sceneName = sceneName;
        this.tier = tier;
        this.badge = badge;
        this.glowColor = glowColor;
        this.isFeatured = isFeatured;
    }

    /**
     * Constructor simplificado para wallpapers FREE sin efectos especiales
     */
    public WallpaperItem(int resourceIdPreview, String nombre, String descripcion) {
        this(nombre, descripcion, resourceIdPreview, nombre, WallpaperTier.FREE, null, 0, false);
    }

    /**
     * Constructor con disponibilidad (compatibilidad hacia atrás)
     */
    public WallpaperItem(int resourceIdPreview, String nombre, String descripcion, boolean isAvailable) {
        this(
                nombre,
                descripcion,
                resourceIdPreview,
                nombre,
                isAvailable ? WallpaperTier.FREE : WallpaperTier.COMING_SOON,
                null,
                0,
                false
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // BUILDER PATTERN (para construcción fluida)
    // ═══════════════════════════════════════════════════════════════

    public static class Builder {
        private String nombre;
        private String descripcion = "";
        private int resourceIdPreview;
        private String sceneName;
        private WallpaperTier tier = WallpaperTier.FREE;
        private String badge = null;
        private int glowColor = 0;
        private boolean isFeatured = false;

        public Builder(String nombre) {
            this.nombre = nombre;
            this.sceneName = nombre; // Por defecto, sceneName = nombre
        }

        public Builder descripcion(String descripcion) {
            this.descripcion = descripcion;
            return this;
        }

        public Builder preview(int resourceId) {
            this.resourceIdPreview = resourceId;
            return this;
        }

        public Builder sceneName(String sceneName) {
            this.sceneName = sceneName;
            return this;
        }

        public Builder tier(WallpaperTier tier) {
            this.tier = tier;
            return this;
        }

        public Builder badge(String badge) {
            this.badge = badge;
            return this;
        }

        public Builder glow(int color) {
            this.glowColor = color;
            return this;
        }

        public Builder featured() {
            this.isFeatured = true;
            return this;
        }

        public WallpaperItem build() {
            return new WallpaperItem(
                    nombre, descripcion, resourceIdPreview,
                    sceneName, tier, badge, glowColor, isFeatured
            );
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GETTERS BÁSICOS
    // ═══════════════════════════════════════════════════════════════

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getResourceIdPreview() {
        return resourceIdPreview;
    }

    // ═══════════════════════════════════════════════════════════════
    // GETTERS DE ACCESO
    // ═══════════════════════════════════════════════════════════════

    /** Nombre interno para SceneFactory */
    public String getSceneName() {
        return sceneName;
    }

    /** Nivel de acceso (FREE, PREMIUM, etc.) */
    public WallpaperTier getTier() {
        return tier;
    }

    /**
     * Verifica si el wallpaper está disponible para usar
     * (compatibilidad hacia atrás)
     */
    public boolean isAvailable() {
        return tier.isPlayable();
    }

    // ═══════════════════════════════════════════════════════════════
    // GETTERS DE PRESENTACIÓN
    // ═══════════════════════════════════════════════════════════════

    /** Etiqueta especial ("NUEVO", "HOT", null si no tiene) */
    public String getBadge() {
        return badge;
    }

    /** Verifica si tiene badge */
    public boolean hasBadge() {
        return badge != null && !badge.isEmpty();
    }

    /** Color de glow para destacar (0 = sin glow) */
    public int getGlowColor() {
        return glowColor;
    }

    /** Verifica si tiene efecto glow */
    public boolean hasGlow() {
        return glowColor != 0;
    }

    /** Si está destacado en el catálogo */
    public boolean isFeatured() {
        return isFeatured;
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verifica si un usuario con cierto nivel puede acceder
     * @param userLevel Nivel del usuario (0=FREE, 1=PREMIUM, 2=VIP)
     */
    public boolean isAccessibleBy(int userLevel) {
        return tier.isAccessibleBy(userLevel);
    }

    /**
     * Obtiene el color del badge del tier
     */
    public int getTierBadgeColor() {
        return tier.getBadgeColor();
    }

    @Override
    public String toString() {
        return "WallpaperItem{" +
                "nombre='" + nombre + '\'' +
                ", tier=" + tier +
                ", badge='" + badge + '\'' +
                ", featured=" + isFeatured +
                '}';
    }
}
