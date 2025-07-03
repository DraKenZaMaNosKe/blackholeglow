// ╔══════════════════════════════════════════════════════════════════════╗
// ║ 🌈 AnimatedBorderTextureView.java – Lienzo de Marcas Animadas       ║
// ║                                                                      ║
// ║  📌 Este componente extiende TextureView para renderizar, usando      ║
// ║     OpenGL ES, un borde animado configurable alrededor de cada ítem.  ║
// ║  🎨 Permite asignar dinámicamente pares de shaders GLSL distintos      ║
// ║     (vertex + fragment) para lograr múltiples efectos visuales.      ║
// ╚══════════════════════════════════════════════════════════════════════╝

package com.secret.blackholeglow.opengl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

/**
 * AnimatedBorderTextureView: gestiona la creación de una superficie EGL
 * y el hilo de renderizado AnimatedBorderRendererThread con shaders
 * específicos para cada item de lista.
 */
public class AnimatedBorderTextureView extends TextureView
        implements TextureView.SurfaceTextureListener {

    // Ruta al shader de vértices (assets)
    private String vertexShaderAsset   = "shaders/neon_border_vertex.glsl";
    // Ruta al shader de fragmentos (assets)
    private String fragmentShaderAsset = "shaders/neon_border_fragment.glsl";

    private AnimatedBorderRendererThread rendererThread;

    public AnimatedBorderTextureView(Context context) {
        super(context);
        init();
    }

    public AnimatedBorderTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    // Inicializa listeners y transparencia de fondo
    private void init() {
        setOpaque(false);
        setSurfaceTextureListener(this);
        setWillNotDraw(false);
    }

    /**
     * Define los archivos GLSL a usar para este borde animado.
     * Debe llamarse antes de que la vista esté disponible.
     * @param vertexAssetPath   Ruta en assets al shader de vértices
     * @param fragmentAssetPath Ruta en assets al shader de fragmentos
     */
    public void setShaderAssets(String vertexAssetPath, String fragmentAssetPath) {
        this.vertexShaderAsset   = vertexAssetPath;
        this.fragmentShaderAsset = fragmentAssetPath;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d("AnimatedBorderTextureView", "SurfaceTexture available: " + width + "x" + height);
        if (width <= 0 || height <= 0) return;
        Surface eglSurface = new Surface(surface);
        rendererThread = new AnimatedBorderRendererThread(
                eglSurface, width, height, getContext(),
                vertexShaderAsset, fragmentShaderAsset
        );
        rendererThread.start();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // El renderer fija su propio viewport, no se reconfigura aquí
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d("AnimatedBorderTextureView", "SurfaceTexture destroyed, stopping renderer...");
        if (rendererThread != null) {
            rendererThread.requestExitAndWait();
            rendererThread = null;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // No se usa directamente
    }
}
