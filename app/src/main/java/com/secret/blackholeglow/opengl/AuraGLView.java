package com.secret.blackholeglow.opengl;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;



public class AuraGLView extends GLSurfaceView {

    private final AuraRenderer renderer;

    public AuraGLView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        getHolder().setFormat(PixelFormat.TRANSLUCENT); // transparencia habilitada
        setZOrderOnTop(false); // permite dibujar encima de otras vistas
        renderer = new AuraRenderer(context);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        Log.d("AuraGLView", "GLView creada con transparencia");
    }

    public AuraGLView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setZOrderOnTop(true);
        renderer = new AuraRenderer(context);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        Log.d("AuraGLView", "GLView (con atributos) creada con transparencia");
    }
}