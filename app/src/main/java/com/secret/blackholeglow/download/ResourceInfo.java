package com.secret.blackholeglow.download;

/**
 * ============================================================================
 *                           ResourceInfo
 *              Metadata de un recurso remoto en Supabase
 * ============================================================================
 *
 * Clase compartida por VideoConfig, ImageConfig y ModelConfig.
 * Almacena información sobre recursos descargables.
 *
 * CAMPOS:
 *   - url:         URL completa en Supabase Storage
 *   - sizeBytes:   Tamaño esperado para validación de descarga
 *   - displayName: Nombre legible para UI/logs
 *   - version:     Versión para auto-actualización (incrementar al reemplazar)
 *
 * ============================================================================
 */
public class ResourceInfo {

    public final String url;
    public final long sizeBytes;
    public final String displayName;
    public final int version;

    /**
     * Constructor con versión por defecto (1).
     */
    public ResourceInfo(String url, long sizeBytes, String displayName) {
        this(url, sizeBytes, displayName, 1);
    }

    /**
     * Constructor completo.
     *
     * @param url         URL completa del recurso en Supabase
     * @param sizeBytes   Tamaño esperado en bytes (para validación)
     * @param displayName Nombre descriptivo para logs/UI
     * @param version     Versión del recurso (incrementar al actualizar en Supabase)
     */
    public ResourceInfo(String url, long sizeBytes, String displayName, int version) {
        this.url = url;
        this.sizeBytes = sizeBytes;
        this.displayName = displayName;
        this.version = version;
    }

    @Override
    public String toString() {
        return "ResourceInfo{" +
                "displayName='" + displayName + '\'' +
                ", version=" + version +
                ", size=" + (sizeBytes / 1024) + "KB" +
                '}';
    }
}
