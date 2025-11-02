/**
 * Vertex shader para Sol con Plasma
 * Pasa normales y posición mundial para efectos de corona
 */

attribute vec4 a_Position;
attribute vec2 a_TexCoord;

uniform mat4 u_MVP;

varying vec2 v_TexCoord;
varying vec3 v_Normal;
varying vec3 v_WorldPos;

void main() {
    // Posición en clip space
    gl_Position = u_MVP * a_Position;

    // Pasar coordenadas UV
    v_TexCoord = a_TexCoord;

    // Para esfera, la normal es la posición normalizada
    v_Normal = normalize(a_Position.xyz);

    // Posición en espacio mundo
    v_WorldPos = a_Position.xyz;
}
