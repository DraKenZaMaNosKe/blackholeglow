package com.secret.blackholeglow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Utilerías unificadas para shaders y texturas.
 * Combina la funcionalidad de ambas clases ShaderUtils.
 */
public class ShaderUtils {

    private static final String TAG = "ShaderUtils";

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

        // Verificar compilación
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }

        return shader;
    }

    /**
     * Alias de loadShader() para compatibilidad con código existente.
     * Algunas clases usan compileShader() en lugar de loadShader().
     */
    public static int compileShader(int type, String source) {
        return loadShader(type, source);
    }

    /**
     * Crea un programa enlazando los dos shaders dados como cadenas.
     * Necesario para clases que usan shaders inline.
     */
    public static int createProgram(String vertexSource, String fragmentSource) {
        int vs = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);

        if (vs == 0 || fs == 0) {
            Log.e(TAG, "Failed to compile shaders");
            return 0;
        }

        int prog = GLES20.glCreateProgram();
        GLES20.glAttachShader(prog, vs);
        GLES20.glAttachShader(prog, fs);
        GLES20.glLinkProgram(prog);

        // Verificar enlace
        final int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Error linking program: " + GLES20.glGetProgramInfoLog(prog));
            GLES20.glDeleteProgram(prog);
            return 0;
        }

        Log.d(TAG, "Shader program created successfully");
        return prog;
    }

    /**
     * Crea un programa OpenGL ES a partir de archivos .glsl en assets.
     * Este método es el principal, usado por la mayoría del código.
     */
    public static int createProgramFromAssets(
            Context ctx,
            String vertexAssetPath,
            String fragmentAssetPath) {
        String vSrc = loadAssetAsString(ctx, vertexAssetPath);
        String fSrc = loadAssetAsString(ctx, fragmentAssetPath);

        if (vSrc == null || fSrc == null) {
            Log.e(TAG, "Shader source is null. Check asset paths.");
            return 0;
        }

        return createProgram(vSrc, fSrc);
    }

    /**
     * Método alternativo con el mismo nombre que la versión de opengl.ShaderUtils
     * para mantener compatibilidad con AnimatedBorderRendererThread
     */
    public static int createProgram(Context context, String vertexAssetPath, String fragmentAssetPath) {
        return createProgramFromAssets(context, vertexAssetPath, fragmentAssetPath);
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
            Log.e(TAG, "Error al generar ID de textura");
            throw new RuntimeException("Error al generar ID de textura");
        }

        // Decodificar bitmap sin escalado
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = false;
        Bitmap bmp = BitmapFactory.decodeResource(
                context.getResources(), resourceId, opts
        );
        if (bmp == null) {
            Log.e(TAG, "No se pudo decodificar recurso: " + resourceId);
            throw new RuntimeException(
                    "No se pudo decodificar recurso: " + resourceId);
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
            Log.e(TAG, "Error configurando textura. GL error: " + err);
            throw new RuntimeException(
                    "Error configurando textura. GL error: " + err);
        }

        return handle[0];
    }

    /** Lee un archivo de texto en assets y lo devuelve como String. */
    public static String loadAssetAsString(Context ctx, String assetPath) {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = ctx.getAssets().open(assetPath);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (IOException e) {
            Log.e(TAG, "Error leyendo asset: " + assetPath, e);
            return null;
        }
    }
}