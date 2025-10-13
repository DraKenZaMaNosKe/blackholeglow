#version 100
precision highp float;

varying float v_PosY;
varying float v_PosX;

void main() {
    // COLOR SÓLIDO AZUL LLUVIA - CUBRE TODO EL ESPACIO
    vec3 color = vec3(0.3, 0.5, 0.9);  // Azul lluvia
    gl_FragColor = vec4(color, 1.0);
}
