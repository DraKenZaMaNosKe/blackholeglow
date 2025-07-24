#ifdef GL_ES
precision highp float;
#endif

uniform float u_Time;
uniform vec2 u_Resolution;

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = (fragCoord.xy / u_Resolution.xy) - .5;
    float t = u_Time * .1 + ((.25 + .05 * sin(u_Time * .1))
    /(length(uv) + .07)) * 2.2;
    float si = sin(t);
    float co = cos(t);
    mat2 ma = mat2(co, si, -si, co);

    float v1=0., v2=0., v3=0., s=0.;
    for (int i = 0; i < 90; i++) {
        vec3 p = s * vec3(uv, 0.0);
        p.xy *= ma;
        p += vec3(.22, .3, s - 1.5 - sin(u_Time * .13)*.1);
        for (int j = 0; j < 8; j++)
        p = abs(p) / dot(p,p) - 0.659;
        v1 += dot(p,p)*.0015*(1.8 + sin(length(uv*13.) + .5 - u_Time*.2));
        v2 += dot(p,p)*.0013*(1.5 + sin(length(uv*14.5)+1.2 - u_Time*.3));
        v3 += length(p.xy*10.)*.0003;
        s += .035;
    }

    float len = length(uv);
    v1 *= smoothstep(.7, 0., len);
    v2 *= smoothstep(.5, 0., len);
    v3 *= smoothstep(.9, 0., len);

    vec3 col = vec3(
    v3*(1.5 + sin(u_Time*.2)*.4),
    (v1+v3)*.3,
    v2
    ) + smoothstep(.2,0.,len)*.85 + smoothstep(0.,.6,v3)*.3;

    fragColor = vec4(vec3(0.0,1.0,0.0), 1.0);
}

void main() {
    mainImage(gl_FragColor, gl_FragCoord.xy);
}
