package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Clase que gestiona la carga de texturas para toda la aplicación.
 * Implementa la interfaz TextureLoader para unificar la lógica.
 */
public class TextureManager implements TextureLoader {

    private final Context context;
    private final Map<Integer, Integer> textureCache = new HashMap<>();
    private boolean initialized = false;

    public TextureManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Inicializa todas las texturas necesarias si OpenGL está listo.
     * Este método se debe llamar desde onSurfaceCreated.
     */
    public void initialize() {
        if (initialized) return;
        try {
            textureCache.put(R.drawable.star_glow,
                    ShaderUtils.loadTexture(context, R.drawable.star_glow));
            // Aquí podrías cargar más texturas si quieres
            initialized = true;
        } catch (RuntimeException e) {
            Log.e("TextureManager", "❌ Error al inicializar texturas", e);
        }
    }

    /**
     * Método para obtener la textura. Si no está lista o no se pudo cargar, retorna 0.
     */
    @Override
    public int getTexture(int resourceId) {
        if (!initialized) return 0;
        Integer tex = textureCache.get(resourceId);
        return tex != null ? tex : 0;
    }

    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public int getStarTexture() {
        return getTexture(R.drawable.star_glow);
    }
}