// Vertex Shader para Partículas Explosivas
// Simple y optimizado - solo MVP

attribute vec4 a_Position;

uniform mat4 u_MVP;

void main() {
    gl_Position = u_MVP * a_Position;
}
