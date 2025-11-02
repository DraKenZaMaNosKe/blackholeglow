/**
 * ╔════════════════════════════════════════════════════════════════╗
 * ║  🎨 DEMO - Black Hole Glow Shader Library                   ║
 * ║  Demostración de todas las funciones disponibles            ║
 * ╚════════════════════════════════════════════════════════════════╝
 *
 * Este shader demuestra el uso de la librería completa.
 * Puedes cambiar entre diferentes efectos modificando DEMO_MODE.
 */

#ifdef GL_ES
precision mediump float;
#endif

uniform vec2 u_Resolution;
uniform float u_Time;

// ═══════════════════════════════════════════════════════════════
// CAMBIAR ESTE VALOR PARA VER DIFERENTES DEMOS (1-8)
// ═══════════════════════════════════════════════════════════════
#define DEMO_MODE 1

// ═══════════════════════════════════════════════════════════════
// COPIAR FUNCIONES DE LA LIBRERÍA
// (En producción, usar sistema de includes o preprocesador)
// ═══════════════════════════════════════════════════════════════

// ─── CORE.GLSL ────
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

// ─── COLOR.GLSL ────
vec3 hsb2rgb(vec3 c) {
    vec3 rgb = clamp(abs(mod(c.x * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0, 0.0, 1.0);
    rgb = rgb * rgb * (3.0 - 2.0 * rgb);
    return c.z * mix(vec3(1.0), rgb, c.y);
}

vec3 palette(float t, vec3 a, vec3 b, vec3 c, vec3 d) {
    return a + b * cos(6.28318 * (c * t + d));
}

// ─── SHAPES.GLSL ────
#define PI 3.14159265359
#define TWO_PI 6.28318530718

float sdCircle(vec2 st, float radius) {
    return length(st) - radius;
}

mat2 rotate2d(float angle) {
    return mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
}

float sdPolygon(vec2 st, int N) {
    float a = atan(st.y, st.x) + PI;
    float r = TWO_PI / float(N);
    float d = cos(floor(0.5 + a / r) * r - a) * length(st);
    return d - 0.5;
}

// ─── EFFECTS.GLSL ────
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

vec2 cellularNoise2(vec2 st, float scale) {
    st *= scale;
    vec2 i_st = floor(st);
    vec2 f_st = fract(st);
    float min_dist1 = 1.0;
    float min_dist2 = 1.0;

    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 neighbor = vec2(float(x), float(y));
            vec2 point = random(i_st + neighbor) * vec2(1.0);
            vec2 diff = neighbor + point - f_st;
            float dist = length(diff);

            if (dist < min_dist1) {
                min_dist2 = min_dist1;
                min_dist1 = dist;
            } else if (dist < min_dist2) {
                min_dist2 = dist;
            }
        }
    }

    return vec2(min_dist1, min_dist2);
}

// ═══════════════════════════════════════════════════════════════
// DEMOS
// ═══════════════════════════════════════════════════════════════

void main() {
    vec2 st = gl_FragCoord.xy / u_Resolution.xy;
    vec2 centered = st - 0.5;
    centered.x *= u_Resolution.x / u_Resolution.y;

    vec3 color = vec3(0.0);

    // ═══════════════════════════════════════════════════════════
    // DEMO 1: ARCO ÍRIS ANIMADO (HSB)
    // ═══════════════════════════════════════════════════════════
    if (DEMO_MODE == 1) {
        float hue = st.x + u_Time * 0.1;
        color = hsb2rgb(vec3(hue, 0.8, 0.9));
    }

    // ═══════════════════════════════════════════════════════════
    // DEMO 2: CELLULAR NOISE ORGÁNICO (Burbujas/Agua)
    // ═══════════════════════════════════════════════════════════
    else if (DEMO_MODE == 2) {
        float cells = cellularNoise(st * 3.0 + u_Time * 0.1, 5.0);
        // Color oceánico usando HSB
        color = hsb2rgb(vec3(0.55 + cells * 0.1, 0.7, 0.8 + cells * 0.2));
    }

    // ═══════════════════════════════════════════════════════════
    // DEMO 3: BORDES DE CÉLULAS (Estilo Comic)
    // ═══════════════════════════════════════════════════════════
    else if (DEMO_MODE == 3) {
        vec2 cells2 = cellularNoise2(st, 8.0);
        float borders = cells2.y - cells2.x;
        float outline = smoothstep(0.0, 0.05, borders);

        // Color neón
        vec3 neonColor = hsb2rgb(vec3(0.5 + u_Time * 0.1, 0.9, 1.0));
        color = neonColor * (1.0 - outline);
    }

    // ═══════════════════════════════════════════════════════════
    // DEMO 4: PALETA PROCEDURAL DE INIGO QUILEZ
    // ═══════════════════════════════════════════════════════════
    else if (DEMO_MODE == 4) {
        float t = length(centered) + u_Time * 0.2;
        color = palette(t,
            vec3(0.5, 0.5, 0.5),
            vec3(0.5, 0.5, 0.5),
            vec3(1.0, 1.0, 1.0),
            vec3(0.0, 0.33, 0.67)
        );
    }

    // ═══════════════════════════════════════════════════════════
    // DEMO 5: FORMAS GEOMÉTRICAS ROTANTES (Distance Fields)
    // ═══════════════════════════════════════════════════════════
    else if (DEMO_MODE == 5) {
        vec2 rotated = rotate2d(u_Time) * centered;

        // Hexágono
        float hex = sdPolygon(rotated, 6);
        float shape = smoothstep(0.01, 0.0, hex);

        color = hsb2rgb(vec3(u_Time * 0.1, 0.8, shape));
    }

    // ═══════════════════════════════════════════════════════════
    // DEMO 6: RUIDO FRACTAL (FBM-like con noise)
    // ═══════════════════════════════════════════════════════════
    else if (DEMO_MODE == 6) {
        float n = 0.0;
        float amplitude = 0.5;
        vec2 freq = st * 3.0;

        for (int i = 0; i < 4; i++) {
            n += amplitude * noise(freq + u_Time * 0.1);
            freq *= 2.0;
            amplitude *= 0.5;
        }

        color = hsb2rgb(vec3(n, 0.6, 0.9));
    }

    // ═══════════════════════════════════════════════════════════
    // DEMO 7: CÍRCULOS CONCÉNTRICOS CON COLOR
    // ═══════════════════════════════════════════════════════════
    else if (DEMO_MODE == 7) {
        float r = length(centered);
        float rings = sin(r * 20.0 - u_Time * 2.0) * 0.5 + 0.5;

        color = hsb2rgb(vec3(r + u_Time * 0.1, 0.8, rings));
    }

    // ═══════════════════════════════════════════════════════════
    // DEMO 8: COMBINACIÓN ÉPICA (Cellular + HSB + Shapes)
    // ═══════════════════════════════════════════════════════════
    else if (DEMO_MODE == 8) {
        // Fondo con cellular noise
        float cells = cellularNoise(st * 2.0 + u_Time * 0.05, 5.0);
        vec3 bg = hsb2rgb(vec3(0.6 + cells * 0.2, 0.6, 0.3));

        // Círculo central brillante
        float circle = sdCircle(centered, 0.2);
        float circleMask = smoothstep(0.01, 0.0, circle);

        // Color del círculo
        vec3 circleColor = hsb2rgb(vec3(u_Time * 0.1, 0.9, 1.0));

        // Combinar
        color = mix(bg, circleColor, circleMask);
    }

    gl_FragColor = vec4(color, 1.0);
}
