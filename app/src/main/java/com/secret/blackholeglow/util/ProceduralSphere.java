package com.secret.blackholeglow.util;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════
 * ProceduralSphere - Generador de Esferas con UVs Perfectos
 * ═══════════════════════════════════════════════════════════════
 *
 * Genera esferas matemáticamente con UVs correctos y sin seams problemáticos.
 *
 * VENTAJAS vs archivo .obj:
 *  ✅ UVs perfectos sin distorsión en polos
 *  ✅ No hay problemas de seam UV
 *  ✅ Control total de resolución (latitudes/longitudes)
 *  ✅ Genera normales correctas automáticamente
 *  ✅ No depende de archivos externos
 *
 * CUÁNDO USAR:
 *  - Planetas y cuerpos celestes esféricos
 *  - Bolas disco y objetos decorativos esféricos
 *  - Cualquier esfera donde la textura sea importante
 *
 * CUÁNDO NO USAR:
 *  - Modelos complejos no esféricos (usar Blender + ObjLoader)
 *  - Objetos con geometría irregular
 */
public class ProceduralSphere {
    private static final String TAG = "ProceduralSphere";

    /**
     * Mesh data - Compatible con ObjLoader.Mesh
     */
    public static class Mesh {
        public final FloatBuffer vertexBuffer;   // XYZ positions
        public final FloatBuffer uvBuffer;       // UV coordinates
        public final FloatBuffer normalBuffer;   // Normal vectors
        public final ShortBuffer indexBuffer;    // Triangle indices
        public final int vertexCount;
        public final int indexCount;

        public Mesh(FloatBuffer vb, FloatBuffer uvb, FloatBuffer nb, ShortBuffer ib,
                    int vCount, int iCount) {
            this.vertexBuffer = vb;
            this.uvBuffer = uvb;
            this.normalBuffer = nb;
            this.indexBuffer = ib;
            this.vertexCount = vCount;
            this.indexCount = iCount;
        }
    }

    /**
     * ═══════════════════════════════════════════════════════════
     * Genera una esfera procedural con UVs perfectos
     * ═══════════════════════════════════════════════════════════
     *
     * @param radius Radio de la esfera
     * @param latitudes Número de divisiones verticales (más = más suave)
     * @param longitudes Número de divisiones horizontales (más = más suave)
     * @return Mesh con todos los datos necesarios para renderizar
     *
     * RECOMENDACIONES:
     *  - Para esferas simples: 16-24 latitudes, 32-48 longitudes
     *  - Para esferas detalladas: 32 latitudes, 64 longitudes
     *  - Más polígonos = mejor calidad pero menor rendimiento
     */
    public static Mesh generate(float radius, int latitudes, int longitudes) {
        // 🔧 FIX: Validación de parámetros para evitar división por cero
        if (radius <= 0) {
            throw new IllegalArgumentException("Radio debe ser mayor a 0, recibido: " + radius);
        }
        if (latitudes < 2) {
            throw new IllegalArgumentException("Latitudes debe ser >= 2, recibido: " + latitudes);
        }
        if (longitudes < 3) {
            throw new IllegalArgumentException("Longitudes debe ser >= 3, recibido: " + longitudes);
        }

        Log.d(TAG, "════════════════════════════════════════════════");
        Log.d(TAG, "Generando esfera procedural:");
        Log.d(TAG, "  Radio: " + radius);
        Log.d(TAG, "  Latitudes: " + latitudes);
        Log.d(TAG, "  Longitudes: " + longitudes);

        // ═══════════════════════════════════════════════════════════
        // 1. GENERAR VÉRTICES, UVs Y NORMALES
        // ═══════════════════════════════════════════════════════════
        List<Float> vertices = new ArrayList<>();
        List<Float> uvs = new ArrayList<>();
        List<Float> normals = new ArrayList<>();

        // Recorrer latitudes (vertical - de polo norte a polo sur)
        for (int lat = 0; lat <= latitudes; lat++) {
            // Ángulo vertical (theta): 0 en polo norte, π en polo sur
            float theta = (float) lat / latitudes * (float) Math.PI;
            float sinTheta = (float) Math.sin(theta);
            float cosTheta = (float) Math.cos(theta);

            // Recorrer longitudes (horizontal - alrededor del ecuador)
            for (int lon = 0; lon <= longitudes; lon++) {
                // Ángulo horizontal (phi): 0 a 2π alrededor de la esfera
                float phi = (float) lon / longitudes * 2.0f * (float) Math.PI;
                float sinPhi = (float) Math.sin(phi);
                float cosPhi = (float) Math.cos(phi);

                // ═══════════════════════════════════════════════════
                // POSICIÓN del vértice (coordenadas esféricas → cartesianas)
                // ═══════════════════════════════════════════════════
                float x = radius * cosPhi * sinTheta;
                float y = radius * cosTheta;
                float z = radius * sinPhi * sinTheta;

                vertices.add(x);
                vertices.add(y);
                vertices.add(z);

                // ═══════════════════════════════════════════════════
                // NORMAL (para esferas centradas en origen, normal = posición normalizada)
                // ═══════════════════════════════════════════════════
                normals.add(x / radius);
                normals.add(y / radius);
                normals.add(z / radius);

                // ═══════════════════════════════════════════════════
                // UVs (mapeo esférico natural)
                // ═══════════════════════════════════════════════════
                // U: recorre horizontalmente [0, 1]
                // V: recorre verticalmente [0, 1] (0=polo norte, 1=polo sur)
                float u = (float) lon / longitudes;
                float v = (float) lat / latitudes;

                uvs.add(u);
                uvs.add(v);
            }
        }

        int vertexCount = vertices.size() / 3;
        Log.d(TAG, "  Vértices generados: " + vertexCount);

        // ═══════════════════════════════════════════════════════════
        // 2. GENERAR ÍNDICES (triángulos)
        // ═══════════════════════════════════════════════════════════
        List<Short> indices = new ArrayList<>();

        for (int lat = 0; lat < latitudes; lat++) {
            for (int lon = 0; lon < longitudes; lon++) {
                // Índices de los 4 vértices del quad actual
                int first = (lat * (longitudes + 1)) + lon;
                int second = first + longitudes + 1;

                // Primer triángulo del quad (v0, v1, v2)
                indices.add((short) first);
                indices.add((short) second);
                indices.add((short) (first + 1));

                // Segundo triángulo del quad (v2, v1, v3)
                indices.add((short) (second));
                indices.add((short) (second + 1));
                indices.add((short) (first + 1));
            }
        }

        int indexCount = indices.size();
        Log.d(TAG, "  Índices generados: " + indexCount + " (" + (indexCount / 3) + " triángulos)");

        // ═══════════════════════════════════════════════════════════
        // 3. CONVERTIR A BUFFERS (formato GPU)
        // ═══════════════════════════════════════════════════════════

        // Vertex buffer
        float[] vertexArray = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            vertexArray[i] = vertices.get(i);
        }
        FloatBuffer vertexBuffer = createFloatBuffer(vertexArray);

        // UV buffer
        float[] uvArray = new float[uvs.size()];
        for (int i = 0; i < uvs.size(); i++) {
            uvArray[i] = uvs.get(i);
        }
        FloatBuffer uvBuffer = createFloatBuffer(uvArray);

        // Normal buffer
        float[] normalArray = new float[normals.size()];
        for (int i = 0; i < normals.size(); i++) {
            normalArray[i] = normals.get(i);
        }
        FloatBuffer normalBuffer = createFloatBuffer(normalArray);

        // Index buffer
        short[] indexArray = new short[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            indexArray[i] = indices.get(i);
        }
        ShortBuffer indexBuffer = createShortBuffer(indexArray);

        Log.d(TAG, "════════════════════════════════════════════════");
        Log.d(TAG, "✓ Esfera procedural generada exitosamente");
        Log.d(TAG, "════════════════════════════════════════════════");

        return new Mesh(vertexBuffer, uvBuffer, normalBuffer, indexBuffer,
                vertexCount, indexCount);
    }

    /**
     * ═══════════════════════════════════════════════════════════
     * Helper: Crear FloatBuffer para GPU
     * ═══════════════════════════════════════════════════════════
     */
    private static FloatBuffer createFloatBuffer(float[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(data);
        fb.position(0);
        return fb;
    }

    /**
     * ═══════════════════════════════════════════════════════════
     * Helper: Crear ShortBuffer para GPU
     * ═══════════════════════════════════════════════════════════
     */
    private static ShortBuffer createShortBuffer(short[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 2);
        bb.order(ByteOrder.nativeOrder());
        ShortBuffer sb = bb.asShortBuffer();
        sb.put(data);
        sb.position(0);
        return sb;
    }

    /**
     * ═══════════════════════════════════════════════════════════
     * Presets comunes para diferentes casos de uso
     * ═══════════════════════════════════════════════════════════
     */

    /**
     * Esfera de baja resolución (para objetos pequeños o lejanos)
     */
    public static Mesh generateLowPoly(float radius) {
        return generate(radius, 8, 16);
    }

    /**
     * Esfera de resolución media (balance entre calidad y rendimiento)
     */
    public static Mesh generateMedium(float radius) {
        return generate(radius, 16, 32);
    }

    /**
     * ⚡ Esfera OPTIMIZADA - Balance perfecto entre calidad visual y rendimiento
     * 12 lat × 24 lon = 576 triángulos (vs 256 LowPoly, vs 1024 Medium)
     */
    public static Mesh generateOptimized(float radius) {
        return generate(radius, 12, 24);
    }

    /**
     * Esfera de alta resolución (para objetos grandes o con texturas detalladas)
     */
    public static Mesh generateHigh(float radius) {
        return generate(radius, 32, 64);
    }

    /**
     * Esfera ultra detallada (para casos especiales - cuidado con rendimiento)
     */
    public static Mesh generateUltra(float radius) {
        return generate(radius, 64, 128);
    }
}
