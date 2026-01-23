package com.secret.blackholeglow.scenes;

import android.opengl.GLES20;
import android.util.Log;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;
import com.secret.blackholeglow.video.AbyssalLurker3D;
import com.secret.blackholeglow.video.AbyssalLeviathan3D;
import com.secret.blackholeglow.video.BubbleSystem;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║              🌊 ABYSSIA - Fondo del Mar Alienígena                       ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Video de fondo con plantas bioluminiscentes + peces 3D interactivos.    ║
 * ║                                                                          ║
 * ║  CARACTERÍSTICAS ÚNICAS:                                                 ║
 * ║  • AbyssalLurker3D - Pez pequeño que sigue el dedo del usuario           ║
 * ║  • AbyssalLeviathan3D - Bestia colosal del fondo oceánico                ║
 * ║  • BubbleSystem - Burbujas que emergen de los peces al "respirar"        ║
 * ║  • Interacción: El Lurker HUYE del Leviathan                             ║
 * ║                                                                          ║
 * ║  🎮 INTERACCIÓN:                                                         ║
 * ║  • Toca la pantalla → El pez pequeño nada hacia donde tocaste            ║
 * ║  • El pez huye automáticamente si el Leviathan se acerca                 ║
 * ║                                                                          ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  📖 EJEMPLO DE ESCENA CON MÚLTIPLES OBJETOS 3D INTERACTUANDO:            ║
 * ║  Esta escena demuestra cómo hacer que objetos 3D interactúen             ║
 * ║  entre sí (el Lurker conoce la posición del Leviathan).                  ║
 * ║                                                                          ║
 * ║  ✅ Video: marZerg.mp4                                                   ║
 * ║  ✅ Tema: ABYSSIA (púrpura/turquesa bioluminiscente)                     ║
 * ║  ✅ Objetos 3D: AbyssalLurker3D, AbyssalLeviathan3D, BubbleSystem        ║
 * ║  ✅ Clock + Battery incluidos automáticamente                            ║
 * ║                                                                          ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * @see BaseVideoScene para documentación completa de la clase base
 * @see AbyssalLurker3D para el pez pequeño interactivo
 * @see AbyssalLeviathan3D para la bestia del fondo
 * @see BubbleSystem para el sistema de partículas de burbujas
 */
public class OceanFloorScene extends BaseVideoScene {
    private static final String TAG = "OceanScene";

    // ═══════════════════════════════════════════════════════════════════════════
    // 🐟 CRIATURAS MARINAS 3D
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Pez pequeño que sigue el dedo del usuario.
     * Huye automáticamente cuando el Leviathan se acerca.
     */
    private AbyssalLurker3D abyssalLurker;

    /**
     * Bestia colosal del fondo oceánico.
     * Se mueve de forma autónoma por la escena.
     */
    private AbyssalLeviathan3D abyssalLeviathan;

    /**
     * Sistema de partículas de burbujas.
     * Las burbujas emergen de la "boca" de los peces al respirar.
     */
    private BubbleSystem bubbleSystem;

    // ═══════════════════════════════════════════════════════════════════════════
    // 🫧 TIMERS PARA RESPIRACIÓN (spawn de burbujas)
    // ═══════════════════════════════════════════════════════════════════════════

    private float lurkerBubbleTimer = 0f;
    private float leviathanBubbleTimer = 0f;

    /** Intervalo de respiración del pez pequeño (más lento) */
    private static final float LURKER_BUBBLE_INTERVAL = 1.8f;

    /** Intervalo de respiración del Leviathan (más rápido, es más grande) */
    private static final float LEVIATHAN_BUBBLE_INTERVAL = 1.2f;

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎛️ SISTEMA DE CALIBRACIÓN (para ajustar posiciones en desarrollo)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Activar para entrar en modo calibración.
     * - Double-tap: Cambiar pez seleccionado (Lurker ↔ Leviathan)
     * - Tap esquina superior izquierda: Cambiar modo (PosXY/PosZ/RotXY/RotZ/Scale)
     * - Arrastrar: Ajustar valor según modo activo
     */
    private static final boolean CALIBRATION_MODE = false;  // ✓ Calibración completada

    private int selectedFish = 0;  // 0 = Lurker, 1 = Leviathan
    private int adjustMode = 0;    // 0=PosXY, 1=PosZ, 2=RotXY, 3=RotZ, 4=Scale
    private float lastTouchX = 0f;
    private float lastTouchY = 0f;
    private long lastTapTime = 0;
    private static final String[] MODE_NAMES = {"PosXY", "PosZ", "RotXY", "RotZ", "Scale"};

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎯 MÉTODOS OBLIGATORIOS - Configuración básica de la escena
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public String getName() {
        return "ABYSSIA";
    }

    @Override
    public String getDescription() {
        return "Océano alienígena con plantas bioluminiscentes";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_oceano_sc;
    }

    @Override
    protected String getVideoFileName() {
        return "marZerg.mp4";
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.ABYSSIA;  // 🌊 Púrpura/Turquesa bioluminiscente
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 🔧 HOOKS PARA CRIATURAS MARINAS + BURBUJAS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Configura los peces 3D y el sistema de burbujas.
     *
     * 📖 ORDEN DE CARGA:
     * 1. AbyssalLurker3D (pez pequeño interactivo)
     * 2. AbyssalLeviathan3D (bestia grande autónoma)
     * 3. BubbleSystem (partículas de burbujas)
     */
    @Override
    protected void setupSceneSpecific() {
        // 🐟 Lurker - pez pequeño que sigue el dedo
        try {
            abyssalLurker = new AbyssalLurker3D(context);
            abyssalLurker.initialize();

            if (CALIBRATION_MODE) {
                abyssalLurker.setCalibrationMode(true);
            }

            Log.d(TAG, "✅ AbyssalLurker3D cargado - SIGUE EL DEDO");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error AbyssalLurker3D: " + e.getMessage());
        }

        // 🐉 Leviathan - bestia colosal del fondo
        try {
            abyssalLeviathan = new AbyssalLeviathan3D(context);
            abyssalLeviathan.initialize();

            if (CALIBRATION_MODE) {
                abyssalLeviathan.setCalibrationMode(true);
            }

            Log.d(TAG, "✅ AbyssalLeviathan3D cargado - MOVIMIENTO AUTÓNOMO");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error AbyssalLeviathan3D: " + e.getMessage());
        }

        // 🫧 Sistema de burbujas
        try {
            bubbleSystem = new BubbleSystem();
            bubbleSystem.initialize();
            Log.d(TAG, "✅ BubbleSystem activado - RESPIRACIÓN DE PECES");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error BubbleSystem: " + e.getMessage());
        }

        // Log de calibración si está activo
        if (CALIBRATION_MODE) {
            Log.d("CALIBRATE", "════════════════════════════════════════════════");
            Log.d("CALIBRATE", "🎛️ MODO CALIBRACIÓN ACTIVADO");
            Log.d("CALIBRATE", "   Double-tap: Cambiar pez (Lurker↔Leviathan)");
            Log.d("CALIBRATE", "   Tap sup-izq: Cambiar modo ajuste");
            Log.d("CALIBRATE", "   Arrastrar: Ajusta valor según modo");
            Log.d("CALIBRATE", "════════════════════════════════════════════════");
        }
    }

    /**
     * Actualiza los peces y el sistema de burbujas.
     *
     * 📖 FLUJO DE ACTUALIZACIÓN:
     * 1. Actualizar Leviathan primero (se mueve independientemente)
     * 2. Pasar posición del Leviathan al Lurker (para que huya)
     * 3. Actualizar Lurker (con info del Leviathan)
     * 4. Actualizar burbujas y spawn desde bocas de los peces
     */
    @Override
    protected void updateSceneSpecific(float deltaTime) {
        // 1. Actualizar Leviathan primero (movimiento autónomo)
        if (abyssalLeviathan != null) {
            abyssalLeviathan.update(deltaTime);
        }

        // 2. Pasar posición del Leviathan al Lurker (para IA de huida)
        if (abyssalLurker != null && abyssalLeviathan != null) {
            abyssalLurker.setLeviathanPosition(
                abyssalLeviathan.getPosX(),
                abyssalLeviathan.getPosY()
            );
        }

        // 3. Actualizar Lurker (con info del Leviathan)
        if (abyssalLurker != null) {
            abyssalLurker.update(deltaTime);
        }

        // 4. Actualizar sistema de burbujas
        if (bubbleSystem != null) {
            bubbleSystem.update(deltaTime);

            // 🫧 Spawn burbujas del Lurker (respiración lenta)
            if (abyssalLurker != null) {
                lurkerBubbleTimer += deltaTime;
                if (lurkerBubbleTimer >= LURKER_BUBBLE_INTERVAL) {
                    lurkerBubbleTimer = 0f;
                    float depth = abyssalLurker.getPosY();
                    bubbleSystem.spawn(
                        abyssalLurker.getMouthX(),
                        abyssalLurker.getMouthY(),
                        depth
                    );
                }
            }

            // 🫧 Spawn burbujas del Leviathan (respiración más frecuente)
            if (abyssalLeviathan != null) {
                leviathanBubbleTimer += deltaTime;
                if (leviathanBubbleTimer >= LEVIATHAN_BUBBLE_INTERVAL) {
                    leviathanBubbleTimer = 0f;
                    float depth = abyssalLeviathan.getPosY();
                    bubbleSystem.spawn(
                        abyssalLeviathan.getMouthX(),
                        abyssalLeviathan.getMouthY(),
                        depth
                    );
                }
            }
        }
    }

    /**
     * Dibuja los peces y burbujas.
     *
     * 📖 ORDEN DE DIBUJADO (importante para profundidad):
     * 1. Leviathan (grande, más atrás)
     * 2. Lurker (pequeño, más adelante)
     * 3. Burbujas (sobre los peces, debajo de UI)
     *
     * NOTA: Esta escena usa GLES20 por compatibilidad con los shaders de los peces.
     */
    @Override
    protected void drawSceneSpecific() {
        // Dibujar peces con depth test habilitado
        if (abyssalLeviathan != null) abyssalLeviathan.draw();  // Grande, fondo
        if (abyssalLurker != null) abyssalLurker.draw();        // Pequeño, frente

        // Deshabilitar depth test para burbujas (son 2D)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        // 🫧 Burbujas
        if (bubbleSystem != null && screenHeight > 0) {
            float aspectRatio = (float) screenWidth / screenHeight;
            bubbleSystem.draw(aspectRatio);
        }
    }

    /**
     * Libera recursos de los peces y burbujas.
     */
    @Override
    protected void releaseSceneSpecificResources() {
        if (abyssalLurker != null) {
            abyssalLurker.release();
            abyssalLurker = null;
        }
        if (abyssalLeviathan != null) {
            abyssalLeviathan.release();
            abyssalLeviathan = null;
        }
        if (bubbleSystem != null) {
            bubbleSystem = null;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 👆 TOUCH - El Lurker sigue el dedo (o calibración)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Maneja eventos de touch.
     *
     * MODO NORMAL:
     * - Tocar/arrastrar: El Lurker nada hacia donde tocaste
     *
     * MODO CALIBRACIÓN (CALIBRATION_MODE = true):
     * - Double-tap: Cambiar pez seleccionado
     * - Tap esquina superior izquierda: Cambiar modo de ajuste
     * - Arrastrar: Ajustar valor según modo
     */
    @Override
    public boolean onTouchEvent(float normalizedX, float normalizedY, int action) {
        if (CALIBRATION_MODE) {
            return handleCalibrationTouch(normalizedX, normalizedY, action);
        }

        // 👆 LURKER sigue el dedo del usuario
        if (action == android.view.MotionEvent.ACTION_DOWN ||
            action == android.view.MotionEvent.ACTION_MOVE) {

            if (abyssalLurker != null) {
                // Convertir coordenadas normalizadas (-1 a 1) a espacio mundo
                float worldX = normalizedX * 0.8f;
                float worldY = normalizedY * 0.9f;
                abyssalLurker.setTouchTarget(worldX, worldY);
                return true;
            }
        }

        return false;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎛️ CALIBRACIÓN - Sistema de ajuste por touch (para desarrollo)
    // ═══════════════════════════════════════════════════════════════════════════

    private boolean handleCalibrationTouch(float x, float y, int action) {
        switch (action) {
            case android.view.MotionEvent.ACTION_DOWN:
                long now = System.currentTimeMillis();

                // Double-tap: cambiar pez seleccionado
                if (now - lastTapTime < 300) {
                    selectedFish = (selectedFish + 1) % 2;
                    String fishName = selectedFish == 0 ? "🐟 LURKER" : "🐉 LEVIATHAN";
                    Log.d("CALIBRATE", "═══ SELECCIONADO: " + fishName + " ═══");
                    logCurrentFish("SELECTED");
                }
                // Tap esquina superior izquierda: cambiar modo
                else if (x < -0.7f && y > 0.7f) {
                    adjustMode = (adjustMode + 1) % 5;
                    Log.d("CALIBRATE", "📐 MODO: " + MODE_NAMES[adjustMode]);
                }

                lastTapTime = now;
                lastTouchX = x;
                lastTouchY = y;
                return true;

            case android.view.MotionEvent.ACTION_MOVE:
                float dx = x - lastTouchX;
                float dy = y - lastTouchY;
                applyCalibrationDelta(dx, dy);
                lastTouchX = x;
                lastTouchY = y;
                return true;

            case android.view.MotionEvent.ACTION_UP:
                logCurrentFish("FINAL");
                return true;
        }
        return false;
    }

    private void applyCalibrationDelta(float dx, float dy) {
        float posSens = 0.5f;
        float rotSens = 50f;
        float scaleSens = 0.3f;

        if (selectedFish == 0 && abyssalLurker != null) {
            switch (adjustMode) {
                case 0: abyssalLurker.adjustPosition(dx * posSens, -dy * posSens, 0); break;
                case 1: abyssalLurker.adjustPosition(0, 0, dy * posSens); break;
                case 2: abyssalLurker.adjustRotation(-dy * rotSens, dx * rotSens, 0); break;
                case 3: abyssalLurker.adjustRotation(0, 0, dx * rotSens); break;
                case 4: abyssalLurker.adjustScale(-dy * scaleSens); break;
            }
        } else if (selectedFish == 1 && abyssalLeviathan != null) {
            switch (adjustMode) {
                case 0: abyssalLeviathan.adjustPosition(dx * posSens, -dy * posSens, 0); break;
                case 1: abyssalLeviathan.adjustPosition(0, 0, dy * posSens); break;
                case 2: abyssalLeviathan.adjustRotation(-dy * rotSens, dx * rotSens, 0); break;
                case 3: abyssalLeviathan.adjustRotation(0, 0, dx * rotSens); break;
                case 4: abyssalLeviathan.adjustScale(-dy * scaleSens); break;
            }
        }
    }

    private void logCurrentFish(String event) {
        if (selectedFish == 0 && abyssalLurker != null) {
            abyssalLurker.logCalibration(event);
        } else if (selectedFish == 1 && abyssalLeviathan != null) {
            abyssalLeviathan.logCalibration(event);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 📝 NOTA PARA CLAUDE CODE:
    // ═══════════════════════════════════════════════════════════════════════════
    //
    // Esta escena es un EJEMPLO AVANZADO de interacción entre objetos 3D:
    //
    // PATRÓN DE INTERACCIÓN ENTRE OBJETOS:
    // 1. Actualizar el objeto "dominante" primero (Leviathan)
    // 2. Pasar su posición al objeto "dependiente" (Lurker)
    // 3. El objeto dependiente reacciona (huye del Leviathan)
    //
    // PATRÓN DE SISTEMA DE PARTÍCULAS SINCRONIZADO:
    // 1. Crear sistema de partículas (BubbleSystem)
    // 2. En update(), usar timers para spawn periódico
    // 3. Obtener posición de origen del objeto 3D (getMouthX/Y)
    // 4. Spawn partículas desde esa posición
    //
    // SISTEMA DE CALIBRACIÓN:
    // Para recalibrar posiciones de los peces:
    // 1. Cambiar CALIBRATION_MODE = true
    // 2. Compilar e instalar
    // 3. Usar gestos de touch para ajustar
    // 4. Ver LogCat con tag "CALIBRATE" para valores
    // 5. Copiar valores al código de AbyssalLurker3D/AbyssalLeviathan3D
    // 6. Cambiar CALIBRATION_MODE = false
    // ═══════════════════════════════════════════════════════════════════════════
}
