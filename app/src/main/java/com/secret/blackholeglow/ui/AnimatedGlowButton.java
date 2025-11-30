package com.secret.blackholeglow.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.OvershootInterpolator;
import androidx.appcompat.widget.AppCompatButton;

/**
 * ╔════════════════════════════════════════════════════════════════╗
 * ║  ✨ AnimatedGlowButton - Efecto ÉPICO al Tocar                 ║
 * ║                                                                ║
 * ║  OPTIMIZADO + IMPACTANTE:                                      ║
 * ║  • Gradiente estático (no consume recursos)                    ║
 * ║  • Efecto "pulse" al presionar (on-demand)                     ║
 * ║  • Onda expansiva al soltar                                    ║
 * ║  • Vibración háptica sutil                                     ║
 * ║  • Rebote satisfactorio con OvershootInterpolator              ║
 * ╚════════════════════════════════════════════════════════════════╝
 */
public class AnimatedGlowButton extends AppCompatButton {

    private Paint gradientPaint;
    private Paint borderPaint;
    private Paint ripplePaint;      // Para onda expansiva
    private Paint glowPaint;        // Para destello
    private RectF rectF;
    private LinearGradient cachedGradient;
    private int lastWidth = 0;
    private int lastHeight = 0;

    // 🌊 Efecto de onda expansiva
    private float rippleRadius = 0f;
    private float rippleAlpha = 0f;
    private float rippleCenterX = 0f;
    private float rippleCenterY = 0f;
    private boolean isRippling = false;

    // ✨ Destello brillante
    private float glowIntensity = 0f;

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
        // Paint para el fondo con gradiente
        gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gradientPaint.setStyle(Paint.Style.FILL);

        // Paint para el borde (simple, sin shadow)
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);
        borderPaint.setColor(Color.parseColor("#FFFFFF"));
        borderPaint.setAlpha(80);

        // 🌊 Paint para onda expansiva
        ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ripplePaint.setStyle(Paint.Style.STROKE);
        ripplePaint.setStrokeWidth(4f);
        ripplePaint.setColor(Color.WHITE);

        // ✨ Paint para destello central
        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.FILL);

        rectF = new RectF();

        // Configurar texto
        setTextColor(Color.WHITE);
        setTextSize(16);
        setTypeface(getTypeface(), android.graphics.Typeface.BOLD);
        setBackgroundColor(Color.TRANSPARENT);

        // Habilitar clicks
        setClickable(true);
        setFocusable(true);
    }

    // ══════════════════════════════════════════════════════════════════
    // 🎯 TOUCH HANDLER - Efecto épico al tocar
    // ══════════════════════════════════════════════════════════════════
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return super.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 📍 Guardar punto de toque
                rippleCenterX = event.getX();
                rippleCenterY = event.getY();

                // 🔽 Efecto de presión (encoge)
                animatePress();

                // 📳 Vibración sutil
                triggerHapticFeedback();
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // 🚀 Efecto de liberación épico
                animateRelease();

                // 🌊 Onda expansiva desde el punto de toque
                startRippleEffect();

                // ✨ Destello brillante
                startGlowEffect();
                break;
        }

        return super.onTouchEvent(event);
    }

    // ══════════════════════════════════════════════════════════════════
    // 🔽 Animación de presión (encoge el botón)
    // ══════════════════════════════════════════════════════════════════
    private void animatePress() {
        animate()
            .scaleX(0.92f)
            .scaleY(0.92f)
            .setDuration(80)
            .start();
    }

    // ══════════════════════════════════════════════════════════════════
    // 🚀 Animación de liberación (rebote satisfactorio)
    // ══════════════════════════════════════════════════════════════════
    private void animateRelease() {
        animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(400)
            .setInterpolator(new OvershootInterpolator(3f)) // Rebote exagerado
            .start();
    }

    // ══════════════════════════════════════════════════════════════════
    // 🌊 Onda expansiva desde el punto de toque
    // ══════════════════════════════════════════════════════════════════
    private void startRippleEffect() {
        isRippling = true;
        float maxRadius = (float) Math.hypot(getWidth(), getHeight());

        ValueAnimator rippleAnim = ValueAnimator.ofFloat(0f, maxRadius);
        rippleAnim.setDuration(500);
        rippleAnim.addUpdateListener(animation -> {
            rippleRadius = (float) animation.getAnimatedValue();
            rippleAlpha = 1f - (rippleRadius / maxRadius); // Se desvanece al expandirse
            invalidate();
        });
        rippleAnim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                isRippling = false;
                invalidate();
            }
        });
        rippleAnim.start();
    }

    // ══════════════════════════════════════════════════════════════════
    // ✨ Destello brillante central
    // ══════════════════════════════════════════════════════════════════
    private void startGlowEffect() {
        ValueAnimator glowAnim = ValueAnimator.ofFloat(0f, 1f, 0f);
        glowAnim.setDuration(300);
        glowAnim.addUpdateListener(animation -> {
            glowIntensity = (float) animation.getAnimatedValue();
            invalidate();
        });
        glowAnim.start();
    }

    // ══════════════════════════════════════════════════════════════════
    // 📳 Vibración háptica sutil
    // ══════════════════════════════════════════════════════════════════
    private void triggerHapticFeedback() {
        try {
            Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(
                        20, // 20ms - muy corta
                        android.os.VibrationEffect.DEFAULT_AMPLITUDE
                    ));
                } else {
                    vibrator.vibrate(20);
                }
            }
        } catch (Exception e) {
            // Ignorar si no hay permiso de vibración
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getWidth() == 0 || getHeight() == 0) return;

        float cornerRadius = 24f;
        float padding = 20f;
        rectF.set(padding, padding, getWidth() - padding, getHeight() - padding);

        // Solo recrear gradiente si el tamaño cambió
        if (lastWidth != getWidth() || lastHeight != getHeight()) {
            cachedGradient = new LinearGradient(
                rectF.left, rectF.top,
                rectF.right, rectF.bottom,
                gradientColors,
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP
            );
            gradientPaint.setShader(cachedGradient);
            lastWidth = getWidth();
            lastHeight = getHeight();
        }

        // ══════════════════════════════════════════════════════════════════
        // 🎨 DIBUJO BASE
        // ══════════════════════════════════════════════════════════════════

        // Fondo con gradiente
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, gradientPaint);

        // ══════════════════════════════════════════════════════════════════
        // ✨ DESTELLO BRILLANTE (cuando se toca)
        // ══════════════════════════════════════════════════════════════════
        if (glowIntensity > 0) {
            // Gradiente radial que brilla desde el centro
            RadialGradient glowGradient = new RadialGradient(
                getWidth() / 2f, getHeight() / 2f,
                getWidth() * 0.8f,
                new int[]{
                    Color.argb((int)(180 * glowIntensity), 255, 255, 255),
                    Color.argb((int)(100 * glowIntensity), 139, 92, 246),
                    Color.TRANSPARENT
                },
                new float[]{0f, 0.4f, 1f},
                Shader.TileMode.CLAMP
            );
            glowPaint.setShader(glowGradient);
            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, glowPaint);
        }

        // ══════════════════════════════════════════════════════════════════
        // 🌊 ONDA EXPANSIVA (desde punto de toque)
        // ══════════════════════════════════════════════════════════════════
        if (isRippling && rippleRadius > 0) {
            // Guardar estado del canvas
            canvas.save();

            // Clipear al rectángulo del botón para que la onda no salga
            canvas.clipRect(rectF);

            // Dibujar anillo de onda
            ripplePaint.setAlpha((int)(255 * rippleAlpha * 0.7f));
            ripplePaint.setStrokeWidth(8f * rippleAlpha + 2f);
            canvas.drawCircle(rippleCenterX, rippleCenterY, rippleRadius, ripplePaint);

            // Segunda onda más pequeña y tenue
            if (rippleRadius > 30) {
                ripplePaint.setAlpha((int)(180 * rippleAlpha * 0.5f));
                ripplePaint.setStrokeWidth(4f);
                canvas.drawCircle(rippleCenterX, rippleCenterY, rippleRadius * 0.6f, ripplePaint);
            }

            canvas.restore();
        }

        // Borde simple
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, borderPaint);

        // Texto
        super.onDraw(canvas);
    }

    // Métodos vacíos para compatibilidad
    public void pauseAnimation() { }
    public void resumeAnimation() { }
}
