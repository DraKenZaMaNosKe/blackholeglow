package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class TextureManager implements TextureLoader {

    private final Context context;
    private final Map<Integer,Integer> textureCache = new HashMap<>();
    private boolean initialized = false;

    public TextureManager(Context ctx) {
        this.context = ctx.getApplicationContext();
    }

    public boolean initialize() {
        if (initialized) return true;
        try {
            int texId = ShaderUtils.loadTexture(context,
                    R.drawable.textura_universo_estrellado);
            textureCache.put(R.drawable.textura_universo_estrellado, texId);
            initialized = true;
            Log.d("TextureManager","Textures init: "+textureCache);
            return true;
        } catch (RuntimeException e) {
            Log.e("TextureManager","Error loading textures",e);
            return false;
        }
    }

    @Override
    public int getTexture(int resourceId) {
        if (!initialized) initialize();
        Integer id = textureCache.get(resourceId);
        return id!=null ? id : 0;
    }

    @Override
    public int getStarTexture() {
        return getTexture(R.drawable.star_glow);
    }
}
