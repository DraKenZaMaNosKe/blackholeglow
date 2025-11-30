// ╔════════════════════════════════════════════════════════════════════╗
// ║ 🖼️ WallpaperPreviewActivity.java – Vista del Santuario del Fondo    ║
// ║                                                                    ║
// ║  ✨ Esta Activity despliega una previsualización del live wallpaper  ║
// ║     y ofrece el rito para establecerlo como fondo animado.          ║
// ║  🌌 Inspirado en el templo de Athena, guía al usuario en el camino  ║
// ║     de elegir su protector cósmico (wallpaper).                   ║
// ╚════════════════════════════════════════════════════════════════════╝

package com.secret.blackholeglow.activities;

import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.secret.blackholeglow.LiveWallpaperService;
import com.secret.blackholeglow.R;
import com.secret.blackholeglow.core.WallpaperDirector;
import com.secret.blackholeglow.WallpaperPreferences;

/**
 * ╔═════════════════════════════════════════════════════════════════╗
 * ║ 🌠 WallpaperPreviewActivity – Guardián de la Elección           ║
 * ║                                                               ║
 * ║  • Muestra cómo lucirá el live wallpaper antes de sellar tu   ║
 * ║    destino en tu pantalla principal.                          ║
 * ║  • Permite al Caballero del Usuario consagrar el wallpaper    ║
 * ║    escogido como protector eterno.                            ║
 * ╚═════════════════════════════════════════════════════════════════╝
 */
public class WallpaperPreviewActivity extends AppCompatActivity {

    // ════════════════════════════════
    // 🎨 Vistas y Variables de Estado
    // ════════════════════════════════
    /** GLSurfaceView para renderizar la escena OpenGL ES */
    private GLSurfaceView glSurfaceView;
    /** Nombre identificador del wallpaper seleccionado */
    private String nombre_wallpaper = "";
    private WallpaperDirector wallpaperDirector;

    /** Flag para detectar si el usuario fue a establecer el wallpaper */
    private boolean waitingForWallpaperResult = false;

    /** Container principal para mostrar mensajes */
    private FrameLayout rootContainer;

    // ╔════════════════════════════════════╗
    // ║ 🎬 onCreate(): Ritual de Invocación ║
    // ╚════════════════════════════════════╝
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🎨 Habilitar Edge-to-Edge (borde a borde)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // 1️⃣ Recuperar parámetros de la invocación
        int previewId = getIntent().getIntExtra(
                "WALLPAPER_PREVIEW_ID", R.drawable.ic_launcher_background);
        nombre_wallpaper = getIntent().getStringExtra("WALLPAPER_ID");
        Log.d("WallpaperPreviewActivity",
                "🌀 Wallpaper elegido: " + nombre_wallpaper);

        // 2️⃣ Construir layout dinámico: un santuario de visión
        rootContainer = new FrameLayout(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // 3️⃣ Forjar la GLSurfaceView del cosmos OpenGL
        wallpaperDirector = new WallpaperDirector(this);
        wallpaperDirector.changeScene(nombre_wallpaper);

        // 🎬 FORZAR MODO WALLPAPER para preview (no mostrar panel de control)
        wallpaperDirector.setPreviewMode(true);

        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2); // OpenGL ES 3.0
        glSurfaceView.setPreserveEGLContextOnPause(true);
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 24, 0);
        glSurfaceView.setRenderer(wallpaperDirector);

        // 4️⃣ Botón para sellar el destino del wallpaper
        Button setWallpaperButton = new Button(this);
        setWallpaperButton.setText("Establecer como fondo de pantalla");

        // 5️⃣ Añadir vistas al layout: primer plano OpenGL y botón de invocación
        layout.addView(glSurfaceView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0, 1f)); // GLSurfaceView ocupa la mayor parte
        layout.addView(setWallpaperButton,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        // Agregar layout al container principal
        rootContainer.addView(layout, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        // 6️⃣ Renderizar el templo en pantalla
        setContentView(rootContainer);

        // 🎨 Aplicar insets para que el contenido no quede tapado por las barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(layout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Aplicar padding solo abajo (donde está el botón)
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        // ╔════════════════════════════╗
        // ║ 🛡️ Botón: Consagrar Wallpaper ║
        // ╚════════════════════════════╝
        setWallpaperButton.setOnClickListener(v -> {
            // ⚙️ Guardar elección usando WallpaperPreferences (Firebase + SharedPreferences)
            WallpaperPreferences prefs = WallpaperPreferences.getInstance(this);

            prefs.setSelectedWallpaper(nombre_wallpaper, (success, message) -> {
                Log.d("WallpaperPreviewActivity", "Wallpaper guardado: " + message);

                // 📌 Marcar que estamos esperando el resultado
                waitingForWallpaperResult = true;

                // 📜 Crear intent para el selector de Live Wallpaper
                Intent intent = new Intent(
                        WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
                intent.putExtra(
                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        new android.content.ComponentName(
                                this, LiveWallpaperService.class));

                // 🚀 Lanzar el selector cósmico
                startActivity(intent);
            });
        });
    }

    // ╔════════════════════════════════════╗
    // ║ 🌜 onPause(): Silencio Temporal      ║
    // ╚════════════════════════════════════╝
    @Override
    protected void onPause() {
        super.onPause();
        // Detener renderizado OpenGL hasta retomar
        glSurfaceView.onPause();
        // ⚠️ NO llamar release() aquí - destruye las escenas modulares
        // Solo pausar el renderer
        if (wallpaperDirector != null) {
            wallpaperDirector.pause();
        }
    }

    // ╔════════════════════════════════════╗
    // ║ 🌞 onResume(): Renacer del Cosmos    ║
    // ╚════════════════════════════════════╝
    @Override
    protected void onResume() {
        super.onResume();
        // Reiniciar renderizado OpenGL
        glSurfaceView.onResume();

        // Recuperar el renderer actual y reiniciar tiempo
        if (wallpaperDirector != null) {
            wallpaperDirector.resume();
        }

        // 🎉 Verificar si el usuario acaba de aplicar el wallpaper
        if (waitingForWallpaperResult) {
            waitingForWallpaperResult = false;

            // Verificar si nuestro wallpaper está activo
            if (isOurWallpaperActive()) {
                showSuccessMessage();
            }
        }
    }

    // ╔════════════════════════════════════╗
    // ║ 💀 onDestroy(): Liberación Final    ║
    // ╚════════════════════════════════════╝
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Liberar recursos OpenGL cuando la Activity se destruye
        if (wallpaperDirector != null) {
            wallpaperDirector.release();
            wallpaperDirector = null;
        }
    }

    // ╔════════════════════════════════════════════════════════════════════╗
    // ║ ✅ Verificar si nuestro Live Wallpaper está activo                 ║
    // ╚════════════════════════════════════════════════════════════════════╝
    private boolean isOurWallpaperActive() {
        try {
            WallpaperManager wm = WallpaperManager.getInstance(this);
            WallpaperInfo info = wm.getWallpaperInfo();
            if (info != null) {
                String packageName = info.getPackageName();
                return packageName.equals(getPackageName());
            }
        } catch (Exception e) {
            Log.e("WallpaperPreviewActivity", "Error verificando wallpaper: " + e.getMessage());
        }
        return false;
    }

    // ╔════════════════════════════════════════════════════════════════════╗
    // ║ 🎉 Mostrar mensaje de éxito con animación                          ║
    // ╚════════════════════════════════════════════════════════════════════╝
    private void showSuccessMessage() {
        // Crear contenedor del mensaje
        LinearLayout messageContainer = new LinearLayout(this);
        messageContainer.setOrientation(LinearLayout.VERTICAL);
        messageContainer.setGravity(Gravity.CENTER);
        messageContainer.setPadding(60, 40, 60, 40);

        // Fondo con gradiente y bordes redondeados
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(40f);
        background.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        background.setColors(new int[]{
                Color.parseColor("#1A1A2E"),  // Azul oscuro
                Color.parseColor("#16213E")   // Azul más oscuro
        });
        background.setStroke(3, Color.parseColor("#00D9FF"));  // Borde cyan
        messageContainer.setBackground(background);

        // Icono de check
        TextView iconView = new TextView(this);
        iconView.setText("✓");
        iconView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 60);
        iconView.setTextColor(Color.parseColor("#00FF88"));  // Verde brillante
        iconView.setGravity(Gravity.CENTER);
        messageContainer.addView(iconView);

        // Título
        TextView titleView = new TextView(this);
        titleView.setText("Wallpaper Aplicado");
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        titleView.setTextColor(Color.WHITE);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 20, 0, 10);
        messageContainer.addView(titleView);

        // Subtítulo con nombre del wallpaper
        TextView subtitleView = new TextView(this);
        subtitleView.setText(nombre_wallpaper);
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        subtitleView.setTextColor(Color.parseColor("#00D9FF"));  // Cyan
        subtitleView.setGravity(Gravity.CENTER);
        messageContainer.addView(subtitleView);

        // Mensaje adicional
        TextView infoView = new TextView(this);
        infoView.setText("Tu fondo de pantalla está listo");
        infoView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        infoView.setTextColor(Color.parseColor("#888888"));
        infoView.setGravity(Gravity.CENTER);
        infoView.setPadding(0, 20, 0, 0);
        messageContainer.addView(infoView);

        // Configurar posición centrada
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        messageContainer.setLayoutParams(params);

        // Agregar al root container
        rootContainer.addView(messageContainer);

        // Animación de entrada (fade in)
        messageContainer.setAlpha(0f);
        messageContainer.animate()
                .alpha(1f)
                .setDuration(300)
                .start();

        // Auto cerrar después de 2 segundos
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Animación de salida (fade out)
            messageContainer.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        // Volver al catálogo
                        finish();
                    })
                    .start();
        }, 2000);
    }
}
