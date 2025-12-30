package com.secret.blackholeglow.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

/**
 * TextView con gradiente animado para títulos épicos
 */
public class GradientTextView extends AppCompatTextView {

    private int[] gradientColors;
    private boolean gradientSet = false;

    public GradientTextView(Context context) {
        super(context);
    }

    public GradientTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GradientTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Establece los colores del gradiente
     * @param colors Array de colores (mínimo 2)
     */
    public void setGradientColors(int... colors) {
        this.gradientColors = colors;
        this.gradientSet = false;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (gradientColors != null && gradientColors.length >= 2 && !gradientSet) {
            float width = getPaint().measureText(getText().toString());

            LinearGradient gradient = new LinearGradient(
                0, 0, width, 0,
                gradientColors,
                null,
                Shader.TileMode.CLAMP
            );

            getPaint().setShader(gradient);
            gradientSet = true;
        }
        super.onDraw(canvas);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        gradientSet = false;
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
        gradientSet = false;
    }
}
