/**
 * ╔════════════════════════════════════════════════════════════════╗
 * ║  ✨ EFFECTS.GLSL - Efectos Avanzados y Patrones             ║
 * ║  Black Hole Glow Shader Library v1.0.0                       ║
 * ╚════════════════════════════════════════════════════════════════╝
 *
 * Efectos procedurales avanzados:
 * - Cellular Noise (Worley Noise) - ORGÁNICO e IMPRESIONANTE
 * - Voronoi Diagrams - Patrones de células
 * - Grid y Patterns - Geometría procedural
 *
 * NOTA: Estos efectos son más costosos que noise() básico.
 * Usar con moderación en móviles.
 *
 * Basado en "The Book of Shaders" - Cellular Noise chapter
 */

#ifdef GL_ES
precision mediump float;
#endif

// ═══════════════════════════════════════════════════════════════
// Nota: Necesita función random() de core.glsl
// Copiar aquí si no usas sistema de includes:
// ═══════════════════════════════════════════════════════════════

// float random(vec2 st) {
//     return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
// }

// ═══════════════════════════════════════════════════════════════
// 🦠 CELLULAR NOISE - El Efecto MÁS ORGÁNICO
// ═══════════════════════════════════════════════════════════════

/**
 * Cellular Noise (Worley Noise) - Diagrama de Voronoi.
 * Crea patrones que parecen CÉLULAS, BURBUJAS, PIEDRAS, AGUA.
 *
 * CÓMO FUNCIONA:
 * - Divide el espacio en una cuadrícula (tiling)
 * - Coloca un punto aleatorio en cada celda
 * - Calcula la distancia al punto más cercano
 *
 * @param st        Coordenadas de entrada
 * @param scale     Escala (más alto = más células)
 * @return          Distancia al punto más cercano (0.0 a ~1.0)
 *
 * Uso:
 *   float cells = cellularNoise(st, 5.0);
 *   gl_FragColor = vec4(vec3(cells), 1.0);
 *
 *   // Animado:
 *   float cells = cellularNoise(st * 5.0 + u_Time * 0.1, 5.0);
 *
 *   // Como textura orgánica:
 *   vec3 color = hsb2rgb(vec3(cells, 0.7, 0.9));
 *
 * RENDIMIENTO: MEDIO - Usa con moderación (escala 3-10 OK)
 *
 * Fuente: The Book of Shaders - Cellular Noise
 * https://thebookofshaders.com/12/
 */
float cellularNoise(vec2 st, float scale) {
    st *= scale;

    // Tile the space
    vec2 i_st = floor(st);
    vec2 f_st = fract(st);

    float min_dist = 1.0;  // Minimum distance

    // Buscar en las 9 celdas vecinas (3x3)
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            // Celda vecina
            vec2 neighbor = vec2(float(x), float(y));

            // Punto aleatorio en esta celda
            vec2 point = random(i_st + neighbor) * vec2(1.0);

            // Vector desde pixel hasta punto
            vec2 diff = neighbor + point - f_st;

            // Distancia al punto
            float dist = length(diff);

            // Guardar la distancia mínima
            min_dist = min(min_dist, dist);
        }
    }

    return min_dist;
}

/**
 * Cellular Noise que retorna la SEGUNDA distancia más cercana.
 * Útil para crear BORDES entre células.
 *
 * @param st        Coordenadas
 * @param scale     Escala
 * @return          vec2(distancia_1, distancia_2)
 *
 * Uso:
 *   vec2 cells = cellularNoise2(st, 5.0);
 *   float borders = cells.y - cells.x;  // Distancia entre puntos
 *   float outline = smoothstep(0.0, 0.1, borders);
 */
vec2 cellularNoise2(vec2 st, float scale) {
    st *= scale;

    vec2 i_st = floor(st);
    vec2 f_st = fract(st);

    float min_dist1 = 1.0;
    float min_dist2 = 1.0;

    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 neighbor = vec2(float(x), float(y));
            vec2 point = random(i_st + neighbor) * vec2(1.0);
            vec2 diff = neighbor + point - f_st;
            float dist = length(diff);

            if (dist < min_dist1) {
                min_dist2 = min_dist1;
                min_dist1 = dist;
            } else if (dist < min_dist2) {
                min_dist2 = dist;
            }
        }
    }

    return vec2(min_dist1, min_dist2);
}

/**
 * Cellular Noise ANIMADO con puntos que se mueven.
 *
 * @param st        Coordenadas
 * @param scale     Escala
 * @param time      Tiempo (u_Time)
 * @param speed     Velocidad de animación
 * @return          Distancia al punto más cercano
 *
 * Uso:
 *   float cells = cellularNoiseAnimated(st, 5.0, u_Time, 0.2);
 *
 * RENDIMIENTO: BAJO - Más costoso que cellularNoise()
 */
float cellularNoiseAnimated(vec2 st, float scale, float time, float speed) {
    st *= scale;

    vec2 i_st = floor(st);
    vec2 f_st = fract(st);

    float min_dist = 1.0;

    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 neighbor = vec2(float(x), float(y));

            // Punto animado
            vec2 point = random(i_st + neighbor) * vec2(1.0);
            point = vec2(
                0.5 + 0.5 * sin(time * speed + random(i_st + neighbor) * 6.28),
                0.5 + 0.5 * cos(time * speed + random(i_st + neighbor + vec2(1.0)) * 6.28)
            );

            vec2 diff = neighbor + point - f_st;
            float dist = length(diff);
            min_dist = min(min_dist, dist);
        }
    }

    return min_dist;
}

// ═══════════════════════════════════════════════════════════════
// 📐 GRID Y PATTERNS
// ═══════════════════════════════════════════════════════════════

/**
 * Grid/cuadrícula procedural.
 *
 * @param st        Coordenadas
 * @param scale     Tamaño de la cuadrícula
 * @param thickness Grosor de las líneas
 * @return          1.0 en las líneas, 0.0 en los espacios
 *
 * Uso:
 *   float grid = gridPattern(st, 10.0, 0.05);
 */
float gridPattern(vec2 st, float scale, float thickness) {
    st *= scale;
    vec2 grid = abs(fract(st - 0.5) - 0.5) / fwidth(st);
    float line = min(grid.x, grid.y);
    return 1.0 - min(line, 1.0);
}

/**
 * Grid con líneas suaves (anti-aliased).
 *
 * @param st        Coordenadas
 * @param scale     Tamaño
 * @param thickness Grosor
 * @return          Valor entre 0-1 con bordes suaves
 */
float smoothGrid(vec2 st, float scale, float thickness) {
    st *= scale;
    vec2 f = fract(st);
    vec2 d = min(f, 1.0 - f);
    float grid = min(d.x, d.y);
    return smoothstep(thickness, thickness + 0.01, grid);
}

/**
 * Patrón de tablero de ajedrez.
 *
 * @param st        Coordenadas
 * @param scale     Tamaño de los cuadrados
 * @return          0.0 o 1.0 alternado
 *
 * Uso:
 *   float checker = checkerboard(st, 10.0);
 */
float checkerboard(vec2 st, float scale) {
    st *= scale;
    vec2 i = floor(st);
    return mod(i.x + i.y, 2.0);
}

/**
 * Patrón de rayas horizontales.
 *
 * @param st        Coordenadas
 * @param scale     Número de rayas
 * @return          0.0 o 1.0
 */
float stripes(vec2 st, float scale) {
    return step(0.5, fract(st.y * scale));
}

/**
 * Patrón de puntos (dots).
 *
 * @param st        Coordenadas
 * @param scale     Densidad de puntos
 * @param radius    Tamaño de puntos
 * @return          1.0 en puntos, 0.0 fuera
 *
 * Uso:
 *   float dots = dotPattern(st, 10.0, 0.2);
 */
float dotPattern(vec2 st, float scale, float radius) {
    st *= scale;
    vec2 f = fract(st) - 0.5;
    float d = length(f);
    return step(d, radius);
}

// ═══════════════════════════════════════════════════════════════
// 🌊 EFECTOS DE ONDAS Y DISTORSIÓN
// ═══════════════════════════════════════════════════════════════

/**
 * Ondas radiales concéntricas.
 *
 * @param st            Coordenadas (centradas)
 * @param frequency     Frecuencia de ondas
 * @param amplitude     Amplitud
 * @param time          Tiempo para animar
 * @return              Valor de onda (-1 a 1)
 *
 * Uso:
 *   vec2 st = gl_FragCoord.xy / u_Resolution.xy - 0.5;
 *   float wave = radialWaves(st, 10.0, 0.5, u_Time);
 */
float radialWaves(vec2 st, float frequency, float amplitude, float time) {
    float r = length(st);
    return sin(r * frequency - time) * amplitude;
}

/**
 * Distorsión de ondas en coordenadas.
 * Útil para efectos de agua o calor.
 *
 * @param st            Coordenadas
 * @param frequency     Frecuencia de distorsión
 * @param amplitude     Fuerza de distorsión
 * @param time          Tiempo
 * @return              Coordenadas distorsionadas
 *
 * Uso:
 *   vec2 distorted = waveDistortion(st, 5.0, 0.05, u_Time);
 *   vec4 tex = texture2D(u_Texture, distorted);
 */
vec2 waveDistortion(vec2 st, float frequency, float amplitude, float time) {
    st.x += sin(st.y * frequency + time) * amplitude;
    st.y += cos(st.x * frequency + time) * amplitude;
    return st;
}

/**
 * Efecto de turbulencia/vórtice.
 *
 * @param st        Coordenadas (centradas)
 * @param strength  Fuerza del vórtice
 * @param time      Tiempo
 * @return          Coordenadas rotadas
 *
 * Uso:
 *   vec2 vortex = vortexEffect(st, 2.0, u_Time);
 */
vec2 vortexEffect(vec2 st, float strength, float time) {
    float r = length(st);
    float angle = atan(st.y, st.x);
    angle += sin(time) * strength / (r + 0.1);

    return vec2(
        cos(angle) * r,
        sin(angle) * r
    );
}

// ═══════════════════════════════════════════════════════════════
// 🎆 EFECTOS DE PARTÍCULAS Y BRILLO
// ═══════════════════════════════════════════════════════════════

/**
 * Genera "estrellas" aleatorias en posiciones fijas.
 *
 * @param st            Coordenadas
 * @param density       Densidad de estrellas (ej. 100.0 = grid 100x100)
 * @param threshold     Umbral de aparición (0.95 = 5% de pixels)
 * @param brightness    Brillo de estrellas
 * @return              Brillo de estrella en esta posición
 *
 * Uso:
 *   float stars = starfield(st, 100.0, 0.98, 1.0);
 *   gl_FragColor = vec4(vec3(stars), 1.0);
 */
float starfield(vec2 st, float density, float threshold, float brightness) {
    vec2 grid = floor(st * density);
    float r = random(grid);
    return step(threshold, r) * brightness;
}

/**
 * Estrellas con parpadeo (twinkle).
 *
 * @param st            Coordenadas
 * @param density       Densidad
 * @param threshold     Umbral
 * @param time          Tiempo
 * @return              Brillo parpadeante
 */
float twinklingStars(vec2 st, float density, float threshold, float time) {
    vec2 grid = floor(st * density);
    float r = random(grid);

    if (r > threshold) {
        float twinkle = 0.5 + 0.5 * sin(time * 3.0 + r * 100.0);
        return twinkle;
    }

    return 0.0;
}

/**
 * ════════════════════════════════════════════════════════════
 * 💡 EJEMPLOS DE USO
 * ════════════════════════════════════════════════════════════
 *
 * // 1. CELLULAR NOISE BÁSICO (Burbujas)
 * vec2 st = gl_FragCoord.xy / u_Resolution.xy;
 * float cells = cellularNoise(st, 5.0);
 * gl_FragColor = vec4(vec3(cells), 1.0);
 *
 * // 2. BORDES DE CÉLULAS (Estilo manga/comic)
 * vec2 cells2 = cellularNoise2(st, 8.0);
 * float borders = cells2.y - cells2.x;
 * float outline = smoothstep(0.0, 0.05, borders);
 * gl_FragColor = vec4(vec3(outline), 1.0);
 *
 * // 3. AGUA/LAVA con COLOR HSB
 * float organic = cellularNoise(st * 3.0 + u_Time * 0.1, 5.0);
 * vec3 color = hsb2rgb(vec3(0.55 + organic * 0.1, 0.7, 0.9));
 * gl_FragColor = vec4(color, 1.0);
 *
 * // 4. ESTRELLAS PARPADEANTES
 * vec2 centered = st - 0.5;
 * float stars = twinklingStars(st, 50.0, 0.97, u_Time);
 * gl_FragColor = vec4(vec3(stars), 1.0);
 *
 * // 5. GRID CYBERPUNK
 * float grid = smoothGrid(st, 20.0, 0.02);
 * vec3 neon = vec3(0.0, 1.0, 1.0) * (1.0 - grid);
 * gl_FragColor = vec4(neon, 1.0);
 *
 * ════════════════════════════════════════════════════════════
 */
