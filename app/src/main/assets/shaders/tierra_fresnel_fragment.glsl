/**
 * ╔════════════════════════════════════════════════════════════════╗
 * ║  🌍 TIERRA - Fragment Shader con Océanos Brillantes            ║
 * ║  Los océanos tienen brillo especular animado                   ║
 * ╚════════════════════════════════════════════════════════════════╝
 */

#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_Texture;
uniform float u_Alpha;
uniform float u_Time;

varying vec2 v_TexCoord;
varying vec3 v_Normal;
varying vec3 v_Position;

void main() {
    // ✅ FIX: ObjLoader ya maneja el flip de V para modelos Meshy
    // NO voltear aquí para evitar doble flip
    vec2 uv = v_TexCoord;

    // ─────────────────────────────────────────────────────────────
    // 1. TEXTURA BASE DE LA TIERRA
    // ─────────────────────────────────────────────────────────────
    vec4 texColor = texture2D(u_Texture, uv);
    vec3 earthColor = texColor.rgb;

    // ─────────────────────────────────────────────────────────────
    // 2. DETECTAR OCÉANOS - Con transición suave
    // ─────────────────────────────────────────────────────────────

    float r = texColor.r;
    float g = texColor.g;
    float b = texColor.b;

    // Luminosidad
    float lum = (r + g + b) / 3.0;

    // Detección suave de océanos (gradiente en vez de 0/1)
    float blueRatio = b / (r + 0.01);
    float blueVsGreen = b / (g + 0.01);

    // Océano = transición suave basada en qué tan azul es
    float isOcean = smoothstep(1.0, 1.5, blueRatio) * smoothstep(0.8, 1.2, blueVsGreen);

    // También considerar áreas oscuras con tinte azul
    float darkBlue = smoothstep(0.5, 0.3, lum) * smoothstep(0.9, 1.1, b / (max(r, g) + 0.01));
    isOcean = max(isOcean, darkBlue);

    // Suavizar máscara de océano
    isOcean = smoothstep(0.1, 0.6, isOcean);

    // ─────────────────────────────────────────────────────────────
    // 3. REFLEJO DE SOL EN OCÉANOS (suave y natural)
    // ─────────────────────────────────────────────────────────────

    // Punto de reflejo del sol que se mueve lentamente
    float sunX = 0.5 + 0.25 * sin(u_Time * 0.3);
    float sunY = 0.5 + 0.15 * cos(u_Time * 0.2);

    // Distancia al punto de reflejo
    float distToSun = distance(uv, vec2(sunX, sunY));

    // Brillo del reflejo - MUY suave en los bordes
    float sunReflect = 1.0 - smoothstep(0.0, 0.5, distToSun);
    sunReflect = pow(sunReflect, 3.0);  // Más concentrado en el centro

    // Ondas de agua más sutiles
    float waves = sin(uv.x * 25.0 + u_Time * 2.0) * sin(uv.y * 20.0 - u_Time * 1.5);
    waves = waves * 0.3 + 0.7;

    // Combinar reflejo con ondas
    float oceanShine = sunReflect * waves;

    // ─────────────────────────────────────────────────────────────
    // 4. APLICAR EFECTO
    // ─────────────────────────────────────────────────────────────

    vec3 finalColor = earthColor;

    // Brillo blanco-cyan suave en océanos
    vec3 shineColor = vec3(0.9, 0.95, 1.0);
    finalColor += shineColor * oceanShine * isOcean * 0.4;

    // Realzar azul de océanos muy ligeramente
    finalColor.b += isOcean * 0.03;

    // Brillo base
    finalColor *= 1.12;

    finalColor = clamp(finalColor, 0.0, 1.0);

    gl_FragColor = vec4(finalColor, texColor.a * u_Alpha);
}
