package com.secret.blackholeglow.scenes;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;

/**
 * SUPERCAMPEONES SCENE - Captain Tsubasa golden kick.
 */
public class SupercampeonesScene extends BaseVideoScene {

    @Override
    public String getName() {
        return "SUPERCAMPEONES";
    }

    @Override
    public String getDescription() {
        return "Captain Tsubasa - Super Strike";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_supercampeones;
    }

    @Override
    protected String getVideoFileName() {
        return "supercampeones_scene.mp4";
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.DEFAULT;
    }
}
