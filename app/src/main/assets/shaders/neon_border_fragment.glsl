precision mediump float;

uniform float u_Time;
uniform vec2 u_Resolution;

// Colores de neón (puedes personalizar)
const vec3 neonColor1 = vec3(0.8, 0.2, 1.0); // Rosa neón
const vec3 neonColor2 = vec3(0.2, 0.8, 1.0); // Azul neón

void main() {
    // Coordenadas normalizadas (0 a 1)
    vec2 uv = gl_FragCoord.xy / u_Resolution.xy;

    // Calcula distancia mínima al borde
    float borderThickness = 0.01; // el valor que te gustó
    float border = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));

    // Glow en el borde
    float glow = smoothstep(borderThickness, borderThickness + 0.025, border);

    // Luz animada recorriendo el borde
    float angle = atan(uv.y - 0.5, uv.x - 0.5);
    float lightAnim = 0.5 + 0.5 * sin(u_Time * 2.7 + angle * 6.0);
    float edgeLight = smoothstep(borderThickness - 0.01, borderThickness + 0.01, border) * lightAnim;

    // Color neón animado
    vec3 neonGlow = mix(neonColor1, neonColor2, uv.x + 0.2 * sin(u_Time + uv.y * 6.0));
    vec3 color = neonGlow * (glow * 1.1 + edgeLight * 2.5);

    // Alpha solo donde hay borde (para el resto, 0)
    float alpha = clamp(glow + edgeLight, 0.0, 1.0);

    // Haz fondo totalmente transparente si alpha < 0.01
    if (alpha < 0.01) {
        discard; // ¡No pintes el pixel!
    }

    gl_FragColor = vec4(color, alpha);
}
