// Fragment shader para meteoritos con efectos de fuego y plasma
precision mediump float;

uniform vec4 u_Color;
uniform float u_Opacity;
uniform float u_Time;

varying vec2 v_TexCoord;
varying vec3 v_Position;

// Función simple de ruido
float noise(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    // Calcular distancia al centro para efecto de calor
    vec2 center = vec2(0.5, 0.5);
    float dist = length(v_TexCoord - center);

    // Efecto de calor/brillo desde el centro
    float heat = 1.0 - smoothstep(0.0, 0.5, dist);
    heat = pow(heat, 1.2);

    // Efecto de fuego animado
    float fire = noise(v_TexCoord * 10.0 + u_Time) * 0.3;
    fire += sin(u_Time * 8.0 + v_TexCoord.x * 5.0) * 0.2;

    // Gradiente de color fuego (naranja -> amarillo -> blanco)
    vec3 fireColor = mix(vec3(1.0, 0.3, 0.0), vec3(1.0, 0.8, 0.0), heat);
    fireColor = mix(fireColor, vec3(1.0, 1.0, 0.8), heat * heat);

    // Aplicar variaciones de fuego
    fireColor *= (1.0 + fire * 0.5);

    // Añadir brillo extra en el centro
    fireColor += vec3(1.0, 0.9, 0.7) * heat * heat * 0.5;

    // Efecto de corona ardiente
    float corona = 1.0 - dist;
    corona = pow(corona, 3.0);
    fireColor += vec3(1.0, 0.5, 0.2) * corona * 0.3;

    // Pulso de energía
    float pulse = sin(u_Time * 6.0) * 0.1 + 0.9;
    fireColor *= pulse;

    // Calcular alpha con efecto de desvanecimiento en los bordes
    float alpha = u_Opacity * u_Color.a;
    alpha *= (heat * 0.7 + 0.3);

    // Color final mezclado con el color del uniforme
    vec3 finalColor = mix(u_Color.rgb, fireColor, 0.7);

    gl_FragColor = vec4(finalColor, alpha);
}