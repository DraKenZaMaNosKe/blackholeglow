// ============================================
// archivo: sol_lava_fragment.glsl
// Shader de lava estilizado para el sol
// Efecto de videojuego con superficie fluida de lava
// ============================================

#ifdef GL_ES
precision mediump float;
#endif

// ------- Uniforms -------
uniform float u_Time;
uniform sampler2D u_Texture;
uniform int u_UseSolidColor;
uniform vec4 u_SolidColor;
uniform float u_Alpha;
uniform vec2 u_Resolution;

// ------- Varyings -------
varying vec2 v_TexCoord;
varying vec3 v_WorldPos;

// ============================================
// Funciones de ruido para efectos procedurales
// ============================================

// Ruido 2D simple
float noise2D(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

// Ruido suavizado
float smoothNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float a = noise2D(i);
    float b = noise2D(i + vec2(1.0, 0.0));
    float c = noise2D(i + vec2(0.0, 1.0));
    float d = noise2D(i + vec2(1.0, 1.0));

    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

// Ruido fractal ULTRA-OPTIMIZADO (1 octava - máximo rendimiento)
float fbm(vec2 p) {
    // Solo 1 octava para rendimiento extremo
    return smoothNoise(p * 2.0) * 0.5;
}

// ============================================
// Función principal de efecto de lava
// ============================================

vec3 getLavaColor(vec2 uv, float time) {
    // Coordenadas polares para efectos radiales
    vec2 center = vec2(0.5, 0.5);
    vec2 toCenter = uv - center;
    float dist = length(toCenter);
    float angle = atan(toCenter.y, toCenter.x);

    // Animación de flujo de lava
    float flowSpeed = 0.3;
    vec2 flowOffset = vec2(
        sin(time * flowSpeed) * 0.1,
        cos(time * flowSpeed * 0.7) * 0.1
    );

    // Distorsión UV para simular flujo
    vec2 distortedUV = uv + flowOffset;

    // OPTIMIZADO: Solo 2 capas de ruido (en lugar de 3)
    float noise1 = fbm(distortedUV * 3.0 + time * 0.2);
    float noise2 = smoothNoise(distortedUV * 6.0 - time * 0.15);

    // Combinar ruidos simplificado
    float lavaPattern = noise1 * 0.6 + noise2 * 0.4;

    // OPTIMIZADO: Burbujas ELIMINADAS (ahorro ~15 ops/pixel)

    // OPTIMIZADO: Anillos simplificados (sin pow costoso)
    float rings = sin(dist * 20.0 - time * 0.8) * 0.5 + 0.5;
    lavaPattern += rings * 0.08 * (1.0 - dist);

    // Gradiente radial para el núcleo más caliente
    float coreIntensity = 1.0 - smoothstep(0.0, 0.5, dist);
    lavaPattern = mix(lavaPattern, 1.0, coreIntensity * 0.5);

    // Paleta de colores de lava estilizada
    vec3 coolLava = vec3(0.5, 0.1, 0.0);     // Rojo oscuro
    vec3 midLava = vec3(1.0, 0.3, 0.0);      // Naranja
    vec3 hotLava = vec3(1.0, 0.8, 0.0);      // Amarillo
    vec3 superHotLava = vec3(1.0, 1.0, 0.6); // Blanco amarillento

    // Mapear el patrón a la paleta de colores
    vec3 color;
    if(lavaPattern < 0.25) {
        color = mix(coolLava, midLava, lavaPattern * 4.0);
    } else if(lavaPattern < 0.5) {
        color = mix(midLava, hotLava, (lavaPattern - 0.25) * 4.0);
    } else if(lavaPattern < 0.75) {
        color = mix(hotLava, superHotLava, (lavaPattern - 0.5) * 4.0);
    } else {
        color = superHotLava;
    }

    // Añadir emisión/glow
    float glow = pow(lavaPattern, 2.0) * 0.5;
    color += vec3(glow * 0.5, glow * 0.3, glow * 0.1);

    // Pulsación MUY sutil y lenta (respiración suave del sol)
    float pulse = sin(time * 0.6) * 0.03 + 0.97;  // Reducido de 4.0 a 0.6 (6.6x más lento)
                                                     // Variación reducida de 5% a 3%
    color *= pulse;

    return color;
}

// ============================================
// Main
// ============================================

void main() {
    vec2 uv = v_TexCoord;

    // Efecto de lava procedural
    vec3 lavaColor = getLavaColor(uv, u_Time);

    // Si hay textura, mezclarla sutilmente
    if(u_UseSolidColor == 0) {
        vec4 texColor = texture2D(u_Texture, uv);
        // Usar la textura como máscara de intensidad
        float texIntensity = (texColor.r + texColor.g + texColor.b) / 3.0;
        lavaColor *= 0.7 + texIntensity * 0.3;
    }

    // Oscurecer los bordes para efecto esférico
    vec2 center = vec2(0.5, 0.5);
    float edgeDist = length(uv - center);
    float edgeFactor = 1.0 - smoothstep(0.3, 0.5, edgeDist);
    lavaColor *= edgeFactor;

    // Alpha y salida final - SOL COMPLETAMENTE SÓLIDO
    float finalAlpha = 1.0;  // SIEMPRE OPACO

    // NO hacer transparente en ningún caso para cubrir toda la esfera
    // El modelo 3D ya define la forma esférica

    // Asegurar que el color nunca sea negro (evitar agujeros)
    lavaColor = max(lavaColor, vec3(0.1, 0.05, 0.0));  // Mínimo color lava oscuro

    gl_FragColor = vec4(lavaColor, finalAlpha);
}