// Fragment shader para estrellas hermosas y relajantes
#ifdef GL_ES
precision mediump float;
#endif

uniform vec4 u_Color;
uniform float u_Time;
uniform float u_StarId;
uniform float u_Pulse;
varying vec3 v_Position;

// Función de ruido suave
float smoothNoise(float t) {
    return sin(t) * 0.5 + sin(t * 2.1) * 0.25 + sin(t * 4.3) * 0.125;
}

void main() {
    // Calcular distancia al centro para crear forma de estrella
    vec2 center = vec2(0.5, 0.5);
    vec2 pos = gl_FragCoord.xy / 15.0;  // Ajustar tamaño
    float dist = distance(pos, center);

    // Crear forma de estrella con múltiples capas de brillo
    float star = 0.0;

    // Núcleo brillante
    float core = 1.0 / (1.0 + dist * dist * 50.0);
    star += core * 2.0;

    // Halo medio
    float halo = exp(-dist * 8.0);
    star += halo * 0.6;

    // Corona exterior suave
    float corona = exp(-dist * 3.0);
    star += corona * 0.3;

    // Rayos de luz (4 puntas)
    vec2 dir = normalize(pos - center);
    float rays = pow(abs(dir.x * dir.y), 0.3);
    star += rays * (1.0 - smoothstep(0.0, 0.4, dist)) * 0.5;

    // Parpadeo suave y relajante
    float twinkle = smoothNoise(u_Time * (1.5 + u_StarId * 0.3) + u_StarId * 6.28);
    twinkle = twinkle * 0.15 + 0.85;  // Parpadeo muy sutil

    // Respiración lenta de la estrella
    float breathing = sin(u_Time * 0.5 + u_StarId * 3.14) * 0.05 + 0.95;

    // Pulsos ocasionales más intensos
    float intensePulse = 0.0;
    if (u_Pulse > 0.0) {
        intensePulse = u_Pulse * sin(u_Time * 8.0) * 0.3;
    }

    // Color de la estrella con variaciones sutiles
    vec3 starColor = u_Color.rgb;

    // Variación de color muy sutil basada en el tiempo
    float colorShift = smoothNoise(u_Time * 0.3 + u_StarId * 2.0) * 0.05;
    starColor.r += colorShift;
    starColor.b -= colorShift;

    // Añadir tono dorado/plateado según el tipo
    if (u_StarId < 0.3) {
        // Estrellas azules-plateadas
        starColor *= vec3(0.9, 0.95, 1.0);
    } else if (u_StarId < 0.6) {
        // Estrellas blancas puras
        starColor *= vec3(1.0, 1.0, 0.98);
    } else {
        // Estrellas doradas cálidas
        starColor *= vec3(1.0, 0.95, 0.85);
    }

    // Aplicar todos los efectos
    star *= twinkle * breathing;
    star += intensePulse;

    // Asegurar que el centro siempre sea visible
    star = max(star, core * 0.3);

    // Alpha final con desvanecimiento suave
    float alpha = star * u_Color.a;
    alpha = clamp(alpha, 0.0, 1.0);

    // Añadir un toque de brillo extra en el núcleo
    if (dist < 0.1) {
        starColor += vec3(0.2, 0.2, 0.3);
    }

    gl_FragColor = vec4(starColor * star, alpha);
}