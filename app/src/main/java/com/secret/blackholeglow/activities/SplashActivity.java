package com.secret.blackholeglow.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.secret.blackholeglow.LoginActivity;
import com.secret.blackholeglow.MusicPermissionActivity;
import com.secret.blackholeglow.R;
import com.secret.blackholeglow.UserManager;

/*
╔══════════════════════════════════════════════════════════════════════════════╗
║                                                                              ║
║   🌅 SplashActivity.java – Experiencia Mágica de Inicio                     ║
║                                                                              ║
║   🌌 "Cada apertura es una puerta al cosmos" 🌌                              ║
║      ✨ Animaciones hermosas ✨ Navegación inteligente ✨ 3 segundos mágicos ║
║                                                                              ║
║   🔍 Descripción General:                                                    ║
║     • Splash screen animado que se muestra CADA VEZ que se abre la app     ║
║     • Logo con fade-in, scale y efecto de pulso continuo                   ║
║     • Fondo de estrellas con animación de parpadeo                         ║
║     • Texto animado con entrada desde abajo                                ║
║     • Duración: 3 segundos                                                 ║
║     • Navegación inteligente según estado de login                         ║
║                                                                              ║
║   🎨 Efectos Visuales:                                                       ║
║     1. Fondo: Gradiente cósmico + estrellas parpadeantes                  ║
║     2. Logo: Fade-in con scale + pulso continuo                           ║
║     3. Texto: Fade-in con translate hacia arriba                          ║
║     4. ProgressBar: Indicador de carga elegante                           ║
║                                                                              ║
╚══════════════════════════════════════════════════════════════════════════════╝
*/
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DURATION = 3000; // 🎯 3 segundos mágicos

    // ╔════════════════════════════════════════════════════════════════════╗
    // ║    📱 Referencias a Elementos de UI                                ║
    // ╚════════════════════════════════════════════════════════════════════╝
    private ImageView logo;
    private TextView appName;

    // ╔════════════════════════════════════════════════════════════════════╗
    // ║    🌟 onCreate: Punto de Entrada de la Experiencia Mágica        ║
    // ╚════════════════════════════════════════════════════════════════════╝
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🎨 Inflar layout con el diseño hermoso
        setContentView(R.layout.activity_splash);

        // ╔════════════════════════════════════════════════════════════════════╗
        // ║    🔗 1) OBTENER REFERENCIAS A ELEMENTOS DE UI                    ║
        // ╚════════════════════════════════════════════════════════════════════╝
        logo = findViewById(R.id.logo);
        appName = findViewById(R.id.app_name);

        // ╔════════════════════════════════════════════════════════════════════╗
        // ║    🎵 3) VERIFICAR PERMISO DE AUDIO (SIN BLOQUEAR ANIMACIONES)    ║
        // ╚════════════════════════════════════════════════════════════════════╝
        checkAudioPermission();

        // ╔════════════════════════════════════════════════════════════════════╗
        // ║    ⏳ 4) NAVEGACIÓN DESPUÉS DE 3 SEGUNDOS                         ║
        // ║    • Si usuario tiene sesión → MainActivity                       ║
        // ║    • Si usuario NO tiene sesión → LoginActivity                   ║
        // ╚════════════════════════════════════════════════════════════════════╝
        new Handler().postDelayed(() -> {
            navigateToNextScreen();
        }, SPLASH_DURATION);
    }


    // ╔════════════════════════════════════════════════════════════════════╗
    // ║    🎵 checkAudioPermission: Verifica permiso de audio            ║
    // ╚════════════════════════════════════════════════════════════════════╝
    /**
     * Verifica si la app tiene permiso de audio (RECORD_AUDIO)
     * Si no lo tiene, muestra la pantalla de solicitud de permisos
     * Esto NO bloquea las animaciones del splash
     */
    private void checkAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasAudioPermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;

            if (!hasAudioPermission) {
                // ➤ No tiene permiso, mostrar pantalla de solicitud
                // (Esto se hace en paralelo con el splash, no bloquea)
                Intent permIntent = new Intent(SplashActivity.this, MusicPermissionActivity.class);
                startActivity(permIntent);
            }
        }
    }

    // ╔════════════════════════════════════════════════════════════════════╗
    // ║    🚀 navigateToNextScreen: Decide a dónde ir después del splash ║
    // ╚════════════════════════════════════════════════════════════════════╝
    /**
     * Navegación inteligente basada en estado de login:
     * • Si el usuario ya tiene sesión activa → MainActivity
     * • Si el usuario NO tiene sesión → LoginActivity
     */
    private void navigateToNextScreen() {
        UserManager userManager = UserManager.getInstance(this);

        Intent intent;
        if (userManager.isLoggedIn()) {
            // ✅ Usuario tiene sesión → ir directo a MainActivity
            intent = new Intent(SplashActivity.this, MainActivity.class);
        } else {
            // ❌ Usuario NO tiene sesión → ir a LoginActivity
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        startActivity(intent);
        finish(); // 💫 Cerrar SplashActivity para no volver atrás
    }
}
