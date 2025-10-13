#ifdef GL_ES
precision mediump float;
#endif

uniform vec2 u_Resolution;
uniform float u_Time;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

// Rayo eléctrico
float lightning(vec2 uv, float seed) {
    float t = u_Time * 2.0 + seed;
    float flash = step(0.95, fract(t * 0.5));

    float x = uv.x + sin(uv.y * 10.0 + t) * 0.02;
    float bolt = smoothstep(0.01, 0.0, abs(x - hash(vec2(seed))));

    return bolt * flash;
}

void main() {
    vec2 uv = gl_FragCoord.xy / u_Resolution.xy;
    vec3 color = vec3(0.0);
    float alpha = 0.0;

    // ========== SOLO DIBUJAR EN LOS BORDES ==========
    float borderWidth = 0.18;
    float borderDist = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));
    float borderMask = smoothstep(borderWidth, borderWidth * 0.5, borderDist);

    // SOLO generar rayos en el borde
    if (borderMask > 0.01) {
        float bolts = 0.0;
        for (float i = 0.0; i < 8.0; i++) {
            bolts += lightning(uv, i);
        }

        // Color eléctrico amarillo/blanco
        color = vec3(1.0, 0.9, 0.3) * bolts;
        alpha = bolts * borderMask * 0.85;
    }

    gl_FragColor = vec4(color, alpha);
}
