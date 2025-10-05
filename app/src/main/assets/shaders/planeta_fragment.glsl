// ============================================
// archivo: planeta_fragment.glsl
// Shader del planeta con efectos de glow mejorados usando HSB (SIN TEXTURA)
// ============================================

#ifdef GL_ES
precision mediump float;
#endif

uniform float u_Time;
varying vec2 v_TexCoord;
varying vec3 v_WorldPos;
uniform vec4 u_SolidColor;
uniform float u_Alpha;

// Parámetros de glow
const float GLOW_RADIUS = 0.45;
const float CORE_RADIUS = 0.2;

// ============================================
// Funciones de conversión RGB ↔ HSB
// De The Book of Shaders / Iñigo Quilez
// ============================================

vec3 rgb2hsb(in vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz),
                 vec4(c.gb, K.xy),
                 step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r),
                 vec4(c.r, p.yzx),
                 step(p.x, c.r));
    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)),
    d / (q.x + e),
    q.x);
}

vec3 hsb2rgb(in vec3 c) {
    vec3 rgb = clamp(abs(mod(c.x * 6.0 + vec3(0.0, 4.0, 2.0),
                             6.0) - 3.0) - 1.0,
                     0.0,
                     1.0);
    rgb = rgb * rgb * (3.0 - 2.0 * rgb);
    return c.z * mix(vec3(1.0), rgb, c.y);
}

void main() {
    // Usar solo color sólido (sin textura)
    vec4 color = u_SolidColor;
    color.a *= u_Alpha;

    vec2 center = vec2(0.5, 0.5);
    float dist = length(v_TexCoord - center);

    // Convertir color base a HSB para manipulación más natural
    vec3 hsb = rgb2hsb(color.rgb);

    // Núcleo brillante solar
    if (dist < CORE_RADIUS) {
        float intensity = 1.0 - (dist / CORE_RADIUS);

        // Aumentar brillo (brightness) en el núcleo
        hsb.z = min(hsb.z + intensity * 0.5, 1.0);

        // Reducir saturación en el centro para efecto "blanco caliente"
        hsb.y = hsb.y * (1.0 - intensity * 0.4);

        // Añadir tinte dorado variando el hue
        hsb.x = hsb.x + intensity * 0.05; // Shift hacia amarillo/dorado

        color.rgb = hsb2rgb(hsb);
        color.a = 1.0;
    }
    // Halo exterior con gradiente suave
    else if (dist < GLOW_RADIUS) {
        float fade = 1.0 - ((dist - CORE_RADIUS) / (GLOW_RADIUS - CORE_RADIUS));

        // Pulsación animada
        float pulse = sin(u_Time * 3.0 + dist * 10.0) * 0.1 + 0.9;

        // Efecto de "swirl" rotacional en el halo
        float angle = atan(v_TexCoord.y - center.y, v_TexCoord.x - center.x);
        float swirl = sin(angle * 3.0 + u_Time * 2.0) * 0.5 + 0.5;

        // Modular hue para crear variaciones de color en el halo
        hsb.x = hsb.x + swirl * 0.1;

        // Aumentar saturación en el halo para colores más vivos
        hsb.y = min(hsb.y * (1.0 + fade * 0.3), 1.0);

        // Brightness con fade
        hsb.z = hsb.z * pulse * fade;

        color.rgb = hsb2rgb(hsb);
        color.a *= fade;
    }
    else {
        discard; // No dibujar fuera del glow
    }

    gl_FragColor = color;
}