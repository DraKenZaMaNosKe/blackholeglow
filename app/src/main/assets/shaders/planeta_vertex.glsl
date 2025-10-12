// ============================================
// archivo: planeta_vertex.glsl
// ============================================
#ifdef GL_ES
precision mediump float;
#endif

attribute vec4 a_Position;
attribute vec2 a_TexCoord;
uniform mat4 u_MVP;
uniform float u_Time;
varying vec2 v_TexCoord;
varying vec3 v_WorldPos;

void main() {
    gl_Position = u_MVP * a_Position;
    v_TexCoord = a_TexCoord;
    v_WorldPos = a_Position.xyz;
}