/**
 * Fragment shader para meteoritos pequeños con iluminación mejorada
 */
#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_Texture;
uniform vec4 u_Color;
uniform float u_Opacity;

varying vec2 v_TexCoord;
varying vec3 v_Position;

// Configuración de luz MÁS BRILLANTE para meteoritos pequeños
const vec3 LIGHT_DIR = vec3(0.5, 0.3, 1.0);
const float AMBIENT = 0.5;   // Más luz ambiente (50%)
const float DIFFUSE = 0.5;   // Menos contraste

void main() {
    // 1. Obtener color de la textura
    vec4 texColor = texture2D(u_Texture, v_TexCoord);
    
    // 2. Calcular iluminación usando la posición como normal
    vec3 normal = normalize(v_Position);
    vec3 lightDir = normalize(LIGHT_DIR);
    float diffuse = max(dot(normal, lightDir), 0.0);
    
    // 3. Combinar luz ambiente + difusa
    float lighting = AMBIENT + diffuse * DIFFUSE;
    
    // 4. Aplicar iluminación a la textura
    vec3 litColor = texColor.rgb * lighting;
    
    // 5. Aplicar opacidad
    float alpha = u_Opacity * texColor.a;
    
    // 6. Salida final
    gl_FragColor = vec4(litColor, alpha);
}
