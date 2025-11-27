#version 300 es
// ╔═══════════════════════════════════════════════════════════════════╗
// ║   🚀 Particle Instanced Vertex Shader - GLSL ES 3.0 🚀           ║
// ╚═══════════════════════════════════════════════════════════════════╝
//
// Renderiza miles de partículas con UNA sola draw call usando instancing.
// Cada instancia tiene: posición, color, y tamaño únicos.

// Atributos de vértice (por vértice del quad)
layout(location = 0) in vec3 a_Position;    // Posición del vértice del quad
layout(location = 1) in vec2 a_TexCoord;    // UV del quad

// Atributos de instancia (por partícula)
layout(location = 2) in vec3 a_InstancePos;   // Posición de la partícula en mundo
layout(location = 3) in vec4 a_InstanceColor; // Color RGBA de la partícula
layout(location = 4) in float a_InstanceSize; // Tamaño de la partícula

// Uniforms
uniform mat4 u_VP;           // View-Projection matrix
uniform float u_Time;        // Tiempo para animación

// Salidas al fragment shader
out vec2 v_TexCoord;
out vec4 v_Color;
out float v_DistanceFromCenter;

void main() {
    // Escalar el quad según el tamaño de la partícula
    vec3 scaledPos = a_Position * a_InstanceSize;

    // Billboarding simple: el quad siempre mira a la cámara
    // En un sistema real, usaríamos la matriz de vista inversa
    // Aquí simplificamos manteniendo el quad en el plano XY
    vec3 worldPos = scaledPos + a_InstancePos;

    // Aplicar View-Projection
    gl_Position = u_VP * vec4(worldPos, 1.0);

    // Pasar datos al fragment shader
    v_TexCoord = a_TexCoord;
    v_Color = a_InstanceColor;
    v_DistanceFromCenter = length(a_TexCoord - vec2(0.5));
}
