package com.secret.blackholeglow.gl3;

import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║   🚀 GLMesh - Sistema VAO/VBO para OpenGL ES 3.0 🚀              ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * Encapsula Vertex Array Object (VAO) y Vertex Buffer Objects (VBOs)
 * para renderizado eficiente en OpenGL ES 3.0.
 *
 * Beneficios:
 * - VAO guarda el estado de los VBOs (menos llamadas GL por frame)
 * - VBOs almacenan datos en GPU (no CPU→GPU cada frame)
 * - Soporte para instanced rendering
 * - Manejo automático de limpieza de recursos
 *
 * Uso:
 *   GLMesh mesh = new GLMesh.Builder()
 *       .addVertexBuffer(positions, 3)  // vec3 posiciones
 *       .addVertexBuffer(uvs, 2)        // vec2 UVs
 *       .setIndexBuffer(indices)
 *       .build();
 *
 *   mesh.bind();
 *   mesh.draw();
 *   mesh.unbind();
 */
public class GLMesh {
    private static final String TAG = "GLMesh";

    // VAO ID (Vertex Array Object)
    private int vaoId = 0;

    // VBO IDs (Vertex Buffer Objects)
    private int[] vboIds;

    // EBO ID (Element Buffer Object / Index Buffer)
    private int eboId = 0;

    // Configuración de dibujo
    private int vertexCount;
    private int indexCount;
    private boolean hasIndices;
    private int drawMode = GLES30.GL_TRIANGLES;

    // Para instanced rendering
    private int instanceVboId = 0;
    private int instanceCount = 0;

    /**
     * Constructor privado - usar Builder para crear instancias
     */
    private GLMesh() {}

    /**
     * Vincula este mesh (activa el VAO)
     * Después de bind(), el estado de atributos queda configurado
     */
    public void bind() {
        GLES30.glBindVertexArray(vaoId);
    }

    /**
     * Desvincula el mesh (desactiva VAO)
     */
    public void unbind() {
        GLES30.glBindVertexArray(0);
    }

    /**
     * Dibuja el mesh con la configuración actual
     */
    public void draw() {
        if (hasIndices) {
            GLES30.glDrawElements(drawMode, indexCount, GLES30.GL_UNSIGNED_SHORT, 0);
        } else {
            GLES30.glDrawArrays(drawMode, 0, vertexCount);
        }
    }

    /**
     * Dibuja múltiples instancias del mesh (instanced rendering)
     * @param instances Número de instancias a dibujar
     */
    public void drawInstanced(int instances) {
        if (hasIndices) {
            GLES30.glDrawElementsInstanced(drawMode, indexCount,
                    GLES30.GL_UNSIGNED_SHORT, 0, instances);
        } else {
            GLES30.glDrawArraysInstanced(drawMode, 0, vertexCount, instances);
        }
    }

    /**
     * Actualiza el buffer de instancias con nuevos datos
     * @param data Datos de instancias (posiciones, colores, etc.)
     * @param floatsPerInstance Floats por instancia
     */
    public void updateInstanceBuffer(float[] data, int floatsPerInstance) {
        if (instanceVboId == 0) {
            Log.w(TAG, "No hay buffer de instancias configurado");
            return;
        }

        instanceCount = data.length / floatsPerInstance;

        FloatBuffer buffer = createFloatBuffer(data);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, instanceVboId);
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, data.length * 4, buffer);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
    }

    /**
     * Obtiene el número de instancias actual
     */
    public int getInstanceCount() {
        return instanceCount;
    }

    /**
     * Obtiene el ID del VAO
     */
    public int getVaoId() {
        return vaoId;
    }

    /**
     * Establece el modo de dibujo (GL_TRIANGLES, GL_LINES, etc.)
     */
    public void setDrawMode(int mode) {
        this.drawMode = mode;
    }

    /**
     * Libera todos los recursos GL
     */
    public void dispose() {
        if (vaoId != 0) {
            GLES30.glDeleteVertexArrays(1, new int[]{vaoId}, 0);
            vaoId = 0;
        }

        if (vboIds != null) {
            GLES30.glDeleteBuffers(vboIds.length, vboIds, 0);
            vboIds = null;
        }

        if (eboId != 0) {
            GLES30.glDeleteBuffers(1, new int[]{eboId}, 0);
            eboId = 0;
        }

        if (instanceVboId != 0) {
            GLES30.glDeleteBuffers(1, new int[]{instanceVboId}, 0);
            instanceVboId = 0;
        }

        Log.d(TAG, "Mesh recursos liberados");
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILIDADES ESTÁTICAS
    // ═══════════════════════════════════════════════════════════════

    private static FloatBuffer createFloatBuffer(float[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(data);
        fb.position(0);
        return fb;
    }

    private static ShortBuffer createShortBuffer(short[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 2);
        bb.order(ByteOrder.nativeOrder());
        ShortBuffer sb = bb.asShortBuffer();
        sb.put(data);
        sb.position(0);
        return sb;
    }

    // ═══════════════════════════════════════════════════════════════
    // BUILDER PATTERN
    // ═══════════════════════════════════════════════════════════════

    /**
     * Builder para crear GLMesh de forma fluida
     */
    public static class Builder {
        private java.util.List<VertexAttribute> attributes = new java.util.ArrayList<>();
        private short[] indices = null;
        private int drawMode = GLES30.GL_TRIANGLES;
        private InstanceAttribute instanceAttr = null;

        /**
         * Agrega un buffer de vértices
         * @param data Datos del buffer
         * @param componentsPerVertex Componentes por vértice (ej: 3 para vec3)
         * @return Builder para encadenar
         */
        public Builder addVertexBuffer(float[] data, int componentsPerVertex) {
            attributes.add(new VertexAttribute(data, componentsPerVertex, false, 0));
            return this;
        }

        /**
         * Agrega un buffer de vértices con divisor para instancing
         * @param data Datos del buffer
         * @param componentsPerVertex Componentes por vértice
         * @param divisor 0=per-vertex, 1=per-instance
         * @return Builder para encadenar
         */
        public Builder addVertexBuffer(float[] data, int componentsPerVertex, int divisor) {
            attributes.add(new VertexAttribute(data, componentsPerVertex, divisor > 0, divisor));
            return this;
        }

        /**
         * Agrega buffer de índices (opcional)
         * @param indices Array de índices (short)
         * @return Builder para encadenar
         */
        public Builder setIndexBuffer(short[] indices) {
            this.indices = indices;
            return this;
        }

        /**
         * Establece modo de dibujo
         * @param mode GL_TRIANGLES, GL_LINES, GL_POINTS, etc.
         * @return Builder para encadenar
         */
        public Builder setDrawMode(int mode) {
            this.drawMode = mode;
            return this;
        }

        /**
         * Configura buffer de instancias para instanced rendering
         * @param maxInstances Máximo de instancias soportadas
         * @param floatsPerInstance Floats por instancia
         * @param startAttribute Índice del atributo inicial
         * @return Builder para encadenar
         */
        public Builder setInstanceBuffer(int maxInstances, int floatsPerInstance, int startAttribute) {
            this.instanceAttr = new InstanceAttribute(maxInstances, floatsPerInstance, startAttribute);
            return this;
        }

        /**
         * Construye el GLMesh
         * @return GLMesh configurado y listo para usar
         */
        public GLMesh build() {
            GLMesh mesh = new GLMesh();
            mesh.drawMode = this.drawMode;

            // ═══ CREAR VAO ═══
            int[] vaoArray = new int[1];
            GLES30.glGenVertexArrays(1, vaoArray, 0);
            mesh.vaoId = vaoArray[0];
            GLES30.glBindVertexArray(mesh.vaoId);

            Log.d(TAG, "VAO creado: " + mesh.vaoId);

            // ═══ CREAR VBOs ═══
            mesh.vboIds = new int[attributes.size()];
            GLES30.glGenBuffers(attributes.size(), mesh.vboIds, 0);

            for (int i = 0; i < attributes.size(); i++) {
                VertexAttribute attr = attributes.get(i);

                // Bind VBO
                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mesh.vboIds[i]);

                // Subir datos a GPU
                FloatBuffer buffer = createFloatBuffer(attr.data);
                GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,
                        attr.data.length * 4, buffer, GLES30.GL_STATIC_DRAW);

                // Configurar atributo
                GLES30.glVertexAttribPointer(i, attr.components,
                        GLES30.GL_FLOAT, false, 0, 0);
                GLES30.glEnableVertexAttribArray(i);

                // Divisor para instancing
                if (attr.divisor > 0) {
                    GLES30.glVertexAttribDivisor(i, attr.divisor);
                }

                // Calcular vertex count del primer atributo
                if (i == 0) {
                    mesh.vertexCount = attr.data.length / attr.components;
                }

                Log.d(TAG, "VBO[" + i + "] creado: " + mesh.vboIds[i] +
                           " (" + attr.components + " components, " +
                           attr.data.length + " floats)");
            }

            // ═══ CREAR EBO (índices) ═══
            if (indices != null) {
                int[] eboArray = new int[1];
                GLES30.glGenBuffers(1, eboArray, 0);
                mesh.eboId = eboArray[0];

                GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, mesh.eboId);
                ShortBuffer indexBuffer = createShortBuffer(indices);
                GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER,
                        indices.length * 2, indexBuffer, GLES30.GL_STATIC_DRAW);

                mesh.indexCount = indices.length;
                mesh.hasIndices = true;

                Log.d(TAG, "EBO creado: " + mesh.eboId + " (" + indices.length + " índices)");
            }

            // ═══ CREAR INSTANCE BUFFER ═══
            if (instanceAttr != null) {
                int[] instanceVbo = new int[1];
                GLES30.glGenBuffers(1, instanceVbo, 0);
                mesh.instanceVboId = instanceVbo[0];

                int bufferSize = instanceAttr.maxInstances * instanceAttr.floatsPerInstance * 4;

                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mesh.instanceVboId);
                GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, bufferSize, null, GLES30.GL_DYNAMIC_DRAW);

                // Configurar atributos de instancia
                // Por ejemplo, si floatsPerInstance=7 (pos3 + color4), usamos 2 atributos
                int offset = 0;
                int attrIndex = instanceAttr.startAttribute;
                int stride = instanceAttr.floatsPerInstance * 4;

                // Atributo 1: Posición (vec3)
                if (instanceAttr.floatsPerInstance >= 3) {
                    GLES30.glVertexAttribPointer(attrIndex, 3,
                            GLES30.GL_FLOAT, false, stride, offset);
                    GLES30.glEnableVertexAttribArray(attrIndex);
                    GLES30.glVertexAttribDivisor(attrIndex, 1);
                    offset += 3 * 4;
                    attrIndex++;
                }

                // Atributo 2: Color (vec4) si hay espacio
                if (instanceAttr.floatsPerInstance >= 7) {
                    GLES30.glVertexAttribPointer(attrIndex, 4,
                            GLES30.GL_FLOAT, false, stride, offset);
                    GLES30.glEnableVertexAttribArray(attrIndex);
                    GLES30.glVertexAttribDivisor(attrIndex, 1);
                    offset += 4 * 4;
                    attrIndex++;
                }

                // Atributo 3: Escala (float) si hay espacio
                if (instanceAttr.floatsPerInstance >= 8) {
                    GLES30.glVertexAttribPointer(attrIndex, 1,
                            GLES30.GL_FLOAT, false, stride, offset);
                    GLES30.glEnableVertexAttribArray(attrIndex);
                    GLES30.glVertexAttribDivisor(attrIndex, 1);
                }

                Log.d(TAG, "Instance VBO creado: " + mesh.instanceVboId +
                           " (max " + instanceAttr.maxInstances + " instancias)");
            }

            // Unbind VAO
            GLES30.glBindVertexArray(0);
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
            GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0);

            Log.d(TAG, "═══ GLMesh construido exitosamente ═══");
            Log.d(TAG, "VAO=" + mesh.vaoId + ", VBOs=" + attributes.size() +
                       ", vertices=" + mesh.vertexCount +
                       ", indices=" + mesh.indexCount);

            return mesh;
        }

        // Clase interna para atributos de vértice
        private static class VertexAttribute {
            float[] data;
            int components;
            boolean instanced;
            int divisor;

            VertexAttribute(float[] data, int components, boolean instanced, int divisor) {
                this.data = data;
                this.components = components;
                this.instanced = instanced;
                this.divisor = divisor;
            }
        }

        // Clase interna para atributos de instancia
        private static class InstanceAttribute {
            int maxInstances;
            int floatsPerInstance;
            int startAttribute;

            InstanceAttribute(int maxInstances, int floatsPerInstance, int startAttribute) {
                this.maxInstances = maxInstances;
                this.floatsPerInstance = floatsPerInstance;
                this.startAttribute = startAttribute;
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // MESHES PREDEFINIDOS (Quad, Cube, Sphere básica)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Crea un quad fullscreen (para post-processing, fondos, etc.)
     * Posiciones en NDC (-1 a 1), UVs de 0 a 1
     */
    public static GLMesh createFullscreenQuad() {
        float[] positions = {
            -1.0f, -1.0f, 0.0f,  // Bottom-left
             1.0f, -1.0f, 0.0f,  // Bottom-right
             1.0f,  1.0f, 0.0f,  // Top-right
            -1.0f,  1.0f, 0.0f   // Top-left
        };

        float[] uvs = {
            0.0f, 0.0f,  // Bottom-left
            1.0f, 0.0f,  // Bottom-right
            1.0f, 1.0f,  // Top-right
            0.0f, 1.0f   // Top-left
        };

        short[] indices = {
            0, 1, 2,  // Triangle 1
            0, 2, 3   // Triangle 2
        };

        return new Builder()
                .addVertexBuffer(positions, 3)
                .addVertexBuffer(uvs, 2)
                .setIndexBuffer(indices)
                .build();
    }

    /**
     * Crea un punto para partículas (usado con instancing)
     * @param maxParticles Número máximo de partículas
     */
    public static GLMesh createParticlePoint(int maxParticles) {
        // Geometría mínima: un solo punto
        float[] position = { 0.0f, 0.0f, 0.0f };

        return new Builder()
                .addVertexBuffer(position, 3)
                .setDrawMode(GLES30.GL_POINTS)
                .setInstanceBuffer(maxParticles, 8, 1) // pos(3) + color(4) + size(1)
                .build();
    }

    /**
     * Crea un quad para partículas billboarded (usado con instancing)
     * @param maxParticles Número máximo de partículas
     */
    public static GLMesh createParticleQuad(int maxParticles) {
        // Quad centrado de tamaño 1x1
        float[] positions = {
            -0.5f, -0.5f, 0.0f,
             0.5f, -0.5f, 0.0f,
             0.5f,  0.5f, 0.0f,
            -0.5f,  0.5f, 0.0f
        };

        float[] uvs = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f
        };

        short[] indices = { 0, 1, 2, 0, 2, 3 };

        return new Builder()
                .addVertexBuffer(positions, 3)
                .addVertexBuffer(uvs, 2)
                .setIndexBuffer(indices)
                .setInstanceBuffer(maxParticles, 8, 2) // pos(3) + color(4) + size(1)
                .build();
    }
}
