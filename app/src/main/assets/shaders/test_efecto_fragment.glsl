#ifdef GL_ES
precision mediump float;
#endif

#define PI 3.14159265359

uniform vec2 u_Resolution;
uniform vec2 u_mouse;
uniform float u_Time;

float plot(vec2 st, float pct){
    return  smoothstep( pct-0.2, pct, st.y) -
    smoothstep( pct, pct+0.02, st.y);
}

void main() {
    vec2 st = gl_FragCoord.xy/u_Resolution;

    float t = mod(u_Time, 100.0); // Limita el tiempo a 100 segundos

    // Smooth interpolation between 0.1 and 0.9
    float y = smoothstep(0.2,0.4,st.x) - smoothstep(0.5,0.8,st.x);

    y += 0.1 * sin(2.0 * (st.x + t)); // Suma el tiempo a x

    vec3 color = vec3(y);

    float pct = plot(st,y);
    color = (1.0-pct)*color+pct*vec3(0.0,1.0,0.0);

    gl_FragColor = vec4(color,1.0);
}