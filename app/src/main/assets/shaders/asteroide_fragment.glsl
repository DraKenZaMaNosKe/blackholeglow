/**
 * ╔════════════════════════════════════════════════════════════════╗
 * ║  ☄️ ASTEROIDE REALISTA - Procedural Asteroid Shader          ║
 * ║  Textura rocosa + Cráteres + Variación de color              ║
 * ╚════════════════════════════════════════════════════════════════╝
 *
 * Efectos:
 * - Cellular noise para textura rocosa
 * - Cráteres procedurales
 * - Variación de color (gris, marrón, óxido)
 * - Iluminación basada en normales
 * - Rotación irregular
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
// EFECTOS ESPECÍFICOS DE ASTEROIDES
// ═══════════════════════════════════════════════════════════════

/**
 * Genera textura rocosa usando cellular noise
 */
float rockTexture(vec2 uv) {
    // Múltiples escalas de cellular noise para detalle
    float large = cellularNoise(uv, 3.0);   // Formaciones grandes
    float medium = cellularNoise(uv, 8.0);  // Detalles medianos
    float small = cellularNoise(uv, 15.0);  // Rugosidad fina

    // Combinar escalas
    return large * 0.5 + medium * 0.3 + small * 0.2;
}

/**
 * Genera cráteres procedurales
 */
float craters(vec2 uv) {
    // Cellular noise invertido = cráteres
    float cells = cellularNoise(uv, 5.0);

    // Solo mostrar los centros como cráteres profundos
    return smoothstep(0.05, 0.15, cells);
}

/**
 * Variación de color por zona (simula diferentes minerales)
 */
vec3 mineralVariation(vec2 uv, float rockTex) {
    // Usar noise para determinar tipo de roca
    float mineralType = noise(uv * 2.0);

    // Colores base para diferentes minerales
    vec3 ironGrey = hsb2rgb(vec3(0.0, 0.0, 0.4 + rockTex * 0.2));     // Gris hierro
    vec3 rustBrown = hsb2rgb(vec3(0.08, 0.5, 0.3 + rockTex * 0.2));   // Marrón óxido
    vec3 darkRock = hsb2rgb(vec3(0.0, 0.0, 0.2 + rockTex * 0.15));    // Roca oscura

    // Mezclar según tipo de mineral
    vec3 color;
    if (mineralType < 0.33) {
        color = ironGrey;
    } else if (mineralType < 0.66) {
        color = rustBrown;
    } else {
        color = darkRock;
    }

    return color;
}

// ═══════════════════════════════════════════════════════════════
// MAIN
// ═══════════════════════════════════════════════════════════════

void main() {
    // ═══════════════════════════════════════════════════════════
    // 1. CONFIGURACIÓN BÁSICA
    // ═══════════════════════════════════════════════════════════
    vec2 uv = v_TexCoord;
    vec3 normal = normalize(v_Normal);

    // Dirección de la luz (sol)
    vec3 lightDir = normalize(vec3(0.5, 0.3, 1.0));

    // Iluminación difusa (Lambert)
    float diffuse = max(dot(normal, lightDir), 0.0);

    // ═══════════════════════════════════════════════════════════
    // 2. GENERAR TEXTURA ROCOSA
    // ═══════════════════════════════════════════════════════════
    float rockTex = rockTexture(uv);

    // ═══════════════════════════════════════════════════════════
    // 3. GENERAR CRÁTERES
    // ═══════════════════════════════════════════════════════════
    float craterMask = craters(uv);

    // Oscurecer zonas de cráteres
    float craterDepth = 1.0 - craterMask;
    rockTex *= mix(0.5, 1.0, craterMask);  // Cráteres más oscuros

    // ═══════════════════════════════════════════════════════════
    // 4. COLOR BASE CON VARIACIÓN MINERAL
    // ═══════════════════════════════════════════════════════════
    vec3 baseColor = mineralVariation(uv, rockTex);

    // ═══════════════════════════════════════════════════════════
    // 5. ILUMINACIÓN
    // ═══════════════════════════════════════════════════════════

    // Luz ambiente (muy tenue en el espacio)
    float ambient = 0.15;

    // Luz difusa
    vec3 litColor = baseColor * (ambient + diffuse * 0.85);

    // ═══════════════════════════════════════════════════════════
    // 6. BORDE OSCURO (efecto de terminador - zona día/noche)
    // ═══════════════════════════════════════════════════════════
    // En el borde entre luz y sombra, oscurecer aún más
    float terminator = smoothstep(0.0, 0.2, diffuse);
    litColor *= 0.5 + 0.5 * terminator;

    // ═══════════════════════════════════════════════════════════
    // 7. POLVO ESPACIAL (sutil variación en superficie)
    // ═══════════════════════════════════════════════════════════
    float dust = noise(uv * 30.0);
    litColor += vec3(0.05) * dust * 0.1;  // Muy sutil

    // ═══════════════════════════════════════════════════════════
    // 8. SALIDA FINAL
    // ═══════════════════════════════════════════════════════════
    gl_FragColor = vec4(litColor, u_Alpha);
}

/**
 * ════════════════════════════════════════════════════════════
 * 🎨 VARIACIONES POSIBLES
 * ════════════════════════════════════════════════════════════
 *
 * Para diferentes tipos de asteroides:
 *
 * 1. ASTEROIDE METÁLICO (brillante):
 *    - En mineralVariation(), usar más ironGrey
 *    - Agregar especular: pow(dot(reflect(-lightDir, normal), viewDir), 16.0)
 *
 * 2. ASTEROIDE CARBONOSO (muy oscuro):
 *    - Reducir brightness en hsb2rgb de 0.4 a 0.2
 *    - Ambient de 0.15 a 0.05
 *
 * 3. MÁS CRÁTERES:
 *    - En craters(), cambiar escala de 5.0 a 8.0
 *    - Cambiar threshold de 0.05-0.15 a 0.10-0.20
 *
 * 4. SUPERFICIE MÁS LISA:
 *    - En rockTexture(), reducir peso de small noise
 *
 * 5. ASTEROIDE ÍGNEO (rojo/naranja):
 *    - Cambiar hue en rustBrown de 0.08 a 0.03 (más rojo)
 *
 * 6. ANIMACIÓN DE ROTACIÓN:
 *    - Agregar uv += vec2(u_Time * 0.01, 0.0) antes de calcular texturas
 *
 * ════════════════════════════════════════════════════════════
 */
