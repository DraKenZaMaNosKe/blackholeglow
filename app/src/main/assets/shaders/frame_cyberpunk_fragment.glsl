#ifdef GL_ES
precision mediump float;
#endif

uniform vec2 u_Resolution;
uniform float u_Time;

void main() {
    vec2 uv = gl_FragCoord.xy / u_Resolution.xy;
    vec3 color = vec3(0.0);
    float alpha = 0.0;

    // Distancia desde el borde
    float borderDist = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));
    float borderWidth = 0.08;
    float border = smoothstep(borderWidth + 0.02, borderWidth, borderDist);

    // Neón corriendo por los bordes
    float angle = atan(uv.y - 0.5, uv.x - 0.5);
    float distFromCenter = length(uv - 0.5);

    // Pulso de neón que corre
    float neonPulse = sin(angle * 2.0 - u_Time * 4.0) * 0.5 + 0.5;
    neonPulse += sin(angle * 4.0 + u_Time * 6.0) * 0.3;

    // Colores cyberpunk: rosa y azul
    vec3 pink = vec3(1.0, 0.0, 0.5);
    vec3 cyan = vec3(0.0, 0.8, 1.0);
    color = mix(pink, cyan, neonPulse);

    alpha = border * (0.6 + 0.4 * neonPulse);

    gl_FragColor = vec4(color, alpha);
}
