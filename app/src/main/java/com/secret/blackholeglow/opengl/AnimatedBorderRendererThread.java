// ╔══════════════════════════════════════════════════════════════════════╗
// ║ 🔥 AnimatedBorderRendererThread.java – Artífice de Efectos Dinámicos ║
// ║                                                                      ║
// ║  🎭 Este hilo inicializa EGL y compila shaders dinámicos para aplicar  ║
// ║     distintos marcos animados en cada ítem de la lista.             ║
// ╚══════════════════════════════════════════════════════════════════════╝

package com.secret.blackholeglow.opengl;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

/**
 * AnimatedBorderRendererThread maneja todo el ciclo de EGL y renderizado
 * OpenGL ES 2.0 usando un par de shaders definidos dinámicamente.
 */
public class AnimatedBorderRendererThread extends Thread {
    private final Surface surface;
    private final int width;
    private final int height;
    private final Context context;
    private final String vertexAsset;
    private final String fragmentAsset;
    private volatile boolean running = true;

    /**
     * @param surface        Superficie EGL donde se dibuja
     * @param width          Ancho del viewport
     * @param height         Alto del viewport
     * @param context        Contexto Android para cargar assets
     * @param vertexAsset    Ruta al shader de vértices en assets
     * @param fragmentAsset  Ruta al shader de fragmentos en assets
     */
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

    /**
     * Solicita el fin del bucle de dibujo y espera la limpieza EGL.
     */
    public void requestExitAndWait() {
        running = false;
        try {
            join();
        } catch (InterruptedException e) {
            Log.d("AnimatedBorderRendererThread", "Interrupted during join()", e);
        }
    }

    @Override
    public void run() {
        // 1️⃣ Inicializar EGL
        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        egl.eglInitialize(display, null);

        // 2️⃣ Elegir configuración RGBA8888
        int[] configSpec = {
                EGL10.EGL_RENDERABLE_TYPE, 4,
                EGL10.EGL_RED_SIZE,        8,
                EGL10.EGL_GREEN_SIZE,      8,
                EGL10.EGL_BLUE_SIZE,       8,
                EGL10.EGL_ALPHA_SIZE,      8,
                EGL10.EGL_DEPTH_SIZE,      0,
                EGL10.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfig = new int[1];
        egl.eglChooseConfig(display, configSpec, configs, 1, numConfig);
        EGLConfig config = configs[0];

        // 3️⃣ Contexto OpenGL ES 2.0
        int[] attribs = {0x3098, 2, EGL10.EGL_NONE};
        EGLContext eglContext = egl.eglCreateContext(
                display, config, EGL10.EGL_NO_CONTEXT, attribs);

        // 4️⃣ Crear superficie y hacerla current
        EGLSurface eglSurface = egl.eglCreateWindowSurface(
                display, config, surface, null);
        egl.eglMakeCurrent(display, eglSurface, eglSurface, eglContext);

        // 5️⃣ Compilar shaders dinámicos
        int program = ShaderUtils.createProgram(
                context, vertexAsset, fragmentAsset);
        GLES20.glUseProgram(program);

        // 6️⃣ Localizar atributos y uniforms básicos
        int aPosition = GLES20.glGetAttribLocation(program, "a_Position");
        int uTime     = GLES20.glGetUniformLocation(program, "u_Time");
        int uRes      = GLES20.glGetUniformLocation(program, "u_Resolution");

        // 7️⃣ Crear buffer de un quad fullscreen
        float[] quad = {-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f};
        FloatBuffer vb = ByteBuffer
                .allocateDirect(quad.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(quad);
        vb.position(0);

        long startTime = System.nanoTime();
        float t;

        // ╔══════════════════════════════════╗
        // ║ 🔁 Bucle de dibujado infinito      ║
        // ╚══════════════════════════════════╝
        while (running) {
            t = (System.nanoTime() - startTime) / 1_000_000_000f;
            if (t > Float.MAX_VALUE) startTime = System.nanoTime();

            GLES20.glClearColor(0f, 0f, 0f, 0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            GLES20.glUseProgram(program);
            GLES20.glEnableVertexAttribArray(aPosition);
            GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_FLOAT,
                    false, 0, vb);
            GLES20.glUniform1f(uTime, t);
            GLES20.glUniform2f(uRes, width, height);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            GLES20.glDisableVertexAttribArray(aPosition);

            egl.eglSwapBuffers(display, eglSurface);
            try { Thread.sleep(16); } catch (InterruptedException ignored) {}
        }

        // 8️⃣ Limpieza EGL
        egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE,
                EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
        egl.eglDestroySurface(display, eglSurface);
        egl.eglDestroyContext(display, eglContext);
        egl.eglTerminate(display);

        surface.release();
    }
}
