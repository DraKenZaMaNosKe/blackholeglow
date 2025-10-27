// FireButton Vertex Shader - SIMPLE
attribute vec2 a_Position;
varying vec2 v_Position;

void main() {
    gl_Position = vec4(a_Position, 0.0, 1.0);
    v_Position = a_Position;
}
