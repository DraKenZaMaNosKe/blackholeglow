package com.secret.blackholeglow.opengl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

/*
╔════════════════════════════════════════════════════════════════════╗
║ 🌟 NeonBorderTextureView.java – Borde Neón (OpenGL ES) 🌟          ║
║                                                                    ║
║  ✨ Este componente extiende TextureView para renderizar un marco   ║
║     animado de neón alrededor de cada tarjeta en la lista.         ║
║  🌌 Inspirado en la energía cósmica de Saint Seiya y el poder de    ║
║     Sagitario disparando flechas luminosas.                        ║
╚════════════════════════════════════════════════════════════════════╝
*/
public class NeonBorderTextureView extends TextureView
        implements TextureView.SurfaceTextureListener {

    // ╔══════════════════════════════════════════════╗
    // ║ 🔧 Variable Miembro: rendererThread         ║
    // ║   • NeonBorderRendererThread: hilo dedicado ║
    // ║     a ejecutar el shader de frontera neón. ║
    // ╚══════════════════════════════════════════════╝
    private NeonBorderRendererThread rendererThread;

    // ╔══════════════════════════════════════════════╗
    // ║ 🛡 Constructores                             ║
    // ╚══════════════════════════════════════════════╝
    /**
     * Constructor usado al instanciar desde código.
     */
    public NeonBorderTextureView(Context context) {
        super(context);
        init();  // inicialización común
    }

    /**
     * Constructor usado al inflar desde XML.
     */
    public NeonBorderTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();  // inicialización común
    }

    // ╔══════════════════════════════════════════════╗
    // ║ ⚙️ init(): Configuración Inicial           ║
    // ╚══════════════════════════════════════════════╝
    /**
     * • setOpaque(false): fondo transparente para que
     *   solo se vea el shader de neón.
     * • setSurfaceTextureListener: escucha eventos
     *   de disponibilidad del SurfaceTexture.
     * • setWillNotDraw(false): permite que se invoque draw().
     */
    private void init() {
        setOpaque(false);
        setSurfaceTextureListener(this);
        setWillNotDraw(false);
    }

    // ╔══════════════════════════════════════════════╗
    // ║ 🌠 onSurfaceTextureAvailable(): Inicio Render ║
    // ╚══════════════════════════════════════════════╝
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface,
                                          int width, int height) {
        Log.d("NeonBorderTextureView",
                "🌀 SurfaceTexture disponible: " + width + "x" + height);
        // ❌ Validar tamaño
        if (width == 0 || height == 0) {
            Log.e("NeonBorderTextureView", "❌ TAMAÑO CERO, no renderizar.");
            return;
        }
        // 🌐 Crear superficie EGL a partir del SurfaceTexture
        Surface eglSurface = new Surface(surface);
        // 🚀 Iniciar hilo de render con dimensiones y contexto
        rendererThread = new NeonBorderRendererThread(
                eglSurface, width, height, getContext());
        rendererThread.start();
    }

    // ╔══════════════════════════════════════════════╗
    // ║ 🔄 onSurfaceTextureSizeChanged(): Opcional   ║
    // ╚══════════════════════════════════════════════╝
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
                                            int width, int height) {
        // No implementado: tamaño de viewport fijo en este diseño
    }

    // ╔══════════════════════════════════════════════╗
    // ║ 🧹 onSurfaceTextureDestroyed(): Limpieza      ║
    // ╚══════════════════════════════════════════════╝
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d("NeonBorderTextureView", "🧹 SurfaceTexture destruido, deteniendo hilo...");
        if (rendererThread != null) {
            // Solicitar fin y esperar terminación segura
            rendererThread.requestExitAndWait();
            rendererThread = null;
        }
        return true;  // libera el SurfaceTexture
    }

    // ╔══════════════════════════════════════════════╗
    // ║ 🔄 onSurfaceTextureUpdated(): No necesario   ║
    // ╚══════════════════════════════════════════════╝
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Invocado cuando la textura ha sido actualizada (opcional)
    }
}
