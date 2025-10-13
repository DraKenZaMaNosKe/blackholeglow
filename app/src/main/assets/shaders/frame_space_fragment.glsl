#ifdef GL_ES
precision mediump float;
#endif

uniform vec2 u_Resolution;
uniform float u_Time;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

// Función para dibujar estrellas pulsantes
float star(vec2 uv, vec2 pos, float size) {
    float d = length(uv - pos);
    float pulse = 0.5 + 0.5 * sin(u_Time * 3.0 + hash(pos) * 6.28);
    float brightness = smoothstep(size, 0.0, d) * pulse;
    return brightness;
}

void main() {
    vec2 uv = gl_FragCoord.xy / u_Resolution.xy;
    vec3 color = vec3(0.0);
    float alpha = 0.0;

    // ========== CLAVE: SOLO DIBUJAR EN LOS BORDES ==========
    // Distancia desde cada borde
    float borderWidth = 0.18; // 18% de ancho de marco
    float distLeft = uv.x;
    float distRight = 1.0 - uv.x;
    float distBottom = uv.y;
    float distTop = 1.0 - uv.y;

    // Mínima distancia a cualquier borde
    float borderDist = min(min(distLeft, distRight), min(distBottom, distTop));

    // Máscara de borde: 1.0 en los bordes, 0.0 en el centro
    float borderMask = smoothstep(borderWidth, borderWidth * 0.5, borderDist);

    // SOLO generar efectos si estamos en el borde
    if (borderMask > 0.01) {
        // Generar estrellas en los bordes
        float stars = 0.0;
        for (float i = 0.0; i < 25.0; i++) {
            vec2 starPos = vec2(hash(vec2(i, 0.0)), hash(vec2(i, 1.0)));
            stars += star(uv, starPos, 0.008);
        }

        // Color azul/blanco estelar
        color = vec3(0.8, 0.9, 1.0) * stars;
        alpha = stars * borderMask * 0.9;
    }

    gl_FragColor = vec4(color, alpha);
}
