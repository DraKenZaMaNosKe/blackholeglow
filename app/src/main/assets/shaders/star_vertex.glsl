// Vertex shader para estrellas animadas
uniform mat4 u_MVP;
attribute vec3 a_Position;
varying vec3 v_Position;

void main() {
    gl_Position = u_MVP * vec4(a_Position, 1.0);
    v_Position = a_Position;
}