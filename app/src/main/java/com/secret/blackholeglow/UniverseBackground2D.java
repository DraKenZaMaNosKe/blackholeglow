package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.os.SystemClock;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * UniverseBackground2D - Versión 2D pura sin perspectiva
 * Dibuja el fondo como un quad fullscreen en espacio de pantalla
 * Garantiza cobertura completa sin distorsión
 */
public class UniverseBackground2D implements SceneObject {
    private static final String TAG = "UniverseBackground2D";

    // Shader program
    private int programId;

    // Buffers
    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;

    // Uniform locations
    private int uTextureLoc;
    private int uTimeLoc;
    private int uResolutionLoc;
    private int uAspectRatioLoc;

    // Attribute locations
    private int aPositionLoc;
    private int aTexCoordLoc;

    // Texture
    private final int textureId;
    private final float timeOffset;

    // Vertex shader - espacio de pantalla (-1 a 1)
    private static final String VERTEX_SHADER =
            "attribute vec2 a_Position;\n" +
            "attribute vec2 a_TexCoord;\n" +
            "varying vec2 v_TexCoord;\n" +
            "void main() {\n" +
            "    gl_Position = vec4(a_Position, 0.0, 1.0);\n" +
            "    v_TexCoord = a_TexCoord;\n" +
            "}\n";

    // Fragment shader con ajuste de aspect ratio
    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform sampler2D u_Texture;\n" +
            "uniform float u_Time;\n" +
            "uniform vec2 u_Resolution;\n" +
            "uniform float u_AspectRatio;\n" +
            "varying vec2 v_TexCoord;\n" +
            "\n" +
            "void main() {\n" +
            "    // Ajustar UV para mantener proporción y cubrir toda la pantalla\n" +
            "    vec2 uv = v_TexCoord;\n" +
            "    \n" +
            "    // Centrar y escalar para cubrir (cover mode)\n" +
            "    vec2 center = vec2(0.5, 0.5);\n" +
            "    uv = (uv - center);\n" +
            "    \n" +
            "    // Ajuste de aspecto para modo cover\n" +
            "    float screenAspect = u_Resolution.x / u_Resolution.y;\n" +
            "    float textureAspect = 1.77777; // Asumiendo imagen 16:9\n" +
            "    \n" +
            "    if (screenAspect > textureAspect) {\n" +
            "        // Pantalla más ancha - escalar en Y\n" +
            "        uv.y *= textureAspect / screenAspect;\n" +
            "    } else {\n" +
            "        // Pantalla más alta - escalar en X\n" +
            "        uv.x *= screenAspect / textureAspect;\n" +
            "    }\n" +
            "    \n" +
            "    uv += center;\n" +
            "    \n" +
            "    // Animación desactivada (opcional para después)\n" +
            "    // uv.x += sin(u_Time * 0.05) * 0.01;\n" +
            "    // uv.y += cos(u_Time * 0.03) * 0.01;\n" +
            "    \n" +
            "    // Clamp para evitar bordes\n" +
            "    uv = clamp(uv, 0.0, 1.0);\n" +
            "    \n" +
            "    gl_FragColor = texture2D(u_Texture, uv);\n" +
            "}\n";

    public UniverseBackground2D(Context context, TextureManager textureManager, int textureResourceId) {
        Log.d(TAG, "Creating 2D Background");

        this.timeOffset = SystemClock.uptimeMillis() * 0.001f;
        this.textureId = textureManager.getTexture(textureResourceId);

        // Crear shader program
        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        programId = GLES20.glCreateProgram();
        GLES20.glAttachShader(programId, vertexShader);
        GLES20.glAttachShader(programId, fragmentShader);
        GLES20.glLinkProgram(programId);

        // Verificar enlace
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Error linking shader program: " + GLES20.glGetProgramInfoLog(programId));
            throw new RuntimeException("Error linking shader program");
        }

        // Obtener locations
        aPositionLoc = GLES20.glGetAttribLocation(programId, "a_Position");
        aTexCoordLoc = GLES20.glGetAttribLocation(programId, "a_TexCoord");
        uTextureLoc = GLES20.glGetUniformLocation(programId, "u_Texture");
        uTimeLoc = GLES20.glGetUniformLocation(programId, "u_Time");
        uResolutionLoc = GLES20.glGetUniformLocation(programId, "u_Resolution");
        uAspectRatioLoc = GLES20.glGetUniformLocation(programId, "u_AspectRatio");

        // Crear quad fullscreen (-1 a 1 en espacio NDC)
        float[] vertices = {
            -1.0f, -1.0f,  // Bottom left
             1.0f, -1.0f,  // Bottom right
            -1.0f,  1.0f,  // Top left
             1.0f,  1.0f   // Top right
        };

        // UV coordinates (0 a 1)
        float[] texCoords = {
            0.0f, 1.0f,  // Bottom left
            1.0f, 1.0f,  // Bottom right
            0.0f, 0.0f,  // Top left
            1.0f, 0.0f   // Top right
        };

        // Crear buffers
        vertexBuffer = createFloatBuffer(vertices);
        texCoordBuffer = createFloatBuffer(texCoords);

        Log.d(TAG, "2D Background initialized successfully");
    }

    private int compileShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);

        int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }

        return shader;
    }

    private FloatBuffer createFloatBuffer(float[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(data);
        fb.position(0);
        return fb;
    }

    @Override
    public void update(float deltaTime) {
        // No animation in model space
    }

    @Override
    public void draw() {
        // Usar nuestro programa
        GLES20.glUseProgram(programId);

        // Desactivar depth test para fondo
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthMask(false);

        // Configurar uniforms
        float time = (SystemClock.uptimeMillis() * 0.001f - timeOffset) % 100.0f;
        GLES20.glUniform1f(uTimeLoc, time);

        float screenWidth = SceneRenderer.screenWidth;
        float screenHeight = SceneRenderer.screenHeight;
        GLES20.glUniform2f(uResolutionLoc, screenWidth, screenHeight);
        GLES20.glUniform1f(uAspectRatioLoc, screenWidth / screenHeight);

        // Configurar textura
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTextureLoc, 0);

        // Configurar atributos
        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(aTexCoordLoc);
        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

        // Dibujar como triangle strip
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Limpiar
        GLES20.glDisableVertexAttribArray(aPositionLoc);
        GLES20.glDisableVertexAttribArray(aTexCoordLoc);

        // Restaurar depth test
        GLES20.glDepthMask(true);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }
}