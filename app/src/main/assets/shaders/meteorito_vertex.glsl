// Vertex shader simple y funcional para meteoritos
uniform mat4 u_MVP;

attribute vec3 a_Position;
attribute vec2 a_TexCoord;

varying vec2 v_TexCoord;
varying vec3 v_Position;

void main() {
    // Posición sin modificaciones complejas
    gl_Position = u_MVP * vec4(a_Position, 1.0);

    // Pasar datos al fragment shader
    v_TexCoord = a_TexCoord;
    v_Position = a_Position;
}