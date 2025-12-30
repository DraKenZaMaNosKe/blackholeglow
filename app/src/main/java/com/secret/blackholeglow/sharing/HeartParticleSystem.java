package com.secret.blackholeglow.sharing;

import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * 💖 SISTEMA DE PARTÍCULAS DE CORAZONES
 *
 * Crea una explosión de corazoncitos cuando el usuario
 * toca el botón de Like. Estilo TikTok/Instagram.
 *
 * Características:
 * - Múltiples corazones con diferentes tamaños
 * - Colores variados (rosa, rojo, magenta)
 * - Física con gravedad y velocidad inicial
 * - Fade out gradual
 * - Rotación individual
 */
public class HeartParticleSystem {
    private static final String TAG = "HeartParticles";
    private static final int MAX_PARTICLES = 30;

    // OpenGL
    private int programId;
    private FloatBuffer heartBuffer;
    private boolean isInitialized = false;

    // Uniforms/Attributes
    private int positionHandle;
    private int mvpMatrixHandle;
    private int colorHandle;

    // Partículas activas
    private List<HeartParticle> particles = new ArrayList<>();
    private Random random = new Random();

    // ⚡ OPTIMIZACIÓN: Matrices reutilizables (evita allocations cada frame)
    private float[] reusableModelMatrix = new float[16];
    private float[] reusableFinalMatrix = new float[16];
    private float[] reusableColor = new float[4];

    // Shaders
    private static final String VERTEX_SHADER =
            "attribute vec4 a_Position;\n" +
            "uniform mat4 u_MVPMatrix;\n" +
            "void main() {\n" +
            "    gl_Position = u_MVPMatrix * a_Position;\n" +
            "}";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform vec4 u_Color;\n" +
            "void main() {\n" +
            "    gl_FragColor = u_Color;\n" +
            "}";

    // Colores de corazones (rosa, rojo, magenta, coral)
    private float[][] heartColors = {
            {1.0f, 0.4f, 0.6f, 1.0f},   // Rosa
            {1.0f, 0.2f, 0.3f, 1.0f},   // Rojo
            {1.0f, 0.3f, 0.8f, 1.0f},   // Magenta
            {1.0f, 0.5f, 0.5f, 1.0f},   // Coral
            {1.0f, 0.6f, 0.8f, 1.0f},   // Rosa claro
            {0.9f, 0.1f, 0.4f, 1.0f},   // Rojo intenso
    };

    /**
     * 🎨 Inicializa OpenGL
     */
    public void init() {
        if (isInitialized) return;

        // Crear programa
        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        programId = GLES30.glCreateProgram();
        GLES30.glAttachShader(programId, vertexShader);
        GLES30.glAttachShader(programId, fragmentShader);
        GLES30.glLinkProgram(programId);

        // Obtener handles
        positionHandle = GLES30.glGetAttribLocation(programId, "a_Position");
        mvpMatrixHandle = GLES30.glGetUniformLocation(programId, "u_MVPMatrix");
        colorHandle = GLES30.glGetUniformLocation(programId, "u_Color");

        // Crear geometría del corazón
        createHeartGeometry();

        isInitialized = true;
        Log.d(TAG, "💖 HeartParticleSystem inicializado");
    }

    /**
     * 💖 Crea la geometría de un corazón pequeño
     */
    private void createHeartGeometry() {
        int segments = 32;
        float[] vertices = new float[(segments + 2) * 2];

        // Centro
        vertices[0] = 0f;
        vertices[1] = -0.1f;

        // Puntos del corazón (ecuación paramétrica)
        for (int i = 0; i <= segments; i++) {
            float t = (float) (2.0 * Math.PI * i / segments);
            float x = (float) (16.0 * Math.pow(Math.sin(t), 3));
            float y = (float) (13.0 * Math.cos(t) - 5.0 * Math.cos(2*t)
                    - 2.0 * Math.cos(3*t) - Math.cos(4*t));
            vertices[(i + 1) * 2] = x / 17.0f;
            vertices[(i + 1) * 2 + 1] = y / 17.0f;
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        heartBuffer = bb.asFloatBuffer();
        heartBuffer.put(vertices);
        heartBuffer.position(0);
    }

    /**
     * 💥 Genera una explosión de corazones
     */
    public void emit(float x, float y, int count) {
        for (int i = 0; i < count && particles.size() < MAX_PARTICLES; i++) {
            HeartParticle p = new HeartParticle();
            p.x = x;
            p.y = y;

            // Velocidad inicial en forma de abanico hacia arriba
            float angle = (float) (Math.PI * 0.3 + Math.random() * Math.PI * 0.4); // 54° a 126°
            float speed = 0.8f + random.nextFloat() * 1.2f;
            p.vx = (float) Math.cos(angle) * speed * (random.nextBoolean() ? 1 : -1);
            p.vy = (float) Math.sin(angle) * speed;

            // Tamaño aleatorio
            p.size = 0.03f + random.nextFloat() * 0.04f;

            // Color aleatorio de la paleta
            p.colorIndex = random.nextInt(heartColors.length);

            // Rotación
            p.rotation = random.nextFloat() * 360f;
            p.rotationSpeed = -100f + random.nextFloat() * 200f;

            // Vida
            p.life = 1.0f;
            p.decay = 0.015f + random.nextFloat() * 0.01f;

            particles.add(p);
        }

        Log.d(TAG, "💥 Emitidas " + count + " partículas (total: " + particles.size() + ")");
    }

    /**
     * 🔄 Actualiza todas las partículas
     */
    public void update(float deltaTime) {
        Iterator<HeartParticle> it = particles.iterator();
        while (it.hasNext()) {
            HeartParticle p = it.next();

            // Física
            p.vy -= 2.5f * deltaTime;  // Gravedad
            p.x += p.vx * deltaTime;
            p.y += p.vy * deltaTime;

            // Rotación
            p.rotation += p.rotationSpeed * deltaTime;

            // Vida
            p.life -= p.decay;

            // Eliminar partículas muertas
            if (p.life <= 0 || p.y < -1.5f) {
                it.remove();
            }
        }
    }

    /**
     * 🎬 Dibuja todas las partículas
     * ⚡ OPTIMIZADO: Reutiliza matrices y colores (no crea nuevos cada frame)
     */
    public void draw(float[] mvpMatrix) {
        if (!isInitialized || particles.isEmpty()) return;

        GLES30.glUseProgram(programId);

        // Habilitar blending
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // Habilitar atributos UNA VEZ para todas las partículas
        GLES30.glEnableVertexAttribArray(positionHandle);
        GLES30.glVertexAttribPointer(positionHandle, 2, GLES30.GL_FLOAT, false, 0, heartBuffer);

        HeartParticle[] particlesCopy = particles.toArray(new HeartParticle[0]);
        for (HeartParticle p : particlesCopy) {
            // ⚡ OPTIMIZADO: Usar matrices reutilizables
            android.opengl.Matrix.setIdentityM(reusableModelMatrix, 0);
            android.opengl.Matrix.translateM(reusableModelMatrix, 0, p.x, p.y, 0);
            android.opengl.Matrix.rotateM(reusableModelMatrix, 0, p.rotation, 0, 0, 1);
            android.opengl.Matrix.scaleM(reusableModelMatrix, 0, p.size, p.size, 1);

            android.opengl.Matrix.multiplyMM(reusableFinalMatrix, 0, mvpMatrix, 0, reusableModelMatrix, 0);

            GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, reusableFinalMatrix, 0);

            // ⚡ OPTIMIZADO: Usar color reutilizable (no clone())
            float[] baseColor = heartColors[p.colorIndex];
            reusableColor[0] = baseColor[0];
            reusableColor[1] = baseColor[1];
            reusableColor[2] = baseColor[2];
            reusableColor[3] = p.life;  // Alpha = vida restante
            GLES30.glUniform4fv(colorHandle, 1, reusableColor, 0);

            // Dibujar corazón
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 34);
        }

        GLES30.glDisableVertexAttribArray(positionHandle);
    }

    /**
     * 📊 Retorna si hay partículas activas
     */
    public boolean hasActiveParticles() {
        return !particles.isEmpty();
    }

    /**
     * 🎨 Carga un shader
     */
    private int loadShader(int type, String shaderCode) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);
        return shader;
    }

    /**
     * 🗑️ Libera recursos
     */
    public void cleanup() {
        if (programId != 0) {
            GLES30.glDeleteProgram(programId);
            programId = 0;
        }
        particles.clear();
        isInitialized = false;
    }

    /**
     * 💖 Clase interna para cada partícula
     */
    private static class HeartParticle {
        float x, y;           // Posición
        float vx, vy;         // Velocidad
        float size;           // Tamaño
        float rotation;       // Rotación actual
        float rotationSpeed;  // Velocidad de rotación
        float life;           // Vida (1.0 a 0.0)
        float decay;          // Velocidad de decay
        int colorIndex;       // Índice de color
    }
}
