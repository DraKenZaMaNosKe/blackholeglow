// fragment shader para UniverseBackground
precision mediump float;

// 1) Recibe las UV interpoladas
varying vec2 v_TexCoord;

// 2) Textura de fondo (sampler2D)
uniform sampler2D u_Texture;

void main() {
    // 3) Muestra el color de la textura en la coordenada UV
    gl_FragColor = texture2D(u_Texture, v_TexCoord);
}
