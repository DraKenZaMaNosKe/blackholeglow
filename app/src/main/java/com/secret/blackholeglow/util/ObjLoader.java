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

import java.util.ArrayList;
import java.util.List;

public class ObjLoader {
    private static final String TAG = "Depurando";

    public static class Mesh {
        public final FloatBuffer vertexBuffer;      // Buffer GPU de vértices XYZ
        public final FloatBuffer uvBuffer;          // Buffer GPU de coords UV
        public final int          vertexCount;      // Número de vértices (X,Y,Z)
        public final float[]      originalVertices; // Array plano XYZ original
        public final List<short[]> faces;           // Lista de caras (polígonos)

        public Mesh(FloatBuffer vb, float[] verts, List<short[]> faceList,
                    FloatBuffer uvb, int vCount) {
            this.vertexBuffer     = vb;
            this.originalVertices = verts;
            this.faces            = faceList;
            this.uvBuffer         = uvb;
            this.vertexCount      = vCount;
        }
    }

    public static Mesh loadObj(Context ctx, String assetPath) throws IOException {
        Log.d(TAG, "ObjLoader: iniciando carga de `" + assetPath + "`");
        List<float[]> tmpVerts = new ArrayList<>();
        List<float[]> tmpUVs   = new ArrayList<>();
        List<short[]> faceList = new ArrayList<>();

        InputStream is = ctx.getAssets().open(assetPath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
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
                    tmpVerts.add(new float[]{ x, y, z });
                    break;
                case "vt":
                    // Coordenada UV
                    float u = Float.parseFloat(tokens[1]);
                    float v = Float.parseFloat(tokens[2]);
                    tmpUVs.add(new float[]{ u, v });
                    break;
                case "f":
                    // Caras: puede ser "v", "v/vt" o "v/vt/vn"
                    int nv = tokens.length - 1;
                    short[] idx = new short[nv];
                    for (int i = 0; i < nv; i++) {
                        String[] parts = tokens[i+1].split("/");
                        int vertIndex = Integer.parseInt(parts[0]) - 1;
                        idx[i] = (short) vertIndex;
                    }
                    faceList.add(idx);
                    break;
                default:
                    // ignorar normales, comentarios, etc.
                    break;
            }
        }
        reader.close();

        int vCount = tmpVerts.size();
        Log.d(TAG, "ObjLoader: vértices leídos = " + vCount +
                ", UVs leídos = " + tmpUVs.size() +
                ", caras leídas = " + faceList.size());

        // Aplanar vértices
        float[] vertsArr = new float[vCount * 3];
        for (int i = 0; i < vCount; i++) {
            float[] v = tmpVerts.get(i);
            vertsArr[i*3]   = v[0];
            vertsArr[i*3+1] = v[1];
            vertsArr[i*3+2] = v[2];
        }

        // Preparar array de UVs (por defecto ceros)
        float[] uvArr = new float[vCount * 2];
        for (int i = 0; i < vCount; i++) {
            uvArr[i*2]   = 0f;
            uvArr[i*2+1] = 0f;
        }
        // Si hay UVs, asignarlos según el índice de la cara
        if (!tmpUVs.isEmpty()) {
            for (short[] face : faceList) {
                for (short vertIdx : face) {
                    // suponemos correspondencia 1:1 v/vt en el OBJ para cada vertice
                    // aquí simplemente tomamos el mismo índice para la UV si existe
                    if (vertIdx < tmpUVs.size()) {
                        float[] uv = tmpUVs.get(vertIdx);
                        uvArr[vertIdx*2]   = uv[0];
                        uvArr[vertIdx*2+1] = uv[1];
                    }
                }
            }
        }

        // Crear vertexBuffer
        FloatBuffer vBuf = ByteBuffer
                .allocateDirect(vertsArr.length * Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertsArr);
        vBuf.position(0);

        // Crear uvBuffer
        FloatBuffer uvBuf = ByteBuffer
                .allocateDirect(uvArr.length * Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(uvArr);
        uvBuf.position(0);

        Log.d(TAG, "ObjLoader: buffers preparados (vBuf, uvBuf).");

        return new Mesh(vBuf, vertsArr, faceList, uvBuf, vCount);
    }
}
