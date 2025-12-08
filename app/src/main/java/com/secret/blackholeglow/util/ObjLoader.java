// app/src/main/java/com/secret/blackholeglow/util/ObjLoader.java
package com.secret.blackholeglow.util;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import java.util.ArrayList;
import java.util.List;

public class ObjLoader {
    private static final String TAG = "Depurando";

    public static class Mesh {
        public final FloatBuffer vertexBuffer;      // Buffer GPU de vértices XYZ
        public final FloatBuffer uvBuffer;          // Buffer GPU de coords UV
        public final int vertexCount;      // Número de vértices (X,Y,Z)
        public final float[] originalVertices; // Array plano XYZ original
        public final List<int[]> faces;             // Lista de caras (int[] para modelos grandes)

        public Mesh(FloatBuffer vb, float[] verts, List<int[]> faceList,
                    FloatBuffer uvb, int vCount) {
            this.vertexBuffer = vb;
            this.originalVertices = verts;
            this.faces = faceList;
            this.uvBuffer = uvb;
            this.vertexCount = vCount;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Face data structure - Stores vertex AND UV indices
    // ═══════════════════════════════════════════════════════════
    public static class Face {
        public int[] vertexIndices;    // Indices to tmpVerts (int para modelos grandes >32k vértices)
        public int[] uvIndices;        // Indices to tmpUVs (puede ser null)

        public Face(int[] verts, int[] uvs) {
            this.vertexIndices = verts;
            this.uvIndices = uvs;
        }
    }

    public static Mesh loadObj(Context ctx, String assetPath) throws IOException {
        Log.d(TAG, "════════════════════════════════════════════════");
        Log.d(TAG, "ObjLoader: Cargando `" + assetPath + "`");
        Log.d(TAG, "════════════════════════════════════════════════");

        List<float[]> tmpVerts = new ArrayList<>();
        List<float[]> tmpUVs = new ArrayList<>();
        List<Face> faceList = new ArrayList<>();

        // 🔧 FIX: Usar try-with-resources para cerrar streams automáticamente
        try (InputStream is = ctx.getAssets().open(assetPath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.trim().split("\\s+");
                if (tokens.length < 1) continue;
                switch (tokens[0]) {
                    case "v":
                        // Vertice XYZ
                        float x = Float.parseFloat(tokens[1]);
                        float y = Float.parseFloat(tokens[2]);
                        float z = Float.parseFloat(tokens[3]);
                        tmpVerts.add(new float[]{x, y, z});
                        break;
                    case "vt":
                        // Coordenada UV
                        float u = Float.parseFloat(tokens[1]);
                        float v = Float.parseFloat(tokens[2]);
                        tmpUVs.add(new float[]{u, v});
                        break;
                    case "f":
                        // ═══════════════════════════════════════════════
                        // ✅ FIX: Parsear correctamente v/vt/vn
                        // ═══════════════════════════════════════════════
                        // Formato: f v1/vt1/vn1 v2/vt2/vn2 v3/vt3/vn3...
                        int nv = tokens.length - 1;
                        int[] vertIndices = new int[nv];  // ✅ int[] para modelos grandes
                        int[] uvIndices = new int[nv];    // ✅ int[] para modelos grandes
                        boolean hasUVs = true;

                        for (int i = 0; i < nv; i++) {
                            String[] parts = tokens[i + 1].split("/");

                            // Índice de vértice (siempre presente)
                            int vertIndex = Integer.parseInt(parts[0]) - 1;
                            vertIndices[i] = vertIndex;  // ✅ Sin cast a short

                            // Índice de UV (opcional - puede ser "v//vn" o "v/vt/vn")
                            if (parts.length >= 2 && !parts[1].isEmpty()) {
                                int uvIndex = Integer.parseInt(parts[1]) - 1;
                                uvIndices[i] = uvIndex;  // ✅ Sin cast a short
                            } else {
                                hasUVs = false;
                            }
                        }

                        faceList.add(new Face(vertIndices, hasUVs ? uvIndices : null));
                        break;
                    default:
                        // ignorar normales, comentarios, etc.
                        break;
                }
            }
        } // Streams se cierran automáticamente aquí

        int vCount = tmpVerts.size();
        Log.d(TAG, "ObjLoader: vértices leídos = " + vCount);
        Log.d(TAG, "ObjLoader: UVs leídos = " + tmpUVs.size());
        Log.d(TAG, "ObjLoader: Caras leídas = " + faceList.size());

        // ═══════════════════════════════════════════════════════════
        // ✅ EXPANDIR VÉRTICES PARA UV MAPPING CORRECTO
        // ═══════════════════════════════════════════════════════════
        // El problema: En OBJ, un mismo vértice puede tener diferentes UVs
        // dependiendo de la cara. Necesitamos crear vértices duplicados
        // para cada combinación única de vértice+UV.

        boolean hasValidUVs = !tmpUVs.isEmpty();

        // Contar total de vértices expandidos (cada vértice de cada cara = único)
        int expandedVertCount = 0;
        for (Face face : faceList) {
            expandedVertCount += face.vertexIndices.length;
        }

        Log.d(TAG, "ObjLoader: Expandiendo " + vCount + " vértices a " + expandedVertCount + " (para UVs correctos)");

        // Arrays expandidos
        float[] expandedVerts = new float[expandedVertCount * 3];
        float[] expandedUVs = new float[expandedVertCount * 2];

        // Nuevas caras con índices actualizados
        List<int[]> newFaceList = new ArrayList<>();

        int currentIndex = 0;
        for (Face face : faceList) {
            int[] newFaceIndices = new int[face.vertexIndices.length];

            for (int i = 0; i < face.vertexIndices.length; i++) {
                int vertIdx = face.vertexIndices[i];

                // Copiar posición del vértice
                float[] vert = tmpVerts.get(vertIdx);
                expandedVerts[currentIndex * 3] = vert[0];
                expandedVerts[currentIndex * 3 + 1] = vert[1];
                expandedVerts[currentIndex * 3 + 2] = vert[2];

                // Copiar UV
                if (hasValidUVs && face.uvIndices != null && face.uvIndices[i] >= 0 && face.uvIndices[i] < tmpUVs.size()) {
                    float[] uv = tmpUVs.get(face.uvIndices[i]);
                    expandedUVs[currentIndex * 2] = uv[0];
                    expandedUVs[currentIndex * 2 + 1] = uv[1];
                } else {
                    // Fallback: generar UV esférico
                    float[] sphericalUV = generateSphericalUV(vert[0], vert[1], vert[2]);
                    expandedUVs[currentIndex * 2] = sphericalUV[0];
                    expandedUVs[currentIndex * 2 + 1] = sphericalUV[1];
                }

                newFaceIndices[i] = currentIndex;
                currentIndex++;
            }

            newFaceList.add(newFaceIndices);
        }

        Log.d(TAG, "ObjLoader: ✓ Vértices expandidos: " + expandedVertCount);
        Log.d(TAG, "ObjLoader: ✓ UVs asignados correctamente");

        // Crear vertexBuffer con vértices expandidos
        FloatBuffer vBuf = ByteBuffer
                .allocateDirect(expandedVerts.length * Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(expandedVerts);
        vBuf.position(0);

        // Crear uvBuffer con UVs expandidos
        FloatBuffer uvBuf = ByteBuffer
                .allocateDirect(expandedUVs.length * Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(expandedUVs);
        uvBuf.position(0);

        Log.d(TAG, "ObjLoader: buffers preparados (vBuf, uvBuf).");

        Log.d(TAG, "════════════════════════════════════════════════");
        Log.d(TAG, "ObjLoader: ✓ Carga completada exitosamente");
        Log.d(TAG, "════════════════════════════════════════════════");

        return new Mesh(vBuf, expandedVerts, newFaceList, uvBuf, expandedVertCount);
    }

    /**
     * ═══════════════════════════════════════════════════════════
     * Genera coordenadas UV esféricas proceduralmente (MEJORADO)
     * ═══════════════════════════════════════════════════════════
     * Convierte coordenadas cartesianas (x,y,z) a UVs esféricos.
     *
     * MATEMÁTICA MEJORADA para evitar distorsión en polos:
     *
     * 1. Coordenadas esféricas:
     *    - θ (theta) = ángulo horizontal (azimuth)
     *    - φ (phi) = ángulo vertical (elevation)
     *
     * 2. Conversión a UV:
     *    - U = θ / (2π)          →  rango [0, 1]
     *    - V = (π - φ) / π       →  rango [0, 1]  (invertido para textura estándar)
     *
     * 3. Cálculo robusto de ángulos:
     *    - θ = atan2(-z, x)      ← Nota el signo negativo en Z para orientación correcta
     *    - φ = acos(y / r)       ← Más estable que asin en los polos
     *
     * VENTAJAS:
     *  ✅ No hay singularidades en polos (acos es más estable que asin)
     *  ✅ Distribución uniforme de UVs
     *  ✅ Compatible con texturas estándar (V=0 arriba, V=1 abajo)
     *
     * @param x Coordenada X del vértice
     * @param y Coordenada Y del vértice
     * @param z Coordenada Z del vértice
     * @return Array [u, v] con coordenadas UV en rango [0, 1]
     */
    private static float[] generateSphericalUV(float x, float y, float z) {
        // ═══════════════════════════════════════════════════════════
        // 1. NORMALIZACIÓN (manejar esferas de cualquier radio)
        // ═══════════════════════════════════════════════════════════
        float r = (float) Math.sqrt(x * x + y * y + z * z);
        if (r < 0.0001f) {
            // Centro de la esfera - UV arbitrario
            return new float[]{0.5f, 0.5f};
        }

        float nx = x / r;
        float ny = y / r;
        float nz = z / r;

        // ═══════════════════════════════════════════════════════════
        // 2. CALCULAR ÁNGULO HORIZONTAL (θ - theta)
        // ═══════════════════════════════════════════════════════════
        // atan2 devuelve rango [-π, π]
        // Usamos atan2(-z, x) para que la textura esté orientada correctamente
        // (frente de la esfera = centro de la textura)
        float theta = (float) Math.atan2(-nz, nx);

        // Convertir de [-π, π] a [0, 2π]
        if (theta < 0) {
            theta += (float) (2.0 * Math.PI);
        }

        // Normalizar a [0, 1]
        float u = theta / (float) (2.0 * Math.PI);

        // ═══════════════════════════════════════════════════════════
        // 3. CALCULAR ÁNGULO VERTICAL (φ - phi)
        // ═══════════════════════════════════════════════════════════
        // acos devuelve rango [0, π] (MÁS ESTABLE que asin en polos)
        // acos(y) da 0 en polo norte (+Y) y π en polo sur (-Y)

        // Clamp ny para evitar errores numéricos en acos
        float ny_clamped = Math.max(-1.0f, Math.min(1.0f, ny));
        float phi = (float) Math.acos(ny_clamped);

        // Normalizar a [0, 1]
        // V=0 en polo norte, V=1 en polo sur (convención estándar)
        float v = phi / (float) Math.PI;

        // ═══════════════════════════════════════════════════════════
        // 4. CLAMP FINAL (seguridad contra imprecisiones)
        // ═══════════════════════════════════════════════════════════
        u = Math.max(0.0f, Math.min(1.0f, u));
        v = Math.max(0.0f, Math.min(1.0f, v));

        return new float[]{u, v};
    }

    /**
     * Utility: Construye un ShortBuffer de índices a partir de una lista de caras.
     * Triangula polígonos usando fan triangulation.
     *
     * @param faces Lista de caras (cada cara es un array de índices de vértices)
     * @param indexCount Número total de índices (triCount * 3)
     * @return ShortBuffer listo para glDrawElements
     */
    public static ShortBuffer buildIndexBuffer(List<short[]> faces, int indexCount) {
        ShortBuffer ib = ByteBuffer
                .allocateDirect(indexCount * Short.BYTES)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();

        for (short[] face : faces) {
            // Fan triangulation: v0, v1, v2 -> v0, v2, v3 -> ...
            short v0 = face[0];
            for (int i = 1; i < face.length - 1; i++) {
                ib.put(v0);
                ib.put(face[i]);
                ib.put(face[i + 1]);
            }
        }

        ib.position(0);
        return ib;
    }
}
