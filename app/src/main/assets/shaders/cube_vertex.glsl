#version 100
precision mediump float;

// Atributo posición del vértice
attribute vec3 a_Position;
// Uniform para la matriz Modelo-Vista-Proyección
uniform mat4 u_MVP;
// Varying para pasar la posición en espacio modelo al fragment
varying vec3 vPosModel;

void main() {
    vPosModel = a_Position;
    gl_Position = u_MVP * vec4(a_Position, 1.0);
}