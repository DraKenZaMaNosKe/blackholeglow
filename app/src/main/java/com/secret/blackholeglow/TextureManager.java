// TextureManager.java
package com.secret.blackholeglow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import com.secret.blackholeglow.core.DeviceProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestor de texturas que permite cargar cualquier recurso
 * bajo demanda y cachearlo para reutilización posterior.
 */
public class TextureManager implements TextureLoader {

    // ═══════════════════════════════════════════════════════════════
    // 🧠 MEMORY TIER — Adaptación automática según RAM del dispositivo
    // ═══════════════════════════════════════════════════════════════
    public enum MemoryTier {
        LOW,     // < 4 GB RAM → max 1024px, inSampleSize=2
        MEDIUM,  // 4-6 GB RAM → max 1536px, inSampleSize=1
        HIGH     // > 6 GB RAM → max 2048px, inSampleSize=1
    }

    private final Context context;
    private final Map<Integer,Integer> textureCache = new HashMap<>();
    private final Map<String, Integer> fileTextureCache = new HashMap<>();
    private boolean initialized = false;

    // Memory tier configuration
    private MemoryTier memoryTier = MemoryTier.MEDIUM;
    private int maxTextureDimension = 1536;
    private int defaultInSampleSize = 1;

    // 🛡️ FALLBACK: Textura de error (1x1 pixel magenta para debugging)
    private int fallbackTextureId = 0;
    private static final int FALLBACK_COLOR = 0xFFFF00FF;  // Magenta para detectar fácilmente

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

        // 🧠 Leer tier de memoria desde DeviceProfile (singleton central)
        DeviceProfile dp = DeviceProfile.get();
        memoryTier = dp.getMemoryTier();
        maxTextureDimension = dp.getMaxTextureDimension();
        defaultInSampleSize = dp.getDefaultInSampleSize();

        // 🛡️ Crear textura de fallback para errores
        createFallbackTexture();

        Log.d("TextureManager", "Inicializado - Memory tier: " + memoryTier
                + ", maxDim=" + maxTextureDimension + ", inSampleSize=" + defaultInSampleSize);
        return true;
    }

    /**
     * 🛡️ Crea una textura de 1x1 pixel como fallback para errores.
     * Evita que errores de carga dejen el GL en estado corrupto.
     */
    private void createFallbackTexture() {
        if (fallbackTextureId != 0) return;

        try {
            // Crear bitmap 2x2 (algunos drivers tienen problemas con 1x1)
            Bitmap fallbackBitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
            fallbackBitmap.eraseColor(FALLBACK_COLOR);

            int[] textureIds = new int[1];
            GLES30.glGenTextures(1, textureIds, 0);
            fallbackTextureId = textureIds[0];

            if (fallbackTextureId != 0) {
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, fallbackTextureId);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT);
                GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, fallbackBitmap, 0);
                Log.d("TextureManager", "🛡️ Textura de fallback creada (ID=" + fallbackTextureId + ")");
            }

            fallbackBitmap.recycle();
        } catch (Exception e) {
            Log.e("TextureManager", "Error creando textura de fallback: " + e.getMessage());
        }
    }

    /**
     * 🛡️ Obtiene la textura de fallback para usar cuando hay errores.
     */
    public int getFallbackTexture() {
        if (fallbackTextureId == 0) {
            createFallbackTexture();
        }
        return fallbackTextureId;
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
            // Carga bajo demanda con inSampleSize según tier de memoria
            try {
                texId = ShaderUtils.loadTexture(context, resourceId, defaultInSampleSize);
                textureCache.put(resourceId, texId);
                Log.d("TextureManager", "Textura cargada y cacheada: resId="
                        + resourceId + " → texId=" + texId + " (inSampleSize=" + defaultInSampleSize + ")");
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

        // 🛡️ Liberar textura de fallback
        if (fallbackTextureId != 0) {
            int[] textures = {fallbackTextureId};
            GLES30.glDeleteTextures(1, textures, 0);
            fallbackTextureId = 0;
            Log.d("TextureManager", "🛡️ Textura de fallback liberada");
        }

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
     * 🛡️ Si falla, retorna la textura de fallback en vez de 0.
     *
     * @param filePath Ruta absoluta al archivo de imagen
     * @return ID de textura OpenGL, o fallback si falla
     */
    public int loadTextureFromFile(String filePath) {
        if (!initialized) initialize();

        // Verificar cache primero
        Integer cached = fileTextureCache.get(filePath);
        if (cached != null && cached != 0) {
            return cached;
        }

        // 🛡️ VERIFICACIÓN: El archivo existe y es legible?
        java.io.File file = new java.io.File(filePath);
        if (!file.exists()) {
            Log.e("TextureManager", "🛡️ Archivo no existe: " + filePath);
            return getFallbackTexture();
        }
        if (!file.canRead()) {
            Log.e("TextureManager", "🛡️ Sin permiso de lectura: " + filePath);
            return getFallbackTexture();
        }
        if (file.length() == 0) {
            Log.e("TextureManager", "🛡️ Archivo vacío: " + filePath);
            return getFallbackTexture();
        }

        try {
            // 🧠 STEP 1: Decode bounds only to determine dimensions
            BitmapFactory.Options boundsOpts = new BitmapFactory.Options();
            boundsOpts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filePath, boundsOpts);

            int origWidth = boundsOpts.outWidth;
            int origHeight = boundsOpts.outHeight;

            // 🧠 STEP 2: Calculate inSampleSize to cap at maxTextureDimension
            int inSampleSize = 1;
            if (origWidth > maxTextureDimension || origHeight > maxTextureDimension) {
                int maxDim = Math.max(origWidth, origHeight);
                while (maxDim / inSampleSize > maxTextureDimension) {
                    inSampleSize *= 2;
                }
            }

            // 🔧 FIX MEMORY: Prefer RGB_565 (16-bit, 2 bytes/pixel) over ARGB_8888 (32-bit, 4 bytes/pixel).
            // For a 1024x1024 texture: 4 MB → 2 MB on GPU. Halves memory for opaque images.
            // The decoder automatically upgrades to ARGB_8888 if the image has an alpha channel (PNG with transparency).
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;  // No escalar
            options.inSampleSize = inSampleSize;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);

            if (inSampleSize > 1) {
                Log.d("TextureManager", "File texture downscaled: " + origWidth + "x" + origHeight
                        + " → " + (bitmap != null ? bitmap.getWidth() + "x" + bitmap.getHeight() : "null")
                        + " (inSampleSize=" + inSampleSize + ", maxDim=" + maxTextureDimension + ")");
            }

            if (bitmap == null) {
                Log.e("TextureManager", "🛡️ No se pudo decodificar: " + filePath);
                return getFallbackTexture();
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
            // 🧠 Sin mipmaps: file textures son fondos full-screen, no se escalan
            // Ahorra 33% de GPU memory por textura
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

            // Subir bitmap a GPU (sin glGenerateMipmap)
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);

            // Liberar bitmap (ya está en GPU)
            bitmap.recycle();

            // Cachear
            fileTextureCache.put(filePath, texId);
            Log.d("TextureManager", "Textura cargada desde archivo: " + filePath + " → texId=" + texId);

            return texId;

        } catch (Exception e) {
            Log.e("TextureManager", "🛡️ Error cargando textura: " + filePath + " - " + e.getMessage());
            return getFallbackTexture();
        }
    }

    /**
     * 🧠 Libera SOLO las texturas cargadas desde archivo (Supabase downloads).
     * Mantiene las texturas de recursos (drawable) en caché.
     * Llamar al cambiar de escena o bajo presión de memoria.
     */
    public void releaseFileTextures() {
        if (fileTextureCache.isEmpty()) return;

        int count = 0;
        List<String> keys = new ArrayList<>(fileTextureCache.keySet());
        for (String key : keys) {
            Integer texId = fileTextureCache.remove(key);
            if (texId != null && texId != 0) {
                int[] textures = {texId};
                GLES30.glDeleteTextures(1, textures, 0);
                count++;
            }
        }

        Log.d("TextureManager", "🧠 " + count + " file textures released (resource textures kept)");
    }

    /**
     * 🧠 Getters for memory tier configuration
     */
    public MemoryTier getMemoryTier() {
        return memoryTier;
    }

    public int getMaxTextureDimension() {
        return maxTextureDimension;
    }

    public int getDefaultInSampleSize() {
        return defaultInSampleSize;
    }

    /**
     * 🛡️ Verifica si un archivo está listo para ser cargado como textura.
     * Útil para verificar antes de intentar cargar.
     */
    public static boolean isFileReadable(String filePath) {
        if (filePath == null || filePath.isEmpty()) return false;
        java.io.File file = new java.io.File(filePath);
        return file.exists() && file.canRead() && file.length() > 0;
    }
}
