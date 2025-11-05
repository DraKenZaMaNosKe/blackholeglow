// ============================================
// archivo: sol_lava_fragment.glsl
// ☀️ SOL ÉPICO - COMBO COMPLETO
// Corona + Llamaradas + Plasma Burbujeante + Música
// ============================================

#ifdef GL_ES
precision mediump float;
#endif

// ------- Uniforms -------
uniform float u_Time;
uniform sampler2D u_Texture;
uniform int u_UseSolidColor;
uniform vec4 u_SolidColor;
uniform float u_Alpha;
uniform vec2 u_Resolution;

// 🎵 Reactividad Musical (DESHABILITADA - no disponible en Planeta.java)
// uniform float u_MusicBass;      // 0.0-1.0 (graves)
// uniform float u_MusicTreble;    // 0.0-1.0 (agudos)
// uniform float u_MusicBeat;      // 0.0-1.0 (beats)

// ------- Varyings -------
varying vec2 v_TexCoord;
varying vec3 v_WorldPos;
varying float v_Displacement;  // Recibido del vertex shader

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

// Ruido fractal (FBM) para detalles complejos
float fbm(vec2 p, int octaves) {
    float value = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;

    for (int i = 0; i < 8; i++) {
        if (i >= octaves) break;
        value += amplitude * noise(p * frequency);
        frequency *= 2.0;
        amplitude *= 0.5;
    }
    return value;
}

// ============================================
// 🌊 PLASMA BURBUJEANTE (Convección Solar)
// ============================================

float plasmaConvection(vec2 uv, float time) {
    // Células de convección en la superficie
    vec2 cellUV = uv * 8.0;

    // Animación lenta de burbujas
    cellUV += vec2(
        sin(time * 0.1 + cellUV.y * 3.0) * 0.3,
        cos(time * 0.15 + cellUV.x * 2.0) * 0.3
    );

    // Patrón de células hexagonales (burbujas)
    float cells = fbm(cellUV, 4);

    // Bordes de las células más oscuros
    float cellEdges = smoothstep(0.3, 0.5, cells) * smoothstep(0.7, 0.5, cells);

    return cellEdges * 0.6 + 0.4; // Variación 0.4 - 1.0
}

// ============================================
// 🔥 LLAMARADAS SOLARES (Solar Flares)
// ============================================

vec3 solarFlares(vec2 uv, float time, float musicIntensity) {
    vec2 center = vec2(0.5, 0.5);
    float angle = atan(uv.y - center.y, uv.x - center.x);
    float dist = length(uv - center);

    vec3 flareGlow = vec3(0.0);

    // 6 llamaradas en diferentes posiciones (más épico)
    for (int i = 0; i < 6; i++) {
        float seed = float(i) * 123.456;

        // Ángulo de la llamarada (rota lentamente)
        float flareAngle = (float(i) / 6.0) * 6.28318 + time * 0.15 + seed;

        // Intensidad que aparece/desaparece (SIEMPRE VISIBLE ahora)
        float flareTime = time * 0.25 + seed;
        float flareActive = sin(flareTime) * 0.4 + 0.6;  // Rango 0.2 - 1.0 (siempre visible)

        // Intensidad aumenta con música
        flareActive *= (1.0 + musicIntensity * 0.5);

        // Diferencia angular
        float angleDiff = abs(mod(angle - flareAngle + 3.14159, 6.28318) - 3.14159);

        // Forma de la llamarada (arco que se extiende) - MÁS ANCHAS
        float flareWidth = 0.3 + noise(vec2(time * 0.2, seed)) * 0.15;
        float flareMask = smoothstep(flareWidth, 0.0, angleDiff);

        // Extensión radial (sale MUCHO más del borde del sol)
        float extension = smoothstep(0.35, 0.50, dist) * smoothstep(0.95, 0.70, dist);

        // Intensidad de la llamarada
        float flareIntensity = flareMask * extension * flareActive;

        // Color: naranja brillante → amarillo → blanco
        vec3 flareColor = mix(
            vec3(1.0, 0.5, 0.1),  // Naranja intenso
            vec3(1.0, 1.0, 0.9),  // Blanco brillante
            flareIntensity * 0.8
        );

        // MUCHO MÁS BRILLANTE (4x en lugar de 1.5x)
        flareGlow += flareColor * flareIntensity * 4.0;
    }

    return flareGlow;
}

// ============================================
// 👑 CORONA SOLAR ÉPICA (Eclipse Effect)
// ============================================

vec3 epicCorona(vec2 uv, float time, float musicIntensity) {
    vec2 center = vec2(0.5, 0.5);
    float dist = length(uv - center);
    float angle = atan(uv.y - center.y, uv.x - center.x);

    vec3 coronaGlow = vec3(0.0);

    // ═══════════════════════════════════════════════════════════
    // 🌟 HALO PRINCIPAL (siempre visible)
    // ═══════════════════════════════════════════════════════════
    float coronaStart = 0.40;
    float coronaEnd = 0.75;
    float coronaMask = smoothstep(coronaStart, coronaStart + 0.05, dist) *
                       (1.0 - smoothstep(coronaEnd - 0.1, coronaEnd, dist));

    // Color base de la corona (dorado brillante)
    vec3 coronaColor = vec3(1.0, 0.9, 0.6);

    // Variación con ruido
    float coronaNoise = fbm(uv * 5.0 + time * 0.1, 3);
    coronaColor *= (0.8 + coronaNoise * 0.4);

    coronaGlow += coronaColor * coronaMask * 0.6;

    // ═══════════════════════════════════════════════════════════
    // ⚡ RAYOS RADIALES ROTATIVOS (dramáticos)
    // ═══════════════════════════════════════════════════════════
    int numRays = 12;
    for (int i = 0; i < 12; i++) {
        float rayAngle = (float(i) / float(numRays)) * 6.28318;
        rayAngle += time * 0.2; // Rotación lenta

        // Diferencia angular
        float angleDiff = abs(mod(angle - rayAngle + 3.14159, 6.28318) - 3.14159);

        // Ancho del rayo (delgado)
        float rayWidth = 0.08 + sin(time * 2.0 + float(i)) * 0.02;
        float rayMask = smoothstep(rayWidth, rayWidth * 0.3, angleDiff);

        // Extensión del rayo (solo en la corona)
        rayMask *= coronaMask;

        // Intensidad que pulsa
        float rayPulse = sin(time * 3.0 + float(i) * 0.5) * 0.3 + 0.7;
        rayPulse *= (1.0 + musicIntensity * 0.4); // Música aumenta brillo

        // Añadir rayo dorado brillante
        coronaGlow += vec3(1.0, 0.95, 0.7) * rayMask * rayPulse * 0.4;
    }

    // ═══════════════════════════════════════════════════════════
    // ✨ PARTÍCULAS FLOTANTES en la corona
    // ═══════════════════════════════════════════════════════════
    if (coronaMask > 0.1) {
        vec2 particleUV = uv * 30.0;
        particleUV += vec2(time * 0.05, time * 0.08); // Movimiento lento

        float particles = noise(particleUV);
        particles = smoothstep(0.7, 0.75, particles); // Partículas pequeñas

        coronaGlow += vec3(1.0, 1.0, 0.9) * particles * coronaMask * 0.3;
    }

    return coronaGlow;
}

// ============================================
// ☀️ SUPERFICIE DEL SOL (mejorada)
// ============================================

vec3 getSunSurface(vec2 uv, float time, float musicIntensity) {
    vec2 center = vec2(0.5, 0.5);
    float dist = length(uv - center);

    // ═══════════════════════════════════════════════════════════
    // 🌀 FLUJO DE PLASMA VISIBLE
    // ═══════════════════════════════════════════════════════════
    vec2 flowUV = uv * 4.0;
    flowUV.x += time * 0.12;
    flowUV.y += sin(uv.x * 15.0 + time * 0.2) * 0.08;

    float plasma = fbm(flowUV, 4);

    // ═══════════════════════════════════════════════════════════
    // 🌊 BURBUJAS DE CONVECCIÓN
    // ═══════════════════════════════════════════════════════════
    float convection = plasmaConvection(uv, time);

    // ═══════════════════════════════════════════════════════════
    // ☀️ MANCHAS SOLARES (oscuras)
    // ═══════════════════════════════════════════════════════════
    vec2 spotUV = uv * 7.0 + vec2(time * 0.04, time * 0.03);
    float spots = noise(spotUV);
    spots = smoothstep(0.65, 0.75, spots);
    float darkening = 1.0 - spots * 0.5;

    // ═══════════════════════════════════════════════════════════
    // 🎨 PALETA DE COLORES MEJORADA
    // ═══════════════════════════════════════════════════════════
    float intensity = 1.0 - smoothstep(0.0, 0.45, dist);
    intensity += plasma * 0.4;
    intensity *= convection;
    intensity *= darkening;

    // Pulsación con música
    float pulse = sin(time * 0.15) * 0.03 + 0.97;
    pulse += musicIntensity * 0.1; // Beats aumentan brillo
    intensity *= pulse;

    // Gradiente de color épico
    vec3 deepRed = vec3(0.7, 0.15, 0.0);      // Rojo profundo
    vec3 deepOrange = vec3(0.9, 0.3, 0.05);   // Naranja oscuro
    vec3 brightOrange = vec3(1.0, 0.55, 0.15); // Naranja brillante
    vec3 hotYellow = vec3(1.0, 0.9, 0.5);     // Amarillo cálido
    vec3 whiteHot = vec3(1.0, 1.0, 0.95);     // Blanco caliente

    vec3 color;
    if (intensity < 0.2) {
        color = mix(deepRed, deepOrange, intensity / 0.2);
    } else if (intensity < 0.5) {
        color = mix(deepOrange, brightOrange, (intensity - 0.2) / 0.3);
    } else if (intensity < 0.8) {
        color = mix(brightOrange, hotYellow, (intensity - 0.5) / 0.3);
    } else {
        color = mix(hotYellow, whiteHot, (intensity - 0.8) / 0.2);
    }

    // Variación de plasma
    color += vec3(plasma * 0.2, plasma * 0.15, plasma * 0.05);

    return color;
}

// ============================================
// 🎵 MAIN - COMBINAR TODO
// ============================================

void main() {
    vec2 uv = v_TexCoord;

    // ═══════════════════════════════════════════════════════════
    // ☀️ SOLO TEXTURA BASE - SIMPLE Y LIMPIA
    // ═══════════════════════════════════════════════════════════
    vec4 texColor = texture2D(u_Texture, uv);

    gl_FragColor = texColor;
}
