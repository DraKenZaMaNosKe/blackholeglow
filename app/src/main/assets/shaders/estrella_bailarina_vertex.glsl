// Vertex Shader para EstrelaBailarina
// Estrella reactiva a música con efectos espectaculares

attribute vec3 a_Position;
attribute vec2 a_TexCoord;

uniform mat4 u_MVP;
uniform float u_Time;

varying vec2 v_TexCoord;
varying vec3 v_WorldPos;

void main() {
    v_TexCoord = a_TexCoord;
    v_WorldPos = a_Position;

    gl_Position = u_MVP * vec4(a_Position, 1.0);
}
