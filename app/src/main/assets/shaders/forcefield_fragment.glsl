#ifdef GL_ES
precision mediump float;
#endif

// ============================================
// Fragment Shader - Campo de Fuerza ETÉREO
// Diseñado para ser hermoso, translúcido y mágico
// ============================================

varying vec2 v_TexCoord;
varying vec3 v_WorldPos;

uniform float u_Time;
uniform sampler2D u_Texture;
uniform vec4 u_Color;
uniform float u_Alpha;
uniform vec3 u_ImpactPos[8];  // Posiciones de impactos
uniform float u_ImpactIntensity[8];  // Intensidad de cada impacto
uniform float u_Health;  // 0.0 a 1.0

// ============================================
// Funciones de ruido para efectos suaves
// ============================================

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));

    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

// Ruido multicapa suave
float smoothNoise(vec2 p, float time) {
    float n = noise(p + time * 0.1);
    n += noise(p * 2.0 - time * 0.05) * 0.5;
    n += noise(p * 4.0 + time * 0.08) * 0.25;
    return n / 1.75;
}

void main() {
    // ============================================
    // CONFIGURACIÓN BASE
    // ============================================

    vec3 baseColor = u_Color.rgb;
    float baseAlpha = u_Alpha;

    // Coordenadas UV ajustadas
    vec2 uv = v_TexCoord;

    // Distancia desde el centro de la esfera (para efectos radiales)
    float distFromCenter = length(v_WorldPos);

    // ============================================
    // EFECTO FRESNEL - Más transparente al centro
    // ============================================

    // Simulación de Fresnel: más brillante/visible en los bordes
    float fresnel = smoothstep(0.0, 0.6, distFromCenter);
    fresnel = pow(fresnel, 1.5);  // Curva de intensidad

    // ============================================
    // CAPAS DE ENERGÍA ONDULANTES
    // ============================================

    // Capa 1: Ondas verticales suaves
    vec2 wave1UV = uv * 6.0;
    wave1UV.x += sin(uv.y * 10.0 + u_Time * 0.4) * 0.15;
    float wave1 = smoothNoise(wave1UV, u_Time) * 0.4;

    // Capa 2: Ondas horizontales lentas
    vec2 wave2UV = uv * 8.0;
    wave2UV.y += cos(uv.x * 12.0 - u_Time * 0.3) * 0.12;
    float wave2 = smoothNoise(wave2UV, u_Time * 0.7) * 0.3;

    // Capa 3: Nebulosa de partículas
    vec2 nebulaUV = uv * 12.0 + vec2(u_Time * 0.05, -u_Time * 0.03);
    float nebula = smoothNoise(nebulaUV, u_Time * 0.5) * 0.25;

    // Combinar capas
    float energyPattern = wave1 + wave2 + nebula;
    energyPattern = smoothstep(0.2, 0.8, energyPattern);

    // ============================================
    // VETAS DE ENERGÍA (rayos sutiles)
    // ============================================

    float rays = 0.0;
    for (float i = 0.0; i < 2.0; i++) {
        float angle = u_Time * 0.2 + i * 3.14159;
        vec2 rayDir = vec2(cos(angle), sin(angle));
        float rayPattern = sin((dot(uv - 0.5, rayDir) * 15.0 + u_Time * 0.5)) * 0.5 + 0.5;
        rayPattern = pow(rayPattern, 8.0);  // Rayos muy finos
        rays += rayPattern * 0.15;
    }

    // ============================================
    // PULSACIÓN RESPIRATORIA
    // ============================================

    float pulse = 0.88 + sin(u_Time * 0.6) * 0.12;  // Pulsación lenta y sutil

    // ============================================
    // COMBINAR EFECTOS
    // ============================================

    // Brillo base (muy suave)
    float brightness = 0.3 + energyPattern * 0.4 + rays;
    brightness *= pulse;
    brightness *= (0.5 + fresnel * 1.5);  // Más brillo en los bordes

    // Color final con brillo
    vec3 finalColor = baseColor * brightness;

    // ============================================
    // EFECTO DE SALUD (daño → rojizo)
    // ============================================

    if (u_Health < 1.0) {
        float damage = 1.0 - u_Health;
        vec3 damageColor = vec3(1.0, 0.2, 0.3);
        finalColor = mix(finalColor, damageColor, damage * 0.35);
    }

    // ============================================
    // EFECTOS DE IMPACTO - SIMPLIFICADOS Y LIMPIOS
    // ============================================

    vec3 impactGlow = vec3(0.0);
    float impactAlphaBoost = 0.0;

    for (int i = 0; i < 8; i++) {
        if (u_ImpactIntensity[i] > 0.0) {
            float impactDist = length(v_WorldPos - u_ImpactPos[i]);
            float impactRadius = 0.35;  // Radio más pequeño para impactos más concentrados

            if (impactDist < impactRadius) {
                // Gradiente suave desde el centro del impacto
                float impactStrength = (1.0 - (impactDist / impactRadius)) * u_ImpactIntensity[i];
                impactStrength = pow(impactStrength, 2.5);  // Curva más pronunciada = bordes más difusos

                // Destello simple sin ondas complejas (elimina manchas)
                vec3 impactColor = baseColor * 1.2 + vec3(1.0) * 0.3;  // Color azul eléctrico brillante
                impactGlow += impactColor * impactStrength * 0.8;

                // Aumentar alpha solo en el punto de impacto
                impactAlphaBoost += impactStrength * 0.9;
            }
        }
    }

    finalColor += impactGlow;

    // ============================================
    // ALPHA FINAL - EXTREMADAMENTE TRANSPARENTE
    // ============================================

    // Alpha base EXTREMADAMENTE reducido (prácticamente invisible sin impactos)
    float finalAlpha = baseAlpha * 0.003;  // Solo 0.3% del alpha → casi imperceptible

    // Fresnel ULTRA sutil solo en los bordes (apenas visible)
    finalAlpha *= (0.01 + fresnel * 0.05);  // Extremadamente transparente

    // Los impactos son lo ÚNICO visible
    finalAlpha += impactAlphaBoost;

    // ============================================
    // SALIDA FINAL
    // ============================================

    // Suavizar el color para evitar bandas duras
    finalColor = pow(finalColor, vec3(0.95));  // Gamma suave

    // Clamp para evitar valores fuera de rango
    finalColor = clamp(finalColor, 0.0, 1.5);
    finalAlpha = clamp(finalAlpha, 0.0, 1.0);

    gl_FragColor = vec4(finalColor, finalAlpha);
}
