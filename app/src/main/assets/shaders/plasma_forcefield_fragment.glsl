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

    // ===== EFECTOS DE IMPACTO (SUTILES) =====
    vec3 impactGlow = vec3(0.0);
    float impactAlphaBoost = 0.0;

    for (int i = 0; i < 8; i++) {
        if (u_ImpactIntensity[i] > 0.0 && u_Health > 0.05) {  // Solo si hay vida
            float impactDist = length(v_WorldPos - u_ImpactPos[i]);
            float impactRadius = 0.5;

            if (impactDist < impactRadius) {
                float impactStrength = (1.0 - (impactDist / impactRadius)) * u_ImpactIntensity[i];
                impactStrength = pow(impactStrength, 1.5);

                // Onda expansiva SUTIL
                float wave = sin(impactDist * 30.0 - effectiveTime * 15.0) * 0.5 + 0.5;
                wave = pow(wave, 3.0);

                // Resplandor del impacto (color del campo, no blanco)
                vec3 impactColor = mix(baseColor * 1.5, vec3(1.0, 0.9, 0.6), 0.3);
                impactGlow += impactColor * impactStrength * wave * 0.8;  // Reducido
                impactAlphaBoost += impactStrength * 0.4;  // Reducido
            }
        }
    }

    finalColor += impactGlow;

    // ===== GRADIENTE DE COLOR (verde centro → cyan bordes) =====
    vec3 gradientColor = mix(
        vec3(0.2, 0.8, 0.5),  // Verde en el centro
        vec3(0.2, 0.6, 1.0),  // Cyan en los bordes
        fresnelEffect
    );

    // Mezclar gradiente con el color final
    finalColor = mix(finalColor, gradientColor, 0.5);

    // ===== ALPHA FINAL (MÁS TRANSPARENTE) =====
    float finalAlpha = u_Alpha * 0.45; // Base MUY transparente (45% vs 70%)
    finalAlpha *= (0.15 + fresnelEffect * 0.85); // Casi invisible al centro
    finalAlpha += rays * 0.15; // Rayos sutiles
    finalAlpha += impactAlphaBoost * 0.7;
    finalAlpha += musicIntensity * 0.1; // Música aumenta visibilidad sutilmente

    // ===== SALIDA =====
    finalColor = clamp(finalColor, 0.0, 2.0); // Permitir brillo extra
    finalAlpha = clamp(finalAlpha, 0.0, 0.8); // Máximo 80% alpha

    gl_FragColor = vec4(finalColor, finalAlpha);
}
