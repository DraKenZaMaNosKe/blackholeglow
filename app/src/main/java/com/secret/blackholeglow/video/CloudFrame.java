package com.secret.blackholeglow.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import com.secret.blackholeglow.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                    ☁️ CloudFrame - Marco de Nubes Rotatorio              ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Capa frontal para LabScene con rotación infinita en sentido horario.    ║
 * ║  Las nubes giran eternamente sin saltos ni reversa.                      ║
 * ║  Mantiene proporción 1:1 sin estirar la imagen.                          ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class CloudFrame {
    private static final String TAG = "CloudFrame";

    // Velocidad de rotación (grados por segundo) - sentido horario
    private static final float ROTATION_SPEED = 8.0f;

    private int textureId = -1;
    private int shaderProgram;
    private FloatBuffer vertexBuffer;

    private int aPositionLoc;
    private int aTexCoordLoc;
    private int uTextureLoc;
    private int uTimeLoc;
    private int uAspectLoc;

    private float time = 0f;
    private float aspectRatio = 1.78f;  // 1920/1080 default

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
        "uniform float uTime;\n" +
        "uniform float uAspect;\n" +
        "in vec2 vTexCoord;\n" +
        "out vec4 fragColor;\n" +
        "void main() {\n" +
        "    vec2 center = vec2(0.5);\n" +
        "    vec2 uv = vTexCoord - center;\n" +
        "    \n" +
        "    // Corregir aspect ratio: estirar X para compensar pantalla alta\n" +
        "    uv.y *= uAspect;\n" +
        "    \n" +
        "    // Rotación\n" +
        "    float c = cos(uTime);\n" +
        "    float s = sin(uTime);\n" +
        "    vec2 rotatedUV = vec2(\n" +
        "        uv.x * c - uv.y * s,\n" +
        "        uv.x * s + uv.y * c\n" +
        "    );\n" +
        "    \n" +
        "    // Revertir aspect ratio y volver al centro\n" +
        "    rotatedUV.y /= uAspect;\n" +
        "    rotatedUV += center;\n" +
        "    \n" +
        "    vec4 col = texture(uTexture, rotatedUV);\n" +
        "    \n" +
        "    // Máscara circular fija (sin distorsión por aspect)\n" +
        "    vec2 maskUV = vTexCoord - center;\n" +
        "    maskUV.y *= uAspect;\n" +
        "    float dist = length(maskUV);\n" +
        "    float centerMask = smoothstep(0.25, 0.35, dist);\n" +
        "    col.a *= centerMask;\n" +
        "    \n" +
        "    fragColor = col;\n" +
        "}\n";

    public CloudFrame(Context context) {
        ByteBuffer bb = ByteBuffer.allocateDirect(VERTICES.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(VERTICES);
        vertexBuffer.position(0);

        compileShader();
        loadTexture(context);

        Log.d(TAG, "☁️ CloudFrame inicializado (vel: " + ROTATION_SPEED + "°/s)");
    }

    private void compileShader() {
        int vs = compileShaderCode(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = compileShaderCode(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        if (vs == 0 || fs == 0) {
            Log.e(TAG, "❌ Error compilando shaders");
            return;
        }

        shaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(shaderProgram, vs);
        GLES30.glAttachShader(shaderProgram, fs);
        GLES30.glLinkProgram(shaderProgram);

        int[] linked = new int[1];
        GLES30.glGetProgramiv(shaderProgram, GLES30.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            Log.e(TAG, "❌ Link error: " + GLES30.glGetProgramInfoLog(shaderProgram));
            GLES30.glDeleteProgram(shaderProgram);
            shaderProgram = 0;
            GLES30.glDeleteShader(vs);
            GLES30.glDeleteShader(fs);
            return;
        }

        aPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "aPosition");
        aTexCoordLoc = GLES30.glGetAttribLocation(shaderProgram, "aTexCoord");
        uTextureLoc = GLES30.glGetUniformLocation(shaderProgram, "uTexture");
        uTimeLoc = GLES30.glGetUniformLocation(shaderProgram, "uTime");
        uAspectLoc = GLES30.glGetUniformLocation(shaderProgram, "uAspect");

        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);
    }

    private int compileShaderCode(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Shader error: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private void loadTexture(Context context) {
        int[] texIds = new int[1];
        GLES30.glGenTextures(1, texIds, 0);
        textureId = texIds[0];

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        // 🔧 FIX OOM: Downsample large resource to 512x512 max
        BitmapFactory.Options cfOpts = new BitmapFactory.Options();
        cfOpts.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.getResources(), R.drawable.preview_oceano_sc, cfOpts);
        int maxCfSize = 512;
        cfOpts.inSampleSize = 1;
        while (cfOpts.outWidth / cfOpts.inSampleSize > maxCfSize ||
               cfOpts.outHeight / cfOpts.inSampleSize > maxCfSize) {
            cfOpts.inSampleSize *= 2;
        }
        cfOpts.inJustDecodeBounds = false;
        cfOpts.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.preview_oceano_sc, cfOpts);
        if (bitmap != null) {
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();
            Log.d(TAG, "✅ Textura frame cargada");
        } else {
            Log.e(TAG, "❌ No se pudo cargar textura frame");
        }
    }

    /**
     * Configura el tamaño de pantalla para corregir aspect ratio
     */
    public void setScreenSize(int width, int height) {
        aspectRatio = (float) height / (float) width;
        Log.d(TAG, "📐 Aspect ratio: " + aspectRatio);
    }

    public void update(float deltaTime) {
        time -= deltaTime * ROTATION_SPEED * (float)(Math.PI / 180.0);
        if (time < -6.283185f) {
            time += 6.283185f;
        }
    }

    public void draw() {
        if (shaderProgram == 0 || textureId == -1) return;

        GLES30.glUseProgram(shaderProgram);

        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // Uniforms
        GLES30.glUniform1f(uTimeLoc, time);
        GLES30.glUniform1f(uAspectLoc, aspectRatio);

        // Textura
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(uTextureLoc, 0);

        // Vértices (posición)
        vertexBuffer.position(0);
        GLES30.glEnableVertexAttribArray(aPositionLoc);
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer);

        // Vértices (texcoord)
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
        Log.d(TAG, "🗑️ CloudFrame liberado");
    }
}
