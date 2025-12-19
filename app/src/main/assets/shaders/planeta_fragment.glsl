// ============================================
// archivo: planeta_fragment.glsl
// Shader de planeta SIMPLIFICADO - Solo textura + color sólido
// Sin efectos de glow que causan artefactos
// ============================================

#ifdef GL_ES
precision mediump float;
#endif

// ------- Uniforms -------
uniform float u_Time;
uniform sampler2D u_Texture;
uniform int u_UseSolidColor;
uniform vec4 u_SolidColor;
uniform float u_Alpha;

// ------- Varyings -------
varying vec2 v_TexCoord;
varying vec3 v_WorldPos;

void main() {
    // Obtener color base
    vec4 texColor = texture2D(u_Texture, v_TexCoord);

    // ✅ FIX: Forzar alpha a 1.0 para evitar transparencia no deseada
    texColor.a = 1.0;

    // Elegir entre textura o color sólido
    vec4 baseColor;
    if (u_UseSolidColor == 1) {
        baseColor = u_SolidColor;
    } else {
        baseColor = texColor;
    }

    // Aplicar alpha global
    baseColor.a *= u_Alpha;

    // Salida final - solo el color, sin efectos complicados
    gl_FragColor = baseColor;
}
