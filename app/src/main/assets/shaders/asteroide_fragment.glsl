precision highp float;
varying vec2 v_TexCoord;
uniform sampler2D u_Texture;
uniform int     u_UseSolidColor;
uniform vec4    u_SolidColor;
uniform float   u_Alpha;

void main() {
    vec4 baseColor = texture2D(u_Texture, v_TexCoord);
    if (u_UseSolidColor == 1) {
        gl_FragColor = vec4(u_SolidColor.rgb, u_SolidColor.a * u_Alpha);
    } else {
        gl_FragColor = vec4(baseColor.rgb, baseColor.a * u_Alpha);
    }
}
