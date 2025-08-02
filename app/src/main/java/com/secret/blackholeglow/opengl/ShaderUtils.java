package com.secret.blackholeglow.opengl;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ShaderUtils {

    private static final String TAG = "ShaderUtils";

    public static int createProgram(Context context, String vertexAssetPath, String fragmentAssetPath) {
        String vertexCode = loadShaderFromAssets(context, vertexAssetPath);
        String fragmentCode = loadShaderFromAssets(context, fragmentAssetPath);

        if (vertexCode == null || fragmentCode == null) {
            Log.e(TAG, "🛑 Shader source is null. Check asset paths.");
            return 0;
        }

        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexCode);
        int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentCode);

        if (vertexShader == 0 || fragmentShader == 0) {
            Log.e(TAG, "🛑 Failed to compile shaders.");
            return 0;
        }

        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        final int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "🛑 Error linking program: " + GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            return 0;
        }

        Log.d("openGL/ShaderUtils", "✅ Shader program created successfully.");
        return program;
    }

    private static int compileShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            Log.e(TAG, "🛑 Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }

        return shader;
    }

    private static String loadShaderFromAssets(Context context, String fileName) {
        StringBuilder sb = new StringBuilder();
        try {
            InputStream is = context.getAssets().open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            is.close();
        } catch (Exception e) {
            Log.e(TAG, "🛑 Error loading shader from assets: " + fileName, e);
            return null;
        }
        return sb.toString();
    }
}
