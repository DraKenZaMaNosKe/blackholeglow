#ifdef GL_ES
precision mediump float;
#endif

// ===================================================
// Uniforms
// ===================================================
uniform vec2 u_Resolution;  // Resolución de la pantalla (ancho, alto)
uniform float u_Time;       // Tiempo en segundos (para la animación)

// ===================================================
// Función auxiliar: mezcla de colores con animación
// ===================================================
vec3 colorAnim(float t) {
    // Tonos base: azul → morado
    vec3 colorA = vec3(0.2, 0.4, 1.0); // azul brillante
    vec3 colorB = vec3(0.7, 0.2, 1.0); // morado violeta

    // Oscilación suave entre colorA y colorB
    float k = sin(t) * 0.5 + 0.5;      // valor entre 0 y 1
    return mix(colorA, colorB, k);
}

// ===================================================
// Función principal
// ===================================================
void main() {
    // Coordenadas normalizadas (0.0 a 1.0)
    vec2 uv = gl_FragCoord.xy / u_Resolution.xy;

    // Corrige la relación de aspecto
    uv -= 0.5;
    uv.x *= u_Resolution.x / u_Resolution.y;
    uv += 0.5;

    // Grosor del borde (0.0 a 0.05 recomendado)
    float borde = 0.05;

    // Detección del borde (1.0 en los bordes, 0.0 en el centro)
    float mask = 0.0;
    mask += step(uv.x, borde);                   // borde izquierdo
    mask += step(1.0 - borde, uv.x);             // borde derecho
    mask += step(uv.y, borde);                   // borde inferior
    mask += step(1.0 - borde, uv.y);             // borde superior
    mask = clamp(mask, 0.0, 1.0);

    // Color animado
    vec3 colorBorde = colorAnim(u_Time * 2.0);

    // Suavizado de transición
    float suavizado = smoothstep(borde - 0.005, borde, abs(uv.x - 0.5)) +
    smoothstep(borde - 0.005, borde, abs(uv.y - 0.5));

    // Color final (sin relleno, solo borde)
    vec3 colorFinal = colorBorde * mask;
    float alpha = mask;

    gl_FragColor = vec4(colorFinal, alpha);
}
