// road_scene_fragment.glsl
#version 100
precision mediump float;

// — Uniforms desde tu Renderer en Java/Kotlin —
uniform float u_Time;        // tiempo en segundos
uniform vec2  u_Mouse;       // posición normalizada del “mouse” (0–1)
uniform vec2  u_Resolution;  // resolución del viewport (ancho, alto)

// — Función que dibuja la carretera en perspectiva —
vec3 road(vec3 pos) {
    vec3 c1 = vec3(0.1, 0.9, 0.1);
    vec3 c2 = vec3(0.1, 0.6, 0.1);
    float a = u_Time;
    float k = sin(0.2 * a);
    // efecto de “curva” ondulada en X según el tiempo
    pos.x -= 0.05 * k * k * k * pos.y * pos.y;

    // franjas de carril
    if (abs(pos.x) < 1.0) {
        c1 = vec3(0.9, 0.1, 0.1);
        c2 = vec3(0.9, 0.9, 0.9);
    }
    if (abs(pos.x) < 0.8) {
        c1 = vec3(0.5);
        c2 = vec3(0.5);
    }
    if (abs(pos.x) < 0.002) {
        c1 = vec3(0.5);
        c2 = vec3(0.9);
    }

    float t = u_Time * 15.0;
    float rep  = fract(pos.y + t);
    float blur = dot(pos, pos) * 0.005;
    float f1 = smoothstep(0.25 - blur * 0.25, 0.25 + blur * 0.25, rep);
    float f2 = smoothstep(0.75 + blur * 0.25, 0.75 - blur * 0.25, rep);
    return mix(c1, c2, f1 * f2);
}

// — Función que dibuja el cielo con gradiente vertical —
vec3 sky(vec2 uv) {
    return mix(vec3(1.0), vec3(0.1, 0.7, 1.0), uv.y);
}

// — Función que dibuja un “auto” sencillo controlado por el mouse —
vec3 car(vec2 uv) {
    if (uv.y > -0.3) return vec3(0.0);
    float carpos = u_Mouse.x * 2.0 - 1.0;  // mapeo [0,1]→[-1,1]
    if (abs(uv.x - carpos) < 0.15) {
        // rojo si está apartado >0.4, blanco si centrado
        return abs(carpos) > 0.4
        ? vec3(1.0, 0.0, 0.0)
        : vec3(1.0);
    }
    return vec3(0.0);
}

// — Función auxiliar para mezclar con el fondo solo donde hay el auto —
float insidecar(vec3 col) {
    return length(col) > 0.0 ? 1.0 : 0.0;
}

void main() {
    // 1) Resolución y UV escalados respecto a la altura
    vec2 res = u_Resolution / u_Resolution.y;
    vec2 uv  = gl_FragCoord.xy / u_Resolution.y;
    // 2) Centro de pantalla en (0,0)
    uv -= res * 0.5;

    // 3) Posición 3D en “espacio carretera”: (X/Y perspectiva, profundidad, cielo/tierra)
    vec3 pos = vec3(
    uv.x / abs(uv.y),
    1.0 / abs(uv.y),
    step(0.0, uv.y) * 2.0 - 1.0
    );

    // 4) Mezcla carretera y cielo según pos.z
    vec3 color = mix(road(pos), sky(uv), step(0.0, pos.z));

    // 5) Dibujamos el auto encima
    vec3 carcol = car(uv);
    color = mix(color, carcol, insidecar(carcol));

    // 6) Salida final
    gl_FragColor = vec4(color, 1.0);
}
