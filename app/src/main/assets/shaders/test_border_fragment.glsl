// blobs_adaptado_fragment.glsl
#version 100
precision highp  float;

// — Uniforms que debes setear desde tu Renderer en Java —
uniform float u_Time;        // tiempo en segundos
uniform vec2  u_Resolution;  // resolución del viewport (ancho, alto)

// — Función auxiliar que genera cada “burbuja” animada —
float makePoint(float x, float y, float fx, float fy, float sx, float sy, float t) {
    // Desplaza la posición según seno y coseno para animar
    float xx = x + sin(t * fx) * sx;
    float yy = y + cos(t * fy) * sy;
    // Retorna intensidad inversa a la distancia
    return 1.0 / sqrt(xx * xx + yy * yy);
}

void main() {
    // 1) Mapeo de coordenadas a espacio centrado [-1,1] con aspect ratio
    vec2 p = (gl_FragCoord.xy / u_Resolution.x) * 2.0
    - vec2(1.0, u_Resolution.y / u_Resolution.x);

    // 2) Escalamos para multiplicar la densidad de burbujas
    p *= 2.0;
    float x = p.x;
    float y = p.y;

    // 3) Sumo varias contribuciones para el canal rojo
    float a = makePoint(x, y, 3.3, 2.9, 0.3, 0.3, u_Time);
    a += makePoint(x, y, 1.9, 2.0, 0.4, 0.4, u_Time);
    a += makePoint(x, y, 0.8, 0.7, 0.4, 0.5, u_Time);
    a += makePoint(x, y, 2.3, 0.1, 0.6, 0.3, u_Time);
    a += makePoint(x, y, 1.8, 1.7, 0.5, 0.4, u_Time);

    // 4) Sumo varias contribuciones para el canal verde
    float b = makePoint(x, y, 1.2, 1.9, 0.3, 0.3, u_Time);
    b += makePoint(x, y, 0.7, 2.7, 0.4, 0.4, u_Time);
    b += makePoint(x, y, 1.4, 0.6, 0.4, 0.5, u_Time);
    b += makePoint(x, y, 2.6, 0.4, 0.6, 0.3, u_Time);
    b += makePoint(x, y, 0.7, 1.4, 0.5, 0.4, u_Time);
    b += makePoint(x, y, 0.7, 1.7, 0.4, 0.4, u_Time);
    b += makePoint(x, y, 0.8, 0.5, 0.4, 0.5, u_Time);
    b += makePoint(x, y, 1.4, 0.9, 0.6, 0.3, u_Time);
    b += makePoint(x, y, 0.7, 1.3, 0.5, 0.4, u_Time);

    // 5) Sumo varias contribuciones para el canal azul
    float c = makePoint(x, y, 3.7, 0.3, 0.3, 0.3, u_Time);
    c += makePoint(x, y, 1.9, 1.3, 0.4, 0.4, u_Time);
    c += makePoint(x, y, 0.8, 0.9, 0.4, 0.5, u_Time);
    c += makePoint(x, y, 1.2, 1.7, 0.6, 0.3, u_Time);
    c += makePoint(x, y, 0.3, 0.6, 0.5, 0.4, u_Time);
    c += makePoint(x, y, 0.3, 0.3, 0.4, 0.4, u_Time);
    c += makePoint(x, y, 1.4, 0.8, 0.4, 0.5, u_Time);
    c += makePoint(x, y, 0.2, 0.6, 0.6, 0.3, u_Time);
    c += makePoint(x, y, 1.3, 0.5, 0.5, 0.4, u_Time);

    // 6) Combinación final y normalización
    vec3 color = vec3(a, b, c) / 32.0;

    // 7) Envío el color al framebuffer
    gl_FragColor = vec4(color, 1.0);
}
