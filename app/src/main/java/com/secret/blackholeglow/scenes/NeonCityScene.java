package com.secret.blackholeglow.scenes;

import android.util.Log;

import com.secret.blackholeglow.DeLorean3D;
import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                    🚗 NEON CITY SCENE - Synthwave                        ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  DeLorean en carretera infinita estilo synthwave/retrowave.              ║
 * ║  Video de fondo con grid neón + modelo 3D del carro.                     ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  📖 EJEMPLO DE ESCENA CON OBJETO 3D:                                     ║
 * ║  Esta escena demuestra cómo agregar objetos 3D adicionales               ║
 * ║  usando los hooks de BaseVideoScene.                                     ║
 * ║                                                                          ║
 * ║  ✅ Video: neoncityScene.mp4                                             ║
 * ║  ✅ Tema: SYNTHWAVE (Hot Pink/Cyan/Magenta - retrowave 80s)              ║
 * ║  ✅ Objeto 3D: DeLorean3D con calibración por touch                      ║
 * ║  ✅ Clock + Battery incluidos automáticamente                            ║
 * ║                                                                          ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * @see BaseVideoScene para documentación completa de la clase base
 * @see DeLorean3D para el modelo 3D del carro
 */
public class NeonCityScene extends BaseVideoScene {
    private static final String TAG = "NeonCityScene";

    // ═══════════════════════════════════════════════════════════════════════════
    // 🚗 OBJETO 3D ESPECÍFICO DE ESTA ESCENA
    // ═══════════════════════════════════════════════════════════════════════════

    /** Modelo 3D del DeLorean con movimiento automático */
    private DeLorean3D delorean;

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎯 MÉTODOS OBLIGATORIOS - Configuración básica de la escena
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public String getName() {
        return "NEON_CITY";
    }

    @Override
    public String getDescription() {
        return "Neon City - Synthwave Retrowave";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_neoncity;
    }

    @Override
    protected String getVideoFileName() {
        return "neoncityScene.mp4";
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.SYNTHWAVE;  // 🌆 Retrowave 80s
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 🔧 HOOKS PARA OBJETO 3D ADICIONAL (DeLorean)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Configura el DeLorean 3D.
     * Se llama automáticamente después de configurar video/ecualizador/clock/battery.
     */
    @Override
    protected void setupSceneSpecific() {
        try {
            delorean = new DeLorean3D(context, textureManager);
            delorean.setCameraController(camera);
            Log.d(TAG, "✅ DeLorean 3D cargado");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error DeLorean3D: " + e.getMessage());
        }
    }

    /**
     * Actualiza el DeLorean cada frame.
     */
    @Override
    protected void updateSceneSpecific(float deltaTime) {
        if (delorean != null) {
            delorean.update(deltaTime);
        }
    }

    /**
     * Dibuja el DeLorean sobre el video pero debajo de la UI.
     */
    @Override
    protected void drawSceneSpecific() {
        if (delorean != null) {
            delorean.draw();
        }
    }

    /**
     * Libera recursos del DeLorean.
     */
    @Override
    protected void releaseSceneSpecificResources() {
        if (delorean != null) {
            delorean.release();
            delorean = null;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 👆 TOUCH - Pasa eventos al DeLorean para calibración
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Maneja eventos de touch para calibrar el DeLorean.
     *
     * 📖 SISTEMA DE CALIBRACIÓN:
     * - TAP: Cambia el modo (POSITION_XY → POSITION_Z → ROTATE → SCALE)
     * - DRAG: Ajusta según el modo activo
     * - Los valores se loguean en LogCat para copiar al código
     */
    @Override
    public boolean onTouchEvent(float normalizedX, float normalizedY, int action) {
        if (delorean != null) {
            boolean handled = delorean.onTouchEvent(normalizedX, normalizedY, action);
            if (handled) return true;
        }
        return super.onTouchEvent(normalizedX, normalizedY, action);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 📝 NOTA PARA CLAUDE CODE:
    // ═══════════════════════════════════════════════════════════════════════════
    //
    // Esta escena es un EJEMPLO INTERMEDIO de BaseVideoScene con objeto 3D.
    //
    // Patrón para agregar objetos 3D:
    // 1. Declarar el objeto como campo de la clase
    // 2. Inicializarlo en setupSceneSpecific()
    // 3. Actualizarlo en updateSceneSpecific()
    // 4. Dibujarlo en drawSceneSpecific()
    // 5. Liberarlo en releaseSceneSpecificResources()
    // 6. (Opcional) Manejar touch para calibración
    //
    // El DeLorean tiene su propio sistema de calibración por touch.
    // Ver DeLorean3D.java para más detalles.
    // ═══════════════════════════════════════════════════════════════════════════
}
