// black_fragment.glsl
// Fragment shader para UniverseBackground: pinta todo de azul
precision mediump float;

// Recibimos UV aunque no las usemos
varying vec2 v_TexCoord;

// Declaramos sampler y uniforms para mantener compatibilidad
uniform sampler2D u_Texture;
uniform float     u_Alpha;
uniform float     u_Time;

// Número de puntos que queremos dibujar
const int POINT_COUNT   = 60;
// Radio de cada punto (ajusta para hacerlo más grande/pequeño)
const float POINT_RADIUS = 0.008;

float rand(vec2 co) {
    return fract(sin(dot(co, vec2(12.9898,78.233))) * 43758.5453123);
}

void main() {
    vec2 uv = v_TexCoord;
    // RGBA: azul puro con la transparencia que le pases desde Java (u_Alpha)
    vec3 outColor = vec3(0.7,0.2,1.0); // fondo negro

    // Iteramos sobre cada “punto” procedural
    for(int i = 0; i < POINT_COUNT; i++) {
        float fi = float(i);

        // Semilla única por punto
        vec2 seed = vec2(fi, fi * 1.618);

        // Posición aleatoria en pantalla
        vec2 pos = vec2(
        rand(seed + 0.123),
        rand(seed + 4.321)
        );

        // Distancia del fragmento a ese punto
        float d = distance(uv, pos);

        if(d < POINT_RADIUS) {
            // Generamos un color RGB aleatorio para este punto
            float r = rand(seed + 2.0);
            float g = rand(seed + 5.0);
            float b = rand(seed + 9.0);
            outColor = vec3(0.0, 0.1, 0.9);
            break; // salimos al primer punto que encontremos

        }else{
            outColor = vec3(0.0, 0.1, 0.9);
        }

    }

    gl_FragColor = vec4(outColor, u_Alpha);

}



/*
// Número de puntos que queremos dibujar
const int POINT_COUNT   = 60;
// Radio de cada punto (ajusta para hacerlo más grande/pequeño)
const float POINT_RADIUS = 0.008;

// Función pseudo-aleatoria reproducible
float rand(vec2 co) {
    return fract(sin(dot(co, vec2(12.9898,78.233))) * 43758.5453123);
}

void main() {
    vec2 uv = v_TexCoord;
    vec3 outColor = vec3(0.0); // fondo negro

    // Iteramos sobre cada “punto” procedural
    for(int i = 0; i < POINT_COUNT; i++) {
        float fi = float(i);

        // Semilla única por punto
        vec2 seed = vec2(fi, fi * 1.618);

        // Posición aleatoria en pantalla
        vec2 pos = vec2(
        rand(seed + 0.123),
        rand(seed + 4.321)
        );

        // Distancia del fragmento a ese punto
        float d = distance(uv, pos);

        // Si estamos dentro del radio, asignamos color
        if(d < POINT_RADIUS) {
            // Generamos un color RGB aleatorio para este punto
            float r = rand(seed + 2.0);
            float g = rand(seed + 5.0);
            float b = rand(seed + 9.0);
            outColor = vec3(r, g, b);
            break; // salimos al primer punto que encontremos
        }
    }

    // Salida final (fondo negro + puntos de color) con alpha desde Java
    gl_FragColor = vec4(outColor, u_Alpha);
}
*/