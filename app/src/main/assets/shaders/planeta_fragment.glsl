// assets/shaders/planeta_fragment_test.glsl
precision mediump float;

varying vec2 v_TexCoord;
uniform sampler2D u_Texture;
uniform float      u_Time;

void main() {
    vec4 tc = texture2D(u_Texture, v_TexCoord);

    // un tint entre naranja y rosa en función del time
    float f = (sin(u_Time * 2.0) + 1.0) * 0.5;
    vec3 tint = mix(vec3(1.0,0.5,0.0), vec3(1.0,0.2,0.8), f);

    // 20% tint + 80% textura
    vec3 outColor = mix(tc.rgb, tint, 0.2);

    gl_FragColor = vec4(outColor, tc.a);
}
