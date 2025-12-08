package com.secret.blackholeglow;

import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════╗
 * ║   🚀 SpeedLines - Efecto de Velocidad/Viaje Espacial                         ║
 * ╠═══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                               ║
 * ║   Líneas que emanan del centro hacia afuera, creando sensación de:           ║
 * ║   - Viaje a alta velocidad                                                   ║
 * ║   - Salto al hiperespacio (estilo Star Wars)                                 ║
 * ║   - Movimiento constante a través del espacio                                ║
 * ║                                                                               ║
 * ║   Las líneas:                                                                 ║
 * ║   - Nacen cerca del centro                                                   ║
 * ║   - Se estiran conforme se alejan                                            ║
 * ║   - Se desvanecen al llegar a los bordes                                     ║
 * ║                                                                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════════╝
 */
public class SpeedLines implements SceneObject {
    private static final String TAG = "SpeedLines";

    // Configuración
    private static final int MAX_LINES = 60;           // Número máximo de líneas
    private static final float SPAWN_RATE = 0.03f;     // Segundos entre spawns
    private static final float MIN_SPEED = 0.8f;       // Velocidad mínima
    private static final float MAX_SPEED = 2.5f;       // Velocidad máxima
    private static final float MIN_LENGTH = 0.02f;     // Longitud mínima inicial
    private static final float MAX_LENGTH = 0.15f;     // Longitud máxima (cuando se estira)
    private static final float STRETCH_FACTOR = 3.0f;  // Factor de estiramiento

    // Estructura de cada línea
    private static class SpeedLine {
        float x, y;           // Posición actual (normalizada -1 a 1)
        float angle;          // Ángulo de dirección (radianes)
        float speed;          // Velocidad
        float length;         // Longitud actual
        float baseLength;     // Longitud base
        float alpha;          // Transparencia
        float distFromCenter; // Distancia desde el centro
        boolean active;       // Si está activa

        // Color (variación sutil)
        float r, g, b;
    }

    private SpeedLine[] lines;
    private float spawnTimer = 0f;
    private Random random;

    // OpenGL
    private int shaderProgram = 0;
    private int aPositionLoc = -1;
    private int uColorLoc = -1;
    private FloatBuffer vertexBuffer;

    // Estado
    private float intensity = 1.0f;  // Intensidad del efecto (0-1)
    private boolean enabled = true;

    // Shaders
    private static final String VERTEX_SHADER =
        "attribute vec2 aPosition;\n" +
        "void main() {\n" +
        "    gl_Position = vec4(aPosition, 0.0, 1.0);\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "uniform vec4 uColor;\n" +
        "void main() {\n" +
        "    gl_FragColor = uColor;\n" +
        "}\n";

    public SpeedLines() {
        Log.d(TAG, "🚀 Creando SpeedLines...");
        random = new Random();
        lines = new SpeedLine[MAX_LINES];
        for (int i = 0; i < MAX_LINES; i++) {
            lines[i] = new SpeedLine();
            lines[i].active = false;
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

        // Buffer para 2 vértices por línea (start + end)
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * 4);  // 2 vertices * 2 floats * 4 bytes
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();

        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);

        Log.d(TAG, "✓ SpeedLines OpenGL inicializado");
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
     * Spawns una nueva línea de velocidad
     */
    private void spawnLine() {
        // Buscar línea inactiva
        for (SpeedLine line : lines) {
            if (!line.active) {
                // Posición inicial cerca del centro (con pequeña variación)
                float spawnRadius = 0.05f + random.nextFloat() * 0.1f;
                float angle = random.nextFloat() * (float)(Math.PI * 2);

                line.x = (float)Math.cos(angle) * spawnRadius;
                line.y = (float)Math.sin(angle) * spawnRadius;
                line.angle = angle;  // Sale en la misma dirección radial

                // Propiedades aleatorias
                line.speed = MIN_SPEED + random.nextFloat() * (MAX_SPEED - MIN_SPEED);
                line.baseLength = MIN_LENGTH + random.nextFloat() * 0.03f;
                line.length = line.baseLength;
                line.alpha = 0.3f + random.nextFloat() * 0.4f;  // 0.3 - 0.7
                line.distFromCenter = spawnRadius;

                // Color: mayormente blanco/cyan con variación sutil
                float colorType = random.nextFloat();
                if (colorType < 0.6f) {
                    // Blanco
                    line.r = 0.9f + random.nextFloat() * 0.1f;
                    line.g = 0.9f + random.nextFloat() * 0.1f;
                    line.b = 1.0f;
                } else if (colorType < 0.85f) {
                    // Cyan
                    line.r = 0.4f + random.nextFloat() * 0.2f;
                    line.g = 0.8f + random.nextFloat() * 0.2f;
                    line.b = 1.0f;
                } else {
                    // Amarillo/dorado (raro)
                    line.r = 1.0f;
                    line.g = 0.8f + random.nextFloat() * 0.2f;
                    line.b = 0.3f + random.nextFloat() * 0.3f;
                }

                line.active = true;
                break;
            }
        }
    }

    @Override
    public void update(float deltaTime) {
        if (!enabled) return;

        // Spawn nuevas líneas
        spawnTimer += deltaTime;
        while (spawnTimer >= SPAWN_RATE) {
            spawnTimer -= SPAWN_RATE;
            spawnLine();
            // Spawnar múltiples para más densidad
            if (random.nextFloat() < 0.5f) spawnLine();
        }

        // Actualizar líneas existentes
        for (SpeedLine line : lines) {
            if (!line.active) continue;

            // Mover en dirección radial (hacia afuera)
            float dx = (float)Math.cos(line.angle) * line.speed * deltaTime;
            float dy = (float)Math.sin(line.angle) * line.speed * deltaTime;
            line.x += dx;
            line.y += dy;

            // Calcular distancia desde el centro
            line.distFromCenter = (float)Math.sqrt(line.x * line.x + line.y * line.y);

            // Estirar línea conforme se aleja (efecto warp)
            float stretchProgress = Math.min(1f, line.distFromCenter / 1.5f);
            line.length = line.baseLength + (MAX_LENGTH - line.baseLength) * stretchProgress * STRETCH_FACTOR;
            line.length = Math.min(line.length, MAX_LENGTH);

            // Fade out cerca de los bordes
            if (line.distFromCenter > 0.8f) {
                float fadeProgress = (line.distFromCenter - 0.8f) / 0.7f;
                line.alpha = Math.max(0f, line.alpha - deltaTime * 2f);
            }

            // Desactivar si sale de pantalla o alpha muy bajo
            if (line.distFromCenter > 1.8f || line.alpha <= 0.01f) {
                line.active = false;
            }
        }
    }

    @Override
    public void draw() {
        if (!enabled || shaderProgram == 0) return;

        GLES30.glUseProgram(shaderProgram);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // Línea más gruesa para mejor visibilidad
        GLES30.glLineWidth(2.0f);

        GLES30.glEnableVertexAttribArray(aPositionLoc);

        for (SpeedLine line : lines) {
            if (!line.active) continue;

            // Calcular puntos de la línea
            // La línea va desde (x,y) hacia atrás en la dirección opuesta al movimiento
            float endX = line.x;
            float endY = line.y;
            float startX = line.x - (float)Math.cos(line.angle) * line.length;
            float startY = line.y - (float)Math.sin(line.angle) * line.length;

            // Llenar buffer
            float[] vertices = {startX, startY, endX, endY};
            vertexBuffer.clear();
            vertexBuffer.put(vertices);
            vertexBuffer.position(0);

            // Color con alpha
            float finalAlpha = line.alpha * intensity;
            GLES30.glUniform4f(uColorLoc, line.r, line.g, line.b, finalAlpha);

            // Dibujar
            GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);
            GLES30.glDrawArrays(GLES30.GL_LINES, 0, 2);
        }

        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glLineWidth(1.0f);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // API Pública
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Establece la intensidad del efecto (0.0 - 1.0)
     */
    public void setIntensity(float intensity) {
        this.intensity = Math.max(0f, Math.min(1f, intensity));
    }

    /**
     * Habilita/deshabilita el efecto
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return true si el efecto está habilitado
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Libera recursos OpenGL
     */
    public void cleanup() {
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
    }
}
