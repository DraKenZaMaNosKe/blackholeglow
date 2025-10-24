// ============================================
// avatar_fragment.glsl
// Fragment shader simple para AvatarSphere
// Muestra la textura del avatar sin efectos
// ============================================

precision mediump float;

uniform sampler2D u_Texture;
uniform float u_Alpha;

varying vec2 v_TexCoord;

void main() {
    // ════════════════════════════════════════════════════════════
    // ✅ TEXTURA SIMPLE SIN EFECTOS
    // ════════════════════════════════════════════════════════════
    // Solo samplear la textura y aplicar alpha
    // Sin destellos, sin animaciones, solo la imagen del avatar

    vec4 texColor = texture2D(u_Texture, v_TexCoord);

    // Aplicar alpha uniform (permite fade in/out si es necesario)
    gl_FragColor = vec4(texColor.rgb, texColor.a * u_Alpha);
}
