
// ============================================
// archivo: universe_fragment.glsl
// ============================================
precision mediump float;
varying vec2 v_TexCoord;
uniform sampler2D u_Texture;
uniform float u_Time;  // Agregar tiempo para efectos animados

void main() {
    // Efecto de parallax sutil
    vec2 uv = v_TexCoord;
    uv.x += sin(u_Time * 0.1) * 0.01;
    uv.y += cos(u_Time * 0.15) * 0.01;

    vec4 color = texture2D(u_Texture, uv);

    // Añadir brillo pulsante sutil
    float pulse = 0.95 + 0.05 * sin(u_Time * 2.0);
    color.rgb *= pulse;

    gl_FragColor = color;
}