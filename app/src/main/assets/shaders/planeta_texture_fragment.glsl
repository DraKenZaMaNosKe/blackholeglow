// Fragment shader básico: muestrea la textura sin cambios adicionales
precision highp float;        // alta precisión para floats
varying vec2 v_TexCoord;      // UV escaladas desde el vertex shader
uniform sampler2D u_Texture;  // textura del planeta

void main() {
    // Simplemente pintamos el color exacto de la textura
    gl_FragColor = texture2D(u_Texture, v_TexCoord);
}
