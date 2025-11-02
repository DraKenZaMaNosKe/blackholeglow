/**
 * ╔════════════════════════════════════════════════════════════════╗
 * ║  🌍 PLANETA TIERRA REALISTA - Procedural Earth Shader        ║
 * ║  Océanos + Continentes + Nubes + Atmósfera                   ║
 * ╚════════════════════════════════════════════════════════════════╝
 *
 * Efectos:
 * - Océanos con cellular noise (agua animada)
 * - Continentes con noise para topografía
 * - Nubes procedurales con FBM
 * - Atmósfera brillante en bordes
 * - Especular en océanos (reflexión solar)
 * - Ciclo día/noche con gradiente de sombras
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

// Texturas opcionales (si quieres combinar con texturas reales)
uniform sampler2D u_Texture;
uniform bool u_UseSolidColor;
uniform vec4 u_SolidColor;
uniform float u_Alpha;

// Varyings
varying vec2 v_TexCoord;
varying vec3 v_Normal;      // Normal en espacio mundo
varying vec3 v_WorldPos;    // Posición en espacio mundo

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
// EFECTOS ESPECÍFICOS DE LA TIERRA
// ═══════════════════════════════════════════════════════════════

/**
 * Genera mapa de continentes vs océanos usando noise
 * Return: 0.0 = océano, 1.0 = tierra
 */
float landMask(vec2 uv) {
    // Usar FBM para generar continentes realistas
    vec2 st = uv * 3.0;  // Escala para continentes grandes

    // Múltiples capas de noise para detalle
    float continents = fbm(st, 3);

    // Ajustar threshold para ~30% tierra, 70% agua (como la Tierra real)
    float land = smoothstep(0.45, 0.55, continents);

    return land;
}

/**
 * Genera mapa de elevación/topografía
 */
float elevation(vec2 uv) {
    vec2 st = uv * 8.0;  // Más detalle que continentes
    return fbm(st, 2);
}

/**
 * Genera nubes procedurales animadas
 */
float clouds(vec2 uv, float time) {
    // Movimiento lento de nubes (rotación terrestre)
    vec2 st = uv + vec2(time * 0.02, 0.0);
    st *= 4.0;  // Escala de nubes

    // FBM para nubes realistas
    float cloudPattern = fbm(st, 3);

    // Solo mostrar zonas con suficiente densidad
    return smoothstep(0.5, 0.7, cloudPattern);
}

/**
 * Calcula iluminación especular en océanos (reflexión solar)
 * Simula reflejo del sol en el agua
 */
float oceanSpecular(vec3 normal, vec3 lightDir, vec3 viewDir, float roughness) {
    vec3 halfVector = normalize(lightDir + viewDir);
    float spec = pow(max(dot(normal, halfVector), 0.0), 32.0 / roughness);
    return spec;
}

/**
 * Atmósfera procedural (glow en los bordes)
 */
float atmosphere(vec3 normal, vec3 viewDir) {
    // Fresnel effect - brilla más en los bordes
    float fresnel = pow(1.0 - max(dot(normal, viewDir), 0.0), 3.0);
    return fresnel;
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

    // Dirección de la luz (sol desde arriba-derecha)
    vec3 lightDir = normalize(vec3(0.5, 0.3, 1.0));

    // Dirección de vista (hacia la cámara)
    vec3 viewDir = normalize(-v_WorldPos);

    // Iluminación base (Lambert)
    float diffuse = max(dot(normal, lightDir), 0.0);

    // ═══════════════════════════════════════════════════════════
    // 2. GENERAR MÁSCARAS DE TERRENO
    // ═══════════════════════════════════════════════════════════
    float land = landMask(uv);
    float elev = elevation(uv);
    float cloudMask = clouds(uv, u_Time);

    // ═══════════════════════════════════════════════════════════
    // 3. COLORES BASE (usando HSB para control intuitivo)
    // ═══════════════════════════════════════════════════════════

    // Océanos: azul profundo con cellular noise para olas
    float oceanWaves = cellularNoise(uv * 10.0 + u_Time * 0.05, 5.0);
    vec3 oceanColor = hsb2rgb(vec3(
        0.55 + oceanWaves * 0.02,  // Hue: azul con ligera variación
        0.7,                        // Saturation: agua intensa
        0.4 + oceanWaves * 0.2      // Brightness: variación por olas
    ));

    // Continentes: verde/marrón según elevación
    // Playas (bajo) → verde (medio) → marrón/montañas (alto)
    float landHue = mix(0.12, 0.30, elev);  // De marrón a verde
    vec3 landColor = hsb2rgb(vec3(
        landHue,
        0.6,  // Saturation: tierra natural
        0.4 + elev * 0.2  // Brightness: montañas más claras
    ));

    // Nubes: blanco con sombras suaves
    vec3 cloudColor = vec3(0.95, 0.95, 1.0);  // Blanco azulado

    // ═══════════════════════════════════════════════════════════
    // 4. COMBINAR CAPAS (océano → tierra → nubes)
    // ═══════════════════════════════════════════════════════════
    vec3 surfaceColor = mix(oceanColor, landColor, land);

    // Agregar nubes con transparencia
    surfaceColor = mix(surfaceColor, cloudColor, cloudMask * 0.8);

    // ═══════════════════════════════════════════════════════════
    // 5. ILUMINACIÓN
    // ═══════════════════════════════════════════════════════════

    // Luz difusa
    vec3 litColor = surfaceColor * (0.3 + 0.7 * diffuse);

    // Especular SOLO en océanos (agua refleja el sol)
    if (land < 0.5) {  // Es océano
        float spec = oceanSpecular(normal, lightDir, viewDir, 0.1);
        litColor += vec3(1.0, 0.98, 0.95) * spec * 0.5;  // Reflejo dorado del sol
    }

    // ═══════════════════════════════════════════════════════════
    // 6. ATMÓSFERA (glow azul en los bordes)
    // ═══════════════════════════════════════════════════════════
    float atmo = atmosphere(normal, viewDir);
    vec3 atmosphereColor = hsb2rgb(vec3(0.55, 0.8, 1.0));  // Azul brillante
    litColor += atmosphereColor * atmo * 0.3;

    // ═══════════════════════════════════════════════════════════
    // 7. SOMBRA NOCTURNA (lado oscuro del planeta)
    // ═══════════════════════════════════════════════════════════
    // En el lado nocturno, agregar luces de ciudades (puntos amarillos en tierra)
    if (diffuse < 0.2 && land > 0.5) {  // Noche en tierra
        float cityLights = noise(uv * 20.0) * noise(uv * 40.0);
        cityLights = smoothstep(0.6, 0.7, cityLights);
        vec3 cityColor = vec3(1.0, 0.9, 0.6);  // Amarillo cálido
        litColor += cityColor * cityLights * 0.3;
    }

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
 * Para diferentes efectos, ajusta:
 *
 * 1. MÁS TIERRA (más continentes):
 *    - En landMask(), cambiar threshold de 0.45 a 0.40
 *
 * 2. MÁS NUBES:
 *    - En clouds(), cambiar threshold de 0.5 a 0.4
 *
 * 3. OCÉANOS MÁS CALMADOS:
 *    - Reducir escala de cellularNoise de 10.0 a 5.0
 *
 * 4. ATMÓSFERA MÁS BRILLANTE:
 *    - En atmósfera, cambiar * 0.3 a * 0.5
 *
 * 5. ROTACIÓN MÁS RÁPIDA DE NUBES:
 *    - En clouds(), cambiar time * 0.02 a time * 0.05
 *
 * 6. PLANETA ALIENÍGENA:
 *    - Cambiar oceanColor hue de 0.55 (azul) a 0.33 (verde) o 0.0 (rojo)
 *    - Cambiar landColor hue de 0.12-0.30 a 0.8-0.9 (púrpura)
 *
 * 7. LUCES DE CIUDADES MÁS VISIBLES:
 *    - Cambiar * 0.3 a * 0.8 en cityLights
 *
 * ════════════════════════════════════════════════════════════
 */
