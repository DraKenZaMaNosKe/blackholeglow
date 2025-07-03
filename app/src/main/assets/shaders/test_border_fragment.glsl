/*
╔════════════════════════════════════════════╗
║   🌟 Shader BlueScreen v1.1 🌟             ║
║  • Versión simplificada: solo fondo azul ║
╚════════════════════════════════════════════╝
*/
precision mediump float;

// Uniforms originales (aunque no los usemos ahora)
uniform float u_Time;        // tiempo en segundos
uniform vec2  u_Resolution;  // resolución del viewport (px)

void main() {
    // 🎨 Pintamos toda la pantalla de un azul puro
    gl_FragColor = vec4(0.0, 0.0, 1.0, 1.0);
}
