#version 300 es
// ╔═══════════════════════════════════════════════════════════════════╗
// ║   ✨ Starry Background Fragment Shader - GLSL ES 3.0 ✨           ║
// ╚═══════════════════════════════════════════════════════════════════╝
//
// Combina imagen de fondo con estrellas procedurales generadas.
// Versión optimizada para OpenGL ES 3.0.

precision mediump float;

// Inputs
in vec2 v_TexCoord;

// Outputs (obligatorio en GLSL 300 es)
out vec4 fragColor;

// Uniforms
uniform float u_Time;
uniform vec2 u_Resolution;
uniform float u_AspectRatio;
uniform sampler2D u_Texture;

// ════════════════════════════════════════════════════════════════════════
// 🌟 FUNCIONES DE RUIDO
// ════════════════════════════════════════════════════════════════════════
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

// ════════════════════════════════════════════════════════════════════════
// ⭐ FUNCIONES DE ESTRELLAS
// ════════════════════════════════════════════════════════════════════════
float drawStar(vec2 uv, vec2 pos, float size, float brightness, float phase) {
    float dist = length(uv - pos);
    float star = 0.0;

    if (dist < size) {
        star = (1.0 - (dist / size)) * brightness;
    }

    float glowSize = size * 2.5;
    if (dist < glowSize) {
        float glow = (1.0 - (dist / glowSize)) * brightness * 0.3;
        star += glow;
    }

    float twinkle = 0.7 + sin(u_Time * (1.0 + phase) + phase * 6.28) * 0.3;
    star *= twinkle;

    return star;
}

float drawSparkle(vec2 uv, vec2 pos, float size, float brightness) {
    float dist = length(uv - pos);
    float core = max(0.0, 1.0 - (dist / size)) * brightness;

    vec2 delta = uv - pos;
    float crossGlow = 0.0;

    float horizDist = abs(delta.y);
    if (horizDist < size * 0.3) {
        crossGlow += (1.0 - horizDist / (size * 0.3)) * brightness * 0.5;
    }

    float vertDist = abs(delta.x);
    if (vertDist < size * 0.3) {
        crossGlow += (1.0 - vertDist / (size * 0.3)) * brightness * 0.5;
    }

    return core + crossGlow;
}

// ════════════════════════════════════════════════════════════════════════
// 🌸 ESTRELLA MÁGICA
// ════════════════════════════════════════════════════════════════════════
vec3 drawMagicalStarSimple(vec2 uv, vec2 pos, float time) {
    float dist = length(uv - pos);
    vec3 magicColor = vec3(0.0);

    float colorPhase = time * 0.8;

    vec3 colorRosa = vec3(1.0, 0.0, 0.5);
    vec3 colorPurpura = vec3(0.8, 0.0, 1.0);
    vec3 colorAzul = vec3(0.0, 0.5, 1.0);

    vec3 colorCore;
    float phase = mod(colorPhase, 6.28);

    if (phase < 2.09) {
        float t = phase / 2.09;
        colorCore = mix(colorRosa, colorPurpura, t);
    } else if (phase < 4.18) {
        float t = (phase - 2.09) / 2.09;
        colorCore = mix(colorPurpura, colorAzul, t);
    } else {
        float t = (phase - 4.18) / 2.10;
        colorCore = mix(colorAzul, colorRosa, t);
    }

    float pulse1 = sin(time * 0.8) * 0.5 + 0.5;
    float pulse2 = sin(time * 1.2) * 0.5 + 0.5;
    float breathe = mix(pulse1, pulse2, 0.5);
    breathe = 0.75 + breathe * 0.25;

    float sparkleInterval = 9.0;
    float sparkleTime = mod(time + 2.0, sparkleInterval);
    float sparkle = 1.0;

    if (sparkleTime < 0.4) {
        float t = sparkleTime / 0.4;
        sparkle = 1.0 + sin(t * 3.14159) * 1.8;
    }

    float coreSize = 0.010 * breathe;

    if (dist < coreSize) {
        float coreIntensity = 1.0 - (dist / coreSize);
        coreIntensity = pow(coreIntensity, 0.8);
        float coreBrightness = 0.9 + breathe * 0.3;
        magicColor += colorCore * coreIntensity * coreBrightness * sparkle;
    }

    float haloSize = 0.030 * breathe;

    if (dist < haloSize) {
        float glowIntensity = 1.0 - (dist / haloSize);
        glowIntensity = pow(glowIntensity, 2.5);
        float haloBrightness = 0.3 + breathe * 0.15;
        magicColor += colorCore * glowIntensity * haloBrightness * sparkle;
    }

    return magicColor;
}

// ════════════════════════════════════════════════════════════════════════
// 🌌 NÚCLEO GALÁCTICO
// ════════════════════════════════════════════════════════════════════════
vec3 drawGalacticCore(vec2 uv, vec2 pos, float time, float seed) {
    float dist = length(uv - pos);
    vec3 coreColor = vec3(0.0);

    float colorPhase = time * 0.8;

    vec3 color1, color2, color3;

    if (seed < 0.15) {
        color1 = vec3(0.0, 1.0, 0.3);
        color2 = vec3(1.0, 1.0, 0.0);
        color3 = vec3(1.0, 0.5, 0.0);
    } else if (seed < 0.25) {
        color1 = vec3(1.0, 0.5, 0.0);
        color2 = vec3(1.0, 0.0, 0.0);
        color3 = vec3(1.0, 0.0, 0.6);
    } else if (seed < 0.55) {
        color1 = vec3(1.0, 0.0, 0.2);
        color2 = vec3(1.0, 0.6, 0.0);
        color3 = vec3(1.0, 1.0, 0.2);
    } else if (seed < 0.75) {
        color1 = vec3(0.0, 0.3, 1.0);
        color2 = vec3(0.0, 1.0, 1.0);
        color3 = vec3(0.2, 1.0, 0.5);
    } else {
        color1 = vec3(0.3, 1.0, 0.5);
        color2 = vec3(0.0, 1.0, 0.8);
        color3 = vec3(0.2, 0.6, 1.0);
    }

    vec3 galaxyColorCore;
    float phase = mod(colorPhase + seed * 2.0, 6.28);

    if (phase < 2.09) {
        float t = phase / 2.09;
        galaxyColorCore = mix(color1, color2, t);
    } else if (phase < 4.18) {
        float t = (phase - 2.09) / 2.09;
        galaxyColorCore = mix(color2, color3, t);
    } else {
        float t = (phase - 4.18) / 2.10;
        galaxyColorCore = mix(color3, color1, t);
    }

    float pulseSpeed = 0.6 + seed * 0.5;
    float pulse = sin(time * pulseSpeed + seed * 6.28) * 0.15 + 0.85;

    float sparkleInterval = 9.0;
    float sparkleTime = mod(time + seed * 5.0, sparkleInterval);
    float sparkle = 1.0;

    if (sparkleTime < 0.4) {
        float t = sparkleTime / 0.4;
        sparkle = 1.0 + sin(t * 3.14159) * 1.8;
    }

    float coreSize = 0.005;
    if (dist < coreSize) {
        float coreIntensity = (1.0 - dist / coreSize) * pulse;
        coreIntensity = pow(coreIntensity, 0.8);
        coreColor += galaxyColorCore * coreIntensity * 1.5 * sparkle;
    }

    float haloSize1 = 0.012;
    if (dist < haloSize1) {
        float haloIntensity = (1.0 - dist / haloSize1) * pulse;
        haloIntensity = pow(haloIntensity, 1.5);
        coreColor += galaxyColorCore * haloIntensity * 0.7 * sparkle;
    }

    float haloSize2 = 0.0275;
    if (dist < haloSize2) {
        float haloIntensity = (1.0 - dist / haloSize2) * pulse;
        haloIntensity = pow(haloIntensity, 2.0);
        coreColor += galaxyColorCore * haloIntensity * 0.35 * sparkle;
    }

    float glowSize = 0.05;
    if (dist < glowSize) {
        float glowIntensity = (1.0 - dist / glowSize) * pulse;
        glowIntensity = pow(glowIntensity, 3.0);
        coreColor += galaxyColorCore * glowIntensity * 0.15 * sparkle;
    }

    return coreColor;
}

// ════════════════════════════════════════════════════════════════════════
// 🔍 DETECCIÓN DE BORDES
// ════════════════════════════════════════════════════════════════════════
float detectEdges(sampler2D tex, vec2 uv, vec2 resolution) {
    vec2 texelSize = 1.0 / resolution;

    vec3 tl = texture(tex, uv + vec2(-texelSize.x,  texelSize.y)).rgb;
    vec3 tc = texture(tex, uv + vec2(0.0,           texelSize.y)).rgb;
    vec3 tr = texture(tex, uv + vec2( texelSize.x,  texelSize.y)).rgb;
    vec3 ml = texture(tex, uv + vec2(-texelSize.x,  0.0)).rgb;
    vec3 mr = texture(tex, uv + vec2( texelSize.x,  0.0)).rgb;
    vec3 bl = texture(tex, uv + vec2(-texelSize.x, -texelSize.y)).rgb;
    vec3 bc = texture(tex, uv + vec2(0.0,          -texelSize.y)).rgb;
    vec3 br = texture(tex, uv + vec2( texelSize.x, -texelSize.y)).rgb;

    vec3 sobelX = -tl + tr - 2.0*ml + 2.0*mr - bl + br;
    vec3 sobelY = -tl - 2.0*tc - tr + bl + 2.0*bc + br;

    vec3 edgeVector = sqrt(sobelX * sobelX + sobelY * sobelY);
    float edgeStrength = dot(edgeVector, vec3(0.299, 0.587, 0.114));
    edgeStrength = smoothstep(0.2, 0.5, edgeStrength);

    return edgeStrength;
}

// ════════════════════════════════════════════════════════════════════════
// 🎬 MAIN
// ════════════════════════════════════════════════════════════════════════
void main() {
    // Parallax sutil para profundidad
    vec2 parallaxOffset = vec2(
        sin(u_Time * 0.003) * 0.002,
        cos(u_Time * 0.004) * 0.002
    );

    vec2 uv = v_TexCoord + parallaxOffset;
    vec3 color = vec3(0.0);

    // ═══ ESTRELLAS FUGACES ═══
    float shootingTime1 = mod(u_Time, 15.0);
    if (shootingTime1 < 2.5) {
        float progress1 = shootingTime1 / 2.5;
        vec2 starPos1 = vec2(progress1 * 1.3 - 0.15, 0.15 + progress1 * 0.7);
        float fade1 = smoothstep(0.0, 0.15, progress1) * smoothstep(1.0, 0.85, progress1);

        vec3 starColor1 = mix(vec3(0.3, 0.8, 1.0), vec3(1.0, 0.9, 0.4), progress1);
        float starCore = drawStar(uv, starPos1, 0.015, 1.0, 0.0) * fade1;
        color += starColor1 * starCore * 1.5;

        for (int i = 1; i < 8; i++) {
            float trailOffset = float(i) * 0.035;
            vec2 trailPos = starPos1 - vec2(trailOffset * 1.5, trailOffset * 0.9);
            float sparkle = drawSparkle(uv, trailPos, 0.012, 0.6) * fade1;
            float particleFade = (1.0 - float(i) * 0.12);
            vec3 trailColor = mix(vec3(0.4, 0.9, 1.0), vec3(0.8, 0.3, 1.0), float(i) / 8.0);
            color += trailColor * sparkle * particleFade;
        }

        float halo = drawStar(uv, starPos1, 0.04, 0.4, 0.0) * fade1;
        color += starColor1 * halo * 0.8;
    }

    float shootingTime2 = mod(u_Time + 7.5, 13.0);
    if (shootingTime2 < 2.2) {
        float progress2 = shootingTime2 / 2.2;
        vec2 starPos2 = vec2(1.15 - progress2 * 1.3, 0.25 + progress2 * 0.6);
        float fade2 = smoothstep(0.0, 0.12, progress2) * smoothstep(1.0, 0.88, progress2);

        vec3 starColor2 = mix(vec3(0.9, 0.4, 1.0), vec3(0.3, 0.7, 1.0), progress2);
        float starCore2 = drawStar(uv, starPos2, 0.013, 0.95, 0.0) * fade2;
        color += starColor2 * starCore2 * 1.4;

        for (int i = 1; i < 7; i++) {
            float trailOffset = float(i) * 0.04;
            vec2 trailPos = starPos2 + vec2(trailOffset * 1.4, -trailOffset * 0.7);
            float sparkle = drawSparkle(uv, trailPos, 0.010, 0.55) * fade2;
            float particleFade = (1.0 - float(i) * 0.14);
            vec3 trailColor = mix(vec3(0.8, 0.4, 1.0), vec3(0.3, 0.9, 0.8), float(i) / 7.0);
            color += trailColor * sparkle * particleFade;
        }

        float halo2 = drawStar(uv, starPos2, 0.035, 0.35, 0.0) * fade2;
        color += starColor2 * halo2 * 0.7;
    }

    // ═══ TEXTURA DE FONDO ═══
    vec3 backgroundTexture = texture(u_Texture, v_TexCoord).rgb;

    // ═══ EDGE GLOW ═══
    float edgeIntensity = detectEdges(u_Texture, v_TexCoord, u_Resolution);
    vec3 edgeColor = vec3(0.85, 0.95, 1.0);
    vec3 edgeGlow = edgeColor * edgeIntensity * 0.11;
    float edgeOpacity = edgeIntensity * 0.3;
    backgroundTexture = mix(backgroundTexture, backgroundTexture + edgeGlow, edgeOpacity);

    // ═══ ESTRELLAS PARPADEANTES ═══
    vec3 twinklingStars = vec3(0.0);
    float gridSize = 20.0;
    vec2 gridCoord = v_TexCoord * gridSize;
    vec2 gridCell = floor(gridCoord);

    for (float y = -1.0; y <= 1.0; y += 1.0) {
        for (float x = -1.0; x <= 1.0; x += 1.0) {
            vec2 neighborCell = gridCell + vec2(x, y);
            float cellHash = hash(neighborCell);

            if (cellHash > 0.5) {
                vec2 starPosInCell = vec2(
                    hash(neighborCell + vec2(123.0, 0.0)),
                    hash(neighborCell + vec2(0.0, 456.0))
                );
                vec2 starPos = (neighborCell + starPosInCell) / gridSize;

                float dist = length((v_TexCoord - starPos) * u_Resolution / max(u_Resolution.x, u_Resolution.y));
                float starSize = 0.0015 + hash(neighborCell + vec2(789.0, 0.0)) * 0.002;

                float starSeed = cellHash;
                float frequency = 0.8 + starSeed * 1.5;

                float twinkle1 = sin(u_Time * frequency + starSeed * 6.28) * 0.5 + 0.5;
                float twinkle2 = sin(u_Time * frequency * 1.3 + starSeed * 3.14) * 0.5 + 0.5;
                float twinkle3 = noise(neighborCell + u_Time * 0.15);
                float twinkleAmount = mix(twinkle1, twinkle2, 0.4) * 0.6 + twinkle3 * 0.4;
                twinkleAmount = pow(twinkleAmount, 1.5);

                float brightness = 0.5 + twinkleAmount * 1.0;

                if (dist < starSize * 3.0) {
                    float core = smoothstep(starSize * 1.5, 0.0, dist);
                    float glow = smoothstep(starSize * 3.0, starSize * 0.5, dist);
                    float intensity = core * 1.0 + glow * 0.3;
                    intensity *= brightness;

                    vec3 starColor;
                    if (starSeed < 0.33) {
                        starColor = vec3(0.9, 0.95, 1.0);
                    } else if (starSeed < 0.66) {
                        starColor = vec3(1.0, 1.0, 0.95);
                    } else {
                        starColor = vec3(1.0, 0.95, 0.9);
                    }

                    twinklingStars += starColor * intensity;
                }
            }
        }
    }

    // ═══ NÚCLEOS GALÁCTICOS ═══
    vec3 galacticCores = vec3(0.0);
    galacticCores += drawGalacticCore(v_TexCoord, vec2(0.737, 0.485), u_Time, 0.1);
    galacticCores += drawMagicalStarSimple(v_TexCoord, vec2(0.277, 0.342), u_Time);
    galacticCores += drawGalacticCore(v_TexCoord, vec2(0.125, 0.560), u_Time, 0.5);
    galacticCores += drawGalacticCore(v_TexCoord, vec2(0.567, 0.671), u_Time, 0.7);
    galacticCores += drawGalacticCore(v_TexCoord, vec2(0.832, 0.721), u_Time, 0.2);
    galacticCores += drawGalacticCore(v_TexCoord, vec2(0.252, 0.690), u_Time, 0.9);

    // ═══ COMBINAR TODO ═══
    vec3 finalColor = backgroundTexture + twinklingStars + color + galacticCores;

    // ═══ VIGNETTE ═══
    vec2 centerUv = v_TexCoord - 0.5;
    float distFromCenter = length(centerUv);
    float vignette = 1.0 - smoothstep(0.3, 0.8, distFromCenter);
    vignette = mix(0.85, 1.0, vignette);

    finalColor *= vignette;

    fragColor = vec4(finalColor, 1.0);
}
