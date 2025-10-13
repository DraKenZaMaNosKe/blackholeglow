#version 100
precision highp float;

varying float v_PosY;
varying float v_PosX;

void main() {
    // COLOR SÓLIDO AZUL ESPACIAL - CUBRE TODO EL ESPACIO
    vec3 color = vec3(0.2, 0.4, 1.0);  // Azul espacial brillante
    gl_FragColor = vec4(color, 1.0);
}
