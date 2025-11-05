// app/src/main/java/com/secret/blackholeglow/util/MtlLoader.java
package com.secret.blackholeglow.util;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * MtlLoader - Carga archivos .mtl exportados desde Blender
 *
 * Parsea materiales con:
 * - Colores difusos (Kd)
 * - Colores especulares (Ks)
 * - Texturas difusas (map_Kd)
 * - Transparencia (d / Tr)
 */
public class MtlLoader {
    private static final String TAG = "MtlLoader";

    /**
     * Clase que representa un material
     */
    public static class Material {
        public String name;                // Nombre del material
        public float[] diffuseColor;       // Color difuso RGB [0-1]
        public float[] specularColor;      // Color especular RGB [0-1]
        public float[] ambientColor;       // Color ambiente RGB [0-1]
        public float alpha;                // Transparencia [0-1] (1 = opaco)
        public float shininess;            // Brillo especular
        public String diffuseTexture;      // Ruta a textura difusa (map_Kd)
        public String specularTexture;     // Ruta a textura especular (map_Ks)
        public String normalTexture;       // Ruta a textura de normales (map_bump)

        public Material(String name) {
            this.name = name;
            this.diffuseColor = new float[]{0.8f, 0.8f, 0.8f}; // Gris por defecto
            this.specularColor = new float[]{1.0f, 1.0f, 1.0f}; // Blanco
            this.ambientColor = new float[]{0.2f, 0.2f, 0.2f}; // Gris oscuro
            this.alpha = 1.0f; // Opaco por defecto
            this.shininess = 32.0f;
            this.diffuseTexture = null;
            this.specularTexture = null;
            this.normalTexture = null;
        }

        @Override
        public String toString() {
            return String.format("Material{name='%s', diffuse=%s, texture='%s', alpha=%.2f}",
                    name,
                    colorToString(diffuseColor),
                    diffuseTexture != null ? diffuseTexture : "none",
                    alpha);
        }

        private String colorToString(float[] color) {
            return String.format("(%.2f, %.2f, %.2f)", color[0], color[1], color[2]);
        }
    }

    /**
     * Carga todos los materiales desde un archivo .mtl
     *
     * @param ctx Context de Android
     * @param mtlPath Ruta del archivo .mtl en assets (ej: "modelo.mtl")
     * @return Mapa de nombre → Material
     */
    public static Map<String, Material> loadMtl(Context ctx, String mtlPath) throws IOException {
        Log.d(TAG, "════════════════════════════════════════════════");
        Log.d(TAG, "MtlLoader: Cargando materiales desde `" + mtlPath + "`");
        Log.d(TAG, "════════════════════════════════════════════════");

        Map<String, Material> materials = new HashMap<>();
        Material currentMaterial = null;

        InputStream is = ctx.getAssets().open(mtlPath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;

        while ((line = reader.readLine()) != null) {
            line = line.trim();

            // Ignorar líneas vacías y comentarios
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] tokens = line.split("\\s+");
            if (tokens.length < 1) continue;

            switch (tokens[0]) {
                case "newmtl":
                    // Nuevo material
                    if (tokens.length >= 2) {
                        String materialName = tokens[1];
                        currentMaterial = new Material(materialName);
                        materials.put(materialName, currentMaterial);
                        Log.d(TAG, "  📦 Material nuevo: " + materialName);
                    }
                    break;

                case "Ka":
                    // Color ambiente (Ambient)
                    if (currentMaterial != null && tokens.length >= 4) {
                        currentMaterial.ambientColor = new float[]{
                                Float.parseFloat(tokens[1]),
                                Float.parseFloat(tokens[2]),
                                Float.parseFloat(tokens[3])
                        };
                    }
                    break;

                case "Kd":
                    // Color difuso (Diffuse)
                    if (currentMaterial != null && tokens.length >= 4) {
                        currentMaterial.diffuseColor = new float[]{
                                Float.parseFloat(tokens[1]),
                                Float.parseFloat(tokens[2]),
                                Float.parseFloat(tokens[3])
                        };
                        Log.d(TAG, "    🎨 Color difuso: " + currentMaterial.colorToString(currentMaterial.diffuseColor));
                    }
                    break;

                case "Ks":
                    // Color especular (Specular)
                    if (currentMaterial != null && tokens.length >= 4) {
                        currentMaterial.specularColor = new float[]{
                                Float.parseFloat(tokens[1]),
                                Float.parseFloat(tokens[2]),
                                Float.parseFloat(tokens[3])
                        };
                    }
                    break;

                case "Ns":
                    // Exponente especular (Shininess)
                    if (currentMaterial != null && tokens.length >= 2) {
                        currentMaterial.shininess = Float.parseFloat(tokens[1]);
                    }
                    break;

                case "d":
                    // Transparencia (dissolve) - 1.0 = opaco, 0.0 = transparente
                    if (currentMaterial != null && tokens.length >= 2) {
                        currentMaterial.alpha = Float.parseFloat(tokens[1]);
                    }
                    break;

                case "Tr":
                    // Transparencia alternativa (inverse dissolve)
                    if (currentMaterial != null && tokens.length >= 2) {
                        currentMaterial.alpha = 1.0f - Float.parseFloat(tokens[1]);
                    }
                    break;

                case "map_Kd":
                    // Textura difusa
                    if (currentMaterial != null && tokens.length >= 2) {
                        // Tomar el último token (nombre del archivo)
                        currentMaterial.diffuseTexture = tokens[tokens.length - 1];
                        Log.d(TAG, "    🖼️  Textura difusa: " + currentMaterial.diffuseTexture);
                    }
                    break;

                case "map_Ks":
                    // Textura especular
                    if (currentMaterial != null && tokens.length >= 2) {
                        currentMaterial.specularTexture = tokens[tokens.length - 1];
                    }
                    break;

                case "map_bump":
                case "bump":
                    // Textura de normales/bump
                    if (currentMaterial != null && tokens.length >= 2) {
                        currentMaterial.normalTexture = tokens[tokens.length - 1];
                    }
                    break;

                default:
                    // Ignorar otras propiedades (illum, etc.)
                    break;
            }
        }

        reader.close();

        Log.d(TAG, "════════════════════════════════════════════════");
        Log.d(TAG, "MtlLoader: ✓ " + materials.size() + " materiales cargados");
        for (Material mat : materials.values()) {
            Log.d(TAG, "  " + mat.toString());
        }
        Log.d(TAG, "════════════════════════════════════════════════");

        return materials;
    }

    /**
     * Extrae solo el nombre del archivo de una ruta
     * Ej: "textures/wood.png" → "wood.png"
     */
    public static String getFileName(String path) {
        if (path == null) return null;
        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }
}
