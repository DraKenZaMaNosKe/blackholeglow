package com.secret.blackholeglow;

import android.opengl.GLES20;

public class StarShader {
    public static final String VERTEX_SHADER =
            "attribute vec4 a_Position;" +
                    "attribute vec2 a_TexCoord;" +
                    "varying vec2 v_TexCoord;" +
                    "void main() {" +
                    "    gl_Position = a_Position;" +
                    "    v_TexCoord = a_TexCoord;" +
                    "}";

    public static final String FRAGMENT_SHADER =
            "precision mediump float;" +
                    "uniform sampler2D u_Texture;" +
                    "varying vec2 v_TexCoord;" +
                    "void main() {" +
                    "    vec4 texColor = texture2D(u_Texture, v_TexCoord);" +
                    "    if (texColor.a < 0.05) discard;" +
                    "    texColor.rgb *= texColor.a;" +  // Atenúa el color según la transparencia
                    "    gl_FragColor = texColor;" +
                    "}";
}