#ifdef GL_ES
precision mediump float;
#endif

// ============================================
// Fragment Shader - Campo de Fuerza PLASMA
// Efecto de lámpara de plasma con rayos eléctricos
// Optimizado para rendimiento + Reactivo a música
// ============================================

varying vec2 v_TexCoord;
varying vec3 v_WorldPos;

uniform float u_Time;
uniform sampler2D u_Texture;
uniform vec4 u_Color;
uniform float u_Alpha;
uniform vec3 u_ImpactPos[8];
uniform float u_ImpactIntensity[8];
uniform float u_Health;

// NUEVO: Uniforms para reactividad musical
uniform float u_MusicBass;      // 0.0-1.0 intensidad de graves
uniform float u_MusicTreble;    // 0.0-1.0 intensidad de agudos
uniform float u_MusicBeat;      // 0.0-1.0 intensidad de beat

// ============================================
// Funciones de ruido para rayos eléctricos
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

// Ruido fractal para rayos más complejos
float fbm(vec2 p, float time) {
    float value = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;

    for (int i = 0; i < 3; i++) {
        value += amplitude * noise(p * frequency + time * 0.3);
        frequency *= 2.0;
        amplitude *= 0.5;
    }
    return value;
}

// ============================================
// Generador de rayos eléctricos
// ============================================

float electricBolt(vec2 uv, vec2 start, vec2 end, float time, float seed) {
    // Calcular la dirección del rayo
    vec2 dir = end - start;
    float len = length(dir);
    dir = normalize(dir);

    // Calcular distancia perpendicular al rayo
    vec2 toPoint = uv - start;
    float alongRay = dot(toPoint, dir);

    // Solo dibujar si está dentro del rango del rayo
    if (alongRay < 0.0 || alongRay > len) return 0.0;

    // Distancia perpendicular con ruido para zigzag
    vec2 perpDir = vec2(-dir.y, dir.x);
    float perpDist = abs(dot(toPoint, perpDir));

    // Agregar zigzag con ruido (MUY sutil)
    float zigzag = fbm(vec2(alongRay * 12.0, seed), time * 2.0) * 0.015;
    perpDist = abs(perpDist - zigzag);

    // Intensidad del rayo (SUPER FINO - apenas visible)
    float boltWidth = 0.0008;  // Extremadamente delgado
    float intensity = smoothstep(boltWidth * 2.0, 0.0, perpDist);
    intensity = pow(intensity, 0.3); // Curva suave

    // Parpadeo MUY sutil
    float flicker = 0.5 + noise(vec2(time * 8.0, seed)) * 0.2;
    intensity *= flicker * 0.6;  // Reducir intensidad general

    return intensity;
}

// ============================================
// Sistema de rayos radiales (desde centro)
// ============================================

float plasmaRays(vec2 uv, float time, float musicIntensity) {
    float totalRays = 0.0;

    // Centro de la esfera
    vec2 center = vec2(0.5, 0.5);

    // POCOS rayos sutiles (3-5 rayos max)
    int numRays = 3 + int(musicIntensity * 2.0); // 3-5 rayos

    for (int i = 0; i < 10; i++) {
        if (i >= numRays) break;

        float seed = float(i) * 123.456;

        // Ángulo rotativo
        float angle = (float(i) / float(numRays)) * 6.28318 + time * 0.3;

        // Punto de inicio (cerca del centro)
        vec2 startPoint = center + vec2(cos(angle), sin(angle)) * 0.1;

        // Punto final (borde variable)
        float radius = 0.4 + noise(vec2(time * 0.5 + seed, 0.0)) * 0.15;
        vec2 endPoint = center + vec2(cos(angle), sin(angle)) * radius;

        // Generar el rayo
        float bolt = electricBolt(uv, startPoint, endPoint, time, seed);
        totalRays += bolt;
    }

    return totalRays;
}

// ============================================
// Efecto Fresnel para transparencia
// ============================================

float fresnel(vec3 worldPos) {
    float dist = length(worldPos);
    float f = smoothstep(0.0, 0.7, dist);
    return pow(f, 1.2);
}

// ============================================
// 🔷 PATRÓN HEXAGONAL ENERGÉTICO
// ============================================

// Función para calcular distancia al borde del hexágono más cercano
float hexagonPattern(vec2 uv, float scale) {
    // Coordenadas hexagonales (sistema oblicuo)
    vec2 r = vec2(1.0, 1.73);  // sqrt(3) para hexágonos regulares
    vec2 h = r * 0.5;

    vec2 a = mod(uv * scale, r) - h;
    vec2 b = mod(uv * scale - h, r) - h;

    vec2 gv = length(a) < length(b) ? a : b;

    // Distancia al centro del hexágono
    float d = length(gv);

    // Calcular distancia a los bordes (6 lados del hexágono)
    float angle = atan(gv.y, gv.x);
    float hexRadius = 0.5;
    float hexEdge = hexRadius * cos(3.14159 / 6.0) / cos(mod(angle, 3.14159 / 3.0) - 3.14159 / 6.0);

    // Distancia normalizada al borde
    return abs(d - hexEdge);
}

// Grid de líneas hexagonales brillantes
float hexagonalGrid(vec2 uv, float scale, float lineWidth) {
    float hexDist = hexagonPattern(uv, scale);

    // Líneas brillantes en los bordes
    float lines = smoothstep(lineWidth, lineWidth * 0.5, hexDist);

    return lines;
}

// ============================================
// 💥 SISTEMA DE GRIETAS (cuando está dañado)
// ============================================

float crackPattern(vec2 uv, float damage, float time) {
    if (damage < 0.1) return 0.0;  // No hay grietas si no está dañado

    float cracks = 0.0;

    // Múltiples grietas radiales desde diferentes puntos
    for (int i = 0; i < 6; i++) {
        float seed = float(i) * 43.758;

        // Punto de origen de la grieta (distribuido por la esfera)
        vec2 origin = vec2(
            noise(vec2(seed, 0.0)),
            noise(vec2(0.0, seed))
        );

        // Vector desde origen hasta punto actual
        vec2 toPoint = uv - origin;
        float dist = length(toPoint);
        float angle = atan(toPoint.y, toPoint.x);

        // Grieta principal con ramificaciones
        float crackWidth = 0.005 + damage * 0.01;  // Más anchas con más daño
        float crackLength = 0.3 + damage * 0.4;     // Más largas con más daño

        // Solo dibujar si está en rango
        if (dist < crackLength) {
            // Zigzag de la grieta
            float zigzag = fbm(vec2(dist * 20.0, seed), time * 0.5) * 0.02;
            float perpDist = abs(sin(angle * 3.0 + seed) * dist - zigzag);

            // Intensidad de la grieta (suavizada)
            float crackIntensity = smoothstep(crackWidth * 2.0, 0.0, perpDist);
            crackIntensity *= smoothstep(crackLength, crackLength * 0.5, dist);

            // Parpadeo sutil de las grietas
            crackIntensity *= 0.7 + noise(vec2(time * 3.0, seed)) * 0.3;

            cracks = max(cracks, crackIntensity * damage);
        }
    }

    return cracks;
}

// ============================================
// 🌊 PULSOS DE ENERGÍA DESDE EL CENTRO
// ============================================

float energyPulses(vec2 uv, float time) {
    vec2 center = vec2(0.5, 0.5);
    float dist = length(uv - center);

    // Múltiples ondas que se expanden desde el centro
    float wave1 = sin(dist * 15.0 - time * 2.0) * 0.5 + 0.5;
    float wave2 = sin(dist * 20.0 - time * 2.5) * 0.5 + 0.5;

    // Combinar ondas
    float waves = (wave1 + wave2) * 0.5;

    // Solo visible en ciertas distancias (anillos)
    waves = pow(waves, 3.0);

    // Desvanecer hacia los bordes
    float fadeOut = smoothstep(0.7, 0.3, dist);

    return waves * fadeOut * 0.3;
}

// ============================================
// MAIN SHADER
// ============================================

void main() {
    // ===== CONFIGURACIÓN BASE =====
    vec3 baseColor = u_Color.rgb;
    vec2 uv = v_TexCoord;

    // ===== REACTIVIDAD MUSICAL =====
    // Agudos → Más rayos y velocidad
    float trebleBoost = u_MusicTreble * 0.8;
    // Graves → Intensidad y grosor de rayos
    float bassBoost = u_MusicBass * 0.6;
    // Beats → Destellos repentinos
    float beatFlash = u_MusicBeat * 1.2;

    float musicIntensity = (trebleBoost + bassBoost + beatFlash) / 2.0;
    musicIntensity = clamp(musicIntensity, 0.0, 1.0);

    // ===== TIEMPO ACELERADO POR MÚSICA =====
    float effectiveTime = u_Time * (1.0 + musicIntensity * 0.5);

    // ===== RAYOS ELÉCTRICOS (MUY SUTILES) =====
    // NO mostrar rayos si el campo está destruido
    float rays = 0.0;
    if (u_Health > 0.05) {  // Solo si tiene vida
        rays = plasmaRays(uv, effectiveTime, musicIntensity);

        // Reducir intensidad general (apenas visibles)
        rays *= 0.25;  // Solo 25% de intensidad

        // Aumentar SUTILMENTE con música
        rays *= (1.0 + musicIntensity * 0.3);

        // Desaparecer gradualmente con el daño
        rays *= u_Health;  // Se reduce conforme pierde vida
    }

    // ===== BRILLO BASE DEL PLASMA =====
    // Nebulosa de fondo
    float nebula = fbm(uv * 4.0, effectiveTime * 0.2) * 0.2;

    // Pulsación reactiva a música
    float pulse = 0.6 + sin(effectiveTime * 1.5) * 0.2;
    pulse += bassBoost * 0.3; // Graves aumentan pulsación

    // ===== FRESNEL (más brillante en bordes) =====
    float fresnelEffect = fresnel(v_WorldPos);

    // ===== COMBINAR EFECTOS (menos brillante) =====
    float brightness = nebula + rays * 0.6 + pulse * 0.2;
    brightness *= (0.3 + fresnelEffect * 0.9);

    // Destello de beat MUY SUTIL
    brightness += beatFlash * 0.15;

    // ===== COLOR DE RAYOS (SIN blanco, solo color del campo) =====
    // Usar principalmente el color base del campo
    vec3 electricColor = mix(
        vec3(0.4, 0.7, 1.0),  // Azul eléctrico suave
        baseColor,
        0.7  // 70% color del campo
    );

    // Rayos SUTILES (sin blanco brillante)
    vec3 rayColor = mix(electricColor, baseColor * 1.3, rays * 0.4);

    vec3 finalColor = rayColor * brightness;

    // ===== EFECTO DE SALUD (rojo cuando está dañado) =====
    if (u_Health < 1.0) {
        float damage = 1.0 - u_Health;
        vec3 damageColor = vec3(1.0, 0.2, 0.3);
        finalColor = mix(finalColor, damageColor, damage * 0.4);
    }

    // ═══════════════════════════════════════════════════════════════
    // 💥 EFECTOS DE IMPACTO ÉPICOS (Ondas expansivas dramáticas)
    // ═══════════════════════════════════════════════════════════════
    vec3 impactGlow = vec3(0.0);
    float impactAlphaBoost = 0.0;

    for (int i = 0; i < 8; i++) {
        if (u_ImpactIntensity[i] > 0.0 && u_Health > 0.05) {  // Solo si hay vida
            float impactDist = length(v_WorldPos - u_ImpactPos[i]);
            float impactRadius = 0.9;  // Radio más grande para ondas más visibles

            if (impactDist < impactRadius) {
                float impactStrength = (1.0 - (impactDist / impactRadius)) * u_ImpactIntensity[i];
                impactStrength = pow(impactStrength, 1.2);

                // 🌊 MÚLTIPLES ONDAS EXPANSIVAS CONCÉNTRICAS (muy épicas)
                float wave1 = sin(impactDist * 18.0 - effectiveTime * 18.0) * 0.5 + 0.5;
                float wave2 = sin(impactDist * 28.0 - effectiveTime * 24.0) * 0.5 + 0.5;
                float wave3 = sin(impactDist * 38.0 - effectiveTime * 30.0) * 0.5 + 0.5;

                // Combinar ondas con diferentes intensidades
                float waves = wave1 * 0.5 + wave2 * 0.3 + wave3 * 0.2;
                waves = pow(waves, 1.8);  // Ondas más definidas y brillantes

                // ⚡ FLASH CENTRAL (epicentro del impacto MUY brillante)
                float epicenter = smoothstep(0.2, 0.0, impactDist);

                // 🔥 COLOR DEL IMPACTO: Azul-blanco eléctrico intenso
                vec3 impactColor = mix(
                    vec3(0.2, 0.8, 1.0),   // Azul cyan eléctrico
                    vec3(1.0, 1.0, 1.0),   // Blanco puro brillante
                    epicenter * 0.9        // Centro casi blanco
                );

                // Intensidad combinada (ondas + epicentro)
                float totalImpact = (waves * 3.0 + epicenter * 4.0) * impactStrength;

                impactGlow += impactColor * totalImpact;
                impactAlphaBoost += totalImpact * 0.7;
            }
        }
    }

    finalColor += impactGlow;

    // ═══════════════════════════════════════════════════════════════
    // 🔷 HEXÁGONOS ENERGÉTICOS (Azul eléctrico sci-fi)
    // ═══════════════════════════════════════════════════════════════

    // Escala de los hexágonos (reactiva a música)
    float hexScale = 8.0 + musicIntensity * 2.0;  // Más hexágonos con música

    // Generar grid hexagonal con líneas brillantes
    float hexGrid = hexagonalGrid(uv, hexScale, 0.08);

    // Pulsación de las líneas (sutil)
    float hexPulse = 0.6 + sin(effectiveTime * 2.0) * 0.2 + bassBoost * 0.3;
    hexGrid *= hexPulse;

    // 🔷 COLOR AZUL ELÉCTRICO BRILLANTE
    vec3 hexColor = vec3(0.2, 0.7, 1.0);  // Azul cyan brillante
    vec3 hexGlow = vec3(0.4, 0.9, 1.0);   // Azul claro brillante para bordes

    // Líneas hexagonales brillantes
    vec3 hexagonPattern = mix(hexColor * 0.3, hexGlow, hexGrid);

    // Intensificar con música
    hexagonPattern *= (1.0 + musicIntensity * 0.5);

    // Añadir brillo extra en impactos (hexágonos se iluminan)
    if (length(impactGlow) > 0.1) {
        // Los hexágonos brillan blanco-azul en impactos
        hexagonPattern = mix(hexagonPattern, vec3(0.8, 1.0, 1.0), length(impactGlow) * 0.4);
    }

    // Combinar hexágonos con el color base
    finalColor = mix(finalColor, hexagonPattern, 0.7);

    // ═══════════════════════════════════════════════════════════════
    // 💥 GRIETAS CUANDO ESTÁ DAÑADO (efecto dramático)
    // ═══════════════════════════════════════════════════════════════
    float damage = 1.0 - u_Health;  // 0.0 = sin daño, 1.0 = destruido
    float cracks = crackPattern(uv, damage, effectiveTime);

    if (cracks > 0.0) {
        // Grietas rojas brillantes (peligro!)
        vec3 crackColor = vec3(1.0, 0.2, 0.1);  // Rojo-naranja intenso
        vec3 crackGlow = vec3(1.0, 0.4, 0.2);   // Brillo naranja

        // Mezclar color de grieta según intensidad
        vec3 crackFinal = mix(crackColor, crackGlow, cracks * 0.5);

        // Añadir grietas al color final (muy visibles)
        finalColor = mix(finalColor, crackFinal * 2.0, cracks * 0.8);
    }

    // ═══════════════════════════════════════════════════════════════
    // 🌊 PULSOS DE ENERGÍA DESDE EL CENTRO (campo generándose)
    // ═══════════════════════════════════════════════════════════════
    float pulses = energyPulses(uv, effectiveTime);

    // Pulsos más intensos con música
    pulses *= (1.0 + musicIntensity * 0.4);

    // Añadir pulsos azules brillantes
    vec3 pulseColor = vec3(0.3, 0.9, 1.0);  // Azul cyan eléctrico
    finalColor += pulseColor * pulses * 1.5;

    // ═══════════════════════════════════════════════════════════════
    // 🔷 ALPHA FINAL - HEXÁGONOS VISIBLES CON TRANSPARENCIA
    // ═══════════════════════════════════════════════════════════════
    // Base moderadamente transparente para ver los hexágonos
    float finalAlpha = u_Alpha * 0.15; // 15% visible base

    // HEXÁGONOS - Alpha según las líneas del grid
    finalAlpha += hexGrid * 0.35; // Líneas hexagonales visibles (35% en líneas)

    // BORDES con efecto fresnel
    finalAlpha += fresnelEffect * 0.25; // Bordes brillantes (25%)

    // Rayos eléctricos sutiles
    finalAlpha += rays * 0.02;

    // 💥 GRIETAS - MUY VISIBLES cuando está dañado
    finalAlpha += cracks * 0.6; // Grietas muy opacas (60%)

    // 🌊 PULSOS DE ENERGÍA - Visibles
    finalAlpha += pulses * 0.3; // Pulsos visibles (30%)

    // IMPACTOS - SUPER BRILLANTES Y VISIBLES
    finalAlpha += impactAlphaBoost * 3.5; // Impactos épicos

    // Música aumenta visibilidad
    finalAlpha += musicIntensity * 0.08;

    // ===== SALIDA =====
    finalColor = clamp(finalColor, 0.0, 3.0); // Permitir mucho brillo en impactos
    finalAlpha = clamp(finalAlpha, 0.0, 0.95); // Máximo 95% en impactos

    gl_FragColor = vec4(finalColor, finalAlpha);
}
