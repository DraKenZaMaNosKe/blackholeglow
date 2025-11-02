/**
 * ╔════════════════════════════════════════════════════════════════╗
 * ║  🎨 COLOR.GLSL - Sistemas de Color y Conversiones            ║
 * ║  Black Hole Glow Shader Library v1.0.0                       ║
 * ╚════════════════════════════════════════════════════════════════╝
 *
 * Funciones para trabajar con diferentes espacios de color:
 * - HSB/HSV (Hue, Saturation, Brightness)
 * - YUV (espacio de color cinematográfico)
 * - Paletas procedurales de Inigo Quilez
 *
 * Basado en "The Book of Shaders" y técnicas de Inigo Quilez
 */

#ifdef GL_ES
precision mediump float;
#endif

// ═══════════════════════════════════════════════════════════════
// 🌈 HSB/HSV - El Espacio de Color MÁS ÚTIL para Shaders
// ═══════════════════════════════════════════════════════════════

/**
 * Convierte HSB/HSV a RGB.
 * HSB es PERFECTO para animar colores de forma suave y natural.
 *
 * @param c     vec3(hue, saturation, brightness)
 *              - hue: 0.0 a 1.0 (0=rojo, 0.33=verde, 0.66=azul, 1.0=rojo)
 *              - saturation: 0.0 (gris) a 1.0 (color puro)
 *              - brightness: 0.0 (negro) a 1.0 (brillante)
 * @return      Color RGB (0-1)
 *
 * Uso:
 *   vec3 color = hsb2rgb(vec3(u_Time * 0.1, 0.8, 0.9));  // Arcoíris animado
 *   vec3 ocean = hsb2rgb(vec3(0.55, 0.7, 0.6));          // Azul océano
 *   vec3 sunset = hsb2rgb(vec3(0.08, 0.9, 0.95));        // Naranja atardecer
 *
 * Fuente: The Book of Shaders
 * https://thebookofshaders.com/06/
 */
vec3 hsb2rgb(vec3 c) {
    vec3 rgb = clamp(
        abs(mod(c.x * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0,
        0.0,
        1.0
    );
    rgb = rgb * rgb * (3.0 - 2.0 * rgb); // Smoothstep
    return c.z * mix(vec3(1.0), rgb, c.y);
}

/**
 * Convierte RGB a HSB/HSV.
 * Útil cuando necesitas MODIFICAR un color existente.
 *
 * @param c     Color RGB (0-1)
 * @return      vec3(hue, saturation, brightness)
 *
 * Uso:
 *   vec3 hsb = rgb2hsb(texture2D(u_Texture, uv).rgb);
 *   hsb.x += 0.1;  // Rotar hue
 *   hsb.y *= 1.5;  // Aumentar saturación
 *   vec3 modified = hsb2rgb(hsb);
 *
 * Fuente: The Book of Shaders
 */
vec3 rgb2hsb(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(
        abs(q.z + (q.w - q.y) / (6.0 * d + e)),
        d / (q.x + e),
        q.x
    );
}

// ═══════════════════════════════════════════════════════════════
// 📺 YUV - Espacio de Color Cinematográfico
// ═══════════════════════════════════════════════════════════════

/**
 * Convierte YUV a RGB.
 * YUV separa luminancia (Y) de crominancia (UV).
 * Usado en video/TV para comprimir color.
 *
 * @param yuv   vec3(Y, U, V) donde Y=luminancia (0-1), U y V=crominancia (-0.5 a 0.5)
 * @return      Color RGB (0-1)
 *
 * Uso:
 *   vec3 color = yuv2rgb(vec3(0.5, u * 0.5, v * 0.5));
 *
 * VENTAJA: Puedes animar luminancia y color por separado.
 *
 * Fuente: The Book of Shaders - Matrix chapter
 */
vec3 yuv2rgb(vec3 yuv) {
    mat3 yuv2rgbMatrix = mat3(
        1.0,      1.0,      1.0,
        0.0,     -0.39465, 2.03211,
        1.13983, -0.58060, 0.0
    );
    return yuv2rgbMatrix * yuv;
}

/**
 * Convierte RGB a YUV.
 *
 * @param rgb   Color RGB (0-1)
 * @return      vec3(Y, U, V)
 *
 * Uso:
 *   vec3 yuv = rgb2yuv(originalColor);
 *   yuv.x *= 1.2;  // Aumentar brillo
 *   vec3 brighter = yuv2rgb(yuv);
 */
vec3 rgb2yuv(vec3 rgb) {
    mat3 rgb2yuvMatrix = mat3(
        0.2126,  0.7152,  0.0722,
        -0.09991, -0.33609, 0.43600,
        0.615,   -0.5586, -0.05639
    );
    return rgb2yuvMatrix * rgb;
}

// ═══════════════════════════════════════════════════════════════
// 🎨 PALETAS PROCEDURALES - Técnica de Inigo Quilez
// ═══════════════════════════════════════════════════════════════

/**
 * Genera paletas de colores suaves y continuas.
 * Técnica famosa de Inigo Quilez (creador de Shadertoy).
 *
 * @param t     Valor de entrada (0-1), típicamente u_Time o coordenada
 * @param a     vec3 - Offset base
 * @param b     vec3 - Amplitud
 * @param c     vec3 - Frecuencia
 * @param d     vec3 - Fase
 * @return      Color RGB generado proceduralmente
 *
 * Uso - Paletas Predefinidas:
 *
 *   // SUNSET (Atardecer cálido)
 *   vec3 sunset = palette(t,
 *       vec3(0.5, 0.5, 0.5),
 *       vec3(0.5, 0.5, 0.5),
 *       vec3(1.0, 1.0, 1.0),
 *       vec3(0.0, 0.33, 0.67)
 *   );
 *
 *   // OCEAN (Océano profundo)
 *   vec3 ocean = palette(t,
 *       vec3(0.5, 0.5, 0.5),
 *       vec3(0.5, 0.5, 0.5),
 *       vec3(1.0, 1.0, 1.0),
 *       vec3(0.3, 0.2, 0.2)
 *   );
 *
 *   // NEON (Colores vibrantes cyberpunk)
 *   vec3 neon = palette(t,
 *       vec3(0.5, 0.5, 0.5),
 *       vec3(0.5, 0.5, 0.5),
 *       vec3(2.0, 1.0, 0.0),
 *       vec3(0.5, 0.2, 0.25)
 *   );
 *
 * Fuente: https://iquilezles.org/articles/palettes/
 */
vec3 palette(float t, vec3 a, vec3 b, vec3 c, vec3 d) {
    return a + b * cos(6.28318 * (c * t + d));
}

// ═══════════════════════════════════════════════════════════════
// 🌟 FUNCIONES DE COLOR ÚTILES
// ═══════════════════════════════════════════════════════════════

/**
 * Invierte un color (negativo).
 *
 * @param c     Color RGB (0-1)
 * @return      Color invertido
 *
 * Uso:
 *   vec3 negative = invertColor(originalColor);
 */
vec3 invertColor(vec3 c) {
    return vec3(1.0) - c;
}

/**
 * Convierte a escala de grises (luminancia perceptual).
 * Usa pesos correctos para visión humana.
 *
 * @param c     Color RGB (0-1)
 * @return      Valor de gris (0-1)
 *
 * Uso:
 *   float gray = luminance(color);
 *   vec3 grayscale = vec3(gray);
 */
float luminance(vec3 c) {
    return dot(c, vec3(0.299, 0.587, 0.114));
}

/**
 * Ajusta el contraste de un color.
 *
 * @param c         Color RGB (0-1)
 * @param contrast  Factor de contraste (1.0 = sin cambio, >1.0 = más contraste)
 * @return          Color con contraste ajustado
 *
 * Uso:
 *   vec3 highContrast = adjustContrast(color, 1.5);
 */
vec3 adjustContrast(vec3 c, float contrast) {
    return (c - 0.5) * contrast + 0.5;
}

/**
 * Ajusta la saturación de un color.
 *
 * @param c             Color RGB (0-1)
 * @param saturation    Factor de saturación (0.0 = gris, 1.0 = original, >1.0 = supersaturado)
 * @return              Color con saturación ajustada
 *
 * Uso:
 *   vec3 vivid = adjustSaturation(color, 1.5);      // Más saturado
 *   vec3 desaturated = adjustSaturation(color, 0.5); // Menos saturado
 */
vec3 adjustSaturation(vec3 c, float saturation) {
    float lum = luminance(c);
    return mix(vec3(lum), c, saturation);
}

/**
 * Mezcla dos colores con un gradiente suave.
 *
 * @param colorA    Primer color
 * @param colorB    Segundo color
 * @param t         Factor de mezcla (0.0 = colorA, 1.0 = colorB)
 * @return          Color mezclado con smoothstep
 *
 * Uso:
 *   vec3 gradient = smoothMix(red, blue, smoothstep(0.0, 1.0, x));
 */
vec3 smoothMix(vec3 colorA, vec3 colorB, float t) {
    return mix(colorA, colorB, smoothstep(0.0, 1.0, t));
}

/**
 * ════════════════════════════════════════════════════════════
 * 💡 EJEMPLOS PRÁCTICOS
 * ════════════════════════════════════════════════════════════
 *
 * // 1. ARCOÍRIS ANIMADO (HSB)
 * vec2 st = gl_FragCoord.xy / u_Resolution.xy;
 * vec3 rainbow = hsb2rgb(vec3(st.x + u_Time * 0.1, 0.8, 0.9));
 * gl_FragColor = vec4(rainbow, 1.0);
 *
 * // 2. PALETA PROCEDURAL ANIMADA
 * float t = length(st - 0.5) + u_Time * 0.2;
 * vec3 color = palette(t,
 *     vec3(0.5), vec3(0.5), vec3(1.0), vec3(0.0, 0.33, 0.67)
 * );
 * gl_FragColor = vec4(color, 1.0);
 *
 * // 3. EFECTO VINTAGE (YUV)
 * vec3 yuv = rgb2yuv(originalColor);
 * yuv.yz *= 0.6;  // Reducir crominancia (menos color)
 * yuv.x *= 0.9;   // Reducir luminancia (más oscuro)
 * vec3 vintage = yuv2rgb(yuv);
 *
 * // 4. MODIFICAR HUE DE TEXTURA
 * vec3 hsb = rgb2hsb(texture2D(u_Texture, uv).rgb);
 * hsb.x = mod(hsb.x + u_Time * 0.1, 1.0); // Rotar hue
 * vec3 colorShift = hsb2rgb(hsb);
 *
 * ════════════════════════════════════════════════════════════
 */
