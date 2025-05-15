precision mediump float;
uniform float u_Time;
uniform vec2 u_Resolution;

void main() {
    // Coordenadas centradas [0,0] centro
    vec2 uv = (gl_FragCoord.xy / u_Resolution) * 2.0 - 1.0;
    float r = length(uv);

    // Borde entre 0.85 y 1.0 (ajusta para hacerlo más o menos delgado)
    float border = smoothstep(0.92, 1.0, r) * (1.0 - smoothstep(1.0, 1.1, r));


    // HSL arcoíris animado
    float angle = atan(uv.y, uv.x) / 6.2831 + 0.5 + u_Time * 0.08;
    float hue = mod(angle, 1.0);

    // Convierte HSL a RGB (fórmula compacta)
    float s = 0.9, l = 0.6;
    float c = (1.0 - abs(2.0 * l - 1.0)) * s;
    float x = c * (1.0 - abs(mod(hue * 6.0, 2.0) - 1.0));
    float m = l - c / 2.0;
    vec3 rgb;
    if      (hue < 1.0/6.0) rgb = vec3(c, x, 0.0);
    else if (hue < 2.0/6.0) rgb = vec3(x, c, 0.0);
    else if (hue < 3.0/6.0) rgb = vec3(0.0, c, x);
    else if (hue < 4.0/6.0) rgb = vec3(0.0, x, c);
    else if (hue < 5.0/6.0) rgb = vec3(x, 0.0, c);
    else                    rgb = vec3(c, 0.0, x);
    rgb += m;

    // Glow con alpha
    float glow = border * (0.6 + 0.4 * sin(u_Time*1.5 + r*15.0));
    gl_FragColor = vec4(rgb * glow * 2.2, glow);
}
