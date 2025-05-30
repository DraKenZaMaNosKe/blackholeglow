package com.secret.blackholeglow;

import android.opengl.GLES20;
import java.nio.FloatBuffer;

import static com.secret.blackholeglow.SceneRenderer.screenWidth;
import static com.secret.blackholeglow.SceneRenderer.screenHeight;

/*
 * ╔════════════════════════════════════════════════════════════╗
 * ║    🤖🪐 Athena te guía: túnel universo vivo (v6.0)          ║
 * ║  Gradiente estático + estrellas en 3D + túnel sutil       ║
 * ╚════════════════════════════════════════════════════════════╝
 */
public class StarTunnelBackground implements SceneObject {

    private int program;
    private int uTimeLocation;
    private int uResolutionLocation;
    private float time = 0f;

    public StarTunnelBackground() {
        setupShader();
    }

    private void setupShader() {
        String vertexShader =
                "attribute vec4 a_Position;\n" +
                        "void main() {\n" +
                        "    gl_Position = a_Position;\n" +
                        "}";

        // Fragment shader: degradado fijo + estrellas 3D + túnel muy leve
        String fragmentShader =
                "precision mediump float;\n" +
                        "uniform float u_Time;\n" +
                        "uniform vec2 u_Resolution;\n" +
                        "void main() {\n" +
                        "    // 1️⃣ Coordenadas normalizadas\n" +
                        "    vec2 uv = gl_FragCoord.xy / u_Resolution;\n" +
                        "\n" +
                        "    // 2️⃣ Fondo: azul obscuro arriba → negro abajo (siempre estático)\n" +
                        "    vec3 topColor    = vec3(0.0, 0.0, 0.2);\n" +
                        "    vec3 bottomColor = vec3(0.0, 0.0, 0.0);\n" +
                        "    vec3 color = mix(topColor, bottomColor, uv.y);\n" +
                        "\n" +
                        "    // 3️⃣ Sublime túnel: solo un halo muy leve\n" +
                        "    vec2 center = u_Resolution * 0.5;\n" +
                        "    vec2 pos    = gl_FragCoord.xy - center;\n" +
                        "    pos /= min(u_Resolution.x, u_Resolution.y);\n" +
                        "    float dist = length(pos);\n" +
                        "    float glow = smoothstep(4.0, 0.0, dist);\n" +
                        "    // factor pequeño para no cubrir fondo\n" +
                        "    color += vec3(0.0, 0.0, glow * 0.08);\n" +
                        "\n" +
                        "    // 4️⃣ Ruido único por fragmento\n" +
                        "    float rnd = fract(sin(dot(gl_FragCoord.xy,\n" +
                        "                            vec2(12.9898,78.233)))\n" +
                        "                      * 43758.5453);\n" +
                        "\n" +
                        "    // 5️⃣ Máscaras de tamaño:\n" +
                        "    float largeMask = step(0.9985, rnd);\n" +
                        "    float medMask   = step(0.997, rnd)  - largeMask;\n" +
                        "    float smallMask = step(0.995, rnd)  - (largeMask + medMask);\n" +
                        "\n" +
                        "    // 6️⃣ Parpadeo muy lento en medianas (~ period ≈ 60s)\n" +
                        "    float blink = 0.5 + 0.5 * sin(u_Time * 0.1 + rnd * 6.2831);\n" +
                        "\n" +
                        "    // 7️⃣ Agrega cada “tipo” de estrella:\n" +
                        "    color += largeMask * 1.0;    // ⭐ grandes, siempre on\n" +
                        "    color += medMask   * blink;  // ✴️ medianas, parpadean\n" +
                        "    color += smallMask * 0.4;    // · pequeñas, brillo tenue\n" +
                        "\n" +
                        "    // 8️⃣ Clampea al rango [0,1]\n" +
                        "    color = clamp(color, 0.0, 1.0);\n" +
                        "\n" +
                        "    gl_FragColor = vec4(color, 1.0);\n" +
                        "}";

        // Compila y vincula
        program = ShaderUtils.createProgram(vertexShader, fragmentShader);
        uTimeLocation       = GLES20.glGetUniformLocation(program, "u_Time");
        uResolutionLocation = GLES20.glGetUniformLocation(program, "u_Resolution");
    }

    @Override
    public void update(float deltaTime) {
        time += deltaTime;  // avanza el tiempo cósmico
    }

    @Override
    public void draw() {
        GLES20.glUseProgram(program);

        // Pasa uniforms
        GLES20.glUniform1f(uTimeLocation, time);
        GLES20.glUniform2f(uResolutionLocation,
                (float) screenWidth,
                (float) screenHeight);

        // Quad pantalla completa
        float[] quad = {
                -1f, -1f,
                1f, -1f,
                -1f,  1f,
                1f,  1f
        };
        FloatBuffer buf = ShaderUtils.createFloatBuffer(quad);

        int posLoc = GLES20.glGetAttribLocation(program, "a_Position");
        GLES20.glEnableVertexAttribArray(posLoc);
        GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 0, buf);

        // Dibuja el fragment shader
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(posLoc);
    }
}
