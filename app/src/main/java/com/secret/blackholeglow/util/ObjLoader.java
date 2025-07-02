// ╔════════════════════════════════════════════════════════════════════╗
// ║ 🌌 ObjLoader.java – Forjador de Mallas Cósmicas (OBJ Loader) 🌌   ║
// ║                                                                    ║
// ║  Este utilitario sagrado interpreta archivos OBJ simples (vértices ║
// ║  y caras) desde la carpeta assets/, convirtiéndolos en estructuras  ║
// ║  de datos listas para ser forjadas en buffers de GPU (FloatBuffer)  ║
// ║  y recorrer polígonos de N lados (caras arbitrarias).               ║
// ╚════════════════════════════════════════════════════════════════════╝

package com.secret.blackholeglow.util;

import android.content.Context;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import java.util.ArrayList;
import java.util.List;

/**
 * ╔═════════════════════════════════════════════════════════════╗
 * ║ 🛡 ObjLoader – Protector de la Geometría                    ║
 * ║  • Lee y parsea archivos .obj con líneas de vértices (v)   ║
 * ║    y caras (f) que pueden tener N vértices (polígonos).     ║
 * ║  • Devuelve un Mesh con:
 * ║      - FloatBuffer de vértices en formato intercalado XYZ.
 * ║      - Array plano de floats original (para regenerar datos).
 * ║      - Lista de caras, cada una como arreglo de índices cortos.
 * ╚═════════════════════════════════════════════════════════════╝
 */
public class ObjLoader {

    /**
     * ╔══════════════════════════════════════════════════════════╗
     * ║ 💠 Clase Interna: Mesh – Estructura Principal            ║
     * ║                                                          ║
     * ║  • FloatBuffer vertexBuffer:                           ║
     * ║      - Contiene los vértices XYZ en orden contiguo.     ╇
     * ║      - Se usa ByteOrder.nativeOrder() para compatibilidad║
     * ║        nativa con GPU.                                   ║
     * ║  • float[] originalVertices:                           ║
     * ║      - Copia de los vértices en un array de punto flotante║
     * ║        para conservar datos sin mutar.                   ║
     * ║  • List<short[]> faces:                                ║
     * ║      - Cada elemento es un polígono: índices de vértices.║
     * ║      - short se elige por ahorro de memoria y compatibilidad ║
     * ║        con glDrawElements.                               ║
     * ╚══════════════════════════════════════════════════════════╝
     */
    public static class Mesh {
        public final FloatBuffer vertexBuffer;      // Buffer GPU de vértices XYZ
        public final float[]     originalVertices;  // Array plano XY...Z original
        public final List<short[]> faces;           // Lista de caras (polígonos)

        /**
         * Constructor de Mesh.
         * @param vb       FloatBuffer con vértices intercalados (X, Y, Z).
         * @param verts    Array plano de floats [x0,y0,z0,...] para backup.
         * @param faceList Lista de caras, cada cara como short[].
         */
        public Mesh(FloatBuffer vb, float[] verts, List<short[]> faceList) {
            this.vertexBuffer     = vb;
            this.originalVertices = verts;
            this.faces            = faceList;
        }
    }

    /**
     * ╔════════════════════════════════════════════════════════╗
     * ║ 🎯 Método: loadObj() – Explorador de Archivos OBJ      ║
     * ║                                                        ║
     * ║ Lee un archivo OBJ desde assets y construye un Mesh.
     * ╚════════════════════════════════════════════════════════╝
     * @param ctx       Contexto Android para acceder a assets.
     * @param assetPath Nombre del archivo OBJ en assets/ (ej. "plane.obj").
     * @return          Mesh con vértices y caras.
     * @throws IOException Si falla la lectura de assets.
     */
    public static Mesh loadObj(Context ctx, String assetPath) throws IOException {
        // 🏰 Colecciones temporales para lectura:
        List<float[]> tmpVerts  = new ArrayList<>(); // Almacenará {x,y,z} por línea "v"
        List<short[]> faceList  = new ArrayList<>(); // Almacenará índices por línea "f"

        // 📖 Abrir flujo del asset obj:
        InputStream is = ctx.getAssets().open(assetPath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;

        // 🔍 Recorrer cada línea del OBJ:
        while ((line = reader.readLine()) != null) {
            // Dividir tokens por espacios (\s+)
            String[] tokens = line.trim().split("\\s+");
            if (tokens.length < 1) continue;

            switch (tokens[0]) {
                case "v": // ══ Vértice: "v x y z"
                    // Parsear coordenadas x, y, z como float
                    float x = Float.parseFloat(tokens[1]);
                    float y = Float.parseFloat(tokens[2]);
                    float z = Float.parseFloat(tokens[3]);
                    // Agregar al listado temporal
                    tmpVerts.add(new float[]{ x, y, z });
                    break;

                case "f": // ══ Cara: "f i1 i2 i3 ... iN"
                    int nv = tokens.length - 1;         // Número de vértices en la cara
                    short[] idx = new short[nv];       // Array de índices
                    for (int i = 0; i < nv; i++) {
                        // Token puede ser "i/j/k". Nos interesa el índice i.
                        String vertex = tokens[i+1].split("/")[0];
                        idx[i] = (short)(Short.parseShort(vertex) - 1); // OBJ es 1-based
                    }
                    faceList.add(idx);
                    break;

                default:
                    // Omitir líneas comentadas (#) u otras directivas p.ej. "mtllib"
                    break;
            }
        }
        reader.close(); // Cerrar lector para liberar recursos

        // 🔨 Aplanar lista de vértices en array de floats contiguo:
        float[] vertsArr = new float[tmpVerts.size() * 3];
        for (int i = 0; i < tmpVerts.size(); i++) {
            float[] v = tmpVerts.get(i);
            vertsArr[i*3]   = v[0];
            vertsArr[i*3+1] = v[1];
            vertsArr[i*3+2] = v[2];
        }

        // ⚡ Crear FloatBuffer con ByteBuffer nativo:
        //    - allocateDirect para evitar copia Java heap → nativo
        //    - order(ByteOrder.nativeOrder()) para compatibilidad CPU/GPU
        FloatBuffer vBuf = ByteBuffer
                .allocateDirect(vertsArr.length * Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertsArr);
        vBuf.position(0); // Resetea posición del buffer a inicio

        // 🏅 Retornar Mesh con datos listos para glVertexAttribPointer
        return new Mesh(vBuf, vertsArr, faceList);
    }
}
