// Fragment shader SIMPLE para meteoritos - Solo textura sin efectos
#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_Texture;
uniform vec4 u_Color;
uniform float u_Opacity;

varying vec2 v_TexCoord;

void main() {
    // Obtener el color de la textura
    vec4 texColor = texture2D(u_Texture, v_TexCoord);

    // Aplicar opacidad
    float alpha = u_Opacity * texColor.a;

    // Color final = textura pura (sin tintes ni efectos)
    gl_FragColor = vec4(texColor.rgb, alpha);
}
