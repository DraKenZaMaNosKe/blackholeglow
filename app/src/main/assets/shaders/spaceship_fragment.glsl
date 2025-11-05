// Fragment Shader para Naves Espaciales
// Textura con transparencia y opción de tinte de color

precision mediump float;

uniform sampler2D u_Texture;
uniform vec4 u_TintColor;  // Color de tinte (RGB) + intensidad (A)
uniform float u_Alpha;      // Alpha global

varying vec2 v_TexCoord;

void main() {
    vec4 texColor = texture2D(u_Texture, v_TexCoord);

    // Alpha final = textura * alpha global
    float finalAlpha = texColor.a * u_Alpha;

    // ✅ DESCARTAR píxeles casi transparentes (elimina cuadros blancos)
    if (finalAlpha < 0.1) {
        discard;
    }

    // Mezclar con color de tinte si hay intensidad
    vec3 finalColor = mix(texColor.rgb, u_TintColor.rgb, u_TintColor.a);

    gl_FragColor = vec4(finalColor, finalAlpha);
}
