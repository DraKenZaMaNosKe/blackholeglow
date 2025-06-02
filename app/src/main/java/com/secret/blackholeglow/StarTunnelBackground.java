package com.secret.blackholeglow;

import android.opengl.GLES20;
import java.nio.FloatBuffer;

import static com.secret.blackholeglow.SceneRenderer.screenWidth;
import static com.secret.blackholeglow.SceneRenderer.screenHeight;

/*
╔════════════════════════════════════════════════════════════════════════╗
║                                                                        ║
║     🌌🌠  StarTunnelBackground.java - Fondo Tipo “Túnel Estelar”  🌠🌌    ║
║                                                                        ║
║   ASCII Art:                                                           ║
║            .-""""""-.      🌟       .-""""""-.                          ║
║          .'          '.    🔭    .'          '.                        ║
║         /   🌌 🌌 🌌   \    🚀   /   🌌 🌌 🌌   \                       ║
║       ｜  🌠       🌠  ｜       ｜  🌠       🌠  ｜                      ║
║         \   🔹  🔹  🔹  /    🌌   \   🔹  🔹  🔹  /                       ║
║          '._      _.'         '._      _.'                          ║
║             `"----"`             `"----"`                            ║
║                                                                        ║
║   🔍 Descripción General:                                               ║
║   • Clase responsable de dibujar un fondo animado “túnel estelar”:      ║
║     combina un degradado de cielo, túnel sutil y estrellas 3D parpadeantes. ║
║   • Utiliza shaders GLSL (OpenGL ES 2.0) para generar el efecto en tiempo real. ║
║   • El método update() avanza el tiempo cósmico, y draw() lanza el shader. ║
║                                                                        ║
╚════════════════════════════════════════════════════════════════════════╝
*/

/**
 * StarTunnelBackground - Fondo animado estilo túnel estelar con degradado y parpadeo de estrellas.
 *
 * 📝 Propósito:
 *   - Renderizar un fondo dinámico que simula un cielo profundo con un “túnel” sutil en el centro.
 *   - Permitir modificar parámetros de color, intensidad de túnel y densidad de estrellas ajustando el shader.
 *   - Sirve como uno de los posibles fondos instanciados en SceneRenderer.prepareScene().
 *
 * ⚙️ Flujo Interno:
 *   1. En el constructor se invoca setupShader(): compila un vertex shader simple y un fragment shader complejo.
 *   2. En update(deltaTime): avanza el uniform u_Time para animar el parpadeo de estrellas.
 *   3. En draw():
 *       - Activa el programa GL (glUseProgram).
 *       - Pasa uniform de tiempo (u_Time) y resolución (u_Resolution).
 *       - Define un “quad” de pantalla completa y configura el atributo a_Position.
 *       - Llama a glDrawArrays para ejecutar el fragment shader en cada píxel.
 *   4. El fragment shader calcula:
 *       • Coordenadas normalizadas (uv).
 *       • Degradado estático de arriba (azul oscuro) a abajo (negro).
 *       • Un “glow” de túnel sutil centrado, que añade un halo de color azul tenue.
 *       • Ruido pseudoaleatorio por fragmento (rnd) para ubicar estrellas aleatorias.
 *       • Máscaras para estrellas grandes, medianas y pequeñas (largeMask, medMask, smallMask).
 *       • Parpadeo lento en estrellas medianas, controlado por u_Time (≈60s de ciclo).
 *       • Ensambla el color final, suma de degradado + túnel + estrellas, y clampa entre [0,1].
 */
public class StarTunnelBackground implements SceneObject {

    // 🔗 ID del programa OpenGL resultante de compilar vertex + fragment shader
    private int program;

    // 📍 Ubicaciones de uniforms en el shader para tiempo y resolución
    private int uTimeLocation;
    private int uResolutionLocation;

    // ⏱️ Tiempo acumulado en segundos, avanza en update(), usado en fragment shader para animar parpadeo
    private float time = 0f;

    /**
     * Constructor StarTunnelBackground - Llama a setupShader() al crear la instancia.
     *
     * ASCII Art:
     *   ╔═════════════════════════════════╗
     *   ║  🛠️  StarTunnelBackground()      ║
     *   ║  Invoca setupShader()            ║
     *   ╚═════════════════════════════════╝
     */
    public StarTunnelBackground() {
        setupShader();
    }

    /**
     * setupShader - Compila y vincula los shaders GLSL (vertex + fragment).
     * Obtiene ubicaciones de uniforms “u_Time” y “u_Resolution” para usarlos en draw().
     *
     * 🎨 Vertex Shader:
     *   - Recibe atributo a_Position (vec4).
     *   - Solo asigna gl_Position = a_Position (passthrough).
     *   - Este shader es “render to screen” para un quad completo.
     *
     * 🎨 Fragment Shader:
     *   - precision mediump float: precisión media en cálculos flotantes.
     *   - uniform float u_Time: tiempo en segundos (para animar parpadeo de estrellas).
     *   - uniform vec2 u_Resolution: resolución actual de la pantalla (x=width, y=height).
     *
     *   Pasos del fragment shader (línea a línea):
     *     1️⃣ Coordenadas normalizadas (uv):
     *         vec2 uv = gl_FragCoord.xy / u_Resolution;
     *         - gl_FragCoord.xy da coordenadas en píxeles, dividiendo entre resolución obtenemos [0,1].
     *
     *     2️⃣ Degradado estático de cielo:
     *         vec3 topColor    = vec3(0.0, 0.0, 0.2);
     *         vec3 bottomColor = vec3(0.0, 0.0, 0.0);
     *         vec3 color = mix(topColor, bottomColor, uv.y);
     *         - topColor: azul oscuro; bottomColor: negro.
     *         - mix(blend) entre ambos según uv.y (arriba→abajo).
     *
     *     3️⃣ “Túnel” sutil centrado:
     *         vec2 center = u_Resolution * 0.5;
     *         vec2 pos    = gl_FragCoord.xy - center;
     *         pos /= min(u_Resolution.x, u_Resolution.y);
     *         float dist = length(pos);
     *         float glow = smoothstep(4.0, 0.0, dist);
     *         color += vec3(0.0, 0.0, glow * 0.08);
     *         - center: centro en píxeles.
     *         - pos: vector desde el fragmento al centro, normalizado.
     *         - dist: distancia normalizada.
     *         - smoothstep(4.0, 0.0, dist): produce “halo” en rango [0,4], invirtiendo progresión.
     *         - glow * 0.08: factor pequeño (8%) para no ocultar degradado.
     *
     *     4️⃣ Ruido pseudoaleatorio por fragmento:
     *         float rnd = fract(sin(dot(gl_FragCoord.xy,
     *                         vec2(12.9898,78.233)))
     *                       * 43758.5453);
     *         - dot: combinación lineal de coordenadas con constantes.
     *         - sin() * 43758… y fract() produce valor en [0,1).
     *         - rnd es diferente para cada píxel, determina ubicación de estrellas.
     *
     *     5️⃣ Máscaras de tamaño para estrellas:
     *         float largeMask = step(0.9985, rnd);
     *         float medMask   = step(0.997, rnd)  - largeMask;
     *         float smallMask = step(0.995, rnd)  - (largeMask + medMask);
     *         - step(edge, rnd): 1 si rnd ≥ edge, 0 si rnd < edge.
     *         - largeMask: rnd ≥ 0.9985 → muy pocas estrellas grandes.
     *         - medMask: rnd ≥ 0.997 pero < 0.9985 → estrellas medianas.
     *         - smallMask: rnd ≥ 0.995 pero < 0.997 → estrellas pequeñas.
     *         - La densidad relativa: grandes (~0.15%), medianas (~0.3%), pequeñas (~1.5%).
     *
     *     6️⃣ Parpadeo lento de estrellas medianas:
     *         float blink = 0.5 + 0.5 * sin(u_Time * 0.1 + rnd * 6.2831);
     *         - u_Time * 0.1: ciclo lento (~periodo 2π/0.1 ≈ 62.8s).
     *         - rnd * 2π: fase aleatoria por estrella.
     *         - Sin oscila en [-1,1], se mapea a [0,1].
     *
     *     7️⃣ Sumatoria de cada tipo de estrella:
     *         color += largeMask * 1.0;    // ⭐ grandes: siempre encendidas (brillo máximo)
     *         color += medMask   * blink;  // ✴️ medianas: parpadean suave
     *         color += smallMask * 0.4;    // · pequeñas: brillo tenue fijo (40%)
     *
     *     8️⃣ Clamp final del color:
     *         color = clamp(color, 0.0, 1.0);
     *         - Asegura que componentes RGB permanezcan en [0,1].
     *
     *     gl_FragColor = vec4(color, 1.0);
     *         - Establece el color final con alpha=1.0 (opaco).
     *
     * 🔧 Detalles Importantes para Modificación:
     *   • Ajustar “topColor” y “bottomColor” para cambiar degradado de cielo.
     *   • Modificar valores en smoothstep(4.0, 0.0, dist) para ampliar o contraer túnel:
     *       ▪ Aumentar 4.0 → disminuir tamaño del halo; disminuir 4.0 → expandir halo.
     *   • Cambiar “glow * 0.08” a otro factor para hacer el túnel más o menos visible.
     *   • Afinar thresholds de step(): 0.9985, 0.997, 0.995 para controlar densidad de estrellas:
     *       ▪ Ejemplo: step(0.999, rnd) → menos estrellas grandes; step(0.99, rnd) → más estrellas pequeñas.
     *   • Modificar velocidad de parpadeo: u_Time * 0.1:
     *       ▪ 0.1 → ciclo lento (~60s); 0.5 → ciclo rápido (~12s); etc.
     *   • Cambiar amplitud de parpadeo: 0.5 + 0.5 * sin(...) → 0.3+0.7*sin(...) para variar intensidad.
     */
    private void setupShader() {
        // ╔════════════════════════════════════════════════════════════════════════════╗
        // ║   Vertex Shader (GLSL): Mapea posición de vértice a gl_Position directamente  ║
        // ╚════════════════════════════════════════════════════════════════════════════╝
        String vertexShader =
                "attribute vec4 a_Position;\n" +
                        "void main() {\n" +
                        "    gl_Position = a_Position;\n" +
                        "}";

        // ╔════════════════════════════════════════════════════════════════════════════╗
        // ║   Fragment Shader (GLSL): Calcula degradado + túnel sutil + estrellas 3D    ║
        // ╚════════════════════════════════════════════════════════════════════════════╝
        String fragmentShader =
                "precision mediump float;\n" +
                        "uniform float u_Time;\n" +
                        "uniform vec2 u_Resolution;\n" +
                        "void main() {\n" +
                        "    // 1️⃣ Coordenadas normalizadas en [0, 1]\n" +
                        "    vec2 uv = gl_FragCoord.xy / u_Resolution;\n" +
                        "\n" +
                        "    // 2️⃣ Degradado estático: cielo azul oscuro → negro\n" +
                        "    vec3 topColor    = vec3(0.0, 0.0, 0.2);\n" +
                        "    vec3 bottomColor = vec3(0.0, 0.0, 0.0);\n" +
                        "    vec3 color = mix(topColor, bottomColor, uv.y);\n" +
                        "\n" +
                        "    // 3️⃣ Túnel sutil con desplazamiento radial en función del tiempo\n" +
                        "    vec2 center = u_Resolution * 0.5;\n" +
                        "    vec2 pos = gl_FragCoord.xy - center;\n" +
                        "    pos /= min(u_Resolution.x, u_Resolution.y);\n" +
                        "\n" +
                        "    // ✨ Desplazamiento radial: las estrellas se mueven hacia afuera con velocidad “speed”\n" +
                        "    float speed = 2.0; // Ajusta este valor: 2.0 → más rápido que 0.1 por defecto\n" +
                        "    pos += normalize(pos) * (u_Time * speed * 0.1);\n" +
                        "\n" +
                        "    float dist = length(pos);\n" +
                        "    float glow = smoothstep(4.0, 0.0, dist);\n" +
                        "    color += vec3(0.0, 0.0, glow * 0.08);\n" +
                        "\n" +
                        "    // 4️⃣ Ruido pseudoaleatorio por fragmento\n" +
                        "    float rnd = fract(sin(dot(gl_FragCoord.xy,\n" +
                        "                            vec2(12.9898,78.233)))\n" +
                        "                      * 43758.5453);\n" +
                        "\n" +
                        "    // 5️⃣ Máscaras de tamaño para estrellas (densidad aumentada)\n" +
                        "    float largeMask = step(0.9995, rnd);   // Umbral más alto: menos estrellas grandes\n" +
                        "    float medMask   = step(0.998, rnd)  - largeMask;  // Un poco más de medianas\n" +
                        "    float smallMask = step(0.995, rnd)  - (largeMask + medMask);\n" +
                        "\n" +
                        "    // 6️⃣ Parpadeo más rápido para estrellas medianas (u_Time * 1.0 en lugar de 0.1)\n" +
                        "    float blink = 0.5 + 0.5 * sin(u_Time * 1.0 + rnd * 6.2831);\n" +
                        "\n" +
                        "    // 7️⃣ Sumatoria del brillo de cada categoría de estrella\n" +
                        "    color += largeMask * 1.0;    // ⭐ grandes\n" +
                        "    color += medMask   * blink;  // ✴️ medianas parpadean rápido\n" +
                        "    color += smallMask * 0.4;    // · pequeñas\n" +
                        "\n" +
                        "    // 8️⃣ Clamp final del color en rango [0,1]\n" +
                        "    color = clamp(color, 0.0, 1.0);\n" +
                        "\n" +
                        "    gl_FragColor = vec4(color, 1.0);\n" +
                        "}";

        // ╔════════════════════════════════════════════════════════════════════════════╗
        // ║   Compilar y vincular shaders, obtener ubicaciones de uniforms            ║
        // ╚════════════════════════════════════════════════════════════════════════════╝
        program = ShaderUtils.createProgram(vertexShader, fragmentShader);

        // Ubicaciones de uniforms en el programa compilado
        uTimeLocation       = GLES20.glGetUniformLocation(program, "u_Time");
        uResolutionLocation = GLES20.glGetUniformLocation(program, "u_Resolution");
    }

    /**
     * update - Actualiza el tiempo interno “time” en segundos.
     * Este valor se usa en el fragment shader para animar el parpadeo de estrellas.
     *
     * @param deltaTime - Tiempo transcurrido (en segundos) desde el último frame.
     *
     * ASCII Art:
     *   ╔═════════════════════════════════╗
     *   ║      ⏱️  update(deltaTime)     ⏱️   ║
     *   ╚═════════════════════════════════╝
     *        time = time + deltaTime;
     */
    @Override
    public void update(float deltaTime) {
        // Avanza el “tiempo cósmico” para animar el parpadeo lento
        time += deltaTime;
    }

    /**
     * draw - Dibuja el fondo “túnel estelar” usando shaders y quad de pantalla completa.
     *
     * Pasos:
     *   1. glUseProgram(program): activa el programa shader.
     *   2. glUniform1f(uTimeLocation, time): pasa el tiempo uniform.
     *   3. glUniform2f(uResolutionLocation, screenWidth, screenHeight): pasa resolución uniform.
     *   4. Define un quad de pantalla completa en coordenadas Normalizadas [-1,1].
     *   5. Crea un FloatBuffer con esos vértices y configura a_Position.
     *   6. Llama a glDrawArrays(GL_TRIANGLE_STRIP, 0, 4) para ejecutar el fragment shader.
     *   7. Deshabilita el array de vértices.
     *
     * @see -screenWidth, screenHeight - dimensiones estáticas definidas en SceneRenderer.
     *
     * ASCII Art:
     *   ╔════════════════════════════════════════════════════════════════╗
     *   ║         🖌️  draw(): Renderiza quad + shader GLSL         ║
     *   ╚════════════════════════════════════════════════════════════════╝
     *          ┌────────────────────────────────────────────┐
     *          │ ▪ glUseProgram(program)                   │
     *          │ ▪ Pasa uniforms: u_Time, u_Resolution     │
     *          │ ▪ Vertices quad pantalla [-1,-1]-[1,1]    │
     *          │ ▪ glVertexAttribPointer(a_Position, …)    │
     *          │ ▪ glDrawArrays(GL_TRIANGLE_STRIP)         │
     *          │ ▪ glDisableVertexAttribArray(a_Position)  │
     *          └────────────────────────────────────────────┘
     */
    @Override
    public void draw() {
        // 1️⃣ Activar programa shader
        GLES20.glUseProgram(program);

        // 2️⃣ Pasar uniform “u_Time” (tiempo en segundos)
        GLES20.glUniform1f(uTimeLocation, time);

        // 3️⃣ Pasar uniform “u_Resolution” (ancho, alto)
        GLES20.glUniform2f(uResolutionLocation,
                (float) screenWidth,
                (float) screenHeight);

        // 4️⃣ Definir vertices de un cuadrado que cubre toda la pantalla:
        //    Coordenadas XY en NDC (Normalized Device Coordinates) de [-1,1]
        float[] quad = {
                -1f, -1f,  // esquina inferior izquierda
                1f, -1f,  // esquina inferior derecha
                -1f,  1f,  // esquina superior izquierda
                1f,  1f   // esquina superior derecha
        };
        // Crear FloatBuffer a partir del array de vértices
        FloatBuffer buf = ShaderUtils.createFloatBuffer(quad);

        // 5️⃣ Obtener ubicación del atributo a_Position en el shader
        int posLoc = GLES20.glGetAttribLocation(program, "a_Position");
        // Habilitar el atributo de vértices
        GLES20.glEnableVertexAttribArray(posLoc);
        // Enlazar el buffer de vértices al atributo (2 floats por vértice, sin stride)
        GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 0, buf);

        // 6️⃣ Dibujar el quad como TRIANGLE_STRIP (cuatro vértices → dos triángulos)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // 7️⃣ Deshabilitar el array de vértices
        GLES20.glDisableVertexAttribArray(posLoc);
    }
}
