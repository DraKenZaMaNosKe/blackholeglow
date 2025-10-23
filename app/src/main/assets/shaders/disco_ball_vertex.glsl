// ============================================
// DISCO BALL VERTEX SHADER
// Features: Breathing effect, audio reactivity ready
// ============================================

precision mediump float;

uniform mat4 u_MVP;
uniform float u_Time;
uniform float u_AudioBass;  // 0.0 to 1.0 - bass intensity

attribute vec3 a_Position;
attribute vec2 a_TexCoord;
attribute vec3 a_Normal;

varying vec2 v_TexCoord;
varying vec3 v_Normal;
varying vec3 v_WorldPos;
varying float v_Pulse;

void main() {
    vec3 pos = a_Position;

    // ============================================
    // BREATHING EFFECT - Ball pulses smoothly
    // ============================================
    float breathe = sin(u_Time * 1.5) * 0.03;  // Slow breathing
    float pulse = 1.0 + breathe;

    // Add audio reactivity (bass makes it bigger)
    pulse += u_AudioBass * 0.12;

    // Apply pulse to position
    pos *= pulse;

    // ============================================
    // WAVE RIPPLE EFFECT (subtle)
    // ============================================
    // Small waves travel across surface
    float wave = sin(a_Position.y * 4.0 - u_Time * 3.0) * 0.015;
    vec3 normal = normalize(a_Position);
    pos += normal * wave;

    // ============================================
    // Output to fragment shader
    // ============================================
    v_TexCoord = a_TexCoord;
    v_Normal = a_Normal;
    v_WorldPos = pos;
    v_Pulse = pulse;  // Pass pulse value for brightness

    gl_Position = u_MVP * vec4(pos, 1.0);
}
