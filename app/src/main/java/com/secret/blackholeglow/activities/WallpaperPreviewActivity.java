package com.secret.blackholeglow.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.opengl.GLSurfaceView;
import android.app.WallpaperManager; // Importa WallpaperManager
import androidx.appcompat.app.AppCompatActivity;

import com.secret.blackholeglow.LiveWallpaperService;
import com.secret.blackholeglow.SceneRenderer;
import com.secret.blackholeglow.R;

public class WallpaperPreviewActivity extends AppCompatActivity {

    private GLSurfaceView glSurfaceView;
    // (opcional) Si quieres mostrar preview de imagen
    // private ImageView imageViewPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Leer el id del recurso del wallpaper seleccionado
        int previewId = getIntent().getIntExtra("WALLPAPER_PREVIEW_ID", R.drawable.ic_launcher_background);
        String nombre_wallpaper = getIntent().getStringExtra("WALLPAPER_ID");
        Log.d("WallpaperPreviewActivity", "Wallpaper seleccionado: " + nombre_wallpaper);

        // Crear un LinearLayout para colocar tanto el GLSurfaceView como el botón
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);  // Establecemos la orientación vertical

        // Crear la vista GLSurfaceView para OpenGL
        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2); // OpenGL ES 2.0
        glSurfaceView.setRenderer(new SceneRenderer(this, nombre_wallpaper));
        // Si quieres mostrar un preview de imagen, descomenta esto:
        // imageViewPreview = new ImageView(this);
        // imageViewPreview.setImageResource(previewId);

        // Crear el botón para abrir la selección de Live Wallpaper
        Button setWallpaperButton = new Button(this);
        setWallpaperButton.setText("Establecer como fondo de pantalla");

        // Añadir el GLSurfaceView y el botón al layout
        layout.addView(glSurfaceView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));  // GLSurfaceView ocupa el espacio principal
        // layout.addView(imageViewPreview, new LinearLayout.LayoutParams(
        //         LinearLayout.LayoutParams.MATCH_PARENT, 400));
        layout.addView(setWallpaperButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Configurar el contenido de la actividad con el layout
        setContentView(layout);

        // Configurar el evento de clic para el botón
        setWallpaperButton.setOnClickListener(v -> {
            // 1️⃣ Almacenamos el nombre del wallpaper en preferencias
            getSharedPreferences("blackholeglow_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("selected_wallpaper", nombre_wallpaper)
                    .apply();
            // Crear la Intent para abrir el selector de Live Wallpaper
            Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    new android.content.ComponentName(this, LiveWallpaperService.class));

            startActivity(intent);
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();
    }
}
