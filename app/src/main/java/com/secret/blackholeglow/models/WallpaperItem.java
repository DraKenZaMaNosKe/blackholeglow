package com.secret.blackholeglow.models;

/**
 * Clase que representa un fondo de pantalla animado en la lista.
 */
public class WallpaperItem {

    private String nombre;
    private int resourceIdPreview; // ID de recurso de la imagen preview
    private String descripcion;
    private boolean isAvailable; // Indica si el wallpaper está disponible para ver

    public WallpaperItem(int resourceIdPreview, String nombre, String descripcion){
        this.nombre = nombre;
        this.resourceIdPreview = resourceIdPreview;
        this.descripcion = descripcion;
        this.isAvailable = true; // Por defecto está disponible
    }

    public WallpaperItem(int resourceIdPreview, String nombre, String descripcion, boolean isAvailable){
        this.nombre = nombre;
        this.resourceIdPreview = resourceIdPreview;
        this.descripcion = descripcion;
        this.isAvailable = isAvailable;
    }

    public String getNombre() {
        return nombre;
    }

    public int getResourceIdPreview() {
        return resourceIdPreview;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isAvailable() {
        return isAvailable;
    }
}