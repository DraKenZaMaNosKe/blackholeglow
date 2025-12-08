/**
 * ╔════════════════════════════════════════════════════════════════╗
 * ║  🪐 SATURNO - Fragment Shader Simple                           ║
 * ║  Renderiza textura con alpha                                   ║
 * ╚════════════════════════════════════════════════════════════════╝
 */

#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_Texture;
uniform float u_Alpha;

varying vec2 v_TexCoord;

void main() {
    // Invertir V para compatibilidad con modelos Meshy
    vec2 uv = vec2(v_TexCoord.x, 1.0 - v_TexCoord.y);

    vec4 texColor = texture2D(u_Texture, uv);

    // Brillo ligeramente aumentado
    vec3 finalColor = texColor.rgb * 1.15;

    gl_FragColor = vec4(finalColor, texColor.a * u_Alpha);
}
