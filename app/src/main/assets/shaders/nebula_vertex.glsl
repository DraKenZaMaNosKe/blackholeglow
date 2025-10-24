// ═══════════════════════════════════════════════════════════════════════
// ☁️ NEBULA VERTEX SHADER - Para nubes de gas cósmico
// ═══════════════════════════════════════════════════════════════════════

attribute vec4 a_Position;
attribute vec2 a_TexCoord;

uniform mat4 u_MVP;

varying vec2 v_TexCoord;
varying vec3 v_Position;

void main() {
    v_TexCoord = a_TexCoord;
    v_Position = a_Position.xyz;
    gl_Position = u_MVP * a_Position;
}
