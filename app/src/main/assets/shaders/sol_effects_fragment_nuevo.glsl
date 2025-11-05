// ============================================
// archivo: sol_effects_fragment_nuevo.glsl
// ☀️ CAPA DE EFECTOS - TEXTURA + EFECTOS VISUALES
// Misma textura que el sol base + efectos con transparencia
// ============================================

#ifdef GL_ES
precision mediump float;
#endif

// ------- Uniforms -------
uniform float u_Time;
uniform vec2 u_Resolution;
uniform sampler2D u_Texture;  // Misma textura que el sol base

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
// 🎨 MAIN - EFECTOS VISUALES CON TRANSPARENCIA
// ============================================

void main() {
    vec2 uv = v_TexCoord;
    vec2 center = vec2(0.5, 0.5);
    float dist = length(uv - center);

    // ═══════════════════════════════════════════════════════════
    // 🎨 TEXTURA BASE (misma que el sol principal)
    // ═══════════════════════════════════════════════════════════
    vec4 texColor = texture2D(u_Texture, uv);

    // ═══════════════════════════════════════════════════════════
    // 🌡️ GRADIENTE RADIAL DE TEMPERATURA
    // ═══════════════════════════════════════════════════════════
    vec3 colorCentroBlanco = vec3(1.0, 1.0, 0.98);      // Blanco super caliente
    vec3 colorAmarillo = vec3(1.0, 0.95, 0.65);         // Amarillo brillante
    vec3 colorNaranja = vec3(1.0, 0.65, 0.25);          // Naranja
    vec3 colorNaranjaOscuro = vec3(0.95, 0.45, 0.15);   // Naranja oscuro bordes

    vec3 gradientColor;
    if (dist < 0.15) {
        gradientColor = mix(colorCentroBlanco, colorAmarillo, dist / 0.15);
    } else if (dist < 0.35) {
        gradientColor = mix(colorAmarillo, colorNaranja, (dist - 0.15) / 0.2);
    } else {
        gradientColor = mix(colorNaranja, colorNaranjaOscuro, (dist - 0.35) / 0.15);
    }

    // ═══════════════════════════════════════════════════════════
    // 🌊 CÉLULAS DE CONVECCIÓN / GRANULACIÓN SOLAR
    // ═══════════════════════════════════════════════════════════
    vec2 cellUV = uv * 12.0;
    cellUV += vec2(u_Time * 0.02, u_Time * 0.015);

    float cells = fbm(cellUV, 4);
    float cellPattern = smoothstep(0.35, 0.5, cells) * smoothstep(0.75, 0.6, cells);

    float cellBrightness = 1.0 - dist * 0.5;
    cellPattern *= cellBrightness;

    // ═══════════════════════════════════════════════════════════
    // ✨ BRILLO REACTIVO AL DESPLAZAMIENTO
    // ═══════════════════════════════════════════════════════════
    float displacementBrightness = v_Displacement * 3.0;
    displacementBrightness = clamp(displacementBrightness, -0.3, 0.3);

    // ═══════════════════════════════════════════════════════════
    // 🎯 COMBINAR TEXTURA + EFECTOS
    // ═══════════════════════════════════════════════════════════

    // Mezclar textura con gradiente (30% gradiente)
    vec3 finalColor = mix(texColor.rgb, gradientColor, 0.3);

    // Añadir células de convección
    finalColor *= (0.8 + cellPattern * 0.4);

    // Añadir brillo reactivo
    finalColor += gradientColor * displacementBrightness * 0.6;

    // Aumentar brillo
    finalColor *= 1.2;

    // ═══════════════════════════════════════════════════════════
    // 🌟 ALPHA - Controla transparencia de toda la capa
    // ═══════════════════════════════════════════════════════════
    float alpha = 0.4;  // Semitransparente (ajustable)

    gl_FragColor = vec4(finalColor, alpha);
}
