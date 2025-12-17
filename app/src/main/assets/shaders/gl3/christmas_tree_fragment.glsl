#version 300 es
precision highp float;

// Christmas Tree Fragment Shader - With Texture

in vec2 v_TexCoord;
in float v_Height;

uniform sampler2D u_Texture;
uniform float u_Time;

out vec4 fragColor;

void main() {
    // Sample the texture
    vec4 texColor = texture(u_Texture, v_TexCoord);

    // Discard fully transparent pixels
    if (texColor.a < 0.1) {
        discard;
    }

    // Subtle ambient occlusion - darker at the bottom
    float ao = mix(0.7, 1.0, smoothstep(-0.5, 1.5, v_Height));

    // Subtle sparkle effect for ornaments (based on brightness)
    float brightness = dot(texColor.rgb, vec3(0.299, 0.587, 0.114));
    if (brightness > 0.6) {
        float sparkle = sin(u_Time * 5.0 + v_Height * 10.0) * 0.5 + 0.5;
        texColor.rgb += sparkle * 0.1 * texColor.rgb;
    }

    // Apply ambient occlusion
    texColor.rgb *= ao;

    // Slight warm tint for Christmas atmosphere
    texColor.rgb = mix(texColor.rgb, texColor.rgb * vec3(1.05, 1.0, 0.95), 0.1);

    fragColor = texColor;
}
