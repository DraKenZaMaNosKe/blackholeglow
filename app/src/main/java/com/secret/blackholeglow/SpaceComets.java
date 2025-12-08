package com.secret.blackholeglow;

import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║   ☄️ SpaceComets - Cometas con Estela Brillante                              ║
 * ╠═══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                               ║
 * ║   Cometas que cruzan la pantalla con colas brillantes que se desvanecen:     ║
 * ║   - Núcleo brillante en la cabeza                                            ║
 * ║   - Estela que se desvanece gradualmente                                     ║
 * ║   - Efecto de partículas en la cola                                          ║
 * ║   - Cruzan en diagonal (sensación de viaje)                                  ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 */
public class SpaceComets implements SceneObject {
    private static final String TAG = "SpaceComets";

    // Configuración
    private static final int MAX_COMETS = 8;           // Pocos pero impactantes
    private static final float SPAWN_INTERVAL = 2.5f;  // Segundos entre cometas
    private static final int TRAIL_SEGMENTS = 20;      // Segmentos de la estela
    private static final float MIN_SPEED = 0.6f;
    private static final float MAX_SPEED = 1.2f;

    // Estructura de cada cometa
    private static class Comet {
        float x, y;              // Posición actual
        float vx, vy;            // Velocidad
        float speed;             // Magnitud de velocidad
        float size;              // Tamaño del núcleo
        float trailLength;       // Longitud de la estela
        float alpha;             // Transparencia
        boolean active;

        // Color del cometa
        float r, g, b;

        // Historial de posiciones para la estela
        float[] trailX = new float[TRAIL_SEGMENTS];
        float[] trailY = new float[TRAIL_SEGMENTS];
        int trailIndex = 0;
        boolean trailFull = false;
    }

    private Comet[] comets;
    private float spawnTimer = 0f;
    private Random random;

    // OpenGL
    private int shaderProgram = 0;
    private int aPositionLoc = -1;
    private int uColorLoc = -1;
    private FloatBuffer vertexBuffer;

    // Estado
    private boolean enabled = true;

    // Shaders GLSL ES 3.0
    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "precision highp float;\n" +
        "in vec2 aPosition;\n" +
        "uniform float uPointSize;\n" +
        "void main() {\n" +
        "    gl_Position = vec4(aPosition, 0.0, 1.0);\n" +
        "    gl_PointSize = uPointSize;\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "uniform vec4 uColor;\n" +
        "out vec4 fragColor;\n" +
        "void main() {\n" +
        "    vec2 coord = gl_PointCoord - vec2(0.5);\n" +
        "    float dist = length(coord);\n" +
        "    if (dist > 0.5) discard;\n" +
        "    float alpha = 1.0 - smoothstep(0.2, 0.5, dist);\n" +
        "    fragColor = vec4(uColor.rgb * uColor.a * alpha, uColor.a * alpha);\n" +
        "}\n";

    private int uPointSizeLoc = -1;

    public SpaceComets() {
        Log.d(TAG, "☄️ Creando SpaceComets...");
        random = new Random();
        comets = new Comet[MAX_COMETS];
        for (int i = 0; i < MAX_COMETS; i++) {
            comets[i] = new Comet();
            comets[i].active = false;
        }
        initOpenGL();
    }

    private void initOpenGL() {
        int vs = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        if (vs == 0 || fs == 0) return;

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vs);
        GLES30.glAttachShader(shaderProgram, fs);
        GLES30.glLinkProgram(shaderProgram);

        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "aPosition");
        uColorLoc = GLES30.glGetUniformLocation(shaderProgram, "uColor");
        uPointSizeLoc = GLES30.glGetUniformLocation(shaderProgram, "uPointSize");

        // Buffer para puntos
        ByteBuffer bb = ByteBuffer.allocateDirect(2 * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();

        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);

        Log.d(TAG, "✓ SpaceComets OpenGL inicializado");
    }

    private int compileShader(int type, String code) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, code);
        GLES30.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Error shader: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    /**
     * Spawns un nuevo cometa
     */
    private void spawnComet() {
        for (Comet comet : comets) {
            if (!comet.active) {
                // Decidir desde qué borde aparece
                int edge = random.nextInt(3);  // 0=arriba, 1=derecha, 2=arriba-derecha

                switch (edge) {
                    case 0:  // Desde arriba
                        comet.x = -1.0f + random.nextFloat() * 2.0f;
                        comet.y = 1.3f;
                        comet.vx = -0.2f + random.nextFloat() * 0.4f;
                        comet.vy = -1.0f;
                        break;
                    case 1:  // Desde la derecha
                        comet.x = 1.3f;
                        comet.y = 0.5f + random.nextFloat() * 0.8f;
                        comet.vx = -1.0f;
                        comet.vy = -0.3f - random.nextFloat() * 0.4f;
                        break;
                    default:  // Diagonal desde arriba-derecha
                        comet.x = 0.5f + random.nextFloat() * 0.8f;
                        comet.y = 1.3f;
                        comet.vx = -0.6f - random.nextFloat() * 0.3f;
                        comet.vy = -0.8f - random.nextFloat() * 0.3f;
                        break;
                }

                // Normalizar velocidad
                float mag = (float)Math.sqrt(comet.vx * comet.vx + comet.vy * comet.vy);
                comet.speed = MIN_SPEED + random.nextFloat() * (MAX_SPEED - MIN_SPEED);
                comet.vx = (comet.vx / mag) * comet.speed;
                comet.vy = (comet.vy / mag) * comet.speed;

                // Propiedades visuales
                comet.size = 8.0f + random.nextFloat() * 12.0f;  // 8-20 píxeles
                comet.trailLength = 0.15f + random.nextFloat() * 0.2f;
                comet.alpha = 0.7f + random.nextFloat() * 0.3f;

                // Color del cometa
                float colorType = random.nextFloat();
                if (colorType < 0.4f) {
                    // Cyan/Azul hielo
                    comet.r = 0.6f + random.nextFloat() * 0.2f;
                    comet.g = 0.9f + random.nextFloat() * 0.1f;
                    comet.b = 1.0f;
                } else if (colorType < 0.7f) {
                    // Blanco brillante
                    comet.r = 1.0f;
                    comet.g = 1.0f;
                    comet.b = 1.0f;
                } else if (colorType < 0.9f) {
                    // Azul profundo
                    comet.r = 0.3f + random.nextFloat() * 0.2f;
                    comet.g = 0.5f + random.nextFloat() * 0.3f;
                    comet.b = 1.0f;
                } else {
                    // Dorado/naranja (raro, como cometa de fuego)
                    comet.r = 1.0f;
                    comet.g = 0.6f + random.nextFloat() * 0.3f;
                    comet.b = 0.2f + random.nextFloat() * 0.2f;
                }

                // Limpiar historial de trail
                comet.trailIndex = 0;
                comet.trailFull = false;
                for (int i = 0; i < TRAIL_SEGMENTS; i++) {
                    comet.trailX[i] = comet.x;
                    comet.trailY[i] = comet.y;
                }

                comet.active = true;
                Log.d(TAG, "☄️ Cometa spawneado en (" + comet.x + ", " + comet.y + ")");
                break;
            }
        }
    }

    @Override
    public void update(float deltaTime) {
        if (!enabled) return;

        // Spawn nuevos cometas
        spawnTimer += deltaTime;
        if (spawnTimer >= SPAWN_INTERVAL) {
            spawnTimer = 0f;
            spawnComet();
        }

        // Actualizar cometas
        for (Comet comet : comets) {
            if (!comet.active) continue;

            // Guardar posición actual en el trail
            comet.trailX[comet.trailIndex] = comet.x;
            comet.trailY[comet.trailIndex] = comet.y;
            comet.trailIndex = (comet.trailIndex + 1) % TRAIL_SEGMENTS;
            if (comet.trailIndex == 0) comet.trailFull = true;

            // Mover
            comet.x += comet.vx * deltaTime;
            comet.y += comet.vy * deltaTime;

            // Desactivar si sale de pantalla
            if (comet.x < -1.5f || comet.x > 1.5f || comet.y < -1.5f || comet.y > 1.5f) {
                comet.active = false;
            }
        }
    }

    @Override
    public void draw() {
        if (!enabled || shaderProgram == 0) return;

        GLES30.glUseProgram(shaderProgram);
        GLES30.glEnable(GLES30.GL_BLEND);
        // Additive blending con premultiplied alpha (GL_ONE porque ya multiplicamos en el shader)
        GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE);

        GLES30.glEnableVertexAttribArray(aPositionLoc);

        for (Comet comet : comets) {
            if (!comet.active) continue;

            // Dibujar la estela (de más viejo a más nuevo)
            int segments = comet.trailFull ? TRAIL_SEGMENTS : comet.trailIndex;
            for (int i = 0; i < segments; i++) {
                // Calcular índice real (desde el más viejo)
                int idx = comet.trailFull ?
                    (comet.trailIndex + i) % TRAIL_SEGMENTS : i;

                // Alpha y tamaño decrecen hacia la cola
                float progress = (float)i / segments;
                float trailAlpha = comet.alpha * progress * 0.6f;
                float trailSize = comet.size * progress * 0.7f;

                if (trailAlpha < 0.05f || trailSize < 1.0f) continue;

                // Posición del segmento
                float[] vertex = {comet.trailX[idx], comet.trailY[idx]};
                vertexBuffer.clear();
                vertexBuffer.put(vertex);
                vertexBuffer.position(0);

                // Color con fade
                GLES30.glUniform4f(uColorLoc, comet.r, comet.g, comet.b, trailAlpha);
                GLES30.glUniform1f(uPointSizeLoc, trailSize);

                GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);
                GLES30.glDrawArrays(GLES30.GL_POINTS, 0, 1);
            }

            // Dibujar el núcleo (cabeza del cometa) - más brillante
            float[] vertex = {comet.x, comet.y};
            vertexBuffer.clear();
            vertexBuffer.put(vertex);
            vertexBuffer.position(0);

            // Núcleo con glow
            // Primero un halo grande y tenue
            GLES30.glUniform4f(uColorLoc, comet.r, comet.g, comet.b, comet.alpha * 0.3f);
            GLES30.glUniform1f(uPointSizeLoc, comet.size * 2.5f);
            GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);
            GLES30.glDrawArrays(GLES30.GL_POINTS, 0, 1);

            // Luego el núcleo brillante
            GLES30.glUniform4f(uColorLoc, 1.0f, 1.0f, 1.0f, comet.alpha);
            GLES30.glUniform1f(uPointSizeLoc, comet.size);
            GLES30.glDrawArrays(GLES30.GL_POINTS, 0, 1);
        }

        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);  // Restaurar blend normal
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // API Pública
    // ═══════════════════════════════════════════════════════════════════════════

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void cleanup() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
    }
}
