package com.secret.blackholeglow;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.secret.blackholeglow.core.WallpaperDirector;

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
    private WallpaperDirector director;  // Usar WallpaperDirector en lugar de SceneRenderer
    private ImageView exitButton;
    private Handler autoHideHandler;
    private Runnable autoHideRunnable;

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
        // ║    🎬 CREAR DIRECTOR CON ESCENA SELECCIONADA                      ║
        // ╚════════════════════════════════════════════════════════════════════╝

        // Leer escena seleccionada de SharedPreferences
        String selectedWallpaper = getSharedPreferences("blackholeglow_prefs", MODE_PRIVATE)
                .getString("selected_wallpaper", "Universo");

        Log.d(TAG, "🎨 Cargando escena: " + selectedWallpaper);

        // Usar WallpaperDirector (arquitectura de actores) en lugar de SceneRenderer
        director = new WallpaperDirector(this);
        director.setPreviewMode(true);  // Dream siempre en modo preview (directo al wallpaper)
        director.changeScene(selectedWallpaper);
        glSurfaceView.setRenderer(director);

        // Renderizado continuo para animaciones fluidas
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // ╔════════════════════════════════════════════════════════════════════╗
        // ║    📱 ESTABLECER VISTA COMO CONTENIDO DEL DREAM                   ║
        // ╚════════════════════════════════════════════════════════════════════╝

        // Crear un FrameLayout para contener el GLSurfaceView + botones
        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Agregar el GLSurfaceView como fondo
        rootLayout.addView(glSurfaceView);

        // ╔════════════════════════════════════════════════════════════════════╗
        // ║    🎨 BOTÓN CIRCULAR FLOTANTE ELEGANTE (AUTO-OCULTAR)             ║
        // ╚════════════════════════════════════════════════════════════════════╝

        Log.d(TAG, "🎨 Creando botón circular flotante elegante");

        // ═══════════════════════════════════════════════════════════════════
        // Crear botón circular con icono "X"
        // ═══════════════════════════════════════════════════════════════════
        exitButton = new ImageView(this);

        // Tamaño del botón en dp
        int buttonSizeDp = 48;
        int buttonSizePx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, buttonSizeDp,
                getResources().getDisplayMetrics()
        );

        // Crear drawable circular con gradiente
        GradientDrawable circleDrawable = new GradientDrawable();
        circleDrawable.setShape(GradientDrawable.OVAL);
        circleDrawable.setColors(new int[]{
                Color.argb(200, 30, 30, 35),   // Gris oscuro semi-transparente
                Color.argb(220, 20, 20, 25)    // Más oscuro en el borde
        });
        circleDrawable.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        circleDrawable.setGradientRadius(buttonSizePx / 2f);
        circleDrawable.setStroke(2, Color.argb(100, 255, 255, 255)); // Borde blanco sutil

        exitButton.setBackground(circleDrawable);

        // Configurar el texto "✕" como contenido
        exitButton.setScaleType(ImageView.ScaleType.CENTER);
        exitButton.setContentDescription("Salir");

        // Usar TextView dentro para el icono "✕"
        android.widget.TextView iconText = new android.widget.TextView(this);
        iconText.setText("✕");
        iconText.setTextColor(Color.WHITE);
        iconText.setTextSize(24);
        iconText.setGravity(Gravity.CENTER);

        // Crear FrameLayout para contener el icono
        FrameLayout buttonContainer = new FrameLayout(this);
        buttonContainer.setBackground(circleDrawable);
        buttonContainer.addView(iconText);

        // Posicionar en esquina superior derecha
        FrameLayout.LayoutParams buttonParams = new FrameLayout.LayoutParams(
                buttonSizePx,
                buttonSizePx
        );
        buttonParams.gravity = Gravity.TOP | Gravity.END;
        buttonParams.topMargin = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 24,
                getResources().getDisplayMetrics()
        );
        buttonParams.rightMargin = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 24,
                getResources().getDisplayMetrics()
        );
        buttonContainer.setLayoutParams(buttonParams);

        // ═══════════════════════════════════════════════════════════════════
        // Acción del botón: finalizar el Dream
        // ═══════════════════════════════════════════════════════════════════
        buttonContainer.setOnClickListener(v -> {
            Log.d(TAG, "🚪 Usuario presionó botón de salida");
            finish();
        });

        rootLayout.addView(buttonContainer);

        // ═══════════════════════════════════════════════════════════════════
        // Auto-ocultar el botón después de 3 segundos
        // ═══════════════════════════════════════════════════════════════════
        autoHideHandler = new Handler(Looper.getMainLooper());
        autoHideRunnable = () -> {
            Log.d(TAG, "🫥 Ocultando botón automáticamente");
            buttonContainer.animate()
                    .alpha(0f)
                    .setDuration(500)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            buttonContainer.setVisibility(View.GONE);
                        }
                    });
        };

        // Iniciar temporizador de auto-ocultar
        autoHideHandler.postDelayed(autoHideRunnable, 3000);

        // ═══════════════════════════════════════════════════════════════════
        // Tocar pantalla: mostrar botón de nuevo
        // ═══════════════════════════════════════════════════════════════════
        glSurfaceView.setOnClickListener(v -> {
            Log.d(TAG, "👆 Usuario tocó pantalla - Mostrando botón");

            // Cancelar auto-ocultar anterior
            autoHideHandler.removeCallbacks(autoHideRunnable);

            // Mostrar botón si está oculto
            if (buttonContainer.getVisibility() != View.VISIBLE) {
                buttonContainer.setVisibility(View.VISIBLE);
                buttonContainer.setAlpha(0f);
                buttonContainer.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .setListener(null);
            }

            // Reiniciar temporizador de auto-ocultar
            autoHideHandler.postDelayed(autoHideRunnable, 3000);
        });

        setContentView(rootLayout);

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
        if (director != null) {
            director.resume();
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
        if (director != null) {
            director.pause();
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

        if (director != null) {
            director.release();
            director = null;
        }
    }
}
