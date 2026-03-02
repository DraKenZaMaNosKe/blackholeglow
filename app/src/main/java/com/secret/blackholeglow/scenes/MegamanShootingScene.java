package com.secret.blackholeglow.scenes;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;

/**
 * MEGAMAN SHOOTING SCENE - Megaman charging energy blast video wallpaper.
 */
public class MegamanShootingScene extends BaseVideoScene {

    @Override
    public String getName() {
        return "MEGAMAN_SHOOTING";
    }

    @Override
    public String getDescription() {
        return "Megaman - Energy Blast";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_megaman_shooting;
    }

    @Override
    protected String getVideoFileName() {
        return "megaman_shooting_scene.mp4";
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.KAMEHAMEHA;
    }
}
