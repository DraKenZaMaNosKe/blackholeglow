// Fragment Shader para EstrelaBailarina
// Estrella espectacular que BRILLA y DESTELLA con la música

#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_TexCoord;
varying vec3 v_WorldPos;

uniform float u_Time;
uniform sampler2D u_Texture;
uniform vec4 u_SolidColor;
uniform float u_Alpha;

// Uniforms especiales para música
uniform float u_BassBoost;      // Nivel de bajos (0-1)
uniform float u_BeatPulse;      // Pulso de beat (0-1)
uniform float u_MusicEnergy;    // Energía musical general (0-1)

// Función de ruido para partículas
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

void main() {
    vec2 uv = v_TexCoord;
    vec2 center = vec2(0.5, 0.5);
    vec2 toCenter = uv - center;
    float dist = length(toCenter);
    float angle = atan(toCenter.y, toCenter.x);

    // COLOR BASE: Blanco brillante con tintes de colores musicales
    vec3 baseColor = vec3(1.0, 0.95, 0.85);  // Blanco cálido

    // ========== EFECTO DE BAJOS: PULSO Y EXPANSIÓN ==========
    float bassEffect = u_BassBoost * 2.0;  // Amplificar bajos

    // Ondas expansivas con los bajos
    float bassWaves = sin(dist * 15.0 - u_Time * 3.0 - bassEffect * 5.0) * 0.5 + 0.5;
    bassWaves = pow(bassWaves, 3.0) * bassEffect;

    // Brillo pulsante con bajos
    float bassPulse = 1.0 + bassEffect * 0.8;

    // ========== EFECTO DE BEATS: DESTELLO EXPLOSIVO ==========
    float beatFlash = u_BeatPulse * 3.0;  // Destellos intensos

    // Flash radial desde el centro
    float beatRays = 0.0;
    for (float i = 0.0; i < 6.0; i++) {
        float rayAngle = angle + i * 3.14159 / 3.0;
        float ray = sin(rayAngle * 3.0 + u_Time * 2.0) * 0.5 + 0.5;
        ray = smoothstep(0.4, 0.6, ray);
        beatRays += ray * beatFlash * 0.3;
    }

    // ========== PARTÍCULAS MUSICALES FLOTANTES ==========
    float particles = 0.0;

    // Más partículas cuando hay energía musical
    float particleDensity = 8.0 + u_MusicEnergy * 12.0;  // 8-20 partículas

    for (float i = 0.0; i < 20.0; i++) {
        if (i >= particleDensity) break;

        vec2 particleUV = uv * 10.0 + vec2(
            sin(u_Time * 0.5 + i * 0.7) * 2.0,
            cos(u_Time * 0.3 + i * 0.5) * 2.0
        );

        float particle = noise(particleUV);
        particle = smoothstep(0.92, 0.98, particle);

        // Partículas parpadean con la música
        float twinkle = sin(u_Time * 4.0 + i * 2.0 + u_BassBoost * 10.0) * 0.5 + 0.5;
        particles += particle * twinkle * 0.15;
    }

    // ========== NÚCLEO BRILLANTE ==========
    float core = 1.0 - smoothstep(0.0, 0.4, dist);
    core = pow(core, 2.0);

    // El núcleo pulsa con la música
    core *= bassPulse;
    core += beatFlash * 0.5;

    // ========== CORONA ENERGÉTICA ==========
    float corona = smoothstep(0.3, 0.5, dist) * (1.0 - smoothstep(0.5, 0.7, dist));
    corona *= 0.8 + u_MusicEnergy * 0.4;

    // ========== COLORES MUSICALES DINÁMICOS ==========
    // Bajos → Tonos naranjas/amarillos
    vec3 bassColor = vec3(1.0, 0.7, 0.3) * bassEffect;

    // Beats → Destellos blancos brillantes
    vec3 beatColor = vec3(1.0, 1.0, 1.0) * beatFlash;

    // Energía → Tonos azules/cyan en los bordes
    vec3 energyColor = vec3(0.4, 0.8, 1.0) * u_MusicEnergy * corona;

    // ========== COMBINAR TODOS LOS EFECTOS ==========
    vec3 finalColor = baseColor;

    // Núcleo brillante
    finalColor += core * 2.0;

    // Ondas de bajos
    finalColor += bassWaves * 0.8;

    // Colores musicales
    finalColor += bassColor;
    finalColor += beatColor;
    finalColor += energyColor;

    // Rayos de beats
    finalColor += beatRays;

    // Partículas flotantes
    finalColor += particles * vec3(1.0, 0.9, 0.7);

    // ========== BRILLO GENERAL CON MÚSICA ==========
    float musicBrightness = 1.0 + u_MusicEnergy * 0.5;
    finalColor *= musicBrightness;

    // ========== ALPHA CON DEGRADADO SUAVE ==========
    float alpha = u_Alpha;

    // Alpha aumenta en el centro
    alpha *= smoothstep(0.8, 0.2, dist);

    // Más visible con música
    alpha += u_MusicEnergy * 0.3;
    alpha = min(1.0, alpha);

    // ========== EFECTO DE RESPLANDOR (BLOOM) ==========
    float bloom = smoothstep(0.5, 0.0, dist) * 0.5;
    finalColor += bloom * u_MusicEnergy;

    // Asegurar que nunca sea negro
    finalColor = max(finalColor, vec3(0.1));

    // Clamp para evitar valores excesivos
    finalColor = min(finalColor, vec3(3.0));

    gl_FragColor = vec4(finalColor, alpha);
}
