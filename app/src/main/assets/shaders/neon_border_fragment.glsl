precision highp float;

uniform float u_Time;
uniform vec2 u_Resolution;

void main() {
    vec2 fragCoord = gl_FragCoord.xy;
    // reproducimos uv = fragCoord / iResolution - 0.5
    vec2 uv = (fragCoord / u_Resolution) - 0.5;

    // tiempo modulado y desplazamientos
    float t = u_Time * 0.1
    + ((0.25 + 0.05 * sin(u_Time * 0.1))
    / (length(uv) + 0.07)
    ) * 2.2;
    float si = sin(t);
    float co = cos(t);
    mat2 ma = mat2(co, si, -si, co);

    float v1 = 0.0;
    float v2 = 0.0;
    float v3 = 0.0;
    float s  = 0.0;

    // Primer bucle: capas de fractal
    for (int i = 0; i < 90; i++) {
        // p en 3D con z en s
        vec3 p = s * vec3(uv, 0.0);
        // rotación
        p.xy *= ma;
        // desplazamiento
        p += vec3(0.22, 0.3, s - 1.5 - sin(u_Time * 0.13) * 0.1);

        // bucle interior: folding fractal
        for (int j = 0; j < 8; j++) {
            p = abs(p) / dot(p, p) - 0.659;
        }

        float dp2 = dot(p,p);
        v1 += dp2 * 0.0015 * (1.8 + sin(length(uv * 13.0) + 0.5 - u_Time * 0.2));
        v2 += dp2 * 0.0013 * (1.5 + sin(length(uv * 14.5) + 1.2 - u_Time * 0.3));
        v3 += length(p.xy * 10.0) * 0.0003;
        s  += 0.035;
    }

    // atenuaciones radiales
    float len = length(uv);
    v1 *= smoothstep(0.7, 0.0, len);
    v2 *= smoothstep(0.5, 0.0, len);
    v3 *= smoothstep(0.9, 0.0, len);

    // mezcla de canales
    vec3 col = vec3(
    v3 * (1.5 + sin(u_Time * 0.2) * 0.4),
    (v1 + v3) * 0.3,
    v2
    )
    + smoothstep(0.2, 0.0, len) * 0.85
    + smoothstep(0.0, 0.6, v3) * 0.3;

    // corrección de rango
    col = min(pow(abs(col), vec3(1.2)), vec3(1.0));

    gl_FragColor = vec4(col, 1.0);
}
