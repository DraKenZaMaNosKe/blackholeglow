#version 100
precision highp float;

varying float v_PosY;
varying float v_PosX;

void main() {
    // COLOR SÓLIDO ROSA NEÓN - CUBRE TODO EL ESPACIO
    vec3 color = vec3(1.0, 0.2, 0.6);  // Rosa neón brillante
    gl_FragColor = vec4(color, 1.0);
}
