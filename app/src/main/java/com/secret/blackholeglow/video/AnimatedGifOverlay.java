package com.secret.blackholeglow.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                    🎞️ AnimatedGifOverlay - GIF Animado en OpenGL         ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Carga un GIF animado y lo renderiza como overlay con transparencia.     ║
 * ║  Usa android.graphics.Movie para decodificar los frames.                 ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class AnimatedGifOverlay {
    private static final String TAG = "AnimatedGifOverlay";

    private Movie movie;
    private Bitmap frameBitmap;
    private Canvas frameCanvas;
    private int textureId = -1;
    private int shaderProgram;
    private FloatBuffer vertexBuffer;

    private int aPositionLoc;
    private int aTexCoordLoc;
    private int uTextureLoc;
    private int uAlphaLoc;

    private float alpha = 0.85f;  // Transparencia general (0-1)

    private long startTime = 0;
    private int gifWidth, gifHeight;

    private static final float[] VERTICES = {
        // X, Y, U, V
        -1f, -1f, 0f, 1f,
         1f, -1f, 1f, 1f,
        -1f,  1f, 0f, 0f,
         1f,  1f, 1f, 0f,
    };

    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "in vec2 aPosition;\n" +
        "in vec2 aTexCoord;\n" +
        "out vec2 vTexCoord;\n" +
        "void main() {\n" +
        "    gl_Position = vec4(aPosition, 0.0, 1.0);\n" +
        "    vTexCoord = aTexCoord;\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "uniform sampler2D uTexture;\n" +
        "uniform float uAlpha;\n" +
        "in vec2 vTexCoord;\n" +
        "out vec4 fragColor;\n" +
        "void main() {\n" +
        "    vec4 col = texture(uTexture, vTexCoord);\n" +
        "    float lum = dot(col.rgb, vec3(0.299, 0.587, 0.114));\n" +
        "    float edgeFade = smoothstep(0.2, 0.6, lum);\n" +
        "    col.a *= edgeFade * uAlpha;\n" +
        "    fragColor = col;\n" +
        "}\n";

    public AnimatedGifOverlay(Context context, String gifFileName) {
        try {
            // Cargar GIF desde assets
            InputStream is = context.getAssets().open(gifFileName);
            movie = Movie.decodeStream(is);
            is.close();

            if (movie != null) {
                gifWidth = movie.width();
                gifHeight = movie.height();

                // Crear bitmap para renderizar frames
                frameBitmap = Bitmap.createBitmap(gifWidth, gifHeight, Bitmap.Config.ARGB_8888);
                frameCanvas = new Canvas(frameBitmap);

                Log.d(TAG, "🎞️ GIF cargado: " + gifWidth + "x" + gifHeight +
                      ", duración: " + movie.duration() + "ms");
            } else {
                Log.e(TAG, "❌ Movie.decodeStream retornó null");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error cargando GIF: " + e.getMessage());
        }

        // Crear vertex buffer
        ByteBuffer bb = ByteBuffer.allocateDirect(VERTICES.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(VERTICES);
        vertexBuffer.position(0);

        // Compilar shaders
        compileShader();

        // Crear textura OpenGL
        createTexture();

        startTime = System.currentTimeMillis();
    }

    private void compileShader() {
        int vs = GLES30.glCreateShader(GLES30.GL_VERTEX_SHADER);
        GLES30.glShaderSource(vs, VERTEX_SHADER);
        GLES30.glCompileShader(vs);

        int fs = GLES30.glCreateShader(GLES30.GL_FRAGMENT_SHADER);
        GLES30.glShaderSource(fs, FRAGMENT_SHADER);
        GLES30.glCompileShader(fs);

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vs);
        GLES30.glAttachShader(shaderProgram, fs);
        GLES30.glLinkProgram(shaderProgram);

        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "aPosition");
        aTexCoordLoc = GLES30.glGetAttribLocation(shaderProgram, "aTexCoord");
        uTextureLoc = GLES30.glGetUniformLocation(shaderProgram, "uTexture");
        uAlphaLoc = GLES30.glGetUniformLocation(shaderProgram, "uAlpha");

        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);
    }

    private void createTexture() {
        int[] texIds = new int[1];
        GLES30.glGenTextures(1, texIds, 0);
        textureId = texIds[0];

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        Log.d(TAG, "✅ Textura OpenGL creada: " + textureId);
    }

    public void draw() {
        if (movie == null || shaderProgram == 0 || textureId == -1) return;

        // Calcular tiempo actual con PING-PONG (sin salto al reiniciar)
        int duration = movie.duration();
        if (duration == 0) duration = 1000; // fallback

        // Ciclo completo = 2x duración (ida y vuelta)
        long elapsed = System.currentTimeMillis() - startTime;
        int cycleTime = (int)(elapsed % (duration * 2));

        int currentTime;
        if (cycleTime < duration) {
            // Primera mitad: reproducir hacia adelante
            currentTime = cycleTime;
        } else {
            // Segunda mitad: reproducir hacia atrás (ping-pong)
            currentTime = duration - (cycleTime - duration);
        }

        // Renderizar frame actual a bitmap
        frameBitmap.eraseColor(0x00000000); // Limpiar con transparente
        movie.setTime(currentTime);
        movie.draw(frameCanvas, 0, 0);

        // Subir bitmap a textura OpenGL
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, frameBitmap, 0);

        // Dibujar
        GLES30.glUseProgram(shaderProgram);

        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(uTextureLoc, 0);
        GLES30.glUniform1f(uAlphaLoc, alpha);

        vertexBuffer.position(0);
        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer);

        vertexBuffer.position(2);
        GLES30.glEnableVertexAttribArray(aTexCoordLoc);
        GLES30.glVertexAttribPointer(aTexCoordLoc, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glDisableVertexAttribArray(aPositionLoc);
        GLES30.glDisableVertexAttribArray(aTexCoordLoc);
    }

    public void release() {
        if (textureId != -1) {
            GLES30.glDeleteTextures(1, new int[]{textureId}, 0);
            textureId = -1;
        }
        if (shaderProgram != 0) {
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
        if (frameBitmap != null) {
            frameBitmap.recycle();
            frameBitmap = null;
        }
        movie = null;
        Log.d(TAG, "🗑️ AnimatedGifOverlay liberado");
    }

    public boolean isLoaded() {
        return movie != null;
    }
}
