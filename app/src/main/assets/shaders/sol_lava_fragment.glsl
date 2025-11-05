// ============================================
// ☀️ SOL - FRAGMENT SHADER MEJORADO
// Textura + Gradiente + Plasma + Llamaradas + Corona
// ============================================

#ifdef GL_ES
precision mediump float;
#endif

// Uniforms
uniform float u_Time;
uniform sampler2D u_Texture;

// Varyings del vertex shader
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
// 🌟 MAIN
// ============================================

void main() {
    // Coordenadas UV centradas (-0.5 a 0.5)
    vec2 uv = v_TexCoord - 0.5;

    // Distancia al centro (0.0 = centro, 0.5 = borde)
    float dist = length(uv);

    // Ángulo polar (para efectos direccionales)
    float angle = atan(uv.y, uv.x);

    // ═══════════════════════════════════════════════════════════
    // 1️⃣ TEXTURA BASE
    // ═══════════════════════════════════════════════════════════

    vec3 baseColor = texture2D(u_Texture, v_TexCoord).rgb;

    // ═══════════════════════════════════════════════════════════
    // 2️⃣ GRADIENTE RADIAL MEJORADO (temperatura)
    // ═══════════════════════════════════════════════════════════

    // Colores de temperatura más dramáticos
    vec3 coreColor = vec3(1.5, 1.3, 1.0);        // Centro: MUY brillante
    vec3 midColor = vec3(1.2, 1.0, 0.7);         // Medio: amarillo-dorado
    vec3 edgeColor = vec3(1.0, 0.5, 0.3);        // Borde: naranja-rojo intenso

    // Gradiente suave según distancia
    vec3 tempGradient = mix(coreColor, midColor, smoothstep(0.0, 0.25, dist));
    tempGradient = mix(tempGradient, edgeColor, smoothstep(0.25, 0.45, dist));

    // Aplicar gradiente sobre textura
    baseColor *= tempGradient;

    // ═══════════════════════════════════════════════════════════
    // 3️⃣ PLASMA BURBUJEANTE MEJORADO
    // ═══════════════════════════════════════════════════════════

    float time = u_Time * 0.08;

    // Capa 1: Plasma grande
    vec2 plasmaUV1 = uv * 3.5 + vec2(time * 0.06, time * 0.04);
    float plasma1 = noise(plasmaUV1);

    // Capa 2: Plasma medio
    vec2 plasmaUV2 = uv * 7.0 + vec2(-time * 0.08, time * 0.07);
    float plasma2 = noise(plasmaUV2);

    // Capa 3: Plasma fino
    vec2 plasmaUV3 = uv * 11.0 + vec2(time * 0.05, -time * 0.06);
    float plasma3 = noise(plasmaUV3);

    // Combinar con pesos
    float plasmaTotal = plasma1 * 0.5 + plasma2 * 0.3 + plasma3 * 0.2;

    // Aplicar plasma más intenso
    baseColor += vec3(plasmaTotal * 0.2, plasmaTotal * 0.15, plasmaTotal * 0.08);

    // ═══════════════════════════════════════════════════════════
    // 4️⃣ CORONA SOLAR (halo sutil)
    // ═══════════════════════════════════════════════════════════

    // Corona muy sutil justo en el borde
    float coronaMask = smoothstep(0.42, 0.48, dist) * smoothstep(0.52, 0.48, dist);

    // Color de corona (dorado brillante)
    vec3 coronaColor = vec3(1.3, 1.0, 0.6);

    // Variación de corona con tiempo
    float coronaPulse = sin(time * 0.3) * 0.2 + 0.8;

    baseColor += coronaColor * coronaMask * coronaPulse * 0.4;

    // ═══════════════════════════════════════════════════════════
    // 6️⃣ BRILLO NUCLEAR EN EL CENTRO
    // ═══════════════════════════════════════════════════════════

    float coreBrightness = smoothstep(0.25, 0.0, dist);
    baseColor += vec3(1.2, 1.1, 0.9) * coreBrightness * 0.5;

    // ═══════════════════════════════════════════════════════════
    // 7️⃣ OUTPUT FINAL - SIEMPRE OPACO
    // ═══════════════════════════════════════════════════════════

    gl_FragColor = vec4(baseColor, 1.0);  // ✅ Alpha = 1.0 SIEMPRE
}
