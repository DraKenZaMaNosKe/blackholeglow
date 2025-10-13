#version 100
precision highp float;

varying float v_PosY;
varying float v_PosX;

void main() {
    // COLOR SÓLIDO VERDE BOSQUE - CUBRE TODO EL ESPACIO
    vec3 color = vec3(0.2, 0.8, 0.3);  // Verde brillante
    gl_FragColor = vec4(color, 1.0);
}
