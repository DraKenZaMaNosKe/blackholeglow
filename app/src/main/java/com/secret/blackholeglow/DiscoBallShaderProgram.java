// ============================================
// DiscoBallShaderProgram.java
// Shader program for disco ball with mirror tiles,
// color cycling, and audio reactivity
// ============================================

package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;

/**
 * Shader program específico para la bola disco.
 *
 * Features:
 *  - Breathing effect (pulsación suave)
 *  - Mirror tile grid (cuadritos espejo)
 *  - Color cycling (colores rainbow)
 *  - Sparkle effects (brillos aleatorios)
 *  - Audio reactivity (bass response)
 *
 * Uniforms adicionales:
 *  - u_AudioBass: Intensidad de bajos (0.0 - 1.0)
 */
public class DiscoBallShaderProgram extends BaseShaderProgram {

    // Uniform locations
    private final int uAudioBassLoc;

    /**
     * Constructor - carga los shaders desde assets
     */
    public DiscoBallShaderProgram(Context ctx) {
        super(ctx,
              "shaders/disco_ball_vertex.glsl",
              "shaders/disco_ball_fragment.glsl");

        // Cachear ubicación del uniform de audio
        uAudioBassLoc = GLES20.glGetUniformLocation(programId, "u_AudioBass");
    }

    /**
     * Establece el nivel de bajos para audio reactivity.
     *
     * @param bassLevel Intensidad de bajos (0.0 = silencio, 1.0 = máximo)
     */
    public void setAudioBass(float bassLevel) {
        // Clamp entre 0 y 1 por seguridad
        bassLevel = Math.max(0.0f, Math.min(1.0f, bassLevel));
        GLES20.glUniform1f(uAudioBassLoc, bassLevel);
    }
}
