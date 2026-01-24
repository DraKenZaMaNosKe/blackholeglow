package com.secret.blackholeglow.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.secret.blackholeglow.LoginActivity;
import com.secret.blackholeglow.MusicPermissionActivity;
import com.secret.blackholeglow.R;
import com.secret.blackholeglow.UserManager;
import com.secret.blackholeglow.image.ImageDownloadManager;
import com.secret.blackholeglow.model.ModelDownloadManager;
import com.secret.blackholeglow.video.VideoDownloadManager;

/*
╔══════════════════════════════════════════════════════════════════════════════╗
║                                                                              ║
║   🌅 SplashActivity.java – Experiencia Mágica de Inicio                     ║
║                                                                              ║
║   🌌 "Cada apertura es una puerta al cosmos" 🌌                              ║
║                                                                              ║
║   FLUJO:                                                                     ║
║   1. Mostrar splash con logo                                                ║
║   2. Verificar si recursos del panel están descargados                      ║
║   3. Si no están → descargar con barra de progreso                          ║
║   4. Cuando estén listos → navegar a siguiente pantalla                     ║
║                                                                              ║
╚══════════════════════════════════════════════════════════════════════════════╝
*/
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    // NOTA: Video del panel (thehouse.mp4) eliminado en v5.0.7
    private static final int MIN_SPLASH_DURATION = 2000; // Mínimo 2 segundos

    // Recursos del Panel a descargar
    private static final String[] PANEL_IMAGES = {
        "grimoire_texture.png",  // Textura del libro
        "huevo_zerg.png",        // LikeButton ABYSSIA
        "fire_orb.png"           // LikeButton PYRALIS
    };
    private static final String[] PANEL_MODELS = {
        "grimoire.obj"           // Modelo del libro
    };

    // UI Elements
    private View downloadContainer;
    private View progressFill;
    private TextView statusText;
    private TextView progressText;
    private ProgressBar loadingSpinner;

    private Handler mainHandler;
    private long startTime;
    private int progressBarMaxWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mainHandler = new Handler(Looper.getMainLooper());
        startTime = System.currentTimeMillis();

        // Obtener referencias UI
        downloadContainer = findViewById(R.id.download_container);
        progressFill = findViewById(R.id.progress_fill);
        statusText = findViewById(R.id.status_text);
        progressText = findViewById(R.id.progress_text);
        loadingSpinner = findViewById(R.id.loading_spinner);

        // Verificar permiso de audio (en paralelo)
        checkAudioPermission();

        // Iniciar verificación de recursos
        checkAndDownloadResources();
    }

    /**
     * Verifica si los recursos están descargados, si no los descarga
     */
    private void checkAndDownloadResources() {
        // Contar recursos faltantes
        int missingCount = countMissingResources();

        if (missingCount == 0) {
            Log.d(TAG, "✅ Todos los recursos del panel ya disponibles");
            onResourcesReady();
        } else {
            Log.d(TAG, "📥 Descargando " + missingCount + " recursos del panel...");
            showDownloadUI();
            downloadAllPanelResources();
        }
    }

    /**
     * Cuenta cuántos recursos faltan por descargar
     */
    private int countMissingResources() {
        int missing = 0;
        ImageDownloadManager imageMgr = ImageDownloadManager.getInstance(this);
        ModelDownloadManager modelMgr = ModelDownloadManager.getInstance(this);

        // Ya no hay video del panel (eliminado en v5.0.7)
        for (String img : PANEL_IMAGES) {
            if (!imageMgr.isImageAvailable(img)) missing++;
        }
        for (String model : PANEL_MODELS) {
            if (!modelMgr.isModelAvailable(model)) missing++;
        }
        return missing;
    }

    /**
     * Descarga todos los recursos del panel secuencialmente
     */
    private void downloadAllPanelResources() {
        Log.d(TAG, "🚀 Iniciando descarga de recursos del panel...");
        new Thread(() -> {
            ImageDownloadManager imageMgr = ImageDownloadManager.getInstance(this);
            ModelDownloadManager modelMgr = ModelDownloadManager.getInstance(this);

            // Ya no hay video del panel (eliminado en v5.0.7)
            int totalResources = PANEL_IMAGES.length + PANEL_MODELS.length;
            int completed = 0;
            Log.d(TAG, "📊 Total recursos: " + totalResources);

            // 1. Modelo del grimoire
            for (String model : PANEL_MODELS) {
                Log.d(TAG, "📥 [" + (completed+1) + "/" + totalResources + "] Verificando modelo: " + model);
                if (!modelMgr.isModelAvailable(model)) {
                    Log.d(TAG, "⬇️ Descargando modelo: " + model);
                    final int c = completed;
                    updateStatusUI("Descargando modelo...", c, totalResources);
                    modelMgr.downloadModelSync(model, percent ->
                        updateProgressUI(percent, c, totalResources));
                    Log.d(TAG, "✅ Modelo descargado: " + model);
                } else {
                    Log.d(TAG, "✅ Modelo ya disponible: " + model);
                }
                completed++;
            }

            // 2. Imágenes (grimoire texture, like button textures)
            for (String img : PANEL_IMAGES) {
                Log.d(TAG, "📥 [" + (completed+1) + "/" + totalResources + "] Verificando imagen: " + img);
                if (!imageMgr.isImageAvailable(img)) {
                    Log.d(TAG, "⬇️ Descargando: " + img);
                    final int c = completed;
                    updateStatusUI("Descargando texturas...", c, totalResources);
                    imageMgr.downloadImageSync(img, percent ->
                        updateProgressUI(percent, c, totalResources));
                } else {
                    Log.d(TAG, "✅ Ya disponible: " + img);
                }
                completed++;
            }

            // Todo listo
            Log.d(TAG, "✅ Descarga de recursos del panel COMPLETADA");
            mainHandler.post(() -> {
                statusText.setText("¡Listo!");
                onResourcesReady();
            });
        }).start();
    }

    /**
     * Actualiza el texto de estado en UI thread
     */
    private void updateStatusUI(String status, int completedResources, int totalResources) {
        mainHandler.post(() -> statusText.setText(status));
    }

    /**
     * Actualiza la barra de progreso considerando múltiples recursos
     */
    private void updateProgressUI(int resourcePercent, int completedResources, int totalResources) {
        mainHandler.post(() -> {
            // Progreso global = (recursos completados + progreso actual) / total
            float globalProgress = (completedResources + resourcePercent / 100f) / totalResources * 100f;
            int percent = (int) globalProgress;
            progressText.setText(percent + "%");

            if (progressBarMaxWidth > 0) {
                ViewGroup.LayoutParams params = progressFill.getLayoutParams();
                params.width = (int) (progressBarMaxWidth * percent / 100f);
                progressFill.setLayoutParams(params);
            }
        });
    }

    /**
     * Muestra la UI de descarga
     */
    private void showDownloadUI() {
        loadingSpinner.setVisibility(View.GONE);
        downloadContainer.setVisibility(View.VISIBLE);
        statusText.setText("Preparando experiencia...");

        // Obtener el ancho máximo de la barra de progreso después del layout
        downloadContainer.post(() -> {
            View progressBarBg = (View) progressFill.getParent();
            progressBarMaxWidth = progressBarBg.getWidth();
        });
    }

    /**
     * Llamado cuando los recursos están listos
     */
    private void onResourcesReady() {
        // Asegurar tiempo mínimo de splash para no ser abrupto
        long elapsed = System.currentTimeMillis() - startTime;
        long remainingDelay = Math.max(0, MIN_SPLASH_DURATION - elapsed);

        mainHandler.postDelayed(this::navigateToNextScreen, remainingDelay);
    }

    /**
     * Verifica permiso de audio
     */
    private void checkAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasAudioPermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;

            if (!hasAudioPermission) {
                Intent permIntent = new Intent(SplashActivity.this, MusicPermissionActivity.class);
                startActivity(permIntent);
            }
        }
    }

    /**
     * Navega a la siguiente pantalla según estado de login
     */
    private void navigateToNextScreen() {
        UserManager userManager = UserManager.getInstance(this);

        Intent intent;
        if (userManager.isLoggedIn()) {
            intent = new Intent(SplashActivity.this, MainActivity.class);
        } else {
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        startActivity(intent);
        finish();
    }
}
