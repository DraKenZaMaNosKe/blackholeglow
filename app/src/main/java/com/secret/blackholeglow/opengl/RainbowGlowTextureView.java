package com.secret.blackholeglow.opengl;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;

public class RainbowGlowTextureView extends TextureView implements TextureView.SurfaceTextureListener {
    private com.secret.blackholeglow.opengl.RainbowGlowRendererThread rendererThread;

    public RainbowGlowTextureView(Context context) {
        super(context);
        init();
    }

    public RainbowGlowTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(android.graphics.SurfaceTexture surface, int width, int height) {
        rendererThread = new com.secret.blackholeglow.opengl.RainbowGlowRendererThread(new Surface(surface), width, height, getContext());
        rendererThread.start();
    }

    @Override
    public void onSurfaceTextureSizeChanged(android.graphics.SurfaceTexture surface, int width, int height) {}

    @Override
    public boolean onSurfaceTextureDestroyed(android.graphics.SurfaceTexture surface) {
        if (rendererThread != null) {
            rendererThread.requestExitAndWait();
            rendererThread = null;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(android.graphics.SurfaceTexture surface) {}
}
