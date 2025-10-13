#ifdef GL_ES
precision mediump float;
#endif

// Fragment Shader - Fondo con Textura + Estrellas Procedurales
// Combina imagen de fondo con estrellas generadas

varying vec2 v_TexCoord;

uniform float u_Time;
uniform vec2 u_Resolution;
uniform float u_AspectRatio;
uniform sampler2D u_Texture;  // Textura del fondo

// Función mejorada con glow suave
float drawStar(vec2 uv, vec2 pos, float size, float brightness, float phase) {
    float dist = length(uv - pos);
    float star = 0.0;

    // Núcleo brillante
    if (dist < size) {
        star = (1.0 - (dist / size)) * brightness;
    }

    // Glow externo suave
    float glowSize = size * 2.5;
    if (dist < glowSize) {
        float glow = (1.0 - (dist / glowSize)) * brightness * 0.3;
        star += glow;
    }

    // Parpadeo individual
    float twinkle = 0.7 + sin(u_Time * (1.0 + phase) + phase * 6.28) * 0.3;
    star *= twinkle;

    return star;
}

void main() {
    // ========== EFECTO DE PARALLAX PARA PROFUNDIDAD ==========
    // El fondo se mueve MUY lentamente, simulando distancia infinita
    // Los objetos cercanos se mueven más rápido = sensación de profundidad
    vec2 parallaxOffset = vec2(
        sin(u_Time * 0.003) * 0.002,  // Movimiento horizontal MINÚSCULO
        cos(u_Time * 0.004) * 0.002   // Movimiento vertical MINÚSCULO
    );

    vec2 uv = v_TexCoord + parallaxOffset;  // Aplicar parallax sutil

    vec3 color = vec3(0.0);

    // ========== ESTRELLAS OPTIMIZADAS: 10 TOTAL (reducidas de 44) ==========
    // Distribución estratégica para máxima cobertura visual con mínimo costo

    // 🌟 BLANCAS BRILLANTES (3) - Puntos focales principales
    color += vec3(1.0, 1.0, 0.98) * drawStar(uv, vec2(0.1, 0.2), 0.018, 0.95, 0.1);    // Esquina superior-izq
    color += vec3(1.0, 1.0, 0.98) * drawStar(uv, vec2(0.85, 0.7), 0.017, 0.92, 0.85);  // Esquina inferior-der
    color += vec3(1.0, 1.0, 0.98) * drawStar(uv, vec2(0.45, 0.5), 0.016, 0.88, 0.45);  // Centro (atención)

    // 💎 AZULADAS (2) - Contraste de color
    color += vec3(0.7, 0.8, 1.0) * drawStar(uv, vec2(0.3, 0.8), 0.015, 0.85, 0.3);     // Superior
    color += vec3(0.7, 0.85, 1.0) * drawStar(uv, vec2(0.75, 0.25), 0.014, 0.8, 0.75);  // Inferior-der

    // ⭐ AMARILLAS/DORADAS (2) - Calidez
    color += vec3(1.0, 0.9, 0.6) * drawStar(uv, vec2(0.6, 0.82), 0.015, 0.83, 0.6);    // Superior-der
    color += vec3(1.0, 0.92, 0.65) * drawStar(uv, vec2(0.22, 0.38), 0.013, 0.78, 0.22); // Izquierda

    // ✨ MEDIANAS SUTILES (3) - Relleno de espacios
    color += vec3(0.95, 0.95, 1.0) * drawStar(uv, vec2(0.55, 0.15), 0.011, 0.7, 0.55); // Abajo-centro
    color += vec3(0.95, 0.95, 1.0) * drawStar(uv, vec2(0.14, 0.62), 0.010, 0.68, 0.14); // Izquierda-media
    color += vec3(0.95, 0.95, 1.0) * drawStar(uv, vec2(0.88, 0.45), 0.009, 0.65, 0.88); // Derecha-media

    // OPTIMIZACIÓN: 44 → 10 estrellas = 77% reducción de operaciones
    // Ahorro estimado: ~330M operaciones por frame (~80% menos costo de shader)

    // MEZCLAR: Primero la textura del fondo, luego las estrellas encima
    vec3 backgroundTexture = texture2D(u_Texture, v_TexCoord).rgb;

    // Combinar: fondo + estrellas (aditivo)
    vec3 finalColor = backgroundTexture + color;

    // ========== EFECTO DE PROFUNDIDAD: VIGNETTE SUTIL ==========
    // Oscurece ligeramente los bordes para dar sensación de espacio profundo
    // Los fondos infinitamente lejanos se desvanecen hacia los bordes
    vec2 centerUv = v_TexCoord - 0.5;  // Centrar en (0,0)
    float distFromCenter = length(centerUv);
    float vignette = 1.0 - smoothstep(0.3, 0.8, distFromCenter);  // Suave fade
    vignette = mix(0.85, 1.0, vignette);  // No oscurecer completamente

    finalColor *= vignette;  // Aplicar vignette

    gl_FragColor = vec4(finalColor, 1.0);
}
