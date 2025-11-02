/**
 * Vertex shader para Ocean Deep wallpaper.
 * Shader simple que solo pasa las coordenadas.
 */

attribute vec4 a_Position;

void main() {
    gl_Position = a_Position;
}
