#version 300 es
// ╔═══════════════════════════════════════════════════════════════════╗
// ║   ✨ Starry Background Vertex Shader - GLSL ES 3.0 ✨             ║
// ╚═══════════════════════════════════════════════════════════════════╝
//
// Renderiza el fondo INFINITAMENTE LEJOS (en el far plane)
// usando la sintaxis moderna de OpenGL ES 3.0.

// Inputs
layout(location = 0) in vec2 a_Position;
layout(location = 1) in vec2 a_TexCoord;

// Outputs
out vec2 v_TexCoord;

void main() {
    v_TexCoord = a_TexCoord;

    // TÉCNICA PROFESIONAL DE SKYBOX:
    // Renderizar en Z = 0.9999 (casi en el far plane)
    // Esto hace que el fondo esté INFINITAMENTE lejos
    // Todos los objetos 3D (con 0.0 < Z < 0.9999) se dibujan DELANTE
    gl_Position = vec4(a_Position, 0.9999, 1.0);
}
