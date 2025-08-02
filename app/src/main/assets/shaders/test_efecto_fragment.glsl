precision highp float;

uniform float u_Time;
uniform vec2 u_Resolution;

void main() {

    vec2 fragCoord = gl_FragCoord.xy;
    // reproducimos uv = fragCoord / iResolution - 0.5
    vec2 uv = (fragCoord / u_Resolution) - 0.5;

    // tiempo modulado y desplazamientos
    float t = u_Time * 0.1
    + ((0.25 + 0.05 * sin(u_Time * 0.1))
    / (length(uv) + 0.07)
    ) * 2.2;
    float si = sin(t);
    float co = cos(t);

    gl_FragColor = vec4(vec3(0.0,0.8,0.0), 1.0);
}
