/**
 * ╔════════════════════════════════════════════════════════════════╗
 * ║  🌍 MESHY MODELS - Shader Simple sin Sombras                   ║
 * ║  Solo muestra la textura sin iluminación direccional           ║
 * ║  Perfecto para modelos de Meshy AI                             ║
 * ╚════════════════════════════════════════════════════════════════╝
 */

#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_Texture;
uniform float u_Alpha;
uniform float u_Time;

varying vec2 v_TexCoord;

void main() {
    // Invertir V para compatibilidad con modelos Meshy
    vec2 uv = vec2(v_TexCoord.x, 1.0 - v_TexCoord.y);

    // Solo mostrar la textura tal cual, sin sombras
    vec4 texColor = texture2D(u_Texture, uv);

    // Aumentar un poco el brillo para que se vea bien
    vec3 finalColor = texColor.rgb * 1.2;

    // Clamp para evitar sobre-exposición
    finalColor = clamp(finalColor, 0.0, 1.0);

    gl_FragColor = vec4(finalColor, texColor.a * u_Alpha);
}
