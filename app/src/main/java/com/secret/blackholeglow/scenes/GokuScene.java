package com.secret.blackholeglow.scenes;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                    🐉 GOKU SCENE - Kamehameha                            ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Video de Goku lanzando Kamehameha con ecualizador azul energía Ki.      ║
 * ║  Estilo Dragon Ball FighterZ - visual espectacular.                      ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  📖 EJEMPLO DE ESCENA SIMPLE:                                            ║
 * ║  Esta escena demuestra el uso mínimo de BaseVideoScene.                  ║
 * ║  Solo necesita implementar 5 métodos abstractos.                         ║
 * ║                                                                          ║
 * ║  ✅ Video: gokufinalkamehamehaHD.mp4                                     ║
 * ║  ✅ Tema: KAMEHAMEHA (azul/cyan/blanco - energía Ki)                     ║
 * ║  ✅ Clock + Battery incluidos automáticamente                            ║
 * ║                                                                          ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * @see BaseVideoScene para documentación completa de la clase base
 */
public class GokuScene extends BaseVideoScene {

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎯 MÉTODOS OBLIGATORIOS - Configuración básica de la escena
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public String getName() {
        return "GOKU";
    }

    @Override
    public String getDescription() {
        return "Goku Kamehameha - Energía Ki";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_goku;
    }

    @Override
    protected String getVideoFileName() {
        return "gokufinalkamehamehaHD.mp4";
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.KAMEHAMEHA;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 📝 NOTA PARA CLAUDE CODE:
    // ═══════════════════════════════════════════════════════════════════════════
    //
    // Esta escena es un ejemplo MINIMALISTA de BaseVideoScene.
    // Todo lo demás (video, ecualizador, clock, battery, pause/resume,
    // auto-recovery, liberación de recursos) se hereda automáticamente.
    //
    // Si necesitas agregar objetos 3D adicionales, usa los hooks:
    // - setupSceneSpecific()
    // - updateSceneSpecific(float deltaTime)
    // - drawSceneSpecific()
    // - releaseSceneSpecificResources()
    //
    // Ver WalkingDeadScene para un ejemplo con objetos 3D adicionales.
    // ═══════════════════════════════════════════════════════════════════════════
}
