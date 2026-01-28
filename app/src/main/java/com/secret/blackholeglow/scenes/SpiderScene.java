package com.secret.blackholeglow.scenes;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                    🕷️ SPIDER SCENE - Black Spider                        ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Araña gigante con ojos rojos brillantes en atmósfera oscura de terror.  ║
 * ║  Video con efectos de respiración, ojos pulsantes y niebla roja.         ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  📖 ESCENA BASADA EN VIDEO:                                              ║
 * ║  Hereda de BaseVideoScene para reproducción automática de video.         ║
 * ║                                                                          ║
 * ║  ✅ Video: spiderscene.mp4                                               ║
 * ║  ✅ Tema: SPIDER (rojo sangre / negro / carmesí)                         ║
 * ║  ✅ Clock + Battery incluidos automáticamente                            ║
 * ║                                                                          ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * @see BaseVideoScene para documentación completa de la clase base
 */
public class SpiderScene extends BaseVideoScene {

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎯 MÉTODOS OBLIGATORIOS - Configuración básica de la escena
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public String getName() {
        return "SPIDER";
    }

    @Override
    public String getDescription() {
        return "Black Spider - Terror Arachnid";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_spider;
    }

    @Override
    protected String getVideoFileName() {
        return "spiderscene.mp4";
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.SPIDER;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 📝 NOTA PARA CLAUDE CODE:
    // ═══════════════════════════════════════════════════════════════════════════
    //
    // Esta escena es similar a SupermanScene - solo video sin objetos 3D.
    // Todo lo demás (video, ecualizador, clock, battery, pause/resume,
    // auto-recovery, liberación de recursos) se hereda automáticamente.
    //
    // TEMA DE COLORES:
    // - Rojo sangre/carmesí (ojos de la araña)
    // - Negro profundo (cuerpo/sombras)
    // - Púrpura oscuro (niebla/ambiente)
    //
    // Si necesitas agregar objetos 3D adicionales, usa los hooks:
    // - setupSceneSpecific()
    // - updateSceneSpecific(float deltaTime)
    // - drawSceneSpecific()
    // - releaseSceneSpecificResources()
    // ═══════════════════════════════════════════════════════════════════════════
}
