// left_rainbow_water_wavy_frame_fragment.glsl
#version 100
precision highp float;

// Uniforms
uniform vec2  u_Resolution;
uniform float u_Time;

// Conversión HSV → RGB
vec3 hsv2rgb(vec3 c) {
    vec3 p = abs(mod(c.x * 6.0 + vec3(0.0,4.0,2.0), 6.0) - 3.0) - 1.0;
    return c.z * mix(vec3(1.0), clamp(p,0.0,1.0), c.y);
}

// SDF de rectángulo redondeado
float sdRoundedRect(vec2 p, vec2 halfSize, float r) {
    vec2 d = abs(p) - halfSize;
    vec2 dm = max(d, vec2(0.0));
    return length(dm) - r;
}

// Ruido “Perlin-like” 2D rápido
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1,311.7))) * 43758.5453123);
}
float noise2d(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    // cuatro esquinas
    float a = hash(i + vec2(0.0,0.0));
    float b = hash(i + vec2(1.0,0.0));
    float c = hash(i + vec2(0.0,1.0));
    float d = hash(i + vec2(1.0,1.0));
    // suavizado cúbico
    vec2 u = f*f*(3.0-2.0*f);
    // interpola
    return mix(a, b, u.x)
    + (c - a)*u.y*(1.0 - u.x)
    + (d - b)*u.x*u.y;
}

void main() {
    vec2 uv = gl_FragCoord.xy / u_Resolution;
    float t   = u_Time;

    // Parámetros
    float thW       = 0.01;         // grosor vertical = 1%
    float thH       = 0.01;         // grosor horizontal = 1%
    float glowIn    = 0.02;         // halo interior
    float glowOut   = glowIn * 3.0; // halo difuso
    float cornerRad = 0.005;        // radio esquinas
    float waveAmp   = 0.005;        // amp base onda
    float waveFreq  = 2.6;
    float waveSpeed = 5.5;
    float rainSpeed = 0.2;
    float noiseAmp  = 0.002;        // amp de distorsión
    float noiseFreq = 5.0;          // frecuencia ruido

    // ruido desplazamiento
    float n = (noise2d(uv * noiseFreq + t * 0.5) - 0.5) * noiseAmp;

    // —— IZQUIERDO ——
    vec2 pL = uv - vec2(thW*0.5, 0.5);
    float wWL = thW*0.5 + sin(uv.y*waveFreq + t*waveSpeed)*waveAmp + n;
    float hL  = 0.5;
    float dL  = sdRoundedRect(pL, vec2(wWL,hL), cornerRad);

    // —— DERECHO ——
    vec2 pR = uv - vec2(1.0 - thW*0.5, 0.5);
    float wWR = thW*0.5 + sin(uv.y*waveFreq + t*waveSpeed + 3.1416)*waveAmp + n;
    float hR  = 0.5;
    float dR  = sdRoundedRect(pR, vec2(wWR,hR), cornerRad);

    // —— INFERIOR ——
    vec2 pB = uv - vec2(0.5, thH*0.5);
    float wHB = thH*0.5 + sin(uv.x*waveFreq + t*waveSpeed)*waveAmp + n;
    float wBW = 0.5;
    float dB  = sdRoundedRect(pB, vec2(wBW,wHB), cornerRad);

    // —— SUPERIOR ——
    vec2 pT = uv - vec2(0.5, 1.0 - thH*0.5);
    float wHT = thH*0.5 + sin(uv.x*waveFreq + t*waveSpeed + 3.1416)*waveAmp + n;
    float wTW = 0.5;
    float dT  = sdRoundedRect(pT, vec2(wTW,wHT), cornerRad);

    // Color arcoíris animado
    vec3 outCol = vec3(0.0);
    float hue, f;

    // Borde izquierdo
    if (dL < glowOut) {
        f   = (dL<0.0) ? 1.0 : 1.0 - (dL/glowIn);
        hue = fract(uv.y + t * rainSpeed);
        outCol = hsv2rgb(vec3(hue,1.0,1.0)) * clamp(f,0.0,1.0);
    }
    // Borde derecho
    else if (dR < glowOut) {
        f   = (dR<0.0) ? 1.0 : 1.0 - (dR/glowIn);
        hue = fract(uv.y + t * rainSpeed);
        outCol = hsv2rgb(vec3(hue,1.0,1.0)) * clamp(f,0.0,1.0);
    }
    // Borde inferior
    else if (dB < glowOut) {
        f   = (dB<0.0) ? 1.0 : 1.0 - (dB/glowIn);
        hue = fract(uv.x + t * rainSpeed);
        outCol = hsv2rgb(vec3(hue,1.0,1.0)) * clamp(f,0.0,1.0);
    }
    // Borde superior
    else if (dT < glowOut) {
        f   = (dT<0.0) ? 1.0 : 1.0 - (dT/glowIn);
        hue = fract(uv.x + t * rainSpeed);
        outCol = hsv2rgb(vec3(hue,1.0,1.0)) * clamp(f,0.0,1.0);
    } else {
        discard;
    }

    gl_FragColor = vec4(outCol, 1.0);
}
