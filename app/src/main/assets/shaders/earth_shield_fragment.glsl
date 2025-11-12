// earth_shield_fragment.glsl
// Fragment Shader para EarthShield - Efectos de impacto volcánicos/fracturas

precision mediump float;

uniform float u_Time;
uniform sampler2D u_Texture;
uniform vec3 u_Color;           // Color base (naranja/rojo)
uniform float u_Alpha;          // Transparencia del escudo
uniform float u_Health;         // Salud (siempre 1.0)

// Sistema de impactos (16 simultáneos)
uniform vec3 u_ImpactPos[16];
uniform float u_ImpactIntensity[16];

varying vec2 v_TexCoord;
varying vec3 v_WorldPos;

// ============================================
// UTILIDADES
// ============================================

// Hash para ruido procedural
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

// Ruido simple
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

// ============================================
// EFECTOS DE IMPACTO
// ============================================

// Verificar si el punto está del mismo lado de la esfera que el impacto
float sameSide(vec3 worldPos, vec3 impactPos) {
    vec3 worldNormal = normalize(worldPos);
    vec3 impactNormal = normalize(impactPos);

    // Producto punto: >0.5 = mismo lado, <0 = lado opuesto
    float dotProduct = dot(worldNormal, impactNormal);

    // Solo mostrar efectos si está en el mismo hemisferio (umbral: 0.3)
    return smoothstep(0.0, 0.3, dotProduct);
}

// Efecto de grietas radiales desde el punto de impacto (en espacio 3D)
float crackPattern3D(vec3 worldPos, vec3 impactPos, float time) {
    // Verificar que esté del mismo lado
    float sideMask = sameSide(worldPos, impactPos);
    if (sideMask < 0.01) return 0.0;

    vec3 delta = worldPos - impactPos;
    float dist = length(delta);

    // Proyectar en un plano perpendicular al impacto
    vec3 impactDir = normalize(impactPos);
    vec3 tangent = normalize(cross(impactDir, vec3(0.0, 1.0, 0.0)));
    vec3 bitangent = cross(impactDir, tangent);

    float x = dot(delta, tangent);
    float y = dot(delta, bitangent);
    float angle = atan(y, x);

    // Grietas radiales (8 rayos)
    float rays = abs(sin(angle * 8.0 + time * 2.0));
    rays = pow(rays, 3.0);

    // Decaimiento con distancia 3D
    float falloff = smoothstep(0.3, 0.0, dist);

    return rays * falloff * sideMask;
}

// Ondas de choque concéntricas (en espacio 3D)
float shockWave3D(vec3 worldPos, vec3 impactPos, float intensity, float time) {
    // Verificar que esté del mismo lado
    float sideMask = sameSide(worldPos, impactPos);
    if (sideMask < 0.01) return 0.0;

    vec3 delta = worldPos - impactPos;
    float dist = length(delta);

    // Onda que se expande
    float waveSpeed = 0.8;
    float waveRadius = time * waveSpeed;

    // Múltiples ondas
    float wave = 0.0;
    for (float i = 0.0; i < 3.0; i += 1.0) {
        float r = waveRadius - i * 0.1;
        wave += exp(-abs(dist - r) * 30.0) * (1.0 - i * 0.3);
    }

    return wave * intensity * sideMask;
}

// Fragmentos/chispas disparadas desde el punto de impacto
float fragmentsSparks(vec3 worldPos, vec3 impactPos, float intensity, float time) {
    // Verificar que esté del mismo lado
    float sideMask = sameSide(worldPos, impactPos);
    if (sideMask < 0.01) return 0.0;

    vec3 delta = worldPos - impactPos;
    float dist = length(delta);

    // Dirección radial desde el impacto
    vec3 impactDir = normalize(impactPos);
    vec3 fragmentDir = normalize(delta);

    // Ángulo entre la dirección del fragmento y la normal del impacto
    float alignment = dot(fragmentDir, impactDir);

    // Los fragmentos se disparan hacia afuera (alignment > 0)
    float radialMask = smoothstep(-0.2, 0.5, alignment);

    // Crear patrón de fragmentos usando ruido
    vec3 seed = impactPos * 100.0;
    float fragmentPattern = 0.0;

    // Múltiples fragmentos en ángulos aleatorios
    for (float i = 0.0; i < 8.0; i += 1.0) {
        vec2 noiseCoord = vec2(
            dot(worldPos, vec3(sin(i * 0.785), cos(i * 0.785), 0.0)),
            dot(worldPos, vec3(0.0, sin(i * 0.785), cos(i * 0.785)))
        ) * 30.0 + seed.xy;

        float fragmentNoise = noise(noiseCoord);

        // Fragmentos que se mueven con el tiempo
        float fragmentDist = dist - time * 0.6; // Velocidad de fragmentos
        float fragment = exp(-abs(fragmentDist - 0.05) * 50.0) * fragmentNoise;

        fragmentPattern += fragment;
    }

    // Decaimiento con distancia
    float falloff = smoothstep(0.4, 0.0, dist);

    return fragmentPattern * radialMask * falloff * intensity * sideMask;
}

// Efecto de calor/distorsión (en espacio 3D)
float heatDistortion3D(vec3 worldPos, vec3 impactPos, float intensity) {
    // Verificar que esté del mismo lado
    float sideMask = sameSide(worldPos, impactPos);
    if (sideMask < 0.01) return 0.0;

    vec3 delta = worldPos - impactPos;
    float dist = length(delta);

    // Turbulencia de calor
    vec2 noiseCoord = worldPos.xy * 20.0 + worldPos.yz * 10.0;
    float heat = noise(noiseCoord + u_Time * 0.5);
    heat += noise(noiseCoord * 2.0 - u_Time * 0.3) * 0.5;

    // Decaimiento con distancia
    float falloff = smoothstep(0.25, 0.0, dist);

    return heat * falloff * intensity * sideMask;
}

// ============================================
// MAIN
// ============================================

void main() {
    vec2 uv = v_TexCoord;
    vec3 worldPos = v_WorldPos;  // Posición en espacio del objeto (sin normalizar)

    // Color base del escudo (normalmente invisible)
    vec4 baseColor = vec4(u_Color, u_Alpha);

    // Acumuladores de efectos
    float totalCracks = 0.0;
    float totalWaves = 0.0;
    float totalFragments = 0.0;
    float totalHeat = 0.0;

    // Procesar todos los impactos activos
    for (int i = 0; i < 16; i++) {
        float intensity = u_ImpactIntensity[i];

        if (intensity > 0.01) {
            // Posición del impacto en coordenadas mundiales
            vec3 impactPos = u_ImpactPos[i];

            // Calcular tiempo del impacto (invertido para que empiece en 1.0)
            float impactTime = 1.0 - intensity;

            // Aplicar efectos en espacio 3D
            totalCracks += crackPattern3D(worldPos, impactPos, impactTime) * intensity;
            totalWaves += shockWave3D(worldPos, impactPos, intensity, impactTime);
            totalFragments += fragmentsSparks(worldPos, impactPos, intensity, impactTime);
            totalHeat += heatDistortion3D(worldPos, impactPos, intensity);
        }
    }

    // ============================================
    // COMPOSICIÓN FINAL
    // ============================================

    // Color de grietas (rojo volcánico INTENSO)
    vec3 crackColor = vec3(1.0, 0.2, 0.0) * totalCracks * 4.0;

    // Color de ondas (naranja brillante)
    vec3 waveColor = vec3(1.0, 0.6, 0.1) * totalWaves * 5.0;

    // Color de fragmentos/chispas (amarillo-blanco MUY brillante)
    vec3 fragmentColor = vec3(1.0, 0.9, 0.6) * totalFragments * 8.0;

    // Efecto de calor (amarillo)
    vec3 heatColor = vec3(1.0, 0.8, 0.3) * totalHeat * 1.0;

    // Combinar efectos (AHORA INCLUYE FRAGMENTOS)
    vec3 impactEffects = crackColor + waveColor + fragmentColor + heatColor;

    // Color final
    vec3 finalColor = baseColor.rgb + impactEffects;

    // Alpha final (escudo invisible + efectos visibles)
    float finalAlpha = baseColor.a + clamp(length(impactEffects), 0.0, 1.0);

    gl_FragColor = vec4(finalColor, finalAlpha);
}
