// black_fragment.glsl
#ifdef GL_ES
precision mediump float;
#endif

// Recibimos UV aunque no las usemos
varying vec2 v_TexCoord;

// Declaramos sampler y uniforms para mantener compatibilidad
uniform sampler2D u_Texture;
uniform float     u_Alpha;
uniform float     u_Time;
uniform vec2      u_Resolution;

void main() {
    vec2 uv = v_TexCoord;
    vec2 st = gl_FragCoord.xy / u_Resolution;

    gl_FragColor = vec4(0.0,0.0,0.7, u_Alpha);

}