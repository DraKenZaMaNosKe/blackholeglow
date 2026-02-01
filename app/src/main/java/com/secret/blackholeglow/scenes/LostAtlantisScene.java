package com.secret.blackholeglow.scenes;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║              🏛️ LOST ATLANTIS SCENE - Ciudad Sumergida                   ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Templo ancestral sumergido en aguas turquesa con energía mística,      ║
 * ║  pétalos de cerezo flotando, linternas doradas y peces nadando.         ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  📖 ESCENA BASADA EN VIDEO:                                              ║
 * ║  Hereda de BaseVideoScene para reproducción automática de video.         ║
 * ║                                                                          ║
 * ║  ✅ Video: lostatlanstis.mp4                                             ║
 * ║  ✅ Tema: ATLANTIS (cyan / turquesa / dorado)                            ║
 * ║  ✅ Clock + Battery incluidos automáticamente                            ║
 * ║                                                                          ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * @see BaseVideoScene para documentación completa de la clase base
 */
public class LostAtlantisScene extends BaseVideoScene {

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎯 MÉTODOS OBLIGATORIOS - Configuración básica de la escena
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public String getName() {
        return "LOST_ATLANTIS";
    }

    @Override
    public String getDescription() {
        return "Lost Atlantis - Ancient Underwater Temple";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_lost_atlantis;
    }

    @Override
    protected String getVideoFileName() {
        return "lostatlanstis.mp4";
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.ATLANTIS;
    }
}
