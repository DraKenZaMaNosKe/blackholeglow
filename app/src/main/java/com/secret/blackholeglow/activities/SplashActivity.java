package com.secret.blackholeglow.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.secret.blackholeglow.R;

/*
╔══════════════════════════════════════════════════════════════════════════════╗
║                                                                              ║
║   🌅 SplashActivity.java – Activación Inicial y Puerta Cósmica del Proyecto 🌅 ║
║                                                                              ║
║   🤖🟰🤖 Martianito Android “Saga de Sagitario” 🟰🤖🤖                              ║
║      (•_•)      (•_•)                                                        ║
║      /︻▇████╗ ╔████▇︻\    ¡Que comience la gran aventura!                      ║
║                                                                              ║
║   🔍 Descripción General:                                                      ║
║     • Pantalla de carga que presenta el logo y la atmósfera cósmica.         ║
║     • Tras una breve espera, abre MainActivity y cierra esta Activity.       ║
║   📜 Inspirado en la flecha de Sagitario apuntando hacia el futuro del app.   ║
║                                                                              ║
╚══════════════════════════════════════════════════════════════════════════════╝
*/
public class SplashActivity extends AppCompatActivity {

    // ╔════════════════════════════════════════════════════════════════════╗
    // ║    🌟 onCreate: Punto de Entrada de la Actividad                ║
    // ╚════════════════════════════════════════════════════════════════════╝
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 🎨 Inflar layout: activity_splash.xml que contiene el logo y fondo animado
        setContentView(R.layout.activity_splash);

        // ╔════════════════════════════════════════════════════════════════════╗
        // ║    ⏳ Handler con Retardo de 2.5 segundos                          ║
        // ║    • Simula carga inicial y muestra animación de splash.         ║
        // ║    • Luego transiciona a MainActivity para iniciar la app.       ║
        // ╚════════════════════════════════════════════════════════════════════╝
        new Handler().postDelayed(() -> {
            // ➤ Crear Intent para navegar a la pantalla principal
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);   // 🚀 Lanzar MainActivity
            finish();                // 💫 Cerrar SplashActivity para no volver atrás
        }, 2500); // ⏱️ 2500 ms de espera (2.5 segundos)
    }
}
