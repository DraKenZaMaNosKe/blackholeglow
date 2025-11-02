package com.secret.blackholeglow.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;
import androidx.appcompat.widget.AppCompatButton;

/**
 * ╔════════════════════════════════════════════════════════════════╗
 * ║  ✨ AnimatedGlowButton - Botón Épico con Efectos Animados     ║
 * ║                                                                ║
 * ║  Efectos implementados:                                        ║
 * ║  • Gradiente animado (púrpura → cyan → púrpura)                ║
 * ║  • Borde brillante con luz recorriendo (shimmer)               ║
 * ║  • Glow pulsante suave                                         ║
 * ║  • Esquinas redondeadas con sombra                             ║
 * ╚════════════════════════════════════════════════════════════════╝
 */
public class AnimatedGlowButton extends AppCompatButton {

    private Paint gradientPaint;
    private Paint borderPaint;
    private Paint glowPaint;
    private RectF rectF;

    private float animationProgress = 0f;
    private float shimmerOffset = 0f;
    private ValueAnimator gradientAnimator;
    private ValueAnimator shimmerAnimator;

    private final int[] gradientColors = {
        Color.parseColor("#8B5CF6"), // Púrpura
        Color.parseColor("#06B6D4"), // Cyan
        Color.parseColor("#8B5CF6")  // Púrpura
    };

    public AnimatedGlowButton(Context context) {
        super(context);
        init();
    }

    public AnimatedGlowButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnimatedGlowButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // ══════════════════════════════════════════════════════════════
        // 🎨 Configurar Paints
        // ══════════════════════════════════════════════════════════════

        // Paint para el fondo con gradiente animado
        gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gradientPaint.setStyle(Paint.Style.FILL);

        // Paint para el borde brillante
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(6f);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setShadowLayer(15f, 0f, 0f, Color.WHITE);

        // Paint para el glow exterior
        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.FILL);
        glowPaint.setShadowLayer(30f, 0f, 0f, Color.parseColor("#8B5CF6"));

        rectF = new RectF();

        // ══════════════════════════════════════════════════════════════
        // 🎬 Animación del gradiente (ciclo continuo)
        // ══════════════════════════════════════════════════════════════
        gradientAnimator = ValueAnimator.ofFloat(0f, 1f);
        gradientAnimator.setDuration(3000);
        gradientAnimator.setRepeatCount(ValueAnimator.INFINITE);
        gradientAnimator.setInterpolator(new LinearInterpolator());
        gradientAnimator.addUpdateListener(animation -> {
            animationProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        gradientAnimator.start();

        // ══════════════════════════════════════════════════════════════
        // ✨ Animación del shimmer (luz recorriendo bordes)
        // ══════════════════════════════════════════════════════════════
        shimmerAnimator = ValueAnimator.ofFloat(0f, 1f);
        shimmerAnimator.setDuration(2000);
        shimmerAnimator.setRepeatCount(ValueAnimator.INFINITE);
        shimmerAnimator.setInterpolator(new LinearInterpolator());
        shimmerAnimator.addUpdateListener(animation -> {
            shimmerOffset = (float) animation.getAnimatedValue();
        });
        shimmerAnimator.start();

        // Configurar texto
        setTextColor(Color.WHITE);
        setTextSize(16);
        setTypeface(getTypeface(), android.graphics.Typeface.BOLD);

        // Hacer el botón completamente custom (sin fondo default)
        setBackgroundColor(Color.TRANSPARENT);
        setLayerType(LAYER_TYPE_HARDWARE, null); // Aceleración GPU para mejor rendimiento
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getWidth() == 0 || getHeight() == 0) return;

        float cornerRadius = 24f;
        float padding = 20f;

        rectF.set(padding, padding, getWidth() - padding, getHeight() - padding);

        // ══════════════════════════════════════════════════════════════
        // 🌈 1. Dibujar fondo con gradiente animado
        // ══════════════════════════════════════════════════════════════
        LinearGradient gradient = new LinearGradient(
            rectF.left, rectF.top,
            rectF.right, rectF.bottom,
            gradientColors,
            new float[]{0f, animationProgress, 1f},
            Shader.TileMode.CLAMP
        );
        gradientPaint.setShader(gradient);
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, gradientPaint);

        // ══════════════════════════════════════════════════════════════
        // ✨ 2. Dibujar borde brillante con shimmer
        // ══════════════════════════════════════════════════════════════
        float shimmerAlpha = (float) (0.3f + 0.7f * Math.sin(shimmerOffset * Math.PI * 2));
        borderPaint.setAlpha((int) (shimmerAlpha * 255));
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, borderPaint);

        // ══════════════════════════════════════════════════════════════
        // 💫 3. Dibujar glow pulsante
        // ══════════════════════════════════════════════════════════════
        float glowAlpha = (float) (0.2f + 0.1f * Math.sin(animationProgress * Math.PI * 2));
        glowPaint.setAlpha((int) (glowAlpha * 255));
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, glowPaint);

        // Dibujar texto encima
        super.onDraw(canvas);
    }

    /**
     * Pausar animaciones (llamar cuando la vista está fuera de pantalla)
     */
    public void pauseAnimation() {
        if (gradientAnimator != null && gradientAnimator.isRunning()) {
            gradientAnimator.pause();
        }
        if (shimmerAnimator != null && shimmerAnimator.isRunning()) {
            shimmerAnimator.pause();
        }
    }

    /**
     * Reanudar animaciones (llamar cuando la vista vuelve a pantalla)
     */
    public void resumeAnimation() {
        if (gradientAnimator != null) {
            if (gradientAnimator.isPaused()) {
                gradientAnimator.resume();
            } else if (!gradientAnimator.isRunning()) {
                gradientAnimator.start();
            }
        }
        if (shimmerAnimator != null) {
            if (shimmerAnimator.isPaused()) {
                shimmerAnimator.resume();
            } else if (!shimmerAnimator.isRunning()) {
                shimmerAnimator.start();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (gradientAnimator != null) {
            gradientAnimator.cancel();
        }
        if (shimmerAnimator != null) {
            shimmerAnimator.cancel();
        }
    }
}
