package com.secret.blackholeglow.util;

import android.opengl.GLES30;
import android.util.Log;

/**
 * TextureConfig - Sistema centralizado de configuración de texturas OpenGL
 *
 * ════════════════════════════════════════════════════════════════════════
 * ✨ Propósito:
 * ════════════════════════════════════════════════════════════════════════
 * Estandarizar la configuración de texturas según su tipo y uso,
 * evitando errores comunes de wrapping y filtering.
 *
 * ════════════════════════════════════════════════════════════════════════
 * 📚 Tipos de Textura:
 * ════════════════════════════════════════════════════════════════════════
 *
 * AVATAR:
 *   - Fotos de perfil, imágenes únicas que NO deben repetirse
 *   - Wrapping: CLAMP_TO_EDGE (bordes fijos)
 *   - Filter: LINEAR (suave, sin mipmaps)
 *   - Uso: AvatarSphere, imágenes UI
 *
 * PLANET:
 *   - Texturas de planetas con uvScale variable
 *   - Wrapping: REPEAT (permite tiling)
 *   - Filter: LINEAR_MIPMAP_LINEAR (mejor calidad a distancia)
 *   - Uso: Planeta, esferas con texturas repetibles
 *
 * TILEABLE:
 *   - Texturas diseñadas para repetirse sin costuras
 *   - Wrapping: REPEAT (tiling infinito)
 *   - Filter: LINEAR_MIPMAP_LINEAR + mipmaps
 *   - Uso: Terrenos, backgrounds, patrones
 *
 * ════════════════════════════════════════════════════════════════════════
 * 💡 Ejemplo de uso:
 * ════════════════════════════════════════════════════════════════════════
 *
 * // En AvatarSphere
 * int textureId = ...;
 * TextureConfig.configure(textureId, TextureConfig.Type.AVATAR);
 *
 * // En Planeta
 * int textureId = ...;
 * TextureConfig.configure(textureId, TextureConfig.Type.PLANET);
 *
 * ════════════════════════════════════════════════════════════════════════
 */
public class TextureConfig {
    private static final String TAG = "TextureConfig";

    /**
     * Tipos de textura con configuraciones predefinidas
     */
    public enum Type {
        /**
         * AVATAR - Imágenes únicas que no se repiten
         * Ejemplo: Fotos de perfil, logos, imágenes UI
         */
        AVATAR,

        /**
         * PLANET - Texturas de planetas con tiling controlado
         * Ejemplo: Superficies planetarias, esferas texturizadas
         */
        PLANET,

        /**
         * TILEABLE - Texturas que se repiten infinitamente
         * Ejemplo: Terrenos, patrones, backgrounds repetitivos
         */
        TILEABLE
    }

    /**
     * Configura una textura OpenGL según su tipo
     *
     * @param textureId ID de la textura OpenGL generada por glGenTextures
     * @param type Tipo de textura (AVATAR, PLANET, TILEABLE)
     */
    public static void configure(int textureId, Type type) {
        if (textureId <= 0) {
            Log.e(TAG, "❌ TextureID inválido: " + textureId);
            return;
        }

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);

        switch (type) {
            case AVATAR:
                configureAvatar();
                Log.d(TAG, "✓ Textura configurada como AVATAR (CLAMP_TO_EDGE, no repeat) - ID: " + textureId);
                break;

            case PLANET:
                configurePlanet();
                Log.d(TAG, "✓ Textura configurada como PLANET (REPEAT, mipmaps) - ID: " + textureId);
                break;

            case TILEABLE:
                configureTileable();
                Log.d(TAG, "✓ Textura configurada como TILEABLE (REPEAT, mipmaps optimizados) - ID: " + textureId);
                break;

            default:
                Log.w(TAG, "⚠️ Tipo de textura desconocido, usando configuración por defecto");
                configureDefault();
                break;
        }

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
    }

    /**
     * ════════════════════════════════════════════════════════════════
     * AVATAR - No repetir, bordes fijos
     * ════════════════════════════════════════════════════════════════
     */
    private static void configureAvatar() {
        // Wrapping: Los UVs fuera de 0..1 toman el color del borde
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        // Filtering: Suave sin mipmaps (imágenes cercanas de alta calidad)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
    }

    /**
     * ════════════════════════════════════════════════════════════════
     * PLANET - Repetir con mipmaps para texturas a distancia
     * ════════════════════════════════════════════════════════════════
     */
    private static void configurePlanet() {
        // Wrapping: Repetir la textura (permite tiling con uvScale)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT);

        // Filtering: Mipmaps para mejor calidad a diferentes distancias
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);

        // Generar mipmaps
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);
    }

    /**
     * ════════════════════════════════════════════════════════════════
     * TILEABLE - Repetir infinitamente con mipmaps optimizados
     * ════════════════════════════════════════════════════════════════
     */
    private static void configureTileable() {
        // Wrapping: Repetir en ambas direcciones
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT);

        // Filtering: Mipmaps con trilinear filtering
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);

        // Generar mipmaps
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);
    }

    /**
     * ════════════════════════════════════════════════════════════════
     * DEFAULT - Configuración por defecto segura
     * ════════════════════════════════════════════════════════════════
     */
    private static void configureDefault() {
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
    }
}
