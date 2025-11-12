/**
 * ╔════════════════════════════════════════════════════════════════╗
 * ║  ☄️ ASTEROIDE REALISTA - Shader con Textura + Iluminación    ║
 * ║  Usa matasteroide.png con iluminación realista                ║
 * ╚════════════════════════════════════════════════════════════════╝
 */

#ifdef GL_ES
precision mediump float;
#endif

// Uniforms
uniform sampler2D u_Texture;   // Textura matasteroide.png
uniform float u_Alpha;          // Transparencia
uniform float u_Time;           // Tiempo para animaciones

// Varyings
varying vec2 v_TexCoord;
varying vec3 v_Normal;

// Configuración de luz
const vec3 LIGHT_DIR = vec3(0.5, 0.3, 1.0);  // Dirección del sol
const float AMBIENT = 0.25;                   // Luz ambiente (25%)
const float DIFFUSE = 0.75;                   // Luz difusa (75%)

void main() {
    // 1. Obtener color de la textura
    vec4 texColor = texture2D(u_Texture, v_TexCoord);
    
    // 2. Calcular iluminación difusa (Lambert)
    vec3 normal = normalize(v_Normal);
    vec3 lightDir = normalize(LIGHT_DIR);
    float diffuse = max(dot(normal, lightDir), 0.0);
    
    // 3. Combinar luz ambiente + difusa
    float lighting = AMBIENT + diffuse * DIFFUSE;
    
    // 4. Aplicar iluminación a la textura
    vec3 finalColor = texColor.rgb * lighting;
    
    // 5. Salida final
    gl_FragColor = vec4(finalColor, texColor.a * u_Alpha);
}
