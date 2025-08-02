package com.secret.blackholeglow.models;

/**
 * Clase que representa un fondo de pantalla animado en la lista.
 */
public class WallpaperItem {

    private String nombre;
    private int resourceIdPreview; // ID de recurso de la imagen preview
    private String descripcion;

    public WallpaperItem(int resourceIdPreview, String nombre, String descripcion){
        this.nombre = nombre;
        this.resourceIdPreview = resourceIdPreview;
        this.descripcion = descripcion;
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
}