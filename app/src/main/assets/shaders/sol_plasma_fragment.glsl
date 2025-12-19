/**
 * ╔════════════════════════════════════════════════════════════════╗
 * ║  ☀️ SOL CON PLASMA - Procedural Sun with Plasma Effects      ║
 * ║  Plasma + Manchas Solares + Corona + Erupciones              ║
 * ╚════════════════════════════════════════════════════════════════╝
 *
 * Efectos:
 * - Plasma animado con FBM multi-octava
 * - Manchas solares con cellular noise
 * - Corona brillante con gradiente HSB
 * - Erupciones solares en el borde
 * - Distorsión procedural para movimiento orgánico
 * - Emisión de luz intensa
 *
 * Optimizado para móviles - 60fps objetivo
 */

#ifdef GL_ES
precision mediump float;
#endif

// ═══════════════════════════════════════════════════════════════
// UNIFORMS
// ═══════════════════════════════════════════════════════════════
uniform float u_Time;
uniform mat4 u_MVP;
uniform vec2 u_Resolution;
uniform float u_Alpha;

// Varyings
varying vec2 v_TexCoord;
varying vec3 v_Normal;
varying vec3 v_WorldPos;

// ═══════════════════════════════════════════════════════════════
// CONFIGURACIÓN DE EFECTOS - VERSIÓN SUTIL
// ═══════════════════════════════════════════════════════════════
const float PLASMA_SPEED = 0.05;         // Velocidad de plasma (más lento)
const float PLASMA_INTENSITY = 0.25;     // Intensidad del plasma (muy reducida)
const float SUNSPOT_SCALE = 4.0;         // Escala de manchas solares
const float CORONA_SIZE = 0.3;           // Tamaño de la corona (reducido)
const float FLARE_INTENSITY = 0.15;      // Intensidad de erupciones (reducida)

// ═══════════════════════════════════════════════════════════════
// FUNCIONES DE LA SHADER LIBRARY
// ═══════════════════════════════════════════════════════════════

// ─── core.glsl ────
float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

float noise(vec2 st) {
    vec2 i = floor(st);
    vec2 f = fract(st);
    float a = random(i);
    float b = random(i + vec2(1.0, 0.0));
    float c = random(i + vec2(0.0, 1.0));
    float d = random(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float fbm(vec2 st, int octaves) {
    float value = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;
    for (int i = 0; i < 8; i++) {
        if (i >= octaves) break;
        value += amplitude * noise(st * frequency);
        frequency *= 2.0;
        amplitude *= 0.5;
    }
    return value;
}

// ─── color.glsl ────
vec3 hsb2rgb(vec3 c) {
    vec3 rgb = clamp(abs(mod(c.x * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0, 0.0, 1.0);
    rgb = rgb * rgb * (3.0 - 2.0 * rgb);
    return c.z * mix(vec3(1.0), rgb, c.y);
}

// ─── effects.glsl ────
float cellularNoise(vec2 st, float scale) {
    st *= scale;
    vec2 i_st = floor(st);
    vec2 f_st = fract(st);
    float min_dist = 1.0;

    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 neighbor = vec2(float(x), float(y));
            vec2 point = random(i_st + neighbor) * vec2(1.0);
            vec2 diff = neighbor + point - f_st;
            float dist = length(diff);
            min_dist = min(min_dist, dist);
        }
    }

    return min_dist;
}

// ═══════════════════════════════════════════════════════════════
// EFECTOS ESPECÍFICOS DEL SOL
// ═══════════════════════════════════════════════════════════════

/**
 * Genera plasma animado usando FBM
 */
float plasmaPattern(vec2 uv, float time) {
    // Distorsionar UVs para movimiento orgánico
    vec2 distorted = uv;
    distorted.x += noise(uv * 2.0 + time * PLASMA_SPEED * 0.5) * 0.1;
    distorted.y += noise(uv * 2.0 + vec2(100.0) + time * PLASMA_SPEED * 0.5) * 0.1;

    // FBM para plasma multi-escala
    float plasma = fbm(distorted * 3.0 + time * PLASMA_SPEED, 4);

    return plasma;
}

/**
 * Genera manchas solares (zonas más oscuras)
 */
float sunspots(vec2 uv, float time) {
    // Manchas solares se mueven lentamente
    vec2 st = uv + vec2(time * 0.01, 0.0);

    // Cellular noise para manchas irregulares
    float spots = cellularNoise(st, SUNSPOT_SCALE);

    // Solo mostrar las manchas más densas
    return smoothstep(0.1, 0.2, spots);
}

/**
 * Genera corona solar (halo brillante)
 */
float corona(vec3 normal, vec3 viewDir, float time) {
    // Efecto Fresnel (brilla en los bordes)
    float fresnel = pow(1.0 - max(dot(normal, viewDir), 0.0), 2.0);

    // Animación de pulsación
    float pulse = 0.5 + 0.5 * sin(time * 0.5);

    return fresnel * pulse;
}

/**
 * Genera erupciones solares en el borde
 */
float solarFlares(vec2 uv, vec3 normal, vec3 viewDir, float time) {
    // Solo en el borde
    float edge = pow(1.0 - max(dot(normal, viewDir), 0.0), 4.0);

    // Erupciones aleatorias
    float flareNoise = noise(uv * 10.0 + time * 0.2);
    float flares = smoothstep(0.7, 0.9, flareNoise);

    return edge * flares;
}

/**
 * Mapa de temperatura del sol (núcleo más caliente)
 */
float temperatureMap(vec2 uv, float plasma) {
    // Centro más caliente
    vec2 center = uv - vec2(0.5);
    float distFromCenter = length(center);

    // Temperatura disminuye del centro hacia afuera
    float temp = 1.0 - smoothstep(0.0, 0.5, distFromCenter);

    // Modular con plasma
    temp = mix(temp, temp + plasma * 0.3, PLASMA_INTENSITY);

    return temp;
}

// ═══════════════════════════════════════════════════════════════
// UNIFORMS ADICIONALES
// ═══════════════════════════════════════════════════════════════
uniform sampler2D u_Texture;

// ═══════════════════════════════════════════════════════════════
// MAIN
// ═══════════════════════════════════════════════════════════════

void main() {
    // ═══════════════════════════════════════════════════════════
    // 1. CONFIGURACIÓN BÁSICA
    // ═══════════════════════════════════════════════════════════
    vec2 uv = v_TexCoord;
    // ✅ FIX: ObjLoader ya maneja el flip de V para modelos Meshy
    // NO voltear aquí para evitar doble flip
    vec2 texUV = uv;

    vec3 normal = normalize(v_Normal);
    vec3 viewDir = normalize(-v_WorldPos);

    // ═══════════════════════════════════════════════════════════
    // 2. TEXTURA BASE DEL SOL (principal)
    // ═══════════════════════════════════════════════════════════
    vec4 texColor = texture2D(u_Texture, texUV);
    vec3 baseColor = texColor.rgb * 1.2;  // Textura original brillante

    // ═══════════════════════════════════════════════════════════
    // 3. EFECTO DE PLASMA SUTIL (aparece de vez en cuando)
    // ═══════════════════════════════════════════════════════════
    float plasma = plasmaPattern(uv, u_Time);

    // Onda que controla cuándo aparece el plasma (cada ~8 segundos)
    float plasmaWave = sin(u_Time * 0.4) * 0.5 + 0.5;  // 0 a 1
    plasmaWave = smoothstep(0.6, 1.0, plasmaWave);  // Solo activo parte del tiempo

    // Color del plasma (naranja brillante)
    vec3 plasmaColor = vec3(1.0, 0.6, 0.2) * plasma;

    // ═══════════════════════════════════════════════════════════
    // 4. DISTORSIÓN DE CALOR SUTIL
    // ═══════════════════════════════════════════════════════════
    float heatDistort = sin(uv.y * 20.0 + u_Time * 2.0) * 0.02;
    heatDistort *= sin(u_Time * 0.5) * 0.5 + 0.5;  // Intermitente

    vec2 distortedUV = texUV + vec2(heatDistort, 0.0);
    vec3 distortedColor = texture2D(u_Texture, distortedUV).rgb;

    // ═══════════════════════════════════════════════════════════
    // 5. MEZCLAR: 90% textura + 10% efectos
    // ═══════════════════════════════════════════════════════════
    vec3 finalColor = baseColor;

    // Añadir distorsión de calor muy sutil
    finalColor = mix(finalColor, distortedColor * 1.2, 0.15);

    // Añadir plasma solo cuando la onda lo permite
    finalColor += plasmaColor * plasmaWave * 0.2;

    // ═══════════════════════════════════════════════════════════
    // 6. BRILLO EN LOS BORDES (corona muy sutil)
    // ═══════════════════════════════════════════════════════════
    float fresnel = pow(1.0 - max(dot(normal, viewDir), 0.0), 3.0);
    finalColor += vec3(1.0, 0.8, 0.4) * fresnel * 0.15;

    // ═══════════════════════════════════════════════════════════
    // 7. SALIDA FINAL
    // ═══════════════════════════════════════════════════════════
    finalColor = clamp(finalColor, 0.0, 1.0);
    gl_FragColor = vec4(finalColor, texColor.a * u_Alpha);
}

/**
 * ════════════════════════════════════════════════════════════
 * 🎨 VARIACIONES POSIBLES
 * ════════════════════════════════════════════════════════════
 *
 * Para diferentes efectos solares:
 *
 * 1. PLASMA MÁS RÁPIDO (sol más activo):
 *    - Cambiar PLASMA_SPEED de 0.1 a 0.3
 *
 * 2. MÁS MANCHAS SOLARES:
 *    - En sunspots(), cambiar SUNSPOT_SCALE de 5.0 a 8.0
 *    - Cambiar threshold de 0.1-0.2 a 0.2-0.3
 *
 * 3. CORONA MÁS GRANDE:
 *    - Cambiar CORONA_SIZE de 1.3 a 2.0
 *
 * 4. SOL MÁS ROJO (estrella enana roja):
 *    - En color base, cambiar hue de 0.0-0.15 a 0.0-0.05
 *
 * 5. SOL MÁS AZUL (estrella gigante azul):
 *    - Cambiar hue de 0.0-0.15 a 0.50-0.65
 *    - Aumentar brightness a 1.0 + plasma * 0.5
 *
 * 6. MÁS ERUPCIONES:
 *    - Cambiar FLARE_INTENSITY de 0.5 a 1.5
 *    - En solarFlares(), threshold de 0.7-0.9 a 0.5-0.7
 *
 * 7. PLASMA MÁS INTENSO:
 *    - Cambiar PLASMA_INTENSITY de 0.8 a 1.5
 *    - Usar 5 octavas en FBM en lugar de 4
 *
 * 8. SOL ALIENÍGENA (verde/púrpura):
 *    - Cambiar toda la paleta de colores HSB
 *    - Verde: hue 0.33, Púrpura: hue 0.80
 *
 * ════════════════════════════════════════════════════════════
 */
