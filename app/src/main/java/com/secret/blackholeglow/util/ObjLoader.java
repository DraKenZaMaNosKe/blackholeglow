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
 * Carga un OBJ simple (v y f con capacidad para polígonos) desde assets/.
 */
public class ObjLoader {
    /**
     * Mesh que contiene vértices y lista de caras arbitrarias.
     */
    public static class Mesh {
        public final FloatBuffer vertexBuffer;
        public final float[] originalVertices;
        public final List<short[]> faces;

        public Mesh(FloatBuffer vb, float[] verts, List<short[]> faceList) {
            this.vertexBuffer     = vb;
            this.originalVertices = verts;
            this.faces            = faceList;
        }
    }

    /**
     * Lee un OBJ desde assets/ y admite caras con N vértices (polígonos).
     * @param ctx Contexto
     * @param assetPath Nombre de archivo OBJ en assets/
     */
    public static Mesh loadObj(Context ctx, String assetPath) throws IOException {
        List<float[]> tmpVerts = new ArrayList<>();
        List<short[]> faceList = new ArrayList<>();

        InputStream is = ctx.getAssets().open(assetPath);
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = r.readLine()) != null) {
            String[] p = line.trim().split("\\s+");
            if (p.length < 1) continue;
            if (p[0].equals("v")) {
                // v x y z
                tmpVerts.add(new float[]{
                        Float.parseFloat(p[1]),
                        Float.parseFloat(p[2]),
                        Float.parseFloat(p[3])
                });
            } else if (p[0].equals("f")) {
                // f i1 i2 i3 ... iN
                int nv = p.length - 1;
                short[] idx = new short[nv];
                for (int i = 0; i < nv; i++) {
                    // soporta formatos i/j/k: nos quedamos con antes de '/'
                    String token = p[i+1];
                    String[] parts = token.split("/");
                    idx[i] = (short)(Short.parseShort(parts[0]) - 1);
                }
                faceList.add(idx);
            }
        }
        r.close();

        // Aplanar vértices
        float[] vertsArr = new float[tmpVerts.size() * 3];
        for (int i = 0; i < tmpVerts.size(); i++) {
            float[] v = tmpVerts.get(i);
            vertsArr[i*3]   = v[0];
            vertsArr[i*3+1] = v[1];
            vertsArr[i*3+2] = v[2];
        }

        // Crear FloatBuffer
        FloatBuffer vBuf = ByteBuffer
                .allocateDirect(vertsArr.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertsArr);
        vBuf.position(0);

        return new Mesh(vBuf, vertsArr, faceList);
    }
}