#version 100
precision highp float;

varying float v_PosY;
varying float v_PosX;

void main() {
    // COLOR SÓLIDO NARANJA SAFARI - CUBRE TODO EL ESPACIO
    vec3 color = vec3(1.0, 0.6, 0.1);  // Naranja brillante
    gl_FragColor = vec4(color, 1.0);
}
