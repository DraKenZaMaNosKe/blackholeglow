// BaseShaderProgram.java
package com.secret.blackholeglow;

import android.content.Context;
import android.opengl.GLES20;

/**
 * Clase base para todos los programas GLSL:
 *  - Carga shaders desde assets.
 *  - Cachea ubicaciones de uniforms básicos: u_Time, u_MVP, u_Resolution.
 *  - Te da un método setCommonUniforms(dt, mvp, w, h) para enviar
 *    time, MVP y resolución cada frame.
 */
public abstract class BaseShaderProgram {
    protected final int programId;
    private final int uTimeLoc;
    private final int uMvpLoc;
    private final int uResolutionLoc;

    private float time = 0f;

    public BaseShaderProgram(Context ctx,
                             String vertexAssetPath,
                             String fragmentAssetPath) {
        // compila y linka
        programId = ShaderUtils.createProgramFromAssets(ctx,
                vertexAssetPath, fragmentAssetPath);

        // localiza uniforms
        uTimeLoc       = GLES20.glGetUniformLocation(programId, "u_Time");
        uMvpLoc        = GLES20.glGetUniformLocation(programId, "u_MVP");
        uResolutionLoc = GLES20.glGetUniformLocation(programId, "u_Resolution");
    }

    /** Activa este programa (glUseProgram) */
    public void useProgram() {
        GLES20.glUseProgram(programId);
    }

    /**
     * Envía uniforms comunes:
     *  - incrementa y pasa u_Time
     *  - pasa la matriz u_MVP
     *  - pasa u_Resolution (ancho, alto)
     */
    public void setCommonUniforms(float dt,
                                  float[] mvpMatrix,
                                  int width, int height) {
        time += dt;
        GLES20.glUniform1f(uTimeLoc, time);
        GLES20.glUniformMatrix4fv(uMvpLoc, 1, false, mvpMatrix, 0);
        GLES20.glUniform2f(uResolutionLoc, (float)width, (float)height);
    }
}
