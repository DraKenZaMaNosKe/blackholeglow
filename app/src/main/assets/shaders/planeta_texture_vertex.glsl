// Vertex shader para texturizar planeta con tiling (escala UV)
attribute vec4 a_Position;    // posición XYZ del vértice
attribute vec2 a_TexCoord;    // coordenadas UV
uniform mat4 u_MVP;           // matriz Model-View-Proyección
uniform float u_UvScale;      // factor de escala de las UV
varying vec2 v_TexCoord;      // pasamos UV escaladas al fragment shader

void main() {
    // Transformación de vértice
    gl_Position = u_MVP * a_Position;
    // Escalamos las UV para repetir la textura
    v_TexCoord  = a_TexCoord * u_UvScale;
}
