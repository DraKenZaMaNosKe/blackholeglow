#ifdef GL_ES
precision mediump float;
#endif

// Fragment Shader - Fondo con Textura + Estrellas Procedurales
// Combina imagen de fondo con estrellas generadas

varying vec2 v_TexCoord;

uniform float u_Time;
uniform vec2 u_Resolution;
uniform float u_AspectRatio;
uniform sampler2D u_Texture;  // Textura del fondo

// Función mejorada con glow suave
float drawStar(vec2 uv, vec2 pos, float size, float brightness, float phase) {
    float dist = length(uv - pos);
    float star = 0.0;

    // Núcleo brillante
    if (dist < size) {
        star = (1.0 - (dist / size)) * brightness;
    }

    // Glow externo suave
    float glowSize = size * 2.5;
    if (dist < glowSize) {
        float glow = (1.0 - (dist / glowSize)) * brightness * 0.3;
        star += glow;
    }

    // Parpadeo individual
    float twinkle = 0.7 + sin(u_Time * (1.0 + phase) + phase * 6.28) * 0.3;
    star *= twinkle;

    return star;
}

void main() {
    vec2 uv = v_TexCoord;

    vec3 color = vec3(0.0);

    // ========== ESTRELLAS GRANDES Y BRILLANTES (MÁS PEQUEÑAS) ==========
    // Blancas - Reducidas ~60%
    color += vec3(1.0, 1.0, 0.98) * drawStar(uv, vec2(0.1, 0.2), 0.016, 0.9, 0.1);
    color += vec3(1.0, 1.0, 0.98) * drawStar(uv, vec2(0.3, 0.8), 0.018, 0.85, 0.3);
    color += vec3(1.0, 1.0, 0.98) * drawStar(uv, vec2(0.7, 0.3), 0.017, 0.8, 0.7);
    color += vec3(1.0, 1.0, 0.98) * drawStar(uv, vec2(0.85, 0.7), 0.015, 0.9, 0.85);
    color += vec3(1.0, 1.0, 0.98) * drawStar(uv, vec2(0.2, 0.6), 0.016, 0.8, 0.2);
    color += vec3(1.0, 1.0, 0.98) * drawStar(uv, vec2(0.45, 0.15), 0.014, 0.75, 0.45);

    // Azuladas - Reducidas
    color += vec3(0.7, 0.8, 1.0) * drawStar(uv, vec2(0.15, 0.9), 0.014, 0.8, 0.15);
    color += vec3(0.7, 0.8, 1.0) * drawStar(uv, vec2(0.5, 0.45), 0.015, 0.9, 0.5);
    color += vec3(0.7, 0.8, 1.0) * drawStar(uv, vec2(0.9, 0.25), 0.013, 0.75, 0.9);
    color += vec3(0.7, 0.85, 1.0) * drawStar(uv, vec2(0.33, 0.5), 0.014, 0.7, 0.33);

    // Amarillas/Doradas - Reducidas
    color += vec3(1.0, 0.9, 0.6) * drawStar(uv, vec2(0.4, 0.12), 0.014, 0.85, 0.4);
    color += vec3(1.0, 0.88, 0.6) * drawStar(uv, vec2(0.6, 0.82), 0.016, 0.8, 0.6);
    color += vec3(1.0, 0.9, 0.65) * drawStar(uv, vec2(0.78, 0.52), 0.013, 0.78, 0.78);
    color += vec3(1.0, 0.92, 0.65) * drawStar(uv, vec2(0.22, 0.38), 0.015, 0.75, 0.22);

    // ========== ESTRELLAS MEDIANAS (REDUCIDAS) ==========
    color += vec3(0.95, 0.95, 1.0) * drawStar(uv, vec2(0.25, 0.33), 0.010, 0.7, 0.25);
    color += vec3(0.95, 0.95, 1.0) * drawStar(uv, vec2(0.55, 0.68), 0.011, 0.65, 0.55);
    color += vec3(0.95, 0.95, 1.0) * drawStar(uv, vec2(0.73, 0.18), 0.009, 0.7, 0.73);
    color += vec3(0.95, 0.95, 1.0) * drawStar(uv, vec2(0.14, 0.58), 0.010, 0.6, 0.14);
    color += vec3(0.95, 0.95, 1.0) * drawStar(uv, vec2(0.86, 0.86), 0.009, 0.65, 0.86);
    color += vec3(0.95, 0.95, 1.0) * drawStar(uv, vec2(0.37, 0.22), 0.011, 0.7, 0.37);
    color += vec3(0.95, 0.95, 1.0) * drawStar(uv, vec2(0.63, 0.57), 0.008, 0.6, 0.63);
    color += vec3(0.95, 0.95, 1.0) * drawStar(uv, vec2(0.17, 0.42), 0.010, 0.65, 0.17);
    color += vec3(0.95, 0.95, 1.0) * drawStar(uv, vec2(0.82, 0.38), 0.009, 0.6, 0.82);
    color += vec3(0.95, 0.95, 1.0) * drawStar(uv, vec2(0.47, 0.73), 0.011, 0.7, 0.47);

    // ========== ESTRELLAS PEQUEÑAS (POLVO ESTELAR - MÁS SUTILES) ==========
    color += vec3(0.85, 0.88, 0.95) * drawStar(uv, vec2(0.08, 0.47), 0.006, 0.55, 0.08);
    color += vec3(0.85, 0.88, 0.95) * drawStar(uv, vec2(0.19, 0.77), 0.006, 0.5, 0.19);
    color += vec3(0.85, 0.88, 0.95) * drawStar(uv, vec2(0.31, 0.93), 0.005, 0.5, 0.31);
    color += vec3(0.85, 0.88, 0.95) * drawStar(uv, vec2(0.43, 0.58), 0.007, 0.6, 0.43);
    color += vec3(0.85, 0.88, 0.95) * drawStar(uv, vec2(0.51, 0.21), 0.006, 0.5, 0.51);
    color += vec3(0.85, 0.88, 0.95) * drawStar(uv, vec2(0.64, 0.74), 0.006, 0.55, 0.64);
    color += vec3(0.85, 0.88, 0.95) * drawStar(uv, vec2(0.71, 0.44), 0.005, 0.5, 0.71);
    color += vec3(0.85, 0.88, 0.95) * drawStar(uv, vec2(0.81, 0.64), 0.007, 0.6, 0.81);
    color += vec3(0.85, 0.88, 0.95) * drawStar(uv, vec2(0.91, 0.33), 0.006, 0.5, 0.91);
    color += vec3(0.85, 0.88, 0.95) * drawStar(uv, vec2(0.29, 0.51), 0.006, 0.55, 0.29);
    color += vec3(0.85, 0.88, 0.95) * drawStar(uv, vec2(0.49, 0.29), 0.005, 0.5, 0.49);
    color += vec3(0.85, 0.88, 0.95) * drawStar(uv, vec2(0.66, 0.91), 0.007, 0.6, 0.66);
    color += vec3(0.85, 0.88, 0.95) * drawStar(uv, vec2(0.07, 0.71), 0.006, 0.5, 0.07);
    color += vec3(0.85, 0.88, 0.95) * drawStar(uv, vec2(0.93, 0.93), 0.006, 0.55, 0.93);
    color += vec3(0.85, 0.88, 0.95) * drawStar(uv, vec2(0.12, 0.13), 0.005, 0.5, 0.12);
    color += vec3(0.85, 0.88, 0.95) * drawStar(uv, vec2(0.56, 0.07), 0.006, 0.55, 0.56);
    color += vec3(0.85, 0.88, 0.95) * drawStar(uv, vec2(0.38, 0.64), 0.006, 0.5, 0.38);
    color += vec3(0.85, 0.88, 0.95) * drawStar(uv, vec2(0.74, 0.81), 0.005, 0.6, 0.74);
    color += vec3(0.85, 0.88, 0.95) * drawStar(uv, vec2(0.26, 0.96), 0.006, 0.5, 0.26);
    color += vec3(0.85, 0.88, 0.95) * drawStar(uv, vec2(0.89, 0.11), 0.006, 0.55, 0.89);

    // MEZCLAR: Primero la textura del fondo, luego las estrellas encima
    vec3 backgroundTexture = texture2D(u_Texture, v_TexCoord).rgb;

    // Combinar: fondo + estrellas (aditivo)
    vec3 finalColor = backgroundTexture + color;

    gl_FragColor = vec4(finalColor, 1.0);
}
