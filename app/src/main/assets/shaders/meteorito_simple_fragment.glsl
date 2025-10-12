// Fragment shader simple para meteoritos con efecto de fuego
precision mediump float;

uniform vec4 u_Color;
uniform float u_Opacity;
uniform float u_Time;
varying vec2 v_TexCoord;

void main() {
    // Efecto de fuego/calor simple
    float heat = sin(u_Time * 10.0 + v_TexCoord.x * 5.0) * 0.1 + 0.9;
    vec3 fireColor = u_Color.rgb * heat;

    // Añadir brillo en el centro
    float dist = length(v_TexCoord - 0.5);
    float glow = 1.0 - smoothstep(0.0, 0.5, dist);
    fireColor += vec3(1.0, 0.8, 0.4) * glow * 0.5;

    gl_FragColor = vec4(fireColor, u_Opacity * u_Color.a);
}