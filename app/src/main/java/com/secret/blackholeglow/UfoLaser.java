// UfoLaser.java - Rayo láser disparado por el OVNI
package com.secret.blackholeglow;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * 🔫 UfoLaser - Rayo láser que dispara el OVNI hacia la Tierra
 *
 * Características:
 * - Viaja desde el OVNI hacia la Tierra
 * - Efecto de glow verde/cyan alien
 * - Detecta impacto con la Tierra
 * - Se destruye al impactar o salir de pantalla
 */
public class UfoLaser {
    private static final String TAG = "UfoLaser";

    // Posición y movimiento
    public float x, y, z;
    public float targetX, targetY, targetZ;
    public float velocityX, velocityY, velocityZ;
    private float speed = 4.0f;  // Velocidad del láser

    // Estado
    public boolean active = true;
    public boolean hitTarget = false;
    private float lifetime = 0f;
    private float maxLifetime = 3.0f;  // Máximo 3 segundos de vida

    // Visual
    private float length = 0.3f;
    private float width = 0.02f;
    private float glowIntensity = 1.0f;

    // OpenGL
    private int shaderProgram;
    private FloatBuffer vertexBuffer;
    private int aPositionHandle;
    private int uColorHandle;
    private int uMVPHandle;

    // Shader simple para el láser
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

    // Buffer de vértices (línea gruesa como quad)
    private float[] vertices = new float[12];  // 4 vértices x 3 componentes

    /**
     * Constructor
     */
    public UfoLaser(float startX, float startY, float startZ,
                    float targetX, float targetY, float targetZ) {
        this.x = startX;
        this.y = startY;
        this.z = startZ;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;

        // Calcular dirección normalizada
        float dx = targetX - startX;
        float dy = targetY - startY;
        float dz = targetZ - startZ;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist > 0.01f) {
            velocityX = (dx / dist) * speed;
            velocityY = (dy / dist) * speed;
            velocityZ = (dz / dist) * speed;
        }

        // Crear shader
        createShader();

        // Crear buffer de vértices
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();

        Log.d(TAG, "🔫 Láser creado desde (" + startX + "," + startY + "," + startZ + ")");
    }

    private void createShader() {
        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vertexShader);
        GLES20.glAttachShader(shaderProgram, fragmentShader);
        GLES20.glLinkProgram(shaderProgram);

        aPositionHandle = GLES20.glGetAttribLocation(shaderProgram, "a_Position");
        uColorHandle = GLES20.glGetUniformLocation(shaderProgram, "u_Color");
        uMVPHandle = GLES20.glGetUniformLocation(shaderProgram, "u_MVP");
    }

    private int compileShader(int type, String code) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);
        return shader;
    }

    /**
     * Actualizar posición del láser
     */
    public void update(float deltaTime, float earthX, float earthY, float earthZ, float earthRadius) {
        if (!active) return;

        // Mover láser
        x += velocityX * deltaTime;
        y += velocityY * deltaTime;
        z += velocityZ * deltaTime;

        // Incrementar tiempo de vida
        lifetime += deltaTime;
        if (lifetime >= maxLifetime) {
            active = false;
            return;
        }

        // Efecto de pulso en el glow
        glowIntensity = 0.8f + (float) Math.sin(lifetime * 20) * 0.2f;

        // Detectar impacto con la Tierra
        float dx = x - earthX;
        float dy = y - earthY;
        float dz = z - earthZ;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist <= earthRadius + 0.1f) {
            hitTarget = true;
            active = false;
            Log.d(TAG, "💥 Láser impactó la Tierra!");
        }
    }

    /**
     * Dibujar el láser
     */
    public void draw(float[] mvpMatrix) {
        if (!active) return;

        GLES20.glUseProgram(shaderProgram);

        // Calcular vértices del quad (línea gruesa)
        // Perpendicular a la dirección de movimiento
        float perpX = -velocityZ;
        float perpZ = velocityX;
        float perpLen = (float) Math.sqrt(perpX * perpX + perpZ * perpZ);
        if (perpLen > 0.01f) {
            perpX = (perpX / perpLen) * width;
            perpZ = (perpZ / perpLen) * width;
        }

        // Punto trasero del láser
        float backX = x - velocityX * length / speed;
        float backY = y - velocityY * length / speed;
        float backZ = z - velocityZ * length / speed;

        // 4 vértices del quad
        vertices[0] = x - perpX;      vertices[1] = y;      vertices[2] = z - perpZ;
        vertices[3] = x + perpX;      vertices[4] = y;      vertices[5] = z + perpZ;
        vertices[6] = backX - perpX;  vertices[7] = backY;  vertices[8] = backZ - perpZ;
        vertices[9] = backX + perpX;  vertices[10] = backY; vertices[11] = backZ + perpZ;

        vertexBuffer.position(0);
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // Configurar atributos
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        // Color verde/cyan alien con glow
        GLES20.glUniform4f(uColorHandle, 0.2f * glowIntensity, 1.0f * glowIntensity, 0.8f * glowIntensity, 1.0f);
        GLES20.glUniformMatrix4fv(uMVPHandle, 1, false, mvpMatrix, 0);

        // Dibujar como triangle strip
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Dibujar núcleo más brillante (más delgado)
        width = 0.01f;
        perpX = -velocityZ;
        perpZ = velocityX;
        if (perpLen > 0.01f) {
            perpX = (perpX / perpLen) * width;
            perpZ = (perpZ / perpLen) * width;
        }

        vertices[0] = x - perpX;      vertices[1] = y;      vertices[2] = z - perpZ;
        vertices[3] = x + perpX;      vertices[4] = y;      vertices[5] = z + perpZ;
        vertices[6] = backX - perpX;  vertices[7] = backY;  vertices[8] = backZ - perpZ;
        vertices[9] = backX + perpX;  vertices[10] = backY; vertices[11] = backZ + perpZ;

        vertexBuffer.position(0);
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // Núcleo blanco brillante
        GLES20.glUniform4f(uColorHandle, 1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Restaurar width
        width = 0.02f;

        GLES20.glDisableVertexAttribArray(aPositionHandle);
    }

    /**
     * Verificar si el láser colisiona con un meteoro
     */
    public boolean checkMeteorCollision(float meteorX, float meteorY, float meteorZ, float meteorRadius) {
        float dx = x - meteorX;
        float dy = y - meteorY;
        float dz = z - meteorZ;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        return dist <= meteorRadius + 0.1f;
    }
}
