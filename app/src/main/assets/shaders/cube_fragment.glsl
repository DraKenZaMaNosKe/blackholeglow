#version 100
precision mediump float;

// Uniform que recibe el tiempo en segundos
uniform float u_Time;
// Varying con la posición en espacio modelo
varying vec3 vPosModel;

// Identifica la cara (0…5) según la componente con mayor valor absoluto
float faceId() {
    vec3 ap = abs(vPosModel);
    if (ap.x >= ap.y && ap.x >= ap.z) return vPosModel.x > 0.0 ? 0.0 : 1.0;
    if (ap.y >= ap.x && ap.y >= ap.z) return vPosModel.y > 0.0 ? 2.0 : 3.0;
    return vPosModel.z > 0.0 ? 4.0 : 5.0;
}

void main() {
    float fid = faceId();

    // Colores base
    vec3 yellow = vec3(1.0, 1.0, 0.0);
    vec3 white  = vec3(1.0, 1.0, 1.0);

    // Oscilación lenta, con desfase según la cara
    float frequency = 1.0;              // velocidad general
    float phase     = fid * (3.1416/3.0); // ~60° por cara
    float t = 0.5 + 0.5 * sin(u_Time * frequency + phase);

    // Mezcla entre amarillo y blanco
    vec3 color = mix(yellow, white, t);

    gl_FragColor = vec4(color, 1.0);
}