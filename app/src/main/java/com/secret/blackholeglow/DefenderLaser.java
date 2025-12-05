// DefenderLaser.java - Láser de la nave defensora
package com.secret.blackholeglow;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   🔵 DefenderLaser - Láser azul de la nave defensora                     ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  • Color: Azul brillante (diferente al verde del OVNI)                   ║
 * ║  • Viaja hacia el objetivo (OVNI)                                        ║
 * ║  • Efecto de estela luminosa                                             ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class DefenderLaser {

    // Posición actual
    public float x, y, z;

    // Dirección normalizada
    private float dirX, dirY, dirZ;

    // Estado
    public boolean active = true;
    public boolean hitTarget = false;

    // Configuración
    private static final float SPEED = 6.0f;           // Más rápido que el del OVNI
    private static final float MAX_DISTANCE = 10.0f;
    private static final float LENGTH = 0.25f;         // Longitud del rayo
    private static final float WIDTH = 0.02f;          // Ancho del rayo

    private float distanceTraveled = 0f;
    private float startX, startY, startZ;

    // OpenGL
    private static int shaderProgram = 0;
    private static int aPositionHandle;
    private static int aColorHandle;
    private static int uMVPMatrixHandle;

    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;

    // Tiempo para efectos
    private final long startTime = System.currentTimeMillis();

    /**
     * Constructor - crear láser desde origen hacia destino
     */
    public DefenderLaser(float startX, float startY, float startZ,
                         float targetX, float targetY, float targetZ) {
        this.x = startX;
        this.y = startY;
        this.z = startZ;
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;

        // Calcular dirección normalizada
        float dx = targetX - startX;
        float dy = targetY - startY;
        float dz = targetZ - startZ;
        float length = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);

        if (length > 0.001f) {
            dirX = dx / length;
            dirY = dy / length;
            dirZ = dz / length;
        } else {
            dirX = 0;
            dirY = 0;
            dirZ = 1;
        }

        // Inicializar shaders si no existen
        if (shaderProgram == 0) {
            initShaders();
        }

        // Crear buffers
        createBuffers();
    }

    /**
     * Inicializar shaders (una sola vez)
     */
    private static void initShaders() {
        String vertexShaderCode =
            "attribute vec4 a_Position;\n" +
            "attribute vec4 a_Color;\n" +
            "uniform mat4 u_MVPMatrix;\n" +
            "varying vec4 v_Color;\n" +
            "void main() {\n" +
            "  gl_Position = u_MVPMatrix * a_Position;\n" +
            "  v_Color = a_Color;\n" +
            "}";

        String fragmentShaderCode =
            "precision mediump float;\n" +
            "varying vec4 v_Color;\n" +
            "void main() {\n" +
            "  gl_FragColor = v_Color;\n" +
            "}";

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vertexShader);
        GLES20.glAttachShader(shaderProgram, fragmentShader);
        GLES20.glLinkProgram(shaderProgram);

        aPositionHandle = GLES20.glGetAttribLocation(shaderProgram, "a_Position");
        aColorHandle = GLES20.glGetAttribLocation(shaderProgram, "a_Color");
        uMVPMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "u_MVPMatrix");
    }

    private static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    /**
     * Crear buffers para el láser (quad orientado hacia la cámara)
     */
    private void createBuffers() {
        // 4 vértices para un quad
        vertexBuffer = ByteBuffer.allocateDirect(4 * 3 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();

        colorBuffer = ByteBuffer.allocateDirect(4 * 4 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
    }

    /**
     * Actualizar posición del láser
     */
    public void update(float deltaTime) {
        if (!active) return;

        // Mover en la dirección
        float move = SPEED * deltaTime;
        x += dirX * move;
        y += dirY * move;
        z += dirZ * move;

        distanceTraveled += move;

        // Desactivar si viajó demasiado
        if (distanceTraveled > MAX_DISTANCE) {
            active = false;
        }
    }

    /**
     * Distancia a un punto
     */
    public float distanceTo(float tx, float ty, float tz) {
        float dx = x - tx;
        float dy = y - ty;
        float dz = z - tz;
        return (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    /**
     * Dibujar el láser
     */
    public void draw(float[] mvpMatrix) {
        if (!active) return;

        GLES20.glUseProgram(shaderProgram);

        // Blending aditivo para brillo
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);

        // Calcular vértices del láser (línea gruesa en 3D)
        // Punta del láser
        float frontX = x;
        float frontY = y;
        float frontZ = z;

        // Cola del láser
        float backX = x - dirX * LENGTH;
        float backY = y - dirY * LENGTH;
        float backZ = z - dirZ * LENGTH;

        // Crear un quad perpendicular a la dirección
        // Usar una perpendicular simple (cross product con UP)
        float perpX = -dirZ * WIDTH;
        float perpY = 0;
        float perpZ = dirX * WIDTH;

        // Si la dirección es muy vertical, usar otra perpendicular
        if (Math.abs(dirY) > 0.9f) {
            perpX = WIDTH;
            perpY = 0;
            perpZ = 0;
        }

        float[] vertices = {
            // Frente
            frontX - perpX, frontY - perpY, frontZ - perpZ,
            frontX + perpX, frontY + perpY, frontZ + perpZ,
            // Atrás
            backX - perpX, backY - perpY, backZ - perpZ,
            backX + perpX, backY + perpY, backZ + perpZ,
        };

        // Colores: Azul brillante con gradiente (frente más brillante)
        float pulse = (float)Math.sin((System.currentTimeMillis() - startTime) * 0.02f) * 0.2f + 0.8f;

        float[] colors = {
            // Frente: blanco-azul brillante
            0.7f * pulse, 0.9f * pulse, 1.0f, 1.0f,
            0.7f * pulse, 0.9f * pulse, 1.0f, 1.0f,
            // Atrás: azul más oscuro con fade
            0.2f, 0.5f, 1.0f, 0.3f,
            0.2f, 0.5f, 1.0f, 0.3f,
        };

        vertexBuffer.clear();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        colorBuffer.clear();
        colorBuffer.put(colors);
        colorBuffer.position(0);

        // Uniforms
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);

        // Atributos
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(aColorHandle);
        GLES20.glVertexAttribPointer(aColorHandle, 4, GLES20.GL_FLOAT, false, 0, colorBuffer);

        // Dibujar como triangle strip
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // === GLOW (versión más grande y transparente) ===
        float glowWidth = WIDTH * 3f;
        perpX = -dirZ * glowWidth;
        perpZ = dirX * glowWidth;

        if (Math.abs(dirY) > 0.9f) {
            perpX = glowWidth;
            perpZ = 0;
        }

        float[] glowVertices = {
            frontX - perpX, frontY, frontZ - perpZ,
            frontX + perpX, frontY, frontZ + perpZ,
            backX - perpX, backY, backZ - perpZ,
            backX + perpX, backY, backZ + perpZ,
        };

        float[] glowColors = {
            0.3f, 0.6f, 1.0f, 0.4f * pulse,
            0.3f, 0.6f, 1.0f, 0.4f * pulse,
            0.1f, 0.3f, 0.8f, 0.0f,
            0.1f, 0.3f, 0.8f, 0.0f,
        };

        vertexBuffer.clear();
        vertexBuffer.put(glowVertices);
        vertexBuffer.position(0);

        colorBuffer.clear();
        colorBuffer.put(glowColors);
        colorBuffer.position(0);

        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glVertexAttribPointer(aColorHandle, 4, GLES20.GL_FLOAT, false, 0, colorBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(aPositionHandle);
        GLES20.glDisableVertexAttribArray(aColorHandle);

        // Restaurar blending normal
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }
}
