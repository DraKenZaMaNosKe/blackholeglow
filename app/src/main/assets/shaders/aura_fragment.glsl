precision mediump float;

uniform float u_Time;
uniform vec2 u_Resolution;

void main() {
    vec2 uv = gl_FragCoord.xy / u_Resolution;

    vec2 center = vec2(0.5, 0.5);
    vec2 offset = uv - center;
    float dist = length(offset);

    // 🔥 Controlamos dónde se verá el fuego (borde exterior)
    float border = smoothstep(0.35, 0.45, dist) * (1.0 - smoothstep(0.45, 0.55, dist));

    // 🔄 Movimiento tipo fuego (ruido ondulante animado)
    float flame = sin(uv.y * 20.0 - u_Time * 10.0 + sin(uv.x * 10.0)) * 0.5 + 0.5;

    // 🌈 Colores cálidos tipo fuego
    vec3 fireColor = vec3(1.0, 0.5, 0.0) * flame +
    vec3(1.0, 0.2, 0.0) * (1.0 - flame);

    // 🔅 Control de intensidad y transparencia
    float intensity = border * flame;
    gl_FragColor = vec4(fireColor * intensity, intensity);
}
