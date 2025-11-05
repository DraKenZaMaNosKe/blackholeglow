// ============================================
// archivo: forcefield_effects_fragment.glsl
// 🛡️ CAMPO DE FUERZA - CAPA DE EFECTOS DINÁMICOS
// Impactos + Grietas + Pulsos de energía
// Diseñado para compositing sobre el campo de fuerza base
// ============================================

#ifdef GL_ES
precision mediump float;
#endif

// ------- Uniforms -------
uniform float u_Time;
uniform vec2 u_Resolution;
uniform float u_Health;  // 0.0 - 1.0

// Impactos (hasta 8 simultáneos)
uniform vec3 u_ImpactPos[8];
uniform float u_ImpactIntensity[8];

// ------- Varyings -------
varying vec2 v_TexCoord;
varying vec3 v_WorldPos;

// ============================================
// 🔧 FUNCIONES DE RUIDO
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

float fbm(vec2 p, int octaves) {
    float value = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;

    for (int i = 0; i < 8; i++) {
        if (i >= octaves) break;
        value += amplitude * noise(p * frequency);
        frequency *= 2.0;
        amplitude *= 0.5;
    }
    return value;
}

// ============================================
// ⚡ PULSOS DE ENERGÍA (desde el centro)
// ============================================

vec3 energyPulses(vec2 uv, float time) {
    vec2 center = vec2(0.5, 0.5);
    float dist = length(uv - center);

    // Ondas concéntricas
    float wave1 = sin(dist * 20.0 - time * 3.0) * 0.5 + 0.5;
    float wave2 = sin(dist * 30.0 - time * 4.0) * 0.5 + 0.5;

    float waves = (wave1 + wave2) * 0.5;
    waves = pow(waves, 4.0);

    // Fade out hacia el borde
    float fadeOut = smoothstep(0.8, 0.3, dist);

    vec3 pulseColor = vec3(0.3, 0.9, 1.0);  // Azul cyan brillante

    return pulseColor * waves * fadeOut * 0.4;
}

// ============================================
// 💥 IMPACTOS ÉPICOS (ondas expansivas)
// ============================================

vec3 impactWaves(vec2 uv, float time) {
    vec3 impactGlow = vec3(0.0);

    for (int i = 0; i < 8; i++) {
        if (u_ImpactIntensity[i] > 0.0) {
            float impactDist = length(v_WorldPos - u_ImpactPos[i]);

            // Tiempo efectivo del impacto
            float effectiveTime = time * 12.0;

            // 🌊 MÚLTIPLES ONDAS EXPANSIVAS
            float wave1 = sin(impactDist * 25.0 - effectiveTime * 20.0) * 0.5 + 0.5;
            float wave2 = sin(impactDist * 35.0 - effectiveTime * 28.0) * 0.5 + 0.5;
            float wave3 = sin(impactDist * 45.0 - effectiveTime * 36.0) * 0.5 + 0.5;

            float waves = wave1 * 0.5 + wave2 * 0.3 + wave3 * 0.2;
            waves = pow(waves, 2.0);

            // Epicentro brillante
            float epicenter = smoothstep(0.25, 0.0, impactDist);

            // Decay del impacto
            float impactStrength = u_ImpactIntensity[i];

            // Color: azul brillante → blanco en el centro
            vec3 impactColor = mix(
                vec3(0.2, 0.8, 1.0),   // Azul cyan
                vec3(1.0, 1.0, 1.0),   // Blanco
                epicenter * 0.9
            );

            float totalImpact = (waves * 4.0 + epicenter * 6.0) * impactStrength;
            impactGlow += impactColor * totalImpact;
        }
    }

    return impactGlow;
}

// ============================================
// 🔥 GRIETAS (cuando está dañado)
// ============================================

vec3 damageGracks(vec2 uv, float time, float health) {
    float damage = 1.0 - health;

    // Solo visible cuando está dañado
    if (damage < 0.1) return vec3(0.0);

    vec3 cracks = vec3(0.0);

    // 6 grietas radiales desde diferentes puntos
    for (int i = 0; i < 6; i++) {
        float seed = float(i) * 43.758;

        // Origen de la grieta (punto de impacto antiguo)
        vec2 origin = vec2(
            noise(vec2(seed, 0.0)),
            noise(vec2(0.0, seed))
        );

        vec2 toOrigin = uv - origin;
        float distToOrigin = length(toOrigin);

        // Grieta solo visible cerca del origen
        if (distToOrigin > damage * 0.6) continue;

        // Patrón de grieta
        float angle = atan(toOrigin.y, toOrigin.x);
        float crackPattern = noise(vec2(angle * 5.0, distToOrigin * 15.0));
        crackPattern = smoothstep(0.6, 0.65, crackPattern);

        // Línea de grieta
        float crackLine = smoothstep(0.02, 0.0, abs(crackPattern - 0.5));

        // Intensidad que pulsa
        float pulse = sin(time * 3.0 + float(i)) * 0.3 + 0.7;

        // Color rojo-naranja (daño)
        vec3 crackColor = mix(
            vec3(1.0, 0.3, 0.0),   // Naranja fuego
            vec3(1.0, 0.1, 0.0),   // Rojo intenso
            pulse
        );

        cracks += crackColor * crackLine * damage * pulse * 0.8;
    }

    return cracks;
}

// ============================================
// ✨ CHISPAS DE ENERGÍA (partículas)
// ============================================

vec3 energySparks(vec2 uv, float time) {
    vec3 sparks = vec3(0.0);

    // Chispas que flotan
    vec2 sparkUV = uv * 25.0;
    sparkUV += vec2(time * 0.5, time * 0.7);

    float sparkPattern = noise(sparkUV);
    sparkPattern = smoothstep(0.75, 0.78, sparkPattern);

    // Color cyan brillante
    vec3 sparkColor = vec3(0.5, 1.0, 1.0);

    return sparkColor * sparkPattern * 0.5;
}

// ============================================
// 🎵 MAIN - EFECTOS DINÁMICOS
// ============================================

void main() {
    vec2 uv = v_TexCoord;
    vec2 center = vec2(0.5, 0.5);
    float dist = length(uv - center);

    // Obtener health
    float health = u_Health;

    // ═══════════════════════════════════════════════════════════
    // 🛡️ EFECTOS DINÁMICOS DEL CAMPO DE FUERZA
    // ═══════════════════════════════════════════════════════════

    vec3 effects = vec3(0.0);

    // ⚡ Pulsos de energía (siempre activos)
    if (health > 0.05) {
        effects += energyPulses(uv, u_Time);
    }

    // 💥 Impactos (cuando hay golpes)
    effects += impactWaves(uv, u_Time);

    // 🔥 Grietas (cuando está dañado)
    effects += damageGracks(uv, u_Time, health);

    // ✨ Chispas de energía (cuando está activo)
    if (health > 0.05) {
        effects += energySparks(uv, u_Time);
    }

    // Calcular alpha
    float alpha = length(effects);
    alpha = clamp(alpha, 0.0, 1.0);

    gl_FragColor = vec4(effects, alpha);
}
