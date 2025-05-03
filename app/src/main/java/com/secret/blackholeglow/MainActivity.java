package com.secret.blackholeglow;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.opengl.GLSurfaceView;
import android.app.WallpaperManager; // Importa WallpaperManager
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private GLSurfaceView glSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Gatito.lineaseparadora();
        Log.d("MainActivity", "onCreate: ");
        super.onCreate(savedInstanceState);

        // Crear un LinearLayout para colocar tanto el GLSurfaceView como el botón
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);  // Establecemos la orientación vertical

        // Crear la vista GLSurfaceView para OpenGL
        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2); // OpenGL ES 2.0
        glSurfaceView.setRenderer(new SceneRenderer(this));

        // Crear el botón para abrir la selección de Live Wallpaper
        Button setWallpaperButton = new Button(this);
        Gatito.lineaseparadora();
        setWallpaperButton.setText("Establecer como fondo de pantalla");

        // Añadir el GLSurfaceView y el botón al layout
        layout.addView(glSurfaceView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));  // GLSurfaceView ocupa el espacio principal
        layout.addView(setWallpaperButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Configurar el contenido de la actividad con el layout
        setContentView(layout);

        // Configurar el evento de clic para el botón
        setWallpaperButton.setOnClickListener(v -> {
            // Crear la Intent para abrir el selector de Live Wallpaper
            Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            // Asegúrate de que el nombre del servicio del wallpaper esté correctamente configurado
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    new android.content.ComponentName(this, LiveWallpaperService.class));
            startActivity(intent);
        });
    }

    @Override
    protected void onPause() {
        Gatito.lineaseparadora();
        Log.d("MainActivity", "onPause: ");
        super.onPause();
        glSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        Gatito.lineaseparadora();
        Log.d("MainActivity", "onResume: ");
        super.onResume();
        glSurfaceView.onResume();
    }
}