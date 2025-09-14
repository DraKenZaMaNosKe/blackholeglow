attribute vec4 a_Position;
attribute vec2 a_TexCoord;
uniform mat4 u_MVP;
uniform float u_UvScale;
varying vec2 v_TexCoord;

void main() {
    gl_Position = u_MVP * a_Position;
    v_TexCoord  = a_TexCoord * u_UvScale;
}
