package com.secret.blackholeglow.scenes;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                    🦸 SUPERMAN SCENE - Man of Steel                      ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Video de Superman volando con ecualizador rojo/azul/amarillo.           ║
 * ║  Estilo épico cinematográfico - El Hombre de Acero.                      ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  📖 ESCENA BASADA EN VIDEO:                                              ║
 * ║  Hereda de BaseVideoScene para reproducción automática de video.         ║
 * ║                                                                          ║
 * ║  ✅ Video: superman_scene.mp4                                            ║
 * ║  ✅ Tema: SUPERMAN (rojo/azul/amarillo - traje clásico)                  ║
 * ║  ✅ Clock + Battery incluidos automáticamente                            ║
 * ║                                                                          ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * @see BaseVideoScene para documentación completa de la clase base
 */
public class SupermanScene extends BaseVideoScene {

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎯 MÉTODOS OBLIGATORIOS - Configuración básica de la escena
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public String getName() {
        return "SUPERMAN";
    }

    @Override
    public String getDescription() {
        return "Superman - Man of Steel";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_superman;
    }

    @Override
    protected String getVideoFileName() {
        return "superman_scene.mp4";
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.SUPERMAN;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 📝 NOTA PARA CLAUDE CODE:
    // ═══════════════════════════════════════════════════════════════════════════
    //
    // Esta escena es similar a GokuScene - solo video sin objetos 3D adicionales.
    // Todo lo demás (video, ecualizador, clock, battery, pause/resume,
    // auto-recovery, liberación de recursos) se hereda automáticamente.
    //
    // Si necesitas agregar objetos 3D adicionales, usa los hooks:
    // - setupSceneSpecific()
    // - updateSceneSpecific(float deltaTime)
    // - drawSceneSpecific()
    // - releaseSceneSpecificResources()
    //
    // Ver WalkingDeadScene para un ejemplo con objetos 3D adicionales.
    // ═══════════════════════════════════════════════════════════════════════════
}
