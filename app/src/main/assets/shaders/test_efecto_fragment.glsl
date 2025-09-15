#ifdef GL_ES
precision mediump float;
#endif

uniform vec2  u_Resolution;
uniform float u_Time;

#define PI 3.14159265359
#define TWO_PI 6.28318530718

void main() {
    // 1) Coordenadas normalizadas y centradas
    vec2 st = gl_FragCoord.xy / u_Resolution;


    gl_FragColor = vec4(0.0,0.0,1.0, 0.2);
}
