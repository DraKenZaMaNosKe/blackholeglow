package com.secret.blackholeglow.scenes;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;

/**
 * GOKU KAMEHAMEHA SCENE - SSJ Goku unleashing Kamehameha energy blast.
 */
public class GokuKamehameScene extends BaseVideoScene {

    @Override
    public String getName() {
        return "GOKU_KAMEHAME";
    }

    @Override
    public String getDescription() {
        return "Goku Kamehameha - Ki Energy Blast";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_goku_kamehame;
    }

    @Override
    protected String getVideoFileName() {
        return "goku_kamehame_scene.mp4";
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.KAMEHAMEHA;
    }
}
