#version 100
precision highp float;

varying float v_PosY;
varying float v_PosX;

void main() {
    // COLOR SÓLIDO MAGENTA RETRO - CUBRE TODO EL ESPACIO
    vec3 color = vec3(0.9, 0.1, 0.7);  // Magenta retro brillante
    gl_FragColor = vec4(color, 1.0);
}
