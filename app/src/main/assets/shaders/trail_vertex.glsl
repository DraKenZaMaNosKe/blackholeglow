// Vertex shader para estelas de meteoritos con efectos dinámicos
uniform mat4 u_MVP;
uniform float u_Time;
uniform float u_TrailLength;

attribute vec3 a_Position;
attribute vec4 a_Color;
attribute float a_Age;  // Edad del segmento de estela

varying vec4 v_Color;
varying float v_Age;
varying vec2 v_UV;

void main() {
    // Aplicar ondulación a la estela
    vec3 pos = a_Position;

    // Ondulación basada en la edad del segmento
    float wave = sin(u_Time * 5.0 + a_Age * 3.14) * 0.02;
    pos.x += wave * (1.0 - a_Age);  // Más ondulación en la cola
    pos.y += wave * 0.5 * (1.0 - a_Age);

    // Efecto de dispersión en la cola
    float dispersion = a_Age * a_Age * 0.1;
    pos.xy *= 1.0 + dispersion;

    gl_Position = u_MVP * vec4(pos, 1.0);

    // Pasar datos al fragment shader
    v_Color = a_Color;
    v_Age = a_Age;

    // Calcular UV para efectos en el fragment shader
    v_UV = vec2(a_Age, 0.5);
}