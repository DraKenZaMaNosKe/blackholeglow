#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_Texture;
uniform float u_Time;
uniform vec2 u_Resolution;
uniform float u_AspectRatio;

varying vec2 v_TexCoord;

void main() {
    // Ajustar UV para mantener proporción y cubrir toda la pantalla
    vec2 uv = v_TexCoord;

    // Centrar y escalar para cubrir (cover mode)
    vec2 center = vec2(0.5, 0.5);
    uv = (uv - center);

    // Ajuste de aspecto para modo cover
    float screenAspect = u_Resolution.x / u_Resolution.y;
    float textureAspect = 1.77777; // Asumiendo imagen 16:9

    if (screenAspect > textureAspect) {
        // Pantalla más ancha - escalar en Y
        uv.y *= textureAspect / screenAspect;
    } else {
        // Pantalla más alta - escalar en X
        uv.x *= screenAspect / textureAspect;
    }

    uv += center;

    // Animación desactivada (opcional para después)
    // uv.x += sin(u_Time * 0.05) * 0.01;
    // uv.y += cos(u_Time * 0.03) * 0.01;

    // Clamp para evitar bordes
    uv = clamp(uv, 0.0, 1.0);

    gl_FragColor = texture2D(u_Texture, uv);
}
