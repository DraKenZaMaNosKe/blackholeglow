precision highp float;

uniform float u_Time;           // Tiempo en segundos (incrementa continuamente)
varying vec2 v_TexCoord;        // Coordenadas de textura
uniform sampler2D u_Texture;    // Textura base

uniform int   u_UseSolidColor;  // 1 = uso color sólido, 0 = uso textura
uniform vec4  u_SolidColor;     // Color sólido RGBA
uniform float u_Alpha;          // Alpha global

// Parámetros fijos para el glow solar
const float GLOW_RADIUS    = 0.001;
const float NOISE_SCALE    = 8.5;
const float NOISE_SPEED    = 0.23;
const float FLARE_FREQ     = 5.7;
const float FLARE_MAG      = 0.049;
const float COLOR_FREQ     = 3.2;
const float GLOW_ALPHA     = 0.1;

// Parámetros de animación para CORE_RADIUS y OUTER_FADE
const float CORE_RADIUS_MAX    = 0.01;   // Valor máximo de CORE_RADIUS
const float OUTER_FADE_MAX     = 0.77;  // Valor máximo de OUTER_FADE
const float CORE_OSC_FREQ      = 0.0;   // Frecuencia (ciclos por segundo) para CORE_RADIUS
const float OUTER_OSC_FREQ     = 0.005;   // Frecuencia para OUTER_FADE

// Función swirl existente para distorsionar UV
float swirl(vec2 uv, float t) {
    float a = atan(uv.y - 0.5, uv.x - 0.5);
    float d = length(uv - vec2(0.5));
    float s = 0.24 * sin(a * NOISE_SCALE + d * 7.3 - t * NOISE_SPEED * 0.72);
    s += 0.11 * cos(a * (NOISE_SCALE * 0.85) + t * 0.61 + d * 5.2);
    s += 0.07 * sin(a * 3.2 + t * 0.45);
    return s;
}

void main() {
    // 1) Carga base de color/textura y aplica alpha global
    vec4 base  = texture2D(u_Texture, v_TexCoord);
    vec4 color = (u_UseSolidColor == 1) ? u_SolidColor : base;
    color.a   *= u_Alpha;

    // 2) Calcula valores animados usando seno para oscilar entre 0.0 y el máximo
    //    Mapeo: sin() ∈ [-1,1] -> (sin*0.5+0.5) ∈ [0,1] -> *MAX → ∈ [0,MAX]
    float coreRadius  = CORE_RADIUS_MAX * (0.5 + 0.5 * sin(u_Time * CORE_OSC_FREQ * 2.0 * 3.14159));
    float outerFade   = OUTER_FADE_MAX  * (0.5 + 0.5 * sin(u_Time * OUTER_OSC_FREQ * 2.0 * 3.14159));

    if (u_UseSolidColor == 1) {
        vec2 uv = v_TexCoord;
        float d = length(uv - vec2(0.5));
        float pulse = 0.94 + 0.09 * sin(u_Time * 0.87 + d * 7.5);

        // Núcleo súper brillante, ahora variable en tamaño
        if (d < coreRadius) {
            vec3 coreColor = mix(
            vec3(1.0, 0.62, 0.15),
            vec3(1.19, 0.38, 0.12),
            smoothstep(0.0, coreRadius, d)
            );
            color.rgb = mix(color.rgb, coreColor, 0.97);
            color.a   = 1.0;
        }
        // Glow intermedio
        else if (d < GLOW_RADIUS) {
            float t        = u_Time * 0.6;
            float swirlFx  = swirl(uv, t) * 0.27;
            float flare    = FLARE_MAG * sin(
            FLARE_FREQ * atan(uv.y - 0.5, uv.x - 0.5)
            + t * 0.8 + d * 9.3
            );
            float border   = GLOW_RADIUS + swirlFx + flare - d;
            float glow     = smoothstep(0.07, 0.18, border) * pulse;

            float cMix     = smoothstep(coreRadius, GLOW_RADIUS, d)
            + 0.08 * sin(d * COLOR_FREQ - t * 0.34);

            vec3 auraColor = mix(
            vec3(1.0, 0.34, 0.13),
            vec3(0.75, 0.11, 0.11),
            cMix
            );
            auraColor = mix(auraColor, vec3(0.42, 0.14, 0.32),
            smoothstep(0.76, 1.05, cMix));

            color.rgb += auraColor * glow * 1.07;
            color.a   += glow * GLOW_ALPHA * (0.86 + 0.14 * sin(t * 0.85 + d * 11.0));
        }
        // Fade externo, ahora variable
        else if (d < outerFade) {
            float fading   = 1.0 - smoothstep(GLOW_RADIUS, outerFade, d);
            vec3 fadeColor = mix(
            vec3(0.82, 0.12, 0.12),
            vec3(0.22, 0.05, 0.18),
            fading
            );
            color.rgb += fadeColor * fading * 0.22;
            color.a   += fading * 0.22;
        }
    }

    // Descarta fragmentos casi transparentes para limpieza
    if (color.a < 0.01) discard;
    gl_FragColor = color;
}
