// TextureManager.java
package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
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
     * Conveniencia para texturitas de “estrella”
     */
    @Override
    public int getStarTexture() {
        return getTexture(R.drawable.star_glow);
    }
}
