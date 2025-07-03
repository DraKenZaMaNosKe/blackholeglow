precision mediump float;

uniform float u_Time;       // segundos desde inicio
uniform vec2  u_Resolution; // tamaño en px

// Grosor del borde aumentado al 4%
const float THICKNESS = 0.02;
// Velocidad más rápida: 1 vuelta por segundo
const float SPEED     = 0.25;
// Anchura del haz ampliada al 8%
const float HEAD_WID  = 0.08;

void main(){
    vec2 uv = gl_FragCoord.xy / u_Resolution;
    // Distancia al área central
    float b = min(min(uv.x, 1.0-uv.x), min(uv.y, 1.0-uv.y));
    // Sólo dibujamos en la banda del borde
    if (b > THICKNESS) discard;

    // Calcula la posición a lo largo del perímetro [0,1)
    float inner = 1.0 - THICKNESS;
    float p;
    if (uv.x < THICKNESS) {
        p = uv.y;
    } else if (uv.y > inner) {
        p = THICKNESS + uv.x;
    } else if (uv.x > inner) {
        p = THICKNESS + inner + (inner - uv.y);
    } else {
        p = THICKNESS + inner*2.0 + (inner - uv.x);
    }
    float perim = 4.0 * inner;
    float pos   = p / perim;

    // Posición del haz, avanza con el tiempo y SPEED
    float headPos = fract(u_Time * SPEED);
    float d = abs(headPos - pos);
    d = min(d, 1.0 - d);

    // Perfil de brillo: 1 en el centro, 0 fuera de HEAD_WID
    float glow = smoothstep(0.0, HEAD_WID, HEAD_WID - d);

    // Color base semitransparente (azul suave)
    vec3 baseColor = vec3(0.0, 0.5, 1.0);
    // Color del haz blanco brillante
    vec3 headColor = vec3(1.0, 1.0, 1.0);

    // Mezcla según glow, y alpha igual a 0.6 + 0.4*glow
    vec3 col = mix(baseColor, headColor, glow);
    float alpha = 0.6 + 0.4 * glow;

    gl_FragColor = vec4(col, alpha);
}
