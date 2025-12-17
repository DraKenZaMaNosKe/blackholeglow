package com.secret.blackholeglow.christmas;

import android.content.Context;
import android.opengl.GLES30;
import android.util.Log;

import com.secret.blackholeglow.CameraAware;
import com.secret.blackholeglow.CameraController;
import com.secret.blackholeglow.SceneObject;
import com.secret.blackholeglow.scenes.Disposable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                                                                           ║
 * ║   ❄️ SnowParticles - Sistema de Partículas de Nieve GPU ❄️               ║
 * ║                                                                           ║
 * ║   Sistema de partículas optimizado para OpenGL ES 3.0 que simula         ║
 * ║   nieve cayendo con física realista de viento y turbulencia.             ║
 * ║                                                                           ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                           ║
 * ║   CARACTERÍSTICAS:                                                        ║
 * ║   • Renderizado GPU con GL_POINTS                                        ║
 * ║   • Física de viento con ruido Perlin simplificado                       ║
 * ║   • Copos de diferentes tamaños y velocidades                            ║
 * ║   • Efecto de ráfaga al tocar la pantalla                                ║
 * ║   • Reciclaje automático de partículas                                   ║
 * ║                                                                           ║
 * ║   SHADER FEATURES:                                                        ║
 * ║   • Tamaño variable por partícula (gl_PointSize)                         ║
 * ║   • Alpha fadeout en bordes                                              ║
 * ║   • Movimiento ondulante horizontal                                       ║
 * ║   • Rotación sutil de copos                                              ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 */
public class SnowParticles implements SceneObject, CameraAware, Disposable {

    private static final String TAG = "SnowParticles";

    // ═══════════════════════════════════════════════════════════════
    // 🎮 CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════
    private final int particleCount;
    private final float baseSpeed;
    private final float windStrength;

    // Límites del área de nieve (NDC coordinates)
    private static final float AREA_WIDTH = 3.0f;   // Ancho del área
    private static final float AREA_HEIGHT = 4.0f;  // Alto del área
    private static final float AREA_DEPTH = 2.0f;   // Profundidad

    // ═══════════════════════════════════════════════════════════════
    // 🎨 OPENGL RESOURCES
    // ═══════════════════════════════════════════════════════════════
    private int programId;
    private int vaoId;
    private int vboPositions;
    private int vboVelocities;
    private int vboSizes;

    // Uniform locations
    private int uMVPLocation;
    private int uTimeLocation;
    private int uWindLocation;
    private int uIntensityLocation;

    // Buffers
    private FloatBuffer positionBuffer;
    private FloatBuffer velocityBuffer;
    private FloatBuffer sizeBuffer;

    // Arrays de datos
    private float[] positions;      // x, y, z por partícula
    private float[] velocities;     // vx, vy, vz por partícula
    private float[] sizes;          // tamaño por partícula

    // ═══════════════════════════════════════════════════════════════
    // 🎬 ESTADO
    // ═══════════════════════════════════════════════════════════════
    private CameraController camera;
    private float time = 0f;
    private float intensity = 1.0f;         // Intensidad actual (1 = normal)
    private float intensifyTimer = 0f;      // Timer para efecto intenso
    private boolean isDisposed = false;
    private final Random random = new Random();

    // ═══════════════════════════════════════════════════════════════
    // 🔧 SHADERS
    // ═══════════════════════════════════════════════════════════════
    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "precision highp float;\n" +
        "\n" +
        "layout(location = 0) in vec3 a_Position;\n" +
        "layout(location = 1) in vec3 a_Velocity;\n" +
        "layout(location = 2) in float a_Size;\n" +
        "\n" +
        "uniform mat4 u_MVP;\n" +
        "uniform float u_Time;\n" +
        "uniform vec2 u_Wind;\n" +
        "uniform float u_Intensity;\n" +
        "\n" +
        "out float v_Alpha;\n" +
        "out float v_Rotation;\n" +
        "\n" +
        "// Simplex noise aproximado para turbulencia\n" +
        "float noise(vec2 p) {\n" +
        "    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);\n" +
        "}\n" +
        "\n" +
        "void main() {\n" +
        "    // Posición base\n" +
        "    vec3 pos = a_Position;\n" +
        "    \n" +
        "    // Movimiento ondulante horizontal (viento)\n" +
        "    float windOffset = sin(u_Time * 0.5 + pos.y * 2.0) * u_Wind.x;\n" +
        "    windOffset += cos(u_Time * 0.3 + pos.x * 1.5) * u_Wind.y * 0.5;\n" +
        "    \n" +
        "    // Turbulencia basada en ruido\n" +
        "    float turb = noise(vec2(pos.x + u_Time * 0.1, pos.y)) * 0.1;\n" +
        "    \n" +
        "    pos.x += windOffset + turb;\n" +
        "    \n" +
        "    // Transformar posición\n" +
        "    gl_Position = u_MVP * vec4(pos, 1.0);\n" +
        "    \n" +
        "    // Tamaño del punto (varía con profundidad y tamaño base)\n" +
        "    float depthFactor = 1.0 - (pos.z + 1.0) * 0.3;\n" +
        "    gl_PointSize = a_Size * depthFactor * u_Intensity * 20.0;\n" +
        "    \n" +
        "    // Alpha basado en profundidad (más lejos = más transparente)\n" +
        "    v_Alpha = depthFactor * 0.8;\n" +
        "    \n" +
        "    // Rotación para variedad visual\n" +
        "    v_Rotation = u_Time + pos.x * 10.0;\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "\n" +
        "in float v_Alpha;\n" +
        "in float v_Rotation;\n" +
        "\n" +
        "out vec4 fragColor;\n" +
        "\n" +
        "void main() {\n" +
        "    // Coordenadas del punto (0,0 = centro, 1,1 = esquina)\n" +
        "    vec2 coord = gl_PointCoord - vec2(0.5);\n" +
        "    \n" +
        "    // Rotar coordenadas para efecto de giro\n" +
        "    float c = cos(v_Rotation * 0.1);\n" +
        "    float s = sin(v_Rotation * 0.1);\n" +
        "    coord = vec2(coord.x * c - coord.y * s, coord.x * s + coord.y * c);\n" +
        "    \n" +
        "    // Distancia al centro para forma circular suave\n" +
        "    float dist = length(coord) * 2.0;\n" +
        "    \n" +
        "    // Forma de copo (estrella de 6 puntas simplificada)\n" +
        "    float angle = atan(coord.y, coord.x);\n" +
        "    float star = 0.3 + 0.1 * cos(angle * 6.0);\n" +
        "    \n" +
        "    // Alpha con fadeout suave en bordes\n" +
        "    float alpha = smoothstep(0.5, 0.1, dist) * v_Alpha;\n" +
        "    \n" +
        "    // Color blanco con tinte azulado\n" +
        "    vec3 snowColor = vec3(0.95, 0.97, 1.0);\n" +
        "    \n" +
        "    // Brillo en el centro\n" +
        "    float glow = exp(-dist * 3.0) * 0.5;\n" +
        "    snowColor += vec3(glow);\n" +
        "    \n" +
        "    fragColor = vec4(snowColor, alpha);\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════
    // 🏗️ CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════

    public SnowParticles(Context context, int particleCount, float baseSpeed, float windStrength) {
        this.particleCount = particleCount;
        this.baseSpeed = baseSpeed;
        this.windStrength = windStrength;

        Log.d(TAG, "❄️ Creando sistema de nieve con " + particleCount + " partículas");

        initArrays();
        initParticles();
        initGL();
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔧 INICIALIZACIÓN
    // ═══════════════════════════════════════════════════════════════

    private void initArrays() {
        positions = new float[particleCount * 3];
        velocities = new float[particleCount * 3];
        sizes = new float[particleCount];

        positionBuffer = createFloatBuffer(particleCount * 3);
        velocityBuffer = createFloatBuffer(particleCount * 3);
        sizeBuffer = createFloatBuffer(particleCount);
    }

    private void initParticles() {
        for (int i = 0; i < particleCount; i++) {
            resetParticle(i, true);
        }
    }

    private void resetParticle(int index, boolean randomY) {
        int i3 = index * 3;

        // Posición inicial aleatoria
        positions[i3] = (random.nextFloat() - 0.5f) * AREA_WIDTH;      // X
        positions[i3 + 1] = randomY ?
            (random.nextFloat() - 0.5f) * AREA_HEIGHT :                // Y aleatorio
            AREA_HEIGHT * 0.5f + random.nextFloat() * 0.5f;            // Y arriba
        positions[i3 + 2] = (random.nextFloat() - 0.5f) * AREA_DEPTH;  // Z

        // Velocidad (principalmente hacia abajo)
        velocities[i3] = (random.nextFloat() - 0.5f) * 0.1f;           // VX (deriva)
        velocities[i3 + 1] = -baseSpeed * (0.5f + random.nextFloat()); // VY (caída)
        velocities[i3 + 2] = (random.nextFloat() - 0.5f) * 0.05f;      // VZ

        // Tamaño aleatorio (variedad visual)
        sizes[index] = 0.3f + random.nextFloat() * 0.7f;
    }

    private void initGL() {
        // Compilar shaders
        programId = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        if (programId == 0) {
            Log.e(TAG, "Error creando programa de shaders");
            return;
        }

        // Obtener ubicaciones de uniforms
        uMVPLocation = GLES30.glGetUniformLocation(programId, "u_MVP");
        uTimeLocation = GLES30.glGetUniformLocation(programId, "u_Time");
        uWindLocation = GLES30.glGetUniformLocation(programId, "u_Wind");
        uIntensityLocation = GLES30.glGetUniformLocation(programId, "u_Intensity");

        // Crear VAO
        int[] vaos = new int[1];
        GLES30.glGenVertexArrays(1, vaos, 0);
        vaoId = vaos[0];
        GLES30.glBindVertexArray(vaoId);

        // Crear VBOs
        int[] vbos = new int[3];
        GLES30.glGenBuffers(3, vbos, 0);
        vboPositions = vbos[0];
        vboVelocities = vbos[1];
        vboSizes = vbos[2];

        // Configurar VBO de posiciones
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboPositions);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, particleCount * 3 * 4,
            null, GLES30.GL_DYNAMIC_DRAW);
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, 0);
        GLES30.glEnableVertexAttribArray(0);

        // Configurar VBO de velocidades
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboVelocities);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, particleCount * 3 * 4,
            null, GLES30.GL_DYNAMIC_DRAW);
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 0, 0);
        GLES30.glEnableVertexAttribArray(1);

        // Configurar VBO de tamaños
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboSizes);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, particleCount * 4,
            null, GLES30.GL_STATIC_DRAW);
        GLES30.glVertexAttribPointer(2, 1, GLES30.GL_FLOAT, false, 0, 0);
        GLES30.glEnableVertexAttribArray(2);

        // Subir datos iniciales de tamaños
        sizeBuffer.put(sizes).position(0);
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, particleCount * 4, sizeBuffer);

        GLES30.glBindVertexArray(0);

        Log.d(TAG, "✓ OpenGL inicializado para nieve");
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔄 UPDATE
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void update(float deltaTime) {
        if (isDisposed) return;

        time += deltaTime;

        // Actualizar timer de intensidad
        if (intensifyTimer > 0) {
            intensifyTimer -= deltaTime;
            if (intensifyTimer <= 0) {
                intensity = 1.0f;
            }
        }

        // Actualizar física de cada partícula
        for (int i = 0; i < particleCount; i++) {
            int i3 = i * 3;

            // Aplicar velocidad
            positions[i3] += velocities[i3] * deltaTime;
            positions[i3 + 1] += velocities[i3 + 1] * deltaTime * intensity;
            positions[i3 + 2] += velocities[i3 + 2] * deltaTime;

            // Reciclar partículas que salen del área
            if (positions[i3 + 1] < -AREA_HEIGHT * 0.5f) {
                resetParticle(i, false);
            }

            // Wrap horizontal
            if (positions[i3] > AREA_WIDTH * 0.5f) {
                positions[i3] = -AREA_WIDTH * 0.5f;
            } else if (positions[i3] < -AREA_WIDTH * 0.5f) {
                positions[i3] = AREA_WIDTH * 0.5f;
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 🎨 DRAW
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void draw() {
        if (isDisposed || programId == 0) return;

        // Habilitar blending para transparencia
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // Deshabilitar depth write (partículas no deben ocluirse entre sí)
        GLES30.glDepthMask(false);

        GLES30.glUseProgram(programId);
        GLES30.glBindVertexArray(vaoId);

        // Actualizar buffer de posiciones
        positionBuffer.put(positions).position(0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboPositions);
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, particleCount * 3 * 4, positionBuffer);

        // Actualizar buffer de velocidades
        velocityBuffer.put(velocities).position(0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboVelocities);
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, particleCount * 3 * 4, velocityBuffer);

        // Configurar uniforms
        if (camera != null) {
            GLES30.glUniformMatrix4fv(uMVPLocation, 1, false,
                camera.getViewProjectionMatrix(), 0);
        }
        GLES30.glUniform1f(uTimeLocation, time);
        GLES30.glUniform2f(uWindLocation, windStrength, windStrength * 0.5f);
        GLES30.glUniform1f(uIntensityLocation, intensity);

        // Dibujar partículas como puntos
        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, particleCount);

        // Restaurar estado
        GLES30.glBindVertexArray(0);
        GLES30.glDepthMask(true);
    }

    // ═══════════════════════════════════════════════════════════════
    // 🎮 INTERACTIVIDAD
    // ═══════════════════════════════════════════════════════════════

    /**
     * Crea una ráfaga de nieve en una posición específica
     */
    public void createBurst(float normalizedX, float normalizedY) {
        // Convertir coordenadas normalizadas a espacio de escena
        float worldX = normalizedX * AREA_WIDTH * 0.5f;
        float worldY = normalizedY * AREA_HEIGHT * 0.5f;

        // Afectar partículas cercanas
        int burstCount = Math.min(50, particleCount / 4);
        for (int i = 0; i < burstCount; i++) {
            int idx = random.nextInt(particleCount);
            int i3 = idx * 3;

            // Mover partícula cerca del punto de toque
            positions[i3] = worldX + (random.nextFloat() - 0.5f) * 0.5f;
            positions[i3 + 1] = worldY + (random.nextFloat() - 0.5f) * 0.5f;

            // Dar velocidad explosiva
            velocities[i3] = (random.nextFloat() - 0.5f) * 2.0f;
            velocities[i3 + 1] = random.nextFloat() * 1.5f;  // Hacia arriba
        }
    }

    /**
     * Intensifica la nieve temporalmente
     */
    public void intensify(float duration) {
        intensity = 2.0f;
        intensifyTimer = duration;
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔧 UTILIDADES
    // ═══════════════════════════════════════════════════════════════

    private FloatBuffer createFloatBuffer(int size) {
        return ByteBuffer
            .allocateDirect(size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource);

        if (vertexShader == 0 || fragmentShader == 0) {
            return 0;
        }

        int program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vertexShader);
        GLES30.glAttachShader(program, fragmentShader);
        GLES30.glLinkProgram(program);

        int[] linked = new int[1];
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            Log.e(TAG, "Error linking program: " + GLES30.glGetProgramInfoLog(program));
            GLES30.glDeleteProgram(program);
            return 0;
        }

        // Limpiar shaders (ya están enlazados al programa)
        GLES30.glDeleteShader(vertexShader);
        GLES30.glDeleteShader(fragmentShader);

        return program;
    }

    private int loadShader(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            String typeStr = (type == GLES30.GL_VERTEX_SHADER) ? "vertex" : "fragment";
            Log.e(TAG, "Error compiling " + typeStr + " shader: " +
                GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return 0;
        }

        return shader;
    }

    // ═══════════════════════════════════════════════════════════════
    // 📷 CAMERA
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    // ═══════════════════════════════════════════════════════════════
    // 🗑️ DISPOSE
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void dispose() {
        if (isDisposed) return;
        isDisposed = true;

        Log.d(TAG, "🗑️ Liberando recursos de SnowParticles...");

        if (programId != 0) {
            GLES30.glDeleteProgram(programId);
            programId = 0;
        }

        if (vaoId != 0) {
            GLES30.glDeleteVertexArrays(1, new int[]{vaoId}, 0);
            vaoId = 0;
        }

        int[] vbos = {vboPositions, vboVelocities, vboSizes};
        GLES30.glDeleteBuffers(3, vbos, 0);

        Log.d(TAG, "✓ SnowParticles liberado");
    }

    @Override
    public boolean isDisposed() {
        return isDisposed;
    }
}
