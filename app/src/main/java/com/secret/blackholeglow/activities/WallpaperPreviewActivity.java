// ╔════════════════════════════════════════════════════════════════════╗
// ║ 🖼️ WallpaperPreviewActivity.java – Vista del Santuario del Fondo    ║
// ║                                                                    ║
// ║  ✨ Esta Activity despliega una previsualización del live wallpaper  ║
// ║     y ofrece el rito para establecerlo como fondo animado.          ║
// ║  🌌 Inspirado en el templo de Athena, guía al usuario en el camino  ║
// ║     de elegir su protector cósmico (wallpaper).                   ║
// ╚════════════════════════════════════════════════════════════════════╝

package com.secret.blackholeglow.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.opengl.GLSurfaceView;
import android.app.WallpaperManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import com.secret.blackholeglow.LiveWallpaperService;
import com.secret.blackholeglow.SceneRenderer;
import com.secret.blackholeglow.WallpaperPreferences;
import com.secret.blackholeglow.R;

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
    // (opcional) Vista de imagen para preview estático:
    // private ImageView imageViewPreview;

    private SceneRenderer sceneRenderer;

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
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // 3️⃣ Forjar la GLSurfaceView del cosmos OpenGL
        sceneRenderer = new SceneRenderer(this, nombre_wallpaper);
        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2); // OpenGL ES 2.0
        glSurfaceView.setRenderer(sceneRenderer);
        // (opcional) Vista estática si no quieres renderizar 3D
        // imageViewPreview = new ImageView(this);
        // imageViewPreview.setImageResource(previewId);

        // 4️⃣ Botón para sellar el destino del wallpaper
        Button setWallpaperButton = new Button(this);
        setWallpaperButton.setText("Establecer como fondo de pantalla");

        // 5️⃣ Añadir vistas al layout: primer plano OpenGL y botón de invocación
        layout.addView(glSurfaceView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0, 1f)); // GLSurfaceView ocupa la mayor parte
        // layout.addView(imageViewPreview, ...); // imagen preview opcional
        layout.addView(setWallpaperButton,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        // 6️⃣ Renderizar el templo en pantalla
        setContentView(layout);

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
        if (sceneRenderer != null) {
            sceneRenderer.release();
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
        if (sceneRenderer != null) {
            sceneRenderer.resume();
        }
    }
}
