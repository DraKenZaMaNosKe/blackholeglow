package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ☀️🔥 EFECTO DE CALOR/PLASMA ALREDEDOR DEL SOL
 *
 * Simula la distorsión del aire caliente alrededor del sol
 * Similar al efecto de "heat shimmer" o "vapor de agua"
 * cuando hay fuego o superficies muy calientes
 *
 * Se dibuja como un halo semi-transparente con ondulaciones
 */
public class SunHeatEffect implements SceneObject, CameraAware {
    private static final String TAG = "SunHeatEffect";

    // Posición del sol (debe coincidir con SolProcedural)
    private float sunX = -8.0f;
    private float sunY = 4.0f;
    private float sunZ = -15.0f;
    private float sunScale = 1.5f;

    // Parámetros del efecto
    private float haloScale = 2.8f;  // Tamaño del halo respecto al sol
    private float time = 0f;

    // OpenGL
    private int shaderProgram;
    private int aPositionLoc;
    private int uMVPLoc;
    private int uTimeLoc;
    private int uAlphaLoc;

    private FloatBuffer vertexBuffer;
    private static final int NUM_SEGMENTS = 32;  // Segmentos del círculo

    // Camera
    private CameraController camera;

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    public SunHeatEffect(Context context) {
        Log.d(TAG, "☀️🔥 Creando efecto de calor del Sol...");
        createShaders();
        createGeometry();
        Log.d(TAG, "☀️🔥 Efecto de calor inicializado");
    }

    private void createShaders() {
        // Vertex shader - Billboard que siempre mira a la cámara
        String vertexShader =
            "attribute vec4 a_Position;\n" +
            "uniform mat4 u_MVP;\n" +
            "uniform float u_Time;\n" +
            "varying vec2 v_Position;\n" +
            "void main() {\n" +
            "    v_Position = a_Position.xy;\n" +
            "    \n" +
            "    // Ondulación sutil basada en posición y tiempo\n" +
            "    vec4 pos = a_Position;\n" +
            "    float dist = length(pos.xy);\n" +
            "    float wave = sin(dist * 8.0 - u_Time * 3.0) * 0.03;\n" +
            "    pos.xy *= 1.0 + wave;\n" +
            "    \n" +
            "    gl_Position = u_MVP * pos;\n" +
            "}\n";

        // Fragment shader - Efecto de distorsión/calor
        String fragmentShader =
            "precision mediump float;\n" +
            "uniform float u_Time;\n" +
            "uniform float u_Alpha;\n" +
            "varying vec2 v_Position;\n" +
            "\n" +
            "// Función de ruido simple\n" +
            "float noise(vec2 st) {\n" +
            "    return fract(sin(dot(st, vec2(12.9898, 78.233))) * 43758.5453);\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "    float dist = length(v_Position);\n" +
            "    \n" +
            "    // Gradiente radial (más fuerte cerca del sol, desvanece hacia afuera)\n" +
            "    float innerRadius = 0.5;   // Donde empieza el efecto\n" +
            "    float outerRadius = 1.0;   // Donde termina\n" +
            "    \n" +
            "    if (dist < innerRadius || dist > outerRadius) {\n" +
            "        discard;  // Fuera del anillo\n" +
            "    }\n" +
            "    \n" +
            "    // Intensidad basada en distancia (más fuerte cerca del sol)\n" +
            "    float intensity = 1.0 - smoothstep(innerRadius, outerRadius, dist);\n" +
            "    intensity = intensity * intensity;  // Curva más pronunciada\n" +
            "    \n" +
            "    // Ondulaciones de calor\n" +
            "    float angle = atan(v_Position.y, v_Position.x);\n" +
            "    float wave1 = sin(angle * 12.0 + u_Time * 2.5) * 0.5 + 0.5;\n" +
            "    float wave2 = sin(angle * 8.0 - u_Time * 1.8 + 2.0) * 0.5 + 0.5;\n" +
            "    float wave3 = sin(dist * 15.0 - u_Time * 4.0) * 0.5 + 0.5;\n" +
            "    \n" +
            "    // Combinar ondulaciones\n" +
            "    float shimmer = wave1 * 0.4 + wave2 * 0.35 + wave3 * 0.25;\n" +
            "    shimmer = shimmer * intensity;\n" +
            "    \n" +
            "    // Ruido para variación orgánica\n" +
            "    float n = noise(v_Position * 10.0 + u_Time * 0.5);\n" +
            "    shimmer *= 0.8 + n * 0.4;\n" +
            "    \n" +
            "    // Color: naranja/amarillo cálido con variación\n" +
            "    vec3 hotColor = vec3(1.0, 0.6, 0.1);    // Naranja\n" +
            "    vec3 warmColor = vec3(1.0, 0.85, 0.4); // Amarillo cálido\n" +
            "    vec3 color = mix(warmColor, hotColor, shimmer);\n" +
            "    \n" +
            "    // Alpha final: muy sutil para efecto de distorsión\n" +
            "    float alpha = shimmer * intensity * u_Alpha * 0.25;\n" +
            "    \n" +
            "    gl_FragColor = vec4(color, alpha);\n" +
            "}\n";

        int vShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexShader);
        int fShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShader);

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vShader);
        GLES30.glAttachShader(shaderProgram, fShader);
        GLES30.glLinkProgram(shaderProgram);

        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(shaderProgram, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Error linking shader: " + GLES30.glGetProgramInfoLog(shaderProgram));
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
            return;
        }

        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "a_Position");
        uMVPLoc = GLES30.glGetUniformLocation(shaderProgram, "u_MVP");
        uTimeLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Time");
        uAlphaLoc = GLES30.glGetUniformLocation(shaderProgram, "u_Alpha");

        GLES30.glDeleteShader(vShader);
        GLES30.glDeleteShader(fShader);

        Log.d(TAG, "✓ Shaders de calor compilados");
    }

    private int compileShader(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Shader compile error: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private void createGeometry() {
        // Crear un disco con anillo (para el efecto de halo)
        // Usamos un quad grande que cubre el área del efecto
        float[] vertices = new float[(NUM_SEGMENTS + 1) * 2 * 3];  // (centro + segmentos) * 2 (inner/outer) * 3 coords

        int idx = 0;

        // Generar vértices en forma de anillo
        for (int i = 0; i <= NUM_SEGMENTS; i++) {
            float angle = (float) (i * 2.0 * Math.PI / NUM_SEGMENTS);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            // Vértice exterior (radio 1.0)
            vertices[idx++] = cos;
            vertices[idx++] = sin;
            vertices[idx++] = 0;

            // Vértice interior (radio 0.5)
            vertices[idx++] = cos * 0.5f;
            vertices[idx++] = sin * 0.5f;
            vertices[idx++] = 0;
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);
    }

    public void setSunPosition(float x, float y, float z, float scale) {
        this.sunX = x;
        this.sunY = y;
        this.sunZ = z;
        this.sunScale = scale;
    }

    @Override
    public void setCameraController(CameraController camera) {
        this.camera = camera;
    }

    @Override
    public void update(float deltaTime) {
        time += deltaTime;
        // Mantener tiempo en rango razonable para evitar pérdida de precisión
        if (time > 1000f) time -= 1000f;
    }

    @Override
    public void draw() {
        if (camera == null || shaderProgram == 0) return;

        // Configurar blending para efecto aditivo/transparente
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE);  // Blending aditivo
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);  // Sin depth test para overlay

        GLES30.glUseProgram(shaderProgram);

        // Construir matriz modelo (esfera centrada en el sol)
        // No necesita ser billboard, el efecto de calor rodea la esfera
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, sunX, sunY, sunZ);

        // Escalar al tamaño del halo
        float effectScale = sunScale * haloScale;
        Matrix.scaleM(modelMatrix, 0, effectScale, effectScale, effectScale);

        // Rotar suavemente para dar vida al efecto
        Matrix.rotateM(modelMatrix, 0, time * 5.0f, 0, 1, 0);

        // Calcular MVP
        camera.computeMvp(modelMatrix, mvpMatrix);

        // Enviar uniforms
        GLES30.glUniformMatrix4fv(uMVPLoc, 1, false, mvpMatrix, 0);
        GLES30.glUniform1f(uTimeLoc, time);
        GLES30.glUniform1f(uAlphaLoc, 1.0f);

        // Dibujar anillo
        vertexBuffer.position(0);
        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glVertexAttribPointer(aPositionLoc, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        // Dibujar como triangle strip para el anillo
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, (NUM_SEGMENTS + 1) * 2);

        GLES30.glDisableVertexAttribArray(aPositionLoc);

        // Restaurar estado
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
    }
}
