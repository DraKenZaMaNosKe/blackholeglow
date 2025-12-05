/**
 * ╔════════════════════════════════════════════════════════════════╗
 * ║  ☁️ CLOUD LAYER - Fragment Shader                              ║
 * ║  Nubes procedurales animadas con transparencia                 ║
 * ╚════════════════════════════════════════════════════════════════╝
 */

#ifdef GL_ES
precision mediump float;
#endif

uniform float u_Time;
uniform float u_Alpha;

varying vec2 v_TexCoord;
varying vec3 v_Position;

// ═══════════════════════════════════════════════════════════════
// FUNCIONES DE NOISE
// ═══════════════════════════════════════════════════════════════

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);

    return mix(
        mix(hash(i + vec2(0.0, 0.0)), hash(i + vec2(1.0, 0.0)), u.x),
        mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x),
        u.y
    );
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;

    for (int i = 0; i < 5; i++) {
        value += amplitude * noise(p * frequency);
        amplitude *= 0.5;
        frequency *= 2.0;
    }

    return value;
}

// ═══════════════════════════════════════════════════════════════
// MAIN
// ═══════════════════════════════════════════════════════════════

void main() {
    vec2 uv = v_TexCoord;

    // ─────────────────────────────────────────────────────────────
    // CAPA 1: Nubes principales moviéndose hacia el este
    // ─────────────────────────────────────────────────────────────
    float cloudSpeed1 = 0.03;
    vec2 cloudUV1 = uv;
    cloudUV1.x += u_Time * cloudSpeed1;

    float clouds1 = fbm(cloudUV1 * vec2(3.0, 2.0));

    // ─────────────────────────────────────────────────────────────
    // CAPA 2: Nubes secundarias más rápidas
    // ─────────────────────────────────────────────────────────────
    float cloudSpeed2 = 0.05;
    vec2 cloudUV2 = uv;
    cloudUV2.x += u_Time * cloudSpeed2;
    cloudUV2.y += u_Time * 0.01;

    float clouds2 = fbm(cloudUV2 * vec2(4.0, 2.5) + vec2(50.0, 25.0));

    // ─────────────────────────────────────────────────────────────
    // COMBINAR CAPAS
    // ─────────────────────────────────────────────────────────────
    float clouds = max(clouds1, clouds2 * 0.8);

    // Crear nubes más definidas con threshold
    clouds = smoothstep(0.45, 0.75, clouds);

    // ─────────────────────────────────────────────────────────────
    // MENOS NUBES EN LOS POLOS (más realista)
    // ─────────────────────────────────────────────────────────────
    float latitude = abs(uv.y - 0.5) * 2.0; // 0 en ecuador, 1 en polos
    float polarFade = 1.0 - smoothstep(0.6, 1.0, latitude);
    clouds *= polarFade;

    // ─────────────────────────────────────────────────────────────
    // COLOR Y TRANSPARENCIA
    // ─────────────────────────────────────────────────────────────

    // Color de las nubes: blanco brillante con toque azulado
    vec3 cloudColor = vec3(1.0, 1.0, 1.0);

    // Alpha basado en densidad de nubes
    float alpha = clouds * 0.85;

    // Si no hay nube, completamente transparente
    if (alpha < 0.05) {
        discard;
    }

    gl_FragColor = vec4(cloudColor, alpha * u_Alpha);
}
