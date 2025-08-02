#version 100
precision highp float;

uniform vec4  u_Color;      // Color base RGBA
uniform float u_Reveal;     // Fracción de altura revelada (0.0–1.0)
uniform float u_HaloWidth;  // Anchura del halo (0.0–0.5)

varying float v_PosY;
varying float v_PosX;

void main() {
    // 1) Descarta fragmentos que aún no se revelan
    if (v_PosY > u_Reveal) discard;

    // 2) Color base: gradiente azul→blanco según vertical
    vec3 baseColor = mix(
    vec3(0.2, 0.4, 1.0),  // azul saturado abajo
    vec3(1.0, 1.0, 1.0),  // blanco en punta
    v_PosY               // v_PosY = 0 abajo, 1 arriba
    );

    // 3) Cálculo de halo: suave brillo lateral
    float distX = abs(v_PosX - 0.5);
    float halo = smoothstep(u_HaloWidth, 0.0, distX);

    // 4) Mezcla final
    vec3 finalColor = baseColor + halo * 0.5;

    gl_FragColor = vec4(finalColor, 1.0);
}
