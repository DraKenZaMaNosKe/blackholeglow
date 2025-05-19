precision mediump float;

uniform float u_Time;
uniform vec2 u_Resolution;

const vec3 neonColor1 = vec3(0.8, 0.2, 1.0);
const vec3 neonColor2 = vec3(0.2, 0.8, 1.0);

void main() {
    vec2 uv = gl_FragCoord.xy / u_Resolution.xy;
    float borderThickness = 0.01;
    float border = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));

    float glow = smoothstep(borderThickness, borderThickness + 0.025, border);
    float angle = atan(uv.y - 0.5, uv.x - 0.5);
    float lightAnim = 0.5 + 0.5 * sin(u_Time * 2.7 + angle * 6.0);
    float edgeLight = smoothstep(borderThickness - 0.01, borderThickness + 0.01, border) * lightAnim;

    vec3 neonGlow = mix(neonColor1, neonColor2, uv.x + 0.2 * sin(u_Time + uv.y * 6.0));
    vec3 color = neonGlow * (glow * 1.1 + edgeLight * 2.5);

    float alpha = clamp(glow + edgeLight, 0.0, 1.0);
    if (alpha < 0.01) discard;
    gl_FragColor = vec4(color, alpha);
}
