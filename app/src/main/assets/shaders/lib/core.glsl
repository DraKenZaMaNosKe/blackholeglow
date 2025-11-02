/**
 * ╔════════════════════════════════════════════════════════════════╗
 * ║  🎲 CORE.GLSL - Funciones Fundamentales                      ║
 * ║  Black Hole Glow Shader Library v1.0.0                       ║
 * ╚════════════════════════════════════════════════════════════════╝
 *
 * Funciones base para ruido, aleatoriedad y operaciones matemáticas.
 * Optimizado para OpenGL ES 2.0 (móviles Android).
 *
 * Basado en "The Book of Shaders" by Patricio Gonzalez Vivo
 * https://thebookofshaders.com/
 */

#ifdef GL_ES
precision mediump float;
#endif

// ═══════════════════════════════════════════════════════════════
// 🎲 RANDOM - Ruido Pseudo-Aleatorio 2D
// ═══════════════════════════════════════════════════════════════
/**
 * Genera un valor pseudo-aleatorio a partir de coordenadas 2D.
 *
 * @param st    Coordenadas de entrada (normalizado 0-1 recomendado)
 * @return      Valor aleatorio entre 0.0 y 1.0
 *
 * Uso:
 *   float r = random(st);
 *   float r = random(st * 10.0); // Más "células" de ruido
 */
float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

// ═══════════════════════════════════════════════════════════════
// 🌊 NOISE - Ruido Suave (Value Noise)
// ═══════════════════════════════════════════════════════════════
/**
 * Ruido suave interpolado - más orgánico que random().
 *
 * @param st    Coordenadas de entrada
 * @return      Valor de ruido suave entre 0.0 y 1.0
 *
 * Uso:
 *   float n = noise(st * 5.0 + u_Time);
 */
float noise(vec2 st) {
    vec2 i = floor(st);
    vec2 f = fract(st);

    // Four corners in 2D of a tile
    float a = random(i);
    float b = random(i + vec2(1.0, 0.0));
    float c = random(i + vec2(0.0, 1.0));
    float d = random(i + vec2(1.0, 1.0));

    // Smooth interpolation (smoothstep)
    vec2 u = f * f * (3.0 - 2.0 * f);

    // Mix 4 corners
    return mix(a, b, u.x) +
           (c - a) * u.y * (1.0 - u.x) +
           (d - b) * u.x * u.y;
}

// ═══════════════════════════════════════════════════════════════
// 🌀 FBM - Fractal Brownian Motion
// ═══════════════════════════════════════════════════════════════
/**
 * Ruido fractal multi-octava - patrones naturales complejos.
 * Combina múltiples capas de noise a diferentes escalas.
 *
 * @param st        Coordenadas de entrada
 * @param octaves   Número de capas (más = más detalle, pero más lento)
 * @return          Valor de ruido fractal entre 0.0 y 1.0
 *
 * Uso:
 *   float clouds = fbm(st * 3.0, 4);  // 4 octavas
 *   float terrain = fbm(st * 10.0, 6); // 6 octavas = más detalle
 *
 * NOTA: Más octavas = más costoso. Usar 3-5 en móviles.
 */
float fbm(vec2 st, int octaves) {
    float value = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;

    for (int i = 0; i < 8; i++) {
        if (i >= octaves) break;

        value += amplitude * noise(st * frequency);
        frequency *= 2.0;
        amplitude *= 0.5;
    }

    return value;
}

// ═══════════════════════════════════════════════════════════════
// 📐 UTILIDADES MATEMÁTICAS
// ═══════════════════════════════════════════════════════════════

/**
 * Mapea un valor de un rango a otro.
 * Equivalente a Arduino map() o Processing map().
 *
 * @param value     Valor a mapear
 * @param inMin     Mínimo del rango de entrada
 * @param inMax     Máximo del rango de entrada
 * @param outMin    Mínimo del rango de salida
 * @param outMax    Máximo del rango de salida
 * @return          Valor mapeado al nuevo rango
 *
 * Uso:
 *   float mapped = map(x, 0.0, 1.0, -1.0, 1.0); // 0-1 a -1-1
 */
float map(float value, float inMin, float inMax, float outMin, float outMax) {
    return outMin + (outMax - outMin) * ((value - inMin) / (inMax - inMin));
}

/**
 * Normaliza coordenadas de píxel a rango 0-1.
 * Corrige aspect ratio para formas no distorsionadas.
 *
 * @param fragCoord     gl_FragCoord.xy
 * @param resolution    u_Resolution
 * @return              Coordenadas normalizadas (0-1) con aspect ratio correcto
 *
 * Uso:
 *   vec2 st = normalizeCoords(gl_FragCoord.xy, u_Resolution);
 */
vec2 normalizeCoords(vec2 fragCoord, vec2 resolution) {
    vec2 st = fragCoord / resolution.xy;
    st.x *= resolution.x / resolution.y; // Corregir aspect ratio
    return st;
}

/**
 * Centra las coordenadas en el origen (0, 0).
 * Útil para dibujar formas desde el centro.
 *
 * @param st    Coordenadas normalizadas (0-1)
 * @return      Coordenadas centradas (-0.5 a 0.5)
 *
 * Uso:
 *   vec2 centered = centerCoords(st);
 */
vec2 centerCoords(vec2 st) {
    return st - vec2(0.5);
}

// ═══════════════════════════════════════════════════════════════
// 🎨 SMOOTHING FUNCTIONS
// ═══════════════════════════════════════════════════════════════

/**
 * Interpolación suave (easing) - más suave que smoothstep().
 * Útil para transiciones y animaciones fluidas.
 *
 * @param t     Valor entre 0.0 y 1.0
 * @return      Valor interpolado suavemente
 *
 * Uso:
 *   float smooth = smootherStep(t);
 */
float smootherStep(float t) {
    return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
}

/**
 * Pulso suave - sube y baja de forma orgánica.
 * Perfecto para efectos de "respiración" o latido.
 *
 * @param t         Tiempo (u_Time)
 * @param speed     Velocidad del pulso
 * @return          Valor entre 0.0 y 1.0
 *
 * Uso:
 *   float pulse = smoothPulse(u_Time, 2.0); // Pulso cada 2 seg
 */
float smoothPulse(float t, float speed) {
    return (sin(t * speed) + 1.0) * 0.5;
}

/**
 * ════════════════════════════════════════════════════════════
 * 💡 EJEMPLOS DE USO
 * ════════════════════════════════════════════════════════════
 *
 * // 1. RUIDO ANIMADO
 * vec2 st = normalizeCoords(gl_FragCoord.xy, u_Resolution);
 * float n = noise(st * 10.0 + u_Time);
 * gl_FragColor = vec4(vec3(n), 1.0);
 *
 * // 2. NUBES PROCEDURALES
 * float clouds = fbm(st * 3.0 + u_Time * 0.1, 4);
 * gl_FragColor = vec4(vec3(clouds), 1.0);
 *
 * // 3. ESTRELLAS ALEATORIAS
 * float stars = step(0.98, random(floor(st * 100.0)));
 * gl_FragColor = vec4(vec3(stars), 1.0);
 *
 * ════════════════════════════════════════════════════════════
 */
