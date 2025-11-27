package com.secret.blackholeglow.gl3;

import android.content.Context;
import android.opengl.GLES30;
import android.util.Log;

import com.secret.blackholeglow.SceneObject;
import com.secret.blackholeglow.TimeManager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║   ✨ InstancedParticles - Sistema de Partículas OpenGL ES 3.0 ✨  ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * Renderiza miles de partículas con UNA sola draw call usando instancing.
 *
 * Rendimiento típico:
 * - Sin instancing: 1000 partículas = 1000 draw calls = ~15 FPS
 * - Con instancing: 1000 partículas = 1 draw call = ~60 FPS
 *
 * Cada partícula tiene:
 * - Posición (vec3)
 * - Color (vec4)
 * - Tamaño (float)
 * - Velocidad (para update)
 * - Tiempo de vida
 */
public class InstancedParticles implements SceneObject {
    private static final String TAG = "InstancedParticles";

    // Configuración
    private final int maxParticles;
    private int activeParticles;

    // OpenGL
    private int vaoId;
    private int quadVboId;
    private int instanceVboId;
    private ShaderProgram3 shader;

    // Datos de partículas (CPU side)
    private final float[] particleData;  // pos(3) + color(4) + size(1) = 8 floats/particle
    private FloatBuffer particleBuffer;

    // Estado de cada partícula
    private final float[] velocities;    // vel(3) per particle
    private final float[] lifetimes;     // lifetime per particle
    private final float[] maxLifetimes;  // max lifetime per particle

    // Configuración de emisión
    private float emissionRate = 50f;    // Partículas por segundo
    private float emissionAccum = 0f;
    private final float[] emitterPosition = {0f, 0f, 0f};
    private final float[] emitterVelocityMin = {-0.5f, 0.5f, -0.5f};
    private final float[] emitterVelocityMax = {0.5f, 2f, 0.5f};
    private float particleSizeMin = 0.05f;
    private float particleSizeMax = 0.15f;
    private float lifetimeMin = 1f;
    private float lifetimeMax = 3f;

    // Color base (puede variar por partícula)
    private final float[] baseColor = {1f, 0.8f, 0.3f, 1f};  // Naranja dorado

    // Random reusable
    private final Random random = new Random();

    // Matrices
    private final float[] vpMatrix = new float[16];

    /**
     * Constructor
     * @param context Context de Android
     * @param maxParticles Número máximo de partículas
     */
    public InstancedParticles(Context context, int maxParticles) {
        this.maxParticles = maxParticles;
        this.activeParticles = 0;

        // Inicializar arrays
        this.particleData = new float[maxParticles * 8];  // 8 floats per particle
        this.velocities = new float[maxParticles * 3];
        this.lifetimes = new float[maxParticles];
        this.maxLifetimes = new float[maxParticles];

        // Crear buffer nativo
        ByteBuffer bb = ByteBuffer.allocateDirect(maxParticles * 8 * 4);
        bb.order(ByteOrder.nativeOrder());
        this.particleBuffer = bb.asFloatBuffer();

        // Inicializar OpenGL
        initGL(context);

        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║   ✨ INSTANCED PARTICLES CREADO ✨     ║");
        Log.d(TAG, "║   Max: " + maxParticles + " partículas              ║");
        Log.d(TAG, "║   1 Draw Call para TODAS              ║");
        Log.d(TAG, "╚════════════════════════════════════════╝");
    }

    private void initGL(Context context) {
        // ═══ CREAR VAO ═══
        int[] vaoArray = new int[1];
        GLES30.glGenVertexArrays(1, vaoArray, 0);
        vaoId = vaoArray[0];
        GLES30.glBindVertexArray(vaoId);

        // ═══ CREAR QUAD VBO (geometría compartida) ═══
        float[] quadVertices = {
            // Posición (3)    // UV (2)
            -0.5f, -0.5f, 0f,  0f, 0f,
             0.5f, -0.5f, 0f,  1f, 0f,
             0.5f,  0.5f, 0f,  1f, 1f,
            -0.5f, -0.5f, 0f,  0f, 0f,
             0.5f,  0.5f, 0f,  1f, 1f,
            -0.5f,  0.5f, 0f,  0f, 1f
        };

        int[] vbos = new int[2];
        GLES30.glGenBuffers(2, vbos, 0);
        quadVboId = vbos[0];
        instanceVboId = vbos[1];

        // Subir datos del quad
        FloatBuffer quadBuffer = createFloatBuffer(quadVertices);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, quadVboId);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,
                quadVertices.length * 4, quadBuffer, GLES30.GL_STATIC_DRAW);

        // Configurar atributos del quad (location 0 = position, 1 = texcoord)
        int stride = 5 * 4;  // 5 floats * 4 bytes

        // location 0: a_Position (vec3)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0);
        GLES30.glEnableVertexAttribArray(0);

        // location 1: a_TexCoord (vec2)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, stride, 3 * 4);
        GLES30.glEnableVertexAttribArray(1);

        // ═══ CREAR INSTANCE VBO ═══
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, instanceVboId);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,
                maxParticles * 8 * 4, null, GLES30.GL_DYNAMIC_DRAW);

        // Configurar atributos de instancia
        int instanceStride = 8 * 4;  // 8 floats * 4 bytes

        // location 2: a_InstancePos (vec3)
        GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, instanceStride, 0);
        GLES30.glEnableVertexAttribArray(2);
        GLES30.glVertexAttribDivisor(2, 1);  // 1 = per instance

        // location 3: a_InstanceColor (vec4)
        GLES30.glVertexAttribPointer(3, 4, GLES30.GL_FLOAT, false, instanceStride, 3 * 4);
        GLES30.glEnableVertexAttribArray(3);
        GLES30.glVertexAttribDivisor(3, 1);

        // location 4: a_InstanceSize (float)
        GLES30.glVertexAttribPointer(4, 1, GLES30.GL_FLOAT, false, instanceStride, 7 * 4);
        GLES30.glEnableVertexAttribArray(4);
        GLES30.glVertexAttribDivisor(4, 1);

        // Unbind
        GLES30.glBindVertexArray(0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);

        // ═══ CREAR SHADER ═══
        shader = new ShaderProgram3(context,
                "shaders/gl3/particle_instanced_vertex.glsl",
                "shaders/gl3/particle_instanced_fragment.glsl");

        if (!shader.isValid()) {
            Log.e(TAG, "Error creando shader de partículas instanciadas");
        }

        Log.d(TAG, "OpenGL inicializado - VAO: " + vaoId);
    }

    /**
     * Establece la posición del emisor
     */
    public void setEmitterPosition(float x, float y, float z) {
        emitterPosition[0] = x;
        emitterPosition[1] = y;
        emitterPosition[2] = z;
    }

    /**
     * Establece el rango de velocidad inicial
     */
    public void setVelocityRange(float[] min, float[] max) {
        System.arraycopy(min, 0, emitterVelocityMin, 0, 3);
        System.arraycopy(max, 0, emitterVelocityMax, 0, 3);
    }

    /**
     * Establece la tasa de emisión
     */
    public void setEmissionRate(float particlesPerSecond) {
        this.emissionRate = particlesPerSecond;
    }

    /**
     * Establece el color base
     */
    public void setBaseColor(float r, float g, float b, float a) {
        baseColor[0] = r;
        baseColor[1] = g;
        baseColor[2] = b;
        baseColor[3] = a;
    }

    /**
     * Emite una explosión de partículas
     */
    public void burst(int count) {
        for (int i = 0; i < count && activeParticles < maxParticles; i++) {
            spawnParticle();
        }
    }

    /**
     * Establece la matriz View-Projection
     */
    public void setVPMatrix(float[] vp) {
        System.arraycopy(vp, 0, vpMatrix, 0, 16);
    }

    @Override
    public void update(float deltaTime) {
        // ═══ EMITIR NUEVAS PARTÍCULAS ═══
        emissionAccum += emissionRate * deltaTime;
        while (emissionAccum >= 1f && activeParticles < maxParticles) {
            spawnParticle();
            emissionAccum -= 1f;
        }

        // ═══ ACTUALIZAR PARTÍCULAS EXISTENTES ═══
        int writeIndex = 0;
        for (int i = 0; i < activeParticles; i++) {
            int dataIdx = i * 8;
            int velIdx = i * 3;

            // Actualizar tiempo de vida
            lifetimes[i] -= deltaTime;

            if (lifetimes[i] > 0) {
                // Partícula viva - actualizar posición
                particleData[dataIdx + 0] += velocities[velIdx + 0] * deltaTime;
                particleData[dataIdx + 1] += velocities[velIdx + 1] * deltaTime;
                particleData[dataIdx + 2] += velocities[velIdx + 2] * deltaTime;

                // Aplicar gravedad suave
                velocities[velIdx + 1] -= 0.5f * deltaTime;

                // Fade out basado en tiempo de vida
                float lifeRatio = lifetimes[i] / maxLifetimes[i];
                particleData[dataIdx + 6] = baseColor[3] * lifeRatio;  // Alpha

                // Copiar a posición de escritura (compactar array)
                if (writeIndex != i) {
                    System.arraycopy(particleData, dataIdx, particleData, writeIndex * 8, 8);
                    System.arraycopy(velocities, velIdx, velocities, writeIndex * 3, 3);
                    lifetimes[writeIndex] = lifetimes[i];
                    maxLifetimes[writeIndex] = maxLifetimes[i];
                }
                writeIndex++;
            }
        }

        activeParticles = writeIndex;

        // ═══ SUBIR DATOS A GPU ═══
        if (activeParticles > 0) {
            particleBuffer.clear();
            particleBuffer.put(particleData, 0, activeParticles * 8);
            particleBuffer.position(0);

            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, instanceVboId);
            GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0,
                    activeParticles * 8 * 4, particleBuffer);
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
        }
    }

    @Override
    public void draw() {
        if (activeParticles == 0 || !shader.isValid()) {
            return;
        }

        // Configurar blending para partículas aditivas
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE);

        // Desactivar depth write (partículas no bloquean otras partículas)
        GLES30.glDepthMask(false);

        // Usar shader
        shader.use();
        shader.setUniformMatrix4fv("u_VP", vpMatrix);
        shader.setUniform("u_Time", TimeManager.getTime());

        // Bind VAO y dibujar instanciado
        GLES30.glBindVertexArray(vaoId);
        GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLES, 0, 6, activeParticles);
        GLES30.glBindVertexArray(0);

        // Restaurar estado
        GLES30.glDepthMask(true);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void spawnParticle() {
        int idx = activeParticles;
        int dataIdx = idx * 8;
        int velIdx = idx * 3;

        // Posición inicial (en el emisor con variación)
        particleData[dataIdx + 0] = emitterPosition[0] + randomRange(-0.1f, 0.1f);
        particleData[dataIdx + 1] = emitterPosition[1] + randomRange(-0.1f, 0.1f);
        particleData[dataIdx + 2] = emitterPosition[2] + randomRange(-0.1f, 0.1f);

        // Color (con variación)
        particleData[dataIdx + 3] = baseColor[0] * randomRange(0.8f, 1.2f);
        particleData[dataIdx + 4] = baseColor[1] * randomRange(0.8f, 1.2f);
        particleData[dataIdx + 5] = baseColor[2] * randomRange(0.8f, 1.2f);
        particleData[dataIdx + 6] = baseColor[3];

        // Tamaño
        particleData[dataIdx + 7] = randomRange(particleSizeMin, particleSizeMax);

        // Velocidad
        velocities[velIdx + 0] = randomRange(emitterVelocityMin[0], emitterVelocityMax[0]);
        velocities[velIdx + 1] = randomRange(emitterVelocityMin[1], emitterVelocityMax[1]);
        velocities[velIdx + 2] = randomRange(emitterVelocityMin[2], emitterVelocityMax[2]);

        // Tiempo de vida
        maxLifetimes[idx] = randomRange(lifetimeMin, lifetimeMax);
        lifetimes[idx] = maxLifetimes[idx];

        activeParticles++;
    }

    private float randomRange(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private FloatBuffer createFloatBuffer(float[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(data);
        fb.position(0);
        return fb;
    }

    /**
     * Obtiene el número de partículas activas
     */
    public int getActiveCount() {
        return activeParticles;
    }

    /**
     * Libera recursos
     */
    public void dispose() {
        if (vaoId != 0) {
            GLES30.glDeleteVertexArrays(1, new int[]{vaoId}, 0);
        }
        if (quadVboId != 0) {
            GLES30.glDeleteBuffers(1, new int[]{quadVboId}, 0);
        }
        if (instanceVboId != 0) {
            GLES30.glDeleteBuffers(1, new int[]{instanceVboId}, 0);
        }
        if (shader != null) {
            shader.dispose();
        }
        Log.d(TAG, "InstancedParticles liberado");
    }
}
