package com.secret.blackholeglow.models;

/**
 * Clasificacion de peso de recursos de una escena.
 * Usado para estimar consumo de memoria GPU y decidir si mostrar warnings OOM.
 */
public enum SceneWeight {
    LIGHT,   // Imagen estática o shader simple (<20 MB GPU)
    MEDIUM,  // Video background o modelos simples (20-50 MB GPU)
    HEAVY    // Modelos 3D + texturas + video (>50 MB GPU)
}
