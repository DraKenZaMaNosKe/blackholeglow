package com.secret.blackholeglow.scenes;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;

/**
 * TREN NOCTURNO SCENE - Pixel art night train
 * Video wallpaper estilo retro pixelado.
 */
public class TrenNocturnoScene extends BaseVideoScene {

    @Override
    public String getName() {
        return "TREN_NOCTURNO";
    }

    @Override
    public String getDescription() {
        return "Tren Nocturno - Pixel Night Train";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_tren_nocturno;
    }

    @Override
    protected String getVideoFileName() {
        return "tren_nocturno.mp4";
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.SYNTHWAVE;
    }
}
