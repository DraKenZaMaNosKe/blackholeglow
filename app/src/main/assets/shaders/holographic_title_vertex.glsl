precision mediump float;

attribute vec2 a_Position;
attribute vec2 a_TexCoord;

varying vec2 v_TexCoord;

uniform float u_Time;
uniform float u_GlitchIntensity;

// Función de ruido simple
float random(float x) {
    return fract(sin(x) * 43758.5453123);
}

void main() {
    v_TexCoord = a_TexCoord;

    vec2 pos = a_Position;

    // Pequeña vibración holográfica
    float vibration = sin(u_Time * 15.0 + a_Position.y * 20.0) * 0.002;
    pos.x += vibration * u_GlitchIntensity;

    // Glitch ocasional que desplaza todo el texto
    float glitchTime = floor(u_Time * 8.0);
    float glitchActive = step(0.92, random(glitchTime));
    pos.x += (random(glitchTime + 0.5) - 0.5) * 0.02 * glitchActive * u_GlitchIntensity;

    gl_Position = vec4(pos, 0.0, 1.0);
}
