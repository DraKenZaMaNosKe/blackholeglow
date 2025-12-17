#version 300 es
precision highp float;

// ╔═══════════════════════════════════════════════════════════════╗
// ║  Christmas Tree Vertex Shader                                 ║
// ║  Wind animation based on vertex height                        ║
// ╚═══════════════════════════════════════════════════════════════╝

// Attributes
in vec3 a_Position;
in vec2 a_TexCoord;

// Uniforms
uniform mat4 u_MVP;
uniform float u_Time;
uniform float u_Wind;

// Varyings
out vec2 v_TexCoord;
out float v_Height;

void main() {
    vec3 pos = a_Position;

    // Wind effect - more movement at the top of the tree
    // The higher the vertex, the more it sways
    float heightFactor = max(0.0, pos.y);  // Only positive Y (above ground)
    float windDisplacement = sin(u_Time * 2.0 + pos.y * 3.0) * u_Wind * heightFactor;

    // Apply wind displacement to X position
    pos.x += windDisplacement * 0.5;

    // Also a subtle Z sway for more natural movement
    pos.z += cos(u_Time * 1.5 + pos.y * 2.0) * u_Wind * heightFactor * 0.3;

    gl_Position = u_MVP * vec4(pos, 1.0);

    v_TexCoord = a_TexCoord;
    v_Height = pos.y;
}
