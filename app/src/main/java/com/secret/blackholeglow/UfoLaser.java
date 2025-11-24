// UfoLaser.java - Rayo láser disparado por el OVNI (OPTIMIZADO)
package com.secret.blackholeglow;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * 🔫 UfoLaser - Rayo láser OPTIMIZADO
 *
 * ⚡ OPTIMIZACIONES:
 * - Shader estático compartido (compilado una sola vez)
 * - FloatBuffer estático reutilizable
 * - Sin allocaciones en update/draw
 * - Sin logs en runtime (solo errores)
 */
public class UfoLaser {
    // ═══════════════════════════════════════════════════════════
    // ⚡ RECURSOS ESTÁTICOS COMPARTIDOS (una sola instancia)
    // ═══════════════════════════════════════════════════════════
    private static int sharedShaderProgram = 0;
    private static int sharedAPositionHandle = -1;
    private static int sharedUColorHandle = -1;
    private static int sharedUMVPHandle = -1;
    private static FloatBuffer sharedVertexBuffer;
    private static final float[] sharedVertices = new float[12];  // 4 vértices x 3

    private static final String VERTEX_SHADER =
        "attribute vec4 a_Position;\n" +
        "uniform mat4 u_MVP;\n" +
        "void main() {\n" +
        "    gl_Position = u_MVP * a_Position;\n" +
        "}";

    private static final String FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "uniform vec4 u_Color;\n" +
        "void main() {\n" +
        "    gl_FragColor = u_Color;\n" +
        "}";

    // ═══════════════════════════════════════════════════════════
    // DATOS DE INSTANCIA (mínimos)
    // ═══════════════════════════════════════════════════════════
    public float x, y, z;
    public float velocityX, velocityY, velocityZ;
    public boolean active = true;
    public boolean hitTarget = false;

    private float lifetime = 0f;
    private static final float MAX_LIFETIME = 3.0f;
    private static final float SPEED = 4.0f;
    private static final float LENGTH = 0.3f;
    private static final float WIDTH = 0.02f;

    /**
     * ⚡ Inicializar recursos estáticos (llamar una vez al inicio)
     */
    public static void initSharedResources() {
        if (sharedShaderProgram != 0) return;  // Ya inicializado

        // Compilar shader una sola vez
        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        sharedShaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(sharedShaderProgram, vertexShader);
        GLES20.glAttachShader(sharedShaderProgram, fragmentShader);
        GLES20.glLinkProgram(sharedShaderProgram);

        sharedAPositionHandle = GLES20.glGetAttribLocation(sharedShaderProgram, "a_Position");
        sharedUColorHandle = GLES20.glGetUniformLocation(sharedShaderProgram, "u_Color");
        sharedUMVPHandle = GLES20.glGetUniformLocation(sharedShaderProgram, "u_MVP");

        // Crear buffer compartido
        ByteBuffer bb = ByteBuffer.allocateDirect(sharedVertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        sharedVertexBuffer = bb.asFloatBuffer();
    }

    private static int compileShader(int type, String code) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);
        return shader;
    }

    /**
     * Constructor LIGERO (sin allocaciones)
     */
    public UfoLaser(float startX, float startY, float startZ,
                    float targetX, float targetY, float targetZ) {
        this.x = startX;
        this.y = startY;
        this.z = startZ;

        // Calcular dirección normalizada
        float dx = targetX - startX;
        float dy = targetY - startY;
        float dz = targetZ - startZ;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist > 0.01f) {
            float invDist = SPEED / dist;
            velocityX = dx * invDist;
            velocityY = dy * invDist;
            velocityZ = dz * invDist;
        }

        // Asegurar que recursos estáticos existan
        if (sharedShaderProgram == 0) {
            initSharedResources();
        }
    }

    /**
     * ⚡ Update OPTIMIZADO (sin allocaciones)
     */
    public void update(float deltaTime, float earthX, float earthY, float earthZ, float earthRadius) {
        if (!active) return;

        // Mover láser
        x += velocityX * deltaTime;
        y += velocityY * deltaTime;
        z += velocityZ * deltaTime;

        // Tiempo de vida
        lifetime += deltaTime;
        if (lifetime >= MAX_LIFETIME) {
            active = false;
            return;
        }

        // Detectar impacto con la Tierra (sin sqrt cuando posible)
        float dx = x - earthX;
        float dy = y - earthY;
        float dz = z - earthZ;
        float distSq = dx * dx + dy * dy + dz * dz;
        float radiusSq = (earthRadius + 0.1f) * (earthRadius + 0.1f);

        if (distSq <= radiusSq) {
            hitTarget = true;
            active = false;
        }
    }

    /**
     * ⚡ Draw OPTIMIZADO (usa recursos estáticos)
     */
    public void draw(float[] mvpMatrix) {
        if (!active || sharedShaderProgram == 0) return;

        GLES20.glUseProgram(sharedShaderProgram);

        // Calcular vértices del quad (en array estático)
        float perpX = -velocityZ;
        float perpZ = velocityX;
        float perpLen = (float) Math.sqrt(perpX * perpX + perpZ * perpZ);

        if (perpLen > 0.01f) {
            float invLen = WIDTH / perpLen;
            perpX *= invLen;
            perpZ *= invLen;
        }

        // Punto trasero del láser
        float backX = x - velocityX * LENGTH / SPEED;
        float backY = y - velocityY * LENGTH / SPEED;
        float backZ = z - velocityZ * LENGTH / SPEED;

        // Llenar array estático (sin crear nuevo array)
        sharedVertices[0] = x - perpX;      sharedVertices[1] = y;      sharedVertices[2] = z - perpZ;
        sharedVertices[3] = x + perpX;      sharedVertices[4] = y;      sharedVertices[5] = z + perpZ;
        sharedVertices[6] = backX - perpX;  sharedVertices[7] = backY;  sharedVertices[8] = backZ - perpZ;
        sharedVertices[9] = backX + perpX;  sharedVertices[10] = backY; sharedVertices[11] = backZ + perpZ;

        // Actualizar buffer compartido
        sharedVertexBuffer.position(0);
        sharedVertexBuffer.put(sharedVertices);
        sharedVertexBuffer.position(0);

        // Configurar atributos
        GLES20.glEnableVertexAttribArray(sharedAPositionHandle);
        GLES20.glVertexAttribPointer(sharedAPositionHandle, 3, GLES20.GL_FLOAT, false, 0, sharedVertexBuffer);

        // Color verde/cyan con pulso
        float pulse = 0.8f + (float) Math.sin(lifetime * 20) * 0.2f;
        GLES20.glUniform4f(sharedUColorHandle, 0.2f * pulse, 1.0f * pulse, 0.8f * pulse, 1.0f);
        GLES20.glUniformMatrix4fv(sharedUMVPHandle, 1, false, mvpMatrix, 0);

        // Dibujar glow exterior
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Núcleo brillante (más delgado)
        float coreWidth = WIDTH * 0.5f;
        if (perpLen > 0.01f) {
            float invLen = coreWidth / perpLen;
            perpX = -velocityZ * invLen;
            perpZ = velocityX * invLen;
        }

        sharedVertices[0] = x - perpX;      sharedVertices[2] = z - perpZ;
        sharedVertices[3] = x + perpX;      sharedVertices[5] = z + perpZ;
        sharedVertices[6] = backX - perpX;  sharedVertices[8] = backZ - perpZ;
        sharedVertices[9] = backX + perpX;  sharedVertices[11] = backZ + perpZ;

        sharedVertexBuffer.position(0);
        sharedVertexBuffer.put(sharedVertices);
        sharedVertexBuffer.position(0);

        GLES20.glUniform4f(sharedUColorHandle, 1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(sharedAPositionHandle);
    }
}
