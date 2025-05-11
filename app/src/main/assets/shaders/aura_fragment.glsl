precision mediump float;

uniform float u_Time;
uniform vec2 u_Resolution;

// Función pseudoaleatoria
float rand(vec2 co) {
    return fract(sin(dot(co, vec2(12.9898,78.233))) * 43758.5453);
}

void main() {
    vec2 uv = gl_FragCoord.xy / u_Resolution;

    vec2 center = vec2(0.5, 0.5);
    vec2 offset = uv - center;
    float dist = length(offset);
    float angle = atan(offset.y, offset.x);

    // 🌈 Anillo de energía giratorio
    float spiral = 0.5 + 0.5 * sin(16.0 * dist - u_Time * 6.0 + angle * 8.0);
    float ring = smoothstep(0.10, 0.4, dist) * (1.0 - smoothstep(0.10, 0.9, dist));

    vec3 color = vec3(
    0.5 + 0.5 * sin(u_Time + angle),
    0.5 + 0.5 * sin(u_Time + angle + 2.0),
    0.5 + 0.5 * sin(u_Time + angle + 4.0)
    );

    // ✨ Partículas girando como estrellas
    float sparks = 0.0;
    const int NUM_PARTICLES = 20;
    for (int i = 0; i < NUM_PARTICLES; i++) {
        float t = u_Time * 0.7 + float(i) * 0.4;
        vec2 pos = center + 0.3 * vec2(cos(t + float(i)), sin(t + float(i)));
        float d = length(uv - pos);
        // Aumentamos el área visible de la chispa
        sparks += smoothstep(0.03, 0.0, d) * 1.2;
    }

    float glow = (spiral * ring) + sparks;

    gl_FragColor = vec4(color * glow, glow);
}