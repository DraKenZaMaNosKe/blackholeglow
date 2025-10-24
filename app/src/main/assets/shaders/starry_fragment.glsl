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

// ════════════════════════════════════════════════════════════════════════
// 🌟 FUNCIONES DE RUIDO PARA PARPADEO DE ESTRELLAS
// ════════════════════════════════════════════════════════════════════════
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);  // Smoothstep

    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));

    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

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

// ════════════════════════════════════════════════════════════════════════
// ✨ DESTELLO DE PARTÍCULA (para estelas de estrellas fugaces)
// ════════════════════════════════════════════════════════════════════════
float drawSparkle(vec2 uv, vec2 pos, float size, float brightness) {
    float dist = length(uv - pos);

    // Núcleo brillante con forma de cruz
    float core = max(0.0, 1.0 - (dist / size)) * brightness;

    // Cruz de luz (efecto de destello)
    vec2 delta = uv - pos;
    float crossGlow = 0.0;

    // Línea horizontal
    float horizDist = abs(delta.y);
    if (horizDist < size * 0.3) {
        crossGlow += (1.0 - horizDist / (size * 0.3)) * brightness * 0.5;
    }

    // Línea vertical
    float vertDist = abs(delta.x);
    if (vertDist < size * 0.3) {
        crossGlow += (1.0 - vertDist / (size * 0.3)) * brightness * 0.5;
    }

    return core + crossGlow;
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

    // ═══════════════════════════════════════════════════════════════════════
    // ✨ ESTRELLAS FUGACES OCASIONALES (Opción C)
    // ═══════════════════════════════════════════════════════════════════════
    // Sistema más dinámico y cinematográfico que reemplaza estrellas estáticas
    // 2 estrellas fugaces que aparecen cada 12-15 segundos
    // ═══════════════════════════════════════════════════════════════════════

    // ═══════════════════════════════════════════════════════════════════════
    // 🌠 ESTRELLA FUGAZ 1 - Cyan/Dorada con destellos mágicos
    // ═══════════════════════════════════════════════════════════════════════
    float shootingTime1 = mod(u_Time, 15.0);  // Ciclo de 15 segundos
    if (shootingTime1 < 2.5) {  // Visible 2.5 segundos (más tiempo para apreciarla)
        float progress1 = shootingTime1 / 2.5;  // 0.0 → 1.0

        // Trayectoria diagonal elegante
        vec2 starPos1 = vec2(
            progress1 * 1.3 - 0.15,          // Atraviesa toda la pantalla
            0.15 + progress1 * 0.7           // Ángulo pronunciado
        );

        // Fade in/out muy suave y largo
        float fade1 = smoothstep(0.0, 0.15, progress1) * smoothstep(1.0, 0.85, progress1);

        // ✨ ESTRELLA PRINCIPAL con colores cyan-dorado
        vec3 starColor1 = mix(
            vec3(0.3, 0.8, 1.0),    // Cyan brillante
            vec3(1.0, 0.9, 0.4),    // Dorado
            progress1               // Transición durante el viaje
        );
        float starCore = drawStar(uv, starPos1, 0.015, 1.0, 0.0) * fade1;
        color += starColor1 * starCore * 1.5;  // Más brillante

        // ✨ DESTELLOS DE PARTÍCULAS en la estela (lucesitas!)
        for (int i = 1; i < 8; i++) {
            float trailOffset = float(i) * 0.035;
            vec2 trailPos = starPos1 - vec2(trailOffset * 1.5, trailOffset * 0.9);

            // Cada partícula tiene un destello en cruz
            float sparkle = drawSparkle(uv, trailPos, 0.012, 0.6) * fade1;
            float particleFade = (1.0 - float(i) * 0.12);  // Se desvanecen gradualmente

            // Color que va de cyan a púrpura en la estela
            vec3 trailColor = mix(
                vec3(0.4, 0.9, 1.0),    // Cyan
                vec3(0.8, 0.3, 1.0),    // Púrpura
                float(i) / 8.0
            );

            color += trailColor * sparkle * particleFade;
        }

        // ✨ HALO DIFUSO alrededor de la estrella
        float halo = drawStar(uv, starPos1, 0.04, 0.4, 0.0) * fade1;
        color += starColor1 * halo * 0.8;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🌠 ESTRELLA FUGAZ 2 - Púrpura/Azul con destellos mágicos
    // ═══════════════════════════════════════════════════════════════════════
    float shootingTime2 = mod(u_Time + 7.5, 13.0);  // Ciclo de 13s, desfasada 7.5s
    if (shootingTime2 < 2.2) {  // Visible 2.2 segundos
        float progress2 = shootingTime2 / 2.2;

        // Trayectoria opuesta (derecha a izquierda)
        vec2 starPos2 = vec2(
            1.15 - progress2 * 1.3,          // De derecha a izquierda
            0.25 + progress2 * 0.6           // Ángulo diferente
        );

        float fade2 = smoothstep(0.0, 0.12, progress2) * smoothstep(1.0, 0.88, progress2);

        // ✨ ESTRELLA PRINCIPAL con colores púrpura-azul
        vec3 starColor2 = mix(
            vec3(0.9, 0.4, 1.0),    // Púrpura brillante
            vec3(0.3, 0.7, 1.0),    // Azul brillante
            progress2
        );
        float starCore2 = drawStar(uv, starPos2, 0.013, 0.95, 0.0) * fade2;
        color += starColor2 * starCore2 * 1.4;

        // ✨ DESTELLOS DE PARTÍCULAS (lucesitas!)
        for (int i = 1; i < 7; i++) {
            float trailOffset = float(i) * 0.04;
            vec2 trailPos = starPos2 + vec2(trailOffset * 1.4, -trailOffset * 0.7);

            float sparkle = drawSparkle(uv, trailPos, 0.010, 0.55) * fade2;
            float particleFade = (1.0 - float(i) * 0.14);

            // Color que va de púrpura a azul-verde en la estela
            vec3 trailColor = mix(
                vec3(0.8, 0.4, 1.0),    // Púrpura
                vec3(0.3, 0.9, 0.8),    // Azul-verde (aqua)
                float(i) / 7.0
            );

            color += trailColor * sparkle * particleFade;
        }

        // ✨ HALO DIFUSO
        float halo2 = drawStar(uv, starPos2, 0.035, 0.35, 0.0) * fade2;
        color += starColor2 * halo2 * 0.7;
    }

    // OPTIMIZACIÓN: De 10 estrellas estáticas a 2 estrellas fugaces ocasionales
    // Costo promedio: ~95% reducción (solo visibles ~20% del tiempo)
    // Efecto visual: MÁS cinematográfico y llamativo

    // ═══════════════════════════════════════════════════════════════════════
    // ⭐ ESTRELLAS PARPADEANTES PROCEDURALES (CIRCULARES)
    // ═══════════════════════════════════════════════════════════════════════
    // En lugar de modificar píxeles cuadrados, dibujamos estrellas circulares
    // ═══════════════════════════════════════════════════════════════════════

    vec3 backgroundTexture = texture2D(u_Texture, v_TexCoord).rgb;
    vec3 twinklingStars = vec3(0.0);

    // Grid de 20x20 para generar estrellas de forma consistente
    float gridSize = 20.0;
    vec2 gridCoord = v_TexCoord * gridSize;
    vec2 gridCell = floor(gridCoord);

    // Revisar celdas vecinas (3x3 grid) para evitar que estrellas se corten
    for (float y = -1.0; y <= 1.0; y += 1.0) {
        for (float x = -1.0; x <= 1.0; x += 1.0) {
            vec2 neighborCell = gridCell + vec2(x, y);

            // Generar posición aleatoria dentro de la celda
            float cellHash = hash(neighborCell);

            // No todas las celdas tienen estrella (50% de probabilidad)
            if (cellHash > 0.5) {
                // Posición exacta de la estrella dentro de la celda
                vec2 starPosInCell = vec2(
                    hash(neighborCell + vec2(123.0, 0.0)),
                    hash(neighborCell + vec2(0.0, 456.0))
                );
                vec2 starPos = (neighborCell + starPosInCell) / gridSize;

                // Distancia del pixel actual a la estrella
                float dist = length((v_TexCoord - starPos) * u_Resolution / max(u_Resolution.x, u_Resolution.y));

                // Tamaño de la estrella (varía por estrella)
                float starSize = 0.0015 + hash(neighborCell + vec2(789.0, 0.0)) * 0.002;

                // Frecuencia de parpadeo única por estrella
                float starSeed = cellHash;
                float frequency = 0.8 + starSeed * 1.5;

                // Parpadeo suave
                float twinkle1 = sin(u_Time * frequency + starSeed * 6.28) * 0.5 + 0.5;
                float twinkle2 = sin(u_Time * frequency * 1.3 + starSeed * 3.14) * 0.5 + 0.5;
                float twinkle3 = noise(neighborCell + u_Time * 0.15);
                float twinkleAmount = mix(twinkle1, twinkle2, 0.4) * 0.6 + twinkle3 * 0.4;
                twinkleAmount = pow(twinkleAmount, 1.5);

                // Brillo de la estrella (50%-150%)
                float brightness = 0.5 + twinkleAmount * 1.0;

                // Dibujar estrella CIRCULAR con glow suave
                if (dist < starSize * 3.0) {
                    // Núcleo brillante
                    float core = smoothstep(starSize * 1.5, 0.0, dist);

                    // Glow exterior suave
                    float glow = smoothstep(starSize * 3.0, starSize * 0.5, dist);

                    // Intensidad combinada
                    float intensity = core * 1.0 + glow * 0.3;
                    intensity *= brightness;

                    // Color de la estrella (aleatorio entre blanco, azul y amarillo)
                    vec3 starColor;
                    if (starSeed < 0.33) {
                        starColor = vec3(0.9, 0.95, 1.0);  // Blanco azulado
                    } else if (starSeed < 0.66) {
                        starColor = vec3(1.0, 1.0, 0.95);  // Blanco cálido
                    } else {
                        starColor = vec3(1.0, 0.95, 0.9);  // Amarillo suave
                    }

                    // Acumular luz de la estrella
                    twinklingStars += starColor * intensity;
                }
            }
        }
    }

    // Combinar: fondo + estrellas procedurales + estrellas fugaces
    vec3 finalColor = backgroundTexture + twinklingStars + color;

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
