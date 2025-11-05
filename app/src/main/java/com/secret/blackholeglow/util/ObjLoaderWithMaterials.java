// app/src/main/java/com/secret/blackholeglow/util/ObjLoaderWithMaterials.java
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ObjLoaderWithMaterials - Enhanced OBJ loader with material group support
 *
 * Parses .obj files with mtllib and usemtl directives to group faces by material.
 * This allows rendering objects with multiple textures/materials from Blender.
 */
public class ObjLoaderWithMaterials {
    private static final String TAG = "Depurando";

    /**
     * Enhanced Mesh with material groups
     */
    public static class MeshWithMaterials {
        public final FloatBuffer vertexBuffer;      // Buffer GPU de vértices XYZ
        public final FloatBuffer uvBuffer;          // Buffer GPU de coords UV
        public final int vertexCount;               // Número de vértices
        public final float[] originalVertices;      // Array plano XYZ original
        public final List<MaterialGroup> materialGroups;  // Grupos de materiales
        public final String mtlFile;                // Nombre del archivo .mtl

        public MeshWithMaterials(FloatBuffer vb, float[] verts, FloatBuffer uvb,
                                 int vCount, List<MaterialGroup> matGroups, String mtl) {
            this.vertexBuffer = vb;
            this.originalVertices = verts;
            this.uvBuffer = uvb;
            this.vertexCount = vCount;
            this.materialGroups = matGroups;
            this.mtlFile = mtl;
        }
    }

    /**
     * Face structure with material name
     */
    private static class FaceWithMaterial {
        public int[] vertexIndices;    // ✅ int[] para modelos grandes >32k vértices
        public int[] uvIndices;        // ✅ int[] para modelos grandes
        public String materialName;  // Material asignado a esta cara

        public FaceWithMaterial(int[] verts, int[] uvs, String mat) {
            this.vertexIndices = verts;
            this.uvIndices = uvs;
            this.materialName = mat;
        }
    }

    /**
     * ═══════════════════════════════════════════════════════════════
     * LOAD OBJ WITH MATERIALS
     * ═══════════════════════════════════════════════════════════════
     * Carga un archivo .obj detectando mtllib y usemtl para agrupar
     * caras por material.
     */
    public static MeshWithMaterials loadObjWithMaterials(Context ctx, String objPath, String mtlPath)
            throws IOException {

        Log.d(TAG, "════════════════════════════════════════════════");
        Log.d(TAG, "ObjLoaderWithMaterials: Cargando `" + objPath + "`");
        Log.d(TAG, "════════════════════════════════════════════════");

        List<float[]> tmpVerts = new ArrayList<>();
        List<float[]> tmpUVs = new ArrayList<>();
        List<FaceWithMaterial> faceList = new ArrayList<>();
        String mtlFile = null;
        String currentMaterial = null;  // Material activo mientras leemos caras

        // ═══════════════════════════════════════════════════════════
        // PASO 1: Parsear archivo .obj
        // ═══════════════════════════════════════════════════════════
        InputStream is = ctx.getAssets().open(objPath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;

        while ((line = reader.readLine()) != null) {
            String[] tokens = line.trim().split("\\s+");
            if (tokens.length < 1) continue;

            switch (tokens[0]) {
                case "mtllib":
                    // Detectar archivo .mtl
                    if (tokens.length >= 2) {
                        mtlFile = tokens[1];
                        Log.d(TAG, "  📁 Archivo MTL detectado: " + mtlFile);
                    }
                    break;

                case "usemtl":
                    // Cambiar material activo
                    if (tokens.length >= 2) {
                        currentMaterial = tokens[1];
                        Log.d(TAG, "  🎨 Cambiando a material: " + currentMaterial);
                    }
                    break;

                case "v":
                    // Vértice XYZ
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
                    // Face con material asignado
                    int nv = tokens.length - 1;
                    int[] vertIndices = new int[nv];  // ✅ int[] para modelos grandes
                    int[] uvIndices = new int[nv];    // ✅ int[] para modelos grandes
                    boolean hasUVs = true;

                    for (int i = 0; i < nv; i++) {
                        String[] parts = tokens[i + 1].split("/");

                        // Índice de vértice
                        int vertIndex = Integer.parseInt(parts[0]) - 1;
                        vertIndices[i] = vertIndex;  // ✅ Sin cast a short

                        // Índice de UV (opcional)
                        if (parts.length >= 2 && !parts[1].isEmpty()) {
                            int uvIndex = Integer.parseInt(parts[1]) - 1;
                            uvIndices[i] = uvIndex;  // ✅ Sin cast a short
                        } else {
                            hasUVs = false;
                        }
                    }

                    faceList.add(new FaceWithMaterial(
                            vertIndices,
                            hasUVs ? uvIndices : null,
                            currentMaterial  // ✅ Asignar material a esta cara
                    ));
                    break;

                default:
                    // Ignorar otros comandos
                    break;
            }
        }
        reader.close();

        int vCount = tmpVerts.size();
        Log.d(TAG, "ObjLoader: vértices leídos = " + vCount);
        Log.d(TAG, "ObjLoader: UVs leídos = " + tmpUVs.size());
        Log.d(TAG, "ObjLoader: Caras leídas = " + faceList.size());

        // ═══════════════════════════════════════════════════════════
        // PASO 2: Agrupar caras por material
        // ═══════════════════════════════════════════════════════════
        Map<String, MaterialGroup> groupMap = new HashMap<>();

        for (FaceWithMaterial face : faceList) {
            String matName = face.materialName != null ? face.materialName : "default";

            if (!groupMap.containsKey(matName)) {
                groupMap.put(matName, new MaterialGroup(matName));
            }

            // Convertir a Face de ObjLoader
            ObjLoader.Face objFace = new ObjLoader.Face(face.vertexIndices, face.uvIndices);
            groupMap.get(matName).faces.add(objFace);
        }

        List<MaterialGroup> materialGroups = new ArrayList<>(groupMap.values());

        Log.d(TAG, "  ✅ Grupos de materiales creados: " + materialGroups.size());
        for (MaterialGroup group : materialGroups) {
            Log.d(TAG, "    - " + group.materialName + ": " + group.faces.size() + " caras");
        }

        // ═══════════════════════════════════════════════════════════
        // PASO 3: Cargar materiales desde .mtl si existe
        // ═══════════════════════════════════════════════════════════
        if (mtlPath != null) {
            try {
                Map<String, MtlLoader.Material> materials = MtlLoader.loadMtl(ctx, mtlPath);

                // Asignar materiales a grupos
                for (MaterialGroup group : materialGroups) {
                    if (materials.containsKey(group.materialName)) {
                        group.material = materials.get(group.materialName);
                        Log.d(TAG, "    ✅ Material '" + group.materialName + "' asignado");
                    } else {
                        Log.w(TAG, "    ⚠️  Material '" + group.materialName + "' no encontrado en .mtl");
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "  ⚠️  Error cargando .mtl: " + e.getMessage());
            }
        }

        // ═══════════════════════════════════════════════════════════
        // PASO 4: Construir buffers
        // ═══════════════════════════════════════════════════════════

        // Aplanar vértices
        float[] vertsArr = new float[vCount * 3];
        for (int i = 0; i < vCount; i++) {
            float[] v = tmpVerts.get(i);
            vertsArr[i * 3] = v[0];
            vertsArr[i * 3 + 1] = v[1];
            vertsArr[i * 3 + 2] = v[2];
        }

        // Construir UVs
        float[] uvArr = new float[vCount * 2];
        if (!tmpUVs.isEmpty()) {
            // Usar UVs del archivo
            for (int i = 0; i < vCount; i++) {
                uvArr[i * 2] = -1f;
                uvArr[i * 2 + 1] = -1f;
            }

            // Asignar UVs usando índices de caras
            for (FaceWithMaterial face : faceList) {
                if (face.uvIndices != null) {
                    for (int i = 0; i < face.vertexIndices.length; i++) {
                        int vertIdx = face.vertexIndices[i];
                        int uvIdx = face.uvIndices[i];

                        if (uvIdx >= 0 && uvIdx < tmpUVs.size()) {
                            float[] uv = tmpUVs.get(uvIdx);
                            uvArr[vertIdx * 2] = uv[0];
                            uvArr[vertIdx * 2 + 1] = uv[1];
                        }
                    }
                }
            }
        }

        // Crear buffers GPU
        FloatBuffer vBuf = ByteBuffer
                .allocateDirect(vertsArr.length * Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertsArr);
        vBuf.position(0);

        FloatBuffer uvBuf = ByteBuffer
                .allocateDirect(uvArr.length * Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(uvArr);
        uvBuf.position(0);

        Log.d(TAG, "════════════════════════════════════════════════");
        Log.d(TAG, "ObjLoaderWithMaterials: ✓ Carga completada");
        Log.d(TAG, "════════════════════════════════════════════════");

        return new MeshWithMaterials(vBuf, vertsArr, uvBuf, vCount, materialGroups, mtlFile);
    }

    /**
     * Construye un ShortBuffer de índices para un MaterialGroup específico
     */
    public static ShortBuffer buildIndexBufferForGroup(MaterialGroup group) {
        int triangleCount = group.getTriangleCount();
        int indexCount = triangleCount * 3;

        ShortBuffer ib = ByteBuffer
                .allocateDirect(indexCount * Short.BYTES)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();

        for (ObjLoader.Face face : group.faces) {
            // Fan triangulation
            short v0 = (short) face.vertexIndices[0];  // ✅ Cast a short
            for (int i = 1; i < face.vertexIndices.length - 1; i++) {
                ib.put(v0);
                ib.put((short) face.vertexIndices[i]);      // ✅ Cast a short
                ib.put((short) face.vertexIndices[i + 1]);  // ✅ Cast a short
            }
        }

        ib.position(0);
        return ib;
    }
}
