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
║   2. Verificar si video del panel está descargado                          ║
║   3. Si no está → descargar con barra de progreso                          ║
║   4. Cuando esté listo → navegar a siguiente pantalla                      ║
║                                                                              ║
╚══════════════════════════════════════════════════════════════════════════════╝
*/
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final String PANEL_VIDEO_FILE = "thehouse.mp4";
    private static final int MIN_SPLASH_DURATION = 2000; // Mínimo 2 segundos

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
        VideoDownloadManager manager = VideoDownloadManager.getInstance(this);

        if (manager.isVideoAvailable(PANEL_VIDEO_FILE)) {
            // Video ya está descargado
            Log.d(TAG, "✅ Video del panel ya disponible");
            onResourcesReady();
        } else {
            // Necesita descargar
            Log.d(TAG, "📥 Iniciando descarga del video del panel...");
            showDownloadUI();
            startVideoDownload(manager);
        }
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
     * Inicia la descarga del video
     */
    private void startVideoDownload(VideoDownloadManager manager) {
        manager.downloadVideo(PANEL_VIDEO_FILE, new VideoDownloadManager.DownloadCallback() {
            @Override
            public void onProgress(int percent, long downloadedBytes, long totalBytes) {
                mainHandler.post(() -> updateProgress(percent, downloadedBytes, totalBytes));
            }

            @Override
            public void onComplete(String filePath) {
                Log.d(TAG, "✅ Video descargado: " + filePath);
                mainHandler.post(() -> {
                    updateProgress(100, 0, 0);
                    statusText.setText("¡Listo!");
                    onResourcesReady();
                });
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "❌ Error descargando: " + message);
                mainHandler.post(() -> {
                    // Continuar de todos modos, el video se descargará después
                    statusText.setText("Continuando...");
                    onResourcesReady();
                });
            }
        });
    }

    /**
     * Actualiza la UI de progreso
     */
    private void updateProgress(int percent, long downloadedBytes, long totalBytes) {
        progressText.setText(percent + "%");

        // Actualizar ancho de la barra
        if (progressBarMaxWidth > 0) {
            ViewGroup.LayoutParams params = progressFill.getLayoutParams();
            params.width = (int) (progressBarMaxWidth * percent / 100f);
            progressFill.setLayoutParams(params);
        }

        // Actualizar texto de estado
        if (totalBytes > 0) {
            float mbDownloaded = downloadedBytes / (1024f * 1024f);
            float mbTotal = totalBytes / (1024f * 1024f);
            statusText.setText(String.format("Descargando recursos... %.1f/%.1f MB", mbDownloaded, mbTotal));
        }
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
