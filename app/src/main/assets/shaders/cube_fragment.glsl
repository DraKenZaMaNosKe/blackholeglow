#version 100
precision mediump float;

uniform vec4 u_Color;

void main() {
    // Color uniforme
    gl_FragColor = u_Color;
}
