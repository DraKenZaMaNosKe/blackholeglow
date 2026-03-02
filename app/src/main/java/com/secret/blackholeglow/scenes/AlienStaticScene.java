package com.secret.blackholeglow.scenes;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;

/**
 * ALIEN STATIC - Xenomorph image wallpaper (no video).
 */
public class AlienStaticScene extends BaseParallaxScene {

    @Override
    public String getName() {
        return "ALIEN_STATIC";
    }

    @Override
    public String getDescription() {
        return "Alien Xenomorph - Static";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_alien;
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.ABYSSIA;
    }

    @Override
    protected ParallaxLayer[] getLayers() {
        return new ParallaxLayer[] {
            new ParallaxLayer("alien_bg.webp", null, 0f, 1.0f, true)
        };
    }

    @Override
    protected float getGyroSensitivity() {
        return 0f;
    }
}
