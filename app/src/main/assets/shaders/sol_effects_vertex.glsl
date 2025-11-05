// ============================================
// archivo: sol_effects_vertex.glsl
// ☀️ CAPA DE EFECTOS - VERTEX SHADER
// Mismo desplazamiento que el sol base para sincronización perfecta
// ============================================

#ifdef GL_ES
precision mediump float;
#endif

attribute vec4 a_Position;
attribute vec2 a_TexCoord;

uniform mat4 u_MVP;
uniform float u_Time;

varying vec2 v_TexCoord;
varying vec3 v_WorldPos;
varying float v_Displacement;  // Para efectos reactivos en fragment

// ============================================
// 🔧 RUIDO 3D
// ============================================

float hash(vec3 p) {
    return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453);
}

float noise3D(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float n000 = hash(i);
    float n100 = hash(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash(i + vec3(1.0, 1.0, 1.0));

    float x00 = mix(n000, n100, f.x);
    float x10 = mix(n010, n110, f.x);
    float x01 = mix(n001, n101, f.x);
    float x11 = mix(n011, n111, f.x);

    float y0 = mix(x00, x10, f.y);
    float y1 = mix(x01, x11, f.y);

    return mix(y0, y1, f.z);
}

// ============================================
// 🌊 MAIN - MISMO DESPLAZAMIENTO QUE SOL BASE
// ============================================

void main() {
    // Posición original del vértice
    vec3 pos = a_Position.xyz;

    // Normal (dirección desde el centro hacia el vértice)
    vec3 normal = normalize(pos);

    // ═══════════════════════════════════════════════════════════
    // 🔥 DESPLAZAMIENTO CON RUIDO 3D - PLASMA BURBUJEANTE (base)
    // ═══════════════════════════════════════════════════════════

    // Primera capa de ruido (movimiento lento)
    vec3 noisePos1 = pos * 2.5 + vec3(u_Time * 0.08);
    float noise1 = noise3D(noisePos1);

    // Segunda capa de ruido (frecuencia diferente)
    vec3 noisePos2 = pos * 4.0 + vec3(u_Time * 0.05, u_Time * 0.06, u_Time * 0.07);
    float noise2 = noise3D(noisePos2);

    // Combinar capas de ruido base
    float plasmaDisplacement = (noise1 * 0.6 + noise2 * 0.4) - 0.5;  // Rango: -0.5 a 0.5

    // ═══════════════════════════════════════════════════════════
    // 🌊 ONDAS VIAJERAS - Viajan desde polo norte hacia ecuador
    // ═══════════════════════════════════════════════════════════

    // Calcular latitud (altura Y) - para ondas que viajan de polo a ecuador
    float latitude = pos.y;  // Rango: -1.0 a 1.0

    // Calcular longitud (ángulo alrededor del eje Y)
    float longitude = atan(pos.z, pos.x);  // Rango: -π a π

    // Onda 1: Viaja de norte a sur (MÁS SUAVE - frecuencia reducida)
    float wave1 = sin(latitude * 3.0 - u_Time * 0.12) * 0.5;

    // Onda 2: Viaja con componente longitudinal (espiral MÁS SUAVE)
    float wave2 = sin(latitude * 2.5 + longitude * 1.0 - u_Time * 0.08) * 0.3;

    // Onda 3: Movimiento más lento y gradual
    float wave3 = sin(latitude * 4.0 - longitude * 0.5 + u_Time * 0.15) * 0.2;

    // Combinar ondas
    float waveDisplacement = (wave1 + wave2 + wave3) * 0.5;  // Normalizar

    // Modular las ondas con ruido para que no sean perfectamente uniformes
    waveDisplacement *= (0.6 + noise1 * 0.4);

    // ═══════════════════════════════════════════════════════════
    // ⚡ COMBINAR EFECTOS
    // ═══════════════════════════════════════════════════════════

    // Mezclar plasma burbujeante (60%) + ondas viajeras (40%)
    float finalDisplacement = plasmaDisplacement * 0.6 + waveDisplacement * 0.4;

    // Desplazar el vértice a lo largo de su normal (SUTIL - 4%)
    pos += normal * finalDisplacement * 0.04;

    // ═══════════════════════════════════════════════════════════
    // 🎯 OUTPUT
    // ═══════════════════════════════════════════════════════════

    gl_Position = u_MVP * vec4(pos, 1.0);
    v_TexCoord = a_TexCoord;
    v_WorldPos = pos;
    v_Displacement = finalDisplacement;  // Pasar al fragment para efectos reactivos
}
