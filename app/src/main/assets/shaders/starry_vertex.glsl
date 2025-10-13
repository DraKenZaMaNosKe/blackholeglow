#ifdef GL_ES
precision mediump float;
#endif

// Vertex Shader para Fondo con Textura + Estrellas
// Renderiza el fondo INFINITAMENTE LEJOS (en el far plane)
// Esto evita que los objetos 3D parezcan atravesar el fondo

attribute vec2 a_Position;
attribute vec2 a_TexCoord;

varying vec2 v_TexCoord;

void main() {
    v_TexCoord = a_TexCoord;

    // TÉCNICA PROFESIONAL DE SKYBOX:
    // Renderizar en Z = 0.9999 (casi en el far plane)
    // Esto hace que el fondo esté INFINITAMENTE lejos
    // Todos los objetos 3D (con 0.0 < Z < 0.9999) se dibujan DELANTE
    gl_Position = vec4(a_Position, 0.9999, 1.0);
}
