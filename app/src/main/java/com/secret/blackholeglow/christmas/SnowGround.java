package com.secret.blackholeglow.christmas;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import com.secret.blackholeglow.CameraAware;
import com.secret.blackholeglow.CameraController;
import com.secret.blackholeglow.SceneObject;
import com.secret.blackholeglow.TimeManager;
import com.secret.blackholeglow.TextureManager;
import com.secret.blackholeglow.scenes.Disposable;
import com.secret.blackholeglow.scenes.SceneConstants;
import com.secret.blackholeglow.systems.ScreenManager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                                                                           ║
 * ║   🏔️ SnowGround - Suelo Nevado Procedural 🏔️                             ║
 * ║                                                                           ║
 * ║   "Un manto de nieve brillante que cubre el suelo del bosque"            ║
 * ║                                                                           ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                           ║
 * ║   CARACTERÍSTICAS:                                                        ║
 * ║   • Plano inclinado en la parte inferior de la pantalla                  ║
 * ║   • Textura procedural de nieve con brillos                              ║
 * ║   • Ondulaciones suaves simulando relieve                                ║
 * ║   • Reflejos de luz ambiental                                            ║
 * ║                                                                           ║
 * ║   SHADER EFFECTS:                                                         ║
 * ║   • Ruido Perlin para textura de nieve                                   ║
 * ║   • Sparkles/brillos aleatorios                                          ║
 * ║   • Gradiente de profundidad (fade hacia el fondo)                       ║
 * ║   • Ondulación sutil del terreno                                         ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 */
public class SnowGround implements SceneObject, CameraAware, Disposable {
    private static final String TAG = "SnowGround";

    // ═══════════════════════════════════════════════════════════════
    // 🎨 OPENGL RESOURCES
    // ═══════════════════════════════════════════════════════════════
    private int programId;
    private int vaoId;
    private int vboId;

    // Uniform locations
    private int uMVPLocation;
    private int uTimeLocation;
    private int uResolutionLocation;

    // ═══════════════════════════════════════════════════════════════
    // 🎬 ESTADO
    // ═══════════════════════════════════════════════════════════════
    private CameraController camera;
    private float time = 0f;
    private final float timeOffset;
    private boolean isDisposed = false;

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // ═══════════════════════════════════════════════════════════════
    // 🔧 SHADERS
    // ═══════════════════════════════════════════════════════════════
    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "precision highp float;\n" +
        "\n" +
        "layout(location = 0) in vec3 a_Position;\n" +
        "layout(location = 1) in vec2 a_TexCoord;\n" +
        "\n" +
        "uniform mat4 u_MVP;\n" +
        "uniform float u_Time;\n" +
        "\n" +
        "out vec2 v_TexCoord;\n" +
        "out float v_Depth;\n" +
        "\n" +
        "void main() {\n" +
        "    vec3 pos = a_Position;\n" +
        "    \n" +
        "    // Ondulación sutil del terreno\n" +
        "    float wave = sin(pos.x * 3.0 + u_Time * 0.5) * 0.02;\n" +
        "    wave += cos(pos.z * 2.0 + u_Time * 0.3) * 0.015;\n" +
        "    pos.y += wave;\n" +
        "    \n" +
        "    gl_Position = u_MVP * vec4(pos, 1.0);\n" +
        "    v_TexCoord = a_TexCoord;\n" +
        "    v_Depth = (pos.z + 2.0) / 4.0;  // Normalizar profundidad\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision highp float;\n" +
        "\n" +
        "in vec2 v_TexCoord;\n" +
        "in float v_Depth;\n" +
        "\n" +
        "uniform float u_Time;\n" +
        "uniform vec2 u_Resolution;\n" +
        "\n" +
        "out vec4 fragColor;\n" +
        "\n" +
        "// ═══ FUNCIONES DE RUIDO ═══\n" +
        "float hash(vec2 p) {\n" +
        "    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);\n" +
        "}\n" +
        "\n" +
        "float noise(vec2 p) {\n" +
        "    vec2 i = floor(p);\n" +
        "    vec2 f = fract(p);\n" +
        "    f = f * f * (3.0 - 2.0 * f);\n" +
        "    \n" +
        "    float a = hash(i);\n" +
        "    float b = hash(i + vec2(1.0, 0.0));\n" +
        "    float c = hash(i + vec2(0.0, 1.0));\n" +
        "    float d = hash(i + vec2(1.0, 1.0));\n" +
        "    \n" +
        "    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);\n" +
        "}\n" +
        "\n" +
        "// Ruido fractal (FBM)\n" +
        "float fbm(vec2 p) {\n" +
        "    float value = 0.0;\n" +
        "    float amplitude = 0.5;\n" +
        "    for (int i = 0; i < 4; i++) {\n" +
        "        value += amplitude * noise(p);\n" +
        "        p *= 2.0;\n" +
        "        amplitude *= 0.5;\n" +
        "    }\n" +
        "    return value;\n" +
        "}\n" +
        "\n" +
        "// Sparkles de nieve\n" +
        "float sparkle(vec2 uv, float time) {\n" +
        "    vec2 grid = floor(uv * 50.0);\n" +
        "    float h = hash(grid);\n" +
        "    \n" +
        "    // Solo algunos puntos brillan\n" +
        "    if (h > 0.97) {\n" +
        "        float sparkleTime = fract(time * 0.5 + h * 10.0);\n" +
        "        float intensity = sin(sparkleTime * 3.14159) * 2.0;\n" +
        "        return max(0.0, intensity);\n" +
        "    }\n" +
        "    return 0.0;\n" +
        "}\n" +
        "\n" +
        "void main() {\n" +
        "    vec2 uv = v_TexCoord;\n" +
        "    \n" +
        "    // ═══ TEXTURA DE NIEVE ═══\n" +
        "    // Base con ruido fractal\n" +
        "    float snowNoise = fbm(uv * 8.0) * 0.15 + 0.85;\n" +
        "    \n" +
        "    // Detalle fino\n" +
        "    float detail = noise(uv * 30.0) * 0.05;\n" +
        "    \n" +
        "    // Color base de nieve (blanco azulado)\n" +
        "    vec3 snowColor = vec3(0.92, 0.95, 1.0);\n" +
        "    snowColor *= snowNoise + detail;\n" +
        "    \n" +
        "    // ═══ SOMBRAS Y RELIEVE ═══\n" +
        "    float shadow = fbm(uv * 4.0 + vec2(u_Time * 0.02, 0.0));\n" +
        "    shadow = smoothstep(0.3, 0.7, shadow);\n" +
        "    snowColor *= mix(0.85, 1.0, shadow);\n" +
        "    \n" +
        "    // ═══ SPARKLES ═══\n" +
        "    float sparkles = sparkle(uv, u_Time);\n" +
        "    snowColor += vec3(1.0, 1.0, 0.95) * sparkles * 0.5;\n" +
        "    \n" +
        "    // ═══ GRADIENTE DE PROFUNDIDAD ═══\n" +
        "    // Fade hacia el fondo para integrarse con el background\n" +
        "    float depthFade = smoothstep(0.0, 0.3, 1.0 - v_Depth);\n" +
        "    \n" +
        "    // ═══ BORDES SUAVES ═══\n" +
        "    // Fade en los bordes laterales\n" +
        "    float edgeFade = smoothstep(0.0, 0.15, uv.x) * smoothstep(1.0, 0.85, uv.x);\n" +
        "    \n" +
        "    // ═══ TINTE AMBIENTAL ═══\n" +
        "    // Luz cálida del pueblo\n" +
        "    vec3 ambientLight = vec3(1.0, 0.95, 0.85) * 0.1;\n" +
        "    snowColor += ambientLight * (1.0 - v_Depth);\n" +
        "    \n" +
        "    // ═══ ALPHA FINAL ═══\n" +
        "    float alpha = depthFade * edgeFade;\n" +
        "    \n" +
        "    fragColor = vec4(snowColor, alpha * 0.9);\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════
    // 🏗️ CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════

    public SnowGround(Context context, TextureManager textureManager) {
        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║   🏔️ SNOW GROUND GL3.0 🏔️              ║");
        Log.d(TAG, "╚════════════════════════════════════════╝");

        this.timeOffset = TimeManager.getTime();

        initGL();
        initModelMatrix();

        Log.d(TAG, "✓ SnowGround inicializado");
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔧 INICIALIZACIÓN
    // ═══════════════════════════════════════════════════════════════

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
        uResolutionLocation = GLES30.glGetUniformLocation(programId, "u_Resolution");

        // ═══ CREAR VAO ═══
        int[] vaoArray = new int[1];
        GLES30.glGenVertexArrays(1, vaoArray, 0);
        vaoId = vaoArray[0];
        GLES30.glBindVertexArray(vaoId);

        // ═══ CREAR VBO CON GEOMETRÍA DEL SUELO ═══
        // Plano inclinado que cubre la parte inferior de la escena
        float[] vertexData = createGroundGeometry();

        int[] vboArray = new int[1];
        GLES30.glGenBuffers(1, vboArray, 0);
        vboId = vboArray[0];

        FloatBuffer buffer = createFloatBuffer(vertexData);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,
                vertexData.length * 4, buffer, GLES30.GL_STATIC_DRAW);

        // ═══ CONFIGURAR ATRIBUTOS ═══
        int stride = 5 * 4;  // 5 floats (pos3 + uv2) * 4 bytes

        // location 0: a_Position (vec3)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0);
        GLES30.glEnableVertexAttribArray(0);

        // location 1: a_TexCoord (vec2)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, stride, 3 * 4);
        GLES30.glEnableVertexAttribArray(1);

        // Unbind
        GLES30.glBindVertexArray(0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);

        Log.d(TAG, "✓ VAO=" + vaoId + ", VBO=" + vboId);
    }

    private float[] createGroundGeometry() {
        // Crear un plano subdividido para permitir ondulación en el vertex shader
        int subdivisions = 10;
        int vertexCount = (subdivisions + 1) * (subdivisions + 1) * 6; // 2 triángulos por quad
        float[] vertices = new float[vertexCount * 5]; // pos3 + uv2

        float width = 4.0f;   // Ancho del suelo
        float depth = 3.0f;   // Profundidad
        float yBase = -1.5f;  // Posición Y base
        float tilt = 0.3f;    // Inclinación (más alto atrás)

        int idx = 0;
        for (int z = 0; z < subdivisions; z++) {
            for (int x = 0; x < subdivisions; x++) {
                // Coordenadas normalizadas
                float x0 = (float) x / subdivisions;
                float x1 = (float) (x + 1) / subdivisions;
                float z0 = (float) z / subdivisions;
                float z1 = (float) (z + 1) / subdivisions;

                // Posiciones en mundo
                float px0 = (x0 - 0.5f) * width;
                float px1 = (x1 - 0.5f) * width;
                float pz0 = z0 * depth - depth * 0.5f;
                float pz1 = z1 * depth - depth * 0.5f;

                // Y con inclinación
                float py0 = yBase + z0 * tilt;
                float py1 = yBase + z1 * tilt;

                // Triángulo 1
                vertices[idx++] = px0; vertices[idx++] = py0; vertices[idx++] = pz0;
                vertices[idx++] = x0; vertices[idx++] = z0;

                vertices[idx++] = px1; vertices[idx++] = py0; vertices[idx++] = pz0;
                vertices[idx++] = x1; vertices[idx++] = z0;

                vertices[idx++] = px0; vertices[idx++] = py1; vertices[idx++] = pz1;
                vertices[idx++] = x0; vertices[idx++] = z1;

                // Triángulo 2
                vertices[idx++] = px1; vertices[idx++] = py0; vertices[idx++] = pz0;
                vertices[idx++] = x1; vertices[idx++] = z0;

                vertices[idx++] = px1; vertices[idx++] = py1; vertices[idx++] = pz1;
                vertices[idx++] = x1; vertices[idx++] = z1;

                vertices[idx++] = px0; vertices[idx++] = py1; vertices[idx++] = pz1;
                vertices[idx++] = x0; vertices[idx++] = z1;
            }
        }

        return vertices;
    }

    private void initModelMatrix() {
        Matrix.setIdentityM(modelMatrix, 0);
        // Posicionar el suelo (centrado, abajo, hacia atrás)
        Matrix.translateM(modelMatrix, 0,
            0.0f,  // X centrado
            SceneConstants.Christmas.GROUND_POSITION_Y,
            -2.0f);  // Z hacia atrás
    }

    private FloatBuffer createFloatBuffer(float[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(data);
        fb.position(0);
        return fb;
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔄 UPDATE
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void update(float deltaTime) {
        if (isDisposed) return;
        time = TimeManager.getTime() - timeOffset;
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

        GLES30.glUseProgram(programId);
        GLES30.glBindVertexArray(vaoId);

        // Calcular MVP
        if (camera != null) {
            Matrix.multiplyMM(mvpMatrix, 0, camera.getViewProjectionMatrix(), 0, modelMatrix, 0);
            GLES30.glUniformMatrix4fv(uMVPLocation, 1, false, mvpMatrix, 0);
        }

        GLES30.glUniform1f(uTimeLocation, time);
        GLES30.glUniform2f(uResolutionLocation, ScreenManager.getWidth(), ScreenManager.getHeight());

        // Dibujar
        int vertexCount = 10 * 10 * 6; // subdivisions^2 * 6 vertices per quad
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, vertexCount);

        GLES30.glBindVertexArray(0);
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔧 SHADER UTILITIES
    // ═══════════════════════════════════════════════════════════════

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

        Log.d(TAG, "🗑️ Liberando recursos de SnowGround...");

        if (programId != 0) {
            GLES30.glDeleteProgram(programId);
            programId = 0;
        }

        if (vaoId != 0) {
            GLES30.glDeleteVertexArrays(1, new int[]{vaoId}, 0);
            vaoId = 0;
        }

        if (vboId != 0) {
            GLES30.glDeleteBuffers(1, new int[]{vboId}, 0);
            vboId = 0;
        }

        Log.d(TAG, "✓ SnowGround liberado");
    }

    @Override
    public boolean isDisposed() {
        return isDisposed;
    }
}
