// ============================================
// avatar_vertex.glsl
// Vertex shader simple para AvatarSphere
// Sin efectos, solo transformación MVP
// ============================================

attribute vec4 a_Position;
attribute vec2 a_TexCoord;

uniform mat4 u_MVP;

varying vec2 v_TexCoord;

void main() {
    // Solo transformación MVP, sin efectos
    gl_Position = u_MVP * a_Position;

    // Pasar UVs al fragment shader
    v_TexCoord = a_TexCoord;
}
