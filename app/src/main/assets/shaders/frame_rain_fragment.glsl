#ifdef GL_ES
precision mediump float;
#endif

uniform vec2 u_Resolution;
uniform float u_Time;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

// Función para dibujar gotas de lluvia
float rainDrop(vec2 uv, float seed) {
    float speed = 0.5 + hash(vec2(seed)) * 0.5;
    float y = fract(uv.y - u_Time * speed);
    float x = uv.x + hash(vec2(seed, 1.0)) * 0.02;

    float drop = smoothstep(0.02, 0.0, abs(x - hash(vec2(seed, 2.0))));
    drop *= smoothstep(0.0, 0.02, y) * smoothstep(0.1, 0.05, y);

    return drop;
}

void main() {
    vec2 uv = gl_FragCoord.xy / u_Resolution.xy;
    vec3 color = vec3(0.0);
    float alpha = 0.0;

    // ========== SOLO DIBUJAR EN LOS BORDES ==========
    float borderWidth = 0.18;
    float borderDist = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));
    float borderMask = smoothstep(borderWidth, borderWidth * 0.6, borderDist);

    // SOLO generar gotas en el borde
    if (borderMask > 0.01) {
        float rain = 0.0;
        for (float i = 0.0; i < 12.0; i++) {
            rain += rainDrop(uv, i);
        }

        // Color azulado de lluvia
        color = vec3(0.3, 0.5, 0.8) * rain;
        alpha = rain * borderMask * 0.7;
    }

    gl_FragColor = vec4(color, alpha);
}
