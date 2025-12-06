// ═══════════════════════════════════════════════════════════════════════
// 🔥 Fragment shader para estelas de plasma y fuego - OPTIMIZADO
// ═══════════════════════════════════════════════════════════════════════
// ⚡ Optimizaciones:
// - Reducido de ~12 llamadas a noise() a solo 2
// - Eliminados cálculos de chispas costosos
// - Simplificados gradientes con menos branches
// ═══════════════════════════════════════════════════════════════════════
#ifdef GL_ES
precision mediump float;
#endif

uniform float u_Time;
uniform float u_TrailType;  // 0 = fuego, 0.5 = plasma, 1 = arcoíris

varying vec4 v_Color;
varying float v_Age;
varying vec2 v_UV;

// ⚡ Función de ruido optimizada (solo 2 llamadas en todo el shader)
float noise(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

// ⚡ Gradiente de fuego simplificado (sin branches)
vec3 fireGradient(float t) {
    vec3 c1 = vec3(0.5, 0.0, 0.0);   // Rojo oscuro
    vec3 c2 = vec3(1.0, 0.5, 0.0);   // Naranja
    vec3 c3 = vec3(1.0, 0.9, 0.3);   // Amarillo

    float t2 = t * 2.0;
    vec3 color = mix(c1, c2, clamp(t2, 0.0, 1.0));
    color = mix(color, c3, clamp(t2 - 1.0, 0.0, 1.0));
    return color;
}

// ⚡ Gradiente de plasma simplificado
vec3 plasmaGradient(float t, float wave) {
    vec3 c1 = vec3(0.0, 0.2, 0.8);   // Azul
    vec3 c2 = vec3(0.0, 0.8, 1.0);   // Cyan
    vec3 c3 = vec3(0.7, 1.0, 1.0);   // Cyan claro

    float t2 = t * 2.0;
    vec3 color = mix(c1, c2, clamp(t2, 0.0, 1.0));
    color = mix(color, c3, clamp(t2 - 1.0, 0.0, 1.0));

    // Efecto eléctrico simple
    color += vec3(0.2, 0.4, 0.8) * wave * 0.3;
    return color;
}

void main() {
    // ⚡ Solo 2 llamadas a noise en todo el shader
    float n1 = noise(v_UV * 8.0 + u_Time);
    float n2 = noise(v_UV * 15.0 - u_Time * 2.0);

    // Turbulencia sutil
    vec2 turbUV = v_UV + vec2(n1, n2) * 0.03;

    // Brillo base (más brillante al principio)
    float glow = 1.0 - v_Age;

    // Color base según el tipo
    vec3 trailColor;

    if (u_TrailType < 0.3) {
        // ═══ FUEGO ═══
        trailColor = fireGradient(v_Age);
        trailColor *= (1.0 + n1 * 0.25);

    } else if (u_TrailType < 0.7) {
        // ═══ PLASMA ═══
        float wave = sin(turbUV.x * 15.0 + u_Time * 4.0) * 0.5 + 0.5;
        trailColor = plasmaGradient(v_Age, wave * n2);

        // Pulso de energía
        float energy = sin(u_Time * 8.0 - v_Age * 4.0) * 0.2 + 0.8;
        trailColor *= energy;

    } else {
        // ═══ ARCOÍRIS ═══
        float hue = v_Age * 3.0 + u_Time * 2.0;
        trailColor.r = sin(hue) * 0.5 + 0.5;
        trailColor.g = sin(hue + 2.094) * 0.5 + 0.5;
        trailColor.b = sin(hue + 4.188) * 0.5 + 0.5;
        trailColor *= 1.4;
    }

    // Brillo en bordes (simple)
    float edgeGlow = 1.0 - abs(v_UV.y - 0.5) * 2.0;
    trailColor *= (1.0 + edgeGlow * 0.4);

    // Intensificar brillo inicial
    trailColor *= (1.0 + glow * 0.4);

    // Alpha con desvanecimiento
    float alpha = v_Color.a * pow(1.0 - v_Age, 0.8);

    // Disolución en la cola (usando n2 ya calculado)
    if (v_Age > 0.65) {
        float dissolve = (v_Age - 0.65) * 2.85;
        alpha *= (1.0 - dissolve);
        alpha *= step(n2, 0.5 + v_Age * 0.3);
    }

    // Inicio brillante
    if (v_Age < 0.1) {
        alpha = max(alpha, 0.75);
        trailColor *= 1.3;
    }

    gl_FragColor = vec4(trailColor, alpha);
}
