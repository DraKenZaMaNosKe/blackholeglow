/**
 * Fragment shader SIMPLE - Solo textura sin efectos
 */

#ifdef GL_ES
precision mediump float;
#endif

// Uniforms
uniform sampler2D u_Texture;   // Textura matasteroide.png
uniform float u_Alpha;          // Transparencia

// Varyings
varying vec2 v_TexCoord;

void main() {
    // Obtener color directamente de la textura
    vec4 texColor = texture2D(u_Texture, v_TexCoord);
    
    // Salida: Solo la textura, sin modificaciones
    gl_FragColor = vec4(texColor.rgb, 1.0);
}
