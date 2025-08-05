// Simple vertex shader para un quad que llena todo el NDC.
// Recibe la posición del plano (en [-1,1]) y la proyecta con MVP.
attribute vec4 a_Position;
uniform mat4 u_MVP;

void main() {
    gl_Position = u_MVP * a_Position;
}
