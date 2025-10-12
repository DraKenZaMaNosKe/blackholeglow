// Fragment shader para estelas hermosas de plasma y fuego
precision mediump float;

uniform float u_Time;
uniform float u_TrailType;  // 0 = fuego, 0.5 = plasma, 1 = arcoíris

varying vec4 v_Color;
varying float v_Age;
varying vec2 v_UV;

// Función de ruido para efectos
float noise(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

// Gradiente de fuego mejorado
vec3 fireGradient(float t) {
    // Colores de fuego más vibrantes
    vec3 color;
    if (t < 0.2) {
        color = mix(vec3(0.5, 0.0, 0.0), vec3(1.0, 0.0, 0.0), t * 5.0);  // Rojo oscuro a rojo
    } else if (t < 0.4) {
        color = mix(vec3(1.0, 0.0, 0.0), vec3(1.0, 0.5, 0.0), (t - 0.2) * 5.0);  // Rojo a naranja
    } else if (t < 0.7) {
        color = mix(vec3(1.0, 0.5, 0.0), vec3(1.0, 0.9, 0.0), (t - 0.4) * 3.33);  // Naranja a amarillo
    } else {
        color = mix(vec3(1.0, 0.9, 0.0), vec3(1.0, 1.0, 0.8), (t - 0.7) * 3.33);  // Amarillo a blanco
    }
    return color;
}

// Efecto de plasma eléctrico mejorado
vec3 plasmaGradient(float t, vec2 uv) {
    // Ondas eléctricas
    float wave1 = sin(uv.x * 15.0 + u_Time * 5.0) * 0.5 + 0.5;
    float wave2 = cos(uv.y * 12.0 - u_Time * 4.0) * 0.5 + 0.5;
    float electric = wave1 * wave2;

    // Colores de plasma más vibrantes
    vec3 color;
    if (t < 0.25) {
        color = mix(vec3(0.0, 0.0, 0.3), vec3(0.0, 0.3, 1.0), t * 4.0);  // Azul oscuro a azul
    } else if (t < 0.5) {
        color = mix(vec3(0.0, 0.3, 1.0), vec3(0.0, 0.8, 1.0), (t - 0.25) * 4.0);  // Azul a cyan
    } else if (t < 0.75) {
        color = mix(vec3(0.0, 0.8, 1.0), vec3(0.5, 1.0, 1.0), (t - 0.5) * 4.0);  // Cyan a cyan claro
    } else {
        color = mix(vec3(0.5, 1.0, 1.0), vec3(1.0, 1.0, 1.0), (t - 0.75) * 4.0);  // Cyan claro a blanco
    }

    // Añadir efecto eléctrico
    color += vec3(0.2, 0.5, 1.0) * electric * 0.3;

    // Chispas eléctricas ocasionales
    float spark = noise(uv * 30.0 + u_Time * 8.0);
    if (spark > 0.97) {
        color = vec3(0.7, 0.9, 1.0) * 2.0;
    }

    return color;
}

void main() {
    // Turbulencia para más dinamismo
    vec2 turbUV = v_UV + vec2(
        noise(v_UV * 8.0 + u_Time) * 0.05,
        noise(v_UV * 8.0 - u_Time) * 0.05
    );

    // Color base de la estela según el tipo
    vec3 trailColor;
    float glow = 1.0 - v_Age;  // Más brillante al principio

    if (u_TrailType < 0.3) {
        // Estela de fuego
        trailColor = fireGradient(v_Age);

        // Efecto de llamas
        float flame = noise(turbUV * 20.0 + u_Time * 3.0);
        trailColor *= (1.0 + flame * 0.3);

        // Chispas de fuego
        float sparks = noise(v_UV * 50.0 + u_Time * 5.0);
        if (sparks > 0.95) {
            trailColor += vec3(1.0, 0.8, 0.3) * 2.0;
        }

    } else if (u_TrailType < 0.7) {
        // Estela de plasma
        trailColor = plasmaGradient(v_Age, turbUV);

        // Efecto de energía pulsante
        float energy = sin(u_Time * 10.0 - v_Age * 5.0) * 0.3 + 0.7;
        trailColor *= energy;

        // Rayos de energía
        float lightning = pow(noise(turbUV * 15.0 + u_Time * 7.0), 3.0);
        trailColor += vec3(0.3, 0.6, 1.0) * lightning * 2.0;

    } else {
        // Estela arcoíris (bonus)
        float hue = v_Age * 3.0 + u_Time * 2.0;
        trailColor.r = sin(hue) * 0.5 + 0.5;
        trailColor.g = sin(hue + 2.094) * 0.5 + 0.5;
        trailColor.b = sin(hue + 4.188) * 0.5 + 0.5;
        trailColor *= 1.5;  // Más brillante
    }

    // Efecto de brillo en los bordes
    float edgeGlow = 1.0 - abs(v_UV.y - 0.5) * 2.0;
    edgeGlow = pow(edgeGlow, 2.0);
    trailColor += trailColor * edgeGlow * 0.5;

    // Pulso de energía general
    float pulse = sin(u_Time * 8.0 - v_Age * 3.14) * 0.2 + 0.8;
    trailColor *= pulse;

    // Intensificar el brillo
    trailColor *= (1.0 + glow * 0.5);

    // Alpha con desvanecimiento suave
    float alpha = v_Color.a * (1.0 - v_Age);
    alpha = pow(alpha, 0.8);  // Desvanecimiento más suave

    // Efecto de disolución en la cola
    float dissolution = noise(v_UV * 25.0 + u_Time * 4.0);
    if (v_Age > 0.6) {
        alpha *= (1.0 - (v_Age - 0.6) * 2.5);
        if (dissolution > 0.4 + v_Age * 0.4) {
            alpha *= 0.3;
        }
    }

    // Asegurar que el inicio de la estela sea brillante
    if (v_Age < 0.1) {
        alpha = max(alpha, 0.8);
        trailColor *= 1.5;
    }

    gl_FragColor = vec4(trailColor, alpha);
}