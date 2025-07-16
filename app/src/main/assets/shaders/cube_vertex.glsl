#version 100
precision mediump float;

attribute vec3 a_Position;
uniform mat4 u_MVP;

void main() {
    // Transformación MVP
    gl_Position = u_MVP * vec4(a_Position, 1.0);
}
