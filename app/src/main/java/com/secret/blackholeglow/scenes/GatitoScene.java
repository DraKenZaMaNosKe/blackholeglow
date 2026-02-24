package com.secret.blackholeglow.scenes;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;

/**
 * GATITO SCENE - Gatito animado
 * Video wallpaper con gatito adorable.
 */
public class GatitoScene extends BaseVideoScene {

    @Override
    public String getName() {
        return "GATITO";
    }

    @Override
    public String getDescription() {
        return "Gatito - Cute Cat Animation";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_gatito;
    }

    @Override
    protected String getVideoFileName() {
        return "gatito_scene_final.mp4";
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.DEFAULT;
    }
}
