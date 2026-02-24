package com.secret.blackholeglow.scenes;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;

/**
 * PIXEL CITY SCENE - Ciudad pixel art animada
 * Video wallpaper con ciudad pixel retro.
 */
public class PixelCityScene extends BaseVideoScene {

    @Override
    public String getName() {
        return "PIXEL_CITY";
    }

    @Override
    public String getDescription() {
        return "Pixel City - Retro Pixel Art City";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_pixel_city;
    }

    @Override
    protected String getVideoFileName() {
        return "pixel_city_scene.mp4";
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.DEFAULT;
    }
}
