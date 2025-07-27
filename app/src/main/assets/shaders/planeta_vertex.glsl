attribute vec4 a_Position;   // posición XYZ del vértice
attribute vec2 a_TexCoord;   // coordenadas UV del vértice

uniform mat4 u_MVP;          // matriz Model-View-Proyección

varying vec2 v_TexCoord;     // pasamos las UV al fragment

void main() {
    gl_Position = u_MVP * a_Position;  // transformamos la posición
    v_TexCoord  = a_TexCoord;          // interpolamos las UV
}
