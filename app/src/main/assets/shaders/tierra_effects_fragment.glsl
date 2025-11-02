// ============================================
// archivo: tierra_effects_fragment.glsl
// 🌍 TIERRA - CAPA DE EFECTOS ATMOSFÉRICOS
// Atmósfera + Nubes + Océanos brillantes + Auroras
// Diseñado para compositing sobre la Tierra texturizada
// ============================================

#ifdef GL_ES
precision mediump float;
#endif

// ------- Uniforms -------
uniform float u_Time;
uniform vec2 u_Resolution;
uniform float u_Health;  // 0.0 - 1.0 (para efectos cuando está dañada)

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
// 🌫️ NUBES ANIMADAS (sobre la superficie)
// ============================================

vec3 animatedClouds(vec2 uv, float time) {
    vec2 center = vec2(0.5, 0.5);
    float dist = length(uv - center);

    // Solo visible dentro del disco
    if (dist > 0.48) return vec3(0.0);

    // Nubes en movimiento (MÁS LENTAS)
    vec2 cloudUV = uv * 8.0;
    cloudUV.x += time * 0.01;  // MUY lentas

    float clouds = fbm(cloudUV, 4);
    clouds = smoothstep(0.45, 0.65, clouds);

    // Nubes blancas semitransparentes (MÁS TRANSPARENTES)
    vec3 cloudColor = vec3(1.0, 1.0, 1.0);

    return cloudColor * clouds * 0.15;  // Más sutiles y transparentes
}

// ============================================
// 🌊 OCÉANOS BRILLANTES (especular)
// ============================================

vec3 oceanSpecular(vec2 uv, float time) {
    vec2 center = vec2(0.5, 0.5);
    float dist = length(uv - center);

    // Solo visible dentro del disco
    if (dist > 0.48) return vec3(0.0);

    // Detectar áreas "oscuras" en la textura base (océanos)
    // Usamos ruido para simular donde están los océanos
    vec2 oceanUV = uv * 5.0;
    float oceanMask = fbm(oceanUV, 3);
    oceanMask = smoothstep(0.3, 0.5, oceanMask);

    // Onda de agua brillante (MÁS LENTA)
    vec2 waveUV = uv * 15.0;
    waveUV += vec2(time * 0.02, time * 0.016);  // Muy lentas
    float waves = sin(waveUV.x * 3.0 + time * 0.2) * sin(waveUV.y * 4.0 + time * 0.14);
    waves = waves * 0.5 + 0.5;
    waves = pow(waves, 3.0);

    // Brillo especular azul-blanco (MÁS TRANSPARENTE)
    vec3 oceanGlow = mix(
        vec3(0.3, 0.6, 1.0),  // Azul océano
        vec3(0.8, 0.9, 1.0),  // Blanco brillante
        waves
    );

    return oceanGlow * oceanMask * waves * 0.12;  // Más transparente
}

// ============================================
// 🌌 ATMÓSFERA BRILLANTE (halo azul)
// ============================================

vec3 atmosphereGlow(vec2 uv, float time) {
    vec2 center = vec2(0.5, 0.5);
    float dist = length(uv - center);

    // Halo atmosférico en el borde
    float atmosStart = 0.42;
    float atmosEnd = 0.65;

    float atmosMask = smoothstep(atmosStart, atmosStart + 0.05, dist) *
                      (1.0 - smoothstep(atmosEnd - 0.08, atmosEnd, dist));

    // Color azul cielo con variación
    vec3 atmosColor = vec3(0.4, 0.7, 1.0);  // Azul cielo

    // Pulsación sutil (MÁS LENTA)
    float pulse = sin(time * 0.1) * 0.2 + 0.8;

    return atmosColor * atmosMask * pulse * 0.3;  // Más transparente
}

// ============================================
// 🌈 AURORAS BOREALES (en los polos)
// ============================================

vec3 polarAuroras(vec2 uv, float time) {
    vec2 center = vec2(0.5, 0.5);
    vec2 fromCenter = uv - center;
    float dist = length(fromCenter);

    // Solo en el borde del disco
    if (dist < 0.35 || dist > 0.48) return vec3(0.0);

    // Polos norte y sur (arriba y abajo)
    float latitude = abs(fromCenter.y);
    float polarMask = smoothstep(0.2, 0.4, latitude);

    // Ondas de aurora (MÁS LENTAS)
    vec2 auroraUV = uv * 10.0;
    auroraUV.x += sin(uv.y * 8.0 + time * 0.1) * 0.3;
    auroraUV.y += time * 0.03;

    float aurora = fbm(auroraUV, 3);
    aurora = smoothstep(0.4, 0.6, aurora);

    // Colores de aurora (verde-azul-violeta, MÁS LENTOS)
    float colorShift = sin(time * 0.06 + uv.x * 5.0) * 0.5 + 0.5;
    vec3 auroraColor = mix(
        vec3(0.2, 1.0, 0.5),   // Verde brillante
        vec3(0.5, 0.3, 1.0),   // Violeta
        colorShift
    );

    return auroraColor * aurora * polarMask * 0.2;  // Más transparente
}

// ============================================
// ⚡ EFECTO DE DAÑO (cuando HP bajo)
// ============================================

vec3 damageEffect(vec2 uv, float time, float health) {
    // Solo visible cuando HP < 50%
    if (health > 0.5) return vec3(0.0);

    vec2 center = vec2(0.5, 0.5);
    float dist = length(uv - center);

    // Solo dentro del disco
    if (dist > 0.48) return vec3(0.0);

    float damageIntensity = 1.0 - (health * 2.0);  // 0.0 - 1.0

    // Grietas rojas pulsantes
    vec2 crackUV = uv * 12.0;
    float cracks = fbm(crackUV, 3);
    cracks = smoothstep(0.65, 0.7, cracks);

    // Parpadeo de alerta (MÁS LENTO)
    float blink = sin(time * 0.8) * 0.5 + 0.5;

    vec3 damageColor = vec3(1.0, 0.2, 0.0);  // Rojo fuego

    return damageColor * cracks * damageIntensity * blink * 0.3;  // Más transparente
}

// ============================================
// 🎵 MAIN - EFECTOS ATMOSFÉRICOS
// ============================================

void main() {
    vec2 uv = v_TexCoord;
    vec2 center = vec2(0.5, 0.5);
    float dist = length(uv - center);

    // Obtener health (default 1.0 si no está disponible)
    float health = 1.0;
    #ifdef u_Health
        health = u_Health;
    #endif

    // ═══════════════════════════════════════════════════════════
    // 🌍 EFECTOS ATMOSFÉRICOS
    // ═══════════════════════════════════════════════════════════

    vec3 effects = vec3(0.0);

    // 🌫️ Nubes animadas (sobre tierra)
    effects += animatedClouds(uv, u_Time);

    // 🌊 Océanos brillantes (especular)
    effects += oceanSpecular(uv, u_Time);

    // 🌌 Atmósfera brillante (halo azul)
    effects += atmosphereGlow(uv, u_Time);

    // 🌈 Auroras boreales (polos)
    effects += polarAuroras(uv, u_Time);

    // ⚡ Efecto de daño cuando HP bajo
    effects += damageEffect(uv, u_Time, health);

    // Calcular alpha (MÁS TRANSPARENTE)
    float alpha = length(effects);
    alpha = clamp(alpha, 0.0, 0.6);  // Máximo 60% opacidad

    gl_FragColor = vec4(effects, alpha);
}
