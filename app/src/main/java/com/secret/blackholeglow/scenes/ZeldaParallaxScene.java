package com.secret.blackholeglow.scenes;

import android.opengl.GLES30;
import android.util.Log;

import com.secret.blackholeglow.R;
import com.secret.blackholeglow.Clock3D;
import com.secret.blackholeglow.Link3D;
import com.secret.blackholeglow.EqualizerBarsDJ;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                                      ║
 * ║          🗡️ ZELDA PARALLAX SCENE - Breath of the Wild Style                          ║
 * ║                                                                                      ║
 * ╠══════════════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                                      ║
 * ║  ╔════════════════════════════════════════════════════════════════════════════════╗  ║
 * ║  ║                     ⚠️ NO BORRAR POR FAVOR - DOCUMENTACIÓN ⚠️                  ║  ║
 * ║  ║                                                                                ║  ║
 * ║  ║  Esta escena demuestra cómo usar BaseParallaxScene con:                        ║  ║
 * ║  ║  • Múltiples capas PNG con depth maps                                          ║  ║
 * ║  ║  • Modelo 3D (Link) con calibración touch                                      ║  ║
 * ║  ║  • Tema visual ZELDA para UI                                                   ║  ║
 * ║  ╚════════════════════════════════════════════════════════════════════════════════╝  ║
 * ║                                                                                      ║
 * ╠══════════════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                                      ║
 * ║  📖 TECNOLOGÍA: Depth Map Displacement                                               ║
 * ║  ══════════════════════════════════                                                  ║
 * ║                                                                                      ║
 * ║  Esta escena usa el MISMO sistema de parallax que la clase base:                     ║
 * ║  • Cada capa tiene una imagen COLOR + una imagen DEPTH (grayscale)                   ║
 * ║  • El shader desplaza píxeles según su profundidad                                   ║
 * ║  • Blanco en depth map = cerca = se mueve MÁS con giroscopio                         ║
 * ║  • Negro en depth map = lejos = se mueve MENOS con giroscopio                        ║
 * ║                                                                                      ║
 * ╠══════════════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                                      ║
 * ║  📖 CAPAS DE LA ESCENA:                                                              ║
 * ║  ═══════════════════════                                                             ║
 * ║                                                                                      ║
 * ║  ┌─────────────────────────────────────────────────────────────────────────────┐     ║
 * ║  │  CAPA  │ ARCHIVO              │ DEPTH MAP            │ EFECTO              │     ║
 * ║  ├─────────────────────────────────────────────────────────────────────────────┤     ║
 * ║  │  1     │ zelda_fondo.png      │ zelda_fondo_depth    │ Parallax 3D         │     ║
 * ║  │  2     │ zelda_paisaje.png    │ (ninguno)            │ Estático            │     ║
 * ║  │  3     │ zelda_piedra.png     │ (ninguno)            │ Estático            │     ║
 * ║  │  4     │ Link3D               │ N/A                  │ Modelo 3D           │     ║
 * ║  └─────────────────────────────────────────────────────────────────────────────┘     ║
 * ║                                                                                      ║
 * ║  NOTA: Link NO es una capa PNG, es un modelo 3D renderizado en drawSceneSpecific()  ║
 * ║                                                                                      ║
 * ╠══════════════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                                      ║
 * ║  📖 INTERACCIÓN CON LINK 3D:                                                         ║
 * ║  ═════════════════════════════                                                       ║
 * ║                                                                                      ║
 * ║  El modelo 3D de Link soporta calibración por touch:                                 ║
 * ║  • 1 dedo: Mover posición X/Y                                                        ║
 * ║  • 2 dedos (pinch): Escalar                                                          ║
 * ║  • 2 dedos (rotar): Cambiar rotación Y                                               ║
 * ║                                                                                      ║
 * ║  Para habilitar calibración, usa: link3D.setTouchEnabled(true);                      ║
 * ║  Los valores finales se loguean al soltar el touch.                                  ║
 * ║                                                                                      ║
 * ╚══════════════════════════════════════════════════════════════════════════════════════╝
 *
 * @author Claude Code - Migración a BaseParallaxScene
 * @version 2.0.0
 * @see BaseParallaxScene para documentación del sistema de parallax
 * @see Link3D para el modelo 3D de Link
 */
public class ZeldaParallaxScene extends BaseParallaxScene {
    private static final String TAG = "ZeldaParallax";

    // ═══════════════════════════════════════════════════════════════════════════════════
    // ⚠️ NO BORRAR POR FAVOR - ARCHIVOS DE TEXTURAS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /** Imagen del cielo/fondo con montañas */
    private static final String FILE_CIELO = "zelda_fondo.png";
    /** Depth map del cielo para efecto parallax */
    private static final String FILE_CIELO_DEPTH = "zelda_fondo_depth.png";
    /** Paisaje medio (árboles, etc) */
    private static final String FILE_PAISAJE = "zelda_paisaje.png";
    /** Piedras del primer plano */
    private static final String FILE_PIEDRA = "zelda_piedra.png";

    // ═══════════════════════════════════════════════════════════════════════════════════
    // ⚠️ NO BORRAR POR FAVOR - CONFIGURACIÓN DEL DEPTH MAP
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * Escala del efecto depth para el fondo.
     * • 0.0f = sin movimiento
     * • 0.08f = efecto sutil (recomendado)
     * • 0.15f = efecto pronunciado
     */
    private static final float DEPTH_SCALE_CIELO = 0.08f;

    // ═══════════════════════════════════════════════════════════════════════════════════
    // ⚠️ NO BORRAR POR FAVOR - LINK 3D
    // ═══════════════════════════════════════════════════════════════════════════════════

    /** Modelo 3D de Link */
    private Link3D link3D;

    /** Toggle para habilitar/deshabilitar Link 3D */
    private boolean useLink3D = true;

    // ═══════════════════════════════════════════════════════════════════════════════════
    //
    //                    📖 MÉTODOS OBLIGATORIOS DE BaseParallaxScene
    //
    //                    ⚠️ NO BORRAR POR FAVOR - DOCUMENTACIÓN ⚠️
    //
    // ═══════════════════════════════════════════════════════════════════════════════════

    @Override
    public String getName() {
        return "ZELDA_BOTW";
    }

    @Override
    public String getDescription() {
        return "Zelda BOTW - 3D Depth Parallax";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_zelda;
    }

    /**
     * ╔════════════════════════════════════════════════════════════════════════════════╗
     * ║  📖 getTheme() - Tema visual de la escena                                      ║
     * ╠════════════════════════════════════════════════════════════════════════════════╣
     * ║                                                                                ║
     * ║  Usa el tema ZELDA que tiene colores:                                          ║
     * ║  • Verde Hyrule para las barras del ecualizador                                ║
     * ║  • Dorado para acentos y texto                                                 ║
     * ║  • Estilo inspirado en Breath of the Wild                                      ║
     * ║                                                                                ║
     * ╚════════════════════════════════════════════════════════════════════════════════╝
     */
    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.ZELDA;
    }

    /**
     * ╔════════════════════════════════════════════════════════════════════════════════╗
     * ║  📖 getLayers() - Define las capas parallax de la escena                       ║
     * ╠════════════════════════════════════════════════════════════════════════════════╣
     * ║                                                                                ║
     * ║  ORDEN DE CAPAS (atrás → adelante):                                            ║
     * ║  1. Cielo con depth map - se mueve con giroscopio                              ║
     * ║  2. Paisaje - estático                                                         ║
     * ║  3. Piedra - estático                                                          ║
     * ║                                                                                ║
     * ║  NOTA: Link 3D NO es una capa, se dibuja en drawSceneSpecific()                ║
     * ║                                                                                ║
     * ╚════════════════════════════════════════════════════════════════════════════════╝
     */
    @Override
    protected ParallaxLayer[] getLayers() {
        return new ParallaxLayer[] {
            // ═══════════════════════════════════════════════════════════════════════
            // CAPA 1: CIELO/FONDO - CON DEPTH MAP
            // ═══════════════════════════════════════════════════════════════════════
            // Esta capa se mueve con el giroscopio gracias al depth map.
            // Los objetos blancos en el depth map se mueven más.
            new ParallaxLayer(
                FILE_CIELO,           // Imagen de color
                FILE_CIELO_DEPTH,     // Depth map
                DEPTH_SCALE_CIELO,    // Intensidad del efecto (0.08 = sutil)
                1.0f,                 // Alpha (opaco)
                true                  // useCoverMode (ajustar aspect ratio)
            ),

            // ═══════════════════════════════════════════════════════════════════════
            // CAPA 2: PAISAJE - ESTÁTICO
            // ═══════════════════════════════════════════════════════════════════════
            // Árboles y vegetación media. Sin depth map = no se mueve.
            new ParallaxLayer(FILE_PAISAJE),

            // ═══════════════════════════════════════════════════════════════════════
            // CAPA 3: PIEDRAS - ESTÁTICO
            // ═══════════════════════════════════════════════════════════════════════
            // Rocas del primer plano. Sin depth map = no se mueve.
            new ParallaxLayer(FILE_PIEDRA)
        };
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    //
    //                    📖 CONFIGURACIÓN PERSONALIZADA
    //
    //                    ⚠️ NO BORRAR POR FAVOR - DOCUMENTACIÓN ⚠️
    //
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * Color de fondo (clear color) - Azul cielo de Hyrule
     */
    @Override
    protected float[] getBackgroundColor() {
        return new float[] { 0.4f, 0.6f, 0.8f, 1.0f };
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    //
    //                    📖 HOOKS PARA LINK 3D
    //
    //                    ⚠️ NO BORRAR POR FAVOR - DOCUMENTACIÓN ⚠️
    //
    //  Estos hooks configuran, actualizan y dibujan el modelo 3D de Link.
    //
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * ╔════════════════════════════════════════════════════════════════════════════════╗
     * ║  📖 setupSceneSpecific() - Crear Link 3D                                       ║
     * ╠════════════════════════════════════════════════════════════════════════════════╣
     * ║                                                                                ║
     * ║  VALORES DE CALIBRACIÓN FINALES:                                               ║
     * ║  Estos valores fueron ajustados mediante touch interactivo.                    ║
     * ║  Para recalibrar: setTouchEnabled(true), ajustar, copiar valores del log.      ║
     * ║                                                                                ║
     * ║  POSICIÓN:                                                                     ║
     * ║  • X = -0.490f → Ligeramente a la izquierda del centro                         ║
     * ║  • Y = -1.045f → En la parte inferior de la pantalla                           ║
     * ║  • Z = 0.000f  → Sin profundidad adicional                                     ║
     * ║                                                                                ║
     * ║  ESCALA:                                                                       ║
     * ║  • 0.439f → Tamaño que encaja bien con el paisaje                              ║
     * ║                                                                                ║
     * ║  ROTACIÓN:                                                                     ║
     * ║  • 9.2f → Ligeramente girado para verse más natural                            ║
     * ║                                                                                ║
     * ╚════════════════════════════════════════════════════════════════════════════════╝
     */
    @Override
    protected void setupSceneSpecific() {
        if (!useLink3D) return;

        try {
            link3D = new Link3D(context);

            // ═══════════════════════════════════════════════════════════════════
            // ⚠️ NO BORRAR - VALORES CALIBRADOS
            // ═══════════════════════════════════════════════════════════════════
            link3D.setPosition(-0.490f, -1.045f, 0.000f);  // Posición final
            link3D.setScale(0.439f);                        // Escala final
            link3D.setRotationY(9.2f);                      // Rotación final

            // Para recalibrar, cambiar a true y ajustar con touch
            link3D.setTouchEnabled(false);

            link3D.setScreenSize(screenWidth, screenHeight);
            Log.d(TAG, "✅ Link 3D activado (valores calibrados)");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error Link3D: " + e.getMessage());
            useLink3D = false;
        }
    }

    /**
     * Actualiza Link 3D cada frame
     */
    @Override
    protected void updateSceneSpecific(float deltaTime) {
        if (link3D != null) {
            link3D.update(deltaTime);
        }
    }

    /**
     * ╔════════════════════════════════════════════════════════════════════════════════╗
     * ║  📖 drawSceneSpecific() - Dibujar Link 3D                                      ║
     * ╠════════════════════════════════════════════════════════════════════════════════╣
     * ║                                                                                ║
     * ║  Link se dibuja DESPUÉS de las capas parallax, ANTES de la UI.                 ║
     * ║  Esto lo posiciona correctamente sobre el paisaje.                             ║
     * ║                                                                                ║
     * ║  IMPORTANTE: Habilitamos depth test para que Link tenga profundidad correcta.  ║
     * ║                                                                                ║
     * ╚════════════════════════════════════════════════════════════════════════════════╝
     */
    @Override
    protected void drawSceneSpecific() {
        if (link3D != null && useLink3D) {
            GLES30.glEnable(GLES30.GL_DEPTH_TEST);
            GLES30.glDepthFunc(GLES30.GL_LEQUAL);
            link3D.draw();
            GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        }
    }

    /**
     * Libera recursos de Link 3D
     */
    @Override
    protected void releaseSceneSpecificResources() {
        if (link3D != null) {
            link3D.dispose();
            link3D = null;
            Log.d(TAG, "🗑️ Link3D liberado");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    //
    //                    📖 TOUCH PARA CALIBRAR LINK 3D
    //
    //                    ⚠️ NO BORRAR POR FAVOR - DOCUMENTACIÓN ⚠️
    //
    //  Este método permite ajustar Link 3D interactivamente.
    //  Solo funciona si link3D.setTouchEnabled(true).
    //
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * ╔════════════════════════════════════════════════════════════════════════════════╗
     * ║  📖 onTouchEventRaw() - Calibración de Link 3D                                 ║
     * ╠════════════════════════════════════════════════════════════════════════════════╣
     * ║                                                                                ║
     * ║  CONTROLES:                                                                    ║
     * ║  • 1 dedo + arrastrar: Mover posición X/Y                                      ║
     * ║  • 2 dedos + pinch: Cambiar escala                                             ║
     * ║  • 2 dedos + rotar: Cambiar rotación Y                                         ║
     * ║                                                                                ║
     * ║  FLUJO DE CALIBRACIÓN:                                                         ║
     * ║  1. Activar: link3D.setTouchEnabled(true) en setupSceneSpecific()              ║
     * ║  2. Ejecutar app y ajustar Link con touch                                      ║
     * ║  3. Soltar touch → los valores se loguean en Logcat                            ║
     * ║  4. Copiar valores al código                                                   ║
     * ║  5. Desactivar: link3D.setTouchEnabled(false)                                  ║
     * ║                                                                                ║
     * ╚════════════════════════════════════════════════════════════════════════════════╝
     */
    @Override
    public boolean onTouchEventRaw(android.view.MotionEvent event) {
        if (link3D != null && useLink3D) {
            int action = event.getActionMasked();
            int pointerCount = event.getPointerCount();

            float x1 = event.getX(0);
            float y1 = event.getY(0);

            float x2 = -1, y2 = -1;
            if (pointerCount >= 2) {
                x2 = event.getX(1);
                y2 = event.getY(1);
            }

            link3D.onTouch(action, x1, y1, x2, y2, pointerCount);
            return true;
        }
        return super.onTouchEventRaw(event);
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    //
    //                    📖 SCREEN SIZE - Actualizar Link 3D
    //
    // ═══════════════════════════════════════════════════════════════════════════════════

    @Override
    public void setScreenSize(int width, int height) {
        super.setScreenSize(width, height);
        if (link3D != null) {
            link3D.setScreenSize(width, height);
        }
    }
}
