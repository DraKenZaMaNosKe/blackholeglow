// earth_shield_vertex.glsl
// Vertex Shader para EarthShield (Escudo de la Tierra)

attribute vec4 a_Position;
attribute vec2 a_TexCoord;

uniform mat4 u_MVP;

varying vec2 v_TexCoord;
varying vec3 v_WorldPos;

void main() {
    v_TexCoord = a_TexCoord;
    v_WorldPos = a_Position.xyz;
    gl_Position = u_MVP * a_Position;
}
