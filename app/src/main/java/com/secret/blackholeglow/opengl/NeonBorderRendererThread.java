// ╔════════════════════════════════════════════════════════════════════╗
// ║ 🔥 NeonBorderRendererThread.java – Llama Cósmica del Neón 🔥      ║
// ║                                                                    ║
// ║  ⚔️ Hilo encargado de invocar los shaders de neón con la fuerza   ║
// ║     y disciplina de un caballero del zodiaco, manteniendo viva    ║
// ║     la energía en un loop sin fin, protegiendo la UI de bloqueos   ║
// ╚════════════════════════════════════════════════════════════════════╝

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
 * ╔═════════════════════════════════════════════════════════════╗
 * ║ ⚔️ NeonBorderRendererThread – Guerrero de Luz               ║
 * ║                                                           ║
 * ║  • Ejecuta el pipeline EGL y OpenGL ES 2.0 en un thread    ║
 * ║    separado para dibujar un borde neón animado.           ║
 * ╚═════════════════════════════════════════════════════════════╝
 */
public class NeonBorderRendererThread extends Thread {
    // ╔═════════════════════════════════╗
    // ║ 🛡 Variables de Batalla         ║
    // ╚═════════════════════════════════╝
    /** Superficie EGL creada desde SurfaceTexture */
    private final Surface surface;
    /** Ancho de la superficie en píxeles */
    private final int width;
    /** Alto de la superficie en píxeles */
    private final int height;
    /** Contexto Android para cargar shaders desde assets */
    private final Context context;
    /** Flag de control: mientras true, el hilo renderiza */
    private volatile boolean running = true;

    // ╔═════════════════════════════════╗
    // ║ 🏰 Constructor – Forja de Armadura ║
    // ╚═════════════════════════════════╝
    /**
     * @param surface EGL Surface creada a partir de TextureView
     * @param width   Ancho en píxeles
     * @param height  Alto en píxeles
     * @param context Contexto para acceder a recursos
     */
    public NeonBorderRendererThread(Surface surface, int width, int height, Context context) {
        this.surface = surface;
        this.width   = width;
        this.height  = height;
        this.context = context;
    }

    // ╔═════════════════════════════════╗
    // ║ 🛡 requestExitAndWait() – Retirada ║
    // ╚═════════════════════════════════╝
    /**
     * Señala al hilo que deje de ejecutar el loop y espera su finalización
     * para liberar recursos EGL con honor.
     */
    public void requestExitAndWait() {
        running = false;
        try {
            join();  // Espera a que run() termine
        } catch (InterruptedException e) {
            Log.d("NeonBorderRendererThread",
                    "⚔️ Hilo interrumpido durante join()", e);
        }
    }

    // ╔═════════════════════════════════╗
    // ║ 🎬 run() – Batalla Infinita      ║
    // ╚═════════════════════════════════╝
    @Override
    public void run() {
        // 1️⃣ Inicialización EGL (obtención de display+contexto)
        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        egl.eglInitialize(display, null);

        // 2️⃣ Especificar configuración RGBA8888, sin profundidad
        int[] configSpec = new int[]{
                EGL10.EGL_RENDERABLE_TYPE, 4,        // OpenGL ES 2.0
                EGL10.EGL_RED_SIZE,        8,
                EGL10.EGL_GREEN_SIZE,      8,
                EGL10.EGL_BLUE_SIZE,       8,
                EGL10.EGL_ALPHA_SIZE,      8,
                EGL10.EGL_DEPTH_SIZE,      0,
                EGL10.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] num_config = new int[1];
        egl.eglChooseConfig(display, configSpec, configs, 1, num_config);
        EGLConfig config = configs[0];

        // 3️⃣ Crear contexto OpenGL ES 2.0
        int[] attrib_list = { 0x3098, 2, // EGL_CONTEXT_CLIENT_VERSION=2
                EGL10.EGL_NONE };
        EGLContext eglContext = egl.eglCreateContext(
                display, config, EGL10.EGL_NO_CONTEXT, attrib_list);

        // 4️⃣ Crear y hacer current la superficie EGL de window
        EGLSurface eglSurface = egl.eglCreateWindowSurface(
                display, config, surface, null);
        egl.eglMakeCurrent(display, eglSurface, eglSurface, eglContext);

        // 5️⃣ Compilar y activar programa de shaders neón
        int program = ShaderUtils.createProgram(
                context,
                "shaders/neon_border_vertex.glsl",
                "shaders/neon_border_fragment.glsl"
        );
        GLES20.glUseProgram(program);

        // 6️⃣ Obtener ubicaciones de atributos y uniformes
        int aPosition   = GLES20.glGetAttribLocation(program, "a_Position");
        int uTime       = GLES20.glGetUniformLocation(program, "u_Time");
        int uResolution = GLES20.glGetUniformLocation(program, "u_Resolution");

        // 7️⃣ Crear buffer de vértices para un quad full-screen
        float[] vertices = new float[]{
                -1f, -1f,   1f, -1f,   -1f, 1f,   1f, 1f
        };
        FloatBuffer vertexBuffer = ByteBuffer
                .allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertices);
        vertexBuffer.position(0);

        // 8️⃣ Configurar temporizador de animación (loop cada 3 minutos)
        long   startTime = System.nanoTime();
        float  timeSec   = 0f;

        // ╔═════════════════════════════════╗
        // ║ 🔁 Bucle de Render – Energía eterna ║
        // ╚═════════════════════════════════╝
        while (running) {
            // Calcular tiempo transcurrido en segundos
            timeSec = (System.nanoTime() - startTime) / 1_000_000_000f;
            if (timeSec > 180f) { // Reinicio cada 3 minutos
                startTime = System.nanoTime();
                timeSec    = 0f;
            }

            // Limpiar con alpha=0 para evitar ghosting
            GLES20.glClearColor(0f, 0f, 0f, 0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            // Enviar datos al shader y dibujar
            GLES20.glUseProgram(program);
            GLES20.glEnableVertexAttribArray(aPosition);
            GLES20.glVertexAttribPointer(
                    aPosition, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
            GLES20.glUniform1f(uTime, timeSec);
            GLES20.glUniform2f(uResolution, width, height);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            GLES20.glDisableVertexAttribArray(aPosition);

            // Presentar el frame en pantalla
            egl.eglSwapBuffers(display, eglSurface);

            // Descanso breve para ~60 FPS
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                break;
            }
        }

        // ╔═════════════════════════════════╗
        // ║ 🛠️ Limpieza – Honor al Finalizar ║
        // ╚═════════════════════════════════╝
        egl.eglMakeCurrent(display,
                EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
        egl.eglDestroySurface(display, eglSurface);
        egl.eglDestroyContext(display, eglContext);
        egl.eglTerminate(display);

        // Liberar la superficie Android
        surface.release();
    }
}
