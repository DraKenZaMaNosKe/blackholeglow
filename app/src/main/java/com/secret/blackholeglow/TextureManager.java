// TextureManager.java
package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES30;
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
        if (textureCache.isEmpty()) return;

        Log.d("TextureManager", "🗑️ Liberando " + textureCache.size() + " texturas...");

        for (Map.Entry<Integer, Integer> entry : textureCache.entrySet()) {
            int texId = entry.getValue();
            if (texId != 0) {
                int[] textures = {texId};
                GLES30.glDeleteTextures(1, textures, 0);
            }
        }

        textureCache.clear();
        initialized = false;
        Log.d("TextureManager", "✅ Todas las texturas liberadas");
    }

    /**
     * Devuelve el número de texturas en caché.
     */
    public int getCachedTextureCount() {
        return textureCache.size();
    }
}
