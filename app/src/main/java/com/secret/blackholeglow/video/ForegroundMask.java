package com.secret.blackholeglow.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔════════════════════════════════════════════════════════════════╗
 * ║         ForegroundMask - Capa de Primer Plano                  ║
 * ╠════════════════════════════════════════════════════════════════╣
 * ║  Renderiza una imagen PNG con transparencia encima de todo.    ║
 * ║  Esto crea la ilusión de profundidad: el pez nada "detrás"     ║
 * ║  de las plantas del primer plano.                              ║
 * ║                                                                ║
 * ║  Orden de renderizado:                                         ║
 * ║  1. Video de fondo                                             ║
 * ║  2. Pez alienígena                                             ║
 * ║  3. ForegroundMask (plantas frontales) ← ESTE                  ║
 * ╚════════════════════════════════════════════════════════════════╝
 */
public class ForegroundMask {
    private static final String TAG = "ForegroundMask";

    private Context context;
    private String maskFileName;  // Archivo en assets (ej: "foreground_plants.png")

    private int textureId = -1;
    private int shaderProgram;
    private int aPositionLoc;
    private int aTexCoordLoc;
    private int uTextureLoc;
    private int uTimeLoc;

    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;

    private boolean initialized = false;
    private boolean hasTexture = false;

    // Tiempo acumulado para animaciones
    private float time = 0f;

    // Fullscreen quad
    private static final float[] VERTICES = {
        -1f, -1f,
         1f, -1f,
        -1f,  1f,
         1f,  1f
    };

    private static final float[] TEX_COORDS = {
        0f, 1f,
        1f, 1f,
        0f, 0f,
        1f, 0f
    };

    // Shaders simples para textura con alpha
    private static final String VERTEX_SHADER =
        "attribute vec2 aPosition;\n" +
        "attribute vec2 aTexCoord;\n" +
        "varying vec2 vTexCoord;\n" +
        "void main() {\n" +
        "    gl_Position = vec4(aPosition, 0.0, 1.0);\n" +
        "    vTexCoord = aTexCoord;\n" +
        "}\n";

    // Shader con efectos: ondulación submarina + brillo bioluminiscente
    private static final String FRAGMENT_SHADER =
        "precision mediump float;\n" +
        "uniform sampler2D uTexture;\n" +
        "uniform float uTime;\n" +
        "varying vec2 vTexCoord;\n" +
        "void main() {\n" +
        "    vec2 uv = vTexCoord;\n" +
        "    // Ondulacion suave como plantas bajo el agua\n" +
        "    float wave = sin(uv.y * 8.0 + uTime * 2.0) * 0.004;\n" +
        "    float wave2 = sin(uv.y * 12.0 - uTime * 1.5) * 0.003;\n" +
        "    uv.x += (wave + wave2) * (1.0 - uv.y);\n" +
        "    vec4 color = texture2D(uTexture, uv);\n" +
        "    // Brillo bioluminiscente en cyan\n" +
        "    float isCyan = step(0.3, color.b) * step(color.r, 0.6);\n" +
        "    float pulse = 0.12 * sin(uTime * 3.0) + 0.12;\n" +
        "    color.rgb += vec3(0.0, pulse * 0.4, pulse) * isCyan * color.a;\n" +
        "    gl_FragColor = color;\n" +
        "}\n";

    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════

    public ForegroundMask(Context context, String maskFileName) {
        this.context = context;
        this.maskFileName = maskFileName;
    }

    // ═══════════════════════════════════════════════════════════════
    // INICIALIZACIÓN
    // ═══════════════════════════════════════════════════════════════

    public void initialize() {
        // Crear buffers
        ByteBuffer bb = ByteBuffer.allocateDirect(VERTICES.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(VERTICES);
        vertexBuffer.position(0);

        bb = ByteBuffer.allocateDirect(TEX_COORDS.length * 4);
        bb.order(ByteOrder.nativeOrder());
        texCoordBuffer = bb.asFloatBuffer();
        texCoordBuffer.put(TEX_COORDS);
        texCoordBuffer.position(0);

        // Compilar shaders
        int vs = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vs);
        GLES20.glAttachShader(shaderProgram, fs);
        GLES20.glLinkProgram(shaderProgram);

        aPositionLoc = GLES20.glGetAttribLocation(shaderProgram, "aPosition");
        aTexCoordLoc = GLES20.glGetAttribLocation(shaderProgram, "aTexCoord");
        uTextureLoc = GLES20.glGetUniformLocation(shaderProgram, "uTexture");
        uTimeLoc = GLES20.glGetUniformLocation(shaderProgram, "uTime");

        // Cargar textura PNG
        loadTexture();

        initialized = true;
        Log.d(TAG, "🌿 ForegroundMask inicializado" + (hasTexture ? " con textura" : " SIN textura"));
    }

    // ═══════════════════════════════════════════════════════════════
    // CARGAR TEXTURA PNG
    // ═══════════════════════════════════════════════════════════════

    private void loadTexture() {
        try {
            InputStream is = context.getAssets().open(maskFileName);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            is.close();

            if (bitmap == null) {
                Log.w(TAG, "⚠️ No se pudo decodificar: " + maskFileName);
                return;
            }

            // Crear textura OpenGL
            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            textureId = textures[0];

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();

            hasTexture = true;
            Log.d(TAG, "✅ Textura cargada: " + maskFileName);

        } catch (IOException e) {
            Log.w(TAG, "⚠️ Archivo no encontrado: " + maskFileName + " (esto es normal si aún no existe)");
            hasTexture = false;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // DRAW
    // ═══════════════════════════════════════════════════════════════

    public void draw(float deltaTime) {
        if (!initialized || !hasTexture) return;

        // Actualizar tiempo para animaciones
        time += deltaTime;
        if (time > 628.0f) time -= 628.0f; // Reset cada ~100 ciclos

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glUseProgram(shaderProgram);

        // Pasar tiempo al shader
        GLES20.glUniform1f(uTimeLoc, time);

        // Bind textura
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTextureLoc, 0);

        // Vertices
        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(aTexCoordLoc);
        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

        // Dibujar
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(aPositionLoc);
        GLES20.glDisableVertexAttribArray(aTexCoordLoc);
    }

    // Sobrecarga para compatibilidad
    public void draw() {
        draw(0.016f); // ~60fps default
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════════

    private int compileShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Shader error: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    public boolean hasTexture() {
        return hasTexture;
    }

    public void release() {
        if (textureId != -1) {
            int[] textures = {textureId};
            GLES20.glDeleteTextures(1, textures, 0);
            textureId = -1;
        }
        if (shaderProgram != 0) {
            GLES20.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        initialized = false;
        hasTexture = false;
    }
}
