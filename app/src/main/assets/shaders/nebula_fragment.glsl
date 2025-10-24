#ifdef GL_ES
precision mediump float;
#endif

// ═══════════════════════════════════════════════════════════════════════
// ☁️ NEBULA FRAGMENT SHADER - Nubes de gas cósmico procedurales
// ═══════════════════════════════════════════════════════════════════════
// Genera nebulosas/nubes de gas con ruido procedural, sin textura
// Colores: azul, púrpura, naranja, rosa (como nebulosas reales)
// Movimiento suave con desplazamiento UV
// ═══════════════════════════════════════════════════════════════════════

varying vec2 v_TexCoord;
varying vec3 v_Position;

uniform float u_Time;
uniform vec3 u_NebulaColor;  // Color base de la nebulosa
uniform float u_Offset;      // Desplazamiento para animación

// ════════════════════════════════════════════════════════════════════════
// 🌀 FUNCIONES DE RUIDO PROCEDURAL (Simplex-like noise)
// ════════════════════════════════════════════════════════════════════════

// Hash para pseudo-randomness
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

// Ruido básico
float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);  // Smoothstep

    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));

    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

// FBM SIMPLIFICADO - sin loops (máxima compatibilidad)
float fbm(vec2 p) {
    float value = 0.0;

    // Octava 1
    value += 0.5 * noise(p);
    p *= 2.0;

    // Octava 2
    value += 0.25 * noise(p);
    p *= 2.0;

    // Octava 3
    value += 0.125 * noise(p);

    return value;
}

// ════════════════════════════════════════════════════════════════════════
// ☁️ MAIN - Generar nebulosa
// ════════════════════════════════════════════════════════════════════════

void main() {
    // ════════════════════════════════════════════════════════════════
    // ☁️ NEBULOSA - FORMA DEFINIDA POR RUIDO (no rectángulo!)
    // ════════════════════════════════════════════════════════════════

    vec2 uv = v_TexCoord;

    // ═══════════════════════════════════════════════════════════
    // 🌀 MOVIMIENTO
    // ═══════════════════════════════════════════════════════════
    vec2 movingUV = uv;
    movingUV.x += u_Offset * 0.05;
    movingUV.y += u_Time * 0.015;

    // ═══════════════════════════════════════════════════════════
    // ☁️ DENSIDAD DE NUBE - Contraste fuerte entre zonas densas/vacías
    // ═══════════════════════════════════════════════════════════
    // Escala grande para formas de nube
    float cloudDensity = fbm(movingUV * 2.5);

    // Escala mediana para detalles
    cloudDensity += fbm(movingUV * 5.0) * 0.5;

    // Normalizar
    cloudDensity = cloudDensity / 1.5;

    // ═══════════════════════════════════════════════════════════
    // 🎯 THRESHOLD - Crear "agujeros" en la nube
    // ═══════════════════════════════════════════════════════════
    // Solo mostrar donde la densidad es alta (> 0.35)
    cloudDensity = smoothstep(0.35, 0.7, cloudDensity);

    // Si la densidad es muy baja, descartar (crear forma orgánica)
    if (cloudDensity < 0.02) {
        discard;
    }

    // ═══════════════════════════════════════════════════════════
    // 🎨 COLOR - Variación en zonas densas
    // ═══════════════════════════════════════════════════════════
    float colorVariation = fbm(movingUV * 4.0);
    vec3 color1 = u_NebulaColor * 0.8;
    vec3 color2 = u_NebulaColor * 1.8;

    vec3 finalColor = mix(color1, color2, colorVariation);

    // Brillo en zonas muy densas
    finalColor += vec3(pow(cloudDensity, 3.0) * 0.5);

    // ═══════════════════════════════════════════════════════════
    // 💫 ALPHA - Basado en densidad con suavizado de bordes
    // ═══════════════════════════════════════════════════════════
    vec2 center = uv - 0.5;
    float distFromCenter = length(center);

    // Fade suave solo en bordes EXTREMOS (no en todo el quad)
    float edgeFade = 1.0 - smoothstep(0.35, 0.5, distFromCenter);

    // Alpha final = densidad * fade de bordes
    float alpha = cloudDensity * edgeFade * 0.6; // Max 60%

    gl_FragColor = vec4(finalColor, alpha);
}
