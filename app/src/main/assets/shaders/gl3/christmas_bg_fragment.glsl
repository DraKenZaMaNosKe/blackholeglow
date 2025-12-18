#version 300 es
// ╔═══════════════════════════════════════════════════════════════════╗
// ║   🎄 Christmas Background Fragment Shader - GLSL ES 3.0          ║
// ║   Base limpia para construir efectos artísticos                  ║
// ╚═══════════════════════════════════════════════════════════════════╝

precision mediump float;

// Inputs
in vec2 v_TexCoord;

// Outputs
out vec4 fragColor;

// Uniforms
uniform sampler2D u_Texture;
uniform float u_Time;
uniform vec2 u_Resolution;

void main() {
    vec2 uv = v_TexCoord;

    // Textura base - sin efectos
    vec3 color = texture(u_Texture, uv).rgb;

    fragColor = vec4(color, 1.0);
}
