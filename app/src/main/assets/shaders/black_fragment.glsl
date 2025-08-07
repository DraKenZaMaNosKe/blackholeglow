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

float plot(vec2 st, float pct ){
    return smoothstep(pct-0.02, pct, st.y) - smoothstep(pct, pct+0.02,st.y);
}

void main() {
    vec2 uv = v_TexCoord;
    vec2 st = gl_FragCoord.xy / u_Resolution;

    float y = pow(st.x,2.0);

    vec3 color = vec3(y);

    float pct = plot(st,y);
    color = (1.0-pct)*color*pct*vec3(0.0,1.0,0.0);

    gl_FragColor = vec4(color, 1.0);

}