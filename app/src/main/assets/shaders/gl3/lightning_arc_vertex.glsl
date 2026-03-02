#version 300 es
precision mediump float;

in vec2 a_Position;
in float a_Alpha;

uniform mat4 u_MVPMatrix;

out float v_Alpha;

void main() {
    v_Alpha = a_Alpha;
    gl_Position = u_MVPMatrix * vec4(a_Position, 0.0, 1.0);
}
