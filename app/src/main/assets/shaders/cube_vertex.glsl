// cube_vertex.glsl
attribute vec3 a_Position;

uniform mat4 u_MVP;
uniform float u_Time;
uniform vec2 u_Resolution;

varying float v_Time;
varying vec3  v_Position;

void main() {
    v_Time     = u_Time;
    v_Position = a_Position;
    gl_Position = u_MVP * vec4(a_Position, 1.0);
}