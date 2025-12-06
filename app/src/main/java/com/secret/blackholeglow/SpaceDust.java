package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔════════════════════════════════════════════════════════════════════════╗
 * ║  ✨ POLVO ESPACIAL - Partículas de Gas Interestelar                   ║
 * ║  Crea la ilusión de viajar a través del espacio                       ║
 * ╚════════════════════════════════════════════════════════════════════════╝
 *
 * Características:
 * - Partículas pequeñas que pasan horizontalmente
 * - Velocidades variadas para efecto de profundidad
 * - Colores tenues (blanco, azul claro, dorado)
 * - Estelas sutiles para sensación de velocidad
 * - Aparición ocasional (no constante)
 */
public class SpaceDust implements SceneObject {
    private static final String TAG = "SpaceDust";

    // ════════════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN - 🚀 AUMENTADA para efecto VISIBLE
    // ════════════════════════════════════════════════════════════════════════
    private static final int MAX_PARTICLES = 40;          // Más partículas
    private static final float SPAWN_RATE = 3.0f;         // Partículas por segundo (más frecuente)
    private static final float MIN_SPEED = 0.025f;        // Velocidad mínima (más rápido)
    private static final float MAX_SPEED = 0.08f;         // Velocidad máxima (más rápido)
    private static final float MIN_SIZE = 3.0f;           // Tamaño mínimo (más grande)
    private static final float MAX_SIZE = 7.0f;           // Tamaño máximo (más grande)
    private static final float TRAIL_LENGTH = 0.15f;      // Largo de la estela (más larga)

    // Dirección de viaje (hacia la izquierda = viajamos a la derecha)
    private static final float TRAVEL_DIR_X = -1.0f;
    private static final float TRAVEL_DIR_Y = -0.1f;      // Ligera inclinación

    // ════════════════════════════════════════════════════════════════════════
    // DATOS DE PARTÍCULAS
    // ════════════════════════════════════════════════════════════════════════
    private float[] posX;           // Posición X
    private float[] posY;           // Posición Y
    private float[] speeds;         // Velocidad individual
    private float[] sizes;          // Tamaño individual
    private float[] alphas;         // Transparencia
    private float[] colorR;         // Color R
    private float[] colorG;         // Color G
    private float[] colorB;         // Color B
    private boolean[] active;       // ¿Está activa?

    private float spawnTimer = 0f;
    private float time = 0f;
    private java.util.Random random = new java.util.Random();

    // OpenGL
    private int programId;
    private int aPositionLoc;
    private int aColorLoc;
    private int uPointSizeLoc;

    // Buffers (para dibujar líneas de estela + punto)
    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;

    public SpaceDust(Context context) {
        Log.d(TAG, "✨ Creando sistema de polvo espacial...");

        initParticles();
        initShader();
        initBuffers();

        Log.d(TAG, "✨ Sistema de polvo espacial creado (" + MAX_PARTICLES + " partículas)");
    }

    private void initParticles() {
        posX = new float[MAX_PARTICLES];
        posY = new float[MAX_PARTICLES];
        speeds = new float[MAX_PARTICLES];
        sizes = new float[MAX_PARTICLES];
        alphas = new float[MAX_PARTICLES];
        colorR = new float[MAX_PARTICLES];
        colorG = new float[MAX_PARTICLES];
        colorB = new float[MAX_PARTICLES];
        active = new boolean[MAX_PARTICLES];

        // Todas inactivas al inicio
        for (int i = 0; i < MAX_PARTICLES; i++) {
            active[i] = false;
        }
    }

    private void initShader() {
        // Vertex shader para líneas (estelas)
        String vertexShader =
            "attribute vec2 a_Position;\n" +
            "attribute vec4 a_Color;\n" +
            "varying vec4 v_Color;\n" +
            "void main() {\n" +
            "    v_Color = a_Color;\n" +
            "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
            "    gl_PointSize = 4.0;\n" +
            "}\n";

        // Fragment shader con gradiente suave
        String fragmentShader =
            "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
            "varying vec4 v_Color;\n" +
            "void main() {\n" +
            "    gl_FragColor = v_Color;\n" +
            "}\n";

        programId = ShaderUtils.createProgram(vertexShader, fragmentShader);

        if (programId != 0) {
            aPositionLoc = GLES20.glGetAttribLocation(programId, "a_Position");
            aColorLoc = GLES20.glGetAttribLocation(programId, "a_Color");
            Log.d(TAG, "✓ Shader compilado");
        } else {
            Log.e(TAG, "✗ Error compilando shader");
        }
    }

    private void initBuffers() {
        // Buffer para posiciones (2 vértices por partícula: inicio y fin de estela)
        ByteBuffer bb = ByteBuffer.allocateDirect(MAX_PARTICLES * 2 * 2 * 4);  // 2 vértices * 2 coords * 4 bytes
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();

        // Buffer para colores (RGBA por vértice)
        ByteBuffer cb = ByteBuffer.allocateDirect(MAX_PARTICLES * 2 * 4 * 4);  // 2 vértices * 4 componentes * 4 bytes
        cb.order(ByteOrder.nativeOrder());
        colorBuffer = cb.asFloatBuffer();
    }

    /**
     * Activa una partícula nueva
     */
    private void spawnParticle() {
        // Buscar partícula inactiva
        for (int i = 0; i < MAX_PARTICLES; i++) {
            if (!active[i]) {
                // Posición: aparece por la derecha
                posX[i] = 1.3f + random.nextFloat() * 0.2f;
                posY[i] = -0.9f + random.nextFloat() * 1.8f;

                // Velocidad variable
                speeds[i] = MIN_SPEED + random.nextFloat() * (MAX_SPEED - MIN_SPEED);

                // Tamaño
                sizes[i] = MIN_SIZE + random.nextFloat() * (MAX_SIZE - MIN_SIZE);

                // Alpha (transparencia) - 🚀 MÁS VISIBLE
                alphas[i] = 0.5f + random.nextFloat() * 0.5f;

                // Color: mayoría blanco/azulado, algunos dorados
                float colorType = random.nextFloat();
                if (colorType < 0.5f) {
                    // Blanco azulado
                    colorR[i] = 0.8f + random.nextFloat() * 0.2f;
                    colorG[i] = 0.9f + random.nextFloat() * 0.1f;
                    colorB[i] = 1.0f;
                } else if (colorType < 0.8f) {
                    // Cyan claro
                    colorR[i] = 0.6f + random.nextFloat() * 0.2f;
                    colorG[i] = 0.9f + random.nextFloat() * 0.1f;
                    colorB[i] = 1.0f;
                } else {
                    // Dorado/amarillo (polvo cósmico)
                    colorR[i] = 1.0f;
                    colorG[i] = 0.85f + random.nextFloat() * 0.15f;
                    colorB[i] = 0.5f + random.nextFloat() * 0.2f;
                }

                active[i] = true;
                break;
            }
        }
    }

    @Override
    public void update(float deltaTime) {
        time += deltaTime;
        spawnTimer += deltaTime;

        // ═══════════════════════════════════════════════════════════════════
        // SPAWN DE NUEVAS PARTÍCULAS
        // ═══════════════════════════════════════════════════════════════════
        float spawnInterval = 1.0f / SPAWN_RATE;
        // Variación aleatoria en el spawn
        if (spawnTimer > spawnInterval * (0.5f + random.nextFloat())) {
            spawnParticle();
            spawnTimer = 0f;
        }

        // ═══════════════════════════════════════════════════════════════════
        // ACTUALIZAR PARTÍCULAS ACTIVAS
        // ═══════════════════════════════════════════════════════════════════
        for (int i = 0; i < MAX_PARTICLES; i++) {
            if (!active[i]) continue;

            // Mover partícula
            posX[i] += TRAVEL_DIR_X * speeds[i];
            posY[i] += TRAVEL_DIR_Y * speeds[i];

            // Desactivar si sale de pantalla
            if (posX[i] < -1.4f || posY[i] < -1.2f || posY[i] > 1.2f) {
                active[i] = false;
            }
        }
    }

    @Override
    public void draw() {
        if (programId == 0) return;

        // Contar partículas activas
        int activeCount = 0;
        for (int i = 0; i < MAX_PARTICLES; i++) {
            if (active[i]) activeCount++;
        }
        if (activeCount == 0) return;

        GLES20.glUseProgram(programId);

        // Configurar blending aditivo para brillo
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);

        // Llenar buffers con datos de partículas activas
        vertexBuffer.position(0);
        colorBuffer.position(0);

        int drawCount = 0;
        for (int i = 0; i < MAX_PARTICLES; i++) {
            if (!active[i]) continue;

            // Calcular estela (línea desde posición actual hacia "atrás")
            float trailLen = TRAIL_LENGTH * (speeds[i] / MAX_SPEED);  // Más rápido = estela más larga
            float tailX = posX[i] - TRAVEL_DIR_X * trailLen;
            float tailY = posY[i] - TRAVEL_DIR_Y * trailLen;

            // Vértice 1: Cabeza (brillante)
            vertexBuffer.put(posX[i]);
            vertexBuffer.put(posY[i]);
            colorBuffer.put(colorR[i]);
            colorBuffer.put(colorG[i]);
            colorBuffer.put(colorB[i]);
            colorBuffer.put(alphas[i]);

            // Vértice 2: Cola (transparente)
            vertexBuffer.put(tailX);
            vertexBuffer.put(tailY);
            colorBuffer.put(colorR[i]);
            colorBuffer.put(colorG[i]);
            colorBuffer.put(colorB[i]);
            colorBuffer.put(0.0f);  // Alpha = 0 en la cola

            drawCount++;
        }

        vertexBuffer.position(0);
        colorBuffer.position(0);

        // Configurar atributos
        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(aColorLoc);
        GLES20.glVertexAttribPointer(aColorLoc, 4, GLES20.GL_FLOAT, false, 0, colorBuffer);

        // Dibujar líneas
        GLES20.glLineWidth(2.0f);
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, drawCount * 2);

        // Limpiar
        GLES20.glDisableVertexAttribArray(aPositionLoc);
        GLES20.glDisableVertexAttribArray(aColorLoc);

        // Restaurar estados
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }
}
