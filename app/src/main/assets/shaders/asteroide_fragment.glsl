// ============================================
// archivo: asteroide_fragment.glsl
// ============================================
precision highp float;
varying vec2 v_TexCoord;
varying float v_Height;
uniform sampler2D u_Texture;
uniform int u_UseSolidColor;
uniform vec4 u_SolidColor;
uniform float u_Alpha;
uniform float u_Time;

void main() {
    vec4 baseColor = texture2D(u_Texture, v_TexCoord);

    if (u_UseSolidColor == 1) {
        gl_FragColor = vec4(u_SolidColor.rgb, u_SolidColor.a * u_Alpha);
    } else {
        // Añadir variación de color basada en altura
        float heightFactor = 0.7 + 0.3 * v_Height;
        baseColor.rgb *= heightFactor;

        // Efecto de "quemar" en la entrada a la atmósfera
        float heat = sin(u_Time * 5.0) * 0.2 + 0.8;
        baseColor.r *= heat * 1.2;

        gl_FragColor = vec4(baseColor.rgb, baseColor.a * u_Alpha);
    }
}