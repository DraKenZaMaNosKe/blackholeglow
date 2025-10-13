#version 100
precision highp float;

varying float v_PosY;
varying float v_PosX;

void main() {
    // COLOR SÓLIDO ROSA SAKURA - CUBRE TODO EL ESPACIO
    vec3 color = vec3(1.0, 0.7, 0.8);  // Rosa sakura suave
    gl_FragColor = vec4(color, 1.0);
}
