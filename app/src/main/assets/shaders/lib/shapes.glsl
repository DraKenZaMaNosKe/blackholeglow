/**
 * ╔════════════════════════════════════════════════════════════════╗
 * ║  ⭐ SHAPES.GLSL - Distance Fields y Formas Geométricas       ║
 * ║  Black Hole Glow Shader Library v1.0.0                       ║
 * ╚════════════════════════════════════════════════════════════════╝
 *
 * Distance Fields: Técnica para dibujar formas perfectas SIN texturas.
 * Ventajas:
 * - Bordes perfectamente suaves
 * - Escalables sin pérdida de calidad
 * - Muy ligeros (solo matemáticas)
 * - Fáciles de animar y transformar
 *
 * Basado en "The Book of Shaders" e Inigo Quilez
 * https://iquilezles.org/articles/distfunctions2d/
 */

#ifdef GL_ES
precision mediump float;
#endif

#define PI 3.14159265359
#define TWO_PI 6.28318530718

// ═══════════════════════════════════════════════════════════════
// 📐 COORDENADAS POLARES
// ═══════════════════════════════════════════════════════════════

/**
 * Convierte coordenadas cartesianas a polares.
 * ESENCIAL para formas radiales (círculos, mandalas, flores).
 *
 * @param st    Coordenadas cartesianas (centradas en 0,0)
 * @return      vec2(radio, ángulo)
 *              - radio: distancia desde el centro (0 a ~0.707)
 *              - ángulo: ángulo en radianes (0 a TWO_PI)
 *
 * Uso:
 *   vec2 st = gl_FragCoord.xy / u_Resolution.xy - 0.5;
 *   vec2 polar = toPolar(st);
 *   float r = polar.x;  // Radio
 *   float a = polar.y;  // Ángulo
 *
 * Fuente: The Book of Shaders - Polar Shapes
 */
vec2 toPolar(vec2 st) {
    return vec2(
        length(st),
        atan(st.y, st.x)
    );
}

/**
 * Normaliza ángulo a rango 0-1.
 * Útil para mapear ángulos a colores o texturas.
 *
 * @param angle     Ángulo en radianes (-PI a PI)
 * @return          Ángulo normalizado (0.0 a 1.0)
 *
 * Uso:
 *   float normalizedAngle = normalizeAngle(polar.y);
 *   vec3 color = hsb2rgb(vec3(normalizedAngle, 0.8, 0.9));
 */
float normalizeAngle(float angle) {
    return (angle + PI) / TWO_PI;
}

// ═══════════════════════════════════════════════════════════════
// ⭕ DISTANCE FIELDS - FORMAS BÁSICAS
// ═══════════════════════════════════════════════════════════════

/**
 * Círculo perfecto usando Signed Distance Field (SDF).
 * Retorna la DISTANCIA al borde del círculo.
 *
 * @param st        Coordenadas (centradas recomendado)
 * @param radius    Radio del círculo
 * @return          Distancia al borde (negativo=dentro, positivo=fuera, 0=borde)
 *
 * Uso:
 *   vec2 st = gl_FragCoord.xy / u_Resolution.xy - 0.5;
 *   float d = sdCircle(st, 0.2);
 *   float circle = step(0.0, -d);  // Relleno sólido
 *   float border = smoothstep(0.0, 0.01, abs(d));  // Solo borde suave
 *
 * Fuente: Inigo Quilez
 */
float sdCircle(vec2 st, float radius) {
    return length(st) - radius;
}

/**
 * Rectángulo/Caja con bordes rectos.
 *
 * @param st    Coordenadas (centradas)
 * @param size  vec2(ancho, alto) del rectángulo
 * @return      Distancia al borde
 *
 * Uso:
 *   float d = sdBox(st, vec2(0.3, 0.2));
 *   float box = step(0.0, -d);
 *
 * Fuente: Inigo Quilez
 */
float sdBox(vec2 st, vec2 size) {
    vec2 d = abs(st) - size;
    return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0);
}

/**
 * Rectángulo con esquinas redondeadas.
 *
 * @param st            Coordenadas (centradas)
 * @param size          vec2(ancho, alto)
 * @param cornerRadius  Radio de las esquinas redondeadas
 * @return              Distancia al borde
 *
 * Uso:
 *   float d = sdRoundedBox(st, vec2(0.3, 0.2), 0.05);
 */
float sdRoundedBox(vec2 st, vec2 size, float cornerRadius) {
    vec2 d = abs(st) - size + cornerRadius;
    return length(max(d, 0.0)) - cornerRadius + min(max(d.x, d.y), 0.0);
}

/**
 * Polígono regular de N lados (triángulo, hexágono, etc).
 * Usa coordenadas POLARES internamente.
 *
 * @param st    Coordenadas (centradas)
 * @param N     Número de lados (3=triángulo, 4=cuadrado, 6=hexágono)
 * @return      Distancia aproximada al borde
 *
 * Uso:
 *   float triangle = sdPolygon(st, 3);
 *   float hexagon = sdPolygon(st, 6);
 *   float star = sdPolygon(st, 5);  // Pentágono (casi estrella)
 *
 * Fuente: The Book of Shaders - Polar Shapes
 */
float sdPolygon(vec2 st, int N) {
    vec2 polar = toPolar(st);
    float r = polar.x;
    float a = polar.y;

    // Simetría radial
    float segment = TWO_PI / float(N);
    a = mod(a, segment) - segment * 0.5;

    // Distancia al borde del polígono
    return r - cos(a) * 0.5;
}

// ═══════════════════════════════════════════════════════════════
// ⭐ FORMAS PROCEDURALES AVANZADAS
// ═══════════════════════════════════════════════════════════════

/**
 * Estrella de N puntas usando modulación del radio.
 *
 * @param st        Coordenadas (centradas)
 * @param points    Número de puntas de la estrella
 * @param radius    Radio base
 * @param sharpness Factor de agudeza de las puntas (0.0-1.0)
 * @return          Distancia al borde
 *
 * Uso:
 *   float star5 = sdStar(st, 5, 0.2, 0.5);  // Estrella de 5 puntas
 *   float star8 = sdStar(st, 8, 0.25, 0.3); // Estrella de 8 puntas suave
 */
float sdStar(vec2 st, int points, float radius, float sharpness) {
    vec2 polar = toPolar(st);
    float r = polar.x;
    float a = polar.y;

    // Modular radio según ángulo
    float segment = TWO_PI / float(points);
    float modulation = cos(a * float(points)) * sharpness + (1.0 - sharpness);

    return r - radius * modulation;
}

/**
 * Flor/Mandala con pétalos suaves.
 *
 * @param st        Coordenadas (centradas)
 * @param petals    Número de pétalos
 * @param size      Tamaño de la flor
 * @return          Distancia al borde
 *
 * Uso:
 *   float flower = sdFlower(st, 6, 0.3);
 */
float sdFlower(vec2 st, int petals, float size) {
    vec2 polar = toPolar(st);
    float r = polar.x;
    float a = polar.y;

    // Pétalos suaves con coseno
    float petalShape = abs(cos(a * float(petals) * 0.5)) * 0.5 + 0.5;

    return r - size * petalShape;
}

/**
 * Cruz/Plus perfecta.
 *
 * @param st        Coordenadas (centradas)
 * @param thickness Grosor de los brazos
 * @param size      Tamaño total
 * @return          Distancia al borde
 *
 * Uso:
 *   float cross = sdCross(st, 0.05, 0.3);
 */
float sdCross(vec2 st, float thickness, float size) {
    vec2 d = abs(st);
    float crossH = sdBox(st, vec2(size, thickness));
    float crossV = sdBox(st, vec2(thickness, size));
    return min(crossH, crossV);
}

// ═══════════════════════════════════════════════════════════════
// 🔄 TRANSFORMACIONES 2D
// ═══════════════════════════════════════════════════════════════

/**
 * Matriz de rotación 2D.
 * Usa esto para rotar formas alrededor del origen.
 *
 * @param angle     Ángulo en radianes
 * @return          Matriz de rotación 2x2
 *
 * Uso:
 *   vec2 st = gl_FragCoord.xy / u_Resolution.xy - 0.5;
 *   st = rotate2d(u_Time) * st;  // Rotar coordenadas
 *   float shape = sdBox(st, vec2(0.2));
 *
 * Fuente: The Book of Shaders - Matrix
 */
mat2 rotate2d(float angle) {
    return mat2(
        cos(angle), -sin(angle),
        sin(angle),  cos(angle)
    );
}

/**
 * Escala coordenadas desde el centro.
 *
 * @param st        Coordenadas
 * @param scale     Factor de escala (1.0 = sin cambio)
 * @return          Coordenadas escaladas
 *
 * Uso:
 *   st = scaleFromCenter(st, 2.0);  // 2x más grande
 */
vec2 scaleFromCenter(vec2 st, float scale) {
    return st / scale;
}

/**
 * Repite el espacio en mosaico (tiling).
 *
 * @param st        Coordenadas
 * @param tiles     Número de repeticiones
 * @return          Coordenadas repetidas (0-1 en cada tile)
 *
 * Uso:
 *   vec2 tiled = tile(st * 5.0);  // 5x5 mosaico
 *   float circle = sdCircle(tiled - 0.5, 0.3);
 */
vec2 tile(vec2 st) {
    return fract(st);
}

// ═══════════════════════════════════════════════════════════════
// 🎨 OPERACIONES BOOLEANAS (Combinar Formas)
// ═══════════════════════════════════════════════════════════════

/**
 * Unión de dos formas (OR).
 *
 * @param d1    Distancia de forma 1
 * @param d2    Distancia de forma 2
 * @return      Distancia combinada
 */
float opUnion(float d1, float d2) {
    return min(d1, d2);
}

/**
 * Intersección de dos formas (AND).
 *
 * @param d1    Distancia de forma 1
 * @param d2    Distancia de forma 2
 * @return      Distancia combinada
 */
float opIntersection(float d1, float d2) {
    return max(d1, d2);
}

/**
 * Sustracción (forma 1 MENOS forma 2).
 *
 * @param d1    Distancia de forma 1
 * @param d2    Distancia de forma 2
 * @return      Distancia combinada
 */
float opSubtraction(float d1, float d2) {
    return max(d1, -d2);
}

/**
 * Unión suave (blend orgánico).
 *
 * @param d1        Distancia de forma 1
 * @param d2        Distancia de forma 2
 * @param k         Factor de suavidad
 * @return          Distancia combinada suavemente
 *
 * Fuente: Inigo Quilez
 */
float opSmoothUnion(float d1, float d2, float k) {
    float h = clamp(0.5 + 0.5 * (d2 - d1) / k, 0.0, 1.0);
    return mix(d2, d1, h) - k * h * (1.0 - h);
}

/**
 * ════════════════════════════════════════════════════════════
 * 💡 EJEMPLOS DE USO
 * ════════════════════════════════════════════════════════════
 *
 * // 1. CÍRCULO CON BORDE SUAVE
 * vec2 st = gl_FragCoord.xy / u_Resolution.xy - 0.5;
 * float d = sdCircle(st, 0.2);
 * float circle = smoothstep(0.01, 0.0, d);
 * gl_FragColor = vec4(vec3(circle), 1.0);
 *
 * // 2. HEXÁGONO ROTANDO
 * st = rotate2d(u_Time) * st;
 * float hex = sdPolygon(st, 6);
 * float shape = step(0.0, -hex);
 *
 * // 3. FLORES REPETIDAS
 * vec2 tiled = tile(st * 3.0) - 0.5;
 * float flower = sdFlower(tiled, 6, 0.3);
 * float pattern = smoothstep(0.01, 0.0, flower);
 *
 * // 4. DOS CÍRCULOS UNIDOS
 * float c1 = sdCircle(st - vec2(-0.1, 0.0), 0.15);
 * float c2 = sdCircle(st - vec2(0.1, 0.0), 0.15);
 * float combined = opSmoothUnion(c1, c2, 0.1);
 *
 * ════════════════════════════════════════════════════════════
 */
