/*
╔════════════════════════════════════════════════════════════════╗
║   🌈 Shader DualPulsingRingsLiquidColor3D v1.2 🌊 🌈            ║
║  • Dos anillos huecos “líquidos” con estela arcoíris          ║
║  • Colores con sombreado 3D (iluminación difusa + especular)  ║
║  • Fondo transparente, solo anillos y estelas                 ║
╚════════════════════════════════════════════════════════════════╝
*/
precision mediump float;

uniform float u_Time;        // segundos transcurridos
uniform vec2  u_Resolution;  // resolución del viewport (px)

// ── Parámetros ────────────────────────────────────────────────
const float BASE_RADIUS   = 0.05;
const float SPEED         = 0.2;
const float THICKNESS     = 0.015;
const float EDGE_FADE     = 0.005;
const int   TRAIL_STEPS   = 7;
const float TRAIL_OFFSET  = 0.05;
const float TRAIL_DECAY   = 0.6;
const float SECOND_SHIFT  = 0.5;

// ── Conversión HSV → RGB ──────────────────────────────────────
vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0,2.0/3.0,1.0/3.0,3.0);
    vec3 p = abs(fract(c.xxx + K.xyz)*6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx,0.0,1.0), c.y);
}

// ── Ruido simple 2D ───────────────────────────────────────────
float hash(float x) {
    return fract(sin(x*12.9898 + 78.233) * 43758.5453);
}
float noise(in vec2 p) {
    float n = hash(p.x + p.y*57.0);
    return fract(n + hash(n));
}

void main() {
    vec2 uv     = gl_FragCoord.xy / u_Resolution;
    vec2 center = vec2(0.5);
    float dist  = distance(uv, center);
    float maxRad = length(vec2(0.5));

    // dirección de la luz para sombreado 3D
    vec3 lightDir = normalize(vec3(-0.5, 0.5, 1.0));
    vec3 viewDir  = vec3(0.0, 0.0, 1.0);

    vec3  accumColor = vec3(0.0);
    float accumAlpha = 0.0;

    for(int i = 0; i < TRAIL_STEPS; i++) {
        float fi = float(i);
        float t0 = u_Time - fi * TRAIL_OFFSET;

        // Sawtooth cycles para ambos anillos
        float cycle1 = fract(t0 * SPEED);
        float cycle2 = fract(cycle1 + SECOND_SHIFT);

        // Radios animados
        float r1 = mix(BASE_RADIUS, maxRad, cycle1);
        float r2 = mix(BASE_RADIUS, maxRad, cycle2);

        // Perfiles huecos
        float o1 = smoothstep(r1+EDGE_FADE, r1, dist);
        float i1 = smoothstep(r1-THICKNESS, r1-THICKNESS-EDGE_FADE, dist);
        float ring1 = o1 * (1.0 - i1);

        float o2 = smoothstep(r2+EDGE_FADE, r2, dist);
        float i2 = smoothstep(r2-THICKNESS, r2-THICKNESS-EDGE_FADE, dist);
        float ring2 = o2 * (1.0 - i2);

        float weight = pow(TRAIL_DECAY, fi);

        // Distorsión “líquida”
        vec2 uvDist = uv + 0.02 * (noise(uv*3.0 + u_Time*0.5 + fi) - 0.5);

        // Generación de matices
        float hue1 = fract(uvDist.x*0.3 + cycle1*0.5 + u_Time*0.1 + fi/float(TRAIL_STEPS));
        float hue2 = fract(uvDist.y*0.3 + cycle2*0.5 + u_Time*0.1 + fi/float(TRAIL_STEPS));

        vec3 col1 = hsv2rgb(vec3(hue1, 0.8, 1.0));
        vec3 col2 = hsv2rgb(vec3(hue2, 0.8, 1.0));

        // Cálculo de normales para cada fragmento del anillo
        vec2 rd = normalize(uv - center);
        vec3 normal = vec3(rd, 0.0);

        // Iluminación difusa
        float diff1 = max(dot(normal, lightDir), 0.1);
        float diff2 = diff1; // mismo lighting para segundo anillo

        // Especular (brillo de punto)
        vec3 reflectDir = reflect(-lightDir, normal);
        float spec1 = pow(max(dot(viewDir, reflectDir), 0.0), 16.0) * 0.3;
        float spec2 = spec1;

        // Colores sombreados 3D
        vec3 shaded1 = col1 * diff1 + spec1;
        vec3 shaded2 = col2 * diff2 + spec2;

        // Acumulamos primer anillo
        accumColor += shaded1 * ring1 * weight;
        accumAlpha += ring1 * weight;

        // Acumulamos segundo anillo (más tenue)
        accumColor += shaded2 * ring2 * (weight * 0.7);
        accumAlpha += ring2 * (weight * 0.7);
    }

    if (accumAlpha < 0.01) discard;

    vec3  finalColor = accumColor / accumAlpha;
    float finalAlpha = clamp(accumAlpha, 0.0, 1.0);

    gl_FragColor = vec4(finalColor, finalAlpha);
}
