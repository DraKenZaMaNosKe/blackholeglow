precision mediump float;
uniform vec2 u_Resolution;

void main(){
    // Normalizamos coords [0,1]
    vec2 uv = gl_FragCoord.xy / u_Resolution;
    // Grosor del borde = 2% del tamaño
    float t = 0.02;
    // Calculamos si estamos dentro del borde en X o Y
    bool left   = uv.x < t;
    bool right  = uv.x > 1.0 - t;
    bool bottom = uv.y < t;
    bool top    = uv.y > 1.0 - t;
    if (!(left || right || bottom || top)) {
        // Dentro del área central → nada
        discard;
    }
    // Borde rojo sólido
    gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);
}
