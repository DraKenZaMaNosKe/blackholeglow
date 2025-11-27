package com.secret.blackholeglow.gl3;

import android.content.Context;
import android.opengl.GLES30;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║   🎨 ShaderProgram3 - Gestión de Shaders OpenGL ES 3.0 🎨        ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * Clase para cargar, compilar y gestionar shaders GLSL ES 3.0.
 *
 * Características:
 * - Carga shaders desde assets
 * - Cache de uniform locations (evita glGetUniformLocation cada frame)
 * - Soporte para GLSL ES 3.0 (#version 300 es)
 * - Validación y logging de errores
 *
 * Uso:
 *   ShaderProgram3 shader = new ShaderProgram3(context,
 *       "shaders/particle_vertex.glsl",
 *       "shaders/particle_fragment.glsl");
 *
 *   shader.use();
 *   shader.setUniform("u_Time", time);
 *   shader.setUniform("u_MVP", mvpMatrix);
 */
public class ShaderProgram3 {
    private static final String TAG = "ShaderProgram3";

    private int programId;
    private final Map<String, Integer> uniformCache = new HashMap<>();
    private final Map<String, Integer> attributeCache = new HashMap<>();

    /**
     * Crea un shader program desde archivos en assets
     * @param context Context para acceder a assets
     * @param vertexPath Ruta al vertex shader (ej: "shaders/vertex.glsl")
     * @param fragmentPath Ruta al fragment shader
     */
    public ShaderProgram3(Context context, String vertexPath, String fragmentPath) {
        String vertexSource = loadShaderSource(context, vertexPath);
        String fragmentSource = loadShaderSource(context, fragmentPath);

        if (vertexSource == null || fragmentSource == null) {
            Log.e(TAG, "Error cargando shaders desde: " + vertexPath + ", " + fragmentPath);
            programId = 0;
            return;
        }

        programId = createProgram(vertexSource, fragmentSource);

        if (programId != 0) {
            Log.d(TAG, "✓ Shader program creado: " + programId);
            Log.d(TAG, "  Vertex: " + vertexPath);
            Log.d(TAG, "  Fragment: " + fragmentPath);
        }
    }

    /**
     * Crea un shader program desde strings de código fuente
     */
    public ShaderProgram3(String vertexSource, String fragmentSource) {
        programId = createProgram(vertexSource, fragmentSource);

        if (programId != 0) {
            Log.d(TAG, "✓ Shader program creado desde strings: " + programId);
        }
    }

    /**
     * Usa este programa de shaders
     */
    public void use() {
        GLES30.glUseProgram(programId);
    }

    /**
     * Obtiene el ID del programa
     */
    public int getProgramId() {
        return programId;
    }

    /**
     * Verifica si el programa es válido
     */
    public boolean isValid() {
        return programId != 0 && GLES30.glIsProgram(programId);
    }

    // ═══════════════════════════════════════════════════════════════
    // UNIFORMS - Con cache automático
    // ═══════════════════════════════════════════════════════════════

    /**
     * Obtiene location de uniform (con cache)
     */
    public int getUniformLocation(String name) {
        Integer cached = uniformCache.get(name);
        if (cached != null) {
            return cached;
        }

        int location = GLES30.glGetUniformLocation(programId, name);
        if (location == -1) {
            Log.w(TAG, "Uniform no encontrado: " + name);
        }
        uniformCache.put(name, location);
        return location;
    }

    /**
     * Establece uniform float
     */
    public void setUniform(String name, float value) {
        int loc = getUniformLocation(name);
        if (loc != -1) {
            GLES30.glUniform1f(loc, value);
        }
    }

    /**
     * Establece uniform int
     */
    public void setUniform(String name, int value) {
        int loc = getUniformLocation(name);
        if (loc != -1) {
            GLES30.glUniform1i(loc, value);
        }
    }

    /**
     * Establece uniform vec2
     */
    public void setUniform(String name, float x, float y) {
        int loc = getUniformLocation(name);
        if (loc != -1) {
            GLES30.glUniform2f(loc, x, y);
        }
    }

    /**
     * Establece uniform vec3
     */
    public void setUniform(String name, float x, float y, float z) {
        int loc = getUniformLocation(name);
        if (loc != -1) {
            GLES30.glUniform3f(loc, x, y, z);
        }
    }

    /**
     * Establece uniform vec4
     */
    public void setUniform(String name, float x, float y, float z, float w) {
        int loc = getUniformLocation(name);
        if (loc != -1) {
            GLES30.glUniform4f(loc, x, y, z, w);
        }
    }

    /**
     * Establece uniform vec3 desde array
     */
    public void setUniform3fv(String name, float[] values) {
        int loc = getUniformLocation(name);
        if (loc != -1) {
            GLES30.glUniform3fv(loc, 1, values, 0);
        }
    }

    /**
     * Establece uniform vec4 desde array
     */
    public void setUniform4fv(String name, float[] values) {
        int loc = getUniformLocation(name);
        if (loc != -1) {
            GLES30.glUniform4fv(loc, 1, values, 0);
        }
    }

    /**
     * Establece uniform mat4
     */
    public void setUniformMatrix4fv(String name, float[] matrix) {
        int loc = getUniformLocation(name);
        if (loc != -1) {
            GLES30.glUniformMatrix4fv(loc, 1, false, matrix, 0);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ATTRIBUTES - Con cache automático
    // ═══════════════════════════════════════════════════════════════

    /**
     * Obtiene location de attribute (con cache)
     */
    public int getAttributeLocation(String name) {
        Integer cached = attributeCache.get(name);
        if (cached != null) {
            return cached;
        }

        int location = GLES30.glGetAttribLocation(programId, name);
        if (location == -1) {
            Log.w(TAG, "Attribute no encontrado: " + name);
        }
        attributeCache.put(name, location);
        return location;
    }

    // ═══════════════════════════════════════════════════════════════
    // LIMPIEZA
    // ═══════════════════════════════════════════════════════════════

    /**
     * Libera recursos del shader
     */
    public void dispose() {
        if (programId != 0) {
            GLES30.glDeleteProgram(programId);
            programId = 0;
            uniformCache.clear();
            attributeCache.clear();
            Log.d(TAG, "Shader program eliminado");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // MÉTODOS PRIVADOS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Carga código fuente de shader desde assets
     */
    private String loadShaderSource(Context context, String path) {
        try {
            InputStream is = context.getAssets().open(path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }

            reader.close();
            return sb.toString();

        } catch (IOException e) {
            Log.e(TAG, "Error leyendo shader: " + path, e);
            return null;
        }
    }

    /**
     * Compila y linka un programa de shaders
     */
    private int createProgram(String vertexSource, String fragmentSource) {
        // Compilar vertex shader
        int vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }

        // Compilar fragment shader
        int fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource);
        if (fragmentShader == 0) {
            GLES30.glDeleteShader(vertexShader);
            return 0;
        }

        // Crear programa
        int program = GLES30.glCreateProgram();
        if (program == 0) {
            Log.e(TAG, "Error creando programa");
            return 0;
        }

        // Adjuntar shaders
        GLES30.glAttachShader(program, vertexShader);
        GLES30.glAttachShader(program, fragmentShader);

        // Linkear
        GLES30.glLinkProgram(program);

        // Verificar link
        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0);

        if (linkStatus[0] == 0) {
            String error = GLES30.glGetProgramInfoLog(program);
            Log.e(TAG, "Error linkeando programa: " + error);
            GLES30.glDeleteProgram(program);
            GLES30.glDeleteShader(vertexShader);
            GLES30.glDeleteShader(fragmentShader);
            return 0;
        }

        // Eliminar shaders (ya están en el programa)
        GLES30.glDeleteShader(vertexShader);
        GLES30.glDeleteShader(fragmentShader);

        return program;
    }

    /**
     * Compila un shader individual
     */
    private int compileShader(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        if (shader == 0) {
            Log.e(TAG, "Error creando shader");
            return 0;
        }

        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);

        int[] compileStatus = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0);

        if (compileStatus[0] == 0) {
            String error = GLES30.glGetShaderInfoLog(shader);
            String typeName = (type == GLES30.GL_VERTEX_SHADER) ? "VERTEX" : "FRAGMENT";
            Log.e(TAG, "Error compilando " + typeName + " shader:\n" + error);

            // Mostrar líneas problemáticas
            String[] lines = source.split("\n");
            for (int i = 0; i < Math.min(lines.length, 20); i++) {
                Log.e(TAG, String.format("%3d: %s", i + 1, lines[i]));
            }

            GLES30.glDeleteShader(shader);
            return 0;
        }

        return shader;
    }
}
