package com.secret.blackholeglow.scenes;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;

/**
 * ⚔️ KRATOS vs CYCLOPS — Imagen estática con UI overlay
 *
 * Extiende BaseParallaxScene para reutilizar: ecualizador, reloj, batería.
 * SIN parallax (gyro desactivado). Imagen estática solamente.
 */
public class KratosCyclopsScene extends BaseParallaxScene {

    @Override
    public String getName() {
        return "KRATOS_CYCLOPS";
    }

    @Override
    public String getDescription() {
        return "Kratos vs Cyclops - God of War";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_kratos_cyclops;
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.KRATOS;
    }

    @Override
    protected ParallaxLayer[] getLayers() {
        return new ParallaxLayer[] {
            new ParallaxLayer("kratos_cyclops_bg.jpg", null, 0f, 1.0f, true)
        };
    }

    @Override
    protected float getGyroSensitivity() {
        return 0f;
    }
}
