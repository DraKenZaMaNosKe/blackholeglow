#ifdef GL_ES
precision mediump float;
#endif

// Vertex Shader para Fondo con Textura + Estrellas
// Quad fullscreen en espacio NDC (-1 a 1)

attribute vec2 a_Position;
attribute vec2 a_TexCoord;

varying vec2 v_TexCoord;

void main() {
    v_TexCoord = a_TexCoord;
    gl_Position = vec4(a_Position, 0.0, 1.0);
}
