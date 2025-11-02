/**
 * ╔════════════════════════════════════════════════════════════════╗
 * ║  🌊 OCÉANO PROFUNDO - Deep Ocean Shader                     ║
 * ║  Wallpaper orgánico con cellular noise y HSB colors         ║
 * ╚════════════════════════════════════════════════════════════════╝
 *
 * Efectos:
 * - Cellular noise para simular burbujas y corrientes
 * - Gradiente HSB para profundidad oceánica
 * - Rayos de luz filtrados (God rays)
 * - Movimiento ondulante del agua
 *
 * Optimizado para móviles - 60fps garantizado
 */

#ifdef GL_ES
precision mediump float;
#endif

uniform vec2 u_Resolution;
uniform float u_Time;

// ═══════════════════════════════════════════════════════════════
// FUNCIONES DE LA LIBRERÍA (Black Hole Glow Shader Library)
// ═══════════════════════════════════════════════════════════════

// ─── core.glsl ────
float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

float noise(vec2 st) {
    vec2 i = floor(st);
    vec2 f = fract(st);
    float a = random(i);
    float b = random(i + vec2(1.0, 0.0));
    float c = random(i + vec2(0.0, 1.0));
    float d = random(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

// ─── color.glsl ────
vec3 hsb2rgb(vec3 c) {
    vec3 rgb = clamp(abs(mod(c.x * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0, 0.0, 1.0);
    rgb = rgb * rgb * (3.0 - 2.0 * rgb);
    return c.z * mix(vec3(1.0), rgb, c.y);
}

// ─── effects.glsl ────
float cellularNoise(vec2 st, float scale) {
    st *= scale;
    vec2 i_st = floor(st);
    vec2 f_st = fract(st);
    float min_dist = 1.0;

    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 neighbor = vec2(float(x), float(y));
            vec2 point = random(i_st + neighbor) * vec2(1.0);
            vec2 diff = neighbor + point - f_st;
            float dist = length(diff);
            min_dist = min(min_dist, dist);
        }
    }

    return min_dist;
}

// ═══════════════════════════════════════════════════════════════
// EFECTOS ESPECÍFICOS DEL OCÉANO
// ═══════════════════════════════════════════════════════════════

/**
 * Simula corrientes oceánicas con ruido.
 */
vec2 oceanCurrents(vec2 st, float time) {
    float flowX = noise(st * 2.0 + time * 0.1) * 0.03;
    float flowY = noise(st * 2.0 + vec2(100.0) + time * 0.15) * 0.02;
    return st + vec2(flowX, flowY);
}

/**
 * Rayos de luz que penetran el agua (God rays).
 */
float godRays(vec2 st, float time) {
    // Rayos verticales con variación
    float rays = 0.0;

    for (int i = 0; i < 3; i++) {
        float offset = float(i) * 0.33;
        float x = st.x + offset + sin(time * 0.3 + float(i)) * 0.1;
        float ray = sin(x * 20.0 + time * 0.5) * 0.5 + 0.5;
        ray = pow(ray, 8.0); // Afilar rayos
        rays += ray;
    }

    // Fade por profundidad (más rayos arriba)
    rays *= smoothstep(1.0, 0.3, st.y);

    return rays * 0.15;
}

/**
 * Burbujas ascendiendo usando cellular noise.
 */
float bubbles(vec2 st, float time) {
    // Movimiento ascendente
    st.y -= time * 0.15;

    // Cellular noise para burbujas
    float cells = cellularNoise(st, 8.0);

    // Solo mostrar como burbujas (no fondo continuo)
    return smoothstep(0.15, 0.1, cells) * 0.3;
}

// ═══════════════════════════════════════════════════════════════
// MAIN
// ═══════════════════════════════════════════════════════════════

void main() {
    vec2 st = gl_FragCoord.xy / u_Resolution.xy;

    // ═══════════════════════════════════════════════════════════
    // 1. APLICAR CORRIENTES OCEÁNICAS
    // ═══════════════════════════════════════════════════════════
    vec2 flowingSt = oceanCurrents(st, u_Time);

    // ═══════════════════════════════════════════════════════════
    // 2. FONDO CON CELLULAR NOISE (Textura orgánica)
    // ═══════════════════════════════════════════════════════════
    float organicTexture = cellularNoise(flowingSt * 3.0, 5.0);

    // ═══════════════════════════════════════════════════════════
    // 3. GRADIENTE DE PROFUNDIDAD (Oscuro abajo, más claro arriba)
    // ═══════════════════════════════════════════════════════════
    float depth = st.y;

    // Color oceánico usando HSB
    // Hue: azul (0.55) con ligera variación por textura
    // Saturation: alta (0.7)
    // Brightness: varía con profundidad y textura
    vec3 oceanColor = hsb2rgb(vec3(
        0.55 + organicTexture * 0.05,  // Hue: azul con variación
        0.7,                            // Saturation: color intenso
        0.3 + depth * 0.4 + organicTexture * 0.1  // Brightness: más claro arriba
    ));

    // ═══════════════════════════════════════════════════════════
    // 4. AGREGAR RAYOS DE LUZ (God rays)
    // ═══════════════════════════════════════════════════════════
    float rays = godRays(st, u_Time);
    oceanColor += vec3(rays);

    // ═══════════════════════════════════════════════════════════
    // 5. AGREGAR BURBUJAS ASCENDENTES
    // ═══════════════════════════════════════════════════════════
    float bubbleGlow = bubbles(st, u_Time);
    oceanColor += vec3(bubbleGlow) * vec3(0.8, 0.9, 1.0); // Burbujas azul claro

    // ═══════════════════════════════════════════════════════════
    // 6. TOQUE FINAL: Vignette (oscurecer bordes)
    // ═══════════════════════════════════════════════════════════
    vec2 centered = st - 0.5;
    float vignette = 1.0 - length(centered) * 0.5;
    oceanColor *= vignette;

    gl_FragColor = vec4(oceanColor, 1.0);
}

/**
 * ════════════════════════════════════════════════════════════
 * 🎨 VARIACIONES POSIBLES
 * ════════════════════════════════════════════════════════════
 *
 * Para experimentar, prueba cambiar:
 *
 * 1. COLOR: Cambiar hue en línea 122
 *    - 0.33 = verde (selva submarina)
 *    - 0.60 = azul profundo
 *    - 0.45 = turquesa tropical
 *
 * 2. BURBUJAS: Cambiar escala en cellularNoise()
 *    - 5.0 = burbujas grandes
 *    - 12.0 = burbujas pequeñas
 *
 * 3. CORRIENTES: Ajustar velocidad en oceanCurrents()
 *    - time * 0.05 = muy lento
 *    - time * 0.2 = más rápido
 *
 * 4. LUZ: Intensidad de rayos (línea 76, return rays * 0.15)
 *    - * 0.05 = sutil
 *    - * 0.3 = muy visible
 *
 * ════════════════════════════════════════════════════════════
 */
