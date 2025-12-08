// BaseShaderProgram.java
package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES30;

/**
 * Base para todos los programas GLSL:
 *  - Compila y linka shaders desde assets.
 *  - Cachea ubicaciones de uniforms: u_Time, u_MVP, u_Resolution.
 *  - NO acumula tiempo por sí mismo.
 *  - Provee métodos para enviar:
 *      • setTime(float phase)          → la fase de tu pulso (0…2π).
 *      • setMvpAndResolution(float[], int, int) → MVP + resolución.
 */
public abstract class BaseShaderProgram {
    protected final int programId;
    private final int uTimeLoc;
    private final int uMvpLoc;
    private final int uResolutionLoc;

    public BaseShaderProgram(Context ctx,
                             String vertexAssetPath,
                             String fragmentAssetPath) {
        // 1) Compila y linka
        programId = ShaderUtils.createProgramFromAssets(
                ctx, vertexAssetPath, fragmentAssetPath);
        // 2) Cachea las ubicaciones de los uniforms
        uTimeLoc       = GLES30.glGetUniformLocation(programId, "u_Time");
        uMvpLoc        = GLES30.glGetUniformLocation(programId, "u_MVP");
        uResolutionLoc = GLES30.glGetUniformLocation(programId, "u_Resolution");
    }

    /** Activa este programa (glUseProgram). */
    public void useProgram() {
        GLES30.glUseProgram(programId);
    }

    /**
     * Envía la fase de tu pulso (0…2π).
     * Llama a esto UNA sola vez por draw, antes de dibujar.
     */
    public void setTime(float phase) {
        GLES30.glUniform1f(uTimeLoc, phase);
    }

    /**
     * Envía la matriz MVP y la resolución de pantalla.
     * No toca el uniform u_Time.
     */
    public void setMvpAndResolution(float[] mvpMatrix,
                                    int width, int height) {
        GLES30.glUniformMatrix4fv(uMvpLoc, 1, false, mvpMatrix, 0);
        GLES30.glUniform2f(uResolutionLoc, (float)width, (float)height);
    }
}
