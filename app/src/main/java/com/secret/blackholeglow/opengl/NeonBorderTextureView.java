package com.secret.blackholeglow.opengl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

public class NeonBorderTextureView extends TextureView implements TextureView.SurfaceTextureListener {
    private NeonBorderRendererThread rendererThread;

    public NeonBorderTextureView(Context context) {
        super(context);
        init();
    }

    public NeonBorderTextureView(Context context, AttributeSet attrs) {
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
        Log.d("NeonBorderTextureView", "🌀 SurfaceTexture disponible: " + width + "x" + height);
        if (width == 0 || height == 0) {
            Log.e("NeonBorderTextureView", "❌ TAMAÑO CERO, no se puede renderizar.");
            return;
        }
        Surface eglSurface = new Surface(surface);
        rendererThread = new NeonBorderRendererThread(eglSurface, width, height, getContext());
        rendererThread.start();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d("NeonBorderTextureView", "🧹 SurfaceTexture destruido, matando thread...");
        if (rendererThread != null) {
            rendererThread.requestExitAndWait();
            rendererThread = null;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
}
