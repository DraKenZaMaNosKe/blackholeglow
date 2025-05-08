precision mediump float;

uniform float u_Time;
uniform vec2 u_Resolution;

float noise(vec2 p) {
    return fract(sin(dot(p ,vec2(12.9898,78.233))) * 43758.5453);
}

void main() {
    vec2 uv = gl_FragCoord.xy / u_Resolution;
    vec2 center = vec2(0.5, 0.5);
    float dist = distance(uv, center);
    
    float edge = 0.45 + 0.02 * sin(u_Time * 2.0 + dist * 30.0);
    float intensity = smoothstep(0.005, 0.0, abs(dist - edge));

    float glow = intensity + 0.1 * noise(uv * 50.0 + u_Time);
    
    vec3 color = vec3(0.0, 1.0, 1.0) * glow; // azul neón

    gl_FragColor = vec4(color, glow);
}
