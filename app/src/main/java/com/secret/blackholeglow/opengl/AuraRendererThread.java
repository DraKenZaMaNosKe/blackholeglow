package com.secret.blackholeglow.opengl;

import android.content.Context;
import android.opengl.GLES20;
import android.view.Surface;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.*;

public class AuraRendererThread extends Thread {

    private final Surface surface;
    private final int width;
    private final int height;
    private final Context context;
    private volatile boolean running = true;

    public AuraRendererThread(Surface surface, int width, int height, Context context) {
        this.surface = surface;
        this.width = width;
        this.height = height;
        this.context = context;
    }

    public void requestExitAndWait() {
        running = false;
        try {
            join();
        } catch (InterruptedException e) {
            Log.e("AuraRendererThread", "Thread interrupted during join()", e);
        }
    }

    @Override
    public void run() {
        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        egl.eglInitialize(display, null);

        int[] configSpec = {
                EGL10.EGL_RENDERABLE_TYPE, 4, // EGL_OPENGL_ES2_BIT
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 0,
                EGL10.EGL_NONE
        };

        EGLConfig[] configs = new EGLConfig[1];
        int[] num_config = new int[1];
        egl.eglChooseConfig(display, configSpec, configs, 1, num_config);
        EGLConfig config = configs[0];

        int[] attrib_list = {
                0x3098, 2, // EGL_CONTEXT_CLIENT_VERSION
                EGL10.EGL_NONE
        };

        EGLContext eglContext = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, attrib_list);
        EGLSurface eglSurface = egl.eglCreateWindowSurface(display, config, surface, null);
        egl.eglMakeCurrent(display, eglSurface, eglSurface, eglContext);

        int program = ShaderUtils.createProgram(context, "shaders/aura_vertex.glsl", "shaders/aura_fragment.glsl");
        GLES20.glUseProgram(program);

        int aPosition = GLES20.glGetAttribLocation(program, "a_Position");
        int uTime = GLES20.glGetUniformLocation(program, "u_Time");
        int uResolution = GLES20.glGetUniformLocation(program, "u_Resolution");

        float[] vertices = {
            -1f, -1f,
             1f, -1f,
            -1f,  1f,
             1f,  1f
        };

        FloatBuffer vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(vertices).position(0);

        long startTime = System.nanoTime();

        while (running) {
            float time = (System.nanoTime() - startTime) / 1_000_000_000f;

            GLES20.glViewport(0, 0, width, height);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            GLES20.glUseProgram(program);
            GLES20.glEnableVertexAttribArray(aPosition);
            GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

            GLES20.glUniform1f(uTime, time);
            GLES20.glUniform2f(uResolution, width, height);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

            GLES20.glDisableVertexAttribArray(aPosition);

            egl.eglSwapBuffers(display, eglSurface);

            try {
                Thread.sleep(16); // ~60fps
            } catch (InterruptedException e) {
                break;
            }
        }

        egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
        egl.eglDestroySurface(display, eglSurface);
        egl.eglDestroyContext(display, eglContext);
        egl.eglTerminate(display);

        surface.release();
    }
}
