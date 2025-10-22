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
    private Paint borderPaint;
    private Paint glowPaint;
    private Paint innerGlowPaint;
    private Paint outerGlowPaint;
    private Paint bloomPaint;  // Nuevo: para partículas de luz
    private RectF rectF;
    private Path borderPath;
    private PathMeasure pathMeasure;

    private float animationProgress = 0f;
    private float shimmerOffset = 0f;
    private float pulseOffset = 0f;
    private float particleProgress = 0f; // Nuevo: progreso de las partículas (0 a 1)
    private ValueAnimator gradientAnimator;
    private ValueAnimator shimmerAnimator;
    private ValueAnimator pulseAnimator;
    private ValueAnimator particleAnimator; // Nuevo: controla velocidad de partículas

    // Partículas de luz tipo Blender Bloom (OPTIMIZADO)
    private static final int PARTICLE_COUNT = 2; // Solo 2 partículas sutiles
    private float[] particleOffsets;  // Posiciones a lo largo del borde
    private float[] particleSizes;    // Tamaños aleatorios
    private float[] particleAlphas;   // Opacidades individuales
    private float[] particleSpeeds;   // Velocidad individual (para variación)
    private Random random;

    // ✨ EFECTO DE ESTELA (Particle Trail)
    private static final int TRAIL_LENGTH = 8; // Longitud de la estela (8 puntos)
    private float[][] trailPositions; // [particleIndex][trailPointIndex * 2] (x,y para cada punto)
    private int[] trailHeadIndex;     // Índice circular para cada partícula
    private Paint trailPaintCache;    // Paint reutilizable para estela

    // Cache para optimización
    private float[] posCache = new float[2]; // Reutilizar array para getPosTan
    private Paint haloPaintCache;  // Paint reutilizable para halos

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
        // 🎨 Configurar Paints - IDÉNTICO AL BOTÓN
        // ══════════════════════════════════════════════════════════════

        // Paint para el borde con gradiente animado
        gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gradientPaint.setStyle(Paint.Style.STROKE);
        gradientPaint.setStrokeWidth(4f); // Stroke para el borde (no fill)

        // Paint para el borde suave (semi-transparente, no blanco brillante)
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f); // Más delgado
        borderPaint.setColor(Color.parseColor("#A090D0")); // Lila suave
        borderPaint.setShadowLayer(8f, 0f, 0f, Color.parseColor("#8070C0")); // Sombra morada suave

        // Paint para el glow principal
        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(2f);
        glowPaint.setShadowLayer(15f, 0f, 0f, Color.parseColor("#6B46C1"));

        // Paint para glow interior (capa adicional de profundidad)
        innerGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerGlowPaint.setStyle(Paint.Style.STROKE);
        innerGlowPaint.setStrokeWidth(1f);
        innerGlowPaint.setColor(Color.parseColor("#9D5A8F")); // Rosita
        innerGlowPaint.setShadowLayer(10f, 0f, 0f, Color.parseColor("#9D5A8F"));

        // Paint para glow exterior (resplandor amplio)
        outerGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outerGlowPaint.setStyle(Paint.Style.STROKE);
        outerGlowPaint.setStrokeWidth(3f);
        outerGlowPaint.setColor(Color.parseColor("#4C7A9B")); // Azul
        outerGlowPaint.setShadowLayer(25f, 0f, 0f, Color.parseColor("#4C7A9B"));

        // Paint para partículas bloom (pequeños puntos brillantes)
        bloomPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bloomPaint.setStyle(Paint.Style.FILL);
        bloomPaint.setColor(Color.WHITE);
        bloomPaint.setShadowLayer(12f, 0f, 0f, Color.WHITE);

        rectF = new RectF();
        borderPath = new Path();

        // Inicializar partículas de luz con posiciones y velocidades aleatorias
        random = new Random();
        particleOffsets = new float[PARTICLE_COUNT];
        particleSizes = new float[PARTICLE_COUNT];
        particleAlphas = new float[PARTICLE_COUNT];
        particleSpeeds = new float[PARTICLE_COUNT];

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particleOffsets[i] = random.nextFloat(); // Posición aleatoria 0-1
            particleSizes[i] = 1.5f + random.nextFloat() * 2.5f; // Tamaño 1.5 a 4
            particleAlphas[i] = 0.3f + random.nextFloat() * 0.4f; // Alpha 0.3 a 0.7
            particleSpeeds[i] = 0.8f + random.nextFloat() * 0.6f; // Velocidad 0.8 a 1.4
        }

        // Inicializar cache de Paint para halos (optimización)
        haloPaintCache = new Paint(Paint.ANTI_ALIAS_FLAG);
        haloPaintCache.setStyle(Paint.Style.FILL);
        haloPaintCache.setColor(Color.parseColor("#A090D0")); // Lila suave

        // ✨ Inicializar arrays para estela de partículas
        trailPositions = new float[PARTICLE_COUNT][TRAIL_LENGTH * 2]; // x,y para cada punto
        trailHeadIndex = new int[PARTICLE_COUNT];
        // Inicializar todas las posiciones en -1 (invisible hasta que se llenen)
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            for (int j = 0; j < TRAIL_LENGTH * 2; j++) {
                trailPositions[i][j] = -1f;
            }
            trailHeadIndex[i] = 0;
        }

        // Paint para estela (gradiente con fade)
        trailPaintCache = new Paint(Paint.ANTI_ALIAS_FLAG);
        trailPaintCache.setStyle(Paint.Style.FILL);
        trailPaintCache.setColor(Color.parseColor("#A090D0")); // Lila suave

        // ══════════════════════════════════════════════════════════════
        // 🎬 Animación del gradiente - MUCHO MÁS LENTA (8 segundos)
        // ══════════════════════════════════════════════════════════════
        gradientAnimator = ValueAnimator.ofFloat(0f, 1f);
        gradientAnimator.setDuration(8000); // 8 seg (botón: 3 seg)
        gradientAnimator.setRepeatCount(ValueAnimator.INFINITE);
        gradientAnimator.setInterpolator(new LinearInterpolator());
        gradientAnimator.addUpdateListener(animation -> {
            animationProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        gradientAnimator.start();

        // ══════════════════════════════════════════════════════════════
        // ✨ Animación del shimmer - MUCHO MÁS LENTA (6 segundos)
        // ══════════════════════════════════════════════════════════════
        shimmerAnimator = ValueAnimator.ofFloat(0f, 1f);
        shimmerAnimator.setDuration(6000); // 6 seg (botón: 2 seg)
        shimmerAnimator.setRepeatCount(ValueAnimator.INFINITE);
        shimmerAnimator.setInterpolator(new LinearInterpolator());
        shimmerAnimator.addUpdateListener(animation -> {
            shimmerOffset = (float) animation.getAnimatedValue();
        });
        shimmerAnimator.start();

        // ══════════════════════════════════════════════════════════════
        // 💫 Animación de pulso - Ritmo diferente (2.5 segundos MÁS RÁPIDO)
        // ══════════════════════════════════════════════════════════════
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(2500); // 2.5 seg - MÁS RÁPIDO para las partículas
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new LinearInterpolator());
        pulseAnimator.addUpdateListener(animation -> {
            pulseOffset = (float) animation.getAnimatedValue();
            // No llamar invalidate() aquí - gradientAnimator ya lo hace
        });
        pulseAnimator.start();

        // ══════════════════════════════════════════════════════════════
        // ⭐ Animación ESPECÍFICA para partículas (5 segundos por vuelta)
        // ══════════════════════════════════════════════════════════════
        particleAnimator = ValueAnimator.ofFloat(0f, 1f);
        particleAnimator.setDuration(5000); // 5 segundos por vuelta completa
        particleAnimator.setRepeatCount(ValueAnimator.INFINITE);
        particleAnimator.setInterpolator(new LinearInterpolator());
        particleAnimator.addUpdateListener(animation -> {
            particleProgress = (float) animation.getAnimatedValue();
            // No llamar invalidate() aquí - gradientAnimator ya lo hace
        });
        particleAnimator.start();

        setBackgroundColor(Color.TRANSPARENT);
        setLayerType(LAYER_TYPE_SOFTWARE, null); // Para sombras
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (getWidth() == 0 || getHeight() == 0) return;

        float cornerRadius = 28f;
        float padding = 2f;
        rectF.set(padding, padding, getWidth() - padding, getHeight() - padding);

        // Crear path del borde para las partículas
        borderPath.reset();
        borderPath.addRoundRect(rectF, cornerRadius, cornerRadius, Path.Direction.CW);
        pathMeasure = new PathMeasure(borderPath, true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getWidth() == 0 || getHeight() == 0) return;

        float cornerRadius = 28f;

        // ══════════════════════════════════════════════════════════════
        // 🌊 1. Glow exterior amplio (capa más lejana) - MUY TRANSPARENTE
        // ══════════════════════════════════════════════════════════════
        float outerAlpha = (float) (0.01f + 0.01f * Math.sin(pulseOffset * Math.PI * 2)); // Reducido 85%
        outerGlowPaint.setAlpha((int) (outerAlpha * 255));
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, outerGlowPaint);

        // ══════════════════════════════════════════════════════════════
        // 🌈 2. Borde con gradiente animado suave - MUY TRANSPARENTE
        // ══════════════════════════════════════════════════════════════
        LinearGradient gradient = new LinearGradient(
            rectF.left, rectF.top,
            rectF.right, rectF.bottom,
            gradientColors,
            new float[]{0f, 0.33f, 0.66f, 1f},
            Shader.TileMode.CLAMP
        );
        gradientPaint.setShader(gradient);
        // Alpha que pulsa de forma suave - REDUCIDO A 15% (85% transparente)
        float gradientAlpha = (float) (0.07f + 0.04f * Math.sin(animationProgress * Math.PI * 2));
        gradientPaint.setAlpha((int) (gradientAlpha * 255));
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, gradientPaint);

        // ══════════════════════════════════════════════════════════════
        // 💎 3. Glow interior (profundidad) - MUY TRANSPARENTE
        // ══════════════════════════════════════════════════════════════
        float innerAlpha = (float) (0.02f + 0.015f * Math.sin((pulseOffset + 0.5f) * Math.PI * 2)); // Reducido 85%
        innerGlowPaint.setAlpha((int) (innerAlpha * 255));
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, innerGlowPaint);

        // ══════════════════════════════════════════════════════════════
        // ✨ 4. Borde shimmer sutil - MUY TRANSPARENTE
        // ══════════════════════════════════════════════════════════════
        float shimmerAlpha = (float) (0.03f + 0.03f * Math.sin(shimmerOffset * Math.PI * 2)); // Reducido 85%
        borderPaint.setAlpha((int) (shimmerAlpha * 255));
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, borderPaint);

        // ══════════════════════════════════════════════════════════════
        // 💫 5. Glow pulsante principal - MUY TRANSPARENTE
        // ══════════════════════════════════════════════════════════════
        float glowAlpha = (float) (0.02f + 0.015f * Math.sin(animationProgress * Math.PI * 2)); // Reducido 85%
        glowPaint.setAlpha((int) (glowAlpha * 255));
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, glowPaint);

        // ══════════════════════════════════════════════════════════════
        // ✨ 6. Partículas de luz BLOOM + ESTELA (Particle Trail Effect)
        // ══════════════════════════════════════════════════════════════
        if (pathMeasure != null) {
            float pathLength = pathMeasure.getLength();

            for (int i = 0; i < PARTICLE_COUNT; i++) {
                // Movimiento SUAVE y CONTINUO antihorario
                float offsetBase = particleOffsets[i];
                float currentPosition = (offsetBase + (1.0f - particleProgress)) % 1.0f;
                float distance = currentPosition * pathLength;

                // Obtener posición actual x,y en el path
                pathMeasure.getPosTan(distance, posCache, null);

                // ┌───────────────────────────────────────────────────┐
                // │ ✨ ACTUALIZAR ESTELA (circular buffer)            │
                // └───────────────────────────────────────────────────┘
                int headIdx = trailHeadIndex[i];
                trailPositions[i][headIdx * 2] = posCache[0];     // x
                trailPositions[i][headIdx * 2 + 1] = posCache[1]; // y
                trailHeadIndex[i] = (headIdx + 1) % TRAIL_LENGTH; // Avanzar índice circular

                // ┌───────────────────────────────────────────────────┐
                // │ ✨ DIBUJAR ESTELA BRILLANTE (Cyan/Blanco)        │
                // └───────────────────────────────────────────────────┘
                for (int j = 0; j < TRAIL_LENGTH; j++) {
                    // Índice circular: empezar desde el más viejo (justo después del head)
                    int idx = (trailHeadIndex[i] + j) % TRAIL_LENGTH;
                    float trailX = trailPositions[i][idx * 2];
                    float trailY = trailPositions[i][idx * 2 + 1];

                    // Saltar si aún no está inicializado
                    if (trailX < 0) continue;

                    // Fade: más opaco cuanto más nuevo (j=0 es viejo, j=TRAIL_LENGTH-1 es nuevo)
                    float ageFactor = (float) j / TRAIL_LENGTH; // 0.0 (viejo) a 1.0 (nuevo)
                    float trailAlpha = 0.6f + ageFactor * 0.4f; // MUY BRILLANTE: 60% a 100% opacidad

                    // Tamaño decreciente hacia atrás
                    float trailSize = particleSizes[i] * (0.4f + ageFactor * 0.6f);

                    // Color BRILLANTE: Cyan eléctrico con gradiente a blanco
                    int trailColor = interpolateColor(
                        Color.parseColor("#00D4FF"), // Cyan brillante (viejo)
                        Color.WHITE,                  // Blanco puro (nuevo)
                        ageFactor
                    );
                    trailPaintCache.setColor(trailColor);
                    trailPaintCache.setAlpha((int) (trailAlpha * 255));

                    // Sombra BLANCA brillante intensa
                    trailPaintCache.setShadowLayer(15f * ageFactor, 0f, 0f, Color.WHITE);

                    // Dibujar punto de estela
                    canvas.drawCircle(trailX, trailY, trailSize, trailPaintCache);
                }

                // ┌───────────────────────────────────────────────────┐
                // │ 🌈 DIBUJAR PARTÍCULA CON GRADIENTE ANIMADO       │
                // └───────────────────────────────────────────────────┘
                float flickerPhase = (float) ((pulseOffset + i * 0.5f) * Math.PI * 2);
                float flicker = (float) Math.sin(flickerPhase);
                float particleAlpha = particleAlphas[i] * (0.75f + 0.25f * flicker);

                // ✨ COLOR DINÁMICO basado en posición (morado → azul → rosita → morado)
                // Dividir el recorrido en 3 segmentos para 3 colores
                int particleColor;
                if (currentPosition < 0.33f) {
                    // Segmento 1: Morado → Azul
                    float segmentFactor = currentPosition / 0.33f;
                    particleColor = interpolateColor(
                        Color.parseColor("#9D5AFF"), // Morado brillante
                        Color.parseColor("#5AC8FF"), // Azul brillante
                        segmentFactor
                    );
                } else if (currentPosition < 0.66f) {
                    // Segmento 2: Azul → Rosita
                    float segmentFactor = (currentPosition - 0.33f) / 0.33f;
                    particleColor = interpolateColor(
                        Color.parseColor("#5AC8FF"), // Azul brillante
                        Color.parseColor("#FF5ACF"), // Rosita brillante
                        segmentFactor
                    );
                } else {
                    // Segmento 3: Rosita → Morado
                    float segmentFactor = (currentPosition - 0.66f) / 0.34f;
                    particleColor = interpolateColor(
                        Color.parseColor("#FF5ACF"), // Rosita brillante
                        Color.parseColor("#9D5AFF"), // Morado brillante
                        segmentFactor
                    );
                }

                // Núcleo brillante con color dinámico
                bloomPaint.setColor(particleColor);
                bloomPaint.setAlpha((int) (particleAlpha * 255));
                canvas.drawCircle(posCache[0], posCache[1], particleSizes[i], bloomPaint);

                // Halo exterior con color dinámico
                float haloAlpha = particleAlpha * 0.45f;
                haloPaintCache.setColor(particleColor);
                haloPaintCache.setAlpha((int) (haloAlpha * 255));
                haloPaintCache.setShadowLayer(8f, 0f, 0f, particleColor);
                canvas.drawCircle(posCache[0], posCache[1], particleSizes[i] * 2.5f, haloPaintCache);
            }
        }
    }

    /**
     * ══════════════════════════════════════════════════════════════
     * 🎨 HELPER: Interpolar entre dos colores
     * ══════════════════════════════════════════════════════════════
     * @param color1 Color inicial
     * @param color2 Color final
     * @param factor Factor de interpolación (0.0 a 1.0)
     * @return Color interpolado
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
        // ══════════════════════════════════════════════════════════════
        // ♻️ REINICIAR ANIMACIONES cuando la vista se reutiliza
        // ══════════════════════════════════════════════════════════════
        // Esto es crucial para RecyclerView que recicla vistas
        if (gradientAnimator != null && !gradientAnimator.isRunning()) {
            gradientAnimator.start();
        }
        if (shimmerAnimator != null && !shimmerAnimator.isRunning()) {
            shimmerAnimator.start();
        }
        if (pulseAnimator != null && !pulseAnimator.isRunning()) {
            pulseAnimator.start();
        }
        if (particleAnimator != null && !particleAnimator.isRunning()) {
            particleAnimator.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // ══════════════════════════════════════════════════════════════
        // 🛑 DETENER ANIMACIONES para liberar recursos
        // ══════════════════════════════════════════════════════════════
        // Cancelar todas las animaciones cuando la vista sale de pantalla
        if (gradientAnimator != null) {
            gradientAnimator.cancel();
        }
        if (shimmerAnimator != null) {
            shimmerAnimator.cancel();
        }
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
        if (particleAnimator != null) {
            particleAnimator.cancel();
        }
    }
}
