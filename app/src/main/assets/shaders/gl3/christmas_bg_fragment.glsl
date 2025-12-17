#version 300 es
// ╔═══════════════════════════════════════════════════════════════════╗
// ║   🎄 Christmas Background Fragment Shader - GLSL ES 3.0 🎄        ║
// ╚═══════════════════════════════════════════════════════════════════╝
//
// Efectos visuales para el bosque navideño:
// • Aurora boreal animada (ondulaciones sutiles)
// • Luces del pueblo parpadeando
// • Vignette atmosférico invernal
// • Partículas de nieve flotante (efecto visual)

precision mediump float;

// Inputs
in vec2 v_TexCoord;

// Outputs
out vec4 fragColor;

// Uniforms
uniform sampler2D u_Texture;
uniform float u_Time;
uniform vec2 u_Resolution;

// ════════════════════════════════════════════════════════════════════════
// 🔧 UTILIDADES
// ════════════════════════════════════════════════════════════════════════

// Ruido simple para efectos
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f); // smoothstep

    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));

    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

// ════════════════════════════════════════════════════════════════════════
// ✨ EFECTO AURORA BOREAL
// ════════════════════════════════════════════════════════════════════════

vec3 aurora(vec2 uv, float time) {
    // Solo aplicar en la parte superior de la imagen
    if (uv.y > 0.4) return vec3(0.0);

    // Ondulación base
    float wave = sin(uv.x * 8.0 + time * 0.5) * 0.03;
    wave += sin(uv.x * 4.0 - time * 0.3) * 0.02;

    // Intensidad basada en posición y ruido
    float intensity = smoothstep(0.4, 0.15, uv.y + wave);
    intensity *= noise(vec2(uv.x * 3.0 + time * 0.2, uv.y * 2.0)) * 0.5 + 0.5;

    // Colores de aurora (verde/cyan/violeta)
    vec3 auroraColor = mix(
        vec3(0.2, 0.8, 0.4),  // Verde
        vec3(0.5, 0.3, 0.8),  // Violeta
        sin(uv.x * 5.0 + time * 0.4) * 0.5 + 0.5
    );

    // Agregar cyan en los bordes
    auroraColor = mix(auroraColor, vec3(0.3, 0.9, 0.9),
                      sin(uv.x * 10.0 + time) * 0.3 + 0.2);

    return auroraColor * intensity * 0.15;
}

// ════════════════════════════════════════════════════════════════════════
// 💡 EFECTO LUCES PARPADEANTES
// ════════════════════════════════════════════════════════════════════════

vec3 twinklingLights(vec2 uv, float time) {
    vec3 lights = vec3(0.0);

    // Posiciones aproximadas de las luces en la imagen
    // (ajustar según la textura real)
    vec2 lightPositions[6];
    lightPositions[0] = vec2(0.35, 0.55);  // Cabaña izquierda
    lightPositions[1] = vec2(0.45, 0.52);  // Luz central
    lightPositions[2] = vec2(0.55, 0.54);  // Cabaña derecha
    lightPositions[3] = vec2(0.40, 0.60);  // Luz baja izq
    lightPositions[4] = vec2(0.50, 0.58);  // Luz baja centro
    lightPositions[5] = vec2(0.60, 0.62);  // Luz baja der

    vec3 lightColors[6];
    lightColors[0] = vec3(1.0, 0.9, 0.6);  // Amarillo cálido
    lightColors[1] = vec3(1.0, 0.8, 0.5);  // Naranja suave
    lightColors[2] = vec3(1.0, 0.95, 0.7); // Blanco cálido
    lightColors[3] = vec3(1.0, 0.7, 0.4);  // Naranja
    lightColors[4] = vec3(1.0, 0.85, 0.6); // Amarillo
    lightColors[5] = vec3(1.0, 0.9, 0.55); // Dorado

    for (int i = 0; i < 6; i++) {
        vec2 pos = lightPositions[i];
        vec3 col = lightColors[i];

        float dist = distance(uv, pos);

        // Parpadeo con diferentes frecuencias
        float flicker = 0.7 + 0.3 * sin(time * (2.0 + float(i) * 0.5) + float(i) * 1.5);
        flicker *= 0.8 + 0.2 * noise(vec2(time * 3.0, float(i)));

        // Glow suave
        float glow = exp(-dist * 40.0) * flicker;

        lights += col * glow * 0.3;
    }

    return lights;
}

// ════════════════════════════════════════════════════════════════════════
// ❄️ PARTÍCULAS DE NIEVE AMBIENTAL
// ════════════════════════════════════════════════════════════════════════

float snowParticle(vec2 uv, vec2 pos, float size) {
    float dist = distance(uv, pos);
    return smoothstep(size, size * 0.3, dist);
}

vec3 ambientSnow(vec2 uv, float time) {
    vec3 snow = vec3(0.0);

    // Varias capas de nieve con diferentes velocidades
    for (int layer = 0; layer < 3; layer++) {
        float speed = 0.03 + float(layer) * 0.02;
        float size = 0.003 - float(layer) * 0.0008;
        float density = 15.0 + float(layer) * 5.0;

        for (int i = 0; i < 8; i++) {
            float seed = float(i + layer * 10);

            // Posición con movimiento
            vec2 pos;
            pos.x = fract(hash(vec2(seed, 0.0)) + time * 0.05 * (hash(vec2(seed, 1.0)) - 0.5));
            pos.y = fract(hash(vec2(seed, 2.0)) - time * speed);

            // Ondulación horizontal
            pos.x += sin(pos.y * 10.0 + time + seed) * 0.02;

            float particle = snowParticle(uv, pos, size);

            // Alpha basado en profundidad (layer)
            float alpha = 0.3 - float(layer) * 0.08;

            snow += vec3(1.0, 1.0, 1.0) * particle * alpha;
        }
    }

    return snow;
}

// ════════════════════════════════════════════════════════════════════════
// 🎬 MAIN
// ════════════════════════════════════════════════════════════════════════

void main() {
    vec2 uv = v_TexCoord;

    // ═══ TEXTURA BASE ═══
    vec3 baseColor = texture(u_Texture, uv).rgb;

    // ═══ EFECTOS ═══
    vec3 auroraEffect = aurora(uv, u_Time);
    vec3 lightsEffect = twinklingLights(uv, u_Time);
    vec3 snowEffect = ambientSnow(uv, u_Time);

    // ═══ COMBINAR ═══
    vec3 finalColor = baseColor;
    finalColor += auroraEffect;
    finalColor += lightsEffect;
    finalColor += snowEffect;

    // ═══ TINTE INVERNAL (azul frío sutil) ═══
    vec3 winterTint = vec3(0.9, 0.95, 1.0);
    finalColor *= winterTint;

    // ═══ VIGNETTE ATMOSFÉRICO ═══
    vec2 centerUv = uv - 0.5;
    float distFromCenter = length(centerUv);
    float vignette = 1.0 - smoothstep(0.4, 0.9, distFromCenter);
    vignette = mix(0.7, 1.0, vignette);
    finalColor *= vignette;

    // ═══ AJUSTE DE BRILLO ═══
    finalColor *= 0.95;  // Ligeramente oscurecido para atmósfera nocturna

    fragColor = vec4(finalColor, 1.0);
}
