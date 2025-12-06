#version 300 es
// ╔═══════════════════════════════════════════════════════════════════╗
// ║   ✨ Starry Background - ROTACIÓN DE BRAZOS GALÁCTICOS ✨         ║
// ╚═══════════════════════════════════════════════════════════════════╝
//
// Distorsión ROTACIONAL para simular que los brazos de las galaxias giran.

precision mediump float;

// Inputs
in vec2 v_TexCoord;

// Outputs
out vec4 fragColor;

// Uniforms
uniform sampler2D u_Texture;
uniform float u_Time;

// ════════════════════════════════════════════════════════════════════════
// 🌀 DISTORSIÓN ESPIRAL/ROTACIONAL
// ════════════════════════════════════════════════════════════════════════

vec2 spiralDistortion(vec2 uv, vec2 center, float time, float strength, float direction) {
    vec2 delta = uv - center;
    float dist = length(delta);

    // Si estamos muy lejos del centro, no aplicar distorsión
    if (dist > 0.15) return uv;

    float angle = atan(delta.y, delta.x);

    // Rotación MUY VISIBLE - más fuerte cerca del centro
    float falloff = 1.0 - (dist / 0.15);  // 1.0 en centro, 0.0 en borde
    falloff = falloff * falloff;  // Curva más suave

    float rotationAmount = strength * falloff;

    // Aplicar rotación continua
    float newAngle = angle + rotationAmount * direction * time;

    return center + vec2(cos(newAngle), sin(newAngle)) * dist;
}

// ════════════════════════════════════════════════════════════════════════
// 🎬 MAIN
// ════════════════════════════════════════════════════════════════════════
void main() {
    vec2 uv = v_TexCoord;

    // ═══ CENTROS DE LAS 7 GALAXIAS - ROTACIÓN MUY VISIBLE ═══
    // strength aumentado 10x para que sea muy notorio

    // Galaxia 0
    uv = spiralDistortion(uv, vec2(0.74, 0.48), u_Time, 0.15, -1.0);

    // Galaxia 1
    uv = spiralDistortion(uv, vec2(0.45, 0.465), u_Time, 0.12, 1.0);

    // Galaxia 2
    uv = spiralDistortion(uv, vec2(0.28, 0.345), u_Time, 0.14, -1.0);

    // Galaxia 3
    uv = spiralDistortion(uv, vec2(0.125, 0.56), u_Time, 0.16, 1.0);

    // Galaxia 4
    uv = spiralDistortion(uv, vec2(0.25, 0.69), u_Time, 0.13, -1.0);

    // Galaxia 5
    uv = spiralDistortion(uv, vec2(0.57, 0.67), u_Time, 0.11, 1.0);

    // Galaxia 6
    uv = spiralDistortion(uv, vec2(0.83, 0.72), u_Time, 0.14, -1.0);

    // ═══ CLAMP UV ═══
    uv = clamp(uv, 0.001, 0.999);

    // ═══ TEXTURA DE FONDO ═══
    vec3 backgroundTexture = texture(u_Texture, uv).rgb;

    // ═══ OSCURECER ÁREAS OSCURAS ═══
    float luminance = dot(backgroundTexture, vec3(0.299, 0.587, 0.114));
    float darkening = smoothstep(0.0, 0.5, luminance);
    backgroundTexture *= mix(0.5, 1.0, darkening);

    // ═══ REDUCIR BRILLO GENERAL ═══
    backgroundTexture *= 0.75;

    // ═══ VIGNETTE ═══
    vec2 centerUv = v_TexCoord - 0.5;
    float distFromCenter = length(centerUv);
    float vignette = 1.0 - smoothstep(0.3, 0.8, distFromCenter);
    vignette = mix(0.85, 1.0, vignette);

    vec3 finalColor = backgroundTexture * vignette;

    fragColor = vec4(finalColor, 1.0);
}
