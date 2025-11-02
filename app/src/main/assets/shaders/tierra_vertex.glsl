/**
 * Vertex shader para Planeta Tierra
 * Pasa normales y posición mundial para iluminación avanzada
 */

attribute vec4 a_Position;
attribute vec2 a_TexCoord;
attribute vec3 a_Normal;  // Opcional si el OBJ tiene normales

uniform mat4 u_MVP;
uniform mat4 u_Model;  // Matriz de modelo (sin proyección)

varying vec2 v_TexCoord;
varying vec3 v_Normal;
varying vec3 v_WorldPos;

void main() {
    // Posición en clip space
    gl_Position = u_MVP * a_Position;

    // Pasar coordenadas UV
    v_TexCoord = a_TexCoord;

    // Transformar normal a espacio mundo
    // Para esfera simple, la normal es igual a la posición normalizada
    v_Normal = normalize(a_Position.xyz);

    // Si tienes u_Model disponible, usar:
    // v_Normal = normalize(mat3(u_Model) * a_Normal);

    // Posición en espacio mundo (para cálculos de vista)
    v_WorldPos = a_Position.xyz;
}
