package com.secret.blackholeglow.util;

import android.content.Context;
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
 * Carga OBJ simples (v/f) y OBJ+MTL con materiales.
 */
public class ObjLoader {

    // —— MESH SIMPLE —————————————————————
    public static class Mesh {
        public final FloatBuffer vertexBuffer;
        public final ShortBuffer indexBuffer;
        public final int vertexCount;
        public final int indexCount;
        public final float[] originalVertices;

        public Mesh(FloatBuffer vb, ShortBuffer ib, int vCount, int iCount, float[] orig) {
            this.vertexBuffer     = vb;
            this.indexBuffer      = ib;
            this.vertexCount      = vCount;
            this.indexCount       = iCount;
            this.originalVertices = orig;
        }
    }

    /**
     * Lee un OBJ (solo "v x y z" y "f i j k") desde assets/ y devuelve un Mesh.
     */
    public static Mesh loadObj(Context ctx, String assetPath) throws IOException {
        List<float[]> tmpVerts = new ArrayList<>();
        List<short[]> tmpFaces = new ArrayList<>();

        InputStream  is = ctx.getAssets().open(assetPath);
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = r.readLine()) != null) {
            String[] p = line.trim().split("\\s+");
            if (p.length<1) continue;
            if (p[0].equals("v")) {
                tmpVerts.add(new float[]{
                        Float.parseFloat(p[1]),
                        Float.parseFloat(p[2]),
                        Float.parseFloat(p[3])
                });
            } else if (p[0].equals("f")) {
                tmpFaces.add(new short[]{
                        (short)(Short.parseShort(p[1]) - 1),
                        (short)(Short.parseShort(p[2]) - 1),
                        (short)(Short.parseShort(p[3]) - 1)
                });
            }
        }
        r.close();

        // Aplanar
        float[] vertsArr = new float[tmpVerts.size()*3];
        for (int i=0; i<tmpVerts.size(); i++){
            float[] v = tmpVerts.get(i);
            vertsArr[i*3]=v[0]; vertsArr[i*3+1]=v[1]; vertsArr[i*3+2]=v[2];
        }
        short[] idxArr = new short[tmpFaces.size()*3];
        for (int i=0; i<tmpFaces.size(); i++){
            short[] f = tmpFaces.get(i);
            idxArr[i*3]=f[0]; idxArr[i*3+1]=f[1]; idxArr[i*3+2]=f[2];
        }

        // Vertex Buffer
        FloatBuffer vBuf = ByteBuffer
                .allocateDirect(vertsArr.length*4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertsArr);
        vBuf.position(0);

        // Index Buffer
        ShortBuffer iBuf = ByteBuffer
                .allocateDirect(idxArr.length*2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
                .put(idxArr);
        iBuf.position(0);

        return new Mesh(vBuf, iBuf, vertsArr.length/3, idxArr.length, vertsArr);
    }

    // —— OBJ+MTL CON MATERIALES —————————————————————

    /** Sub-malla con su propio índice de triángulos y color */
    public static class SubMesh {
        public final ShortBuffer indexBuffer;
        public final int indexCount;
        public final float[] color; // rgb

        public SubMesh(ShortBuffer ib, int ic, float[] col) {
            indexBuffer = ib;
            indexCount  = ic;
            color       = col;
        }
    }

    /** Mesh completo con lista de sub-mallas coloreadas */
    public static class MeshWithMaterials {
        public final FloatBuffer vertexBuffer;
        public final List<SubMesh> subMeshes;

        public MeshWithMaterials(FloatBuffer vb, List<SubMesh> subs) {
            vertexBuffer = vb;
            subMeshes    = subs;
        }
    }

    /**
     * Lee OBJ + MTL desde assets/ y retorna un MeshWithMaterials.
     * OBJ debe usar "mtllib" y "usemtl" correctamente.
     */
    public static MeshWithMaterials loadMeshWithMaterials(
            Context ctx, String objFile, String mtlFile) throws IOException {

        // 1) Leer MTL
        Map<String,float[]> matColors = new HashMap<>();
        InputStream mtlIs = ctx.getAssets().open(mtlFile);
        BufferedReader mtlR = new BufferedReader(new InputStreamReader(mtlIs));
        String line, currentMat = null;
        while((line = mtlR.readLine())!=null){
            String[] p = line.trim().split("\\s+");
            if(p.length<1) continue;
            if(p[0].equals("newmtl")){
                currentMat = p[1];
            } else if(p[0].equals("Kd") && currentMat!=null){
                matColors.put(currentMat, new float[]{
                        Float.parseFloat(p[1]),
                        Float.parseFloat(p[2]),
                        Float.parseFloat(p[3])
                });
            }
        }
        mtlR.close();

        // 2) Leer OBJ
        List<float[]> tmpVerts = new ArrayList<>();
        Map<String,List<Short>> idxPerMat = new HashMap<>();
        idxPerMat.put("default", new ArrayList<>());
        currentMat = "default";

        InputStream objIs = ctx.getAssets().open(objFile);
        BufferedReader objR = new BufferedReader(new InputStreamReader(objIs));
        while((line = objR.readLine())!=null){
            String[] p = line.trim().split("\\s+");
            if(p.length<1) continue;
            switch(p[0]){
                case "v":
                    tmpVerts.add(new float[]{
                            Float.parseFloat(p[1]),
                            Float.parseFloat(p[2]),
                            Float.parseFloat(p[3])
                    });
                    break;
                case "usemtl":
                    currentMat = p[1];
                    idxPerMat.putIfAbsent(currentMat, new ArrayList<>());
                    break;
                case "f":
                    short i0 = (short)(Short.parseShort(p[1]) - 1);
                    short i1 = (short)(Short.parseShort(p[2]) - 1);
                    short i2 = (short)(Short.parseShort(p[3]) - 1);
                    idxPerMat.get(currentMat).add(i0);
                    idxPerMat.get(currentMat).add(i1);
                    idxPerMat.get(currentMat).add(i2);
                    break;
            }
        }
        objR.close();

        // 3) Vertex Buffer
        float[] vertsArr = new float[tmpVerts.size()*3];
        for(int i=0;i<tmpVerts.size();i++){
            float[] v = tmpVerts.get(i);
            vertsArr[i*3]=v[0]; vertsArr[i*3+1]=v[1]; vertsArr[i*3+2]=v[2];
        }
        FloatBuffer vBuf = ByteBuffer
                .allocateDirect(vertsArr.length*4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertsArr);
        vBuf.position(0);

        // 4) Sub-mallas
        List<SubMesh> subs = new ArrayList<>();
        for(Map.Entry<String,List<Short>> e : idxPerMat.entrySet()){
            List<Short> lst = e.getValue();
            short[] ia = new short[lst.size()];
            for(int i=0;i<ia.length;i++) ia[i]=lst.get(i);
            ShortBuffer iBuf = ByteBuffer
                    .allocateDirect(ia.length*2)
                    .order(ByteOrder.nativeOrder())
                    .asShortBuffer()
                    .put(ia);
            iBuf.position(0);

            float[] col = matColors.getOrDefault(e.getKey(), new float[]{1f,1f,1f});
            subs.add(new SubMesh(iBuf, ia.length, col));
        }

        return new MeshWithMaterials(vBuf, subs);
    }
}
