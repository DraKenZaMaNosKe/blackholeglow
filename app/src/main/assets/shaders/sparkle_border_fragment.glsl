/*
╔════════════════════════════════════════════════╗
║   🌌 Shader FullScreenNeonFlow v1.0 🌌        ║
║  • Flujo neón dinámico a pantalla completa    ║
║  • Ondas, remolinos y chispeo de ruido        ║
╚════════════════════════════════════════════════╝
*/
precision highp  float;

// Uniforms
uniform float u_Time;        // Tiempo en segundos
uniform vec2  u_Resolution;  // Resolución del viewport (px)

// Colores base neón
const vec3 neonColor1 = vec3(0.8, 0.2, 1.0); // Magenta vibrante
const vec3 neonColor2 = vec3(0.2, 0.8, 1.0); // Cian brillante

// ── Ruido sencillo para chispeo ─────────────────
float rand(vec2 co) {
    return fract(sin(dot(co.xy, vec2(12.9898,78.233))) * 43758.5453);
}

void main() {
    // 1) Coordenadas normalizadas [0,1]
    vec2 uv = gl_FragCoord.xy / u_Resolution.xy;

    // 2) Remapeo a [-1,1], conservando proporción
    vec2 p = uv * 2.0 - 1.0;
    p.x *= u_Resolution.x / u_Resolution.y;

    // 3) Distancia radial y ángulo polar
    float len   = length(p);
    float angle = atan(p.y, p.x);

    // 4) Remolino base (varía con el ángulo y el tiempo)
    float swirl = sin(angle * 4.0 - u_Time * 0.8) * 0.5 + 0.5;

    // 5) Ondas concéntricas (se propagan hacia afuera)
    float waves = sin(len * 10.0 - u_Time * 3.0) * 0.5 + 0.5;

    // 6) Gradiente suave para fundir bordes
    float borderFade = smoothstep(1.0, 0.7, len);

    // 7) Chispeo aleatorio por píxel
    float noise = rand(gl_FragCoord.xy * 0.3 + u_Time * 0.5);

    // 8) Mezcla de colores neón según remolino
    vec3 colorMix = mix(neonColor1, neonColor2, swirl);

    // 9) Composición final de color
    vec3 col = colorMix * waves * borderFade;
    col += noise * 0.15; // destellos de ruido

    // 10) Salida final (alpha = 1.0 para full-screen)
    gl_FragColor = vec4(col, 1.0);
}
