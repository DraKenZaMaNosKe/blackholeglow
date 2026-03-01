package com.secret.blackholeglow.scenes;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;

/**
 * ITACHI SCENE - Sharingan eye video wallpaper.
 */
public class ItachiScene extends BaseVideoScene {

    @Override
    public String getName() {
        return "ITACHI";
    }

    @Override
    public String getDescription() {
        return "Itachi Uchiha - Sharingan";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_itachi;
    }

    @Override
    protected String getVideoFileName() {
        return "itachi_scene_00.mp4";
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.DEFAULT;
    }
}
