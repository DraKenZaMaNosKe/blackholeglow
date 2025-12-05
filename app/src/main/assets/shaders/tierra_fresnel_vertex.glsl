/**
 * ╔════════════════════════════════════════════════════════════════╗
 * ║  🌍 TIERRA FRESNEL - Vertex Shader                             ║
 * ║  Pasa normal y posición para efecto de atmósfera               ║
 * ╚════════════════════════════════════════════════════════════════╝
 */

attribute vec4 a_Position;
attribute vec2 a_TexCoord;

uniform mat4 u_MVP;

varying vec2 v_TexCoord;
varying vec3 v_Normal;
varying vec3 v_Position;

void main() {
    gl_Position = u_MVP * a_Position;
    v_TexCoord = a_TexCoord;

    // Para una esfera centrada en origen, la normal = posición normalizada
    v_Normal = normalize(a_Position.xyz);
    v_Position = a_Position.xyz;
}
