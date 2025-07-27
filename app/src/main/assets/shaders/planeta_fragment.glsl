// shaders/planeta_fragment.glsl

precision highp float;            // alta precisión para fase estable
uniform highp float u_Time;       // fase en radianes [0, 2π)
// (envuelta en Java: mod(accTime, period)*2π/period)
varying vec2    v_TexCoord;       // UV del vertex shader
uniform sampler2D u_Texture;      // textura del planeta

// Parámetros de pulso:
// cada [0.5s] un pulso → #PULSE_PERIOD#
const highp float period       = 0.5;
// duración del pulso  [0.1s]  → #PULSE_DURATION#
const highp float duration     = 0.1;
// dos π
const highp float twoPi        = 6.28318530718;
// anchura del pulso en radianes
const highp float widthPhase   = (duration/period) * twoPi;

void main() {
    // 1) Base: textura
    vec4 color = texture2D(u_Texture, v_TexCoord);

    // 2) Intensidad triangular: de 1 a 0 en [0, widthPhase]
    highp float intensity = clamp(1.0 - u_Time/widthPhase, 0.0, 1.0);

    // 3) Brillo entre 0.85 y 1.15 según intensidad
    highp float brightness = mix(0.85, 1.15, intensity);

    // 4) Aplico el pulso
    color.rgb *= brightness;

    gl_FragColor = color;
}

