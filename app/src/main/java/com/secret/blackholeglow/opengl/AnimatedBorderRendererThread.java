package com.secret.blackholeglow.opengl;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLES30;
import android.util.Log;
import android.view.Surface;

import com.secret.blackholeglow.ShaderUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

/**
 * AnimatedBorderRendererThread
 * Maneja ciclo EGL y dibuja un quad fullscreen usando shaders dinámicos.
 * Ahora soporta ALPHA real gracias a blending habilitado y surface translúcido.
 */
public class AnimatedBorderRendererThread extends Thread {
    private final Surface surface;
    private final int width;
    private final int height;
    private final Context context;
    private final String vertexAsset;
    private final String fragmentAsset;
    private volatile boolean running = true;

    public AnimatedBorderRendererThread(
            Surface surface,
            int width,
            int height,
            Context context,
            String vertexAsset,
            String fragmentAsset) {
        this.surface       = surface;
        this.width         = width;
        this.height        = height;
        this.context       = context;
        this.vertexAsset   = vertexAsset;
        this.fragmentAsset = fragmentAsset;
    }

    public void requestExitAndWait() {
        running = false;
        interrupt();
        try {
            join();
        } catch (InterruptedException e) {
            Log.e("RendererThread", "Error al cerrar hilo", e);
        }
    }

    @Override
    public void run() {
        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        egl.eglInitialize(display, null);

        // 🔑 IMPORTANTE: Configuración RGBA8888 con alpha
        int[] configSpec = {
                EGL10.EGL_RENDERABLE_TYPE, 4,
                EGL10.EGL_RED_SIZE,        8,
                EGL10.EGL_GREEN_SIZE,      8,
                EGL10.EGL_BLUE_SIZE,       8,
                EGL10.EGL_ALPHA_SIZE,      8,  // aseguramos canal alpha
                EGL10.EGL_DEPTH_SIZE,      0,
                EGL10.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfig = new int[1];
        egl.eglChooseConfig(display, configSpec, configs, 1, numConfig);
        EGLConfig config = configs[0];

        int[] attribs = {0x3098, 2, EGL10.EGL_NONE};
        EGLContext eglContext = egl.eglCreateContext(
                display, config, EGL10.EGL_NO_CONTEXT, attribs);

        EGLSurface eglSurface = egl.eglCreateWindowSurface(
                display, config, surface, null);
        egl.eglMakeCurrent(display, eglSurface, eglSurface, eglContext);

        // 🔥 ACTIVAMOS BLENDING para usar el canal alpha del fragment shader
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // Compilación de shaders
        int program = ShaderUtils.createProgram(context, vertexAsset, fragmentAsset);
        GLES30.glUseProgram(program);

        int aPosition = GLES30.glGetAttribLocation(program, "a_Position");
        int uTime     = GLES30.glGetUniformLocation(program, "u_Time");
        int uRes      = GLES30.glGetUniformLocation(program, "u_Resolution");

        float[] quad = {-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f};
        FloatBuffer vb = ByteBuffer.allocateDirect(quad.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(quad);
        vb.position(0);

        long startTime = System.nanoTime();
        final double LOOP_DURATION = 60.0;

        while (running) {
            double elapsed = (System.nanoTime() - startTime) / 1_000_000_000.0;
            float phase = (float)((elapsed % LOOP_DURATION) / LOOP_DURATION);

            // Limpieza: usamos clear con alpha=0 para que el fondo quede TRANSPARENTE
            GLES30.glClearColor(0f, 0f, 0f, 0f);
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

            GLES30.glUseProgram(program);
            GLES30.glEnableVertexAttribArray(aPosition);
            GLES30.glVertexAttribPointer(aPosition, 2, GLES30.GL_FLOAT, false, 0, vb);

            GLES30.glUniform1f(uTime, phase);
            GLES30.glUniform2f(uRes, width, height);

            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
            GLES30.glDisableVertexAttribArray(aPosition);

            egl.eglSwapBuffers(display, eglSurface);
            try { Thread.sleep(16); } catch (InterruptedException ignored) {}
        }

        // 🔧 FIX: Limpiar shader program antes de destruir contexto
        GLES30.glDeleteProgram(program);

        egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE,
                EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
        egl.eglDestroySurface(display, eglSurface);
        egl.eglDestroyContext(display, eglContext);
        egl.eglTerminate(display);
        surface.release();
    }
}
