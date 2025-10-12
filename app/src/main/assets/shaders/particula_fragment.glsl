// Fragment Shader para Partículas Explosivas
// Color sólido con alpha - optimizado para brillo

precision mediump float;

uniform vec4 u_Color;  // Color RGBA de la partícula

void main() {
    // Color sólido simple
    gl_FragColor = u_Color;
}
