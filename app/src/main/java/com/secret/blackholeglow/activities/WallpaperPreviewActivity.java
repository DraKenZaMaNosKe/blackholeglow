// ╔════════════════════════════════════════════════════════════════════╗
// ║ 🖼️ WallpaperPreviewActivity.java – Vista Previa del Wallpaper      ║
// ║                                                                    ║
// ║  ✨ Muestra preview del live wallpaper con instrucciones claras    ║
// ║  🎮 Guía al usuario sobre cómo usar PLAY y STOP                    ║
// ║  🛡️ Verifica si el wallpaper ya está activo antes de instalar      ║
// ╚════════════════════════════════════════════════════════════════════╝

package com.secret.blackholeglow.activities;

import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.secret.blackholeglow.LiveWallpaperService;
import com.secret.blackholeglow.R;
import com.secret.blackholeglow.WallpaperPreferences;
import com.secret.blackholeglow.image.ImageDownloadManager;
import com.secret.blackholeglow.model.ModelDownloadManager;
import com.secret.blackholeglow.systems.AdsManager;
import com.secret.blackholeglow.systems.EventBus;
import com.secret.blackholeglow.core.PreFlightCheck;
import com.secret.blackholeglow.core.ResourcePreloader;
import com.secret.blackholeglow.systems.WallpaperNotificationManager;
import com.secret.blackholeglow.video.VideoDownloadManager;

import java.util.List;

/**
 * ╔═════════════════════════════════════════════════════════════════╗
 * ║ 🌠 WallpaperPreviewActivity v2.0                                ║
 * ║                                                                 ║
 * ║  • Verifica si el wallpaper ya está instalado                   ║
 * ║  • Muestra instrucciones claras (PLAY/STOP)                     ║
 * ║  • Botón mejorado estilo neón                                   ║
 * ╚═════════════════════════════════════════════════════════════════╝
 */
public class WallpaperPreviewActivity extends AppCompatActivity {

    private static final String TAG = "WallpaperPreview";

    // ════════════════════════════════════════
    // 🎨 Colores del tema
    // ════════════════════════════════════════
    private static final int COLOR_CYAN = Color.parseColor("#00D9FF");
    private static final int COLOR_PINK = Color.parseColor("#FF0080");
    private static final int COLOR_GREEN = Color.parseColor("#00FF88");
    private static final int COLOR_RED = Color.parseColor("#FF4444");
    private static final int COLOR_DARK_BG = Color.parseColor("#0A0A15");
    private static final int COLOR_CARD_BG = Color.parseColor("#1A1A2E");

    // ════════════════════════════════════════
    // 🎮 Vistas y Variables
    // ════════════════════════════════════════
    private String nombre_wallpaper = "";
    private int previewResourceId = 0;
    private boolean waitingForWallpaperResult = false;
    private FrameLayout rootContainer;

    // 🛡️ Memory leak fix: guardar referencia al bitmap para reciclar
    private Bitmap previewBitmap = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 🔧 FIX Android 15: Habilitar Edge-to-Edge ANTES de super.onCreate()
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        // 1️⃣ Recuperar parámetros
        nombre_wallpaper = getIntent().getStringExtra("WALLPAPER_ID");
        if (nombre_wallpaper == null) nombre_wallpaper = "Batalla Cósmica";
        previewResourceId = getIntent().getIntExtra("WALLPAPER_PREVIEW_ID", R.drawable.preview_space);
        String displayName = getIntent().getStringExtra("WALLPAPER_DISPLAY_NAME");
        if (displayName == null) displayName = nombre_wallpaper;
        Log.d(TAG, "🌀 Wallpaper elegido: " + nombre_wallpaper + ", preview: " + previewResourceId);

        // 2️⃣ Construir layout
        buildLayout();
    }

    /**
     * Construye el layout - Imagen de fondo + botones flotantes
     */
    private void buildLayout() {
        rootContainer = new FrameLayout(this);
        rootContainer.setBackgroundColor(COLOR_DARK_BG);

        // Imagen de fondo (del wallpaper seleccionado)
        // ⚡ FIX: Cargar con inSampleSize para evitar OutOfMemoryError en dispositivos con poca RAM
        // 🛡️ FIX: Guardar referencia para reciclar en onDestroy()
        ImageView backgroundImage = new ImageView(this);
        try {
            // Reciclar bitmap anterior si existe (por si se recrea la actividad)
            if (previewBitmap != null && !previewBitmap.isRecycled()) {
                previewBitmap.recycle();
                previewBitmap = null;
            }
            previewBitmap = decodeSampledBitmapFromResource(getResources(), previewResourceId, 1080, 1920);
            backgroundImage.setImageBitmap(previewBitmap);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "⚠️ OutOfMemory cargando preview, usando placeholder");
            backgroundImage.setBackgroundColor(Color.parseColor("#1a1a2e"));
        }
        backgroundImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        rootContainer.addView(backgroundImage, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        // Gradiente inferior para los botones
        View gradientOverlay = new View(this);
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{Color.parseColor("#DD000000"), Color.TRANSPARENT});
        gradientOverlay.setBackground(gradient);
        FrameLayout.LayoutParams gradientParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dpToPx(250));
        gradientParams.gravity = Gravity.BOTTOM;
        rootContainer.addView(gradientOverlay, gradientParams);

        // Contenedor de botones (abajo)
        LinearLayout buttonContainer = new LinearLayout(this);
        buttonContainer.setOrientation(LinearLayout.VERTICAL);
        buttonContainer.setPadding(dpToPx(30), dpToPx(20), dpToPx(30), dpToPx(40));
        buttonContainer.setGravity(Gravity.CENTER);

        // Boton principal (Instalar)
        View mainButton = createMainButton();
        buttonContainer.addView(mainButton);

        FrameLayout.LayoutParams buttonContainerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        buttonContainerParams.gravity = Gravity.BOTTOM;
        rootContainer.addView(buttonContainer, buttonContainerParams);

        setContentView(rootContainer);

        // Aplicar insets
        final LinearLayout finalButtonContainer = buttonContainer;
        ViewCompat.setOnApplyWindowInsetsListener(rootContainer, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            finalButtonContainer.setPadding(dpToPx(30), dpToPx(20), dpToPx(30), dpToPx(40) + systemBars.bottom);
            return insets;
        });
    }

    /**
     * Crea el boton principal de definir fondo
     */
    private View createMainButton() {
        LinearLayout button = new LinearLayout(this);
        button.setOrientation(LinearLayout.HORIZONTAL);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dpToPx(28), dpToPx(12), dpToPx(28), dpToPx(12));
        button.setElevation(dpToPx(4));

        GradientDrawable buttonBg = new GradientDrawable();
        buttonBg.setShape(GradientDrawable.RECTANGLE);
        buttonBg.setCornerRadius(dpToPx(25));
        buttonBg.setColor(Color.parseColor("#00D9FF"));
        button.setBackground(buttonBg);

        TextView iconBtn = new TextView(this);
        iconBtn.setText("\uD83C\uDF84");
        iconBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        iconBtn.setPadding(0, 0, dpToPx(8), 0);
        button.addView(iconBtn);

        TextView textBtn = new TextView(this);
        textBtn.setText("Install");
        textBtn.setTextColor(Color.WHITE);
        textBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        textBtn.setTypeface(null, Typeface.BOLD);
        button.addView(textBtn);

        button.setOnClickListener(v -> onSetWallpaperClicked());

        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.setScaleX(0.95f);
                    v.setScaleY(0.95f);
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.setScaleX(1.0f);
                    v.setScaleY(1.0f);
                    break;
            }
            return false;
        });

        return button;
    }

    /**
     * Crea el panel de instrucciones para el usuario
     */
    private LinearLayout createInstructionsPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(40, 30, 40, 20);

        // Fondo semi-transparente
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#DD0A0A15"));
        panel.setBackground(bg);

        // ════════════════════════════════════════
        // 📖 Título de instrucciones
        // ════════════════════════════════════════
        TextView title = new TextView(this);
        title.setText("📖 How to Use the Wallpaper");
        title.setTextColor(COLOR_CYAN);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, 20);
        panel.addView(title);

        // ════════════════════════════════════════
        // ▶️ Instrucción PLAY
        // ════════════════════════════════════════
        LinearLayout playRow = createInstructionRow(
                "▶",
                COLOR_GREEN,
                "PLAY",
                "Press to start the wallpaper animation"
        );
        panel.addView(playRow);

        // ════════════════════════════════════════
        // ⏹️ Instrucción STOP
        // ════════════════════════════════════════
        LinearLayout stopRow = createInstructionRow(
                "⏹",
                COLOR_PINK,
                "STOP",
                "Press to stop and return to the control panel"
        );
        panel.addView(stopRow);

        // ════════════════════════════════════════
        // 💡 Tip adicional
        // ════════════════════════════════════════
        TextView tip = new TextView(this);
        tip.setText("💡 The STOP button appears inside the active wallpaper");
        tip.setTextColor(Color.parseColor("#888888"));
        tip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tip.setPadding(0, 25, 0, 0);
        panel.addView(tip);

        return panel;
    }

    /**
     * Crea una fila de instrucción con icono y texto
     */
    private LinearLayout createInstructionRow(String icon, int iconColor, String title, String description) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 12, 0, 12);
        row.setGravity(Gravity.CENTER_VERTICAL);

        // Icono circular
        TextView iconView = new TextView(this);
        iconView.setText(icon);
        iconView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        iconView.setTextColor(iconColor);
        iconView.setGravity(Gravity.CENTER);

        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor(Color.parseColor("#2A2A3E"));
        iconBg.setStroke(2, iconColor);
        iconView.setBackground(iconBg);

        int iconSize = dpToPx(44);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconParams.setMargins(0, 0, 20, 0);
        iconView.setLayoutParams(iconParams);
        row.addView(iconView);

        // Texto
        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        titleView.setTypeface(null, Typeface.BOLD);
        textContainer.addView(titleView);

        TextView descView = new TextView(this);
        descView.setText(description);
        descView.setTextColor(Color.parseColor("#AAAAAA"));
        descView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        textContainer.addView(descView);

        row.addView(textContainer);

        return row;
    }

    /**
     * Crea la sección del botón principal
     */
    private View createButtonSection() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(30, 20, 30, 30);
        container.setGravity(Gravity.CENTER);

            TextView activeMsg = new TextView(this);

        // ════════════════════════════════════════
        // 🔘 Botón Neón Premium
        // ════════════════════════════════════════
        FrameLayout buttonFrame = new FrameLayout(this);

        // Glow exterior (shadow)
        View glowView = new View(this);
        GradientDrawable glowBg = new GradientDrawable();
        glowBg.setShape(GradientDrawable.RECTANGLE);
        glowBg.setCornerRadius(dpToPx(30));
        glowBg.setColor(Color.TRANSPARENT);
        glowBg.setStroke(dpToPx(3), COLOR_CYAN);
        glowView.setBackground(glowBg);
        glowView.setAlpha(0.5f);

        FrameLayout.LayoutParams glowParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dpToPx(60));
        glowParams.setMargins(dpToPx(-4), dpToPx(-4), dpToPx(-4), dpToPx(-4));
        buttonFrame.addView(glowView, glowParams);

        // Botón principal
        LinearLayout button = new LinearLayout(this);
        button.setOrientation(LinearLayout.HORIZONTAL);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dpToPx(30), dpToPx(16), dpToPx(30), dpToPx(16));

        // Gradiente del botón
        GradientDrawable buttonBg = new GradientDrawable();
        buttonBg.setShape(GradientDrawable.RECTANGLE);
        buttonBg.setCornerRadius(dpToPx(30));
        buttonBg.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        buttonBg.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        buttonBg.setColors(new int[]{
                Color.parseColor("#00D9FF"),  // Cyan
                Color.parseColor("#0099CC")   // Cyan oscuro
        });
        button.setBackground(buttonBg);

        // Icono del botón
        TextView iconBtn = new TextView(this);
        iconBtn.setText("🚀");
        iconBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        iconBtn.setPadding(0, 0, dpToPx(12), 0);
        button.addView(iconBtn);

        // Texto del botón
        TextView textBtn = new TextView(this);
        textBtn.setText("Set wallpaper");
        textBtn.setTextColor(Color.BLACK);
        textBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        textBtn.setTypeface(null, Typeface.BOLD);
        button.addView(textBtn);

        FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dpToPx(56));
        buttonFrame.addView(button, btnParams);

        // Click listener
        button.setOnClickListener(v -> onSetWallpaperClicked());

        // Efecto de presión
        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.setScaleX(0.97f);
                    v.setScaleY(0.97f);
                    v.setAlpha(0.9f);
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.setScaleX(1.0f);
                    v.setScaleY(1.0f);
                    v.setAlpha(1.0f);
                    break;
            }
            return false;
        });

        container.addView(buttonFrame, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        // Animación de pulso para el glow
        animateGlow(glowView);

        return container;
    }

    // ════════════════════════════════════════
    // 🧹 Código de desinstalar ELIMINADO
    // El usuario no necesita desinstalar - simplemente cambia a otro wallpaper
    // ════════════════════════════════════════

    /**
     * Animación de pulso para el glow del botón
     */
    private void animateGlow(View glowView) {
        glowView.animate()
                .alpha(0.3f)
                .setDuration(1000)
                .withEndAction(() -> {
                    glowView.animate()
                            .alpha(0.6f)
                            .setDuration(1000)
                            .withEndAction(() -> animateGlow(glowView))
                            .start();
                })
                .start();
    }

    /**
     * Maneja el click en el botón de establecer wallpaper.
     * Muestra dialog de resumen pre-instalación con estado de recursos y sistema.
     */
    private void onSetWallpaperClicked() {
        showInstallReadinessDialog();
    }

    /**
     * Muestra dialog con resumen visual del estado de recursos y sistema.
     */
    private void showInstallReadinessDialog() {
        PreFlightCheck.InstallCheckResult result = PreFlightCheck.runInstallCheck(this, nombre_wallpaper);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog);
        builder.setCancelable(true);

        // Contenido del dialog
        LinearLayout content = buildReadinessView(result);

        // Scroll wrapper por si el contenido es largo
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.addView(content);

        builder.setView(scrollView);

        if (result.canInstall) {
            builder.setPositiveButton("Install", (d, w) -> proceedToSetWallpaper());
            builder.setNegativeButton("Cancel", null);
        } else if (!result.allResourcesReady) {
            builder.setNegativeButton("Back", null);
            // Se agrega botón Reintentar dinámicamente abajo
        } else {
            // systemHealthy = false, solo info
            builder.setNegativeButton("Back", null);
        }

        android.app.AlertDialog dialog = builder.create();

        // Si faltan recursos, agregar botón neutral de reintentar
        if (!result.allResourcesReady) {
            dialog.setButton(android.app.AlertDialog.BUTTON_POSITIVE, "Retry \u21BB",
                    (d, w) -> retryMissingResources(result));
        }

        dialog.show();
    }

    /**
     * Construye el LinearLayout con el contenido visual del dialog.
     */
    private LinearLayout buildReadinessView(PreFlightCheck.InstallCheckResult result) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dpToPx(24), dpToPx(20), dpToPx(24), dpToPx(8));

        // Título
        TextView title = new TextView(this);
        title.setText(result.canInstall ? "\uD83D\uDEE1\uFE0F Pre-Install Summary"
                : "\u26A0\uFE0F Pre-Install Summary");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.WHITE);
        title.setPadding(0, 0, 0, dpToPx(16));
        root.addView(title);

        // Sección Recursos
        if (!result.resources.isEmpty()) {
            TextView resourceHeader = new TextView(this);
            resourceHeader.setText("\uD83D\uDCE6 Resources");
            resourceHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            resourceHeader.setTypeface(null, Typeface.BOLD);
            resourceHeader.setTextColor(Color.parseColor("#AAAAAA"));
            resourceHeader.setPadding(0, 0, 0, dpToPx(8));
            root.addView(resourceHeader);

            for (PreFlightCheck.ResourceStatus rs : result.resources) {
                String typeName;
                switch (rs.type) {
                    case VIDEO: typeName = "Video"; break;
                    case IMAGE: typeName = "Images"; break;
                    case MODEL: typeName = "3D Models"; break;
                    default: typeName = "Resource"; break;
                }

                String icon = rs.isComplete() ? "\u2705" : "\u274C";
                String line = icon + " " + typeName + " (" + rs.available + "/" + rs.total + ")";

                TextView resourceLine = new TextView(this);
                resourceLine.setText(line);
                resourceLine.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                resourceLine.setTextColor(Color.WHITE);
                resourceLine.setPadding(dpToPx(8), dpToPx(2), 0, dpToPx(2));
                root.addView(resourceLine);

                // Mostrar nombres de archivos faltantes
                if (!rs.isComplete()) {
                    for (String missing : rs.missing) {
                        TextView missingLine = new TextView(this);
                        missingLine.setText("     Missing: " + missing);
                        missingLine.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                        missingLine.setTextColor(Color.parseColor("#FF6666"));
                        missingLine.setPadding(dpToPx(16), 0, 0, dpToPx(2));
                        root.addView(missingLine);
                    }
                }
            }
        } else {
            // Escena sin recursos descargables (puro local)
            TextView noResources = new TextView(this);
            noResources.setText("\u2705 No external resources needed");
            noResources.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            noResources.setTextColor(Color.WHITE);
            noResources.setPadding(dpToPx(8), dpToPx(4), 0, dpToPx(4));
            root.addView(noResources);
        }

        // Separador
        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#333333"));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1));
        dividerParams.setMargins(0, dpToPx(12), 0, dpToPx(12));
        root.addView(divider, dividerParams);

        // Sección Sistema
        TextView systemHeader = new TextView(this);
        systemHeader.setText("\uD83D\uDCCA System");
        systemHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        systemHeader.setTypeface(null, Typeface.BOLD);
        systemHeader.setTextColor(Color.parseColor("#AAAAAA"));
        systemHeader.setPadding(0, 0, 0, dpToPx(8));
        root.addView(systemHeader);

        // RAM
        String ramIcon = result.availableRamMB >= 50 ? "\u2705" : "\u26A0\uFE0F";
        String ramText = "RAM: " + formatSize(result.availableRamMB) + " free " + ramIcon;
        TextView ramLine = new TextView(this);
        ramLine.setText(ramText);
        ramLine.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        ramLine.setTextColor(Color.WHITE);
        ramLine.setPadding(dpToPx(8), dpToPx(2), 0, dpToPx(2));
        root.addView(ramLine);

        // Disco
        String diskIcon = result.freeDiskMB >= 30 ? "\u2705" : "\u26A0\uFE0F";
        String diskText = "Disk: " + formatSize(result.freeDiskMB) + " free " + diskIcon;
        TextView diskLine = new TextView(this);
        diskLine.setText(diskText);
        diskLine.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        diskLine.setTextColor(Color.WHITE);
        diskLine.setPadding(dpToPx(8), dpToPx(2), 0, dpToPx(2));
        root.addView(diskLine);

        return root;
    }

    /**
     * Formatea MB en texto legible (ej: 1800 MB → "1.8 GB", 450 MB → "450 MB")
     */
    private String formatSize(long mb) {
        if (mb >= 1024) {
            return String.format("%.1f GB", mb / 1024.0);
        }
        return mb + " MB";
    }

    /**
     * Re-descarga los recursos faltantes en background, luego re-muestra el dialog.
     */
    private void retryMissingResources(PreFlightCheck.InstallCheckResult failedResult) {
        android.app.ProgressDialog progress = new android.app.ProgressDialog(this);
        progress.setMessage("Downloading missing resources...");
        progress.setCancelable(false);
        progress.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
        List<String> allMissing = failedResult.getAllMissing();
        progress.setMax(allMissing.size());
        progress.show();

        new Thread(() -> {
            int downloaded = 0;
            for (String file : allMissing) {
                if (isFinishing() || isDestroyed()) break;
                final int current = downloaded + 1;
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        progress.setProgress(current);
                        progress.setMessage("Downloading: " + file);
                    }
                });

                boolean success = false;
                if (file.endsWith(".mp4")) {
                    success = VideoDownloadManager.getInstance(this)
                            .downloadVideoSync(file, percent -> {});
                } else if (file.endsWith(".obj")) {
                    success = ModelDownloadManager.getInstance(this)
                            .downloadModelSync(file, percent -> {});
                } else {
                    success = ImageDownloadManager.getInstance(this)
                            .downloadImageSync(file, percent -> {});
                }

                if (success) downloaded++;
                Log.d(TAG, (success ? "\u2705" : "\u274C") + " Retry: " + file);
            }

            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    progress.dismiss();
                    // Re-mostrar el dialog con estado actualizado
                    showInstallReadinessDialog();
                }
            });
        }, "RetryDownload").start();
    }

    /**
     * Procede a establecer el wallpaper (recursos ya verificados por dialog).
     *
     * \uD83D\uDEE1\uFE0F CONSOLIDADO: Una sola escritura via WallpaperPreferences
     */
    private void proceedToSetWallpaper() {
        // \uD83D\uDEE1\uFE0F CONSOLIDADO: Solo usar WallpaperPreferences (maneja SharedPrefs + Firebase)
        WallpaperPreferences.getInstance(this).setSelectedWallpaper(nombre_wallpaper, null);
        Log.d(TAG, "\u2705 Wallpaper guardado via WallpaperPreferences: " + nombre_wallpaper);

        EventBus.get().publish("wallpaper_set",
                new EventBus.EventData().put("wallpaper_id", nombre_wallpaper));

        // \uD83D\uDD27 FIX: Si nuestro wallpaper YA est\u00E1 activo, NO abrir el system picker.
        if (isOurWallpaperActive()) {
            Log.d(TAG, "\uD83D\uDD27 Wallpaper ya activo - cambio directo sin system picker");
            showSuccessMessage();
        } else {
            Log.d(TAG, "\uD83C\uDD95 Primera instalaci\u00F3n - abriendo system picker");
            waitingForWallpaperResult = true;

            Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            intent.putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    new android.content.ComponentName(this, LiveWallpaperService.class));

            startActivity(intent);
        }
    }

    // ════════════════════════════════════════
    // 🔄 Ciclo de Vida
    // ════════════════════════════════════════

    @Override
    protected void onResume() {
        super.onResume();
        if (waitingForWallpaperResult) {
            waitingForWallpaperResult = false;
            if (isOurWallpaperActive()) {
                showSuccessMessage();
            } else {
                // 🛡️ Usuario canceló el system picker - revertir preferencia
                // para que el catálogo no muestre "INSTALADO" incorrectamente
                String previousWallpaper = getIntent().getStringExtra("PREVIOUS_WALLPAPER_ID");
                Log.d(TAG, "🔙 System picker cancelado - revirtiendo preferencia a: " + previousWallpaper);
                if (previousWallpaper != null && !previousWallpaper.isEmpty()) {
                    WallpaperPreferences.getInstance(this).setSelectedWallpaper(previousWallpaper, null);
                } else {
                    // No había wallpaper anterior - limpiar preferencia
                    WallpaperPreferences.getInstance(this).setSelectedWallpaper("", null);
                }
            }
        }
    }

    // ════════════════════════════════════════
    // 🛠️ Utilidades
    // ════════════════════════════════════════

    private boolean isOurWallpaperActive() {
        try {
            WallpaperManager wm = WallpaperManager.getInstance(this);
            WallpaperInfo info = wm.getWallpaperInfo();
            if (info != null) {
                return info.getPackageName().equals(getPackageName());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error verificando wallpaper: " + e.getMessage());
        }
        return false;
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }

    /**
     * Muestra mensaje de éxito al aplicar el wallpaper
     */
    private void showSuccessMessage() {
        LinearLayout messageContainer = new LinearLayout(this);
        messageContainer.setOrientation(LinearLayout.VERTICAL);
        messageContainer.setGravity(Gravity.CENTER);
        messageContainer.setPadding(60, 50, 60, 50);

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(40f);
        background.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        background.setColors(new int[]{COLOR_CARD_BG, Color.parseColor("#16213E")});
        background.setStroke(3, COLOR_GREEN);
        messageContainer.setBackground(background);

        // Icono
        TextView iconView = new TextView(this);
        iconView.setText("🎉");
        iconView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 50);
        iconView.setGravity(Gravity.CENTER);
        messageContainer.addView(iconView);

        // Título
        TextView titleView = new TextView(this);
        titleView.setText("Wallpaper Applied!");
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        titleView.setTextColor(Color.WHITE);
        titleView.setGravity(Gravity.CENTER);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setPadding(0, 20, 0, 10);
        messageContainer.addView(titleView);

        // Subtítulo
        TextView subtitleView = new TextView(this);
        subtitleView.setText(nombre_wallpaper);
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        subtitleView.setTextColor(COLOR_CYAN);
        subtitleView.setGravity(Gravity.CENTER);
        messageContainer.addView(subtitleView);

        // Instrucción
        TextView infoView = new TextView(this);
        infoView.setText("Go to home and press ▶ PLAY");
        infoView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        infoView.setTextColor(Color.parseColor("#888888"));
        infoView.setGravity(Gravity.CENTER);
        infoView.setPadding(0, 25, 0, 0);
        messageContainer.addView(infoView);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        messageContainer.setLayoutParams(params);

        messageContainer.setAlpha(0f);
        rootContainer.addView(messageContainer);
        messageContainer.animate().alpha(1f).setDuration(300).start();

        // Enviar notificación de wallpaper instalado (verificada contra el sistema)
        try {
            String displayName = getIntent().getStringExtra("WALLPAPER_DISPLAY_NAME");
            if (displayName == null) displayName = nombre_wallpaper;
            WallpaperNotificationManager.getInstance(this)
                    .notifyWallpaperInstalled(nombre_wallpaper, displayName);
        } catch (Exception e) {
            Log.w(TAG, "Error enviando notificación de instalación: " + e.getMessage());
        }

        // 🧹 Limpiar recursos de escenas anteriores (instalación exitosa confirmada)
        // Solo mantiene: panel + escena recién instalada. Libera espacio en disco.
        try {
            ResourcePreloader preloader = new ResourcePreloader(this);
            preloader.cleanupAfterInstallation(nombre_wallpaper);
        } catch (Exception e) {
            Log.w(TAG, "Error iniciando limpieza post-instalación: " + e.getMessage());
        }

        // Auto cerrar
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            messageContainer.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(this::finish)
                    .start();
        }, 2500);
    }

    // ════════════════════════════════════════
    // ⚡ FIX: Carga eficiente de Bitmaps (evita OutOfMemoryError)
    // ════════════════════════════════════════

    /**
     * Calcula el inSampleSize óptimo para cargar una imagen con dimensiones reducidas
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calcular el mayor inSampleSize que mantenga las dimensiones >= target
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /**
     * Decodifica un recurso de imagen con dimensiones reducidas para ahorrar memoria
     */
    private Bitmap decodeSampledBitmapFromResource(android.content.res.Resources res, int resId, int reqWidth, int reqHeight) {
        // Primero, obtener dimensiones sin cargar el bitmap
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calcular inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decodificar con inSampleSize
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;  // Usa 2 bytes/pixel en vez de 4

        Log.d(TAG, "⚡ Cargando preview: original=" + options.outWidth + "x" + options.outHeight +
                   " → inSampleSize=" + options.inSampleSize);

        return BitmapFactory.decodeResource(res, resId, options);
    }

    // ════════════════════════════════════════
    // 🛡️ MEMORY LEAK FIX: Reciclar bitmap al destruir
    // ════════════════════════════════════════
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Reciclar bitmap para liberar memoria
        if (previewBitmap != null && !previewBitmap.isRecycled()) {
            previewBitmap.recycle();
            previewBitmap = null;
            Log.d(TAG, "🛡️ Preview bitmap reciclado");
        }
    }
}
