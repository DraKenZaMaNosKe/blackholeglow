#ifdef GL_ES
precision mediump float;
#endif

// Vertex Shader - Campo de Fuerza

attribute vec3 a_Position;
attribute vec2 a_TexCoord;

uniform mat4 u_MVP;

varying vec2 v_TexCoord;
varying vec3 v_WorldPos;

void main() {
    v_TexCoord = a_TexCoord;
    v_WorldPos = a_Position;  // Posición en espacio local (para detección de impactos)

    gl_Position = u_MVP * vec4(a_Position, 1.0);
}
