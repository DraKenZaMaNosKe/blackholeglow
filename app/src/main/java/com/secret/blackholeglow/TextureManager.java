// TextureManager.java
package com.secret.blackholeglow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestor de texturas que permite cargar cualquier recurso
 * bajo demanda y cachearlo para reutilización posterior.
 */
public class TextureManager implements TextureLoader {

    private final Context context;
    private final Map<Integer,Integer> textureCache = new HashMap<>();
    private final Map<String, Integer> fileTextureCache = new HashMap<>();
    private boolean initialized = false;

    public TextureManager(Context ctx) {
        // Usamos el contexto de aplicación para evitar fugas
        this.context = ctx.getApplicationContext();
    }

    /**
     * Inicializa el gestor. Ya no carga ninguna textura
     * en concreto: solo marca que puede usarse.
     */
    public boolean initialize() {
        if (initialized) return true;
        initialized = true;
        Log.d("TextureManager", "Inicializado sin texturas precargadas.");
        return true;
    }

    /**
     * Devuelve el ID de OpenGL de la textura para resourceId.
     * Si no estaba en caché, la carga ahora y la cachea.
     */
    @Override
    public int getTexture(int resourceId) {
        if (!initialized) initialize();

        Integer texId = textureCache.get(resourceId);
        if (texId == null) {
            // Carga bajo demanda
            try {
                texId = ShaderUtils.loadTexture(context, resourceId);
                textureCache.put(resourceId, texId);
                Log.d("TextureManager", "Textura cargada y cacheada: resId="
                        + resourceId + " → texId=" + texId);
            } catch (RuntimeException e) {
                Log.e("TextureManager", "Error cargando textura resId=" + resourceId, e);
                return 0;
            }
        }
        return texId;
    }

    /**
     * 🔧 FIX: Libera todas las texturas cacheadas de la GPU.
     * Llamar cuando la escena se destruye o la app se cierra.
     */
    public void release() {
        int totalCount = textureCache.size() + fileTextureCache.size();
        if (totalCount == 0) return;

        Log.d("TextureManager", "🗑️ Liberando " + totalCount + " texturas...");

        // Liberar texturas de recursos
        for (Map.Entry<Integer, Integer> entry : textureCache.entrySet()) {
            int texId = entry.getValue();
            if (texId != 0) {
                int[] textures = {texId};
                GLES30.glDeleteTextures(1, textures, 0);
            }
        }

        // Liberar texturas de archivos
        for (Map.Entry<String, Integer> entry : fileTextureCache.entrySet()) {
            int texId = entry.getValue();
            if (texId != 0) {
                int[] textures = {texId};
                GLES30.glDeleteTextures(1, textures, 0);
            }
        }

        textureCache.clear();
        fileTextureCache.clear();
        initialized = false;
        Log.d("TextureManager", "✅ Todas las texturas liberadas");
    }

    /**
     * Devuelve el número de texturas en caché.
     */
    public int getCachedTextureCount() {
        return textureCache.size();
    }

    /**
     * Carga una textura desde un archivo en disco (para recursos descargados de Supabase).
     * Cachea la textura usando el path como clave.
     *
     * @param filePath Ruta absoluta al archivo de imagen
     * @return ID de textura OpenGL, o 0 si falla
     */
    public int loadTextureFromFile(String filePath) {
        if (!initialized) initialize();

        // Verificar cache primero
        Integer cached = fileTextureCache.get(filePath);
        if (cached != null && cached != 0) {
            return cached;
        }

        try {
            // Cargar bitmap desde archivo
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;  // No escalar
            Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);

            if (bitmap == null) {
                Log.e("TextureManager", "No se pudo cargar bitmap desde: " + filePath);
                return 0;
            }

            // Crear textura OpenGL
            int[] textureIds = new int[1];
            GLES30.glGenTextures(1, textureIds, 0);
            int texId = textureIds[0];

            if (texId == 0) {
                Log.e("TextureManager", "Error generando textura OpenGL");
                bitmap.recycle();
                return 0;
            }

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId);

            // Configurar parámetros de textura
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

            // Subir bitmap a GPU
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
            GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);

            // Liberar bitmap (ya está en GPU)
            bitmap.recycle();

            // Cachear
            fileTextureCache.put(filePath, texId);
            Log.d("TextureManager", "Textura cargada desde archivo: " + filePath + " → texId=" + texId);

            return texId;

        } catch (Exception e) {
            Log.e("TextureManager", "Error cargando textura desde archivo: " + filePath, e);
            return 0;
        }
    }
}
