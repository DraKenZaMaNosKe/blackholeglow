package com.secret.blackholeglow.systems;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import com.secret.blackholeglow.TextureLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                      ResourceManager                              ║
 * ║                      "El Utilero"                                 ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  Gestiona todos los recursos de OpenGL:                          ║
 * ║  • Texturas (con cache)                                          ║
 * ║  • Shaders (con cache)                                           ║
 * ║  • Programas de shader (con cache)                               ║
 * ║  • Assets (OBJ, strings)                                         ║
 * ║                                                                  ║
 * ║  RESPONSABILIDADES:                                              ║
 * ║  • Cargar recursos bajo demanda (lazy loading)                   ║
 * ║  • Cachear recursos para reutilización                           ║
 * ║  • Liberar recursos cuando no se necesitan                       ║
 * ║  • Reportar uso de memoria                                       ║
 * ║                                                                  ║
 * ║  NO HACE:                                                        ║
 * ║  ✗ Decidir qué recursos cargar (lo decide la escena)             ║
 * ║  ✗ Renderizar nada                                               ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class ResourceManager implements TextureLoader {
    private static final String TAG = "ResourceManager";

    // ═══════════════════════════════════════════════════════════════
    // 📦 SINGLETON
    // ═══════════════════════════════════════════════════════════════

    private static ResourceManager instance;
    private Context context;
    private boolean initialized = false;

    // ═══════════════════════════════════════════════════════════════
    // 🗃️ CACHES
    // ═══════════════════════════════════════════════════════════════

    // Cache de texturas: resourceId → GL texture ID
    private final Map<Integer, Integer> textureCache = new HashMap<>();

    // Cache de shaders compilados: assetPath → GL shader ID
    private final Map<String, Integer> shaderCache = new HashMap<>();

    // Cache de programas: "vertex|fragment" → GL program ID
    private final Map<String, Integer> programCache = new HashMap<>();

    // ═══════════════════════════════════════════════════════════════
    // 📊 ESTADÍSTICAS
    // ═══════════════════════════════════════════════════════════════

    private int texturesLoaded = 0;
    private int shadersCompiled = 0;
    private int programsLinked = 0;
    private long totalTextureMemory = 0;  // Estimado en bytes

    // 🧠 Memory tier: inSampleSize propagado desde TextureManager
    private int inSampleSize = 1;

    // ═══════════════════════════════════════════════════════════════
    // 🔧 INICIALIZACIÓN
    // ═══════════════════════════════════════════════════════════════

    private ResourceManager() {
        // Constructor privado para singleton
    }

    /**
     * Obtener instancia singleton
     */
    public static ResourceManager get() {
        if (instance == null) {
            instance = new ResourceManager();
        }
        return instance;
    }

    /**
     * Inicializar con contexto (llamar desde onSurfaceCreated)
     */
    public void init(Context ctx) {
        if (initialized && this.context != null) {
            return;
        }
        this.context = ctx.getApplicationContext();
        this.initialized = true;
        Log.d(TAG, "🎨 ResourceManager inicializado");
    }

    /**
     * Verificar si está inicializado
     */
    public boolean isInitialized() {
        return initialized && context != null;
    }

    /**
     * 🧠 Configura el inSampleSize para texturas de recursos.
     * Propagado desde TextureManager según el tier de memoria.
     */
    public void setInSampleSize(int inSampleSize) {
        this.inSampleSize = Math.max(1, inSampleSize);
        Log.d(TAG, "🧠 inSampleSize configurado: " + this.inSampleSize);
    }

    // ═══════════════════════════════════════════════════════════════
    // 🖼️ TEXTURAS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Obtener textura por resource ID (carga bajo demanda)
     * Implementa TextureLoader para compatibilidad con código existente
     */
    @Override
    public int getTexture(int resourceId) {
        if (!initialized) {
            Log.e(TAG, "❌ ResourceManager no inicializado!");
            return 0;
        }

        Integer cachedId = textureCache.get(resourceId);
        if (cachedId != null) {
            return cachedId;
        }

        // Cargar textura
        int textureId = loadTextureInternal(resourceId);
        if (textureId != 0) {
            textureCache.put(resourceId, textureId);
            texturesLoaded++;
        }

        return textureId;
    }

    /**
     * Precargar una lista de texturas (útil para loading screens)
     */
    public void preloadTextures(int... resourceIds) {
        for (int resourceId : resourceIds) {
            getTexture(resourceId);
        }
        Log.d(TAG, "📦 Precargadas " + resourceIds.length + " texturas");
    }

    /**
     * Liberar una textura específica
     */
    public void releaseTexture(int resourceId) {
        Integer textureId = textureCache.remove(resourceId);
        if (textureId != null && textureId != 0) {
            GLES30.glDeleteTextures(1, new int[]{textureId}, 0);
            Log.d(TAG, "🗑️ Textura liberada: " + resourceId);
        }
    }

    private int loadTextureInternal(int resourceId) {
        final int[] handle = new int[1];
        GLES30.glGenTextures(1, handle, 0);

        if (handle[0] == 0) {
            Log.e(TAG, "❌ Error generando ID de textura");
            return 0;
        }

        // Decodificar bitmap con inSampleSize según tier de memoria
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = false;
        opts.inSampleSize = inSampleSize;
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), resourceId, opts);
        if (bmp == null) {
            Log.e(TAG, "❌ No se pudo decodificar recurso: " + resourceId);
            return 0;
        }

        // Convertir a ARGB_8888 si es necesario
        if (bmp.getConfig() != Bitmap.Config.ARGB_8888) {
            Bitmap converted = bmp.copy(Bitmap.Config.ARGB_8888, false);
            bmp.recycle();
            bmp = converted;
        }

        // Estimar memoria usada
        totalTextureMemory += bmp.getByteCount();

        // Subir a GPU
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, handle[0]);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bmp, 0);

        int width = bmp.getWidth();
        int height = bmp.getHeight();
        bmp.recycle();

        // Generar mipmaps
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);

        // Configurar filtros
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT);

        Log.d(TAG, "✓ Textura cargada: " + resourceId + " (" + width + "x" + height + ") → ID:" + handle[0]);

        return handle[0];
    }

    // ═══════════════════════════════════════════════════════════════
    // 🎨 SHADERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Compilar shader desde código fuente
     */
    public int compileShader(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);

        int[] status = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0);

        if (status[0] == 0) {
            String error = GLES30.glGetShaderInfoLog(shader);
            Log.e(TAG, "❌ Error compilando shader: " + error);
            GLES30.glDeleteShader(shader);
            return 0;
        }

        shadersCompiled++;
        return shader;
    }

    /**
     * Compilar shader desde asset (con cache)
     */
    public int compileShaderFromAsset(int type, String assetPath) {
        // Verificar cache
        Integer cached = shaderCache.get(assetPath);
        if (cached != null) {
            return cached;
        }

        String source = loadAssetAsString(assetPath);
        if (source == null) {
            return 0;
        }

        int shaderId = compileShader(type, source);
        if (shaderId != 0) {
            shaderCache.put(assetPath, shaderId);
        }

        return shaderId;
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔗 PROGRAMAS DE SHADER
    // ═══════════════════════════════════════════════════════════════

    /**
     * Crear programa desde código fuente
     */
    public int createProgram(String vertexSource, String fragmentSource) {
        int vs = compileShader(GLES30.GL_VERTEX_SHADER, vertexSource);
        int fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource);

        if (vs == 0 || fs == 0) {
            return 0;
        }

        int program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vs);
        GLES30.glAttachShader(program, fs);
        GLES30.glLinkProgram(program);

        int[] status = new int[1];
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, status, 0);

        if (status[0] == 0) {
            String error = GLES30.glGetProgramInfoLog(program);
            Log.e(TAG, "❌ Error enlazando programa: " + error);
            GLES30.glDeleteProgram(program);
            return 0;
        }

        // Limpiar shaders (ya están en el programa)
        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);

        programsLinked++;
        return program;
    }

    /**
     * Crear programa desde assets (con cache)
     */
    public int createProgramFromAssets(String vertexAssetPath, String fragmentAssetPath) {
        String cacheKey = vertexAssetPath + "|" + fragmentAssetPath;

        // Verificar cache
        Integer cached = programCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String vSrc = loadAssetAsString(vertexAssetPath);
        String fSrc = loadAssetAsString(fragmentAssetPath);

        if (vSrc == null || fSrc == null) {
            Log.e(TAG, "❌ No se pudo cargar shader: " + vertexAssetPath + " / " + fragmentAssetPath);
            return 0;
        }

        int programId = createProgram(vSrc, fSrc);
        if (programId != 0) {
            programCache.put(cacheKey, programId);
            Log.d(TAG, "✓ Programa creado: " + cacheKey.replace("|", " + "));
        }

        return programId;
    }

    /**
     * Liberar un programa específico
     */
    public void releaseProgram(String vertexAssetPath, String fragmentAssetPath) {
        String cacheKey = vertexAssetPath + "|" + fragmentAssetPath;
        Integer programId = programCache.remove(cacheKey);
        if (programId != null && programId != 0) {
            GLES30.glDeleteProgram(programId);
            Log.d(TAG, "🗑️ Programa liberado: " + cacheKey);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 📄 ASSETS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Cargar archivo de texto desde assets
     */
    public String loadAssetAsString(String assetPath) {
        if (context == null) {
            Log.e(TAG, "❌ Context es null!");
            return null;
        }

        StringBuilder sb = new StringBuilder();
        try (InputStream is = context.getAssets().open(assetPath);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {

            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();

        } catch (IOException e) {
            Log.e(TAG, "❌ Error leyendo asset: " + assetPath + " - " + e.getMessage());
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 🛠️ UTILIDADES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Crear FloatBuffer desde array
     */
    public static FloatBuffer createFloatBuffer(float[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(data).position(0);
        return fb;
    }

    /**
     * Obtener contexto (para clases legacy)
     */
    public Context getContext() {
        return context;
    }

    // ═══════════════════════════════════════════════════════════════
    // 📊 ESTADÍSTICAS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Imprimir estadísticas de uso
     */
    public void printStats() {
        Log.d(TAG, "╔══════════════════════════════════════════════╗");
        Log.d(TAG, "║         RESOURCE MANAGER STATS               ║");
        Log.d(TAG, "╠══════════════════════════════════════════════╣");
        Log.d(TAG, "║ Texturas cargadas: " + String.format("%-24d", texturesLoaded) + "║");
        Log.d(TAG, "║ Texturas en cache: " + String.format("%-24d", textureCache.size()) + "║");
        Log.d(TAG, "║ Shaders compilados: " + String.format("%-23d", shadersCompiled) + "║");
        Log.d(TAG, "║ Programas enlazados: " + String.format("%-22d", programsLinked) + "║");
        Log.d(TAG, "║ Programas en cache: " + String.format("%-23d", programCache.size()) + "║");
        Log.d(TAG, "║ Memoria texturas: " + String.format("%-22s", formatBytes(totalTextureMemory)) + "║");
        Log.d(TAG, "╚══════════════════════════════════════════════╝");
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024f);
        return String.format("%.1f MB", bytes / (1024f * 1024f));
    }

    // ═══════════════════════════════════════════════════════════════
    // 🗑️ LIMPIEZA
    // ═══════════════════════════════════════════════════════════════

    /**
     * Liberar todos los recursos
     */
    public void release() {
        // Liberar texturas
        for (Integer textureId : textureCache.values()) {
            if (textureId != 0) {
                GLES30.glDeleteTextures(1, new int[]{textureId}, 0);
            }
        }
        textureCache.clear();

        // Liberar programas
        for (Integer programId : programCache.values()) {
            if (programId != 0) {
                GLES30.glDeleteProgram(programId);
            }
        }
        programCache.clear();

        // Liberar shaders (por si quedaron)
        for (Integer shaderId : shaderCache.values()) {
            if (shaderId != 0) {
                GLES30.glDeleteShader(shaderId);
            }
        }
        shaderCache.clear();

        Log.d(TAG, "🧹 ResourceManager: todos los recursos liberados");
        printStats();

        // Reset estadísticas
        texturesLoaded = 0;
        shadersCompiled = 0;
        programsLinked = 0;
        totalTextureMemory = 0;
        initialized = false;
    }

    /**
     * Liberar solo recursos de una escena específica (para cambios de escena)
     */
    public void releaseSceneResources(int... textureResourceIds) {
        for (int resourceId : textureResourceIds) {
            releaseTexture(resourceId);
        }
        Log.d(TAG, "🧹 Liberados " + textureResourceIds.length + " recursos de escena");
    }

    /**
     * Reset singleton (para recreación completa)
     */
    public static void reset() {
        if (instance != null) {
            instance.release();
            instance = null;
        }
        Log.d(TAG, "🔄 ResourceManager reset");
    }
}
