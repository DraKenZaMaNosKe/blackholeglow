/**
 * ╔════════════════════════════════════════════════════════════════╗
 * ║  🌍 TIERRA CON NUBES ANIMADAS - Fragment Shader                ║
 * ║  Nubes procedurales que se mueven sobre la superficie          ║
 * ╚════════════════════════════════════════════════════════════════╝
 */

#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_Texture;
uniform float u_Alpha;
uniform float u_Time;

varying vec2 v_TexCoord;

// ═══════════════════════════════════════════════════════════════
// FUNCIONES DE NOISE PARA NUBES PROCEDURALES
// ═══════════════════════════════════════════════════════════════

// Hash function para generar pseudo-random
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

// Smooth noise
float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);

    // Smooth interpolation
    vec2 u = f * f * (3.0 - 2.0 * f);

    return mix(
        mix(hash(i + vec2(0.0, 0.0)), hash(i + vec2(1.0, 0.0)), u.x),
        mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x),
        u.y
    );
}

// Fractal Brownian Motion para nubes más realistas
float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;

    // 4 octavas de noise
    for (int i = 0; i < 4; i++) {
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
    // ✅ FIX: ObjLoader ya maneja el flip de V para modelos Meshy
    vec2 uv = v_TexCoord;

    // ─────────────────────────────────────────────────────────────
    // 1. TEXTURA BASE DE LA TIERRA
    // ─────────────────────────────────────────────────────────────
    vec4 earthColor = texture2D(u_Texture, uv);

    // ─────────────────────────────────────────────────────────────
    // 2. GENERAR NUBES PROCEDURALES
    // ─────────────────────────────────────────────────────────────

    // Nubes se mueven con el tiempo
    float cloudSpeed = 0.05;
    vec2 cloudUV = uv;
    cloudUV.x += u_Time * cloudSpeed;

    // Escala de las nubes - GRANDES
    vec2 cloudScale = vec2(2.5, 1.5);

    // Generar patrón de nubes con FBM
    float cloudNoise = fbm(cloudUV * cloudScale);

    // Segundo layer
    vec2 cloudUV2 = uv;
    cloudUV2.x += u_Time * cloudSpeed * 0.7;
    float cloudNoise2 = fbm(cloudUV2 * cloudScale * 0.8 + vec2(100.0, 50.0));

    // Combinar layers
    float clouds = max(cloudNoise, cloudNoise2);

    // ─────────────────────────────────────────────────────────────
    // 3. AJUSTAR DENSIDAD - NUBES MUY VISIBLES
    // ─────────────────────────────────────────────────────────────

    // Crear nubes más definidas
    clouds = smoothstep(0.4, 0.7, clouds);

    // ─────────────────────────────────────────────────────────────
    // 4. MEZCLAR TIERRA CON NUBES
    // ─────────────────────────────────────────────────────────────

    // Color de las nubes (blanco puro)
    vec3 cloudColor = vec3(1.0, 1.0, 1.0);

    // Mezclar - nubes semi-transparentes sobre la tierra
    vec3 finalColor = mix(earthColor.rgb, cloudColor, clouds * 0.8);

    // Brillo
    finalColor *= 1.2;
    finalColor = clamp(finalColor, 0.0, 1.0);

    gl_FragColor = vec4(finalColor, earthColor.a * u_Alpha);
}
