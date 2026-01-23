package com.secret.blackholeglow.scenes;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║          🌳 ADVENTURE TIME SCENE - Hora de Aventura Fogata              ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Video de Finn, Jake, Tronquitos y Princess Bubblegum alrededor de      ║
 * ║  una fogata en el bosque al atardecer.                                   ║
 * ║                                                                          ║
 * ║  Estilo: Cozy campfire vibes con ecualizador cálido.                    ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  📖 EJEMPLO DE ESCENA SIMPLE:                                            ║
 * ║  Escena minimalista usando BaseVideoScene.                               ║
 * ║                                                                          ║
 * ║  ✅ Video: escenaHDA.mp4                                                 ║
 * ║  ✅ Tema: PYRALIS (rojo/naranja/amarillo - fuego de fogata)             ║
 * ║  ✅ Clock + Battery incluidos automáticamente                            ║
 * ║                                                                          ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * @see BaseVideoScene para documentación completa de la clase base
 */
public class AdventureTimeScene extends BaseVideoScene {

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎯 MÉTODOS OBLIGATORIOS - Configuración básica de la escena
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public String getName() {
        return "ADVENTURE_TIME";
    }

    @Override
    public String getDescription() {
        return "Hora de Aventura - Fogata con Finn y Jake";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.hdapreview;
    }

    @Override
    protected String getVideoFileName() {
        return "escenaHDA.mp4";
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.PYRALIS;  // 🔥 Colores fuego para fogata
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 📝 NOTA PARA CLAUDE CODE:
    // ═══════════════════════════════════════════════════════════════════════════
    //
    // El tema PYRALIS (fuego) combina perfectamente con la fogata del video.
    // Los colores rojo → naranja → amarillo simulan las llamas.
    //
    // Si quisieras agregar partículas de fuego o chispas flotantes,
    // implementarías setupSceneSpecific() y drawSceneSpecific().
    // ═══════════════════════════════════════════════════════════════════════════
}
