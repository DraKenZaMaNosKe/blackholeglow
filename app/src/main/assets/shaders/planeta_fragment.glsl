precision mediump float;

// UV interpoladas desde el vertex shader
varying vec2 v_TexCoord;

// Textura base (lava, magma, etc.)
uniform sampler2D u_Texture;

// Tiempo animado (en segundos), lo sube BaseShaderProgram
uniform float u_Time;

// Resolución de pantalla (opcional, si lo necesitas en efectos)
uniform vec2 u_Resolution;

void main() {
    // --- 1) Textura base ---
    vec4 texColor = texture2D(u_Texture, v_TexCoord);

    // --- 2) Coordenadas centradas (para efectos radiales) ---
    vec2 centered = v_TexCoord - 0.5;
    float dist = length(centered) * 2.0;

    // --- 3) Rayos solares pulsantes ---
    // Generamos “flamas” radialmente desplazadas por el tiempo
    float angle = atan(centered.y, centered.x);
    float flame = sin(12.0 * dist - u_Time * 3.0 + angle * 4.0);
    flame = smoothstep(0.2, 1.0, flame);

    // --- 4) Pulsación extra (golpes de energía) ---
    float pulse = sin(u_Time * 5.0 + dist * 10.0);
    pulse = pow(pulse * 0.5 + 0.5, 3.0);

    // --- 5) Color de las llamaradas ---
    // Tonos cálidos (amarillo/naranja) realzados por el pulso
    vec3 flareColor = mix(vec3(1.0, 0.5, 0.0), vec3(1.0, 0.9, 0.3), pulse);
    flareColor *= flame;

    // --- 6) Combina la textura con las llamaradas ---
    // Ajusta el peso según intensidad de flama y pulso
    float intensity = 0.6 * flame + 0.4 * pulse;
    vec3 finalColor = mix(texColor.rgb, flareColor + texColor.rgb, intensity);

    gl_FragColor = vec4(finalColor, 1.0);
}
