// ============================================
// archivo: planeta_fragment.glsl
// Shader del planeta con glow HSB + overlay aditivo
// Compatible con textura o color sólido, sin seams
// ============================================

#ifdef GL_ES
precision mediump float;   // Precisión media: segura en la mayoría de GPUs móviles
#endif

// ------- Uniforms compartidos -------
// u_Time: tiempo en segundos (≈ 0..∞) para animaciones
uniform float u_Time;
// u_Texture: textura del planeta (solo se usa si u_UseSolidColor==0)
uniform sampler2D u_Texture;
// u_UseSolidColor: 1 => usar u_SolidColor como albedo base; 0 => usar u_Texture
uniform int   u_UseSolidColor;
// u_SolidColor: color base cuando no hay textura (rgba en 0..1)
uniform vec4  u_SolidColor;
// u_Alpha: multiplicador global de alpha (0..1)
uniform float u_Alpha;

// ------- Varyings -------
varying vec2 v_TexCoord;   // Coordenadas UV en [0,1]
varying vec3 v_WorldPos;   // Posición mundial (no usada aquí, pero disponible)

// ------- Parámetros del glow (radios en 0..~1.0) -------
const float GLOW_RADIUS = 0.45;  // radio externo del halo
const float CORE_RADIUS = 0.20;  // radio del núcleo brillante
const float EDGE_SOFT   = 0.03;  // suavizado de bordes (0.01..0.08 recomendado)

// ============================================
// Utilidades RGB <-> HSB  (The Book of Shaders / IQ)
// ============================================
vec3 rgb2hsb(in vec3 c){
    vec4 K = vec4(0.0,-1.0/3.0,2.0/3.0,-1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y)/(6.0*d + e)), d/(q.x + e), q.x);
}

vec3 hsb2rgb(in vec3 c){
    vec3 rgb = clamp(abs(mod(c.x*6.0 + vec3(0.0,4.0,2.0), 6.0) - 3.0) - 1.0,0.0,1.0);
    rgb = rgb*rgb*(3.0 - 2.0*rgb);
    return c.z * mix(vec3(1.0), rgb, c.y);
}

// ============================================
// Helpers para máscara radial "seamless"
//  - Centro en (0.5,0.5) pero con wrap en U para evitar la costura
// ============================================
vec2 centeredUVSeamless(vec2 uv){
    // uvX en (-0.5,0.5] con wrap; uvY centrado en [-0.5,0.5]
    float x = fract(uv.x + 0.5) - 0.5;
    float y = uv.y - 0.5;
    return vec2(x, y);
}

// Devuelve pesos (coreW, haloW) en [0..1] usando radios y suavizado
vec2 glowWeights(float r){ // r: distancia radial normalizada (≈0 centro, ≲1 borde)
                           // Núcleo: 1 en centro, 0 tras CORE_RADIUS con suave EDGE_SOFT
                           float coreW = 1.0 - smoothstep(CORE_RADIUS, CORE_RADIUS + EDGE_SOFT, r);

                           // Halo: activo entre CORE_RADIUS y GLOW_RADIUS (sin invadir el núcleo)
                           float inner  = smoothstep(CORE_RADIUS, CORE_RADIUS + EDGE_SOFT, r);
                           float outer  = 1.0 - smoothstep(GLOW_RADIUS, GLOW_RADIUS + EDGE_SOFT, r);
                           float haloW  = clamp(inner * outer, 0.0, 1.0);

                           return vec2(coreW, haloW);
}

void main(){
    // 1) Albedo base: textura o color sólido, sin tocar UV ni HSB aquí
    vec4 baseTex = texture2D(u_Texture, v_TexCoord);           // muestra textura una vez
    vec4 albedo  = (u_UseSolidColor == 1) ? u_SolidColor : baseTex;
    albedo.a    *= u_Alpha;                                    // aplica alpha global (0..1)

    // 2) Coordenadas centradas sin seam y distancia radial (normalizada ~0..1.414)
    vec2 p = centeredUVSeamless(v_TexCoord);                   // [-0.5,0.5] “sin costura”
    float r = length(p) / 0.5;                                 // 0 en centro; ~1 es el borde del cuadrito

    // 3) Pesos de núcleo/halo con bordes suaves
    vec2 w = glowWeights(r);                                   // w.x = core, w.y = halo

    // 4) Animaciones suaves: pulso (brillo) y “swirl” (tono)
    float pulse = sin(u_Time*3.0 + r*10.0)*0.10 + 0.90;        // 0.80..1.00 aprox
    float ang   = atan(p.y, p.x);                              // ángulo polar [-π,π]
    float swirl = sin(ang*3.0 + u_Time*2.0)*0.5 + 0.5;         // 0..1

    // 5) Color base del glow en HSB parte de u_SolidColor (así no alteramos la textura)
    vec3 hsbBase = rgb2hsb(u_SolidColor.rgb);

    // Núcleo: más brillante, un poco menos saturado, tinte dorado
    vec3 hsbCore = hsbBase;
    hsbCore.z = min(hsbCore.z + 0.50, 1.0);                    // +brillo (0..1)
    hsbCore.y = hsbCore.y * (1.0 - 0.40);                      // -saturación
    hsbCore.x = hsbCore.x + 0.05;                              // +hue (≈dorado)

    // Halo: variación de hue con swirl, algo más saturado y brillo con pulso
    vec3 hsbHalo = hsbBase;
    hsbHalo.x = hsbHalo.x + swirl*0.10;                        // hue animado
    hsbHalo.y = min(hsbHalo.y*(1.0 + 0.30), 1.0);              // +saturación
    hsbHalo.z = hsbHalo.z * pulse;                             // brillo pulsante

    // 6) Colores RGB del glow y mezcla aditiva (¡sin discard!)
    vec3 glowCore = hsb2rgb(hsbCore) * w.x;                    // núcleo ponderado
    vec3 glowHalo = hsb2rgb(hsbHalo) * w.y;                    // halo ponderado
    vec3 glowRGB  = glowCore + glowHalo;                       // suma de contribuciones

    // 7) Composición final (aditiva sobre el albedo). Clamp evita sobreexposición
    vec3 finalRGB = clamp(albedo.rgb + glowRGB, 0.0, 1.0);
    gl_FragColor  = vec4(finalRGB, albedo.a);                  // alpha = del albedo
}
