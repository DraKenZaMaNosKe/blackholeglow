// Vertex shader para el planeta texturizado

attribute vec4 a_Position;    // posición XYZ
attribute vec2 a_TexCoord;    // coordenadas de textura UV

// Uniforms
uniform mat4 u_MVP;           // matriz Model-View-Proyección

// Varyings (se interpolan para el fragment shader)
varying vec2 v_TexCoord;

void main() {
    // Transforma la posición
    gl_Position = u_MVP * a_Position;
    // Pasa la UV al fragment shader
    v_TexCoord = a_TexCoord;
}
