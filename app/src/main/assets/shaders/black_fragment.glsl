// shaders/stars_fragment.glsl
precision mediump float;

// Alpha global desde Java
uniform float u_Alpha;
// Tiempo en segundos para animar el parpadeo
uniform float u_Time;

// Generador pseudo‐aleatorio a partir de vec2
float rand(vec2 co){
    return fract(sin(dot(co,vec2(12.9898,78.233)))*43758.5453);
}

void main(){
    // Coordenadas de fragmento normalizadas a [0..1]
    vec2 uv = gl_FragCoord.xy / vec2(800.0, 600.0);
    // ↑ Ajusta 800×600 a tu resolución real (ó pásala con otro uniform)

    // Definimos un tamaño de “celda” para agrupar posibles estrellas
    float cellSize = 0.05;        // cada 5% de pantalla
    vec2  cell    = floor(uv / cellSize);
    vec2  f       = fract(uv / cellSize);

    // Probabilidad de que en esta celda haya estrella (~2%)
    if (rand(cell) > 0.02) {
        discard;
    }

    // Posición aleatoria dentro de la celda
    vec2  starPos = vec2(rand(cell+1.0), rand(cell+2.0));

    // Tamaño aleatorio (simula distancia)
    float starSize = mix(0.01, 0.03, rand(cell+3.0));

    // Distancia desde este pixel hasta el centro de estrella
    float d = distance(f, starPos);

    // Parpadeo con fase y velocidad distintas
    float speed = 0.5 + rand(cell+4.0)*1.5;
    float phase = rand(cell+5.0) * 6.2831;
    float blink = sin(u_Time * speed + phase) * 0.5 + 0.5;

    // Si estamos dentro del radio de la estrella, la dibujamos
    if (d < starSize) {
        // Color según “temperatura”
        vec3 cold  = vec3(0.7, 0.8, 1.0);
        vec3 warm  = vec3(1.0, 0.8, 0.6);
        float tcol = rand(cell+6.0);
        vec3  col  = mix(cold, warm, tcol);
        gl_FragColor = vec4(col * blink, u_Alpha);
    } else {
        discard;
    }
}
