package com.secret.blackholeglow.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * ╔════════════════════════════════════════════════════════════════╗
 * ║  ✨ AnimatedGlowCard - Borde ESTÁTICO Optimizado               ║
 * ║                                                                ║
 * ║  VERSIÓN OPTIMIZADA:                                           ║
 * ║  • Sin animaciones (mejor rendimiento)                         ║
 * ║  • Gradiente estático con colores bonitos                      ║
 * ║  • Sin setShadowLayer (muy costoso)                            ║
 * ║  • Gradiente cacheado (no se recrea en cada draw)              ║
 * ╚════════════════════════════════════════════════════════════════╝
 */
public class AnimatedGlowCard extends View {

    private Paint gradientPaint;
    private RectF rectF;
    private LinearGradient cachedGradient;
    private int lastWidth = 0;
    private int lastHeight = 0;

    // Colores suaves semi-oscuros (morado → cyan → rosita)
    private final int[] gradientColors = {
        Color.parseColor("#6B46C1"), // Púrpura
        Color.parseColor("#06B6D4"), // Cyan
        Color.parseColor("#EC4899"), // Rosa
        Color.parseColor("#6B46C1")  // Púrpura
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
        // Paint para el borde con gradiente
        gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gradientPaint.setStyle(Paint.Style.STROKE);
        gradientPaint.setStrokeWidth(3f);
        gradientPaint.setAlpha(100); // Semi-transparente

        rectF = new RectF();
        setBackgroundColor(Color.TRANSPARENT);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getWidth() == 0 || getHeight() == 0) return;

        float cornerRadius = 28f;
        float padding = 2f;
        rectF.set(padding, padding, getWidth() - padding, getHeight() - padding);

        // Solo recrear gradiente si el tamaño cambió
        if (lastWidth != getWidth() || lastHeight != getHeight()) {
            cachedGradient = new LinearGradient(
                rectF.left, rectF.top,
                rectF.right, rectF.bottom,
                gradientColors,
                new float[]{0f, 0.33f, 0.66f, 1f},
                Shader.TileMode.CLAMP
            );
            gradientPaint.setShader(cachedGradient);
            lastWidth = getWidth();
            lastHeight = getHeight();
        }

        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, gradientPaint);
    }

    // Métodos vacíos para compatibilidad con código existente
    public void pauseAnimation() { }
    public void resumeAnimation() { }
}
