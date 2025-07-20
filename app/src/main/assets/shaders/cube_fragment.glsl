// cube_fragment.glsl
precision mediump float;

uniform vec4  u_Color;
uniform float u_Time;
uniform vec2  u_Resolution;

varying float v_Time;
varying vec3  v_Position;

void main() {
    vec2 uv = gl_FragCoord.xy / u_Resolution;
    vec2 p = vec2(0.5) - uv;
    float a = atan(p.x,p.y);

    float r = length(p);

    float e = 1.0 - smoothstep(0.2,0.2,r);
    vec3 fin = vec3(e);

    gl_FragColor = vec4(fin, 1.0);
}