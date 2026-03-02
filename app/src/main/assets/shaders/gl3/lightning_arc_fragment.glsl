#version 300 es
precision mediump float;

in float v_Alpha;

uniform vec3 u_CoreColor;
uniform vec3 u_GlowColor;
uniform float u_Intensity;
uniform float u_Time;

out vec4 fragColor;

void main() {
    // Flicker effect
    float flicker = 0.85 + 0.15 * sin(u_Time * 30.0 + v_Alpha * 10.0);

    // Core is brighter, edges use glow color
    vec3 color = mix(u_GlowColor, u_CoreColor, v_Alpha);
    float alpha = v_Alpha * u_Intensity * flicker;

    fragColor = vec4(color * alpha, alpha);
}
