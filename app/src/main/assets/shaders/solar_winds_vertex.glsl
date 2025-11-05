// ═══════════════════════════════════════════════════════════════════════════════
// 🌊 SOLAR WINDS - Vertex Shader
// ═══════════════════════════════════════════════════════════════════════════════
// Vertex shader simple para vientos solares
// ═══════════════════════════════════════════════════════════════════════════════

#ifdef GL_ES
precision mediump float;
#endif

// Atributos de entrada
attribute vec3 a_Position;   // Posición del vértice
attribute vec2 a_TexCoord;   // Coordenadas de textura

// Uniforms
uniform mat4 u_MVP;          // Matriz Model-View-Projection

// Varyings (salida al fragment shader)
varying vec2 v_TexCoord;

void main() {
    // Pasar coordenadas de textura al fragment shader
    v_TexCoord = a_TexCoord;

    // Transformar posición con matriz MVP
    gl_Position = u_MVP * vec4(a_Position, 1.0);
}
