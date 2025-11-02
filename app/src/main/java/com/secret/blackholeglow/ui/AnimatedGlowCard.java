package com.secret.blackholeglow.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import java.util.Random;

/**
 * ╔════════════════════════════════════════════════════════════════╗
 * ║  ✨ AnimatedGlowCard - Borde Épico Mejorado                   ║
 * ║                                                                ║
 * ║  Efectos implementados:                                        ║
 * ║  • Gradiente animado suave (morado → azul → rosita)            ║
 * ║  • Múltiples capas de glow con diferentes velocidades          ║
 * ║  • Efecto de profundidad con sombras dinámicas                 ║
 * ║  • Resplandor pulsante sincronizado                            ║
 * ╚════════════════════════════════════════════════════════════════╝
 */
public class AnimatedGlowCard extends View {

    private Paint gradientPaint;
    private Paint glowPaint;
    private RectF rectF;

    private float animationProgress = 0f;
    private ValueAnimator gradientAnimator;

    // Colores suaves semi-oscuros (morado → azul → rosita → morado)
    private final int[] gradientColors = {
        Color.parseColor("#6B46C1"), // Púrpura semi-oscuro
        Color.parseColor("#4C7A9B"), // Azul semi-oscuro
        Color.parseColor("#9D5A8F"), // Rosita semi-oscuro
        Color.parseColor("#6B46C1")  // Púrpura semi-oscuro
    };

    public AnimatedGlowCard(Context context) {
        super(context);
        init();
    }

    public AnimatedGlowCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnimatedGlowCard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // ══════════════════════════════════════════════════════════════
        // 🎨 Configurar Paints SIMPLIFICADOS
        // ══════════════════════════════════════════════════════════════

        // Paint para el borde con gradiente animado
        gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gradientPaint.setStyle(Paint.Style.STROKE);
        gradientPaint.setStrokeWidth(3f);

        // Paint para el glow sutil
        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(2f);
        glowPaint.setShadowLayer(12f, 0f, 0f, Color.parseColor("#6B46C1"));

        rectF = new RectF();

        // ══════════════════════════════════════════════════════════════
        // 🎬 UNA SOLA Animación de gradiente (4 segundos)
        // ══════════════════════════════════════════════════════════════
        gradientAnimator = ValueAnimator.ofFloat(0f, 1f);
        gradientAnimator.setDuration(4000); // 4 segundos - velocidad moderada
        gradientAnimator.setRepeatCount(ValueAnimator.INFINITE);
        gradientAnimator.setInterpolator(new LinearInterpolator());
        gradientAnimator.addUpdateListener(animation -> {
            animationProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        gradientAnimator.start();

        setBackgroundColor(Color.TRANSPARENT);
        setLayerType(LAYER_TYPE_HARDWARE, null); // Aceleración GPU para mejor rendimiento
    }


    @Override
    protected void onDraw(Canvas canvas) {
        if (getWidth() == 0 || getHeight() == 0) return;

        float cornerRadius = 28f;
        float padding = 2f;
        rectF.set(padding, padding, getWidth() - padding, getHeight() - padding);

        // ══════════════════════════════════════════════════════════════
        // 🌈 1. Borde con gradiente animado suave
        // ══════════════════════════════════════════════════════════════
        LinearGradient gradient = new LinearGradient(
            rectF.left, rectF.top,
            rectF.right, rectF.bottom,
            gradientColors,
            new float[]{0f, 0.33f, 0.66f, 1f},
            Shader.TileMode.CLAMP
        );
        gradientPaint.setShader(gradient);
        // Alpha que pulsa de forma suave
        float gradientAlpha = (float) (0.15f + 0.10f * Math.sin(animationProgress * Math.PI * 2));
        gradientPaint.setAlpha((int) (gradientAlpha * 255));
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, gradientPaint);

        // ══════════════════════════════════════════════════════════════
        // 💫 2. Glow pulsante sutil
        // ══════════════════════════════════════════════════════════════
        float glowAlpha = (float) (0.08f + 0.05f * Math.sin(animationProgress * Math.PI * 2));
        glowPaint.setAlpha((int) (glowAlpha * 255));
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, glowPaint);
    }

    /**
     * Pausar animación (llamar cuando la vista está fuera de pantalla)
     */
    public void pauseAnimation() {
        if (gradientAnimator != null && gradientAnimator.isRunning()) {
            gradientAnimator.pause();
        }
    }

    /**
     * Reanudar animación (llamar cuando la vista vuelve a pantalla)
     */
    public void resumeAnimation() {
        if (gradientAnimator != null && gradientAnimator.isPaused()) {
            gradientAnimator.resume();
        } else if (gradientAnimator != null && !gradientAnimator.isRunning()) {
            gradientAnimator.start();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // ♻️ REINICIAR ANIMACIÓN cuando la vista se reutiliza (RecyclerView)
        resumeAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // 🛑 DETENER ANIMACIÓN para liberar recursos
        if (gradientAnimator != null) {
            gradientAnimator.cancel();
        }
    }
}
