package com.secret.blackholeglow.gl3;

import android.content.Context;
import android.opengl.GLES30;
import android.util.Log;

import com.secret.blackholeglow.util.ObjLoader;

import java.io.IOException;
import java.nio.FloatBuffer;

/**
 * ============================================================================
 *  ObjMeshLoader - Carga modelos OBJ directamente a GLMesh (VAO/VBO)
 * ============================================================================
 *
 *  Convierte archivos .obj en GLMesh optimizados para GPU.
 *  Los datos se suben a la GPU una sola vez, no cada frame.
 *
 *  Uso:
 *    GLMesh mesh = ObjMeshLoader.load(context, "defender_ship.obj");
 *    mesh.bind();
 *    mesh.draw();
 *    mesh.unbind();
 *
 * ============================================================================
 */
public class ObjMeshLoader {
    private static final String TAG = "ObjMeshLoader";

    /**
     * Carga un archivo OBJ y lo convierte en GLMesh optimizado
     *
     * @param context Context para acceder a assets
     * @param objFileName Nombre del archivo OBJ en assets (ej: "defender_ship.obj")
     * @return GLMesh listo para renderizar, o null si hay error
     */
    public static GLMesh load(Context context, String objFileName) {
        try {
            Log.d(TAG, "Cargando OBJ: " + objFileName);

            // Cargar mesh con ObjLoader existente
            ObjLoader.Mesh objMesh = ObjLoader.loadObj(context, objFileName);

            if (objMesh == null || objMesh.vertexBuffer == null) {
                Log.e(TAG, "Error: OBJ no tiene vertices");
                return null;
            }

            // Convertir FloatBuffer a float[]
            float[] vertices = bufferToArray(objMesh.vertexBuffer);
            float[] uvs = objMesh.uvBuffer != null ? bufferToArray(objMesh.uvBuffer) : null;

            Log.d(TAG, "  Vertices: " + (vertices.length / 3));
            Log.d(TAG, "  UVs: " + (uvs != null ? uvs.length / 2 : 0));

            // Construir indices desde las caras
            int totalIndices = 0;
            for (int[] face : objMesh.faces) {
                totalIndices += (face.length - 2) * 3; // Triangular fan
            }

            short[] indices = new short[totalIndices];
            int idx = 0;
            for (int[] face : objMesh.faces) {
                int v0 = face[0];
                for (int i = 1; i < face.length - 1; i++) {
                    indices[idx++] = (short) v0;
                    indices[idx++] = (short) face[i];
                    indices[idx++] = (short) face[i + 1];
                }
            }

            Log.d(TAG, "  Indices: " + indices.length);

            // Crear GLMesh con Builder
            GLMesh.Builder builder = new GLMesh.Builder()
                    .addVertexBuffer(vertices, 3)  // posiciones (vec3)
                    .setIndexBuffer(indices);

            if (uvs != null && uvs.length > 0) {
                builder.addVertexBuffer(uvs, 2);  // UVs (vec2)
            }

            GLMesh mesh = builder.build();

            Log.d(TAG, "✓ GLMesh creado exitosamente para: " + objFileName);

            return mesh;

        } catch (IOException e) {
            Log.e(TAG, "Error cargando OBJ: " + objFileName, e);
            return null;
        }
    }

    /**
     * Carga OBJ con centrado automatico del modelo
     */
    public static GLMesh loadCentered(Context context, String objFileName) {
        try {
            Log.d(TAG, "Cargando OBJ centrado: " + objFileName);

            ObjLoader.Mesh objMesh = ObjLoader.loadObj(context, objFileName);

            if (objMesh == null || objMesh.vertexBuffer == null) {
                Log.e(TAG, "Error: OBJ no tiene vertices");
                return null;
            }

            float[] vertices = bufferToArray(objMesh.vertexBuffer);
            float[] uvs = objMesh.uvBuffer != null ? bufferToArray(objMesh.uvBuffer) : null;

            // Calcular centro y centrar vertices
            float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
            float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;
            float minZ = Float.MAX_VALUE, maxZ = Float.MIN_VALUE;

            for (int i = 0; i < vertices.length; i += 3) {
                minX = Math.min(minX, vertices[i]);
                maxX = Math.max(maxX, vertices[i]);
                minY = Math.min(minY, vertices[i + 1]);
                maxY = Math.max(maxY, vertices[i + 1]);
                minZ = Math.min(minZ, vertices[i + 2]);
                maxZ = Math.max(maxZ, vertices[i + 2]);
            }

            float centerX = (minX + maxX) / 2f;
            float centerY = (minY + maxY) / 2f;
            float centerZ = (minZ + maxZ) / 2f;

            // Centrar vertices
            for (int i = 0; i < vertices.length; i += 3) {
                vertices[i] -= centerX;
                vertices[i + 1] -= centerY;
                vertices[i + 2] -= centerZ;
            }

            Log.d(TAG, "  Centrado: offset(" + centerX + ", " + centerY + ", " + centerZ + ")");

            // Construir indices
            int totalIndices = 0;
            for (int[] face : objMesh.faces) {
                totalIndices += (face.length - 2) * 3;
            }

            short[] indices = new short[totalIndices];
            int idx = 0;
            for (int[] face : objMesh.faces) {
                int v0 = face[0];
                for (int i = 1; i < face.length - 1; i++) {
                    indices[idx++] = (short) v0;
                    indices[idx++] = (short) face[i];
                    indices[idx++] = (short) face[i + 1];
                }
            }

            // Crear GLMesh
            GLMesh.Builder builder = new GLMesh.Builder()
                    .addVertexBuffer(vertices, 3)
                    .setIndexBuffer(indices);

            if (uvs != null && uvs.length > 0) {
                builder.addVertexBuffer(uvs, 2);
            }

            GLMesh mesh = builder.build();

            Log.d(TAG, "✓ GLMesh centrado creado para: " + objFileName);

            return mesh;

        } catch (IOException e) {
            Log.e(TAG, "Error cargando OBJ: " + objFileName, e);
            return null;
        }
    }

    /**
     * Convierte FloatBuffer a float[]
     */
    private static float[] bufferToArray(FloatBuffer buffer) {
        buffer.position(0);
        float[] array = new float[buffer.remaining()];
        buffer.get(array);
        buffer.position(0);
        return array;
    }

    /**
     * Resultado de carga con metadatos adicionales
     */
    public static class MeshData {
        public GLMesh mesh;
        public int vertexCount;
        public int indexCount;
        public float[] bounds; // minX, maxX, minY, maxY, minZ, maxZ

        public float getWidth() {
            return bounds[1] - bounds[0];
        }

        public float getHeight() {
            return bounds[3] - bounds[2];
        }

        public float getDepth() {
            return bounds[5] - bounds[4];
        }
    }

    /**
     * Carga OBJ con metadatos adicionales
     */
    public static MeshData loadWithData(Context context, String objFileName) {
        try {
            ObjLoader.Mesh objMesh = ObjLoader.loadObj(context, objFileName);

            if (objMesh == null || objMesh.vertexBuffer == null) {
                return null;
            }

            float[] vertices = bufferToArray(objMesh.vertexBuffer);
            float[] uvs = objMesh.uvBuffer != null ? bufferToArray(objMesh.uvBuffer) : null;

            // Calcular bounds
            float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
            float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

            for (int i = 0; i < vertices.length; i += 3) {
                minX = Math.min(minX, vertices[i]);
                maxX = Math.max(maxX, vertices[i]);
                minY = Math.min(minY, vertices[i + 1]);
                maxY = Math.max(maxY, vertices[i + 1]);
                minZ = Math.min(minZ, vertices[i + 2]);
                maxZ = Math.max(maxZ, vertices[i + 2]);
            }

            // Construir indices
            int totalIndices = 0;
            for (int[] face : objMesh.faces) {
                totalIndices += (face.length - 2) * 3;
            }

            short[] indices = new short[totalIndices];
            int idx = 0;
            for (int[] face : objMesh.faces) {
                int v0 = face[0];
                for (int i = 1; i < face.length - 1; i++) {
                    indices[idx++] = (short) v0;
                    indices[idx++] = (short) face[i];
                    indices[idx++] = (short) face[i + 1];
                }
            }

            // Crear GLMesh
            GLMesh.Builder builder = new GLMesh.Builder()
                    .addVertexBuffer(vertices, 3)
                    .setIndexBuffer(indices);

            if (uvs != null && uvs.length > 0) {
                builder.addVertexBuffer(uvs, 2);
            }

            MeshData data = new MeshData();
            data.mesh = builder.build();
            data.vertexCount = vertices.length / 3;
            data.indexCount = indices.length;
            data.bounds = new float[]{minX, maxX, minY, maxY, minZ, maxZ};

            Log.d(TAG, "✓ MeshData creado: " + objFileName);
            Log.d(TAG, "  Size: " + data.getWidth() + " x " + data.getHeight() + " x " + data.getDepth());

            return data;

        } catch (IOException e) {
            Log.e(TAG, "Error cargando OBJ: " + objFileName, e);
            return null;
        }
    }
}
