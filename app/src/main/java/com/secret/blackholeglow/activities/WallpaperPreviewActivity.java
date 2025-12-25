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
import android.view.ViewGroup;
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
import com.secret.blackholeglow.systems.AdsManager;
import com.secret.blackholeglow.systems.EventBus;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 🔧 FIX Android 15: Habilitar Edge-to-Edge ANTES de super.onCreate()
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        // 1️⃣ Recuperar parámetros
        nombre_wallpaper = getIntent().getStringExtra("WALLPAPER_ID");
        if (nombre_wallpaper == null) nombre_wallpaper = "Batalla Cósmica";
        previewResourceId = getIntent().getIntExtra("WALLPAPER_PREVIEW_ID", R.drawable.preview_space);
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
        ImageView backgroundImage = new ImageView(this);
        backgroundImage.setImageResource(previewResourceId);
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

        // Boton principal
        View mainButton = createMainButton();
        buttonContainer.addView(mainButton);

        // Boton desinstalar
        View uninstallButton = createUninstallButton();
        LinearLayout.LayoutParams uninstallParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        uninstallParams.setMargins(0, dpToPx(15), 0, 0);
        buttonContainer.addView(uninstallButton, uninstallParams);

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
        textBtn.setText("Instalar");
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
        title.setText("📖 Cómo usar el Wallpaper");
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
                "Presiona para iniciar la animación del wallpaper"
        );
        panel.addView(playRow);

        // ════════════════════════════════════════
        // ⏹️ Instrucción STOP
        // ════════════════════════════════════════
        LinearLayout stopRow = createInstructionRow(
                "⏹",
                COLOR_PINK,
                "STOP",
                "Presiona para detener y volver al panel de control"
        );
        panel.addView(stopRow);

        // ════════════════════════════════════════
        // 💡 Tip adicional
        // ════════════════════════════════════════
        TextView tip = new TextView(this);
        tip.setText("💡 El botón STOP aparece dentro del wallpaper activo");
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
        textBtn.setText("Definir fondo de pantalla");
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

        // ════════════════════════════════════════
        // 🗑️ Botón Desinstalar (siempre visible)
        // ════════════════════════════════════════
        View uninstallButton = createUninstallButton();
        LinearLayout.LayoutParams uninstallParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        uninstallParams.setMargins(0, dpToPx(15), 0, 0);
        container.addView(uninstallButton, uninstallParams);

        return container;
    }

    /**
     * Crea el botón de desinstalar wallpaper
     */
    private View createUninstallButton() {
        TextView button = new TextView(this);
        button.setText("Desinstalar");
        button.setTextColor(Color.parseColor("#AAAAAA"));
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dpToPx(20), dpToPx(8), dpToPx(20), dpToPx(8));

        button.setOnClickListener(v -> showUninstallConfirmation());

        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    ((TextView) v).setTextColor(COLOR_RED);
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    ((TextView) v).setTextColor(Color.parseColor("#AAAAAA"));
                    break;
            }
            return false;
        });

        return button;
    }

    /**
     * Muestra confirmación antes de desinstalar
     */
    private void showUninstallConfirmation() {
        LinearLayout overlay = new LinearLayout(this);
        overlay.setOrientation(LinearLayout.VERTICAL);
        overlay.setGravity(Gravity.CENTER);
        overlay.setBackgroundColor(Color.parseColor("#CC000000"));

        LinearLayout messageContainer = new LinearLayout(this);
        messageContainer.setOrientation(LinearLayout.VERTICAL);
        messageContainer.setGravity(Gravity.CENTER);
        messageContainer.setPadding(60, 50, 60, 40);

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(40f);
        background.setColor(COLOR_CARD_BG);
        background.setStroke(3, COLOR_RED);
        messageContainer.setBackground(background);

        // Icono
        TextView iconView = new TextView(this);
        iconView.setText("🗑️");
        iconView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 45);
        iconView.setGravity(Gravity.CENTER);
        messageContainer.addView(iconView);

        // Título
        TextView titleView = new TextView(this);
        titleView.setText("¿Desinstalar wallpaper?");
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        titleView.setTextColor(Color.WHITE);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 20, 0, 10);
        messageContainer.addView(titleView);

        // Descripción
        TextView descView = new TextView(this);
        descView.setText("Se restaurará tu fondo de\npantalla anterior");
        descView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        descView.setTextColor(Color.parseColor("#AAAAAA"));
        descView.setGravity(Gravity.CENTER);
        messageContainer.addView(descView);

        // Contenedor de botones
        LinearLayout buttonsRow = new LinearLayout(this);
        buttonsRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonsRow.setGravity(Gravity.CENTER);
        buttonsRow.setPadding(0, 30, 0, 0);

        // Botón Cancelar
        TextView cancelBtn = new TextView(this);
        cancelBtn.setText("Cancelar");
        cancelBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        cancelBtn.setTextColor(Color.parseColor("#888888"));
        cancelBtn.setPadding(dpToPx(25), dpToPx(12), dpToPx(25), dpToPx(12));
        cancelBtn.setOnClickListener(v -> rootContainer.removeView(overlay));
        buttonsRow.addView(cancelBtn);

        // Espacio
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(20), 1));
        buttonsRow.addView(spacer);

        // Botón Confirmar
        TextView confirmBtn = new TextView(this);
        confirmBtn.setText("Desinstalar");
        confirmBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        confirmBtn.setTextColor(COLOR_RED);
        confirmBtn.setTypeface(null, Typeface.BOLD);
        confirmBtn.setPadding(dpToPx(25), dpToPx(12), dpToPx(25), dpToPx(12));

        GradientDrawable confirmBg = new GradientDrawable();
        confirmBg.setShape(GradientDrawable.RECTANGLE);
        confirmBg.setCornerRadius(dpToPx(20));
        confirmBg.setColor(Color.parseColor("#331111"));
        confirmBg.setStroke(2, COLOR_RED);
        confirmBtn.setBackground(confirmBg);

        confirmBtn.setOnClickListener(v -> {
            rootContainer.removeView(overlay);
            uninstallWallpaper();
        });
        buttonsRow.addView(confirmBtn);

        messageContainer.addView(buttonsRow);

        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        overlay.addView(messageContainer, containerParams);

        FrameLayout.LayoutParams overlayParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);

        overlay.setAlpha(0f);
        rootContainer.addView(overlay, overlayParams);
        overlay.animate().alpha(1f).setDuration(200).start();
    }

    /**
     * Desinstala el wallpaper estableciendo un fondo negro sólido
     */
    private void uninstallWallpaper() {
        new Thread(() -> {
            try {
                WallpaperManager wm = WallpaperManager.getInstance(this);

                // Crear un bitmap negro de 1x1 pixel (se expandirá automáticamente)
                Bitmap blackBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                blackBitmap.eraseColor(Color.BLACK);

                // Establecer como wallpaper (esto reemplaza el live wallpaper)
                wm.setBitmap(blackBitmap);

                blackBitmap.recycle();

                Log.d(TAG, "✅ Wallpaper desinstalado - fondo negro establecido");

                // Mostrar mensaje de éxito en UI thread
                runOnUiThread(this::showUninstallSuccess);

            } catch (Exception e) {
                Log.e(TAG, "Error al desinstalar wallpaper: " + e.getMessage());
                runOnUiThread(() -> showUninstallError(e.getMessage()));
            }
        }).start();
    }

    /**
     * Muestra mensaje de éxito al desinstalar
     */
    private void showUninstallSuccess() {
        LinearLayout messageContainer = new LinearLayout(this);
        messageContainer.setOrientation(LinearLayout.VERTICAL);
        messageContainer.setGravity(Gravity.CENTER);
        messageContainer.setPadding(60, 50, 60, 50);

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(40f);
        background.setColor(COLOR_CARD_BG);
        background.setStroke(3, COLOR_GREEN);
        messageContainer.setBackground(background);

        // Icono
        TextView iconView = new TextView(this);
        iconView.setText("✅");
        iconView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 45);
        iconView.setGravity(Gravity.CENTER);
        messageContainer.addView(iconView);

        // Título
        TextView titleView = new TextView(this);
        titleView.setText("Wallpaper desinstalado");
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        titleView.setTextColor(Color.WHITE);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 20, 0, 10);
        messageContainer.addView(titleView);

        // Descripción
        TextView descView = new TextView(this);
        descView.setText("Tu fondo de pantalla ha sido\nrestaurado a negro");
        descView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        descView.setTextColor(Color.parseColor("#AAAAAA"));
        descView.setGravity(Gravity.CENTER);
        messageContainer.addView(descView);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        messageContainer.setLayoutParams(params);

        messageContainer.setAlpha(0f);
        rootContainer.addView(messageContainer);
        messageContainer.animate().alpha(1f).setDuration(300).start();

        // Auto cerrar y volver después de 2 segundos
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            messageContainer.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        // Actualizar UI para reflejar que ya no está instalado
                        recreate();
                    })
                    .start();
        }, 2000);
    }

    /**
     * Muestra mensaje de error al desinstalar
     */
    private void showUninstallError(String errorMessage) {
        LinearLayout messageContainer = new LinearLayout(this);
        messageContainer.setOrientation(LinearLayout.VERTICAL);
        messageContainer.setGravity(Gravity.CENTER);
        messageContainer.setPadding(60, 50, 60, 50);

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(40f);
        background.setColor(COLOR_CARD_BG);
        background.setStroke(3, COLOR_RED);
        messageContainer.setBackground(background);

        // Icono
        TextView iconView = new TextView(this);
        iconView.setText("⚠️");
        iconView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 45);
        iconView.setGravity(Gravity.CENTER);
        messageContainer.addView(iconView);

        // Título
        TextView titleView = new TextView(this);
        titleView.setText("No se pudo desinstalar");
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        titleView.setTextColor(Color.WHITE);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 20, 0, 10);
        messageContainer.addView(titleView);

        // Descripción
        TextView descView = new TextView(this);
        descView.setText("Ve a Ajustes > Pantalla >\nFondo de pantalla");
        descView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        descView.setTextColor(Color.parseColor("#AAAAAA"));
        descView.setGravity(Gravity.CENTER);
        messageContainer.addView(descView);

        // Botón OK
        TextView okBtn = new TextView(this);
        okBtn.setText("Entendido");
        okBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        okBtn.setTextColor(COLOR_CYAN);
        okBtn.setTypeface(null, Typeface.BOLD);
        okBtn.setGravity(Gravity.CENTER);
        okBtn.setPadding(40, 30, 40, 10);
        okBtn.setOnClickListener(v -> {
            rootContainer.removeView(messageContainer);
        });
        messageContainer.addView(okBtn);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        messageContainer.setLayoutParams(params);

        messageContainer.setAlpha(0f);
        rootContainer.addView(messageContainer);
        messageContainer.animate().alpha(1f).setDuration(300).start();
    }

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
     * Maneja el click en el botón de establecer wallpaper
     */
    private void onSetWallpaperClicked() {
        // ✅ Siempre permitir cambiar/reinstalar wallpaper
        // (El usuario puede querer cambiar de Batalla Cósmica a Bosque Navideño)

        // Mostrar anuncio y proceder
        AdsManager.get().showInterstitialAd(this, shown -> {
            Log.d(TAG, "Ad completado: " + shown);
            proceedToSetWallpaper();
        });
    }

    /**
     * Muestra mensaje de que el wallpaper ya está instalado
     */
    private void showAlreadyInstalledMessage() {
        LinearLayout messageContainer = new LinearLayout(this);
        messageContainer.setOrientation(LinearLayout.VERTICAL);
        messageContainer.setGravity(Gravity.CENTER);
        messageContainer.setPadding(60, 50, 60, 50);

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(40f);
        background.setColor(COLOR_CARD_BG);
        background.setStroke(3, COLOR_CYAN);
        messageContainer.setBackground(background);

        // Icono
        TextView iconView = new TextView(this);
        iconView.setText("✓");
        iconView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 50);
        iconView.setTextColor(COLOR_GREEN);
        iconView.setGravity(Gravity.CENTER);
        messageContainer.addView(iconView);

        // Título
        TextView titleView = new TextView(this);
        titleView.setText("Wallpaper ya instalado");
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        titleView.setTextColor(Color.WHITE);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 20, 0, 10);
        messageContainer.addView(titleView);

        // Subtítulo
        TextView subtitleView = new TextView(this);
        subtitleView.setText("Ve a tu pantalla de inicio y\npresiona ▶ PLAY para iniciarlo");
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        subtitleView.setTextColor(Color.parseColor("#AAAAAA"));
        subtitleView.setGravity(Gravity.CENTER);
        messageContainer.addView(subtitleView);

        // Botón OK
        TextView okBtn = new TextView(this);
        okBtn.setText("Entendido");
        okBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        okBtn.setTextColor(COLOR_CYAN);
        okBtn.setTypeface(null, Typeface.BOLD);
        okBtn.setGravity(Gravity.CENTER);
        okBtn.setPadding(40, 30, 40, 10);
        okBtn.setOnClickListener(v -> {
            ((ViewGroup) messageContainer.getParent()).removeView(messageContainer);
        });
        messageContainer.addView(okBtn);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        messageContainer.setLayoutParams(params);

        messageContainer.setAlpha(0f);
        rootContainer.addView(messageContainer);
        messageContainer.animate().alpha(1f).setDuration(200).start();
    }

    /**
     * Procede a establecer el wallpaper después del anuncio
     */
    private void proceedToSetWallpaper() {
        // Guardar directamente en SharedPreferences (sincrono)
        getSharedPreferences("blackholeglow_prefs", MODE_PRIVATE)
            .edit()
            .putString("selected_wallpaper", nombre_wallpaper)
            .commit();

        Log.d(TAG, "✅ Wallpaper guardado directamente: " + nombre_wallpaper);

        // Verificar que se guardo
        String saved = getSharedPreferences("blackholeglow_prefs", MODE_PRIVATE)
            .getString("selected_wallpaper", "NONE");
        Log.d(TAG, "✅ Verificacion - valor guardado: " + saved);

        waitingForWallpaperResult = true;

        EventBus.get().publish("wallpaper_set",
                new EventBus.EventData().put("wallpaper_id", nombre_wallpaper));

        Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
        intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                new android.content.ComponentName(this, LiveWallpaperService.class));

        startActivity(intent);

        // Guardar tambien via WallpaperPreferences (para Firebase)
        WallpaperPreferences.getInstance(this).setSelectedWallpaper(nombre_wallpaper, null);
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
        titleView.setText("¡Wallpaper Aplicado!");
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
        infoView.setText("Ve a inicio y presiona ▶ PLAY");
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

        // Auto cerrar
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            messageContainer.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(this::finish)
                    .start();
        }, 2500);
    }
}
