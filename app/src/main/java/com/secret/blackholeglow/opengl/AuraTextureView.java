
package com.secret.blackholeglow.opengl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.Surface;

public class AuraTextureView extends TextureView implements TextureView.SurfaceTextureListener {

    private AuraRendererThread rendererThread;

    public AuraTextureView(Context context) {
        super(context);
        init();
    }

    public AuraTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setOpaque(false);
        setSurfaceTextureListener(this);
        setWillNotDraw(false);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Surface eglSurface = new Surface(surface); // 👈 Se convierte SurfaceTexture a Surface
        rendererThread = new AuraRendererThread(eglSurface, width, height, getContext());
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // No acción por ahora
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (rendererThread != null) {
            rendererThread.requestExitAndWait();
            rendererThread = null;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Nada por ahora
    }
}
