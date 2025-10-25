// ============================================
// archivo: planeta_iluminado_fragment.glsl
// Shader para planetas con iluminación del sol
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
uniform vec2 u_Resolution;

// ------- Varyings -------
varying vec2 v_TexCoord;
varying vec3 v_WorldPos;

void main() {
    // Obtener color base de la textura
    vec4 texColor = texture2D(u_Texture, v_TexCoord);
    vec4 baseColor = (u_UseSolidColor == 1) ? u_SolidColor : texColor;

    // ============================================
    // SISTEMA DE ILUMINACIÓN SIMPLE
    // ============================================

    // Posición del sol (centro de la escena)
    vec3 sunPosition = vec3(0.0, 0.0, 0.0);

    // Calcular posición aproximada del fragmento en el planeta
    // Usamos las UV para aproximar la normal de la esfera
    vec2 centered = v_TexCoord - 0.5;
    float distFromCenter = length(centered);

    // NO descartar píxeles - dejar que el modelo 3D defina la forma
    // Esto evita agujeros en la esfera
    // if(distFromCenter > 0.5) {
    //     discard;
    // }

    // Calcular normal aproximada de la esfera
    float z = sqrt(max(0.0, 0.25 - centered.x * centered.x - centered.y * centered.y));
    vec3 normal = normalize(vec3(centered.x, centered.y, z * 2.0));

    // Dirección hacia el sol (simplificada)
    vec3 lightDir = normalize(vec3(-v_WorldPos.x, -v_WorldPos.y, 1.0));

    // ============================================
    // COMPONENTES DE ILUMINACIÓN
    // ============================================

    // 1. Luz Ambiente - siempre presente para que no quede totalmente negro
    vec3 ambientColor = baseColor.rgb * 0.2;  // 20% de luz ambiente

    // 2. Luz Difusa - iluminación principal del sol
    float diffuseFactor = max(0.0, dot(normal, lightDir));
    vec3 sunColor = vec3(1.0, 0.95, 0.8);  // Color cálido del sol
    vec3 diffuseColor = baseColor.rgb * sunColor * diffuseFactor * 0.8;

    // 3. Sombra propia - oscurecer el lado opuesto al sol
    float shadowFactor = smoothstep(-0.2, 0.3, diffuseFactor);

    // ============================================
    // COMBINAR ILUMINACIÓN
    // ============================================

    vec3 finalColor = ambientColor + (diffuseColor * shadowFactor);

    // Añadir un poco de variación con el tiempo (opcional)
    float pulse = sin(u_Time * 2.0) * 0.02 + 0.98;
    finalColor *= pulse;

    // Asegurar que no sobrepasamos el blanco
    finalColor = clamp(finalColor, 0.0, 1.0);

    // Alpha final - mantener completamente opaco para evitar agujeros
    float finalAlpha = baseColor.a * u_Alpha;

    // NO suavizar bordes para mantener la esfera completa
    // Los bordes los define el modelo 3D, no el shader

    gl_FragColor = vec4(finalColor, finalAlpha);
}