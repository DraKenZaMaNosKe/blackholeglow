#version 300 es
// ╔═══════════════════════════════════════════════════════════════════╗
// ║   ✨ Particle Instanced Fragment Shader - GLSL ES 3.0 ✨         ║
// ╚═══════════════════════════════════════════════════════════════════╝
//
// Fragment shader para partículas con efecto de brillo suave.

precision mediump float;

// Entradas del vertex shader
in vec2 v_TexCoord;
in vec4 v_Color;
in float v_DistanceFromCenter;

// Salida
out vec4 fragColor;

void main() {
    // Calcular distancia desde el centro del quad (0.5, 0.5)
    vec2 center = vec2(0.5);
    float dist = length(v_TexCoord - center);

    // Crear efecto de brillo suave (glow circular)
    // El brillo es más intenso en el centro y desvanece hacia los bordes
    float glow = 1.0 - smoothstep(0.0, 0.5, dist);

    // Agregar un núcleo más brillante
    float core = 1.0 - smoothstep(0.0, 0.15, dist);
    glow = glow + core * 0.5;

    // Aplicar color de la instancia con el brillo
    fragColor = vec4(v_Color.rgb * glow, v_Color.a * glow);
}
