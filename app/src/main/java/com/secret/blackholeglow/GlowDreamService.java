package com.secret.blackholeglow;

import android.opengl.GLSurfaceView;
import android.service.dreams.DreamService;
import android.util.Log;
import android.view.View;

/*
╔══════════════════════════════════════════════════════════════════════════════╗
║                                                                              ║
║   🌌 GlowDreamService.java – Protector de Pantalla Galáctico                ║
║                                                                              ║
║   ✨ "Mientras descansa, el universo sigue girando" ✨                        ║
║      🔋 Se activa automáticamente durante la carga                           ║
║      🌟 Muestra el wallpaper animado de OpenGL                               ║
║      ⚡ Daydream/Screen Saver oficial de Android                             ║
║                                                                              ║
║   🔍 Descripción General:                                                    ║
║     • DreamService que extiende la funcionalidad de Android                 ║
║     • Se activa cuando el dispositivo está:                                 ║
║       - Conectado al cargador (cargando)                                    ║
║       - En reposo/inactivo                                                  ║
║       - En un dock/soporte                                                  ║
║     • Reutiliza SceneRenderer para mostrar escenas 3D                       ║
║     • Interactivo (puede responder a toques) o modo observación             ║
║                                                                              ║
║   🎨 Características:                                                        ║
║     • OpenGL ES 2.0 con GLSurfaceView                                       ║
║     • Pantalla completa sin barra de estado                                 ║
║     • Usa la escena seleccionada en SharedPreferences                       ║
║     • Modo interactivo habilitado para permitir toques                      ║
║                                                                              ║
║   📱 Configuración del Usuario:                                             ║
║     Ajustes → Pantalla → Protector de pantalla → Black Hole Glow           ║
║                                                                              ║
╚══════════════════════════════════════════════════════════════════════════════╝
*/
public class GlowDreamService extends DreamService {

    private static final String TAG = "GlowDreamService";

    private GLSurfaceView glSurfaceView;
    private SceneRenderer renderer;

    // ╔════════════════════════════════════════════════════════════════════╗
    // ║    🌟 onAttachedToWindow: Configuración inicial del Dream         ║
    // ╚════════════════════════════════════════════════════════════════════╝
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        Log.d(TAG, "🌌 Iniciando Daydream - Black Hole Glow");

        // ╔════════════════════════════════════════════════════════════════════╗
        // ║    🎨 CONFIGURACIÓN DEL DREAM                                     ║
        // ╚════════════════════════════════════════════════════════════════════╝

        // Modo interactivo: permite toques en la pantalla
        setInteractive(true);

        // Pantalla completa: oculta barra de estado y navegación
        setFullscreen(true);

        // Mantener pantalla encendida mientras el Dream está activo
        setScreenBright(true);

        // ╔════════════════════════════════════════════════════════════════════╗
        // ║    🪐 CREAR GLSurfaceView CON ESCENA 3D                           ║
        // ╚════════════════════════════════════════════════════════════════════╝

        glSurfaceView = new GLSurfaceView(this);

        // Configurar OpenGL ES 2.0
        glSurfaceView.setEGLContextClientVersion(2);

        // Configurar fondo transparente (para efectos de blend)
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        glSurfaceView.getHolder().setFormat(android.graphics.PixelFormat.TRANSLUCENT);
        glSurfaceView.setZOrderOnTop(false);

        // ╔════════════════════════════════════════════════════════════════════╗
        // ║    🎬 CREAR RENDERER CON ESCENA SELECCIONADA                      ║
        // ╚════════════════════════════════════════════════════════════════════╝

        // Leer escena seleccionada de SharedPreferences
        String selectedWallpaper = getSharedPreferences("blackholeglow_prefs", MODE_PRIVATE)
                .getString("selected_wallpaper", "Universo");

        Log.d(TAG, "🎨 Cargando escena: " + selectedWallpaper);

        renderer = new SceneRenderer(this, selectedWallpaper);
        glSurfaceView.setRenderer(renderer);

        // Renderizado continuo para animaciones fluidas
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // ╔════════════════════════════════════════════════════════════════════╗
        // ║    📱 ESTABLECER VISTA COMO CONTENIDO DEL DREAM                   ║
        // ╚════════════════════════════════════════════════════════════════════╝

        setContentView(glSurfaceView);

        Log.d(TAG, "✅ Daydream iniciado correctamente");
    }

    // ╔════════════════════════════════════════════════════════════════════╗
    // ║    🎬 onDreamingStarted: Cuando el Dream comienza a mostrarse     ║
    // ╚════════════════════════════════════════════════════════════════════╝
    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        Log.d(TAG, "🌟 Dream comenzó - Wallpaper animado activo");

        // Reanudar renderizado si estaba pausado
        if (glSurfaceView != null) {
            glSurfaceView.onResume();
        }
        if (renderer != null) {
            renderer.resume();
        }
    }

    // ╔════════════════════════════════════════════════════════════════════╗
    // ║    ⏸️ onDreamingStopped: Cuando el Dream se detiene              ║
    // ╚════════════════════════════════════════════════════════════════════╝
    @Override
    public void onDreamingStopped() {
        super.onDreamingStopped();
        Log.d(TAG, "⏸️ Dream detenido - Usuario activo o cargador desconectado");

        // Pausar renderizado para ahorrar recursos
        if (renderer != null) {
            renderer.pause();
        }
        if (glSurfaceView != null) {
            glSurfaceView.onPause();
        }
    }

    // ╔════════════════════════════════════════════════════════════════════╗
    // ║    🗑️ onDetachedFromWindow: Limpieza de recursos                 ║
    // ╚════════════════════════════════════════════════════════════════════╝
    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d(TAG, "🗑️ Limpiando recursos del Dream");

        // Limpiar OpenGL
        if (glSurfaceView != null) {
            glSurfaceView.onPause();
            glSurfaceView = null;
        }

        if (renderer != null) {
            renderer = null;
        }
    }
}
