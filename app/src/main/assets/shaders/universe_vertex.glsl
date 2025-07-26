// vertex shader para UniverseBackground
// 1) Recibe posición y coordenadas UV
attribute vec4 a_Position;
attribute vec2 a_TexCoord;

// 2) Pasa las UV al fragment shader
varying vec2 v_TexCoord;

// 3) Matriz de modelo-vista-proyección
uniform mat4 u_MVP;

void main() {
    // Guarda la coordenada de textura para el fragment
    v_TexCoord = a_TexCoord;
    // Calcula la posición final del vértice
    gl_Position = u_MVP * a_Position;
}
