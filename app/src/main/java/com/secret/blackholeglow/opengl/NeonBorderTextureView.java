// ╔════════════════════════════════════════════════════════════════════╗
// ║ 🌟 NeonBorderTextureView.java – Borde Neón (OpenGL ES) 🌟          ║
// ║                                                                    ║
// ║  ✨ Este componente extiende TextureView y forja un aura de neón   ║
// ║     tan poderosa como las flechas de Sagitario, iluminando cada   ║
// ║     elemento en tu lista con la energía cósmica de los Caballeros   ║
// ║     del Zodiaco.                                                  ║
// ╚════════════════════════════════════════════════════════════════════╝

package com.secret.blackholeglow.opengl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║ ⚔️ NeonBorderTextureView: Guardián del Resplandor                 ║
 * ║     • Actúa como un escudo transparente, para que solo el neón    ║
 * ║       sea visible, dejando el fondo de la UI intacto.            ║
 * ║     • Escucha los eventos del SurfaceTexture para encender o      ║
 * ║       apagar la forja del borde neón en su hilo dedicado.         ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
public class NeonBorderTextureView extends TextureView
        implements TextureView.SurfaceTextureListener {

    // ╔══════════════════════════════════════════════╗
    // ║ 🔧 rendererThread – Hilo de Combate            ║
    // ║   • Tipo: NeonBorderRendererThread             ║
    // ║   • Forja y ejecuta el shader neón en segundo    ║
    // ║     plano (EGL), protegiendo al main thread.    ║
    // ╚══════════════════════════════════════════════╝
    private NeonBorderRendererThread rendererThread;

    // ╔══════════════════════════════════════════════╗
    // ║ 🛡 Constructores                               ║
    // ╚══════════════════════════════════════════════╝
    /**
     * Invocado cuando instancias programáticamente.
     */
    public NeonBorderTextureView(Context context) {
        super(context);
        init();  // Configuración épica
    }

    /**
     * Invocado cuando inflas desde XML.
     */
    public NeonBorderTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();  // Configuración épica
    }

    // ╔══════════════════════════════════════════════╗
    // ║ ⚙️ init(): Ritual de Preparación              ║
    // ╚══════════════════════════════════════════════╝
    /**
     * 1️⃣ setOpaque(false): el campo de batalla se vuelve transparente,
     *    solo el neón resplandece sobre la UI.
     * 2️⃣ setSurfaceTextureListener(this): "Oye, SurfaceTexture,
     *    cuando estés listo avísame y encenderé el fuego neón".
     * 3️⃣ setWillNotDraw(false): habilita llamadas a draw(),
     *    aunque aquí no las usemos, es requisito de TextureView.
     */
    private void init() {
        setOpaque(false);
        setSurfaceTextureListener(this);
        setWillNotDraw(false);
    }

    // ╔══════════════════════════════════════════════╗
    // ║ 🌠 onSurfaceTextureAvailable(): Despertar del Neón ║
    // ╚══════════════════════════════════════════════╝
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface,
                                          int width, int height) {
        Log.d("NeonBorderTextureView",
                "🌀 SurfaceTexture listo: " + width + "x" + height);
        // 🛑 Validación: no renderizar si cualquier dimensión es cero
        if (width == 0 || height == 0) {
            Log.d("NeonBorderTextureView", "❌ Dimensiones inválidas, cancelando render.");
            return;
        }
        // 🌐 Crea Surface EGL desde SurfaceTexture
        Surface eglSurface = new Surface(surface);
        // 🚀 Comienza el hilo de render para el borde neón
        rendererThread = new NeonBorderRendererThread(
                eglSurface, width, height, getContext());
        rendererThread.start();
    }

    // ╔══════════════════════════════════════════════╗
    // ║ 🔄 onSurfaceTextureSizeChanged(): Ajuste Cósmico ║
    // ╚══════════════════════════════════════════════╝
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
                                            int width, int height) {
        // 🛠️ No reconfiguramos aquí: el viewport se fija en el hilo EGL.
    }

    // ╔══════════════════════════════════════════════╗
    // ║ 🧹 onSurfaceTextureDestroyed(): Apagado del Fénix ║
    // ╚══════════════════════════════════════════════╝
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d("NeonBorderTextureView",
                "🧹 SurfaceTexture destruido, deteniendo forja neón...");
        if (rendererThread != null) {
            // 🛡 Solicita cese del hilo y espera su fin limpio
            rendererThread.requestExitAndWait();
            rendererThread = null;
        }
        return true; // Liberar SurfaceTexture
    }

    // ╔══════════════════════════════════════════════╗
    // ║ 🔄 onSurfaceTextureUpdated(): Eco de la Luz    ║
    // ╚══════════════════════════════════════════════╝
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // ✨ Invocado tras update(): opcional, no usado aquí.
    }
}
