// black_fragment.glsl
#ifdef GL_ES
precision mediump float;
#endif

#define PI 3.14159265359

// Recibimos UV aunque no las usemos
varying vec2 v_TexCoord;

// Declaramos sampler y uniforms para mantener compatibilidad
uniform sampler2D u_Texture;
uniform float     u_Alpha;
uniform float     u_Time;
uniform vec2      u_Resolution;
vec3 colorA = vec3(0.149,0.141,0.912);
vec3 colorB = vec3(1.000,0.833,0.224);
void main() {
    vec3 color = vec3(0.0);
    float pct = abs(sin(u_Time));
    // Mix uses pct (a value from 0-1) to
    // mix the two colors
    color = mix(colorA, colorB, pct);
    gl_FragColor = vec4(color,1.0);
}

