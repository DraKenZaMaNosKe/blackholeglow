#ifdef GL_ES
precision mediump float;
#endif

#define PI 3.14159265359

uniform vec2 u_resolution;
uniform float u_time;

float plot(vec2 st, float pct){
    return smoothstep(pct - 0.02, pct, st.y) -
    smoothstep(pct, pct + 0.02, st.y);
}

void main() {
    vec2 st = gl_FragCoord.xy / u_resolution;

    // Mapeo suave entre dos valores según step
    float y = mix(0.3, 0.7, step(0.5, st.x)); // 🔥 Aquí está el fix

    vec3 color = vec3(y); // Fondo gris o blanco

    float pct = plot(st, y); // Línea verde en y = 0.3 o y = 0.7
    color = (1.0 - pct) * color + pct * vec3(0.0, 1.0, 0.0);

    gl_FragColor = vec4(0.0,0.0,1.0, 1.0);
}
