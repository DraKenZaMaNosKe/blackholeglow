// FireButton Fragment Shader - Botón con efectos + Cooldown Ring
precision mediump float;

uniform vec2 u_ButtonPos;
uniform float u_Radius;
uniform vec2 u_Resolution;
uniform float u_Pressed;        // 0.0 = normal, 1.0 = presionado
uniform float u_CooldownProgress; // 0.0 = inicio cooldown, 1.0 = listo

varying vec2 v_Position;

#define PI 3.14159265359

void main() {
    // Compensar aspect ratio
    float aspectRatio = u_Resolution.x / u_Resolution.y;
    vec2 relativePos = v_Position - u_ButtonPos;
    relativePos.x *= aspectRatio;

    float dist = length(relativePos);
    float normalizedDist = dist / u_Radius;

    // Fuera del botón
    if (dist > u_Radius) {
        gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
        return;
    }

    // ═══════════════════════════════════════════
    // 🎨 COLORES DEL BOTÓN
    // ═══════════════════════════════════════════
    vec3 colorNormal = vec3(0.2, 0.8, 0.3);      // Verde normal
    vec3 colorPressed = vec3(0.1, 0.6, 0.2);     // Verde oscuro presionado
    vec3 colorBorder = vec3(0.0, 1.0, 0.4);      // Verde brillante borde

    // Interpolar entre normal y presionado
    vec3 baseColor = mix(colorNormal, colorPressed, u_Pressed);

    // ═══════════════════════════════════════════
    // 🔘 GRADIENTE RADIAL (efecto 3D)
    // ═══════════════════════════════════════════
    float gradient = 1.0 - normalizedDist * 0.5;
    vec3 color = baseColor * gradient;

    // ═══════════════════════════════════════════
    // ⭕ BORDE BRILLANTE
    // ═══════════════════════════════════════════
    float borderWidth = 0.08;
    float borderStart = 1.0 - borderWidth;

    if (normalizedDist > borderStart) {
        float borderFactor = (normalizedDist - borderStart) / borderWidth;
        color = mix(color, colorBorder, borderFactor * 0.8);
    }

    // ═══════════════════════════════════════════
    // ✨ BRILLO CENTRAL
    // ═══════════════════════════════════════════
    float highlight = smoothstep(0.6, 0.0, normalizedDist) * 0.3;
    color += vec3(highlight);

    // ═══════════════════════════════════════════
    // ⏱️ ANILLO DE COOLDOWN
    // ═══════════════════════════════════════════
    if (u_CooldownProgress < 1.0) {
        // Anillo exterior (entre 0.85 y 1.0 del radio)
        float ringOuterStart = 0.85;
        float ringInnerStart = 0.75;

        if (normalizedDist > ringInnerStart && normalizedDist < 1.0) {
            // Calcular ángulo (0° = arriba, sentido horario)
            vec2 dir = relativePos / dist;
            float angle = atan(dir.x, dir.y);  // -PI a PI
            angle = angle / (2.0 * PI) + 0.5;   // 0.0 a 1.0

            // El anillo se llena en sentido horario desde arriba
            if (angle <= u_CooldownProgress) {
                // Color del anillo de cooldown (amarillo/naranja)
                vec3 cooldownColor = vec3(1.0, 0.8, 0.0);

                // Fade del anillo
                float ringFade = smoothstep(ringInnerStart, ringInnerStart + 0.05, normalizedDist) *
                                 (1.0 - smoothstep(ringOuterStart, 1.0, normalizedDist));

                // Mezclar con el color existente
                color = mix(color, cooldownColor, ringFade * 0.9);
            }
        }
    }

    // ═══════════════════════════════════════════
    // 🎯 SUAVIZAR BORDES (antialiasing)
    // ═══════════════════════════════════════════
    float edgeSoftness = 0.02;
    float alpha = 1.0 - smoothstep(1.0 - edgeSoftness, 1.0, normalizedDist);

    gl_FragColor = vec4(color, alpha);
}
