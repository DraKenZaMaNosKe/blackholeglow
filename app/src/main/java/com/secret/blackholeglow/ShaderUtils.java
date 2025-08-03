package com.secret.blackholeglow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Utilerías para shaders y texturas.
 */
public class ShaderUtils {

    /** Crea un FloatBuffer a partir de un array de floats. */
    public static FloatBuffer createFloatBuffer(float[] data) {
        ByteBuffer bb = ByteBuffer
                .allocateDirect(data.length * 4)
                .order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(data).position(0);
        return fb;
    }

    /** Compila un shader de tipo dado (VERTEX o FRAGMENT). */
    public static int loadShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        return shader;
    }

    /**
     * Crea un programa enlazando los dos shaders dados como cadenas.
     * Necesario para clases que usan shaders inline (p.ej. DeformableCubeBackground).
     */
    public static int createProgram(String vertexSource, String fragmentSource) {
        int vs = loadShader(GLES20.GL_VERTEX_SHADER,   vertexSource);
        int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        int prog = GLES20.glCreateProgram();
        GLES20.glAttachShader(prog, vs);
        GLES20.glAttachShader(prog, fs);
        GLES20.glLinkProgram(prog);
        return prog;
    }

    /**
     * Crea un programa OpenGL ES a partir de archivos .glsl en assets.
     */
    public static int createProgramFromAssets(
            Context ctx,
            String vertexAssetPath,
            String fragmentAssetPath) {
        String vSrc = loadAssetAsString(ctx, vertexAssetPath);
        String fSrc = loadAssetAsString(ctx, fragmentAssetPath);
        return createProgram(vSrc, fSrc);
    }

    /**
     * Carga una textura desde resourceId, genera mipmaps,
     * aplica wrap REPEAT y filtros apropiados. NO habilita blending.
     */
    public static int loadTexture(Context context, int resourceId) {
        // Generar handle
        final int[] handle = new int[1];
        GLES20.glGenTextures(1, handle, 0);
        if (handle[0] == 0) {
            throw new RuntimeException("❌ Error al generar ID de textura");
        }

        // Decodificar bitmap sin escalado
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = false;
        Bitmap bmp = BitmapFactory.decodeResource(
                context.getResources(), resourceId, opts
        );
        if (bmp == null) {
            throw new RuntimeException(
                    "❌ No se pudo decodificar recurso: " + resourceId);
        }

        // Bind y subir a GPU
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, handle[0]);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
        bmp.recycle();

        // Generar mipmaps
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);

        // Filtros: LINEAR con mipmaps
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR_MIPMAP_LINEAR
        );
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR
        );

        // Wrap en modo REPEAT
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_REPEAT
        );
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_REPEAT
        );

        // Verificar errores
        int err = GLES20.glGetError();
        if (err != GLES20.GL_NO_ERROR) {
            throw new RuntimeException(
                    "❌ Error configurando textura. GL error: " + err);
        }

        return handle[0];
    }

    /** Lee un archivo de texto en assets y lo devuelve como String. */
    public static String loadAssetAsString(
            Context ctx, String assetPath) {
        try (InputStream is = ctx.getAssets().open(assetPath);
             BufferedReader br = new BufferedReader(
                     new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException(
                    "Error leyendo asset: " + assetPath, e);
        }
    }
}
