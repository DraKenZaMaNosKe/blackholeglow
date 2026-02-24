package com.secret.blackholeglow.scenes;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;

/**
 * GATITO DJ SCENE - Gatito DJ bailando
 * Video wallpaper con gatito animado haciendo DJ.
 */
public class GatitoDJScene extends BaseVideoScene {

    @Override
    public String getName() {
        return "GATITO_DJ";
    }

    @Override
    public String getDescription() {
        return "Gatito DJ - Dancing Cat";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_gatito_dj;
    }

    @Override
    protected String getVideoFileName() {
        return "gatito_dance.mp4";
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.DEFAULT;
    }
}
