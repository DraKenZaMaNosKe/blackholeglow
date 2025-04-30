package com.secret.blackholeglow;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.FloatBuffer;

import static com.secret.blackholeglow.SceneRenderer.screenWidth;
import static com.secret.blackholeglow.SceneRenderer.screenHeight;


public class StarTunnelBackground implements SceneObject {

    private int uResolutionLocation;

    private final float[] modelMatrix = new float[16];
    private int program;
    private int uTimeLocation;

    private float time = 0f;

    public StarTunnelBackground() {
        setupShader();
    }

    private void setupShader() {
        String vertexShader =
                "attribute vec4 a_Position;" +
                        "void main() {" +
                        "    gl_Position = a_Position;" +
                        "}";

        String fragmentShader =
                "precision mediump float;" +
                        "uniform float u_Time;" +
                        "uniform vec2 u_Resolution;" +
                        "void main() {" +
                        "    vec2 uv = gl_FragCoord.xy / u_Resolution;" + // Ajusta a tu resolución
                        "    vec2 center = vec2(0.5, 0.5);" +
                        "    float dist = distance(uv, center);" +
                        "    float glow = smoothstep(0.5, 0.1, dist);" +
                        "    float pulse = 0.5 + 0.5 * sin(u_Time);" +
                        "    gl_FragColor = vec4(glow * pulse, glow * pulse * 0.5, glow * 0.7, 1.0);" +
                        "}";

        program = ShaderUtils.createProgram(vertexShader, fragmentShader);
        uTimeLocation = GLES20.glGetUniformLocation(program, "u_Time");
        uResolutionLocation = GLES20.glGetUniformLocation(program, "u_Resolution");
    }

    @Override
    public void update(float deltaTime) {
        time += deltaTime;
    }

    @Override
    public void draw() {
        GLES20.glUseProgram(program);
        GLES20.glUniform1f(uTimeLocation, time);
        GLES20.glUniform2f(uResolutionLocation, (float)screenWidth, (float)screenHeight);


        float[] quad = {
                -1f, -1f,
                1f, -1f,
                -1f,  1f,
                1f,  1f
        };

        FloatBuffer buffer = ShaderUtils.createFloatBuffer(quad);  // ✔️ Usa FloatBuffer
        int aPosition = GLES20.glGetAttribLocation(program, "a_Position");
        GLES20.glEnableVertexAttribArray(aPosition);
        GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_FLOAT, false, 0, buffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(aPosition);
    }
}