package com.secret.blackholeglow;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Activity para solicitar permisos de audio necesarios para MusicVisualizer
 */
public class MusicPermissionActivity extends Activity {
    private static final String TAG = "depurar";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "╔════════════════════════════════════════════╗");
        Log.d(TAG, "║   MUSIC PERMISSION ACTIVITY INICIADA     ║");
        Log.d(TAG, "╚════════════════════════════════════════════╝");

        // Verificar si ya tiene el permiso
        if (checkAudioPermission()) {
            Log.d(TAG, "[MusicPermission] ✓ Permiso ya otorgado, cerrando...");
            showSuccessAndFinish();
            return;
        }

        // Crear UI simple
        setContentView(createSimpleLayout());
    }

    private android.view.View createSimpleLayout() {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        layout.setBackgroundColor(0xFF1A1A2E);

        // Título
        TextView title = new TextView(this);
        title.setText("🎵 Audio Permission Required");
        title.setTextSize(24);
        title.setTextColor(0xFFFFFFFF);
        title.setPadding(0, 20, 0, 30);
        layout.addView(title);

        // Mensaje
        TextView message = new TextView(this);
        message.setText("For the wallpaper to react to your music, it needs audio access.\n\n" +
                "✓ Does not record or store audio\n" +
                "✓ Only analyzes frequencies in real time\n" +
                "✓ Your music is NOT affected");
        message.setTextSize(16);
        message.setTextColor(0xFFCCCCCC);
        message.setPadding(0, 0, 0, 40);
        layout.addView(message);

        // Botón de permiso
        Button btnGrant = new Button(this);
        btnGrant.setText("Grant Permission");
        btnGrant.setTextSize(18);
        btnGrant.setBackgroundColor(0xFF4CAF50);
        btnGrant.setTextColor(0xFFFFFFFF);
        btnGrant.setPadding(20, 30, 20, 30);
        btnGrant.setOnClickListener(v -> requestAudioPermission());
        layout.addView(btnGrant);

        // Botón de cancelar
        Button btnCancel = new Button(this);
        btnCancel.setText("Use without Music (Visual Only)");
        btnCancel.setTextSize(14);
        btnCancel.setBackgroundColor(0xFF666666);
        btnCancel.setTextColor(0xFFFFFFFF);
        btnCancel.setPadding(20, 20, 20, 20);
        btnCancel.setOnClickListener(v -> {
            Toast.makeText(this, "The wallpaper will work without music reactivity", Toast.LENGTH_LONG).show();
            finish();
        });
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 20, 0, 0);
        btnCancel.setLayoutParams(params);
        layout.addView(btnCancel);

        return layout;
    }

    private boolean checkAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true; // En versiones antiguas no se necesita
    }

    private void requestAudioPermission() {
        Log.d(TAG, "[MusicPermission] Solicitando permiso RECORD_AUDIO...");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Verificar si el usuario rechazó permanentemente
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                // Mostrar explicación adicional
                showRationaleAndRequest();
            } else {
                // Solicitar directamente
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void showRationaleAndRequest() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("The wallpaper needs audio access to create visual effects that react to your music.\n\n" +
                           "No audio is recorded or stored.")
                .setPositiveButton("Got it", (dialog, which) -> {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            PERMISSION_REQUEST_CODE);
                })
                .setNegativeButton("Not Now", (dialog, which) -> finish())
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "[MusicPermission] ✓✓✓ PERMISO OTORGADO!");
                showSuccessAndFinish();
            } else {
                Log.w(TAG, "[MusicPermission] ✗ Permiso DENEGADO");

                // Verificar si el usuario marcó "No volver a preguntar"
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                    // Ofrecer ir a configuración
                    showGoToSettingsDialog();
                } else {
                    Toast.makeText(this, "Without audio permission, the wallpaper won't react to music",
                                 Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }

    private void showGoToSettingsDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Permission Blocked")
                .setMessage("You have permanently denied the permission. To enable music reactivity, " +
                           "go to Settings → Apps → Black Hole Glow → Permissions → Microphone")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", (dialog, which) -> finish())
                .show();
    }

    private void showSuccessAndFinish() {
        Toast.makeText(this, "✓ Permission granted! The wallpaper will react to your music 🎵",
                     Toast.LENGTH_LONG).show();
        finish();
    }
}
