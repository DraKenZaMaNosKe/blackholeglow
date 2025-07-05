// left_rainbow_wavy_frame_fragment.glsl
#version 100
precision highp float;

// Uniforms
uniform vec2  u_Resolution;   // resolución del viewport
uniform float u_Time;         // tiempo en segundos

// Convierte HSV a RGB
vec3 hsv2rgb(vec3 c) {
    vec3 p = abs(mod(c.x * 6.0 + vec3(0.0,4.0,2.0), 6.0) - 3.0) - 1.0;
    return c.z * mix(vec3(1.0), clamp(p, 0.0, 1.0), c.y);
}

void main() {
    // Normaliza a [0,1]
    vec2 uv = gl_FragCoord.xy / u_Resolution;

    // Parámetros del marco
    float widthUV      = 0.01;   // 1% del ancho total
    float marginX      = 0.001;   // 5% desde la izquierda
    float marginY      = 0.05;   // 5% arriba y abajo
    float cornerRadius = 0.005;  // 0.5% de radio en esquinas
    float waveAmp      = 0.009;
    float waveFreq     = 2.6;    // frecuencia 1.3×2.0
    float t            = u_Time * 5.5;

    // Calcula centro y semidimensiones
    float halfW = widthUV * 0.5;
    float halfH = (1.0 - 2.0 * marginY) * 0.5;
    vec2  center = vec2(marginX + halfW, 0.5);
    vec2  p = uv - center;

    // Bordes ondulados
    float waveX = sin(uv.y * waveFreq + t) * waveAmp;
    float wW    = halfW + waveX;
    float waveY = sin(uv.x * waveFreq + t) * waveAmp;
    float wH    = halfH + waveY;

    // SDF rectángulo redondeado ondulado
    vec2 q    = abs(p) - vec2(wW, wH);
    vec2 qmax = max(q, vec2(0.0));
    float distRect = length(qmax) - cornerRadius;

    // Definimos el ancho de borde
    float borderWidth = widthUV;

    // Seleccionamos solo la franja de borde
    // distRect ∈ [-borderWidth, 0]
    if (distRect > 0.0 || distRect < -borderWidth) {
        discard;
    }

    // Factor de intensidad según cercanía al centro de la franja
    float f = clamp(1.0 + distRect / borderWidth, 0.0, 1.0);

    // Hue animado en función de la posición vertical y el tiempo
    float hue = fract(uv.y + u_Time * 0.2);

    // Color arcoíris puro
    vec3 rgb = hsv2rgb(vec3(hue, 1.0, 1.0));

    // Salida con atenuación f
    gl_FragColor = vec4(rgb * f, 1.0);
}
