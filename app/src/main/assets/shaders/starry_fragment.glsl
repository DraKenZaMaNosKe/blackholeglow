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

// ════════════════════════════════════════════════════════════════════════
// 🌸 ESTRELLA MÁGICA - Rosa con pulsación + cambio de color gradual
// ════════════════════════════════════════════════════════════════════════
vec3 drawMagicalStarSimple(vec2 uv, vec2 pos, float time) {
    float dist = length(uv - pos);
    vec3 magicColor = vec3(0.0);

    // ═══════════════════════════════════════════════════════════
    // 🌈 CAMBIO DE COLOR GRADUAL (rosa → púrpura → azul → rosa)
    // ═══════════════════════════════════════════════════════════
    // Ciclo MÁS RÁPIDO y MÁS VISIBLE
    float colorPhase = time * 0.8;  // Velocidad más rápida (ciclo cada 8 seg)

    // Tres colores MUCHO MÁS INTENSOS y saturados
    vec3 colorRosa = vec3(1.0, 0.0, 0.5);       // ROSA MAGENTA INTENSO
    vec3 colorPurpura = vec3(0.8, 0.0, 1.0);    // PÚRPURA MUY BRILLANTE
    vec3 colorAzul = vec3(0.0, 0.5, 1.0);       // AZUL CYAN INTENSO

    // Interpolación suave entre los 3 colores
    float transition = sin(colorPhase) * 0.5 + 0.5;  // 0.0 - 1.0

    vec3 colorCore, colorGlow;

    // Transición en 3 fases
    float phase = mod(colorPhase, 6.28);  // 0 a 2π

    if (phase < 2.09) {
        // Fase 1: Rosa → Púrpura
        float t = phase / 2.09;
        colorCore = mix(colorRosa, colorPurpura, t);
        colorGlow = colorCore;  // Mismo color para que sea más visible
    } else if (phase < 4.18) {
        // Fase 2: Púrpura → Azul
        float t = (phase - 2.09) / 2.09;
        colorCore = mix(colorPurpura, colorAzul, t);
        colorGlow = colorCore;  // Mismo color para que sea más visible
    } else {
        // Fase 3: Azul → Rosa
        float t = (phase - 4.18) / 2.10;
        colorCore = mix(colorAzul, colorRosa, t);
        colorGlow = colorCore;  // Mismo color para que sea más visible
    }

    // ═══════════════════════════════════════════════════════════
    // 💓 PULSACIÓN ORGÁNICA (respiración suave)
    // ═══════════════════════════════════════════════════════════
    // Dos ondas de pulsación combinadas para efecto natural
    float pulse1 = sin(time * 0.8) * 0.5 + 0.5;  // Onda lenta
    float pulse2 = sin(time * 1.2) * 0.5 + 0.5;  // Onda media
    float breathe = mix(pulse1, pulse2, 0.5);    // Mezcla suave
    breathe = 0.75 + breathe * 0.25;             // Rango: 0.75 - 1.0

    // ═══════════════════════════════════════════════════════════
    // ✨ DESTELLO OCASIONAL (cada 9 segundos)
    // ═══════════════════════════════════════════════════════════
    float sparkleInterval = 9.0;  // Intervalo entre destellos
    float sparkleTime = mod(time + 2.0, sparkleInterval);  // Desfase único
    float sparkle = 1.0;  // Sin destello por defecto

    // Destello dura 0.4 segundos
    if (sparkleTime < 0.4) {
        // Fade in rápido y fade out rápido
        float t = sparkleTime / 0.4;
        sparkle = 1.0 + sin(t * 3.14159) * 1.8;  // Pico de 2.8x brillo
    }

    // ═══════════════════════════════════════════════════════════
    // 💎 NÚCLEO CENTRAL ROSA (crece y se encoge)
    // ═══════════════════════════════════════════════════════════
    float coreSize = 0.010 * breathe;  // Tamaño varía con la respiración

    if (dist < coreSize) {
        float coreIntensity = 1.0 - (dist / coreSize);
        coreIntensity = pow(coreIntensity, 0.8);

        // Brillo también pulsa + DESTELLO
        float coreBrightness = 0.9 + breathe * 0.3;

        magicColor += colorCore * coreIntensity * coreBrightness * sparkle;
    }

    // ═══════════════════════════════════════════════════════════
    // 🌟 HALO PÚRPURA SUAVE (también respira)
    // ═══════════════════════════════════════════════════════════
    float haloSize = 0.030 * breathe;  // Halo también crece/decrece

    if (dist < haloSize) {
        float glowIntensity = 1.0 - (dist / haloSize);
        glowIntensity = pow(glowIntensity, 2.5);

        // Brillo del halo pulsa suavemente + DESTELLO
        float haloBrightness = 0.3 + breathe * 0.15;

        magicColor += colorGlow * glowIntensity * haloBrightness * sparkle;
    }

    return magicColor;
}

// ════════════════════════════════════════════════════════════════════════
// 🌌 NÚCLEO GALÁCTICO (galaxias vivas con pulsación)
// ════════════════════════════════════════════════════════════════════════
vec3 drawGalacticCore(vec2 uv, vec2 pos, float time, float seed) {
    float dist = length(uv - pos);
    vec3 coreColor = vec3(0.0);

    // ═══════════════════════════════════════════════════════════
    // 🌈 CAMBIO DE COLOR GRADUAL PARA CADA ESTRELLA
    // ═══════════════════════════════════════════════════════════
    // Cada estrella tiene su propia paleta de 3 colores que van cambiando
    float colorPhase = time * 0.8;  // Velocidad rápida (ciclo cada 8 seg)

    // Definir 3 colores según el seed (cada estrella tiene colores únicos)
    vec3 color1, color2, color3;

    if (seed < 0.15) {
        // Estrella #1 (seed 0.1): Verde → Amarillo → Naranja
        color1 = vec3(0.0, 1.0, 0.3);       // Verde brillante
        color2 = vec3(1.0, 1.0, 0.0);       // Amarillo puro
        color3 = vec3(1.0, 0.5, 0.0);       // Naranja intenso
    } else if (seed < 0.25) {
        // Estrella #5 (seed 0.2): Naranja → Rojo → Magenta
        color1 = vec3(1.0, 0.5, 0.0);       // Naranja
        color2 = vec3(1.0, 0.0, 0.0);       // Rojo puro
        color3 = vec3(1.0, 0.0, 0.6);       // Magenta rosado
    } else if (seed < 0.55) {
        // Estrella #3 (seed 0.5): Rojo → Naranja → Amarillo
        color1 = vec3(1.0, 0.0, 0.2);       // Rojo intenso
        color2 = vec3(1.0, 0.6, 0.0);       // Naranja fuego
        color3 = vec3(1.0, 1.0, 0.2);       // Amarillo dorado
    } else if (seed < 0.75) {
        // Estrella #4 (seed 0.7): Azul → Cyan → Verde
        color1 = vec3(0.0, 0.3, 1.0);       // Azul eléctrico
        color2 = vec3(0.0, 1.0, 1.0);       // Cyan brillante
        color3 = vec3(0.2, 1.0, 0.5);       // Verde aqua
    } else {
        // Estrella #6 (seed 0.9): Verde → Cyan → Azul
        color1 = vec3(0.3, 1.0, 0.5);       // Verde lima
        color2 = vec3(0.0, 1.0, 0.8);       // Cyan verdoso
        color3 = vec3(0.2, 0.6, 1.0);       // Azul cielo
    }

    // Interpolación suave entre los 3 colores (igual que la estrella mágica)
    vec3 galaxyColorCore, galaxyColorInner, galaxyColorOuter, galaxyColorGlow;
    float phase = mod(colorPhase + seed * 2.0, 6.28);  // Desfase único por estrella

    if (phase < 2.09) {
        // Fase 1: Color1 → Color2
        float t = phase / 2.09;
        galaxyColorCore = mix(color1, color2, t);
    } else if (phase < 4.18) {
        // Fase 2: Color2 → Color3
        float t = (phase - 2.09) / 2.09;
        galaxyColorCore = mix(color2, color3, t);
    } else {
        // Fase 3: Color3 → Color1
        float t = (phase - 4.18) / 2.10;
        galaxyColorCore = mix(color3, color1, t);
    }

    // Todos los halos usan el mismo color para máxima visibilidad
    galaxyColorInner = galaxyColorCore;
    galaxyColorOuter = galaxyColorCore;
    galaxyColorGlow = galaxyColorCore;

    // ═══════════════════════════════════════════════════════════
    // ⏱️ VELOCIDAD DE PULSACIÓN VARIABLE (cada galaxia tiene su ritmo)
    // ═══════════════════════════════════════════════════════════
    // Algunas pulsan más rápido (activas), otras más lento (tranquilas)
    float pulseSpeed = 0.6 + seed * 0.5;  // Rango: 0.6 - 1.1 (varía según seed)
    float pulse = sin(time * pulseSpeed + seed * 6.28) * 0.15 + 0.85;  // 0.7 - 1.0

    // ═══════════════════════════════════════════════════════════
    // ✨ DESTELLO OCASIONAL (cada 9 segundos, desfasado por seed)
    // ═══════════════════════════════════════════════════════════
    float sparkleInterval = 9.0;  // Intervalo entre destellos
    float sparkleTime = mod(time + seed * 5.0, sparkleInterval);  // Desfase único por estrella
    float sparkle = 1.0;  // Sin destello por defecto

    // Destello dura 0.4 segundos
    if (sparkleTime < 0.4) {
        // Fade in rápido y fade out rápido
        float t = sparkleTime / 0.4;
        sparkle = 1.0 + sin(t * 3.14159) * 1.8;  // Pico de 2.8x brillo
    }

    // ═══════════════════════════════════════════════════════════
    // 🔥 NÚCLEO CENTRAL MUY BRILLANTE
    // ═══════════════════════════════════════════════════════════
    float coreSize = 0.005;  // 🔧 TAMAÑO del núcleo
    if (dist < coreSize) {
        float coreIntensity = (1.0 - dist / coreSize) * pulse;
        coreIntensity = pow(coreIntensity, 0.8);
        coreColor += galaxyColorCore * coreIntensity * 1.5 * sparkle;  // 🔧 BRILLO + DESTELLO
    }

    // ═══════════════════════════════════════════════════════════
    // ✨ HALO INTERNO
    // ═══════════════════════════════════════════════════════════
    float haloSize1 = 0.012;  // 🔧 TAMAÑO del halo interno
    if (dist < haloSize1) {
        float haloIntensity = (1.0 - dist / haloSize1) * pulse;
        haloIntensity = pow(haloIntensity, 1.5);
        coreColor += galaxyColorInner * haloIntensity * 0.7 * sparkle;  // 🔧 BRILLO + DESTELLO
    }

    // ═══════════════════════════════════════════════════════════
    // 🌟 HALO EXTERNO
    // ═══════════════════════════════════════════════════════════
    float haloSize2 = 0.0275;  // 🔧 TAMAÑO del halo externo
    if (dist < haloSize2) {
        float haloIntensity = (1.0 - dist / haloSize2) * pulse;
        haloIntensity = pow(haloIntensity, 2.0);
        coreColor += galaxyColorOuter * haloIntensity * 0.35 * sparkle;  // 🔧 BRILLO + DESTELLO
    }

    // ═══════════════════════════════════════════════════════════
    // 💫 RESPLANDOR MUY DIFUSO
    // ═══════════════════════════════════════════════════════════
    float glowSize = 0.05;  // 🔧 TAMAÑO del resplandor difuso
    if (dist < glowSize) {
        float glowIntensity = (1.0 - dist / glowSize) * pulse;
        glowIntensity = pow(glowIntensity, 3.0);
        coreColor += galaxyColorGlow * glowIntensity * 0.15 * sparkle;  // 🔧 BRILLO + DESTELLO
    }

    return coreColor;
}

// ════════════════════════════════════════════════════════════════════════
// 🔍 DETECCIÓN DE BORDES (Edge Detection) - Filtro Sobel
// ════════════════════════════════════════════════════════════════════════
// Detecta los contornos de las galaxias y nebulosas del fondo
// Retorna la intensidad del borde detectado (0.0 = sin borde, 1.0 = borde fuerte)

float detectEdges(sampler2D tex, vec2 uv, vec2 resolution) {
    // Tamaño de 1 píxel en coordenadas UV
    vec2 texelSize = 1.0 / resolution;

    // ═══════════════════════════════════════════════════════════
    // 📐 MUESTREAR 9 PÍXELES (3x3 grid) ALREDEDOR DEL PÍXEL ACTUAL
    // ═══════════════════════════════════════════════════════════
    // Layout del grid:
    // [tl] [tc] [tr]    (top-left, top-center, top-right)
    // [ml] [mc] [mr]    (middle-left, middle-center, middle-right)
    // [bl] [bc] [br]    (bottom-left, bottom-center, bottom-right)

    vec3 tl = texture2D(tex, uv + vec2(-texelSize.x,  texelSize.y)).rgb;  // Top-left
    vec3 tc = texture2D(tex, uv + vec2(0.0,           texelSize.y)).rgb;  // Top-center
    vec3 tr = texture2D(tex, uv + vec2( texelSize.x,  texelSize.y)).rgb;  // Top-right

    vec3 ml = texture2D(tex, uv + vec2(-texelSize.x,  0.0)).rgb;          // Middle-left
    vec3 mr = texture2D(tex, uv + vec2( texelSize.x,  0.0)).rgb;          // Middle-right

    vec3 bl = texture2D(tex, uv + vec2(-texelSize.x, -texelSize.y)).rgb;  // Bottom-left
    vec3 bc = texture2D(tex, uv + vec2(0.0,          -texelSize.y)).rgb;  // Bottom-center
    vec3 br = texture2D(tex, uv + vec2( texelSize.x, -texelSize.y)).rgb;  // Bottom-right

    // ═══════════════════════════════════════════════════════════
    // 🧮 OPERADORES SOBEL (detectan cambios de brillo)
    // ═══════════════════════════════════════════════════════════
    // Sobel X (gradiente horizontal):
    // [-1  0  +1]
    // [-2  0  +2]
    // [-1  0  +1]

    vec3 sobelX = -tl + tr - 2.0*ml + 2.0*mr - bl + br;

    // Sobel Y (gradiente vertical):
    // [-1 -2 -1]
    // [ 0  0  0]
    // [+1 +2 +1]

    vec3 sobelY = -tl - 2.0*tc - tr + bl + 2.0*bc + br;

    // ═══════════════════════════════════════════════════════════
    // 📊 CALCULAR MAGNITUD DEL GRADIENTE (intensidad del borde)
    // ═══════════════════════════════════════════════════════════
    // Combinamos gradientes horizontal y vertical
    vec3 edgeVector = sqrt(sobelX * sobelX + sobelY * sobelY);

    // Convertir a escala de grises (luminancia)
    float edgeStrength = dot(edgeVector, vec3(0.299, 0.587, 0.114));

    // FILTRO MÁS ESTRICTO - Solo detectar bordes GRANDES (galaxias, no estrellas)
    // Threshold más alto = ignora detalles pequeños
    edgeStrength = smoothstep(0.2, 0.5, edgeStrength);  // Solo bordes muy pronunciados

    return edgeStrength;
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

    // ═══════════════════════════════════════════════════════════════════════
    // ✨ EDGE GLOW - Resaltar bordes de galaxias y nebulosas
    // ═══════════════════════════════════════════════════════════════════════
    // Detectar bordes de la imagen de fondo usando filtro Sobel
    float edgeIntensity = detectEdges(u_Texture, v_TexCoord, u_Resolution);

    // Color del glow: AZUL-BLANCO BRILLANTE con transparencia
    vec3 edgeColor = vec3(0.85, 0.95, 1.0);  // Azul-blanco eléctrico brillante

    // Crear glow con intensidad reducida (50% menos)
    vec3 edgeGlow = edgeColor * edgeIntensity * 0.11;  // 11% de intensidad (50% reducido)

    // Agregar el glow con TRANSPARENCIA (mezcla suave en vez de suma directa)
    // mix() crea un efecto de overlay translúcido
    float edgeOpacity = edgeIntensity * 0.3;  // Opacidad basada en intensidad del borde
    backgroundTexture = mix(backgroundTexture, backgroundTexture + edgeGlow, edgeOpacity);

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

    // ═══════════════════════════════════════════════════════════════════════
    // 🌌 NÚCLEOS GALÁCTICOS VIVOS (en las posiciones marcadas por el usuario)
    // ═══════════════════════════════════════════════════════════════════════
    vec3 galacticCores = vec3(0.0);

    // Posiciones aproximadas basadas en la imagen (normalizadas 0-1)
    // Ajustadas para las galaxias visibles en el fondo
    galacticCores += drawGalacticCore(v_TexCoord, vec2(0.737, 0.485), u_Time, 0.1);  // #1 Arriba izquierda
    galacticCores += drawMagicalStarSimple(v_TexCoord, vec2(0.277, 0.342), u_Time);  // #2 ✨ ESTRELLA MÁGICA
    galacticCores += drawGalacticCore(v_TexCoord, vec2(0.125, 0.560), u_Time, 0.5);  // #3 Centro
    galacticCores += drawGalacticCore(v_TexCoord, vec2(0.567, 0.671), u_Time, 0.7);  // #4 Centro derecha
    galacticCores += drawGalacticCore(v_TexCoord, vec2(0.832, 0.721), u_Time, 0.2);  // #5 Abajo izquierda
    galacticCores += drawGalacticCore(v_TexCoord, vec2(0.252, 0.690), u_Time, 0.9);  // #6 Abajo derecha


    // Combinar: fondo + estrellas procedurales + estrellas fugaces + núcleos galácticos
    vec3 finalColor = backgroundTexture + twinklingStars + color + galacticCores;

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
