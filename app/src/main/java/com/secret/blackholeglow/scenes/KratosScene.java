package com.secret.blackholeglow.scenes;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                    ⚔️ KRATOS SCENE - God of War                         ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Kratos, el Dios de la Guerra, en una escena épica con ecualizador     ║
 * ║  rojo/naranja estilo llamas de furia.                                  ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class KratosScene extends BaseVideoScene {

    @Override
    public String getName() {
        return "KRATOS";
    }

    @Override
    public String getDescription() {
        return "Kratos - God of War";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_kratos;
    }

    @Override
    protected String getVideoFileName() {
        return "kratos_scene.mp4";
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.KRATOS;
    }
}
