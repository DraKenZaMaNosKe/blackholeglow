package com.secret.blackholeglow.models;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                        WallpaperTier                              ║
 * ║                   "Niveles de Acceso"                             ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  Define los niveles de acceso para wallpapers:                    ║
 * ║                                                                   ║
 * ║  FREE        → Disponible para todos los usuarios                 ║
 * ║  PREMIUM     → Requiere suscripción Premium                       ║
 * ║  VIP         → Requiere suscripción VIP (máximo nivel)            ║
 * ║  COMING_SOON → En desarrollo, próximamente disponible             ║
 * ║  BETA        → Versión de prueba, acceso limitado                 ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public enum WallpaperTier {

    // ═══════════════════════════════════════════════════════════════
    // NIVELES DE ACCESO
    // ═══════════════════════════════════════════════════════════════

    FREE(0, "Gratis", "Disponible para todos"),
    PREMIUM(1, "Premium", "Requiere suscripción Premium"),
    VIP(2, "VIP", "Exclusivo para miembros VIP"),
    COMING_SOON(99, "Próximamente", "En desarrollo"),
    BETA(98, "Beta", "Versión de prueba");

    // ═══════════════════════════════════════════════════════════════
    // PROPIEDADES
    // ═══════════════════════════════════════════════════════════════

    private final int level;
    private final String displayName;
    private final String description;

    WallpaperTier(int level, String displayName, String description) {
        this.level = level;
        this.displayName = displayName;
        this.description = description;
    }

    // ═══════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════

    /** Nivel numérico (0=FREE, 1=PREMIUM, 2=VIP) */
    public int getLevel() {
        return level;
    }

    /** Nombre para mostrar en UI */
    public String getDisplayName() {
        return displayName;
    }

    /** Descripción del nivel */
    public String getDescription() {
        return description;
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verifica si un usuario con cierto nivel puede acceder a este tier
     * @param userLevel Nivel del usuario (0=FREE, 1=PREMIUM, 2=VIP)
     * @return true si puede acceder
     */
    public boolean isAccessibleBy(int userLevel) {
        // COMING_SOON y BETA no son accesibles por nivel normal
        if (this == COMING_SOON) return false;
        if (this == BETA) return userLevel >= 1; // Solo Premium+ puede ver betas

        return userLevel >= this.level;
    }

    /**
     * Verifica si este wallpaper está disponible para usar
     * (no está en desarrollo)
     */
    public boolean isPlayable() {
        return this != COMING_SOON;
    }

    /**
     * Obtiene el color asociado al tier (para badges)
     * @return Color en formato ARGB
     */
    public int getBadgeColor() {
        switch (this) {
            case FREE:
                return 0xFF4CAF50; // Verde
            case PREMIUM:
                return 0xFFFFD700; // Dorado
            case VIP:
                return 0xFFE91E63; // Rosa/Magenta
            case COMING_SOON:
                return 0xFF9E9E9E; // Gris
            case BETA:
                return 0xFF2196F3; // Azul
            default:
                return 0xFFFFFFFF; // Blanco
        }
    }
}
