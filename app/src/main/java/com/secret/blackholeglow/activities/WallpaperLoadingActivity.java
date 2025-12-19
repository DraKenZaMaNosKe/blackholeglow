package com.secret.blackholeglow.activities;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.core.ResourcePreloader;

/**
 * WallpaperLoadingActivity - Pantalla de Carga Elegante
 *
 * Muestra una barra de progreso animada mientras se precargan
 * todos los recursos del wallpaper seleccionado.
 *
 * CARACTERISTICAS:
 * - Fondo con gradiente espacial animado
 * - Barra de progreso con glow effect
 * - Texto de estado animado
 * - Icono de planeta giratorio
 * - Particulas de estrellas de fondo
 */
public class WallpaperLoadingActivity extends AppCompatActivity implements ResourcePreloader.PreloadListener {

    private static final String TAG = "WallpaperLoading";

    // UI Elements
    private View progressBarFill;
    private TextView textProgress;
    private TextView textCurrentTask;
    private TextView textWallpaperName;
    private View glowEffect;
    private ImageView iconPlanet;

    // Data
    private String wallpaperName = "";
    private int wallpaperPreviewId = 0;
    private ResourcePreloader preloader;
    private Handler handler;

    // Animation
    private ObjectAnimator planetRotation;
    private ValueAnimator glowAnimator;
    private int currentProgress = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 🔧 FIX Android 15: Habilitar Edge-to-Edge ANTES de super.onCreate()
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        // Obtener datos del intent
        wallpaperName = getIntent().getStringExtra("WALLPAPER_ID");
        wallpaperPreviewId = getIntent().getIntExtra("WALLPAPER_PREVIEW_ID", R.drawable.ic_launcher_background);

        if (wallpaperName == null) wallpaperName = "Wallpaper";

        handler = new Handler(Looper.getMainLooper());

        // Crear UI programaticamente para maximo control
        createUI();

        // Iniciar animaciones
        startAnimations();

        // Iniciar precarga
        startPreloading();
    }

    private void createUI() {
        // Root container con fondo espacial
        FrameLayout root = new FrameLayout(this);

        // Fondo con gradiente espacial
        GradientDrawable background = new GradientDrawable();
        background.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        background.setGradientRadius(800f);
        background.setColors(new int[]{
                Color.parseColor("#0D1B2A"),  // Centro - azul oscuro
                Color.parseColor("#1B263B"),  // Medio
                Color.parseColor("#0D1B2A"),  // Borde
                Color.parseColor("#000814")   // Muy oscuro
        });
        root.setBackground(background);

        // Container central
        LinearLayout centerContainer = new LinearLayout(this);
        centerContainer.setOrientation(LinearLayout.VERTICAL);
        centerContainer.setGravity(Gravity.CENTER);
        centerContainer.setPadding(60, 0, 60, 0);

        // ═══════════════════════════════════════════════════════════════
        // 🐱 ICONO DE ORBIX (GATITO MASCOTA) GIRATORIO
        // ═══════════════════════════════════════════════════════════════
        iconPlanet = new ImageView(this);
        iconPlanet.setImageResource(R.mipmap.ic_launcher_foreground);
        iconPlanet.setScaleType(ImageView.ScaleType.FIT_CENTER);

        LinearLayout.LayoutParams planetParams = new LinearLayout.LayoutParams(200, 200);
        planetParams.gravity = Gravity.CENTER;
        planetParams.bottomMargin = 60;
        centerContainer.addView(iconPlanet, planetParams);

        // ═══════════════════════════════════════════════════════════════
        // TITULO DEL WALLPAPER
        // ═══════════════════════════════════════════════════════════════
        textWallpaperName = new TextView(this);
        textWallpaperName.setText(wallpaperName);
        textWallpaperName.setTextSize(28);
        textWallpaperName.setTextColor(Color.WHITE);
        textWallpaperName.setGravity(Gravity.CENTER);
        textWallpaperName.setTypeface(null, android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.bottomMargin = 10;
        centerContainer.addView(textWallpaperName, titleParams);

        // ═══════════════════════════════════════════════════════════════
        // SUBTITULO "Cargando recursos..."
        // ═══════════════════════════════════════════════════════════════
        TextView subtitle = new TextView(this);
        subtitle.setText("Preparando experiencia...");
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.parseColor("#8892B0"));
        subtitle.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        subtitleParams.bottomMargin = 50;
        centerContainer.addView(subtitle, subtitleParams);

        // ═══════════════════════════════════════════════════════════════
        // BARRA DE PROGRESO CUSTOM
        // ═══════════════════════════════════════════════════════════════
        FrameLayout progressContainer = new FrameLayout(this);

        // Fondo de la barra
        View progressBg = new View(this);
        GradientDrawable bgDrawable = new GradientDrawable();
        bgDrawable.setCornerRadius(20f);
        bgDrawable.setColor(Color.parseColor("#1B263B"));
        bgDrawable.setStroke(2, Color.parseColor("#415A77"));
        progressBg.setBackground(bgDrawable);

        FrameLayout.LayoutParams bgParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, 40);
        progressContainer.addView(progressBg, bgParams);

        // Relleno de la barra (animado) - ❄️ TEMA HELADO
        progressBarFill = new View(this);
        GradientDrawable fillDrawable = new GradientDrawable();
        fillDrawable.setCornerRadius(20f);
        fillDrawable.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        fillDrawable.setColors(new int[]{
                Color.parseColor("#FFFFFF"),  // Blanco hielo
                Color.parseColor("#B8E4F0"),  // Azul hielo claro
                Color.parseColor("#7DD3E8")   // Cyan helado
        });
        progressBarFill.setBackground(fillDrawable);

        FrameLayout.LayoutParams fillParams = new FrameLayout.LayoutParams(0, 36);
        fillParams.setMargins(2, 2, 2, 2);
        progressContainer.addView(progressBarFill, fillParams);

        // Glow effect sobre la barra - ❄️ TEMA HELADO
        glowEffect = new View(this);
        GradientDrawable glowDrawable = new GradientDrawable();
        glowDrawable.setCornerRadius(20f);
        glowDrawable.setColor(Color.parseColor("#60FFFFFF"));  // Brillo blanco
        glowEffect.setBackground(glowDrawable);
        glowEffect.setAlpha(0.6f);

        FrameLayout.LayoutParams glowParams = new FrameLayout.LayoutParams(60, 36);
        glowParams.setMargins(2, 2, 2, 2);
        progressContainer.addView(glowEffect, glowParams);

        LinearLayout.LayoutParams progressContainerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        progressContainerParams.bottomMargin = 20;
        centerContainer.addView(progressContainer, progressContainerParams);

        // ═══════════════════════════════════════════════════════════════
        // TEXTO DE PORCENTAJE
        // ═══════════════════════════════════════════════════════════════
        textProgress = new TextView(this);
        textProgress.setText("0%");
        textProgress.setTextSize(24);
        textProgress.setTextColor(Color.parseColor("#E0F7FA"));  // ❄️ Cyan helado claro
        textProgress.setGravity(Gravity.CENTER);
        textProgress.setTypeface(null, android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams progressTextParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        progressTextParams.bottomMargin = 10;
        centerContainer.addView(textProgress, progressTextParams);

        // ═══════════════════════════════════════════════════════════════
        // TEXTO DE TAREA ACTUAL
        // ═══════════════════════════════════════════════════════════════
        textCurrentTask = new TextView(this);
        textCurrentTask.setText("Iniciando...");
        textCurrentTask.setTextSize(12);
        textCurrentTask.setTextColor(Color.parseColor("#64748B"));
        textCurrentTask.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams taskParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        taskParams.bottomMargin = 40;
        centerContainer.addView(textCurrentTask, taskParams);

        // ═══════════════════════════════════════════════════════════════
        // 😊 MENSAJE TRANQUILIZADOR (para el usuario)
        // ═══════════════════════════════════════════════════════════════
        TextView reassuringText = new TextView(this);
        reassuringText.setText("✨ Preparando tu wallpaper...");
        reassuringText.setTextSize(14);
        reassuringText.setTextColor(Color.parseColor("#B2EBF2"));
        reassuringText.setGravity(Gravity.CENTER);
        reassuringText.setAlpha(0.9f);

        LinearLayout.LayoutParams reassuringParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        centerContainer.addView(reassuringText, reassuringParams);

        // Agregar container central al root
        FrameLayout.LayoutParams centerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        centerParams.gravity = Gravity.CENTER;
        root.addView(centerContainer, centerParams);

        // ═══════════════════════════════════════════════════════════════
        // TEXTO INFERIOR "Powered by OpenGL ES"
        // ═══════════════════════════════════════════════════════════════
        TextView footer = new TextView(this);
        footer.setText("Powered by OpenGL ES 2.0");
        footer.setTextSize(10);
        footer.setTextColor(Color.parseColor("#3D4F5F"));
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, 0, 0, 40);

        FrameLayout.LayoutParams footerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        footerParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        root.addView(footer, footerParams);

        setContentView(root);
    }

    private void startAnimations() {
        // Rotacion del planeta
        planetRotation = ObjectAnimator.ofFloat(iconPlanet, "rotation", 0f, 360f);
        planetRotation.setDuration(8000);
        planetRotation.setRepeatCount(ValueAnimator.INFINITE);
        planetRotation.setInterpolator(new LinearInterpolator());
        planetRotation.start();

        // Animacion del glow
        glowAnimator = ValueAnimator.ofFloat(0f, 1f);
        glowAnimator.setDuration(1500);
        glowAnimator.setRepeatCount(ValueAnimator.INFINITE);
        glowAnimator.setRepeatMode(ValueAnimator.REVERSE);
        glowAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            glowEffect.setAlpha(0.3f + (value * 0.5f));
        });
        glowAnimator.start();
    }

    private void startPreloading() {
        preloader = new ResourcePreloader(this);
        preloader.setListener(this);

        // Preparar tareas segun el wallpaper
        if (wallpaperName.contains("Batalla") || wallpaperName.contains("Universo")) {
            preloader.prepareBatallaCosmicaTasks();
        } else {
            preloader.prepareBatallaCosmicaTasks();  // Default por ahora
        }

        // Iniciar precarga
        preloader.startPreloading();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PRELOAD LISTENER CALLBACKS
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void onProgressUpdate(int progress, int total, String currentTask) {
        int percentage = (int) ((progress / (float) total) * 100);

        // Animar el progreso suavemente
        animateProgressTo(percentage);

        // Actualizar texto de porcentaje
        textProgress.setText(percentage + "%");

        // Actualizar tarea actual
        textCurrentTask.setText(currentTask);

        Log.d(TAG, "Progreso: " + percentage + "% - " + currentTask);
    }

    @Override
    public void onPreloadComplete() {
        Log.d(TAG, "Precarga completada!");

        // Animar a 100%
        animateProgressTo(100);
        textProgress.setText("100%");
        textCurrentTask.setText("Listo!");

        // Esperar un momento y luego ir al preview
        handler.postDelayed(() -> {
            goToPreview();
        }, 500);
    }

    @Override
    public void onPreloadError(String error) {
        Log.e(TAG, "Error en precarga: " + error);
        textCurrentTask.setText("Error: " + error);

        // Ir al preview de todos modos despues de un delay
        handler.postDelayed(() -> {
            goToPreview();
        }, 1000);
    }

    private void animateProgressTo(int targetPercentage) {
        // Calcular ancho de la barra
        View parent = (View) progressBarFill.getParent();
        int parentWidth = parent.getWidth();
        if (parentWidth == 0) parentWidth = getResources().getDisplayMetrics().widthPixels - 120;

        int targetWidth = (int) ((targetPercentage / 100f) * (parentWidth - 4));

        // Animar el ancho
        ValueAnimator animator = ValueAnimator.ofInt(progressBarFill.getLayoutParams().width, targetWidth);
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) progressBarFill.getLayoutParams();
            params.width = (int) animation.getAnimatedValue();
            progressBarFill.setLayoutParams(params);

            // Mover el glow al final de la barra
            FrameLayout.LayoutParams glowParams = (FrameLayout.LayoutParams) glowEffect.getLayoutParams();
            glowParams.leftMargin = Math.max(2, params.width - 30);
            glowEffect.setLayoutParams(glowParams);
        });
        animator.start();

        currentProgress = targetPercentage;
    }

    private void goToPreview() {
        // Detener animaciones
        if (planetRotation != null) planetRotation.cancel();
        if (glowAnimator != null) glowAnimator.cancel();

        // Ir a WallpaperPreviewActivity
        Intent intent = new Intent(this, WallpaperPreviewActivity.class);
        intent.putExtra("WALLPAPER_PREVIEW_ID", wallpaperPreviewId);
        intent.putExtra("WALLPAPER_ID", wallpaperName);
        startActivity(intent);

        // Cerrar esta activity con transicion
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (preloader != null) {
            preloader.cancel();
        }

        if (planetRotation != null) {
            planetRotation.cancel();
        }

        if (glowAnimator != null) {
            glowAnimator.cancel();
        }
    }

    @Override
    public void onBackPressed() {
        // Cancelar precarga y volver
        if (preloader != null) {
            preloader.cancel();
        }
        super.onBackPressed();
    }
}
