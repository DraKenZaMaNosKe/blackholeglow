package com.secret.blackholeglow.models;

/**
 * Categories for organizing wallpapers in the catalog.
 * Empty categories (no wallpapers assigned) are hidden from the UI.
 */
public enum WallpaperCategory {
    ALL("TODOS", "\u2726"),
    ANIME("ANIME", "\uD83D\uDC09"),
    GAMING("VIDEOJUEGOS", "\uD83C\uDFAE"),
    SCENES("ESCENAS", "\uD83C\uDFAC"),
    ANIMALS("ANIMALES", "\uD83D\uDC31"),
    NATURE("NATURALEZA", "\uD83C\uDF3F"),
    UNIVERSE("UNIVERSO", "\uD83C\uDF0C"),
    MISC("VARIOS", "\u2728"),
    CHRISTMAS("NAVIDAD", "\uD83C\uDF84"),
    SUMMER("VERANO", "\u2600\uFE0F"),
    AUTUMN("OTO\u00D1O", "\uD83C\uDF42"),
    WINTER("INVIERNO", "\u2744\uFE0F"),
    SPECIAL("DIAS ESPECIALES", "\uD83C\uDF89");

    private final String displayName;
    private final String emoji;

    WallpaperCategory(String displayName, String emoji) {
        this.displayName = displayName;
        this.emoji = emoji;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getDisplayLabel() {
        return emoji + " " + displayName;
    }

    /**
     * Parse from string (case-insensitive), defaults to MISC if unknown.
     */
    public static WallpaperCategory fromString(String value) {
        if (value == null || value.isEmpty()) return MISC;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MISC;
        }
    }
}
