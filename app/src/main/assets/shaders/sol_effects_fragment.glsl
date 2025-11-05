// ============================================
// archivo: sol_effects_fragment.glsl
// ☀️ SOL - CAPA DE EFECTOS: CORONA SOLAR REALISTA
// Corona con rayos radiales como en fotos reales
// Diseñado para compositing sobre el sol texturizado
// ============================================

#ifdef GL_ES
precision mediump float;
#endif

// ------- Uniforms -------
uniform float u_Time;
uniform vec2 u_Resolution;

// ------- Varyings -------
varying vec2 v_TexCoord;
varying vec3 v_WorldPos;

// ============================================
// 🔧 FUNCIONES DE RUIDO
// ============================================

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));

    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

// ============================================
// 👑 CORONA SOLAR REALISTA CON RAYOS
// ============================================

vec3 solarCorona(vec2 uv, float time) {
    vec2 center = vec2(0.5, 0.5);
    float dist = length(uv - center);
    float angle = atan(uv.y - center.y, uv.x - center.x);

    // La corona empieza en el borde del sol
    float coronaStart = 0.45;
    float coronaEnd = 0.85;

    // Solo renderizar en la zona de la corona
    if (dist < coronaStart || dist > coronaEnd) return vec3(0.0);

    // ═══════════════════════════════════════════════════════════
    // ⚡ RAYOS RADIALES (lo más importante)
    // ═══════════════════════════════════════════════════════════

    // Crear variación angular usando ruido - esto crea los rayos
    float angleNoise = noise(vec2(angle * 3.0, time * 0.1)) * 0.5 +
                       noise(vec2(angle * 6.0, time * 0.15)) * 0.3 +
                       noise(vec2(angle * 12.0, time * 0.08)) * 0.2;

    // Los rayos son más intensos en ciertas direcciones angulares
    float rayPattern = angleNoise;

    // ═══════════════════════════════════════════════════════════
    // 🌊 VARIACIÓN RADIAL (streamers que fluyen hacia afuera)
    // ═══════════════════════════════════════════════════════════

    // Crear variación en la distancia - algunos rayos llegan más lejos
    float radialNoise = noise(vec2(angle * 4.0 + time * 0.05, dist * 2.0));

    // Algunos rayos son más largos
    float rayLength = 0.7 + radialNoise * 0.3;

    // ═══════════════════════════════════════════════════════════
    // 💫 INTENSIDAD DE LA CORONA
    // ═══════════════════════════════════════════════════════════

    // Fade in desde el borde del sol
    float fadeIn = smoothstep(coronaStart, coronaStart + 0.03, dist);

    // Fade out gradual hacia afuera (más largo que un halo simple)
    float fadeOut = 1.0 - smoothstep(coronaStart + 0.15, coronaEnd * rayLength, dist);

    // Combinar fades
    float coronaIntensity = fadeIn * fadeOut;

    // Multiplicar por el patrón de rayos - MUCHO MÁS CONTRASTE
    coronaIntensity *= (0.1 + rayPattern * 0.9);

    // ═══════════════════════════════════════════════════════════
    // 🎨 COLOR DE LA CORONA
    // ═══════════════════════════════════════════════════════════

    // Colores MUY BRILLANTES para que se vean
    vec3 coronaColorInner = vec3(1.0, 1.0, 0.95);   // Blanco super brillante
    vec3 coronaColorMid = vec3(1.0, 0.95, 0.7);     // Dorado brillante
    vec3 coronaColorOuter = vec3(1.0, 0.8, 0.4);    // Naranja brillante

    // Mezcla de colores según distancia
    float colorMix = (dist - coronaStart) / (coronaEnd - coronaStart);
    vec3 coronaColor;

    if (colorMix < 0.5) {
        coronaColor = mix(coronaColorInner, coronaColorMid, colorMix * 2.0);
    } else {
        coronaColor = mix(coronaColorMid, coronaColorOuter, (colorMix - 0.5) * 2.0);
    }

    // Añadir MÁS variación de color según los rayos
    coronaColor *= (0.6 + rayPattern * 0.8);

    // ═══════════════════════════════════════════════════════════
    // ✨ OUTPUT FINAL - MUCHO MÁS INTENSO
    // ═══════════════════════════════════════════════════════════

    return coronaColor * coronaIntensity * 2.5;  // 3x más brillante que antes
}

// ============================================
// 🎵 MAIN - CORONA REALISTA
// ============================================

void main() {
    vec2 uv = v_TexCoord;
    vec2 center = vec2(0.5, 0.5);
    float dist = length(uv - center);

    // ═══════════════════════════════════════════════════════════
    // 🔄 ROTACIÓN DE LA CORONA
    // ═══════════════════════════════════════════════════════════
    float angle = atan(uv.y - center.y, uv.x - center.x);

    // Agregar rotación basada en el tiempo (1 vuelta completa cada ~30 segundos)
    angle += u_Time * 0.2;  // Velocidad de rotación visible

    vec3 corona = vec3(0.0);
    float alpha = 0.0;

    // Corona MÁS CERCANA al sol - solo en el borde
    float coronaStart = 0.45;  // Borde del sol
    float coronaEnd = 0.62;    // Mucho más cerca (antes era 0.85)

    // Solo renderizar en la zona de corona
    if (dist > coronaStart && dist < coronaEnd) {

        // ═══════════════════════════════════════════════════════════
        // ⚡ RAYOS SIMPLES usando senos (12 rayos principales)
        // ═══════════════════════════════════════════════════════════

        // Crear patrón de 12 rayos usando sin()
        float rayPattern = sin(angle * 6.0) * 0.5 + 0.5;  // 12 rayos (6*2)

        // Hacer los rayos más definidos con pow
        rayPattern = pow(rayPattern, 3.0);  // Rayos más delgados y contrastados

        // Agregar un poco de ruido para irregularidad MÍNIMA
        float angleNoise = noise(vec2(angle * 4.0, u_Time * 0.05));
        rayPattern = rayPattern * (0.7 + angleNoise * 0.3);

        // Los rayos son más anchos cerca del sol, más delgados lejos
        float rayWidth = 1.0 - (dist - coronaStart) / (coronaEnd - coronaStart) * 0.5;
        rayPattern *= rayWidth;

        // ═══════════════════════════════════════════════════════════
        // 🎨 COLOR Y FADE
        // ═══════════════════════════════════════════════════════════

        // Color brillante
        corona = vec3(1.0, 0.95, 0.7);  // Dorado brillante

        // Fade in/out
        float fadeIn = smoothstep(coronaStart, coronaStart + 0.02, dist);
        float fadeOut = 1.0 - smoothstep(coronaEnd - 0.08, coronaEnd, dist);

        // Combinar todo - los rayos son MUY visibles
        alpha = fadeIn * fadeOut * (0.3 + rayPattern * 0.7);
    }

    // Output
    gl_FragColor = vec4(corona * alpha, alpha);
}
