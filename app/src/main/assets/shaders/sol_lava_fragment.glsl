// ============================================
// archivo: sol_lava_fragment.glsl
// Shader de sol procedural SIMPLIFICADO
// Sin distorsión, solo gradientes suaves y animación mínima
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

// ------- Varyings -------
varying vec2 v_TexCoord;
varying vec3 v_WorldPos;

// ============================================
// Ruido simple (sin distorsión)
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

// ════════════════════════════════════════════════════════════════
// ☀️ SOL CON VIDA - Manchas solares + flujo de plasma
// ════════════════════════════════════════════════════════════════

vec3 getSunColor(vec2 uv, float time) {
    vec2 center = vec2(0.5, 0.5);
    float dist = length(uv - center);
    float angle = atan(uv.y - center.y, uv.x - center.x);

    // ═══════════════════════════════════════════════════════════
    // 🌀 FLUJO DE PLASMA (movimiento visible)
    // ═══════════════════════════════════════════════════════════
    vec2 flowUV = uv * 3.0;
    flowUV.x += time * 0.08;  // Flujo horizontal
    flowUV.y += sin(uv.x * 10.0 + time * 0.1) * 0.05; // Ondulación

    float plasma = noise(flowUV) * 0.5 + noise(flowUV * 2.0) * 0.25;

    // ═══════════════════════════════════════════════════════════
    // ☀️ MANCHAS SOLARES (dark spots que se mueven)
    // ═══════════════════════════════════════════════════════════
    vec2 spotUV = uv * 6.0 + vec2(time * 0.03, time * 0.02);
    float spots = noise(spotUV);
    spots = smoothstep(0.6, 0.7, spots); // Solo manchas oscuras

    // Manchas más oscuras
    float darkening = 1.0 - spots * 0.4;

    // ═══════════════════════════════════════════════════════════
    // 🎨 GRADIENTE RADIAL + PLASMA
    // ═══════════════════════════════════════════════════════════
    float intensity = 1.0 - smoothstep(0.0, 0.5, dist);
    intensity += plasma * 0.3;
    intensity *= darkening; // Aplicar manchas oscuras

    // ═══════════════════════════════════════════════════════════
    // 🔥 PALETA SOLAR (naranja → amarillo → blanco)
    // ═══════════════════════════════════════════════════════════
    vec3 deepOrange = vec3(0.8, 0.25, 0.0);   // Naranja oscuro (menos amarillo)
    vec3 brightOrange = vec3(1.0, 0.5, 0.1);  // Naranja brillante
    vec3 hotYellow = vec3(1.0, 0.85, 0.4);    // Amarillo cálido

    vec3 color;
    if (intensity < 0.3) {
        color = mix(deepOrange, brightOrange, intensity / 0.3);
    } else if (intensity < 0.7) {
        color = mix(brightOrange, hotYellow, (intensity - 0.3) / 0.4);
    } else {
        color = hotYellow;
    }

    // Añadir variación de plasma
    color += vec3(plasma * 0.2, plasma * 0.15, plasma * 0.05);

    // Pulsación SUTIL
    float pulse = sin(time * 0.12) * 0.02 + 0.98;
    color *= pulse;

    return color;
}

// ============================================
// Main
// ============================================

void main() {
    vec2 uv = v_TexCoord;

    // Color del sol (limpio, sin distorsión)
    vec3 sunColor = getSunColor(uv, u_Time);

    // Si hay textura, mezclarla MÍNIMAMENTE
    if(u_UseSolidColor == 0) {
        vec4 texColor = texture2D(u_Texture, uv);
        float texIntensity = (texColor.r + texColor.g + texColor.b) / 3.0;
        sunColor *= 0.9 + texIntensity * 0.1;  // Influencia mínima
    }

    // ════════════════════════════════════════════════════════════════
    // ☀️ CORONA SOLAR SIMPLE - Solo glow suave, sin rayos complejos
    // ════════════════════════════════════════════════════════════════
    vec2 center = vec2(0.5, 0.5);
    float dist = length(uv - center);

    // Corona MUY simple - solo un glow radial suave
    float coronaStart = 0.42;
    float coronaEnd = 0.60;
    float coronaMask = smoothstep(coronaStart, coronaStart + 0.08, dist) *
                       (1.0 - smoothstep(coronaEnd - 0.08, coronaEnd, dist));

    // Glow amarillo suave
    vec3 coronaGlow = vec3(1.0, 0.9, 0.5) * 0.3 * coronaMask;
    sunColor += coronaGlow;

    // Alpha final - siempre opaco
    float finalAlpha = 1.0;

    gl_FragColor = vec4(sunColor, finalAlpha);
}