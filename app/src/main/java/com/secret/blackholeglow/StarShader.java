package com.secret.blackholeglow;

import android.opengl.GLES20;

public class StarShader {
    public static final String VERTEX_SHADER =
            "attribute vec4 a_Position;" +
                    "attribute vec4 a_Color;" +
                    "varying vec4 v_Color;" +
                    "uniform float u_PointSize;" +
                    "void main() {" +
                    "    gl_Position = a_Position;" +
                    "    gl_PointSize = u_PointSize;" +
                    "    v_Color = a_Color;" +
                    "}";

    public static final String FRAGMENT_SHADER =
            "precision mediump float;" +
                    "varying vec4 v_Color;" +
                    "void main() {" +
                    "    float dist = length(gl_PointCoord - vec2(0.5));" +
                    "    if (dist > 0.5) discard;" +  // Hace la estrella redonda
                    "    gl_FragColor = v_Color;" +
                    "}";

}