// ============================================
// archivo: universe_vertex.glsl
// ============================================
attribute vec4 a_Position;
attribute vec2 a_TexCoord;
varying vec2 v_TexCoord;
uniform mat4 u_MVP;

void main() {
    v_TexCoord = a_TexCoord;
    gl_Position = u_MVP * a_Position;
}