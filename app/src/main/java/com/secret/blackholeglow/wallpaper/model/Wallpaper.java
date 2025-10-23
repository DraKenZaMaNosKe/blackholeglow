package com.secret.blackholeglow.wallpaper.model;

/**
 * ═══════════════════════════════════════════════════════════════
 * Wallpaper - Modelo de Datos (MVC Pattern)
 * ═══════════════════════════════════════════════════════════════
 *
 * Representa un wallpaper animado con toda su metadata.
 *
 * Campos:
 *  - id: Identificador único del wallpaper (usado por SceneRenderer)
 *  - name: Nombre técnico (ej: "Universo", "DiscoBall")
 *  - displayName: Nombre visual con emoji (ej: "🌌 Universo Infinito")
 *  - description: Descripción larga del efecto
 *  - thumbnailResId: Recurso drawable para preview
 *  - backgroundColor: Color de fondo único para testing (formato #RRGGBB)
 *
 * Patrón: Immutable Value Object
 */
public class Wallpaper {

    private final String id;
    private final String name;
    private final String displayName;
    private final String description;
    private final int thumbnailResId;
    private final String backgroundColor;  // Para testing (ej: "#FF0000")

    /**
     * Constructor completo
     */
    public Wallpaper(String id, String name, String displayName, String description,
                     int thumbnailResId, String backgroundColor) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.thumbnailResId = thumbnailResId;
        this.backgroundColor = backgroundColor;
    }

    /**
     * Constructor simplificado (sin background color)
     */
    public Wallpaper(String id, String name, String displayName, String description,
                     int thumbnailResId) {
        this(id, name, displayName, description, thumbnailResId, "#000000");
    }

    // ═══════════════════════════════════════════════════════════
    // Getters (immutable)
    // ═══════════════════════════════════════════════════════════

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getThumbnailResId() {
        return thumbnailResId;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    // ═══════════════════════════════════════════════════════════
    // Utility methods
    // ═══════════════════════════════════════════════════════════

    @Override
    public String toString() {
        return "Wallpaper{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Wallpaper wallpaper = (Wallpaper) o;
        return id.equals(wallpaper.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
