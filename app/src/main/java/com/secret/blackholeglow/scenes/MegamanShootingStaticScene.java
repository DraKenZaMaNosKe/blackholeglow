package com.secret.blackholeglow.scenes;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;

/**
 * MEGAMAN SHOOTING STATIC - Megaman image wallpaper (no video).
 */
public class MegamanShootingStaticScene extends BaseParallaxScene {

    @Override
    public String getName() {
        return "MEGAMAN_SHOOTING_STATIC";
    }

    @Override
    public String getDescription() {
        return "Megaman - Energy Blast Static";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_megaman_shooting;
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.KAMEHAMEHA;
    }

    @Override
    protected ParallaxLayer[] getLayers() {
        return new ParallaxLayer[] {
            new ParallaxLayer("megaman_shooting_bg.webp", null, 0f, 1.0f, true)
        };
    }

    @Override
    protected float getGyroSensitivity() {
        return 0f;
    }
}
