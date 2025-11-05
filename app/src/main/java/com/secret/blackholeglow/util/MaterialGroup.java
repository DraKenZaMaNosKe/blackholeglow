// app/src/main/java/com/secret/blackholeglow/util/MaterialGroup.java
package com.secret.blackholeglow.util;

import java.util.ArrayList;
import java.util.List;

/**
 * MaterialGroup - Agrupa caras que comparten el mismo material
 *
 * Usado por ObjLoader cuando un modelo tiene múltiples materiales.
 * Cada grupo representa un conjunto de caras que deben renderizarse
 * con el mismo material/textura.
 */
public class MaterialGroup {
    public String materialName;                    // Nombre del material (ej: "material_01")
    public MtlLoader.Material material;            // Propiedades del material (colores, texturas)
    public List<ObjLoader.Face> faces;             // Lista de caras de este grupo
    public int faceStartIndex;                     // Índice de inicio en el buffer de índices
    public int faceCount;                          // Número de caras en este grupo

    public MaterialGroup(String materialName) {
        this.materialName = materialName;
        this.faces = new ArrayList<>();
        this.material = null;
        this.faceStartIndex = 0;
        this.faceCount = 0;
    }

    /**
     * Calcula el número de triángulos en este grupo
     * (considerando fan triangulation para polígonos)
     */
    public int getTriangleCount() {
        int triangles = 0;
        for (ObjLoader.Face face : faces) {
            // Fan triangulation: n vértices = n-2 triángulos
            triangles += Math.max(0, face.vertexIndices.length - 2);
        }
        return triangles;
    }

    @Override
    public String toString() {
        return String.format("MaterialGroup{name='%s', faces=%d, triangles=%d, material=%s}",
                materialName,
                faces.size(),
                getTriangleCount(),
                material != null ? material.name : "null");
    }
}
