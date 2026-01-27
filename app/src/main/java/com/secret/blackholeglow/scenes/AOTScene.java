package com.secret.blackholeglow.scenes;

import com.secret.blackholeglow.EqualizerBarsDJ;
import com.secret.blackholeglow.R;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                    ⚔️ AOT SCENE - Attack on Titan                        ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Eren Jaeger con el Titán Colosal - Escena épica de Shingeki no Kyojin.  ║
 * ║  Efectos: Ojos verdes brillantes, vapor del titán, atmósfera oscura.     ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  📖 ESCENA BASADA EN VIDEO:                                              ║
 * ║  Hereda de BaseVideoScene para reproducción automática de video.         ║
 * ║                                                                          ║
 * ║  ✅ Video: erenEscena01.mp4                                              ║
 * ║  ✅ Tema: AOT (verde/cyan ojos, rojo sangre, naranja titán)              ║
 * ║  ✅ Clock + Battery incluidos automáticamente                            ║
 * ║                                                                          ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * @see BaseVideoScene para documentación completa de la clase base
 */
public class AOTScene extends BaseVideoScene {

    // ═══════════════════════════════════════════════════════════════════════════
    // 🎯 MÉTODOS OBLIGATORIOS - Configuración básica de la escena
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public String getName() {
        return "AOT";
    }

    @Override
    public String getDescription() {
        return "Attack on Titan - Eren Jaeger";
    }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.preview_aot;
    }

    @Override
    protected String getVideoFileName() {
        return "erenEscena01.mp4";
    }

    @Override
    protected EqualizerBarsDJ.Theme getTheme() {
        return EqualizerBarsDJ.Theme.AOT;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 📝 NOTA PARA CLAUDE CODE:
    // ═══════════════════════════════════════════════════════════════════════════
    //
    // Esta escena es similar a GokuScene - solo video sin objetos 3D adicionales.
    // Todo lo demás (video, ecualizador, clock, battery, pause/resume,
    // auto-recovery, liberación de recursos) se hereda automáticamente.
    //
    // Colores del tema AOT:
    // - Verde/Cyan neón: Ojos brillantes de Eren pre-transformación
    // - Rojo sangre: La violencia y sacrificio del anime
    // - Naranja titán: El vapor y fuego de los titanes
    //
    // ═══════════════════════════════════════════════════════════════════════════
}
