package com.secret.blackholeglow.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * ╔════════════════════════════════════════════════════════════════╗
 * ║  ✨ AnimatedMeshGradientView - Fondo Mesh Gradient iOS Style  ║
 * ║                                                                ║
 * ║  Gradiente de malla animado con múltiples puntos de color     ║
 * ║  que se mueven suavemente creando un efecto fluido y moderno  ║
 * ╚════════════════════════════════════════════════════════════════╝
 */
public class AnimatedMeshGradientView extends View {

    private Paint[] gradientPaints;
    private float animationProgress = 0f;
    private ValueAnimator meshAnimator;

    // ✨ PARTÍCULAS FLOTANTES
    private static final int PARTICLE_COUNT = 30; // 30 partículas flotando
    private float[] particleX;      // Posición X de cada partícula
    private float[] particleY;      // Posición Y de cada partícula
    private float[] particleSize;   // Tamaño de cada partícula
    private float[] particleSpeed;  // Velocidad de caída
    private float[] particleAlpha;  // Opacidad individual
    private Paint particlePaint;

    // Colores VISIBLES para el mesh (más brillantes y contrastantes)
    private final int[][] colorSets = {
        // Set 1: Morados, azules y cianes BRILLANTES
        {
            Color.parseColor("#1A0A3E"), // Morado oscuro pero visible
            Color.parseColor("#0A2A4E"), // Azul profundo visible
            Color.parseColor("#2A1A4E"), // Morado-azul visible
            Color.parseColor("#0A1A3E")  // Azul oscuro visible
        },
        // Set 2: Tonos complementarios brillantes
        {
            Color.parseColor("#2A1A5E"), // Morado más claro
            Color.parseColor("#0A3A5E"), // Azul cyan oscuro
            Color.parseColor("#1A2A5E"), // Azul medio
            Color.parseColor("#1A0A4E")  // Morado azulado
        }
    };

    // Posiciones de los gradientes radiales (se moverán)
    private float[] gradientX = new float[4];
    private float[] gradientY = new float[4];
    private float[] gradientOffsetX = new float[4];
    private float[] gradientOffsetY = new float[4];

    public AnimatedMeshGradientView(Context context) {
        super(context);
        init();
    }

    public AnimatedMeshGradientView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnimatedMeshGradientView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Inicializar arrays de paints
        gradientPaints = new Paint[4];
        for (int i = 0; i < 4; i++) {
            gradientPaints[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
            gradientPaints[i].setStyle(Paint.Style.FILL);
        }

        // Inicializar posiciones base (esquinas + centro)
        gradientOffsetX[0] = 0.2f;  // Superior izquierda
        gradientOffsetY[0] = 0.2f;
        gradientOffsetX[1] = 0.8f;  // Superior derecha
        gradientOffsetY[1] = 0.3f;
        gradientOffsetX[2] = 0.3f;  // Inferior izquierda
        gradientOffsetY[2] = 0.8f;
        gradientOffsetX[3] = 0.7f;  // Inferior derecha
        gradientOffsetY[3] = 0.7f;

        // ══════════════════════════════════════════════════════════════
        // 🎬 Animación del mesh (ciclo lento de 20 segundos)
        // ══════════════════════════════════════════════════════════════
        meshAnimator = ValueAnimator.ofFloat(0f, 1f);
        meshAnimator.setDuration(20000); // 20 segundos por ciclo
        meshAnimator.setRepeatCount(ValueAnimator.INFINITE);
        meshAnimator.setInterpolator(new LinearInterpolator());
        meshAnimator.addUpdateListener(animation -> {
            animationProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        meshAnimator.start();

        // ══════════════════════════════════════════════════════════════
        // ✨ Inicializar partículas flotantes
        // ══════════════════════════════════════════════════════════════
        particleX = new float[PARTICLE_COUNT];
        particleY = new float[PARTICLE_COUNT];
        particleSize = new float[PARTICLE_COUNT];
        particleSpeed = new float[PARTICLE_COUNT];
        particleAlpha = new float[PARTICLE_COUNT];

        java.util.Random random = new java.util.Random();
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            // Posiciones aleatorias en toda la pantalla
            particleX[i] = random.nextFloat(); // 0.0 a 1.0 (porcentaje del ancho)
            particleY[i] = random.nextFloat(); // 0.0 a 1.0 (porcentaje del alto)

            // Tamaños variados (1px a 4px)
            particleSize[i] = 1f + random.nextFloat() * 3f;

            // Velocidad de caída variada (más perceptible)
            particleSpeed[i] = 0.0015f + random.nextFloat() * 0.0025f; // 0.0015 a 0.004 (5x más rápido)

            // Opacidad variada (30% a 80%)
            particleAlpha[i] = 0.3f + random.nextFloat() * 0.5f;
        }

        // Paint para las partículas (estrellas brillantes)
        particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particlePaint.setStyle(Paint.Style.FILL);
        particlePaint.setColor(Color.WHITE);

        setLayerType(LAYER_TYPE_SOFTWARE, null); // Para blend modes
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateGradients();
    }

    private void updateGradients() {
        if (getWidth() == 0 || getHeight() == 0) return;

        float width = getWidth();
        float height = getHeight();

        // Actualizar posiciones con movimiento circular suave
        for (int i = 0; i < 4; i++) {
            // Movimiento circular con diferentes velocidades y radios
            float angle = (float) (animationProgress * Math.PI * 2 + i * Math.PI / 2);
            float radius = 0.1f * (i % 2 == 0 ? 1f : 0.7f); // Radio variable

            gradientX[i] = width * (gradientOffsetX[i] + (float) Math.cos(angle) * radius);
            gradientY[i] = height * (gradientOffsetY[i] + (float) Math.sin(angle) * radius);

            // Interpolar entre dos sets de colores
            int colorIndex = i % 4;
            int color1 = colorSets[0][colorIndex];
            int color2 = colorSets[1][colorIndex];

            // Oscilación suave entre los dos sets
            float colorProgress = (float) (Math.sin(animationProgress * Math.PI * 2) * 0.5f + 0.5f);
            int interpolatedColor = interpolateColor(color1, color2, colorProgress);

            // Crear gradiente radial para cada punto
            float gradientRadius = Math.max(width, height) * 0.7f;
            RadialGradient gradient = new RadialGradient(
                gradientX[i],
                gradientY[i],
                gradientRadius,
                new int[]{interpolatedColor, Color.TRANSPARENT},
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP
            );
            gradientPaints[i].setShader(gradient);
            // ALPHA MUY VISIBLE: 180-255 (70%-100%)
            gradientPaints[i].setAlpha((int) (180 + 75 * Math.sin(animationProgress * Math.PI * 2 + i)));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getWidth() == 0 || getHeight() == 0) return;

        // OPTIMIZACIÓN: Solo actualizar cada 3 frames (reduce carga)
        if (frameCount % 3 == 0) {
            updateGradients();
        }
        frameCount++;

        // Fondo base negro puro
        canvas.drawColor(Color.BLACK);

        // Dibujar cada gradiente radial (se mezclan automáticamente)
        for (int i = 0; i < 4; i++) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), gradientPaints[i]);
        }

        // ══════════════════════════════════════════════════════════════
        // ✨ DIBUJAR Y ACTUALIZAR PARTÍCULAS FLOTANTES
        // ══════════════════════════════════════════════════════════════
        float width = getWidth();
        float height = getHeight();

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            // Actualizar posición Y (caída)
            particleY[i] += particleSpeed[i];

            // Si la partícula sale por abajo, reaparece arriba
            if (particleY[i] > 1.0f) {
                particleY[i] = -0.05f; // Reaparece ligeramente arriba
                particleX[i] = (float) Math.random(); // Nueva posición X aleatoria
            }

            // Convertir posiciones relativas (0-1) a píxeles absolutos
            float x = particleX[i] * width;
            float y = particleY[i] * height;

            // Configurar alpha y dibujar partícula
            particlePaint.setAlpha((int) (particleAlpha[i] * 255));

            // Dibujar partícula como círculo brillante
            canvas.drawCircle(x, y, particleSize[i], particlePaint);

            // Dibujar halo sutil (más grande y tenue)
            particlePaint.setAlpha((int) (particleAlpha[i] * 80)); // 30% del alpha principal
            canvas.drawCircle(x, y, particleSize[i] * 2f, particlePaint);
        }
    }

    private int frameCount = 0; // Contador de frames para optimización

    /**
     * Interpolar entre dos colores
     */
    private int interpolateColor(int color1, int color2, float factor) {
        int a = (int) (Color.alpha(color1) + factor * (Color.alpha(color2) - Color.alpha(color1)));
        int r = (int) (Color.red(color1) + factor * (Color.red(color2) - Color.red(color1)));
        int g = (int) (Color.green(color1) + factor * (Color.green(color2) - Color.green(color1)));
        int b = (int) (Color.blue(color1) + factor * (Color.blue(color2) - Color.blue(color1)));
        return Color.argb(a, r, g, b);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (meshAnimator != null && !meshAnimator.isRunning()) {
            meshAnimator.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (meshAnimator != null) {
            meshAnimator.cancel();
        }
    }
}
