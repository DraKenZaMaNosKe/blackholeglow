package com.secret.blackholeglow.scenes;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║              🦁 THE HUMAN PREDATOR - Guerrero vs León                    ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Guerrero prehistórico con poderes venenosos vs león gigante.           ║
 * ║  Jungla volcánica, león envenenado, combate épico slow-motion.          ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  📖 ESCENA BASADA EN VIDEO:                                              ║
 * ║  Hereda de BaseVideoScene para reproducción automática de video.         ║
 * ║                                                                          ║
 * ║  ✅ Video: guerrerovsleon.mp4                                            ║
 * ║  ✅ Tema: WALKING_DEAD (verde tóxico / rojo sangre)                      ║
 * ║  ✅ Clock + Battery incluidos automáticamente                            ║
 * ║                                                                          ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class TheHumanPredatorScene extends BaseVideoScene {

    @Override
    public String getName() {
        return "THE_HUMAN_PREDATOR";
    }

    @Override
    public String getDescription() {
        return "The Human Predator - Guerrero vs León";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_human_predator;
    }

    @Override
    protected String getVideoFileName() {
        return "guerrerovsleon.mp4";
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.WALKING_DEAD;  // Verde tóxico + rojo sangre
    }
}
