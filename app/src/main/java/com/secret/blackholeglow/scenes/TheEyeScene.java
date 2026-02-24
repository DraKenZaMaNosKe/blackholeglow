package com.secret.blackholeglow.scenes;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;

/**
 * THE EYE SCENE - Ojo gigante misterioso
 * Video wallpaper con iris detallado.
 */
public class TheEyeScene extends BaseVideoScene {

    @Override
    public String getName() {
        return "THE_EYE";
    }

    @Override
    public String getDescription() {
        return "The Eye - All Seeing Iris";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_the_eye;
    }

    @Override
    protected String getVideoFileName() {
        return "theeye_scene.mp4";
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.SPIDER;
    }
}
