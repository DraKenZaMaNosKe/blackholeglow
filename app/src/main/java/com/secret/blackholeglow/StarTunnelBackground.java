package com.secret.blackholeglow;

import android.opengl.GLES20;
import java.nio.FloatBuffer;

import static com.secret.blackholeglow.SceneRenderer.screenWidth;
import static com.secret.blackholeglow.SceneRenderer.screenHeight;

/*
╔════════════════════════════════════════════════════════════════════════════════════════════════╗
║                                                                                                ║
║     🌌🌠  StarTunnelBackground.java - Fondo Tipo “Túnel Estelar”  🌠🌌                        ║
║                                                                                                ║
║   ASCII Art:                                                                                   ║
║            .-""""""-.      🌟       .-""""""-.                                                ║
║          .'   🌟    '.    🔭    .'    🌟   '.                                              ║
║         /   🌌 🌌 🌌   \\    🚀   /   🌌 🌌 🌌   \\                                            ║
║       ｜  🌠       🌠  ｜       ｜  🌠       🌠  ｜                                          ║
║         \\   🔹  🔹  🔹  /    🌌   \\   🔹  🔹  🔹  /                                           ║
║          '._      _.'         '._      _.'                                                ║
║             `"----"`             `"----"`                                                  ║
║                                                                                                ║
║   🔍 Descripción General:                                                                         ║
║   • Clase responsable de dibujar un fondo animado “túnel estelar”:                              ║
║     combina un degradado de cielo, túnel sutil y estrellas 3D parpadeantes con flicker           ║
║     aleatorio y desincronizado; solo unas pocas estrellas destellan lentamente,                  ║
║     con un desvanecimiento muy suave y un ligero destello al apagarse,                           ║
║     mientras la mayoría hace parpadeo normal o mantiene un brillo fijo.                          ║
║   • Utiliza shaders GLSL (OpenGL ES 2.0) para generar el efecto en tiempo real.                  ║
║   • El método update() avanza el tiempo cósmico, y draw() lanza el shader.                       ║
║                                                                                                ║
╚════════════════════════════════════════════════════════════════════════════════════════════════╝
*/

/**
 * StarTunnelBackground - Fondo animado estilo túnel estelar con:
 *   • Degradado de cielo.
 *   • Túnel sutil centrado.
 *   • Estrellas con:
 *       – Parpadeo normal suave (sin desaparecer por completo).
 *       – Un pequeño porcentaje (≈0.5%) que flickeran lento, con desvanecimiento muy suave
 *         y un destello ligero antes de apagarse.
 *       – Variación de brillo mínimo, amplitud y fase para evitar sincronía.
 *       – Variación de “profundidad” simulada usando `rnd` para brillo, flicker y glow.
 *   • Estrellas pequeñas siempre visibles con brillo tenue.
 *
 * 📝 Propósito:
 *   - Renderizar un fondo dinámico que simula un cielo profundo con un “túnel” en el centro.
 *   - Generar diversidad en parpadeo: unas pocas estrellas destellan lentamente (flicker) de manera
 *     desincronizada, con desvanecimiento muy suave y destello; la gran mayoría hace un parpadeo
 *     suave con brillo mínimo; las pequeñas mantienen un brillo tenue constante.
 *
 * ⚙️ Flujo Interno:
 *   1. Constructor invoca setupShader() para compilar vertex + fragment shader.
 *   2. update(deltaTime) incrementa la variable `time` en segundos.
 *   3. draw() pasa `u_Time` y `u_Resolution` al shader y dibuja un quad de pantalla completa.
 *   4. Fragment shader (GLSL) realiza:
 *       1️⃣ Coordenadas normalizadas `uv`.
 *       2️⃣ Degradado estático (azul oscuro → negro).
 *       3️⃣ Cálculo de túnel centrado con `glow`, cuyo factor varía según `rnd`.
 *       4️⃣ Genera ruido `rnd` pseudoaleatorio por fragmento.
 *       5️⃣ Aplica máscaras `largeMask`, `medMask`, `smallMask` para categorizar tamaño de estrella.
 *       6️⃣ Para **unas pocas medianas** (`rnd > 0.995`), aplica *flicker muy lento*:
 *            • `flickerSpeed = mix(2.0, 4.0, rnd)` → velocidad lenta y variable.
 *            • `phaseOffset  = rnd * 12.5664` → fase aleatoria (2π·2).
 *            • `flickRaw     = sin(u_Time * flickerSpeed + phaseOffset)` → oscilación continua muy suave.
 *            • `flickBase    = abs(flickRaw)` → 0 a 1 suave.
 *            • `flickMin     = 0.2` → brillo mínimo (no desaparece por completo).
 *            • `flick        = max(flickBase, flickMin)` → desvanecimiento muy suave.
 *            • `flash        = pow(abs(flickRaw), 20.0) * 0.3`
 *              → destello ligero y muy breve cerca de ±1.
 *            • `blink        = clamp(flick + flash, 0.0, 1.0);`
 *       7️⃣ Para **el resto de medianas** (`rnd <= 0.995`), aplica parpadeo *normal*:
 *            • `blinkSpeed  = mix(0.2, 1.0, rnd)` → velocidad suave variable.
 *            • `base        = mix(0.2, 0.5, rnd)` → brillo mínimo.
 *            • `amp         = mix(0.3, 0.7, rnd)` → amplitud.
 *            • `phaseNorm   = rnd * 6.2831` → fase aleatoria (2π·1).
 *            • `blink       = base + amp * sin(u_Time * blinkSpeed + phaseNorm);`
 *            • `blink       = clamp(blink, base, base + amp);`
 *       8️⃣ Brillo base para **estrellas pequeñas**: `smallBrightness = mix(0.1, 0.3, rnd)`.
 *       9️⃣ Suma de contribuciones de cada categoría al color:
 *            • Grandes: `largeMask * 1.0`
 *            • Medianas: `medMask * blink`
 *            • Pequeñas: `smallMask * smallBrightness`
 *      10️⃣ Clamp final de `color` en [0,1] y salida a `gl_FragColor`.
 *
 * 🔧 Parámetros Ajustables:
 *   • Umbral de flicker: `rnd > 0.995` (0.5% de estrellas medianas). Modifícalo a 0.997 o 0.99.
 *   • Rango de `flickerSpeed`: `mix(2.0, 4.0, rnd)` define frecuencia de flicker; ajústalo a [1,5].
 *   • `flickMin = 0.2`: brillo mínimo durante flicker; ajústalo para más tenue o más visible.
 *   • Rango de `flash`: `pow(abs(sin(...)), 20.0) * 0.3` controla intensidad y brevedad del destello.
 *   • Parpadeo normal: `blinkSpeed = mix(0.2, 1.0, rnd)`, `base = mix(0.2, 0.5, rnd)`, `amp = mix(0.3, 0.7, rnd)`.
 *   • `smallBrightness = mix(0.1, 0.3, rnd)`: brillo base para pequeñas.
 *   • `glowFactor = mix(0.05, 0.15, rnd)`: intensidad del glow del túnel.
 */
public class StarTunnelBackground implements SceneObject {

    // 🔗 ID del programa OpenGL compilado y vinculado
    private int program;

    // 📍 Ubicaciones de uniforms: u_Time y u_Resolution
    private int uTimeLocation;
    private int uResolutionLocation;

    // ⏱️ Tiempo acumulado (en segundos) usado para animaciones
    private float time = 0f;

    /**
     * Constructor StarTunnelBackground - Invoca setupShader() al crear instancia.
     */
    public StarTunnelBackground() {
        setupShader();
    }

    /**
     * setupShader - Compila y vincula vertex + fragment shader, y obtiene ubicaciones de uniforms.
     */
    private void setupShader() {
        // ╔════════════════════════════════════════════════════════════════════╗
        // ║   Vertex Shader (GLSL): Mapea posición de vértice a gl_Position      ║
        // ╚════════════════════════════════════════════════════════════════════╝
        String vertexShader =
                "attribute vec4 a_Position;\n" +
                        "void main() {\n" +
                        "    gl_Position = a_Position;\n" +
                        "}";

        // ╔════════════════════════════════════════════════════════════════════╗
        // ║   Fragment Shader (GLSL): Degradado + Túnel + Estrellas Variables  ║
        // ╚════════════════════════════════════════════════════════════════════╝
        String fragmentShader =
                "precision mediump float;\n" +
                        "uniform float u_Time;\n" +
                        "uniform vec2 u_Resolution;\n" +
                        "void main() {\n" +
                        "    // 1️⃣ Coordenadas normalizadas en [0,1]\n" +
                        "    vec2 uv = gl_FragCoord.xy / u_Resolution;\n" +
                        "\n" +
                        "    // 2️⃣ Degradado estático: cielo azul oscuro → negro\n" +
                        "    vec3 topColor    = vec3(0.0, 0.0, 0.2);\n" +
                        "    vec3 bottomColor = vec3(0.0, 0.0, 0.0);\n" +
                        "    vec3 color = mix(topColor, bottomColor, uv.y);\n" +
                        "\n" +
                        "    // 3️⃣ Cálculo de túnel centrado con glow variable según rnd\n" +
                        "    vec2 center = u_Resolution * 0.5;\n" +
                        "    vec2 pos    = gl_FragCoord.xy - center;\n" +
                        "    pos /= min(u_Resolution.x, u_Resolution.y);\n" +
                        "    float dist = length(pos);\n" +
                        "\n" +
                        "    // 4️⃣ Ruido pseudoaleatorio único por fragmento\n" +
                        "    float rnd = fract(sin(dot(gl_FragCoord.xy,\n" +
                        "                            vec2(12.9898,78.233)))\n" +
                        "                      * 43758.5453);\n" +
                        "\n" +
                        "    // 5️⃣ Glow del túnel: depende de rnd para simular profundidad\n" +
                        "    float glowFactor = mix(0.05, 0.15, rnd);\n" +
                        "    float glow = smoothstep(4.0, 0.0, dist) * glowFactor;\n" +
                        "    color += vec3(0.0, 0.0, glow);\n" +
                        "\n" +
                        "    // 6️⃣ Máscaras de tamaño para estrellas:\n" +
                        "    float largeMask = step(0.999, rnd);\n" +
                        "    float medMask   = step(0.997, rnd) - largeMask;\n" +
                        "    float smallMask = step(0.995, rnd) - (largeMask + medMask);\n" +
                        "\n" +
                        "    // 7️⃣ Parpadeo para medianas: flicker muy lento o parpadeo normal\n" +
                        "    float blink;\n" +
                        "    if (rnd > 0.995) {\n" +
                        "        // 🎇 Flicker suave para una fracción mínima (~0.5%)\n" +
                        "        float flickerSpeed = mix(0.5, 1.0, rnd);          // velocidad lenta\n" +
                        "        float phaseOffset  = rnd * 12.5664;               // fase variada 2π·2\n" +
                        "        float flickRaw     = sin(u_Time * flickerSpeed + phaseOffset);\n" +
                        "        // Desvanecimiento muy suave: valor absoluto y brillo mínimo\n" +
                        "        float flickBase    = abs(flickRaw);               // oscilación 0→1\n" +
                        "        float flickMin     = 0.2;                         // no desaparece\n" +
                        "        float flick        = max(flickBase, flickMin);\n" +
                        "        // Destello leve al apagarse (peaks de sin → ±1)\n" +
                        "        float flash        = pow(abs(flickRaw), 20.0) * 0.3;\n" +
                        "        blink = clamp(flick + flash, 0.0, 1.0);\n" +
                        "    } else {\n" +
                        "        // 🌟 Parpadeo normal para la mayoría de medianas\n" +
                        "        float blinkSpeed = mix(0.2, 1.0, rnd);\n" +
                        "        float base       = mix(0.2, 0.5, rnd);\n" +
                        "        float amp        = mix(0.3, 0.7, rnd);\n" +
                        "        float phaseNorm  = rnd * 6.2831;                   // fase variada 2π·1\n" +
                        "        blink = base + amp * sin(u_Time * blinkSpeed + phaseNorm);\n" +
                        "        blink = clamp(blink, base, base + amp);\n" +
                        "    }\n" +
                        "\n" +
                        "    // 8️⃣ Brillo base para estrellas pequeñas, siempre visible [0.1,0.3]\n" +
                        "    float smallBrightness = mix(0.1, 0.3, rnd);\n" +
                        "\n" +
                        "    // 9️⃣ Suma de contribuciones de cada tipo de estrella:\n" +
                        "    color += largeMask * 1.0;            // ⭐ grandes: brillo máximo siempre\n" +
                        "    color += medMask   * blink;          // ✴️ medianas: flicker muy suave o normal\n" +
                        "    color += smallMask * smallBrightness; // · pequeñas: brillo fijo tenue\n" +
                        "\n" +
                        "    // 🔟 Clamp final del color en [0,1]\n" +
                        "    color = clamp(color, 0.0, 1.0);\n" +
                        "\n" +
                        "    gl_FragColor = vec4(color, 1.0);\n" +
                        "}";

        // Compilar y vincular shaders; obtener ubicaciones de uniforms
        program = ShaderUtils.createProgram(vertexShader, fragmentShader);
        uTimeLocation       = GLES20.glGetUniformLocation(program, "u_Time");
        uResolutionLocation = GLES20.glGetUniformLocation(program, "u_Resolution");
    }

    /**
     * update - Actualiza el tiempo interno “time” en segundos,
     * usado para animar flicker suave y parpadeo normal.
     *
     * @param deltaTime - Tiempo transcurrido (en segundos) desde el último frame.
     */
    @Override
    public void update(float deltaTime) {
        time += deltaTime;
    }

    /**
     * draw - Dibuja el fondo “túnel estelar” usando shaders y un quad de pantalla completa.
     *
     * Pasos:
     *   1. glUseProgram(program): activa el programa GLSL.
     *   2. glUniform1f(uTimeLocation, time): envía tiempo actual al shader.
     *   3. glUniform2f(uResolutionLocation, screenWidth, screenHeight): envía resolución.
     *   4. Define un quad de pantalla completa (coordenadas NDC [-1,1]).
     *   5. Crea FloatBuffer para vértices y configura a_Position.
     *   6. glDrawArrays(GL_TRIANGLE_STRIP, 0, 4): dibuja el quad.
     *   7. Deshabilita el atributo a_Position.
     *
     * @see -SceneRenderer.screenWidth, screenHeight
     */
    @Override
    public void draw() {
        // 1️⃣ Activar el programa GLSL
        GLES20.glUseProgram(program);

        // 2️⃣ Enviar el tiempo uniform (u_Time)
        GLES20.glUniform1f(uTimeLocation, time);

        // 3️⃣ Enviar la resolución uniform (u_Resolution)
        GLES20.glUniform2f(uResolutionLocation,
                (float) screenWidth,
                (float) screenHeight);

        // 4️⃣ Definir vértices del quad (pantalla completa)
        float[] quad = {
                -1f, -1f,  // esquina inferior izquierda
                1f, -1f,  // esquina inferior derecha
                -1f,  1f,  // esquina superior izquierda
                1f,  1f   // esquina superior derecha
        };
        FloatBuffer buf = ShaderUtils.createFloatBuffer(quad);

        // 5️⃣ Obtener la ubicación del atributo a_Position
        int posLoc = GLES20.glGetAttribLocation(program, "a_Position");
        GLES20.glEnableVertexAttribArray(posLoc);
        GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 0, buf);

        // 6️⃣ Dibujar el quad con GL_TRIANGLE_STRIP
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // 7️⃣ Deshabilitar el atributo a_Position
        GLES20.glDisableVertexAttribArray(posLoc);
    }
}

