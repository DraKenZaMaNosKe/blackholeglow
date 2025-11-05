// ═══════════════════════════════════════════════════════════════════════════════
// 🌊 SOLAR WINDS - Fragment Shader
// ═══════════════════════════════════════════════════════════════════════════════
// Shader para vientos solares con nubes cósmicas procedurales
// Basado en técnicas del Book of Shaders - Capítulo de Noise
// ═══════════════════════════════════════════════════════════════════════════════

#ifdef GL_ES
precision mediump float;
#endif

// Uniforms
uniform vec2 u_Resolution;
uniform float u_Time;

// Varyings del vertex shader
varying vec2 v_TexCoord;

// ═══════════════════════════════════════════════════════════════════════════════
// 🎲 FUNCIONES DE NOISE (Book of Shaders)
// ═══════════════════════════════════════════════════════════════════════════════

// 2D Random (pseudo-random)
float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

// 2D Noise (Morgan McGuire - Shader Toy style)
float noise(vec2 st) {
    vec2 i = floor(st);
    vec2 f = fract(st);

    // Four corners in 2D of a tile
    float a = random(i);
    float b = random(i + vec2(1.0, 0.0));
    float c = random(i + vec2(0.0, 1.0));
    float d = random(i + vec2(1.0, 1.0));

    // Smooth Interpolation using Cubic Hermine Curve (same as SmoothStep)
    vec2 u = f * f * (3.0 - 2.0 * f);

    // Mix 4 corners percentages
    return mix(a, b, u.x) +
           (c - a) * u.y * (1.0 - u.x) +
           (d - b) * u.x * u.y;
}

// Fractal Brownian Motion (FBM) - Múltiples octavas de noise
float fbm(vec2 st) {
    float value = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;

    // 4 octavas de noise para detalles complejos
    for (int i = 0; i < 4; i++) {
        value += amplitude * noise(st * frequency);
        frequency *= 2.0;
        amplitude *= 0.5;
    }

    return value;
}

// ═══════════════════════════════════════════════════════════════════════════════
// 🌊 VIENTOS SOLARES - Función principal
// ═══════════════════════════════════════════════════════════════════════════════

vec4 getSolarWinds(vec2 uv, float time) {
    // ========================================
    // 1️⃣ PREPARAR COORDENADAS
    // ========================================

    // Escalar UV para tener más "tiles" de nubes
    vec2 pos = uv * 3.0;

    // ========================================
    // 2️⃣ MOVIMIENTO HORIZONTAL (flujo)
    // ========================================

    // Movimiento principal de izquierda a derecha
    pos.x += time * 0.08;  // Velocidad lenta y constante

    // Añadir distorsión vertical sutil (ondulación)
    pos.y += sin(pos.x * 2.0 + time * 0.3) * 0.1;

    // ========================================
    // 3️⃣ GENERAR NUBES CON FBM
    // ========================================

    // Capa principal de nubes
    float clouds = fbm(pos);

    // Capa secundaria con movimiento ligeramente diferente
    vec2 pos2 = pos * 1.5;
    pos2.x += time * 0.05;  // Más lento que la capa principal
    float clouds2 = fbm(pos2) * 0.5;

    // Combinar capas
    float density = clouds * 0.7 + clouds2 * 0.3;

    // ========================================
    // 4️⃣ AJUSTAR RANGO (contraste)
    // ========================================

    // ✅ DEBUG: Rango más visible (menos umbral)
    density = smoothstep(0.1, 0.6, density);  // Más nubes visibles

    // ========================================
    // 5️⃣ COLORES DE VIENTOS SOLARES
    // ========================================

    // ✅ DEBUG: COLORES MUY BRILLANTES para máxima visibilidad
    vec3 color1 = vec3(1.5, 1.2, 0.5);   // Amarillo súper brillante
    vec3 color2 = vec3(1.5, 0.8, 0.2);   // Naranja intenso brillante
    vec3 color3 = vec3(1.5, 0.5, 0.1);   // Naranja rojizo brillante

    // Mezclar colores basado en densidad
    vec3 finalColor = mix(color1, color2, density);
    finalColor = mix(finalColor, color3, density * density);

    // ✅ DEBUG: BRILLO EXTRA aumentado
    finalColor += vec3(1.5, 1.3, 1.0) * pow(density, 3.0) * 1.5;  // Mucho más brillo

    // ========================================
    // 6️⃣ TRANSPARENCIA (alpha)
    // ========================================

    // ✅ DEBUG: ALPHA AL MÁXIMO para máxima visibilidad
    float alpha = density * 1.0;  // 1.0 = completamente opaco

    // Variación de alpha en el tiempo (pulso sutil)
    float pulse = sin(time * 0.5) * 0.1 + 0.9;  // 0.8 - 1.0
    alpha *= pulse;

    return vec4(finalColor, alpha);
}

// ═══════════════════════════════════════════════════════════════════════════════
// 🎬 MAIN
// ═══════════════════════════════════════════════════════════════════════════════

void main() {
    vec2 uv = v_TexCoord;

    // ═══════════════════════════════════════════════════════════════════
    // 🌊 VIENTOS SOLARES - Flujo orgánico horizontal
    // ═══════════════════════════════════════════════════════════════════

    // 1️⃣ Movimiento vertical (de arriba hacia abajo después de rotación)
    float time = u_Time * 0.08;  // Velocidad lenta

    // Múltiples capas de flujo
    float y1 = uv.y * 3.0 - time;        // Capa 1 (rápida)
    float y2 = uv.y * 2.0 - time * 0.5;  // Capa 2 (lenta)
    float y3 = uv.y * 4.0 - time * 1.5;  // Capa 3 (muy rápida)

    // Ondulación horizontal (variación en X)
    float wave1 = sin(uv.x * 8.0 + time) * 0.2;
    float wave2 = sin(uv.x * 5.0 - time * 0.7) * 0.15;

    y1 += wave1;
    y2 += wave2;

    // 2️⃣ Crear patrones suaves (no rayas duras)
    float flow1 = sin(y1 * 6.28318);  // Seno suave
    float flow2 = sin(y2 * 6.28318);
    float flow3 = sin(y3 * 6.28318);

    // Convertir a rango 0-1 y suavizar
    flow1 = flow1 * 0.5 + 0.5;
    flow2 = flow2 * 0.5 + 0.5;
    flow3 = flow3 * 0.5 + 0.5;

    // Aplicar smoothstep para suavizar más
    flow1 = smoothstep(0.3, 0.7, flow1);
    flow2 = smoothstep(0.2, 0.8, flow2);
    flow3 = smoothstep(0.4, 0.6, flow3);

    // Combinar capas
    float pattern = flow1 * 0.5 + flow2 * 0.3 + flow3 * 0.2;

    // 3️⃣ Colores sutiles naranjas/dorados
    vec3 color1 = vec3(1.0, 0.7, 0.3);   // Naranja suave
    vec3 color2 = vec3(1.0, 0.5, 0.15);  // Naranja más intenso
    vec3 color3 = vec3(0.9, 0.6, 0.2);   // Tono medio

    vec3 finalColor = mix(color1, color2, pattern);
    finalColor = mix(finalColor, color3, flow3 * 0.5);

    // Brillo sutil
    finalColor += vec3(1.0, 0.8, 0.4) * pow(pattern, 2.0) * 0.3;

    // 4️⃣ Alpha más orgánico y variable
    float alpha = pattern * 0.5;  // Base 50% opacidad

    // Variación de opacidad basada en posición
    alpha *= mix(0.5, 1.0, sin(uv.x * 3.0 + time) * 0.5 + 0.5);

    // Fade suave en bordes
    alpha *= smoothstep(0.0, 0.15, uv.x) * smoothstep(1.0, 0.85, uv.x);
    alpha *= smoothstep(0.0, 0.2, uv.y) * smoothstep(1.0, 0.8, uv.y);

    gl_FragColor = vec4(finalColor, alpha);
}
