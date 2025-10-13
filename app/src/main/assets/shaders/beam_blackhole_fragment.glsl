#version 100
precision highp float;

varying float v_PosY;
varying float v_PosX;

void main() {
    // COLOR SÓLIDO PÚRPURA AGUJERO NEGRO - CUBRE TODO EL ESPACIO
    vec3 color = vec3(0.5, 0.1, 0.8);  // Púrpura profundo
    gl_FragColor = vec4(color, 1.0);
}
