/**
 * ╔════════════════════════════════════════════════════════════════╗
 * ║  ☁️ CLOUD LAYER - Vertex Shader                                ║
 * ║  Pasa posición y UV para nubes procedurales                    ║
 * ╚════════════════════════════════════════════════════════════════╝
 */

attribute vec4 a_Position;
attribute vec2 a_TexCoord;

uniform mat4 u_MVP;

varying vec2 v_TexCoord;
varying vec3 v_Position;

void main() {
    gl_Position = u_MVP * a_Position;
    v_TexCoord = a_TexCoord;
    v_Position = a_Position.xyz;
}
