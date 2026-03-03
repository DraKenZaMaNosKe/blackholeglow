#version 300 es
precision mediump float;

in vec2 v_TexCoord;

uniform float u_Time;
uniform vec2  u_Resolution;
uniform sampler2D u_Texture;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash(i),                    hash(i + vec2(1.0, 0.0)), u.x),
        mix(hash(i + vec2(0.0, 1.0)),   hash(i + vec2(1.0, 1.0)), u.x),
        u.y
    );
}

float fbm(vec2 p) {
    float v = 0.0, amp = 0.5;
    for (int i = 0; i < 3; i++) {
        v += amp * noise(p);
        p *= 2.1; amp *= 0.5;
    }
    return v;
}

vec2 liquidDistort(vec2 uv, float t) {
    vec2 p = uv * 2.5;
    float nx = fbm(p + vec2(t * 0.15, t * 0.10));
    float ny = fbm(p + vec2(t * 0.12, t * 0.18) + vec2(5.2, 1.3));
    float rippleX = sin(uv.y * 30.0 + t * 2.0) * 0.0025;
    float rippleY = sin(uv.x * 25.0 + t * 1.7) * 0.002;
    return vec2((nx - 0.5) * 0.016 + rippleX, (ny - 0.5) * 0.016 + rippleY);
}

vec3 chromeBlue(vec2 uv, float t) {
    float sweep = sin(uv.x * 3.14 + t * 0.6 + uv.y * 1.5) * 0.5 + 0.5;
    float band  = smoothstep(0.2, 0.8, sweep);
    vec3 silver    = vec3(0.55, 0.65, 0.80);
    vec3 highlight = vec3(0.75, 0.92, 1.0);
    return mix(silver, highlight, band * 0.5);
}

float eyeGlow(vec2 uv, float t) {
    vec2 eyeL = vec2(0.435, 0.40);
    vec2 eyeR = vec2(0.565, 0.40);
    float d = min(length(uv - eyeL), length(uv - eyeR));
    float pulse   = 0.7 + 0.3 * sin(t * 2.2);
    float flicker = 1.0 + 0.08 * sin(t * 13.0) * sin(t * 7.3);
    return smoothstep(0.09, 0.0, d) * pulse * flicker;
}

float lightBeam(vec2 uv, float t) {
    float beam1 = smoothstep(0.02, 0.0, abs(uv.x - 0.08)) * (0.6 + 0.4 * sin(t * 0.7));
    float beam2 = smoothstep(0.02, 0.0, abs(uv.x - 0.92)) * (0.6 + 0.4 * sin(t * 0.9 + 1.0));
    float beam3 = smoothstep(0.015, 0.0, abs(uv.x - 0.50)) * (0.3 + 0.3 * sin(t * 1.1 + 0.5));
    return beam1 + beam2 + beam3;
}

vec2 waterRipple(vec2 uv, float t) {
    float factor = smoothstep(0.65, 0.85, uv.y);
    float wave   = sin(uv.x * 20.0 + t * 1.5) * 0.006
                 + sin(uv.x * 13.0 - t * 1.1) * 0.004;
    return vec2(wave * factor, 0.0);
}

float mercuryDrops(vec2 uv, float t) {
    vec2 d1 = vec2(0.35 + sin(t * 0.5)  * 0.08, fract(0.05 + t * 0.06));
    vec2 d2 = vec2(0.62 + sin(t * 0.4)  * 0.07, fract(0.30 + t * 0.08));
    vec2 d3 = vec2(0.48 + sin(t * 0.65) * 0.10, fract(0.60 + t * 0.05));
    vec2 d4 = vec2(0.25 + sin(t * 0.3)  * 0.06, fract(0.80 + t * 0.07));
    float r1 = smoothstep(0.025, 0.0, length(uv - d1));
    float r2 = smoothstep(0.018, 0.0, length(uv - d2));
    float r3 = smoothstep(0.014, 0.0, length(uv - d3));
    float r4 = smoothstep(0.012, 0.0, length(uv - d4));
    return clamp(r1 + r2 + r3 + r4, 0.0, 1.0);
}

const float TWO_PI = 6.28318530718;

float safeTime(float raw) {
    return mod(raw, TWO_PI * 100.0);
}

void main() {
    vec2 uv = v_TexCoord;
    float t  = safeTime(u_Time);

    vec2 waterDist = waterRipple(uv, t);
    vec2 sampleUV  = clamp(uv + waterDist, 0.0, 1.0);

    vec4 tex = texture(u_Texture, sampleUV);
    vec3 col  = tex.rgb;

    float lum   = dot(col, vec3(0.299, 0.587, 0.114));
    vec3 chrome = chromeBlue(uv, t);
    float cBlend = (fbm(uv * 3.0 + t * 0.1) * 0.12 + 0.08) * (1.0 - lum * 0.5);
    col = mix(col, chrome, clamp(cBlend, 0.0, 0.3));

    float eye     = eyeGlow(uv, t);
    vec3 eyeColor = vec3(0.1, 0.6, 1.0);
    col = mix(col, eyeColor, eye * 0.85);
    col += eyeColor * eye * 0.4;

    float beams    = lightBeam(uv, t);
    col += vec3(0.0, 0.85, 1.0) * beams * 0.12;

    float drops    = mercuryDrops(uv, t);
    col = mix(col, vec3(0.80, 0.90, 1.0), drops * 0.80);
    col += vec3(1.0) * drops * 0.25;

    vec2 vig  = uv * 2.0 - 1.0;
    col *= 1.0 - dot(vig, vig) * 0.22;

    col  = pow(col, vec3(0.95));
    float gray = dot(col, vec3(0.333));
    col  = mix(vec3(gray), col, 1.12);

    fragColor = vec4(clamp(col, 0.0, 1.0), tex.a);
}
