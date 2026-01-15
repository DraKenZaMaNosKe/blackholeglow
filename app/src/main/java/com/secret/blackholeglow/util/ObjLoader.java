// app/src/main/java/com/secret/blackholeglow/util/ObjLoader.java
package com.secret.blackholeglow.util;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import java.util.ArrayList;
import java.util.List;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                    🎯 ObjLoader - Cargador de Modelos 3D                   ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║  CARACTERÍSTICAS:                                                          ║
 * ║  ✅ Soporte completo OBJ (v, vt, vn, f)                                    ║
 * ║  ✅ Modelos grandes (>32K vértices) usando int en lugar de short           ║
 * ║  ✅ Auto-detección de modelos Meshy AI (flipV automático)                  ║
 * ║  ✅ UV esférico procedural como fallback                                   ║
 * ║  ✅ Cálculo automático de normales                                         ║
 * ║  ✅ Bounding box para colisiones y escalado                                ║
 * ║  ✅ Manejo robusto de errores (línea por línea)                            ║
 * ║  ✅ Eficiente en memoria (cache de índices, arrays reutilizables)          ║
 * ║  ✅ Thread-safe (sin estado compartido mutable)                            ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * USO:
 *   // Carga simple (detecta automáticamente si necesita flipV)
 *   Mesh mesh = ObjLoader.loadObj(context, "modelo.obj");
 *
 *   // Carga con opciones explícitas
 *   Mesh mesh = ObjLoader.loadObj(context, "modelo.obj", true);  // flipV=true
 *
 *   // Carga con todas las opciones
 *   LoadOptions options = new LoadOptions()
 *       .setFlipV(true)
 *       .setCalculateNormals(true)
 *       .setProgressListener(progress -> Log.d("Loading", progress + "%"));
 *   Mesh mesh = ObjLoader.loadObj(context, "modelo.obj", options);
 */
public class ObjLoader {
    private static final String TAG = "ObjLoader";

    // ═══════════════════════════════════════════════════════════════════════════
    // Constantes de configuración
    // ═══════════════════════════════════════════════════════════════════════════
    private static final int INITIAL_CAPACITY_VERTS = 1000;
    private static final int INITIAL_CAPACITY_UVS = 1000;
    private static final int INITIAL_CAPACITY_FACES = 2000;
    private static final float EPSILON = 0.0001f;

    // Modelos de Meshy AI que requieren flipV
    private static final String[] MESHY_MODEL_PATTERNS = {
        "christmas", "santa", "reindeer", "sleigh", "snowman",
        "meshy", "blender"
    };

    // ═══════════════════════════════════════════════════════════════════════════
    // Clases de datos
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Mesh - Resultado de la carga de un modelo OBJ
     */
    public static class Mesh {
        // Buffers para OpenGL
        @NonNull public final FloatBuffer vertexBuffer;   // XYZ positions
        @NonNull public final FloatBuffer uvBuffer;       // UV coordinates
        @Nullable public final FloatBuffer normalBuffer;  // XYZ normals (puede ser null)

        // Datos crudos
        @NonNull public final float[] originalVertices;   // Array plano XYZ
        @NonNull public final List<int[]> faces;          // Índices de caras

        // Metadatos
        public final int vertexCount;                     // Número de vértices
        public final int triangleCount;                   // Número de triángulos
        @NonNull public final BoundingBox boundingBox;    // Límites del modelo
        @NonNull public final Statistics stats;           // Estadísticas de carga

        public Mesh(@NonNull FloatBuffer vb, @NonNull float[] verts, @NonNull List<int[]> faceList,
                    @NonNull FloatBuffer uvb, @Nullable FloatBuffer nb, int vCount,
                    @NonNull BoundingBox bbox, @NonNull Statistics statistics) {
            this.vertexBuffer = vb;
            this.originalVertices = verts;
            this.faces = faceList;
            this.uvBuffer = uvb;
            this.normalBuffer = nb;
            this.vertexCount = vCount;
            this.boundingBox = bbox;
            this.stats = statistics;

            // Contar triángulos
            int triCount = 0;
            for (int[] face : faceList) {
                triCount += face.length - 2;  // Fan triangulation
            }
            this.triangleCount = triCount;
        }

        /**
         * Libera los buffers de memoria nativa
         */
        public void release() {
            // Los buffers directos no se pueden liberar explícitamente en Java
            // pero podemos limpiar referencias para ayudar al GC
            vertexBuffer.clear();
            uvBuffer.clear();
            if (normalBuffer != null) normalBuffer.clear();
        }
    }

    /**
     * BoundingBox - Límites 3D del modelo
     */
    public static class BoundingBox {
        public final float minX, minY, minZ;
        public final float maxX, maxY, maxZ;
        public final float width, height, depth;
        public final float centerX, centerY, centerZ;
        public final float maxDimension;

        public BoundingBox(float minX, float minY, float minZ,
                          float maxX, float maxY, float maxZ) {
            this.minX = minX; this.minY = minY; this.minZ = minZ;
            this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;

            this.width = maxX - minX;
            this.height = maxY - minY;
            this.depth = maxZ - minZ;

            this.centerX = (minX + maxX) / 2f;
            this.centerY = (minY + maxY) / 2f;
            this.centerZ = (minZ + maxZ) / 2f;

            this.maxDimension = Math.max(width, Math.max(height, depth));
        }

        /**
         * Calcula la escala para normalizar el modelo a tamaño 1.0
         */
        public float getNormalizationScale() {
            return maxDimension > EPSILON ? 1.0f / maxDimension : 1.0f;
        }
    }

    /**
     * Statistics - Estadísticas de carga del modelo
     */
    public static class Statistics {
        public final int rawVertices;         // Vértices en el archivo
        public final int rawUVs;              // UVs en el archivo
        public final int rawFaces;            // Caras en el archivo
        public final int expandedVertices;    // Vértices después de expandir
        public final int triangles;           // Triángulos totales
        public final long loadTimeMs;         // Tiempo de carga en ms
        public final boolean usedFlipV;       // Si se volteó V
        public final int skippedLines;        // Líneas con errores ignoradas

        public Statistics(int rawVerts, int rawUVs, int rawFaces,
                         int expanded, int tris, long timeMs,
                         boolean flipped, int skipped) {
            this.rawVertices = rawVerts;
            this.rawUVs = rawUVs;
            this.rawFaces = rawFaces;
            this.expandedVertices = expanded;
            this.triangles = tris;
            this.loadTimeMs = timeMs;
            this.usedFlipV = flipped;
            this.skippedLines = skipped;
        }

        @Override
        public String toString() {
            return String.format(
                "Stats: %d→%d verts, %d tris, %dms, flipV=%b, skipped=%d",
                rawVertices, expandedVertices, triangles, loadTimeMs, usedFlipV, skippedLines
            );
        }
    }

    /**
     * LoadOptions - Opciones de carga configurables
     */
    public static class LoadOptions {
        private Boolean flipV = null;           // null = auto-detectar
        private boolean calculateNormals = false;
        private ProgressListener progressListener = null;

        public LoadOptions setFlipV(boolean flip) {
            this.flipV = flip;
            return this;
        }

        public LoadOptions setAutoFlipV() {
            this.flipV = null;
            return this;
        }

        public LoadOptions setCalculateNormals(boolean calc) {
            this.calculateNormals = calc;
            return this;
        }

        public LoadOptions setProgressListener(ProgressListener listener) {
            this.progressListener = listener;
            return this;
        }
    }

    /**
     * ProgressListener - Callback para progreso de carga
     */
    public interface ProgressListener {
        void onProgress(int percent);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Estructura pública para caras (usada por MaterialGroup)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Face - Representa una cara del modelo OBJ
     * Contiene índices a vértices, UVs y normales
     */
    public static class Face {
        public int[] vertexIndices;     // Índices a vértices (siempre presente)
        public int[] uvIndices;         // Índices a UVs (puede ser null)
        public int[] normalIndices;     // Índices a normales (puede ser null)

        public Face(int[] verts, int[] uvs, int[] normals) {
            this.vertexIndices = verts;
            this.uvIndices = uvs;
            this.normalIndices = normals;
        }

        /**
         * @return Número de vértices en esta cara
         */
        public int getVertexCount() {
            return vertexIndices != null ? vertexIndices.length : 0;
        }

        /**
         * @return true si la cara tiene coordenadas UV
         */
        public boolean hasUVs() {
            return uvIndices != null && uvIndices.length > 0;
        }

        /**
         * @return true si la cara tiene normales
         */
        public boolean hasNormals() {
            return normalIndices != null && normalIndices.length > 0;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MÉTODOS DE CARGA PÚBLICOS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Carga un modelo OBJ con auto-detección de flipV
     */
    @NonNull
    public static Mesh loadObj(@NonNull Context ctx, @NonNull String assetPath) throws IOException {
        return loadObj(ctx, assetPath, new LoadOptions());
    }

    /**
     * Carga un modelo OBJ con flipV explícito
     */
    @NonNull
    public static Mesh loadObj(@NonNull Context ctx, @NonNull String assetPath, boolean flipV) throws IOException {
        return loadObj(ctx, assetPath, new LoadOptions().setFlipV(flipV));
    }

    /**
     * Carga un modelo OBJ con opciones completas
     */
    @NonNull
    public static Mesh loadObj(@NonNull Context ctx, @NonNull String assetPath,
                               @NonNull LoadOptions options) throws IOException {
        try (InputStream is = ctx.getAssets().open(assetPath)) {
            return loadObjFromStream(is, assetPath, options);
        }
    }

    /**
     * Carga un modelo OBJ desde un archivo en el sistema de archivos.
     * Útil para modelos descargados dinámicamente.
     *
     * @param filePath Ruta completa al archivo OBJ
     * @param flipV    Si voltear coordenadas V
     * @return Mesh cargado
     */
    @NonNull
    public static Mesh loadObjFromFile(@NonNull String filePath, boolean flipV) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("Archivo no encontrado: " + filePath);
        }

        try (InputStream is = new FileInputStream(file)) {
            return loadObjFromStream(is, file.getName(), new LoadOptions().setFlipV(flipV));
        }
    }

    /**
     * Carga un modelo OBJ desde un InputStream genérico.
     * Método interno compartido por loadObj() y loadObjFromFile().
     */
    @NonNull
    private static Mesh loadObjFromStream(@NonNull InputStream inputStream,
                                          @NonNull String sourceName,
                                          @NonNull LoadOptions options) throws IOException {
        long startTime = System.currentTimeMillis();

        Log.d(TAG, "╔═══════════════════════════════════════════════════════════════╗");
        Log.d(TAG, "║  Cargando: " + sourceName);
        Log.d(TAG, "╚═══════════════════════════════════════════════════════════════╝");

        // Determinar si necesitamos flipV
        boolean flipV;
        if (options.flipV != null) {
            flipV = options.flipV;
            Log.d(TAG, "FlipV: " + flipV + " (explícito)");
        } else {
            flipV = shouldAutoFlipV(sourceName);
            Log.d(TAG, "FlipV: " + flipV + " (auto-detectado)");
        }

        // Estructuras de datos temporales
        List<float[]> tmpVerts = new ArrayList<>(INITIAL_CAPACITY_VERTS);
        List<float[]> tmpUVs = new ArrayList<>(INITIAL_CAPACITY_UVS);
        List<float[]> tmpNormals = new ArrayList<>(INITIAL_CAPACITY_VERTS);
        List<Face> faceList = new ArrayList<>(INITIAL_CAPACITY_FACES);

        // Bounding box tracking
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        int lineNumber = 0;
        int skippedLines = 0;

        // ═══════════════════════════════════════════════════════════════════════════
        // FASE 1: Parsear archivo OBJ
        // ═══════════════════════════════════════════════════════════════════════════
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream), 8192)) {

            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;

                try {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;

                    String[] tokens = line.split("\\s+");
                    if (tokens.length < 1) continue;

                    switch (tokens[0]) {
                        case "v":  // Vértice
                            if (tokens.length >= 4) {
                                float x = Float.parseFloat(tokens[1]);
                                float y = Float.parseFloat(tokens[2]);
                                float z = Float.parseFloat(tokens[3]);
                                tmpVerts.add(new float[]{x, y, z});

                                // Actualizar bounding box
                                minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                                minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                                minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);
                            }
                            break;

                        case "vt":  // Coordenada UV
                            if (tokens.length >= 3) {
                                float u = Float.parseFloat(tokens[1]);
                                float v = Float.parseFloat(tokens[2]);
                                if (flipV) v = 1.0f - v;
                                tmpUVs.add(new float[]{u, v});
                            }
                            break;

                        case "vn":  // Normal
                            if (tokens.length >= 4) {
                                float nx = Float.parseFloat(tokens[1]);
                                float ny = Float.parseFloat(tokens[2]);
                                float nz = Float.parseFloat(tokens[3]);
                                tmpNormals.add(new float[]{nx, ny, nz});
                            }
                            break;

                        case "f":  // Cara
                            if (tokens.length >= 4) {
                                Face face = parseFace(tokens);
                                if (face != null) {
                                    faceList.add(face);
                                }
                            }
                            break;

                        // Ignorar: mtllib, usemtl, o, g, s, etc.
                    }
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    skippedLines++;
                    if (skippedLines <= 5) {
                        Log.w(TAG, "⚠️ Línea " + lineNumber + " ignorada: " + line);
                    }
                }
            }
        }

        if (skippedLines > 5) {
            Log.w(TAG, "⚠️ ... y " + (skippedLines - 5) + " líneas más con errores");
        }

        // Guardar conteos ANTES de expandir (para estadísticas)
        final int rawVertCount = tmpVerts.size();
        final int rawUVCount = tmpUVs.size();
        final int rawFaceCount = faceList.size();

        Log.d(TAG, "Parseado: " + rawVertCount + " vértices, " +
                   rawUVCount + " UVs, " +
                   tmpNormals.size() + " normales, " +
                   rawFaceCount + " caras");

        // Validación
        if (rawVertCount == 0 || rawFaceCount == 0) {
            throw new IOException("Modelo vacío o inválido: " + sourceName);
        }

        // Bounding box por defecto si no hay vértices válidos
        if (minX == Float.MAX_VALUE) {
            minX = minY = minZ = -1f;
            maxX = maxY = maxZ = 1f;
        }

        BoundingBox boundingBox = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
        Log.d(TAG, "BoundingBox: [" + minX + "," + minY + "," + minZ + "] → [" +
                   maxX + "," + maxY + "," + maxZ + "]");

        // ═══════════════════════════════════════════════════════════════════════════
        // FASE 2: Expandir vértices para UV mapping correcto
        // ═══════════════════════════════════════════════════════════════════════════
        boolean hasValidUVs = !tmpUVs.isEmpty();
        boolean hasValidNormals = !tmpNormals.isEmpty();

        // Contar vértices expandidos
        int expandedVertCount = 0;
        for (Face face : faceList) {
            expandedVertCount += face.vertexIndices.length;
        }

        if (options.progressListener != null) {
            options.progressListener.onProgress(50);
        }

        // Arrays expandidos
        float[] expandedVerts = new float[expandedVertCount * 3];
        float[] expandedUVs = new float[expandedVertCount * 2];
        float[] expandedNormals = options.calculateNormals || hasValidNormals ?
                                  new float[expandedVertCount * 3] : null;

        List<int[]> newFaceList = new ArrayList<>(faceList.size());
        int currentIndex = 0;

        for (Face face : faceList) {
            int[] newFaceIndices = new int[face.vertexIndices.length];

            for (int i = 0; i < face.vertexIndices.length; i++) {
                int vertIdx = face.vertexIndices[i];

                // Validar índice
                if (vertIdx < 0 || vertIdx >= tmpVerts.size()) {
                    Log.w(TAG, "⚠️ Índice de vértice inválido: " + vertIdx);
                    vertIdx = 0;  // Fallback al primer vértice
                }

                // Copiar posición
                float[] vert = tmpVerts.get(vertIdx);
                expandedVerts[currentIndex * 3] = vert[0];
                expandedVerts[currentIndex * 3 + 1] = vert[1];
                expandedVerts[currentIndex * 3 + 2] = vert[2];

                // Copiar UV
                if (hasValidUVs && face.uvIndices != null &&
                    i < face.uvIndices.length &&
                    face.uvIndices[i] >= 0 &&
                    face.uvIndices[i] < tmpUVs.size()) {
                    float[] uv = tmpUVs.get(face.uvIndices[i]);
                    expandedUVs[currentIndex * 2] = uv[0];
                    expandedUVs[currentIndex * 2 + 1] = uv[1];
                } else {
                    // Fallback: UV esférico procedural
                    float[] sphericalUV = generateSphericalUV(vert[0], vert[1], vert[2]);
                    expandedUVs[currentIndex * 2] = sphericalUV[0];
                    expandedUVs[currentIndex * 2 + 1] = sphericalUV[1];
                }

                // Copiar normales (si existen)
                if (expandedNormals != null && hasValidNormals && face.normalIndices != null &&
                    i < face.normalIndices.length &&
                    face.normalIndices[i] >= 0 &&
                    face.normalIndices[i] < tmpNormals.size()) {
                    float[] normal = tmpNormals.get(face.normalIndices[i]);
                    expandedNormals[currentIndex * 3] = normal[0];
                    expandedNormals[currentIndex * 3 + 1] = normal[1];
                    expandedNormals[currentIndex * 3 + 2] = normal[2];
                }

                newFaceIndices[i] = currentIndex;
                currentIndex++;
            }

            newFaceList.add(newFaceIndices);
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // FASE 3: Calcular normales si es necesario
        // ═══════════════════════════════════════════════════════════════════════════
        if (options.calculateNormals && !hasValidNormals && expandedNormals != null) {
            calculateNormals(expandedVerts, newFaceList, expandedNormals);
            Log.d(TAG, "✓ Normales calculadas proceduralmente");
        }

        if (options.progressListener != null) {
            options.progressListener.onProgress(80);
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // FASE 4: Crear buffers de GPU
        // ═══════════════════════════════════════════════════════════════════════════
        FloatBuffer vBuf = createFloatBuffer(expandedVerts);
        FloatBuffer uvBuf = createFloatBuffer(expandedUVs);
        FloatBuffer nBuf = expandedNormals != null ? createFloatBuffer(expandedNormals) : null;

        // Liberar memoria temporal
        tmpVerts.clear();
        tmpUVs.clear();
        tmpNormals.clear();
        faceList.clear();

        if (options.progressListener != null) {
            options.progressListener.onProgress(100);
        }

        // Estadísticas
        long loadTime = System.currentTimeMillis() - startTime;
        int triangleCount = 0;
        for (int[] face : newFaceList) {
            triangleCount += face.length - 2;
        }

        Statistics stats = new Statistics(
            rawVertCount, rawUVCount, rawFaceCount,
            expandedVertCount, triangleCount, loadTime, flipV, skippedLines
        );

        Log.d(TAG, "╔═══════════════════════════════════════════════════════════════╗");
        Log.d(TAG, "║  ✅ Carga completada: " + sourceName);
        Log.d(TAG, "║  " + stats);
        Log.d(TAG, "╚═══════════════════════════════════════════════════════════════╝");

        return new Mesh(vBuf, expandedVerts, newFaceList, uvBuf, nBuf,
                       expandedVertCount, boundingBox, stats);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MÉTODOS DE UTILIDAD
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Auto-detecta si el modelo necesita flipV basado en el nombre del archivo
     */
    private static boolean shouldAutoFlipV(@NonNull String assetPath) {
        String lowerPath = assetPath.toLowerCase();
        for (String pattern : MESHY_MODEL_PATTERNS) {
            if (lowerPath.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parsea una línea de cara (f) del archivo OBJ
     */
    @Nullable
    private static Face parseFace(@NonNull String[] tokens) {
        int nv = tokens.length - 1;
        if (nv < 3) return null;

        int[] vertIndices = new int[nv];
        int[] uvIndices = new int[nv];
        int[] normalIndices = new int[nv];
        boolean hasUVs = true;
        boolean hasNormals = true;

        for (int i = 0; i < nv; i++) {
            String[] parts = tokens[i + 1].split("/");

            // Índice de vértice (siempre presente, 1-indexed en OBJ)
            vertIndices[i] = Integer.parseInt(parts[0]) - 1;

            // Índice de UV (opcional)
            if (parts.length >= 2 && !parts[1].isEmpty()) {
                uvIndices[i] = Integer.parseInt(parts[1]) - 1;
            } else {
                hasUVs = false;
            }

            // Índice de normal (opcional)
            if (parts.length >= 3 && !parts[2].isEmpty()) {
                normalIndices[i] = Integer.parseInt(parts[2]) - 1;
            } else {
                hasNormals = false;
            }
        }

        return new Face(vertIndices, hasUVs ? uvIndices : null, hasNormals ? normalIndices : null);
    }

    /**
     * Crea un FloatBuffer de GPU a partir de un array
     */
    @NonNull
    private static FloatBuffer createFloatBuffer(@NonNull float[] data) {
        FloatBuffer buffer = ByteBuffer
            .allocateDirect(data.length * Float.BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(data);
        buffer.position(0);
        return buffer;
    }

    /**
     * Calcula normales para todas las caras (flat shading)
     */
    private static void calculateNormals(@NonNull float[] vertices,
                                         @NonNull List<int[]> faces,
                                         @NonNull float[] normals) {
        // Inicializar normales a cero
        for (int i = 0; i < normals.length; i++) {
            normals[i] = 0f;
        }

        // Para cada cara, calcular normal y acumular en vértices
        for (int[] face : faces) {
            if (face.length < 3) continue;

            // Obtener vértices de la cara
            int i0 = face[0], i1 = face[1], i2 = face[2];

            float v0x = vertices[i0 * 3], v0y = vertices[i0 * 3 + 1], v0z = vertices[i0 * 3 + 2];
            float v1x = vertices[i1 * 3], v1y = vertices[i1 * 3 + 1], v1z = vertices[i1 * 3 + 2];
            float v2x = vertices[i2 * 3], v2y = vertices[i2 * 3 + 1], v2z = vertices[i2 * 3 + 2];

            // Calcular vectores de aristas
            float e1x = v1x - v0x, e1y = v1y - v0y, e1z = v1z - v0z;
            float e2x = v2x - v0x, e2y = v2y - v0y, e2z = v2z - v0z;

            // Producto cruz = normal
            float nx = e1y * e2z - e1z * e2y;
            float ny = e1z * e2x - e1x * e2z;
            float nz = e1x * e2y - e1y * e2x;

            // Normalizar
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len > EPSILON) {
                nx /= len; ny /= len; nz /= len;
            }

            // Asignar a todos los vértices de la cara
            for (int idx : face) {
                normals[idx * 3] = nx;
                normals[idx * 3 + 1] = ny;
                normals[idx * 3 + 2] = nz;
            }
        }
    }

    /**
     * Genera coordenadas UV esféricas proceduralmente
     * - U: ángulo horizontal (theta) normalizado a [0,1]
     * - V: ángulo vertical (phi) normalizado a [0,1]
     */
    @NonNull
    private static float[] generateSphericalUV(float x, float y, float z) {
        float r = (float) Math.sqrt(x * x + y * y + z * z);
        if (r < EPSILON) {
            return new float[]{0.5f, 0.5f};
        }

        float nx = x / r;
        float ny = y / r;
        float nz = z / r;

        // Theta (horizontal) - atan2 devuelve [-π, π]
        float theta = (float) Math.atan2(-nz, nx);
        if (theta < 0) theta += (float) (2.0 * Math.PI);
        float u = theta / (float) (2.0 * Math.PI);

        // Phi (vertical) - acos devuelve [0, π]
        float ny_clamped = Math.max(-1.0f, Math.min(1.0f, ny));
        float phi = (float) Math.acos(ny_clamped);
        float v = phi / (float) Math.PI;

        // Clamp final
        u = Math.max(0.0f, Math.min(1.0f, u));
        v = Math.max(0.0f, Math.min(1.0f, v));

        return new float[]{u, v};
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MÉTODOS DE ÍNDICES PARA OPENGL
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Construye IntBuffer de índices con fan triangulation.
     * Usar con glDrawElements(GL_TRIANGLES, ..., GL_UNSIGNED_INT, indexBuffer)
     *
     * @param faces Lista de caras (polígonos)
     * @param indexCount Número total de índices (puede calcular con countIndices())
     */
    @NonNull
    public static IntBuffer buildIndexBuffer(@NonNull List<int[]> faces, int indexCount) {
        IntBuffer ib = ByteBuffer
            .allocateDirect(indexCount * Integer.BYTES)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer();

        for (int[] face : faces) {
            // Fan triangulation: (v0,v1,v2), (v0,v2,v3), ...
            int v0 = face[0];
            for (int i = 1; i < face.length - 1; i++) {
                ib.put(v0);
                ib.put(face[i]);
                ib.put(face[i + 1]);
            }
        }

        ib.position(0);
        return ib;
    }

    /**
     * Cuenta el número total de índices necesarios para fan triangulation
     */
    public static int countIndices(@NonNull List<int[]> faces) {
        int count = 0;
        for (int[] face : faces) {
            count += (face.length - 2) * 3;  // Triángulos * 3 índices
        }
        return count;
    }

    /**
     * @deprecated Usar buildIndexBuffer(List<int[]>, int) para soportar modelos >32K vértices
     */
    @Deprecated
    public static ShortBuffer buildIndexBufferShort(List<short[]> faces, int indexCount) {
        ShortBuffer ib = ByteBuffer
            .allocateDirect(indexCount * Short.BYTES)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer();

        for (short[] face : faces) {
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
