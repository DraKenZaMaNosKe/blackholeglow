#ifdef GL_ES
precision mediump float;
#endif

uniform vec2 u_Resolution;
uniform float u_Time;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

// Luciérnagas flotantes
float firefly(vec2 uv, vec2 pos, float seed) {
    float t = u_Time + seed * 6.28;
    vec2 offset = vec2(sin(t * 0.5) * 0.1, cos(t * 0.7) * 0.1);
    float d = length(uv - pos - offset);

    float pulse = 0.5 + 0.5 * sin(t * 4.0);
    return smoothstep(0.015, 0.0, d) * pulse;
}

void main() {
    vec2 uv = gl_FragCoord.xy / u_Resolution.xy;
    vec3 color = vec3(0.0);
    float alpha = 0.0;

    // ========== SOLO DIBUJAR EN LOS BORDES ==========
    float borderWidth = 0.18;
    float borderDist = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));
    float borderMask = smoothstep(borderWidth, borderWidth * 0.5, borderDist);

    // SOLO generar luciérnagas en el borde
    if (borderMask > 0.01) {
        float flies = 0.0;
        for (float i = 0.0; i < 15.0; i++) {
            vec2 pos = vec2(hash(vec2(i, 0.0)), hash(vec2(i, 1.0)));
            flies += firefly(uv, pos, i);
        }

        // Color verde amarillento de luciérnagas
        color = vec3(0.6, 1.0, 0.3) * flies;
        alpha = flies * borderMask * 0.85;
    }

    gl_FragColor = vec4(color, alpha);
}
