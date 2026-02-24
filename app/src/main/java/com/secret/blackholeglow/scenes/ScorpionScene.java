package com.secret.blackholeglow.scenes;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;

/**
 * SCORPION SCENE - Mortal Kombat Scorpion
 * Video wallpaper con tema de fuego.
 */
public class ScorpionScene extends BaseVideoScene {

    @Override
    public String getName() {
        return "SCORPION";
    }

    @Override
    public String getDescription() {
        return "Scorpion - GET OVER HERE!";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_scorpion;
    }

    @Override
    protected String getVideoFileName() {
        return "scorpion_scene.mp4";
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.PYRALIS;
    }
}
