
#version 100
precision mediump float;
uniform vec4  u_Color;
uniform float u_Reveal;
uniform float u_HaloWidth;
varying float v_PosY;
varying float v_PosX;
void main() {
    // descartar hasta altura reveal
    if (v_PosY > u_Reveal) discard;
    // color base gradiente azul→blanco
    vec3 baseColor = mix(
    vec3(0.2, 0.4, 1.0),
    vec3(1.0, 1.0, 1.0),
    v_PosY
    );
    // halo suave lateral
    float distX = abs(v_PosX - 0.5);
    float halo = smoothstep(u_HaloWidth, 0.0, distX);
    vec3 finalColor = baseColor + halo * 0.5;
    gl_FragColor = vec4(finalColor, 1.0);
}