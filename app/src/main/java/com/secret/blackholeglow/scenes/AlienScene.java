package com.secret.blackholeglow.scenes;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;

/**
 * ALIEN SCENE - Xenomorph video wallpaper with acid green flames.
 */
public class AlienScene extends BaseVideoScene {

    @Override
    public String getName() {
        return "ALIEN";
    }

    @Override
    public String getDescription() {
        return "Alien Xenomorph - Acid Terror";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_alien;
    }

    @Override
    protected String getVideoFileName() {
        return "alien_scene.mp4";
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.ABYSSIA;
    }
}
